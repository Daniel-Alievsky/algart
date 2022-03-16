/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Basic test for {@link net.algart.arrays.ArraySorter} class</p>
 *
 * @author Daniel Alievsky
 */
public class ArraySorterTest {
    private String beginOfArray(double[] a, int[] indexes) {
        StringBuffer sb = new StringBuffer();
        int len = a != null ? a.length : indexes.length;
        for (int k = Math.max(leftGap, 0), count = 0; k < len - rightGap; k++, count++) {
            // Math.max here allows to test throwing exceptions in Sorter
            if (count > 0) {
                sb.append(", ");
            }
            if (sb.length() > 115) {
                sb.append("..."); break;
            }
            sb.append(Math.round(indexes == null ? a[k] : a == null ? indexes[k] : a[indexes[k]]));
        }
        return sb.toString();
    }

    private int leftGap = 0, rightGap = 0;
    private ArraySorter sorter;

    private double[] a;
    private int[] indexes;
    private void testSort(double[] source) {
        long t1, t2;
        if (a == null)
            a = new double[source.length];
        if (indexes == null)
            indexes = new int[a.length];
        // - avoid garbage collection for correct timing

        class MyComparatorExchanger implements ArrayComparator32, ArrayExchanger32 {
            public boolean less(int i, int j) {
                return a[i] < a[j];
            }

            public void swap(int i, int j) {
                double temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        };
        MyComparatorExchanger ce = new MyComparatorExchanger();

        int len = a.length - leftGap - rightGap;
        int fromIndex = leftGap, toIndex = a.length - rightGap;

        for (int k = 0; k < a.length; k++)
            indexes[k] = k;
        System.arraycopy(source, 0, a, 0, a.length);
        System.out.println("  Array:   " + beginOfArray(a, null));
        System.out.println("  Indexes: " + beginOfArray(null, indexes));
        System.out.println();

        System.out.println("Sorting " + len + " elements...");
        t1 = System.nanoTime();
        sorter.sort(fromIndex, toIndex, ce, ce);
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.3f ms, %.3f ns/element%n", (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);
        System.out.println("  Array:   " + beginOfArray(a, null));
        if (!ArraySorter.areSorted(fromIndex, toIndex, ce))
            throw new RuntimeException("ERROR found!");
        System.out.println("");

        System.out.println("Sorting " + len + " indexes...");
        System.arraycopy(source, 0, a, 0, a.length);
        t1 = System.nanoTime();
        sorter.sortIndexes(indexes, fromIndex, toIndex, ce);
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.3f ms, %.3f ns/element%n", (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);
        System.out.println("  Array:   " + beginOfArray(a, null));
        System.out.println("  Indexes: " + beginOfArray(null, indexes));
        System.out.println("  Indexed: " + beginOfArray(a, indexes));
        if (!ArraySorter.areIndexesSorted(indexes, fromIndex, toIndex, ce))
            throw new RuntimeException("ERROR found!");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + ArraySorterTest.class.getName()
                + " Insertion|Shell|QuickSort arrayLength [leftGap [rightGap]]");
            return;
        }

        ArraySorterTest test = new ArraySorterTest();
        if (args[0].equalsIgnoreCase("Insertion")) {
            test.sorter = ArraySorter.getInsertionSorter();
        } else if (args[0].equalsIgnoreCase("Shell")) {
            test.sorter = ArraySorter.getShellSorter();
        } else if (args[0].equalsIgnoreCase("QuickSort")) {
            test.sorter = ArraySorter.getQuickSorter();
        } else {
            System.err.println("Unknown sorting algorithm.");
            return;
        }
        if (args.length > 2)
            test.leftGap = Integer.parseInt(args[2]);
        if (args.length > 3)
            test.rightGap = Integer.parseInt(args[3]);
        double[] anArray = new double[Integer.parseInt(args[1])];


        System.out.println("********************");
        System.out.println("TESTING " + test.sorter);
        System.out.println("");
        for (int count = 0; count < 3; count++) {
            System.out.println("********************");
            System.out.println("Iteration #" + (count + 1) + "/3");
            System.out.println("");

            System.out.println("TESTING INCREASING ARRAY");
            for (int k = 0; k < anArray.length; k++)
                anArray[k] = 10.0 * k;
            test.testSort(anArray);

            System.out.println("TESTING DECREASING ARRAY");
            for (int k = 0; k < anArray.length; k++)
                anArray[k] = 10.0 * (anArray.length - 1 - k);
            test.testSort(anArray);

            System.out.println("TESTING CONSTANT ARRAY");
            for (int k = 0; k < anArray.length; k++)
                anArray[k] = 28;
            test.testSort(anArray);

            System.out.println("TESTING RANDOM ARRAY");
            for (int k = 0; k < anArray.length; k++)
                anArray[k] = 9 * (int)(1000 * Math.random());
            test.testSort(anArray);
        }
    }
}
