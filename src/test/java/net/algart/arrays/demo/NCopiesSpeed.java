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

import net.algart.arrays.*;

import java.util.Locale;

/**
 * <p>Test for <tt>Arrays.n<i>Xxx</i>Copies<tt> methods.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class NCopiesSpeed {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + NCopiesSpeed.class.getName()
                + " primitiveType|String|CombinedMulti|CombinedSingle arrayLength [numberOfIterations]");
            return;
        }
        long n = Long.parseLong(args[1]);
        int iterationsNumber = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        Array vZeroCopies =
            args[0].equals("boolean") ? Arrays.nBitCopies(n, false) :
            args[0].equals("char") ? Arrays.nCharCopies(n, (char)0) :
            args[0].equals("byte") ? Arrays.nByteCopies(n, (byte)0) :
            args[0].equals("short") ? Arrays.nShortCopies(n, (short)0) :
            args[0].equals("int") ? Arrays.nIntCopies(n, 0) :
            args[0].equals("long") ? Arrays.nLongCopies(n, 0L) :
            args[0].equals("float") ? Arrays.nFloatCopies(n, 0.0f) :
            args[0].equals("double") ? Arrays.nDoubleCopies(n, 0.0) :
            args[0].equals("String") ? Arrays.nNullCopies(n, String.class) :
            args[0].equals("CombinedMulti") || args[0].equals("CombinedSingle") ?
            Arrays.nObjectCopies(n, null) :
            null;
        if (vZeroCopies == null)
            throw new IllegalArgumentException("Unknown element type");
        Array vNonzeroCopies =
            args[0].equals("boolean") ? Arrays.nBitCopies(n, true) :
            args[0].equals("char") ? Arrays.nCharCopies(n, '*') :
            args[0].equals("byte") ? Arrays.nByteCopies(n, (byte)157) :
            args[0].equals("short") ? Arrays.nShortCopies(n, (short)28) :
            args[0].equals("int") ? Arrays.nIntCopies(n, 1) :
            args[0].equals("long") ? Arrays.nLongCopies(n, 1L) :
            args[0].equals("float") ? Arrays.nFloatCopies(n, 1.0f) :
            args[0].equals("double") ? Arrays.nDoubleCopies(n, 1.0) :
            args[0].equals("String") ? Arrays.nObjectCopies(n, "&") :
            args[0].equals("CombinedMulti") || args[0].equals("CombinedSingle") ?
            Arrays.nObjectCopies(n, new CombinedArraysDemo.Circle(11, 11, 11)) :
            null;
        Object testJavaArray1 = n == (int)n ?
            java.lang.reflect.Array.newInstance(vZeroCopies.elementType(), (int)n) : null;
        Object testJavaArray2 = n == (int)n ?
            java.lang.reflect.Array.newInstance(vZeroCopies.elementType(), (int)n) : null;
        MutableArray testArray1 = Arrays.SystemSettings.globalMemoryModel(vZeroCopies.elementType())
            .newEmptyArray(vZeroCopies.elementType(), n);
        testArray1.length(n);
        MutableArray testArray2 = (args[0].equals("CombinedMulti") ?
            CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerMulti(mm)) :
            args[0].equals("CombinedSingle") ?
            CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerSingle(mm)) :
            mm)
            .newEmptyArray(vZeroCopies.elementType(), n);
        testArray2.length(n);
        for (int count = 1; count <= iterationsNumber; count++) {
            System.out.println("*** ITERATION #" + count);
            Runtime rt = Runtime.getRuntime();
            System.out.printf(Locale.US, "Used memory: %.3f MB%n", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
            if (testJavaArray1 != null)
                System.arraycopy(testJavaArray2, 0, testJavaArray1, 0, (int)n);
            if (n > 1000000)
                System.out.println("Copying... ");
            testArray1.copy(testArray2);

            long t1 = System.nanoTime();
            Object zeroArray = testJavaArray1 != null ? Arrays.toJavaArray(vZeroCopies) : null;
            long t2 = System.nanoTime();
            Object nonzeroArray = testJavaArray1 != null ? Arrays.toJavaArray(vNonzeroCopies) : null;
            long t3 = System.nanoTime();
            if (testJavaArray1 != null)
                vZeroCopies.getData(0, testJavaArray1);
            long t4 = System.nanoTime();
            if (testJavaArray2 != null)
                vNonzeroCopies.getData(0, testJavaArray2);
            long t5 = System.nanoTime();
            if (n > 1000000)
                System.out.println("Clearing...");
            testArray1.copy(vZeroCopies);
            long t6 = System.nanoTime();
            if (n > 1000000)
                System.out.println("Copying...");
            testArray2.copy(vNonzeroCopies);
            long t7 = System.nanoTime();
            if (n > 1000000)
                System.out.println("1st equals (zero)...");
            boolean eqZero = testArray1 instanceof PArray ?
                ((PArray)testArray1).isZeroFilled() :
                vZeroCopies.equals(testArray1);
            long t8 = System.nanoTime();
            if (n > 1000000)
                System.out.println("2nd equals (non-zero)...");
            boolean eqNonzero = vNonzeroCopies.equals(testArray2);
            long t9 = System.nanoTime();
            if (n > 1000000)
                System.out.println("1st hashCode (zero)...");
            int hashZero = vZeroCopies.hashCode();
            long t10 = System.nanoTime();
            if (n > 1000000)
                System.out.println("2nd hashCode (non-zero)...");
            int hashNonzero = vNonzeroCopies.hashCode();
            long t11 = System.nanoTime();
            if (testJavaArray1 != null) {
                System.out.printf(Locale.US,
                    "toArray (zero array):                       %d ns, %.2f ns/element - %s%n",
                    t2 - t1, (t2 - t1) * 1.0 / n, zeroArray);
                System.out.printf(Locale.US,
                    "toArray (non-zero array):                   %d ns, %.2f ns/element - %s%n",
                    t3 - t2, (t3 - t2) * 1.0 / n, nonzeroArray);

                System.out.printf(Locale.US,
                    "getData (zero array):                       %d ns, %.2f ns/element - %s%n",
                    t4 - t3, (t4 - t3) * 1.0 / n, testJavaArray1);
                System.out.printf(Locale.US,
                    "getData (non-zero array):                   %d ns, %.2f ns/element - %s%n",
                    t5 - t4, (t5 - t4) * 1.0 / n, testJavaArray2);
            }
            System.out.printf(Locale.US,
                "clearing array:                             %d ns, %.2f ns/element - %s%n",
                t6 - t5, (t6 - t5) * 1.0 / n, testArray1);
            System.out.printf(Locale.US,
                "copy (non-zero array):                      %d ns, %.2f ns/element - %s%n",
                t7 - t6, (t7 - t6) * 1.0 / n, testArray2);
            System.out.printf(Locale.US,
                (testArray1 instanceof PArray ?
                "isZeroFilled():                             " :
                "equals (with Simple MM, zero array):        ") + "%d ns, %.2f ns/element - %s%n",
                t8 - t7, (t8 - t7) * 1.0 / n, testArray2);
            System.out.printf(Locale.US,
                "equals (with "
                + (CombinedMemoryModel.isCombinedArray(testArray2) ? "Combined MM, non-zero array):  "
                : "Simple MM, non-zero array):    ") + "%d ns, %.2f ns/element - %s%n",
                t9 - t8, (t9 - t8) * 1.0 / n, testArray2);
            System.out.printf(Locale.US,
                "hashCode (zero array):                      %d ns, %.2f ns/element - %H%n",
                t10 - t9, (t10 - t9) * 1.0 / n, hashZero);
            System.out.printf(Locale.US,
                "hashCode (non-zero array):                  %d ns, %.2f ns/element - %H%n",
                t11 - t10, (t11 - t10) * 1.0 / n, hashNonzero);
            if (!eqZero)
                throw new AssertionError("The bug #1 in zero arrays");
            if (!testArray1.equals(vZeroCopies))
                throw new AssertionError("The bug #2 in zero arrays");
            if (!eqNonzero)
                throw new AssertionError("The bug #3 in non-zero arrays");
            if (!testArray2.equals(vNonzeroCopies))
                throw new AssertionError("The bug #4 in non-zero arrays");
            int hashZero2 = testArray1.hashCode();
            if (hashZero != hashZero2)
                throw new AssertionError("The bug #5 in zero arrays: "
                    + Integer.toHexString(hashZero).toUpperCase()
                    + " != " + Integer.toHexString(hashZero2).toUpperCase());
            int hashNonzero2 = testArray2.hashCode();
            if (hashNonzero != hashNonzero2)
                throw new AssertionError("The bug #6 in non-zero arrays"
                    + Integer.toHexString(hashNonzero).toUpperCase()
                    + " != " + Integer.toHexString(hashNonzero2).toUpperCase());
            System.out.println("Zero array:      " + vZeroCopies
                + " (" + Arrays.toString(vZeroCopies, "; ", 40) + ")");
            System.out.println("Non-zero array:  " + vNonzeroCopies
                + " (" + Arrays.toString(vNonzeroCopies, "; ", 40) + ")");
            System.out.println();
       }
    }
}
