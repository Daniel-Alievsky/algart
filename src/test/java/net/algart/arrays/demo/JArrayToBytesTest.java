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

import net.algart.arrays.JArrays;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * <p>Basic test for {@link JArrays#charArrayToBytes(byte[], char[], int, ByteOrder)} and similar methods.</p>
 *
 * @author Daniel Alievsky
 */
public class JArrayToBytesTest {
    /*Repeat() char ==> short,,int,,long,,float,,double;;
               Char ==> Short,,Int,,Long,,Float,,Double;;
               2(\s*\*) ==> 2$1,,4$1,,8$1,,4$1,,8$1;;
               (\/\s*)2 ==> / 2,,/ 4,,/ 8,,/ 4,,/ 8 */

    private static void testCharArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        int count = rnd.nextInt(len / 2 + 1);
        char[] chars = rnd.nextBoolean() ? new char[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        char[] newChars1 = JArrays.bytesToCharArray(chars, serialized, count, order);
        assert newChars1 == chars || chars == null;
        Object newChars2 = JArrays.bytesToArray(chars, serialized, count, char.class, order);
        assert newChars2 == chars || chars == null;
        if (!JArrays.arrayEquals(newChars1, 0, newChars2, 0, count)) {
            throw new AssertionError("Bug A in bytesToCharArray/bytesToArray");
        }
        Object newChars3 = JArrays.bytesToArray(serialized, char.class, order);
        assert newChars3 != serialized;
        if (!JArrays.arrayEquals(newChars1, 0, newChars3, 0, count)) {
            throw new AssertionError("Bug B in bytesToCharArray/bytesToArray");
        }
        char[] newChars4 = JArrays.copy(chars, newChars1, count / 2);
        assert newChars4 == chars || chars == null;
        if (!JArrays.arrayEquals(newChars4, 0, newChars1, 0, count / 2)) {
            throw new AssertionError("Bug C in copy(char[],char[],int)");
        }
        char[] newChars5 = JArrays.copy(chars, newChars1);
        assert newChars5 == chars || chars == null;
        if (!java.util.Arrays.equals(newChars5, newChars1)) {
            throw new AssertionError("Bug D in copy(char[],char[],int)");
        }
        chars = newChars1;

        byte[] back = rnd.nextBoolean() ? new byte[2 * count + 2] : null;
        byte[] newBack1 = JArrays.charArrayToBytes(back, chars, count, order);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, chars, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug E in charArrayToBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(chars, order);
        if (!JArrays.arrayEquals(newBack1, 0, newBack3, 0, count)) {
            throw new AssertionError("Bug F in charArrayToBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug G in bytesToCharArray/charArrayToBytes");
        }

        final int m = Math.min(2 * count + (rnd.nextBoolean() ? 1 : 0), serialized.length);
        back = new byte[m];
        byte[] swapped = JArrays.copyAndSwapByteOrder(null, serialized, m, char.class);
        JArrays.bytesToCharArray(chars, serialized, m / 2, order);
        byte[] checkSwapped = JArrays.charArrayToBytes(null, chars, m / 2,
                order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assert swapped.length == checkSwapped.length || swapped.length == checkSwapped.length + 1;
        checkSwapped = Arrays.copyOf(checkSwapped, swapped.length);
        if (!Arrays.equals(swapped, checkSwapped)) {
            throw new AssertionError("Bug H in copyAndSwapByteOrder");
        }

        newBack1 = JArrays.copyAndSwapByteOrder(back, swapped, m, char.class);
        assert newBack1 == back;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, 2 * (m / 2))) {
            throw new AssertionError("Bug I in copyAndSwapByteOrder");
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private static void testShortArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        int count = rnd.nextInt(len / 2 + 1);
        short[] shorts = rnd.nextBoolean() ? new short[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        short[] newShorts1 = JArrays.bytesToShortArray(shorts, serialized, count, order);
        assert newShorts1 == shorts || shorts == null;
        Object newShorts2 = JArrays.bytesToArray(shorts, serialized, count, short.class, order);
        assert newShorts2 == shorts || shorts == null;
        if (!JArrays.arrayEquals(newShorts1, 0, newShorts2, 0, count)) {
            throw new AssertionError("Bug A in bytesToShortArray/bytesToArray");
        }
        Object newShorts3 = JArrays.bytesToArray(serialized, short.class, order);
        assert newShorts3 != serialized;
        if (!JArrays.arrayEquals(newShorts1, 0, newShorts3, 0, count)) {
            throw new AssertionError("Bug B in bytesToShortArray/bytesToArray");
        }
        short[] newShorts4 = JArrays.copy(shorts, newShorts1, count / 2);
        assert newShorts4 == shorts || shorts == null;
        if (!JArrays.arrayEquals(newShorts4, 0, newShorts1, 0, count / 2)) {
            throw new AssertionError("Bug C in copy(short[],short[],int)");
        }
        short[] newShorts5 = JArrays.copy(shorts, newShorts1);
        assert newShorts5 == shorts || shorts == null;
        if (!java.util.Arrays.equals(newShorts5, newShorts1)) {
            throw new AssertionError("Bug D in copy(short[],short[],int)");
        }
        shorts = newShorts1;

        byte[] back = rnd.nextBoolean() ? new byte[2 * count + 2] : null;
        byte[] newBack1 = JArrays.shortArrayToBytes(back, shorts, count, order);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, shorts, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug E in shortArrayToBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(shorts, order);
        if (!JArrays.arrayEquals(newBack1, 0, newBack3, 0, count)) {
            throw new AssertionError("Bug F in shortArrayToBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug G in bytesToShortArray/shortArrayToBytes");
        }

        final int m = Math.min(2 * count + (rnd.nextBoolean() ? 1 : 0), serialized.length);
        back = new byte[m];
        byte[] swapped = JArrays.copyAndSwapByteOrder(null, serialized, m, short.class);
        JArrays.bytesToShortArray(shorts, serialized, m / 2, order);
        byte[] checkSwapped = JArrays.shortArrayToBytes(null, shorts, m / 2,
                order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assert swapped.length == checkSwapped.length || swapped.length == checkSwapped.length + 1;
        checkSwapped = Arrays.copyOf(checkSwapped, swapped.length);
        if (!Arrays.equals(swapped, checkSwapped)) {
            throw new AssertionError("Bug H in copyAndSwapByteOrder");
        }

        newBack1 = JArrays.copyAndSwapByteOrder(back, swapped, m, short.class);
        assert newBack1 == back;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, 2 * (m / 2))) {
            throw new AssertionError("Bug I in copyAndSwapByteOrder");
        }
    }

    private static void testIntArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        int count = rnd.nextInt(len / 4 + 1);
        int[] ints = rnd.nextBoolean() ? new int[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        int[] newInts1 = JArrays.bytesToIntArray(ints, serialized, count, order);
        assert newInts1 == ints || ints == null;
        Object newInts2 = JArrays.bytesToArray(ints, serialized, count, int.class, order);
        assert newInts2 == ints || ints == null;
        if (!JArrays.arrayEquals(newInts1, 0, newInts2, 0, count)) {
            throw new AssertionError("Bug A in bytesToIntArray/bytesToArray");
        }
        Object newInts3 = JArrays.bytesToArray(serialized, int.class, order);
        assert newInts3 != serialized;
        if (!JArrays.arrayEquals(newInts1, 0, newInts3, 0, count)) {
            throw new AssertionError("Bug B in bytesToIntArray/bytesToArray");
        }
        int[] newInts4 = JArrays.copy(ints, newInts1, count / 4);
        assert newInts4 == ints || ints == null;
        if (!JArrays.arrayEquals(newInts4, 0, newInts1, 0, count / 4)) {
            throw new AssertionError("Bug C in copy(int[],int[],int)");
        }
        int[] newInts5 = JArrays.copy(ints, newInts1);
        assert newInts5 == ints || ints == null;
        if (!java.util.Arrays.equals(newInts5, newInts1)) {
            throw new AssertionError("Bug D in copy(int[],int[],int)");
        }
        ints = newInts1;

        byte[] back = rnd.nextBoolean() ? new byte[4 * count + 2] : null;
        byte[] newBack1 = JArrays.intArrayToBytes(back, ints, count, order);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, ints, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug E in intArrayToBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(ints, order);
        if (!JArrays.arrayEquals(newBack1, 0, newBack3, 0, count)) {
            throw new AssertionError("Bug F in intArrayToBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug G in bytesToIntArray/intArrayToBytes");
        }

        final int m = Math.min(4 * count + (rnd.nextBoolean() ? 1 : 0), serialized.length);
        back = new byte[m];
        byte[] swapped = JArrays.copyAndSwapByteOrder(null, serialized, m, int.class);
        JArrays.bytesToIntArray(ints, serialized, m / 4, order);
        byte[] checkSwapped = JArrays.intArrayToBytes(null, ints, m / 4,
                order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assert swapped.length == checkSwapped.length || swapped.length == checkSwapped.length + 1;
        checkSwapped = Arrays.copyOf(checkSwapped, swapped.length);
        if (!Arrays.equals(swapped, checkSwapped)) {
            throw new AssertionError("Bug H in copyAndSwapByteOrder");
        }

        newBack1 = JArrays.copyAndSwapByteOrder(back, swapped, m, int.class);
        assert newBack1 == back;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, 4 * (m / 4))) {
            throw new AssertionError("Bug I in copyAndSwapByteOrder");
        }
    }

    private static void testLongArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        int count = rnd.nextInt(len / 8 + 1);
        long[] longs = rnd.nextBoolean() ? new long[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        long[] newLongs1 = JArrays.bytesToLongArray(longs, serialized, count, order);
        assert newLongs1 == longs || longs == null;
        Object newLongs2 = JArrays.bytesToArray(longs, serialized, count, long.class, order);
        assert newLongs2 == longs || longs == null;
        if (!JArrays.arrayEquals(newLongs1, 0, newLongs2, 0, count)) {
            throw new AssertionError("Bug A in bytesToLongArray/bytesToArray");
        }
        Object newLongs3 = JArrays.bytesToArray(serialized, long.class, order);
        assert newLongs3 != serialized;
        if (!JArrays.arrayEquals(newLongs1, 0, newLongs3, 0, count)) {
            throw new AssertionError("Bug B in bytesToLongArray/bytesToArray");
        }
        long[] newLongs4 = JArrays.copy(longs, newLongs1, count / 8);
        assert newLongs4 == longs || longs == null;
        if (!JArrays.arrayEquals(newLongs4, 0, newLongs1, 0, count / 8)) {
            throw new AssertionError("Bug C in copy(long[],long[],int)");
        }
        long[] newLongs5 = JArrays.copy(longs, newLongs1);
        assert newLongs5 == longs || longs == null;
        if (!java.util.Arrays.equals(newLongs5, newLongs1)) {
            throw new AssertionError("Bug D in copy(long[],long[],int)");
        }
        longs = newLongs1;

        byte[] back = rnd.nextBoolean() ? new byte[8 * count + 2] : null;
        byte[] newBack1 = JArrays.longArrayToBytes(back, longs, count, order);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, longs, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug E in longArrayToBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(longs, order);
        if (!JArrays.arrayEquals(newBack1, 0, newBack3, 0, count)) {
            throw new AssertionError("Bug F in longArrayToBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug G in bytesToLongArray/longArrayToBytes");
        }

        final int m = Math.min(8 * count + (rnd.nextBoolean() ? 1 : 0), serialized.length);
        back = new byte[m];
        byte[] swapped = JArrays.copyAndSwapByteOrder(null, serialized, m, long.class);
        JArrays.bytesToLongArray(longs, serialized, m / 8, order);
        byte[] checkSwapped = JArrays.longArrayToBytes(null, longs, m / 8,
                order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assert swapped.length == checkSwapped.length || swapped.length == checkSwapped.length + 1;
        checkSwapped = Arrays.copyOf(checkSwapped, swapped.length);
        if (!Arrays.equals(swapped, checkSwapped)) {
            throw new AssertionError("Bug H in copyAndSwapByteOrder");
        }

        newBack1 = JArrays.copyAndSwapByteOrder(back, swapped, m, long.class);
        assert newBack1 == back;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, 8 * (m / 8))) {
            throw new AssertionError("Bug I in copyAndSwapByteOrder");
        }
    }

    private static void testFloatArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        int count = rnd.nextInt(len / 4 + 1);
        float[] floats = rnd.nextBoolean() ? new float[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        float[] newFloats1 = JArrays.bytesToFloatArray(floats, serialized, count, order);
        assert newFloats1 == floats || floats == null;
        Object newFloats2 = JArrays.bytesToArray(floats, serialized, count, float.class, order);
        assert newFloats2 == floats || floats == null;
        if (!JArrays.arrayEquals(newFloats1, 0, newFloats2, 0, count)) {
            throw new AssertionError("Bug A in bytesToFloatArray/bytesToArray");
        }
        Object newFloats3 = JArrays.bytesToArray(serialized, float.class, order);
        assert newFloats3 != serialized;
        if (!JArrays.arrayEquals(newFloats1, 0, newFloats3, 0, count)) {
            throw new AssertionError("Bug B in bytesToFloatArray/bytesToArray");
        }
        float[] newFloats4 = JArrays.copy(floats, newFloats1, count / 4);
        assert newFloats4 == floats || floats == null;
        if (!JArrays.arrayEquals(newFloats4, 0, newFloats1, 0, count / 4)) {
            throw new AssertionError("Bug C in copy(float[],float[],int)");
        }
        float[] newFloats5 = JArrays.copy(floats, newFloats1);
        assert newFloats5 == floats || floats == null;
        if (!java.util.Arrays.equals(newFloats5, newFloats1)) {
            throw new AssertionError("Bug D in copy(float[],float[],int)");
        }
        floats = newFloats1;

        byte[] back = rnd.nextBoolean() ? new byte[4 * count + 2] : null;
        byte[] newBack1 = JArrays.floatArrayToBytes(back, floats, count, order);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, floats, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug E in floatArrayToBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(floats, order);
        if (!JArrays.arrayEquals(newBack1, 0, newBack3, 0, count)) {
            throw new AssertionError("Bug F in floatArrayToBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug G in bytesToFloatArray/floatArrayToBytes");
        }

        final int m = Math.min(4 * count + (rnd.nextBoolean() ? 1 : 0), serialized.length);
        back = new byte[m];
        byte[] swapped = JArrays.copyAndSwapByteOrder(null, serialized, m, float.class);
        JArrays.bytesToFloatArray(floats, serialized, m / 4, order);
        byte[] checkSwapped = JArrays.floatArrayToBytes(null, floats, m / 4,
                order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assert swapped.length == checkSwapped.length || swapped.length == checkSwapped.length + 1;
        checkSwapped = Arrays.copyOf(checkSwapped, swapped.length);
        if (!Arrays.equals(swapped, checkSwapped)) {
            throw new AssertionError("Bug H in copyAndSwapByteOrder");
        }

        newBack1 = JArrays.copyAndSwapByteOrder(back, swapped, m, float.class);
        assert newBack1 == back;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, 4 * (m / 4))) {
            throw new AssertionError("Bug I in copyAndSwapByteOrder");
        }
    }

    private static void testDoubleArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        int count = rnd.nextInt(len / 8 + 1);
        double[] doubles = rnd.nextBoolean() ? new double[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        double[] newDoubles1 = JArrays.bytesToDoubleArray(doubles, serialized, count, order);
        assert newDoubles1 == doubles || doubles == null;
        Object newDoubles2 = JArrays.bytesToArray(doubles, serialized, count, double.class, order);
        assert newDoubles2 == doubles || doubles == null;
        if (!JArrays.arrayEquals(newDoubles1, 0, newDoubles2, 0, count)) {
            throw new AssertionError("Bug A in bytesToDoubleArray/bytesToArray");
        }
        Object newDoubles3 = JArrays.bytesToArray(serialized, double.class, order);
        assert newDoubles3 != serialized;
        if (!JArrays.arrayEquals(newDoubles1, 0, newDoubles3, 0, count)) {
            throw new AssertionError("Bug B in bytesToDoubleArray/bytesToArray");
        }
        double[] newDoubles4 = JArrays.copy(doubles, newDoubles1, count / 8);
        assert newDoubles4 == doubles || doubles == null;
        if (!JArrays.arrayEquals(newDoubles4, 0, newDoubles1, 0, count / 8)) {
            throw new AssertionError("Bug C in copy(double[],double[],int)");
        }
        double[] newDoubles5 = JArrays.copy(doubles, newDoubles1);
        assert newDoubles5 == doubles || doubles == null;
        if (!java.util.Arrays.equals(newDoubles5, newDoubles1)) {
            throw new AssertionError("Bug D in copy(double[],double[],int)");
        }
        doubles = newDoubles1;

        byte[] back = rnd.nextBoolean() ? new byte[8 * count + 2] : null;
        byte[] newBack1 = JArrays.doubleArrayToBytes(back, doubles, count, order);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, doubles, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug E in doubleArrayToBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(doubles, order);
        if (!JArrays.arrayEquals(newBack1, 0, newBack3, 0, count)) {
            throw new AssertionError("Bug F in doubleArrayToBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug G in bytesToDoubleArray/doubleArrayToBytes");
        }

        final int m = Math.min(8 * count + (rnd.nextBoolean() ? 1 : 0), serialized.length);
        back = new byte[m];
        byte[] swapped = JArrays.copyAndSwapByteOrder(null, serialized, m, double.class);
        JArrays.bytesToDoubleArray(doubles, serialized, m / 8, order);
        byte[] checkSwapped = JArrays.doubleArrayToBytes(null, doubles, m / 8,
                order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assert swapped.length == checkSwapped.length || swapped.length == checkSwapped.length + 1;
        checkSwapped = Arrays.copyOf(checkSwapped, swapped.length);
        if (!Arrays.equals(swapped, checkSwapped)) {
            throw new AssertionError("Bug H in copyAndSwapByteOrder");
        }

        newBack1 = JArrays.copyAndSwapByteOrder(back, swapped, m, double.class);
        assert newBack1 == back;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, 8 * (m / 8))) {
            throw new AssertionError("Bug I in copyAndSwapByteOrder");
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    private static void testByteArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] serialized = new byte[len];
        IntStream.range(0, serialized.length).forEach(k -> serialized[k] = (byte) rnd.nextInt());
        byte[] saved = serialized.clone();
        int count = rnd.nextInt(len  + 1);
        byte[] bytes = rnd.nextBoolean() ? new byte[count + 2] : null;
        ByteOrder order = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        byte[] newBytes1 = JArrays.copy(bytes, serialized, count);
        assert newBytes1 == bytes || bytes == null;
        if (!JArrays.arrayEquals(newBytes1, 0, saved, 0, count)) {
            throw new AssertionError("Bug A in copyBytes");        }
        Object newBytes2 = JArrays.bytesToArray(bytes, serialized, count, byte.class, order);
        assert newBytes2 == bytes || bytes == null;
        if (!JArrays.arrayEquals(newBytes1, 0, newBytes2, 0, count)) {
            throw new AssertionError("Bug B in copyBytes/bytesToArray");
        }
        Object newBytes3 = JArrays.bytesToArray(serialized, byte.class, order);
        assert newBytes3 != serialized;
        if (!java.util.Arrays.equals((byte[]) newBytes3, serialized)) {
            throw new AssertionError("Bug C in copyBytes/bytesToArray");
        }
        bytes = newBytes1;
        byte[] back = rnd.nextBoolean() ? new byte[ count + 2] : null;
        byte[] newBack1 = JArrays.copy(back, bytes, count);
        assert back == null || newBack1 == back;
        byte[] newBack2 = JArrays.arrayToBytes(back, bytes, count, order);
        assert back == null || newBack2 == back;
        if (!java.util.Arrays.equals(newBack1, newBack2)) {
            throw new AssertionError("Bug D in copyBytes/arrayToBytes");
        }
        byte[] newBack3 = JArrays.arrayToBytes(bytes, order);
        assert newBack3 != bytes;
        if (!java.util.Arrays.equals(bytes, newBack3)) {
            throw new AssertionError("Bug E in copyBytes/arrayToBytes");
        }
        back = newBack1;
        if (!JArrays.arrayEquals(back, 0, serialized, 0, count)) {
            throw new AssertionError("Bug F in copyBytes/copyBytes");
        }
    }

    private static void testBooleanArray(Random rnd, int arrayLength) {
        final int len = rnd.nextInt(arrayLength);
        byte[] bytes = new byte[len];
        IntStream.range(0, bytes.length).forEach(k -> bytes[k] = (byte) rnd.nextInt(3));
        int count = rnd.nextInt(len / 2 + 1);
        boolean[] booleans = rnd.nextBoolean() ? new boolean[count + 2] : null;
        boolean[] newBooleans = JArrays.bytesToBooleanArray(booleans, bytes, count);
        assert booleans == null || newBooleans == booleans;
        booleans = newBooleans;
        byte[] back = rnd.nextBoolean() ? new byte[2 * count + 2] : null;
        byte[] newBytes = JArrays.booleanArrayToBytes(back, booleans, count);
        assert back == null || newBytes == back;
        back = newBytes;
        for (int k = 0; k < count; k++) {
            boolean v = bytes[k] != 0;
            if (back[k] < 0 || back[k] > 1 || v != (back[k] == 1)) {
                throw new AssertionError("Bug in byteToBooleanArray/booleanToByteArray");
            }
        }
    }

    private static void miscellaneousTest(Random rnd, int arrayLength) {
        float[] data1 = new float[arrayLength];
        IntStream.range(0, data1.length).forEach(k -> data1[k] =  rnd.nextBoolean() ? 0 : rnd.nextFloat());

        // JArrays.arrayToBytes(new String[0], ByteOrder.LITTLE_ENDIAN);
        byte[] bytes1 = JArrays.arrayToBytes(data1, ByteOrder.LITTLE_ENDIAN);
        byte[] saved = bytes1.clone();
        float[] floats1 = JArrays.bytesToFloatArray(bytes1, ByteOrder.LITTLE_ENDIAN);
        assert java.util.Arrays.equals(data1, floats1);
        float[] floats2 = (float[]) JArrays.bytesToArray(bytes1, float.class, ByteOrder.LITTLE_ENDIAN);
        assert java.util.Arrays.equals(data1, floats2);
        byte[] bytes2 = JArrays.arrayToBytes(bytes1, ByteOrder.LITTLE_ENDIAN);
        assert bytes2 != bytes1 && java.util.Arrays.equals(bytes1, bytes2);
        java.util.Arrays.fill(bytes2, (byte) 0);
        byte[] bytes3 = JArrays.arrayToBytes(bytes2, bytes1, bytes1.length, ByteOrder.LITTLE_ENDIAN);
        assert bytes3 == bytes2 && java.util.Arrays.equals(saved, bytes2);
        Object data2 = JArrays.bytesToArray(bytes1, float.class, ByteOrder.LITTLE_ENDIAN);
        assert JArrays.arrayEquals(data2, 0, floats1, 0, floats1.length);
        Object data3 = JArrays.bytesToArray(new float[arrayLength], bytes1, arrayLength, float.class,
                ByteOrder.LITTLE_ENDIAN);
        assert JArrays.arrayEquals(data3, 0, floats1, 0, floats1.length);
        // JArrays.bytesToArray(bytes1, 10000000000L, float.class, ByteOrder.LITTLE_ENDIAN);
        assert JArrays.arrayEquals(data3, 0, floats1, 0, floats1.length);

        byte[] bytes4 = JArrays.copy(null, bytes1, bytes1.length);
        assert java.util.Arrays.equals(bytes4, bytes1);

        bytes4 = JArrays.copyAndSwapByteOrder(bytes1, byte.class);
        assert java.util.Arrays.equals(bytes4, bytes1);

        boolean[] booleans = JArrays.bytesToBooleanArray(bytes1);
        bytes2 = JArrays.arrayToBytes(booleans, ByteOrder.BIG_ENDIAN);
        for (int k = 0; k < data1.length; k++) {
            assert bytes2[k] == (booleans[k] ? 1 : 0);
        }
        bytes3 = JArrays.booleanArrayToBytes(booleans);
        assert java.util.Arrays.equals(bytes2, bytes3);
    }

    public static void main(String[] args) {
        final boolean superLarge = false;
        if (args.length < 2) {
            System.out.println("Usage: " + JArrayToBytesTest.class.getName()
                    + " arrayLength numberOfTests [randSeed]");
            return;
        }

        int arrayLength = Integer.parseInt(args[0]);
        int numberOfTests = Integer.parseInt(args[1]);
        if (arrayLength > Integer.MAX_VALUE / 10) {
            throw new IllegalArgumentException("Too large arrayLength = " + arrayLength);
        }
        long seed;
        if (args.length < 3) {
            seed = new Random().nextLong();
        } else {
            seed = Long.parseLong(args[2]);
        }

        Random rnd = new Random(seed);
        System.out.println("Testing " + arrayLength + " elements with start random seed " + seed);

        miscellaneousTest(rnd, arrayLength);

        for (int testIndex = 0; testIndex < numberOfTests; testIndex++) {
            testByteArray(rnd, arrayLength);
            testCharArray(rnd, arrayLength);
            testShortArray(rnd, arrayLength);
            testIntArray(rnd, arrayLength);
            testLongArray(rnd, arrayLength);
            testFloatArray(rnd, arrayLength);
            testDoubleArray(rnd, arrayLength);
            testBooleanArray(rnd, arrayLength);
            PackedBitArraysTest.showProgress(testIndex);
        }
        System.out.println("           ");
        System.out.println("All O'k: testing time "
                + (System.currentTimeMillis() - PackedBitArraysTest.tStart) + " ms");
    }
}
