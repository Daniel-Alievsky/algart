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

package net.algart.matrices.skeletons.demo;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.Morphology;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.skeletons.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Locale;

public class AnalyseSkeletonConfigurations {
    private static final boolean CHECK_ONLY_8_CONNECTED = false; // usually don't increase speed essentially

    static final int NUMBER_OF_BITS_A = 1 << 8;
    static final int NUMBER_OF_BITS_B = 1 << 16;
    static final int NUMBER_OF_ADDITIONAL_BITS_3X5_EVEN = 1 << 12;
    static final int NUMBER_OF_ADDITIONAL_BITS_3X5_ODD = 1 << 13;
    static final int NUMBER_OF_BITS_C = 1 << 24;

    static final int[] STABLY_IMPOSSIBLE_BITS_A_FOR_QUADRUPLE_3x5 = {
        // This list is saved in ac_q3x5_3_impossible3x3.txt by the call
        // ...AnalyseSkeletonConfigurations -writeConfigurations Quadruple3x5ThinningSkeleton2D 3
        // It must be renewed after any possible changes of Quadruple3x5ThinningSkeleton2D class.
        3,
        6,
        7,
        10,
        11,
        14,
        15,
        26,
        27,
        30,
        31,
        42,
        43,
        46,
        47,
        58,
        59,
        62,
        63,
        106,
        107,
        110,
        111,
        122,
        123,
        126,
        127,
    };

    static final int[] STABLY_IMPOSSIBLE_BITS_A_FOR_STRONG_QUADRUPLE_3x5 = {
        // This list is saved in ac_sq3x5_3_impossible3x3.txt by the call
        // ...AnalyseSkeletonConfigurations -writeConfigurations StrongQuadruple3x5ThinningSkeleton2D 3
        // It must be renewed after any possible changes of StrongQuadruple3x5ThinningSkeleton2D class.
        3,
        6,
        7,
        10,
        11,
        14,
        15,
        26,
        27,
        30,
        31,
        42,
        43,
        46,
        47,
        58,
        59,
        62,
        63,
        106,
        107,
        110,
        111,
        122,
        123,
        126,
        127,
        // The following configurations can be found by
        // ...AnalyseSkeletonConfigurations -writeConfigurations Quadruple3x5ThinningSkeleton2D 5
        // Note: StrongQuadruple3x5ThinningSkeleton2D is stronger than Quadruple3x5ThinningSkeleton2D
        // (it is checked by CompareSkeletonStrength)
        91,
        187,
        191,
    };

    private static final Morphology MORPHOLOGY = BasicMorphology.getInstance(null);
    private static final int BUFFER_LENGTH = 16384;

    private static final Pattern SQUARE_3X3 = Patterns.newRectangularIntegerPattern(
        IPoint.valueOf(-1, -1), IPoint.valueOf(1, 1));
    private static final Pattern TWO_RECTANGLES_3X5 = Patterns.newUnion(
        Patterns.newRectangularIntegerPattern(IPoint.valueOf(-2, -1), IPoint.valueOf(2, 1)),
        Patterns.newRectangularIntegerPattern(IPoint.valueOf(-1, -2), IPoint.valueOf(1, 2)));

