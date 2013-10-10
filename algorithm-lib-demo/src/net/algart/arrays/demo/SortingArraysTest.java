package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.util.Locale;

/**
 * <p>Test for sorting AlgART arrays.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
        long sum = source instanceof PFixedArray ? Arrays.preciseSumOf((PFixedArray)source) : 0;
        System.out.println("  Array:   " + Arrays.toString(a, "; ", 100));
        System.out.println("           " + a);
        System.out.println("Sorting " + a.length() + " elements by decreasing...");
        t1 = System.nanoTime();
//        Arrays.zeroFill(a);
        Arrays.sort(a, Arrays.reverseOrderComparator(a));
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.3f ms, %.3f ns/element%n",
            (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / a.length());
        System.out.println("  Array:    " + Arrays.toString(a, "; ", 100));
        if (!ArraySorter.areSorted(0, a.length(), Arrays.reverseOrderComparator(a)))
            throw new RuntimeException("ERROR in order found!");
        long newSum = a instanceof PFixedArray ? Arrays.preciseSumOf((PFixedArray)a) : 0;
        if (newSum != sum)
            throw new RuntimeException("ERROR in sum found: " + newSum + " instead of " + sum + "!");
        a.copy(source);  // - avoid garbage collection for correct timing
        System.out.println("Sorting " + a.length() + " elements by increasing...");
        t1 = System.nanoTime();
        Arrays.sort(a, Arrays.normalOrderComparator(a));
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.3f ms, %.3f ns/element%n",
            (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / a.length());
        System.out.println("  Array:    " + Arrays.toString(a, "; ", 100));
        if (!ArraySorter.areSorted(0, a.length(), Arrays.normalOrderComparator(a)))
            throw new RuntimeException("ERROR found!");
        newSum = a instanceof PFixedArray ? Arrays.preciseSumOf((PFixedArray)a) : 0;
        if (newSum != sum)
            throw new RuntimeException("ERROR in sum found: " + newSum + " instead of " + sum + "!");
    }

    public static void main(String[] args) {
        int startArgIndex = 0;
        SortingArraysTest test = new SortingArraysTest();
        if (test.sortLazyCopy = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-sortLazyCopy"))
            startArgIndex++;
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage: " + SortingArraysTest.class.getName()
                + " [-sortLazyCopy] " + DemoUtils.possibleArg(true) + " arrayLength");
            return;
        }
        System.out.println(Arrays.SystemSettings.globalMemoryModel());
        System.out.println();
        UpdatableArray array = DemoUtils.createTestUnresizableArray(
            args[startArgIndex],
            Long.parseLong(args[startArgIndex + 1]));
        test.mm = DemoUtils.memoryModel(args[startArgIndex]);

        System.out.println("********************");
        System.out.println("TESTING ");
        System.out.println();
        for (int count = 0; count < 3; count++) {
            long len = array.length();

            System.out.println("********************");
            System.out.println("Iteration #" + (count + 1) + "/3");
            System.out.println();

            System.out.println("TESTING INCREASING ARRAY");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k, (int)k);
            test.testSort(array);

            System.out.println("TESTING DECREASING ARRAY");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k, (int)(len - 1 - k));
            test.testSort(array);

            System.out.println("TESTING CONSTANT ARRAY");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k, 28);
            test.testSort(array);

            System.out.println("TESTING RANDOM ARRAY");
            for (long k = 0; k < len; k++)
                DemoUtils.changeTestArray(array, k,
                    (int)(Math.round(Arrays.maxPossibleValue(array.getClass(), 100.0) * Math.random())));
            test.testSort(array);
            System.gc();
            System.out.println("System.gc()");
            System.out.println();
        }
        array.freeResources(null);
        test = null; // allows garbage collection
        DemoUtils.fullGC();
    }
}
