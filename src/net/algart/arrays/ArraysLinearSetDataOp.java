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

import net.algart.math.functions.LinearFunc;

/**
 * <p>Implementation of {@link UpdatableArray#setData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays for linear functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
strictfp class ArraysLinearSetDataOp {
    private static final boolean OPTIMIZE_LINEAR_FOR_JARRAYS = true;

    private final boolean truncateOverflows;
    private final UpdatablePArray x;
    private final long length;

    private final double a, aInv;
    private final double b;
    private final boolean isCast;
    private final Object ja; // null or underlying Java array
    private final long subArrayOffset;
    private final int xElementTypeCode;
    private final int destElementTypeCode;

    ArraysLinearSetDataOp(boolean truncateOverflows, UpdatablePArray x, LinearFunc.Updatable lf,
        int destElementTypeCode)
    {
        if (lf == null)
            throw new AssertionError("Null lf argument");
        if (lf.n() == 0)
            throw new AssertionError("No coefficients in the passed function " + lf);
        this.truncateOverflows = truncateOverflows;
        this.x = x;
        this.length = x.length();
        this.a = lf.a(0);
        this.aInv = this.a == 1.0 ? 1.0 : 1.0 / this.a; // checking 1.0 to be on the safe side
        this.b = lf.b();
        this.isCast = this.a == 1.0 && this.b == 0.0;
        if (OPTIMIZE_LINEAR_FOR_JARRAYS) {
            this.ja = Arrays.javaArrayInternal(this.x);
            if (this.ja != null) {
                this.subArrayOffset= Arrays.javaArrayOffsetInternal(this.x);
            } else {
                this.subArrayOffset = 0;
            }
        } else {
            this.ja = null;
            this.subArrayOffset = 0;
        }
        if (this.x instanceof BitArray) {
            this.xElementTypeCode = ArraysFuncImpl.BIT_TYPE_CODE;
        } else if (this.x instanceof CharArray) {
            this.xElementTypeCode = ArraysFuncImpl.CHAR_TYPE_CODE;
        } else if (this.x instanceof ByteArray) {
            this.xElementTypeCode = ArraysFuncImpl.BYTE_TYPE_CODE;
        } else if (this.x instanceof ShortArray) {
            this.xElementTypeCode = ArraysFuncImpl.SHORT_TYPE_CODE;
        } else if (this.x instanceof IntArray) {
            this.xElementTypeCode = ArraysFuncImpl.INT_TYPE_CODE;
        } else if (this.x instanceof LongArray) {
            this.xElementTypeCode = ArraysFuncImpl.LONG_TYPE_CODE;
        } else if (this.x instanceof FloatArray) {
            this.xElementTypeCode = ArraysFuncImpl.FLOAT_TYPE_CODE;
        } else if (this.x instanceof DoubleArray) {
            this.xElementTypeCode = ArraysFuncImpl.DOUBLE_TYPE_CODE;
        } else {
            throw new AssertionError("Illegal UpdatablePArray type: " + this.x.getClass());
        }
        this.destElementTypeCode = destElementTypeCode;
    }

    void setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
        if (srcArray == null)
            throw new NullPointerException("Null srcArray argument");
        if (count < 0)
            throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
        if (arrayPos < 0)
            throw AbstractArray.rangeException(arrayPos, length, x.getClass());
        if (arrayPos > length - count)
            throw AbstractArray.rangeException(arrayPos + count - 1, length, x.getClass());
        for (; count > 0; ) {
            int len = Math.min(count, ArraysLinearGetDataOp.LINEAR_BUFFER_LENGTH);
            boolean optimizeJArray = ja != null;
            int[] intBuf = null;
            double[] doubleBuf = null;
            boolean intBufRequested = false, doubleBufRequested = false;
            try {
                if (isCast && destElementTypeCode <= ArraysFuncImpl.INT_TYPE_CODE) {
                    if (!(destElementTypeCode == ArraysFuncImpl.INT_TYPE_CODE && srcArrayOffset == 0)) {
                        intBuf = (int[])ArraysLinearGetDataOp.INT_BUFFERS.requestArray();
                        intBufRequested = true;
                    }
                } else {
                    if (!(isCast && destElementTypeCode == ArraysFuncImpl.DOUBLE_TYPE_CODE && srcArrayOffset == 0)) {
                        doubleBuf = (double[])ArraysLinearGetDataOp.DOUBLE_BUFFERS.requestArray();
                        doubleBufRequested = true;
                    }
                }
                switch (destElementTypeCode) {
                    case ArraysFuncImpl.BIT_TYPE_CODE: {
                        boolean[] src = (boolean[])srcArray;
                        if (isCast) {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                intBuf[j] = src[srcArrayOffset] ? 1 : 0;
                            }
                        } else {
                            double vFalse = -b / a + 0.0, vTrue = (1.0 - b) / a + 0.0;
                            // "/ a" is more precise than "* aInv" and can help to produce precise 0.0 in ax+b
                            // adding 0.0 replaces -0.0 with +0.0: necessary for compatibility with optimization branch
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = src[srcArrayOffset] ? vTrue : vFalse;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.CHAR_TYPE_CODE: {
                        char[] src = (char[])srcArray;
                        if (isCast) {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                intBuf[j] = src[srcArrayOffset];
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = (src[srcArrayOffset] - b) * aInv;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.BYTE_TYPE_CODE: {
                        byte[] src = (byte[])srcArray;
                        if (isCast) {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                intBuf[j] = src[srcArrayOffset] & 0xFF;
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = ((src[srcArrayOffset] & 0xFF) - b) * aInv;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.SHORT_TYPE_CODE: {
                        short[] src = (short[])srcArray;
                        if (isCast) {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                intBuf[j] = src[srcArrayOffset] & 0xFFFF;
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = ((src[srcArrayOffset] & 0xFFFF) - b) * aInv;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.INT_TYPE_CODE: {
                        int[] src = (int[])srcArray;
                        if (isCast) {
                            if (srcArrayOffset == 0) {
                                intBuf = src;
                            } else {
                                System.arraycopy(src, srcArrayOffset, intBuf, 0, len);
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = (src[srcArrayOffset] - b) * aInv;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.LONG_TYPE_CODE: {
                        long[] src = (long[])srcArray;
                        if (isCast) {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = src[srcArrayOffset];
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = (src[srcArrayOffset] - b) * aInv;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                        float[] src = (float[])srcArray;
                        if (isCast) {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = src[srcArrayOffset];
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = (src[srcArrayOffset] - b) * aInv;
                            }
                        }
                        break;
                    }

                    case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                        double[] src = (double[])srcArray;
                        if (isCast) {
                            if (srcArrayOffset == 0) {
                                doubleBuf = src;
                            } else {
                                System.arraycopy(src, srcArrayOffset, doubleBuf, 0, len);
                            }
                        } else {
                            for (int j = 0; j < len; j++, srcArrayOffset++) {
                                doubleBuf[j] = (src[srcArrayOffset] - b) * aInv;
                            }
                        }
                        break;
                    }
                    default:
                        throw new AssertionError("Illegal destElementTypeCode");
                }
                assert (intBuf == null) != (doubleBuf == null); // one from them is not null, but not both

                switch (xElementTypeCode) {
                    case ArraysFuncImpl.BIT_TYPE_CODE: {
                        boolean[] dest = (boolean[])ArraysLinearGetDataOp.BOOLEAN_BUFFERS.requestArray();
                        try {
                            if (intBuf != null) {
                                for (int j = 0; j < len; j++) {
                                    dest[j] = intBuf[j] != 0;
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    dest[j] = doubleBuf[j] != 0;
                                }
                            }
                            x.setData(arrayPos, dest, 0, len);
                        } finally {
                            ArraysLinearGetDataOp.BOOLEAN_BUFFERS.releaseArray(dest);
                        }
                        break;
                    }

                    case ArraysFuncImpl.CHAR_TYPE_CODE: {
                        if (optimizeJArray) {
                            char[] dest = (char[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        int v = intBuf[j];
                                        dest[destOffset] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                            v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                                (char)v;
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (char)intBuf[j];
                                    }
                                }
                            } else {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        int v = (int)doubleBuf[j];
                                        dest[destOffset] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                            v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                                (char)v;
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (char)(long)doubleBuf[j];
                                    }
                                }
                            }
                        } else {
                            char[] dest = (char[])ArraysLinearGetDataOp.CHAR_BUFFERS.requestArray();
                            try {
                                if (intBuf != null) {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            int v = intBuf[j];
                                            dest[j] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                                v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                                    (char)v;
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (char)intBuf[j];
                                        }
                                    }
                                } else {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            int v = (int)doubleBuf[j];
                                            dest[j] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                                v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                                    (char)v;
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (char)(long)doubleBuf[j];
                                        }
                                    }
                                }
                                x.setData(arrayPos, dest, 0, len);
                            } finally {
                                ArraysLinearGetDataOp.CHAR_BUFFERS.releaseArray(dest);
                            }
                        }
                        break;
                    }
                    //[[Repeat() byte ==> short;;
                    //           BYTE ==> SHORT;;
                    //           0xFF ==> 0xFFFF]]
                    case ArraysFuncImpl.BYTE_TYPE_CODE: {
                        if (optimizeJArray) {
                            byte[] dest = (byte[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        int v = intBuf[j];
                                        dest[destOffset] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (byte)intBuf[j];
                                    }
                                }
                            } else {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        int v = (int)doubleBuf[j];
                                        dest[destOffset] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (byte)(long)doubleBuf[j];
                                    }
                                }
                            }
                        } else {
                            byte[] dest = (byte[])ArraysLinearGetDataOp.BYTE_BUFFERS.requestArray();
                            try {
                                if (intBuf != null) {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            int v = intBuf[j];
                                            dest[j] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (byte)intBuf[j];
                                        }
                                    }
                                } else {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            int v = (int)doubleBuf[j];
                                            dest[j] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (byte)(long)doubleBuf[j];
                                        }
                                    }
                                }
                                x.setData(arrayPos, dest, 0, len);
                            } finally {
                                ArraysLinearGetDataOp.BYTE_BUFFERS.releaseArray(dest);
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case ArraysFuncImpl.SHORT_TYPE_CODE: {
                        if (optimizeJArray) {
                            short[] dest = (short[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        int v = intBuf[j];
                                        dest[destOffset] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (short)intBuf[j];
                                    }
                                }
                            } else {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        int v = (int)doubleBuf[j];
                                        dest[destOffset] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (short)(long)doubleBuf[j];
                                    }
                                }
                            }
                        } else {
                            short[] dest = (short[])ArraysLinearGetDataOp.SHORT_BUFFERS.requestArray();
                            try {
                                if (intBuf != null) {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            int v = intBuf[j];
                                            dest[j] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (short)intBuf[j];
                                        }
                                    }
                                } else {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            int v = (int)doubleBuf[j];
                                            dest[j] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (short)(long)doubleBuf[j];
                                        }
                                    }
                                }
                                x.setData(arrayPos, dest, 0, len);
                            } finally {
                                ArraysLinearGetDataOp.SHORT_BUFFERS.releaseArray(dest);
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]

                    case ArraysFuncImpl.INT_TYPE_CODE: {
                        if (optimizeJArray) {
                            int[] dest = (int[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                System.arraycopy(intBuf, 0, dest, destOffset, len);
                            } else {
                                if (truncateOverflows) {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (int)doubleBuf[j];
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destOffset++) {
                                        dest[destOffset] = (int)(long)doubleBuf[j];
                                    }
                                }
                            }
                        } else {
                            if (intBuf != null) {
                                x.setData(arrayPos, intBuf, 0, len);
                            } else {
                                int[] dest = (int[])ArraysLinearGetDataOp.INT_BUFFERS.requestArray();
                                try {
                                    if (truncateOverflows) {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (int)doubleBuf[j];
                                        }
                                    } else {
                                        for (int j = 0; j < len; j++) {
                                            dest[j] = (int)(long)doubleBuf[j];
                                        }
                                    }
                                    x.setData(arrayPos, dest, 0, len);
                                } finally {
                                    ArraysLinearGetDataOp.INT_BUFFERS.releaseArray(dest);
                                }
                            }
                        }
                        break;
                    }
                    //[[Repeat() long ==> float;;
                    //           LONG ==> FLOAT]]
                    case ArraysFuncImpl.LONG_TYPE_CODE: {
                        if (optimizeJArray) {
                            long[] dest = (long[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                for (int j = 0; j < len; j++, destOffset++) {
                                    dest[destOffset] = intBuf[j];
                                }
                            } else {
                                for (int j = 0; j < len; j++, destOffset++) {
                                    dest[destOffset] = (long)doubleBuf[j];
                                }
                            }
                        } else {
                            long[] dest = (long[])ArraysLinearGetDataOp.LONG_BUFFERS.requestArray();
                            try {
                                if (intBuf != null) {
                                    for (int j = 0; j < len; j++) {
                                        dest[j] = intBuf[j];
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        dest[j] = (long)doubleBuf[j];
                                    }
                                }
                                x.setData(arrayPos, dest, 0, len);
                            } finally {
                                ArraysLinearGetDataOp.LONG_BUFFERS.releaseArray(dest);
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                        if (optimizeJArray) {
                            float[] dest = (float[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                for (int j = 0; j < len; j++, destOffset++) {
                                    dest[destOffset] = intBuf[j];
                                }
                            } else {
                                for (int j = 0; j < len; j++, destOffset++) {
                                    dest[destOffset] = (float)doubleBuf[j];
                                }
                            }
                        } else {
                            float[] dest = (float[])ArraysLinearGetDataOp.FLOAT_BUFFERS.requestArray();
                            try {
                                if (intBuf != null) {
                                    for (int j = 0; j < len; j++) {
                                        dest[j] = intBuf[j];
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        dest[j] = (float)doubleBuf[j];
                                    }
                                }
                                x.setData(arrayPos, dest, 0, len);
                            } finally {
                                ArraysLinearGetDataOp.FLOAT_BUFFERS.releaseArray(dest);
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]

                    case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                        if (optimizeJArray) {
                            double[] dest = (double[])ja;
                            int destOffset = (int)(arrayPos + subArrayOffset);
                            if (intBuf != null) {
                                for (int j = 0; j < len; j++, destOffset++) {
                                    dest[destOffset] = intBuf[j];
                                }
                            } else {
                                System.arraycopy(doubleBuf, 0, dest, destOffset, len);
                            }
                        } else {
                            if (intBuf == null) {
                                x.setData(arrayPos, doubleBuf, 0, len);
                            } else {
                                double[] dest = (double[])ArraysLinearGetDataOp.DOUBLE_BUFFERS.requestArray();
                                try {
                                    for (int j = 0; j < len; j++) {
                                        dest[j] = intBuf[j];
                                    }
                                    x.setData(arrayPos, dest, 0, len);
                                } finally {
                                    ArraysLinearGetDataOp.DOUBLE_BUFFERS.releaseArray(dest);
                                }
                            }
                        }
                        break;
                    }
                    default:
                        throw new AssertionError("Illegal srcElementTypeCode");
                }

            } finally {
                if (intBufRequested) {
                    ArraysLinearGetDataOp.INT_BUFFERS.releaseArray(intBuf);
                }
                if (doubleBufRequested) {
                    ArraysLinearGetDataOp.DOUBLE_BUFFERS.releaseArray(doubleBuf);
                }
            }
            arrayPos += len;
            count -= len;
        }
    }
}
