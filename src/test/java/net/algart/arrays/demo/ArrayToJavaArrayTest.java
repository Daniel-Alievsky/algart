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

public class ArrayToJavaArrayTest {
    private static void checkArray(Array array, boolean cannotBeWrapper) {
        Object ja = array.ja();
        Object ja2 = array.ja();
        assert ja != null && ja2 != null;
        final boolean wrapper = array.isJavaArrayWrapper();
        if (wrapper && cannotBeWrapper) {
            throw new AssertionError("Unexpected wrapper!");
        }
        final Object daArray = array instanceof DirectAccessible da && da.hasJavaArray() ? da.javaArray() : null;
        final int offset = array instanceof DirectAccessible da && da.hasJavaArray() ? da.javaArrayOffset() : -1;
        if (daArray == null) {
            System.out.printf("%s%n    not direct-accessible, ja() = %s, %s%n", array, ja, ja2);
            assert !wrapper;
        } else {
            assert ((DirectAccessible) array).javaArrayLength() == array.length() : "Invalid javaArrayLength()";
            int length = java.lang.reflect.Array.getLength(daArray);
            assert wrapper == (offset == 0 && length == array.length());
            System.out.printf("%s%n    DIRECT-ACCESSIBLE %s[%d] (%s), offset %d%s%n",
                    array,
                    daArray.getClass().getComponentType().getSimpleName(), length,
                    length == array.length() ? "the same length" : "DIFFERENT length",
                    offset,
                    wrapper ? ": WRAPPER" : "");
        }
        if (wrapper) {
            assert ja == daArray : ("ja()/DirectAccessible content mismatch");
            assert ja2 == ja;
        } else {
            assert ja != ja2;
        }
        checkJa(array);
        if (array instanceof BitArray bitArray) {
            checkBitArray(bitArray, cannotBeWrapper);
        }
        if (array instanceof PArray) {
            checkConversions((PArray) array);
        }
    }

    private static void checkBitArray(BitArray array, boolean cannotBeWrapper) {
        long[] ja = array.jaBit();
        long[] ja2 = array.jaBit();
        assert ja != null && ja2 != null;
        final boolean wrapper = array.isPackedBitArrayWrapper();
        if (wrapper && cannotBeWrapper) {
            throw new AssertionError("Unexpected packed bit wrapper!");
        }
        if (array.isJavaArrayWrapper() || array instanceof DirectAccessible) {
            throw new AssertionError("Cannot be usual wrapper for bits");
        }
        if (!wrapper) {
            System.out.printf("    not packed bit array wrapper, ja() = %s, %s%n", ja, ja2);
        } else {
            System.out.printf("    PACKED BIT ARRAY WRAPPER bit[%d] (%s)%n", array.length(), ja);
        }
        if (wrapper) {
            assert ja2 == ja;
        } else {
            assert ja != ja2;
        }
        checkJaBits(array);
    }

    private static void check(Array array, boolean cannotBeWrapper) {
        System.out.printf("%nTesting %s and its sub-arrays...%n", array);
        checkArray(array, cannotBeWrapper);
        checkArray(array.asImmutable(), true);
        checkArray(array.subArray(0, array.length()), cannotBeWrapper);
        checkArray(array.subArray(10, array.length()), true);
        checkArray(array.subArray(10, array.length()).asImmutable(), true);
        checkArray(array.subArr(1, 0), true);
    }

