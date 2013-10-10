package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <p>Test for amount of memory required for different AlgART arrays.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */

public class UsedMemoryDemo {
    private static void test(String elementTypeName, long len, int numberOfArrays) {
        Runtime rt = Runtime.getRuntime();
        long m1 = rt.totalMemory() - rt.freeMemory();
        List<UpdatableArray> arrays = new ArrayList<UpdatableArray>();
        long t1 = System.nanoTime();
        long summaryLength = 0;
        long summarySizeOf = 0;
        for (int k = 0; k < numberOfArrays; k++) {
            UpdatableArray a = DemoUtils.createTestArray(elementTypeName, len, true, false);
            summaryLength += a.length();
            long sizeOf = Arrays.sizeOf(a);
            if (sizeOf == -1) {
                summarySizeOf = -1;
            } else {
                summarySizeOf += sizeOf;
            }
            arrays.add(a);
        }
        long m2 = rt.totalMemory() - rt.freeMemory();
        long t2 = System.nanoTime();
        System.out.println("Allocated " + summaryLength + " elements: ");
        System.out.printf(Locale.US, "    %.3f MB, %.3f bytes/element, %.3f bits/element%n",
            (m2 - m1) / 1048576.0, (m2 - m1) * 1.0 / summaryLength, (m2 - m1) * 8.0 / summaryLength);
        if (summarySizeOf == -1)
            System.out.printf(Locale.US, "    sizeOf: unknown%n");
        else
            System.out.printf(Locale.US,
                "    sizeOf: %d bytes (%.5f MB), %.3f bytes/element%n",
                summarySizeOf, summarySizeOf / 1048576.0, summarySizeOf * 1.0 / summaryLength);
        System.out.printf(Locale.US,
            "    %.3f ms, %.2f ns/element%n", (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / summaryLength);
        if (arrays.size() > 0) {
            System.out.println("    first array: " + arrays.get(0));
            if (CombinedMemoryModel.isCombinedArray(arrays.get(0))) {
                String[] storage = CombinedMemoryModel.getStorageToStrings(arrays.get(0));
                for (String aStorage : storage)
                    System.out.println("        " + aStorage);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: " + UsedMemoryDemo.class.getName() + " " + DemoUtils.possibleArg(false)
                + " arrayLength[K|M|G] numberOfArrays [numberOfIterations]");
            return;
        }
        System.out.println(Arrays.SystemSettings.globalMemoryModel().toString());
        Runtime rt = Runtime.getRuntime();
        System.out.printf(Locale.US, "Total memory %.3f MB, free memory %.3f MB, max memory %.3f MB%n%n",
            rt.totalMemory() / 1048576.0,
            rt.freeMemory() / 1048576.0,
            rt.maxMemory() / 1048576.0);
        int numberOfIterations = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        for (int count = 1; count <= numberOfIterations; count++) {
            System.out.println("*** ITERATION #" + count);
            test(args[0], Arrays.SystemSettings.parseLongWithMetricalSuffixes(args[1]), Integer.parseInt(args[2]));
            System.out.printf(Locale.US,
                "Used memory %.3f MB, total memory %.3f MB, free memory %.3f MB, max memory %.3f MB%n",
                (rt.totalMemory() - rt.freeMemory()) / 1048576.0,
                rt.totalMemory() / 1048576.0,
                rt.freeMemory() / 1048576.0,
                rt.maxMemory() / 1048576.0);
            System.out.println("System.gc() (5 times)");
            for (int k = 0; k < 5; k++) {
                System.gc();
            }
            System.out.printf(Locale.US,
                "Used memory %.3f MB, total memory %.3f MB, free memory %.3f MB, max memory %.3f MB%n",
                (rt.totalMemory() - rt.freeMemory()) / 1048576.0,
                rt.totalMemory() / 1048576.0,
                rt.freeMemory() / 1048576.0,
                rt.maxMemory() / 1048576.0);
            System.out.println();
        }
    }
}
