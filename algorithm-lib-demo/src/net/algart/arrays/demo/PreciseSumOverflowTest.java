package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.util.Locale;

/**
 * <p>Test for overflow in {@link Arrays#preciseSumOf(PFixedArray, boolean)} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class PreciseSumOverflowTest {
    static void test(PArray array) {
        if (array instanceof PFixedArray) {
            boolean overflow = false;
            long pSum = -1;
            try {
                pSum = Arrays.preciseSumOf((PFixedArray)array, true);
            } catch (ArithmeticException ex) {
                overflow = true;
            }
            double sum = Arrays.sumOf(array);
            System.out.printf(Locale.US, Arrays.toString(array, ", ", 10000) + ": %n    "
                + (overflow ? "OVERFLOW" : "sum = " + pSum) + ", ~= %.3f%n", sum);
        } else {
            double sum = Arrays.sumOf(array);
            System.out.printf(Locale.US, Arrays.toString(array, ", ", 10000) + ": %n    sum ~= %.3f%n", sum);
        }
    }

    public static void main(String[] args) {
        Object[] arrays = new Object[] {
            new byte[] {-100, -100},
            new int[] {Integer.MAX_VALUE, 1},
            new long[] {Long.MAX_VALUE, -100, 99},
            new long[] {Long.MAX_VALUE, 100, 99},
            new long[] {Long.MIN_VALUE, 100, Long.MIN_VALUE},
            new long[] {Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE},
            new long[] {Long.MIN_VALUE + 1, Long.MAX_VALUE, Long.MIN_VALUE},
            new double[] {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE},
        };
        for (int k = 0; k < arrays.length; k++)
            test((PArray)SimpleMemoryModel.asUpdatableArray(arrays[k]));
    }
}
