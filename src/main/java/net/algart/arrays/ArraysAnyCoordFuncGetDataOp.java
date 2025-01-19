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

/**
 * <p>Implementation of {@link Array#getData(long, Object, int, int)} methods
 * in the custom implementations of functional arrays, created by
 * {@link Arrays#asIndexFuncArray(boolean, Func, Class, long)} method, for any functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysAnyCoordFuncGetDataOp {
    private final boolean truncateOverflows;
    private final long[] dim;
    private final long dimX;
    private final long dimXY;
    private final long length;
    private final PArray result;

    private final Func f;
    private final int destElementTypeCode;

    ArraysAnyCoordFuncGetDataOp(PArray result,
        boolean truncateOverflows, long[] dim, Func f, int destElementTypeCode)
    {
        this.truncateOverflows = truncateOverflows;
        this.dim = dim;
        this.dimX = dim[0];
        this.dimXY = dim.length > 1 ? dim[0] * dim[1] : dim[0];
        this.length = result.length();
        this.result = result;
        assert Arrays.longMul(dim) == result.length();
        this.f = f;
        this.destElementTypeCode = destElementTypeCode;
    }

    void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw AbstractArray.rangeException(arrayPos, length, result.getClass());
        }
        if (arrayPos > length - count) {
            throw AbstractArray.rangeException(arrayPos + count - 1, length, result.getClass());
        }
        final int destArrayOffsetMax = destArrayOffset + count;
        double[] coordinates = new double[dim.length];
        switch (destElementTypeCode) {
            case ArraysFuncImpl.BIT_TYPE_CODE: {
                boolean[] dest = (boolean[])destArray;
                if (dim.length == 1) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos);
                        dest[destArrayOffset] = v != 0.0;
                    }
                } else if (dim.length == 2) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos / dimX);
                        dest[destArrayOffset] = v != 0.0;
                    }
                } else if (dim.length == 3) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                        dest[destArrayOffset] = v != 0.0;
                    }
                } else {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                        double v = f.get(coordinates);
                        dest[destArrayOffset] = v != 0.0;
                    }
                }
                break;
            }
            case ArraysFuncImpl.CHAR_TYPE_CODE: {
                char[] dest = (char[])destArray;
                if (truncateOverflows) {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] =
                                v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] =
                                v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] =
                                v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] =
                                v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                        }
                    }
                } else {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = (char)(long)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = (char)(long)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = (char)(long)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = (char)(long)v;
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
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = v < 0 ? (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = v < 0 ? (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = v < 0 ? (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = v < 0 ? (byte)0 : v > 0xFF ? (byte)0xFF : (byte)v;
                        }
                    }
                } else {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = (byte)(long)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = (byte)(long)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = (byte)(long)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = (byte)(long)v;
                        }
                    }
                }
                break;
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            case ArraysFuncImpl.SHORT_TYPE_CODE: {
                short[] dest = (short[])destArray;
                if (truncateOverflows) {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = v < 0 ? (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = v < 0 ? (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = v < 0 ? (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = v < 0 ? (short)0 : v > 0xFFFF ? (short)0xFFFF : (short)v;
                        }
                    }
                } else {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = (short)(long)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = (short)(long)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = (short)(long)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = (short)(long)v;
                        }
                    }
                }
                break;
            }
            //[[Repeat.AutoGeneratedEnd]]
            case ArraysFuncImpl.INT_TYPE_CODE: {
                int[] dest = (int[])destArray;
                if (truncateOverflows) {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = (int)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = (int)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = (int)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = (int)v;
                        }
                    }
                } else {
                    if (dim.length == 1) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos);
                            dest[destArrayOffset] = (int)(long)v;
                        }
                    } else if (dim.length == 2) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos / dimX);
                            dest[destArrayOffset] = (int)(long)v;
                        }
                    } else if (dim.length == 3) {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                            dest[destArrayOffset] = (int)(long)v;
                        }
                    } else {
                        for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                            ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                            double v = f.get(coordinates);
                            dest[destArrayOffset] = (int)(long)v;
                        }
                    }
                }
                break;
            }
            //[[Repeat() long(?!Buf)   ==> float,,double;;
            //           LONG          ==> FLOAT,,DOUBLE;;
            //           (\(double\)v) ==> $1,,v]]
            case ArraysFuncImpl.LONG_TYPE_CODE: {
                long[] dest = (long[])destArray;
                if (dim.length == 1) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos);
                        dest[destArrayOffset] = (long)v;
                    }
                } else if (dim.length == 2) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos / dimX);
                        dest[destArrayOffset] = (long)v;
                    }
                } else if (dim.length == 3) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                        dest[destArrayOffset] = (long)v;
                    }
                } else {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                        double v = f.get(coordinates);
                        dest[destArrayOffset] = (long)v;
                    }
                }
                break;
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            case ArraysFuncImpl.FLOAT_TYPE_CODE: {
                float[] dest = (float[])destArray;
                if (dim.length == 1) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos);
                        dest[destArrayOffset] = (float)v;
                    }
                } else if (dim.length == 2) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos / dimX);
                        dest[destArrayOffset] = (float)v;
                    }
                } else if (dim.length == 3) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                        dest[destArrayOffset] = (float)v;
                    }
                } else {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                        double v = f.get(coordinates);
                        dest[destArrayOffset] = (float)v;
                    }
                }
                break;
            }
            case ArraysFuncImpl.DOUBLE_TYPE_CODE: {
                double[] dest = (double[])destArray;
                if (dim.length == 1) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos);
                        dest[destArrayOffset] = v;
                    }
                } else if (dim.length == 2) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos / dimX);
                        dest[destArrayOffset] = v;
                    }
                } else if (dim.length == 3) {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        double v = f.get(arrayPos % dimX, arrayPos % dimXY / dimX, arrayPos / dimXY);
                        dest[destArrayOffset] = v;
                    }
                } else {
                    for (; destArrayOffset < destArrayOffsetMax; destArrayOffset++, arrayPos++) {
                        ArraysFuncImpl.coordinatesInDoubles(arrayPos, dim, coordinates);
                        double v = f.get(coordinates);
                        dest[destArrayOffset] = v;
                    }
                }
                break;
            }
            //[[Repeat.AutoGeneratedEnd]]
            default:
                throw new AssertionError("Illegal destElementTypeCode");
        }
    }
}
