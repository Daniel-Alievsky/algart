/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.finalizing.demo;

import java.io.*;

/**
 * <p>Test for very intensive calls of <tt>System.gc()</tt> and <tt>System.runFinalization</tt> methods.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.4
 */
public class IntensiveGcTest {
    public static void main(String[] args) throws IOException {
        boolean doGc = false, doRunFinalization = false;
        int startArgIndex = 0;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("gc")) {
                doGc = true;
                startArgIndex++;
            } else if (arg.equalsIgnoreCase("runFinalization")) {
                doRunFinalization = true;
                startArgIndex++;
            }
        }

        if (args.length < startArgIndex + 1) {
            System.out.println("Usage: " + IntensiveGcTest.class.getName()
                + " [gc] [runFinalization] timeoutInMilliseconds");
            return;
        }

        if (!(doGc || doRunFinalization)) {
            System.out.println("No gc or runFinalization flag is set: nothing to do");
            return;
        }
        long timeoutInMilliseconds = Long.parseLong(args[startArgIndex]);

        long tFix = System.currentTimeMillis(), t;
        int count = 0;
        byte[][] data = new byte[1024][];
        do {
            for (int k = 0; k < data.length; k++)
                data[k] = new byte[100 * k];
            data = new byte[1024][];
            for (int k = 0; k < 100; k++) {
                ++count;
                if (doRunFinalization)
                    System.runFinalization();
                if (doGc)
                    System.gc();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    System.err.println("Unexpected interruption: " + e);
                }
            }
            t = System.currentTimeMillis();
            System.out.print("\r" + count + " calls");
        } while (t - tFix < timeoutInMilliseconds);
        System.out.println();
    }
}
