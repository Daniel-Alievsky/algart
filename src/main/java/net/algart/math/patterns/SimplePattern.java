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

package net.algart.math.patterns;

import net.algart.math.Point;
import net.algart.math.Range;

import java.util.*;

/**
 * <p>The simplest implementation of the {@link Pattern} interface, based on a set (<tt>java.util.Set</tt>
 * or some equivalent form), containing all pattern points.</p>
 *
 * <p>If a pattern is an instance of this class, then there are all common guarantees for
 * {@link DirectPointSetPattern} (see the documentation for that interface) and, in addition,
 * you can be sure that {@link #points()} method works very quickly and does not spend memory:
 * it just returns <tt>Collections.unmodifiableSet</tt> for the set of points,
 * stored in an internal field.</p>
 *
 * <p>This class does not support any decompositions:
 * {@link #hasMinkowskiDecomposition()} method returns <tt>false</tt>,
 * {@link #minkowskiDecomposition(int)}, {@link #unionDecomposition(int)} and
 * {@link #allUnionDecompositions(int)} methods return trivial decompositions consisting of
 * the only this pattern.</p>
 *
 * <p>The methods of pattern transformation &mdash; {@link #shift(Point)}, {@link #symmetric()},
 * {@link #multiply(double)}, {@link #scale(double...)}, {@link #projectionAlongAxis(int)},
 * {@link #minBound(int)}, {@link #maxBound(int)} &mdash; always return instances of this class.</p>
 *
 * <p>All calculations in this class are performed in <tt>strictfp</tt> mode, so the result
 * is absolutely identical on all platforms.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public strictfp class SimplePattern extends AbstractPattern implements DirectPointSetPattern {
    private final Set<Point> points;

    /**
     * Creates a pattern containing the given collection of points.
     *
     * <p>This passed collection is cloned by this constructor and converted into <tt>java.util.Set</tt>.
     * No references to the passed argument are maintained by the created instance.
     *
     * @param points collection of all points of the pattern.
     * @throws NullPointerException     if the argument is <tt>null</tt> or if some element in the passed
     *                                  collection is <tt>null</tt>
     * @throws IllegalArgumentException if <tt>points</tt> argument is an empty collection,
     *                                  or if some points have different number of coordinates.
     * @see Patterns#newPattern(java.util.Collection)
     */
    public SimplePattern(Collection<Point> points) {
        super(getDimCountAndCheck(points));
        HashSet<Point> pointSet = new HashSet<Point>(points);
        if (getDimCountAndCheck(pointSet) != dimCount)
            throw new IllegalArgumentException("Points dimensions were changed in a parallel thread");
        this.points = pointSet;
        fillCoordRangesWithCheck(this.points);
    }

    @Override
    public final long pointCount() {
        return points.size();
    }

    @Override
    public final Set<Point> points() {
        return Collections.unmodifiableSet(points);
    }

    @Override
    public Range coordRange(int coordIndex) {
        return coordRanges[coordIndex];
    }

    @Override
    public final boolean isSurelySinglePoint() {
        return points.size() == 1;
    }

    /**
     * This method is implemented here by shifting all points in the point set, stored in this object and
     * returned by {@link #points()} method, by the call <tt>p.{@link Point#add(Point) add}(shift)</tt>.
     *
     * @param shift the shift.
     * @return the shifted pattern.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link Point#coordCount() coordCount()}!={@link #dimCount()}</tt>.
     * @throws TooLargePatternCoordinatesException
     *                                  if the set of shifted points does not fulfil the restrictions,
     *                                  described in the {@link Pattern comments to this interface},
     *                                  section "Coordinate restrictions".
     */
    @Override
    public SimplePattern shift(Point shift) {
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        Set<Point> resultPoints = new HashSet<Point>(points.size());
        for (Point p : points) {
            resultPoints.add(p.add(shift));
        }
        return new SimplePattern(resultPoints);
    }

    @Override
    public SimplePattern symmetric() {
        return (SimplePattern) super.symmetric();
    }

    @Override
    public SimplePattern multiply(double multiplier) {
        return (SimplePattern) super.multiply(multiplier);
    }

    /**
     * This method is implemented here by scaling all points in the point set, stored in this object and
     * returned by {@link #points()} method, by the call <tt>p.{@link Point#scale(double...) scale}(multipliers)</tt>.
     *
     * @param multipliers the multipliers for all coordinates.
     * @return the scaled pattern.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>multipliers.length!={@link #dimCount() dimCount()}</tt>.
     * @throws TooLargePatternCoordinatesException
     *                                  if the set of scaled points does not fulfil the restrictions,
     *                                  described in the {@link Pattern comments to this interface},
     *                                  section "Coordinate restrictions".
     */
    @Override
    public SimplePattern scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != dimCount)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount);
        multipliers = multipliers.clone(); // to be on the safe side
        Set<Point> resultPoints = new HashSet<Point>(points.size());
        for (Point p : points) {
            resultPoints.add(p.scale(multipliers));
        }
        return new SimplePattern(resultPoints);
    }

    @Override
    public SimplePattern projectionAlongAxis(int coordIndex) {
        checkCoordIndex(coordIndex);
        assert dimCount > 0;
        if (dimCount == 1)
            throw new IllegalStateException("Cannot perform projection for 1-dimensional pattern");
        Set<Point> resultPoints = new HashSet<Point>(points.size());
        for (Point p : points) {
            resultPoints.add(p.projectionAlongAxis(coordIndex));
        }
        return new SimplePattern(resultPoints);
    }

    /*Repeat() min(?!us) ==> max */

    /**
     * This implementation is similar to the implementation from {@link AbstractPattern} class,
     * but (unlike that) this implementation always returns an instance of this class.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the minimal boundary of this pattern for the given axis.
     * @throws IndexOutOfBoundsException   if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #dimCount()}</tt>.
     */
    @Override
    public SimplePattern minBound(int coordIndex) {
        return (SimplePattern) minBound(coordIndex, true);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * This implementation is similar to the implementation from {@link AbstractPattern} class,
     * but (unlike that) this implementation always returns an instance of this class.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the maximal boundary of this pattern for the given axis.
     * @throws IndexOutOfBoundsException   if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #dimCount()}</tt>.
     */
    @Override
    public SimplePattern maxBound(int coordIndex) {
        return (SimplePattern) maxBound(coordIndex, true);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * the minimum and maximum numbers in this range.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(dimCount + "D multipoint common pattern containing "
            + points.size() + " points");
        if (pointCount() <= 32) {
            sb.append(" ").append(points());
        }
        if (pointCount() <= 1024) {
            sb.append(" inside ").append(coordArea());
        }
        return sb.toString();
    }

    /**
     * Returns the hash code of this pattern. The result depends on
     * all {@link #points() points}.
     *
     * @return the hash code of this pattern.
     */
    @Override
    public final int hashCode() {
        return points().hashCode();
    }

    /**
     * Indicates whether some other pattern is equal to this instance,
     * that is the set of its points is the same in terms of <tt>java.util.Set.equals</tt> method.
     * See more precise specification in comments about <tt>equals()</tt>
     * in {@link Pattern comments to Pattern interface}.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return <tt>true</tt> if the specified object is a pattern equal to this one.
     */
    @Override
    public final boolean equals(Object obj) {
        return obj instanceof Pattern && simplePatternsEqual(this, (Pattern) obj);
    }

    static int getDimCountAndCheck(Collection<Point> points) {
        if (points == null)
            throw new NullPointerException("Null points argument");
        int n = points.size();
        if (n == 0)
            throw new IllegalArgumentException("Empty points set");
        Iterator<Point> iterator = points.iterator();
        Point p = iterator.next();
        if (p == null)
            throw new NullPointerException("Null point is the collection");
        int result = p.coordCount();
        for (; iterator.hasNext(); ) {
            p = iterator.next();
            if (p == null)
                throw new NullPointerException("Null point is the collection");
            if (p.coordCount() != result)
                throw new IllegalArgumentException("Points dimensions mismatch: the first point has "
                    + result + " coordinates, but some of points has " + p.coordCount());
            checkPoint(p);
        }
        return result;
    }

    static boolean simplePatternsEqual(Pattern first, Pattern second) {
        return second == first
            || (second instanceof SimplePattern || second instanceof BasicDirectPointSetUniformGridPattern
            || second instanceof TwoPointsPattern || second instanceof OnePointPattern)
            && second.points().equals(first.points());
    }
}
