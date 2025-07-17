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

/**
 * <p>Speed of System.arraycopy and long[].clone methods in comparison with a simple loop</p>
 *
 * @author Daniel Alievsky
 */
public class ArraycopyAndCloneSpeed {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: " + ArraycopyAndCloneSpeed.class.getName()
                + " maxArrayLength loopLength numberOfIterations");
            return;
        }
        final int maxLen = Integer.parseInt(args[0]);
        final int n = Integer.parseInt(args[1]);
        int numberOfIterations = Integer.parseInt(args[2]);
        for (int iteration = 1; iteration <= numberOfIterations; iteration++) {
            System.out.println("Test #" + iteration);
            for (int len = 0; len < maxLen; len++) {
                long[] a = new long[len], b = new long[len];
                long t1 = System.nanoTime();
                for (int k = 0; k < n; k++) {
                    for (int j = 0; j < a.length; j++)
                        b[j] = a[j];
                }
                long t2 = System.nanoTime();
                for (int k = 0; k < n; k++) {
                    System.arraycopy(a, 0, b, 0, a.length);
                }
                long t3 = System.nanoTime();
                for (int k = 0; k < n; k++) {
                    b = a.clone();
                }
                long t4 = System.nanoTime();
                System.out.printf(Locale.US, "%d elements: %.3f ns simple loop, %.3f ns arraycopy, %.3f ns clone%n",
                    len, (double)(t2 - t1) / n, (double)(t3 - t2) / n, (double)(t4 - t3) / n);
            }
            System.out.println();
        }
    }
}
