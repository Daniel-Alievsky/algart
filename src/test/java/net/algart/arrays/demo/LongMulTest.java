/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Arrays;

/**
 * <p>Test for {@link Arrays#longMul(long[])} method.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class LongMulTest {
    static String toHex(long n) {
        if (n >= 0)
            return Long.toHexString(n).toUpperCase();
        else
            return "-" + Long.toHexString(-n).toUpperCase();
    }

    static void test(long a, long b, boolean hex) {
        long c = Arrays.longMul(a, b);
        if (hex) {
            System.out.println("longMul(" + toHex(a) + ", " + toHex(b) + ") = "
                + (c == Long.MIN_VALUE ? " overflow " : toHex(c)));
        } else {
            System.out.println("longMul(" + a + ", " + b + ") = "
                + (c == Long.MIN_VALUE ? " overflow " : c));
        }
    }

    static void testSigned(long a, long b, boolean hex) {
        test(a, b, hex);
        test(-a, b, hex);
        test(a, -b, hex);
        test(-a, -b, hex);
    }

    public static void main(String[] args) {
        testSigned(1000L * 1000 * 1000, 2000L * 1000 * 1000, false);
        testSigned(3000L * 1000 * 1000, 1000L * 1000 * 1000, false);
        testSigned(4000L * 1000 * 1000, 4000L * 1000 * 1000, false);
        testSigned(0x7FFFFFFFL, 0xFFFFFFFFL, true);
        testSigned(Long.MAX_VALUE, 1, true);
        testSigned(1, Long.MAX_VALUE, true);
        testSigned(Long.MAX_VALUE, 2, true);
        testSigned(2, Long.MAX_VALUE, true);
        testSigned(0x80000000L, 0x100000000L, true);
        testSigned(Long.MIN_VALUE, 1, true);
        testSigned(Long.MIN_VALUE, 0, true);
        testSigned(0, Long.MIN_VALUE, true);
        testSigned(Long.MIN_VALUE, 157, true);
        testSigned(Long.MIN_VALUE, 4, true);
        System.out.println("longMul(1000) = " + Arrays.longMul(1000L));
        System.out.println("longMul() = " + Arrays.longMul());
    }
}
