package net.algart.matrices.skeletons.demo;

import net.algart.arrays.BitArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.arrays.UpdatableBitArray;
import net.algart.matrices.skeletons.*;

import java.util.Locale;

public class VerifySkeletonSymmetry {
    private static final SimpleMemoryModel SMM = SimpleMemoryModel.getInstance();

    private boolean quickCheckZeroCenter = false;
    private boolean topological = false;
    private boolean diagonal = true;
    private final Class<? extends ThinningSkeleton> skeletonType;

    public VerifySkeletonSymmetry(String[] args) throws UsageException {
        int startArgIndex = 0;
        for (;;) {
            if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-quickCheckZeroCenter")) {
                quickCheckZeroCenter = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-topological")) {
                topological = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-straightOnly")) {
                diagonal = false;
                startArgIndex++;
            } else {
                break;
            }
        }
        if (args.length < startArgIndex + 1) {
            System.out.println("Usage:");
            System.out.println("    " + getClass().getName());
            System.out.println("      [-quickCheckZeroCenter] [-topological] [-straightOnly]");
            System.out.println("      [Weak]OctupleThinningSkeleton2D|[Strong]Quadruple3x5ThinningSkeleton2D");
            System.out.println("If -quickCheckZeroCenter, the test just checks whether zero center always stays zero");
            throw new UsageException();
        }
        skeletonType = AnalyseSkeletonConfigurations.getSkeletonType(args[startArgIndex]);
    }

    public void testSymmetry() {
        Matrix<UpdatableBitArray> matrix8x8 = SMM.newBitMatrix(8, 8);
        Matrix<UpdatableBitArray> matrix8x8_90 = SMM.newBitMatrix(8, 8);
        Matrix<UpdatableBitArray> matrix8x8_180 = SMM.newBitMatrix(8, 8);
        Matrix<UpdatableBitArray> matrix8x8_270 = SMM.newBitMatrix(8, 8);
        UpdatableBitArray array8x8 = matrix8x8.array();
        UpdatableBitArray array8x8_90 = matrix8x8_90.array();
        UpdatableBitArray array8x8_180 = matrix8x8_180.array();
        UpdatableBitArray array8x8_270 = matrix8x8_270.array();
        Matrix<? extends BitArray> thinningXP = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8).asThinning(0);
        Matrix<? extends BitArray> thinningXPYP = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8).asThinning(1);
        Matrix<? extends BitArray> thinningYP = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8_90).asThinning(2);
        Matrix<? extends BitArray> thinningXMYP = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8_90).asThinning(3);
        Matrix<? extends BitArray> thinningXM = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8_180).asThinning(4);
        Matrix<? extends BitArray> thinningXMYM = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8_180).asThinning(5);
        Matrix<? extends BitArray> thinningYM = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8_270).asThinning(6);
        Matrix<? extends BitArray> thinningXPYM = AnalyseSkeletonConfigurations.getSkeleton(
            skeletonType, diagonal, topological, matrix8x8_270).asThinning(7);
        boolean only4Direction = thinningXPYP == matrix8x8 && thinningXMYP == matrix8x8_90
            && thinningXMYM == matrix8x8_180 && thinningXPYM == matrix8x8_270;
        UpdatableBitArray thinnedXP = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedXPYP = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedYP = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedXMYP = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedXM = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedXMYM = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedYM = SMM.newUnresizableBitArray(64);
        UpdatableBitArray thinnedXPYM = SMM.newUnresizableBitArray(64);
        System.out.printf("Testing %d directions%n", only4Direction ? 4 : 8);
        boolean problemsFound = false;
        long t1 = System.nanoTime();
        int total5x5 = 1 << 24;
        for (int bits = 0; bits < total5x5; bits++) {
            if ((bits & 4095) == 4095) {
                double part = bits / (double) total5x5;
                double time = (System.nanoTime() - t1) * 1e-9;
                System.err.printf(Locale.US,
                    "\r(%.1f%%, %.0f/%.0f sec) %d 5x5 configurations checked for symmetry  \r",
                    part * 100, time, time / part, bits);
            }
            array8x8.fill(false);
            array8x8_90.copy(array8x8);
            array8x8_180.copy(array8x8);
            array8x8_270.copy(array8x8);
            int bitIndex = 0;
            for (int y = 0; y <= 4; y++) {
                for (int x = 0; x <= 4; x++) {
                    if (x == 2 && y == 2) {
                        continue;
                    }
                    boolean b = (bits & (1 << bitIndex)) != 0;
                    array8x8.setBit(y * 8 + x, b);
                    array8x8_90.setBit(x * 8 + (4 - y), b);
                    array8x8_180.setBit((4 - y) * 8 + (4 - x), b);
                    array8x8_270.setBit((4 - x) * 8 + y, b);
                    bitIndex++;
                }
            }
            assert bitIndex == 24;

            if (thinningXP.array().getBit(2 * 8 + 2)) {
                System.out.printf("ZERO BECOMES UNIT IN XP at the center for%n%s%n",
                    AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                throw new AssertionError("Invalid skeleton: 0 center becomes 1");
            }
            if (thinningYP.array().getBit(2 * 8 + 2)) {
                System.out.printf("ZERO BECOMES UNIT IN YP at the center for%n%s%n",
                    AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                throw new AssertionError("Invalid skeleton: 0 center becomes 1");
            }
            if (thinningXM.array().getBit(2 * 8 + 2)) {
                System.out.printf("ZERO BECOMES UNIT IN XM at the center for%n%s%n",
                    AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                throw new AssertionError("Invalid skeleton: 0 center becomes 1");
            }
            if (thinningYM.array().getBit(2 * 8 + 2)) {
                System.out.printf("ZERO BECOMES UNIT IN YM at the center for%n%s%n",
                    AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                throw new AssertionError("Invalid skeleton: 0 center becomes 1");
            }
            if (!only4Direction) {
                if (thinningXPYP.array().getBit(2 * 8 + 2)) {
                    System.out.printf("ZERO BECOMES UNIT IN XPYP at the center for%n%s%n",
                        AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                    throw new AssertionError("Invalid skeleton: 0 center becomes 1");
                }
                if (thinningXMYP.array().getBit(2 * 8 + 2)) {
                    System.out.printf("ZERO BECOMES UNIT IN XMYP at the center for%n%s%n",
                        AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                    throw new AssertionError("Invalid skeleton: 0 center becomes 1");
                }
                if (thinningXMYM.array().getBit(2 * 8 + 2)) {
                    System.out.printf("ZERO BECOMES UNIT IN XMYM at the center for%n%s%n",
                        AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                    throw new AssertionError("Invalid skeleton: 0 center becomes 1");
                }
                if (thinningXPYM.array().getBit(2 * 8 + 2)) {
                    System.out.printf("ZERO BECOMES UNIT IN XPYM at the center for%n%s%n",
                        AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                    throw new AssertionError("Invalid skeleton: 0 center becomes 1");
                }
            }
            if (quickCheckZeroCenter) {
                continue;
            }

            array8x8.setBit(2 * 8 + 2, true);
            array8x8_90.setBit(2 * 8 + 2, true);
            array8x8_180.setBit(2 * 8 + 2, true);
            array8x8_270.setBit(2 * 8 + 2, true);

            thinnedXP.copy(thinningXP.array());
            thinnedYP.copy(thinningYP.array());
            thinnedXM.copy(thinningXM.array());
            thinnedYM.copy(thinningYM.array());
            if (!only4Direction) {
                thinnedXPYP.copy(thinningXPYP.array());
                thinnedXMYP.copy(thinningXMYP.array());
                thinnedXMYM.copy(thinningXMYM.array());
                thinnedXPYM.copy(thinningXPYM.array());
            }
            int b1, b2, b3, b4;
            b1 = thinnedXP.getBit(2 * 8 + 2) ? 1 : 0;
            b2 = thinnedYP.getBit(2 * 8 + 2) ? 1 : 0;
            b3 = thinnedXM.getBit(2 * 8 + 2) ? 1 : 0;
            b4 = thinnedYM.getBit(2 * 8 + 2) ? 1 : 0;
            if (b1 != b2 || b1 != b3 || b1 != b4) {
                problemsFound = true;
                System.out.printf("ASYMMETRY at the center: xp=%d, yp=%d, xm=%d, ym=%d for%n%s%n",
                    b1, b2, b3, b4,
                    AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
            }
            if (!only4Direction) {
                b1 = thinnedXPYP.getBit(2 * 8 + 2) ? 1 : 0;
                b2 = thinnedXMYP.getBit(2 * 8 + 2) ? 1 : 0;
                b3 = thinnedXMYM.getBit(2 * 8 + 2) ? 1 : 0;
                b4 = thinnedXPYM.getBit(2 * 8 + 2) ? 1 : 0;
                if (b1 != b2 || b1 != b3 || b1 != b4) {
                    problemsFound = true;
                    System.out.printf("ASYMMETRY at the center: xpyp=%d, xmyp=%d, xmym=%d, xpym=%d for%n%s%n",
                        b1, b2, b3, b4,
                        AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5), "  ", true));
                }
            }
            // Theoretically, the following checking is not necessary, if we are sure
            // that the skeleton really depends on 5x5 aperture only
            for (int y = 0; y <= 4; y++) {
                for (int x = 0; x <= 4; x++) {
                    b1 = thinnedXP.getBit(y * 8 + x) ? 1 : 0;
                    b2 = thinnedYP.getBit(x * 8 + (4 - y)) ? 1 : 0;
                    b3 = thinnedXM.getBit((4 - y) * 8 + (4 - x)) ? 1 : 0;
                    b4 = thinnedYM.getBit((4 - x) * 8 + y) ? 1 : 0;
                    if (b1 != b2 || b1 != b3 || b1 != b4) {
                        problemsFound = true;
                        System.out.printf("ASYMMETRY at the pixel (x,y)=(%d,%d): "
                            + "xp=%d, yp=%d, xm=%d, ym=%d for%n%s%n",
                            x, y, b1, b2, b3, b4,
                            AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5),
                                "  ", true));
                    }
                    if (!only4Direction) {
                        b1 = thinnedXPYP.getBit(y * 8 + x) ? 1 : 0;
                        b2 = thinnedXMYP.getBit(x * 8 + (4 - y)) ? 1 : 0;
                        b3 = thinnedXMYM.getBit((4 - y) * 8 + (4 - x)) ? 1 : 0;
                        b4 = thinnedXPYM.getBit((4 - x) * 8 + y) ? 1 : 0;
                        if (b1 != b2 || b1 != b3 || b1 != b4) {
                            problemsFound = true;
                            System.out.printf("ASYMMETRY at the pixel (x,y)=(%d,%d): "
                                + "xp=%d, yp=%d, xm=%d, ym=%d for%n%s%n",
                                x, y, b1, b2, b3, b4,
                                AnalyseSkeletonConfigurations.bitMatrixToString(matrix8x8.subMatr(0, 0, 5, 5),
                                    "  ", true));
                        }
                    }
                }
            }
        }
        long t2 = System.nanoTime();
        System.err.printf("\r%78s\r", "");
        System.out.printf(Locale.US, "%d configurations 5x5 were verified in %.3f seconds%n%s%n",
            total5x5, (t2 - t1) * 1e-9,
            problemsFound ? "PROBLEMS FOUND" : "No problems found");
    }

    public static void main(String[] args) {
        VerifySkeletonSymmetry verifier;
        try {
            verifier = new VerifySkeletonSymmetry(args);
        } catch (UsageException e) {
            return;
        }
        verifier.testSymmetry();
    }
}
