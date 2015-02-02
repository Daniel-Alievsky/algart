/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.IRange;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.QuickPointCountPattern;
import net.algart.math.patterns.TooManyPointsInPatternError;
import net.algart.math.patterns.UniformGridPattern;

import java.util.ArrayList;
import java.util.List;

class Summator extends RankOperationProcessor {
    private final boolean optimizeGetData = OPTIMIZE_GET_DATA;
    private final boolean optimizeDirectArrays = OPTIMIZE_DIRECT_ARRAYS;
    private final boolean optimizeSegmentsAlongAxes = OPTIMIZE_SEGMENTS_ALONG_AXES;
    private final boolean specialOptimizeThinPatternsPowersOfTwo = SPECIAL_OPTIMIZE_THIN_PATTERNS_POWERS_OF_TWO;

    private final Func processingFunc;
    private final boolean linearFunc;
    private final boolean dividingByPowerOfTwoWithRoundingFunc;
    private final double lfA, lfB;
    private final long halfDivisor;
    private final int logDivisor;

    Summator(ArrayContext context, Func processingFunc) {
        super(context, RankPrecision.BITS_1.bitLevels); // bitLevels are not used by this class
        if (processingFunc == null)
            throw new NullPointerException("Null contrastingFunc");
        this.processingFunc = processingFunc;
        if (processingFunc == Func.IDENTITY) {
            this.linearFunc = true;
            this.dividingByPowerOfTwoWithRoundingFunc = true;
            this.lfA = 1.0;
            this.lfB = 0.0;
            this.halfDivisor = 0;
            this.logDivisor = 0;
        } else if (processingFunc instanceof LinearFunc) {
            this.linearFunc = true;
            this.lfA = ((LinearFunc)processingFunc).a(0); // IndexOutOfBoundsException possible for invalid LinearFunc
            this.lfB = ((LinearFunc)processingFunc).b();
            long m = Math.round(1.0 / lfA);
            if (this.lfB == 0.5 && m > 0 && this.lfA == 1.0 / (double)m && (m & (m - 1)) == 0) {
                this.dividingByPowerOfTwoWithRoundingFunc = true;
                this.halfDivisor = m >> 1;
                this.logDivisor = 63 - Long.numberOfLeadingZeros(m);
            } else {
                this.dividingByPowerOfTwoWithRoundingFunc = false;
                this.halfDivisor = 0;
                this.logDivisor = -1;
            }
        } else {
            this.linearFunc = false;
            this.dividingByPowerOfTwoWithRoundingFunc = false;
            this.lfA = Double.NaN;
            this.lfB = Double.NaN;
            this.halfDivisor = 0;
            this.logDivisor = -1;
        }
    }

    @Override
    public void process(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        List<? extends Matrix<? extends PArray>> additionalMatrices, Pattern pattern)
    {
        if (additionalMatrices == null)
            throw new NullPointerException("Null additionalMatrices argument");
        if (optimizeSegmentsAlongAxes
            && pattern instanceof UniformGridPattern
            && pattern instanceof QuickPointCountPattern
            && ((UniformGridPattern) pattern).isActuallyRectangular())
        {
            additionalMatrices = new ArrayList<Matrix<? extends PArray>>(additionalMatrices);
            // - to avoid changing by parallel threads
            checkArguments(dest, src, additionalMatrices, pattern);
            final long pointCount = pattern.pointCount();
            if (pointCount > 1) {
                if (pointCount > Integer.MAX_VALUE)
                    throw new TooManyPointsInPatternError("Too large number of points in the pattern: "
                        + pointCount + " > Integer.MAX_VALUE");
                // There is an analogous requirement in the usual branch in RankOperationProcessor:
                // we build a Java array of all pattern points, so it is impossible to process more
                // than Integer.MAX_VALUE points.
                // In this branch, it is theoretically possible to process up to Long.MAX_VALUE points,
                // but it is practically useless; this check allows to avoid overflow while integer summarizing.
                for (int k = 1, n = pattern.dimCount(); k < n; k++) {
                    // Starting from k=1: no sense to specially optimize summarizing along x-axis
                    IRange range = pattern.roundedCoordRange(k);
                    if (range.size() == pointCount) {
                        processAlongAxis(dest.array(), src.array(), src.dimensions(), k, range.min(), range.max());
                        return;
                    }
                }
            }
        }
        super.process(dest, src, additionalMatrices, pattern);
    }

