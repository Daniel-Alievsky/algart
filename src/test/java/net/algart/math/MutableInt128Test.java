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

package net.algart.math;

import java.math.BigInteger;
import java.util.Random;

public class MutableInt128Test {
    private static void testEquality(MutableInt128 a, MutableInt128 b, boolean print) {
        if (print) {
            System.out.printf("%s %s %s, hash-codes: 0x%08X, 0x%08X%n",
                    a, a.equals(b) ? "==" : "!=", b, a.hashCode(), b.hashCode());
        }
    }

    private static void testBitAndShift(MutableInt128 a, int shift, boolean print) {
        if (shift < 128 && !a.clone().setBit(shift, true).getBit(shift)) {
            throw new AssertionError("Error in setBit(" + shift + ", true)");
        }
        if (shift < 128 && a.clone().setBit(shift, false).getBit(shift)) {
            throw new AssertionError("Error in setBit(" + shift + ", false)");
        }
        MutableInt128 shifted = a.clone().shiftRight(shift);
        if (print) {
            System.out.printf("%s >>> %d = %s, ", a, shift, shifted);
        }
        int count = 0;
        for (int k = 0; k < 150; k++) {
            if (a.getBit(k)) {
                count++;
            }
            if (shifted.getBit(k) != a.getBit(k + shift)) {
                throw new AssertionError("Invalid right shift of " + a + " by " + shift);
            }
        }
        if (count != a.bitCount()) {
            throw new AssertionError("Illegal bitCount = " + a.bitCount());
        }
        if (shift == 64 && shifted.low64Bits() != a.high64Bits()) {
            throw new AssertionError("Invalid right shift of " + a + " by "
                    + shift + " or low/high64Bits()");
        }
        if (shifted.numberOfLeadingZeros() < Math.min(128, shift)) {
            throw new AssertionError("Invalid numberOfLeadingZeros() = "
                    + shifted.numberOfLeadingZeros() + " < " + Math.min(128, shift));
        }
        shifted = a.clone();
        ArithmeticException overflow = null;
        try {
            shifted.shiftLeft(shift);
        } catch (ArithmeticException e) {
            overflow = e;
            if (!shifted.equals(a)) {
                throw new AssertionError("left shift damaged the object in a case of exception");
            }
        }
        if (print) {
            System.out.printf("%s <<< %d = %s%n", a, shift, overflow != null ? "overflow" : shifted);
        }
        if (overflow == null && shifted.numberOfTrailingZeros() < Math.min(128, shift)) {
            throw new AssertionError("Invalid numberOfTrailingZeros() = "
                    + shifted.numberOfTrailingZeros() + " < " + Math.min(128, shift));
        }
        boolean correctOverflow = false;
        for (int k = 0; k < 150; k++) {
            correctOverflow |= k + shift >= 128 && a.getBit(k);
        }
        if ((overflow != null) != correctOverflow) {
            throw new AssertionError("Invalid overflow "
                    + (correctOverflow ? "skipping" : "detection") + " in left shift <<"
                    + shift + " of " + a.toString(16) + " (" + a.toString(2) + "): " + overflow);
        }
        if (overflow == null) {
            for (int k = 0; k < 150; k++) {
                if (shifted.getBit(k) != (k >= shift && a.getBit(k - shift))) {
                    throw new AssertionError("Invalid left shift of " + a + " by " + shift);
                }
            }
            if (!shifted.shiftRight(shift).equals(a)) {
                throw new AssertionError("Left/right shift mismatch");
            }
        }
    }

