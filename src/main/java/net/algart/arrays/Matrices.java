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

package net.algart.arrays;

import net.algart.math.IRange;
import net.algart.math.functions.ApertureFilterOperator;
import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * <p>Utilities useful for working with {@link Matrix AlgART matrices}.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class Matrices {
    private static final double INVERTED_MEGABYTE = 1.0 / 1048576.0;
    // - calculated exactly

    private Matrices() {
    }

    /**
     * Interpolation method for representing AlgART matrix as a mathematical {@link Func function}.
     * Used by {@link Matrices#asInterpolationFunc(Matrix, InterpolationMethod, boolean)} and
     * {@link Matrices#asInterpolationFunc(Matrix, InterpolationMethod, double)} methods.
     */
    public enum InterpolationMethod {

        /**
         * Simplest interpolation method: the real coordinates are truncated to integer numbers by <code>(long)x</code>
         * Java operator. See details in comments to
         * {@link Matrices#asInterpolationFunc(Matrix, InterpolationMethod, boolean)}.
         */
        STEP_FUNCTION(IRange.of(0, 0)),

        /**
         * Polylinear interpolation method: the function value is calculated as a polylinear interpolation of
         * 2<sup><i>n</i></sup> neighbour matrix elements. See details in comments to
         * {@link Matrices#asInterpolationFunc(Matrix, InterpolationMethod, boolean)}.
         */
        POLYLINEAR_FUNCTION(IRange.of(0, 1));

        private final IRange dependenceCoordRange;

        InterpolationMethod(IRange dependenceCoordRange) {
            this.dependenceCoordRange = dependenceCoordRange;
        }

        /**
         * Returns the <i>dependence range</i> of this interpolation algorithm for all coordinates.
         *
         * <p><i>Dependence range</i> is the such range <code>min..max</code>,
         * that the value of the interpolation function
         * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ...),
         * created by this interpolation method,
         * is fully defined by only the elements of the source matrix
         * with the coordinates <code>i<sub>0</sub></code>, <code>i<sub>1</sub></code>, ...,
         * lain in the ranges:
         * <p><code>&nbsp;&nbsp;&nbsp;&nbsp;
         * (long)<i>x</i><sub>0</sub> + min &lt;= i<sub>0</sub> &lt;= (long)<i>x</i><sub>0</sub> + max,
         * (long)<i>x</i><sub>1</sub> + min &lt;= i<sub>1</sub> &lt;= (long)<i>x</i><sub>1</sub> + max,
         * ...
         * </code></p>
         * <p>This method allows to get the dependence range for this interpolation algorithm.
         * In particular, for the {@link #STEP_FUNCTION}, representing some matrix,
         * the returned range is always <code>0..0</code> ({@link IRange#of(long, long) IRange.of(0, 0)}).
         * For the case of {@link #POLYLINEAR_FUNCTION}, the range is little larger:
         * <code>0..1</code>. For more complex interpolation schemes, it could be <code>-1..1</code>,
         * <code>-2..2</code> and so on.
         *
         * <p>In future versions of this package,
         * if this method is not able to return a suitable range
         * (for example, some value of the returned function depends on all matrix elements),
         * it will return a range, always including all matrix coordinates,
         * for example, <code>Long.MIN_VALUE..Long.MAX_VALUE</code>.
         *
         * <p>This method never returns {@code null}.
         *
         * @return the dependence range for this interpolation algorithm.
         */
        public IRange dependenceCoordRange() {
            return dependenceCoordRange;
        }
    }

    /**
     * <p>Resizing mode for {@link Matrices#asResized Matrices.asResized} method.</p>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static class ResizingMethod {
        /**
         * Simplest resizing method.
         * See details in comments to {@link Matrices#asResized Matrices.asResized}.
         */
        public static final ResizingMethod SIMPLE = new ResizingMethod(InterpolationMethod.STEP_FUNCTION);

        /**
         * Resizing with averaging (while compression), but without interpolation.
         * See details in comments to {@link Matrices#asResized Matrices.asResized}.
         */
        public static final ResizingMethod AVERAGING = newAveraging(InterpolationMethod.STEP_FUNCTION);

        /**
         * Resizing with poly-linear interpolation (useful for expanding), but without interpolation.
         * See details in comments to {@link Matrices#asResized Matrices.asResized}.
         */
        public static final ResizingMethod POLYLINEAR_INTERPOLATION = new ResizingMethod(
                InterpolationMethod.POLYLINEAR_FUNCTION);

        /**
         * Resizing with poly-linear interpolation and averaging while compression.
         * See details in comments to {@link Matrices#asResized Matrices.asResized}.
         */
        public static final ResizingMethod POLYLINEAR_AVERAGING =
                newAveraging(InterpolationMethod.POLYLINEAR_FUNCTION);

        final InterpolationMethod interpolationMethod;

        private ResizingMethod(InterpolationMethod interpolationMethod) {
            Objects.requireNonNull(interpolationMethod, "Null interpolationMethod");
            this.interpolationMethod = interpolationMethod;
        }

        /**
         * Returns <code>true</code> for any instances of {@link Averaging} class,
         * in particular, for {@link #AVERAGING} and {@link #POLYLINEAR_AVERAGING} cases,
         * <code>false</code> for other cases.
         *
         * @return whether this mode performs averaging of several elements.
         */
        public boolean averaging() {
            return false;
        }

        /**
         * Returns <code>true</code> for {@link #POLYLINEAR_INTERPOLATION} and {@link #POLYLINEAR_AVERAGING} cases,
         * and for inheritors of {@link Averaging} class created with
         * {@link InterpolationMethod#POLYLINEAR_FUNCTION} constructor argument,
         * <code>false</code> for other cases.
         *
         * @return whether this mode performs some form of interpolation between elements.
         */
        public final boolean interpolation() {
            return interpolationMethod != InterpolationMethod.STEP_FUNCTION;
        }

        /**
         * Returns {@link InterpolationMethod#POLYLINEAR_FUNCTION} for
         * {@link #POLYLINEAR_INTERPOLATION} and {@link #POLYLINEAR_AVERAGING} cases,
         * {@link InterpolationMethod#STEP_FUNCTION} for
         * {@link #SIMPLE} and {@link #AVERAGING} cases,
         * and the argument of the constructor for inheritors of {@link Averaging} class.
         *
         * @return the interpolation method used while resizing.
         */
        public final InterpolationMethod interpolationMethod() {
            return interpolationMethod;
        }

        /**
         * Resizing method with averaging (while compression). Can be extended to customize
         * the averaging function: to do this, please override {@link #getAveragingFunc(long[])} method.
         *
         * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
         * there are no ways to modify the settings of the created instance.
         * Any possible inheritors of this class also must be <b>immutable</b>
         * and <b>thread-safe</b>.</p>
         */
        public static class Averaging extends ResizingMethod {
            /**
             * Creates a new instance of this class.
             *
             * @param interpolationMethod interpolation that should be used while resizing.
             * @throws NullPointerException if the argument is {@code null}.
             */
            protected Averaging(InterpolationMethod interpolationMethod) {
                super(interpolationMethod);
            }

            @Override
            public final boolean averaging() {
                return true;
            }

            /**
             * Returns the function that should be used for "averaging" several elements while compression.
             * May return {@code null}, than the default behavior (usual averaging the source elements).
             * This implementation returns {@code null}.
             *
             * @param apertureDim the sizes of averaged aperture in the source matrix.
             * @return the function that will be used for averaging; can be {@code null}
             * (default behavior).
             */
            protected Func getAveragingFunc(long[] apertureDim) {
                return null;
            }
        }


        // This private function helps to avoid the following IntelliJ IDEA warning:
        //     Referencing subclass ConstantImpl from superclass ContinuationMode initializer
        //     might lead to class loading deadlock
        //     Inspection info: Reports classes that refer to their subclasses
        //     in static initializers or static fields.
        //     Such references can cause JVM-level deadlocks in a multithreaded environment,
        //     when one thread tries to load the superclass and another thread tries
        //     to load the subclass at the same time.
        private static ResizingMethod newAveraging(InterpolationMethod interpolationMethod) {
            return new Averaging(interpolationMethod);
        }
    }

    /**
     * <p>Region in <i>n</i>-dimensional space. More precisely, this class defines a random set of points
     * <i>with integer coordinates</i> in <i>n</i>-dimensional space.
     * Though some kinds of regions are defined in terms of real coordinates,
     * this class is useful for processing only integer points belonging to the region.
     * This class is designed for copying/filling regions in {@link Matrix AlgART matrices} by</p>
     *
     * <ul>
     * <li>{@link Matrices#copyRegion(ArrayContext, Matrix, Matrix, net.algart.arrays.Matrices.Region, long...)},</li>
     * <li>{@link Matrices#copyRegion(ArrayContext, Matrix, Matrix, net.algart.arrays.Matrices.Region, long[], Object)}
     * and</li>
     * <li>{@link Matrices#fillRegion(ArrayContext, Matrix, net.algart.arrays.Matrices.Region, Object)}</li>
     * </ul>
     *
     * <p>methods, where non-integer coordinates are needless.</p>
     *
     * <p>This class is abstract and describes the region (set of points) by the only one simple abstract method
     * {@link #contains(long... coordinates)},
     * which returns <code>true</code> if and only if the point with the specified coordinates belongs to the region.
     * It is enough to implement this method to define a new region.
     * In addition, this class always requires to specify {@link #coordRanges() coordinate ranges}:
     * such ranges that coordinates of all region points surely belong to them.</p>
     *
     * <p>However, the region, constructed by this way, provides low performance of
     * {@link Matrices#copyRegion Matrices.copyRegion} / {@link Matrices#fillRegion Matrices.fillRegion} methods,
     * because the only way to process it is checking all points separately by
     * {@link #contains(long...)} method.
     * Therefore, this class provides the additional method
     * {@link #sectionAtLastCoordinate(long sectionCoordinateValue)},
     * which builds an intersection of this region with some hyperplane and returns this intersection
     * as one or several regions with less number of dimensions. This method is not abstract
     * (it is implemented via {@link #contains(long...)} method by default), but an inheritor
     * can offer much faster implementation for most cases &mdash; and all inheritors from this package
     * really do it.</p>
     *
     * <p>An idea of this method is the following. It allows to represent the <i>n</i>-dimensional region
     * (a set of integer points) as a union of its (<i>n</i>&minus;1)-dimensional <i>sections</i>:
     * results of calls of {@link #sectionAtLastCoordinate(long) sectionAtLastCoordinate}
     * for all possible values of its argument <code>sectionCoordinateValue</code>.
     * Then every (<i>n</i>&minus;1)-dimensional section can be similarly represented
     * as a union of its (<i>n</i>&minus;2)-dimensional sections, etc.
     * But for 2-dimensional case most region types (in particular, all inheritors of this class
     * from this package) can return the required intersection (with a horizontal line)
     * as one or several <i>continuous segments</i>: regions of special type (1-dimensional
     * {@link Matrices.Hyperparallelepiped Hyperparallelepiped}), which can be processes very quickly.</p>
     *
     * <p>If an inheritor correctly implements {@link #sectionAtLastCoordinate(long) sectionAtLastCoordinate}
     * and if this method does not use {@link #contains(long...)} method and the parent (default)
     * implementation {@link #sectionAtLastCoordinate(long) Region.sectionAtLastCoordinate}, then the inheritor
     * <i>is allowed not to implement</i> {@link #contains(long...)} method.
     * Instead, it is enough to override {@link #isContainsSupported()} method and return <code>false</code> by it.
     * In this case, {@link #contains(long...)} method should throw
     * <code>UnsupportedOperationException</code>.
     *
     * <p>This class can represent an empty region (containing no points).
     * The number of dimensions of the range is always positive (1, 2, ...).</p>
     *
     * <p>This package offers the following implementations of the regions:</p>
     *
     * <ol>
     * <li>{@link Matrices.Hyperparallelepiped}: the simplest possible region (a segment in 1-dimensional
     * case, a rectangle in 2-dimensional case, a parallelepiped in 3-dimensional case);</li>
     * <li>{@link Matrices.ConvexHyperpolyhedron}: an intersection of several <i>n</i>-dimensional
     * half-spaces (in other words, a convex hyper-polyhedron);</li>
     * <li>{@link Matrices.Simplex}: the simplest kind of <i>n</i>-dimensional hyper-polyhedron &mdash;
     * a hyper-polyhedron with <i>n</i>+1 vertices (a segment in 1-dimensional case,
     * a triangle in 2-dimensional case, a tetrahedron in 3-dimensional case);</li>
     * <li>{@link Matrices.Polygon2D}: a random 2-dimensional polygon, maybe non-convex and even
     * self-intersecting.</li>
     * </ol>
     *
     * <p>Note that all region types are always restricted by the hyper-parallelepiped, defined by the
     * {@link #coordRanges() coordinate ranges}, so a region cannot be infinite.</p>
     *
     * <p>Also note: this class and its inheritors from this package do not implement own
     * <code>equals</code> and <code>hashCode</code> methods.
     * So, this class does not provide a mechanism for comparing different regions.
     *
     * <p>Inheritors of this abstract class are usually <b>immutable</b> and
     * always <b>thread-safe</b>: all methods of this class may be freely used
     * while simultaneous accessing the same instance from several threads.
     * All inheritors of this class from this package are <b>immutable</b>.</p>
     */
    public static abstract class Region {
        static final Region[] EMPTY_REGIONS = new Region[0];

        final IRange[] coordRanges;
        final long minX, maxX;
        final int n;

        /**
         * Creates new instance of this class.
         *
         * <p>The passed <code>coordRanges</code> array is cloned by this constructor: no references to it
         * are maintained by the created object.
         *
         * @param coordRanges the ranges that surely contain coordinates of all points of this region
         *                    (they will be returned by {@link #coordRanges()} method).
         * @throws NullPointerException     if the <code>coordRanges</code> array some of
         *                                  its elements is {@code null}.
         * @throws IllegalArgumentException if the passed array is empty (<code>coordRanges.length==0</code>).
         */
        protected Region(IRange[] coordRanges) {
            Objects.requireNonNull(coordRanges, "Null coordRanges argument");
            if (coordRanges.length == 0) {
                throw new IllegalArgumentException("Empty coordRanges array");
            }
            for (int k = 0; k < coordRanges.length; k++) {
                Objects.requireNonNull(coordRanges[k], "Null coordRanges[" + k + "]");
            }
            this.n = coordRanges.length;
            this.coordRanges = coordRanges.clone();
            this.minX = coordRanges[0].min();
            this.maxX = coordRanges[0].max();
        }

        /**
         * Creates 1-dimensional segment, described by the given range. Equivalent to
         * {@link #getHyperparallelepiped(IRange...) getHyperparallelepiped(xRange)}.
         *
         * @param xRange the range of the only coordinate of all points of the segment.
         * @return the segment containing all points from the specified range
         * (including minimal and maximal values).
         * @throws NullPointerException if the argument is {@code null}.
         */
        public static Hyperparallelepiped getSegment(IRange xRange) {
            return new Hyperparallelepiped(xRange);
        }

        /**
         * Creates 2-dimensional rectangle with sides, parallel to coordinate axes,
         * described by the given ranges of coordinates. Equivalent to
         * {@link #getHyperparallelepiped(IRange...) getHyperparallelepiped(xRange, yRange)}.
         *
         * <p>Note: an equivalent region can be constructed by {@link #getPolygon2D(double[][] vertices)} method.
         * But the region, constructed by this method, is processed little faster.
         *
         * @param xRange the <i>x</i>-projection of the rectangle.
         * @param yRange the <i>y</i>-projection of the rectangle.
         * @return the rectangle <code>xRange &times; yRange</code>.
         * @throws NullPointerException if one of the arguments is {@code null}.
         */
        public static Hyperparallelepiped getRectangle2D(IRange xRange, IRange yRange) {
            return new Hyperparallelepiped(xRange, yRange);
        }

        /**
         * Creates 3-dimensional parallelepiped with edges, parallel to coordinate axes,
         * described by the given ranges of coordinates. Equivalent to
         * {@link #getHyperparallelepiped(IRange...) getHyperparallelepiped(xRange, yRange, zRange)}.
         *
         * @param xRange the <i>x</i>-projection of the rectangle.
         * @param yRange the <i>y</i>-projection of the rectangle.
         * @param zRange the <i>z</i>-projection of the rectangle.
         * @return the parallelepiped <code>xRange &times; yRange &times; zRange</code>.
         * @throws NullPointerException if one of the arguments is {@code null}.
         */
        public static Hyperparallelepiped getParallelepiped3D(IRange xRange, IRange yRange, IRange zRange) {
            return new Hyperparallelepiped(xRange, yRange, zRange);
        }

        /**
         * Creates <i>n</i>-dimensional {@link Hyperparallelepiped hyper-parallelepiped} with edges,
         * parallel to coordinate axes, described by the given ranges of coordinates.
         * More precisely, the returned region contains all such points
         * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>), that
         *
         * <blockquote>
         * <code>coordRanges[0].{@link IRange#min() min()}</code> &le; <i>x</i><sub>0</sub> &le;
         * <code>coordRanges[0].{@link IRange#max() max()}</code>,<br>
         * <code>coordRanges[1].{@link IRange#min() min()}</code> &le; <i>x</i><sub>1</sub> &le;
         * <code>coordRanges[1].{@link IRange#max() max()}</code>,<br>
         * ...,<br>
         * <code>coordRanges[<i>n</i>&minus;1].{@link IRange#min()
         * min()}</code> &le; <i>x</i><sub><i>n</i>&minus;1</sub> &le;
         * <code>coordRanges[<i>n</i>&minus;1].{@link IRange#max() max()}</code>.
         * </blockquote>
         *
         * <p>The number <i>n</i> of dimensions of the created region is equal to <code>coordRanges.length</code>.
         *
         * <p>The passed <code>coordRanges</code> array is cloned by this method: no references to it
         * are maintained by the created object.
         *
         * @param coordRanges the ranges of the hyper-parallelepiped coordinates.
         * @return the hyper-parallelepiped <code>coordRanges[0] &times; coordRanges[1] &times; ...</code>
         * @throws NullPointerException     if the <code>coordRanges</code> array some of its elements
         *                                  is {@code null}.
         * @throws IllegalArgumentException if the passed array is empty (<code>coordRanges.length==0</code>).
         */
        public static Hyperparallelepiped getHyperparallelepiped(IRange... coordRanges) {
            return new Hyperparallelepiped(coordRanges);
        }

        /**
         * Creates 2-dimensional triangle with the specified coordinates of vertices. Equivalent to
         * <code>{@link #getSimplex(double[][])
         * getSimplex}(new double[][] {{x1,y1},{x2,y2},{x3,y3}})</code>.
         *
         * <p>The specified vertices must not lie in the same straight line.
         *
         * <p>Note: an equivalent region can be constructed by {@link #getPolygon2D(double[][] vertices)} method.
         * But the region, constructed by this method, is processed little faster.
         * On the other hand, {@link #getPolygon2D(double[][]) getPolygon2D} method works correcly even
         * if the vertices lie in the same straight line.
         *
         * @param x1 the <i>x</i>-coordinate of the 1st vertex.
         * @param y1 the <i>y</i>-coordinate of the 1st vertex.
         * @param x2 the <i>x</i>-coordinate of the 2nd vertex.
         * @param y2 the <i>y</i>-coordinate of the 2nd vertex.
         * @param x3 the <i>x</i>-coordinate of the 3rd vertex.
         * @param y3 the <i>y</i>-coordinate of the 3rd vertex.
         * @return the triangle with the specified vertices.
         * @throws DegeneratedSimplexException if all vertices lies in the same straight line
         *                                     (as it is detected by analysing the coordinates
         *                                     via calculations with standard Java <code>double</code> numbers).
         */
        public static Simplex getTriangle2D(double x1, double y1, double x2, double y2, double x3, double y3) {
            return new Simplex(new double[][]{{x1, y1}, {x2, y2}, {x3, y3}});
        }

        /**
         * Creates 3-dimensional tetrahedron with the specified coordinates of vertices. Equivalent to
         * <code>{@link #getSimplex(double[][]) getSimplex}(new double[][]
         * {{x1,y1,z1},{x2,y2,z2},{x3,y3,z3},{x4,y4,z4}})</code>.
         *
         * <p>The specified vertices must not lie in the same plane.
         *
         * @param x1 the <i>x</i>-coordinate of the 1st vertex.
         * @param y1 the <i>y</i>-coordinate of the 1st vertex.
         * @param z1 the <i>z</i>-coordinate of the 1st vertex.
         * @param x2 the <i>x</i>-coordinate of the 2nd vertex.
         * @param y2 the <i>y</i>-coordinate of the 2nd vertex.
         * @param z2 the <i>z</i>-coordinate of the 2nd vertex.
         * @param x3 the <i>x</i>-coordinate of the 3rd vertex.
         * @param y3 the <i>y</i>-coordinate of the 3rd vertex.
         * @param z3 the <i>z</i>-coordinate of the 3rd vertex.
         * @param x4 the <i>x</i>-coordinate of the 4th vertex.
         * @param y4 the <i>y</i>-coordinate of the 4th vertex.
         * @param z4 the <i>z</i>-coordinate of the 4th vertex.
         * @return the tetrahedron with the specified vertices.
         * @throws DegeneratedSimplexException if all vertices lies in the same plane
         *                                     (as it is detected by analysing the coordinates
         *                                     via calculations with standard Java <code>double</code> numbers).
         */
        public static Simplex getTetrahedron3D(
                double x1, double y1, double z1,
                double x2, double y2, double z2,
                double x3, double y3, double z3,
                double x4, double y4, double z4) {
            return new Simplex(new double[][]{{x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}, {x4, y4, z4}});
        }

        /**
         * Creates <i>n</i>-dimensional {@link Simplex simplex} with the specified coordinates of vertices.
         * More precisely, this method creates a simplex &mdash; the simplest <i>n</i>-dimensional hyper-polyhedron
         * with <i>n</i>+1 vertices, where the vertex #<i>k</i> (<i>k</i>=0,1,...,<i>n</i>)
         * has the coordinates <code>vertices[<i>k</i>][0]</code>,
         * <code>vertices[<i>k</i>][1]</code>, ...,
         * <code>vertices[<i>k</i>][<i>n</i>&minus;1]</code>.
         *
         * <p>The number <i>n</i> of dimensions of the created region is equal to
         * <code>vertices[<i>k</i>].length</code>; this length must be same for all <i>k</i>.
         * The number of vertices <code>vertices.length</code> must be equal to <i>n</i>+1.
         *
         * <p>The points, lying precisely in the (hyper)facets of the simplex (in particular, the vertices),
         * belong to the resulting region.
         *
         * <p>The created region is defined in terms of real coordinates,
         * but only integer points belonging to this region are really processed by methods of this package.
         *
         * <p>The passed <code>vertices</code> array is deeply cloned by this method: no references to it
         * or its elements are maintained by the created object.
         *
         * <p>The specified vertices must not lie in the same <i>(n&minus;1)</i>-dimensional hyperplane.
         *
         * <p>Note: this method allocates two Java <code>double[]</code> arrays containing <i>n+1</i> and
         * <i>n</i>*(<i>n</i>+1) elements. We do not recommend create simplexes with large number of dimensions:
         * this method can work very slowly when <i>n</i> is greater than 6&ndash;7.
         *
         * @param vertices coordinates of all vertices.
         * @return the simplex with the specified vertices.
         * @throws NullPointerException        if the <code>vertices</code> array or some of its elements
         *                                     is {@code null}.
         * @throws IllegalArgumentException    if the <code>vertices</code> array or some of its elements is empty
         *                                     (has zero length),
         *                                     or if the length of some <code>vertices[k]</code> array is not equal to
         *                                     <code>vertices[0].length+1</code>.
         * @throws DegeneratedSimplexException if all vertices lies in the same <i>(n&minus;1)</i>-dimensional
         *                                     hyperplane (as it is detected by analysing the coordinates
         *                                     via calculations with standard Java <code>double</code> numbers).
         */
        public static Simplex getSimplex(double[][] vertices) {
            return new Simplex(vertices);
        }

        /**
         * Creates <i>n</i>-dimensional {@link Matrices.ConvexHyperpolyhedron convex hyper-polyhedron},
         * which is an intersection of <i>m</i> <i>n</i>-dimensional half-spaces,
         * specified by inequalities
         * <b>a</b><sub><i>i</i></sub><b>x</b> &le; <i>b</i><sub><i>i</i></sub>
         * (<i>i</i>=0,1,...,<i>m</i>&minus;1),
         * and the hyper-parallelepiped, built by {@link #getHyperparallelepiped(IRange... coordRanges)} method with
         * the same <code>coordRanges</code> argument. Here <b>a</b><sub><i>i</i></sub><b>x</b> means
         * the scalar product of the line #<i>i</i> of the matrix <b>A</b>, passed by the first argument,
         * and the vector of coordinates
         * <b>x</b>=(<i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>n</i>&minus;1</sub>).
         * The elements of the matrix <b>A</b> must be listed, row by row, in the <code>a</code> array:
         * <b>A</b>={<i>a</i><sub><i>ij</i></sub>},
         * <i>a</i><sub><i>ij</i></sub>=<code>a[<i>i</i>*<i>n</i>+<i>j</i>]</code>,
         * <i>i</i> is the index of the row (0..<i>m</i>&minus;1),
         * <i>j</i> is the index of the column (0..<i>n</i>&minus;1),
         * <i>m</i>=<code>b.length</code>.
         * The elements of the vector
         * <b>b</b>=(<i>b</i><sub>0</sub>,<i>b</i><sub>1</sub>,...,<i>b</i><sub><i>m</i>&minus;1</sub>)
         * must be listed in <code>b</code> argument.
         * The length <code>a.length</code> of the <code>a</code> array must be equal to the product <i>nm</i>,
         * where <i>n</i>=<code>coordRanges.length</code>, <i>m</i>=<code>b.length</code>.
         * The number of inequalities <i>m</i> can be any non-negative integer 0,1,2,...
         *
         * <p>The number <i>n</i> of dimensions of the created region is equal to <code>coordRanges.length</code>.
         *
         * <p>The points, lying precisely in the (hyper)facets of the hyper-polyhedron (in particular, the vertices),
         * belong to the resulting region.
         *
         * <p>The created region is defined in terms of real coordinates,
         * but only integer points belonging to this region are really processed by methods of this package.
         *
         * <p>The passed Java arrays are cloned by this method: no references to them
         * are maintained by the created object.
         *
         * @param a           the matrix of the left side coefficients for the inequalities, defining the half-spaces.
         * @param b           the values on the right side of inequalities, defining the half-spaces.
         * @param coordRanges the ranges of the containing hyper-parallelepiped coordinates.
         * @return the intersection of the specified half-spaces and hyper-parallelepiped.
         * @throws NullPointerException     if one of the arguments is {@code null}
         *                                  or if some element of <code>coordRanges</code> array is {@code null}.
         * @throws IllegalArgumentException if <code>coordRanges.length==0</code>,
         *                                  or if <code>a.length!=coordRanges.length*b.length</code>.
         */
        public static ConvexHyperpolyhedron getConvexHyperpolyhedron(
                double[] a, double[] b, IRange... coordRanges) {
            return new ConvexHyperpolyhedron(coordRanges, a, b);
        }

        /**
         * Creates {@link Polygon2D 2-dimensional polygon} with the specified coordinates of vertices.
         * More precisely, this method creates a polygon
         * with <i>m</i>=<code>vertices.length</code> vertices, where the vertex #<i>k</i>
         * (<i>k</i>=0,1,...,<i>m</i>&minus;1)
         * has the coordinates <i>x</i><sub><i>k</i></sub>=<code>vertices[<i>k</i>][0]</code>,
         * <i>y</i><sub><i>k</i></sub>=<code>vertices[<i>k</i>][1]</code>.
         * All arrays <code>vertices[<i>k</i>]</code> must consist of 2 elements.
         *
         * <p>The created polygon can be non-convex and even self-intersecting.
         * The vertices can be specified both in clockwise or in anticlockwise order.
         * The points, lying precisely in the sides of the polygon (in particular, the vertices),
         * belong to the resulting region.
         *
         * <p>The created region is defined in terms of real coordinates,
         * but only integer points belonging to this region are really processed by methods of this package.
         *
         * <p>The passed <code>vertices</code> array is deeply cloned by this method: no references to it
         * or its elements are maintained by the created object.
         *
         * <p>Note: this method allocates two Java <code>double[]</code> arrays containing <i>m</i> elements.
         *
         * @param vertices coordinates of all vertices.
         * @return the 2-dimensional polygon with the specified vertices.
         * @throws NullPointerException     if the <code>vertices</code> array or some of its elements
         *                                  is {@code null}.
         * @throws IllegalArgumentException if the <code>vertices</code> array or some of its elements is empty
         *                                  (has zero length),
         *                                  or if the length of some <code>vertices[k]</code> array is not 2.
         */
        public static Polygon2D getPolygon2D(double[][] vertices) {
            return new Polygon2D(vertices);
        }

        /**
         * Returns the number of dimensions of this region. The returned value is always positive (1, 2, ...).
         *
         * @return the number of dimensions of this region.
         */
        public final int n() {
            return n;
        }

        /**
         * Returns the coordinate ranges, passed to the constructor.
         * The length of the returned array is equal to {@link #n()}.
         *
         * <p>For {@link Matrices.Hyperparallelepiped} and {@link Matrices.ConvexHyperpolyhedron} classes,
         * these ranges are the same as the corresponding argument of the instantiation methods
         * ({@link #getHyperparallelepiped(IRange...)} and
         * {@link #getConvexHyperpolyhedron(double[], double[], IRange...)}).
         *
         * <p>For {@link Matrices.Simplex} and {@link Matrices.Polygon2D} classes,
         * these ranges are calculated automatically as the minimal integer ranges, containing all vertices,
         * passed to the instantiation methods
         * ({@link #getSimplex(double[][])} and {@link #getPolygon2D(double[][])}).
         *
         * <p>The returned array is a clone of the internal array stored in this object.
         *
         * @return the ranges that surely contain coordinates of all points of this region.
         */
        public final IRange[] coordRanges() {
            return coordRanges.clone();
        }

        /**
         * Returns the coordinate range <code>#coordIndex</code>.
         * Equivalent to <code>{@link #coordRanges()}[coordIndex]</code>, but works faster.
         *
         * @param coordIndex the index of coordinate.
         * @return the ranges that surely contain the coordinate <code>#coordIndex</code>
         * of all points of this region.
         * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
         *                                   <code>coordIndex&gt;={@link #n()}</code>.
         */
        public final IRange coordRange(int coordIndex) {
            return coordRanges[coordIndex];
        }

        /**
         * Returns <code>true</code> if this region is rectangular, that is if it contains the same set of
         * integer points (points with integer coordinates) as some
         * {@link Matrices.Hyperparallelepiped hyper-parallelepiped}.
         * This method always returns <code>false</code> if this region is not rectangular,
         * but there is no guarantee that it returns <code>true</code> when it is rectangular.
         *
         * <p>This default implementation returns <code>false</code>.
         * In {@link Matrices.Hyperparallelepiped} class this method returns <code>true</code>.
         * In all other inheritors of this class, implemented in this package, it returns <code>false</code>.
         *
         * @return <code>true</code> if this region is rectangular.
         */
        public boolean isRectangular() {
            return false;
        }

        /**
         * Returns <code>true</code> if and only if the point with the specified integer coordinates
         * belongs to this region.
         *
         * <p>The <code>coordinates</code> must contain at least {@link #n()} elements.
         * It can contain more than {@link #n()} elements; then the extra elements will be ignored.
         *
         * <p><i>Warning!</i> Some inheritors of this class does not provide correct implementation of this method.
         * In this case, {@link #isContainsSupported()} method returns <code>false</code> and this method throws
         * <code>UnsupportedOperationException</code>. So, you must always check the result of
         * {@link #isContainsSupported()} method before calling this one.
         *
         * <p>However, this method <i>must</i> be correctly implemented, if this region is a
         * 1-dimensional (<code>{@link #n()}==1</code>) and {@link #isRectangular()}
         * method returns <code>false</code>.
         *
         * <p>Note: even if the inheritor does not provide correct implementation of this method,
         * it must always provide correct implementation of {@link #sectionAtLastCoordinate(long)} method.
         *
         * @param coordinates the coordinates of the point: the first element is <i>x</i>, the second is <i>y</i>, ...
         * @return <code>true</code> if and only if the point with the specified coordinates
         * belongs to this region.
         * @throws NullPointerException          if the argument is {@code null}.
         * @throws IndexOutOfBoundsException     if the length of the passed array is less than {@link #n()}.
         * @throws UnsupportedOperationException if the inheritor does not implement this operation.
         */
        public abstract boolean contains(long... coordinates);

        /**
         * Indicates whether the method {@link #contains(long...)} in this class works correctly.
         * You should use {@link #contains(long...)} method only
         * if this method returns <code>true</code>;
         * in another case, {@link #contains(long...)} throws <code>UnsupportedOperationException</code>.
         *
         * <p>This default implementation returns <code>true</code>.
         * So, if you prefer not to implement {@link #contains(long...)} method,
         * you must override this method and return <code>false</code>.
         * This method <i>must</i> return <code>true</code> if
         * <code>{@link #n()}==1 &amp;&amp; !{@link #isRectangular()}</code>.
         *
         * @return <code>true</code> if {@link #contains(long...)} method works correctly;
         * otherwise {@link #contains(long...)} method throws <code>UnsupportedOperationException</code>.
         */
        public boolean isContainsSupported() {
            return true;
        }

        /**
         * Finds the intersection of this region with the hyperplane, described by the equation
         * <i>x</i><sub><i>{@link #n() n}</i>&minus;1</sub>=<code>sectionCoordinateValue</code>,
         * and returns this intersection as an array of (<i>n</i>&minus;1)-dimensional
         * regions.
         * (Here <i>x</i><sub><i>{@link #n() n}</i>&minus;1</sub> is the last coordinate of the points:
         * <i>y</i>-coordinate in 2-dimensional case,
         * <i>z</i>-coordinate in 3-dimensional case, etc.)
         * If the intersection is empty, this method returns an empty array (<code>"new&nbsp;Region[0]"</code>).
         * This method never returns {@code null}.
         *
         * <p>This method must not be used if this region is 1-dimensional
         * (<code>{@link #n()}==1</code>). In this case, it throws <code>IllegalStateException</code>.
         *
         * <p>This default implementation is based on {@link #contains(long...)} method, which is supposed
         * to be correctly implemented.
         *
         * <p>Note: it is possible (in some rare exotic cases), that the regions, returned by this method,
         * intersects with each other: some points will belong to 2 and more elements of the result.
         * In particular, it is possible for {@link Polygon2D},
         * if some sides of the polygon lie exactly at the horizontal <i>y</i>=<code>sectionCoordinateValue</code>.
         *
         * <p>Implementations of this method in these packages, besides the implementation in
         * {@link Polygon2D} class, never return more than 1 region in the result.
         *
         * <p>You <i>must</i> override this method if you prefer not to implement {@link #contains(long...)} method
         * ({@link #isContainsSupported()} returns <code>false</code>). In this case, your implementation
         * must not call {@link #contains(long...)} method or
         * <code>super.{@link #sectionAtLastCoordinate(long)}</code>.
         *
         * @param sectionCoordinateValue the value of the last coordinate.
         * @return the intersection of this region and the
         * (<i>n</i>&minus;1)-dimensional hyperplane,
         * corresponding to the specified value of the last coordinate
         * (0, 1 or more regions, every region is
         * (<i>n</i>&minus;1)-dimensional).
         * @throws IllegalStateException if this region is 1-dimensional (<code>{@link #n()}==1</code>).
         */
        public Region[] sectionAtLastCoordinate(final long sectionCoordinateValue) {
            if (!checkSectionAtLastCoordinate(sectionCoordinateValue)) {
                return EMPTY_REGIONS;
            }
            final Region parent = this;
            return new Region[]{new Region((IRange[]) JArrays.copyOfRange(coordRanges, 0, n - 1)) {
                @Override
                public boolean contains(long... coordinates) {
                    long[] parentCoordinates = new long[n + 1];
                    for (int k = 0; k < n; k++) { // it's better than System.arraycopy for little arrays
                        parentCoordinates[k] = coordinates[k];
                    }
                    parentCoordinates[n] = sectionCoordinateValue;
                    return parent.contains(parentCoordinates);
                }

                @Override
                public String toString() {
                    return "section at " + sectionCoordinateValue + " of " + parent;
                }
            }};
        }

        /**
         * Returns <code>true</code> if and only the specified coordinate value lies inside the corresponding
         * {@link #coordRange(int) coordinate range}. In other words, returns the result of the following check:
         * <code>{@link #coordRange(int) coordRange}({@link #n() n()}-1).{@link IRange#contains(long)
         * contains}(coordinateValue)</code>.
         * Besides this, this method checks the number of dimensions {@link #n()} and throws
         * <code>IllegalStateException</code> if <code>{@link #n()}==1</code>.
         *
         * <p>This method is usually called at the beginning of {@link #sectionAtLastCoordinate(long)} method.
         * If it returns <code>false</code>, that method returns an empty array.
         *
         * @param sectionCoordinateValue the value of the last coordinate.
         * @return <code>true</code> if and only the specified coordinate value lies inside
         * the corresponding {@link #coordRange(int) coordinate range}.
         * @throws IllegalStateException if this region is 1-dimensional (<code>{@link #n()}==1</code>).
         */
        protected final boolean checkSectionAtLastCoordinate(long sectionCoordinateValue) {
            if (n == 1) {
                throw new IllegalStateException("Cannot get a section for 1-dimensional region");
            }
            return coordRanges[n - 1].contains(sectionCoordinateValue);
        }

        boolean sectionIsUninterruptedSegment(long sectionCoordinateValue) {
            return false;
        }

        boolean segmentSectionAtLastCoordinate(MutableIRange result, long sectionCoordinateValue) {
            throw new UnsupportedOperationException();
        }

        static class MutableIRange {
            long min, max;
        }
    }

    /**
     * <p>Hyper-parallelepiped: the simplest <i>n</i>-dimensional region.
     * In 1-dimensional case it is a segment,
     * in 2-dimensional case it is a rectangle,
     * in 3-dimensional case it is a parallelepiped.
     * All edges of the hyper-parallelepiped are supposed to be parallel to coordinate axes.</p>
     *
     * <p>More precisely, the region, specified by this class, consists of all such points
     * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>), that:</p>
     *
     * <blockquote>
     * <code>coordRanges[0].{@link IRange#min() min()}</code> &le; <i>x</i><sub>0</sub> &le;
     * <code>coordRanges[0].{@link IRange#max() max()}</code>,<br>
     * <code>coordRanges[1].{@link IRange#min() min()}</code> &le; <i>x</i><sub>1</sub> &le;
     * <code>coordRanges[1].{@link IRange#max() max()}</code>,<br>
     * ...,<br>
     * <code>coordRanges[<i>n</i>&minus;1].{@link IRange#min()
     * min()}</code> &le; <i>x</i><sub><i>n</i>&minus;1</sub> &le;
     * <code>coordRanges[<i>n</i>&minus;1].{@link IRange#max() max()}</code>,
     * </blockquote>
     *
     * <p>where <code>coordRanges</code> is the result of {@link #coordRanges()} method.</p>
     *
     * <p>Hyperparallelepipeds can be created by the following methods:</p>
     *
     * <ul>
     * <li>{@link #getHyperparallelepiped(IRange...)},</li>
     * <li>{@link #getSegment(IRange)},</li>
     * <li>{@link #getRectangle2D(IRange, IRange)},</li>
     * <li>{@link #getParallelepiped3D(IRange, IRange, IRange)}.</li>
     * </ul>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static final class Hyperparallelepiped extends Region {
        private Hyperparallelepiped(IRange... coordRanges) {
            super(coordRanges);
        }

        @Override
        public boolean isRectangular() {
            return true;
        }

        @Override
        public boolean contains(long... coordinates) {
            for (int k = 0; k < n; k++) {
                if (!coordRanges[k].contains(coordinates[k])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Region[] sectionAtLastCoordinate(long sectionCoordinateValue) {
            if (!checkSectionAtLastCoordinate(sectionCoordinateValue)) {
                return EMPTY_REGIONS;
            }
            return new Region[]{new Hyperparallelepiped((IRange[]) JArrays.copyOfRange(coordRanges, 0, n - 1))};
        }

        /**
         * Returns <code>true</code> if and only if all points coordinates (with integer coordinates),
         * belonging to this hyper-parallelepiped, lie inside the specified matrix.
         *
         * <p>Note: the number of matrix dimensions can differ from the number of this region's dimensions.
         * (All matrix dimensions after the first {@link #n()}, as usual, are supposed to be 1.)
         *
         * <p>More precisely, this method returns <code>true</code> if and only if the following 2*{@link #n()}
         * conditions are fulfilled:
         *
         * <blockquote>
         * <code>0 &le; coordRanges[0].{@link IRange#min() min()}</code>,&nbsp;&nbsp;&nbsp;
         * <code>coordRanges[0].{@link IRange#max() max()} &lt; matrix.{@link Matrix#dim(int) dim}(0)</code>,<br>
         * <code>0 &le; coordRanges[1].{@link IRange#min() min()}</code>,&nbsp;&nbsp;&nbsp;
         * <code>coordRanges[1].{@link IRange#max() max()} &lt; matrix.{@link Matrix#dim(int) dim}(1)</code>,<br>
         * ...,<br>
         * <code>0 &le; coordRanges[<i>n</i>&minus;1].{@link IRange#min() min()}</code>,&nbsp;&nbsp;&nbsp;
         * <code>coordRanges[<i>n</i>&minus;1].{@link IRange#max() max()} &lt;
         * matrix.{@link Matrix#dim(int) dim}(<i>n</i>&minus;1)</code>
         * </blockquote>
         *
         * <p>This method is equivalent to the following call:
         * <code>{@link #isInsideMatrix(Matrix, long...) isInsideMatrix}(matrix, new long[0])</code>.
         *
         * @param matrix the matrix.
         * @return <code>true</code> if this region lies fully inside the specified matrix.
         * @throws NullPointerException if the <code>matrix</code> argument is {@code null}.
         */
        public boolean isInsideMatrix(Matrix<?> matrix) {
            return isInsideMatrix(matrix, new long[0]);
        }

        /**
         * Returns <code>true</code> if and only if the coordinates of all points (with integer coordinates),
         * belonging to this hyper-parallelepiped, will lie inside the specified matrix after subtraction
         * the specified values <code>backShifts</code> from them.
         *
         * <p>Note: the number of matrix dimensions can differ from the number of dimensions of this region.
         * (All matrix dimensions after the first {@link #n()}, as usual, are supposed to be 1.)
         * The number of elements of <code>backShifts</code> also can differ from the number of dimensions.
         * All missing elements of <code>backShifts</code> array are supposed to be zero.
         *
         * <p>More precisely, this method returns <code>true</code> if and only if the following 2*{@link #n()}
         * conditions are fulfilled:
         *
         * <blockquote>
         * <code>0 &le; coordRanges[0].{@link IRange#min() min()} - <i>sh</i><sub>0</sub></code>,<br>
         * <code>coordRanges[0].{@link IRange#max() max()} - <i>sh</i><sub>0</sub> &lt;
         * matrix.{@link Matrix#dim(int) dim}(0)</code>,<br>
         * <code>0 &le; coordRanges[1].{@link IRange#min() min()} - <i>sh</i><sub>1</sub></code>,<br>
         * <code>coordRanges[1].{@link IRange#max() max()} - <i>sh</i><sub>1</sub> &lt;
         * matrix.{@link Matrix#dim(int) dim}(1)</code>,<br>
         * ...,<br>
         * <code>0 &le; coordRanges[<i>n</i>&minus;1].{@link IRange#min() min()} -
         * <i>sh</i><sub><i>n</i>&minus;1</sub></code>,<br>
         * <code>coordRanges[<i>n</i>&minus;1].{@link IRange#max() max()} - <i>sh</i><sub><i>n</i>&minus;1</sub> &lt;
         * matrix.{@link Matrix#dim(int) dim}(<i>n</i>&minus;1)</code>,
         * </blockquote>
         *
         * <p>where <code>sh<sub><i>k</i></sub> = k &lt; backShifts.length ? backShifts[k] : 0</code>
         *
         * @param matrix     the matrix.
         * @param backShifts the shifts, which are subtracted from all coordinates of this region before the check.
         * @return <code>true</code> if this region, shifted backwards by the specified shifts,
         * lies fully inside the specified matrix.
         * @throws NullPointerException if the <code>matrix</code> or <code>backShifts</code> argument
         *                              is {@code null}.
         */
        public boolean isInsideMatrix(Matrix<?> matrix, long... backShifts) {
            Objects.requireNonNull(matrix, "Null matrix argument");
            Objects.requireNonNull(backShifts, "Null backShifts argument");
            for (int k = 0; k < n; k++) {
                long shift = k < backShifts.length ? backShifts[k] : 0;
                if (coordRanges[k].min() - shift < 0) {
                    return false;
                }
                if (coordRanges[k].max() - shift >= matrix.dim(k)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            return n + "-dimensional rectangular region " + JArrays.toString(coordRanges, "x", 100);
        }

        @Override
        boolean sectionIsUninterruptedSegment(long sectionCoordinateValue) {
            return n == 2 && coordRanges[1].contains(sectionCoordinateValue);
        }

        @Override
        boolean segmentSectionAtLastCoordinate(MutableIRange result, long sectionCoordinateValue) {
            result.min = minX;
            result.max = maxX;
            return true;
        }
    }

    /**
     * <p>Convex hyper-polyhedron: an intersection of several <i>n</i>-dimensional
     * half-spaces and some {@link Matrices.Hyperparallelepiped hyper-parallelepiped}.
     * While creating regions of this class, it is always necessary to specify some containing
     * hyper-parallelepiped. The coordinate ranges, returned by {@link #coordRanges()} method of this class,
     * are the corresponding ranges of the specified containing hyper-parallelepiped.</p>
     *
     * <p>More precisely, the region, specified by this class, consists of all such points
     * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>), that:</p>
     *
     * <blockquote>
     * <i>a</i><sub>00</sub><i>x</i><sub>0</sub> + <i>a</i><sub>01</sub><i>x</i><sub>1</sub> + ...
     * + <i>a</i><sub>0,<i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> &le; <i>b</i><sub>0</sub>,<br>
     * <i>a</i><sub>10</sub><i>x</i><sub>0</sub> + <i>a</i><sub>11</sub><i>x</i><sub>1</sub> + ...
     * + <i>a</i><sub>1,<i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> &le; <i>b</i><sub>1</sub>,<br>
     * ...,<br>
     * <i>a</i><sub><i>m</i>&minus;1,0</sub><i>x</i><sub>0</sub>
     * + <i>a</i><sub><i>m</i>&minus;1,1</sub><i>x</i><sub>1</sub> + ...
     * + <i>a</i><sub><i>m</i>&minus;1,<i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> &le;
     * <i>b</i><sub><i>m</i>&minus;1</sub>,<br>
     * and also this point belongs to the specified containing {@link Hyperparallelepiped hyper-parallelepiped}:<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<code>{@link #coordRange(int)
     * coordRange}(<i>k</i>).{@link IRange#min() min()}</code> &le;
     * <i>x</i><sub><i>k</i></sub> &le;
     * <code>{@link #coordRange(int) coordRange}(<i>k</i>).{@link IRange#max() max()}</code> for all
     * <i>k</i>=0,1,...,<i>n</i>&minus;1.
     * </blockquote>
     *
     * <p>The number of inequalities <i>m</i> can be any non-negative integer 0,1,2,...
     * (the degenerated case <i>m</i>=0 is equivalent to the hyper-parallelepiped).</p>
     *
     * <p>Convex hyper-polyhedrons can be created by the following methods:</p>
     *
     * <ul>
     * <li>{@link #getConvexHyperpolyhedron(double[] a, double[] b, IRange... coordRanges)},</li>
     * <li>{@link #getSimplex(double[][] vertices)},</li>
     * <li>{@link #getTriangle2D(double x1, double y1, double x2, double y2, double x3, double y3)},</li>
     * <li>{@link #getTetrahedron3D(double x1, double y1, double z1,
     * double x2, double y2, double z2,
     * double x3, double y3, double z3,
     * double x4, double y4, double z4)}.</li>
     * </ul>
     *
     * <p>In the first method, you must directly specify
     * the matrix <b>A</b> of coefficients <i>a</i><sub><i>ij</i></sub>,
     * the vector <b>b</b> of coefficients <i>b</i><sub><i>i</i></sub> and the containing hyper-parallelepiped.
     * (The containing hyper-parallelepiped will be identical to the hyper-parallelepiped, constructed
     * by {@link #getHyperparallelepiped(IRange... coordRanges)} method with the same <code>coordRanges</code>.)</p>
     *
     * <p>Other 3 methods build a {@link Simplex simplex} &mdash; a particular case of the convex hyper-polyhedron.
     * In these cases you need to specify its vertices only; necessary matrix <b>A</b> and vector <b>b</b>
     * are calculated automatically.</p>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static class ConvexHyperpolyhedron extends Region {
        final double[] a, b;

        // coordRanges is the first argument to provide early check of arguments in inheritors
        private ConvexHyperpolyhedron(IRange[] coordRanges, double[] a, double[] b) {
            super(coordRanges);
            Objects.requireNonNull(a, "Null A coefficients");
            Objects.requireNonNull(b, "Null b coefficients");
            if (a.length != (long) n * (long) b.length) {
                throw new IllegalArgumentException("Illegal size of A matrix: a.length=" + a.length
                        + " must be equal to b.length*n=" + (long) n * (long) b.length);
            }
            this.a = a.clone();
            this.b = b.clone();
        }

        @Override
        public boolean contains(long... coordinates) {
            for (int k = 0; k < n; k++) {
                if (!coordRanges[k].contains(coordinates[k])) {
                    return false;
                }
            }
            for (int i = 0, ofs = 0; i < b.length; i++) {
                double scalarProd = 0.0;
                for (int j = 0; j < n; j++, ofs++) {
                    scalarProd += a[ofs] * coordinates[j];
                }
                if (scalarProd > b[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Region[] sectionAtLastCoordinate(long sectionCoordinateValue) {
            if (!checkSectionAtLastCoordinate(sectionCoordinateValue)) {
                return EMPTY_REGIONS;
            }
            // We have the following inequalities system (below m=this.n-1):
            //     a00*x0 + a01*x1 + a02*x2 + a03*x3 + ... + a0m*xm <= b0
            //     a10*x0 + a11*x1 + a12*x2 + a13*x3 + ... + a1m*xm <= b1
            //                                        . . .
            // Let xm=c=sectionCoordinateValue. Then the modified system will be very simple:
            //     a00*x0 + a01*x1 + a02*x2 + a03*x3 + ... + a0(m-1)*x(m-1) <= b0-a0m*c
            //     a10*x0 + a11*x1 + a12*x2 + a13*x3 + ... + a1(m-1)*x(m-1) <= b1-a1m*c
            //                                        . . .
            if (n > 2) {
                double[] newA = new double[(n - 1) * b.length];
                double[] newB = new double[b.length];
                for (int i = 0, ofs = 0; i < b.length; i++) {
                    System.arraycopy(a, ofs, newA, i * (n - 1), n - 1);
                    ofs += n - 1;
                    newB[i] = b[i] - a[ofs] * sectionCoordinateValue;
                    ofs++;
                }
                return new Region[]{new ConvexHyperpolyhedron(
                        (IRange[]) JArrays.copyOfRange(coordRanges, 0, n - 1),
                        newA, newB)
                };
            } else {
                assert n == 2;
                MutableIRange xRange = new MutableIRange();
                if (!segmentSectionAtLastCoordinate(xRange, sectionCoordinateValue)) {
                    return EMPTY_REGIONS;
                }
                return new Region[]{new Hyperparallelepiped(IRange.of(xRange.min, xRange.max))};
            }
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            return n + "-dimensional convex hyperpolyhedral region (inside "
                    + JArrays.toString(coordRanges, "x", 100) + ")";
        }

        @Override
        boolean sectionIsUninterruptedSegment(long sectionCoordinateValue) {
            return n == 2 && coordRanges[1].contains(sectionCoordinateValue);
        }

        @Override
        boolean segmentSectionAtLastCoordinate(MutableIRange result, long sectionCoordinateValue) {
            // The modified system:
            //     a00*x <= b0-a01*c
            //     a10*x <= b1-a11*c
            //        . . .
            // We should find the corresponding segment minX..maxX
            double left = coordRanges[0].min();
            double right = coordRanges[0].max();
            for (int i = 0; i < b.length; i++) {
                double c = a[2 * i];
                double d = b[i] - a[2 * i + 1] * sectionCoordinateValue;
                // We have the inequality: c*x <= d
                if (c > 0.0) { // x <= d/c
                    double v = d / c;
                    if (v < right) {
                        right = v;
                    }
                } else if (c < 0.0) {  // x >= d/c
                    double v = d / c;
                    if (v > left) {
                        left = v;
                    }
                } else { // degenerated inequality: 0 <= d
                    if (d < 0.0) {
                        return false;
                    } // if d >= 0.0, this condition should be skipped
                }
            }
            result.min = (long) StrictMath.ceil(left);
            result.max = (long) StrictMath.floor(right);
            return result.min <= result.max;
        }

        /**
         * Returns the matrix <b>A</b>: coefficients of the left side of inequalities, defining the half-spaces
         * (see the {@link Matrices.ConvexHyperpolyhedron comments to this class}).
         * The elements of the matrix <b>A</b> will be listed in the returned array row by row:
         * <i>a</i><sub><i>ij</i></sub>=<code>a[<i>i</i>*<i>n</i>+<i>j</i>]</code>,
         * <i>i</i> is the index of the row (0..<i>m</i>&minus;1),
         * <i>j</i> is the index of the column (0..<i>n</i>&minus;1),
         * <code>a</code> is the result of this method with length <code>a.length=</code><i>nm</i>.
         *
         * <p>The returned array is a clone of the internal array stored in this object.
         *
         * @return the matrix <b>A</b>: coefficients of the left side of inequalities, defining the half-spaces.
         */
        public double[] a() {
            return a.clone();
        }

        /**
         * Returns the vector <b>b</b>: right sides of inequalities, defining the half-spaces
         * (see the {@link Matrices.ConvexHyperpolyhedron comments to this class}).
         * The elements of the vector <b>b</b> will be listed in the returned array.
         * The length of the returned array is the number of half-spaces.
         *
         * <p>The returned array is a clone of the internal array stored in this object.
         *
         * @return the vector <b>b</b>: right sides of inequalities, defining the half-spaces.
         */
        public double[] b() {
            return b.clone();
        }
    }

    /**
     * <p>Simplex: the simplest <i>n</i>-dimensional hyper-polyhedron with <i>n</i>+1 vertices.
     * In 1-dimensional case it is a segment,
     * in 2-dimensional case it is a triangle,
     * in 3-dimensional case it is a tetrahedron.</p>
     *
     * <p>Simplex is a particular case of the {@link Matrices.ConvexHyperpolyhedron convex hyper-polyhedron}.</p>
     *
     * <p>Simplex is specified by its vertices and can be created by the following methods:</p>
     *
     * <ul>
     * <li>{@link #getSimplex(double[][] vertices)},</li>
     * <li>{@link #getTriangle2D(double x1, double y1, double x2, double y2, double x3, double y3)},</li>
     * <li>{@link #getTetrahedron3D(double x1, double y1, double z1,
     * double x2, double y2, double z2,
     * double x3, double y3, double z3,
     * double x4, double y4, double z4)}.</li>
     * </ul>
     *
     * <p>Note: degenerated simplexes, when all vertices lie in the same <i>(n&minus;1)</i>-dimensional hyperplane,
     * are not allowed. (In 1-dimensional it means that 2 vertices are identical,
     * in 2-dimensional &mdash; that 3 vertices lie in the same straight line,
     * in 2-dimensional &mdash; that 4 vertices lie in the same plane.)
     * Such simplexes cannot be constructed by the methods above:
     * {@link DegeneratedSimplexException} is thrown in these cases.
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static final class Simplex extends ConvexHyperpolyhedron {
        private final double[][] vertices;

        private Simplex(double[][] vertices) {
            super(coordRangesOfVertices(vertices, 0),
                    new double[(int) Math.min(Integer.MAX_VALUE,
                            (long) vertices[0].length * ((long) vertices[0].length + 1))],
                    new double[(int) Math.min(Integer.MAX_VALUE, (long) vertices[0].length + 1)]);
            this.vertices = vertices.clone(); // this field must be assigned before the following exception
            boolean degenerated = !buildSimplexByVertices(vertices, this.a, this.b);
            if (degenerated) {
                throw new DegeneratedSimplexException("Degenerated simplex is not allowed: " + this);
            }
            for (int k = 0; k < vertices.length; k++) {
                this.vertices[k] = vertices[k].clone();
            }
        }

        /**
         * Returns <code>true</code> if and only if the specified vertices lies in the same
         * <i>(n&minus;1)</i>-dimensional hyperplane, as far as it can be detected by analysing the coordinates
         * via calculations with standard Java <code>double</code> numbers.
         * Here <i>n</i>=<code>vertices[<i>k</i>].length</code>; this length must be
         * same for all <i>k</i>, and <code>vertices.length</code> must be equal to <i>n</i>+1.
         *
         * <p>{@link #getSimplex(double[][] vertices)} method throws {@link DegeneratedSimplexException} if
         * and only if this method returns <code>true</code> for the same argument.
         *
         * @param vertices coordinates of all vertices.
         * @return whether the simplex with the specified vertices is degenerated and cannot be described
         * by this class.
         * @throws NullPointerException     if the <code>vertices</code> array or some of its elements
         *                                  is {@code null}.
         * @throws IllegalArgumentException if the <code>vertices</code> array or some of its elements is empty
         *                                  (has zero length),
         *                                  or if the length of some <code>vertices[k]</code> array is not equal to
         *                                  <code>vertices[0].length+1</code>.
         */
        public static boolean isSimplexDegenerated(double[][] vertices) {
            coordRangesOfVertices(vertices, 0);
            return !buildSimplexByVertices(vertices,
                    new double[(int) Math.min(Integer.MAX_VALUE,
                            (long) vertices[0].length * ((long) vertices[0].length + 1))],
                    new double[(int) Math.min(Integer.MAX_VALUE, (long) vertices[0].length + 1)]);
        }

        /**
         * Returns the coordinates of all vertices of the simplex.
         * The returned arrays is identical to an array, passed to {@link #getSimplex(double[][] vertices)}
         * method.
         *
         * <p>The returned array is a deep clone of the internal data stored in this object:
         * no references, maintained by this object, are returned.
         *
         * @return the coordinates of all vertices of the simplex: the element (line) #<code>k</code> of the returned
         * 2-dimensional array contains <i>n</i> coordinates of the vertex #<code>k</code>.
         */
        public double[][] vertices() {
            double[][] result = new double[vertices.length][];
            for (int k = 0; k < result.length; k++) {
                result[k] = vertices[k].clone();
            }
            return result;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(n + "-dimensional simplex"
                    + (n == 2 ? " (triangle)" : n == 3 ? " (tetrahedron)" : "")
                    + " with vertices ");
            for (int k = 0; k < vertices.length; k++) {
                if (k > 0) {
                    sb.append(", ");
                }
                sb.append("(").append(JArrays.toString(vertices[k], ",", 100)).append(")");
            }
            return sb.toString();
        }
    }

    /**
     * <p>2-dimensional polygon. It can be non-convex and even self-intersecting.
     *
     * <p>The points, lying precisely in the sides of the polygon (in particular, the vertices),
     * belong to this region.
     *
     * <p>Any degenerated cases are allowed: for example, vertices can be equal.
     *
     * <p>Polygon is specified by its vertices and can be created by the following method:</p>
     *
     * <ul>
     * <li>{@link #getPolygon2D(double[][] vertices)}.</li>
     * </ul>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static final class Polygon2D extends Region {
        private final double[] vx, vy;

        private Polygon2D(double[][] vertices) {
            super(coordRangesOfVertices(vertices, 2));
            this.vx = new double[vertices.length];
            this.vy = new double[vertices.length];
            for (int k = 0; k < vertices.length; k++) {
                this.vx[k] = vertices[k][0];
                this.vy[k] = vertices[k][1];
            }
        }

        @Override
        public boolean contains(long... coordinates) {
            throw new UnsupportedOperationException("Inside method is not supported by " + getClass());
        }

        @Override
        public boolean isContainsSupported() {
            return false;
        }

        @Override
        public Region[] sectionAtLastCoordinate(long sectionY) {
            if (!checkSectionAtLastCoordinate(sectionY)) {
                return EMPTY_REGIONS;
            }
            final int m = vx.length;
            double[] sectionX = new double[m];
            int horizontalCount = 0, sectionCount = 0;
            for (int k = 0; k < m; k++) {
                double vxPrev = k > 0 ? vx[k - 1] : vx[m - 1];
                double vyPrev = k > 0 ? vy[k - 1] : vy[m - 1];
                if (vy[k] != sectionY) {
                    if (vy[k] > sectionY ? vyPrev < sectionY : vyPrev > sectionY) {
                        // intersection in an internal point
                        sectionX[sectionCount++] = vx[k]
                                + (vxPrev - vx[k]) * (sectionY - vy[k]) / (vyPrev - vy[k]);
                    }
                } else if (vyPrev != sectionY) { // && vy[k] == sectionY
                    // We come at this horizontal; we should scan further vertices and find, how we shall leave it
                    for (int i = k; ; ) {
                        ++i;
                        if (i == m) {
                            i = 0;
                        }
                        if (i == k) {
                            throw new AssertionError("Cannot find another vy, though vyPrev!=vy[k]");
                        }
                        double vyNext = vy[i];
                        if (vyNext != sectionY) {
                            sectionX[sectionCount++] = vx[k];
                            if ((vyNext > sectionY) == (vyPrev > sectionY)) {
                                // it means that we only touch the horizontal; in another case, we pass through it
                                sectionX[sectionCount++] = vx[k]; // adding this vertex twice
                            }
                            break;
                        }
                    }
                } else { // vy[k] == vyPrev == sectionY: count the horizontal segment.
                    // We add a segment even if vx[k]==vxPrev: it is possible that all vertices are the same point
                    horizontalCount++;
                }
            }
            if (sectionCount % 2 != 0) {
                throw new AssertionError("Odd number " + sectionCount + " of intersections of " + this
                        + " and the horizontal y=" + sectionY);
            }
            if ((long) (sectionCount / 2) + (long) horizontalCount > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Too large number of horizontal segments");
            }
            java.util.Arrays.sort(sectionX, 0, sectionCount);
            Region[] result = new Region[sectionCount / 2 + horizontalCount];
            int resultCount = 0;
            for (int k = 0; k < sectionCount; k += 2) {
                long minX = (long) StrictMath.ceil(sectionX[k]);
                long maxX = (long) StrictMath.floor(sectionX[k + 1]);
                if (minX <= maxX) {
                    result[resultCount++] = getSegment(IRange.of(minX, maxX));
                }
            }
            if (horizontalCount > 0) { // second scanning in this rare situation
                for (int k = 0; k < m; k++) {
                    if (vy[k] == sectionY) {
                        double vxPrev = k > 0 ? vx[k - 1] : vx[m - 1];
                        double vyPrev = k > 0 ? vy[k - 1] : vy[m - 1];
                        if (vyPrev == sectionY) {
                            long minX = (long) StrictMath.ceil(StrictMath.min(vx[k], vxPrev));
                            long maxX = (long) StrictMath.floor(StrictMath.max(vx[k], vxPrev));
                            if (minX <= maxX) {
                                result[resultCount++] = getSegment(IRange.of(minX, maxX));
                            }
                        }
                    }
                }
            }
            if (resultCount < result.length) { // rare situation: some segments do not contain integer points
                result = (Region[]) JArrays.copyOfRange(result, 0, resultCount);
            }
            return result;
        }

        /**
         * Returns the number of vertices of the polygon. It is the number of elements in the array,
         * returned by {@link #vertices()} method.
         *
         * @return the number of vertices of the polygon.
         */
        public int verticesCount() {
            return vx.length;
        }

        /**
         * Returns the <i>x</i>-coordinate of the vertix <code>#index</code>.
         *
         * @param index the index of vertex.
         * @return the <i>x</i>-coordinate of the vertix <code>#index</code>.
         * @throws IndexOutOfBoundsException if <code>index&lt;0</code> or
         *                                   <code>index&ge;{@link #verticesCount()}</code>.
         */
        public double vertexX(int index) {
            return vx[index];
        }

        /**
         * Returns the <i>y</i>-coordinate of the vertix <code>#index</code>.
         *
         * @param index the index of vertex.
         * @return the <i>y</i>-coordinate of the vertix <code>#index</code>.
         * @throws IndexOutOfBoundsException if <code>index&lt;0</code> or
         *                                   <code>index&ge;{@link #verticesCount()}</code>.
         */
        public double vertexY(int index) {
            return vy[index];
        }

        /**
         * Returns the coordinates of all vertices of the polygon.
         * The returned arrays is identical to an array, passed to {@link #getPolygon2D(double[][] vertices)}
         * method.
         *
         * <p>The returned array is a deep clone of the internal array stored in this object:
         * no references, maintained by this object, are returned.
         *
         * @return the coordinates of all vertices of the simplex: the element (line) #<code>k</code> of the returned
         * 2-dimensional array contains <i>2</i> coordinates of the vertex #<code>k</code>.
         */
        public double[][] vertices() {
            double[][] result = new double[vx.length][2];
            for (int k = 0; k < result.length; k++) {
                result[k][0] = vx[k];
                result[k][1] = vy[k];
            }
            return result;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("2-dimensional polygon ");
            for (int k = 0; k < vx.length; k++) {
                if (k > 0) {
                    sb.append("-");
                }
                sb.append("(").append(vx[k]).append(",").append(vy[k]).append(")");
            }
            return sb.toString();
        }
    }

    /**
     * Returns new {@link Matrix matrix} (multi-dimensional array),
     * backed by the specified AlgART array, with the given dimensions.
     *
     * <p>The array must be {@link Array#isUnresizable() unresizable}.
     * The product of all dimensions (<code>dim[0]*dim[1]*...*dim[dim.length-1]</code>)
     * must be equal to <code>array.length()</code>. The dimensions must not be negative.
     * The {@link Matrix#dimCount()} method will return <code>dim.length</code>,
     * and {@link Matrix#dim(int) Matrix.dim(n)} method will return <code>dim[n]</code>.</p>
     *
     * <p>The passed <code>dim</code> argument is cloned by this method: no references to it
     * are maintained by the created matrix.</p>
     *
     * <p>This method has brief aliases:<br>
     * {@link Array#matrix(long...)} and overridden versions in the subclasses:
     * {@link PArray#matrix(long...)},
     * {@link PFixedArray#matrix(long...)},
     * {@link PNumberArray#matrix(long...)},
     * {@link PFloatingArray#matrix(long...)},
     * {@link PIntegerArray#matrix(long...)},
     * {@link BitArray#matrix(long...)},
     * {@link CharArray#matrix(long...)},
     * {@link ByteArray#matrix(long...)},
     * {@link ShortArray#matrix(long...)},
     * {@link IntArray#matrix(long...)},
     * {@link LongArray#matrix(long...)},
     * {@link FloatArray#matrix(long...)},
     * {@link DoubleArray#matrix(long...)},<br>
     * {@link UpdatableArray#matrix(long...)} and overridden versions in the subclasses:
     * {@link UpdatablePArray#matrix(long...)},
     * {@link UpdatablePFixedArray#matrix(long...)},
     * {@link UpdatablePNumberArray#matrix(long...)},
     * {@link UpdatablePFloatingArray#matrix(long...)},
     * {@link UpdatablePIntegerArray#matrix(long...)},
     * {@link UpdatableBitArray#matrix(long...)},
     * {@link UpdatableCharArray#matrix(long...)},
     * {@link UpdatableByteArray#matrix(long...)},
     * {@link UpdatableShortArray#matrix(long...)},
     * {@link UpdatableIntArray#matrix(long...)},
     * {@link UpdatableLongArray#matrix(long...)},
     * {@link UpdatableFloatArray#matrix(long...)},
     * {@link UpdatableDoubleArray#matrix(long...)}.</p>
     *
     * @param <T>   the generic type of AlgART array.
     * @param array an array storing all matrix elements.
     * @param dim   the matrix dimensions.
     * @return new matrix backed by <code>array</code> with the given dimensions.
     * @throws NullPointerException     if <code>array</code> or <code>dim</code> argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is resizable
     *                                  (for example, implements {@link MutableArray}),
     *                                  or if the number of dimensions is 0 (empty <code>dim</code> Java array),
     *                                  or if some of the dimensions are negative.
     * @throws SizeMismatchException    if the product of all dimensions is not equal to the array length.
     * @throws TooLargeArrayException   if the product of all dimensions is greater than <code>Long.MAX_VALUE</code>.
     * @see MemoryModel#newMatrix(Class, Class, long...)
     * @see #matrixAtSubArray(Array, long, long...)
     */
    public static <T extends Array> Matrix<T> matrix(T array, long... dim) {
        return new MatrixImpl<>(array, dim);
    }

    /**
     * Returns new {@link Matrix matrix} (multi-dimensional array) with the given dimensions,
     * backed by the part of the specified AlgART array, starting from the given position.
     * Unlike {@link #matrix(Array, long...)}, this method allows to use an array, the length
     * of which is greater than the product of all passed dimensions:
     * its {@link Array#subArray(long, long) subarray} with <code>fromIndex=position</code>
     * will be used instead of the full array.
     *
     * <p>The array must be {@link Array#isUnresizable() unresizable}.
     * The <code>position</code> must be in range <code>0..array.length()</code>, and
     * the product of all dimensions (<code>dim[0]*dim[1]*...*dim[dim.length-1]</code>)
     * must be not greater than <code>array.length()-position</code>.
     * The dimensions and the given position must not be negative.
     * The {@link Matrix#dimCount()} method will return <code>dim.length</code>,
     * and {@link Matrix#dim(int) Matrix.dim(n)} method will return <code>dim[n]</code>.
     *
     * <p>This method returns the same result as the call
     * <code>{@link #matrix(Array, long...) matrix}(array.{@link Array#subArr(long, long)
     * subArr}(position,product),dim)</code>, where <code>product=dim[0]*dim[1]*...*dim[dim.length-1]</code>.
     * But {@link Array#subArr(long, long) subArr} method is not called in a case of invalid dimensions;
     * in this case, an exception is thrown.
     *
     * <p>The passed <code>dim</code> argument is cloned by this method: no references to it
     * are maintained by the created matrix.
     *
     * @param <T>      the generic type of AlgART array.
     * @param array    an array storing all matrix elements.
     * @param position the starting position inside the array, from which the matrix elements are placed.
     * @param dim      the matrix dimensions.
     * @return new matrix backed by <code>array</code> with the given dimensions.
     * @throws NullPointerException      if <code>array</code> or <code>dim</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal <code>position</code>
     *                                   (<code>position &lt; 0 || position &gt; array.length()</code>).
     * @throws IllegalArgumentException  if the passed array is resizable
     *                                   (for example, implements {@link MutableArray}),
     *                                   or if the number of dimensions is 0 (empty <code>dim</code> Java array),
     *                                   or if some of the dimensions are negative.
     * @throws SizeMismatchException     if the product of all dimensions is greater than
     *                                   <code>array.length()-position</code>.
     * @throws TooLargeArrayException    if the product of all dimensions is greater than <code>Long.MAX_VALUE</code>.
     * @see MemoryModel#newMatrix(Class, Class, long...)
     */
    public static <T extends Array> Matrix<T> matrixAtSubArray(T array, long position, long... dim) {
        if (array != null && dim != null) {
            if (position < 0 || position > array.length()) {
                throw new IndexOutOfBoundsException("Illegal position = " + position
                        + " (must be in range 0.." + array.length() + ")");
            }
            dim = dim.clone();
            long correctLen = AbstractMatrix.checkDimensions(dim);
            if (correctLen > array.length() - position) {
                throw new SizeMismatchException("Dimensions are too large for the given position and length:"
                        + " dim[0] * dim[1] * ... = " + correctLen + ", which is greater than the array length "
                        + array.length() + " + position " + position);
            }
            array = InternalUtils.cast(array.subArr(position, correctLen));
        }
        return new MatrixImpl<>(array, dim);
    }

    /**
     * Checks, whether the passed <code>arraySupertype</code> is one of 18 basic non-mutable array types
     * <code>{@link BitArray}.class</code>, <code>{@link CharArray}.class</code>,
     * <code>{@link ByteArray}.class</code>, <code>{@link ShortArray}.class</code>,
     * <code>{@link IntArray}.class</code>, <code>{@link LongArray}.class</code>,
     * <code>{@link FloatArray}.class</code>, <code>{@link DoubleArray}.class</code>,
     * <code>{@link ObjectArray}.class</code>,
     * <code>{@link UpdatableBitArray}.class</code>, <code>{@link UpdatableCharArray}.class</code>,
     * <code>{@link UpdatableByteArray}.class</code>, <code>{@link UpdatableShortArray}.class</code>,
     * <code>{@link UpdatableIntArray}.class</code>, <code>{@link UpdatableLongArray}.class</code>,
     * <code>{@link UpdatableFloatArray}.class</code>, <code>{@link UpdatableDoubleArray}.class</code>,
     * <code>{@link UpdatableObjectArray}.class</code>,
     * suitable for storing the specified element type.
     * In another case, an exception is thrown.
     *
     * <p>More precisely, this method throws <code>ClassCastException</code>, if
     * <code>!arraySupertype.isAssignableFrom(type)</code>, where
     * <code>type={@link Arrays#type Arrays.type}(UpdatableArray.class,elementType)</code>.
     * Before this check, this method also throws <code>IllegalArgumentException</code> if the passed
     * type is {@link MutableArray mutable}, i.e. if <code>MutableArray.class.isAssignableFrom(arraySupertype)</code>.
     * And in the very beginning this method throws an exception if one of its arguments is {@code null}
     * or if <code>elementType==void.class</code>.
     * In all other cases, this method does nothing.
     *
     * <p>This method is useful if you are planning to create a new matrix with the given element type,
     * for example, by {@link MemoryModel#newMatrix(Class, Class, long...) MemoryModel.newMatrix} method,
     * and you want to be sure that you will be able to {@link Matrix#cast(Class) cast it} to
     * the specified generic type Matrix&lt;T&gt;.
     *
     * <p>Note: unlike {@link MemoryModel#newMatrix(Class, Class, long...) MemoryModel.newMatrix} method,
     * this method allows specifying non-updatable <code>arraySupertype</code> (for example,
     * <code>{@link ByteArray}.class</code>).
     *
     * @param <T>            the generic type of AlgART array.
     * @param arraySupertype the desired type of the underlying array (the generic argument of the matrix type).
     * @param elementType    the type of matrix elements.
     * @throws NullPointerException     if <code>elementType</code> or <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if <code>elementType</code> is <code>void.class</code>
     *                                  or if <code>arraySupertype</code> is {@link MutableArray} or its subtype.
     * @throws ClassCastException       if <code>arraySupertype</code> and <code>elementType</code> do not match.
     */
    public static <T extends Array> void checkNewMatrixType(Class<T> arraySupertype, Class<?> elementType) {
        Objects.requireNonNull(arraySupertype, "Null arraySupertype argument");
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (MutableArray.class.isAssignableFrom(arraySupertype)) {
            throw new IllegalArgumentException("Illegal arraySupertype = " + arraySupertype
                    + ": it is MutableArray or its subtype, but a matrix cannot be based on a resizable array");
        }
        Class<UpdatableArray> type = Arrays.type(UpdatableArray.class, elementType);
        if (!arraySupertype.isAssignableFrom(type)) {
            throw new ClassCastException("The passed array supertype " + arraySupertype.getName()
                    + " is not a supertype for " + type.getName());
        }
    }

    /**
     * Estimates the size of the matrix in bytes.
     * Equivalent to <code>{@link Arrays#sizeOf(Array) Arrays.sizeOf}(matrix.{@link Matrix#array() array()})</code>.
     *
     * @param matrix some AlgART matrix.
     * @return the estimated size of this matrix in bytes.
     * @throws NullPointerException if the argument is {@code null}.
     * @see Arrays#sizeOf(Array)
     */
    public static long sizeOf(Matrix<?> matrix) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        return Arrays.sizeOf(matrix.array());
    }

    /**
     * Estimates the size of the matrix in megabytes.
     * Equivalent to <code>(double) {@link #sizeOf(Matrix)} / 1048576.0</code>.
     *
     * @param matrix some AlgART matrix.
     * @return the estimated size of this matrix in megabytes.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public static double sizeOfMB(Matrix<?> matrix) {
        return (double) sizeOf(matrix) * INVERTED_MEGABYTE;
    }

    /**
     * Estimates the summary size of all the matrices in the specified collection in bytes.
     * This method just calls {@link #sizeOf(Matrix)} method for each element of this collection
     * and returns the sum of the results of all these calls.
     *
     * @param matrices some collection of AlgART matrices.
     * @return the estimated summary size of all these matrices in bytes.
     * @throws NullPointerException if the argument is {@code null} or
     *                              if one of elements of the passed collection is {@code null}.
     * @see Arrays#sizeOf(Array)
     */
    public static double sizeOf(Collection<? extends Matrix<?>> matrices) {
        Objects.requireNonNull(matrices, "Null matrices argument");
        double result = 0.0;
        for (Matrix<?> matrix : matrices) {
            result += sizeOf(matrix);
        }
        return result;
    }

    /**
     * Estimates the summary size of all the matrices in the specified collection in megabytes.
     * Equivalent to <code>{@link #sizeOf(Collection)} / 1048576.0</code>.
     *
     * @param matrices some collection of AlgART matrices.
     * @return the estimated summary size of all these matrices in megabytes.
     * @throws NullPointerException if the argument is {@code null} or
     *                              if one of elements of the passed collection is {@code null}.
     */
    public static double sizeOfMB(Collection<? extends Matrix<?>> matrices) {
        return sizeOf(matrices) * INVERTED_MEGABYTE;
    }

    /**
     * Returns default dimensions of a tile, used by {@link Matrix#tile()} method to create a tiled matrix.
     *
     * <p>The returned array is a newly created Java array, containing <code>dimCount</code> elements.
     * The elements of the returned array are positive numbers, probably equal to each other and not too large
     * (several hundreds or thousands).
     *
     * @param dimCount the number of dimensions of the matrix.
     * @return an array containing all dimensions of every tile in a tiled matrix,
     * created by {@link Matrix#tile()} method.
     */
    public static long[] defaultTileDimensions(int dimCount) {
        if (dimCount <= 0) {
            throw new IllegalArgumentException("Zero or negative number of dimensions");
        }
        long[] result = new long[dimCount];
        long defaultTileSide = InternalUtils.DEFAULT_MATRIX_TILE_SIDES[
                Math.min(result.length, InternalUtils.DEFAULT_MATRIX_TILE_SIDES.length - 1)];
        java.util.Arrays.fill(result, defaultTileSide);
        return result;
    }

    /**
     * Returns an updatable list <code>java.util.Arrays.asList(matrices.clone())</code>,
     * if all these matrices are not {@code null} and all their built-in arrays
     * <code>matrices[k].{@link Matrix#array() array()}</code> are instances of the required <code>arrayClass</code>,
     * or throws an exception in another case.
     *
     * <p>Unlike <code>java.util.Arrays.asList</code> method, this one allows creating a list
     * without unchecked warnings.
     *
     * <p>Note: this method clones the passed array <code>matrices</code>
     * before converting it into <code>java.util.List</code>
     * and before checking the types of arrays. It is necessary to provide a guarantee
     * that the elements of the returned list will not be changed to matrices of unallowed type.
     *
     * @param <T>        the generic type of AlgART arrays.
     * @param arrayClass the required class / interface of built-in arrays for all passed matrices.
     * @param matrices   an array of any matrices.
     * @return the <code>matrices</code> argument (the same reference to a Java array).
     * @throws NullPointerException if <code>arrayClass</code> or one of the passed matrices is {@code null}.
     * @throws ClassCastException   if one of matrices contains built-in AlgART array that is not an instance
     *                              of the type <code>arrayClass</code>
     *                              (<code>!arrayClass.isInstance(matrices[k].{@link Matrix#array() array()})</code>).
     */
    public static <T extends Array> List<Matrix<? extends T>> several(Class<T> arrayClass, Matrix<?>... matrices) {
        // Note: we don't need @SafeVarargs annotation, because we use non-reifiable type Matrix<?>
        // and avoid warnings by another way: explicit check of classes and InternalUtils.cast
        Objects.requireNonNull(arrayClass, "Null arrayClass argument");
        if (matrices.length == 0) {
            return java.util.Collections.emptyList();
        }
        matrices = matrices.clone();
        for (int k = 0; k < matrices.length; k++) {
            Objects.requireNonNull(matrices[k], "Null matrices[" + k + "] argument");
            if (!arrayClass.isInstance(matrices[k].array())) {
                throw new ClassCastException("Illegal type of matrices[" + k + "] argument (" + matrices[k]
                        + "; required array class is " + arrayClass.getName() + ")");
            }
        }
        Matrix<T>[] cast = InternalUtils.cast(matrices);
        return java.util.Arrays.asList(cast);
    }

    /**
     * Checks, whether all passed matrices are not {@code null} and have
     * {@link Matrix#dimEquals(Matrix) equal dimensions} and, if it is true,
     * creates and returns Java array of {@link Matrix#array() built-in AlgART arrays} of all passed matrices.
     * In other words, in the returned array the element <code>#k</code> is
     * <code>matrices.get(k).{@link Matrix#array() array()}</code>.
     * If some passed matrices are {@code null} or have different dimensions, throws a corresponding exception.
     *
     * @param <T>        the generic type of AlgART arrays.
     * @param arrayClass the required class / interface of built-in arrays for all passed matrices.
     * @param matrices   list of some matrices.
     * @return array of {@link Matrix#array() built-in AlgART arrays} of all passed matrices.
     * @throws NullPointerException  if <code>arrayClass</code> argument, the <code>matrices</code> list
     *                               or one of its elements is {@code null}.
     * @throws SizeMismatchException if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                               different dimensions.
     * @throws ClassCastException    if one of matrices contains built-in AlgART array that is not an instance
     *                               of the type <code>arrayClass</code>
     *                               (<code>!arrayClass.isInstance(matrices[k].{@link Matrix#array() array()})</code>);
     *                               it is impossible if you use generalized arguments.
     */
    public static <T extends Array> T[] arraysOfParallelMatrices(
            Class<T> arrayClass,
            List<? extends Matrix<?>> matrices) {
        return arraysOfParallelMatrices(arrayClass, matrices, false);
    }

    /**
     * Equivalent of {@link #arraysOfParallelMatrices(Class, List)} method, but,
     * if <code>requireIdenticalType=true</code>, also checks that all passed matrices have identical
     * {@link Matrix#elementType() element type}.
     *
     * @param <T>        the generic type of AlgART array.
     * @param arrayClass the required class / interface of built-in arrays for all passed matrices.
     * @param matrices   list of some matrices.
     * @return array of {@link Matrix#array() built-in AlgART arrays} of all passed matrices.
     * @throws NullPointerException     if <code>arrayClass</code> argument, the <code>matrices</code> list
     *                                  or one of its elements is {@code null}.
     * @throws SizeMismatchException    if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     * @throws IllegalArgumentException if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different element type.
     * @throws ClassCastException       if one of matrices contains built-in AlgART array that is not an instance
     *                                  of the type <code>arrayClass</code>
     *                                  (<code>!arrayClass.isInstance(matrices[k].{@link
     *                                  Matrix#array() array()})</code>).
     */
    public static <T extends Array> T[] arraysOfParallelMatrices(
            Class<T> arrayClass,
            Collection<? extends Matrix<?>> matrices,
            boolean requireIdenticalType) {
        Objects.requireNonNull(arrayClass, "Null arrayClass argument");
        Objects.requireNonNull(matrices, "Null list of matrices");
        matrices = new ArrayList<Matrix<?>>(matrices);
        // - to be sure that its size and content cannot be changed in parallel thread
        Matrix<?> m0 = null;
        Class<?> elementType = null;
        int k = 0;
        T[] result = InternalUtils.cast(java.lang.reflect.Array.newInstance(arrayClass, matrices.size()));
        for (Matrix<?> m : matrices) {
            Objects.requireNonNull(m, "Null matrix #" + k);
            if (!arrayClass.isInstance(m.array())) {
                throw new ClassCastException("Illegal type of the matrix #" + k + " (" + m
                        + "; required array class is " + arrayClass.getName() + ")");
            }
            if (m0 == null) {
                m0 = m;
                elementType = m.elementType();
            } else {
                if (requireIdenticalType && m.elementType() != elementType) {
                    throw new IllegalArgumentException("The matrix #" + k +
                            " and the matrix #0 element type mismatch: "
                            + "the matrix #" + k + " has " + m.elementType() +
                            " elements, the matrix #0 has " + elementType + " elements");
                }
                if (!m.dimEquals(m0)) {
                    throw new SizeMismatchException("The matrix #" + k + " and the matrix #0 dimensions mismatch: "
                            + "the matrix #" + k + " is " + m + ", the matrix #0 is " + m0);
                }
            }
            result[k] = InternalUtils.cast(m.array());
            k++;
        }
        return result;
    }

    /**
     * Applies the specified function to each matrix in the <code>channels</code> collection
     * and returns an {@link ArrayList} consisting of the results in the same order.
     *
     * <p>The same results can be obtained using the following code:
     *
     * <pre>
     *     channels.stream().map(function::apply).collect(Collectors.toCollection(ArrayList::new));
     * </pre>
     * <p>Before processing, this method also performs the check
     * <code>{@link #checkDimensionEquality(Collection) Matrices.checkDimensionEquality}(channels)</code>
     *
     * @param function some function applied to each matrix.
     * @param channels a collection of matrices (e.g., image color channels).
     * @return the list containing results of the function for each channel, in the same order.
     * @throws NullPointerException  if the <code>function</code> argument,
     *                               the <code>channels</code> list or one of its elements is {@code null}.
     * @throws SizeMismatchException if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                               different dimensions.
     */
    public static <R extends Array, T extends Array> List<Matrix<R>> apply(
            Function<? super Matrix<T>, ? extends Matrix<R>> function,
            Collection<? extends Matrix<? extends T>> channels) {
        Objects.requireNonNull(function, "Null function argument");
        Objects.requireNonNull(channels, "Null list of channels");
        Matrices.checkDimensionEquality(channels);
        final List<Matrix<R>> result = new ArrayList<>();
        for (Matrix<? extends T> channel : channels) {
            final Matrix<T> castChannel = new MatrixImpl<>(channel.array(), channel.dimensions());
            // - the same matrix, but with a strict generic type T
            final Matrix<R> applied = function.apply(castChannel);
            result.add(applied);
        }
        return result;
    }

    /**
     * Equivalent to {@link #separate(ArrayContext, List, Matrix)
     * separate((ArrayContext) null, result, interleaved)}.
     *
     * @param result      the list of the result matrices.
     * @param interleaved the source interleaved matrix.
     * @throws NullPointerException     if <code>interleaved</code> argument, the <code>results</code> list or
     *                                  one of its elements is {@code null}.
     * @throws SizeMismatchException    if some of the result matrices have dimensions, that do not match the last
     *                                  <i>n</i> dimensions
     *                                  <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     *                                  of (<i>n</i>+1)-dimensional source <code>interleaved</code> matrix.
     * @throws IllegalArgumentException if number of elements in the <code>result</code> list is not equal to
     *                                  the first <code>interleaved</code> dimension <i>M</i><sub>0</sub>,
     *                                  or if some of the result matrices have element type that is different from
     *                                  the element type of the source <code>interleaved</code> matrix.
     * @throws IllegalStateException    if <code>interleaved</code> matrix is 1-dimensional.
     */
    public static void separate(
            List<? extends Matrix<? extends UpdatablePArray>> result,
            Matrix<? extends PArray> interleaved) {
        separate(null, result, interleaved);
    }

    /**
     * Analog of <code>{@link #separate(ArrayContext, Matrix)}</code> method, that does not allocate memory,
     * but stores the results into previously created list of matrices.
     *
     * <p>Note: if the source <code>interleaved</code> matrix has (<i>n</i>+1)-dimensions
     * <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>,
     * then the <code>result</code> list must contain <i>M</i><sub>0</sub> elements, and each of them must be
     * <i>n</i>-dimensional matrix <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>.
     *
     * @param context     the context;
     *                    can be {@code null}, then {@link ArrayContext#DEFAULT_SINGLE_THREAD} will be used.
     * @param result      the list of the result matrices.
     * @param interleaved the source interleaved matrix.
     * @throws NullPointerException     if <code>interleaved</code> argument, the <code>results</code> list or
     *                                  one of its elements is {@code null}.
     * @throws SizeMismatchException    if some of the result matrices have dimensions, that do not match the last
     *                                  <i>n</i> dimensions
     *                                  <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     *                                  of (<i>n</i>+1)-dimensional source <code>interleaved</code> matrix.
     * @throws IllegalArgumentException if number of elements in the <code>result</code> list is not equal to
     *                                  the first <code>interleaved</code> dimension <i>M</i><sub>0</sub>,
     *                                  or if some of the result matrices have element type that is different from
     *                                  the element type of the source <code>interleaved</code> matrix.
     * @throws IllegalStateException    if <code>interleaved</code> matrix is 1-dimensional.
     */
    public static void separate(
            ArrayContext context,
            List<? extends Matrix<? extends UpdatablePArray>> result,
            Matrix<? extends PArray> interleaved) {
        Objects.requireNonNull(result, "Null result list");
        Objects.requireNonNull(interleaved, "Null interleaved matrix");
        if (context == null) {
            context = ArrayContext.DEFAULT_SINGLE_THREAD;
        }
        final UpdatablePArray[] arrays = arraysOfParallelMatrices(UpdatablePArray.class, result, true);
        checkDimensionEqualityWithInterleaving(interleaved, result);
        if (arrays.length > 0) {
            try (var unpacker = InterleavingBandsUnpacker.getInstance(context, arrays, interleaved.array())) {
                unpacker.process();
            }
        }
    }

    /**
     * Equivalent to {@link #separate(ArrayContext, Matrix)
     * separate((ArrayContext) null, interleaved)}.
     *
     * @param <T>         the generic type of the built-in AlgART arrays.
     * @param interleaved the source interleaved matrix.
     * @return a list of matrices: "channels", interleaved in the source matrix along the first dimension.
     * @throws NullPointerException  if <code>interleaved</code> argument is {@code null}.
     * @throws IllegalStateException if <code>interleaved</code> matrix is 1-dimensional.
     */
    public static <T extends PArray> List<Matrix<T>> separate(Matrix<? extends T> interleaved) {
        return separate((ArrayContext) null, interleaved);
    }

    /**
     * Equivalent to <code>{@link #separate(ArrayContext, Matrix, int)
     * separate}(context, interleaved, Integer.MAX_VALUE)</code> (no limitations).
     *
     * @param <T>         the generic type of the built-in AlgART arrays.
     * @param context     the context;
     *                    can be {@code null}, then {@link ArrayContext#DEFAULT_SINGLE_THREAD} will be used.
     * @param interleaved the source interleaved matrix.
     * @return a list of matrices: "channels", interleaved in the source matrix along the first dimension.
     * @throws NullPointerException  if <code>interleaved</code> argument is {@code null}.
     * @throws IllegalStateException if <code>interleaved</code> matrix is 1-dimensional.
     */
    public static <T extends PArray> List<Matrix<T>> separate(ArrayContext context, Matrix<? extends T> interleaved) {
        return separate(context, interleaved, Integer.MAX_VALUE);
    }

    /**
     * Equivalent to {@link #separate(ArrayContext, Matrix, int)
     * separate((ArrayContext) null, interleaved, limit)}.
     *
     * @param <T>         the generic type of the built-in AlgART arrays.
     * @param interleaved the source interleaved matrix.
     * @param limit       maximal allowed number of returned matrices (the first dimension of the source matrix).
     * @return a list of matrices: "channels", interleaved in the source matrix along the first dimension
     * (like the red, green, blue channels for 3-channel RGB image, stored in RGBRGB... format).
     * @throws NullPointerException     if <code>interleaved</code> argument is {@code null}.
     * @throws IllegalStateException    if <code>interleaved</code> matrix is 1-dimensional.
     * @throws IllegalArgumentException if <code>limit &le; 0</code> or
     *                                  if the number of returned matrices {@code >limit}.
     */
    public static <T extends PArray> List<Matrix<T>> separate(
            Matrix<? extends T> interleaved,
            int limit) {
        return separate(null, interleaved, limit);
    }

    /**
     * Splits a single (<i>n</i>+1)-dimensional <code>interleaved</code> matrix
     * <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     * along dimension #0
     * to <i>M</i><sub>0</sub> separate <i>n</i>-dimensional matrices
     * <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     * and returns them as a list.
     * The element with index (<i>i</i><sub>0</sub>,<i>i</i><sub>1</sub>,...,<i>i<sub>n</sub></i>)
     * of the source matrix will be equal to the element with index
     * (<i>i</i><sub>1</sub>,...,<i>i<sub>n</sub></i>)
     * of the matrix <code>list.get(<i>i</i><sub>0</sub>)</code> in the returned list.
     *
     * <p>If the first dimension <i>M</i><sub>0</sub>=0, the returned list will be empty.</p>
     *
     * <p>This method also checks, that the first dimension
     * <i>M</i><sub>0</sub>=<code>interleaved.dim(0)</code> (which will be
     * equal to the size of the returned list) is not greater than the passed limit
     * and throws an exception if this limit is exceeded. Typically, this method is used for unpacking
     * matrices where the first dimension cannot be too large &mdash; for example the number
     * of color channels in RGBRGB... interleaving format &mdash; so it makes sense to limit
     * this value, because too large first dimension (millions) usually means incorrect usage of this function.
     * In any case, the number of returned matrices, greater than {@code Integer.MAX_VALUE}, usually leads to
     * <code>OutOfMemoryError</code>.</p>
     *
     * <p>Note that the result of this method is a newly created
     * modifiable <code>List</code> (probable {@link ArrayList}):
     * you can modify it, in particular remove or add new elements.</p>
     *
     * @param <T>         the generic type of the built-in AlgART arrays.
     * @param context     the context; allows specifying (in particular)
     *                    the memory model for creating returned matrices;
     *                    can be {@code null}, then {@link ArrayContext#DEFAULT_SINGLE_THREAD} will be used.
     * @param interleaved the source interleaved matrix.
     * @param limit       maximal allowed number of returned matrices (the first dimension of the source matrix).
     * @return a list of matrices: "channels", interleaved in the source matrix along the first dimension
     * (like the red, green, blue channels for 3-channel RGB image, stored in RGBRGB... format).
     * @throws NullPointerException     if <code>interleaved</code> argument is {@code null}.
     * @throws IllegalStateException    if <code>interleaved</code> matrix is 1-dimensional.
     * @throws IllegalArgumentException if <code>limit &le; 0</code> or
     *                                  if the number of returned matrices {@code >limit}.
     * @see #interleave(ArrayContext, List)
     * @see #asLayers(Matrix, int)
     */
    public static <T extends PArray> List<Matrix<T>> separate(
            ArrayContext context,
            Matrix<? extends T> interleaved,
            int limit) {
        Objects.requireNonNull(interleaved, "Null interleaved matrix");
        if (context == null) {
            context = ArrayContext.DEFAULT_SINGLE_THREAD;
        }
        final long[] dimensions = interleaved.dimensions();
        final long numberOfMatrices = numberOfChannels(dimensions, false, limit);
        final long[] reducedDimensions = java.util.Arrays.copyOfRange(dimensions, 1, dimensions.length);
        final MemoryModel mm = context.getMemoryModel();
        Class<?> elementType = interleaved.elementType();
        final UpdatablePArray[] arrays = new UpdatablePArray[(int) numberOfMatrices];
        final List<Matrix<T>> result = new ArrayList<>();
        if (numberOfMatrices > 0) {
            for (int k = 0; k < numberOfMatrices; k++) {
                final Matrix<UpdatablePArray> m = mm.newMatrix(UpdatablePArray.class, elementType, reducedDimensions);
                arrays[k] = m.array();
                result.add(InternalUtils.cast(m));
            }
            try (var unpacker = InterleavingBandsUnpacker.getInstance(context, arrays, interleaved.array())) {
                unpacker.process();
            }
        }
        return result;
    }


    /**
     * Equivalent to {@link #interleave(ArrayContext, Matrix, List)
     * interleave((ArrayContext) null, result, separated)}.
     *
     * @param result    the result matrix.
     * @param separated list of the source matrices; must be non-empty.
     * @throws NullPointerException     if <code>result</code> argument, the <code>separated</code> list or
     *                                  one of its elements is {@code null}.
     * @throws SizeMismatchException    if some of the source matrices have dimensions, that do not match the last
     *                                  <i>n</i> dimensions
     *                                  <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     *                                  of (<i>n</i>+1)-dimensional <code>result</code> matrix.
     * @throws IllegalArgumentException if <code>separated</code> list is empty, or
     *                                  if number of elements in the <code>separated</code> list is not equal to
     *                                  the first <code>result</code> dimension <i>M</i><sub>0</sub>,
     *                                  or if some of the source matrices have element type that is different from
     *                                  the element type of the <code>result</code> matrix.
     * @throws IllegalStateException    if <code>result</code> matrix is 1-dimensional.
     */
    public static void interleave(
            Matrix<? extends UpdatablePArray> result,
            List<? extends Matrix<? extends PArray>> separated) {
        interleave(null, result, separated);
    }

    /**
     * Analog of <code>{@link #interleave(ArrayContext, List)}</code> method, that does not allocate memory,
     * but stores the results into previously created <code>result</code> matrix.
     *
     * <p>Note: if the source <code>separated</code> matrices have <i>n</i>-dimensions
     * <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     * (remember that they must have identical element types and dimensions),
     * then the <code>result</code> list must be
     * (<i>n</i>+1)-dimensional matrix <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>,
     * where <i>M</i><sub>0</sub>=<code>separated.size()</code>.
     *
     * <p>The <code>separated</code> list must not be empty (<i>M</i><sub>0</sub>&gt;0).</p>
     *
     * @param context   the context;
     *                  can be {@code null}, then {@link ArrayContext#DEFAULT_SINGLE_THREAD} will be used.
     * @param result    the result matrix.
     * @param separated list of the source matrices; must be non-empty.
     * @throws NullPointerException     if <code>result</code> argument, the <code>separated</code> list or
     *                                  one of its elements is {@code null}.
     * @throws SizeMismatchException    if some of the source matrices have dimensions, that do not match the last
     *                                  <i>n</i> dimensions
     *                                  <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     *                                  of (<i>n</i>+1)-dimensional <code>result</code> matrix.
     * @throws IllegalArgumentException if <code>separated</code> list is empty, or
     *                                  if number of elements in the <code>separated</code> list is not equal to
     *                                  the first <code>result</code> dimension <i>M</i><sub>0</sub>,
     *                                  or if some of the source matrices have element type that is different from
     *                                  the element type of the <code>result</code> matrix.
     * @throws IllegalStateException    if <code>result</code> matrix is 1-dimensional.
     */
    public static void interleave(
            ArrayContext context,
            Matrix<? extends UpdatablePArray> result,
            List<? extends Matrix<? extends PArray>> separated) {
        Objects.requireNonNull(result, "Null result argument");
        Objects.requireNonNull(separated, "Null separated argument");
        if (context == null) {
            context = ArrayContext.DEFAULT_SINGLE_THREAD;
        }
        final List<Matrix<? extends PArray>> list = new ArrayList<>(separated);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Empty separated matrix list");
        }
        final PArray[] arrays = arraysOfParallelMatrices(PArray.class, list, true);
        assert arrays.length > 0;
        checkDimensionEqualityWithInterleaving(result, separated);
        try (var packer = InterleavingBandsPacker.getInstance(context, arrays, result.array())) {
            packer.process();
        }
    }

    /**
     * Equivalent to {@link #interleave(ArrayContext, List)
     * interleave((ArrayContext) null, separated)}.
     *
     * @param <T>       the generic type of the built-in AlgART arrays.
     * @param separated list of the source matrices-"channels" (like the red, green, blue channels for 3-channel
     *                  RGB image); must be non-empty.
     * @return result matrix, where "channels" are interleaved along the first dimension
     * (RGBRGB... sequence for 3-channel RGB image).
     * @throws NullPointerException     if <code>separated</code> list or one of its elements is {@code null}.
     * @throws SizeMismatchException    if <code>separated.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     * @throws IllegalArgumentException if <code>separated</code> list is empty, or if
     *                                  <code>separated.size()&gt;1</code> and some of the passed matrices have
     *                                  different element type.
     */
    public static <T extends PArray> Matrix<T> interleave(
            List<? extends Matrix<? extends T>> separated) {
        return interleave((ArrayContext) null, separated);
    }

    /**
     * Merges (interleaves) <i>K</i> <i>n</i>-dimensional matrices with identical element types and dimensions
     * <i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>, passed in the <code>separated</code> list,
     * into a single (<i>n</i>+1)-dimensional matrix
     * <i>K</i>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     * along the first dimension.
     * The element with index (<i>i</i><sub>0</sub>,<i>i</i><sub>1</sub>,...,<i>i<sub>n</sub></i>)
     * of the returned matrix will be equal to the element with index
     * (<i>i</i><sub>1</sub>,...,<i>i<sub>n</sub></i>)
     * of the matrix <code>matrices.get(<i>i</i><sub>0</sub>)</code>.
     *
     * <p>The <code>separated</code> list must not be empty (<i>K</i>&gt;0).</p>
     *
     * <p>For example, if the source <code>separated</code> list contains 3 2-dimensional matrices,
     * describing red, green and blue channels of RGB color image,
     * then the result will be 3-dimensional matrix, where the lowest (first) dimension is 3
     * (for 3 channels) and the pixels are packed into the {@link Matrix#array() underlying array}
     * as a sequence RGBRGB...</p>
     *
     * <p>Note: the matrix, returned by this method, is created by the call
     * <code>memoryModel.{@link MemoryModel#newMatrix(Class, Class, long...) newMatrix}(...)</code>,
     * where <code>memoryModel</code> is <code>context.getMemoryModel()</code> or
     * {@link SimpleMemoryModel} for {@code null} context.
     * In the latter case ({@link SimpleMemoryModel}) this provides a guarantee, that
     * the array in the returned matrix will be direct-accessible wrapper: {@link Array#isJavaArrayWrapper()}
     * will return <code>true</code> for the {@link Matrix#array() built-in array}.</p>
     *
     * @param <T>       the generic type of the built-in AlgART arrays.
     * @param context   the context; allows specifying (in particular)
     *                  the memory model for creating returned matrix;
     *                  can be {@code null}, then {@link ArrayContext#DEFAULT_SINGLE_THREAD} will be used.
     * @param separated list of the source matrices-"channels" (like the red, green, blue channels for 3-channel
     *                  RGB image); must be non-empty.
     * @return result matrix, where "channels" are interleaved along the first dimension
     * (RGBRGB... sequence for 3-channel RGB image).
     * @throws NullPointerException     if <code>separated</code> list or one of its elements is {@code null}.
     * @throws SizeMismatchException    if <code>separated.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     * @throws IllegalArgumentException if <code>separated</code> list is empty, or if
     *                                  <code>separated.size()&gt;1</code> and some of the passed matrices have
     *                                  different element type.
     * @see #separate(ArrayContext, Matrix, int)
     * @see #mergeLayers(MemoryModel, List)
     */
    public static <T extends PArray> Matrix<T> interleave(
            ArrayContext context,
            List<? extends Matrix<? extends T>> separated) {
        Objects.requireNonNull(separated, "Null separated argument");
        if (context == null) {
            context = ArrayContext.DEFAULT_SINGLE_THREAD;
        }
        final List<Matrix<? extends T>> list = new ArrayList<>(separated);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Empty separated matrix list");
        }
        final PArray[] arrays = arraysOfParallelMatrices(PArray.class, list, true);
        final Matrix<?> m0 = list.get(0);
        final long[] dimensions = new long[m0.dimCount() + 1];
        System.arraycopy(m0.dimensions(), 0, dimensions, 1, dimensions.length - 1);
        dimensions[0] = arrays.length;
        final MemoryModel mm = context.getMemoryModel();
        final Matrix<UpdatablePArray> result = mm.newMatrix(UpdatablePArray.class, m0.elementType(), dimensions);
        try (var packer = InterleavingBandsPacker.getInstance(context, arrays, result.array())) {
            packer.process();
        }
        return InternalUtils.cast(result);
    }

    /**
     * Equivalent to <code>{@link #asLayers(Matrix, int)
     * asLayers}(merged, Integer.MAX_VALUE)</code> (no limitations).
     *
     * @param <T>    the generic type of the built-in AlgART arrays.
     * @param merged the source merged matrix.
     * @return a list of matrices: "layers" of the source one along the last dimension.
     * @throws NullPointerException  if <code>merged</code> argument is {@code null}.
     * @throws IllegalStateException if <code>merged</code> matrix is 1-dimensional.
     */
    public static <T extends Array> List<Matrix<T>> asLayers(Matrix<T> merged) {
        return asLayers(merged, Integer.MAX_VALUE);
    }

    /**
     * Splits a single (<i>n</i>+1)-dimensional matrix
     * <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i></sub>
     * to <i>M</i><sub><i>n</i></sub> separate <i>n</i>-dimensional matrices
     * <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i>&minus;1</sub>
     * and returns them as a list.
     * The element with index (<i>i</i><sub>0</sub>,<i>i</i><sub>1</sub>,...,<i>i<sub>n</sub></i>)
     * of the source matrix will be equal to the element with index
     * (<i>i</i><sub>0</sub>,<i>i</i><sub>1</sub>,...,<i>i</i><sub><i>n</i>&minus;1</sub>)
     * of the matrix <code>list.get(<i>i<sub>n</sub></i>)</code> in the returned list.
     *
     * <p>If the last dimension <i>M</i><sub><i>n</i></sub>=0, the returned list will be empty.</p>
     *
     * <p>This method also checks, that the last dimension
     * <i>M</i><sub><i>n</i></sub>=<code>merged.dim(<i>n</i>)</code> (that will be
     * equal to the size of the returned list) is not greater than the passed limit
     * and throws an exception if this limit is exceeded. Typically, this method is used for unpacking
     * matrices where the last dimension cannot be too large &mdash; for example the number
     * of color channels or the number of frames in a movie &mdash; so it makes sense to limit
     * this value, because too large last dimension (millions) usually means incorrect usage of this function.
     * In any case, the number of returned matrices, greater than {@code Integer.MAX_VALUE}, usually leads to
     * <code>OutOfMemoryError</code>.</p>
     *
     * <p>Note that the result of this method is a newly created
     * modifiable <code>List</code> (probable {@link ArrayList}):
     * you can modify it, in particular remove or add new elements.</p>
     *
     * <p>At the same time, the <i>elements</i> of the returned list are the <i>views</i>
     * of the corresponding regions of the source matrix:
     * modification in the source matrix will affect the returned matrices, and vice versa.</p>
     *
     * @param <T>    the generic type of the built-in AlgART arrays.
     * @param merged the source merged matrix.
     * @param limit  maximal allowed number of returned matrices (the last dimension of the source matrix).
     * @return a list of matrices: "layers" of the source one along the last dimension.
     * @throws NullPointerException     if <code>merged</code> argument is {@code null}.
     * @throws IllegalStateException    if <code>merged</code> matrix is 1-dimensional.
     * @throws IllegalArgumentException if <code>limit &le; 0</code> or
     *                                  if the number of returned matrices {@code >limit}.
     * @see #mergeLayers(MemoryModel, List)
     * @see #separate(ArrayContext, Matrix, int)
     */
    public static <T extends Array> List<Matrix<T>> asLayers(Matrix<T> merged, int limit) {
        Objects.requireNonNull(merged, "Null merged matrix");
        final long[] dimensions = merged.dimensions();
        final int numberOfMatrices = numberOfChannels(dimensions, true, limit);
        final long[] reducedDimensions = java.util.Arrays.copyOf(dimensions, dimensions.length - 1);
        final long size = Arrays.longMul(reducedDimensions);
        final T array = merged.array();
        final List<Matrix<T>> result = new ArrayList<>();
        long p = 0;
        for (int k = 0; k < numberOfMatrices; k++, p += size) {
            final T subArray = InternalUtils.cast(array.subArr(p, size));
            result.add(Matrices.matrix(subArray, reducedDimensions));
        }
        return result;
    }

    /**
     * Equivalent to {@link #mergeLayers(MemoryModel, List)
     * mergeLayers(Arrays.SMM, matrices)}.
     *
     * @param <T>      the generic type of the built-in AlgART arrays.
     * @param matrices the list of the source matrices; must be non-empty.
     * @return result merged matrix.
     * @throws NullPointerException     if <code>matrices</code> list or
     *                                  one of its elements is {@code null}.
     * @throws SizeMismatchException    if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     * @throws IllegalArgumentException if <code>matrices</code> list is empty, or if
     *                                  <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different element type.
     */
    public static <T extends Array> Matrix<T> mergeLayers(
            List<? extends Matrix<? extends T>> matrices) {
        return mergeLayers(Arrays.SMM, matrices);

    }

    /**
     * Merges (concatenates) <i>K</i> <i>n</i>-dimensional matrices with identical element types and dimensions
     * <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i>&minus;1</sub>,
     * passed in the <code>matrices</code> list,
     * into a single (<i>n</i>+1)-dimensional matrix
     * <i>M</i><sub>0</sub>x<i>M</i><sub>1</sub>x...x<i>M</i><sub><i>n</i>&minus;1</sub>x<i>K</i>
     * along the last dimension.
     * The element with index (<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n</sub></i>)
     * of the returned matrix will be equal to the element with index
     * (<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i</i><sub><i>n</i>&minus;1</sub>)
     * of the matrix <code>matrices.get(<i>i<sub>n</sub></i>)</code>.
     *
     * <p>The <code>matrices</code> list must not be empty (<i>K</i>&gt;0).</p>
     *
     * <p>For example, if the source list contains 3 2-dimensional matrices, describing red, green and blue channels
     * of RGB color image, then the result will be 3-dimensional matrix, where the highest (new) dimension is 3
     * (for 3 channels) and the pixels are stored sequentially in "planes": RRR... (plane with z-index 0),
     * then GGG... (plane with z-index 1), then BBB... (plane with z-index 2).</p>
     *
     * @param <T>         the generic type of the built-in AlgART arrays.
     * @param memoryModel memory model for creating the result matrix.
     * @param matrices    list of the source matrices; must be non-empty.
     * @return result merged matrix.
     * @throws NullPointerException     if <code>memoryModel</code> argument, the <code>matrices</code> list or
     *                                  one of its elements is {@code null}.
     * @throws SizeMismatchException    if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     * @throws IllegalArgumentException if <code>matrices</code> list is empty, or if
     *                                  <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different element type.
     * @see #asLayers(Matrix, int)
     * @see #interleave(ArrayContext, List)
     */
    public static <T extends Array> Matrix<T> mergeLayers(
            MemoryModel memoryModel,
            List<? extends Matrix<? extends T>> matrices) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(matrices, "Null matrices argument");
        final List<Matrix<?>> list = new ArrayList<>(matrices);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Empty matrices list");
        }
        final Array[] arrays = arraysOfParallelMatrices(Array.class, list, true);
        final Matrix<?> m0 = list.get(0);
        final long[] dimensions = java.util.Arrays.copyOf(m0.dimensions(), m0.dimCount() + 1);
        dimensions[dimensions.length - 1] = arrays.length;
        final long size = m0.size();
        final Matrix<UpdatableArray> result = memoryModel.newMatrix(
                UpdatableArray.class, m0.elementType(), dimensions);
        final UpdatableArray array = result.array();
        long p = 0;
        for (Matrix<?> m : list) {
            array.subArr(p, size).copy(m.array());
            p += size;
        }
        return InternalUtils.cast(result);
    }

    /**
     * Checks the same condition as {@link #checkDimensionEquality(Collection)} and,
     * if <code>requireIdenticalType=true</code>, also checks that all passed matrices have identical
     * {@link Matrix#elementType() element type}.
     *
     * @param matrices list of some matrices.
     * @throws NullPointerException     if the <code>matrices</code> list or one of its elements is {@code null}.
     * @throws SizeMismatchException    if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     * @throws IllegalArgumentException if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                                  different element type.
     * @see #checkDimensionEquality(Matrix[])
     */
    public static void checkDimensionEquality(
            Collection<? extends Matrix<?>> matrices,
            boolean requireIdenticalType) {
        Objects.requireNonNull(matrices, "Null list of matrices");
        Matrix<? extends Array> m0 = null;
        Class<?> elementType = null;
        int k = 0;
        for (Matrix<? extends Array> m : matrices) {
            Objects.requireNonNull(m, "Null matrix #" + k);
            if (m0 == null) {
                m0 = m;
                elementType = m.elementType();
            } else {
                if (requireIdenticalType && m.elementType() != elementType) {
                    throw new IllegalArgumentException("The matrix #" + k +
                            " and the matrix #0 element type mismatch: "
                            + "the matrix #" + k + " has " + m.elementType() +
                            " elements, the matrix #0 has " + elementType + " elements");
                }
                if (!m.dimEquals(m0)) {
                    throw new SizeMismatchException("The matrix #" + k +
                            " and the matrix #0 dimensions mismatch: "
                            + "the matrix #" + k + " is " + m + ", the matrix #0 is " + m0);
                }
            }
            k++;
        }
    }

    /**
     * Checks, whether all passed matrices are not {@code null} and have
     * {@link Matrix#dimEquals(Matrix) equal dimensions} and, it is not so, throws a corresponding exception.
     * The same check is performed by {@link #arraysOfParallelMatrices} method,
     * but this method does not return any result.
     *
     * @param matrices list of some matrices.
     * @throws NullPointerException  if the <code>matrices</code> list or one of its elements is {@code null}.
     * @throws SizeMismatchException if <code>matrices.size()&gt;1</code> and some of the passed matrices have
     *                               different dimensions.
     * @see #checkDimensionEquality(Matrix[])
     */
    public static void checkDimensionEquality(Collection<? extends Matrix<?>> matrices) {
        checkDimensionEquality(matrices, false);
    }

    /**
     * Checks, whether all passed matrices are not {@code null} and have
     * {@link Matrix#dimEquals(Matrix) equal dimensions} and, it is not so, throws a corresponding exception.
     * Equivalent to <code>{@link #checkDimensionEquality(java.util.Collection)
     * checkDimensionEquality}({@link Matrices#several Matrices.several}(matrices))</code>.
     *
     * @param matrices list of some matrices.
     * @throws NullPointerException  if the <code>matrices</code> list or one of its elements is {@code null}.
     * @throws SizeMismatchException if <code>matrices.length&gt;1</code> and some of the passed matrices have
     *                               different dimensions.
     */
    public static void checkDimensionEquality(Matrix<?>... matrices) {
        Objects.requireNonNull(matrices, "Null array of matrices");
        for (int k = 0; k < matrices.length; k++) {
            Objects.requireNonNull(matrices[k], "Null matrix #" + k);
            if (k > 0 && !(matrices[k]).dimEquals(matrices[0])) {
                throw new SizeMismatchException("The matrix #" + k + " and the matrix #0 dimensions mismatch: "
                        + "the matrix #" + k + " is " + matrices[k] + ", the matrix #0 is " + matrices[0]);
            }
        }
    }

    /**
     * Returns a function of the coordinates represented by the given matrix
     * with the given interpolation method.
     * It means: if <i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>
     * (<i>n</i>=<code>matrix.{@link Matrix#dimCount() dimCount()}</code>) are integers
     * inside the dimensions of the given matrix, then the returned function for such arguments returns
     *
     * <p><code>
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>)=matrix.{@link
     * Matrix#array() array()}.{@link PArray#getDouble(long) getDouble}(matrix.{@link Matrix#index(long...)
     * index}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>))
     * </code></p>
     *
     * <p>If the arguments of the returned function are not integer, the returned function uses
     * the specified interpolation algorithm. Namely:
     * <ul>
     * <li>If <code>interpolationMethod</code> is {@link InterpolationMethod#STEP_FUNCTION STEP_FUNCTION},
     * the arguments are just truncated to integer indexes by Java operator <code>(long)<i>x</i></code>.
     * It is the simplest possible interpolation algorithm: the result is a step function.
     * <li>If <code>interpolationMethod</code> is {@link InterpolationMethod#POLYLINEAR_FUNCTION POLYLINEAR_FUNCTION},
     * the function value is calculated as a polylinear interpolation of
     * 2<sup><code>matrix.{@link Matrix#dimCount() dimCount()}</code></sup> neighbour matrix elements
     * (linear interpolation in one-dimensional case, bi-linear in 2-dimensional, etc.)
     * </ul>
     *
     * <p>The real coordinates <i>x</i><sub><i>k</i></sub> must be inside the matrix:
     * <p><code>
     * 0 &lt;= <i>x</i><sub><i>k</i></sub> &lt; matrix.{@link Matrix#dim(int) dim}(<i>k</i>)
     * </code></p>
     * <p>In another case, the behavior of the returned function depends on <code>checkRanges</code> argument.
     * If it is <code>true</code>, <code>IndexOutOfBoundsException</code> is thrown while calling
     * {@link Func#get(double...)} method, as while using {@link Matrix#index(long...)} method.
     * If <code>checkRanges</code> is <code>false</code>,
     * the results will be unspecified: maybe, some runtime exception will be thrown,
     * maybe, {@link Func#get(double...)} method will return an incorrect value.
     * Please use <code>checkRanges=false</code> if you are sure that the returned function will never be used
     * outside the matrix:
     * this mode can little increase the performance of algorithms that use the returned function.
     *
     * <p>For the case {@link InterpolationMethod#POLYLINEAR_FUNCTION POLYLINEAR_FUNCTION}, please note,
     * that if the real coordinates <i>x</i><sub><i>k</i></sub> is near the high boundary,
     * namely if
     * <code>matrix.{@link Matrix#dim(int) dim}(<i>k</i>)&minus;1 &lt;= <i>x</i><sub><i>k</i></sub>
     * &lt; matrix.{@link Matrix#dim(int) dim}(<i>k</i>)</code>,
     * then interpolation will not be used, because necessary next matrix element is outside the matrix.
     * Such real coordinate is processed as if it would be equal to
     * <code>matrix.{@link Matrix#dim(int) dim}(<i>k</i>)&minus;1</code>.
     *
     * <p>The number <i>n</i> of the arguments of {@link Func#get(double...)} method of the returned instance
     * may be not equal to the number of matrix dimensions <code>matrix.{@link Matrix#dimCount() dimCount()}</code>.
     * If <i>n</i> is less than <code>matrix.{@link Matrix#dimCount() dimCount()}</code>,
     * the missing coordinates are supposed to be zero (<code>0.0</code>), i.e. ignored.
     * If <i>n</i> is too big, all extra arguments <i>x</i><sub><i>k</i></sub> are ignored.
     * In any case, the number of argument <i>n</i> must never be zero:
     * the returned function throws <code>IndexOutOfBoundsException</code> when called without arguments.
     *
     * @param matrix              the source AlgART matrix.
     * @param interpolationMethod the algorithm of interpolation for non-integer arguments of the returned function.
     * @param checkRanges         whether the returned function must check all indexes to be inside the matrix.
     * @return the view of the matrix as a mathematical function of
     * <code>matrix.{@link Matrix#dimCount() dimCount()}</code> arguments.
     * @throws NullPointerException if <code>matrix</code> or <code>interpolationMethod</code> argument
     *                              is {@code null}.
     * @see #asInterpolationFunc(Matrix, InterpolationMethod, double)
     * @see #isInterpolationFunc(Func)
     * @see #asResized(net.algart.arrays.Matrices.ResizingMethod, Matrix, long...)
     */
    public static Func asInterpolationFunc(
            Matrix<? extends PArray> matrix,
            InterpolationMethod interpolationMethod, boolean checkRanges) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        Objects.requireNonNull(interpolationMethod, "Null interpolationMethod argument");
        PArray a = matrix.array();
        switch (interpolationMethod) {
            case STEP_FUNCTION: {
                if (checkRanges) {
                    if (Arrays.javaArrayInternal(a) != null && a.length() < Integer.MAX_VALUE) {
                        // length check is necessary to be sure that
                        // (int)x > dimX if x > dimX, (int)y > dimY if y > dimY, ...
                        //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                        //           if(?=\s+\(a\s+instanceof\s+) ==> } else if,,...]]
                        if (a instanceof ByteArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialByteInterpolation(matrix);
                            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        } else if (a instanceof CharArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialCharInterpolation(matrix);
                        } else if (a instanceof ShortArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialShortInterpolation(matrix);
                        } else if (a instanceof IntArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialIntInterpolation(matrix);
                        } else if (a instanceof LongArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialLongInterpolation(matrix);
                        } else if (a instanceof FloatArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialFloatInterpolation(matrix);
                        } else if (a instanceof DoubleArray) {
                            return new ArraysInterpolationsImpl.CheckedTrivialDoubleInterpolation(matrix);
                            //[[Repeat.AutoGeneratedEnd]]
                        } else {
                            return new ArraysInterpolationsImpl.CheckedTrivialInterpolation(matrix);
                        }
                    } else {
                        return new ArraysInterpolationsImpl.CheckedTrivialInterpolation(matrix);
                    }
                } else {
                    if (Arrays.javaArrayInternal(a) != null && a.length() < Integer.MAX_VALUE) {
                        //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                        //           if(?=\s+\(a\s+instanceof\s+) ==> } else if,,...]]
                        if (a instanceof ByteArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialByteInterpolation(matrix);
                            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        } else if (a instanceof CharArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialCharInterpolation(matrix);
                        } else if (a instanceof ShortArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialShortInterpolation(matrix);
                        } else if (a instanceof IntArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialIntInterpolation(matrix);
                        } else if (a instanceof LongArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialLongInterpolation(matrix);
                        } else if (a instanceof FloatArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialFloatInterpolation(matrix);
                        } else if (a instanceof DoubleArray) {
                            return new ArraysInterpolationsImpl.UncheckedTrivialDoubleInterpolation(matrix);
                            //[[Repeat.AutoGeneratedEnd]]
                        } else {
                            return new ArraysInterpolationsImpl.UncheckedTrivialInterpolation(matrix);
                        }
                    } else {
                        return new ArraysInterpolationsImpl.UncheckedTrivialInterpolation(matrix);
                    }
                }
            }
            case POLYLINEAR_FUNCTION: {
                if (checkRanges) {
                    if (Arrays.javaArrayInternal(a) != null && a.length() < Integer.MAX_VALUE) {
                        // length check is necessary to be sure that
                        // (int)x > dimX if x > dimX, (int)y > dimY if y > dimY, ...
                        //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                        //           if(?=\s+\(a\s+instanceof\s+) ==> } else if,,...]]
                        if (a instanceof ByteArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearByteInterpolation(
                                    matrix);
                            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        } else if (a instanceof CharArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearCharInterpolation(
                                    matrix);
                        } else if (a instanceof ShortArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearShortInterpolation(
                                    matrix);
                        } else if (a instanceof IntArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearIntInterpolation(
                                    matrix);
                        } else if (a instanceof LongArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearLongInterpolation(
                                    matrix);
                        } else if (a instanceof FloatArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearFloatInterpolation(
                                    matrix);
                        } else if (a instanceof DoubleArray) {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearDoubleInterpolation(
                                    matrix);
                            //[[Repeat.AutoGeneratedEnd]]
                        } else {
                            return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearInterpolation(matrix);
                        }
                    } else {
                        return new ArraysPolylinearInterpolationsImpl.CheckedPolylinearInterpolation(matrix);
                    }
                } else {
                    if (Arrays.javaArrayInternal(a) != null && a.length() < Integer.MAX_VALUE) {
                        //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                        //           if(?=\s+\(a\s+instanceof\s+) ==> } else if,,...]]
                        if (a instanceof ByteArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearByteInterpolation(
                                    matrix);
                            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        } else if (a instanceof CharArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearCharInterpolation(
                                    matrix);
                        } else if (a instanceof ShortArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearShortInterpolation(
                                    matrix);
                        } else if (a instanceof IntArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearIntInterpolation(
                                    matrix);
                        } else if (a instanceof LongArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearLongInterpolation(
                                    matrix);
                        } else if (a instanceof FloatArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearFloatInterpolation(
                                    matrix);
                        } else if (a instanceof DoubleArray) {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearDoubleInterpolation(
                                    matrix);
                            //[[Repeat.AutoGeneratedEnd]]
                        } else {
                            return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearInterpolation(matrix);
                        }
                    } else {
                        return new ArraysPolylinearInterpolationsImpl.UncheckedPolylinearInterpolation(matrix);
                    }
                }
            }
        }
        throw new InternalError("Impossible switch case");
    }

    /**
     * An analog of <code>{@link #asInterpolationFunc(Matrix, InterpolationMethod, boolean)}</code> method,
     * that use constant continuation for all coordinates outside the matrix.
     * The returned function works almost like the result of
     * <code>{@link #asInterpolationFunc(Matrix, InterpolationMethod, boolean)
     * asInterpolationFunc(matrix, interpolationMethod, true)}</code>,
     * but if the integer indexes are out of the required ranges,
     * {@link Func#get(double...)} method returns the <code>outsideValue</code> instead throwing
     * <code>IndexOutOfBoundsException</code>.
     *
     * @param matrix              the source AlgART matrix.
     * @param interpolationMethod the algorithm of interpolation for non-integer arguments of the returned function.
     * @param outsideValue        the value returned by {@link Func#get(double...)} method outside the matrix.
     * @return the view of the matrix as a mathematical function of
     * <code>matrix.{@link Matrix#dimCount() dimCount()}</code> arguments.
     * @throws NullPointerException if <code>matrix</code> or <code>interpolationMethod</code> argument
     *                              is {@code null}.
     * @see #isInterpolationFunc(Func)
     */
    public static Func asInterpolationFunc(
            Matrix<? extends PArray> matrix,
            InterpolationMethod interpolationMethod, double outsideValue) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        Objects.requireNonNull(interpolationMethod, "Null interpolationMethod argument");
        PArray a = matrix.array();
        switch (interpolationMethod) {
            case STEP_FUNCTION: {
                if (Arrays.javaArrayInternal(a) != null && a.length() < Integer.MAX_VALUE) {
                    // length check is necessary to be sure that
                    // (int)x > dimX if x > dimX, (int)y > dimY if y > dimY, ...
                    //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                    //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                    //           if(?=\s+\(a\s+instanceof\s+) ==> } else if,,...]]
                    if (a instanceof ByteArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialByteInterpolation(matrix, outsideValue);
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    } else if (a instanceof CharArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialCharInterpolation(matrix, outsideValue);
                    } else if (a instanceof ShortArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialShortInterpolation(matrix, outsideValue);
                    } else if (a instanceof IntArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialIntInterpolation(matrix, outsideValue);
                    } else if (a instanceof LongArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialLongInterpolation(matrix, outsideValue);
                    } else if (a instanceof FloatArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialFloatInterpolation(matrix, outsideValue);
                    } else if (a instanceof DoubleArray) {
                        return new ArraysInterpolationsImpl.ContinuedTrivialDoubleInterpolation(matrix, outsideValue);
                        //[[Repeat.AutoGeneratedEnd]]
                    } else {
                        return new ArraysInterpolationsImpl.ContinuedTrivialInterpolation(matrix, outsideValue);
                    }
                } else {
                    return new ArraysInterpolationsImpl.ContinuedTrivialInterpolation(matrix, outsideValue);
                }
            }
            case POLYLINEAR_FUNCTION: {
                if (Arrays.javaArrayInternal(a) != null && a.length() < Integer.MAX_VALUE) {
                    // length check is necessary to be sure that
                    // (int)x > dimX if x > dimX, (int)y > dimY if y > dimY, ...
                    //[[Repeat() byte ==> char,,short,,int,,long,,float,,double;;
                    //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
                    //           if(?=\s+\(a\s+instanceof\s+) ==> } else if,,...]]
                    if (a instanceof ByteArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearByteInterpolation(
                                matrix, outsideValue);
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    } else if (a instanceof CharArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearCharInterpolation(
                                matrix, outsideValue);
                    } else if (a instanceof ShortArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearShortInterpolation(
                                matrix, outsideValue);
                    } else if (a instanceof IntArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearIntInterpolation(
                                matrix, outsideValue);
                    } else if (a instanceof LongArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearLongInterpolation(
                                matrix, outsideValue);
                    } else if (a instanceof FloatArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearFloatInterpolation(
                                matrix, outsideValue);
                    } else if (a instanceof DoubleArray) {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearDoubleInterpolation(
                                matrix, outsideValue);
                        //[[Repeat.AutoGeneratedEnd]]
                    } else {
                        return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearInterpolation(
                                matrix, outsideValue);
                    }
                } else {
                    return new ArraysPolylinearInterpolationsImpl.ContinuedPolylinearInterpolation(
                            matrix, outsideValue);
                }
            }
        }
        throw new InternalError("Impossible switch case");
    }


    /**
     * Returns <code>true</code> if the passed function is not {@code null} interpolation view
     * of an AlgART matrix, created by this package.
     * More precisely, if returns <code>true</code>
     * if and only if the function is a result of one of the following methods:
     * <ul>
     * <li>{@link #asInterpolationFunc(Matrix, InterpolationMethod, boolean)},
     * <li>{@link #asInterpolationFunc(Matrix, InterpolationMethod, double)}.
     * </ul>
     * You may get the underlying matrix, passed to those methods in the first argument,
     * by <code>{@link #getUnderlyingMatrix(Func)}</code>.
     *
     * @param f some mathematical function (can be {@code null}, than the method returns <code>false</code>).
     * @return whether this function is an interpolation view of some AlgART matrix.
     * @see #getUnderlyingMatrix(Func)
     * @see #isOnlyInsideInterpolationFunc(Func)
     * @see #isCheckedOnlyInsideInterpolationFunc(Func)
     * @see #isContinuedInterpolationFunc(Func)
     */
    public static boolean isInterpolationFunc(Func f) {
        return f instanceof ArraysInterpolationsImpl.AbstractInterpolation;
    }

    /**
     * Returns <code>true</code> if the passed function is not {@code null} interpolation view
     * of an AlgART matrix, created by {@link #asInterpolationFunc(Matrix, InterpolationMethod, boolean)} method.
     * You may get the underlying matrix, passed to those methods in the first argument,
     * by <code>{@link #getUnderlyingMatrix(Func)}</code>.
     *
     * @param f some mathematical function (can be {@code null}, than the method returns <code>false</code>).
     * @return whether this function is an interpolation view of some AlgART matrix without outside continuation.
     * @see #isInterpolationFunc(Func)
     * @see #getUnderlyingMatrix(Func)
     */
    public static boolean isOnlyInsideInterpolationFunc(Func f) {
        return f instanceof ArraysInterpolationsImpl.AbstractInterpolation
                && ((ArraysInterpolationsImpl.AbstractInterpolation) f).isChecked() != null;
    }

    /**
     * Returns <code>true</code> if the passed function is not {@code null} interpolation view of
     * an AlgART matrix,
     * created by {@link #asInterpolationFunc(Matrix, InterpolationMethod, boolean)} method with <code>true</code>
     * last argument.
     * You may get the underlying matrix, passed to those methods in the first argument,
     * by <code>{@link #getUnderlyingMatrix(Func)}</code>.
     *
     * @param f some mathematical function (can be {@code null}, than the method returns <code>false</code>).
     * @return whether this function is an interpolation view of some AlgART matrix without outside continuation
     * with checking indexes.
     * @see #isInterpolationFunc(Func)
     * @see #getUnderlyingMatrix(Func)
     */
    public static boolean isCheckedOnlyInsideInterpolationFunc(Func f) {
        Boolean v;
        return f instanceof ArraysInterpolationsImpl.AbstractInterpolation
                && (v = ((ArraysInterpolationsImpl.AbstractInterpolation) f).isChecked()) != null
                && v;
    }

    /**
     * Returns <code>true</code> if the passed function is not {@code null} interpolation view
     * of an AlgART matrix,
     * created by {@link #asInterpolationFunc(Matrix, InterpolationMethod, double)} method.
     * You may get the underlying matrix, passed to those methods in the first argument,
     * by <code>{@link #getUnderlyingMatrix(Func)}</code>.
     *
     * @param f some mathematical function (can be {@code null}, than the method returns <code>false</code>).
     * @return whether this function is an interpolation view of some AlgART matrix with outside continuation.
     * @see #isInterpolationFunc(Func)
     * @see #getUnderlyingMatrix(Func)
     */
    public static boolean isContinuedInterpolationFunc(Func f) {
        return f instanceof ArraysInterpolationsImpl.AbstractInterpolation
                && ((ArraysInterpolationsImpl.AbstractInterpolation) f).outsideValue() != null;
    }

    /**
     * If the passed function {@link #isInterpolationFunc(Func) is an interpolation view of an AlgART matrix},
     * returns the reference to this matrix.
     *
     * @param f some interpolation view of an AlgART matrix.
     * @return the reference to the AlgART matrix, represented by the passed view.
     * @throws NullPointerException     if <code>f</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>!{@link #isInterpolationFunc(Func) isInterpolationFunc}(f)</code>
     * @see #isInterpolationFunc(Func)
     * @see #getInterpolationMethod(Func)
     */
    public static Matrix<? extends PArray> getUnderlyingMatrix(Func f) {
        Objects.requireNonNull(f, "Null f argument");
        if (!isInterpolationFunc(f)) {
            throw new IllegalArgumentException("The passed function is not a view of a matrix");
        }
        return ((ArraysInterpolationsImpl.AbstractInterpolation) f).m;
    }

    /**
     * If the passed function {@link #isContinuedInterpolationFunc(Func) is a continued interpolation
     * view of an AlgART matrix}, return the value used outside the matrix.
     * In other words, this method returns the last argument of
     * {@link #asInterpolationFunc(Matrix, InterpolationMethod, double)} method,
     * used for creating the passed view.
     *
     * @param f some continued interpolation view of an AlgART matrix.
     * @return the value used outside the matrix.
     * @throws NullPointerException     if <code>f</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>!{@link #isContinuedInterpolationFunc(Func)
     *                                  isContinuedInterpolationFunc}(f)</code>
     * @see #isInterpolationFunc(Func)
     * @see #getInterpolationMethod(Func)
     */
    public static double getOutsideValue(Func f) {
        Objects.requireNonNull(f, "Null f argument");
        if (!isInterpolationFunc(f)) {
            throw new IllegalArgumentException("The passed function is not a view of a matrix");
        }
        if (!isContinuedInterpolationFunc(f)) {
            throw new IllegalArgumentException("The passed interpolation function isn't continued outside the matrix");
        }
        return ((ArraysInterpolationsImpl.AbstractInterpolation) f).outsideValue();
    }

    /**
     * If the passed function {@link #isInterpolationFunc(Func) is an interpolation view of some AlgART matrix},
     * returns the interpolation algorithm, used while creating this function.
     * The result is equal to the second argument of methods
     * {@link #asInterpolationFunc(Matrix, InterpolationMethod, boolean)},
     * {@link #asInterpolationFunc(Matrix, InterpolationMethod, double)},
     * used for creating the passed function.
     *
     * @param f some interpolation view of an AlgART matrix.
     * @return the interpolation algorithm, used by this function.
     * @throws NullPointerException     if <code>f</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>!{@link #isInterpolationFunc(Func)
     *                                  isInterpolationFunc}(f)</code>
     * @see #isInterpolationFunc(Func)
     */
    public static InterpolationMethod getInterpolationMethod(Func f) {
        Objects.requireNonNull(f, "Null f argument");
        if (!isInterpolationFunc(f)) {
            throw new IllegalArgumentException("The passed function is not a view of a matrix");
        }
        return ((ArraysInterpolationsImpl.AbstractInterpolation) f).getInterpolationMethod();
    }

    /**
     * Returns a constant matrix, filled by the specified constant.
     * Equivalent to <code>{@link #asCoordFuncMatrix(boolean, Func, Class, long...)
     * asCoordFuncMatrix}(true, {@link ConstantFunc#getInstance(double)
     * ConstantFunc.getInstance}(constant), requiredType, dim)</code>.
     *
     * @param <T>          the generic type of AlgART array.
     * @param constant     the constant value of all elements of the returned matrix.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param dim          dimensions of the returned matrix.
     * @return the matrix, defined by the passed function.
     * @throws NullPointerException     if <code>requiredType</code> argument is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asCoordFuncMatrix(boolean, Func, Class, long...)} method.
     */
    public static <T extends PArray> Matrix<T> constantMatrix(
            double constant, Class<? extends T> requiredType, long... dim) {
        return asCoordFuncMatrix(true, ConstantFunc.getInstance(constant), requiredType, dim);
    }

    /**
     * Equivalent to {@link #asCoordFuncMatrix(boolean, Func, Class, long...)
     * asCoordFuncMatrix(true, f, requiredType, dim)}.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function used for calculating all result matrix elements.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param dim          dimensions of the returned matrix.
     * @return the matrix, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asCoordFuncMatrix(boolean, Func, Class, long...)} method.
     */
    public static <T extends PArray> Matrix<T> asCoordFuncMatrix(
            Func f, Class<? extends T> requiredType, long... dim) {
        return asCoordFuncMatrix(true, f, requiredType, dim);
    }

    /**
     * An analog of the {@link #asFuncMatrix(boolean, Func, Class, List)} method,
     * where the passed function is applied <i>not</i> to the elements of some source matrices,
     * but to the indexes of the resulting matrix.
     * More precisely, if <code>result</code> is the matrix returned by this method,
     * then each its element
     * <pre>
     * result.{@link Matrix#array() array()}.{@link PArray#getDouble(long)
     * getDouble}(result.{@link Matrix#index(long...)
     * index}(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>))
     * </pre>
     * <p>is a result of the call
     * <code>f.{@link Func#get(double...)
     * get}(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>)</code>.
     * So, this method does not require any source matrices.
     *
     * <p>Matrices, created by this method, are called <i>functional</i> matrices.
     *
     * <p>Please read comments to {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * about precise details of forming the elements of the returned matrix on the base
     * of the real result of calling {@link Func#get(double...)} method.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function used for calculating all result matrix elements.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param dim               dimensions of the returned matrix.
     * @return the matrix, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}.
     * @throws IllegalArgumentException if the number of dimensions is 0 (empty <code>dim</code> Java array),
     *                                  or if some of dimensions are negative,
     *                                  and also in the same situations as
     *                                  {@link Arrays#asIndexFuncArray(boolean, Func, Class, long)}
     *                                  method.
     * @see #asResized(net.algart.arrays.Matrices.ResizingMethod, Matrix, long...)
     * @see Arrays#isFuncArray(Array)
     * @see Arrays#getFunc(Array)
     */
    public static <T extends PArray> Matrix<T> asCoordFuncMatrix(
            boolean truncateOverflows,
            Func f, Class<? extends T> requiredType, long... dim) {
        dim = dim.clone(); // to be on the safe side
        T array = ArraysFuncImpl.asCoordFuncMatrix(truncateOverflows, f, requiredType, dim);
        return matrix(array, dim);
    }

    /**
     * Returns <code>true</code> if the passed matrix is not {@code null} <i>functional</i> matrix,
     * created by this package, calculated on the base of coordinates only, not depending on another arrays/matrices.
     * This method is equivalent to the following operator:
     * <pre>
     * <code>matrix != null &amp;&amp; {@link Arrays#isIndexFuncArray
     * Arrays.isIndexFuncArray}(matrix.{@link Matrix#array() array()})</code>
     * </pre>
     *
     * @param matrix the checked AlgART matrix (can be {@code null}, then the method returns <code>false</code>).
     * @return <code>true</code> if the passed matrix a functional one, calculated on the base of coordinates only.
     */
    public static boolean isCoordFuncMatrix(Matrix<? extends PArray> matrix) {
        return matrix != null && Arrays.isIndexFuncArray(matrix.array());
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(Func, Class, List)
     * asFuncMatrix}(f, requiredType, {@link #several(Class, Matrix[]) several}(PArray.class, x))</code>.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to the passed AlgART matrix.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param x            the AlgART matrix.
     * @return a view of the passed matrix, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  the passed matrix is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x) {
        return asFuncMatrix(f, requiredType, several(PArray.class, x));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(boolean, Func, Class, List)
     * asFuncMatrix}(truncateOverflows, f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x))</code>.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to the passed AlgART matrix.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param x                 the AlgART matrix.
     * @return a view of the passed matrix, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  the passed matrix is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            boolean truncateOverflows, Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x) {
        return asFuncMatrix(truncateOverflows, f, requiredType, several(PArray.class, x));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(Func, Class, List)
     * asFuncMatrix}(f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2))</code>.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to all passed AlgART matrices.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param x1           1st AlgART matrix.
     * @param x2           2nd AlgART matrix.
     * @return a view of the passed matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x1, Matrix<? extends PArray> x2) {
        return asFuncMatrix(f, requiredType, several(PArray.class, x1, x2));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(boolean, Func, Class, List)
     * asFuncMatrix}(truncateOverflows, f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2))</code>.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to all passed AlgART matrices.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param x1                1st AlgART matrix.
     * @param x2                2nd AlgART matrix.
     * @return a view of the passed matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            boolean truncateOverflows, Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x1, Matrix<? extends PArray> x2) {
        return asFuncMatrix(truncateOverflows, f, requiredType, several(PArray.class, x1, x2));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(Func, Class, List)
     * asFuncMatrix}(f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2, x3))</code>.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to all passed AlgART matrices.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param x1           1st AlgART matrix.
     * @param x2           2nd AlgART matrix.
     * @param x3           3rd AlgART matrix.
     * @return a view of the passed matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x1, Matrix<? extends PArray> x2, Matrix<? extends PArray> x3) {
        return asFuncMatrix(f, requiredType, several(PArray.class, x1, x2, x3));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(boolean, Func, Class, List)
     * asFuncMatrix}(truncateOverflows, f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2, x3))</code>.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to all passed AlgART matrices.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param x1                1st AlgART matrix.
     * @param x2                2nd AlgART matrix.
     * @param x3                3rd AlgART matrix.
     * @return a view of the passed matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            boolean truncateOverflows, Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x1, Matrix<? extends PArray> x2, Matrix<? extends PArray> x3) {
        return asFuncMatrix(truncateOverflows, f, requiredType, several(PArray.class, x1, x2, x3));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(Func, Class, List)
     * asFuncMatrix}(f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2, x3, x4))</code>.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to all passed AlgART matrices.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param x1           1st AlgART matrix.
     * @param x2           2nd AlgART matrix.
     * @param x3           3rd AlgART matrix.
     * @param x4           4th AlgART matrix.
     * @return a view of the passed matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x1, Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3, Matrix<? extends PArray> x4) {
        return asFuncMatrix(f, requiredType, several(PArray.class, x1, x2, x3, x4));
    }

    /**
     * Equivalent to <code>{@link #asFuncMatrix(boolean, Func, Class, List)
     * asFuncMatrix}(truncateOverflows, f, requiredType, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2, x3, x4))</code>.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to all passed AlgART matrices.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param x1                1st AlgART matrix.
     * @param x2                2nd AlgART matrix.
     * @param x3                3rd AlgART matrix.
     * @param x4                4th AlgART matrix.
     * @return a view of the passed matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or
     *                                  one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)} method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            boolean truncateOverflows, Func f, Class<? extends T> requiredType,
            Matrix<? extends PArray> x1, Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3, Matrix<? extends PArray> x4) {
        return asFuncMatrix(truncateOverflows, f, requiredType, several(PArray.class, x1, x2, x3, x4));
    }

    /**
     * Equivalent to {@link #asFuncMatrix(boolean, Func, Class, List)
     * asFuncMatrix(true, f, requiredType, x)}.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to all passed AlgART matrices.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param x            several AlgART matrices; must not be empty.
     * @return a view of the passed <code>x</code> matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or <code>x</code> argument
     *                                  is {@code null} or if one of <code>x</code> elements is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncMatrix(boolean, Func, Class, List)}.
     * @throws SizeMismatchException    if <code>x.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            Func f,
            Class<? extends T> requiredType,
            List<? extends Matrix<? extends PArray>> x) {
        return asFuncMatrix(true, f, requiredType, x);
    }

    /**
     * An analog of {@link
     * Arrays#asFuncArray(boolean, Func, Class, PArray...)} method
     * for AlgART matrices. More precisely, this method is equivalent to
     * <code>m0.{@link Matrix#matrix(Array)
     * matrix}({@link Arrays#asFuncArray(boolean, Func, Class, PArray...)
     * Arrays.asFuncArray(truncateOverflows, f, requiredType, arrays)})</code>,
     * where <code>m0</code> is <code>x.get(0)</code> and <code>arrays</code> is
     * <code>{x.get(0).{@link Matrix#array() array}(), x.get(1).{@link Matrix#array() array}(), ...}</code>.
     *
     * <p>In addition, this method checks, whether all passed matrices have the
     * {@link Matrix#dimEquals(Matrix) same dimensions}, and throws an exception in another case.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to all passed AlgART matrices.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param x                 several AlgART matrices; must not be empty.
     * @return a view of the passed <code>x</code> matrices, defined by the passed function.
     * @throws NullPointerException     if <code>f</code>, <code>requiredType</code> or <code>x</code> argument
     *                                  is {@code null} or if one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException if <code>x.isEmpty()</code> (no matrices passed),
     *                                  and also in the same situations as
     *                                  {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if <code>x.size()&gt;1</code> and some of the passed matrices have
     *                                  different dimensions.
     */
    public static <T extends PArray> Matrix<T> asFuncMatrix(
            boolean truncateOverflows,
            Func f,
            Class<? extends T> requiredType,
            List<? extends Matrix<? extends PArray>> x) {
        PArray[] arrays = arraysOfParallelMatrices(PArray.class, x);
        if (arrays.length == 0) // check this, not x.size(), which can be changed in a parallel thread
        {
            throw new IllegalArgumentException("Empty x (list of AlgART matrices)");
        }
        return x.get(0).matrix(Arrays.asFuncArray(truncateOverflows, f, requiredType, arrays));
    }

    /**
     * Equivalent to {@link #asUpdatableFuncMatrix(boolean, net.algart.math.functions.Func.Updatable, Class, Matrix)
     * asUpdatableFuncMatrix(true, f, requiredType, x)}.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to the passed AlgART matrix.
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param x            the source matrix.
     * @return an updatable view of the passed <code>x</code> matrix, defined by the passed function.
     * @throws NullPointerException     if <code>requiredType</code> or <code>x</code> argument is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#asUpdatableFuncArray(boolean,
     *                                  net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)} method.
     */
    public static <T extends UpdatablePArray> Matrix<T> asUpdatableFuncMatrix(
            Func.Updatable f,
            Class<? extends T> requiredType,
            Matrix<? extends UpdatablePArray> x) {
        return asUpdatableFuncMatrix(true, f, requiredType, x);
    }

    /**
     * An analog of {@link
     * Arrays#asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)
     * Arrays.asUpdatableFuncArray}
     * method for AlgART matrices. More precisely, this method is equivalent to
     * <code>x.{@link Matrix#matrix(Array)
     * matrix}({@link
     * Arrays#asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)
     * Arrays.asUpdatableFuncArray(truncateOverflows, f, requiredType, x.array())})</code>.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to {@link
     *                          Arrays#asUpdatableFuncArray(boolean,
     *                          net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)}
     *                          method).
     * @param f                 the mathematical function applied to the passed AlgART matrix.
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param x                 the original AlgART matrix.
     * @return an updatable view of the passed <code>x</code> matrix, defined by the passed function.
     * @throws NullPointerException     if <code>requiredType</code> or <code>x</code> argument is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#asUpdatableFuncArray(boolean,
     *                                  net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)}
     *                                  method.
     */
    public static <T extends UpdatablePArray> Matrix<T> asUpdatableFuncMatrix(
            boolean truncateOverflows,
            Func.Updatable f,
            Class<? extends T> requiredType,
            Matrix<? extends UpdatablePArray> x) {
        return x.matrix(Arrays.asUpdatableFuncArray(truncateOverflows, f, requiredType, x.array()));
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, Func, Matrix, Matrix)
     * applyFunc(null, f, result, x)}.
     *
     * @param f      the mathematical function applied to the source AlgART matrices.
     * @param result the destination matrix.
     * @param x      the source matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x) {
        applyFunc(null, f, result, x);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, Func, Matrix, Matrix)
     * applyFunc(context, true, f, result, x)}.
     *
     * @param context the context of copying; can be {@code null}.
     * @param f       the mathematical function applied to the source AlgART matrices.
     * @param result  the destination matrix.
     * @param x       the source matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x) {
        applyFunc(context, true, f, result, x);
    }

    /**
     * Equivalent to <code>{@link #applyFunc(ArrayContext, boolean, Func, Matrix, List)
     * applyFunc}(context, truncateOverflows, f, result, {@link #several(Class, Matrix[])
     * several}(PArray.class, x))</code>.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to the source AlgART matrices.
     * @param result            the destination matrix.
     * @param x                 the AlgART matrix.
     * @throws NullPointerException     if <code>f</code> or one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x) {
        applyFunc(context, truncateOverflows, f, result, several(PArray.class, x));
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc(null, f, result, x1, x2)}.
     *
     * @param f      the mathematical function applied to the source AlgART matrices.
     * @param result the destination matrix.
     * @param x1     1st AlgART matrix.
     * @param x2     2nd AlgART matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2) {
        applyFunc(null, f, result, x1, x2);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, Func, Matrix, Matrix, Matrix)
     * applyFunc(context, true, f, result, x1, x2)}.
     *
     * @param context the context of copying; can be {@code null}.
     * @param f       the mathematical function applied to the source AlgART matrices.
     * @param result  the destination matrix.
     * @param x1      1st AlgART matrix.
     * @param x2      2nd AlgART matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2) {
        applyFunc(context, true, f, result, x1, x2);
    }

    /**
     * Equivalent to <code>{@link #applyFunc(ArrayContext, boolean, Func, Matrix, List)
     * applyFunc}(context, truncateOverflows, f, result, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2))</code>.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to the source AlgART matrices.
     * @param result            the destination matrix.
     * @param x1                1st AlgART matrix.
     * @param x2                2nd AlgART matrix.
     * @throws NullPointerException     if <code>f</code> or one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2) {
        applyFunc(context, truncateOverflows, f, result, several(PArray.class, x1, x2));
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix, Matrix)
     * applyFunc(null, f, result, x1, x2, x3)}.
     *
     * @param f      the mathematical function applied to the source AlgART matrices.
     * @param result the destination matrix.
     * @param x1     1st AlgART matrix.
     * @param x2     2nd AlgART matrix.
     * @param x3     3rd AlgART matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3) {
        applyFunc(null, f, result, x1, x2, x3);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, Func, Matrix, Matrix, Matrix, Matrix)
     * applyFunc(context, true, f, result, x1, x2, x3)}.
     *
     * @param context the context of copying; can be {@code null}.
     * @param f       the mathematical function applied to the source AlgART matrices.
     * @param result  the destination matrix.
     * @param x1      1st AlgART matrix.
     * @param x2      2nd AlgART matrix.
     * @param x3      3rd AlgART matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context, Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3) {
        applyFunc(context, true, f, result, x1, x2, x3);
    }

    /**
     * Equivalent to <code>{@link #applyFunc(ArrayContext, boolean, Func, Matrix, List)
     * applyFunc}(context, truncateOverflows, f, result, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2, x3))</code>.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to the source AlgART matrices.
     * @param result            the destination matrix.
     * @param x1                1st AlgART matrix.
     * @param x2                2nd AlgART matrix.
     * @param x3                3rd AlgART matrix.
     * @throws NullPointerException     if <code>f</code> or one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3) {
        applyFunc(context, truncateOverflows, f, result, several(PArray.class, x1, x2, x3));
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix, Matrix)
     * applyFunc(null, f, result, x1, x2, x3, x4)}.
     *
     * @param f      the mathematical function applied to the source AlgART matrices.
     * @param result the destination matrix.
     * @param x1     1st AlgART matrix.
     * @param x2     2nd AlgART matrix.
     * @param x3     3rd AlgART matrix.
     * @param x4     4th AlgART matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3,
            Matrix<? extends PArray> x4) {
        applyFunc(null, f, result, x1, x2, x3, x4);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, Func, Matrix, Matrix, Matrix, Matrix)
     * applyFunc(context, true, f, result, x1, x2, x3, x4)}.
     *
     * @param context the context of copying; can be {@code null}.
     * @param f       the mathematical function applied to the source AlgART matrices.
     * @param result  the destination matrix.
     * @param x1      1st AlgART matrix.
     * @param x2      2nd AlgART matrix.
     * @param x3      3rd AlgART matrix.
     * @param x4      4th AlgART matrix.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3,
            Matrix<? extends PArray> x4) {
        applyFunc(context, true, f, result, x1, x2, x3, x4);
    }

    /**
     * Equivalent to <code>{@link #applyFunc(ArrayContext, boolean, Func, Matrix, List)
     * applyFunc}(context, truncateOverflows, f, result, {@link #several(Class, Matrix[])
     * several}(PArray.class, x1, x2, x3, x4))</code>.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to the source AlgART matrices.
     * @param result            the destination matrix.
     * @param x1                1st AlgART matrix.
     * @param x2                2nd AlgART matrix.
     * @param x3                3rd AlgART matrix.
     * @param x4                4th AlgART matrix.
     * @throws NullPointerException     if <code>f</code> or one of passed matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> x3,
            Matrix<? extends PArray> x4) {
        applyFunc(context, truncateOverflows, f, result, several(PArray.class, x1, x2, x3, x4));
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, Func, Matrix, List)
     * applyFunc(null, f, result, x)}.
     *
     * @param f      the mathematical function applied to the source AlgART matrices.
     * @param result the destination matrix.
     * @param x      several AlgART matrices; may be empty.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            Func f,
            Matrix<? extends UpdatablePArray> result,
            List<? extends Matrix<? extends PArray>> x) {
        applyFunc(null, f, result, x);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, Func, Matrix, List)
     * applyFunc(context, true, f, result, x)}.
     *
     * @param context the context of copying; can be {@code null}.
     * @param f       the mathematical function applied to the source AlgART matrices.
     * @param result  the destination matrix.
     * @param x       several AlgART matrices; may be empty.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            List<? extends Matrix<? extends PArray>> x) {
        applyFunc(context, true, f, result, x);
    }

    /**
     * Calls to <code>{@link Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)
     * Arrays.applyFunc}(context, truncateOverflows, f, result.array(), arrays)</code>,
     * where <code>arrays</code> is
     * <code>{x.get(0).{@link Matrix#array() array}(), x.get(1).{@link Matrix#array() array}(), ...}</code>.
     *
     * <p>In addition, this method checks, whether all passed matrices have the
     * {@link Matrix#dimEquals(Matrix) same dimensions} as <code>result</code> one,
     * and throws an exception in another case.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>, <code>short</code>,
     *                          <code>byte</code> and <code>char</code> resulting values (see comments to
     *                          {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to the source AlgART matrices.
     * @param result            the destination matrix.
     * @param x                 several AlgART matrices; may be empty.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> matrices is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  Arrays#applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)}
     *                                  method.
     * @throws SizeMismatchException    if some of the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            Func f,
            Matrix<? extends UpdatablePArray> result,
            List<? extends Matrix<? extends PArray>> x) {
        Objects.requireNonNull(result, "Null result argument");
        PArray[] arrays = arraysOfParallelMatrices(PArray.class, x);
        int k = 0;
        for (Matrix<? extends PArray> m : x) {
            if (!m.dimEquals(result)) {
                throw new SizeMismatchException("x.get(" + k + ") and result matrix dimensions mismatch: "
                        + "matrix #" + k + " is " + m + ", result matrix is " + result);
            }
            arrays[k++] = m.array();
        }
        Arrays.applyFunc(context, truncateOverflows, f, result.array(), arrays);
    }

    /**
     * Returns an immutable view of the passed AlgART matrix, cast to another primitive element type
     * (other precision) with automatic scaling, so that 0.0 is cast to 0.0 and
     * {@link PArray#maxPossibleValue(double) maximal possible value} of the source matrix
     * is scaled to maximal possible value of the result. (For <code>float</code> and <code>double</code>
     * elements we suppose that maximal possible value is 1.0.)
     *
     * <p>More precisely, if <code>newElementType==matrix.elementType()</code>, this function just returns
     * the <code>matrix</code> argument without changes; in another case, it is equivalent to the following operators:
     * <pre>
     *     final Class&lt;PArray&gt; newType = Arrays.type(PArray.class, newElementType);
     *     final Range destRange = Range.valueOf(0.0, {@link Arrays#maxPossibleValue(Class)
     *     Arrays.maxPossibleValue}(newType));
     *     final Range srcRange = Range.valueOf(0.0, matrix.{@link Matrix#maxPossibleValue()
     *     maxPossibleValue()});
     *     return {@link Matrices#asFuncMatrix(Func, Class, Matrix)
     *     Matrices.asFuncMatrix}(LinearFunc.getInstance(destRange, srcRange), newType, matrix);
     * </pre>
     *
     * @param matrix         the source AlgART matrix.
     * @param newElementType required element type.
     * @return the matrix with the required element type, where every element is equal to
     * the corresponding element of the source matrix, multiplied
     * by the automatically chosen scale.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the required element type is not a primitive type.
     */
    public static Matrix<? extends PArray> asPrecision(Matrix<? extends PArray> matrix, Class<?> newElementType) {
        Objects.requireNonNull(matrix, "Null matrix");
        Objects.requireNonNull(newElementType, "Null newElementType");
        if (newElementType == matrix.elementType()) {
            return matrix;
        }
        return matrix.matrix(Arrays.asPrecision(matrix.array(), newElementType));
    }

    /**
     * Equivalent to {@link #applyPrecision(ArrayContext, Matrix, Matrix)
     * applyPrecision(null, result, matrix)}.
     *
     * @param result the destination matrix.
     * @param matrix the source matrix.
     * @throws NullPointerException  if <code>result</code> or <code>matrix</code> is {@code null}.
     * @throws SizeMismatchException if passed matrices have different dimensions.
     */
    public static void applyPrecision(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> matrix) {
        applyPrecision(null, result, matrix);
    }

    /**
     * Equivalent to creating a "lazy" matrix by <code>lazy = {@link #asPrecision(Matrix, Class)
     * asPrecision(matrix, result.elementType()}</code> call
     * and copying it into the <code>result</code> argument by
     * <code>{@link #copy(ArrayContext, Matrix, Matrix) copy(context, result, lazy)}</code> call.
     *
     * <p>In addition, this method checks, whether all passed matrices have the
     * {@link Matrix#dimEquals(Matrix) same dimensions},
     * and throws an exception in another case.
     *
     * <p>If the source and result matrices have the same element type, this method just copies <code>matrix</code>
     * to <code>result</code>.
     *
     * @param context the context of copying; can be {@code null}.
     * @param result  the destination matrix.
     * @param matrix  the source matrix.
     * @throws NullPointerException  if <code>result</code> or <code>matrix</code> is {@code null}.
     * @throws SizeMismatchException if passed matrices have different dimensions.
     * @throws java.io.IOError       if the current thread is interrupted by the standard
     *                               <code>Thread.interrupt()</code> call.
     */
    public static void applyPrecision(
            ArrayContext context,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> matrix) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(matrix, "Null source matrix");
        if (!matrix.dimEquals(result)) {
            throw new SizeMismatchException("Source and result matrix dimensions mismatch: "
                    + "source matrix is " + matrix + ", result matrix is " + result);
        }
        Arrays.applyPrecision(context, result.array(), matrix.array());
    }

    /**
     * Returns an immutable view of the passed AlgART matrix, resized to the specified dimensions <code>newDim</code>.
     * If is also a good example of cooperative using
     * {@link #asInterpolationFunc(Matrix, net.algart.arrays.Matrices.InterpolationMethod, boolean)
     * asInterpolationFunc},
     * {@link #asCoordFuncMatrix(boolean, net.algart.math.functions.Func, Class, long...)
     * asCoordFuncMatrix} and
     * {@link LinearOperator affine transforms} of the functions: see below.
     *
     * <p>Namely, this method performs conversion of the matrix to
     * a mathematical function from the coordinates
     * (by {@link #asInterpolationFunc(Matrix, InterpolationMethod, double)} method),
     * transforms that function by the resizing linear operator
     * (see {@link LinearOperator#getDiagonalInstance(double...)} method)
     * and then performs the back conversion of the transformed function to the result matrix
     * (by {@link #asCoordFuncMatrix(Func, Class, long...)} method).
     * The details depend on <code>resizingMethod</code> argument:
     *
     * <ol>
     * <li>If <code>resizingMethod</code> is {@link ResizingMethod#SIMPLE}, this method is equivalent to:
     * <pre>
     * {@link Func} interpolation = {@link Matrices}.{@link #asInterpolationFunc(Matrix, InterpolationMethod, double)
     * asInterpolationFunc}(
     *     matrix, {@link InterpolationMethod#STEP_FUNCTION}, 0.0);
     * {@link Func} transformed = {@link LinearOperator}.{@link LinearOperator#getDiagonalInstance(double...)
     * getDiagonalInstance}(diagonal).apply(interpolation);
     * {@link Matrix} result = {@link Matrices}.{@link #asCoordFuncMatrix(Func, Class, long...)
     * asCoordFuncMatrix}(
     *     transformed, matrix.{@link Matrix#type(Class) type}(PArray.class), newDim);
     * </pre>
     * Here <code>diagonal</code> is the array of relations between old and new dimensions:
     * <pre>
     *     diagonal[k] = (double)matrix.{@link Matrix#dim(int) dim}(k) / (double)newDim[k]
     * </pre>
     * It is the simplest possible resizing method without any interpolation or averaging:
     * every element of the returned matrix is strictly equal to some element of the source one.
     * <br>&nbsp;
     *
     * <li>If <code>resizingMethod</code> is {@link ResizingMethod#AVERAGING}, this method does the same,
     * but if some of <code>diagonal</code> values are greater than 1.5 (that means compression),
     * it also performs additional transformation of <code>transformed</code> function
     * by the operator {@link ApertureFilterOperator#getAveragingInstance(long... apertureDim)},
     * where <code>apertureDim</code> is the sizes of the aperture in the source matrix,
     * mapped to a single result element.
     * As a result, the value of each result element is an average from several source elements,
     * that are mapped to that one by this compression.
     * The details of this averaging are not specified.
     * This method is necessary while compression, if you want to get maximally "good" result picture.
     * It works fine while compression in the integer number of times (all <code>diagonal</code> values are integers).
     * In another case, {@link ResizingMethod#POLYLINEAR_AVERAGING} mode can produce better results.
     * <br>&nbsp;
     *
     * <li>If <code>resizingMethod</code> is {@link ResizingMethod#POLYLINEAR_INTERPOLATION} or
     * {@link ResizingMethod#POLYLINEAR_AVERAGING}, this method does the same as in cases 1 and 2,
     * but the argument of {@link #asInterpolationFunc(Matrix, InterpolationMethod, double) asInterpolationFunc}
     * is {@link InterpolationMethod#POLYLINEAR_FUNCTION POLYLINEAR_FUNCTION}
     * instead of {@link InterpolationMethod#STEP_FUNCTION STEP_FUNCTION}.
     * These modes are useful while expanding the matrix, because allow to use interpolation
     * for values "between" its elements.
     * The {@link ResizingMethod#POLYLINEAR_AVERAGING} is usually the best choice.
     * <br>&nbsp;
     *
     * <li>If <code>resizingMethod</code> is some inheritor of {@link ResizingMethod.Averaging} class,
     * this method does the same as in cases {@link ResizingMethod#AVERAGING} and
     * {@link ResizingMethod#POLYLINEAR_AVERAGING},
     * but the averaging operator will be created not by
     * {@link ApertureFilterOperator#getAveragingInstance(long... apertureDim)},
     * but by {@link ApertureFilterOperator#getInstance(Func func, long... apertureDim)} method,
     * where the first argument is the result of {@link ResizingMethod.Averaging#getAveragingFunc(long[])
     * resizingMethod.getAveragingFunc(apertureDim)}.
     * This allows specifying non-standard averaging algorithm.
     * For example, for binary matrices, containing a little number of unit elements, {@link Func#MAX} can be
     * a better choice than the usual linear averaging.
     * </ol>
     *
     * <p>There is an exception to the rules listed above. If the source matrix is empty
     * (at least one dimension is 0), the returned matrix will be zero-filled always.
     * (In this case, there are no ways to calculate any elements.)
     *
     * @param resizingMethod the algorithm of resizing.
     * @param matrix         the source AlgART matrix.
     * @param newDim         the dimensions of resized matrix.
     * @return the resized matrix.
     * @throws NullPointerException     if <code>resizingMethod</code>, <code>matrix</code> or
     *                                  <code>newDim</code> argument is {@code null}.
     * @throws IllegalArgumentException if the number of <code>newDim</code> elements is not equal to
     *                                  <code>matrix.{@link Matrix#dimCount() dimCount()}</code>,
     *                                  or if some of new dimensions are negative.
     * @see #resize(ArrayContext, Matrices.ResizingMethod, Matrix, Matrix)
     * @see #asResized(Matrices.ResizingMethod, Matrix, long[], double[])
     */
    public static Matrix<PArray> asResized(
            ResizingMethod resizingMethod,
            Matrix<? extends PArray> matrix, long... newDim) {
        return ArraysMatrixResizer.asResized(resizingMethod, matrix, newDim, null);
    }

    /**
     * An extended analog of {@link #asResized(Matrices.ResizingMethod, Matrix, long...)} method,
     * allowing to precisely specify a custom scaling value along every coordinate.
     * Namely, while that method scales every coordinate <code>#k</code> in
     * <code>newDim[k]/matrix.{@link Matrix#dim(int) dim}(k)</code> times,
     * this method scales it precisely in <code>scales[k]</code> time.
     * If, for every <code>k</code>, we have
     * <pre>
     *     scales[k]==(double)newDim[k]/(double)matrix.{@link Matrix#dim(int) dim}(k)
     * </pre>
     * or if <code>scales</code> argument is {@code null}, this method is equivalent to
     * {@link #asResized(Matrices.ResizingMethod, Matrix, long...)}.
     *
     * <p>To get a strict behavior specification of this method, please
     * look at the comments to {@link #asResized(Matrices.ResizingMethod, Matrix, long...)},
     * section 1, and replace definition of <code>diagonal</code> array with the following:
     * <pre>
     *     diagonal[k] = scales == null ? (double)matrix.{@link Matrix#dim(int)
     * dim}(k) / (double)newDim[k] : 1.0 / scales[k]
     * </pre>
     *
     * @param resizingMethod the algorithm of resizing.
     * @param matrix         the source AlgART matrix.
     * @param newDim         the dimensions of resized matrix.
     * @param scales         the scales of resizing along every coordinate; can be {@code null},
     *                       then calculated automatically as
     *                       <code>scales[k] = (double)newDim[k]/(double)matrix.{@link Matrix#dim(int) dim}(k)</code>.
     * @return the resized matrix.
     * @throws NullPointerException     if <code>resizingMethod</code>, <code>matrix</code> or
     *                                  <code>newDim</code> argument is {@code null}.
     * @throws IllegalArgumentException if the length of <code>newDim</code> or (when <code>scales!=null</code>)
     *                                  <code>scales</code> array is not equal to
     *                                  <code>matrix.{@link Matrix#dimCount() dimCount()}</code>,
     *                                  or if some of new dimensions are negative.
     * @see #resize(ArrayContext, Matrices.ResizingMethod, Matrix, Matrix)
     */
    public static Matrix<PArray> asResized(
            ResizingMethod resizingMethod,
            Matrix<? extends PArray> matrix, long[] newDim, double[] scales) {
        // Note! The following declaration would be incorrect:
        // public static <T extends PArray> Matrix<T> asResized(
        //    ResizingMethod resizingMethod,
        //    Matrix<T> matrix, long... newDim)
        // The reason is Matrix<UpdatableArray>: this method cannot return updatable matrices.
        return ArraysMatrixResizer.asResized(resizingMethod, matrix, newDim, scales);
    }

    /**
     * Equivalent to {@link #resize(ArrayContext, ResizingMethod, Matrix, Matrix)
     * resize(null, resizingMethod, result, src)}.
     *
     * @param resizingMethod the algorithm of resizing.
     * @param result         the destination matrix.
     * @param src            the source matrix.
     * @throws NullPointerException     if <code>resizingMethod</code>, <code>result</code> or
     *                                  <code>src</code> argument is {@code null}.
     * @throws IllegalArgumentException if the passed matrices have different number of dimensions.
     */
    public static void resize(
            ResizingMethod resizingMethod,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> src) {
        resize(null, resizingMethod, result, src);
    }

    /**
     * Resizes the source matrix to the dimensions of the passed <code>result</code> matrix
     * and stores the resized matrix in <code>result</code> argument.
     *
     * <p>This method is equivalent to the following operators:
     * <pre>
     *     Matrix&lt;?&gt; lazy = {@link #asResized(Matrices.ResizingMethod, Matrix, long...)
     *     Matrices.asResized}(resizingMethod, src, result.{@link Matrix#dimensions() dimensions()});
     *     {@link #copy(ArrayContext, Matrix, Matrix, int, boolean)
     *     Matrices.copy}(context, result, lazy, 0, false);
     * </pre>
     *
     * <p>Note: in many cases this method works essentially faster than simple reading all elements of
     * the result of {@link #asResized(net.algart.arrays.Matrices.ResizingMethod, Matrix, long...) asResized} method.
     *
     * <p>Please draw attention to the argument <code>strictMode=false</code> of <code>Matrices.copy</code> method:
     * it is important for providing good performance for resizing large matrices.
     * This argument specifies that the precise results of this method may little differ from
     * the elements of the {@link #asResized(Matrices.ResizingMethod, Matrix, long...) asResized} result
     * (the "<code>lazy</code>" matrix above).
     *
     * <p>Moreover, the precise results of this method may little differ also from the results of the code above
     * (calling <code>Matrices.copy</code> method).
     *
     * <p>Usually the differences are little numeric errors,
     * connected with limited precision of floating-point calculating the coordinates.
     * For example, it is possible that the coordinates of the set of points,
     * the values of which are averaged in {@link ResizingMethod#AVERAGING} resizing method,
     * will be slightly different in this method and in the precise specification of
     * {@link #asResized(net.algart.arrays.Matrices.ResizingMethod, Matrix, long...)}
     * (according usage of {@link ApertureFilterOperator#getAveragingInstance(long... apertureDim)} operator).
     * For {@link ResizingMethod#AVERAGING} mode it may lead to rounding coordinates to another integers,
     * so, the result of averaging will be little other.
     * For more accurate {@link ResizingMethod#POLYLINEAR_AVERAGING}) mode the result of averaging will
     * be almost the same.
     *
     * <p>By the way, in a case of such differences, the results of this method usually better correspond
     * to intuitive expectations of the resizing results.
     *
     * @param context        the context of resizing; can be {@code null}, then it will be ignored.
     * @param resizingMethod the algorithm of resizing.
     * @param result         the destination matrix.
     * @param src            the source matrix.
     * @throws NullPointerException     if <code>resizingMethod</code>, <code>result</code> or
     *                                  <code>src</code> argument is {@code null}.
     * @throws IllegalArgumentException if the passed matrices have different number of dimensions.
     */
    public static void resize(
            ArrayContext context,
            ResizingMethod resizingMethod,
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> src) {
        ArraysMatrixResizer.resize(context, resizingMethod, result, src);
    }

    /**
     * Addition: equivalent to
     * <code>{@link #addToOther(Matrix, Matrix, Matrix) addToOther}(result, result, other)</code>.
     *
     * @param result matrix <b>a</b> that will be replaced with the elementwise sum <b>a + b</b>.
     * @param other  matrix <b>b</b>.
     */
    public static void add(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> other) {
        addToOther(result, result, other);
    }

    /**
     * Addition: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, {@link Func#X_PLUS_Y
     * Func.X_PLUS_Y}, result, a, b)</code>.
     *
     * @param result elementwise sum <b>a + b</b>.
     * @param a      matrix <b>a</b>.
     * @param b      matrix <b>b</b>.
     */
    public static void addToOther(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> a,
            Matrix<? extends PArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.X_PLUS_Y, result, a, b);
    }

    /**
     * Subtraction: equivalent to
     * <code>{@link #subtractToOther(Matrix, Matrix, Matrix) subtractToOther}(result, result, other)</code>.
     *
     * @param result matrix <b>a</b> that will be replaced with the difference <b>a &minus; b</b>.
     * @param other  matrix <b>b</b>.
     */
    public static void subtract(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> other) {
        subtractToOther(result, result, other);
    }

    /**
     * Subtraction: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, {@link Func#X_MINUS_Y
     * Func.X_MINUS_Y}, result, a, b)</code>.
     *
     * @param result elementwise difference <b>a &minus; b</b>.
     * @param a      matrix <b>a</b>.
     * @param b      matrix <b>b</b>.
     */
    public static void subtractToOther(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> a,
            Matrix<? extends PArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.X_MINUS_Y, result, a, b);
    }

    /**
     * Binary OR: equivalent to
     * <code>{@link #bitOrToOther(Matrix, Matrix, Matrix) bitOrToOther}(result, result, other)</code>.
     *
     * @param result matrix <b>a</b> that will be replaced with the binary OR <b>a | b</b>.
     * @param other  matrix <b>b</b>.
     */
    public static void bitOr(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitOrToOther(result, result, other);
    }

    /**
     * Binary OR: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, Func.MAX, result, a, b)</code>.
     *
     * @param result binary OR <b>a | b</b>.
     * @param a      matrix <b>a</b>.
     * @param b      matrix <b>b</b>.
     */
    public static void bitOrToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.MAX, result, a, b);
    }

    /**
     * Binary AND: equivalent to
     * <code>{@link #bitAndToOther(Matrix, Matrix, Matrix) bitAndToOther}(result, result, other)</code>.
     *
     * @param result matrix <b>a</b> that will be replaced with the binary AND <b>a &amp; b</b>.
     * @param other  matrix <b>b</b>.
     */
    public static void bitAnd(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitAndToOther(result, result, other);
    }

    /**
     * Binary AND: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, Func.MIN, result, a, b)</code>.
     *
     * @param result binary AND <b>a | b</b>.
     * @param a      matrix <b>a</b>.
     * @param b      matrix <b>b</b>.
     */
    public static void bitAndToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.MIN, result, a, b);
    }


    /**
     * Binary XOR: equivalent to
     * <code>{@link #bitXorToOther(Matrix, Matrix, Matrix) bitXorToOther}(result, result, other)</code>.
     *
     * @param result matrix <b>a</b> that will be replaced with the binary XOR <b>a ^ b</b>.
     * @param other  matrix <b>b</b>.
     */
    public static void bitXor(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitXorToOther(result, result, other);
    }

    /**
     * Binary XOR: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, Func.ABS_DIFF, result, a, b)</code>.
     *
     * @param result binary XOR <b>a ^ b</b>.
     * @param a      matrix <b>a</b>.
     * @param b      matrix <b>b</b>.
     */
    public static void bitXorToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.ABS_DIFF, result, a, b);
        // - actually this and other functions are performed by classes like ArraysDiffGetDataOp
        // via maximally efficient binary operations
    }

    /**
     * Binary AND-NOT: equivalent to
     * <code>{@link #bitDiffToOther(Matrix, Matrix, Matrix) bitDiffToOther}(result, result, other)</code>.
     *
     * @param result matrix <b>a</b> that will be replaced with the binary AND-NOT <b>a &amp; ~b</b>.
     * @param other  matrix <b>b</b>.
     */
    public static void bitDiff(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitDiffToOther(result, result, other);
    }

    /**
     * Binary AND-NOT: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, Func.POSITIVE_DIFF, result, a, b)</code>.
     *
     * @param result binary AND-NOT <b>a &amp; ~b</b>.
     * @param a      matrix <b>a</b>.
     * @param b      matrix <b>b</b>.
     */
    public static void bitDiffToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.POSITIVE_DIFF, result, a, b);
    }

    /**
     * Binary NOT: equivalent to
     * <code>{@link #bitNotToOther(Matrix, Matrix) bitNotToOther}(bitMatrix, bitMatrix)</code>.
     *
     * @param bitMatrix matrix <b>a</b> that will be replaced with the binary NOT <b>~a</b>.
     */
    public static void bitNot(Matrix<? extends UpdatableBitArray> bitMatrix) {
        bitNotToOther(bitMatrix, bitMatrix);
    }

    /**
     * Binary NOT: equivalent to
     * <code>{@link #applyFunc(ArrayContext, Func, Matrix, Matrix)
     * applyFunc}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, Func.REVERSE, result, source)</code>.
     *
     * @param result binary NOT <b>~a</b>.
     * @param source matrix <b>a</b>.
     */
    public static void bitNotToOther(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> source) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.REVERSE, result, source);
    }

    /*Repeat() packBitsGreater ==> packBitsLess,,packBitsGreaterOrEqual,,packBitsLessOrEqual */

    /**
     * Equivalent to
     * <code>{@link Arrays#packBitsGreater(UpdatableBitArray, PArray, double)
     * packBitsGreater}(result.array(), intensities.array(), threshold)</code>
     *
     * @param result      result bit matrix.
     * @param intensities source matrix.
     * @param threshold   threshold that will be compared with all elements of <code>intensities</code>.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void packBitsGreater(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(intensities, "Null intensities matrix");
        checkDimensionEquality(result, intensities);
        Arrays.packBitsGreater(result.array(), intensities.array(), threshold);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to
     * <code>{@link Arrays#packBitsLess(UpdatableBitArray, PArray, double)
     * packBitsLess}(result.array(), intensities.array(), threshold)</code>
     *
     * @param result      result bit matrix.
     * @param intensities source matrix.
     * @param threshold   threshold that will be compared with all elements of <code>intensities</code>.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void packBitsLess(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(intensities, "Null intensities matrix");
        checkDimensionEquality(result, intensities);
        Arrays.packBitsLess(result.array(), intensities.array(), threshold);
    }

    /**
     * Equivalent to
     * <code>{@link Arrays#packBitsGreaterOrEqual(UpdatableBitArray, PArray, double)
     * packBitsGreaterOrEqual}(result.array(), intensities.array(), threshold)</code>
     *
     * @param result      result bit matrix.
     * @param intensities source matrix.
     * @param threshold   threshold that will be compared with all elements of <code>intensities</code>.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void packBitsGreaterOrEqual(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(intensities, "Null intensities matrix");
        checkDimensionEquality(result, intensities);
        Arrays.packBitsGreaterOrEqual(result.array(), intensities.array(), threshold);
    }

    /**
     * Equivalent to
     * <code>{@link Arrays#packBitsLessOrEqual(UpdatableBitArray, PArray, double)
     * packBitsLessOrEqual}(result.array(), intensities.array(), threshold)</code>
     *
     * @param result      result bit matrix.
     * @param intensities source matrix.
     * @param threshold   threshold that will be compared with all elements of <code>intensities</code>.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void packBitsLessOrEqual(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(intensities, "Null intensities matrix");
        checkDimensionEquality(result, intensities);
        Arrays.packBitsLessOrEqual(result.array(), intensities.array(), threshold);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to
     * <code>{@link Arrays#unpackBits(UpdatablePArray, BitArray, double, double)
     * unpackBits}(result.array(), bits.array(), filler0, filler1)</code>
     *
     * @param result  result matrix.
     * @param bits    source bit matrix.
     * @param filler0 the value that will be set in <code>result</code> for zero bits.
     * @param filler1 the value that will be set in <code>result</code> for unit bits.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void unpackBits(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends BitArray> bits,
            double filler0,
            double filler1) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(bits, "Null bits matrix");
        checkDimensionEquality(result, bits);
        Arrays.unpackBits(result.array(), bits.array(), filler0, filler1);
    }

    /**
     * Equivalent to
     * <code>{@link Arrays#unpackUnitBits(UpdatablePArray, BitArray, double)
     * unpackUnitBits}(result.array(), bits.array(), filler1)</code>
     *
     * @param result  result matrix.
     * @param bits    source bit matrix.
     * @param filler1 the value that will be set in <code>result</code> for unit bits.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void unpackUnitBits(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends BitArray> bits,
            double filler1) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(bits, "Null bits matrix");
        checkDimensionEquality(result, bits);
        Arrays.unpackUnitBits(result.array(), bits.array(), filler1);
    }

    /**
     * Equivalent to
     * <code>{@link Arrays#unpackZeroBits(UpdatablePArray, BitArray, double)
     * unpackZeroBits}(result.array(), bits.array(), filler0)</code>
     *
     * @param result  result matrix.
     * @param bits    source bit matrix.
     * @param filler0 the value that will be set in <code>result</code> for zero bits.
     * @throws NullPointerException  if one of the passed matrices is {@code null}.
     * @throws SizeMismatchException if the passed matrices have different dimensions.
     */
    public static void unpackZeroBits(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends BitArray> bits,
            double filler0) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(bits, "Null bits matrix");
        checkDimensionEquality(result, bits);
        Arrays.unpackZeroBits(result.array(), bits.array(), filler0);
    }

    /**
     * Returns an immutable view of the passed AlgART matrix,
     * pseudo-cyclically shifted to the right for every coordinate.
     *
     * <p>The shift is not really cyclic.
     * Really, the built-in AlgART is
     * {@link Arrays#asShifted(Array, long) cyclically shifted} to the right,
     * that means shifting end elements of every line <code>#y</code> to the beginning
     * of the next line <code>#y+1</code>,
     * end lines of every plane (layer) <code>#z</code> to the beginning of the next plane <code>#z+1</code>, etc.
     * More precisely, this method is equivalent to the following operators:
     *
     * <pre>
     * Array array = matrix.{@link Matrix#array() array}();
     * Array shifted = {@link Arrays#asShifted(Array, long) Arrays.asShifted}(array, shift);
     * Matrix&lt;Array&gt; result = matrix.{@link Matrix#matrix(Array) matrix}(shifted);
     * </pre>
     *
     * <p>where the <code>shift</code> of the built-in array is calculated as
     *
     * <pre>
     * shifts[0] +
     * + shifts[1] * matrix.{@link Matrix#dim(int) dim}(0) +
     * + shifts[2] * matrix.{@link Matrix#dim(int) dim}(0) * matrix.{@link Matrix#dim(int) dim}(1) +
     * + . . . +
     * + shifts[<i>n</i>&minus;1] * matrix.{@link Matrix#dim(int) dim}(0) * matrix.{@link Matrix#dim(int)
     * dim}(1) * ... * matrix.{@link Matrix#dim(int) dim}(<i>n</i>-2) (<i>n</i> = shifts.length)
     * </pre>
     *
     * <p>All calculations are performed with <code>long</code> type without any overflow checks.
     * All elements of <code>shifts</code> array are always used, regardless of the number of matrix dimensions.
     * (You can note that extra elements of <code>shifts</code> array are ignored in fact:
     * they add <code>k*length</code> summand, where <code>k</code> is an integer and
     * <code>length</code> is the array length.)
     * If <code>shifts</code> array is empty, the resulting <code>shift=0</code>.
     *
     * <p>The result of this method has <code>{@link Matrix}&lt;{@link Array}&gt;</code> generic type always,
     * though the built-in array of the returned matrix implements the more specific interface
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray}, {@link DoubleArray} or {@link ObjectArray}
     * (the same as the built-in array of the passed matrix).
     * If you need to get the matrix with more specific generic type, please use {@link Matrix#cast(Class)} method.
     * For example:
     *
     * <pre>
     * {@link Matrix}&lt;{@link PFixedArray}&gt; src = ...; // source matrix
     * {@link Matrix}&lt;{@link PFixedArray}&gt; dest = Matrices.asShifted(src, dx, dy).{@link
     * Matrix#cast(Class) cast}({@link PFixedArray}.class);
     * </pre>
     *
     * @param matrix the source AlgART matrix.
     * @param shifts the shifts (to the right) of all indexes in the returned view.
     * @return a shifted view of the passed matrix.
     * @throws NullPointerException if <code>shifts</code> or <code>matrix</code> argument is {@code null}.
     */
    public static Matrix<Array> asShifted(Matrix<? extends Array> matrix, long... shifts) {
        // Note! The following declaration would be incorrect:
        // public static <T extends Array> Matrix<T> asShifted(
        //     Matrix<T> matrix, long ...shifts)
        // The reason is Matrix<UpdatableArray>: this method cannot return updatable matrices.
        Objects.requireNonNull(matrix, "Null matrix argument");
        Objects.requireNonNull(shifts, "Null shifts argument");
        long shift = shifts.length == 0 ? 0 : shifts[shifts.length - 1];
        for (int k = shifts.length - 2; k >= 0; k--) {
            shift = shift * matrix.dim(k) + shifts[k];
        }
        Array array = matrix.array();
        Array shifted = Arrays.asShifted(array, shift);
        return matrix.matrix(shifted);
    }

    /**
     * Equivalent to {@link #clone(MemoryModel, Matrix) clone(Arrays.SMM, matrix)}.
     *
     * <p>Note: this operation can optimize access to this matrix in many times, if it is lazy-calculated
     * and not too large (can be placed in available Java memory).
     * It performs cloning with maximal speed via multithreading optimization. We recommend to call
     * it after lazy calculations.</p>
     *
     * @param matrix the matrix to be cloned.
     * @return an exact updatable clone of the passed matrix.
     * @throws NullPointerException if the argument is {@code null}.
     * @see Matrix#clone()
     * @see Matrix#clone(ArrayContext)
     */
    public static Matrix<? extends UpdatablePArray> clone(Matrix<? extends PArray> matrix) {
        return clone(Arrays.SMM, matrix);
    }

    /**
     * Returns an exact updatable clone of the given matrix, created in the given memory model.
     * Equivalent to the following operators:
     * <pre>
     *     final Matrix&lt;UpdatablePArray&gt; result = memoryModel.{@link MemoryModel#newMatrix(Class, Matrix)
     *     newMatrix}(UpdatablePArray.class, matrix);
     *     {@link Matrices#copy(ArrayContext, Matrix, Matrix)
     *     Matrices.copy}(null, result, matrix); // - maximally fast multithreading copying
     *     (return result)
     * </pre>
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @param matrix      the matrix to be cloned.
     * @return an exact updatable clone of the passed matrix.
     * @throws NullPointerException if one of the arguments is {@code null}.
     * @see Matrix#clone(ArrayContext)
     */
    public static Matrix<? extends UpdatablePArray> clone(MemoryModel memoryModel, Matrix<? extends PArray> matrix) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(matrix, "Null matrix");
        final Matrix<UpdatablePArray> result = memoryModel.newMatrix(UpdatablePArray.class, matrix);
        Matrices.copy(null, result, matrix);
        // - maximally fast multithreading copying
        return result;
    }

    /**
     * This method just calls <code>{@link Arrays#copy(ArrayContext, UpdatableArray, Array)
     * Arrays.copy}(context, dest.{@link Matrix#array() array()}, src.{@link Matrix#array() array()})</code>,
     * if the passed matrices {@link Matrix#dimEquals have the same dimensions},
     * or throws {@link SizeMismatchException} in another case.
     *
     * @param context the context of copying; can be {@code null}.
     * @param dest    the destination matrix.
     * @param src     the source matrix.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static Arrays.CopyStatus copy(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src) {
        return copy(context, dest, src, 0);
    }

    /**
     * This method just calls <code>{@link Arrays#copy(ArrayContext, UpdatableArray, Array, int)
     * Arrays.copy}(context, dest.{@link Matrix#array() array()}, src.{@link Matrix#array() array()},
     * numberOfTasks)</code>,
     * if the passed matrices {@link Matrix#dimEquals have the same dimensions},
     * or throws {@link SizeMismatchException} in another case.
     *
     * @param context       the context of copying; can be {@code null}.
     * @param dest          the destination matrix.
     * @param src           the source matrix.
     * @param numberOfTasks the required number of parallel tasks;
     *                      can be <code>0</code>, then it will be chosen automatically.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match,
     *                                  or if the <code>numberOfThreads</code> argument is negative.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static Arrays.CopyStatus copy(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            int numberOfTasks) {
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(src, "Null src argument");
        if (!dest.dimEquals(src)) {
            throw new SizeMismatchException("dest and src matrix dimensions mismatch: " + dest + " and " + src);
        }
        return Arrays.copy(context, dest.array(), src.array(), numberOfTasks);
    }

    /**
     * This method just calls <code>{@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)
     * Arrays.copy}(context, dest.{@link Matrix#array() array()}, src.{@link Matrix#array() array()},
     * numberOfTasks, strictMode)</code>,
     * if the passed matrices {@link Matrix#dimEquals have the same dimensions},
     * or throws {@link SizeMismatchException} in another case.
     *
     * @param context       the context of copying; can be {@code null}.
     * @param dest          the destination matrix.
     * @param src           the source matrix.
     * @param numberOfTasks the required number of parallel tasks;
     *                      can be <code>0</code>, then it will be chosen automatically.
     * @param strictMode    if <code>false</code>, optimization is allowed even if it can lead to little differences
     *                      between the source and copied elements.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match,
     *                                  or if the <code>numberOfThreads</code> argument is negative.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static Arrays.CopyStatus copy(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            int numberOfTasks, boolean strictMode) {
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(src, "Null src argument");
        if (!dest.dimEquals(src)) {
            throw new SizeMismatchException("dest and src matrix dimensions mismatch: "
                    + dest + " and " + src);
        }
        return Arrays.copy(context, dest.array(), src.array(), numberOfTasks, strictMode);
    }

    /**
     * This method just calls <code>{@link Arrays#compareAndCopy(ArrayContext, UpdatableArray, Array)
     * Arrays.compareAndCopy}(context, dest.{@link Matrix#array() array()}, src.{@link Matrix#array() array()})</code>,
     * if the passed matrices {@link Matrix#dimEquals have the same dimensions},
     * or throws {@link SizeMismatchException} in another case.
     *
     * @param context the context of copying; can be {@code null}.
     * @param dest    the destination matrix.
     * @param src     the source matrix.
     * @return the result of {@link Arrays#compareAndCopy Arrays.compareAndCopy} call.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static Arrays.ComparingCopyStatus compareAndCopy(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src) {
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(src, "Null src argument");
        if (!dest.dimEquals(src)) {
            throw new SizeMismatchException("dest and src matrix dimensions mismatch: "
                    + dest + " and " + src);
        }
        return Arrays.compareAndCopy(context, dest.array(), src.array());
    }

    /**
     * Just copies <code>src</code> into <code>dest</code> without using multithreading.
     * Equivalent to <code>{@link #copy(ArrayContext, Matrix, Matrix)
     * copy}(ArrayContext.{@link ArrayContext#DEFAULT_SINGLE_THREAD DEFAULT_SINGLE_THREAD}, dest, src)</code>.
     *
     * @param dest the destination matrix.
     * @param src  the src matrix.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the src and destination element types do not match.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     */
    public static void copy(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
        copy(ArrayContext.DEFAULT_SINGLE_THREAD, dest, src);
    }

    /**
     * Equivalent to {@link #copyRegion(ArrayContext, Matrix, Matrix, Region, long...)
     * copyRegion(null, dest, src, destRegion, shifts)}.
     *
     * @param dest       the destination matrix.
     * @param src        the source matrix.
     * @param destRegion the region in the destination matrix that should be copied from the source one.
     * @param shifts     the shift between the source and destination regions.
     * @throws NullPointerException      if <code>dest</code>, <code>src</code>, <code>destRegion</code>
     *                                   or <code>shifts</code> argument is {@code null}.
     * @throws IllegalArgumentException  if the source and destination element types do not match,
     *                                   i.e. if <code>dest.{@link Matrix#elementType() elementType()}</code>
     *                                   is not equal to <code>src.{@link Matrix#elementType() elementType()}</code>
     *                                   and is not its superclass (for non-primitive element types).
     * @throws IndexOutOfBoundsException if some integer point <i>x</i>, belonging to <code>destRegion</code>,
     *                                   lies outside <code>dest</code> matrix,
     *                                   or the integer point <i>x</i>', obtained from it by
     *                                   subtracting <code>shifts</code>,  lies outside <code>src</code> matrix
     *                                   (for regions, other than {@link Matrices.Hyperparallelepiped}, can be thrown
     *                                   in the middle of working <i>after</i> copying some elements).
     */
    public static void copyRegion(
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            Region destRegion,
            long... shifts) {
        copyRegion(null, dest, src, destRegion, shifts);
    }

    /**
     * Copies the specified region from <code>src</code> AlgART matrix to <code>dest</code> AlgART matrix.
     * The region in <code>dest</code> matrix, that will be copied from <code>src</code>, is specified by
     * <code>destRegion</code> argument.
     * The corresponding region in <code>src</code> matrix, that will be copied into <code>dest</code>,
     * is obtained from <code>destRegion</code> by <i>subtracting</i> the specified <code>shifts</code>
     * from all coordinates of <code>destRegion</code>.
     *
     * <p>More precisely, this method is 3 the following loop (and, of course, works faster):
     *
     * <p><code>
     * for (all coordinates <i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub><br>
     * &#32;   in ranges destRegion.{@link Region#coordRange(int)
     * coordRange}(0), destRegion.{@link Region#coordRange(int) coordRange}(1),<br>
     * &#32;   ..., destRegion.{@link Region#coordRange(int) coordRange}(<i>n</i>&minus;1))<br>
     * {<br>
     * &#32;   if (destRegion.{@link Region#contains(long...)
     * contains}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>) {<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long <i>x</i>'<sub>0</sub> = <i>x</i><sub>0</sub> - (shifts.length &gt; 0 ? shifts[0] : 0);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long <i>x</i>'<sub>1</sub> = <i>x</i><sub>1</sub> - (shifts.length &gt; 1 ? shifts[1] : 0);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * ...<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long <i>x</i>'<sub><i>n</i>&minus;1</sub> = <i>x</i><sub><i>n</i>&minus;1</sub> -
     * (shifts.length &gt; <i>n</i>&minus;1 ? shifts[<i>n</i>&minus;1] : 0);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long destIndex = dest.{@link Matrix#index(long...)
     * index}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long srcIndex = src.{@link Matrix#index(long...)
     * index}(<i>x</i>'<sub>0</sub>, <i>x</i>'<sub>1</sub>, ..., <i>x</i>'<sub><i>n</i>&minus;1</sub>);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * Object element = src.array().{@link Array#getElement(long) getElement}(srcIndex);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * dest.array().{@link UpdatableArray#setElement(long, Object) setElement}(destIndex, element);<br>
     * &#32;   }<br>
     * }
     * </code></p>
     *
     * <p>Here <i>n</i><code>=destRegion.{@link Region#n() n()}</code> is the number of dimensions.
     * Please note that the number of dimensions of the matrices
     * (<code>src.{@link Matrix#dimCount() dimCount()}</code>
     * and <code>dest.{@link Matrix#dimCount() dimCount()}</code>) are ignored!
     * It is possible, because {@link Matrix#index(long... coordinates)} method works with any number
     * of passed coordinates: missing coordinates are supposed to be zero, extra coordinates
     * (after first <code>destRegion.{@link Region#n() n()}</code> ones) must be zero.
     *
     * <p>As you can see, the number of elements of <code>shifts</code> also can differ from the number of dimensions.
     * All missing elements of <code>shifts</code> array are supposed to be zero.
     *
     * <p>The <code>context</code> argument is used as in
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method,
     * but <b>without multithreading</b>. Namely, if <code>context</code> is not {@code null},
     * this method periodically calls its {@link ArrayContext#updateProgress updateProgress} and
     * {@link ArrayContext#checkInterruption checkInterruption} methods.
     *
     * <p>Warning: this method (as well as the loop listed above)
     * <b>can be non-atomic regarding <code>IndexOutOfBoundsException</code></b>.
     * Namely, if some integer point <i>x</i>, belonging to <code>destRegion</code>,
     * lies outside <code>dest</code> matrix,
     * or the integer point <i>x</i>', obtained from it by subtracting <code>shifts</code>,
     * lies outside <code>src</code> matrix, then <code>IndexOutOfBoundsException</code> is thrown,
     * exactly as in the loop above (where {@link Matrix#index(long...)} method throws this exception).
     * But some elements can be already copied before this moment.
     *
     * <p>All other possible exceptions are checked before any other actions. Moreover,
     * if <code>destRegion</code> is {@link Hyperparallelepiped}, then the region is also fully checked
     * before starting the copying, and <code>IndexOutOfBoundsException</code> is thrown if necessary
     * in the very beginning: so, this method is atomic regarding failures for hyper-parallelepiped.
     *
     * @param context    the context of copying; can be {@code null}.
     * @param dest       the destination matrix.
     * @param src        the source matrix.
     * @param destRegion the region in the destination matrix that should be copied from the source one.
     * @param shifts     the shift between the source and destination regions.
     * @throws NullPointerException      if <code>dest</code>, <code>src</code>, <code>destRegion</code>
     *                                   or <code>shifts</code> argument is {@code null}.
     * @throws IllegalArgumentException  if the source and destination element types do not match,
     *                                   i.e. if <code>dest.{@link Matrix#elementType() elementType()}</code>
     *                                   is not equal to <code>src.{@link Matrix#elementType() elementType()}</code>
     *                                   and is not its superclass (for non-primitive element types).
     * @throws IndexOutOfBoundsException if some integer point <i>x</i>, belonging to <code>destRegion</code>,
     *                                   lies outside <code>dest</code> matrix,
     *                                   or the integer point <i>x</i>', obtained from it by
     *                                   subtracting <code>shifts</code>,  lies outside <code>src</code> matrix
     *                                   (for regions, other than {@link Matrices.Hyperparallelepiped}, can be thrown
     *                                   in the middle of working <i>after</i> copying some elements).
     * @see #copyRegion(ArrayContext, Matrix, Matrix, Region, long[], Object)
     */
    public static void copyRegion(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            Region destRegion,
            long... shifts) {
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(destRegion, "Null destRegion argument");
        Objects.requireNonNull(shifts, "Null shifts argument");
        shifts = shifts.clone();
        AbstractArray.checkCopyArguments(dest.array(), src.array());
        if (destRegion instanceof Hyperparallelepiped destParallelepiped) {
            if (!destParallelepiped.isInsideMatrix(dest)) {
                throw new IndexOutOfBoundsException("The destination region (" + destRegion
                        + ") is not inside the destination " + dest);
            }
            if (!destParallelepiped.isInsideMatrix(src, shifts)) {
                throw new IndexOutOfBoundsException("The source region (" + destRegion + ", shifted backwards by "
                        + JArrays.toString(shifts, ",", 100) +
                        ") is not inside the source " + src);
            }
            if (ArraysSubMatrixCopier.copySubMatrixRegion(
                    context, dest, src, Matrix.ContinuationMode.NONE,
                    destRegion, shifts)) {
                return;
            }
        }
        ArraysMatrixRegionCopier regionCopier = ArraysMatrixRegionCopier.getInstance(context, destRegion.n(),
                dest, src, shifts, null, true);
        regionCopier.process(destRegion);
    }

    /**
     * An extended analog of {@link #copyRegion(ArrayContext, Matrix, Matrix, Region, long...)} method,
     * allowing to copy regions which do not lie fully inside <code>dest</code> and <code>src</code> matrices.
     * Namely, instead of throwing <code>IndexOutOfBoundsException</code>,
     * attempts to read elements outside <code>src</code> matrix produce <code>outsideValue</code>,
     * and attempts to write elements outside <code>dest</code> matrix are just ignored.
     *
     * <p>For non-primitive element types, the <code>outsideValue</code> argument
     * must be some instance of the class <code>src.{@link Matrix#elementType() elementType()}</code>,
     * or its superclass, or {@code null}.
     * For primitive element types, <code>outsideValue</code> can be {@code null} or <i>any</i>
     * wrapper for primitive types: <code>Boolean</code>, <code>Byte</code>, etc. The rules of conversion
     * of this value to required primitive type are exactly the same as in
     * {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)} method,
     * the case of the {@link Matrix.ContinuationMode#getConstantMode(Object) constant continuation mode}.
     *
     * <p>More precisely, this method is equivalent to the following loop (and, of course, works faster):
     *
     * <p><code>
     * for (all coordinates <i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub><br>
     * &#32;   in ranges destRegion.{@link Region#coordRange(int)
     * coordRange}(0), destRegion.{@link Region#coordRange(int) coordRange}(1),<br>
     * &#32;   ..., destRegion.{@link Region#coordRange(int) coordRange}(<i>n</i>&minus;1))<br>
     * {<br>
     * &#32;   if (dest.{@link Matrix#inside(long...)
     * inside}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>) &amp;&amp;<br>
     * &#32;       destRegion.{@link Region#contains(long...)
     * contains}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)<br>
     * &#32;   {<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long <i>x</i>'<sub>0</sub> = <i>x</i><sub>0</sub> - (shifts.length &gt; 0 ? shifts[0] : 0);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long <i>x</i>'<sub>1</sub> = <i>x</i><sub>1</sub> - (shifts.length &gt; 1 ? shifts[1] : 0);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * ...<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long <i>x</i>'<sub><i>n</i>&minus;1</sub> = <i>x</i><sub><i>n</i>&minus;1</sub> -
     * (shifts.length &gt; <i>n</i>&minus;1 ? shifts[<i>n</i>&minus;1] : 0);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * long destIndex = dest.{@link Matrix#index(long...)
     * index}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * Object element;<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * if (src.{@link Matrix#inside(long...)
     * inside}(<i>x</i>'<sub>0</sub>, <i>x</i>'<sub>1</sub>, ..., <i>x</i>'<sub><i>n</i>&minus;1</sub>) {<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * long srcIndex = src.{@link Matrix#index(long...)
     * index}(<i>x</i>'<sub>0</sub>, <i>x</i>'<sub>1</sub>, ..., <i>x</i>'<sub><i>n</i>&minus;1</sub>);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * element = src.array().{@link Array#getElement(long) getElement}(srcIndex);<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;    } else {<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * element = outsideValue, casted by the rules of the {@link
     * Matrix.ContinuationMode#getConstantMode(Object) constant submatrix continuation mode};<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;    }<br>
     * &#32;   &nbsp;&nbsp;&nbsp;&nbsp;
     * dest.array().{@link UpdatableArray#setElement(long, Object) setElement}(destIndex, element);<br>
     * &#32;   }<br>
     * }
     * </code></p>
     *
     * @param context      the context of copying; can be {@code null}.
     * @param dest         the destination matrix.
     * @param src          the source matrix.
     * @param destRegion   the region in the destination matrix that should be copied from the source one.
     * @param shifts       the shift between the source and destination regions.
     * @param outsideValue the value used while copying elements, lying outside <code>src</code> matrix.
     * @throws NullPointerException     if <code>dest</code>, <code>src</code>, <code>destRegion</code>
     *                                  or <code>shifts</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match,
     *                                  i.e. if <code>dest.{@link Matrix#elementType() elementType()}</code>
     *                                  is not equal to <code>src.{@link Matrix#elementType() elementType()}</code>
     *                                  and is not its superclass (for non-primitive element types).
     * @throws ClassCastException       if <code>outsideValue</code> is not {@code null} and its class
     *                                  is illegal, i.e. cannot be cast to the necessary type according
     *                                  the rules specified for
     *                                  the {@link Matrix.ContinuationMode#getConstantMode(Object)
     *                                  constant submatrix continuation mode}.
     */
    public static void copyRegion(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            Region destRegion,
            long[] shifts,
            Object outsideValue) {
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(destRegion, "Null destRegion argument");
        Objects.requireNonNull(shifts, "Null shifts argument");
        shifts = shifts.clone();
        AbstractArray.checkCopyArguments(dest.array(), src.array());
        if (ArraysSubMatrixCopier.copySubMatrixRegion(
                context, dest, src, Matrix.ContinuationMode.getConstantMode(outsideValue),
                destRegion, shifts)) {
            return;
        }
        ArraysMatrixRegionCopier regionCopier = ArraysMatrixRegionCopier.getInstance(context, destRegion.n(),
                dest, src, shifts, outsideValue, false);
        regionCopier.process(destRegion);
    }

    /**
     * Fills all elements of the matrix with zero value. Equivalent to <code>{@link #fill fill}(result, o.0)</code>.
     *
     * @param result matrix to fill with zero.
     */
    public static void clear(Matrix<? extends UpdatablePArray> result) {
        fill(result, 0.0);
    }

    /**
     * Fills all elements of the matrix with the specified value. Equivalent to
     * <code>result.array().fill(value)</code>.
     *
     * @param result matrix to fill.
     * @param value  the value to be stored in all elements of the matrix.
     */
    public static void fill(Matrix<? extends UpdatablePArray> result, double value) {
        result.array().fill(value);
    }

    /**
     * Equivalent to {@link #fillRegion(ArrayContext, Matrix, Region, Object)
     * fillRegion(null, dest, destRegion, value)}.
     *
     * @param dest       the destination matrix.
     * @param destRegion the region in the destination matrix that should be filled by the specified value.
     * @param value      the value to be stored in all elements of the matrix inside the region.
     * @throws NullPointerException if <code>dest</code> or <code>destRegion</code>
     *                              argument is {@code null}.
     * @throws ClassCastException   if <code>value</code> is not {@code null} and its class is illegal, i.e.
     *                              cannot be cast to the necessary type according the rules specified
     *                              for the {@link Matrix.ContinuationMode#getConstantMode(Object)
     *                              constant submatrix continuation mode}.
     */
    public static void fillRegion(
            Matrix<? extends UpdatableArray> dest,
            Region destRegion,
            Object value) {
        fillRegion(null, dest, destRegion, value);
    }

    /**
     * Fills the specified region in <code>dest</code> AlgART matrix with the specified value.
     * Equivalent to the following call:
     * <pre>
     * {@link #copyRegion(ArrayContext, Matrix, Matrix, Region, long[], Object)
     * copyRegion}(context, dest, dest, destRegion, dest.{@link Matrix#dimensions() dimensions()}, value);
     * </pre>
     *
     * <p>(In this call, shifting by <code>dest.{@link Matrix#dimensions() dimensions()}</code> means that
     * the shifted point of the region lies fully outside the matrix, if the original point lies inside it.
     * So, all elements will be filled by the <code>value</code>.)
     *
     * <p>For non-primitive element types, the <code>value</code> argument
     * must be some instance of the class <code>src.{@link Matrix#elementType() elementType()}</code>,
     * or its superclass, or {@code null}.
     * For primitive element types, <code>value</code> can be {@code null} or <i>any</i>
     * wrapper for primitive types: <code>Boolean</code>, <code>Byte</code>, etc. The rules of conversion
     * of this value to required primitive type are exactly the same as in
     * {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)} method,
     * the case of the {@link Matrix.ContinuationMode#getConstantMode(Object) constant continuation mode}.
     *
     * @param context    the context of filling; can be {@code null}, then it will be ignored.
     * @param dest       the destination matrix.
     * @param destRegion the region in the destination matrix that should be filled by the specified value.
     * @param value      the value to be stored in all elements of the matrix inside the region.
     * @throws NullPointerException if <code>dest</code> or <code>destRegion</code>
     *                              argument is {@code null}.
     * @throws ClassCastException   if <code>value</code> is not {@code null} and its class is illegal, i.e.
     *                              cannot be cast to the necessary type according the rules specified
     *                              for the {@link Matrix.ContinuationMode#getConstantMode(Object)
     *                              constant submatrix continuation mode}.
     */
    public static void fillRegion(
            ArrayContext context,
            Matrix<? extends UpdatableArray> dest,
            Region destRegion,
            Object value) {
        copyRegion(context, dest, dest, destRegion, dest.dimensions(), value);
    }

    /**
     * Equivalent to <code>{@link #fillBoundary(Matrix, int, double) fillBoundary}(result, boundaryWidth, 0.0)</code>.
     *
     * @param result        the matrix to process.
     * @param boundaryWidth width of the boundary to fill.
     */
    public static void clearBoundary(Matrix<? extends UpdatablePArray> result, int boundaryWidth) {
        fillBoundary(result, boundaryWidth, 0.0);
    }

    /**
     * Fills the boundary <code>result</code> matrix with the given width with the specified value..
     * If <code>boundaryWidth==0</code>, does nothing.
     *
     * <p>Equivalent to
     * <pre>
     * {@link #fillOutside fillOutsideInMatrix}(
     *     result,
     *     boundaryWidth,
     *     boundaryWidth,
     *     result.dimX() - 2 * (long) boundaryWidth,
     *     result.dimY() - 2 * (long) boundaryWidth,
     *     value)
     * </pre>
     *
     * @param result        the matrix to process.
     * @param boundaryWidth width of the boundary to fill.
     * @param value         the value to be stored in all elements near the matrix boundary.
     * @throws IllegalArgumentException if <code>boundaryWidth&lt;0.</code>
     */
    public static void fillBoundary(
            Matrix<? extends UpdatablePArray> result,
            int boundaryWidth,
            double value) {
        if (boundaryWidth < 0) {
            throw new IllegalArgumentException("Negative boundaryWidth = " + boundaryWidth);
        }
        //noinspection SuspiciousNameCombination
        fillOutside(
                result,
                boundaryWidth,
                boundaryWidth,
                result.dimX() - 2 * (long) boundaryWidth,
                result.dimY() - 2 * (long) boundaryWidth,
                value);
    }

    /**
     * Fills all <code>result</code> matrix, <i>excepting</i> elements in the rectangle
     * <code>minX&le;x&lt;minX+sizeX</code>, <code>minY&le;y&lt;minY+sizeY</code>, with the specified value.
     * If <code>sizeX&le;0</code> or <code>sizeY&le;0</code>, fills all the matrix.
     *
     * @param result the matrix to process.
     * @param minX   minimal x-coordinate, which is <i>not</i> filled.
     * @param minY   minimal y-coordinate, which is <i>not</i> filled.
     * @param sizeX  width of the rectangle, which is <i>not</i> filled.
     * @param sizeY  height of the rectangle, which is <i>not</i> filled.
     * @param value  the value to be stored in all elements outside this rectangle.
     * @throws IndexOutOfBoundsException if <code>sizeX&gt;0</code>, <code>sizeY&gt;0</code> and the specified area
     *                                   is not fully inside the matrix.
     */
    public static void fillOutside(
            Matrix<? extends UpdatablePArray> result,
            long minX,
            long minY,
            long sizeX,
            long sizeY,
            double value) {
        if (sizeX <= 0 || sizeY <= 0) {
            result.array().fill(value);
            return;
        }
        long toX = minX + sizeX;
        long toY = minY + sizeY;
        final long dimX = result.dimX();
        final long dimY = result.dimY();
        if (minX != 0) {
            // if minX < 0, we throws an exception; etc.
            result.subMatrix(0, 0, minX, dimY).array().fill(value);
        }
        if (toX != dimX) {
            result.subMatrix(toX, 0, dimX, dimY).array().fill(value);
        }
        if (minY != 0) {
            result.subMatrix(0, 0, dimX, minY).array().fill(value);
        }
        if (toY != dimY) {
            result.subMatrix(0, toY, dimX, dimY).array().fill(value);
        }
    }

    /**
     * Returns the string representation for array <code>dim</code>, probably containing dimensions of some
     * matrix, like the result of {@link Matrix#dimensions()} method.
     *
     * @param dim dimensions of some matrix.
     * @return the string representations of all elements joined into one string.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public static String dimensionsToString(long[] dim) {
        return dim.length == 1 ? dim[0] + "(x1)" : JArrays.toString(dim, "x", 1000);
    }

    @SuppressWarnings("UnnecessaryBoxing")
    static Object castOutsideValue(Object outsideValue, Array array) {
        if (array instanceof PArray) {
            Number number;
            if (outsideValue == null) {
                number = 0L;
            } else if (outsideValue instanceof Boolean) {
                number = Long.valueOf((Boolean) outsideValue ? 1 : 0);
            } else if (outsideValue instanceof Character) {
                number = Long.valueOf(((Character) outsideValue).charValue());
            } else if (outsideValue instanceof Byte) {
                number = Long.valueOf(((Byte) outsideValue).byteValue() & 0xFF);
            } else if (outsideValue instanceof Short) {
                number = Long.valueOf(((Short) outsideValue).shortValue() & 0xFFFF);
            } else if (outsideValue instanceof Integer) {
                number = Long.valueOf(((Integer) outsideValue).intValue());
            } else if (outsideValue instanceof Long) {
                number = (Long) outsideValue;
            } else if (outsideValue instanceof Float) {
                number = Double.valueOf(((Float) outsideValue).floatValue());
            } else if (outsideValue instanceof Double) {
                number = (Double) outsideValue;
            } else {
                throw new ClassCastException("Cannot cast outside value from "
                        + outsideValue.getClass() + " to any primitive type");
            }
            if (array instanceof BitArray) {
                return number.doubleValue() != 0;
            } else if (array instanceof CharArray) {
                return (char) number.intValue();
            } else if (array instanceof ByteArray) {
                return number.byteValue();
            } else if (array instanceof ShortArray) {
                return number.shortValue();
            } else if (array instanceof IntArray) {
                return number.intValue();
            } else if (array instanceof LongArray) {
                return number.longValue();
            } else if (array instanceof FloatArray) {
                return number.floatValue();
            } else if (array instanceof DoubleArray) {
                return number.doubleValue();
            } else {
                throw new AssertionError("Unallowed type of built-in array: " + array.getClass());
            }
        } else {
            if (outsideValue == null) {
                return null;
            } else if (array.elementType().isAssignableFrom(outsideValue.getClass())) {
                return outsideValue;
            } else {
                throw new ClassCastException("Cannot cast outside value from "
                        + outsideValue.getClass() + " to " + array.elementType());
            }
        }
    }

    private static void checkDimensionEqualityWithInterleaving(
            Matrix<? extends PArray> interleaved,
            List<? extends Matrix<? extends PArray>> list) {
        final long[] dimensions = interleaved.dimensions();
        final long numberOfMatrices = numberOfChannels(dimensions, false);
        if (list.size() != numberOfMatrices) {
            throw new IllegalArgumentException("Number of elements in the specified list of separated matrices = "
                    + list.size() + " does not match to the first dimension " + numberOfMatrices +
                    " of the interleaved matrix " + interleaved);
        }
        if (numberOfMatrices > 0) {
            final long[] reducedDimensions = java.util.Arrays.copyOfRange(dimensions, 1, dimensions.length);
            final Matrix<?> m0 = list.get(0);
            if (!m0.dimEquals(reducedDimensions)) {
                throw new SizeMismatchException("Dimensions mismatch: the interleaved matrix is " + interleaved +
                        ", the separated matrices in the list are " + dimensionsToString(m0.dimensions()) +
                        ", but their dimensions must be equal to the highest " + reducedDimensions.length +
                        " of all " + dimensions.length + " dimensions of the interleaved matrix");
            }
            if (m0.elementType() != interleaved.elementType()) {
                throw new IllegalArgumentException("Different element type: " +
                        m0.elementType() + " in the separated matrices, " +
                        interleaved.elementType() + " in the interleaved matrix");
            }
        }
    }

    private static long numberOfChannels(long[] dimensions, boolean lastDimension) {
        if (dimensions.length <= 1) {
            throw new IllegalStateException("The matrix must have at least 2 dimensions");
        }
        final long n = lastDimension ? dimensions[dimensions.length - 1] : dimensions[0];
        assert n >= 0 : "illegal Matrix.dimensions() behavior: negative dimension";
        return n;
    }

    private static int numberOfChannels(long[] dimensions, boolean lastDimension, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Zero or negative limit " + limit);
        }
        final long n = numberOfChannels(dimensions, lastDimension);
        if (n > limit) {
            throw new IllegalArgumentException("Too large number of result matrices: "
                    + n + " > allowed limit " + limit);
        }
        assert n == (int) n : "n must be <= 31-bit limit " + limit;
        return (int) n;
    }

    private static IRange[] coordRangesOfVertices(double[][] vertices, int requiredDimCount) {
        Objects.requireNonNull(vertices, "Null vertices array");
        if (vertices.length == 0) {
            throw new IllegalArgumentException("No vertices are specified");
        }
        for (int k = 0; k < vertices.length; k++) {
            Objects.requireNonNull(vertices[k], "Null vertices[" + k + "]");
            if (vertices[k].length == 0) {
                throw new IllegalArgumentException("Empty vertices[" + k + "]: 0-dimensional points are not allowed");
            }
            if (requiredDimCount > 0 && vertices[k].length != requiredDimCount) {
                throw new IllegalArgumentException("The vertex #" + k + " is " + vertices[k].length
                        + "-dimensional, but only " + requiredDimCount + "-dimensional vertices are allowed");
            }
            if (vertices[k].length != vertices[0].length) {
                throw new IllegalArgumentException("Different number of dimensions in the vertex #" + k + " ("
                        + vertices[k].length + "-dimensional) and the vertex #0 (" + vertices[0].length +
                        "-dimensional");
            }
        }
        IRange[] result = new IRange[vertices[0].length];
        for (int j = 0; j < result.length; j++) {
            result[j] = IRange.of(
                    (long) StrictMath.floor(vertices[0][j]),
                    (long) StrictMath.ceil(vertices[0][j]));
        }
        for (int k = 1; k < vertices.length; k++) {
            for (int j = 0; j < result.length; j++) {
                result[j] = IRange.of(
                        Math.min(result[j].min(), (long) StrictMath.floor(vertices[k][j])),
                        Math.max(result[j].max(), (long) StrictMath.ceil(vertices[k][j])));
            }
        }
        return result;
    }

    private static boolean buildSimplexByVertices(double[][] vertices, double[] a, double[] b) {
        Objects.requireNonNull(vertices, "Null vertices array");
        if (vertices.length == 0) {
            throw new IllegalArgumentException("No vertices are specified");
        }
        final int n = vertices[0].length; // number of dimensions
        if (vertices.length != n + 1) {
            throw new IllegalArgumentException("Illegal number of vertices " + vertices.length + ": the "
                    + (n == 2 ? "triangle" : n == 3 ? "tetrahedron" : n + "-dimensional simplex")
                    + " must be defined by " + (n + 1) + " vertices");
        }
        if ((long) vertices.length * (long) n != a.length) // possible in overflow case only
        {
            throw new OutOfMemoryError("Too large A matrix");
        }
        if (vertices.length != b.length) // possible in overflow case only
        {
            throw new OutOfMemoryError("Too large b vector");
        }
        boolean result = true;
        double[] minorMatrix = new double[n * n];
        for (int k = 0, aOfs = 0; k < vertices.length; k++, aOfs += n) {
            double[] vertex = vertices[k];
            if (vertex.length != n) // extra check for this class
            {
                throw new IllegalArgumentException("Different number of dimensions in the vertex #" + k + " ("
                        + vertex.length + "-dimensional) and the vertex #0 (" + n + "-dimensional");
            }
            // Let's consider the hyperfacet opposite to the vertex #i.
            // The equation set for this hyperfacet is (m=n-1):
            //     |   x0   x1 ...   xm 1 |
            //     | v0x0 v0x1 ... v0xm 1 |
            //     | v1x0 v1x1 ... v1xm 1 | = 0
            //     |     . . .            |
            //     | vmx0 vmx1 ... vmxm 1 |
            // where v0,...,vm is all vertices excepting this one (#i), or
            //     a0*x0 + a1*x1 + ... + am*xm = b,
            // where a0, a1, ... and -b (not b!) are minors of this determinant.
            for (int minorIndex = 0; minorIndex <= n; minorIndex++) {
                for (int i = 0, vIndex = 0, minorOfs = 0; i < n; i++, vIndex++) {
                    if (vIndex == k) {
                        vIndex++; // all vertices besides the current
                    }
                    double[] v = vertices[vIndex];
                    for (int j = 0, vOfs = 0; j < n; j++) {
                        if (j != minorIndex) {
                            minorMatrix[minorOfs++] = v[vOfs];
                        }
                        vOfs++;
                    }
                    if (minorIndex < n) {
                        minorMatrix[minorOfs++] = 1.0;
                    }
                }
                double minorValue = (minorIndex & 1) == 0 ?
                        determinant(n, minorMatrix) :
                        -determinant(n, minorMatrix);
                if (minorIndex < n) {
                    a[aOfs + minorIndex] = minorValue;
                } else {
                    b[k] = -minorValue;
                }
            }
            // Finding the sign of the inequality: we must have the inequality
            //     a0*x0 + a1*x1 + ... + am*xm <= b
            double sum = 0.0;
            for (int j = 0; j < n; j++) {
                sum += a[aOfs + j] * vertex[j];
            }
            if (sum == b[k]) { // the vertex lies at the hyperplane: the degeneration case
                result = false;
            } else if (sum > b[k]) { // we should invert the sign, because the given vertex must fulfil the inequality
                for (int j = 0; j < n; j++) {
                    a[aOfs + j] = -a[aOfs + j];
                }
                b[k] = -b[k];
            }
        }
        return result;
    }

    private static double determinant(int n, double... a) {
        assert a.length == n * n;
        if (n <= 0) {
            throw new IllegalArgumentException("Zero or negative matrix size");
        }
        if (n == 1) {
            return a[0];
        }
        if (n == 2) {
            return a[0] * a[3] - a[1] * a[2];
        }
        double[] minor = new double[(n - 1) * (n - 1)];
        double sum = 0.0;
        for (int minorIndex = 0; minorIndex < n; minorIndex++) {
            for (int i = 1, aOfs = n, minorOfs = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (j != minorIndex) {
                        minor[minorOfs++] = a[aOfs];
                    }
                    aOfs++;
                }
            }
            if ((minorIndex & 1) == 0) {
                sum += a[minorIndex] * determinant(n - 1, minor);
            } else {
                sum -= a[minorIndex] * determinant(n - 1, minor);
            }
        }
        return sum;
    }
}
