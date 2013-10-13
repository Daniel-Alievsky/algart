package net.algart.arrays.demo;

import java.util.Random;
import net.algart.arrays.*;

/**
 * <p>Test for {@link Arrays#compactCyclicPositions} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class CompactCyclicPositionsTest {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: " + CompactCyclicPositionsTest.class.getName()
                + " length numberOfPositions numberOfTests [randSeed]");
            return;
        }
        int length = Integer.parseInt(args[0]);
        int numberOfPositions = Integer.parseInt(args[1]);
        int numberOfTests = Integer.parseInt(args[2]);
        long seed;
        if (args.length < 4) {
            seed = new Random().nextLong();
        } else {
            seed = Long.parseLong(args[3]);
        }
        long[] positions = new long[numberOfPositions];
        long[] positionsCopy = new long[numberOfPositions];
        Random rnd = new Random(seed);

        System.out.println("Testing " + numberOfPositions + " positions in the 0.."
            + (length - 1) + " ring " + numberOfTests
            + " times with start random seed " + seed);

        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            boolean compact = rnd.nextBoolean();
            boolean simple = rnd.nextBoolean();
            int shift = rnd.nextInt(length);
            for (int k = 0; k < positions.length; k++) {
                int v = simple ? Math.min(k, length - 1) :
                    compact ? rnd.nextInt(Math.max(length / 4, 1)) :
                    rnd.nextInt(length);
                v -= shift;
                if (v < 0)
                    v += length;
                positions[k] = v;
            }
            long sum1 = Arrays.preciseSumOf(SimpleMemoryModel.asUpdatableLongArray(positions));
            System.arraycopy(positions, 0, positionsCopy, 0, positions.length);
            long p = Arrays.compactCyclicPositions(length, positions);
            if (p < 0)
                throw new AssertionError("The bug found (negative result) in test #"
                    + testCount + ": "
                    + "simple=" + simple + ", compact=" + compact + ", shift=" + shift + ", result=" + p
                    + ", positions: " + JArrays.toString(positionsCopy, " ", 20) + " before, "
                    + JArrays.toString(positions, " ", 20) + " after");
            if (positions.length > 0 && positions[0] != 0)
                throw new AssertionError("The bug found (non-zero positions[0]) in test #"
                    + testCount + ": "
                    + "simple=" + simple + ", compact=" + compact + ", shift=" + shift + ", result=" + p
                    + ", positions: " + JArrays.toString(positionsCopy, " ", 20) + " before, "
                    + JArrays.toString(positions, " ", 20) + " after");
            for (int k = 1; k < positions.length; k++)
                if (positions[k] < positions[k - 1])
                    throw new AssertionError("The bug found (result array is not sorted) in test #"
                        + testCount + ": "
                        + "simple=" + simple + ", compact=" + compact + ", shift=" + shift + ", result=" + p
                        + ", positions: " + JArrays.toString(positionsCopy, " ", 20) + " before, "
                        + JArrays.toString(positions, " ", 20) + " after");
            long sum2 = 0;
            for (int k = 0; k < positions.length; k++) {
                sum2 += (positions[k] + p) % length;
            }
            if (sum1 != sum2)
                throw new AssertionError("The bug found (the sum of array elements is "
                    + sum2 + " instead of " + sum1 + ") in test #"
                    + testCount + ": "
                    + "simple=" + simple + ", compact=" + compact + ", shift=" + shift + ", result=" + p
                    + ", positions: " + JArrays.toString(positionsCopy, " ", 20) + " before, "
                    + JArrays.toString(positions, " ", 20) + " after");
            DemoUtils.showProgress(testCount, numberOfTests);
        }
        System.out.println("All O'k");
    }
}
