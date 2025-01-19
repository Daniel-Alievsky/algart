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

package net.algart.arrays.demo;

import net.algart.arrays.JArrays;
import net.algart.arrays.PackedBitArrays;
import net.algart.arrays.PackedBitBuffers;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Locale;
import java.util.Random;

/**
 * <p>Basic test for {@link PackedBitBuffers} class</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitBuffersTest {
    private static long getBits64Simple(LongBuffer src, long srcPos, int count) {
        long result = 0;
        for (int k = 0; k < count; k++) {
            if (srcPos + k >= 64 * (long) src.limit()) {
                break;
            }
            final long bit = PackedBitBuffers.getBit(src, srcPos + k) ? 1L : 0L;
            result |= bit << k;
        }
        return result;
    }

    private static void setBits64Simple(LongBuffer dest, long destPos, long bits, int count) {
        for (int k = 0; k < count; k++) {
            final long bit = (bits >>> k) & 1L;
            if (destPos + k >= 64 * (long) dest.limit()) {
                break;
            }
            PackedBitBuffers.setBit(dest, destPos + k, bit != 0);
        }
    }

    public static void main(String[] args) {
        boolean superLarge = false;
        int startArgIndex = 0;
        if (args.length > 0 && args[startArgIndex].equalsIgnoreCase("-SuperLarge")) {
            superLarge = true;
            startArgIndex++;
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
        if (superLarge) {
            startOffset = 2333333333L;
        }

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
            for (int k = 0; k < bSrc.length; k++) {
                bSrc[k] = rnd.nextDouble() <= density;
            }
            boolean[] bDest = new boolean[len];
            for (int k = 0; k < bDest.length; k++) {
                bDest[k] = rnd.nextDouble() <= density;
            }
            boolean[] bDestWork1 = bDest.clone();
            boolean[] bDestWork2 = bDest.clone();
            int cardCorrect = 0;
            for (int k = 0; k < len; k++) {
                if (bSrc[k]) {
                    cardCorrect++;
                }
            }

            long longPackedLen = PackedBitBuffers.packedLength(startOffset + len);
            int packedLen = (int) longPackedLen;
            if (packedLen != longPackedLen) {
                throw new IllegalArgumentException("Too large bit array (>2^37-64 bits)");
            }
            LongBuffer pSrc = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();
            LongBuffer pDest = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();

            System.out.println("Packing source bits... ");
            PackedBitBuffers.packBits(pSrc, startOffset, bSrc, 0, len);
            System.out.println("Packing target bits...");
            PackedBitBuffers.packBits(pDest, startOffset, bDest, 0, len);
            LongBuffer pDestWork1 = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();
            pDestWork1.put(pDest);
            LongBuffer pDestWork2 = ByteBuffer.allocateDirect(packedLen * 8).asLongBuffer();
            pDestWork2.put(pDest);
            long[] pDestWorkJA = new long[packedLen];
            Runtime rt = Runtime.getRuntime();
            System.out.printf(Locale.US, "Used memory (for all test arrays and buffers): %.3f MB%n",
                    (rt.totalMemory() - rt.freeMemory()) / 1048576.0);

            long card = PackedBitBuffers.cardinality(pSrc, startOffset, startOffset + len);
            if (card != cardCorrect) {
                throw new AssertionError("The bug in cardinality found at start: " + card +
                        " instead of " + cardCorrect);
            }
            System.out.println("Number of high source bits is " + card);
            System.out.println();

            System.out.println("Testing \"setBit/getBit\" methods...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDestWork1.position(0);
                pDestWork1.put(pDest);
                for (int k = 0; k < len; k++) {
                    if (bSrc[k] != PackedBitBuffers.getBit(pSrc, k)) {
                        throw new AssertionError("The bug A in getBit found in test #" + testCount
                                + ", error found at " + k);
                    }
                }
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                for (int k = 0; k < count; k++) {
                    PackedBitBuffers.setBit(pDestWork1, destPos + k, bSrc[srcPos + k]);
                }
                for (int k = 0; k < count; k++) {
                    if (bSrc[srcPos + k] != PackedBitBuffers.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug B in setBit found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                pDest.position(0);
                pDestWork1.position(0);
                pDestWork1.put(pDest);
                for (int k = 0; k < count; k++) {
                    PackedBitBuffers.setBitNoSync(pDestWork1, destPos + k, bSrc[srcPos + k]);
                }
                for (int k = 0; k < count; k++) {
                    if (bSrc[srcPos + k] != PackedBitBuffers.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug C in setBitNoSync found in test #" +
                                testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count +
                                ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"getBits64\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                for (int k = 0; k < len; k++) {
                    boolean bTest = PackedBitBuffers.getBits64(pSrc, startOffset + k, 1) == 1;
                    boolean b = PackedBitBuffers.getBit(pSrc, startOffset + k);
                    if (b != bTest) {
                        throw new AssertionError("The bug A in getBits64 found in test #" +
                                testCount + ", error found at " + k);
                    }
                }
                int srcPos = rnd.nextInt(len + 100);
                int count = rnd.nextInt(65);
                long vTest = PackedBitBuffers.getBits64(pSrc, startOffset + srcPos, count);
                long v = getBits64Simple(pSrc, startOffset + srcPos, count);
                if (vTest != v) {
                    throw new AssertionError("The bug B in getBits64 found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + Long.toBinaryString(vTest) + " instead of " + Long.toBinaryString(v));
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"setBits64\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDestWork1.position(0);
                pDestWork1.put(pDest);
                pDest.position(0);
                pDestWork2.position(0);
                pDestWork2.put(pDest);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(65);
                long v = PackedBitBuffers.getBits64(pSrc, srcPos, count);
                if (count < 64 && v != (v & ((1L << count) - 1))) {
                    throw new AssertionError(Long.toBinaryString(v));
                }
                boolean sync = rnd.nextBoolean();
                setBits64Simple(pDestWork1, destPos, v, count);
                if (sync) {
                    PackedBitBuffers.setBits64(pDestWork2, destPos, v, count);
                } else {
                    PackedBitBuffers.setBits64NoSync(pDestWork2, destPos, v, count);
                }
                long vTest = getBits64Simple(pDestWork2,destPos, count);
                if (destPos + count <= len && vTest != v) {
                    throw new AssertionError("The bug A in setBits64 found in test #" + testCount +
                            ": destPos = " + destPos + ", count = " + count + ", " +
                            Long.toBinaryString(vTest) + " instead of " + Long.toBinaryString(v) +
                            ", " + (sync ? "" : "no-sync version"));
                }
                for (int k = 0; k < pDestWork1.limit(); k++) {
                    if (pDestWork1.get(k) != pDestWork2.get(k)) {
                        throw new AssertionError("The bug B in setBits64 " +
                                "found in test #" + testCount +
                                ", " + (sync ? "" : "no-sync version"));
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            if (startOffset == 0) {
                System.out.println("Testing \"packBits\" method...");
                for (int testCount = 0; testCount < numberOfTests; testCount++) {
                    pDest.position(0);
                    pDestWork1.position(0);
                    pDestWork1.put(pDest);
                    int srcPos = rnd.nextInt(len + 1);
                    int destPos = rnd.nextInt(len + 1);
                    int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                    PackedBitBuffers.packBits(pDestWork1, destPos, bSrc, srcPos, count);
                    for (int k = 0; k < count; k++) {
                        if (bSrc[srcPos + k] != PackedBitBuffers.getBit(pDestWork1, destPos + k)) {
                            throw new AssertionError("The bug A in packBits found in test #" +
                                    testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " +
                                    count + ", error found at " + k);
                        }
                    }
                    for (int k = 0; k < destPos; k++) {
                        if (PackedBitBuffers.getBit(pDestWork1, k) != PackedBitBuffers.getBit(pDest, k)) {
                            throw new AssertionError("The bug B in packBits found in test #" +
                                    testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " +
                                    count + ", error found at " + k);
                        }
                    }
                    for (int k = destPos + count; k < len; k++) {
                        if (PackedBitBuffers.getBit(pDestWork1, k) != PackedBitBuffers.getBit(pDest, k)) {
                            throw new AssertionError("The bug C in packBits found in test #" +
                                    testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " +
                                    count + ", error found at " + k);
                        }
                    }
                    PackedBitArraysTest.showProgress(testCount);
                }
            }

            System.out.println("Testing \"copyBits\" method, two different buffers, standard or reverse order...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDestWork1.position(0);
                pDestWork1.put(pDest);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                boolean reverseOrder = rnd.nextBoolean();
                PackedBitBuffers.copyBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count,
                        reverseOrder);
                PackedBitBuffers.unpackBits(bDestWork1, destPos, pDestWork1, startOffset + destPos, count);
                System.arraycopy(bSrc, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" +
                                testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count +
                                ", reverseOrder = " + reverseOrder + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"copyBits\" method, inside a single buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDestWork1.position(0);
                pDestWork1.put(pDest);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.copyBits(
                        pDestWork1, startOffset + destPos, pDestWork1, startOffset + srcPos, count);
                PackedBitBuffers.unpackBits(bDestWork1, destPos, pDestWork1, startOffset + destPos, count);
                System.arraycopy(bDestWork2, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" +
                                testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count +
                                ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"fillBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDestWork1.position(0);
                pDestWork1.put(pDest);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - destPos);
                boolean value = rnd.nextBoolean();
                PackedBitBuffers.fillBits(pDestWork1, startOffset + destPos, count, value);
                PackedBitBuffers.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] = value;
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in fillBits found in test #" +
                                testCount + ": "
                                + "destPos = " + destPos + ", count = " + count + ", value = " + value
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"notBits\" method, array &= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDest.get(pDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.notBits(
                        pDestWorkJA, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] = !bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in notBits found in test #" +
                                testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"andBits\" method, array &= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDest.get(pDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.andBits(
                        pDestWorkJA, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] &= bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in andBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"orBits\" method, array |= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDest.get(pDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.orBits(
                        pDestWorkJA, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] |= bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in orBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"xorBits\" method, array ^= buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDest.get(pDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.xorBits(
                        pDestWorkJA, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] ^= bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in xorBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"andNotBits\" method, array &= ~buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDest.get(pDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.andNotBits(
                        pDestWorkJA, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] &= !bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in andNotBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"orNotBits\" method, array |= ~buffer...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                pDest.position(0);
                pDest.get(pDestWorkJA);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitBuffers.orNotBits(
                        pDestWorkJA, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWorkJA, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] |= !bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in orNotBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"indexOfBit\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int lowIndex = rnd.nextInt(len + 1);
                int highIndex = rnd.nextInt(len + 1);
                boolean value = rnd.nextBoolean();
                long indexCorrect = JArrays.indexOfBoolean(bSrc, lowIndex, highIndex, value);
                if (indexCorrect != -1) {
                    indexCorrect += startOffset;
                }
                long index = PackedBitBuffers.indexOfBit(
                        pSrc, startOffset + lowIndex, startOffset + highIndex, value);
                if (index != indexCorrect) {
                    throw new AssertionError("The bug in indexOfBit found in test #" + testCount + ": "
                            + index + " instead of " + indexCorrect
                            + ", fromIndex=" + lowIndex + ", toIndex=" + highIndex + ", value=" + value);
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"lastIndexOfBit\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int lowIndex = rnd.nextInt(len + 1);
                int highIndex = rnd.nextInt(len + 1);
                boolean value = rnd.nextBoolean();
                long indexCorrect = JArrays.lastIndexOfBoolean(bSrc, lowIndex, highIndex, value);
                if (indexCorrect != -1) {
                    indexCorrect += startOffset;
                }
                long index = PackedBitBuffers.lastIndexOfBit(pSrc,
                        startOffset + lowIndex, startOffset + highIndex, value);
                if (index != indexCorrect) {
                    throw new AssertionError("The bug in lastIndexOfBit found in test #" + testCount + ": "
                            + index + " instead of " + indexCorrect
                            + ", fromIndex=" + lowIndex + ", toIndex=" + highIndex + ", value=" + value);
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"cardinality\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int pos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - pos);
                cardCorrect = 0;
                for (int k = 0; k < count; k++) {
                    if (bSrc[pos + k]) {
                        cardCorrect++;
                    }
                }
                card = PackedBitBuffers.cardinality(
                        pSrc, startOffset + pos, startOffset + pos + count);
                if (card != cardCorrect) {
                    throw new AssertionError("The bug in cardinality found in test #" + testCount + ": "
                            + card + " instead of " + cardCorrect);
                }
                PackedBitArraysTest.showProgress(testCount);
            }
        }
        System.out.println("           ");
        System.out.println("All O'k: testing time "
                + (System.currentTimeMillis() - PackedBitArraysTest.tStart) + " ms");
    }
}
