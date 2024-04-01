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
import net.algart.arrays.PackedBitArrays;
import net.algart.arrays.PackedBitArraysPer8;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * <p>Basic test for {@link PackedBitArraysPer8} class.</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitArraysPer8Test {
    private static long getBitsSimple(byte[] src, long srcPos, int count) {
        long r = 0;
        for (int k = 0; k < count; k++) {
            if (srcPos + k >= 8 * (long) src.length) {
                break;
            }
            r |= (PackedBitArraysPer8.getBit(src, srcPos + k) ? 1L : 0L) << k;
        }
        return r;
    }

    private static long getBitsSimpleInReverseOrder(byte[] src, long srcPos, int count) {
        long r = 0;
        for (int k = 0; k < count; k++) {
            if (srcPos + k >= 8 * (long) src.length) {
                break;
            }
            r |= (PackedBitArraysPer8.getBitInReverseOrder(src, srcPos + k) ? 1L : 0L) << (count - 1 - k);
        }
        return r;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + PackedBitArraysPer8Test.class.getName()
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
        Random rnd = new Random(seed);
        System.out.println("Testing " + len + " bits with start random seed " + seed);

        for (int densityIndex = 0; densityIndex < 25; densityIndex++) {
            double density = rnd.nextDouble();
            if (densityIndex == 0) {
                density = 0.0;
            } else if (densityIndex == 1) {
                density = 1.0;
            }
            System.out.println("           ");
            System.out.println("Test #" + (densityIndex + 1) + "/25 with density " + density);
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
                if (!bSrc[k]) {
                    continue;
                }
                cardCorrect++;
            }

            long longPackedLen = PackedBitArraysPer8.packedLength(len);
            int packedLen = (int) longPackedLen;
            if (packedLen != longPackedLen)
                throw new IllegalArgumentException("Too large bit array (>2^34-8 bits)");
            byte[] pSrc = new byte[packedLen];
            byte[] pDest = new byte[packedLen];

            System.out.println("Packing source bits... ");
            PackedBitArraysPer8.packBits(pSrc, 0, bSrc, 0, len);
            System.out.println("Packing target bits...");
            PackedBitArraysPer8.packBits(pDest, 0, bDest, 0, len);
            byte[] pSrcWork = pSrc.clone();
            byte[] pDestWork = pDest.clone();
            Runtime rt = Runtime.getRuntime();
            System.out.printf(Locale.US, "Used memory (for all test arrays): %.3f MB%n",
                    (rt.totalMemory() - rt.freeMemory()) / 1048576.0);

            long card = PackedBitArraysPer8.cardinality(pSrc, 0, len);
            if (card != cardCorrect)
                throw new AssertionError("The bug in cardinality found at start: " +
                        card + " instead of " + cardCorrect);
            System.out.println("Number of high source bits is " + card);
            System.out.println();

            System.out.println("Testing \"setBit/getBit\" methods...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                for (int k = 0; k < len; k++) {
                    if (bSrc[k] != PackedBitArraysPer8.getBit(pSrc, k)) {
                        throw new AssertionError("The bug A in getBit found in test #" + testCount
                                + ", error found at " + k);
                    }
                }
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                for (int k = 0; k < count; k++) {
                    PackedBitArraysPer8.setBit(pDestWork, destPos + k, bSrc[srcPos + k]);
                }
                for (int k = 0; k < count; k++) {
                    if (bSrc[srcPos + k] != PackedBitArraysPer8.getBit(pDestWork, destPos + k)) {
                        throw new AssertionError("The bug B in setBit found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"getBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                for (int k = 0; k < len; k++) {
                    boolean bTest = PackedBitArraysPer8.getBits(pSrc, k, 1) == 1;
                    boolean b = PackedBitArraysPer8.getBit(pSrc, k);
                    if (b != bTest) {
                        throw new AssertionError("The bug A in getBits found in test #" +
                                testCount + ", error found at " + k);
                    }
                }
                int srcPos = rnd.nextInt(len);
                int count = rnd.nextInt(65);
                long v = PackedBitArraysPer8.getBits(pSrc, srcPos, count);
                long simple = getBitsSimple(pSrc, srcPos, count);
                if (v != simple) {
                    throw new AssertionError("The bug getBits found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + v + " instead of " + simple);
                }
                PackedBitArraysTest.showProgress(testCount);
            }


            System.out.println("Testing \"reverseBitOrder\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                int destPos = rnd.nextInt(pDest.length + 1);
                int count = rnd.nextInt(pDest.length + 1 - destPos);
                PackedBitArraysPer8.reverseBitsOrderInEachByte(pDestWork, destPos, count);
                for (int k = 0; k < count; k++) {
                    if (pDestWork[destPos + k] != (byte) (Integer.reverse(pDest[destPos + k] & 0xFF) >>> 24)) {
                        throw new AssertionError("The bug A in reverseBitOrder found in test #" + testCount + ": "
                                + "destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysPer8.reverseBitsOrderInEachByte(pDestWork, destPos, count);
                if (!java.util.Arrays.equals(pDest, pDestWork)) {
                    throw new AssertionError("The bug B in reverseBitOrder found in test #" + testCount + ": "
                            + "destPos = " + destPos + ", count = " + count);
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"setBitInReverseOrder/getBitInReverseOrder\" methods...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pSrc, 0, pSrcWork, 0, pSrc.length);
                PackedBitArraysPer8.reverseBitsOrderInEachByte(pSrcWork);
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                for (int k = 0; k < len; k++) {
                    int reverseIndex = (k & ~7) + 7 - (k & 7);
                    boolean b = reverseIndex < bSrc.length && bSrc[reverseIndex];
                    boolean bTest = PackedBitArraysPer8.getBitInReverseOrder(pSrc, k);
                    if (b != bTest) {
                        throw new AssertionError("The bug A in getBitInReverseOrder found in test #" +
                                testCount + ", error found at " + k + ", reverse index " + reverseIndex);
                    }
                    if (bTest != PackedBitArraysPer8.getBit(pSrcWork, k)) {
                        throw new AssertionError("The bug B in getBit/getBitInReverseOrder found in test #" +
                                testCount + ", error found at " + k + ", reverse index " + reverseIndex);
                    }
                }
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                for (int k = 0; k < count; k++) {
                    PackedBitArraysPer8.setBitInReverseOrder(pDestWork, destPos + k,
                            PackedBitArraysPer8.getBitInReverseOrder(pSrc, srcPos + k));
                }
                for (int k = 0; k < count; k
                        ++) {
                    if (PackedBitArraysPer8.getBitInReverseOrder(pSrc, srcPos + k) !=
                            PackedBitArraysPer8.getBitInReverseOrder(pDestWork, destPos + k)) {
                        throw new AssertionError("The bug C in setBitInReverseOrder found in test #" + testCount +
                                ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"getBitsInReverseOrder\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                for (int k = 0; k < len; k++) {
                    boolean bTest = PackedBitArraysPer8.getBitsInReverseOrder(pSrc, k, 1) == 1;
                    boolean b = PackedBitArraysPer8.getBitInReverseOrder(pSrc, k);
                    if (b != bTest) {
                        throw new AssertionError("The bug A in getBitsInReverseOrder found in test #" +
                                testCount + ", error found at " + k);
                    }
                }
                System.arraycopy(pSrc, 0, pDestWork, 0, pDest.length);
                PackedBitArraysPer8.reverseBitsOrderInEachByte(pDestWork);
                int srcPos = rnd.nextInt(len);
                int count = rnd.nextInt(65);
                long vTest = PackedBitArraysPer8.getBitsInReverseOrder(pSrc, srcPos, count);
                long v = BitsUnpacker.getBits(pSrc, srcPos, count);
                if (vTest != v) {
                    throw new AssertionError("The bug in getBitsInReverseOrder found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + vTest + " instead of " + v);
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"packBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.packBits(pDestWork, destPos, bSrc, srcPos, count);
                for (int k = 0; k < count; k++)
                    if (bSrc[srcPos + k] != PackedBitArraysPer8.getBit(pDestWork, destPos + k)) {
                        throw new AssertionError("The bug A in packBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                for (int k = 0; k < destPos; k++)
                    if (PackedBitArraysPer8.getBit(pDestWork, k) != PackedBitArraysPer8.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                for (int k = destPos + count; k < len; k++)
                    if (PackedBitArraysPer8.getBit(pDestWork, k) != PackedBitArraysPer8.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
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
                PackedBitArraysPer8.copyBits(pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++) {
                    boolean bit = PackedBitArraysPer8.getBit(pDestWork, destPos + k);
                    if (bSrc[srcPos + k] != bit) {
                        throw new AssertionError("The bug A in copyBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + " (src=" + JArrays.toBinaryString(bSrc, "", 200)
                                + ", dest=" + JArrays.toBinaryString(bDest, "", 200)
                                + ", we have " + JArrays.toBinaryString(bDestWork1, "", 200)
                                + ", dest[" + (destPos + k) + "]=" + bit
                                + " instead of " + bSrc[srcPos + k] + ")");
                    }
                }
                System.arraycopy(bSrc, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug B in copyBits or unpackBits found in test #" + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k
                                + " (src=" + JArrays.toBinaryString(bSrc, "", 200)
                                + ", dest=" + JArrays.toBinaryString(bDest, "", 200)
                                + ", we have " + JArrays.toBinaryString(bDestWork1, "", 200)
                                + " instead of " + JArrays.toBinaryString(bDestWork2, "", 200) + ")");
                    }
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
                PackedBitArraysPer8.copyBits(pDestWork, destPos, pDestWork, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, destPos, pDestWork, destPos, count);
                System.arraycopy(bDestWork2, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in copyBits or unpackBits found in test #" + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
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
                PackedBitArraysPer8.fillBits(pDestWork, destPos, count, value);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] = value;
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in fillBits found in test #" + testCount + ": "
                                + "destPos = " + destPos + ", count = " + count + ", value = " + value
                                + ", error found at " + k);
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
                card = PackedBitArraysPer8.cardinality(pSrc, pos, pos + count);
                if (card != cardCorrect) {
                    throw new AssertionError("The bug in cardinality found in test #" + testCount + ": "
                            + card + " instead of " + cardCorrect);
                }
                PackedBitArraysTest.showProgress(testCount);
            }

            System.out.println("Testing \"toLongArray/toByteArray\" methods...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                int count = rnd.nextInt(len + 1);
                final int n = (int) PackedBitArraysPer8.packedLength(count);
                long[] longs1 = PackedBitArraysPer8.toLongArray(Arrays.copyOf(pDestWork, n));
                if (longs1.length != PackedBitArrays.packedLength(count)) {
                    throw new AssertionError("The bug in toLongArray() length");
                }
                for (int k = 0; k < count; k++) {
                    if (PackedBitArraysPer8.getBit(pDest, k) != PackedBitArrays.getBit(longs1, k)) {
                        throw new AssertionError("The bug A in toLongArray found in test #" + testCount + ": "
                                + "count = " + count + ", error found at " + k);
                    }
                }
                long[] longs2 = PackedBitArraysPer8.toLongArray(ByteBuffer.wrap(Arrays.copyOf(pDestWork, n)));
                if (!java.util.Arrays.equals(longs1, longs2)) {
                    throw new AssertionError("The bug B in toLongArray found in test #" + testCount + ": "
                            + "count = " + count);
                }
                byte[] bytes = PackedBitArraysPer8.toByteArray(longs1, count);
                if (!java.util.Arrays.equals(bytes, Arrays.copyOf(pDestWork, n))) {
                    throw new AssertionError("The bug C in toByteArray found in test #" + testCount + ": "
                            + "count = " + count);
                }
                PackedBitArraysTest.showProgress(testCount);
            }
        }
        System.out.println("           ");
        System.out.println("All O'k: testing time "
                + (System.currentTimeMillis() - PackedBitArraysTest.tStart) + " ms");
    }
}
