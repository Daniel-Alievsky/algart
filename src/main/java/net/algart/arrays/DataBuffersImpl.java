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

import java.util.Objects;

/**
 * <p>Implementations of {@link DataBuffer data buffers} for AlgART arrays.</p>
 *
 * @author Daniel Alievsky
 */
class DataBuffersImpl {
    static final int BITS_GAP = 256; // a little gap for better bits alignment; must be 2^k
    static final int MAX_BUFFER_SIZE = 65536; // bytes or byte pairs for 64-bit types: 64K or 128K; must be <2^31
    // This constant and the specified behavior is used in AbstractArray.largeBufferCapacity!

    static final JArrayPool BIT_BUFFERS = JArrayPool.getInstance(long.class, MAX_BUFFER_SIZE / 8 + BITS_GAP / 64 + 1);
    static final JArrayPool CHAR_BUFFERS = JArrayPool.getInstance(char.class, MAX_BUFFER_SIZE / 2);
    static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, MAX_BUFFER_SIZE);
    static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, MAX_BUFFER_SIZE / 2);
    static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, MAX_BUFFER_SIZE / 4);
    static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, MAX_BUFFER_SIZE / 4); // byte pairs
    static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, MAX_BUFFER_SIZE / 4);
    static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, MAX_BUFFER_SIZE / 4); // byte pairs

    static class ArrayBuffer implements DataBuffer {
        Object data = null;
        private final Array array;
        private final boolean isNCopies;
        private final boolean isDirect;
        private final JArrayPool bufferPool;
        private final AccessMode mode;
        private final long capacity;
        private final int javaArrayLength;
        private long position = 0;
        private boolean initialized;
        private boolean disposed;
        long from = 0, to = 0, count = 0;
        boolean caching = false;
        // "caching" field and "dispose()" method may be set inside this package only.
        // In another case, the following terrible scenario is possible:
        //    1) a bad client sets this flag and maps the buffer (Java array is allocated in pool);
        //    2) he requests the reference to the array by "data()" call and saves it;
        //    3) he disposes the buffer - the array is returned to the pool;
        //    4) someone uses this class, and simultaneously the bad client
        //       damages the buffer data via the saved reference.

        ArrayBuffer(Array array, AccessMode mode, long capacity, boolean trusted) {
            Objects.requireNonNull(array, "Null array argument");
            Objects.requireNonNull(mode, "Null mode argument");
            long maxCap = SimpleMemoryModel.maxSupportedLengthImpl(array.elementType());
            if (array instanceof BitArray) {
                maxCap -= BITS_GAP;
            }
            if (capacity <= 0 || capacity > maxCap) {
                throw new IllegalArgumentException("Illegal capacity=" + capacity
                    + ": it must be in range 1.." + maxCap);
            }
            if (capacity > array.length()) {
                capacity = (int) array.length();
            }
            if (!(array instanceof UpdatableArray) && mode != AccessMode.READ) {
                throw new IllegalArgumentException(mode + " is allowed for UpdatableArray only");
            }
            this.array = array;
            this.mode = mode;
            this.capacity = capacity;
            this.isNCopies = Arrays.isNCopies(array);
            if (array instanceof BitArray) {
                long[] ja = null;
                if (mode != AccessMode.PRIVATE) {
                    if (trusted || (!array.isImmutable() && !array.isCopyOnNextWrite())) {
                        ja = Arrays.longJavaArrayInternal((BitArray) array);
                    }
                }
                this.isDirect = ja != null;
                if (isDirect) {
                    this.bufferPool = null;
                    this.data = ja;
                    this.javaArrayLength = -1;
                } else {
                    long packedLen = PackedBitArrays.packedLength(capacity + BITS_GAP);
                    this.javaArrayLength = (int) packedLen;
                    assert packedLen == this.javaArrayLength;
                    if (packedLen <= BIT_BUFFERS.arrayLength()) {
                        this.bufferPool = BIT_BUFFERS;
                    } else {
                        this.bufferPool = null;
                    }
                }
            } else {
                assert capacity == (int) capacity;
                Object ja = null;
                if (mode != AccessMode.PRIVATE) {
                    if (trusted) {
                        ja = Arrays.javaArrayInternal(array);
                    } else {
                        if (array instanceof DirectAccessible && ((DirectAccessible) array).hasJavaArray()
                            && !array.isCopyOnNextWrite()) {
                            ja = ((DirectAccessible) array).javaArray();
                        }
                    }
                }
                this.isDirect = ja != null;
                if (isDirect) {
                    this.bufferPool = null;
                    this.data = ja;
                    this.javaArrayLength = -1;
                } else {
                    JArrayPool pool = array instanceof CharArray ? CHAR_BUFFERS
                        : array instanceof ByteArray ? BYTE_BUFFERS
                        : array instanceof ShortArray ? SHORT_BUFFERS
                        : array instanceof IntArray ? INT_BUFFERS
                        : array instanceof LongArray ? LONG_BUFFERS
                        : array instanceof FloatArray ? FLOAT_BUFFERS
                        : array instanceof DoubleArray ? DOUBLE_BUFFERS
                        : null;
                    this.javaArrayLength = (int) capacity;
                    assert capacity == this.javaArrayLength;
                    if (pool != null && this.javaArrayLength <= pool.arrayLength()) {
                        this.bufferPool = pool;
                    } else {
                        this.bufferPool = null;
                    }
                }
            }
            initialized = false;
            assert this.count >= 0;
            assert this.count <= this.capacity;
            assert this.capacity <= array.length();
        }

        public AccessMode mode() {
            return mode;
        }

        public Object data() {
            if (disposed) {
                throw new IllegalStateException("The data buffer is disposed");
            }
            return initialized ? data : null;
        }

        public DataBuffer map(long position) {
            return map(position, capacity, true);
        }

        public DataBuffer map(long position, boolean readData) {
            return map(position, capacity, readData);
        }

        public DataBuffer mapNext() {
            return map(position + count, capacity, true);
        }

        public DataBuffer mapNext(boolean readData) {
            return map(position + count, capacity, readData);
        }

        public DataBuffer map(long position, long maxCount) {
            return map(position, maxCount, true);
        }

        public DataBuffer map(long position, long maxCount, boolean readData) {
            if (disposed) {
                throw new IllegalStateException("The data buffer is disposed");
            }
            if (position < 0) {
                throw new IndexOutOfBoundsException("Negative position argument");
            }
            if (maxCount < 0) {
                throw new IllegalArgumentException("Negative maxCount argument");
            }
            long len = array.length();
            if (position > len) {
                throw new IndexOutOfBoundsException("Illegal position argument (" + position
                    + "): it's greater than array length");
            }
            allocateData();
            this.position = position;
            count = Math.min(maxCount, Math.min(capacity, len - position));
            if (!isNCopies) { // in another case, the data are loaded in allocateData() only once
                if (array instanceof BitArray) {
                    if (isDirect) {
                        from = position + Arrays.longJavaArrayOffsetInternal((BitArray) array);
                    } else {
                        from = Arrays.goodStartOffsetInArrayOfLongs((BitArray) array, position, BITS_GAP);
                        // Now from+count<=count+BITS_GAP-1<capacity+BITS_GAP
                        if (readData && count > 0) {
                            ((BitArray) array).getBits(position, (long[]) data, from, count);
                        }
                    }
                } else {
                    if (isDirect) {
                        from = position + Arrays.javaArrayOffsetInternal(array);
                    } else {
                        assert from == 0;
                        assert count == (int) count;
                        if (readData && count > 0) {
                            array.getData(position, data, 0, (int) count);
                        }
                    }
                }
            }
            to = from + count;
            initialized = true;
            assert count >= (position == len || maxCount == 0 ? 0 : 1);
            assert count <= capacity;
            assert count <= len - position;
            assert from >= 0;
            return this;
        }

        public DataBuffer force() {
            return force(from, to);
        }

        public DataBuffer force(long fromIndex, long toIndex) {
            if (disposed) {
                throw new IllegalStateException("The data buffer is disposed");
            }
            if (mode == AccessMode.READ) {
                throw new IllegalStateException("Cannot force read-only data buffer");
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("Illegal force range: fromIndex = " + fromIndex
                    + " > toIndex = " + toIndex);
            }
            if (fromIndex < this.from || toIndex > this.to) {
                throw new IllegalArgumentException("Illegal force range " + fromIndex + ".." + toIndex
                    + ": it must be inside the current actual array range " + this.from + ".." + this.to);
            }
            if (mode != AccessMode.PRIVATE && count > 0) {
                if (!isNCopies && !isDirect) {
                    if (array instanceof UpdatableBitArray) {
                        ((UpdatableBitArray) array).setBits(position, (long[]) data, fromIndex, toIndex - fromIndex);
                    } else {
                        assert fromIndex == (int) fromIndex;
                        assert toIndex == (int) toIndex;
                        ((UpdatableArray) array).setData(position, data, (int) fromIndex, (int) toIndex);
                    }
                }
            }
            return this;
        }

        void dispose() {
            if (disposed) {
                throw new IllegalStateException("The data buffer is already disposed");
            }
            if (caching && bufferPool != null) {
                bufferPool.releaseArray(data);
            }
            disposed = true;
        }

        public long position() {
            return position;
        }

        public long capacity() {
            return capacity;
        }

        public long fromIndex() {
            return from;
        }

        public long toIndex() {
            return to;
        }

        public long count() {
            return count;
        }

        public boolean hasData() {
            return count != 0;
        }

        public boolean isDirect() {
            return isDirect;
        }

        public int from() {
            return (int) from; // this simple implementation is overridden in ArrayBitBuffer
        }

        public int to() {
            return (int) to; // this simple implementation is overridden in ArrayBitBuffer
        }

        public int cnt() {
            return (int) count; // this simple implementation is overridden in ArrayBitBuffer
        }

        public String toString() {
            assert count == to - from;
            return "data " + (initialized ? "" : "non-initialized ")
                + "buffer [" + (isDirect ? "direct; " : "indirect; ")
                + "mode=" + mode + ", capacity=" + capacity + ", position=" + position
                + ", actual range " + from + ".." + to + "] for " + array;
        }

        private void allocateData() {
            if (data != null) {
                return;
            }
            if (caching && bufferPool != null) {
//              System.err.println("##Requesting buffer from " + bufferPool);
                this.data = bufferPool.requestArray();
            } else {
//              System.err.println("##New buffer " + javaArrayLength + " " + caching + "; " + this);
                if (array instanceof BitArray) {
                    this.data = new long[javaArrayLength];
                } else {
                    this.data = array.newJavaArray(javaArrayLength);
                }
            }
            if (isNCopies) {
                this.count = this.to = capacity;
                if (array instanceof BitArray) {
                    ((BitArray) array).getBits(0, (long[]) data, 0, capacity);
                } else {
                    array.getData(0, data);
                }
            }
        }
    }

    static class ArrayBitBuffer extends ArrayBuffer implements DataBitBuffer {
        ArrayBitBuffer(BitArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataBitBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataBitBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataBitBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataBitBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataBitBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataBitBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataBitBuffer force() {
            super.force();
            return this;
        }

        public long[] data() {
            return (long[]) super.data();
        }

        public int from() {
            if (from > Integer.MAX_VALUE) {
                throw new DataBufferIndexOverflowException("Cannot cast big fromIndex=" + from + " to int value");
            }
            return (int) from;
        }

        public int to() {
            if (to > Integer.MAX_VALUE) {
                throw new DataBufferIndexOverflowException("Cannot cast big toIndex=" + to + " to int value");
            }
            return (int) to;
        }

        public int cnt() {
            if (count > Integer.MAX_VALUE) {
                throw new DataBufferIndexOverflowException("Cannot cast big count=" + to + " to int value");
            }
            return (int) count;
        }
    }

    /*Repeat() char ==> byte,,short,,int,,long,,float,,double;;
               Char ==> Byte,,Short,,Int,,Long,,Float,,Double */

    static class ArrayCharBuffer extends ArrayBuffer implements DataCharBuffer {
        ArrayCharBuffer(CharArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataCharBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataCharBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataCharBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataCharBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataCharBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataCharBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataCharBuffer force() {
            super.force();
            return this;
        }

        public char[] data() {
            return (char[]) super.data();
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    static class ArrayByteBuffer extends ArrayBuffer implements DataByteBuffer {
        ArrayByteBuffer(ByteArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataByteBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataByteBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataByteBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataByteBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataByteBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataByteBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataByteBuffer force() {
            super.force();
            return this;
        }

        public byte[] data() {
            return (byte[]) super.data();
        }
    }

    static class ArrayShortBuffer extends ArrayBuffer implements DataShortBuffer {
        ArrayShortBuffer(ShortArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataShortBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataShortBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataShortBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataShortBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataShortBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataShortBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataShortBuffer force() {
            super.force();
            return this;
        }

        public short[] data() {
            return (short[]) super.data();
        }
    }

    static class ArrayIntBuffer extends ArrayBuffer implements DataIntBuffer {
        ArrayIntBuffer(IntArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataIntBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataIntBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataIntBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataIntBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataIntBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataIntBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataIntBuffer force() {
            super.force();
            return this;
        }

        public int[] data() {
            return (int[]) super.data();
        }
    }

    static class ArrayLongBuffer extends ArrayBuffer implements DataLongBuffer {
        ArrayLongBuffer(LongArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataLongBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataLongBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataLongBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataLongBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataLongBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataLongBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataLongBuffer force() {
            super.force();
            return this;
        }

        public long[] data() {
            return (long[]) super.data();
        }
    }

    static class ArrayFloatBuffer extends ArrayBuffer implements DataFloatBuffer {
        ArrayFloatBuffer(FloatArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataFloatBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataFloatBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataFloatBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataFloatBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataFloatBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataFloatBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataFloatBuffer force() {
            super.force();
            return this;
        }

        public float[] data() {
            return (float[]) super.data();
        }
    }

    static class ArrayDoubleBuffer extends ArrayBuffer implements DataDoubleBuffer {
        ArrayDoubleBuffer(DoubleArray array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataDoubleBuffer map(long position) {
            super.map(position);
            return this;
        }

        public DataDoubleBuffer map(long position, boolean readData) {
            super.map(position, readData);
            return this;
        }

        public DataDoubleBuffer mapNext() {
            super.mapNext();
            return this;
        }

        public DataDoubleBuffer mapNext(boolean readData) {
            super.mapNext(readData);
            return this;
        }

        public DataDoubleBuffer map(long position, long maxCount) {
            super.map(position, maxCount);
            return this;
        }

        public DataDoubleBuffer map(long position, long maxCount, boolean readData) {
            super.map(position, maxCount, readData);
            return this;
        }

        public DataDoubleBuffer force() {
            super.force();
            return this;
        }

        public double[] data() {
            return (double[]) super.data();
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    static class ArrayObjectBuffer<E> extends ArrayBuffer implements DataObjectBuffer<E> {
        ArrayObjectBuffer(ObjectArray<E> array, AccessMode mode, long capacity, boolean trusted) {
            super(array, mode, capacity, trusted);
        }

        public DataObjectBuffer<E> map(long position) {
            super.map(position);
            return this;
        }

        public DataObjectBuffer<E> mapNext() {
            super.mapNext();
            return this;
        }

        public DataObjectBuffer<E> force() {
            super.force();
            return this;
        }

        public E[] data() {
            return InternalUtils.cast(super.data());
        }
    }
}
