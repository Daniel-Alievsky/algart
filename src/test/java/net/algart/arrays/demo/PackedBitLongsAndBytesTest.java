/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

public class PackedBitLongsAndBytesTest {
    private static boolean[] showBits(byte[] bytes, int length) {
        final boolean[] booleans = new boolean[length];
        PackedBitArraysPer8.unpackBits(booleans, 0, bytes, 0, length);
        System.out.println(JArrays.toBinaryString(booleans, "", 200));
        return booleans;
    }

    private static boolean[] showBits(long[] longs, int length) {
        final boolean[] booleans = new boolean[length];
        PackedBitArrays.unpackBits(booleans, 0, longs, 0, length);
        System.out.println(JArrays.toBinaryString(booleans, "", 200));
        return booleans;
    }

    public static void main(String[] args) {
        final int numberOfBits = 2 * 64 + 13;
        final byte[] bytes = new byte[(numberOfBits + 7) / 8];
        for (int k = 0; k < bytes.length; k++) {
            bytes[k] = (byte) k;
        }
        boolean[] bits1 = showBits(bytes, numberOfBits);
        final long[] longs = PackedBitArraysPer8.toLongArray(bytes, numberOfBits);
        boolean[] bits2 = showBits(longs, numberOfBits);
        if (!Arrays.equals(bits1, bits2)) {
            throw new AssertionError("Bug A");
        }
        final long[] longsFromBuffer = PackedBitArraysPer8.toLongArray(ByteBuffer.wrap(bytes), numberOfBits);
        boolean[] bits3 = showBits(longsFromBuffer, numberOfBits);
        if (!Arrays.equals(bits1, bits3)) {
            throw new AssertionError("Bug B");
        }

        final byte[] resultBytes = PackedBitArraysPer8.toByteArray(longs, numberOfBits);
        if (!Arrays.equals(bytes, resultBytes)) {
            throw new AssertionError("Bug C");
        }
    }
}
