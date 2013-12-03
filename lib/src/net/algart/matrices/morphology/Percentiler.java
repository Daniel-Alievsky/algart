/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.functions.Func;

class Percentiler extends RankOperationProcessor {
    private final boolean optimizeGetData = OPTIMIZE_GET_DATA;
    private final boolean optimizeDirectArrays = OPTIMIZE_DIRECT_ARRAYS;
    private final boolean inlineOneLevel = INLINE_ONE_LEVEL;
    final boolean interpolated;

    Percentiler(ArrayContext context, boolean interpolated, int[] bitLevels) {
        super(context, bitLevels);
        this.interpolated = interpolated;
    }

    @Override
    PArray asProcessed(Class<? extends PArray> desiredType, PArray src, PArray[] additional,
        long[] dimensions, final long[] shifts, final long[] left, final long[] right)
    {
        if (additional.length == 0)
            throw new IllegalArgumentException("One additional matrix is required (percentile indexes)");
        assert shifts.length > 0;
        assert left.length == right.length;
        final boolean direct = optimizeDirectArrays &&
            src instanceof DirectAccessible && ((DirectAccessible)src).hasJavaArray();
        final PArray fPerc = additional[0] instanceof PIntegerArray || src instanceof PFloatingArray || interpolated ?
            additional[0] :
            Arrays.asFuncArray(true, Func.IDENTITY, LongArray.class, additional[0]);
        final PIntegerArray iPerc = fPerc instanceof PIntegerArray ? (PIntegerArray)fPerc : null;
        final ArrayPool ap =
            optimizeDirectArrays && (Arrays.isNCopies(fPerc) || SimpleMemoryModel.isSimpleArray(fPerc)) ? null :
                ArrayPool.getInstance(Arrays.SMM, fPerc.elementType(), BUFFER_BLOCK_SIZE);
        if (src instanceof BitArray) {
            final BitArray a = (BitArray)src;
            final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
            return new AbstractBitArray(src.length(), true, src) {
                public boolean getBit(long index) {
                    long r;
                    final double rank;
                    if (iPerc != null) {
                        rank = -1.0;
                        r = iPerc.getLong(index);
                    } else {
                        rank = fPerc.getDouble(index);
                        checkNaN(rank);
                        r = (long)rank;
                    }
                    if (r >= shifts.length) {
                        return true;
                    }
                    int leftBar = 0;
                    for (long shift : shifts) {
                        long i = index - shift;
                        if (i < 0) {
                            i += length;
                        }
                        if (!a.getBit(i)) {
                            leftBar++;
                        }
                    }
                    if (r < 0) {
                        return leftBar == 0;
                    }
                    if (iPerc != null) {
                        return r >= leftBar;
                    } else {
                        if (r >= leftBar || r == shifts.length - 1) {
                            return leftBar < shifts.length; // inside the bar [1,2[ or the latest narrow bar
                        } else if (r < leftBar - 1) {
                            assert leftBar > 0; // because r>=0 and leftBar>=r+1
                            return false; // inside the bar [0,1-1/leftBar[
                        } else {
                            assert r >= 0 && r == leftBar - 1; // so, leftBar > 0
                            final int rightBar = shifts.length - leftBar;
                            final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
                            final double fraction = rank - r;
                            final double newPreciseValue = (leftBar - 1 + fraction) * leftStripe;
                            final double weightedMeanStripe = leftBar == rightBar ?
                                leftStripe :
                                leftStripe + (rank - r) * (1.0 / rightBar - leftStripe);
                            assert weightedMeanStripe >= -0.001;
                            final double rangeCenter = newPreciseValue + 0.5 * Math.max(weightedMeanStripe, 1e-10);
                            // to be on the safe side, we guarantee that rangeCenter > newPreciseValue
                            // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
                            return rangeCenter >= 1.0;
                        }
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
                    boolean[] dest = (boolean[])destArray;
                    UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                    final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                    int[] hist = histogramCache.get(arrayPos); // here we save initialization stage
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
                        if (ap != null) {
                            buf.copy(fPerc.subArr(arrayPos, len));
                        }
                        for (int k = 0; k < len; k++, destArrayOffset++) {
                            final boolean v;
                            if (iPerc != null) {
                                final long r = ap != null ? ((PIntegerArray)buf).getLong(k) : iPerc.getLong(arrayPos);
                                if (r < 0) {
                                    v = hist[0] == 0;
                                } else {
                                    v = r >= hist[0]; // in particular, if r >= shifts.length
                                }
                            } else { // to understand the code below, please see Histogram.moveToPreciseRank
                                final double rank = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                checkNaN(rank);
                                final long r = (long)rank;
                                final int leftBar = hist[0];
                                if (r >= shifts.length) {
                                    v = true;
                                } else if (r >= leftBar || r == shifts.length - 1) {
                                    v = leftBar < shifts.length; // inside the bar [1,2[ or the latest narrow bar
                                } else if (r < 0 || r < leftBar - 1) {
                                    v = leftBar == 0; // < 0 or inside the bar [0,1-1/leftBar[
                                } else {
                                    assert r >= 0 && r == leftBar - 1; // so, leftBar > 0
                                    final int rightBar = shifts.length - leftBar;
                                    final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
                                    final double fraction = rank - r;
                                    final double newPreciseValue = (leftBar - 1 + fraction) * leftStripe;
                                    final double weightedMeanStripe = leftBar == rightBar ?
                                        leftStripe :
                                        leftStripe + (rank - r) * (1.0 / rightBar - leftStripe);
                                    assert weightedMeanStripe >= -0.001;
                                    final double rangeCenter = newPreciseValue + 0.5 * Math.max(weightedMeanStripe, 1e-10);
                                    // to be on the safe side, we guarantee that rangeCenter > newPreciseValue
                                    // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
                                    v = rangeCenter >= 1.0;
                                }
                            }
                            dest[destArrayOffset] = v;
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
                    if (ap != null) {
                        ap.releaseArray(buf);
                    }
                }
            };
        }

        //[[Repeat() 0xFFFF          ==> 0xFF,,0xFFFF;;
        //           \(char\)0;      ==> 0;,,...;;
        //           (return\s+)\(char\) ==> $1,,$1;;
        //           char            ==> byte,,short;;
        //           (\w+\s+)getChar ==> int getByte,,int getShort;;
        //           Char            ==> Byte,,Short;;
        //           16              ==> 8,,16 ]]
        if (src instanceof CharArray) {
            final CharArray a = (CharArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 16);
            final int bs = 16 - nab;
            if (direct) {
                final char[] ja = (char[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && iPerc != null) { // char A: simple histogram, direct, without Histogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractCharArray(src.length(), true, src) {
                        public char getChar(long index) {
                            long percentileIndex = iPerc.getLong(index);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return (char)(p << bs);
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    if (percentileIndex < 0) {
                                        percentileIndex = 0;
                                    } else if (percentileIndex > shifts.length) {
                                        percentileIndex = shifts.length;
                                    }
                                    if (percentileIndex < currentIRank) {
                                        do {
                                            --currentIValue;
                                            currentIRank -= hist[currentIValue];
                                        } while (percentileIndex < currentIRank);
                                        assert currentIRank >= 0;
                                    } else if (percentileIndex < shifts.length) {
                                        int b = hist[currentIValue];
                                        while (percentileIndex >= currentIRank + b) {
                                            currentIRank += b;
                                            ++currentIValue;
                                            b = hist[currentIValue];
                                        }
                                        assert currentIRank < shifts.length;
                                    } else if (currentIRank == shifts.length) {
                                        // special decreasing branch: percentileIndex == shifts.length
                                        assert currentIValue == hist.length || hist[currentIValue] == 0;
                                        assert currentIValue > 0;
                                        while (hist[currentIValue - 1] == 0) {
                                            --currentIValue;
                                            assert currentIValue > 0;
                                        }
                                    } else {
                                        // special increasing branch: percentileIndex == shifts.length
                                        assert currentIRank < shifts.length;
                                        do {
                                            currentIRank += hist[currentIValue];
                                            ++currentIValue;
                                        } while (currentIRank < shifts.length);
                                        assert currentIRank == shifts.length;
                                    }
                                    dest[destArrayOffset] = (char)(Math.min(currentIValue, hist.length - 1) << bs);
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
                                        if (value < currentIValue) {
                                            --currentIRank;
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
                                        if (value < currentIValue) {
                                            ++currentIRank;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else if (implementHere) { // char B: precise percentile, direct, without Histogram class
                    assert iPerc == null;
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractCharArray(src.length(), true, src) {
                        public char getChar(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return (char)(p << bs);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    checkNaN(percentileIndex);
                                    final int r;
                                    if (percentileIndex < 0) {
                                        percentileIndex = r = 0;
                                    } else if (percentileIndex > shifts.length) {
                                        percentileIndex = r = shifts.length;
                                    } else {
                                        r = (int)percentileIndex;
                                    }
                                    if (r < currentIRank) {
                                        do {
                                            --currentIValue;
                                            currentIRank -= hist[currentIValue];
                                        } while (r < currentIRank);
                                        assert currentIRank >= 0;
                                    } else if (r < shifts.length) {
                                        int b = hist[currentIValue];
                                        while (r >= currentIRank + b) {
                                            currentIRank += b;
                                            ++currentIValue;
                                            b = hist[currentIValue];
                                        }
                                        assert currentIRank < shifts.length;
                                    } else if (currentIRank == shifts.length) {
                                        // special decreasing branch: r == shifts.length
                                        assert currentIValue == hist.length || hist[currentIValue] == 0;
                                        assert currentIValue > 0;
                                        while (hist[currentIValue - 1] == 0) {
                                            --currentIValue;
                                            assert currentIValue > 0;
                                        }
                                    } else {
                                        // special increasing branch: r == shifts.length
                                        assert currentIRank < shifts.length;
                                        do {
                                            currentIRank += hist[currentIValue];
                                            ++currentIValue;
                                        } while (currentIRank < shifts.length);
                                        assert currentIRank == shifts.length;
                                    }
                                    if (r < shifts.length - 1) { // the rightmost range (b-1)/b..1.0 is not corrected
                                        final int leftValue = currentIValue;
                                        final int indexInBar = r - currentIRank;
                                        final int leftBar = hist[currentIValue];
                                        assert indexInBar < leftBar;
                                        if (r != percentileIndex && indexInBar == leftBar - 1) {
                                            final int leftRank = currentIRank;
                                            final double leftPreciseValue = leftBar == 1 ? leftValue :
                                                leftValue + (double)indexInBar / (double)leftBar;
                                            currentIRank += leftBar;
                                            assert r + 1 == currentIRank;
                                            ++currentIValue;
                                            int rightBar = hist[currentIValue];
                                            while (rightBar == 0) {
                                                ++currentIValue;
                                                rightBar = hist[currentIValue];
                                            }
                                            assert currentIRank < shifts.length;
                                            final double newPreciseValue = leftPreciseValue
                                                + (percentileIndex - r) * (currentIValue - leftPreciseValue);
                                            final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
                                            final double weightedMeanStripe = leftBar == rightBar ?
                                                leftStripe :
                                                leftStripe + (percentileIndex - r) * (1.0 / rightBar - leftStripe);
                                            assert weightedMeanStripe >= -0.001;
                                            final double rangeCenter = newPreciseValue
                                                + 0.5 * Math.max(weightedMeanStripe, 1e-10);
                                            // to be on the safe side, we guarantee that newValue+1 > newPreciseValue
                                            // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
                                            currentIValue = (int)rangeCenter;
                                            if (currentIValue == leftValue) {
                                                currentIRank = leftRank;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = (char)(Math.min(currentIValue, hist.length - 1) << bs);
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
                                        if (value < currentIValue) {
                                            --currentIRank;
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
                                        if (value < currentIValue) {
                                            ++currentIRank;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else if (iPerc != null) { // char C: simple percentile, direct
                    assert !implementHere;
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractCharArray(src.length(), true, src) {
                        public char getChar(long index) {
                            long percentileIndex = iPerc.getLong(index);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return (char)(p << bs);
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    hist.moveToIRank(percentileIndex);
                                    dest[destArrayOffset] = (char)(Math.min(hist.currentIValue(),
                                        hist.length() - 1) << bs);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // char D: precise percentile, direct
                    assert iPerc == null;
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractCharArray(src.length(), true, src) {
                        public char getChar(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return (char)(p << bs);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    dest[destArrayOffset] = (char)(Math.min(hist.currentIValue(),
                                        hist.length() - 1) << bs);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (iPerc != null) { // char E: simple percentile, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractCharArray(src.length(), true, src) {
                    public char getChar(long index) {
                        long percentileIndex = iPerc.getLong(index);
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                        return (char)(p << bs);
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
                        UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist.include(a.getInt(i) >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                hist.moveToIRank(percentileIndex);
                                dest[destArrayOffset] = (char)(Math.min(hist.currentIValue(),
                                    hist.length() - 1) << bs);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.exclude(a.getInt(i) >> bs);
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
                                    hist.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // char F: precise percentile, indirect
                assert iPerc == null;
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractCharArray(src.length(), true, src) {
                    public char getChar(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                        return (char)(p << bs);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist.include(a.getInt(i) >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                dest[destArrayOffset] = (char)(Math.min(hist.currentIValue(),
                                    hist.length() - 1) << bs);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.exclude(a.getInt(i) >> bs);
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
                                    hist.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
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
            if (direct) {
                final byte[] ja = (byte[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && iPerc != null) { // byte A: simple histogram, direct, without Histogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractByteArray(src.length(), true, src) {
                        public int getByte(long index) {
                            long percentileIndex = iPerc.getLong(index);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    if (percentileIndex < 0) {
                                        percentileIndex = 0;
                                    } else if (percentileIndex > shifts.length) {
                                        percentileIndex = shifts.length;
                                    }
                                    if (percentileIndex < currentIRank) {
                                        do {
                                            --currentIValue;
                                            currentIRank -= hist[currentIValue];
                                        } while (percentileIndex < currentIRank);
                                        assert currentIRank >= 0;
                                    } else if (percentileIndex < shifts.length) {
                                        int b = hist[currentIValue];
                                        while (percentileIndex >= currentIRank + b) {
                                            currentIRank += b;
                                            ++currentIValue;
                                            b = hist[currentIValue];
                                        }
                                        assert currentIRank < shifts.length;
                                    } else if (currentIRank == shifts.length) {
                                        // special decreasing branch: percentileIndex == shifts.length
                                        assert currentIValue == hist.length || hist[currentIValue] == 0;
                                        assert currentIValue > 0;
                                        while (hist[currentIValue - 1] == 0) {
                                            --currentIValue;
                                            assert currentIValue > 0;
                                        }
                                    } else {
                                        // special increasing branch: percentileIndex == shifts.length
                                        assert currentIRank < shifts.length;
                                        do {
                                            currentIRank += hist[currentIValue];
                                            ++currentIValue;
                                        } while (currentIRank < shifts.length);
                                        assert currentIRank == shifts.length;
                                    }
                                    dest[destArrayOffset] = (byte)(Math.min(currentIValue, hist.length - 1) << bs);
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
                                        if (value < currentIValue) {
                                            --currentIRank;
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
                                        if (value < currentIValue) {
                                            ++currentIRank;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else if (implementHere) { // byte B: precise percentile, direct, without Histogram class
                    assert iPerc == null;
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractByteArray(src.length(), true, src) {
                        public int getByte(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    checkNaN(percentileIndex);
                                    final int r;
                                    if (percentileIndex < 0) {
                                        percentileIndex = r = 0;
                                    } else if (percentileIndex > shifts.length) {
                                        percentileIndex = r = shifts.length;
                                    } else {
                                        r = (int)percentileIndex;
                                    }
                                    if (r < currentIRank) {
                                        do {
                                            --currentIValue;
                                            currentIRank -= hist[currentIValue];
                                        } while (r < currentIRank);
                                        assert currentIRank >= 0;
                                    } else if (r < shifts.length) {
                                        int b = hist[currentIValue];
                                        while (r >= currentIRank + b) {
                                            currentIRank += b;
                                            ++currentIValue;
                                            b = hist[currentIValue];
                                        }
                                        assert currentIRank < shifts.length;
                                    } else if (currentIRank == shifts.length) {
                                        // special decreasing branch: r == shifts.length
                                        assert currentIValue == hist.length || hist[currentIValue] == 0;
                                        assert currentIValue > 0;
                                        while (hist[currentIValue - 1] == 0) {
                                            --currentIValue;
                                            assert currentIValue > 0;
                                        }
                                    } else {
                                        // special increasing branch: r == shifts.length
                                        assert currentIRank < shifts.length;
                                        do {
                                            currentIRank += hist[currentIValue];
                                            ++currentIValue;
                                        } while (currentIRank < shifts.length);
                                        assert currentIRank == shifts.length;
                                    }
                                    if (r < shifts.length - 1) { // the rightmost range (b-1)/b..1.0 is not corrected
                                        final int leftValue = currentIValue;
                                        final int indexInBar = r - currentIRank;
                                        final int leftBar = hist[currentIValue];
                                        assert indexInBar < leftBar;
                                        if (r != percentileIndex && indexInBar == leftBar - 1) {
                                            final int leftRank = currentIRank;
                                            final double leftPreciseValue = leftBar == 1 ? leftValue :
                                                leftValue + (double)indexInBar / (double)leftBar;
                                            currentIRank += leftBar;
                                            assert r + 1 == currentIRank;
                                            ++currentIValue;
                                            int rightBar = hist[currentIValue];
                                            while (rightBar == 0) {
                                                ++currentIValue;
                                                rightBar = hist[currentIValue];
                                            }
                                            assert currentIRank < shifts.length;
                                            final double newPreciseValue = leftPreciseValue
                                                + (percentileIndex - r) * (currentIValue - leftPreciseValue);
                                            final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
                                            final double weightedMeanStripe = leftBar == rightBar ?
                                                leftStripe :
                                                leftStripe + (percentileIndex - r) * (1.0 / rightBar - leftStripe);
                                            assert weightedMeanStripe >= -0.001;
                                            final double rangeCenter = newPreciseValue
                                                + 0.5 * Math.max(weightedMeanStripe, 1e-10);
                                            // to be on the safe side, we guarantee that newValue+1 > newPreciseValue
                                            // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
                                            currentIValue = (int)rangeCenter;
                                            if (currentIValue == leftValue) {
                                                currentIRank = leftRank;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = (byte)(Math.min(currentIValue, hist.length - 1) << bs);
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
                                        if (value < currentIValue) {
                                            --currentIRank;
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
                                        if (value < currentIValue) {
                                            ++currentIRank;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else if (iPerc != null) { // byte C: simple percentile, direct
                    assert !implementHere;
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractByteArray(src.length(), true, src) {
                        public int getByte(long index) {
                            long percentileIndex = iPerc.getLong(index);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.include((ja[jaOfs + (int)i] & 0xFF) >> bs); // &0xFF for preprocessing
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    hist.moveToIRank(percentileIndex);
                                    dest[destArrayOffset] = (byte)(Math.min(hist.currentIValue(),
                                        hist.length() - 1) << bs);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // byte D: precise percentile, direct
                    assert iPerc == null;
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractByteArray(src.length(), true, src) {
                        public int getByte(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                            }
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.include((ja[jaOfs + (int)i] & 0xFF) >> bs); // &0xFF for preprocessing
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    dest[destArrayOffset] = (byte)(Math.min(hist.currentIValue(),
                                        hist.length() - 1) << bs);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFF) >> bs;
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (iPerc != null) { // byte E: simple percentile, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractByteArray(src.length(), true, src) {
                    public int getByte(long index) {
                        long percentileIndex = iPerc.getLong(index);
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                        return (p << bs);
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
                        UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist.include(a.getInt(i) >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                hist.moveToIRank(percentileIndex);
                                dest[destArrayOffset] = (byte)(Math.min(hist.currentIValue(),
                                    hist.length() - 1) << bs);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.exclude(a.getInt(i) >> bs);
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
                                    hist.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // byte F: precise percentile, indirect
                assert iPerc == null;
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractByteArray(src.length(), true, src) {
                    public int getByte(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                        return (p << bs);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist.include(a.getInt(i) >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                dest[destArrayOffset] = (byte)(Math.min(hist.currentIValue(),
                                    hist.length() - 1) << bs);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.exclude(a.getInt(i) >> bs);
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
                                    hist.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            }
        }
        if (src instanceof ShortArray) {
            final ShortArray a = (ShortArray)src;
            final int nab = Math.min(numberOfAnalyzedBits, 16);
            final int bs = 16 - nab;
            if (direct) {
                final short[] ja = (short[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && iPerc != null) { // short A: simple histogram, direct, without Histogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractShortArray(src.length(), true, src) {
                        public int getShort(long index) {
                            long percentileIndex = iPerc.getLong(index);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    if (percentileIndex < 0) {
                                        percentileIndex = 0;
                                    } else if (percentileIndex > shifts.length) {
                                        percentileIndex = shifts.length;
                                    }
                                    if (percentileIndex < currentIRank) {
                                        do {
                                            --currentIValue;
                                            currentIRank -= hist[currentIValue];
                                        } while (percentileIndex < currentIRank);
                                        assert currentIRank >= 0;
                                    } else if (percentileIndex < shifts.length) {
                                        int b = hist[currentIValue];
                                        while (percentileIndex >= currentIRank + b) {
                                            currentIRank += b;
                                            ++currentIValue;
                                            b = hist[currentIValue];
                                        }
                                        assert currentIRank < shifts.length;
                                    } else if (currentIRank == shifts.length) {
                                        // special decreasing branch: percentileIndex == shifts.length
                                        assert currentIValue == hist.length || hist[currentIValue] == 0;
                                        assert currentIValue > 0;
                                        while (hist[currentIValue - 1] == 0) {
                                            --currentIValue;
                                            assert currentIValue > 0;
                                        }
                                    } else {
                                        // special increasing branch: percentileIndex == shifts.length
                                        assert currentIRank < shifts.length;
                                        do {
                                            currentIRank += hist[currentIValue];
                                            ++currentIValue;
                                        } while (currentIRank < shifts.length);
                                        assert currentIRank == shifts.length;
                                    }
                                    dest[destArrayOffset] = (short)(Math.min(currentIValue, hist.length - 1) << bs);
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
                                        if (value < currentIValue) {
                                            --currentIRank;
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
                                        if (value < currentIValue) {
                                            ++currentIRank;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else if (implementHere) { // short B: precise percentile, direct, without Histogram class
                    assert iPerc == null;
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractShortArray(src.length(), true, src) {
                        public int getShort(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    checkNaN(percentileIndex);
                                    final int r;
                                    if (percentileIndex < 0) {
                                        percentileIndex = r = 0;
                                    } else if (percentileIndex > shifts.length) {
                                        percentileIndex = r = shifts.length;
                                    } else {
                                        r = (int)percentileIndex;
                                    }
                                    if (r < currentIRank) {
                                        do {
                                            --currentIValue;
                                            currentIRank -= hist[currentIValue];
                                        } while (r < currentIRank);
                                        assert currentIRank >= 0;
                                    } else if (r < shifts.length) {
                                        int b = hist[currentIValue];
                                        while (r >= currentIRank + b) {
                                            currentIRank += b;
                                            ++currentIValue;
                                            b = hist[currentIValue];
                                        }
                                        assert currentIRank < shifts.length;
                                    } else if (currentIRank == shifts.length) {
                                        // special decreasing branch: r == shifts.length
                                        assert currentIValue == hist.length || hist[currentIValue] == 0;
                                        assert currentIValue > 0;
                                        while (hist[currentIValue - 1] == 0) {
                                            --currentIValue;
                                            assert currentIValue > 0;
                                        }
                                    } else {
                                        // special increasing branch: r == shifts.length
                                        assert currentIRank < shifts.length;
                                        do {
                                            currentIRank += hist[currentIValue];
                                            ++currentIValue;
                                        } while (currentIRank < shifts.length);
                                        assert currentIRank == shifts.length;
                                    }
                                    if (r < shifts.length - 1) { // the rightmost range (b-1)/b..1.0 is not corrected
                                        final int leftValue = currentIValue;
                                        final int indexInBar = r - currentIRank;
                                        final int leftBar = hist[currentIValue];
                                        assert indexInBar < leftBar;
                                        if (r != percentileIndex && indexInBar == leftBar - 1) {
                                            final int leftRank = currentIRank;
                                            final double leftPreciseValue = leftBar == 1 ? leftValue :
                                                leftValue + (double)indexInBar / (double)leftBar;
                                            currentIRank += leftBar;
                                            assert r + 1 == currentIRank;
                                            ++currentIValue;
                                            int rightBar = hist[currentIValue];
                                            while (rightBar == 0) {
                                                ++currentIValue;
                                                rightBar = hist[currentIValue];
                                            }
                                            assert currentIRank < shifts.length;
                                            final double newPreciseValue = leftPreciseValue
                                                + (percentileIndex - r) * (currentIValue - leftPreciseValue);
                                            final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
                                            final double weightedMeanStripe = leftBar == rightBar ?
                                                leftStripe :
                                                leftStripe + (percentileIndex - r) * (1.0 / rightBar - leftStripe);
                                            assert weightedMeanStripe >= -0.001;
                                            final double rangeCenter = newPreciseValue
                                                + 0.5 * Math.max(weightedMeanStripe, 1e-10);
                                            // to be on the safe side, we guarantee that newValue+1 > newPreciseValue
                                            // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
                                            currentIValue = (int)rangeCenter;
                                            if (currentIValue == leftValue) {
                                                currentIRank = leftRank;
                                            }
                                        }
                                    }
                                    dest[destArrayOffset] = (short)(Math.min(currentIValue, hist.length - 1) << bs);
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
                                        if (value < currentIValue) {
                                            --currentIRank;
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
                                        if (value < currentIValue) {
                                            ++currentIRank;
                                        }
                                    }
                                }
                            }
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else if (iPerc != null) { // short C: simple percentile, direct
                    assert !implementHere;
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractShortArray(src.length(), true, src) {
                        public int getShort(long index) {
                            long percentileIndex = iPerc.getLong(index);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    hist.moveToIRank(percentileIndex);
                                    dest[destArrayOffset] = (short)(Math.min(hist.currentIValue(),
                                        hist.length() - 1) << bs);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // short D: precise percentile, direct
                    assert iPerc == null;
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractShortArray(src.length(), true, src) {
                        public int getShort(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
                            int[] hist = new int[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                            }
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return (p << bs);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.include((ja[jaOfs + (int)i] & 0xFFFF) >> bs); // &0xFFFF for preprocessing
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    dest[destArrayOffset] = (short)(Math.min(hist.currentIValue(),
                                        hist.length() - 1) << bs);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        barIndexes[j] = (ja[jaOfs + (int)i] & 0xFFFF) >> bs;
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (iPerc != null) { // short E: simple percentile, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractShortArray(src.length(), true, src) {
                    public int getShort(long index) {
                        long percentileIndex = iPerc.getLong(index);
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                        return (p << bs);
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
                        UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist.include(a.getInt(i) >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                hist.moveToIRank(percentileIndex);
                                dest[destArrayOffset] = (short)(Math.min(hist.currentIValue(),
                                    hist.length() - 1) << bs);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.exclude(a.getInt(i) >> bs);
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
                                    hist.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // short F: precise percentile, indirect
                assert iPerc == null;
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractShortArray(src.length(), true, src) {
                    public int getShort(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
                        int[] hist = new int[1 << nab];
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            hist[a.getInt(i) >> bs]++;
                        }
                        int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                        return (p << bs);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist.include(a.getInt(i) >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                dest[destArrayOffset] = (short)(Math.min(hist.currentIValue(),
                                    hist.length() - 1) << bs);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist.exclude(a.getInt(i) >> bs);
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
                                    hist.include(a.getInt(i) >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
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
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final int[] ja = (int[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (iPerc != null) { // int A: simple percentile, direct
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            long percentileIndex = iPerc.getLong(index);
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
                            int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return p << bs;
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.include(v >> bs);
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    hist.moveToIRank(percentileIndex);
                                    int p = Math.min(hist.currentIValue(), hist.length() - 1);
                                    dest[destArrayOffset] = p << bs;
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
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // int B: precise percentile, direct
                    assert iPerc == null;
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
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
                            int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return p << bs;
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.include(v >> bs);
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    int p = Math.min(hist.currentIValue(), hist.length() - 1);
                                    dest[destArrayOffset] = p << bs;
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
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (iPerc != null) { // int C: simple percentile, indirect
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        long percentileIndex = iPerc.getLong(index);
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
                        int p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                        return p << bs;
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
                        UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = a.getInt(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist.include(v >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                hist.moveToIRank(percentileIndex);
                                int p = Math.min(hist.currentIValue(), hist.length() - 1);
                                dest[destArrayOffset] = p << bs;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = a.getInt(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.exclude(v >> bs);
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
                                    hist.include(v >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // int D: precise percentile, indirect
                assert iPerc == null;
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
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
                        int p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                        return p << bs;
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = a.getInt(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist.include(v >> bs);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                int p = Math.min(hist.currentIValue(), hist.length() - 1);
                                dest[destArrayOffset] = p << bs;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    int v = a.getInt(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.exclude(v >> bs);
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
                                    hist.include(v >> bs);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
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
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final long[] ja = (long[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (iPerc != null) { // long A: simple percentile, direct
                    return new AbstractLongArray(src.length(), true, src) {
                        public long getLong(long index) {
                            long percentileIndex = iPerc.getLong(index);
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
                            long p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                            return p << bs;
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
                            UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.include((int)(v >> bs));
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                    hist.moveToIRank(percentileIndex);
                                    long p = Math.min(hist.currentIValue(), hist.length() - 1);
                                    dest[destArrayOffset] = p << bs;
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
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // long B: precise percentile, direct
                    assert iPerc == null;
                    return new AbstractLongArray(src.length(), true, src) {
                        public long getLong(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
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
                            long p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                            return p << bs;
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = ja[jaOfs + (int)i];
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.include((int)(v >> bs));
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    long p = Math.min(hist.currentIValue(), hist.length() - 1);
                                    dest[destArrayOffset] = p << bs;
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
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (iPerc != null) { // long C: simple percentile, indirect
                return new AbstractLongArray(src.length(), true, src) {
                    public long getLong(long index) {
                        long percentileIndex = iPerc.getLong(index);
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
                        long p = Math.min(Histogram.iValue(hist, percentileIndex), hist.length - 1);
                        return p << bs;
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
                        UpdatablePIntegerArray buf = ap == null ? null : (UpdatablePIntegerArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = a.getLong(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist.include((int)(v >> bs));
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                long percentileIndex = ap != null ? buf.getLong(k) : iPerc.getLong(arrayPos);
                                hist.moveToIRank(percentileIndex);
                                long p = Math.min(hist.currentIValue(), hist.length() - 1);
                                dest[destArrayOffset] = p << bs;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = a.getLong(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.exclude((int)(v >> bs));
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
                                    hist.include((int)(v >> bs));
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // long D: precise percentile, indirect
                assert iPerc == null;
                return new AbstractLongArray(src.length(), true, src) {
                    public long getLong(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
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
                        long p = Math.min(Histogram.iPreciseValue(hist, percentileIndex), hist.length - 1);
                        return p << bs;
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(1 << nab, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = a.getLong(i);
                                if (v < 0) {
                                    v = 0;
                                }
                                hist.include((int)(v >> bs));
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                long p = Math.min(hist.currentIValue(), hist.length() - 1);
                                dest[destArrayOffset] = p << bs;
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    long v = a.getLong(i);
                                    if (v < 0) {
                                        v = 0;
                                    }
                                    hist.exclude((int)(v >> bs));
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
                                    hist.include((int)(v >> bs));
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
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
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final float[] ja = (float[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // float A: simple percentile, direct
                    return new AbstractFloatArray(src.length(), true, src) {
                        public strictfp float getFloat(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
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
                            double p = Histogram.value(hist, percentileIndex);
                            return (float)(p * multiplierInv);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(histLength, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist.include(histIndex);
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToRank(percentileIndex);
                                    dest[destArrayOffset] = (float)(hist.currentValue() * multiplierInv);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // float B: precise percentile, direct
                    return new AbstractFloatArray(src.length(), true, src) {
                        public strictfp float getFloat(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
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
                            double p = Histogram.preciseValue(hist, percentileIndex);
                            return (float)(p * multiplierInv);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(histLength, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist.include(histIndex);
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    dest[destArrayOffset] = (float)(hist.currentValue() * multiplierInv);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // float C: simple percentile, indirect
                return new AbstractFloatArray(src.length(), true, src) {
                    public strictfp float getFloat(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
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
                        double p = Histogram.value(hist, percentileIndex);
                        return (float)(p * multiplierInv);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(histLength, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist.include(histIndex);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToRank(percentileIndex);
                                dest[destArrayOffset] = (float)(hist.currentValue() * multiplierInv);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist.exclude(histIndex);
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
                                    hist.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // float D: precise percentile, direct
                return new AbstractFloatArray(src.length(), true, src) {
                    public strictfp float getFloat(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
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
                        double p = Histogram.preciseValue(hist, percentileIndex);
                        return (float)(p * multiplierInv);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(histLength, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist.include(histIndex);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                dest[destArrayOffset] = (float)(hist.currentValue() * multiplierInv);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist.exclude(histIndex);
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
                                    hist.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
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
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final double[] ja = (double[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // double A: simple percentile, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public strictfp double getDouble(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
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
                            double p = Histogram.value(hist, percentileIndex);
                            return (p * multiplierInv);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(histLength, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist.include(histIndex);
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToRank(percentileIndex);
                                    dest[destArrayOffset] = (hist.currentValue() * multiplierInv);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                } else { // double B: precise percentile, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public strictfp double getDouble(long index) {
                            double percentileIndex = fPerc.getDouble(index);
                            checkNaN(percentileIndex);
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
                            double p = Histogram.preciseValue(hist, percentileIndex);
                            return (p * multiplierInv);
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
                            UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            Histogram hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = Histogram.newIntHistogram(histLength, bitLevels);
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                    hist.include(histIndex);
                                }
                            }
                            int[] barIndexes = (int[])indexesPool.requestArray();
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(fPerc.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                    hist.moveToPreciseRank(percentileIndex);
                                    dest[destArrayOffset] = (hist.currentValue() * multiplierInv);
                                    for (int j = 0; j < right.length; j++) {
                                        long i = arrayPos - right[j];
                                        if (i < 0) {
                                            i += length;
                                        }
                                        double v = ja[jaOfs + (int)i];
                                        barIndexes[j] = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                            (int)(v * multiplier);
                                    }
                                    hist.exclude(barIndexes);
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
                                    hist.include(barIndexes);
                                }
                            }
                            indexesPool.releaseArray(barIndexes);
                            histogramCache.put(arrayPos, hist);
                            if (ap != null) {
                                ap.releaseArray(buf);
                            }
                        }
                    };
                }
            } else if (!interpolated) { // double C: simple percentile, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public strictfp double getDouble(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
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
                        double p = Histogram.value(hist, percentileIndex);
                        return (p * multiplierInv);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(histLength, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist.include(histIndex);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToRank(percentileIndex);
                                dest[destArrayOffset] = (hist.currentValue() * multiplierInv);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist.exclude(histIndex);
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
                                    hist.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
                        }
                    }
                };
            } else { // double D: precise percentile, direct
                return new AbstractDoubleArray(src.length(), true, src) {
                    public strictfp double getDouble(long index) {
                        double percentileIndex = fPerc.getDouble(index);
                        checkNaN(percentileIndex);
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
                        double p = Histogram.preciseValue(hist, percentileIndex);
                        return (p * multiplierInv);
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
                        UpdatablePArray buf = ap == null ? null : (UpdatablePArray)ap.requestArray();
                        final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                        Histogram hist = histogramCache.get(arrayPos);
                        if (hist == null) {
                            hist = Histogram.newIntHistogram(histLength, bitLevels);
                            for (long shift : shifts) {
                                long i = arrayPos - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                hist.include(histIndex);
                            }
                        }
                        for (; count > 0; count -= bufLen) {
                            final int len = Math.min(bufLen, count);
                            if (ap != null) {
                                buf.copy(fPerc.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double percentileIndex = ap != null ? buf.getDouble(k) : fPerc.getDouble(arrayPos);
                                hist.moveToPreciseRank(percentileIndex);
                                dest[destArrayOffset] = (hist.currentValue() * multiplierInv);
                                for (long shift : right) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = a.getDouble(i);
                                    int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 :
                                        (int)(v * multiplier);
                                    hist.exclude(histIndex);
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
                                    hist.include(histIndex);
                                }
                            }
                        }
                        histogramCache.put(arrayPos, hist);
                        if (ap != null) {
                            ap.releaseArray(buf);
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
            throw new IllegalArgumentException("Illegal rank (NaN) in some percentile indexes");
    }
}
