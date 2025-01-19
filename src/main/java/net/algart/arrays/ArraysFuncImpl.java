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

import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Implementations of {@link Arrays} methods making functional arrays.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysFuncImpl {
    private static final long MIN_LENGTH_OF_SHORT_ARRAYS_FOR_TABLE_OPTIMIZATION = 65536;

    static final int BUFFER_SIZE = 65536; // bytes, or byte pairs for 64-bit types; must be <2^31
    static final int BIT_BUFFER_LENGTH = 64 * 8192; // bits; must be <2^31
    static final int BITS_GAP = 256; // a little gap for better bits alignment; must be 2^k and >=64

    static final JArrayPool BIT_BUFFERS = JArrayPool.getInstance(long.class, (BIT_BUFFER_LENGTH + BITS_GAP) / 64 + 1);
    static final JArrayPool CHAR_BUFFERS = JArrayPool.getInstance(char.class, BUFFER_SIZE / 2);
    static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, BUFFER_SIZE);
    static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, BUFFER_SIZE / 2);
    static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, BUFFER_SIZE / 4);
    static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, BUFFER_SIZE / 4);
    static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, BUFFER_SIZE / 4);
    static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, BUFFER_SIZE / 4);

    private static final JArrayPool SMALL_LONG_BUFFERS = JArrayPool.getInstance(long.class, 256);

    static JArrayPool smallLongBuffers(int length) { // for little service arrays
        if (length <= SMALL_LONG_BUFFERS.arrayLength()) {
            return SMALL_LONG_BUFFERS;
        } else {
            return JArrayPool.getInstance(long.class, length);
        }
    }

    // Note: the order of the following constants - bit, char, byte, short, int, long, float, double - is important!
    // For example, sometimes we check whether the type is not floating-point by comparison "c<=LONG_TYPE_CODE".
    static final int BIT_TYPE_CODE = 1;
    static final int CHAR_TYPE_CODE = 2;
    static final int BYTE_TYPE_CODE = 3;
    static final int SHORT_TYPE_CODE = 4;
    static final int INT_TYPE_CODE = 5;
    static final int LONG_TYPE_CODE = 6;
    static final int FLOAT_TYPE_CODE = 7;
    static final int DOUBLE_TYPE_CODE = 8;

    static <T extends PArray> T asCoordFuncMatrix(
        final boolean truncateOverflows,
        Func f, Class<? extends T> requiredType, long[] dim)
    {
        Objects.requireNonNull(f, "Null f argument");
        Objects.requireNonNull(requiredType, "Null requiredType argument");
        if (UpdatableArray.class.isAssignableFrom(requiredType)) {
            throw new IllegalArgumentException("requiredType, " + requiredType + ", must not be updatable");
        }
        long len = MatrixImpl.checkDimensions(dim);
        if (f instanceof ConstantFunc) {
            return asConstantFuncArray(truncateOverflows, requiredType, f, len);
        }
        if (f instanceof ArraysInterpolationsImpl.AbstractInterpolation) {
            // all implementations of that class, when called for integer coordinates, just return the source elements
            Matrix<? extends PArray> mParent = ((ArraysInterpolationsImpl.AbstractInterpolation)f).m;
            if (!java.util.Arrays.equals(mParent.dimensions(), dim)) {
                if (Matrices.isOnlyInsideInterpolationFunc(f)) {
                    mParent = mParent.subMatrix(new long[dim.length], dim);
                    // here is a little change in the behavior: IndexOutOfBoundException is thrown immediately,
                    // when the source array leads to this exception only while accessing elements
                } else {
                    mParent = mParent.subMatrix(new long[dim.length], dim,
                        Matrix.ContinuationMode.getConstantMode(Matrices.getOutsideValue(f)));
                }
            }
            if (mParent.elementType() == Arrays.elementType(requiredType)) {
                return InternalUtils.<T>cast(mParent.array().asImmutable());
            }
            return asIdentityFuncArray(truncateOverflows, requiredType, Func.IDENTITY, mParent.array());
        }
        if (Arrays.isBitType(requiredType)) {
            if (dim.length == 1) {
                return InternalUtils.<T>cast(
                    new CoordFuncBitArray(truncateOverflows, len, f, dim) {
                        public boolean getBit(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return this.f.get(index) != 0.0;
                        }
                    });
            } else if (dim.length == 2) {
                return InternalUtils.<T>cast(
                    new CoordFuncBitArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0];

                        public boolean getBit(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return this.f.get(index % dimX, index / dimX) != 0.0;
                        }
                    });
            } else if (dim.length == 3) {
                return InternalUtils.<T>cast(
                    new CoordFuncBitArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0],
                            dimXY = dimX * dim[1];

                        public boolean getBit(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return this.f.get(index % dimX, index % dimXY / dimX, index / dimXY) != 0.0;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new CoordFuncBitArray(truncateOverflows, len, f, dim) {
                        public boolean getBit(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            double[] coordinates = new double[dim.length];
                            coordinatesInDoubles(index, dim, coordinates);
                            return this.f.get(coordinates) != 0.0;
                        }
                    });
            }
        }

        if (Arrays.isCharType(requiredType)) {
            if (truncateOverflows) {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index);
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index % dimX, index / dimX);
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                int v = (int)this.f.get(coordinates);
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                }
            } else {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (char)(long)this.f.get(index);
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (char)(long)this.f.get(index % dimX, index / dimX);
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (char)(long)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncCharArray(truncateOverflows, len, f, dim) {
                            public char getChar(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                return (char)(long)this.f.get(coordinates);
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                }
            }
        }
        //[[Repeat() byte               ==> short;;
        //           isByteType         ==> isShortType;;
        //           FuncByteArray      ==> FuncShortArray;;
        //           getByte(?!\(index) ==> getShort;;
        //           BYTE               ==> SHORT;;
        //           0xFF(?![\]Ff])     ==> 0xFFFF]]
        if (Arrays.isByteType(requiredType)) {
            if (truncateOverflows) {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index);
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index % dimX, index / dimX);
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                int v = (int)this.f.get(coordinates);
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                }
            } else {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index % dimX, index / dimX) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public int getByte(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncByteArray(truncateOverflows, len, f, dim) {
                            public int getByte(long index) {
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                return (int)(long)this.f.get(coordinates) & 0xFF;
                            }
                        });
                }
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isShortType(requiredType)) {
            if (truncateOverflows) {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index);
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index % dimX, index / dimX);
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                int v = (int)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                int v = (int)this.f.get(coordinates);
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                }
            } else {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index % dimX, index / dimX) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public int getShort(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncShortArray(truncateOverflows, len, f, dim) {
                            public int getShort(long index) {
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                return (int)(long)this.f.get(coordinates) & 0xFFFF;
                            }
                        });
                }
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        if (Arrays.isIntType(requiredType)) {
            if (truncateOverflows) {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)this.f.get(index);
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)this.f.get(index % dimX, index / dimX);
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                return (int)this.f.get(coordinates);
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                }
            } else {
                if (dim.length == 1) {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index);
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 2) {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0];

                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index % dimX, index / dimX);
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (dim.length == 3) {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            final long dimX = dim[0],
                                dimXY = dimX * dim[1];

                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                return (int)(long)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new CoordFuncIntArray(truncateOverflows, len, f, dim) {
                            public int getInt(long index) {
                                if (index < 0 || index >= length) {
                                    throw rangeException(index);
                                }
                                double[] coordinates = new double[dim.length];
                                coordinatesInDoubles(index, dim, coordinates);
                                return (int)(long)this.f.get(coordinates);
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                }
            }
        }

        //[[Repeat() long(?!\s+(index|arrayPos|dimX)) ==> float,,double;;
        //           Long                             ==> Float,,Double;;
        //           LONG                             ==> FLOAT,,DOUBLE;;
        //           \(double\)                       ==> ,, ]]
        if (Arrays.isLongType(requiredType)) {
            // truncateOverflows has no effect
            if (dim.length == 1) {
                return InternalUtils.<T>cast(
                    new CoordFuncLongArray(truncateOverflows, len, f, dim) {
                        public long getLong(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return (long)this.f.get(index);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else if (dim.length == 2) {
                return InternalUtils.<T>cast(
                    new CoordFuncLongArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0];

                        public long getLong(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return (long)this.f.get(index % dimX, index / dimX);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else if (dim.length == 3) {
                return InternalUtils.<T>cast(
                    new CoordFuncLongArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0],
                            dimXY = dimX * dim[1];

                        public long getLong(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return (long)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new CoordFuncLongArray(truncateOverflows, len, f, dim) {
                        public long getLong(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            double[] coordinates = new double[dim.length];
                            coordinatesInDoubles(index, dim, coordinates);
                            return (long)this.f.get(coordinates);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isFloatType(requiredType)) {
            // truncateOverflows has no effect
            if (dim.length == 1) {
                return InternalUtils.<T>cast(
                    new CoordFuncFloatArray(truncateOverflows, len, f, dim) {
                        public float getFloat(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return (float)this.f.get(index);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else if (dim.length == 2) {
                return InternalUtils.<T>cast(
                    new CoordFuncFloatArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0];

                        public float getFloat(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return (float)this.f.get(index % dimX, index / dimX);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else if (dim.length == 3) {
                return InternalUtils.<T>cast(
                    new CoordFuncFloatArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0],
                            dimXY = dimX * dim[1];

                        public float getFloat(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return (float)this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new CoordFuncFloatArray(truncateOverflows, len, f, dim) {
                        public float getFloat(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            double[] coordinates = new double[dim.length];
                            coordinatesInDoubles(index, dim, coordinates);
                            return (float)this.f.get(coordinates);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }
                    });
            }
        }
        if (Arrays.isDoubleType(requiredType)) {
            // truncateOverflows has no effect
            if (dim.length == 1) {
                return InternalUtils.<T>cast(
                    new CoordFuncDoubleArray(truncateOverflows, len, f, dim) {
                        public double getDouble(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return this.f.get(index);
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else if (dim.length == 2) {
                return InternalUtils.<T>cast(
                    new CoordFuncDoubleArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0];

                        public double getDouble(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return this.f.get(index % dimX, index / dimX);
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else if (dim.length == 3) {
                return InternalUtils.<T>cast(
                    new CoordFuncDoubleArray(truncateOverflows, len, f, dim) {
                        final long dimX = dim[0],
                            dimXY = dimX * dim[1];

                        public double getDouble(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            return this.f.get(index % dimX, index % dimXY / dimX, index / dimXY);
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new CoordFuncDoubleArray(truncateOverflows, len, f, dim) {
                        public double getDouble(long index) {
                            if (index < 0 || index >= length) {
                                throw rangeException(index);
                            }
                            double[] coordinates = new double[dim.length];
                            coordinatesInDoubles(index, dim, coordinates);
                            return this.f.get(coordinates);
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal required type (" + requiredType
            + "): it must be one of primitive XxxArray interfaces");
    }

    static <T extends PArray> T asFuncArray(
        final boolean truncateOverflows,
        Func f, Class<? extends T> requiredType, PArray[] x, long len)
    {
        Objects.requireNonNull(f, "Null f argument");
        Objects.requireNonNull(requiredType, "Null requiredType argument");
        if (UpdatableArray.class.isAssignableFrom(requiredType)) {
            throw new IllegalArgumentException("requiredType, " + requiredType + ", must not be updatable");
        }

        for (int k = 0; k < x.length; k++) {
            Objects.requireNonNull(x[k], "Null x[" + k + "] argument");
            if (x[k].length() != len) {
                throw new SizeMismatchException("x[" + k + "].length() and x[0].length() mismatch");
            }
        }
        long[] tileDimensions = null; // will be non-null if all non-constant arrays are identically tiled
        long[] baseMatrixDimensions = null;
        for (PArray a : x) {
            if (Arrays.isNCopies(a)) {
                continue; // skip constant arrays: their tiling is not important
            }
            if (!Arrays.isTiled(a)) {
                tileDimensions = null;
                break;
            }
            ArraysTileMatrixImpl.TileMatrixArray tma = (ArraysTileMatrixImpl.TileMatrixArray) a;
            if (tileDimensions == null) {
                tileDimensions = tma.tileDimensions();
                baseMatrixDimensions = tma.baseMatrix().dimensions();
            } else {
                if (!java.util.Arrays.equals(tileDimensions, tma.tileDimensions()) ||
                        !java.util.Arrays.equals(baseMatrixDimensions, tma.baseMatrix().dimensions())) {
                    tileDimensions = null;
                    break;
                }
            }
        }
        if (tileDimensions != null) {
            assert baseMatrixDimensions != null;
            PArray[] baseArrays = new PArray[x.length];
            for (int k = 0; k < x.length; k++) {
                if (Arrays.isNCopies(x[k])) {
                    baseArrays[k] = x[k]; continue;
                }
                assert Arrays.isTiled(x[k]);
                ArraysTileMatrixImpl.TileMatrixArray tma = (ArraysTileMatrixImpl.TileMatrixArray)x[k];
                assert java.util.Arrays.equals(tileDimensions, tma.tileDimensions());
                assert java.util.Arrays.equals(baseMatrixDimensions, tma.baseMatrix().dimensions());
                baseArrays[k] = (PArray)tma.baseMatrix().array();
            }
            T result = asFuncArray(truncateOverflows, f, requiredType, baseArrays, len);
            return Matrices.matrix(result, baseMatrixDimensions).tile(tileDimensions).array();
        }

        boolean quickTableVersion = x.length == 1
            && (x[0] instanceof BitArray
            || x[0] instanceof ByteArray);
//            || (x[0] instanceof ShortArray && x[0].length() > MIN_LENGTH_OF_SHORT_ARRAYS_FOR_TABLE_OPTIMIZATION);
// - It was a bug! Not a problem to allocate 10000 or 100000 instances of table functions and to "eat" all Java memory
// Byte tables are more safe: it requires little more that a pure Java object
        if (f == Func.IDENTITY || f == Func.UPDATABLE_IDENTITY) {
            if (x.length < 1) {
                throw new IllegalArgumentException("At least one array is necessary for the identity function");
            }
            if (!quickTableVersion || !(x[0] instanceof BitArray)) {
                return asIdentityFuncArray(truncateOverflows, requiredType, f, x[0]);
            }
        }
        if (f instanceof ConstantFunc) {
            return asConstantFuncArray(truncateOverflows, requiredType, f, len);
        }
        if (f == Func.POSITIVE_DIFF) {
            if (x.length < 2) {
                throw new IllegalArgumentException("Insufficient number of arrays for "
                    + "the positive difference function");
            }
            Class<?> eType = x[0].elementType();
            if (eType == x[1].elementType() && eType == Arrays.elementType(requiredType)
                && (eType == boolean.class || (truncateOverflows
                && (eType == byte.class || eType == short.class || eType == char.class)))) {
                return InternalUtils.<T>cast(asSubtractionFunc(truncateOverflows, f, x));
            }
        }
        if (f == Func.ABS_DIFF) {
            if (x.length < 2) {
                throw new IllegalArgumentException("Insufficient number of arrays for "
                    + "the absolute difference function");
            }
            Class<?> eType = x[0].elementType();
            if (eType != long.class && eType == x[1].elementType() && eType == Arrays.elementType(requiredType)) {
                // long values cannot be processed correctly because overflow and loss of precision are possible
                return InternalUtils.<T>cast(asAbsDiffFuncArray(truncateOverflows, f, x));
            }
        }
        if (f instanceof LinearFunc lf) {
            int n = lf.n();
            if (x.length < n) {
                throw new IllegalArgumentException("Insufficient number of arrays for the linear function");
            }
            double[] a = lf.a();
            if (n != a.length) {
                throw new IllegalArgumentException("Illegal implementation of the linear function: n()!=a().length");
            }
            double b = lf.b();
            Class<?> eType = x[0].elementType();
            if (n == 1 && b == 1.0 && a[0] == -1.0 // 1-x: Func.REVERSE
                && eType == boolean.class
                && eType == Arrays.elementType(requiredType)) {
                // for boolean, 1-x should be translated to x^1: this case is processed
                // by special quicker branch by ArraysDiffGetDataOp
                return InternalUtils.<T>cast(asAbsDiffFuncArray(truncateOverflows, lf, x[0],
                    Arrays.nBitCopies(x[0].length(), true)));
            }
            if (n == 2 && b == 0.0
                && ((a[0] == 1.0 && a[1] == -1.0) || (a[0] == -1.0 && a[1] == 1.0)) // x-y or y-x
                && eType != long.class
                && eType == x[1].elementType() && eType == Arrays.elementType(requiredType))
            // long values cannot be processed correctly because overflow and loss of precision are possible
            {
                if (eType == boolean.class) {
                    // for boolean, x-y should be translated to x-y!=0.0, that is to x^y
                    return InternalUtils.<T>cast(asAbsDiffFuncArray(truncateOverflows, lf, x));
                } else if (a[0] == 1.0) {
                    return InternalUtils.<T>cast(asSubtractionFunc(truncateOverflows, lf, x));
                } else {
                    return InternalUtils.<T>cast(asSubtractionFunc(truncateOverflows, lf,
                        x[1], x[0]));
                }
            }
            if (!quickTableVersion || (!(x[0] instanceof BitArray) && b == 0.0 && a[0] == 1.0)) {
                // - It is not an obvious solution: sometimes we need a lot of lazy linear-function arrays,
                // and even little "quick tables" can lead to spending a lot of memory, up to 1 KB/array.
                // But the standard asLinearFuncArray implementation leads to comparable memory usage
                // and works almost in 2 times slower.
                return asLinearFuncArray(truncateOverflows, requiredType, lf, x, len);
            }
        }
        if (f == Func.MIN || f == Func.MAX) {
            if (x.length == 0) {
                return asConstantFuncArray(truncateOverflows, requiredType, f, len);
            } else if (x.length == 1) {
                if (!quickTableVersion || !(x[0] instanceof BitArray)) {
                    return asIdentityFuncArray(truncateOverflows, requiredType, f, x[0]);
                }
            } else {
                boolean sameType = true;
                for (int k = 1; k < x.length; k++) {
                    if (x[k].elementType() != x[0].elementType()) {
                        sameType = false;
                    }
                }
                if (sameType) {
                    x = addUnderlyingArraysWithSameMinMaxFunc(f, x);
                    PArray result = f == Func.MIN ?
                        asMinFuncArray(f, x) :
                        asMaxFuncArray(f, x);
                    if (result.elementType() == Arrays.elementType(requiredType)) {
                        // in this case, illegal requiredType should lead to exception in Arrays.elementType(...)
                        return InternalUtils.<T>cast(result);
                    } else {
                        return asIdentityFuncArray(truncateOverflows, requiredType,
                            Func.IDENTITY, result);
                    }
                }
            }
        }

        if (Arrays.isBitType(requiredType)) {
            if (x.length == 1) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, BIT_TYPE_CODE);
                            final boolean v0 = tgdo.booleanTable[0];
                            final boolean v1 = tgdo.booleanTable[1];
//                            {
//                                System.out.println(v0 + "; " + v1 + "; " + f);
//                            }

                            public boolean getBit(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, BIT_TYPE_CODE);
                            final boolean[] v = tgdo.booleanTable;

                            public boolean getBit(long index) {
                                return v[x0.getByte(index) & 0xFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray && quickTableVersion) {
                    return InternalUtils.<T>cast(
                        new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, BIT_TYPE_CODE);
                            final boolean[] v = tgdo.booleanTable;

                            public boolean getBit(long index) {
                                return v[x0.getShort(index) & 0xFFFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                            public boolean getBit(long index) {
                                return this.f.get(this.x[0].getDouble(index)) != 0.0;
                            }
                        });
                }
            } else if (x.length == 2) {
                return InternalUtils.<T>cast(
                    new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                        public boolean getBit(long index) {
                            return this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index)) != 0.0;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                        public boolean getBit(long index) {
                            double[] args = new double[this.x.length];
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return this.f.get(args) != 0.0;
                        }
                    });
            }
        }

        if (Arrays.isCharType(requiredType)) {
            if (x.length == 1 && quickTableVersion) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, CHAR_TYPE_CODE);
                            final char v0 = tgdo.charTable[0];
                            final char v1 = tgdo.charTable[1];

                            public char getChar(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, CHAR_TYPE_CODE);
                            final char[] v = tgdo.charTable;

                            public char getChar(long index) {
                                return v[x0.getByte(index) & 0xFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, CHAR_TYPE_CODE);
                            final char[] v = tgdo.charTable;

                            public char getChar(long index) {
                                return v[x0.getShort(index) & 0xFFFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            } else if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index));
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                int v = (int)this.f.get(args);
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                return (char)(long)this.f.get(this.x[0].getDouble(index));
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                return (char)(long)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (char)(long)this.f.get(args);
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }
                        });
                }
            }
        }
        //[[Repeat() byte               ==> short;;
        //           isByteType         ==> isShortType;;
        //           FuncByteArray      ==> FuncShortArray;;
        //           getByte(?!\(index) ==> getShort;;
        //           BYTE               ==> SHORT;;
        //           0xFF(?![\]Ff])     ==> 0xFFFF]]
        if (Arrays.isByteType(requiredType)) {
            if (x.length == 1 && quickTableVersion) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, BYTE_TYPE_CODE);
                            final int v0 = tgdo.byteTable[0] & 0xFF;
                            final int v1 = tgdo.byteTable[1] & 0xFF;

                            public int getByte(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, BYTE_TYPE_CODE);
                            final byte[] v = tgdo.byteTable;

                            public int getByte(long index) {
                                return v[x0.getByte(index) & 0xFF] & 0xFF;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, BYTE_TYPE_CODE);
                            final byte[] v = tgdo.byteTable;

                            public int getByte(long index) {
                                return v[x0.getShort(index) & 0xFFFF] & 0xFF;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            } else if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index));
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                int v = (int)this.f.get(args);
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index)) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index),
                                    this.x[1].getDouble(index)) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)(long)this.f.get(args) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                }
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isShortType(requiredType)) {
            if (x.length == 1 && quickTableVersion) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, SHORT_TYPE_CODE);
                            final int v0 = tgdo.shortTable[0] & 0xFFFF;
                            final int v1 = tgdo.shortTable[1] & 0xFFFF;

                            public int getShort(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, SHORT_TYPE_CODE);
                            final short[] v = tgdo.shortTable;

                            public int getShort(long index) {
                                return v[x0.getByte(index) & 0xFF] & 0xFFFF;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, SHORT_TYPE_CODE);
                            final short[] v = tgdo.shortTable;

                            public int getShort(long index) {
                                return v[x0.getShort(index) & 0xFFFF] & 0xFFFF;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            } else if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index));
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                int v = (int)this.f.get(args);
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index)) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index),
                                    this.x[1].getDouble(index)) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)(long)this.f.get(args) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                }
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        if (Arrays.isIntType(requiredType)) {
            if (x.length == 1 && quickTableVersion) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, INT_TYPE_CODE);
                            final int v0 = tgdo.intTable[0];
                            final int v1 = tgdo.intTable[1];

                            public int getInt(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, INT_TYPE_CODE);
                            final int[] v = tgdo.intTable;

                            public int getInt(long index) {
                                return v[x0.getByte(index) & 0xFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, INT_TYPE_CODE);
                            final int[] v = tgdo.intTable;

                            public int getInt(long index) {
                                return v[x0.getShort(index) & 0xFFFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            } else if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                return (int)this.f.get(this.x[0].getDouble(index));
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                return (int)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)this.f.get(args);
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index));
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else if (x.length == 2) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                double[] args = new double[this.x.length];
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)(long)this.f.get(args);
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }
                        });
                }
            }
        }

        //[[Repeat() long(?!\s+(index|arrayPos)) ==> float,,double;;
        //           Long                        ==> Float,,Double;;
        //           LONG                        ==> FLOAT,,DOUBLE;;
        //           \(double\)                  ==> ,, ]]
        if (Arrays.isLongType(requiredType)) {
            // truncateOverflows has no effect
            if (x.length == 1) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, LONG_TYPE_CODE);
                            final long v0 = tgdo.longTable[0];
                            final long v1 = tgdo.longTable[1];

                            public long getLong(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, LONG_TYPE_CODE);
                            final long[] v = tgdo.longTable;

                            public long getLong(long index) {
                                return v[x0.getByte(index) & 0xFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray && quickTableVersion) {
                    return InternalUtils.<T>cast(
                        new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, LONG_TYPE_CODE);
                            final long[] v = tgdo.longTable;

                            public long getLong(long index) {
                                return v[x0.getShort(index) & 0xFFFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                            public long getLong(long index) {
                                return (long)this.f.get(this.x[0].getDouble(index));
                                // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                            }
                        });
                }
            } else if (x.length == 2) {
                return InternalUtils.<T>cast(
                    new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                        public long getLong(long index) {
                            return (long)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                        public long getLong(long index) {
                            double[] args = new double[this.x.length];
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return (long)this.f.get(args);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isFloatType(requiredType)) {
            // truncateOverflows has no effect
            if (x.length == 1) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, FLOAT_TYPE_CODE);
                            final float v0 = tgdo.floatTable[0];
                            final float v1 = tgdo.floatTable[1];

                            public float getFloat(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, FLOAT_TYPE_CODE);
                            final float[] v = tgdo.floatTable;

                            public float getFloat(long index) {
                                return v[x0.getByte(index) & 0xFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray && quickTableVersion) {
                    return InternalUtils.<T>cast(
                        new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, FLOAT_TYPE_CODE);
                            final float[] v = tgdo.floatTable;

                            public float getFloat(long index) {
                                return v[x0.getShort(index) & 0xFFFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                            public float getFloat(long index) {
                                return (float)this.f.get(this.x[0].getDouble(index));
                                // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                            }
                        });
                }
            } else if (x.length == 2) {
                return InternalUtils.<T>cast(
                    new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                        public float getFloat(long index) {
                            return (float)this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                        public float getFloat(long index) {
                            double[] args = new double[this.x.length];
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return (float)this.f.get(args);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }
                    });
            }
        }
        if (Arrays.isDoubleType(requiredType)) {
            // truncateOverflows has no effect
            if (x.length == 1) {
                if (x[0] instanceof BitArray) {
                    return InternalUtils.<T>cast(
                        new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                            final BitArray x0 = (BitArray)x[0];
                            final ArraysBitTableGetDataOp tgdo = new ArraysBitTableGetDataOp(
                                truncateOverflows, x0, f, DOUBLE_TYPE_CODE);
                            final double v0 = tgdo.doubleTable[0];
                            final double v1 = tgdo.doubleTable[1];

                            public double getDouble(long index) {
                                return x0.getBit(index) ? v1 : v0;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ByteArray) {
                    return InternalUtils.<T>cast(
                        new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                            final ByteArray x0 = (ByteArray)x[0];
                            final ArraysByteTableGetDataOp tgdo = new ArraysByteTableGetDataOp(
                                truncateOverflows, x0, f, DOUBLE_TYPE_CODE);
                            final double[] v = tgdo.doubleTable;

                            public double getDouble(long index) {
                                return v[x0.getByte(index) & 0xFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else if (x[0] instanceof ShortArray && quickTableVersion) {
                    return InternalUtils.<T>cast(
                        new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                            final ShortArray x0 = (ShortArray)x[0];
                            final ArraysShortTableGetDataOp tgdo = new ArraysShortTableGetDataOp(
                                truncateOverflows, x0, f, DOUBLE_TYPE_CODE);
                            final double[] v = tgdo.doubleTable;

                            public double getDouble(long index) {
                                return v[x0.getShort(index) & 0xFFFF];
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                tgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                            public double getDouble(long index) {
                                return this.f.get(this.x[0].getDouble(index));
                                // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                            }
                        });
                }
            } else if (x.length == 2) {
                return InternalUtils.<T>cast(
                    new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                        public double getDouble(long index) {
                            return this.f.get(this.x[0].getDouble(index), this.x[1].getDouble(index));
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                        public double getDouble(long index) {
                            double[] args = new double[this.x.length];
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return this.f.get(args);
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal required type (" + requiredType
            + "): it must be one of primitive XxxArray interfaces");
    }

    static <T extends UpdatablePArray> T asUpdatableFuncArray(
        final boolean truncateOverflows,
        Func.Updatable f, Class<? extends T> requiredType, UpdatablePArray[] x)
    {
        Objects.requireNonNull(f, "Null f argument");
        Objects.requireNonNull(requiredType, "Null requiredType argument");
        if (MutableArray.class.isAssignableFrom(requiredType)) {
            throw new IllegalArgumentException("requiredType, " + requiredType + ", must not be resizable");
        }
        if (x.length == 0) {
            throw new IllegalArgumentException("Empty x[] (array of AlgART arrays)");
        }
        final long len = x[0].length();
        for (int k = 0; k < x.length; k++) {
            Objects.requireNonNull(x[k], "Null x[" + k + "] argument");
            if (x[k].length() != len) {
                throw new SizeMismatchException("x[" + k + "].length() and x[0].length() mismatch");
            }
        }
        if (f == Func.UPDATABLE_IDENTITY) {
            return asUpdatableIdentityFunc(truncateOverflows, requiredType, f, x[0]);
        }

        if (f instanceof LinearFunc.Updatable ulf) {
            int n = ulf.n();
            if (x.length < n) {
                throw new IllegalArgumentException("Insufficient number of arrays for the updatable linear function");
            }
            if (n == 1) {
                return asUpdatableLinearFunc(truncateOverflows, requiredType, ulf, x[0]);
            }
        }

        final boolean[] truncateInSet = new boolean[x.length];
        final boolean[] longPrecisionInSet = new boolean[x.length];
        final double[] minXElement = new double[x.length];
        final double[] maxXElement = new double[x.length];
        for (int k = 0; k < x.length; k++) {
            truncateInSet[k] = truncateOverflows && x[k] instanceof PFixedArray
                && !(x[k] instanceof LongArray || x[k] instanceof BitArray);
            longPrecisionInSet[k] = !truncateOverflows && x[k] instanceof PFixedArray
                && !(x[k] instanceof LongArray || x[k] instanceof BitArray);
            minXElement[k] = truncateInSet[k] ? ((PFixedArray)x[k]).minPossibleValue() : Double.MIN_VALUE;
            maxXElement[k] = truncateInSet[k] ? ((PFixedArray)x[k]).maxPossibleValue() : Double.MAX_VALUE;
            // if truncation in set is necessary, minXElement/maxXElement values are stored precisely by "double" type
        }
        final double[] args = new double[x.length];
        // Unlike asFuncArray, synchronization is not necessary here, because it is not an immutable array.
        if (Arrays.isBitType(requiredType)) {
            if (x.length == 1) {
                final double[] argsFalse = args;
                final double[] argsTrue = new double[1];
                f.set(argsFalse, 0.0);
                f.set(argsTrue, 1.0);
                return InternalUtils.<T>cast(
                    new UpdatableFuncBitArray(truncateOverflows, len, f, x) {
                        public boolean getBit(long index) {
                            return this.f.get(this.x[0].getDouble(index)) != 0.0;
                        }

                        public void setBit(long index, boolean value) {
                            if (truncateInSet[0]) {
                                double v = value ? argsTrue[0] : argsFalse[0];
                                this.x[0].setDouble(index,
                                    v < minXElement[0] ? minXElement[0] : Math.min(v, maxXElement[0]));
                            } else {
                                this.x[0].setDouble(index, value ? argsTrue[0] : argsFalse[0]);
                            }
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncBitArray(truncateOverflows, len, f, x) {
                        public boolean getBit(long index) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return this.f.get(args) != 0.0;
                        }

                        public void setBit(long index, boolean value) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            this.f.set(args, value ? 1.0 : 0.0);
                            for (int k = 0; k < this.x.length; k++) {
                                if (truncateInSet[k]) {
                                    double v = args[k];
                                    this.x[k].setDouble(index,
                                        v < minXElement[k] ? minXElement[k] : Math.min(v, maxXElement[k]));
                                } else {
                                    this.x[k].setDouble(index, args[k]);
                                }
                            }
                        }
                    });
            }
        }

        if (Arrays.isCharType(requiredType)) {
            if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncCharArray(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index));
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }

                            public void setChar(long index, char value) {
                                this.f.set(args, value);
                                if (truncateInSet[0]) {
                                    this.x[0].setDouble(index,
                                        args[0] < minXElement[0] ? minXElement[0] :
                                                Math.min(args[0], maxXElement[0]));
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncCharArray(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                int v = (int)this.f.get(args);
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }

                            public void setChar(long index, char value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (truncateInSet[k]) {
                                        double v = args[k];
                                        this.x[k].setDouble(index,
                                            v < minXElement[k] ? minXElement[k] :
                                                    Math.min(v, maxXElement[k]));
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncCharArray(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                return (char)(long)this.f.get(this.x[0].getDouble(index));
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }

                            public void setChar(long index, char value) {
                                this.f.set(args, value);
                                if (longPrecisionInSet[0]) {
                                    this.x[0].setLong(index, (long)args[0]);
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncCharArray(truncateOverflows, len, f, x) {
                            public char getChar(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (char)(long)this.f.get(args);
                                // note: for float array, (char)(long)v differs from (int)v for very large floats
                            }

                            public void setChar(long index, char value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (longPrecisionInSet[k]) {
                                        this.x[k].setLong(index, (long)args[k]);
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            }
        }
        //[[Repeat() byte ==> short;;
        //           Byte ==> Short;;
        //           0xFF ==> 0xFFFF]]
        if (Arrays.isByteType(requiredType)) {
            if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncByteArray(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index));
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }

                            public void setByte(long index, byte value) {
                                this.f.set(args, value & 0xFF);
                                if (truncateInSet[0]) {
                                    this.x[0].setDouble(index,
                                        args[0] < minXElement[0] ? minXElement[0] :
                                                Math.min(args[0], maxXElement[0]));
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncByteArray(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                int v = (int)this.f.get(args);
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }

                            public void setByte(long index, byte value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value & 0xFF);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (truncateInSet[k]) {
                                        double v = args[k];
                                        this.x[k].setDouble(index,
                                            v < minXElement[k] ? minXElement[k] :
                                                    Math.min(v, maxXElement[k]));
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncByteArray(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index)) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }

                            public void setByte(long index, byte value) {
                                this.f.set(args, value & 0xFF);
                                if (longPrecisionInSet[0]) {
                                    this.x[0].setLong(index, (long)args[0]);
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncByteArray(truncateOverflows, len, f, x) {
                            public int getByte(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)(long)this.f.get(args) & 0xFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }

                            public void setByte(long index, byte value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value & 0xFF);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (longPrecisionInSet[k]) {
                                        this.x[k].setLong(index, (long)args[k]);
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isShortType(requiredType)) {
            if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncShortArray(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                int v = (int)this.f.get(this.x[0].getDouble(index));
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }

                            public void setShort(long index, short value) {
                                this.f.set(args, value & 0xFFFF);
                                if (truncateInSet[0]) {
                                    this.x[0].setDouble(index,
                                        args[0] < minXElement[0] ? minXElement[0] :
                                                Math.min(args[0], maxXElement[0]));
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncShortArray(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                int v = (int)this.f.get(args);
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }

                            public void setShort(long index, short value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value & 0xFFFF);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (truncateInSet[k]) {
                                        double v = args[k];
                                        this.x[k].setDouble(index,
                                            v < minXElement[k] ? minXElement[k] :
                                                    Math.min(v, maxXElement[k]));
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncShortArray(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index)) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }

                            public void setShort(long index, short value) {
                                this.f.set(args, value & 0xFFFF);
                                if (longPrecisionInSet[0]) {
                                    this.x[0].setLong(index, (long)args[0]);
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncShortArray(truncateOverflows, len, f, x) {
                            public int getShort(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)(long)this.f.get(args) & 0xFFFF;
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }

                            public void setShort(long index, short value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value & 0xFFFF);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (longPrecisionInSet[k]) {
                                        this.x[k].setLong(index, (long)args[k]);
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            }
        }
        //[[Repeat.AutoGeneratedEnd]]

        if (Arrays.isIntType(requiredType)) {
            if (truncateOverflows) {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncIntArray(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                return (int)this.f.get(this.x[0].getDouble(index));
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }

                            public void setInt(long index, int value) {
                                this.f.set(args, value);
                                if (truncateInSet[0]) {
                                    this.x[0].setDouble(index,
                                        args[0] < minXElement[0] ? minXElement[0] :
                                                Math.min(args[0], maxXElement[0]));
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncIntArray(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)this.f.get(args);
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }

                            public void setInt(long index, int value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (truncateInSet[k]) {
                                        double v = args[k];
                                        this.x[k].setDouble(index,
                                            v < minXElement[k] ? minXElement[k] :
                                                    Math.min(v, maxXElement[k]));
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            } else {
                if (x.length == 1) {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncIntArray(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                return (int)(long)this.f.get(this.x[0].getDouble(index));
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }

                            public void setInt(long index, int value) {
                                this.f.set(args, value);
                                if (longPrecisionInSet[0]) {
                                    this.x[0].setLong(index, (long)args[0]);
                                } else {
                                    this.x[0].setDouble(index, args[0]);
                                }
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new UpdatableFuncIntArray(truncateOverflows, len, f, x) {
                            public int getInt(long index) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                return (int)(long)this.f.get(args);
                                // note: for float array, (int)(long)v differs from (int)v for very large floats
                            }

                            public void setInt(long index, int value) {
                                for (int k = 0; k < this.x.length; k++) {
                                    args[k] = this.x[k].getDouble(index);
                                }
                                this.f.set(args, value);
                                for (int k = 0; k < this.x.length; k++) {
                                    if (longPrecisionInSet[k]) {
                                        this.x[k].setLong(index, (long)args[k]);
                                    } else {
                                        this.x[k].setDouble(index, args[k]);
                                    }
                                }
                            }
                        });
                }
            }
        }

        if (Arrays.isLongType(requiredType)) {
            // truncateOverflows has no effect
            if (x.length == 1) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncLongArray(truncateOverflows, len, f, x) {
                        public long getLong(long index) {
                            return (long)this.f.get(this.x[0].getDouble(index));
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }

                        public void setLong(long index, long value) {
                            this.f.set(args, value);
                            if (truncateInSet[0]) {
                                this.x[0].setDouble(index,
                                    args[0] < minXElement[0] ? minXElement[0] :
                                            Math.min(args[0], maxXElement[0]));
                            } else if (longPrecisionInSet[0]) {
                                this.x[0].setLong(index, (long)args[0]);
                            } else {
                                this.x[0].setDouble(index, args[0]);
                            }
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncLongArray(truncateOverflows, len, f, x) {
                        public long getLong(long index) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return (long)this.f.get(args);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }

                        public void setLong(long index, long value) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            this.f.set(args, value);
                            for (int k = 0; k < this.x.length; k++) {
                                if (truncateInSet[k]) {
                                    double v = args[k];
                                    this.x[k].setDouble(index,
                                        v < minXElement[k] ? minXElement[k] : Math.min(v, maxXElement[k]));
                                } else if (longPrecisionInSet[k]) {
                                    this.x[k].setLong(index, (long)args[k]);
                                } else {
                                    this.x[k].setDouble(index, args[k]);
                                }
                            }
                        }
                    });
            }
        }

        //[[Repeat() float ==> double;;
        //           Float ==> Double;;
        //           \(double\) ==> ]]
        if (Arrays.isFloatType(requiredType)) {
            // truncateOverflows has no effect
            if (x.length == 1) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncFloatArray(truncateOverflows, len, f, x) {
                        public float getFloat(long index) {
                            return (float)this.f.get(this.x[0].getDouble(index));
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }

                        public void setFloat(long index, float value) {
                            this.f.set(args, value);
                            if (truncateInSet[0]) {
                                this.x[0].setDouble(index,
                                    args[0] < minXElement[0] ? minXElement[0] :
                                            Math.min(args[0], maxXElement[0]));
                            } else if (longPrecisionInSet[0]) {
                                this.x[0].setLong(index, (long)args[0]);
                            } else {
                                this.x[0].setDouble(index, args[0]);
                            }
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncFloatArray(truncateOverflows, len, f, x) {
                        public float getFloat(long index) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return (float)this.f.get(args);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }

                        public void setFloat(long index, float value) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            this.f.set(args, value);
                            for (int k = 0; k < this.x.length; k++) {
                                if (truncateInSet[k]) {
                                    double v = args[k];
                                    this.x[k].setDouble(index,
                                        v < minXElement[k] ? minXElement[k] : Math.min(v, maxXElement[k]));
                                } else if (longPrecisionInSet[k]) {
                                    this.x[k].setLong(index, (long)args[k]);
                                } else {
                                    this.x[k].setDouble(index, args[k]);
                                }
                            }
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isDoubleType(requiredType)) {
            // truncateOverflows has no effect
            if (x.length == 1) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncDoubleArray(truncateOverflows, len, f, x) {
                        public double getDouble(long index) {
                            return this.f.get(this.x[0].getDouble(index));
                            // Java automatically truncates double values to Double.MIN_VALUE..MAX_VALUE here
                        }

                        public void setDouble(long index, double value) {
                            this.f.set(args, value);
                            if (truncateInSet[0]) {
                                this.x[0].setDouble(index,
                                    args[0] < minXElement[0] ? minXElement[0] :
                                            Math.min(args[0], maxXElement[0]));
                            } else if (longPrecisionInSet[0]) {
                                this.x[0].setLong(index, (long)args[0]);
                            } else {
                                this.x[0].setDouble(index, args[0]);
                            }
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncDoubleArray(truncateOverflows, len, f, x) {
                        public double getDouble(long index) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            return this.f.get(args);
                            // Java automatically truncates double values to Double.MIN_VALUE..MAX_VALUE here
                        }

                        public void setDouble(long index, double value) {
                            for (int k = 0; k < this.x.length; k++) {
                                args[k] = this.x[k].getDouble(index);
                            }
                            this.f.set(args, value);
                            for (int k = 0; k < this.x.length; k++) {
                                if (truncateInSet[k]) {
                                    double v = args[k];
                                    this.x[k].setDouble(index,
                                        v < minXElement[k] ? minXElement[k] : Math.min(v, maxXElement[k]));
                                } else if (longPrecisionInSet[k]) {
                                    this.x[k].setLong(index, (long)args[k]);
                                } else {
                                    this.x[k].setDouble(index, args[k]);
                                }
                            }
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal required type (" + requiredType
            + "): it must be one of primitive XxxArray or UpdatableXxxArray interfaces");
    }

    private static <T extends PArray> PArray asMinFuncArray(Func f, final PArray... x) {
        if (x.length == 0) {
            throw new IllegalArgumentException("Empty x array");
        }
        if (x[0] instanceof BitArray) {
            final BitArray[] xClone = assertTypeAndCast(BitArray.class, x);
            return new FuncBitArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone, null, true);

                public boolean getBit(long index) {
                    boolean result = xClone[0].getBit(index);
                    for (int k = 1; k < xClone.length; k++) {
                        result &= xClone[k].getBit(index);
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }

                public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
                    mmgdo.getBits(arrayPos, destArray, destArrayOffset, count);
                }

                @Override
                public long nextQuickPosition(long position) {
                    return xClone[0].nextQuickPosition(position);
                }
            };
        }

        //[[Repeat() int\s+(getByte|result|v) ==> char $1,,int $1,,int $1,,long $1,,float $1,,double $1;;
        //           byte ==> char,,short,,int,,long,,float,,double;;
        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
        //           (\s+&\s+0xFF) ==> ,,$1FF,, ,, ...]]
        if (x[0] instanceof ByteArray) {
            final ByteArray[] xClone = assertTypeAndCast(ByteArray.class, x);
            return new FuncByteArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getByteMinOp(), true);

                public int getByte(long index) {
                    int result = xClone[0].getByte(index);
                    for (int k = 1; k < xClone.length; k++) {
                        int v = xClone[k].getByte(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (x[0] instanceof CharArray) {
            final CharArray[] xClone = assertTypeAndCast(CharArray.class, x);
            return new FuncCharArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getCharMinOp(), true);

                public char getChar(long index) {
                    char result = xClone[0].getChar(index);
                    for (int k = 1; k < xClone.length; k++) {
                        char v = xClone[k].getChar(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof ShortArray) {
            final ShortArray[] xClone = assertTypeAndCast(ShortArray.class, x);
            return new FuncShortArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getShortMinOp(), true);

                public int getShort(long index) {
                    int result = xClone[0].getShort(index);
                    for (int k = 1; k < xClone.length; k++) {
                        int v = xClone[k].getShort(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof IntArray) {
            final IntArray[] xClone = assertTypeAndCast(IntArray.class, x);
            return new FuncIntArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getIntMinOp(), true);

                public int getInt(long index) {
                    int result = xClone[0].getInt(index);
                    for (int k = 1; k < xClone.length; k++) {
                        int v = xClone[k].getInt(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof LongArray) {
            final LongArray[] xClone = assertTypeAndCast(LongArray.class, x);
            return new FuncLongArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getLongMinOp(), true);

                public long getLong(long index) {
                    long result = xClone[0].getLong(index);
                    for (int k = 1; k < xClone.length; k++) {
                        long v = xClone[k].getLong(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof FloatArray) {
            final FloatArray[] xClone = assertTypeAndCast(FloatArray.class, x);
            return new FuncFloatArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getFloatMinOp(), true);

                public float getFloat(long index) {
                    float result = xClone[0].getFloat(index);
                    for (int k = 1; k < xClone.length; k++) {
                        float v = xClone[k].getFloat(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof DoubleArray) {
            final DoubleArray[] xClone = assertTypeAndCast(DoubleArray.class, x);
            return new FuncDoubleArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getDoubleMinOp(), true);

                public double getDouble(long index) {
                    double result = xClone[0].getDouble(index);
                    for (int k = 1; k < xClone.length; k++) {
                        double v = xClone[k].getDouble(index);
                        if (v < result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal array type (" + x[0]
            + "): it must implement one of primitive XxxArray interfaces");
    }

    private static <T extends PArray> PArray asMaxFuncArray(Func f, final PArray... x) {
        if (x.length == 0) {
            throw new IllegalArgumentException("Empty x array");
        }
        if (x[0] instanceof BitArray) {
            final BitArray[] xClone = assertTypeAndCast(BitArray.class, x);
            return new FuncBitArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone, null, false);

                public boolean getBit(long index) {
                    boolean result = xClone[0].getBit(index);
                    for (int k = 1; k < xClone.length; k++) {
                        result |= xClone[k].getBit(index);
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }

                public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
                    mmgdo.getBits(arrayPos, destArray, destArrayOffset, count);
                }

                @Override
                public long nextQuickPosition(long position) {
                    return xClone[0].nextQuickPosition(position);
                }
            };
        }

        //[[Repeat() int\s+(getByte|result|v) ==> char $1,,int $1,,int $1,,long $1,,float $1,,double $1;;
        //           byte ==> char,,short,,int,,long,,float,,double;;
        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
        //           (\s+&\s+0xFF) ==> ,,$1FF,, ,, ...]]
        if (x[0] instanceof ByteArray) {
            final ByteArray[] xClone = assertTypeAndCast(ByteArray.class, x);
            return new FuncByteArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getByteMaxOp(), false);

                public int getByte(long index) {
                    int result = xClone[0].getByte(index);
                    for (int k = 1; k < xClone.length; k++) {
                        int v = xClone[k].getByte(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (x[0] instanceof CharArray) {
            final CharArray[] xClone = assertTypeAndCast(CharArray.class, x);
            return new FuncCharArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getCharMaxOp(), false);

                public char getChar(long index) {
                    char result = xClone[0].getChar(index);
                    for (int k = 1; k < xClone.length; k++) {
                        char v = xClone[k].getChar(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof ShortArray) {
            final ShortArray[] xClone = assertTypeAndCast(ShortArray.class, x);
            return new FuncShortArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getShortMaxOp(), false);

                public int getShort(long index) {
                    int result = xClone[0].getShort(index);
                    for (int k = 1; k < xClone.length; k++) {
                        int v = xClone[k].getShort(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof IntArray) {
            final IntArray[] xClone = assertTypeAndCast(IntArray.class, x);
            return new FuncIntArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getIntMaxOp(), false);

                public int getInt(long index) {
                    int result = xClone[0].getInt(index);
                    for (int k = 1; k < xClone.length; k++) {
                        int v = xClone[k].getInt(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof LongArray) {
            final LongArray[] xClone = assertTypeAndCast(LongArray.class, x);
            return new FuncLongArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getLongMaxOp(), false);

                public long getLong(long index) {
                    long result = xClone[0].getLong(index);
                    for (int k = 1; k < xClone.length; k++) {
                        long v = xClone[k].getLong(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof FloatArray) {
            final FloatArray[] xClone = assertTypeAndCast(FloatArray.class, x);
            return new FuncFloatArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getFloatMaxOp(), false);

                public float getFloat(long index) {
                    float result = xClone[0].getFloat(index);
                    for (int k = 1; k < xClone.length; k++) {
                        float v = xClone[k].getFloat(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof DoubleArray) {
            final DoubleArray[] xClone = assertTypeAndCast(DoubleArray.class, x);
            return new FuncDoubleArrayWithArguments(false, x[0].length(), f, x) {
                final ArraysMinMaxGetDataOp mmgdo = new ArraysMinMaxGetDataOp(this, xClone,
                    ArraysMinMaxGetDataOp.getDoubleMaxOp(), false);

                public double getDouble(long index) {
                    double result = xClone[0].getDouble(index);
                    for (int k = 1; k < xClone.length; k++) {
                        double v = xClone[k].getDouble(index);
                        if (v > result) {
                            result = v;
                        }
                    }
                    return result;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    mmgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal array type (" + x[0]
            + "): it must implement one of primitive XxxArray interfaces");
    }

    private static <T extends PArray> T asLinearFuncArray(
            boolean truncateOverflows,
            Class<? extends T> requiredType,
            Func f, final PArray[] x, long len)
    {
        Objects.requireNonNull(requiredType, "Null requiredType argument");
        if (UpdatableArray.class.isAssignableFrom(requiredType)) {
            throw new IllegalArgumentException("requiredType must not be updatable");
        }
        final LinearFunc lf;
        if (f == Func.IDENTITY || f == Func.UPDATABLE_IDENTITY
            || (x.length == 1 && (f == Func.MIN || f == Func.MAX))) {
            lf = LinearFunc.getInstance(0.0, 1.0);
        } else if (f instanceof ConstantFunc || (x.length == 0 && (f == Func.MIN || f == Func.MAX))) {
            lf = LinearFunc.getInstance(f.get());
        } else if (f instanceof LinearFunc) {
            lf = (LinearFunc)f;
        } else {
            throw new AssertionError("asLinearFunc is called for unsupported function " + f);
        }
        final int n = lf.n();
        final double b = lf.b();

        boolean allAZeroes = true; // in particular, when n==0
        for (int k = 0; k < n; k++) {
            Objects.requireNonNull(x[k], "Null x[" + k + "] argument");
            if (lf.a(k) != 0.0) {
                allAZeroes = false;
            }
        }
        if (n <= 1 || allAZeroes) {

            final double a0 = n == 0 ? 0.0 : lf.a(0);

            if (Arrays.isBitType(requiredType)) {
                if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesBitArray(len, b != 0.0, truncateOverflows, f));
                } else {
                    return InternalUtils.<T>cast(
                        new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, BIT_TYPE_CODE);

                            public boolean getBit(long index) {
                                return a0 * x0.getDouble(index) != -b;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }

            if (Arrays.isCharType(requiredType)) {
                if (truncateOverflows) {
                    if (allAZeroes) {
                        int v = (int)b;
                        return InternalUtils.<T>cast(
                            new CopiesArraysImpl.CopiesCharArray(len,
                                v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char) v, truncateOverflows, f));
                    } else {
                        return InternalUtils.<T>cast(
                            new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                                final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                    truncateOverflows, x, lf, CHAR_TYPE_CODE);

                                public char getChar(long index) {
                                    int v = (int)(a0 * x0.getDouble(index) + b);
                                    return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                        v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                            (char)v;
                                }

                                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                    lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                                }
                            });
                    }
                } else if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesCharArray(len, (char) (long) b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0 && x[0] instanceof PFixedArray) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final PFixedArray x0Fix = (PFixedArray)x0;
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, CHAR_TYPE_CODE);

                            public char getChar(long index) {
                                return (char)x0Fix.getLong(index); // optimization for integer arrays
                                // note: for float array, (char)getLong will differ from getInt for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, CHAR_TYPE_CODE);

                            public char getChar(long index) {
                                return (char)(long)(a0 * x0.getDouble(index) + b);
                                // note: for float array, (char)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat() byte ==> short;;
            //           Byte ==> Short;;
            //           BYTE ==> SHORT;;
            //           0xFF ==> 0xFFFF]]
            if (Arrays.isByteType(requiredType)) {
                if (truncateOverflows) {
                    if (allAZeroes) {
                        int v = (int)b;
                        return InternalUtils.<T>cast(
                            new CopiesArraysImpl.CopiesByteArray(len,
                                (byte)(v < 0 ? 0 : Math.min(v, 0xFF)), truncateOverflows, f));
                    } else {
                        return InternalUtils.<T>cast(
                            new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                                final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                    truncateOverflows, x, lf, BYTE_TYPE_CODE);

                                public int getByte(long index) {
                                    int v = (int)(a0 * x0.getDouble(index) + b);
                                    return v < 0 ? 0 : Math.min(v, 0xFF);
                                }

                                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                    lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                                }
                            });
                    }
                } else if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesByteArray(len, (byte) (long) b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0 && x[0] instanceof PFixedArray) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final PFixedArray x0Fix = (PFixedArray)x0;
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, BYTE_TYPE_CODE);

                            public int getByte(long index) {
                                return (int)x0Fix.getLong(index) & 0xFF; // optimization for integer arrays
                                // note: for float array, (int)getLong will differ from getInt for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, BYTE_TYPE_CODE);

                            public int getByte(long index) {
                                return (int)(long)(a0 * x0.getDouble(index) + b) & 0xFF;
                                // note: for float array, (int)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            if (Arrays.isShortType(requiredType)) {
                if (truncateOverflows) {
                    if (allAZeroes) {
                        int v = (int)b;
                        return InternalUtils.<T>cast(
                            new CopiesArraysImpl.CopiesShortArray(len,
                                (short)(v < 0 ? 0 : Math.min(v, 0xFFFF)), truncateOverflows, f));
                    } else {
                        return InternalUtils.<T>cast(
                            new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                                final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                    truncateOverflows, x, lf, SHORT_TYPE_CODE);

                                public int getShort(long index) {
                                    int v = (int)(a0 * x0.getDouble(index) + b);
                                    return v < 0 ? 0 : Math.min(v, 0xFFFF);
                                }

                                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                    lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                                }
                            });
                    }
                } else if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesShortArray(len, (short) (long) b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0 && x[0] instanceof PFixedArray) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final PFixedArray x0Fix = (PFixedArray)x0;
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, SHORT_TYPE_CODE);

                            public int getShort(long index) {
                                return (int)x0Fix.getLong(index) & 0xFFFF; // optimization for integer arrays
                                // note: for float array, (int)getLong will differ from getInt for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, SHORT_TYPE_CODE);

                            public int getShort(long index) {
                                return (int)(long)(a0 * x0.getDouble(index) + b) & 0xFFFF;
                                // note: for float array, (int)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat.AutoGeneratedEnd]]
            if (Arrays.isIntType(requiredType)) {
                if (truncateOverflows) {
                    if (allAZeroes) {
                        return InternalUtils.<T>cast(
                            new CopiesArraysImpl.CopiesIntArray(len, (int) b, truncateOverflows, f));
                    } else {
                        return InternalUtils.<T>cast(
                            new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                                final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                    truncateOverflows, x, lf, INT_TYPE_CODE);

                                public int getInt(long index) {
                                    return (int)(a0 * x0.getDouble(index) + b);
                                    // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                                }

                                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                    lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                                }
                            });
                    }
                } else if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesIntArray(len, (int) (long) b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0 && x[0] instanceof PFixedArray) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final PFixedArray x0Fix = (PFixedArray)x0;
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, INT_TYPE_CODE);

                            public int getInt(long index) {
                                return (int)x0Fix.getLong(index); // optimization for integer arrays
                                // note: for float array, (int)getLong will differ from getInt for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, INT_TYPE_CODE);

                            public int getInt(long index) {
                                return (int)(long)(a0 * x0.getDouble(index) + b);
                                // note: for float array, (int)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }

            if (Arrays.isLongType(requiredType)) {
                // truncateOverflows has no effect
                if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesLongArray(len, (long) b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0 && x[0] instanceof PFixedArray) {
                    return InternalUtils.<T>cast(
                        new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                            final PFixedArray x0Fix = (PFixedArray)x0;
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, LONG_TYPE_CODE);

                            public long getLong(long index) {
                                return x0Fix.getLong(index); // optimization
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, LONG_TYPE_CODE);

                            public long getLong(long index) {
                                return (long)(a0 * x0.getDouble(index) + b);
                                // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }

            //[[Repeat() float ==> double;;
            //           Float ==> Double;;
            //           FLOAT ==> DOUBLE;;
            //           \(double\) ==> ]]
            if (Arrays.isFloatType(requiredType)) {
                // truncateOverflows has no effect
                if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesFloatArray(len, (float) b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0) {
                    return InternalUtils.<T>cast(
                        new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, FLOAT_TYPE_CODE);

                            public float getFloat(long index) {
                                return (float)x0.getDouble(index); // optimization
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, FLOAT_TYPE_CODE);

                            public float getFloat(long index) {
                                return (float)(a0 * x0.getDouble(index) + b);
                                // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            if (Arrays.isDoubleType(requiredType)) {
                // truncateOverflows has no effect
                if (allAZeroes) {
                    return InternalUtils.<T>cast(
                        new CopiesArraysImpl.CopiesDoubleArray(len,  b, truncateOverflows, f));
                } else if (a0 == 1.0 && b == 0.0) {
                    return InternalUtils.<T>cast(
                        new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, DOUBLE_TYPE_CODE);

                            public double getDouble(long index) {
                                return x0.getDouble(index); // optimization
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, DOUBLE_TYPE_CODE);

                            public double getDouble(long index) {
                                return (a0 * x0.getDouble(index) + b);
                                // Java automatically truncates double values to Double.MIN_VALUE..MAX_VALUE here
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat.AutoGeneratedEnd]]
            throw new IllegalArgumentException("Illegal required type (" + requiredType
                + "): it must be one of primitive XxxArray interfaces");

        } else { // lf.n() > 1
            final double[] a = lf.isNonweighted() ? null : lf.a();
            final double a0 = lf.a(0);
            if (a != null) {
                assert a.length == n;
            }

            if (Arrays.isBitType(requiredType)) {
                return InternalUtils.<T>cast(
                    new FuncBitArrayWithArguments(truncateOverflows, len, f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, BIT_TYPE_CODE);

                        public boolean getBit(long index) {
                            double sum = 0.0;
                            if (a == null) {
                                for (int k = 0; k < n; k++) {
                                    sum += this.x[k].getDouble(index);
                                }
                                sum *= a0;
                            } else {
                                for (int k = 0; k < n; k++) {
                                    sum += a[k] * this.x[k].getDouble(index);
                                }
                            }
                            return sum + b != 0.0;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }
                    });
            }

            if (Arrays.isCharType(requiredType)) {
                if (truncateOverflows) {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, CHAR_TYPE_CODE);

                            public char getChar(long index) {
                                int v;
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    v = (int)(sum * a0 + b);
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    v = (int)sum;
                                }
                                return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncCharArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, CHAR_TYPE_CODE);

                            public char getChar(long index) {
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    return (char)(long)(sum * a0 + b);
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    return (char)(long)sum;
                                }
                                // note: for float array, (char)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat() byte ==> short;;
            //           Byte ==> Short;;
            //           BYTE ==> SHORT;;
            //           0xFF ==> 0xFFFF]]
            if (Arrays.isByteType(requiredType)) {
                if (truncateOverflows) {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, BYTE_TYPE_CODE);

                            public int getByte(long index) {
                                int v;
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    v = (int)(sum * a0 + b);
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    v = (int)sum;
                                }
                                return v < 0 ? 0 : Math.min(v, 0xFF);
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncByteArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, BYTE_TYPE_CODE);

                            public int getByte(long index) {
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    return (int)(long)(sum * a0 + b) & 0xFF;
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    return (int)(long)sum & 0xFF;
                                }
                                // note: for float array, (int)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            if (Arrays.isShortType(requiredType)) {
                if (truncateOverflows) {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, SHORT_TYPE_CODE);

                            public int getShort(long index) {
                                int v;
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    v = (int)(sum * a0 + b);
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    v = (int)sum;
                                }
                                return v < 0 ? 0 : Math.min(v, 0xFFFF);
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncShortArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, SHORT_TYPE_CODE);

                            public int getShort(long index) {
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    return (int)(long)(sum * a0 + b) & 0xFFFF;
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    return (int)(long)sum & 0xFFFF;
                                }
                                // note: for float array, (int)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }
            //[[Repeat.AutoGeneratedEnd]]
            if (Arrays.isIntType(requiredType)) {
                if (truncateOverflows) {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, INT_TYPE_CODE);

                            public int getInt(long index) {
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    return (int)(sum * a0 + b);
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    return (int)sum;
                                }
                                // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                } else {
                    return InternalUtils.<T>cast(
                        new FuncIntArrayWithArguments(truncateOverflows, len, f, x) {
                            final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                                truncateOverflows, x, lf, INT_TYPE_CODE);

                            public int getInt(long index) {
                                if (a == null) {
                                    double sum = 0.0;
                                    for (int k = 0; k < n; k++) {
                                        sum += this.x[k].getDouble(index);
                                    }
                                    return (int)(long)(sum * a0 + b);
                                } else {
                                    double sum = b;
                                    for (int k = 0; k < n; k++) {
                                        sum += a[k] * this.x[k].getDouble(index);
                                    }
                                    return (int)(long)sum;
                                }
                                // note: for float array, (int)(long)v will differ from (int)v for very large floats
                            }

                            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                                lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                            }
                        });
                }
            }

            //[[Repeat() long(?!\s+(index|arrayPos)) ==> float,,double;;
            //           Long                        ==> Float,,Double;;
            //           LONG                        ==> FLOAT,,DOUBLE;;
            //           (x0\.getFloat)              ==> (float)x0.getDouble,,$1;;
            //           \(double\)                  ==> ,, ]]
            if (Arrays.isLongType(requiredType)) {
                // truncateOverflows has no effect
                return InternalUtils.<T>cast(
                    new FuncLongArrayWithArguments(truncateOverflows, len, f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, LONG_TYPE_CODE);

                        public long getLong(long index) {
                            if (a == null) {
                                double sum = 0.0;
                                for (int k = 0; k < n; k++) {
                                    sum += this.x[k].getDouble(index);
                                }
                                return (long)(sum * a0 + b);
                            } else {
                                double sum = b;
                                for (int k = 0; k < n; k++) {
                                    sum += a[k] * this.x[k].getDouble(index);
                                }
                                return (long)sum;
                            }
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }
                    });
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            if (Arrays.isFloatType(requiredType)) {
                // truncateOverflows has no effect
                return InternalUtils.<T>cast(
                    new FuncFloatArrayWithArguments(truncateOverflows, len, f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, FLOAT_TYPE_CODE);

                        public float getFloat(long index) {
                            if (a == null) {
                                double sum = 0.0;
                                for (int k = 0; k < n; k++) {
                                    sum += this.x[k].getDouble(index);
                                }
                                return (float)(sum * a0 + b);
                            } else {
                                double sum = b;
                                for (int k = 0; k < n; k++) {
                                    sum += a[k] * this.x[k].getDouble(index);
                                }
                                return (float)sum;
                            }
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }
                    });
            }
            if (Arrays.isDoubleType(requiredType)) {
                // truncateOverflows has no effect
                return InternalUtils.<T>cast(
                    new FuncDoubleArrayWithArguments(truncateOverflows, len, f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, DOUBLE_TYPE_CODE);

                        public double getDouble(long index) {
                            if (a == null) {
                                double sum = 0.0;
                                for (int k = 0; k < n; k++) {
                                    sum += this.x[k].getDouble(index);
                                }
                                return (sum * a0 + b);
                            } else {
                                double sum = b;
                                for (int k = 0; k < n; k++) {
                                    sum += a[k] * this.x[k].getDouble(index);
                                }
                                return sum;
                            }
                            // Java automatically truncates float values to Double.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }
                    });
            }
            //[[Repeat.AutoGeneratedEnd]]
            throw new IllegalArgumentException("Illegal required type (" + requiredType
                + "): it must be one of primitive XxxArray interfaces");
        }
    }

    private static <T extends UpdatablePArray> T asUpdatableLinearFunc(
        boolean truncateOverflows,
        Class<? extends T> requiredType,
        Func.Updatable f, UpdatablePArray x)
    {
        Objects.requireNonNull(x, "Null x argument");
        Objects.requireNonNull(requiredType, "Null requiredType argument");
        final double b, a, aInv;
        if (f == Func.UPDATABLE_IDENTITY) {
            a = 1.0;
            b = 0.0;
            aInv = 1.0;
        } else if (f instanceof LinearFunc.Updatable) {
            double[] allA = ((LinearFunc)f).a();
            a = allA[0];
            b = ((LinearFunc)f).b();
            aInv = 1.0 / a;
            if (allA.length != ((LinearFunc)f).n()) {
                throw new AssertionError("Illegal implementation of LinearFunc: n()!=a().length");
            }
        } else {
            throw new AssertionError("asUpdatableLinearFunc is called for unsupported function " + f);
        }
        final LinearFunc.Updatable lf = LinearFunc.getUpdatableInstance(b, a);
        final boolean truncateInSet = truncateOverflows && x instanceof PFixedArray
            && !(x instanceof LongArray || x instanceof BitArray);
        final boolean longPrecisionInSet = !truncateOverflows && x instanceof PFixedArray
            && !(x instanceof LongArray || x instanceof BitArray);
        final double minXElement = truncateInSet ? ((PFixedArray)x).minPossibleValue() : Double.MIN_VALUE;
        final double maxXElement = truncateInSet ? ((PFixedArray)x).maxPossibleValue() : Double.MAX_VALUE;
        // if truncation in set is necessary, these values are stored precisely by "double" type

        if (Arrays.isBitType(requiredType)) {
            final double vFalse = -b / a + 0.0, vTrue = (1.0 - b) / a + 0.0;
            // "/ a" is more precise than "* aInv" and can help to produce precise 0.0 in ax+b
            // adding 0.0 replaces -0.0 with +0.0: necessary for compatibility with optimization branch
            return InternalUtils.<T>cast(
                new UpdatableFuncBitArray(truncateOverflows, x.length(), f, x) {
                    final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                        truncateOverflows, x, lf, BIT_TYPE_CODE);
                    final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                        truncateOverflows, x[0], lf, BIT_TYPE_CODE);

                    public boolean getBit(long index) {
                        return a * x[0].getDouble(index) != -b;
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }

                    public void setBit(long index, boolean value) {
                        if (truncateInSet) {
                            double v = value ? vTrue : vFalse;
                            x[0].setDouble(index, v < minXElement ? minXElement : Math.min(v, maxXElement));
                        } else {
                            x[0].setDouble(index, value ? vTrue : vFalse);
                        }
                    }

                    public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                        lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                        return this;
                    }
                });
        }

        if (Arrays.isCharType(requiredType)) {
            if (truncateOverflows) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncCharArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, CHAR_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, CHAR_TYPE_CODE);

                        public char getChar(long index) {
                            int v = (int)(a * x[0].getDouble(index) + b);
                            return v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                    (char)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setChar(long index, char value) {
                            if (truncateInSet) {
                                double v = (value - b) * aInv;
                                x[0].setDouble(index,
                                    v < minXElement ? minXElement : Math.min(v, maxXElement));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0 && x instanceof PFixedArray) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncCharArray(truncateOverflows, x.length(), f, x) {
                        final PFixedArray x0Fix = (PFixedArray)x[0];
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, CHAR_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, CHAR_TYPE_CODE);

                        public char getChar(long index) {
                            return (char)x0Fix.getLong(index); // optimization for integer arrays
                            // note: for float array, (char)getLong will differ from getInt for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setChar(long index, char value) {
                            x[0].setInt(index, value); // optimization for integer arrays
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncCharArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, CHAR_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, CHAR_TYPE_CODE);

                        public char getChar(long index) {
                            return (char)(long)(a * x[0].getDouble(index) + b);
                            // note: for float array, (char)(long)v will differ from (int)v for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setChar(long index, char value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)((value - b) * aInv));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }
        //[[Repeat() byte ==> short;;
        //           Byte ==> Short;;
        //           BYTE ==> SHORT;;
        //           0xFF ==> 0xFFFF]]
        if (Arrays.isByteType(requiredType)) {
            if (truncateOverflows) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncByteArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo =new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, BYTE_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, BYTE_TYPE_CODE);

                        public int getByte(long index) {
                            int v = (int)(a * x[0].getDouble(index) + b);
                            return v < 0 ? 0 : Math.min(v, 0xFF);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setByte(long index, byte value) {
                            if (truncateInSet) {
                                double v = ((value & 0xFF) - b) * aInv;
                                x[0].setDouble(index,
                                    v < minXElement ? minXElement : Math.min(v, maxXElement));
                            } else {
                                x[0].setDouble(index, ((value & 0xFF) - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0 && x instanceof PFixedArray) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncByteArray(truncateOverflows, x.length(), f, x) {
                        final PFixedArray x0Fix = (PFixedArray)x[0];
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, BYTE_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, BYTE_TYPE_CODE);

                        public int getByte(long index) {
                            return (int)x0Fix.getLong(index) & 0xFF; // optimization for integer arrays
                            // note: for float array, (int)getLong will differ from getInt for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setByte(long index, byte value) {
                            x[0].setInt(index, value & 0xFF); // optimization for integer arrays
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncByteArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, BYTE_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, BYTE_TYPE_CODE);

                        public int getByte(long index) {
                            return (int)(long)(a * x[0].getDouble(index) + b) & 0xFF;
                            // note: for float array, (int)(long)v will differ from (int)v for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setByte(long index, byte value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)(((value & 0xFF) - b) * aInv));
                            } else {
                                x[0].setDouble(index, ((value & 0xFF) - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isShortType(requiredType)) {
            if (truncateOverflows) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncShortArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo =new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, SHORT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, SHORT_TYPE_CODE);

                        public int getShort(long index) {
                            int v = (int)(a * x[0].getDouble(index) + b);
                            return v < 0 ? 0 : Math.min(v, 0xFFFF);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setShort(long index, short value) {
                            if (truncateInSet) {
                                double v = ((value & 0xFFFF) - b) * aInv;
                                x[0].setDouble(index,
                                    v < minXElement ? minXElement : Math.min(v, maxXElement));
                            } else {
                                x[0].setDouble(index, ((value & 0xFFFF) - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0 && x instanceof PFixedArray) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncShortArray(truncateOverflows, x.length(), f, x) {
                        final PFixedArray x0Fix = (PFixedArray)x[0];
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, SHORT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, SHORT_TYPE_CODE);

                        public int getShort(long index) {
                            return (int)x0Fix.getLong(index) & 0xFFFF; // optimization for integer arrays
                            // note: for float array, (int)getLong will differ from getInt for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setShort(long index, short value) {
                            x[0].setInt(index, value & 0xFFFF); // optimization for integer arrays
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncShortArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, SHORT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, SHORT_TYPE_CODE);

                        public int getShort(long index) {
                            return (int)(long)(a * x[0].getDouble(index) + b) & 0xFFFF;
                            // note: for float array, (int)(long)v will differ from (int)v for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setShort(long index, short value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)(((value & 0xFFFF) - b) * aInv));
                            } else {
                                x[0].setDouble(index, ((value & 0xFFFF) - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        if (Arrays.isIntType(requiredType)) {
            if (truncateOverflows) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncIntArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, INT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, INT_TYPE_CODE);

                        public int getInt(long index) {
                            return (int)(a * x[0].getDouble(index) + b);
                            // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setInt(long index, int value) {
                            if (truncateInSet) {
                                double v = (value - b) * aInv;
                                x[0].setDouble(index,
                                    v < minXElement ? minXElement : Math.min(v, maxXElement));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0 && x instanceof PFixedArray) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncIntArray(truncateOverflows, x.length(), f, x) {
                        final PFixedArray x0Fix = (PFixedArray)x[0];
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, INT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, INT_TYPE_CODE);

                        public int getInt(long index) {
                            return (int)x0Fix.getLong(index); // optimization for integer arrays
                            // note: for float array, (int)getLong will differ from getInt for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setInt(long index, int value) {
                            x[0].setInt(index, value); // optimization for integer arrays
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncIntArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, INT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, INT_TYPE_CODE);

                        public int getInt(long index) {
                            return (int)(long)(a * x[0].getDouble(index) + b);
                            // note: for float array, (int)(long)v will differ from (int)v for very large floats
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setInt(long index, int value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)((value - b) * aInv));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }

        if (Arrays.isLongType(requiredType)) {
            // truncateOverflows has no effect for get methods
            if (truncateInSet) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncLongArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, LONG_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, LONG_TYPE_CODE);

                        public long getLong(long index) {
                            return (long)(a * x[0].getDouble(index) + b);
                            // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setLong(long index, long value) {
                            double v = (value - b) * aInv;
                            x[0].setDouble(index, v < minXElement ? minXElement : Math.min(v, maxXElement));
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0 && x instanceof PFixedArray) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncLongArray(truncateOverflows, x.length(), f, x) {
                        final PFixedArray x0Fix = (PFixedArray)x[0];
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, LONG_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, LONG_TYPE_CODE);

                        public long getLong(long index) {
                            return x0Fix.getLong(index); // optimization
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setLong(long index, long value) {
                            x[0].setLong(index, value); // optimization
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncLongArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, LONG_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, LONG_TYPE_CODE);

                        public long getLong(long index) {
                            return (long)(a * x[0].getDouble(index) + b);
                            // Java automatically truncates float values to Long.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setLong(long index, long value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)((value - b) * aInv));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }

        //[[Repeat() float ==> double;;
        //           Float ==> Double;;
        //           FLOAT ==> DOUBLE;;
        //           \(double\) ==> ]]
        if (Arrays.isFloatType(requiredType)) {
            // truncateOverflows has no effect for get methods
            if (truncateInSet) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncFloatArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, FLOAT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, FLOAT_TYPE_CODE);

                        public float getFloat(long index) {
                            return (float)(a * x[0].getDouble(index) + b);
                            // Java automatically truncates float values to Integer.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setFloat(long index, float value) {
                            double v = (value - b) * aInv;
                            x[0].setDouble(index, v < minXElement ? minXElement : Math.min(v, maxXElement));
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncFloatArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, FLOAT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, FLOAT_TYPE_CODE);

                        public float getFloat(long index) {
                            return (float)x[0].getDouble(index); // optimization
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setFloat(long index, float value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)value);
                            } else {
                                x[0].setDouble(index, value);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncFloatArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, FLOAT_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, FLOAT_TYPE_CODE);

                        public float getFloat(long index) {
                            return (float)(a * x[0].getDouble(index) + b);
                            // Java automatically truncates float values to Float.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setFloat(long index, float value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)((value - b) * aInv));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (Arrays.isDoubleType(requiredType)) {
            // truncateOverflows has no effect for get methods
            if (truncateInSet) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncDoubleArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, DOUBLE_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, DOUBLE_TYPE_CODE);

                        public double getDouble(long index) {
                            return (a * x[0].getDouble(index) + b);
                            // Java automatically truncates double values to Integer.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setDouble(long index, double value) {
                            double v = (value - b) * aInv;
                            x[0].setDouble(index, v < minXElement ? minXElement : Math.min(v, maxXElement));
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else if (a == 1.0 && b == 0.0) {
                return InternalUtils.<T>cast(
                    new UpdatableFuncDoubleArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, DOUBLE_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, DOUBLE_TYPE_CODE);

                        public double getDouble(long index) {
                            return x[0].getDouble(index); // optimization
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setDouble(long index, double value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)value);
                            } else {
                                x[0].setDouble(index, value);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            } else {
                return InternalUtils.<T>cast(
                    new UpdatableFuncDoubleArray(truncateOverflows, x.length(), f, x) {
                        final ArraysLinearGetDataOp lgdo = new ArraysLinearGetDataOp(
                            truncateOverflows, x, lf, DOUBLE_TYPE_CODE);
                        final ArraysLinearSetDataOp lsdo = new ArraysLinearSetDataOp(
                            truncateOverflows, x[0], lf, DOUBLE_TYPE_CODE);

                        public double getDouble(long index) {
                            return (a * x[0].getDouble(index) + b);
                            // Java automatically truncates double values to Double.MIN_VALUE..MAX_VALUE here
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            lgdo.getData(arrayPos, destArray, destArrayOffset, count);
                        }

                        public void setDouble(long index, double value) {
                            if (longPrecisionInSet) {
                                x[0].setLong(index, (long)((value - b) * aInv));
                            } else {
                                x[0].setDouble(index, (value - b) * aInv);
                            }
                        }

                        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                            lsdo.setData(arrayPos, srcArray, srcArrayOffset, count);
                            return this;
                        }
                    });
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal required type (" + requiredType
            + "): it must be one of primitive XxxArray or UpdatableXxxArray interfaces");
    }

    private static <T extends PArray> PArray asSubtractionFunc(
        boolean truncateOverflows,
        Func f, final PArray... x)
    {
        if (x.length != 2) {
            throw new IllegalArgumentException("x.length must be 2");
        }
        if (x[0] instanceof BitArray) {
            return new FuncBitArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final BitArray x0 = (BitArray)x[0],
                    x1 = (BitArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1, null, false, false);

                public boolean getBit(long index) {
                    return x0.getBit(index) & !x1.getBit(index);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }

                public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
                    dgdo.getBits(arrayPos, destArray, destArrayOffset, count);
                }

                @Override
                public long nextQuickPosition(long position) {
                    return x0.nextQuickPosition(position);
                }
            };
        }

        //[[Repeat() int\s+(getByte|result) ==> char $1,,int $1;;
        //           (return\s+v\b)(.*?); ==> return (char) (v$2);,,$1$2; ;;
        //           (return\s+Math\b)(.*?); ==> return (char) (Math$2);,,$1$2; ;;
        //           byte ==> char,,short;;
        //           Byte ==> Char,,Short;;
        //           (\s+&\s+0xFF) ==> $1FF,,...]]
        if (x[0] instanceof ByteArray) {
            if (truncateOverflows) {
                return new FuncByteArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final ByteArray x0 = (ByteArray)x[0],
                        x1 = (ByteArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getByteSubtractOp(truncateOverflows), false, truncateOverflows);

                    public int getByte(long index) {
                        int v = x0.getByte(index) - x1.getByte(index);
                        return Math.max(v, 0);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            } else {
                return new FuncByteArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final ByteArray x0 = (ByteArray)x[0],
                        x1 = (ByteArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getByteSubtractOp(truncateOverflows), false, truncateOverflows);

                    public int getByte(long index) {
                        int v = x0.getByte(index) - x1.getByte(index);
                        return v & 0xFF;
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (x[0] instanceof CharArray) {
            if (truncateOverflows) {
                return new FuncCharArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final CharArray x0 = (CharArray)x[0],
                        x1 = (CharArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getCharSubtractOp(truncateOverflows), false, truncateOverflows);

                    public char getChar(long index) {
                        int v = x0.getChar(index) - x1.getChar(index);
                        return (char) (Math.max(v, 0));
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            } else {
                return new FuncCharArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final CharArray x0 = (CharArray)x[0],
                        x1 = (CharArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getCharSubtractOp(truncateOverflows), false, truncateOverflows);

                    public char getChar(long index) {
                        int v = x0.getChar(index) - x1.getChar(index);
                        return (char) (v & 0xFFFF);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            }
        }
        if (x[0] instanceof ShortArray) {
            if (truncateOverflows) {
                return new FuncShortArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final ShortArray x0 = (ShortArray)x[0],
                        x1 = (ShortArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getShortSubtractOp(truncateOverflows), false, truncateOverflows);

                    public int getShort(long index) {
                        int v = x0.getShort(index) - x1.getShort(index);
                        return Math.max(v, 0);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            } else {
                return new FuncShortArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final ShortArray x0 = (ShortArray)x[0],
                        x1 = (ShortArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getShortSubtractOp(truncateOverflows), false, truncateOverflows);

                    public int getShort(long index) {
                        int v = x0.getShort(index) - x1.getShort(index);
                        return v & 0xFFFF;
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedEnd]]

        if (x[0] instanceof IntArray) {
            if (truncateOverflows) {
                return new FuncIntArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final IntArray x0 = (IntArray)x[0],
                        x1 = (IntArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getIntSubtractOp(truncateOverflows), false, truncateOverflows);

                    public int getInt(long index) {
                        long v = (long)x0.getInt(index) - (long)x1.getInt(index);
                        return v < Integer.MIN_VALUE ? Integer.MIN_VALUE :
                            v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)v;
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            } else {
                return new FuncIntArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final IntArray x0 = (IntArray)x[0],
                        x1 = (IntArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getIntSubtractOp(truncateOverflows), false, truncateOverflows);

                    public int getInt(long index) {
                        return x0.getInt(index) - x1.getInt(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            }
        }

        //[[Repeat() long(?!\s+(index|arrayPos)) ==> float,,double;;
        //           Long                        ==> Float,,Double]]
        if (x[0] instanceof LongArray) {
            return new FuncLongArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final LongArray x0 = (LongArray)x[0],
                    x1 = (LongArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getLongSubtractOp(truncateOverflows), false, truncateOverflows);

                public long getLong(long index) {
                    return x0.getLong(index) - x1.getLong(index);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (x[0] instanceof FloatArray) {
            return new FuncFloatArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final FloatArray x0 = (FloatArray)x[0],
                    x1 = (FloatArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getFloatSubtractOp(truncateOverflows), false, truncateOverflows);

                public float getFloat(long index) {
                    return x0.getFloat(index) - x1.getFloat(index);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof DoubleArray) {
            return new FuncDoubleArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final DoubleArray x0 = (DoubleArray)x[0],
                    x1 = (DoubleArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getDoubleSubtractOp(truncateOverflows), false, truncateOverflows);

                public double getDouble(long index) {
                    return x0.getDouble(index) - x1.getDouble(index);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new IllegalArgumentException("Illegal array type (" + x[0]
            + "): it must implement one of primitive XxxArray interfaces");
    }

    private static <T extends PArray> PArray asAbsDiffFuncArray(
        boolean truncateOverflows,
        Func f, final PArray... x)
    {
        if (x.length != 2) {
            throw new IllegalArgumentException("x.length must be 2");
        }
        if (x[0] instanceof BitArray) {
            return new FuncBitArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final BitArray x0 = (BitArray)x[0],
                    x1 = (BitArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1, null, true, false);

                public boolean getBit(long index) {
                    return x0.getBit(index) ^ x1.getBit(index);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }

                public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
                    dgdo.getBits(arrayPos, destArray, destArrayOffset, count);
                }

                @Override
                public long nextQuickPosition(long position) {
                    return x0.nextQuickPosition(position);
                }
            };
        }

        //[[Repeat() int\s+(getByte|result|v) ==> char $1,,int $1,,long $1,,float $1,,double $1;;
        //           (return\s+v)(.*?); ==> return (char)(v$2);,,$1$2;,,...;;
        //           byte ==> char,,short,,long,,float,,double;;
        //           Byte ==> Char,,Short,,Long,,Float,,Double]]
        if (x[0] instanceof ByteArray) {
            return new FuncByteArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final ByteArray x0 = (ByteArray)x[0],
                    x1 = (ByteArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getByteAbsDiffOp(), true, truncateOverflows);

                public int getByte(long index) {
                    int v0 = x0.getByte(index), v1 = x1.getByte(index);
                    return v0 >= v1 ? v0 - v1 : v1 - v0;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (x[0] instanceof CharArray) {
            return new FuncCharArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final CharArray x0 = (CharArray)x[0],
                    x1 = (CharArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getCharAbsDiffOp(), true, truncateOverflows);

                public char getChar(long index) {
                    char v0 = x0.getChar(index), v1 = x1.getChar(index);
                    return (char)(v0 >= v1 ? v0 - v1 : v1 - v0);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof ShortArray) {
            return new FuncShortArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final ShortArray x0 = (ShortArray)x[0],
                    x1 = (ShortArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getShortAbsDiffOp(), true, truncateOverflows);

                public int getShort(long index) {
                    int v0 = x0.getShort(index), v1 = x1.getShort(index);
                    return v0 >= v1 ? v0 - v1 : v1 - v0;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof LongArray) {
            return new FuncLongArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final LongArray x0 = (LongArray)x[0],
                    x1 = (LongArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getLongAbsDiffOp(), true, truncateOverflows);

                public long getLong(long index) {
                    long v0 = x0.getLong(index), v1 = x1.getLong(index);
                    return v0 >= v1 ? v0 - v1 : v1 - v0;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof FloatArray) {
            return new FuncFloatArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final FloatArray x0 = (FloatArray)x[0],
                    x1 = (FloatArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getFloatAbsDiffOp(), true, truncateOverflows);

                public float getFloat(long index) {
                    float v0 = x0.getFloat(index), v1 = x1.getFloat(index);
                    return v0 >= v1 ? v0 - v1 : v1 - v0;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        if (x[0] instanceof DoubleArray) {
            return new FuncDoubleArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                final DoubleArray x0 = (DoubleArray)x[0],
                    x1 = (DoubleArray)x[1];
                final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                    ArraysDiffGetDataOp.getDoubleAbsDiffOp(), true, truncateOverflows);

                public double getDouble(long index) {
                    double v0 = x0.getDouble(index), v1 = x1.getDouble(index);
                    return v0 >= v1 ? v0 - v1 : v1 - v0;
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                }
            };
        }
        //[[Repeat.AutoGeneratedEnd]]

        if (x[0] instanceof IntArray) {
            if (truncateOverflows) {
                return new FuncIntArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final IntArray x0 = (IntArray)x[0],
                        x1 = (IntArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getIntAbsDiffOp(truncateOverflows), true, truncateOverflows);

                    public int getInt(long index) {
                        long v = (long)x0.getInt(index) - (long)x1.getInt(index);
                        if (v < 0) {
                            v = -v;
                        }
                        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)v;
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            } else {
                return new FuncIntArrayWithArguments(truncateOverflows, x[0].length(), f, x) {
                    final IntArray x0 = (IntArray)x[0],
                        x1 = (IntArray)x[1];
                    final ArraysDiffGetDataOp dgdo = new ArraysDiffGetDataOp(this, x0, x1,
                        ArraysDiffGetDataOp.getIntAbsDiffOp(truncateOverflows), true, truncateOverflows);

                    public int getInt(long index) {
                        int v0 = x0.getInt(index), v1 = x1.getInt(index);
                        return v0 >= v1 ? v0 - v1 : v1 - v0;
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        dgdo.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            }
        }

        throw new IllegalArgumentException("Illegal array type (" + x[0]
            + "): it must implement one of primitive XxxArray interfaces");
    }

    private static <T extends PArray> T asConstantFuncArray(
        boolean truncateOverflows,
        Class<? extends T> requiredType, Func f, long len)
    {
        return asLinearFuncArray(truncateOverflows, requiredType, f, new PArray[0], len);
    }

    private static <T extends PArray> T asIdentityFuncArray(
        boolean truncateOverflows,
        Class<? extends T> requiredType, Func f, final PArray x)
    {
        return asLinearFuncArray(truncateOverflows, requiredType, f, new PArray[]{x}, x.length());
    }

    private static <T extends UpdatablePArray> T asUpdatableIdentityFunc(
        boolean truncateOverflows,
        Class<? extends T> requiredType, Func.Updatable f, final UpdatablePArray x)
    {
        return asUpdatableLinearFunc(truncateOverflows, requiredType, f, x);
    }

    interface FuncArray {
        Func f();

        boolean truncateOverflows();
    }

    interface CoordFuncArray extends FuncArray {
        long[] dimensions();
    }

    interface FuncArrayWithArguments extends FuncArray {
        // no methods necessary: the arguments can be retrieved via standard AbstractArray.underlyingArrays field
    }

    interface OptimizationHelperInfo {
        Object getOptimizationHelperInfo();

        void setOptimizationHelperInfo(Object helperInfo);
    }

    //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
    //           BIT ==> CHAR,,BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
    //           boolean(?!\s+(isLazy|truncateOverflows)) ==> char,,byte,,short,,int,,long,,float,,double]]
    private static abstract class AbstractFuncBitArray
        extends AbstractBitArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncBitArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncBitArray
        extends AbstractFuncBitArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncBitArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, BIT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array boolean[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncBitArrayWithArguments
        extends AbstractFuncBitArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncBitArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, BIT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array boolean[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static abstract class AbstractFuncCharArray
        extends AbstractCharArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncCharArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncCharArray
        extends AbstractFuncCharArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncCharArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, CHAR_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array char[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncCharArrayWithArguments
        extends AbstractFuncCharArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncCharArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, CHAR_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array char[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class AbstractFuncByteArray
        extends AbstractByteArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncByteArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncByteArray
        extends AbstractFuncByteArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncByteArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, BYTE_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array byte[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncByteArrayWithArguments
        extends AbstractFuncByteArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncByteArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, BYTE_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array byte[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class AbstractFuncShortArray
        extends AbstractShortArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncShortArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncShortArray
        extends AbstractFuncShortArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncShortArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, SHORT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array short[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncShortArrayWithArguments
        extends AbstractFuncShortArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncShortArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, SHORT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array short[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class AbstractFuncIntArray
        extends AbstractIntArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncIntArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncIntArray
        extends AbstractFuncIntArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncIntArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, INT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array int[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncIntArrayWithArguments
        extends AbstractFuncIntArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncIntArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, INT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array int[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class AbstractFuncLongArray
        extends AbstractLongArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncLongArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncLongArray
        extends AbstractFuncLongArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncLongArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, LONG_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array long[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncLongArrayWithArguments
        extends AbstractFuncLongArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncLongArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, LONG_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array long[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class AbstractFuncFloatArray
        extends AbstractFloatArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncFloatArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncFloatArray
        extends AbstractFuncFloatArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncFloatArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, FLOAT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array float[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncFloatArrayWithArguments
        extends AbstractFuncFloatArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncFloatArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, FLOAT_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array float[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class AbstractFuncDoubleArray
        extends AbstractDoubleArray
        implements FuncArray, OptimizationHelperInfo
    {
        final Func f;
        final boolean truncateOverflows;
        private volatile Object optimizationHelperInfo;

        AbstractFuncDoubleArray(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public boolean isLazy() { // all classes in this library are lazy, even without underlying arrays
            return true;
        }

        public abstract String toString();
    }

    private static abstract class CoordFuncDoubleArray
        extends AbstractFuncDoubleArray
        implements CoordFuncArray
    {
        private final ArraysAnyCoordFuncGetDataOp acfgdo;
        final long[] dim;

        protected CoordFuncDoubleArray(boolean truncateOverflows, long length, Func f, long[] dim) {
            super(truncateOverflows, length, f);
            this.dim = dim.clone();
            this.acfgdo = new ArraysAnyCoordFuncGetDataOp(this, truncateOverflows, dim, f, DOUBLE_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            acfgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public long[] dimensions() {
            return this.dim.clone();
        }

        public String toString() {
            return "immutable AlgART array double[" + length + "]" + " built by " + f
                + " (function of " + dim.length + " indexes in "
                + (dim.length == 1 ? "one-dimensional array)" : JArrays.toString(dim, "x", 1000) + " matrix)");
        }
    }

    private static abstract class FuncDoubleArrayWithArguments
        extends AbstractFuncDoubleArray
        implements FuncArrayWithArguments
    {
        private final ArraysAnyFuncGetDataOp afgdo;
        final PArray[] x;
        final PArray x0;

        FuncDoubleArrayWithArguments(boolean truncateOverflows, long length, Func f, PArray... x) {
            super(truncateOverflows, length, f, x);
            this.x = x.clone();
            this.x0 = x[0];
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, DOUBLE_TYPE_CODE);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        public String toString() {
            return "immutable AlgART array double[" + length + "]" + " built by " + f
                + " with " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    //[[Repeat.AutoGeneratedEnd]]

    //[[Repeat() Bit                             ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
    //           BIT                             ==> CHAR,,BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
    //           boolean(?!\s+truncateOverflows) ==> char,,byte,,short,,int,,long,,float,,double;;
    //           (byte|short)\s+get(Byte|Short)  ==> ,,int get$2,,int get$2,, ,, ...]]

    private static abstract class UpdatableFuncBitArray
        extends AbstractUpdatableBitArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncBitArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, BIT_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public BitArray asImmutable() {
            final BitArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            BitArray result = // use BitArray here for more thorough control by the compiler
                new FuncBitArrayWithArguments(truncateOverflows, length, f, ua) {
                    public boolean getBit(long index) {
                        return parent.getBit(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array boolean[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]

    private static abstract class UpdatableFuncCharArray
        extends AbstractUpdatableCharArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncCharArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, CHAR_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public CharArray asImmutable() {
            final CharArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            CharArray result = // use CharArray here for more thorough control by the compiler
                new FuncCharArrayWithArguments(truncateOverflows, length, f, ua) {
                    public char getChar(long index) {
                        return parent.getChar(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array char[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class UpdatableFuncByteArray
        extends AbstractUpdatableByteArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncByteArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, BYTE_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public ByteArray asImmutable() {
            final ByteArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            ByteArray result = // use ByteArray here for more thorough control by the compiler
                new FuncByteArrayWithArguments(truncateOverflows, length, f, ua) {
                    public int getByte(long index) {
                        return parent.getByte(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array byte[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class UpdatableFuncShortArray
        extends AbstractUpdatableShortArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncShortArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, SHORT_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public ShortArray asImmutable() {
            final ShortArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            ShortArray result = // use ShortArray here for more thorough control by the compiler
                new FuncShortArrayWithArguments(truncateOverflows, length, f, ua) {
                    public int getShort(long index) {
                        return parent.getShort(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array short[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class UpdatableFuncIntArray
        extends AbstractUpdatableIntArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncIntArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, INT_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public IntArray asImmutable() {
            final IntArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            IntArray result = // use IntArray here for more thorough control by the compiler
                new FuncIntArrayWithArguments(truncateOverflows, length, f, ua) {
                    public int getInt(long index) {
                        return parent.getInt(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array int[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class UpdatableFuncLongArray
        extends AbstractUpdatableLongArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncLongArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, LONG_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public LongArray asImmutable() {
            final LongArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            LongArray result = // use LongArray here for more thorough control by the compiler
                new FuncLongArrayWithArguments(truncateOverflows, length, f, ua) {
                    public long getLong(long index) {
                        return parent.getLong(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array long[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class UpdatableFuncFloatArray
        extends AbstractUpdatableFloatArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncFloatArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, FLOAT_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public FloatArray asImmutable() {
            final FloatArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            FloatArray result = // use FloatArray here for more thorough control by the compiler
                new FuncFloatArrayWithArguments(truncateOverflows, length, f, ua) {
                    public float getFloat(long index) {
                        return parent.getFloat(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array float[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }

    private static abstract class UpdatableFuncDoubleArray
        extends AbstractUpdatableDoubleArray
        implements FuncArrayWithArguments, OptimizationHelperInfo
    {
        final Func.Updatable f;
        final boolean truncateOverflows;
        final UpdatablePArray[] x;
        private final ArraysAnyFuncGetDataOp afgdo;
        private volatile Object optimizationHelperInfo;

        UpdatableFuncDoubleArray(boolean truncateOverflows,
            long length, Func.Updatable f, UpdatablePArray... x)
        {
            super(length, true, x);
            this.truncateOverflows = truncateOverflows;
            this.f = f;
            this.x = x.clone();
            this.afgdo = new ArraysAnyFuncGetDataOp(truncateOverflows, this.x, f, DOUBLE_TYPE_CODE);
        }

        public boolean truncateOverflows() {
            return this.truncateOverflows;
        }

        public Func f() {
            return this.f;
        }

        public Object getOptimizationHelperInfo() {
            return optimizationHelperInfo;
        }

        public void setOptimizationHelperInfo(Object optimizationHelperInfo) {
            this.optimizationHelperInfo = optimizationHelperInfo;
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            afgdo.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public DoubleArray asImmutable() {
            final DoubleArray parent = this;
            PArray[] ua = new PArray[x.length];
            for (int k = 0; k < ua.length; k++) {
                ua[k] = x[k];
            }
            DoubleArray result = // use DoubleArray here for more thorough control by the compiler
                new FuncDoubleArrayWithArguments(truncateOverflows, length, f, ua) {
                    public double getDouble(long index) {
                        return parent.getDouble(index);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        parent.getData(arrayPos, destArray, destArrayOffset, count);
                    }
                };
            return result;
        }

        public String toString() {
            return "unresizable AlgART array double[" + length + "]" + " built by " + f + " with "
                + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : "");
        }
    }
    //[[Repeat.AutoGeneratedEnd]]

    private static <T extends Array> T[] assertTypeAndCast(Class<T> requiredType, Array[] x) {
        for (int k = 0; k < x.length; k++) {
            if (!requiredType.isInstance(x[k])) {
                throw new AssertionError("x[" + k + "] is not an instance of " + requiredType);
            }
        }
        T[] result = InternalUtils.cast(java.lang.reflect.Array.newInstance(requiredType, x.length));
        System.arraycopy(x, 0, result, 0, x.length);
        return result;
    }

    private static PArray[] addUnderlyingArraysWithSameMinMaxFunc(Func f, PArray[] arrays) {
        assert f == Func.MIN || f == Func.MAX;
        List<Array> expanded = new ArrayList<>(arrays.length);
        for (Array a : arrays) {
            if (a instanceof FuncArray && ((FuncArray)a).f() == f) {
                Array[] underlyings = ((AbstractArray)a).underlyingArrays;
                boolean sameType = true;
                for (Array u : underlyings) {
                    assert u instanceof PArray;
                    if (u.elementType() != a.elementType()) {
                        sameType = false;
                    }
                }
                if (sameType) {
                    expanded.addAll(java.util.Arrays.asList(underlyings));
                    // recursion is needless here: due to this processing, there is no ways to create FuncArray
                    // where some underlying arrays are also FuncArrays with same element type and MIN/MAX function
                    continue;
                }
            }
            expanded.add(a);
        }
        assert !expanded.isEmpty() : "Empty list after extracting underlying functions";
        // MIN and MAX always have at least 1 argument: in a case of 0 arguments, asFuncArray method uses LinearFunc
        return expanded.toArray(new PArray[0]);
    }

    static void coordinatesInDoubles(long index, long[] dim, double[] result) {
        if (index < 0) {
            throw new AssertionError("Negative index argument");
        }
        if (result.length < dim.length) {
            throw new AssertionError("Too short result array: long[" + result.length
                + "]; " + dim.length + " elements required to store coordinates");
        }
        long a = index;
        for (int k = 0; k < dim.length - 1; k++) {
            long b = a / dim[k];
            result[k] = a - b * dim[k]; // here "*" is faster than "%"
            a = b;
        }
        if (a >= dim[dim.length - 1]) {
            throw new IndexOutOfBoundsException("Too large index argument: " + index
                + " >= matrix size " + JArrays.toString(dim, "*", 10000));
        }
        result[dim.length - 1] = a;
    }
}
