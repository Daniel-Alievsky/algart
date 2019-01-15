/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math;

import java.math.BigInteger;
import java.util.Random;

public class MutableInt128ToDoubleTest {
    static double maxToDoubleByAddingError = 0.0;

    static void testToDouble(MutableInt128 a, boolean print) {
        double standard = a.toBigInteger().doubleValue();
        final double v = a.toDouble();
        if (print) {
            System.out.printf("%s ~ %s%n", a, v);
        }
        if (v != standard) {
            throw new AssertionError("Error in toDouble(): " + a.toDouble()
                    + " instead of " + standard
                    + " for " + a + " = " + a.toString(16).toUpperCase() + "h");
        }
        final MutableInt128 b = MutableInt128.valueOfDouble(v);
        if (b.toDouble() != v) {
            throw new AssertionError("Error in toDouble()/valueOfDouble() for " + b
                    + ": " + b.toDouble() + " != " + v);
        }
        if (a.isExactlyConvertibleToDouble() && !b.equals(a)) {
            throw new AssertionError("Error in isExactlyConvertibleToDouble() for " + a
                    + " = " + a.toString(2) + "b: reverse valueOfDouble = "
                    + b.toString(2) + "b");
        }
        double w = a.toDoubleByAdding();
        double error = Math.abs(w - standard);
        if (error > 1e-10 * Math.abs(standard)) {
            throw new AssertionError("Error " + error / Math.abs(standard) + " in toDouble(): "
                    + a.toDoubleByAdding() + " instead of " + standard
                    + " for " + a + " = " + a.toString(16).toUpperCase() + "h");
        }
        if (standard != 0.0) {
            maxToDoubleByAddingError = Math.max(maxToDoubleByAddingError, error / standard);
        }
        a = a.clone().abs().and(MutableInt128.valueOfUnsigned(-1L));
        standard = a.toBigInteger().doubleValue();
        if (a.toDouble() != standard) {
            throw new AssertionError("Error in 64-bit toDouble(): " + a.toDouble()
                    + " instead of " + standard
                    + " for " + a + " = " + a.toString(16).toUpperCase() + "h");
        }
        if (MutableInt128.unsignedToDouble(a.low64Bits()) != standard) {
            throw new AssertionError("Error in unsignedToDouble " + a
                    + ": " + MutableInt128.unsignedToDouble(a.low64Bits()) + " instead of " + standard);
        }
    }

    public static void main(String[] args) {
        testToDouble(MutableInt128.valueOfBits(0xFFFFFFFFFFFFF4B0L, 0xF59F874F3F287DFL,
                false), true);
        testToDouble(MutableInt128.valueOfBits(0x17E7C76D0A7E5280L, 0,
                false), true);
        testToDouble(MutableInt128.valueOfBits(0,0xFFFFFFFFFFFFF4D0L, true), true);
        testToDouble(MutableInt128.valueOfBits(-1, -1, false), true);
        testToDouble(MutableInt128.valueOfBits(1, 0, true), true);
        testToDouble(MutableInt128.valueOfBits(0x7FFFFFFFFFFFFFFDL, 0x7FFFFFFFFFFFFFFEL,
                false), true);
        testToDouble(MutableInt128.valueOfBits(-1, Long.MIN_VALUE, false), true);

        Random rnd = new Random();
        long seed = rnd.nextLong();
        rnd.setSeed(seed);
        System.out.printf("%nTesting with rand-seed %s%n", seed);

        int n1 = 5000;
        int n2 = 5000;
        long[] longs = new long[n2];
        MutableInt128[] ints128 = new MutableInt128[n2];
        BigInteger[] bigIntegers = new BigInteger[n2];
        double[] doubles = new double[n2];
        double sum = 0.0; // some usage necessary to avoid (not 100%!) removing the code by JIT optimizer
        for (int test = 1; test <= 5; test++) {
            for (int i = 0; i < n2; i++) {
                ints128[i] = MutableInt128Test.randomInt128WithoutSpecialValues(rnd);
                bigIntegers[i] = ints128[i].toBigInteger();
                longs[i] = ints128[i].low64Bits();
            }
            long t1 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                for (int i = 0; i < n2; i++) {
                    doubles[i] = ints128[i].toDouble();
                }
            }
            for (int i = 0; i < n2; i++) {
                // this loop helps to avoid JVM optimization, when toDouble calls are removed at all
                sum += doubles[i];
            }
            long t2 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                for (int i = 0; i < n2; i++) {
                    doubles[i] = ints128[i].toDoubleByAdding();
                }
            }
            for (int i = 0; i < n2; i++) {
                // this loop helps to avoid JVM optimization, when toDouble calls are removed at all
                sum += doubles[i];
            }
            long t3 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                for (int i = 0; i < n2; i++) {
                    doubles[i] = bigIntegers[i].doubleValue();
                }
            }
            for (int i = 0; i < n2; i++) {
                // this loop helps to avoid JVM optimization, when toDouble calls are removed at all
                sum += doubles[i];
            }
            long t4 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                for (int i = 0; i < n2; i++) {
                    doubles[i] = MutableInt128.unsignedToDouble(longs[i]);
                }
            }
            for (int i = 0; i < n2; i++) {
                // this loop helps to avoid JVM optimization, when toDouble calls are removed at all
                sum += doubles[i];
            }
            long t5 = System.nanoTime();
            System.out.printf("%nTiming test #%d (total sum %f)%n", test, sum);
            System.out.printf("    toDouble(): %.5f ns%n", (t2 - t1) / (double) (n1 * n2));
            System.out.printf("    high-level analogue of toDouble(): %.5f ns%n", (t3 - t2) / (double) (n1 * n2));
            System.out.printf("    BigInteger.doubleValue(): %.5f ns%n", (t4 - t3) / (double) (n1 * n2));
            System.out.printf("    unsignedToDouble() static method: %.5f ns%n", (t5 - t4) / (double) (n1 * n2));
        }
        System.out.println();

        n1 = 50000;
        n2 = 20000;
        for (long k = 1, n = (long) n1 * (long) n2; k < n; k++) {
            if (k % n2 == 0) {
                System.out.printf("\rTest #%d (%.1f%%)", k, (k * 100.0) / n);
            }
            testToDouble(MutableInt128Test.randomInt128(rnd), false);
        }
        System.out.printf("\r%d tests done              %n", n1 * n2);
        System.out.printf("Maximal error of toDoubleByAdding(): %s%n", maxToDoubleByAddingError);
    }
}
