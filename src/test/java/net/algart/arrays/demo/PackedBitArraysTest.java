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

import java.util.Random;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>Basic test for {@link PackedBitArrays} class</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitArraysTest {
    private static final float FLOAT_BIT_0 = -1f, FLOAT_BIT_1 = 157f;

    final static long tStart = System.currentTimeMillis();
    private static long tFix = tStart;

    private static long getBits64Simple(long[] src, long srcPos, int count) {
        long result = 0;
        for (int k = 0; k < count; k++) {
            if (srcPos + k >= 64 * (long) src.length) {
                break;
            }
            final long bit = PackedBitArrays.getBit(src, srcPos + k) ? 1L : 0L;
            result |= bit << k;
        }
        return result;
    }

    static void showProgress(int testCount) {
        long t = System.currentTimeMillis();
        if (t - tFix > 500) {
            tFix = t;
            System.out.print("\r" + testCount + "\r");
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
            System.out.println("Usage: " + PackedBitArraysTest.class.getName()
                    + " [-SuperLarge] arrayLength numberOfTests [randSeed]");
            System.out.println("-SuperLarge key will perform test for bit arrays after 2^32 index (>512 MB");
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
            short[] sSrc = new short[len];
            for (int k = 0; k < sSrc.length; k++) {
                sSrc[k] = (short) rnd.nextInt();
            }
            float[] fSrc = new float[len];
            for (int k = 0; k < fSrc.length; k++) {
                fSrc[k] = (float) rnd.nextDouble();
            }
            boolean[] bDest = new boolean[len];
            for (int k = 0; k < bDest.length; k++) {
                bDest[k] = rnd.nextDouble() <= density;
            }
            int[] iDest = new int[len];
            for (int k = 0; k < iDest.length; k++) {
                iDest[k] = k;
            }
            float[] fDest = new float[len];
            for (int k = 0; k < bDest.length; k++) {
                fDest[k] = rnd.nextFloat();
            }
            boolean[] bDestWork1 = bDest.clone();
            boolean[] bDestWork2 = bDest.clone();
            int[] iDestWork1 = iDest.clone();
            int[] iDestWork2 = iDest.clone();
            float[] fDestWork = fDest.clone();
            int cardCorrect = 0;
            for (int k = 0; k < len; k++) {
                if (bSrc[k]) {
                    cardCorrect++;
                }
            }

            long longPackedLen = PackedBitArrays.packedLength(startOffset + len);
            int packedLen = (int) longPackedLen;
            if (packedLen != longPackedLen) {
                throw new IllegalArgumentException("Too large bit array (>2^37-64 bits)");
            }
            long[] pSrc = new long[packedLen];
            long[] pDest = new long[packedLen];

            System.out.println("Packing source bits... ");
            PackedBitArrays.packBits(pSrc, startOffset, bSrc, 0, len);
            System.out.println("Packing target bits...");
            PackedBitArrays.packBits(pDest, startOffset, bDest, 0, len);
            long[] pDestWork1 = pDest.clone();
            long[] pDestWork2 = pDest.clone();
            Runtime rt = Runtime.getRuntime();
            System.out.printf(Locale.US, "Used memory (for all test arrays): %.3f MB%n",
                    (rt.totalMemory() - rt.freeMemory()) / 1048576.0);

            long card = PackedBitArrays.cardinality(pSrc, startOffset, startOffset + len);
            if (card != cardCorrect) {
                throw new AssertionError("The bug in cardinality found at start: " + card +
                        " instead of " + cardCorrect);
            }
            System.out.println("Number of high source bits is " + card);
            System.out.println();

            System.out.println("Testing \"setBit/getBit\" methods...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                for (int k = 0; k < len; k++) {
                    if (bSrc[k] != PackedBitArrays.getBit(pSrc, startOffset + k)) {
                        throw new AssertionError("The bug A in getBit found in test #" + testCount
                                + ", error found at " + k);
                    }
                }
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                for (int k = 0; k < count; k++) {
                    PackedBitArrays.setBit(pDestWork1, startOffset + destPos + k, bSrc[srcPos + k]);
                }
                for (int k = 0; k < count; k++) {
                    if (bSrc[srcPos + k] != PackedBitArrays.getBit(pDestWork1, startOffset + destPos + k)) {
                        throw new AssertionError("The bug B in setBit found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                for (int k = 0; k < count; k++) {
                    PackedBitArrays.setBitNoSync(pDestWork1, startOffset + destPos + k, bSrc[srcPos + k]);
                }
                for (int k = 0; k < count; k++) {
                    if (bSrc[srcPos + k] != PackedBitArrays.getBit(pDestWork1, startOffset + destPos + k)) {
                        throw new AssertionError("The bug C in setBitNoSync found in test #" +
                                testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"getBits64\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                for (int k = 0; k < len; k++) {
                    boolean bTest = PackedBitArrays.getBits64(pSrc, startOffset + k, 1) == 1;
                    boolean b = PackedBitArrays.getBit(pSrc, startOffset + k);
                    if (b != bTest) {
                        throw new AssertionError("The bug A in getBits64 found in test #" +
                                testCount + ", error found at " + k);
                    }
                }
                int srcPos = rnd.nextInt(len);
                int count = rnd.nextInt(65);
                long vTest = PackedBitArrays.getBits64(pSrc, startOffset + srcPos, count);
                long v = getBits64Simple(pSrc, startOffset + srcPos, count);
                if (vTest != v) {
                    throw new AssertionError("The bug B in getBits64 found in test #" + testCount +
                            ": srcPos = " + srcPos + ", count = " + count
                            + ", " + Long.toBinaryString(vTest) + " instead of " + Long.toBinaryString(v));
                }
                showProgress(testCount);
            }

            if (startOffset == 0) {
                System.out.println("Testing \"packBits\" method...");
                for (int testCount = 0; testCount < numberOfTests; testCount++) {
                    System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                    System.arraycopy(pDest, 0, pDestWork2, 0, pDest.length);
                    int srcPos = rnd.nextInt(len + 1);
                    int destPos = rnd.nextInt(len + 1);
                    int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                    PackedBitArrays.packBits(pDestWork1, destPos, bSrc, srcPos, count);
                    PackedBitArrays.packBitsNoSync(pDestWork2, destPos, bSrc, srcPos, count);
                    for (int k = 0; k < count; k++) {
                        if (bSrc[srcPos + k] != PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                            throw new AssertionError("The bug A in packBits found in test #"
                                    + testCount + ": "
                                    + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                    + ", error found at " + k);
                        }
                    }
                    for (int k = 0; k < destPos; k++) {
                        if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                            throw new AssertionError("The bug B in packBits found in test #"
                                    + testCount + ": "
                                    + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                    + ", error found at " + k);
                        }
                    }
                    for (int k = destPos + count; k < len; k++) {
                        if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                            throw new AssertionError("The bug C in packBits found in test #"
                                    + testCount + ": "
                                    + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                    + ", error found at " + k);
                        }
                    }
                    long[] packed = PackedBitArrays.packBits(bSrc, srcPos, count);
                    for (int k = 0; k < 64 * packed.length; k++) {
                        if (PackedBitArrays.getBit(packed, k) != (k < count && bSrc[srcPos + k])) {
                            throw new AssertionError("The bug D in packBits found in test #"
                                    + testCount + ": "
                                    + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                    + ", error found at " + k);
                        }
                    }
                    if (!java.util.Arrays.equals(pDestWork1, pDestWork2)) {
                        throw new AssertionError("The bug E in packBitsNoSync " +
                                "found in test #" + testCount);
                    }
                    showProgress(testCount);
                }
            }

            System.out.println("Testing \"packBitsInverted\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.packBitsInverted(pDestWork1, destPos, bSrc, srcPos, count);
                for (int k = 0; k < count; k++) {
                    if (bSrc[srcPos + k] == PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsInverted found in test #"
                                + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsInverted found in test #"
                                + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsInverted found in test #"
                                + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"packBitsGreater\" from short[] method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                int threshold = rnd.nextInt() & 0xFFFF;
                PackedBitArrays.packBitsGreater(pDestWork1, destPos, sSrc, srcPos, count, threshold);
                for (int k = 0; k < count; k++) {
                    if (((sSrc[srcPos + k] & 0xFFFF) > threshold) !=
                            PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsGreater found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsGreater found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsGreater found in test #"
                                + testCount + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"packBitsLess\" from short[] method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                int threshold = rnd.nextInt() & 0xFFFF;
                PackedBitArrays.packBitsLess(pDestWork1, destPos, sSrc, srcPos, count, threshold);
                for (int k = 0; k < count; k++) {
                    if (((sSrc[srcPos + k] & 0xFFFF) < threshold) !=
                            PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsLess found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsLess found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsLess found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"packBitsGreaterOrEqual\" from short[] method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                int threshold = rnd.nextInt() & 0xFFFF;
                PackedBitArrays.packBitsGreaterOrEqual(pDestWork1, destPos, sSrc, srcPos, count, threshold);
                for (int k = 0; k < count; k++) {
                    if (((sSrc[srcPos + k] & 0xFFFF) >= threshold) !=
                            PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsGreaterOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsGreaterOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsGreaterOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"packBitsLessOrEqual\" from short[] method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                int threshold = rnd.nextInt() & 0xFFFF;
                PackedBitArrays.packBitsLessOrEqual(pDestWork1, destPos, sSrc, srcPos, count, threshold);
                for (int k = 0; k < count; k++) {
                    if (((sSrc[srcPos + k] & 0xFFFF) <= threshold) !=
                            PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsLessOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsLessOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsLessOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"packBitsGreaterOrEqual\" from float[] method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                float threshold = (float) rnd.nextDouble();
                PackedBitArrays.packBitsGreaterOrEqual(pDestWork1, destPos, fSrc, srcPos, count, threshold);
                for (int k = 0; k < count; k++) {
                    if ((fSrc[srcPos + k] >= threshold) != PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsGreaterOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsGreaterOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsGreaterOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"packBitsLessOrEqual\" from float[] method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                float threshold = (float) rnd.nextDouble();
                PackedBitArrays.packBitsLessOrEqual(pDestWork1, destPos, fSrc, srcPos, count, threshold);
                for (int k = 0; k < count; k++) {
                    if ((fSrc[srcPos + k] <= threshold) != PackedBitArrays.getBit(pDestWork1, destPos + k)) {
                        throw new AssertionError("The bug A in packBitsLessOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = 0; k < destPos; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug B in packBitsLessOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                for (int k = destPos + count; k < len; k++) {
                    if (PackedBitArrays.getBit(pDestWork1, k) != PackedBitArrays.getBit(pDest, k)) {
                        throw new AssertionError("The bug C in packBitsLessOrEqual found in test #"
                                + testCount
                                + ": " + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"copyBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(pDest, 0, pDestWork2, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.copyBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.copyBitsNoSync(
                        pDestWork2, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(
                        bDestWork1, 0, pDestWork1, startOffset, len);
                for (int k = 0; k < count; k++) {
                    boolean bit = PackedBitArrays.getBit(pDestWork1, startOffset + destPos + k);
                    if (bSrc[srcPos + k] != bit) {
                        throw new AssertionError("The bug A in copyBits found in test #"
                                + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + " (src=" + JArrays.toBinaryString(bSrc, "", 200)
                                + ", dest=" + JArrays.toBinaryString(bDest, "", 200)
                                + ", we have " + JArrays.toBinaryString(bDestWork1, "", 200)
                                + ", dest[" + (destPos + k) + "]=" + bit
                                + " instead of " + bSrc[srcPos + k] + ")");
                    }
                }
                System.arraycopy(bSrc, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug B in copyBits or unpackBits found in test #"
                                + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k
                                + " (src=" + JArrays.toBinaryString(bSrc, "", 200)
                                + ", dest=" + JArrays.toBinaryString(bDest, "", 200)
                                + ", we have " + JArrays.toBinaryString(bDestWork1, "", 200)
                                + " instead of " + JArrays.toBinaryString(bDestWork2, "", 200)
                                + ")");
                    }
                }
                if (!java.util.Arrays.equals(pDestWork1, pDestWork2)) {
                    throw new AssertionError("The bug C in copyBitsNoSync " +
                            "found in test #" + testCount);
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"copyBits\" + \"unpackBits\" method, inside a single array...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(pDest, 0, pDestWork2, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.copyBits(
                        pDestWork1, startOffset + destPos, pDestWork1, startOffset + srcPos, count);
                PackedBitArrays.copyBitsNoSync(
                        pDestWork2, startOffset + destPos, pDestWork2, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, destPos, pDestWork1, startOffset + destPos, count);
                System.arraycopy(bDestWork2, srcPos, bDestWork2, destPos, count);
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug A in copyBits or unpackBits found in test #"
                                + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                if (!java.util.Arrays.equals(pDestWork1, pDestWork2)) {
                    throw new AssertionError("The bug B in copyBitsNoSync " +
                            "found in test #" + testCount);
                }
                boolean[] unpacked = PackedBitArrays.unpackBits(pDestWork1, destPos, count);
                if (unpacked.length != count) {
                    throw new AssertionError("Invalid length");
                }
                for (int k = 0; k < count; k++) {
                    if (unpacked[k] != bDestWork1[destPos + k]) {
                        throw new AssertionError("The bug C in unpackBits in test #" +
                                testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " +
                                count + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"unpackBits\" to float[]...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(fDest, 0, fDestWork, 0, fDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.unpackBits(fDestWork, destPos, pSrc, startOffset + srcPos, count,
                        FLOAT_BIT_0, FLOAT_BIT_1);
                System.arraycopy(bSrc, srcPos, bDestWork1, destPos, count);
                float[] floats = PackedBitArrays.unpackBitsToFloats(pSrc, startOffset + srcPos, count,
                        FLOAT_BIT_0, FLOAT_BIT_1);
                for (int k = 0; k < len; k++) {
                    if (fDestWork[k] != (k < destPos || k >= destPos + count ? fDest[k] :
                            bDestWork1[k] ? FLOAT_BIT_1 : FLOAT_BIT_0)) {
                        throw new AssertionError("The bug A in unpackBits to float[] found in test #" +
                                testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count +
                                ", error found at " + k);
                    }
                }
                for (int k = 0; k < count; k++) {
                    if (floats[k] != fDestWork[destPos + k]) {
                        throw new AssertionError("The bug B in unpackBitsToFloats found in test #" +
                                testCount + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count +
                                ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"unpackUnitBits\" to float[]...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(fDest, 0, fDestWork, 0, fDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.unpackUnitBits(
                        fDestWork, destPos, pSrc, startOffset + srcPos, count, FLOAT_BIT_1);
                System.arraycopy(bSrc, srcPos, bDestWork1, destPos, count);
                for (int k = 0; k < len; k++) {
                    if (fDestWork[k] != (k < destPos || k >= destPos + count ? fDest[k] :
                            bDestWork1[k] ? FLOAT_BIT_1 : fDest[k])) {
                        throw new AssertionError("The bug in unpackUnitBits to float[] found in test #"
                                + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"unpackZeroBits\" to float[]...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(fDest, 0, fDestWork, 0, fDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.unpackZeroBits(
                        fDestWork, destPos, pSrc, startOffset + srcPos, count, FLOAT_BIT_0);
                System.arraycopy(bSrc, srcPos, bDestWork1, destPos, count);
                for (int k = 0; k < len; k++) {
                    if (fDestWork[k] != (k < destPos || k >= destPos + count ? fDest[k] :
                            bDestWork1[k] ? fDest[k] : FLOAT_BIT_0)) {
                        System.out.println(JArrays.toBinaryString(bSrc, ",", 100));
                        System.out.println(JArrays.toBinaryString(bDestWork1, ",", 100));
                        System.out.println(JArrays.toString(fDestWork, ",", 100));
                        throw new AssertionError("The bug in unpackZeroBits to float[] found in test #"
                                + testCount
                                + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"addBitsToInts\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(iDest, 0, iDestWork1, 0, iDest.length);
                System.arraycopy(iDest, 0, iDestWork2, 0, iDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.addBitsToInts(iDestWork1, destPos, pSrc, startOffset + srcPos, count);
                for (int k = 0; k < count; k++) {
                    iDestWork2[destPos + k] += bSrc[srcPos + k] ? 1 : 0;
                }
                for (int k = 0; k < len; k++) {
                    if (iDestWork1[k] != iDestWork2[k]) {
                        throw new AssertionError("The bug in addBitsToInts found in test #"
                                + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"fillBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(pDest, 0, pDestWork2, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - destPos);
                boolean value = rnd.nextBoolean();
                PackedBitArrays.fillBits(pDestWork1, startOffset + destPos, count, value);
                PackedBitArrays.fillBitsNoSync(pDestWork2, startOffset + destPos, count, value);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] = value;
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug A in fillBits found in test #"
                                + testCount + ": "
                                + "destPos = " + destPos + ", count = " + count + ", value = " + value
                                + ", error found at " + k);
                    }
                }
                if (!java.util.Arrays.equals(pDestWork1, pDestWork2)) {
                    throw new AssertionError("The bug B in fillBitsNoSync " +
                            "found in test #" + testCount);
                }
                showProgress(testCount);
            }

            //[[Repeat.SectionStart logicalOperations]]
            System.out.println("Testing \"notBits\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.notBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] = !bSrc[srcPos + k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug A in notBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                PackedBitArrays.copyBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.notBits(pDestWork1, startOffset + destPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug B in notBits found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"andBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.andBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
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
                showProgress(testCount);
            }

            System.out.println("Testing \"orBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.orBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
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
                showProgress(testCount);
            }

            System.out.println("Testing \"xorBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.xorBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
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
                showProgress(testCount);
            }

            System.out.println("Testing \"andNotBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.andNotBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
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
                showProgress(testCount);
            }

            System.out.println("Testing \"orNotBits\" method, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.orNotBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
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
                showProgress(testCount);
            }
            //[[Repeat.SectionEnd logicalOperations]]

            System.out.println("Testing \"reverseBitsOrder\" method...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                System.arraycopy(bDest, 0, bDestWork2, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.reverseBitsOrder(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                PackedBitArrays.unpackBits(bDestWork1, 0, pDestWork1, startOffset, len);
                for (int k = 0; k < count; k++) {
                    bDestWork2[destPos + k] = bSrc[srcPos + count - 1 - k];
                }
                for (int k = 0; k < len; k++) {
                    if (bDestWork1[k] != bDestWork2[k]) {
                        throw new AssertionError("The bug in reverseBitsOrder found in test #"
                                + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                                + ", error found at " + k);
                    }
                }
                showProgress(testCount);
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
                long index = PackedBitArrays.indexOfBit(
                        pSrc, startOffset + lowIndex, startOffset + highIndex, value);
                if (index != indexCorrect) {
                    throw new AssertionError("The bug in indexOfBit found in test #" + testCount + ": "
                            + index + " instead of " + indexCorrect
                            + ", fromIndex=" + lowIndex + ", toIndex=" + highIndex + ", value=" + value);
                }
                showProgress(testCount);
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
                long index = PackedBitArrays.lastIndexOfBit(pSrc,
                        startOffset + lowIndex, startOffset + highIndex, value);
                if (index != indexCorrect) {
                    throw new AssertionError("The bug in lastIndexOfBit found in test #"
                            + testCount + ": "
                            + index + " instead of " + indexCorrect
                            + ", fromIndex=" + lowIndex + ", toIndex=" + highIndex + ", value=" + value);
                }
                showProgress(testCount);
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
                card = PackedBitArrays.cardinality(pSrc, startOffset + pos, startOffset + pos + count);
                if (card != cardCorrect) {
                    throw new AssertionError("The bug in cardinality found in test #" + testCount + ": "
                            + card + " instead of " + cardCorrect);
                }
                showProgress(testCount);
            }

            System.out.println("Testing \"bitHashCode\" and \"bitEquals\" methods, two different arrays...");
            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                System.arraycopy(pDest, 0, pDestWork1, 0, pDest.length);
                System.arraycopy(bDest, 0, bDestWork1, 0, bDest.length);
                int srcPos = rnd.nextInt(len + 1);
                int destPos = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos));
                PackedBitArrays.copyBits(
                        pDestWork1, startOffset + destPos, pSrc, startOffset + srcPos, count);
                System.arraycopy(bSrc, srcPos, bDestWork1, destPos, count);
                if (!PackedBitArrays.bitEquals(
                        pSrc, startOffset + srcPos, pDestWork1, startOffset + destPos, count)) {
                    throw new AssertionError("The bug in bitEquals (illegal false) found in test #"
                            + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                }
                int h1 = PackedBitArrays.bitHashCode(
                        pSrc, startOffset + srcPos, startOffset + srcPos + count);
                int h2 = PackedBitArrays.bitHashCode(
                        pDestWork1, startOffset + destPos, startOffset + destPos + count);
                if (h1 != h2) {
                    throw new AssertionError("The bug in bitHashCode found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                }
                if (count > 0) {
                    PackedBitArrays.setBit(pDestWork1, startOffset + destPos + count - 1,
                            !PackedBitArrays.getBit(pDestWork1, startOffset + destPos + count - 1));
                    if (PackedBitArrays.bitEquals(
                            pSrc, startOffset + srcPos, pDestWork1, startOffset + destPos, count)) {
                        throw new AssertionError("The bug in bitEquals "
                                + "(illegal true after changing last bit) "
                                + "found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                    }
                    h1 = PackedBitArrays.bitHashCode(
                            pDestWork1, startOffset + destPos, startOffset + destPos + count);
                    if (h1 == h2) {
                        throw new AssertionError("The bug in bitHashCode "
                                + "(changing last bit was not detected) "
                                + "found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                    }
                    PackedBitArrays.setBit(pDestWork1, startOffset + destPos + count - 1,
                            !PackedBitArrays.getBit(pDestWork1, startOffset + destPos + count - 1));
                    PackedBitArrays.setBit(pDestWork1, startOffset + destPos,
                            !PackedBitArrays.getBit(pDestWork1, startOffset + destPos));
                    if (PackedBitArrays.bitEquals(
                            pSrc, startOffset + srcPos, pDestWork1, startOffset + destPos, count)) {
                        throw new AssertionError("The bug in bitEquals "
                                + "(illegal true after changing first bit) "
                                + "found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                    }
                    h1 = PackedBitArrays.bitHashCode(
                            pDestWork1, startOffset + destPos, startOffset + destPos + count);
                    if (h1 == h2) {
                        throw new AssertionError("The bug in bitHashCode "
                                + "(changing first bit was not detected) "
                                + "found in test #" + testCount + ": "
                                + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                    }
                }
                if (!JArrays.arrayEquals(bSrc, srcPos, bDestWork1, destPos, count)) {
                    throw new AssertionError("The bug in arrayEquals (illegal false) found in test #"
                            + testCount
                            + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                }
                int h3 = JArrays.arrayHashCode(bSrc, srcPos, srcPos + count);
                int h4 = JArrays.arrayHashCode(bDestWork1, destPos, destPos + count);
                if (h3 != h4) {
                    throw new AssertionError("The bug in arrayHashCode found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
                }
                showProgress(testCount);
            }
        }
        System.out.println("           ");
        System.out.println("All O'k: testing time " + (System.currentTimeMillis() - tStart) + " ms");
    }
}
