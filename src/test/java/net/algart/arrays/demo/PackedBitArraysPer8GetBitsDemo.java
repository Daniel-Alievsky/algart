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

import java.util.Random;

/**
 * <p>Illustration for {@link PackedBitArraysPer8#getBits64} and {@link PackedBitArraysPer8#setBits64}
 * methods.</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitArraysPer8GetBitsDemo {
    private static String bitsToString(long packed, int count) {
        final StringBuilder sb = new StringBuilder();
        for (int k = 0; k < count; k++) {
            sb.append((packed >>> k) & 1);
        }
        return sb.toString();
    }

    private static String bitsToStringInReverseOrder(long packed, int count) {
        final StringBuilder sb = new StringBuilder();
        for (int k = count - 1; k >= 0; k--) {
            sb.append((packed >>> k) & 1);
        }
        return sb.toString();
    }

    private static String toBinaryString(byte[] pSrc, int len) {
        return JArrays.toBinaryString(
                PackedBitArraysPer8.unpackBits(pSrc, 0, len), "", 200);
    }

    private static String toBinaryStringInReverseOrder(byte[] pSrc, int len) {
        byte[] reverse = pSrc.clone();
        PackedBitArraysPer8.reverseBitsOrderInEachByte(reverse);
        return JArrays.toBinaryString(
                PackedBitArraysPer8.unpackBits(reverse, 0, len), "", 200);
    }

    private static void getBitsTest(byte[] pSrc, int pos, int count, int len) {
        System.out.println("Test in normal order");
        System.out.println("[       ".repeat(len / 8));
        final String full = toBinaryString(pSrc, len);
        System.out.println(full);

        long bits = PackedBitArraysPer8.getBits64(pSrc, pos, count);
        final String local = bitsToString(bits, count);
        System.out.println(" ".repeat(pos) + local);
        if (!full.substring(pos, pos + count).equals(local)) {
            throw new AssertionError("Bug in getBits64");
        }
        byte[] pDest = pSrc.clone();
        for (long testBits : new long[] {0, 3, -1, -4, ~bits, bits, 0x5555555555555555L}) {
            System.out.println(" ".repeat(pos) + bitsToString(testBits, count) + " - updating:");
            PackedBitArraysPer8.setBits64(pDest, pos, testBits, count);
            System.out.println(toBinaryString(pDest, len));
            bits = PackedBitArraysPer8.getBits64(pDest, pos, count);
            testBits &= (1L << count) - 1;
            if (bits != testBits) {
                System.out.println(" ".repeat(pos) + bitsToString(bits, count));
                throw new AssertionError("Bug in setBits64");
            }
        }
    }

    private static void getBitsTestInReverseOrder(byte[] pSrc, int pos, int count, int len) {
        System.out.println("Test in reverse order");
        System.out.println("[       ".repeat(len / 8));
        final String full = toBinaryStringInReverseOrder(pSrc, len);
        System.out.println(full);

        // Other way to show bits:
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < len; k++) {
            boolean b = PackedBitArraysPer8.getBitInReverseOrder(pSrc, k);
            sb.append(b ? "1" : "0");
        }
        if (!sb.toString().equals(full)) {
            throw new AssertionError("Bug in getBitInReverseOrder (single bit)");
        }

        long bits = PackedBitArraysPer8.getBits64InReverseOrder(pSrc, pos, count);
        final String local = bitsToStringInReverseOrder(bits, count);
        System.out.println(" ".repeat(pos) + local);
        if (!full.substring(pos, pos + count).equals(local)) {
            throw new AssertionError("Bug in getBitsInReverseOrder");
        }
        byte[] pDest = pSrc.clone();
        for (long testBits : new long[] {0, 3, -1, -4, ~bits, bits, 0x5555555555555555L}) {
            System.out.println(" ".repeat(pos) + bitsToStringInReverseOrder(testBits, count) + " - updating:");
            PackedBitArraysPer8.setBits64InReverseOrder(pDest, pos, testBits, count);
            System.out.println(toBinaryStringInReverseOrder(pDest, len));
            bits = PackedBitArraysPer8.getBits64InReverseOrder(pDest, pos, count);
            testBits &= (1L << count) - 1;
            if (bits != testBits) {
                System.out.println(" ".repeat(pos) + bitsToStringInReverseOrder(bits, count));
                throw new AssertionError("Bug in setBits64InReverseOrder");
            }
        }
    }
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + PackedBitArraysPer8GetBitsDemo.class.getName()
                    + " pos count [randSeed]");
            return;
        }

        int pos = Integer.parseInt(args[0]);
        int count = Integer.parseInt(args[1]);
        long seed;
        if (args.length < 3) {
            seed = new Random().nextLong();
        } else {
            seed = Long.parseLong(args[2]);
        }
        Random rnd = new Random(seed);
        System.out.println("Start random seed " + seed);
        final int len = 100;
        final int packedLen = PackedBitArraysPer8.packedLength(len);
        boolean[] bSrc = new boolean[len];
        System.out.printf("Length %d, packed length %d, reverse unpacked length %d%n",
                len,
                packedLen,
                PackedBitArrays.unpackedLength(packedLen));

        for (int test = 1; test <= 50; test++) {
            System.out.printf("%nTest #%d%n", test);
            for (int k = 0; k < bSrc.length; k++) {
                bSrc[k] = rnd.nextBoolean();
            }
            final byte[] pSrc = new byte[packedLen];
            PackedBitArraysPer8.packBits(pSrc, 0, bSrc, 0, len);

            getBitsTest(pSrc, pos, count, len);
            getBitsTestInReverseOrder(pSrc, pos, count, len);
        }
    }
}
