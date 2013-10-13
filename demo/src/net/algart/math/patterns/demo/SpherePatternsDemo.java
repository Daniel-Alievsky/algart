package net.algart.math.patterns.demo;

import net.algart.math.IPoint;
import net.algart.math.Point;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.math.patterns.UniformGridPattern;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>Simple test that creates sphere patterns.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class SpherePatternsDemo {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: " + SpherePatternsDemo.class.getName()
                + " numberOfDimensions radius center-x center-y ..."
                + " minimalPointCountForDecomposition [numberOfIterations]");
            return;
        }

        int dimCount = Integer.parseInt(args[0]);
        double r = Double.parseDouble(args[1]);
        double[] center = new double[dimCount];
        for (int k = 0; k < center.length; k++)
            center[k] = Double.parseDouble(args[2 + k]);
        int minimalPointCountForDecomposition = Integer.parseInt(args[2 + center.length]);
        int numberOfIterations = args.length > 3 + center.length ? Integer.parseInt(args[3 + center.length]) : 1;
        System.out.println("Sphere with center " + Point.valueOf(center) + " and radius " + r);
        long t1 = System.nanoTime();
        Pattern p = Patterns.newSphereIntegerPattern(Point.valueOf(center), r);
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Pattern created in %.3f ms: %s%n", (t2 - t1) * 1e-6, p);
        Set<Point> points = p.points();
        System.out.println(points.size() + " points");
        if (points.size() < 1000)
            System.out.println(new TreeSet<Point>(points));
        for (int iterationIndex = 1; iterationIndex <= numberOfIterations; iterationIndex++) {
            System.out.println();
            System.out.println("Iteration #" + iterationIndex);
            if (p instanceof UniformGridPattern && ((UniformGridPattern)p).isActuallyRectangular()) {
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
            System.out.println("Minimal coordinates: " + p.coordMin());
            System.out.println("Maximal coordinates: " + p.coordMax());
            for (int k = 0; k < p.dimCount(); k++) {
                System.out.println("Range of coordinate #" + k + ": " + p.coordRange(k));
            }
            points = p.round().lowerSurface(iterationIndex % dimCount).points();
            System.out.println(points.size() + " left points along axis #" + iterationIndex % dimCount + ":");
            if (points.size() < 1000)
                System.out.println(new TreeSet<Point>(points));
            points = p.round().upperSurface(iterationIndex % dimCount).points();
            System.out.println(points.size() + " right points along axis #" + iterationIndex % dimCount + ":");
            if (points.size() < 1000)
                System.out.println(new TreeSet<Point>(points));
            points = p.round().surface().points();
            System.out.println(points.size() + " boundary:");
            if (points.size() < 1000)
                System.out.println(new TreeSet<Point>(points));
            t1 = System.nanoTime();
            List<Pattern> minkowskiDecomposition = p.minkowskiDecomposition(minimalPointCountForDecomposition);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Minkowski decomposition to %d patterns (%.3f ms, %.3f ns / point):%n",
                minkowskiDecomposition.size(), (t2 - t1) * 1e-6, (t2 - t1) / (double)p.pointCount());
            for (Pattern q : minkowskiDecomposition) {
                System.out.println("    " + q);
            }
            System.out.println();

            t1 = System.nanoTime();
            List<List<Pattern>> allUnionDecompositions = p.allUnionDecompositions(minimalPointCountForDecomposition);
            t2 = System.nanoTime();
            for (int k = 0, n = allUnionDecompositions.size(); k < n; k++) {
                List<Pattern> unionDecomposition = allUnionDecompositions.get(k);
                System.out.printf(Locale.US, "Union decompositions #%d to %d patterns (%.3f ms, %.3f ns / point):%n",
                    k + 1, unionDecomposition.size(), (t2 - t1) * 1e-6, (t2 - t1) / (double) p.pointCount());
                for (Pattern q : unionDecomposition) {
                    System.out.println("    " + q
                        + (!q.hasMinkowskiDecomposition() ? " " + new TreeSet<IPoint>(q.roundedPoints()) : ""));
                }
                t1 = System.nanoTime();
                long totalPatternCount = 0, totalPointCount = 0;
                for (Pattern q : allUnionDecompositions.get(0)) {
                    List<Pattern> mink = q.minkowskiDecomposition(minimalPointCountForDecomposition);
                    totalPatternCount += mink.size();
                    for (Pattern s : mink)
                        totalPointCount += s.pointCount();
                }
                t2 = System.nanoTime();
                System.out.printf(Locale.US, "Minkowski decomposition of this union elements to %d patterns, "
                    + "%d total points (%.3f ms, %.3f ns / point)%n%n",
                    totalPatternCount, totalPointCount, (t2 - t1) * 1e-6, (t2 - t1) / (double)p.pointCount());
            }
        }
    }
}
