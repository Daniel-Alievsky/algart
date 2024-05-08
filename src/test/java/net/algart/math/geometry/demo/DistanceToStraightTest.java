/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.geometry.demo;

import net.algart.math.Point;
import net.algart.math.geometry.Orthonormal3DBasis;
import net.algart.math.geometry.StraightLine3D;

import java.util.Random;

public class DistanceToStraightTest {
    static final Random rnd = new Random();

    private static double rnd() {
        return 10000.0 * rnd.nextDouble() - 5000.0;
    }

    private static boolean different(double a, double b) {
        return Math.abs(a - b) > 1e-7;
    }

    public static double distanceToStraightSimple(double dx, double dy, double dz, double x, double y, double z) {
        final double t = x * dx + y * dy + z * dz;
        return Math.sqrt(x * x + y * y + z * z - t * t);
    }

    public static void main(String[] args) {
        for (int testIndex = 1; testIndex <= 100000000; testIndex++) {
            if (testIndex % 100 == 0) {
                System.out.print("\r" + testIndex + " ");
            }
            StraightLine3D s1 = StraightLine3D.getInstanceFromOrigin(rnd(), rnd(), rnd());
            StraightLine3D s2 = StraightLine3D.getInstance(rnd(), rnd(), rnd(), rnd(), rnd(), rnd());
            Point p = Point.valueOf(rnd(), rnd(), rnd());
            double x = p.x();
            double y = p.y();
            double z = p.z();
            double d1 = s1.distanceToStraight(x, y, z);
            double d2 = StraightLine3D.distanceToStraight(s1.dx(), s1.dy(), s1.dz(), x, y, z);
            double d3 = Math.sqrt(s1.distanceToStraightSquare(x, y, z));
            double d4 = Math.sqrt(StraightLine3D.distanceToStraightSquare(s1.dx(), s1.dy(), s1.dz(), x, y, z));
            double d5 = distanceToStraightSimple(s1.dx(), s1.dy(), s1.dz(), x, y, z);
            if (d1 != d2) throw new AssertionError(d1 + "!=" + d2);
            if (d1 != d3) throw new AssertionError(d1 + "!=" + d3);
            if (d1 != d4) throw new AssertionError(d1 + "!=" + d4);
            if (different(d1, d5)) throw new AssertionError(d1 + "!=" + d5);
            d1 = s2.distanceToStraight(x, y, z);
            d2 = StraightLine3D.distanceToStraight(
                    s2.dx(), s2.dy(), s2.dz(),
                    x - s2.x0(), y - s2.y0(), z - s2.z0());
            if (d1 != d2) throw new AssertionError(d1 + "!=" + d2);
            d5 = distanceToStraightSimple(
                    s2.dx(), s2.dy(), s2.dz(),
                    x - s2.x0(), y - s2.y0(), z - s2.z0());
            if (different(d1, d5)) throw new AssertionError(d1 + "!=" + d5);
            d1 = s2.distanceToStartPoint(x, y, z);
            d2 = Orthonormal3DBasis.length(x - s2.x0(), y - s2.y0(), z - s2.z0());
            if (d1 != d2) throw new AssertionError(d1 + "!=" + d2);
        }
        System.out.println("O'k");
    }
}
