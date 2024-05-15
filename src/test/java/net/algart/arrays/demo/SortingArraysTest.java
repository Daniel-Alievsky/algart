/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.Locale;
import java.util.Random;

/**
 * <p>Test for sorting AlgART arrays.</p>
 *
 * @author Daniel Alievsky
 */

public class SortingArraysTest {
    private MemoryModel mm;
    private UpdatableArray a;
    private boolean sortLazyCopy;

    private void testSort(UpdatableArray source) {
        long t1, t2;
        if (sortLazyCopy) {
            a = mm.newUnresizableLazyCopy(source);
        } else {
            if (a == null) {
                a = mm.newUnresizableArray(source);
            }
            a.copy(source);  // - avoid garbage collection for correct timing
        }
        long sum = source instanceof PFixedArray ? Arrays.preciseSumOf((PFixedArray) source) : 0;
        System.out.println("  Array:   " + Arrays.toString(a, "; ", 200));
        System.out.println("           " + a);
        System.out.println("Sorting " + a.length() + " elements by decreasing...");
        t1 = System.nanoTime();
//        Arrays.zeroFill(a);
        Arrays.sort(a, Arrays.reverseOrderComparator(a));
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / a.length());
        System.out.println("  Sorted array:    " + Arrays.toString(a, "; ", 200));
        if (!ArraySorter.areSorted(0, a.length(), Arrays.reverseOrderComparator(a)))
            throw new RuntimeException("ERROR in order found!");
        long newSum = a instanceof PFixedArray ? Arrays.preciseSumOf((PFixedArray) a) : 0;
        if (newSum != sum)
            throw new RuntimeException("ERROR in sum found: " + newSum + " instead of " + sum + "!");
        a.copy(source);  // - avoid garbage collection for correct timing
        System.out.println("Sorting " + a.length() + " elements by increasing...");
        t1 = System.nanoTime();
        Arrays.sort(a, Arrays.normalOrderComparator(a));
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / a.length());
        System.out.println("  Sorted array:    " + Arrays.toString(a, "; ", 200));
        if (!ArraySorter.areSorted(0, a.length(), Arrays.normalOrderComparator(a)))
            throw new RuntimeException("ERROR found!");
        newSum = a instanceof PFixedArray ? Arrays.preciseSumOf((PFixedArray) a) : 0;
        if (newSum != sum)
            throw new RuntimeException("ERROR in sum found: " + newSum + " instead of " + sum + "!");
    }

    public static void main(String[] args) {
        int startArgIndex = 0;
        SortingArraysTest test = new SortingArraysTest();
        if (test.sortLazyCopy = startArgIndex < args.length
                && args[startArgIndex].equalsIgnoreCase("-sortLazyCopy")) {
            startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage: " + SortingArraysTest.class.getName()
                    + " [-sortLazyCopy] " + DemoUtils.possibleArg(true) + " arrayLength");
            return;
        }
        final String elementTypeName = args[startArgIndex];
        final long arrayLength = Long.parseLong(args[startArgIndex + 1]);

        System.out.println(Arrays.SystemSettings.globalMemoryModel());
        System.out.println();
        UpdatableArray array = DemoUtils.createTestUnresizableArray(elementTypeName, arrayLength);
        test.mm = DemoUtils.memoryModel(elementTypeName);

        System.out.println("********************");
        System.out.println("TESTING ");
        System.out.println();
        for (int count = 0; count < 3; count++) {
            long len = array.length();

            System.out.println("********************");
            System.out.println("Iteration #" + (count + 1) + "/3");
            System.out.println();

            System.out.printf("%nTESTING INCREASING ARRAY%n");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k, (int) k);
            test.testSort(array);

            System.out.printf("%nTESTING DECREASING ARRAY%n");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k, (int) (len - 1 - k));
            test.testSort(array);

            System.out.printf("%nTESTING CONSTANT ARRAY%n");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k, 28);
            test.testSort(array);

            System.out.printf("%nTESTING RANDOM ARRAY%n");
            final Random rnd = new Random();
            for (long k = 0; k < len; k++) {
                DemoUtils.changeTestArray(array, k,
                        (int) (Math.round(Arrays.maxPossibleValue(array.getClass(), 100.0)
                                * rnd.nextDouble())));
            }
            test.testSort(array);

            if (array instanceof PFloatingArray) {
                System.out.printf("%nTESTING RANDOM ARRAY WITH NAN%n");
                for (long k = 0; k < len; k++) {
                    if (rnd.nextInt(3) == 0) {
                        ((UpdatablePArray) array).setDouble(k, Double.NaN);
                    } else {
                        DemoUtils.changeTestArray(array, k,
                                (int) (Math.round(Arrays.maxPossibleValue(array.getClass(), 100.0)
                                        * rnd.nextDouble())));
                    }
                }
                test.testSort(array);
            }
            System.gc();
            System.out.println("System.gc()");
            System.out.println();
        }
        array.freeResources();
        test = null; // allows garbage collection
        DemoUtils.fullGC();
    }
}
