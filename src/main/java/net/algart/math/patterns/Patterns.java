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

package net.algart.math.patterns;

import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.Point;
import net.algart.math.functions.Func;

import java.math.BigDecimal;
import java.util.*;

/**
 * <p>A set of static methods operating with and returning {@link Pattern patterns}.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class Patterns {
    private Patterns() {
    }

    private static final BigDecimal BIG_DECIMAL_MAX_COORDINATE = new BigDecimal(Pattern.MAX_COORDINATE);

    // TODO!! Note: this method always returns SimplePattern or UniformGridPattern.
    public static DirectPointSetPattern newPattern(Collection<Point> points) {
        Objects.requireNonNull(points, "Null points argument");
        points = new ArrayList<>(points);
        boolean allInteger = true;
        for (Point p : points) {
            Objects.requireNonNull(p, "Null point in the set");
            allInteger &= p.isInteger() & AbstractUniformGridPattern.isAllowedGridIndex(p.toIntegerPoint());
        }
        if (allInteger) {
            HashSet<IPoint> integerPoints = new HashSet<>();
            for (Point p : points) {
                integerPoints.add(p.toIntegerPoint());
            }
            return newIntegerPattern(integerPoints);
        }
        if (points.size() <= 2) {
            final Point[] pointsArray = points.toArray(new Point[0]);
            if (pointsArray.length == 1 || pointsArray[0].equals(pointsArray[1])) {
                return new OnePointPattern(pointsArray[0]);
            } else {
                return new TwoPointsPattern(pointsArray[0], pointsArray[1]);
            }
        } else {
            return new SimplePattern(points);
        }
    }

    // TODO!! Note: this method always returns SimplePattern or UniformGridPattern.
    public static DirectPointSetPattern newPattern(Point... points) {
        Objects.requireNonNull(points, "Null points argument");
        return newPattern(List.of(points));
    }

    public static DirectPointSetUniformGridPattern newUniformGridPattern(
            Point originOfGrid,
            double[] stepsOfGrid,
            Collection<IPoint> gridIndexes) {
        Objects.requireNonNull(originOfGrid, "Null originOfGrid");
        Objects.requireNonNull(gridIndexes, "Null gridIndexes argument");
        return new BasicDirectPointSetUniformGridPattern(originOfGrid, stepsOfGrid, new HashSet<>(gridIndexes));
    }

    /**
     * Creates new pattern consisting of all specified points.
     *
     * <p>The returned pattern is so-called <i>simple</i>, or <i>multipoint</i> pattern:
     * it means that its implementation is based on a usual set of {@link IPoint} instances.
     * For such pattern, the {@link Pattern#dimCount() number of dimensions} is equal to
     * the {@link IPoint#coordCount() number of coordinates}
     * in all passed points (it must be the same for all source points).
     * The {@link UniformGridPattern#isActuallyRectangular()} method is non-overridden {@link AbstractUniformGridPattern#isActuallyRectangular()} method.
     * The {@link Pattern#minkowskiAdd(Pattern)} method returns a new <i>simple</i> pattern
     * according the definition of Minkowski sum; this method requires <i>O</i>(<i>NM</i>) operations,
     * where <i>N</i> and <i>M</i> is the number of points in both patterns, and may work slowly.
     * The {@link Pattern#minkowskiDecomposition(int)} method returns the list containing
     * this pattern instance as the only element.
     * The <code>equals</code> method returns <code>true</code> if and only if the passed object is also simple
     * pattern, consisting of the same points and created by this or equivalent method from this package.
     *
     * <p>The coordinates of all points must be in range <code>-Long.MAX_VALUE/2..Long.MAX_VALUE/2</code>,
     * excepting the only case when the number of points is 1.
     *
     * <p>The returned pattern will be "safe" in the sense that no references to the passed set are maintained by it.
     * In other words, this method always allocates a new set (probably <code>HashSet</code>) and copies
     * the passed set into it.
     *
     * @param points the source points set.
     * @return the pattern consisting of all source points.
     * @throws NullPointerException     if <code>points</code> argument is {@code null}
     *                                  or some of passed points is {@code null}.
     * @throws IllegalArgumentException if <code>points</code> argument is an empty set,
     *                                  or if some points have different number of coordinates,
     *                                  or if there are 2 or more points and some point coordinates are out of range
     *                                  <code>-Long.MAX_VALUE/2..Long.MAX_VALUE/2</code>.
     */
    public static DirectPointSetUniformGridPattern newIntegerPattern(Collection<IPoint> points) {
        Objects.requireNonNull(points, "Null points argument");
        HashSet<IPoint> gridIndexes = new HashSet<>(points);
        return new BasicDirectPointSetUniformGridPattern(
                gridIndexes.isEmpty() ? 1 : gridIndexes.iterator().next().coordCount(),
                gridIndexes);
    }

    /**
     * Equivalent to <code>{@link #newIntegerPattern
     * newIntegerPattern}List.of(points)))</code>.
     *
     * @param points the source points set.
     * @return the pattern consisting of all source points.
     * @throws NullPointerException     if <code>points</code> argument is {@code null}
     *                                  or some of passed points is {@code null}.
     * @throws IllegalArgumentException if <code>points</code> argument is an empty array,
     *                                  or if some points have different number of coordinates,
     *                                  or if there are 2 or more points and some point coordinates are out of range
     *                                  <code>-Long.MAX_VALUE/2..Long.MAX_VALUE/2</code>.
     */
    public static DirectPointSetUniformGridPattern newIntegerPattern(IPoint... points) {
        Objects.requireNonNull(points, "Null points argument");
        return newIntegerPattern(List.of(points));
    }

    /**
     * A concrete variant of {@link #newIntegerPattern} method returning
     * the pattern consisting of all such points inside <i>n</i>-dimensional sphere
     * with the given radius <i>r</i> and center.
     *
     * <p>More precisely, let
     * <i>C</i>=(<i>c</i><sub>0</sub>,<i>c</i><sub>1</sub>,...,<i>c</i><sub><i>n</i>-1</sub>)
     * is the passed center point.
     * The result of this method consists of all points
     * <i>A</i>=(<i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>n</i>-1</sub>)
     * that |<i>OA</i>|<sup>2</sup> =
     * (<i>x</i><sub>0</sub>-<i>c</i><sub>0</sub>)<sup>2</sup>
     * + (<i>x</i><sub>1</sub>-<i>c</i><sub>1</sub>)<sup>2</sup>
     * + ... + (<i>x</i><sub><i>n</i>-1</sub>-<i>c</i><sub><i>n</i>-1</sub>)<sup>2</sup>
     * &le; <i>r</i><sup>2</sup>.
     *
     * <p>The {@link Pattern#roundedCoordRange(int)} method in the returned pattern works very quickly,
     * unlike patterns created by {@link #newIntegerPattern} method.
     *
     * <p>Please note: integer values of the radius usually produce "non-beautiful" patterns
     * with very uneven edges. For example, in 2-dimensional case (circles)
     * we recommend to pass <i>r</i>~<i>k</i>-0.2, where <i>k</i> is a positive integer.
     *
     * <p>Please be careful: too large values of <code>r</code> argument may lead to
     * very long execution and even to <code>OutOfMemoryError</code>,
     * especially for 2- and 3-dimensional patterns.
     * Moreover, there are no ways to interrupt this method or to show its execution progress.
     * We recommend to restrict the passed radius by some suitable value,
     * for example, 1000&ndash;2000 or less.
     *
     * @param center the center of the sphere (circle in 2-dimensional case).
     * @param r      the radius of the sphere.
     * @return the pattern consisting of all points inside this sphere (circle in 2-dimensional case).
     * @throws IllegalArgumentException    if <code>r&lt;0.0</code>,
     * @throws TooManyPointsInPatternError if <code>r</code> is about <code>Integer.MAX_VALUE</code> or greater.
     */
    public static UniformGridPattern newSphereIntegerPattern(Point center, final double r) {
        Objects.requireNonNull(center, "Null center argument");
        if (r < 0.0) {
            throw new IllegalArgumentException("Negative sphere radius");
        }
        double[] semiAxes = new double[center.coordCount()];
        java.util.Arrays.fill(semiAxes, r);
        return newEllipsoidIntegerPattern(center, semiAxes);
/* OLD ALGORITHM
        final int n = center.coordCount();
        if (n == 1)
            return newIntegerPattern(newRectangularIntegerPattern(IRange.valueOf(
                StrictMath.round(StrictMath.ceil(center.coord(0) - r)),
                StrictMath.round(StrictMath.floor(center.coord(0) + r)))).roundedPoints());
        int ir = (int) (r + 2.0);
        if (ir == Integer.MAX_VALUE)
            throw new TooManyPointsInPatternError("Too large desired " + n + "D sphere radius: " + r);
        final IRange[] oneSegmentCoordRanges = new IRange[n];
        HashSet<IPoint> points = new HashSet<IPoint>();
        for (int k = 0; k < oneSegmentCoordRanges.length; k++) {
            addPointsToSphere(points, new double[]{center.coord(k)}, new long[1], 0, ir, r * r, 0.0);
            // for checking maximal radius by common algorithm; new double/long here are zero-filled
            oneSegmentCoordRanges[k] = new BasicUniformGridPattern(1, points).gridIndexRange(0);
            points.clear();
        }
        addPointsToSphere(points, center.coordinates(), new long[n], 0, ir, r * r, 0.0);
        return new BasicUniformGridPattern(n, points) {
            @Override
            public IRange gridIndexRange(int coordIndex) {
                return oneSegmentCoordRanges[coordIndex];
            }

            public String toString() {
                return super.toString() + " ("
                    + (dimCount == 1 ? "segment" : dimCount == 2 ? "circle" : "sphere")
                    + ", r = " + r + ")";
            }
        };
*/
    }

    public static UniformGridPattern newEllipsoidIntegerPattern(Point center, double... semiAxes) {
        Objects.requireNonNull(center, "Null center argument");
        Objects.requireNonNull(semiAxes, "Null semiAxes argument");
        final int n = center.coordCount();
        if (semiAxes.length != n) {
            throw new IllegalArgumentException("Number of semi-axes " + semiAxes.length
                    + " is not equal to center.coordCount()=" + n);
        }
        final double[] semiAxesClone = semiAxes.clone();
        final double[] semiAxesInv = new double[n];
        final int[] semiAxesUpperBounds = new int[n];
        for (int k = 0; k < n; k++) {
            double semiAxis = semiAxesClone[k];
            if (semiAxis < 0.0) {
                throw new IllegalArgumentException("Negative semiAxes[" + k + "] = " + semiAxis);
            }
            semiAxesUpperBounds[k] = (int) (semiAxis + 2.0);
            if (semiAxesUpperBounds[k] == Integer.MAX_VALUE) {
                throw new TooManyPointsInPatternError("Too large desired " + n + "D ellipsoid: semiAxes["
                        + k + "]=" + semiAxis);
            }
            semiAxesInv[k] = 1.0 / semiAxis; // maybe Infinity
        }
        if (n == 1) {
            return newIntegerPattern(newRectangularIntegerPattern(IRange.valueOf(
                    StrictMath.round(StrictMath.ceil(center.coord(0) - semiAxesClone[0])),
                    StrictMath.round(StrictMath.floor(center.coord(0) + semiAxesClone[0])))).roundedPoints());
        }
        final IRange[] oneSegmentCoordRanges = new IRange[n];
        HashSet<IPoint> points = new HashSet<>();
        for (int k = 0; k < oneSegmentCoordRanges.length; k++) {
            addPointsToEllipsoid(points,
                    new double[]{center.coord(k)},
                    new int[]{semiAxesUpperBounds[k]},
                    new double[]{semiAxesInv[k]},
                    new long[]{0}, 0, 0.0);
            // 1-dimensional "ellipsoid" to get maximal radius by common algorithm
            oneSegmentCoordRanges[k] = new BasicDirectPointSetUniformGridPattern(1, points).gridIndexRange(0);
            points.clear();
        }
        addPointsToEllipsoid(points, center.coordinates(), semiAxesUpperBounds, semiAxesInv,
                new long[n], // zero-filled
                0, 0.0);
        return new BasicDirectPointSetUniformGridPattern(n, points) {
            @Override
            public IRange gridIndexRange(int coordIndex) {
                return oneSegmentCoordRanges[coordIndex];
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < dimCount; k++) {
                    if (k > 0) {
                        sb.append(",");
                    }
                    sb.append(semiAxesClone[k]);
                }
                return super.toString() + " ("
                        + (dimCount == 1 ? "segment" : dimCount == 2 ? "ellipse" : "ellipsoid")
                        + ", semiAxes = " + sb + ")";
            }
        };
    }

    public static Pattern newSurface(Pattern projection, final Func surface) {
        Objects.requireNonNull(projection, "Null projection argument");
        Objects.requireNonNull(surface, "Null surface argument");
        final int dimCount = projection.dimCount();
        final Set<Point> projectionPoints = projection.points();
        Set<Point> resultPoints = new HashSet<>();
        double[] coordinates = new double[dimCount];
        double[] resultPoint = new double[dimCount + 1];
        for (Point projectionPoint : projectionPoints) {
            projectionPoint.coordinates(coordinates);
            System.arraycopy(coordinates, 0, resultPoint, 0, dimCount);
            resultPoint[dimCount] = surface.get(coordinates);
            resultPoints.add(Point.valueOf(resultPoint));
        }
        return new SimplePattern(resultPoints) {
            public String toString() {
                return super.toString() + " (surface " + surface + ")";
            }
        };
    }

    public static UniformGridPattern newSpaceSegment(
            UniformGridPattern projection,
            final Func minSurface,
            final Func maxSurface,
            double lastCoordinateOfOrigin,
            double lastCoordinateStep) {
        Objects.requireNonNull(projection, "Null projection argument");
        Objects.requireNonNull(minSurface, "Null minSurface argument");
        Objects.requireNonNull(maxSurface, "Null maxSurface argument");
        if (lastCoordinateStep <= 0.0) {
            throw new IllegalArgumentException("Zero or negative last step of the grid is not allowed");
        }
        final int dimCount = projection.dimCount();
        final Set<IPoint> projectionIndexes = projection.gridIndexes();
        final Point projectionOrigin = projection.originOfGrid();
        final double[] projectionSteps = projection.stepsOfGrid();
        double[] origin = new double[dimCount + 1];
        projectionOrigin.coordinates(origin);
        origin[dimCount] = lastCoordinateOfOrigin;
        double[] steps = new double[dimCount + 1];
        System.arraycopy(projectionSteps, 0, steps, 0, dimCount);
        steps[dimCount] = lastCoordinateStep;
        Set<IPoint> resultIndexes = new HashSet<>();
        double[] coordinates = new double[dimCount];
        long[] resultIndex = new long[dimCount + 1];
        for (IPoint projectionIndex : projectionIndexes) {
            projectionIndex.scaleAndShift(coordinates, projectionSteps, projectionOrigin);
            double min = minSurface.get(coordinates);
            double max = maxSurface.get(coordinates);
            if (min > max) {
                continue;
            }
            long minIndex = (long) StrictMath.ceil(lastCoordinateStep == 1.0 ? min - lastCoordinateOfOrigin :
                    (min - lastCoordinateOfOrigin) / lastCoordinateStep);
            long maxIndex = (long) StrictMath.floor(lastCoordinateStep == 1.0 ? max - lastCoordinateOfOrigin :
                    (max - lastCoordinateOfOrigin) / lastCoordinateStep);
            projectionIndex.coordinates(resultIndex);
            for (long i = minIndex; i <= maxIndex; i++) {
                resultIndex[dimCount] = i;
                resultIndexes.add(IPoint.valueOf(resultIndex));
            }
        }
        if (resultIndexes.isEmpty()) {
            throw new IllegalArgumentException("Empty pattern: in all points of the projection pattern "
                    + "the minimal surface is above the maximal surface");
        }
        return new BasicDirectPointSetUniformGridPattern(Point.valueOf(origin), steps, resultIndexes) {
            public String toString() {
                return super.toString() + " (segment between surfaces " + minSurface + " and " + maxSurface + ")";
            }
        };
    }


    public static RectangularPattern newRectangularUniformGridPattern(
            Point originOfGrid,
            double[] stepsOfGrid,
            IRange... gridIndexRanges) {
        Objects.requireNonNull(originOfGrid, "Null originOfGrid");
        Objects.requireNonNull(gridIndexRanges, "Null gridIndexRanges argument");
        if (gridIndexRanges.length == 0) {
            throw new IllegalArgumentException("Empty gridIndexRanges array");
        }
        return new BasicRectangularPattern(originOfGrid, stepsOfGrid, gridIndexRanges);
    }

    /**
     * Creates new {@link UniformGridPattern#isActuallyRectangular() rectangular} pattern
     * (<i>n</i>-dimensional rectangular parallelepiped), consisting of all such points
     * (<i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>n</i>-1</sub>) that
     * <code>ranges[<i>i</i>].{@link IRange#min()
     * min()}</code>&lt;=<i>x</i><sub><i>i</i></sub>&lt;=<code>ranges[<i>i</i>].{@link IRange#max()
     * max()}</code>, <i>i</i>=0,1,...,<i>n</i>-1, <i>n</i>=<code>ranges.length</code>.
     * The number of space dimensions of the returned pattern will be equal to the length of <code>ranges</code> array.
     *
     * <p>In the returned pattern,
     * the {@link UniformGridPattern#isActuallyRectangular()} method returns <code>true</code> always.
     * The {@link Pattern#minkowskiAdd(Pattern)} method creates a new rectangular pattern (via this method),
     * if its argument is also rectangular, according the definition of Minkowski sum;
     * in other case, it returns new {@link #newIntegerPattern simple} pattern and works slowly
     * (<i>O</i>(<i>NM</i>) operations, where <i>N</i> and <i>M</i> is the number of points in both patterns).
     * The {@link Pattern#minkowskiDecomposition(int)} method returns the list containing
     * ~log<sub>2</sub>(<i>N</i>) 2-point simple patterns or, maybe,
     * the list containing one 1-point pattern if this rectangular pattern is degenerate
     * (1-point).
     * The <code>equals</code> method returns <code>true</code> if and only if the passed object is also rectangular
     * pattern, consisting of the same points and created by this or equivalent method from this package.
     *
     * <p>The returned pattern will be "safe" in the sense that no references to the passed array are maintained by it.
     * In other words, this method always allocates new array for storing ranges and copies
     * the passed ranges into it.
     *
     * @param ranges the source ranges describing <i>n</i>-dimensional rectangular parallelepiped.
     * @return the rectangular pattern.
     * @throws NullPointerException     if <code>ranges</code> argument is {@code null}
     *                                  or some of passed ranges are {@code null}.
     * @throws IllegalArgumentException if <code>ranges</code> argument is empty (contains no elements).
     */
    public static RectangularPattern newRectangularIntegerPattern(IRange... ranges) {
        Objects.requireNonNull(ranges, "Null ranges argument");
        if (ranges.length == 0) {
            throw new IllegalArgumentException("Empty ranges array");
        }
        return new BasicRectangularPattern(ranges);
    }

    /**
     * Equivalent to {@link #newRectangularIntegerPattern newRectangularIntegerPattern(ranges)},
     * where <code>ranges[k]</code> is <code>{@link IRange#valueOf(long, long)
     * IRange.valueOf}(min.{@link IPoint#coord(int) coord(k)}, max.{@link IPoint#coord(int) coord(k)})</code>.
     * The number of dimensions of the created pattern is equal to the number of coordinates
     * of <code>min</code> and <code>max</code> points.
     *
     * <p>The number of coordinates of <code>min</code> and <code>max</code> points must be the same.
     * All coordinates of <code>min</code> point must not be greater than the corresponding coordinates
     * of the <code>max</code> points.
     *
     * @param min starting points of the parallelepiped (left top vertex in 2D case), inclusive.
     * @param max ending points of the parallelepiped (left top vertex in 2D case), inclusive.
     * @return the rectangular pattern, two vertices of which are the passed points.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the numbers of coordinates of <code>min</code> and <code>max</code>
     *                                  points are different,
     *                                  or if <code>min.{@link IPoint#coord(int) coord}(k) &gt;
     *                                  max.{@link IPoint#coord(int) coord}(k)</code> for some <code>k</code>,
     *                                  or if the difference between these coordinates is
     *                                  <code>&gt;=Long.MAX_VALUE</code>.
     */
    public static UniformGridPattern newRectangularIntegerPattern(IPoint min, IPoint max) {
        int n = min.coordCount();
        if (n != max.coordCount()) {
            throw new IllegalArgumentException("Coordinates count mismatch: \"min\" is "
                    + n + "-dimensional, \"max\" is " + max.coordCount() + "-dimensional");
        }
        IRange[] ranges = new IRange[n];
        for (int k = 0; k < n; k++) {
            ranges[k] = IRange.valueOf(min.coord(k), max.coord(k));
        }
        return new BasicRectangularPattern(ranges);
    }


