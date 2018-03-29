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
import java.nio.*;
import net.algart.arrays.*;

/**
 * <p>Basic test for {@link PackedBitBuffers} class</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.4
 */
public class PackedBitBuffersTest {
    public static void main(String[] args) {
        boolean superLarge = false;
        int startArgIndex = 0;
        if (args.length > 0 && args[startArgIndex].equalsIgnoreCase("-SuperLarge")) {
            superLarge = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage: " + PackedBitBuffersTest.class.getName()
                + " [-SuperLarge] arrayLength numberOfTests [randSeed]");
            System.out.println("-SuperLarge key will perform test for bit buffers after 2^32 index (>512 MB");
            return;
        }

        int len = Integer.parseInt(args[startArgIndex]);
        int numberOfTests = Integer.parseInt(args[startArgIndex + 1]);
        long seed;
        if (args.length < startArgIndex + 3) {
            seed = new Random().nextLong();
        } else {
            seed = Long.parseLong(args[startArgIndex + 2]);
        }
        long startOffset = 0;
        if (superLarge)
            startOffset = 2333333333L;

        Random rnd = new Random(seed);
        System.out.println("Testing " + len + " bits with start random seed " + seed);

        for (int densityIndex = 0; densityIndex < 50; densityIndex++) {
            double density = rnd.nextDouble();
            if (densityIndex == 0)
                density = 0.0;
            else if (densityIndex == 1)
                density = 1.0;
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

            long longPackedLen = PackedBitArrays.packedLength(startOffset + len);
            int packedLen = (int)longPackedLen;
            if (packedLen != longPackedLen)
                throw new IllegalArgumentException("Too large bit array (>2^37-64 bits)");
            LongBuffer lSrc = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();
            LongBuffer lDest = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();

            System.out.println("Packing source bits... ");
            PackedBitBuffers.packBits(lSrc, startOffset, bSrc, 0, len);
            System.out.println("Packing target bits...");
            PackedBitBuffers.packBits(lDest, startOffset, bDest, 0, len);
            LongBuffer lDestWork = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();
            lDestWork.put(lDest);
            long[] lDestWorkJA = new long[packedLen];
            Runtime rt = Runtime.getRuntime();
            System.out.printf(Locale.US, "Used memory (for all test arrays and buffers): %.3f MB%n",
                (rt.totalMemory() - rt.freeMemory()) / 1048576.0);

            long card = PackedBitBuffers.cardinality(lSrc, startOffset, startOffset + len);
            if (card != cardCorrect)
                throw new AssertionError("The bug in cardinality found at start: " + card + " instead of " + cardCorrect);
            System.out.println("Number of high source bits is " + card);
            System.out.println();

            if (startOffset == 0) {
                System.out.println("Testing \"packBits\" method...");
                for (int testCount = 0; testCount < numberOfTests; testCount++) {
                    lDest.position(0); lDestWork.position(0); lDestWork.put(lDest);
                    int srcPos = rnd.nextInt(len + 1);
                    int destPos = rnd.nextInt(len + 1);
                    int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                    PackedBitBuffers.packBits(lDestWork, destPos, bSrc, srcPos, count);
                    for (int k = 0; k < count; k++)
                        if (bSrc[srcPos + k] != PackedBitBuffers.getBit(lDestWork, destPos + k))
                            throw new AssertionError("The bug A in packBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    for (int k = 0; k < destPos; k++)
                        if (PackedBitBuffers.getBit(lDestWork, k) != PackedBitBuffers.getBit(lDest, k))
                            throw new AssertionError("The bug B in packBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    for (int k = destPos + count; k < len; k++)
                        if (PackedBitBuffers.getBit(lDestWork, k) != PackedBitBuffers.getBit(lDest, k))
                            throw new AssertionError("The bug C in packBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    PackedBitArraysTest.showProgress(testCount);
                }
            }

            System.out.println("Testing \"copyBits\" method, two different buffers, standard or reverse order...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDestWork.position(0); lDestWork.put(lDest);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                boolean reverseOrder = rnd.nextBoolean();
                PackedBitBuffers.copyBits(lDestWork, startOffset + destPos, lSrc, startOffset + srcPos, count,
                    reverseOrder);
                PackedBitBuffers.unpackBits(bDestWork1, destPos, lDestWork, startOffset + destPos, count);
                System.arraycopy(bSrc, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", reverseOrder = " + reverseOrder
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"copyBits\" method, inside a single buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDestWork.position(0); lDestWork.put(lDest);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.copyBits(lDestWork, startOffset + destPos, lDestWork, startOffset + srcPos, count);
                PackedBitBuffers.unpackBits(bDestWork1, destPos, lDestWork, startOffset + destPos, count);
                System.arraycopy(bDestWork2, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"fillBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDestWork.position(0); lDestWork.put(lDest);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - destPos);
                boolean value = rnd.nextBoolean();
                PackedBitBuffers.fillBits(lDestWork, startOffset + destPos, count, value);
                PackedBitBuffers.unpackBits(bDestWork1, 0, lDestWork, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] = value;
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in fillBits found in test #" + testCount + ": "
                            + "destPos = " + destPos + ", count = " + count + ", value = " + value
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"notBits\" method, array &= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDest.get(lDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.notBits(lDestWorkJA, startOffset + destPos, lSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, lDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] = !bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in notBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"andBits\" method, array &= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDest.get(lDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.andBits(lDestWorkJA, startOffset + destPos, lSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, lDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] &= bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in andBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"orBits\" method, array |= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDest.get(lDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.orBits(lDestWorkJA, startOffset + destPos, lSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, lDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] |= bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in orBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"xorBits\" method, array ^= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDest.get(lDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.xorBits(lDestWorkJA, startOffset + destPos, lSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, lDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] ^= bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in xorBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"andNotBits\" method, array &= ~buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDest.get(lDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.andNotBits(lDestWorkJA, startOffset + destPos, lSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, lDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] &= !bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in andNotBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"orNotBits\" method, array |= ~buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                lDest.position(0); lDest.get(lDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.orNotBits(lDestWorkJA, startOffset + destPos, lSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, lDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] |= !bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in orNotBits found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"indexOfBit\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int lowIndex = rnd.nextInt(len + 1);
                int highIndex = rnd.nextInt(len + 1);
                boolean value = rnd.nextBoolean();
                long indexCorrect = JArrays.indexOfBoolean(bSrc, lowIndex, highIndex, value);
                if (indexCorrect != -1)
                    indexCorrect += startOffset;
                long index = PackedBitBuffers.indexOfBit(lSrc, startOffset + lowIndex, startOffset + highIndex, value);
                if (index != indexCorrect)
                    throw new AssertionError("The bug in indexOfBit found in test #" + testCount + ": "
                        + index + " instead of " + indexCorrect
                        + ", fromIndex=" + lowIndex + ", toIndex=" + highIndex + ", value=" + value);
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"lastIndexOfBit\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int lowIndex = rnd.nextInt(len + 1);
                int highIndex = rnd.nextInt(len + 1);
                boolean value = rnd.nextBoolean();
                long indexCorrect = JArrays.lastIndexOfBoolean(bSrc, lowIndex, highIndex, value);
                if (indexCorrect != -1)
                    indexCorrect += startOffset;
                long index = PackedBitBuffers.lastIndexOfBit(lSrc,
                    startOffset + lowIndex, startOffset + highIndex, value);
                if (index != indexCorrect)
                    throw new AssertionError("The bug in lastIndexOfBit found in test #" + testCount + ": "
                        + index + " instead of " + indexCorrect
                        + ", fromIndex=" + lowIndex + ", toIndex=" + highIndex + ", value=" + value);
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
                card = PackedBitBuffers.cardinality(lSrc, startOffset + pos, startOffset + pos + count);
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
