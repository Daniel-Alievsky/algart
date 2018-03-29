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

package net.algart.arrays.demo;

import java.util.Random;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>Simple access to large array from several threads.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class MultithreadAccessTest {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();

    // Unfortunately, we cannot test "swap" and "copy" operations in this test:
    // though they do not change the total sum, but the loop of sum calculation
    // may return invalid value (unless full this loop is synchronized)

    private static long calculateSum(ByteArray a, boolean blockMode) {
        long sum = 0;
        if (blockMode) {
            byte[] buf = new byte[65536];
            for (long pos = 0, n = a.length(); pos < n; pos += buf.length) {
//                Thread.yield();
                a.getData(pos, buf);
                for (int j = 0, len = (int) Math.min(n - pos, buf.length); j < len; j++) {
                    sum += buf[j];
                }
//                a.freeResources(null);
            }
        } else {
            for (long k = 0, n = a.length(); k < n; k++) {
                sum += (byte) a.getByte(k);
            }
        }
        return sum;
    }

    private static long fillAndReturnSum(UpdatableByteArray a, boolean blockMode, boolean random, boolean shuffle) {
        long sum = 0;
        Random rnd = random ? new Random(157) : null;
        if (shuffle && !blockMode)
            throw new IllegalArgumentException();
        if (blockMode) {
            byte[] buf = new byte[65536];
            for (long pos = 0, n = a.length(); pos < n; pos += buf.length) {
                int len = (int) Math.min(n - pos, buf.length);
                if (shuffle) {
                    if (random) {
                        for (int j = len - 1; j >= 0; j--) {
                            buf[j] = (byte) (rnd.nextInt() >>> 24);
                            sum += buf[j];
                        }
                    } else {
                        for (int j = 0; j < len; j++) {
                            buf[len - 1 - j] = (byte) (pos + j);
                            sum += buf[len - 1 - j];
                        }
                    }
                } else {
                    if (random) {
                        for (int j = 0; j < len; j++) {
                            buf[j] = (byte) (rnd.nextInt() >>> 24);
                            sum += buf[j];
                        }
                    } else {
                        for (int j = 0; j < len; j++) {
                            buf[j] = (byte) (pos + j);
                            sum += buf[j];
                        }
                    }
                }
//                a.freeResources(null);
//                Thread.yield();
                a.setData(pos, buf);
            }
        } else {
            if (random) {
                for (long k = 0, n = a.length(); k < n; k++) {
                    byte v = (byte) (rnd.nextInt() >>> 24);
                    a.setByte(k, v);
                    sum += v;
                }
            } else {
                for (long k = 0, n = a.length(); k < n; k++) {
                    byte v = (byte) k;
                    a.setByte(k, v);
                    sum += v;
                }
            }
        }
        return sum;
    }

    private boolean readOnly = false;
    private boolean random = false;
    private boolean shuffle = false;
    private long requiredSum;
    private int numberOfPasses;

    private void test(UpdatableByteArray a, int mode) {
        if (mode >= 2 && readOnly)
            return;
        boolean blockMode = mode % 2 == 0 || shuffle;
        // Even in LARGE model, built-in synchronization is insufficient if we shall access individual bytes
        long t1 = System.nanoTime();
        long sum = requiredSum;
        for (int pass = 0; pass < numberOfPasses; pass++) {
            if (mode < 2) {
                sum = calculateSum(a, blockMode);
            } else {
                sum = fillAndReturnSum(a, blockMode, random, shuffle);
            }
            if (sum != requiredSum) {
                System.err.println("Bug #" + mode + " is found! sum = " + sum
                    + " instead of " + requiredSum
                    + (mode < 2 ? " (reading " : " (updating ")
                    + (blockMode ? "per blocks)" : "per bytes)"));
                Arrays.freeAllResources();
                System.exit(1);
            }
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, Thread.currentThread() + " %d passes "
            + (mode < 2 ? "Sum of the array  " : "Filling the array ")
            + (blockMode ? "per blocks: " : "per bytes:  ")
            + "%d (%.4f ms, %.2f ns/element)%n",
            numberOfPasses, sum, (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / a.length() / numberOfPasses);
    }

    private static void mainImpl(String[] args) throws InterruptedException {
        final MultithreadAccessTest instance = new MultithreadAccessTest();
        int startArgIndex = 0;
        instance.random = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-random");
        if (instance.random)
            startArgIndex++;
        instance.shuffle = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-shuffle");
        if (instance.shuffle)
            startArgIndex++;
        instance.readOnly = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-readOnly");
        if (instance.readOnly)
            startArgIndex++;

        if (args.length < startArgIndex + 4) {
            System.out.println("Usage: " + MultithreadAccessTest.class.getName()
                + " [-random] [-shuffle] [-readOnly]"
                + " byteArrayLength passCount threadCount testCount");
            return;
        }
        DemoUtils.initializeClass();
        System.out.println(mm.toString());

        long n = Long.parseLong(args[startArgIndex]);
        instance.numberOfPasses = Integer.parseInt(args[startArgIndex + 1]);
        int numberOfThreads = Integer.parseInt(args[startArgIndex + 2]);
        int numberOfTests = Integer.parseInt(args[startArgIndex + 3]);

        final UpdatableByteArray a = mm.newUnresizableByteArray(n);
        System.out.println("Created array: " + a);
        instance.requiredSum = fillAndReturnSum(a, true, instance.random, instance.shuffle);
        System.out.println("Successfully filled: sum = " + instance.requiredSum);

        Thread[] threads = new Thread[numberOfThreads];
        for (int count = 1; count <= numberOfTests; count++) {
            System.out.println();
            System.out.println("*** ITERATION #" + count);

            for (int m = 0; m < numberOfThreads; m++) {
                final int mode = m + count;
                threads[m] = new Thread("Thread #" + m) {
                    public void run() {
                        instance.test(a, mode % 4);
                        instance.test(a, (mode + 1) % 4);
                        instance.test(a, (mode + 2) % 4);
                        instance.test(a, (mode + 3) % 4);
                    }

                    public String toString() {
                        return getName();
                    }
                };
                threads[m].start();
            }
            System.out.println("Waiting for " + numberOfThreads + " threads...");
            for (int m = 0; m < numberOfThreads; m++) {
                threads[m].join();
            }
            System.out.println(numberOfThreads + " threads are finished");
        }
    }

    public static void main(String[] args) {
        try {
            mainImpl(args);
            // separate method allows to finalize all objects
            // freeResource here will not allow file deletion on fullGC()
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        DemoUtils.fullGC();
    }
}
