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

public abstract class DilationBySquare3x3 extends PercentileBySquare3x3 {
    DilationBySquare3x3(Class<?> elementType, long[] dimensions) {
        super(elementType, dimensions, 8);
    }

    public static DilationBySquare3x3 newInstance(Class<?> elementType, long dimX, long dimY) {
        return newInstance(elementType, new long[]{dimX, dimY});
    }

    public static DilationBySquare3x3 newInstance(Class<?> elementType, long[] dimensions) {
        if (elementType == char.class) {
            return new ForChar(dimensions);
        } else if (elementType == boolean.class) {
            return new ForBit(dimensions);
        } else if (elementType == byte.class) {
            return new ForByte(dimensions);
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


    /*Repeat() Char                 ==> Byte,,Short,,Int,,Long,,Float,,Double;;
               char                 ==> byte,,short,,int,,long,,float,,double;;
               (v\w\s*=\s*)((?:source)\[[^\]]+\])       ==> $1$2 & 0xFF,,
                                                            $1$2 & 0xFFFF,,
                                                            $1$2,,...;;
               (int)(\s+v\w|\s+w\w) ==> $1$2,,$1$2,,$1$2,,long$2,,float$2,,double$2;;
               (int)(\s+max)        ==> $1$2,,$1$2,,$1$2,,long$2,,float$2,,double$2;;
               \((?:int|long|float|double)\)\s+ ==> ,,... */

    private static class ForChar extends DilationBySquare3x3 {
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
            result[resultLineOffset] = (char) max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                int max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(v2, v3);
                int max3 = Math.max(v4, v5);
                int max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = (char) Math.max(max1, max3);
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
                result[++resultLineOffset] = (char) max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static int max(
                int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            int max3 = Math.max(w4, w5);
            int max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private static class ForByte extends DilationBySquare3x3 {
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
            result[resultLineOffset] = (byte) max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                int max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(v2, v3);
                int max3 = Math.max(v4, v5);
                int max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = (byte) Math.max(max1, max3);
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
                result[++resultLineOffset] = (byte) max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static int max(
                int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            int max3 = Math.max(w4, w5);
            int max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    private static class ForShort extends DilationBySquare3x3 {
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
            result[resultLineOffset] = (short) max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                int max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(v2, v3);
                int max3 = Math.max(v4, v5);
                int max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = (short) Math.max(max1, max3);
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
                result[++resultLineOffset] = (short) max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static int max(
                int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            int max3 = Math.max(w4, w5);
            int max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    private static class ForInt extends DilationBySquare3x3 {
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
            result[resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                int max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(v2, v3);
                int max3 = Math.max(v4, v5);
                int max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = Math.max(max1, max3);
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
                result[++resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static int max(
                int w0, int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            int max3 = Math.max(w4, w5);
            int max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    private static class ForLong extends DilationBySquare3x3 {
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
            result[resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                long max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                long max2 = Math.max(v2, v3);
                long max3 = Math.max(v4, v5);
                long max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = Math.max(max1, max3);
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
                result[++resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static long max(
                long w0, long w1, long w2, long w3, long w4, long w5, long w6, long w7, long w8) {
            long max1 = Math.max(w0, w1);
            long max2 = Math.max(w2, w3);
            long max3 = Math.max(w4, w5);
            long max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    private static class ForFloat extends DilationBySquare3x3 {
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
            result[resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                float max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                float max2 = Math.max(v2, v3);
                float max3 = Math.max(v4, v5);
                float max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = Math.max(max1, max3);
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
                result[++resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static float max(
                float w0, float w1, float w2, float w3, float w4, float w5, float w6, float w7, float w8) {
            float max1 = Math.max(w0, w1);
            float max2 = Math.max(w2, w3);
            float max3 = Math.max(w4, w5);
            float max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    private static class ForDouble extends DilationBySquare3x3 {
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
            result[resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                double max1 = Math.max(v0, v1);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                double max2 = Math.max(v2, v3);
                double max3 = Math.max(v4, v5);
                double max4 = Math.max(v6, v7);
                max3 = Math.max(max3, v8);
                max1 = Math.max(max1, max2);
                max3 = Math.max(max3, max4);
                result[++resultLineOffset] = Math.max(max1, max3);
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
                result[++resultLineOffset] = max(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private static double max(
                double w0, double w1, double w2, double w3, double w4, double w5, double w6, double w7, double w8) {
            double max1 = Math.max(w0, w1);
            double max2 = Math.max(w2, w3);
            double max3 = Math.max(w4, w5);
            double max4 = Math.max(w6, w7);
            max3 = Math.max(max3, w8);
            max1 = Math.max(max1, max2);
            max3 = Math.max(max3, max4);
            return Math.max(max1, max3);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private static class ForBit extends DilationBySquare3x3 {
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
            result[resultLineOffset] = v0 || v1 || v2 || v3 || v4 || v5 || v6 || v7 || v8;
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
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
                result[++resultLineOffset] = v0 || v1 || v2 || v3 || v4 || v5 || v6 || v7 || v8;
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
                result[++resultLineOffset] = v0 || v1 || v2 || v3 || v4 || v5 || v6 || v7 || v8;
            }
        }
    }
}
