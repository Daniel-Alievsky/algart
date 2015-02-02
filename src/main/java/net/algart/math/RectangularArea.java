/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.Collection;
import java.util.Queue;

/**
 * <p>Rectangular real area, i&#46;e&#46;
 * hyperparallelepiped in multidimensional space with real coordinates of vertices.
 * All edges of the hyperparallelepiped are parallel to coordinate axes.
 * In 1-dimensional case it is an equivalent of {@link Range} class,
 * in 2-dimensional case it is an analog of the standard <tt>java.awt.geom.Rectangle2D</tt> class.</p>
 *
 * <p>More precisely, the region, specified by this class, is defined by two <i>n</i>-dimensional points
 * with real coordinates ({@link Point}),
 * named the <i>minimal vertex</i> <b>min</b> and <i>maximal vertex</i> <b>max</b>,
 * and consists of all such points
 * <nobr>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>, that:</p>
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
 * and all coordinates of both vertices are never <tt>Double.NaN</tt>.
 *
 * <p>All calculations in this class are performed in <tt>strictfp</tt> mode, so the result
 * is absolutely identical on all platforms.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 * @see IRectangularArea
 */
public strictfp class RectangularArea {

    final Point min;
    final Point max;

    private RectangularArea(Point min, Point max) {
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
     * @return    the new rectangular area "between" these vertices.
     * @throws NullPointerException     if one of arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the {@link #coordCount() numbers of dimensions} in <tt>min</tt>
     *                                  and <tt>max</tt> points are different,
     *                                  or if, for some <i>i</i>,
     *                                  <tt>min.{@link Point#coord(int) coord}(<i>i</i>)
     *                                  &gt; max.{@link Point#coord(int) coord}(<i>i</i>)</tt>,
     *                                  or if one of these coordinates is <tt>Double.NaN</tt>.
     */
    public static RectangularArea valueOf(Point min, Point max) {
        if (min == null)
            throw new NullPointerException("Null min vertex");
        if (max == null)
            throw new NullPointerException("Null max vertex");
        int n = min.coordinates.length;
        if (n != max.coordinates.length)
            throw new IllegalArgumentException("min.coordCount() = " + n
                + " does not match max.coordCount() = " + max.coordinates.length);
        for (int k = 0; k < n; k++) {
            if (Double.isNaN(min.coordinates[k]))
                throw new IllegalArgumentException("min.coord(" + k + ") is NaN");
            if (Double.isNaN(max.coordinates[k]))
                throw new IllegalArgumentException("max.coord(" + k + ") is NaN");
            if (min.coordinates[k] > max.coordinates[k])
                throw new IllegalArgumentException("min.coord(" + k + ") > max.coord(" + k + ")"
                    + " (min = " + min + ", max = " + max + ")");
        }
        return new RectangularArea(min, max);
    }

    /**
     * Returns the Cartesian product of the specified coordinate ranges.
     * More precisely, return an <i>n</i>-dimensional {@link RectangularArea rectangular area}
     * with the minimal vertex <b>min</b> and maximal vertex <b>max</b>, where
     * <i>n</i><tt>=coordRanges.length</tt>,
     * <b>min</b>.{@link Point#coord(int)
     * coord(<i>i</i>)}<tt>=coordRanges[<i>i</i>].{@link Range#min() min()}</tt>,
     * <b>max</b>.{@link Point#coord(int)
     * coord(<i>i</i>)}<tt>=coordRanges[<i>i</i>].{@link Range#max() max()}</tt>.
     * See the {@link RectangularArea comments to this class} for more details.
     *
     * @param coordRanges the coordinate ranges.
     * @return            the Cartesian product of the specified coordinate ranges.
     * @throws NullPointerException     if the argument is <tt>null</tt>
     *                                  or if one of specified <tt>coordRanges</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no ranges are passed).
     */
    public static RectangularArea valueOf(Range... coordRanges) {
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
        double[] min = new double[n];
        double[] max = new double[n];
        for (int k = 0; k < n; k++) {
            min[k] = coordRanges[k].min;
            max[k] = coordRanges[k].max;
        }
        return new RectangularArea(new Point(min), new Point(max));
    }

    /**
     * Returns a new rectangular area with the same coordinates as the given area.
     * All <tt>long</tt> coordinates of the passed area are converted
     * to <tt>double</tt> coordinates of the returned area by standard
     * Java typecast <tt>(double)longValue</tt>.
     * Equivalent to <tt>{@link #valueOf(Point, Point) valueOf}({@link Point#valueOf(IPoint)
     * Point.valueOf}(iArea.{@link #min() min()}),&nbsp;{@link Point#valueOf(IPoint)
     * Point.valueOf}(iArea.{@link #max() max()}))</tt>.
     *
     * @param iArea the integer rectangular area.
     * @return      the real rectangular area with same coordinates.
     * @throws NullPointerException     if the passed area is <tt>null</tt>.
     */
    public static RectangularArea valueOf(IRectangularArea iArea) {
        if (iArea == null)
            throw new NullPointerException("Null iArea argument");
        return new RectangularArea(Point.valueOf(iArea.min), Point.valueOf(iArea.max));
        // integer min and max, converted to real, are always acceptable for valueOf(Point min, Point max) method
    }

    /**
     * Returns the number of dimensions of this rectangular area.
     * Equivalent to <tt>{@link #min()}.{@link Point#coordCount() coordCount()}</tt>
     * or <tt>{@link #max()}.{@link Point#coordCount() coordCount()}</tt>, but works faster.
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
     * Equivalent to <tt>{@link Point#valueOf(double...) Point.valueOf}({@link #sizes()})</tt>.
     * The returned point is equal to
     * <tt>{@link #max()}.{@link Point#subtract(Point) subtract}({@link #min()})</tt>.
     *
     * @return all sizes of this rectangular area in a form of {@link Point}.
     */
    public Point size() {
        return new Point(sizes());
    }

    /**
     * Returns <tt>{@link #min()}.{@link Point#coord(int) coord}(coordIndex)</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #min()}.{@link Point#coord(int) coord}(coordIndex)</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public double min(int coordIndex) {
        return min.coordinates[coordIndex];
    }

    /**
     * Returns <tt>{@link #max()}.{@link Point#coord(int) coord}(coordIndex)</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #max()}.{@link Point#coord(int) coord}(coordIndex)</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public double max(int coordIndex) {
        return max.coordinates[coordIndex];
    }

    /**
     * Returns <tt>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link #max(int) max}(coordIndex) - {@link #min(int) min}(coordIndex)</tt>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or <tt>coordIndex&gt;={@link #coordCount()}</tt>.
     */
    public double size(int coordIndex) {
        return max.coordinates[coordIndex] - min.coordinates[coordIndex];
    }

    /**
     * Returns the sizes of this rectangular area along all dimensions.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <tt>#k</tt> contains <tt>{@link #size(int) size}(k)</tt>.
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
     * Returns <tt>{@link Range}.{@link Range#valueOf(double, double)
     * valueOf}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</tt>.
     *
     * @param coordIndex the index of the coordinate.
     * @return           <tt>{@link Range}.{@link Range#valueOf(double, double)
     *                   valueOf}({@link #min(int) min}(coordIndex), {@link #max(int) max}(coordIndex))</tt>.
     */
    public Range range(int coordIndex) {
        return new Range(min.coordinates[coordIndex], max.coordinates[coordIndex]);
    }

    /**
     * Returns the projections of this rectangular area to all axes.
     * The returned array consists of {@link #coordCount()} elements,
     * and the element <tt>#k</tt> contains <tt>{@link #range(int) range}(k)</tt>.
     *
     * @return the projections of this rectangular area to all axes.
     */
    public Range[] ranges() {
        Range[] ranges = new Range[min.coordinates.length];
        for (int k = 0; k < ranges.length; k++) {
            ranges[k] = Range.valueOf(min.coordinates[k], max.coordinates[k]);
        }
        return ranges;
    }

    /**
     * Returns <tt>true</tt> if and only if
     * <tt>{@link #min(int) min}(k)&lt;=point.{@link Point#coord(int) coord}(k)&lt;={@link #max(int) max}(k)</tt>
     * for all <i>k</i>.
     *
     * @param point the checked point.
     * @return      <tt>true</tt> if this rectangular area contains the given point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link Point#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean contains(Point point) {
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
    public boolean contains(RectangularArea area) {
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
    public boolean intersects(RectangularArea area) {
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

    /**
     * Returns <tt>true</tt> if and only if
     * <tt>{@link #min(int) min}(k)&lt;area.{@link #max(int) max}(k)</tt>
     * and <tt>area.{@link #min(int) min}(k)&lt;{@link #max(int) max}(k)</tt>
     * for all <i>k</i>.
     *
     * @param area the checked rectangular area.
     * @return     <tt>true</tt> if the checked rectangular area overlaps with this area in some internal points.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public boolean overlaps(RectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + area.min.coordinates.length + " instead of " + n);
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
      (\[k\])\s*[+-]\s*1 ==> $1 !! Auto-generated: NOT EDIT !! */
    /**
     * Returns the set-theoretical intersection <b>A</b>&nbsp;&cap;&nbsp;<b>B</b> of this (<b>A</b>) and
     * the passed rectangular area (<b>B</b>) or <tt>null</tt> if they
     * do not {@link #intersects(RectangularArea) intersect}
     * (<b>A</b>&nbsp;&cap;&nbsp;<b>B</b>&nbsp;=&nbsp;&empty;).
     * Equivalent to
     * <pre>thisInstance.{@link #intersects(RectangularArea) intersects}(area) ? {@link #valueOf(Point, Point)
     * RectangularArea.valueOf}(
     * thisInstance.{@link #min()}.{@link Point#max(Point) max}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link Point#min(Point) min}(area.{@link #max()})) :
     * null</pre>.
     *
     * @param area the second rectangular area.
     * @return     intersection of this and the second rectangular area or <tt>null</tt> if they do not intersect.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea intersection(RectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        int n = min.coordinates.length;
        if (area.min.coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + area.min.coordinates.length + " instead of " + n);
        double[] newMin = new double[n];
        double[] newMax = new double[n];
        for (int k = 0; k < n; k++) {
            newMin[k] = min.coordinates[k] >= area.min.coordinates[k] ? min.coordinates[k] : area.min.coordinates[k];
            newMax[k] = max.coordinates[k] <= area.max.coordinates[k] ? max.coordinates[k] : area.max.coordinates[k];
            if (newMin[k] > newMax[k]) {
                return null;
            }
        }
        return new RectangularArea(new Point(newMin), new Point(newMax));
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
    public Collection<RectangularArea> difference(Collection<RectangularArea> results, RectangularArea area) {
        if (results == null)
            throw new NullPointerException("Null results argument");
        if (!intersects(area)) { // also checks number of dimensions
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
                results.add(new RectangularArea(Point.valueOf(min), Point.valueOf(max)));
            }
            if (area.max.coordinates[k] < this.max.coordinates[k]) {
                min[k] = area.max.coordinates[k];
                max[k] = this.max.coordinates[k];
                results.add(new RectangularArea(Point.valueOf(min), Point.valueOf(max)));
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
     * for (RectangularArea area : whatToSubtract) {
     *     for (int i = 0, n = fromWhatToSubtract.size(); i < n; i++) {
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
     * @see #subtractCollection(java.util.Queue, RectangularArea...)
     */
    public static Queue<RectangularArea> subtractCollection(
        Queue<RectangularArea> fromWhatToSubtract,
        Collection<RectangularArea> whatToSubtract)
    {
        if (fromWhatToSubtract == null)
            throw new NullPointerException("Null fromWhatToSubtract");
        if (whatToSubtract == null)
            throw new NullPointerException("Null whatToSubtract");
        for (RectangularArea area : whatToSubtract) {
            for (int i = 0, n = fromWhatToSubtract.size(); i < n; i++) {
                RectangularArea minuend = fromWhatToSubtract.poll();
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
    public static Queue<RectangularArea> subtractCollection(
        Queue<RectangularArea> fromWhatToSubtract,
        RectangularArea... whatToSubtract)
    {
        return subtractCollection(fromWhatToSubtract, java.util.Arrays.asList(whatToSubtract));
    }

    /**
     * Returns the minimal rectangular area, containing this area and the given point.
     * In the returned area, the {@link #min() minimal vertex} is equal to
     * <tt>thisInstance.{@link #min()}.{@link Point#min(Point) min}(point)</tt> and
     * the {@link #max() maximal vertex} is equal to
     * <tt>thisInstance.{@link #max()}.{@link Point#max(Point) max}(point)</tt>.
     *
     * @param point some point that should be included to the new rectangular area.
     * @return      the expanded rectangular area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link Point#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance,
     *                                  or if the points
     *                                  <tt>thisInstance.{@link #min()}.{@link Point#min(Point) min}(point)</tt>
     *                                  and
     *                                  <tt>thisInstance.{@link #max()}.{@link Point#max(Point) max}(point)</tt>
     *                                  do not match requirements of {@link #valueOf(Point, Point)} method.
     */
    public RectangularArea expand(Point point) {
        if (contains(point)) {
            return this;
        }
        double[] newMin = new double[min.coordinates.length];
        double[] newMax = new double[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = min.coordinates[k] <= point.coordinates[k] ? min.coordinates[k] : point.coordinates[k];
            newMax[k] = max.coordinates[k] >= point.coordinates[k] ? max.coordinates[k] : point.coordinates[k];
        }
        return valueOf(new Point(newMin), new Point(newMax));
    }

    /**
     * Returns the minimal rectangular area, containing this and the passed area.
     * Equivalent to
     * <pre>{@link #valueOf(Point, Point) RectangularArea.valueOf}(
     * thisInstance.{@link #min()}.{@link Point#min(Point) min}(area.{@link #min()}),
     * thisInstance.{@link #max()}.{@link Point#max(Point) max}(area.{@link #max()}))</pre>.
     *
     * @param area the second rectangular area.
     * @return     the minimal rectangular area, containing this and the passed area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>area.{@link #coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea expand(RectangularArea area) {
        if (contains(area)) {
            return this;
        }
        double[] newMin = new double[min.coordinates.length];
        double[] newMax = new double[min.coordinates.length];
        for (int k = 0; k < min.coordinates.length; k++) {
            newMin[k] = min.coordinates[k] <= area.min.coordinates[k] ? min.coordinates[k] : area.min.coordinates[k];
            newMax[k] = max.coordinates[k] >= area.max.coordinates[k] ? max.coordinates[k] : area.max.coordinates[k];
        }
        return new RectangularArea(new Point(newMin), new Point(newMax));
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
     * @throws IllegalArgumentException if <tt>point.{@link Point#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public double parallelDistance(Point point) {
        if (point == null)
            throw new NullPointerException("Null point argument");
        return parallelDistance(point.coordinates);
    }

    /**
     * Equivalent to {@link #parallelDistance(Point) parallelDistance}({@link Point#valueOf(double...)
     * Point.valueOf}(coordinates)), but works faster because does not require to create an instance
     * of {@link Point} class.
     *
     * @param coordinates coordinates of some point.
     * @return      the parallel distance from this point to this rectangular area.
     * @throws NullPointerException     if <tt>coordinates</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>coordinates.length</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public double parallelDistance(double... coordinates) {
        if (coordinates == null)
            throw new NullPointerException("Null coordinates argument");
        int n = this.min.coordinates.length;
        if (coordinates.length != n)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + coordinates.length + " instead of " + n);
        double min = this.min.coordinates[0];
        double max = this.max.coordinates[0];
        double x = coordinates[0];
        double maxD = min - x >= x - max ? min - x : x - max;
        for (int k = 1; k < n; k++) {
            min = this.min.coordinates[k];
            max = this.max.coordinates[k];
            double xk = coordinates[k];
            double d = min - xk >= xk - max ? min - xk : xk - max;
            if (d > maxD) {
                maxD = d;
            }
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(Point) parallelDistance}({@link Point#valueOf(double...)
     * Point.valueOf}(x, y)), but works faster because does not require to allocate any objects.
     * Works only for 2-dimensional rectangular areas, in other cases throws
     * <tt>IllegalArgumentException</tt>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @return  the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <tt>coordinates.length!=2</tt> .
     */
    public double parallelDistance(double x, double y) {
        int n = min.coordinates.length;
        if (n != 2)
            throw new IllegalArgumentException("Dimensions count mismatch: 2 instead of " + n);
        double min = this.min.coordinates[0];
        double max = this.max.coordinates[0];
        double maxD = min - x >= x - max ? min - x : x - max;
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        double d = min - y >= y - max ? min - y : y - max;
        if (d > maxD) {
            maxD = d;
        }
        return maxD;
    }

    /**
     * Equivalent to {@link #parallelDistance(Point) parallelDistance}({@link Point#valueOf(double...)
     * Point.valueOf}(x, y, z)), but works faster because does not require to allocate any objects.
     * Works only for 3-dimensional rectangular areas, in other cases throws
     * <tt>IllegalArgumentException</tt>.
     *
     * @param x the 1st coordinate of some point.
     * @param y the 2nd coordinate of some point.
     * @param z the 3rd coordinate of some point.
     * @return  the parallel distance from this point to this rectangular area.
     * @throws IllegalArgumentException if <tt>coordinates.length!=2</tt> .
     */
    public double parallelDistance(double x, double y, double z) {
        int n = min.coordinates.length;
        if (n != 3)
            throw new IllegalArgumentException("Dimensions count mismatch: 3 instead of " + n);
        double min = this.min.coordinates[0];
        double max = this.max.coordinates[0];
        double maxD = min - x >= x - max ? min - x : x - max;
        min = this.min.coordinates[1];
        max = this.max.coordinates[1];
        double d = min - y >= y - max ? min - y : y - max;
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
    /*Repeat.IncludeEnd*/

    /**
     * Shifts this rectangular area by the specified vector and returns the shifted area.
     * Equivalent to
     * <pre>{@link #valueOf(Point, Point)
     * valueOf}(thisInstance.{@link #min()}.{@link Point#add(Point)
     * add}(vector), thisInstance.{@link #max()}.{@link Point#add(Point) add}(vector))</pre>
     *
     * @param vector the vector which is added to all vertices of this area.
     * @return       the shifted area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>vector.{@link Point#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea shift(Point vector) {
        if (vector == null)
            throw new NullPointerException("Null vector argument");
        if (vector.coordinates.length != min.coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + vector.coordinates.length + " instead of " + min.coordinates.length);
        if (vector.isOrigin()) {
            return this;
        }
        return new RectangularArea(min.add(vector), max.add(vector));
    }

    /**
     * Shifts this rectangular area by <tt>vector.{@link Point#symmetric() symmetric()}</tt>
     * and returns the shifted area.
     * Equivalent to
     * <pre>{@link #valueOf(Point, Point)
     * valueOf}(thisInstance.{@link #min()}.{@link Point#subtract(Point)
     * subtract}(vector), thisInstance.{@link #max()}.{@link Point#subtract(Point) subtract}(vector))</pre>
     *
     * @param vector the vector which is subtracted from all vertices of this area.
     * @return       the shifted area.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>vector.{@link Point#coordCount() coordCount()}</tt> is not equal to
     *                                  the {@link #coordCount() number of dimensions} of this instance.
     */
    public RectangularArea shiftBack(Point vector) {
        if (vector == null)
            throw new NullPointerException("Null vector argument");
        if (vector.coordinates.length != min.coordinates.length)
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + vector.coordinates.length + " instead of " + min.coordinates.length);
        if (vector.isOrigin()) {
            return this;
        }
        return new RectangularArea(min.subtract(vector), max.subtract(vector));
    }

    /**
     * Equivalent to
     * <tt>{@link IRectangularArea#valueOf(RectangularArea) IRectangularArea.valueOf}(thisInstance)</tt>,
     * with the only difference that <tt>IllegalStateException</tt> is thrown instead of
     * <tt>IllegalArgumentException</tt> for unallowed rectangular area.
     *
     * @return the integer rectangular area with same (cast) coordinates.
     * @throws IllegalStateException in the same situation when {@link IRectangularArea#valueOf(RectangularArea)}
     *                               method throws <tt>IllegalArgumentException</tt>.
     */
    public IRectangularArea toIntegerRectangularArea() {
        return IRectangularArea.valueOf(IPoint.valueOf(min), IPoint.valueOf(max), true);
    }

    /**
     * Equivalent to
     * <tt>{@link IRectangularArea#roundOf(RectangularArea) IRectangularArea.roundOf}(thisInstance)</tt>,
     * with the only difference that <tt>IllegalStateException</tt> is thrown instead of
     * <tt>IllegalArgumentException</tt> for unallowed rectangular area.
     *
     * @return the integer rectangular area with same (rounded) coordinates.
     * @throws IllegalStateException in the same situation when {@link IRectangularArea#roundOf(RectangularArea)}
     *                               method throws <tt>IllegalArgumentException</tt>.
     */
    public IRectangularArea toRoundedRectangularArea() {
        return IRectangularArea.valueOf(IPoint.roundOf(min), IPoint.roundOf(max), true);
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
     * Returns <tt>true</tt> if and only if <tt>obj instanceof RectangularArea</tt>,
     * <tt>((RectangularArea)obj).min().equals(this.min())</tt> and
     * <tt>((RectangularArea)obj).max().equals(this.max())</tt>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <tt>true</tt> if the specified object is a rectangular area equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof RectangularArea
            && ((RectangularArea)obj).min.equals(this.min) && ((RectangularArea)obj).max.equals(this.max);
    }
}
