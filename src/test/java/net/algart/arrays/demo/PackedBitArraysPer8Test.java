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
    private static final float FLOAT_BIT_0 = -1f, FLOAT_BIT_1 = 157f;

    private static long simpleBits(byte[] src, long srcPos, int count) {
        long result = 0;
        for (int k = 0; k < count; k++) {
            if (srcPos + k >= 8 * (long) src.length) {
                break;
            }
            final long bit = PackedBitArraysPer8.getBit(src, srcPos + k) ? 1L : 0L;
            result |= bit << k;
        }
        return result;
    }

    private static long simpleBitsInReverseOrder(byte[] src, long srcPos, int count) {
        long result = 0;
        for (int k = 0; k < count; k++) {
            if (srcPos + k < 8 * (long) src.length) {
                final long bit = PackedBitArraysPer8.getBitInReverseOrder(src, srcPos + k) ? 1L : 0L;
                result |= bit << (count - 1 - k);
            }
        }
        return result;
    }

    static void showProgress(int testCount) {
        PackedBitArraysTest.showProgress(testCount);
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
            float[] fDest = new float[len];
            for (int k = 0; k < bDest.length; k++) {
                fDest[k] = rnd.nextFloat();
            }
            boolean[] bDestWork1 = bDest.clone();
            boolean[] bDestWork2 = bDest.clone();
            float[] fDestWork = fDest.clone();
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
                showProgress(testCount);
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
                showProgress(testCount);
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
                showProgress(testCount);
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
                long vTest = PackedBitArraysPer8.getBits(pSrc, srcPos, count);
                long v = simpleBits(pSrc, srcPos, count);
                if (vTest != v) {
                    throw new AssertionError("The bug B in getBits found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + Long.toBinaryString(vTest) + " instead of " + Long.toBinaryString(v));
                }
                showProgress(testCount);
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
                int srcPos = rnd.nextInt(len);
                int count = rnd.nextInt(65);
                long vTest = PackedBitArraysPer8.getBitsInReverseOrder(pSrc, srcPos, count);
                long v = simpleBitsInReverseOrder(pSrc, srcPos, count);
                if (vTest != v) {
                    throw new AssertionError("The bug B in getBitsInReverseOrder found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + Long.toBinaryString(vTest) + " instead of " + Long.toBinaryString(v));
                }

                System.arraycopy(pSrc, 0, pDestWork, 0, pDest.length);
                PackedBitArraysPer8.reverseBitsOrderInEachByte(pDestWork);

                count = rnd.nextInt(33);
                if (srcPos + count > pSrc.length * 8) {
                    count = pSrc.length * 8 - srcPos;
                    // Note: results will be different in a case of scanning outside the array!
                    // ScifioBitBuffer simply stops the loop and leaves the result not shifted left enough,
                    // but AlgART supposes that we have zeros outside the array
                }
                vTest = (int) PackedBitArraysPer8.getBitsInReverseOrder(pSrc, srcPos, count);
                ScifioBitBuffer bitBuffer = new ScifioBitBuffer(pSrc);
                bitBuffer.skipBits(srcPos);
                v = bitBuffer.getBits(count);
                if (vTest != v) {
                    throw new AssertionError("The bug C in getBitsInReverseOrder found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + Long.toBinaryString(vTest) + " instead of " + Long.toBinaryString(v));
                }
                showProgress(testCount);
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
                showProgress(testCount);
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
                showProgress(testCount);
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
                showProgress(testCount);
            }

            System.out.println("Testing \"unpackBits\" to float[]...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(fDest, 0, fDestWork, 0, fDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.unpackBits(fDestWork, destPos, pSrc, srcPos, count,
                        FLOAT_BIT_0, FLOAT_BIT_1);
                System.arraycopy(bSrc, srcPos, bDestWork1, destPos, count);
                for (int k = 0; k < len; k++)
                    if (fDestWork[k] != (k < destPos || k >= destPos + count ? fDest[k] :
                            bDestWork1[k] ? FLOAT_BIT_1 : FLOAT_BIT_0)) {
                        throw new AssertionError("The bug in unpackBits to float[] found in test #" + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                showProgress(testCount);
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
                showProgress(testCount);
            }

            //[[Repeat(INCLUDE_FROM_FILE, PackedBitArraysTest.java, logicalOperations)
            //      PackedBitArrays ==> PackedBitArraysPer8 ;;
            //      startOffset, ==> 0, ;;
            //      startOffset\s*\+\s* ==>
            // !! Auto-generated: NOT EDIT !! ]]
            System.out.println("Testing \"notBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.notBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] = !bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug A in notBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                PackedBitArraysPer8.copyBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.notBits(
                        pDestWork, destPos, pDestWork, destPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug B in notBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                showProgress(testCount);
            }

            System.out.println("Testing \"andBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.andBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] &= bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in andBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                showProgress(testCount);
            }

            System.out.println("Testing \"orBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.orBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] |= bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in orBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                showProgress(testCount);
            }

            System.out.println("Testing \"xorBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.xorBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] ^= bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in xorBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                showProgress(testCount);
            }

            System.out.println("Testing \"andNotBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.andNotBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] &= !bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in andNotBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                showProgress(testCount);
            }

            System.out.println("Testing \"orNotBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArraysPer8.orNotBits(
                        pDestWork, destPos, pSrc, srcPos, count);
                PackedBitArraysPer8.unpackBits(bDestWork1, 0, pDestWork, 0, len);
                for (int k = 0; k < count; k++)
                    bDestWork2[destPos + k] |= !bSrc[srcPos + k];
                for (int k = 0; k < len; k++)
                    if (bDestWork1[k] != bDestWork2[k])
                        throw new AssertionError("The bug in orNotBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                showProgress(testCount);
            }
            //[[Repeat.IncludeEnd]]

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
                showProgress(testCount);
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
                showProgress(testCount);
            }
        }
        System.out.println("           ");
        System.out.println("All O'k: testing time "
                + (System.currentTimeMillis() - PackedBitArraysTest.tStart) + " ms");
    }

    /**
     * A copy of SCIFIO class io.scif.codec.BitBuffer by Eric Kjellman,
     * made for testing exact compatibility with it.
     */
    private static class ScifioBitBuffer {

        // -- Constants --

        /**
         * Various bitmasks for the 0000xxxx side of a byte.
         */
        private static final int[] BACK_MASK = {0x00, // 00000000
                0x01, // 00000001
                0x03, // 00000011
                0x07, // 00000111
                0x0F, // 00001111
                0x1F, // 00011111
                0x3F, // 00111111
                0x7F // 01111111
        };

        /**
         * Various bitmasks for the xxxx0000 side of a byte.
         */
        private static final int[] FRONT_MASK = {0x0000, // 00000000
                0x0080, // 10000000
                0x00C0, // 11000000
                0x00E0, // 11100000
                0x00F0, // 11110000
                0x00F8, // 11111000
                0x00FC, // 11111100
                0x00FE // 11111110
        };

        private final byte[] byteBuffer;

        private int currentByte;

        private int currentBit;

        private final int eofByte;

        private boolean eofFlag;

        /**
         * Default constructor.
         */
        public ScifioBitBuffer(final byte[] byteBuffer) {
            this.byteBuffer = byteBuffer;
            currentByte = 0;
            currentBit = 0;
            eofByte = byteBuffer.length;
        }

        /**
         * Skips a number of bits in the BitBuffer.
         *
         * @param bits Number of bits to skip
         */
        public void skipBits(final long bits) {
            if (bits < 0) {
                throw new IllegalArgumentException("Bits to skip may not be negative");
            }

            // handles skipping past eof
            if ((long) eofByte * 8 < (long) currentByte * 8 + currentBit + bits) {
                eofFlag = true;
                currentByte = eofByte;
                currentBit = 0;
                return;
            }

            final int skipBytes = (int) (bits / 8);
            final int skipBits = (int) (bits % 8);
            currentByte += skipBytes;
            currentBit += skipBits;
            while (currentBit >= 8) {
                currentByte++;
                currentBit -= 8;
            }
        }

        /**
         * Returns an int value representing the value of the bits read from the byte
         * array, from the current position. Bits are extracted from the "left side"
         * or high side of the byte.
         * <p>
         * The current position is modified by this call.
         * <p>
         * Bits are pushed into the int from the right, endianness is not considered
         * by the method on its own. So, if 5 bits were read from the buffer "10101",
         * the int would be the integer representation of 000...0010101 on the target
         * machine.
         * <p>
         * In general, this also means the result will be positive unless a full 32
         * bits are read.
         * <p>
         * Requesting more than 32 bits is allowed, but only up to 32 bits worth of
         * data will be returned (the last 32 bits read).
         * <p>
         *
         * @param bitsToRead the number of bits to read from the bit buffer
         * @return the value of the bits read
         */
        public int getBits(int bitsToRead) {
            if (bitsToRead < 0) {
                throw new IllegalArgumentException("Bits to read may not be negative");
            }
            if (bitsToRead == 0) return 0;
            if (eofFlag) return -1; // Already at end of file
            int toStore = 0;
            while (bitsToRead != 0 && !eofFlag) {
                if (currentBit < 0 || currentBit > 7) {
                    throw new IllegalStateException("byte=" + currentByte + ", bit = " +
                            currentBit);
                }

                // if we need to read from more than the current byte in the
                // buffer...
                final int bitsLeft = 8 - currentBit;
                if (bitsToRead >= bitsLeft) {
                    toStore <<= bitsLeft;
                    bitsToRead -= bitsLeft;
                    final int cb = byteBuffer[currentByte];
                    if (currentBit == 0) {
                        // we can read in a whole byte, so we'll do that.
                        toStore += cb & 0xff;
                    } else {
                        // otherwise, only read the appropriate number of bits off
                        // the back
                        // side of the byte, in order to "finish" the current byte
                        // in the
                        // buffer.
                        toStore += cb & BACK_MASK[bitsLeft];
                        currentBit = 0;
                    }
                    currentByte++;
                } else {
                    // We will be able to finish using the current byte.
                    // read the appropriate number of bits off the front side of the
                    // byte,
                    // then push them into the int.
                    toStore = toStore << bitsToRead;
                    final int cb = byteBuffer[currentByte] & 0xff;
                    toStore += (cb & (0x00FF - FRONT_MASK[currentBit])) >> (bitsLeft -
                            bitsToRead);
                    currentBit += bitsToRead;
                    bitsToRead = 0;
                }
                // If we reach the end of the buffer, return what we currently have.
                if (currentByte == eofByte) {
                    eofFlag = true;
                    return toStore;
                }
            }
            return toStore;
        }
    }
}
