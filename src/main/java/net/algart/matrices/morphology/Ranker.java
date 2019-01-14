/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

class Ranker extends RankOperationProcessor {
    private static final boolean DEBUG_MODE = false; // thorough checking getInt / getDouble results

    private final boolean optimizeGetData = OPTIMIZE_GET_DATA;
    private final boolean optimizeDirectArrays = OPTIMIZE_DIRECT_ARRAYS;
    private final boolean inlineOneLevel = INLINE_ONE_LEVEL;
    private final boolean interpolated;

    Ranker(ArrayContext context, boolean interpolated, int[] bitLevels) {
        super(context, bitLevels);
        this.interpolated = interpolated;
    }

    @Override
    PArray asProcessed(Class<? extends PArray> desiredType, PArray src, PArray[] additional,
        long[] dimensions, final long[] shifts, final long[] left, final long[] right)
    {
        if (additional.length == 0)
            throw new IllegalArgumentException("One additional matrix is required "
                + "(the matrix, which should be ranked in relation to this one)");
        assert left.length == right.length;
        final boolean direct = optimizeDirectArrays &&
            src instanceof DirectAccessible && ((DirectAccessible)src).hasJavaArray();
        PArray rankedArray = additional[0];
        if (rankedArray.elementType() != src.elementType()) {
            rankedArray = Arrays.asFuncArray(Func.IDENTITY, src.type(), rankedArray);
        }
        final ArrayPool ap =
            optimizeDirectArrays && (Arrays.isNCopies(rankedArray) || SimpleMemoryModel.isSimpleArray(rankedArray)) ?
                null :
                ArrayPool.getInstance(Arrays.SMM, rankedArray.elementType(), BUFFER_BLOCK_SIZE);
        if (src instanceof BitArray) {
            final BitArray a = (BitArray)src;
            final BitArray ra = (BitArray)rankedArray;
            final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
            switch (interpolated ? 1 : 0) { // for bit case, the interpolated rank is the same as the simple one
                //[[Repeat() IntArray ==> DoubleArray;;
                //           int\s+getInt ==> double getDouble;;
                //           int\[\](\s+dest|\)) ==> double[]$1;;
                //           case\s+0\: ==> case 1: ]]
                case 0:
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            boolean level = ra.getBit(index);
                            if (!level) {
                                return 0;
                            }
                            int hist0 = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                if (!a.getBit(i)) {
                                    hist0++;
                                }
                            }
                            return hist0;
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
                            UpdatableBitArray buf = ap == null ? null : (UpdatableBitArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                                if (ap != null) {
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    boolean level = ap != null ? buf.getBit(k) : ra.getBit(arrayPos);
                                    dest[destArrayOffset] = level ? hist[0] : 0;
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
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                case 1:
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            boolean level = ra.getBit(index);
                            if (!level) {
                                return 0;
                            }
                            int hist0 = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                if (!a.getBit(i)) {
                                    hist0++;
                                }
                            }
                            return hist0;
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
                            UpdatableBitArray buf = ap == null ? null : (UpdatableBitArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
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
                                if (ap != null) {
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    boolean level = ap != null ? buf.getBit(k) : ra.getBit(arrayPos);
                                    dest[destArrayOffset] = level ? hist[0] : 0;
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
                //[[Repeat.AutoGeneratedEnd]]
            }
        }

        //[[Repeat() 0xFFFF          ==> 0xFF,,0xFFFF;;
        //           char            ==> byte,,short;;
        //           Char            ==> Byte,,Short;;
        //           16              ==> 8,,16 ]]
        if (src instanceof CharArray) {
            final CharArray a = (CharArray)src;
            final CharArray ra = (CharArray)rankedArray;
            final int nab = Math.min(numberOfAnalyzedBits, 16);
            final int bs = 16 - nab;
            if (direct) {
                final char[] ja = (char[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // char A: simple rank, direct, without Histogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getChar(index) >> bs;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFFFF) >> bs; // &0xFFFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableCharArray buf = ap == null ? null : (UpdatableCharArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            int[] hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = new int[1 << nab];
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF added for preprocessing
                                }
                            }
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getChar(k) >> bs : ra.getChar(arrayPos) >> bs;
                                    if (level < currentIValue) {
                                        for (int j = currentIValue - 1; j >= level; j--) {
                                            currentIRank -= hist[j];
                                        }
                                        assert currentIRank >= 0;
                                    } else {
                                        for (int j = currentIValue; j < level; j++) {
                                            currentIRank += hist[j];
                                        }
                                        assert currentIRank <= shifts.length;
                                    }
                                    currentIValue = level;
                                    dest[destArrayOffset] = currentIRank;
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
                } else if (!interpolated) { // char B: simple rank, direct
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getChar(index) >> bs;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFFFF) >> bs; // &0xFFFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableCharArray buf = ap == null ? null : (UpdatableCharArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getChar(k) >> bs : ra.getChar(arrayPos) >> bs;
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = (int)hist.currentIRank();
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
                } else { // char C: precise rank, direct
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            int level = ra.getChar(index) >> bs;
                            int cnt = 0;
                            int left = -1, right = Integer.MAX_VALUE;
                            int leftBar = 157;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFFFF) >> bs; // &0xFFFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                }
                            }
                            if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (right == level) { // there is a value equal to level
                                result = cnt;
                            } else {
                                assert left < level && level < right;
                                assert leftBar > 0;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (level - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[1 << nab];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                                }
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                        + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                        + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableCharArray buf = ap == null ? null : (UpdatableCharArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getChar(k) >> bs : ra.getChar(arrayPos) >> bs;
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
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
            } else if (!interpolated) { // char D: simple rank, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        int level = ra.getChar(index) >> bs;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int histIndex = a.getInt(i) >> bs;
                            if (histIndex < level) {
                                cnt++;
                            }
                        }
                        return cnt;
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
                        UpdatableCharArray buf = ap == null ? null : (UpdatableCharArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getChar(k) >> bs : ra.getChar(arrayPos) >> bs;
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = (int)hist.currentIRank();
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
            } else { // char E: precise rank, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        int level = ra.getChar(index) >> bs;
                        int cnt = 0;
                        int left = -1, right = Integer.MAX_VALUE;
                        int leftBar = 157;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int histIndex = a.getInt(i) >> bs;
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            }
                        }
                        if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (right == level) { // there is a value equal to level
                            result = cnt;
                        } else {
                            assert left < level && level < right;
                            assert leftBar > 0;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (level - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[a.getInt(i) >> bs]++;
                            }
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                    + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                    + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableCharArray buf = ap == null ? null : (UpdatableCharArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getChar(k) >> bs : ra.getChar(arrayPos) >> bs;
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = hist.currentPreciseRank();
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
            final ByteArray ra = (ByteArray)rankedArray;
            final int nab = Math.min(numberOfAnalyzedBits, 8);
            final int bs = 8 - nab;
            if (direct) {
                final byte[] ja = (byte[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // byte A: simple rank, direct, without Histogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getByte(index) >> bs;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFF) >> bs; // &0xFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableByteArray buf = ap == null ? null : (UpdatableByteArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            int[] hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = new int[1 << nab];
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF added for preprocessing
                                }
                            }
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getByte(k) >> bs : ra.getByte(arrayPos) >> bs;
                                    if (level < currentIValue) {
                                        for (int j = currentIValue - 1; j >= level; j--) {
                                            currentIRank -= hist[j];
                                        }
                                        assert currentIRank >= 0;
                                    } else {
                                        for (int j = currentIValue; j < level; j++) {
                                            currentIRank += hist[j];
                                        }
                                        assert currentIRank <= shifts.length;
                                    }
                                    currentIValue = level;
                                    dest[destArrayOffset] = currentIRank;
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
                } else if (!interpolated) { // byte B: simple rank, direct
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getByte(index) >> bs;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFF) >> bs; // &0xFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableByteArray buf = ap == null ? null : (UpdatableByteArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getByte(k) >> bs : ra.getByte(arrayPos) >> bs;
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = (int)hist.currentIRank();
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
                } else { // byte C: precise rank, direct
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            int level = ra.getByte(index) >> bs;
                            int cnt = 0;
                            int left = -1, right = Integer.MAX_VALUE;
                            int leftBar = 157;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFF) >> bs; // &0xFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                }
                            }
                            if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (right == level) { // there is a value equal to level
                                result = cnt;
                            } else {
                                assert left < level && level < right;
                                assert leftBar > 0;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (level - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[1 << nab];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFF) >> bs]++; // &0xFF for preprocessing
                                }
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                        + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                        + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableByteArray buf = ap == null ? null : (UpdatableByteArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getByte(k) >> bs : ra.getByte(arrayPos) >> bs;
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
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
            } else if (!interpolated) { // byte D: simple rank, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        int level = ra.getByte(index) >> bs;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int histIndex = a.getInt(i) >> bs;
                            if (histIndex < level) {
                                cnt++;
                            }
                        }
                        return cnt;
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
                        UpdatableByteArray buf = ap == null ? null : (UpdatableByteArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getByte(k) >> bs : ra.getByte(arrayPos) >> bs;
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = (int)hist.currentIRank();
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
            } else { // byte E: precise rank, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        int level = ra.getByte(index) >> bs;
                        int cnt = 0;
                        int left = -1, right = Integer.MAX_VALUE;
                        int leftBar = 157;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int histIndex = a.getInt(i) >> bs;
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            }
                        }
                        if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (right == level) { // there is a value equal to level
                            result = cnt;
                        } else {
                            assert left < level && level < right;
                            assert leftBar > 0;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (level - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[a.getInt(i) >> bs]++;
                            }
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                    + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                    + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableByteArray buf = ap == null ? null : (UpdatableByteArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getByte(k) >> bs : ra.getByte(arrayPos) >> bs;
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = hist.currentPreciseRank();
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
            final ShortArray ra = (ShortArray)rankedArray;
            final int nab = Math.min(numberOfAnalyzedBits, 16);
            final int bs = 16 - nab;
            if (direct) {
                final short[] ja = (short[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final boolean implementHere = inlineOneLevel && bitLevels.length == 0;
                if (implementHere && !interpolated) { // short A: simple rank, direct, without Histogram class
                    final HistogramCache<int[]> histogramCache = new HistogramCache<int[]>();
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getShort(index) >> bs;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFFFF) >> bs; // &0xFFFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableShortArray buf = ap == null ? null : (UpdatableShortArray)ap.requestArray();
                            final int bufLen = ap == null ? count : BUFFER_BLOCK_SIZE;
                            int[] hist = histogramCache.get(arrayPos);
                            if (hist == null) {
                                hist = new int[1 << nab];
                                for (long shift : shifts) {
                                    long i = arrayPos - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF added for preprocessing
                                }
                            }
                            int currentIValue = 0;
                            int currentIRank = 0; // number of elements less than currentIValue
                            for (; count > 0; count -= bufLen) {
                                final int len = Math.min(bufLen, count);
                                if (ap != null) {
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getShort(k) >> bs : ra.getShort(arrayPos) >> bs;
                                    if (level < currentIValue) {
                                        for (int j = currentIValue - 1; j >= level; j--) {
                                            currentIRank -= hist[j];
                                        }
                                        assert currentIRank >= 0;
                                    } else {
                                        for (int j = currentIValue; j < level; j++) {
                                            currentIRank += hist[j];
                                        }
                                        assert currentIRank <= shifts.length;
                                    }
                                    currentIValue = level;
                                    dest[destArrayOffset] = currentIRank;
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
                } else if (!interpolated) { // short B: simple rank, direct
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getShort(index) >> bs;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFFFF) >> bs; // &0xFFFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableShortArray buf = ap == null ? null : (UpdatableShortArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getShort(k) >> bs : ra.getShort(arrayPos) >> bs;
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = (int)hist.currentIRank();
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
                } else { // short C: precise rank, direct
                    final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                    final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            int level = ra.getShort(index) >> bs;
                            int cnt = 0;
                            int left = -1, right = Integer.MAX_VALUE;
                            int leftBar = 157;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int histIndex = (ja[jaOfs + (int)i] & 0xFFFF) >> bs; // &0xFFFF for preprocessing
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                }
                            }
                            if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (right == level) { // there is a value equal to level
                                result = cnt;
                            } else {
                                assert left < level && level < right;
                                assert leftBar > 0;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (level - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[1 << nab];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    hist[(ja[jaOfs + (int)i] & 0xFFFF) >> bs]++; // &0xFFFF for preprocessing
                                }
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                        + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                        + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableShortArray buf = ap == null ? null : (UpdatableShortArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getShort(k) >> bs : ra.getShort(arrayPos) >> bs;
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
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
            } else if (!interpolated) { // short D: simple rank, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        int level = ra.getShort(index) >> bs;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int histIndex = a.getInt(i) >> bs;
                            if (histIndex < level) {
                                cnt++;
                            }
                        }
                        return cnt;
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
                        UpdatableShortArray buf = ap == null ? null : (UpdatableShortArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getShort(k) >> bs : ra.getShort(arrayPos) >> bs;
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = (int)hist.currentIRank();
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
            } else { // short E: precise rank, indirect
                final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        int level = ra.getShort(index) >> bs;
                        int cnt = 0;
                        int left = -1, right = Integer.MAX_VALUE;
                        int leftBar = 157;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int histIndex = a.getInt(i) >> bs;
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            }
                        }
                        if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (right == level) { // there is a value equal to level
                            result = cnt;
                        } else {
                            assert left < level && level < right;
                            assert leftBar > 0;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (level - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[1 << nab];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                hist[a.getInt(i) >> bs]++;
                            }
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                    + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                    + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableShortArray buf = ap == null ? null : (UpdatableShortArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getShort(k) >> bs : ra.getShort(arrayPos) >> bs;
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = hist.currentPreciseRank();
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

        //[[Repeat() getInt          ==> getLong;;
        //           int\s+getLong   ==> int getInt;;
        //           (?<!Abstract)IntArray ==> LongArray;;
        //           Integer(?!\.MAX_VALUE) ==> Long;;
        //           int(\s+[ABCD]\:|\[\]\s+ja|\s+v\b) ==> long$1;;
        //           (int\s+level)\s*=\s*([^;]*?); ==> $1 = (int)($2); ;;
        //           \(int\[\]\)(\(\(DirectAccessible) ==> (long[])$1;;
        //           (v\s*\>\>\s*bs) ==> (int)($1);;
        //           31              ==> 63 ]]
        if (src instanceof IntArray) {
            final IntArray a = (IntArray)src;
            final IntArray ra = (IntArray)rankedArray;
            final int nab = Math.min(numberOfAnalyzedBits, 30);
            final int bs = 31 - nab;
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final int[] ja = (int[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // int A: simple rank, direct
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = ra.getInt(index) >> bs;
                            if (level <= 0) {
                                return 0;
                            }
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                int histIndex = v >> bs;
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableIntArray buf = ap == null ? null : (UpdatableIntArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getInt(k) >> bs : ra.getInt(arrayPos) >> bs;
                                    if (level < 0) {
                                        level = 0;
                                    }
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = (int)hist.currentIRank();
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
                } else { // int B: precise rank, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            int level = ra.getInt(index) >> bs;
                            int left = -1, right = Integer.MAX_VALUE;
                            int leftBar = 157;
                            if (level <= 0) {
                                return 0;
                            }
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                int v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                int histIndex = v >> bs;
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                }
                            }
                            if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (right == level) { // there is a value equal to level
                                result = cnt;
                            } else {
                                assert left < level && level < right;
                                assert leftBar > 0;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (level - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[1 << nab];
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
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                        + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                        + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableIntArray buf = ap == null ? null : (UpdatableIntArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = ap != null ? buf.getInt(k) >> bs : ra.getInt(arrayPos) >> bs;
                                    if (level < 0) {
                                        level = 0;
                                    }
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
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
            } else if (!interpolated) { // int C: simple rank, indirect
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        int level = ra.getInt(index) >> bs;
                        if (level <= 0) {
                            return 0;
                        }
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int v = a.getInt(i);
                            if (v < 0) {
                                v = 0;
                            }
                            int histIndex = v >> bs;
                            if (histIndex < level) {
                                cnt++;
                            }
                        }
                        return cnt;
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
                        UpdatableIntArray buf = ap == null ? null : (UpdatableIntArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getInt(k) >> bs : ra.getInt(arrayPos) >> bs;
                                if (level < 0) {
                                    level = 0;
                                }
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = (int)hist.currentIRank();
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
            } else { // int D: precise rank, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        int level = ra.getInt(index) >> bs;
                        int left = -1, right = Integer.MAX_VALUE;
                        int leftBar = 157;
                        if (level <= 0) {
                            return 0;
                        }
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            int v = a.getInt(i);
                            if (v < 0) {
                                v = 0;
                            }
                            int histIndex = v >> bs;
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            }
                        }
                        if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (right == level) { // there is a value equal to level
                            result = cnt;
                        } else {
                            assert left < level && level < right;
                            assert leftBar > 0;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (level - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[1 << nab];
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
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                    + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                    + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableIntArray buf = ap == null ? null : (UpdatableIntArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = ap != null ? buf.getInt(k) >> bs : ra.getInt(arrayPos) >> bs;
                                if (level < 0) {
                                    level = 0;
                                }
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = hist.currentPreciseRank();
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
            final LongArray ra = (LongArray)rankedArray;
            final int nab = Math.min(numberOfAnalyzedBits, 30);
            final int bs = 63 - nab;
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final long[] ja = (long[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // long A: simple rank, direct
                    return new AbstractIntArray(src.length(), true, src) {
                        public int getInt(long index) {
                            int level = (int)(ra.getLong(index) >> bs);
                            if (level <= 0) {
                                return 0;
                            }
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                int histIndex = (int)(v >> bs);
                                if (histIndex < level) {
                                    cnt++;
                                }
                            }
                            return cnt;
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
                            UpdatableLongArray buf = ap == null ? null : (UpdatableLongArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = (int)(ap != null ? buf.getLong(k) >> bs : ra.getLong(arrayPos) >> bs);
                                    if (level < 0) {
                                        level = 0;
                                    }
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = (int)hist.currentIRank();
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
                } else { // long B: precise rank, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            int level = (int)(ra.getLong(index) >> bs);
                            int left = -1, right = Integer.MAX_VALUE;
                            int leftBar = 157;
                            if (level <= 0) {
                                return 0;
                            }
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                long v = ja[jaOfs + (int)i];
                                if (v < 0) {
                                    v = 0;
                                }
                                int histIndex = (int)(v >> bs);
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                }
                            }
                            if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (right == level) { // there is a value equal to level
                                result = cnt;
                            } else {
                                assert left < level && level < right;
                                assert leftBar > 0;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (level - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[1 << nab];
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
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                        + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                        + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableLongArray buf = ap == null ? null : (UpdatableLongArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    int level = (int)(ap != null ? buf.getLong(k) >> bs : ra.getLong(arrayPos) >> bs);
                                    if (level < 0) {
                                        level = 0;
                                    }
                                    hist.moveToIValue(level);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
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
            } else if (!interpolated) { // long C: simple rank, indirect
                return new AbstractIntArray(src.length(), true, src) {
                    public int getInt(long index) {
                        int level = (int)(ra.getLong(index) >> bs);
                        if (level <= 0) {
                            return 0;
                        }
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            long v = a.getLong(i);
                            if (v < 0) {
                                v = 0;
                            }
                            int histIndex = (int)(v >> bs);
                            if (histIndex < level) {
                                cnt++;
                            }
                        }
                        return cnt;
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
                        UpdatableLongArray buf = ap == null ? null : (UpdatableLongArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = (int)(ap != null ? buf.getLong(k) >> bs : ra.getLong(arrayPos) >> bs);
                                if (level < 0) {
                                    level = 0;
                                }
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = (int)hist.currentIRank();
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
            } else { // long D: precise rank, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        int level = (int)(ra.getLong(index) >> bs);
                        int left = -1, right = Integer.MAX_VALUE;
                        int leftBar = 157;
                        if (level <= 0) {
                            return 0;
                        }
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            long v = a.getLong(i);
                            if (v < 0) {
                                v = 0;
                            }
                            int histIndex = (int)(v >> bs);
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            }
                        }
                        if (cnt == 0 || cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (right == level) { // there is a value equal to level
                            result = cnt;
                        } else {
                            assert left < level && level < right;
                            assert leftBar > 0;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (level - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[1 << nab];
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
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - level) > 0.01 || Math.abs(p2 - level) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + level
                                    + " (range = " + left + ".." + right + ", leftBar = " + leftBar + " = "
                                    + hist[left] + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableLongArray buf = ap == null ? null : (UpdatableLongArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                int level = (int)(ap != null ? buf.getLong(k) >> bs : ra.getLong(arrayPos) >> bs);
                                if (level < 0) {
                                    level = 0;
                                }
                                hist.moveToIValue(level);
                                dest[destArrayOffset] = hist.currentPreciseRank();
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

        //[[Repeat() float ==> double;;
        //           Float ==> Double]]
        if (src instanceof FloatArray) {
            final FloatArray a = (FloatArray)src;
            final FloatArray ra = (FloatArray)rankedArray;
            final int histLength = 1 << numberOfAnalyzedBits;
            final double multiplier = histLength - 1; // only strict 1.0 is transformed to histLength-1
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final float[] ja = (float[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // float A: simple rank, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double w = ra.getDouble(index);
                            checkNaN(w);
                            if (w <= 0.0) {
                                return 0;
                            } else if (w > 1.0) {
                                return shifts.length;
                            }
                            w *= multiplier;
                            int level = (int)w;
                            int currentBar = 0;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                if (histIndex < level) {
                                    cnt++;
                                } else if (histIndex == level) {
                                    currentBar++;
                                }
                            }
                            if (cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (currentBar > 0) { // there are values equal to level
                                double indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                                result = cnt + indexInBar;
                            } else {
                                return cnt;
                                // should not be tested if DEBUG_MODE: moveToRank cannot find values between bars
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[histLength];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                                }
                                double p1 = Histogram.value(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", value is " + p1 + " or " + p2 + " instead of " + w
                                        + ", currentBar = " + currentBar + " = " + hist[level]
                                        + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableFloatArray buf = ap == null ? null : (UpdatableFloatArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                    checkNaN(w);
                                    if (w <= 0.0) {
                                        dest[destArrayOffset] = 0;
                                    } else if (w > 1.0) {
                                        dest[destArrayOffset] = shifts.length;
                                    } else {
                                        hist.moveToValue(w * multiplier);
                                        dest[destArrayOffset] = hist.currentRank();
                                    }
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
                } else { // float B: precise rank, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double w = ra.getDouble(index);
                            checkNaN(w);
                            if (w <= 0.0) {
                                return 0;
                            } else if (w > 1.0) {
                                return shifts.length;
                            }
                            w *= multiplier;
                            int level = (int)w;
                            int left = -1, right = Integer.MAX_VALUE;
                            // we use the fact that numberOfAnalyzedBits<=30, so histIndex cannot be =Integer.MAX_VALUE
                            int leftBar = 157, currentBar = 0;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else if (histIndex > level) {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                } else {
                                    currentBar++;
                                }
                            }
                            if (cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (currentBar > 0) { // there are values equal to level
                                double indexInBar;
                                if (currentBar > 1 && (indexInBar = (w - level) * currentBar) <= currentBar - 1) {
                                    result = cnt + indexInBar;
                                } else if (cnt + currentBar == shifts.length) {
                                    indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                                    return w == level ? cnt : cnt + indexInBar; // should not be tested if DEBUG_MODE
                                } else if (w == level) {
                                    assert currentBar == 1; // if >1, then indexInBar was zero
                                    result = cnt;
                                } else {
                                    assert right != Integer.MAX_VALUE; // because cnt+currentBar < shifts.length
                                    double v1 = currentBar == 1 ? level :
                                        level + (double)(currentBar - 1) / (double)currentBar;
                                    result = cnt + currentBar - 1 + (w - v1) / (right - v1);
                                }
                            } else if (cnt == 0) {
                                return 0.0; // should not be tested if DEBUG_MODE
                            } else {
                                assert left != -1; // because cnt != 0
                                assert right != Integer.MAX_VALUE; // because cnt != shifts.length
                                assert left < level && level < right;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (w - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[histLength];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                                }
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + w + " (range = "
                                        + left + ".." + right
                                        + ", leftBar = " + leftBar + " = " + hist[left]
                                        + ", currentBar = " + currentBar + " = " + hist[level]
                                        + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableFloatArray buf = ap == null ? null : (UpdatableFloatArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                    checkNaN(w);
                                    if (w <= 0.0) {
                                        dest[destArrayOffset] = 0;
                                    } else if (w > 1.0) {
                                        dest[destArrayOffset] = shifts.length;
                                    } else {
                                        hist.moveToValue(w * multiplier);
                                        dest[destArrayOffset] = hist.currentPreciseRank();
                                    }
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
            } else if (!interpolated) { // float C: simple rank, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double w = ra.getDouble(index);
                        checkNaN(w);
                        if (w <= 0.0) {
                            return 0;
                        } else if (w > 1.0) {
                            return shifts.length;
                        }
                        w *= multiplier;
                        int level = (int)w;
                        int currentBar = 0;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            if (histIndex < level) {
                                cnt++;
                            } else if (histIndex == level) {
                                currentBar++;
                            }
                        }
                        if (cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (currentBar > 0) { // there are values equal to level
                            double indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                            result = cnt + indexInBar;
                        } else {
                            return cnt;
                            // should not be tested if DEBUG_MODE: moveToRank cannot find values between bars
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                            }
                            double p1 = Histogram.value(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", value is " + p1 + " or " + p2 + " instead of " + w
                                    + ", currentBar = " + currentBar + " = " + hist[level]
                                    + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableFloatArray buf = ap == null ? null : (UpdatableFloatArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                checkNaN(w);
                                if (w <= 0.0) {
                                    dest[destArrayOffset] = 0;
                                } else if (w > 1.0) {
                                    dest[destArrayOffset] = shifts.length;
                                } else {
                                    hist.moveToValue(w * multiplier);
                                    dest[destArrayOffset] = hist.currentRank();
                                }
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
            } else { // float D: precise rank, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double w = ra.getDouble(index);
                        checkNaN(w);
                        if (w <= 0.0) {
                            return 0;
                        } else if (w > 1.0) {
                            return shifts.length;
                        }
                        w *= multiplier;
                        int level = (int)w;
                        int left = -1, right = Integer.MAX_VALUE;
                        // we use the fact that numberOfAnalyzedBits<=30, so histIndex cannot be =Integer.MAX_VALUE
                        int leftBar = 157, currentBar = 0;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else if (histIndex > level) {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            } else {
                                currentBar++;
                            }
                        }
                        if (cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (currentBar > 0) { // there are values equal to level
                            double indexInBar;
                            if (currentBar > 1 && (indexInBar = (w - level) * currentBar) <= currentBar - 1) {
                                result = cnt + indexInBar;
                            } else if (cnt + currentBar == shifts.length) {
                                indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                                return w == level ? cnt : cnt + indexInBar; // should not be tested if DEBUG_MODE
                            } else if (w == level) {
                                assert currentBar == 1; // if >1, then indexInBar was zero
                                result = cnt;
                            } else {
                                assert right != Integer.MAX_VALUE; // because cnt+currentBar < shifts.length
                                double v1 = currentBar == 1 ? level :
                                    level + (double)(currentBar - 1) / (double)currentBar;
                                result = cnt + currentBar - 1 + (w - v1) / (right - v1);
                            }
                        } else if (cnt == 0) {
                            return 0.0; // should not be tested if DEBUG_MODE
                        } else {
                            assert left != -1; // because cnt != 0
                            assert right != Integer.MAX_VALUE; // because cnt != shifts.length
                            assert left < level && level < right;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (w - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                            }
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + w + " (range = "
                                    + left + ".." + right
                                    + ", leftBar = " + leftBar + " = " + hist[left]
                                    + ", currentBar = " + currentBar + " = " + hist[level]
                                    + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableFloatArray buf = ap == null ? null : (UpdatableFloatArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                checkNaN(w);
                                if (w <= 0.0) {
                                    dest[destArrayOffset] = 0;
                                } else if (w > 1.0) {
                                    dest[destArrayOffset] = shifts.length;
                                } else {
                                    hist.moveToValue(w * multiplier);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
                                }
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
            final DoubleArray ra = (DoubleArray)rankedArray;
            final int histLength = 1 << numberOfAnalyzedBits;
            final double multiplier = histLength - 1; // only strict 1.0 is transformed to histLength-1
            final HistogramCache<Histogram> histogramCache = new HistogramCache<Histogram>();
            if (direct) {
                final double[] ja = (double[])((DirectAccessible)a).javaArray();
                final int jaOfs = ((DirectAccessible)a).javaArrayOffset();
                final JArrayPool indexesPool = JArrayPool.getInstance(int.class, left.length);
                if (!interpolated) { // double A: simple rank, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double w = ra.getDouble(index);
                            checkNaN(w);
                            if (w <= 0.0) {
                                return 0;
                            } else if (w > 1.0) {
                                return shifts.length;
                            }
                            w *= multiplier;
                            int level = (int)w;
                            int currentBar = 0;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                if (histIndex < level) {
                                    cnt++;
                                } else if (histIndex == level) {
                                    currentBar++;
                                }
                            }
                            if (cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (currentBar > 0) { // there are values equal to level
                                double indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                                result = cnt + indexInBar;
                            } else {
                                return cnt;
                                // should not be tested if DEBUG_MODE: moveToRank cannot find values between bars
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[histLength];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                                }
                                double p1 = Histogram.value(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", value is " + p1 + " or " + p2 + " instead of " + w
                                        + ", currentBar = " + currentBar + " = " + hist[level]
                                        + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableDoubleArray buf = ap == null ? null : (UpdatableDoubleArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                    checkNaN(w);
                                    if (w <= 0.0) {
                                        dest[destArrayOffset] = 0;
                                    } else if (w > 1.0) {
                                        dest[destArrayOffset] = shifts.length;
                                    } else {
                                        hist.moveToValue(w * multiplier);
                                        dest[destArrayOffset] = hist.currentRank();
                                    }
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
                } else { // double B: precise rank, direct
                    return new AbstractDoubleArray(src.length(), true, src) {
                        public double getDouble(long index) {
                            double w = ra.getDouble(index);
                            checkNaN(w);
                            if (w <= 0.0) {
                                return 0;
                            } else if (w > 1.0) {
                                return shifts.length;
                            }
                            w *= multiplier;
                            int level = (int)w;
                            int left = -1, right = Integer.MAX_VALUE;
                            // we use the fact that numberOfAnalyzedBits<=30, so histIndex cannot be =Integer.MAX_VALUE
                            int leftBar = 157, currentBar = 0;
                            int cnt = 0;
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = ja[jaOfs + (int)i];
                                int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                                if (histIndex < level) {
                                    cnt++;
                                    if (histIndex == left) {
                                        leftBar++;
                                    } else if (histIndex > left) {
                                        left = histIndex;
                                        leftBar = 1;
                                    }
                                } else if (histIndex > level) {
                                    if (histIndex < right) {
                                        right = histIndex;
                                    }
                                } else {
                                    currentBar++;
                                }
                            }
                            if (cnt == shifts.length) { // in particular, if shifts.length==0
                                return cnt;
                            }
                            assert shifts.length > 0;
                            double result;
                            if (currentBar > 0) { // there are values equal to level
                                double indexInBar;
                                if (currentBar > 1 && (indexInBar = (w - level) * currentBar) <= currentBar - 1) {
                                    result = cnt + indexInBar;
                                } else if (cnt + currentBar == shifts.length) {
                                    indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                                    return w == level ? cnt : cnt + indexInBar; // should not be tested if DEBUG_MODE
                                } else if (w == level) {
                                    assert currentBar == 1; // if >1, then indexInBar was zero
                                    result = cnt;
                                } else {
                                    assert right != Integer.MAX_VALUE; // because cnt+currentBar < shifts.length
                                    double v1 = currentBar == 1 ? level :
                                        level + (double)(currentBar - 1) / (double)currentBar;
                                    result = cnt + currentBar - 1 + (w - v1) / (right - v1);
                                }
                            } else if (cnt == 0) {
                                return 0.0; // should not be tested if DEBUG_MODE
                            } else {
                                assert left != -1; // because cnt != 0
                                assert right != Integer.MAX_VALUE; // because cnt != shifts.length
                                assert left < level && level < right;
                                double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                                // v1 is supposed precise value #(cnt-1), right is the value #cnt
                                result = cnt - 1 + (w - v1) / (right - v1);
                            }
                            if (DEBUG_MODE) {
                                long[] hist = new long[histLength];
                                for (long shift : shifts) {
                                    long i = index - shift;
                                    if (i < 0) {
                                        i += length;
                                    }
                                    double v = ja[jaOfs + (int)i];
                                    hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                                }
                                double p1 = Histogram.preciseValue(hist, result);
                                double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                    .currentValue(); // comparing with alternative, long[] histogram
                                if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                    throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                        + ", precise value is " + p1 + " or " + p2 + " instead of " + w + " (range = "
                                        + left + ".." + right
                                        + ", leftBar = " + leftBar + " = " + hist[left]
                                        + ", currentBar = " + currentBar + " = " + hist[level]
                                        + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                            }
                            return result;
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
                            UpdatableDoubleArray buf = ap == null ? null : (UpdatableDoubleArray)ap.requestArray();
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
                                    buf.copy(ra.subArr(arrayPos, len));
                                }
                                for (int k = 0; k < len; k++, destArrayOffset++) {
                                    double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                    checkNaN(w);
                                    if (w <= 0.0) {
                                        dest[destArrayOffset] = 0;
                                    } else if (w > 1.0) {
                                        dest[destArrayOffset] = shifts.length;
                                    } else {
                                        hist.moveToValue(w * multiplier);
                                        dest[destArrayOffset] = hist.currentPreciseRank();
                                    }
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
            } else if (!interpolated) { // double C: simple rank, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double w = ra.getDouble(index);
                        checkNaN(w);
                        if (w <= 0.0) {
                            return 0;
                        } else if (w > 1.0) {
                            return shifts.length;
                        }
                        w *= multiplier;
                        int level = (int)w;
                        int currentBar = 0;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            if (histIndex < level) {
                                cnt++;
                            } else if (histIndex == level) {
                                currentBar++;
                            }
                        }
                        if (cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (currentBar > 0) { // there are values equal to level
                            double indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                            result = cnt + indexInBar;
                        } else {
                            return cnt;
                            // should not be tested if DEBUG_MODE: moveToRank cannot find values between bars
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                            }
                            double p1 = Histogram.value(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", value is " + p1 + " or " + p2 + " instead of " + w
                                    + ", currentBar = " + currentBar + " = " + hist[level]
                                    + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableDoubleArray buf = ap == null ? null : (UpdatableDoubleArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                checkNaN(w);
                                if (w <= 0.0) {
                                    dest[destArrayOffset] = 0;
                                } else if (w > 1.0) {
                                    dest[destArrayOffset] = shifts.length;
                                } else {
                                    hist.moveToValue(w * multiplier);
                                    dest[destArrayOffset] = hist.currentRank();
                                }
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
            } else { // double D: precise rank, indirect
                return new AbstractDoubleArray(src.length(), true, src) {
                    public double getDouble(long index) {
                        double w = ra.getDouble(index);
                        checkNaN(w);
                        if (w <= 0.0) {
                            return 0;
                        } else if (w > 1.0) {
                            return shifts.length;
                        }
                        w *= multiplier;
                        int level = (int)w;
                        int left = -1, right = Integer.MAX_VALUE;
                        // we use the fact that numberOfAnalyzedBits<=30, so histIndex cannot be =Integer.MAX_VALUE
                        int leftBar = 157, currentBar = 0;
                        int cnt = 0;
                        for (long shift : shifts) {
                            long i = index - shift;
                            if (i < 0) {
                                i += length;
                            }
                            double v = a.getDouble(i);
                            int histIndex = v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier);
                            if (histIndex < level) {
                                cnt++;
                                if (histIndex == left) {
                                    leftBar++;
                                } else if (histIndex > left) {
                                    left = histIndex;
                                    leftBar = 1;
                                }
                            } else if (histIndex > level) {
                                if (histIndex < right) {
                                    right = histIndex;
                                }
                            } else {
                                currentBar++;
                            }
                        }
                        if (cnt == shifts.length) { // in particular, if shifts.length==0
                            return cnt;
                        }
                        assert shifts.length > 0;
                        double result;
                        if (currentBar > 0) { // there are values equal to level
                            double indexInBar;
                            if (currentBar > 1 && (indexInBar = (w - level) * currentBar) <= currentBar - 1) {
                                result = cnt + indexInBar;
                            } else if (cnt + currentBar == shifts.length) {
                                indexInBar = currentBar == 1 ? w - level : (w - level) * currentBar;
                                return w == level ? cnt : cnt + indexInBar; // should not be tested if DEBUG_MODE
                            } else if (w == level) {
                                assert currentBar == 1; // if >1, then indexInBar was zero
                                result = cnt;
                            } else {
                                assert right != Integer.MAX_VALUE; // because cnt+currentBar < shifts.length
                                double v1 = currentBar == 1 ? level :
                                    level + (double)(currentBar - 1) / (double)currentBar;
                                result = cnt + currentBar - 1 + (w - v1) / (right - v1);
                            }
                        } else if (cnt == 0) {
                            return 0.0; // should not be tested if DEBUG_MODE
                        } else {
                            assert left != -1; // because cnt != 0
                            assert right != Integer.MAX_VALUE; // because cnt != shifts.length
                            assert left < level && level < right;
                            double v1 = left + (double)(leftBar - 1) / (double)leftBar;
                            // v1 is supposed precise value #(cnt-1), right is the value #cnt
                            result = cnt - 1 + (w - v1) / (right - v1);
                        }
                        if (DEBUG_MODE) {
                            long[] hist = new long[histLength];
                            for (long shift : shifts) {
                                long i = index - shift;
                                if (i < 0) {
                                    i += length;
                                }
                                double v = a.getDouble(i);
                                hist[v < 0.0 ? 0 : v >= 1.0 ? histLength - 1 : (int)(v * multiplier)]++;
                            }
                            double p1 = Histogram.preciseValue(hist, result);
                            double p2 = Histogram.newLongHistogram(hist).moveToPreciseRank(result)
                                .currentValue(); // comparing with alternative, long[] histogram
                            if (Math.abs(p1 - w) > 0.01 || Math.abs(p2 - w) > 0.01)
                                throw new AssertionError("Bug: for found rank " + result + " at index " + index
                                    + ", precise value is " + p1 + " or " + p2 + " instead of " + w + " (range = "
                                    + left + ".." + right
                                    + ", leftBar = " + leftBar + " = " + hist[left]
                                    + ", currentBar = " + currentBar + " = " + hist[level]
                                    + ", histogram is " + JArrays.toString(hist, ",", 2048) + ")");
                        }
                        return result;
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
                        UpdatableDoubleArray buf = ap == null ? null : (UpdatableDoubleArray)ap.requestArray();
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
                                buf.copy(ra.subArr(arrayPos, len));
                            }
                            for (int k = 0; k < len; k++, destArrayOffset++) {
                                double w = ap != null ? buf.getDouble(k) : ra.getDouble(arrayPos);
                                checkNaN(w);
                                if (w <= 0.0) {
                                    dest[destArrayOffset] = 0;
                                } else if (w > 1.0) {
                                    dest[destArrayOffset] = shifts.length;
                                } else {
                                    hist.moveToValue(w * multiplier);
                                    dest[destArrayOffset] = hist.currentPreciseRank();
                                }
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
            throw new IllegalArgumentException("Illegal value (NaN) in some ranked elements");
    }
}
