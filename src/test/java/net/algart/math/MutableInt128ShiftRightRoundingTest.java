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

package net.algart.math;

import java.util.Locale;
import java.util.Random;

public class MutableInt128ShiftRightRoundingTest {
    private static long simpleRoundingRightShift(long a, int shift) {
        return (long) StrictMath.rint(StrictMath.scalb((double) a, -shift));
    }

    private static MutableInt128 shiftRightRoundingHighLevel(MutableInt128 value, int shift) {
        if (shift < 0) {
            throw new IllegalArgumentException("Negative shift");
        }
        if (shift == 0) {
            return value;
        }
        if (shift >= 128) {
            return MutableInt128.newZero();
        }
        boolean negative = value.isNegative();
        value.abs();
        final MutableInt128 middle = MutableInt128.newOne().shiftLeft(shift - 1);
        // 1L << (shift - 1)
        final MutableInt128 otherBits = value.clone().and(middle.clone().shiftLeft(1).subtract(MutableInt128.newOne()));
        // value & ((middle << 1) - 1)
        MutableInt128 result = value.clone().shiftRight(shift);
        if (otherBits.equals(middle) ? result.getBit(0) : otherBits.compareTo(middle) >= 0) {
            // otherBits>=middle is correct, because both numbers are non-negative
            result.addLong(1);
        }
        if (negative) {
            result.negate();
        }
        return result;
    }

    private static void testRightShiftRounding(MutableInt128 a, int shift, boolean print) {
        MutableInt128 shifted = a.clone().shiftRightRounding(shift);
        if (print) {
            System.out.printf("0x%s >>(rounding) %d = 0x%s%n",
                    a.toString(16), shift, shifted.toString(16));
        }
        MutableInt128 shiftedHighLevel = shiftRightRoundingHighLevel(a.clone(), shift);
        if (!shifted.equals(shiftedHighLevel)) {
            throw new AssertionError("Invalid shiftRightRounding(" + shift + ") for 0x"
                    + a.toString(16)
                    + ": 0x" + shifted.toString(16)
                    + " instead of 0x" + shiftedHighLevel.toString(16));
        }
        if (a.high64Bits() == 0) {
            final long longValue = a.low64Bits();
            final long longShifted = MutableInt128.unsignedShiftRightRounding(longValue, shift);
            if (shifted.low64Bits() != longShifted) {
                throw new AssertionError("shiftRightRounding for MutableInt128 and "
                        + "unsignedShiftRightRounding mismatch for 0x" + a.toString(16)
                        + ": MutableInt128 = 0x" + shifted.toString(16)
                        + ", unsigned long = 0x" + Long.toHexString(
                        MutableInt128.unsignedShiftRightRounding(a.toLongExact(), shift)));
            }
            if (longValue >= 0 && longShifted != MutableInt128.shiftRightRounding(longValue, shift)) {
                throw new AssertionError("shiftRightRounding and unsignedShiftRightRounding "
                        + "mismatch for non-negative " + Long.toHexString(longValue));
            }
        }
        if (a.isConvertibleToLong()) {
            final long longValue = a.toLongExact();
            final long longShifted = MutableInt128.shiftRightRounding(longValue, shift);
            if (longValue >= -(1L << 53) && longValue <= 1L << 53) {
                if (longShifted != simpleRoundingRightShift(a.toLongExact(), shift)) {
                    throw new AssertionError("Invalid shiftRightRounding(" + a.toLongExact() + ", "
                            + shift + ") for 0x" + a.toString(16)
                            + ": 0x" + Long.toHexString(MutableInt128.shiftRightRounding(a.toLongExact(), shift))
                            + " instead of floating-point version 0x"
                            + Long.toHexString(simpleRoundingRightShift(a.toLongExact(), shift)));
                }
            }
            if (shifted.toLongExact() != MutableInt128.shiftRightRounding(a.toLongExact(), shift)) {
                throw new AssertionError("shiftRightRounding for MutableInt128 and long "
                        + "mismatch for 0x" + a.toString(16)
                        + ": MutableInt128 = 0x" + shifted.toString(16)
                        + ", long = 0x" + Long.toHexString(MutableInt128.shiftRightRounding(a.toLongExact(), shift)));
            }
        }
        if (a.isExactlyConvertibleToDouble()) {
            final double d = StrictMath.rint(StrictMath.scalb(a.toDouble(), -shift));
            final double v = shifted.toDouble();
            assert shifted.isExactlyConvertibleToDouble();
            if (d != v) {
                throw new AssertionError(String.format(Locale.US,
                        "Invalid shiftRightRounding(%d) for 0x%s: 0x%s = %.1f instead of %.1f = 0x%s (%s)",
                        shift, a.toString(16),
                        a.clone().shiftRightRounding(shift).toString(16),
                        v, d, MutableInt128.valueOfDouble(d).toString(16),
                        a.isConvertibleToLong() ?
                                Long.toHexString(MutableInt128.shiftRightRounding(
                                        Math.abs(a.toLongExact()), shift)) : "n/a"));
            }
        }
    }


