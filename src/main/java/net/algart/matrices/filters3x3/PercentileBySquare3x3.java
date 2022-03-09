package net.algart.matrices.filters3x3;

import java.util.Arrays;
import java.util.Random;

public abstract class PercentileBySquare3x3 extends AbstractQuickFilter3x3 {
    final int percentileIndex;
    final int percentileIndexP1;
    final int percentileIndexM1;

    PercentileBySquare3x3(Class<?> elementType, long[] dimensions, int percentileIndex) {
        super(elementType, dimensions);
        if (percentileIndex < 0 || percentileIndex > 8) {
            throw new IllegalArgumentException("Percentile index is out of allowed range 0..8");
        }
        this.percentileIndex = percentileIndex;
        this.percentileIndexP1 = percentileIndex + 1;
        this.percentileIndexM1 = percentileIndex - 1;
    }

    public static PercentileBySquare3x3 newInstance(
            Class<?> elementType,
            long dimX,
            long dimY,
            int percentileIndex) {
        return newInstance(elementType, new long[]{dimX, dimY}, percentileIndex);
    }

    public static PercentileBySquare3x3 newInstance(
            Class<?> elementType,
            long[] dimensions,
            int percentileIndex) {
        return newInstance(elementType, dimensions, percentileIndex, true);
    }

    public static PercentileBySquare3x3 newInstance(
            Class<?> elementType,
            long[] dimensions,
            int percentileIndex,
            boolean specialAlgorithmWhenPossible) {
        if (specialAlgorithmWhenPossible) {
            switch (percentileIndex) {
                case 4:
                    return MedianBySquare3x3.newInstance(elementType, dimensions);
                case 0:
                    return ErosionBySquare3x3.newInstance(elementType, dimensions);
                case 8:
                    return DilationBySquare3x3.newInstance(elementType, dimensions);
            }
        }
        if (elementType == char.class) {
            return new ForChar(dimensions, percentileIndex);
        } else if (elementType == boolean.class) {
            return new ForBit(dimensions, percentileIndex);
        } else if (elementType == byte.class) {
            return new ForByte(dimensions, percentileIndex);
        } else if (elementType == short.class) {
            return new ForShort(dimensions, percentileIndex);
        } else if (elementType == int.class) {
            return new ForInt(dimensions, percentileIndex);
        } else if (elementType == long.class) {
            return new ForLong(dimensions, percentileIndex);
        } else if (elementType == float.class) {
            return new ForFloat(dimensions, percentileIndex);
        } else if (elementType == double.class) {
            return new ForDouble(dimensions, percentileIndex);
        } else {
            throw new UnsupportedOperationException("Non-primitive element type " + elementType + " is not supported");
        }
    }

    public int percentileIndex() {
        return percentileIndex;
    }

    /*Repeat() Char                 ==> Byte,,Short,,Int,,Long,,Float,,Double;;
               char                 ==> byte,,short,,int,,long,,float,,double;;
               (v\d\s*=\s*)((?:source)\[[^\]]+\])       ==> $1$2 & 0xFF,,
                                                            $1$2 & 0xFFFF,,
                                                            $1$2,,...;;
               (int)(\s+v\w|\s+ai|\s+[vwr]\b|\[)        ==> $1$2,,$1$2,,$1$2,,long$2,,float$2,,double$2;;
               (int)(\s+min\b|\s+max\b)                 ==> $1$2,,$1$2,,$1$2,,long$2,,float$2,,double$2;;
               (int)(\s+slowPercentile|\s+percentile(?!Index))  ==> $1$2,,$1$2,,$1$2,,long$2,,float$2,,double$2;;
               (int)(\s*\.\.\.)     ==> $1$2,,$1$2,,$1$2,,long$2,,float$2,,double$2;;
               \(double\)\s+        ==> ,,...
     */
    private static class ForChar extends PercentileBySquare3x3 {
        private final int[][] threadLeft;
        private final int[][] threadRight;

