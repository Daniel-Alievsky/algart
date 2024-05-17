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

import net.algart.arrays.Arrays;
import net.algart.arrays.MemoryModel;
import net.algart.arrays.UpdatableBitArray;

import java.util.Random;

/**
 * <p>Test for multithreading usage of setBit</p>
 *
 * @author Daniel Alievsky
 */
public class SetBitMultithreadingTest {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
    // - for LargeMemoryModel, current AlgART version works correctly always
    private static final boolean UNSAFE = true;
    // - must be true to see AssertionError (when regionLength%64 !-= 0)

    private static void testBitArrayRegion(
            boolean[] data,
            UpdatableBitArray a,
            int offset,
            int length,
            boolean noSync) {
        final Random rnd = new Random(offset);
        for (int i = 0; i < 3 * length; i++) {
            final int where = rnd.nextInt(3);
            final int k = where == 0 ? 0 : where == 1 ? length - 1 : rnd.nextInt(length);
            if (noSync)
                // synchronized (a)
                // - uncommenting the previous synchronization restores stable behaviour
            {
                a.clearBitNoSync(offset + k);
                a.setBitNoSync(offset + k);
                a.setBitNoSync(offset + k, data[offset + k]);
                // We should not call setData here: this method perform synchronization, so all will work fine
            } else {
                a.clearBit(offset + k);
                a.setBit(offset + k);
                a.setBit(offset + k, data[offset + k]);
                if (k % 100 == 0) {
                    a.setData(offset, data, offset, length);
                }
            }
        }
    }

    private static void testThreads(
            boolean[] data,
            UpdatableBitArray a,
            int numberOfRegions,
            int regionLength,
            boolean unsafe) {
        Thread[] threads = new Thread[numberOfRegions];
        for (int i = 0; i < numberOfRegions; i++) {
            int rangeIndex = i;
            Thread thread = new Thread(() -> testBitArrayRegion(
                    data, a, rangeIndex * regionLength, regionLength, unsafe));
            thread.start();
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new InternalError(e);
            }
        }
        boolean[] resultData = a.ja();
        for (int i = 0; i < resultData.length; i++) {
            if (resultData[i] != data[i]) {
                throw new AssertionError("Data corrupted at " + i);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 3) {
            System.out.println("Usage: " + SetBitMultithreadingTest.class.getName()
                + " regionLength numberOfRegions numberOfTests");
            System.out.println("regionLength must be not divisible by 64 to see effect of unsafe mode.");
            return;
        }

        final int regionLength = Integer.parseInt(args[0]);
        final int numberOfRegions = Integer.parseInt(args[1]);
        final int numberOfTests = Integer.parseInt(args[2]);

        System.out.println(mm.toString());
        System.out.printf("Splitting large array to %d regions per %d bits%n", numberOfRegions, regionLength);

        Random rnd = new Random(92);
        boolean[] testData = new boolean[numberOfRegions * regionLength];
        for (int i = 0; i < regionLength; i++) {
            testData[i] = rnd.nextBoolean();
        }
        UpdatableBitArray a = mm.newUnresizableBitArray(testData.length);
        a.setData(0, testData);

        for (int test = 1; test <= numberOfTests; test++) {
            System.out.printf("\rTest #%d... ", test);
            testThreads(testData, a, numberOfRegions, regionLength, false);
        }
        System.out.println();
        if (regionLength % 64 == 0) {
            System.out.println("Synchronization not necessary: region length % 64 == 0");
        } else if (UNSAFE) {
            System.out.println("DANGEROUS: behaviour should be unstable, AssertionError is probable");
        }
        if (regionLength % 64 == 0 || UNSAFE) {
            // - For even lengths, unsafe version should also work
            for (int test = 1; test <= numberOfTests; test++) {
                System.out.printf("\rTest no-sync #%d... ", test);
                testThreads(testData, a, numberOfRegions, regionLength, true);
            }
        }
        System.out.printf("%nO'k");
        a.freeResources();
        System.gc();
        System.gc();
    }
}
