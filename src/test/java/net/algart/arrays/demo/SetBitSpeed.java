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
import net.algart.math.functions.LinearFunc;

import java.nio.IntBuffer;
import java.util.Locale;
import java.util.Random;

/**
 * <p>Speed of primitive arrays: simplest speed test</p>
 *
 * @author Daniel Alievsky
 */
public class SetBitSpeed {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();

    private static void testNormal(
            boolean[] data,
            UpdatableBitArray a1,
            UpdatableBitArray a2,
            UpdatableBitArray a3,
            UpdatableBitArray a4) {
        final int arrayLength = data.length;
        long t1 = System.nanoTime();
        a1.setData(0, data);
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "setData: %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);

        boolean ok = true;
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            ok = ok & (a1.getBit(i) == data[i]);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "getBit(i): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!ok) {
            throw new AssertionError("Bug in getBit(i)");
        }

        a2.fill(0);
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            a2.setBit(i, data[i]);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "setBit(i, boolean): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!a1.equals(a2)) {
            throw new AssertionError("Bug in setBit(i, boolean)");
        }

        a3.fill(0);
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            if (data[i]) {
                a3.setBit(i);
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "setBit(i): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!a1.equals(a3)) {
            throw new AssertionError("Bug in setBit(i)");
        }

        a4.fill(1);
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            if (!data[i]) {
                a4.clearBit(i);
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "clearBit(i): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!a1.equals(a4)) {
            throw new AssertionError("Bug in clearBit(i)");
        }
    }

    private static void testNoSync(
            boolean[] data,
            UpdatableBitArray a1,
            UpdatableBitArray a2,
            UpdatableBitArray a3,
            UpdatableBitArray a4) {
        final int arrayLength = data.length;
        long t1 = System.nanoTime();
        a1.setData(0, data);
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "setData: %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);

        a2.fill(0);
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            a2.setBitNoSync(i, data[i]);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "setBitNoSync(i, boolean): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!a1.equals(a2)) {
            throw new AssertionError("Bug in setBitNoSync(i, boolean)");
        }

        a3.fill(0);
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            if (data[i]) {
                a3.setBitNoSync(i);
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "setBitNoSync(i): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!a1.equals(a3)) {
            throw new AssertionError("Bug in setBitNoSync(i)");
        }

        a4.fill(1);
        t1 = System.nanoTime();
        for (int i = 0; i < arrayLength; i++) {
            if (!data[i]) {
                a4.clearBitNoSync(i);
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "clearBitNoSync(i): %.3f ms, %.2f ns/element)%n",
                (t2 - t1) * 1e-6, (double) (t2 - t1) / arrayLength);
        if (!a1.equals(a4)) {
            throw new AssertionError("Bug in clearBitNoSync(i)");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.out.println("Usage: " + SetBitSpeed.class.getName()
                + " arrayLength numberOfIterations");
            return;
        }
        System.out.println(mm.toString());

        int arrayLength = Integer.parseInt(args[0]);
        int numberOfIterations = Integer.parseInt(args[1]);

        Random rnd = new Random(156);
        boolean[] testData = new boolean[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            testData[i] = rnd.nextBoolean();
        }

        boolean[] data = new boolean[arrayLength];
        UpdatableBitArray a1 = mm.newUnresizableBitArray(arrayLength);
        UpdatableBitArray a2 = mm.newUnresizableBitArray(arrayLength);
        UpdatableBitArray a3 = mm.newUnresizableBitArray(arrayLength);
        UpdatableBitArray a4 = mm.newUnresizableBitArray(arrayLength);

        for (int test = 1; test <= numberOfIterations; test++) {
            System.out.printf("%nTest #%d for %s%n", test, a1);

            System.out.println("  Random values:");
            System.arraycopy(testData, 0, data, 0, data.length);
            testNormal(data,a1, a2, a3, a4);
            testNoSync(data,a1, a2, a3, a4);

            System.out.println("  1 values:");
            java.util.Arrays.fill(data, true);
            testNormal(data,a1, a2, a3, a4);
            testNoSync(data,a1, a2, a3, a4);

            System.out.println("  0 values:");
            java.util.Arrays.fill(data, false);
            testNormal(data,a1, a2, a3, a4);
            testNoSync(data,a1, a2, a3, a4);
        }
        a1.freeResources();
        a2.freeResources();
        a3.freeResources();
        a4.freeResources();
        System.gc();
        System.gc();
    }
}
