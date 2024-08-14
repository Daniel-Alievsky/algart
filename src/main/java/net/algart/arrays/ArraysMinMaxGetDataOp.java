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

import net.algart.arrays.BufferArraysImpl.AbstractBufferArray;

import java.util.Objects;

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays for min(x0,x1,...) and max(x0,x1,...) functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysMinMaxGetDataOp {
    private static final boolean OPTIMIZE_AND_OR_ALIGNMENT = true;
    private static final boolean OPTIMIZE_MIN_MAX_FOR_JARRAYS = true;
    private static final boolean OPTIMIZE_MIN_MAX_FOR_JBUFFERS = InternalUtils.SERVER_OPTIMIZATION;

    private final PArray[] x;
    private final PArray result;

    private final boolean isBit;
    private final long length;
    private final Object[] jaOrDStor; // every non-null element contains either DataStorages.Storage or Java array
    private final long[] saShift;
    private final long[] subArrayOffset;
    private final JArrayPool quickPositionsPool; // pool for temporary buffers
    private final JArrayPool jaOrDStorOffsetsPool; // pool for temporary buffers
    private final JArrayPool bufferPool; // one of static pools
    private final ArrayMinMaxOp ammo;
    private final boolean isMin;

    ArraysMinMaxGetDataOp(PArray result, PArray[] x, ArrayMinMaxOp ammo, boolean isMin) {
        if (x.length == 0) {
            throw new AssertionError("Empty x[] argument");
        }
        this.length = result.length();
        for (PArray xk : x) {
            if (xk.elementType() != result.elementType()) {
                throw new AssertionError("Different x[] / result element types");
            }
            if (xk.length() != this.length) {
                throw new AssertionError("Different x[] / result lengths");
            }
        }
        this.x = x; // this class is used only with x clones
        this.result = result;
        this.isBit = this.x[0] instanceof BitArray;
        this.jaOrDStor = new Object[this.x.length];
        this.subArrayOffset = new long[this.x.length];
        this.saShift = new long[this.x.length];
        this.quickPositionsPool = ArraysFuncImpl.smallLongBuffers(this.x.length);
        for (int k = 0; k < this.x.length; k++) {
            Array array = this.x[k];
            if (Arrays.isShifted(array)) {
                saShift[k] = Arrays.getShift(array);
                array = Arrays.getUnderlyingArrays(array)[0];
            }
            this.jaOrDStor[k] = isBit ?
                    Arrays.longJavaArrayInternal((BitArray) array) :
                    Arrays.javaArrayInternal(array);
            if (this.jaOrDStor[k] != null) {
                this.subArrayOffset[k] = isBit ?
                        Arrays.longJavaArrayOffsetInternal((BitArray) array) :
                        Arrays.javaArrayOffsetInternal(array);
            } else if (array instanceof AbstractBufferArray) {
                this.jaOrDStor[k] = ((AbstractBufferArray) array).storage;
                this.subArrayOffset[k] = ((AbstractBufferArray) array).offset;
            }
        }
        this.jaOrDStorOffsetsPool = ArraysFuncImpl.smallLongBuffers(this.x.length);
        this.bufferPool =
                this.x[0] instanceof CharArray ? ArraysFuncImpl.CHAR_BUFFERS
                        : this.x[0] instanceof ByteArray ? ArraysFuncImpl.BYTE_BUFFERS
                        : this.x[0] instanceof ShortArray ? ArraysFuncImpl.SHORT_BUFFERS
                        : this.x[0] instanceof IntArray ? ArraysFuncImpl.INT_BUFFERS
                        : this.x[0] instanceof LongArray ? ArraysFuncImpl.LONG_BUFFERS
                        : this.x[0] instanceof FloatArray ? ArraysFuncImpl.FLOAT_BUFFERS
                        : this.x[0] instanceof DoubleArray ? ArraysFuncImpl.DOUBLE_BUFFERS
                        : null;
        this.ammo = ammo;
        this.isMin = isMin;
    }

    void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw AbstractArray.rangeException(arrayPos, x[0].length(), x[0].getClass());
        }
        if (arrayPos > x[0].length() - count) {
            throw AbstractArray.rangeException(arrayPos + count - 1, x[0].length(), x[0].getClass());
        }
        while (count > 0) {
            int len;
            if (isBit) {
                len = Math.min(count, ArraysFuncImpl.BIT_BUFFER_LENGTH);
                long[] buf = (long[]) ArraysFuncImpl.BIT_BUFFERS.requestArray();
                try {
                    int gap = Arrays.goodStartOffsetInArrayOfLongs((BitArray) result, arrayPos,
                            ArraysFuncImpl.BITS_GAP);
                    ((BitArray) result).getBits(arrayPos, buf, gap, len);
                    PackedBitArrays.unpackBits((boolean[]) destArray, destArrayOffset, buf, gap, len);
                } finally {
                    ArraysFuncImpl.BIT_BUFFERS.releaseArray(buf);
                }
            } else {
                Object destBuf = null;
                long[] jaOrDStorOffsets = (long[]) jaOrDStorOffsetsPool.requestArray();
                // Necessary to use new array (instead of global long[] field), because this method modifies it
                try {
                    len = Math.min(count, bufferPool.arrayLength());
                    long analyzeResult = analyzeSourceArrays(jaOrDStor, saShift, subArrayOffset,
                            arrayPos, length, len, destArray, destArrayOffset, ALL_OFFSETS, jaOrDStorOffsets);
                    Object dest = destArray;
                    int destOffset = destArrayOffset;
                    if (analyzeResult == DANGEROUS) {
                        destBuf = bufferPool.requestArray();
                        dest = destBuf;
                        destOffset = 0;
                    }
                    if (analyzeResult != SAFE_IN_PLACE_TO_ARRAY_0_WITH_SAME_OFFSET) {
                        // in another case, the copying does nothing and can be skipped
                        x[0].getData(arrayPos, dest, destOffset, len);
                    }
                    for (int k = 1; k < x.length; k++) {
                        boolean optimizeJBuffer = false, optimizeJArray = false;
                        if (jaOrDStorOffsets[k] != -1) {
                            optimizeJBuffer = jaOrDStor[k] instanceof DataStorage;
                            optimizeJArray = !optimizeJBuffer;
                        }
                        if (OPTIMIZE_MIN_MAX_FOR_JBUFFERS && optimizeJBuffer) {
                            DataStorage storage = (DataStorage) jaOrDStor[k];
                            if (isMin) {
                                storage.minData(jaOrDStorOffsets[k], dest, destOffset, len);
                            } else {
                                storage.maxData(jaOrDStorOffsets[k], dest, destOffset, len);
                            }
                        } else if (OPTIMIZE_MIN_MAX_FOR_JARRAYS && optimizeJArray) {
                            ammo.process(dest, destOffset, jaOrDStor[k], (int) jaOrDStorOffsets[k], len);
                        } else {
                            Object buf = null;
                            try {
                                // Both buffers may be necessary in the following situation:
                                // x[0] and x[1] are simple arrays,
                                // x[0] is a.subArr(i,len),
                                // !optimizeJArray (for example, it is lazy),
                                // and the resulting Java array is a.subArr(j,len), j<i
                                buf = bufferPool.requestArray();
                                x[k].getData(arrayPos, buf, 0, len);
                                ammo.process(dest, destOffset, buf, 0, len);
                            } finally {
                                bufferPool.releaseArray(buf);
                            }
                        }
                    }
                    if (analyzeResult == DANGEROUS) {
                        System.arraycopy(destBuf, 0, destArray, destArrayOffset, len);
                    }
                } finally {
                    jaOrDStorOffsetsPool.releaseArray(jaOrDStorOffsets);
                    bufferPool.releaseArray(destBuf);
                }
            }
            destArrayOffset += len;
            arrayPos += len;
            count -= len;
        }
    }

    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        if (!isBit) {
            throw new AssertionError("Illegal usage of " + getClass().getName() + ".getBits");
        }
        // Necessary, because this method modifies this.quickPositions
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw AbstractArray.rangeException(arrayPos, x[0].length(), getClass());
        }
        if (arrayPos > x[0].length() - count) {
            throw AbstractArray.rangeException(arrayPos + count - 1, x[0].length(), getClass());
        }
        final BitArray x0 = (BitArray) x[0];
        while (count > 0) {
            final int len = (int) Math.min(count, ArraysFuncImpl.BIT_BUFFER_LENGTH);
            int processedArraysCount = 0; // for debugging
            long positionsMap = 0L, goodPositionsMap = 0L; // - arrays of 64 bits
            long analyzeResult = analyzeSourceArrays(jaOrDStor, saShift, subArrayOffset,
                    arrayPos, length, len, destArray, destArrayOffset, ALL_OFFSETS, null);
            long[] longBuf = null;
            long[] destLongBuf = null;
            long[] quickPositions = null;
            try {
                longBuf = (long[]) ArraysFuncImpl.BIT_BUFFERS.requestArray();
                destLongBuf = (long[]) ArraysFuncImpl.BIT_BUFFERS.requestArray();
                quickPositions = (long[]) quickPositionsPool.requestArray();
                // Necessary to use new array (instead of global long[] field), because this method modifies it
                long[] dest = destArray;
                long destOffset = destArrayOffset;
                final int gap = Arrays.goodStartOffsetInArrayOfLongs(x0, arrayPos, ArraysFuncImpl.BITS_GAP);
                if (OPTIMIZE_AND_OR_ALIGNMENT && x.length >= 3) {
                    // for 1 or 2 arrays there is no sense to avoid non-aligned and/or: use the simple algorithm
                    for (int k = 0; k < x.length; k++) {
                        quickPositions[k] = ((BitArray) x[k]).nextQuickPosition(arrayPos);
                        if (quickPositions[k] != -1) {
                            int sh = (int) (arrayPos - quickPositions[k]) & 63;
                            if ((positionsMap & (1L << sh)) != 0) {
                                goodPositionsMap |= 1L << sh;
                            }
                            positionsMap |= 1L << sh;
                        }
                    }
                    // the bit #j in goodPositions is set if there are at least 2 arrays with such quick position
                }
                if (goodPositionsMap != 0L) {
                    // the complex algorithm: all and/or operations will be aligned
                    boolean firstArray = true;
                    int lastSh = -1;
                    for (int sh = 0; sh < 64; sh++) { // we'll process array in order of increasing shift
                        if ((goodPositionsMap & (1L << sh)) == 0) {
                            continue; // - no arrays with such position
                        }
                        if (lastSh != -1) {
                            PackedBitArrays.copyBits(destLongBuf, sh, destLongBuf, lastSh, len);
                        }
                        for (int k = 0; k < x.length; k++) {
                            if (quickPositions[k] == -1 || ((int) (arrayPos - quickPositions[k]) & 63) != sh) {
                                continue; // - this array does not correspond to this shift
                            }
                            if (firstArray) {
                                ((BitArray) x[k]).getBits(arrayPos, destLongBuf, sh, len); // - aligned copying
                                firstArray = false;
                            } else {
                                processBits(k, arrayPos, destLongBuf, sh, len, sh, longBuf);
                            }
                            processedArraysCount++;
                        }
                        lastSh = sh;
                    }
                    assert lastSh != -1 :
                            "lastSh == -1: goodPositionsMap = " + Long.toBinaryString(goodPositionsMap);
                    // Here is the situation alike DANGEROUS: in the further simple algorithm,
                    // we should process destLongBuf (that is partially ready now) instead of destArray.
                    // Alternative solution could be copying here:
                    // PackedBitArrays.copyBits(destArray, destArrayOffset, destLongBuf, lastSh, len);
                    // But it leads to a bug, if x[0] was not processed yet
                    // and destArray is x[0] ("safe-in-place"): then we may destroy its content here.
                    dest = destLongBuf;
                    destOffset = lastSh;
                } else { // no good positions
                    if (analyzeResult == DANGEROUS) {
                        dest = destLongBuf;
                        destOffset = gap;
                    }
                    if (analyzeResult != SAFE_IN_PLACE_TO_ARRAY_0_WITH_SAME_OFFSET) {
                        x0.getBits(arrayPos, dest, destOffset, len);
                    }
                    processedArraysCount = 1;
                }
                // the simple algorithm: processing all or (if goodPositionsMap!=0) some of arrays
                for (int k = goodPositionsMap != 0L ? 0 : 1; k < x.length; k++) {
                    if (goodPositionsMap != 0L) {
                        long qp = ((BitArray) x[k]).nextQuickPosition(arrayPos);
                        if (qp != -1) {
                            int sh = (int) (arrayPos - qp) & 63;
                            if ((goodPositionsMap & (1L << sh)) != 0) {
                                continue; // this array was already processed in the complex algorithm
                            }
                        }
                    }
                    processBits(k, arrayPos, dest, destOffset, len, gap, longBuf);
                    processedArraysCount++;
                }
                if (dest != destArray) {
                    assert dest == destLongBuf;
                    PackedBitArrays.copyBits(destArray, destArrayOffset, dest, destOffset, len);
                }
            } finally {
                quickPositionsPool.releaseArray(quickPositions);
                ArraysFuncImpl.BIT_BUFFERS.releaseArray(longBuf);
                ArraysFuncImpl.BIT_BUFFERS.releaseArray(destLongBuf);
            }
            if (processedArraysCount != x.length) {
                throw new AssertionError("Not all or too many arrays are processed: "
                        + processedArraysCount + " instead of " + x.length);
            }
            destArrayOffset += len;
            arrayPos += len;
            count -= len;
        }
    }

    static final long SAFE = -100;
    static final long SAFE_IN_PLACE_TO_ARRAY_0_WITH_SAME_OFFSET = -101;
    static final long DANGEROUS = -102;
    static final int ALL_OFFSETS = -1;

    static long analyzeSourceArrays(
            Object[] jaOrDStor, long[] saShift, long[] subArrayOffset,
            long arrayPos, long arrayLength, long len, Object destArray, long destArrayOffset,
            int desiredOffsetIndex, long[] jaOrDStorOffsets)
    // long result allows to avoid allocation of jaOrDStorOffsets in some situations
    {
        boolean dangerousDestOverlapPossible = false, inPlaceOp = false;
        int inPlaceCount = 0;
        int startIndex = desiredOffsetIndex == ALL_OFFSETS ? 0 : desiredOffsetIndex;
        int endIndex = desiredOffsetIndex == ALL_OFFSETS ? jaOrDStor.length - 1 : desiredOffsetIndex;
        for (int k = startIndex; k <= endIndex; k++) {
            long p = arrayPos;
            boolean optimizeJBuffer = jaOrDStor[k] instanceof DataStorage;
            boolean optimizeJArray = !optimizeJBuffer && jaOrDStor[k] != null;
            if (optimizeJArray || optimizeJBuffer) {
                p -= saShift[k];
                if (p < 0) {
                    p += arrayLength;
                    if (p >= arrayLength - len) { // copied block is divided
                        optimizeJArray = optimizeJBuffer = false;
                    }
                }
            }
            long ofs;
            if (optimizeJArray || optimizeJBuffer) {
                ofs = p + subArrayOffset[k];
                assert ofs >= 0;
            } else {
                ofs = -1;
            }
            if (desiredOffsetIndex != ALL_OFFSETS) {
                return ofs;
            }
            if (optimizeJArray) {
                if (jaOrDStor[k] == destArray) {
                    if (k == 0) {
                        inPlaceOp = destArrayOffset == ofs;
                    } else {
                        if (inPlaceOp) {
                            inPlaceCount++;
                            dangerousDestOverlapPossible |= inPlaceCount >= 2 || ofs < destArrayOffset;
                            // if there is only 1 same Java array excepting the first one,
                            // and its offset is >=destArrayOffset,
                            // it is safe to perform operation in place
                        } else {
                            dangerousDestOverlapPossible = true;
                        }
                    }
                }
            } else if (!optimizeJBuffer) {
                // x[k] may depend on the destArray in any lazy way
                dangerousDestOverlapPossible = true;
            }
            if (jaOrDStorOffsets != null) {
                jaOrDStorOffsets[k] = ofs;
            }
        }
        if (dangerousDestOverlapPossible) {
            return DANGEROUS;
        } else if (inPlaceOp) {
            return SAFE_IN_PLACE_TO_ARRAY_0_WITH_SAME_OFFSET;
        } else {
            return SAFE;
        }
    }

    private void processBits(
            int xIndex, long arrayPos, long[] dest, long destOffset, int len,
            int gap, long[] longBuf) {
        long p = arrayPos;
        boolean optimizeJBuffer = jaOrDStor[xIndex] instanceof DataStorage;
        boolean optimizeJArray = !optimizeJBuffer && jaOrDStor[xIndex] != null;
        if (optimizeJArray || optimizeJBuffer) {
            p -= saShift[xIndex];
            if (p < 0) {
                p += length;
                if (p >= length - len) { // copied block is divided
                    optimizeJArray = optimizeJBuffer = false;
                }
            }
        }
        if (optimizeJBuffer) {
            DataBitStorage storage = (DataBitStorage) jaOrDStor[xIndex];
            if (isMin) {
                storage.andBits(p + subArrayOffset[xIndex], dest, destOffset, len);
            } else {
                storage.orBits(p + subArrayOffset[xIndex], dest, destOffset, len);
            }
        } else if (optimizeJArray) {
            if (isMin) {
                PackedBitArrays.andBits(dest, destOffset, (long[]) jaOrDStor[xIndex], p + subArrayOffset[xIndex], len);
            } else {
                PackedBitArrays.orBits(dest, destOffset, (long[]) jaOrDStor[xIndex], p + subArrayOffset[xIndex], len);
            }
        } else {
            ((BitArray) x[xIndex]).getBits(arrayPos, longBuf, gap, len);
            if (isMin) {
                PackedBitArrays.andBits(dest, destOffset, longBuf, gap, len);
            } else {
                PackedBitArrays.orBits(dest, destOffset, longBuf, gap, len);
            }
        }
    }

    private interface ArrayMinMaxOp {
        void process(Object dest, int destOffset, Object src, int srcOffset, int len);
    }

    //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
    //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
    //           (min|max)u ==> $1,,$1u,,$1,,$1,,$1,,$1]]
    static ArrayMinMaxOp getByteMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minByteArray((byte[]) dest, destOffset, (byte[]) src, srcOffset, len);
            }

            public String toString() {
                return "byte array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getByteMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxByteArray((byte[]) dest, destOffset, (byte[]) src, srcOffset, len);
            }

            public String toString() {
                return "byte array maximum (Java)";
            }
        };
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static ArrayMinMaxOp getCharMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minCharArray((char[]) dest, destOffset, (char[]) src, srcOffset, len);
            }

            public String toString() {
                return "char array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getCharMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxCharArray((char[]) dest, destOffset, (char[]) src, srcOffset, len);
            }

            public String toString() {
                return "char array maximum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getShortMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minShortArray((short[]) dest, destOffset, (short[]) src, srcOffset, len);
            }

            public String toString() {
                return "short array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getShortMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxShortArray((short[]) dest, destOffset, (short[]) src, srcOffset, len);
            }

            public String toString() {
                return "short array maximum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getIntMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minIntArray((int[]) dest, destOffset, (int[]) src, srcOffset, len);
            }

            public String toString() {
                return "int array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getIntMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxIntArray((int[]) dest, destOffset, (int[]) src, srcOffset, len);
            }

            public String toString() {
                return "int array maximum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getLongMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minLongArray((long[]) dest, destOffset, (long[]) src, srcOffset, len);
            }

            public String toString() {
                return "long array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getLongMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxLongArray((long[]) dest, destOffset, (long[]) src, srcOffset, len);
            }

            public String toString() {
                return "long array maximum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getFloatMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minFloatArray((float[]) dest, destOffset, (float[]) src, srcOffset, len);
            }

            public String toString() {
                return "float array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getFloatMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxFloatArray((float[]) dest, destOffset, (float[]) src, srcOffset, len);
            }

            public String toString() {
                return "float array maximum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getDoubleMinOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.minDoubleArray((double[]) dest, destOffset, (double[]) src, srcOffset, len);
            }

            public String toString() {
                return "double array minimum (Java)";
            }
        };
    }

    static ArrayMinMaxOp getDoubleMaxOp() {
        return new ArrayMinMaxOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.maxDoubleArray((double[]) dest, destOffset, (double[]) src, srcOffset, len);
            }

            public String toString() {
                return "double array maximum (Java)";
            }
        };
    }

    //[[Repeat.AutoGeneratedEnd]]
}
