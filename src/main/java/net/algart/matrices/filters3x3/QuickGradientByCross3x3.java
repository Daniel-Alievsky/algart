/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

public abstract class QuickGradientByCross3x3 extends AbstractQuickFilter3x3 {
    private QuickGradientByCross3x3(Class<?> elementType, long[] dimensions) {
        super(elementType, dimensions);
    }

    public static QuickGradientByCross3x3 newInstance(Class<?> elementType, long dimX, long dimY) {
        return newInstance(elementType, new long[]{dimX, dimY});
    }

    public static QuickGradientByCross3x3 newInstance(Class<?> elementType, long[] dimensions) {
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


    /*Repeat() Char                 ==> Bit,,Byte,,Short,,Int,,Long,,Float,,Double;;
               char                 ==> boolean,,byte,,short,,int,,long,,float,,double;;
               (=\s*|-\s*)((?:source)\[[^\]]+\]) ==> $1($2 ? 1 : 0),,
                                                     $1($2 & 0xFF),,
                                                     $1($2 & 0xFFFF),,
                                                     $1$2,,...;;
               \(boolean\)\s+(\w+); ==> $1 != 0;,,...;;
               (int\s+)(v|r\b)      ==> $1$2,,$1$2,,$1$2,,long $2,,double $2,,...;;
               (>>\s1;)             ==> $1,,$1,,$1,,$1,,* 0.5;,,...;;
               \(double\)\s+        ==> ,,...
     */
    private static class ForChar extends QuickGradientByCross3x3 {

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
            int r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
            result[resultLineOffset] = (char) r;
            for (int k = 1; k < dimXm1; k++) {
                vL = source[middleLineOffset + k - 1];
                vR = source[middleLineOffset + k + 1];
                vU = source[firstLineOffset + k];
                vD = source[lastLineOffset + k];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + k] = (char) r;
            }
            if (dimX >= 2) {
                vL = source[middleLineOffset + dimXm1 - 1];
                vR = source[middleLineOffset];
                vU = source[firstLineOffset + dimXm1];
                vD = source[lastLineOffset + dimXm1];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + dimXm1] = (char) r;
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static class ForBit extends QuickGradientByCross3x3 {

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
            int vL = (source[middleLineOffset + dimXm1] ? 1 : 0);
            int vR = (source[middleLineOffset + rem1ForDimX] ? 1 : 0);
            int vU = (source[firstLineOffset] ? 1 : 0);
            int vD = (source[lastLineOffset] ? 1 : 0);
            int r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
            result[resultLineOffset] = r != 0;
            for (int k = 1; k < dimXm1; k++) {
                vL = (source[middleLineOffset + k - 1] ? 1 : 0);
                vR = (source[middleLineOffset + k + 1] ? 1 : 0);
                vU = (source[firstLineOffset + k] ? 1 : 0);
                vD = (source[lastLineOffset + k] ? 1 : 0);
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + k] = r != 0;
            }
            if (dimX >= 2) {
                vL = (source[middleLineOffset + dimXm1 - 1] ? 1 : 0);
                vR = (source[middleLineOffset] ? 1 : 0);
                vU = (source[firstLineOffset + dimXm1] ? 1 : 0);
                vD = (source[lastLineOffset + dimXm1] ? 1 : 0);
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + dimXm1] = r != 0;
            }
        }
    }


    private static class ForByte extends QuickGradientByCross3x3 {

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
            int vL = (source[middleLineOffset + dimXm1] & 0xFF);
            int vR = (source[middleLineOffset + rem1ForDimX] & 0xFF);
            int vU = (source[firstLineOffset] & 0xFF);
            int vD = (source[lastLineOffset] & 0xFF);
            int r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
            result[resultLineOffset] = (byte) r;
            for (int k = 1; k < dimXm1; k++) {
                vL = (source[middleLineOffset + k - 1] & 0xFF);
                vR = (source[middleLineOffset + k + 1] & 0xFF);
                vU = (source[firstLineOffset + k] & 0xFF);
                vD = (source[lastLineOffset + k] & 0xFF);
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + k] = (byte) r;
            }
            if (dimX >= 2) {
                vL = (source[middleLineOffset + dimXm1 - 1] & 0xFF);
                vR = (source[middleLineOffset] & 0xFF);
                vU = (source[firstLineOffset + dimXm1] & 0xFF);
                vD = (source[lastLineOffset + dimXm1] & 0xFF);
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + dimXm1] = (byte) r;
            }
        }
    }


    private static class ForShort extends QuickGradientByCross3x3 {

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
            int vL = (source[middleLineOffset + dimXm1] & 0xFFFF);
            int vR = (source[middleLineOffset + rem1ForDimX] & 0xFFFF);
            int vU = (source[firstLineOffset] & 0xFFFF);
            int vD = (source[lastLineOffset] & 0xFFFF);
            int r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
            result[resultLineOffset] = (short) r;
            for (int k = 1; k < dimXm1; k++) {
                vL = (source[middleLineOffset + k - 1] & 0xFFFF);
                vR = (source[middleLineOffset + k + 1] & 0xFFFF);
                vU = (source[firstLineOffset + k] & 0xFFFF);
                vD = (source[lastLineOffset + k] & 0xFFFF);
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + k] = (short) r;
            }
            if (dimX >= 2) {
                vL = (source[middleLineOffset + dimXm1 - 1] & 0xFFFF);
                vR = (source[middleLineOffset] & 0xFFFF);
                vU = (source[firstLineOffset + dimXm1] & 0xFFFF);
                vD = (source[lastLineOffset + dimXm1] & 0xFFFF);
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + dimXm1] = (short) r;
            }
        }
    }


    private static class ForInt extends QuickGradientByCross3x3 {

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
            long vL = source[middleLineOffset + dimXm1];
            long vR = source[middleLineOffset + rem1ForDimX];
            long vU = source[firstLineOffset];
            long vD = source[lastLineOffset];
            long r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
            result[resultLineOffset] = (int) r;
            for (int k = 1; k < dimXm1; k++) {
                vL = source[middleLineOffset + k - 1];
                vR = source[middleLineOffset + k + 1];
                vU = source[firstLineOffset + k];
                vD = source[lastLineOffset + k];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + k] = (int) r;
            }
            if (dimX >= 2) {
                vL = source[middleLineOffset + dimXm1 - 1];
                vR = source[middleLineOffset];
                vU = source[firstLineOffset + dimXm1];
                vD = source[lastLineOffset + dimXm1];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) >> 1;
                result[resultLineOffset + dimXm1] = (int) r;
            }
        }
    }


    private static class ForLong extends QuickGradientByCross3x3 {

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
            double vL = source[middleLineOffset + dimXm1];
            double vR = source[middleLineOffset + rem1ForDimX];
            double vU = source[firstLineOffset];
            double vD = source[lastLineOffset];
            double r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
            result[resultLineOffset] = (long) r;
            for (int k = 1; k < dimXm1; k++) {
                vL = source[middleLineOffset + k - 1];
                vR = source[middleLineOffset + k + 1];
                vU = source[firstLineOffset + k];
                vD = source[lastLineOffset + k];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
                result[resultLineOffset + k] = (long) r;
            }
            if (dimX >= 2) {
                vL = source[middleLineOffset + dimXm1 - 1];
                vR = source[middleLineOffset];
                vU = source[firstLineOffset + dimXm1];
                vD = source[lastLineOffset + dimXm1];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
                result[resultLineOffset + dimXm1] = (long) r;
            }
        }
    }


    private static class ForFloat extends QuickGradientByCross3x3 {

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
            double vL = source[middleLineOffset + dimXm1];
            double vR = source[middleLineOffset + rem1ForDimX];
            double vU = source[firstLineOffset];
            double vD = source[lastLineOffset];
            double r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
            result[resultLineOffset] = (float) r;
            for (int k = 1; k < dimXm1; k++) {
                vL = source[middleLineOffset + k - 1];
                vR = source[middleLineOffset + k + 1];
                vU = source[firstLineOffset + k];
                vD = source[lastLineOffset + k];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
                result[resultLineOffset + k] = (float) r;
            }
            if (dimX >= 2) {
                vL = source[middleLineOffset + dimXm1 - 1];
                vR = source[middleLineOffset];
                vU = source[firstLineOffset + dimXm1];
                vD = source[lastLineOffset + dimXm1];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
                result[resultLineOffset + dimXm1] = (float) r;
            }
        }
    }


    private static class ForDouble extends QuickGradientByCross3x3 {

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
            double r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
            result[resultLineOffset] = r;
            for (int k = 1; k < dimXm1; k++) {
                vL = source[middleLineOffset + k - 1];
                vR = source[middleLineOffset + k + 1];
                vU = source[firstLineOffset + k];
                vD = source[lastLineOffset + k];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
                result[resultLineOffset + k] = r;
            }
            if (dimX >= 2) {
                vL = source[middleLineOffset + dimXm1 - 1];
                vR = source[middleLineOffset];
                vU = source[firstLineOffset + dimXm1];
                vD = source[lastLineOffset + dimXm1];
                r = ((vL < vR ? vR - vL : vL - vR) + (vU < vD ? vD - vU : vU - vD)) * 0.5;
                result[resultLineOffset + dimXm1] = r;
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/
}
