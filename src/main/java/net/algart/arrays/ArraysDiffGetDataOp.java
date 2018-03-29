/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays for x0-x1 and |x0-x1| functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class ArraysDiffGetDataOp {
    private static final boolean OPTIMIZE_SUBTRACT_FOR_JARRAYS = true;
    private static final boolean OPTIMIZE_SUBTRACT_FOR_JBUFFERS = InternalUtils.SERVER_OPTIMIZATION;

    private final PArray[] x;
    private final PArray result;

    private final boolean isBit;
    private final long length;
    private final Object[] jaOrDStor; // every non-null element contains either DataStorages.Storage or Java array
    private final long[] saShift;
    private final long[] subArrayOffset;
    private final JArrayPool bufferPool;
    private final ArrayDiffOp ado;
    private final boolean isAbsDiff;
    private final boolean truncateOverflows;
    ArraysDiffGetDataOp(PArray result, PArray x0, PArray x1, ArrayDiffOp ado,
        boolean isAbsDiff, boolean truncateOverflows)
    {
        this.length = result.length();
        this.x = new PArray[] {x0, x1};
        for (PArray xk : this.x) {
            if (xk.elementType() != result.elementType())
                throw new AssertionError("Different x[] / result element types");
            if (xk.length() != this.length)
                throw new AssertionError("Different x[] / result lengths");
        }
        this.result = result;
        this.isBit = this.x[0] instanceof BitArray;
        this.jaOrDStor = new Object[this.x.length];
        this.subArrayOffset = new long[this.x.length];
        this.saShift = new long[this.x.length];
        for (int k = 0; k < this.x.length; k++) {
            Array array = this.x[k];
            if (Arrays.isShifted(array)) {
                saShift[k] = Arrays.getShift(array);
                array = Arrays.getUnderlyingArrays(array)[0];
            }
            this.jaOrDStor[k] = isBit ?
                Arrays.longJavaArrayInternal((BitArray)array) :
                Arrays.javaArrayInternal(array);
            if (this.jaOrDStor[k] != null) {
                this.subArrayOffset[k] = isBit ?
                    Arrays.longJavaArrayOffsetInternal((BitArray)array) :
                    Arrays.javaArrayOffsetInternal(array);
            }
            if (this.jaOrDStor[k] == null) {
                if (array instanceof AbstractBufferArray) {
                    this.jaOrDStor[k] = ((AbstractBufferArray)array).storage;
                    this.subArrayOffset[k] = ((AbstractBufferArray)array).offset;
                }
            }
        }
        this.bufferPool =
            this.x[0] instanceof CharArray ? ArraysFuncImpl.CHAR_BUFFERS :
            this.x[0] instanceof ByteArray ? ArraysFuncImpl.BYTE_BUFFERS :
            this.x[0] instanceof ShortArray ? ArraysFuncImpl.SHORT_BUFFERS :
            this.x[0] instanceof IntArray ? ArraysFuncImpl.INT_BUFFERS :
            this.x[0] instanceof LongArray ? ArraysFuncImpl.LONG_BUFFERS :
            this.x[0] instanceof FloatArray ? ArraysFuncImpl.FLOAT_BUFFERS :
            this.x[0] instanceof DoubleArray ? ArraysFuncImpl.DOUBLE_BUFFERS :
            null;
        this.ado = ado;
        this.isAbsDiff = isAbsDiff;
        this.truncateOverflows = truncateOverflows;
    }

    void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        if (destArray == null)
            throw new NullPointerException("Null destArray argument");
        if (count < 0)
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        if (arrayPos < 0)
            throw AbstractArray.rangeException(arrayPos, x[0].length(), x[0].getClass());
        if (arrayPos > x[0].length() - count)
            throw AbstractArray.rangeException(arrayPos + count - 1, x[0].length(), x[0].getClass());
        for (; count > 0; ) {
            int len;
            if (isBit) {
                len = Math.min(count, ArraysFuncImpl.BIT_BUFFER_LENGTH);
                long[] buf = (long[])ArraysFuncImpl.BIT_BUFFERS.requestArray();
                try {
                    int gap = Arrays.goodStartOffsetInArrayOfLongs((BitArray)result, arrayPos,
                        ArraysFuncImpl.BITS_GAP);
                    ((BitArray)result).getBits(arrayPos, buf, gap, len);
                    PackedBitArrays.unpackBits((boolean[])destArray, destArrayOffset, buf, gap, len);
                } finally {
                    ArraysFuncImpl.BIT_BUFFERS.releaseArray(buf);
                }
            } else {
                Object destBuf = null;
                try {
                    len = Math.min(count, bufferPool.arrayLength());
                    long analyzeResult = ArraysMinMaxGetDataOp.analyzeSourceArrays(
                        jaOrDStor, saShift, subArrayOffset,
                        arrayPos, length, len, destArray, destArrayOffset,
                        ArraysMinMaxGetDataOp.ALL_OFFSETS, null);
                    Object dest = destArray;
                    int destOffset = destArrayOffset;
                    if (analyzeResult == ArraysMinMaxGetDataOp.DANGEROUS) {
                        destBuf = bufferPool.requestArray();
                        dest = destBuf;
                        destOffset = 0;
                    }
                    if (analyzeResult != ArraysMinMaxGetDataOp.SAFE_IN_PLACE_TO_ARRAY_0_WITH_SAME_OFFSET) {
                        // in other case, the copying does nothing and can be skipped
                        x[0].getData(arrayPos, dest, destOffset, len);
                    }
                    for (int k = 1; k < x.length; k++) { // 1 iteration always in this version
                        boolean optimizeJBuffer = false, optimizeJArray = false;
                        long jaOrDStorOffset = ArraysMinMaxGetDataOp.analyzeSourceArrays(
                            jaOrDStor, saShift, subArrayOffset,
                            arrayPos, length, len, destArray, destArrayOffset,
                            k, null);
                        if (jaOrDStorOffset != -1) {
                            optimizeJBuffer = jaOrDStor[k] instanceof DataStorage;
                            optimizeJArray = !optimizeJBuffer;
                        }
                        if (OPTIMIZE_SUBTRACT_FOR_JBUFFERS && optimizeJBuffer) {
                            DataStorage storage = (DataStorage)jaOrDStor[k];
                            if (isAbsDiff) {
                                storage.absDiffData(jaOrDStorOffset, dest, destOffset, len, truncateOverflows);
                            } else {
                                storage.subtractData(jaOrDStorOffset, dest, destOffset, len, truncateOverflows);
                            }
                        } else if (OPTIMIZE_SUBTRACT_FOR_JARRAYS && optimizeJArray) {
                            ado.process(dest, destOffset, jaOrDStor[k], (int)jaOrDStorOffset, len);
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
                                ado.process(dest, destOffset, buf, 0, len);
                            } finally {
                                bufferPool.releaseArray(buf);
                            }
                        }
                    }
                    if (analyzeResult == ArraysMinMaxGetDataOp.DANGEROUS) {
                        System.arraycopy(destBuf, 0, destArray, destArrayOffset, len);
                    }
                } finally {
                    bufferPool.releaseArray(destBuf);
                }
            }
            destArrayOffset += len;
            arrayPos += len;
            count -= len;
        }
    }

    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        if (!isBit)
            throw new AssertionError("Illegal usage of " + getClass().getName() + ".getBits");
        if (destArray == null)
            throw new NullPointerException("Null destArray argument");
        if (count < 0)
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        if (arrayPos < 0)
            throw AbstractArray.rangeException(arrayPos, x[0].length(), getClass());
        if (arrayPos > x[0].length() - count)
            throw AbstractArray.rangeException(arrayPos + count - 1, x[0].length(), getClass());
        for (; count > 0; ) {
            final int len = (int)Math.min(count, ArraysFuncImpl.BIT_BUFFER_LENGTH);
            long analyzeResult = ArraysMinMaxGetDataOp.analyzeSourceArrays(
                jaOrDStor, saShift, subArrayOffset,
                arrayPos, length, len, destArray, destArrayOffset,
                ArraysMinMaxGetDataOp.ALL_OFFSETS, null);
            long[] longBuf = null;
            long[] destLongBuf = null;
            try {
                longBuf = (long[])ArraysFuncImpl.BIT_BUFFERS.requestArray();
                long[] dest = destArray;
                long destOffset = destArrayOffset;
                int gap = Arrays.goodStartOffsetInArrayOfLongs((BitArray)x[0], arrayPos, ArraysFuncImpl.BITS_GAP);
                if (analyzeResult == ArraysMinMaxGetDataOp.DANGEROUS) {
                    destLongBuf = (long[])ArraysFuncImpl.BIT_BUFFERS.requestArray();
                    dest = destLongBuf;
                    destOffset = gap;
                }
                if (analyzeResult != ArraysMinMaxGetDataOp.SAFE_IN_PLACE_TO_ARRAY_0_WITH_SAME_OFFSET) {
                    ((BitArray)x[0]).getBits(arrayPos, dest, destOffset, len);
                }
                for (int k = 1; k < x.length; k++) { // 1 iteration always in this version
                    processBits(k, arrayPos, dest, destOffset, len, gap, longBuf);
                }
                if (dest != destArray) {
                    assert dest == destLongBuf;
                    PackedBitArrays.copyBits(destArray, destArrayOffset, dest, destOffset, len);
                }
            } finally {
                ArraysFuncImpl.BIT_BUFFERS.releaseArray(longBuf);
                ArraysFuncImpl.BIT_BUFFERS.releaseArray(destLongBuf);
            }
            destArrayOffset += len;
            arrayPos += len;
            count -= len;
        }
    }

    private void processBits(int xIndex, long arrayPos, long[] dest, long destOffset, int len,
        int gap, long[] longBuf)
    {
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
            DataBitStorage storage = (DataBitStorage)jaOrDStor[xIndex];
            if (isAbsDiff) {
                storage.xorBits(p + subArrayOffset[xIndex], dest, destOffset, len);
            } else {
                storage.andNotBits(p + subArrayOffset[xIndex], dest, destOffset, len);
            }
        } else if (optimizeJArray) {
            if (isAbsDiff) {
                PackedBitArrays.xorBits(dest, destOffset,
                    (long[])jaOrDStor[xIndex], p + subArrayOffset[xIndex], len);
            } else {
                PackedBitArrays.andNotBits(dest, destOffset,
                    (long[])jaOrDStor[xIndex], p + subArrayOffset[xIndex], len);
            }
        } else if (isAbsDiff && x[xIndex] instanceof CopiesArraysImpl.CopiesBitArray) {
            // this "thick" is often used for performing NOT operation via asFuncArray
            if (((CopiesArraysImpl.CopiesBitArray)x[xIndex]).element) // if false, then nothing to do
                PackedBitArrays.notBits(dest, destOffset, dest, destOffset, len);
        } else {
            ((BitArray)x[xIndex]).getBits(arrayPos, longBuf, gap, len);
            if (isAbsDiff) {
                PackedBitArrays.xorBits(dest, destOffset, longBuf, gap, len);
            } else {
                PackedBitArrays.andNotBits(dest, destOffset, longBuf, gap, len);
            }
        }
    }

    private interface ArrayDiffOp {
        public void process(Object dest, int destOffset, Object src, int srcOffset, int len);
    }

    //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
    //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
    //           (len)(,\s*truncateOverflows) ==> $1$2,,$1$2,,$1$2,,$1,,...]]
    static ArrayDiffOp getByteSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractByteArray((byte[])dest, destOffset, (byte[])src, srcOffset, len, truncateOverflows);
            }

            public String toString() {
                return "byte array subtraction (Java)";
            }
        };
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static ArrayDiffOp getCharSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractCharArray((char[])dest, destOffset, (char[])src, srcOffset, len, truncateOverflows);
            }

            public String toString() {
                return "char array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getShortSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractShortArray((short[])dest, destOffset, (short[])src, srcOffset, len, truncateOverflows);
            }

            public String toString() {
                return "short array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getIntSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractIntArray((int[])dest, destOffset, (int[])src, srcOffset, len, truncateOverflows);
            }

            public String toString() {
                return "int array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getLongSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractLongArray((long[])dest, destOffset, (long[])src, srcOffset, len);
            }

            public String toString() {
                return "long array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getFloatSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractFloatArray((float[])dest, destOffset, (float[])src, srcOffset, len);
            }

            public String toString() {
                return "float array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getDoubleSubtractOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.subtractDoubleArray((double[])dest, destOffset, (double[])src, srcOffset, len);
            }

            public String toString() {
                return "double array subtraction (Java)";
            }
        };
    }

    //[[Repeat.AutoGeneratedEnd]]

    //[[Repeat() byte ==> char,,short,,long,,float,,double;;
    //           Byte ==> Char,,Short,,Long,,Float,,Double]]
    static ArrayDiffOp getByteAbsDiffOp() {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfByteArray((byte[])dest, destOffset, (byte[])src, srcOffset, len);
            }

            public String toString() {
                return "byte array subtraction (Java)";
            }
        };
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static ArrayDiffOp getCharAbsDiffOp() {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfCharArray((char[])dest, destOffset, (char[])src, srcOffset, len);
            }

            public String toString() {
                return "char array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getShortAbsDiffOp() {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfShortArray((short[])dest, destOffset, (short[])src, srcOffset, len);
            }

            public String toString() {
                return "short array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getLongAbsDiffOp() {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfLongArray((long[])dest, destOffset, (long[])src, srcOffset, len);
            }

            public String toString() {
                return "long array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getFloatAbsDiffOp() {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfFloatArray((float[])dest, destOffset, (float[])src, srcOffset, len);
            }

            public String toString() {
                return "float array subtraction (Java)";
            }
        };
    }

    static ArrayDiffOp getDoubleAbsDiffOp() {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfDoubleArray((double[])dest, destOffset, (double[])src, srcOffset, len);
            }

            public String toString() {
                return "double array subtraction (Java)";
            }
        };
    }

    //[[Repeat.AutoGeneratedEnd]]

    static ArrayDiffOp getIntAbsDiffOp(final boolean truncateOverflows) {
        return new ArrayDiffOp() {
            public void process(Object dest, int destOffset, Object src, int srcOffset, int len) {
                JArrays.absDiffOfIntArray((int[])dest, destOffset, (int[])src, srcOffset, len, truncateOverflows);
            }

            public String toString() {
                return "byte array subtraction (Java)";
            }
        };
    }
}