    // C C C C C C C D
    // C B B B B B C D
    // C B A A A B C D
    // C B A 1 A B C D
    // C B A A A B C D
    // C B B B B B C D
    // C C C C C C C D
    // D D D D D D D D
    static final long[][] MASKS_1X1 = new long[9][1];
    static final long[] MASK_1X1 = new long[1];
    static final long[] MASK_3X1 = new long[1];
    static final long[] MASK_1X3 = new long[1];
    static final long[] MASK_3X3 = new long[1];
    static final long[] MASK_5X3 = new long[1];
    static final long[] MASK_3X5 = new long[1];
    static final long[] MASK_5X5 = new long[1];
    static {
        Matrix<UpdatableBitArray> matrix8x8 = Arrays.SMM.newBitMatrix(8, 8);
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(3, 3, 1, 1).array().fill(true);
        matrix8x8.array().getBits(0, MASK_1X1, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(2, 3, 3, 1).array().fill(true);
        matrix8x8.array().getBits(0, MASK_3X1, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(3, 2, 1, 3).array().fill(true);
        matrix8x8.array().getBits(0, MASK_1X3, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(2, 2, 3, 3).array().fill(true);
        matrix8x8.array().getBits(0, MASK_3X3, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(1, 2, 5, 3).array().fill(true);
        matrix8x8.array().getBits(0, MASK_5X3, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(2, 1, 3, 5).array().fill(true);
        matrix8x8.array().getBits(0, MASK_3X5, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(1, 1, 5, 5).array().fill(true);
        matrix8x8.array().getBits(0, MASK_5X5, 0, matrix8x8.size());
        matrix8x8.array().fill(false);
        matrix8x8.subMatr(0, 0, 7, 7).array().fill(true);
        for (int y = -1, k = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++, k++) {
                matrix8x8.array().fill(false);
                matrix8x8.subMatr(3 + x, 3 + y, 1, 1).array().fill(true);
                matrix8x8.array().getBits(0, MASKS_1X1[k], 0, matrix8x8.size());
            }
        }
    }

    // Z X-X-X X X Z .
    // X B B B b b X .
    // X B[A]A a b X .
    // X B A 1 a b X .
    // X B-A-A a b X .
    // X b b b b b X .
    // Z X X X X X Z .
    // . . . . . . . .
    private static final int MASK_C_Z = 1 | 1 << 6 | 1 << 12 | 1 << 18;
    // In 3x5-skeletons, the A pixel can depend on X pixel only in the following cases:
    // a) this X pixel has a unit B neighbour;
    // b) this X pixel has a unit X neighbour, which has a unit B neighbour.

    private boolean includeRotated = false;
    private boolean includeFilled5x5 = false;
    private boolean writeConfigurations = false;
    private boolean all5x5 = false;
    private boolean estimateComplexity = false;
    private boolean topological = false;
    private boolean diagonal = true;
    private String filePrefix;
    private final Class<? extends ThinningSkeleton> skeletonType;
    private final int analysingLevel;
    private final boolean skeleton3x5;
    private final BitSet checked3x3;

    MutableIntArray allPossible3x3 = null;
    MutableIntArray allImpossible3x3 = null;
    MutableIntArray allQuestionable3x3 = null;
    MutableIntArray allPossible3x3Rotated = null;
    MutableIntArray allImpossible3x3Rotated = null;
    MutableIntArray allQuestionable3x3Rotated = null;
    MutableIntArray[] illustratingB = null;
    MutableIntArray[] illustratingC = null;
    MutableIntArray[] questionableB = null;
    MutableIntArray[] improbableB = null;
    MutableIntArray[] impossibleB = null;
    long[] countB = null;

    public AnalyseSkeletonConfigurations(String[] args) throws UsageException, IOException {
        filePrefix = "ac_";
        int startArgIndex = 0;
        for (;;) {
            if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-all5x5")) {
                all5x5 = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-includeRotated")) {
                includeRotated = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-includeFilled5x5")) {
                includeFilled5x5 = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-writeConfigurations")) {
                writeConfigurations = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-estimateComplexity")) {
                estimateComplexity = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-topological")) {
                filePrefix += "t";
                topological = true;
                startArgIndex++;
            } else if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-straightOnly")) {
                filePrefix += "s";
                diagonal = false;
                startArgIndex++;
            } else {
                break;
            }
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage:");
            System.out.println("    " + getClass().getName());
            System.out.println("      [-includeRotated] [-includeFilled5x5] [-all5x5]"
                + " [-writeConfigurations] [-estimateComplexity] [-topological] [-straightOnly]");
            System.out.println("      [Weak]OctupleThinningSkeleton2D|[Strong]Quadruple3x5ThinningSkeleton2D 3|5|6"
                + " [NNN|listOf3x3Configurations.txt]");
            System.out.println("Number 3, 5 or 6 is the level of analysis; higher levels work more slowly.");
            System.out.println("-includeRotated is needless if the skeleton is correct: if checks whether"
                + " the algorithm is really symmetric");
            System.out.println("-all5x5 flag requires to find all possible 5x5 configurations and some impossible;"
                + " in this case the minimal level is 5.");
            System.out.println("-estimateComplexity flag allows to estimate number of operations for levels 5/6.");
            System.out.println("NNN is a concrete code of 3x3-configuration (0..255);"
                + " listOf3x3Configurations.txt contains several lines with such a code in each line.");
            System.out.println("Note: this utility saves results in the current directory "
                + "in several files ac_xxxx.txt");
            throw new UsageException();
        }
        skeletonType = getSkeletonType(args[startArgIndex]);
        if (skeletonType == OctupleThinningSkeleton2D.class) {
            skeleton3x5 = false;
            filePrefix += "o_";
        } else if (skeletonType == WeakOctupleThinningSkeleton2D.class) {
            skeleton3x5 = false;
            filePrefix += "wo_";
        } else if (skeletonType == Quadruple3x5ThinningSkeleton2D.class) {
            skeleton3x5 = true;
            filePrefix += "q3x5_";
        } else if (skeletonType == StrongQuadruple3x5ThinningSkeleton2D.class) {
            skeleton3x5 = true;
            filePrefix += "sq3x5_";
        } else {
            throw new IllegalArgumentException("Unknown skeletonization algorithm " + skeletonType);
        }
        if (args[startArgIndex + 1].equals("3")) {
            analysingLevel = all5x5 ? 5 : 3;
        } else if (args[startArgIndex + 1].equals("5")) {
            analysingLevel = 5;
        } else if (args[startArgIndex + 1].equals("6")) {
            analysingLevel = 6;
        } else {
            throw new IllegalArgumentException("The analysis level 3, 5 or 6 must be specified");
        }
        if (args.length > startArgIndex + 2) {
            boolean isBitsASpecified = true;
            int bitsA = 0;
            try {
                bitsA = Integer.parseInt(args[startArgIndex + 2]);
            } catch (NumberFormatException e) {
                isBitsASpecified = false;
            }
            if (isBitsASpecified) {
                if (bitsA < 0 || bitsA > 255) {
                    throw new IllegalArgumentException("The configuration code NNN must be in 0..255 range");
                }
                filePrefix += "c" + bitsA + "_" + analysingLevel + "_";
                checked3x3 = new BitSet(NUMBER_OF_BITS_A);
                checked3x3.set(bitsA);
            } else {
                filePrefix += "list_" + analysingLevel + "_";
                checked3x3 = readNumbersSet(args[startArgIndex + 2]);
            }
        } else {
            filePrefix += analysingLevel + "_";
            checked3x3 = new BitSet(NUMBER_OF_BITS_A);
            checked3x3.set(0, NUMBER_OF_BITS_A);
        }

        if (analysingLevel >= 5 || all5x5) {
            long t1 = System.nanoTime();
            int[] dummy = SortedBitsB.SORTED_BITS_B; // initializing
            dummy = SortedBitsC.SORTED_BITS_C; // initializing
            long t2 = System.nanoTime();
            if (t2 - t1 > 1e8) {
                System.out.printf(Locale.US, "Class is initialized in %.3f seconds%n", (t2 - t1) * 1e-9);
            }
        }
    }

    public void analyseConfigurations(boolean checkAll3x3, boolean checkAll5x5) {
        int[] stablyImpossibleBitsA = new int[0];
        if (allImpossible3x3 != null) {
            stablyImpossibleBitsA = Arrays.toJavaArray(allImpossible3x3); // checkAll5x5 mode
        } else if (skeletonType == Quadruple3x5ThinningSkeleton2D.class) {
            stablyImpossibleBitsA = STABLY_IMPOSSIBLE_BITS_A_FOR_QUADRUPLE_3x5;
        } else if (skeletonType == StrongQuadruple3x5ThinningSkeleton2D.class) {
            stablyImpossibleBitsA = STABLY_IMPOSSIBLE_BITS_A_FOR_STRONG_QUADRUPLE_3x5;
        }

        allPossible3x3 = Arrays.SMM.newEmptyIntArray();
        allImpossible3x3 = Arrays.SMM.newEmptyIntArray();
        allQuestionable3x3 = Arrays.SMM.newEmptyIntArray();
        allPossible3x3Rotated = Arrays.SMM.newEmptyIntArray();
        allImpossible3x3Rotated = Arrays.SMM.newEmptyIntArray();
        allQuestionable3x3Rotated = Arrays.SMM.newEmptyIntArray();
        illustratingB = new MutableIntArray[NUMBER_OF_BITS_A];
        illustratingC = new MutableIntArray[NUMBER_OF_BITS_A];
        questionableB = new MutableIntArray[NUMBER_OF_BITS_A];
        improbableB = new MutableIntArray[NUMBER_OF_BITS_A];
        impossibleB = new MutableIntArray[NUMBER_OF_BITS_A];
        countB = new long[NUMBER_OF_BITS_A];

        boolean isQuadruple3x5 = skeletonType == Quadruple3x5ThinningSkeleton2D.class
            || skeletonType == StrongQuadruple3x5ThinningSkeleton2D.class;
        System.out.println();
        System.out.println("Analysing " + skeletonType.getSimpleName()
            + (topological && !isQuadruple3x5 ? " topological" : "")
            + (!diagonal && !isQuadruple3x5 ? " straight-only" : "")
            + ", level " + analysingLevel
            + (checkAll5x5 ? ", full lists of 5x5 configurations" : ""));
        System.out.println("Known impossible 3x3 configurations: "
            + (stablyImpossibleBitsA.length == 0 ? "none" : JArrays.toString(stablyImpossibleBitsA, ",", 1000)));

        BitSet bitsAForConstructingLargeConfigurations = new BitSet(NUMBER_OF_BITS_A);
        bitsAForConstructingLargeConfigurations.set(0, NUMBER_OF_BITS_A);
        MutableIntArray ma = Arrays.SMM.newEmptyIntArray();
        for (int bitsA : stablyImpossibleBitsA) {
            add3x3(ma, bitsA, true);
        }
        boolean bitsAForConstructingLargeConfigurationsUsed = ma.length() > 0;
        for (long k = 0, n = ma.length(); k < n; k++) {
            bitsAForConstructingLargeConfigurations.clear(ma.getInt(k));
        }

        final Matrix<UpdatableBitArray> matrix8x8 = Arrays.SMM.newBitMatrix(8, 8);
        // - adding 1-pixel white space for correct skeletonization: 1 long value
        final long[] work8x8_1 = new long[1], work8x8_2 = new long[1];
        final Matrix<UpdatableBitArray> matrix8xN = Arrays.SMM.newBitMatrix(8, 8 * (long) BUFFER_LENGTH);
        final long[] work8xN_1 = new long[BUFFER_LENGTH], work8xN_2 = new long[BUFFER_LENGTH];
        final BitArray[] thinnedCache8x8 = new BitArray[8];
        final BitArray[] thinnedCache8xN = new BitArray[8];
        // new BitArray[16] for 8xN enables multithreading, but it seems to be antioptimization
        final byte[] status = new byte[BUFFER_LENGTH];

        final Matrix<UpdatableBitArray> matrix8x8Work = Arrays.SMM.newBitMatrix(8, 8);
        final UpdatableBitArray array8x8 = matrix8x8.array();
        final UpdatableBitArray array8xN = matrix8xN.array();
        final UpdatableBitArray save3x3Array8x8 = array8x8.updatableClone(Arrays.SMM);
        final UpdatableBitArray save5x5Array8x8 = array8x8.updatableClone(Arrays.SMM);
        final ThinningSkeleton skeleton8x8 = getSkeleton(skeletonType, diagonal, topological, matrix8x8);
        final ThinningSkeleton skeleton8xN = getSkeleton(skeletonType, diagonal, topological, matrix8xN);
        ConnectedObjectScanner scanner8x8OfWork = ConnectedObjectScanner.getBreadthFirstScanner(
            matrix8x8Work, ConnectivityType.STRAIGHT_AND_DIAGONAL);

//        for (int bitsA = 0; bitsA < NUMBER_OF_BITS_A; bitsA++) {
//            if (bitsAForConstructingLargeConfigurations.get(bitsA)) {
//                System.out.println(bitsA);
//            }
//        }
//        System.out.println("MASK_1X1");
//        array8x8.setBits(0, MASK_1X1, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));
//        System.out.println("MASK_3X1");
//        array8x8.setBits(0, MASK_3X1, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));
//        System.out.println("MASK_1X3");
//        array8x8.setBits(0, MASK_1X3, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));
//        System.out.println("MASK_3X3");
//        array8x8.setBits(0, MASK_3X3, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));
//        System.out.println("MASK_5X3");
//        array8x8.setBits(0, MASK_5X3, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));
//        System.out.println("MASK_3X5");
//        array8x8.setBits(0, MASK_3X5, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));
//        System.out.println("MASK_5X5");
//        array8x8.setBits(0, MASK_5X5, 0, array8x8.length());
//        System.out.println(bitMatrixToString(matrix8x8, ""));

        int count3x3 = 0;
        long t1 = System.nanoTime();
        for (int bitsA = 0; bitsA < NUMBER_OF_BITS_A; bitsA++) {
            if (!checkAll3x3 && !checked3x3.get(bitsA)) {
                continue;
            }
            synchronized (this) { // for a case of multithreading in future
                printStatus("[3x3c=%d]...", bitsA);
                if (!includeRotated) {
                    if (allPossible3x3Rotated.indexOf(0, allPossible3x3Rotated.length(), bitsA) != -1
                        || allImpossible3x3Rotated.indexOf(0, allImpossible3x3Rotated.length(), bitsA) != -1
                        || allQuestionable3x3Rotated.indexOf(0,
                        allQuestionable3x3Rotated.length(), bitsA) != -1)
                    {
                        continue;
                    }
                }
            }
            count3x3++;
            array8x8.fill(false);
            setAllBitsAAndCenter1(bitsA, array8x8);
            if (getAllShiftedBitsA(0, 0, array8x8) != bitsA)
                throw new AssertionError("Error in setAllBitsAAndCenter1/getAllShiftedBitsA");
            boolean possible3x3 = false;
            boolean impossible3x3 = false;
            MutableIntArray possibleBitsB = Arrays.SMM.newEmptyIntArray();
            MutableIntArray possibleBitsC = Arrays.SMM.newEmptyIntArray();
            MutableIntArray questionableBitsB = Arrays.SMM.newEmptyIntArray();
            MutableIntArray improbableBitsB = Arrays.SMM.newEmptyIntArray();
            MutableIntArray impossibleBitsB = Arrays.SMM.newEmptyIntArray();
            long count5x5 = 0;
            TestingThis3x3:
            {
                checkStability(status, skeleton8x8, MASK_3X3, MASK_3X3, work8x8_1, work8x8_2, thinnedCache8x8);
                if (status[0] != 0 && !checkAll5x5) {
                    possible3x3 = true;
                    break TestingThis3x3;
                }
                save3x3Array8x8.copy(array8x8);
                boolean all5x5Unstable = true;
                for (int bitsB : SortedBitsB.SORTED_BITS_B) {
                    array8x8.copy(save3x3Array8x8);
                    setAllBitsB(bitsB, array8x8);
                    if (getAllBitsB(array8x8) != bitsB)
                        throw new AssertionError("Error in setAllBitsB/getAllBitsB");
                    checkStability(status, skeleton8x8,
                        skeleton3x5 ? MASK_3X1 : MASK_3X3,
                        skeleton3x5 ? MASK_1X3 : MASK_3X3, work8x8_1, work8x8_2, thinnedCache8x8);
                    if (status[0] > 1 && !checkAll5x5) {
                        possible3x3 = true;
                        possibleBitsB.pushInt(bitsB);
                        break TestingThis3x3;
                    }
                    if (status[0] != 0) {
                        all5x5Unstable = false;
                    }
                }
                if (all5x5Unstable) {
                    // all 5x5 configurations (B pixels) destroy:
                    //     central A area (3x3 skeleton) or
                    //     central 3 horizontal or vertical pixels (3x5 skeleton)
                    impossible3x3 = true;
                    break TestingThis3x3;
                }
                if (skeleton3x5) {
                    for (int yCenter = -1, centerIndex = 0; yCenter <= 1; yCenter++) {
                        for (int xCenter = -1; xCenter <= 1; xCenter++, centerIndex++) {
                            if (xCenter == 0 && yCenter == 0) {
                                continue;
                            }
                            if (save3x3Array8x8.getBit((3 + yCenter) * 8 + 3 + xCenter)) {
                                printStatus("[3x3c=%d] (%d,%d)...", bitsA, xCenter, yCenter);
                                boolean allShifted5x5Unstable = true;
                                int m = Math.abs(xCenter) + Math.abs(yCenter) == 2 ?
                                    NUMBER_OF_ADDITIONAL_BITS_3X5_ODD :
                                    NUMBER_OF_ADDITIONAL_BITS_3X5_EVEN;
                                for (int bits3x5 = 0; bits3x5 < m; bits3x5++) {
                                    array8x8.copy(save3x3Array8x8);
                                    setAllShiftedBits3x5(bits3x5, yCenter, xCenter, array8x8);
                                    checkStability(status, skeleton8x8,
                                        MASKS_1X1[centerIndex],
                                        MASKS_1X1[centerIndex], work8x8_1, work8x8_2, thinnedCache8x8);
                                    if (status[0] > 1 && !checkAll5x5) {
                                        possible3x3 = true;
                                        possibleBitsB.pushInt(getAllBitsB(array8x8));
                                        possibleBitsC.pushInt(getAllBitsC(array8x8));
                                        break TestingThis3x3;
                                    }
                                    if (status[0] != 0) {
                                        allShifted5x5Unstable = false;
                                    }
                                }
                                if (allShifted5x5Unstable) {
                                    // all 5x5 configurations around (xCenter,yCenter) destroy this center
                                    impossible3x3 = true;
                                    break TestingThis3x3;
                                }
                            }
                        }
                    }
                }
                array8x8.copy(save3x3Array8x8);
                MORPHOLOGY.dilation(matrix8x8Work, matrix8x8, TWO_RECTANGLES_3X5, true);
                int maskCFor3x5 = getAllBitsC(matrix8x8Work.array());
                if ((maskCFor3x5 & MASK_C_Z) != 0)
                    throw new AssertionError("Invalid maskCFor3x5");
                if (analysingLevel >= 5 || checkAll5x5) { // 3x3 is questionable yet or we need the full list of 5x5
                    printStatus("[3x3c=%d] 5x5 testing...", bitsA);
                    long[] count5x5PerBitCount = new long[17];
                    long count7x7 = 0;
                    boolean all7x7Unstable = true;
                    for (int bitsB : SortedBitsB.SORTED_BITS_B) {
                        if (!includeFilled5x5 && bitsA == NUMBER_OF_BITS_A - 1 && bitsB == NUMBER_OF_BITS_B - 1) {
                            continue;
                        }
                        array8x8.copy(save3x3Array8x8);
                        setAllBitsB(bitsB, array8x8);
                        save5x5Array8x8.copy(array8x8);
                        // we are sure that it is unstable for mask5x5 for all bitsB
                        checkStability(status, skeleton8x8,
                            skeleton3x5 ? MASK_3X1 : MASK_3X3,
                            skeleton3x5 ? MASK_1X3 : MASK_3X3, work8x8_1, work8x8_2, thinnedCache8x8);
                        if (status[0] == 0) { // then it will be destroyed with any pixels C
                            continue;
                        }
                        if (CHECK_ONLY_8_CONNECTED || checkAll5x5) {
                            if (!isConnected(array8x8, scanner8x8OfWork)) {
                                continue;
                            }
                        }
                        if (bitsAForConstructingLargeConfigurationsUsed) {
                            if (containsIllegal3x3(array8x8, bitsAForConstructingLargeConfigurations)) {
                                continue;
                            }
                        }
                        array8x8.copy(save5x5Array8x8); // to be on the safe side
                        MORPHOLOGY.dilation(matrix8x8Work, matrix8x8, SQUARE_3X3, true);
                        int maskCFor3x3 = getAllBitsC(matrix8x8Work.array());
                        int maskC;
                        if (skeleton3x5) {
                            if (analysingLevel >= 6) {
                                maskC = (1 << 24) - 1;
                            } else {
                                maskC = extendUnitBitsX(maskCFor3x3) & maskCFor3x5;
                            }
                        } else {
                            maskC = maskCFor3x3;
                        }

//                        System.out.println();
//                        System.out.println(bitMatrixToString(matrix8x8, ""));
//                        matrix8x8Work.array().fill(false);
//                        setAllBitsC(maskCFor3x3, matrix8x8Work.array());
//                        System.out.println(bitMatrixToString(matrix8x8Work, "maskCFor3x3  "));
//                        matrix8x8Work.array().fill(false);
//                        setAllBitsC(maskCFor3x5, matrix8x8Work.array());
//                        System.out.println(bitMatrixToString(matrix8x8Work, "maskCFor3x5  "));
//                        matrix8x8Work.array().fill(false);
//                        setAllBitsC(maskC, matrix8x8Work.array());
//                        System.out.println(bitMatrixToString(matrix8x8Work, "maskC        "));
//                        System.out.println();

                        int remainingIterations = 1 << Integer.bitCount(maskC);
                        if (!estimateComplexity || count5x5 % 10 == 0) {
                            printStatus("[3x3c=%d] 5x5 testing [0x%X, %d bits] %d (%d, %s)...",
                                bitsA, bitsB, Integer.bitCount(bitsB), count5x5, possibleBitsB.length(),
                                all7x7Unstable ? "no" : "maybe");
                        }
                        count5x5++;
                        boolean possible5x5 = false;
                        boolean all7x7ForThis5x5Unstable = true;
                        if (estimateComplexity) {
                            all7x7Unstable = false;
                            all7x7ForThis5x5Unstable = false;
                            count5x5PerBitCount[Integer.bitCount(bitsB)]++;
                            count7x7 += remainingIterations;
//                            // The same calculation, but slower:
//                            for (int bitsC = 0; bitsC < NUMBER_OF_BITS_C; bitsC++) {
//                                if ((bitsC & maskC) == bitsC) {
//                                    count7x7++;
//                                }
//                            }
                        } else {
                            save5x5Array8x8.getBits(0, work8x8_1, 0, array8x8.length());
                            long long5x5 = work8x8_1[0];
                            int pos = 0;
                            LoopBitsC:
                            for (int bitsC : SortedBitsC.SORTED_BITS_C) {
                                if ((bitsC & maskC) != bitsC) {
                                    continue;
                                }
                                count7x7++;
                                remainingIterations--;
                                long long7x7 = setAllBitsC(bitsC, long5x5);

                                if (pos < BUFFER_LENGTH) { // accumulate in buffer
                                    work8xN_1[pos++] = long7x7;
                                }
                                if (pos == BUFFER_LENGTH || remainingIterations == 0) {
                                    array8xN.setBits(0, work8xN_1, 0, 64 * (long) pos);
                                    array8xN.fill(64 * (long) pos, 64 * (long) (BUFFER_LENGTH - pos), false);
                                    checkStability(status, skeleton8xN,
                                        !skeleton3x5 ? MASK_5X5 : analysingLevel >= 6 ? MASK_5X3 : MASK_3X3,
                                        !skeleton3x5 ? MASK_5X5 : analysingLevel >= 6 ? MASK_3X5 : MASK_3X3,
                                        work8xN_1, work8xN_2, thinnedCache8xN);
                                    for (int k = 0; k < pos; k++) {
                                        if (status[k] > 1) {
                                            possible5x5 = true;
                                            possibleBitsB.pushInt(bitsB);
                                            possibleBitsC.pushInt(getAllBitsC(array8xN.subArr(64 * (long) k, 64)));
                                            break LoopBitsC;
                                        }
                                        if (status[k] != 0) {
                                            all7x7ForThis5x5Unstable = false;
                                            all7x7Unstable = false;
                                        }
                                    }
                                    pos = 0;
                                }
                            }
                            // bitsC
                            if (remainingIterations != 0 && !possible5x5)
                                throw new AssertionError("Invalid remainingIterations = " + remainingIterations);
                        }
                        if (possible5x5) {
                            possible3x3 = true;
                            if (!checkAll5x5) {
                                break TestingThis3x3;
                            }
                        } else if (all7x7ForThis5x5Unstable) {
                            if (checkAll5x5) {
                                (skeleton3x5 ? improbableBitsB : impossibleBitsB).pushInt(bitsB);
                            }
                        } else {
                            if (checkAll5x5) {
                                questionableBitsB.pushInt(bitsB);
                            }
                        }
                    } // bitsB
                    if (!possible3x3 && all7x7Unstable) {
                        impossible3x3 = true;
                    }
                    if (estimateComplexity) {
                        printStatus("[3x3c=%d] 5x5 complete: %d 5x5, %d 7x7 (%.1f 7x7 / each 5x5)",
                            bitsA, count5x5, count7x7, (double) count7x7 / (double) count5x5);
                        printStatus("%n");
                        printStatus("    5x5: %s%n", JArrays.toString(count5x5PerBitCount, "+", 1000));
                    }
                }
            } // TestingThis3x3
            synchronized (this) { // for a case of multithreading in future
                if (possible3x3) {
                    allPossible3x3.pushInt(bitsA);
                    add3x3(allPossible3x3Rotated, bitsA, !includeRotated);
                    illustratingB[bitsA] = possibleBitsB;
                    illustratingC[bitsA] = possibleBitsC;
                    questionableB[bitsA] = questionableBitsB;
                    improbableB[bitsA] = improbableBitsB;
                    impossibleB[bitsA] = impossibleBitsB;
                    countB[bitsA] = count5x5;
                } else if (impossible3x3) {
                    allImpossible3x3.pushInt(bitsA);
                    add3x3(allImpossible3x3Rotated, bitsA, !includeRotated);
                } else {
                    allQuestionable3x3.pushInt(bitsA);
                    add3x3(allQuestionable3x3Rotated, bitsA, !includeRotated);
                }
            }
        }
        long t2 = System.nanoTime();
        printStatus("");
        System.out.printf(Locale.US, "Analysis complete in %.3f seconds%n", (t2 - t1) * 1e-9);
        System.out.printf("%d total checked 3x3 configurations%n", count3x3);
    }

    public void writeReport() throws IOException {
        System.out.printf("%n%dx%d analysis:%n", analysingLevel, analysingLevel);
        if (includeRotated) {
            System.out.printf("%d possible 3x3 configurations%n", allPossible3x3.length());
            System.out.printf("%d impossible 3x3 configurations%n", allImpossible3x3.length());
            System.out.printf("%d questionable 3x3 configurations%n", allQuestionable3x3.length());
            assert allPossible3x3.length() == allPossible3x3Rotated.length();
            assert allImpossible3x3.length() == allImpossible3x3Rotated.length();
            assert allQuestionable3x3.length() == allQuestionable3x3Rotated.length();
        } else {
            System.out.printf("%d possible 3x3 configurations, %d with rotated%n",
                allPossible3x3.length(), allPossible3x3Rotated.length());
            System.out.printf("%d impossible 3x3 configurations, %d with rotated%n",
                allImpossible3x3.length(), allImpossible3x3Rotated.length());
            System.out.printf("%d questionable 3x3 configurations, %d with rotated%n",
                allQuestionable3x3.length(), allQuestionable3x3Rotated.length());
        }
        if (!estimateComplexity) {
            FileWriter fw = new FileWriter(filePrefix + "report3x3.txt");
            fw.write(String.format("%d possible 3x3 configurations:\n", allPossible3x3.length()));
            printMatrices3x3(fw, allPossible3x3, null, false);
            fw.write(String.format("\n%d impossible 3x3 configurations:\n", allImpossible3x3.length()));
            printMatrices3x3(fw, allImpossible3x3, null, false);
            fw.write(String.format("\n%d questionable 3x3 configurations:\n", allQuestionable3x3.length()));
            printMatrices3x3(fw, allQuestionable3x3, null, false);
            fw.close();
            fw = new FileWriter(filePrefix + "report3x3_with_rotated.txt");
            fw.write(String.format("%d possible 3x3 configurations:\n", allPossible3x3Rotated.length()));
            printMatrices3x3(fw, allPossible3x3Rotated, allPossible3x3, false);
            fw.write(String.format("\n%d impossible 3x3 configurations:\n", allImpossible3x3Rotated.length()));
            printMatrices3x3(fw, allImpossible3x3Rotated, allImpossible3x3, false);
            fw.write(String.format("\n%d questionable 3x3 configurations:\n", allQuestionable3x3Rotated.length()));
            printMatrices3x3(fw, allQuestionable3x3Rotated, allQuestionable3x3, false);
            fw.close();
            if (all5x5) {
                for (long k = 0, n = allPossible3x3.length(); k < n; k++) {
                    int bitsA = allPossible3x3.getInt(k);
                    fw = new FileWriter(filePrefix + "report5x5_" + bitsA + ".txt");
                    printMatrices3x3(fw, allPossible3x3.subArr(k, 1), null, true);
                    fw.close();
                }
            }
            if (writeConfigurations) {
                printNumbers(filePrefix + "possible3x3.txt", allPossible3x3);
                printNumbers(filePrefix + "impossible3x3.txt", allImpossible3x3);
                printNumbers(filePrefix + "questionable3x3.txt", allQuestionable3x3);
                printNumbers(filePrefix + "possible3x3_with_rotated.txt", allPossible3x3Rotated);
                printNumbers(filePrefix + "impossible3x3_with_rotated.txt", allImpossible3x3Rotated);
                printNumbers(filePrefix + "questionable3x3_with_rotated.txt", allQuestionable3x3Rotated);
            }
        }
        if (includeRotated) {
            checkRotated3x3(allPossible3x3, "Abnormal skeleton " + skeletonType
                + ": non-symmetric possible 3x3");
            checkRotated3x3(allImpossible3x3, "Abnormal skeleton " + skeletonType
                + ": non-symmetric impossible 3x3");
            checkRotated3x3(allQuestionable3x3, "Abnormal skeleton " + skeletonType
                + ": non-symmetric questionable 3x3");
        }
    }

    private void printMatrices3x3(FileWriter fw,
        PIntegerArray bitsAList,
        PIntegerArray importantBitsAList,
        boolean printAll5x5)
        throws IOException
    {
        Matrix<UpdatableBitArray> matrix8x8 = Arrays.SMM.newBitMatrix(8, 8);
        Matrix<UpdatableBitArray> mask8x8 = Arrays.SMM.newBitMatrix(8, 8);
        final UpdatablePIntegerArray sortedBits3x3 = bitsAList.updatableClone(Arrays.SMM);
        Arrays.sort(sortedBits3x3, new ArrayComparator() {
            public boolean less(long firstIndex, long secondIndex) {
                int a = sortedBits3x3.getInt(firstIndex);
                int b = sortedBits3x3.getInt(secondIndex);
                int aCard = Integer.bitCount(a);
                int bCard = Integer.bitCount(b);
                if (aCard != bCard) {
                    return aCard < bCard;
                }
                int aMinimalRotated = minimalRotated(a);
                int bMinimalRotated = minimalRotated(b);
                if (aMinimalRotated != bMinimalRotated) {
                    return aMinimalRotated < bMinimalRotated;
                }
                return a < b;
            }
        });
        UpdatableBitArray array8x8 = matrix8x8.array();
        for (long k = 0, n = sortedBits3x3.length(); k < n; k++) {
            array8x8.fill(false);
            int bitsA = sortedBits3x3.getInt(k);
            String padding = "      ";
            if (importantBitsAList != null
                && importantBitsAList.indexOf(0, importantBitsAList.length(), bitsA) == -1)
            {
                padding = "(rotated)       ";
            }
            setAllBitsAAndCenter1(bitsA, array8x8);
            fw.write(padding + "(" + (k + 1) + "/" + n + ") "
                + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits):\n");
            fw.write(bitMatrixToString(matrix8x8.subMatr(2, 2, 3, 3), padding, false));
            if (printAll5x5 && countB[bitsA] > 0) {
                fw.write("\n" + padding.substring(0, padding.length() - 4) + countB[bitsA]
                    + " tested 5x5 configurations");
            }
            if (illustratingB != null && illustratingC != null
                && illustratingB[bitsA] != null && illustratingC[bitsA] != null
                && illustratingB[bitsA].length() > 0)
            {
                long m = printAll5x5 ? illustratingB[bitsA].length() : 1;
                if (printAll5x5) {
                    // it is possible that illustratingC is empty; it means that we did not check all 7x7
                    fw.write("\n" + padding.substring(0, padding.length() - 4) + m
                        + " possible 5x5 configurations"
                        + (illustratingC[bitsA].length() == 0 ? " with zero 7x7 extension" : "")
                        + ":\n");
                    int stableBitsB = findStableBits(illustratingB[bitsA]);
                    setAllBitsB(~stableBitsB, mask8x8.array());
                    Matrix<? extends BitArray> unknown = mask8x8.subMatr(1, 1, 5, 5);
                    int bitsB = illustratingB[bitsA].getInt(0);
                    setAllBitsB(bitsB, array8x8);
                    Matrix<? extends BitArray> matrix = matrix8x8.subMatr(1, 1, 5, 5);
                    String pad = padding.substring(0, padding.length() - 2);
                    fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                        + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                        + " is possible inside the following series of 5x5 configurations\n");
                    fw.write(bitMatrixToString(matrix, unknown, pad, false));
                }
                for (long i = 0; i < m; i++) {
                    int bitsB = illustratingB[bitsA].getInt(i);
                    int bitsC = i < illustratingC[bitsA].length() ? illustratingC[bitsA].getInt(i) : 0;
                    setAllBitsB(bitsB, array8x8);
                    setAllBitsC(bitsC, array8x8);
                    Matrix<? extends BitArray> matrix = bitsC == 0 ?
                        matrix8x8.subMatr(1, 1, 5, 5) :
                        matrix8x8.subMatr(0, 0, 7, 7);
                    String pad = padding.substring(0, padding.length() - ((int) matrix.dimX() - 3));
                    fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                        + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                        + " is possible inside 0x" + Integer.toHexString(bitsB).toUpperCase()
                        + " (5x5, +" + Integer.bitCount(bitsB) + " unit bits)"
                        + (bitsC == 0 ? "" :
                        " inside 0x" + Integer.toHexString(bitsC).toUpperCase()
                        + " (7x7, +" + Integer.bitCount(bitsC) + " unit bits)")
                        + "\n");
                    fw.write(bitMatrixToString(matrix, pad, false));
                }
            }
            if (printAll5x5 && impossibleB != null && impossibleB[bitsA] != null && impossibleB[bitsA].length() > 0) {
                long m = impossibleB[bitsA].length();
                fw.write("\n" + padding.substring(0, padding.length() - 4) + m + " impossible 5x5 configurations:\n");
                int stableBitsB = findStableBits(impossibleB[bitsA]);
                setAllBitsB(~stableBitsB, mask8x8.array());
                Matrix<? extends BitArray> unknown = mask8x8.subMatr(1, 1, 5, 5);
                int bitsB = impossibleB[bitsA].getInt(0);
                setAllBitsB(bitsB, array8x8);
                Matrix<? extends BitArray> matrix = matrix8x8.subMatr(1, 1, 5, 5);
                String pad = padding.substring(0, padding.length() - 2);
                fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                    + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                    + " inside the following series of impossible 5x5 configurations\n");
                fw.write(bitMatrixToString(matrix, unknown, pad, false));
                for (long i = 0; i < m; i++) {
                    bitsB = impossibleB[bitsA].getInt(i);
                    setAllBitsB(bitsB, array8x8);
                    setAllBitsC(0, array8x8);
                    matrix = matrix8x8.subMatr(1, 1, 5, 5);
                    fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                        + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                        + " inside impossible 0x" + Integer.toHexString(bitsB).toUpperCase()
                        + " (5x5, +" + Integer.bitCount(bitsB) + " unit bits)"
                        + "\n");
                    fw.write(bitMatrixToString(matrix, pad, false));
                }
            }
            if (printAll5x5 && improbableB != null && improbableB[bitsA] != null && improbableB[bitsA].length() > 0) {
                long m = improbableB[bitsA].length();
                fw.write("\n" + padding.substring(0, padding.length() - 4) + m + " improbable 5x5 configurations:\n");
                int stableBitsB = findStableBits(improbableB[bitsA]);
                setAllBitsB(~stableBitsB, mask8x8.array());
                Matrix<? extends BitArray> unknown = mask8x8.subMatr(1, 1, 5, 5);
                int bitsB = improbableB[bitsA].getInt(0);
                setAllBitsB(bitsB, array8x8);
                Matrix<? extends BitArray> matrix = matrix8x8.subMatr(1, 1, 5, 5);
                String pad = padding.substring(0, padding.length() - 2);
                fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                    + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                    + " inside the following series of improbable 5x5 configurations\n");
                fw.write(bitMatrixToString(matrix, unknown, pad, false));
                for (long i = 0; i < m; i++) {
                    bitsB = improbableB[bitsA].getInt(i);
                    setAllBitsB(bitsB, array8x8);
                    setAllBitsC(0, array8x8);
                    matrix = matrix8x8.subMatr(1, 1, 5, 5);
                    fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                        + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                        + " inside improbable 0x" + Integer.toHexString(bitsB).toUpperCase()
                        + " (5x5, +" + Integer.bitCount(bitsB) + " unit bits)"
                        + "\n");
                    fw.write(bitMatrixToString(matrix, pad, false));
                }
            }
            if (printAll5x5 && questionableB != null && questionableB[bitsA] != null
                && questionableB[bitsA].length() > 0)
            {
                long m = questionableB[bitsA].length();
                fw.write("\n" + padding.substring(0, padding.length() - 4) + m
                    + " questionable 5x5 configurations:\n");
                int stableBitsB = findStableBits(questionableB[bitsA]);
                setAllBitsB(~stableBitsB, mask8x8.array());
                Matrix<? extends BitArray> unknown = mask8x8.subMatr(1, 1, 5, 5);
                int bitsB = questionableB[bitsA].getInt(0);
                setAllBitsB(bitsB, array8x8);
                Matrix<? extends BitArray> matrix = matrix8x8.subMatr(1, 1, 5, 5);
                String pad = padding.substring(0, padding.length() - 2);
                fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                    + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                    + " inside the following series of questionable 5x5 configurations\n");
                fw.write(bitMatrixToString(matrix, unknown, pad, false));
                for (long i = 0; i < m; i++) {
                    bitsB = questionableB[bitsA].getInt(i);
                    setAllBitsB(bitsB, array8x8);
                    setAllBitsC(0, array8x8);
                    matrix = matrix8x8.subMatr(1, 1, 5, 5);
                    fw.write("\n" + pad + bitsA + "=0x" + Integer.toHexString(bitsA).toUpperCase()
                        + " (3x3, " + (Integer.bitCount(bitsA) + 1) + " unit bits)"
                        + " inside questionable 0x" + Integer.toHexString(bitsB).toUpperCase()
                        + " (5x5, +" + Integer.bitCount(bitsB) + " unit bits)"
                        + "\n");
                    fw.write(bitMatrixToString(matrix, pad, false));
                }
            }
            fw.write("\n");
        }
    }

    private boolean containsIllegal3x3(BitArray array8x8, BitSet bitsAForConstructingLargeConfigurations) {
        for (int yCenter = -1; yCenter <= 1; yCenter++) {
            for (int xCenter = -1; xCenter <= 1; xCenter++) {
                if (!array8x8.getBit(8 * (3 + yCenter) + 3 + xCenter)) {
                    continue;
                }
                int shiftedBitsA = getAllShiftedBitsA(xCenter, yCenter, array8x8);
                if (!bitsAForConstructingLargeConfigurations.get(shiftedBitsA)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isConnected(BitArray array8x8, ConnectedObjectScanner scanner8x8OfWork) {
        scanner8x8OfWork.matrix().array().copy(array8x8);
        scanner8x8OfWork.clear(null, 3, 3);
        return scanner8x8OfWork.matrix().array().indexOf(0, array8x8.length(), true) == -1;
    }


    static Class<? extends ThinningSkeleton> getSkeletonType(String className) {
        try {
            return Class.forName(ThinningSkeleton.class.getPackage().getName()
                + '.' + className).asSubclass(ThinningSkeleton.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown skeletonization algorithm " + className);
        }
    }

    static ThinningSkeleton getSkeleton(Class<? extends ThinningSkeleton> skeletonType,
        boolean diagonal, boolean topological,
        Matrix<? extends UpdatableBitArray> m)
    {
        if (skeletonType == OctupleThinningSkeleton2D.class) {
            return OctupleThinningSkeleton2D.getInstance(null,  m, diagonal, topological);
        } else if (skeletonType == WeakOctupleThinningSkeleton2D.class) {
            return WeakOctupleThinningSkeleton2D.getInstance(null,  m, true, diagonal, topological);
        } else if (skeletonType == Quadruple3x5ThinningSkeleton2D.class) {
            return Quadruple3x5ThinningSkeleton2D.getInstance(null, m);
        } else if (skeletonType == StrongQuadruple3x5ThinningSkeleton2D.class) {
            return StrongQuadruple3x5ThinningSkeleton2D.getInstance(null, m);
        } else {
            throw new IllegalArgumentException("Unallowed skeleton type " + skeletonType);
        }
    }

    // 0 = unstable, 1 = stable withing mask, >1 = stable without mask
    static void checkStability(byte[] status, ThinningSkeleton skeleton,
        long[] mask04, long[] mask26, long[] work1, long[] work2, BitArray[] thinnedCache)
    {
        assert work1.length == work2.length;
        assert work1.length <= status.length;
        for (int k = 0; k < work1.length; k++) {
            work1[k] = 0;
            work2[k] = 0;
            status[k] = 2; // stable
        }
        Matrix<? extends UpdatableBitArray> m = skeleton.result();
        long len = m.array().length();
        m.array().getBits(0, work1, 0, len);
        for (int directionIndex = 0; directionIndex < 8; directionIndex++) {
            if (!skeleton.isThinningRequired(directionIndex)) {
                continue;
            }
            BitArray thinned = thinnedCache[directionIndex];
            if (thinned == null) {
                thinnedCache[directionIndex] = thinned = skeleton.asThinning(directionIndex).array();
            }
            if (thinnedCache.length >= 16) {
                UpdatableBitArray actual = (UpdatableBitArray)thinnedCache[directionIndex + 8];
                if (actual == null || actual.length() < len) {
                    thinnedCache[directionIndex + 8] = actual = Arrays.SMM.newBitArray(len);
                }
                Arrays.copy(null, actual, thinned); // multithreading
                thinned = actual;
            }
            thinned.getBits(0, work2, 0, len);
            long[] mask = (directionIndex & 2) == 0 ? mask04 : mask26;
            for (int k = 0, j = 0; k < work1.length; k++) {
                if ((work1[k] & mask[j]) != (work2[k] & mask[j])) {
                    status[k] = 0;
                }
                if (work1[k] != work2[k]) {
                    if (status[k] == 2) {
                        status[k] = 1;
                    }
                }
                j++;
                if (j == mask.length) {
                    j = 0;
                }
            }
        }
    }

    static String bitMatrixToString(Matrix<? extends BitArray> m, String padding, boolean systemCR) {
        return bitMatrixToString(m, null, -1, -1, -1, -1, -1, -1, padding, systemCR);
    }

    static String bitMatrixToString(Matrix<? extends BitArray> m, Matrix<? extends BitArray> unknown,
        String padding, boolean systemCR)
    {
        return bitMatrixToString(m, unknown, -1, -1, -1, -1, -1, -1, padding, systemCR);
    }

    static String bitMatrixToString(Matrix<? extends BitArray> m,
        int selectedMinX, int selectedMinY, int selectedMaxX, int selectedMaxY, int centerX, int centerY,
        String padding, boolean systemCR)
    {
        return bitMatrixToString(m, null,
            selectedMinX, selectedMinY, selectedMaxX, selectedMaxY, centerX, centerY, padding, systemCR);
    }

    static String bitMatrixToString(Matrix<? extends BitArray> m, Matrix<? extends BitArray> unknown,
        int selectedMinX, int selectedMinY, int selectedMaxX, int selectedMaxY, int centerX, int centerY,
        String padding, boolean systemCR)
    {
        BitArray a = m.array();
        StringBuilder sb = new StringBuilder();
        for (long y = 0, dimY = m.dimY(), disp = 0; y < dimY; y++) {
            sb.append(padding);
            for (long x = 0, dimX = m.dimX(); x < dimX; x++, disp++) {
                if (unknown != null && unknown.array().getBit(disp)) {
                    sb.append('?');
                } else if (x == centerX && y == centerY) {
                    sb.append(a.getBit(disp) ? 'C' : '-');
                } else if (x >= selectedMinX && x <= selectedMaxX && y >= selectedMinY && y <= selectedMaxY) {
                    sb.append(a.getBit(disp) ? '#' : ',');
                } else {
                    sb.append(a.getBit(disp) ? '*' : '.');
                }
                if (x < dimX) {
                    sb.append(' ');
                }
            }
            sb.append(systemCR ? String.format("%n"): "\n");
        }
        return sb.toString();
    }

    static synchronized void printStatus(String format, Object... arguments) {
        String s = String.format(Locale.US, format, arguments);
        if (s.length() > 78) {
            s = "..." + s.substring(s.length() - 75);
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\r').append(s);
        for (int k = s.length(); k < 78; k++) {
            sb.append(' ');
        }
        sb.append('\r');
        System.err.print(sb);
    }

    static int findStableBits(IntArray a) {
        int max = 0;
        int min = 0xFFFFFFFF;
        for (long k = 0, n = a.length(); k < n; k++) {
            int v = a.getInt(k);
            max |= v;
            min &= v;
        }
        return min | ~max;
    }

    // C C C C C C C D
    // C B B B B B C D
    // C B A A A B C D
    // C B A 1 A B C D
    // C B A A A B C D
    // C B B B B B C D
    // C C C C C C C D
    // D D D D D D D D
    static void setAllBitsAAndCenter1(int bitsA, UpdatableBitArray array8x8) { // 0 <= bitsA < 2^8
        array8x8.setBit(24 + 3, true);
        for (int bit = 0; bit < 3; bit++) {
            array8x8.setBit(16 + 2 + bit, (bitsA & (1 << bit)) != 0);
        }
        array8x8.setBit(24 + 4, (bitsA & (1 << 3)) != 0);
        for (int bit = 0; bit < 3; bit++) {
            array8x8.setBit(32 + 4 - bit, (bitsA & (1 << (4 + bit))) != 0);
        }
        array8x8.setBit(24 + 2, (bitsA & (1 << 7)) != 0);
    }

    static int getAllShiftedBitsA(int dx, int dy, BitArray array8x8) {
        assert -2 <= dx && dx <= 2 && -2 <= dy && dy <= 2;
        int bitsA = 0;
        for (int bit = 0; bit < 3; bit++) {
            if (array8x8.getBit(8 * (2 + dy) + 2 + dx + bit)) {
                bitsA |= 1 << bit;
            }
            if (array8x8.getBit(8 * (4 + dy) + 4 + dx - bit)) {
                bitsA |= 1 << (4 + bit);
            }
        }
        if (array8x8.getBit(8 * (3 + dy) + 4 + dx)) {
            bitsA |= 1 << 3;
        }
        if (array8x8.getBit(8 * (3 + dy) + 2 + dx)) {
            bitsA |= 1 << 7;
        }
        return bitsA;
    }

    // . . . . . . . .
    // . 0 1 2 3 4 . .
    // .15 A A A 5 . .
    // .14 A 1 A 6 . .
    // .13 A A A 7 . .
    // .12 11109 8 . .
    // . . . . . . . .
    // . . . . . . . .
    static void setAllBitsB(int bitsB, UpdatableBitArray array8x8) { // 0 <= bitsB < 2^16
        for (int bit = 0; bit < 5; bit++) {
            array8x8.setBit(8 + 1 + bit, (bitsB & (1 << bit)) != 0);
            array8x8.setBit(40 + 5 - bit, (bitsB & (1 << (8 + bit))) != 0);
        }
        for (int bit = 0; bit < 3; bit++) {
            array8x8.setBit(8 * (2 + bit) + 5, (bitsB & (1 << (5 + bit))) != 0);
            array8x8.setBit(8 * (4 - bit) + 1, (bitsB & (1 << (13 + bit))) != 0);
        }
    }

    static long setAllBitsB(int bitsB, long array8x8) { // 0 <= bitsB < 2^16
        long result = 0;
        for (int bit = 0; bit < 5; bit++) {
            if ((bitsB & (1 << bit)) != 0) {
                result |= 1L << (8 + 1 + bit);
            }
            if ((bitsB & (1 << (8 + bit))) != 0) {
                result |= 1L << (40 + 5 - bit);
            }
        }
        for (int bit = 0; bit < 3; bit++) {
            if ((bitsB & (1 << (5 + bit))) != 0) {
                result |= 1L << (8 * (2 + bit) + 5);
            }
            if ((bitsB & (1 << (13 + bit))) != 0) {
                result |= 1L << (8 * (4 - bit) + 1);
            }
        }
        return result | array8x8;
    }

    static int getAllBitsB(BitArray array8x8) {
        int bitsB = 0;
        for (int bit = 0; bit < 5; bit++) {
            if (array8x8.getBit(8 + 1 + bit)) {
                bitsB |= 1 << bit;
            }
            if (array8x8.getBit(40 + 5 - bit)) {
                bitsB |= 1 << (8 + bit);
            }
        }
        for (int bit = 0; bit < 3; bit++) {
            if (array8x8.getBit(8 * (2 + bit) + 5)) {
                bitsB |= 1 << (5 + bit);
            }
            if (array8x8.getBit(8 * (4 - bit) + 1)) {
                bitsB |= 1 << (13 + bit);
            }
        }
        return bitsB;
    }

    // 0 1 2 3 4 5 6 .
    //23 B B B B B 7 .
    //22 B A A A B 8 .
    //21 B A 1 A B 9 .
    //20 B A A A B 10.
    //19 B B B B B 11.
    //18 171615141312.
    // . . . . . . . .
    static void setAllBitsC(int bitsC, UpdatableBitArray array8x8) { // 0 <= bitsC < 2^24
        for (int bit = 0; bit < 7; bit++) {
            array8x8.setBit(bit, (bitsC & (1 << bit)) != 0);
            array8x8.setBit(48 + 6 - bit, (bitsC & (1 << (12 + bit))) != 0);
        }
        for (int bit = 0; bit < 5; bit++) {
            array8x8.setBit(8 * (1 + bit) + 6, (bitsC & (1 << (7 + bit))) != 0);
            array8x8.setBit(8 * (5 - bit), (bitsC & (1 << (19 + bit))) != 0);
        }
    }

    static long setAllBitsC(int bitsC, long array8x8) { // 0 <= bitsC < 2^24
        long result = (long)(bitsC & 0x7F);
        for (int bit = 0; bit < 7; bit++) {
            if ((bitsC & (1 << (12 + bit))) != 0) {
                result |= 1L << (48 + 6 - bit);
            }
        }
        for (int bit = 0; bit < 5; bit++) {
            if ((bitsC & (1 << (7 + bit))) != 0) {
                result |= 1L << (8 * (1 + bit) + 6);
            }
            if ((bitsC & (1 << (19 + bit))) != 0) {
                result |= 1L << (8 * (5 - bit));
            }
        }
        return result | array8x8;
    }

    static int getAllBitsC(BitArray array8x8) {
        int bitsC = 0;
        for (int bit = 0; bit < 7; bit++) {
            if (array8x8.getBit(bit)) {
                bitsC |= 1 << bit;
            }
            if (array8x8.getBit(48 + 6 - bit)) {
                bitsC |= 1 << (12 + bit);
            }
        }
        for (int bit = 0; bit < 5; bit++) {
            if (array8x8.getBit(8 * (1 + bit) + 6)) {
                bitsC |= 1 << (7 + bit);
            }
            if (array8x8.getBit(8 * (5 - bit))) {
                bitsC |= 1 << (19 + bit);
            }
        }
        return bitsC;
    }

    // "Dilation" of bits X
    static int extendUnitBitsX(int bitsC) {
        int result = bitsC;
        for (int bit = 1; bit <= 5; bit++) {
            if ((bit > 1 && (bitsC & (1 << (bit - 1))) != 0) || (bit < 5 && (bitsC & (1 << (bit + 1))) != 0)) {
                result |= 1 << bit;
            }
        }
        for (int bit = 7; bit <= 11; bit++) {
            if ((bit > 7 && (bitsC & (1 << (bit - 1))) != 0) || (bit < 11 && (bitsC & (1 << (bit + 1))) != 0)) {
                result |= 1 << bit;
            }
        }
        for (int bit = 13; bit <= 17; bit++) {
            if ((bit > 13 && (bitsC & (1 << (bit - 1))) != 0) || (bit < 17 && (bitsC & (1 << (bit + 1))) != 0)) {
                result |= 1 << bit;
            }
        }
        for (int bit = 19; bit <= 23; bit++) {
            if ((bit > 19 && (bitsC & (1 << (bit - 1))) != 0) || (bit < 23 && (bitsC & (1 << (bit + 1))) != 0)) {
                result |= 1 << bit;
            }
        }
        return result;
    }

    // . . . . . . . D        . . . . . . . D
    // . . . . . . . D        . . . . . . . D
    // . . , , , b . D        . . , , , . . D
    // . . , 1 , b b D        . b , 1 , b . D
    // . . , , A b b D        . b , A , b . D
    // . . b b b b b D        . b b b b b . D
    // . . . b b b . D        . . b b b . . D
    // D D D D D D D D        D D D D D D D D
    // dy=1, dx=1: 13         dy=1, dx=0: 12
    static void setAllShiftedBits3x5(int bits3x5, int dy, int dx, UpdatableBitArray array8x8) {
        // 0 <= bits3x5 < 2^12/2^13
        assert -1 <= dx && dx <= 1 && -1 <= dy && dy <= 1;
        int bit = 0;
        for (int y = dy - 2; y <= dy + 2; y++) {
            for (int x = dx - 2; x <= dx + 2; x++) {
                if (Math.abs(y) <= 1 && Math.abs(x) <= 1) {
                    continue;
                }
                if (Math.abs(y - dy) == 2 && Math.abs(x - dx) == 2) {
                    continue;
                }
                array8x8.setBit(8 * (3 + y) + 3 + x, (bits3x5 & (1 << bit)) != 0);
                bit++;
            }
        }
        assert bit == (Math.abs(dx) + Math.abs(dy) == 2 ? 13 : 12);
    }

    static void add3x3(MutableIntArray result, int bitsA, boolean addRotated) {
        // MutablePIntegerArray - AA-198 required
        if (result.indexOf(0, result.length(), bitsA) == -1) {
            result.pushInt(bitsA);
        }
        if (!addRotated) {
            return;
        }
        // These symmetries correspond to the logic of Quadruple3x5ThinningSkeleton2D and VerifySkeletonSymmetry:
        int rotated90 = rotated90BitsA(bitsA);
        if (result.indexOf(0, result.length(), rotated90) == -1) {
            result.pushInt(rotated90);
        }
        int rotated180 = rotated180BitsA(bitsA);
        if (result.indexOf(0, result.length(), rotated180) == -1) {
            result.pushInt(rotated180);
        }
        int rotated270 = rotated270BitsA(bitsA);
        if (result.indexOf(0, result.length(), rotated270) == -1) {
            result.pushInt(rotated270);
        }
    }

    static int minimalRotated(int bitsA) {
        int rotated90 = rotated90BitsA(bitsA);
        int rotated180 = rotated180BitsA(bitsA);
        int rotated270 = rotated270BitsA(bitsA);
        return Math.min(Math.min(bitsA, rotated90), Math.min(rotated180, rotated270));
    }

    static void checkRotated3x3(PIntegerArray configurations3x3, String msg) {
        for (long k = 0, n = configurations3x3.length(); k < n; k++) {
            int bitsA = configurations3x3.getInt(k);
            int rotated90 = rotated90BitsA(bitsA);
            if (configurations3x3.indexOf(0, configurations3x3.length(), rotated90) == -1)
                throw new AssertionError(msg + ": "
                    + bitsA + " does exist, but rotated-90 " + rotated90 + " does not");
            int rotated180 = rotated180BitsA(bitsA);
            if (configurations3x3.indexOf(0, configurations3x3.length(), rotated180) == -1)
                throw new AssertionError(msg + ": "
                    + bitsA + " does exist, but rotated-180 " + rotated180 + " does not");
            int rotated270 = rotated270BitsA(bitsA);
            if (configurations3x3.indexOf(0, configurations3x3.length(), rotated270) == -1)
                throw new AssertionError(msg + ": "
                    + bitsA + " does exist, but rotated-270 " + rotated270 + " does not");
        }
    }

    // A A A    0 1 2
    // A * A    7 * 3
    // A A A    6 5 4
    private static int rotated90BitsA(int bitsA) {
        assert 0 <= bitsA && bitsA < 256;
        int bit0 = bitsA & 1;
        int bit1 = (bitsA >> 1) & 1;
        int bit2 = (bitsA >> 2) & 1;
        int bit3 = (bitsA >> 3) & 1;
        int bit4 = (bitsA >> 4) & 1;
        int bit5 = (bitsA >> 5) & 1;
        int bit6 = (bitsA >> 6) & 1;
        int bit7 = (bitsA >> 7) & 1;
        return bit6 |
            (bit7 << 1) |
            (bit0 << 2) |
            (bit1 << 3) |
            (bit2 << 4) |
            (bit3 << 5) |
            (bit4 << 6) |
            (bit5 << 7);
    }

    private static int rotated180BitsA(int bitsA) {
        assert 0 <= bitsA && bitsA < 256;
        int bit0 = bitsA & 1;
        int bit1 = (bitsA >> 1) & 1;
        int bit2 = (bitsA >> 2) & 1;
        int bit3 = (bitsA >> 3) & 1;
        int bit4 = (bitsA >> 4) & 1;
        int bit5 = (bitsA >> 5) & 1;
        int bit6 = (bitsA >> 6) & 1;
        int bit7 = (bitsA >> 7) & 1;
        return bit4 |
            (bit5 << 1) |
            (bit6 << 2) |
            (bit7 << 3) |
            (bit0 << 4) |
            (bit1 << 5) |
            (bit2 << 6) |
            (bit3 << 7);
    }

    private static int rotated270BitsA(int bitsA) {
        assert 0 <= bitsA && bitsA < 256;
        int bit0 = bitsA & 1;
        int bit1 = (bitsA >> 1) & 1;
        int bit2 = (bitsA >> 2) & 1;
        int bit3 = (bitsA >> 3) & 1;
        int bit4 = (bitsA >> 4) & 1;
        int bit5 = (bitsA >> 5) & 1;
        int bit6 = (bitsA >> 6) & 1;
        int bit7 = (bitsA >> 7) & 1;
        return bit2 |
            (bit3 << 1) |
            (bit4 << 2) |
            (bit5 << 3) |
            (bit6 << 4) |
            (bit7 << 5) |
            (bit0 << 6) |
            (bit1 << 7);
    }

    private static void printNumbers(String fileName, PIntegerArray numbers) throws IOException {
        FileWriter fw = new FileWriter(fileName);
        for (long k = 0, n = numbers.length(); k < n; k++) {
            fw.write(numbers.getInt(k) + "\n");
        }
        fw.close();
    }

    private static BitSet readNumbersSet(String fileName) throws IOException {
        BitSet result = new BitSet();
        try {
            BufferedReader fr = new BufferedReader(new FileReader(fileName));
            String s;
            while ((s = fr.readLine()) != null) {
                if (s.trim().length() == 0) {
                    continue;
                }
                result.set(Integer.parseInt(s));
            }
            fr.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IllegalArgumentException ex = new IllegalArgumentException("Illegal syntax of "
                + fileName + ": " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
        return result;
    }

    private static int[] allIntegersSortedByNumberOfBits(int n) {
        int[] result = new int[n];
        for (int k = 0; k < n; k++) {
            result[k] = k;
        }
//        final int capacity = 31 - Integer.numberOfLeadingZeros(n);
        ArraySorter.getQuickSorter().sortIndexes(result, 0, n, new ArrayComparator() {
            public boolean less(long firstIndex, long secondIndex) {
                int a = (int) firstIndex;
                int b = (int) secondIndex;
                int aBitsCount = Integer.bitCount(a);
                int bBitCount = Integer.bitCount(b);
                if (aBitsCount != bBitCount) {
                    return aBitsCount < bBitCount;
                }
//                int aShifted = a >>> 1 | (a & 1) << (capacity - 1);
//                int bShifted = b >>> 1 | (b & 1) << (capacity - 1);
//                int aNeighbourBitsCount = Integer.bitCount(a & aShifted);
//                int bNeighbourBitsCount = Integer.bitCount(b & bShifted);
//                if (aNeighbourBitsCount != bNeighbourBitsCount) {
//                    return aNeighbourBitsCount < bNeighbourBitsCount;
//                }
                return a < b;
            }
        });
//        System.out.println(JArrays.toString(result, ", ", 10000));
        return result;
    }

    public static void main(String[] args) throws IOException {
        AnalyseSkeletonConfigurations analyser;
        try {
            analyser = new AnalyseSkeletonConfigurations(args);
        } catch (UsageException e) {
            return;
        }
        if (analyser.all5x5) {
            analyser.analyseConfigurations(true, false);
            analyser.analyseConfigurations(false, true);
        } else {
            analyser.analyseConfigurations(false, false);
        }
        analyser.writeReport();
    }

    static class SortedBitsB {
        static int[] SORTED_BITS_B = allIntegersSortedByNumberOfBits(NUMBER_OF_BITS_B);
    }

    static class SortedBitsC {
        static int[] SORTED_BITS_C = allIntegersSortedByNumberOfBits(NUMBER_OF_BITS_C);
    }
}
