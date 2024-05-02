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
        final MutablePArray array = (MutablePArray) mm.newEmptyArray(elementType);
        final MutablePArray addedInt = (MutablePArray) mm.newEmptyArray(elementType);
        final MutablePArray addedLong = (MutablePArray) mm.newEmptyArray(elementType);
        final MutablePArray addedDouble = (MutablePArray) mm.newEmptyArray(elementType);
        for (int k = 0; k < arrayLength; k++) {
            int v = (int) longs[k];
            addedInt.add(v);
            addedLong.add((long) v);
            // - must be equivalent
        }
        if (!addedInt.equals(addedLong)) {
            throw new AssertionError("Bug in add(int)/add(long): " + addedInt);
        }
        addedInt.fill(0);
        addedLong.fill(1);
        for (int k = 0; k < arrayLength; k++) {
            int v = (int) longs[k];
            addedInt.setInt(k, v);
            addedLong.setLong(k, v);
            // - must be equivalent
        }
        if (!addedInt.equals(addedLong)) {
            throw new AssertionError("Bug in setInt/setLong: " + addedInt);
        }
        addedInt.clear();
        addedLong.clear();
        for (int k = 0; k < arrayLength; k++) {
            if (array instanceof BitStack a) {
                boolean v = longs[k] != 0;
                a.pushBit(v);
                addedInt.add(v ? 1 : 0);
                addedLong.add(v ? 1L : 0L);
                addedDouble.add(v ? 1.0 : 0.0);
                assert ((PFixedArray) addedInt).getInt(k) == (v ? 1 : 0) : v;
                assert ((PFixedArray) addedLong).getLong(k) == (v ? 1 : 0) : v;
                assert addedDouble.getDouble(k) == (v ? 1.0 : 0.0) : v;
            } else if (array instanceof ByteStack a) {
                byte v = (byte) longs[k];
                a.pushByte(v);
                addedInt.add((int) v);
                addedLong.add((long) v);
                addedDouble.add((double) v);
                assert ((PFixedArray) addedInt).getInt(k) == (v & 0xFF) : v;
                assert ((PFixedArray) addedLong).getLong(k) == (v & 0xFF) : v;
                assert addedDouble.getDouble(k) == (v & 0xFF) : v;
            } else if (array instanceof CharStack a) {
                char v = (char) longs[k];
                a.pushChar(v);
                addedInt.add((int) v);
                addedLong.add((long) v);
                addedDouble.add((double) v);
                assert ((PFixedArray) addedInt).getInt(k) == v : v;
                assert ((PFixedArray) addedLong).getLong(k) == v : v;
                assert addedDouble.getDouble(k) == v : v;
            } else if (array instanceof ShortStack a) {
                short v = (short) longs[k];
                a.pushShort(v);
                addedInt.add((int) v);
                addedLong.add((long) v);
                addedDouble.add((double) v);
                assert ((PFixedArray) addedInt).getInt(k) == (v & 0xFFFF) : v;
                assert ((PFixedArray) addedLong).getLong(k) == (v & 0xFFFf) : v;
                assert addedDouble.getDouble(k) == (v & 0xFFFF) : v;
            } else if (array instanceof IntStack a) {
                int v = (int) longs[k];
                a.pushInt(v);
                addedInt.add((int) v);
                addedLong.add((long) v);
                addedDouble.add((double) v);
                assert ((PFixedArray) addedInt).getInt(k) == v : v;
                assert ((PFixedArray) addedLong).getLong(k) == v : v;
                assert addedDouble.getDouble(k) == v : v;
            } else if (array instanceof LongStack a) {
                long v = longs[k];
                v = (long) (double) v;
                // - some long values cannot be exactly represented in double
                a.pushLong(v);
                addedInt.add((int) v);
                addedLong.add(v);
                addedDouble.add((double) v);
                assert ((PFixedArray) addedInt).getInt(k) == (int) v;
                assert ((PFixedArray) addedLong).getInt(k) ==
                        Arrays.truncate(v, Integer.MIN_VALUE, Integer.MAX_VALUE) : v;
                assert ((PFixedArray) addedLong).getLong(k) == v : v;
                assert addedDouble.getDouble(k) == v : v;
                addedInt.removeTop();
                addedInt.add(v);
                // adding long for further comparison
            } else if (array instanceof FloatStack a) {
                float v = (float) doubles[k];
                a.pushFloat(v);
                addedInt.add((int) v);
                addedLong.add((long) v);
                addedDouble.add((double) v);
                assert array.getDouble(k) == v : v;
                assert addedInt.getDouble(k) == (float) (int) v : v;
                assert addedLong.getDouble(k) == (float) (long) v : v;
                assert addedDouble.getDouble(k) == v : v;
            } else if (array instanceof DoubleStack a) {
                double v = doubles[k];
                a.pushDouble(v);
                addedInt.add((int) v);
                addedLong.add((long) v);
                addedDouble.add((double) v);
                assert array.getDouble(k) == v : v;
                assert addedInt.getDouble(k) == (int) v : v;
                assert addedLong.getDouble(k) == (double) (long) v : v;
                assert addedDouble.getDouble(k) == v : v;
            }
        }
        if (array instanceof PFixedArray) {
            if (!array.equals(addedInt)) {
                throw new AssertionError("Bug in add(int): " + addedInt);
            }
            if (!array.equals(addedLong)) {
                throw new AssertionError("Bug in add(long): " + addedLong);
            }
            if (!array.equals(addedDouble)) {
                throw new AssertionError("Bug in add(double): " + addedDouble);
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
