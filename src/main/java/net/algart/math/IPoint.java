/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math;

import java.util.Arrays;
import java.util.Collection;
import java.util.zip.*;

/**
 * <p>Point in multidimensional space with integer coordinates.
 * Represented as an array of <tt>long</tt> numbers.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 * @see Point
 */
public class IPoint implements Comparable<IPoint> {
    /*Repeat.SectionStart begin*/
    private static final IPoint originsCache[] = new IPoint[16];
    static {
        for (int i = 1; i <= originsCache.length; i++)
            originsCache[i - 1] = new IPoint(new long[i]) { // zero-filled by Java
                @Override
                public boolean isOrigin() {
                    return true;
                }
            };
    }

    final long[] coordinates;

    IPoint(long[] coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Returns a new point with the given set of coordinates: <i>x</i>, <i>y</i>, <i>z</i>, ...
     * For example, <tt>valueOf(x,y)</tt> returns the 2D point with given coordinates.
     *
     * <p>The <tt>coordinates</tt> array must contain at least 1 element.
     *
     * <p>The passed <tt>coordinates</tt> array is cloned by this method: no references to it
     * are maintained by the created object.
     *
     * @param coordinates cartesian coordinates of the point.
     * @return            the point with the given coordinates.
     * @throws NullPointerException     if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     */
    public static IPoint valueOf(long ...coordinates) {
        if (coordinates == null)
            throw new NullPointerException("Null coordinates argument");
        if (coordinates.length == 0)
            throw new IllegalArgumentException("Empty coordinates array");
        if (coordinates.length <= originsCache.length) {
            boolean origin = true;
            for (long coord : coordinates) {
                if (coord != 0) {
                    origin = false;
                }
            }
            if (origin)
                return originsCache[coordinates.length - 1];
        }
        return new IPoint(coordinates.clone());
    }

    /**
     * Returns a new 1-dimensional point with the given coordinate.
     *
     * @param x cartesian coordinate of the point.
     * @return point with the given coordinates.
     */
    public static IPoint valueOf(long x) {
        return x == 0 ? originsCache[0] : new IPoint(new long[]{x});
    }

    /**
     * Returns a new 2-dimensional point with the given coordinates.
     *
     * @param x cartesian x-coordinate of the point.
     * @param y cartesian y-coordinate of the point.
     * @return point with the given coordinates.
     */
    public static IPoint valueOf(long x, long y) {
        return x == 0 && y == 0 ? originsCache[1] : new IPoint(new long[] {x, y});
    }

    /**
     * Returns a new 3-dimensional point with the given coordinates.
     *
     * @param x cartesian x-coordinate of the point.
     * @param y cartesian y-coordinate of the point.
     * @param z cartesian z-coordinate of the point.
     * @return point with the given coordinates.
     */
    public static IPoint valueOf(long x, long y, long z) {
        return x == 0 && y == 0 && z == 0 ? originsCache[2] : new IPoint(new long[] {x, y, z});
    }

    /**
     * Returns a new point in <i>n</i>-dimensional space, where <i>n</i><tt>=coordCount</tt>
     * and all coordinates of the point are equal to the given value <tt>filler</tt>.
     * For example, <nobr><tt>valueOfEqualCoordinates(3, 1)</tt></nobr> returns the 3D point <nobr>(1,1,1)</nobr>.
     * If <tt>filler==0</tt>, this method is equivalent to {@link #origin(int) origin(coordCount)}.
     *
     * @param coordCount the number of dimensions.
     * @param filler     the value of each coordinate of the created point.
     * @return           the point with equal coordinates.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static IPoint valueOfEqualCoordinates(int coordCount, long filler) {
        if (filler == 0) {
            return origin(coordCount);
        }
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        long[] coordinates = new long[coordCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = filler;
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the origin of coordinates in <i>n</i>-dimensional space,
     * where <i>n</i><tt>=coordCount</tt> is the argument of this method.
     *
     * @param coordCount the number of dimensions.
     * @return           the origin of coordinates in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static IPoint origin(int coordCount) {
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        if (coordCount <= originsCache.length) {
            return originsCache[coordCount - 1];
        } else {
            return new IPoint(new long[coordCount]); // zero-filled by Java
        }
    }

    /**
     * Returns the "minimal" point in <i>n</i>-dimensional space,
     * where <i>n</i><tt>=coordCount</tt> is the argument of this method,
     * that is the point with all coordinates are equal to <tt>Long.MIN_VALUE</tt>.
     *
     * @param coordCount the number of dimensions.
     * @return           the "minimal" point in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static IPoint minValue(int coordCount) {
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        long[] coordinates = new long[coordCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Long.MIN_VALUE;
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the "maximal" point in <i>n</i>-dimensional space,
     * where <i>n</i><tt>=coordCount</tt> is the argument of this method,
     * that is the point with all coordinates are equal to <tt>Long.MAX_VALUE</tt>.
     *
     * @param coordCount the number of dimensions.
     * @return           the "maximal" point in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static IPoint maxValue(int coordCount) {
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        long[] coordinates = new long[coordCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Long.MAX_VALUE;
        }
        return new IPoint(coordinates);
    }
    /*Repeat.SectionEnd begin*/

    /**
     * Returns a new point with the same coordinates as the given real point.
     * All <tt>double</tt> coordinates of the passed real point are converted
     * to <tt>long</tt> coordinates of the returned point by standard
     * Java typecast <tt>(long)doubleValue</tt>.
     *
     * @param point the real point.
     * @return      the integer point with same (cast) coordinates.
     * @throws NullPointerException if the passed point is <tt>null</tt>.
     */
    public static IPoint valueOf(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        long[] coordinates = new long[point.coordCount()];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = (long)point.coord(k);
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns a new point with the same coordinates as the given real point.
     * All <tt>double</tt> coordinates of the passed real point are converted
     * to <tt>long</tt> coordinates of the returned point by
     * <tt>StrictMath.round</tt> method.
     *
     * @param point the real point.
     * @return      the integer point with same (rounded) coordinates.
     * @throws NullPointerException if the passed point is <tt>null</tt>.
     */
    public static IPoint roundOf(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        long[] coordinates = new long[point.coordCount()];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = StrictMath.round(point.coord(k));
        }
        return new IPoint(coordinates);
    }

    /*Repeat.SectionStart main*/
    /**
     * Returns the number of dimensions of this point.
     * Equivalent to <tt>{@link #coordinates()}.length</tt>, but works faster.
     *
     * <p>The result of this method is always positive (&gt;0).
     *
     * @return the number of dimensions of this point.
     */
    public int coordCount() {
        return coordinates.length;
    }

    /**
     * Returns all coordinates of this point. The element <tt>#0</tt> of the returned array
     * is <i>x</i>-coordinate, the element <tt>#1</tt> is <i>y</i>-coordinate, etc.
     * The length of the returned array is the number of dimensions of this point.
     *
     * <p>The returned array is a clone of the internal coordinates array stored in this object.
     * The returned array is never empty (its length &gt;0 always).
     *
     * @return all coordinates of this point.
     */
    public long[] coordinates() {
        return coordinates.clone();
    }

    /**
     * Copies all coordinates of this point into <tt>result</tt> array.
     * The element <tt>#0</tt> of this array will contain <i>x</i>-coordinate,
     * the element <tt>#1</tt> will contain <i>y</i>-coordinate, etc.
     * The length of the passed array must be not less than the number of dimensions of this point.
     *
     * @param result the array where you want to store results.
     * @return       a reference to the passed <tt>result</tt> array.
     * @throws NullPointerException     if <tt>result</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>result.length&lt;{@link #coordCount()}</tt>.
     */
    public long[] coordinates(long[] result) {
        if (result.length < coordinates.length) {
            throw new IllegalArgumentException("Too short result array: long[" + result.length
                +"]; " + coordinates.length + " elements required to store coordinates");
        }
        System.arraycopy(coordinates, 0, result, 0, coordinates.length);
        return result;
    }

    /**
     * Returns the coordinate <tt>#coordIndex</tt>: <i>x</i>-coordinate for <tt>coordIndex=0</tt>,
     * <i>y</i>-coordinate for <tt>coordIndex=1</tt>, etc.
     *
     * @param coordIndex the index of the coordinate.
     * @return           the coordinate.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public long coord(int coordIndex) {
        return coordinates[coordIndex];
    }

    /**
     * Returns the <i>x</i>-coordinate: equivalent to {@link #coord(int) coord(0)}.
     *
     * @return <i>x</i>-coordinate.
     */
    public long x() {
        return coordinates[0];
    }

    /**
     * Returns <i>y</i>-coordinate: equivalent to {@link #coord(int) coord(1)}.
     * The only difference: in a case of 1-dimensional point (<tt>{@link #coordCount()}&lt;2</tt>),
     * this method throws <tt>IllegalStateException</tt> instead of <tt>IndexOutOfBoundsException</tt>.
     *
     * @return <i>y</i>-coordinate.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;2</tt>.
     */
    public long y() {
        if (coordinates.length < 2)
            throw new IllegalStateException("Cannot get y-coordinate of " + coordinates.length + "-dimensional point");
        return coordinates[1];
    }

    /**
     * Returns <i>z</i>-coordinate: equivalent to {@link #coord(int) coord(2)}.
     * The only difference: in a case of 1- or 2-dimensional point (<tt>{@link #coordCount()}&lt;3</tt>),
     * this method throws <tt>IllegalStateException</tt> instead of <tt>IndexOutOfBoundsException</tt>.
     *
     * @return <i>z</i>-coordinate.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;3</tt>.
     */
    public long z() {
        if (coordinates.length < 3)
            throw new IllegalStateException("Cannot get z-coordinate of " + coordinates.length + "-dimensional point");
        return coordinates[2];
    }

    /**
     * Returns <tt>true</tt> if this point is the origin of coordinates.
     * In other words, returns <tt>true</tt> if all coordinates of this point are zero.
     *
     * @return <tt>true</tt> if this point is the origin of coordinates.
     */
    public boolean isOrigin() {
        for (long coord : coordinates) {
            if (coord != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the distance between this point and the origin of coordinates.
     *
     * <p>All calculations are performed in <tt>strictfp</tt> mode, so the result
     * is absolutely identical on all platforms.
     *
     * @return the distance between this point and the origin of coordinates.
     */
    public strictfp double distanceFromOrigin() {
        if (coordinates.length == 1) {
            return StrictMath.abs(coordinates[0]);
        }
        double dSqr = 0.0;
        for (long coord : coordinates) {
            dSqr += (double) coord * (double) coord;
        }
        return StrictMath.sqrt(dSqr);
    }

    /**
     * Returns the minimal distance between this point and any point from the passed collection.
     * If is also called the Hausdorff distance between the point and the point set.
     * If the passed collection is empty, returns <tt>Double.POSITIVE_INFINITY</tt>.
     *
     * <p>All calculations are performed in <tt>strictfp</tt> mode, so the result
     * is absolutely identical on all platforms.
     *
     * @param points some collection of points.
     * @return       the Hausdorff distance from this point to the set of points, passed via the collection.
     * @throws NullPointerException     if the argument is <tt>null</tt> or if some elements of the passed
     *                                  collection are <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this point and in some
     *                                  of the given points are different.
     */
    public strictfp double distanceFrom(Collection<IPoint> points) {
        if (points == null)
            throw new NullPointerException("Null points argument");
        double result = Double.POSITIVE_INFINITY;
        for (IPoint point : points) {
            if (point.coordCount() != coordinates.length)
                throw new IllegalArgumentException("Dimensions count mismatch: some of the passed points has "
                    + point.coordCount() + " dimensions instead of " + coordinates.length);
            double d;
            if (coordinates.length == 1) {
                d = StrictMath.abs((double) coordinates[0] - (double) point.coordinates[0]);
            } else if (coordinates.length == 2) {
                d = StrictMath.hypot(
                    (double) coordinates[0] - (double) point.coordinates[0],
                    (double) coordinates[1] - (double) point.coordinates[1]);
            } else {
                double dSqr = 0.0;
                for (int k = 0; k < coordinates.length; k++) {
                    double diff = (double) coordinates[k] - (double) point.coordinates[k];
                    dSqr += diff * diff;
                }
                d = StrictMath.sqrt(dSqr);
            }
            if (d < result) {
                result = d;
            }
        }
        return result;
    }

    /**
     * Returns the vector sum of this and given point:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>thisInstance.{@link #coord(int) coord(i)}+point.{@link #coord(int) coord(i)}</tt>.
     *
     * @param point the added point.
     * @return      the vector sum of this and given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public IPoint add(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] + point.coordinates[k];
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the vector difference of this and given point:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>thisInstance.{@link #coord(int) coord(i)}-point.{@link #coord(int) coord(i)}</tt>.
     *
     * @param point the subtracted point.
     * @return      the vector difference of this and given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public IPoint subtract(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] - point.coordinates[k];
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the coordinate-wise minimum of this and given point:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>Math.min(thisInstance.{@link #coord(int) coord(i)},point.{@link #coord(int) coord(i)})</tt>.
     *
     * @param point the compared point.
     * @return      the coordinate-wise minimum this and given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public IPoint min(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Math.min(this.coordinates[k], point.coordinates[k]);
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the coordinate-wise maximum of this and given point:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>Math.max(thisInstance.{@link #coord(int) coord(i)},point.{@link #coord(int) coord(i)})</tt>.
     *
     * @param point the compared point.
     * @return      the coordinate-wise maximum this and given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public IPoint max(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Math.max(this.coordinates[k], point.coordinates[k]);
        }
        return new IPoint(coordinates);
    }

    /**
     * Adds the given value to all coordinates of this point and returns the resulting point:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>thisInstance.{@link #coord(int) coord(i)}+increment</tt>.
     * In other words, shifts this point along all axes by the given value.
     *
     * <p>Equivalent to <tt>{@link #add(IPoint) add}({@link IPoint#valueOfEqualCoordinates(int, long)
     * IPoint.valueOfEqualCoordinates}(n,increment))</tt>, where <tt>n={@link #coordCount()}</tt>.
     *
     * @param increment the value, which will be added to all coordinates of this point.
     * @return          this resulting point.
     */
    public IPoint addToAllCoordinates(long increment) {
        if (increment == 0) {
            return this;
        }
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] + increment;
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the product of this point and the given scalar <tt>multiplier</tt>:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>StrictMath.round(thisInstance.{@link #coord(int) coord(i)}*multiplier)</tt>.
     *
     * <p>Equivalent to {@link #roundedScale(double... multipliers)}, where all {@link #coordCount()} arguments
     * of that method are equal to <tt>multiplier</tt>.
     *
     * @param multiplier the multiplier.
     * @return     the product of this point and the given scalar <tt>multiplier</tt>.
     */
    public IPoint roundedMultiply(double multiplier) {
        if (multiplier == 0.0) {
            return origin(coordinates.length);
        } else if (multiplier == 1.0) {
            return this;
        } else if (multiplier == -1.0) {
            return symmetric();
        } else {
            long[] coordinates = new long[this.coordinates.length];
            for (int k = 0; k < coordinates.length; k++) {
                coordinates[k] = StrictMath.round(this.coordinates[k] * multiplier);
            }
            return new IPoint(coordinates);
        }
    }

    /**
     * Returns new point, each coordinate <tt>#i</tt> of which is
     * <tt>StrictMath.round(thisInstance.{@link #coord(int) coord(i)}*multipliers[i])</tt>.
     * The length of <tt>multipliers</tt> array must be equal to {@link #coordCount()}.
     *
     * <p>Note: this method does not perform actual multiplication to multipliers, equal to 1.0, 0.0 and &minus;1.0.
     * If the condition <tt>multipliers[k]==1.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> is just copied from this point into the result.
     * If the condition <tt>multipliers[k]==0.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> in the result will be <tt>0.0</tt> (always <tt>+0.0</tt>, regardless
     * of the sign of this coordinate in the source point).
     * If the condition <tt>multipliers[k]==-1.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> in the result will be equal to <tt>-{@link #coord(int) coord}(k)</tt>.
     *
     * @param multipliers the multipliers for all coordinates.
     * @return            the point, each coordinate <tt>#i</tt> of which is product of the corresponding coordinate
     *                    <tt>#i</tt> of this one and the corresponding multiplier <tt>multiplier[i]</tt>.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is not equal to
     *                                  <tt>multipliers.length</tt>.
     */
    public IPoint roundedScale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != coordinates.length)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + coordinates.length);
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = multipliers[k] == 0.0 ? 0 :
                multipliers[k] == 1.0 ? this.coordinates[k] :
                    multipliers[k] == -1.0 ? -this.coordinates[k] :
                        StrictMath.round(this.coordinates[k] * multipliers[k]);
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns new point, each coordinate <tt>#i</tt> of which is
     * <tt>shift.{@link Point#coord(int) coord(i)}+thisInstance.{@link #coord(int) coord(i)}*multipliers[i]</tt>.
     * The length of <tt>multipliers</tt> array must be equal to {@link #coordCount()}.
     *
     * <p>Note: this method does not perform actual multiplication to multipliers, equal to 1.0, 0.0 and &minus;1.0.
     * If the condition <tt>multipliers[k]==1.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> will be equal to
     * <tt>shift.{@link Point#coord(int) coord}(k)+thisInstance.{@link #coord(int) coord}(k)</tt>.
     * If the condition <tt>multipliers[k]==0.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> in the result will be equal to
     * <tt>shift.{@link Point#coord(int) coord}(k)</tt>.
     * If the condition <tt>multipliers[k]==-1.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> in the result will be equal to
     * <tt>shift.{@link Point#coord(int) coord}(k)-thisInstance.{@link #coord(int) coord}(k)</tt>.
     *
     * @param multipliers the multipliers for all coordinates.
     * @param shift       the shift along all coordinates.
     * @return            the point, each coordinate <tt>#i</tt> of which is product of the corresponding coordinate
     *                    <tt>#i</tt> of this one and the corresponding multiplier <tt>multiplier[i]</tt>,
     *                    incremented by the corresponding coordinate
     *                    <tt>shift.{@link Point#coord(int) coord(i)}</tt>.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is not equal to
     *                                  <tt>multipliers.length</tt>.
     */
    public Point scaleAndShift(double[] multipliers, Point shift) {
        double[] coordinates = new double[this.coordinates.length];
        scaleAndShift(coordinates, multipliers, shift);
        return new Point(coordinates);
    }

    /**
     * More efficient version of {@link #scaleAndShift(double[], Point)} method,
     * which stores the coordinates of the result in the passed Java array instead of creating new instance
     * of this class.
     * Equivalent to the following call:
     * <nobr><tt>{@link #scaleAndShift(double[], Point) scaleAndShift}(multipliers,shift).{@link #coordinates(long[])
     * coordinates}(resultCoordinates)</tt></nobr>, but works little faster.
     *
     * @param resultCoordinates Java array for storing results.
     * @param multipliers       the multipliers for all coordinates.
     * @param shift             the shift along all coordinates.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is greater than
     *                                  <tt>resultCoordinates.length</tt> or is not equal to
     *                                  <tt>multipliers.length</tt>.
     */
    public void scaleAndShift(double[] resultCoordinates, double[] multipliers, Point shift) {
        if (resultCoordinates.length < coordinates.length) {
            throw new IllegalArgumentException("Too short result coordinates array: double["
                + resultCoordinates.length +"]; " + coordinates.length + " elements required to store coordinates");
        }
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != coordinates.length)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + coordinates.length);
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + shift.coordCount() + " instead of " + coordinates.length);
        for (int k = 0; k < coordinates.length; k++) {
            resultCoordinates[k] = multipliers[k] == 0.0 ? shift.coord(k) :
                multipliers[k] == 1.0 ? shift.coord(k) + this.coordinates[k] :
                    multipliers[k] == -1.0 ? shift.coord(k) - this.coordinates[k] :
                        shift.coord(k) + this.coordinates[k] * multipliers[k];
        }
    }

    /**
     * Returns this point shifted by the passed <tt>shift</tt> along the axis <tt>#coordIndex</tt>.
     * Equivalent to {@link #add(IPoint) add(p)}, where <tt>p</tt> is the point with coordinates
     * <i>p</i><sub>0</sub>, <i>p</i><sub>1</sub>, ...,
     * <i>p<sub>k</sub></i><tt>=0</tt> for <i>k</i><tt>!=coordIndex</tt>,
     * <i>p<sub>k</sub></i><tt>=shift</tt> for <i>k</i><tt>=coordIndex</tt>.
     *
     * @param coordIndex the index of the axis.
     * @param shift      the shift along this axis.
     * @return           this point shifted along this axis.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public IPoint shiftAlongAxis(int coordIndex, long shift) {
        coord(coordIndex); // check for illegal coordIndex
        if (shift == 0) {
            return this;
        }
        long[] coordinates = this.coordinates.clone();
        coordinates[coordIndex] += shift;
        return new IPoint(coordinates);
    }

    /**
     * Returns the scalar product of this and given point.
     * The result is the sum of all products
     * <tt>(double)thisInstance.{@link #coord(int) coord(i)}*(double)point.{@link #coord(int) coord(i)}</tt>
     * for all coordinate indexes <tt>i</tt>.
     *
     * <p>All calculations are performed in <tt>strictfp</tt> mode, so the result
     * is absolutely identical on all platforms.
     *
     * @param point another point.
     * @return      the scalar product of this and given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public strictfp double scalarProduct(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        double result = 0.0;
        for (int k = 0; k < coordinates.length; k++) {
            result += (double)coordinates[k] * (double)point.coordinates[k];
        }
        return result;
    }

    /**
     * Returns the symmetric point relatively the origin of coordinates.
     * Equivalent to {@link #multiply(double) multiply(-1.0)}.
     *
     * @return the symmetric point relatively the origin of coordinates.
     */
    public IPoint symmetric() {
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = -this.coordinates[k];
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the projection of this point along the given axis with the number of coordinates,
     * decreased by 1. Namely, the resulting point <i>P</i> has
     * {@link #coordCount()}&minus;1 coordinates, equal to
     * <nobr><i>P</i><tt>.{@link #coord(int) coord}(</tt><i>i</i><tt>)=thisInstance.{@link #coord(int)
     * coord}(</tt><i>i'</i><tt>)</tt></nobr>, <i>i'</i>=<i>i</i> for <i>i</i>&lt;<tt>coordIndex</tt> or
     * <i>i'</i>=<i>i</i>+1 for <i>i</i>&ge;<tt>coordIndex</tt>.
     *
     * @param coordIndex the number of coordinate, along which the projecting is performed.
     * @return           the projection of this point along the coordinates axis <tt>#coordIndex</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     * @throws IllegalStateException     if this point is 1-dimensional (<tt>{@link #coordCount()}==1</tt>).
     */
    public IPoint projectionAlongAxis(int coordIndex) {
        coord(coordIndex); // check for illegal coordIndex
        if (this.coordinates.length == 1)
            throw new IllegalStateException("Cannot perform projection of 1-dimensional figures");
        long[] coordinates = new long[this.coordinates.length - 1];
        System.arraycopy(this.coordinates, 0, coordinates, 0, coordIndex);
        System.arraycopy(this.coordinates, coordIndex + 1, coordinates, coordIndex, coordinates.length - coordIndex);
        return new IPoint(coordinates);
    }

    /**
     * Equivalent to <tt>{@link #projectionAlongAxis(int) projectionAlongAxis}(coordIndex).equals(point)</tt>,
     * but works faster (no Java objects are allocated).
     *
     * @param coordIndex the number of coordinate, along which the projecting is performed.
     * @param point      another point.
     * @return           <tt>true</tt> if and only if the projection of this point along the given axis
     *                   is a point equal to the second argument.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>
     * @throws IllegalStateException     if this point is 1-dimensional (<tt>{@link #coordCount()}==1</tt>).
     * @throws IllegalArgumentException  if the {@link #coordCount() numbers of dimensions} of this point
     *                                   is not equal to <tt>point.{@link #coordCount()}+1</tt>.
     */
    boolean projectionAlongAxisEquals(int coordIndex, IPoint point) {
        // Right now I'm not sure that this method is really useful. Can be made "public" at any moment.
        coord(coordIndex); // check for illegal coordIndex
        if (this.coordinates.length == 1)
            throw new IllegalStateException("Cannot perform projection of 1-dimensional figures");
        if (this.coordinates.length != point.coordCount() + 1)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " is not equal to " + coordinates.length + "-1");

        for (int k = 0; k < coordIndex; k++) {
            if (this.coordinates[k] != point.coordinates[k]) {
                return false;
            }
        }
        for (int k = coordIndex + 1; k < this.coordinates.length; k++) {
            if (this.coordinates[k] != point.coordinates[k - 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares points lexicographically.
     *
     * <p>More precisely, let
     * <i>x</i><sub><i>i</i></sub> is <tt>thisInstance.{@link #coord(int) coord}(<i>i</i>)</tt>
     * for <tt>0&lt;=<i>i</i>&lt;thisInstance.{@link #coordCount()}</tt> and
     * <i>x</i><sub><i>i</i></sub>=0 for <tt><i>i</i>&gt;=thisInstance.{@link #coordCount()}</tt>.
     * Then, let
     * <i>y</i><sub><i>i</i></sub> is <tt>o.{@link #coord(int) coord}(<i>i</i>)</tt>
     * for <tt>0&lt;=<i>i</i>&lt;o.{@link #coordCount()}</tt> and
     * <i>y</i><sub><i>i</i></sub>=0 for <tt><i>i</i>&gt;=o.{@link #coordCount()}</tt>.
     * This method returns a negative integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&lt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>;
     * this method returns a positive integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&gt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>.
     * If all <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub>,
     * this method returns a negative integer, 0 or a positive integer if
     * <tt>thisInstance.{@link #coordCount()}</tt> is less than, equal to, or greater than
     * <tt>o.{@link #coordCount()}</tt>.
     *
     * @param o the point to be compared.
     * @return  negative integer, zero, or a positive integer as this point
     *          is lexicographically less than, equal to, or greater than <tt>o</tt>.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public int compareTo(IPoint o) {
        return compareTo(o, 0);
    }

    /**
     * Compares points lexicographically alike {@link #compareTo(IPoint)} method,
     * but with the cyclical shift of all indexes of coordinates:
     * the coordinate <tt>#firstCoordIndex</tt> instead of <i>x</i>,
     * <tt>#firstCoordIndex+1</tt> instead of <i>y</i>, etc.
     *
     * <p>More precisely, let
     * <i>n</i><tt>=max(thisInstance.{@link #coordCount()},o.{@link #coordCount()})</tt>,
     * <i>x</i><sub><i>i</i></sub> is <tt>thisInstance.{@link #coord(int)
     * coord}((<i>i</i>+firstCoordIndex)%<i>n</i>)</tt>,
     * <i>y</i><sub><i>i</i></sub> is <tt>o.{@link #coord(int)
     * coord}((<i>i</i>+firstCoordIndex)%<i>n</i>)</tt>.
     * As in {@link #compareTo(IPoint)} method, we suppose here that
     * all coordinates {@link #coord(int) coord(<i>k</i>)} with <tt><i>k</i>&gt;={@link #coordCount()}</tt>
     * are zero.
     * This method returns a negative integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&lt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>;
     * this method returns a positive integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&gt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>.
     * If all <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub>,
     * this method returns a negative integer, 0 or a positive integer if
     * <tt>thisInstance.{@link #coordCount()}</tt> is less than, equal to, or greater than
     * <tt>o.{@link #coordCount()}</tt>.
     *
     * @param o               the point to be compared.
     * @param firstCoordIndex the index of "first" coordinate, that is compared after all other coordinates.
     * @return                negative integer, zero, or a positive integer as this point
     *                        is lexicographically less than, equal to, or greater than <tt>o</tt>.
     * @throws NullPointerException     if the <tt>o</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>firstCoordIndex</tt> is negative.
     */
    public int compareTo(IPoint o, int firstCoordIndex) {
        int n = Math.max(coordinates.length, o.coordinates.length);
        if (firstCoordIndex < 0)
            throw new IllegalArgumentException("Negative firstCoordIndex argument");
        firstCoordIndex = firstCoordIndex % n;
        for (int k = n - 1; k >= 0; k--) {
            int index = k + firstCoordIndex;
            if (index >= n)
                index -= n;
            long thisCoord = index >= coordinates.length ? 0 : coordinates[index];
            long otherCoord = index >= o.coordinates.length ? 0 : o.coordinates[index];
            if (thisCoord > otherCoord) {
                return 1;
            }
            if (thisCoord < otherCoord) {
                return -1;
            }
        }
        return coordinates.length < o.coordinates.length ? -1 :
            coordinates.length > o.coordinates.length ? 1 : 0;
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * a list of all point coordinates.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        StringBuffer result = new StringBuffer("(");
        for (int k = 0; k < coordinates.length; k++) {
            if (k > 0) {
                result.append(", ");
            }
            result.append(coordinates[k]);
        }
        result.append(")");
        return result.toString();
    }

    /**
     * Returns the hash code of this point. The result depends on
     * all {@link #coordinates() coordinates}.
     *
     * @return the hash code of this point.
     */
    public int hashCode() {
        Checksum sum = new CRC32();
        byte[] bytes = new byte[coordinates.length * 8];
        getBytes(coordinates, bytes);
        sum.update(bytes, 0, bytes.length);
        return (int)sum.getValue() ^ 1839581;
    }
    /*Repeat.SectionEnd main*/

    /**
     * Indicates whether some other point is equal to this instance,
     * that is the number of coordinates is the same and all corresponding coordinates are equal.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <tt>true</tt> if the specified object is a point equal to this one.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof IPoint)) {
            return false;
        }
        IPoint ip = (IPoint) obj;
        if (ip.coordinates.length != coordinates.length) {
            return false;
        }
        for (int k = 0; k < coordinates.length; k++) {
            if (ip.coordinates[k] != coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the product of this point and the given scalar <tt>multiplier</tt>:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>(long)(thisInstance.{@link #coord(int) coord(i)}*multiplier)</tt>.
     *
     * <p>Equivalent to {@link #scale(double... multipliers)}, where all {@link #coordCount()} arguments
     * of that method are equal to <tt>multiplier</tt>.
     *
     * @param multiplier the multiplier.
     * @return     the product of this point and the given scalar <tt>multiplier</tt>.
     */
    public IPoint multiply(double multiplier) {
        if (multiplier == 0.0) {
            return origin(coordinates.length);
        } else if (multiplier == 1.0) {
            return this;
        } else if (multiplier == -1.0) {
            return symmetric();
        } else {
            long[] coordinates = new long[this.coordinates.length];
            for (int k = 0; k < coordinates.length; k++) {
                coordinates[k] = (long)(this.coordinates[k] * multiplier);
            }
            return new IPoint(coordinates);
        }
    }

    /**
     * Returns new point, each coordinate <tt>#i</tt> of which is
     * <tt>(long)(thisInstance.{@link #coord(int) coord(i)}*multipliers[i])</tt>.
     * The length of <tt>multipliers</tt> array must be equal to {@link #coordCount()}.
     *
     * <p>Note: this method does not perform actual multiplication to multipliers, equal to 1.0, 0.0 and &minus;1.0.
     * If the condition <tt>multipliers[k]==1.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> is just copied from this point into the result.
     * If the condition <tt>multipliers[k]==0.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> in the result will be <tt>0.0</tt> (always <tt>+0.0</tt>, regardless
     * of the sign of this coordinate in the source point).
     * If the condition <tt>multipliers[k]==-1.0</tt> is <tt>true</tt> for some <tt>k</tt>,
     * then the coordinate <tt>#k</tt> in the result will be equal to <tt>-{@link #coord(int) coord}(k)</tt>.
     *
     * @param multipliers the multipliers for all coordinates.
     * @return            the point, each coordinate <tt>#i</tt> of which is product of the corresponding coordinate
     *                    <tt>#i</tt> of this one and the corresponding multiplier <tt>multiplier[i]</tt>.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is not equal to
     *                                  <tt>multipliers.length</tt>.
     */
    public IPoint scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != coordinates.length)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + coordinates.length);
        long[] coordinates = new long[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = multipliers[k] == 0.0 ? 0 :
                multipliers[k] == 1.0 ? this.coordinates[k] :
                    multipliers[k] == -1.0 ? -this.coordinates[k] :
                        (long) (this.coordinates[k] * multipliers[k]);
        }
        return new IPoint(coordinates);
    }

    /**
     * Returns the index in the one-dimensional array, storing (in usual order) some <i>n</i>-dimensional matrix
     * with given dimensions, corresponding to the position in this matrix, describing
     * by coordinates of this point.
     *
     * <p>More precisely, if the <tt>pseudoCyclicTruncation</tt> argument is <tt>false</tt>, returns the following value:
     * <tt>
     * shift = {@link #coord(int) coord(0)} +
     * {@link #coord(int) coord(1)}*<i>dim</i><sub>0</sub> +
     * {@link #coord(int) coord(2)}*<i>dim</i><sub>0</sub>*<i>dim</i><sub>1</sub> +
     * ... +
     * {@link #coord(int)
     * coord(<i>n</i>-1)}*<i>dim</i><sub>0</sub>*<i>dim</i><sub>1</sub>*...*<i>dim</i><sub><i>n</i>-2</sub>
     * </tt>
     * (<i>n</i> = {@link #coordCount()},
     * where <tt><i>dim</i><sub><i>i</i></sub>=<i>i</i>&gt;=dimensions.length?1:dimensions[<i>i</i>]</tt>.
     * If <tt>pseudoCyclicTruncation</tt> is <tt>true</tt>, returns the positive remainder of division of this value
     * by the product of all dimensions:
     * <pre>
     * shift%product &gt;= 0 ? shift%product : shift%product&nbsp;+&nbsp;product,
     * </pre>
     * <tt>product&nbsp;=&nbsp;<i>dim</i><sub>0</sub>*<i>dim</i><sub>1</sub>*...*<i>dim</i><sub><i>n</i>-1</sub></tt>.
     * (In the special case <tt>product==0</tt>, if <tt>pseudoCyclicTruncation</tt> is <tt>true</tt>, this method
     * returns 0 and does not throw "division by zero" exception.)
     *
     * <p>All elements of <tt>dimensions</tt> array must be positive or zero.
     * All point coordinates are always used, regardless of the length of <tt>dimensions</tt> array.
     *
     * <p>If <tt>pseudoCyclicTruncation</tt> is <tt>true</tt> and the product of all dimensions
     * <tt>product&nbsp;=&nbsp;<i>dim</i><sub>0</sub>*<i>dim</i><sub>1</sub>*...*<i>dim</i><sub><i>n</i>-1</sub></tt>
     * is not greater than <tt>Long.MAX_VALUE</tt>, then all calculations are performed absolutely precisely,
     * even in a case when the direct calculation according the formulas above leads to overflow (because some
     * of values in these formulas are out of <tt>Long.MIN_VALUE..Long.MAX_VALUE</tt> range).
     * However, if <tt>product&gt;Long.MAX_VALUE</tt>, the results will be probably incorrect due to overflow.
     *
     * <p>If <tt>pseudoCyclicTruncation</tt> is false, the result is calculated by the traditional Horner scheme
     * without any overflow checks, using standard Java <tt>long</tt> arithmetic:
     * <pre>
     * (...({@link #coord(int)
     * coord(<i>n</i>-1)}*<i>dim</i><sub><i>n</i>-2</sub>+{@link #coord(int)
     * coord(<i>n</i>-2)})*<i>dim</i><sub><i>n</i>-3</sub>+...)*<i>dim</i><sub>0</sub>+{@link #coord(int) coord(0)}
     * </pre>
     * So, the result can be incorrect in a case of overflow.
     *
     * @param dimensions             the dimensions of some <i>n</i>-dimensional matrix, stored in the one-dimensional
     *                               array.
     * @param pseudoCyclicTruncation if <tt>true</tt>, the result is replaced with the positive remainder of division
     *                               by the product of all dimensions.
     * @return                       the index in this array, corresponding the position in the matrix,
     *                               describing by this point.
     * @throws NullPointerException     if <tt>dimensions</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if some elements of <tt>dimensions</tt> array are negative
     *                                  (however note, that this method does not check elements, indexes of which are
     *                                  &gt;={@link #coordCount()})
     */
    public long toOneDimensional(long[] dimensions, boolean pseudoCyclicTruncation) {
        // 4D example:
        // index = (u*nz*ny*nx + z*ny*nz + y*nx + x) % N =
        //       = (u'*nz*ny*nx + z'*ny*nz + y'*nx + x') % N =
        //       = ((((u'*nz + z')*ny + y')*nx + x') % N
        // (N = nu*nz*ny*nx), where
        //       u' = u % nu
        //       z' = z % (nu*nz)
        //       y' = y % (nu*nz*ny)
        //       x' = x % (nu*nz*ny*nx)
        //
        if (pseudoCyclicTruncation) {
            int n = coordinates.length <= dimensions.length ? coordinates.length : dimensions.length;
            if (n == 0) {
                return 0; // N = 1, so index = something % N = 0
            }
            --n;
            if (dimensions[n] < 0)
                throw new IllegalArgumentException("Negative dimensions[" + n + "]");
            long limit = dimensions[n];
            long result = limit == 0 ? 0 : coordinates[n] % limit;
            if (result < 0) {
                result += limit;
            }
            while (n > 0) {
                --n;
                if (dimensions[n] < 0)
                    throw new IllegalArgumentException("Negative dimensions[" + n + "]");
                limit *= dimensions[n];
                long coord = limit == 0 ? 0 : coordinates[n] % limit;
                // Note: if limit becomes 0, then this and all further "coord" will be 0, and the result will be 0
                if (coord < 0) {
                    coord += limit;
                }
                result *= dimensions[n];
                // If product of all dimensions is 63-bit, then overflow here is possible in the only case when some
                // of further dimensions are zero, and in this case it is unimportant: the result will be 0
                result += coord;
                if (result < 0 || result >= limit) { // If "*=* did not lead to overflow, "< 0" here means overflow
                    result -= limit;
                }
            }
            return result;

        } else {
            int n = coordinates.length - 1;
            if (n < dimensions.length && dimensions[n] < 0)
                throw new IllegalArgumentException("Negative dimensions[" + n + "]");
            long result = coordinates[n];
            for (int k = n - 1; k >= 0; k--) {
                if (k >= dimensions.length) {
                    result += coordinates[k];
                } else {
                    if (dimensions[k] < 0)
                        throw new IllegalArgumentException("Negative dimensions[" + k + "]");
                    result = result * dimensions[k] + coordinates[k];
                }
            }
            return result;
        }
    }

    /**
     * Equivalent to <tt>{@link Point#valueOf(IPoint) Point.valueOf}(thisInstance)</tt>.
     *
     * @return the real point with same coordinates.
     */
    public Point toPoint() {
        return Point.valueOf(this);
    }

    private static void getBytes(long[] array, byte[] result) {
        for (int disp = 0, k = 0; k < array.length; k++) {
            long l = array[k];
            int value = (int)l ^ 182596182; // provide different results than for other similar classes
            result[disp++] = (byte)value;
            result[disp++] = (byte)(value >>> 8);
            result[disp++] = (byte)(value >>> 16);
            result[disp++] = (byte)(value >>> 24);
            value = (int)(l >>> 32) ^ 785916747; // provide different results than for other similar classes
            result[disp++] = (byte)value;
            result[disp++] = (byte)(value >>> 8);
            result[disp++] = (byte)(value >>> 16);
            result[disp++] = (byte)(value >>> 24);
        }
    }

    /**
     * The simplest test for this class: shows a sorted array of several points.
     */
    static class Test {
        public static void main(String[] args) {
            IPoint[] p = {
                IPoint.valueOf(12, 3),
                IPoint.valueOf(12, 3, 1),
                IPoint.valueOf(12, 3, 0),
                IPoint.valueOf(12, 3, 0, 1234),
                IPoint.valueOf(12, 3, 0, -21234),
                IPoint.valueOf(-12, 123453, 27182, 821234),
                IPoint.valueOf(14, -3),
                IPoint.valueOf(0),
                IPoint.valueOf(0, 0),
                IPoint.valueOf(0, 2),
                IPoint.valueOf(0, 1),
                IPoint.valueOf(-1, -14),
                IPoint.valueOf(-1, 1),
                IPoint.valueOf(1, 4),
                IPoint.valueOf(1, 1),
                IPoint.valueOf(2, 4),
                IPoint.valueOf(2, 3),
                IPoint.valueOf(0, 0, 0),
                IPoint.origin(3),
                IPoint.valueOf(new long[18]),
                IPoint.valueOf(new long[18]),
                IPoint.valueOf(13, 0),
                IPoint.valueOf(-13, 0),
                IPoint.valueOf(13, 0, 1),
                IPoint.valueOf(3, 4, 0),
                IPoint.valueOf(13),
                IPoint.valueOf(Long.MIN_VALUE, Long.MIN_VALUE),
                IPoint.valueOf(100, Long.MAX_VALUE),
                IPoint.valueOf(Long.MIN_VALUE + 1, -2),
                IPoint.valueOf(Long.MIN_VALUE + 1, -2),
                IPoint.valueOf(Long.MIN_VALUE + 1, -3),
            };
            java.util.Arrays.sort(p);
            long[] dimensions = {10, 10, 10};
            for (long[] ends : new long[][] {
                {0, 10},
                {0, Long.MAX_VALUE},
                {Long.MIN_VALUE, -100},
                {Long.MIN_VALUE + 1, -100},
                {Long.MIN_VALUE + 2, -100},
                {Long.MIN_VALUE + 2, 100},
            }) {
                System.out.println("Range " + ends[0] + ".." + ends[1] + " is "
                    + (IRange.isAllowedRange(ends[0], ends[1]) ? "allowed: " + IRange.valueOf(ends[0], ends[1])
                    : "not allowed"));
            }
            for (IPoint ip : p) {
                System.out.println(ip + "; symmetric: " + ip.symmetric()
                    + "; distance from origin: " + ip.distanceFromOrigin()
                    + " = " + ip.distanceFrom(Arrays.asList(IPoint.origin(ip.coordCount())))
                    + (ip.coordCount() > 1 && ip.projectionAlongAxisEquals(0, IPoint.origin(ip.coordCount() - 1)) ?
                    "; x-projection is origin" : "")
                    + "; x-shift: " + ip.shiftAlongAxis(0, 100)
                    + "; x-projection: "
                    + (ip.coordCount() == 1 ? "impossible" : ip.projectionAlongAxis(0))
                    + "; last-axis-projection: "
                    + (ip.coordCount() == 1 ? "impossible" : ip.projectionAlongAxis(ip.coordCount() - 1))
                    + "; shift in 10x10x10: " + ip.toOneDimensional(dimensions, true)
                    + "; *1.1: " + ip.multiply(1.1)
                    + " = " + ip.scale(Point.valueOfEqualCoordinates(ip.coordCount(), 1.1).coordinates())
                    + "; round *1.1: " + ip.roundedMultiply(1.1)
                    + " = " + ip.roundedScale(Point.valueOfEqualCoordinates(ip.coordCount(), 1.1).coordinates())
                    + " ~ " + ip.scaleAndShift(
                    Point.valueOfEqualCoordinates(ip.coordCount(), 1.1).coordinates(),
                    Point.origin(ip.coordCount()))
                    + "; sqr: " + ip.scalarProduct(ip)
                    + "; hash: " + ip.hashCode() + "; address: " + System.identityHashCode(ip));
            }
            System.out.println();
            for (int k = 0; k < p.length - 1; k += 2) {
                try {
                    IRectangularArea ra = IRectangularArea.valueOf(p[k], p[k + 1]);
                    assert IRectangularArea.valueOf(ra.ranges()).equals(ra);
                    System.out.println(ra + "; ranges: " + java.util.Arrays.asList(ra.ranges())
                        + "; contains(origin): " + ra.contains(IPoint.origin(ra.coordCount()))
                        + "; expand(origin): " + ra.expand(IPoint.origin(ra.coordCount()))
                        + "; expand(-1,-1..2,2): " + ra.expand(IRectangularArea.valueOf(
                        IPoint.valueOfEqualCoordinates(ra.coordCount(), -1),
                        IPoint.valueOfEqualCoordinates(ra.coordCount(), 2)))
                        + " hash: " + ra.hashCode());
                } catch (Exception e) {
                    System.out.println("  Cannot create area with " + p[k] + " and " + p[k + 1] + ": " + e);
                }
            }
        }
    }
}
