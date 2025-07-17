/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays.demo.jvm;

import java.util.Locale;

public class MillionsOfArraysTest {
    private static void testAllocation(int numberOfArrays, int arrayLength) {
        final Runtime rt =  Runtime.getRuntime();
        long m1 = rt.totalMemory() - rt.freeMemory();
        final byte[][] arrays = new byte[numberOfArrays][];
        long t1 = System.nanoTime();
        for (int i = 0; i < numberOfArrays; i++) {
            arrays[i] = new byte[arrayLength];
        }
        long m2 = rt.totalMemory() - rt.freeMemory();
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "%d arrays byte[%d] allocated in %.3f ms (%.2f ns/array), " +
                        "used memory %.3f MB (%.2f bytes/array)%n",
                numberOfArrays, arrays[0].length,
                (t2 - t1) * 1e-6, (double) (t2 - t1) / numberOfArrays,
                (double) (m2 - m1) / 1048576.0, (double) (m2 - m1) / numberOfArrays);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + MillionsOfArraysTest.class.getName()
                    + " arrayLength numberOfArrays");
            return;
        }
        final int arrayLength = Integer.parseInt(args[0]);
        final int numberOfArrays = Integer.parseInt(args[1]);

        for (int test = 0; test < 16; test++) {
            final Runtime rt = Runtime.getRuntime();
            System.out.printf("%nTest %d: current memory %.3f MB%n",
                    test, (double) (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
            testAllocation(numberOfArrays, arrayLength);

            System.gc();
            System.gc();
            System.gc();
        }
    }
}
