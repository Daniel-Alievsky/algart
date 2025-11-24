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

package net.algart.math;

import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * <p>Point in a multidimensional space with real coordinates.
 * Represented as an array of <code>double</code> numbers.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see IPoint
 */
public class Point implements Comparable<Point> {
    /*Repeat(INCLUDE_FROM_FILE, IPoint.java, begin)
      Long.MIN_VALUE ==> Double.NEGATIVE_INFINITY ;;
      Long.MAX_VALUE ==> Double.POSITIVE_INFINITY ;;
      IPoint ==> Point;;
      long ==> double   !! Auto-generated: NOT EDIT !! */
    private static final Point[] originsCache = new Point[16];

    static {
        for (int i = 1; i <= originsCache.length; i++) {
            originsCache[i - 1] = new Point(new double[i]) { // zero-filled by Java
                @Override
                public boolean isOrigin() {
                    return true;
                }
            };
        }
    }

    final double[] coordinates;

    Point(double[] coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Returns a new point with the given set of coordinates: <i>x</i>, <i>y</i>, <i>z</i>, ...
     * For example, <code>of(x,y)</code> returns the 2D point with given coordinates.
     *
     * <p>The <code>coordinates</code> array must contain at least 1 element.
     *
     * <p>The passed <code>coordinates</code> array is cloned by this method: no references to it
     * are maintained by the created object.
     *
     * @param coordinates cartesian coordinates of the point.
     * @return the point with the given coordinates.
     * @throws NullPointerException     if the passed array is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     */
    public static Point of(double... coordinates) {
        Objects.requireNonNull(coordinates, "Null coordinates argument");
        if (coordinates.length == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        if (coordinates.length <= originsCache.length) {
            boolean origin = true;
            for (double coord : coordinates) {
                if (coord != 0) {
                    origin = false;
                    break;
                }
            }
            if (origin) {
                return originsCache[coordinates.length - 1];
            }
        }
        return new Point(coordinates.clone());
    }

    @Deprecated
    public static Point valueOf(double... coordinates) {
        return of(coordinates);
    }

    /**
     * Returns a new 1-dimensional point with the given coordinate.
     *
     * @param x cartesian coordinate of the point.
     * @return point with the given coordinates.
     */
    public static Point of(double x) {
        return x == 0 ? originsCache[0] : new Point(new double[]{x});
    }

    @Deprecated
    public static Point valueOf(double x) {
        return of(x);
    }

    /**
     * Returns a new 2-dimensional point with the given coordinates.
     *
     * @param x cartesian x-coordinate of the point.
     * @param y cartesian y-coordinate of the point.
     * @return point with the given coordinates.
     */
    public static Point of(double x, double y) {
        return x == 0 && y == 0 ? originsCache[1] : new Point(new double[]{x, y});
    }

    @Deprecated
    public static Point valueOf(double x, double y) {
        return of(x, y);
    }

    /**
     * Returns a new 3-dimensional point with the given coordinates.
     *
     * @param x cartesian x-coordinate of the point.
     * @param y cartesian y-coordinate of the point.
     * @param z cartesian z-coordinate of the point.
     * @return point with the given coordinates.
     */
    public static Point of(double x, double y, double z) {
        return x == 0 && y == 0 && z == 0 ? originsCache[2] : new Point(new double[]{x, y, z});
    }

    @Deprecated
    public static Point valueOf(double x, double y, double z) {
        return of(x, y, z);
    }

    /**
     * Returns a new point in <i>n</i>-dimensional space, where <i>n</i><code>=coordCount</code>
     * and all coordinates of the point are equal to the given value <code>filler</code>.
     * For example, <code>ofEqualCoordinates(3, 1)</code> returns the 3D point (1,1,1).
     * If <code>filler==0</code>, this method is equivalent to {@link #origin(int) origin(coordCount)}.
     *
     * @param coordCount the number of dimensions.
     * @param filler     the value of each coordinate in the created point.
     * @return the point with equal coordinates.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point ofEqualCoordinates(int coordCount, double filler) {
        if (filler == 0) {
            return origin(coordCount);
        }
        if (coordCount <= 0) {
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        }
        double[] coordinates = new double[coordCount];
        Arrays.fill(coordinates, filler);
        return new Point(coordinates);
    }

    @Deprecated
    public static Point valueOfEqualCoordinates(int coordCount, double filler) {
        return ofEqualCoordinates(coordCount, filler);
    }

    /**
     * Returns the origin of coordinates in <i>n</i>-dimensional space,
     * where <i>n</i><code>=coordCount</code> is the argument of this method.
     *
     * @param coordCount the number of dimensions.
     * @return the origin of coordinates in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point origin(int coordCount) {
        if (coordCount <= 0) {
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        }
        if (coordCount <= originsCache.length) {
            return originsCache[coordCount - 1];
        } else {
            return new Point(new double[coordCount]); // zero-filled by Java
        }
    }

    /**
     * Returns the "minimal" point in <i>n</i>-dimensional space,
     * where <i>n</i><code>=coordCount</code> is the argument of this method,
     * that is the point with all coordinates are equal to <code>Double.NEGATIVE_INFINITY</code>.
     *
     * @param coordCount the number of dimensions.
     * @return the "minimal" point in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point minValue(int coordCount) {
        if (coordCount <= 0) {
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        }
        double[] coordinates = new double[coordCount];
        Arrays.fill(coordinates, Double.NEGATIVE_INFINITY);
        return new Point(coordinates);
    }

    /**
     * Returns the "maximal" point in <i>n</i>-dimensional space,
     * where <i>n</i><code>=coordCount</code> is the argument of this method,
     * that is the point with all coordinates are equal to <code>Double.POSITIVE_INFINITY</code>.
     *
     * @param coordCount the number of dimensions.
     * @return the "maximal" point in <i>n</i>-dimensional space.
     * @throws IllegalArgumentException if the passed number of dimensions is zero or negative.
     */
    public static Point maxValue(int coordCount) {
        if (coordCount <= 0) {
            throw new IllegalArgumentException("Negative or zero number of coordinates: " + coordCount);
        }
        double[] coordinates = new double[coordCount];
        Arrays.fill(coordinates, Double.POSITIVE_INFINITY);
        return new Point(coordinates);
    }
    /*Repeat.IncludeEnd*/

    /**
     * Returns a new point with the same coordinates as the given integer point.
     * All <code>long</code> coordinates of the passed integer point are converted
     * to <code>double</code> coordinates of the returned point by standard
     * Java typecast <code>(double)longValue</code>.
     *
     * @param iPoint the integer point.
     * @return the real point with the same coordinates.
     * @throws NullPointerException if the passed integer point is {@code null}.
     */
    public static Point of(IPoint iPoint) {
        Objects.requireNonNull(iPoint, "Null iPoint argument");
        double[] coordinates = new double[iPoint.coordCount()];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = iPoint.coord(k);
        }
        return new Point(coordinates);
    }

    @Deprecated
    public static Point valueOf(IPoint iPoint) {
        return of(iPoint);
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
     * Returns the number of dimensions.
     * Equivalent to <code>{@link #coordinates()}.length</code>, but works faster.
     *
     * <p>The result of this method is always positive (&gt;0).
     *
     * @return the number of dimensions.
     */
    public int coordCount() {
        return coordinates.length;
    }

    /**
     * Returns all coordinates of this point. The element <code>#0</code> of the returned array
     * is <i>x</i>-coordinate, the element <code>#1</code> is <i>y</i>-coordinate, etc.
     * The length of the returned array is the number of dimensions.
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
     * Copies all coordinates of this point into <code>result</code> array.
     * The element <code>#0</code> of this array will contain <i>x</i>-coordinate,
     * the element <code>#1</code> will contain <i>y</i>-coordinate, etc.
     * The length of the passed array must be not less than the number of the point dimensions.
     *
     * @param result the array where you want to store results.
     * @return a reference to the passed <code>result</code> array.
     * @throws NullPointerException     if <code>result</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>result.length&lt;{@link #coordCount()}</code>.
     */
    public double[] coordinates(double[] result) {
        if (result.length < coordinates.length) {
            throw new IllegalArgumentException("Too short result array: double[" + result.length
                    + "]; " + coordinates.length + " elements required to store coordinates");
        }
        System.arraycopy(coordinates, 0, result, 0, coordinates.length);
        return result;
    }

    /**
     * Returns the coordinate <code>#coordIndex</code>: <i>x</i>-coordinate for <code>coordIndex=0</code>,
     * <i>y</i>-coordinate for <code>coordIndex=1</code>, etc.
     *
     * @param coordIndex the index of the coordinate.
     * @return the coordinate.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
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
     * The only difference: in a case of 1-dimensional point (<code>{@link #coordCount()}&lt;2</code>),
     * this method throws <code>IllegalStateException</code> instead of <code>IndexOutOfBoundsException</code>.
     *
     * @return <i>y</i>-coordinate.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public double y() {
        if (coordinates.length < 2) {
            throw new IllegalStateException("Cannot get y-coordinate of " + coordinates.length + "-dimensional point");
        }
        return coordinates[1];
    }

    /**
     * Returns <i>z</i>-coordinate: equivalent to {@link #coord(int) coord(2)}.
     * The only difference: in a case of 1- or 2-dimensional point (<code>{@link #coordCount()}&lt;3</code>),
     * this method throws <code>IllegalStateException</code> instead of <code>IndexOutOfBoundsException</code>.
     *
     * @return <i>z</i>-coordinate.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;3</code>.
     */
    public double z() {
        if (coordinates.length < 3) {
            throw new IllegalStateException("Cannot get z-coordinate of " + coordinates.length + "-dimensional point");
        }
        return coordinates[2];
    }

    /**
     * Returns <code>true</code> if this point is the origin of coordinates.
     * In other words, returns <code>true</code> if all coordinates of this point are zero.
     *
     * @return <code>true</code> if this point is the origin of coordinates.
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
     * @return the distance between this point and the origin of coordinates.
     */
    public double distanceFromOrigin() {
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
     * If the passed collection is empty, returns <code>Double.POSITIVE_INFINITY</code>.
     *
     * @param points some collection of points.
     * @return the Hausdorff distance from this point to the set of points, passed via the collection.
     * @throws NullPointerException     if the argument is {@code null} or if some elements of the passed
     *                                  collection are {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this point and in some
     *                                  of the given points are different.
     */
    public double distanceFrom(Collection<Point> points) {
        Objects.requireNonNull(points, "Null points argument");
        double result = Double.POSITIVE_INFINITY;
        for (Point point : points) {
            if (point.coordCount() != coordinates.length) {
                throw new IllegalArgumentException("Dimensions count mismatch: some of the passed points has "
                        + point.coordCount() + " dimensions instead of " + coordinates.length);
            }
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
     * every coordinate <code>#i</code> in the result is
     * <code>thisInstance.{@link #coord(int) coord(i)}+point.{@link #coord(int) coord(i)}</code>.
     *
     * @param point the added point.
     * @return the vector sum of this and given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public Point add(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        if (point.coordCount() != coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordCount() + " instead of " + coordinates.length);
        }
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] + point.coordinates[k];
        }
        return new Point(coordinates);
    }

    /**
     * Returns the vector difference of this and given point:
     * every coordinate <code>#i</code> in the result is
     * <code>thisInstance.{@link #coord(int) coord(i)}-point.{@link #coord(int) coord(i)}</code>.
     *
     * @param point the subtracted point.
     * @return the vector difference of this and given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public Point subtract(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        if (point.coordCount() != coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordCount() + " instead of " + coordinates.length);
        }
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = this.coordinates[k] - point.coordinates[k];
        }
        return new Point(coordinates);
    }

    /**
     * Returns the coordinate-wise minimum of this and given point:
     * every coordinate <code>#i</code> in the result is
     * <code>Math.min(thisInstance.{@link #coord(int) coord(i)},point.{@link #coord(int) coord(i)})</code>.
     *
     * @param point the compared point.
     * @return the coordinate-wise minimum this and given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public Point min(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        if (point.coordCount() != coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordCount() + " instead of " + coordinates.length);
        }
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Math.min(this.coordinates[k], point.coordinates[k]);
        }
        return new Point(coordinates);
    }

    /**
     * Returns the coordinate-wise maximum of this and given point:
     * every coordinate <code>#i</code> in the result is
     * <code>Math.max(thisInstance.{@link #coord(int) coord(i)},point.{@link #coord(int) coord(i)})</code>.
     *
     * @param point the compared point.
     * @return the coordinate-wise maximum this and given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public Point max(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        if (point.coordCount() != coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordCount() + " instead of " + coordinates.length);
        }
        double[] coordinates = new double[this.coordinates.length];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = Math.max(this.coordinates[k], point.coordinates[k]);
        }
        return new Point(coordinates);
    }

    /**
     * Adds the given value to all coordinates of this point and returns the resulting point:
     * every coordinate <code>#i</code> in the result is
     * <code>thisInstance.{@link #coord(int) coord(i)}+increment</code>.
     * In other words, shifts this point along all axes by the given value.
     *
     * <p>Equivalent to <code>{@link #add(Point) add}({@link Point#ofEqualCoordinates(int, double)
     * Point.ofEqualCoordinates}(n,increment))</code>, where <code>n={@link #coordCount()}</code>.
     *
     * @param increment the value, which will be added to all coordinates of this point.
     * @return this resulting point.
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
     * Returns the product of this point and the given scalar <code>multiplier</code>:
     * every coordinate <code>#i</code> in the result is
     * <code>thisInstance.{@link #coord(int) coord(i)}*multiplier</code>.
     *
     * <p>Equivalent to {@link #scale(double... multipliers)}, where all {@link #coordCount()} arguments
     * of that method are equal to <code>multiplier</code>.
     *
     * @param multiplier the multiplier.
     * @return the product of this point and the given scalar <code>multiplier</code>.
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
     * Returns new point, each coordinate <code>#i</code> of which is
     * <code>thisInstance.{@link #coord(int) coord(i)}*multipliers[i]</code>.
     * The length of <code>multipliers</code> array must be equal to {@link #coordCount()}.
     *
     * <p>Note: this method does not perform actual multiplication to multipliers, equal to 1.0, 0.0 and &minus;1.0.
     * If the condition <code>multipliers[k]==1.0</code> is <code>true</code> for some <code>k</code>,
     * then the coordinate <code>#k</code> is just copied from this point into the result.
     * If the condition <code>multipliers[k]==0.0</code> is <code>true</code> for some <code>k</code>,
     * then the coordinate <code>#k</code> in the result will be <code>0.0</code> (always <code>+0.0</code>, regardless
     * of the sign of this coordinate in the source point).
     * If the condition <code>multipliers[k]==-1.0</code> is <code>true</code> for some <code>k</code>,
     * then the coordinate <code>#k</code> in the result will be equal to <code>-{@link #coord(int) coord}(k)</code>.
     *
     * @param multipliers the multipliers for all coordinates.
     * @return the point, each coordinate <code>#i</code> of which is product of the corresponding coordinate
     * <code>#i</code> of this one and the corresponding multiplier <code>multiplier[i]</code>.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is not equal to
     *                                  <code>multipliers.length</code>.
     */
    public Point scale(double... multipliers) {
        Objects.requireNonNull(multipliers, "Null multipliers argument");
        if (multipliers.length != coordinates.length) {
            throw new IllegalArgumentException("Illegal number of multipliers: "
                    + multipliers.length + " instead of " + coordinates.length);
        }
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
     * Returns new point, each coordinate <code>#i</code> of which is
     * <code>shift.{@link Point#coord(int) coord(i)}+thisInstance.{@link #coord(int) coord(i)}*multipliers[i]</code>.
     * The length of <code>multipliers</code> array must be equal to {@link #coordCount()}.
     *
     * <p>Note: this method does not perform actual multiplication to multipliers, equal to 1.0, 0.0 and &minus;1.0.
     * If the condition <code>multipliers[k]==1.0</code> is <code>true</code> for some <code>k</code>,
     * then the coordinate <code>#k</code> will be equal to
     * <code>shift.{@link Point#coord(int) coord}(k)+thisInstance.{@link #coord(int) coord}(k)</code>.
     * If the condition <code>multipliers[k]==0.0</code> is <code>true</code> for some <code>k</code>,
     * then the coordinate <code>#k</code> in the result will be equal to
     * <code>shift.{@link Point#coord(int) coord}(k)</code>.
     * If the condition <code>multipliers[k]==-1.0</code> is <code>true</code> for some <code>k</code>,
     * then the coordinate <code>#k</code> in the result will be equal to
     * <code>shift.{@link Point#coord(int) coord}(k)-thisInstance.{@link #coord(int) coord}(k)</code>.
     *
     * @param multipliers the multipliers for all coordinates.
     * @param shift       the shift along all coordinates.
     * @return the point, each coordinate <code>#i</code> of which is product of the corresponding coordinate
     * <code>#i</code> of this one and the corresponding multiplier <code>multiplier[i]</code>,
     * incremented by the corresponding coordinate
     * <code>shift.{@link Point#coord(int) coord(i)}</code>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is not equal to
     *                                  <code>multipliers.length</code>.
     */
    public Point scaleAndShift(double[] multipliers, Point shift) {
        double[] coordinates = new double[this.coordinates.length];
        scaleAndShift(coordinates, multipliers, shift);
        return new Point(coordinates);
    }

    /**
     * More efficient version of {@link #scaleAndShift(double[], Point)} method,
     * which stores the coordinates of the result in the specified Java array instead of creating new instance
     * of this class.
     * Equivalent to the following call:
     * <code>{@link #scaleAndShift(double[], Point) scaleAndShift}(multipliers,shift).{@link #coordinates(double[])
     * coordinates}(resultCoordinates)</code>, but works little faster.
     *
     * @param resultCoordinates Java array for storing results.
     * @param multipliers       the multipliers for all coordinates.
     * @param shift             the shift along all coordinates.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} is greater than
     *                                  <code>resultCoordinates.length</code> or is not equal to
     *                                  <code>multipliers.length</code>.
     */
    public void scaleAndShift(double[] resultCoordinates, double[] multipliers, Point shift) {
        if (resultCoordinates.length < coordinates.length) {
            throw new IllegalArgumentException("Too short result coordinates array: double["
                    + resultCoordinates.length + "]; " + coordinates.length
                    + " elements required to store coordinates");
        }
        Objects.requireNonNull(multipliers, "Null multipliers argument");
        if (multipliers.length != coordinates.length) {
            throw new IllegalArgumentException("Illegal number of multipliers: "
                    + multipliers.length + " instead of " + coordinates.length);
        }
        Objects.requireNonNull(shift, "Null shift argument");
        if (shift.coordCount() != coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + shift.coordCount() + " instead of " + coordinates.length);
        }
        for (int k = 0; k < coordinates.length; k++) {
            resultCoordinates[k] = multipliers[k] == 0.0 ? shift.coord(k) :
                    multipliers[k] == 1.0 ? shift.coord(k) + this.coordinates[k] :
                            multipliers[k] == -1.0 ? shift.coord(k) - this.coordinates[k] :
                                    shift.coord(k) + this.coordinates[k] * multipliers[k];
        }
    }

    /**
     * Returns this point shifted by the passed <code>shift</code> along the axis <code>#coordIndex</code>.
     * Equivalent to {@link #add(Point) add(p)}, where <code>p</code> is the point with coordinates
     * <i>p</i><sub>0</sub>, <i>p</i><sub>1</sub>, ...,
     * <i>p<sub>k</sub></i><code>=0</code> for <i>k</i><code>!=coordIndex</code>,
     * <i>p<sub>k</sub></i><code>=shift</code> for <i>k</i><code>=coordIndex</code>.
     *
     * @param coordIndex the index of the axis.
     * @param shift      the shift along this axis.
     * @return this point shifted along this axis.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or <code>coordIndex&gt;
     *                                   ={@link #coordCount()}</code>.
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
     * <code>thisInstance.{@link #coord(int) coord(i)}*point.{@link #coord(int) coord(i)}</code>
     * for all coordinate indexes <code>i</code>.
     *
     * @param point another point.
     * @return the scalar product of this and given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in this and given points
     *                                  are different.
     */
    public double scalarProduct(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        if (point.coordCount() != coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordCount() + " instead of " + coordinates.length);
        }
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
     * <i>P</i><code>.{@link #coord(int) coord}(</code><i>i</i><code>)=thisInstance.{@link #coord(int)
     * coord}(</code><i>i'</i><code>)</code>, <i>i'</i>=<i>i</i> for <i>i</i>&lt;<code>coordIndex</code> or
     * <i>i'</i>=<i>i</i>+1 for <i>i</i>&ge;<code>coordIndex</code>.
     *
     * @param coordIndex the number of coordinate, along which the projecting is performed.
     * @return the projection of this point along the coordinates axis <code>#coordIndex</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     * @throws IllegalStateException     if this point is 1-dimensional (<code>{@link #coordCount()}==1</code>).
     */
    public Point projectionAlongAxis(int coordIndex) {
        coord(coordIndex); // check for illegal coordIndex
        if (this.coordinates.length == 1) {
            throw new IllegalStateException("Cannot perform projection of 1-dimensional figures");
        }
        double[] coordinates = new double[this.coordinates.length - 1];
        System.arraycopy(this.coordinates, 0, coordinates, 0, coordIndex);
        System.arraycopy(this.coordinates, coordIndex + 1, coordinates, coordIndex, coordinates.length - coordIndex);
        return new Point(coordinates);
    }

    /**
     * Equivalent to <code>{@link #projectionAlongAxis(int) projectionAlongAxis}(coordIndex).equals(point)</code>,
     * but works faster (no Java objects are allocated).
     *
     * @param coordIndex the number of coordinate, along which the projecting is performed.
     * @param point      another point.
     * @return <code>true</code> if and only if the projection of this point along the given axis
     * is a point equal to the second argument.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>
     * @throws IllegalStateException     if this point is 1-dimensional (<code>{@link #coordCount()}==1</code>).
     * @throws IllegalArgumentException  if the {@link #coordCount() numbers of dimensions} of this point
     *                                   is not equal to <code>point.{@link #coordCount()}+1</code>.
     */
    boolean projectionAlongAxisEquals(int coordIndex, Point point) {
        // Right now I'm not sure that this method is really useful. Can be made "public" at any moment.
        coord(coordIndex); // check for illegal coordIndex
        if (this.coordinates.length == 1) {
            throw new IllegalStateException("Cannot perform projection of 1-dimensional figures");
        }
        if (this.coordinates.length != point.coordCount() + 1) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordCount() + " is not equal to " + coordinates.length + "-1");
        }

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
     * <i>x</i><sub><i>i</i></sub> is <code>thisInstance.{@link #coord(int) coord}(<i>i</i>)</code>
     * for <code>0&lt;=<i>i</i>&lt;thisInstance.{@link #coordCount()}</code> and
     * <i>x</i><sub><i>i</i></sub>=0 for <code><i>i</i>&gt;=thisInstance.{@link #coordCount()}</code>.
     * Then, let
     * <i>y</i><sub><i>i</i></sub> is <code>o.{@link #coord(int) coord}(<i>i</i>)</code>
     * for <code>0&lt;=<i>i</i>&lt;o.{@link #coordCount()}</code> and
     * <i>y</i><sub><i>i</i></sub>=0 for <code><i>i</i>&gt;=o.{@link #coordCount()}</code>.
     * This method returns a negative integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&lt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>;
     * this method returns a positive integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&gt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>.
     * If all <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub>,
     * this method returns a negative integer, 0 or a positive integer if
     * <code>thisInstance.{@link #coordCount()}</code> is less than, equal to, or greater than
     * <code>o.{@link #coordCount()}</code>.
     *
     * @param o the point to be compared.
     * @return negative integer, zero, or a positive integer as this point
     * is lexicographically less than, equal to, or greater than <code>o</code>.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public int compareTo(Point o) {
        return compareTo(o, 0);
    }

    /**
     * Compares points lexicographically alike {@link #compareTo(Point)} method,
     * but with the cyclical shift of all indexes of coordinates:
     * the coordinate <code>#firstCoordIndex</code> instead of <i>x</i>,
     * <code>#firstCoordIndex+1</code> instead of <i>y</i>, etc.
     *
     * <p>More precisely, let
     * <i>n</i><code>=max(thisInstance.{@link #coordCount()},o.{@link #coordCount()})</code>,
     * <i>x</i><sub><i>i</i></sub> is <code>thisInstance.{@link #coord(int)
     * coord}((<i>i</i>+firstCoordIndex)%<i>n</i>)</code>,
     * <i>y</i><sub><i>i</i></sub> is <code>o.{@link #coord(int)
     * coord}((<i>i</i>+firstCoordIndex)%<i>n</i>)</code>.
     * As in {@link #compareTo(Point)} method, we suppose here that
     * all coordinates {@link #coord(int) coord(<i>k</i>)} with <code><i>k</i>&gt;={@link #coordCount()}</code>
     * are zero.
     * This method returns a negative integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&lt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>;
     * this method returns a positive integer if there is such index <i>k</i> that
     * <i>x</i><sub><i>k</i></sub>&gt;<i>y</i><sub><i>k</i></sub>
     * and <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub> for all <i>i</i>&gt;<i>k</i>.
     * If all <i>x</i><sub><i>i</i></sub>=<i>y</i><sub><i>i</i></sub>,
     * this method returns a negative integer, 0 or a positive integer if
     * <code>thisInstance.{@link #coordCount()}</code> is less than, equal to, or greater than
     * <code>o.{@link #coordCount()}</code>.
     *
     * @param o               the point to be compared.
     * @param firstCoordIndex the index of "first" coordinate, that is compared after all other coordinates.
     * @return negative integer, zero, or a positive integer as this point
     * is lexicographically less than, equal to, or greater than <code>o</code>.
     * @throws NullPointerException     if the <code>o</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>firstCoordIndex</code> is negative.
     */
    public int compareTo(Point o, int firstCoordIndex) {
        int n = Math.max(coordinates.length, o.coordinates.length);
        if (firstCoordIndex < 0) {
            throw new IllegalArgumentException("Negative firstCoordIndex argument");
        }
        firstCoordIndex = firstCoordIndex % n;
        for (int k = n - 1; k >= 0; k--) {
            int index = k + firstCoordIndex;
            if (index >= n) {
                index -= n;
            }
            double thisCoord = index >= coordinates.length ? 0 : coordinates[index];
            double otherCoord = index >= o.coordinates.length ? 0 : o.coordinates[index];
            if (thisCoord > otherCoord) {
                return 1;
            }
            if (thisCoord < otherCoord) {
                return -1;
            }
        }
        return Integer.compare(coordinates.length, o.coordinates.length);
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
        final StringBuilder result = new StringBuilder("(");
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
        return (int) sum.getValue() ^ 1839581;
    }
    /*Repeat.IncludeEnd*/

    /**
     * Indicates whether some other point is equal to this instance,
     * that is the number of coordinates is the same and all corresponding coordinates are equal.
     * The corresponding coordinates are compared as in <code>Double.equals</code> method,
     * i.e. they are converted to <code>long</code> values by <code>Double.doubleToLongBits</code> method
     * and the results are compared.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return <code>true</code> if the specified object is a point equal to this one.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Point p)) {
            return false;
        }
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
     * Returns <code>true</code> if and only if this point is really integer, that is if
     * for any of its coordinate
     * <i>x</i><sub><i>i</i></sub>=<code>{@link #coord(int) coord}(</code><i>i</i><code>)</code>
     * the Java expression <i>x</i><sub><i>i</i></sub><code>==(long)</code><i>x</i><sub><i>i</i></sub>
     * is <code>true</code>.
     *
     * @return <code>true</code> if all coordinates of this point are integer numbers.
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
     * Equivalent to <code>{@link IPoint#of(Point) IPoint.of}(thisInstance)</code>.
     *
     * @return the integer point with the same (cast) coordinates.
     */
    public IPoint toIntegerPoint() {
        return IPoint.of(this);
    }

    /**
     * Equivalent to <code>{@link IPoint#roundOf(Point) IPoint.roundOf}(thisInstance)</code>.
     *
     * @return the integer point with the same (rounded) coordinates.
     */
    public IPoint toRoundedPoint() {
        return IPoint.roundOf(this);
    }

    private static void getBytes(double[] array, byte[] result) {
        for (int disp = 0, k = 0; k < array.length; k++) {
            long l = Double.doubleToLongBits(array[k]);
            int value = (int) l ^ 630591835; // provide different results than for other similar classes
            result[disp++] = (byte) value;
            result[disp++] = (byte) (value >>> 8);
            result[disp++] = (byte) (value >>> 16);
            result[disp++] = (byte) (value >>> 24);
            value = (int) (l >>> 32) ^ 928687429; // provide different results than for other similar classes
            result[disp++] = (byte) value;
            result[disp++] = (byte) (value >>> 8);
            result[disp++] = (byte) (value >>> 16);
            result[disp++] = (byte) (value >>> 24);
        }
    }
}
