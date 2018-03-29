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

package net.algart.math.patterns.demo;

import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.Point;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.math.patterns.QuickPointCountPattern;
import net.algart.math.patterns.UniformGridPattern;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>Simple test that creates rectangular patterns.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class RectangularPatternsDemo {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: " + RectangularPatternsDemo.class.getName()
                + " dimCount originX originY ... gridMinX gridMaxX gridMinY gridMaxY ..."
                + " step minimalPointCountForDecomposition");
            return;
        }

        int dimCount = Integer.parseInt(args[0]);
        double[] origin = new double[dimCount];
        int argIndex = 0;
        for (int k = 0; k < dimCount; k++) {
            origin[k] = Double.parseDouble(args[++argIndex]);
        }
        IRange[] ranges = new IRange[dimCount];
        for (int k = 0; k < ranges.length; k++) {
            ranges[k] = IRange.valueOf(Long.parseLong(args[++argIndex]), Long.parseLong(args[++argIndex]));
        }
        double step = Double.parseDouble(args[++argIndex]);
        int minimalPointCountForDecomposition = Integer.parseInt(args[++argIndex]);
        UniformGridPattern p = Patterns.newRectangularIntegerPattern(ranges)
            .multiply(step).shift(Point.valueOf(origin));
        System.out.println("Created pattern: " + p);
        System.out.println("Integer number of points: " + p.pointCount());
        System.out.println("Approximate number of points: " + p.largePointCount());
        if (p instanceof QuickPointCountPattern && ((QuickPointCountPattern) p).isPointCountVeryLarge()) {
            System.out.println("It is very large");
        }
        if (p.isActuallyRectangular()) {
            System.out.println("It is rectangular");
        }
        if (p.isSurelySinglePoint()) {
            System.out.println("It is 1-point");
        }
        if (p.isSurelyOriginPoint()) {
            System.out.println("It is origin of coordinates");
        }
        if (p.isSurelyInteger()) {
            System.out.println("It is integer");
        }
        if (p.pointCount() < 1000) {
            Pattern samePoints = Patterns.newPattern(p.points());
            System.out.println("Pattern with same points: " + samePoints);
            if (samePoints instanceof UniformGridPattern && ((UniformGridPattern)samePoints).isActuallyRectangular()) {
                System.out.println("It is also rectangular");
            }
        }
        System.out.println("Grid origin: " + p.originOfGrid());
        System.out.println("Grid steps vector: " + Point.valueOf(p.stepsOfGrid()));
        System.out.println("Minimal coordinates: " + p.coordMin());
        System.out.println("Maximal coordinates: " + p.coordMax());
        System.out.println("Minimal grid indexes: " + p.gridIndexMin());
        System.out.println("Maximal grid indexes: " + p.gridIndexMax());
        Set<IPoint> roundedPoints = p.roundedPoints();
        System.out.println(roundedPoints.size() + " rounded points");
        if (roundedPoints.size() < 1000) {
            System.out.println(new TreeSet<IPoint>(roundedPoints));
        }
        Set<Point> points = p.points();
        System.out.println(points.size() + " points");
        if (points.size() < 1000) {
            System.out.println(new TreeSet<Point>(points));
        }
        points = p.lowerSurface(0).points();
        System.out.println(points.size() + " left points:");
        if (points.size() < 1000) {
            System.out.println(new TreeSet<Point>(points));
        }
        points = p.upperSurface(0).points();
        System.out.println(points.size() + " right points:");
        if (points.size() < 1000) {
            System.out.println(new TreeSet<Point>(points));
        }
        points = p.surface().points();
        System.out.println(points.size() + " boundary:");
        if (points.size() < 1000) {
            System.out.println(new TreeSet<Point>(points));
        }
        System.out.println("Carcass:");
        System.out.println("    " + p.carcass() + " " + new TreeSet<Point>(p.carcass().points()));
        List<Pattern> unionDecomposition = p.unionDecomposition(minimalPointCountForDecomposition);
        System.out.println("Union decomposition to " + unionDecomposition.size() + " patterns:");
        for (Pattern q : unionDecomposition) {
            System.out.println("    " + q
                + (!q.hasMinkowskiDecomposition() ? " " + new TreeSet<Point>(q.points()) : ""));
        }
        List<Pattern> minkowskiDecomposition = p.minkowskiDecomposition(minimalPointCountForDecomposition);
        System.out.println("Minkowski decomposition to " + minkowskiDecomposition.size() + " patterns:");
        for (Pattern q : minkowskiDecomposition) {
            System.out.println("    " + q
                + (!q.hasMinkowskiDecomposition() ? " " + new TreeSet<Point>(q.points()) : ""));
        }
    }
}
