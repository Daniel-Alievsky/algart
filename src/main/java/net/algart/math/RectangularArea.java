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

/**
 * <p>Rectangular real area, i&#46;e&#46;
 * hyper-parallelepiped in multidimensional space with real coordinates of vertices.
 * All edges of the hyper-parallelepiped are parallel to coordinate axes.
 * In 1-dimensional case it is an equivalent of {@link Range} class,
 * in 2-dimensional case it is an analog of the standard <code>java.awt.geom.Rectangle2D</code> class.</p>
 *
 * <p>More precisely, the region, specified by this class, is defined by two <i>n</i>-dimensional points
 * with real coordinates ({@link Point}),
 * named the <i>minimal vertex</i> <b>min</b> and <i>maximal vertex</i> <b>max</b>,
 * and consists of all such points
 * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>), that:</p>
 *
 * <blockquote>
 * <b>min</b>.{@link Point#coord(int) coord(0)} &le; <i>x</i><sub>0</sub> &le;
 * <b>max</b>.{@link Point#coord(int) coord(0)},<br>
 * <b>min</b>.{@link Point#coord(int) coord(1)} &le; <i>x</i><sub>1</sub> &le;
 * <b>max</b>.{@link Point#coord(int) coord(1)},<br>
 * ...,<br>
 * <b>min</b>.{@link Point#coord(int) coord(<i>n</i>-1)} &le; <i>x</i><sub><i>n</i>&minus;1</sub> &le;
 * <b>max</b>.{@link Point#coord(int) coord(<i>n</i>-1)}.
 * </blockquote>
 *
 * <p>The <b>min</b> and <b>max</b> points are specified while creating an instance of this class
 * and can be retrieved by {@link #min()} and {@link #max()} methods.</p>
 *
 * <p>The coordinates of the minimal vertex
 * <b>min</b>.{@link Point#coord(int) coord(<i>i</i>)}
 * are never greater than the corresponding coordinates of the maximal vertex
 * <b>max</b>.{@link Point#coord(int) coord(<i>i</i>)},
 * and all coordinates of both vertices are never <code>Double.NaN</code>.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see IRectangularArea
 */
public class RectangularArea {

    final Point min;
    final Point max;

