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

import net.algart.arrays.JArrays;
import net.algart.arrays.PackedBitArraysPer8;

import java.util.Locale;
import java.util.Random;

/**
 * <p>Basic test for {@link PackedBitArraysPer8} class</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitArraysTestPer8 {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + PackedBitArraysTestPer8.class.getName()
                + " arrayLength numberOfTests [randSeed]");
            return;
        }

        int len = Integer.parseInt(args[0]);
        int numberOfTests = Integer.parseInt(args[1]);
        long seed;
        if (args.length < 3) {
            seed = new Random().nextLong();
        } else {
            seed = Long.parseLong(args[2]);
        }
        long startOffset = 0;
        Random rnd = new Random(seed);
        System.out.println("Testing " + len + " bits with start random seed " + seed);

        for (int densityIndex = 0; densityIndex < 50; densityIndex++) {
            double density = rnd.nextDouble();
            if (densityIndex == 0) {
                density = 0.0;
            } else if (densityIndex == 1) {
                density = 1.0;
            }
            System.out.println("           ");
            System.out.println("Test #" + (densityIndex + 1) + "/50 with density " + density);
            System.out.println("Allocating memory... ");
            boolean[] bSrc = new boolean[len];
            for (int k = 0; k < bSrc.length; k++)
                bSrc[k] = rnd.nextDouble() <= density;
            boolean[] bDest = new boolean[len];
            for (int k = 0; k < bDest.length; k++)
                bDest[k] = rnd.nextDouble() <= density;
            boolean[] bDestWork1 = bDest.clone();
            boolean[] bDestWork2 = bDest.clone();
            int cardCorrect = 0;
            for (int k = 0; k < len; k++)
                if (bSrc[k])
                    cardCorrect++;

            long longPackedLen = PackedBitArraysPer8.packedLength(startOffset + len);
            int packedLen = (int)longPackedLen;
            if (packedLen != longPackedLen)
                throw new IllegalArgumentException("Too large bit array (>2^34-8 bits)");
            byte[] pSrc = new byte[packedLen];
            byte[] pDest = new byte[packedLen];

            System.out.println("Packing source bits... ");
            PackedBitArraysPer8.packBits(pSrc, startOffset, bSrc, 0, len);
            System.out.println("Packing target bits...");
            PackedBitArraysPer8.packBits(pDest, startOffset, bDest, 0, len);
            byte[] pDestWork = pDest.clone();
            Runtime rt = Runtime.getRuntime();
            System.out.printf(Locale.US, "Used memory (for all test arrays): %.3f MB%n",
                (rt.totalMemory() - rt.freeMemory()) / 1048576.0);

            long card = PackedBitArraysPer8.cardinality(pSrc, startOffset, startOffset + len);
            if (card != cardCorrect)
                throw new AssertionError("The bug in cardinality found at start: " +
                        card + " instead of " + cardCorrect);
            System.out.println("Number of high source bits is " + card);
            System.out.println();

            System.out.println("Testing \"packBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.packBits(pDestWork, destPos, bSrc, srcPos, count);
                for (int k = 0; k < count; k++)
                    if (bSrc[srcPos + k] != PackedBitArraysPer8.getBit(pDestWork, destPos + k))
                        throw new AssertionError("The bug A in packBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                for (int k = 0; k < destPos; k++)
                    if (PackedBitArraysPer8.getBit(pDestWork, k) != PackedBitArraysPer8.getBit(pDest, k))
                        throw new AssertionError("The bug B in packBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                for (int k = destPos + count; k < len; k++)
                    if (PackedBitArraysPer8.getBit(pDestWork, k) != PackedBitArraysPer8.getBit(pDest, k))
                        throw new AssertionError("The bug C in packBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }
            System.out.println("Testing \"copyBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.copyBits(
                        pDestWork, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, startOffset, len);
                System.arraycopy(bSrc, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" + testCount
                            + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k
                            + " (src=" + JArrays.toBinaryString(bSrc, "", 200)
                            + ", dest=" + JArrays.toBinaryString(bDest, "", 200)
                            + ", we have " + JArrays.toBinaryString(bDestWork1, "", 200)
                            + " instead of " + JArrays.toBinaryString(bDestWork2, "", 200) + ")");
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"copyBits\" + \"unpackBits\" method, inside a single array...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.copyBits(
                        pDestWork, startOffset + destPos, pDestWork, startOffset + srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, destPos, pDestWork, startOffset + destPos, count);
                System.arraycopy(bDestWork2, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" + testCount
                            + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"fillBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - destPos);
                boolean value = rnd.nextBoolean();
                PackedBitArraysPer8.fillBits(pDestWork, startOffset + destPos, count, value);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] = value;
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in fillBits found in test #" + testCount + ": "
                            + "destPos = " + destPos + ", count = " + count + ", value = " + value
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"cardinality\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int pos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - pos);
                cardCorrect = 0;
                for (int k = 0; k < count; k++)
                    if (bSrc[pos + k])
                        cardCorrect++;
                card = PackedBitArraysPer8.cardinality(
                        pSrc, startOffset + pos, startOffset + pos + count);
                if (card != cardCorrect)
                    throw new AssertionError("The bug in cardinality found in test #" + testCount + ": "
                        + card + " instead of " + cardCorrect);
                PackedBitArraysTest.showProgress(testCount);
            }
        }
        System.out.println("           ");
        System.out.println("All O'k: testing time "
                + (System.currentTimeMillis() - PackedBitArraysTest.tStart) + " ms");
    }
}
