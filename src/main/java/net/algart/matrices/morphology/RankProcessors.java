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
import net.algart.matrices.StreamingApertureProcessor;

/**
 * <p>A set of {@link StreamingApertureProcessor streaming aperture processors},
 * implementing all operations, specified in {@link RankMorphology} interface.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class RankProcessors {
    private RankProcessors() {}

    /**
     * Creates a new streaming aperture processor, which finds the <i>percentile</i> of the source matrix&nbsp;<b>M</b>.
     * See {@link RankMorphology comments to RankMorphology}, section 4 about the "<i>percentile</i>" term.
     * The real index <i>r</i> of the percentile for every element of the result is equal
     * to the corresponding element of the additional matrix <b>M</b><sub>0</sub>.
     *
     * <p>More precisely, let <tt>rm</tt> is an instance of {@link BasicRankMorphology}, created by the call
     *
     * <blockquote><tt>rm = {@link
     * BasicRankMorphology#getInstance(ArrayContext context, double dilationLevel, CustomRankPrecision precision)},
     * </tt></blockquote>
     *
     * <p>so that <tt>context</tt>, <tt>precision.{@link CustomRankPrecision#interpolated() interpolated()}</tt> and
     * <tt>precision.{@link CustomRankPrecision#bitLevels() bitLevels()}</tt> are the same as the arguments
     * of this method. Then in the streaming aperture processor, created by this method:
     *
     * <ul>
     * <li>the number of required additional matrices is 1;</li>
     * <li>{@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equivalent to
     * <b>R</b>=<tt>rm.{@link RankMorphology#asPercentile(Matrix, Matrix, net.algart.math.patterns.Pattern)
     * asPercentile}</tt>(<b>M</b>, <b>M</b><sub>0</sub>, <b>P</b>), if the <tt>requiredType</tt> argument of
     * {@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equal to
     * <b>M</b>.{@link Matrix#type() type()}==<b>R</b>.{@link Matrix#type() type()};</li>
     *
     * <li>if <tt>requiredType</tt> is not equal to <b>M</b>.{@link Matrix#type() type()}, it is equivalent to
     * <tt>{@link Matrices#asFuncMatrix(boolean, Func, Class, Matrix)
     * Matrices.asFuncMatrix}(true, {@link Func#IDENTITY}, requiredType, </tt><b>R</b><tt>)</tt>;</li>
     *
     * <li>{@link
     * StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * process} method is equivalent to
     * <tt>rm.{@link RankMorphology#percentile(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern)
     * percentile}</tt>(<b>R</b>, <b>M</b>, <b>M</b><sub>0</sub>, <b>P</b>).</li>
     * </ul>
     *
     * <p>This processor is really created and called in the implementation of
     * {@link BasicRankMorphology#asPercentile(Matrix, Matrix, net.algart.math.patterns.Pattern) asPercentile}
     * and {@link BasicRankMorphology#percentile(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern) percentile}
     * methods in {@link BasicRankMorphology} class.
     *
     * @param context      the {@link StreamingApertureProcessor#context() context} that will be used by this object;
     *                     can be {@code null}, then it will be ignored.
     * @param interpolated the histogram model used while calculating percentile: <tt>true</tt> means
     *                     the precise histogram model, <tt>false</tt> means the simple histogram model
     *                     (see comments to {@link Histogram} class).
     * @param bitLevels    the {@link CustomRankPrecision#bitLevels() bit levels} used while calculations.
     * @return             the new streaming aperture processor, finding the percentile.
     * @throws NullPointerException     if <tt>bitLevels</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>bitLevels.length==0</tt>, or if <tt>bitLevels.length&gt;31</tt>,
     *                                  or if some of the elements <tt>bitLevels</tt> is not in 1..30 range, or if
     *                                  <tt>bitLevels</tt>[<i>k</i>]&gt;=<tt>bitLevels</tt>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static StreamingApertureProcessor getPercentiler(ArrayContext context,
        boolean interpolated, int... bitLevels)
    {
        return new Percentiler(context, interpolated, bitLevels);
    }

    /**
     * Creates a new streaming aperture processor, which finds the <i>rank</i> of the source matrix&nbsp;<b>M</b>.
     * See {@link RankMorphology comments to RankMorphology}, section 4 about the "<i>rank</i>" term.
     * The real value <i>v</i> for every element of the result is equal
     * to the corresponding element of the additional matrix <b>M</b><sub>0</sub>.
     *
     * <p>More precisely, let <tt>rm</tt> is an instance of {@link BasicRankMorphology}, created by the call
     *
     * <blockquote><tt>rm = {@link
     * BasicRankMorphology#getInstance(ArrayContext context, double dilationLevel, CustomRankPrecision precision)},
     * </tt></blockquote>
     *
     * <p>so that <tt>context</tt>, <tt>precision.{@link CustomRankPrecision#interpolated() interpolated()}</tt> and
     * <tt>precision.{@link CustomRankPrecision#bitLevels() bitLevels()}</tt> are the same as the arguments
     * of this method. Then in the streaming aperture processor, created by this method:
     *
     * <ul>
     * <li>the number of required additional matrices is 1;</li>
     * <li>{@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equivalent to
     * <tt>rm.{@link RankMorphology#asRank(Class, Matrix, Matrix, net.algart.math.patterns.Pattern)
     * asRank}</tt>(<tt>requiredType</tt>, <b>M</b>, <b>M</b><sub>0</sub>, <b>P</b>),
     * where <tt>requiredType</tt> is the first argument of
     * {@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method;</li>
     *
     * <li>{@link
     * StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * process} method is equivalent to
     * <tt>rm.{@link RankMorphology#rank(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern)
     * rank}</tt>(<b>R</b>, <b>M</b>, <b>M</b><sub>0</sub>, <b>P</b>).</li>
     * </ul>
     *
     * <p>This processor is really created and called in the implementation of
     * {@link BasicRankMorphology#asRank(Class, Matrix, Matrix, net.algart.math.patterns.Pattern) asRank}
     * and {@link BasicRankMorphology#rank(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern) rank}
     * methods in {@link BasicRankMorphology} class.
     *
     * @param context      the {@link StreamingApertureProcessor#context() context} that will be used by this object;
     *                     can be {@code null}, then it will be ignored.
     * @param interpolated the histogram model used while calculating percentile: <tt>true</tt> means
     *                     the precise histogram model, <tt>false</tt> means the simple histogram model
     *                     (see comments to {@link Histogram} class).
     * @param bitLevels    the {@link CustomRankPrecision#bitLevels() bit levels} used while calculations.
     * @return             the new streaming aperture processor, finding the rank.
     * @throws NullPointerException     if <tt>bitLevels</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>bitLevels.length==0</tt>, or if <tt>bitLevels.length&gt;31</tt>,
     *                                  or if some of the elements <tt>bitLevels</tt> is not in 1..30 range, or if
     *                                  <tt>bitLevels</tt>[<i>k</i>]&gt;=<tt>bitLevels</tt>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static StreamingApertureProcessor getRanker(ArrayContext context,
        boolean interpolated, int... bitLevels)
    {
        return new Ranker(context, interpolated, bitLevels);
    }

    /**
     * Creates a new streaming aperture processor, which finds the <i>mean between 2 percentiles</i>
     * of the source matrix&nbsp;<b>M</b>.
     * See {@link RankMorphology comments to RankMorphology}, section 4 about the
     * "<i>mean between 2 percentiles</i>" term.
     * The real indexes <i>r</i><sub>1</sub> and <i>r</i><sub>2</sub> of the percentiles
     * for every element of the result are equal
     * to the corresponding elements of the additional matrices <b>M</b><sub>0</sub> and <b>M</b><sub>1</sub>.
     *
     * <p>More precisely, let <tt>rm</tt> is an instance of {@link BasicRankMorphology}, created by the call
     *
     * <blockquote><tt>rm = {@link
     * BasicRankMorphology#getInstance(ArrayContext context, double dilationLevel, CustomRankPrecision precision)},
     * </tt></blockquote>
     *
     * <p>so that <tt>context</tt>, <tt>precision.{@link CustomRankPrecision#interpolated() interpolated()}</tt> and
     * <tt>precision.{@link CustomRankPrecision#bitLevels() bitLevels()}</tt> are the same as the arguments
     * of this method. Then in the streaming aperture processor, created by this method:
     *
     * <ul>
     * <li>the number of required additional matrices is 2;</li>
     * <li>{@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equivalent to
     * <b>R</b>=<tt>rm.{@link
     * RankMorphology#asMeanBetweenPercentiles(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * asMeanBetweenPercentiles}</tt>(<b>M</b>, <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, <b>P</b>,
     * <tt>filler</tt>) (where <tt>filler</tt> is the argument of this method),
     * if the <tt>requiredType</tt> argument of
     * {@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equal to
     * <b>M</b>.{@link Matrix#type() type()}==<b>R</b>.{@link Matrix#type() type()};</li>
     *
     * <li>if <tt>requiredType</tt> is not equal to <b>M</b>.{@link Matrix#type() type()}, it analogously calculates
     * the mean between percentile with maximal (<tt>double</tt>) precision and then
     * casts the floating-point results to the desired element type by the same rules
     * as {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <tt>truncateOverflows=true</tt>;
     * note that such result cannot be obtained by {@link RankMorphology} methods;</li>
     *
     * <li>{@link
     * StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * process} method is equivalent to
     * <tt>rm.{@link
     * RankMorphology#meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * meanBetweenPercentiles}</tt>(<b>R</b>, <b>M</b>, <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, <b>P</b>,
     * <tt>filler</tt>) (where <tt>filler</tt> is the argument of this method).</li>
     * </ul>
     *
     * <p>This processor is really created and called in the implementation of
     * {@link
     * BasicRankMorphology#asMeanBetweenPercentiles(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * asMeanBetweenPercentiles}
     * and {@link
     * BasicRankMorphology#meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * meanBetweenPercentiles}
     * methods in {@link BasicRankMorphology} class.
     *
     * @param context      the {@link StreamingApertureProcessor#context() context} that will be used by this object;
     *                     can be {@code null}, then it will be ignored.
     * @param filler       the reserved value, returned when
     *                     <i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub>.
     * @param interpolated the histogram model used while calculating percentile: <tt>true</tt> means
     *                     the precise histogram model, <tt>false</tt> means the simple histogram model
     *                     (see comments to {@link Histogram} class).
     * @param bitLevels    the {@link CustomRankPrecision#bitLevels() bit levels} used while calculations.
     * @return             the new streaming aperture processor, finding the mean between 2 percentiles.
     * @throws NullPointerException     if <tt>bitLevels</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>bitLevels.length==0</tt>, or if <tt>bitLevels.length&gt;31</tt>,
     *                                  or if some of the elements <tt>bitLevels</tt> is not in 1..30 range, or if
     *                                  <tt>bitLevels</tt>[<i>k</i>]&gt;=<tt>bitLevels</tt>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static StreamingApertureProcessor getAveragerBetweenPercentiles(ArrayContext context,
        double filler, boolean interpolated, int... bitLevels)
    {
        return new AveragerBetweenPercentiles(context, filler, interpolated, bitLevels);
    }

    /**
     * Creates a new streaming aperture processor, which finds the <i>mean between 2 values</i>
     * of the source matrix&nbsp;<b>M</b>.
     * See {@link RankMorphology comments to RankMorphology}, section 4 about the
     * "<i>mean between 2 values</i>" term.
     * The real numbers <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub>
     * for every element of the result are equal
     * to the corresponding elements of the additional matrices <b>M</b><sub>0</sub> and <b>M</b><sub>1</sub>.
     *
     * <p>More precisely, let <tt>rm</tt> is an instance of {@link BasicRankMorphology}, created by the call
     *
     * <blockquote><tt>rm = {@link
     * BasicRankMorphology#getInstance(ArrayContext context, double dilationLevel, CustomRankPrecision precision)},
     * </tt></blockquote>
     *
     * <p>so that <tt>context</tt>, <tt>precision.{@link CustomRankPrecision#interpolated() interpolated()}</tt> and
     * <tt>precision.{@link CustomRankPrecision#bitLevels() bitLevels()}</tt> are the same as the arguments
     * of this method. Then in the streaming aperture processor, created by this method:
     *
     * <ul>
     * <li>the number of required additional matrices is 2;</li>
     * <li>{@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equivalent to
     * <b>R</b>=<tt>rm.{@link
     * RankMorphology#asMeanBetweenValues(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * asMeanBetweenValues}</tt>(<b>M</b>, <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, <b>P</b>,
     * <tt>filler</tt>) (where <tt>filler</tt> is the argument of this method),
     * if the <tt>requiredType</tt> argument of
     * {@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equal to
     * <b>M</b>.{@link Matrix#type() type()}==<b>R</b>.{@link Matrix#type() type()};</li>
     *
     * <li>if <tt>requiredType</tt> is not equal to <b>M</b>.{@link Matrix#type() type()}, it analogously calculates
     * the mean between values with maximal (<tt>double</tt>) precision and then
     * casts the floating-point results to the desired element type by the same rules
     * as {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <tt>truncateOverflows=true</tt>;
     * note that such result cannot be obtained by {@link RankMorphology} methods;</li>
     *
     * <li>{@link
     * StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * process} method is equivalent to
     * <tt>rm.{@link
     * RankMorphology#meanBetweenValues(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * meanBetweenValues}</tt>(<b>R</b>, <b>M</b>, <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, <b>P</b>,
     * <tt>filler</tt>) (where <tt>filler</tt> is the argument of this method).</li>
     * </ul>
     *
     * <p>This processor is really created and called in the implementation of
     * {@link
     * BasicRankMorphology#asMeanBetweenValues(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * asMeanBetweenValues}
     * and {@link
     * BasicRankMorphology#meanBetweenValues(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, double)
     * meanBetweenValues}
     * methods in {@link BasicRankMorphology} class.
     *
     * @param context      the {@link StreamingApertureProcessor#context() context} that will be used by this object;
     *                     can be {@code null}, then it will be ignored.
     * @param filler       the reserved value, returned when
     *                     <i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;),
     *                     or one of the special keys {@link RankMorphology#FILL_MIN_VALUE},
     *                     {@link RankMorphology#FILL_MAX_VALUE}, {@link RankMorphology#FILL_NEAREST_VALUE},
     *                     which mean using of special calculation modes B, C, D.
     * @param interpolated the histogram model used while calculating percentile: <tt>true</tt> means
     *                     the precise histogram model, <tt>false</tt> means the simple histogram model
     *                     (see comments to {@link Histogram} class).
     * @param bitLevels    the {@link CustomRankPrecision#bitLevels() bit levels} used while calculations.
     * @return             the new streaming aperture processor, finding the mean between 2 values.
     * @throws NullPointerException     if <tt>bitLevels</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>bitLevels.length==0</tt>, or if <tt>bitLevels.length&gt;31</tt>,
     *                                  or if some of the elements <tt>bitLevels</tt> is not in 1..30 range, or if
     *                                  <tt>bitLevels</tt>[<i>k</i>]&gt;=<tt>bitLevels</tt>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static StreamingApertureProcessor getAveragerBetweenValues(ArrayContext context,
        double filler, boolean interpolated, int... bitLevels)
    {
        return new AveragerBetweenValues(context, filler, interpolated, bitLevels);
    }

    /**
     * Creates a new streaming aperture processor, which finds the result of some given function
     * <i>f</i>(<i>S</i>) of the <i>aperture sum S</i>
     * of the source matrix&nbsp;<b>M</b>.
     * See {@link RankMorphology comments to RankMorphology}, section 4 about the
     * "<i>aperture sum</i>" term.
     *
     * <p>More precisely, let <tt>rm</tt> is an instance of {@link BasicRankMorphology}, created by the call
     *
     * <blockquote><tt>rm = {@link
     * BasicRankMorphology#getInstance(ArrayContext context, double dilationLevel, CustomRankPrecision precision)},
     * </tt></blockquote>
     *
     * <p>so that <tt>context</tt> is the same as the argument
     * of this method and other argument are any (they do not affect calculating the aperture sum).
     * Then in the streaming aperture processor, created by this method:
     *
     * <ul>
     * <li>no additional matrices are required;</li>
     * <li>{@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equivalent to
     * <b>R</b>=<tt>rm.{@link
     * RankMorphology#asFunctionOfSum(Matrix, net.algart.math.patterns.Pattern, Func)
     * asFunctionOfSum}</tt>(<b>M</b>, <b>P</b>, <tt>processingFunc</tt>)
     * (where <tt>processingFunc</tt> is the argument of this method),
     * if the <tt>requiredType</tt> argument of
     * {@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equal to
     * <b>M</b>.{@link Matrix#type() type()}==<b>R</b>.{@link Matrix#type() type()};</li>
     *
     * <li>if <tt>requiredType</tt> is not equal to <b>M</b>.{@link Matrix#type() type()}, it analogously calculates
     * the function <i>f</i>(<i>S</i>) of the aperture sum <i>S</i>
     * with maximal (<tt>double</tt>) precision and then
     * casts the floating-point results to the desired element type by the same rules
     * as {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <tt>truncateOverflows=true</tt>;
     * note that such result cannot be obtained by {@link RankMorphology} methods;</li>
     *
     * <li>{@link
     * StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * process} method is equivalent to
     * <tt>rm.{@link
     * RankMorphology#functionOfSum(Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * functionOfSum}</tt>(<b>R</b>, <b>M</b>, <b>P</b>, <tt>processingFunc</tt>)
     * (where <tt>processingFunc</tt> is the argument of this method).</li>
     * </ul>
     *
     * <p>This processor is really created and called in the implementation of
     * {@link BasicRankMorphology#asFunctionOfSum(Matrix, net.algart.math.patterns.Pattern, Func)
     * asFunctionOfSum}
     * and {@link BasicRankMorphology#functionOfSum(Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * functionOfSum}
     * methods in {@link BasicRankMorphology} class.
     *
     * @param context        the {@link StreamingApertureProcessor#context() context} that will be used by this object;
     *                       can be {@code null}, then it will be ignored.
     * @param processingFunc the function, which should be applied to every calculated aperture sum.
     * @return               the new streaming aperture processor, finding the given function of the aperture sum.
     * @throws NullPointerException     if <tt>bitLevels</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>bitLevels.length==0</tt>, or if <tt>bitLevels.length&gt;31</tt>,
     *                                  or if some of the elements <tt>bitLevels</tt> is not in 1..30 range, or if
     *                                  <tt>bitLevels</tt>[<i>k</i>]&gt;=<tt>bitLevels</tt>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static StreamingApertureProcessor getSummator(ArrayContext context, Func processingFunc) {
        return new Summator(context, processingFunc);
    }

    /**
     * Creates a new streaming aperture processor, which finds the result of some given function
     * <i>f</i>(<i>v</i><sub>0</sub>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>)
     * of some matrix <b>M</b><sub>0</sub>
     * and two <i>percentiles</i> <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>
     * of the source matrix&nbsp;<b>M</b>.
     * See {@link RankMorphology comments to RankMorphology}, section 4 about the "<i>percentile</i>" term.
     * The real indexes <i>r</i> of two percentiles for every element of the result are equal
     * to the corresponding elements of the additional matrices <b>M</b><sub>1</sub> and <b>M</b><sub>2</sub>.
     *
     * <p>More precisely, let <tt>rm</tt> is an instance of {@link BasicRankMorphology}, created by the call
     *
     * <blockquote><tt>rm = {@link
     * BasicRankMorphology#getInstance(ArrayContext context, double dilationLevel, CustomRankPrecision precision)},
     * </tt></blockquote>
     *
     * <p>so that <tt>context</tt>, <tt>precision.{@link CustomRankPrecision#interpolated() interpolated()}</tt> and
     * <tt>precision.{@link CustomRankPrecision#bitLevels() bitLevels()}</tt> are the same as the arguments
     * of this method. Then in the streaming aperture processor, created by this method:
     *
     * <ul>
     * <li>the number of required additional matrices is 3;</li>
     * <li>{@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equivalent to
     * <b>R</b>=<tt>rm.{@link
     * RankMorphology#asFunctionOfPercentilePair(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * asFunctionOfPercentilePair}</tt>(<b>M</b>, <b>M</b><sub>1</sub>, <b>M</b><sub>2</sub>, <b>P</b>,
     * <tt>processingFunc</tt>) (where <tt>processingFunc</tt> is the argument of this method),
     * if <b>M</b><sub>0</sub>==<b>M</b> and the <tt>requiredType</tt> argument of
     * {@link
     * StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * asProcessed} method is equal to
     * <b>M</b>.{@link Matrix#type() type()}==<b>R</b>.{@link Matrix#type() type()};</li>
     *
     * <li>if <tt>requiredType</tt> is not equal to
     * <b>M</b>.{@link Matrix#type() type()}, it analogously calculates
     * the function <i>f</i>(<i>v</i><sub>0</sub>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>)
     * of the matrix <b>M</b><sub>0</sub>
     * and two percentiles <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>
     * of the source matrix&nbsp;<b>M</b>
     * with maximal (<tt>double</tt>) precision and then
     * casts the floating-point results to the desired element type by the same rules
     * as {@link Arrays#asFuncArray(boolean, Func, Class, PArray...)}
     * method with the argument <tt>truncateOverflows=true</tt>;</li>
     *
     * <li>if <b>M</b><sub>0</sub>==<b>M</b>, {@link
     * StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, net.algart.math.patterns.Pattern)
     * process} method is equivalent to
     * <tt>rm.{@link
     * RankMorphology#functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * functionOfPercentilePair}</tt>(<b>R</b>, <b>M</b>, <b>M</b><sub>1</sub>, <b>M</b><sub>2</sub>, <b>P</b>,
     * <tt>processingFunc</tt>) (where <tt>processingFunc</tt> is the argument of this method),
     * in other case if works analogously, but gets the first argument of <i>f</i> function from <b>M</b><sub>0</sub>
     * matrix instead of <b>M</b>.</li>
     * </ul>
     *
     * <p>Note that this processor, unlike {@link
     * RankMorphology#asFunctionOfPercentilePair(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * RankMorphology.asFunctionOfPercentilePair} and {@link
     * RankMorphology#functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * RankMorphology.functionOfPercentilePair}
     * methods, allows to use any matrix <b>M</b><sub>0</sub> as the source of first arguments of the function
     * <i>f</i>(<i>v</i><sub>0</sub>, <i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>).
     * (Those methods always get the first function argument from the source <b>M</b> matrix.)
     *
     * <p>This processor is really created and called in the implementation of
     * {@link
     * BasicRankMorphology#asFunctionOfPercentilePair(Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * asFunctionOfPercentilePair}
     * and {@link
     * BasicRankMorphology#functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, net.algart.math.patterns.Pattern, Func)
     * functionOfPercentilePair}
     * methods in {@link BasicRankMorphology} class. Those methods pass the same <tt>src</tt> matrix to this
     * processor twice: as the main source matrix <b>M</b> and as the first additional matrix <b>M</b><sub>0</sub>.
     *
     * @param context        the {@link StreamingApertureProcessor#context() context} that will be used by this object;
     *                       can be {@code null}, then it will be ignored.
     * @param processingFunc the function, which should be applied to every three
     *                       (<i>v</i><sub>0</sub>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>),
     *                       where <i>v</i><sub>0</sub> is the element of <b>M</b><sub>0</sub> matrix,
     *                       <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles
     *                       of the source <b>M</b> matrix.
     * @param interpolated   the histogram model used while calculating percentiles: <tt>true</tt> means
     *                       the precise histogram model, <tt>false</tt> means the simple histogram model
     *                       (see comments to {@link Histogram} class).
     * @param bitLevels      the {@link CustomRankPrecision#bitLevels() bit levels} used while calculations.
     * @return               the new streaming aperture processor, finding the given function of the additional
     *                       matrix <b>M</b><sub>0</sub> and the pair of percentiles.
     * @throws NullPointerException     if <tt>bitLevels</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>bitLevels.length==0</tt>, or if <tt>bitLevels.length&gt;31</tt>,
     *                                  or if some of the elements <tt>bitLevels</tt> is not in 1..30 range, or if
     *                                  <tt>bitLevels</tt>[<i>k</i>]&gt;=<tt>bitLevels</tt>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static StreamingApertureProcessor getPercentilePairProcessor(ArrayContext context,
        Func processingFunc, boolean interpolated, int... bitLevels)
    {
        return new PercentilePairProcessor(context, processingFunc, interpolated, bitLevels);
    }
}
