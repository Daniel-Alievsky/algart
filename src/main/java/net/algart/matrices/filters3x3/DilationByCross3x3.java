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

public abstract class DilationByCross3x3 extends AbstractQuickFilter3x3 {
    DilationByCross3x3(Class<?> elementType, long[] dimensions) {
        super(elementType, dimensions);
    }

    public static DilationByCross3x3 newInstance(Class<?> elementType, long dimX, long dimY) {
        return newInstance(elementType, new long[]{dimX, dimY});
    }

    public static DilationByCross3x3 newInstance(Class<?> elementType, long[] dimensions) {
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

    private static class ForChar extends DilationByCross3x3 {
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
            int vL = source[middleLineOffset + dimXm1];
            int vR = source[middleLineOffset + rem1ForDimX];
            int vU = source[firstLineOffset];
            int vD = source[lastLineOffset];
            int vC = source[middleLineOffset];
            result[resultLineOffset] = (char) max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset];
                vU = source[firstLineOffset++];
                vD = source[lastLineOffset++];
                int max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = (char) Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1];
                vU = source[firstLineOffset];
                vD = source[lastLineOffset];
                result[++resultLineOffset] = (char) max(vL, vR, vU, vD, vC);
            }
        }

        private static int max(int w0, int w1, int w2, int w3, int w4) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private static class ForByte extends DilationByCross3x3 {
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
            int vL = source[middleLineOffset + dimXm1] & 0xFF;
            int vR = source[middleLineOffset + rem1ForDimX] & 0xFF;
            int vU = source[firstLineOffset] & 0xFF;
            int vD = source[lastLineOffset] & 0xFF;
            int vC = source[middleLineOffset] & 0xFF;
            result[resultLineOffset] = (byte) max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset] & 0xFF;
                vU = source[firstLineOffset++] & 0xFF;
                vD = source[lastLineOffset++] & 0xFF;
                int max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = (byte) Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1] & 0xFF;
                vU = source[firstLineOffset] & 0xFF;
                vD = source[lastLineOffset] & 0xFF;
                result[++resultLineOffset] = (byte) max(vL, vR, vU, vD, vC);
            }
        }

        private static int max(int w0, int w1, int w2, int w3, int w4) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    private static class ForShort extends DilationByCross3x3 {
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
            int vL = source[middleLineOffset + dimXm1] & 0xFFFF;
            int vR = source[middleLineOffset + rem1ForDimX] & 0xFFFF;
            int vU = source[firstLineOffset] & 0xFFFF;
            int vD = source[lastLineOffset] & 0xFFFF;
            int vC = source[middleLineOffset] & 0xFFFF;
            result[resultLineOffset] = (short) max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset] & 0xFFFF;
                vU = source[firstLineOffset++] & 0xFFFF;
                vD = source[lastLineOffset++] & 0xFFFF;
                int max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = (short) Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1] & 0xFFFF;
                vU = source[firstLineOffset] & 0xFFFF;
                vD = source[lastLineOffset] & 0xFFFF;
                result[++resultLineOffset] = (short) max(vL, vR, vU, vD, vC);
            }
        }

        private static int max(int w0, int w1, int w2, int w3, int w4) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    private static class ForInt extends DilationByCross3x3 {
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
            int vL = source[middleLineOffset + dimXm1];
            int vR = source[middleLineOffset + rem1ForDimX];
            int vU = source[firstLineOffset];
            int vD = source[lastLineOffset];
            int vC = source[middleLineOffset];
            result[resultLineOffset] = max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset];
                vU = source[firstLineOffset++];
                vD = source[lastLineOffset++];
                int max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                int max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1];
                vU = source[firstLineOffset];
                vD = source[lastLineOffset];
                result[++resultLineOffset] = max(vL, vR, vU, vD, vC);
            }
        }

        private static int max(int w0, int w1, int w2, int w3, int w4) {
            int max1 = Math.max(w0, w1);
            int max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    private static class ForLong extends DilationByCross3x3 {
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
            long vL = source[middleLineOffset + dimXm1];
            long vR = source[middleLineOffset + rem1ForDimX];
            long vU = source[firstLineOffset];
            long vD = source[lastLineOffset];
            long vC = source[middleLineOffset];
            result[resultLineOffset] = max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset];
                vU = source[firstLineOffset++];
                vD = source[lastLineOffset++];
                long max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                long max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1];
                vU = source[firstLineOffset];
                vD = source[lastLineOffset];
                result[++resultLineOffset] = max(vL, vR, vU, vD, vC);
            }
        }

        private static long max(long w0, long w1, long w2, long w3, long w4) {
            long max1 = Math.max(w0, w1);
            long max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    private static class ForFloat extends DilationByCross3x3 {
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
            float vL = source[middleLineOffset + dimXm1];
            float vR = source[middleLineOffset + rem1ForDimX];
            float vU = source[firstLineOffset];
            float vD = source[lastLineOffset];
            float vC = source[middleLineOffset];
            result[resultLineOffset] = max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset];
                vU = source[firstLineOffset++];
                vD = source[lastLineOffset++];
                float max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                float max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1];
                vU = source[firstLineOffset];
                vD = source[lastLineOffset];
                result[++resultLineOffset] = max(vL, vR, vU, vD, vC);
            }
        }

        private static float max(float w0, float w1, float w2, float w3, float w4) {
            float max1 = Math.max(w0, w1);
            float max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    private static class ForDouble extends DilationByCross3x3 {
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
            double vL = source[middleLineOffset + dimXm1];
            double vR = source[middleLineOffset + rem1ForDimX];
            double vU = source[firstLineOffset];
            double vD = source[lastLineOffset];
            double vC = source[middleLineOffset];
            result[resultLineOffset] = max(vL, vR, vU, vD, vC);
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset];
                vU = source[firstLineOffset++];
                vD = source[lastLineOffset++];
                double max1 = Math.max(vL, vR);
                // - note: Math.min and Math.max are well optimized inside JVM, better than direct comparison
                // and even better than antiDozNoOverflow
                double max2 = Math.max(vU, vD);
                max1 = Math.max(max1, vC);
                result[++resultLineOffset] = Math.max(max1, max2);
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1];
                vU = source[firstLineOffset];
                vD = source[lastLineOffset];
                result[++resultLineOffset] = max(vL, vR, vU, vD, vC);
            }
        }

        private static double max(double w0, double w1, double w2, double w3, double w4) {
            double max1 = Math.max(w0, w1);
            double max2 = Math.max(w2, w3);
            max1 = Math.max(max1, w4);
            return Math.max(max1, max2);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private static class ForBit extends DilationByCross3x3 {
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
            boolean vL = source[middleLineOffset + dimXm1];
            boolean vR = source[middleLineOffset + rem1ForDimX];
            boolean vU = source[firstLineOffset];
            boolean vD = source[lastLineOffset];
            boolean vC = source[middleLineOffset];
            result[resultLineOffset] = vL || vR || vU || vD || vC;
            final int resultLineOffsetTo = resultLineOffset + dimX - 2;
            ++firstLineOffset;
            ++middleLineOffset;
            ++lastLineOffset;
            while (resultLineOffset < resultLineOffsetTo) {
                vL = vC;
                vC = vR;
                vR = source[++middleLineOffset];
                vU = source[firstLineOffset++];
                vD = source[lastLineOffset++];
                result[++resultLineOffset] = vL || vR || vU || vD || vC;
            }
            if (dimX >= 2) {
                vL = vC;
                vC = vR;
                vR = source[middleLineOffset - dimXm1];
                vU = source[firstLineOffset];
                vD = source[lastLineOffset];
                result[++resultLineOffset] = vL || vR || vU || vD || vC;
            }
        }
    }
}
