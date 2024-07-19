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

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * <p>Test for {@link JArrays#getBytes8} and {@link JArrays#setBytes8}  methods.</p>
 *
 * @author Daniel Alievsky
 */
public class JArraysGetSetBytes8Test {


    private static long packSimple(byte[] data, int pos, int count, boolean little) {
        long total = 0;
        for (int i = 0, ndx = pos; i < count; i++, ndx++) {
            total |= (data[ndx] < 0 ? 256L + data[ndx] : (long) data[ndx]) << ((little ? i : count - i - 1) * 8);
        }
        return total;
    }

    public static void unpackSimple(byte[] data, int pos, int count, long value, boolean little) {
        if (data.length < pos + count) {
            throw new IllegalArgumentException("Invalid indices: data.length=" +
                    data.length + ", pos=" + pos + ", count=" + count);
        }
        if (little) {
            for (int i = 0; i < count; i++) {
                data[pos + i] = (byte) ((value >> (8 * i)) & 0xff);
            }
        }
        else {
            for (int i = 0; i < count; i++) {
                data[pos + i] = (byte) ((value >> (8 * (count - i - 1))) & 0xff);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + JArraysGetSetBytes8Test.class.getName() +
                    " arrayLength numberOfTests");
            return;
        }

        int arrayLength = Integer.parseInt(args[0]);
        int numberOfTests = Integer.parseInt(args[1]);
        Random rnd = new Random(157);
        byte[] data = new byte[arrayLength];
        for (int test = 1; test <= numberOfTests; test++) {
            System.out.printf("\r%d... ", test);
            for (int k = 0; k < data.length; k++) {
                data[k] = (byte) rnd.nextInt();
            }
            byte[] clone = data.clone();
            long value = rnd.nextLong();
            int pos = rnd.nextInt(arrayLength);
            int count = Math.min(rnd.nextInt(9), arrayLength - pos);
            ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            JArrays.setBytes8(data, pos, value, count, order);
            unpackSimple(clone, pos, count, value, order == ByteOrder.LITTLE_ENDIAN);
            if (!Arrays.equals(data, clone)) {
                throw new AssertionError("Bug in setBytes8");
            }
            for (int m = 0; m < 100; m++) {
                pos = rnd.nextInt(arrayLength);
                count = Math.min(rnd.nextInt(9), arrayLength - pos);
                long bytes8LE = JArrays.getBytes8(data, pos, count);
                long bytes8BE = JArrays.getBytes8InBigEndianOrder(data, pos, count);
                if (bytes8BE != JArrays.getBytes8(data, pos, count, ByteOrder.BIG_ENDIAN))
                    throw new AssertionError();
                if (bytes8LE != JArrays.getBytes8(data, pos, count, ByteOrder.LITTLE_ENDIAN))
                    throw new AssertionError();
                if (bytes8LE != packSimple(data, pos, count, true)) {
                    throw new AssertionError("Bug in getBytes8");
                }
                if (bytes8BE != packSimple(data, pos, count, false)) {
                    throw new AssertionError("Bug in getBytes8InBigEndianOrder");
                }
            }
        }
        System.out.println("\rO'k       ");
        //TODO!! test speed
    }
}
