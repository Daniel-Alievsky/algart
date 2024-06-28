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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.patterns.Pattern;

/**
 * <p>Rank operations over {@link Matrix <i>n</i>-dimensional matrices}:
 * percentile, rank, mean between given percentiles or values, etc&#46;,
 * calculated on all matrix elements in an aperture with the fixed shape, represented by {@link Pattern} class.
 * It is supposed that the type of matrix elements is one of primitive Java types
 * (<code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>, <code>int</code>,
 * <code>long</code>, <code>float</code>, <code>double</code>) and, so, represents an integer or a real number,
 * according to comments to {@link PFixedArray#getLong(long)} and {@link PArray#getDouble(long)} methods.
 * In 2-dimensional case, these operations can be used for processing grayscale digital images.</p>
 *
 * <p>The simplest rank operation is a percentile, for example, minimum, maximum or median.
 * In the case of maximum, the percentile is strictly equivalent to <i>dilation</i> &mdash; the basic
 * operation of the mathematical morphology, offered by {@link Morphology} interface.
 * In the case of minimum, the percentile by some pattern <b>P</b> is equivalent to <i>erosion</i>
 * by the symmetric pattern <b>P</b>.{@link Pattern#symmetric() symmetric()}.
 * It allows to consider rank operations as an extension of the operation set of
 * the traditional mathematical morphology. Really, this interface extends
 * {@link Morphology} interface, and it is supposed, by definition, that
 * <code>{@link #dilation(Matrix, Pattern) dilation}(m,pattern)</code> method is equivalent to
 * <code>{@link #percentile(Matrix, double, Pattern) percentile}(m,c*<i>N</i>,pattern)</code> and
 * <code>{@link #erosion(Matrix, Pattern) erosion}(m,pattern)</code> method is equivalent to
 * <code>{@link #percentile(Matrix, double, Pattern) percentile}(m,c*<i>N</i>,pattern.{@link Pattern#symmetric()
 * symmetric()})</code>, where <code><i>N</i>=pattern.{@link Pattern#pointCount() pointCount()}-1</code>
 * and <code>c</code> is some constant, specified while instantiating this object
 * (it is <code>dilationLevel</code> argument of
 * {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision) BasicRankMorphology.getInstance}
 * method).</p>
 *
 * <p>Below is the formal definition of 5 basic rank operations with the given pattern <b>P</b>
 * and matrix <b>M</b>: <i>percentile</i>, <i>rank</i>, <i>mean between 2 percentiles</i>,
 * <i>mean between 2 ranks</i> and <i>aperture sum</i>, calculated by this class.</p>
 *
 * <ol>
 * <li>For any integer point, or <i>position</i>
 * <b>x</b> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>),
 * <i>n</i>=<b>M</b>.{@link Matrix#dimCount() dimCount()}, the <i>aperture</i> of this point,
 * or the <i>aperture at the position</i> <b>x</b>,
 * is a set of points
 * <b>x</b>&minus;<b>p</b><sub><i>i</i></sub> = (<i>x</i><sub>0</sub>&minus;<i>p</i><sub><i>i</i>0</sub>,
 * <i>x</i><sub>1</sub>&minus;<i>p</i><sub><i>i</i>1</sub>, ...,
 * <i>x</i><sub><i>n</i>&minus;1</sub>&minus;<i>p</i><sub><i>i</i>,<i>n</i>&minus;1</sub>)
 * for all <b>p</b><sub><i>i</i></sub>&isin;<b>P</b> ({@link Pattern#roundedPoints() points}
 * of the pattern&nbsp;<b>P</b>).
 * We always consider that the point <b>x</b> lies inside <b>M</b> matrix
 * (0&le;<i>x</i><sub><i>k</i></sub>&lt;<b>M</b>.<code>{@link Matrix#dim(int) dim}(<i>k</i>)</code>
 * for all <i>k</i>), but this condition can be not true for points of the aperture
 * <b>x</b>&minus;<b>p</b><sub><i>i</i></sub>.
 * <br>&nbsp;</li>
 *
 * <li>For every point <b>x</b>' = <b>x</b>&minus;<b>p</b><sub><i>i</i></sub> of the aperture
 * we consider the corresponding <i>value</i> <i>v<sub>i</sub></i> of the source matrix <b>M</b>.
 * The precise definition of the value can depend on implementation of this interface.
 * Usually, if the point lies inside the matrix
 * (0&le;<i>x</i><sub><i>k</i></sub>&minus;<i>p</i><sub><i>i,k</i></sub>&lt;<b>M</b>.<code>{@link Matrix#dim(int)
 * dim}(<i>k</i>)</code> for all <i>k</i>), it is the value of the element
 * (integer: {@link PFixedArray#getLong(long)}, if the type of the matrix elements is
 * <code>boolean</code>, <code>char</code>,
 * <code>byte</code>, <code>short</code>, <code>int</code> or <code>long</code>,
 * or real: {@link PArray#getDouble(long)},
 * if the element type is <code>float</code> or <code>double</code>) of the underlying array
 * <b>M</b>.{@link Matrix#array() array()} with an index
 * <b>M</b>.{@link Matrix#index(long...)
 * index}(<i>x</i>'<sub>0</sub>, <i>x</i>'<sub>1</sub>, ..., <i>x</i>'<sub><i>n</i>&minus;1</sub>),
 * where <i>x</i>'<sub><i>k</i></sub> = <i>x</i><sub><i>k</i></sub>&minus;<i>p</i><sub><i>i,k</i></sub>.
 * In particular, it is true for all implementations offered by this package.
 * If the point <b>x</b>' = <b>x</b>&minus;<b>p</b><sub><i>i</i></sub> lies outside the matrix
 * (<i>x</i>'<sub><i>k</i></sub>&lt;0 or
 * <i>x</i>'<sub><i>k</i></sub>&ge;<b>M</b>.<code>{@link Matrix#dim(int) dim}(<i>k</i>)</code>
 * for some <i>k</i>), then:
 *     <ul>
 *     <li>in the {@link BasicRankMorphology} implementation, <i>v<sub>i</sub></i> is the value of the element
 *     ({@link PFixedArray#getLong(long)} for the fixed-point case or {@link PFloatingArray#getDouble(long)}
 *     for the floating-point case) of the underlying array <b>M</b>.{@link Matrix#array() array()}
 *     with an index <b>M</b>.{@link Matrix#pseudoCyclicIndex(long...)
 *     pseudoCyclicIndex}(<i>x</i>'<sub>0</sub>, <i>x</i>'<sub>1</sub>, ...,
 *     <i>x</i>'<sub><i>n</i>&minus;1</sub>);</li>
 *     <li>in the {@link ContinuedRankMorphology} implementation, <i>v<sub>i</sub></i> is the
 *     calculated according to the continuation mode, passed to
 *     {@link ContinuedRankMorphology#getInstance(RankMorphology, Matrix.ContinuationMode)}
 *     method;</li>
 *     <li>other implementations may offer other ways for calculating <i>v<sub>i</sub></i>.
 *     <br>&nbsp;</li>
 *     </ul>
 * </li>
 *
 * <li>So, for every point <b>x</b> we have an aperture, consisting of
 * <i>N</i>=<b>P</b>.{@link Pattern#pointCount() pointCount()} "neighbour" points
 * <b>x</b>&minus;<b>p</b><sub><i>i</i></sub>,
 * and a corresponding set of integer or real values <i>v<sub>i</sub></i>,
 * <i>i</i>=0,1,...,<i>N</i>&minus;1. Then we consider a <i>histogram</i> built on the base of
 * <i>v<sub>i</sub></i> values &mdash; the histogram, corresponding to the point <b>x</b>. Namely, this histogram
 * is an array of non-negative integer numbers <b>b</b>[<i>w</i>], 0&le;<i>w</i>&lt;<i>M</i>,
 * where every element <b>b</b>[<i>w</i>] represents the number of occurrence of the value <i>w</i>
 * in array <b>A</b>, consisting of the following <i>N</i> integer elements
 * <i>a</i><sub>0</sub>, <i>a</i><sub>1</sub>, ..., <i>x</i><sub><i>N</i>&minus;1</sub>:
 *     <ul>
 *     <li><i>a<sub>i</sub></i> = min(<i>M</i>&minus;1,
 *     &lfloor;max(0, <i>v<sub>i</sub></i>) * &sigma;&rfloor;)
 *     (here and below &lfloor;<i>y</i>&rfloor; means the integer part of <i>y</i> or <code>(long)</code><i>y</i>
 *     for non-negative numbers, <i>M</i> and &sigma; are defined below);
 *     in other words, <i>a<sub>i</sub></i> is an integer part of &sigma;<i>v<sub>i</sub></i>,
 *     truncated to 0..<i>M</i>&minus;1 range of allowed histogram indexes;</li>
 *
 *     <li>the <i>histogram length M</i> is the global parameter of this object and is equal to some power of two:
 *     <i>M</i>=2<sup>&mu;</sup>, &mu;=0,1,2... There is a guarantee that
 *     if the matrix is fixed-point (<b>M</b>.{@link Matrix#elementType()} is
 *     <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
 *     <code>int</code> or <code>long</code>),
 *     then &mu;&le;&beta;, where &beta; is
 *     the number of bits per element:
 *     &beta; = <b>M</b>.{@link Matrix#array() array()}.{@link PArray#bitsPerElement() bitsPerElement()}.
 *     If this object is an instance of {@link BasicRankMorphology}, then
 *     &mu; = <code>bitLevels[bitLevels.length-1]</code>
 *     for floating-point matrix elements (<b>M</b>.{@link Matrix#elementType()} is
 *     <code>float</code> or <code>double</code>) or
 *     &mu; = min(<code>bitLevels[bitLevels.length-1]</code>, &beta;)
 *     for fixed-point matrix elements, where <code>bitLevels</code> is
 *     an array of <i>{@link CustomRankPrecision#bitLevels() bit levels}</i>, specified while instantiating
 *     this class via the last argument of
 *     {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision)} method;</li>
 *
 *     <li>the real number &sigma; ("scale") is equal to <i>M</i> for a floating-point matrix
 *     or equal to <i>M</i>/2<sup>&beta;</sup>=2<sup>&mu;&minus;&beta;</sup>,
 *     &beta; = <b>M</b>.{@link Matrix#array() array()}.{@link PArray#bitsPerElement()
 *     bitsPerElement()} for a fixed-point matrix. So, in the case of a fixed-point matrix
 *     there is a guarantee that 1/&sigma; is a positive integer number
 *     (2<sup>&beta;&minus;&mu;</sup>).</li>
 *     </ul>
 * In other words, the "standard" allowed range of element values
 * <code>0..{@link Arrays#maxPossibleValue(Class, double)
 * Arrays.maxPossibleValue}(<b>M</b>.{@link Matrix#elementType() elementType()},1.0)</code>
 * is split into <i>M</i>=2<sup>&mu;</sup> histogram bars and all aperture values
 * <i>v<sub>i</sub></i> are distributed between these bars; elements, which are out of the allowed range,
 * are distributed between the first and last bars.
 * In the simplest case of <code>byte</code> elements, <i>M</i> is usually chosen to be 256;
 * then &sigma;=1.0 and <i>a<sub>i</sub></i> = <i>v<sub>i</sub></i>
 * (because <code>byte</code> elements, in terms of AlgART libraries, cannot be out of 0..255 range).
 * <br>&nbsp;</li>
 *
 * <li>The histogram <b>b</b>[0], <b>b</b>[1], ..., <b>b</b>[<i>M</i>&minus;1], specified above,
 * is interpreted in terms of {@link Histogram} and {@link SummingHistogram} classes.
 * Namely, we define the following <i>rank characteristics</i>:
 *     <ul>
 *     <li>the <b><i>percentile</i></b> with the given real index <i>r</i> is
 *     <i>v</i>(<i>r</i>)/&sigma;,
 *     where <i>v</i>(<i>r</i>) function is defined in comments to {@link Histogram} class;</li>
 *
 *     <li>the <b><i>rank</i></b> of the given real value <i>v</i> is <i>r</i>(<i>v</i>*&sigma;),
 *     where <i>r</i>(<i>v</i>) function is defined in comments to {@link Histogram} class;</li>
 *
 *     <li>the <b><i>mean between 2 given percentiles</i></b> with the real indexes
 *     <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> is
 *     (<i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>)) /
 *     ((<i>r</i><sub>2</sub>&minus;<i>r</i><sub>1</sub>)*&sigma;)
 *     if <i>r</i><sub>1</sub>&lt;<i>r</i><sub>2</sub> or some reserved value <i>filler</i>
 *     if <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>,
 *     where <i>S</i>(<i>r</i>) function is defined in comments to {@link SummingHistogram} class;</li>
 *
 *     <li>the <b><i>mean between 2 given values</i></b>, the real numbers
 *     <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub>, is
 *     (<i>s</i>(<i>v</i><sub>2</sub>*&sigma;)&minus;<i>s</i>(<i>v</i><sub>1</sub>*&sigma;)) /
 *     (<i>r</i>(<i>v</i><sub>2</sub>*&sigma;)&minus;<i>r</i>(<i>v</i><sub>1</sub>*&sigma;))*&sigma;)
 *     if <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
 *     <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&lt;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
 *     where <i>r</i>(<i>v</i>) function is defined in comments to {@link Histogram} class and
 *     <i>s</i>(<i>v</i>) function is defined in comments to {@link SummingHistogram} class.
 *     If the conditions <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
 *     <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&lt;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;) are not
 *     fulfilled, we use one of 4 following <i>modes of calculation</i>:
 *         <ol type="A">
 *         <li>if any of these two conditions is not fulfilled, it is equal to some reserved value <i>filler</i>;</li>
 *         <li>if any of these two conditions is not fulfilled, it is equal to <i>v</i><sub>1</sub>;</li>
 *         <li>if any of these two conditions is not fulfilled, it is equal to <i>v</i><sub>2</sub>;</li>
 *         <li>(most useful definition)
 *             <ol type="I">
 *             <li>if <i>v</i><sub>1</sub>&ge;<i>v</i><sub>2</sub>, it is equal to
 *             (<i>v</i><sub>1</sub>+<i>v</i><sub>2</sub>)/2;</li>
 *             <li>if <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
 *             <i>r</i>(<i>v</i><sub>2</sub>*&sigma;)=<i>r</i>(<i>v</i><sub>1</sub>*&sigma;)=0,
 *             it is equal to <i>v</i><sub>2</sub> (see also
 *             {@link net.algart.arrays.SummingHistogram.CountOfValues#isLeftBound()
 *             CountOfValues.isLeftBound()});</li>
 *             <li>if <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
 *             <i>r</i>(<i>v</i><sub>2</sub>*&sigma;)=<i>r</i>(<i>v</i><sub>1</sub>*&sigma;)=<i>N</i>,
 *             it is equal to <i>v</i><sub>1</sub> (see also
 *             {@link net.algart.arrays.SummingHistogram.CountOfValues#isRightBound()
 *             CountOfValues.isRightBound()});</li>
 *             <li>in other cases, it is equal to
 *             (<i>v</i><sub>1</sub>+<i>v</i><sub>2</sub>)/2.</li>
 *             </ol>
 *         </li>
 *         </ol>
 *     Remember that <i>r</i>(<i>v</i>) is always a non-decreasing function, so, the sentence
 *     "the conditions <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
 *     <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&lt;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;) are not
 *     fulfilled" means, that either <i>v</i><sub>1</sub>&ge;<i>v</i><sub>2</sub>,
 *     or <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
 *     <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)=<i>r</i>(<i>v</i><sub>2</sub>*&sigma;).
 *     The last situation usually occurs "outside" the histogram, when both <i>v</i><sub>1</sub> and
 *     <i>v</i><sub>2</sub> values are less than (or equal to) the minimal value in the aperture
 *     or greater than (or equal to) the maximum value in the aperture.
 *     However, while using the simple histogram model, such situation is also possible on a sparse histogram
 *     with many zero bars;
 *     </li>
 *     <li>the <b><i>aperture sum</i></b> is just a usual sum of all values
 *     <i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub> &mdash;
 *     the only rank characteristic which does not use the histogram <b>b</b>[<i>k</i>].
 *     (Of course, you can also calculate the <i>mean</i> of all values on the base of this sum:
 *     it is enough to divide the sum by <i>N</i>.)</li>
 *     </ul>
 * <b>Note 1:</b> it is obvious that all rank characteristics, excepting the <i>aperture sum</i>, depend on the
 * <i>histogram model</i>: simple or precise (see comments to {@link Histogram} and {@link SummingHistogram} classes).
 * The used model is chosen while instantiating this class, usually via
 * {@link CustomRankPrecision#interpolated()} flag in the argument of {@link CustomRankPrecision} class:
 * <code>true</code> value ("interpolated") means the precise model, <code>false</code> means the simple one.
 * <br>
 * <b>Note 2</b>: for <code>int</code>, <code>long</code>,
 * <code>float</code> and <code>double</code> element type of the source
 * matrix <b>M</b> and for all characteristics, excepting the <i>aperture sum</i>, this definition supposes that
 * all matrix elements lie in the "standard range": <code>0..Integer/Long.MAX_VALUE</code>
 * (all non-negative values) for integers or <code>0.0..1.0</code> for floating-point elements.
 * If some matrix elements are negative, they are interpreted as 0; if some elements of a floating-point
 * matrix are greater than <code>1.0</code>, they are interpreted as <code>1.0</code>.
 * For floating-point case, the histogram length <i>M</i> actually specifies
 * the <i>precision</i> of calculations: the source elements are represented with the precision
 * 1/<i>M</i>. The <i>aperture sum</i> characteristic is an exception from this rule: aperture sums
 * are calculated as usual sums of the source elements <i>v<sub>i</sub></i> with <code>double</code> precision.
 * <br>
 * <b>Note 3:</b> for floating-point case, if the value of some element <i>v<sub>i</sub></i>
 * of the source matrix <b>M</b> inside the aperture is <b>NaN</b>
 * (<code>Float.NaN</code> for <code>float</code> type, <code>Double.NaN</code> for <code>double</code> type),
 * this situation does not lead to an exception, but the resulting values of all characteristics, listed above,
 * are undocumented.
 * <br>
 * <b>Note 4:</b> unlike this, if some arguments of the characteristics, listed above &mdash;
 * the real index <i>r</i> for the percentile, the real value <i>v</i> for the rank,
 * the real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> for the mean between percentiles
 * or the real values <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> for the mean between values &mdash;
 * are <b>NaN</b>, then any attempt to calculate these characteristics by methods of this interface
 * <b>can lead to <code>IllegalArgumentException</code></b>.
 * </li>
 * </ol>
 *
 * <p>This interface provides method for calculating the described 5 rank characteristics for every
 * integer point <b>x</b> lying in the matrix <b>M</b>. The resulting characteristics are returned
 * in the result matrix with the same dimensions as <b>M</b>. The necessary arguments &mdash;
 * the real index <i>r</i> for the percentile, the real value <i>v</i> for the rank,
 * the real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> for the mean between percentiles
 * or the real values <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> for the mean between values &mdash;
 * are retrieved from the corresponding element (with the index
 * <b>M</b>.{@link Matrix#index(long...)
 * index}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>))
 * of the {@link Matrix#array() built-in array} of some additional matrix or pair of matrices,
 * passed to the methods, by {@link PArray#getDouble(long)} method. It is supposed that those matrices
 * have the same dimensions as <b>M</b>. For the percentile and the mean between percentiles,
 * there are simplified versions of the methods, which use the constant rank indexes instead of matrices of
 * indexes.</p>
 *
 * <p>Most of methods of this interface allow to return not only floating-point, but also integer result matrix.
 * In this case, the real rank characteristics, defined above in the section 4, are rounded by some rules,
 * that are specified in comments to the concrete methods.</p>
 *
 * <p>For every rank characteristics this interface offers 3 methods for calculating it. Below are some comments
 * about them.</p>
 * <ul>
 * <li>The first version is always called "<code>as<i>Operation</i></code>", for example,
 * {@link #asPercentile(Matrix, Matrix, Pattern) asPercentile}.
 * This method returns an immutable view of the passed source matrix,
 * such that any reading data from it calculates and returns the necessary rank characteristic
 * of the source matrix with the specified pattern (aperture shape).
 * The result of such method is usually "lazy", that means that this method finishes immediately and all
 * actual calculations are performed while getting elements of the returned matrix.
 * It is true for all implementations provided by this package.
 * However, some implementations may not support lazy execution;
 * then the method may be equivalent to the second version described below.
 * Note that the sequential access to the lazy result, returned by this method
 * (via {@link Array#getData(long, Object, int, int) Array.getData} method, called for the
 * {@link Matrix#array() built-in array} of the returned matrix), usually works much faster
 * than the random access to elements of the matrix.</li>
 *
 * <li>The second version is called "<code><i>operation</i></code>", for example,
 * {@link #percentile(Matrix, Matrix, Pattern) percentile}.
 * This method always returns actual (non-lazy) updatable result:
 * <code>Matrix&lt;? extends UpdatablePArray&gt;</code>.
 * This method can work essentially faster than an access to
 * the lazy matrix returned by the first variant of the method
 * (for example, than copying it into a new matrix).
 * In implementations, offered by this package, there are no difference if the source
 * matrix <code>src</code> (for rank operations, <code>baseMatrix</code>) is direct accessible:
 * <code>src.{@link Matrix#array() array()} instanceof {@link DirectAccessible} &amp;&amp;
 * ((DirectAccessible)src.{@link Matrix#array() array()}).{@link DirectAccessible#hasJavaArray()
 * hasJavaArray()}</code>. If the source matrix is not direct accessible,
 * the implementations, offered by this package, use {@link net.algart.matrices.StreamingApertureProcessor} technique
 * to accelerate processing. Calculating <i>aperture sums</i> is an exception:
 * {@link #functionOfSum(Matrix, Pattern, Func) functionOfSum} method uses some optimization for some kinds of
 * patterns and can work much faster than accessing to {@link #asFunctionOfSum(Matrix, Pattern, Func) asFunctionOfSum}
 * result.</li>
 *
 * <li>The third version is also called "<code><i>operation</i></code>", but it is a void method:
 * for example, {@link #percentile(Matrix, Matrix, Matrix, Pattern)}.
 * The result matrix is passed via first argument named <code>dest</code> and supposed to be allocated
 * before calling the method. This way allows to save memory and time, if you need to perform several
 * rank operation, because you can use the same matrices for temporary results.
 * In addition, these methods allow to choose the type of element of the resulting matrix for any
 * operation. The precise rules of type conversions are described in comments to concrete methods.
 * </li>
 * </ul>
 *
 * <p>This package provides the following basic methods for creating objects, implementing this interface:</p>
 *
 * <ul>
 * <li>{@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision)};</li>
 * <li>{@link ContinuedRankMorphology#getInstance(RankMorphology, Matrix.ContinuationMode)}.</li>
 * </ul>
 *
 * <p><b>Warning</b>: all implementations of this interface, provided by this package, can process only patterns
 * where <code>{@link Pattern#pointCount() pointCount()}&le;Integer.MAX_VALUE</code>.
 * More precisely, any methods of this interface (including methods, inherited from its superinterface
 * {@link Morphology}), implemented by classes of this package, which have {@link Pattern} argument,
 * can throw {@link net.algart.math.patterns.TooManyPointsInPatternError TooManyPointsInPatternError}
 * or <code>OutOfMemoryError</code> in the same situations as {@link Pattern#points()} method.</p>
 *
 * <p><b>Warning</b>: the methods of this interface, which save results into the passed <code>dest</code>
 * matrix (like {@link #percentile(Matrix, Matrix, Matrix, Pattern)}),
 * as well as any attempts to read the "lazy" results (of the methods
 * like {@link #asPercentile(Matrix, Matrix, Pattern)}),
 * <b>can be non-atomic regarding the failure</b>, if the arguments of the calculated rank characteristics &mdash;
 * the real index <i>r</i> for the percentile, the real value <i>v</i> for the rank,
 * the real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> for the mean between percentiles
 * or the real values <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> for the mean between values &mdash;
 * are floating-point <b>NaN</b> values for some aperture positions. In this case,
 * it is possible that the result will be partially filled, and only after this the <b>NaN</b> value will lead to
 * <code>IllegalArgumentException</code>.</p>
 *
 * <p>The classes, implementing this interface, are <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public interface RankMorphology extends Morphology {

    /**
     * Special value of <code>filler</code> argument of methods, calculating <i>mean between 2 values</i>,
     * which activates the mode B of calculation:
     * if <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     * the mean is considered to be equal <i>v</i><sub>1</sub>.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 values</i>" term.
     *
     * <p>This constant can be used with {@link #asMeanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)},
     * {@link #meanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)} and
     * {@link #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)} methods.
     *
     * <p>This constant contains <code>Double.NEGATIVE_INFINITY</code> value,
     * which is usually useless in a role of the ordinary <i>filler</i> for the mode A of calculations.
     */
    double FILL_MIN_VALUE = Double.NEGATIVE_INFINITY;

    /**
     * Special value of <code>filler</code> argument of methods, calculating <i>mean between 2 values</i>,
     * which activates the mode C of calculation:
     * if <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     * the mean is considered to be equal <i>v</i><sub>2</sub>.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 values</i>" term.
     *
     * <p>This constant can be used with {@link #asMeanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)},
     * {@link #meanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)} and
     * {@link #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)} methods.
     *
     * <p>This constant contains <code>Double.POSITIVE_INFINITY</code> value,
     * which is usually useless in a role of the ordinary <i>filler</i> for the mode A of calculations.
     */
    double FILL_MAX_VALUE = Double.POSITIVE_INFINITY;

    /**
     * Special value of <code>filler</code> argument of methods, calculating <i>mean between 2 values</i>,
     * which activates the mode D of calculation.
     * In this case:
     * <ol type="I">
     * <li>if <i>v</i><sub>1</sub>&ge;<i>v</i><sub>2</sub>, the mean is considered to be equal to
     * (<i>v</i><sub>1</sub>+<i>v</i><sub>2</sub>)/2;</li>
     * <li>if <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
     * <i>r</i>(<i>v</i><sub>2</sub>*&sigma;)=<i>r</i>(<i>v</i><sub>1</sub>*&sigma;)=0,
     * the mean is considered to be equal to <i>v</i><sub>2</sub> (see also
     * {@link net.algart.arrays.SummingHistogram.CountOfValues#isLeftBound()
     * CountOfValues.isLeftBound()});</li>
     * <li>if <i>v</i><sub>1</sub>&lt;<i>v</i><sub>2</sub> and
     * <i>r</i>(<i>v</i><sub>2</sub>*&sigma;)=<i>r</i>(<i>v</i><sub>1</sub>*&sigma;)=<i>N</i>,
     * the mean is considered to be equal to <i>v</i><sub>1</sub> (see also
     * {@link net.algart.arrays.SummingHistogram.CountOfValues#isRightBound()
     * CountOfValues.isRightBound()});</li>
     * <li>in other cases,
     * if <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)=<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     * the mean is considered to be equal to
     * (<i>v</i><sub>1</sub>+<i>v</i><sub>2</sub>)/2.</li>
     * </ol>
     * <p>
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 values</i>" term.
     *
     * <p>This constant can be used with {@link #asMeanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)},
     * {@link #meanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)} and
     * {@link #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)} methods.
     *
     * <p>This constant contains <code>Double.NaN</code> value, which is usually useless in a role of the ordinary
     * <i>filler</i> for the mode A of calculations.
     *
     * <p><b>Warning:</b> because this constant is <code>Double.NaN</code>, according to rules of Java language,
     * you <b>must not compare any numbers with this constant</b>! A check like
     * <pre>
     * if (x == <code>RankMorphology.FILL_NEAREST_VALUE</code>) {
     *     ...
     * }
     * </pre>
     * always returns <code>false</code>. You should use <code>Double.isNaN(x)</code> call instead.
     */
    double FILL_NEAREST_VALUE = Double.NaN;

    RankMorphology context(ArrayContext newContext);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>percentile</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real index <i>r</i> of the percentile for every element of the result is equal
     * to the corresponding element of <code>percentileIndexes</code> matrix.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real percentile <i>v</i>(<i>r</i>)/&sigma;,
     * defined in the comments to this class.
     * In this case, this method, instead of the real value <i>v</i>(<i>r</i>)/&sigma;,
     * returns an integer <i>w</i>/&sigma;, where <i>w</i> is:
     * <ul>
     * <li>either the integer result of {@link Histogram#iValue(long[], long)
     * Histogram.iValue} (<b>b</b>, <code>(long)Math.floor</code>(<i>r</i>)),
     * if this object works in the simple histogram model
     * (where <b>b</b> is the histogram, corresponding to every aperture position),</li>
     * <li>or the integer result of {@link Histogram#iPreciseValue(long[], double)
     * Histogram.iPreciseValue} (<b>b</b>, <i>r</i>),
     * if it works in the precise histogram model.</li>
     * </ul>
     * It is necessary to remind: in a case of fixed-point elements
     * there is a guarantee that 1/&sigma; is a positive integer number, and
     * <i>w</i>/&sigma;=<i>w</i>*2<sup>&beta;&minus;&mu;</sup>,
     * &beta; = <code>src.{@link Matrix#array() array()}.{@link PArray#bitsPerElement()
     * bitsPerElement()}</code>,
     * &mu; is the binary logarithm of the histogram length <i>M</i> &mdash;
     * see the {@link RankMorphology comments to this class}, section 3.
     * So, if &mu; is chosen less than the precision of this matrix &beta;
     * (8 for <code>byte</code>, 16 for <code>short</code>, etc.), then &beta;&minus;&mu;
     * lowest bits in the result will be always zero.
     * For {@link BasicRankMorphology} object, &mu; is chosen while
     * {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision) instantiating this object}
     * as min(<code>bitLevels[bitLevels.length-1]</code>, &beta;).
     *
     * @param src               the source matrix.
     * @param percentileIndexes the matrix containing <i>r</i> argument: the indexes of the percentile
     *                          for every element of the result.
     * @param pattern           the pattern: the shape of the aperture.
     * @return the "lazy" matrix containing the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asPercentile(Matrix, double, Pattern)
     * @see #percentile(Matrix, Matrix, Pattern)
     * @see #percentile(Matrix, Matrix, Matrix, Pattern)
     */
    Matrix<? extends PArray> asPercentile(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes, Pattern pattern);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>percentile</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real index <i>r</i> of the percentile is equal to <code>percentileIndex</code> argument for
     * all aperture positions. The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #asPercentile(Matrix, Matrix, Pattern)
     * asPercentile}(src, src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex), pattern)</pre>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real percentile <i>v</i>(<i>r</i>)/&sigma;,
     * defined in the comments to this class.
     * In this case, this method, instead of the real value <i>v</i>(<i>r</i>)/&sigma;,
     * returns an integer <i>w</i>/&sigma;, where <i>w</i> is:
     * <ul>
     * <li>either the integer result of {@link Histogram#iValue(long[], long)
     * Histogram.iValue} (<b>b</b>, <code>(long)Math.floor</code>(<i>r</i>)),
     * if this object works in the simple histogram model
     * (where <b>b</b> is the histogram, corresponding to every aperture position),</li>
     * <li>or the integer result of {@link Histogram#iPreciseValue(long[], double)
     * Histogram.iPreciseValue} (<b>b</b>, <i>r</i>),
     * if it works in the precise histogram model.</li>
     * </ul>
     * It is necessary to remind: in a case of fixed-point elements
     * there is a guarantee that 1/&sigma; is a positive integer number, and
     * <i>w</i>/&sigma;=<i>w</i>*2<sup>&beta;&minus;&mu;</sup>,
     * &beta; = <code>src.{@link Matrix#array() array()}.{@link PArray#bitsPerElement()
     * bitsPerElement()}</code>,
     * &mu; is the binary logarithm of the histogram length <i>M</i> &mdash;
     * see the {@link RankMorphology comments to this class}, section 3.
     * So, if &mu; is chosen less than the precision of this matrix &beta;
     * (8 for <code>byte</code>, 16 for <code>short</code>, etc.), then &beta;&minus;&mu;
     * lowest bits in the result will be always zero.
     * For {@link BasicRankMorphology} object, &mu; is chosen while
     * {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision) instantiating this object}
     * as min(<code>bitLevels[bitLevels.length-1]</code>, &beta;).
     *
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @return the "lazy" matrix containing the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asPercentile(Matrix, Matrix, Pattern)
     * @see #percentile(Matrix, double, Pattern)
     * @see #percentile(Matrix, Matrix, double, Pattern)
     */
    Matrix<? extends PArray> asPercentile(
            Matrix<? extends PArray> src,
            double percentileIndex, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>percentile</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real index <i>r</i> of the percentile for every element of the result is equal
     * to the corresponding element of <code>percentileIndexes</code> matrix.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real percentile <i>v</i>(<i>r</i>)/&sigma;,
     * defined in the comments to this class.
     * In this case, this method, instead of the real value <i>v</i>(<i>r</i>)/&sigma;,
     * returns an integer <i>w</i>/&sigma;, where <i>w</i> is:
     * <ul>
     * <li>either the integer result of {@link Histogram#iValue(long[], long)
     * Histogram.iValue} (<b>b</b>, <code>(long)Math.floor</code>(<i>r</i>)),
     * if this object works in the simple histogram model
     * (where <b>b</b> is the histogram, corresponding to every aperture position),</li>
     * <li>or the integer result of {@link Histogram#iPreciseValue(long[], double)
     * Histogram.iPreciseValue} (<b>b</b>, <i>r</i>),
     * if it works in the precise histogram model.</li>
     * </ul>
     * It is necessary to remind: in a case of fixed-point elements
     * there is a guarantee that 1/&sigma; is a positive integer number, and
     * <i>w</i>/&sigma;=<i>w</i>*2<sup>&beta;&minus;&mu;</sup>,
     * &beta; = <code>src.{@link Matrix#array() array()}.{@link PArray#bitsPerElement()
     * bitsPerElement()}</code>,
     * &mu; is the binary logarithm of the histogram length <i>M</i> &mdash;
     * see the {@link RankMorphology comments to this class}, section 3.
     * So, if &mu; is chosen less than the precision of this matrix &beta;
     * (8 for <code>byte</code>, 16 for <code>short</code>, etc.), then &beta;&minus;&mu;
     * lowest bits in the result will be always zero.
     * For {@link BasicRankMorphology} object, &mu; is chosen while
     * {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision) instantiating this object}
     * as min(<code>bitLevels[bitLevels.length-1]</code>, &beta;).
     *
     * @param src               the source matrix.
     * @param percentileIndexes the matrix containing <i>r</i> argument: the indexes of the percentile
     *                          for every element of the result.
     * @param pattern           the pattern: the shape of the aperture.
     * @return the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #percentile(Matrix, double, Pattern)
     * @see #asPercentile(Matrix, Matrix, Pattern)
     * @see #percentile(Matrix, Matrix, Matrix, Pattern)
     */
    Matrix<? extends UpdatablePArray> percentile(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>percentile</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real index <i>r</i> of the percentile is equal to <code>percentileIndex</code> argument for
     * all aperture positions. The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #percentile(Matrix, Matrix, Pattern)
     * percentile}(src, src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex), pattern)</pre>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real percentile <i>v</i>(<i>r</i>)/&sigma;,
     * defined in the comments to this class.
     * In this case, this method, instead of the real value <i>v</i>(<i>r</i>)/&sigma;,
     * returns an integer <i>w</i>/&sigma;, where <i>w</i> is:
     * <ul>
     * <li>either the integer result of {@link Histogram#iValue(long[], long)
     * Histogram.iValue} (<b>b</b>, <code>(long)Math.floor</code>(<i>r</i>)),
     * if this object works in the simple histogram model
     * (where <b>b</b> is the histogram, corresponding to every aperture position),</li>
     * <li>or the integer result of {@link Histogram#iPreciseValue(long[], double)
     * Histogram.iPreciseValue} (<b>b</b>, <i>r</i>),
     * if it works in the precise histogram model.</li>
     * </ul>
     * It is necessary to remind: in a case of fixed-point elements
     * there is a guarantee that 1/&sigma; is a positive integer number, and
     * <i>w</i>/&sigma;=<i>w</i>*2<sup>&beta;&minus;&mu;</sup>,
     * &beta; = <code>src.{@link Matrix#array() array()}.{@link PArray#bitsPerElement()
     * bitsPerElement()}</code>,
     * &mu; is the binary logarithm of the histogram length <i>M</i> &mdash;
     * see the {@link RankMorphology comments to this class}, section 3.
     * So, if &mu; is chosen less than the precision of this matrix &beta;
     * (8 for <code>byte</code>, 16 for <code>short</code>, etc.), then &beta;&minus;&mu;
     * lowest bits in the result will be always zero.
     * For {@link BasicRankMorphology} object, &mu; is chosen while
     * {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision) instantiating this object}
     * as min(<code>bitLevels[bitLevels.length-1]</code>, &beta;).
     *
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @return the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #percentile(Matrix, Matrix, Pattern)
     * @see #asPercentile(Matrix, double, Pattern)
     * @see #percentile(Matrix, Matrix, double, Pattern)
     */
    Matrix<? extends UpdatablePArray> percentile(
            Matrix<? extends PArray> src,
            double percentileIndex, Pattern pattern);

    /**
     * Equivalent to {@link #percentile(Matrix, Matrix, Pattern)} method, but the result matrix
     * will be placed in the <code>dest</code> argument.
     *
     * <p>If the {@link Matrix#elementType() element type} of the passed <code>dest</code> matrix
     * is the same as the element type of the source one, the result, saved in <code>dest</code>, will be identically
     * equal to the result of {@link #percentile(Matrix, Matrix, Pattern)} method with the same
     * <code>src</code>, <code>percentileIndexes</code> and <code>pattern</code> arguments. In other case,
     * the result, saved in <code>dest</code>, will be equal to
     *
     * <pre>{@link Matrices#asFuncMatrix(boolean, Func, Class, Matrix)
     * Matrices.asFuncMatrix}(true, {@link Func#IDENTITY Func.IDENTITY},
     * &#32;   dest.array().{@link Array#type() type()}, {@link #percentile(Matrix, Matrix, Pattern)
     * percentile}(src, percentileIndexes, pattern))</pre>
     *
     * <p>So, even if the precision of <code>dest</code> matrix is better than the precision
     * of <code>src</code> &mdash;
     * for example, if <code>src.{@link Matrix#elementType() elementType()}</code> is <code>byte</code>, but
     * <code>dest.{@link Matrix#elementType() elementType()}</code> is <code>double</code> &mdash;
     * this method does not try to calculate more precise percentile and rounds results like
     * {@link #asPercentile(Matrix, Matrix, Pattern)} and
     * {@link #percentile(Matrix, Matrix, Pattern)} methods.
     *
     * @param dest              the target matrix.
     * @param src               the source matrix.
     * @param percentileIndexes the matrix containing <i>r</i> argument: the indexes of the percentile
     *                          for every element of the result.
     * @param pattern           the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #percentile(Matrix, Matrix, double, Pattern)
     * @see #asPercentile(Matrix, Matrix, Pattern)
     * @see #percentile(Matrix, Matrix, Pattern)
     */
    void percentile(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes, Pattern pattern);

    /**
     * Equivalent to {@link #percentile(Matrix, double, Pattern)} method, but the result matrix
     * will be placed in the <code>dest</code> argument.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #percentile(Matrix, Matrix, Matrix, Pattern)
     * percentile}(dest, src, src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex), pattern)</pre>
     *
     * @param dest            the target matrix.
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #percentile(Matrix, Matrix, Matrix, Pattern)
     * @see #asPercentile(Matrix, double, Pattern)
     * @see #percentile(Matrix, double, Pattern)
     */
    void percentile(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            double percentileIndex, Pattern pattern);

    /**
     * Returns an immutable view of the passed <code>baseMatrix</code> matrix,
     * such that any reading data from it calculates and returns the <i>ranks</i>
     * of some given values <i>v</i> regarding the source matrix <code>baseMatrix</code> with the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>rank</i>" term.
     * The real value <i>v</i> for every element of the result is equal
     * to the corresponding element of <code>rankedMatrix</code> matrix.
     *
     * <p>The matrix, returned by this method, is immutable, and the class of its built-in array
     * implements one of the basic interfaces
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray} or {@link DoubleArray}.
     * The class of desired interface (one of 8 possible classes) must be passed as <code>requiredType</code> argument.
     * So, it defines the element type of the returned matrix.
     * The rules of casting the floating-point ranks to the desired element type are the same as in
     * {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>The element types of <code>baseMatrix</code> and <code>rankedMatrix</code> are usually equal.
     * If they are different (<code>baseMatrix.{@link Matrix#elementType()
     * elementType()}!=rankedMatrix.{@link Matrix#elementType() elementType()}</code>),
     * this method replaces <code>rankedMatrix</code> with
     *
     * <pre>{@link Matrices#asFuncMatrix(boolean, Func, Class, Matrix)
     * Matrices.asFuncMatrix}(true, {@link Func#IDENTITY
     * Func.IDENTITY}, baseMatrix.array().{@link Array#type() type()}, rankedMatrix)</pre>
     *
     * <p>before all other calculations. In other words, this method always casts the type of
     * the ranked elements to the type of <code>baseMatrix</code> elements.
     * As a result, we can be sure, that if the source <code>baseMatrix</code> matrix is fixed-point
     * (<code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> elements),
     * then the rank <i>r</i>(<i>v</i>*&sigma;), according to the definition of the "rank" term,
     * will be an integer number. In this case, you can specify <code>requiredType=IntArray.class</code> and
     * get the precise rank without precision loss. Moreover, if you know that the number of points in the pattern
     * (<code>pattern.{@link Pattern#pointCount() pointCount()}</code>) is less than
     * 2<sup>16</sup>=65536 or 2<sup>8</sup>=256, it is enough to specify correspondingly
     * <code>requiredType=ShortArray.class</code> or <code>ByteArray.class</code>.
     *
     * @param requiredType the desired type of the built-in array in the returned matrix.
     * @param baseMatrix   the source matrix.
     * @param rankedMatrix the matrix containing <i>v</i> argument: the values,
     *                     the rank of which should be calculated.
     * @param pattern      the pattern: the shape of the aperture.
     * @return the "lazy" matrix containing the rank of the given values.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>baseMatrix.{@link Matrix#dimCount() dimCount()}</code>,
     *                                  or if <code>requiredType</code> is not one of classes
     *                                  <code>{@link BitArray}.class</code>, <code>{@link CharArray}.class</code>,
     *                                  <code>{@link ByteArray}.class</code>, <code>{@link ShortArray}.class</code>,
     *                                  <code>{@link IntArray}.class</code>, <code>{@link LongArray}.class</code>,
     *                                  <code>{@link FloatArray}.class</code> or <code>{@link DoubleArray}.class</code>.
     * @see #rank(Class, Matrix, Matrix, Pattern)
     * @see #rank(Matrix, Matrix, Matrix, Pattern)
     */
    <T extends PArray> Matrix<T> asRank(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>rank</i>
     * of some given values <i>v</i> regarding the source matrix <code>baseMatrix</code> with the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>rank</i>" term.
     * The real value <i>v</i> for every element of the result is equal
     * to the corresponding element of <code>rankedMatrix</code> matrix.
     *
     * <p>The matrix, returned by this method, is updatable, and the class of its built-in array
     * implements one of the basic interfaces
     * {@link UpdatableBitArray}, {@link UpdatableCharArray},
     * {@link UpdatableByteArray}, {@link UpdatableShortArray},
     * {@link UpdatableIntArray}, {@link UpdatableLongArray},
     * {@link UpdatableFloatArray} or {@link UpdatableDoubleArray}.
     * The class of desired interface (one of 8 possible classes) must be passed as <code>requiredType</code> argument.
     * So, it defines the element type of the returned matrix.
     * Instead of these classes, you can also pass one of corresponding immutable interfaces
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray} or {@link DoubleArray}:
     * the result will be the same.
     * The rules of casting the floating-point ranks to the desired element type are the same as in
     * {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>The element types of <code>baseMatrix</code> and <code>rankedMatrix</code> are usually equal.
     * If they are different (<code>baseMatrix.{@link Matrix#elementType()
     * elementType()}!=rankedMatrix.{@link Matrix#elementType() elementType()}</code>),
     * this method replaces <code>rankedMatrix</code> with
     *
     * <pre>{@link Matrices#asFuncMatrix(boolean, Func, Class, Matrix)
     * Matrices.asFuncMatrix}(true, {@link Func#IDENTITY
     * Func.IDENTITY}, baseMatrix.array().{@link Array#type() type()}, rankedMatrix)</pre>
     *
     * <p>before all other calculations. In other words, this method always casts the type of
     * the ranked elements to the type of <code>baseMatrix</code> elements.
     * As a result, we can be sure, that if the source <code>baseMatrix</code> matrix is fixed-point
     * (<code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> elements),
     * then the rank <i>r</i>(<i>v</i>*&sigma;), according to the definition of the "rank" term,
     * will be an integer number. In this case, you can specify <code>requiredType=IntArray.class</code> and
     * get the precise rank without precision loss. Moreover, if you know that the number of points in the pattern
     * (<code>pattern.{@link Pattern#pointCount() pointCount()}</code>) is less than
     * 2<sup>16</sup>=65536 or 2<sup>8</sup>=256, it is enough to specify correspondingly
     * <code>requiredType=ShortArray.class</code> or <code>ByteArray.class</code>.
     * The less result precision allows you to save memory.
     *
     * @param requiredType the desired type of the built-in array in the returned matrix.
     * @param baseMatrix   the source matrix.
     * @param rankedMatrix the matrix containing <i>v</i> argument: the values,
     *                     the rank of which should be calculated.
     * @param pattern      the pattern: the shape of the aperture.
     * @return the rank of the given values.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>baseMatrix.{@link Matrix#dimCount() dimCount()}</code>,
     *                                  or if <code>requiredType</code> is not one of classes
     *                                  <code>{@link UpdatableBitArray}.class</code> /
     *                                  <code>{@link BitArray}.class</code>,
     *                                  <code>{@link UpdatableCharArray}.class</code> /
     *                                  <code>{@link CharArray}.class</code>,
     *                                  <code>{@link UpdatableByteArray}.class</code> /
     *                                  <code>{@link ByteArray}.class</code>,
     *                                  <code>{@link UpdatableShortArray}.class</code> /
     *                                  <code>{@link ShortArray}.class</code>,
     *                                  <code>{@link UpdatableIntArray}.class</code> /
     *                                  <code>{@link IntArray}.class</code>,
     *                                  <code>{@link UpdatableLongArray}.class</code> /
     *                                  <code>{@link LongArray}.class</code>,
     *                                  <code>{@link UpdatableFloatArray}.class</code> /
     *                                  <code>{@link FloatArray}.class</code>
     *                                  or <code>{@link UpdatableDoubleArray}.class</code> /
     *                                  <code>{@link DoubleArray}.class</code>.
     * @see #asRank(Class, Matrix, Matrix, Pattern)
     * @see #rank(Matrix, Matrix, Matrix, Pattern)
     */
    <T extends PArray> Matrix<? extends T> rank(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern);

    /**
     * Equivalent to {@link #rank(Class, Matrix, Matrix, Pattern)} method, but the result matrix
     * will be placed in the <code>dest</code> argument and the required type will be chosen automatically
     * as <code>dest.{@link Matrix#type(Class) type}(PArray.class)</code>.
     * More precisely, the result, saved in <code>dest</code>, will be equal to
     *
     * <pre>{@link #rank(Class, Matrix, Matrix, Pattern)
     * rank}(dest.array().{@link Array#type() type()}, baseMatrix, rankedMatrix, pattern)</pre>
     *
     * <p>The element types of <code>baseMatrix</code> and <code>rankedMatrix</code> are usually equal.
     * If they are different (<code>baseMatrix.{@link Matrix#elementType()
     * elementType()}!=rankedMatrix.{@link Matrix#elementType() elementType()}</code>),
     * this method replaces <code>rankedMatrix</code> with
     *
     * <pre>{@link Matrices#asFuncMatrix(boolean, Func, Class, Matrix)
     * Matrices.asFuncMatrix}(true, {@link Func#IDENTITY
     * Func.IDENTITY}, baseMatrix.array().{@link Array#type() type()}, rankedMatrix)</pre>
     *
     * <p>before all other calculations. In other words, this method always casts the type of
     * the ranked elements to the type of <code>baseMatrix</code> elements.
     * As a result, we can be sure, that if the source <code>baseMatrix</code> matrix is fixed-point
     * (<code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> elements),
     * then the rank <i>r</i>(<i>v</i>*&sigma;), according to the definition of the "rank" term,
     * will be an integer number. In this case, you can specify <code>requiredType=IntArray.class</code> and
     * get the precise rank without precision loss. Moreover, if you know that the number of points in the pattern
     * (<code>pattern.{@link Pattern#pointCount() pointCount()}</code>) is less than
     * 2<sup>16</sup>=65536 or 2<sup>8</sup>=256, it is enough to specify correspondingly
     * <code>requiredType=ShortArray.class</code> or <code>ByteArray.class</code>.
     * The less result precision allows you to save memory.
     *
     * @param dest         the target matrix.
     * @param baseMatrix   the source matrix.
     * @param rankedMatrix the matrix containing <i>v</i> argument: the values,
     *                     the rank of which should be calculated.
     * @param pattern      the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>baseMatrix.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asRank(Class, Matrix, Matrix, Pattern)
     * @see #rank(Class, Matrix, Matrix, Pattern)
     */
    void rank(Matrix<? extends UpdatablePArray> dest,
              Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern);


    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>mean between 2 percentiles</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 percentiles</i>" term.
     * The real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> of the percentiles
     * for every element of the result are equal to the corresponding elements of
     * <code>fromPercentileIndexes</code> and <code>toPercentileIndexes</code> matrices.
     * The reserved value <i>filler</i> is specified by the last argument of this method.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param src                   the source matrix.
     * @param fromPercentileIndexes the matrix containing <i>r</i><sub>1</sub> argument: the indexes of
     *                              the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndexes   the matrix containing <i>r</i><sub>2</sub> argument: the indexes of
     *                              the greater percentile of the averaged range for every element of the result.
     * @param pattern               the pattern: the shape of the aperture.
     * @param filler                the reserved value, returned when
     *                              <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @return the "lazy" matrix containing the mean between 2 given percentiles
     * of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asMeanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     */
    Matrix<? extends PArray> asMeanBetweenPercentiles(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentileIndexes,
            Matrix<? extends PArray> toPercentileIndexes,
            Pattern pattern, double filler);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>mean between 2 percentiles</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 percentiles</i>" term.
     * The real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> of the percentiles
     * for every element of the result are equal to <code>fromPercentileIndex</code>
     * and <code>toPercentileIndex</code> arguments for all aperture positions.
     * The reserved value <i>filler</i> is specified by the last argument of this method.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #asMeanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * asMeanBetweenPercentiles}(src,
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), fromPercentileIndex),
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), toPercentileIndex),
     * &#32;   pattern, filler)</pre>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param src                 the source matrix.
     * @param fromPercentileIndex <i>r</i><sub>1</sub> argument: the index of
     *                            the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndex   <i>r</i><sub>2</sub> argument: the indexes of
     *                            the greater percentile of the averaged range for every element of the result.
     * @param pattern             the pattern: the shape of the aperture.
     * @param filler              the reserved value, returned when
     *                            <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @return the "lazy" matrix containing the mean between 2 given percentiles
     * of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asMeanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, Matrix, double, double, Pattern, double)
     */
    Matrix<? extends PArray> asMeanBetweenPercentiles(
            Matrix<? extends PArray> src,
            double fromPercentileIndex,
            double toPercentileIndex,
            Pattern pattern, double filler);

    /**
     * Returns a new updatable matrix, containing the <i>mean between 2 percentiles</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 percentiles</i>" term.
     * The real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> of the percentiles
     * for every element of the result are equal to the corresponding elements of
     * <code>fromPercentileIndexes</code> and <code>toPercentileIndexes</code> matrices.
     * The reserved value <i>filler</i> is specified by the last argument of this method.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param src                   the source matrix.
     * @param fromPercentileIndexes the matrix containing <i>r</i><sub>1</sub> argument: the indexes of
     *                              the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndexes   the matrix containing <i>r</i><sub>2</sub> argument: the indexes of
     *                              the greater percentile of the averaged range for every element of the result.
     * @param pattern               the pattern: the shape of the aperture.
     * @param filler                the reserved value, returned when
     *                              <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @return the mean between 2 given percentiles of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #meanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * @see #asMeanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     */
    Matrix<? extends UpdatablePArray> meanBetweenPercentiles(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentileIndexes,
            Matrix<? extends PArray> toPercentileIndexes,
            Pattern pattern, double filler);

    /**
     * Returns a new updatable matrix, containing the <i>mean between 2 percentiles</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 percentiles</i>" term.
     * The real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> of the percentiles
     * for every element of the result are equal to <code>fromPercentileIndex</code>
     * and <code>toPercentileIndex</code> arguments for all aperture positions.
     * The reserved value <i>filler</i> is specified by the last argument of this method.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(src,
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), fromPercentileIndex),
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), toPercentileIndex),
     * &#32;   pattern, filler)</pre>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param src                 the source matrix.
     * @param fromPercentileIndex <i>r</i><sub>1</sub> argument: the index of
     *                            the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndex   <i>r</i><sub>2</sub> argument: the indexes of
     *                            the greater percentile of the averaged range for every element of the result.
     * @param pattern             the pattern: the shape of the aperture.
     * @param filler              the reserved value, returned when
     *                            <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @return the mean between 2 given percentiles of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * @see #asMeanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, Matrix, double, double, Pattern, double)
     */
    Matrix<? extends UpdatablePArray> meanBetweenPercentiles(
            Matrix<? extends PArray> src,
            double fromPercentileIndex,
            double toPercentileIndex,
            Pattern pattern, double filler);

    /**
     * Equivalent to {@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)} method,
     * but the result matrix will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param dest                  the target matrix.
     * @param src                   the source matrix.
     * @param fromPercentileIndexes the matrix containing <i>r</i><sub>1</sub> argument: the indexes of
     *                              the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndexes   the matrix containing <i>r</i><sub>2</sub> argument: the indexes of
     *                              the greater percentile of the averaged range for every element of the result.
     * @param pattern               the pattern: the shape of the aperture.
     * @param filler                the reserved value, returned when
     *                              <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #meanBetweenPercentiles(Matrix, Matrix, double, double, Pattern, double)
     * @see #asMeanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     */
    void meanBetweenPercentiles(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentileIndexes,
            Matrix<? extends PArray> toPercentileIndexes,
            Pattern pattern, double filler);

    /**
     * Equivalent to {@link #meanBetweenPercentiles(Matrix, double, double, Pattern, double)} method,
     * but the result matrix will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param dest                the target matrix.
     * @param src                 the source matrix.
     * @param fromPercentileIndex <i>r</i><sub>1</sub> argument: the index of
     *                            the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndex   <i>r</i><sub>2</sub> argument: the indexes of
     *                            the greater percentile of the averaged range for every element of the result.
     * @param pattern             the pattern: the shape of the aperture.
     * @param filler              the reserved value, returned when
     *                            <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * @see #asMeanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * @see #meanBetweenPercentiles(Matrix, double, double, Pattern, double)
     */
    void meanBetweenPercentiles(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            double fromPercentileIndex,
            double toPercentileIndex,
            Pattern pattern, double filler);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>mean between 2 values</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 values</i>" term.
     * The real numbers <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub>
     * for every element of the result are equal to the corresponding elements of
     * <code>minValues</code> and <code>maxValues</code> matrices.
     * The reserved value <i>filler</i> and the <i>mode of calculation</i> (for the case
     * <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;))
     * are specified by the last <code>filler</code> argument of this method:
     * <ul>
     * <li>if <code>filler</code> argument is {@link #FILL_MIN_VALUE} (<code>Double.NEGATIVE_INFINITY</code>),
     * the mode B is used;</li>
     * <li>if <code>filler</code> argument is {@link #FILL_MAX_VALUE} (<code>Double.POSITIVE_INFINITY</code>),
     * the mode C is used;</li>
     * <li>if <code>filler</code> argument is {@link #FILL_NEAREST_VALUE} (<code>Double.NaN</code>),
     * the mode D is used;</li>
     * <li>if <code>filler</code> argument contains any other value, the mode A is used and this argument specifies
     * the reserved value <i>filler</i>.</li>
     * </ul>
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param src       the source matrix.
     * @param minValues the matrix containing <i>v</i><sub>1</sub> argument: the low bound
     *                  of the averaged range of values for every element of the result.
     * @param maxValues the matrix containing <i>v</i><sub>2</sub> argument: the high bound
     *                  of the averaged range of values for every element of the result.
     * @param pattern   the pattern: the shape of the aperture.
     * @param filler    the reserved value, returned when
     *                  <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     *                  or one of the special keys {@link #FILL_MIN_VALUE}, {@link #FILL_MAX_VALUE},
     *                  {@link #FILL_NEAREST_VALUE}, which mean using of special calculation modes B, C, D.
     * @return the "lazy" matrix containing the mean between 2 given values of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #meanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     */
    Matrix<? extends PArray> asMeanBetweenValues(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern, double filler);

    /**
     * Returns a new updatable matrix, containing the <i>mean between 2 values</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>mean between 2 values</i>" term.
     * The real numbers <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub>
     * for every element of the result are equal to the corresponding elements of
     * <code>minValues</code> and <code>maxValues</code> matrices.
     * The reserved value <i>filler</i> and the <i>mode of calculation</i> (for the case
     * <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;))
     * are specified by the last <code>filler</code> argument of this method:
     * <ul>
     * <li>if <code>filler</code> argument is {@link #FILL_MIN_VALUE} (<code>Double.NEGATIVE_INFINITY</code>),
     * the mode B is used;</li>
     * <li>if <code>filler</code> argument is {@link #FILL_MAX_VALUE} (<code>Double.POSITIVE_INFINITY</code>),
     * the mode C is used;</li>
     * <li>if <code>filler</code> argument is {@link #FILL_NEAREST_VALUE} (<code>Double.NaN</code>),
     * the mode D is used;</li>
     * <li>if <code>filler</code> argument contains any other value, the mode A is used and this argument specifies
     * the reserved value <i>filler</i>.</li>
     * </ul>
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param src       the source matrix.
     * @param minValues the matrix containing <i>v</i><sub>1</sub> argument: the low bound
     *                  of the averaged range of values for every element of the result.
     * @param maxValues the matrix containing <i>v</i><sub>2</sub> argument: the high bound
     *                  of the averaged range of values for every element of the result.
     * @param pattern   the pattern: the shape of the aperture.
     * @param filler    the reserved value, returned when
     *                  <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     *                  or one of the special keys {@link #FILL_MIN_VALUE}, {@link #FILL_MAX_VALUE},
     *                  {@link #FILL_NEAREST_VALUE}, which mean using of special calculation modes B, C, D.
     * @return the mean between 2 given values of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asMeanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     */
    Matrix<? extends UpdatablePArray> meanBetweenValues(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern, double filler);

    /**
     * Equivalent to {@link #meanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)} method, but the result matrix
     * will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined in the comments to this class.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param dest      the target matrix.
     * @param src       the source matrix.
     * @param minValues the matrix containing <i>v</i><sub>1</sub> argument: the low bound
     *                  of the averaged range of values for every element of the result.
     * @param maxValues the matrix containing <i>v</i><sub>2</sub> argument: the high bound
     *                  of the averaged range of values for every element of the result.
     * @param pattern   the pattern: the shape of the aperture.
     * @param filler    the reserved value, returned when
     *                  <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     *                  or one of the special keys {@link #FILL_MIN_VALUE}, {@link #FILL_MAX_VALUE},
     *                  {@link #FILL_NEAREST_VALUE}, which mean using of special calculation modes B, C, D.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asMeanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)
     * @see #meanBetweenValues(Matrix, Matrix, Matrix, Pattern, double)
     */
    void meanBetweenValues(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern, double filler);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>mean</i>
     * of the source matrix by the specified pattern.
     *
     * <p>More precisely, this method is equivalent to
     * <code>{@link #asFunctionOfSum(Matrix, Pattern, Func) asFunctionOfSum}(src, pattern, meanFunc)</code>,
     * where the function <code>meanFunc</code> is:
     * <ul>
     * <li><code>{@link net.algart.math.functions.LinearFunc#getInstance(double, double...)
     * LinearFunc.getInstance}(0.0, 1.0/<i>N</i>)</code>3,
     * <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the source matrix is floating-point (<code>src.{@link Matrix#elementType() elementType()}</code>
     * is <code>float</code> or <code>double</code>) &mdash;
     * in other words, this method calculates the usual mean of all elements in the aperture:
     * (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) /
     * <i>N</i>;</li>
     *
     * <li><code>{@link net.algart.math.functions.LinearFunc#getInstance(double, double...)
     * LinearFunc.getInstance}(0.5, 1.0/<i>N</i>)</code>,
     * <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the source matrix is fixed-point (<code>src.{@link Matrix#elementType() elementType()}</code>
     * is <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code>) &mdash;
     * in other words, this method calculates the mean of all elements in the aperture plus 0.5:
     * (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) /
     * <i>N</i> + 0.5.</li>
     * </ul>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined above.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>The described rounding rule explains, why we add 0.5 to the mean in the case of a fixed-point source
     * elements. Namely, in this case the combination of adding 0.5 and further truncation to the integer part
     * works as rounding to the nearest integer:
     * &lfloor;<i>m</i>+0.5&rfloor;=<code>Math.round(</code><i>m</i><code>)</code>
     * (excepting some rare cases when the real mean <i>m</i> is a strictly half-integer:
     * <i>m</i>=2<i>k</i>+&frac12;, <i>k</i>&isin;<b>Z</b>). In other words,
     * this behaviour provides the maximal possible precision of the returned integer mean.
     *
     * <p>Note: the behaviour of this method is similar to
     * <code>{@link #asMeanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * asMeanBetweenPercentiles}(src, 0, <i>N</i>, pattern, <i>anyFiller</i>)</code>,
     * where <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the histogram is processed in the simple histogram model (see
     * {@link SummingHistogram comments to SummingHistogram class}).
     * Really, in this case the mean between 2 percentiles is equal to
     *
     * <blockquote>(<i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>)) /
     *     ((<i>r</i><sub>2</sub>&minus;<i>r</i><sub>1</sub>)*&sigma;) = <i>S</i>(<i>N</i>) / (<i>N</i>*&sigma;) =
     *     (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>j</i>&lt;<i>M</i></sub>(<i>j</i>+0.5)*<b>b</b>[<i>j</i>])
     *     / (<i>N</i>*&sigma;)
     * </blockquote>
     *
     * <p>In the simplest case, when the source elements are real numbers in the "standard" allowed range
     * <code>0.0..1.0</code>,
     * we have &sigma; = <i>M</i> (the histogram length),
     * <i>a<sub>i</sub></i> = &lfloor;<i>v<sub>i</sub></i>*<i>M</i>&rfloor;,
     * and this expression is equal to
     *
     * <blockquote>
     *     (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>i</i>&lt;<i>N</i></sub>(<i>a<sub>i</sub></i>+0.5)) / (<i>NM</i>) =
     *     (<i>v'</i><sub>0</sub>+<i>v'</i><sub>1</sub>+...+<i>v'</i><sub><i>N</i>&minus;1</sub>) / <i>N</i>
     *     + 0.5 / <i>M</i> &asymp;
     *     (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) / <i>N</i>
     * </blockquote>
     *
     * <p>where <i>v'<sub>i</sub></i> = <i>a<sub>i</sub></i>/<i>M</i> is an attempt
     * to represent the real number <i>v<sub>i</sub></i> with the given precision &mu;=log<sub>2</sub><i>M</i> bits
     * and a correction 0.5/<i>M</i> is very little for large <i>M</i>.
     *
     * <p>In another simplest case, when the source elements are integer numbers, &sigma;=1 and
     * the elements <i>v<sub>i</sub></i> are non-negative integers,
     * we have <i>a<sub>i</sub></i> = <i>v<sub>i</sub></i>, and this expression is equal to
     *
     * <blockquote>
     *     (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>i</i>&lt;<i>N</i></sub>(<i>v<sub>i</sub></i>+0.5)) / <i>N</i> =
     *     (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) / <i>N</i> + 0.5
     * </blockquote>
     *
     * @param src     the source matrix.
     * @param pattern the pattern: the shape of the aperture.
     * @return the "lazy" matrix containing the mean of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #mean(Matrix, Pattern)
     * @see #mean(Matrix, Matrix, Pattern)
     */
    Matrix<? extends PArray> asMean(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>mean</i>
     * of the source matrix by the specified pattern.
     *
     * <p>More precisely, this method is equivalent to
     * <code>{@link #functionOfSum(Matrix, Pattern, Func) functionOfSum}(src, pattern, meanFunc)</code>,
     * where the function <code>meanFunc</code> is:
     * <ul>
     * <li><code>{@link net.algart.math.functions.LinearFunc#getInstance(double, double...)
     * LinearFunc.getInstance}(0.0, 1.0/<i>N</i>)</code>,
     * <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the source matrix is floating-point (<code>src.{@link Matrix#elementType() elementType()}</code>
     * is <code>float</code> or <code>double</code>) &mdash;
     * in other words, this method calculates the usual mean of all elements in the aperture:
     * (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) /
     * <i>N</i>;</li>
     *
     * <li><code>{@link net.algart.math.functions.LinearFunc#getInstance(double, double...)
     * LinearFunc.getInstance}(0.5, 1.0/<i>N</i>)</code>,
     * <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the source matrix is fixed-point (<code>src.{@link Matrix#elementType() elementType()}</code>
     * is <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code>) &mdash;
     * in other words, this method calculates the mean of all elements in the aperture plus 0.5:
     * (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) /
     * <i>N</i> + 0.5.</li>
     * </ul>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined above.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>The described rounding rule explains, why we add 0.5 to the mean in the case of a fixed-point source
     * elements. Namely, in this case the combination of adding 0.5 and further truncation to the integer part
     * works as rounding to the nearest integer:
     * &lfloor;<i>m</i>+0.5&rfloor;=<code>Math.round(</code><i>m</i><code>)</code>
     * (excepting some rare cases when the real mean <i>m</i> is a strictly half-integer:
     * <i>m</i>=2<i>k</i>+&frac12;, <i>k</i>&isin;<b>Z</b>). In other words,
     * this behaviour provides the maximal possible precision of the returned integer mean.
     *
     * <p>Note: the behaviour of this method is similar to
     * <code>{@link #meanBetweenPercentiles(Matrix, double, double, Pattern, double)
     * meanBetweenPercentiles}(src, 0, <i>N</i>, pattern, <i>anyFiller</i>)</code>,
     * where <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the histogram is processed in the simple histogram model (see
     * {@link SummingHistogram comments to SummingHistogram class}).
     * Really, in this case the mean between 2 percentiles is equal to
     *
     * <blockquote>(<i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>)) /
     *     ((<i>r</i><sub>2</sub>&minus;<i>r</i><sub>1</sub>)*&sigma;) = <i>S</i>(<i>N</i>) / (<i>N</i>*&sigma;) =
     *     (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>j</i>&lt;<i>M</i></sub>(<i>j</i>+0.5)*<b>b</b>[<i>j</i>])
     *     / (<i>N</i>*&sigma;)
     * </blockquote>
     *
     * <p>In the simplest case, when the source elements are real numbers in the "standard" allowed range
     * <code>0.0..1.0</code>,
     * we have &sigma; = <i>M</i> (the histogram length),
     * <i>a<sub>i</sub></i> = &lfloor;<i>v<sub>i</sub></i>*<i>M</i>&rfloor;,
     * and this expression is equal to
     *
     * <blockquote>
     *     (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>i</i>&lt;<i>N</i></sub>(<i>a<sub>i</sub></i>+0.5)) / (<i>NM</i>) =
     *     (<i>v'</i><sub>0</sub>+<i>v'</i><sub>1</sub>+...+<i>v'</i><sub><i>N</i>&minus;1</sub>) / <i>N</i>
     *     + 0.5 / <i>M</i> &asymp;
     *     (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) / <i>N</i>
     * </blockquote>
     *
     * <p>where <i>v'<sub>i</sub></i> = <i>a<sub>i</sub></i>/<i>M</i> is an attempt
     * to represent the real number <i>v<sub>i</sub></i> with the given precision &mu;=log<sub>2</sub><i>M</i> bits
     * and a correction 0.5/<i>M</i> is very little for large <i>M</i>.
     *
     * <p>In another simplest case, when the source elements are integer numbers, &sigma;=1 and
     * the elements <i>v<sub>i</sub></i> are non-negative integers,
     * we have <i>a<sub>i</sub></i> = <i>v<sub>i</sub></i>, and this expression is equal to
     *
     * <blockquote>
     *     (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>i</i>&lt;<i>N</i></sub>(<i>v<sub>i</sub></i>+0.5)) / <i>N</i> =
     *     (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) / <i>N</i> + 0.5
     * </blockquote>
     *
     * @param src     the source matrix.
     * @param pattern the pattern: the shape of the aperture.
     * @return the mean of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asMean(Matrix, Pattern)
     * @see #mean(Matrix, Matrix, Pattern)
     */
    Matrix<? extends UpdatablePArray> mean(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Equivalent to {@link #mean(Matrix, Pattern)} method,
     * but the result matrix will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real mean, defined above.
     * In this case, the found mean value <i>m</i> is usually truncated to its integer part
     * &lfloor;<i>m</i>&rfloor;=<code>(long)</code><i>m</i>
     * (remember that the mean value, according to our definition, is always &ge;0).
     * More precisely, the rules of casting the floating-point means to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>The described rounding rule explains, why we add 0.5 to the mean in the case of a fixed-point source
     * elements. Namely, in this case the combination of adding 0.5 and further truncation to the integer part
     * works as rounding to the nearest integer:
     * &lfloor;<i>m</i>+0.5&rfloor;=<code>Math.round(</code><i>m</i><code>)</code>
     * (excepting some rare cases when the real mean <i>m</i> is a strictly half-integer:
     * <i>m</i>=2<i>k</i>+&frac12;, <i>k</i>&isin;<b>Z</b>). In other words,
     * this behaviour provides the maximal possible precision of the returned integer mean.
     *
     * <p>Note: the behaviour of this method is similar to
     * <code>{@link #meanBetweenPercentiles(Matrix, Matrix, double, double, Pattern, double)
     * meanBetweenPercentiles}(src, 0, <i>N</i>, pattern, <i>anyFiller</i>)</code>,
     * where <code><i>N</i> = pattern.{@link Pattern#pointCount() pointCount()}</code>,
     * if the histogram is processed in the simple histogram model (see
     * {@link SummingHistogram comments to SummingHistogram class}).
     * Really, in this case the mean between 2 percentiles is equal to
     *
     * <blockquote>(<i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>)) /
     * ((<i>r</i><sub>2</sub>&minus;<i>r</i><sub>1</sub>)*&sigma;) = <i>S</i>(<i>N</i>) / (<i>N</i>*&sigma;) =
     * (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>j</i>&lt;<i>M</i></sub>(<i>j</i>+0.5)*<b>b</b>[<i>j</i>])
     * / (<i>N</i>*&sigma;)
     * </blockquote>
     *
     * <p>In the simplest case, when the source elements are real numbers in the "standard" allowed range
     * <code>0.0..1.0</code>,
     * we have &sigma; = <i>M</i> (the histogram length),
     * <i>a<sub>i</sub></i> = &lfloor;<i>v<sub>i</sub></i>*<i>M</i>&rfloor;,
     * and this expression is equal to
     *
     * <blockquote>
     * (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>i</i>&lt;<i>N</i></sub>(<i>a<sub>i</sub></i>+0.5)) / (<i>NM</i>) =
     * (<i>v'</i><sub>0</sub>+<i>v'</i><sub>1</sub>+...+<i>v'</i><sub><i>N</i>&minus;1</sub>) / <i>N</i>
     * + 0.5 / <i>M</i> &asymp;
     * (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) / <i>N</i>
     * </blockquote>
     *
     * <p>where <i>v'<sub>i</sub></i> = <i>a<sub>i</sub></i>/<i>M</i> is an attempt
     * to represent the real number <i>v<sub>i</sub></i> with the given precision &mu;=log<sub>2</sub><i>M</i> bits
     * and a correction 0.5/<i>M</i> is very little for large <i>M</i>.
     *
     * <p>In another simplest case, when the source elements are integer numbers, &sigma;=1 and
     * the elements <i>v<sub>i</sub></i> are non-negative integers,
     * we have <i>a<sub>i</sub></i> = <i>v<sub>i</sub></i>, and this expression is equal to
     *
     * <blockquote>
     * (<big>&Sigma;</big>&nbsp;<sub>0&le;<i>i</i>&lt;<i>N</i></sub>(<i>v<sub>i</sub></i>+0.5)) / <i>N</i> =
     * (<i>v</i><sub>0</sub>+<i>v</i><sub>1</sub>+...+<i>v</i><sub><i>N</i>&minus;1</sub>) / <i>N</i> + 0.5
     * </blockquote>
     *
     * @param dest    the target matrix.
     * @param src     the source matrix.
     * @param pattern the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asMean(Matrix, Pattern)
     * @see #mean(Matrix, Pattern)
     */
    void mean(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the result of some given function
     * <i>f</i>(<i>S</i>) of the <i>aperture sum S</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>aperture sum</i>" term.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>The function, applied to each calculated aperture sum <i>S</i>, is specified via
     * <code>processingFunc</code> argument. Namely, for each aperture position this method
     * calculates the aperture sum <i>S</i> of the source matrix and returns
     * <code>processingFunc.{@link Func#get(double) get}(</code><i>S</i><code>)</code>
     * in the corresponding element of the resulting matrix.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>This method can be considered as a generalization of {@link #asMean(Matrix, Pattern)}.
     *
     * @param src            the source matrix.
     * @param pattern        the pattern: the shape of the aperture.
     * @param processingFunc the function, which should be applied to every calculated aperture sum.
     * @return the "lazy" matrix containing the result of the given function for
     * the aperture sum of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #functionOfSum(Matrix, Pattern, Func)
     * @see #functionOfSum(Matrix, Matrix, Pattern, Func)
     */
    Matrix<? extends PArray> asFunctionOfSum(
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc);

    /**
     * Returns a new updatable matrix, containing the result of some given function
     * <i>f</i>(<i>S</i>) of the <i>aperture sum S</i>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the
     * "<i>aperture sum</i>" term.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>The function, applied to each calculated aperture sum <i>S</i>, is specified via
     * <code>processingFunc</code> argument. Namely, for each aperture position this method
     * calculates the aperture sum <i>S</i> of the source matrix and returns
     * <code>processingFunc.{@link Func#get(double) get}(</code><i>S</i><code>)</code>
     * in the corresponding element of the resulting matrix.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>This method can be considered as a generalization of {@link #mean(Matrix, Pattern)}.
     *
     * @param src            the source matrix.
     * @param pattern        the pattern: the shape of the aperture.
     * @param processingFunc the function, which should be applied to every calculated aperture sum.
     * @return the result of the given function for the aperture sum of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asFunctionOfSum(Matrix, Pattern, Func)
     * @see #functionOfSum(Matrix, Matrix, Pattern, Func)
     */
    Matrix<? extends UpdatablePArray> functionOfSum(
            Matrix<? extends PArray> src,
            Pattern pattern, Func processingFunc);

    /**
     * Equivalent to {@link #functionOfSum(Matrix, Pattern, Func)} method,
     * but the result matrix will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>This method can be considered as a generalization of {@link #mean(Matrix, Matrix, Pattern)}.
     *
     * @param dest           the target matrix.
     * @param src            the source matrix.
     * @param pattern        the pattern: the shape of the aperture.
     * @param processingFunc the function, which should be applied to every calculated aperture sum.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asFunctionOfSum(Matrix, Pattern, Func)
     * @see #functionOfSum(Matrix, Pattern, Func)
     */
    void functionOfSum(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the result of some given function
     * <i>f</i>(<i>v</i>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>) of the source matrix <i>v</i>
     * and two <i>percentiles</i> <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real indexes <i>r</i> of two percentiles for every element of the result are equal
     * to the corresponding elements of <code>percentileIndexes1</code> (for <i>v</i><sub>1</sub>)
     * or <code>percentileIndexes2</code> matrix (for <i>v</i><sub>2</sub>).
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>The function, applied to each calculated three
     * (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>), is specified via
     * <code>processingFunc</code> argument. Namely, for each aperture position <b>x</b> this method takes
     * the value <i>v</i> &mdash; the element of the source matrix <code>src</code> at this aperture position <b>x</b>,
     * calculates two percentiles <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> of the source matrix and returns
     * <code>processingFunc.{@link Func#get(double, double, double)
     * get}(</code><i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub><code>)</code>
     * in the corresponding element of the resulting matrix.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>You can get the same results by 2 calls of {@link #asPercentile(Matrix, Matrix, Pattern)} method
     * for both matrices of percentile indexes and applying the function to them and to the source matrix via
     * {@link Matrices#asFuncMatrix(Func, Class, Matrix, Matrix, Matrix)} method.
     * But such a way works slower and is less convenient, than this method. A typical application of this method
     * in image processing area is the contrasting image &mdash; in this case, we recommend using
     * {@link net.algart.math.functions.ContrastingFunc ContrastingFunc} object as <code>processingFunc</code> argument.
     *
     * @param src                the source matrix.
     * @param percentileIndexes1 the 1st matrix containing <i>r</i> argument: the indexes of the 1st percentile
     *                           <i>v</i><sub>1</sub> for every element of the result.
     * @param percentileIndexes2 the 2nd matrix containing <i>r</i> argument: the indexes of the 2nd percentile
     *                           <i>v</i><sub>2</sub> for every element of the result.
     * @param pattern            the pattern: the shape of the aperture.
     * @param processingFunc     the function, which should be applied to every calculated three
     *                           (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                           where <i>v</i> is the element of the source matrix,
     *                           <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return the "lazy" matrix containing the result of the given function.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asFunctionOfPercentilePair(Matrix, double, double, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     */
    Matrix<? extends PArray> asFunctionOfPercentilePair(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes1,
            Matrix<? extends PArray> percentileIndexes2,
            Pattern pattern,
            Func processingFunc);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the result of some given function
     * <i>f</i>(<i>v</i>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>) of the source matrix <i>v</i>
     * and two <i>percentiles</i> <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real indexes <i>r</i> of two percentiles for every element of the result are equal
     * to <code>percentileIndex1</code> (for <i>v</i><sub>1</sub>)
     * or <code>percentileIndex2</code> argument (for <i>v</i><sub>2</sub>).
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #asFunctionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func) asFunctionOfPercentilePair}(src,
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex1),
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex1),
     * &#32;   pattern, processingFunc)</pre>
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>You can get the same results by 2 calls of {@link #asPercentile(Matrix, double, Pattern)} method
     * for both matrices of percentile indexes and applying the function to them and to the source matrix via
     * {@link Matrices#asFuncMatrix(Func, Class, Matrix, Matrix, Matrix)} method.
     * But such a way works slower and is less convenient, than this method. A typical application of this method
     * in image processing area is the contrasting image &mdash; in this case, we recommend using
     * {@link net.algart.math.functions.ContrastingFunc ContrastingFunc} object
     * as <code>processingFunc</code> argument.
     *
     * @param src              the source matrix.
     * @param percentileIndex1 the 1st <i>r</i> argument: the index of the 1st percentile <i>v</i><sub>1</sub>.
     * @param percentileIndex2 the 2nd <i>r</i> argument: the index of the 2nd percentile <i>v</i><sub>2</sub>.
     * @param pattern          the pattern: the shape of the aperture.
     * @param processingFunc   the function, which should be applied to every calculated three
     *                         (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                         where <i>v</i> is the element of the source matrix,
     *                         <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return the "lazy" matrix containing the result of the given function.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asFunctionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, double, double, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, Matrix, double, double, Pattern, Func)
     */
    Matrix<? extends PArray> asFunctionOfPercentilePair(
            Matrix<? extends PArray> src,
            double percentileIndex1,
            double percentileIndex2,
            Pattern pattern,
            Func processingFunc);

    /**
     * Returns a new updatable matrix, containing the result of some given function
     * <i>f</i>(<i>v</i>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>) of the source matrix <i>v</i>
     * and two <i>percentiles</i> <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real indexes <i>r</i> of two percentiles for every element of the result are equal
     * to the corresponding elements of <code>percentileIndexes1</code> (for <i>v</i><sub>1</sub>)
     * or <code>percentileIndexes2</code> matrix (for <i>v</i><sub>2</sub>).
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>The function, applied to each calculated three
     * (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>), is specified via
     * <code>processingFunc</code> argument. Namely, for each aperture position <b>x</b> this method takes
     * the value <i>v</i> &mdash; the element of the source matrix <code>src</code> at this aperture position <b>x</b>,
     * calculates two percentiles <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> of the source matrix and returns
     * <code>processingFunc.{@link Func#get(double, double, double)
     * get}(</code><i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub><code>)</code>
     * in the corresponding element of the resulting matrix.
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>You can get the same results by 2 calls of {@link #percentile(Matrix, Matrix, Pattern)} method
     * for both matrices of percentile indexes and applying the function to them and to the source matrix via
     * {@link Matrices#asFuncMatrix(Func, Class, Matrix, Matrix, Matrix)} method.
     * But such a way works slower and is less convenient, than this method. A typical application of this method
     * in image processing area is the contrasting image &mdash; in this case, we recommend using
     * {@link net.algart.math.functions.ContrastingFunc ContrastingFunc} object as <code>processingFunc</code> argument.
     *
     * @param src                the source matrix.
     * @param percentileIndexes1 the 1st matrix containing <i>r</i> argument: the indexes of the 1st percentile
     *                           <i>v</i><sub>1</sub> for every element of the result.
     * @param percentileIndexes2 the 2nd matrix containing <i>r</i> argument: the indexes of the 2nd percentile
     *                           <i>v</i><sub>2</sub> for every element of the result.
     * @param pattern            the pattern: the shape of the aperture.
     * @param processingFunc     the function, which should be applied to every calculated three
     *                           (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                           where <i>v</i> is the element of the source matrix,
     *                           <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return the result of the given function.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #functionOfPercentilePair(Matrix, double, double, Pattern, Func)
     * @see #asFunctionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     */
    Matrix<? extends UpdatablePArray> functionOfPercentilePair(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes1,
            Matrix<? extends PArray> percentileIndexes2,
            Pattern pattern,
            Func processingFunc);

    /**
     * Returns a new updatable matrix, containing the result of some given function
     * <i>f</i>(<i>v</i>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>) of the source matrix <i>v</i>
     * and two <i>percentiles</i> <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>
     * of the source matrix by the specified pattern.
     * See the {@link RankMorphology comments to this class}, section 4 about the "<i>percentile</i>" term.
     * The real indexes <i>r</i> of two percentiles for every element of the result are equal
     * to <code>percentileIndex1</code> (for <i>v</i><sub>1</sub>)
     * or <code>percentileIndex2</code> argument (for <i>v</i><sub>2</sub>).
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>This method is equivalent to
     *
     * <pre>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func) functionOfPercentilePair}(src,
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex1),
     * &#32;   src.matrix({@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex1),
     * &#32;   pattern, processingFunc)</pre>
     *
     * <p>If the element type of the source matrix (and, thus, of the result) is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * <p>You can get the same results by 2 calls of {@link #percentile(Matrix, double, Pattern)} method
     * for both matrices of percentile indexes and applying the function to them and to the source matrix via
     * {@link Matrices#asFuncMatrix(Func, Class, Matrix, Matrix, Matrix)} method.
     * But such a way works slower and is less convenient, than this method. A typical application of this method
     * in image processing area is the contrasting image &mdash; in this case, we recommend using
     * {@link net.algart.math.functions.ContrastingFunc ContrastingFunc} object as <code>processingFunc</code> argument.
     *
     * @param src              the source matrix.
     * @param percentileIndex1 the 1st <i>r</i> argument: the index of the 1st percentile <i>v</i><sub>1</sub>.
     * @param percentileIndex2 the 2nd <i>r</i> argument: the index of the 2nd percentile <i>v</i><sub>2</sub>.
     * @param pattern          the pattern: the shape of the aperture.
     * @param processingFunc   the function, which should be applied to every calculated three
     *                         (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                         where <i>v</i> is the element of the source matrix,
     *                         <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return the result of the given function.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * @see #asFunctionOfPercentilePair(Matrix, double, double, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, Matrix, double, double, Pattern, Func)
     */
    Matrix<? extends UpdatablePArray> functionOfPercentilePair(
            Matrix<? extends PArray> src,
            double percentileIndex1,
            double percentileIndex2,
            Pattern pattern,
            Func processingFunc);

    /**
     * Equivalent to {@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)} method,
     * but the result matrix will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param dest               the target matrix.
     * @param src                the source matrix.
     * @param percentileIndexes1 the 1st matrix containing <i>r</i> argument: the indexes of the 1st percentile
     *                           <i>v</i><sub>1</sub> for every element of the result.
     * @param percentileIndexes2 the 2nd matrix containing <i>r</i> argument: the indexes of the 2nd percentile
     *                           <i>v</i><sub>2</sub> for every element of the result.
     * @param pattern            the pattern: the shape of the aperture.
     * @param processingFunc     the function, which should be applied to every calculated three
     *                           (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                           where <i>v</i> is the element of the source matrix,
     *                           <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #functionOfPercentilePair(Matrix, Matrix, double, double, Pattern, Func)
     * @see #asFunctionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     */
    void functionOfPercentilePair(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes1,
            Matrix<? extends PArray> percentileIndexes2,
            Pattern pattern,
            Func processingFunc);

    /**
     * Equivalent to {@link #functionOfPercentilePair(Matrix, double, double, Pattern, Func)} method,
     * but the result matrix will be placed in the <code>dest</code> argument.
     *
     * <p>If the element type of <code>dest</code> matrix is fixed-point &mdash;
     * <code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>,
     * <code>int</code> or <code>long</code> &mdash;
     * then we need to round the real function result.
     * In this case, the found function result <i>f</i> is usually truncated to its integer part
     * <code>(long)</code><i>f</i>.
     * More precisely, the rules of casting the floating-point function results to the desired element type
     * are the same as in {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <code>truncateOverflows=true</code>.
     *
     * @param dest             the target matrix.
     * @param src              the source matrix.
     * @param percentileIndex1 the 1st <i>r</i> argument: the index of the 1st percentile <i>v</i><sub>1</sub>.
     * @param percentileIndex2 the 2nd <i>r</i> argument: the index of the 2nd percentile <i>v</i><sub>2</sub>.
     * @param pattern          the pattern: the shape of the aperture.
     * @param processingFunc   the function, which should be applied to every calculated three
     *                         (<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                         where <i>v</i> is the element of the source matrix,
     *                         <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     * @see #asFunctionOfPercentilePair(Matrix, double, double, Pattern, Func)
     * @see #functionOfPercentilePair(Matrix, double, double, Pattern, Func)
     */
    void functionOfPercentilePair(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            double percentileIndex1,
            double percentileIndex2,
            Pattern pattern,
            Func processingFunc);
}
