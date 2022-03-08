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
 * <p>Test for overflow in {@link Arrays#preciseSumOf(PFixedArray, boolean)} method.</p>
 *
 * @author Daniel Alievsky
 */
public class PreciseSumOverflowTest {
    static void test(PArray array) {
        if (array instanceof PFixedArray) {
            boolean overflow = false;
            long pSum = -1;
            try {
                pSum = Arrays.preciseSumOf((PFixedArray)array, true);
            } catch (ArithmeticException ex) {
                overflow = true;
            }
            double sum = Arrays.sumOf(array);
            System.out.printf(Locale.US, Arrays.toString(array, ", ", 10000) + ": %n    "
                + (overflow ? "OVERFLOW" : "sum = " + pSum) + ", ~= %.3f%n", sum);
        } else {
            double sum = Arrays.sumOf(array);
            System.out.printf(Locale.US, Arrays.toString(array, ", ", 10000) + ": %n    sum ~= %.3f%n", sum);
        }
    }

    public static void main(String[] args) {
        Object[] arrays = new Object[] {
            new byte[] {-100, -100},
            new int[] {Integer.MAX_VALUE, 1},
            new long[] {Long.MAX_VALUE, -100, 99},
            new long[] {Long.MAX_VALUE, 100, 99},
            new long[] {Long.MIN_VALUE, 100, Long.MIN_VALUE},
            new long[] {Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE},
            new long[] {Long.MIN_VALUE + 1, Long.MAX_VALUE, Long.MIN_VALUE},
            new double[] {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE},
        };
        for (int k = 0; k < arrays.length; k++)
            test((PArray)SimpleMemoryModel.asUpdatableArray(arrays[k]));
    }
}
