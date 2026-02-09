/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.Objects;

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays, created by
 * {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method, for any functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysAnyFuncGetDataOp {
    private static final int ANY_FUNC_BUFFER_LENGTH = 16384; // elements (nArgs * double[] + element-type[])
    private static final boolean OPTIMIZE_ANY_FUNC_FOR_JARRAYS = true;

    private static final JArrayPool BOOLEAN_BUFFERS = JArrayPool.getInstance(boolean.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool CHAR_BUFFERS = JArrayPool.getInstance(char.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, ANY_FUNC_BUFFER_LENGTH);
    private static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, ANY_FUNC_BUFFER_LENGTH);

    private final boolean truncateOverflows;
    private final PArray[] x;
    private final long length;

    private final Func f;
    private final Object[] ja; // every non-null element contains Java array
    private final long[] saShift;
    private final long[] subArrayOffset;
    private final int[] srcElementTypeCode;
    private final int destElementTypeCode;
    private final JArrayPool arrayOfDoubleBuffersPool; // pool for double[nArg][] array
    private final JArrayPool argsPool; // pool for f.get arguments
    ArraysAnyFuncGetDataOp(boolean truncateOverflows, PArray[] x, Func f, int destElementTypeCode) {
        this.truncateOverflows = truncateOverflows;
        this.x = x.clone();
        this.length = this.x[0].length();
        for (PArray xk : this.x) {
            if (xk.length() != this.length) {
                throw new AssertionError("Different x[] lengths");
            }
        }
        this.f = f;
        this.ja = new Object[this.x.length];
        this.subArrayOffset = new long[this.x.length];
        this.saShift = new long[this.x.length];
        for (int k = 0; k < this.x.length; k++) {
            Array array = this.x[k];
            if (Arrays.isShifted(array)) {
                saShift[k] = Arrays.getShift(array);
                array = Arrays.getUnderlyingArrays(array)[0];
            }
            if (OPTIMIZE_ANY_FUNC_FOR_JARRAYS) {
                this.ja[k] = Arrays.javaArrayInternal(array);
                if (this.ja[k] != null) {
                    assert !(array instanceof BitArray);
                    this.subArrayOffset[k] = Arrays.javaArrayOffsetInternal(array);
                }
            }
        }
        this.srcElementTypeCode = new int[x.length];
        for (int k = 0; k < this.x.length; k++) {
            if (this.x[k] instanceof BitArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.BIT_TYPE_CODE;
            } else if (this.x[k] instanceof CharArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.CHAR_TYPE_CODE;
            } else if (this.x[k] instanceof ByteArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.BYTE_TYPE_CODE;
            } else if (this.x[k] instanceof ShortArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.SHORT_TYPE_CODE;
            } else if (this.x[k] instanceof IntArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.INT_TYPE_CODE;
            } else if (this.x[k] instanceof LongArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.LONG_TYPE_CODE;
            } else if (this.x[k] instanceof FloatArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.FLOAT_TYPE_CODE;
            } else if (this.x[k] instanceof DoubleArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.DOUBLE_TYPE_CODE;
            } else {
                throw new AssertionError("Illegal PArray type: " + this.x[k].getClass());
            }
        }
        this.destElementTypeCode = destElementTypeCode;
        this.arrayOfDoubleBuffersPool = JArrayPool.getInstance(double[].class, this.x.length);
        this.argsPool = JArrayPool.getInstance(double.class, this.x.length);
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
            int len = Math.min(count, ANY_FUNC_BUFFER_LENGTH);
            double[] args = (double[])argsPool.requestArray();
            double[][] doubleBufs = (double[][])arrayOfDoubleBuffersPool.requestArray();
            try {
                for (int k = 0; k < x.length; k++) {
                    double[] doubleBuf = doubleBufs[k] = (double[])DOUBLE_BUFFERS.requestArray();
                    long p = arrayPos;
                    boolean optimizeJArray = ja[k] != null;
                    if (optimizeJArray) {
                        p -= saShift[k];
                        if (p < 0) {
                            p += length;
                            if (p >= length - len) { // copied block is divided
                                optimizeJArray = false;
                            }
                        }
                    }
                    switch (srcElementTypeCode[k]) {
                        case ArraysFuncImpl.BIT_TYPE_CODE: {
                            boolean[] src = (boolean[])BOOLEAN_BUFFERS.requestArray();
                            try {
                                x[k].getData(arrayPos, src, 0, len);
                                for (int j = 0; j < len; j++) {
                                    doubleBuf[j] = src[j] ? 1.0 : 0.0;
                                }
                            } finally {
                                BOOLEAN_BUFFERS.releaseArray(src);
                            }
                            break;
                        }
                        //[[Repeat() char       ==> byte,,short,,int,,long,,float;;
                        //           Char       ==> Byte,,Short,,Int,,Long,,Float;;
                        //           CHAR       ==> BYTE,,SHORT,,INT,,LONG,,FLOAT;;
                        //           (src\[\w+\]) ==> ($1 & 0xFF),,($1 & 0xFFFF),,$1,,...]]
                        case ArraysFuncImpl.CHAR_TYPE_CODE: {
                            if (optimizeJArray) {
                                char[] src = (char[])ja[k];
                                int srcPos = (int)(p + subArrayOffset[k]);
                                for (int destPos = 0; destPos < len; srcPos++, destPos++) {
                                    doubleBuf[destPos] = src[srcPos];
                                }
                            } else {
                                char[] src = (char[])CHAR_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    for (int j = 0; j < len; j++) {
                                        doubleBuf[j] = src[j];
                                    }
                                } finally {
                                    CHAR_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        case ArraysFuncImpl.BYTE_TYPE_CODE: {
                            if (optimizeJArray) {
                                byte[] src = (byte[])ja[k];
                                int srcPos = (int)(p + subArrayOffset[k]);
                                for (int destPos = 0; destPos < len; srcPos++, destPos++) {
                                    doubleBuf[destPos] = (src[srcPos] & 0xFF);
                                }
                            } else {
                                byte[] src = (byte[])BYTE_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    for (int j = 0; j < len; j++) {
                                        doubleBuf[j] = (src[j] & 0xFF);
                                    }
                                } finally {
                                    BYTE_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.SHORT_TYPE_CODE: {
                            if (optimizeJArray) {
                                short[] src = (short[])ja[k];
                                int srcPos = (int)(p + subArrayOffset[k]);
                                for (int destPos = 0; destPos < len; srcPos++, destPos++) {
                                    doubleBuf[destPos] = (src[srcPos] & 0xFFFF);
                                }
                            } else {
                                short[] src = (short[])SHORT_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    for (int j = 0; j < len; j++) {
                                        doubleBuf[j] = (src[j] & 0xFFFF);
                                    }
                                } finally {
                                    SHORT_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.INT_TYPE_CODE: {
                            if (optimizeJArray) {
                                int[] src = (int[])ja[k];
                                int srcPos = (int)(p + subArrayOffset[k]);
                                for (int destPos = 0; destPos < len; srcPos++, destPos++) {
                                    doubleBuf[destPos] = src[srcPos];
                                }
                            } else {
                                int[] src = (int[])INT_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    for (int j = 0; j < len; j++) {
                                        doubleBuf[j] = src[j];
                                    }
                                } finally {
                                    INT_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.LONG_TYPE_CODE: {
                            if (optimizeJArray) {
                                long[] src = (long[])ja[k];
                                int srcPos = (int)(p + subArrayOffset[k]);
                                for (int destPos = 0; destPos < len; srcPos++, destPos++) {
                                    doubleBuf[destPos] = src[srcPos];
                                }
                            } else {
                                long[] src = (long[])LONG_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    for (int j = 0; j < len; j++) {
                                        doubleBuf[j] = src[j];
                                    }
                                } finally {
                                    LONG_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                            if (optimizeJArray) {
                                float[] src = (float[])ja[k];
                                int srcPos = (int)(p + subArrayOffset[k]);
                                for (int destPos = 0; destPos < len; srcPos++, destPos++) {
                                    doubleBuf[destPos] = src[srcPos];
                                }
                            } else {
                                float[] src = (float[])FLOAT_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    for (int j = 0; j < len; j++) {
                                        doubleBuf[j] = src[j];
                                    }
                                } finally {
                                    FLOAT_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        //[[Repeat.AutoGeneratedEnd]]
                        case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                            x[k].getData(arrayPos, doubleBuf, 0, len);
                            break;
                        }

                        default:
                            throw new AssertionError("Illegal srcElementTypeCode[" + k + "]");
                    }
                }

                switch (destElementTypeCode) {
                    case ArraysFuncImpl.BIT_TYPE_CODE: {
                        boolean[] dest = (boolean[])destArray;
                        for (int j = 0; j < len; j++, destArrayOffset++) {
                            for (int k = 0; k < args.length; k++) {
                                args[k] = doubleBufs[k][j];
                            }
                            double v = f.get(args);
                            dest[destArrayOffset] = v != 0.0;
                        }
                        break;
                    }
                    case ArraysFuncImpl.CHAR_TYPE_CODE: {
                        char[] dest = (char[])destArray;
                        for (int j = 0; j < len; j++, destArrayOffset++) {
                            for (int k = 0; k < args.length; k++) {
                                args[k] = doubleBufs[k][j];
                            }
                            double v = f.get(args);
                            if (truncateOverflows) {
                                dest[destArrayOffset] =
                                    v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                    (char)v;
                            } else {
                                dest[destArrayOffset] = (char)(long)v;
                            }
                        }
                        break;
                    }
                    //[[Repeat() byte ==> short;;
                    //           BYTE ==> SHORT;;
                    //           0xFF ==> 0xFFFF]]
                    case ArraysFuncImpl.BYTE_TYPE_CODE: {
                        byte[] dest = (byte[])destArray;
                        for (int j = 0; j < len; j++, destArrayOffset++) {
                            for (int k = 0; k < args.length; k++) {
                                args[k] = doubleBufs[k][j];
                            }
                            double v = f.get(args);
                            if (truncateOverflows) {
                                dest[destArrayOffset] = v < 0 ? (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                            } else {
                                dest[destArrayOffset] = (byte)(long)v;
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case ArraysFuncImpl.SHORT_TYPE_CODE: {
                        short[] dest = (short[])destArray;
                        for (int j = 0; j < len; j++, destArrayOffset++) {
                            for (int k = 0; k < args.length; k++) {
                                args[k] = doubleBufs[k][j];
                            }
                            double v = f.get(args);
                            if (truncateOverflows) {
                                dest[destArrayOffset] = v < 0 ? (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                            } else {
                                dest[destArrayOffset] = (short)(long)v;
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]
                    case ArraysFuncImpl.INT_TYPE_CODE: {
                        int[] dest = (int[])destArray;
                        for (int j = 0; j < len; j++, destArrayOffset++) {
                            for (int k = 0; k < args.length; k++) {
                                args[k] = doubleBufs[k][j];
                            }
                            double v = f.get(args);
                            if (truncateOverflows) {
                                dest[destArrayOffset] = (int)v;
                            } else {
                                dest[destArrayOffset] = (int)(long)v;
                            }
                        }
                        break;
                    }
                    //[[Repeat() long(?!Buf) ==> float,,double;;
                    //           LONG        ==> FLOAT,,DOUBLE;;
                    //           \(double\)  ==> ,, ]]
                    case ArraysFuncImpl.LONG_TYPE_CODE: {
                        long[] dest = (long[])destArray;
                        if (args.length == 1) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (long)f.get(doubleBufs[0][j]);
                            }
                        } else if (args.length == 2) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (long)f.get(doubleBufs[0][j], doubleBufs[1][j]);
                            }
                        } else if (args.length == 3) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (long)f.get(
                                    doubleBufs[0][j], doubleBufs[1][j], doubleBufs[2][j]);
                            }
                        } else {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                for (int k = 0; k < args.length; k++) {
                                    args[k] = doubleBufs[k][j];
                                }
                                dest[destArrayOffset] = (long)f.get(args);
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                        float[] dest = (float[])destArray;
                        if (args.length == 1) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)f.get(doubleBufs[0][j]);
                            }
                        } else if (args.length == 2) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)f.get(doubleBufs[0][j], doubleBufs[1][j]);
                            }
                        } else if (args.length == 3) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)f.get(
                                    doubleBufs[0][j], doubleBufs[1][j], doubleBufs[2][j]);
                            }
                        } else {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                for (int k = 0; k < args.length; k++) {
                                    args[k] = doubleBufs[k][j];
                                }
                                dest[destArrayOffset] = (float)f.get(args);
                            }
                        }
                        break;
                    }
                    case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                        double[] dest = (double[])destArray;
                        if (args.length == 1) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = f.get(doubleBufs[0][j]);
                            }
                        } else if (args.length == 2) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = f.get(doubleBufs[0][j], doubleBufs[1][j]);
                            }
                        } else if (args.length == 3) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = f.get(
                                    doubleBufs[0][j], doubleBufs[1][j], doubleBufs[2][j]);
                            }
                        } else {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                for (int k = 0; k < args.length; k++) {
                                    args[k] = doubleBufs[k][j];
                                }
                                dest[destArrayOffset] = f.get(args);
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]
                    default:
                        throw new AssertionError("Illegal destElementTypeCode");
                }
            } finally {
                for (int k = x.length - 1; k >= 0; k--) {
                    DOUBLE_BUFFERS.releaseArray(doubleBufs[k]);
                }
                arrayOfDoubleBuffersPool.releaseArray(doubleBufs);
                argsPool.releaseArray(args);
            }
            arrayPos += len;
            count -= len;
        }
    }
}
