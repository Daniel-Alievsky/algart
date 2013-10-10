package net.algart.matrices.skeletons.demo;

import net.algart.arrays.*;
import net.algart.matrices.skeletons.*;

import java.util.Locale;

public class CompareSkeletonStrength {
    static final int NUMBER_OF_BITS_A = AnalyseSkeletonConfigurations.NUMBER_OF_BITS_A;
    static final int NUMBER_OF_BITS_B = AnalyseSkeletonConfigurations.NUMBER_OF_BITS_B;

    private static final SimpleMemoryModel SMM = SimpleMemoryModel.getInstance();

    public static void main(String[] args) {
        final Matrix<UpdatableBitArray> matrix8x8 = SMM.newBitMatrix(8, 8);
        // - adding 1-pixel white space for correct skeletonization: 1 long value
        final Matrix<UpdatableBitArray> matrix8xN = SMM.newBitMatrix(8, 8 * (long)NUMBER_OF_BITS_B);
        final long[] work8xN_1 = new long[NUMBER_OF_BITS_B], work8xN_2 = new long[NUMBER_OF_BITS_B];

        final UpdatableBitArray array8x8 = matrix8x8.array();
        final UpdatableBitArray array8xN = matrix8xN.array();
        final ThinningSkeleton[] skeletons8xN = {
            WeakOctupleThinningSkeleton2D.getInstance(null, matrix8xN),
            OctupleThinningSkeleton2D.getInstance(null, matrix8xN),
            Quadruple3x5ThinningSkeleton2D.getInstance(null, matrix8xN),
            StrongQuadruple3x5ThinningSkeleton2D.getInstance(null, matrix8xN),
        };
        final String[] skeletonCodes = {
            "wo",
            "o",
            "q",
            "sq",
        };
        System.out.println(skeletons8xN.length + " skeletons tested:");
        for (int m = 0; m < skeletons8xN.length; m++) {
            System.out.printf("    %2s: %s%n", skeletonCodes[m], skeletons8xN[m]);
        }
        System.out.println("Strength codes:");
        System.out.println("    0: stable in some 5x5 configurations;");
        System.out.println("    1: unstable in all 5x5 configurations, but maybe stable in some 7x7;");
        System.out.println("    2: unstable always (central point removed in all 5x5 configurations).");
        System.out.println();
        final byte[][] status = new byte[skeletons8xN.length][NUMBER_OF_BITS_B];
        final byte[] maxStatus = new byte[skeletons8xN.length];
        final BitArray[][] thinnedCache8xN = new BitArray[skeletons8xN.length][8];
        MutableIntArray all3x3 = SMM.newEmptyIntArray();
        int count3x3 = 0;
        boolean problemsFound = false;
        long t1 = System.nanoTime();
        for (int bitsA = 0; bitsA < NUMBER_OF_BITS_A; bitsA++) {
            if (all3x3.indexOf(0, all3x3.length(), bitsA) != -1) {
                continue;
            }
            count3x3++;
            AnalyseSkeletonConfigurations.add3x3(all3x3, bitsA, true);
            array8x8.fill(false);
            AnalyseSkeletonConfigurations.setAllBitsAAndCenter1(bitsA, array8x8);
            if (AnalyseSkeletonConfigurations.getAllShiftedBitsA(0, 0, array8x8) != bitsA)
                throw new AssertionError("Error in setAllBitsAAndCenter1/getAllShiftedBitsA");
            array8x8.getBits(0, work8xN_1, 0, array8x8.length());
            long long3x3 = work8xN_1[0];
            assert AnalyseSkeletonConfigurations.SortedBitsB.SORTED_BITS_B.length == NUMBER_OF_BITS_B;
            for (int k = 0; k < NUMBER_OF_BITS_B; k++) {
                int bitsB = AnalyseSkeletonConfigurations.SortedBitsB.SORTED_BITS_B[k];
                work8xN_1[k] = AnalyseSkeletonConfigurations.setAllBitsB(bitsB, long3x3);
            }
            array8xN.setBits(0, work8xN_1, 0, 64 * (long) NUMBER_OF_BITS_B);
            System.out.printf("%-12s", "[3x3c=" + bitsA + "]:");
            for (int m = 0; m < skeletons8xN.length; m++) {
                AnalyseSkeletonConfigurations.checkStability(status[m], skeletons8xN[m],
                    AnalyseSkeletonConfigurations.MASK_1X1, AnalyseSkeletonConfigurations.MASK_1X1,
                    work8xN_1, work8xN_2, thinnedCache8xN[m]);
                maxStatus[m] = 0;
                for (int k = 0; k < NUMBER_OF_BITS_B; k++) {
//                    array8x8.copy(array8xN.subArr(64 * k, 64));
//                    System.out.println(k + ": " + status[m][k]);
//                    System.out.println(AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8, k + ": "));
                    maxStatus[m] = (byte)Math.max(maxStatus[m], status[m][k]);
                }
                System.out.printf("%s%d (%s)",
                    m == 0 ? "" : maxStatus[m - 1] > maxStatus[m] ? " < " :
                        maxStatus[m - 1] == maxStatus[m] ? " = " : " > ",
                    2 - maxStatus[m], skeletonCodes[m]);
            }
            for (int k = 0; k < NUMBER_OF_BITS_B; k++) {
                boolean strangeIncreases = true;
                for (int m = 1; m < skeletons8xN.length; m++) {
                    if (status[m][k] > status[m - 1][k]) {
                        strangeIncreases = false;
                        break;
                    }
                }
                if (!strangeIncreases) {
                    problemsFound = true;
                    array8x8.copy(array8xN.subArr(64 * k, 64));
                    StringBuilder sb = new StringBuilder((2 - status[0][k]) + "(" + skeletonCodes[0] + ")");
                    for (int m = 1; m < skeletons8xN.length; m++) {
                        sb.append(", ").append(2 - status[m][k]).append("(").append(skeletonCodes[m]).append(")");
                    }
                    System.out.printf("%nPROBLEM: strange of skeletons does not increase! "
                        + "For example, configuration 0x%h (3x3) inside 0x%h (5x5):%n%sStrengths: %s%n",
                        bitsA,
                        AnalyseSkeletonConfigurations.getAllBitsB(array8x8),
                        AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8, "  ", true),
                        sb);
                    break;
                }
            }
            System.out.println();
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "%n%d configurations 3x3 were tested in %.3f seconds%n%s%n",
            count3x3, (t2 - t1) * 1e-9,
            problemsFound ? "PROBLEMS FOUND" : "No problems found");
    }
}
