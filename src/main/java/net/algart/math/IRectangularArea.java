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
 * <p>Rectangular integer area, i&#46;e&#46;
 * hyper-parallelepiped in multidimensional space with integer coordinates of vertices.
 * All edges of the hyper-parallelepiped are parallel to coordinate axes.
 * In 1-dimensional case it is an equivalent of {@link IRange} class,
 * in 2-dimensional case it is an analog of the standard <code>java.awt.Rectangle</code> class.</p>
 *
 * <p>More precisely, the region, specified by this class, is defined by two <i>n</i>-dimensional points
 * with integer coordinates ({@link IPoint}),
 * named the <i>minimal vertex</i> <b>min</b> and <i>maximal vertex</i> <b>max</b>,
 * and consists of all such points
 * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>), that:</p>
 *
 * <blockquote>
 * <b>min</b>.{@link IPoint#coord(int) coord(0)} &le; <i>x</i><sub>0</sub> &le;
 * <b>max</b>.{@link IPoint#coord(int) coord(0)},<br>
 * <b>min</b>.{@link IPoint#coord(int) coord(1)} &le; <i>x</i><sub>1</sub> &le;
 * <b>max</b>.{@link IPoint#coord(int) coord(1)},<br>
 * ...,<br>
 * <b>min</b>.{@link IPoint#coord(int) coord(<i>n</i>-1)} &le; <i>x</i><sub><i>n</i>&minus;1</sub> &le;
 * <b>max</b>.{@link IPoint#coord(int) coord(<i>n</i>-1)}.
 * </blockquote>
 *
 * <p>The <b>min</b> and <b>max</b> points are specified while creating an instance of this class
 * and can be retrieved by {@link #min()} and {@link #max()} methods.</p>
 *
 * <p>The coordinates of the minimal vertex
 * <b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}
 * are never greater than the corresponding coordinates of the maximal vertex
 * <b>max</b>.{@link IPoint#coord(int) coord(<i>i</i>)},
 * the coordinates of the minimal and maximal vertices are always in range
 * <code>-Long.MAX_VALUE+1..Long.MAX_VALUE-1</code>,
 * and their difference is always <i>less</i> than <code>Long.MAX_VALUE</code>.
 * In other words,
 * "<code><b>max</b>.{@link IPoint#coord(int)
 * coord(<i>i</i>)}-<b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}+1</code>" expression,
 * returned by {@link #size(int)} method, and also
 * "<code><b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}-1</code>",
 * "<code><b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}-2</code>" and
 * "<code><b>max</b>.{@link IPoint#coord(int) coord(<i>i</i>)}+1</code>" expressions
 * are always calculated without overflow,
 * and the {@link #range(int)} method is always possible to return an allowed range.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see RectangularArea
 */
public class IRectangularArea {
    final IPoint min;
    final IPoint max;

    private IRectangularArea(IPoint min, IPoint max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns an instance of this class with the given minimal vertex <b>min</b> and
     * maximal vertex <b>max</b>.
     * See the {@link IRectangularArea comments to this class} for more details.
     *
     * @param min the minimal vertex, inclusive.
     * @param max the maximal vertex, inclusive.
     * @return the new rectangular area "between" these vertices.
     * @throws NullPointerException     if one of arguments is {@code null}.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in <code>min</code>
     *                                  and <code>max</code> points are different,
     *                                  or if, for some <i>i</i>,
     *                                  <code>min.{@link IPoint#coord(int) coord}(<i>i</i>)
     *                                  &gt; max.{@link IPoint#coord(int) coord}(<i>i</i>)</code>,
     *                                  or if <code>max.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>)-min.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>)+1
     *                                  &gt; Long.MAX_VALUE</code>
     *                                  (more precisely, if this Java expression is nonpositive
     *                                  due to integer overflow),
     *                                  or if <code>min.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>) &lt;= -Long.MAX_VALUE</code>,
     *                                  or if <code>max.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>) == Long.MAX_VALUE</code>.
     */
    public static IRectangularArea of(IPoint min, IPoint max) {
        return of(min, max, false);
    }

    @Deprecated
    public static IRectangularArea valueOf(IPoint min, IPoint max) {
        return of(min, max);
    }

    /**
     * Returns the Cartesian product of the specified coordinate ranges.
     * More precisely, return an <i>n</i>-dimensional {@link IRectangularArea rectangular area}
     * with the minimal vertex <b>min</b> and maximal vertex <b>max</b>, where
     * <i>n</i><code>=coordRanges.length</code>,
     * <b>min</b>.{@link IPoint#coord(int)
     * coord(<i>i</i>)}<code>=coordRanges[<i>i</i>].{@link IRange#min() min()}</code>,
     * <b>max</b>.{@link IPoint#coord(int)
     * coord(<i>i</i>)}<code>=coordRanges[<i>i</i>].{@link IRange#max() max()}</code>.
     * See the {@link IRectangularArea comments to this class} for more details.
     *
     * @param coordRanges the coordinate ranges.
     * @return the Cartesian product of the specified coordinate ranges.
     * @throws NullPointerException     if the argument is {@code null}
     *                                  or if one of specified <code>coordRanges</code> is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no ranges are passed).
     */
    public static IRectangularArea of(IRange... coordRanges) {
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
        long[] min = new long[n];
        long[] max = new long[n];
        for (int k = 0; k < n; k++) {
            min[k] = coordRanges[k].min;
            max[k] = coordRanges[k].max;
        }
        return new IRectangularArea(new IPoint(min), new IPoint(max));
    }

    @Deprecated
    public static IRectangularArea valueOf(IRange... coordRanges) {
        return of(coordRanges);
    }

    /**
     * Returns a 1-dimensional rectangular area (range) with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #of(IPoint, IPoint) of}(
     *      {@link IPoint#of(long) IPoint.of}(minX),
     *      {@link IPoint#of(long) IPoint.of}(maxX));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @return the new 1-dimensional rectangular area.
     * @throws IllegalArgumentException in the same situations as {@link #of(IPoint, IPoint)} method.
     */
    public static IRectangularArea of(long minX, long maxX) {
        return of(IPoint.of(minX), IPoint.of(maxX));
    }

    @Deprecated
    public static IRectangularArea valueOf(long minX, long maxX) {
        return of(minX, maxX);
    }


    /**
     * Returns a 2-dimensional rectangle with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #of(IPoint, IPoint) of}(
     *      {@link IPoint#of(long...) IPoint.of}(minX, minY),
     *      {@link IPoint#of(long...) IPoint.of}(maxX, maxY));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param minY the minimal <i>y</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @param maxY the maximal <i>y</i>-coordinate, inclusive.
     * @return the new 2-dimensional rectangle.
     * @throws IllegalArgumentException in the same situations as {@link #of(IPoint, IPoint)} method.
     */
    public static IRectangularArea of(long minX, long minY, long maxX, long maxY) {
        return of(IPoint.of(minX, minY), IPoint.of(maxX, maxY));
    }

    @Deprecated
    public static IRectangularArea valueOf(long minX, long minY, long maxX, long maxY) {
        return of(minX, minY, maxX, maxY);
    }

    /**
     * Returns a 3-dimensional parallelepiped with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #of(IPoint, IPoint) of}(
     *      {@link IPoint#of(long...) IPoint.of}(minX, minY, minZ),
     *      {@link IPoint#of(long...) IPoint.of}(maxX, maxY, maxZ));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param minY the minimal <i>y</i>-coordinate, inclusive.
     * @param minZ the minimal <i>z</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @param maxY the maximal <i>y</i>-coordinate, inclusive.
     * @param maxZ the maximal <i>z</i>-coordinate, inclusive.
     * @return the new 3-dimensional parallelepiped.
     * @throws IllegalArgumentException in the same situations as {@link #of(IPoint, IPoint)} method.
     */
    public static IRectangularArea of(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        return of(IPoint.of(minX, minY, minZ), IPoint.of(maxX, maxY, maxZ));
    }

    @Deprecated
    public static IRectangularArea valueOf(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        return of(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Returns a new rectangular area with the same coordinates as the given area.
     * All <code>double</code> coordinates of the passed area are converted
     * to <code>long</code> coordinates of the returned area by standard
     * Java typecast <code>(long)doubleValue</code>.
     * Equivalent to <code>{@link #of(IPoint, IPoint) of}({@link IPoint#of(Point)
     * IPoint.of}(area.{@link #min() min()}),&nbsp;{@link IPoint#of(Point)
     * IPoint.of}(area.{@link #max() max()}))</code>.
     *
     * @param area the real rectangular area.
     * @return the integer rectangular area with the same (cast) coordinates.
     * @throws NullPointerException     if the passed area is {@code null}.
     * @throws IllegalArgumentException if the points <code>{@link IPoint#of(Point)
     *                                  IPoint.of}(area.{@link #min() min()})</code>
     *                                  and <code>{@link IPoint#of(Point)
     *                                  IPoint.of}(area.{@link #max() max()})</code>
     *                                  do not match requirements of {@link #of(IPoint, IPoint)} method.
     */
    public static IRectangularArea of(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        return of(IPoint.of(area.min), IPoint.of(area.max));
    }

    @Deprecated
    public static IRectangularArea valueOf(RectangularArea area) {
        return of(area);
    }

    /**
     * Returns a new rectangular area with the same coordinates as the given area.
     * All <code>double</code> coordinates of the passed area are converted
     * to <code>long</code> coordinates of the returned area by <code>StrictMath.round</code> method.
     * Java typecast <code>(long)doubleValue</code>.
     * Equivalent to <code>{@link #of(IPoint, IPoint) of}({@link IPoint#roundOf(Point)
     * IPoint.roundOf}(area.{@link #min() min()}),&nbsp;{@link IPoint#roundOf(Point)
     * IPoint.roundOf}(area.{@link #max() max()}))</code>.
     *
     * @param area the real rectangular area.
     * @return the integer rectangular area with the same (rounded) coordinates.
     * @throws NullPointerException     if the passed area is {@code null}.
     * @throws IllegalArgumentException if the points <code>{@link IPoint#of(Point)
     *                                  IPoint.of}(area.{@link #min() min()})</code>
     *                                  and <code>{@link IPoint#of(Point)
     *                                  IPoint.of}(area.{@link #max() max()})</code>
     *                                  do not match requirements of {@link #of(IPoint, IPoint)} method.
     */
    public static IRectangularArea roundOf(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        return of(IPoint.roundOf(area.min), IPoint.roundOf(area.max));
    }

    /**
     * Returns the number of dimensions.
     * Equivalent to <code>{@link #min()}.{@link IPoint#coordCount() coordCount()}</code>
     * or <code>{@link #max()}.{@link IPoint#coordCount() coordCount()}</code>, but works faster.
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
     * See the {@link IRectangularArea comments to this class} for more details.
     *
     * @return the minimal vertex of this rectangular area.
     */
    public IPoint min() {
        return min;
    }

    /**
     * Returns the maximal vertex of this rectangular area:
     * the point with maximal coordinates, belonging to this area.
     * See the {@link IRectangularArea comments to this class} for more details.
     *
     * @return the maximal vertex of this rectangular area.
     */
    public IPoint max() {
        return max;
    }

    /**
     * Returns all sizes of this rectangular area in a form of {@link IPoint}.
     * Equivalent to <code>{@link IPoint#of(long...) IPoint.of}({@link #sizes()})</code>.
     * The coordinates of the returned point are greater by 1 than coordinates of
     * <code>{@link #max()}.{@link IPoint#subtract(IPoint) subtract}({@link #min()})</code>.
     *
     * @return all sizes of this rectangular area in a form of {@link IPoint}.
     */
    public IPoint size() {
        return new IPoint(sizes());
    }

    /**
     * Returns all sizes of this rectangular area, decreased by 1, in a form of {@link IPoint}.
     * Equivalent to <code>{@link IPoint#of(long...) IPoint.of}({@link #widths()})</code>.
     * The coordinates of the returned point are equal to coordinates of
     * <code>{@link #max()}.{@link IPoint#subtract(IPoint) subtract}({@link #min()})</code>.
     *
     * @return all sizes of this rectangular area, decreased by 1, in a form of {@link IPoint}.
     */
    public IPoint width() {
        return new IPoint(widths());
    }

    /**
     * Returns <code>{@link #min()}.{@link IPoint#coord(int) coord}(coordIndex)</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #min()}.{@link IPoint#coord(int) coord}(coordIndex)</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public long min(int coordIndex) {
        return min.coordinates[coordIndex];
    }

    /**
     * Returns <code>{@link #max()}.{@link IPoint#coord(int) coord}(coordIndex)</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #max()}.{@link IPoint#coord(int) coord}(coordIndex)</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public long max(int coordIndex) {
        return max.coordinates[coordIndex];
    }

    /**
     * Returns <code>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex) + 1</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex) + 1</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public long size(int coordIndex) {
        return max.coordinates[coordIndex] - min.coordinates[coordIndex] + 1;
    }

    /**
     * Returns <code>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</code>.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #coordCount()}</code>.
     */
    public long width(int coordIndex) {
        return max.coordinates[coordIndex] - min.coordinates[coordIndex];
    }

    /**
     * Returns <code>{@link #min()}.{@link IPoint#x() x()}</code>.
     *
     * @return <code>{@link #min()}.{@link IPoint#x() x()}</code>.
     */
    public long minX() {
        return min.coordinates[0];
    }

    /**
     * Returns <code>{@link #max()}.{@link IPoint#x() x()}</code>.
     *
     * @return <code>{@link #max()}.{@link IPoint#x() x()}</code>.
     */
    public long maxX() {
        return max.coordinates[0];
    }

    /**
     * Returns <code>{@link #maxX() maxX()} - {@link #minX() minX()} + 1</code>.
     *
     * @return <code>{@link #maxX() maxX()} - {@link #minX() minX()} + 1</code>.
     */
    public long sizeX() {
        return max.coordinates[0] - min.coordinates[0] + 1;
    }

    /**
     * Returns <code>{@link #maxX() maxX()} - {@link #minX() minX()}</code>.
     *
     * @return <code>{@link #maxX() maxX()} - {@link #minX() minX()}</code>.
     */
    public long widthX() {
        return max.coordinates[0] - min.coordinates[0];
    }

    /**
     * Returns <code>{@link #min()}.{@link IPoint#y() y()}</code>.
     *
     * @return <code>{@link #min()}.{@link IPoint#y() y()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public long minY() {
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
    public long maxY() {
        if (min.coordinates.length < 2) {
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[1];
    }

    /**
     * Returns <code>{@link #maxY() maxY()} - {@link #minY() minY()} + 1</code>.
     *
     * @return <code>{@link #maxY() maxY()} - {@link #minY() minY()} + 1</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public long sizeY() {
        if (min.coordinates.length < 2) {
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[1] - min.coordinates[1] + 1;
    }

    /**
     * Returns <code>{@link #maxY() maxY()} - {@link #minY() minY()}</code>.
     *
     * @return <code>{@link #maxY() maxY()} - {@link #minY() minY()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;2</code>.
     */
    public long widthY() {
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
    public long minZ() {
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
    public long maxZ() {
        if (min.coordinates.length < 3) {
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[2];
    }

    /**
     * Returns <code>{@link #maxZ() maxZ()} - {@link #minZ() minZ()} + 1</code>.
     *
     * @return <code>{@link #maxZ() maxZ()} - {@link #minZ() minZ()} + 1</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;3</code>.
     */
    public long sizeZ() {
        if (min.coordinates.length < 3) {
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        }
        return max.coordinates[2] - min.coordinates[2] + 1;
    }

    /**
     * Returns <code>{@link #maxZ() maxZ()} - {@link #minZ() minZ()}</code>.
     *
     * @return <code>{@link #maxZ() maxZ()} - {@link #minZ() minZ()}</code>.
     * @throws IllegalStateException if <code>{@link #coordCount()}&lt;3</code>.
     */
    public long widthZ() {
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
    public long[] sizes() {
        long[] sizes = new long[min.coordinates.length];
        for (int k = 0; k < sizes.length; k++) {
            sizes[k] = max.coordinates[k] - min.coordinates[k] + 1;
        }
        return sizes;
    }

    /**
     * Returns the sizes of this rectangular area along all dimensions, decreased by 1.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <code>#k</code> contains <code>{@link #width(int) width}(k)</code>.
     *
     * @return the sizes of this rectangular area along all dimensions, decreased by 1.
     */
    public long[] widths() {
        long[] widths = new long[min.coordinates.length];
        for (int k = 0; k < widths.length; k++) {
            widths[k] = max.coordinates[k] - min.coordinates[k];
        }
        return widths;
    }

    /**
     * Returns the volume of this rectangular area: the product of all sizes
     * returned by {@link #sizes()} method. This area is calculated in <code>double</code> values.
     *
     * @return the multidimensional volume of this rectangular area (usual area in 2-dimensional case).
     */
    public double volume() {
        double result = max.coordinates[0] - min.coordinates[0] + 1;
        for (int k = 1; k < min.coordinates.length; k++) {
            result *= max.coordinates[k] - min.coordinates[k] + 1;
        }
        return result;
    }

    /**
     * Returns <code>{@link IRange}.{@link IRange#of(long, long)
     * of}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</code>.
     *
     * @param coordIndex the index of the coordinate.
     * @return <code>{@link IRange}.{@link IRange#of(long, long)
     * of}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</code>.
     */
    public IRange range(int coordIndex) {
        return new IRange(min.coordinates[coordIndex], max.coordinates[coordIndex]);
    }

    /**
     * Returns the projections of this rectangular area to all axes.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <code>#k</code> contains <code>{@link #range(int) range}(k)</code>.
     *
     * @return the projections of this rectangular area to all axes.
     */
    public IRange[] ranges() {
        IRange[] ranges = new IRange[min.coordinates.length];
        for (int k = 0; k < ranges.length; k++) {
            ranges[k] = IRange.of(min.coordinates[k], max.coordinates[k]);
        }
        return ranges;
    }

    /**
     * Returns <code>true</code> if and only if
     * <code>{@link #min(int) min}(k)&lt;=point.{@link IPoint#coord(int) coord}(k)&lt;={@link #max(int) max}(k)</code>
     * for all <i>k</i>.
     *
     * @param point the checked point.
     * @return <code>true</code> if this rectangular area contains the given point.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link IPoint#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean contains(IPoint point) {
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
     * (see {@link #contains(IPoint)} method).
     *
     * @param areas list of checked rectangular areas.
     * @param point the checked point.
     * @return <code>true</code> if one of the passed areas contains the given point.
     * @throws NullPointerException     if one of the arguments or one of the areas is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link IPoint#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas.
     */
    public static boolean contains(Collection<IRectangularArea> areas, IPoint point) {
        Objects.requireNonNull(areas, "Null areas argument");
        Objects.requireNonNull(point, "Null point argument");
        for (IRectangularArea a : areas) {
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
    public boolean contains(IRectangularArea area) {
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
     * Returns <code>true</code> if at least one of the specified <code>areas</code>
     * contains the passed <code>area</code>
     * (see {@link #contains(IRectangularArea)} method).
     *
     * @param areas list of checked rectangular areas.
     * @param area  the checked area.
     * @return <code>true</code> if one of the passed areas (1st argument) contains the given area (2nd argument).
     * @throws NullPointerException     if one of the arguments or one of the areas is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas
     *                                  in the 1st argument.
     */
    public static boolean contains(Collection<IRectangularArea> areas, IRectangularArea area) {
        Objects.requireNonNull(areas, "Null areas argument");
        Objects.requireNonNull(area, "Null area argument");
        for (IRectangularArea a : areas) {
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
    public boolean intersects(IRectangularArea area) {
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

    /*Repeat.SectionStart operationsAndParallelDistance*/

    /**
     * Returns the set-theoretical intersection <b>A</b>&nbsp;&cap;&nbsp;<b>B</b> of this (<b>A</b>) and
     * the passed rectangular area (<b>B</b>) or {@code null} if they
     * do not {@link #intersects(IRectangularArea) intersect}
     * (<b>A</b>&nbsp;&cap;&nbsp;<b>B</b>&nbsp;=&nbsp;&empty;).
     * Equivalent to
     * <pre>thisInstance.{@link #intersects(IRectangularArea) intersects}(area) ? {@link #of(IPoint, IPoint)
     * IRectangularArea.of}(
     * thisInstance.{@link #min()}.{@link IPoint#max(IPoint) max}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link IPoint#min(IPoint) min}(area.{@link #max()})) :
     * null</pre>.
     *
     * @param area the second rectangular area.
     * @return intersection of this and the second rectangular area or {@code null} if they do not intersect.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public IRectangularArea intersection(IRectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + area.min.coordinates.length + " instead of " + n);
        }
        long[] newMin = new long[n];
        long[] newMax = new long[n];
        for (int k = 0; k < n; k++) {
            newMin[k] = Math.max(min.coordinates[k], area.min.coordinates[k]);
            newMax[k] = Math.min(max.coordinates[k], area.max.coordinates[k]);
            if (newMin[k] > newMax[k]) {
                return null;
            }
        }
        return new IRectangularArea(new IPoint(newMin), new IPoint(newMax));
    }

    /**
     * Returns a list of set-theoretical intersections <b>A</b>&nbsp;&cap;&nbsp;<b>B<sub><i>i</i></sub></b>
     * of this rectangular area (<b>A</b>) and all rectangular areas (<b>B<sub><i>i</i></sub></b>), specified
     * by <code>areas</code> argument.
     * If the passed collection doesn't contain areas, intersecting this area, the result will be an empty list.
     * <p>Equivalent to the following loop:
     * <pre>
     * final List&lt;IRectangularArea>gt; result = ... (some empty list);
     * for (IRectangularArea area : areas) {
     *     IRectangularArea intersection = {@link #intersection(IRectangularArea) intersection}(area);
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
    public List<IRectangularArea> intersection(Collection<IRectangularArea> areas) {
        Objects.requireNonNull(areas, "Null areas argument");
        final List<IRectangularArea> result = new ArrayList<>();
        for (IRectangularArea area : areas) {
            IRectangularArea intersection = intersection(area);
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
     * i.e. this area <b>A</b> is a subset of the passed one <b>B</b>. In this case, this method does nothing.
     *
     * <p>It is possible that the difference is equal to this area
     * (<b>A</b>&nbsp;\&nbsp;<b>B</b>&nbsp;=&nbsp;<b>A</b>),
     * i.e. this area <b>A</b> does not intersect the passed one <b>B</b>.
     * In this case, this method is equivalent to <code>results.add(thisInstance)</code> call.
     *
     * <p>In other cases, there is more than 1 way to represent the resulting difference
     * in a form of union of several rectangular areas
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
    public Collection<IRectangularArea> difference(Collection<IRectangularArea> results, IRectangularArea area) {
        Objects.requireNonNull(results, "Null results argument");
        if (!intersects(area)) { // also checks number of dimensions
            results.add(this);
            return results;
        }
        long[] min = this.min.coordinates.clone();
        long[] max = this.max.coordinates.clone();
        for (int k = min.length - 1; k >= 0; k--) {
            assert area.max.coordinates[k] >= min[k] && area.min.coordinates[k] <= max[k]; // because they intersect
            if (area.min.coordinates[k] > this.min.coordinates[k]) {
                min[k] = this.min.coordinates[k];
                max[k] = area.min.coordinates[k] - 1;
                results.add(new IRectangularArea(IPoint.of(min), IPoint.of(max)));
            }
            if (area.max.coordinates[k] < this.max.coordinates[k]) {
                min[k] = area.max.coordinates[k] + 1;
                max[k] = this.max.coordinates[k];
                results.add(new IRectangularArea(IPoint.of(min), IPoint.of(max)));
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
     * for (IRectangularArea area : whatToSubtract) {
     *     for (int i = 0, n = fromWhatToSubtract.size(); i &lt; n; i++) {
     *         IRectangularArea minuend = fromWhatToSubtract.poll();
     *         minuend.{@link #difference(Collection, IRectangularArea) difference}(fromWhatToSubtract, area);
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
     * @throws IllegalArgumentException if some of the elements of the passed collections
     *                                  have different {@link #coordCount()}.
     * @see #subtractCollection(java.util.Queue, IRectangularArea...)
     */
    public static Queue<IRectangularArea> subtractCollection(
            Queue<IRectangularArea> fromWhatToSubtract,
            Collection<IRectangularArea> whatToSubtract) {
        Objects.requireNonNull(fromWhatToSubtract, "Null fromWhatToSubtract");
        Objects.requireNonNull(whatToSubtract, "Null whatToSubtract");
        for (IRectangularArea area : whatToSubtract) {
            for (int i = 0, n = fromWhatToSubtract.size(); i < n; i++) {
                IRectangularArea minuend = fromWhatToSubtract.poll();
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
    public static Queue<IRectangularArea> subtractCollection(
            Queue<IRectangularArea> fromWhatToSubtract,
            IRectangularArea... whatToSubtract) {
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
    public Queue<IRectangularArea> subtract(Collection<IRectangularArea> whatToSubtract) {
        Objects.requireNonNull(whatToSubtract, "Null whatToSubtract");
        Queue<IRectangularArea> difference = new ArrayDeque<>();
        difference.add(this);
        IRectangularArea.subtractCollection(difference, whatToSubtract);
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
    public Queue<IRectangularArea> subtract(IRectangularArea... whatToSubtract) {
        return subtract(java.util.Arrays.asList(whatToSubtract));
    }

    /**
     * Returns the minimal rectangular area, containing this area and the given point.
     * In the returned area, the {@link #min() minimal vertex} is equal to
     * <code>thisInstance.{@link #min()}.{@link IPoint#min(IPoint) min}(point)</code> and
     * the {@link #max() maximal vertex} is equal to
     * <code>thisInstance.{@link #max()}.{@link IPoint#max(IPoint) max}(point)</code>.
     *
     * @param point some point that should be included to the new rectangular area.
     * @return the expanded rectangular area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>point.{@link IPoint#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the points
     *                                  <code>thisInstance.{@link #min()}.{@link IPoint#min(IPoint) min}(point)</code>
     *                                  and
     *                                  <code>thisInstance.{@link #max()}.{@link IPoint#max(IPoint) max}(point)</code>
     *                                  do not match requirements of {@link #of(IPoint, IPoint)} method.
     */
    public IRectangularArea expand(IPoint point) {
        if (contains(point)) {
            // - also checks number of dimensions
            return this;
        }
        long[] newMin = new long[min.coordinates.length];
        long[] newMax = new long[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = Math.min(min.coordinates[k], point.coordinates[k]);
            newMax[k] = Math.max(max.coordinates[k], point.coordinates[k]);
        }
        return of(new IPoint(newMin), new IPoint(newMax));
    }

    /**
     * Returns the minimal rectangular area, containing this and the passed area.
     * Equivalent to
     * <pre>{@link #of(IPoint, IPoint) IRectangularArea.of}(
     * thisInstance.{@link #min()}.{@link IPoint#min(IPoint) min}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link IPoint#max(IPoint) max}(area.{@link #max()}))</pre>.
     *
     * @param area the second rectangular area.
     * @return the minimal rectangular area, containing this and the passed area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>area.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public IRectangularArea expand(IRectangularArea area) {
        if (contains(area)) {
            // - also checks number of dimensions
            return this;
        }
        long[] newMin = new long[min.coordinates.length];
        long[] newMax = new long[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = Math.min(min.coordinates[k], area.min.coordinates[k]);
            newMax[k] = Math.max(max.coordinates[k], area.max.coordinates[k]);
        }
        return new IRectangularArea(new IPoint(newMin), new IPoint(newMax));
    }

    /**
     * Returns the minimal rectangular area, containing all passed areas.
     * Equivalent to the loop of {@link #expand(IRectangularArea)} methods, called for each element
     * of the passed collection, but works faster.
     *
     * <p>If the passed collection is empty, returns {@code null}.
     *
     * @param areas some collection of rectangular areas.
     * @return the minimal rectangular area, containing all them, or {@code null} for empty collection.
     * @throws NullPointerException     if the argument or one of the passed areas is {@code null}.
     * @throws IllegalArgumentException if <code>{@link #coordCount() coordCount()}</code> is not equal for all areas.
     */
    public static IRectangularArea minimalContainingArea(Collection<IRectangularArea> areas) {
        Objects.requireNonNull(areas, "Null areas");
        if (areas.isEmpty()) {
            return null;
        }
        final int coordCount = areas.iterator().next().coordCount();
        final long[] min = new long[coordCount];
        final long[] max = new long[coordCount];
        java.util.Arrays.fill(min, Long.MAX_VALUE);
        java.util.Arrays.fill(max, Long.MIN_VALUE);
        for (IRectangularArea area : areas) {
            if (area.coordCount() != coordCount) {
                throw new IllegalArgumentException("Some areas have different number of dimension: "
                        + area.coordCount() + " and " + coordCount);
            }
            for (int k = 0; k < coordCount; k++) {
                min[k] = Math.min(min[k], area.min(k));
                max[k] = Math.max(max[k], area.max(k));
            }
        }
        return of(new IPoint(min), new IPoint(max));
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
     * @throws IllegalArgumentException if <code>point.{@link IPoint#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public long parallelDistance(IPoint point) {
        Objects.requireNonNull(point, "Null point argument");
        return parallelDistance(point.coordinates);
    }

    /**
     * Equivalent to {@link #parallelDistance(IPoint) parallelDistance}({@link IPoint#of(long...)
     * IPoint.of}(coordinates)), but works faster because does not require creating an instance
     * of {@link IPoint} class.
     *
     * @param coordinates coordinates of some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws NullPointerException     if <code>coordinates</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>coordinates.length</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public long parallelDistance(long... coordinates) {
        Objects.requireNonNull(coordinates, "Null coordinates argument");
        int n = this.min.coordinates.length;
        if (coordinates.length != n) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + coordinates.length + " instead of " + n);
        }
        long min = this.min.coordinates[0];
        long max = this.max.coordinates[0];
        long x = coordinates[0];
        long maxD = Math.max(min - x, x - max);
        for (int k = 1; k < n; k++) {
            min = this.min.coordinates[k];
            max = this.max.coordinates[k];
            long xk = coordinates[k];
            long d = Math.max(min - xk, xk - max);
            if (d > maxD) {
                maxD = d;
            }
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(IPoint) parallelDistance}({@link IPoint#of(long...)
     * IPoint.of}(x, y)), but works faster because does not require allocating any objects.
     * Works only for 2-dimensional rectangular areas, in other cases throws
     * <code>IllegalArgumentException</code>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <code>coordinates.length!=2</code> .
     */
    public long parallelDistance(long x, long y) {
        int n = min.coordinates.length;
        if (n != 2) {
            throw new IllegalArgumentException("Dimensions count mismatch: 2 instead of " + n);
        }
        long min = this.min.coordinates[0];
        long max = this.max.coordinates[0];
        long maxD = Math.max(min - x, x - max);
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        long d = Math.max(min - y, y - max);
        if (d > maxD) {
            maxD = d;
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(IPoint) parallelDistance}({@link IPoint#of(long...)
     * IPoint.of}(x, y, z)), but works faster because does not require allocating any objects.
     * Works only for 3-dimensional rectangular areas, in other cases throws
     * <code>IllegalArgumentException</code>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @param z the 3rd coordinate of some point.
     * @return the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <code>coordinates.length!=2</code> .
     */
    public long parallelDistance(long x, long y, long z) {
        int n = min.coordinates.length;
        if (n != 3) {
            throw new IllegalArgumentException("Dimensions count mismatch: 3 instead of " + n);
        }
        long min = this.min.coordinates[0];
        long max = this.max.coordinates[0];
        long maxD = Math.max(min - x, x - max);
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        long d = Math.max(min - y, y - max);
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
    /*Repeat.SectionEnd operationsAndParallelDistance*/

    /**
     * Shifts this rectangular area by the specified vector and returns the shifted area.
     * Equivalent to
     * <pre>{@link #of(IPoint, IPoint)
     * of}(thisInstance.{@link #min()}.{@link IPoint#addExact(IPoint)
     * addExact}(vector), thisInstance.{@link #max()}.{@link IPoint#addExact(IPoint) addExact}(vector))</pre>
     *
     * <p>Note: the coordinates of new areas are calculated with overflow control.
     *
     * @param vector the vector which is added to all vertices of this area.
     * @return the shifted area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>vector.{@link IPoint#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the result is illegal due to the integer overflow.
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public IRectangularArea shift(IPoint vector) {
        Objects.requireNonNull(vector, "Null vector argument");
        if (vector.coordinates.length != min.coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + vector.coordinates.length + " instead of " + min.coordinates.length);
        }
        if (vector.isOrigin()) {
            return this;
        }
        return of(min.addExact(vector), max.addExact(vector));
    }

    /**
     * Shifts this rectangular area by <code>vector.{@link IPoint#symmetric() symmetric()}</code>
     * and returns the shifted area.
     * Equivalent to
     * <pre>{@link #of(IPoint, IPoint)
     * of}(thisInstance.{@link #min()}.{@link IPoint#subtractExact(IPoint)
     * subtractExact}(vector), thisInstance.{@link #max()}.{@link IPoint#subtractExact(IPoint) subtractExact}(vector))</pre>
     *
     * <p>Note: the coordinates of new areas are calculated with overflow control.
     *
     * @param vector the vector which is subtracted from all vertices of this area.
     * @return the shifted area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>vector.{@link IPoint#coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the result is illegal due to the integer overflow.
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public IRectangularArea shiftBack(IPoint vector) {
        Objects.requireNonNull(vector, "Null vector argument");
        if (vector.coordinates.length != min.coordinates.length) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + vector.coordinates.length + " instead of " + min.coordinates.length);
        }
        if (vector.isOrigin()) {
            return this;
        }
        return of(min.subtractExact(vector), max.subtractExact(vector));
    }

    /**
     * Returns this rectangular area, dilated (expanded) according the argument. More precisely,
     * returns
     * <pre>IRectangularArea.of(
     * thisInstance.{@link #min() min()}.{@link IPoint#subtractExact(IPoint) subtractExact}(expansion),
     * thisInstance.{@link #max() max()}.{@link IPoint#addExact(IPoint) addExact}(expansion))</pre>
     *
     * <p>(but if <code>expansion.{@link IPoint#isOrigin() isOrigin()}</code>,
     * returns this object without changes).</p>
     *
     * @param expansion how to dilate this area.
     * @return dilated area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>expansion.{@link #coordCount() coordCount()}</code> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the result area will be incorrect (see comments to
     *                                  {@link #of(IPoint, IPoint)} method).
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public IRectangularArea dilate(IPoint expansion) {
        Objects.requireNonNull(expansion, "Null expansion");
        if (expansion.coordCount() != coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + expansion.coordCount() + " instead of " + coordCount());
        }
        if (expansion.isOrigin()) {
            return this;
        }
        return of(min().subtractExact(expansion), max().addExact(expansion));
    }

    /**
     * Equivalent to <code>3{@link #dilate(IPoint) dilate}(IPoint.ofEqualCoordinates(thisObjects.{@link
     * #coordCount() coordCount()}, expansion)</code>.
     *
     * @param expansion how to dilate this area.
     * @return dilated area.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the result area will be incorrect (see comments to
     *                                  {@link #of(IPoint, IPoint)} method).
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public IRectangularArea dilate(long expansion) {
        return dilate(IPoint.ofEqualCoordinates(coordCount(), expansion));
    }

    /**
     * Returns this area, dilated according the argument only <i>along coordinate axes</i>,
     * without full hypercube areas near vertices (like in {@link #dilate(IPoint)} method).
     *
     * <p>More precisely, the result is a list, consisting of this area and (usually) 2*{@link #coordCount()}
     * rectangular areas, lying along facets of this area, like in the following picture:
     * <pre>
     *     aaaaaaaaaaaa
     *   bb<b>RRRRRRRRRRRR</b>cc
     *   bb<b>RRRRRRRRRRRR</b>cc
     *   bb<b>RRRRRRRRRRRR</b>cc
     *     ddddddddddd
     * </pre>
     * This figure shows dilation of some 2-dimensional rectangle <code><b>R</b></code> by
     * expansion=<code>IPoint.of(2,1)</code>:
     * the results consist of the original rectangle and 4 rectangles <code>a</code>, <code>b</code> (height 1) and
     * <code>c</code>, <code>d</code> (width 2).
     *
     * <p>Note: all coordinates of <code>expansion</code> argument <b>must</b> be non-negative
     * (unlike {@link #dilate(IPoint)} method).
     *
     * <p>Note: the coordinates of new areas are calculated with overflow control:
     * if the result cannot be exactly represented by 64-bit <code>long</code> integers,
     * this method throws <code>ArithmeticException</code>.
     *
     * <p>If some coordinates of the point <code>expansion</code> are zero, new areas along the corresponding
     * facets are not added (recanglar area cannot be empty).
     * In particular, if <code>expansion.{@link IPoint#isOrigin() isOrigin()}</code>,
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
     *                                  {@link #of(IPoint, IPoint)} method).
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public List<IRectangularArea> dilateStraightOnly(List<IRectangularArea> results, IPoint expansion) {
        Objects.requireNonNull(results, "Null results");
        Objects.requireNonNull(expansion, "Null expansion");
        results.add(this);
        final int coordCount = coordCount();
        if (expansion.coordCount() != coordCount) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                    + expansion.coordCount() + " instead of " + coordCount);
        }
        final long[] min = this.min.coordinates();
        final long[] max = this.max.coordinates();
        for (int k = 0; k < coordCount; k++) {
            final long delta = expansion.coordinates[k];
            if (delta == 0) {
                continue;
            }
            if (delta < 0) {
                throw new IllegalArgumentException("Negative expansion is impossible: " + expansion);
            }
            final long saveMin = min[k];
            final long saveMax = max[k];
            min[k] = IPoint.subtractExact(saveMin, delta);
            max[k] = IPoint.subtractExact(saveMin, 1);
            results.add(of(IPoint.of(min), IPoint.of(max)));
            min[k] = IPoint.addExact(saveMax, 1);
            max[k] = IPoint.addExact(saveMax, delta);
            results.add(of(IPoint.of(min), IPoint.of(max)));
            min[k] = saveMin;
            max[k] = saveMax;
        }
        return results;
    }

    /**
     * Equivalent to <code>{@link #dilateStraightOnly(List, IPoint)
     * dilateStraightOnly}(results, IPoint.ofEqualCoordinates(thisObjects.{@link
     * #coordCount() coordCount()}, expansion)</code>.
     *
     * @param results   the list to store results (new areas will be added to the end of this list).
     * @param expansion how to dilate this area.
     * @return a reference to the <code>results</code> argument.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>expansion &lt; 0</code>
     *                                  or if the result area will be incorrect (see comments to
     *                                  {@link #of(IPoint, IPoint)} method).
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public List<IRectangularArea> dilateStraightOnly(List<IRectangularArea> results, long expansion) {
        return dilateStraightOnly(results, IPoint.ofEqualCoordinates(coordCount(), expansion));
    }

    /**
     * Dilates all areas, specified by the argument, by {@link #dilate(IPoint) dilate} or
     * {@link #dilateStraightOnly(List, IPoint) dilateStraightOnly} method,
     * and returns the list of dilated areas.
     * <p>If <code>straightOnly</code> argument is <code>false</code>, this method is equivalent to the following code:
     * <pre>
     * final List&lt;IRectangularArea&gt; result = new ArrayList&lt;IRectangularArea&gt;();
     * for (IRectangularArea area : areas) {
     *     result.add(area.{@link #dilate(IPoint) dilate}(expansion));
     * }</pre>
     * <p>If <code>straightOnly</code> argument is <code>true</code>, this method is equivalent to the following code:
     * <pre>
     * final List&lt;IRectangularArea&gt; result = new ArrayList&lt;IRectangularArea&gt;();
     * for (IRectangularArea area : areas) {
     *     area.{@link #dilateStraightOnly(List, IPoint) dilateStraightOnly}(result, expansion);
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
     *                                  {@link #of(IPoint, IPoint)} method).
     * @throws ArithmeticException      in a case of <code>long</code> overflow.
     */
    public static List<IRectangularArea> dilate(
            Collection<IRectangularArea> areas,
            IPoint expansion,
            boolean straightOnly) {
        Objects.requireNonNull(areas, "Null areas");
        final List<IRectangularArea> result = new ArrayList<>();
        for (IRectangularArea area : areas) {
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
     * <code>{@link RectangularArea#of(IRectangularArea) RectangularArea.of}(thisInstance)</code>.
     *
     * @return the rectangular area with the same real coordinates as this one.
     */
    public RectangularArea toRectangularArea() {
        return RectangularArea.of(this);
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
     * Returns <code>true</code> if and only if <code>obj instanceof IRectangularArea</code>,
     * <code>((IRectangularArea)obj).min().equals(this.min())</code> and
     * <code>((IRectangularArea)obj).max().equals(this.max())</code>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return <code>true</code> if the specified object is a rectangular area equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof IRectangularArea
                && ((IRectangularArea) obj).min.equals(this.min) && ((IRectangularArea) obj).max.equals(this.max);
    }

    static IRectangularArea of(IPoint min, IPoint max, boolean ise) {
        Objects.requireNonNull(min, "Null min vertex");
        Objects.requireNonNull(max, "Null max vertex");
        int n = min.coordinates.length;
        if (n != max.coordinates.length) {
            throw new IllegalArgumentException("min.coordCount() = " + n
                    + " does not match max.coordCount() = " + max.coordinates.length);
        }
        for (int k = 0; k < n; k++) {
            if (min.coordinates[k] > max.coordinates[k]) {
                throw IRange.invalidBoundsException("min.coord(" + k + ") > max.coord(" + k + ")"
                        + " (min = " + min + ", max = " + max + ")", ise);
            }
            if (max.coordinates[k] == Long.MAX_VALUE) {
                throw IRange.invalidBoundsException("max.coord(" + k + ") == Long.MAX_VALUE", ise);
            }
            if (min.coordinates[k] <= -Long.MAX_VALUE) {
                throw IRange.invalidBoundsException("min.coord(" + k + ") == Long.MAX_VALUE or Long.MIN_VALUE+1", ise);
            }
            if (max.coordinates[k] - min.coordinates[k] + 1L <= 0L) {
                throw IRange.invalidBoundsException("max.coord(" + k + ") - min.coord(" + k + ")"
                        + " >= Long.MAX_VALUE (min = " + min + ", max = " + max + ")", ise);
            }
        }
        return new IRectangularArea(min, max);
    }
}
