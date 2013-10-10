package net.algart.matrices.spectra;

import net.algart.arrays.ArrayContext;
import net.algart.matrices.spectra.ComplexScalarSampleArray.*;

class ReverseBits {
    static final int[] REVERSE_16 = new int[65536];
    static {
        REVERSE_16[0] = 0;
        for (int k = 0; k < 65536; k++) {
            REVERSE_16[k] = Integer.reverse(k) >>> 16;
        }
    }

    public static long reverseBits64(long v) {
        return (long)REVERSE_16[(int)(v >>> 48)]
            | (long)REVERSE_16[(int)(v >>> 32) & 65535] << 16
            | (long)REVERSE_16[(int)(v >>> 16) & 65535] << 32
            | (long)REVERSE_16[(int)v & 65535] << 48;
    }

    public static int reverseBits32(int v) {
        return REVERSE_16[v >>> 16] | REVERSE_16[v & 65535] << 16;
    }

    public static void reorderForButterfly(ArrayContext context, SampleArray samples) {
        final long n = samples.length();
        if ((n & (n - 1)) != 0)
            throw new IllegalArgumentException("The length of sample " + n + " is not 2^k");
        // Special branches for DirectComplexFloatSampleArray and DirectComplexDoubleSampleArray
        // do not optimize the loop below on good JVMs
        final int numberOfLeadingZeros = Long.numberOfLeadingZeros(n);
        if (n <= Integer.MAX_VALUE) { // - litte optimization (~5%)
            final int shift = numberOfLeadingZeros - 31;
            for (int i = 1, m = (int)n - 1; i < m; ) {
                final int iMax = i > m - 1024 ? m : i + 1024; // "m - 1024" allows avoiding overflow
                for (; i < iMax; i++) {
                    int j = (REVERSE_16[i >>> 16] | REVERSE_16[i & 65535] << 16) >>> shift;
                    if (i < j) {
                        samples.swap(i, j);
                    }
                }
                if (context != null) {
                    context.checkInterruptionAndUpdateProgress(null, iMax, m);
                }
            }
        } else {
            final int shift = numberOfLeadingZeros + 1;
            for (long i = 1, m = n - 1; i < m; ) {
                final long iMax = i > m - 65536 ? m : i + 65536; // "m - 65536" allows avoiding overflow
                for (; i < iMax; i++) {
                    long j = Long.reverse(i) >>> shift;
                    if (i < j) {
                        samples.swap(i, j);
                    }
                }
                if (context != null) {
                    context.checkInterruptionAndUpdateProgress(null, iMax, m);
                }
            }
        }
    }
}