    private static void checkJa(Array array) {
        boolean wrapper = array.isJavaArrayWrapper();
        final Object ja = array.ja();
        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
        //           boolean ==> char,,byte,,short,,int,,long,,float,,double]]
        if (array instanceof BitArray a) {
            boolean[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (array instanceof CharArray a) {
            char[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        if (array instanceof ByteArray a) {
            byte[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        if (array instanceof ShortArray a) {
            short[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        if (array instanceof IntArray a) {
            int[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        if (array instanceof LongArray a) {
            long[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        if (array instanceof FloatArray a) {
            float[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        if (array instanceof DoubleArray a) {
            double[] j = a.ja();
            if (wrapper ? j != ja : !JArrays.arrayEquals(j, 0, ja, 0, j.length)) {
                throw new AssertionError();
            }
        }
        //[[Repeat.AutoGeneratedEnd]]
        if (wrapper && array instanceof ObjectArray<?> a) {
            Object[] j = a.ja();
            if (j != ja) {
                throw new AssertionError();
            }
        }
    }

    private static void checkJaBits(BitArray array) {
        final long[] ja = array.jaBit();
        final long[] j = array.toBit();
        if (!java.util.Arrays.equals(ja, j)) {
                throw new AssertionError();
            }
    }

    private static void checkConversions(PArray array) {
        boolean wrapper = array.isJavaArrayWrapper();
        final Object ja = array.ja();
        //[[Repeat() Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
        //           byte ==> char,,short,,int,,long,,float,,double]]
        byte[] bytes = array.toByte();
        if (array instanceof ByteArray a) {
            assert ja instanceof byte[];
            assert java.util.Arrays.equals(bytes, (byte[]) ja);
            assert java.util.Arrays.equals(bytes, a.toJavaArray());
        }
        byte[] bytesExt = new byte[bytes.length + 100];
        array.toByte(bytesExt);
        assert JArrays.arrayEquals(bytes, 0, bytesExt, 0, bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            assert checkByteEqual(bytes[i], array.getDouble(i)) : bytes[i] + " does not match " + array.getDouble(i);
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        char[] chars = array.toChar();
        if (array instanceof CharArray a) {
            assert ja instanceof char[];
            assert java.util.Arrays.equals(chars, (char[]) ja);
            assert java.util.Arrays.equals(chars, a.toJavaArray());
        }
        char[] charsExt = new char[chars.length + 100];
        array.toChar(charsExt);
        assert JArrays.arrayEquals(chars, 0, charsExt, 0, chars.length);
        for (int i = 0; i < chars.length; i++) {
            assert checkCharEqual(chars[i], array.getDouble(i)) : chars[i] + " does not match " + array.getDouble(i);
        }
        short[] shorts = array.toShort();
        if (array instanceof ShortArray a) {
            assert ja instanceof short[];
            assert java.util.Arrays.equals(shorts, (short[]) ja);
            assert java.util.Arrays.equals(shorts, a.toJavaArray());
        }
        short[] shortsExt = new short[shorts.length + 100];
        array.toShort(shortsExt);
        assert JArrays.arrayEquals(shorts, 0, shortsExt, 0, shorts.length);
        for (int i = 0; i < shorts.length; i++) {
            assert checkShortEqual(shorts[i], array.getDouble(i)) : shorts[i] + " does not match " + array.getDouble(i);
        }
        int[] ints = array.toInt();
        if (array instanceof IntArray a) {
            assert ja instanceof int[];
            assert java.util.Arrays.equals(ints, (int[]) ja);
            assert java.util.Arrays.equals(ints, a.toJavaArray());
        }
        int[] intsExt = new int[ints.length + 100];
        array.toInt(intsExt);
        assert JArrays.arrayEquals(ints, 0, intsExt, 0, ints.length);
        for (int i = 0; i < ints.length; i++) {
            assert checkIntEqual(ints[i], array.getDouble(i)) : ints[i] + " does not match " + array.getDouble(i);
        }
        long[] longs = array.toLong();
        if (array instanceof LongArray a) {
            assert ja instanceof long[];
            assert java.util.Arrays.equals(longs, (long[]) ja);
            assert java.util.Arrays.equals(longs, a.toJavaArray());
        }
        long[] longsExt = new long[longs.length + 100];
        array.toLong(longsExt);
        assert JArrays.arrayEquals(longs, 0, longsExt, 0, longs.length);
        for (int i = 0; i < longs.length; i++) {
            assert checkLongEqual(longs[i], array.getDouble(i)) : longs[i] + " does not match " + array.getDouble(i);
        }
        float[] floats = array.toFloat();
        if (array instanceof FloatArray a) {
            assert ja instanceof float[];
            assert java.util.Arrays.equals(floats, (float[]) ja);
            assert java.util.Arrays.equals(floats, a.toJavaArray());
        }
        float[] floatsExt = new float[floats.length + 100];
        array.toFloat(floatsExt);
        assert JArrays.arrayEquals(floats, 0, floatsExt, 0, floats.length);
        for (int i = 0; i < floats.length; i++) {
            assert checkFloatEqual(floats[i], array.getDouble(i)) : floats[i] + " does not match " + array.getDouble(i);
        }
        double[] doubles = array.toDouble();
        if (array instanceof DoubleArray a) {
            assert ja instanceof double[];
            assert java.util.Arrays.equals(doubles, (double[]) ja);
            assert java.util.Arrays.equals(doubles, a.toJavaArray());
        }
        double[] doublesExt = new double[doubles.length + 100];
        array.toDouble(doublesExt);
        assert JArrays.arrayEquals(doubles, 0, doublesExt, 0, doubles.length);
        for (int i = 0; i < doubles.length; i++) {
            assert checkDoubleEqual(doubles[i], array.getDouble(i)) : doubles[i] + " does not match " + array.getDouble(i);
        }
        //[[Repeat.AutoGeneratedEnd]]
        if (wrapper && array instanceof ObjectArray<?> a) {
            Object[] j = a.ja();
            if (j != ja) {
                throw new AssertionError();
            }
        }
    }

    private static class TestArray extends AbstractDoubleArray {
        final double[] ja;

        public TestArray(double[] ja) {
            super(ja.length, false);
            this.ja = ja;
        }

        @Override
        public double getDouble(long index) {
            return ja[(int) index];
        }
    }

    private static class TestDAArray extends TestArray implements DirectAccessible {
        public TestDAArray(double[] ja) {
            super(ja);
        }

        @Override
        public boolean hasJavaArray() {
            return true;
        }

        @Override
        public Object javaArray() {
            return ja;
        }

        @Override
        public int javaArrayOffset() {
            return 0;
        }

        @Override
        public int javaArrayLength() {
            return ja.length;
        }

        @Override
        public boolean isImmutable() {
            return false;
        }

        @Override
        public DoubleArray asImmutable() {
            return DoubleArray.as(ja).asImmutable();
        }
    }

    private static UpdatablePArray testFill(UpdatablePArray array) {
        final double scale = array.maxPossibleValue(1000.0);
        for (long p = 0; p < array.length(); p++) {
            if (array instanceof UpdatableBitArray a) {
                a.setBit(p, (p & 3) != 0);
            } else {
                double value = scale * p / (double) (array.length() - 1);
                if ((p & 3) == 0) {
                    value = -value;
                }
                array.setDouble(p, value);
            }
        }
//        System.out.println(Arrays.toString(array, ",", 5000));
        return array;
    }

    private static boolean checkByteEqual(byte test, double value) {
        return (test & 0xFF) == (int) clamp(value, 0, 255);
    }

    private static boolean checkCharEqual(char test, double value) {
        return test == (int) clamp(value, 0, 0xFFFF);
    }

    private static boolean checkShortEqual(short test, double value) {
        return (test & 0xFFFF) == (int) clamp(value, 0, 0xFFFF);
    }

    private static boolean checkIntEqual(int test, double value) {
        return test == (int) clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static boolean checkLongEqual(long test, double value) {
        return test == (long) clamp(value, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean checkFloatEqual(float test, double value) {
        return test == (float) value;
    }

    private static boolean checkDoubleEqual(double test, double value) {
        return test == value;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    private static void testAll() {
        final int n = 1011;
        for (MemoryModel mm : List.of(Arrays.SMM, BufferMemoryModel.getInstance(), LargeMemoryModel.getInstance())) {
            final boolean nonSMM = mm != Arrays.SMM;
            check(testFill(mm.newUnresizableByteArray(n)), nonSMM);
            check(testFill(mm.newUnresizableBitArray(n)), nonSMM);
            check(testFill(mm.newUnresizableCharArray(n)), nonSMM);
            check(testFill(mm.newUnresizableShortArray(n)), nonSMM);
            check(testFill(mm.newUnresizableIntArray(n)), nonSMM);
            check(testFill(mm.newUnresizableLongArray(n)), nonSMM);
            check(testFill(mm.newUnresizableFloatArray(n)), nonSMM);
            check(testFill(mm.newUnresizableDoubleArray(n)), nonSMM);
            check(testFill(mm.newEmptyCharArray(n + 100).length(n)), true);
            check(testFill(mm.newIntArray(n)), nonSMM);
            check(testFill(mm.newShortMatrix(100, 100).array()), nonSMM);
            if (mm.isElementTypeSupported(Object.class)) {
                check(mm.newObjectArray(String.class, 100), nonSMM);
            }
        }
        check(Arrays.nIntCopies(n, 1), true);
        check(Arrays.nBitCopies(100, false), true);
        check(Arrays.nObjectCopies(1033, null), true);
        double[] doubles = new double[n];
        testFill(DoubleArray.as(doubles));
        check(new TestArray(doubles), true);
        check(new TestDAArray(doubles), false);
    }

    public static void main(String[] args) throws InterruptedException {
        testAll();
        System.gc();
        Thread.sleep(500);
        // - after usage of LargeMemoryModel
        System.gc();
    }
}