    private static void testSumAndDifferenceAndToLong(MutableInt128 a, MutableInt128 b, boolean print) {
        if (a.signum() != a.toBigInteger().signum()) {
            throw new AssertionError("Error in signum: " + a.signum() + " for " + a);
        }
        MutableInt128 c = null;
        try {
            c = a.clone().add(b);
        } catch (ArithmeticException e) {
            if (print) {
                throw e;
            }
        }
        if (print) {
            System.out.printf("%s + %s = %s (0x%s, %s), zero: %s, positive: %s, negative: %s%n", a, b, c,
                    c.toString(16), c.toDouble(),
                    c.isZero(), c.isPositive(), c.isNegative());
        }
        if (c == null) {
            return;
        }
        MutableInt128ToDoubleTest.testToDouble(a, false);
        MutableInt128ToDoubleTest.testToDouble(b, false);
        MutableInt128ToDoubleTest.testToDouble(c, false);
        if (!c.toBigInteger().equals(a.toBigInteger().add(b.toBigInteger()))) {
            throw new AssertionError("Error in add");
        }
        try {
            c = a.clone().add(b.clone().negate());
        } catch (ArithmeticException e) {
            c = null;
        }
        int compare = a.compareTo(b);
        if (print) {
            System.out.printf("%s - %s = %s, comparison: %s, zero: %s, positive: %s, negative: %s%n", a, b, c,
                    compare,
                    c == null ? "n/a" : c.isZero(),
                    c == null ? "n/a" : c.isPositive(),
                    c == null ? "n/a" : c.isNegative());
        }
        if (c != null && !c.toBigInteger().equals(a.toBigInteger().subtract(b.toBigInteger()))) {
            throw new AssertionError("Error in add (negative)");
        }
        if (a.toBigInteger().compareTo(b.toBigInteger()) != compare) {
            throw new AssertionError("Error in compare");
        }
        MutableInt128 d = null;
        try {
            d = a.clone().subtract(b);
        } catch (ArithmeticException ignored) {
        }
        if (!((d == c) || (d != null && d.equals(c)))) {
            throw new AssertionError("Error in subtract");
        }
        if (a.isConvertibleToLong()) {
            if (!MutableInt128.valueOf(a.toLongExact()).equals(a)) {
                throw new AssertionError("Error in toLongExact");
            }
        }
        BigInteger aAbs = a.clone().abs().toBigInteger();
        BigInteger bAbs = b.clone().abs().toBigInteger();
        if (!a.clone().and(b).abs().toBigInteger().equals(aAbs.and(bAbs))) {
            throw new AssertionError("Error in andAbsoluteValue");
        }
        if (!a.clone().or(b).abs().toBigInteger().equals(aAbs.or(bAbs))) {
            throw new AssertionError("Error in orAbsoluteValue");
        }
        if (!a.clone().xor(b).abs().toBigInteger().equals(aAbs.xor(bAbs))) {
            throw new AssertionError("Error in xorAbsoluteValue");
        }
        final MutableInt128 xorWithUnits = MutableInt128.valueOfBits(-1, -1, false)
                .xor(a);
        final MutableInt128 not = a.clone().not().abs();
        if (!not.equals(xorWithUnits)) {
            throw new AssertionError("Error in notAbsoluteValue: not="
                    + not.toString(16) + ", xor=" + xorWithUnits.toString(16));
        }
    }

