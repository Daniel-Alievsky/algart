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

/*Repeat(INCLUDE_FROM_FILE, ArraysByteTableGetDataOp.java, main)
  Byte ==> Short ;;
  byte\[\]\s+data\s+= ==> short[] data = ;;
  \(byte\[\]\)\s*ArraysFuncImpl\.BYTE_BUFFERS ==> (short[]) ArraysFuncImpl.SHORT_BUFFERS ;;
  BYTE_BUFFERS ==> SHORT_BUFFERS ;;
  (data\[\w+\]\s*&\s*)0xFF ==> $10xFFFF ;;
  256 ==> 65536
     !! Auto-generated: NOT EDIT !! */

import net.algart.math.functions.Func;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays for one-argument functions and {@link ShortArray} argument.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysShortTableGetDataOp {
    private final ReentrantLock lock = new ReentrantLock();
    private final ShortArray x0;
    private final DataShortBuffer dBuf;
    boolean[] booleanTable;
    char[] charTable;
    byte[] byteTable;
    short[] shortTable;
    int[] intTable;
    long[] longTable;
    float[] floatTable;
    double[] doubleTable;
    private final int destElementTypeCode;

    ArraysShortTableGetDataOp(boolean truncateOverflows, ShortArray x0, Func f, int destElementTypeCode) {
        this.x0 = x0;
        this.dBuf = (DataShortBuffer) Arrays.bufferInternal(x0, DataBuffer.AccessMode.READ);
        // - necessary not only for performance, but also for the guarantee
        // that direct buffer really returns the references to the internal Java array
        switch (destElementTypeCode) {
            case ArraysFuncImpl.BIT_TYPE_CODE: {
                booleanTable = new boolean[65536];
                for (int k = 0; k < booleanTable.length; k++) {
                    booleanTable[k] = f.get(k) != 0.0;
                }
                break;
            }
            case ArraysFuncImpl.CHAR_TYPE_CODE: {
                charTable = new char[65536];
                if (truncateOverflows) {
                    for (int k = 0; k < charTable.length; k++) {
                        int v = (int) f.get(k);
                        charTable[k] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char) v;
                    }
                } else {
                    for (int k = 0; k < charTable.length; k++) {
                        charTable[k] = (char) (long) f.get(k);
                    }
                }
                break;
            }

            case ArraysFuncImpl.BYTE_TYPE_CODE: {
                byteTable = new byte[65536];
                if (truncateOverflows) {
                    for (int k = 0; k < byteTable.length; k++) {
                        int v = (int) f.get(k);
                        byteTable[k] = v < 0 ? (byte) 0 : v > 0xFF ? (byte) 0xFF : (byte) v;
                    }
                } else {
                    for (int k = 0; k < byteTable.length; k++) {
                        byteTable[k] = (byte) (long) f.get(k);
                    }
                }
                break;
            }

            case ArraysFuncImpl.SHORT_TYPE_CODE: {
                shortTable = new short[65536];
                if (truncateOverflows) {
                    for (int k = 0; k < shortTable.length; k++) {
                        int v = (int) f.get(k);
                        shortTable[k] = v < 0 ? (short) 0 : v > 0xFFFF ? (short) 0xFFFF : (short) v;
                    }
                } else {
                    for (int k = 0; k < shortTable.length; k++) {
                        shortTable[k] = (short) (long) f.get(k);
                    }
                }
                break;
            }

            case ArraysFuncImpl.INT_TYPE_CODE: {
                intTable = new int[65536];
                if (truncateOverflows) {
                    for (int k = 0; k < intTable.length; k++) {
                        intTable[k] = (int) f.get(k);
                    }
                } else {
                    for (int k = 0; k < intTable.length; k++) {
                        intTable[k] = (int) (long) f.get(k);
                    }
                }
                break;
            }

            case ArraysFuncImpl.LONG_TYPE_CODE: {
                longTable = new long[65536];
                for (int k = 0; k < longTable.length; k++) {
                    longTable[k] = (long) f.get(k);
                }
                break;
            }

            case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                floatTable = new float[65536];
                for (int k = 0; k < floatTable.length; k++) {
                    floatTable[k] = (float) f.get(k);
                }
                break;
            }
            case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                doubleTable = new double[65536];
                for (int k = 0; k < doubleTable.length; k++) {
                    doubleTable[k] = f.get(k);
                }
                break;
            }

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
            boolean usePool = false;
            short[] data = null;
            try {
                final int from, to;
                usePool = !dBuf.isDirect();
                if (usePool) {
                    data = (short[]) ArraysFuncImpl.SHORT_BUFFERS.requestArray();
                    len = Math.min(count, data.length);
                    x0.getData(arrayPos, data, 0, len);
                    from = 0;
                    to = len;
                } else {
                    // Synchronization is necessary to provide thread-safety.
                    // In this case, the cost of synchronization is low:
                    // buffer mapping work very quickly for direct buffers.
                    lock.lock();
                    try {
                        dBuf.map(arrayPos, count);
                        len = dBuf.cnt();
                        assert len == dBuf.count() : "too large buffer";
                        data = dBuf.data();
                        from = dBuf.from();
                        to = dBuf.to();
                    } finally {
                        // We may unlock here: data array is never modified for direct buffers.
                        lock.unlock();
                    }
                }
                switch (destElementTypeCode) {

                    case ArraysFuncImpl.BIT_TYPE_CODE: {
                        boolean[] dest = (boolean[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = booleanTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.CHAR_TYPE_CODE: {
                        char[] dest = (char[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = charTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.BYTE_TYPE_CODE: {
                        byte[] dest = (byte[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = byteTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.SHORT_TYPE_CODE: {
                        short[] dest = (short[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = shortTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.INT_TYPE_CODE: {
                        int[] dest = (int[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = intTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.LONG_TYPE_CODE: {
                        long[] dest = (long[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = longTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                        float[] dest = (float[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = floatTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                        double[] dest = (double[]) destArray;
                        for (int j = from; j < to; j++, destArrayOffset++) {
                            dest[destArrayOffset] = doubleTable[data[j] & 0xFFFF];
                        }
                        break;
                    }
                    default:
                        throw new AssertionError("Illegal destElementTypeCode");
                }
            } finally {
                if (usePool) {
                    ArraysFuncImpl.SHORT_BUFFERS.releaseArray(data);
                }
            }
            arrayPos += len;
            count -= len;
        }
    }
}

/*Repeat.IncludeEnd*/
