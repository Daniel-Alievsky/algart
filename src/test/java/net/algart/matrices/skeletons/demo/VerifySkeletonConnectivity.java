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

package net.algart.matrices.skeletons.demo;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.skeletons.*;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VerifySkeletonConnectivity {

    private static final SimpleMemoryModel SMM = SimpleMemoryModel.getInstance();
    private static final int BUFFER_LENGTH = 32768;

    private boolean estimateComplexity = false;
    private boolean diagonal = false;
    private boolean topological = false;
    private final Class<? extends ThinningSkeleton> skeletonType;

    private final int pDimX = 3, pDimY = 5;
    private int qDimX, qDimY, nX, nY, nXY;
    private int cX, cY, pMinX, pMinY, pMaxX, pMaxY;
    private long numberOfMatricesQ;
    private long numberOfMatricesP;
    private int[] cNeighboursBitIndexes;
    private int[] pBitIndexes;
    private int[] qBitIndexes;

    private Matrix<UpdatableBitArray> matrixQ;
    private Matrix<UpdatableBitArray> matrixQWork;
    private Matrix<UpdatableBitArray> matrixQLarge;
    private Matrix<UpdatableBitArray> matrixQLargeWork;
    private UpdatableBitArray arrayQ;
    private UpdatableBitArray arrayQWork;
    private UpdatableBitArray arrayQForScanning;
    private UpdatableBitArray arrayQLarge;
    private UpdatableBitArray arrayQLargeWork;
    private BitArray thinningXPOrXPYP;
    private BitArray thinningXPOrXPYPLarge;
    private ConnectedObjectScanner clearingScanner, uncheckedClearingScanner;
    private long[] bitArrayQ = new long[1];
    private long[] bitArrayQWork = new long[1];

    private volatile Pattern dependenceAperture = null;

    private final long minPCode, maxPCode;
    private long countP = 0;
    private long countConnectiveP = 0;
    private long countQ = 0;

    public VerifySkeletonConnectivity(String[] args) throws UsageException {
        int startArgIndex = 0;
        for (;;) {
            if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-estimateComplexity")) {
                estimateComplexity = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-diagonal")) {
                diagonal = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-topological")) {
                topological = true;
                startArgIndex++;
            } else {
                break;
            }
        }
        if (args.length < startArgIndex + 3) {
            System.out.println("Usage:");
            System.out.println("    " + getClass().getName());
            System.out.println("      [-estimateComplexity] [-diagonal] [-topological]");
            System.out.println("      [Weak]OctupleThinningSkeleton2D|[Strong]Quadruple3x5ThinningSkeleton2D");
            System.out.println("      QX QY [minPCode [maxPCode]]");
            System.out.println("-diagonal must be also checked for 8-connected skeletons");
            throw new UsageException();
        }
        skeletonType = AnalyseSkeletonConfigurations.getSkeletonType(args[startArgIndex]);
        int qDimX = Integer.parseInt(args[startArgIndex + 1]);
        int qDimY = Integer.parseInt(args[startArgIndex + 2]);
        int cX = (qDimX - 1) / 2;
        int cY = (qDimY - 1) / 2;
        setDimensions(qDimX, qDimY, cX, cY);
        if (startArgIndex + 3 < args.length) {
            minPCode = Math.max(0, Long.parseLong(args[startArgIndex + 3]));
        } else {
            minPCode = 1; // don't check the degenerated 1-point case!
        }
        if (startArgIndex + 4 < args.length) {
            maxPCode = Math.min(numberOfMatricesP - 1, Long.parseLong(args[startArgIndex + 4]));
        } else {
            maxPCode = numberOfMatricesP - 1;
        }
    }

    public void analyseDependenceAperture() {
        // . . . . . .  0
        // P P P P P .  1
        // P P P P P .  2
        // P P 1 p P .  3
        // P P P P P .  4
        // P P P P P .  5
        // . . . . . .  6
        // 0 1 2 3 4 5
        final int logCpuCount = Math.min(4, // 2^4 = 16 kernels
            31 - Integer.numberOfLeadingZeros(Runtime.getRuntime().availableProcessors()));
        Thread[] tasks = new Thread[1 << logCpuCount];
        System.err.printf("Analysing aperture...");
        final boolean[] centerBits = new boolean[1 << 24];
        final AtomicBoolean
            notDependOnUnconnected = new AtomicBoolean(true),
            leftAlwaysZero = new AtomicBoolean(true),
            leftOrLeftTopAlwaysZero = new AtomicBoolean(true),
            leftOrLeftPairAlwaysZero = new AtomicBoolean(true),
            rightAlwaysUnit = new AtomicBoolean(true),
            rightAlwaysUnitBesidesIsolatedPixelCase = new AtomicBoolean(true),
            rightAlwaysUnitInSkeleton = new AtomicBoolean(true), // besides isolated center
            rightOrRightTopAlwaysUnitInSkeleton = new AtomicBoolean(true), // besides isolated center
            rightInSkeletonOrRightPairAlwaysUnit = new AtomicBoolean(true), // besides isolated center
            rightLocality = new AtomicBoolean(true); // besides isolated center
        final AtomicReference<Matrix<? extends BitArray>>
            notDependOnUnconnectedViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            leftAlwaysZeroViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            leftOrLeftTopAlwaysZeroViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            leftOrLeftPairAlwaysZeroViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightAlwaysUnitViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightAlwaysUnitBesidesIsolatedPixelCaseViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightAlwaysUnitInSkeletonViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightAlwaysUnitInSkeletonViolatingSkeleton = new AtomicReference<Matrix<? extends BitArray>>(),
            rightOrRightTopAlwaysUnitInSkeletonViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightOrRightTopAlwaysUnitInSkeletonViolatingSkeleton = new AtomicReference<Matrix<? extends BitArray>>(),
            rightInSkeletonOrRightPairAlwaysUnitViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightInSkeletonOrRightPairAlwaysUnitViolatingSkeleton = new AtomicReference<Matrix<? extends BitArray>>(),
            rightLocalityViolation = new AtomicReference<Matrix<? extends BitArray>>(),
            rightLocalityViolatingSkeleton = new AtomicReference<Matrix<? extends BitArray>>();
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        long t1 = System.nanoTime();
        for (int taskIndex = 0; taskIndex < tasks.length; taskIndex++) {
            // This task should be performed maximally quickly: it is just preprocessing and executed always
            final int ti = taskIndex;
            tasks[ti] = new Thread() {
                @Override
                public void run() {
                    try {
                        Matrix<UpdatableBitArray> matrix6x7 = SMM.newBitMatrix(6, 7);
                        Matrix<UpdatableBitArray> matrix6x7Save = SMM.newBitMatrix(6, 7);
                        Matrix<UpdatableBitArray> matrix6x7Work = SMM.newBitMatrix(6, 7);
                        final UpdatableBitArray array6x7 = matrix6x7.array();
                        BitArray thinningXPOrXPYP = getThinningXPOrXPYP(matrix6x7);
                        ConnectedObjectScanner clearer = ConnectedObjectScanner.getBreadthFirstScanner(
                            matrix6x7Work, ConnectivityType.STRAIGHT_AND_DIAGONAL);
                        ConnectedObjectScanner.ElementVisitor drawer = new ConnectedObjectScanner.ElementVisitor() {
                            public void visit(long[] coordinatesInMatrix, long indexInArray) {
                                array6x7.setBit(indexInArray);
                            }
                        };
                        int bitsFrom = ti << (24 - logCpuCount);
                        int bitsTo = bitsFrom + (1 << (24 - logCpuCount));
                        boolean[] centerBitsLocal = new boolean[1 << (24 - logCpuCount)];
                        for (int bits = bitsFrom; bits < bitsTo; bits++) {
                            if (exception.get() != null) {
                                return;
                            }
                            array6x7.fill(false);
                            array6x7.setBit(3 * 6 + 2, true);
                            int bitIndex = 0;
                            boolean hasNeighbour = false;
                            for (int y = 0; y < 5; y++) {
                                for (int x = 0, disp = (y + 1) * 6; x < 5; x++, disp++) {
                                    if (x == 2 && y == 2) {
                                        continue;
                                    }
                                    boolean b = (bits & 1 << bitIndex) != 0;
                                    if (y >= 1 && y <= 3 && x >= 1 && x <= 3) {
                                        hasNeighbour |= b;
                                    }
                                    array6x7.setBit(disp, b);
                                    bitIndex++;
                                }
                            }
                            assert bitIndex == 24;
                            boolean centerStaysUnit = thinningXPOrXPYP.getBit(3 * 6 + 2);
                            centerBitsLocal[bits - bitsFrom] = centerStaysUnit;
                            if (!centerStaysUnit) {
                                if (array6x7.getBit(3 * 6 + 1)) {
                                    leftAlwaysZero.set(false);
                                    leftAlwaysZeroViolation.compareAndSet(null,
                                        matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                    if (array6x7.getBit(2 * 6 + 1)) {
                                        leftOrLeftTopAlwaysZero.set(false);
                                        leftOrLeftTopAlwaysZeroViolation.compareAndSet(null,
                                            matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                    }
                                    if (array6x7.getBit(2 * 6 + 1) || array6x7.getBit(4 * 6 + 1)) {
                                        leftOrLeftPairAlwaysZero.set(false);
                                        leftOrLeftPairAlwaysZeroViolation.compareAndSet(null,
                                            matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                    }
                                }
                                if (!array6x7.getBit(3 * 6 + 3)) {
                                    rightAlwaysUnit.set(false);
                                    rightAlwaysUnitViolation.compareAndSet(null,
                                        matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                    if (hasNeighbour) {
                                        rightAlwaysUnitBesidesIsolatedPixelCase.set(false);
                                        rightAlwaysUnitBesidesIsolatedPixelCaseViolation.compareAndSet(null,
                                            matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));

                                    }
                                }
                                if (hasNeighbour) {
                                    boolean willHaveRightNeighbour = thinningXPOrXPYP.getBit(3 * 6 + 3);
                                    if (!willHaveRightNeighbour) {
                                        rightAlwaysUnitInSkeleton.set(false);
                                        rightAlwaysUnitInSkeletonViolation.compareAndSet(null,
                                            matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                        rightAlwaysUnitInSkeletonViolatingSkeleton.compareAndSet(null,
                                            matrix6x7.matrix(thinningXPOrXPYP.updatableClone(Arrays.SMM)));
                                        boolean hasRightTopNeighbour = array6x7.getBit(2 * 6 + 3);
                                        boolean hasRightBottomNeighbour = array6x7.getBit(4 * 6 + 3);
                                        boolean willHaveRightTopNeighbour = true;
                                        for (int top3Bits = 0; top3Bits < 8; top3Bits++) {
                                            for (int x = 0; x < 3; x++) {
                                                array6x7.setBit(2 + x, (top3Bits & 1 << x) != 0);
                                            }
                                            if (!thinningXPOrXPYP.getBit(2 * 6 + 3)) {
                                                willHaveRightTopNeighbour = false;
                                                break; // save "bad" configuration at finish
                                            }
                                        }
                                        boolean willHaveRightBottomNeighbour = true;
                                        for (int bottom3Bits = 0; bottom3Bits < 8; bottom3Bits++) {
                                            for (int x = 0; x < 3; x++) {
                                                array6x7.setBit(6 * 6 + 2 + x, (bottom3Bits & 1 << x) != 0);
                                            }
                                            if (!thinningXPOrXPYP.getBit(4 * 6 + 3)) {
                                                willHaveRightBottomNeighbour = false;
                                                break; // save "bad" configuration at finish
                                            }
                                        }
                                        if (willHaveRightTopNeighbour && !hasRightTopNeighbour)
                                            throw new AssertionError(String.format(
                                                "ZERO BECOMES UNIT at (3,2):%n%s%nis transformed to%n%s",
                                                AnalyseSkeletonConfigurations.bitMatrixToString(
                                                    matrix6x7, 0, 1, 4, 5, 2, 3, "    ", true),
                                                AnalyseSkeletonConfigurations.bitMatrixToString(matrix6x7.matrix(
                                                    thinningXPOrXPYP), 0, 1, 4, 5, 2, 3, "    ", true)));
                                        if (willHaveRightBottomNeighbour && !hasRightBottomNeighbour)
                                            throw new AssertionError(String.format(
                                                "ZERO BECOMES UNIT at (3,4):%n%s%nis transformed to%n%s",
                                                AnalyseSkeletonConfigurations.bitMatrixToString(
                                                    matrix6x7, 0, 1, 4, 5, 2, 3, "    ", true),
                                                AnalyseSkeletonConfigurations.bitMatrixToString(matrix6x7.matrix(
                                                    thinningXPOrXPYP), 0, 1, 4, 5, 2, 3, "    ", true)));
                                        // the assertions above are completely verified by VerifySkeletonSymmetry
                                        if (!(willHaveRightTopNeighbour && hasRightBottomNeighbour)) {
                                            rightOrRightTopAlwaysUnitInSkeleton.set(false);
                                            rightOrRightTopAlwaysUnitInSkeletonViolation.compareAndSet(null,
                                                matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                            rightOrRightTopAlwaysUnitInSkeletonViolatingSkeleton.compareAndSet(null,
                                                matrix6x7.matrix(thinningXPOrXPYP.updatableClone(Arrays.SMM)));
                                        }
                                        if (!((willHaveRightTopNeighbour && hasRightBottomNeighbour) ||
                                            (willHaveRightBottomNeighbour && hasRightTopNeighbour)))
                                        {
                                            rightInSkeletonOrRightPairAlwaysUnit.set(false);
                                            rightInSkeletonOrRightPairAlwaysUnitViolation.compareAndSet(null,
                                                matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                            rightInSkeletonOrRightPairAlwaysUnitViolatingSkeleton.compareAndSet(null,
                                                matrix6x7.matrix(thinningXPOrXPYP.updatableClone(Arrays.SMM)));
                                        }
                                        boolean willHaveBottomNeighbour = true;
                                        if (!willHaveRightTopNeighbour && !willHaveRightBottomNeighbour) {
                                            for (int bottom3Bits = 0; bottom3Bits < 8; bottom3Bits++) {
                                                for (int x = 0; x < 3; x++) {
                                                    array6x7.setBit(6 * 6 + 1 + x, (bottom3Bits & 1 << x) != 0);
                                                }
                                                if (!thinningXPOrXPYP.getBit(4 * 6 + 2)) {
                                                    willHaveBottomNeighbour = false;
                                                    break; // save "bad" configuration at finish
                                                }
                                            }
                                            if (!willHaveBottomNeighbour) {
                                                rightLocality.set(false);
                                                rightLocalityViolation.compareAndSet(null,
                                                    matrix6x7.matrix(array6x7.updatableClone(Arrays.SMM)));
                                                rightLocalityViolatingSkeleton.compareAndSet(null,
                                                    matrix6x7.matrix(thinningXPOrXPYP.updatableClone(Arrays.SMM)));
                                            }
                                        }
                                    }
                                }
                                matrix6x7Save.array().copy(array6x7);
                                matrix6x7Work.array().copy(array6x7);
                                array6x7.fill(false);
                                clearer.clear(null, drawer, 2, 3);
//                            System.out.printf("%n%s%s",
//                                AnalyseSkeletonConfigurations.bitMatrixToString(matrix6x7Save,
//                                    0, 1, 4, 5, 2, 3, "    ", true),
//                                AnalyseSkeletonConfigurations.bitMatrixToString(matrix6x7,
//                                    0, 1, 4, 5, 2, 3, ">   ", true));
                                if (thinningXPOrXPYP.getBit(3 * 6 + 2)) {
                                    notDependOnUnconnected.set(false);
                                    notDependOnUnconnectedViolation.compareAndSet(null,
                                        matrix6x7.matrix(matrix6x7Save.array().updatableClone(Arrays.SMM)));
                                }
                            }
                        }
                        synchronized (centerBits) {
                            System.arraycopy(centerBitsLocal, 0,
                                centerBits, ti << (24 - logCpuCount), 1 << (24 - logCpuCount));
                        }
                    } catch (Throwable e) {
                        exception.compareAndSet(null, e);
                    }
                }
            };
        }
        for (Thread task : tasks) {
            task.start();
        }
        for (Thread task : tasks) {
            try {
                task.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.err.printf("\r%78s\r", "");
        if (exception.get() != null) {
            if (exception.get() instanceof Error)
                throw (Error) exception.get();
            if (exception.get() instanceof RuntimeException)
                throw (RuntimeException) exception.get();
            throw new AssertionError(exception.get());
        }
        Set<IPoint> points = new HashSet<IPoint>();
        points.add(IPoint.valueOf(0, 0));
        for (int bitIndex = 0; bitIndex < 24; bitIndex++) {
            boolean dependent = false;
            int mask = 1 << bitIndex;
            for (int bits = 0; bits < 1 << 24; bits++) {
                if (centerBits[bits] != centerBits[bits ^ mask]) {
                    dependent = true;
                    break;
                }
            }
            if (dependent) {
                int matrixIndex = bitIndex < 12 ? bitIndex : bitIndex + 1;
                points.add(IPoint.valueOf(matrixIndex % 5 - 2, matrixIndex / 5 - 2));
            }
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Aperture tested in %.3f seconds%n", (t2 - t1) * 1e-9);
        this.dependenceAperture = Patterns.newIntegerPattern(points);

        Matrix<UpdatableBitArray> matrix5x5 = SMM.newBitMatrix(5, 5); // zero filled
        matrix5x5.array().setBit(2 * 5 + 2, true);
        long aXMin = dependenceAperture.roundedCoordRange(0).min();
        long aXMax = dependenceAperture.roundedCoordRange(0).max();
        long aYMin = dependenceAperture.roundedCoordRange(1).min();
        long aYMax = dependenceAperture.roundedCoordRange(1).max();
        boolean dependenceApertureInside3x5 = aXMin >= -1 && aXMax <= 1 && aYMin >= -2 && aYMax <= 2;
        System.out.printf("The aperture of the skeleton: %s%n"
            + "The aperture ranges: (%d..%d)x(%d..%d)%n%s%n",
            dependenceAperture, aXMin, aXMax, aYMin, aYMax,
            AnalyseSkeletonConfigurations.bitMatrixToString(
                BasicMorphology.getInstance(null).dilation(matrix5x5, dependenceAperture).cast(BitArray.class),
                2, 2, 2, 2, 2, 2, "    ", true));
        System.out.printf("Condition, that the center does not depend on pixels from other 8-connected components, "
            + "%s%n%s",
            notDependOnUnconnected.get() && dependenceApertureInside3x5 ? "is fulfilled" :
                notDependOnUnconnected.get() ? "STAYS UNCHECKED, because the aperture is not inside (-1..1)x(-2..2)" :
                    "is VIOLATED: see below",
            notDependOnUnconnected.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                notDependOnUnconnectedViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true));
        System.out.printf("Condition I+ is %s%n%s",
            leftAlwaysZero.get() ?
                "fulfilled: the center is cleared only if the left neighbour is 0" :
                "violated: the center is sometimes cleared when the left neighbour is 1, see below",
            leftAlwaysZero.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                leftAlwaysZeroViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true));
        if (leftAlwaysZero.get()) {
            assert leftOrLeftPairAlwaysZero.get();
        } else {
            System.out.printf("Condition I is %s%n%s",
                leftOrLeftPairAlwaysZero.get() ?
                    "fulfilled: the center is cleared only if the left neighbour is 0 or "
                        + "both left-top and is left-bottom are 0" :
                    "VIOLATED: the center is sometimes cleared when left neighbour is 0 and "
                        + "left-top or left-bottom is 1, see below",
                leftOrLeftPairAlwaysZero.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                    leftOrLeftPairAlwaysZeroViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true));
        }
        if (leftOrLeftPairAlwaysZero.get()) {
            assert leftOrLeftTopAlwaysZero.get();
        } else {
            System.out.printf("Condition I- is %s%n%s",
                leftOrLeftTopAlwaysZero.get() ?
                    "fulfilled: the center is cleared only if the left or left-top neighbour is 0" :
                    "VIOLATED: the center is sometimes cleared when both left and left-top neighbours are 1, "
                        + "see below",
                leftOrLeftTopAlwaysZero.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                    leftOrLeftTopAlwaysZeroViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true));
        }
        if (!dependenceApertureInside3x5) {
            System.out.printf("Conditions Ia-, Ia, Ia+ (right neighbours in the skeleton) "
                + "STAY UNCHECKED, because the aperture is not inside (-1..1)x(-2..2)%n");
        } else {
            System.out.printf("Condition Ia+ is %s%n%s",
                rightAlwaysUnitInSkeleton.get() ?
                    "fulfilled: if a non-isolated center is cleared, the right neighbour in skeleton stays 1 always" :
                    "violated: when a non-isolated center is cleared, the right neighbour in skeleton becomes "
                        + "0 sometimes, see below",
                rightAlwaysUnitInSkeleton.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                    rightAlwaysUnitInSkeletonViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true)
                    + "is transformed to" + String.format("%n") + AnalyseSkeletonConfigurations.bitMatrixToString(
                    rightAlwaysUnitInSkeletonViolatingSkeleton.get(), 0, 1, 4, 5, 2, 3, "    ", true));
            if (rightAlwaysUnitInSkeleton.get()) {
                assert rightOrRightTopAlwaysUnitInSkeleton.get();
            } else {
                System.out.printf("Condition Ia is %s%n%s",
                    rightOrRightTopAlwaysUnitInSkeleton.get() ?
                        "fulfilled: if a non-isolated center is cleared, then"
                            + String.format("%n  ") + "the right neighbour stays 1 in skeleton or"
                            + String.format("%n  ") + "{right-top neighbour stays 1 and right-bottom one was 1}" :
                        "VIOLATED: when a non-isolated center is cleared, sometimes it is false that"
                            + String.format("%n  ") + "the right neighbour stays 1 in skeleton or"
                            + String.format("%n  ") + "{right-top neighbour stays 1 and right-bottom one was 1}"
                            + ", see below",
                    rightOrRightTopAlwaysUnitInSkeleton.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                        rightOrRightTopAlwaysUnitInSkeletonViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true)
                        + "is transformed to" + String.format("%n") + AnalyseSkeletonConfigurations.bitMatrixToString(
                        rightOrRightTopAlwaysUnitInSkeletonViolatingSkeleton.get(), 0, 1, 4, 5, 2, 3, "    ", true));
            }
            if (rightOrRightTopAlwaysUnitInSkeleton.get()) {
                assert rightInSkeletonOrRightPairAlwaysUnit.get();
            } else {
                System.out.printf("Condition Ia- is %s%n%s",
                    rightInSkeletonOrRightPairAlwaysUnit.get() ?
                        "fulfilled: if a non-isolated center is cleared, then"
                            + String.format("%n  ") + "the right neighbour stays 1 in skeleton or"
                            + String.format("%n  ") + "{right-top neighbour stays 1 and right-bottom one was 1} or"
                            + String.format("%n  ") + "{right-bottom neighbour stays 1 and right-top one was 1}" :
                        "VIOLATED: when a non-isolated center is cleared, sometimes it is false that"
                            + String.format("%n  ") + "the right neighbour stays 1 in skeleton or"
                            + String.format("%n  ") + "{right-top neighbour stays 1 and right-bottom one was 1} or"
                            + String.format("%n  ") + "{right-bottom neighbour stays 1 and right-top one was 1}"
                            + ", see below",
                    rightInSkeletonOrRightPairAlwaysUnit.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                        rightInSkeletonOrRightPairAlwaysUnitViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true)
                        + "is transformed to" + String.format("%n") + AnalyseSkeletonConfigurations.bitMatrixToString(
                        rightInSkeletonOrRightPairAlwaysUnitViolatingSkeleton.get(), 0, 1, 4, 5, 2, 3, "    ", true));
            }
            if (rightInSkeletonOrRightPairAlwaysUnit.get()) {
                assert rightLocality.get();
            } else {
                System.out.printf("Condition Ia-- of \"right locality\" is %s%n%s",
                    rightLocality.get() ?
                        "fulfilled: if a non-isolated center is cleared, then"
                            + String.format("%n  ") + "the right neighbour stays 1 in skeleton or"
                            + String.format("%n  ") + "right-top neighbour stays 1 or"
                            + String.format("%n  ") + "right-bottom neighbour stays 1 or"
                            + String.format("%n  ") + "bottom neighbour stays 1" :
                        "VIOLATED: when a non-isolated center is cleared, sometimes it is false that"
                            + String.format("%n  ") + "the right neighbour stays 1 in skeleton or"
                            + String.format("%n  ") + "right-top neighbour stays 1 or"
                            + String.format("%n  ") + "right-bottom neighbour stays 1 or"
                            + String.format("%n  ") + "bottom neighbour stays 1"
                            + ", see below",
                    rightLocality.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                        rightLocalityViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true)
                        + "is transformed to" + String.format("%n") + AnalyseSkeletonConfigurations.bitMatrixToString(
                        rightLocalityViolatingSkeleton.get(), 0, 1, 4, 5, 2, 3, "    ", true));
            }
        }
        System.out.printf("Condition Ic+ is %s%n%s",
            rightAlwaysUnit.get() ?
                "fulfilled: the center is cleared only if the right neighbour is 1" :
                "violated: the center is sometimes cleared when the right neighbour is 0, see below",
            rightAlwaysUnit.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                rightAlwaysUnitViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true));
        if (rightAlwaysUnit.get()) {
            assert rightAlwaysUnitBesidesIsolatedPixelCase.get();
        } else {
            System.out.printf("Condition Ic is %s%n%s",
                rightAlwaysUnitBesidesIsolatedPixelCase.get() ?
                    "fulfilled: the non-isolated center is cleared only if the right neighbour is 1" :
                    "VIOLATED: the non-isolated center is sometimes cleared when the right neighbour is 0, see below",
                rightAlwaysUnitBesidesIsolatedPixelCase.get() ? "" : AnalyseSkeletonConfigurations.bitMatrixToString(
                    rightAlwaysUnitBesidesIsolatedPixelCaseViolation.get(), 0, 1, 4, 5, 2, 3, "    ", true));
        }
        System.out.println();
    }

    public void testConnectivity() {
        // q q q q q .
        // q q q q q .
        // q P P P q .
        // q P P P q .
        // q P 1 P q .
        // q P P P q .
        // q P P P q .
        // q q q q q .
        // q q q q q .
        // . . . . . .
        // If qDimX or qDimY is not even, the center is shifted by 0.5 leftward (x-0.5) or upward (y-0.5)
        if (dependenceAperture == null)
            throw new IllegalStateException("dependenceAperture must be found");

        System.out.printf("Testing for connectivity %d configurations %dx%d=(%d..%d)x(%d..%d), center at (%d,%d)%n",
            numberOfMatricesP, pDimX, pDimY, pMinX, pMaxX, pMinY, pMaxY, cX, cY);
        allocateBuffers();
        long t1 = System.nanoTime(), tLast = t1;
        for (long bitsP = minPCode; bitsP <= maxPCode; bitsP++) {
            long t = System.nanoTime();
            double part = (bitsP - minPCode) / (double) (maxPCode - minPCode + 1);
            if (t - tLast > 200 * 1000 * 1000) { // 200 ms
                double time = (t - t1) * 1e-9;
                System.err.printf(Locale.US,
                    "\r(%.0f%%, %.0f/%.0f sec, pCode %d from %d..%d) %d configurations %dx%d done \r",
                    part * 100, time, time / part, bitsP, minPCode, maxPCode, countConnectiveP, pDimX, pDimY);
                tLast = t;
            }
            testPConfiguration(bitsP);
        } // bitsP
        long t2 = System.nanoTime();
        System.err.printf("\r%78s\r", "");
        System.out.printf(Locale.US, "%d connected configurations %dx%d %s in %.3f seconds%n"
            +"%d configurations %dx%d with clearing center found%n"
            +"%d configurations %dx%d at all%n"
            +"%,d configurations %dx%d %s (%.2f per each %dx%d)%n"
            +"All OK%n",
            countConnectiveP, pDimX, pDimY, estimateComplexity ? "are counted" : "are verified",
            (t2 - t1) * 1e-9,
            countP, pDimX, pDimY,
            numberOfMatricesP, pDimX, pDimY,
            countQ, qDimX, qDimY, estimateComplexity ? "are counted" : "are verified",
            countQ / (double) countP, pDimX, pDimY);
    }

    public void testPConfiguration(long bitsP) {
        arrayQ.fill(false);
        arrayQ.setBit(cY * nX + cX, true);
        for (int bitIndex = 0; bitIndex < pBitIndexes.length; bitIndex++) {
            boolean b = (bitsP & (1L << bitIndex)) != 0;
            arrayQ.setBit(pBitIndexes[bitIndex], b);
        }

        arrayQ.getBits(0, bitArrayQ, 0, nX * qDimY);
        final long wP = bitArrayQ[0];
        if (thinningXPOrXPYP.getBit(cY * nX + cX)) {
            return; // the center is not removed
        }
//            System.out.printf("%n%d %dx%d%n%s%n", bitsP, pDimX, pDimY,
//                qMatrixToString(matrixQ.subMatr(0, 0, qDimX, qDimY)));

        countP++;
        arrayQForScanning.fill(false);
        arrayQForScanning.copy(arrayQ); // first line and column are empty
        clearingScanner.clear(null, cX, cY);
        if (arrayQForScanning.indexOf(0, arrayQForScanning.length(), true) != -1) {
            return; // the source matrix is not a single connected component
        }
        countConnectiveP++;

        if (bitsP != 0) { // degenerated 1-point case
            arrayQWork.copy(arrayQ);
            arrayQWork.clearBit(cY * nX + cX); // clearing only 1 bit at the center
            int neighbourIndex = -1;
            for (int i : cNeighboursBitIndexes) {
                if (arrayQWork.getBit(i)) {
                    neighbourIndex = i;
                    break;
                }
            }
            if (neighbourIndex == -1)
                throw new AssertionError("Bug in ConnectedObjectScanner: "
                    + "cannot find a neighbour in a connected object");
            arrayQForScanning.copy(arrayQWork);
            long clearedCount = clearingScanner.clear(null, neighbourIndex % nX, neighbourIndex / nX);
            assert clearedCount > 0 : "nothing to clear (A), bitsP = " + bitsP;  // we've found non-zero neighbour
            arrayQForScanning.getBits(0, bitArrayQWork, 0, nX * qDimY);
            if (bitArrayQWork[0] != 0) {
                arrayQWork.copy(thinningXPOrXPYP);
                System.err.printf("\r%78s\r", "");
                System.out.printf("LOCAL CONNECTIVITY ERROR DETECTED at pCode %d: connectivity is broken "
                    + "after removing only the center in %dx%d aperture%n%s%n"
                    + "is transformed to%n%s%n",
                    bitsP, pDimX, pDimY, qMatrixToString(matrixQ), qMatrixToString(matrixQWork));
                throw new AssertionError("Invalid skeleton: connectivity is broken after removing only the center");
            }
        }

        long remainingIterations = numberOfMatricesQ;
        countQ += remainingIterations;
        if (estimateComplexity) {
            return;
        }
        arrayQLarge.fill(false);
        arrayQLargeWork.fill(false);
        long pos = 0;
        for (long bitsQ = 0; bitsQ < numberOfMatricesQ; bitsQ++) {
            remainingIterations--;
            long wQ = wP;
            for (int bitIndex = 0; bitIndex < qBitIndexes.length; bitIndex++) {
                if ((bitsQ & (1 << bitIndex)) != 0) {
                    wQ |= 1L << qBitIndexes[bitIndex];
                }
            }
            bitArrayQ[0] = wQ;
            if (pos < BUFFER_LENGTH) { // accumulate in buffer
                arrayQLarge.setBits(pos * nXY, bitArrayQ, 0, nX * qDimY);
                pos++;
            }
            if (pos == BUFFER_LENGTH || remainingIterations == 0) {
                arrayQLargeWork.subArr(0, pos * nXY).copy(thinningXPOrXPYPLarge); // actual skeletonization
                for (long k = 0; k < pos; k++) {
//                        arrayQ.setBits(0, bitArrayQ, 0, nX * qDimY);
                    arrayQLargeWork.getBits(k * nXY, bitArrayQWork, 0, nX * qDimY);
                    final long wThinnedQ = bitArrayQWork[0];
                    int neighbourIndex = -1;
                    for (int i : cNeighboursBitIndexes) {
                        if ((wThinnedQ & (1 << i)) != 0) {
                            neighbourIndex = i;
                            break;
                        }
                    }
                    if (neighbourIndex == -1) {
                        System.err.printf("\r%78s\r", "");
                        System.out.printf("LOCALITY ERROR DETECTED at pCode %d: "
                            + "no neighbours of the center in the skeleton%n%s%n"
                            + "is transformed to%n%s%n", bitsP,
                            qMatrixToString(matrixQLarge.subMatr(0, k * nY, nX, nY)),
                            qMatrixToString(matrixQLargeWork.subMatr(0, k * nY, nX, nY)));
                        throw new AssertionError("Too strong skeleton: no unit neighbours of the removed center");
                    }

                    bitArrayQWork[0] = wThinnedQ;
                    arrayQForScanning.fill(false); // empty first 2 lines and the last line
                    arrayQForScanning.setBits(2 * nX, bitArrayQWork, 0, nX * qDimY);
                    long clearedCount = uncheckedClearingScanner.clear(null,
                        neighbourIndex % nX, 2 + neighbourIndex / nX);
                    assert clearedCount > 0 : "nothing to clear (B)";  // we've found non-zero neighbour
                    arrayQForScanning.getBits(2 * nX, bitArrayQWork, 0, nX * qDimY);
                    if (bitArrayQWork[0] != 0) { // it is possible if the original configuration is not connected
                        arrayQForScanning.subArr(2 * nX, nX * qDimY).copy(arrayQLarge.subArr(k * nXY, nX * qDimY));
                        clearedCount = clearingScanner.clear(null, cX, 2 + cY);
                        assert clearedCount > 0 : "nothing to clear (C)";
                        arrayQForScanning.getBits(2 * nX, bitArrayQWork, 0, nX * qDimY);
                        boolean connectedQ = bitArrayQWork[0] == 0;
                        if (!connectedQ) {
                            continue;
                        }
                        System.err.printf("\r%78s\r", "");
                        System.out.printf("Common connectivity error detected at pCode %d: "
                            + "connectivity is broken%n%s%n"
                            + "is transformed to%n%s%n", bitsP,
                            qMatrixToString(matrixQLarge.subMatr(0, k * nY, nX, nY)),
                            qMatrixToString(matrixQLargeWork.subMatr(0, k * nY, nX, nY)));
                        throw new AssertionError("Invalid skeleton: connectivity is broken");
                    }
                }
                pos = 0;
            }
        } // bitsQ
    }

    private void setDimensions(int qDimX, int qDimY, int cX, int cY) {
        if (qDimX < pDimX)
            throw new IllegalArgumentException("QX must be not less than " + pDimX);
        if (qDimY < pDimY)
            throw new IllegalArgumentException("QY must be not less than " + pDimY);
        this.qDimX = qDimX;
        this.qDimY = qDimY;
        this.cX = cX;
        this.cY = cY;
        nX = qDimX + 1;
        nY = qDimY + 1;
        nXY = nX * nY;
        if (nX * qDimY > 64)
            throw new IllegalArgumentException("Cannot analyse so large aperture");
        pMinX = (qDimX - pDimX) / 2;
        pMinY = (qDimY - pDimY) / 2;
        pMaxX = pMinX + pDimX - 1;
        pMaxY = pMinY + pDimY - 1;
        numberOfMatricesP = 1L << (pDimX * pDimY - 1);
        numberOfMatricesQ = 1L << (qDimX * qDimY - pDimX * pDimY);
        cNeighboursBitIndexes = new int[8];
        int bitIndex = 0;
        for (int y = cY - 1; y <= cY + 1; y++) {
            for (int x = cX - 1; x <= cX + 1; x++) {
                if (x == cX && y == cY) {
                    continue;
                }
                cNeighboursBitIndexes[bitIndex++] = y * nX + x;
            }
        }
        assert bitIndex == cNeighboursBitIndexes.length;
        pBitIndexes = new int[pDimX * pDimY - 1];
        bitIndex = 0;
        for (int y = pMinY; y <= pMaxY; y++) {
            for (int x = pMinX; x <= pMaxX; x++) {
                if (x == cX && y == cY) {
                    continue;
                }
                pBitIndexes[bitIndex++] = y * nX + x;
            }
        }
        assert bitIndex == pBitIndexes.length;
        qBitIndexes = new int[qDimX * qDimY - pDimX * pDimY];
        bitIndex = 0;
        for (int y = 0; y < qDimY; y++) {
            for (int x = 0; x < qDimX; x++) {
                if (y >= pMinY && y <= pMaxY && x >= pMinX && x <= pMaxX) {
                    continue;
                }
                qBitIndexes[bitIndex++] = y * nX + x;
            }
        }
        assert bitIndex == qBitIndexes.length;
        matrixQ = SMM.newBitMatrix(nX, nY);
        matrixQWork = SMM.newBitMatrix(nX, nY);
        arrayQ = matrixQ.array();
        arrayQWork = matrixQWork.array();
        thinningXPOrXPYP = getThinningXPOrXPYP(matrixQ);

        Matrix<UpdatableBitArray> matrixQForScanning = SMM.newBitMatrix(nX, 2 + nY);
        arrayQForScanning = matrixQForScanning.array();
        clearingScanner = ConnectedObjectScanner.getBreadthFirstScanner(
            matrixQForScanning, ConnectivityType.STRAIGHT_AND_DIAGONAL);
        uncheckedClearingScanner = ConnectedObjectScanner.getUncheckedBreadthFirstScanner(
            matrixQForScanning, ConnectivityType.STRAIGHT_AND_DIAGONAL);
    }

    private void allocateBuffers() {
        matrixQLarge = SMM.newBitMatrix(nX, nY * BUFFER_LENGTH);
        matrixQLargeWork = SMM.newBitMatrix(nX, nY * BUFFER_LENGTH);
        arrayQLarge = matrixQLarge.array();
        arrayQLargeWork = matrixQLargeWork.array();
        assert arrayQLarge.length() <= Integer.MAX_VALUE;
        thinningXPOrXPYPLarge = getThinningXPOrXPYP(matrixQLarge);
    }

    private BitArray getThinningXPOrXPYP(Matrix<? extends UpdatableBitArray> matrix) {
        return AnalyseSkeletonConfigurations.getSkeleton(skeletonType,
            true, topological, matrix).asThinning(diagonal ? 1 : 0).array();
    }

    private String qMatrixToString(Matrix<? extends BitArray> matrix) {
        return AnalyseSkeletonConfigurations.bitMatrixToString(matrix.subMatr(0, 0, qDimX, qDimY),
            pMinX, pMinY, pMaxX, pMaxY, cX, cY, "  ", true);
    }

    public static void main(String[] args) {
        VerifySkeletonConnectivity verifier;
        try {
            verifier = new VerifySkeletonConnectivity(args);
        } catch (UsageException e) {
            return;
        }
        verifier.analyseDependenceAperture();
        verifier.testConnectivity();
    }

}
