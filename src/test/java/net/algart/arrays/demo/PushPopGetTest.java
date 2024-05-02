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

import net.algart.arrays.*;

import java.util.List;
import java.util.Random;

/**
 * <p>Test for <tt>MutablePArrays.getNnn/popNnn/pushNnn/add<tt> methods.</p>
 *
 * @author Daniel Alievsky
 */
public class PushPopGetTest {
    private MemoryModel mm;

    private final int arrayLength;
    private final long[] longs;
    private final double[] doubles;

    PushPopGetTest(Random rnd, int arrayLength) {
        if (arrayLength <= 0) {
            throw new IllegalArgumentException("Zero or negative array length");
        }
        this.arrayLength = arrayLength;
        this.longs = new long[arrayLength];
        this.doubles = new double[arrayLength];
        for (int k = 0; k <arrayLength; k++) {
            longs[k] = rnd.nextBoolean() ? 0 : rnd.nextLong();
            doubles[k] = rnd.nextBoolean() ? 2 * rnd.nextDouble() - 1
                    : rnd.nextBoolean() ? rnd.nextInt() + rnd.nextDouble()
                    : rnd.nextBoolean() ? rnd.nextLong() + rnd.nextDouble()
                    : 100000000 * (rnd.nextDouble() - 0.5);
        }
    }

    public PushPopGetTest setMm(MemoryModel mm) {
        this.mm = mm;
        return this;
    }

    private void testArray(Class<?> elementType) {
        final MutablePArray array1 = (MutablePArray) mm.newEmptyArray(elementType);
        final MutablePArray array2 = (MutablePArray) mm.newEmptyArray(elementType);
        final MutablePArray array3 = (MutablePArray) mm.newEmptyArray(elementType);
        final MutablePArray array4 = (MutablePArray) mm.newEmptyArray(elementType);
        for (int k = 0; k < arrayLength; k++) {
            if (array1 instanceof BitStack a) {
                boolean v = longs[k] != 0;
                a.pushBit(v);
                array2.add(v ? 1 : 0);
                array3.add(v ? 1L : 0L);
                array4.add(v ? 1.0 : 0.0);
                assert ((PFixedArray) array2).getInt(k) == (v ? 1 : 0) : v;
                assert ((PFixedArray) array3).getLong(k) == (v ? 1 : 0) : v;
                assert array4.getDouble(k) == (v ? 1.0 : 0.0) : v;
            } else if (array1 instanceof ByteStack a) {
                byte v = (byte) longs[k];
                a.pushByte(v);
                array2.add((int) v);
                array3.add((long) v);
                array4.add((double) v);
                assert ((PFixedArray) array2).getInt(k) == (v & 0xFF) : v;
                assert ((PFixedArray) array3).getLong(k) == (v & 0xFF) : v;
                assert array4.getDouble(k) == (v & 0xFF) : v;
            } else if (array1 instanceof CharStack a) {
                char v = (char) longs[k];
                a.pushChar(v);
                array2.add((int) v);
                array3.add((long) v);
                array4.add((double) v);
                assert ((PFixedArray) array2).getInt(k) == v : v;
                assert ((PFixedArray) array3).getLong(k) == v : v;
                assert array4.getDouble(k) == v : v;
            } else if (array1 instanceof ShortStack a) {
                short v = (short) longs[k];
                a.pushShort(v);
                array2.add((int) v);
                array3.add((long) v);
                array4.add((double) v);
                assert ((PFixedArray) array2).getInt(k) == (v & 0xFFFF) : v;
                assert ((PFixedArray) array3).getLong(k) == (v & 0xFFFf) : v;
                assert array4.getDouble(k) == (v & 0xFFFF) : v;
            } else if (array1 instanceof IntStack a) {
                int v = (int) longs[k];
                a.pushInt(v);
                array2.add((int) v);
                array3.add((long) v);
                array4.add((double) v);
                assert ((PFixedArray) array2).getInt(k) == v : v;
                assert ((PFixedArray) array3).getLong(k) == v : v;
                assert array4.getDouble(k) == v : v;
            } else if (array1 instanceof LongStack a) {
                long v = longs[k];
                v = (long) (double) v;
                // - some long values cannot be exactly represented in double
                a.pushLong(v);
                array2.add(v);
                // - adding long instead of int to avoid precision lost
                array3.add(v);
                array4.add((double) v);
                assert ((PFixedArray) array2).getInt(k) ==
                        Arrays.truncate(v, Integer.MIN_VALUE, Integer.MAX_VALUE) : v;
                assert ((PFixedArray) array3).getLong(k) == v : v;
                assert array4.getDouble(k) == v : v;
            } else if (array1 instanceof FloatStack a) {
                float v = (float) doubles[k];
                a.pushFloat(v);
                array2.add((int) v);
                array3.add((long) v);
                array4.add((double) v);
                assert array1.getDouble(k) == v : v;
                assert array2.getDouble(k) == (float) (int) v : v;
                assert array3.getDouble(k) == (float) (long) v : v;
                assert array4.getDouble(k) == v : v;
            } else if (array1 instanceof DoubleStack a) {
                double v = doubles[k];
                a.pushDouble(v);
                array2.add((int) v);
                array3.add((long) v);
                array4.add((double) v);
                assert array1.getDouble(k) == v : v;
                assert array2.getDouble(k) == (int) v : v;
                assert array3.getDouble(k) == (double) (long) v : v;
                assert array4.getDouble(k) == v : v;
            }
        }
        if (array1 instanceof PFixedArray) {
            if (!array1.equals(array2)) {
                throw new AssertionError("Bug in add(int): " + array2);
            }
            if (!array1.equals(array3)) {
                throw new AssertionError("Bug in add(long): " + array3);
            }
            if (!array1.equals(array4)) {
                throw new AssertionError("Bug in add(double): " + array4);
            }
        }
        //TODO!! check popInt/Long/Double
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + PushPopGetTest.class.getName() + " arrayLength numberOfTests");
            return;
        }
        int arrayLength = Integer.parseInt(args[0]);
        int numberOfTests = Integer.parseInt(args[1]);
        Random rnd = new Random(157);
        PushPopGetTest test = new PushPopGetTest(rnd, arrayLength);

        for (int testIndex = 1; testIndex <= numberOfTests; testIndex++) {
            if (testIndex % 100 == 0) {
                System.out.print("\r" + testIndex + " ");
                for (MemoryModel mm : List.of(Arrays.SMM, Arrays.BMM)) {
                    test.setMm(mm);
                    test.testArray(boolean.class);
                    test.testArray(byte.class);
                    test.testArray(char.class);
                    test.testArray(short.class);
                    test.testArray(int.class);
                    test.testArray(long.class);
                    test.testArray(float.class);
                    test.testArray(double.class);
                }
            }
        }
        System.out.println("O'k");
    }
}
