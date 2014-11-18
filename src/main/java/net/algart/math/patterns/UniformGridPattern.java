/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.Point;

import java.util.Collection;
import java.util.Set;

/**
 * <p>Interface, used by {@link Pattern} implementations to indicate that
 * they are <i>uniform-grid patterns</i>, i&#46;e&#46;
 * subsets of the set of all mesh nodes of some uniform grids.
 * See also the section "Uniform-grid patterns" in the comments to {@link Pattern} interface.</p>
 *
 * <p>More precisely, a pattern, implementing this interface, is some set of <i>N</i> points (<i>N</i>&gt;0)
 * <nobr><b>x</b><sup>(<i>k</i>)</sup> = (<i>x</i><sub>0</sub><sup>(<i>k</i>)</sup>,
 * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup>, ...,
 * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>)</nobr>
 * (<i>k</i>=0,1,...,<i>N</i>&minus;1, <i>n</i>={@link #dimCount() dimCount()}),
 * produced by the following formulas:
 *
 * <blockquote>
 * <i>x</i><sub>0</sub><sup>(<i>k</i>)</sup> =
 * <i>o</i><sub>0</sub> + <i>i</i><sub>0</sub><sup>(<i>k</i>)</sup><i>d</i><sub>0</sub><br>
 * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup> =
 * <i>o</i><sub>1</sub> + <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup><i>d</i><sub>1</sub><br>
 * . . .<br>
 * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
 * <i>o</i><sub><i>n</i>&minus;1</sub>
 * + <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup><i>d</i><sub><i>n</i>&minus;1</sub>
 * </blockquote>
 *
 * <p>where <i>o</i><sub><i>j</i></sub> and <i>d</i><sub><i>j</i></sub>
 * are some constants (<i>d</i><sub><i>j</i></sub>&gt;0)
 * and <i>i</i><sub><i>j</i></sub><sup>(<i>k</i>)</sup> are any integer numbers.
 * The point
 * <b>o</b>=(<i>o</i><sub>0</sub>,<i>o</i><sub>1</sub>,...,<i>o</i><sub><i>n</i>&minus;1</sub>),
 * named <i>origin</i> of the grid, and the vector
 * <b>d</b>=(<i>d</i><sub>0</sub>,<i>d</i><sub>1</sub>,...,<i>d</i><sub><i>n</i>&minus;1</sub>),
 * named <i>steps</i> of the grid, are specified while creating the pattern.
 * Moreover, these parameters about (<b>o</b> and <b>d</b>)
 * <b>are stored inside the object and can be quickly read at any time
 * by {@link #originOfGrid()} and {@link #stepsOfGrid()} methods</b>
 * &mdash; this condition is a requirement for all implementations of this interface.</p>
 *
 * <p>The numbers <i>i</i><sub><i>j</i></sub><sup>(<i>k</i>)</sup> are called <i>grid indexes</i>
 * of the points of the pattern (or, briefly, <i>grid indexes</i> of the pattern).
 * The integer points
 * <nobr><b>i</b><sup>(<i>k</i>)</sup> = (<i>i</i><sub>0</sub><sup>(<i>k</i>)</sup>,
 * <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup>, ...,
 * <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>)</nobr>
 * (<i>k</i>=0,1,...,<i>N</i>&minus;1)
 * form an integer pattern (see the section "Integer patterns" in the comments to {@link Pattern} interface),
 * which is called <i>grid index pattern</i> and can be got at any time by
 * {@link #gridIndexPattern()} method. If the number of points is not extremely large, the grid indexes
 * can be also retrieved directly as Java set of <b>i</b><sup>(<i>k</i>)</sup> points
 * by {@link #gridIndexes()} method.</p>
 *
 * <p>Warning: not only patterns, implementing this interface, are actually such sets of points.
 * Moreover, many patterns, created by this package, are really uniform grid
 * (all their points really belong to set of mesh points of some uniform grid),
 * but they do not implement this interface and are not considered to be "uniform-grid".
 * The typical examples are Minkowski sums, created by
 * {@link Patterns#newMinkowskiSum(Collection)} method,
 * and unions, created by {@link Patterns#newUnion(Collection)} method.
 * It is obvious that a Minkowski sum or a union of several uniform-grid patterns, having
 * zero origin <b>o</b>=(0,0,...,0) and the same steps <b>d</b>, are also uniform-grid patterns
 * with the same origin and steps. However, it is very probably that the objects, returned by
 * {@link Patterns#newMinkowskiSum(Collection)} and {@link Patterns#newUnion(Collection)} methods,
 * will not implement {@link UniformGridPattern} interface even in this "good" case.</p>
 *
 * <h4>Grid index restrictions</h4>
 *
 * <p>There are the following guarantees for grid indexes <i>i</i><sub><i>j</i></sub><sup>(<i>k</i>)</sup>
 * of any uniform-grid pattern:</p>
 *
 * <ol>
 * <li>if <b>p</b>=(<i>i</i><sub>0</sub>,<i>i</i><sub>1</sub>,...,<i>i</i><sub><i>n</i>&minus;1</sub>) is
 * the grid index of some point of the pattern, then
 * &minus;{@link #MAX_COORDINATE}&le;<i>i</i><sub><i>j</i></sub>&le;{@link #MAX_COORDINATE}
 * for all <i>j</i>;</li>
 * <li>if <b>p</b>=(<i>i</i><sub>0</sub><sup>1</sup>,<i>i</i><sub>1</sub><sup>1</sup>,...,<i>i</i><sub
 * ><i>n</i>&minus;1</sub><sup>1</sup>) and
 * <b>q</b>=(<i>i</i><sub>0</sub><sup>2</sup>,<i>i</i><sub>1</sub><sup>2</sup>,...,<i>i</i><sub
 * ><i>n</i>&minus;1</sub><sup>2</sup>)
 * are the grid indexes of some two points of the pattern, then
 * |<i>i</i><sub><i>j</i></sub><sup>1</sup>&minus;<i>i</i><sub><i>j</i></sub><sup>2</sup>|&le;{@link
 * #MAX_COORDINATE} for all <i>j</i>.</li>
 * </ol>
 *
 * <p>Each implementation of this interface <i>must</i> fulfil both restriction.
 * Any attempt to create a uniform-grid pattern, the grid indexes of which do not satisfy these restrictions,
 * leads to {@link TooLargePatternCoordinatesException}.</p>
 *
 * <p>These restrictions are guaranteed <i>together</i> with the coordinate restrictions,
 * described in the section "Coordinate restrictions" in the comments to {@link Pattern} interface.
 * These restrictions provide a guarantee that the method {@link #gridIndexPattern()} always
 * works successfully and creates a correct integer pattern.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UniformGridPattern extends Pattern {
    /**
     * Returns the grid <i>origin</i> <b>o</b> of this pattern.
     * See the {@link UniformGridPattern comments to this interface} for more details.
     *
     * <p>There is a guarantee, that this method always works very quickly
     * (<i>O</i>({@link #dimCount() dimCount()}) operations) and without exceptions.
     *
     * @return the origin <b>o</b> of the uniform grid of this pattern.
     */
    public Point originOfGrid();

    /**
     * Returns the array of grid <i>steps</i> <b>d</b> of this pattern.
     * The length of the returned array is equal to {@link #dimCount() dimCount()},
     * and the element #<i>j</i> contains the grid step <i>d</i><sub><i>j</i></sub> along the coordinate #<i>j</i>.
     * See the {@link UniformGridPattern comments to this interface} for more details.
     *
     * <p>The returned array is a clone of the internal array of the steps, stored in this object.
     * The returned array is never empty (its length cannot be zero).
     * The elements of the returned array are always positive (<tt>&lt;0.0</tt>).
     *
     * <p>There is a guarantee, that this method always works very quickly
     * (<i>O</i>({@link #dimCount() dimCount()}) operations) and without exceptions.
     *
     * @return an array containing all grid steps of this pattern.
     */
    public double[] stepsOfGrid();

    /**
     * Returns the grid <i>step</i> <i>d</i><sub><i>j</i></sub> along the coordinate #<i>j</i> of this pattern
     * along the coordinate #<i>j</i>=<tt>coordIndex</tt>.
     * Equivalent to <tt>{@link #stepsOfGrid()}[coordIndex]</tt>, but works faster.
     *
     * <p>There is a guarantee, that this method always works very quickly
     * (maximally <i>O</i>({@link #dimCount() dimCount()}) operations) and without exceptions.
     *
     * @return the grid step of this pattern along the specified coordinate axis.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or
     *                                   <tt>coordIndex&gt;={@link #dimCount() dimCount()}</tt>.
     */
    public double stepOfGrid(int coordIndex);

    /**
     * Indicates whether the other uniform-grid pattern has the same grid steps.
     * In other words, returns <tt>true</tt> if and only if
     * both patterns have the same dimension count ({@link #dimCount() dimCount()})
     * and the corresponding grid steps {@link #stepOfGrid(int) stepOfGrid((k)} are equal for every <tt>k</tt>.
     *
     * <p>Note: this method does not compare the origin of grid.
     *
     * <p>Equality of grid steps is important, for example, while calculation of a Minkowski sum
     * of this and another patterns by {@link #minkowskiAdd(Pattern)} method.
     * If two uniform-grid patterns have identical grid steps, then a Minkowski sum of them
     * can be also represented by uniform-grid pattern (with same grid steps).
     * In other case, it is usually impossible &mdash; the Minkowski sum, returned by
     * {@link #minkowskiAdd(Pattern)}, will not implement {@link UniformGridPattern}.
     *
     * @param pattern another uniform-grid pattern,
     *                the grid steps of which should be compared with grid steps of this one.
     * @return <tt>true</tt> if the specified pattern has the same steps of grid.
     */
    public boolean stepsOfGridEqual(UniformGridPattern pattern);

    /**
     * Returns a set of all <i>grid indexes</i> <i>i</i><sub><i>j</i></sub> of this pattern.
     * Namely, the elements of the returned set contain grid indexes
     * <nobr><b>i</b><sup>(<i>k</i>)</sup> = (<i>i</i><sub>0</sub><sup>(<i>k</i>)</sup>,
     * <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup>, ...,
     * <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>)</nobr>
     * of all points <nobr><b>x</b><sup>(<i>k</i>)</sup> = (<i>x</i><sub>0</sub><sup>(<i>k</i>)</sup>,
     * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup>, ...,
     * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>)</nobr>
     * of this pattern:
     * <blockquote>
     * <i>x</i><sub>0</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>0</sub> + <i>i</i><sub>0</sub><sup>(<i>k</i>)</sup><i>d</i><sub>0</sub><br>
     * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>1</sub> + <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup><i>d</i><sub>1</sub><br>
     * . . .<br>
     * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub><i>n</i>&minus;1</sub>
     * + <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup><i>d</i><sub><i>n</i>&minus;1</sub>
     * </blockquote>
     *
     * <p>The result of this method is immutable (<tt>Collections.unmodifiableSet</tt>).
     * Moreover, the result is always the same for different calls of this method for the same instance &mdash;
     * there are no ways to change it, in particular, via any custom methods of the implementation class
     * (it is a conclusion from the common requirement, that all implementations of {@link Pattern} interface must be
     * immutable).
     *
     * <p>The returned set is always non-empty,
     * and the number of its elements is always equal to {@link #pointCount()}.
     *
     * <p><b>Warning!</b> This method can work slowly for some forms of large patterns.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <tt>OutOfMemoryError</tt>.
     * This method surely fails (throws one of these exception), if the total number of points
     * <tt>{@link #pointCount()}&gt;Integer.MAX_VALUE</tt>, because Java <tt>Set</tt> object
     * cannot contain more than <tt>Integer.MAX_VALUE</tt> elements.
     *
     * <p>For example, implementations of the {@link RectangularPattern rectangular patterns}
     * allow to successfully define a very large 3D parallelepiped
     * <nobr><i>n</i> x <i>n</i> x <i>n</i></nobr>.
     * Fur such pattern, this method will require a lot of memory
     * for <i>n</i>=1000 and will fail (probably with {@link TooManyPointsInPatternError})
     * for <i>n</i>=2000 (2000<sup>3</sup>&gt;<tt>Integer.MAX_VALUE</tt>).
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) operations and memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>Note: if you do not really need to get a Java collection of all grid indexes,
     * you can use {@link #gridIndexPattern()} method, which returns the same result in a form
     * of another (integer) pattern. That method, unlike this one, never spends extreme amount of memory
     * and time and has no risk to fail with {@link TooManyPointsInPatternError} / <tt>OutOfMemoryError</tt>.
     *
     * @return all grid indexes of this pattern.
     * @throws TooManyPointsInPatternError if the number of points is greater than <tt>Integer.MAX_VALUE</tt> or,
     *                                     in some rare situations, is near this limit
     *                                     (<tt>OutOfMemoryError</tt> can be also thrown instead of this exception).
     * @see #gridIndexPattern()
     */
    public Set<IPoint> gridIndexes();

    /**
     * Returns the minimal and maximal <i>grid index</i> <i>i</i><sub><i>j</i></sub>
     * among all points of this pattern
     * for the specified coordinate index <i>j</i>==<tt>coordIndex</tt>.
     * The minimal grid index will be <tt>r.{@link net.algart.math.IRange#min() min()}</tt>,
     * the maximal grid index will be <tt>r.{@link net.algart.math.IRange#max() max()}</tt>,
     * where <tt>r</tt> is the result of this method.
     * See the {@link UniformGridPattern comments to this interface} for more details.
     *
     * <p>There is a guarantee, that if this object implements {@link RectangularPattern} interface,
     * then this method works very quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * <p>Moreover, all patterns, implemented in this package, have very quick implementations of this method
     * (<i>O</i>(1) operations). Also, the implementations of this method in this package never throw exceptions.
     *
     * <p>It is theoretically possible, that in custom implementations of this interface
     * (outside this package) this method will work slowly, up to <i>O</i>(<i>N</i>) operations,
     * <i>N</i> is the number of points in this pattern.
     * However, even in such implementations this method <i>must not</i> lead to
     * {@link TooManyPointsInPatternError} / <tt>OutOfMemoryError</tt>, like {@link #points()} method.
     *
     * @param coordIndex the index <i>j</i> of the coordinate (0 for <i>x</i>, 1 for <i>y</i>, 2 for <i>z</i>, etc.).
     * @return the range from minimal to maximal grid index <i>i</i><sub><i>j</i></sub>.
     * @throws IndexOutOfBoundsException if <tt>coordIndex&lt;0</tt> or
     *                                   <tt>coordIndex&gt;={@link #dimCount() dimCount()}</tt>.
     * @see #gridIndexMin()
     * @see #gridIndexMax()
     * @see #gridIndexArea()
     */
    public IRange gridIndexRange(int coordIndex);

    /**
     * Returns the minimal and maximal <i>grid index</i> <i>i</i><sub><i>j</i></sub>
     * among all points of this pattern
     * for all coordinate axes <i>j</i>
     * If <tt>a</tt> is the result of this method,
     * then <tt>a.{@link IRectangularArea#coordCount() coordCount()}=={@link #dimCount() dimCount()}</tt>
     * and <tt>a.{@link IRectangularArea#range(int) range}(k)</tt>
     * is equal to <tt>{@link #gridIndexRange(int) gridIndexRange}(k)</tt> for all <tt>k</tt>.
     *
     * <p>All, said in the comments to {@link #gridIndexRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <tt>OutOfMemoryError</tt>,
     * is also true for this method.
     *
     * @return the ranges from minimal to maximal grid index for all space dimensions.
     */
    public IRectangularArea gridIndexArea();

    /**
     * Returns the point, each coordinate #<i>j</i> of which
     * is equal to the minimal corresponding grid index <i>i</i><sub><i>j</i></sub>
     * among all points of this pattern.
     * Equivalent to <tt>{@link #gridIndexArea()}.{@link IRectangularArea#min() min()}</tt>.
     *
     * <p>All, said in the comments to {@link #gridIndexRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <tt>OutOfMemoryError</tt>,
     * is also true for this method.
     *
     * @return minimal grid index for all space dimensions as a point.
     */
    public IPoint gridIndexMin();

    /**
     * Returns the point, each coordinate #<i>j</i> of which
     * is equal to the maximal corresponding grid index <i>i</i><sub><i>j</i></sub>
     * among all points of this pattern.
     * Equivalent to <tt>{@link #gridIndexArea()}.{@link IRectangularArea#max() max()}</tt>.
     *
     * <p>All, said in the comments to {@link #gridIndexRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <tt>OutOfMemoryError</tt>,
     * is also true for this method.
     *
     * @return maximal grid index for all space dimensions as a point.
     */
    public IPoint gridIndexMax();

    /**
     * Returns <tt>true</tt> if and only if this uniform-grid pattern is an <i>ordinary integer pattern</i>,
     * i&#46;e&#46; if the grid origin <b>o</b> is the origin of coordinates (0,0,...,0)
     * and all grid steps <i>d</i><sub><i>j</i></sub> are 1.0.
     * Equivalent to
     * <tt>{@link #originOfGrid()}.{@link Point#isOrigin() isOrigin()}
     * && {@link #gridIndexRange(int) gridIndexRange}(0)==1.0
     * && {@link #gridIndexRange(int) gridIndexRange}(1)==1.0
     * &&&nbsp;...</tt>
     * Ordinary integer patterns are a simplest form of <i>integer</i> pattern:
     * see comments to {@link Pattern} interface, section "Uniform-grid patterns".
     *
     * <p>There is a guarantee, that this method always works very quickly
     * (maximally <i>O</i>({@link #dimCount() dimCount()}) operations) and without exceptions.
     *
     * @return whether the grid origin <b>o</b>=(0,0,...,0) and also all grid steps <i>d</i><sub><i>j</i></sub>=1.0.
     */
    public boolean isOrdinary();

    /**
     * Returns <tt>true</tt> if this pattern is <i>n</i>-dimensional rectangular parallelepiped.
     * (For 2D patterns it means a rectangle, for 1D pattern it means an interval.)
     * In other words, it returns <tt>true</tt> if this pattern is the set of all points
     * <nobr>(<i>o</i><sub>0</sub>+<i>i</i><sub>0</sub><i>d</i><sub>0</sub>,
     * <i>o</i><sub>1</sub>+<i>i</i><sub>1</sub><i>d</i><sub>1</sub>, ...,
     * <i>o</i><sub><i>n</i>&minus;1</sub>+<i>i</i><sub><i>n</i>&minus;1</sub><i>d</i><sub><i>n</i>&minus;1</sub>)</nobr>,
     * where <i>o<sub>j</sub></i> are coordinates of the {@link #originOfGrid() origin of the grid},
     * <i>d<sub>j</sub></i> are {@link #stepsOfGrid() steps of the grid}
     * and <i>i<sub>j</sub></i> are all integers in the ranges
     * <nobr>{@link #gridIndexRange(int) gridIndexRange(<i>j</i>)}.{@link net.algart.math.IRange#min()
     * min()}&lt;=<i>i</i><sub><i>j</i></sub>&lt;={@link #gridIndexRange(int)
     * gridIndexRange(<i>j</i>)}.{@link net.algart.math.IRange#max()
     * max()}</nobr>, <nobr><i>j</i>=0,1,...,{@link #dimCount() dimCount()}&minus;1</nobr>.</p>
     *
     * <p>Note that this condition is the same as in the definition of rectangular patterns, represented by
     * {@link RectangularPattern} interface. Really, if the object implements {@link RectangularPattern},
     * this method always returns <tt>true</tt>. However, this method tries to investigate
     * the actual point set for other types of patterns.
     *
     * <p>There are no strict guarantees that this method <i>always</i> returns <tt>true</tt> if the pattern is
     * <i>n</i>-dimensional rectangular parallelepiped. (In some complex situations, such analysis can
     * be too difficult.) But there is this guarantee for all uniform-grid patterns, created by this package.
     * And, of course, there is the reverse guarantee: if this method returns <tt>true</tt>, the pattern is
     * really a rectangular parallelepiped.</p>
     *
     * <p>You may be also sure that this method always works quickly enough and without exceptions.
     * In the worst case, it can require <i>O</i>(<i>N</i>) operations,
     * <i>N</i>={@link #pointCount() pointCount()},
     * but usually it works much more quickly.
     * (So, if you implement this method yourself and there is a risk, that calculations
     * can lead to {@link TooManyPointsInPatternError}, <tt>OutOfMemory</tt> or another exception due to
     * extremely large number of points, you <i>must</i> return <tt>false</tt> instead of
     * throwing an exception. Please compare this with {@link #pointCount()} and {@link #points()} methods,
     * which do not provide such guarantees and <i>may</i> lead to an exception.)
     *
     * @return <tt>true</tt> if this pattern is <i>n</i>-dimensional rectangular parallelepiped.
     */
    public boolean isActuallyRectangular();

    /**
     * Returns an {@link #isOrdinary() ordinary} integer pattern with the same set of <i>grid indexes</i>
     * <i>i</i><sub><i>j</i></sub><sup>(<i>k</i>)</sup> as this pattern.
     * In other words, if this pattern is a set of points
     *
     * <blockquote>
     * <i>x</i><sub>0</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>0</sub> + <i>i</i><sub>0</sub><sup>(<i>k</i>)</sup><i>d</i><sub>0</sub><br>
     * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>1</sub> + <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup><i>d</i><sub>1</sub><br>
     * . . .<br>
     * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub><i>n</i>&minus;1</sub>
     * + <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup><i>d</i><sub><i>n</i>&minus;1</sub>
     * </blockquote>
     *
     * <p>(<i>k</i>=0,1,...,<i>N</i>&minus;1, <i>n</i>={@link #dimCount() dimCount()}), then the returned pattern
     * consists of points
     *
     * <blockquote>
     * <i>y</i><sub>0</sub><sup>(<i>k</i>)</sup> = <tt>(double)</tt><i>i</i><sub>0</sub><sup>(<i>k</i>)</sup><br>
     * <i>y</i><sub>1</sub><sup>(<i>k</i>)</sup> = <tt>(double)</tt><i>i</i><sub>1</sub><sup>(<i>k</i>)</sup><br>
     * . . .<br>
     * <i>y</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
     * <tt>(double)</tt><i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>
     * </blockquote>
     *
     * <p>Note: here is a guarantee, that all grid indexes <i>i</i><sub><i>j</i></sub> will be strictly
     * represented by <tt>double</tt> type.
     * Moreover, there is a guarantee that the returned pattern is correct, i.e. will be successfully built
     * without a risk of {@link TooLargePatternCoordinatesException}.
     * See the comments to {@link Pattern#MAX_COORDINATE}
     * and the section "Grid index restrictions" in the comments to {@link UniformGridPattern} interface.
     *
     * <p>You can use this method to get a set of all grid indexes (integer values):
     * it is enough to call {@link #roundedPoints()} in the returned pattern.
     * The results will be the same as the result of {@link #gridIndexes()} method.
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}.
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     *
     * <p>This method works quickly enough: in the worst case,
     * it can require <i>O</i>(<i>N</i>) operations (<i>N</i>={@link #pointCount() pointCount()}).
     * <!--
     * It is really so in our implementation of DirectPointSetUniformGridPattern:
     * we do not eliminate checking of all coordinates -->
     *
     * @return an ordinary integer pattern, consisting of all grid indexes of this pattern (represented
     *         by <tt>double</tt> values).
     * @see #gridIndexes()
     */
    public UniformGridPattern gridIndexPattern();

    /**
     * Returns another uniform-grid pattern, identical to this one with the only difference, that
     * the grid index
     * <nobr><b>i</b><sup>(<i>k</i>)</sup> = (<i>i</i><sub>0</sub><sup>(<i>k</i>)</sup>,
     * <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup>, ...,
     * <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>)</nobr>
     * for each point #<i>k</i> of the result is shifted by the argument of this method via the call
     * <b>i</b><sup>(<i>k</i>)</sup><tt>.{@link IPoint#add(IPoint) add}(shift)</tt>.
     *
     * In other words, if this pattern is a set of points
     *
     * <blockquote>
     * <i>x</i><sub>0</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>0</sub> + <i>i</i><sub>0</sub><sup>(<i>k</i>)</sup><i>d</i><sub>0</sub><br>
     * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>1</sub> + <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup><i>d</i><sub>1</sub><br>
     * . . .<br>
     * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub><i>n</i>&minus;1</sub>
     * + <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup><i>d</i><sub><i>n</i>&minus;1</sub>
     * </blockquote>
     *
     * <p>(<i>k</i>=0,1,...,<i>N</i>&minus;1, <i>n</i>={@link #dimCount() dimCount()}), then the returned pattern
     * has the same {@link #originOfGrid() grid oridin}, the same {@link #stepsOfGrid() grid steps}
     * and consists of points
     *
     * <blockquote>
     * <i>y</i><sub>0</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>0</sub> + (<i>i</i><sub>0</sub><sup>(<i>k</i>)</sup>+<tt>shift.{@link IPoint#coord(int)
     * coord}(0)</tt>)*<i>d</i><sub>0</sub><br>
     * <i>y</i><sub>1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub>1</sub> + (<i>i</i><sub>1</sub><sup>(<i>k</i>)</sup>+<tt>shift.{@link IPoint#coord(int)
     * coord}(1)</tt>)*<i>d</i><sub>1</sub><br>
     * . . .<br>
     * <i>y</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
     * <i>o</i><sub><i>n</i>&minus;1</sub>
     * + (<i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>+<tt>shift.{@link IPoint#coord(int)
     * coord}(<i>n</i>&minus;1)</tt>)*<i>d</i><sub><i>n</i>&minus;1</sub>
     * </blockquote>
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     * For comparison, an attempt to do the same operation via getting all grid indexes via
     * {@link #gridIndexes()} call, correcting them and forming a new pattern via
     * {@link Patterns#newUniformGridPattern(Point, double[], java.util.Collection)}
     * will lead to {@link TooManyPointsInPatternError} / <tt>OutOfMemoryError</tt> for some forms of large patterns.
     *
     * <p>Warning: this method can fail with {@link TooLargePatternCoordinatesException}, if some of new points
     * violate restrictions, described in the comments to {@link Pattern} interface,
     * section "Coordinate restrictions", and in the comments to {@link UniformGridPattern} interface,
     * section "Coordinate restrictions" (for example, due to very large shift).
     *
     * <p>Note: the similar results can be got with help of {@link #shift(Point)} method
     * with a corresponding floating-point shift. However, this method
     * guarantees that the returned pattern has the same origin of the grid, but corrected <i>grid indexes</i>.
     * Unlike this, a good implementation of {@link #shift(Point)} method just corrects the grid origin,
     * but does not change grid indexes. This difference is important, if you are going to get
     * the grid indexes from the shifted pattern via {@link #gridIndexPattern()} or {@link #gridIndexes()} method.
     *
     * @param shift the shift of the grid indexes.
     * @return the shifted pattern.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link Point#coordCount() coordCount()}!={@link #dimCount()}</tt>.
     * @throws TooLargePatternCoordinatesException
     *                                  if the set of shifted points does not fulfil the restrictions,
     *                                  described in the comments to {@link Pattern} interface,
     *                                  section "Coordinate restrictions", and in the comments
     *                                  to {@link UniformGridPattern} interface,
     *                                  section "Coordinate restrictions".
     */
    public UniformGridPattern shiftGridIndexes(IPoint shift);

    /*Repeat()
        min(?!(us|g)) ==> max;;
        less ==> greater;;
        lower ==> upper;;
        lowerSurface ==> upperSurface;;
        backward ==> forward;;
        leftward ==> rightward;;
        decreasing ==> increasing;;
        &minus;<i>d</i> ==> +<i>d</i>;;
        minBound ==> maxBound;;
        \*[ \t]+(\*) ==> $1 */

    /**
     * Returns the <i>lower boundary</i> of this pattern along the given axis:
     * a pattern consisting of all such points <i>A</i> of this pattern,
     * that the neighbour point <i>B</i>,
     * generated by the backward shift of point <i>A</i> along the coordinate #<i>j</i>=<tt>coordIndex</tt>
     * by the corresponding grid step <nobr><i>d</i><sub><i>j</i></sub>={@link #stepOfGrid(int)
     * stepOfGrid(coordIndex)}</nobr>, does not belong to this pattern.
     * The number of dimensions in the resulting pattern ({@link #dimCount() dimCount()}) is the same as in this one.
     *
     * <p>In other words, the point
     * <nobr><i>A</i> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ...,
     * <i>x</i><sub><i>j</i></sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * belongs to the returned pattern if and only if it belongs to this pattern and the point
     * <nobr><i>B</i> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ...,
     * <i>x</i><sub><i>j</i></sub>&minus;<i>d</i><sub><i>j</i></sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * (corresponding to decreasing the <i>grid index</i> <i>i</i><sub><i>j</i></sub> by 1)
     * does not belong to this pattern.
     *
     * <p>Please compare with {@link #minBound(int)} method. This method can return a pattern
     * containing more points than {@link #minBound(int)}, in particular, if this pattern contains some "holes".</p>
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>Note: if this object is not {@link DirectPointSetPattern}
     * and is not {@link RectangularPattern}, this method can work slowly for some large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <tt>OutOfMemoryError</tt>. The situation is like in {@link #points()} and {@link #roundedPoints()} method.
     * However, this situation is possible only in custom implementation of this interface &mdash;
     * all implementations, provided by this package, implement either {@link DirectPointSetPattern}
     * or {@link RectangularPattern} interface.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>There is a guarantee, that if this object implements {@link RectangularPattern} interface,
     * then this method works quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>, 1 for <i>y</i>, 2 for <i>z</i>, etc.)
     * @return the "lower boundary" of this pattern: new pattern consisting of all points of this pattern,
     *         which have no leftward neighbour along the given coordinate.
     * @throws IndexOutOfBoundsException   if <tt>coordIndex&lt;0</tt> or
     *                                     <tt>coordIndex&gt;={@link #dimCount() dimCount()}</tt>.
     * @throws TooManyPointsInPatternError (impossible for implementations, provided by this package)
     *                                     if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <tt>Integer.MAX_VALUE</tt> or,
     *                                     in some rare situations, is near this limit
     *                                     (<tt>OutOfMemoryError</tt> can be also thrown instead of this exception).
     */
    public UniformGridPattern lowerSurface(int coordIndex);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Returns the <i>upper boundary</i> of this pattern along the given axis:
     * a pattern consisting of all such points <i>A</i> of this pattern,
     * that the neighbour point <i>B</i>,
     * generated by the forward shift of point <i>A</i> along the coordinate #<i>j</i>=<tt>coordIndex</tt>
     * by the corresponding grid step <nobr><i>d</i><sub><i>j</i></sub>={@link #stepOfGrid(int)
     * stepOfGrid(coordIndex)}</nobr>, does not belong to this pattern.
     * The number of dimensions in the resulting pattern ({@link #dimCount() dimCount()}) is the same as in this one.
     *
     * <p>In other words, the point
     * <nobr><i>A</i> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ...,
     * <i>x</i><sub><i>j</i></sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * belongs to the returned pattern if and only if it belongs to this pattern and the point
     * <nobr><i>B</i> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ...,
     * <i>x</i><sub><i>j</i></sub>+<i>d</i><sub><i>j</i></sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * (corresponding to increasing the <i>grid index</i> <i>i</i><sub><i>j</i></sub> by 1)
     * does not belong to this pattern.
     *
     * <p>Please compare with {@link #maxBound(int)} method. This method can return a pattern
     * containing more points than {@link #maxBound(int)}, in particular, if this pattern contains some "holes".</p>
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>Note: if this object is not {@link DirectPointSetPattern}
     * and is not {@link RectangularPattern}, this method can work slowly for some large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <tt>OutOfMemoryError</tt>. The situation is like in {@link #points()} and {@link #roundedPoints()} method.
     * However, this situation is possible only in custom implementation of this interface &mdash;
     * all implementations, provided by this package, implement either {@link DirectPointSetPattern}
     * or {@link RectangularPattern} interface.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>There is a guarantee, that if this object implements {@link RectangularPattern} interface,
     * then this method works quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>, 1 for <i>y</i>, 2 for <i>z</i>, etc.)
     * @return the "upper boundary" of this pattern: new pattern consisting of all points of this pattern,
     *         which have no rightward neighbour along the given coordinate.
     * @throws IndexOutOfBoundsException   if <tt>coordIndex&lt;0</tt> or
     *                                     <tt>coordIndex&gt;={@link #dimCount() dimCount()}</tt>.
     * @throws TooManyPointsInPatternError (impossible for implementations, provided by this package)
     *                                     if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <tt>Integer.MAX_VALUE</tt> or,
     *                                     in some rare situations, is near this limit
     *                                     (<tt>OutOfMemoryError</tt> can be also thrown instead of this exception).
     */
    public UniformGridPattern upperSurface(int coordIndex);
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns the set-theoretical union of all patterns, returned by {@link #lowerSurface(int)}
     * {@link #upperSurface(int)} methods for all coordinates.
     * In other words, the returned pattern contains full "boundary" of this pattern.
     * The number of dimensions in the resulting pattern ({@link #dimCount() dimCount()}) is the same as in this one.
     *
     * <p>Note: if this object is not {@link DirectPointSetPattern}
     * and is not {@link RectangularPattern}, this method can work slowly for some large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <tt>OutOfMemoryError</tt>. The situation is like in {@link #points()} and {@link #roundedPoints()} method.
     * However, this situation is possible only in custom implementation of this interface &mdash;
     * all implementations, provided by this package, implement either {@link DirectPointSetPattern}
     * or {@link RectangularPattern} interface.
     *
     * @return the "boundary" of this pattern: new pattern consisting of all points of this pattern,
     *         which have no leftward or rightward neighbour along at least one coordinate.
     * @throws TooManyPointsInPatternError (impossible for implementations, provided by this package)
     *                                     if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <tt>Integer.MAX_VALUE</tt> or,
     *                                     in some rare situations, is near this limit
     *                                     (<tt>OutOfMemoryError</tt> can be also thrown instead of this exception).
     */
    public Pattern surface();

    public UniformGridPattern shift(Point shift);

    public UniformGridPattern symmetric();

    public UniformGridPattern multiply(double multiplier);

    public UniformGridPattern scale(double... multipliers);

    public UniformGridPattern projectionAlongAxis(int coordIndex);

    public UniformGridPattern minBound(int coordIndex);

    public UniformGridPattern maxBound(int coordIndex);

    public UniformGridPattern carcass();
}
