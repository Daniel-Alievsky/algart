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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * <p>Test for {@link JArrays#getBytes8} and {@link JArrays#setBytes8}  methods.</p>
 *
 * @author Daniel Alievsky
 */
public class JArraysGetSetBytes8Test {
    // - A copy of org.scijava.util.Bytes.toLong method from scijava-common library
    private static long toLong(byte[] data, int pos, int count, boolean little) {
        long total = 0;
        for (int i = 0, ndx = pos; i < count; i++, ndx++) {
            total |= (data[ndx] < 0 ? 256L + data[ndx] : (long) data[ndx]) << ((little ? i : count - i - 1) * 8);
        }
        return total;
    }

    // - A copy of org.scijava.util.Bytes.unpack method from scijava-common library
    public static void unpack(byte[] data, int pos, int count, long value, boolean little) {
        if (data.length < pos + count) {
            throw new IllegalArgumentException("Invalid indices: data.length=" +
                    data.length + ", pos=" + pos + ", count=" + count);
        }
        if (little) {
            for (int i = 0; i < count; i++) {
                data[pos + i] = (byte) ((value >> (8 * i)) & 0xff);
            }
        } else {
            for (int i = 0; i < count; i++) {
                data[pos + i] = (byte) ((value >> (8 * (count - i - 1))) & 0xff);
            }
        }
    }

    private static void testUnpack(byte[] data, int pos, int count, long value, boolean little) {

    }

    public static void main(String[] args) {
        final int arrayLength = 160000;
        final int numberOfTests = 1000;
        Random rnd = new Random(157);
        final long valueForTestingSpeed = rnd.nextLong();
        byte[] data = new byte[arrayLength];
        for (int test = 1; test <= numberOfTests; test++) {
            System.out.printf("\r%d... ", test);
            for (int k = 0; k < data.length; k++) {
                data[k] = (byte) rnd.nextInt();
            }
            byte[] clone = data.clone();
            long value = rnd.nextLong();
            int pos = rnd.nextInt(arrayLength + 1);
            int count = Math.min(rnd.nextInt(9), arrayLength - pos);
            ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            JArrays.setBytes8(data, pos, value, count, order);
            unpack(clone, pos, count, value, order == ByteOrder.LITTLE_ENDIAN);
            if (!Arrays.equals(data, clone)) {
                throw new AssertionError("Bug in setBytes8");
            }
            for (int m = 0; m < 100; m++) {
                pos = rnd.nextInt(arrayLength + 1);
                count = Math.min(rnd.nextInt(9), arrayLength - pos);
                long bytes8LE = JArrays.getBytes8(data, pos, count);
                long bytes8BE = JArrays.getBytes8InBigEndianOrder(data, pos, count);
                if (bytes8BE != JArrays.getBytes8(data, pos, count, ByteOrder.BIG_ENDIAN)) {
                    throw new AssertionError();
                }
                if (bytes8LE != JArrays.getBytes8(data, pos, count, ByteOrder.LITTLE_ENDIAN)) {
                    throw new AssertionError();
                }
                if (bytes8LE != toLong(data, pos, count, true)) {
                    throw new AssertionError("Bug in getBytes8");
                }
                if (bytes8BE != toLong(data, pos, count, false)) {
                    throw new AssertionError("Bug in getBytes8InBigEndianOrder");
                }
                for (int k = 0; k < count; k++) {
                    final long be = (bytes8BE >>> ((count - 1 - k) * 8)) & 0xFF;
                    final long le = (bytes8LE >>> (k * 8)) & 0xFF;
                    if (le != be) {
                        throw new AssertionError();
                    }
                    if (le != (data[pos + k] & 0xFF)) {
                        throw new AssertionError();
                    }
                }
            }
        }
        System.out.println("\rO'k       ");
        for (int test = 1; test <= 100; test++) {
            System.out.printf("%nTest #%d...%n", test);
            final int n = data.length / 8;
            final int aligned = n * 8;
            for (int bigEndian = 0; bigEndian <= 1; bigEndian++) {
                final boolean little = bigEndian == 0;
                final ByteOrder byteOrder = little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
                for (int count = 0; count <= 8; count++) {
                    long t1, t2;
                    t1 = System.nanoTime();
                    for (int k = 0; k < aligned; k += 8) {
                        unpack(data, k, count, valueForTestingSpeed, little);
                    }
                    t2 = System.nanoTime();
                    System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                            "unpack, " + byteOrder, count,
                            (t2 - t1) * 1e-6, (double) (t2 - t1) / n);

                    t1 = System.nanoTime();
                    for (int k = 0; k < aligned; k += 8) {
                        JArrays.setBytes8(data, k, valueForTestingSpeed, count, byteOrder);
                    }
                    t2 = System.nanoTime();
                    System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                            "setBytes8, " + byteOrder, count,
                            (t2 - t1) * 1e-6, (double) (t2 - t1) / n);

                    t1 = System.nanoTime();
                    if (little) {
                        for (int k = 0; k < aligned; k += 8) {
                            JArrays.setBytes8(data, k, valueForTestingSpeed, count);
                        }
                    } else {
                        for (int k = 0; k < aligned; k += 8) {
                            JArrays.setBytes8InBigEndianOrder(data, k, valueForTestingSpeed, count);
                        }
                    }
                    t2 = System.nanoTime();
                    System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                            little ? "setBytes8" : "setBytes8InBigEndianOrder", count,
                            (t2 - t1) * 1e-6, (double) (t2 - t1) / n);

                    long info1 = 0;
                    t1 = System.nanoTime();
                    for (int k = 0; k < aligned; k += 8) {
                        info1 += toLong(data, k, count, little);
                    }
                    t2 = System.nanoTime();
                    System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                            "toLong, " + byteOrder, count,
                            (t2 - t1) * 1e-6, (double) (t2 - t1) / n);

                    long info2 = 0;
                    t1 = System.nanoTime();
                    for (int k = 0; k < aligned; k += 8) {
                        info2 += JArrays.getBytes8(data, k, count, byteOrder);
                    }
                    t2 = System.nanoTime();
                    if (info2 != info1) {
                        throw new AssertionError();
                    }
                    System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                            "getBytes8, " + byteOrder, count,
                            (t2 - t1) * 1e-6, (double) (t2 - t1) / n);

                    info2 = 0;
                    t1 = System.nanoTime();
                    if (little) {
                        for (int k = 0; k < aligned; k += 8) {
                            info2 += JArrays.getBytes8(data, k, count);
                        }
                    } else {
                        for (int k = 0; k < aligned; k += 8) {
                            info2 += JArrays.getBytes8InBigEndianOrder(data, k, count);
                        }
                    }
                    t2 = System.nanoTime();
                    if (info2 != info1) {
                        throw new AssertionError();
                    }
                    System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                            little ? "getBytes8" : "getBytes8InBigEndianOrder", count,
                            (t2 - t1) * 1e-6, (double) (t2 - t1) / n);

                    if (count == 4) {
                        var buffer = ByteBuffer.wrap(data).order(byteOrder).asIntBuffer();
                        final int aligned4 = 2 * n;
                        info2 = 0;
                        t1 = System.nanoTime();
                            for (int k = 0; k < aligned4; k += 2) {
                                info2 += ((long) buffer.get(k)) & 0xFFFFFFFFL;
                            }
                        t2 = System.nanoTime();
                        if (info2 != info1) {
                            throw new AssertionError("IntBuffer / getBytes8 mismatch!");
                        }
                        System.out.printf("%-32s (%d bytes): %.3f ms, %.2f ns/call%n",
                                "IntBuffer.get(), " + byteOrder, count,
                                (t2 - t1) * 1e-6, (double) (t2 - t1) / n);
                    }
                    System.out.println();
                }
            }
        }
    }
}
