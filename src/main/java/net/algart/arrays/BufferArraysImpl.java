/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.arrays;

import java.util.EmptyStackException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.ByteOrder;

/**
 * <p>Implementations of arrays for {@link BufferMemoryModel} and {@link LargeMemoryModel}.
 *
 * @author Daniel Alievsky
 */
class BufferArraysImpl {
    /**
     * This method <b>must</b> be called after creating any {@link AbstractBufferArray} instance
     * via <tt>new</tt> operator.
     *
     * <p>This method is not called automatically (from the constructor) to provide a guarantee
     * that JVM will not store any references to <tt>a</tt> instance in the <tt>Runntable</tt> implementation
     * of the finalization task.
     *
     * @param a new created array.
     */
    static void forgetOnDeallocation(AbstractBufferArray a) {
        if (!(a.storage instanceof DirectDataStorages.DirectStorage)) {
            LargeMemoryModel.globalArrayFinalizer.invokeOnDeallocation(a, new AbstractBufferArrayFinalizer(a));
        }
    }

    private static class AbstractBufferArrayFinalizer implements Runnable {
        final int id;
        volatile DataStorage storageRef;

        AbstractBufferArrayFinalizer(AbstractBufferArray a) {
            this.id = System.identityHashCode(a);
            this.storageRef = a.storage;
            a.finalizer = this;
        }

        public void run() {
            storageRef.forgetArray(id);
        }
    }

    static abstract class AbstractBufferArray extends AbstractArray {
        /**
         * Storage of array data.
         */
        protected DataStorage storage;

        /**
         * The offset of a subarray; 0 if it is not a subarray.
         */
        protected long offset = 0;

        /**
         * The copy-on-next-write flag.
         */
        protected boolean copyOnNextWrite = false;

        /**
         * Underlying (parent) array, if this instance is a view of it, created by
         * {@link #subArray}, {@link #asImmutable}, {@link #asCopyOnNextWrite} or {@link UpdatableArray#asUnresizable}
         * methods, or <tt>null</tt> in other cases.
         *
         * <p>If it is <tt>null</tt>, this instance must be attached to the {@link #storage}
         * and controlled by {@link LargeMemoryModel#globalArrayFinalizer}.
         * But creating a lot of such objects leads to much workload for the garbage collector and even
         * can lead to unexpected <tt>OutOfMemory</tt>.
         *
         * <p>If it is not <tt>null</tt>, this field is a strong reference guaranteeing that the parent array
         * will be collected as garbage only after its views &mdash; and, so, that the data storage
         * will not be informed about finalization all array too early.
         */
        protected AbstractBufferArray underlyingArray;

        boolean attached;

        /**
         * The finalizer that fill be called after deallocation this instance by the garbage collector.
         * If this instance switches to another {@link DataStorage}, the
         * <tt>finalizer.{@link AbstractBufferArrayFinalizer#storageRef}</tt> field must be corrected.
         * (See the implementation of {@link #reallocateStorage()}.)
         *
         * <p>This field is initialized by <tt>forgetOnDeallocation</tt> method.
         */
        volatile AbstractBufferArrayFinalizer finalizer = null;

        /**
         * The lock for critical operations alike controlling garbage collection.
         */
        private final ReentrantLock lock = new ReentrantLock();

        AbstractBufferArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(initialCapacity, initialLength);
            this.offset = initialOffset;
            this.storage = storage;
            this.storage.attachArray(this);
            this.attached = true;
            if (doAllocate) {
                if (!(this instanceof UpdatableArray))
                    throw new AssertionError("doAllocate argument may be true only in updatable arrays");
                this.storage.allocate(initialCapacity, isUnresizable());
                setNewStatus();
            }
        }

        AbstractBufferArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(initialCapacity, initialLength);
            this.offset = initialOffset;
            this.storage = storage;
            if (underlyingArray == null) {
                if (!(this instanceof UpdatableArray))
                    throw new AssertionError("underlyingArray argument may be null only in updatable arrays");
                this.storage.attachArray(this);
                this.attached = true;
                this.storage.allocate(initialCapacity, isUnresizable());
                setNewStatus();
            } else {
                this.attached = false;
            }
            this.underlyingArray = underlyingArray;
        }

        final void switchStorage(DataStorage newStorage) {
            lock.lock();
            try {
                boolean alreadyAttached = this.attached;
                if (alreadyAttached) {
                    this.storage.forgetArray(System.identityHashCode(this));
                    if (!(this.storage instanceof DirectDataStorages.DirectStorage)) {
                        assert this.finalizer != null : "Null AbstractBufferArray.finalizer field";
                        this.finalizer.storageRef = newStorage;
                    }
                }
                this.attached = true;
                this.underlyingArray = null;
                this.storage = newStorage;
                this.storage.attachArray(this);
                if (!alreadyAttached) {
                    forgetOnDeallocation(this);
                }
            } finally {
                lock.unlock();
            }
        }

        final void reallocateStorage() {
            if (this.isImmutable() || !(this instanceof UpdatableArray))
                throw new AssertionError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed reallocation)");
            boolean unresizable = isUnresizable();
            DataStorage newStorage = storage.newCompatibleEmptyStorage(unresizable);
            newStorage.allocate(length, unresizable);
            if (!newStorage.copy(this.storage, this.offset, 0, length))
                throw new AssertionError("Cannot reallocateStorage(): newCompatibleEmptyStorage "
                        + "cannot be copied from this storage");
            // It is very important here to copy the storage BEFORE switching!
            // In other case, the following situation will be possible:
            // the old storage have no attached arrays, but someone (we here,
            // while calling "copy") activate new mappings in that storage.
            // This situation is illegal and will not be properly processed by finalization
            // algorithms of some storage implementations (namely MappedDataStorages).
            switchStorage(newStorage);
            this.offset = 0;
            this.copyOnNextWrite = false;
            this.setNewStatus(); // necessary when reallocating copy-on-next-write arrays
        }

        public ByteOrder byteOrder() {
            return storage.byteOrder();
        }

        public final void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            storage.getData(offset + arrayPos, destArray, destArrayOffset, count);
        }

        public final void getData(long arrayPos, Object destArray) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (arrayPos < 0 || arrayPos > length)
                throw rangeException(arrayPos);
            int count = java.lang.reflect.Array.getLength(destArray);
            if (count > length - arrayPos) {
                count = (int) (length - arrayPos);
            }
            storage.getData(offset + arrayPos, destArray, 0, count);
        }

        public final void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (!(this instanceof BitArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed getBits)");
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            ((DataBitStorage) storage).getBits(offset + arrayPos, destArray, destArrayOffset, count);
        }

        public long nextQuickPosition(long from) {
            if (from >= length) {
                return -1;
            }
            // now we can be sure that offset+from <= offset+length and may be calculated without overflow
            long result = ((offset + (from < 0 ? 63 : from + 63)) & ~63) - offset;
            assert (offset + result) % 64 == 0;
            if (result < 0 || result >= length) { // < 0 means overflow
                return -1;
            }
            return result;
        }

        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (!(this instanceof UpdatableArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed setData)");
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            if (isCopyOnNextWrite())
                reallocateStorage();
            storage.setData(offset + arrayPos, srcArray, srcArrayOffset, count);
            return (UpdatableArray) this;
        }

        public UpdatableArray setData(long arrayPos, Object srcArray) {
            if (!(this instanceof UpdatableArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed setData)");
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (arrayPos < 0 || arrayPos > length)
                throw rangeException(arrayPos);
            int count = java.lang.reflect.Array.getLength(srcArray);
            if (count > length - arrayPos)
                count = (int) (length - arrayPos);
            if (isCopyOnNextWrite())
                reallocateStorage();
            storage.setData(offset + arrayPos, srcArray, 0, count);
            return (UpdatableArray) this;
        }

        public UpdatableBitArray setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
            if (!(this instanceof UpdatableBitArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed setBits)");
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            if (isCopyOnNextWrite())
                reallocateStorage();
            ((DataBitStorage) storage).setBits(offset + arrayPos, srcArray, srcArrayOffset, count);
            return (UpdatableBitArray) this;
        }

        public final void copy(long destIndex, long srcIndex, long count) {
            if (!(this instanceof UpdatableArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed copy)");
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of copied elements (count = " + count
                        + ") in " + getClass());
            if (srcIndex < 0)
                throw rangeException(srcIndex);
            if (srcIndex > length - count)
                throw rangeException(srcIndex + count - 1);
            if (destIndex < 0)
                throw rangeException(destIndex);
            if (destIndex > length - count)
                throw rangeException(destIndex + count - 1);
            if (isCopyOnNextWrite())
                reallocateStorage();
            storage.copy(storage, offset + srcIndex, offset + destIndex, count);
        }

        public final void swap(long firstIndex, long secondIndex, long count) {
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of swapped elements (count = " + count
                        + ") in " + getClass());
            if (firstIndex < 0)
                throw rangeException(firstIndex);
            if (firstIndex > length - count)
                throw rangeException(firstIndex + count - 1);
            if (secondIndex < 0)
                throw rangeException(secondIndex);
            if (secondIndex > length - count)
                throw rangeException(secondIndex + count - 1);
            if (isCopyOnNextWrite())
                reallocateStorage();
            storage.swap(storage, offset + firstIndex, offset + secondIndex, count);
        }


        /* // Bad version with double copying: reminder, how easy to make a bug for several years
        public UpdatableArray copyOld(Array src) {
            if (!(this instanceof UpdatableArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                    + "(unallowed copy)");
            if (src instanceof AbstractBufferArray) {
                AbstractBufferArray a = (AbstractBufferArray)src;
                long count = a.length < length ? a.length : length;
                if (a.storage.getClass() == storage.getClass()) {
                    checkCopyArguments((UpdatableArray)this, src);
                    if (!storage.copy(a.storage, a.offset, offset, count))
                        defaultCopy((UpdatableArray)this, src);
                } else {
                    defaultCopy((UpdatableArray)this, src);
                }
            }
            defaultCopy((UpdatableArray)this, src);
            return (UpdatableArray)this;
        }
        */

        public UpdatableArray copy(Array src) {
            if (!(this instanceof UpdatableArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed copy)");
            if (src instanceof AbstractBufferArray) {
                AbstractBufferArray a = (AbstractBufferArray) src;
                long count = a.length < length ? a.length : length;
                if (a.storage.getClass() == storage.getClass()) {
                    checkCopyArguments((UpdatableArray) this, src);
                    if (isCopyOnNextWrite())
                        reallocateStorage();
                    if (storage.copy(a.storage, a.offset, offset, count)) {
                        return (UpdatableArray) this;
                    }
                }
            }
            defaultCopy((UpdatableArray) this, src);
            return (UpdatableArray) this;
        }

        public UpdatableArray swap(UpdatableArray another) {
            if (!(this instanceof UpdatableArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                        + "(unallowed swap)");
            defaultSwap((UpdatableArray) this, another);
            return (UpdatableArray) this;
        }

        public void setNonNew() {
            setNewStatus(false);
        }

        final void ensureCapacityImpl(long minCapacity) {
            long currentCapacity = capacity();
            if (minCapacity > currentCapacity) {
                long newCapacity;
                if (storage instanceof MappedDataStorages.MappedStorage) {
                    newCapacity = (minCapacity + 255L) & ~255L;
                    // here is no sense for complex algorithms:
                    // storage.changeCapacity will increase the file length by bankSize() blocks
                } else {
                    newCapacity = Math.min(DataStorage.maxSupportedLengthImpl(elementType()),
                            currentCapacity < 10000 ? currentCapacity * 3 :
                                    currentCapacity < 500000 ? currentCapacity * 2 :
                                            (currentCapacity * 3) / 2 + 1);
                }
                // Overflow is possible above!
                // But in this case newCapacity will be negative and
                // will be replaced by the following operator:
                if (newCapacity < minCapacity) {
                    // in particular, if the required newCapacity > maxSupportedLengthImpl
                    newCapacity = minCapacity;
                }
                DataStorage newStorage = storage.changeCapacity(newCapacity, offset, this.length);
                if (newStorage != storage) {
                    switchStorage(newStorage);
                    this.copyOnNextWrite = false;
                    this.offset = 0;
                }
                this.capacity = newCapacity;
            }
        }

        final void lengthImpl(long newLength) {
            long oldLength = this.length;
            if (newLength != oldLength) {
                ensureCapacityImpl(newLength);
                this.length = newLength; // must be changed BEFORE reallocateStorage()
                if (isCopyOnNextWrite()) {
                    reallocateStorage();
                } else if (newLength < oldLength) {
                    storage.clearData(offset + newLength, oldLength - newLength);
                }
                // It's better to clear data while rare array-reducing operation,
                // than every time while creating new large array.
            }
        }

        final void trimImpl() {
            if (this.length < capacity()) {
                DataStorage newStorage = storage.changeCapacity(this.length, 0, this.length);
                if (newStorage != storage) {
                    switchStorage(newStorage);
                    this.copyOnNextWrite = false;
                    this.offset = 0;
                }
                this.capacity = this.length;
            }
        }

        public final MutableCharArray append(String value) {
            if (!(this instanceof MutableCharArray))
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation (unallowed append)");
            char[] chars = new char[value.length()];
            value.getChars(0, chars.length, chars, 0);
            return ((MutableCharArray) this).append(SimpleMemoryModel.asUpdatableCharArray(chars));
        }

        public final boolean isCopyOnNextWrite() {
            return copyOnNextWrite;
        }

        public final void checkUnallowedMutation() throws UnallowedMutationError {
        }

        @Override
        public final void loadResources(ArrayContext context) {
            storage.loadResources(offset, offset + length);
        }

        @Override
        public final void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
            storage.actualizeLazyFilling(context, offset, offset + length);
            storage.flushResources(offset, offset + length, forcePhysicalWriting);
        }

        @Override
        public final void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
            storage.actualizeLazyFilling(context, offset, offset + length);
            storage.freeResources(this, forcePhysicalWriting);
        }
    }

    /*Repeat()
      (public(?:\s+\w+)+\s+(?:get|set|pop|push)Int.*?(?:\r(?!\n)|\n|\r\n)\s*}(?=\s*public)\s*) ==>
          $1,,$1,,$1,, ,,$1,,$1;;
      (public(?:\s+\w+)+\s+(?:get|set|pop|push)Long.*?(?:\r(?!\n)|\n|\r\n)\s*}(?=\s*public)\s*) ==>
          $1,,$1,,$1,,$1,, ,,$1;;
      (public(?:\s+\w+)+\s+\w+ndexOf\(long\s+\w+,\s*long\s+\w+,\s*long(.*?)\n\s*}\s*) ==> $1,,$1,,$1,,$1,, ,,$1;;
      (public(?:\s+\w+)+\s+(?:get|set|pop|push)Double.*?(?:\r(?!\n)|\n|\r\n)\s*}\s*) ==> $1,,$1,,$1,,$1,,$1,, ;;
      (public(?:\s+\w+)+\s+\w+ndexOf\(long\s+\w+,\s*long\s+\w+,\s*double(.*?)\n\s*}\s*) ==> $1,,$1,,$1,,$1,,$1,, ;;
      (public(?:\s+\w+)+\s+fill\((?:long\s+\w+,\s*long\s+\w+,\s*)?long\s+va.*?(?:\r(?!\n)|\n|\r\n)\s*}\s*) ==>
          $1,,$1,,$1,,$1,, ,,$1;;
      (public(?:\s+\w+)+\s+fill\((?:long\s+\w+,\s*long\s+\w+,\s*)?double\s+va.*?(?:\r(?!\n)|\n|\r\n)\s*}\s*) ==>
          $1,,$1,,$1,,$1,,$1,, ;;
      (return\s+)-157777 ==> $10,,$10,,$10,,$1Integer.MIN_VALUE,,$1Long.MIN_VALUE,,$1-157777;;
      (return\s+)157778  ==> $10xFFFF,,$10xFF,,$10xFFFF,,$1Integer.MAX_VALUE,,$1Long.MAX_VALUE,,$1157778;;
      (return\s+)(valueForFloatingPoint)(?=;\s*\/\/min) ==>
          $1minPossibleValue(),,$1minPossibleValue(),,$1minPossibleValue(),,
          $1minPossibleValue(),,$1minPossibleValue(),,$1$2;;
      (return\s+)(valueForFloatingPoint)(?=;\s*\/\/max) ==>
          $1maxPossibleValue(),,$1maxPossibleValue(),,$1maxPossibleValue(),,
          $1maxPossibleValue(),,$1maxPossibleValue(),,$1$2;;
      (value\s*==\s*)(\(float\)\s*value) ==>
          $1$2,,$1((int) value & 0xFF),,$1((int) value & 0xFFFF),,$1$2,,$1$2,,$1$2 ;;
      (float\s+getFloat) ==> $1,,int getByte,,int getShort,,$1,,$1,,$1;;
      float              ==> char,,byte,,short,,int,,long,,double;;
      Float\.valueOf\((get) ==> Character.valueOf($1,,Byte.valueOf((byte) $1,,Short.valueOf((short) $1,,
          Integer.valueOf($1,,Long.valueOf($1,,Double.valueOf($1;;
      \bFloat\b          ==> Character,,Byte,,Short,,Integer,,Long,,Double;;
      Float(?!ing)       ==> Char,,Byte,,Short,,Int,,Long,,Double;;
      FLOAT              ==> CHAR,,BYTE,,SHORT,,INT,,LONG,,DOUBLE;;
      \((double|long|int)\)\s*(popByte\(\)|popShort\(\)) ==>
          ($1) $2,,($1) ($2 & 0xFF),,($1) ($2 & 0xFFFF),,($1) $2,,... ;;
      (return\s+(?:\(\w+\)\s?)?)(storage\.get(?:Byte|Short)\((?:offset\s*\+\s*)?index\))\s*; ==>
          $1$2;,,return ($2 & 0xFF);,,return ($2 & 0xFFFF);,,$1$2;,,$1$2;,,$1$2; ;;
      (\(int\)\s*)(storage\.getLong\([^)]*\)) ==> $1$2,,$1$2,,$1$2,,$1$2,,Arrays.truncateLongToInt($2),,$1$2 ;;
      (\(int\)\s*popLong\(\)) ==> $1,,$1,,$1,,$1,,Arrays.truncateLongToInt(popLong()),,$1
    */
    static class BufferFloatArray extends AbstractBufferArray implements FloatArray {
        BufferFloatArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferFloatArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return float.class;
        }

        public Class<? extends FloatArray> type() {
            return FloatArray.class;
        }

        public Class<? extends UpdatableFloatArray> updatableType() {
            return UpdatableFloatArray.class;
        }

        public Class<? extends MutableFloatArray> mutableType() {
            return MutableFloatArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Float.valueOf(getFloat(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_FLOAT;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint; //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint; //max
        }

        public final long minPossibleValue() {
            return -157777;
        }

        public final long maxPossibleValue() {
            return 157778;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getFloat(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == (float) value ? indexOf(lowIndex, highIndex, (float) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (float) value ? lastIndexOf(lowIndex, highIndex, (float) value) : -1;
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (long) storage.getFloat(offset + index);
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (int) storage.getFloat(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == (float) value ? indexOf(lowIndex, highIndex, (float) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (float) value ? lastIndexOf(lowIndex, highIndex, (float) value) : -1;
        }

        public final float getFloat(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getFloat(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfFloat(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfFloat(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferFloatArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferFloatArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataFloatBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataFloatBuffer) super.buffer(mode, capacity);
        }

        public DataFloatBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataFloatBuffer) super.buffer(mode);
        }

        public DataFloatBuffer buffer(long capacity) {
            return (DataFloatBuffer) super.buffer(capacity);
        }

        public DataFloatBuffer buffer() {
            return (DataFloatBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public FloatArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final FloatArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableFloatArray mutableClone(MemoryModel memoryModel) {
            return (MutableFloatArray) super.mutableClone(memoryModel);
        }

        public final UpdatableFloatArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableFloatArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferFloatArray result = (BufferFloatArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public float[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array float[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferFloatArray extends BufferFloatArray implements UpdatableFloatArray {
        UpdatableBufferFloatArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferFloatArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setFloat(index, (Float) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setFloat(index, (float) value);
        }

        public final void setLong(long index, long value) {
            setFloat(index, (float) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setFloat(offset + index, (float) value);
        }

        public final void setFloat(long index, float value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setFloat(offset + index, value);
        }

        public UpdatableFloatArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableFloatArray fill(long position, long count, double value) {
            return fill(position, count, (float) value);
        }

        public UpdatableFloatArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableFloatArray fill(long position, long count, long value) {
            return fill(position, count, (float) value);
        }

        public UpdatableFloatArray fill(float value) {
            return fill(0, length, value);
        }

        public UpdatableFloatArray fill(long position, long count, float value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Float filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableFloatArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableFloatArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final FloatArray asImmutable() {
            return new BufferFloatArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableFloatArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferFloatArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array float[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferFloatArray extends UpdatableBufferFloatArray implements MutableFloatArray {
        MutableBufferFloatArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferFloatArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableFloatArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableFloatArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableFloatArray trim() {
            trimImpl();
            return this;
        }

        public MutableFloatArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Float.valueOf(popFloat());
        }

        public void pushElement(Object value) {
            pushFloat((Float) value);
        }

        public double popDouble() {
            return (double) popFloat();
        }

        public long popLong() {
            return (long) popFloat();
        }

        public int popInt() {
            return (int) popFloat();
        }

        public void add(double value) {
            pushFloat((float) value);
        }

        public void add(long value) {
            pushFloat((float) value);
        }

        public void add(int value) {
            pushFloat((float) value);
        }

        public float popFloat() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getFloat(offset + length);
        }

        public void pushFloat(float value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setFloat(offset + length - 1, value);
        }

        public MutableFloatArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableFloatArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableFloatArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableFloatArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableFloatArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferFloatArray result = new MutableBufferFloatArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableFloatArray asUnresizable() {
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableFloatArray shallowClone() {
            return (MutableBufferFloatArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array float[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static class BufferCharArray extends AbstractBufferArray implements CharArray {
        BufferCharArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferCharArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return char.class;
        }

        public Class<? extends CharArray> type() {
            return CharArray.class;
        }

        public Class<? extends UpdatableCharArray> updatableType() {
            return UpdatableCharArray.class;
        }

        public Class<? extends MutableCharArray> mutableType() {
            return MutableCharArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Character.valueOf(getChar(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_CHAR;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue(); //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue(); //max
        }

        public final long minPossibleValue() {
            return 0;
        }

        public final long maxPossibleValue() {
            return 0xFFFF;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getChar(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == (char) value ? indexOf(lowIndex, highIndex, (char) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (char) value ? lastIndexOf(lowIndex, highIndex, (char) value) : -1;
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (long) storage.getChar(offset + index);
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (int) storage.getChar(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == (char) value ? indexOf(lowIndex, highIndex, (char) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (char) value ? lastIndexOf(lowIndex, highIndex, (char) value) : -1;
        }

        public final char getChar(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getChar(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfChar(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfChar(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferCharArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferCharArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataCharBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataCharBuffer) super.buffer(mode, capacity);
        }

        public DataCharBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataCharBuffer) super.buffer(mode);
        }

        public DataCharBuffer buffer(long capacity) {
            return (DataCharBuffer) super.buffer(capacity);
        }

        public DataCharBuffer buffer() {
            return (DataCharBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public CharArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final CharArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableCharArray mutableClone(MemoryModel memoryModel) {
            return (MutableCharArray) super.mutableClone(memoryModel);
        }

        public final UpdatableCharArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableCharArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferCharArray result = (BufferCharArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public char[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array char[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferCharArray extends BufferCharArray implements UpdatableCharArray {
        UpdatableBufferCharArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferCharArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setChar(index, (Character) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setChar(index, (char) value);
        }

        public final void setLong(long index, long value) {
            setChar(index, (char) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setChar(offset + index, (char) value);
        }

        public final void setChar(long index, char value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setChar(offset + index, value);
        }

        public UpdatableCharArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableCharArray fill(long position, long count, double value) {
            return fill(position, count, (char) value);
        }

        public UpdatableCharArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableCharArray fill(long position, long count, long value) {
            return fill(position, count, (char) value);
        }

        public UpdatableCharArray fill(char value) {
            return fill(0, length, value);
        }

        public UpdatableCharArray fill(long position, long count, char value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Character filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableCharArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableCharArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final CharArray asImmutable() {
            return new BufferCharArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableCharArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferCharArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array char[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferCharArray extends UpdatableBufferCharArray implements MutableCharArray {
        MutableBufferCharArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferCharArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableCharArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableCharArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableCharArray trim() {
            trimImpl();
            return this;
        }

        public MutableCharArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Character.valueOf(popChar());
        }

        public void pushElement(Object value) {
            pushChar((Character) value);
        }

        public double popDouble() {
            return (double) popChar();
        }

        public long popLong() {
            return (long) popChar();
        }

        public int popInt() {
            return (int) popChar();
        }

        public void add(double value) {
            pushChar((char) value);
        }

        public void add(long value) {
            pushChar((char) value);
        }

        public void add(int value) {
            pushChar((char) value);
        }

        public char popChar() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getChar(offset + length);
        }

        public void pushChar(char value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setChar(offset + length - 1, value);
        }

        public MutableCharArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableCharArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableCharArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableCharArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableCharArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferCharArray result = new MutableBufferCharArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableCharArray asUnresizable() {
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableCharArray shallowClone() {
            return (MutableBufferCharArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array char[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }


    static class BufferByteArray extends AbstractBufferArray implements ByteArray {
        BufferByteArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferByteArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return byte.class;
        }

        public Class<? extends ByteArray> type() {
            return ByteArray.class;
        }

        public Class<? extends UpdatableByteArray> updatableType() {
            return UpdatableByteArray.class;
        }

        public Class<? extends MutableByteArray> mutableType() {
            return MutableByteArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Byte.valueOf((byte) getByte(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_BYTE;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue(); //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue(); //max
        }

        public final long minPossibleValue() {
            return 0;
        }

        public final long maxPossibleValue() {
            return 0xFF;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getByte(offset + index) & 0xFF);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == ((int) value & 0xFF) ? indexOf(lowIndex, highIndex, (byte) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == ((int) value & 0xFF) ? lastIndexOf(lowIndex, highIndex, (byte) value) : -1;
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getByte(offset + index) & 0xFF);
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getByte(offset + index) & 0xFF);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == ((int) value & 0xFF) ? indexOf(lowIndex, highIndex, (byte) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == ((int) value & 0xFF) ? lastIndexOf(lowIndex, highIndex, (byte) value) : -1;
        }

        public final int getByte(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getByte(offset + index) & 0xFF);
        }

        public final long indexOf(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfByte(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfByte(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferByteArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferByteArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataByteBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataByteBuffer) super.buffer(mode, capacity);
        }

        public DataByteBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataByteBuffer) super.buffer(mode);
        }

        public DataByteBuffer buffer(long capacity) {
            return (DataByteBuffer) super.buffer(capacity);
        }

        public DataByteBuffer buffer() {
            return (DataByteBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public ByteArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final ByteArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableByteArray mutableClone(MemoryModel memoryModel) {
            return (MutableByteArray) super.mutableClone(memoryModel);
        }

        public final UpdatableByteArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableByteArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferByteArray result = (BufferByteArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public byte[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array byte[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferByteArray extends BufferByteArray implements UpdatableByteArray {
        UpdatableBufferByteArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferByteArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setByte(index, (Byte) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setByte(index, (byte) value);
        }

        public final void setLong(long index, long value) {
            setByte(index, (byte) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setByte(offset + index, (byte) value);
        }

        public final void setByte(long index, byte value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setByte(offset + index, value);
        }

        public UpdatableByteArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableByteArray fill(long position, long count, double value) {
            return fill(position, count, (byte) value);
        }

        public UpdatableByteArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableByteArray fill(long position, long count, long value) {
            return fill(position, count, (byte) value);
        }

        public UpdatableByteArray fill(byte value) {
            return fill(0, length, value);
        }

        public UpdatableByteArray fill(long position, long count, byte value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Byte filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableByteArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableByteArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final ByteArray asImmutable() {
            return new BufferByteArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableByteArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferByteArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array byte[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferByteArray extends UpdatableBufferByteArray implements MutableByteArray {
        MutableBufferByteArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferByteArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableByteArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableByteArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableByteArray trim() {
            trimImpl();
            return this;
        }

        public MutableByteArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Byte.valueOf(popByte());
        }

        public void pushElement(Object value) {
            pushByte((Byte) value);
        }

        public double popDouble() {
            return (double) (popByte() & 0xFF);
        }

        public long popLong() {
            return (long) (popByte() & 0xFF);
        }

        public int popInt() {
            return (int) (popByte() & 0xFF);
        }

        public void add(double value) {
            pushByte((byte) value);
        }

        public void add(long value) {
            pushByte((byte) value);
        }

        public void add(int value) {
            pushByte((byte) value);
        }

        public byte popByte() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getByte(offset + length);
        }

        public void pushByte(byte value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setByte(offset + length - 1, value);
        }

        public MutableByteArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableByteArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableByteArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableByteArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableByteArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferByteArray result = new MutableBufferByteArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableByteArray asUnresizable() {
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableByteArray shallowClone() {
            return (MutableBufferByteArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array byte[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }


    static class BufferShortArray extends AbstractBufferArray implements ShortArray {
        BufferShortArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferShortArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return short.class;
        }

        public Class<? extends ShortArray> type() {
            return ShortArray.class;
        }

        public Class<? extends UpdatableShortArray> updatableType() {
            return UpdatableShortArray.class;
        }

        public Class<? extends MutableShortArray> mutableType() {
            return MutableShortArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Short.valueOf((short) getShort(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_SHORT;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue(); //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue(); //max
        }

        public final long minPossibleValue() {
            return 0;
        }

        public final long maxPossibleValue() {
            return 0xFFFF;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getShort(offset + index) & 0xFFFF);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == ((int) value & 0xFFFF) ? indexOf(lowIndex, highIndex, (short) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == ((int) value & 0xFFFF) ? lastIndexOf(lowIndex, highIndex, (short) value) : -1;
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getShort(offset + index) & 0xFFFF);
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getShort(offset + index) & 0xFFFF);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == ((int) value & 0xFFFF) ? indexOf(lowIndex, highIndex, (short) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == ((int) value & 0xFFFF) ? lastIndexOf(lowIndex, highIndex, (short) value) : -1;
        }

        public final int getShort(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (storage.getShort(offset + index) & 0xFFFF);
        }

        public final long indexOf(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfShort(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfShort(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferShortArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferShortArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataShortBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataShortBuffer) super.buffer(mode, capacity);
        }

        public DataShortBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataShortBuffer) super.buffer(mode);
        }

        public DataShortBuffer buffer(long capacity) {
            return (DataShortBuffer) super.buffer(capacity);
        }

        public DataShortBuffer buffer() {
            return (DataShortBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public ShortArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final ShortArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableShortArray mutableClone(MemoryModel memoryModel) {
            return (MutableShortArray) super.mutableClone(memoryModel);
        }

        public final UpdatableShortArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableShortArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferShortArray result = (BufferShortArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public short[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array short[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferShortArray extends BufferShortArray implements UpdatableShortArray {
        UpdatableBufferShortArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferShortArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setShort(index, (Short) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setShort(index, (short) value);
        }

        public final void setLong(long index, long value) {
            setShort(index, (short) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setShort(offset + index, (short) value);
        }

        public final void setShort(long index, short value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setShort(offset + index, value);
        }

        public UpdatableShortArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableShortArray fill(long position, long count, double value) {
            return fill(position, count, (short) value);
        }

        public UpdatableShortArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableShortArray fill(long position, long count, long value) {
            return fill(position, count, (short) value);
        }

        public UpdatableShortArray fill(short value) {
            return fill(0, length, value);
        }

        public UpdatableShortArray fill(long position, long count, short value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Short filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableShortArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableShortArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final ShortArray asImmutable() {
            return new BufferShortArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableShortArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferShortArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array short[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferShortArray extends UpdatableBufferShortArray implements MutableShortArray {
        MutableBufferShortArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferShortArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableShortArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableShortArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableShortArray trim() {
            trimImpl();
            return this;
        }

        public MutableShortArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Short.valueOf(popShort());
        }

        public void pushElement(Object value) {
            pushShort((Short) value);
        }

        public double popDouble() {
            return (double) (popShort() & 0xFFFF);
        }

        public long popLong() {
            return (long) (popShort() & 0xFFFF);
        }

        public int popInt() {
            return (int) (popShort() & 0xFFFF);
        }

        public void add(double value) {
            pushShort((short) value);
        }

        public void add(long value) {
            pushShort((short) value);
        }

        public void add(int value) {
            pushShort((short) value);
        }

        public short popShort() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getShort(offset + length);
        }

        public void pushShort(short value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setShort(offset + length - 1, value);
        }

        public MutableShortArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableShortArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableShortArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableShortArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableShortArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferShortArray result = new MutableBufferShortArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableShortArray asUnresizable() {
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableShortArray shallowClone() {
            return (MutableBufferShortArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array short[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }


    static class BufferIntArray extends AbstractBufferArray implements IntArray {
        BufferIntArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferIntArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return int.class;
        }

        public Class<? extends IntArray> type() {
            return IntArray.class;
        }

        public Class<? extends UpdatableIntArray> updatableType() {
            return UpdatableIntArray.class;
        }

        public Class<? extends MutableIntArray> mutableType() {
            return MutableIntArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Integer.valueOf(getInt(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_INT;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue(); //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue(); //max
        }

        public final long minPossibleValue() {
            return Integer.MIN_VALUE;
        }

        public final long maxPossibleValue() {
            return Integer.MAX_VALUE;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getInt(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == (int) value ? indexOf(lowIndex, highIndex, (int) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (int) value ? lastIndexOf(lowIndex, highIndex, (int) value) : -1;
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (long) storage.getInt(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == (int) value ? indexOf(lowIndex, highIndex, (int) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (int) value ? lastIndexOf(lowIndex, highIndex, (int) value) : -1;
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getInt(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfInt(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfInt(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferIntArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferIntArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataIntBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataIntBuffer) super.buffer(mode, capacity);
        }

        public DataIntBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataIntBuffer) super.buffer(mode);
        }

        public DataIntBuffer buffer(long capacity) {
            return (DataIntBuffer) super.buffer(capacity);
        }

        public DataIntBuffer buffer() {
            return (DataIntBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public IntArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final IntArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableIntArray mutableClone(MemoryModel memoryModel) {
            return (MutableIntArray) super.mutableClone(memoryModel);
        }

        public final UpdatableIntArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableIntArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferIntArray result = (BufferIntArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public int[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array int[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferIntArray extends BufferIntArray implements UpdatableIntArray {
        UpdatableBufferIntArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferIntArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setInt(index, (Integer) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setInt(index, (int) value);
        }

        public final void setLong(long index, long value) {
            setInt(index, (int) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setInt(offset + index, value);
        }

        public UpdatableIntArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableIntArray fill(long position, long count, double value) {
            return fill(position, count, (int) value);
        }

        public UpdatableIntArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableIntArray fill(long position, long count, long value) {
            return fill(position, count, (int) value);
        }

        public UpdatableIntArray fill(int value) {
            return fill(0, length, value);
        }

        public UpdatableIntArray fill(long position, long count, int value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Integer filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableIntArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableIntArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final IntArray asImmutable() {
            return new BufferIntArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableIntArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferIntArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array int[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferIntArray extends UpdatableBufferIntArray implements MutableIntArray {
        MutableBufferIntArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferIntArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableIntArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableIntArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableIntArray trim() {
            trimImpl();
            return this;
        }

        public MutableIntArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Integer.valueOf(popInt());
        }

        public void pushElement(Object value) {
            pushInt((Integer) value);
        }

        public double popDouble() {
            return (double) popInt();
        }

        public long popLong() {
            return (long) popInt();
        }

        public void add(double value) {
            pushInt((int) value);
        }

        public void add(long value) {
            pushInt((int) value);
        }

        public void add(int value) {
            pushInt((int) value);
        }

        public int popInt() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getInt(offset + length);
        }

        public void pushInt(int value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setInt(offset + length - 1, value);
        }

        public MutableIntArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableIntArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableIntArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableIntArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableIntArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferIntArray result = new MutableBufferIntArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableIntArray asUnresizable() {
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableIntArray shallowClone() {
            return (MutableBufferIntArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array int[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }


    static class BufferLongArray extends AbstractBufferArray implements LongArray {
        BufferLongArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferLongArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return long.class;
        }

        public Class<? extends LongArray> type() {
            return LongArray.class;
        }

        public Class<? extends UpdatableLongArray> updatableType() {
            return UpdatableLongArray.class;
        }

        public Class<? extends MutableLongArray> mutableType() {
            return MutableLongArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Long.valueOf(getLong(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_LONG;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue(); //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue(); //max
        }

        public final long minPossibleValue() {
            return Long.MIN_VALUE;
        }

        public final long maxPossibleValue() {
            return Long.MAX_VALUE;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getLong(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == (long) value ? indexOf(lowIndex, highIndex, (long) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (long) value ? lastIndexOf(lowIndex, highIndex, (long) value) : -1;
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return Arrays.truncateLongToInt(storage.getLong(offset + index));
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getLong(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfLong(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfLong(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferLongArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferLongArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataLongBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataLongBuffer) super.buffer(mode, capacity);
        }

        public DataLongBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataLongBuffer) super.buffer(mode);
        }

        public DataLongBuffer buffer(long capacity) {
            return (DataLongBuffer) super.buffer(capacity);
        }

        public DataLongBuffer buffer() {
            return (DataLongBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public LongArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final LongArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableLongArray mutableClone(MemoryModel memoryModel) {
            return (MutableLongArray) super.mutableClone(memoryModel);
        }

        public final UpdatableLongArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableLongArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferLongArray result = (BufferLongArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public long[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array long[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferLongArray extends BufferLongArray implements UpdatableLongArray {
        UpdatableBufferLongArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferLongArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setLong(index, (Long) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setLong(index, (long) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setLong(offset + index, (long) value);
        }

        public final void setLong(long index, long value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setLong(offset + index, value);
        }

        public UpdatableLongArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableLongArray fill(long position, long count, double value) {
            return fill(position, count, (long) value);
        }

        public UpdatableLongArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableLongArray fill(long position, long count, long value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Long filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableLongArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableLongArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final LongArray asImmutable() {
            return new BufferLongArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableLongArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferLongArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array long[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferLongArray extends UpdatableBufferLongArray implements MutableLongArray {
        MutableBufferLongArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferLongArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableLongArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableLongArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableLongArray trim() {
            trimImpl();
            return this;
        }

        public MutableLongArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Long.valueOf(popLong());
        }

        public void pushElement(Object value) {
            pushLong((Long) value);
        }

        public double popDouble() {
            return (double) popLong();
        }

        public int popInt() {
            return Arrays.truncateLongToInt(popLong());
        }

        public void add(double value) {
            pushLong((long) value);
        }

        public void add(long value) {
            pushLong((long) value);
        }

        public void add(int value) {
            pushLong((long) value);
        }

        public long popLong() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getLong(offset + length);
        }

        public void pushLong(long value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setLong(offset + length - 1, value);
        }

        public MutableLongArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableLongArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableLongArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableLongArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableLongArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferLongArray result = new MutableBufferLongArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableLongArray asUnresizable() {
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableLongArray shallowClone() {
            return (MutableBufferLongArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array long[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }


    static class BufferDoubleArray extends AbstractBufferArray implements DoubleArray {
        BufferDoubleArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferDoubleArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return double.class;
        }

        public Class<? extends DoubleArray> type() {
            return DoubleArray.class;
        }

        public Class<? extends UpdatableDoubleArray> updatableType() {
            return UpdatableDoubleArray.class;
        }

        public Class<? extends MutableDoubleArray> mutableType() {
            return MutableDoubleArray.class;
        }

        public final Object getElement(long index) {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Double.valueOf(getDouble(index));
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_DOUBLE;
        }

        public final double minPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint; //min
        }

        public final double maxPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint; //max
        }

        public final long minPossibleValue() {
            return -157777;
        }

        public final long maxPossibleValue() {
            return 157778;
        }

        public final long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (long) storage.getDouble(offset + index);
        }

        public final int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return (int) storage.getDouble(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == (double) value ? indexOf(lowIndex, highIndex, (double) value) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (double) value ? lastIndexOf(lowIndex, highIndex, (double) value) : -1;
        }

        public final double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getDouble(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfDouble(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfDouble(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferDoubleArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferDoubleArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataDoubleBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataDoubleBuffer) super.buffer(mode, capacity);
        }

        public DataDoubleBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataDoubleBuffer) super.buffer(mode);
        }

        public DataDoubleBuffer buffer(long capacity) {
            return (DataDoubleBuffer) super.buffer(capacity);
        }

        public DataDoubleBuffer buffer() {
            return (DataDoubleBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public DoubleArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final DoubleArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableDoubleArray mutableClone(MemoryModel memoryModel) {
            return (MutableDoubleArray) super.mutableClone(memoryModel);
        }

        public final UpdatableDoubleArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableDoubleArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferDoubleArray result = (BufferDoubleArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public double[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array double[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferDoubleArray extends BufferDoubleArray implements UpdatableDoubleArray {
        UpdatableBufferDoubleArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferDoubleArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setDouble(index, (Double) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setLong(long index, long value) {
            setDouble(index, (double) value);
        }

        public final void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setDouble(offset + index, (double) value);
        }

        public final void setDouble(long index, double value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setDouble(offset + index, value);
        }

        public UpdatableDoubleArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableDoubleArray fill(long position, long count, long value) {
            return fill(position, count, (double) value);
        }

        public UpdatableDoubleArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableDoubleArray fill(long position, long count, double value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            final Double filler = value;
            storage.fillData(offset + position, count, filler);
            return this;
        }

        public UpdatableDoubleArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableDoubleArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final DoubleArray asImmutable() {
            return new BufferDoubleArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableDoubleArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferDoubleArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array double[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    // Some casts become necessary after replacing by regexps in Repeater:
    @SuppressWarnings("cast")
    static final class MutableBufferDoubleArray extends UpdatableBufferDoubleArray implements MutableDoubleArray {
        MutableBufferDoubleArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferDoubleArray(
                DataStorage storage, long initialCapacity, long initialLength,
                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableDoubleArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableDoubleArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableDoubleArray trim() {
            trimImpl();
            return this;
        }

        public MutableDoubleArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            // boxing necessary for regexps in Repeater
            //noinspection UnnecessaryBoxing
            return Double.valueOf(popDouble());
        }

        public void pushElement(Object value) {
            pushDouble((Double) value);
        }

        public long popLong() {
            return (long) popDouble();
        }

        public int popInt() {
            return (int) popDouble();
        }

        public void add(double value) {
            pushDouble((double) value);
        }

        public void add(long value) {
            pushDouble((double) value);
        }

        public void add(int value) {
            pushDouble((double) value);
        }

        public double popDouble() {
            if (length == 0)
                throw new EmptyStackException();
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return storage.getDouble(offset + length);
        }

        public void pushDouble(double value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setDouble(offset + length - 1, value);
        }

        public MutableDoubleArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableDoubleArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableDoubleArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableDoubleArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableDoubleArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferDoubleArray result = new MutableBufferDoubleArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableDoubleArray asUnresizable() {
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableDoubleArray shallowClone() {
            return (MutableBufferDoubleArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array double[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    static class BufferBitArray extends AbstractBufferArray implements BitArray {
        BufferBitArray(DataStorage storage, long initialCapacity, long initialLength,
                       long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        BufferBitArray(DataStorage storage, long initialCapacity, long initialLength,
                       long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final Class<?> elementType() {
            return boolean.class;
        }

        public Class<? extends BitArray> type() {
            return BitArray.class;
        }

        public Class<? extends UpdatableBitArray> updatableType() {
            return UpdatableBitArray.class;
        }

        public Class<? extends MutableBitArray> mutableType() {
            return MutableBitArray.class;
        }

        public final Object getElement(long index) {
            return getBit(index);
        }

        public final long bitsPerElement() {
            return Arrays.BITS_PER_BIT;
        }

        public double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue();
        }

        public double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue();
        }

        public long minPossibleValue() {
            return 0;
        }

        public long maxPossibleValue() {
            return 1;
        }

        public final double getDouble(long index) {
            return getBit(index) ? 1.0 : 0.0;
        }

        public final long indexOf(long lowIndex, long highIndex, double value) {
            return value == 0 || value == 1 ? indexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == 0 || value == 1 ? lastIndexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public final long getLong(long index) {
            return getBit(index) ? 1 : 0;
        }

        public final int getInt(long index) {
            return getBit(index) ? 1 : 0;
        }

        public final long indexOf(long lowIndex, long highIndex, long value) {
            return value == 0 || value == 1 ? indexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == 0 || value == 1 ? lastIndexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public final boolean getBit(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            return storage.getBit(offset + index);
        }

        public final long indexOf(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.indexOfBit(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public final long lastIndexOf(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            if (highIndex <= lowIndex) {
                // this check guarantees that overflow is impossible below:
                // offset + lowIndex <= offset + highIndex <= offset + length <= Long.MAX_VALUE
                return -1;
            }
            long i = storage.lastIndexOfBit(offset + lowIndex, offset + highIndex, value);
            return i == -1 ? -1 : i - offset;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            return new BufferBitArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            return new BufferBitArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
        }

        public DataBitBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataBitBuffer) super.buffer(mode, capacity);
        }

        public DataBitBuffer buffer(DataBuffer.AccessMode mode) {
            return (DataBitBuffer) super.buffer(mode);
        }

        public DataBitBuffer buffer(long capacity) {
            return (DataBitBuffer) super.buffer(capacity);
        }

        public DataBitBuffer buffer() {
            return (DataBitBuffer) super.buffer();
        }

        public boolean isUnresizable() {
            return true;
        }

        public BitArray asImmutable() {
            return this;
        }

        public boolean isImmutable() {
            return true;
        }

        public final BitArray asTrustedImmutable() {
            return asImmutable();
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public final MutableBitArray mutableClone(MemoryModel memoryModel) {
            return (MutableBitArray) super.mutableClone(memoryModel);
        }

        public final UpdatableBitArray updatableClone(MemoryModel memoryModel) {
            return (UpdatableBitArray) super.updatableClone(memoryModel);
        }

        public Array shallowClone() {
            BufferBitArray result = (BufferBitArray) standardObjectClone();
            if (underlyingArray == null) {
                result.storage.attachArray(result);
                forgetOnDeallocation(result);
            }
            return result;
        }

        public boolean[] ja() {
            return Arrays.toJavaArray(this);
        }

        public String toString() {
            return "immutable AlgART array bit [" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isNew() ? ", new" : isNewReadOnlyView() ? ", new read-only view" : ", view");
        }
    }

    static class UpdatableBufferBitArray extends BufferBitArray implements UpdatableBitArray {
        UpdatableBufferBitArray(DataStorage storage, long initialCapacity, long initialLength,
                                long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        UpdatableBufferBitArray(DataStorage storage, long initialCapacity, long initialLength,
                                long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public final void setElement(long index, Object value) {
            setBit(index, (Boolean) value);
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length)
                throw rangeException(srcIndex);
            if (destIndex < 0 || destIndex >= length)
                throw rangeException(destIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.copy(offset + destIndex, offset + srcIndex);
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length)
                throw rangeException(firstIndex);
            if (secondIndex < 0 || secondIndex >= length)
                throw rangeException(secondIndex);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.swap(offset + firstIndex, offset + secondIndex);
        }

        public final void setDouble(long index, double value) {
            setBit(index, value != 0.0);
        }

        public final void setLong(long index, long value) {
            setBit(index, value != 0);
        }

        public final void setInt(long index, int value) {
            setBit(index, value != 0);
        }

        public final void setBit(long index, boolean value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setBit(offset + index, value);
        }

        public final void setBit(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setBit(offset + index, true);
        }

        public final void clearBit(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.setBit(offset + index, false);
        }

        public UpdatableBitArray fill(double value) {
            return fill(0, length, value);
        }

        public UpdatableBitArray fill(long position, long count, double value) {
            return fill(position, count, value != 0);
        }

        public UpdatableBitArray fill(long value) {
            return fill(0, length, value);
        }

        public UpdatableBitArray fill(long position, long count, long value) {
            return fill(position, count, value != 0);
        }

        public UpdatableBitArray fill(boolean value) {
            return fill(0, length, value);
        }

        public UpdatableBitArray fill(long position, long count, boolean value) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            storage.fillData(offset + position, count, value);
            return this;
        }

        public UpdatableBitArray subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw rangeException(fromIndex);
            if (toIndex > length)
                throw rangeException(toIndex - 1);
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                        + " > toIndex = " + toIndex + ") in " + getClass());
            UpdatableBufferBitArray result = new UpdatableBufferBitArray(storage,
                    toIndex - fromIndex, toIndex - fromIndex, offset + fromIndex,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public UpdatableBitArray subArr(long position, long count) {
            if (position < 0)
                throw rangeException(position);
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                        + ") in " + getClass());
            if (position > length - count)
                throw rangeException(position + count - 1);
            UpdatableBufferBitArray result = new UpdatableBufferBitArray(storage,
                    count, count, offset + position,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public final BitArray asImmutable() {
            return new BufferBitArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
        }

        public final boolean isImmutable() {
            return false;
        }

        public UpdatableArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite()) {
                return this;
            }
            UpdatableBufferBitArray result = new UpdatableBufferBitArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public UpdatableBitArray asUnresizable() {
            return this;
        }

        public UpdatableArray shallowClone() {
            return (UpdatableBufferBitArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "unresizable AlgART array bit[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }

    static final class MutableBufferBitArray extends UpdatableBufferBitArray implements MutableBitArray {
        MutableBufferBitArray(DataStorage storage, long initialCapacity, long initialLength,
                              long initialOffset, boolean doAllocate) {
            super(storage, initialCapacity, initialLength, initialOffset, doAllocate);
        }

        MutableBufferBitArray(DataStorage storage, long initialCapacity, long initialLength,
                              long initialOffset, AbstractBufferArray underlyingArray) {
            super(storage, initialCapacity, initialLength, initialOffset, underlyingArray);
        }

        public MutableBitArray length(long newLength) {
            if (length < 0)
                throw new IllegalArgumentException("Negative desired array length");
            lengthImpl(newLength);
            return this;
        }

        public MutableBitArray ensureCapacity(long minCapacity) {
            if (minCapacity < 0)
                throw new IllegalArgumentException("Negative desired array minimal capacity");
            ensureCapacityImpl(minCapacity);
            return this;
        }

        public MutableBitArray trim() {
            trimImpl();
            return this;
        }

        public MutableBitArray append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public Object popElement() {
            return popBit();
        }

        public void pushElement(Object value) {
            pushBit((Boolean) value);
        }

        public double popDouble() {
            return popBit() ? 1.0 : 0.0;
        }

        public long popLong() {
            return popBit() ? 1L : 0L;
        }

        public int popInt() {
            return popBit() ? 1 : 0;
        }

        public void add(double value) {
            pushBit(value != 0.0);
        }

        public void add(long value) {
            pushBit(value != 0);
        }

        public void add(int value) {
            pushBit(value != 0);
        }

        public boolean popBit() {
            if (length == 0)
                throw new EmptyStackException();
            boolean result = getBit(length - 1);
            this.length--; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            return result;
        }

        public void pushBit(boolean value) {
            long newLength = length + 1;
            if (newLength < 0) { // overflow
                assert newLength == Long.MIN_VALUE;
                throw new TooLargeArrayException("Too large desired array length (2^63)");
            }
            if (newLength > capacity) {
                ensureCapacityImpl(newLength);
            }
            this.length = newLength; // must be changed BEFORE reallocateStorage()
            if (copyOnNextWrite) {
                reallocateStorage();
            }
            setBit(newLength - 1, value);
        }

        public MutableBitArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableBitArray setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableBitArray copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableBitArray swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableBitArray asCopyOnNextWrite() {
            if (isCopyOnNextWrite())
                return this;
            MutableBufferBitArray result = new MutableBufferBitArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = true;
            return result;
        }

        public boolean isUnresizable() {
            return false;
        }

        public UpdatableBitArray asUnresizable() {
            UpdatableBufferBitArray result = new UpdatableBufferBitArray(storage, capacity, length, offset,
                    underlyingArray == null ? this : underlyingArray);
            result.copyOnNextWrite = copyOnNextWrite;
            return result;
        }

        public MutableBitArray shallowClone() {
            return (MutableBufferBitArray) super.shallowClone();
        }

        public String toString() {
            assert !isNewReadOnlyView();
            return "mutable AlgART array bit[" + length() + "], @<"
                    + storage + ">, capacity " + capacity()
                    + (offset == 0 ? "" : ", offset = " + offset)
                    + (isCopyOnNextWrite() ? ", copy on next write" : "")
                    + (isNew() ? ", new" : ", view");
        }
    }
}
