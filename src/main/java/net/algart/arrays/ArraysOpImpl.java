/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays;

import java.util.Locale;
import java.util.Objects;

/**
 * <p>Implementations of some {@link Arrays} methods.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysOpImpl {
    static class ComparingCopier extends Arrays.ParallelExecutor {
        private static final MemoryModel smm = SimpleMemoryModel.getInstance();
        private static final int BIT_BLOCK_SIZE = 8 * 65536;
        private static final int BLOCK_SIZE = 32768;
        private static final ArrayPool BIT_ARRAYS = ArrayPool.getInstance(smm, boolean.class, BIT_BLOCK_SIZE);
        private static final ArrayPool CHAR_ARRAYS = ArrayPool.getInstance(smm, char.class, BLOCK_SIZE);
        private static final ArrayPool BYTE_ARRAYS = ArrayPool.getInstance(smm, byte.class, BLOCK_SIZE);
        private static final ArrayPool SHORT_ARRAYS = ArrayPool.getInstance(smm, short.class, BLOCK_SIZE);
        private static final ArrayPool INT_ARRAYS = ArrayPool.getInstance(smm, int.class, BLOCK_SIZE);
        private static final ArrayPool LONG_ARRAYS = ArrayPool.getInstance(smm, long.class, BLOCK_SIZE);
        private static final ArrayPool FLOAT_ARRAYS = ArrayPool.getInstance(smm, float.class, BLOCK_SIZE);
        private static final ArrayPool DOUBLE_ARRAYS = ArrayPool.getInstance(smm, double.class, BLOCK_SIZE);

        private final ArrayPool arrayPool;
        volatile boolean changed = false;

        public ComparingCopier(ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks) {
            super(context,
                src.length() <= dest.length() ? dest.subArr(0, src.length()) : dest,
                src.length() <= dest.length() ? src : src.subArr(0, dest.length()),
                src instanceof BitArray ? BIT_BLOCK_SIZE : BLOCK_SIZE,
                numberOfTasks, 0);
            if (!dest.elementType().isAssignableFrom(src.elementType()))
                // this check is necessary in a case of empty arrays
            {
                throw new IllegalArgumentException("Element types mismatch ("
                    + dest.elementType() + " and " + src.elementType() + ")");
            }
            this.arrayPool =
                dest instanceof BitArray ? BIT_ARRAYS :
                dest instanceof CharArray ? CHAR_ARRAYS :
                dest instanceof ByteArray ? BYTE_ARRAYS :
                dest instanceof ShortArray ? SHORT_ARRAYS :
                dest instanceof IntArray ? INT_ARRAYS :
                dest instanceof LongArray ? LONG_ARRAYS :
                dest instanceof FloatArray ? FLOAT_ARRAYS :
                dest instanceof DoubleArray ? DOUBLE_ARRAYS :
                null;
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            UpdatableArray buf = arrayPool == null ?
                SimpleMemoryModel.getInstance().newUnresizableArray(dest.elementType(), this.blockSize) :
                arrayPool.requestArray();
            try {
                UpdatableArray subBuf = buf.subArr(0, count);
                Array subSrc = src.subArr(position, count);
                subBuf.copy(subSrc);
                UpdatableArray subDest = dest.subArr(position, count);
                if (!subDest.equals(subBuf)) {
                    changed = true;
                    subDest.copy(subBuf);
                }
            } finally {
                if (arrayPool != null) {
                    arrayPool.releaseArray(buf);
                }
            }
        }
    }

    static class RangeCalculator extends Arrays.ParallelExecutor {
        private final DataBuffer[] buffers;
        private final Arrays.MinMaxInfo result;
        private long indexOfMin = -1;
        private long indexOfMax = -1;
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;

        public RangeCalculator(ArrayContext context, PArray src, Arrays.MinMaxInfo result) {
            super(context, null, src, AbstractArray.largeBufferCapacity(src), 0, 0);
            this.buffers = new DataBuffer[this.numberOfTasks];
            this.result = result;
        }

        @Override
        public void process() {
            if (src.isEmpty()) {
                result.setEmpty();
            } else if (src instanceof BitArray) {
                // usually very quick algorithm, excepting very long constant bit arrays
                boolean v0 = ((BitArray)src).getBit(0);
                if (v0) {
                    indexOfMax = 0;
                } else {
                    indexOfMin = 0;
                }
                DataBuffer buf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Loop:for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    long[] ja = (long[])buf.data();
                    for (long kMin = buf.fromIndex(), kMax = buf.toIndex(), k = kMin; k < kMax; k++) {
                        boolean v = PackedBitArrays.getBit(ja, k);
                        if (v != v0) {
                            if (v) {
                                indexOfMax = buf.position() + (k - kMin);
                            } else {
                                indexOfMin = buf.position() + (k - kMin);
                            }
                            break Loop;
                        }
                    }
                }
                assert indexOfMin != -1 || indexOfMax != -1;
                if (indexOfMin == -1) // no zero bits: so, the first 1 is both minimum and maximum
                {
                    indexOfMin = 0;
                }
                if (indexOfMax == -1) // no unit bits: so, the first 0 is both minimum and maximum
                {
                    indexOfMax = 0;
                }
                finish();
            } else {
                super.process();
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer buf = buffers[threadIndex];
            if (buf == null) {
                buf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(buf);
                buffers[threadIndex] = buf;
            }
            buf.map(position, count);
            long index = position;
            if (src instanceof BitArray) {
                throw new AssertionError("Illegal usage");
                //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                //           int\s+v\s*=\s*(.*?)\s+&\s+0xFF; ==> int v = ($1);,,int v = ($1) & 0xFFFF;,,
                //               int v = $1;,,long v = $1;,,float v = $1;,,double v = $1; ;;
                //           (Integer.MAX_VALUE)      ==> $1,,$1,,$1,,Long.MAX_VALUE,,
                //               Float.POSITIVE_INFINITY,,Double.POSITIVE_INFINITY;;
                //           (Integer.MIN_VALUE)      ==> $1,,$1,,$1,,Long.MIN_VALUE,,
                //               Float.NEGATIVE_INFINITY,,Double.NEGATIVE_INFINITY;;
                //            int\s+(min|max)          ==> int $1,,int $1,,int $1,,long $1,,float $1,,double $1]]
            } else if (src instanceof ByteArray) {
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                long indexOfMin = -1, indexOfMax = -1;
                byte[] ja = (byte[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    int v = ja[k] & 0xFF;
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (src instanceof CharArray) {
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                long indexOfMin = -1, indexOfMax = -1;
                char[] ja = (char[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    int v = (ja[k]);
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
            } else if (src instanceof ShortArray) {
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                long indexOfMin = -1, indexOfMax = -1;
                short[] ja = (short[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    int v = (ja[k]) & 0xFFFF;
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
            } else if (src instanceof IntArray) {
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                long indexOfMin = -1, indexOfMax = -1;
                int[] ja = (int[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    int v = ja[k];
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
            } else if (src instanceof LongArray) {
                long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
                long indexOfMin = -1, indexOfMax = -1;
                long[] ja = (long[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    long v = ja[k];
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
            } else if (src instanceof FloatArray) {
                float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
                long indexOfMin = -1, indexOfMax = -1;
                float[] ja = (float[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    float v = ja[k];
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
            } else if (src instanceof DoubleArray) {
                double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                long indexOfMin = -1, indexOfMax = -1;
                double[] ja = (double[])buf.data();
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                    double v = ja[k];
                    if (v < min) {
                        min = v; indexOfMin = index;
                    }
                    if (v > max) {
                        max = v; indexOfMax = index;
                    }
                }
                synchronized(this) {
                    // - note: we must also check indexes, for a case of using several parallel threads
                    if (min < this.min || (min == this.min && indexOfMin < this.indexOfMin)) {
                        this.indexOfMin = indexOfMin; this.min = min;
                    }
                    if (max > this.max || (max == this.max && indexOfMax < this.indexOfMax)) {
                        this.indexOfMax = indexOfMax; this.max = max;
                    }
                }
                //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
        }

        @Override
        protected synchronized void finish() {
            if (!src.isEmpty()) {
                result.setAll(indexOfMin, indexOfMax, (PArray) src);
            }
            for (DataBuffer buf : buffers) {
                Arrays.dispose(buf);
            }
        }
    }

    static class Summator extends Arrays.ParallelExecutor {
        private final DataBuffer[] buffers;
        private double result = 0;

        public Summator(ArrayContext context, PArray src) {
            super(context, null, src,
                Math.min(AbstractArray.largeBufferCapacity(src), src instanceof IntArray ? 32768 : 65536),
                1, 1);
            // Very important! Here we MUST use only 1 thread to provide strict order of summing!
            // In another case, we have a risk to produce different results
            // even while different calls: the result of floating-point sum depends on the summing order.
            this.buffers = new DataBuffer[this.numberOfTasks];
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer buf = buffers[threadIndex];
            if (buf == null) {
                buf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(buf);
                buffers[threadIndex] = buf;
            }
            buf.map(position, count);
            assert buf.count() == count;
            if (src instanceof BitArray) {
                long card = PackedBitArrays.cardinality((long[])buf.data(), buf.fromIndex(), buf.toIndex());
                synchronized(this) {
                    result += card;
                }
            } else if (src instanceof ByteArray) {
                byte[] ja = (byte[])buf.data();
                int sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k] & 0xFF;
                }
                synchronized(this) {
                    result += sum;
                }
            } else if (src instanceof CharArray) {
                char[] ja = (char[])buf.data();
                int sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    result += sum;
                }
            } else if (src instanceof ShortArray) {
                short[] ja = (short[])buf.data();
                int sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k] & 0xFFFF;
                }
                synchronized(this) {
                    result += sum;
                }
            } else if (src instanceof IntArray) {
                int[] ja = (int[])buf.data();
                long sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    result += sum;
                }
            } else if (src instanceof LongArray) {
                long[] ja = (long[])buf.data();
                double sum = 0.0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    result += sum;
                }
            } else if (src instanceof FloatArray) {
                float[] ja = (float[])buf.data();
                double sum = 0.0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    result += sum;
                }
            } else if (src instanceof DoubleArray) {
                double[] ja = (double[])buf.data();
                double sum = 0.0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    result += sum;
                }
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
        }

        @Override
        protected void finish() {
            for (DataBuffer buf : buffers) {
                Arrays.dispose(buf);
            }
        }

        public synchronized double result() {
            return result;
        }
    }

    static class PreciseSummator extends Arrays.ParallelExecutor {
        private final boolean checkOverflow;
        private final DataBuffer[] buffers;
        private long result = 0;

        public PreciseSummator(ArrayContext context, PFixedArray src, boolean checkOverflow) {
            super(context, null, src,
                Math.min(AbstractArray.largeBufferCapacity(src), src instanceof IntArray ? 32768 : 65536),
                0, 0);
            // 65536 * maximal possible element is a correct int for byte, char, short element type
            // 32768 * maximal possible element is a correct long for int element type
            this.checkOverflow = checkOverflow;
            this.buffers = new DataBuffer[this.numberOfTasks];
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer buf = buffers[threadIndex];
            if (buf == null) {
                buf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(buf);
                buffers[threadIndex] = buf;
            }
            buf.map(position, count);
            assert buf.count() == count;
            if (src instanceof BitArray) {
                long card = PackedBitArrays.cardinality((long[])buf.data(), buf.fromIndex(), buf.toIndex());
                synchronized(this) {
                    result += card;
                }
            } else if (src instanceof ByteArray) {
                byte[] ja = (byte[])buf.data();
                int sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k] & 0xFF;
                }
                synchronized(this) {
                    result += sum;
                    if (checkOverflow && result < 0) {
                        throw new ArithmeticException("Overflow while sum calculation");
                    }
                }
            } else if (src instanceof CharArray) {
                char[] ja = (char[])buf.data();
                int sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    result += sum;
                    if (checkOverflow && result < 0) {
                        throw new ArithmeticException("Overflow while sum calculation");
                    }
                }
            } else if (src instanceof ShortArray) {
                short[] ja = (short[])buf.data();
                int sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k] & 0xFFFF;
                }
                synchronized(this) {
                    result += sum;
                    if (checkOverflow && result < 0) {
                        throw new ArithmeticException("Overflow while sum calculation");
                    }
                }
            } else if (src instanceof IntArray) {
                int[] ja = (int[])buf.data();
                long sum = 0;
                for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                    sum += ja[k];
                }
                synchronized(this) {
                    if (checkOverflow && (sum > 0 ? result > 0 && result + sum <= 0 : result < 0 && result + sum >= 0)) {
                        throw new ArithmeticException("Overflow while sum calculation");
                    }
                    result += sum;
                }
            } else if (src instanceof LongArray) {
                long[] ja = (long[])buf.data();
                long sum = 0;
                if (checkOverflow) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        if (ja[k] > 0 ? sum > 0 && sum + ja[k] <= 0 : sum < 0 && sum + ja[k] >= 0) {
                            throw new ArithmeticException("Overflow while sum calculation");
                        }
                        sum += ja[k];
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        sum += ja[k];
                    }
                }
                synchronized(this) {
                    if (checkOverflow && (sum > 0 ? result > 0 && result + sum <= 0 : result < 0 && result + sum >= 0)) {
                        throw new ArithmeticException("Overflow while sum calculation");
                    }
                    result += sum;
                }
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
        }

        @Override
        protected void finish() {
            for (DataBuffer buf : buffers) {
                Arrays.dispose(buf);
            }
        }

        public synchronized long result() {
            return result;
        }
    }

    static class HistogramCalculator extends Arrays.ParallelExecutor {
        private final DataBuffer[] buffers;
        private final long[] histogram;
        private final long[][] histograms;
        private final double from, to, multiplier;
        private long cardinality = 0;
        boolean allInside = true;

        public HistogramCalculator(ArrayContext context, PArray src, long[] histogram, double from, double to) {
            super(context, null, src, AbstractArray.largeBufferCapacity(src), 0, 0);
            if (histogram.length == 0) {
                throw new AssertionError("Empty histogram");
            }
            this.buffers = new DataBuffer[this.numberOfTasks];
            this.histogram = histogram;
            this.histograms = new long[this.numberOfTasks][histogram.length]; // zero-filled
            this.from = from;
            this.to = to;
            this.multiplier = histogram.length / (to - from);
        }


        @Override
        public void process() {
            if (from >= to) {
                allInside = false;
            } else {
                super.process();
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer buf = buffers[threadIndex];
            long[] hist = histograms[threadIndex];
            boolean outside = false;
            if (buf == null) {
                buf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(buf);
                buffers[threadIndex] = buf;
            }
            buf.map(position, count);
            if (src instanceof BitArray) {
                long card = PackedBitArrays.cardinality((long[])buf.data(), buf.fromIndex(), buf.toIndex());
                synchronized(this) {
                    cardinality += card;
                }
                //[[Repeat() byte ==> char,,short,,int;;
                //           Byte ==> Char,,Short,,Int;;
                //          (\s+&\s+0xFF) ==> ,,$1FF,,]]
            } else if (src instanceof ByteArray) {
                byte[] ja = (byte[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        int m = ja[k] & 0xFF;
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k] & 0xFF;
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (src instanceof CharArray) {
                char[] ja = (char[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        int m = ja[k];
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
            } else if (src instanceof ShortArray) {
                short[] ja = (short[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        int m = ja[k] & 0xFFFF;
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k] & 0xFFFF;
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
            } else if (src instanceof IntArray) {
                int[] ja = (int[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        int m = ja[k];
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
                //[[Repeat.AutoGeneratedEnd]]
                //[[Repeat() long ==> float,,double;;
                //           Long ==> Float,,Double]]
            } else if (src instanceof LongArray) {
                long[] ja = (long[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        int m = (int)v;
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (src instanceof FloatArray) {
                float[] ja = (float[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        int m = (int)v;
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
            } else if (src instanceof DoubleArray) {
                double[] ja = (double[])buf.data();
                if (from == 0.0 && multiplier == 1.0) {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        int m = (int)v;
                        if (m >= 0 && m < hist.length) {
                            hist[m]++;
                        } else {
                            outside = true;
                        }
                    }
                } else {
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                        double v = ja[k];
                        v = (v - from) * multiplier;
                        int m = (int)v;
                        if (m > 0 && m < hist.length) {
                            hist[m]++;
                        } else if (m == 0 && v >= 0.0) {
                            hist[0]++;
                        } else {
                            outside = true;
                        }
                    }
                }
                //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            synchronized(this) {
                if (outside) {
                    allInside = false;
                }
                // synchronization also guarantees that the main thread will see correct data in finish()
            }
        }

        @Override
        protected void finish() {
            for (DataBuffer buf : buffers) {
                Arrays.dispose(buf);
            }
            synchronized(this) {
                if (src instanceof BitArray) {
                    int m0 = (int)StrictMath.floor(-from * multiplier);
                    if (m0 >= 0 && m0 < histogram.length) {
                        histogram[m0] += src.length() - cardinality;
                    } else {
                        if (cardinality < src.length()) { // there is at least one 0 bit
                            allInside = false;
                        }
                    }
                    int m1 = (int)StrictMath.floor((1.0 - from) * multiplier);
                    if (m1 >= 0 && m1 < histogram.length) {
                        histogram[m1] += cardinality;
                    } else {
                        if (cardinality > 0) { // there is at least one 1 bit
                            allInside = false;
                        }
                    }

                } else {
                    for (int threadIndex = 0; threadIndex < numberOfTasks; threadIndex++) {
                        long[] hist = histograms[threadIndex];
                        for (int k = 0; k < histogram.length; k++) {
                            histogram[k] += hist[k];
                        }
                    }
                }
            }
        }
    }

    /*Repeat.SectionStart BitsPacker*/
    //[[Repeat.SectionStart BitsGreaterPacker_commonPart]]
    static class BitsGreaterPacker extends Arrays.ParallelExecutor {
        private final UpdatableBitArray bits;
        private final DataBuffer[] srcBuffers;
        private final DataBitBuffer[] destBuffers;
        private final double threshold;

        public BitsGreaterPacker(ArrayContext context, UpdatableBitArray bits, PArray array, double threshold) {
            super(context, bits, array, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null bits argument");
            this.bits = bits;
            this.threshold = threshold;
            this.srcBuffers = new DataBuffer[this.numberOfTasks];
            this.destBuffers = new DataBitBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBitBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = (DataBitBuffer)Arrays.bufferInternal(bits,
                    DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();
            //[[Repeat.SectionEnd BitsGreaterPacker_commonPart]]
            if (src instanceof BitArray) {
                if (threshold >= 1.0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= 0.0) {
                    PackedBitArrays.copyBits(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            //[[Repeat() \(char\)(StrictMath.floor\(threshold\)) ==>
            //                                   (int)$1,,(int)$1,,(int)$1,,(long)$1,,(float)threshold,,threshold;;
            //           Character.MAX_VALUE ==> 0xFF,,0xFFFF,,Integer.MAX_VALUE,,Long.MAX_VALUE,,
            //                                   Float.POSITIVE_INFINITY,,Double.POSITIVE_INFINITY;;
            //           Character.MIN_VALUE ==> 0,,0,,Integer.MIN_VALUE,,Long.MIN_VALUE,,
            //                                   Float.NEGATIVE_INFINITY,,Double.NEGATIVE_INFINITY;;
            //           char ==> byte,,short,,int,,long,,float,,double;;
            //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double]]
            } else if (src instanceof CharArray) {
                if (threshold >= Character.MAX_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= Character.MIN_VALUE) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (char[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (char)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (src instanceof ByteArray) {
                if (threshold >= 0xFF) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= 0) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (byte[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof ShortArray) {
                if (threshold >= 0xFFFF) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= 0) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (short[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof IntArray) {
                if (threshold >= Integer.MAX_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= Integer.MIN_VALUE) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (int[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof LongArray) {
                if (threshold >= Long.MAX_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= Long.MIN_VALUE) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (long)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof FloatArray) {
                if (threshold >= Float.POSITIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= Float.NEGATIVE_INFINITY) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (float[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (float)threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof DoubleArray) {
                if (threshold >= Double.POSITIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold >= Double.NEGATIVE_INFINITY) {
                    PackedBitArrays.packBitsGreater(destBuf.data(), destBuf.fromIndex(),
                        (double[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, BitsGreaterPacker_commonPart)
    //  Greater ==> Less   !! Auto-generated: NOT EDIT !! ]]
    static class BitsLessPacker extends Arrays.ParallelExecutor {
        private final UpdatableBitArray bits;
        private final DataBuffer[] srcBuffers;
        private final DataBitBuffer[] destBuffers;
        private final double threshold;

        public BitsLessPacker(ArrayContext context, UpdatableBitArray bits, PArray array, double threshold) {
            super(context, bits, array, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null bits argument");
            this.bits = bits;
            this.threshold = threshold;
            this.srcBuffers = new DataBuffer[this.numberOfTasks];
            this.destBuffers = new DataBitBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBitBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = (DataBitBuffer)Arrays.bufferInternal(bits,
                    DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();
            //[[Repeat.IncludeEnd]]
            if (src instanceof BitArray) {
                if (threshold <= 0.0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= 1.0) {
                    PackedBitArrays.notBits(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            //[[Repeat() \(char\)(StrictMath.ceil\(threshold\)) ==>
            //                                   (int)$1,,(int)$1,,(int)$1,,(long)$1,,(float)threshold,,threshold;;
            //           Character.MAX_VALUE ==> 0xFF,,0xFFFF,,Integer.MAX_VALUE,,Long.MAX_VALUE,,
            //                                   Float.POSITIVE_INFINITY,,Double.POSITIVE_INFINITY;;
            //           Character.MIN_VALUE ==> 0,,0,,Integer.MIN_VALUE,,Long.MIN_VALUE,,
            //                                   Float.NEGATIVE_INFINITY,,Double.NEGATIVE_INFINITY;;
            //           char ==> byte,,short,,int,,long,,float,,double;;
            //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double]]
            } else if (src instanceof CharArray) {
                if (threshold <= Character.MIN_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= Character.MAX_VALUE) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (char[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (char)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (src instanceof ByteArray) {
                if (threshold <= 0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= 0xFF) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (byte[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof ShortArray) {
                if (threshold <= 0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= 0xFFFF) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (short[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof IntArray) {
                if (threshold <= Integer.MIN_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= Integer.MAX_VALUE) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (int[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof LongArray) {
                if (threshold <= Long.MIN_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= Long.MAX_VALUE) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (long)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof FloatArray) {
                if (threshold <= Float.NEGATIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= Float.POSITIVE_INFINITY) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (float[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (float)threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof DoubleArray) {
                if (threshold <= Double.NEGATIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold <= Double.POSITIVE_INFINITY) {
                    PackedBitArrays.packBitsLess(destBuf.data(), destBuf.fromIndex(),
                        (double[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }
    /*Repeat.SectionEnd BitsPacker*/

    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, BitsPacker)
      (Greater|Less) ==> $1OrEqual ;;
      ceil ==> TTTT ;;
      floor ==> ceil ;;
      TTTT ==> floor ;;
      (threshold\s*[><])= ==> $1   !! Auto-generated: NOT EDIT !! */

    static class BitsGreaterOrEqualPacker extends Arrays.ParallelExecutor {
        private final UpdatableBitArray bits;
        private final DataBuffer[] srcBuffers;
        private final DataBitBuffer[] destBuffers;
        private final double threshold;

        public BitsGreaterOrEqualPacker(ArrayContext context, UpdatableBitArray bits, PArray array, double threshold) {
            super(context, bits, array, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null bits argument");
            this.bits = bits;
            this.threshold = threshold;
            this.srcBuffers = new DataBuffer[this.numberOfTasks];
            this.destBuffers = new DataBitBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBitBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = (DataBitBuffer)Arrays.bufferInternal(bits,
                    DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();

            if (src instanceof BitArray) {
                if (threshold > 1.0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > 0.0) {
                    PackedBitArrays.copyBits(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }

            } else if (src instanceof CharArray) {
                if (threshold > Character.MAX_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > Character.MIN_VALUE) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (char[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (char)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }

            } else if (src instanceof ByteArray) {
                if (threshold > 0xFF) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > 0) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (byte[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof ShortArray) {
                if (threshold > 0xFFFF) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > 0) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (short[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof IntArray) {
                if (threshold > Integer.MAX_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > Integer.MIN_VALUE) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (int[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof LongArray) {
                if (threshold > Long.MAX_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > Long.MIN_VALUE) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (long)StrictMath.ceil(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof FloatArray) {
                if (threshold > Float.POSITIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > Float.NEGATIVE_INFINITY) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (float[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (float)threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof DoubleArray) {
                if (threshold > Double.POSITIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold > Double.NEGATIVE_INFINITY) {
                    PackedBitArrays.packBitsGreaterOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (double[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }

            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }


    static class BitsLessOrEqualPacker extends Arrays.ParallelExecutor {
        private final UpdatableBitArray bits;
        private final DataBuffer[] srcBuffers;
        private final DataBitBuffer[] destBuffers;
        private final double threshold;

        public BitsLessOrEqualPacker(ArrayContext context, UpdatableBitArray bits, PArray array, double threshold) {
            super(context, bits, array, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null bits argument");
            this.bits = bits;
            this.threshold = threshold;
            this.srcBuffers = new DataBuffer[this.numberOfTasks];
            this.destBuffers = new DataBitBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBitBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = (DataBitBuffer)Arrays.bufferInternal(bits,
                    DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();

            if (src instanceof BitArray) {
                if (threshold < 0.0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < 1.0) {
                    PackedBitArrays.notBits(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }

            } else if (src instanceof CharArray) {
                if (threshold < Character.MIN_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < Character.MAX_VALUE) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (char[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (char)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }

            } else if (src instanceof ByteArray) {
                if (threshold < 0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < 0xFF) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (byte[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof ShortArray) {
                if (threshold < 0) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < 0xFFFF) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (short[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof IntArray) {
                if (threshold < Integer.MIN_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < Integer.MAX_VALUE) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (int[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (int)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof LongArray) {
                if (threshold < Long.MIN_VALUE) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < Long.MAX_VALUE) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (long[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (long)StrictMath.floor(threshold));
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof FloatArray) {
                if (threshold < Float.NEGATIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < Float.POSITIVE_INFINITY) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (float[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), (float)threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }
            } else if (src instanceof DoubleArray) {
                if (threshold < Double.NEGATIVE_INFINITY) {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), false);
                } else if (threshold < Double.POSITIVE_INFINITY) {
                    PackedBitArrays.packBitsLessOrEqual(destBuf.data(), destBuf.fromIndex(),
                        (double[])srcBuf.data(), srcBuf.from(), srcBuf.cnt(), threshold);
                } else {
                    PackedBitArrays.fillBits(destBuf.data(), destBuf.fromIndex(), srcBuf.count(), true);
                }

            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }
    /*Repeat.IncludeEnd*/

    //[[Repeat.SectionStart BitsUnpacker_commonPart]]
    static class UnitBitsUnpacker extends Arrays.ParallelExecutor {
        private final DataBitBuffer[] srcBuffers;
        private final DataBuffer[] destBuffers;
        private final double filler;

        public UnitBitsUnpacker(ArrayContext context, UpdatablePArray array, BitArray bits, double filler) {
            super(context, array, bits, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null array argument");
            this.filler = filler;
            this.srcBuffers = new DataBitBuffer[this.numberOfTasks];
            this.destBuffers = new DataBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBitBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = (DataBitBuffer)Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = Arrays.bufferInternal(dest, DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();
            //[[Repeat.SectionEnd BitsUnpacker_commonPart]]
            if (dest instanceof BitArray) {
                if (filler != 0.0) {
                    PackedBitArrays.orBits((long[])destBuf.data(), destBuf.fromIndex(),
                        srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                } else {
                    PackedBitArrays.andNotBits((long[])destBuf.data(), destBuf.fromIndex(),
                        srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                }
            //[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
            //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double;;
            //           \(double\) ==> ,,... ]]
            } else if (dest instanceof CharArray) {
                PackedBitArrays.unpackUnitBits((char[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (char)filler);
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (dest instanceof ByteArray) {
                PackedBitArrays.unpackUnitBits((byte[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (byte)filler);
            } else if (dest instanceof ShortArray) {
                PackedBitArrays.unpackUnitBits((short[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (short)filler);
            } else if (dest instanceof IntArray) {
                PackedBitArrays.unpackUnitBits((int[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (int)filler);
            } else if (dest instanceof LongArray) {
                PackedBitArrays.unpackUnitBits((long[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (long)filler);
            } else if (dest instanceof FloatArray) {
                PackedBitArrays.unpackUnitBits((float[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (float)filler);
            } else if (dest instanceof DoubleArray) {
                PackedBitArrays.unpackUnitBits((double[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), filler);
            //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, BitsUnpacker_commonPart)
    //  UnitBits ==> ZeroBits   !! Auto-generated: NOT EDIT !! ]]
    static class ZeroBitsUnpacker extends Arrays.ParallelExecutor {
        private final DataBitBuffer[] srcBuffers;
        private final DataBuffer[] destBuffers;
        private final double filler;

        public ZeroBitsUnpacker(ArrayContext context, UpdatablePArray array, BitArray bits, double filler) {
            super(context, array, bits, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null array argument");
            this.filler = filler;
            this.srcBuffers = new DataBitBuffer[this.numberOfTasks];
            this.destBuffers = new DataBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBitBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = (DataBitBuffer)Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = Arrays.bufferInternal(dest, DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();
            //[[Repeat.IncludeEnd]]
            if (dest instanceof BitArray) {
                if (filler != 0.0) {
                    PackedBitArrays.orNotBits((long[])destBuf.data(), destBuf.fromIndex(),
                        srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                } else {
                    PackedBitArrays.andBits((long[])destBuf.data(), destBuf.fromIndex(),
                        srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                }
            //[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
            //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double;;
            //           \(double\) ==> ,,... ]]
            } else if (dest instanceof CharArray) {
                PackedBitArrays.unpackZeroBits((char[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (char)filler);
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (dest instanceof ByteArray) {
                PackedBitArrays.unpackZeroBits((byte[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (byte)filler);
            } else if (dest instanceof ShortArray) {
                PackedBitArrays.unpackZeroBits((short[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (short)filler);
            } else if (dest instanceof IntArray) {
                PackedBitArrays.unpackZeroBits((int[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (int)filler);
            } else if (dest instanceof LongArray) {
                PackedBitArrays.unpackZeroBits((long[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (long)filler);
            } else if (dest instanceof FloatArray) {
                PackedBitArrays.unpackZeroBits((float[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (float)filler);
            } else if (dest instanceof DoubleArray) {
                PackedBitArrays.unpackZeroBits((double[])destBuf.data(), destBuf.from(),
                    srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), filler);
            //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }

    static class BothBitsUnpacker extends Arrays.ParallelExecutor {
        private final DataBitBuffer[] srcBuffers;
        private final DataBuffer[] destBuffers;
        private final double filler0;
        private final double filler1;

        public BothBitsUnpacker(
                ArrayContext context, UpdatablePArray array, BitArray bits,
                double filler0, double filler1) {
            super(context, array, bits, Math.min(AbstractArray.largeBufferCapacity(array), 32768), 0, 0);
            Objects.requireNonNull(bits, "Null array argument");
            this.filler0 = filler0;
            this.filler1 = filler1;
            this.srcBuffers = new DataBitBuffer[this.numberOfTasks];
            this.destBuffers = new DataBuffer[this.numberOfTasks];
        }

        @Override
        protected void finish() {
            for (DataBuffer destBuf : destBuffers) {
                Arrays.dispose(destBuf);
            }
            for (DataBuffer srcBuf : srcBuffers) {
                Arrays.dispose(srcBuf);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            DataBitBuffer srcBuf = srcBuffers[threadIndex];
            if (srcBuf == null) {
                srcBuf = (DataBitBuffer)Arrays.bufferInternal(src, DataBuffer.AccessMode.READ, blockSize, true);
                Arrays.enableCaching(srcBuf);
                srcBuffers[threadIndex] = srcBuf;
            }
            DataBuffer destBuf = destBuffers[threadIndex];
            if (destBuf == null) {
                destBuf = Arrays.bufferInternal(dest, DataBuffer.AccessMode.READ_WRITE, blockSize, true);
                Arrays.enableCaching(destBuf);
                destBuffers[threadIndex] = destBuf;
            }
            srcBuf.map(position, count);
            destBuf.map(position, count);
            assert srcBuf.count() == destBuf.count();
            if (dest instanceof BitArray) {
                boolean b0 = filler0 != 0.0;
                boolean b1 = filler1 != 0.0;
                if (b0 == b1) {
                    PackedBitArrays.fillBits((long[])destBuf.data(), destBuf.fromIndex(), srcBuf.count(), b0);
                } else if (b1) {
                    assert !b0;
                    PackedBitArrays.copyBits((long[])destBuf.data(), destBuf.fromIndex(),
                            srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                }  else {
                    assert b0;
                    PackedBitArrays.notBits((long[])destBuf.data(), destBuf.fromIndex(),
                            srcBuf.data(), srcBuf.fromIndex(), srcBuf.count());
                }
                //[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
                //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double;;
                //           \(double\) ==> ,,... ]]
            } else if (dest instanceof CharArray) {
                PackedBitArrays.unpackBits((char[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (char) filler0, (char) filler1);
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            } else if (dest instanceof ByteArray) {
                PackedBitArrays.unpackBits((byte[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (byte) filler0, (byte) filler1);
            } else if (dest instanceof ShortArray) {
                PackedBitArrays.unpackBits((short[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (short) filler0, (short) filler1);
            } else if (dest instanceof IntArray) {
                PackedBitArrays.unpackBits((int[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (int) filler0, (int) filler1);
            } else if (dest instanceof LongArray) {
                PackedBitArrays.unpackBits((long[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (long) filler0, (long) filler1);
            } else if (dest instanceof FloatArray) {
                PackedBitArrays.unpackBits((float[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(), (float) filler0, (float) filler1);
            } else if (dest instanceof DoubleArray) {
                PackedBitArrays.unpackBits((double[])destBuf.data(), destBuf.from(),
                        srcBuf.data(), srcBuf.fromIndex(), destBuf.cnt(),  filler0,  filler1);
                //[[Repeat.AutoGeneratedEnd]]
            } else {
                throw new AssertionError("Unallowed type of passed array: " + src.getClass());
            }
            destBuf.force();
        }
    }

    static ArrayComparator defaultComparator(final UpdatableArray array, boolean reverse) {
        Objects.requireNonNull(array, "Null array argument");
        if (array instanceof UpdatableBitArray a) {
            if (!reverse) {
                return (first, second) -> !a.getBit(first) && a.getBit(second);
            } else {
                return (first, second) -> !a.getBit(second) && a.getBit(first);
            }
        }
        //[[Repeat() char ==> byte,,short,,int,,long;;
        //           Char ==> Byte,,Short,,Int,,Long]]
        if (array instanceof UpdatableCharArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final char[] ja = (char[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> ja[offset + (int) first] < ja[offset + (int) second];
                } else {
                    return (first, second) -> ja[offset + (int) second] < ja[offset + (int) first];
                }
            } else {
                if (!reverse) {
                    return (first, second) -> a.getChar(first) < a.getChar(second);
                } else {
                    return (first, second) -> a.getChar(second) < a.getChar(first);
                }
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (array instanceof UpdatableByteArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final byte[] ja = (byte[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> ja[offset + (int) first] < ja[offset + (int) second];
                } else {
                    return (first, second) -> ja[offset + (int) second] < ja[offset + (int) first];
                }
            } else {
                if (!reverse) {
                    return (first, second) -> a.getByte(first) < a.getByte(second);
                } else {
                    return (first, second) -> a.getByte(second) < a.getByte(first);
                }
            }
        }
        if (array instanceof UpdatableShortArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final short[] ja = (short[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> ja[offset + (int) first] < ja[offset + (int) second];
                } else {
                    return (first, second) -> ja[offset + (int) second] < ja[offset + (int) first];
                }
            } else {
                if (!reverse) {
                    return (first, second) -> a.getShort(first) < a.getShort(second);
                } else {
                    return (first, second) -> a.getShort(second) < a.getShort(first);
                }
            }
        }
        if (array instanceof UpdatableIntArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final int[] ja = (int[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> ja[offset + (int) first] < ja[offset + (int) second];
                } else {
                    return (first, second) -> ja[offset + (int) second] < ja[offset + (int) first];
                }
            } else {
                if (!reverse) {
                    return (first, second) -> a.getInt(first) < a.getInt(second);
                } else {
                    return (first, second) -> a.getInt(second) < a.getInt(first);
                }
            }
        }
        if (array instanceof UpdatableLongArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final long[] ja = (long[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> ja[offset + (int) first] < ja[offset + (int) second];
                } else {
                    return (first, second) -> ja[offset + (int) second] < ja[offset + (int) first];
                }
            } else {
                if (!reverse) {
                    return (first, second) -> a.getLong(first) < a.getLong(second);
                } else {
                    return (first, second) -> a.getLong(second) < a.getLong(first);
                }
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        if (array instanceof UpdatableFloatArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final float[] ja = (float[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> Float.compare(ja[offset + (int) first], ja[offset + (int) second]) < 0;
                } else {
                    return (first, second) -> Float.compare(ja[offset + (int) second], ja[offset + (int) first]) < 0;
                }
            } else {
                if (!reverse) {
                    return (first, second) -> Float.compare(a.getFloat(first), a.getFloat(second)) < 0;
                } else {
                    return (first, second) -> Float.compare(a.getFloat(second), a.getFloat(first)) < 0;
                }
            }
        }
        if (array instanceof UpdatableDoubleArray a) {
            if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
                final double[] ja = (double[])((DirectAccessible)a).javaArray();
                final int offset = ((DirectAccessible)a).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> Double.compare(ja[offset + (int) first], ja[offset + (int) second]) < 0;
                } else {
                    return (first, second) -> Double.compare(ja[offset + (int) second], ja[offset + (int) first]) < 0;
                }
            } else {
                if (!reverse) {
                    return (first, second) -> Double.compare(a.getDouble(first), a.getDouble(second)) < 0;
                } else {
                    return (first, second) -> Double.compare(a.getDouble(second), a.getDouble(first)) < 0;
                }
            }
        }
        if (array instanceof UpdatableObjectInPlaceArray<?> && !(array instanceof DirectAccessible)) {
            // Below is anti-optimization: in-place technique does not improve performance usually.
            // However, it can be necessary in real-time implementation, where heap access should be reduced
            final UpdatableObjectInPlaceArray<? extends Comparable<Object>> a = InternalUtils.cast(array);
            final Object work1 = a.allocateElement();
            final Object work2 = a.allocateElement();
            if (!reverse) {
                return (first, second) -> a.getInPlace(first, work1).compareTo(a.getInPlace(second, work2)) < 0;
            } else {
                return (first, second) -> a.getInPlace(second, work1).compareTo(a.getInPlace(first, work2)) < 0;
            }
        }
        if (array instanceof UpdatableObjectArray<?>) {
            if (array instanceof DirectAccessible && ((DirectAccessible)array).hasJavaArray()) {
                final Object[] ja = (Object[])((DirectAccessible)array).javaArray();
                final int offset = ((DirectAccessible)array).javaArrayOffset();
                if (!reverse) {
                    return (first, second) -> InternalUtils.<Comparable<Object>> cast(ja[offset + (int) first])
                        .compareTo(ja[offset + (int) second]) < 0;
                } else {
                    return (first, second) -> InternalUtils.<Comparable<Object>> cast(ja[offset + (int) second])
                        .compareTo(ja[offset + (int) first]) < 0;
                }
            } else {
                final UpdatableObjectArray<? extends Comparable<Object>> a = InternalUtils.cast(array);
                if (!reverse) {
                    return (first, second) -> a.get(first).compareTo(a.get(second)) < 0;
                } else {
                    return (first, second) -> a.get(second).compareTo(a.get(first)) < 0;
                }
            }
        }
        throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
    }

/*  // OBSOLETE ONE-THREAD ALGORITHM
    static Range rangeOf(PArray array, Arrays.MinMaxInfo info) {
        Objects.requireNonNull(array, "Null array argument");
        if (info == null)
            info = new Arrays.MinMaxInfo();
        if (array.length() == 0) {
            info.setAll(-1, -1, 0.0, 0.0);
            return Range.of(0.0, 0.0);
        }

        long indexOfMin = -1;
        long indexOfMax = -1;
        long index = 0;
        if (array instanceof BitArray) {
            boolean v0 = ((BitArray)array).getBit(0);
            if (v0)
                indexOfMax = 0;
            else
                indexOfMin = 0;
            DataBitBuffer buf = (DataBitBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ,
                AbstractArray.largeBufferCapacity(array), true);
            try {
                Arrays.enableCaching(buf);
                Loop:
                    for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    long[] ja = buf.data();
                    for (long kMin = buf.fromIndex(), kMax = buf.toIndex(), k = kMin; k < kMax; k++) {
                        boolean v = PackedBitArrays.getBit(ja, k);
                        if (v != v0) {
                            if (v)
                                indexOfMax = buf.position() + (k - kMin);
                            else
                                indexOfMin = buf.position() + (k - kMin);
                            break Loop;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            assert indexOfMin != -1 || indexOfMax != -1;
            if (indexOfMin == -1) // no zero bits: so, the first 1 is both minimum and maximum
                indexOfMin = 0;
            if (indexOfMax == -1) // no unit bits: so, the first 0 is both minimum and maximum
                indexOfMax = 0;
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();

            //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
            //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
            //          (\s+&\s+0xFF) ==> ,,$1FF,, ,, ,, ,, ]]
        } else if (array instanceof ByteArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataByteBuffer buf = (DataByteBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    byte[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k] & 0xFF;
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        } else if (array instanceof CharArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataCharBuffer buf = (DataCharBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    char[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k];
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
        } else if (array instanceof ShortArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataShortBuffer buf = (DataShortBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    short[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k] & 0xFFFF;
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
        } else if (array instanceof IntArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataIntBuffer buf = (DataIntBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    int[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k];
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
        } else if (array instanceof LongArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataLongBuffer buf = (DataLongBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    long[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k];
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
        } else if (array instanceof FloatArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataFloatBuffer buf = (DataFloatBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    float[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k];
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
        } else if (array instanceof DoubleArray) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            DataDoubleBuffer buf = (DataDoubleBuffer)Arrays.bufferInternal(array, DataBuffer.AccessMode.READ);
            try {
                Arrays.enableCaching(buf);
                for (buf.map(0); buf.hasData(); buf.mapNext()) {
                    double[] ja = buf.data();
                    for (int k = buf.from(), kMax = buf.to(); k < kMax; k++, index++) {
                        double v = ja[k];
                        if (v > max) {
                            max = v; indexOfMax = index;
                        }
                        if (v < min) {
                            min = v; indexOfMin = index;
                        }
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
            info.setAll(indexOfMin, indexOfMax, array.getDouble(indexOfMin), array.getDouble(indexOfMax));
            return info.range();
            //[[Repeat.AutoGeneratedEnd]]
        } else {
            throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
        }
    }
*/

    static Arrays.CopyStatus copy(ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks,
        boolean strictMode, boolean compare)
    {
        long t1 = Arrays.CONFIG_LOGGABLE ? System.nanoTime() : 0;
        Array srcCopy = !Arrays.DEBUG_MODE ? null : SimpleMemoryModel.getInstance().newUnresizableArray(
            src.elementType(), Math.min(src.length(), dest.length())).copy(src);
        ArraysBufferedCopier copier = ArraysBufferedCopier.getInstance(context, dest, src, numberOfTasks,
            strictMode, compare);
        boolean changed = copier.process();
        Arrays.CopyStatus result = compare ?
            new Arrays.ComparingCopyStatus(copier.usedAlgorithm, strictMode, changed) :
            new Arrays.CopyStatus(copier.usedAlgorithm, strictMode);
        if (srcCopy != null && strictMode) {
            if (!srcCopy.equals(dest.subArr(0, srcCopy.length()))) {
                throw new AssertionError("Error while copying " + src + " to " + dest);
            }
        }
        if (Arrays.CONFIG_LOGGABLE && Arrays.SystemSettings.profilingMode()) {
            long t2 = System.nanoTime();
            if (t2 - t1 > (long)Arrays.SystemSettings.RECOMMENDED_ELAPSED_TIME_FOR_ADDITIONAL_LOGGING * 1000000L) {
                // there is no sense to spend time for logging very quick algorithms
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                StringBuilder stackInfo = new StringBuilder();
                for (int k = 1; k < stack.length; k++) {
                    String methodName = stack[k].getMethodName();
                    String className = stack[k].getClassName();
                    if (stackInfo.isEmpty()) {
                        if ("copy".equals(methodName) &&
                            (Arrays.class.getName().equals(className) || Matrices.class.getName().equals(className))) {
                            continue;
                        }
                    } else {
                        stackInfo.append("/");
                    }
                    stackInfo.append(stack[k].getMethodName());
                }
                Arrays.LOGGER.config(String.format(Locale.US,
                    "Array is copied in %.3f ms (%.2f ns/element, %d underlying) "
                    + "[%s -> %s] in %s",
                    1e-6 * (t2 - t1), (t2 - t1) / Math.max(1.0, (double) src.length()),
                    Arrays.getUnderlyingArraysCount(src),
                    src, dest, stackInfo));
            }
        }
        return result;
    }

    static Array asConcatenation(Array... arrays) {
        Objects.requireNonNull(arrays, "Null arrays argument");
        if (arrays.length == 0) {
            throw new IllegalArgumentException("Empty arrays[] (array of AlgART arrays)");
        }
        Class<?> elementType = arrays[0].elementType();
        for (int k = 0; k < arrays.length; k++) {
            if (!arrays[k].isUnresizable()) {
                throw new IllegalArgumentException("asConcatenation method cannot be applied to resizable arrays: "
                    + "please use UpdatableArray.asUnresizable() method before constructing a concatenation");
            }
            if (k > 0 && arrays[k].elementType() != elementType) {
                throw new IllegalArgumentException("asConcatenation method cannot be applied to arrays "
                    + "with different element type: arrays[" + k + "] is " + arrays[k]
                    + ", but arrays[0] is " + arrays[0]);
            }
        }
        //[[Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (elementType == boolean.class) {
            BitArray[] a = new BitArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (BitArray) arrays[k];
            }
            return new ConcatenatedBitArray(a);
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == char.class) {
            CharArray[] a = new CharArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (CharArray) arrays[k];
            }
            return new ConcatenatedCharArray(a);
        } else
        if (elementType == byte.class) {
            ByteArray[] a = new ByteArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (ByteArray) arrays[k];
            }
            return new ConcatenatedByteArray(a);
        } else
        if (elementType == short.class) {
            ShortArray[] a = new ShortArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (ShortArray) arrays[k];
            }
            return new ConcatenatedShortArray(a);
        } else
        if (elementType == int.class) {
            IntArray[] a = new IntArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (IntArray) arrays[k];
            }
            return new ConcatenatedIntArray(a);
        } else
        if (elementType == long.class) {
            LongArray[] a = new LongArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (LongArray) arrays[k];
            }
            return new ConcatenatedLongArray(a);
        } else
        if (elementType == float.class) {
            FloatArray[] a = new FloatArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (FloatArray) arrays[k];
            }
            return new ConcatenatedFloatArray(a);
        } else
        if (elementType == double.class) {
            DoubleArray[] a = new DoubleArray[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                a[k] = (DoubleArray) arrays[k];
            }
            return new ConcatenatedDoubleArray(a);
        } else //[[Repeat.AutoGeneratedEnd]]
        {
            ObjectArray<Object>[] a = InternalUtils.cast(new ObjectArray<?>[arrays.length]);
            for (int k = 0; k < arrays.length; k++) {
                a[k] = ((ObjectArray<?>) arrays[k]).cast(Object.class);
            }
            return new ConcatenatedObjectArray<>(a);
        }
    }

    static Array asShifted(Array array, long shift) {
        Objects.requireNonNull(array, "Null array argument");
        if (!array.isUnresizable()) {
            throw new IllegalArgumentException("asShifted method cannot be applied to resizable array: "
                + "please use UpdatableArray.asUnresizable() method before constructing a shifted array");
        }
        // if shift == 0 or Arrays.isNCopies(array), we still process this case
        // (to be on the safe side; in old versions, it was necessary to provide correct
        // memory model for newCompatibleXxxArray methods)
        final long len = array.length();
        if (len > 0) {
            shift %= len;
            if (shift < 0) {
                shift += len;
            }
        }
        if (len > 0) {
            assert 0 <= shift && shift < len;
        }
        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (array instanceof BitArray) {
            return new ShiftedBitArray((BitArray) array, shift);
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (array instanceof CharArray) {
            return new ShiftedCharArray((CharArray) array, shift);
        } else
        if (array instanceof ByteArray) {
            return new ShiftedByteArray((ByteArray) array, shift);
        } else
        if (array instanceof ShortArray) {
            return new ShiftedShortArray((ShortArray) array, shift);
        } else
        if (array instanceof IntArray) {
            return new ShiftedIntArray((IntArray) array, shift);
        } else
        if (array instanceof LongArray) {
            return new ShiftedLongArray((LongArray) array, shift);
        } else
        if (array instanceof FloatArray) {
            return new ShiftedFloatArray((FloatArray) array, shift);
        } else
        if (array instanceof DoubleArray) {
            return new ShiftedDoubleArray((DoubleArray) array, shift);
        } else //[[Repeat.AutoGeneratedEnd]]
        if (array instanceof ObjectArray<?> a) {
            return new ShiftedObjectArray<>(a.cast(Object.class), shift);
        } else {
            throw new AssertionError("The array does not implement necessary interfaces: " + array.getClass());
        }
    }

    interface ConcatenatedArray {
        long[] startPositions();
    }

    private static long sumOfLengths(Array[] arrays) throws TooLargeArrayException {
        long sum = 0;
        for (Array array : arrays) {
            long len = array.length();
            assert len >= 0 : "illegal length() implementation in " + array;
            sum += len;
            if (sum < 0) {
                throw new TooLargeArrayException("The length of concatenation of arrays is greater than 2^63-1");
            }
        }
        return sum;
    }

    private static long[] getStartPositions(Array[] arrays) {
        long[] result = new long[arrays.length];
        for (int k = 1; k < arrays.length; k++) {
            result[k] = result[k - 1] + arrays[k - 1].length();
            assert result[k] >= result[k - 1];
        }
        return result;
    }

    private static int searchInConcatenatedArray(long[] startPositions, long index) {
        int result = java.util.Arrays.binarySearch(startPositions, index);
        if (result >= 0) {
            while (result < startPositions.length - 1 && startPositions[result + 1] == index) {
                result++; // we must skip possible empty arrays
            }
        } else {
            // -result-1 is the first index in startPositions greater than the index
            result = -result - 2; // so, the required array is before it
        }
        return result;
    }

    //[[Repeat()
    //  \@Override\s+public\s+(void\s+getBits|long\s+nextQuickPosition).*?}//EndOfMethod.*?(?:\r(?!\n)|\n|\r\n) ==>
    //                  ,, ,, ,, ,, ,, ,, ,,;;
    //  boolean|bit ==> char,,byte,,short,,int,,long,,float,,double,,E;;
    //  Bit         ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //  (byte|short)\s+get(Byte|Short) ==> ,,int get$2,,int get$2,, ,, ...;;
    //  (super\(sumOfLengths) ==> $1,,$1,,$1,,$1,,$1,,$1,,$1,,super(arrays[0].elementType(), sumOfLengths;;
    //  (new\s+ObjectArray\[.*?\]) ==> InternalUtils.cast($1),, ...;;
    //  (ObjectArray)(?!\(|\)|\[\w) ==> $1<E>,, ...;;
    //  (ObjectArray)(?=\[\w) ==> $1<?>,, ...;;
    //  (new\s+ConcatenatedObjectArray) ==> $1<E>,, ...;;
    //  getObject ==> get,, ...;;
    //  E\[\" ==> \" + elementType().getName() + \"[\",, ...]]
    private static class ConcatenatedBitArray extends AbstractBitArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final BitArray[] arrays;

        ConcatenatedBitArray(BitArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public boolean getBit(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getBit(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }

        @Override
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                long len = Math.min(count, nextStartPos - startPos - p);
                arrays[k].getBits(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }//EndOfMethod !! this comment is necessary for preprocessing by Repeater !!

        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                BitArray[] a = new BitArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedBitArray(a);
            }
        }

        @Override
        public long nextQuickPosition(long position) {
            if (position >= length) {
                return -1;
            }
            if (position < 0) {
                position = 0;
            }
            int k = searchInConcatenatedArray(startPositions, position);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + position + " (" + this + ")";
            long p = position - startPositions[k];
            assert p >= 0;
            long qp = arrays[k].nextQuickPosition(p);
            while (qp == -1) { // try the next arrays
                if (k == startPositions.length - 1) {
                    return -1;
                }
                k++;
                p = 0;
                position = startPositions[k];
                qp = arrays[k].nextQuickPosition(0);
            }
            assert qp >= p : "illegal nextQuickPosition implementation in " + arrays[k];
            long result = position + (qp - p);
            return result >= length ? -1 : result;
        }//EndOfMethod !! this comment is necessary for preprocessing by Repeater !!

        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array bit[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " bits)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static class ConcatenatedCharArray extends AbstractCharArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final CharArray[] arrays;

        ConcatenatedCharArray(CharArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public char getChar(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getChar(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                CharArray[] a = new CharArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedCharArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array char[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " chars)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedByteArray extends AbstractByteArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final ByteArray[] arrays;

        ConcatenatedByteArray(ByteArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public int getByte(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getByte(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                ByteArray[] a = new ByteArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedByteArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array byte[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " bytes)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedShortArray extends AbstractShortArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final ShortArray[] arrays;

        ConcatenatedShortArray(ShortArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public int getShort(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getShort(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                ShortArray[] a = new ShortArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedShortArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array short[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " shorts)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedIntArray extends AbstractIntArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final IntArray[] arrays;

        ConcatenatedIntArray(IntArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public int getInt(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getInt(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                IntArray[] a = new IntArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedIntArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array int[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " ints)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedLongArray extends AbstractLongArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final LongArray[] arrays;

        ConcatenatedLongArray(LongArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public long getLong(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getLong(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                LongArray[] a = new LongArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedLongArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array long[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " longs)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedFloatArray extends AbstractFloatArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final FloatArray[] arrays;

        ConcatenatedFloatArray(FloatArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public float getFloat(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getFloat(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                FloatArray[] a = new FloatArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedFloatArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array float[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " floats)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedDoubleArray extends AbstractDoubleArray implements ConcatenatedArray {
        private final long[] startPositions;
        private final DoubleArray[] arrays;

        ConcatenatedDoubleArray(DoubleArray[] arrays) {
            super(sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public double getDouble(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].getDouble(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                DoubleArray[] a = new DoubleArray[k2 - k1 + 1];
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedDoubleArray(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array double[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " doubles)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ConcatenatedObjectArray<E> extends AbstractObjectArray<E> implements ConcatenatedArray {
        private final long[] startPositions;
        private final ObjectArray<E>[] arrays;

        ConcatenatedObjectArray(ObjectArray<E>[] arrays) {
            super(arrays[0].elementType(), sumOfLengths(arrays), false, arrays);
            this.startPositions = getStartPositions(arrays);
            this.arrays = arrays;
        }

        @Override
        public E get(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            int k = searchInConcatenatedArray(startPositions, index);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + index + " (" + this + ")";
            return arrays[k].get(index - startPositions[k]);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            int k = searchInConcatenatedArray(startPositions, arrayPos);
            assert k >= 0 && k < startPositions.length :
                "illegal underlying array index " + k + " for position " + arrayPos + " (" + this + ")";
            long startPos = startPositions[k];
            long p = arrayPos - startPos;
            assert p >= 0;
            do {
                assert k < startPositions.length : "startPositions array exhausted";
                long nextStartPos = k == startPositions.length - 1 ? this.length : startPositions[k + 1];
                int len = (int)Math.min(count, nextStartPos - startPos - p);
                arrays[k].getData(p, destArray, destArrayOffset, len);
                destArrayOffset += len;
                count -= len;
                k++;
                startPos = nextStartPos;
                p = 0;
            } while (count > 0);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            int k1 = searchInConcatenatedArray(startPositions, fromIndex);
            assert k1 >= 0 && k1 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            int k2 = searchInConcatenatedArray(startPositions, toIndex - 1);
            assert k2 >= 0 && k2 < startPositions.length :
                "illegal underlying array index " + k1 + " for position " + fromIndex + " (" + this + ")";
            assert k2 >= k1;
            long p1 = fromIndex - startPositions[k1];
            long p2 = toIndex - 1 - startPositions[k2];
            if (k2 == k1) {
                return arrays[k1].subArray(p1, p2 + 1).asImmutable();
            } else {
                long len1 = arrays[k1].length();
                long len2 = arrays[k2].length();
                ObjectArray<E>[] a = InternalUtils.cast(new ObjectArray<?>[k2 - k1 + 1]);
                a[0] = p1 == 0 ? arrays[k1] :
                    InternalUtils.cast(arrays[k1].subArray(p1, len1));
                // InternalUtils.cast is necessary in auto-generated code of ConcatenatedObjectArray<E>
                for (int k = k1 + 1; k < k2; k++) {
                    a[k - k1] = arrays[k];
                }
                a[a.length - 1] = p2 == len2 - 1 ? arrays[k2] :
                    InternalUtils.cast(arrays[k2].subArray(0, p2 + 1));
                return new ConcatenatedObjectArray<E>(a);
            }
        }


        public long[] startPositions() {
            return this.startPositions.clone();
        }

        public String toString() {
            long[] lengths = new long[arrays.length];
            for (int k = 0; k < arrays.length; k++) {
                lengths[k] = arrays[k].length();
            }
            return "immutable AlgART array " + elementType().getName() + "[" + length + "]"
                + " built by concatenation of " + arrays.length + " arrays ("
                + JArrays.toString(lengths, ", ", 200) + " Es)";
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just arrays[k1].subArray(...).asImmutable(), which have correct loadResources
    }

    //[[Repeat.AutoGeneratedEnd]]

    interface ShiftedArray {
        long shift();
    }

    //[[Repeat()
    //  \@Override\s+public\s+(void\s+getBits|long\s+nextQuickPosition).*?}//EndOfMethod.*?(?:\r(?!\n)|\n|\r\n) ==>
    //              ,, ,, ,, ,, ,, ,, ,,;;
    //  boolean ==> char,,byte,,short,,int,,long,,float,,double,,E;;
    //  Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //  (byte|short)\s+get(Byte|Short) ==> ,,int get$2,,int get$2,, ,, ...;;
    //  (super\(a\.length) ==> $1,,$1,,$1,,$1,,$1,,$1,,$1,,super(a.elementType(), a.length;;
    //  (ObjectArray)(?!\() ==> $1<E>,, ...;;
    //  getObject ==> get,, ...;;
    //  E\[\" ==> \" + elementType().getName() + \"[\",, ...]]
    private static class ShiftedBitArray extends AbstractBitArray implements ShiftedArray {
        private final BitArray a;
        private final long shift;
        ShiftedBitArray(BitArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public boolean getBit(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getBit will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getBit(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getBits will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        long rear = length - arrayPos;
                        a.getBits(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getBits(arrayPos, destArray, destArrayOffset, count);
        }//EndOfMethod !! this comment is necessary for preprocessing by Repeater !!

        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }

        @Override
        public long nextQuickPosition(long position) {
            if (position >= length) {
                return -1;
            }
            if (position < 0) {
                position = 0;
            }
            long p = position - shift;
            if (p < 0) {
                if (shift == 0 || shift > length / 2) // shift to the left
                {
                    p += length;
                } else // shift to the right
                {
                    p = (p & 255) % length; // better behavior for a case from=0
                }
            }
            long qp = a.nextQuickPosition(p);
            if (qp == -1) {
                return -1;
            }
            assert qp >= p : "illegal nextQuickPosition implementation in " + a;
            long result = position + (qp - p);
            return result >= length ? -1 : result;
        }//EndOfMethod !! this comment is necessary for preprocessing by Repeater !!

        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array boolean[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static class ShiftedCharArray extends AbstractCharArray implements ShiftedArray {
        private final CharArray a;
        private final long shift;
        ShiftedCharArray(CharArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public char getChar(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getChar will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getChar(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array char[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedByteArray extends AbstractByteArray implements ShiftedArray {
        private final ByteArray a;
        private final long shift;
        ShiftedByteArray(ByteArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public int getByte(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getByte will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getByte(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array byte[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedShortArray extends AbstractShortArray implements ShiftedArray {
        private final ShortArray a;
        private final long shift;
        ShiftedShortArray(ShortArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public int getShort(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getShort will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getShort(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array short[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedIntArray extends AbstractIntArray implements ShiftedArray {
        private final IntArray a;
        private final long shift;
        ShiftedIntArray(IntArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public int getInt(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getInt will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getInt(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array int[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedLongArray extends AbstractLongArray implements ShiftedArray {
        private final LongArray a;
        private final long shift;
        ShiftedLongArray(LongArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public long getLong(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getLong will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getLong(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array long[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedFloatArray extends AbstractFloatArray implements ShiftedArray {
        private final FloatArray a;
        private final long shift;
        ShiftedFloatArray(FloatArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public float getFloat(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getFloat will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getFloat(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array float[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedDoubleArray extends AbstractDoubleArray implements ShiftedArray {
        private final DoubleArray a;
        private final long shift;
        ShiftedDoubleArray(DoubleArray a, long shift) {
            super(a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public double getDouble(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.getDouble will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.getDouble(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array double[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    private static class ShiftedObjectArray<E> extends AbstractObjectArray<E> implements ShiftedArray {
        private final ObjectArray<E> a;
        private final long shift;
        ShiftedObjectArray(ObjectArray<E> a, long shift) {
            super(a.elementType(), a.length(), false, a);
            if (length > 0) {
                shift %= length;
                if (shift < 0) {
                    shift += length;
                }
                assert 0 <= shift && shift < length;
            }
            this.a = a;
            this.shift = shift;
        }

        @Override
        public E get(long index) {
            if (index >= 0 && index < length) { // else do nothing: a.get will throw exception
                index -= shift;
                if (index < 0) {
                    index += length;
                }
            }
            return a.get(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (arrayPos >= 0 && count > 0 && arrayPos + count <= length) {
                // else do nothing: a.getData will throw exception
                // if count == 0, we also must not do execute the following code:
                // shift may be invalid when length == 0
                long arrayPosSave = arrayPos;
                arrayPos -= shift;
                if (arrayPos < 0) { // "< 0" means overflow
                    arrayPos += length;
                    assert arrayPos >= arrayPosSave && arrayPos < length:
                        "arrayPos=" + arrayPos + ", arrayPosSave=" + arrayPosSave + ", length=" + length;
                    if (arrayPos >= length - count) { // copied block is divided
                        int rear = (int)(length - arrayPos);
                        a.getData(arrayPos, destArray, destArrayOffset, rear);
                        count -= rear;
                        destArrayOffset += rear;
                        arrayPos = 0;
                    }
                }
            }
            a.getData(arrayPos, destArray, destArrayOffset, count);
        }


        @Override
        public Array subArray(final long fromIndex, final long toIndex) {
            if (fromIndex == toIndex) {
                return super.subArray(fromIndex, toIndex);
            }
            checkSubArrayArguments(fromIndex, toIndex);
            // now we are sure that fromIndex < toIndex
            long from = fromIndex - shift;
            if (from < 0) {
                from += length;
            }
            long toMinus1 = toIndex - 1 - shift;
            if (toMinus1 < 0) {
                toMinus1 += length;
            }
            assert from < length;
            assert toMinus1 < length;
            if (from <= toMinus1) {
                return a.subArray(from, toMinus1 + 1).asImmutable();
            } else {
                return super.subArray(fromIndex, toIndex);
            }
        }


        public long shift() {
            return this.shift;
        }

        public String toString() {
            return "immutable AlgART array " + elementType().getName() + "[" + length
                + "]" + " built by shifting by " + shift + " of " + a;
        }

        // No needs to override here loadResources(long, long):
        // usually we work with subarrays of this array, and they are
        // usually just a.subArray(...).asImmutable(), which have correct loadResources
    }

    //[[Repeat.AutoGeneratedEnd]]
}
