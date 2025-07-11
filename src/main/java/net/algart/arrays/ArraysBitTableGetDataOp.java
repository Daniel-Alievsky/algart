/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays for one-argument functions and {@link BitArray} argument.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysBitTableGetDataOp {
    private static final int BOOLEAN_BUFFER_LENGTH = 32768; // elements
    private static final JArrayPool BOOLEAN_BUFFERS = JArrayPool.getInstance(boolean.class, BOOLEAN_BUFFER_LENGTH);

    private final ReentrantLock lock = new ReentrantLock();
    private final BitArray x0;
    private final DataBitBuffer dbuf;
    boolean[] booleanTable;
    char[] charTable;
    byte[] byteTable;
    short[] shortTable;
    int[] intTable;
    long[] longTable;
    float[] floatTable;
    double[] doubleTable;
    private final int destElementTypeCode;

    ArraysBitTableGetDataOp(boolean truncateOverflows, BitArray x0, Func f, int destElementTypeCode) {
        this.x0 = x0;
        if (x0.nextQuickPosition(0) == -1) {
            this.dbuf = null;
        } else {
            this.dbuf = (DataBitBuffer) Arrays.bufferInternal(x0, DataBuffer.AccessMode.READ);
            // - necessary not only for performance, but also for the guarantee
            // that direct buffer really returns the references to the internal Java array
        }
        double[] w = {0.0, 1.0};
        switch (destElementTypeCode) {
            case ArraysFuncImpl.BIT_TYPE_CODE: {
                booleanTable = new boolean[2];
                for (int k = 0; k < w.length; k++) {
                    booleanTable[k] = f.get(w[k]) != 0.0;
                }
                break;
            }
            case ArraysFuncImpl.CHAR_TYPE_CODE: {
                charTable = new char[2];
                if (truncateOverflows) {
                    for (int k = 0; k < w.length; k++) {
                        int v = (int) f.get(w[k]);
                        charTable[k] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char) v;
                    }
                } else {
                    for (int k = 0; k < w.length; k++) {
                        charTable[k] = (char) (long) f.get(w[k]);
                    }
                }
                break;
            }
            //[[Repeat() byte ==> short;;
            //           BYTE ==> SHORT;;
            //           0xFF ==> 0xFFFF]]
            case ArraysFuncImpl.BYTE_TYPE_CODE: {
                byteTable = new byte[2];
                if (truncateOverflows) {
                    for (int k = 0; k < w.length; k++) {
                        int v = (int) f.get(w[k]);
                        byteTable[k] = v < 0 ? (byte) 0 : v > 0xFF ? (byte) 0xFF : (byte) v;
                    }
                } else {
                    for (int k = 0; k < w.length; k++) {
                        byteTable[k] = (byte) (long) f.get(w[k]);
                    }
                }
                break;
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            case ArraysFuncImpl.SHORT_TYPE_CODE: {
                shortTable = new short[2];
                if (truncateOverflows) {
                    for (int k = 0; k < w.length; k++) {
                        int v = (int) f.get(w[k]);
                        shortTable[k] = v < 0 ? (short) 0 : v > 0xFFFF ? (short) 0xFFFF : (short) v;
                    }
                } else {
                    for (int k = 0; k < w.length; k++) {
                        shortTable[k] = (short) (long) f.get(w[k]);
                    }
                }
                break;
            }
            //[[Repeat.AutoGeneratedEnd]]
            case ArraysFuncImpl.INT_TYPE_CODE: {
                intTable = new int[2];
                if (truncateOverflows) {
                    for (int k = 0; k < w.length; k++) {
                        intTable[k] = (int) f.get(w[k]);
                    }
                } else {
                    for (int k = 0; k < w.length; k++) {
                        intTable[k] = (int) (long) f.get(w[k]);
                    }
                }
                break;
            }
            //[[Repeat() long ==> float,,double;;
            //           LONG ==> FLOAT,,DOUBLE;;
            //           \(double\)\s+ ==> ,, ]]
            case ArraysFuncImpl.LONG_TYPE_CODE: {
                longTable = new long[2];
                for (int k = 0; k < w.length; k++) {
                    longTable[k] = (long) f.get(w[k]);
                }
                break;
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                floatTable = new float[2];
                for (int k = 0; k < w.length; k++) {
                    floatTable[k] = (float) f.get(w[k]);
                }
                break;
            }
            case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                doubleTable = new double[2];
                for (int k = 0; k < w.length; k++) {
                    doubleTable[k] = f.get(w[k]);
                }
                break;
            }
            //[[Repeat.AutoGeneratedEnd]]
        }
        this.destElementTypeCode = destElementTypeCode;
    }

    void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw AbstractArray.rangeException(arrayPos, x0.length(), x0.getClass());
        }
        if (arrayPos > x0.length() - count) {
            throw AbstractArray.rangeException(arrayPos + count - 1, x0.length(), x0.getClass());
        }
        while (count > 0) {
            int len;
            if (dbuf != null) {
                final long[] data;
                final long fromIndex;
                // Synchronization is necessary to provide thread-safety.
                // In this case, for DIRECT buffers (SimpleMemoryModel) the cost of synchronization is low:
                // buffer mapping work very quickly for this case.
                // In another case, synchronization is expensive enough (several CPUs cannot process the same array),
                // but it is not too popular situation and I do not optimize it.
                lock.lock();
                final boolean direct = dbuf.isDirect();
                boolean unlocked = false;
                try {
                    dbuf.map(arrayPos, count);
                    len = dbuf.cnt();
                    assert len == dbuf.count() : "too large buffer";
                    data = dbuf.data();
                    fromIndex = dbuf.fromIndex();
                    if (direct) {
                        // We may unlock here: data array is never modified for direct buffers.
                        unlocked = true;
                        lock.unlock();
                    }
                    switch (destElementTypeCode) {
                        //[[Repeat() BIT     ==> CHAR,,BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
                        //           boolean ==> char,,byte,,short,,int,,long,,float,,double]]
                        case ArraysFuncImpl.BIT_TYPE_CODE: {
                            boolean[] dest = (boolean[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    booleanTable[0], booleanTable[1]);
                            break;
                        }
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        case ArraysFuncImpl.CHAR_TYPE_CODE: {
                            char[] dest = (char[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    charTable[0], charTable[1]);
                            break;
                        }
                        case ArraysFuncImpl.BYTE_TYPE_CODE: {
                            byte[] dest = (byte[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    byteTable[0], byteTable[1]);
                            break;
                        }
                        case ArraysFuncImpl.SHORT_TYPE_CODE: {
                            short[] dest = (short[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    shortTable[0], shortTable[1]);
                            break;
                        }
                        case ArraysFuncImpl.INT_TYPE_CODE: {
                            int[] dest = (int[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    intTable[0], intTable[1]);
                            break;
                        }
                        case ArraysFuncImpl.LONG_TYPE_CODE: {
                            long[] dest = (long[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    longTable[0], longTable[1]);
                            break;
                        }
                        case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                            float[] dest = (float[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    floatTable[0], floatTable[1]);
                            break;
                        }
                        case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                            double[] dest = (double[]) destArray;
                            PackedBitArrays.unpackBits(dest, destArrayOffset, data, fromIndex, len,
                                    doubleTable[0], doubleTable[1]);
                            break;
                        }
                        //[[Repeat.AutoGeneratedEnd]]
                        default:
                            throw new AssertionError("Illegal destElementTypeCode");
                    }
                } finally {
                    if (!unlocked) {
                        lock.unlock();
                    }
                }
                destArrayOffset += len;
            } else {
                boolean[] data = (boolean[]) BOOLEAN_BUFFERS.requestArray();
                try {
                    len = Math.min(count, data.length);
                    x0.getData(arrayPos, data, 0, len);
                    switch (destElementTypeCode) {
                        //[[Repeat() BIT     ==> CHAR,,BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
                        //           boolean ==> char,,byte,,short,,int,,long,,float,,double]]
                        case ArraysFuncImpl.BIT_TYPE_CODE: {
                            boolean[] dest = (boolean[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? booleanTable[1] : booleanTable[0];
                            }
                            break;
                        }
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        case ArraysFuncImpl.CHAR_TYPE_CODE: {
                            char[] dest = (char[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? charTable[1] : charTable[0];
                            }
                            break;
                        }
                        case ArraysFuncImpl.BYTE_TYPE_CODE: {
                            byte[] dest = (byte[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? byteTable[1] : byteTable[0];
                            }
                            break;
                        }
                        case ArraysFuncImpl.SHORT_TYPE_CODE: {
                            short[] dest = (short[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? shortTable[1] : shortTable[0];
                            }
                            break;
                        }
                        case ArraysFuncImpl.INT_TYPE_CODE: {
                            int[] dest = (int[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? intTable[1] : intTable[0];
                            }
                            break;
                        }
                        case ArraysFuncImpl.LONG_TYPE_CODE: {
                            long[] dest = (long[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? longTable[1] : longTable[0];
                            }
                            break;
                        }
                        case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                            float[] dest = (float[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? floatTable[1] : floatTable[0];
                            }
                            break;
                        }
                        case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                            double[] dest = (double[]) destArray;
                            for (int j = 0; j < len; j++, destArrayOffset++) {
                                dest[destArrayOffset] = data[j] ? doubleTable[1] : doubleTable[0];
                            }
                            break;
                        }
                        //[[Repeat.AutoGeneratedEnd]]
                        default:
                            throw new AssertionError("Illegal destElementTypeCode");
                    }
                } finally {
                    BOOLEAN_BUFFERS.releaseArray(data);
                }
            }
            arrayPos += len;
            count -= len;
        }
    }
}
