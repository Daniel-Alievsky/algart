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

package net.algart.matrices.filters3x3;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public abstract class MedianBySquare3x3 extends PercentileBySquare3x3 {
    private static final boolean USE_ANTI_DOZ_TABLE_FOR_BYTES = false;
    // - should be false for better performance
    static final int[] ANTI_DOZ_TABLE_FOR_BYTES = createAntiDozTable();

    MedianBySquare3x3(Class<?> elementType, long[] dimensions) {
        super(elementType, dimensions, 4);
    }

    public static MedianBySquare3x3 newInstance(Class<?> elementType, long dimX, long dimY) {
        return newInstance(elementType, new long[]{dimX, dimY});
    }

    public static MedianBySquare3x3 newInstance(Class<?> elementType, long[] dimensions) {
        if (elementType == char.class) {
            return new ForChar(dimensions);
        } else if (elementType == boolean.class) {
            return new ForBit(dimensions);
        } else if (elementType == byte.class) {
            return USE_ANTI_DOZ_TABLE_FOR_BYTES ? new ForByteTableBased(dimensions) : new ForByte(dimensions);
        } else if (elementType == short.class) {
            return new ForShort(dimensions);
        } else if (elementType == int.class) {
            return new ForInt(dimensions);
        } else if (elementType == long.class) {
            return new ForLong(dimensions);
        } else if (elementType == float.class) {
            return new ForFloat(dimensions);
        } else if (elementType == double.class) {
            return new ForDouble(dimensions);
        } else {
            throw new UnsupportedOperationException("Non-primitive element type " + elementType + " is not supported");
        }
    }

    public static Matrix<UpdatablePArray> apply(Matrix<? extends PArray> source) {
        Objects.requireNonNull(source, "Null source matrix");
        return newInstance(source.elementType(), source.dimensions()).filter(source);
    }

    // For char type we also can use optimization based on antiDozNoOverflow, but it is not important:
    // this type is almost never used

    /*Repeat() Char                 ==> Int,,Long,,Float,,Double;;
               char                 ==> int,,long,,float,,double;;
               (int)(\s+v\w|\s+w\w) ==> $1$2,,long$2,,float$2,,double$2;;
               (int)(\s+temp\b)     ==> $1$2,,long$2,,float$2,,double$2;;
               (int)(\s+median)     ==> $1$2,,long$2,,float$2,,double$2;;
               \((?:int|long|float|double)\)\s+ ==> ,,... */

    private static class ForChar extends MedianBySquare3x3 {
        private ForChar(long[] dimensions) {
            super(char.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final char[] result = (char[]) resultJavaArray;
            final char[] source = (char[]) sourceJavaArray;
            int v0 = source[firstLineOffset + dimXm1];
            int v1 = source[firstLineOffset];
            int v2 = source[firstLineOffset + rem1ForDimX];
            int v3 = source[middleLineOffset + dimXm1];
            int v4 = source[middleLineOffset];
            int v5 = source[middleLineOffset + rem1ForDimX];
            int v6 = source[lastLineOffset + dimXm1];
            int v7 = source[lastLineOffset];
            int v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = (char) median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset];
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset];
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset];
                int w0 = v0;
                int w1 = v1;
                int w2 = v2;
                int w3 = v3;
                int w4 = v4;
                int w5 = v5;
                int w6 = v6;
                int w7 = v7;
                int w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                int temp;
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w1) {
                    temp = w0; w0 = w1; w1 = temp;
                }
                if (w3 > w4) {
                    temp = w3; w3 = w4; w4 = temp;
                }
                if (w6 > w7) {
                    temp = w6; w6 = w7; w7 = temp;
                }
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w3) {
                    w3 = w0;
                }
                if (w5 > w8) {
                    w5 = w8;
                }
                if (w4 > w7) {
                    temp = w4; w4 = w7; w7 = temp;
                }
                if (w3 > w6) {
                    w6 = w3;
                }
                if (w1 > w4) {
                    w4 = w1;
                }
                if (w2 > w5) {
                    w2 = w5;
                }
                if (w4 > w7) {
                    w4 = w7;
                }
                if (w4 > w2) {
                    temp = w4; w4 = w2; w2 = temp;
                }
                if (w6 > w4) {
                    w4 = w6;
                }
                if (w4 > w2) {
                    w4 = w2;
                }
                result[++resultLineOffset] = (char) w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1];
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1];
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1];
                result[++resultLineOffset] = (char) median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static int median(
                int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
            int temp;
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w1) {
                temp = w0; w0 = w1; w1 = temp;
            }
            if (w3 > w4) {
                temp = w3; w3 = w4; w4 = temp;
            }
            if (w6 > w7) {
                temp = w6; w6 = w7; w7 = temp;
            }
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w3) {
                w3 = w0;
            }
            if (w5 > w8) {
                w5 = w8;
            }
            if (w4 > w7) {
                temp = w4; w4 = w7; w7 = temp;
            }
            if (w3 > w6) {
                w6 = w3;
            }
            if (w1 > w4) {
                w4 = w1;
            }
            if (w2 > w5) {
                w2 = w5;
            }
            if (w4 > w7) {
                w4 = w7;
            }
            if (w4 > w2) {
                temp = w4; w4 = w2; w2 = temp;
            }
            if (w6 > w4) {
                w4 = w6;
            }
            if (w4 > w2) {
                w4 = w2;
            }
            return w4;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private static class ForInt extends MedianBySquare3x3 {
        private ForInt(long[] dimensions) {
            super(int.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final int[] result = (int[]) resultJavaArray;
            final int[] source = (int[]) sourceJavaArray;
            int v0 = source[firstLineOffset + dimXm1];
            int v1 = source[firstLineOffset];
            int v2 = source[firstLineOffset + rem1ForDimX];
            int v3 = source[middleLineOffset + dimXm1];
            int v4 = source[middleLineOffset];
            int v5 = source[middleLineOffset + rem1ForDimX];
            int v6 = source[lastLineOffset + dimXm1];
            int v7 = source[lastLineOffset];
            int v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset];
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset];
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset];
                int w0 = v0;
                int w1 = v1;
                int w2 = v2;
                int w3 = v3;
                int w4 = v4;
                int w5 = v5;
                int w6 = v6;
                int w7 = v7;
                int w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                int temp;
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w1) {
                    temp = w0; w0 = w1; w1 = temp;
                }
                if (w3 > w4) {
                    temp = w3; w3 = w4; w4 = temp;
                }
                if (w6 > w7) {
                    temp = w6; w6 = w7; w7 = temp;
                }
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w3) {
                    w3 = w0;
                }
                if (w5 > w8) {
                    w5 = w8;
                }
                if (w4 > w7) {
                    temp = w4; w4 = w7; w7 = temp;
                }
                if (w3 > w6) {
                    w6 = w3;
                }
                if (w1 > w4) {
                    w4 = w1;
                }
                if (w2 > w5) {
                    w2 = w5;
                }
                if (w4 > w7) {
                    w4 = w7;
                }
                if (w4 > w2) {
                    temp = w4; w4 = w2; w2 = temp;
                }
                if (w6 > w4) {
                    w4 = w6;
                }
                if (w4 > w2) {
                    w4 = w2;
                }
                result[++resultLineOffset] = w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1];
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1];
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1];
                result[++resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static int median(
                int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
            int temp;
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w1) {
                temp = w0; w0 = w1; w1 = temp;
            }
            if (w3 > w4) {
                temp = w3; w3 = w4; w4 = temp;
            }
            if (w6 > w7) {
                temp = w6; w6 = w7; w7 = temp;
            }
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w3) {
                w3 = w0;
            }
            if (w5 > w8) {
                w5 = w8;
            }
            if (w4 > w7) {
                temp = w4; w4 = w7; w7 = temp;
            }
            if (w3 > w6) {
                w6 = w3;
            }
            if (w1 > w4) {
                w4 = w1;
            }
            if (w2 > w5) {
                w2 = w5;
            }
            if (w4 > w7) {
                w4 = w7;
            }
            if (w4 > w2) {
                temp = w4; w4 = w2; w2 = temp;
            }
            if (w6 > w4) {
                w4 = w6;
            }
            if (w4 > w2) {
                w4 = w2;
            }
            return w4;
        }
    }

    private static class ForLong extends MedianBySquare3x3 {
        private ForLong(long[] dimensions) {
            super(long.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final long[] result = (long[]) resultJavaArray;
            final long[] source = (long[]) sourceJavaArray;
            long v0 = source[firstLineOffset + dimXm1];
            long v1 = source[firstLineOffset];
            long v2 = source[firstLineOffset + rem1ForDimX];
            long v3 = source[middleLineOffset + dimXm1];
            long v4 = source[middleLineOffset];
            long v5 = source[middleLineOffset + rem1ForDimX];
            long v6 = source[lastLineOffset + dimXm1];
            long v7 = source[lastLineOffset];
            long v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset];
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset];
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset];
                long w0 = v0;
                long w1 = v1;
                long w2 = v2;
                long w3 = v3;
                long w4 = v4;
                long w5 = v5;
                long w6 = v6;
                long w7 = v7;
                long w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                long temp;
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w1) {
                    temp = w0; w0 = w1; w1 = temp;
                }
                if (w3 > w4) {
                    temp = w3; w3 = w4; w4 = temp;
                }
                if (w6 > w7) {
                    temp = w6; w6 = w7; w7 = temp;
                }
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w3) {
                    w3 = w0;
                }
                if (w5 > w8) {
                    w5 = w8;
                }
                if (w4 > w7) {
                    temp = w4; w4 = w7; w7 = temp;
                }
                if (w3 > w6) {
                    w6 = w3;
                }
                if (w1 > w4) {
                    w4 = w1;
                }
                if (w2 > w5) {
                    w2 = w5;
                }
                if (w4 > w7) {
                    w4 = w7;
                }
                if (w4 > w2) {
                    temp = w4; w4 = w2; w2 = temp;
                }
                if (w6 > w4) {
                    w4 = w6;
                }
                if (w4 > w2) {
                    w4 = w2;
                }
                result[++resultLineOffset] = w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1];
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1];
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1];
                result[++resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static long median(
                long w0, long w1, long w2, long w3, long w4, long w5, long w6, long w7, long w8) {
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
            long temp;
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w1) {
                temp = w0; w0 = w1; w1 = temp;
            }
            if (w3 > w4) {
                temp = w3; w3 = w4; w4 = temp;
            }
            if (w6 > w7) {
                temp = w6; w6 = w7; w7 = temp;
            }
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w3) {
                w3 = w0;
            }
            if (w5 > w8) {
                w5 = w8;
            }
            if (w4 > w7) {
                temp = w4; w4 = w7; w7 = temp;
            }
            if (w3 > w6) {
                w6 = w3;
            }
            if (w1 > w4) {
                w4 = w1;
            }
            if (w2 > w5) {
                w2 = w5;
            }
            if (w4 > w7) {
                w4 = w7;
            }
            if (w4 > w2) {
                temp = w4; w4 = w2; w2 = temp;
            }
            if (w6 > w4) {
                w4 = w6;
            }
            if (w4 > w2) {
                w4 = w2;
            }
            return w4;
        }
    }

    private static class ForFloat extends MedianBySquare3x3 {
        private ForFloat(long[] dimensions) {
            super(float.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final float[] result = (float[]) resultJavaArray;
            final float[] source = (float[]) sourceJavaArray;
            float v0 = source[firstLineOffset + dimXm1];
            float v1 = source[firstLineOffset];
            float v2 = source[firstLineOffset + rem1ForDimX];
            float v3 = source[middleLineOffset + dimXm1];
            float v4 = source[middleLineOffset];
            float v5 = source[middleLineOffset + rem1ForDimX];
            float v6 = source[lastLineOffset + dimXm1];
            float v7 = source[lastLineOffset];
            float v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset];
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset];
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset];
                float w0 = v0;
                float w1 = v1;
                float w2 = v2;
                float w3 = v3;
                float w4 = v4;
                float w5 = v5;
                float w6 = v6;
                float w7 = v7;
                float w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                float temp;
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w1) {
                    temp = w0; w0 = w1; w1 = temp;
                }
                if (w3 > w4) {
                    temp = w3; w3 = w4; w4 = temp;
                }
                if (w6 > w7) {
                    temp = w6; w6 = w7; w7 = temp;
                }
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w3) {
                    w3 = w0;
                }
                if (w5 > w8) {
                    w5 = w8;
                }
                if (w4 > w7) {
                    temp = w4; w4 = w7; w7 = temp;
                }
                if (w3 > w6) {
                    w6 = w3;
                }
                if (w1 > w4) {
                    w4 = w1;
                }
                if (w2 > w5) {
                    w2 = w5;
                }
                if (w4 > w7) {
                    w4 = w7;
                }
                if (w4 > w2) {
                    temp = w4; w4 = w2; w2 = temp;
                }
                if (w6 > w4) {
                    w4 = w6;
                }
                if (w4 > w2) {
                    w4 = w2;
                }
                result[++resultLineOffset] = w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1];
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1];
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1];
                result[++resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static float median(
                float w0, float w1, float w2, float w3, float w4, float w5, float w6, float w7, float w8) {
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
            float temp;
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w1) {
                temp = w0; w0 = w1; w1 = temp;
            }
            if (w3 > w4) {
                temp = w3; w3 = w4; w4 = temp;
            }
            if (w6 > w7) {
                temp = w6; w6 = w7; w7 = temp;
            }
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w3) {
                w3 = w0;
            }
            if (w5 > w8) {
                w5 = w8;
            }
            if (w4 > w7) {
                temp = w4; w4 = w7; w7 = temp;
            }
            if (w3 > w6) {
                w6 = w3;
            }
            if (w1 > w4) {
                w4 = w1;
            }
            if (w2 > w5) {
                w2 = w5;
            }
            if (w4 > w7) {
                w4 = w7;
            }
            if (w4 > w2) {
                temp = w4; w4 = w2; w2 = temp;
            }
            if (w6 > w4) {
                w4 = w6;
            }
            if (w4 > w2) {
                w4 = w2;
            }
            return w4;
        }
    }

    private static class ForDouble extends MedianBySquare3x3 {
        private ForDouble(long[] dimensions) {
            super(double.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final double[] result = (double[]) resultJavaArray;
            final double[] source = (double[]) sourceJavaArray;
            double v0 = source[firstLineOffset + dimXm1];
            double v1 = source[firstLineOffset];
            double v2 = source[firstLineOffset + rem1ForDimX];
            double v3 = source[middleLineOffset + dimXm1];
            double v4 = source[middleLineOffset];
            double v5 = source[middleLineOffset + rem1ForDimX];
            double v6 = source[lastLineOffset + dimXm1];
            double v7 = source[lastLineOffset];
            double v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset];
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset];
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset];
                double w0 = v0;
                double w1 = v1;
                double w2 = v2;
                double w3 = v3;
                double w4 = v4;
                double w5 = v5;
                double w6 = v6;
                double w7 = v7;
                double w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                double temp;
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w1) {
                    temp = w0; w0 = w1; w1 = temp;
                }
                if (w3 > w4) {
                    temp = w3; w3 = w4; w4 = temp;
                }
                if (w6 > w7) {
                    temp = w6; w6 = w7; w7 = temp;
                }
                if (w1 > w2) {
                    temp = w1; w1 = w2; w2 = temp;
                }
                if (w4 > w5) {
                    temp = w4; w4 = w5; w5 = temp;
                }
                if (w7 > w8) {
                    temp = w7; w7 = w8; w8 = temp;
                }
                if (w0 > w3) {
                    w3 = w0;
                }
                if (w5 > w8) {
                    w5 = w8;
                }
                if (w4 > w7) {
                    temp = w4; w4 = w7; w7 = temp;
                }
                if (w3 > w6) {
                    w6 = w3;
                }
                if (w1 > w4) {
                    w4 = w1;
                }
                if (w2 > w5) {
                    w2 = w5;
                }
                if (w4 > w7) {
                    w4 = w7;
                }
                if (w4 > w2) {
                    temp = w4; w4 = w2; w2 = temp;
                }
                if (w6 > w4) {
                    w4 = w6;
                }
                if (w4 > w2) {
                    w4 = w2;
                }
                result[++resultLineOffset] = w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1];
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1];
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1];
                result[++resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static double median(
                double w0, double w1, double w2, double w3, double w4, double w5, double w6, double w7, double w8) {
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
            double temp;
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w1) {
                temp = w0; w0 = w1; w1 = temp;
            }
            if (w3 > w4) {
                temp = w3; w3 = w4; w4 = temp;
            }
            if (w6 > w7) {
                temp = w6; w6 = w7; w7 = temp;
            }
            if (w1 > w2) {
                temp = w1; w1 = w2; w2 = temp;
            }
            if (w4 > w5) {
                temp = w4; w4 = w5; w5 = temp;
            }
            if (w7 > w8) {
                temp = w7; w7 = w8; w8 = temp;
            }
            if (w0 > w3) {
                w3 = w0;
            }
            if (w5 > w8) {
                w5 = w8;
            }
            if (w4 > w7) {
                temp = w4; w4 = w7; w7 = temp;
            }
            if (w3 > w6) {
                w6 = w3;
            }
            if (w1 > w4) {
                w4 = w1;
            }
            if (w2 > w5) {
                w2 = w5;
            }
            if (w4 > w7) {
                w4 = w7;
            }
            if (w4 > w2) {
                temp = w4; w4 = w2; w2 = temp;
            }
            if (w6 > w4) {
                w4 = w6;
            }
            if (w4 > w2) {
                w4 = w2;
            }
            return w4;
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() Short                ==> Byte;;
               short                ==> byte;;
               0xFFFF               ==> 0xFF
     */
    private static class ForShort extends MedianBySquare3x3 {
        private ForShort(long[] dimensions) {
            super(short.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final short[] result = (short[]) resultJavaArray;
            final short[] source = (short[]) sourceJavaArray;
            int v0 = source[firstLineOffset + dimXm1] & 0xFFFF;
            int v1 = source[firstLineOffset] & 0xFFFF;
            int v2 = source[firstLineOffset + rem1ForDimX] & 0xFFFF;
            int v3 = source[middleLineOffset + dimXm1] & 0xFFFF;
            int v4 = source[middleLineOffset] & 0xFFFF;
            int v5 = source[middleLineOffset + rem1ForDimX] & 0xFFFF;
            int v6 = source[lastLineOffset + dimXm1] & 0xFFFF;
            int v7 = source[lastLineOffset] & 0xFFFF;
            int v8 = source[lastLineOffset + rem1ForDimX] & 0xFFFF;
            result[resultLineOffset] = (short) medianNoOverflow(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset] & 0xFFFF;
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset] & 0xFFFF;
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset] & 0xFFFF;
                int w0 = v0;
                int w1 = v1;
                int w2 = v2;
                int w3 = v3;
                int w4 = v4;
                int w5 = v5;
                int w6 = v6;
                int w7 = v7;
                int w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                int temp;
                temp = antiDozNoOverflow(w1, w2); w1 += temp; w2 -= temp;
                temp = antiDozNoOverflow(w4, w5); w4 += temp; w5 -= temp;
                temp = antiDozNoOverflow(w7, w8); w7 += temp; w8 -= temp;
                temp = antiDozNoOverflow(w0, w1); w0 += temp; w1 -= temp;
                temp = antiDozNoOverflow(w3, w4); w3 += temp; w4 -= temp;
                temp = antiDozNoOverflow(w6, w7); w6 += temp; w7 -= temp;
                temp = antiDozNoOverflow(w1, w2); w1 += temp; w2 -= temp;
                temp = antiDozNoOverflow(w4, w5); w4 += temp; w5 -= temp;
                temp = antiDozNoOverflow(w7, w8); w7 += temp; w8 -= temp;
                w3 = Math.max(w0, w3);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                w5 = Math.min(w5, w8);
                temp = antiDozNoOverflow(w4, w7); w4 += temp; w7 -= temp;
                w6 = Math.max(w3, w6);
                w4 = Math.max(w1, w4);
                w2 = Math.min(w2, w5);
                w4 = Math.min(w4, w7);
                temp = antiDozNoOverflow(w4, w2); w4 += temp; w2 -= temp;
                w4 = Math.max(w6, w4);
                w4 = Math.min(w4, w2);
                result[++resultLineOffset] = (short) w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1] & 0xFFFF;
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1] & 0xFFFF;
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1] & 0xFFFF;
                result[++resultLineOffset] = (short) medianNoOverflow(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static class ForByte extends MedianBySquare3x3 {
        private ForByte(long[] dimensions) {
            super(byte.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final byte[] result = (byte[]) resultJavaArray;
            final byte[] source = (byte[]) sourceJavaArray;
            int v0 = source[firstLineOffset + dimXm1] & 0xFF;
            int v1 = source[firstLineOffset] & 0xFF;
            int v2 = source[firstLineOffset + rem1ForDimX] & 0xFF;
            int v3 = source[middleLineOffset + dimXm1] & 0xFF;
            int v4 = source[middleLineOffset] & 0xFF;
            int v5 = source[middleLineOffset + rem1ForDimX] & 0xFF;
            int v6 = source[lastLineOffset + dimXm1] & 0xFF;
            int v7 = source[lastLineOffset] & 0xFF;
            int v8 = source[lastLineOffset + rem1ForDimX] & 0xFF;
            result[resultLineOffset] = (byte) medianNoOverflow(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset] & 0xFF;
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset] & 0xFF;
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset] & 0xFF;
                int w0 = v0;
                int w1 = v1;
                int w2 = v2;
                int w3 = v3;
                int w4 = v4;
                int w5 = v5;
                int w6 = v6;
                int w7 = v7;
                int w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                int temp;
                temp = antiDozNoOverflow(w1, w2); w1 += temp; w2 -= temp;
                temp = antiDozNoOverflow(w4, w5); w4 += temp; w5 -= temp;
                temp = antiDozNoOverflow(w7, w8); w7 += temp; w8 -= temp;
                temp = antiDozNoOverflow(w0, w1); w0 += temp; w1 -= temp;
                temp = antiDozNoOverflow(w3, w4); w3 += temp; w4 -= temp;
                temp = antiDozNoOverflow(w6, w7); w6 += temp; w7 -= temp;
                temp = antiDozNoOverflow(w1, w2); w1 += temp; w2 -= temp;
                temp = antiDozNoOverflow(w4, w5); w4 += temp; w5 -= temp;
                temp = antiDozNoOverflow(w7, w8); w7 += temp; w8 -= temp;
                w3 = Math.max(w0, w3);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                w5 = Math.min(w5, w8);
                temp = antiDozNoOverflow(w4, w7); w4 += temp; w7 -= temp;
                w6 = Math.max(w3, w6);
                w4 = Math.max(w1, w4);
                w2 = Math.min(w2, w5);
                w4 = Math.min(w4, w7);
                temp = antiDozNoOverflow(w4, w2); w4 += temp; w2 -= temp;
                w4 = Math.max(w6, w4);
                w4 = Math.min(w4, w2);
                result[++resultLineOffset] = (byte) w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1] & 0xFF;
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1] & 0xFF;
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1] & 0xFF;
                result[++resultLineOffset] = (byte) medianNoOverflow(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private static class ForByteTableBased extends MedianBySquare3x3 {
        private ForByteTableBased(long[] dimensions) {
            super(byte.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final byte[] result = (byte[]) resultJavaArray;
            final byte[] source = (byte[]) sourceJavaArray;
            int v0 = source[firstLineOffset + dimXm1] & 0xFF;
            int v1 = source[firstLineOffset] & 0xFF;
            int v2 = source[firstLineOffset + rem1ForDimX] & 0xFF;
            int v3 = source[middleLineOffset + dimXm1] & 0xFF;
            int v4 = source[middleLineOffset] & 0xFF;
            int v5 = source[middleLineOffset + rem1ForDimX] & 0xFF;
            int v6 = source[lastLineOffset + dimXm1] & 0xFF;
            int v7 = source[lastLineOffset] & 0xFF;
            int v8 = source[lastLineOffset + rem1ForDimX] & 0xFF;
            result[resultLineOffset] = (byte) medianNoOverflow(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset] & 0xFF;
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset] & 0xFF;
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset] & 0xFF;
                int w0 = v0;
                int w1 = v1;
                int w2 = v2;
                int w3 = v3;
                int w4 = v4;
                int w5 = v5;
                int w6 = v6;
                int w7 = v7;
                int w8 = v8;
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
                int temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w2 - w1]; w1 += temp; w2 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w5 - w4]; w4 += temp; w5 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w8 - w7]; w7 += temp; w8 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w1 - w0]; w0 += temp; w1 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w4 - w3]; w3 += temp; w4 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w7 - w6]; w6 += temp; w7 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w2 - w1]; w1 += temp; w2 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w5 - w4]; w4 += temp; w5 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w8 - w7]; w7 += temp; w8 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w3 - w0]; w3 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w8 - w5]; w5 += temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w7 - w4]; w4 += temp; w7 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w6 - w3]; w6 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w4 - w1]; w4 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w5 - w2]; w2 += temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w7 - w4]; w4 += temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w2 - w4]; w4 += temp; w2 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w4 - w6]; w4 -= temp;
                temp = ANTI_DOZ_TABLE_FOR_BYTES[256 + w2 - w4]; w4 += temp;
                result[++resultLineOffset] = (byte) w4;
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1] & 0xFF;
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1] & 0xFF;
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1] & 0xFF;
                result[++resultLineOffset] = (byte) medianNoOverflow(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }
    }

    private static class ForBit extends MedianBySquare3x3 {
        private ForBit(long[] dimensions) {
            super(boolean.class, dimensions);
        }

        @Override
        protected void process3Lines(
                Object resultJavaArray,
                int resultLineOffset,
                Object sourceJavaArray,
                int firstLineOffset,
                int middleLineOffset,
                int lastLineOffset,
                int multithreadingRangeIndex) {
            final boolean[] result = (boolean[]) resultJavaArray;
            final boolean[] source = (boolean[]) sourceJavaArray;
            boolean v0 = source[firstLineOffset + dimXm1];
            boolean v1 = source[firstLineOffset];
            boolean v2 = source[firstLineOffset + rem1ForDimX];
            boolean v3 = source[middleLineOffset + dimXm1];
            boolean v4 = source[middleLineOffset];
            boolean v5 = source[middleLineOffset + rem1ForDimX];
            boolean v6 = source[lastLineOffset + dimXm1];
            boolean v7 = source[lastLineOffset];
            boolean v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            // - RIGHT boundary of square, not middle!
            while (resultLineOffset < resultLineOffsetTo) {
                v0 = v1;
                v1 = v2;
                v2 = source[++firstLineOffset];
                v3 = v4;
                v4 = v5;
                v5 = source[++middleLineOffset];
                v6 = v7;
                v7 = v8;
                v8 = source[++lastLineOffset];
                result[++resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
            if (dimX >= 2) {
                v0 = v1;
                v1 = v2;
                v2 = source[firstLineOffset - dimXm1];
                v3 = v4;
                v4 = v5;
                v5 = source[middleLineOffset - dimXm1];
                v6 = v7;
                v7 = v8;
                v8 = source[lastLineOffset - dimXm1];
                result[++resultLineOffset] = median(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private boolean median(
                boolean v0,
                boolean v1,
                boolean v2,
                boolean v3,
                boolean v4,
                boolean v5,
                boolean v6,
                boolean v7,
                boolean v8) {
            return PercentileBySquare3x3.ForBit.percentile(4, v0, v1, v2, v3, v4, v5, v6, v7, v8);
        }
    }

    static int medianNoOverflow(int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
//            op(p1, p2); op(p4, p5); op(p7, p8); op(p0, p1);
//            op(p3, p4); op(p6, p7); op(p1, p2); op(p4, p5);
//            op(p7, p8); op(p0, p3); op(p5, p8); op(p4, p7);
//            op(p3, p6); op(p1, p4); op(p2, p5); op(p4, p7);
//            op(p4, p2); op(p6, p4); op(p4, p2);
        int temp;
        temp = antiDozNoOverflow(w1, w2); w1 += temp; w2 -= temp;
        temp = antiDozNoOverflow(w4, w5); w4 += temp; w5 -= temp;
        temp = antiDozNoOverflow(w7, w8); w7 += temp; w8 -= temp;
        temp = antiDozNoOverflow(w0, w1); w0 += temp; w1 -= temp;
        temp = antiDozNoOverflow(w3, w4); w3 += temp; w4 -= temp;
        temp = antiDozNoOverflow(w6, w7); w6 += temp; w7 -= temp;
        temp = antiDozNoOverflow(w1, w2); w1 += temp; w2 -= temp;
        temp = antiDozNoOverflow(w4, w5); w4 += temp; w5 -= temp;
        temp = antiDozNoOverflow(w7, w8); w7 += temp; w8 -= temp;
        temp = antiDozNoOverflow(w0, w3); w3 -= temp;
        temp = antiDozNoOverflow(w5, w8); w5 += temp;
        temp = antiDozNoOverflow(w4, w7); w4 += temp; w7 -= temp;
        temp = antiDozNoOverflow(w3, w6); w6 -= temp;
        temp = antiDozNoOverflow(w1, w4); w4 -= temp;
        temp = antiDozNoOverflow(w2, w5); w2 += temp;
        temp = antiDozNoOverflow(w4, w7); w4 += temp;
        temp = antiDozNoOverflow(w4, w2); w4 += temp; w2 -= temp;
        temp = antiDozNoOverflow(w6, w4); w4 -= temp;
        temp = antiDozNoOverflow(w4, w2); w4 += temp;
        return w4;
    }

    static void medianNoOverflow(int[] w) {
        int temp;
        temp = antiDozNoOverflow(w[1], w[2]); w[1] += temp; w[2] -= temp;
        temp = antiDozNoOverflow(w[4], w[5]); w[4] += temp; w[5] -= temp;
        temp = antiDozNoOverflow(w[7], w[8]); w[7] += temp; w[8] -= temp;
        temp = antiDozNoOverflow(w[0], w[1]); w[0] += temp; w[1] -= temp;
        temp = antiDozNoOverflow(w[3], w[4]); w[3] += temp; w[4] -= temp;
        temp = antiDozNoOverflow(w[6], w[7]); w[6] += temp; w[7] -= temp;
        temp = antiDozNoOverflow(w[1], w[2]); w[1] += temp; w[2] -= temp;
        temp = antiDozNoOverflow(w[4], w[5]); w[4] += temp; w[5] -= temp;
        temp = antiDozNoOverflow(w[7], w[8]); w[7] += temp; w[8] -= temp;
        temp = antiDozNoOverflow(w[0], w[3]); w[0] += temp; w[3] -= temp;
        temp = antiDozNoOverflow(w[5], w[8]); w[5] += temp; w[8] -= temp;
        temp = antiDozNoOverflow(w[4], w[7]); w[4] += temp; w[7] -= temp;
        temp = antiDozNoOverflow(w[3], w[6]); w[3] += temp; w[6] -= temp;
        temp = antiDozNoOverflow(w[1], w[4]); w[1] += temp; w[4] -= temp;
        temp = antiDozNoOverflow(w[2], w[5]); w[2] += temp; w[5] -= temp;
        temp = antiDozNoOverflow(w[4], w[7]); w[4] += temp; w[7] -= temp;
        temp = antiDozNoOverflow(w[4], w[2]); w[4] += temp; w[2] -= temp;
        temp = antiDozNoOverflow(w[6], w[4]); w[6] += temp; w[4] -= temp;
        temp = antiDozNoOverflow(w[4], w[2]); w[4] += temp; w[2] -= temp;
    }

    // This function works correctly only if a - b is calculated without overflow,
    // for example, if both arguments are positive.
    static int dozNoOverflow(int a, int b) {
        int c = a - b;
        return c & ~(c >> 31);
    }

    static int antiDozNoOverflow(int a, int b) {
        int c = b - a;
        return c & (c >> 31);
    }

    static int antiDozForBytes(int a, int b) {
        return ANTI_DOZ_TABLE_FOR_BYTES[256 + b - a];
    }

    static int[] createAntiDozTable() {
        int[] table = new int[512];
        for (int k = -256; k <= 255; k++) {
            table[256 + k] = Math.min(k, 0);
        }
        return table;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            for (int j = 0; j < 10000; j++) {
                int a = i;
                int b = j;
                if (dozNoOverflow(a, b) != Math.max(a - b, 0)) {
                    throw new AssertionError("Invalid doz");
                }
                final int antiDoz = antiDozNoOverflow(a, b);
                if (antiDoz != Math.min(b - a, 0)) {
                    throw new AssertionError("Invalid anti-doz");
                }
                if (i < 256 && j < 256 && antiDoz != antiDozForBytes(a, b)) {
                    throw new AssertionError("Invalid anti-doz table");
                }
                final int max = b - antiDoz;
                final int min = a + antiDoz;
                if (min != Math.min(a, b) || max != Math.max(a, b)) {
                    throw new AssertionError("Invalid min/max");
                }
                a += antiDoz;
                b -= antiDoz;
                if (a != min || b != max) {
                    throw new AssertionError("Invalid min/max");
                }
            }
        }

        final int max = 100;
        final Random rnd = new Random(22);
        int[] v = new int[9];
        for (int test = 1, testCount = 16 * 1024 * 1024; test <= testCount; test++) {
            if ((test & 0xFF) == 0) {
                System.out.print("\r" + test);
            }
            Arrays.setAll(v, k -> rnd.nextInt(max));
            int[] vSorted = v.clone();
            Arrays.sort(vSorted);
            final int medianSimple = vSorted[4];
            final int median = ForInt.median(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
            if (median != medianSimple) {
                throw new AssertionError(test + ": Invalid median: "
                        + median + " instead of " + medianSimple + " for " + Arrays.toString(v));
            }
            final int medianNoOverflow = medianNoOverflow(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
            if (medianNoOverflow != medianSimple) {
                throw new AssertionError(test + ": Invalid median for byte: "
                        + medianNoOverflow + " instead of " + medianSimple + " for " + Arrays.toString(v));
            }
            medianNoOverflow(v);
            if (v[4] != medianSimple) {
                throw new AssertionError(test + ": Invalid median for byte (array): "
                        + medianNoOverflow + " instead of " + medianSimple + " for " + Arrays.toString(v));
            }
//            The following is not true:
//            if (!Arrays.equals(vSorted, v)) {
//                throw new AssertionError(test + ": Invalid sorting for byte: "+ Arrays.toString(v));
//            }
        }
    }
}
