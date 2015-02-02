/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.nio.*;

/**
 * <p>A set of internal classes and static methods used for implementation of {@link DataStorage}
 * for a case of {@link BufferMemoryModel}.
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class DirectDataStorages {
    private DirectDataStorages() {}

    /**
     * The simple implementation of data storage, based on buffers in RAM
     * created by <tt>ByteBuffer.allocateDirect</tt>.
     */
    static abstract class DirectStorage extends DataStorage {
        ByteBuffer bb;
        boolean unresizable = false;

        MemoryModel newCompatibleMemoryModel() {
            return BufferMemoryModel.getInstance();
        }

        public String toString() {
            return "direct NIO storage: @" + Integer.toHexString(System.identityHashCode(bb));
        }

        ByteOrder byteOrder() {
            return ByteOrder.nativeOrder();
        }

        void allocate(long capacity, boolean unresizable) {
            this.unresizable = unresizable;
            bb = newByteBuffer(capacity);
            setSpecificBuffer();
        }

        DataStorage changeCapacity(long newCapacity, long offset, long length) {
            if (unresizable)
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                    + "(unallowed changeCapacity)");
            DirectStorage result = (DirectStorage)newCompatibleEmptyStorage(false);
            ByteBuffer byteBuffer = newByteBuffer(newCapacity);
            if (this instanceof DirectBitStorage) {
                LongBuffer longBuffer = byteBuffer.asLongBuffer();
                PackedBitBuffers.copyBits(longBuffer, 0, ((DirectBitStorage)this).lb, offset, length);
                result.bb = byteBuffer;
            } else {
                JBuffers.copyByteBuffer(byteBuffer, 0, bb, (int)offset << bytesPerBufferElementLog(),
                    (int)length << bytesPerBufferElementLog());
                result.bb = byteBuffer;
            }
            result.setSpecificBuffer();
            return result;
        }

        abstract void setSpecificBuffer();

        private ByteBuffer newByteBuffer(long capacity) {
            if (capacity < 0)
                throw new AssertionError("Negative capacity in package-private method");
            long arrayLength = capacity;
            if (this instanceof DirectBitStorage) {
                arrayLength = PackedBitArrays.packedLength(capacity);
            }
            if (arrayLength != (int)arrayLength)
                throw new TooLargeArrayException("Too large desired array capacity for LargeMemoryModel: "
                    + capacity + " (" + this + ")");
            ByteBuffer result = ByteBuffer.allocateDirect((int)arrayLength << bytesPerBufferElementLog());
            result.order(ByteOrder.nativeOrder());
            // The following code has become unnecessary after correction of JDK JavaDoc in Java 1.7:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6535542
            //
            // if (DO_LAZY_INIT) {
            //     JBuffers.fillByteBuffer(result, 0, result.limit(), ZERO_INIT_FILLER);
            // }
            return result;
        }
    }

    static class DirectBitStorage extends DirectStorage implements DataBitStorage {
        private LongBuffer lb;

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectBitStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_LONG_LOG;
        }

        @Override
        final boolean getBit(long index) {
            int ii = (int)(index >>> 6), bit = ((int)index) & 63;
            return (lb.get(ii) & (1L << bit)) != 0L;
        }

        @Override
        final void setBit(long index, boolean value) {
            int ii = (int)(index >>> 6), bit = ((int)index) & 63;
            synchronized(lb) {
                if (value)
                    lb.put(ii, lb.get(ii) | 1L << bit);
                else
                    lb.put(ii, lb.get(ii) & ~(1L << bit));
            }
        }

        @Override
        final long indexOfBit(long lowIndex, long highIndex, boolean value) {
            return PackedBitBuffers.indexOfBit(lb, lowIndex, highIndex, value);
        }

        @Override
        final long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            return PackedBitBuffers.lastIndexOfBit(lb, lowIndex, highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            setBit(destIndex, getBit(srcIndex));
        }

        final void swap(long firstIndex, long secondIndex) {
            int ii1 = (int)(firstIndex >>> 6), bit1 = ((int)firstIndex) & 63;
            int ii2 = (int)(secondIndex >>> 6), bit2 = ((int)secondIndex) & 63;
            synchronized(lb) {
                long l1 = lb.get(ii1);
                long l2 = lb.get(ii2);
                boolean v1 = (l1 & (1L << bit1)) != 0L;
                boolean v2 = (l2 & (1L << bit2)) != 0L;
                if (v1 != v2) {
                    if (v2)
                        lb.put(ii1, l1 | 1L << bit1);
                    else
                        lb.put(ii1, l1 & ~(1L << bit1));
                    l2 = lb.get(ii2); // for swapping 2 bits in the same long element
                    if (v1)
                        lb.put(ii2, l2 | 1L << bit2);
                    else
                        lb.put(ii2, l2 & ~(1L << bit2));
                }
            }
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            PackedBitBuffers.unpackBits((boolean[])destArray, destArrayOffset, lb, pos, count);
        }

        public void getBits(long pos, long[] destArray, long destArrayOffset, long count) {
            PackedBitBuffers.copyBits(LongBuffer.wrap(destArray), destArrayOffset, lb, pos, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            PackedBitBuffers.packBits(lb, pos, (boolean[])srcArray, srcArrayOffset, count);
        }

        public void setBits(long pos, long[] srcArray, long srcArrayOffset, long count) {
            PackedBitBuffers.copyBits(lb, pos, LongBuffer.wrap(srcArray), srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            PackedBitBuffers.fillBits(lb, pos, count, (Boolean)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, booleanZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectBitStorage) {
                PackedBitBuffers.copyBits(lb, destPos, ((DirectBitStorage)src).lb, srcPos, count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectBitStorage) {
                PackedBitBuffers.swapBits(((DirectBitStorage)another).lb, anotherPos,
                    lb, thisPos, count);
                return true;
            } else {
                return false;
            }
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            throw new UnsupportedOperationException("minData is not supported for bit storages");
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            throw new UnsupportedOperationException("maxData is not supported for bit storages");
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            throw new UnsupportedOperationException("addData is not supported for bit storages");
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            throw new UnsupportedOperationException("addData is not supported for bit storages");
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            throw new UnsupportedOperationException("subtractData is not supported for bit storages");
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            throw new UnsupportedOperationException("absDiffData is not supported for bit storages");
        }

        public void andBits(long pos, long[] destArray, long destArrayOffset, long count) {
            PackedBitBuffers.andBits(destArray, destArrayOffset, lb, pos, count);
        }

        public void orBits(long pos, long[] destArray, long destArrayOffset, long count) {
            PackedBitBuffers.orBits(destArray, destArrayOffset, lb, pos, count);
        }

        public void xorBits(long pos, long[] destArray, long destArrayOffset, long count) {
            PackedBitBuffers.xorBits(destArray, destArrayOffset, lb, pos, count);
        }

        public void andNotBits(long pos, long[] destArray, long destArrayOffset, long count) {
            PackedBitBuffers.andNotBits(destArray, destArrayOffset, lb, pos, count);
        }

        void setSpecificBuffer() {
            lb = bb == null ? null : bb.asLongBuffer();
        }
    }

    static class DirectByteStorage extends DirectStorage {
        DirectByteStorage() {
        }

        DirectByteStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectByteStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_BYTE_LOG;
        }

        @Override
        final byte getByte(long index) {
            return bb.get((int)index);
        }

        @Override
        final void setByte(long index, byte value) {
            bb.put((int)index, value);
        }

        @Override
        final long indexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfByte(bb, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfByte(bb, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            bb.put((int)destIndex, bb.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            byte v1 = bb.get(i1);
            byte v2 = bb.get(i2);
            bb.put(i1, v2);
            bb.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            ByteBuffer dup = bb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((byte[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            ByteBuffer dup = bb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((byte[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillByteBuffer(bb, (int)pos, (int)count, (Byte)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, byteZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectByteStorage) {
                JBuffers.copyByteBuffer(bb, (int)destPos, ((DirectByteStorage)src).bb, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectByteStorage) {
                JBuffers.swapByteBuffer(((DirectByteStorage)another).bb, (int)anotherPos,
                    bb, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minByteArrayAndBuffer((byte[])destArray, destArrayOffset, bb, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxByteArrayAndBuffer((byte[])destArray, destArrayOffset, bb, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addByteBufferToArray(destArray, destArrayOffset, bb, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addByteBufferToArray(destArray, destArrayOffset, bb, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractByteBufferFromArray((byte[])destArray, destArrayOffset,
                bb, (int)pos, count, truncateOverflows);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfByteArrayAndBuffer((byte[])destArray, destArrayOffset,
                bb, (int)pos, count);
        }

        void setSpecificBuffer() {
        }
    }

    /*Repeat() short     ==> char,,int,,long,,float,,double;;
               \bShort\b ==> Character,,Integer,,Long,,Float,,Double;;
               Short     ==> Char,,Int,,Long,,Float,,Double;;
               SHORT     ==> CHAR,,INT,,LONG,,FLOAT,,DOUBLE;;
               sb        ==> cb,,ib,,lb,,fb,,db;;
               (count)(,\s*truncateOverflows) ==> $1$2,,$1$2,,$1,,...;;
               (JBuffers\.absDiffOfIntArrayAndBuffer\(.*?)\); ==> $1, truncateOverflows);,,...
     */
    static class DirectShortStorage extends DirectStorage {
        private ShortBuffer sb;

        DirectShortStorage() {
        }

        DirectShortStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
            sb = byteBuffer.asShortBuffer();
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectShortStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_SHORT_LOG;
        }

        @Override
        final short getShort(long index) {
            return sb.get((int)index);
        }

        @Override
        final void setShort(long index, short value) {
            sb.put((int)index, value);
        }

        @Override
        final long indexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfShort(sb, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfShort(sb, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            sb.put((int)destIndex, sb.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            short v1 = sb.get(i1);
            short v2 = sb.get(i2);
            sb.put(i1, v2);
            sb.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            ShortBuffer dup = sb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((short[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            ShortBuffer dup = sb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((short[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillShortBuffer(sb, (int)pos, (int)count, (Short)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, shortZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectShortStorage) {
                JBuffers.copyShortBuffer(sb, (int)destPos, ((DirectShortStorage)src).sb, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectShortStorage) {
                JBuffers.swapShortBuffer(((DirectShortStorage)another).sb, (int)anotherPos,
                    sb, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void setSpecificBuffer() {
            sb = bb == null ? null : bb.asShortBuffer();
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minShortArrayAndBuffer((short[])destArray, destArrayOffset, sb, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxShortArrayAndBuffer((short[])destArray, destArrayOffset, sb, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addShortBufferToArray(destArray, destArrayOffset, sb, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addShortBufferToArray(destArray, destArrayOffset, sb, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractShortBufferFromArray((short[])destArray, destArrayOffset,
                sb, (int)pos, count, truncateOverflows);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfShortArrayAndBuffer((short[])destArray, destArrayOffset,
                sb, (int)pos, count);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static class DirectCharStorage extends DirectStorage {
        private CharBuffer cb;

        DirectCharStorage() {
        }

        DirectCharStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
            cb = byteBuffer.asCharBuffer();
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectCharStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_CHAR_LOG;
        }

        @Override
        final char getChar(long index) {
            return cb.get((int)index);
        }

        @Override
        final void setChar(long index, char value) {
            cb.put((int)index, value);
        }

        @Override
        final long indexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfChar(cb, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfChar(cb, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            cb.put((int)destIndex, cb.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            char v1 = cb.get(i1);
            char v2 = cb.get(i2);
            cb.put(i1, v2);
            cb.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            CharBuffer dup = cb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((char[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            CharBuffer dup = cb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((char[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillCharBuffer(cb, (int)pos, (int)count, (Character)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, charZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectCharStorage) {
                JBuffers.copyCharBuffer(cb, (int)destPos, ((DirectCharStorage)src).cb, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectCharStorage) {
                JBuffers.swapCharBuffer(((DirectCharStorage)another).cb, (int)anotherPos,
                    cb, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void setSpecificBuffer() {
            cb = bb == null ? null : bb.asCharBuffer();
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minCharArrayAndBuffer((char[])destArray, destArrayOffset, cb, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxCharArrayAndBuffer((char[])destArray, destArrayOffset, cb, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addCharBufferToArray(destArray, destArrayOffset, cb, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addCharBufferToArray(destArray, destArrayOffset, cb, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractCharBufferFromArray((char[])destArray, destArrayOffset,
                cb, (int)pos, count, truncateOverflows);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfCharArrayAndBuffer((char[])destArray, destArrayOffset,
                cb, (int)pos, count);
        }
    }

    static class DirectIntStorage extends DirectStorage {
        private IntBuffer ib;

        DirectIntStorage() {
        }

        DirectIntStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
            ib = byteBuffer.asIntBuffer();
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectIntStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_INT_LOG;
        }

        @Override
        final int getInt(long index) {
            return ib.get((int)index);
        }

        @Override
        final void setInt(long index, int value) {
            ib.put((int)index, value);
        }

        @Override
        final long indexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfInt(ib, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfInt(ib, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            ib.put((int)destIndex, ib.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            int v1 = ib.get(i1);
            int v2 = ib.get(i2);
            ib.put(i1, v2);
            ib.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            IntBuffer dup = ib.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((int[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            IntBuffer dup = ib.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((int[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillIntBuffer(ib, (int)pos, (int)count, (Integer)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, intZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectIntStorage) {
                JBuffers.copyIntBuffer(ib, (int)destPos, ((DirectIntStorage)src).ib, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectIntStorage) {
                JBuffers.swapIntBuffer(((DirectIntStorage)another).ib, (int)anotherPos,
                    ib, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void setSpecificBuffer() {
            ib = bb == null ? null : bb.asIntBuffer();
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minIntArrayAndBuffer((int[])destArray, destArrayOffset, ib, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxIntArrayAndBuffer((int[])destArray, destArrayOffset, ib, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addIntBufferToArray(destArray, destArrayOffset, ib, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addIntBufferToArray(destArray, destArrayOffset, ib, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractIntBufferFromArray((int[])destArray, destArrayOffset,
                ib, (int)pos, count, truncateOverflows);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfIntArrayAndBuffer((int[])destArray, destArrayOffset,
                ib, (int)pos, count, truncateOverflows);
        }
    }

    static class DirectLongStorage extends DirectStorage {
        private LongBuffer lb;

        DirectLongStorage() {
        }

        DirectLongStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
            lb = byteBuffer.asLongBuffer();
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectLongStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_LONG_LOG;
        }

        @Override
        final long getLong(long index) {
            return lb.get((int)index);
        }

        @Override
        final void setLong(long index, long value) {
            lb.put((int)index, value);
        }

        @Override
        final long indexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfLong(lb, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfLong(lb, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            lb.put((int)destIndex, lb.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            long v1 = lb.get(i1);
            long v2 = lb.get(i2);
            lb.put(i1, v2);
            lb.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            LongBuffer dup = lb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((long[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            LongBuffer dup = lb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((long[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillLongBuffer(lb, (int)pos, (int)count, (Long)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, longZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectLongStorage) {
                JBuffers.copyLongBuffer(lb, (int)destPos, ((DirectLongStorage)src).lb, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectLongStorage) {
                JBuffers.swapLongBuffer(((DirectLongStorage)another).lb, (int)anotherPos,
                    lb, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void setSpecificBuffer() {
            lb = bb == null ? null : bb.asLongBuffer();
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minLongArrayAndBuffer((long[])destArray, destArrayOffset, lb, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxLongArrayAndBuffer((long[])destArray, destArrayOffset, lb, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addLongBufferToArray(destArray, destArrayOffset, lb, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addLongBufferToArray(destArray, destArrayOffset, lb, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractLongBufferFromArray((long[])destArray, destArrayOffset,
                lb, (int)pos, count);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfLongArrayAndBuffer((long[])destArray, destArrayOffset,
                lb, (int)pos, count);
        }
    }

    static class DirectFloatStorage extends DirectStorage {
        private FloatBuffer fb;

        DirectFloatStorage() {
        }

        DirectFloatStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
            fb = byteBuffer.asFloatBuffer();
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectFloatStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_FLOAT_LOG;
        }

        @Override
        final float getFloat(long index) {
            return fb.get((int)index);
        }

        @Override
        final void setFloat(long index, float value) {
            fb.put((int)index, value);
        }

        @Override
        final long indexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfFloat(fb, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfFloat(fb, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            fb.put((int)destIndex, fb.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            float v1 = fb.get(i1);
            float v2 = fb.get(i2);
            fb.put(i1, v2);
            fb.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            FloatBuffer dup = fb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((float[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            FloatBuffer dup = fb.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((float[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillFloatBuffer(fb, (int)pos, (int)count, (Float)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, floatZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectFloatStorage) {
                JBuffers.copyFloatBuffer(fb, (int)destPos, ((DirectFloatStorage)src).fb, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectFloatStorage) {
                JBuffers.swapFloatBuffer(((DirectFloatStorage)another).fb, (int)anotherPos,
                    fb, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void setSpecificBuffer() {
            fb = bb == null ? null : bb.asFloatBuffer();
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minFloatArrayAndBuffer((float[])destArray, destArrayOffset, fb, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxFloatArrayAndBuffer((float[])destArray, destArrayOffset, fb, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addFloatBufferToArray(destArray, destArrayOffset, fb, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addFloatBufferToArray(destArray, destArrayOffset, fb, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractFloatBufferFromArray((float[])destArray, destArrayOffset,
                fb, (int)pos, count);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfFloatArrayAndBuffer((float[])destArray, destArrayOffset,
                fb, (int)pos, count);
        }
    }

    static class DirectDoubleStorage extends DirectStorage {
        private DoubleBuffer db;

        DirectDoubleStorage() {
        }

        DirectDoubleStorage(ByteBuffer byteBuffer) {
            bb = byteBuffer;
            db = byteBuffer.asDoubleBuffer();
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new DirectDoubleStorage();
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_DOUBLE_LOG;
        }

        @Override
        final double getDouble(long index) {
            return db.get((int)index);
        }

        @Override
        final void setDouble(long index, double value) {
            db.put((int)index, value);
        }

        @Override
        final long indexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.indexOfDouble(db, (int)lowIndex, (int)highIndex, value);
        }

        @Override
        final long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex >= highIndex) {
                return -1;
            } // after this check we are sure that overflow is impossible: indexes are <=length while calling this
            return JBuffers.lastIndexOfDouble(db, (int)lowIndex, (int)highIndex, value);
        }

        final void copy(long destIndex, long srcIndex) {
            db.put((int)destIndex, db.get(((int)srcIndex)));
        }

        final void swap(long firstIndex, long secondIndex) {
            int i1 = (int)firstIndex, i2 = (int)secondIndex;
            double v1 = db.get(i1);
            double v2 = db.get(i2);
            db.put(i1, v2);
            db.put(i2, v1);
        }

        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            DoubleBuffer dup = db.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.get((double[])destArray, destArrayOffset, count);
        }

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            DoubleBuffer dup = db.duplicate(); // necessary while multithread access
            dup.position((int)pos);
            dup.put((double[])srcArray, srcArrayOffset, count);
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            JBuffers.fillDoubleBuffer(db, (int)pos, (int)count, (Double)fillerWrapper);
        }

        void clearData(long pos, long count) {
            fillData(pos, count, doubleZero);
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (src instanceof DirectDoubleStorage) {
                JBuffers.copyDoubleBuffer(db, (int)destPos, ((DirectDoubleStorage)src).db, (int)srcPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (another instanceof DirectDoubleStorage) {
                JBuffers.swapDoubleBuffer(((DirectDoubleStorage)another).db, (int)anotherPos,
                    db, (int)thisPos, (int)count);
                return true;
            } else {
                return false;
            }
        }

        void setSpecificBuffer() {
            db = bb == null ? null : bb.asDoubleBuffer();
        }

        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.minDoubleArrayAndBuffer((double[])destArray, destArrayOffset, db, (int)pos, count);
        }

        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxDoubleArrayAndBuffer((double[])destArray, destArrayOffset, db, (int)pos, count);
        }

        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addDoubleBufferToArray(destArray, destArrayOffset, db, (int)pos, count);
        }

        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            JBuffers.addDoubleBufferToArray(destArray, destArrayOffset, db, (int)pos, count, mult);
        }

        void subtractData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.subtractDoubleBufferFromArray((double[])destArray, destArrayOffset,
                db, (int)pos, count);
        }

        void absDiffData(long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows)
        {
            JBuffers.absDiffOfDoubleArrayAndBuffer((double[])destArray, destArrayOffset,
                db, (int)pos, count);
        }
    }
    /*Repeat.AutoGeneratedEnd*/
}
