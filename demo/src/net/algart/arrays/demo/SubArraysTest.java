/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Simple test for subarrays.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class SubArraysTest {
    private static void showArrays(String msg, Array a, Array aSub1, Array aSub2) {
        System.out.println(msg);
        DemoUtils.showArray("a:     ", a);
        DemoUtils.showArray("aSub1: ", aSub1);
        DemoUtils.showArray("aSub2: ", aSub2);
        System.out.println();
    }

    public static void main(String[] args) {
        boolean cnw = false;
        int startArgIndex = 0;
        if (args.length > 0 && args[startArgIndex].equalsIgnoreCase("-copyOnNextWrite")) {
            cnw = true;
            startArgIndex++;
        }
        if (args.length == startArgIndex) {
            System.out.println("Usage: " + SubArraysTest.class.getName()
                + " [-copyOnNextWrite] " + DemoUtils.possibleArg(false) + "|byteFunc");
            return;
        }
        Array aR, aOrig;
        boolean byteFunc = args[startArgIndex].equals("byteFunc");
        if (byteFunc) {
            aR = aOrig = new AbstractByteArray(Long.MAX_VALUE, false) {
                public int getByte(long k) {
                    return (int) k;
                }
            };
        } else {
            aR = aOrig = DemoUtils.createTestArray(args[startArgIndex], 15);
        }
        if (cnw) {
            aR = aOrig.asCopyOnNextWrite();
            // in SimpleMemoryModel, this array will be shown in toString() as "subarray":
            // it is an optimization ("subarrays" work little slower due to additional functionality)
            System.out.println("COPY-ON-NEXT-WRITE mode");
        }
        Array aSub1R, aSub2R;
        UpdatableArray a, aSub1U, aSub2U;
        MutableArray aSub1, aSub2;
        Array aConcatenation, aConcatenationR;

        aSub1R = aR.subArray(3, 8);
        aSub2R = aSub1R.subArr(2, 2);
        showArrays("aSub1 = a.subArray(3,8); aSub2 = aSub1.subArr(2,2) (-> resizable)", aR, aSub1R, aSub2R);
        if (aSub1R instanceof PArray) {
            long index = ((PArray) aSub1R).indexOf(Long.MAX_VALUE - 2, Long.MAX_VALUE,
                ((PArray) aR).getDouble(0));
            long lastIndex = ((PArray) aSub1R).lastIndexOf(Long.MAX_VALUE - 2, Long.MAX_VALUE,
                ((PArray) aR).getDouble(1));
            if (index != -1 || lastIndex != -1)
                throw new AssertionError("Strange indexOf/lastIndexOf: " + index + " and " + lastIndex);
        }
        if (!byteFunc) {
            a = (UpdatableArray) aR;
            aSub1U = (UpdatableArray) aSub1R;
            aSub2U = (UpdatableArray) aSub2R;
            Arrays.sort(aSub1U, Arrays.reverseOrderComparator(aSub1U));
            showArrays("Arrays.sort(aSub1, <decreasing>)", a, aSub1U, aSub2U);
            aSub1 = aSub1U.mutableClone(Arrays.SMM);
            aSub2 = aSub2U.mutableClone(Arrays.SMM);
            aSub1.length(0);
            showArrays("aSub1.length(0)", a, aSub1, aSub2);
            aSub2.length(0);
            showArrays("aSub2.length(0)", a, aSub1, aSub2);
            aSub1.append(a.subArray(0, 3));
            DemoUtils.changeTestArray(aSub1, 1, 11);
            showArrays("aSub1.append(a.subArray(0,3)); aSub1.set(1,11)", a, aSub1, aSub2);
            aSub2.length(1);
            DemoUtils.changeTestArray(aSub2, 0, 51);
            showArrays("aSub2.length(1); aSub2.set(0,51)", a, aSub1, aSub2);
            aSub1.trim();
            showArrays("aSub1.trim()", a, aSub1, aSub2);
            aSub1.length(0);
            aSub1.length(12);
            showArrays("aSub1.length(0); aSub1.length(12)", a, aSub1, aSub2);
            aConcatenation = Arrays.asConcatenation(
                a.asUnresizable(),
                aSub2.asUnresizable(),
                aSub1.asUnresizable(),
                aSub2.asUnresizable());
            DemoUtils.showArray("Concatenation a+aSub2+aSub1+aSub2: ", aConcatenation);
            System.out.println();
            aConcatenationR = Arrays.asConcatenation(
                a.asUnresizable(),
                aSub2R,
                aSub1R,
                aSub2R);
            DemoUtils.showArray("Concatenation a+aSub2R+aSub1R+aSub2R: ", aConcatenationR);
            System.out.println();
        }
        // Following assigning null allow garbage collection. Comment some of them to test LargeMemoryModel finalizers.
        aOrig = null;
        aR = null;
        aSub1R = null;
        aSub2R = null;
        a = null;
        aSub1U = null;
        aSub2U = null;
        aSub1 = null;
        aSub2 = null;
        aConcatenation = null;
        aConcatenationR = null;
        DemoUtils.fullGC();
        System.out.println();
        for (Array arr : new Array[]{aOrig, aR, aSub1R, aSub2R, a, aSub1U, aSub2U, aSub1, aSub2,
            aConcatenation, aConcatenationR})
        {
            // - this call is necessary to disallow JVM to free these arrays and to test correctness of arrays after gc
            System.out.println(arr + (arr != null && arr.length() > 0 ? ": " + arr.getElement(0) + ", ..." : ""));
        }
    }
}
