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

package net.algart.matrices.morphology;

import net.algart.arrays.*;

class AveragerBetweenPercentiles extends RankOperationProcessor {
    private static final boolean DEBUG_MODE = false; // thorough checking getData

    private final boolean optimizeGetData = OPTIMIZE_GET_DATA;
    private final boolean optimizeDirectArrays = OPTIMIZE_DIRECT_ARRAYS;
    private final boolean inlineOneLevel = INLINE_ONE_LEVEL;
    private final double filler;
    private final boolean interpolated;

    AveragerBetweenPercentiles(ArrayContext context, double filler, boolean interpolated, int[] bitLevels) {
        super(context, bitLevels);
        this.filler = filler;
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
        final PArray fPerc1 = additional[0];
        final ArrayPool ap1 =
            optimizeDirectArrays && (Arrays.isNCopies(fPerc1) || SimpleMemoryModel.isSimpleArray(fPerc1)) ? null :
                ArrayPool.getInstance(Arrays.SMM, fPerc1.elementType(), BUFFER_BLOCK_SIZE);
        final PArray fPerc2 = additional[1];
        final ArrayPool ap2 =
            optimizeDirectArrays && (Arrays.isNCopies(fPerc2) || SimpleMemoryModel.isSimpleArray(fPerc2)) ? null :
                ArrayPool.getInstance(Arrays.SMM, fPerc2.elementType(), BUFFER_BLOCK_SIZE);
        if (src instanceof BitArray) { // in the bit case, the interpolated integral is the same as the simple one
            final BitArray a = (BitArray)src;
            final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
            return new AbstractDoubleArray(src.length(), true, src) {
                public double getDouble(long index) {
                    double pIndex1 = fPerc1.getDouble(index);
                    checkNaN(pIndex1);
                    double pIndex2 = fPerc2.getDouble(index);
                    checkNaN(pIndex2);
                    final double n = pIndex2 - pIndex1; // before changing pIndex1 and pIndex2!
                    if (n <= 0.0) {
                        return filler;
                    }
                    final int r1;
                    if (pIndex1 < 0) {
                        pIndex1 = r1 = 0;
                    } else if (pIndex1 > shifts.length) {
                        pIndex1 = r1 = shifts.length;
                    } else {
                        r1 = (int)pIndex1;
                    }
                    final int r2;
                    if (pIndex2 < 0) {
                        pIndex2 = r2 = 0;
                    } else if (pIndex2 > shifts.length) {
                        pIndex2 = r2 = shifts.length;
                    } else {
                        r2 = (int)pIndex2;
                    }
                    assert 0 <= r1 && r1 <= r2 && r2 <= shifts.length;
                    if (r1 == shifts.length || pIndex2 == 0.0) { // not r2 here: it can be 0 for small nonzero pIndex2
                        assert pIndex1 == pIndex2;
                        return 0.0;
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
                    final double i;
                    if (pIndex2 <= b) {
                        assert b > 0;
                        i = (pIndex2 - pIndex1) * 0.5 * (pIndex1 + pIndex2) / b;
                    } else if (r1 >= b) {
                        assert b < shifts.length;
                        i = (pIndex2 - pIndex1) * (1.0 + (0.5 * (pIndex1 + pIndex2) - b) / (shifts.length - b));
                    } else {
                        assert b > 0; // else r1>=b
                        assert b < shifts.length; // else pIndex2<=b
                        i = (b - pIndex1) * 0.5 * (pIndex1 + b) / b
                            + (pIndex2 - b) * (1.0 + 0.5 * (pIndex2 - b) / (shifts.length - b));
                    }
                    return i / n;
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
                            buf1.copy(fPerc1.subArr(arrayPos, len));
                        }
                        if (ap2 != null) {
                            buf2.copy(fPerc2.subArr(arrayPos, len));
                        }
                        for (int k = 0; k < len; k++, destArrayOffset++) {
                            double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                            checkNaN(pIndex1);
                            double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                            checkNaN(pIndex2);
                            double w = filler;
                            final double n = pIndex2 - pIndex1; // before changing pIndex1 and pIndex2!
                            if (n > 0.0) {
                                final int r1;
                                if (pIndex1 < 0) {
                                    pIndex1 = r1 = 0;
                                } else if (pIndex1 > shifts.length) {
                                    pIndex1 = r1 = shifts.length;
                                } else {
                                    r1 = (int)pIndex1;
                                }
                                final int r2;
                                if (pIndex2 < 0) {
                                    pIndex2 = r2 = 0;
                                } else if (pIndex2 > shifts.length) {
                                    pIndex2 = r2 = shifts.length;
                                } else {
                                    r2 = (int)pIndex2;
                                }
                                assert 0 <= r1 && r1 <= r2 && r2 <= shifts.length;
                                final int b = hist[0];
                                final double i;
                                if (r1 == shifts.length || pIndex2 == 0.0) { // not r2 here
                                    assert pIndex1 == pIndex2;
                                    i = 0.0;
                                } else if (pIndex2 <= b) {
                                    assert b > 0;
                                    i = (pIndex2 - pIndex1) * 0.5 * (pIndex1 + pIndex2) / b;
                                } else if (r1 >= b) {
                                    assert b < shifts.length;
                                    i = (pIndex2 - pIndex1)
                                        * (1.0 + (0.5 * (pIndex1 + pIndex2) - b) / (shifts.length - b));
                                } else {
                                    assert b > 0; // else r1>=b
                                    assert b < shifts.length; // else pIndex2<=b
                                    i = (b - pIndex1) * 0.5 * (pIndex1 + b) / b
                                        + (pIndex2 - b) * (1.0 + 0.5 * (pIndex2 - b) / (shifts.length - b));
                                }
                                w = i / n;
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
            if (direct) {
                final char[] ja = (char[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // char A: simple, direct, without SummingHistogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    checkNaN(pIndex1);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    checkNaN(pIndex2);
                                    double w = filler;
                                    final double n = pIndex2 - pIndex1; // before changing pIndex1 and pIndex2!
                                    if (n > 0.0) {
                                        // Implementation of hist1.moveToRank((int)pIndex1):
                                        final int r1;
                                        if (pIndex1 < 0) {
                                            pIndex1 = r1 = 0;
                                        } else if (pIndex1 > shifts.length) {
                                            pIndex1 = r1 = shifts.length;
                                        } else {
                                            r1 = (int)pIndex1;
                                        }
                                        if (r1 < currentIRank1) {
                                            do {
                                                --currentIValue1;
                                                int b = hist[currentIValue1];
                                                currentIRank1 -= b;
                                                currentSum1 -= (long)b * (long)currentIValue1;
                                            } while (r1 < currentIRank1);
                                            assert currentIRank1 >= 0;
                                        } else if (r1 < shifts.length) {
                                            int b = hist[currentIValue1];
                                            while (r1 >= currentIRank1 + b) {
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)currentIValue1;
                                                ++currentIValue1;
                                                b = hist[currentIValue1];
                                            }
                                            assert currentIRank1 < shifts.length;
                                        } else if (currentIRank1 == shifts.length) {
                                            // special decreasing branch: r1 == shifts.length
                                            assert currentIValue1 == hist.length || hist[currentIValue1] == 0;
                                            assert currentIValue1 > 0;
                                            while (hist[currentIValue1 - 1] == 0) {
                                                --currentIValue1;
                                                assert currentIValue1 > 0;
                                            }
                                        } else {
                                            // special increasing branch: r1 == shifts.length
                                            assert currentIRank1 < shifts.length;
                                            do {
                                                int b = hist[currentIValue1];
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)currentIValue1;
                                                ++currentIValue1;
                                            } while (currentIRank1 < shifts.length);
                                            assert currentIRank1 == shifts.length;
                                        }

                                        // Implementation of hist2.moveToRank((int)pIndex2):
                                        final int r2;
                                        if (pIndex2 < 0) {
                                            pIndex2 = r2 = 0;
                                        } else if (pIndex2 > shifts.length) {
                                            pIndex2 = r2 = shifts.length;
                                        } else {
                                            r2 = (int)pIndex2;
                                        }
                                        if (r2 < currentIRank2) {
                                            do {
                                                --currentIValue2;
                                                int b = hist[currentIValue2];
                                                currentIRank2 -= b;
                                                currentSum2 -= (long)b * (long)currentIValue2;
                                            } while (r2 < currentIRank2);
                                            assert currentIRank2 >= 0;
                                        } else if (r2 < shifts.length) {
                                            int b = hist[currentIValue2];
                                            while (r2 >= currentIRank2 + b) {
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)currentIValue2;
                                                ++currentIValue2;
                                                b = hist[currentIValue2];
                                            }
                                            assert currentIRank2 < shifts.length;
                                        } else if (currentIRank2 == shifts.length) {
                                            // special decreasing branch: r2 == shifts.length
                                            assert currentIValue2 == hist.length || hist[currentIValue2] == 0;
                                            assert currentIValue2 > 0;
                                            while (hist[currentIValue2 - 1] == 0) {
                                                --currentIValue2;
                                                assert currentIValue2 > 0;
                                            }
                                        } else {
                                            // special increasing branch: r2 == shifts.length
                                            assert currentIRank2 < shifts.length;
                                            do {
                                                int b = hist[currentIValue2];
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)currentIValue2;
                                                ++currentIValue2;
                                            } while (currentIRank2 < shifts.length);
                                            assert currentIRank2 == shifts.length;
                                        }

                                        double i1 = currentSum1 + 0.5 * currentIRank1;
                                        if (pIndex1 != currentIRank1) {
                                            assert currentIRank1 < pIndex1;
                                            int b = hist[currentIValue1];
                                            assert b > 0;
                                            double delta = (pIndex1 - currentIRank1) / (double)b;
                                            i1 += (pIndex1 - currentIRank1) * (currentIValue1 + 0.5 * delta);
                                        }
                                        double i2 = currentSum2 + 0.5 * currentIRank2;
                                        if (pIndex2 != currentIRank2) {
                                            assert currentIRank2 < pIndex2;
                                            int b = hist[currentIValue2];
                                            assert b > 0;
                                            double delta = (pIndex2 - currentIRank2) / (double)b;
                                            i2 += (pIndex2 - currentIRank2) * (currentIValue2 + 0.5 * delta);
                                        }
                                        double i = i2 - i1;
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i);
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist, pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist, pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist, ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                hist1.include(a.getInt(i) >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
            if (direct) {
                final byte[] ja = (byte[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // byte A: simple, direct, without SummingHistogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    checkNaN(pIndex1);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    checkNaN(pIndex2);
                                    double w = filler;
                                    final double n = pIndex2 - pIndex1; // before changing pIndex1 and pIndex2!
                                    if (n > 0.0) {
                                        // Implementation of hist1.moveToRank((int)pIndex1):
                                        final int r1;
                                        if (pIndex1 < 0) {
                                            pIndex1 = r1 = 0;
                                        } else if (pIndex1 > shifts.length) {
                                            pIndex1 = r1 = shifts.length;
                                        } else {
                                            r1 = (int)pIndex1;
                                        }
                                        if (r1 < currentIRank1) {
                                            do {
                                                --currentIValue1;
                                                int b = hist[currentIValue1];
                                                currentIRank1 -= b;
                                                currentSum1 -= (long)b * (long)currentIValue1;
                                            } while (r1 < currentIRank1);
                                            assert currentIRank1 >= 0;
                                        } else if (r1 < shifts.length) {
                                            int b = hist[currentIValue1];
                                            while (r1 >= currentIRank1 + b) {
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)currentIValue1;
                                                ++currentIValue1;
                                                b = hist[currentIValue1];
                                            }
                                            assert currentIRank1 < shifts.length;
                                        } else if (currentIRank1 == shifts.length) {
                                            // special decreasing branch: r1 == shifts.length
                                            assert currentIValue1 == hist.length || hist[currentIValue1] == 0;
                                            assert currentIValue1 > 0;
                                            while (hist[currentIValue1 - 1] == 0) {
                                                --currentIValue1;
                                                assert currentIValue1 > 0;
                                            }
                                        } else {
                                            // special increasing branch: r1 == shifts.length
                                            assert currentIRank1 < shifts.length;
                                            do {
                                                int b = hist[currentIValue1];
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)currentIValue1;
                                                ++currentIValue1;
                                            } while (currentIRank1 < shifts.length);
                                            assert currentIRank1 == shifts.length;
                                        }

                                        // Implementation of hist2.moveToRank((int)pIndex2):
                                        final int r2;
                                        if (pIndex2 < 0) {
                                            pIndex2 = r2 = 0;
                                        } else if (pIndex2 > shifts.length) {
                                            pIndex2 = r2 = shifts.length;
                                        } else {
                                            r2 = (int)pIndex2;
                                        }
                                        if (r2 < currentIRank2) {
                                            do {
                                                --currentIValue2;
                                                int b = hist[currentIValue2];
                                                currentIRank2 -= b;
                                                currentSum2 -= (long)b * (long)currentIValue2;
                                            } while (r2 < currentIRank2);
                                            assert currentIRank2 >= 0;
                                        } else if (r2 < shifts.length) {
                                            int b = hist[currentIValue2];
                                            while (r2 >= currentIRank2 + b) {
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)currentIValue2;
                                                ++currentIValue2;
                                                b = hist[currentIValue2];
                                            }
                                            assert currentIRank2 < shifts.length;
                                        } else if (currentIRank2 == shifts.length) {
                                            // special decreasing branch: r2 == shifts.length
                                            assert currentIValue2 == hist.length || hist[currentIValue2] == 0;
                                            assert currentIValue2 > 0;
                                            while (hist[currentIValue2 - 1] == 0) {
                                                --currentIValue2;
                                                assert currentIValue2 > 0;
                                            }
                                        } else {
                                            // special increasing branch: r2 == shifts.length
                                            assert currentIRank2 < shifts.length;
                                            do {
                                                int b = hist[currentIValue2];
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)currentIValue2;
                                                ++currentIValue2;
                                            } while (currentIRank2 < shifts.length);
                                            assert currentIRank2 == shifts.length;
                                        }

                                        double i1 = currentSum1 + 0.5 * currentIRank1;
                                        if (pIndex1 != currentIRank1) {
                                            assert currentIRank1 < pIndex1;
                                            int b = hist[currentIValue1];
                                            assert b > 0;
                                            double delta = (pIndex1 - currentIRank1) / (double)b;
                                            i1 += (pIndex1 - currentIRank1) * (currentIValue1 + 0.5 * delta);
                                        }
                                        double i2 = currentSum2 + 0.5 * currentIRank2;
                                        if (pIndex2 != currentIRank2) {
                                            assert currentIRank2 < pIndex2;
                                            int b = hist[currentIValue2];
                                            assert b > 0;
                                            double delta = (pIndex2 - currentIRank2) / (double)b;
                                            i2 += (pIndex2 - currentIRank2) * (currentIValue2 + 0.5 * delta);
                                        }
                                        double i = i2 - i1;
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i);
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist, pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist, pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist, ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                hist1.include(a.getInt(i) >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
            if (direct) {
                final short[] ja = (short[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // short A: simple, direct, without SummingHistogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    checkNaN(pIndex1);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    checkNaN(pIndex2);
                                    double w = filler;
                                    final double n = pIndex2 - pIndex1; // before changing pIndex1 and pIndex2!
                                    if (n > 0.0) {
                                        // Implementation of hist1.moveToRank((int)pIndex1):
                                        final int r1;
                                        if (pIndex1 < 0) {
                                            pIndex1 = r1 = 0;
                                        } else if (pIndex1 > shifts.length) {
                                            pIndex1 = r1 = shifts.length;
                                        } else {
                                            r1 = (int)pIndex1;
                                        }
                                        if (r1 < currentIRank1) {
                                            do {
                                                --currentIValue1;
                                                int b = hist[currentIValue1];
                                                currentIRank1 -= b;
                                                currentSum1 -= (long)b * (long)currentIValue1;
                                            } while (r1 < currentIRank1);
                                            assert currentIRank1 >= 0;
                                        } else if (r1 < shifts.length) {
                                            int b = hist[currentIValue1];
                                            while (r1 >= currentIRank1 + b) {
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)currentIValue1;
                                                ++currentIValue1;
                                                b = hist[currentIValue1];
                                            }
                                            assert currentIRank1 < shifts.length;
                                        } else if (currentIRank1 == shifts.length) {
                                            // special decreasing branch: r1 == shifts.length
                                            assert currentIValue1 == hist.length || hist[currentIValue1] == 0;
                                            assert currentIValue1 > 0;
                                            while (hist[currentIValue1 - 1] == 0) {
                                                --currentIValue1;
                                                assert currentIValue1 > 0;
                                            }
                                        } else {
                                            // special increasing branch: r1 == shifts.length
                                            assert currentIRank1 < shifts.length;
                                            do {
                                                int b = hist[currentIValue1];
                                                currentIRank1 += b;
                                                currentSum1 += (long)b * (long)currentIValue1;
                                                ++currentIValue1;
                                            } while (currentIRank1 < shifts.length);
                                            assert currentIRank1 == shifts.length;
                                        }

                                        // Implementation of hist2.moveToRank((int)pIndex2):
                                        final int r2;
                                        if (pIndex2 < 0) {
                                            pIndex2 = r2 = 0;
                                        } else if (pIndex2 > shifts.length) {
                                            pIndex2 = r2 = shifts.length;
                                        } else {
                                            r2 = (int)pIndex2;
                                        }
                                        if (r2 < currentIRank2) {
                                            do {
                                                --currentIValue2;
                                                int b = hist[currentIValue2];
                                                currentIRank2 -= b;
                                                currentSum2 -= (long)b * (long)currentIValue2;
                                            } while (r2 < currentIRank2);
                                            assert currentIRank2 >= 0;
                                        } else if (r2 < shifts.length) {
                                            int b = hist[currentIValue2];
                                            while (r2 >= currentIRank2 + b) {
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)currentIValue2;
                                                ++currentIValue2;
                                                b = hist[currentIValue2];
                                            }
                                            assert currentIRank2 < shifts.length;
                                        } else if (currentIRank2 == shifts.length) {
                                            // special decreasing branch: r2 == shifts.length
                                            assert currentIValue2 == hist.length || hist[currentIValue2] == 0;
                                            assert currentIValue2 > 0;
                                            while (hist[currentIValue2 - 1] == 0) {
                                                --currentIValue2;
                                                assert currentIValue2 > 0;
                                            }
                                        } else {
                                            // special increasing branch: r2 == shifts.length
                                            assert currentIRank2 < shifts.length;
                                            do {
                                                int b = hist[currentIValue2];
                                                currentIRank2 += b;
                                                currentSum2 += (long)b * (long)currentIValue2;
                                                ++currentIValue2;
                                            } while (currentIRank2 < shifts.length);
                                            assert currentIRank2 == shifts.length;
                                        }

                                        double i1 = currentSum1 + 0.5 * currentIRank1;
                                        if (pIndex1 != currentIRank1) {
                                            assert currentIRank1 < pIndex1;
                                            int b = hist[currentIValue1];
                                            assert b > 0;
                                            double delta = (pIndex1 - currentIRank1) / (double)b;
                                            i1 += (pIndex1 - currentIRank1) * (currentIValue1 + 0.5 * delta);
                                        }
                                        double i2 = currentSum2 + 0.5 * currentIRank2;
                                        if (pIndex2 != currentIRank2) {
                                            assert currentIRank2 < pIndex2;
                                            int b = hist[currentIValue2];
                                            assert b > 0;
                                            double delta = (pIndex2 - currentIRank2) / (double)b;
                                            i2 += (pIndex2 - currentIRank2) * (currentIValue2 + 0.5 * delta);
                                        }
                                        double i = i2 - i1;
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i);
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist, pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist, pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist, ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
                            }
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
                        }
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                hist1.include(a.getInt(i) >> bs);
                            }
                        }
                        SummingHistogram hist2 = hist1.nextSharing();
                        assert hist1.shareCount() == 2;
                        assert hist2.shareCount() == 2;
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap1 != null) {
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
            final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
            if (direct) {
                final int[] ja = (int[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // int A: simple, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
            final HistogramCache<SummingHistogram> histogramCache = new HistogramCache<SummingHistogram>();
            if (direct) {
                final long[] ja = (long[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // long A: simple, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        public strictfp double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        public strictfp double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                    public strictfp double getDouble(long index) {
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                    public strictfp double getDouble(long index) {
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                        public strictfp double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToRank(pIndex1);
                                        hist2.moveToRank(pIndex2);
                                        double i = hist1.currentIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                    + " instead of " + SummingHistogram.integralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                        public strictfp double getDouble(long index) {
                            double pIndex1 = fPerc1.getDouble(index);
                            checkNaN(pIndex1);
                            double pIndex2 = fPerc2.getDouble(index);
                            checkNaN(pIndex2);
                            double n = pIndex2 - pIndex1;
                            if (n <= 0.0) {
                                return filler;
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
                            double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                    buf1.copy(fPerc1.subArr(arrayPos, len));
                                }
                                if (ap2 != null) {
                                    buf2.copy(fPerc2.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                    double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                    double w = filler;
                                    double n = pIndex2 - pIndex1;
                                    if (n > 0.0) {
                                        hist1.moveToPreciseRank(pIndex1);
                                        hist2.moveToPreciseRank(pIndex2);
                                        double i = hist1.currentPreciseIntegralBetweenSharing();
                                        if (i < 0.0)
                                            throw new AssertionError("Negative integral = " + i + " = " +
                                                hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                                + ", simple integral = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                        if (DEBUG_MODE) {
                                            double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2);
                                            if (Math.abs(diff) > 0.01)
                                                throw new AssertionError("Bug: at index " + arrayPos
                                                    + ", integral = " + i + " = "
                                                    + hist2.currentPreciseIntegral() + " - "
                                                    + hist1.currentPreciseIntegral()
                                                    + " instead of "
                                                    + SummingHistogram.preciseIntegralBetweenRanks(
                                                    hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                    + diff + ", histogram is "
                                                    + JArrays.toString(hist1.bars(), ",", 2048));
                                        }
                                        w = (i / n) * multiplierInv;
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
                    public strictfp double getDouble(long index) {
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.integralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToRank(pIndex1);
                                    hist2.moveToRank(pIndex2);
                                    double i = hist1.currentIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        if (Math.abs(i - SummingHistogram.integralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2)) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentIntegral() + " - " + hist1.currentIntegral()
                                                + " instead of " + SummingHistogram.integralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
                    public strictfp double getDouble(long index) {
                        double pIndex1 = fPerc1.getDouble(index);
                        checkNaN(pIndex1);
                        double pIndex2 = fPerc2.getDouble(index);
                        checkNaN(pIndex2);
                        double n = pIndex2 - pIndex1;
                        if (n <= 0.0) {
                            return filler;
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
                        double i = SummingHistogram.preciseIntegralBetweenRanks(hist, pIndex1, pIndex2);
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
                                buf1.copy(fPerc1.subArr(arrayPos, len));
                            }
                            if (ap2 != null) {
                                buf2.copy(fPerc2.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double pIndex1 = ap1 != null ? buf1.getDouble(k) : fPerc1.getDouble(arrayPos);
                                double pIndex2 = ap2 != null ? buf2.getDouble(k) : fPerc2.getDouble(arrayPos);
                                double w = filler;
                                double n = pIndex2 - pIndex1;
                                if (n > 0.0) {
                                    hist1.moveToPreciseRank(pIndex1);
                                    hist2.moveToPreciseRank(pIndex2);
                                    double i = hist1.currentPreciseIntegralBetweenSharing();
                                    if (i < 0.0)
                                        throw new AssertionError("Negative integral = " + i + " = " +
                                            hist2.currentPreciseIntegral() + " - " + hist1.currentPreciseIntegral()
                                            + ", simple integral = "
                                            + hist2.currentIntegral() + " - " + hist1.currentIntegral());
                                    if (DEBUG_MODE) {
                                        double diff = i - SummingHistogram.preciseIntegralBetweenRanks(
                                            hist1.bars(), pIndex1, pIndex2);
                                        if (Math.abs(diff) > 0.01)
                                            throw new AssertionError("Bug: at index " + arrayPos
                                                + ", integral = " + i + " = "
                                                + hist2.currentPreciseIntegral() + " - "
                                                + hist1.currentPreciseIntegral()
                                                + " instead of "
                                                + SummingHistogram.preciseIntegralBetweenRanks(
                                                hist1.bars(), pIndex1, pIndex2) + ", difference = "
                                                + diff + ", histogram is "
                                                + JArrays.toString(hist1.bars(), ",", 2048));
                                    }
                                    w = (i / n) * multiplierInv;
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
            throw new IllegalArgumentException("Illegal rank (NaN) in some elements "
                + "of fromPercentileIndexes or toPercentileIndexes");
    }
}
