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

import net.algart.arrays.*;

import java.util.Random;

/**
 * <p>Test for <code>Arrays.n<i>Xxx</i>Copies.getNnn</code> methods.</p>
 *
 * @author Daniel Alievsky
 */
public class NCopiesGetTest {
    private static void testGetBits64(BitArray array, Random rnd) {
        int len = array.length32();
        int count = rnd.nextInt(65);
        long required = !array.getBit(0) ? 0
                : count == 0 ? 0
                : count == 64 ? -1
                : (1L << count) - 1L;
        int pos = rnd.nextInt(len - count + 1);
        long v = array.getBits64(pos, count);
        if (v != required) throw new AssertionError(count + ": " + v);
    }

    private static void testGet(PArray array) {
        if (array instanceof BitArray a) {
            int v = a.getBit(0) ? 1 : 0;
            if (a.getInt(0) != v) throw new AssertionError(v);
            if (a.getLong(0) != v) throw new AssertionError(v);
            if (a.getDouble(0) != v) throw new AssertionError(v);
        } else if (array instanceof ByteArray a) {
            int v = a.getByte(0);
            if (v != (v & 0xFF)) throw new AssertionError(v);
            if (a.getInt(0) != v) throw new AssertionError(v);
            if (a.getLong(0) != v) throw new AssertionError(v);
            if (a.getDouble(0) != v) throw new AssertionError(v);
        } else if (array instanceof CharArray a) {
            char v = a.getChar(0);
            if (a.getInt(0) != v) throw new AssertionError(v);
            if (a.getLong(0) != v) throw new AssertionError(v);
            if (a.getDouble(0) != v) throw new AssertionError(v);
        } else if (array instanceof ShortArray a) {
            int v = a.getShort(0);
            if (v != (v & 0xFFFF)) throw new AssertionError(v);
            if (a.getInt(0) != v) throw new AssertionError(v);
            if (a.getLong(0) != v) throw new AssertionError(v);
            if (a.getDouble(0) != v) throw new AssertionError(v);
        } else if (array instanceof IntArray a) {
            int v = a.getInt(0);
            if (a.getLong(0) != v) throw new AssertionError(v);
            if (a.getDouble(0) != v) throw new AssertionError(v);
        } else if (array instanceof LongArray a) {
            long v = a.getLong(0);
            if (a.getInt(0) != clamp(v, Integer.MIN_VALUE, Integer.MAX_VALUE))
                throw new AssertionError(v);
            if (a.getDouble(0) != (double) v) throw new AssertionError(v);
        } else if (array instanceof FloatArray a) {
            float v = a.getFloat(0);
            if (a.getDouble(0) != v) throw new AssertionError(v);
        }
    }

    private static double nexDouble(Random rnd) {
        return rnd.nextBoolean() ? 2 * rnd.nextDouble() - 1
                : rnd.nextBoolean() ? rnd.nextInt() + rnd.nextDouble()
                : rnd.nextBoolean() ? rnd.nextLong() + rnd.nextDouble()
                : 100000000 * (rnd.nextDouble() - 0.5);
    }

    // This function is added in Math only in Java 21
    static long clamp(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return Math.min(Math.max(value, min), max);
    }

    public static void main(String[] args) {
        final Random rnd = new Random(157);
        for (int test = 1; test <= 10000000; test++) {
            if (test % 100000 == 0) {
                System.out.print("\r" + test);
            }
            testGetBits64(Arrays.nBitCopies(100, rnd.nextBoolean()), rnd);
            testGet(Arrays.nBitCopies(100, rnd.nextBoolean()));
            testGet(Arrays.nByteCopies(100, (byte) rnd.nextInt()));
            testGet(Arrays.nCharCopies(100, (char) rnd.nextInt()));
            testGet(Arrays.nShortCopies(100, (short) rnd.nextInt()));
            testGet(Arrays.nIntCopies(100, rnd.nextInt()));
            testGet(Arrays.nLongCopies(100, rnd.nextLong()));
            testGet(Arrays.nFloatCopies(100, (float) nexDouble(rnd)));
            testGet(Arrays.nDoubleCopies(100, nexDouble(rnd)));
        }
        System.out.println(" O'k");
    }
}
