/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.math.functions.Func;

import java.nio.ByteOrder;

/**
 * <p>Implementations of <tt>Arrays.nXxxCopies</tt> arrays.
 *
 * @author Daniel Alievsky
 */
class CopiesArraysImpl {
    /**
     * This interface is a marker informing that all elements of this array always have the same value.
     */
    interface CopiesArray {
    }

    /*Repeat.SectionStart copies_array*/
    static class CopiesFloatArray implements FloatArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final float element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesFloatArray(long length, float element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesFloatArray(long length, float element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillFloatArray((float[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            float[] dest = (float[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillFloatArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new float[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesFloatArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesFloatArray(count, element);
        }

        public DataFloatBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataFloatBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataFloatBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataFloatBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataFloatBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public FloatArray asImmutable() {
            return this;
        }

        public FloatArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesFloatArray(this.length, this.element);
        }

        public MutableFloatArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableFloatArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableFloatArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableFloatArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_FLOAT;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (double)element;
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == (float)value ? indexOf(lowIndex, highIndex, (float)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (float)value ? lastIndexOf(lowIndex, highIndex, (float)value) : -1;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (long)element;
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (int)element;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == (float)value ? indexOf(lowIndex, highIndex, (float)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (float)value ? lastIndexOf(lowIndex, highIndex, (float)value) : -1;
        }

        public float getFloat(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of float value " + element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FloatArray))
                return false;
            Array a = (FloatArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty float arrays are equal
            if (a instanceof CopiesFloatArray) {
                float e = ((CopiesFloatArray)a).element;
                return Float.floatToIntBits(e) == Float.floatToIntBits(element);
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.SectionEnd copies_array*/

        public double minPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint;
        }

        public double maxPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint;
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      value\s*==\s*\(float\)value ==> value == 0 || value == 1 ;;
      ndexOf\((long.*?)\(float\)value\) ==> ndexOf($1value != 0) ;;
      private\s+(final) ==> $1 ;;
      (?<!fill)FloatArray ==> BitArray ;;
      FloatBuffer ==> BitBuffer ;;
      return\s+\(double\)element ==> return element ? 1.0 : 0.0 ;;
      return\s+\((int|long)\)element ==> return element ? 1 : 0 ;;
      element\s*!=\s*0 ==> element ;;
      element\s*==\s*0 ==> !element ;;
      Float\.floatToIntBits\((.*?)\) ==> $1 ;;
      getFloat ==> getBit ;;
      PER_FLOAT ==> PER_BIT ;;
      Float(?!ing) ==> Boolean ;;
      float ==> boolean
         !! Auto-generated: NOT EDIT !! */
    static class CopiesBitArray implements BitArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        final long length;
        final boolean element;
        final Func f;
        final boolean truncateOverflows;

        CopiesBitArray(long length, boolean element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesBitArray(long length, boolean element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillBooleanArray((boolean[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            boolean[] dest = (boolean[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillBooleanArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new boolean[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || !element;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesBitArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesBitArray(count, element);
        }

        public DataBitBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataBitBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataBitBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataBitBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataBitBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public BitArray asImmutable() {
            return this;
        }

        public BitArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesBitArray(this.length, this.element);
        }

        public MutableBitArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableBitArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableBitArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableBitArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_BIT;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element ? 1.0 : 0.0;
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == 0 || value == 1 ? indexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == 0 || value == 1 ? lastIndexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element ? 1 : 0;
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element ? 1 : 0;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == 0 || value == 1 ? indexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == 0 || value == 1 ? lastIndexOf(lowIndex, highIndex, value != 0) : -1;
        }

        public boolean getBit(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of boolean value " + element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BitArray))
                return false;
            Array a = (BitArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty boolean arrays are equal
            if (a instanceof CopiesBitArray) {
                boolean e = ((CopiesBitArray)a).element;
                return e == element;
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

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

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            PackedBitArrays.fillBits(destArray, destArrayOffset, count, this.element);
        }

        public long nextQuickPosition(long position) {
            return position >= length ? -1 : position < 0 ? 0 : position;
        }
    }


    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      Float\.floatToIntBits\((.*?)\) ==> $1 ;;
      \bFloat\b ==> Character ;;
      Float(?!ing) ==> Char ;;
      float ==> char ;;
      PER_FLOAT ==> PER_CHAR ;;
      value \"\s*\+\s*element ==> value (char)" + (int)element
         !! Auto-generated: NOT EDIT !! */
    static class CopiesCharArray implements CharArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final char element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesCharArray(long length, char element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesCharArray(long length, char element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillCharArray((char[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            char[] dest = (char[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillCharArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new char[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesCharArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesCharArray(count, element);
        }

        public DataCharBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataCharBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataCharBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataCharBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataCharBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public CharArray asImmutable() {
            return this;
        }

        public CharArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesCharArray(this.length, this.element);
        }

        public MutableCharArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableCharArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableCharArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableCharArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_CHAR;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (double)element;
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == (char)value ? indexOf(lowIndex, highIndex, (char)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (char)value ? lastIndexOf(lowIndex, highIndex, (char)value) : -1;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (long)element;
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (int)element;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == (char)value ? indexOf(lowIndex, highIndex, (char)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (char)value ? lastIndexOf(lowIndex, highIndex, (char)value) : -1;
        }

        public char getChar(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of char value (char)" + (int)element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CharArray))
                return false;
            Array a = (CharArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty char arrays are equal
            if (a instanceof CopiesCharArray) {
                char e = ((CopiesCharArray)a).element;
                return e == element;
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

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
            return 0xFFFF;
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      value\s*==\s*\(float\)value ==> value == ((int)value & 0xFF) ;;
      Float\.floatToIntBits\((.*?)\) ==> $1 ;;
      float\s+getFloat ==> int getByte ;;
      Float(?!ing) ==> Byte ;;
      float ==> byte ;;
      PER_FLOAT ==> PER_BYTE ;;
      (return\s+element)\s*; ==> $1 & 0xFF; ;;
      (return\s+(?:\(long\)|\(double\)))element\s*; ==> $1(element & 0xFF); ;;
      (return\s+\(int\))element\s*; ==> return element & 0xFF; ;;
      value \"\s*\+\s*element ==> value " + element
                + (element >= 0 ? "" : "=(byte)" + (element & 0xFF))
         !! Auto-generated: NOT EDIT !! */
    static class CopiesByteArray implements ByteArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final byte element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesByteArray(long length, byte element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesByteArray(long length, byte element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillByteArray((byte[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            byte[] dest = (byte[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillByteArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element & 0xFF; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new byte[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesByteArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesByteArray(count, element);
        }

        public DataByteBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataByteBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataByteBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataByteBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataByteBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public ByteArray asImmutable() {
            return this;
        }

        public ByteArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesByteArray(this.length, this.element);
        }

        public MutableByteArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableByteArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableByteArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableByteArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_BYTE;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (double)(element & 0xFF);
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == ((int)value & 0xFF) ? indexOf(lowIndex, highIndex, (byte)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == ((int)value & 0xFF) ? lastIndexOf(lowIndex, highIndex, (byte)value) : -1;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (long)(element & 0xFF);
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element & 0xFF;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == ((int)value & 0xFF) ? indexOf(lowIndex, highIndex, (byte)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == ((int)value & 0xFF) ? lastIndexOf(lowIndex, highIndex, (byte)value) : -1;
        }

        public int getByte(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element & 0xFF;
        }

        public long indexOf(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of byte value " + element
                + (element >= 0 ? "" : "=(byte)" + (element & 0xFF));
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteArray))
                return false;
            Array a = (ByteArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty byte arrays are equal
            if (a instanceof CopiesByteArray) {
                byte e = ((CopiesByteArray)a).element;
                return e == element;
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

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
            return 0xFF;
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      value\s*==\s*\(float\)value ==> value == ((int)value & 0xFFFF) ;;
      Float\.floatToIntBits\((.*?)\) ==> $1 ;;
      float\s+getFloat ==> int getShort ;;
      Float(?!ing) ==> Short ;;
      float ==> short ;;
      PER_FLOAT ==> PER_SHORT ;;
      (return\s+element)\s*; ==> $1 & 0xFFFF; ;;
      (return\s+(?:\(long\)|\(double\)))element\s*; ==> $1(element & 0xFFFF); ;;
      (return\s+\(int\))element\s*; ==> return element & 0xFFFF; ;;
      value \"\s*\+\s*element ==> value " + element
                + (element >= 0 ? "" : "=(short)" + (element & 0xFFFF))
         !! Auto-generated: NOT EDIT !! */
    static class CopiesShortArray implements ShortArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final short element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesShortArray(long length, short element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesShortArray(long length, short element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillShortArray((short[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            short[] dest = (short[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillShortArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element & 0xFFFF; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new short[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesShortArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesShortArray(count, element);
        }

        public DataShortBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataShortBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataShortBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataShortBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataShortBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public ShortArray asImmutable() {
            return this;
        }

        public ShortArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesShortArray(this.length, this.element);
        }

        public MutableShortArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableShortArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableShortArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableShortArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_SHORT;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (double)(element & 0xFFFF);
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == ((int)value & 0xFFFF) ? indexOf(lowIndex, highIndex, (short)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == ((int)value & 0xFFFF) ? lastIndexOf(lowIndex, highIndex, (short)value) : -1;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (long)(element & 0xFFFF);
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element & 0xFFFF;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == ((int)value & 0xFFFF) ? indexOf(lowIndex, highIndex, (short)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == ((int)value & 0xFFFF) ? lastIndexOf(lowIndex, highIndex, (short)value) : -1;
        }

        public int getShort(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element & 0xFFFF;
        }

        public long indexOf(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of short value " + element
                + (element >= 0 ? "" : "=(short)" + (element & 0xFFFF));
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ShortArray))
                return false;
            Array a = (ShortArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty short arrays are equal
            if (a instanceof CopiesShortArray) {
                short e = ((CopiesShortArray)a).element;
                return e == element;
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

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
            return 0xFFFF;
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      Float\.floatToIntBits\((.*?)\) ==> $1 ;;
      (?:@Override\s+)?public\s+\w+\s+getInt(.*?)(?:\r(?!\n)|\n|\r\n)\s*}\s* ==> ;;
      \bFloat\b ==> Integer ;;
      Float(?!ing) ==> Int ;;
      PER_FLOAT ==> PER_INT ;;
      float ==> int
         !! Auto-generated: NOT EDIT !! */
    static class CopiesIntArray implements IntArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final int element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesIntArray(long length, int element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesIntArray(long length, int element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillIntArray((int[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            int[] dest = (int[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillIntArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new int[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesIntArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesIntArray(count, element);
        }

        public DataIntBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataIntBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataIntBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataIntBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataIntBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public IntArray asImmutable() {
            return this;
        }

        public IntArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesIntArray(this.length, this.element);
        }

        public MutableIntArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableIntArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableIntArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableIntArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_INT;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (double)element;
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == (int)value ? indexOf(lowIndex, highIndex, (int)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (int)value ? lastIndexOf(lowIndex, highIndex, (int)value) : -1;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (long)element;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == (int)value ? indexOf(lowIndex, highIndex, (int)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (int)value ? lastIndexOf(lowIndex, highIndex, (int)value) : -1;
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of int value " + element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof IntArray))
                return false;
            Array a = (IntArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty int arrays are equal
            if (a instanceof CopiesIntArray) {
                int e = ((CopiesIntArray)a).element;
                return e == element;
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

        public double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue();
        }

        public double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue();
        }

        public long minPossibleValue() {
            return Integer.MIN_VALUE;
        }

        public long maxPossibleValue() {
            return Integer.MAX_VALUE;
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      Float\.floatToIntBits\((.*?)\) ==> $1 ;;
      (?:@Override\s+)?public\s+\w+\s+getLong(.*?)(?:\r|(?!\n)\n|\r\n)\s*}\s* ==> ;;
      (?:@Override\s+)?public\s+\w+\s+(lastI|i)ndexOf\(long\s+\w+,\s*long\s+\w+,\s*long(.*?)\n\s*}\s* ==> ;;
      return\s+\(int\)element ==> return Arrays.truncateLongToInt(element) ;;
      \bFloat\b ==> Long ;;
      Float(?!ing) ==> Long ;;
      PER_FLOAT ==> PER_LONG ;;
      float ==> long
         !! Auto-generated: NOT EDIT !! */
    static class CopiesLongArray implements LongArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final long element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesLongArray(long length, long element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesLongArray(long length, long element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillLongArray((long[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            long[] dest = (long[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillLongArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new long[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesLongArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesLongArray(count, element);
        }

        public DataLongBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataLongBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataLongBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataLongBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataLongBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public LongArray asImmutable() {
            return this;
        }

        public LongArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesLongArray(this.length, this.element);
        }

        public MutableLongArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableLongArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableLongArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableLongArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_LONG;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (double)element;
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            return value == (long)value ? indexOf(lowIndex, highIndex, (long)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return value == (long)value ? lastIndexOf(lowIndex, highIndex, (long)value) : -1;
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return Arrays.truncateLongToInt(element);
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of long value " + element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LongArray))
                return false;
            Array a = (LongArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty long arrays are equal
            if (a instanceof CopiesLongArray) {
                long e = ((CopiesLongArray)a).element;
                return e == element;
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

        public double minPossibleValue(double valueForFloatingPoint) {
            return minPossibleValue();
        }

        public double maxPossibleValue(double valueForFloatingPoint) {
            return maxPossibleValue();
        }

        public long minPossibleValue() {
            return Long.MIN_VALUE;
        }

        public long maxPossibleValue() {
            return Long.MAX_VALUE;
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, copies_array)
      Float\.floatToIntBits ==> Float\.doubleToLongBits ;;
      (?:@Override\s+)?public\s+\w+\s+getDouble(.*?)(?:\r(?!\n)|\n|\r\n)\s*}\s* ==> ;;
      (?:@Override\s+)?public\s+\w+\s+(lastI|i)ndexOf\(long\s+\w+,\s*long\s+\w+,\s*double(.*?)\n\s*}\s* ==> ;;
      Float(?!ing) ==> Double ;;
      float ==> double ;;
      PER_FLOAT ==> PER_DOUBLE
         !! Auto-generated: NOT EDIT !! */
    static class CopiesDoubleArray implements DoubleArray, CopiesArray, ArraysFuncImpl.FuncArray  {
        private final long length;
        private final double element;
        private final Func f;
        private final boolean truncateOverflows;

        CopiesDoubleArray(long length, double element) {
            this.length = length;
            this.element = element;
            this.truncateOverflows = false;
            this.f = null;
        }

        CopiesDoubleArray(long length, double element, boolean truncateOverflows, Func f) {
            // Used by Arrays.asIndexFuncArray, Matrices.asCoordFuncMatrix, etc.
            this.length = length;
            this.element = element;
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public Class<?> elementType() {
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

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillDoubleArray((double[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            double[] dest = (double[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillDoubleArray(dest, 0, count, this.element);
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element; // autoboxing
        }

        public Object newJavaArray(int length) {
            return new double[length];
        }

        public boolean isZeroFilled() {
            return length == 0 || element == 0;
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesDoubleArray(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesDoubleArray(count, element);
        }

        public DataDoubleBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
            return (DataDoubleBuffer)AbstractArray.defaultBuffer(this, mode, capacity);
        }

        public DataDoubleBuffer buffer(DataBuffer.AccessMode mode) {
            return buffer(mode, AbstractArray.defaultBufferCapacity(this));
        }

        public DataDoubleBuffer buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataDoubleBuffer buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public DoubleArray asImmutable() {
            return this;
        }

        public DoubleArray asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesDoubleArray(this.length, this.element);
        }

        public MutableDoubleArray mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (MutableDoubleArray) memoryModel.newArray(this).copy(this);
        }

        public UpdatableDoubleArray updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return (UpdatableDoubleArray) memoryModel.newUnresizableArray(this).copy(this);
        }

        public long bitsPerElement() {
            return Arrays.BITS_PER_DOUBLE;
        }

        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (long)element;
        }

        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return (int)element;
        }

        public long indexOf(long lowIndex, long highIndex, long value) {
            return value == (double)value ? indexOf(lowIndex, highIndex, (double)value) : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return value == (double)value ? lastIndexOf(lowIndex, highIndex, (double)value) : -1;
        }

        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && value == element ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public Func f() {
            return this.f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        @Override
        public String toString() {
            return length + " copies of double value " + element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DoubleArray))
                return false;
            Array a = (DoubleArray)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty double arrays are equal
            if (a instanceof CopiesDoubleArray) {
                double e = ((CopiesDoubleArray)a).element;
                return Double.doubleToLongBits(e) == Double.doubleToLongBits(element);
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }
    /*Repeat.IncludeEnd*/

        public double minPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint;
        }

        public double maxPossibleValue(double valueForFloatingPoint) {
            return valueForFloatingPoint;
        }
    }

    static class CopiesObjectArray<E> implements ObjectArray<E>, CopiesArray {
        final long length;
        final E element;

        CopiesObjectArray(long length, E element) {
            this.length = length;
            this.element = element;
        }

        public Class<E> elementType() {
            return InternalUtils.cast(this.element == null ? this.elementType : this.element.getClass());
        }

        public Class<? extends ObjectArray<E>> type() {
            return InternalUtils.cast(ObjectArray.class);
        }

        public Class<? extends UpdatableObjectArray<E>> updatableType() {
            return InternalUtils.cast(UpdatableObjectArray.class);
        }

        public Class<? extends MutableObjectArray<E>> mutableType() {
            return InternalUtils.cast(MutableObjectArray.class);
        }

        public long length() {
            return this.length;
        }

        public long capacity() {
            return this.length;
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw AbstractArray.rangeException(arrayPos + count - 1, length, getClass());
            JArrays.fillObjectArray((Object[])destArray, destArrayOffset, count, this.element);
        }

        // The following implementation MUST NOT use DataBuffer class: it's implementation may call this method
        public void getData(long arrayPos, Object destArray) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            Object[] dest = (Object[])destArray;
            if (arrayPos < 0 || arrayPos > length)
                throw AbstractArray.rangeException(arrayPos, length, getClass());
            int count = dest.length;
            if (count > length - arrayPos)
                count = (int)(length - arrayPos);
            JArrays.fillObjectArray(dest, 0, count, this.element);
        }

        public Object newJavaArray(int length) {
            return new Object[length];
        }

        public Array subArray(long fromIndex, long toIndex) {
            if (fromIndex < 0)
                throw AbstractArray.rangeException(fromIndex, length(), getClass());
            if (toIndex > length())
                throw AbstractArray.rangeException(toIndex - 1, length(), getClass());
            if (fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Negative number of elements (fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex + ") in " + getClass());
            return new CopiesObjectArray<E>(toIndex - fromIndex, element);
        }

        public Array subArr(long position, long count) {
            if (position < 0)
                throw AbstractArray.rangeException(position, length(), getClass());
            if (count < 0)
                throw new IndexOutOfBoundsException("Negative number of elements (count = " + count
                    + ") in " + getClass());
            if (position > length() - count)
                throw AbstractArray.rangeException(position + count - 1, length(), getClass());
            return new CopiesObjectArray<E>(count, element);
        }

        public DataObjectBuffer<E> buffer(DataBuffer.AccessMode mode, long capacity) {
            return InternalUtils.cast(AbstractArray.defaultBuffer(this, mode, capacity));
        }

        public DataObjectBuffer<E> buffer(DataBuffer.AccessMode mode) {
            return InternalUtils.cast(buffer(mode, AbstractArray.defaultBufferCapacity(this)));
        }

        public DataObjectBuffer<E> buffer(long capacity) {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ,
                capacity);
        }

        public DataObjectBuffer<E> buffer() {
            return buffer(this instanceof UpdatableArray ?
                DataBuffer.AccessMode.READ_WRITE :
                DataBuffer.AccessMode.READ);
        }

        public void checkUnallowedMutation() throws UnallowedMutationError {
        }

        public boolean isImmutable() {
            return true;
        }

        public boolean isUnresizable() {
            return true;
        }

        public ObjectArray<E> asImmutable() {
            return this;
        }

        public ObjectArray<E> asTrustedImmutable() {
            return this;
        }

        public Array asCopyOnNextWrite() {
            return this;
        }

        public boolean isCopyOnNextWrite() {
            return false;
        }

        public boolean isNew() {
            return true;
        }

        public boolean isNewReadOnlyView() {
            return false;
        }

        public boolean isLazy() {
            return false;
        }

        public ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        public Array shallowClone() {
            return new CopiesObjectArray<E>(this.length, this.element);
        }

        public MutableObjectArray<E> mutableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return InternalUtils.cast(memoryModel.newArray(this).copy(this));
        }

        public UpdatableObjectArray<E> updatableClone(MemoryModel memoryModel) {
            if (memoryModel == null)
                throw new NullPointerException("Null memory model");
            return InternalUtils.cast(memoryModel.newUnresizableArray(this).copy(this));
        }

        public Object getElement(long index) {
            if (index < 0 || index >= length)
                throw AbstractArray.rangeException(index, length, getClass());
            return element;
        }

        public long indexOf(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && (value == null ?
                element == null : value.equals(element)) ? lowIndex : -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > length) {
                highIndex = length;
            }
            return lowIndex < highIndex && (value == null ?
                element == null : value.equals(element)) ? highIndex - 1 : -1;
        }

        public void loadResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context) {
        }

        public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        public void freeResources(ArrayContext context) {
        }

        public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        }

        @Override
        public String toString() {
            return length + " copies of " + elementType().getName() + " value " + element;
        }

        @Override
        public int hashCode() {
            return AbstractArray.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ObjectArray<?>))
                return false;
            Array a = (ObjectArray<?>)obj;
            long n = length;
            if (a.length() != n)
                return false;
            if (n == 0 && a.length() == 0)
                return true; // all empty Object arrays are equal
            if (a instanceof CopiesObjectArray<?>) {
                Object e = ((CopiesObjectArray<?>)a).element;
                return e == null ? element == null : e.equals(element);
            }
            return AbstractArray.equals(this, a); // AbstractArray has a good equals implementation
        }

        Class<?> elementType = Object.class; // can be corrected for null elements

        public E get(long index) {
            return InternalUtils.<E>cast(this.getElement(index));
        }

        public <D> ObjectArray<D> cast(Class<D> elementType) {
            Class<?> desiredType = InternalUtils.cast(elementType);
            if (!desiredType.isAssignableFrom(this.elementType()))
                throw new ClassCastException("Illegal desired element type " + elementType + " for " + this);
            return InternalUtils.cast(this);
        }
    }
}