    @Override
    PArray asProcessed(Class<? extends PArray> desiredType, PArray src, PArray[] additional,
        long[] dimensions, final long[] shifts, final long[] left, final long[] right)
    {
        assert shifts.length > 0;
        assert left.length == right.length;
        final boolean direct = optimizeDirectArrays &&
            src instanceof DirectAccessible && ((DirectAccessible)src).hasJavaArray();
        final boolean meanFunc = linearFunc && lfA >= 0.0 && lfA <= 1.0 / shifts.length && lfB >= 0.0 && lfB < 1.0;
        // in this case we can be sure that the linear function for byte will be <=255, for short <=65535
        final boolean meanByPowerOfTwoFunc = meanFunc && dividingByPowerOfTwoWithRoundingFunc;
        if (src instanceof BitArray) {
            final BitArray a = (BitArray)src;
            return new AbstractDoubleArray(src.length(), true, src) {
                public double getDouble(long index) {
                    long sum = 0;
                    for (long shift : shifts) {
                        long i = index - shift;
                        if (i < 0) {
                            i += length;
                        }
                        if (a.getBit(i)) {
                            sum++;
                        }
                    }
                    return processingFunc.get(sum);
                }

                public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                    if (!optimizeGetData) {
                        super.getData(arrayPos, destArray, destArrayOffset, count);
                        return;
                    }
                    if (destArray == null)
                        throw new NullPointerException("Null destArray argument");
                    checkRanges(length, arrayPos, count);
                    if (count == 0) {
                        return;
                    }
                    double[] dest = (double[])destArray;
                    long sum = 0;
                    for (long shift : shifts) {
                        long i = arrayPos - shift;
                        if (i < 0) {
                            i += length;
                        }
                        if (a.getBit(i)) {
                            sum++;
                        }
                    }
                    for (int k = 0; k < count; k++, destArrayOffset++) {
                        dest[destArrayOffset] = processingFunc.get(sum);
                        for (long shift : right) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            if (a.getBit(i)) {
                                sum--;
                                assert sum >= 0 : "Unbalanced 0 and 1 bits";
                            }
                        }
                        arrayPos++;
                        if (arrayPos == length) {
                            arrayPos = 0;
                        }
                        for (long shift : left) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            if (a.getBit(i)) {
                                sum++;
                            }
                        }
                    }
                }
            };
        }

        //[[Repeat() 0xFFFF          ==> 0xFF,,0xFFFF;;
        //           (return)\s+\(char\) ==> $1 (int),,$1 (int);;
        //           char            ==> byte,,short;;
        //           (\w+\s+)getChar ==> int getByte,,int getShort;;
        //           Char            ==> Byte,,Short;;
        //           16              ==> 8,,16 ]]
        if (src instanceof CharArray) {
            final CharArray a = (CharArray)src;
            if (direct) {
                final char[] ja = (char[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (meanFunc && desiredType == CharArray.class) { // char A: direct, returning CharArray
                    return new AbstractCharArray(src.length(), true, src) {
                        public char getChar(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            double v = processingFunc.get(sum);
                            return (char)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            char[] dest = (char[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (specialOptimizeThinPatternsPowersOfTwo && meanByPowerOfTwoFunc && jaOfs == 0) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            long v = (sum + halfDivisor) >> logDivisor;
                                            dest[destArrayOffset] = (char)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[(int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[(int)i] & 0xFFFF;
                                        }
                                    } else if (linearFunc) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            double v = lfA * sum + lfB;
                                            dest[destArrayOffset] = (char)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFFFF;
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            double v = processingFunc.get(sum);
                                            dest[destArrayOffset] = (char)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFFFF;
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                double v = processingFunc.get(sum);
                                dest[destArrayOffset] = (char)v;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i] & 0xFFFF;
                                }
                            }
                        }
                    };
                } else { // char B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            double v = processingFunc.get(sum);
                            return (char)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum + lfB;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFFFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFFFF;
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFFFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFFFF;
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFFFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFFFF;
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFFFF;
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i] & 0xFFFF;
                                }
                            }
                        }
                    };
                }
            } else { // char C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getInt(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getInt(i);
                            }
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (src instanceof ByteArray) {
            final ByteArray a = (ByteArray)src;
            if (direct) {
                final byte[] ja = (byte[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (meanFunc && desiredType == ByteArray.class) { // byte A: direct, returning ByteArray
                    return new AbstractByteArray(src.length(), true, src) {
                        public int getByte(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFF; // &0xFF for preprocessing
                            }
                            double v = processingFunc.get(sum);
                            return (int)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            byte[] dest = (byte[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFF; // &0xFF for preprocessing
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (specialOptimizeThinPatternsPowersOfTwo && meanByPowerOfTwoFunc && jaOfs == 0) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            long v = (sum + halfDivisor) >> logDivisor;
                                            dest[destArrayOffset] = (byte)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[(int)i] & 0xFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[(int)i] & 0xFF;
                                        }
                                    } else if (linearFunc) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            double v = lfA * sum + lfB;
                                            dest[destArrayOffset] = (byte)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFF;
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            double v = processingFunc.get(sum);
                                            dest[destArrayOffset] = (byte)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFF;
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                double v = processingFunc.get(sum);
                                dest[destArrayOffset] = (byte)v;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i] & 0xFF;
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i] & 0xFF;
                                }
                            }
                        }
                    };
                } else { // byte B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFF; // &0xFF for preprocessing
                            }
                            double v = processingFunc.get(sum);
                            return (int)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFF; // &0xFF for preprocessing
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum + lfB;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFF;
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFF;
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFF;
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFF;
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i] & 0xFF;
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i] & 0xFF;
                                }
                            }
                        }
                    };
                }
            } else { // byte C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getInt(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getInt(i);
                            }
                        }
                    }
                };
            }
        }
        if (src instanceof ShortArray) {
            final ShortArray a = (ShortArray)src;
            if (direct) {
                final short[] ja = (short[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (meanFunc && desiredType == ShortArray.class) { // short A: direct, returning ShortArray
                    return new AbstractShortArray(src.length(), true, src) {
                        public int getShort(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            double v = processingFunc.get(sum);
                            return (int)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            short[] dest = (short[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (specialOptimizeThinPatternsPowersOfTwo && meanByPowerOfTwoFunc && jaOfs == 0) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            long v = (sum + halfDivisor) >> logDivisor;
                                            dest[destArrayOffset] = (short)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[(int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[(int)i] & 0xFFFF;
                                        }
                                    } else if (linearFunc) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            double v = lfA * sum + lfB;
                                            dest[destArrayOffset] = (short)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFFFF;
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            double v = processingFunc.get(sum);
                                            dest[destArrayOffset] = (short)v;
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFFFF;
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                double v = processingFunc.get(sum);
                                dest[destArrayOffset] = (short)v;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i] & 0xFFFF;
                                }
                            }
                        }
                    };
                } else { // short B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            double v = processingFunc.get(sum);
                            return (int)v;
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i] & 0xFFFF; // &0xFFFF for preprocessing
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum + lfB;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFFFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFFFF;
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFFFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFFFF;
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i] & 0xFFFF;
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i] & 0xFFFF;
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i] & 0xFFFF;
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i] & 0xFFFF;
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i] & 0xFFFF;
                                }
                            }
                        }
                    };
                }
            } else { // short C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getInt(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getInt(i);
                            }
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedEnd]]

        //[[Repeat() int\s+getInt     ==> long getLong;;
        //           getInt           ==> getLong;;
        //           Integer(?!Array) ==> Long;;
        //           IntArray         ==> LongArray;;
        //           int(\s+[ABCD]\:|\[\]\s+ja|\[\]\s+dest|\)processingFunc\b|\)\(lfA|\)\(\(double) ==> long$1;;
        //           \(int\[\]\)      ==> (long[]);;
        //           long\s+sum       ==> double sum;;
        //           \(double\)sum    ==> sum ]]
        if (src instanceof IntArray) {
            final IntArray a = (IntArray)src;
            if (direct) {// int A: direct
                final int[] ja = (int[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (desiredType == IntArray.class) { // int A: direct, returning IntArray
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return (int)processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            int[] dest = (int[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = (int)(lfA * sum + lfB);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = (int)processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = (int)processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                } else { // int B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            long sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    for (int k = 0; k < count; k++, destArrayOffset++) {
                                        dest[destArrayOffset] = processingFunc.get(sum);
                                        long i = arrayPos + leftInvShift;
                                        sum -= ja[jaOfs + (int)i];
                                        arrayPos++;
                                        i = arrayPos + rightInvShift;
                                        sum += ja[jaOfs + (int)i];
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                }
            } else { // int C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        long sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getInt(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getInt(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getInt(i);
                            }
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (src instanceof LongArray) {
            final LongArray a = (LongArray)src;
            if (direct) {// long A: direct
                final long[] ja = (long[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (desiredType == LongArray.class) { // long A: direct, returning LongArray
                    return new AbstractLongArray(src.length(), true, src) {
                        public long getLong(long index) {
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return (long)processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            long[] dest = (long[])destArray;
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc) {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = (long)(lfA * sum + lfB);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = (long)processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = (long)processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                } else { // long B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    for (int k = 0; k < count; k++, destArrayOffset++) {
                                        dest[destArrayOffset] = processingFunc.get(sum);
                                        long i = arrayPos + leftInvShift;
                                        sum -= ja[jaOfs + (int)i];
                                        arrayPos++;
                                        i = arrayPos + rightInvShift;
                                        sum += ja[jaOfs + (int)i];
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                }
            } else { // long C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double sum = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getLong(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        double sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getLong(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getLong(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getLong(i);
                            }
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedEnd]]

        //[[Repeat() FloatArray(\s+a|\)) ==> DoubleArray$1;;
        //           float(\s+[ABC]|\[\]\s*ja|\[\]\)\(\() ==> double$1 ]]
        if (src instanceof FloatArray) {
            final FloatArray a = (FloatArray)src;
            if (direct) {
                final float[] ja = (float[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (desiredType == FloatArray.class) { // float A: direct, returning FloatArray
                    return new AbstractFloatArray(src.length(), true, src) {
                        public strictfp float getFloat(long index) {
                            double sum = 0.0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return (float)processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            float[] dest = (float[])destArray;
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = (float)(lfA * sum + lfB);
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = (float)(lfA * sum);
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = (float)sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = (float)processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                } else { // float B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public strictfp double getDouble(long index) {
                            double sum = 0.0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum + lfB;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                }
            } else { // float C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public strictfp double getDouble(long index) {
                        double sum = 0.0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getDouble(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        double sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getDouble(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getDouble(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getDouble(i);
                            }
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (src instanceof DoubleArray) {
            final DoubleArray a = (DoubleArray)src;
            if (direct) {
                final double[] ja = (double[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                if (desiredType == FloatArray.class) { // double A: direct, returning FloatArray
                    return new AbstractFloatArray(src.length(), true, src) {
                        public strictfp float getFloat(long index) {
                            double sum = 0.0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return (float)processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            float[] dest = (float[])destArray;
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = (float)(lfA * sum + lfB);
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = (float)(lfA * sum);
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = (float)sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = (float)processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = (float)processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                } else { // double B: direct, returning DoubleArray
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public strictfp double getDouble(long index) {
                            double sum = 0.0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            return processingFunc.get(sum);
                        }

                        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                            if (!optimizeGetData) {
                                super.getData(arrayPos, destArray, destArrayOffset, count);
                                return;
                            }
                            if (destArray == null)
                                throw new NullPointerException("Null destArray argument");
                            checkRanges(length, arrayPos, count);
                            if (count == 0) {
                                return;
                            }
                            double[] dest = (double[])destArray;
                            double sum = 0;
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += ja[jaOfs + (int)i];
                            }
                            if (left.length == 1) {
                                long rightInvShift = left[0] == 0 ? 0 : length - left[0];
                                long leftInvShift = -right[0];
                                assert leftInvShift <= 0 && rightInvShift >= 0;
                                if (arrayPos + leftInvShift >= 0 && arrayPos < length - rightInvShift - count) {
                                    if (linearFunc && jaOfs == 0) {
                                        if (lfB != 0.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum + lfB;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else if (lfA != 1.0) {
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = lfA * sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        } else {
                                            assert lfA == 1.0 && lfB == 0.0;
                                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                                dest[destArrayOffset] = sum;
                                                long i = arrayPos + leftInvShift;
                                                sum -= ja[(int)i];
                                                arrayPos++;
                                                i = arrayPos + rightInvShift;
                                                sum += ja[(int)i];
                                            }
                                        }
                                    } else {
                                        for (int k = 0; k < count; k++, destArrayOffset++) {
                                            dest[destArrayOffset] = processingFunc.get(sum);
                                            long i = arrayPos + leftInvShift;
                                            sum -= ja[jaOfs + (int)i];
                                            arrayPos++;
                                            i = arrayPos + rightInvShift;
                                            sum += ja[jaOfs + (int)i];
                                        }
                                    }
                                    return;
                                }
                            }
                            for (int k = 0; k < count; k++, destArrayOffset++) {
                                dest[destArrayOffset] = processingFunc.get(sum);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum -= ja[jaOfs + (int)i];
                                }
                                arrayPos++;
                                if (arrayPos == length) {
                                    arrayPos = 0;
                                }
                                for (long shift : left) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    sum += ja[jaOfs + (int)i];
                                }
                            }
                        }
                    };
                }
            } else { // double C: indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public strictfp double getDouble(long index) {
                        double sum = 0.0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getDouble(i);
                        }
                        return processingFunc.get(sum);
                    }

                    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                        if (!optimizeGetData) {
                            super.getData(arrayPos, destArray, destArrayOffset, count);
                            return;
                        }
                        if (destArray == null)
                            throw new NullPointerException("Null destArray argument");
                        checkRanges(length, arrayPos, count);
                        if (count == 0) {
                            return;
                        }
                        double[] dest = (double[])destArray;
                        double sum = 0;
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            sum += a.getDouble(i);
                        }
                        for (int k = 0; k < count; k++, destArrayOffset++) {
                            dest[destArrayOffset] = processingFunc.get(sum);
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum -= a.getDouble(i);
                            }
                            arrayPos++;
                            if (arrayPos == length) {
                                arrayPos = 0;
                            }
                            for (long shift : left) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                sum += a.getDouble(i);
                            }
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new AssertionError("Illegal array type (" + src.getClass()
            + "): it must implement one of primitive XxxArray interfaces");
    }

    private static final int BIT_TYPE_CODE = 1;
    private static final int CHAR_TYPE_CODE = 2;
    private static final int BYTE_TYPE_CODE = 3;
    private static final int SHORT_TYPE_CODE = 4;
    private static final int INT_TYPE_CODE = 5;
    private static final int LONG_TYPE_CODE = 6;
    private static final int FLOAT_TYPE_CODE = 7;
    private static final int DOUBLE_TYPE_CODE = 8;

    private void processAlongAxis(UpdatablePArray dest, PArray src, long[] dimensions,
        final int coordIndex, final long min, final long max)
    {
        assert coordIndex > 0; // we don't try to optimize summarizing along x: the general algorithm works well here
        assert max - min + 1 <= Integer.MAX_VALUE;
        final int n = (int)(max - min + 1);
        final long length = src.length();
        if (length == 0) {
            return;
        }
        final boolean meanFunc = linearFunc && lfA >= 0.0 && lfA <= 1.0 / n && lfB >= 0.0 && lfB < 1.0;
        // in this case we can be sure that the linear function for byte will be <=255, for short <=65535
        final boolean meanByPowerOfTwoFunc = meanFunc && dividingByPowerOfTwoWithRoundingFunc;
        long layerSize = dimensions[0];
        for (int k = 1; k < coordIndex; k++) {
            layerSize *= dimensions[k];
        }
        long m = dimensions[coordIndex];
        for (int k = coordIndex + 1; k < dimensions.length; k++) {
            m *= dimensions[k];
        }
        assert m * layerSize == length;
        final long srcBitsPerElement = src.bitsPerElement();
        final Class<?> accElementType = srcBitsPerElement <= 16 && n <= Integer.MAX_VALUE >> srcBitsPerElement ?
            int.class : double.class;
        final boolean direct = optimizeDirectArrays
            && layerSize <= Integer.MAX_VALUE // necessary for allocation Java arrays
            && (double)layerSize * (Arrays.sizeOf(accElementType)
            + (src instanceof BitArray ? 1.0 : Arrays.sizeOf(src.elementType()))
            + (dest instanceof BitArray ? 1.0 : Arrays.sizeOf(dest.elementType())))
            < Arrays.SystemSettings.maxTempJavaMemory();
        double startPart = (double)n / ((double)m + (double)n);
        ArrayContext contextStart = contextPart(0.0, startPart);
        ArrayContext contextMain = contextPart(startPart, 1.0);
        if (direct) {
            final int len = (int)layerSize;
            assert len == layerSize;
            final int[] intAcc = accElementType == int.class ? new int[len] : null;
            final double[] doubleAcc = accElementType == double.class ? new double[len] : null;
            final int srcTypeCode;
            final Object srcBuffer;
            long elementCounter = 0;
            if (src instanceof BitArray) {
                srcTypeCode = BIT_TYPE_CODE;
                boolean[] srcBuf = new boolean[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                    for (int j = 0; j < len; j++) {
                        if (srcBuf[j]) {
                            intAcc[j]++;
                        }
                    }
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
                // The last command of the following Repeat instruction removes all lines with "intAcc" combination
                //[[Repeat() (\s*\&)\s*0xFFFF  ==> $1 0xFF,,$1 0xFFFF,, ,, ,, ,, ;;
                //           char              ==> byte,,short,,int,,long,,float,,double;;
                //           Char              ==> Byte,,Short,,Int,,Long,,Float,,Double;;
                //           CHAR              ==> BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
                //           ([ \t]*[^\n\r]*intAcc.*?(?:\r(?!\n)|\n|\r\n)) ==> $1,,$1,, ,, ...]]
            } else if (src instanceof CharArray) {
                srcTypeCode = CHAR_TYPE_CODE;
                char[] srcBuf = new char[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                    if (intAcc != null) {
                        for (int j = 0; j < len; j++) { // intAcc
                            intAcc[j] += srcBuf[j] & 0xFFFF;
                        } // intAcc
                    } else { // intAcc
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j] & 0xFFFF;
                        }
                    } // intAcc
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (src instanceof ByteArray) {
                srcTypeCode = BYTE_TYPE_CODE;
                byte[] srcBuf = new byte[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                    if (intAcc != null) {
                        for (int j = 0; j < len; j++) { // intAcc
                            intAcc[j] += srcBuf[j] & 0xFF;
                        } // intAcc
                    } else { // intAcc
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j] & 0xFF;
                        }
                    } // intAcc
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
            } else if (src instanceof ShortArray) {
                srcTypeCode = SHORT_TYPE_CODE;
                short[] srcBuf = new short[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                    if (intAcc != null) {
                        for (int j = 0; j < len; j++) { // intAcc
                            intAcc[j] += srcBuf[j] & 0xFFFF;
                        } // intAcc
                    } else { // intAcc
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j] & 0xFFFF;
                        }
                    } // intAcc
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
            } else if (src instanceof IntArray) {
                srcTypeCode = INT_TYPE_CODE;
                int[] srcBuf = new int[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j];
                        }
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
            } else if (src instanceof LongArray) {
                srcTypeCode = LONG_TYPE_CODE;
                long[] srcBuf = new long[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j];
                        }
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
            } else if (src instanceof FloatArray) {
                srcTypeCode = FLOAT_TYPE_CODE;
                float[] srcBuf = new float[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j];
                        }
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
            } else if (src instanceof DoubleArray) {
                srcTypeCode = DOUBLE_TYPE_CODE;
                double[] srcBuf = new double[len];
                srcBuffer = srcBuf;
                for (long shift = min; shift <= max; shift++) {
                    long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                    if (i < 0) {
                        i += length;
                    }
                    src.getData(i, srcBuf);
                        for (int j = 0; j < len; j++) {
                            doubleAcc[j] += srcBuf[j];
                        }
                    if ((elementCounter += len) > 65536 && contextStart != null) {
                        elementCounter = 0;
                        contextStart.checkInterruptionAndUpdateProgress(null,  shift - min + 1, n);
                    }
                }
                //[[Repeat.AutoGeneratedEnd]]

            } else
                throw new AssertionError("Illegal source array type (" + src.getClass()
                    + "): it must implement one of primitive XxxArray interfaces");

            final int destTypeCode;
            final Object destBuffer;
            if (dest instanceof BitArray) {
                destTypeCode = BIT_TYPE_CODE;
                destBuffer = new boolean[len];
            } else if (dest instanceof CharArray) {
                destTypeCode = CHAR_TYPE_CODE;
                destBuffer = new char[len];
            } else if (dest instanceof ByteArray) {
                destTypeCode = BYTE_TYPE_CODE;
                destBuffer = new byte[len];
            } else if (dest instanceof ShortArray) {
                destTypeCode = SHORT_TYPE_CODE;
                destBuffer = new short[len];
            } else if (dest instanceof IntArray) {
                destTypeCode = INT_TYPE_CODE;
                destBuffer = new int[len];
            } else if (dest instanceof LongArray) {
                destTypeCode = LONG_TYPE_CODE;
                destBuffer = new long[len];
            } else if (dest instanceof FloatArray) {
                destTypeCode = FLOAT_TYPE_CODE;
                destBuffer = new float[len];
            } else if (dest instanceof DoubleArray) {
                destTypeCode = DOUBLE_TYPE_CODE;
                destBuffer = new double[len];
            } else
                throw new AssertionError("Illegal destination array type (" + dest.getClass()
                    + "): it must implement one of primitive XxxArray interfaces");

            for (long arrayPos = 0; ; ) {
                switch (destTypeCode) {
                    case BIT_TYPE_CODE: {
                        boolean[] destBuf = (boolean[])destBuffer;
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) {
                                destBuf[j] = processingFunc.get(intAcc[j]) != 0.0;
                            }
                        } else {
                            for (int j = 0; j < len; j++) {
                                destBuf[j] = processingFunc.get(doubleAcc[j]) != 0.0;
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }

                    case CHAR_TYPE_CODE: {
                        char[] destBuf = (char[])destBuffer;
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) {
                                int v = (int)processingFunc.get(intAcc[j]);
                                destBuf[j] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        } else {
                            for (int j = 0; j < len; j++) {
                                int v = (int)processingFunc.get(doubleAcc[j]);
                                destBuf[j] = v < Character.MIN_VALUE ? Character.MIN_VALUE :
                                    v > Character.MAX_VALUE ? Character.MAX_VALUE :
                                        (char)v;
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }

                    //[[Repeat() byte ==> short;;
                    //           BYTE ==> SHORT;;
                    //           0xFF ==> 0xFFFF]]
                    case BYTE_TYPE_CODE: {
                        byte[] destBuf = (byte[])destBuffer;
                        if (intAcc != null) {
                            if (specialOptimizeThinPatternsPowersOfTwo && meanByPowerOfTwoFunc) {
                                for (int j = 0; j < len; j++) {
                                    long v = (intAcc[j] + halfDivisor) >> logDivisor;
                                    destBuf[j] = (byte)v;
                                }
                            } else if (meanFunc) {
                                for (int j = 0; j < len; j++) {
                                    int v = (int)(lfA * intAcc[j] + lfB);
                                    destBuf[j] = (byte)v;
                                }
                            } else if (linearFunc) {
                                if (lfA == 1.0 && lfB == 0.0) {
                                    for (int j = 0; j < len; j++) {
                                        int v = intAcc[j];
                                        destBuf[j] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        int v = (int)(lfA * intAcc[j] + lfB);
                                        destBuf[j] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                    }
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    int v = (int)processingFunc.get(intAcc[j]);
                                    destBuf[j] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                                }
                            }
                        } else {
                            for (int j = 0; j < len; j++) {
                                int v = (int)processingFunc.get(doubleAcc[j]);
                                destBuf[j] = (byte)(v < 0 ? 0 : v > 0xFF ? 0xFF : v);
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case SHORT_TYPE_CODE: {
                        short[] destBuf = (short[])destBuffer;
                        if (intAcc != null) {
                            if (specialOptimizeThinPatternsPowersOfTwo && meanByPowerOfTwoFunc) {
                                for (int j = 0; j < len; j++) {
                                    long v = (intAcc[j] + halfDivisor) >> logDivisor;
                                    destBuf[j] = (short)v;
                                }
                            } else if (meanFunc) {
                                for (int j = 0; j < len; j++) {
                                    int v = (int)(lfA * intAcc[j] + lfB);
                                    destBuf[j] = (short)v;
                                }
                            } else if (linearFunc) {
                                if (lfA == 1.0 && lfB == 0.0) {
                                    for (int j = 0; j < len; j++) {
                                        int v = intAcc[j];
                                        destBuf[j] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        int v = (int)(lfA * intAcc[j] + lfB);
                                        destBuf[j] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                    }
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    int v = (int)processingFunc.get(intAcc[j]);
                                    destBuf[j] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                                }
                            }
                        } else {
                            for (int j = 0; j < len; j++) {
                                int v = (int)processingFunc.get(doubleAcc[j]);
                                destBuf[j] = (short)(v < 0 ? 0 : v > 0xFFFF ? 0xFFFF : v);
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]

                    case INT_TYPE_CODE: {
                        int[] destBuf = (int[])destBuffer;
                        if (linearFunc) {
                            if (lfB != 0.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (int)(lfA * intAcc[j] + lfB);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (int)(lfA * doubleAcc[j] + lfB);
                                    }
                                }
                            } else if (lfA != 1.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (int)(lfA * intAcc[j]);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (int)(lfA * doubleAcc[j]);
                                    }
                                }
                            } else {
                                assert lfA == 1.0 && lfB == 0.0;
                                if (intAcc != null) {
                                    dest.setData(arrayPos, intAcc);
                                    break;
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (int)doubleAcc[j];
                                    }
                                }
                            }
                        } else {
                            if (intAcc != null) {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = (int)processingFunc.get(intAcc[j]);
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = (int)processingFunc.get(doubleAcc[j]);
                                }
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }

                    //[[Repeat() long(?!Buf)  ==> float;;
                    //           LONG         ==> FLOAT;;
                    //           \(double\)   ==> ]]
                    case LONG_TYPE_CODE: {
                        long[] destBuf = (long[])destBuffer;
                        if (linearFunc) {
                            if (lfB != 0.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (long)(lfA * intAcc[j] + lfB);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (long)(lfA * doubleAcc[j] + lfB);
                                    }
                                }
                            } else if (lfA != 1.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (long)(lfA * intAcc[j]);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (long)(lfA * doubleAcc[j]);
                                    }
                                }
                            } else {
                                assert lfA == 1.0 && lfB == 0.0;
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = intAcc[j];
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (long)doubleAcc[j];
                                    }
                                }
                            }
                        } else {
                            if (intAcc != null) {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = (long)processingFunc.get(intAcc[j]);
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = (long)processingFunc.get(doubleAcc[j]);
                                }
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case FLOAT_TYPE_CODE: {
                        float[] destBuf = (float[])destBuffer;
                        if (linearFunc) {
                            if (lfB != 0.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (float)(lfA * intAcc[j] + lfB);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (float)(lfA * doubleAcc[j] + lfB);
                                    }
                                }
                            } else if (lfA != 1.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (float)(lfA * intAcc[j]);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (float)(lfA * doubleAcc[j]);
                                    }
                                }
                            } else {
                                assert lfA == 1.0 && lfB == 0.0;
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = intAcc[j];
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (float)doubleAcc[j];
                                    }
                                }
                            }
                        } else {
                            if (intAcc != null) {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = (float)processingFunc.get(intAcc[j]);
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = (float)processingFunc.get(doubleAcc[j]);
                                }
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]

                    case DOUBLE_TYPE_CODE: {
                        double[] destBuf = (double[])destBuffer;
                        if (linearFunc) {
                            if (lfB != 0.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (lfA * intAcc[j] + lfB);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (lfA * doubleAcc[j] + lfB);
                                    }
                                }
                            } else if (lfA != 1.0) {
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (lfA * intAcc[j]);
                                    }
                                } else {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = (lfA * doubleAcc[j]);
                                    }
                                }
                            } else {
                                assert lfA == 1.0 && lfB == 0.0;
                                if (intAcc != null) {
                                    for (int j = 0; j < len; j++) {
                                        destBuf[j] = intAcc[j];
                                    }
                                } else {
                                    dest.setData(arrayPos, doubleAcc);
                                    break;
                                }
                            }
                        } else {
                            if (intAcc != null) {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = processingFunc.get(intAcc[j]);
                                }
                            } else {
                                for (int j = 0; j < len; j++) {
                                    destBuf[j] = processingFunc.get(doubleAcc[j]);
                                }
                            }
                        }
                        dest.setData(arrayPos, destBuf);
                        break;
                    }

                    default: {
                        throw new InternalError("Cannot occur");
                    }
                }
                if (arrayPos + layerSize == length) {
                    break;
                }
                long i1 = (arrayPos - max * layerSize) % length;
                if (i1 < 0) {
                    i1 += length;
                }
                arrayPos += layerSize;
                assert arrayPos <= length - layerSize;
                long i2 = (arrayPos - min * layerSize) % length;
                if (i2 < 0) {
                    i2 += length;
                }

                switch (srcTypeCode) {
                    case BIT_TYPE_CODE: {
                        boolean[] srcBuf = (boolean[])srcBuffer;
                        src.getData(i1, srcBuf);
                        for (int j = 0; j < len; j++) {
                            if (srcBuf[j]) {
                                intAcc[j]--;
                            }
                        }
                        src.getData(i2, srcBuf);
                        for (int j = 0; j < len; j++) {
                            if (srcBuf[j]) {
                                intAcc[j]++;
                            }
                        }
                        break;
                    }

                    // The last command of the following Repeat instruction removes all lines with "intAcc" combination
                    //[[Repeat() (\s*\&)\s*0xFFFF  ==> $1 0xFF,,$1 0xFFFF,, ,, ,, ,, ;;
                    //           char              ==> byte,,short,,int,,long,,float,,double;;
                    //           CHAR              ==> BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
                    //           ([ \t]*[^\n\r]*intAcc.*?(?:\r(?!\n)|\n|\r\n)) ==> $1,,$1,, ,, ...]]
                    case CHAR_TYPE_CODE: {
                        char[] srcBuf = (char[])srcBuffer;
                        src.getData(i1, srcBuf);
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) { // intAcc
                                intAcc[j] -= srcBuf[j] & 0xFFFF;
                            } // intAcc
                        } else { // intAcc
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j] & 0xFFFF;
                            }
                        } // intAcc
                        src.getData(i2, srcBuf);
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) { // intAcc
                                intAcc[j] += srcBuf[j] & 0xFFFF;
                            } // intAcc
                        } else { // intAcc
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j] & 0xFFFF;
                            }
                        } // intAcc
                        break;
                    }
                    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    case BYTE_TYPE_CODE: {
                        byte[] srcBuf = (byte[])srcBuffer;
                        src.getData(i1, srcBuf);
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) { // intAcc
                                intAcc[j] -= srcBuf[j] & 0xFF;
                            } // intAcc
                        } else { // intAcc
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j] & 0xFF;
                            }
                        } // intAcc
                        src.getData(i2, srcBuf);
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) { // intAcc
                                intAcc[j] += srcBuf[j] & 0xFF;
                            } // intAcc
                        } else { // intAcc
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j] & 0xFF;
                            }
                        } // intAcc
                        break;
                    }
                    case SHORT_TYPE_CODE: {
                        short[] srcBuf = (short[])srcBuffer;
                        src.getData(i1, srcBuf);
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) { // intAcc
                                intAcc[j] -= srcBuf[j] & 0xFFFF;
                            } // intAcc
                        } else { // intAcc
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j] & 0xFFFF;
                            }
                        } // intAcc
                        src.getData(i2, srcBuf);
                        if (intAcc != null) {
                            for (int j = 0; j < len; j++) { // intAcc
                                intAcc[j] += srcBuf[j] & 0xFFFF;
                            } // intAcc
                        } else { // intAcc
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j] & 0xFFFF;
                            }
                        } // intAcc
                        break;
                    }
                    case INT_TYPE_CODE: {
                        int[] srcBuf = (int[])srcBuffer;
                        src.getData(i1, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j];
                            }
                        src.getData(i2, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j];
                            }
                        break;
                    }
                    case LONG_TYPE_CODE: {
                        long[] srcBuf = (long[])srcBuffer;
                        src.getData(i1, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j];
                            }
                        src.getData(i2, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j];
                            }
                        break;
                    }
                    case FLOAT_TYPE_CODE: {
                        float[] srcBuf = (float[])srcBuffer;
                        src.getData(i1, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j];
                            }
                        src.getData(i2, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j];
                            }
                        break;
                    }
                    case DOUBLE_TYPE_CODE: {
                        double[] srcBuf = (double[])srcBuffer;
                        src.getData(i1, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] -= srcBuf[j];
                            }
                        src.getData(i2, srcBuf);
                            for (int j = 0; j < len; j++) {
                                doubleAcc[j] += srcBuf[j];
                            }
                        break;
                    }
                    //[[Repeat.AutoGeneratedEnd]]
                    default: {
                        throw new InternalError("Impossible switch case");
                    }
                }
                if ((elementCounter += len) > 65536 && contextMain != null) {
                    elementCounter = 0;
                    contextMain.checkInterruptionAndUpdateProgress(null, arrayPos, length);
                }
            }

        } else { // not direct
            // here we are sure that layers are large, so multiprocessing can be useful: we shall not disable it
            final Func updater = LinearFunc.getInstance(0.0, 1.0, -1.0, 1.0);
            UpdatablePArray acc = (UpdatablePArray) memoryModel().newUnresizableArray(accElementType, layerSize);
            for (long shift = min; shift <= max; shift++) {
                long i = (-shift * layerSize) % length; // "% length", because min..max can be negative or large
                if (i < 0) {
                    i += length;
                }
                Arrays.applyFunc(contextStart == null ? null : contextStart.part(shift - min, shift - min + 1, n),
                    false, Func.X_PLUS_Y, acc, acc, (PArray)src.subArr(i, layerSize));
            }
            for (long arrayPos = 0; ; ) {
                long halfPos = arrayPos + layerSize / 2;
                Arrays.applyFunc(contextMain == null ? null : contextMain.part(arrayPos, halfPos, length),
                    true, processingFunc, dest.subArr(arrayPos, layerSize), acc);
                if (arrayPos + layerSize == length) {
                    break;
                }
                long i1 = (arrayPos - max * layerSize) % length;
                if (i1 < 0) {
                    i1 += length;
                }
                arrayPos += layerSize;
                assert arrayPos <= length - layerSize;
                long i2 = (arrayPos - min * layerSize) % length;
                if (i2 < 0) {
                    i2 += length;
                }
                Arrays.applyFunc(contextMain == null ? null : contextMain.part(halfPos, arrayPos, length),
                    false, updater, acc,
                    acc,
                    (PArray)src.subArr(i1, layerSize),
                    (PArray)src.subArr(i2, layerSize));
            }
        }
        if (contextMain != null) {
            contextMain.checkInterruptionAndUpdateProgress(accElementType, length, length);
        }
    }

    private static void checkRanges(long length, long arrayPos, int count) {
        if (count < 0)
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        if (arrayPos < 0)
            throw new IndexOutOfBoundsException("arrayPos = " + arrayPos + " < 0");
        if (arrayPos > length - count)
            throw new IndexOutOfBoundsException("arrayPos+count = " + arrayPos + "+" + count + " > length=" + length);
    }
}
