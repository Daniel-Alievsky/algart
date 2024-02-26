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

package net.algart.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.*;

/**
 * <p>Point in multidimensional space with real coordinates.
 * Represented as an array of <tt>double</tt> numbers.</p>
 *
 * <p>All calculations in this class are performed in <tt>strictfp</tt> mode, so the result
 * is absolutely identical on all platforms.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see IPoint
 */
public strictfp class Point implements Comparable<Point> {
    /*Repeat(INCLUDE_FROM_FILE, IPoint.java, begin)
      Long.MIN_VALUE ==> Double.NEGATIVE_INFINITY ;;
      Long.MAX_VALUE ==> Double.POSITIVE_INFINITY ;;
      IPoint ==> Point;;
      long ==> double   !! Auto-generated: NOT EDIT !! */
    private static final Point originsCache[] = new Point[16];
    static {
        for (int i = 1; i <= originsCache.length; i++)
            originsCache[i - 1] = new Point(new double[i]) { // zero-filled by Java
                @Override
                public boolean isOrigin() {
                    return true;
                }
            };
    }

    final double[] coordinates;

    Point(double[] coordinates) {
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
    public static Point valueOf(double ...coordinates) {
        if (coordinates == null)
            throw new NullPointerException("Null coordinates argument");
        if (coordinates.length == 0)
            throw new IllegalArgumentException("Empty coordinates array");
        if (coordinates.length <= originsCache.length) {
            boolean origin = true;
            for (double coord : coordinates) {
                if (coord != 0) {
                    origin = false;
                }
            }
            if (origin)
                return originsCache[coordinates.length - 1];
        }
        return new Point(coordinates.clone());
    }

    /**
     * Returns a new 1-dimensional point with the given coordinate.
     *
     * @param x cartesian coordinate of the point.
     * @return point with the given coordinates.
     */
    public static Point valueOf(double x) {
        return x == 0 ? originsCache[0] : new Point(new double[]{x});
    }

    /**
     * Returns a new 2-dimensional point with the given coordinates.
     *
     * @param x cartesian x-coordinate of the point.
     * @param y cartesian y-coordinate of the point.
     * @return point with the given coordinates.
     */
    public static Point valueOf(double x, double y) {
        return x == 0 && y == 0 ? originsCache[1] : new Point(new double[] {x, y});
    }

    /**
     * Returns a new 3-dimensional point with the given coordinates.
     *
     * @param x cartesian x-coordinate of the point.
     * @param y cartesian y-coordinate of the point.
     * @param z cartesian z-coordinate of the point.
     * @return point with the given coordinates.
     */
    public static Point valueOf(double x, double y, double z) {
        return x == 0 && y == 0 && z == 0 ? originsCache[2] : new Point(new double[] {x, y, z});
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
    public static Point valueOfEqualCoordinates(int coordCount, double filler) {
        if (filler == 0) {
            return origin(coordCount);
        }
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        double[] coordinates = new double[coordCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = filler;
        }
        return new Point(coordinates);
    }

    /**
     * Returns the origin of coordinates in <i>n</i>-dimensional space,
     * where <i>n</i><tt>=coordCount</tt> is the argument of this method.
     *
     * @param coordCount the number of dimensions.
     * @return           the origin of coordinates in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point origin(int coordCount) {
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        if (coordCount <= originsCache.length) {
            return originsCache[coordCount - 1];
        } else {
            return new Point(new double[coordCount]); // zero-filled by Java
        }
    }

    /**
     * Returns the "minimal" point in <i>n</i>-dimensional space,
     * where <i>n</i><tt>=coordCount</tt> is the argument of this method,
     * that is the point with all coordinates are equal to <tt>Double.NEGATIVE_INFINITY</tt>.
     *
     * @param coordCount the number of dimensions.
     * @return           the "minimal" point in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point minValue(int coordCount) {
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        double[] coordinates = new double[coordCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Double.NEGATIVE_INFINITY;
        }
        return new Point(coordinates);
    }

    /**
     * Returns the "maximal" point in <i>n</i>-dimensional space,
     * where <i>n</i><tt>=coordCount</tt> is the argument of this method,
     * that is the point with all coordinates are equal to <tt>Double.POSITIVE_INFINITY</tt>.
     *
     * @param coordCount the number of dimensions.
     * @return           the "maximal" point in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point maxValue(int coordCount) {
        if (coordCount <= 0)
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        double[] coordinates = new double[coordCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Double.POSITIVE_INFINITY;
        }
        return new Point(coordinates);
    }
    /*Repeat.IncludeEnd*/

    /**
     * Returns a new point with the same coordinates as the given integer point.
     * All <tt>long</tt> coordinates of the passed integer point are converted
     * to <tt>double</tt> coordinates of the returned point by standard
     * Java typecast <tt>(double)longValue</tt>.
     *
     * @param iPoint the integer point.
     * @return       the real point with same coordinates.
     * @throws NullPointerException if the passed integer point is <tt>null</tt>.
     */
    public static Point valueOf(IPoint iPoint) {
        if (iPoint == null)
            throw new NullPointerException("Null iPoint argument");
        double[] coordinates = new double[iPoint.coordCount()];
        for (int k = 0; k < coordinates.length; k++)
            coordinates[k] = iPoint.coord(k);
        return new Point(coordinates);
    }

    /*Repeat(INCLUDE_FROM_FILE, IPoint.java, main)
      IPoint ==> Point;;
      (?<![Aa])long ==> double;;
      roundedMultiply ==> multiply;;
      roundedScale ==> scale;;
      StrictMath.round\((thisInstance.*?\*multiplier)\) ==> $1;;
      StrictMath.round\((thisInstance.*?\*multipliers\[i\])\) ==> $1;;
      \(double\)\s?((?:thisInstance|point)\.\{) ==> $1;;
      \(double\)\s?(coord\b|(?:point\.)?coordinates\[\w\]) ==> $1;;
      StrictMath.round ==>   !! Auto-generated: NOT EDIT !! */
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
    public double[] coordinates() {
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
    public double[] coordinates(double[] result) {
        if (result.length < coordinates.length) {
            throw new IllegalArgumentException("Too short result array: double[" + result.length
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
    public double coord(int coordIndex) {
        return coordinates[coordIndex];
    }

    /**
     * Returns the <i>x</i>-coordinate: equivalent to {@link #coord(int) coord(0)}.
     *
     * @return <i>x</i>-coordinate.
     */
    public double x() {
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
    public double y() {
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
    public double z() {
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
        for (double coord : coordinates) {
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
        for (double coord : coordinates) {
            dSqr += coord * coord;
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
    public strictfp double distanceFrom(Collection<Point> points) {
        if (points == null)
            throw new NullPointerException("Null points argument");
        double result = Double.POSITIVE_INFINITY;
        for (Point point : points) {
            if (point.coordCount() != coordinates.length)
                throw new IllegalArgumentException("Dimensions count mismatch: some of the passed points has "
                    + point.coordCount() + " dimensions instead of " + coordinates.length);
            double d;
            if (coordinates.length == 1) {
                d = StrictMath.abs(coordinates[0] - point.coordinates[0]);
            } else if (coordinates.length == 2) {
                d = StrictMath.hypot(
                    coordinates[0] - point.coordinates[0],
                    coordinates[1] - point.coordinates[1]);
            } else {
                double dSqr = 0.0;
                for (int k = 0; k < coordinates.length; k++) {
                    double diff = coordinates[k] - point.coordinates[k];
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
    public Point add(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] + point.coordinates[k];
        }
        return new Point(coordinates);
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
    public Point subtract(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] - point.coordinates[k];
        }
        return new Point(coordinates);
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
    public Point min(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Math.min(this.coordinates[k], point.coordinates[k]);
        }
        return new Point(coordinates);
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
    public Point max(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Math.max(this.coordinates[k], point.coordinates[k]);
        }
        return new Point(coordinates);
    }

    /**
     * Adds the given value to all coordinates of this point and returns the resulting point:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>thisInstance.{@link #coord(int) coord(i)}+increment</tt>.
     * In other words, shifts this point along all axes by the given value.
     *
     * <p>Equivalent to <tt>{@link #add(Point) add}({@link Point#valueOfEqualCoordinates(int, double)
     * Point.valueOfEqualCoordinates}(n,increment))</tt>, where <tt>n={@link #coordCount()}</tt>.
     *
     * @param increment the value, which will be added to all coordinates of this point.
     * @return          this resulting point.
     */
    public Point addToAllCoordinates(double increment) {
        if (increment == 0) {
            return this;
        }
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] + increment;
        }
        return new Point(coordinates);
    }

    /**
     * Returns the product of this point and the given scalar <tt>multiplier</tt>:
     * every coordinate <tt>#i</tt> in the result is
     * <tt>thisInstance.{@link #coord(int) coord(i)}*multiplier</tt>.
     *
     * <p>Equivalent to {@link #scale(double... multipliers)}, where all {@link #coordCount()} arguments
     * of that method are equal to <tt>multiplier</tt>.
     *
     * @param multiplier the multiplier.
     * @return     the product of this point and the given scalar <tt>multiplier</tt>.
     */
    public Point multiply(double multiplier) {
        if (multiplier == 0.0) {
            return origin(coordinates.length);
        } else if (multiplier == 1.0) {
            return this;
        } else if (multiplier == -1.0) {
            return symmetric();
        } else {
            double[] coordinates = new double[this.coordinates.length];
            for (int k = 0; k < coordinates.length; k++) {
                coordinates[k] = (this.coordinates[k] * multiplier);
            }
            return new Point(coordinates);
        }
    }

    /**
     * Returns new point, each coordinate <tt>#i</tt> of which is
     * <tt>thisInstance.{@link #coord(int) coord(i)}*multipliers[i]</tt>.
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
    public Point scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != coordinates.length)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + coordinates.length);
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = multipliers[k] == 0.0 ? 0 :
                multipliers[k] == 1.0 ? this.coordinates[k] :
                    multipliers[k] == -1.0 ? -this.coordinates[k] :
                        (this.coordinates[k] * multipliers[k]);
        }
        return new Point(coordinates);
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
     * <nobr><tt>{@link #scaleAndShift(double[], Point) scaleAndShift}(multipliers,shift).{@link #coordinates(double[])
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
     * Equivalent to {@link #add(Point) add(p)}, where <tt>p</tt> is the point with coordinates
     * <i>p</i><sub>0</sub>, <i>p</i><sub>1</sub>, ...,
     * <i>p<sub>k</sub></i><tt>=0</tt> for <i>k</i><tt>!=coordIndex</tt>,
     * <i>p<sub>k</sub></i><tt>=shift</tt> for <i>k</i><tt>=coordIndex</tt>.
     *
     * @param coordIndex the index of the axis.
     * @param shift      the shift along this axis.
     * @return           this point shifted along this axis.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public Point shiftAlongAxis(int coordIndex, double shift) {
        coord(coordIndex); // check for illegal coordIndex
        if (shift == 0) {
            return this;
        }
        double[] coordinates = this.coordinates.clone();
        coordinates[coordIndex] += shift;
        return new Point(coordinates);
    }

    /**
     * Returns the scalar product of this and given point.
     * The result is the sum of all products
     * <tt>thisInstance.{@link #coord(int) coord(i)}*point.{@link #coord(int) coord(i)}</tt>
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
    public strictfp double scalarProduct(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        if (point.coordCount() != coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordCount() + " instead of " + coordinates.length);
        double result = 0.0;
        for (int k = 0; k < coordinates.length; k++) {
            result += coordinates[k] * point.coordinates[k];
        }
        return result;
    }

    /**
     * Returns the symmetric point relatively the origin of coordinates.
     * Equivalent to {@link #multiply(double) multiply(-1.0)}.
     *
     * @return the symmetric point relatively the origin of coordinates.
     */
    public Point symmetric() {
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = -this.coordinates[k];
        }
        return new Point(coordinates);
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
    public Point projectionAlongAxis(int coordIndex) {
        coord(coordIndex); // check for illegal coordIndex
        if (this.coordinates.length == 1)
            throw new IllegalStateException("Cannot perform projection of 1-dimensional figures");
        double[] coordinates = new double[this.coordinates.length - 1];
        System.arraycopy(this.coordinates, 0, coordinates, 0, coordIndex);
        System.arraycopy(this.coordinates, coordIndex + 1, coordinates, coordIndex, coordinates.length - coordIndex);
        return new Point(coordinates);
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
    boolean projectionAlongAxisEquals(int coordIndex, Point point) {
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
    public int compareTo(Point o) {
        return compareTo(o, 0);
    }

    /**
     * Compares points lexicographically alike {@link #compareTo(Point)} method,
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
     * As in {@link #compareTo(Point)} method, we suppose here that
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
    public int compareTo(Point o, int firstCoordIndex) {
        int n = Math.max(coordinates.length, o.coordinates.length);
        if (firstCoordIndex < 0)
            throw new IllegalArgumentException("Negative firstCoordIndex argument");
        firstCoordIndex = firstCoordIndex % n;
        for (int k = n - 1; k >= 0; k--) {
            int index = k + firstCoordIndex;
            if (index >= n)
                index -= n;
            double thisCoord = index >= coordinates.length ? 0 : coordinates[index];
            double otherCoord = index >= o.coordinates.length ? 0 : o.coordinates[index];
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
    /*Repeat.IncludeEnd*/

    /**
     * Indicates whether some other point is equal to this instance,
     * that is the number of coordinates is the same and all corresponding coordinates are equal.
     * The corresponding coordinates are compared as in <tt>Double.equals</tt> method,
     * i.e. they are converted to <tt>long</tt> values by <tt>Double.doubleToLongBits</tt> method
     * and the results are compared.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <tt>true</tt> if the specified object is a point equal to this one.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Point)) {
            return false;
        }
        Point p = (Point) obj;
        if (p.coordinates.length != coordinates.length) {
            return false;
        }
        for (int k = 0; k < coordinates.length; k++) {
            if (Double.doubleToLongBits(p.coordinates[k]) != Double.doubleToLongBits(coordinates[k])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if this point is really integer, that is if
     * for any its coordinate <i>x</i><sub><i>i</i></sub>=<tt>{@link #coord(int) coord}(</tt><i>i</i><tt>)</tt>
     * the Java expression <i>x</i><sub><i>i</i></sub><tt>==(long)</tt><i>x</i><sub><i>i</i></sub> is <tt>true</tt>.
     *
     * @return <tt>true</tt> if all coordinates of this point are integer numbers.
     */
    public boolean isInteger() {
        for (double coordinate : coordinates) {
            if (coordinate != (long) coordinate) {
                return false;
            }
        }
        return true;
    }

    /**
     * Equivalent to <tt>{@link IPoint#valueOf(Point) IPoint.valueOf}(thisInstance)</tt>.
     *
     * @return the integer point with same (cast) coordinates.
     */
    public IPoint toIntegerPoint() {
        return IPoint.valueOf(this);
    }

    /**
     * Equivalent to <tt>{@link IPoint#roundOf(Point) IPoint.roundOf}(thisInstance)</tt>.
     *
     * @return the integer point with same (rounded) coordinates.
     */
    public IPoint toRoundedPoint() {
        return IPoint.roundOf(this);
    }

    private static void getBytes(double[] array, byte[] result) {
        for (int disp = 0, k = 0; k < array.length; k++) {
            long l = Double.doubleToLongBits(array[k]);
            int value = (int)l ^ 630591835; // provide different results than for other similar classes
            result[disp++] = (byte)value;
            result[disp++] = (byte)(value >>> 8);
            result[disp++] = (byte)(value >>> 16);
            result[disp++] = (byte)(value >>> 24);
            value = (int)(l >>> 32) ^ 928687429; // provide different results than for other similar classes
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
            Range[] ranges = {
                Range.valueOf(1.1, 2.7),
                Range.valueOf(1.6, 2.8),
                Range.valueOf(-27.1, 24.81),
                Range.valueOf(0, Long.MAX_VALUE),
                Range.valueOf(Long.MIN_VALUE, -100),
                Range.valueOf(Long.MIN_VALUE + 1, -100),
                Range.valueOf(Long.MIN_VALUE + 2000000, -100),
                Range.valueOf(Long.MIN_VALUE + 2000000, 100),
                Range.valueOf(-1e3, 1e8),
                Range.valueOf(-4e18, 6e18),
            };
            for (Range r : ranges) {
                IRange cast = null, rounded = null;
                try {
                    cast = r.toIntegerRange();
                    assert IRange.valueOf(r).equals(cast);
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
                    IRange ir = IRange.valueOf(r);
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
            Point[] points = {
                Point.valueOf(12, 3),
                Point.valueOf(1.2, 3, 1),
                Point.valueOf(12, 3, 0),
                Point.valueOf(1.2, 3, 0, 1.234),
                Point.valueOf(12, 3, 0, -21234),
                Point.valueOf(-12, 123453, 27182, 821234),
                Point.valueOf(14, -3),
                Point.valueOf(0),
                Point.valueOf(0, 0),
                Point.valueOf(-3, -2),
                Point.valueOf(-4, 5),
                Point.valueOf(15, 20),
                Point.valueOf(3, 1.33),
                Point.valueOf(4.1, 5),
                Point.valueOf(0, 0, 0),
                Point.origin(3),
                Point.valueOf(new double[18]),
                Point.valueOf(new double[18]),
                Point.valueOf(13, 0.0),
                Point.valueOf(13, -0.0),
                Point.valueOf(4413.1, 0.1),
                Point.valueOf(4413.2, -0.8),
                Point.valueOf(13, 1, 0),
                Point.valueOf(3, 4, 0),
                Point.valueOf(13),
                Point.valueOf(1e3, 30),
                Point.valueOf(1e3, 1e20),
                Point.valueOf(-5e18, 30),
                Point.valueOf(-5e18, -300),
                Point.valueOf(-7e18, -5e18),
                Point.valueOf(5e18, 1e20),
            };
            java.util.Arrays.sort(points);
            for (Point rp : points) {
                System.out.println(rp + "; symmetric: " + rp.symmetric()
                    + "; distance from origin: " + rp.distanceFromOrigin()
                    + " = " + rp.distanceFrom(Arrays.asList(Point.origin(rp.coordCount())))
                    + (rp.coordCount() > 1 && rp.projectionAlongAxisEquals(0, Point.origin(rp.coordCount() - 1)) ?
                    "; x-projection is origin" : "")
                    + "; x-shift: " + rp.shiftAlongAxis(0, 100.0)
                    + "; x-projection: "
                    + (rp.coordCount() == 1 ? "impossible" : rp.projectionAlongAxis(0))
                    + "; last-axis-projection: "
                    + (rp.coordCount() == 1 ? "impossible" : rp.projectionAlongAxis(rp.coordCount() - 1))
                    + "; *2: " + rp.multiply(2.0)
                    + " = " + rp.scale(IPoint.valueOfEqualCoordinates(rp.coordCount(), 2).toPoint().coordinates())
                    + " = " + rp.scaleAndShift(
                    Point.valueOfEqualCoordinates(rp.coordCount(), 2.0).coordinates(),
                    Point.origin(rp.coordCount()))
                    + "; sqr: " + rp.scalarProduct(rp)
                    + "; hash: " + rp.hashCode() + "; address: " + System.identityHashCode(rp));
            }
            System.out.println();
            for (int k = 0; k < points.length - 1; k += 2) {
                System.out.print(k + ": ");
                RectangularArea ra = null;
                try {
                    ra = RectangularArea.valueOf(points[k], points[k + 1]);
                    assert RectangularArea.valueOf(ra.ranges()).equals(ra);
                    Point point = Point.valueOfEqualCoordinates(ra.coordCount(), -1.5);
                    RectangularArea test = RectangularArea.valueOf(point, Point.origin(ra.coordCount()));
                    assert ra.intersects(test) ? ra.intersection(test).equals(RectangularArea.valueOf(
                        ra.min().max(test.min()), ra.max().min(test.max()))) :
                        ra.intersection(test) == null;
                    System.out.println(ra + "; ranges: " + java.util.Arrays.asList(ra.ranges())
                        + "; contains(origin): " + ra.contains(Point.origin(ra.coordCount()))
                        + "; expand(origin): " + ra.expand(Point.origin(ra.coordCount()))
                        + "; expand(-1,-1..2,2): " + ra.expand(RectangularArea.valueOf(
                        Point.valueOfEqualCoordinates(ra.coordCount(), -1),
                        Point.valueOfEqualCoordinates(ra.coordCount(), 2)))
                        + "; parallel distance to (-1.5,-1.5,...): "
                        + (ra.coordCount() == 2 ? ra.parallelDistance(-1.5, -1.5) : ra.coordCount() == 3 ?
                        ra.parallelDistance(-1.5, -1.5, -1.5) :
                        ra.parallelDistance(point))
                        + (ra.contains(point) ? " (inside)" : "")
                        + "; intersection with " + test + ": " + ra.intersection(test)
                        + "; subtracted " + test + ": " + ra.difference(new ArrayList<RectangularArea>(), test)
                        + "; hash: " + ra.hashCode());
                } catch (Exception e) {
                    System.out.println("  Cannot create area with " + points[k] + " and " + points[k + 1] + ": " + e);
                }
                if (ra == null) {
                    continue;
                }
                IRectangularArea cast = null, rounded = null;
                try {
                    cast = ra.toIntegerRectangularArea();
                    assert IRectangularArea.valueOf(ra).equals(cast);
                    System.out.println(ra + " casted to " + cast);
                } catch (Exception e) {
                    System.out.println("  Cannot call toIntegerRectangularArea for " + ra + ": " + e);
                }
                try {
                    rounded = ra.toRoundedRectangularArea();
                    assert IRectangularArea.roundOf(ra).equals(rounded);
                    IPoint point = IPoint.valueOfEqualCoordinates(ra.coordCount(), 10);
                    IRectangularArea test = IRectangularArea.valueOf(IPoint.origin(ra.coordCount()), point);
                    assert rounded.intersects(test) ? rounded.intersection(test).equals(IRectangularArea.valueOf(
                        rounded.min().max(test.min()), rounded.max().min(test.max()))) :
                        rounded.intersection(test) == null;
                    System.out.println(ra + " rounded to " + rounded
                        + "; parallel distance to (10,10,...): "
                        + (ra.coordCount() == 2 ? rounded.parallelDistance(10, 10) : ra.coordCount() == 3 ?
                        rounded.parallelDistance(10, 10, 10) :
                        rounded.parallelDistance(point))
                        + (rounded.contains(point) ? " (inside)" : "")
                        + "; intersection with " + test + ": " + rounded.intersection(test)
                        + "; subtracted " + test + ": " + rounded.difference(new ArrayList<IRectangularArea>(), test)
                        + "; hash: " + rounded.hashCode()
                    );
                } catch (Exception e) {
                    System.out.println("  Cannot call toRoundedRectangularArea for " + ra + ": " + e);
                }
                try {
                    IRectangularArea ira = IRectangularArea.valueOf(ra);
                    assert ira.equals(cast);
                    System.out.println(ra + " casted to " + ira);
                } catch (Exception e) {
                    System.out.println("  Cannot cast range " + ra + ": " + e);
                }
                try {
                    IRectangularArea ira = IRectangularArea.roundOf(ra);
                    assert ira.equals(rounded);
                    System.out.println(ra + " rounded to " + ira);
                } catch (Exception e) {
                    System.out.println("  Cannot round range " + ra + ": " + e);
                }
            }
        }
    }
}
