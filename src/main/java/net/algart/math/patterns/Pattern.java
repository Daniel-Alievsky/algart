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

package net.algart.math.patterns;

import java.util.Set;
import java.util.List;

import net.algart.math.*;

/**
 * <p><i>Pattern</i>: non-empty set of {@link Point real points} in multidimensional space
 * (points with real coordinates).</p>
 *
 * <p>Usually patterns are relatively little point sets: from tens to millions of points not too far from
 * the origin of coordinates. However, please note that the number of points <i>is not limited
 * by any value</i>. In particular, it can be greater than <code>Long.MAX_VALUE</code>.
 * For example, it may occur for {@link Patterns#newRectangularUniformGridPattern(Point, double[], IRange...)
 * rectangular <i>n</i>-dimensional patterns</i>}.</p>
 *
 * <p>Patterns are the arguments of many image processing filters.
 * For example, a pattern may specify the form and sizes of the aperture for a linear filter.</p>
 *
 * <h2>Integer patterns</h2>
 *
 * <p>The very important subclass among all patterns is <b><i>integer patterns</i></b>,
 * consisting of points with integer coordinates. More precisely, a pattern is called <i>integer</i>,
 * if for all pattern's points
 * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)
 * we have <i>x</i><sub><i>j</i></sub><code>==(double)(long)</code><i>x</i><sub><i>j</i></sub> for any index <i>j</i>.
 * There is the standard method {@link #round()},
 * rounding any pattern to the nearest integer pattern &mdash; the result of this method is always integer.</p>
 *
 * <p>Usually integer patterns are uniform-grid patterns (see the next section), but this condition is not absolute:
 * even a pattern, not implementing {@link UniformGridPattern} interface, is called <i>integer pattern</i>,
 * if all its points are really integer. The most popular case of integer patters is so-called
 * <i>ordinary integer patterns</i> &mdash; see below in the next section
 * "Uniform-grid patterns".</p>
 *
 * <p>You can try to investigate, whether some pattern is integer or not, by {@link #isSurelyInteger} method.</p>
 *
 * <p>Integer patterns is the basic pattern type for image processing tasks.</p>
 *
 * <p>In this package, the following methods always create integer patterns:</p>
 *
 * <ul>
 * <li>{@link Pattern#round()},</li>
 * <li>{@link Patterns#newIntegerPattern(net.algart.math.IPoint...)},</li>
 * <li>{@link Patterns#newIntegerPattern(java.util.Collection)},</li>
 * <li>{@link Patterns#newSphereIntegerPattern(net.algart.math.Point, double)},</li>
 * <li>{@link Patterns#newEllipsoidIntegerPattern(net.algart.math.Point, double...)},</li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IRange...)},</li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IPoint, net.algart.math.IPoint)}.</li>
 * </ul>
 *
 * <h2>Uniform-grid patterns</h2>
 *
 * <p>The important subclass among all patterns is <b><i>uniform-grid patterns</i></b>, represented
 * by the subinterface {@link UniformGridPattern}. Uniform-grid patterns is a pattern, all points
 * of which are mesh nodes of some uniform grids, i.e. have coordinates</p>
 *
 * <blockquote>
 * <i>x</i><sub>0</sub> = <i>o</i><sub>0</sub> + <i>i</i><sub>0</sub><i>d</i><sub>0</sub><br>
 * <i>x</i><sub>1</sub> = <i>o</i><sub>1</sub> + <i>i</i><sub>1</sub><i>d</i><sub>1</sub><br>
 * . . .<br>
 * <i>x</i><sub><i>n</i>&minus;1</sub> = <i>o</i><sub><i>n</i>&minus;1</sub>
 * + <i>i</i><sub><i>n</i>&minus;1</sub><i>d</i><sub><i>n</i>&minus;1</sub><br>
 * </blockquote>
 *
 * <p>where <i>o</i><sub><i>j</i></sub> and <i>d</i><sub><i>j</i></sub> are some constants
 * (<i>d</i><sub><i>j</i></sub>&gt;0) and <i>i</i><sub><i>j</i></sub> are any integer numbers.
 * The parameters <i>o</i><sub><i>j</i></sub> (named <i>origin</i>) and
 * <i>d</i><sub><i>j</i></sub> (named <i>steps</i>) are specified while creating the pattern,
 * and <i>they are stored inside the object and can be quickly read by the access methods
 * {@link UniformGridPattern#originOfGrid()} and {@link UniformGridPattern#stepsOfGrid()}</i>.</p>
 *
 * <p>Draw attention to the last condition! You can easily create also a pattern,
 * all points of which lie in mesh nodes of some uniform grid, but which will not "know" anything
 * about this grid and will not implement {@link UniformGridPattern} interface.
 * The simplest way to do this is the call of the constructor</p>
 *
 * <pre>    new {@link SimplePattern#SimplePattern(java.util.Collection)
 * SimplePattern}(pattern.{@link #points() points()}),</pre>
 *
 * <p>where <code>pattern</code> is a uniform-grid pattern. The resulting pattern is geometrically identical
 * to the original uniform-grid one, but it does not implement
 * {@link UniformGridPattern} and is not considered to be uniform-grid, because there are no ways
 * to get information about the grid (origin and steps).</p>
 *
 * <p>It is obvious that a uniform-grid pattern is also an <i>integer</i> pattern (see above),
 * if all numbers <i>o</i><sub><i>j</i></sub> and <i>d</i><sub><i>j</i></sub> are integer.
 * The most important particular case: all <i>o</i><sub><i>j</i></sub>=0 and
 * all <i>d</i><sub><i>j</i></sub>=1. We shall call this kind of patterns
 * <b><i>ordinary integer patterns</i></b>.</p>
 *
 * <p>In this package, uniform-grid patterns are the patterns, created by one of the following ways,
 * and only they:</p>
 *
 * <ul>
 * <li>{@link Patterns#newUniformGridPattern(net.algart.math.Point, double[], java.util.Collection)},</li>
 * <li>{@link Patterns#newIntegerPattern(net.algart.math.IPoint...)} (creates an ordinary integer pattern),</li>
 * <li>{@link Patterns#newIntegerPattern(java.util.Collection)} (creates an ordinary integer pattern),</li>
 * <li>{@link Patterns#newSphereIntegerPattern(net.algart.math.Point, double)}
 * (creates an ordinary integer pattern),</li>
 * <li>{@link Patterns#newEllipsoidIntegerPattern(net.algart.math.Point, double...)}
 * (creates an ordinary integer pattern),</li>
 * <li>{@link Patterns#newSpaceSegment Patterns.newSpaceSegment(UniformGridPattern, Func, Func, double, double)},</li>
 * <li>{@link Patterns#newRectangularUniformGridPattern(net.algart.math.Point, double[], net.algart.math.IRange...)},
 * </li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IRange...)}
 * (creates an ordinary integer pattern),</li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IPoint, net.algart.math.IPoint)}
 * (creates an ordinary integer pattern),</li>
 * </ul>
 *
 * <p>and also, in some cases (depending on the arguments), by the following methods:</p>
 *
 * <ul>
 * <li>{@link Patterns#newPattern(net.algart.math.Point...)},</li>
 * <li>{@link Patterns#newPattern(java.util.Collection)}.</li>
 * </ul>
 *
 * <h2>Direct point-set patterns</h2>
 *
 * <p>One of the most popular, basic kinds of patterns is <b><i>direct point-set patterns</i></b>,
 * represented by the subinterface {@link DirectPointSetPattern}.
 * The pattern is called <i>direct point-set</i> or, briefly, <i>direct</i>,
 * if it is internally represented as an actual set of points
 * like <code>Set&lt;{@link Point}&gt;</code>.</p>
 *
 * <p>Of course, any pattern is a set of points. The main feature of this subclass is that
 * the point-set is stored directly in a form of some collection &mdash; and, so, can be directly accessed
 * at any time via {@link #points()} or {@link #roundedPoints()} methods.
 * As a result, direct point-set pattern cannot contain more than <code>Integer.MAX_VALUE</code> points
 * (because Java <code>Set</code> object cannot contain more than <code>Integer.MAX_VALUE</code> elements).</p>
 *
 * <p>Unlike direct patterns, other forms of pattern, like rectangular or complex (see below),
 * do not actually store the set of their points, though still can build and return it by a request,
 * when you call {@link #points()} or {@link #roundedPoints()}.</p>
 *
 * <p>In this package, direct point-set patterns are the patterns,
 * created by one of the following ways, and only they:</p>
 *
 * <ul>
 * <li>{@link SimplePattern} constructor,</li>
 * <li>{@link Patterns#newPattern(net.algart.math.Point...)},</li>
 * <li>{@link Patterns#newPattern(java.util.Collection)},</li>
 * <li>{@link Patterns#newUniformGridPattern(net.algart.math.Point, double[], java.util.Collection)},</li>
 * <li>{@link Patterns#newIntegerPattern(net.algart.math.IPoint...)},</li>
 * <li>{@link Patterns#newIntegerPattern(java.util.Collection)},</li>
 * <li>{@link Patterns#newSphereIntegerPattern(net.algart.math.Point, double)},</li>
 * <li>{@link Patterns#newEllipsoidIntegerPattern(net.algart.math.Point, double...)},</li>
 * <li>{@link Patterns#newSurface(Pattern, net.algart.math.functions.Func)},</li>
 * <li>{@link Patterns#newSpaceSegment Patterns.newSpaceSegment(UniformGridPattern, Func, Func, double, double)}.</li>
 * </ul>
 *
 * <p>Direct point-set pattern may be, at the same time, uniform-grid. In this case it must implement
 * {@link DirectPointSetUniformGridPattern} interface.
 * This package provides an implementation of direct pattern, which is not uniform-grid: {@link SimplePattern}.
 * Most of other direct point-set patterns, provided by this package, are uniform-grid and
 * implement {@link DirectPointSetUniformGridPattern} interface.</p>
 *
 * <h2>Rectangular patterns</h2>
 *
 * <p>The second popular basic kind of patterns is <b><i>rectangular patterns</i></b>,
 * represented by the subinterface {@link RectangularPattern}.
 * The pattern is called <i>rectangular</i>, if it is uniform-grid (implements {@link UniformGridPattern} interface),
 * and it consists of all points inside some hyperparallelepiped, the parameters (bounds) of which were
 * specified while creating the pattern, <i>are stored inside the object and can be quickly read
 * by methods like {@link #coordRange(int)}</i>.</p>
 *
 * <p>Draw attention to the last condition! Of course, you can create also a <i>direct point-set</i> pattern,
 * consisting of all points inside some hyperparallelepiped. The simplest way to do this is
 * the call of the constructor</p>
 *
 * <pre>    new {@link SimplePattern#SimplePattern(java.util.Collection)
 * SimplePattern}(pattern.{@link #points() points()}),</pre>
 *
 * <p>where <code>pattern</code> is a rectangular pattern.
 * However, the resulting pattern is considered to be direct, but not rectangular.</p>
 *
 * <p>The main difference between direct point-set and rectangular patterns is the behaviour of methods,
 * retrieving the point set like {@link #points()}, and some methods, retrieving boundaries of the pattern,
 * like {@link UniformGridPattern#upperSurface(int)}, {@link UniformGridPattern#maxBound(int)}, etc.
 * In direct patterns, all methods always work stably, i.e. without exceptions (if the passed arguments
 * are correct), but calculation of pattern boundaries can require some time, proportional to the number
 * of points in the pattern.
 * In rectangular patterns, an attempt to get all points by {@link #points()} or {@link #roundedPoints()}
 * method can lead to {@link TooManyPointsInPatternError} or to <code>OutOfMemoryError</code>,
 * because the number of points can be extremely large (for example, 10000x10000x10000 3-dimensional parallelepiped
 * consists of 10<sup>12</sup> points); but the information about boundaries is available very quickly.
 * See the details in comments to {@link DirectPointSetPattern} and {@link RectangularPattern} interfaces.</p>
 *
 * <p>The classes of direct point-set and rectangular patterns do not intersect:
 * a direct point-set pattern cannot be rectangular, and a rectangular pattern cannot be direct.</p>
 *
 * <p>Direct point-set and rectangular pattern are the base, used in many algorithms and
 * allowing to build more specific pattern types (see below).</p>
 *
 * <p>In this package, rectangular patterns are the patterns, created by one of the following ways,
 * and only they:</p>
 *
 * <ul>
 * <li>{@link Patterns#newRectangularUniformGridPattern(net.algart.math.Point, double[], net.algart.math.IRange...)},
 * </li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IRange...)},</li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IPoint, net.algart.math.IPoint)}.</li>
 * </ul>
 *
 * <h2>Complex patterns</h2>
 *
 * <p>Besides the basic types of patterns &mdash; direct point-set and rectangular &mdash; this package
 * allows to create more complex forms of patterns. Such patterns do not actually store information
 * about the point set, but contain some rules allowing to construct this point set.
 * The typical examples are Minkowski sum of several patterns, created by
 * {@link Patterns#newMinkowskiSum(java.util.Collection)} method,
 * and the union of several patterns, created by
 * {@link Patterns#newUnion(java.util.Collection)} method.
 * An attempt to get actual information about the figure of such a pattern via its methods
 * {@link #points()}, {@link #roundedPoints()}, and even usage of the simplest methods
 * {@link #pointCount()}, {@link #largePointCount()}, {@link #isSurelyOriginPoint()}
 * can lead to very long calculations and even to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>.
 * However, such patterns can be used indirectly, usually via their decompositions into more simple patterns
 * by {@link #minkowskiDecomposition(int)} and {@link #unionDecomposition(int)} methods.
 * For example, it is possible to perform morphological dilation filter over an image
 * (see <noindex><a href="http://en.wikipedia.org/wiki/Dilation_%28morphology%29">"Dilation" article
 * in Wikipedia</a></noindex>)
 * with a very large pattern, created by {@link Patterns#newMinkowskiSum(java.util.Collection)}
 * and consisting of millions or milliards points, via sequential dilations with the Minkowski summands
 * of such a pattern, extracted by {@link #minkowskiDecomposition(int)} call.</p>
 *
 * <h2>Coordinate restrictions</h2>
 *
 * <p>There are the following guarantees for coordinates of the points of any pattern:</p>
 *
 * <ol>
 * <li>if <b>p</b>=(<i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>n</i>&minus;1</sub>) is some point
 * of the pattern, then
 * &minus;{@link #MAX_COORDINATE}&le;<i>x</i><sub><i>j</i></sub>&le;{@link #MAX_COORDINATE}
 * for all <i>j</i>; here this inequality means absolutely precise mathematical inequality;</li>
 *
 * <li>if <b>p</b>=(<i>x</i><sub>0</sub><sup>1</sup>,<i>x</i><sub>1</sub><sup>1</sup>,...,<i>x</i><sub
 * ><i>n</i>&minus;1</sub><sup>1</sup>) and
 * <b>q</b>=(<i>x</i><sub>0</sub><sup>2</sup>,<i>x</i><sub>1</sub><sup>2</sup>,...,<i>x</i><sub
 * ><i>n</i>&minus;1</sub><sup>2</sup>)
 * are some two points of the pattern, then
 * |<i>x</i><sub><i>j</i></sub><sup>1</sup>&minus;<i>x</i><sub><i>j</i></sub><sup>2</sup>|&le;{@link
 * #MAX_COORDINATE} for all <i>j</i>, where
 * |<i>x</i><sub><i>j</i></sub><sup>1</sup>&minus;<i>x</i><sub><i>j</i></sub><sup>2</sup>| means
 * <i>the absolute value of mathematically precise difference</i> (not the result of Java operators
 * <code>Math.abs(</code><i>x</i><sub><i>j</i></sub><sup>1</sup>&minus;<i>x</i><sub><i>j</i></sub><sup>2</sup><code>)</code>).
 * (This condition can be checked with help of
 * {@link Patterns#isAllowedDifference(double, double)} method.)</li>
 * </ol>
 *
 * <p>Each implementation of this interface <i>must</i> fulfil both restriction. The point sets,
 * satisfying these requirements, are called <i>allowed points sets</i> for patterns.
 * Any attempt to create a pattern, the set of points of which is not allowed,
 * leads to {@link TooLargePatternCoordinatesException}.</p>
 *
 * <p>Note: though patterns are sets of real points, their coordinates are restricted by <code>long</code>-type constant
 * {@link #MAX_COORDINATE}.</p>
 *
 * <p>Also note: uniform-grid patterns must fulfil, in addition, two similar restrictions for their grid indexes.
 * See more details in the comments to {@link UniformGridPattern} interface,
 * the section "Grid index restrictions".</p>
 *
 * <p>Below are two important theorems, following from these two restrictions.</p>
 *
 * <p><b>Theorem I.</b> If you round the coordinates of all points of a pattern, i.e. replace each pattern's point
 * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>) with a new point
 * (round(<i>x</i><sub>0</sub>), round(<i>x</i><sub>1</sub>), ...,
 * round(<i>x</i><sub><i>n</i>&minus;1</sub>)),
 * where "round(a)" means the result of <code>(double)StrictMath.round(a)</code> call,
 * then the resulting point set will also be allowed. The same statement is true for the point set,
 * consisting of precise integer points, without type cast to <code>double</code>,
 * i.e. for points (<code>StrictMath.round</code>(<i>x</i><sub>0</sub>),
 * <code>StrictMath.round</code>(<i>x</i><sub>1</sub>), ...,
 * <code>StrictMath.round</code>(<i>x</i><sub><i>n</i>&minus;1</sub>)) &mdash;
 * such mathematical point set also fulfils both restrictions 1 and 2.</p>
 *
 * <p>The proof of this is complex enough. The paper
 * <noindex><a href="http://algart.net/ru/numeric_algorithms/rounding_theorem.html"
 * >http://algart.net/ru/numeric_algorithms/rounding_theorem.html</a></noindex> (in Russian)
 * contains such proof: see the theorem of rounding and the theorem of subtraction in this paper.</p>
 *
 * <p>It means that you can freely use {@link #round()} method for any pattern:
 * it always constructs another allowed pattern,
 * both in terms of this interface and in terms in {@link UniformGridPattern},
 * and cannot throw {@link TooLargePatternCoordinatesException}.</p>
 *
 * <p><b>Theorem II.</b> If all points of a pattern are integer, i.e.
 * for all pattern's points
 * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)
 * we have <i>x</i><sub><i>j</i></sub><code>==(double)(long)</code><i>x</i><sub><i>j</i></sub> for any index <i>j</i>,
 * and (<i>X</i><sub>0</sub>,<i>X</i><sub>1</sub>,...,<i>X</i><sub><i>n</i>&minus;1</sub>)
 * is some point of this pattern, then you can subtract (using Java &ldquo;&minus;&rdquo; operator)
 * the coordinate <i>X</i><sub><i>j</i></sub> (<i>j</i> is any index)
 * from the corresponding coordinate of all points of this pattern, i.e. replace each pattern's point
 * (<i>x</i><sub>0</sub>, ..., <i>x</i><sub><i>j</i>&minus;1</sub>,
 * <i>x</i><sub><i>j</i></sub>,
 * <i>x</i><sub><i>j</i>+1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>) with
 * (<i>x</i><sub>0</sub>, ..., <i>x</i><sub><i>j</i>&minus;1</sub>,
 * <i>x</i><sub><i>j</i></sub>&#x2296;<i>X</i><sub><i>j</i></sub>,
 * <i>x</i><sub><i>j</i>+1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>),
 * and the resulting point set will also be allowed.
 * Here and below <i>a</i>&#x2296;<i>b</i> (<i>a</i> and <i>b</i> are real values of <code>double</code>
 * Java type) means the computer difference (not strict mathematical),
 * i.e. the result of execution of Java operator &ldquo;<code><i>a</i>&minus;<i>b</i></code>&rdquo;.</p>
 *
 * <p>Proof.</p>
 *
 * <p>First of all, let's remind that the computer difference <i>a</i>&#x2296;<i>b</i>, according
 * IEEE&nbsp;754 standard and Java language specification, is the nearest <code>double</code> value to
 * the precise mathematical difference <i>a</i>&minus;<i>b</i>.
 * Because all pattern's points are integer, the restriction 2 allows to state that
 * any difference <i>x</i><sub><i>j</i></sub>&minus;<i>X</i><sub><i>j</i></sub>
 * can be represented precisely by <code>double</code> type (see the comments to {@link #MAX_COORDINATE} constant).
 * So, we have
 * <i>x</i><sub><i>j</i></sub>&#x2296;<i>X</i><sub><i>j</i></sub>
 * = <i>x</i><sub><i>j</i></sub>&minus;<i>X</i><sub><i>j</i></sub>:
 * the computer difference is just a mathematical difference.</p>
 *
 * <p>Now the proof is simple.
 * If is enough to show that the restrictions will be satisfied for the coordinate index <i>j</i>.
 * The restriction 2 is obvious: (mathematical) subtracting <i>X</i><sub><i>j</i></sub> does not change
 * the (mathematical!) differences
 * |<i>x</i><sub><i>j</i></sub><sup>1</sup>&minus;<i>x</i><sub><i>j</i></sub><sup>2</sup>|.
 * The new value of this coordinate for each point will be
 * <i>x</i><sub><i>j</i></sub>&minus;<i>X</i><sub><i>j</i></sub>, where both
 * (<i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>n</i>&minus;1</sub>) and
 * (<i>X</i><sub>0</sub>,<i>X</i><sub>1</sub>,...,<i>X</i><sub><i>n</i>&minus;1</sub>) are some points of the pattern;
 * according the condition 2, this difference lies in range
 * &minus;{@link #MAX_COORDINATE}&le;<i>x</i><sub><i>j</i></sub>&minus;<i>X</i><sub><i>j</i></sub>&le;{@link
 * #MAX_COORDINATE}. In other words, the restriction 1 is also satisfied.
 * This completes the proof.</p>
 *
 * <p>Note: this proof is really correct only for patterns, consisting of integer points only.
 * The reason is that all integer coordinates, fulfilling the restriction 1, and all their differences
 * <i>x</i><sub><i>j</i></sub>&minus;<i>X</i><sub><i>j</i></sub> are represented precisely by <code>double</code>
 * Java type. If a pattern contains non-integer points, the statement of this theorem is not true.
 * For example, for 1-dimensional pattern, consisting of three points
 * <i>x</i><sub>1</sub>=2251799813685248.00 (={@link #MAX_COORDINATE}/2),
 * <i>x</i><sub>2</sub>=&minus;2251799813685248.00 (=&minus;{@link #MAX_COORDINATE}/2) and
 * <i>x</i><sub>3</sub>=&minus;2251799813685247.75 (=&minus;{@link #MAX_COORDINATE}/2+0.25), subtracting
 * the point <i>x</i><sub>3</sub> by Java &ldquo;&minus;&rdquo; operator leads to the pattern
 * <i>x</i>'<sub>1</sub>=4503599627370496.00 (={@link #MAX_COORDINATE}) (computer subtraction of <code>double</code>
 * values leads to rounding here),
 * <i>x</i>'<sub>2</sub>=&minus;0.25 and
 * <i>x</i>'<sub>3</sub>=0.0, which obviously violates the mathematically precise restriction 2:
 * |<i>x</i>'<sub>1</sub>&minus;<i>x</i>'<sub>2</sub>|&gt;{@link #MAX_COORDINATE}.</p>
 *
 * <p>As a result, there is an obvious <b>conclusion</b>. If <code>p</code> is one of the {@link #points() points} of
 * some <i>integer</i> <code>pattern</code> (see above), then the method
 * <code>pattern.{@link #shift(Point) shift}(p.{@link Point#symmetric()
 * symmetric()})</code> always works successfully and never throw {@link TooLargePatternCoordinatesException}.</p>
 *
 *
 * <h2>Note about <code>equals()</code></h2>
 * <p>
 * The <code>equals()</code> method in the classes, implementing this interface, <i>may</i> return <code>false</code>
 * for two patterns, consisting of the same point sets,
 * for example, if these patterns belong to different pattern types.
 * For example, a rectangular pattern may be considered to be non-equal
 * to a geometrically identical {@link Patterns#newMinkowskiSum(Pattern...) Minkowski sum} of several segments,
 * because the thorough comparison of these patterns can require too long time and large memory.
 * (Please consider 10000x10000x10000 3-dimensional parallelepiped, consisting of 10<sup>12</sup> points
 * with integer coordinates in range 0..9999. It is geometrically equal to Minkowski sum of 3 orthogonal
 * segments with 10000 integer points in every segment, but we have no resources to check this fact
 * via direct comparison of the point sets.)
 * However, the patterns of the same kind (for example, two rectangular patterns,
 * two {@link Patterns#newMinkowskiSum(Pattern...) Minkowski sums} or
 * two {@link Patterns#newUnion(Pattern...) unions}) are usually compared precisely.
 * In particular, there are the following guarantees:</p>
 *
 * <ul>
 * <li>if both patterns are <i>direct point-set</i> (see above),
 * then <code>equals()</code> method always returns <code>true</code>
 * for geometrically identical patterns;</li>
 *
 * <li>if both patterns are <i>rectangular</i> (see above), then, also, <code>equals()</code>
 * method always returns <code>true</code> for geometrically identical patterns;</li>
 *
 * <li>and, of course, there is the reverse guarantee, that if the <code>equals()</code> method
 * returns <code>true</code>,
 * then two patterns consists of the identical point sets.</li>
 * </ul>
 *
 * <h2>Multithreading compatibility</h2>
 *
 * <p>The classes, implementing this interface, are <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public interface Pattern {
    /**
     * The maximal possible absolute coordinate value and maximal absolute difference between the corresponding
     * coordinates for all points in a pattern.
     * See the {@link Pattern comments to this interface}, section
     * "Coordinate restrictions", for more details.
     *
     * <p>The value of this constant is <code>1L &lt;&lt; 52 = 2<sup>52</sup> = {@value} ~ Long.MAX_VALUE/2048</code>.
     *
     * <p>There is an important feature of this constant.
     * Any integer values <i>x</i> (<code>long</code> Java type) from the range
     * <code>&minus;2*{@link #MAX_COORDINATE}&le;<i>x</i>&le;2*{@link #MAX_COORDINATE}</code>, and also
     * all half-integer values <i>x</i> inside the range
     * <code>&minus;{@link #MAX_COORDINATE}&le;<i>x</i>&le;{@link #MAX_COORDINATE}</code>
     * (i.e. values <i>x</i>=<i>k</i><code>+0.5</code>, where <i>k</i> is <code>long</code>
     * integer in range <code>&minus;{@link #MAX_COORDINATE}&le;<i>k</i>&le;{@link #MAX_COORDINATE}-1</code>)
     * are represented by <code>double</code> Java type precisely, without loss of precision.
     *
     * <p>As a result, we can be sure that for any integer <code>k</code> (<code>long</code> Java type), for which
     * <code>Math.abs(k)&lt;=2*{@link #MAX_COORDINATE}</code>, the following equality is true:
     * <code>(long)(double)k==k</code>.
     *
     * <p>See also the paper <noindex><a href="http://algart.net/ru/numeric_algorithms/rounding_theorem.html"
     * >http://algart.net/ru/numeric_algorithms/rounding_theorem.html</a></noindex> (in Russian)
     * about rounding <code>double</code> values in range
     * <code>&minus;{@link #MAX_COORDINATE}&le;<i>x</i>&le;{@link #MAX_COORDINATE}</code>.
     */
    long MAX_COORDINATE = 1L << 52;


    /**
     * Returns the number of space dimensions of this pattern.
     * This value is always positive (&gt;=1).
     *
     * <p>There is a guarantee, that this method always works very quickly (<i>O</i>(1) operations)
     * and without exceptions.
     *
     * @return the number of space dimensions of this pattern.
     */
    int dimCount();

    /**
     * Returns the number of points in this pattern.
     * This value is always positive (&gt;=1).
     * If the number of points is greater than <code>Long.MAX_VALUE</code>, returns <code>Long.MAX_VALUE</code>.
     *
     * <p><b>Warning!</b> This method can work slowly for some forms of large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points (result of this method).
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>.
     *
     * <p>There is a guarantee, that if this object implements {@link QuickPointCountPattern} interface,
     * then this method works very quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then the result of this method is not greater than <code>Integer.MAX_VALUE</code>.
     *
     * <p>Note: if this method returns some value greater than <code>Integer.MAX_VALUE</code>,
     * it means that you cannot use {@link #points()} and {@link #roundedPoints()} methods,
     * because Java <code>Set</code> object cannot contain more than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return the number of {@link Point points} in this pattern.
     * @throws TooManyPointsInPatternError for some forms of large patterns, if the number of points is greater than
     *                                     <code>Integer.MAX_VALUE</code> or, in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     * @see #largePointCount()
     * @see #isSurelySinglePoint
     * @see QuickPointCountPattern#isPointCountVeryLarge()
     */
    long pointCount();

    /**
     * Returns the number of points in this pattern as <code>double</code> value.
     * In particular, if the result of {@link #pointCount()} method is not greater than <code>Long.MAX_VALUE</code>,
     * there is a guarantee that this method returns the same result, cast to <code>double</code> type.
     *
     * <p><b>Warning!</b> This method can work slowly for some forms of large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points (result of this method).
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>.
     *
     * <p>There is a guarantee, that if this object implements {@link QuickPointCountPattern} interface,
     * then this method works very quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * @return the number of {@link Point points} in this pattern as <code>double</code> value.
     * @throws TooManyPointsInPatternError for some forms of large patterns, if the number of points is greater than
     *                                     <code>Integer.MAX_VALUE</code> or, in some rare situations,
     *                                     is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     * @see QuickPointCountPattern#isPointCountVeryLarge()
     */
    double largePointCount();

    /**
     * Returns a set of all points of this pattern.
     *
     * <p>The result of this method is immutable (<code>Collections.unmodifiableSet</code>).
     * Moreover, the result is always the same for different calls of this method for the same instance &mdash;
     * there are no ways to change it, in particular, via any custom methods of the implementation class
     * (it is a conclusion from the common requirement, that all implementations of this interface must be
     * immutable).
     *
     * <p>The returned set is always non-empty,
     * and the number of its elements is always equal to {@link #pointCount()}.
     *
     * <p><b>Warning!</b> This method can work slowly for some forms of large patterns.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>.
     * This method surely fails (throws one of these exception), if the total number of points
     * <code>{@link #pointCount()}&gt;Integer.MAX_VALUE</code>, because Java <code>Set</code> object
     * cannot contain more than <code>Integer.MAX_VALUE</code> elements.
     *
     * <p>For example, implementations of the {@link RectangularPattern rectangular patterns}
     * allow to successfully define a very large 3D parallelepiped
     * <i>n</i> x <i>n</i> x <i>n</i>.
     * Fur such pattern, this method will require a lot of memory
     * for <i>n</i>=1000 and will fail (probably with {@link TooManyPointsInPatternError})
     * for <i>n</i>=2000 (2000<sup>3</sup>&gt;<code>Integer.MAX_VALUE</code>).
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) operations and memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>Note: this method works very quickly (<i>O</i>(1) operations) in {@link SimplePattern} class.
     *
     * @return all points of this pattern.
     * @throws TooManyPointsInPatternError if the number of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     */
    Set<Point> points();

    /**
     * <p>Returns the set of all {@link IPoint integer points}, obtained from the points of this pattern
     * (results of {@link #points() points()} method by rounding with help of
     * {@link Point#toRoundedPoint()} method.
     * In other words, the results of this method is the same as the result of the following code:
     * <pre>
     *     Set&lt;IPoint&gt; result = new HashSet&lt;IPoint&gt;(); // or another Set implementation
     *     for (Point p : {@link #points() points()}) {
     *         result.add(p.{@link Point#toRoundedPoint() toRoundedPoint()});
     *     }
     *     result = Collections.unmodifiableSet(result);
     * </pre>
     *
     * <p>The result of this method is immutable (<code>Collections.unmodifiableSet</code>).
     * Moreover, the result is always the same for different calls of this method for the same instance &mdash;
     * there are no ways to change it, in particular, via any custom methods of the implementation class
     * (it is a conclusion from the common requirement, that all implementations of this interface must be
     * immutable).
     *
     * <p>The returned set is always non-empty.
     *
     * <p>Note: the number of resulting points can be less than {@link #pointCount()}, because some
     * real points can be rounded to the same integer points.</p>
     *
     * <p>According the basic restriction to pattern coordinates (see
     * the {@link Pattern comments to this interface}, section "Coordinate restrictions"),
     * you may be sure that you will able
     * to create an integer {@link UniformGridPattern uniform-grid} pattern by passing the result of this method
     * to {@link Patterns#newIntegerPattern(java.util.Collection)}.
     *
     * <p><b>Warning!</b> This method can work slowly or throw {@link TooManyPointsInPatternError}
     * / <code>OutOfMemoryError</code> in the same situations as {@link #points()} method.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) operations and memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     * Please compare with {@link #round()} method, which always works quickly and without exceptions also
     * for the case of {@link RectangularPattern}.
     *
     * @return all points of this pattern, rounded to the nearest integer points.
     * @throws TooManyPointsInPatternError if the number of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     */
    Set<IPoint> roundedPoints();

    /**
     * Returns the minimal and maximal coordinate with the given index
     * ({@link Point#coord(int) Point.coord(coordIndex)})
     * among all points of this pattern.
     * The minimal coordinate will be <code>r.{@link Range#min() min()}</code>,
     * the maximal coordinate will be <code>r.{@link Range#max() max()}</code>,
     * where <code>r</code> is the result of this method.
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
     * {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>, like {@link #points()} method.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>, 1 for <i>y</i>, 2 for <i>z</i>, etc.).
     * @return the range from minimal to maximal coordinate with this index.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @see #roundedCoordRange(int)
     * @see #coordMin()
     * @see #coordMax()
     * @see #coordArea()
     */
    Range coordRange(int coordIndex);

    /**
     * Returns the minimal and maximal coordinates
     * among all points of this pattern for all dimensions.
     * If <code>a</code> is the result of this method,
     * then <code>a.{@link RectangularArea#coordCount() coordCount()}=={@link #dimCount() dimCount()}</code>
     * and <code>a.{@link RectangularArea#range(int) range}(k)</code>
     * is equal to <code>{@link #coordRange(int) coordRange}(k)</code> for all <code>k</code>.
     *
     * <p>For example, in 2-dimensional case the result is
     * the circumscribed rectangle (with sides, parallel to the axes).
     *
     * <p>All, said in the comments to {@link #coordRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>,
     * is also true for this method.
     *
     * @return the ranges from minimal to maximal coordinate for all space dimensions.
     * @see #roundedCoordArea()
     */
    RectangularArea coordArea();

    /**
     * Returns the point, each coordinate of which
     * is equal to the minimal corresponding coordinate
     * among all points of this pattern.
     * Equivalent to <code>{@link #coordArea()}.{@link RectangularArea#min() min()}</code>.
     *
     * <p>All, said in the comments to {@link #coordRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>,
     * is also true for this method.
     *
     * @return minimal coordinates for all space dimensions as a point.
     */
    Point coordMin();

    /**
     * Returns the point, each coordinate of which
     * is equal to the maximal corresponding coordinate
     * among all points of this pattern.
     * Equivalent to <code>{@link #coordArea()}.{@link RectangularArea#max() max()}</code>.
     *
     * <p>All, said in the comments to {@link #coordRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>,
     * is also true for this method.
     *
     * @return maximal coordinates for all space dimensions as a point.
     */
    Point coordMax();

    /**
     * Returns the same result as {@link #coordRange(int coordIndex)} method,
     * but both minimal and maximal coordinates are rounded to integer values
     * by <code>StrictMath.round</code> operation.
     * Equivalent to <code>{@link #coordRange(int) coordRange}(coordIndex).{@link Range#toRoundedRange()
     * toRoundedRange()}</code>.
     *
     * <p>According the basic restriction to pattern coordinates (see
     * the {@link Pattern comments to this interface}, section "Coordinate restrictions"),
     * you may be sure that you will be able
     * to create an integer {@link RectangularPattern rectangular pattern} by passing the ranges, got by this method,
     * to {@link Patterns#newRectangularIntegerPattern(IRange...)}.
     *
     * <p>All, said in the comments to {@link #coordRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>,
     * is also true for this method.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>, 1 for <i>y</i>, 2 for <i>z</i>, etc.).
     * @return the range from minimal to maximal coordinate with this index, rounded to the <code>long</code> values.
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @see #roundedCoordArea()
     */
    IRange roundedCoordRange(int coordIndex);

    /**
     * Returns the same result as {@link #coordArea()} method,
     * but all minimal and maximal coordinates are rounded to integer values
     * by <code>StrictMath.round</code> operation.
     * The method {@link IRectangularArea#range(int coordIndex)} in the returned area
     * returns the same result as {@link #roundedCoordRange(int coordIndex)} method in this object.
     *
     * <p>All, said in the comments to {@link #coordRange(int)} method
     * about the speed and impossibility of {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>,
     * is also true for this method.
     *
     * @return the ranges from minimal to maximal coordinate for all space dimensions,
     * rounded to the <code>long</code> values.
     */
    IRectangularArea roundedCoordArea();

    /**
     * Returns <code>true</code> if this pattern consists of the single point, i&#46;e&#46;
     * if <code>{@link #pointCount() pointCount()}==1</code>.
     *
     * <p>There are no strict guarantees that this method <i>always</i> returns <code>true</code> if the pattern
     * consist of the single point. (In some complex situations, such analysis can
     * be too difficult. In particular, if the pattern is a {@link Patterns#newMinkowskiSum(java.util.Collection)
     * Minkowski sum}, then limited floating-point precision can lead to equality of all points of the result.
     * Simple example: a Minkowski sum of two-point one-dimensional pattern, consisting of points
     * 0.0 and 0.000001, and one-point 2<sup>51</sup>=2251799813685248.0, contains only 1 point 2<sup>51</sup>,
     * because the computer cannot represent precise value 2251799813685248.000001 in <code>double</code> type
     * and rounds it to 2251799813685248.0.
     * In such situations, this method sometimes <i>may</i> incorrectly return <code>false</code>.)
     *
     * <p>But there is the reverse guarantee: if this method returns <code>true</code>,
     * the number of points in this pattern is always&nbsp;1.</p>
     *
     * <p>Unlike {@link #pointCount()} method, there is a guarantee that this method
     * never works very slowly and cannot lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>.
     * In situations, when the number of points is very large
     * (and, so, {@link #pointCount()} method is not safe in use),
     * this method must detect this fact in reasonable time and return <code>false</code>.
     *
     * <p>There is a guarantee, that if this object implements {@link QuickPointCountPattern} interface,
     * then this method works very quickly (<i>O</i>(1) operations) and absolutely correctly
     * (always returns <code>true</code> if and only if <code>{@link #pointCount() pointCount()}==1</code>).
     *
     * @return <code>true</code> if it is one-point pattern.
     * @see #isSurelyOriginPoint()
     */
    boolean isSurelySinglePoint();

    /**
     * Returns <code>true</code> if this pattern consists of the single point and
     * this point is the origin of coordinates.
     *
     * <p>There are no strict guarantees that this method <i>always</i> returns <code>true</code> if the pattern
     * consist of the single point, equal to the origin of coordinates. (In some complex situations, such analysis can
     * be too difficult. In such situations, this method <i>may</i> incorrectly return <code>false</code>.)
     * But there is the reverse guarantee: if this method returns <code>true</code>,
     * the number of points in this pattern is always 1 and its only point is the origin of coordinates,
     * in terms of {@link Point#isOrigin()} method.</p>
     *
     * <p>Unlike {@link #pointCount()} method, there is a guarantee that this method
     * never works very slowly and cannot lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>.
     * In situations, when the number of points is very large
     * (and, so, {@link #pointCount()} method is not safe in use),
     * this method must detect this fact in reasonable time and return <code>false</code>.
     *
     * <p>There is a guarantee, that if this object implements {@link QuickPointCountPattern} interface,
     * then this method works very quickly (<i>O</i>(1) operations) and absolutely correctly.
     *
     * @return <code>true</code> if it is one-point pattern containing the origin of coordinates as the single point.
     * @see #isSurelySinglePoint
     */
    boolean isSurelyOriginPoint();

    /**
     * Returns <code>true</code> if this pattern is <i>integer</i>:
     * all coordinates of all points of this pattern are integer numbers.
     * In other words, it means that for each real (<code>double</code>) coordinate <i>x</i> of each point
     * of this pattern the Java expression <i>x</i><code>==(long)</code><i>x</i> is <code>true</code>.
     *
     * <p>More precisely, if this method returns <code>true</code>, then there are the following guarantees:
     * <ol>
     * <li>for each point, returned by {@link #points()} method, as well as by
     * {@link #coordMin()}/{@link #coordMax()}, {@link Point#isInteger()} method returns <code>true</code>;</li>
     * <li>each pattern, returned in the results of {@link #minkowskiDecomposition(int)},
     * {@link #unionDecomposition(int)} and {@link #allUnionDecompositions(int)} methods, is also surely integer,
     * i.e. this method also returns <code>true</code> for it.</li>
     * </ol>
     *
     * <p>However, there are no strict guarantees that this method <i>always</i> returns <code>true</code> if the pattern
     * is really integer. In other words, if this method returns <code>false</code>, there is no guarantee, that
     * this pattern really contains some non-integer points &mdash; but it is probable.
     *
     * <p>Unlike {@link #points()} method, there is a guarantee that this method
     * never works very slowly and cannot lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>.
     * In situations, when the number of points is very large
     * and there is a risk to fail with {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code>,
     * this method must detect this fact in reasonable time and return <code>false</code>.
     *
     * <p>See the {@link Pattern comments to this interface}, section "Integer patterns", for more details.
     *
     * @return <code>true</code> if this pattern and all patterns of its decomposition
     * ({@link #minkowskiDecomposition(int) Minkowski} or {@link #unionDecomposition(int) union})
     * assuredly contain only {@link Point#isInteger() integer} points.
     */
    boolean isSurelyInteger();

    /**
     * Returns this pattern, every point of which is rounded to the nearest integer point.
     * The result is always <i>ordinary integer pattern</i>
     * (see the {@link Pattern comments to this interface}, section "Uniform-grid patterns").
     *
     * <p>More precisely, the resulting pattern:
     * <ol>
     * <li>consists of all points,
     * obtained from all points of this pattern by rounding by the call
     * <code>point.{@link Point#toRoundedPoint() toRoundedPoint()}.{@link IPoint#toPoint() toPoint()}</code>;</li>
     * <li>has zero origin {@link UniformGridPattern#originOfGrid()}=(0,0,...,0)
     * and unit steps {@link UniformGridPattern#stepsOfGrid()}={1,1,..,1}.</li>
     * </ol>
     *
     * <p>Note: the number of points in the result can be less than {@link #pointCount()}, because some
     * real points can be rounded to the same integer points.</p>
     *
     * <p><b>Warning!</b> If this object is not {@link DirectPointSetPattern}
     * and is not {@link RectangularPattern}, this method can work slowly for some large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>. The situation is like in {@link #points()} and {@link #roundedPoints()} method.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) operations and memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>There is a guarantee, that if this object implements {@link RectangularPattern} interface,
     * then this method works quickly (<i>O</i>(1) operations) and without exceptions.
     * It is an important difference from {@link #points()} and {@link #roundedPoints()} method.
     *
     * <p>The theorem I, described in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions", provides a guarantee that this method never throws
     * {@link TooLargePatternCoordinatesException}.
     *
     * @return the integer pattern, geometrically nearest to this one.
     * @throws TooManyPointsInPatternError if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code> can be also thrown instead of this exception).
     */
    UniformGridPattern round();

    /**
     * Returns this pattern, shifted by the argument.
     *
     * <p>More precisely, the resulting pattern consists of the points,
     * obtained from all points of this pattern by the call <code>point.{@link Point#add(Point) add}(shift)</code>.
     * <p>
     * <!--Repeat.SectionStart simple_corrections_features-->
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     * For comparison, an attempt to do the same operation via getting all points ({@link #points()} method),
     * correcting them and forming a new pattern via {@link Patterns#newPattern(java.util.Collection)}
     * will lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> for some forms of large patterns.
     * <!--Repeat.SectionEnd simple_corrections_features-->
     *
     * <p>Warning: this method can fail with {@link TooLargePatternCoordinatesException}, if some of new points
     * violate restrictions, described in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions" (for example, due to very large shift).
     *
     * <p>However, {@link TooLargePatternCoordinatesException} is impossible in many important cases, when
     * this pattern is an <i>integer</i> pattern and each coordinate
     * <i>X</i><sub><i>j</i></sub>=<code>shift.{@link Point#coord(int) coord}(</code><i>j</i><code>)</code>
     * of the argument is equal to &minus;<i>x</i><sub><i>j</i></sub> for some some point
     * (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)
     * of this pattern.
     * In particular, you can use this method for <i>integer</i> patterns without a risk of
     * {@link TooLargePatternCoordinatesException} in the following situations:
     * <ul>
     * <li><code>shift</code> is <code>thisIntegerPattern.{@link #coordMin() coordMin()}.{@link Point#symmetric()
     * symmetric()}</code>,</li>
     * <li><code>shift</code> is <code>thisIntegerPattern.{@link #coordMax() coordMax()}.{@link Point#symmetric()
     * symmetric()}</code>,</li>
     * <li><code>shift</code> is <code>p.{@link Point#symmetric() symmetric()}</code>, where <code>p</code> is
     * some of the {@link #points() points} if this integer pattern.</li>
     * </ul>
     * <p>See more details in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions", the theorem II.
     *
     * @param shift the shift.
     * @return the shifted pattern.
     * @throws NullPointerException                if the argument is {@code null}.
     * @throws IllegalArgumentException            if <code>point.{@link
     *                                             Point#coordCount() coordCount()}!={@link #dimCount()}</code>.
     * @throws TooLargePatternCoordinatesException if the set of shifted points does not fulfil the restrictions,
     *                                             described in the {@link Pattern comments to this interface},
     *                                             section "Coordinate restrictions".
     */
    Pattern shift(Point shift);

    /**
     * Returns the symmetric pattern: equivalent to {@link #multiply(double) multiply(-1.0)}.
     * <p>
     * <!--Repeat(INCLUDE_FROM_FILE, THIS_FILE, simple_corrections_features)!! Auto-generated: NOT EDIT !! -->
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     * For comparison, an attempt to do the same operation via getting all points ({@link #points()} method),
     * correcting them and forming a new pattern via {@link Patterns#newPattern(java.util.Collection)}
     * will lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> for some forms of large patterns.
     * <!--Repeat.IncludeEnd-->
     *
     * @return the symmetric pattern.
     */
    Pattern symmetric();

    /**
     * Returns this pattern, scaled by the specified multiplier along all coordinates.
     *
     * <p>More precisely, the resulting pattern consists of the points,
     * obtained from all points of this pattern by the call
     * <code>point.{@link Point#multiply(double) multiply}(multipliers)</code>.
     *
     * <p>This method is equivalent to {@link #scale(double... multipliers)}, where all
     * {@link #dimCount()} arguments of that method are equal to <code>multiplier</code>.
     * <p>
     * <!--Repeat(INCLUDE_FROM_FILE, THIS_FILE, simple_corrections_features)!! Auto-generated: NOT EDIT !! -->
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     * For comparison, an attempt to do the same operation via getting all points ({@link #points()} method),
     * correcting them and forming a new pattern via {@link Patterns#newPattern(java.util.Collection)}
     * will lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> for some forms of large patterns.
     * <!--Repeat.IncludeEnd-->
     *
     * <p>Warning: this method can fail with {@link TooLargePatternCoordinatesException}, if some of new points
     * violate restrictions, described in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions" (for example, due to a very large multiplier).
     * However, such failure is obviously impossible, if the multiplier is
     * in range <code>-1.0&lt;=multiplier&lt;=1.0</code>.
     *
     * @param multiplier the scale along all coordinates.
     * @return the scaled pattern.
     * @throws TooLargePatternCoordinatesException if the set of scaled points does not fulfil the restrictions,
     *                                             described in the {@link Pattern comments to this interface},
     *                                             section "Coordinate restrictions".
     * @see #scale(double...)
     */
    Pattern multiply(double multiplier);

    /**
     * Returns this pattern, scaled by the specified multipliers along all coordinates.
     *
     * <p>More precisely, the resulting pattern consists of the points,
     * obtained from all points of this pattern by the call
     * <code>point.{@link Point#scale(double...) scale}(multipliers)</code>.
     * <p>
     * <!--Repeat(INCLUDE_FROM_FILE, THIS_FILE, simple_corrections_features)!! Auto-generated: NOT EDIT !! -->
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     * For comparison, an attempt to do the same operation via getting all points ({@link #points()} method),
     * correcting them and forming a new pattern via {@link Patterns#newPattern(java.util.Collection)}
     * will lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> for some forms of large patterns.
     * <!--Repeat.IncludeEnd-->
     *
     * <p>Warning: this method can fail with {@link TooLargePatternCoordinatesException}, if some of new points
     * violate restrictions, described in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions" (for example, due to very large multipliers).
     * However, such failure is obviously impossible, if all multipliers are
     * in range <code>-1.0&lt;=multipliers[k]&lt;=1.0</code>.
     *
     * @param multipliers the scales along coordinates.
     * @return the scaled pattern.
     * @throws NullPointerException                if the argument is {@code null}.
     * @throws IllegalArgumentException            if <code>multipliers.length!={@link #dimCount() dimCount()}</code>.
     * @throws TooLargePatternCoordinatesException if the set of scaled points does not fulfil the restrictions,
     *                                             described in the {@link Pattern comments to this interface},
     *                                             section "Coordinate restrictions".
     * @see #multiply(double)
     */
    Pattern scale(double... multipliers);

    /**
     * Returns the projection of this pattern along the given axis.
     * The number of dimensions in the resulting pattern ({@link #dimCount()}) is less by 1, than in this one.
     *
     * <p>More precisely, the resulting pattern consists of the points,
     * obtained from all points of this pattern by the call
     * <code>point.{@link Point#projectionAlongAxis(int) projectionAlongAxis}(coordIndex)</code>.
     * <p>
     * <!--Repeat(INCLUDE_FROM_FILE, THIS_FILE, simple_corrections_features)!! Auto-generated: NOT EDIT !! -->
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p>There is a guarantee, that this method does not try to allocate much more memory,
     * that it is required for storing this pattern itself, and that it
     * never throws {@link TooManyPointsInPatternError}.
     * For comparison, an attempt to do the same operation via getting all points ({@link #points()} method),
     * correcting them and forming a new pattern via {@link Patterns#newPattern(java.util.Collection)}
     * will lead to {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> for some forms of large patterns.
     * <!--Repeat.IncludeEnd-->
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the projection of this pattern (its {@link #dimCount()} is equal to
     * <code>thisInstance.{@link #dimCount()}-1</code>).
     * @throws IndexOutOfBoundsException if <code>coordIndex&lt;0</code> or
     *                                   <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @throws IllegalStateException     if this pattern is 1-dimensional (<code>{@link #dimCount()}==1</code>).
     */
    Pattern projectionAlongAxis(int coordIndex);

    /*Repeat()
        min(?!(us|g)) ==> max;;
        less ==> greater;;
        lowerSurface ==> upperSurface;;
        \#maxBound ==> \#minBound;;
        \*[ \t]+(\*) ==> $1 */

    /**
     * Returns the <i>minimal boundary</i> of this pattern along the given axis:
     * a pattern consisting of all points of this pattern, for which there are
     * no other points with less coordinate <code>#coordIndex</code>
     * and same other coordinates.
     * The number of dimensions in the resulting pattern ({@link #dimCount()}) is the same as in this one.
     *
     * <p>In other words, this method removes some points from this pattern according the following rule:
     * if this pattern contains several points <b>p</b><sub>0</sub>, <b>p</b><sub>1</sub>, ...,
     * <b>p</b><sub><i>m</i>&minus;1</sub> with identical projection to the given axis
     * (<b>p</b><sub><i>i</i></sub><code>.{@link Point#projectionAlongAxis(int)
     * projectionAlongAxis}(coordIndex).equals(</code><b>p</b><sub><i>j</i></sub><code>.{@link
     * Point#projectionAlongAxis(int) projectionAlongAxis}(coordIndex))</code> for all <i>i</i>,&nbsp;<i>j</i>),
     * then the resulting pattern contains only one from these points, for which
     * the given coordinate <code>{@link Point#coord(int) coord}(coordIndex)</code> has the minimal value.
     *
     * <p>This method is especially useful for {@link UniformGridPattern uniform-grid} patterns.
     * For example, in {@link RectangularPattern rectangular patterns} this method returns
     * one of the facets of the hyperparallelepiped.
     * In most cases (including all {@link RectangularPattern rectangular patterns})
     * this method returns the same result as {@link UniformGridPattern#lowerSurface(int)};
     * but if the figure, described by this pattern, contains some "holes", the result of this method
     * contains fewer points than {@link UniformGridPattern#lowerSurface(int)}.
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p><b>Warning!</b> If this object is not {@link DirectPointSetPattern}
     * and is not {@link RectangularPattern}, this method can work slowly for some large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>. The situation is like in {@link #points()} and {@link #roundedPoints()} method.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>There is a guarantee, that if this object implements {@link RectangularPattern} interface,
     * then this method works quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the minimal boundary of this pattern for the given axis.
     * @throws IndexOutOfBoundsException   if <code>coordIndex&lt;0</code> or
     *                                     <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @throws TooManyPointsInPatternError if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code>
     *                                     can be also thrown instead of this exception).
     * @see #maxBound(int)
     */
    Pattern minBound(int coordIndex);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Returns the <i>maximal boundary</i> of this pattern along the given axis:
     * a pattern consisting of all points of this pattern, for which there are
     * no other points with greater coordinate <code>#coordIndex</code>
     * and same other coordinates.
     * The number of dimensions in the resulting pattern ({@link #dimCount()}) is the same as in this one.
     *
     * <p>In other words, this method removes some points from this pattern according the following rule:
     * if this pattern contains several points <b>p</b><sub>0</sub>, <b>p</b><sub>1</sub>, ...,
     * <b>p</b><sub><i>m</i>&minus;1</sub> with identical projection to the given axis
     * (<b>p</b><sub><i>i</i></sub><code>.{@link Point#projectionAlongAxis(int)
     * projectionAlongAxis}(coordIndex).equals(</code><b>p</b><sub><i>j</i></sub><code>.{@link
     * Point#projectionAlongAxis(int) projectionAlongAxis}(coordIndex))</code> for all <i>i</i>,&nbsp;<i>j</i>),
     * then the resulting pattern contains only one from these points, for which
     * the given coordinate <code>{@link Point#coord(int) coord}(coordIndex)</code> has the maximal value.
     *
     * <p>This method is especially useful for {@link UniformGridPattern uniform-grid} patterns.
     * For example, in {@link RectangularPattern rectangular patterns} this method returns
     * one of the facets of the hyperparallelepiped.
     * In most cases (including all {@link RectangularPattern rectangular patterns})
     * this method returns the same result as {@link UniformGridPattern#upperSurface(int)};
     * but if the figure, described by this pattern, contains some "holes", the result of this method
     * contains fewer points than {@link UniformGridPattern#upperSurface(int)}.
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern implements {@link RectangularPattern}.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p><b>Warning!</b> If this object is not {@link DirectPointSetPattern}
     * and is not {@link RectangularPattern}, this method can work slowly for some large patterns:
     * the required time can be <i>O</i>(<i>N</i>), where <i>N</i> is the number of points.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>. The situation is like in {@link #points()} and {@link #roundedPoints()} method.
     *
     * <p>There is a guarantee, that if this object implements {@link DirectPointSetPattern} interface,
     * then this method requires not greater than <i>O</i>(<i>N</i>) memory
     * (<i>N</i>={@link #pointCount() pointCount()})
     * and never throws {@link TooManyPointsInPatternError}.
     *
     * <p>There is a guarantee, that if this object implements {@link RectangularPattern} interface,
     * then this method works quickly (<i>O</i>(1) operations) and without exceptions.
     *
     * @param coordIndex the index of the coordinate (0 for <i>x</i>-axis , 1 for <i>y</i>-axis,
     *                   2 for <i>z</i>a-xis, etc.).
     * @return the maximal boundary of this pattern for the given axis.
     * @throws IndexOutOfBoundsException   if <code>coordIndex&lt;0</code> or
     *                                     <code>coordIndex&gt;={@link #dimCount()}</code>.
     * @throws TooManyPointsInPatternError if this pattern is not {@link DirectPointSetPattern} and
     *                                     not {@link RectangularPattern} and if, at the same time, the number
     *                                     of points is greater than <code>Integer.MAX_VALUE</code> or,
     *                                     in some rare situations, is near this limit
     *                                     (<code>OutOfMemoryError</code>
     *                                     can be also thrown instead of this exception).
     * @see #minBound(int)
     */
    Pattern maxBound(int coordIndex);
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns the <i>carcass</i> of this pattern.
     * We define the <i>carcass</i> of the pattern P as such point set C, that, for some
     * integer <i>n</i>&gt;=1:
     *
     * <ol type="I">
     * <li>
     * 2&otimes;P = P &oplus; C;<br>
     * 4&otimes;P = (2&otimes;P) &oplus; 2C;<br>
     * 8&otimes;P = (4&otimes;P) &oplus; 4C;<br>
     * ...<br>
     * 2<sup><i>n</i></sup>&otimes;P = (2<sup><i>n</i>&minus;1</sup>&otimes;P) &oplus;
     * 2<sup><i>n</i>&minus;1</sup>C;
     * </li>
     * <li>for any <i>m</i>=1,2,...,<i>n</i> and for any positive integer
     * <i>k</i>&le;2<sup><i>m</i>&minus;1</sup>, we have<br>
     * (2<sup><i>m</i>&minus;1</sup>+<i>k</i>)&otimes;P =
     * (2<sup><i>m</i>&minus;1</sup>&otimes;P) &oplus; <i>k</i>C.</li>
     * </ol>
     *
     * <p>Here A&oplus;B means the {@link #minkowskiAdd(Pattern) Minkowski sum} of patterns A and B,
     * <i>k</i>&otimes;P means P&oplus;P&oplus;...&oplus;P (<i>k</i> summands),
     * and <i>k</i>P means the pointwise geometrical multiplication of the pattern P by the multiplier <i>k</i>,
     * i.e. <code>P.{@link #multiply(double) multiply}(<i>k</i>)</code>.
     *
     * <p>This method tries to find the minimal carcass, consisting of as little as possible number of points,
     * and the maximal value <i>n</i>, for which the formulas above are correct for the found carcass.
     * (The value 2<sup><i>n</i></sup> is called the <i>maximal carcass multiplier</i>
     * and is returned by {@link #maxCarcassMultiplier()} method.)
     * For example, for {@link RectangularPattern rectangular patterns} this method returns
     * the set of vertices of the hyperparallelepiped (in one-dimensional case, the pair of segment ends),
     * and the corresponding <i>n</i>=+&infin;.
     * But this method does not guarantee that the returned result is always the minimal possible carcass
     * and that the found <i>n</i> is really maximal for this carcass.
     *
     * <p>This method allows to optimize calculation of the point set of a Minkowski multiple <i>k</i>&otimes;P.
     * It is really used in the pattern implementations, returned
     * by {@link Patterns#newMinkowskiMultiplePattern(Pattern, int)} method:
     * the result of that method is not always an actual Minkowski sum of <i>N</i> equal patterns,
     * but can be (in the best case) an equal Minkowski sum of ~log<sub>2</sub><i>N</i> patterns
     * P &oplus; C &oplus; 2C &oplus; ... &oplus; 2<i><sup>m</sup></i>C
     * &oplus; (<i>N</i>&minus;2<sup><i>m</i></sup>C),
     * 2<sup><i>m</i></sup>&lt;<i>N</i>&le;2<sup><i>m</i>+1</sup>,
     * or (in not the best case, when <i>N</i> is greater than the maximal carcass multiplier 2<sup><i>n</i></sup>)
     * can be another, not so little Minkowski sum.
     *
     * <p>In the worst case (no optimization is possible), this method just returns this object (C=P),
     * and {@link #maxCarcassMultiplier()} returns 2 (i.e. <i>n</i>=1).
     *
     * <p>The returned pattern has the same number of dimensions ({@link #dimCount()}) as this one.
     *
     * <p>The returned pattern always implements {@link UniformGridPattern}
     * if this pattern implements {@link UniformGridPattern}.
     *
     * <p>This method can require some time and memory for execution,
     * but never throws {@link TooManyPointsInPatternError}.
     * <p>
     * <!-- below is a bug: sum of 2^ik*C is much greater, than k*C
     * <p><small>
     * Note: the condition II is a logical consequence from the conditions I.<br>
     * Proof.<br>
     * Let <i>k</i> = 2<sup><i>i</i><sub>1</sub></sup>+2<sup><i>i</i><sub>2</sub></sup>+...
     * is the binary representation of the number <i>k</i>.
     * According to the conditions I,
     * (2<sup><i>i<sub>k</sub></i></sup>&otimes;P) &oplus; 2<sup><i>i<sub>k</sub></i></sup>C
     * = 2<sup><i>i<sub>k</sub></i>+1</sup>&otimes;P.
     * Summing these equations for all <i>i<sub>k</sub></i>, we have
     * (<i>k</i>&otimes;P) &oplus; <i>k</i>C = 2<i>k</i>&otimes;P.
     * So,<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;(2<sup><i>m</i>&minus;1</sup>+<i>k</i>)&otimes;P
     * = ((2<sup><i>m</i>&minus;1</sup>&minus;<i>k</i>)&otimes;P) &oplus;
     * (2<i>k</i>&otimes;P)
     * = ((2<sup><i>m</i>&minus;1</sup>&minus;<i>k</i>)&otimes;P) &oplus;
     * (<i>k</i>&otimes;P) &oplus; <i>k</i>C
     * = (2<sup><i>m</i>&minus;1</sup>&otimes;P) &oplus; <i>k</i>C.<br>
     * This completes the proof.
     * </small></p>
     * -->
     *
     * @return the <i>carcass</i> of this pattern.
     */
    Pattern carcass();

    /**
     * Returns the maximal multiplier <i>k</i>, for which the calculation of
     * the Minkowski multiple <i>k</i>&otimes;P can be optimized by using the <i>carcass</i> of this pattern P.
     * Please see {@link #carcass()} method for more information.
     *
     * <p>Note: the returned value is always &ge;2. If the correct value is greater than <code>Integer.MAX_VALUE</code>
     * (for example, for {@link RectangularPattern rectangular patterns}),
     * this method returns <code>Integer.MAX_VALUE</code>; in all other cases the returning value is a power of two.
     *
     * <p>This method can require some time and memory for execution,
     * but never throws {@link TooManyPointsInPatternError}.
     * Usually an implementation caches the results of {@link #carcass()} and this methods,
     * so this method works very quickly after the first call of {@link #carcass()}.
     *
     * @return the maximal multiplier (&ge;2),
     * for which the calculation of the Minkowski multiple can be optimized
     * by using the {@link #carcass() carcass}.
     */
    int maxCarcassMultiplier();

    /**
     * Calculates and returns the Minkowski sum of this and specified patterns.
     * Briefly, the returned pattern consists of all points <i>a</i>+<i>b</i>, where
     * <i>a</i> is any point of this pattern, <i>b</i> is any point of the argument "<code>added</code>"
     * and "+" means a vector sum of two points
     * (the result of "<i>a</i>.{@link Point#add(Point) add}(<i>b</i>)" call).
     * Please see details in
     * <a href="http://en.wikipedia.org/wiki/Minkowski_addition">Wikipedia</a>.
     *
     * <p><b>Warning!</b> This method can work slowly for some forms of large patterns.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>.
     *
     * <p>Warning: this method can fail with {@link TooLargePatternCoordinatesException}, if some of new points
     * violate restrictions, described in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions".
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}.
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern and <code>subtracted</code> argument implement {@link RectangularPattern}
     * and both patterns have identical {@link UniformGridPattern#stepsOfGrid() steps}
     * (i.e. <code>thisPattern.{@link UniformGridPattern#stepsOfGridEqual(UniformGridPattern)
     * stepsOfGridEqual}(subtracted)</code> returns <code>true</code>).
     * In this case, this method works very quickly and without
     * {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> exceptions.
     *
     * <p>Please draw attention: there is another way to build a Minkowski sum,
     * namely the method {@link Patterns#newMinkowskiSum(java.util.Collection)}.
     * That method does not perform actual calculations and returns a special implementation
     * of this interface (see {@link Pattern comments to this interface}, section "Complex patterns").
     * Unlike that method, this one tries to actually calculate the Minkowski sum, saving (when possible)
     * the type of the original pattern: see above two guarantees about {@link DirectPointSetPattern}
     * and {@link RectangularPattern} types. If it is impossible to represent the Minkowski sum
     * by Java class of this pattern, it is probable that the result will be constructed
     * as {@link DirectPointSetUniformGridPattern} or as {@link SimplePattern}.
     *
     * @param added another pattern.
     * @return the Minkowski sum of this and another patterns.
     * @throws NullPointerException                if the argument is {@code null}.
     * @throws IllegalArgumentException            if the numbers of space dimensions of both patterns are different.
     * @throws TooManyPointsInPatternError         for some forms of large patterns, if the number of points in this,
     *                                             <code>added</code> or result pattern is greater than
     *                                             <code>Integer.MAX_VALUE</code> or, maybe, is near this limit
     * @throws TooLargePatternCoordinatesException if the resulting set of points does not fulfil the restrictions,
     *                                             described in the {@link Pattern comments to this interface},
     *                                             section "Coordinate restrictions".
     * @see Patterns#newMinkowskiSum(java.util.Collection)
     * @see #minkowskiSubtract(Pattern)
     */
    Pattern minkowskiAdd(Pattern added);

    /**
     * Calculates and returns the erosion of this pattern by specified pattern
     * or {@code null} if this erosion is the empty set.
     * Briefly, the returned pattern consists of all such points <i>p</i>,
     * that for any points <i>b</i> of the "<code>subtracted</code>" pattern the vector sum of two points
     * <i>p</i>+<i>b</i>
     * (the result of "<i>p</i>.{@link Point#add(Point) add}(<i>b</i>)" call)
     * belongs to this pattern.
     * Please see more details in
     * <a href="http://en.wikipedia.org/wiki/Erosion_%28morphology%29">Wikipedia</a> and
     * Google about the "Erosion" and "Minkowski subtraction" terms.
     *
     * <p><b>Warning!</b> This method can work slowly for some forms of large patterns.
     * In these cases, this method can also throw {@link TooManyPointsInPatternError}
     * or <code>OutOfMemoryError</code>.
     *
     * <p>Warning: this method can fail with {@link TooLargePatternCoordinatesException}, if some of new points
     * violate restrictions, described in the {@link Pattern comments to this interface},
     * section "Coordinate restrictions". But it is obvious, that this exception
     * is impossible if the passed pattern "<code>subtracted</code>" contains the origin of coordinates
     * (in this case, the result is a subset of this pattern).
     *
     * <p>The returned pattern always implements {@link DirectPointSetPattern}
     * if this pattern implements {@link DirectPointSetPattern}.
     *
     * <p>The returned pattern always implements {@link RectangularPattern}
     * if this pattern and <code>subtracted</code> argument implement {@link RectangularPattern}
     * and both patterns have identical {@link UniformGridPattern#stepsOfGrid() steps}
     * (i.e. <code>thisPattern.{@link UniformGridPattern#stepsOfGridEqual(UniformGridPattern)
     * stepsOfGridEqual}(subtracted)</code> returns <code>true</code>).
     * In this case, this method works very quickly and without
     * {@link TooManyPointsInPatternError} / <code>OutOfMemoryError</code> exceptions.
     *
     * @param subtracted another pattern.
     * @return the erosion of this pattern by the specified pattern
     * or {@code null} if this erosion is the empty set.
     * @throws NullPointerException                if the argument is {@code null}.
     * @throws IllegalArgumentException            if the numbers of space dimensions of both patterns are different.
     * @throws TooManyPointsInPatternError         for some forms of large patterns, if the number of points in this,
     *                                             <code>subtracted</code> or result pattern is greater than
     *                                             <code>Integer.MAX_VALUE</code> or, maybe, is near this limit
     * @throws TooLargePatternCoordinatesException if the resulting set of points does not fulfil the restrictions,
     *                                             described in the {@link Pattern comments to this interface},
     *                                             section "Coordinate restrictions".
     * @see #minkowskiAdd(Pattern)
     */
    Pattern minkowskiSubtract(Pattern subtracted);

    /**
     * Returns the Minkowski decomposition:
     * a non-empty list of patterns P<sub>0</sub>, P<sub>1</sub>,&nbsp;..., P<sub><i>n</i>&minus;1</sub>,
     * such that this pattern P (the point set represented by it)
     * is a Minkowski sum of them (of the point sets represented by them):
     * P = P<sub>0</sub> &oplus; P<sub>1</sub> &oplus;...&oplus; P<sub><i>n</i>&minus;1</sub>.
     * In other words, each point <b>p</b>&isin;P of this pattern is equal to a vector sum
     * of some <i>n</i> points
     * <b>p</b><sub>0</sub>, <b>p</b><sub>1</sub>,&nbsp;..., <b>p</b><sub><i>n</i>&minus;1</sub>,
     * where <b>p</b><sub><i>i</i></sub>&isin;P<sub><i>i</i></sub>.
     * Please see <a href="http://en.wikipedia.org/wiki/Minkowski_addition">Wikipedia</a>
     * about the "Minkowski sum" term.
     *
     * <p>This method tries to find the best decomposition, that means the list of patterns
     * with minimal summary number of points. For good pattern, the returned patterns list
     * can consist of <i>O</i>(log<sub>2</sub><i>N</i>) points (sum of {@link #pointCount()}
     * values for all returned patterns),
     * where <i>N</i> is the number of points ({@link #pointCount()}) in this pattern.
     * For example, a linear one-dimensional segment {<i>x</i>: 0&lt;=x&lt;2<i><sup>m</sup></i>}
     * is a Minkowski sum of <i>m</i> point pairs {0, 2<i><sup>i</sup></i>}, <i>i</i>=0,1,...,<i>m</i>-1.
     *
     * <p>There is no guarantee that this method returns a good decomposition.
     * If this method cannot find required decomposition, it returns the 1-element list containing
     * this instance as the only element.
     *
     * <p>If the number of points in this pattern is less than the argument, i.e.
     * <code>{@link #pointCount()}&lt;minimalPointCount</code>, then this method probably does not
     * decompose this pattern and returns the 1-element list containing this instance as its element.
     * But it is not guaranteed: if the method "knows" some decomposition, but estimation of the number of points
     * can require a lot of resources, this method may ignore <code>minimalPointCount</code> argument.
     *
     * <p>However, there is a guarantee that if the number of points is 1 or 2,
     * i.e. <code>{@link #pointCount()}&le;2</code>, then this method always returns
     * the 1-element list containing this instance as its element.
     *
     * <p>There is a guarantee that the elements of the resulting list cannot be further decomposed:
     * this method, called for them with the same or larger <code>minimalPointCount</code> argument,
     * always returns a list consisting of one element.
     *
     * <p>The number of space dimensions in all returned patterns ({@link #dimCount()} is the same as in this one.
     *
     * <p>The result of this method is immutable (<code>Collections.unmodifiableList</code>).
     *
     * @param minimalPointCount this method usually does not decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     * @return the decomposition of this pattern to Minkowski sum; always contains &ge;1 elements.
     * @throws IllegalArgumentException if the argument is negative.
     */
    List<Pattern> minkowskiDecomposition(int minimalPointCount);

    /**
     * Returns <code>true</code> if and only if the Minkowski decomposition,
     * returned by {@link #minkowskiDecomposition(int) minkowskiDecomposition(0)} call,
     * consists of 2 or more patterns:
     * <code>{@link #minkowskiDecomposition(int) minkowskiDecomposition(0)}.size()&gt;1</code>.
     *
     * <p>In some situations this method works essentially faster then the actual
     * {@link #minkowskiDecomposition(int) minkowskiDecomposition(0)} call.
     *
     * <p>Note that if this method returns <code>true</code>, then {@link #pointCount()} and
     * {@link #largePointCount()} methods can work very slowly and even may fail with
     * <code>OutOfMemoryError</code> or {@link TooManyPointsInPatternError}.
     *
     * @return <code>true</code> if the Minkowski decomposition contains 2 or more elements.
     */
    boolean hasMinkowskiDecomposition();

    /**
     * Returns a union decomposition:
     * a non-empty list of patterns P<sub>0</sub>, P<sub>1</sub>,&nbsp;..., P<sub><i>n</i>&minus;1</sub>,
     * such that this pattern P (the point set represented by it)
     * is the set-theoretical union of them (of the point sets represented by them):
     * P = P<sub>0</sub> &cup; P<sub>1</sub> &cup;...&cup; P<sub><i>n</i>&minus;1</sub>.
     *
     * <p>This method tries to find such decomposition, that all patterns P<sub><i>i</i></sub> have good
     * {@link #minkowskiDecomposition(int) Minkowski decompositions}
     * and the summary number of points in all Minkowski decompositions
     * P<sub><i>i</i></sub><code>.{@link #minkowskiDecomposition(int)
     * minkowskiDecomposition(minimalPointCount)}</code>
     * of all patterns, returned by this method, is as small as possible &mdash;
     * usually much less than the number of points in this instance.
     * If this pattern already has a good Minkowski decompositions,
     * this method should return the 1-element list containing
     * this instance as the only element.
     *
     * <p>If the number of points in this pattern is less than the argument, i.e.
     * <code>{@link #pointCount()}&lt;minimalPointCount</code>, then this method probably does not
     * decompose this pattern and returns the 1-element list containing this instance as its element.
     * Moreover, this method tries to build such decomposition, that every element P<sub><i>i</i></sub>
     * in the resulting list contains <code>&ge;minimalPointCount</code> elements.
     *
     * <p>There is a guarantee that the elements of the resulting list cannot be further decomposed:
     * this method, called for them with the same or larger <code>minimalPointCount</code> argument,
     * always returns a list consisting of one element.
     *
     * <p>The number of space dimensions in all returned patterns ({@link #dimCount()} is the same as in this one.
     *
     * <p>The result of this method is immutable (<code>Collections.unmodifiableList</code>).
     *
     * @param minimalPointCount this method usually does not decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     * @return a decomposition of this pattern into the union of patterns; always contains &ge;1 elements.
     * @throws IllegalArgumentException if the argument is negative.
     */
    List<Pattern> unionDecomposition(int minimalPointCount);

    /**
     * Returns a non-empty list of all best or almost best
     * {@link #unionDecomposition(int) union decompositions}
     * with equal or similar "quality",
     * i&#46;e&#46; with the same or almost same summary number of points in all Minkowski decompositions
     * of all returned patterns.
     *
     * <p>This method is a useful addition to {@link #unionDecomposition(int)} method for a case,
     * when there are several union decompositions with similar "quality".
     * In this case an algorithm, using union decompositions, is able to choose
     * the best from several variants according additional algorithm-specific criteria.
     *
     * <p>The number of space dimensions in all returned patterns ({@link #dimCount()} is the same as in this one.
     *
     * <p>The result of this method and the elements of the result are immutable
     * (<code>Collections.unmodifiableList</code>).
     *
     * @param minimalPointCount this method usually does not decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     * @return several good variants of decomposition of this pattern to the union of patterns;
     * the result always contains &ge;1 elements,
     * and all its elements also contain &ge;1 elements.
     * @throws IllegalArgumentException if the argument is negative.
     */
    List<List<Pattern>> allUnionDecompositions(int minimalPointCount);
}
