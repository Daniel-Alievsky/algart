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

import net.algart.arrays.*;

import java.util.List;
import java.util.Random;

/**
 * <p>Test for <code>MutablePArrays.getNnn/popNnn/pushNnn/add</code> methods.</p>
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
        for (int k = 0; k < arrayLength; k++) {
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

        // Check contract of addInt
        for (int k = 0; k < arrayLength; k++) {
            int v = (int) longs[k];
            addedInt.addInt(v);
            array.length(k + 1).setInt(k, v);
        }
        if (!addedInt.equals(array)) {
            throw new AssertionError("Bug in addInt/setInt: " + addedInt);
        }
        addedInt.clear();
        array.clear();

        // Check contract of addLong
        for (int k = 0; k < arrayLength; k++) {
            long v = longs[k];
            addedLong.addLong(v);
            array.length(k + 1).setLong(k, v);
        }
        if (!addedLong.equals(array)) {
            throw new AssertionError("Bug in addLong/setLong: " + addedLong);
        }
        addedLong.clear();
        array.clear();

        // Check contract of addDouble
        for (int k = 0; k < arrayLength; k++) {
            double v = doubles[k];
            addedDouble.addDouble(v);
            array.length(k + 1).setDouble(k, v);
        }
        if (!addedDouble.equals(array)) {
            throw new AssertionError("Bug in addDouble/setDouble: " + addedDouble);
        }
        addedDouble.clear();
        array.clear();

        // Check equivalence of addInt/addLong/addDouble
        for (int k = 0; k < arrayLength; k++) {
            int v = (int) longs[k];
            addedInt.addInt(v);
            addedLong.addLong(v);
            addedDouble.addDouble(v);
            // - must be equivalent
        }
        if (!addedInt.equals(addedLong)) {
            throw new AssertionError("Bug in addInt/addLong: " + addedInt);
        }
        if (!addedInt.equals(addedDouble)) {
            throw new AssertionError("Bug in addInt/addDouble: " + addedInt);
        }

        // Check equivalence of setInt/setLong/setDouble
        addedInt.fill(0);
        addedLong.fill(1);
        addedDouble.fill(Double.NaN);
        for (int k = 0; k < arrayLength; k++) {
            int v = (int) longs[k];
            addedInt.setInt(k, v);
            addedLong.setLong(k, v);
            addedDouble.setDouble(k, v);
            // - must be equivalent
        }
        if (!addedInt.equals(addedLong)) {
            throw new AssertionError("Bug in setInt/setLong: " + addedInt);
        }
        if (!addedInt.equals(addedDouble)) {
            throw new AssertionError("Bug in setInt/setDouble: " + addedInt);
        }
        addedInt.clear();
        addedLong.clear();
        addedDouble.clear();

        // Check semantics of pushXxx, addInt/addLong/addDouble, getInt/getLong/getDouble
        for (int k = 0; k < arrayLength; k++) {
            if (array instanceof MutableBitArray a) {
                boolean v = longs[k] != 0;
                a.addBit(v);
                addedInt.addInt(v ? 1 : 0);
                addedLong.addLong(v ? 1L : 0L);
                addedDouble.addDouble(v ? 1.0 : 0.0);
                if (((PFixedArray) addedInt).getInt(k) != (v ? 1 : 0)) throw new AssertionError(v);
                if (((PFixedArray) addedLong).getLong(k) != (v ? 1 : 0)) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != (v ? 1.0 : 0.0)) throw new AssertionError(v);
            } else if (array instanceof ByteStack a) {
                byte v = (byte) longs[k];
                a.pushByte(v);
                addedInt.addInt(v);
                addedLong.addLong(v);
                addedDouble.addDouble(v);
                if (((PFixedArray) addedInt).getInt(k) != (v & 0xFF)) throw new AssertionError(v);
                if (((PFixedArray) addedLong).getLong(k) != (v & 0xFF)) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != (v & 0xFF)) throw new AssertionError(v);
            } else if (array instanceof CharStack a) {
                char v = (char) longs[k];
                a.pushChar(v);
                addedInt.addInt(v);
                addedLong.addLong(v);
                addedDouble.addDouble(v);
                if (((PFixedArray) addedInt).getInt(k) != v) throw new AssertionError((int) v);
                if (((PFixedArray) addedLong).getLong(k) != v) throw new AssertionError((int) v);
                if (addedDouble.getDouble(k) != v) throw new AssertionError((int) v);
            } else if (array instanceof ShortStack a) {
                short v = (short) longs[k];
                a.pushShort(v);
                addedInt.addInt(v);
                addedLong.addLong(v);
                addedDouble.addDouble(v);
                if (((PFixedArray) addedInt).getInt(k) != (v & 0xFFFF)) throw new AssertionError(v);
                if (((PFixedArray) addedLong).getLong(k) != (v & 0xFFFf)) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != (v & 0xFFFF)) throw new AssertionError(v);
            } else if (array instanceof IntStack a) {
                int v = (int) longs[k];
                a.pushInt(v);
                addedInt.addInt(v);
                addedLong.addLong(v);
                addedDouble.addDouble(v);
                if (((PFixedArray) addedInt).getInt(k) != v) throw new AssertionError(v);
                if (((PFixedArray) addedLong).getLong(k) != v) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != v) throw new AssertionError(v);
            } else if (array instanceof LongStack a) {
                long v = longs[k];
                a.pushLong(v);
                addedInt.addInt((int) v);
                addedLong.addLong(v);
                addedDouble.addDouble(v);
                if (((PFixedArray) addedInt).getInt(k) != (int) v) throw new AssertionError();
                if (((PFixedArray) addedLong).getInt(k) != clampLongToInt(v)) throw new AssertionError(v);
                if (((PFixedArray) addedLong).getLong(k) != v) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != v) throw new AssertionError(v);
                addedInt.removeTop();
                addedInt.addLong(v);
                // adding long for further comparison
            } else if (array instanceof FloatStack a) {
                float v = (float) doubles[k];
                a.pushFloat(v);
                addedInt.addInt((int) v);
                addedLong.addLong((long) v);
                addedDouble.addDouble(v);
                if (array.getDouble(k) != v) throw new AssertionError(v);
                if (addedInt.getDouble(k) != (float) (int) v) throw new AssertionError(v);
                if (addedLong.getDouble(k) != (long) v) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != v) throw new AssertionError(v);
            } else if (array instanceof DoubleStack a) {
                double v = doubles[k];
                a.pushDouble(v);
                addedInt.addInt((int) v);
                addedLong.addLong((long) v);
                addedDouble.addDouble(v);
                if (array.getDouble(k) != v) throw new AssertionError(v);
                if (addedInt.getDouble(k) != (int) v) throw new AssertionError(v);
                if (addedLong.getDouble(k) != (double) (long) v) throw new AssertionError(v);
                if (addedDouble.getDouble(k) != v) throw new AssertionError(v);
            }
        }
        if (array instanceof PFixedArray) {
            if (!array.equals(addedInt)) {
                throw new AssertionError("Bug in addInt: " + addedInt);
            }
            if (!array.equals(addedLong)) {
                throw new AssertionError("Bug in addLong: " + addedLong);
            }
            if (!(array instanceof LongArray)) {
                // - some long values cannot be exactly represented in double
                if (!array.equals(addedDouble)) {
                    throw new AssertionError("Bug in addDouble: " + addedDouble);
                }
            }
        }
        // Check semantics of popXxx, popInt/popLong/popDouble
        for (int k = arrayLength - 1; k >= 0; k--) {
            if (array instanceof BitStack a) {
                boolean v = longs[k] != 0;
                if (a.popBit() != v) throw new AssertionError(v);
                if (((MutablePFixedArray) addedInt).popInt() != (v ? 1 : 0)) throw new AssertionError(v);
                if (((MutablePFixedArray) addedLong).popLong() != (v ? 1 : 0)) throw new AssertionError(v);
                if (addedDouble.popDouble() != (v ? 1.0 : 0.0)) throw new AssertionError(v);
            } else if (array instanceof ByteStack a) {
                byte v = (byte) longs[k];
                if (a.popByte() != v) throw new AssertionError(v);
                if (((MutablePFixedArray) addedInt).popInt() != (v & 0xFF)) throw new AssertionError(v);
                if (((MutablePFixedArray) addedLong).popLong() != (v & 0xFF)) throw new AssertionError(v);
                if (addedDouble.popDouble() != (v & 0xFF)) throw new AssertionError(v);
            } else if (array instanceof CharStack a) {
                char v = (char) longs[k];
                a.pushChar(v);
                if (a.popChar() != v) throw new AssertionError((int) v);
                if (((MutablePFixedArray) addedInt).popInt() != v) throw new AssertionError((int) v);
                if (((MutablePFixedArray) addedLong).popLong() != v) throw new AssertionError((int) v);
                if (addedDouble.popDouble() != v) throw new AssertionError((int) v);
            } else if (array instanceof ShortStack a) {
                short v = (short) longs[k];
                if (a.popShort() != v) throw new AssertionError(v);
                if (((MutablePFixedArray) addedInt).popInt() != (v & 0xFFFF)) throw new AssertionError(v);
                if (((MutablePFixedArray) addedLong).popLong() != (v & 0xFFFF)) throw new AssertionError(v);
                if (addedDouble.popDouble() != (v & 0xFFFF)) throw new AssertionError(v);
            } else if (array instanceof IntStack a) {
                int v = (int) longs[k];
                if (a.popInt() != v) throw new AssertionError(v);
                if (((MutablePFixedArray) addedInt).popInt() != v) throw new AssertionError(v);
                if (((MutablePFixedArray) addedLong).popLong() != v) throw new AssertionError(v);
                if (addedDouble.popDouble() != v) throw new AssertionError(v);
            } else if (array instanceof LongStack a) {
                long v = longs[k];
                if (a.popLong() != v) throw new AssertionError(v);
                if (((MutablePFixedArray) addedInt).popInt() != clampLongToInt(v)) throw new AssertionError(v);
                if (((MutablePFixedArray) addedLong).popLong() != v) throw new AssertionError(v);
                if (addedDouble.popDouble() != v) throw new AssertionError(v);
            } else if (array instanceof FloatStack a) {
                float v = (float) doubles[k];
                if (a.popFloat() != v) throw new AssertionError(v);
                if (addedInt.popDouble() != (float) (int) v) throw new AssertionError(v);
                if (addedLong.popDouble() != (long) v) throw new AssertionError(v);
                if (addedDouble.popDouble() != v) throw new AssertionError(v);
            } else if (array instanceof DoubleStack a) {
                double v = doubles[k];
                if (a.popDouble() != v) throw new AssertionError(v);
                if (addedInt.popDouble() != (int) v) throw new AssertionError(v);
                if (addedLong.popDouble() != (long) v) throw new AssertionError(v);
                if (addedDouble.popDouble() != v) throw new AssertionError(v);
            }
        }
    }

    private static long clampLongToInt(long v) {
        return NCopiesGetTest.clamp(v, Integer.MIN_VALUE, Integer.MAX_VALUE);
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
