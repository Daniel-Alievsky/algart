package net.algart.arrays;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Implementation of {@link BitArray#getBits(long, long[], long, long)} methods
 * in the custom implementations of functional arrays for rectangular binary functions.
 * Used by {@link ArraysFuncImpl} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class ArraysComparisonWithThresholdGetBitsOp {
    private final ReentrantLock lock = new ReentrantLock();

    private final PArray x0;
    private final boolean isBit;
    private final DataBuffer dbuf;

    private final double threshold;
    private boolean greater;
    private boolean inclusive;
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
            x0 instanceof CharArray ? ArraysFuncImpl.CHAR_BUFFERS :
            x0 instanceof ByteArray ? ArraysFuncImpl.BYTE_BUFFERS :
            x0 instanceof ShortArray ? ArraysFuncImpl.SHORT_BUFFERS :
            x0 instanceof IntArray ? ArraysFuncImpl.INT_BUFFERS :
            x0 instanceof LongArray ? ArraysFuncImpl.LONG_BUFFERS :
            x0 instanceof FloatArray ? ArraysFuncImpl.FLOAT_BUFFERS :
            x0 instanceof DoubleArray ? ArraysFuncImpl.DOUBLE_BUFFERS :
            null;
        //TODO!! this.always1=..., this.always0=...
    }

    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        if (destArray == null)
            throw new NullPointerException("Null destArray argument");
        if (count < 0)
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        if (arrayPos < 0)
            throw AbstractArray.rangeException(arrayPos, x0.length(), getClass());
        if (arrayPos > x0.length() - count)
            throw AbstractArray.rangeException(arrayPos + count - 1, x0.length(), getClass());
        for (; count > 0; ) {
            int len;
            boolean usePool = false;
            Object data = null;
            try {
                final int from, to;
                usePool = !dbuf.isDirect();
                if (usePool) {
                    if (isBit) { // a loop is not necessary here
                        ((BitArray)x0).getBits(arrayPos, destArray, destArrayOffset, count);
                        break;
                    } else {
                        data = bufferPool.requestArray();
                        len = (int)Math.min(count, bufferPool.arrayLength());
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
                        assert len == dbuf.count():"too large buffer";
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
