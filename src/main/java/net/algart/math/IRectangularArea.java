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

import java.util.*;

/**
 * <p>Rectangular integer area, i&#46;e&#46;
 * hyperparallelepiped in multidimensional space with integer coordinates of vertices.
 * All edges of the hyperparallelepiped are parallel to coordinate axes.
 * In 1-dimensional case it is an equivalent of {@link IRange} class,
 * in 2-dimensional case it is an analog of the standard <tt>java.awt.Rectangle</tt> class.</p>
 *
 * <p>More precisely, the region, specified by this class, is defined by two <i>n</i>-dimensional points
 * with integer coordinates ({@link IPoint}),
 * named the <i>minimal vertex</i> <b>min</b> and <i>maximal vertex</i> <b>max</b>,
 * and consists of all such points
 * <nobr>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>, that:</p>
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
 * <nobr><tt>-Long.MAX_VALUE+1..Long.MAX_VALUE-1</tt></nobr>,
 * and their difference is always <i>less</i> than <tt>Long.MAX_VALUE</tt>.
 * In other words,
 * <nobr>"<tt><b>max</b>.{@link IPoint#coord(int)
 * coord(<i>i</i>)}-<b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}+1</tt>"</nobr> expression,
 * returned by {@link #size(int)} method, and also
 * <nobr>"<tt><b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}-1</tt>"</nobr>,
 * <nobr>"<tt><b>min</b>.{@link IPoint#coord(int) coord(<i>i</i>)}-2</tt>"</nobr> and
 * <nobr>"<tt><b>max</b>.{@link IPoint#coord(int) coord(<i>i</i>)}+1</tt>"</nobr> expressions
 * are always calculated without overflow,
 * and the <nobr>{@link #range(int)}</nobr> method is always possible to return an allowed range.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
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
     * @return    the new rectangular area "between" these vertices.
     * @throws NullPointerException     if one of arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in <tt>min</tt>
     *                                  and <tt>max</tt> points are different,
     *                                  or if, for some <i>i</i>,
     *                                  <tt>min.{@link IPoint#coord(int) coord}(<i>i</i>)
     *                                  &gt; max.{@link IPoint#coord(int) coord}(<i>i</i>)</tt>,
     *                                  or if <tt><nobr>max.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>)-min.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>)+1</nobr>
     *                                  &gt; Long.MAX_VALUE</tt>
     *                                  (more precisely, if this Java expression is nonpositive
     *                                  due to integer overflow),
     *                                  or if <tt><nobr>min.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>) &lt;= -Long.MAX_VALUE</nobr></tt>,
     *                                  or if <tt><nobr>max.{@link IPoint#coord(int)
     *                                  coord}(<i>i</i>) == Long.MAX_VALUE</nobr></tt>.
     */
    public static IRectangularArea valueOf(IPoint min, IPoint max) {
        return valueOf(min, max, false);
    }

    /**
     * Returns the Cartesian product of the specified coordinate ranges.
     * More precisely, return an <i>n</i>-dimensional {@link IRectangularArea rectangular area}
     * with the minimal vertex <b>min</b> and maximal vertex <b>max</b>, where
     * <i>n</i><tt>=coordRanges.length</tt>,
     * <b>min</b>.{@link IPoint#coord(int)
     * coord(<i>i</i>)}<tt>=coordRanges[<i>i</i>].{@link IRange#min() min()}</tt>,
     * <b>max</b>.{@link IPoint#coord(int)
     * coord(<i>i</i>)}<tt>=coordRanges[<i>i</i>].{@link IRange#max() max()}</tt>.
     * See the {@link IRectangularArea comments to this class} for more details.
     *
     * @param coordRanges the coordinate ranges.
     * @return            the Cartesian product of the specified coordinate ranges.
     * @throws NullPointerException     if the argument is <tt>null</tt>
     *                                  or if one of specified <tt>coordRanges</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no ranges are passed).
     */
    public static IRectangularArea valueOf(IRange... coordRanges) {
        if (coordRanges == null)
            throw new NullPointerException("Null coordRanges argument");
        int n = coordRanges.length;
        if (n == 0)
            throw new IllegalArgumentException("Empty coordRanges array");
        coordRanges = coordRanges.clone();
        // cloning before checking guarantees correct check while multithreading
        for (int k = 0; k < n; k++) {
            if (coordRanges[k] == null)
                throw new NullPointerException("Null coordRanges[" + k + "]");
        }
        long[] min = new long[n];
        long[] max = new long[n];
        for (int k = 0; k < n; k++) {
            min[k] = coordRanges[k].min;
            max[k] = coordRanges[k].max;
        }
        return new IRectangularArea(new IPoint(min), new IPoint(max));
    }

    /**
     * Returns a 1-dimensional rectangular area (range) with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #valueOf(IPoint, IPoint) valueOf}(
     *      {@link IPoint#valueOf(long) IPoint.valueOf}(minX),
     *      {@link IPoint#valueOf(long) IPoint.valueOf}(maxX));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @return the new 1-dimensional rectangular area.
     * @throws IllegalArgumentException in the same situations as {@link #valueOf(IPoint, IPoint)} method.
     */
    public static IRectangularArea valueOf(long minX, long maxX) {
        return valueOf(
                IPoint.valueOf(minX),
                IPoint.valueOf(maxX));
    }

    /**
     * Returns a 2-dimensional rectangle with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #valueOf(IPoint, IPoint) valueOf}(
     *      {@link IPoint#valueOf(long...) IPoint.valueOf}(minX, minY),
     *      {@link IPoint#valueOf(long...) IPoint.valueOf}(maxX, maxY));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param minY the minimal <i>y</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @param maxY the maximal <i>y</i>-coordinate, inclusive.
     * @return the new 2-dimensional rectangle.
     * @throws IllegalArgumentException in the same situations as {@link #valueOf(IPoint, IPoint)} method.
     */
    public static IRectangularArea valueOf(long minX, long minY, long maxX, long maxY) {
        return valueOf(
            IPoint.valueOf(minX, minY),
            IPoint.valueOf(maxX, maxY));
    }

    /**
     * Returns a 3-dimensional parallelepiped with the given minimal and maximal vertex.
     * Equivalent to
     * <pre>
     * {@link #valueOf(IPoint, IPoint) valueOf}(
     *      {@link IPoint#valueOf(long...) IPoint.valueOf}(minX, minY, minZ),
     *      {@link IPoint#valueOf(long...) IPoint.valueOf}(maxX, maxY, maxZ));
     * </pre>
     *
     * @param minX the minimal <i>x</i>-coordinate, inclusive.
     * @param minY the minimal <i>y</i>-coordinate, inclusive.
     * @param minZ the minimal <i>z</i>-coordinate, inclusive.
     * @param maxX the maximal <i>x</i>-coordinate, inclusive.
     * @param maxY the maximal <i>y</i>-coordinate, inclusive.
     * @param maxZ the maximal <i>z</i>-coordinate, inclusive.
     * @return the new 3-dimensional parallelepiped.
     * @throws IllegalArgumentException in the same situations as {@link #valueOf(IPoint, IPoint)} method.
     */
    public static IRectangularArea valueOf(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        return valueOf(
            IPoint.valueOf(minX, minY, minZ),
            IPoint.valueOf(maxX, maxY, maxZ));
    }

    /**
     * Returns a new rectangular area with the same coordinates as the given area.
     * All <tt>double</tt> coordinates of the passed area are converted
     * to <tt>long</tt> coordinates of the returned area by standard
     * Java typecast <tt>(long)doubleValue</tt>.
     * Equivalent to <tt>{@link #valueOf(IPoint, IPoint) valueOf}({@link IPoint#valueOf(Point)
     * IPoint.valueOf}(area.{@link #min() min()}),&nbsp;{@link IPoint#valueOf(Point)
     * IPoint.valueOf}(area.{@link #max() max()}))</tt>.
     *
     * @param area the real rectangular area.
     * @return     the integer rectangular area with same (cast) coordinates.
     * @throws NullPointerException     if the passed area is <tt>null</tt>.
     * @throws IllegalArgumentException if the points <tt>{@link IPoint#valueOf(Point)
     *                                  IPoint.valueOf}(area.{@link #min() min()})</tt>
     *                                  and <tt>{@link IPoint#valueOf(Point)
     *                                  IPoint.valueOf}(area.{@link #max() max()})</tt>
     *                                  do not match requirements of {@link #valueOf(IPoint, IPoint)} method.
     */
    public static IRectangularArea valueOf(RectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        return valueOf(IPoint.valueOf(area.min), IPoint.valueOf(area.max));
    }

    /**
     * Returns a new rectangular area with the same coordinates as the given area.
     * All <tt>double</tt> coordinates of the passed area are converted
     * to <tt>long</tt> coordinates of the returned area by <tt>StrictMath.round</tt> method.
     * Java typecast <tt>(long)doubleValue</tt>.
     * Equivalent to <tt>{@link #valueOf(IPoint, IPoint) valueOf}({@link IPoint#roundOf(Point)
     * IPoint.roundOf}(area.{@link #min() min()}),&nbsp;{@link IPoint#roundOf(Point)
     * IPoint.roundOf}(area.{@link #max() max()}))</tt>.
     *
     * @param area the real rectangular area.
     * @return     the integer rectangular area with same (rounded) coordinates.
     * @throws NullPointerException     if the passed area is <tt>null</tt>.
     * @throws IllegalArgumentException if the points <tt>{@link IPoint#valueOf(Point)
     *                                  IPoint.valueOf}(area.{@link #min() min()})</tt>
     *                                  and <tt>{@link IPoint#valueOf(Point)
     *                                  IPoint.valueOf}(area.{@link #max() max()})</tt>
     *                                  do not match requirements of {@link #valueOf(IPoint, IPoint)} method.
     */
    public static IRectangularArea roundOf(RectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        return valueOf(IPoint.roundOf(area.min), IPoint.roundOf(area.max));
    }

    /**
     * Returns the number of dimensions of this rectangular area.
     * Equivalent to <tt>{@link #min()}.{@link IPoint#coordCount() coordCount()}</tt>
     * or <tt>{@link #max()}.{@link IPoint#coordCount() coordCount()}</tt>, but works faster.
     *
     * <p>The result of this method is always positive (&gt;0).
     *
     * @return the number of dimensions of this rectangular area.
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
     * Equivalent to <tt>{@link IPoint#valueOf(long...) IPoint.valueOf}({@link #sizes()})</tt>.
     * The coordinates of the returned point are greater by 1 than coordinates of
     * <tt>{@link #max()}.{@link IPoint#subtract(IPoint) subtract}({@link #min()})</tt>.
     *
     * @return all sizes of this rectangular area in a form of {@link IPoint}.
     */
    public IPoint size() {
        return new IPoint(sizes());
    }

    /**
     * Returns all sizes of this rectangular area, decreased by 1, in a form of {@link IPoint}.
     * Equivalent to <tt>{@link IPoint#valueOf(long...) IPoint.valueOf}({@link #widths()})</tt>.
     * The coordinates of the returned point are equal to coordinates of
     * <tt>{@link #max()}.{@link IPoint#subtract(IPoint) subtract}({@link #min()})</tt>.
     *
     * @return all sizes of this rectangular area, decreased by 1, in a form of {@link IPoint}.
     */
    public IPoint width() {
        return new IPoint(widths());
    }

    /**
     * Returns <tt>{@link #min()}.{@link IPoint#coord(int) coord}(coordIndex)</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #min()}.{@link IPoint#coord(int) coord}(coordIndex)</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public long min(int coordIndex) {
        return min.coordinates[coordIndex];
    }

    /**
     * Returns <tt>{@link #max()}.{@link IPoint#coord(int) coord}(coordIndex)</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #max()}.{@link IPoint#coord(int) coord}(coordIndex)</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public long max(int coordIndex) {
        return max.coordinates[coordIndex];
    }

    /**
     * Returns <tt>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex) + 1</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex) + 1</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public long size(int coordIndex) {
        return max.coordinates[coordIndex] - min.coordinates[coordIndex] + 1;
    }

    /**
     * Returns <tt>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public long width(int coordIndex) {
        return max.coordinates[coordIndex] - min.coordinates[coordIndex];
    }

    /**
     * Returns <tt>{@link #min()}.{@link IPoint#x() x()}</tt>.
     *
     * @return  <tt>{@link #min()}.{@link IPoint#x() x()}</tt>.
     */
    public long minX() {
        return min.coordinates[0];
    }

    /**
     * Returns <tt>{@link #max()}.{@link IPoint#x() x()}</tt>.
     *
     * @return  <tt>{@link #max()}.{@link IPoint#x() x()}</tt>.
     */
    public long maxX() {
        return max.coordinates[0];
    }

    /**
     * Returns <tt>{@link #maxX() maxX()} - {@link #minX() minX()} + 1</tt>.
     *
     * @return <tt>{@link #maxX() maxX()} - {@link #minX() minX()} + 1</tt>.
     */
    public long sizeX() {
        return max.coordinates[0] - min.coordinates[0] + 1;
    }

    /**
     * Returns <tt>{@link #maxX() maxX()} - {@link #minX() minX()}</tt>.
     *
     * @return <tt>{@link #maxX() maxX()} - {@link #minX() minX()}</tt>.
     */
    public long widthX() {
        return max.coordinates[0] - min.coordinates[0];
    }

    /**
     * Returns <tt>{@link #min()}.{@link IPoint#y() y()}</tt>.
     *
     * @return  <tt>{@link #min()}.{@link IPoint#y() y()}</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;2</tt>.
     */
    public long minY() {
        if (min.coordinates.length < 2)
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        return min.coordinates[1];
    }

    /**
     * Returns <tt>{@link #max()}.{@link IPoint#y() y()}</tt>.
     *
     * @return  <tt>{@link #max()}.{@link IPoint#y() y()}</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;2</tt>.
     */
    public long maxY() {
        if (min.coordinates.length < 2)
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        return max.coordinates[1];
    }

    /**
     * Returns <tt>{@link #maxY() maxY()} - {@link #minY() minY()} + 1</tt>.
     *
     * @return <tt>{@link #maxY() maxY()} - {@link #minY() minY()} + 1</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;2</tt>.
     */
    public long sizeY() {
        if (min.coordinates.length < 2)
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        return max.coordinates[1] - min.coordinates[1] + 1;
    }

    /**
     * Returns <tt>{@link #maxY() maxY()} - {@link #minY() minY()}</tt>.
     *
     * @return <tt>{@link #maxY() maxY()} - {@link #minY() minY()}</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;2</tt>.
     */
    public long widthY() {
        if (min.coordinates.length < 2)
            throw new IllegalStateException("Cannot get y-coordinates of " + coordCount() + "-dimensional area");
        return max.coordinates[1] - min.coordinates[1];
    }

    /**
     * Returns <tt>{@link #min()}.{@link IPoint#z() z()}</tt>.
     *
     * @return  <tt>{@link #min()}.{@link IPoint#z() z()}</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;3</tt>.
     */
    public long minZ() {
        if (min.coordinates.length < 3)
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        return min.coordinates[2];
    }

    /**
     * Returns <tt>{@link #max()}.{@link IPoint#z() z()}</tt>.
     *
     * @return  <tt>{@link #max()}.{@link IPoint#z() z()}</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;3</tt>.
     */
    public long maxZ() {
        if (min.coordinates.length < 3)
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        return max.coordinates[2];
    }

    /**
     * Returns <tt>{@link #maxZ() maxZ()} - {@link #minZ() minZ()} + 1</tt>.
     *
     * @return <tt>{@link #maxZ() maxZ()} - {@link #minZ() minZ()} + 1</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;3</tt>.
     */
    public long sizeZ() {
        if (min.coordinates.length < 3)
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        return max.coordinates[2] - min.coordinates[2] + 1;
    }

    /**
     * Returns <tt>{@link #maxZ() maxZ()} - {@link #minZ() minZ()}</tt>.
     *
     * @return <tt>{@link #maxZ() maxZ()} - {@link #minZ() minZ()}</tt>.
     * @throws IllegalStateException if <tt>{@link #coordCount()}&lt;3</tt>.
     */
    public long widthZ() {
        if (min.coordinates.length < 3)
            throw new IllegalStateException("Cannot get z-coordinates of " + coordCount() + "-dimensional area");
        return max.coordinates[2] - min.coordinates[2];
    }

    /**
     * Returns the sizes of this rectangular area along all dimensions.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <tt>#k</tt> contains <tt>{@link #size(int) size}(k)</tt>.
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
     * and the element <tt>#k</tt> contains <tt>{@link #width(int) width}(k)</tt>.
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
     * returned by {@link #sizes()} method. This area is calculated in <tt>double</tt> values.
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
     * Returns <tt>{@link IRange}.{@link IRange#valueOf(long, long)
     * valueOf}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link IRange}.{@link IRange#valueOf(long, long)
     *                   valueOf}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</tt>.
     */
    public IRange range(int coordIndex) {
        return new IRange(min.coordinates[coordIndex], max.coordinates[coordIndex]);
    }

    /**
     * Returns the projections of this rectangular area to all axes.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <tt>#k</tt> contains <tt>{@link #range(int) range}(k)</tt>.
     *
     * @return the projections of this rectangular area to all axes.
     */
    public IRange[] ranges() {
        IRange[] ranges = new IRange[min.coordinates.length];
        for (int k = 0; k < ranges.length; k++) {
            ranges[k] = IRange.valueOf(min.coordinates[k], max.coordinates[k]);
        }
        return ranges;
    }

    /**
     * Returns <tt>true</tt> if and only if
     * <tt>{@link #min(int) min}(k)&lt;=point.{@link IPoint#coord(int) coord}(k)&lt;={@link #max(int) max}(k)</tt>
     * for all <i>k</i>.
     *
     * @param point the checked point.
     * @return      <tt>true</tt> if this rectangular area contains the given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link IPoint#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean contains(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        int n = min.coordinates.length;
        if (point.coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + point.coordinates.length + " instead of " + n);
        for (int k = 0; k < n; k++) {
            if (point.coordinates[k] < min.coordinates[k] || point.coordinates[k] > max.coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <tt>true</tt> if at least one of the specified <tt>areas</tt> contains the passed <tt>point</tt>
     * (see {@link #contains(IPoint)} method).
     *
     * @param areas list of checked rectangular areas.
     * @param point the checked point.
     * @return      <tt>true</tt> if one of the passed areas contains the given point.
     * @throws NullPointerException     if one of the arguments or one of the areas is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link IPoint#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas.
     */
    public static boolean contains(Collection<IRectangularArea> areas, IPoint point) {
        if (areas == null) {
            throw new NullPointerException("Null areas argument");
        }
        if (point == null) {
            throw new NullPointerException("Null point argument");
        }
        for (IRectangularArea a : areas) {
            if (a.contains(point)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <tt>true</tt> if and only if
     * <tt>{@link #min(int) min}(k)&lt;=area.{@link #min(int) min}(k)</tt>
     * and <tt>area.{@link #max(int) max}(k)&lt;={@link #max(int) max}(k)</tt>
     * for all <i>k</i>.
     *
     * @param area the checked rectangular area.
     * @return     <tt>true</tt> if the checked rectangular area is a subset of this area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean contains(IRectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + area.min.coordinates.length + " instead of " + n);
        for (int k = 0; k < n; k++) {
            if (area.min.coordinates[k] < min.coordinates[k] || area.max.coordinates[k] > max.coordinates[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <tt>true</tt> if at least one of the specified <tt>areas</tt> contains the passed <tt>area</tt>
     * (see {@link #contains(IRectangularArea)} method).
     *
     * @param areas list of checked rectangular areas.
     * @param area  the checked area.
     * @return      <tt>true</tt> if one of the passed areas (1st argument) contains the given area (2nd argument).
     * @throws NullPointerException     if one of the arguments or one of the areas is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of one of areas
     *                                  in the 1st argument.
     */
    public static boolean contains(Collection<IRectangularArea> areas, IRectangularArea area) {
        if (areas == null) {
            throw new NullPointerException("Null areas argument");
        }
        if (area == null) {
            throw new NullPointerException("Null area argument");
        }
        for (IRectangularArea a : areas) {
            if (a.contains(area)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <tt>true</tt> if and only if
     * <tt>{@link #min(int) min}(k)&lt;=area.{@link #max(int) max}(k)</tt>
     * and <tt>area.{@link #min(int) min}(k)&lt;={@link #max(int) max}(k)</tt>
     * for all <i>k</i>.
     *
     * @param area the checked rectangular area.
     * @return     <tt>true</tt> if the checked rectangular area overlaps with this area,
     *             maybe in boundary points only.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean intersects(IRectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + area.min.coordinates.length + " instead of " + n);
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
     * the passed rectangular area (<b>B</b>) or <tt>null</tt> if they
     * do not {@link #intersects(IRectangularArea) intersect}
     * (<b>A</b>&nbsp;&cap;&nbsp;<b>B</b>&nbsp;=&nbsp;&empty;).
     * Equivalent to
     * <pre>thisInstance.{@link #intersects(IRectangularArea) intersects}(area) ? {@link #valueOf(IPoint, IPoint)
     * IRectangularArea.valueOf}(
     * thisInstance.{@link #min()}.{@link IPoint#max(IPoint) max}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link IPoint#min(IPoint) min}(area.{@link #max()})) :
     * null</pre>.
     *
     * @param area the second rectangular area.
     * @return     intersection of this and the second rectangular area or <tt>null</tt> if they do not intersect.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public IRectangularArea intersection(IRectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + area.min.coordinates.length + " instead of " + n);
        long[] newMin = new long[n];
        long[] newMax = new long[n];
        for (int k = 0; k < n; k++) {
            newMin[k] = min.coordinates[k] >= area.min.coordinates[k] ? min.coordinates[k] : area.min.coordinates[k];
            newMax[k] = max.coordinates[k] <= area.max.coordinates[k] ? max.coordinates[k] : area.max.coordinates[k];
            if (newMin[k] > newMax[k]) {
                return null;
            }
        }
        return new IRectangularArea(new IPoint(newMin), new IPoint(newMax));
    }

    /**
     * Returns a list of set-theoretical intersections <b>A</b>&nbsp;&cap;&nbsp;<b>B<sub><i>i</i></sub></b>
     * of this rectangular area (<b>A</b>) and all rectangular areas (<b>B<sub><i>i</i></sub></b>), specified
     * by <tt>areas</tt> argument.
     * If the passed collection doesn't contain areas, intersecting this area, the result will be an empty list.
     * <p>Equivalent to the following loop:
     * <pre>
     * final List<IRectangularArea> result = ... (some empty list);
     * for (IRectangularArea area : areas) {
     *     IRectangularArea intersection = {@link #intersection(IRectangularArea) intersection}(area);
     *     if (intersection != null) {
     *         result.add(intersection);
     *     }
     * }
     * </pre>.
     *
     * @param areas collection of areas (we find intersection with each from them).
     * @return     intersection of this and the second rectangular area or <tt>null</tt> if they do not intersect.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if this rectangular area or some of elements of the passed collection
     *                                  have different {@link #coordCount()}.
     */
    public List<IRectangularArea> intersection(Collection<IRectangularArea> areas) {
        if (areas == null) {
            throw new NullPointerException("Null areas argument");
        }
        final List<IRectangularArea> result = new ArrayList<IRectangularArea>();
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
     * <nobr><b>R</b><sub>1</sub>,<b>R</b><sub>2</sub>,...,<b>R</b><sub><i>N</i></sub></nobr>,
     * the set-theoretical union of which is equal to this difference
     * (<nobr><b>R</b><sub>1</sub>&cup;<b>R</b><sub>2</sub>&cup;...&cup;<b>R</b><sub><i>N</i></sub> =
     * <b>A</b>&nbsp;\&nbsp;<b>B</b>)</nobr>.
     * The resulting areas <nobr><b>R</b><sub>1</sub>,<b>R</b><sub>2</sub>,...,<b>R</b><sub><i>N</i></sub></nobr>
     * are added into the collection <tt>results</tt> by <tt>Collection.add(...)</tt> method.
     * So, the collection <tt>results</tt> must be not-null and support adding elements
     * (usually it is <tt>List</tt> or <tt>Queue</tt>).
     *
     * <p>It is possible that the difference is empty (<b>A</b>&nbsp;\&nbsp;<b>B</b>&nbsp;=&nbsp;&empty;),
     * i.e. this area <b>A</b> is a subset of the passed one <b>B</b>. In this case, this method does nothing.
     *
     * <p>It is possible that the difference is equal to this area
     * (<b>A</b>&nbsp;\&nbsp;<b>B</b>&nbsp;=&nbsp;<b>A</b>),
     * i.e. this area <b>A</b> does not intersect the passed one <b>B</b>.
     * In this case, this method is equivalent to <tt>results.add(thisInstance)</tt> call.
     *
     * <p>In other cases, there is more than 1 way to represent the resulting difference
     * in a form of union of several rectangular areas
     * <nobr><b>R</b><sub>1</sub>,<b>R</b><sub>2</sub>,...,<b>R</b><sub><i>N</i></sub></nobr>.
     * The precise way, how this method forms this set of rectangular areas <b>R</b><sub><i>i</i></sub>,
     * is not documented, but this method tries to minimize the number <i>N</i> of such areas.
     * In any case, there is a guarantee that <i>N</i>&le;2*{@link #coordCount()}.
     *
     * @param results the collection to store results.
     * @param area    the area <b>B</b>, subtracted from this area <b>A</b>.
     * @return        a reference to the <tt>results</tt> argument.
     * @throws NullPointerException     if <tt>result</tt> or <tt>area</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     * @see #subtractCollection(java.util.Queue, java.util.Collection)
     */
    public Collection<IRectangularArea> difference(Collection<IRectangularArea> results, IRectangularArea area) {
        if (results == null)
            throw new NullPointerException("Null results argument");
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
                results.add(new IRectangularArea(IPoint.valueOf(min), IPoint.valueOf(max)));
            }
            if (area.max.coordinates[k] < this.max.coordinates[k]) {
                min[k] = area.max.coordinates[k] + 1;
                max[k] = this.max.coordinates[k];
                results.add(new IRectangularArea(IPoint.valueOf(min), IPoint.valueOf(max)));
            }
            min[k] = Math.max(area.min.coordinates[k], this.min.coordinates[k]);
            max[k] = Math.min(area.max.coordinates[k], this.max.coordinates[k]);
            // - intersection of two ranges area.min-max[k] and this.min-max[k]
        }
        return results;
    }

    /**
     * Calculates the set-theoretical difference <b>A</b>&nbsp;\&nbsp;<b>B</b> of
     * the set-theoretical union <b>A</b> of all elements of the collection <tt>fromWhatToSubtract</tt>
     * and the set-theoretical union <b>B</b> of all elements of the collection <tt>whatToSubtract</tt>,
     * in a form of a union of <i>N</i> rectangular areas, and replaces
     * the old content of <tt>fromWhatToSubtract</tt> with the resulting <i>N</i> areas.
     *
     * <p>More precisely, this method is equivalent to the following loop:
     *
     * <pre>
     * for (IRectangularArea area : whatToSubtract) {
     *     for (int i = 0, n = fromWhatToSubtract.size(); i < n; i++) {
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
     * some elements of the collections are <tt>null</tt> or have different number of dimensions),
     * the <tt>fromWhatToSubtract</tt> stays partially modified.
     * In other words, this method <b>is non-atomic regarding failures</b>.
     *
     * @param fromWhatToSubtract the minuend <b>A</b>, which will be replaced with <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @param whatToSubtract     the subtrahend <b>B</b>.
     * @return                   a reference to <tt>fromWhatToSubtract</tt> argument, which will contain
     *                           the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @throws NullPointerException     if <tt>fromWhatToSubtract</tt> or <tt>whatToSubtract</tt> argument
     *                                  is <tt>null</tt> or if one of their elements it <tt>null</tt>.
     * @throws IllegalArgumentException if some of elements of the passed collections
     *                                  have different {@link #coordCount()}.
     * @see #subtractCollection(java.util.Queue, IRectangularArea...)
     */
    public static Queue<IRectangularArea> subtractCollection(
        Queue<IRectangularArea> fromWhatToSubtract,
        Collection<IRectangularArea> whatToSubtract)
    {
        if (fromWhatToSubtract == null)
            throw new NullPointerException("Null fromWhatToSubtract");
        if (whatToSubtract == null)
            throw new NullPointerException("Null whatToSubtract");
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
     * Equivalent to <tt>{@link #subtractCollection(Queue, Collection)
     * subtractCollection}(fromWhatToSubtract, java.util.Arrays.asList(whatToSubtract))</tt>.
     *
     * @param fromWhatToSubtract the minuend <b>A</b>, which will be replaced with <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @param whatToSubtract     the subtrahend <b>B</b>.
     * @return                   a reference to <tt>fromWhatToSubtract</tt> argument, which will contain
     *                           the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>.
     * @throws NullPointerException     if <tt>fromWhatToSubtract</tt> or <tt>whatToSubtract</tt> argument
     *                                  is <tt>null</tt> or if one of their elements it <tt>null</tt>.
     * @throws IllegalArgumentException if some of elements of the passed collection and array
     *                                  have different {@link #coordCount()}.
     */
    public static Queue<IRectangularArea> subtractCollection(
        Queue<IRectangularArea> fromWhatToSubtract,
        IRectangularArea... whatToSubtract)
    {
        return subtractCollection(fromWhatToSubtract, java.util.Arrays.asList(whatToSubtract));
    }

    /**
     * Equivalent to <tt>{@link #subtractCollection(Queue, Collection)
     * subtractCollection}(fromWhatToSubtract, whatToSubtract</tt>,
     * where <tt>fromWhatToSubtract</tt> contains this object as the only element.
     *
     * @param whatToSubtract     the subtrahend <b>B</b>.
     * @return                   new collection, containing the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>
     *                           (<b>A</b> = this object, <b>B</b> = union of all <tt>whatToSubtract</tt>).
     * @throws NullPointerException     if <tt>whatToSubtract</tt> argument
     *                                  is <tt>null</tt> or if one of their elements it <tt>null</tt>.
     * @throws IllegalArgumentException if this rectangular area or some of elements of the passed collection
     *                                  have different {@link #coordCount()}.
     */
    public Queue<IRectangularArea> subtract(Collection<IRectangularArea> whatToSubtract) {
        if (whatToSubtract == null) {
            throw new NullPointerException("Null whatToSubtract");
        }
        Queue<IRectangularArea> difference = new ArrayDeque<IRectangularArea>();
        difference.add(this);
        IRectangularArea.subtractCollection(difference, whatToSubtract);
        return difference;
    }

    /**
     * Equivalent to <tt>{@link #subtract(Collection)
     * subtract}(java.util.Arrays.asList(whatToSubtract))</tt>.
     *
     * @param whatToSubtract     the subtrahend <b>B</b>.
     * @return                   new collection, containing the difference <b>A</b>&nbsp;\&nbsp;<b>B</b>
     *                           (<b>A</b> = this object, <b>B</b> = union of all <tt>whatToSubtract</tt>).
     * @throws NullPointerException     if <tt>whatToSubtract</tt> argument
     *                                  is <tt>null</tt> or if one of their elements it <tt>null</tt>.
     * @throws IllegalArgumentException if this rectangular area or some of elements of the passed array
     *                                  have different {@link #coordCount()}.
     */
    public Queue<IRectangularArea> subtract(IRectangularArea... whatToSubtract) {
        return subtract(java.util.Arrays.asList(whatToSubtract));
    }

    /**
     * Returns the minimal rectangular area, containing this area and the given point.
     * In the returned area, the {@link #min() minimal vertex} is equal to
     * <tt>thisInstance.{@link #min()}.{@link IPoint#min(IPoint) min}(point)</tt> and
     * the {@link #max() maximal vertex} is equal to
     * <tt>thisInstance.{@link #max()}.{@link IPoint#max(IPoint) max}(point)</tt>.
     *
     * @param point some point that should be included to the new rectangular area.
     * @return      the expanded rectangular area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link IPoint#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the points
     *                                  <tt>thisInstance.{@link #min()}.{@link IPoint#min(IPoint) min}(point)</tt>
     *                                  and
     *                                  <tt>thisInstance.{@link #max()}.{@link IPoint#max(IPoint) max}(point)</tt>
     *                                  do not match requirements of {@link #valueOf(IPoint, IPoint)} method.
     */
    public IRectangularArea expand(IPoint point) {
        if (contains(point)) {
            return this;
        }
        long[] newMin = new long[min.coordinates.length];
        long[] newMax = new long[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = min.coordinates[k] <= point.coordinates[k] ? min.coordinates[k] : point.coordinates[k];
            newMax[k] = max.coordinates[k] >= point.coordinates[k] ? max.coordinates[k] : point.coordinates[k];
        }
        return valueOf(new IPoint(newMin), new IPoint(newMax));
    }

    /**
     * Returns the minimal rectangular area, containing this and the passed area.
     * Equivalent to
     * <pre>{@link #valueOf(IPoint, IPoint) IRectangularArea.valueOf}(
     * thisInstance.{@link #min()}.{@link IPoint#min(IPoint) min}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link IPoint#max(IPoint) max}(area.{@link #max()}))</pre>.
     *
     * @param area the second rectangular area.
     * @return     the minimal rectangular area, containing this and the passed area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public IRectangularArea expand(IRectangularArea area) {
        if (contains(area)) {
            return this;
        }
        long[] newMin = new long[min.coordinates.length];
        long[] newMax = new long[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = min.coordinates[k] <= area.min.coordinates[k] ? min.coordinates[k] : area.min.coordinates[k];
            newMax[k] = max.coordinates[k] >= area.max.coordinates[k] ? max.coordinates[k] : area.max.coordinates[k];
        }
        return new IRectangularArea(new IPoint(newMin), new IPoint(newMax));
    }

    /**
     * Returns the <i>parallel distance</i> from the given point to this rectangular area.
     * The parallel distance is a usual distance, with plus or minus sign,
     * from the point to some of hyperplanes, containing the hyperfacets of this hyperparallelepiped,
     * chosen so that:
     *
     * <ol>
     * <li>the parallel distance is zero at the hyperfacets, negative inside the rectangular area and
     * positive outside it;</li>
     * <li>for any constant <i>c</i>,
     * the set of all such points, that the parallel distance from them to this rectangular area &le;<i>c</i>,
     * is also hyperparallelepiped (rectangular area) wich hyperfacets,
     * parallel to the the coordinate hyperplanes,
     * or an empty set if <i>c</i>&lt;<i>c</i><sub>0</sub>, where <i>c</i><sub>0</sub> is the (negative)
     * parallel distance from the geometrical center of this hyperparallelepiped.</li>
     * </ol>
     *
     * <p>Formally, let <b>p</b> is any point with coordinates
     * <nobr><i>p</i><sub>0</sub>, <i>p</i><sub>1</sub>, ..., <i>p</i><sub><i>n</i>&minus;1</sub></nobr>,
     * <i>l<sub>i</i></sub> = {@link #min(int) min}(<i>i</i>),
     * <i>r<sub>i</i></sub> = {@link #max(int) max}(<i>i</i>),
     * <i>d<sub>i</i></sub> = max(<i>l<sub>i</sub></i>&minus;<i>p<sub>i</sub></i>,
     * <i>p<sub>i</sub></i>&minus;<i>r<sub>i</sub></i>).
     * Note that <i>d<sub>i</i></sub> is positive if <i>p<sub>i</sub></i>&lt;<i>l<sub>i</sub></i>
     * or <i>p<sub>i</sub></i>&gt;<i>r<sub>i</sub></i> and negative if <i>p<sub>i</sub></i>
     * is inside <i>l<sub>i</sub></i>..<i>r<sub>i</sub></i> range.
     * The <i>parallel distance</i> from the point <b>p</b> to this rectangular area
     * is defined as maximal value from all <i>d<sub>i</sub></i>:
     * <nobr>max(<i>d</i><sub>0</sub>, <i>d</i><sub>1</sub>, ..., <i>d</i><sub><i>n</i>&minus;1</sub>)</nobr>.
     *
     *
     * @param point some point.
     * @return      the parallel distance from this point to this rectangular area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link IPoint#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public long parallelDistance(IPoint point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        return parallelDistance(point.coordinates);
    }

    /**
     * Equivalent to {@link #parallelDistance(IPoint) parallelDistance}({@link IPoint#valueOf(long...)
     * IPoint.valueOf}(coordinates)), but works faster because does not require to create an instance
     * of {@link IPoint} class.
     *
     * @param coordinates coordinates of some point.
     * @return      the parallel distance from this point to this rectangular area.
     * @throws NullPointerException     if <tt>coordinates</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>coordinates.length</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public long parallelDistance(long... coordinates) {
        if (coordinates == null)
            throw new NullPointerException("Null coordinates argument");
        int n = this.min.coordinates.length;
        if (coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + coordinates.length + " instead of " + n);
        long min = this.min.coordinates[0];
        long max = this.max.coordinates[0];
        long x = coordinates[0];
        long maxD = min - x >= x - max ? min - x : x - max;
        for (int k = 1; k < n; k++) {
            min = this.min.coordinates[k];
            max = this.max.coordinates[k];
            long xk = coordinates[k];
            long d = min - xk >= xk - max ? min - xk : xk - max;
            if (d > maxD) {
                maxD = d;
            }
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(IPoint) parallelDistance}({@link IPoint#valueOf(long...)
     * IPoint.valueOf}(x, y)), but works faster because does not require to allocate any objects.
     * Works only for 2-dimensional rectangular areas, in other cases throws
     * <tt>IllegalArgumentException</tt>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @return  the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <tt>coordinates.length!=2</tt> .
     */
    public long parallelDistance(long x, long y) {
        int n = min.coordinates.length;
        if (n != 2)
            throw new IllegalArgumentException("Dimensions count mismatch: 2 instead of " + n);
        long min = this.min.coordinates[0];
        long max = this.max.coordinates[0];
        long maxD = min - x >= x - max ? min - x : x - max;
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        long d = min - y >= y - max ? min - y : y - max;
        if (d > maxD) {
            maxD = d;
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(IPoint) parallelDistance}({@link IPoint#valueOf(long...)
     * IPoint.valueOf}(x, y, z)), but works faster because does not require to allocate any objects.
     * Works only for 3-dimensional rectangular areas, in other cases throws
     * <tt>IllegalArgumentException</tt>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @param z the 3rd coordinate of some point.
     * @return  the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <tt>coordinates.length!=2</tt> .
     */
    public long parallelDistance(long x, long y, long z) {
        int n = min.coordinates.length;
        if (n != 3)
            throw new IllegalArgumentException("Dimensions count mismatch: 3 instead of " + n);
        long min = this.min.coordinates[0];
        long max = this.max.coordinates[0];
        long maxD = min - x >= x - max ? min - x : x - max;
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        long d = min - y >= y - max ? min - y : y - max;
        if (d > maxD) {
            maxD = d;
        }
        min = this.min.coordinates[2];
        max = this.max.coordinates[2];
        d = min - z >= z - max ? min - z : z - max;
        if (d > maxD) {
            maxD = d;
        }
        return maxD;
    }
    /*Repeat.SectionEnd operationsAndParallelDistance*/

    /**
     * Shifts this rectangular area by the specified vector and returns the shifted area.
     * Equivalent to
     * <pre>{@link #valueOf(IPoint, IPoint)
     * valueOf}(thisInstance.{@link #min()}.{@link IPoint#add(IPoint)
     * add}(vector), thisInstance.{@link #max()}.{@link IPoint#add(IPoint) add}(vector))</pre>
     * Note that integer overflow is not checked here!
     *
     * @param vector the vector which is added to all vertices of this area.
     * @return       the shifted area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>vector.{@link IPoint#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the result is illegal due to the integer overflow.
     */
    public IRectangularArea shift(IPoint vector) {
        if (vector == null)
            throw new NullPointerException("Null vector argument");
        if (vector.coordinates.length != min.coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + vector.coordinates.length + " instead of " + min.coordinates.length);
        if (vector.isOrigin()) {
            return this;
        }
        return IRectangularArea.valueOf(min.add(vector), max.add(vector));
    }

    /**
     * Shifts this rectangular area by <tt>vector.{@link IPoint#symmetric() symmetric()}</tt>
     * and returns the shifted area.
     * Equivalent to
     * <pre>{@link #valueOf(IPoint, IPoint)
     * valueOf}(thisInstance.{@link #min()}.{@link IPoint#subtract(IPoint)
     * subtract}(vector), thisInstance.{@link #max()}.{@link IPoint#subtract(IPoint) subtract}(vector))</pre>
     * Note that integer overflow is not checked here!
     *
     * @param vector the vector which is subtracted from all vertices of this area.
     * @return       the shifted area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>vector.{@link IPoint#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the result is illegal due to the integer overflow.
     */
    public IRectangularArea shiftBack(IPoint vector) {
        if (vector == null)
            throw new NullPointerException("Null vector argument");
        if (vector.coordinates.length != min.coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + vector.coordinates.length + " instead of " + min.coordinates.length);
        if (vector.isOrigin()) {
            return this;
        }
        return IRectangularArea.valueOf(min.subtract(vector), max.subtract(vector));
    }

    /**
     * Equivalent to <tt>{@link RectangularArea#valueOf(IRectangularArea) RectangularArea.valueOf}(thisInstance)</tt>.
     *
     * @return the rectangular area with same real coordinates as this one.
     */
    public RectangularArea toRectangularArea() {
        return RectangularArea.valueOf(this);
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
     * Returns <tt>true</tt> if and only if <tt>obj instanceof IRectangularArea</tt>,
     * <tt>((IRectangularArea)obj).min().equals(this.min())</tt> and
     * <tt>((IRectangularArea)obj).max().equals(this.max())</tt>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <tt>true</tt> if the specified object is a rectangular area equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof IRectangularArea
            && ((IRectangularArea) obj).min.equals(this.min) && ((IRectangularArea) obj).max.equals(this.max);
    }

    static IRectangularArea valueOf(IPoint min, IPoint max, boolean ise) {
        if (min == null)
            throw new NullPointerException("Null min vertex");
        if (max == null)
            throw new NullPointerException("Null max vertex");
        int n = min.coordinates.length;
        if (n != max.coordinates.length)
            throw new IllegalArgumentException("min.coordCount() = " + n
                + " does not match max.coordCount() = " + max.coordinates.length);
        for (int k = 0; k < n; k++) {
            if (min.coordinates[k] > max.coordinates[k])
                throw IRange.invalidBoundsException("min.coord(" + k + ") > max.coord(" + k + ")"
                    + " (min = " + min + ", max = " + max + ")", ise);
            if (max.coordinates[k] == Long.MAX_VALUE)
                throw IRange.invalidBoundsException("max.coord(" + k + ") == Long.MAX_VALUE", ise);
            if (min.coordinates[k] <= -Long.MAX_VALUE)
                throw IRange.invalidBoundsException("min.coord(" + k + ") == Long.MAX_VALUE or Long.MIN_VALUE+1", ise);
            if (max.coordinates[k] - min.coordinates[k] + 1L <= 0L)
                throw IRange.invalidBoundsException("max.coord(" + k + ") - min.coord(" + k + ")"
                    + " >= Long.MAX_VALUE (min = " + min + ", max = " + max + ")", ise);
        }
        return new IRectangularArea(min, max);
    }
}
