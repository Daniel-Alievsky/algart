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

package net.algart.math.points;

import net.algart.math.*;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The simplest test for {@link Point} class: shows a sorted array of several points.
 * You may call it with the argument like {@code additional/point_demo_reports/PointDemo_report.txt}
 * and check whether this file has changed compared to previous versions.
 */
public class PointDemo {
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length > 0) {
            System.setOut(new PrintStream(args[0]));
        }
        Range[] ranges = {
                Range.of(1.1, 2.7),
                Range.of(1.6, 2.8),
                Range.of(-27.1, 24.81),
                Range.of(0, Long.MAX_VALUE),
                Range.of(Long.MIN_VALUE, -100),
                Range.of(Long.MIN_VALUE + 1, -100),
                Range.of(Long.MIN_VALUE + 2000000, -100),
                Range.of(Long.MIN_VALUE + 2000000, 100),
                Range.of(-1e3, 1e8),
                Range.of(-4e18, 6e18),
        };
        for (Range r : ranges) {
            IRange cast = null, rounded = null;
            try {
                cast = r.toIntegerRange();
                assert IRange.of(r).equals(cast);
                System.out.println(r + " casted to " + cast);
            } catch (Exception e) {
                System.out.println("  Cannot call toIntegerRange for " + r + ": " + e);
            }
            try {
                rounded = r.toRoundedRange();
                assert IRange.roundOf(r).equals(rounded);
                System.out.println(r + " rounded to " + rounded);
            } catch (Exception e) {
                System.out.println("  Cannot call toRoundedRange for " + r + ": " + e);
            }
            try {
                IRange ir = IRange.of(r);
                assert ir.equals(cast);
                System.out.println(r + " casted to " + ir);
            } catch (Exception e) {
                System.out.println("  Cannot cast range " + r + ": " + e);
            }
            try {
                IRange ir = IRange.roundOf(r);
                assert ir.equals(rounded);
                System.out.println(r + " rounded to " + ir);
            } catch (Exception e) {
                System.out.println("  Cannot round range " + r + ": " + e);
            }
        }
        System.out.println();
        List<Point> p = new ArrayList<>(List.of(
                Point.of(12, 3),
                Point.of(1.2, 3, 1),
                Point.of(12, 3, 0),
                Point.of(1.2, 3, 0, 1.234),
                Point.of(12, 3, 0, -21234),
                Point.of(-12, 123453, 27182, 821234),
                Point.of(14, -3),
                Point.of(0),
                Point.of(0, 0),
                Point.of(-3, -2),
                Point.of(-4, 5),
                Point.of(15, 20),
                Point.of(3, 1.33),
                Point.of(4.1, 5),
                Point.of(0, 0, 0),
                Point.origin(3),
                Point.of(new double[18]),
                Point.of(new double[18]),
                Point.of(13, 0.0),
                Point.of(13, -0.0),
                Point.of(4413.1, 0.1),
                Point.of(4413.2, -0.8),
                Point.of(13, 1, 0),
                Point.of(3, 4, 0),
                Point.of(13),
                Point.of(1e3, 30),
                Point.of(1e3, 1e20),
                Point.of(-5e18, 30),
                Point.of(-5e18, -300),
                Point.of(-7e18, -5e18),
                Point.of(5e18, 1e20)));
        Collections.sort(p);
        for (Point rp : p) {
            System.out.println(rp + "; symmetric: " + rp.symmetric()
                    + "; distance from origin: " + rp.distanceFromOrigin()
                    + " = " + rp.distanceFrom(List.of(Point.origin(rp.coordCount())))
                    + "; x-shift: " + rp.shiftAlongAxis(0, 100.0)
                    + "; x-projection: "
                    + (rp.coordCount() == 1 ? "impossible" : rp.projectionAlongAxis(0))
                    + "; last-axis-projection: "
                    + (rp.coordCount() == 1 ? "impossible" : rp.projectionAlongAxis(rp.coordCount() - 1))
                    + "; *2: " + rp.multiply(2.0)
                    + " = " + rp.scale(IPoint.ofEqualCoordinates(rp.coordCount(), 2).toPoint().coordinates())
                    + " = " + rp.scaleAndShift(
                    Point.ofEqualCoordinates(rp.coordCount(), 2.0).coordinates(),
                    Point.origin(rp.coordCount()))
                    + "; sqr: " + rp.scalarProduct(rp)
                    + "; hash: " + rp.hashCode());
        }
        System.out.println();
        List<RectangularArea> areas = new ArrayList<>();
        for (int k = 0; k < p.size() - 1; k += 2) {
            System.out.print(k + ": ");
            try {
                RectangularArea ra = RectangularArea.of(p.get(k), p.get(k + 1));
                assert RectangularArea.of(ra.ranges()).equals(ra);
                Point point = Point.ofEqualCoordinates(ra.coordCount(), -1.5);
                RectangularArea test = RectangularArea.of(point, Point.origin(ra.coordCount()));
                assert ra.intersects(test) ? ra.intersection(test).equals(RectangularArea.of(
                        ra.min().max(test.min()), ra.max().min(test.max()))) :
                        ra.intersection(test) == null;
                areas.add(ra);
                System.out.println(ra + "; ranges: " + java.util.Arrays.asList(ra.ranges())
                        + "; contains(origin): " + ra.contains(Point.origin(ra.coordCount()))
                        + "; expand(origin): " + ra.expand(Point.origin(ra.coordCount()))
                        + "; expand(-1,-1..2,2): " + ra.expand(RectangularArea.of(
                        Point.ofEqualCoordinates(ra.coordCount(), -1),
                        Point.ofEqualCoordinates(ra.coordCount(), 2)))
                        + "; parallel distance to (-1.5,-1.5,...): "
                        + (ra.coordCount() == 2 ? ra.parallelDistance(-1.5, -1.5) : ra.coordCount() == 3 ?
                        ra.parallelDistance(-1.5, -1.5, -1.5) :
                        ra.parallelDistance(point))
                        + (ra.contains(point) ? " (inside)" : "")
                        + "; intersection with " + test + ": " + ra.intersection(test)
                        + "; subtracted " + test + ": " + ra.difference(new ArrayList<>(), test)
                        + "; hash: " + ra.hashCode());
            } catch (Exception e) {
                System.out.println("  Cannot create area with " + p.get(k) + " and " + p.get(k + 1) + ": " + e);
            }
        }
        for (RectangularArea ra : areas) {
            IRectangularArea cast = null, rounded = null;
            try {
                cast = ra.toIntegerRectangularArea();
                assert IRectangularArea.of(ra).equals(cast);
                System.out.println(ra + " casted to " + cast);
            } catch (Exception e) {
                System.out.println("  Cannot call toIntegerRectangularArea for " + ra + ": " + e);
            }
            try {
                rounded = ra.toRoundedRectangularArea();
                assert IRectangularArea.roundOf(ra).equals(rounded);
                IPoint point = IPoint.ofEqualCoordinates(ra.coordCount(), 10);
                IRectangularArea test = IRectangularArea.of(IPoint.origin(ra.coordCount()), point);
                assert rounded.intersects(test) ? rounded.intersection(test).equals(IRectangularArea.of(
                        rounded.min().max(test.min()), rounded.max().min(test.max()))) :
                        rounded.intersection(test) == null;
                System.out.println(ra + " rounded to " + rounded
                        + "; parallel distance to (10,10,...): "
                        + (ra.coordCount() == 2 ? rounded.parallelDistance(10, 10) : ra.coordCount() == 3 ?
                        rounded.parallelDistance(10, 10, 10) :
                        rounded.parallelDistance(point))
                        + (rounded.contains(point) ? " (inside)" : "")
                        + "; intersection with " + test + ": " + rounded.intersection(test)
                        + "; subtracted " + test + ": "
                        + rounded.difference(new ArrayList<>(), test)
                        + "; hash: " + rounded.hashCode()
                );
            } catch (Exception e) {
                System.out.println("  Cannot call toRoundedRectangularArea for " + ra + ": " + e);
            }
            try {
                IRectangularArea ira = IRectangularArea.of(ra);
                assert ira.equals(cast);
                System.out.println(ra + " casted to " + ira);
            } catch (Exception e) {
                System.out.println("  Cannot cast range " + ra + ": " + e);
            }
            try {
                final IRectangularArea ira = IRectangularArea.roundOf(ra);
                assert ira.equals(rounded);
                System.out.println(ra + " rounded to " + ira);
            } catch (Exception e) {
                System.out.println("  Cannot round range " + ra + ": " + e);
            }
        }
    }
}