//    /**
//     * Returns <code>true</code> if and only if the given pattern was created by
//     * {@link #newRectangularIntegerPattern(IRange...)} or equivalent method from this package.
//     *
//     * <p>Please compare: unlike this, {@link UniformGridPattern#isRectangular()} method returns <code>true</code>
//     * for any rectangular patterns, in particular, for ones created by {@link #newIntegerPattern(Collection)}
//     * method if this set is rectangular (for example, the single point).
//     *
//     * @param pattern the checked pattern.
//     * @return        <code>true</code> if and only if the given pattern was created by
//     *                {@link #newRectangularIntegerPattern(IRange...)} or equivalent method from this package.
//     */
//    public static boolean isRectangular(Pattern pattern) {
//        if (!NEW_FLOATING_POINT_PATTERNS) {
//            return pattern instanceof RectangularIntegerPattern || pattern instanceof OnePointIntegerPattern;
//        }
//        return pattern instanceof RectangularGridPattern;
//    }

    /**
     * Creates new pattern, representing the Minkowski sum of all passed patterns.
     * Please see <a href="http://en.wikipedia.org/wiki/Minkowski_addition">Wikipedia</a> about the
     * "Minkowski sum" term. See also {@link Pattern#minkowskiAdd(Pattern)} method for comparison.
     *
     * <p>Note: this method does not actually calculate the point set of the sum;
     * if necessary, you will be able to get all points later by {@link Pattern#roundedPoints()} method.
     *
     * <p>In the returned pattern,
     * the {@link Pattern#minkowskiDecomposition(int)} method returns
     * the summary list of Minkowski decompositions of all patterns passed to this method.
     * The {@link UniformGridPattern#isActuallyRectangular()} method is non-overridden
     * {@link AbstractUniformGridPattern#isActuallyRectangular()} method.
     * The {@link Pattern#minkowskiAdd(Pattern)} method creates a new Minkowski sum by this method,
     * that contains an additional summand equal to the added pattern.
     * The <code>equals</code> method returns <code>true</code> if and only if the passed object is also Minkowski
     * sum, consisting of the same summands and created by this or equivalent method from this package.
     *
     * <p>If all passed patterns are {@link UniformGridPattern#isActuallyRectangular() rectangular}, the behavior of this method
     * is another: it just returns {@link #newRectangularIntegerPattern new rectangular pattern}
     * equal to the Minkowski sum of all passed patterns.
     *
     * <p>The returned pattern will be "safe" in the sense that no references to the passed array are maintained by it.
     * In other words, this method always allocates new array for storing summands and copies
     * the passed summands into it.
     *
     * @param patterns the summands of the returned Minkowski sum.
     * @return the Minkowski sum.
     * @throws NullPointerException     if <code>patterns</code> argument is {@code null}
     *                                  or some of passed patterns are {@code null}.
     * @throws IllegalArgumentException if <code>patterns</code> argument is an empty array,
     *                                  or if some patterns have different number of dimensions,
     *                                  or if some of the {@link Pattern#roundedCoordRange(int)
     *                                  coordinate ranges} in the
     *                                  precise result are out of <code>Long.MIN_VALUE..Long.MAX_VALUE</code> range
     *                                  or have the size (maximum - minimum), greater or equal to
     *                                  <code>Long.MAX_VALUE</code>.
     */
    public static Pattern newMinkowskiSum(Pattern... patterns) {
        return newMinkowskiSum(List.of(patterns));
    }

    //TODO!! not forget to comment a case when all patterns are compatible rectangular patterns
    //TODO!! write about limited precision: the result can little differ from the precise sum,
    // if some of pattern are not integer patterns
    public static Pattern newMinkowskiSum(Collection<Pattern> patterns) {
        Objects.requireNonNull(patterns, "Null patterns argument");
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("Empty patterns array");
        }
        Pattern[] patternsArray = patterns.toArray(new Pattern[0]);
        // cloning before checking guarantees correct behavior while multithreading
        boolean allCompatibleRectangular = true;
        Pattern first = patternsArray[0];
        for (int k = 0; k < patternsArray.length; k++) {
            Objects.requireNonNull(patternsArray[k], "Null pattern #" + k + " in the list");
            if (patternsArray[k].dimCount() != first.dimCount()) {
                throw new IllegalArgumentException("Patterns dimensions mismatch: the first pattern has "
                        + first.dimCount() + " dimensions, but pattern #" + k + " has " + patternsArray[k].dimCount());
            }
            if (allCompatibleRectangular // important to check it before typecast
                    && !(patternsArray[k] instanceof RectangularPattern
                    && ((UniformGridPattern) patternsArray[k]).stepsOfGridEqual((UniformGridPattern) first))) {
                allCompatibleRectangular = false;
            }
        }
        if (allCompatibleRectangular) {
            UniformGridPattern ugFirst = (UniformGridPattern) first;
            Pattern result = new BasicRectangularPattern(ugFirst.originOfGrid(), ugFirst.stepsOfGrid(),
                    ugFirst.gridIndexArea().ranges());
            // the actualization is necessary to be sure in the implementation of minkowskiAdd method
            for (int k = 1; k < patternsArray.length; k++) {
                result = result.minkowskiAdd(patternsArray[k]);
                if (!(result instanceof RectangularPattern)) {
                    throw new AssertionError("Invalid SimpleRectangularGridPattern.minkowskiAdd implementation");
                }
            }
            if (result.pointCount() == 1) {
                return new OnePointPattern(result.coordMin());
            } else {
                return result;
            }
        } else {
            return new MinkowskiSum(patternsArray);
        }
    }

    /**
     * Equivalent to {@link #newMinkowskiSum(Pattern...) newMinkowskiSum(patterns)}, where <code>pattern</code>
     * is the array with the length <code>n</code> containing <code>n</code> copies of the passed pattern.
     * Some algorithms of this package work better with such form of the Minkowski sums.
     *
     * @param pattern the pattern.
     * @param n       the multiplier.
     * @return the Minkowski multiple of the passed pattern.
     * @throws NullPointerException     if <code>pattern</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>n &lt;= 0</code>.
     */
    public static Pattern newMinkowskiMultiplePattern(Pattern pattern, int n) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (n <= 0) {
            throw new IllegalArgumentException("Negative or zero n argument");
        }
        Pattern[] patterns = new Pattern[n];
        java.util.Arrays.fill(patterns, pattern);
        return new MinkowskiSum(patterns);
    }

    /**
     * Creates new pattern, representing the set-theoretic union of the passed patterns.
     * This method does not calculate the point set of the union;
     * if necessary, you will be able to get all points later by {@link Pattern#roundedPoints()} method.
     *
     * <p>In the returned pattern,
     * the {@link Pattern#unionDecomposition(int)} method returns
     * the summary list of union decompositions of all patterns passed to this method,
     * and the {@link Pattern#allUnionDecompositions(int)} method returns the same list
     * as the only element of its result.
     * The {@link Pattern#minkowskiDecomposition(int)} method is non-overridden
     * {@link AbstractPattern#minkowskiDecomposition(int)} method.
     * The {@link UniformGridPattern#isActuallyRectangular()} method is non-overridden
     * {@link AbstractUniformGridPattern#isActuallyRectangular()} method.
     * The {@link Pattern#minkowskiAdd(Pattern)} method returns a new <i>simple</i> pattern
     * according the definition of Minkowski sum; this method requires <i>O</i>(<i>NM</i>) operations,
     * where <i>N</i> and <i>M</i> is the number of points in both patterns, and may work slowly.
     * The {@link Pattern#minkowskiDecomposition(int)} method returns the list containing
     * this pattern instance as the only element.
     * The <code>equals</code> method returns <code>true</code> if and only if the passed object is also union
     * of patterns, consisting of the same summands and created by this or equivalent method from this package.
     *
     * <p>The returned pattern will be "safe" in the sense that no references to the passed array are maintained by it.
     * In other words, this method always allocates new array for storing summands and copies
     * the passed summands into it.
     *
     * @param patterns the summands of the returned union.
     * @return the set-theoretic union.
     * @throws NullPointerException     if <code>patterns</code> argument is {@code null}
     *                                  or some of passed patterns are {@code null}.
     * @throws IllegalArgumentException if <code>patterns</code> argument is an empty array,
     *                                  or if some patterns have different number of dimensions,
     *                                  or if some of the {@link Pattern#roundedCoordRange(int) coordinate ranges} in the
     *                                  precise result have the size (maximum - minimum), greater or equal to
     *                                  <code>Long.MAX_VALUE</code>.
     */
    public static Pattern newUnion(Pattern... patterns) {
        Objects.requireNonNull(patterns, "Null patterns argument");
        return new Union(patterns.clone());
        // cloning guarantees correct behavior while multithreading
    }

    public static Pattern newUnion(Collection<Pattern> patterns) {
        Objects.requireNonNull(patterns, "Null patterns argument");
        return new Union(patterns.toArray(new Pattern[0]));
        // cloning guarantees correct behavior while multithreading
    }

    /**
     * Returns <code>true</code> if and only if the absolute value of the precise mathematical difference
     * |<code>x1</code>&minus;<code>x2</code>| is not greater than {@link Pattern#MAX_COORDINATE}:
     * |<code>x1</code>&minus;<code>x2</code>|&le;{@link Pattern#MAX_COORDINATE}.
     *
     * <p>Note: this condition is checked <i>absolutely strictly with ideal precision</i>.
     * If this difference is near to {@link Pattern#MAX_COORDINATE} value,
     * this method uses <code>BigDecimal</code> class to provide absolutely correct result.
     *
     * <p>Special cases: if any of two arguments contains <code>NaN</code> or an infinity,
     * this method returns <code>false</code>.
     *
     * @param x1 the first number.
     * @param x2 the second number.
     * @return if the mathematically precise difference between these numbers is &le;{@link Pattern#MAX_COORDINATE}.
     */
    public static boolean isAllowedDifference(double x1, double x2) {
        if (Double.isNaN(x1) || Double.isNaN(x2) || Double.isInfinite(x1) || Double.isInfinite(x2)) {
            return false;
        }
        @SuppressWarnings("ManualMinMaxCalculation") double a = x1 <= x2 ? x1 : x2;
        @SuppressWarnings("ManualMinMaxCalculation") double b = x1 <= x2 ? x2 : x1;
        double diff = b - a;
        // - according to IEEE 754, it is the nearest double value to the mathematical difference b-a
        if (diff < Pattern.MAX_COORDINATE - 1) { // -1 and +1 to be on the safe side
            return true;
        }
        if (diff > Pattern.MAX_COORDINATE + 1) { // in particular, if x1 or x2 is infinite
            return false;
        }
        BigDecimal bigA = new BigDecimal(a);
        BigDecimal bigB = new BigDecimal(b);
        BigDecimal bigDiff = bigB.subtract(bigA);
        return bigDiff.compareTo(BIG_DECIMAL_MAX_COORDINATE) <= 0;
    }

    private static void addPointsToSphere(
            Set<IPoint> points, double[] center, long[] coordinates,
            int lastCoordinatesCount, int ir, double rSqr, double rSum) {
        int dimCount = coordinates.length;
        int currentCoordIndex = dimCount - 1 - lastCoordinatesCount;
        int min = (int) center[currentCoordIndex] - ir;
        int max = (int) center[currentCoordIndex] + ir;
        if (currentCoordIndex == 0) {
            for (int i = min; i <= max; i++) {
                double diff = i - center[currentCoordIndex];
                if (rSum + diff * diff <= rSqr) {
                    coordinates[0] = i;
                    points.add(IPoint.valueOf(coordinates));
                }
            }
        } else {
            for (int i = min; i <= max; i++) {
                coordinates[currentCoordIndex] = i;
                double diff = i - center[currentCoordIndex];
                addPointsToSphere(points, center, coordinates, lastCoordinatesCount + 1,
                        ir, rSqr, rSum + diff * diff);
            }
        }
    }

    private static void addPointsToEllipsoid(
            Set<IPoint> points, double[] center, int[] semiAxesUpperBounds, double[] semiAxesInv,
            long[] coordinates, int lastCoordinatesCount, double sum) {
        int dimCount = coordinates.length;
        int currentCoordIndex = dimCount - 1 - lastCoordinatesCount;
        double rInv = semiAxesInv[currentCoordIndex];
        int min = (int) center[currentCoordIndex] - semiAxesUpperBounds[currentCoordIndex];
        int max = (int) center[currentCoordIndex] + semiAxesUpperBounds[currentCoordIndex];
        if (currentCoordIndex == 0) {
            for (int i = min; i <= max; i++) {
                double diff = i - center[currentCoordIndex];
                double ratio = diff == 0.0 ? 0.0 : diff * rInv; // rInv is infinite for zero semi-axis
                double newSum = sum + ratio * ratio; // x^2/a^2 + y^2/b^2 + ... : it must be <=1.0
                if (newSum <= 1.000000001) { // theoretically possible that 5 * 1.0/5.0 > 1.0
                    coordinates[0] = i;
                    points.add(IPoint.valueOf(coordinates));
                }
            }
        } else {
            for (int i = min; i <= max; i++) {
                double diff = i - center[currentCoordIndex];
                double ratio = diff == 0.0 ? 0.0 : diff * rInv; // rInv is infinite for zero semi-axis
                double newSum = sum + ratio * ratio;
                if (newSum <= 1.000000001) { // little optimization: avoiding extra loop for some coordinates
                    coordinates[currentCoordIndex] = i;
                    addPointsToEllipsoid(points, center, semiAxesUpperBounds, semiAxesInv,
                            coordinates, lastCoordinatesCount + 1, newSum);
                }
            }
        }
    }
}
