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
 * <p>Illustration for {@link PackedBitArraysPer8#getBits(byte[], long, int)} method.</p>
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

    private static String bitsInReverseOrderToString(long packed, int count) {
        final StringBuilder sb = new StringBuilder();
        for (int k = count - 1; k >= 0; k--) {
            sb.append((packed >>> k) & 1);
        }
        return sb.toString();
    }

    private static void getBitsTest(boolean[] bSrc, byte[] pSrc, int pos, int count) {
        String full = JArrays.toBinaryString(bSrc, "", 200);
        System.out.println(full);

        final long bits = PackedBitArraysPer8.getBits(pSrc, pos, count);
        String local = bitsToString(bits, count);
        System.out.println(" ".repeat(pos) + local);
        if (!full.substring(pos, pos + count).equals(local)) {
            throw new AssertionError();
        }
    }

    private static void getBitsTestInReverseOrder(boolean[] bSrc, byte[] pSrc, int pos, int count) {
        String full = JArrays.toBinaryString(bSrc, "", 200);
        full = new StringBuilder(full).reverse().toString();
        System.out.println(full);

        final long bits = PackedBitArraysPer8.getBitsInReverseOrder(pSrc, pos, count);
        String local = bitsInReverseOrderToString(bits, count);
        System.out.println(" ".repeat(pos) + local);
//        if (!full.substring(pos, pos + count).equals(local)) {
//            throw new AssertionError();
//        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + PackedBitArraysPer8GetBitsDemo.class.getName()
                    + " srcPos count [randSeed]");
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

        for (int test = 0; test < 100; test++) {
            final int len = 100;
            boolean[] bSrc = new boolean[len];
            for (int k = 0; k < bSrc.length; k++) {
                bSrc[k] = rnd.nextDouble() <= 0.5;
            }
            final int packedLen = (int) PackedBitArraysPer8.packedLength(bSrc.length);
            final byte[] pSrc = new byte[packedLen];
            PackedBitArraysPer8.packBits(pSrc, 0, bSrc, 0, len);

            getBitsTest(bSrc, pSrc, pos, count);
            getBitsTestInReverseOrder(bSrc, pSrc, pos, count);
        }
    }
}