    private static void testCorrectness(int n, Random rnd) {
        for (int k = 0; k < n; k++) {
            if (k % 10000 == 0) {
                System.out.printf("\rTesting %.1f%%", (k * 100.0) / n);
            }
            final long v = rnd.nextBoolean() ?
                    (rnd.nextInt(1000) - 500) << rnd.nextInt(3) :
                    -(1L << 52) + rnd.nextLong() & ((1L << 53) - 1);
            //1L << 53 and less are represented by double absolutely exactly
            final int shift = rnd.nextInt(4);
            final long d = simpleRoundingRightShift(v, shift);
            final long i = MutableInt128.shiftRightRounding(v, shift);
            if (d != i) {
                throw new AssertionError("Invalid shiftRightRounding(0x"
                        + Long.toHexString(v) + ", " + shift + ") = 0x"
                        + Long.toHexString(MutableInt128.shiftRightRounding(v, shift))
                        + " instead of " + d + " = round(" + StrictMath.scalb((double) v, -shift) + ")");
            }
            testRightShiftRounding(MutableInt128Test.randomInt128(rnd), shift, false);
        }
        System.out.print("\r                           \r");
    }

    public static void main(String[] args) throws InterruptedException {
        for (int k = -16; k <= 16; k += 4) {
            assert MutableInt128.shiftRightRounding(k, 1) == k / 2;
            assert MutableInt128.shiftRightRounding(k + 1, 1) == k / 2;
            assert MutableInt128.shiftRightRounding(k + 2, 1) == k / 2 + 1;
            assert MutableInt128.shiftRightRounding(k + 3, 1) == k / 2 + 2;
        }
        testRightShiftRounding(MutableInt128.valueOf(-21320308303474571L), 3, true);
        testRightShiftRounding(MutableInt128.valueOfBits(
                0, 0, true), 1000, true);
        testRightShiftRounding(MutableInt128.valueOfBits(
                Long.MAX_VALUE, Long.MAX_VALUE, false), 50, true);
        testRightShiftRounding(MutableInt128.valueOfBits(
                0, 512, false), 70, true);
        testRightShiftRounding(MutableInt128.valueOfBits(
                Long.MAX_VALUE, Long.MAX_VALUE, true), 150, true);
        testRightShiftRounding(MutableInt128.valueOfBits(
                Long.MAX_VALUE, Long.MIN_VALUE, true), 150, true);
        testRightShiftRounding(MutableInt128.valueOf(
                0x8000000000000000L), 40, true);

        Random rnd = new Random(11);
        final long seed = rnd.nextLong();
        rnd.setSeed(seed);
        final int n1 = 2000;
        final int n2 = 10000;
        final int n = n1 * n2;
        testCorrectness(n, rnd);
        for (int test = 1; test <= 5; test++) {
            System.out.printf("%nTest #%d%n", test);
            rnd.setSeed(seed);
            for (int shift = 0; shift < 140; shift++) {
                if (shift % (shift < 10 ? 1 : shift < 50 ? 4 : 16) != 0) {
                    continue;
                }
                long t1 = System.nanoTime();
                int sum1 = 0, sum2 = 0, sum3 = 0, sum4 = 0;
                for (int k = -n; k < n; k++) {
                    sum1 += MutableInt128.shiftRightRounding(k, shift);
                }
                long t2 = System.nanoTime();
                for (int k = -n; k < n; k++) {
                    sum2 += MutableInt128.unsignedShiftRightRounding(k, shift);
                }
                long t3 = System.nanoTime();
                for (int k = -n; k < n; k++) {
                    sum3 += simpleRoundingRightShift(k, shift);
                }
                long t4 = System.nanoTime();
                for (int k = 0; k < n1; k++) {
                    MutableInt128 a = MutableInt128Test.randomInt128(rnd);
                    MutableInt128 b = new MutableInt128();
                    for (int i = 0; i < n2; i++) {
                        b.setTo(a).shiftRightRounding(shift);
                    }
                    sum4 += b.toDouble(); // some usage of b
                }
                long t5 = System.nanoTime();
                if (sum3 != sum1) {
                    throw new AssertionError("Some error in rightShiftRounding");
                }
                System.out.printf(Locale.US,
                        "Shift %d: "
                                + "shiftRightRounding(long, %d): %.5f ns, "
                                + "unsugnedShiftRightRounding(long, %d): %.5f ns, "
                                + "simple floating-point version: %.5f ns, "
                                + "int128.shiftRightRounding(%d): %.5f ns (sum %s)%n",
                        shift,
                        shift, (t2 - t1) / (double) (2 * n),
                        shift, (t3 - t2) / (double) (2 * n),
                        (t4 - t3) / (double) (2 * n),
                        shift, (t5 - t4) / (double) n,
                        sum2 + sum4);
            }
        }
        testCorrectness(100000000, rnd);
        System.out.println("O'k");
    }
}
