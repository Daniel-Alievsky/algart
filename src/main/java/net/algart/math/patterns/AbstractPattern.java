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

import java.util.*;

import net.algart.math.*;

/**
 * <p>A skeletal implementation of the {@link Pattern} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>All non-abstract methods are completely implemented here and may be not overridden in subclasses.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractPattern implements Pattern {

    volatile Boolean surelyOrigin = null;
    volatile Boolean surelyInteger = null;
    final Range[] coordRanges; // null-filled by the constructor
    final Pattern[] minBound; // null-filled by the constructor
    final Pattern[] maxBound; // null-filled by the constructor

    /**
     * The number of space dimensions of this pattern.
     */
    protected final int dimCount;

    /**
     * Creates a pattern with the given number of space dimensions.
     *
     * @param dimCount the number of space dimensions.
     * @throws IllegalArgumentException if <code>dimCount&lt;=0</code>.
     */
    protected AbstractPattern(int dimCount) {
        if (dimCount <= 0) {
            throw new IllegalArgumentException("Negative or zero dimCount=" + dimCount);
        }
        this.dimCount = dimCount;
        this.coordRanges = new Range[dimCount]; // null-filled
        this.minBound = new Pattern[dimCount]; // null-filled
        this.maxBound = new Pattern[dimCount]; // null-filled
    }

    /**
     * This implementation returns {@link #dimCount} field.
     *
     * @return the number of space dimensions of this pattern.
     */
    public final int dimCount() {
        return this.dimCount;
    }

    public abstract long pointCount();

    /**
     * This implementation returns <code>(double){@link #pointCount()}</code>.
     * Please override this method if the pattern can contain more than <code>Long.MAX_VALUE</code> points.
     *
     * @return the number of {@link IPoint integer points} in this pattern.
     */
    public double largePointCount() {
        return (double) pointCount();
    }

    /**
     * This implementation returns <code>false</code>.
     * Please override this method if the pattern can contain more than <code>Long.MAX_VALUE</code> points.
     *
     * @return <code>true</code> if the number of points in this pattern is greater than <code>Long.MAX_VALUE</code>.
     */
    public boolean isPointCountVeryLarge() {
        return false;
    }

    public abstract Set<Point> points();

    /**
     * This implementation calls {@link #points()} method and returns a new set, built from the returned set of
     * real points by conversion of every point to an integer point via {@link Point#toRoundedPoint()} method,
     * as written in
     * {@link Pattern#roundedPoints() comments to this method in Pattern interface}.
     * Please override this method if there is more efficient way to get all rounded points.
     *
     * @return all points of this pattern, rounded to the nearest integer points.
     * @throws TooManyPointsInPatternError if {@link #points()} method throws this exception
     *                                     (<code>OutOfMemoryError</code> can be also thrown in this case).
     */
    public Set<IPoint> roundedPoints() {
        Set<IPoint> result = new HashSet<IPoint>();
        for (Point p : points()) {
            result.add(p.toRoundedPoint());
        }
        return Collections.unmodifiableSet(result);
    }

    public abstract Range coordRange(int coordIndex);

    /**
     * This implementation is based on the loop of calls of {@link #coordRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return the ranges from minimal to maximal grid index for all space dimensions.
     */
    public RectangularArea coordArea() {
        Range[] result = new Range[dimCount];
        for (int k = 0; k < result.length; k++) {
            result[k] = coordRange(k);
        }
        return RectangularArea.valueOf(result);
    }

    /**
     * This implementation is based on the loop of calls of {@link #coordRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return minimal coordinates for all space dimensions as a point.
     */
    public Point coordMin() {
        double[] coordinates = new double[dimCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = coordRange(k).min();
        }
        return Point.valueOf(coordinates);
    }

    /**
     * This implementation is based on the loop of calls of {@link #coordRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return maximal coordinates for all space dimensions as a point.
     */
    public Point coordMax() {
        double[] coordinates = new double[dimCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = coordRange(k).max();
        }
        return Point.valueOf(coordinates);
    }


    /**
     * This implementation returns <code>{@link #coordRange(int) coordRange}(coordIndex).{@link Range#toRoundedRange()
     * toRoundedRange()}</code>.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>, 1 for <i>y</i>, 2 for <i>z</i>, etc.).
     * @return the range from minimal to maximal coordinate with this index, rounded to the <code>long</code> values.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     * <code>coordIndex&gt;={@link #dimCount()}</code>.
     */
    public IRange roundedCoordRange(int coordIndex) {
        return coordRange(coordIndex).toRoundedRange();
    }

    /**
     * This implementation is based on the loop of calls of {@link #roundedCoordRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return the ranges from minimal to maximal coordinate for all space dimensions,
     *         rounded to the <code>long</code> values.
     */
    public IRectangularArea roundedCoordArea() {
        IRange[] result = new IRange[dimCount];
        for (int k = 0; k < result.length; k++) {
            result[k] = roundedCoordRange(k);
        }
        return IRectangularArea.valueOf(result);
    }

    public abstract boolean isSurelySinglePoint();

    /**
     * This implementation checks {@link #isSurelySinglePoint()}, and if it is <code>true</code>,
     * checks, whether the only element of {@link #points()} set is the origin.
     *
     * <p>This method caches its results: the following calls will work faster.
     *
     * @return <code>true</code> if it is one-point pattern containing the origin of coordinates as the single point.
     */
    public boolean isSurelyOriginPoint() {
        if (surelyOrigin == null) {
            surelyOrigin = isSurelySinglePoint() && points().iterator().next().isOrigin();
            // if isSurelySinglePoint(), then this method works quickly enough:
            // usually there are no problems to get the only one point
        }
        return surelyOrigin;
    }

    /**
     * This implementation calls {@link #points()} method and checks, whether all returned points are integer,
     * i&#46;e&#46; {@link Point#isInteger()} method returns <code>true</code> for all elements the returned set.
     * If all points, returned by {@link #points()} call, are integer, this method returns <code>true</code>,
     * in other case it returns <code>false</code>.
     *
     * <p>This method caches its results: the following calls will work faster.
     *
     * <p>Note: according the {@link Pattern#isSurelyInteger() comments to this method in Pattern interface},
     * such implementation is correct only if {@link #minkowskiDecomposition(int)},
     * {@link #unionDecomposition(int)} and {@link #allUnionDecompositions(int)} methods
     * have default implementations (not overridden). If some of them are overridden and
     * return some non-trivial results, this method <i>must</i> be also overridden.
     *
     * <p>Note: according the {@link Pattern#isSurelyInteger() comments to this method in Pattern interface},
     * this method <i>must</i> be overridden if the number of points can be very large and a call of
     * {@link #points()} method leads to a risk of {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>.
     * In particular, this method should be usually overridden in implementations of {@link RectangularPattern}.
     *
     * @return <code>true</code> if this pattern assuredly contain only {@link Point#isInteger() integer} points.
     */
    public boolean isSurelyInteger() {
        if (surelyInteger == null) {
            boolean allInteger = true;
            for (Point p : points()) {
                if (!p.isInteger()) {
                    allInteger = false;
                    break;
                }
            }
            surelyInteger = allInteger;
        }
        return surelyInteger;
    }

    /**
     * This implementation calls {@link #roundedPoints()} method and constructs a new integer pattern on the base
     * of this set, like as it is performed in {@link Patterns#newIntegerPattern(java.util.Collection)} method.
     * Please override this method if there is more efficient way to round this pattern,
     * for example, if this pattern is already an integer one.
     *
     * @return the integer pattern, geometrically nearest to this one.
     * @throws TooManyPointsInPatternError if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     */
    public UniformGridPattern round() {
        return new BasicDirectPointSetUniformGridPattern(dimCount, roundedPoints());
        // Here is a little optimization: we do not clone the result of roundedPoints() call.
        // It is correct for any possible correct implementation of roundedPoints(),
        // because this object is always immutable (it is a requirement of Pattern interface).
    }

    public abstract Pattern projectionAlongAxis(int coordIndex);

    /*Repeat() min(?!us) ==> max */

    /**
     * This implementation calls {@link #points()} method and builds the result on the base of analysis
     * of the returned point set.
     * Please override this method if there is more efficient way to find the result,
     * for example, if this pattern is a rectangular one.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the minimal boundary of this pattern for the given axis.
     * @throws IndexOutOfBoundsException   if <code>coordIndex&lt;0</code> or
     * <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @throws TooManyPointsInPatternError if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     */
    public Pattern minBound(int coordIndex) {
        return minBound(coordIndex, false);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * This implementation calls {@link #points()} method and builds the result on the base of analysis
     * of the returned point set.
     * Please override this method if there is more efficient way to find the result,
     * for example, if this pattern is a rectangular one.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the maximal boundary of this pattern for the given axis.
     * @throws IndexOutOfBoundsException   if <code>coordIndex&lt;0</code> or
     * <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @throws TooManyPointsInPatternError if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     */
    public Pattern maxBound(int coordIndex) {
        return maxBound(coordIndex, false);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * This implementation just returns this object.
     * Please override this method (together with {@link #maxCarcassMultiplier()}),
     * if your class can provide better results.
     *
     * <p>Note: {@link AbstractUniformGridPattern} class provides much better implementation.
     *
     * @return the <i>carcass</i> of this pattern.
     */
    public Pattern carcass() {
        return this;
    }

    /**
     * This implementation just returns 2.
     * Please override this method (together with {@link #carcass()}),
     * if your class can provide better results.
     *
     * <p>Note: {@link AbstractUniformGridPattern} class provides much better implementation.
     *
     * @return the maximal multiplier (&ge;2),
     *         for which the calculation of the Minkowski multiple can be optimized
     *         by using the {@link #carcass() carcass}.
     */
    public int maxCarcassMultiplier() {
        return 2;
    }

    public abstract Pattern shift(Point shift);

    /**
     * This implementation calls {@link #multiply(double) multiply(-1.0)}.
     * There are no reasons to override this method usually.
     *
     * @return the symmetric pattern.
     */
    public Pattern symmetric() {
        return multiply(-1.0);
    }

    /**
     * This implementation creates Java array <code>double[]</code> by the call
     * "<nobr><code>a = new double[{@link #dimCount}]</code></nobr>", fills all its elements by
     * <code>multiplier</code> argument and then calls {@link #scale(double...) scale(a)}.
     * There are no reasons to override this method usually.
     *
     * @param multiplier the scale along all coordinates.
     * @return the scaled pattern.
     * @throws TooLargePatternCoordinatesException
     *          if the set of scaled points does not fulfil the restrictions,
     *          described in the comments to {@link Pattern} interface,
     *          section "Coordinate restrictions".
     */
    public Pattern multiply(double multiplier) {
        double[] multipliers = new double[dimCount];
        Arrays.fill(multipliers, multiplier);
        return scale(multipliers);
    }

    public abstract Pattern scale(double... multipliers);

    /**
     * This implementation is based on the loop for all points returned by {@link #points()} method in both patterns
     * and always returns a {@link DirectPointSetPattern direct point-set pattern},
     * consisting of sums of all point pairs.
     * This algorithm may be very slow for large patterns
     * (<i>O</i>(<i>NM</i>) operations, <i>N</i>={@link #pointCount()}, <i>M</i>=added.{@link #pointCount()})
     * and does not work at all if the number of resulting points is greater than <code>Integer.MAX_VALUE</code>.
     * Please override this method if there is better implementation.
     *
     * @param added another pattern.
     * @return the Minkowski sum of this and another patterns.
     * @throws NullPointerException        if the argument is {@code null}.
     * @throws IllegalArgumentException    if the numbers of space dimensions of both patterns are different.
     * @throws TooManyPointsInPatternError for some forms of large patterns, if the number of points in this,
     *                                     <code>added</code> or result pattern is greater than
     *                                     <code>Integer.MAX_VALUE</code> or, maybe, is near this limit
     * @throws TooLargePatternCoordinatesException
     *                                     if the resulting set of points does not fulfil the restrictions,
     *                                     described in the comments to {@link Pattern} interface,
     *                                     section "Coordinate restrictions".
     */
    public Pattern minkowskiAdd(Pattern added) {
        Objects.requireNonNull(added, "Null added argument");
        if (added.dimCount() != this.dimCount) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + added.dimCount() + " instead of " + this.dimCount);
        }
        long addedPointCount = added.pointCount();
        if (addedPointCount == 1) {
            return shift(added.coordMin());
        }

        Set<Point> resultPoints = new HashSet<Point>();
        Set<Point> points = points();
        Set<Point> addedPoints = added.points();
        for (Point p : points) {
            for (Point q : addedPoints) {
                resultPoints.add(p.add(q));
            }
        }
        return new SimplePattern(resultPoints);
    }

    /**
     * This implementation is based on the loop for all points returned by {@link #points()} method in both patterns
     * and always returns a {@link DirectPointSetPattern direct point-set pattern}.
     * This algorithm may be very slow for large patterns
     * (<i>O</i>(<i>NM</i>) operations, <i>N</i>={@link #pointCount()}, <i>M</i>=added.{@link #pointCount()})
     * and does not work at all if the number of resulting points is greater than <code>Integer.MAX_VALUE</code>.
     * Please override this method if there is better implementation.
     *
     * @param subtracted another pattern.
     * @return the erosion of this pattern by the specified pattern
     *         or {@code null} if this erosion is the empty set.
     * @throws NullPointerException        if the argument is {@code null}.
     * @throws IllegalArgumentException    if the numbers of space dimensions of both patterns are different.
     * @throws TooManyPointsInPatternError for some forms of large patterns, if the number of points in this,
     *                                     <code>subtracted</code> or result pattern is greater than
     *                                     <code>Integer.MAX_VALUE</code> or, maybe, is near this limit
     * @throws TooLargePatternCoordinatesException
     *                                     if the resulting set of points does not fulfil the restrictions,
     *                                     described in the comments to {@link Pattern} interface,
     *                                     section "Coordinate restrictions".
     */
    public Pattern minkowskiSubtract(Pattern subtracted) {
        Objects.requireNonNull(subtracted, "Null subtracted argument");
        if (subtracted.dimCount() != this.dimCount) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + subtracted.dimCount() + " instead of " + this.dimCount);
        }
        Set<Point> subtractedPoints = subtracted.points();
        Point minimal = null;
        double minimalDistance = Double.POSITIVE_INFINITY;
        for (Point p : subtractedPoints) {
            double distance = p.distanceFromOrigin();
            if (minimal == null || distance < minimalDistance) {
                minimal = p;
                minimalDistance = distance;
            }
        }
        assert minimal != null : "Empty subtracted.points()";
        boolean containsOrigin = minimal.isOrigin();
        Set<Point> points = points();
        Set<Point> resultPoints = new HashSet<Point>();
        mainLoop:
        for (Point p : points) {
            // Formally, we need here a loop for infinity number of all points p in the space;
            // but we can use the simple fact that the result is a subset of thisPattern.shift(-minimal)
            if (!containsOrigin) {
                p = p.subtract(minimal);
            }
            for (Point q : subtractedPoints) {
                if (q.equals(minimal)) {
                    continue; // no sense to check
                }
                if (!points.contains(p.add(q))) {
                    continue mainLoop; // don't add p
                }
            }
            resultPoints.add(p);
        }
        return resultPoints.isEmpty() ? null : Patterns.newPattern(resultPoints);
    }

    /**
     * This implementation just returns <code>Collections.&lt;Pattern&gt;singletonList(thisInstance)</code>.
     *
     * <p>Note: {@link AbstractUniformGridPattern} class provides much better implementation for
     * patterns, recognized as rectangular by {@link UniformGridPattern#isActuallyRectangular()} method.
     *
     * @param minimalPointCount this method usually does not decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     * @return the decomposition of this pattern to Minkowski sum; always contains &ge;1 elements.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<Pattern> minkowskiDecomposition(int minimalPointCount) {
        if (minimalPointCount < 0) {
            throw new IllegalArgumentException("Negative minimalPointCount");
        }
        return Collections.<Pattern>singletonList(this);
    }

    /**
     * This implementation just returns <code>false</code>.
     *
     * @return <code>true</code> if the Minkowski decomposition contains 2 or more elements;
     *         always <code>false</code> in this implementation.
     */
    public boolean hasMinkowskiDecomposition() {
        return false;
    }

    /**
     * This implementation returns <code>{@link #allUnionDecompositions(int)
     * allUnionDecompositions(minimalPointCount)}.get(0)</code>.
     *
     * @param minimalPointCount this method usually does not decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     * @return a decomposition of this pattern into the union of patterns; always contains &ge;1 elements.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<Pattern> unionDecomposition(int minimalPointCount) {
        return allUnionDecompositions(minimalPointCount).get(0);
    }

    /**
     * This implementation just returns the list containing 1 list, containing
     * this instance as the only element:
     * <code>Collections.singletonList(Collections.&lt;Pattern&gt;singletonList(thisInstance))</code>.
     *
     * <p>Note: {@link AbstractUniformGridPattern} class provides much better implementation.
     *
     * @param minimalPointCount this method usually does not decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     * @return several good variants of decomposition of this pattern to the union of patterns;
     *         the result always contains &ge;1 elements,
     *         and all its elements also contain &ge;1 elements.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<List<Pattern>> allUnionDecompositions(int minimalPointCount) {
        if (minimalPointCount < 0) {
            throw new IllegalArgumentException("Negative minimalPointCount");
        }
        return Collections.singletonList(Collections.<Pattern>singletonList(this));
    }

    /**
     * Returns <code>true</code> if and only if all coordinates of the specified point lie
     * in range &minus;{@link #MAX_COORDINATE}&le;<i>x</i><sub><i>j</i></sub>&le;{@link #MAX_COORDINATE}.
     *
     * <p>Actually this method checks the 1st restriction for coordinates of any pattern:
     * see comments to {@link Pattern} interface, section "Coordinate restrictions".
     *
     * @param point some point.
     * @return whether this point is an allowed point for patterns.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public static boolean isAllowedPoint(Point point) {
        Objects.requireNonNull(point, "Null point");
        for (int k = 0, n = point.coordCount(); k < n; k++) {
            double coord = point.coord(k);
            if (coord < -Pattern.MAX_COORDINATE || coord > Pattern.MAX_COORDINATE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>true</code> if and only if both boundaries of the specified range,
     * <i>a</i>=<code>range.{@link Range#min() min()}</code> and <i>b</i>=<code>range.{@link Range#max() max()}</code>,
     * lie in range
     * &minus;{@link #MAX_COORDINATE}&le;<i>a</i>&le;<i>b</i>&le;{@link #MAX_COORDINATE}
     * and, at the same time, the call <code>{@link Patterns#isAllowedDifference(double, double)
     * Patterns.isAllowedDifference}(<i>a</i>,<i>b</i>)</code> returns <code>true</code>.
     *
     * <p>This method helps to check the 2nd restriction for coordinates of any pattern:
     * see comments to {@link Pattern} interface, section "Coordinate restrictions".
     *
     * @param range some range.
     * @return whether this range is an allowed coordinate range for patterns.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public static boolean isAllowedCoordRange(Range range) {
        Objects.requireNonNull(range, "Null range");
        double min = range.min();
        double max = range.max();
        assert min <= max;
        return min >= -MAX_COORDINATE && max <= MAX_COORDINATE && Patterns.isAllowedDifference(min, max);
    }

    /**
     * Throws <code>IndexOutOfBoundsException</code>
     * if <code>coordIndex&lt;0</code> or <code>coordIndex&gt;={@link #dimCount()}</code>.
     * Does nothing in other case.
     *
     * @param coordIndex checked index of the coordinate.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     * <code>coordIndex&gt;={@link #dimCount()}</code>.
     */
    protected final void checkCoordIndex(int coordIndex) {
        if (coordIndex < 0 || coordIndex >= dimCount) {
            throw new IndexOutOfBoundsException("Coordinate index "
                + coordIndex + " is out of range 0.." + (dimCount - 1));
        }
    }

    static void checkPoint(Point point) throws TooLargePatternCoordinatesException {
        if (!isAllowedPoint(point)) {
            throw new TooLargePatternCoordinatesException("Point " + point + " has one of coordinates "
                + " out of -" + MAX_COORDINATE + ".." + MAX_COORDINATE
                + " range and cannot be used for building a pattern");
        }
    }

    static void checkCoordRange(Range range) throws TooLargePatternCoordinatesException {
        Objects.requireNonNull(range, "Null range");
        double min = range.min();
        double max = range.max();
        assert min <= max;
        if (min < -MAX_COORDINATE || max > MAX_COORDINATE) {
            throw new TooLargePatternCoordinatesException("Coordinate range " + range
                + " is out of -" + MAX_COORDINATE + ".." + MAX_COORDINATE
                + " range and cannot be used for building a pattern");
        }
        if (!Patterns.isAllowedDifference(min, max)) {
            throw new TooLargePatternCoordinatesException("Coordinate range " + range
                + " has a size larger than " + MAX_COORDINATE + " and cannot be used for building a pattern");
        }
    }

    final void fillCoordRangesWithCheck(Collection<Point> points) {
        double[] minCoord = new double[dimCount];
        double[] maxCoord = new double[dimCount];
        Arrays.fill(minCoord, Double.POSITIVE_INFINITY);
        Arrays.fill(maxCoord, Double.NEGATIVE_INFINITY);
        for (Point p : points) {
            checkPoint(p);
            for (int k = 0; k < dimCount; k++) {
                double coordinate = p.coord(k);
                if (coordinate < minCoord[k]) {
                    minCoord[k] = coordinate;
                }
                if (coordinate > maxCoord[k]) {
                    maxCoord[k] = coordinate;
                }
            }
        }
        for (int k = 0; k < dimCount; k++) {
            this.coordRanges[k] = Range.valueOf(minCoord[k], maxCoord[k]);
            checkCoordRange(this.coordRanges[k]);
        }
    }

    final Pattern minBound(int coordIndex, boolean alwaysSimple) {
        checkCoordIndex(coordIndex);
        synchronized (minBound) {
            if (minBound[coordIndex] == null) {
                Set<Point> points = points();
                Map<Point, Point> map = new HashMap<Point, Point>();
                for (Point p : points) {
                    Point projection = p.projectionAlongAxis(coordIndex);
                    Point bound = map.get(projection);
                    if (bound == null || p.coord(coordIndex) < bound.coord(coordIndex)) {
                        map.put(projection, p);
                    }
                }
                minBound[coordIndex] = alwaysSimple ?
                    new SimplePattern(map.values()) :
                    Patterns.newPattern(map.values());
            }
            return minBound[coordIndex];
        }
    }

    final Pattern maxBound(int coordIndex, boolean alwaysSimple) {
        checkCoordIndex(coordIndex);
        synchronized (maxBound) {
            if (maxBound[coordIndex] == null) {
                Set<Point> points = points();
                Map<Point, Point> map = new HashMap<Point, Point>();
                for (Point p : points) {
                    Point projection = p.projectionAlongAxis(coordIndex);
                    Point bound = map.get(projection);
                    if (bound == null || p.coord(coordIndex) > bound.coord(coordIndex)) {
                        map.put(projection, p);
                    }
                }
                maxBound[coordIndex] = alwaysSimple ?
                    new SimplePattern(map.values()) :
                    Patterns.newPattern(map.values());
            }
            return maxBound[coordIndex];
        }
    }

}
