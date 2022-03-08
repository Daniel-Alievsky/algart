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

import java.nio.IntBuffer;
import java.util.Locale;

import net.algart.arrays.*;
import net.algart.math.functions.*;

/**
 * <p>Speed of primitive arrays: simplest speed test</p>
 *
 * @author Daniel Alievsky
 */
public class SimplePArraysSpeed {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();

    private static int mySum(IntArray a) {
        int result = 0;
        for (long k = 0, n = a.length(); k < n; k++) {
            result += a.getInt(k);
        }
        return result;
    }

    private static int mySumBuffered(IntArray a) {
        int result = 0;
        for (DataIntBuffer buf = a.buffer().map(0); buf.hasData(); buf.mapNext()) {
            int[] data = buf.data();
            for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                result += data[k];
            }
        }
        return result;
    }

    private static int mySumDirect(IntArray a) {
        if (a instanceof DirectAccessible && ((DirectAccessible)a).hasJavaArray()) {
            int[] ja = (int[])((DirectAccessible)a).javaArray();
            int offset = ((DirectAccessible)a).javaArrayOffset();
            int count = ((DirectAccessible)a).javaArrayLength();
            int result = 0;
            for (int k = offset, n = offset + count; k < n; k++) {
                result += ja[k];
            }
            return result;
        } else if (BufferMemoryModel.isBufferArray(a)) {
            IntBuffer ib = BufferMemoryModel.getByteBuffer(a).asIntBuffer();
            int offset = (int)BufferMemoryModel.getBufferOffset(a);
            int result = 0;
            for (int k = offset, n = offset + (int)a.length(); k < n; k++) {
                result += ib.get(k);
            }
            return result;
        } else {
            return mySum(a);
        }
    }

    private static void myFill(UpdatableIntArray a, int value) {
        for (long k = 0, n = a.length(); k < n; k++) {
            a.setInt(k, value++);
        }
    }

    private static void myFillBuffered(UpdatableIntArray a, int value) {
        for (DataIntBuffer buf = a.buffer().map(0); buf.hasData(); buf.mapNext()) {
            int[] data = buf.data();
            for (int k = buf.from(), kMax = buf.to(); k < kMax; k++) {
                data[k] = value++;
            }
            buf.force();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int startArgIndex = 0;
        boolean pack = false, direct = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-pack")) {
            pack = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-direct")) {
            direct = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage: " + SimplePArraysSpeed.class.getName()
                + " -pack -direct arrayLength numberOfIterations");
            return;
        }
        System.out.println(mm.toString());

        long arrayLength = Long.parseLong(args[startArgIndex]);
        int numberOfIterations = Integer.parseInt(args[startArgIndex + 1]);

        long t1, t2, t3, t4, t5;
        t1 = System.nanoTime();
        UpdatableIntArray a = !pack ?
            mm.newUnresizableIntArray(arrayLength) :
            Arrays.asUpdatableFuncArray(LinearFunc.getUpdatableInstance(100, 0.1),
            UpdatableIntArray.class, mm.newUnresizableShortArray(arrayLength));
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Array created (%.5f seconds):%n    %s%n", (t2 - t1) * 1e-9, a);
        Array aView = a; //.subArr(0, 100); //.asImmutable();
        if (Arrays.isFuncArray(aView)) {
            System.out.printf(Locale.US,
                "The used function: %s (truncation mode: %s)%nThe underlying array:%n    %s%n",
                Arrays.getFunc(aView), Arrays.getTruncationMode(aView), Arrays.getUnderlyingArrays(aView)[0]);
        }
        System.out.println();

        double tFillMin = Double.POSITIVE_INFINITY, tFillMax = 0, tFillMean = 0.0;
        double tSumMin = Double.POSITIVE_INFINITY, tSumMax = 0, tSumMean = 0.0;
        double tFillBufferedMin = Double.POSITIVE_INFINITY, tFillBufferedMax = 0, tFillBufferedMean = 0.0;
        double tSumBufferedMin = Double.POSITIVE_INFINITY, tSumBufferedMax = 0, tSumBufferedMean = 0.0;
        for (int count = 1; count <= numberOfIterations; count++) {
            t1 = System.nanoTime();
            myFill(a, count);
            t2 = System.nanoTime();
            int sum1 = direct ? mySumDirect(a) : mySum(a);
            t3 = System.nanoTime();
            myFillBuffered(a, count);
            t4 = System.nanoTime();
            int sum2 = mySumBuffered(a);
            t5 = System.nanoTime();
            double tFill = t2 - t1, tSum = t3 - t2, tFillBuffered = t4 - t3, tSumBuffered = t5 - t4;
            tFillMin = Math.min(tFill, tFillMin);
            tSumMin = Math.min(tSum, tSumMin);
            tFillBufferedMin = Math.min(tFillBuffered, tFillBufferedMin);
            tSumBufferedMin = Math.min(tSumBuffered, tSumBufferedMin);
            tFillMax = Math.max(tFill, tFillMax);
            tSumMax = Math.max(tSum, tSumMax);
            tFillBufferedMax = Math.max(tFillBuffered, tFillBufferedMax);
            tSumBufferedMax = Math.max(tSumBuffered, tSumBufferedMax);
            tFillMean += tFill;
            tSumMean += tSum;
            tFillBufferedMean += tFillBuffered;
            tSumBufferedMean += tSumBuffered;
            System.out.printf(Locale.US, "%d: array filled/summed in "
                + "%.1f/%.1f ms, buffer %.1f/%.1f ms (%.2f/%.2f, %.2f/%.2f ns/element), "
                + "sum = %-20d\r", count,
                tFill * 1e-6, tSum * 1e-6, tFillBuffered * 1e-6, tSumBuffered * 1e-6,
                tFill / arrayLength, tSum / arrayLength, tFillBuffered / arrayLength, tSumBuffered / arrayLength,
                sum1);
            if (sum1 != sum2)
                throw new AssertionError("BUG FOUND: illegal sums");
        }
        tFillMean /= numberOfIterations;
        tSumMean /= numberOfIterations;
        tFillBufferedMean /= numberOfIterations;
        tSumBufferedMean /= numberOfIterations;
        System.out.printf(Locale.US,
            "%n%nMinimal times of filling / summing: %.3f / %.3f seconds, %.3f / %.3f ns/element)%n",
            tFillMin * 1e-9, tSumMin * 1e-9, tFillMin / arrayLength, tSumMin / arrayLength);
        System.out.printf(Locale.US,
            "Minimal times with buffer:          %.3f / %.3f seconds, %.3f / %.3f ns/element)%n",
            tFillBufferedMin * 1e-9, tSumBufferedMin * 1e-9,
            tFillBufferedMin / arrayLength, tSumBufferedMin / arrayLength);
        System.out.printf(Locale.US,
            "Average times of filling / summing: %.3f / %.3f seconds, %.3f / %.3f ns/element)%n",
            tFillMean * 1e-9, tSumMean * 1e-9, tFillMean / arrayLength, tSumMean / arrayLength);
        System.out.printf(Locale.US,
            "Average times of with buffer:       %.3f / %.3f seconds, %.3f / %.3f ns/element)%n",
            tFillBufferedMean * 1e-9, tSumBufferedMean * 1e-9,
            tFillBufferedMean / arrayLength, tSumBufferedMean / arrayLength);
        System.out.printf(Locale.US,
            "Maximal times of filling / summing: %.3f / %.3f seconds, %.3f / %.3f ns/element)%n",
            tFillMax * 1e-9, tSumMax * 1e-9, tFillMax / arrayLength, tSumMax / arrayLength);
        System.out.printf(Locale.US,
            "Maximal times with buffer:          %.3f / %.3f seconds, %.3f / %.3f ns/element)%n",
            tFillBufferedMax * 1e-9, tSumBufferedMax * 1e-9,
            tFillBufferedMax / arrayLength, tSumBufferedMax / arrayLength);
        System.out.printf(Locale.US,
            "Resulting array:%n    %s%n    %s%n",
            a, Arrays.toString(a, ", ", 80));

        a.freeResources(null); // allows garbage collection
        if (Arrays.gcAndAwaitFinalization(5000))
            System.out.println("Finalization complete");
    }
}