        private ForChar(long[] dimensions, int percentileIndex) {
            super(char.class, dimensions, percentileIndex);
            this.threadLeft = new int[numberOfRanges()][7];
            this.threadRight = new int[numberOfRanges()][7];
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
            final int[] left = threadLeft[multithreadingRangeIndex];
            final int[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            int v0 = source[firstLineOffset + dimXm1];
            int v1 = source[firstLineOffset];
            int v2 = source[firstLineOffset + rem1ForDimX];
            int v3 = source[middleLineOffset + dimXm1];
            int v4 = source[middleLineOffset];
            int v5 = source[middleLineOffset + rem1ForDimX];
            int v6 = source[lastLineOffset + dimXm1];
            int v7 = source[lastLineOffset];
            int v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = (char) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                int r;
                int vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = (char) r;
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
                result[++resultLineOffset] = (char) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private int slowPercentile(int... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionA(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            int r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(int[] left, int[] right, int v0, int vL, int vR, int vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(int[] right, int count, int v) {
            int w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(int[] left, int count, int v) {
            int w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionB(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            int r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(int[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                int ai = a[i = k];
                if (ai < a[i - 1]) {
                    int w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionC(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static int percentileStartingFromMinimum(int[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static int percentileStartingFromMaximum(int[] a, int count, int percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static class ForByte extends PercentileBySquare3x3 {
        private final int[][] threadLeft;
        private final int[][] threadRight;

        private ForByte(long[] dimensions, int percentileIndex) {
            super(byte.class, dimensions, percentileIndex);
            this.threadLeft = new int[numberOfRanges()][7];
            this.threadRight = new int[numberOfRanges()][7];
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
            final int[] left = threadLeft[multithreadingRangeIndex];
            final int[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            int v0 = source[firstLineOffset + dimXm1] & 0xFF;
            int v1 = source[firstLineOffset] & 0xFF;
            int v2 = source[firstLineOffset + rem1ForDimX] & 0xFF;
            int v3 = source[middleLineOffset + dimXm1] & 0xFF;
            int v4 = source[middleLineOffset] & 0xFF;
            int v5 = source[middleLineOffset + rem1ForDimX] & 0xFF;
            int v6 = source[lastLineOffset + dimXm1] & 0xFF;
            int v7 = source[lastLineOffset] & 0xFF;
            int v8 = source[lastLineOffset + rem1ForDimX] & 0xFF;
            result[resultLineOffset] = (byte) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                int r;
                int vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = (byte) r;
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
                result[++resultLineOffset] = (byte) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private int slowPercentile(int... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionA(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            int r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(int[] left, int[] right, int v0, int vL, int vR, int vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(int[] right, int count, int v) {
            int w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(int[] left, int count, int v) {
            int w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionB(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            int r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(int[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                int ai = a[i = k];
                if (ai < a[i - 1]) {
                    int w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionC(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static int percentileStartingFromMinimum(int[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static int percentileStartingFromMaximum(int[] a, int count, int percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }


    private static class ForShort extends PercentileBySquare3x3 {
        private final int[][] threadLeft;
        private final int[][] threadRight;

        private ForShort(long[] dimensions, int percentileIndex) {
            super(short.class, dimensions, percentileIndex);
            this.threadLeft = new int[numberOfRanges()][7];
            this.threadRight = new int[numberOfRanges()][7];
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
            final int[] left = threadLeft[multithreadingRangeIndex];
            final int[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            int v0 = source[firstLineOffset + dimXm1] & 0xFFFF;
            int v1 = source[firstLineOffset] & 0xFFFF;
            int v2 = source[firstLineOffset + rem1ForDimX] & 0xFFFF;
            int v3 = source[middleLineOffset + dimXm1] & 0xFFFF;
            int v4 = source[middleLineOffset] & 0xFFFF;
            int v5 = source[middleLineOffset + rem1ForDimX] & 0xFFFF;
            int v6 = source[lastLineOffset + dimXm1] & 0xFFFF;
            int v7 = source[lastLineOffset] & 0xFFFF;
            int v8 = source[lastLineOffset + rem1ForDimX] & 0xFFFF;
            result[resultLineOffset] = (short) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                int r;
                int vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = (short) r;
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
                result[++resultLineOffset] = (short) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private int slowPercentile(int... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionA(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            int r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(int[] left, int[] right, int v0, int vL, int vR, int vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(int[] right, int count, int v) {
            int w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(int[] left, int count, int v) {
            int w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionB(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            int r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(int[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                int ai = a[i = k];
                if (ai < a[i - 1]) {
                    int w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionC(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static int percentileStartingFromMinimum(int[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static int percentileStartingFromMaximum(int[] a, int count, int percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }


    private static class ForInt extends PercentileBySquare3x3 {
        private final int[][] threadLeft;
        private final int[][] threadRight;

        private ForInt(long[] dimensions, int percentileIndex) {
            super(int.class, dimensions, percentileIndex);
            this.threadLeft = new int[numberOfRanges()][7];
            this.threadRight = new int[numberOfRanges()][7];
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
            final int[] left = threadLeft[multithreadingRangeIndex];
            final int[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            int v0 = source[firstLineOffset + dimXm1];
            int v1 = source[firstLineOffset];
            int v2 = source[firstLineOffset + rem1ForDimX];
            int v3 = source[middleLineOffset + dimXm1];
            int v4 = source[middleLineOffset];
            int v5 = source[middleLineOffset + rem1ForDimX];
            int v6 = source[lastLineOffset + dimXm1];
            int v7 = source[lastLineOffset];
            int v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = (int) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                int r;
                int vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = (int) r;
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
                result[++resultLineOffset] = (int) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private int slowPercentile(int... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionA(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            int r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(int[] left, int[] right, int v0, int vL, int vR, int vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(int[] right, int count, int v) {
            int w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(int[] left, int count, int v) {
            int w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionB(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            int r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(int[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                int ai = a[i = k];
                if (ai < a[i - 1]) {
                    int w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private int percentileVersionC(
                int[] left, int[] right,
                int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            int vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static int percentileStartingFromMinimum(int[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static int percentileStartingFromMaximum(int[] a, int count, int percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final int w = a[k];
                int max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    int ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }


    private static class ForLong extends PercentileBySquare3x3 {
        private final long[][] threadLeft;
        private final long[][] threadRight;

        private ForLong(long[] dimensions, int percentileIndex) {
            super(long.class, dimensions, percentileIndex);
            this.threadLeft = new long[numberOfRanges()][7];
            this.threadRight = new long[numberOfRanges()][7];
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
            final long[] left = threadLeft[multithreadingRangeIndex];
            final long[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            long v0 = source[firstLineOffset + dimXm1];
            long v1 = source[firstLineOffset];
            long v2 = source[firstLineOffset + rem1ForDimX];
            long v3 = source[middleLineOffset + dimXm1];
            long v4 = source[middleLineOffset];
            long v5 = source[middleLineOffset + rem1ForDimX];
            long v6 = source[lastLineOffset + dimXm1];
            long v7 = source[lastLineOffset];
            long v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = (long) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                long r;
                long vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = (long) r;
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
                result[++resultLineOffset] = (long) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private long slowPercentile(long... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private long percentileVersionA(
                long[] left, long[] right,
                long v0, long v1, long v2, long v3, long v4, long v5, long v6, long v7, long v8) {
            long vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            long r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(long[] left, long[] right, long v0, long vL, long vR, long vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(long[] right, int count, long v) {
            long w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(long[] left, int count, long v) {
            long w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private long percentileVersionB(
                long[] left, long[] right,
                long v0, long v1, long v2, long v3, long v4, long v5, long v6, long v7, long v8) {
            long vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            long r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(long[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                long ai = a[i = k];
                if (ai < a[i - 1]) {
                    long w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private long percentileVersionC(
                long[] left, long[] right,
                long v0, long v1, long v2, long v3, long v4, long v5, long v6, long v7, long v8) {
            long vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static long percentileStartingFromMinimum(long[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final long w = a[k];
                long min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    long ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static long percentileStartingFromMaximum(long[] a, int count, long percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final long w = a[k];
                long max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    long ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }


    private static class ForFloat extends PercentileBySquare3x3 {
        private final float[][] threadLeft;
        private final float[][] threadRight;

        private ForFloat(long[] dimensions, int percentileIndex) {
            super(float.class, dimensions, percentileIndex);
            this.threadLeft = new float[numberOfRanges()][7];
            this.threadRight = new float[numberOfRanges()][7];
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
            final float[] left = threadLeft[multithreadingRangeIndex];
            final float[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            float v0 = source[firstLineOffset + dimXm1];
            float v1 = source[firstLineOffset];
            float v2 = source[firstLineOffset + rem1ForDimX];
            float v3 = source[middleLineOffset + dimXm1];
            float v4 = source[middleLineOffset];
            float v5 = source[middleLineOffset + rem1ForDimX];
            float v6 = source[lastLineOffset + dimXm1];
            float v7 = source[lastLineOffset];
            float v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = (float) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                float r;
                float vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = (float) r;
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
                result[++resultLineOffset] = (float) slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private float slowPercentile(float... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private float percentileVersionA(
                float[] left, float[] right,
                float v0, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8) {
            float vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            float r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(float[] left, float[] right, float v0, float vL, float vR, float vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(float[] right, int count, float v) {
            float w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(float[] left, int count, float v) {
            float w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private float percentileVersionB(
                float[] left, float[] right,
                float v0, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8) {
            float vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            float r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(float[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                float ai = a[i = k];
                if (ai < a[i - 1]) {
                    float w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private float percentileVersionC(
                float[] left, float[] right,
                float v0, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8) {
            float vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static float percentileStartingFromMinimum(float[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final float w = a[k];
                float min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    float ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static float percentileStartingFromMaximum(float[] a, int count, float percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final float w = a[k];
                float max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    float ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }


    private static class ForDouble extends PercentileBySquare3x3 {
        private final double[][] threadLeft;
        private final double[][] threadRight;

        private ForDouble(long[] dimensions, int percentileIndex) {
            super(double.class, dimensions, percentileIndex);
            this.threadLeft = new double[numberOfRanges()][7];
            this.threadRight = new double[numberOfRanges()][7];
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
            final double[] left = threadLeft[multithreadingRangeIndex];
            final double[] right = threadRight[multithreadingRangeIndex];
            final int percentileIndex = this.percentileIndex;
            final int percentileIndexP1 = this.percentileIndexP1;
            final int percentileIndexM1 = this.percentileIndexM1;
            double v0 = source[firstLineOffset + dimXm1];
            double v1 = source[firstLineOffset];
            double v2 = source[firstLineOffset + rem1ForDimX];
            double v3 = source[middleLineOffset + dimXm1];
            double v4 = source[middleLineOffset];
            double v5 = source[middleLineOffset + rem1ForDimX];
            double v6 = source[lastLineOffset + dimXm1];
            double v7 = source[lastLineOffset];
            double v8 = source[lastLineOffset + rem1ForDimX];
            result[resultLineOffset] = slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                double r;
                double vL, vR, vM;
                if (v3 < v4) {
                    vL = v3;
                    vM = v4;
                } else {
                    vL = v4;
                    vM = v3;
                }
                if (v5 >= vM) {
                    vR = v5;
                } else if (v5 >= vL) {
                    vR = vM;
                    vM = v5;
                } else {
                    vR = vM;
                    vM = vL;
                    vL = v5;
                }
                // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
                int leftCount, rightCount;
                left[0] = vL;
                right[0] = vR;
                if (v0 < vM) {
                    left[1] = v0;
                    leftCount = 2;
                    rightCount = 1;
                } else {
                    right[1] = v0;
                    leftCount = 1;
                    rightCount = 2;
                }
                if (v1 < vM) {
                    left[leftCount++] = v1;
                } else {
                    right[rightCount++] = v1;
                }
                if (v2 < vM) {
                    left[leftCount++] = v2;
                } else {
                    right[rightCount++] = v2;
                }
                if (v6 < vM) {
                    left[leftCount++] = v6;
                } else {
                    right[rightCount++] = v6;
                }
                if (v7 < vM) {
                    left[leftCount++] = v7;
                } else {
                    right[rightCount++] = v7;
                }
                if (v8 < vM) {
                    left[leftCount++] = v8;
                } else {
                    right[rightCount++] = v8;
                }
                if (percentileIndex == leftCount) {
                    r = vM;
                } else if (percentileIndex < leftCount) {
                    r = percentileStartingFromMaximum(left, leftCount,
                            leftCount - percentileIndexP1);
                    // - if it is the median, more probable that percentileIndex is almost maximum
                } else {
                    r = percentileStartingFromMinimum(right, rightCount,
                            percentileIndexM1 - leftCount);
                    // - if it is the median, more probable that percentileIndex is almost minimum
                }
                result[++resultLineOffset] = r;
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
                result[++resultLineOffset] = slowPercentile(v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        private double slowPercentile(double... values) {
            Arrays.sort(values);
            return values[percentileIndex];
        }

        // This method preserved to simplify internal testing.
        private double percentileVersionA(
                double[] left, double[] right,
                double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7, double v8) {
            double vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            if (insertV0(left, right, v0, vL, vR, vM)) {
                leftCount = 2;
                rightCount = 1;
            } else {
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                insertLeft(left, leftCount++, v1);
            } else {
                insertRight(right, rightCount++, v1);
            }
            if (v2 < vM) {
                insertLeft(left, leftCount++, v2);
            } else {
                insertRight(right, rightCount++, v2);
            }
            if (v6 < vM) {
                insertLeft(left, leftCount++, v6);
            } else {
                insertRight(right, rightCount++, v6);
            }
            if (v7 < vM) {
                insertLeft(left, leftCount++, v7);
            } else {
                insertRight(right, rightCount++, v7);
            }
            if (v8 < vM) {
                insertLeft(left, leftCount++, v8);
            } else {
                insertRight(right, rightCount++, v8);
            }
            // assert leftCount + rightCount == 8;
            double r;
            if (percentileIndex < leftCount) {
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private boolean insertV0(double[] left, double[] right, double v0, double vL, double vR, double vM) {
            if (v0 < vM) {
                if (v0 < vL) {
                    left[0] = v0;
                    left[1] = vL;
                } else {
                    left[0] = vL;
                    left[1] = v0;
                }
                right[0] = vR;
                return true;
            } else {
                if (v0 < vR) {
                    right[0] = v0;
                    right[1] = vR;
                } else {
                    right[0] = vR;
                    right[1] = v0;
                }
                left[0] = vL;
                return false;
            }
        }

        private void insertRight(double[] right, int count, double v) {
            double w;
            while (count > 0 && v <= (w = right[count - 1])) {
                right[count--] = w;
            }
            right[count] = v;
        }

        private void insertLeft(double[] left, int count, double v) {
            double w;
            while (count > 0 && v <= (w = left[count - 1])) {
                left[count--] = w;
            }
            left[count] = v;
        }

        // This method preserved to simplify internal testing.
        private double percentileVersionB(
                double[] left, double[] right,
                double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7, double v8) {
            double vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            double r;
            if (percentileIndex < leftCount) {
                insertionSort(left, leftCount);
                r = left[percentileIndex];
            } else if (percentileIndex == leftCount) {
                r = vM;
            } else {
                insertionSort(right, rightCount);
                r = right[percentileIndex - leftCount - 1];
            }
            return r;
        }

        private static void insertionSort(double[] a, int count) {
            for (int i, k = 0; ++k < count; ) {
                double ai = a[i = k];
                if (ai < a[i - 1]) {
                    double w;
                    while (--i >= 0 && ai < (w = a[i])) {
                        a[i + 1] = w;
                    }
                    a[i + 1] = ai;
                }
            }
        }

        // This method preserved to simplify internal testing.
        private double percentileVersionC(
                double[] left, double[] right,
                double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7, double v8) {
            double vL, vR, vM;
            if (v3 < v4) {
                vL = v3;
                vM = v4;
            } else {
                vL = v4;
                vM = v3;
            }
            if (v5 >= vM) {
                vR = v5;
            } else if (v5 >= vL) {
                vR = vM;
                vM = v5;
            } else {
                vR = vM;
                vM = vL;
                vL = v5;
            }
            // - now vL <= vM <= vR: sorted 3 elements v3, v4, v5
            int leftCount, rightCount;
            left[0] = vL;
            right[0] = vR;
            if (v0 < vM) {
                left[1] = v0;
                leftCount = 2;
                rightCount = 1;
            } else {
                right[1] = v0;
                leftCount = 1;
                rightCount = 2;
            }
            if (v1 < vM) {
                left[leftCount++] = v1;
            } else {
                right[rightCount++] = v1;
            }
            if (v2 < vM) {
                left[leftCount++] = v2;
            } else {
                right[rightCount++] = v2;
            }
            if (v6 < vM) {
                left[leftCount++] = v6;
            } else {
                right[rightCount++] = v6;
            }
            if (v7 < vM) {
                left[leftCount++] = v7;
            } else {
                right[rightCount++] = v7;
            }
            if (v8 < vM) {
                left[leftCount++] = v8;
            } else {
                right[rightCount++] = v8;
            }
            if (percentileIndex == leftCount) {
                return vM;
            } else if (percentileIndex < leftCount) {
                return percentileStartingFromMaximum(left, leftCount,
                        leftCount - percentileIndexP1);
                // - if it is the median, more probable that percentileIndex is almost maximum
            } else {
                return percentileStartingFromMinimum(right, rightCount,
                        percentileIndexM1 - leftCount);
                // - if it is the median, more probable that percentileIndex is almost minimum
            }
        }

        private static double percentileStartingFromMinimum(double[] a, int count, int percentileIndex) {
            for (int k = 0; ; k++) {
                final double w = a[k];
                double min = w;
                int indexOfMin = k;
                for (int i = k + 1; i < count; i++) {
                    double ai = a[i];
                    if (ai < min) {
                        min = ai;
                        indexOfMin = i;
                    }
                }
                if (k == percentileIndex) {
                    return min;
                }
                a[indexOfMin] = w;
                // - if indexOfMin == k, it was a real minimum; just go to the next k;
                // if not, we replace minimum with a[k]
            }
        }

        private static double percentileStartingFromMaximum(double[] a, int count, double percentileReverseIndex) {
            for (int k = 0; ; k++) {
                final double w = a[k];
                double max = w;
                int indexOfMax = k;
                for (int i = k + 1; i < count; i++) {
                    double ai = a[i];
                    if (ai > max) {
                        max = ai;
                        indexOfMax = i;
                    }
                }
                if (k == percentileReverseIndex) {
                    return max;
                }
                a[indexOfMax] = w;
                // - if indexOfMax == k, it was a real minimum; just go to the next k;
                // if not, we replace maximum with a[k]
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    static class ForBit extends PercentileBySquare3x3 {
        private ForBit(long[] dimensions, int percentileIndex) {
            super(boolean.class, dimensions, percentileIndex);
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
            result[resultLineOffset] = percentile(percentileIndex, v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                result[++resultLineOffset] = percentile(percentileIndex, v0, v1, v2, v3, v4, v5, v6, v7, v8);
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
                result[++resultLineOffset] = percentile(percentileIndex, v0, v1, v2, v3, v4, v5, v6, v7, v8);
            }
        }

        static boolean percentile(
                int percentileIndex,
                boolean v0,
                boolean v1,
                boolean v2,
                boolean v3,
                boolean v4,
                boolean v5,
                boolean v6,
                boolean v7,
                boolean v8) {
            int count = v0 ? 1 : 0;
            if (v1) {
                count++;
            }
            if (v2) {
                count++;
            }
            if (v3) {
                count++;
            }
            if (v4) {
                count++;
            }
            if (v5) {
                count++;
            }
            if (v6) {
                count++;
            }
            if (v7) {
                count++;
            }
            if (v8) {
                count++;
            }
            return percentileIndex >= 9 - count;
        }
    }

    public static void main(String[] args) {
        final int max = 100;
        final Random rnd = new Random(22);
        int[] v = new int[9];
        for (int test = 1, testCount = 1024 * 1024; test <= testCount; test++) {
            Arrays.setAll(v, k -> rnd.nextInt(max));
            int count = 1 + rnd.nextInt(8);
            int index = rnd.nextInt(count);
            final int percentileL = ForInt.percentileStartingFromMinimum(v.clone(), count, index);
            final int percentileR = ForInt.percentileStartingFromMaximum(v.clone(), count, index);
            Arrays.sort(v, 0, count);
            if (percentileL != v[index]) {
                throw new AssertionError(test + ": Invalid percentileL #" + index
                        + ": " + percentileL + " instead of " + v[index] + " for " + Arrays.toString(v));
            }
            if (percentileR != v[count - 1 - index]) {
                throw new AssertionError(test + ": Invalid percentileR #" + index
                        + ": " + percentileR + " instead of " + v[count - 1 - index] + " for " + Arrays.toString(v));
            }
        }

        int[] left = new int[7];
        int[] right = new int[7];
        for (int test = 1, testCount = 16 * 1024 * 1024; test <= testCount; test++) {
            if ((test & 0xFF) == 0) {
                System.out.print("\r" + test);
            }
            final int index = rnd.nextInt(9);
            final ForInt percentile = new ForInt(new long[]{10, 10}, index);
            Arrays.setAll(v, k -> rnd.nextInt(max));
            final int percentileSimple = percentile.slowPercentile(v);
            final int percentileA = percentile.percentileVersionA(
                    left, right, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
            if (percentileA != percentileSimple) {
                throw new AssertionError(test + ": Invalid percentile-A #" + index
                        + ": " + percentileA + " instead of " + percentileSimple + " for " + Arrays.toString(v));
            }
            final int percentileB = percentile.percentileVersionB(
                    left, right, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
            if (percentileB != percentileSimple) {
                throw new AssertionError(test + ": Invalid percentile-B #" + index
                        + ": " + percentileB + " instead of " + percentileSimple + " for " + Arrays.toString(v));
            }
            final int percentileC = percentile.percentileVersionC(
                    left, right, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
            if (percentileC != percentileSimple) {
                throw new AssertionError(test + ": Invalid percentile-C #" + index
                        + ": " + percentileC + " instead of " + percentileSimple + " for " + Arrays.toString(v));
            }
        }
    }
}
