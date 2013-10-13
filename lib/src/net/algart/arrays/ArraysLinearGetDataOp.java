package net.algart.arrays;

import net.algart.arrays.BufferArraysImpl.AbstractBufferArray;
import net.algart.math.functions.LinearFunc;

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays for linear functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
strictfp class ArraysLinearGetDataOp {
    static final int LINEAR_BUFFER_LENGTH = 16384; // elements (double[] + elementType[])
    private static final boolean OPTIMIZE_LINEAR_FOR_JARRAYS = true;
    private static final boolean OPTIMIZE_LINEAR_FOR_JBUFFERS = InternalUtils.SERVER_OPTIMIZATION;

    static final JArrayPool BOOLEAN_BUFFERS = JArrayPool.getInstance(boolean.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool CHAR_BUFFERS = JArrayPool.getInstance(char.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, LINEAR_BUFFER_LENGTH);
    static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, LINEAR_BUFFER_LENGTH);

    private final boolean truncateOverflows;
    private final PArray[] x;
    private final long length;

    private final double a0;
    private final double b;
    private final double[] a;
    private final boolean isNonweightedSum;
    private final boolean isCast;
    private final Object[] jaOrDStor; // every non-null element contains either DataStorages.Storage or Java array
    private final long[] saShift;
    private final long[] subArrayOffset;
    private final int[] srcElementTypeCode;
    private final int destElementTypeCode;
    private final boolean intBufferForSum;

    ArraysLinearGetDataOp(boolean truncateOverflows, PArray[] x, LinearFunc lf, int destElementTypeCode) {
        if (lf == null)
            throw new AssertionError("Null lf argument");
        if (lf.n() == 0)
            throw new AssertionError("No coefficients in the passed function " + lf);
        this.truncateOverflows = truncateOverflows;
        this.x = new PArray[lf.n()];
        System.arraycopy(x, 0, this.x, 0, this.x.length);
        this.length = this.x[0].length();
        for (PArray xk : this.x) {
            if (xk.length() != this.length)
                throw new AssertionError("Different x[] lengths");
        }
        this.a0 =lf.a(0);
        this.a = lf.a();
        if (this.a.length != lf.n())
            throw new AssertionError("Illegal implementation of LinearFunc: n()!=a().length");
        this.isNonweightedSum = a.length > 1 && a0 != 0.0 && lf.isNonweighted();
        double bTemp = lf.b();
        if (this.isNonweightedSum) {
            for (int k = 0; k < a.length; k++)
                this.a[k] = 1.0; // don't damage source a[]!
            bTemp /= a0;
        }
        this.b = bTemp;
        this.isCast = !this.isNonweightedSum && this.a0 == 1.0 && this.b == 0.0;
        this.jaOrDStor = new Object[this.x.length];
        this.subArrayOffset = new long[this.x.length];
        this.saShift = new long[this.x.length];
        for (int k = 0; k < this.x.length; k++) {
            Array array = this.x[k];
            if (Arrays.isShifted(array)) {
                saShift[k] = Arrays.getShift(array);
                array = Arrays.getUnderlyingArrays(array)[0];
            }
            if (OPTIMIZE_LINEAR_FOR_JARRAYS) {
                if (array instanceof BitArray) {
                    this.jaOrDStor[k] = Arrays.longJavaArrayInternal((BitArray)array);
                } else {
                    this.jaOrDStor[k] = Arrays.javaArrayInternal(array);
                }
                if (this.jaOrDStor[k] != null) {
                    if (array instanceof BitArray) {
                        this.subArrayOffset[k] = Arrays.longJavaArrayOffsetInternal((BitArray)array);
                    } else {
                        this.subArrayOffset[k] = Arrays.javaArrayOffsetInternal(array);
                    }
                }
            }
            if (OPTIMIZE_LINEAR_FOR_JBUFFERS && this.jaOrDStor[k] == null) {
                if (array instanceof AbstractBufferArray && !(array instanceof BitArray)) {
                    this.jaOrDStor[k] = ((AbstractBufferArray)array).storage;
                    this.subArrayOffset[k] = ((AbstractBufferArray)array).offset;
                }
            }
        }
        this.srcElementTypeCode = new int[a.length];
        boolean only16BitsOrLess = true;
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
                only16BitsOrLess = false;
            } else if (this.x[k] instanceof LongArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.LONG_TYPE_CODE;
                only16BitsOrLess = false;
            } else if (this.x[k] instanceof FloatArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.FLOAT_TYPE_CODE;
                only16BitsOrLess = false;
            } else if (this.x[k] instanceof DoubleArray) {
                this.srcElementTypeCode[k] = ArraysFuncImpl.DOUBLE_TYPE_CODE;
                only16BitsOrLess = false;
            } else {
                throw new AssertionError("Illegal PArray type: " + this.x[k].getClass());
            }
        }
        this.destElementTypeCode = destElementTypeCode;
        this.intBufferForSum = this.isNonweightedSum && only16BitsOrLess && a.length < Short.MAX_VALUE;
        // it means that the sum of a.length 16-bit values (or numbers with lower precision)
        // can be represented precisely by "int" type
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
            int len = Math.min(count, LINEAR_BUFFER_LENGTH);
            int[] intBuf = null;
            double[] doubleBuf = null;
            try {
                if (intBufferForSum) {
                    intBuf = (int[])INT_BUFFERS.requestArray();
                } else {
                    doubleBuf = (double[])DOUBLE_BUFFERS.requestArray();
                    if (isNonweightedSum) {
                        JArrays.fillDoubleArray(doubleBuf, 0, len, b);
                    }
                }
                for (int k = 0; k < x.length; k++) {
                    long p = arrayPos;
                    boolean optimizeJBuffer = jaOrDStor[k] instanceof DataStorage;
                    boolean optimizeJArray = !optimizeJBuffer && jaOrDStor[k] != null;
                    if (optimizeJArray || optimizeJBuffer) {
                        p -= saShift[k];
                        if (p < 0) {
                            p += length;
                            if (p >= length - len) { // copied block is divided
                                optimizeJArray = optimizeJBuffer = false;
                            }
                        }
                    }
                    switch (srcElementTypeCode[k]) {
                        case ArraysFuncImpl.BIT_TYPE_CODE: {
                            if (k > 0 && optimizeJArray && isNonweightedSum && intBuf != null) {
                                long[] src = (long[])jaOrDStor[k];
                                long srcOffset = p + subArrayOffset[k];
                                PackedBitArrays.addBitsToInts(intBuf, 0, src, srcOffset, len);
                            } else {
                                boolean[] src = (boolean[])BOOLEAN_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            if (k == 0) {
                                                for (int j = 0; j < len; j++)
                                                    intBuf[j] = src[j] ? 1 : 0;
                                            } else {
                                                for (int j = 0; j < len; j++)
                                                    if (src[j])
                                                        intBuf[j]++;
                                            }
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                if (src[j])
                                                    doubleBuf[j]++;
                                        }
                                    } else {
                                        if (k == 0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j] ? a[0] + b : b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                if (src[j])
                                                    doubleBuf[j] += a[k];
                                        }
                                    }
                                } finally {
                                    BOOLEAN_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        //[[Repeat() char       ==> byte,,short,,int,,long,,float,,double;;
                        //           Char       ==> Byte,,Short,,Int,,Long,,Float,,Double;;
                        //           CHAR       ==> BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
                        //           (src\[j\]) ==> ($1 & 0xFF),,($1 & 0xFFFF),,$1,,...;;
                        //           (//Start_intBuf.*?//End_intBuf.*?)(\r(?!\n)|\n|\r\n) ==>
                        //           $1$2,,$1$2,,throw new AssertionError("Illegal intBuf usage");$2,,...]]
                        case ArraysFuncImpl.CHAR_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        storage.addData(p + subArrayOffset[k], intBuf, 0, len);
                                        //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                char[] src = (char[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        JArrays.addCharArray(intBuf, 0, src, srcOffset, len);
                                        //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                    } else {
                                        JArrays.addCharArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addCharArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                char[] src = (char[])CHAR_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                            if (k == 0) {
                                                for (int j = 0; j < len; j++)
                                                    intBuf[j] = src[j];
                                            } else {
                                                JArrays.addCharArray(intBuf, 0, src, 0, len);
                                            }
                                            //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += src[j];
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j];
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j] + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * src[j] + b;
                                        }
                                    } else {
                                        JArrays.addCharArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    CHAR_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        case ArraysFuncImpl.BYTE_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        storage.addData(p + subArrayOffset[k], intBuf, 0, len);
                                        //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                byte[] src = (byte[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        JArrays.addByteArray(intBuf, 0, src, srcOffset, len);
                                        //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                    } else {
                                        JArrays.addByteArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addByteArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                byte[] src = (byte[])BYTE_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                            if (k == 0) {
                                                for (int j = 0; j < len; j++)
                                                    intBuf[j] = (src[j] & 0xFF);
                                            } else {
                                                JArrays.addByteArray(intBuf, 0, src, 0, len);
                                            }
                                            //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += (src[j] & 0xFF);
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = (src[j] & 0xFF);
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = (src[j] & 0xFF) + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * (src[j] & 0xFF) + b;
                                        }
                                    } else {
                                        JArrays.addByteArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    BYTE_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.SHORT_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        storage.addData(p + subArrayOffset[k], intBuf, 0, len);
                                        //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                short[] src = (short[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        JArrays.addShortArray(intBuf, 0, src, srcOffset, len);
                                        //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                    } else {
                                        JArrays.addShortArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addShortArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                short[] src = (short[])SHORT_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            //Start_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                            if (k == 0) {
                                                for (int j = 0; j < len; j++)
                                                    intBuf[j] = (src[j] & 0xFFFF);
                                            } else {
                                                JArrays.addShortArray(intBuf, 0, src, 0, len);
                                            }
                                            //End_intBuf !! this comment is necessary for preprocessing by Repeater !!
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += (src[j] & 0xFFFF);
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = (src[j] & 0xFFFF);
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = (src[j] & 0xFFFF) + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * (src[j] & 0xFFFF) + b;
                                        }
                                    } else {
                                        JArrays.addShortArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    SHORT_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.INT_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                int[] src = (int[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        JArrays.addIntArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addIntArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                int[] src = (int[])INT_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            throw new AssertionError("Illegal intBuf usage");
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += src[j];
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j];
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j] + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * src[j] + b;
                                        }
                                    } else {
                                        JArrays.addIntArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    INT_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.LONG_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                long[] src = (long[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        JArrays.addLongArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addLongArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                long[] src = (long[])LONG_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            throw new AssertionError("Illegal intBuf usage");
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += src[j];
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j];
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j] + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * src[j] + b;
                                        }
                                    } else {
                                        JArrays.addLongArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    LONG_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                float[] src = (float[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        JArrays.addFloatArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addFloatArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                float[] src = (float[])FLOAT_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            throw new AssertionError("Illegal intBuf usage");
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += src[j];
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j];
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j] + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * src[j] + b;
                                        }
                                    } else {
                                        JArrays.addFloatArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    FLOAT_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                            if (k > 0 && optimizeJBuffer) {
                                DataStorage storage = (DataStorage)jaOrDStor[k];
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, 1.0);
                                    }
                                } else {
                                    storage.addData(p + subArrayOffset[k], doubleBuf, 0, len, a[k]);
                                }
                            } else if (k > 0 && optimizeJArray) {
                                double[] src = (double[])jaOrDStor[k];
                                int srcOffset = (int)(p + subArrayOffset[k]);
                                if (isNonweightedSum) {
                                    if (intBuf != null) {
                                        throw new AssertionError("Illegal intBuf usage");
                                    } else {
                                        JArrays.addDoubleArray(doubleBuf, 0, src, srcOffset, len, 1.0);
                                    }
                                } else {
                                    JArrays.addDoubleArray(doubleBuf, 0, src, srcOffset, len, a[k]);
                                }
                            } else {
                                double[] src = (double[])DOUBLE_BUFFERS.requestArray();
                                try {
                                    x[k].getData(arrayPos, src, 0, len);
                                    if (isNonweightedSum) {
                                        if (intBuf != null) {
                                            throw new AssertionError("Illegal intBuf usage");
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] += src[j];
                                        }
                                    } else if (k == 0) {
                                        if (isCast) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j];
                                        } else if (a0 == 1.0) {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = src[j] + b;
                                        } else {
                                            for (int j = 0; j < len; j++)
                                                doubleBuf[j] = a0 * src[j] + b;
                                        }
                                    } else {
                                        JArrays.addDoubleArray(doubleBuf, 0, src, 0, len, a[k]);
                                    }
                                } finally {
                                    DOUBLE_BUFFERS.releaseArray(src);
                                }
                            }
                            break;
                        }
                        //[[Repeat.AutoGeneratedEnd]]
                        default:
                            throw new AssertionError("Illegal srcElementTypeCode[" + k + "]");
                    }
                }

                switch (destElementTypeCode) {
                    case ArraysFuncImpl.BIT_TYPE_CODE: {
                        boolean[] dest = (boolean[])destArray;
                        if (intBuf != null) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = intBuf[j] + b != 0.0;
                            }
                        } else {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = doubleBuf[j] != 0.0;
                            }
                        }
                        break;
                    }
                    case ArraysFuncImpl.CHAR_TYPE_CODE: {
                        char[] dest = (char[])destArray;
                        if (truncateOverflows) {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)((intBuf[j] + b) * a0);
                                    dest[destArrayOffset] =
                                        v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                        v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)(doubleBuf[j] * a0);
                                    dest[destArrayOffset] =
                                        v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                        v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)doubleBuf[j];
                                    dest[destArrayOffset] =
                                        v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                        v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                                }
                            }
                        } else {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (char)(long)((intBuf[j] + b) * a0);
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (char)(long)(doubleBuf[j] * a0);
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (char)(long)doubleBuf[j];
                                }
                            }
                        }
                        break;
                    }
                    //[[Repeat() byte ==> short;;
                    //           BYTE ==> SHORT;;
                    //           0xFF ==> 0xFFFF]]
                    case ArraysFuncImpl.BYTE_TYPE_CODE: {
                        byte[] dest = (byte[])destArray;
                        if (truncateOverflows) {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                if (b == 0.0 && a0 == 1.0) {
                                    for (int j = 0; j < len; j++, destArrayOffset++) {
                                        int v = intBuf[j];
                                        dest[destArrayOffset] = v < 0 ?
                                            (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destArrayOffset++) {
                                        int v = (int)((intBuf[j] + b) * a0);
                                        dest[destArrayOffset] = v < 0 ?
                                            (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                                    }
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)(doubleBuf[j] * a0);
                                    dest[destArrayOffset] = v < 0 ?
                                        (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)doubleBuf[j];
                                    dest[destArrayOffset] = v < 0 ?
                                        (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                                }
                            }
                        } else {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (byte)(long)((intBuf[j] + b) * a0);
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (byte)(long)(doubleBuf[j] * a0);
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (byte)(long)doubleBuf[j];
                                }
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case ArraysFuncImpl.SHORT_TYPE_CODE: {
                        short[] dest = (short[])destArray;
                        if (truncateOverflows) {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                if (b == 0.0 && a0 == 1.0) {
                                    for (int j = 0; j < len; j++, destArrayOffset++) {
                                        int v = intBuf[j];
                                        dest[destArrayOffset] = v < 0 ?
                                            (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                                    }
                                } else {
                                    for (int j = 0; j < len; j++, destArrayOffset++) {
                                        int v = (int)((intBuf[j] + b) * a0);
                                        dest[destArrayOffset] = v < 0 ?
                                            (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                                    }
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)(doubleBuf[j] * a0);
                                    dest[destArrayOffset] = v < 0 ?
                                        (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    int v = (int)doubleBuf[j];
                                    dest[destArrayOffset] = v < 0 ?
                                        (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                                }
                            }
                        } else {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (short)(long)((intBuf[j] + b) * a0);
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (short)(long)(doubleBuf[j] * a0);
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (short)(long)doubleBuf[j];
                                }
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]
                    case ArraysFuncImpl.INT_TYPE_CODE: {
                        int[] dest = (int[])destArray;
                        if (truncateOverflows) {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                if (b == 0.0 && a0 == 1.0) {
                                    System.arraycopy(intBuf, 0, dest, destArrayOffset, len);
                                    destArrayOffset += len;
                                } else {
                                    for (int j = 0; j < len; j++, destArrayOffset++) {
                                        dest[destArrayOffset] = (int)((intBuf[j] + b) * a0);
                                    }
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (int)(doubleBuf[j] * a0);
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (int)doubleBuf[j];
                                }
                            }
                        } else {
                            if (intBuf != null) {
                                assert isNonweightedSum;
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (int)(long)((intBuf[j] + b) * a0);
                                }
                            } else if (isNonweightedSum && a0 != 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (int)(long)(doubleBuf[j] * a0);
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (int)(long)doubleBuf[j];
                                }
                            }
                        }
                        break;
                    }
                    //[[Repeat() long(?!Buf) ==> float;;
                    //           LONG        ==> FLOAT]]
                    case ArraysFuncImpl.LONG_TYPE_CODE: {
                        long[] dest = (long[])destArray;
                        if (intBuf != null) {
                            assert isNonweightedSum;
                            if (b == 0.0 && a0 == 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = intBuf[j];
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (long)((intBuf[j] + b) * a0);
                                }
                            }
                        } else if (isNonweightedSum && a0 != 1.0) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (long)(doubleBuf[j] * a0);
                            }
                        } else {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (long)doubleBuf[j];
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                        float[] dest = (float[])destArray;
                        if (intBuf != null) {
                            assert isNonweightedSum;
                            if (b == 0.0 && a0 == 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = intBuf[j];
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (float)((intBuf[j] + b) * a0);
                                }
                            }
                        } else if (isNonweightedSum && a0 != 1.0) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)(doubleBuf[j] * a0);
                            }
                        } else {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)doubleBuf[j];
                            }
                        }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]
                    case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                        double[] dest = (double[])destArray;
                        if (intBuf != null) {
                            assert isNonweightedSum;
                            if (b == 0.0 && a0 == 1.0) {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = intBuf[j];
                                }
                            } else {
                                for (int j = 0; j < len; j++, destArrayOffset++) {
                                    dest[destArrayOffset] = (intBuf[j] + b) * a0;
                                }
                            }
                        } else if (isNonweightedSum && a0 != 1.0) {
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = doubleBuf[j] * a0;
                            }
                        } else {
                            System.arraycopy(doubleBuf, 0, dest, destArrayOffset, len);
                            destArrayOffset += len;
                        }
                        break;
                    }
                    default:
                        throw new AssertionError("Illegal destElementTypeCode");
                }
            } finally {
                if (intBufferForSum) {
                    INT_BUFFERS.releaseArray(intBuf);
                } else {
                    DOUBLE_BUFFERS.releaseArray(doubleBuf);
                }
            }
            arrayPos += len;
            count -= len;
        }
    }
}
