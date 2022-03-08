/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.zip.*;
import java.util.Random;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>Simple test for AlgART array hashCode and equals methods.</p>
 *
 * @author Daniel Alievsky
 */
public class HashCodeEqualsTest {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + HashCodeEqualsTest.class.getName() + " ALL|"
                + DemoUtils.possibleArg(true) + " arrayLength [numberOfIterations]");
            return;
        }
        System.out.println(Arrays.SystemSettings.globalMemoryModel().toString());
        System.out.println();

        String passedElementTypeName = args[0];
        String[] allTypeNames = passedElementTypeName.equalsIgnoreCase("all") ?
            DemoUtils.allElementTypes :
            new String[] {passedElementTypeName};
        int arrayLen = Integer.parseInt(args[1]);
        int numberOfIterations = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        ElementTypeLoop:
        for (String elementTypeName : allTypeNames) {
            for (int count = 1; count <= numberOfIterations; count++) {
                System.out.println("*** ITERATION #" + count + " for " + elementTypeName);
                UpdatableArray a1;
                try {
                    a1 = DemoUtils.createTestResizableIfPossibleArray(elementTypeName, arrayLen);
                    if (a1.length() > 12) {
                        DemoUtils.changeTestArray(a1, a1.length() - 11, 0);
                        DemoUtils.changeTestArray(a1, 12, 0);
                    }
                } catch (UnsupportedElementTypeException ex) {
                    System.out.println("This type is not supported by this memory model");
                    System.out.println();
                    continue ElementTypeLoop;
                }
                UpdatableArray a2 = a1.updatableClone(Arrays.SystemSettings.globalMemoryModel());
                UpdatableArray a3 = SimpleMemoryModel.getInstance().newArray(a1.elementType(), a1.length());
                a3.copy(a1);

                Object arr1 = Arrays.toJavaArray(a1);
                System.out.println("Testing hashCode algorithm...");
                int hA = a1.hashCode();
                int hJA = JArrays.arrayHashCode(arr1, 0, arrayLen);
                if (!(a1 instanceof BitArray) && hA != hJA)
                    throw new AssertionError(
                        "The bug found: array hash code is not based on net.algart.arrays.JArrays");
                Random rnd = new Random();
                for (int m = 0; m < 10; m++) {
                    Checksum hash = new CRC32();
                    for (int fromIndex = 0; fromIndex < arrayLen; ) {
                        int toIndex = fromIndex + rnd.nextInt(arrayLen + 1 - fromIndex);
                        JArrays.updateArrayHashCode(arr1, fromIndex, toIndex, hash);
                        fromIndex = toIndex;
                    }
                    if ((int)hash.getValue() != hJA)
                        throw new AssertionError("The bug found (test #" + m
                            + "): sequential calls of updateArrayHashCode lead to another hash");
                }
                System.out.println("Testing OK");

                for (int m = 0; m < (a1.length() < 20 ? 2 : 5); m++) {
                    if (m == 3) {
                        DemoUtils.changeTestArray(a2, a2.length() - 11, a2 instanceof BitArray ? 1 : -117);
                        DemoUtils.changeTestArray(a3, a3.length() - 11, a3 instanceof BitArray ? 1 : -37);
                        System.out.println("a2 and a3 have been modified at " + (a3.length() - 11));
                    }
                    if (m == 4) {
                        DemoUtils.changeTestArray(a2, 12, a2 instanceof BitArray ? 1 : -117);
                        DemoUtils.changeTestArray(a3, 12, a3 instanceof BitArray ? 1 : -37);
                        System.out.println("a2 and a3 have been modified at " + 12);
                    }
                    int fromIndex = m <= 1 ? 0 : 10;
                    int toIndex = m <= 1 ? arrayLen : arrayLen - 10;
                    Object arr = m <= 1 ? arr1 : JArrays.copyOfRange(arr1, fromIndex, toIndex);
                    Array ar1 = m == 0 ? a1.asImmutable() : m == 1 ? a1.asCopyOnNextWrite() :
                        a1.subArray(10, a1.length() - 10);
                    Array ar2 = m == 0 ? a2.asImmutable() : m == 1 ? a2.asCopyOnNextWrite() :
                        a2.subArray(10, a2.length() - 10);
                    Array ar3 = m == 0 ? a3.asImmutable() : m == 1 ? a3.asCopyOnNextWrite() :
                        a3.subArray(10, a3.length() - 10);
                    System.out.println("Array a1: " + ar1);
                    System.out.println("Array a2: " + ar2);
                    System.out.println("Array a3: " + ar3);
                    long t1 = System.nanoTime();
                    int h1 = ar1.hashCode();
                    long t2 = System.nanoTime();
                    int h2 = ar2.hashCode();
                    long t3 = System.nanoTime();
                    int h3 = ar3.hashCode();
                    long t4 = System.nanoTime();
                    boolean e12 = ar1.equals(m == 0 ? a2 : ar2);
                    long t5 = System.nanoTime();
                    boolean e21 = ar2.equals(m == 0 ? a1 : ar1);
                    long t6 = System.nanoTime();
                    boolean e13 = ar1.equals(m == 0 ? a3 : ar3);
                    long t7 = System.nanoTime();
                    boolean e31 = ar3.equals(m == 0 ? a1 : ar1);
                    long t8 = System.nanoTime();
                    hJA = JArrays.arrayHashCode(arr1, fromIndex, toIndex);
                    long t9 = System.nanoTime();
                    int hAStd = arr instanceof boolean[] ? java.util.Arrays.hashCode((boolean[])arr) :
                        arr instanceof char[] ? java.util.Arrays.hashCode((char[])arr) :
                        arr instanceof byte[] ? java.util.Arrays.hashCode((byte[])arr) :
                        arr instanceof short[] ? java.util.Arrays.hashCode((short[])arr) :
                        arr instanceof int[] ? java.util.Arrays.hashCode((int[])arr) :
                        arr instanceof long[] ? java.util.Arrays.hashCode((long[])arr) :
                        arr instanceof float[] ? java.util.Arrays.hashCode((float[])arr) :
                        arr instanceof double[] ? java.util.Arrays.hashCode((double[])arr) :
                        arr instanceof Object[] ? java.util.Arrays.hashCode((Object[])arr) :
                        157;
                    long t10 = System.nanoTime();
                    System.out.printf(Locale.US, "a1.hashCode():       %.2f ms, %.2f ns/element - %H%n",
                        (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / ar1.length(), h1);
                    System.out.printf(Locale.US, "a2.hashCode():       %.2f ms, %.2f ns/element - %H%n",
                        (t3 - t2) * 1e-6, (t3 - t2) * 1.0 / ar2.length(), h2);
                    System.out.printf(Locale.US, "a3.hashCode():       %.2f ms, %.2f ns/element - %H%n",
                        (t4 - t3) * 1e-6, (t4 - t3) * 1.0 / ar3.length(), h3);
                    if (m < 3 && (h1 != h2 || h1 != h3))
                        throw new AssertionError("The bug #1 found");
                    System.out.printf(Locale.US, "a1.equals(a2):       %.2f ms, %.2f ns/element - %s%n",
                        (t5 - t4) * 1e-6, (t5 - t4) * 1.0 / ar1.length(), e12);
                    System.out.printf(Locale.US, "a2.equals(a1):       %.2f ms, %.2f ns/element - %s%n",
                        (t6 - t5) * 1e-6, (t6 - t5) * 1.0 / ar1.length(), e21);
                    System.out.printf(Locale.US, "a1.equals(a3):       %.2f ms, %.2f ns/element - %s%n",
                        (t7 - t6) * 1e-6, (t7 - t6) * 1.0 / ar1.length(), e13);
                    System.out.printf(Locale.US, "a3.equals(a1):       %.2f ms, %.2f ns/element - %s%n",
                        (t8 - t7) * 1e-6, (t8 - t7) * 1.0 / ar1.length(), e31);
                    System.out.printf(Locale.US, "arrayHashCode(arr1): %.2f ms, %.2f ns/element - %H%n",
                        (t9 - t8) * 1e-6, (t9 - t8) * 1.0 / ar1.length(), hJA);
                    System.out.printf(Locale.US, "std. hashCode(arr1): %.2f ms, %.2f ns/element - %H%n",
                        (t10 - t9) * 1e-6, (t10 - t9) * 1.0 / ar1.length(), hAStd);
                    if (m < 3 ? !(e12 && e21 && e13 && e31) : e12 || e21 || e13 || e31)
                        throw new AssertionError("The bug #2 found");
                    if (m < 3 && !(a1 instanceof BitArray) && h1 != hJA)
                        throw new AssertionError("The bug #3 found");
                    System.out.println();
                }
            }
        }
    }
}