    RectangularArea(Point min, Point max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns an instance of this class with the given minimal vertex <b>min</b> and
     * maximal vertex <b>max</b>.
     * See the {@link RectangularArea comments to this class} for more details.
     *
     * @param min the minimal vertex, inclusive.
     * @param max the maximal vertex, inclusive.
     * @return the new rectangular area "between" these vertices.
     * @throws NullPointerException     if one of arguments is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in <code>min</code>
     *                                  and <code>max</code> points are different,
     *                                  or if, for some <i>i</i>,
     *                                  <code>min.{@link Point#coord(int) coord}(<i>i</i>)
     *                                  &gt; max.{@link Point#coord(int) coord}(<i>i</i>)</code>,
     *                                  or if one of these coordinates is <code>Double.NaN</code>.
     */
    public static RectangularArea of(Point min, Point max) {
        Objects.requireNonNull(min, "Null min vertex");
        Objects.requireNonNull(max, "Null max vertex");
        int n = min.coordinates.length;
        if (n != max.coordinates.length) {
            throw new IllegalArgumentException("min.coordCount() = " + n
                    + " does not match max.coordCount() = " + max.coordinates.length);
        }
        for (int k = 0; k < n; k++) {
            if (Double.isNaN(min.coordinates[k])) {
                throw new IllegalArgumentException("min.coord(" + k + ") is NaN");
            }
            if (Double.isNaN(max.coordinates[k])) {
                throw new IllegalArgumentException("max.coord(" + k + ") is NaN");
            }
            if (min.coordinates[k] > max.coordinates[k]) {
                throw new IllegalArgumentException("min.coord(" + k + ") > max.coord(" + k + ")"
                        + " (min = " + min + ", max = " + max + ")");
            }
        }
        return new RectangularArea(min, max);
    }

    @Deprecated
    public static RectangularArea valueOf(Point min, Point max) {
        return of(min, max);
    }

    /**
     * Returns the Cartesian product of the specified coordinate ranges.
     * More precisely, return an <i>n</i>-dimensional {@link RectangularArea rectangular area}
     * with the minimal vertex <b>min</b> and maximal vertex <b>max</b>, where
     * <i>n</i><code>=coordRanges.length</code>,
     * <b>min</b>.{@link Point#coord(int)
     * coord(<i>i</i>)}<code>=coordRanges[<i>i</i>].{@link Range#min() min()}</code>,
     * <b>max</b>.{@link Point#coord(int)
     * coord(<i>i</i>)}<code>=coordRanges[<i>i</i>].{@link Range#max() max()}</code>.
     * See the {@link RectangularArea comments to this class} for more details.
     *
     * @param coordRanges the coordinate ranges.
     * @return the Cartesian product of the specified coordinate ranges.
     * @throws NullPointerException     if the argument is {@code null}
     *                                  or if one of specified <code>coordRanges</code> is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no ranges are passed).
     */
    public static RectangularArea of(Range... coordRanges) {
        Objects.requireNonNull(coordRanges, "Null coordRanges argument");
        int n = coordRanges.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty coordRanges array");
        }
        coordRanges = coordRanges.clone();
        // cloning before checking guarantees correct check while multithreading
        for (int k = 0; k < n; k++) {
            Objects.requireNonNull(coordRanges[k], "Null coordRanges[" + k + "]");
        }
        double[] min = new double[n];
        double[] max = new double[n];
        for (int k = 0; k < n; k++) {
            min[k] = coordRanges[k].min;
            max[k] = coordRanges[k].max;
        }
        return new RectangularArea(new Point(min), new Point(max));
    }

    @Deprecated
    public static RectangularArea valueOf(Range... coordRanges) {
        return of(coordRanges);
    }

    /**
     * Returns a 1-dimensional rectangular area (range) with the given minimal vertex and size.
     * Equivalent to
     * <pre>
     * {@link #of(double, double) of}(
     *      minX,
     *      minX + sizeX);
     * </pre>
     *
     * <p>with the only exception that this method ensures that the provided size is non-negative.</p>
     *
     * @param minX  the minimal <i>x</i>-coordinate, inclusive.
     * @param sizeX the size along the <i>x</i>-axis; must be non-negative.
     * @return the new 1-dimensional rectangular area.
     * @throws IllegalArgumentException if <code>sizeX</code> is negative,
     *                                  or in the same situations as {@link #of(Point, Point)} method.
     */
    public static RectangularArea ofSize(double minX, double sizeX) {
        if (sizeX < 0.0) {
            throw new IllegalArgumentException("Negative sizeX: " + sizeX);
        }
        if (Double.isNaN(sizeX)) {
            throw new IllegalArgumentException("sizeX is NaN");
        }
        return of(minX, minX + sizeX);
    }

    /**
     * Returns a 1-dimensional rectangular area (range) with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #of(Point, Point) of}(
     *      {@link Point#of(double) Point.of}(minX),
     *      {@link Point#of(double) Point.of}(maxX));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @return the new 1-dimensional rectangular area.
     * @throws IllegalArgumentException in the same situations as {@link #of(Point, Point)} method.
     */
    public static RectangularArea of(double minX, double maxX) {
        return of(Point.of(minX), Point.of(maxX));
    }

    @Deprecated
    public static RectangularArea valueOf(double minX, double maxX) {
        return of(minX, maxX);
    }

    /**
     * Returns a 2-dimensional rectangle with the given minimal vertex and sizes along each axis.
     * Equivalent to
     * <pre>
     * {@link #of(double, double, double, double) of}(
     *      minX,
     *      minY,
     *      minX + sizeX,
     *      minY + sizeY);
     * </pre>
     *
     * <p>with the only exception that this method ensures that the provided sizes are non-negative.</p>
     *
     * @param minX  the minimal <i>x</i>-coordinate, inclusive.
     * @param minY  the minimal <i>y</i>-coordinate, inclusive.
     * @param sizeX the size along the <i>x</i>-axis; must be non-negative.
     * @param sizeY the size along the <i>y</i>-axis; must be non-negative.
     * @return the new 2-dimensional rectangle.
     * @throws IllegalArgumentException if <code>sizeX</code> or <code>sizeY</code> are negative,
     *                                  or in the same situations as {@link #of(Point, Point)} method.
     */
    public static RectangularArea ofSize(double minX, double minY, double sizeX, double sizeY) {
        if (sizeX < 0.0) {
            throw new IllegalArgumentException("Negative sizeX: " + sizeX);
        }
        if (sizeY < 0.0) {
            throw new IllegalArgumentException("Negative sizeY: " + sizeY);
        }
        if (Double.isNaN(sizeX)) {
            throw new IllegalArgumentException("sizeX is NaN");
        }
        if (Double.isNaN(sizeY)) {
            throw new IllegalArgumentException("sizeY is NaN");
        }
        return of(minX, minY, minX + sizeX, minY + sizeY);
    }

    /**
     * Returns a 2-dimensional rectangle with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #of(Point, Point) of}(
     *      {@link Point#of(double...) Point.of}(minX, minY),
     *      {@link Point#of(double...) Point.of}(maxX, maxY));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param minY the minimal <i>y</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @param maxY the maximal <i>y</i>-coordinate, inclusive.
     * @return the new 2-dimensional rectangle.
     * @throws IllegalArgumentException in the same situations as {@link #of(Point, Point)} method.
     */
    public static RectangularArea of(double minX, double minY, double maxX, double maxY) {
        return of(Point.of(minX, minY), Point.of(maxX, maxY));
    }

    @Deprecated
    public static RectangularArea valueOf(double minX, double minY, double maxX, double maxY) {
        return of(minX, minY, maxX, maxY);
    }

    /**
     * Returns a 3-dimensional parallelepiped with the given minimal vertex and sizes along each axis.
     * Equivalent to
     * <pre>
     * {@link #of(double, double, double, double, double, double) of}(
     *      minX,
     *      minY,
     *      minZ,
     *      minX + sizeX,
     *      minY + sizeY,
     *      minZ + sizeZ);
     * </pre>
     *
     * <p>with the only exception that this method ensures that the provided sizes are non-negative.</p>
     *
     * @param minX  the minimal <i>x</i>-coordinate, inclusive.
     * @param minY  the minimal <i>y</i>-coordinate, inclusive.
     * @param minZ  the minimal <i>z</i>-coordinate, inclusive.
     * @param sizeX the size along the <i>x</i>-axis; must be non-negative.
     * @param sizeY the size along the <i>y</i>-axis; must be non-negative.
     * @param sizeZ the size along the <i>z</i>-axis; must be non-negative.
     * @return the new 3-dimensional parallelepiped.
     * @throws IllegalArgumentException if <code>sizeX</code>, <code>sizeY</code> or <code>sizeZ</code> are negative,
     *                                  or in the same situations as {@link #of(Point, Point)} method.
     */
    public static RectangularArea ofSize(
            double minX,
            double minY,
            double minZ,
            double sizeX,
            double sizeY,
            double sizeZ) {
        if (sizeX < 0.0) {
            throw new IllegalArgumentException("Negative sizeX: " + sizeX);
        }
        if (sizeY < 0.0) {
            throw new IllegalArgumentException("Negative sizeY: " + sizeY);
        }
        if (sizeZ < 0.0) {
            throw new IllegalArgumentException("Negative sizeZ: " + sizeZ);
        }
        if (Double.isNaN(sizeX)) {
            throw new IllegalArgumentException("sizeX is NaN");
        }
        if (Double.isNaN(sizeY)) {
            throw new IllegalArgumentException("sizeY is NaN");
        }
        if (Double.isNaN(sizeZ)) {
            throw new IllegalArgumentException("sizeZ is NaN");
        }
        return of(minX, minY, minZ, minX + sizeX, minY + sizeY, minZ + sizeZ);
    }

    /**
     * Returns a 3-dimensional parallelepiped with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #of(Point, Point) of}(
     *      {@link Point#of(double...) Point.of}(minX, minY, minZ),
     *      {@link Point#of(double...) Point.of}(maxX, maxY, maxZ));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param minY the minimal <i>y</i>-coordinate, inclusive.
     * @param minZ the minimal <i>z</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @param maxY the maximal <i>y</i>-coordinate, inclusive.
     * @param maxZ the maximal <i>z</i>-coordinate, inclusive.
     * @return the new 3-dimensional parallelepiped.
     * @throws IllegalArgumentException in the same situations as {@link #of(Point, Point)} method.
     */
    public static RectangularArea of(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return of(Point.of(minX, minY, minZ), Point.of(maxX, maxY, maxZ));
    }

    @Deprecated
    public static RectangularArea valueOf(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        return of(minX, minY, minZ, maxX, maxY, maxZ);
    }
    /**
     * Returns a new rectangular area with the same coordinates as the given area.
     * All <code>long</code> coordinates of the passed area are converted
     * to <code>double</code> coordinates of the returned area by standard
     * Java typecast <code>(double)longValue</code>.
     * Equivalent to <code>{@link #of(Point, Point) of}({@link Point#of(IPoint)
     * Point.of}(iArea.{@link #min() min()}),&nbsp;{@link Point#of(IPoint)
     * Point.of}(iArea.{@link #max() max()}))</code>.
     *
     * @param iArea the integer rectangular area.
     * @return the real rectangular area with the same coordinates.
     * @throws NullPointerException if the passed area is {@code null}.
     * @see IRectangularArea#toRectangularArea()
     */
    public static RectangularArea of(IRectangularArea iArea) {
        Objects.requireNonNull(iArea, "Null iArea argument");
        return new RectangularArea(Point.of(iArea.min), Point.of(iArea.max));
        // integer min and max, converted to real, are always acceptable for of(Point min, Point max) method
    }

    @Deprecated
    public static RectangularArea valueOf(IRectangularArea iArea) {
        return of(iArea);
    }

    /**
     * Returns the number of dimensions.
     * Equivalent to <code>{@link #min()}.{@link Point#coordCount() coordCount()}</code>
     * or <code>{@link #max()}.{@link Point#coordCount() coordCount()}</code>, but works faster.
     *
     * <p>The result of this method is always positive (&gt;0).
     *
     * @return the number of dimensions.
     */
    public int coordCount() {
        return min.coordinates.length;
    }

    /**
     * Returns the minimal vertex of this rectangular area:
     * the point with minimal coordinates, belonging to this area.
     * See the {@link RectangularArea comments to this class} for more details.
     *
     * @return the minimal vertex of this rectangular area.
     */
    public Point min() {
        return min;
    }

    /**
     * Returns the maximal vertex of this rectangular area:
     * the point with maximal coordinates, belonging to this area.
     * See the {@link RectangularArea comments to this class} for more details.
     *
     * @return the maximal vertex of this rectangular area.
     */
    public Point max() {
        return max;
    }

    /**
     * Returns all sizes of this rectangular area in a form of {@link Point}.
     * Equivalent to <code>{@link Point#of(double...) Point.of}({@link #sizes()})</code>.
     * The returned point is equal to
     * <code>{@link #max()}.{@link Point#subtract(Point) subtract}({@link #min()})</code>.
     *
     * @return all sizes of this rectangular area in a form of {@link Point}.
     */
    public Point size() {
        return new Point(sizes());
    }

    /**
     * Returns <code>{@link #min()}.{@link Point#coord(int) coord}(coordIndex)</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #min()}.{@link Point#coord(int) coord}(coordIndex)</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public double min(int coordIndex) {
        return min.coordinates[coordIndex];
    }

    /**
     * Returns <code>{@link #max()}.{@link Point#coord(int) coord}(coordIndex)</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #max()}.{@link Point#coord(int) coord}(coordIndex)</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public double max(int coordIndex) {
        return max.coordinates[coordIndex];
    }

    /**
     * Returns <code>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public double size(int coordIndex) {
        return max.coordinates[coordIndex] - min.coordinates[coordIndex];
    }

    /**
     * Returns <code>{@link #min()}.{@link IPoint#x() x()}</code>.
     *
     * @return <code>{@link #min()}.{@link IPoint#x() x()}</code>.
     */
    public double minX() {
        return min.coordinates[0];
    }

    /**
     * Returns <code>{@link #max()}.{@link IPoint#x() x()}</code>.
     *
     * @return <code>{@link #max()}.{@link IPoint#x() x()}</code>.
     */
    public double maxX() {
        return max.coordinates[0];
    }

    /**
     * Returns <code>{@link #maxX() maxX()} - {@link #minX() minX()}</code>.
     *
     * @return <code>{@link #maxX() maxX()} - {@link #minX() minX()}</code>.
     */
    public double sizeX() {
        return max.coordinates[0] - min.coordinates[0];
    }

    /**
     * Returns <code>{@link #min()}.{@link IPoint#y() y()}</code>.
     *
     * @return <code>{@link #min()}.{@link IPoint#y() y()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public double minY() {
        if (min.coordinates.length < 2) {
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        }
        return min.coordinates[1];
    }

    /**
     * Returns <code>{@link #max()}.{@link IPoint#y() y()}</code>.
     *
     * @return <code>{@link #max()}.{@link IPoint#y() y()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public double maxY() {
        if (min.coordinates.length < 2) {
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[1];
    }

    /**
     * Returns <code>{@link #maxY() maxY()} - {@link #minY() minY()}</code>.
     *
     * @return <code>{@link #maxY() maxY()} - {@link #minY() minY()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public double sizeY() {
        if (min.coordinates.length < 2) {
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[1] - min.coordinates[1];
    }

    /**
     * Returns <code>{@link #min()}.{@link IPoint#z() z()}</code>.
     *
     * @return <code>{@link #min()}.{@link IPoint#z() z()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;3</code>.
     */
    public double minZ() {
        if (min.coordinates.length < 3) {
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        }
        return min.coordinates[2];
    }

    /**
     * Returns <code>{@link #max()}.{@link IPoint#z() z()}</code>.
     *
     * @return <code>{@link #max()}.{@link IPoint#z() z()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;3</code>.
     */
    public double maxZ() {
        if (min.coordinates.length < 3) {
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[2];
    }

    /**
     * Returns <code>{@link #maxZ() maxZ()} - {@link #minZ() minZ()}</code>.
     *
     * @return <code>{@link #maxZ() maxZ()} - {@link #minZ() minZ()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;3</code>.
     */
    public double sizeZ() {
        if (min.coordinates.length < 3) {
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[2] - min.coordinates[2];
    }

    /**
     * Returns the sizes of this rectangular area along all dimensions.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <code>#k</code> contains <code>{@link #size(int) size}(k)</code>.
     *
     * @return the sizes of this rectangular area along all dimensions.
     */
    public double[] sizes() {
        double[] sizes = new double[min.coordinates.length];
        for (int k = 0; k < sizes.length; k++) {
            sizes[k] = max.coordinates[k] - min.coordinates[k];
        }
        return sizes;
    }

    /**
     * Returns the volume of this rectangular area: the product of all sizes
     * returned by {@link #sizes()} method.
     *
     * @return the multidimensional volume of this rectangular area (usual area in 2-dimensional case).
     */
    public double volume() {
        double result = max.coordinates[0] - min.coordinates[0];
        for (int k = 1; k < min.coordinates.length; k++) {
            result *= max.coordinates[k] - min.coordinates[k];
        }
        return result;
    }

    /**
     * Returns <code>{@link Range}.{@link Range#of(double, double)
     * of}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link Range}.{@link Range#of(double, double)
     * of}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</code>.
     */
    public Range range(int coordIndex) {
        return new Range(min.coordinates[coordIndex], max.coordinates[coordIndex]);
    }

    /**
     * Returns the projections of this rectangular area to all axes.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <code>#k</code> contains <code>{@link #range(int) range}(k)</code>.
     *
     * @return the projections of this rectangular area to all axes.
     */
    public Range[] ranges() {
        Range[] ranges = new Range[min.coordinates.length];
        for (int k = 0; k < ranges.length; k++) {
            ranges[k] = Range.of(min.coordinates[k], max.coordinates[k]);
        }
        return ranges;
    }

    /**
     * Returns <code>true</code> if and only if
     * <code>{@link #min(int) min}(k)&lt;=point.{@link Point#coord(int) coord}(k)&lt;={@link #max(int) max}(k)</code>
     * for all <i>k</i>.
     *
     * @param point the checked point.
     * @return <code>true</code> if this rectangular area contains the given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link Point#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean contains(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        int n = min.coordinates.length;
        if (point.coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + point.coordinates.length + " instead of " + n);
        }
        for (int k = 0; k < n; k++) {
            if (point.coordinates[k] < min.coordinates[k] || point.coordinates[k] > max.coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>true</code> if at least one of the specified <code>areas</code> contains
     * the passed <code>point</code>
     * (see {@link #contains(Point)} method).
     *
     * @param areas list of checked rectangular areas.
     * @param point the checked point.
     * @return <code>true</code> if one of the passed areas contains the given point.
     * @throws NullPointerException     if one of the arguments or one of the areas is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link Point#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas.
     */
    public static boolean contains(Collection<RectangularArea> areas, Point point) {
        Objects.requireNonNull(areas, "Null areas");
        Objects.requireNonNull(point, "Null point");
        for (RectangularArea a : areas) {
            if (a.contains(point)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if and only if
     * <code>{@link #min(int) min}(k)&lt;=area.{@link #min(int) min}(k)</code>
     * and <code>area.{@link #max(int) max}(k)&lt;={@link #max(int) max}(k)</code>
     * for all <i>k</i>.
     *
     * @param area the checked rectangular area.
     * @return <code>true</code> if the checked rectangular area is a subset of this area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean contains(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + area.min.coordinates.length + " instead of " + n);
        }
        for (int k = 0; k < n; k++) {
            if (area.min.coordinates[k] < min.coordinates[k] || area.max.coordinates[k] > max.coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>true</code> if at least one of the specified <code>areas</code> contains
     * the passed <code>area</code>
     * (see {@link #contains(RectangularArea)} method).
     *
     * @param areas list of checked rectangular areas.
     * @param area  the checked area.
     * @return <code>true</code> if one of the passed areas (1st argument) contains the given area (2nd argument).
     * @throws NullPointerException     if one of the arguments or one of the areas is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas
     *                                  in the 1st argument.
     */
    public static boolean contains(Collection<RectangularArea> areas, RectangularArea area) {
        Objects.requireNonNull(areas, "Null areas argument");
        Objects.requireNonNull(area, "Null area argument");
        for (RectangularArea a : areas) {
            if (a.contains(area)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if and only if
     * <code>{@link #min(int) min}(k)&lt;=area.{@link #max(int) max}(k)</code>
     * and <code>area.{@link #min(int) min}(k)&lt;={@link #max(int) max}(k)</code>
     * for all <i>k</i>.
     *
     * @param area the checked rectangular area.
     * @return <code>true</code> if the checked rectangular area overlaps with this area,
     * maybe in boundary points only.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean intersects(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + area.min.coordinates.length + " instead of " + n);
        }
        for (int k = 0; k < n; k++) {
            if (area.max.coordinates[k] < min.coordinates[k] || area.min.coordinates[k] > max.coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>true</code> if and only if
     * <code>{@link #min(int) min}(k)&lt;area.{@link #max(int) max}(k)</code>
     * and <code>area.{@link #min(int) min}(k)&lt;{@link #max(int) max}(k)</code>
     * for all <i>k</i>.
     *
     * @param area the checked rectangular area.
     * @return <code>true</code> if the checked rectangular area overlaps with this area in some internal points.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean overlaps(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + area.min.coordinates.length + " instead of " + n);
        }
        for (int k = 0; k < n; k++) {
            if (area.max.coordinates[k] <= min.coordinates[k] || area.min.coordinates[k] >= max.coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /*Repeat(INCLUDE_FROM_FILE, IRectangularArea.java, operationsAndParallelDistance)
      Long.MIN_VALUE ==> Double.NEGATIVE_INFINITY ;;
      Long.MAX_VALUE ==> Double.POSITIVE_INFINITY ;;
      IRectangular ==> Rectangular;;
      IPoint ==> Point;;
      long ==> double;;
      saveMin\s*-\s*1 ==> saveMin;;
      saveMax\s*\+\s*1 ==> saveMax;;
      (\[k\])\s*[+-]\s*1 ==> $1 !! Auto-generated: NOT EDIT !! */

    /**
     * Returns the set-theoretical intersection <b>A</b>&nbsp;&cap;&nbsp;<b>B</b> of this (<b>A</b>) and
     * the passed rectangular area (<b>B</b>) or {@code null} if they
     * do not {@link #intersects(RectangularArea) intersect}
     * (<b>A</b>&nbsp;&cap;&nbsp;<b>B</b>&nbsp;=&nbsp;&empty;).
     * Equivalent to
     * <pre>thisInstance.{@link #intersects(RectangularArea) intersects}(area) ? {@link #of(Point, Point)
     * RectangularArea.of}(
     * thisInstance.{@link #min()}.{@link Point#max(Point) max}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link Point#min(Point) min}(area.{@link #max()})) :
     * null</pre>.
     *
     * @param area the second rectangular area.
     * @return intersection of this and the second rectangular area or {@code null} if they do not intersect.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea intersection(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + area.min.coordinates.length + " instead of " + n);
        }
        double[] newMin = new double[n];
        double[] newMax = new double[n];
        for (int k = 0; k < n; k++) {
            newMin[k] = Math.max(min.coordinates[k], area.min.coordinates[k]);
            newMax[k] = Math.min(max.coordinates[k], area.max.coordinates[k]);
            if (newMin[k] > newMax[k]) {
                return null;
            }
        }
        return new RectangularArea(new Point(newMin), new Point(newMax));
    }

    /**
     * Returns a list of set-theoretical intersections <b>A</b>&nbsp;&cap;&nbsp;<b>B<sub><i>i</i></sub></b>
     * of this rectangular area (<b>A</b>) and all rectangular areas (<b>B<sub><i>i</i></sub></b>), specified
     * by <code>areas</code> argument.
     * If the passed collection doesn't contain areas, intersecting this area, the result will be an empty list.
     * <p>Equivalent to the following loop:
     * <pre>
     * final List&lt;RectangularArea>gt; result = ... (some empty list);
     * for (RectangularArea area : areas) {
     *     RectangularArea intersection = {@link #intersection(RectangularArea) intersection}(area);
     *     if (intersection != null) {
     *         result.add(intersection);
     *     }
     * }
     * </pre>.
     *
     * @param areas collection of areas (we find intersection with each from them).
     * @return intersection of this and the second rectangular area or {@code null} if they do not intersect.
     * @throws NullPointerException     if the argument is {@code null} or one of its elements is {@code null}.
     * @throws IllegalArgumentException if this rectangular area or some of the elements of the passed collection
     *                                  have different {@link #coordCount()}.
     */
    public List<RectangularArea> intersection(Collection<RectangularArea> areas) {
        Objects.requireNonNull(areas, "Null areas argument");
        final List<RectangularArea> result = new ArrayList<>();
        for (RectangularArea area : areas) {
            RectangularArea intersection = intersection(area);
            if (intersection != null) {
                result.add(intersection);
            }
        }
        return result;
    }

    /**
     * Calculates the set-theoretical difference <b>A</b>&nbsp;\&nbsp;<b>B</b> of this (<b>A</b>) and
     * the passed rectangular area (<b>B</b>)
     * in a form of <i>N</i> rectangular areas
     * <b>R</b><sub>1</sub>,<b>R</b><sub>2</sub>,...,<b>R</b><sub><i>N</i></sub>,
     * the set-theoretical union of which is equal to this difference
     * (<b>R</b><sub>1</sub>&cup;<b>R</b><sub>2</sub>&cup;...&cup;<b>R</b><sub><i>N</i></sub> =
     * <b>A</b>&nbsp;\&nbsp;<b>B</b>).
     * The resulting areas <b>R</b><sub>1</sub>,<b>R</b><sub>2</sub>,...,<b>R</b><sub><i>N</i></sub>
     * are added into the collection <code>results</code> by <code>Collection.add(...)</code> method.
     * So, the collection <code>results</code> must be not-null and support adding elements
     * (usually it is <code>List</code> or <code>Queue</code>).
     *
     * <p>It is possible that the difference is empty (<b>A</b>&nbsp;\&nbsp;<b>B</b>&nbsp;=&nbsp;&empty;),
     * i.e., this area <b>A</b> is a subset of the passed one <b>B</b>. In this case, this method does nothing.
     *
     * <p>It is possible that the difference is equal to this area
     * (<b>A</b>&nbsp;\&nbsp;<b>B</b>&nbsp;=&nbsp;<b>A</b>),
     * i.e., this area <b>A</b> does not intersect the passed one <b>B</b>.
     * In this case, this method is equivalent to <code>results.add(thisInstance)</code> call.
     *
     * <p>In other cases, there is more than 1 way to represent the resulting difference
     * in the form of several rectangular areas union
     * <b>R</b><sub>1</sub>,<b>R</b><sub>2</sub>,...,<b>R</b><sub><i>N</i></sub>.
     * The precise way, how this method forms this set of rectangular areas <b>R</b><sub><i>i</i></sub>,
     * is not documented, but this method tries to minimize the number <i>N</i> of such areas.
     * In any case, there is a guarantee that <i>N</i>&le;2*{@link #coordCount()}.
     *
     * @param results the collection to store results (new areas will be added to this collection).
     * @param area    the area <b>B</b>, subtracted from this area <b>A</b>.
     * @return a reference to the <code>results</code> argument.
     * @throws NullPointerException     if <code>result</code> or <code>area</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     * @see #subtractCollection(java.util.Queue, java.util.Collection)
     */
    public Collection<RectangularArea> difference(Collection<RectangularArea> results, RectangularArea area) {
        Objects.requireNonNull(results, "Null results argument");
        if (!intersects(area)) { // also checks the number of dimensions
            results.add(this);
            return results;
        }
        double[] min = this.min.coordinates.clone();
        double[] max = this.max.coordinates.clone();
        for (int k = min.length - 1; k >= 0; k--) {
            assert area.max.coordinates[k] >= min[k] && area.min.coordinates[k] <= max[k]; // because they intersect
            if (area.min.coordinates[k] > this.min.coordinates[k]) {
                min[k] = this.min.coordinates[k];
                max[k] = area.min.coordinates[k];
                results.add(new RectangularArea(Point.of(min), Point.of(max)));
            }
            if (area.max.coordinates[k] < this.max.coordinates[k]) {
                min[k] = area.max.coordinates[k];
                max[k] = this.max.coordinates[k];
                results.add(new RectangularArea(Point.of(min), Point.of(max)));
            }
            min[k] = Math.max(area.min.coordinates[k], this.min.coordinates[k]);
            max[k] = Math.min(area.max.coordinates[k], this.max.coordinates[k]);
            // - intersection of two ranges area.min-max[k] and this.min-max[k]
        }
        return results;
    }

    /**
     * Calculates the set-theoretical difference <b>A</b>&nbsp;\&nbsp;<b>B</b> of
     * the set-theoretical union <b>A</b> of all elements of the collection <code>fromWhatToSubtract</code>
     * and the set-theoretical union <b>B</b> of all elements of the collection <code>whatToSubtract</code>,
     * in a form of a union of <i>N</i> rectangular areas, and replaces
     * the old content of <code>fromWhatToSubtract</code> with the resulting <i>N</i> areas.
     *
     * <p>More precisely, this method is equivalent to the following loop:
     *
     * <pre>
     * for (RectangularArea area : whatToSubtract) {
     *     for (int i = 0, n = fromWhatToSubtract.size(); i &lt; n; i++) {
     *         RectangularArea minuend = fromWhatToSubtract.poll();
     *         minuend.{@link #difference(Collection, RectangularArea) difference}(fromWhatToSubtract, area);
     *     }
     *     if (fromWhatToSubtract.isEmpty()) {
     *         break;
     *     }
     * }
     * </pre>
     *
     * <p>Note: if some exception occurs while execution of the listed loop (for example,
     * some elements of the collections are {@code null} or have different number of dimensions),
     * the <code>fromWhatToSubtract</code> stays partially modified.
     * In other words, this method <b>is non-atomic regarding failures</b>.
     *
     * @param fromWhatToSubtract the minuend <b>A</b>, which will be replaced with <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @param whatToSubtract     the subtrahend <b>B</b>.
     * @return a reference to <code>fromWhatToSubtract</code> argument, which will contain
     * the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @throws NullPointerException     if <code>fromWhatToSubtract</code> or <code>whatToSubtract</code> argument
     *                                  is {@code null} or if one of their elements it {@code null}.
     * @throws IllegalArgumentException if some elements of the passed collections
     *                                  have different {@link #coordCount()}.
     * @see #subtractCollection(java.util.Queue, RectangularArea...)
     */
    public static Queue<RectangularArea> subtractCollection(
            Queue<RectangularArea> fromWhatToSubtract,
            Collection<RectangularArea> whatToSubtract) {
        Objects.requireNonNull(fromWhatToSubtract, "Null fromWhatToSubtract");
        Objects.requireNonNull(whatToSubtract, "Null whatToSubtract");
        for (RectangularArea area : whatToSubtract) {
            final int n = fromWhatToSubtract.size();
            for (int i = 0; i < n; i++) {
                final RectangularArea minuend = fromWhatToSubtract.poll();
                if (minuend == null) {
                    throw new AssertionError("Null minuend in fromWhatToSubtract at index " + i);
                }
                minuend.difference(fromWhatToSubtract, area);
            }
            if (fromWhatToSubtract.isEmpty()) {
                break;
            }
        }
        return fromWhatToSubtract;
    }

    /**
     * Equivalent to <code>{@link #subtractCollection(Queue, Collection)
     * subtractCollection}(fromWhatToSubtract, java.util.Arrays.asList(whatToSubtract))</code>.
     *
     * @param fromWhatToSubtract the minuend <b>A</b>, which will be replaced with <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @param whatToSubtract     the subtrahend <b>B</b>.
     * @return a reference to <code>fromWhatToSubtract</code> argument, which will contain
     * the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @throws NullPointerException     if <code>fromWhatToSubtract</code> or <code>whatToSubtract</code> argument
     *                                  is {@code null} or if one of their elements it {@code null}.
     * @throws IllegalArgumentException if some of the elements of the passed collection and array
     *                                  have different {@link #coordCount()}.
     */
    public static Queue<RectangularArea> subtractCollection(
            Queue<RectangularArea> fromWhatToSubtract,
            RectangularArea... whatToSubtract) {
        return subtractCollection(fromWhatToSubtract, java.util.Arrays.asList(whatToSubtract));
    }

    /**
     * Equivalent to <code>{@link #subtractCollection(Queue, Collection)
     * subtractCollection}(fromWhatToSubtract, whatToSubtract</code>,
     * where <code>fromWhatToSubtract</code> contains this object as the only element.
     *
     * @param whatToSubtract the subtrahend <b>B</b>.
     * @return new collection, containing the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>
     * (<b>A</b> = this object, <b>B</b> = union of all <code>whatToSubtract</code>).
     * @throws NullPointerException     if <code>whatToSubtract</code> argument
     *                                  is {@code null} or if one of their elements it {@code null}.
     * @throws IllegalArgumentException if this rectangular area or some of the elements of the passed collection
     *                                  have different {@link #coordCount()}.
     */
    public Queue<RectangularArea> subtract(Collection<RectangularArea> whatToSubtract) {
        Objects.requireNonNull(whatToSubtract, "Null whatToSubtract");
        Queue<RectangularArea> difference = new ArrayDeque<>();
        difference.add(this);
        RectangularArea.subtractCollection(difference, whatToSubtract);
        return difference;
    }

    /**
     * Equivalent to <code>{@link #subtract(Collection)
     * subtract}(java.util.Arrays.asList(whatToSubtract))</code>.
     *
     * @param whatToSubtract the subtrahend <b>B</b>.
     * @return new collection, containing the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>
     * (<b>A</b> = this object, <b>B</b> = union of all <code>whatToSubtract</code>).
     * @throws NullPointerException     if <code>whatToSubtract</code> argument
     *                                  is {@code null} or if one of their elements it {@code null}.
     * @throws IllegalArgumentException if this rectangular area or some of the elements of the passed array
     *                                  have different {@link #coordCount()}.
     */
    public Queue<RectangularArea> subtract(RectangularArea... whatToSubtract) {
        return subtract(java.util.Arrays.asList(whatToSubtract));
    }

    /**
     * Returns the minimal rectangular area, containing this area and the given point.
     * In the returned area, the {@link #min() minimal vertex} is equal to
     * <code>thisInstance.{@link #min()}.{@link Point#min(Point) min}(point)</code> and
     * the {@link #max() maximal vertex} is equal to
     * <code>thisInstance.{@link #max()}.{@link Point#max(Point) max}(point)</code>.
     *
     * @param point some point that should be included to the new rectangular area.
     * @return the expanded rectangular area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link Point#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the points
     *                                  <code>thisInstance.{@link #min()}.{@link Point#min(Point) min}(point)</code>
     *                                  and
     *                                  <code>thisInstance.{@link #max()}.{@link Point#max(Point) max}(point)</code>
     *                                  do not match requirements of {@link #of(Point, Point)} method.
     */
    public RectangularArea expand(Point point) {
        if (contains(point)) {
            // - also checks number of dimensions
            return this;
        }
        double[] newMin = new double[min.coordinates.length];
        double[] newMax = new double[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = Math.min(min.coordinates[k], point.coordinates[k]);
            newMax[k] = Math.max(max.coordinates[k], point.coordinates[k]);
        }
        return of(new Point(newMin), new Point(newMax));
    }

    /**
     * Returns the minimal rectangular area, containing this and the passed area.
     * Equivalent to
     * <pre>{@link #of(Point, Point) RectangularArea.of}(
     * thisInstance.{@link #min()}.{@link Point#min(Point) min}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link Point#max(Point) max}(area.{@link #max()}))</pre>.
     *
     * @param area the second rectangular area.
     * @return the minimal rectangular area, containing this and the passed area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea expand(RectangularArea area) {
        if (contains(area)) {
            // - also checks number of dimensions
            return this;
        }
        double[] newMin = new double[min.coordinates.length];
        double[] newMax = new double[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = Math.min(min.coordinates[k], area.min.coordinates[k]);
            newMax[k] = Math.max(max.coordinates[k], area.max.coordinates[k]);
        }
        return new RectangularArea(new Point(newMin), new Point(newMax));
    }

    /**
     * Returns the minimal rectangular area, containing all passed areas.
     * Equivalent to the loop of {@link #expand(RectangularArea)} methods, called for each element
     * of the passed collection, but works faster.
     *
     * <p>If the passed collection is empty, returns {@code null}.
     *
     * @param areas some collection of rectangular areas.
     * @return the minimal rectangular area, containing all them, or {@code null} for empty collection.
     * @throws NullPointerException     if the argument or one of the passed areas is {@code null}.
     * @throws IllegalArgumentException if <code>{@link #coordCount() coordCount()}</code> is not equal for all areas.
     */
    public static RectangularArea minimalContainingArea(Collection<RectangularArea> areas) {
        Objects.requireNonNull(areas, "Null areas");
        if (areas.isEmpty()) {
            return null;
        }
        final int coordCount = areas.iterator().next().coordCount();
        final double[] min = new double[coordCount];
        final double[] max = new double[coordCount];
        java.util.Arrays.fill(min, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (RectangularArea area : areas) {
            if (area.coordCount() != coordCount) {
                throw new IllegalArgumentException("Some areas have different number of dimension: "
                        + area.coordCount() + " and " + coordCount);
            }
            for (int k = 0; k < coordCount; k++) {
                min[k] = Math.min(min[k], area.min(k));
                max[k] = Math.max(max[k], area.max(k));
            }
        }
        return of(new Point(min), new Point(max));
    }

    /**
     * Returns the <i>parallel distance</i> from the given point to this rectangular area.
     * The parallel distance is a usual distance, with plus or minus sign,
     * from the point to some of the hyperplanes, containing the hyper-facets of this hyper-parallelepiped,
     * chosen so that:
     *
     * <ol>
     * <li>the parallel distance is zero at the hyper-facets, negative inside the rectangular area and
     * positive outside it;</li>
     * <li>for any constant <i>c</i>,
     * the set of all such points, that the parallel distance from them to this rectangular area &le;<i>c</i>,
     * is also hyper-parallelepiped (rectangular area) with hyper-facets
     * parallel to the coordinate hyperplanes,
     * or an empty set if <i>c</i>&lt;<i>c</i><sub>0</sub>, where <i>c</i><sub>0</sub> is the (negative)
     * parallel distance from the geometrical center of this hyper-parallelepiped.</li>
     * </ol>
     *
     * <p>Formally, let <b>p</b> is any point with coordinates
     * <i>p</i><sub>0</sub>, <i>p</i><sub>1</sub>, ..., <i>p</i><sub><i>n</i>&minus;1</sub>,
     * <i>l<sub>i</sub></i> = {@link #min(int) min}(<i>i</i>),
     * <i>r<sub>i</sub></i> = {@link #max(int) max}(<i>i</i>),
     * <i>d<sub>i</sub></i> = max(<i>l<sub>i</sub></i>&minus;<i>p<sub>i</sub></i>,
     * <i>p<sub>i</sub></i>&minus;<i>r<sub>i</sub></i>).
     * Note that <i>d<sub>i</sub></i> is positive if <i>p<sub>i</sub></i>&lt;<i>l<sub>i</sub></i>
     * or <i>p<sub>i</sub></i>&gt;<i>r<sub>i</sub></i> and negative if <i>p<sub>i</sub></i>
     * is inside <i>l<sub>i</sub></i>..<i>r<sub>i</sub></i> range.
     * The <i>parallel distance</i> from the point <b>p</b> to this rectangular area
     * is defined as maximal value from all <i>d<sub>i</sub></i>:
     * max(<i>d</i><sub>0</sub>, <i>d</i><sub>1</sub>, ..., <i>d</i><sub><i>n</i>&minus;1</sub>).
     *
     * @param point some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link Point#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public double parallelDistance(Point point) {
        Objects.requireNonNull(point, "Null point argument");
        return parallelDistance(point.coordinates);
    }

    /**
     * Equivalent to {@link #parallelDistance(Point) parallelDistance}({@link Point#of(double...)
     * Point.of}(coordinates)), but works faster because does not require creating an instance
     * of {@link Point} class.
     *
     * @param coordinates coordinates of some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws NullPointerException     if <code>coordinates</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>coordinates.length</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public double parallelDistance(double... coordinates) {
        Objects.requireNonNull(coordinates, "Null coordinates argument");
        int n = this.min.coordinates.length;
        if (coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + coordinates.length + " instead of " + n);
        }
        double min = this.min.coordinates[0];
        double max = this.max.coordinates[0];
        double x = coordinates[0];
        double maxD = Math.max(min - x, x - max);
        for (int k = 1; k < n; k++) {
            min = this.min.coordinates[k];
            max = this.max.coordinates[k];
            double xk = coordinates[k];
            double d = Math.max(min - xk, xk - max);
            if (d > maxD) {
                maxD = d;
            }
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(Point) parallelDistance}({@link Point#of(double...)
     * Point.of}(x, y)), but works faster because does not require allocating any objects.
     * Works only for 2-dimensional rectangular areas, in other cases throws
     * <code>IllegalArgumentException</code>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <code>coordinates.length!=2</code> .
     */
    public double parallelDistance(double x, double y) {
        int n = min.coordinates.length;
        if (n != 2) {
            throw new IllegalArgumentException("Dimensions count mismatch: 2 instead of " + n);
        }
        double min = this.min.coordinates[0];
        double max = this.max.coordinates[0];
        double maxD = Math.max(min - x, x - max);
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        double d = Math.max(min - y, y - max);
        if (d > maxD) {
            maxD = d;
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(Point) parallelDistance}({@link Point#of(double...)
     * Point.of}(x, y, z)), but works faster because does not require allocating any objects.
     * Works only for 3-dimensional rectangular areas, in other cases throws
     * <code>IllegalArgumentException</code>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @param z the 3rd coordinate of some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <code>coordinates.length!=2</code> .
     */
    public double parallelDistance(double x, double y, double z) {
        int n = min.coordinates.length;
        if (n != 3) {
            throw new IllegalArgumentException("Dimensions count mismatch: 3 instead of " + n);
        }
        double min = this.min.coordinates[0];
        double max = this.max.coordinates[0];
        double maxD = Math.max(min - x, x - max);
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        double d = Math.max(min - y, y - max);
        if (d > maxD) {
            maxD = d;
        }
        min = this.min.coordinates[2];
        max = this.max.coordinates[2];
        d = Math.max(min - z, z - max);
        if (d > maxD) {
            maxD = d;
        }
        return maxD;
    }
    /*Repeat.IncludeEnd*/

    /**
     * Shifts this rectangular area by the specified vector and returns the shifted area.
     * Equivalent to
     * <pre>{@link #of(Point, Point)
     * of}(thisInstance.{@link #min()}.{@link Point#add(Point)
     * add}(vector), thisInstance.{@link #max()}.{@link Point#add(Point) add}(vector))</pre>
     *
     * @param vector the vector which is added to all vertices of this area.
     * @return the shifted area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>vector.{@link Point#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea shift(Point vector) {
        Objects.requireNonNull(vector, "Null vector argument");
        if (vector.coordinates.length != min.coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + vector.coordinates.length + " instead of " + min.coordinates.length);
        }
        if (vector.isOrigin()) {
            return this;
        }
        return new RectangularArea(min.add(vector), max.add(vector));
    }

    /**
     * Shifts this rectangular area by <code>vector.{@link Point#symmetric() symmetric()}</code>
     * and returns the shifted area.
     * Equivalent to
     * <pre>{@link #of(Point, Point)
     * of}(thisInstance.{@link #min()}.{@link Point#subtract(Point)
     * subtract}(vector), thisInstance.{@link #max()}.{@link Point#subtract(Point) subtract}(vector))</pre>
     *
     * @param vector the vector which is subtracted from all vertices of this area.
     * @return the shifted area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>vector.{@link Point#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea shiftBack(Point vector) {
        Objects.requireNonNull(vector, "Null vector argument");
        if (vector.coordinates.length != min.coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + vector.coordinates.length + " instead of " + min.coordinates.length);
        }
        if (vector.isOrigin()) {
            return this;
        }
        return new RectangularArea(min.subtract(vector), max.subtract(vector));
    }

    /**
     * Returns this rectangular area, dilated (expanded) according the argument. More precisely, returns
     *
     * <pre>RectangularArea.of(
     * thisInstance.{@link #min() min()}.{@link Point#subtract(Point) subtract}(expansion),
     * thisInstance.{@link #max() max()}.{@link Point#add(Point) add}(expansion))</pre>
     *
     * <p>(but if <code>expansion.{@link Point#isOrigin() isOrigin()}</code>,
     * returns this object without changes).</p>
     *
     * @param expansion how to dilate this area.
     * @return dilated area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>expansion.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the result area will be incorrect (see comments to
     *                                  {@link #of(Point, Point)} method).
     */
    public RectangularArea dilate(Point expansion) {
        Objects.requireNonNull(expansion, "Null expansion");
        if (expansion.coordCount() != coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + expansion.coordCount() + " instead of " + coordCount());
        }
        if (expansion.isOrigin()) {
            return this;
        }
        return RectangularArea.of(min().subtract(expansion), max().add(expansion));
    }

    /**
     * Equivalent to <code>{@link #dilate(Point) dilate}(Point.ofEqualCoordinates(thisObjects.{@link
     * #coordCount() coordCount()}, expansion)</code>.
     *
     * @param expansion how to dilate this area.
     * @return dilated area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the result area will be incorrect (see comments to
     *                                  {@link #of(Point, Point)} method).
     */
    public RectangularArea dilate(double expansion) {
        return dilate(Point.ofEqualCoordinates(coordCount(), expansion));
    }

    /**
     * Returns this area, dilated according the argument only <i>adouble coordinate axes</i>,
     * without full hypercube areas near vertices (like in {@link #dilate(Point)} method).
     *
     * <p>More precisely, the result is a list, consisting of this area and (usually) 2*{@link #coordCount()}
     * rectangular areas, lying adouble facets of this area, like in the following picture:
     * <pre>
     *     aaaaaaaaaaaa
     *   bb<b>RRRRRRRRRRRR</b>cc
     *   bb<b>RRRRRRRRRRRR</b>cc
     *   bb<b>RRRRRRRRRRRR</b>cc
     *     ddddddddddd
     * </pre>
     * This figure shows dilation of some 2-dimensional rectangle <code><b>R</b></code> by
     * expansion=<code>Point.of(2,1)</code>:
     * the results consist of the original rectangle and 4 rectangles <code>a</code>, <code>b</code> (height 1) and
     * <code>c</code>, <code>d</code> (width 2).
     *
     * <p>Note: all coordinates of <code>expansion</code> argument <b>must</b> be non-negative
     * (unlike {@link #dilate(Point)} method).
     *
     * <p>If some coordinates of the point <code>expansion</code> are zero, new areas adouble the corresponding
     * facets are not added (recanglar area cannot be empty).
     * In particular, if <code>expansion.{@link Point#isOrigin() isOrigin()}</code>,
     * the result will contain this area as the only element.
     *
     * @param results   the list to store results (new areas will be added to the end of this list).
     * @param expansion how to dilate this area.
     * @return a reference to the <code>results</code> argument.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>expansion.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if one of coordinates of <code>expansion</code> is negative,
     *                                  or if the result area will be incorrect (see comments to
     *                                  {@link #of(Point, Point)} method).
     */
    public List<RectangularArea> dilateStraightOnly(List<RectangularArea> results, Point expansion) {
        Objects.requireNonNull(results, "Null results");
        Objects.requireNonNull(expansion, "Null expansion");
        results.add(this);
        final int coordCount = coordCount();
        if (expansion.coordCount() != coordCount) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + expansion.coordCount() + " instead of " + coordCount);
        }
        final double[] min = this.min.coordinates();
        final double[] max = this.max.coordinates();
        for (int k = 0; k < coordCount; k++) {
            final double delta = expansion.coordinates[k];
            if (delta == 0) {
                continue;
            }
            if (delta < 0) {
                throw new IllegalArgumentException("Negative expansion is impossible: " + expansion);
            }
            final double saveMin = min[k];
            final double saveMax = max[k];
            min[k] = saveMin - delta;
            max[k] = saveMin;
            results.add(RectangularArea.of(Point.of(min), Point.of(max)));
            min[k] = saveMax;
            max[k] = saveMax + delta;
            results.add(RectangularArea.of(Point.of(min), Point.of(max)));
            min[k] = saveMin;
            max[k] = saveMax;
        }
        return results;
    }

    /**
     * Equivalent to <code>{@link #dilateStraightOnly(List, Point)
     * dilateStraightOnly}(results, Point.ofEqualCoordinates(thisObjects.{@link
     * #coordCount() coordCount()}, expansion)</code>.
     *
     * @param results   the list to store results (new areas will be added to the end of this list).
     * @param expansion how to dilate this area.
     * @return a reference to the <code>results</code> argument.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>expansion &lt; 0</code>
     *                                  or if the result area will be incorrect (see comments to
     *                                  {@link #of(Point, Point)} method).
     */
    public List<RectangularArea> dilateStraightOnly(List<RectangularArea> results, double expansion) {
        return dilateStraightOnly(results, Point.ofEqualCoordinates(coordCount(), expansion));
    }

    /**
     * Dilates all areas, specified by the argument, by {@link #dilate(Point) dilate} or
     * {@link #dilateStraightOnly(List, Point) dilateStraightOnly} method,
     * and returns the list of dilated areas.
     * <p>If <code>straightOnly</code> argument is <code>false</code>, this method is equivalent to the following code:
     * <pre>
     * final List&lt;RectangularArea&gt; result = new ArrayList&lt;RectangularArea&gt;();
     * for (RectangularArea area : areas) {
     *     result.add(area.{@link #dilate(Point) dilate}(expansion));
     * }</pre>
     * <p>If <code>straightOnly</code> argument is <code>true</code>, this method is equivalent to the following code:
     * <pre>
     * final List&lt;RectangularArea&gt; result = new ArrayList&lt;RectangularArea&gt;();
     * for (RectangularArea area : areas) {
     *     area.{@link #dilateStraightOnly(List, Point) dilateStraightOnly}(result, expansion);
     * }</pre>
     * <p>Note that in the second case the resulting list will usually contain more elements than
     * the source <code>areas</code> collection.
     *
     * @param areas        areas to be dilated.
     * @param expansion    how to dilate these areas.
     * @param straightOnly dilation mode.
     * @return list of dilated areas.
     * @throws NullPointerException     if one of the  arguments is {@code null} or one of areas is {@code null}.
     * @throws IllegalArgumentException if <code>expansion.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas,
     *                                  or if <code>straightOnly</code> amd one of coordinates
     *                                  of <code>expansion</code>
     *                                  is negative (and collection of areas is not empty),
     *                                  or if one of the result areas will be incorrect (see comments to
     *                                  {@link #of(Point, Point)} method).
     */
    public static List<RectangularArea> dilate(
            Collection<RectangularArea> areas,
            Point expansion,
            boolean straightOnly) {
        Objects.requireNonNull(areas, "Null areas");
        final List<RectangularArea> result = new ArrayList<>();
        for (RectangularArea area : areas) {
            if (straightOnly) {
                area.dilateStraightOnly(result, expansion);
            } else {
                result.add(area.dilate(expansion));
            }
        }
        return result;
    }

    /**
     * Equivalent to
     * <code>{@link IRectangularArea#of(RectangularArea) IRectangularArea.of}(thisInstance)</code>,
     * with the only difference that <code>IllegalStateException</code> is thrown instead of
     * <code>IllegalArgumentException</code> for unallowed rectangular area.
     *
     * @return the integer rectangular area with the same (cast) coordinates.
     * @throws IllegalStateException in the same situation when {@link IRectangularArea#of(RectangularArea)}
     *                               method throws <code>IllegalArgumentException</code>.
     */
    public IRectangularArea toIntegerRectangularArea() {
        return IRectangularArea.of(IPoint.of(min), IPoint.of(max), true);
    }

    /**
     * Equivalent to
     * <code>{@link IRectangularArea#roundOf(RectangularArea) IRectangularArea.roundOf}(thisInstance)</code>,
     * with the only difference that <code>IllegalStateException</code> is thrown instead of
     * <code>IllegalArgumentException</code> for unallowed rectangular area.
     *
     * @return the integer rectangular area with the same (rounded) coordinates.
     * @throws IllegalStateException in the same situation when {@link IRectangularArea#roundOf(RectangularArea)}
     *                               method throws <code>IllegalArgumentException</code>.
     */
    public IRectangularArea toRoundedRectangularArea() {
        return IRectangularArea.of(IPoint.roundOf(min), IPoint.roundOf(max), true);
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * information about all coordinates ranges between the minimum and maximum vertices of this area.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("[" + range(0) + "]");
        for (int k = 1; k < min.coordinates.length; k++) {
            sb.append("x").append("[").append(range(k)).append("]");
        }
        return sb.toString();
    }

    /**
     * Returns the hash code of this rectangular area.
     *
     * @return the hash code of this rectangular area.
     */
    public int hashCode() {
        return min.hashCode() * 37 + max.hashCode();
    }

    /**
     * Indicates whether some other rectangular area is equal to this instance.
     * Returns <code>true</code> if and only if <code>obj instanceof RectangularArea</code>,
     * <code>((RectangularArea)obj).min().equals(this.min())</code> and
     * <code>((RectangularArea)obj).max().equals(this.max())</code>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return <code>true</code> if the specified object is a rectangular area equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof RectangularArea
                && ((RectangularArea) obj).min.equals(this.min) && ((RectangularArea) obj).max.equals(this.max);
    }
}