    private static void testLongIntProduct(long a, int b, MutableInt128 c, boolean print) {
        MutableInt128 p = new MutableInt128().setToLongIntProduct(a, b);
        MutableInt128 d = null;
        MutableInt128 s = null;
        try {
            d = c.clone().add(p);
            s = c.clone().addLongIntProduct(a, b); // must be =d
        } catch (ArithmeticException e) {
            if (print) {
                throw e;
            }
            return;
        }
        if (print) {
            System.out.printf("%s * %s = %s (%s), zero: %s, positive: %s, negative: %s, + %s = %s%n",
                    a, b, p, p.toDouble(),
                    p.isZero(), p.isPositive(), p.isNegative(),
                    c, s);
        }
        if (!p.toBigInteger().equals(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)))) {
            throw new AssertionError("Error in long-int product " + a + "*" + b);
        }
        MutableInt128 q = new MutableInt128().setToLongLongProduct(a, b);
        if (!q.equals(p)) {
            throw new AssertionError("Error in long-long product: " + a + "*" + b + " = " + q);
        }
        q.setToLongLongProduct(b, a);
        if (!q.equals(p)) {
            throw new AssertionError("Error in long-long product: " + b + "*" + a + " = " + q);
        }
        if (!d.equals(s)) {
            throw new AssertionError("Error in adding long-int product: " + b + "*" + a + " = " + s);
        }
        try {
            d = c.clone().add(MutableInt128.valueOf(a));
            s = c.clone().addLong(a);
        } catch (ArithmeticException e) {
            if (print) {
                throw e;
            }
            return;
        }
        if (!d.equals(s)) {
            throw new AssertionError("Error in adding long: " + c + "+" + a + " = " + s);
        }
    }

    private static void testLongLongProduct(long a, long b, final MutableInt128 c, boolean print) {
        MutableInt128 p = new MutableInt128().setToLongLongProduct(a, b);
        MutableInt128 d = null;
        MutableInt128 s = null;
        try {
            d = c.clone().add(p);
            s = c.clone().addLongLongProduct(a, b); // must be =d
        } catch (ArithmeticException e) {
            if (print) {
                throw e;
            }
            return;
        }
        if (print) {
            System.out.printf("%s * %s = %s (%s), zero: %s, positive: %s, negative: %s, + %s = %s%n",
                    a, b, p, p.toDouble(),
                    p.isZero(), p.isPositive(), p.isNegative(),
                    c, s);
        }
        if (!p.toBigInteger().equals(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)))) {
            throw new AssertionError("Error in product " + a + "*" + b);
        }
        if (!d.equals(s)) {
            throw new AssertionError("Error in adding long-long product: " + a + "*" + b + " = " + s);
        }
        MutableInt128 q = new MutableInt128().setToLongLongProduct(b, a);
        if (!q.equals(p)) {
            throw new AssertionError("Error in long-long product: "
                    + a + "*" + b + " = " + q + " != " + b + "*" + a);
        }
        q = new MutableInt128().setToUnsignedLongLongProduct(a, b);
        if (!q.toBigInteger().equals(MutableInt128.valueOfUnsigned(a).toBigInteger().multiply(
                MutableInt128.valueOfUnsigned(b).toBigInteger()))) {
            throw new AssertionError("Error in unsigned product " + a + "*" + b);
        }
        if (a >= 0 && b >= 0 && !q.equals(p)) {
            throw new AssertionError("Error in (unsigned?) long-long product: "
                    + a + "*" + b + " = " + q + " != " + p);
        }
        d = null;
        s = null;
        try {
            d = c.clone().add(q);
            s = c.clone().addUnsignedLongLongProduct(a, b); // must be =d
        } catch (ArithmeticException e) {
            return;
        }
        if (!d.equals(s)) {
            throw new AssertionError("Error in adding unsigned long-long product");
        }
        q = new MutableInt128().setToLongLongProduct(a, a);
        MutableInt128 r = new MutableInt128().setToLongSqr(a);
        if (!q.equals(r)) {
            throw new AssertionError("Error in long sqr: "
                    + a + "*" + a + " = " + r
                    + " (0x" + r.toString(16) + " instead of 0x" + q.toString(16) + ")");
        }
        d = null;
        s = null;
        try {
            d = c.clone().add(r);
            s = c.clone().addLongSqr(a); // must be =d
        } catch (ArithmeticException e) {
            return;
        }
        if (!d.equals(s)) {
            throw new AssertionError("Error in adding long sqr");
        }
    }

    private static long randomLong(Random rnd) {
        switch (rnd.nextInt(8)) {
            case 0:
                return 0;
            case 1:
                return Long.MAX_VALUE - rnd.nextInt(3);
            case 2:
                return -Long.MAX_VALUE + rnd.nextInt(3);
            case 3:
                return Long.MIN_VALUE;
            case 4:
                return 0x4000000000000000L;
            case 5:
                return -0x4000000000000000L;
            case 6:
                return rnd.nextLong();
            case 7:
                return rnd.nextInt(10000) - 5000;
            default:
                throw new AssertionError("Impossible");
        }
    }

    private static int randomInt(Random rnd) {
        switch (rnd.nextInt(8)) {
            case 0:
                return 0;
            case 1:
                return Integer.MAX_VALUE - rnd.nextInt(3);
            case 2:
                return -Integer.MAX_VALUE + rnd.nextInt(3);
            case 3:
                return Integer.MIN_VALUE;
            case 4:
                return 0x40000000;
            case 5:
                return -0x40000000;
            case 6:
                return rnd.nextInt();
            case 7:
                return rnd.nextInt(10000) - 5000;
            default:
                throw new AssertionError("Impossible");
        }
    }

    static MutableInt128 randomInt128(Random rnd) {
        final int mode = rnd.nextInt(3);
        if (mode == 0) {
            switch (rnd.nextInt(3)) {
                case 0:
                    return new MutableInt128().setToDouble(10 * (rnd.nextDouble() - 0.5));
                case 1:
                    return new MutableInt128().setToDouble(1e20 * (rnd.nextDouble() - 0.5));
                case 2:
                    return new MutableInt128().setToDouble(1e40 * (rnd.nextDouble() - 0.5));
                default:
                    throw new AssertionError("Impossible");
            }
        }
        if (mode == 1) {
            switch (rnd.nextInt(4)) {
                case 0:
                    return MutableInt128.valueOf(0);
                case 1:
                    return MutableInt128.valueOf(Long.MIN_VALUE);
                case 2:
                    return MutableInt128.valueOf(Long.MAX_VALUE);
                case 3:
                    return MutableInt128.valueOf(rnd.nextLong());
                default:
                    throw new AssertionError("Impossible");
            }
        }
        final MutableInt128 result = randomInt128WithoutSpecialValues(rnd);
        if (rnd.nextBoolean()) {
            int shift = rnd.nextInt(120);
            result.shiftRight(shift);
            result.shiftLeft(shift);
            // clearing random number of low bits
        }
        return result;
    }

    static MutableInt128 randomInt128WithoutSpecialValues(Random rnd) {
        final long high = randomLong(rnd);
        final long low = randomLong(rnd);
        return MutableInt128.valueOfBits(high, low, rnd.nextBoolean());
    }

    public static void main(String[] args) throws InterruptedException {
//        long a = 0xFFFFFFFFL;
//        long b = 1L << 32;
//        System.out.println(Long.toString(a * b));

        testEquality(MutableInt128.valueOfBits(1234, 33, true),
                MutableInt128.valueOfBits(1234, 33, false), true);
        testEquality(MutableInt128.newOne().negate(), MutableInt128.newOne(), true);
        testEquality(MutableInt128.valueOfBits(157, 0, true),
                MutableInt128.valueOfBits(157, 0, true), true);
        testEquality(MutableInt128.valueOfBits(0, 0, true),
                MutableInt128.valueOfBits(0, 0, false), true);
        testBitAndShift(MutableInt128.valueOfBits(0, 0, true), 1000, true);
        testBitAndShift(MutableInt128.valueOfBits(Long.MAX_VALUE, Long.MAX_VALUE, false), 50, true);
        testBitAndShift(MutableInt128.valueOfBits(0, 512, false), 70, true);
        testBitAndShift(MutableInt128.valueOfBits(Long.MAX_VALUE, Long.MAX_VALUE, true), 150, true);
        testBitAndShift(MutableInt128.valueOfBits(Long.MAX_VALUE, Long.MIN_VALUE, true), 150, true);
        testBitAndShift(MutableInt128.valueOf(0x8000000000000000L), 40, true);
        testSumAndDifferenceAndToLong(MutableInt128.valueOf(Long.MAX_VALUE - 1),
                MutableInt128.valueOf(Long.MAX_VALUE), true);
        testSumAndDifferenceAndToLong(new MutableInt128().setToUnsignedLong(-1),
                MutableInt128.valueOf(1), true);
        testSumAndDifferenceAndToLong(MutableInt128.valueOf(10),
                MutableInt128.valueOf(20), true);
        testSumAndDifferenceAndToLong(MutableInt128.valueOf((long) 8e18),
                MutableInt128.valueOf((long) 8e18), true);
        testSumAndDifferenceAndToLong(MutableInt128.valueOf(Long.MAX_VALUE),
                MutableInt128.valueOf(Long.MIN_VALUE), true);
        testSumAndDifferenceAndToLong(MutableInt128.valueOf((long) 8e18),
                MutableInt128.valueOf((long) 8e18), true);
        testSumAndDifferenceAndToLong(new MutableInt128().setToDouble(2e24),
                new MutableInt128().setToDouble(1e25), true);
        testSumAndDifferenceAndToLong(MutableInt128.valueOfBits(Long.MAX_VALUE, Long.MIN_VALUE, true),
                MutableInt128.valueOfBits(Long.MAX_VALUE, Long.MAX_VALUE, false), true);
        testSumAndDifferenceAndToLong(MutableInt128.newMaxValue(),
                MutableInt128.valueOfBits(0, 0, false), true);
        testSumAndDifferenceAndToLong(MutableInt128.newMaxValue(), MutableInt128.newMinValue(), true);
        testLongIntProduct(1000000, 1000000, MutableInt128.valueOf(-1), true);
        testLongIntProduct(1000000, -1000000, MutableInt128.valueOf(1000000), true);
        testLongIntProduct(1000000, 0, new MutableInt128(), true);
        testLongIntProduct(new Random().nextLong(), 0, new MutableInt128(), true);
        testLongIntProduct(Long.MAX_VALUE, 256, MutableInt128.valueOf(Long.MAX_VALUE), true);
        testLongIntProduct(Long.MAX_VALUE, 1000, randomInt128(new Random()).shiftRight(1), true);
        testLongIntProduct(Long.MIN_VALUE, -2, MutableInt128.valueOf(Long.MAX_VALUE).addLong(1), true);
        testLongIntProduct((long) 10e18, Integer.MAX_VALUE, new MutableInt128(), true);
        testLongIntProduct((long) 1e18, Integer.MIN_VALUE, new MutableInt128(), true);
        testLongIntProduct(Long.MAX_VALUE, Integer.MAX_VALUE, new MutableInt128(), true);
        testLongIntProduct(Long.MIN_VALUE, Integer.MIN_VALUE, new MutableInt128(), true);
        testLongIntProduct(0x7fffffffffff0000L, 0x7fff0000, new MutableInt128(), true);
        testLongLongProduct(Long.MAX_VALUE, Long.MAX_VALUE, new MutableInt128(), true);
        testLongLongProduct(Long.MIN_VALUE, Long.MAX_VALUE,
                MutableInt128.valueOf(Long.MAX_VALUE - 1), true);
        testLongLongProduct(1000000000000000L, 2000000000000000L,
                MutableInt128.valueOf(2000000000000000L), true);
        System.out.println();
        Thread.sleep(100);

        Random rnd = new Random();
        long seed = rnd.nextLong();
        rnd.setSeed(seed);
        System.out.println("Testing with rand-seed " + seed);

        final int n1 = 1000;
        final int n2 = 10000;
        double sum = 0.0; // some usage necessary to avoid (not 100%!) removing the code by JIT optimizer
        for (int test = 1; test <= 8; test++) {
            long t1 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd);
                BigInteger bigA = BigInteger.valueOf(a);
                BigInteger bigB = BigInteger.valueOf(b);
                for (int i = 0; i < n2; i++) {
                    BigInteger bigProduct = bigA.multiply(bigB);
                }
            }
            long t2 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomInt(rnd);
                BigInteger bigA = BigInteger.valueOf(a);
                BigInteger bigB = BigInteger.valueOf(b);
                for (int i = 0; i < n2; i++) {
                    BigInteger bigProduct = bigA.multiply(bigB);
                }
            }
            long t3 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd);
                if (b == Long.MIN_VALUE) {
                    b = Long.MAX_VALUE;
                }
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.setToLongLongProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t4 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd);
                if (b == Long.MIN_VALUE) {
                    b = Long.MAX_VALUE;
                }
                b /= n2;
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.addLongLongProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t5 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.setToLongSqr(a);
                }
                sum += longProduct.toDouble();
            }
            long t6 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                a /= n2;
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.addLongSqr(a);
                }
                sum += longProduct.toDouble();
            }
            long t7 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                int b = randomInt(rnd);
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.setToLongIntProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t8 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                int b = randomInt(rnd);
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.addLongIntProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t9 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                int b = randomInt(rnd);
                MutableInt128 longProduct = new MutableInt128();
                for (int i = 0; i < n2; i++) {
                    longProduct.setToUnsignedLongLongProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t10 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd) >>> 1;
                b /= (n2 / 2 + 1);
                MutableInt128 longProduct = MutableInt128.newZero();
                for (int i = 0; i < n2; i++) {
                    longProduct.addUnsignedLongLongProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t11 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd);
                if (b == Long.MIN_VALUE) {
                    b = Long.MAX_VALUE;
                }
                MutableInt128 longProduct = MutableInt128.valueOf(0);
                for (int i = 0; i < n2; i++) {
                    longProduct = new MutableInt128().setToLongLongProduct(a, b);
                }
                sum += longProduct.toDouble();
            }
            long t12 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd);
                long product = 0;
                for (int i = 0; i < n2; i++) {
                    product += a * b;
                }
                sum += (double) product;
            }
            long t13 = System.nanoTime();
            for (int k = 0; k < n1; k++) {
                long a = randomLong(rnd);
                long b = randomLong(rnd);
                long product = 0;
                for (int i = 0; i < n2; i++) {
                    product += Math.multiplyHigh(a, b);
                }
                sum += (double) product;
            }
            long t14 = System.nanoTime();
            System.out.printf("Timing test #%d (total sum %f)%n", test, sum);
            System.out.printf("    BigInteger = 63bit*63bit: %.5f ns%n", (t2 - t1) / (double) (n1 * n2));
            System.out.printf("    BigInteger = 63bit*31bit: %.5f ns%n", (t3 - t2) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 = 63bit*63bit: %.5f ns%n", (t4 - t3) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 += 63bit*63bit: %.5f ns%n", (t5 - t4) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 = 63bit^2: %.5f ns%n", (t6 - t5) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 += 63bit^2: %.5f ns%n", (t7 - t6) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 = 63bit*31bit: %.5f ns%n", (t8 - t7) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 += 63bit*31bit: %.5f ns%n", (t9 - t8) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 = 64bit*64bit (unsigned): %.5f ns%n",
                    (t10 - t9) / (double) (n1 * n2));
            System.out.printf("    MutableInt128+ = 64bit*64bit (unsigned): %.5f ns%n",
                    (t11 - t10) / (double) (n1 * n2));
            System.out.printf("    MutableInt128 = 63bit*63bit in new MutableInt128(): %.5f ns%n",
                    (t12 - t11) / (double) (n1 * n2));
            System.out.printf("    Usual long += 63bit*63bit: %.5f ns%n", (t13 - t12) / (double) (n1 * n2));
            System.out.printf("    long += Math.multiplyHigh(64bit, 64bit): %.5f ns%n", (t14 - t13) /
                    (double) (n1 * n2));
        }

        for (int k = 1; k <= n1 * n2; k++) {
            if (k % n2 == 0) {
                System.out.print("\rTest #" + k);
            }
            testSumAndDifferenceAndToLong(randomInt128(rnd), randomInt128(rnd), false);
            testLongIntProduct(randomLong(rnd), randomInt(rnd), randomInt128(rnd), false);
            testLongLongProduct(randomLong(rnd), randomLong(rnd), randomInt128(rnd), false);
            testBitAndShift(randomInt128(rnd), rnd.nextInt(200), false);
        }
        System.out.printf("\r%d tests done              %n", n1 * n2);
    }
}
