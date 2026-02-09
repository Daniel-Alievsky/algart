/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.math.IPoint;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>Simple test illustrating operations with very large patterns (geometrically).
 * Please modify and debug this code to view how large patters are processed.</p>
 *
 * @author Daniel Alievsky
 */
public class LargePatternTest {
    public static void main(String[] args) {
        Pattern p1 = Patterns.newRectangularIntegerPattern(IPoint.of(0, 0), IPoint.of(20, 20));
        Set<IPoint> points = new HashSet<IPoint>();
        for (int k = 0; k < 1000; k++) {
            points.add(IPoint.of(k, k));
            points.add(IPoint.of(2000000000 + k, k));
            points.add(IPoint.of(k, 2000000000 + k));
        }
        Pattern p2 = Patterns.newIntegerPattern(points);
        Pattern pSum = Patterns.newMinkowskiSum(p1, p2);
        System.out.println("Large sum: " + pSum + ", contains " + pSum.pointCount() + " points");
    }
}
