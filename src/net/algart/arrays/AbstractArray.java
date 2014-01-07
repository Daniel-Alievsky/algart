/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.zip.CRC32;
import java.util.Locale;
import java.nio.ByteOrder;

/**
 * <p>Implementation of basic functions of {@link MutableArray} interface.</p>
 *
 * <p>This class implements only {@link Array} interface and doesn't implement
 * {@link UpdatableArray} / {@link MutableArray}. (It is necessary to allow read-only arrays,
 * implementing {@link Array} only, to be inheritors of <tt>AbstractArray</tt>.
 * In other case, the user would be able to use illegal constructions as the following:</p>
 * <pre>
 * &nbsp;&nbsp;&nbsp;&nbsp;Array ar = some immutable array (inheritor of AbstractArray);
 * &nbsp;&nbsp;&nbsp;&nbsp;((AbstractArray)ar)set(0, newValue);
 * </pre>
 * <p>This package guarantees that such illegal array operations are <i>syntactically</i>
 * impossible.</p>
 *
 * <p>Instead of implementing full {@link MutableArray} interface,
 * this class provides default implementation for some methods of {@link UpdatableArray} / {@link MutableArray}
 * via separate protected static methods: {@link #defaultCopy(UpdatableArray, Array)},
 * {@link #defaultSwap(UpdatableArray, UpdatableArray)}, {@link #defaultAppend(MutableArray, Array)}.
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractArray implements Array, Cloneable {

    static final boolean DETAILED_PROFILING_COPY = false;

    private static final JArrayPool BOOLEAN_BUFFERS = JArrayPool.getInstance(boolean.class, 65536);

    /**
     * The current array's capacity.
     */
    protected long capacity;

    /**
     * The current array's length.
     */
    protected long length;

    /**
     * The underlying arrays passed by the last constructor argument.
     * It is the same reference, which was passed to the constructor (not its clone).
     */
    protected Array[] underlyingArrays;

    private volatile byte newAndNewReadOnlyViewStatus = 0; // bit #0 / #1 is new / new-read-only-view status

    /**
     * Creates an array with the given initial capacity and length.
     * Does not check the <tt>initialCapacity</tt> and <tt>initialLength</tt> arguments:
     * they may contain any values, for example, negative.
     * Some implementations may use bit #63 for service goals.
     *
     * <p>If the created array is a view of some another AlgART arrays,
     * as result of
     * {@link Arrays#asFuncArray(boolean, net.algart.math.functions.Func, Class, PArray...) Arrays.asFuncArray}
     * method, all these arrays should be passed by <tt>underlyingArrays</tt> argument.
     * The {@link Array#flushResources(ArrayContext)}, {@link Array#flushResources(ArrayContext, boolean)} and
     * {@link Array#freeResources(ArrayContext, boolean)} methods in the created instance,
     * and also in all its views created by
     * {@link #subArray(long, long)}, {@link #subArr(long, long)} or {@link #asImmutable()} methods,
     * will call the same methods of all underlying arrays specified by this argument.
     *
     * @param initialCapacity  initial capacity of the array.
     * @param initialLength    initial length of the array.
     * @param underlyingArrays the created arrays is a view of the arrays passed by this argument.
     * @throws NullPointerException if <tt>underlyingArrays</tt> is <tt>null</tt>.
     */
    protected AbstractArray(long initialCapacity, long initialLength, Array... underlyingArrays) {
        if (underlyingArrays == null)
            throw new NullPointerException("Null underlyingArrays argument");
        this.capacity = initialCapacity;
        this.length = initialLength;
        this.underlyingArrays = underlyingArrays;
    }

    /**
     * Equivalent to {@link #AbstractArray(long, long, Array...)
     * AbstractArray(initialCapacity, initialLength, new Array[0])}.
     *
     * @param initialCapacity initial capacity of the array.
     * @param initialLength   initial length of the array.
     */
    protected AbstractArray(long initialCapacity, long initialLength) {
        this.capacity = initialCapacity;
        this.length = initialLength;
        this.underlyingArrays = new Array[0];
    }

    /**
     * Equivalent to {@link #AbstractArray(long, long, Array...)
     * AbstractArray(initialCapacityAndLength, initialCapacityAndLength, new Array[0])}.
     *
     * @param initialCapacityAndLength initial capacity and length of the array.
     */
    protected AbstractArray(long initialCapacityAndLength) {
        this.capacity = this.length = initialCapacityAndLength;
        this.underlyingArrays = new Array[0];
    }

    public abstract Class<?> elementType();

    public abstract Class<? extends Array> type();

    public abstract Class<? extends UpdatableArray> updatableType();

    public abstract Class<? extends MutableArray> mutableType();

    /**
     * This implementation returns <tt>{@link #length} & Long.MAX_VALUE</tt>.
     * (The high bit of length may be used for service goals.)
     *
     * @return the number of elements in this array.
     */
    public long length() {
        return this.length & Long.MAX_VALUE;
    }

    /**
     * This implementation returns <tt>{@link #capacity} & Long.MAX_VALUE</tt>.
     * (The high bit of capacity may be used for service goals.)
     *
     * @return the capacity of this array.
     */
    public long capacity() {
        return this.capacity & Long.MAX_VALUE;
    }

    public abstract Object getElement(long index);

    public abstract void getData(long arrayPos, Object destArray, int destArrayOffset, int count);

    public abstract void getData(long arrayPos, Object destArray);

    /**
     * This implementation returns <tt>java.lang.reflect.Array.newInstance(elementType(),&nbsp;length)</tt>.
     *
     * @param length the length of created Java-array.
     * @return Java-array with the specified length and the same type of elements.
     * @throws NegativeArraySizeException if the specified <tt>length</tt> is negative.
     */
    public Object newJavaArray(int length) {
        return java.lang.reflect.Array.newInstance(elementType(), length);
    }

    /**
     * This method of {@link PArray} interface is fully implemented in this class.
     * If this instance does not implement {@link PArray} (i.e. if it is {@link ObjectArray}),
     * this method throws <tt>UnsupportedOperationException</tt>.
     *
     * @return <tt>true</tt> if and only if all elements of this array are zero, or if this array is empty.
     * @throws UnsupportedOperationException if this instance is an array of non-primitive elements.
     */
    public boolean isZeroFilled() {
        if (!(this instanceof PArray))
            throw new UnsupportedOperationException("isZeroFilled() must not be called for non-primitive arrays)");
        long n = length();
        if (n == 0)
            return true;
        Object ja = Arrays.javaArrayInternal(this);
        if (ja != null) {
            int offset = Arrays.javaArrayOffsetInternal(this);
            return JArrays.areElementsZero(ja, offset, (int) n);
        } else if (this instanceof BitArray) {
            DataBitBuffer buf = (DataBitBuffer) Arrays.bufferInternal(this, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    if (!PackedBitArrays.areBitsZero(buf.data(), buf.fromIndex(), buf.count()))
                        return false;
                }
            } finally {
                Arrays.dispose(buf);
            }
            return true;
        } else {
            DataBuffer buf = Arrays.bufferInternal(this, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    if (!JArrays.areElementsZero(buf.data(), buf.from(), buf.cnt()))
                        return false;
                }
            } finally {
                Arrays.dispose(buf);
            }
            return true;
        }
    }

    public abstract Array subArray(long fromIndex, long toIndex);

    /**
     * This implementation returns <tt>{@link #subArray(long, long) subArray}(position, position + count)</tt>.
     * Please note that the exception message can be not fully correct for the very exotic case
     * <tt>position+count&gt;Long.MAX_VALUE</tt>.
     *
     * @param position start position (inclusive) of the subarray.
     * @param count    number of elements in the subarray.
     * @return a view of the specified range within this array.
     * @throws IndexOutOfBoundsException for illegal position and count
     *                                   (position &lt; 0 || count &lt; 0 || position + count &gt; length).
     */
    public Array subArr(long position, long count) {
        return subArray(position, position + count);
    }

    /**
     * This method is fully implemented in this class.
     *
     * <p>The returned buffer will be <i><a href="DataBuffer.html#directAndIndirect">direct</a></i>,
     * if <tt>mode</tt> is not {@link DataBuffer.AccessMode#PRIVATE PRIVATE},
     * this array is not {@link Array#asImmutable() immutable},
     * is not {@link Array#asCopyOnNextWrite() copy-on-next-write},
     * and either it implements {@link DirectAccessible} interface and
     * its {@link DirectAccessible#hasJavaArray() hasJavaArray()} method returns <tt>true</tt>,
     * or it is a bit array created by the {@link SimpleMemoryModel simple memory model}.
     *
     * @param mode     the access mode for new buffer.
     * @param capacity the capacity of the buffer
     * @return new data buffer for accessing this array.
     * @throws NullPointerException     if <tt>mode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the <tt>mode</tt> is not the {@link DataBuffer.AccessMode#READ},
     *                                  but this arrays does not implement {@link UpdatableArray} interface,
     *                                  or if the specified <tt>capacity</tt> is negative or too high
     *                                  (&gt;=0..2<sup>37</sup> for bits or &gt;=0..2<sup>31</sup> for
     *                                  other element types).
     */
    public DataBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
        return defaultBuffer(this, mode, capacity);
    }

    /**
     * This implementation returns {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)
     * buffer(mode, someCapacity)}, where <tt>mode</tt> is the argument of this method
     * and <tt>someCapacity</tt> is the result of {@link #defaultBufferCapacity(Array)
     * defaultBufferCapacity(thisArray)} method.
     *
     * @param mode the access mode for new buffer.
     * @return new data buffer for accessing this array.
     * @throws NullPointerException     if <tt>mode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the <tt>mode</tt> is not the {@link DataBuffer.AccessMode#READ},
     *                                  but this arrays does not implement {@link UpdatableArray} interface.
     */
    public DataBuffer buffer(DataBuffer.AccessMode mode) {
        return buffer(mode, defaultBufferCapacity(this));
    }

    /**
     * This implementation returns {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)
     * buffer(suitableMode, capacity)}, where <tt>capacity</tt> is the argument of this method
     * and <tt>suitableMode</tt> is
     * <tt>this instanceof UpdatableArray ?
     * DataBuffer.AccessMode.READ_WRITE :
     * DataBuffer.AccessMode.READ</tt>.
     *
     * @param capacity the capacity of the buffer.
     * @return new data buffer for accessing this array.
     * @throws IllegalArgumentException if the specified <tt>capacity</tt> is negative or too high
     *                                  (&gt;=0..2<sup>37</sup> for bits or &gt;=0..2<sup>31</sup> for
     *                                  other element types).
     */
    public DataBuffer buffer(long capacity) {
        return buffer(this instanceof UpdatableArray ?
            DataBuffer.AccessMode.READ_WRITE :
            DataBuffer.AccessMode.READ,
            capacity);
    }

    /**
     * This implementation returns {@link #buffer(net.algart.arrays.DataBuffer.AccessMode)
     * buffer(suitableMode)}, where <tt>suitableMode</tt> is
     * <tt>this instanceof UpdatableArray ?
     * DataBuffer.AccessMode.READ_WRITE :
     * DataBuffer.AccessMode.READ</tt>.
     *
     * @return new data buffer for accessing this array.
     */
    public DataBuffer buffer() {
        return buffer(this instanceof UpdatableArray ?
            DataBuffer.AccessMode.READ_WRITE :
            DataBuffer.AccessMode.READ);
    }

    public abstract Array asImmutable();

    public abstract boolean isImmutable();

    public abstract Array asTrustedImmutable();

    public abstract void checkUnallowedMutation() throws UnallowedMutationError;

    public abstract Array asCopyOnNextWrite();

    public abstract boolean isCopyOnNextWrite();

    public abstract boolean isUnresizable();

    /**
     * This implementation returns a private boolean field, that is
     * <tt>false</tt> by default, but can be changed by protected
     * {@link #setNewStatus(boolean)} method.
     *
     * @return whether this array instance if <i>new</i>: a new object, allocated by some {@link MemoryModel}.
     */
    public boolean isNew() {
        return (this.newAndNewReadOnlyViewStatus & 0x1) != 0;
    }

    /**
     * This implementation returns a private boolean field, that is
     * <tt>false</tt> by default, but can be changed by protected
     * {@link #setNewReadOnlyViewStatus()} method.
     *
     * @return whether this array instance is a newly created <i>view</i> of some
     *         external data, providing <i>read-only</i> access to this data.
     */
    public boolean isNewReadOnlyView() {
        return (this.newAndNewReadOnlyViewStatus & 0x2) != 0;
    }

    /**
     * This implementation returns <tt>{@link #underlyingArrays}.length &gt; 0</tt>.
     * Please override this if the access to underlying arrays is very quick
     * (as for {@link Matrix#subMatrix(long[], long[])} or,
     * vice versa, if there no underlying arrays, but getting an element requires some calculation
     * (as for {@link Arrays#asIndexFuncArray(net.algart.math.functions.Func, Class, long)}).
     *
     * @return <tt>true</tt> if and only if {@link #underlyingArrays} is non-empty.
     */
    public boolean isLazy() {
        return underlyingArrays.length > 0;
    }

    /**
     * This implementation returns <tt>ByteOrder.nativeOrder()</tt>.
     *
     * @return <tt>ByteOrder.nativeOrder()</tt>
     */
    public ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * This implementation returns {@link #standardObjectClone()}.
     *
     * <p>Be careful: if you are extending this class to implement full {@link MutableArray} interface,
     * your should investigate, whether you need to override this method to provide real independence
     * of the length, start offset, capacity, copy-on-next-write and
     * other information about any array characteristics besides its elements.
     *
     * @return a shallow copy of this object.
     */
    public Array shallowClone() {
        return standardObjectClone();
    }

    /**
     * This implementation performs the following:
     * <tt>memoryModel.{@link MemoryModel#newArray(Array)
     * newArray}(thisArray).{@link UpdatableArray#copy(Array) copy}(thisArray)</tt>.
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @return a mutable copy of this array.
     * @throws NullPointerException   if the argument is <tt>null</tt>.
     * @throws UnsupportedElementTypeException
     *                                if <tt>thisArray.{@link Array#elementType()}</tt> is not supported
     *                                by the specified memory model.
     * @throws TooLargeArrayException if the {@link Array#length() length} of this array is too large
     *                                for this the specified memory model.
     */
    public MutableArray mutableClone(MemoryModel memoryModel) {
        if (memoryModel == null)
            throw new NullPointerException("Null memory model");
        return memoryModel.newArray(this).copy(this);
    }

    /**
     * This implementation performs the following:
     * <tt>memoryModel.{@link MemoryModel#newUnresizableArray(Array)
     * newUnresizableArray}(thisArray).{@link UpdatableArray#copy(Array) copy}(thisArray)</tt>.
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @return an updatable copy of this array.
     * @throws NullPointerException   if the argument is <tt>null</tt>.
     * @throws UnsupportedElementTypeException
     *                                if <tt>thisArray.{@link Array#elementType()}</tt> is not supported
     *                                by the specified memory model.
     * @throws TooLargeArrayException if the {@link Array#length() length} of this array is too large
     *                                for this the specified memory model.
     */
    public UpdatableArray updatableClone(MemoryModel memoryModel) {
        if (memoryModel == null)
            throw new NullPointerException("Null memory model");
        return memoryModel.newUnresizableArray(this).copy(this);
    }

    /**
     * This implementation does nothing. You need to override it to provide (in a case of subarray)
     * preloading correct part all underlying arrays, passed via the last argument
     * of the {@link #AbstractArray(long, long, Array...) constructor}.
     *
     * @param context the context of execution; may be <tt>null</tt>, then it will be ignored.
     */
    public void loadResources(ArrayContext context) {
    }

    /**
     * This implementation just calls {@link #flushResources(ArrayContext, boolean) flushResources(context, false)}.
     *
     * @param context the context of execution; may be <tt>null</tt>, then it will be ignored.
     */
    public final void flushResources(ArrayContext context) {
        flushResources(context, false);
    }

    /**
     * This implementation calls the same method with the same arguments
     * for all underlying arrays, passed via the last argument
     * of the {@link #AbstractArray(long, long, Array...) constructor}.
     * Please override it if you want to provide (in a case of subarray)
     * flushing correct part of the underlying arrays.
     *
     * @param context              the context of execution; may be <tt>null</tt>, then it will be ignored.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device.
     */
    public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        for (int k = 0; k < underlyingArrays.length; k++) {
            underlyingArrays[k].flushResources(
                context == null ? null : context.part(k, k + 1, underlyingArrays.length),
                forcePhysicalWriting);
        }
    }

    /**
     * This implementation just calls {@link #freeResources(ArrayContext, boolean) freeResources(context, false)}.
     *
     * @param context the context of execution; may be <tt>null</tt>, then it will be ignored.
     */
    public final void freeResources(ArrayContext context) {
        freeResources(context, false);
    }

    /**
     * This implementation calls the same method for all underlying arrays, passed via the last argument
     * of the {@link #AbstractArray(long, long, Array...) constructor}.
     * Please override it if you want to provide (in a case of subarray)
     * freeing correct part of the underlying arrays.
     *
     * @param context              the context of execution; may be <tt>null</tt>, then it will be ignored.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device.
     */
    public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        for (int k = 0; k < underlyingArrays.length; k++) {
            underlyingArrays[k].freeResources(
                context == null ? null : context.part(k, k + 1, underlyingArrays.length),
                forcePhysicalWriting);
        }
    }

    // abstract toString() method requires subclasses to override it
    @Override
    public abstract String toString();

    /**
     * This method is fully implemented in this class and cannot be overridden
     * (it is declared as <i>final</i> method here).
     *
     * <p>For non-primitive element type ({@link ObjectArray} subinterfaces),
     * this implementation is based on calls
     * of <tt>hashCode</tt> method of the class of elements ({@link #elementType()}).
     *
     * <p>This method is equivalent to the following call: {@link #hashCode(Array) hashCode(this)}.
     *
     * @return the hash code of this array.
     */
    @Override
    public final int hashCode() {
        return hashCode(this);
    }

    /**
     * This method is fully implemented in this class and is not be overridden usually.
     *
     * <p>For non-primitive element type ({@link ObjectArray} subinterface),
     * this implementation is based on calls
     * of <tt>equals</tt> method of the class of elements ({@link #elementType()}).
     *
     * <p>This method is equivalent to the following call:
     * {@link #equals(Array, Object) equals(this, obj)}.
     *
     * <p>In some cases, you may override this method to provide better performance
     * or to exclude dependence on elements' <tt>equals</tt> method.
     * (For example, this method is overridden in {@link CombinedMemoryModel combined arrays}.)
     *
     * @param obj the object to be compared for equality with this array.
     * @return <tt>true</tt> if the specified object is an AlgART array equal to this one.
     */
    @Override
    public boolean equals(Object obj) {
        return equals(this, obj);
    }

    /**
     * Default implementation of {@link Array#hashCode() Array.hashCode()} method.
     *
     * <p><i>All classes implementing {@link Array} interface, that do not extend
     * {@link AbstractArray}, must use this method or
     * fully equivalent algorithm to ensure correct arrays behavior.</i>
     * Classes that extend {@link AbstractArray} always inherit standard implementation
     * {@link AbstractArray#hashCode() AbstractArray.hashCode()}.
     *
     * @param array the AlgART array.
     * @return the hash code of this array.
     */
    public static int hashCode(Array array) {
        long n = array.length();
        if (n == 0)
            return 0;
        Object ja = Arrays.javaArrayInternal(array);
        if (ja != null) {
            int offset = Arrays.javaArrayOffsetInternal(array);
            return JArrays.arrayHashCode(ja, offset, offset + (int) n);
        } else if (array instanceof BitArray) {
            CRC32 hash = new CRC32();
            DataBitBuffer buf = (DataBitBuffer) Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    PackedBitArrays.updateBitHashCode(buf.data(), buf.fromIndex(), buf.toIndex(), hash);
                }
            } finally {
                Arrays.dispose(buf);
            }
            return (int) hash.getValue() * 37 + 28;
            // "*37+28" is because BitArray should produce another hash code than equivalent LongArray
        } else {
            CRC32 hash = new CRC32();
            DataBuffer buf = Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    JArrays.updateArrayHashCode(buf.data(), buf.from(), buf.to(), hash);
                }
            } finally {
                Arrays.dispose(buf);
            }
            return (int) hash.getValue();
        }
    }

    /**
     * Default implementation of {@link Array#equals(Object) Array.equals(Object)} method.
     *
     * <p><i>All classes implementing {@link Array} interface must use this method or
     * fully equivalent algorithm to ensure correct arrays behavior.</i>
     * Classes that extend {@link AbstractArray} may just inherit standard implementation
     * {@link AbstractArray#equals(Object) AbstractArray.equals(Object)}.
     *
     * @param obj1 first compared object.
     * @param obj2 second compared object.
     * @return <tt>true</tt> if <tt>obj2</tt> is a array and the specified arrays are equal.
     */
    public static boolean equals(Array obj1, Object obj2) {
        if (!(obj2 instanceof Array))
            return false;
        Array a = (Array) obj2;
        long n = obj1.length();
        if (a.length() != n)
            return false;
        if (obj1 instanceof PArray != a instanceof PArray)
            return false;
        if (obj1 instanceof PArray && a.elementType() != obj1.elementType())
            return false;
        Object ja1 = obj1 instanceof AbstractArray ? ((AbstractArray) obj1).javaArrayInternal() : null;
        Object ja2 = a instanceof AbstractArray ? ((AbstractArray) a).javaArrayInternal() : null;
        if (ja1 != null && ja2 != null)
            return JArrays.arrayEquals(ja1, ((AbstractArray) obj1).javaArrayOffsetInternal(),
                ja2, ((AbstractArray) a).javaArrayOffsetInternal(), (int) n);
        if (n == 0)
            return true;
        if (obj1 instanceof BitArray) {
            DataBitBuffer buf1 = (DataBitBuffer) Arrays.bufferInternal(obj1, DataBuffer.AccessMode.READ,
                largeBufferCapacity(obj1), true);
            DataBitBuffer buf2 = (DataBitBuffer) Arrays.bufferInternal(a, DataBuffer.AccessMode.READ,
                buf1.capacity(), true);
            try {
                Arrays.enableCaching(buf1);
                Arrays.enableCaching(buf2);
                for (buf1.map(0), buf2.map(0); buf1.hasData(); buf1.mapNext(), buf2.mapNext()) {
                    assert buf1.count() == buf2.count();
                    if (!PackedBitArrays.bitEquals(buf1.data(), buf1.fromIndex(),
                        buf2.data(), buf2.fromIndex(), buf1.count()))
                        return false;
                }
            } finally {
                Arrays.dispose(buf1);
                Arrays.dispose(buf2);
            }
        } else {
            DataBuffer buf1 = Arrays.bufferInternal(obj1, DataBuffer.AccessMode.READ,
                largeBufferCapacity(obj1), // its better to use large blocks while scanning
                true);
            DataBuffer buf2 = Arrays.bufferInternal(a, DataBuffer.AccessMode.READ,
                buf1.capacity(), true);
            try {
                Arrays.enableCaching(buf1);
                Arrays.enableCaching(buf2);
                for (buf1.map(0), buf2.map(0); buf1.hasData(); buf1.mapNext(), buf2.mapNext()) {
                    assert buf1.count() == buf2.count();
                    if (!JArrays.arrayEquals(buf1.data(), buf1.from(), buf2.data(), buf2.from(), buf1.cnt()))
                        return false;
                }
            } finally {
                Arrays.dispose(buf1);
                Arrays.dispose(buf2);
            }

        }
        return true;
    }

    /**
     * Returns the data buffer capacity used by {@link #buffer(net.algart.arrays.DataBuffer.AccessMode)}
     * and {@link #buffer()} methods. The result is calculated so that
     * the buffer occupies ~16-32 KB RAM.
     *
     * <p>In any case, you can be sure that the result will not be greater than <tt>Integer.MAX_VALUE-64</tt>.
     *
     * @param thisArray this array.
     * @return default data buffer capacity, suitable for this array.
     */
    public static int defaultBufferCapacity(Array thisArray) {
        int result;
        if (thisArray instanceof BitArray) {
            result = 8 * 32768;
            // 32 KB: for bit arrays, it may be better choice than 16 KB
            // because it essentially reduce the time for loop organization
            // (most loops work with long[] array, and 32 KB means only 4096 longs)
        } else {
            int bitsPerElement = thisArray instanceof PArray ?
                (int) Math.min(1024, ((PArray) thisArray).bitsPerElement()) :
                32;
            if (bitsPerElement == -1) {
                bitsPerElement = 32;
            }
            result = Math.max(8 * 16384 / bitsPerElement, 1024);
            // 16 KB: suitable size for most processors
        }
        assert result <= largeBufferCapacity(thisArray) : "defaultBufferCapacity / largeBufferCapacity mismatch";
        return result;
    }

    /**
     * Checks whether the passed arguments correct for
     * {@link UpdatableArray#copy(Array)} and throws corresponding exception if no.
     * More precisely, this method throws <tt>IllegalArgumentException</tt>
     * if is not possible to assign elements of <tt>src</tt> array to elements of <tt>thisArray</tt>,
     * i.e. if <tt>!thisArray.{@link #elementType() elementType()}.isAssignableFrom(src.{@link #elementType()
     * elementType()})</tt>.
     *
     * <p>Note: for primitive array types ({@link PArray} and its inheritors), this method throws an exception
     * if and only if <nobr><tt>thisArray.{@link #elementType() elementType()}!=src.{@link #elementType()
     * elementType()}</tt></nobr>.
     *
     * @param thisArray this array.
     * @param src       the source array.
     * @throws NullPointerException     if <tt>thisArray</tt> or <tt>src</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     */
    public static void checkCopyArguments(UpdatableArray thisArray, Array src) {
        if (thisArray == null)
            throw new NullPointerException("Null thisArray argument");
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (!thisArray.elementType().isAssignableFrom(src.elementType()))
            throw new IllegalArgumentException("Element types mismatch ("
                + thisArray.elementType() + " cannot be assigned from " + src.elementType() + ")");
    }

    /**
     * Checks whether the passed arguments correct for
     * {@link UpdatableArray#swap(UpdatableArray)} and throws corresponding exception if no.
     * More precisely, this method throws <tt>IllegalArgumentException</tt>
     * if <tt>another</tt> array <tt>thisArray</tt> have different element types,
     * i.e. if <tt>thisArray.{@link #elementType() elementType()}!=another.{@link #elementType()
     * elementType()}</tt>.
     *
     * @param thisArray this array.
     * @param another   another array.
     * @throws NullPointerException     if <tt>thisArray</tt> or <tt>src</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if another and this element types do not match.
     */
    public static void checkSwapArguments(UpdatableArray thisArray, UpdatableArray another) {
        if (thisArray == null)
            throw new NullPointerException("Null thisArray argument");
        if (another == null)
            throw new NullPointerException("Null another argument");
        if (thisArray.elementType() != another.elementType())
            throw new IllegalArgumentException("Element types mismatch ("
                + thisArray.elementType() + " and " + another.elementType() + ")");
    }

    /**
     * Performs default <tt>Object.clone()</tt> call
     * and clears in its result both {@link Array#isNew() <i>new</i> status}
     * and {@link Array#isNewReadOnlyView() <i>new-read-only-view</i> status}.
     * In other words, it is a usual clone with the only difference,
     * that {@link Array#isNew()} and {@link Array#isNewReadOnlyView()} methods returns <tt>false</tt>
     * in the returned object. (It is a requirement of the contracts of these methods.)
     *
     * <p>The call of this method is a suitable implementation of
     * {@link #shallowClone()} method for most cases.
     *
     * <p>Please note that this method clones a reference {@link #underlyingArrays} and does not tries
     * to create a clone of this Java array.
     *
     * @return result of default <tt>Object.clone()</tt>
     */
    protected final AbstractArray standardObjectClone() {
        try {
            AbstractArray result = (AbstractArray) super.clone();
            result.newAndNewReadOnlyViewStatus = 0;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * Checks whether the passed arguments correct for {@link #subArray(long, long)}
     * and throws corresponding exception if no.
     *
     * @param fromIndex low endpoint (inclusive) of the subarray.
     * @param toIndex   high endpoint (exclusive) of the subarray.
     * @throws IndexOutOfBoundsException for illegal <tt>fromIndex</tt> and <tt>toIndex</tt>
     *                                   (<tt>fromIndex &lt; 0 || toIndex &gt; {@link #length()}
     *                                   || fromIndex &gt; toIndex</tt>).
     */
    protected final void checkSubArrayArguments(long fromIndex, long toIndex) {
        if (fromIndex < 0)
            throw rangeException(fromIndex, length(), getClass());
        if (toIndex > length())
            throw rangeException(toIndex - 1, length(), getClass());
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                + " > toIndex = " + toIndex + ") in " + getClass());
    }

    /**
     * Checks whether the passed arguments correct for {@link #subArr(long, long)}
     * and throws corresponding exception if no.
     *
     * @param position start position (inclusive) of the subarray.
     * @param count    number of elements in the subarray.
     * @throws IndexOutOfBoundsException for illegal <tt>position</tt> and <tt>count</tt>
     *                                   (<tt>position &lt; 0 || count &lt; 0
     *                                   || position + count &gt; {@link #length()}</tt>).
     */
    protected final void checkSubArrArguments(long position, long count) {
        if (position < 0)
            throw rangeException(position, length(), getClass());
        if (count < 0)
            throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                + ") in " + getClass());
        if (position > length() - count)
            throw rangeException(position + count - 1, length(), getClass());
    }


    /**
     * Sets the value of the private boolean field returned by {@link #isNew()} method.
     *
     * <p>This method is called with <tt>true</tt> argument while allocating new AlgART array
     * and reallocating internal storage, for example, in {@link #asCopyOnNextWrite()
     * copy-on-next-write} arrays.
     * This method is called with <tt>false</tt> argument in {@link UpdatableArray#setNonNew()}
     * method only.
     * In your inheritors of this class, you <i>must not</i> provide an ability (via public methods)
     * to call this method with <tt>true</tt> argument.
     *
     * <p>If the argument is <tt>false</tt>, this instance must implement {@link UpdatableArray}
     * interface. In other case, <tt>UnsupportedOperationException</tt> will be thrown.
     *
     * <p>The access to the "new status", provided by this and {@link #isNew()} method,
     * is always internally synchronized (the corresponding private field is <tt>volatile</tt>).
     * So, changing "new-read-only-view status" by this method
     * will be immediately visible in all threads using this instance.
     *
     * @param value new "new status".
     * @throws UnsupportedOperationException if the argument is <tt>false</tt> and this instance does not
     *                                       implement {@link UpdatableArray} interface.
     */
    protected final void setNewStatus(boolean value) {
        if (!(this instanceof UpdatableArray))
            throw new UnsupportedOperationException("setNewStatus(boolean) can be called for updatable arrays only");
        assert this.newAndNewReadOnlyViewStatus < 2 : "new read-only view is impossible for UpdatableArray";
        this.newAndNewReadOnlyViewStatus = value ? (byte) 0x1 : 0;
    }

    /**
     * Sets to <tt>true</tt> the value of the private boolean field returned by {@link #isNewReadOnlyView()} method.
     *
     * <p>This method is called creating a view of external data,
     * providing <i>read-only</i> access to this data. In this package, it is called only by
     * {@link LargeMemoryModel#asArray(Object, Class, long, long, java.nio.ByteOrder)} method,
     * its strict equivalents
     * {@link LargeMemoryModel#asBitArray asBitArray},
     * {@link LargeMemoryModel#asCharArray asCharArray}, etc. and by
     * {@link LargeMemoryModel#asMatrix(Object, MatrixInfo)} method,
     * and only in a case when the data offset, specified while calling these methods,
     * is zero.
     *
     * <p>In your inheritors of this class, you <i>must not</i> provide an ability (via public methods)
     * to call this method.
     *
     * <p>If the argument is <tt>false</tt>, this instance must implement {@link UpdatableArray}
     * interface. In other case, <tt>UnsupportedOperationException</tt> will be thrown.
     *
     * <p>The access to the "new-read-only-view status", provided by this and {@link #isNewReadOnlyView()} method,
     * is always internally synchronized (the corresponding private field is <tt>volatile</tt>).
     * So, changing "new-read-only-view status" by this method
     * will be immediately visible in all threads using this instance.
     *
     * @throws UnsupportedOperationException if this instance implements {@link UpdatableArray} interface.
     */
    protected final void setNewReadOnlyViewStatus() {
        if (this instanceof UpdatableArray)
            throw new UnsupportedOperationException("setNewReadOnlyViewStatus() "
                + "must not be called for updatable arrays");
        assert (this.newAndNewReadOnlyViewStatus & 0x1) == 0 : "new status is possible only in UpdatableArray";
        this.newAndNewReadOnlyViewStatus = 0x2;
    }

    /**
     * Default implementation of {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)} method.
     *
     * @param thisArray this array.
     * @param mode      the access mode for new buffer.
     * @param capacity  the capacity of the buffer
     * @return new data buffer for accessing this array.
     * @throws NullPointerException     if <tt>mode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the <tt>mode</tt> is not the {@link DataBuffer.AccessMode#READ},
     *                                  but this arrays does not implement {@link UpdatableArray} interface,
     *                                  or if the specified <tt>capacity</tt> is negative or too high
     *                                  (&gt;=0..2<sup>37</sup> for bits or &gt;=0..2<sup>31</sup> for
     *                                  other element types).
     */
    protected static DataBuffer defaultBuffer(Array thisArray, DataBuffer.AccessMode mode, long capacity) {
        return Arrays.bufferInternal(thisArray, mode, capacity, false);
    }

    /**
     * Equivalent to {@link #defaultCopy(UpdatableArray, Array, boolean)
     * defaultCopy(thisArray, src, false)}.
     *
     * @param thisArray this array.
     * @param src       the source array.
     * @throws NullPointerException     if <tt>thisArray</tt> or <tt>src</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     */
    protected static void defaultCopy(UpdatableArray thisArray, Array src) {
        defaultCopy(thisArray, src, false);
    }

    /**
     * Possible implementation of {@link UpdatableArray#copy(Array)}
     * based on {@link Array#getData(long, Object)} and {@link UpdatableArray#setData(long, Object)}
     * methods (for some temporary array).
     * Usually called in {@link UpdatableArray#copy(Array)} method in a case
     * when <tt>src.getClass() != this.getClass()</tt>.
     *
     * <p>If <tt>allowNulls</tt> argument is <tt>true</tt>, then this method,
     * in addition to standard behavior, always allows to copy <tt>src</tt>
     * when it is result of
     * Arrays.{@link Arrays#nObjectCopies(long, Object) nObjectCopies(..., null)}
     * and when <tt>thisArray</tt> is {@link UpdatableObjectArray}.
     * (The element type of such source array is <tt>Object</tt> and usually cannot be assigned
     * to element type of <tt>thisArray</tt>.)
     *
     * @param thisArray  this array.
     * @param src        the source array.
     * @param allowNulls whether this method should always allow filling object array by <tt>null</tt>.
     * @throws NullPointerException     if <tt>thisArray</tt> or <tt>src</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     */
    protected static void defaultCopy(UpdatableArray thisArray, Array src, boolean allowNulls) {
        boolean nullFilled = thisArray instanceof UpdatableObjectArray<?>
            && src instanceof CopiesArraysImpl.CopiesObjectArray<?>
            && ((CopiesArraysImpl.CopiesObjectArray<?>) src).element == null;
        if (!(allowNulls && nullFilled)) {
            checkCopyArguments(thisArray, src);
        } // in other case, thisArray and src are not null (they are ObjectArray), so we don't need to check null
        if (ArraysSubMatrixCopier.copySubMatrixArray(null, thisArray, src)) {
            return;
        }
        defaultCopyWithoutOptimizations(thisArray, src);
    }

    /**
     * Possible implementation of {@link UpdatableArray#swap(UpdatableArray)}
     * based on {@link UpdatableArray#getData(long, Object)} and {@link UpdatableArray#setData(long, Object)}
     * methods (for some temporary array).
     * Usually called in <tt>swap</tt> method in a case
     * when <tt>src.getClass() != this.getClass()</tt>.
     *
     * @param thisArray this array.
     * @param another   another array.
     * @throws NullPointerException     if <tt>thisArray</tt> or <tt>another</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if another and this element types do not match.
     */
    protected static void defaultSwap(UpdatableArray thisArray, UpdatableArray another) {
        checkSwapArguments(thisArray, another);
        long len = Math.min(thisArray.length(), another.length());
        if (len > 0) {
            int buffLen = (int) Math.min(len, 32768);
            Object buff1 = java.lang.reflect.Array.newInstance(thisArray.elementType(), buffLen);
            Object buff2 = java.lang.reflect.Array.newInstance(thisArray.elementType(), buffLen);
            for (long disp = 0; disp < len; disp += buffLen) {
                if (buffLen > len - disp)
                    buffLen = (int) (len - disp);
                thisArray.getData(disp, buff1, 0, buffLen);
                another.getData(disp, buff2, 0, buffLen);
                thisArray.setData(disp, buff2, 0, buffLen);
                another.setData(disp, buff1, 0, buffLen);
            }
        }
    }

    /**
     * Simplest implementation of {@link MutableArray#append(Array)},
     * based on {@link MutableArray#length(long)}, {@link UpdatableArray#subArr(long, long)}
     * and {@link UpdatableArray#copy(Array)}
     * methods.
     *
     * @param thisArray     this array.
     * @param appendedArray appended array.
     * @return this array.
     * @throws NullPointerException     if <tt>thisArray</tt> or <tt>appendedArray</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the source and this element types do not match.
     * @throws TooLargeArrayException   if the resulting array length is too large for this type of arrays.
     */
    protected static MutableArray defaultAppend(MutableArray thisArray, Array appendedArray) {
        long currentLength = thisArray.length();
        long appendedLength = appendedArray.length();
        if (appendedLength > 0) {
            Array appended = appendedArray.shallowClone(); // allows correctly appending an array to itself
            Arrays.lengthUnsigned(thisArray, currentLength + appendedLength);
            thisArray.subArr(currentLength, appendedLength).copy(appended);
        }
        return thisArray;
    }

    static void defaultCopyWithoutOptimizations(UpdatableArray thisArray, Array src) {
        long t0 = nanoTime();
        long t1 = -1, t2 = -1, t3 = -1, tt1, tt2, copyCount = 0, tRead = 0, tWrite = 0;
        if (thisArray.length() < src.length()) {
            // correction src, but not dest: below we perform a loop on src
            src = src.subArr(0, thisArray.length());
        }
        if (thisArray instanceof BitArray) {
            if (((BitArray) src).nextQuickPosition(0) == -1 || thisArray.isLazy()) {
                boolean[] buf = (boolean[]) BOOLEAN_BUFFERS.requestArray();
                t1 = nanoTime();
                try {
                    for (long p = 0, n = src.length(); p < n; ) {
                        tt1 = nanoTime();
                        int len = (int) Math.min(n - p, buf.length);
                        src.getData(p, buf, 0, len);
                        tRead += (tt2 = nanoTime()) - tt1;
                        thisArray.setData(p, buf, 0, len);
                        tWrite += nanoTime() - tt2;
                        p += len;
                    }
                    t2 = nanoTime();
                } finally {
                    BOOLEAN_BUFFERS.releaseArray(buf);
                }
            } else {
                long[] jaDest = Arrays.longJavaArrayInternal((BitArray) thisArray);
                if (jaDest != null) {
                    // Popular case: for example, src is a lazy array copied into quick Java array.
                    // Here src is probably not direct (because the copy method from SimpleArraysImpls is not used).
                    long jaOffset = Arrays.longJavaArrayOffsetInternal((BitArray) thisArray);
                    t1 = nanoTime();
                    ((BitArray) src).getBits(0, jaDest, jaOffset, src.length());
                    tRead += (t2 = nanoTime()) - t1;
                } else {
                    UpdatableBitArray dest = (UpdatableBitArray) thisArray;
                    DataBitBuffer srcBuf = (DataBitBuffer) Arrays.bufferInternal(src,
                        DataBuffer.AccessMode.READ, largeBufferCapacity(src), true);
                    Arrays.enableCaching(srcBuf);
                    t1 = nanoTime();
                    try {
                        srcBuf.map(0);
                        tRead = nanoTime() - t1;
                        for (; srcBuf.hasData(); copyCount++) {
                            tt1 = nanoTime();
                            dest.setBits(srcBuf.position(), srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                            tWrite += (tt2 = nanoTime()) - tt1;
                            srcBuf.mapNext();
                            tRead += nanoTime() - tt2;
                        }
                        t2 = nanoTime();
                    } finally {
                        Arrays.dispose(srcBuf);
                    }
                }
            }
        } else {
            Object jaDest = Arrays.javaArrayInternal(thisArray);
            if (jaDest != null) {
                // Popular case: for example, src is a lazy array copied into quick Java array.
                // Here src is probably not direct (because the copy method from SimpleArraysImpls is not used).
                int jaOffset = Arrays.javaArrayOffsetInternal(thisArray);
                t1 = nanoTime();
                src.getData(0, jaDest, jaOffset, (int) src.length());
                tRead += (t2 = nanoTime()) - t1;
            } else {
                DataBuffer srcBuf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ,
                    largeBufferCapacity(thisArray), // its better to use large blocks while copying
                    true);
                Arrays.enableCaching(srcBuf);
                t1 = nanoTime();
                try {
                    srcBuf.map(0);
                    tRead = nanoTime() - t1;
                    for (; srcBuf.hasData(); copyCount++) {
                        tt1 = nanoTime();
                        thisArray.setData(srcBuf.position(), srcBuf.data(), srcBuf.from(), srcBuf.cnt());
                        tWrite += (tt2 = nanoTime()) - tt1;
                        srcBuf.mapNext();
                        tRead += nanoTime() - tt2;
                    }
                    t2 = nanoTime();
                } finally {
                    Arrays.dispose(srcBuf);
                }
            }
        }
        t3 = nanoTime();
        if (DETAILED_PROFILING_COPY && t1 != -1 && t3 - t1 > 100 * 1000 * 1000 // 100 ms
            && Arrays.SystemSettings.profilingMode())
        {
            Arrays.LOGGER.config(String.format(Locale.US, "AbstractArray.defaultCopy: "
                + "%.3f ms = %.3f ms init + %.3f ms reading + %.3f ms writing + %.3f dispose "
                + "[%d iterations, %d underlying, %s -> %s]",
                1e-6 * (t3 - t0), 1e-6 * (t1 - t0),
                1e-6 * tRead, 1e-6 * tWrite, 1e-6 * (t3 - t2),
                copyCount, Arrays.getUnderlyingArraysCount(src), src, thisArray));
            long t4 = System.nanoTime();
            if (t4 - t3 > 2 * 1000 * 1000) { // 2 ms
                Arrays.LOGGER.config(String.format(Locale.US, "AbstractArray.defaultCopy: "
                    + "+ %.3f ms or more for this logging", 1e-6 * (t4 - t3)));
            }
        }
    }

    /**
     * Returns alternate buffer capacity for essentially large buffers,
     * recommended for using with {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)}
     * and {@link #buffer(long)} methods. The result is calculated so that
     * the buffer occupies ~32-64 KB RAM.
     *
     * <p>Please note that this package optimizes creating buffers with this or less capacity,
     * but not do this for larger buffers.
     *
     * @param thisArray this array.
     * @return alternate large data buffer capacity, suitable for this array.
     */
    static int largeBufferCapacity(Array thisArray) {
        if (thisArray instanceof BitArray) {
            return DataBuffersImpl.MAX_BUFFER_SIZE * 8;
        } else {
            int bitsPerElement = thisArray instanceof PArray ?
                (int) Math.min(1024, ((PArray) thisArray).bitsPerElement()) :
                32;
            if (bitsPerElement == -1) {
                bitsPerElement = 32;
            }
            int maxBufferSizeInBits = bitsPerElement == 64 ?
                DataBuffersImpl.MAX_BUFFER_SIZE * 16 :
                DataBuffersImpl.MAX_BUFFER_SIZE * 8;
            return maxBufferSizeInBits / bitsPerElement;
        }
    }

    /**
     * Provides package-private access to the backed array even for immutable arrays.
     * Returns <tt>null</tt> if the is no backed array (not throws an exception).
     *
     * @return the same result as {@link DirectAccessible#javaArray()} method, but also for immutable arrays.
     */
    Object javaArrayInternal() {
        return null;
    }

    /**
     * Provides package-private access to the offset in the backed array even for immutable arrays.
     * For copy-on-next-write arrays, does not clone data.
     *
     * @return the same result as {@link DirectAccessible#javaArrayOffset()} method, but also for immutable arrays.
     */
    int javaArrayOffsetInternal() {
        return 0;
    }

    final <T extends Array> T setNewStatus() {
        // little faster than the full setNewStatus(boolean) - for local usage only
        this.newAndNewReadOnlyViewStatus = 0x1;
        return InternalUtils.<T>cast(this);
    }

    IndexOutOfBoundsException rangeException(long index) {
        // throw keyword in implementations, instead of calling a method that throws an exception,
        // can improve HotSpot optimization
        return rangeException(index, length, getClass());
    }

    static IndexOutOfBoundsException rangeException(long index, long length, Class<?> clazz) {
        return new IndexOutOfBoundsException("Index (" + index
            + (index < 0 ? ") < 0" : ") >= length (" + length + ")")
            + " in " + clazz.getName() + " class");
    }

    private static long nanoTime() {
        return DETAILED_PROFILING_COPY ? System.nanoTime() : -1;
    }
}
