/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

class AveragerBetweenValues extends RankOperationProcessor {
    private static final boolean DEBUG_MODE = false; // thorough checking getData

    private final boolean optimizeGetData = OPTIMIZE_GET_DATA;
    private final boolean optimizeDirectArrays = OPTIMIZE_DIRECT_ARRAYS;
    private final boolean inlineOneLevel = INLINE_ONE_LEVEL;
    private final double filler;
    private final boolean fillUsual, fillMin, fillMax, fillNearest;
    private final boolean interpolated;

    AveragerBetweenValues(ArrayContext context, double filler, boolean interpolated, int[] bitLevels) {
        super(context, bitLevels);
        this.filler = filler;
        this.fillMin = filler == RankMorphology.FILL_MIN_VALUE;
        this.fillMax = filler == RankMorphology.FILL_MAX_VALUE;
        this.fillNearest = Double.isNaN(filler); // RankMorphology.FILL_NEAREST_VALUE;
        this.fillUsual = !fillMin && !fillMax && !fillNearest;
        this.interpolated = interpolated;
    }

    @Override
    PArray asProcessed(Class<? extends PArray> desiredType, PArray src, PArray[] additional,
        long[] dimensions, final long[] shifts, final long[] left, final long[] right)
    {
        if (additional.length < 2)
            throw new IllegalArgumentException("Two additional matrices are required (percentile indexes)");
        assert shifts.length > 0;
        assert left.length == right.length;
        final boolean direct = optimizeDirectArrays &&
            src instanceof DirectAccessible && ((DirectAccessible)src).hasJavaArray();
        final PArray minV = additional[0];
        final ArrayPool ap1 =
            optimizeDirectArrays && (Arrays.isNCopies(minV) || SimpleMemoryModel.isSimpleArray(minV)) ? null :
                ArrayPool.getInstance(Arrays.SMM, minV.elementType(), BUFFER_BLOCK_SIZE);
        final PArray maxV = additional[1];
        final ArrayPool ap2 =
            optimizeDirectArrays && (Arrays.isNCopies(maxV) || SimpleMemoryModel.isSimpleArray(maxV)) ? null :
                ArrayPool.getInstance(Arrays.SMM, maxV.elementType(), BUFFER_BLOCK_SIZE);
        if (src instanceof BitArray) { // in the bit case, the interpolated integral is the same as the simple one
            final BitArray a = (BitArray)src;
            final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
            return new AbstractDoubleArray(src.length(), true, src) {
                public double getDouble(long index) {
                    double v1 = minV.getDouble(index);
                    checkNaN(v1);
                    double v2 = maxV.getDouble(index);
                    checkNaN(v2);
                    if (v1 < 0.0) {
                        v1 = 0.0;
                    }
                    if (v2 > 2.0) {
                        v2 = 2.0;
                    }
                    if (v2 <= v1) {
                        return fillMin ? v1 : fillMax ? v2 : fillNearest ? 0.5 * (v1 + v2) : filler;
                    }
                    int b = 0;
                    for (long shift : shifts) {
                        long i = index - shift;
                        if (i < 0) {
                            i += length;
                        }
                        if (!a.getBit(i)) {
                            b++;
                        }
                    }
                    if (v2 <= 1.0) {
                        if (b == 0) { // it is a left bound
                            return fillMin ? v1 : fillMax ? v2 : fillNearest ? v2 : filler;
                        }
                        return 0.5 * (v1 + v2);
                    } else if (v1 >= 1.0) {
                        if (b == shifts.length) { // it is a right bound
                            return fillMin ? v1 : fillMax ? v2 : fillNearest ? v1 : filler;
                        }
                        return 0.5 * (v1 + v2);
                    } else {
                        assert v1 < 1.0 && 1.0 < v2;
                        double n1 = b == 0 ? 0.0 : (1.0 - v1) * (double)b;
                        double i1 = b == 0 ? 0.0 : 0.5 * (v1 + 1.0) * n1;
                        double n2 = b == shifts.length ? 0.0 : (v2 - 1.0) * (double)(shifts.length - b);
                        double i2 = b == shifts.length ? 0.0 : 0.5 * (1.0 + v2) * n2;
                        return (i1 + i2) / (n1 + n2);
                    }
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
                    UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                    UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                    final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                    int[] hist = histogramCache.get(arrayPos);
                    if (hist == null) {
                        hist = new int[1];
                        for (long shift : shifts) {
                            long i = arrayPos - shift;
                            if (i < 0) {
                                i += length;
                            }
                            if (!a.getBit(i)) {
                                hist[0]++;
                            }
                        }
                    }
                    for (; count > 0; count -= bufLen) {
                        final int len = Math.min(bufLen, count);
                        if (ap1 != null) {
                            buf1.copy(minV.subArr(arrayPos, len));
                        }
                        if (ap2 != null) {
                            buf2.copy(maxV.subArr(arrayPos, len));
                        }
                        for (int k = 0; k < len; k++, destArrayOffset++) {
                            double v1 = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                            checkNaN(v1);
                            double v2 = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                            checkNaN(v2);
                            if (v1 < 0.0) {
                                v1 = 0.0;
                            }
                            if (v2 > 2.0) {
                                v2 = 2.0;
                            }
                            double w = fillMin ? v1 : fillMax ? v2 : fillNearest ? 0.5 * (v1 + v2) : filler;
                            if (v2 > v1) {
                                final int b = hist[0];
                                if (v2 <= 1.0) {
                                    if (b == 0) { // it is a left bound
                                        if (fillNearest) {
                                            w = v2;
                                        }
                                    } else {
                                        w = 0.5 * (v1 + v2);
                                    }
                                } else if (v1 >= 1.0) {
                                    if (b == shifts.length) { // it is a right bound
                                        if (fillNearest) {
                                            w = v1;
                                        }
                                    } else {
                                        w = 0.5 * (v1 + v2);
                                    }
                                } else {
                                    assert v1 < 1.0 && 1.0 < v2;
                                    double n1 = b == 0 ? 0.0 : (1.0 - v1) * (double)b;
                                    double i1 = b == 0 ? 0.0 : 0.5 * (v1 + 1.0) * n1;
                                    double n2 = b == shifts.length ? 0.0 : (v2 - 1.0) * (double)(shifts.length - b);
                                    double i2 = b == shifts.length ? 0.0 : 0.5 * (1.0 + v2) * n2;
                                    w = (i1 + i2) / (n1 + n2);
                                }
                            }
                            dest[destArrayOffset] = w;
                            for (long shift : right) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                if (!a.getBit(i)) {
                                    hist[0]--;
                                    assert hist[0] >= 0 : "Unbalanced 0 and 1 bits";
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
                                if (!a.getBit(i)) {
                                    hist[0]++;
                                }
                            }
                        }
                    }
                    histogramCache.put(arrayPos, hist);
                    if (ap2 != null) {
                        ap2.releaseArray(buf2);
                    }
                    if (ap1 != null) {
                        ap1.releaseArray(buf1);
                    }
                }
            };
        }

        //[[Repeat() 0xFFFF          ==> 0xFF,,0xFFFF;;
        //           char            ==> byte,,short;;
        //           Char            ==> Byte,,Short;;
        //           16              ==> 8,,16 ]]
        if (src instanceof CharArray) {
            final CharArray a = (CharArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 16);
            final int bs = 16 - nab;
            final double multiplierInv = 1 << bs;
            final double multiplier = 1.0 / multiplierInv;
            if (direct) {
                final char[] ja = (char[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // char A: simple, direct, without SummingHistogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            int[] hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = new int[1 << nab];
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                                }
                            }
                            int currentIValue1 = 0;
                            int currentIRank1 = 0; // number of elements less than currentIValue1
                            long currentSum1 = 0; // sum of values < currentIValue1
                            int currentIValue2 = 0;
                            int currentIRank2 = 0; // number of elements less than currentIValue2
                            long currentSum2 = 0; // sum of values < currentIValue2
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double value1 = minValue * multiplier;
                                    double value2 = maxValue * multiplier;
                                    if (value1 < 0.0) {
                                        value1 = 0.0;
                                    }
                                    if (value2 > hist.length) {
                                        value2 = hist.length;
                                    }
                                    if (value2 > value1) {
                                        assert 0.0 <= value1 && value2 <= hist.length;
                                        // Implementation of hist1.moveToValue(value1);
                                        final int v1 = (int)value1;
                                        if (v1 < currentIValue1) {
                                            for (int j = currentIValue1 - 1; j >= v1; j--) {
                                                int b = hist[j];
                                                currentIRank1 -= b;
                                                currentSum1 -= (long)b * (long)j;
                                            }
                                            assert currentIRank1 >= 0;
                                        } else if (v1 > currentIValue1) {
                                            for (int j = currentIValue1; j < v1; j++) {
                                                int b = hist[j];
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)j;
                                            }
                                            assert currentIRank1 <= shifts.length;
                                        }
                                        currentIValue1 = v1;

                                        // Implementation of hist2.moveToValue(value2);
                                        final int v2 = (int)value2;
                                        if (v2 < currentIValue2) {
                                            for (int j = currentIValue2 - 1; j >= v2; j--) {
                                                int b = hist[j];
                                                currentIRank2 -= b;
                                                currentSum2 -= (long)b * (long)j;
                                            }
                                            assert currentIRank2 >= 0;
                                        } else if (v2 > currentIValue2) {
                                            for (int j = currentIValue2; j < v2; j++) {
                                                int b = hist[j];
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)j;
                                            }
                                            assert currentIRank2 <= shifts.length;
                                        }
                                        currentIValue2 = v2;

                                        final double rank1, rank2;
                                        final double correction1, correction2;
                                        double d;
                                        int b1 = 0;
                                        if (v1 == hist.length || (b1 = hist[v1]) == 0 || (d = value1 - v1) == 0.0) {
                                            rank1 = currentIRank1;
                                            correction1 = 0.0;
                                        } else {
                                            double indexInBar = b1 == 1 ? d : d * (double)b1;
                                            rank1 = currentIRank1 + indexInBar;
                                            correction1 = indexInBar * (v1 + 0.5 * d);
                                        }
                                        int b2;
                                        if (v2 == hist.length || (b2 = hist[v2]) == 0 || (d = value2 - v2) == 0.0) {
                                            rank2 = currentIRank2;
                                            correction2 = 0.0;
                                        } else {
                                            double indexInBar = b2 == 1 ? d : d * (double)b2;
                                            rank2 = currentIRank2 + indexInBar;
                                            correction2 = indexInBar * (v2 + 0.5 * d);
                                        }

                                        double i = currentSum2 - currentSum1 + 0.5 * (currentIRank2 - currentIRank1)
                                            + (correction2 - correction1);
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i);
                                        double n = rank2 - rank1;
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + rank2 + " - " + rank1);
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (currentIRank2 == 0) {
                                                // hist2.leftFromOrAtBoundOfNonZeroPart()
                                                w = maxValue;
                                            } else if (currentIRank1 + b1 == shifts.length) {
                                                // hist1.rightFromOrAtBoundOfNonZeroPart()
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (long shift : right) {
                                        long i = arrayPos - shift;
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int value = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                        if (--hist[value] < 0) {
                                            throw new AssertionError("Disbalance in the histogram: negative number "
                                                + hist[value] + " of occurrences of " + value + " value");
                                        }
                                        if (value < currentIValue1) {
                                            --currentIRank1;
                                            currentSum1 -= value;
                                        }
                                        if (value < currentIValue2) {
                                            --currentIRank2;
                                            currentSum2 -= value;
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
                                        int value = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                        ++hist[value];
                                        if (value < currentIValue1) {
                                            ++currentIRank1;
                                            currentSum1 += value;
                                        }
                                        if (value < currentIValue2) {
                                            ++currentIRank2;
                                            currentSum2 += value;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else if (!interpolated) { // char B: simple, direct
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // char C: precise, direct
                    assert interpolated;
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // char D: simple, indirect
                final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist1.include(a.getInt(i) >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.exclude(a.getInt(i) >> bs);
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
                                    hist1.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // char E: precise, indirect
                assert interpolated;
                final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist1.include(a.getInt(i) >> bs); // &0xFFFF for preprocessing
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.exclude(a.getInt(i) >> bs);
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
                                    hist1.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (src instanceof ByteArray) {
            final ByteArray a = (ByteArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 8);
            final int bs = 8 - nab;
            final double multiplierInv = 1 << bs;
            final double multiplier = 1.0 / multiplierInv;
            if (direct) {
                final byte[] ja = (byte[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // byte A: simple, direct, without SummingHistogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            int[] hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = new int[1 << nab];
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                                }
                            }
                            int currentIValue1 = 0;
                            int currentIRank1 = 0; // number of elements less than currentIValue1
                            long currentSum1 = 0; // sum of values < currentIValue1
                            int currentIValue2 = 0;
                            int currentIRank2 = 0; // number of elements less than currentIValue2
                            long currentSum2 = 0; // sum of values < currentIValue2
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double value1 = minValue * multiplier;
                                    double value2 = maxValue * multiplier;
                                    if (value1 < 0.0) {
                                        value1 = 0.0;
                                    }
                                    if (value2 > hist.length) {
                                        value2 = hist.length;
                                    }
                                    if (value2 > value1) {
                                        assert 0.0 <= value1 && value2 <= hist.length;
                                        // Implementation of hist1.moveToValue(value1);
                                        final int v1 = (int)value1;
                                        if (v1 < currentIValue1) {
                                            for (int j = currentIValue1 - 1; j >= v1; j--) {
                                                int b = hist[j];
                                                currentIRank1 -= b;
                                                currentSum1 -= (long)b * (long)j;
                                            }
                                            assert currentIRank1 >= 0;
                                        } else if (v1 > currentIValue1) {
                                            for (int j = currentIValue1; j < v1; j++) {
                                                int b = hist[j];
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)j;
                                            }
                                            assert currentIRank1 <= shifts.length;
                                        }
                                        currentIValue1 = v1;

                                        // Implementation of hist2.moveToValue(value2);
                                        final int v2 = (int)value2;
                                        if (v2 < currentIValue2) {
                                            for (int j = currentIValue2 - 1; j >= v2; j--) {
                                                int b = hist[j];
                                                currentIRank2 -= b;
                                                currentSum2 -= (long)b * (long)j;
                                            }
                                            assert currentIRank2 >= 0;
                                        } else if (v2 > currentIValue2) {
                                            for (int j = currentIValue2; j < v2; j++) {
                                                int b = hist[j];
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)j;
                                            }
                                            assert currentIRank2 <= shifts.length;
                                        }
                                        currentIValue2 = v2;

                                        final double rank1, rank2;
                                        final double correction1, correction2;
                                        double d;
                                        int b1 = 0;
                                        if (v1 == hist.length || (b1 = hist[v1]) == 0 || (d = value1 - v1) == 0.0) {
                                            rank1 = currentIRank1;
                                            correction1 = 0.0;
                                        } else {
                                            double indexInBar = b1 == 1 ? d : d * (double)b1;
                                            rank1 = currentIRank1 + indexInBar;
                                            correction1 = indexInBar * (v1 + 0.5 * d);
                                        }
                                        int b2;
                                        if (v2 == hist.length || (b2 = hist[v2]) == 0 || (d = value2 - v2) == 0.0) {
                                            rank2 = currentIRank2;
                                            correction2 = 0.0;
                                        } else {
                                            double indexInBar = b2 == 1 ? d : d * (double)b2;
                                            rank2 = currentIRank2 + indexInBar;
                                            correction2 = indexInBar * (v2 + 0.5 * d);
                                        }

                                        double i = currentSum2 - currentSum1 + 0.5 * (currentIRank2 - currentIRank1)
                                            + (correction2 - correction1);
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i);
                                        double n = rank2 - rank1;
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + rank2 + " - " + rank1);
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (currentIRank2 == 0) {
                                                // hist2.leftFromOrAtBoundOfNonZeroPart()
                                                w = maxValue;
                                            } else if (currentIRank1 + b1 == shifts.length) {
                                                // hist1.rightFromOrAtBoundOfNonZeroPart()
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (long shift : right) {
                                        long i = arrayPos - shift;
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int value = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                        if (--hist[value] < 0) {
                                            throw new AssertionError("Disbalance in the histogram: negative number "
                                                + hist[value] + " of occurrences of " + value + " value");
                                        }
                                        if (value < currentIValue1) {
                                            --currentIRank1;
                                            currentSum1 -= value;
                                        }
                                        if (value < currentIValue2) {
                                            --currentIRank2;
                                            currentSum2 -= value;
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
                                        int value = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                        ++hist[value];
                                        if (value < currentIValue1) {
                                            ++currentIRank1;
                                            currentSum1 += value;
                                        }
                                        if (value < currentIValue2) {
                                            ++currentIRank2;
                                            currentSum2 += value;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else if (!interpolated) { // byte B: simple, direct
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.include((ja[jaOfs + (int)i] & 0xFF) >> bs); // &0xFF for preprocessing
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // byte C: precise, direct
                    assert interpolated;
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.include((ja[jaOfs + (int)i] & 0xFF) >> bs); // &0xFF for preprocessing
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // byte D: simple, indirect
                final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist1.include(a.getInt(i) >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.exclude(a.getInt(i) >> bs);
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
                                    hist1.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // byte E: precise, indirect
                assert interpolated;
                final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist1.include(a.getInt(i) >> bs); // &0xFF for preprocessing
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.exclude(a.getInt(i) >> bs);
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
                                    hist1.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            }
        }
        if (src instanceof ShortArray) {
            final ShortArray a = (ShortArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 16);
            final int bs = 16 - nab;
            final double multiplierInv = 1 << bs;
            final double multiplier = 1.0 / multiplierInv;
            if (direct) {
                final short[] ja = (short[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // short A: simple, direct, without SummingHistogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            int[] hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = new int[1 << nab];
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                                }
                            }
                            int currentIValue1 = 0;
                            int currentIRank1 = 0; // number of elements less than currentIValue1
                            long currentSum1 = 0; // sum of values < currentIValue1
                            int currentIValue2 = 0;
                            int currentIRank2 = 0; // number of elements less than currentIValue2
                            long currentSum2 = 0; // sum of values < currentIValue2
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double value1 = minValue * multiplier;
                                    double value2 = maxValue * multiplier;
                                    if (value1 < 0.0) {
                                        value1 = 0.0;
                                    }
                                    if (value2 > hist.length) {
                                        value2 = hist.length;
                                    }
                                    if (value2 > value1) {
                                        assert 0.0 <= value1 && value2 <= hist.length;
                                        // Implementation of hist1.moveToValue(value1);
                                        final int v1 = (int)value1;
                                        if (v1 < currentIValue1) {
                                            for (int j = currentIValue1 - 1; j >= v1; j--) {
                                                int b = hist[j];
                                                currentIRank1 -= b;
                                                currentSum1 -= (long)b * (long)j;
                                            }
                                            assert currentIRank1 >= 0;
                                        } else if (v1 > currentIValue1) {
                                            for (int j = currentIValue1; j < v1; j++) {
                                                int b = hist[j];
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)j;
                                            }
                                            assert currentIRank1 <= shifts.length;
                                        }
                                        currentIValue1 = v1;

                                        // Implementation of hist2.moveToValue(value2);
                                        final int v2 = (int)value2;
                                        if (v2 < currentIValue2) {
                                            for (int j = currentIValue2 - 1; j >= v2; j--) {
                                                int b = hist[j];
                                                currentIRank2 -= b;
                                                currentSum2 -= (long)b * (long)j;
                                            }
                                            assert currentIRank2 >= 0;
                                        } else if (v2 > currentIValue2) {
                                            for (int j = currentIValue2; j < v2; j++) {
                                                int b = hist[j];
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)j;
                                            }
                                            assert currentIRank2 <= shifts.length;
                                        }
                                        currentIValue2 = v2;

                                        final double rank1, rank2;
                                        final double correction1, correction2;
                                        double d;
                                        int b1 = 0;
                                        if (v1 == hist.length || (b1 = hist[v1]) == 0 || (d = value1 - v1) == 0.0) {
                                            rank1 = currentIRank1;
                                            correction1 = 0.0;
                                        } else {
                                            double indexInBar = b1 == 1 ? d : d * (double)b1;
                                            rank1 = currentIRank1 + indexInBar;
                                            correction1 = indexInBar * (v1 + 0.5 * d);
                                        }
                                        int b2;
                                        if (v2 == hist.length || (b2 = hist[v2]) == 0 || (d = value2 - v2) == 0.0) {
                                            rank2 = currentIRank2;
                                            correction2 = 0.0;
                                        } else {
                                            double indexInBar = b2 == 1 ? d : d * (double)b2;
                                            rank2 = currentIRank2 + indexInBar;
                                            correction2 = indexInBar * (v2 + 0.5 * d);
                                        }

                                        double i = currentSum2 - currentSum1 + 0.5 * (currentIRank2 - currentIRank1)
                                            + (correction2 - correction1);
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i);
                                        double n = rank2 - rank1;
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + rank2 + " - " + rank1);
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (currentIRank2 == 0) {
                                                // hist2.leftFromOrAtBoundOfNonZeroPart()
                                                w = maxValue;
                                            } else if (currentIRank1 + b1 == shifts.length) {
                                                // hist1.rightFromOrAtBoundOfNonZeroPart()
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (long shift : right) {
                                        long i = arrayPos - shift;
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int value = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                        if (--hist[value] < 0) {
                                            throw new AssertionError("Disbalance in the histogram: negative number "
                                                + hist[value] + " of occurrences of " + value + " value");
                                        }
                                        if (value < currentIValue1) {
                                            --currentIRank1;
                                            currentSum1 -= value;
                                        }
                                        if (value < currentIValue2) {
                                            --currentIRank2;
                                            currentSum2 -= value;
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
                                        int value = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                        ++hist[value];
                                        if (value < currentIValue1) {
                                            ++currentIRank1;
                                            currentSum1 += value;
                                        }
                                        if (value < currentIValue2) {
                                            ++currentIRank2;
                                            currentSum2 += value;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else if (!interpolated) { // short B: simple, direct
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // short C: precise, direct
                    assert interpolated;
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // short D: simple, indirect
                final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist1.include(a.getInt(i) >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.exclude(a.getInt(i) >> bs);
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
                                    hist1.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // short E: precise, indirect
                assert interpolated;
                final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist1.include(a.getInt(i) >> bs); // &0xFFFF for preprocessing
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist1.exclude(a.getInt(i) >> bs);
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
                                    hist1.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
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
        //           int(\s+[ABCD]\:|\[\]\s+ja|\[\]\s+dest|\s+v\b|\s+p\b) ==> long$1;;
        //           \(int\[\]\)(?!indexesPool) ==> (long[]);;
        //           (v\s*\>\>\s*bs)  ==> (int)($1);;
        //           31               ==> 63 ]]
        if (src instanceof IntArray) {
            final IntArray a = (IntArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 30);
            final int bs = 31 - nab;
            final double multiplierInv = 1L << bs; // 1L necessary in LongArray branch
            final double multiplier = 1.0 / multiplierInv;
            final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
            if (direct) {
                final int[] ja = (int[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // int A: simple, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                hist[v >> bs]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include(v >> bs);
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = v >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = v >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // int B: precise, direct
                    assert interpolated;
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                hist[v >> bs]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include(v >> bs);
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = v >> bs;
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        int v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = v >> bs;
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // int C: simple, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int v = a.getInt(i);
                            if (v < 0) {
                                v = 0;
                            }
                            hist[v >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = a.getInt(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist1.include(v >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = a.getInt(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.exclude(v >> bs);
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
                                    int v = a.getInt(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include(v >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // int D: precise, indirect
                assert interpolated;
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int v = a.getInt(i);
                            if (v < 0) {
                                v = 0;
                            }
                            hist[v >> bs]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = a.getInt(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist1.include(v >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = a.getInt(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.exclude(v >> bs);
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
                                    int v = a.getInt(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include(v >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (src instanceof LongArray) {
            final LongArray a = (LongArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 30);
            final int bs = 63 - nab;
            final double multiplierInv = 1L << bs; // 1L necessary in LongArray branch
            final double multiplier = 1.0 / multiplierInv;
            final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
            if (direct) {
                final long[] ja = (long[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // long A: simple, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                hist[(int)(v >> bs)]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include((int)(v >> bs));
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        long v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = (int)(v >> bs);
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        long v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = (int)(v >> bs);
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // long B: precise, direct
                    assert interpolated;
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                hist[(int)(v >> bs)]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include((int)(v >> bs));
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        long v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = (int)(v >> bs);
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        long v = ja[jaOfs + (int)i];
                                        if (v < 0) {
                                            v = 0;
                                        }
                                        barIndexes[j] = (int)(v >> bs);
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // long C: simple, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            long v = a.getLong(i);
                            if (v < 0) {
                                v = 0;
                            }
                            hist[(int)(v >> bs)]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = a.getLong(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist1.include((int)(v >> bs));
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = a.getLong(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.exclude((int)(v >> bs));
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
                                    long v = a.getLong(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include((int)(v >> bs));
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // long D: precise, indirect
                assert interpolated;
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            long v = a.getLong(i);
                            if (v < 0) {
                                v = 0;
                            }
                            hist[(int)(v >> bs)]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(1 << nab, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = a.getLong(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist1.include((int)(v >> bs));
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = a.getLong(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.exclude((int)(v >> bs));
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
                                    long v = a.getLong(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist1.include((int)(v >> bs));
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedEnd]]

        //[[Repeat() \(float\) ==> ;;
        //           float ==> double;;
        //           Float ==> Double;;
        //           \.0f ==> .0]]
        if (src instanceof FloatArray) {
            final FloatArray a = (FloatArray)src;
            final int histLength = 1 << numberOfAnalyzedBits;
            final double multiplier = histLength - 1; // only strict 1.0 is transformed to histLength-1
            final double multiplierInv = 1.0 / multiplier;
            final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
            if (direct) {
                final float[] ja = (float[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // float A: simple, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist[histIndex]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(histLength, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // float B: precise, direct
                    assert interpolated;
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist[histIndex]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(histLength, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // float C: simple, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[histLength];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            hist[histIndex]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(histLength, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist1.include(histIndex);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.exclude(histIndex);
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
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // float D: precise, direct
                assert interpolated;
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[histLength];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            hist[histIndex]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(histLength, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist1.include(histIndex);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.exclude(histIndex);
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
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (src instanceof DoubleArray) {
            final DoubleArray a = (DoubleArray)src;
            final int histLength = 1 << numberOfAnalyzedBits;
            final double multiplier = histLength - 1; // only strict 1.0 is transformed to histLength-1
            final double multiplierInv = 1.0 / multiplier;
            final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
            if (direct) {
                final double[] ja = (double[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // double A: simple, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist[histIndex]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(histLength, true, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        double n = hist2.currentRank() - hist1.currentRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentRank() + " - " + hist1.currentRank());
                                        if (DEBUG_MODE) {
                                            debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                } else { // double B: precise, direct
                    assert interpolated;
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double minValue = minV.getDouble(index);
                            checkNaN(minValue);
                            double maxValue = maxV.getDouble(index);
                            checkNaN(maxValue);
                            double v1 = minValue * multiplier;
                            double v2 = maxValue * multiplier;
                            if (v2 <= v1) {
                                return fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                            }
                            int[] hist = new int[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist[histIndex]++;
                            }
                            SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                            double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                            double n = c.count();
                            if (n <= 0.0) {
                                return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                    c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                        0.5 * (minValue + maxValue);
                            }
                            return (i / n) * multiplierInv;
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
                            UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                            UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                            final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                            SummingHistogram hist1 = histogramCache.get(arrayPos);
                            if (hist1 == null) {
                                hist1 = SummingHistogram.newSummingIntHistogram(histLength, false, bitLevels);
                                hist1.share();
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                            SummingHistogram hist2 = hist1.nextSharing();
                            assert hist1.shareCount() == 2;
                            assert hist2.shareCount() == 2;
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap1 != null) {
                                    buf1.copy(minV.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(maxV.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                    checkNaN(minValue);
                                    double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                    checkNaN(maxValue);
                                    double w = fillMin ? minValue : fillMax ? maxValue :
                                        fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                    double v1 = minValue * multiplier;
                                    double v2 = maxValue * multiplier;
                                    if (v2 > v1) {
                                        hist1.moveToValue(v1);
                                        hist2.moveToValue(v2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < -1.0e-5)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral());
                                        double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                        if (n < -1.0e-5)
                                            throw new AssertionError("Negative rank difference= " + n + " = " +
                                                + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                        if (DEBUG_MODE) {
                                            debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                        }
                                        if (n > 0.0) {
                                            w = (i / n) * multiplierInv;
                                        } else if (fillNearest) {
                                            if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                                w = maxValue;
                                            } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                                w = minValue;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = w;
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.exclude(barIndexes);
                                    arrayPos++;
                                    if (arrayPos == length) {
                                        arrayPos = 0;
                                    }
                                    for (int j = 0; j < left.length; j++) {
                                        long i = arrayPos - left[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist1.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist1);
                            if (ap2 != null) {
                                ap2.releaseArray(buf2);
                            }
                            if (ap1 != null) {
                                ap1.releaseArray(buf1);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // double C: simple, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[histLength];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            hist[histIndex]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.integralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(histLength, true, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist1.include(histIndex);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    double n = hist2.currentRank() - hist1.currentRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentRank() + " - " + hist1.currentRank());
                                    if (DEBUG_MODE) {
                                        debugIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.exclude(histIndex);
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
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            } else { // double D: precise, direct
                assert interpolated;
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double minValue = minV.getDouble(index);
                        checkNaN(minValue);
                        double maxValue = maxV.getDouble(index);
                        checkNaN(maxValue);
                        double v1 = minValue * multiplier;
                        double v2 = maxValue * multiplier;
                        if (v2 <= v1) {
                            return fillMin ? minValue : fillMax ? maxValue :
                                fillNearest ? 0.5 * (minValue + maxValue) : filler;
                        }
                        int[] hist = new int[histLength];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            hist[histIndex]++;
                        }
                        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
                        double i = SummingHistogram.preciseIntegralBetweenValues(hist, v1, v2, c);
                        double n = c.count();
                        if (n <= 0.0) {
                            return fillUsual ? filler : fillMin ? minValue : fillMax ? maxValue :
                                c.isLeftBound() ? maxValue : c.isRightBound() ? minValue :
                                    0.5 * (minValue + maxValue);
                        }
                        return (i / n) * multiplierInv;
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
                        UpdatablePArray buf1 = ap1 == null ? null : (UpdatablePArray)ap1.requestArray();
                        UpdatablePArray buf2 = ap2 == null ? null : (UpdatablePArray)ap2.requestArray();
                        final int bufLen = ap1 == null && ap2 == null ? count : BUFFER_BLOCK_SIZE;
                        SummingHistogram hist1 = histogramCache.get(arrayPos);
                        if (hist1 == null) {
                            hist1 = SummingHistogram.newSummingIntHistogram(histLength, false, bitLevels);
                            hist1.share();
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist1.include(histIndex);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(minV.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(maxV.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double minValue = ap1 != null ? buf1.getDouble(k) : minV.getDouble(arrayPos);
                                checkNaN(minValue);
                                double maxValue = ap2 != null ? buf2.getDouble(k) : maxV.getDouble(arrayPos);
                                checkNaN(maxValue);
                                double w = fillMin ? minValue : fillMax ? maxValue :
                                    fillNearest ? 0.5 * (minValue + maxValue) : filler;
                                double v1 = minValue * multiplier;
                                double v2 = maxValue * multiplier;
                                if (v2 > v1) {
                                    hist1.moveToValue(v1);
                                    hist2.moveToValue(v2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < -1.0e-5)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentPreciseIntegral() + " - "
                                            + hist1.currentPreciseIntegral());
                                    double n = hist2.currentPreciseRank() - hist1.currentPreciseRank();
                                    if (n < -1.0e-5)
                                        throw new AssertionError("Negative rank difference= " + n + " = " +
                                            + hist2.currentPreciseRank() + " - " + hist1.currentPreciseRank());
                                    if (DEBUG_MODE) {
                                        debugPreciseIntegral(arrayPos, v1, v2, hist1, hist2, n, i);
                                    }
                                    if (n > 0.0) {
                                        w = (i / n) * multiplierInv;
                                    } else if (fillNearest) {
                                        if (hist2.leftFromOrAtBoundOfNonZeroPart()) {
                                            w = maxValue;
                                        } else if (hist1.rightFromOrAtBoundOfNonZeroPart()) {
                                            w = minValue;
                                        }
                                    }
                                }
                                dest[destArrayOffset] = w;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.exclude(histIndex);
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
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist1.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist1);
                        if (ap2 != null) {
                            ap2.releaseArray(buf2);
                        }
                        if (ap1 != null) {
                            ap1.releaseArray(buf1);
                        }
                    }
                };
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new AssertionError("Illegal array type (" + src.getClass()
            + "): it must implement one of primitive XxxArray interfaces");
    }

    private static void checkRanges(long length, long arrayPos, int count) {
        if (count < 0)
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        if (arrayPos < 0)
            throw new IndexOutOfBoundsException("arrayPos = " + arrayPos + " < 0");
        if (arrayPos > length - count)
            throw new IndexOutOfBoundsException("arrayPos+count = " + arrayPos + "+" + count + " > length=" + length);
    }

    private static void checkNaN(double rank) {
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal value (NaN) in some elements of minValues or maxValues");
    }

    private static void debugIntegral(long arrayPos, double v1, double v2,
        SummingHistogram hist1, SummingHistogram hist2, double n, double i)
    {
        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
        assert !c.isInitialized();
        if (Math.abs(i - SummingHistogram.integralBetweenValues(hist1.bars(), v1, v2, c)) > 0.01)
            throw new AssertionError("Bug: at index " + arrayPos
                + ", integral between " + v1 + " and " + v2 + " is " + i + " = "
                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                + " instead of " + SummingHistogram.integralBetweenValues(hist1.bars(), v1, v2, c)
                + ", histogram is " + JArrays.toString(hist1.bars(), ",", 2048));
        if (Math.abs(c.count() - n) > 0.001)
            throw new AssertionError("Bug: at index " + arrayPos
                + ", rank difference between " + v1 + " and " + v2 + " is " + n + " = "
                + hist2.currentRank() + "-" + hist1.currentRank()
                + " instead of " + c.count()
                + ", integral = " + SummingHistogram.integralBetweenValues(hist1.bars(), v1, v2, c)
                + ", histogram is " + JArrays.toString(hist1.bars(), ",", 2048));
        if (n <= 0 &&
            (c.isRightBound() != hist1.rightFromOrAtBoundOfNonZeroPart()
                || c.isLeftBound() != hist2.leftFromOrAtBoundOfNonZeroPart()))
            throw new AssertionError("Bug: at index " + arrayPos
                + ", between " + v1 + " and " + v2
                + ", left bound = " + hist2.leftFromOrAtBoundOfNonZeroPart()
                + " and right bound = " + hist1.rightFromOrAtBoundOfNonZeroPart()
                + " instead of " + c
                + ", integral = " + SummingHistogram.integralBetweenValues(hist1.bars(), v1, v2, c)
                + ", histogram is " + JArrays.toString(hist1.bars(), ",", 2048));
    }

    private static void debugPreciseIntegral(long arrayPos, double v1, double v2,
        SummingHistogram hist1, SummingHistogram hist2, double n, double i)
    {
        SummingHistogram.CountOfValues c = new SummingHistogram.CountOfValues();
        assert !c.isInitialized();
        if (Math.abs(i - SummingHistogram.preciseIntegralBetweenValues(hist1.bars(), v1, v2, c)) > 0.01)
            throw new AssertionError("Bug: at index " + arrayPos
                + ", integral between " + v1 + " and " + v2 + " is " + i + " = "
                + hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                + " instead of " + SummingHistogram.preciseIntegralBetweenValues(hist1.bars(), v1, v2, c)
                + ", histogram is " + JArrays.toString(hist1.bars(), ",", 2048));
        if (Math.abs(c.count() - n) > 0.001)
            throw new AssertionError("Bug: at index " + arrayPos
                + ", rank difference between " + v1 + " and " + v2 + " is " + n + " = "
                + hist2.currentPreciseRank() + "-" + hist1.currentPreciseRank()
                + " instead of " + c.count()
                + ", precise integral = " + SummingHistogram.preciseIntegralBetweenValues(hist1.bars(), v1, v2, c)
                + ", histogram is " + JArrays.toString(hist1.bars(), ",", 2048));
        if (n <= 0 &&
            (c.isRightBound() != hist1.rightFromOrAtBoundOfNonZeroPart()
                || c.isLeftBound() != hist2.leftFromOrAtBoundOfNonZeroPart()))
            throw new AssertionError("Bug: at index " + arrayPos
                + ", between " + v1 + " and " + v2
                + ", left bound = " + hist2.leftFromOrAtBoundOfNonZeroPart()
                + " and right bound = " + hist1.rightFromOrAtBoundOfNonZeroPart()
                + " instead of " + c
                + ", precise integral = " + SummingHistogram.preciseIntegralBetweenValues(hist1.bars(), v1, v2, c)
                + ", histogram is " + JArrays.toString(hist1.bars(), ",", 2048));
    }
}
