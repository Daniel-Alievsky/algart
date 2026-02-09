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

package net.algart.math.points;

import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.Point;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The simplest test for {@link IPoint} class: shows a sorted array of several points.
 * You may call it with the argument like {@code additional/point_demo_reports/IPointDemo_report.txt}
 * and check whether this file has changed compared to previous versions.
 */
public class IPointDemo {
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length > 0) {
            System.setOut(new PrintStream(args[0]));
        }
        List<IPoint> p = new ArrayList<>(List.of(
                IPoint.of(12, 3),
                IPoint.of(12, 3, 1),
                IPoint.of(12, 3, 0),
                IPoint.of(12, 3, 0, 1234),
                IPoint.of(12, 3, 0, -21234),
                IPoint.of(-12, 123453, 27182, 821234),
                IPoint.of(14, -3),
                IPoint.of(0),
                IPoint.of(0, 0),
                IPoint.of(0, 2),
                IPoint.of(0, 1),
                IPoint.of(-1, -14),
                IPoint.of(-1, 1),
                IPoint.of(1, 4),
                IPoint.of(1, 1),
                IPoint.of(2, 4),
                IPoint.of(2, 3),
                IPoint.of(0, 0, 0),
                IPoint.origin(3),
                IPoint.of(new long[18]),
                IPoint.of(new long[18]),
                IPoint.of(13, 0),
                IPoint.of(-13, 0),
                IPoint.of(13, 0, 1),
                IPoint.of(3, 4, 0),
                IPoint.of(13),
                IPoint.of(Long.MIN_VALUE, Long.MIN_VALUE),
                IPoint.of(100, Long.MAX_VALUE),
                IPoint.of(Long.MIN_VALUE + 1, -2),
                IPoint.of(Long.MIN_VALUE + 1, -2),
                IPoint.of(Long.MIN_VALUE + 1, -3)));
        Collections.sort(p);
        long[] dimensions = {10, 10, 10};
        for (long[] ends : new long[][]{
                {0, 10},
                {0, Long.MAX_VALUE},
                {Long.MIN_VALUE, -100},
                {Long.MIN_VALUE + 1, -100},
                {Long.MIN_VALUE + 2, -100},
                {Long.MIN_VALUE + 2, 100},
        }) {
            System.out.println("Range " + ends[0] + ".." + ends[1] + " is "
                    + (IRange.isAllowedRange(ends[0], ends[1]) ? "allowed: " + IRange.of(ends[0], ends[1])
                    : "not allowed"));
        }
        for (IPoint ip : p) {
            System.out.println(ip + "; symmetric: " + ip.symmetric()
                    + "; distance from origin: " + ip.distanceFromOrigin()
                    + " = " + ip.distanceFrom(List.of(IPoint.origin(ip.coordCount())))
                    + "; x-shift: " + ip.shiftAlongAxis(0, 100)
                    + "; x-projection: "
                    + (ip.coordCount() == 1 ? "impossible" : ip.projectionAlongAxis(0))
                    + "; last-axis-projection: "
                    + (ip.coordCount() == 1 ? "impossible" : ip.projectionAlongAxis(ip.coordCount() - 1))
                    + "; shift in 10x10x10: " + ip.toOneDimensional(dimensions, true)
                    + "; *1.1: " + ip.multiply(1.1)
                    + " = " + ip.scale(Point.ofEqualCoordinates(ip.coordCount(), 1.1).coordinates())
                    + "; round *1.1: " + ip.roundedMultiply(1.1)
                    + " = " + ip.roundedScale(Point.ofEqualCoordinates(ip.coordCount(), 1.1).coordinates())
                    + " ~ " + ip.scaleAndShift(
                    Point.ofEqualCoordinates(ip.coordCount(), 1.1).coordinates(),
                    Point.origin(ip.coordCount()))
                    + "; sqr: " + ip.scalarProduct(ip)
                    + "; hash: " + ip.hashCode());
        }
        System.out.println();
        List<IRectangularArea> areas = new ArrayList<>();
        for (int k = 0; k < p.size() - 1; k += 2) {
            try {
                final IRectangularArea ra = IRectangularArea.of(p.get(k), p.get(k + 1));
                assert IRectangularArea.of(ra.ranges()).equals(ra);
                areas.add(ra);
            } catch (Exception e) {
                System.out.println("  Cannot create area with " + p.get(k) + " and " + p.get(k + 1) + ": " + e);
            }
        }
        areas.add(IRectangularArea.ofSize(10, -10, Long.MAX_VALUE - 10, Long.MAX_VALUE));
        areas.add(IRectangularArea.ofSize(10, 10, Long.MAX_VALUE - 10, Long.MAX_VALUE - 10));
        for (IRectangularArea ra : areas) {
            IRectangularArea expandPoint = null;
            try {
                expandPoint = ra.expand(IPoint.origin(ra.coordCount()));
            } catch (RuntimeException ignored) {
            }
            IRectangularArea expandRectangle = null;
            try {
                expandRectangle = ra.expand(IRectangularArea.of(
                        IPoint.ofEqualCoordinates(ra.coordCount(), -1),
                        IPoint.ofEqualCoordinates(ra.coordCount(), 2)));
            } catch (RuntimeException ignored) {
            }
            System.out.println(ra + "; ranges: " + java.util.Arrays.asList(ra.ranges())
                    + "; contains(origin): " + ra.contains(IPoint.origin(ra.coordCount()))
                    + "; expand(origin): " + (expandPoint == null ? "impossible" : expandPoint)
                    + "; expand(-1,-1..2,2): " + (expandRectangle == null ? "impossible" : expandRectangle)
                    + " hash: " + ra.hashCode());
        }
    }
}
