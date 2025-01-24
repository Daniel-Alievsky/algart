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

package net.algart.arrays;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Implementation of {@link BitArray#getBits(long, long[], long, long)} methods
 * in the custom implementations of functional arrays for rectangular binary functions.
 * Can be used by {@link ArraysFuncImpl} class, but not used now.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysComparisonWithThresholdGetBitsOp {
    private final ReentrantLock lock = new ReentrantLock();

    private final PArray x0;
    private final boolean isBit;
    private final DataBuffer dbuf;

    private final double threshold;
    private final boolean greater;
    private final boolean inclusive;
    private final JArrayPool bufferPool;

    ArraysComparisonWithThresholdGetBitsOp(PArray x0, double threshold, boolean greater, boolean inclusive) {
        this.x0 = x0;
        this.isBit = x0 instanceof BitArray;
        this.dbuf = Arrays.bufferInternal(x0, DataBuffer.AccessMode.READ);
        // - necessary not only for performance, but also for the guarantee
        // that direct buffer really returns the references to the internal Java array
        this.threshold = threshold;
        this.greater = greater;
        this.inclusive = inclusive;
        this.bufferPool =
                x0 instanceof CharArray ? ArraysFuncImpl.CHAR_BUFFERS
                        : x0 instanceof ByteArray ? ArraysFuncImpl.BYTE_BUFFERS
                        : x0 instanceof ShortArray ? ArraysFuncImpl.SHORT_BUFFERS
                        : x0 instanceof IntArray ? ArraysFuncImpl.INT_BUFFERS
                        : x0 instanceof LongArray ? ArraysFuncImpl.LONG_BUFFERS
                        : x0 instanceof FloatArray ? ArraysFuncImpl.FLOAT_BUFFERS
                        : x0 instanceof DoubleArray ? ArraysFuncImpl.DOUBLE_BUFFERS
                        : null;
    }

    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw AbstractArray.rangeException(arrayPos, x0.length(), getClass());
        }
        if (arrayPos > x0.length() - count) {
            throw AbstractArray.rangeException(arrayPos + count - 1, x0.length(), getClass());
        }
        while (count > 0) {
            int len;
            boolean usePool = false;
            Object data = null;
            try {
                final int from, to;
                usePool = !dbuf.isDirect();
                if (usePool) {
                    if (isBit) { // a loop is not necessary here
                        ((BitArray) x0).getBits(arrayPos, destArray, destArrayOffset, count);
                        break;
                    } else {
                        data = bufferPool.requestArray();
                        len = (int) Math.min(count, bufferPool.arrayLength());
                        x0.getData(arrayPos, data, 0, len);
                        from = 0;
                        to = len;
                    }
                } else {
                    // Synchronization is necessary to provide thread-safety.
                    // In this case, the cost of synchronization is low:
                    // buffer mapping work very quickly for direct buffers.
                    lock.lock();
                    try {
                        dbuf.map(arrayPos, count);
                        len = dbuf.cnt();
                        assert len == dbuf.count() : "too large buffer";
                        data = dbuf.data();
                        from = dbuf.from();
                        to = dbuf.to();
                    } finally {
                        // We may unlock here: data array is never modified for direct buffers.
                        lock.unlock();
                    }
                }
            } finally {
                bufferPool.releaseArray(data);
            }
            destArrayOffset += len;
            arrayPos += len;
            count -= len;
        }
    }

}
