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

package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * <p>Basic test for {@link net.algart.arrays.ArraySelector} class</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 */
public class ArraySelectorTest {
    private static final boolean MEASURE_RANDOM_PERCENTILES = false;
    private static final ByteArraySelector BYTE_ARRAY_SELECTOR = new ByteArraySelector();

    private static void measureSpeed(final float[] array, final double[] levels) {
        final ArraySelector selector = ArraySelector.getQuickSelector();
        final ArrayComparator comparator = new JArrays.FloatArrayComparator(array);
        final ArrayExchanger exchanger = new JArrays.FloatArrayExchanger(array);

        float[] clone1 = array.clone();
        float[] clone2 = array.clone();
        String comment = "";
        if (levels[0] == 0.0) {
            comment = " MIN";
        }
        if (levels[levels.length - 1] == 1.0) {
            comment += " MAX";
        }
        final int n = array.length >= 1000 ? 5 : array.length >= 100 ? 50 : 1000;

        long t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            selector.select(levels, array, array.length);
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Selecting %d from %d float elements (for float[]): %.6f ms, %.3f ns/element%s%n",
                levels.length, array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);

        System.arraycopy(clone2, 0, array, 0, array.length);
        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            selector.select(array.length, levels, comparator, exchanger);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Selecting %d from %d float elements (universal): %.6f ms, %.3f ns/element%s%n",
                levels.length, array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);

        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            Arrays.sort(clone1);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Full sorting %d float elements (java.util): %.6f ms, %.3f ns/element%s%n",
                array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);
        check(array, clone1, levels, comparator, "double levels, optimized for float[]");

        System.arraycopy(clone2, 0, array, 0, array.length);
        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            ArraySorter.getQuickSorter().sort(0, array.length, comparator, exchanger);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Full sorting %d float elements (AlgART): %.6f ms, %.3f ns/element%s%n",
                array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);
    }

    private static void measureSpeed(final byte[] array, final double[] levels) {
        final ArraySelector selector = ArraySelector.getQuickSelector();
        final ArrayComparator32 comparator = (first, second) -> (array[first] & 0xFF) < (array[second] & 0xFF);
        final ArrayExchanger exchanger = new JArrays.ByteArrayExchanger(array);

        byte[] clone1 = array.clone();
        byte[] clone2 = array.clone();
        String comment = "";
        if (levels[0] == 0.0) {
            comment = " MIN";
        }
        if (levels[levels.length - 1] == 1.0) {
            comment += " MAX";
        }
        final int n = array.length >= 1000 ? 5 : array.length >= 100 ? 50 : 1000;

        long t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            selector.select(levels, array, array.length);
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Selecting %d from %d byte elements (for byte[]): %.6f ms, %.3f ns/element%s%n",
                levels.length, array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);

        System.arraycopy(clone2, 0, array, 0, array.length);
        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            selector.select(array.length, levels, comparator, exchanger);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Selecting %d from %d byte elements (universal): %.6f ms, %.3f ns/element%s%n",
                levels.length, array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);

        System.arraycopy(clone2, 0, array, 0, array.length);
        byte[] results = new byte[levels.length];
        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            BYTE_ARRAY_SELECTOR.select(results, levels, array, array.length);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Selecting %d from %d byte elements (histogram-based): %.6f ms, %.3f ns/element%s%n",
                levels.length, array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);

        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            Arrays.sort(clone1);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Full sorting %d byte elements (java.util): %.6f ms, %.3f ns/element%s%n",
                array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);

        System.arraycopy(clone2, 0, array, 0, array.length);
        t1 = System.nanoTime();
        for (int k = 0; k < n; k++) {
            ArraySorter.getQuickSorter().sort(0, array.length, comparator, exchanger);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US,
                "      Full sorting %d byte elements (AlgART): %.6f ms, %.3f ns/element%s%n",
                array.length,
                (t2 - t1) * 1e-6 / n, (t2 - t1) * 1.0 / array.length / n, comment);
    }


    /*Repeat() float ==> double;; Float ==> Double
    */
    private static void testCorrectness(final float[] array, final double[] levels, Random rnd) {
        final ArraySelector selector = ArraySelector.getQuickSelector();
        final ArrayComparator comparator = new JArrays.FloatArrayComparator(array);
        final ArrayExchanger exchanger = new JArrays.FloatArrayExchanger(array);

        float[] clone = array.clone();
        int[] intIndexes = new int[levels.length];
        long[] longIndexes = new long[levels.length];
        for (int k = 0; k < levels.length; k++) {
            longIndexes[k] = ArraySelector.percentileIndex(levels[k], (long) array.length);
            intIndexes[k] = ArraySelector.percentileIndex(levels[k], array.length);
        }
        String branch;
        if (rnd.nextBoolean()) {
            branch = "double levels, common algorithm";
            selector.select(array.length, levels, comparator, exchanger);
        } else if (rnd.nextBoolean()) {
            branch = "double levels, optimized for float[]";
            selector.select(levels, array, array.length);
        } else if (rnd.nextBoolean()) {
            branch = "integer indexes, common algorithm";
            selector.select(array.length, longIndexes, comparator, exchanger);
        } else {
            branch = "integer indexes, optimized for float[]";
            selector.select(intIndexes, array, array.length);
        }
        Arrays.sort(clone);
        check(array, clone, levels, comparator, branch);
        int levelIndex = rnd.nextInt(levels.length);
        int index = ArraySelector.percentileIndex(levels[levelIndex], array.length);
        final float percentile = selector.select(0, array.length, index, array);
        if (!equalsWithNaN(percentile, array[index])) {
            throw new AssertionError("Illegal result of select method: " + percentile + " (position "
                    + index + ", " + branch
                    + "): array " + JArrays.toString(array, ", ", 1000)
                    + ", sorted " + JArrays.toString(clone, ", ", 1000));
        }
    }

    private static void check(
            float[] array,
            float[] sortedArray,
            double[] levels,
            ArrayComparator comparator,
            String bramchName) {
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], array.length);
            float selected = array[percentileIndex];
            float sorted = sortedArray[percentileIndex];
            if (!equalsWithNaN(selected, sorted)) {
                throw new AssertionError("Illegal selected = " + selected + " instead of "
                        + sorted + " (position "
                        + percentileIndex + ", " + bramchName
                        + "):\n  array " + JArrays.toString(array, ", ", 1000)
                        + "\n  sorted " + JArrays.toString(sortedArray, ", ", 1000));
            }
            for (int i = 0; i < percentileIndex; i++) {
                if (comparator.less(percentileIndex, i)) {
                    throw new AssertionError("array[" + percentileIndex + "] < array[" + i + "]");
                }
            }
            for (int i = percentileIndex + 1; i < array.length; i++) {
                if (comparator.less(i, percentileIndex)) {
                    throw new AssertionError("array[" + i + "] < array[" + percentileIndex + "]");
                }
            }
        }
    }

    private static boolean equalsWithNaN(float a, float b) {
        if (Float.isNaN(a) == Float.isNaN(b)) {
            return Float.isNaN(a) || a == b;
        } else {
            return false;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static void testCorrectness(final double[] array, final double[] levels, Random rnd) {
        final ArraySelector selector = ArraySelector.getQuickSelector();
        final ArrayComparator comparator = new JArrays.DoubleArrayComparator(array);
        final ArrayExchanger exchanger = new JArrays.DoubleArrayExchanger(array);

        double[] clone = array.clone();
        int[] intIndexes = new int[levels.length];
        long[] longIndexes = new long[levels.length];
        for (int k = 0; k < levels.length; k++) {
            longIndexes[k] = ArraySelector.percentileIndex(levels[k], (long) array.length);
            intIndexes[k] = ArraySelector.percentileIndex(levels[k], array.length);
        }
        String branch;
        if (rnd.nextBoolean()) {
            branch = "double levels, common algorithm";
            selector.select(array.length, levels, comparator, exchanger);
        } else if (rnd.nextBoolean()) {
            branch = "double levels, optimized for double[]";
            selector.select(levels, array, array.length);
        } else if (rnd.nextBoolean()) {
            branch = "integer indexes, common algorithm";
            selector.select(array.length, longIndexes, comparator, exchanger);
        } else {
            branch = "integer indexes, optimized for double[]";
            selector.select(intIndexes, array, array.length);
        }
        Arrays.sort(clone);
        check(array, clone, levels, comparator, branch);
        int levelIndex = rnd.nextInt(levels.length);
        int index = ArraySelector.percentileIndex(levels[levelIndex], array.length);
        final double percentile = selector.select(0, array.length, index, array);
        if (!equalsWithNaN(percentile, array[index])) {
            throw new AssertionError("Illegal result of select method: " + percentile + " (position "
                    + index + ", " + branch
                    + "): array " + JArrays.toString(array, ", ", 1000)
                    + ", sorted " + JArrays.toString(clone, ", ", 1000));
        }
    }

    private static void check(
            double[] array,
            double[] sortedArray,
            double[] levels,
            ArrayComparator comparator,
            String bramchName) {
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], array.length);
            double selected = array[percentileIndex];
            double sorted = sortedArray[percentileIndex];
            if (!equalsWithNaN(selected, sorted)) {
                throw new AssertionError("Illegal selected = " + selected + " instead of "
                        + sorted + " (position "
                        + percentileIndex + ", " + bramchName
                        + "):\n  array " + JArrays.toString(array, ", ", 1000)
                        + "\n  sorted " + JArrays.toString(sortedArray, ", ", 1000));
            }
            for (int i = 0; i < percentileIndex; i++) {
                if (comparator.less(percentileIndex, i)) {
                    throw new AssertionError("array[" + percentileIndex + "] < array[" + i + "]");
                }
            }
            for (int i = percentileIndex + 1; i < array.length; i++) {
                if (comparator.less(i, percentileIndex)) {
                    throw new AssertionError("array[" + i + "] < array[" + percentileIndex + "]");
                }
            }
        }
    }

    private static boolean equalsWithNaN(double a, double b) {
        if (Double.isNaN(a) == Double.isNaN(b)) {
            return Double.isNaN(a) || a == b;
        } else {
            return false;
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    private static void testCorrectness(final byte[] array, final double[] levels, Random rnd) {
        final ArraySelector selector = ArraySelector.getQuickSelector();
        final ArrayComparator comparator = new JArrays.ByteArrayComparator(array);
        final ArrayExchanger exchanger = new JArrays.ByteArrayExchanger(array);

        final byte[] clone = array.clone();
        int[] intIndexes = new int[levels.length];
        long[] longIndexes = new long[levels.length];
        for (int k = 0; k < levels.length; k++) {
            longIndexes[k] = ArraySelector.percentileIndex(levels[k], (long) array.length);
            intIndexes[k] = ArraySelector.percentileIndex(levels[k], array.length);
        }
        if (rnd.nextBoolean()) {
            selector.select(array.length, levels, comparator, exchanger);
        } else if (rnd.nextBoolean()) {
            selector.select(levels, array, array.length);
        } else if (rnd.nextBoolean()) {
            selector.select(array.length, longIndexes, comparator, exchanger);
        } else {
            selector.select(intIndexes, array, array.length);
        }
        byte[] percentiles = new byte[levels.length];
        if (rnd.nextBoolean()) {
            BYTE_ARRAY_SELECTOR.select(percentiles, levels, clone, clone.length);
        } else {
            BYTE_ARRAY_SELECTOR.select(percentiles, intIndexes, clone, clone.length);
        }
        ArraySorter.getQuickSorter().sort(0, clone.length,
                new JArrays.ByteArrayComparator(clone),
                new JArrays.ByteArrayExchanger(clone));
        check(array, clone, levels, comparator, percentiles);
    }

    private static void check(
            byte[] array,
            byte[] sortedArray,
            double[] levels,
            ArrayComparator comparator,
            byte[] percentiles) {
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], array.length);
            byte selected = array[percentileIndex];
            byte sorted = sortedArray[percentileIndex];
            if (selected != sorted) {
                throw new AssertionError("Illegal selected = " + selected + " (position "
                        + percentileIndex + "): array "
                        + JArrays.toHexString(array, ", ", 1000));
            }
            for (int i = 0; i < percentileIndex; i++) {
                if (comparator.less(percentileIndex, i)) {
                    throw new AssertionError("array[" + percentileIndex + "] < array[" + i + "]");
                }
            }
            for (int i = percentileIndex + 1; i < array.length; i++) {
                if (comparator.less(i, percentileIndex)) {
                    throw new AssertionError("array[" + i + "] < array[" + percentileIndex + "]");
                }
            }
            if (selected != percentiles[k]) {
                throw new AssertionError("Bug in ByteArraySelector (position "
                        + percentileIndex + "): array "
                        + JArrays.toHexString(array, ", ", 1000)
                        + ", found percentiles "
                        + JArrays.toHexString(percentiles, ", ", 1000));
            }
        }
    }

    public static void main(String[] args) {
        boolean skipMeasuring = false;
        int startArgIndex = 0;
        if (args.length > 0 && args[startArgIndex].equalsIgnoreCase("-skipMeasuring")) {
            skipMeasuring = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.printf("Usage: [-skipMeasuring] %s numberOfElements numberOfTests%n", ArraySelectorTest.class);
            return;
        }
        final int numberOfElements = Integer.parseInt(args[startArgIndex]);
        final int numberOfTests = Integer.parseInt(args[startArgIndex + 1]);
        final int numberOfMeasuringTest = skipMeasuring ? 0 : numberOfElements <= 250 ? 300 : 32;
        Random rnd = new Random(0);
        for (int test = 1; test <= numberOfMeasuringTest; test++) {
            System.out.printf("%nSpeed test #%d...%n", test);
            final float[] floats = new float[numberOfElements];
            final byte[] bytes = new byte[numberOfElements];
            final int[] numbersOfPercentiles = {1, 2, 3, 5, 10, 20};
            for (int numberOfPercentiles : numbersOfPercentiles) {
                for (int kind = 1; kind <= (MEASURE_RANDOM_PERCENTILES ? 2 : 1); kind++) {
                    System.out.printf("  %d %s percentiles%n", numberOfPercentiles, kind == 1 ? "uniform" : "random");
                    double[] levels = new double[numberOfPercentiles];
                    for (int k = 0; k < levels.length; k++) {
                        levels[k] = kind == 1 ?
                                (levels.length == 1 ? 0.5 : (double) k / (double) (levels.length - 1)) :
                                rnd.nextDouble();
                    }
                    Arrays.sort(levels);

                    System.out.println("    Increasing order:");
                    for (int k = 0; k < floats.length; k++) {
                        floats[k] = k;
                    }
                    measureSpeed(floats, levels);
                    System.out.println("    Decreasing order:");
                    for (int k = 0; k < floats.length; k++) {
                        floats[k] = -k;
                    }
                    measureSpeed(floats, levels);
                    System.out.println("    Random array:");
                    rnd.setSeed(0);
                    for (int k = 0; k < floats.length; k++) {
                        floats[k] = (float) rnd.nextDouble();
                    }
                    measureSpeed(floats, levels);
                    System.out.println("    Constant array:");
                    Arrays.fill(floats, 1.0f);
                    measureSpeed(floats, levels);

                    System.out.println("    Increasing order:");
                    for (int k = 0; k < bytes.length; k++) {
                        bytes[k] = (byte) (k * 127.0 / bytes.length);
                    }
                    measureSpeed(bytes, levels);
                    System.out.println("    Decreasing order:");
                    for (int k = 0; k < bytes.length; k++) {
                        bytes[k] = (byte) (-Math.round(k * 127.0 / bytes.length));
                    }
                    measureSpeed(bytes, levels);
                    System.out.println("    Random array:");
                    rnd.setSeed(0);
                    for (int k = 0; k < bytes.length; k++) {
                        bytes[k] = (byte) rnd.nextInt();
                    }
                    measureSpeed(bytes, levels);
                    System.out.println("    Constant array:");
                    Arrays.fill(bytes, (byte) 1);
                    measureSpeed(bytes, levels);

                    System.out.println();
                }
            }
            System.out.println();
        }

        rnd.setSeed(new Random().nextLong());
        for (int test = 1; test <= numberOfTests; test++) {
            if (test % 10 == 0) {
                System.out.printf("\rTest for correctness #%d/%d for %d elements...",
                        test, numberOfTests, numberOfElements);
            }
            /*Repeat() float ==> double;; Float ==> Double;; \(double\)\s ==>
             */
            final float[] floats = new float[numberOfElements];
            for (int numberOfPercentiles = 1; numberOfPercentiles <= 10; numberOfPercentiles++) {
                double[] levels = new double[numberOfPercentiles];
                for (int k = 0; k < levels.length; k++) {
                    levels[k] = rnd.nextDouble();
                }
                Arrays.sort(levels);
                for (int k = 0; k < floats.length; k++) {
                    floats[k] = 10.0f + k;
                }
                testCorrectness(floats, levels, rnd);
                for (int k = 0; k < floats.length; k++) {
                    floats[k] = 10.0f - k;
                }
                testCorrectness(floats, levels, rnd);
                for (int k = 0; k < floats.length; k++) {
                    floats[k] = (float) rnd.nextDouble();
                }
                testCorrectness(floats, levels, rnd);
                for (int k = 0; k < floats.length; k++) {
                    floats[k] = rnd.nextBoolean() ? Float.NaN : (float) rnd.nextDouble();
                }
                testCorrectness(floats, levels, rnd);
            }
            /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
            final double[] doubles = new double[numberOfElements];
            for (int numberOfPercentiles = 1; numberOfPercentiles <= 10; numberOfPercentiles++) {
                double[] levels = new double[numberOfPercentiles];
                for (int k = 0; k < levels.length; k++) {
                    levels[k] = rnd.nextDouble();
                }
                Arrays.sort(levels);
                for (int k = 0; k < doubles.length; k++) {
                    doubles[k] = 10.0f + k;
                }
                testCorrectness(doubles, levels, rnd);
                for (int k = 0; k < doubles.length; k++) {
                    doubles[k] = 10.0f - k;
                }
                testCorrectness(doubles, levels, rnd);
                for (int k = 0; k < doubles.length; k++) {
                    doubles[k] = rnd.nextDouble();
                }
                testCorrectness(doubles, levels, rnd);
                for (int k = 0; k < doubles.length; k++) {
                    doubles[k] = rnd.nextBoolean() ? Double.NaN : rnd.nextDouble();
                }
                testCorrectness(doubles, levels, rnd);
            }
            /*Repeat.AutoGeneratedEnd*/
            final byte[] bytes = new byte[numberOfElements];
            for (int numberOfPercentiles = 1; numberOfPercentiles <= 10; numberOfPercentiles++) {
                double[] levels = new double[numberOfPercentiles];
                for (int k = 0; k < levels.length; k++) {
                    levels[k] = rnd.nextDouble();
                }
                Arrays.sort(levels);
                for (int k = 0; k < bytes.length; k++) {
                    bytes[k] = (byte) (10 + k);
                }
                testCorrectness(bytes, levels, rnd);
                for (int k = 0; k < bytes.length; k++) {
                    bytes[k] = (byte) (10 - k);
                }
                testCorrectness(bytes, levels, rnd);
                for (int k = 0; k < bytes.length; k++) {
                    bytes[k] = (byte) rnd.nextInt();
                }
                testCorrectness(bytes, levels, rnd);
            }
        }
        System.out.println("\r          \rO'k");
    }
}
