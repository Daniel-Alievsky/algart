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

package net.algart.math.patterns.demo;

import net.algart.math.Point;
import net.algart.math.patterns.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p>Simple test checking rounding of extremely large patterns (geometrically).
 * Please modify and debug this code to view how large patters are processed.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public strictfp class ExtremelyLargePatternTest {
    static final long MAX = 1L << 61;

    private static void printNeighbourhood(long m, double step) {
        System.out.printf(Locale.US, "Neighbourhood of 0x%x with step %.2f:%n", m, step);
        for (long k = -8; k <= 8; k++) {
            double x = step == 1.0 ? m + k : m + k * step;
            double r = StrictMath.rint(x);
            System.out.printf(Locale.US, "    x=%.2f, StrictMath.round(x)=%s=0x%x, StrictMath.rint(x)=%.2f=%s=0x%x%n",
                new BigDecimal(x), StrictMath.round(x), StrictMath.round(x),
                new BigDecimal(r), (long) r, (long) r);
        }
    }

    public static void main(String[] args) {
        long m = -1;
        int log = 0;
        while ((double) m != (double) (m + 1)) {
            m <<= 1;
            log++;
        }
        log--;
        m = (-1L) << log;
        System.out.printf("M = 0x%x, log = %d%n", m, log);
        printNeighbourhood(m, 1.0);
        m = 1;
        log = 0;
        while ((double) m != (double) (m - 1)) {
            m <<= 1;
            log++;
        }
        log--;
        m = 1L << log;
        System.out.println(((double) m != (double) (m - 1))
            + ", " + StrictMath.round((double) m) + "=" + (double) m + "=" + m
            + ", " + StrictMath.round((double) (m - 1)) + "=" + (double) (m - 1) + "=" + (m - 1));
        System.out.printf("M = 0x%x, log = %d%n", m, log);
        printNeighbourhood(m, 1.0);
        log--;
        m = 1L << log;
        System.out.println(((double) m != (double) (m - 1))
            + ", " + StrictMath.round((double) m) + "=" + (double) m + "=" + m
            + ", " + StrictMath.round((double) (m - 1)) + "=" + (double) (m - 1) + "=" + (m - 1));
        System.out.printf("M = 0x%x, log = %d%n", m, log);
        printNeighbourhood(m, 0.5);
        printNeighbourhood(m, 1.0);
        printNeighbourhood(Pattern.MAX_COORDINATE, 1.0);

        System.out.printf(Locale.US, "%nMAX=0x%x(=%s)%nPattern.MAX_COORDINATE=0x%x(=%s)%n",
            MAX, MAX,
            Pattern.MAX_COORDINATE, Pattern.MAX_COORDINATE);

        double a = 1L << 60;
        double b = -1L << 60;
        double c = -(1L << 60) + 128;
        System.out.printf(Locale.US, "%na=0x%x(=%s=%.2f=2^60), b=0x%x(=%s=%.2f), c=0x%x(=%s=%.2f):%n"
            + "  (b==c)=%s, c-b=%s, [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=MAX)=%s%n",
            StrictMath.round(a), StrictMath.round(a), new BigDecimal(a),
            StrictMath.round(b), StrictMath.round(b), new BigDecimal(b),
            StrictMath.round(c), StrictMath.round(c), new BigDecimal(c),
            b == c, c - b,
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= MAX);
        a -= c;
        b -= c;
        // it violates requirement [a]-[b]<=MAX, because a and b are too large
        System.out.printf(Locale.US, "a:=a-c;%nb:=b-c;%n");
        System.out.printf(Locale.US,
            "a=0x%x(=%s=%s), b=0x%x(=%s=%s)%n  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=MAX)=%s%n",
            StrictMath.round(a), StrictMath.round(a), new BigDecimal(a),
            StrictMath.round(b), StrictMath.round(b), new BigDecimal(b),
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= MAX ? "true (Ok)" : "false (PROBLEM!)");

        a = Pattern.MAX_COORDINATE / 2;
        b = -Pattern.MAX_COORDINATE / 2;
        c = -Pattern.MAX_COORDINATE / 2 + 0.25;
        System.out.printf(Locale.US, "%na=0x%x(=%s=%.2f=Pattern.MAX_COORDINATE/2), "
            + "b=0x%x(=%s=%.2f), c=%.2f:%n"
            + "  (b==c)=%s, c-b=%s, [a]-[b]=0x%x, a-b=%s~%s%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n"
            + "  (a-b<=Pattern.MAX_COORDINATE)=%s (strictly)%n",
            StrictMath.round(a), StrictMath.round(a), new BigDecimal(a),
            StrictMath.round(b), StrictMath.round(b), new BigDecimal(b),
            new BigDecimal(c),
            b == c, c - b,
            StrictMath.round(a) - StrictMath.round(b),
            new BigDecimal(a).subtract(new BigDecimal(b)), new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE,
            Patterns.isAllowedDifference(a, b));
        a -= c;
        b -= c;
        // it violates requirement [a]-[b]<=Pattern.MAX_COORDINATE, because a and b are too large
        System.out.printf(Locale.US, "a:=a-c;%nb:=b-c;%n");
        System.out.printf(Locale.US,
            "a=0x%x(=%s=%s), b=%s%n  [a]-[b]=0x%x, a-b=%s~%s%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n"
                + "  (a-b<=Pattern.MAX_COORDINATE)=%s (strictly)%n",
            StrictMath.round(a), StrictMath.round(a), new BigDecimal(a),
            new BigDecimal(b),
            StrictMath.round(a) - StrictMath.round(b),
            new BigDecimal(a).subtract(new BigDecimal(b)), new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE ? "true (Ok)" : "false (PROBLEM!)",
            Patterns.isAllowedDifference(a, b) ? "true (Ok)" : "false (PROBLEM!)");

        a = Pattern.MAX_COORDINATE - 1.0;
        b = -1.5;
        c = -0.5;
        System.out.printf(Locale.US, "%na=%.2f(=Pattern.MAX_COORDINATE-1.0), [a]=%s, b=%.2f, [b]=%s, c=%.2f:%n"
            + "  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            new BigDecimal(a), StrictMath.round(a),
            b, StrictMath.round(b),
            c,
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE);
        a -= c;
        b -= c;
        // it violates requirement [a]-[b]<=Pattern.MAX_COORDINATE, c is not integer
        System.out.printf(Locale.US, "a:=a-c;%nb:=b-c;%n");
        System.out.printf(Locale.US, "a=%.2f, [a]=%s, b=%.2f, [b]=%s,%n"
            + "  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            new BigDecimal(a), StrictMath.round(a),
            b, StrictMath.round(b),
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE ? "true (Ok)" : "false (PROBLEM!)");

        a = Pattern.MAX_COORDINATE - 1.0;
        b = -1.5;
        c = -1.0;
        System.out.printf(Locale.US, "%na=%.2f(=Pattern.MAX_COORDINATE-1.0), [a]=%s, b=%.2f, [b]=%s, c=%.2f:%n"
            + "  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            new BigDecimal(a), StrictMath.round(a),
            b, StrictMath.round(b),
            c,
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE);
        a -= c;
        b -= c;
        // for comparison, it does not violate requirement [a]-[b]<=Pattern.MAX_COORDINATE
        System.out.printf(Locale.US, "a:=a-c;%nb:=b-c;%n");
        System.out.printf(Locale.US, "a=%.2f, [a]=%s, b=%.2f, [b]=%s,%n"
            + "  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            new BigDecimal(a), StrictMath.round(a),
            b, StrictMath.round(b),
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE ? "true (Ok)" : "false (PROBLEM!)");

        a = 0.4999;
        b = -Pattern.MAX_COORDINATE;
        c = -Pattern.MAX_COORDINATE / 2;
        System.out.printf(Locale.US, "%na=%f, [a]=%s, b=%.2f(=%a=-Pattern.MAX_COORDINATE), [b]=%s(=0x%x), "
            + "c=%.2f(=%a=-Pattern.MAX_COORDINATE/2):%n"
            + "  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            new BigDecimal(a), StrictMath.round(a),
            b, b, StrictMath.round(b), StrictMath.round(b),
            c, c,
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE);
        a -= c;
        b -= c;
        // it violates requirement [a]-[b]<=Pattern.MAX_COORDINATE, though c is integer
        System.out.printf(Locale.US, "a:=a-c;%nb:=b-c;%n");
        System.out.printf(Locale.US, "a=%.2f(=%a), [a]=%s(=0x%x), b=%.2f(=%a), [b]=%s(=0x%x),%n"
            + "  [a]-[b]=0x%x(~0x%x=%s=%s)%n  ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            new BigDecimal(a), a, StrictMath.round(a), StrictMath.round(a),
            b, b, StrictMath.round(b), StrictMath.round(b),
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a - b), StrictMath.round(a - b),
            new BigDecimal(a - b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE ? "true (Ok)" : "false (PROBLEM!)");

        a = Pattern.MAX_COORDINATE;
        b = -0.5;
        System.out.printf(Locale.US, "%na=%.2f(=Pattern.MAX_COORDINATE), [a]=%s, b=%f, [b]=%s:%n"
            + "  a-b=%.2f, (a-b<=Pattern.MAX_COORDINATE)=%s, strictly =%s%n"
            + "  [a]-[b]=%s(=0x%x), ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            a, StrictMath.round(a),
            b, StrictMath.round(b),
            new BigDecimal(a - b),
            a - b <= Pattern.MAX_COORDINATE,
            Patterns.isAllowedDifference(a, b),
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a) - StrictMath.round(b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE ? "true (Ok)" : "false (PROBLEM!)");

        a = Pattern.MAX_COORDINATE - 0.5;
        b = -0.5001;
        System.out.printf(Locale.US, "%na=%.2f(=Pattern.MAX_COORDINATE-0.5), [a]=%s, b=%f, [b]=%s:%n"
            + "  a-b=%.2f, (a-b<=Pattern.MAX_COORDINATE)=%s%n"
            + "  mathematically strictly: a-b=%.7f, (a-b<=Pattern.MAX_COORDINATE)=%s=%s%n"
            + "  [a]-[b]=%s(=0x%x), ([a]-[b]<=Pattern.MAX_COORDINATE)=%s%n",
            a, StrictMath.round(a),
            b, StrictMath.round(b),
            new BigDecimal(a - b),
            a - b <= Pattern.MAX_COORDINATE,
            new BigDecimal(a).subtract(new BigDecimal(b)),
            new BigDecimal(a).subtract(new BigDecimal(b)).compareTo(new BigDecimal(Pattern.MAX_COORDINATE)) <= 0,
            Patterns.isAllowedDifference(b, a),
            StrictMath.round(a) - StrictMath.round(b), StrictMath.round(a) - StrictMath.round(b),
            StrictMath.round(a) - StrictMath.round(b) <= Pattern.MAX_COORDINATE ? "true (Ok)" : "false (PROBLEM!)");

        a = Pattern.MAX_COORDINATE - 0.5;
        System.out.printf(Locale.US, "%na=%.2f(=%s=%a), [a]=StrictMath.round(a)=%s, Math.round(a)=%s%n",
            a, new BigDecimal(a), a, StrictMath.round(a), Math.round(a));
        a = -Pattern.MAX_COORDINATE + 0.5;
        System.out.printf(Locale.US, "a=%.2f(=%s=%a), [a]=StrictMath.round(a)=%s, Math.round(a)=%s%n",
            a, new BigDecimal(a), a, StrictMath.round(a), Math.round(a));
        a = StrictMath.nextAfter(0.5, 0.0);
        System.out.printf(Locale.US, "a=prev(0.5d)=%.20f(=%s=%a), [a]=StrictMath.round(a)=%s, Math.round(a)=%s%n",
            a, new BigDecimal(a), a, StrictMath.round(a), Math.round(a));
        a = StrictMath.nextAfter(a, 0.0);
        System.out.printf(Locale.US, "a=prev(prev(0.5d))=%.20f(=%s=%a), [a]=StrictMath.round(a)=%s, Math.round(a)=%s%n",
            a, new BigDecimal(a), a, StrictMath.round(a), Math.round(a));
        float af = StrictMath.nextAfter(0.5f, 0.0);
        System.out.printf(Locale.US, "a=prev(0.5f)=%.20ff(=%s=%a), [a]=StrictMath.round(a)=%s, Math.round(a)=%s%n",
            af, new BigDecimal(af), af, StrictMath.round(af), Math.round(af));
        af = StrictMath.nextAfter(af, 0.0);
        System.out.printf(Locale.US, "a=prev(prev(0.5f))=%.20ff(=%s=%a), [a]=StrictMath.round(a)=%s, Math.round(a)=%s%n",
            af, new BigDecimal(af), af, StrictMath.round(af), Math.round(af));

        System.out.println();
        Pattern pattern;
        try {
            pattern = new SimplePattern(java.util.Arrays.asList(
                Point.valueOf(Pattern.MAX_COORDINATE - 0.5, 0),
                Point.valueOf(-0.5001, 0)));
            try {
                System.out.println(pattern.round()); // should not work due to exception in the previous constructor
            } catch (Exception e) {
                System.out.println("Unexpected error!");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Cannot create a pattern: " + e);
        }
        pattern = new SimplePattern(java.util.Arrays.asList(
            Point.valueOf(-Pattern.MAX_COORDINATE + 0.5, 0),
            Point.valueOf(StrictMath.nextAfter(0.5, 0.0), 0)));
        System.out.printf("Large pattern: %s, x-range 0x%x..0x%x%n",
            pattern,
            StrictMath.round(pattern.coordRange(0).min()),
            StrictMath.round(pattern.coordRange(0).max()));
        UniformGridPattern rounded = pattern.round();
        System.out.printf("Rounded pattern: %s, x-range 0x%x..0x%x%n%n",
            rounded,
            rounded.gridIndexRange(0).min(),
            rounded.gridIndexRange(0).max());

        SimplePattern patternA = new SimplePattern(Arrays.asList(
            Point.valueOf(0.0, 0.0),
            Point.valueOf(0.0001, 0.0)));
        DirectPointSetPattern patternB = Patterns.newPattern(Point.valueOf(Pattern.MAX_COORDINATE / 2, 0.0));
        pattern = Patterns.newMinkowskiSum(patternA, patternB);
        System.out.printf("Minkowski sum of:%n  %s%n  and %s%n  is %s%n"
            + "  it is %d points {%s}, isSurelySinglePoint()=%s%n",
            patternA, patternB, pattern, pattern.pointCount(), pattern.points(), pattern.isSurelySinglePoint());
    }
}
