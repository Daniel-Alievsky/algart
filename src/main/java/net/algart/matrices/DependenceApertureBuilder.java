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

package net.algart.matrices;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.Pattern;

import java.util.Objects;

/**
 * <p>Helper class for calculation of the rectangular dependence aperture of some matrix processing algorithms.</p>
 *
 * <p>Many algorithms, processing {@link Matrix <i>n</i>-dimensional matrices}, calculate a resulting matrix,
 * where the value of every element depends only on the elements of some source matrix (or matrices),
 * lying inside some rectangular aperture around the coordinates of the resulting element.
 * The typical examples are aperture-dependent processors, represented by {@link ApertureProcessor} interface,
 * and a group of algorithms, described by {@link StreamingApertureProcessor} class.
 * We call such an aperture the <i>dependence aperture</i> of a given processing algorithm.</p>
 *
 * <p>More precisely, the <i>dependence aperture</i> is such rectangular area in <i>n</i>-dimensional space,
 * represented by {@link IRectangularArea} object <b>A</b>,
 * that every element of the resulting <i>n</i>-dimensional matrix with coordinates
 * <nobr><b>x</b> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
 * can depend only on elements of the source matrix (or matrices) with coordinates</p>
 *
 * <blockquote>
 *     <b>x</b>+<b>a</b> =
 *     <i>x</i><sub>0</sub>+<i>a</i><sub>0</sub>,
 *     <i>x</i><sub>1</sub>+<i>a</i><sub>1</sub>, ...,
 *     <i>x</i><sub><i>n</i>&minus;1</sub>+<i>a</i><sub><i>n</i>&minus;1</sub>,
 * </blockquote>
 *
 * <p>where
 * <nobr><b>a</b> = (<i>a</i><sub>0</sub>, <i>a</i><sub>1</sub>, ..., <i>a</i><sub><i>n</i>&minus;1</sub>)</nobr>
 * is one of points belonging to the rectangular area <b>A</b>.
 * Please draw attention that we use plus sing + in this definition, instead of more traditional minus sign &minus;
 * (used, for example, in specifications of {@link StreamingApertureProcessor}
 * or {@link net.algart.matrices.morphology.RankMorphology}).</p>
 *
 * <p>The goal of this class is to build such rectangular aperture <b>A</b> on the base of one or several
 * apertures (patterns), represented by {@link Pattern} interface and specifying, for example,
 * possible dependence aperture for different stages of the full processing algorithm.
 * Draw attention: for the apertures, specified as patterns, we suppose more traditional definitions with
 * minus sign &minus;, as in specifications of {@link StreamingApertureProcessor}
 * or {@link net.algart.matrices.morphology.RankMorphology}
 * (a set of points
 * <nobr><b>x</b>&minus;<b>p</b></sub> = (<i>x</i><sub>0</sub>&minus;<i>p</i><sub>0</sub>,
 * <i>x</i><sub>1</sub>&minus;<i>p</i><sub>1</sub>, ...,
 * <i>x</i><sub><i>n</i>&minus;1</sub>&minus;<i>p</i><sub><i>n</i>&minus;1</sub>)</nobr>
 * for all <nobr><b>p</b>&isin;<b>P</b></nobr>, <b>P</b> is a pattern).</p>
 *
 * <p>The main method, solving this task, is</p>
 *
 * <blockquote>
 *     {@link #getAperture(int dimCount, Pattern[] patterns, boolean[] inverted, short additionalSpace)}
 * </blockquote>
 *
 * <p>The resulting rectangular aperture is built on the base of the rounded coordinate ranges of every
 * pattern ({@link Pattern#roundedCoordRange(int)}) according to combination rules,
 * depending on a concrete instance of this enumeration.
 * Please see comments to the constants of this enumeration class:</p>
 *
 * <ul>
 *     <li>{@link #SUM},</li>
 *     <li>{@link #MAX},</li>
 *     <li>{@link #SUM_MAX_0}.</li>
 * </ul>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public enum DependenceApertureBuilder {
    /**
     * Aperture builder, calculating sum of coordinate ranges of all passed patterns.
     *
     * <p>More precisely, in this builder the basic
     * {@link #getAperture(int dimCount, Pattern[] patterns, boolean[] inverted, short additionalSpace)} method
     * works according the following algorithm:
     *
     * <pre>
     * long[] allMin = new long[dimCount]; // zero-filled by Java
     * long[] allMax = new long[dimCount]; // zero-filled by Java
     * for (int k = 0; k &lt; dimCount; k++) {
     *     for (int i = 0; i &lt; patterns.length; i++) {
     *         {@link IRange} range = patterns[i].{@link Pattern#roundedCoordRange(int)
     *         roundedCoordRange}(k);
     *         long min = inverted[i] ? range.min() : -range.max();
     *         long max = inverted[i] ? range.max() : -range.min();
     *         allMin[k] += min;
     *         allMax[k] += max;
     *     }
     *     allMin[k] -= additionalSpace;
     *     allMax[k] += additionalSpace;
     * }
     * return {@link IRectangularArea#valueOf(IPoint, IPoint)
     * IRectangularArea.valueOf}({@link IPoint#valueOf(long...)
     * IPoint.valueOf}(allMin), {@link IPoint#valueOf(long...)
     * IPoint.valueOf}(allMax));
     * </pre>
     *
     * <p>The only difference from this code is that this class checks possible overflow before every usage of
     * Java <tt>+</tt> and <tt>-</tt> operators with <tt>long</tt> values and, in a case of overflow,
     * throws <tt>IndexOutOfBoundsException</tt>.
     *
     * <p>This mode is suitable for algorithms, which perform several sequential operations over the matrix,
     * when each elements of the result of each operation <tt>#k</tt> with coordinates
     * <nobr><b>x</b> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * depends on the elements of the previous matrix with coordinates
     * <nobr><b>x</b>&minus;<b>p</b><sub><i>i</i></sub> = (<i>x</i><sub>0</sub>&minus;<i>p</i><sub><i>i</i>0</sub>,
     * <i>x</i><sub>1</sub>&minus;<i>p</i><sub><i>i</i>1</sub>, ...,
     * <i>x</i><sub><i>n</i>&minus;1</sub>&minus;<i>p</i><sub><i>i</i>,<i>n</i>&minus;1</sub>)</nobr>.
     * Here <nobr><b>p</b><sub><i>i</i></sub></nobr> are all points of the corresponding pattern
     * <tt>patterns[k]</tt> or, if <tt>inverted[k]</tt> is <tt>true</tt>,
     * of the symmetric pattern <nobr><tt>patterns[k].{@link Pattern#symmetric() symmetric()}</tt></nobr>.
     *
     * <p>An example of algorithm, where this aperture builder can be useful, is
     * {@link net.algart.matrices.morphology.Morphology#dilationErosion(Matrix, Pattern, Pattern,
     * net.algart.matrices.morphology.Morphology.SubtractionMode)} (if the last argument is
     * {@link net.algart.matrices.morphology.Morphology.SubtractionMode#NONE}).
     */
    SUM,

    /**
     * Aperture builder, calculating set-theoretical union of coordinate ranges of all passed patterns,
     * with an additional guarantee that the result will contain the origin of coordinates.
     *
     * <p>More precisely, in this builder the basic
     * {@link #getAperture(int dimCount, Pattern[] patterns, boolean[] inverted, short additionalSpace)} method
     * works according the following algorithm:
     *
     * <pre>
     * long[] allMin = new long[dimCount]; // zero-filled by Java
     * long[] allMax = new long[dimCount]; // zero-filled by Java
     * for (int k = 0; k &lt; dimCount; k++) {
     *     for (int i = 0; i &lt; patterns.length; i++) {
     *         {@link IRange} range = patterns[i].{@link Pattern#roundedCoordRange(int)
     *         roundedCoordRange}(k);
     *         long min = inverted[i] ? range.min() : -range.max();
     *         long max = inverted[i] ? range.max() : -range.min();
     *         allMin[k] = Math.min(allMin[k], min);
     *         allMax[k] = Math.max(allMax[k], max);
     *     }
     *     allMin[k] -= additionalSpace;
     *     allMax[k] += additionalSpace;
     * }
     * return {@link IRectangularArea#valueOf(IPoint, IPoint)
     * IRectangularArea.valueOf}({@link IPoint#valueOf(long...)
     * IPoint.valueOf}(allMin), {@link IPoint#valueOf(long...)
     * IPoint.valueOf}(allMax));
     * </pre>
     *
     * <p>The only difference from this code is that this class checks possible overflow before every usage of
     * Java <tt>+</tt> and <tt>-</tt> operators with <tt>long</tt> values and, in a case of overflow,
     * throws <tt>IndexOutOfBoundsException</tt>.
     *
     * <p>This mode is suitable for algorithms, which perform several independent operations over the same
     * original matrix,
     * when each elements of the result of each operation <tt>#k</tt> with coordinates
     * <nobr><b>x</b> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * depends on the elements of the previous matrix with coordinates
     * <nobr><b>x</b>&minus;<b>p</b><sub><i>i</i></sub> = (<i>x</i><sub>0</sub>&minus;<i>p</i><sub><i>i</i>0</sub>,
     * <i>x</i><sub>1</sub>&minus;<i>p</i><sub><i>i</i>1</sub>, ...,
     * <i>x</i><sub><i>n</i>&minus;1</sub>&minus;<i>p</i><sub><i>i</i>,<i>n</i>&minus;1</sub>)</nobr>.
     * Here <nobr><b>p</b><sub><i>i</i></sub></nobr> are all points of the corresponding pattern
     * <tt>patterns[k]</tt> or, if <tt>inverted[k]</tt> is <tt>true</tt>,
     * of the symmetric pattern <nobr><tt>patterns[k].{@link Pattern#symmetric() symmetric()}</tt></nobr>.
     *
     * <p>An example of algorithm, where this aperture builder can be useful, is
     * {@link net.algart.matrices.morphology.Morphology#beucherGradient(Matrix, Pattern)}.
     */
    MAX,

    /**
     * Aperture builder, calculating sum of coordinate ranges of all passed patterns,
     * with an additional guarantee that the result will contain the origin of coordinates.
     *
     * <p>More precisely, in this builder the basic
     * {@link #getAperture(int dimCount, Pattern[] patterns, boolean[] inverted, short additionalSpace)} method
     * works according the following algorithm:
     *
     * <pre>
     * long[] allMin = new long[dimCount]; // zero-filled by Java
     * long[] allMax = new long[dimCount]; // zero-filled by Java
     * for (int k = 0; k &lt; dimCount; k++) {
     *     for (int i = 0; i &lt; patterns.length; i++) {
     *         {@link IRange} range = patterns[i].{@link Pattern#roundedCoordRange(int)
     *         roundedCoordRange}(k);
     *         long min = inverted[i] ? range.min() : -range.max();
     *         long max = inverted[i] ? range.max() : -range.min();
     *         allMin[k] += min;
     *         allMax[k] += max;
     *     }
     *     allMin[k] = Math.min(allMin[k], 0);
     *     allMax[k] = Math.max(allMax[k], 0);
     *     allMin[k] -= additionalSpace;
     *     allMax[k] += additionalSpace;
     * }
     * return {@link IRectangularArea#valueOf(IPoint, IPoint)
     * IRectangularArea.valueOf}({@link IPoint#valueOf(long...)
     * IPoint.valueOf}(allMin), {@link IPoint#valueOf(long...)
     * IPoint.valueOf}(allMax));
     * </pre>
     *
     * <p>The only difference from this code is that this class checks possible overflow before every usage of
     * Java <tt>+</tt> and <tt>-</tt> operators with <tt>long</tt> values and, in a case of overflow,
     * throws <tt>IndexOutOfBoundsException</tt>.
     *
     * <p>This mode is suitable for algorithms, which perform several sequential operations over the matrix,
     * when each elements of the result of each operation <tt>#k</tt> with coordinates
     * <nobr><b>x</b> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)</nobr>
     * depends on the elements of the previous matrix with coordinates
     * <nobr><b>x</b>&minus;<b>p</b><sub><i>i</i></sub> = (<i>x</i><sub>0</sub>&minus;<i>p</i><sub><i>i</i>0</sub>,
     * <i>x</i><sub>1</sub>&minus;<i>p</i><sub><i>i</i>1</sub>, ...,
     * <i>x</i><sub><i>n</i>&minus;1</sub>&minus;<i>p</i><sub><i>i</i>,<i>n</i>&minus;1</sub>)</nobr>.
     * Here <nobr><b>p</b><sub><i>i</i></sub></nobr> are all points of the corresponding pattern
     * <tt>patterns[k]</tt> or, if <tt>inverted[k]</tt> is <tt>true</tt>,
     * of the symmetric pattern <nobr><tt>patterns[k].{@link Pattern#symmetric() symmetric()}</tt></nobr>.
     *
     * <p>An example of algorithm, where this aperture builder can be useful, is
     * {@link net.algart.matrices.morphology.Morphology#maskedDilationErosion(Matrix, Pattern, Pattern)}.
     */
    SUM_MAX_0;

    /**
     * Default additional space, used by {@link #getAperture(int, Pattern[], boolean[])} method: {@value} elements.
     * This gap is enough for most cases, when a processing algorithm uses not only the elements
     * from the corresponding apertures (specified by {@link Pattern} objects), but probably also their neighbours
     * and neighbours of neighbours.
     */
    public static final short DEFAULT_ADDITIONAL_SPACE = 2;

    /**
     * Equivalent to <tt>{@link #getAperture(int, net.algart.math.patterns.Pattern[], boolean[], short)
     * getAperture}(dimCount, new Pattern[]{pattern},
     * new boolean[]{inverted}, {@link #DEFAULT_ADDITIONAL_SPACE})</tt>.
     *
     * @param dimCount        the number of dimensions.
     * @param pattern         the pattern, describing the dependence apertures the algorithm.
     * @param inverted        if <tt>true</tt>, then <tt>patterns</tt> is supposed to be inverted.
     * @return                rectangular dependence aperture, describing dependence of the elements of the full
     *                        processing algorithm.
     * @throws NullPointerException      if <tt>pattern</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>dimCount&lt;=0</tt>,
     *                                   or the passed pattern has
     *                                   {@link Pattern#dimCount() number of dimensions}, <i>less</i> than
     *                                   <tt>dimCount</tt> argument.
     * @throws IndexOutOfBoundsException in a case of integer (63-bit) overflow while calculation of the resulting
     *                                   aperture: see {@link #SUM}, {@link #MAX} and {@link #SUM_MAX_0} constants.
     */
    public IRectangularArea getAperture(int dimCount, Pattern pattern, boolean inverted) {
        return getAperture(dimCount, new Pattern[]{pattern}, new boolean[]{inverted});
    }

    /**
     * Equivalent to <tt>{@link #getAperture(int, net.algart.math.patterns.Pattern[], boolean[], short)
     * getAperture}(dimCount, new Pattern[]{pattern1, pattern2},
     * new boolean[]{inverted1, inverted2}, {@link #DEFAULT_ADDITIONAL_SPACE})</tt>.
     *
     * @param dimCount        the number of dimensions.
     * @param pattern1        the pattern, describing the dependence apertures of the 1st part
     *                        (stage) of the full algorithm.
     * @param pattern2        the pattern, describing the dependence apertures of the 2nd part
     *                        (stage) of the full algorithm.
     * @param inverted1       if <tt>true</tt>, then <tt>pattern1</tt> is supposed to be inverted.
     * @param inverted2       if <tt>true</tt>, then <tt>pattern2</tt> is supposed to be inverted.
     * @return                rectangular dependence aperture, describing dependence of the elements of the full
     *                        processing algorithm, consisting of 2 parts (stages).
     * @throws NullPointerException      if <tt>patterns1</tt> or <tt>pattern2</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>dimCount&lt;=0</tt>,
     *                                   or if some of the passed patterns have
     *                                   {@link Pattern#dimCount() number of dimensions}, <i>less</i> than
     *                                   <tt>dimCount</tt> argument.
     * @throws IndexOutOfBoundsException in a case of integer (63-bit) overflow while calculation of the resulting
     *                                   aperture: see {@link #SUM}, {@link #MAX} and {@link #SUM_MAX_0} constants.
     *
     */
    public IRectangularArea getAperture(int dimCount,
        Pattern pattern1, boolean inverted1, Pattern pattern2, boolean inverted2) {
        return getAperture(dimCount, new Pattern[]{pattern1, pattern2}, new boolean[]{inverted1, inverted2});
    }

    /**
     * Equivalent to <tt>{@link #getAperture(int, net.algart.math.patterns.Pattern[], boolean[], short)
     * getAperture}(dimCount, patterns, inverted, {@link #DEFAULT_ADDITIONAL_SPACE})</tt>.
     *
     * @param dimCount        the number of dimensions.
     * @param patterns        the set of patterns, describing the dependence apertures of different parts
     *                        (stages) of the full algorithms, for example, in terms
     *                        of {@link StreamingApertureProcessor} class.
     * @param inverted        if some element <tt>inverted[k]</tt> is <tt>true</tt>, then the corresponding element
     *                        <tt>patterns[k]</tt> is supposed to be inverted.
     * @return                rectangular dependence aperture, describing dependence of the elements of the full
     *                        processing algorithm, consisting of <tt>patterns.length</tt> parts (stages).
     * @throws NullPointerException      if <tt>patterns</tt> or <tt>inverted</tt> argument is <tt>null</tt>,
     *                                   or if some elements of <tt>patterns</tt> array is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>dimCount&lt;=0</tt>,
     *                                   or if <tt>patterns</tt> and <tt>inverted</tt> arrays have different lengths,
     *                                   or if their length is zero (<tt>patterns.length==0</tt>),
     *                                   or if some of the passed patterns have
     *                                   {@link Pattern#dimCount() number of dimensions}, <i>less</i> than
     *                                   <tt>dimCount</tt> argument.
     * @throws IndexOutOfBoundsException in a case of integer (63-bit) overflow while calculation of the resulting
     *                                   aperture: see {@link #SUM}, {@link #MAX} and {@link #SUM_MAX_0} constants.
     *
     */
    public IRectangularArea getAperture(int dimCount, Pattern[] patterns, boolean[] inverted) {
        return getAperture(dimCount, patterns,inverted, DEFAULT_ADDITIONAL_SPACE);
    }

    /**
     * Builds the rectangular aperture on the base of specified array of apertures-patterns.
     * If <tt>inverted[k]</tt> is <tt>true</tt>, then the corresponding pattern is supposed to be inverted
     * (i.e. replaced with its {@link Pattern#symmetric() symmetric} version).
     * Please see comments to {@link #SUM}, {@link #MAX} and {@link #SUM_MAX_0} constants
     * for detailed specification of the behaviour of this method.
     *
     * @param dimCount        the number of dimensions.
     * @param patterns        the set of patterns, describing the dependence apertures of different parts
     *                        (stages) of the full algorithms, for example, in terms
     *                        of {@link StreamingApertureProcessor} class.
     * @param inverted        if some element <tt>inverted[k]</tt> is <tt>true</tt>, then the corresponding element
     *                        <tt>patterns[k]</tt> is supposed to be inverted.
     * @param additionalSpace additional gap, added to all coordinate ranges of the resulting aperture.
     * @return                rectangular dependence aperture, describing dependence of the elements of the full
     *                        processing algorithm, consisting of <tt>patterns.length</tt> parts (stages).
     * @throws NullPointerException      if <tt>patterns</tt> or <tt>inverted</tt> argument is <tt>null</tt>,
     *                                   or if some elements of <tt>patterns</tt> array is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>dimCount&lt;=0</tt>,
     *                                   or if <tt>patterns</tt> and <tt>inverted</tt> arrays have different lengths,
     *                                   or if their length is zero (<tt>patterns.length==0</tt>),
     *                                   or if <tt>additionalSpace&lt;0</tt>,
     *                                   or if some of the passed patterns have
     *                                   {@link Pattern#dimCount() number of dimensions}, <i>less</i> than
     *                                   <tt>dimCount</tt> argument.
     * @throws IndexOutOfBoundsException in a case of integer (63-bit) overflow while calculation of the resulting
     *                                   aperture: see {@link #SUM}, {@link #MAX} and {@link #SUM_MAX_0} constants.
     *
     */
    public IRectangularArea getAperture(int dimCount, Pattern[] patterns, boolean[] inverted, short additionalSpace) {
        Objects.requireNonNull(patterns, "Null patterns argument");
        Objects.requireNonNull(inverted, "Null inverted argument");
        if (dimCount <= 0) {
            throw new IllegalArgumentException("Zero or negative dimCount argument");
        }
        if (patterns.length == 0) {
            throw new IllegalArgumentException("Empty patterns argument");
        }
        if (inverted.length == 0) {
            throw new IllegalArgumentException("Empty inverted argument");
        }
        if (patterns.length != inverted.length) {
            throw new IllegalArgumentException("Different lengths of patterns and inverted arguments");
        }
        if (additionalSpace < 0) {
            throw new IllegalArgumentException("Negative additionalSpace argument");
        }
        patterns = patterns.clone(); // after this moment, "patterns" cannot be changed by parallel threads
        inverted = inverted.clone(); // after this moment, "inverted" cannot be changed by parallel threads
        for (int i = 0; i < patterns.length; i++) {
            Objects.requireNonNull(patterns[i], "Null pattern #" + i);
            if (patterns[i].dimCount() < dimCount) {
                throw new IllegalArgumentException("Pattern #" + i
                    + " has insufficient dimensions (<" + dimCount + ")");
            }
        }
        long[] allMin = new long[dimCount]; // zero-filled by Java
        long[] allMax = new long[dimCount]; // zero-filled by Java
        for (int k = 0; k < dimCount; k++) {
            for (int i = 0; i < patterns.length; i++) {
                IRange range = patterns[i].roundedCoordRange(k);
                long min = inverted[i] ? range.min() : -range.max();
                long max = inverted[i] ? range.max() : -range.min();
                switch (this) {
                    case SUM:
                    case SUM_MAX_0:
                        allMin[k] = safelyAdd(allMin[k], min);
                        allMax[k] = safelyAdd(allMax[k], max);
                        break;
                    case MAX:
                        allMin[k] = Math.min(allMin[k], min);
                        allMax[k] = Math.max(allMax[k], max);
                        break;
                }
            }
            if (this == DependenceApertureBuilder.SUM_MAX_0) {
                allMin[k] = Math.min(allMin[k], 0);
                allMax[k] = Math.max(allMax[k], 0);
            }
            allMin[k] = safelyAdd(allMin[k], -additionalSpace);
            allMax[k] = safelyAdd(allMax[k], additionalSpace);
        }
        return IRectangularArea.valueOf(IPoint.valueOf(allMin), IPoint.valueOf(allMax));
    }

    /**
     * Returns a newly created array <tt>result</tt> with the same length as the first argument, where
     * <nobr><tt>result[k] = matrixDimensions[k])+aperture.{@link IRectangularArea#width(int) width}(k)</tt></nobr>.
     * This method also checks that all dimensions in the <tt>matrixDimensions</tt> array,
     * as well as the resulting dimensions <tt>result</tt> array, are allowed dimensions for some
     * AlgART matrix, i.e. are non-negative and their product is not greater than
     * <nobr><tt>Long.MAX_VALUE</tt></nobr>. If it is not so, <tt>IndexOutOfBoundsException</tt> is thrown.
     *
     * <p>In the special case, when some of elements of the <tt>matrixDimensions</tt> array is zero,
     * this method returns a precise clone of this array without changes.
     *
     * <p>Note: the matrix, returned by
     * {@link #extend(Matrix, IRectangularArea, Matrix.ContinuationMode)} method, always has dimensions,
     * equal to the result of this method, called for {@link Matrix#dimensions() dimensions} of the source matrix
     * with the same aperture.
     *
     * @param matrixDimensions {@link Matrix#dimensions() dimensions} of some <i>n</i>-dimensional matrix.
     * @param aperture         the dependence aperture.
     * @return                 new dimensions, extended by the given aperture.
     * @throws NullPointerException      if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>matrixDimensions.length!=aperture.{@link IRectangularArea#coordCount()
     *                                   coordCount()}</tt>,
     *                                   or if <tt>matrixDimensions[k]&lt;0</tt> for some <tt>k</tt>.
     * @throws IndexOutOfBoundsException if product of all elements of <tt>matrixDimensions</tt> array
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>,
     *                                   or in a case of integer overflow while calculating <tt>result[k]</tt>,
     *                                   or if product of all elements of the resulting array
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     */
    public static long[] extendDimensions(long[] matrixDimensions, IRectangularArea aperture) {
        Objects.requireNonNull(matrixDimensions, "Null matrixDimensions");
        Objects.requireNonNull(aperture, "Null aperture");
        if (matrixDimensions.length != aperture.coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: " + matrixDimensions.length
                + " dimensions in array and " + aperture.coordCount() + "-dimensional aperture");
        }
        for (int k = 0; k < matrixDimensions.length; k++) {
            if (matrixDimensions[k] < 0) {
                throw new IllegalArgumentException("Negative matrixDimensions[" + k + "]");
            }
        }
        long[] result = matrixDimensions.clone();
         // after this moment, "result" cannot be changed by parallel thread
        long len = Arrays.longMul(result);
        if (len == Long.MIN_VALUE) {
            throw new IndexOutOfBoundsException("Too large matrixDimensions: their product > Long.MAX_VALUE");
        }
        if (len == 0) {
            return result;
        }
        for (int k = 0; k < result.length; k++) {
            result[k] += aperture.width(k);
            if (result[k] < 0) {
                throw new IndexOutOfBoundsException("Too large matrix continuation: "
                    + "the dimension #" + k + " of the matrix, extended to the corresponding aperture "
                    + aperture + ", is greater than Long.MAX_VALUE");
            }
        }
        if (Arrays.longMul(result) == Long.MIN_VALUE) {
            throw new IndexOutOfBoundsException("Too large matrix continuation: product of dimensions "
                + "of the matrix, extended to the corresponding aperture "
                + aperture + ", is greater than Long.MAX_VALUE");
        }
        return result;
    }

    /**
     * Returns <tt>matrix.{@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     * subMatrix}(from, to, continuationMode)</tt>,
     * where <nobr><tt>from[k] = aperture.{@link IRectangularArea#min(int) min}(k)</tt></nobr>
     * and <nobr><tt>to[k] = matrix.{@link Matrix#dim(int) dim}(k)+aperture.{@link IRectangularArea#max(int)
     * max}(k)</tt></nobr>.
     * This method allows to extends the source matrix in such a way, that every element of the resulting matrix
     * of some processing algorithm, having the given dependence aperture, will depend only on the existing
     * elements of the extended matrix (lying inside its bounds).
     *
     * <p>In the special case <tt>matrix.{@link Matrix#size() size()}==0</tt>,
     * this method returns the <tt>matrix</tt> argument without changes.
     *
     * <p>This method performs additional checks, whether adding <tt>aperture.max().{@link IPoint#coord(int)
     * coord}(k)</tt> to the matrix dimension leads to integer overflow, and throws
     * <tt>IndexOutOfBoundsException</tt> in a case of overflow.
     *
     * @param matrix           some <i>n</i>-dimensional matrix.
     * @param aperture         the dependence aperture.
     * @param continuationMode the continuation mode for extending the matrix.
     * @return                 new matrix, extended by the given aperture.
     * @throws NullPointerException      if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>matrix.{@link Matrix#dimCount()
     *                                   dimCount()}!=aperture.{@link IRectangularArea#coordCount() coordCount()}</tt>.
     * @throws IndexOutOfBoundsException in a case of integer overflow while calculating <tt>to[k]</tt>,
     *                                   or in the same situations as the corresponding
     *                                   {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     *                                   subMatrix} call.
     * @throws ClassCastException        in the same situations as the corresponding
     *                                   {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     *                                   subMatrix} call..
     * @see #extendDimensions(long[], net.algart.math.IRectangularArea)
     */
    public static <T extends PArray> Matrix<T> extend(Matrix<T> matrix, IRectangularArea aperture,
        Matrix.ContinuationMode continuationMode)
    {
        Objects.requireNonNull(matrix, "Null matrix");
        Objects.requireNonNull(aperture, "Null aperture");
        Objects.requireNonNull(continuationMode, "Null continuation mode");
        if (matrix.dimCount() != aperture.coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: " + matrix.dimCount()
                + "-dimensional matrix and " + aperture.coordCount() + "-dimensional aperture");
        }
        if (matrix.size() == 0) {
            return matrix;
        }
        long[] from = new long[matrix.dimCount()];
        long[] to = new long[from.length];
        for (int k = 0; k < from.length; k++) {
            from[k] = aperture.min(k);
            to[k] = matrix.dim(k) + aperture.max(k);
            if (to[k] < 0 && aperture.max(k) >= 0) {
                throw new IndexOutOfBoundsException("Too large matrix continuation: "
                    + "the dimension #" + k + " of the matrix, extended to the corresponding aperture "
                    + aperture + ", is greater than Long.MAX_VALUE");
            }
        }
        return matrix.subMatrix(from, to, continuationMode);
    }

    /**
     * Returns <tt>matrix.{@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     * subMatrix}(from, to, {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC})</tt>,
     * where <nobr><tt>from[k] = -aperture.{@link IRectangularArea#min(int) min}(k)</tt></nobr>
     * and <nobr><tt>to[k] = matrix.{@link Matrix#dim(int) dim}(k)-aperture.{@link IRectangularArea#max(int)
     * max}(k)</tt></nobr>.
     * It is the reverse method for {@link #extend(Matrix, IRectangularArea, Matrix.ContinuationMode)}.
     *
     * <p>In the special case <tt>matrix.{@link Matrix#size() size()}==0</tt>,
     * this method returns the <tt>matrix</tt> argument without changes.
     *
     * @param matrix   some <i>n</i>-dimensional matrix, extended by
     *                 {@link #extend(Matrix, IRectangularArea, Matrix.ContinuationMode)} method.
     * @param aperture the dependence aperture.
     * @return         new matrix, reduced to the original sizes.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>matrix.{@link Matrix#dimCount()
     *                                   dimCount()}!=aperture.{@link IRectangularArea#coordCount() coordCount()}</tt>.
     * @throws IndexOutOfBoundsException in the same situations as the corresponding
     *                                   {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     *                                   subMatrix} call.
     */
    public static <T extends PArray> Matrix<T> reduce(Matrix<T> matrix, IRectangularArea aperture) {
        Objects.requireNonNull(matrix, "Null matrix");
        Objects.requireNonNull(aperture, "Null aperture");
        if (matrix.dimCount() != aperture.coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: " + matrix.dimCount()
                + "-dimensional matrix and " + aperture.coordCount() + "-dimensional aperture");
        }
        if (matrix.size() == 0) {
            return matrix;
        }
        long[] from = new long[matrix.dimCount()];
        long[] to = new long[from.length];
        for (int k = 0; k < from.length; k++) {
            from[k] = -aperture.min(k);
            to[k] = matrix.dim(k) - aperture.max(k);
        }
        return matrix.subMatrix(from, to, Matrix.ContinuationMode.PSEUDO_CYCLIC);
    }

    /**
     * Returns sum of the arguments <tt>a+b</tt>, if this sum can be precisely represented by <tt>long</tt>
     * type, i&#46;e&#46; if it lies in <tt>Long.MIN_VALUE..Long.MAX_VALUE</tt> range,
     * or throws <tt>IndexOutOfBoundsException</tt> in other case.
     * (Unlike this method, the simple Java operator <tt>a+b</tt> always returns low 64 bits
     * of the mathematical sum <tt>a+b</tt> without any checks and exceptions.)
     *
     * <p>This method is useful for accurate calculating matrix dimensions and integer rectangular areas,
     * for example, for calculating dimensions of a matrix, extended with some rectangular aperture.
     *
     * @param a first summand.
     * @param b second summand.
     * @return  sum <tt>a+b</tt>.
     * @throws IndexOutOfBoundsException in a case of integer overflow, i.e. if the mathematical sum <tt>a+b</tt>
     *                                   of this integers is less than <tt>Long.MIN_VALUE</tt>
     *                                   or greater than <tt>Long.MAX_VALUE</tt>.
     */
    public static long safelyAdd(long a, long b) throws IndexOutOfBoundsException {
        long result = a + b;
        if ((a < 0) != (b < 0)) { // overflow impossible when signs are different
            return result;
        }
        if ((a < 0) != (result < 0)) // overflow: the sum has another sign than both summands
        {
            throw new IndexOutOfBoundsException("Integer overflow while summing two long values " + a + " and " + b
                + " (maybe, dimensions of some matrices or areas)");
        }
        return result;
    }

}
