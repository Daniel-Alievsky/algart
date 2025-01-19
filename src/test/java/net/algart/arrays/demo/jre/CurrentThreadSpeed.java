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

package net.algart.arrays.demo.jre;

import java.util.Locale;

public class CurrentThreadSpeed {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + CurrentThreadSpeed.class.getName()
                + " loopLength numberOfIterations");
            return;
        }
        final int n = Integer.parseInt(args[0]);
        int numberOfIterations = Integer.parseInt(args[1]);
        Thread[] threads = new Thread[n];
        for (int iteration = 1; iteration <= numberOfIterations; iteration++) {
            System.out.print("Test #" + iteration + ": ");
            long t1 = System.nanoTime();
            Thread thread = null;
            for (int k = 0; k < n; k++) {
                thread = Thread.currentThread();
                threads[k] = thread;
            }
            long t2 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                threads[k] = thread;
            }
            long t3 = System.nanoTime();
            System.out.printf(Locale.US, "%.4f ns Thread.currentThread() (%s), %.4f ns simple filling%n",
                (double)(t2 - t1) / n, thread, (double)(t3 - t2) / n);
        }
    }
}
