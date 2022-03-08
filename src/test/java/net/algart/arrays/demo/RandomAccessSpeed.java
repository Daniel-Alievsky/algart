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

import java.util.Random;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>Speed random read/write operations.
 * Useful also for testing bank swapping in {@link LargeMemoryModel}.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class RandomAccessSpeed {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: " + RandomAccessSpeed.class.getName()
                + " boolean|byte|short arrayLength numberOfAccesses [numberOfTests]");
            return;
        }
        DemoUtils.initializeClass();
        System.out.println(mm.toString());
        System.out.println();

        Class<?> elementType =
            args[0].equals("boolean") ? boolean.class :
            args[0].equals("byte") ? byte.class :
            args[0].equals("short") ? short.class :
            null;
        if (elementType == null)
            throw new IllegalArgumentException("Unknown element type: " + args[0]);
        int len = Integer.parseInt(args[1]);
        int numberOfAccesses = Integer.parseInt(args[2]);
        int numberOfTests = args.length > 3 ? Integer.parseInt(args[3]) : 3;

        System.out.println("Creating arrays...");
        short[] ja = new short[len];
        UpdatablePFixedArray a = (UpdatablePFixedArray)mm.newUnresizableArray(elementType, len);

        System.out.println("Creating access sequence...");
        int[] indexes = new int[numberOfAccesses];
        Random rnd = new Random(157);
        for (int k = 0; k < indexes.length; k++) {
            indexes[k] = rnd.nextInt(len) + 1;
            if (rnd.nextBoolean())
                indexes[k] = -indexes[k];
        }
        System.out.println("There will be " + indexes.length
            + " read/write random accesses to the following arrays:");
        System.out.println("    " + a);
        System.out.println("    short[" + len + "] Java array");

        for (int count = 1; count <= numberOfTests; count++) {
            System.out.println();
            System.out.println("*** ITERATION #" + count);
            long t1 = System.nanoTime();
            int sum1 = 0;
            if (elementType == boolean.class) {
                for (int k = 0; k < indexes.length; k++) {
                    int i = indexes[k];
                    if (i > 0)
                        ja[i - 1] = (k & 7) == 5 ? (short)1 : 0;
                    else
                        sum1 += ja[ -i - 1];
                }
            } else if (elementType == byte.class) {
                for (int k = 0; k < indexes.length; k++) {
                    int i = indexes[k];
                    if (i > 0)
                        ja[i - 1] = (short)(k & 0xFF);
                    else
                        sum1 += ja[ -i - 1];
                }
            }
            if (elementType == short.class) {
                for (int k = 0; k < indexes.length; k++) {
                    int i = indexes[k];
                    if (i > 0)
                        ja[i - 1] = (short)k;
                    else
                        sum1 += ja[ -i - 1] & 0xFFFF;
                }
            }
            long t2 = System.nanoTime();
            int sum2 = 0;
            if (elementType == boolean.class) {
                for (int k = 0; k < indexes.length; k++) {
                    int i = indexes[k];
                    if (i > 0)
                        a.setInt(i - 1, (k & 7) == 5 ? 1 : 0);
                    else
                        sum2 += a.getInt( -i - 1);
                }
            } else {
                for (int k = 0; k < indexes.length; k++) {
                    int i = indexes[k];
                    if (i > 0)
                        a.setInt(i - 1, k);
                    else
                        sum2 += a.getInt( -i - 1);
                }
            }
            long t3 = System.nanoTime();
            System.out.printf(Locale.US,
                "Random filling and calculating the sum %d, Java array:   %d ns, %.2f ns/element%n",
                sum1, t2 - t1, (t2 - t1) * 1.0 / indexes.length);
            System.out.printf(Locale.US,
                "Random filling and calculating the sum %d, AlgART array: %d ns, %.2f ns/element%n",
                sum2, t3 - t2, (t3 - t2) * 1.0 / indexes.length);
            if (sum1 != sum2)
                throw new AssertionError("Bug found! Different sums!");
            t1 = System.nanoTime();
            for (int k = 0; k < len; k++)
                if ((ja[k] & 0xFFFF) != a.getInt(k))
                    throw new AssertionError("Bug found! Different arrays at position #" + k
                        + ": " + (ja[k] & 0xFFFF) + " in Java array, "
                        + a.getInt(k) + " in AlgART array");
            t2 = System.nanoTime();
            System.out.printf(Locale.US,
                "Comparing filled arrays - all OK: %d ns, %.2f ns/element%n", t2 - t1, (t2 - t1) * 1.0 / len);
        }
        a = null; // allows garbage collection
        DemoUtils.fullGC();
    }
}
