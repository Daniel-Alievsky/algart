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

import java.util.Objects;

/**
 * <p>Histogram: an array of non-negative integer numbers <b>b</b>[<i>v</i>], 0&le;<i>v</i>&lt;<i>M</i>,
 * where every element <b>b</b>[<i>v</i>] represents the number of occurrence of the value <i>v</i>
 * in some source array <b>A</b>, consisting of integer elements in 0..<i>M</i>&minus;1 range.
 * The integer values <i>v</i> in the source array are 31-bit:
 * 0&le;<i>M</i>&lt;2<sup>31</sup> (<code>int</code> Java type). The elements (bars) of the histogram
 * <b>b</b>[<i>v</i>] and also the total number of elements in the source array <i>N</i> are 63-bit:
 * 0&le;<i>N</i>&lt;2<sup>63</sup> (<code>long</code> Java type).
 * The source array <b>A</b> is always supposed to be <i>sorted in increasing order</i>:
 * <b>A</b>[0]&le;<b>A</b>[1]&le;...&le;<b>A</b>[<i>N</i>&minus;1], where <i>N</i>
 * is the number of elements in <b>A</b> (obviously, <i>N</i> is equal to the sum of all bars
 * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>M</i>&minus;1]).</p>
 *
 * <p>This class is designed for solving 2 basic tasks:</p>
 *
 * <ol>
 * <li>to find the value <i>v</i>=<b>A</b>[<i>r</i>], if we know its index <i>r</i> in the sorted array <b>A</b>:
 * this value is called the <i>percentile</i> #<i>r</i> (well-known special case is the <i>median</i>,
 * when <i>r</i>=<i>N</i>/2);</li>
 * <li>to find the index <i>r</i> in the sorted array <b>A</b>, if we know the corresponding value
 * <i>v</i>=<b>A</b>[<i>r</i>]:
 * this value is called the <i>rank</i> of the value <i>v</i>.</li>
 * </ol>
 *
 * <p>It's important that this class does not store and does not try to sort the source array <b>A</b>, it stores
 * only the histogram <b>b</b> and solves both tasks on the base of it.</p>
 *
 * <p>According the simple definition above, the rank and the percentile are integer numbers.
 * However, this class works not with integer, but with <i>real</i> precision, using
 * some generalization of the integer rank and percentile to the floating-point case.
 * Below is the formal definition of the real rank and percentile.</p>
 *
 * <blockquote>
 * <table border="1" style="border-spacing:0">
 * <caption>
 *     <b>Definition of floating-point percentile <i>v</i>(<i>r</i>) and rank <i>r</i>(<i>v</i>)</b>
 * </caption>
 * <tr><td style="padding:8px">
 * <p>Let <b>b</b>[0..<i>M</i>&minus;1] be an array of non-negative integer numbers, called
 * the <i>histogram</i> (and stored by this class), and let <i>N</i> be the sum of all these elements
 * (the length of the supposed, but not stored source sorted array <b>A</b>). In addition,
 * we suppose that there is an additional "virtual" element <b>b</b>[<i>M</i>], which is always zero.
 * The elements <b>b</b>[<i>k</i>] are called the <i>bars</i> of the histogram.</p>
 *
 * <p>This class operates with the function <i>v</i>(<i>r</i>), called the <i>percentile</i>,
 * and its inverse function <i>v</i>(<i>r</i>), called the <i>rank</i>. Both variables <i>r</i> and <i>v</i>
 * are real (floating-point) numbers in the following ranges: 0.0&le;<i>r</i>&le;<i>N</i>,
 * 0.0&le;<i>v</i>&le;<i>M</i>. The definition of both functions depends on so called
 * <i>histogram model</i>. This class supports two following models.</p>
 *
 * <dl>
 * <dt><b>Simple histogram model</b></dt>
 *
 * <dd>
 * <p>In this case, <i>r</i>(<i>v</i>) is a simple polyline generalization of the integer function:
 * while <i>v</i> is increasing from some integer
 * <i>v</i><sub>0</sub> to the next integer <i>v</i><sub>0</sub>+1
 * (some bar of the histogram),
 * the rank <i>r</i>(<i>v</i>) is either the constant if <b>b</b>[<i>v</i><sub>0</sub>]=0,
 * or uniformly increased from some <i>r</i><sub>0</sub> to
 * <i>r</i><sub>0</sub>+<b>b</b>[<i>v</i><sub>0</sub>] if
 * <b>b</b>[<i>v</i><sub>0</sub>]&gt;0.
 * More precisely:</p>
 * <ol>
 * <li>
 * <i>v</i>(<i>r</i>) = <i>v</i><sub>0</sub> +
 * (<i>r</i>&minus;<i>r</i><sub>0</sub>)/<b>b</b>[<i>v</i><sub>0</sub>], where <i>v</i><sub>0</sub> is
 * the minimal integer value so that
 * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>]&gt;&lfloor;<i>r</i>&rfloor;
 * (here and below &lfloor;<i>x</i>&rfloor; means the integer part of <i>x</i> or <code>(long)</code><i>x</i>
 * for our non-negative numbers),
 * and <i>r</i><sub>0</sub> = this&nbsp;sum&minus;<b>b</b>[<i>v</i><sub>0</sub>] =
 * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>&minus;1].
 * In the only special case <i>r</i>=<i>N</i>
 * this formula is not correct, because the sum of histogram bars <b>b</b>[<i>k</i>] cannot be greater than
 * the sum of all bars <i>N</i>, and we consider, by definition, that
 * <i>v</i>(<i>N</i>) = lim<sub><i>x</i>&rarr;<i>N</i>+1</sub><i>v</i>(<i>x</i>) =
 * &lfloor;<i>v</i>(<i>N</i>&minus;1)&rfloor;+1.<br>
 * Note: according this definition,
 * <i>v</i>(0) = <i>min</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)
 * and <i>v</i>(<i>N</i>) = &lfloor;<i>v</i>(<i>N</i>&minus;1)&rfloor;+1 = <i>max</i> (<i>k</i>&isin;<b>Z</b>:
 * <b>b</b>[<i>k</i>]&gt;0)+1.<br>
 * In a case <i>N</i>=0 (empty histogram), <i>v</i>(<i>r</i>) functions is considered to be undefined.
 * <br>&nbsp;</li>
 *
 * <li><i>r</i>(<i>v</i>) = <i>r</i><sub>0</sub> +
 * (<i>v</i>&minus;<i>v</i><sub>0</sub>)*<b>b</b>[<i>v</i><sub>0</sub>], where
 * <i>v</i><sub>0</sub>=&lfloor;<i>v</i>&rfloor; and
 * <i>r</i><sub>0</sub> = <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>&minus;1].<br>
 * Note: according this definition, <i>r</i>(<i>v</i>)=0 when <i>v</i>&lt;<i>v</i>(0) and
 * <i>r</i>(<i>v</i>)=<i>N</i> when <i>v</i>&gt;<i>v</i>(<i>N</i>).
 * In particular, <i>r</i>(0)=0
 * and <i>r</i>(<i>M</i>)=<i>N</i> (remember that <b>b</b>[<i>M</i>]=0 by definition).<br>
 * Unlike <i>v</i>(<i>r</i>), <i>r</i>(<i>v</i>) function is defined also if <i>N</i>=0: in this case it is
 * the zero constant.
 * </li>
 * </ol>
 * <p>It's easy to see that the <i>r</i>(<i>v</i>) function is continuous, but, unfortunately,
 * the <i>v</i>(<i>r</i>) function is discontinuous if some bars <b>b</b>[<i>k</i>] are zero.
 * This behavior is not too good for calculating percentiles on sparse histograms, when a lot of bars are zero.
 * In this case, this generalization to floating-point almost does not improve the precision of
 * the calculated percentile.</p>
 * <p>To better understand the sense of this model, please also read the comments
 * to {@link #value(long[], double)} method.</p>
 * </dd>
 *
 * <dt><b>Precise histogram model</b></dt>
 *
 * <dd>
 * <p>Here <i>v</i>(<i>r</i>) and <i>r</i>(<i>v</i>) are also polyline functions, and they are identical
 * to these function of the simple model in all points where the rank is integer (<i>r</i>&isin;<b>Z</b>).
 * However, if the rank is not integer, we consider, by definition, that
 * <i>v</i>(<i>r</i>) is uniformly increased from <i>v</i>(<i>r</i><sub>0</sub>) to
 * <i>v</i>(<i>r</i><sub>0</sub>+1), where
 * <i>r</i><sub>0</sub>=&lfloor;<i>r</i>&rfloor;.
 * In other words, we interpolate the percentile <i>v</i>(<i>r</i><sub>0</sub>) between integer ranks.
 * More precisely:</p>
 * <ol>
 * <li>
 * <i>v</i>(<i>r</i>) =
 *     <ul>
 *     <li><i>v</i><sub>0</sub> + (<i>r</i>&minus;<i>r</i><sub>0</sub>)/<i>b</i>,
 *     where <i>v</i><sub>0</sub> is the minimal integer value so that
 *     <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>]&gt;&lfloor;<i>r</i>&rfloor;,
 *     <i>r</i><sub>0</sub> = <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>&minus;1],
 *     <i>b</i> = <b>b</b>[<i>v</i><sub>0</sub>]
 *     &mdash; if <i>b</i>&gt;1 and <i>r</i><sub>0</sub> &le; <i>r</i> &le;
 *     <i>r</i><sub>0</sub>+<i>b</i>&minus;1 = <i>r</i><sub>0</sub>';
 *     </li>
 *     <li><i>v</i><sub>0</sub>' +
 *     (<i>r</i>&minus;<i>r</i><sub>0</sub>')*(<i>v</i><sub>1</sub>&minus;<i>v</i><sub>0</sub>'),
 *     where <i>v</i><sub>0</sub>' = <i>v</i><sub>0</sub>+(<i>b</i>&minus;1)/<i>b</i>
 *     = <i>v</i>(<i>r</i><sub>0</sub>')
 *     and <i>v</i><sub>1</sub> = <i>min</i> (<i>k</i>&isin;<b>Z</b>: <i>k</i>&gt;<i>v</i><sub>0</sub>
 *     &amp; <b>b</b>[<i>k</i>]&gt;0) = <i>v</i>(<i>r</i><sub>0</sub>'+1) is the next non-zero histogram bar
 *     &mdash; if <i>r</i><sub>0</sub>'&le;<i>r</i>&le;<i>r</i><sub>0</sub>'+1=<i>r</i><sub>1</sub>.
 *     In a case <i>r</i>&gt;<i>N</i>&minus;1, there is no the next non-zero bar and we consider, by definition,
 *     that <i>v</i><sub>1</sub> = <i>v</i><sub>0</sub>+1.
 *     </li>
 *     </ul>
 * As in the simple model, in the special case <i>r</i>=<i>N</i> we consider, by definition, that
 * <i>v</i>(<i>N</i>) = lim<sub><i>x</i>&rarr;<i>N</i>+1</sub><i>v</i>(<i>x</i>) =
 * &lfloor;<i>v</i>(<i>N</i>&minus;1)&rfloor;+1.<br>
 * As in the simple model, here
 * <i>v</i>(0) = <i>min</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)
 * and <i>v</i>(<i>N</i>) = &lfloor;<i>v</i>(<i>N</i>&minus;1)&rfloor;+1 = <i>max</i> (<i>k</i>&isin;<b>Z</b>:
 * <b>b</b>[<i>k</i>]&gt;0)+1.<br>
 * In a case <i>N</i>=0 (empty histogram), <i>v</i>(<i>r</i>) functions is considered to be undefined.
 * <br>&nbsp;</li>
 *
 * <li><i>r</i>(<i>v</i>) is defined as the inverse function for <i>v</i>(<i>r</i>) if
 * <i>v</i>(0)&le;<i>v</i>&le;<i>v</i>(<i>N</i>);
 * outside this range, we consider <i>r</i>(<i>v</i>)=0 when <i>v</i>&lt;<i>v</i>(0) and
 * <i>r</i>(<i>v</i>)=<i>N</i> when <i>v</i>&gt;<i>v</i>(<i>N</i>).
 * As in the simple model, in the special case <i>N</i>=0 we consider, by definition,
 * that <i>r</i>(<i>v</i>)=0 for any <i>v</i>.
 * </li>
 * </ol>
 * <p>In this model both functions <i>v</i>(<i>r</i>) and <i>r</i>(<i>v</i>) are increasing and continuous.
 * But calculations are more complicated. The difference appears if there are empty bars
 * (<b>b</b>[<i>k</i>]=0); namely, in each non-empty bar
 * <b>b</b>[<i>v</i><sub>0</sub>]=<i>b</i>, followed by empty bars, there is new salient point of
 * the polyline <i>r</i>(<i>v</i>): <i>v</i> = <i>v</i><sub>0</sub>' =
 * <i>v</i><sub>0</sub>+(<i>b</i>&minus;1)/<i>b</i>,
 * and after it the rank <i>r</i>(<i>v</i>) is increasing until the next rank
 * <i>r</i>(<i>v</i><sub>0</sub>)+<i>b</i> during all zero bars following after
 * <b>b</b>[<i>v</i><sub>0</sub>].
 * This feature does not appear at the right end of the histogram, if all bars following after
 * <b>b</b>[<i>v</i><sub>0</sub>] are zero.</p>
 *
 * <p>Note: the traditional definition of the <i>median</i> of the source array <b>A</b> with even length
 * <i>N</i>=2<i>n</i> is (<b>A</b>[<i>n</i>&minus;1]+<b>A</b>[<i>n</i>])/2
 * (if we suppose <b>A</b>[0]&le;<b>A</b>[1]&le;...&le;<b>A</b>[<i>N</i>&minus;1]).
 * This definition is identical to the percentile <i>v</i>(<i>n</i>&minus;0.5)
 * in the precise histogram model, if all bars contains 0 or 1 elements
 * (all <b>b</b>[<i>k</i>]&le;1).</p>
 * <p>To better understand the sense of this model, please also read the comments
 * to {@link #preciseValue(long[], double)} method.</p>
 * </dd>
 * </dl>
 *
 * </td></tr>
 * </table>
 * </blockquote>
 *
 * <p>This class is optimized for the case, when we already know some corresponding pair <i>r</i> (rank)
 * and <i>v</i> (percentile), and we need to slightly change the situation: add or remove several <b>A</b> elements,
 * then, maybe, little increase or decrease the known rank <i>r</i> or the value <i>v</i> and, after this,
 * quickly find (recalculate) the second element of the pair: correspondingly the percentile <i>v</i> or the
 * rank <i>r</i>. To do this, this class provides methods {@link #include(int) include} and
 * {@link #exclude(int) exlcude}, allowing to increment or decrement elements of <b>b</b> array
 * (this action corresponds to adding or removing elements to/from the source array <b>A</b>),
 * and supports the concept of the <i>current rank</i> and <i>current value (percentile)</i>
 * with a necessary set of methods for setting and getting them.
 * Usually all these method work quickly (<i>O</i>(1) operations), so every time when you correct the histogram
 * (add/remove elements) or change the current rank or the value, you can immediately read the second element
 * of the pair (correspondingly the percentile or the rank).</p>
 *
 * <p>More precisely, in every object of this class, at any moment, there are:</p>
 *
 * <ul>
 * <li><i>the current value v</i>: a real number in range 0&le;<i>v</i>&le;<i>M</i>,
 * that can be set by {@link #moveToValue(double)} method or got by {@link #currentValue()} method;</li>
 *
 * <li><i>the current simple rank r<sup>S</sup></i>: a real number in range 0&le;<i>v</i>&le;<i>N</i>,
 * that can be set by {@link #moveToRank(double)} method or got by {@link #currentRank()} method;</li>
 *
 * <li><i>the current precise rank r<sup>P</sup></i>: a real number in range 0&le;<i>v</i>&le;<i>N</i>,
 * that can be set by {@link #moveToPreciseRank(double)} method or got by {@link #currentPreciseRank()} method.</li>
 * </ul>
 *
 * <p>The methods {@link #include(int) include} and {@link #exclude(int) exlcude} do not change the current value,
 * but can change the current simple and precise ranks.</p>
 *
 * <p>This class guarantees that for any <i>v</i> we have
 * <i>r<sup>S</sup></i>=<i>r</i>(<i>v</i>) in terms of the simple histogram model and
 * <i>r<sup>P</sup></i>=<i>r</i>(<i>v</i>) in terms of the precise histogram model.
 * In particular, in the case <i>N</i>=0 both ranks are always zero:
 * <i>r<sup>S</sup></i>=<i>r<sup>P</sup></i>=0.</p>
 *
 * <p>This class guarantees that for any <i>r<sup>S</sup></i>
 * we have <i>v</i>=<i>v</i>(<i>r<sup>S</sup></i>) in terms of the simple histogram
 * model, excepting extreme values
 * <i>r<sup>S</sup></i>=0 and <i>r<sup>S</sup></i>=<i>N</i>
 * and also <i>r<sup>S</sup></i> values where
 * <i>v</i>(<i>r<sup>S</sup></i>) function is discontinuous in this model.
 * If such situation takes place after {@link #moveToValue(double)} call, the current value is equal
 * to the argument of this method.
 * If such situation takes place after {@link #moveToRank(double)} call, the current value is equal
 * to the <i>v</i>(<i>r<sup>S</sup></i>) according the definition of
 * <i>v</i>(<i>r</i>) function in the simple histogram model.
 * In particular, after the call
 * {@link #moveToRank(double) moveToRank(0)} the current value <i>v</i>
 * will be equal to <i>v</i>(0) and after the call
 * {@link #moveToRank(double) moveToRank(<i>N</i>)} the current value <i>v</i>
 * will be equal to <i>v</i>(<i>N</i>).
 * The case <i>N</i>=0 is processed in a special way: see comments to {@link #moveToRank(double)} method.</p>
 *
 * <p>This class guarantees that for any <i>r<sup>P</sup></i>
 * we have <i>v</i>=<i>v</i>(<i>r<sup>P</sup></i>) in terms of the precise histogram
 * model, excepting extreme values
 * <i>r<sup>P</sup></i>=0 and <i>r<sup>P</sup></i>=<i>N</i>.
 * If <i>r<sup>P</sup></i>=0,
 * the corresponding value <i>v</i> can be any number in range 0..<i>v</i>(0)
 * if you set it by {@link #moveToValue(double)} method; but after the call
 * {@link #moveToPreciseRank(double) moveToPreciseRank(0)} the current value <i>v</i>
 * will be equal to <i>v</i>(0).
 * If <i>r<sup>P</sup></i>=<i>N</i>,
 * the corresponding value <i>v</i> can be any number in range <i>v</i>(<i>N</i>)..<i>M</i>
 * if you set it by {@link #moveToValue(double)} method; but after the call
 * {@link #moveToPreciseRank(double) moveToPreciseRank(<i>N</i>)} the current value <i>v</i>
 * will be equal to <i>v</i>(<i>N</i>).
 * The case <i>N</i>=0 is processed in a special way: see comments to {@link #moveToPreciseRank(double)} method.</p>
 *
 * <p>There are additional methods for setting integer values and ranks:
 * {@link #moveToIValue(int)} and {@link #moveToIRank(long)}. They are strictly equivalent
 * to floating-point methods {@link #moveToValue(double)} and {@link #moveToRank(double)},
 * called with the same argument, but work little faster. There is no special integer version
 * of {@link #moveToPreciseRank(double)} method, because for integer ranks the simple and the precise
 * histogram models are identical.</p>
 *
 * <p>There are additional methods for getting integer values and ranks:
 * {@link #currentIValue()} and {@link #currentIRank()}.
 * The {@link #currentIValue()} method returns the current value, rounded to an integer.
 * The rules of rounding are complicated enough and described in the comments to this method;
 * depending on the situation, it can work as rounding to the nearest integer (<code>Math.round</code>)
 * or as truncating (<code>Math.floor</code>). In any case, there is a guarantee that
 * {@link #currentIValue()}&minus;0.5&le;<i>v</i>&lt;{@link #currentIValue()}+1.
 * The {@link #currentIRank()} method return the rank, corresponding to <i>w</i>={@link #currentIValue()}:
 * it is equal to <i>r</i>(&lfloor;<i>w</i>&rfloor;) (this rank is integer and does not
 * depend on the histogram model).</p>
 *
 * <p>You can create an instance of this class by the following methods:</p>
 *
 * <ul>
 * <li>{@link #newLongHistogram(int histogramLength, int... bitLevelsOfPyramid)};</li>
 * <li>{@link #newLongHistogram(long[] histogram, int... bitLevelsOfPyramid)};</li>
 * <li>{@link #newIntHistogram(int histogramLength, int... bitLevelsOfPyramid)};</li>
 * <li>{@link #newIntHistogram(int[] histogram, int... bitLevelsOfPyramid)}.</li>
 * </ul>
 *
 * <p>After creation, the only way to change the bars <b>b</b>[<i>k</i>] is using
 * {@link #include(int)}, {@link #exclude(int)},  {@link #include(int...)} and {@link #exclude(int...)} methods.
 * After creation, the current value <i>v</i> and both current ranks
 * <i>r<sup>S</sup></i> and <i>r<sup>P</sup></i>
 * are zero.</p>
 *
 * <p>Sometimes you need calculating (and supporting) not one, but several pairs "current value + current rank"
 * in the same histogram. If is possible to use a single object of this class and move from one pair to another,
 * but if the necessary values are not close to each other, the <code>moveTo...</code> methods work relatively
 * slowly. In this case, you can <i>share</i> the histogram <b>b</b>[<i>k</i>] between several instance of this
 * class by {@link #share()} method. The sharing instances work with the same <b>b</b>[<i>k</i>] array:
 * any modification by {@link #include include} / {@link #exclude exclude} methods are immediately reflected
 * in all shared instances. But each sharing instance has an independent set of current value, current simple rank
 * and current precise rank. You can get all sharing instances by {@link #nextSharing()} method.</p>
 *
 * <p>This class also provides static methods for calculating <i>v</i>(<i>r</i>) function:
 * {@link #value(long[] histogram, double rank)}, {@link #value(int[] histogram, double rank)}
 * for the simple histogram model,
 * {@link #preciseValue(long[] histogram, double rank)}, {@link #preciseValue(int[] histogram, double rank)}
 * for the precise histogram model.
 * These methods can be useful if you need to process the given histogram only once.</p>
 *
 * <p>There is no guarantee that the same results, got by different ways (for example,
 * by static methods and by creating an instance of this class and using its methods) are absolutely identical:
 * little mismatches in the last digits after the decimal point are possible.</p>
 *
 * <p>This class does not implement own <code>equals</code> and <code>hashCode</code> methods.
 * So, this class does not provide a mechanism for comparing different histograms.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class Histogram {
    static final boolean DEBUG_MODE = false; // thorough checking invariants and algorithms; slows down calculations

    final int length;
    int currentIValue = 0;
    double currentValue = 0.0;
    double currentPreciseRank = Double.NaN;

    Histogram(int length) {
        this.length = length;
    }

    /**
     * Creates new histogram, consisting of <i>M</i>=<code>histogramLength</code> empty bars.
     * In other words, all bars <b>b</b>[<i>k</i>]=0 at first; but they can be increased by
     * {@link #include(int)} method.
     *
     * <p>The <code>bitLevelsOfPyramid</code> argument is used for optimization of large histograms, consisting
     * of thousands or millions bars. Namely, this class automatically builds and supports a
     * <i>pyramid of histograms</i>: <i>m</i>=<code>bitLevelsOfPyramid.length</code> additional arrays
     * <b>b</b><sup>1</sup>[<i>k</i>], <b>b</b><sup>2</sup>[<i>k</i>], ...,
     * <b>b</b><sup><i>m</i></sup>[<i>k</i>], where
     *
     * <blockquote>
     * <b>b</b><sup><i>q</i></sup>[<i>k</i>] =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub>2<sup><i>s</i></sup>*<i>k</i>
     * &le; <i>j</i> &lt; min(2<sup><i>s</i></sup>*(<i>k</i>+1), <i>M</i>)</sub>
     * <b>b</b>[<i>j</i>], <i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1];<br>
     * <b>b</b><sup><i>q</i></sup><code>.length</code> = &lfloor;<i>M</i>/2<sup><i>s</i></sup>&rfloor;
     * = <code>histogramLength</code> &gt;&gt; <code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1].
     * </blockquote>
     *
     * <p>In other words, every "sub-histogram" <b>b</b><sup><i>q</i></sup> consists of "wide" bars,
     * where the width of bars is 2<sup><i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>:
     * it is a sum of 2<sup><i>s</i></sup> bars <b>b</b>[<i>j</i>] of the base histogram,
     * excepting the last "wide" bar which can be a sum of fewer bars <b>b</b>[<i>j</i>].
     * The elements of <code>bitLevelsOfPyramid</code> array must be
     * in 1..31 range and must be listed in increasing order:
     * 0&lt;<code>bitLevelsOfPyramid</code>[0]&lt;...&lt;<code>bitLevelsOfPyramid</code>[<i>m</i>&minus;1]&le;31.
     *
     * <p>Supporting this pyramid little slows down {@link #include(int)} and {@link #exclude(int)} methods:
     * they require <i>O</i>(<i>m</i>) operations to correct <i>m</i> arrays <b>b</b><sup><i>q</i></sup>.
     * But it can essentially optimize all methods which set the current value and rank:
     * in the worst case they require
     *
     * <blockquote>
     * <i>O</i> (<b>b</b><sup><i>m</i></sup><code>.length</code> + <span style="font-size:200%">&sum;</span>
     * <sub><i>q</i>=1,2,...,<i>m</i>&minus;1</sub>2<sup><code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>)
     * </blockquote>
     *
     * <p>and sequential settings of the current value and rank work much faster if the current value changes slightly.
     *
     * <p>Without the pyramid of histograms (<i>m</i>=<code>bitLevelsOfPyramid.length=0</code>), the required time
     * of setting the current value or rank is <i>O</i>(<i>M</i>) in the worst case,
     * and sequential settings of the current value and rank also work much faster if the current
     * value changes slightly.
     * If the histogram length <i>M</i> is not large, for example, 256 or less, it is possible that this class
     * will work faster without <code>bitLevelsOfPyramid</code> arguments.
     *
     * <p>The passed <code>bitLevelsOfPyramid</code> argument is cloned by this method:
     * no references to it are maintained by the created object.
     *
     * <p>If you are sure that the sum of all bars <b>b</b>[<i>k</i>] (the total length of the supposed
     * source array <b>A</b>) will never exceed <code>Integer.MAX_VALUE</code>, you can use
     * {@link #newIntHistogram(int, int...)} method instead of this one:
     * the created object will probably work little faster and will occupy less memory.
     *
     * @param histogramLength    the number <i>M</i> of bars of the new histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new histogram with zero (empty) bars <b>b</b>[<i>k</i>]=0.
     * @throws NullPointerException     if <code>bitLevelsOfPyramid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>histogramLength&lt;0</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code> is not
     *                                  in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static Histogram newLongHistogram(int histogramLength, int... bitLevelsOfPyramid) {
        if (histogramLength < 0)
            throw new IllegalArgumentException("Negative histogramLength");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        return bitLevelsOfPyramid.length == 0 ?
                new Long1LevelHistogram(new long[histogramLength], bitLevelsOfPyramid, true) :
                new LongHistogram(new long[histogramLength], bitLevelsOfPyramid, true);
    }

    /**
     * Creates new histogram, consisting of <i>M</i>=<code>histogram.length</code> bars, equal to elements
     * of the given array.
     * In other words, the bars <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>] at first.
     *
     * <p>The <code>bitLevelsOfPyramid</code> argument is used for optimization of large histograms, consisting
     * of thousands or millions bars. Namely, this class automatically builds and supports a
     * <i>pyramid of histograms</i>: <i>m</i>=<code>bitLevelsOfPyramid.length</code> additional arrays
     * <b>b</b><sup>1</sup>[<i>k</i>], <b>b</b><sup>2</sup>[<i>k</i>], ...,
     * <b>b</b><sup><i>m</i></sup>[<i>k</i>], where
     *
     * <blockquote>
     * <b>b</b><sup><i>q</i></sup>[<i>k</i>] =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub>2<sup><i>s</i></sup>*<i>k</i>
     * &le; <i>j</i> &lt; min(2<sup><i>s</i></sup>*(<i>k</i>+1), <i>M</i>)</sub>
     * <b>b</b>[<i>j</i>], <i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1];<br>
     * <b>b</b><sup><i>q</i></sup><code>.length</code> = &lfloor;<i>M</i>/2<sup><i>s</i></sup>&rfloor;
     * = <code>histogram.length</code> &gt;&gt; <code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1].
     * </blockquote>
     *
     * <p>In other words, every "sub-histogram" <b>b</b><sup><i>q</i></sup> consists of "wide" bars,
     * where the width of bars is 2<sup><i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>:
     * it is a sum of 2<sup><i>s</i></sup> bars <b>b</b>[<i>j</i>] of the base histogram,
     * excepting the last "wide" bar which can be a sum of fewer bars <b>b</b>[<i>j</i>].
     * The elements of <code>bitLevelsOfPyramid</code> array must be
     * in 1..31 range and must be listed in increasing order:
     * 0&lt;<code>bitLevelsOfPyramid</code>[0]&lt;...&lt;<code>bitLevelsOfPyramid</code>[<i>m</i>&minus;1]&le;31.
     *
     * <p>Supporting this pyramid little slows down {@link #include(int)} and {@link #exclude(int)} methods:
     * they require <i>O</i>(<i>m</i>) operations to correct <i>m</i> arrays <b>b</b><sup><i>q</i></sup>.
     * But it can essentially optimize all methods which set the current value and rank:
     * in the worst case they require
     *
     * <blockquote>
     * <i>O</i> (<b>b</b><sup><i>m</i></sup><code>.length</code> + <span style="font-size:200%">&sum;</span>
     * <sub><i>q</i>=1,2,...,<i>m</i>&minus;1</sub>2<sup><code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>)
     * </blockquote>
     *
     * <p>and sequential settings of the current value and rank work much faster if the current value changes slightly.
     *
     * <p>Without the pyramid of histograms (<i>m</i>=<code>bitLevelsOfPyramid.length=0</code>), the required time
     * of setting the current value or rank is <i>O</i>(<i>M</i>) in the worst case,
     * and sequential settings of the current value and rank also work much faster if the current
     * value changes slightly.
     * If the histogram length <i>M</i> is not large, for example, 256 or less, it is possible that this class
     * will work faster without <code>bitLevelsOfPyramid</code> arguments.
     *
     * <p>The passed <code>histogram</code> and <code>bitLevelsOfPyramid</code> arguments are cloned by this method:
     * no references to them are maintained by the created object.
     *
     * <p>If you are sure that the sum of all bars <b>b</b>[<i>k</i>] (the total length of the supposed
     * source array <b>A</b>) will never exceed <code>Integer.MAX_VALUE</code>, you can use
     * {@link #newIntHistogram(int[], int...)} method instead of this one:
     * the created object will probably work little faster and will occupy less memory.
     *
     * @param histogram          initial values of the bars <b>b</b>[<i>k</i>] of the histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new histogram with bars <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>].
     * @throws NullPointerException     if <code>histogram</code> or <code>bitLevelsOfPyramid</code>
     *                                  argument is {@code null}.
     * @throws IllegalArgumentException if some of <code>histogram</code> elements are negative (&lt;0),
     *                                  or if sum of all bars (elements of <code>histogram</code> array) is greater
     *                                  than <code>Long.MAX_VALUE</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code> is not
     *                                  in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static Histogram newLongHistogram(long[] histogram, int... bitLevelsOfPyramid) {
        Objects.requireNonNull(histogram, "Null histogram argument");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        return bitLevelsOfPyramid.length == 0 ?
                new Long1LevelHistogram(histogram.clone(), bitLevelsOfPyramid, false) :
                new LongHistogram(histogram.clone(), bitLevelsOfPyramid, false);
    }

    /**
     * Creates new 32-bit histogram, consisting of <i>M</i>=<code>histogramLength</code> empty bars.
     * In other words, all bars <b>b</b>[<i>k</i>]=0 at first; but they can be increased by
     * {@link #include(int)} method.
     * "32-bit" means that the sum of all bars <b>b</b>[<i>k</i>] (the total length of the supposed
     * source array <b>A</b>) will not be able to exceed <code>Integer.MAX_VALUE</code>.
     * If you need to process greater numbers, please use {@link #newLongHistogram(int, int...)} method
     * instead of this one.
     *
     * <p>The <code>bitLevelsOfPyramid</code> argument is used for optimization of large histograms, consisting
     * of thousands or millions bars. Namely, this class automatically builds and supports a
     * <i>pyramid of histograms</i>: <i>m</i>=<code>bitLevelsOfPyramid.length</code> additional arrays
     * <b>b</b><sup>1</sup>[<i>k</i>], <b>b</b><sup>2</sup>[<i>k</i>], ...,
     * <b>b</b><sup><i>m</i></sup>[<i>k</i>], where
     *
     * <blockquote>
     * <b>b</b><sup><i>q</i></sup>[<i>k</i>] =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub>2<sup><i>s</i></sup>*<i>k</i>
     * &le; <i>j</i> &lt; min(2<sup><i>s</i></sup>*(<i>k</i>+1), <i>M</i>)</sub>
     * <b>b</b>[<i>j</i>], <i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1];<br>
     * <b>b</b><sup><i>q</i></sup><code>.length</code> = &lfloor;<i>M</i>/2<sup><i>s</i></sup>&rfloor;
     * = <code>histogramLength</code> &gt;&gt; <code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1].
     * </blockquote>
     *
     * <p>In other words, every "sub-histogram" <b>b</b><sup><i>q</i></sup> consists of "wide" bars,
     * where the width of bars is 2<sup><i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>:
     * it is a sum of 2<sup><i>s</i></sup> bars <b>b</b>[<i>j</i>] of the base histogram,
     * excepting the last "wide" bar which can be a sum of fewer bars <b>b</b>[<i>j</i>].
     * The elements of <code>bitLevelsOfPyramid</code> array must be
     * in 1..31 range and must be listed in increasing order:
     * 0&lt;<code>bitLevelsOfPyramid</code>[0]&lt;...&lt;<code>bitLevelsOfPyramid</code>[<i>m</i>&minus;1]&le;31.
     *
     * <p>Supporting this pyramid little slows down {@link #include(int)} and {@link #exclude(int)} methods:
     * they require <i>O</i>(<i>m</i>) operations to correct <i>m</i> arrays <b>b</b><sup><i>q</i></sup>.
     * But it can essentially optimize all methods which set the current value and rank:
     * in the worst case they require
     *
     * <blockquote>
     * <i>O</i> (<b>b</b><sup><i>m</i></sup><code>.length</code> + <span style="font-size:200%">&sum;</span>
     * <sub><i>q</i>=1,2,...,<i>m</i>&minus;1</sub>2<sup><code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>)
     * </blockquote>
     *
     * <p>and sequential settings of the current value and rank work much faster if the current value changes slightly.
     *
     * <p>Without the pyramid of histograms (<i>m</i>=<code>bitLevelsOfPyramid.length=0</code>), the required time
     * of setting the current value or rank is <i>O</i>(<i>M</i>) in the worst case,
     * and sequential settings of the current value and rank also work much faster if the current
     * value changes slightly.
     * If the histogram length <i>M</i> is not large, for example, 256 or less, it is possible that this class
     * will work faster without <code>bitLevelsOfPyramid</code> arguments.
     *
     * <p>The passed <code>bitLevelsOfPyramid</code> argument is cloned by this method:
     * no references to it are maintained by the created object.
     *
     * @param histogramLength    the number <i>M</i> of bars of the new histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new histogram with zero (empty) bars <b>b</b>[<i>k</i>]=0.
     * @throws NullPointerException     if <code>bitLevelsOfPyramid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>histogramLength&lt;0</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code> is not
     *                                  in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static Histogram newIntHistogram(int histogramLength, int... bitLevelsOfPyramid) {
        if (histogramLength < 0)
            throw new IllegalArgumentException("Negative histogramLength");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        return bitLevelsOfPyramid.length == 0 ?
                new Int1LevelHistogram(new int[histogramLength], bitLevelsOfPyramid, true) :
                new IntHistogram(new int[histogramLength], bitLevelsOfPyramid, true);
    }

    /**
     * Creates new 32-bit histogram, consisting of <i>M</i>=<code>histogram.length</code> bars, equal to elements
     * of the given array.
     * In other words, the bars <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>] at first.
     * "32-bit" means that the sum of all bars <b>b</b>[<i>k</i>] (the total length of the supposed
     * source array <b>A</b>) cannot exceed and will not be able to exceed <code>Integer.MAX_VALUE</code>.
     * If you need to process greater numbers, please use {@link #newLongHistogram(long[], int...)} method
     * instead of this one.
     *
     * <p>The <code>bitLevelsOfPyramid</code> argument is used for optimization of large histograms, consisting
     * of thousands or millions bars. Namely, this class automatically builds and supports a
     * <i>pyramid of histograms</i>: <i>m</i>=<code>bitLevelsOfPyramid.length</code> additional arrays
     * <b>b</b><sup>1</sup>[<i>k</i>], <b>b</b><sup>2</sup>[<i>k</i>], ...,
     * <b>b</b><sup><i>m</i></sup>[<i>k</i>], where
     *
     * <blockquote>
     * <b>b</b><sup><i>q</i></sup>[<i>k</i>] =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub>2<sup><i>s</i></sup>*<i>k</i>
     * &le; <i>j</i> &lt; min(2<sup><i>s</i></sup>*(<i>k</i>+1), <i>M</i>)</sub>
     * <b>b</b>[<i>j</i>], <i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1];<br>
     * <b>b</b><sup><i>q</i></sup><code>.length</code> = &lfloor;<i>M</i>/2<sup><i>s</i></sup>&rfloor;
     * = <code>histogram.length</code> &gt;&gt; <code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1].
     * </blockquote>
     *
     * <p>In other words, every "sub-histogram" <b>b</b><sup><i>q</i></sup> consists of "wide" bars,
     * where the width of bars is 2<sup><i>s</i>=<code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>:
     * it is a sum of 2<sup><i>s</i></sup> bars <b>b</b>[<i>j</i>] of the base histogram,
     * excepting the last "wide" bar which can be a sum of fewer bars <b>b</b>[<i>j</i>].
     * The elements of <code>bitLevelsOfPyramid</code> array must be
     * in 1..31 range and must be listed in increasing order:
     * 0&lt;<code>bitLevelsOfPyramid</code>[0]&lt;...&lt;<code>bitLevelsOfPyramid</code>[<i>m</i>&minus;1]&le;31.
     *
     * <p>Supporting this pyramid little slows down {@link #include(int)} and {@link #exclude(int)} methods:
     * they require <i>O</i>(<i>m</i>) operations to correct <i>m</i> arrays <b>b</b><sup><i>q</i></sup>.
     * But it can essentially optimize all methods which set the current value and rank:
     * in the worst case they require
     *
     * <blockquote>
     * <i>O</i> (<b>b</b><sup><i>m</i></sup><code>.length</code> + <span style="font-size:200%">&sum;</span>
     * <sub><i>q</i>=1,2,...,<i>m</i>&minus;1</sub>2<sup><code>bitLevelsOfPyramid</code>[<i>q</i>&minus;1]</sup>)
     * </blockquote>
     *
     * <p>and sequential settings of the current value and rank work much faster if the current value changes slightly.
     *
     * <p>Without the pyramid of histograms (<i>m</i>=<code>bitLevelsOfPyramid.length=0</code>), the required time
     * of setting the current value or rank is <i>O</i>(<i>M</i>) in the worst case,
     * and sequential settings of the current value and rank also work much faster if the current
     * value changes slightly.
     * If the histogram length <i>M</i> is not large, for example, 256 or less, it is possible that this class
     * will work faster without <code>bitLevelsOfPyramid</code> arguments.
     *
     * <p>The passed <code>histogram</code> and <code>bitLevelsOfPyramid</code> arguments are cloned by this method:
     * no references to them are maintained by the created object.
     *
     * @param histogram          initial values of the bars <b>b</b>[<i>k</i>] of the histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new histogram with bars <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>].
     * @throws NullPointerException     if <code>histogram</code> or <code>bitLevelsOfPyramid</code>
     *                                  argument is {@code null}.
     * @throws IllegalArgumentException if some of <code>histogram</code> elements are negative (&lt;0),
     *                                  or if sum of all bars (elements of <code>histogram</code> array) is greater
     *                                  than <code>Integer.MAX_VALUE</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code> is not
     *                                  in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static Histogram newIntHistogram(int[] histogram, int... bitLevelsOfPyramid) {
        Objects.requireNonNull(histogram, "Null histogram argument");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        return bitLevelsOfPyramid.length == 0 ?
                new Int1LevelHistogram(histogram.clone(), bitLevelsOfPyramid, false) :
                new IntHistogram(histogram.clone(), bitLevelsOfPyramid, false);
    }

    /**
     * Returns the number <i>M</i> of bars of the histogram.
     *
     * <p>The result of this method is always non-negative (&ge;0).
     *
     * @return the number <i>M</i> of bars of the histogram.
     */
    public final int length() {
        return this.length;
    }

    /**
     * Returns the sum <i>N</i> of all bars <b>b</b>[<i>k</i>] of the histogram.
     * In other words, it is the current length of the supposed source array <b>A</b>,
     * on the base of which this histogram is built.
     *
     * <p>The result of this method is always non-negative (&ge;0).
     *
     * @return the sum <i>N</i> of all bars <b>b</b>[<i>k</i>] of the histogram.
     */
    public abstract long total();

    /**
     * Returns the bar <code>#value</code> of the histogram: <b>b</b>[<code>value</code>].
     * If the index <code>value</code> is negative or &ge;<i>M</i>={@link #length()},
     * this method returns <code>0</code> and does not throw an exception
     * (unlike {@link #include(int) include} and {@link #exclude(int) exclude} methods).
     *
     * <p>The result of this method is always non-negative (&ge;0).
     *
     * @param value the index of the bar; can be out of <code>0..</code><i>M</i> range.
     * @return the bar <code>#value</code> of the histogram.
     */
    public abstract long bar(int value);

    /**
     * Returns all bars of the histogram. The length of the returned array is <i>M</i>={@link #length()},
     * and the element #<i>k</i> is equal to <b>b</b>[<i>k</i>].
     *
     * <p>The returned array is never a reference to an internal array stored in this object:
     * if necessary, the internal Java array is cloned.
     *
     * @return all bars of the histogram as <code>long[]</code> Java array.
     */
    public abstract long[] bars();

    /**
     * Increments the bar <code>#value</code> of the histogram by 1: <b>b</b>[<code>value</code>]<code>++</code>.
     * It can be interpreted as adding one element <code>value</code> to the source array <b>A</b>,
     * on the base of which this histogram is built.
     *
     * <p>If this histogram is 32-bit, that is created by {@link #newIntHistogram(int, int...)} or
     * {@link #newIntHistogram(int[], int...)} method, this method throws <code>IllegalStateException</code>
     * if the current sum of all bars {@link #total()} is <code>Integer.MAX_VALUE</code> (2<sup>31</sup>&minus;1).
     * In other cases, this method throws <code>IllegalStateException</code>
     * if the current sum of all bars is <code>Long.MAX_VALUE</code> (2<sup>63</sup>&minus;1).
     * In a case of throwing an exception, this method does not change the histogram.
     *
     * <p>This method does not change the current value, but can change the current simple and precise ranks.
     * If there are some {@link #nextSharing() sharing instances}, this method change ranks in all sharing instances.
     *
     * @param value the index of the increased histogram bar.
     * @throws IndexOutOfBoundsException if <code>value&lt;0</code> or
     *                                   <code>value&gt;=</code><i>M</i>={@link #length()}.
     * @throws IllegalStateException     if <code>{@link #total()}==Long.MAX_VALUE</code>
     *                                   (or <code>Integer.MAX_VALUE</code>
     *                                   for 32-bit histogram).
     * @see #exclude(int)
     * @see #include(int...)
     * @see #exclude(int...)
     */
    public abstract void include(int value);

    /**
     * Decrements the bar <code>#value</code> of the histogram by 1: <b>b</b>[<code>value</code>]<code>--</code>.
     * It can be interpreted as removing one element, equal to <code>value</code>, from the source array <b>A</b>,
     * on the base of which this histogram is built.
     *
     * <p>If the bar <code>#value</code> of the histogram is zero (<b>b</b>[<code>value</code>]=0), this method
     * throws <code>IllegalStateException</code> and does not change the histogram.
     * So, the bars of the histogram cannot become negative.
     *
     * <p>This method does not change the current value, but can change the current simple and precise ranks.
     * If there are some {@link #nextSharing() sharing instances}, this method change ranks in all sharing instances.
     *
     * @param value the index of the increased histogram bar.
     * @throws IndexOutOfBoundsException if <code>value&lt;0</code> or
     *                                   <code>value&gt;=</code><i>M</i>={@link #length()}.
     * @throws IllegalStateException     if <b>b</b>[<code>value</code>]=0.
     * @see #include(int)
     * @see #include(int...)
     * @see #exclude(int...)
     */
    public abstract void exclude(int value);

    /**
     * Equivalent to a simple loop of calls of {@link #include(int)} method for all passed values.
     *
     * <p>The only possible difference from this loop is that the exception, if it occur, can be thrown
     * at the very beginning (but can also be thrown only while increasing the corresponding bar).
     * As well as the simple loop of {@link #include(int)}, this method
     * <b>can be non-atomic regarding this failure</b>: it can increase some histogram bars
     * and then throw an exception.
     *
     * @param values the indexes of the increased histogram bars. If some index is repeated several times
     *               in this array, the corresponding histogram bar will be increased several times.
     * @throws NullPointerException      if <code>values</code> array is {@code null}.
     * @throws IndexOutOfBoundsException if some <code>values[k]&lt;0</code> or
     *                                   <code>values[k]&gt;=</code><i>M</i>={@link #length()}.
     * @throws IllegalStateException     if <code>{@link #total()}&gt;Long.MAX_VALUE-values.length</code>
     *                                   (or <code>Integer.MAX_VALUE-values.length</code> for 32-bit histogram).
     * @see #include(int)
     * @see #exclude(int)
     * @see #exclude(int...)
     */
    public abstract void include(int... values);

    /**
     * Equivalent to a simple loop of calls of {@link #exclude(int)} method for all passed values.
     *
     * <p>The only possible difference from this loop is that the exception, if it occur, can be thrown
     * at the very beginning (but can also be thrown only while decreasing the corresponding bar).
     * As well as the simple loop of {@link #exclude(int)}, this method
     * <b>can be non-atomic regarding this failure</b>: it can decrease some histogram bars
     * and then throw an exception.
     *
     * @param values the indexes of the decreased histogram bars. If some index is repeated several times
     *               in this array, the corresponding histogram bar will be decreased several times.
     * @throws NullPointerException      if <code>values</code> array is {@code null}.
     * @throws IndexOutOfBoundsException if some <code>values[k]&lt;0</code> or
     *                                   <code>values[k]&gt;=</code><i>M</i>={@link #length()}.
     * @throws IllegalStateException     if <b>b</b>[<code>values[k]</code>]=0 for some <code>k</code>.
     * @see #include(int)
     * @see #exclude(int)
     * @see #include(int...)
     */
    public abstract void exclude(int... values);

    /**
     * Returns the rank <i>r</i>(<i>w</i>) of the value <i>w</i>={@link #currentIValue()}.
     * (This rank is the same in both histogram models.)
     * In other words, it is the sum of all histogram bars <b>b</b>[<i>k</i>] from the left
     * of the bar #<i>w</i>:
     * <span style="font-size:200%">&sum;</span><sub><i>k</i>&lt;<i>w</i></sub> <b>b</b>[<i>k</i>].
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * @return the rank of the integer part of the current value.
     * @see #currentRank()
     * @see #currentPreciseRank()
     */
    public abstract long currentIRank();

    /**
     * Returns the current simple rank: <i>r<sup>S</sup></i>=<i>r</i>(<i>v</i>)
     * in terms of the simple histogram model.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * @return the current simple rank.
     * @see #currentPreciseRank()
     * @see #currentIRank()
     */
    public final double currentRank() {
        final long total = total();
        if (DEBUG_MODE) {
            assert currentValue >= currentIValue - 0.5001 : "currentValue = " + currentValue
                    + " < currentIValue - 0.5001, currentIValue = " + currentIValue;
            assert currentValue < currentIValue + 1.0001 :
                    "currentValue = " + currentValue + " >= currentIValue + 1.0 = " + (currentIValue + 1.0);
        }
        if (total == 0) {
            return 0.0;
        }
        final boolean shifted = currentValue < currentIValue; // possible after moveToPreciseRank
        final int v = shifted ? currentIValue - 1 : currentIValue;
        final long b = bar(v);
        final long r = shifted ? currentIRank() - b : currentIRank();
        if (b == 0) {
            return r;
        }
        final double delta = currentValue - v;
        return r + delta * b;
    }

    /**
     * Returns the current precise rank: <i>r<sup>P</sup></i>=<i>r</i>(<i>v</i>)
     * in terms of the precise histogram model.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * @return the current precise rank.
     * @see #currentRank()
     * @see #currentIRank()
     */
    public final double currentPreciseRank() {
        if (!Double.isNaN(currentPreciseRank)) {
            return currentPreciseRank;
        }
        final long r = currentIRank();
        final long total = total();
        if (r == total) {
            return currentPreciseRank = r;
        }
        final long b = bar(currentIValue);
        if (DEBUG_MODE) {
            if (b < 0)
                throw new AssertionError("Negative histogram bar #" + currentIValue);
            if (r + b > total)
                throw new AssertionError("Current rank " + r + " + bar #"
                        + currentIValue + " = " + b + " > total number of elements " + total);
        }
        if (r + b == 0) {
            return currentPreciseRank = 0.0;
        }
        final double delta = currentValue - currentIValue;
        // Here Double.isNaN(currentPreciseRank), so, currentValue can be != currentIValue only as a result
        // of moveToValue or moveToRank, after which currentIValue<=currentValue<=currentIValue+1
        if (DEBUG_MODE) {
            assert delta >= 0.0 : "currentValue = " + currentValue
                    + " < currentIValue = " + currentIValue + ", though currentPreciseRank is unknown";
            assert delta < 1.0001 :
                    "currentValue = " + currentValue + " >= currentIValue + 1.0 = " + (currentIValue + 1.0);
            checkIntegrity();
        }
        if (b > 0 && delta == 0.0) {
            return currentPreciseRank = r;
        }
        final int savedIValue = currentIValue;
        final double savedValue = currentValue;
        final double v1;
        if (b > 1) {
            double indexInBar = delta * b;
            if (indexInBar <= b - 1) { // delta in range 0..(b-1)/b: simple case
                return currentPreciseRank = r + indexInBar;
            }
            if (r + b == total) { // the rightmost range (b-1)/b..1.0: special case
                return currentPreciseRank = r + indexInBar;
            }
            v1 = currentIValue + (double) (b - 1) / (double) b;
            saveRanks();
        } else {
            if (r + b == total) {
                assert b == 1; // else we should already return due to the check "r==total"
                return currentPreciseRank = r + delta;
            }
            saveRanks();
            if (b == 1) {
                v1 = currentIValue;
            } else {
                assert r > 0; // else we should already return due to the check "r+b==0"
                assert b == 0;
                moveToIRank(r - 1);
                v1 = currentValue; // right boundary of the corresponding bar
            }
        }
        moveToIRank(r + b);
        assert currentValue == currentIValue :
                "bug: we are not at the left boundary of the bar #" + currentIValue + ", we at " + currentValue;
        final double v2 = currentValue;
        restoreRanks();
        currentIValue = savedIValue;
        currentValue = savedValue;
        assert v1 < v2 : "bug: illegal " + v1 + ".." + v2 + " range";
        assert v1 <= currentValue && currentValue <= v2 :
                "bug: currentValue = " + currentValue + " is not in " + v1 + ".." + v2 + " range";
        currentPreciseRank = r + b - 1 + (currentValue - v1) / (v2 - v1);
        if (DEBUG_MODE) {
            checkIntegrity();
        }
        return currentPreciseRank;
    }

    /**
     * Returns the {@link #currentValue() current value} <i>v</i>, rounded to an integer number.
     *
     * <p>The rules of rounding depend on the order of previous calls of 5 methods, changing
     * the current value and ranks: {@link #moveToValue(double)}, {@link #moveToRank(double)},
     * {@link #moveToPreciseRank(double)}, {@link #moveToIValue(int)}, {@link #moveToIRank(long)}.
     * (Calls of any other methods, in particular, {@link #include(int) include} and {@link #exclude(int) exclude},
     * do not affect to the result of this method.)
     *
     * <p>If the last from those methods was
     * {@link #moveToValue(double) moveToValue},
     * {@link #moveToRank(double) moveToRank},
     * {@link #moveToIValue(int) moveToIValue} or
     * {@link #moveToIRank(long) moveToIRank},
     * then the result of this method is just the integer part of the current value: &lfloor;<i>v</i>&rfloor;.
     * But after {@link #moveToPreciseRank(double) moveToPreciseRank} method the returned value will be equal to
     * {@link #iPreciseValue(long[], double) iPreciseValue(histogram, rank)}, where <code>histogram</code>
     * is this histogram (the result of {@link #bars()} method) and <code>rank</code> is the argument of
     * {@link #moveToPreciseRank(double)}.
     *
     * <p>The special case
     * <i>N</i>={@link #total()}=0 is an exception to the last rule:
     * in this case, all 3 methods {@link #moveToPreciseRank(double) moveToPreciseRank},
     * {@link #moveToRank(double) moveToRank} and {@link #moveToIRank(long) moveToIRank} are equivalent,
     * but the concrete results of {@link #currentValue()} method (i.e. <i>v</i>)
     * and this method <i>are not documented</i> &mdash; unlike the result of
     * {@link #iPreciseValue iPreciseValue} static function, which returns 0 in this case.
     * However, in this case, as after any call of {@link #moveToRank(double) moveToRank} /
     * {@link #moveToIRank(long) moveToIRank}, there is a guarantee that the result of this method is equal to
     * the integer part of the current value &lfloor;<i>v</i>&rfloor;.
     *
     * <p>Immediately after creating a new histogram this method always returns 0 (like {@link #currentValue()}).
     *
     * <p>This result of this method always lies between <code>(int)</code><i>v</i>=&lfloor;<i>v</i>&rfloor;
     * and <code>Math.round</code>(<i>v</i>).
     *
     * @return the current value (percentile), rounded to an integer number.
     * @see #currentValue()
     */
    public final int currentIValue() {
        if (DEBUG_MODE) {
            checkIntegrity();
        }
        return currentIValue;
    }

    /**
     * Returns the current value <i>v</i>.
     * It is equal to the percentile of this histogram, found either in terms of the simple histogram model,
     * if before this we called {@link #moveToRank(double)} method, or in terms of the precise histogram model,
     * if before this we called {@link #moveToPreciseRank(double)} method.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * @return the current value (percentile).
     * @see #currentIValue()
     */
    public final double currentValue() {
        if (DEBUG_MODE) {
            checkIntegrity();
        }
        return currentValue;
    }

    /**
     * Returns <code>true</code> if and only if the current bar is zero: <b>b</b>[&lfloor;<i>v</i>&rfloor;]=0
     * (<i>v</i> is the {@link #currentValue() current value})
     * and either all bars from the left, or all bars from the right are also zero.
     * Equivalent to <code>{@link #leftFromNonZeroPart()} || {@link #rightFromNonZeroPart()}</code>.
     *
     * @return <code>true</code> if the current bar (containing the current value)
     * and all bars rightward or leftward from it are zero.
     */
    public final boolean outsideNonZeroPart() {
        long r = currentIRank();
        return r == total() || (r == 0 && bar(currentIValue) == 0);
    }

    /**
     * Returns <code>true</code> if and only if the current bar is zero: <b>b</b>[&lfloor;<i>v</i>&rfloor;]=0
     * (<i>v</i> is the {@link #currentValue() current value})
     * and all bars from the left are also zero: <b>b</b>[<i>k</i>]=0 for all <i>k</i>&lt;&lfloor;<i>v</i>&rfloor;.
     *
     * @return <code>true</code> if the current bar (containing the current value)
     * and all bars leftward from it are zero.
     */
    public final boolean leftFromNonZeroPart() {
        return currentIRank() == 0 && bar(currentIValue) == 0;
    }

    /**
     * Returns <code>true</code> if and only all bars from the left of the current bar are zero:
     * <b>b</b>[<i>k</i>]=0 for all <i>k</i>&lt;&lfloor;<i>v</i>&rfloor;
     * (<i>v</i> is the {@link #currentValue() current value}).
     *
     * @return <code>true</code> if all bars leftward from the current value are zero.
     */
    public final boolean leftFromOrAtBoundOfNonZeroPart() {
        return currentIRank() == 0;
    }

    /**
     * Returns <code>true</code> if and only if the current bar is zero: <b>b</b>[&lfloor;<i>v</i>&rfloor;]=0
     * (<i>v</i> is the {@link #currentValue() current value})
     * and all bars from the right are also zero: <b>b</b>[<i>k</i>]=0 for all <i>k</i>&gt;&lfloor;<i>v</i>&rfloor;.
     *
     * @return <code>true</code> if the current bar (containing the current value)
     * and all bars rightward from it are zero.
     */
    public final boolean rightFromNonZeroPart() {
        return currentIRank() == total();
    }

    /**
     * Returns <code>true</code> if and only all bars from the right of the current bar are zero:
     * <b>b</b>[<i>k</i>]=0 for all <i>k</i>&gt;&lfloor;<i>v</i>&rfloor;
     * (<i>v</i> is the {@link #currentValue() current value}).
     *
     * @return <code>true</code> if all bars rightward from the current value are zero.
     */
    public final boolean rightFromOrAtBoundOfNonZeroPart() {
        return currentIRank() + bar(currentIValue) == total();
    }

    /**
     * Sets the current simple rank <i>r<sup>S</sup></i> and
     * precise rank <i>r<sup>P</sup></i>
     * to be equal of the <code>rank</code> argument.
     * (Because the argument is integer, both <i>r<sup>S</sup></i>
     * and <i>r<sup>P</sup></i> ranks are the same.)
     * If the <code>rank</code> argument is negative, it is replaced with 0 (minimal possible rank);
     * if <code>rank</code>&gt;<i>N</i>={@link #total()},
     * it is replaced with <i>N</i> (maximal possible rank).
     * The {@link #currentValue() current value} <i>v</i> automatically changes
     * in accordance to the new rank.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * <p>In the special case <i>N</i>=0 (all bars of the histograms are zero),
     * both simple and precise ranks are always zero, not depending on calls of this method:
     * <i>r<sup>S</sup></i>=<i>r<sup>P</sup></i>=0
     * (because <i>r</i>(<i>v</i>) is a zero constant by definition).
     * But <i>v</i>(<i>r</i>) function
     * (unlike <i>r</i>(<i>v</i>)) is not defined in this case, so, if <i>N</i>=0,
     * the current value <i>v</i> after calling this method is not documented &mdash;
     * there is the only guarantee that 0&le;<i>v</i>&le;<i>M</i>.
     *
     * <p>This method works little faster than equivalent calls
     * {@link #moveToRank(double) moveToRank(rank)} and
     * {@link #moveToPreciseRank(double) moveToPreciseRank(rank)}.
     *
     * @param rank new rank <i>r<sup>S</sup></i>=<i>r<sup>P</sup></i>.
     * @return the reference to this object.
     * @see #moveToRank(double)
     * @see #moveToPreciseRank(double)
     */
    public abstract Histogram moveToIRank(long rank);

    /**
     * Sets the current simple rank <i>r<sup>S</sup></i>
     * to be equal of the <code>rank</code> argument.
     * If the <code>rank</code> argument is negative, it is replaced with 0 (minimal possible rank);
     * if <code>rank</code>&gt;<i>N</i>={@link #total()},
     * it is replaced with <i>N</i> (maximal possible rank).
     * The {@link #currentPreciseRank() current precise rank} <i>r<sup>P</sup></i>
     * and the {@link #currentValue() current value} <i>v</i>
     * automatically change in accordance to the new simple rank.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * <p>In the special case <i>N</i>=0 (all bars of the histograms are zero),
     * both simple and precise ranks are always zero, not depending on calls of this method:
     * <i>r<sup>S</sup></i>=<i>r<sup>P</sup></i>=0
     * (because <i>r</i>(<i>v</i>) is a zero constant by definition).
     * But <i>v</i>(<i>r</i>) function
     * (unlike <i>r</i>(<i>v</i>)) is not defined in this case, so, if <i>N</i>=0,
     * the current value <i>v</i> after calling this method is not documented &mdash;
     * there is the only guarantee that 0&le;<i>v</i>&le;<i>M</i>.
     *
     * @param rank new simple rank <i>r<sup>S</sup></i>.
     * @return the reference to this object.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #moveToPreciseRank(double)
     * @see #moveToIRank(long)
     */
    public abstract Histogram moveToRank(double rank);

    /**
     * Sets the current precise rank <i>r<sup>P</sup></i>
     * to be equal of the <code>rank</code> argument.
     * If the <code>rank</code> argument is negative, it is replaced with 0 (minimal possible rank);
     * if <code>rank</code>&gt;<i>N</i>={@link #total()},
     * it is replaced with <i>N</i> (maximal possible rank).
     * The {@link #currentRank() current simple rank} <i>r<sup>S</sup></i>
     * and the {@link #currentValue() current value} <i>v</i> automatically change
     * in accordance to the new precise rank.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * <p>In the special case <i>N</i>=0 (all bars of the histograms are zero),
     * both simple and precise ranks are always zero, not depending on calls of this method:
     * <i>r<sup>S</sup></i>=<i>r<sup>P</sup></i>=0
     * (because <i>r</i>(<i>v</i>) is a zero constant by definition).
     * But <i>v</i>(<i>r</i>) function
     * (unlike <i>r</i>(<i>v</i>)) is not defined in this case, so, if <i>N</i>=0,
     * the current value <i>v</i> after calling this method is not documented &mdash;
     * there is the only guarantee that 0&le;<i>v</i>&le;<i>M</i>.
     *
     * @param rank new precise rank <i>r<sup>P</sup></i>.
     * @return the reference to this object.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #moveToRank(double)
     * @see #moveToIRank(long)
     */
    public Histogram moveToPreciseRank(double rank) {
        final long total = total();
        final long r = (long) rank;
        if (r == rank || total == 0 || r < 0 || r >= total) {
            moveToIRank(r);
        } else {
            if (Double.isNaN(rank))
                throw new IllegalArgumentException("Illegal rank argument (NaN)");
            moveToIRank(r);
            final int leftIValue = currentIValue;
            final long leftIRank = currentIRank();
            final long indexInBar = r - leftIRank;
            final long leftBar = bar(currentIValue);
            assert indexInBar >= 0 && indexInBar <= leftBar;
            if (indexInBar < leftBar - 1 // we are inside a single bar
                    || r == total - 1) // the rightmost range (b-1)/b..1.0: special case
            {
                currentValue = currentIValue + (rank - (double) leftIRank) / (double) leftBar;
                // not use "+=" here to provide precise identity with formula in preciseValue static method
            } else {
                final double leftValue = currentValue;
                saveRanks();
                moveToIRank(r + 1);
                final int rightIValue = currentIValue;
                final long rightBar = bar(currentIValue);
                final double rightValue = currentValue;
                assert rightValue == rightIValue;
                final double newValue = leftValue + (rank - r) * (rightValue - leftValue);
                assert leftIValue < rightIValue;
                // Below we suppose that the true left precise value lies in range
                // leftValue..left'=leftValue+1/leftBar
                // and the true right precise value lies in range
                // rightValue..right'=rightValue+1/rightBar,
                // so a = newValue is left boundary of the probable range for new precise value,
                // and b = left' + (rank-r) * (right' - left') is the right boundary.
                // We try to find newValue..newValue+1 will be the range which contains new precise value
                // with maximal probability. In other words, currentIValue..currentIValue+1 range must
                // "cover" the range a..b in the best way.
                // The necessary currentIValue..currentIValue+1 range, which "covers" the range a..b in the best way,
                // is currentIValue=(int)((a+b)/2)=(int)(a+(b-a)/2) for any a..b. Here
                //     b-a = 1/leftBar + (rank-r) * (1/rightBar - 1/leftBar)
                final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
                final double weightedMeanStripe = leftBar == rightBar ?
                        leftStripe :
                        leftStripe + (rank - r) * (1.0 / rightBar - leftStripe);
                assert weightedMeanStripe >= -0.001;
                final double rangeCenter = newValue + 0.5 * Math.max(weightedMeanStripe, 1e-10); // (a+b)/2
                // to be on the safe side, we guarantee that newValue + 1 > newValue
                // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
                final int newIValue = (int) rangeCenter;
                assert newIValue >= leftIValue && newIValue <= rightIValue :
                        "bug: " + newIValue + " is not in [" + leftIValue + ".." + rightIValue + "] range";
                if (newIValue == leftIValue) {
                    restoreRanks();
                    currentIValue = leftIValue;
                } else if (newIValue < rightIValue) {
                    assert bar(newIValue) == 0 : "bug: non-empty bar at corrected currentIValue";
                    moveToIValue(newIValue); // cannot just assign currentIValue: can be incorrect for multilevel case
                } else {
                    assert currentIValue == rightIValue;
                }
                currentValue = newValue;
                assert currentValue >= currentIValue - 0.5001;
                assert currentValue < currentIValue + 1.0; // "+1.0" instead of "+1" to avoid integer overflow
            }
            currentPreciseRank = rank;
        }
        if (DEBUG_MODE) {
            checkIntegrity();
        }
        return this;
    }

    /**
     * Sets the current value <i>v</i>
     * to be equal of the <code>value</code> argument.
     * If the <code>value</code> argument is negative, it is replaced with 0 (minimal possible value);
     * if <code>value</code>&gt;<i>M</i>={@link #length()},
     * it is replaced with <i>M</i> (maximal possible value).
     * The {@link #currentRank() current simple rank} <i>r<sup>S</sup></i>
     * and the {@link #currentPreciseRank() current precise rank} <i>r<sup>P</sup></i>
     * automatically change in accordance to the new value.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * <p>This methods works little faster than the equivalent call
     * {@link #moveToValue(double) moveToValue(value)}.
     *
     * @param value new current value (percentile).
     * @return the reference to this object.
     * @see #moveToValue(double)
     */
    public abstract Histogram moveToIValue(int value);

    /**
     * Sets the current value <i>v</i>
     * to be equal of the <code>value</code> argument.
     * If the <code>value</code> argument is negative, it is replaced with 0 (minimal possible value);
     * if <code>value</code>&gt;<i>M</i>={@link #length()},
     * it is replaced with <i>M</i> (maximal possible value).
     * The {@link #currentRank() current simple rank} <i>r<sup>S</sup></i>
     * and the {@link #currentPreciseRank() current precise rank} <i>r<sup>P</sup></i>
     * automatically change in accordance to the new value.
     * See {@link Histogram comments to Histogram class} for more details.
     *
     * @param value new current value (percentile).
     * @return the reference to this object.
     * @throws IllegalArgumentException if <code>Double.isNaN(value)</code>.
     * @see #moveToIValue(int)
     */
    public Histogram moveToValue(double value) {
        if (Double.isNaN(value))
            throw new IllegalArgumentException("Illegal value argument (NaN)");
        int v = (int) value;
        if (v < 0) {
            value = v = 0;
        } else if (v > length) {
            value = v = length;
        }
        moveToIValue(v);
        currentValue = value;
        return this;
    }

    /**
     * Returns the number of instances of this class, sharing the histogram array <b>b</b>[<i>k</i>]
     * with this instance. In other words, it returns the length of the circular list returned by
     * {@link #nextSharing()} method. Returns 1 if {@link #share()} method was not used.
     *
     * <p>It is obvious that this method always returns the same value for all instances sharing the same
     * histogram array.
     *
     * @return the number of instances of this class, sharing the histogram array <b>b</b>[<i>k</i>] with this one.
     */
    public abstract long shareCount();

    /**
     * Returns the next instance of this class, sharing the histogram array <b>b</b>[<i>k</i>] with this instance.
     *
     * <p>All instances, created by {@link #share()} method, are connected into a circular list, and this method
     * returns the next element in this list. For example, if the instance <code>h1</code> was created by
     * {@link #newLongHistogram(int, int...)} method and, after this, the instance <code>h2</code> was created as
     * <code>h2=h1.{@link #share()}</code>, then this method in <code>h1</code>
     * object returns <code>h2</code>
     * and in <code>h2</code> object returns <code>h1</code>. If there are no sharing instances, this method returns
     * the reference to this instance.
     *
     * <p>You can get all instances, sharing the same array  <b>b</b>[<i>k</i>] with the given histogram
     * <code>hist</code>, by the following loop:
     *
     * <pre>
     * Histogram h = hist.nextSharing();
     * do {
     *     // some processing h instance
     *     h = h.nextSharing();
     * } while (h != hist);
     * </pre>
     *
     * <p>See {@link Histogram comments to Histogram class} for more details.
     *
     * @return the next instance sharing the histogram array <b>b</b>[<i>k</i>] with this instance,
     * or the reference to this instance if you did not use {@link #share()} method.
     * @see #shareCount()
     */
    public abstract Histogram nextSharing();

    /**
     * Creates new instance of this class, which uses the same arrays of bars <b>b</b>[<i>k</i>].
     * The returned instance will share the histogram <b>b</b>[<i>k</i>] with this instance:
     * any modification of <b>b</b> array by {@link #include include} / {@link #exclude exclude} methods
     * will be immediately reflected in all shared instances. However, the created sharing instance
     * has an independent set of {@link #currentValue() current value}, {@link #currentRank() current simple rank}
     * and {@link #currentPreciseRank() current precise rank}, and they are initially set to 0.
     *
     * <p>The returned instance has the absolutely same behavior as this one: it uses the same set of
     * <i>bit levels</i> (see comments to {@link #newLongHistogram(int, int...)} method),
     * it is {@link #newIntHistogram(int, int...) 32-bit} if and only if this instance is 32-bit,
     * it is a {@link SummingHistogram} if and only if this instance is a {@link SummingHistogram}, etc.
     *
     * <p>See {@link Histogram comments to Histogram class} for more details.
     *
     * @return newly created instance sharing the histogram array <b>b</b>[<i>k</i>] with this instance.
     * @see #nextSharing()
     * @see #shareCount()
     */
    public abstract Histogram share();

    abstract void saveRanks();

    abstract void restoreRanks();

    abstract void checkIntegrity();

    /**
     * Returns the element with the given index <code>rank</code> in the sorted array
     * of integer numbers <code>0..histogram.length-1</code>, corresponding to this histogram.
     *
     * <p>More precisely, returns minimal integer value <i>v</i> so that
     * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i>]&gt;<code>rank</code>,
     * <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>].
     * If <code>rank&le;0</code>, this method returns
     * <i>min</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)
     * (the minimal element in the source array).
     * If <code>rank&ge;</code>(sum of all <b>b</b>[<i>k</i>]), it returns
     * <i>max</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)+1
     * (the maximal element plus 1).
     * If all columns <b>b</b>[<i>k</i>] are zero (no elements), this method returns <code>0</code>.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in some source array
     *                  that are equal to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array.
     * @return value of the found element (percentile).
     * @throws NullPointerException if <code>histogram</code> argument is {@code null}.
     * @see #value(long[], double)
     * @see #preciseValue(long[], double)
     */
    public static int iValue(long[] histogram, long rank) {
        //[[Repeat.SectionStart iValueImpl]]
        if (rank < 0) {
            rank = 0;
        }
        long acc = 0;
        int lastNonZero = -1;
        for (int k = 0; k < histogram.length; k++) {
            long b = histogram[k];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + k + "]=" + b);
            if (b > 0) {
                lastNonZero = k;
                acc += b;
                if (rank < acc) {
                    return k;
                }
            }
        }
        return lastNonZero + 1; // = 0 if all columns are zero
        //[[Repeat.SectionEnd iValueImpl]]
    }

    /**
     * Precise equivalent of {@link #iValue(long[], long)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source array
     *                  that are equal to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array.
     * @return value of the found element (percentile).
     * @throws NullPointerException if <code>histogram</code> argument is {@code null}.
     * @see #value(int[], double)
     * @see #preciseValue(int[], double)
     */
    public static int iValue(int[] histogram, long rank) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, iValueImpl)
        //        \blong\b ==> int  !! Auto-generated: NOT EDIT !! ]]
        if (rank < 0) {
            rank = 0;
        }
        int acc = 0;
        int lastNonZero = -1;
        for (int k = 0; k < histogram.length; k++) {
            int b = histogram[k];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + k + "]=" + b);
            if (b > 0) {
                lastNonZero = k;
                acc += b;
                if (rank < acc) {
                    return k;
                }
            }
        }
        return lastNonZero + 1; // = 0 if all columns are zero
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Floating-point version of {@link #iValue(long[], long)}.
     * Alike {@link #preciseValue(long[], double)}, this function supposes that the histogram
     * is built on an array of <i>floating-point</i> values after truncating them to an integer value
     * <code>(long)value</code>,  but it doesn't try to interpolate value between different bars of the histogram.
     *
     * <p>More precisely, we suppose that if <b>b</b>[<i>k</i>]==<i>b</i>
     * (here and below <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>]),
     * it means that the source floating-point array contains <i>b</i> values
     * <i>k</i>+<i>j</i>/<i>b</i>, <i>j</i>=0,1,...,<i>b</i>&minus;1.
     * With this suggestion, this method finds the element of the source array <i>v</i><sub>1</sub>
     * with the index #<i>r</i><sub>1</sub>=&lfloor;<code>rank</code>&rfloor;=<code>(long)rank</code>.
     * Obviously, <i>v</i><sub>1</sub> =
     * <i>v</i><sub>0</sub>+(<i>r</i><sub>1</sub>-<i>r</i><sub>0</sub>)/<b>b</b>[<i>v</i><sub>0</sub>],
     * where <i>v</i><sub>0</sub> is the minimal integer value so that
     * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>]&gt;<i>r</i><sub>1</sub> and
     * <i>r</i><sub>0</sub>=<b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>&minus;1].
     * Then this method returns
     * <i>v</i><sub>1</sub>+(<code>rank</code>&minus;<i>r</i><sub>1</sub>)/<b>b</b>[<i>v</i><sub>0</sub>]
     * (this value is equal to
     * <i>v</i><sub>0</sub>+(<code>rank</code>&minus;<i>r</i><sub>0</sub>)/<b>b</b>[<i>v</i><sub>0</sub>]).
     * Please compare: unlike {@link #preciseValue(long[], double)}, we do not find the next element
     * <i>v</i><sub>2</sub>
     * in the following bars of the histogram, but just interpolate between <i>v</i><sub>1</sub>
     * and <i>v</i><sub>2</sub>=<i>v</i><sub>1</sub>+1/<b>b</b>[<i>v</i><sub>0</sub>].
     *
     * <p>As {@link #iValue(long[], long)}, if <code>rank&lt;0</code>, this method returns
     * <i>min</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)
     * (the minimal element in the source array), and
     * if <code>rank&ge;</code>(sum of all <b>b</b>[<i>k</i>]), it returns
     * <i>max</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)+1
     * (for floating-point array, it means the maximal element plus 1/<b>b</b>[<i>k</i>]).
     * If all columns <b>b</b>[<i>k</i>] are zero (no elements), this method returns <code>0</code>.
     *
     * <p>The result of this method is equal to the percentile <i>v</i>(<i>r</i>) for the passed
     * <i>r</i>=<code>rank</code> in terms of the <i>simple histogram model</i>:
     * see {@link Histogram comments to Histogram class} for more details.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source floating-point array
     *                  that are "almost equal" to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array (if non-integer, this method returns a real value,
     *                  which is little greater than the element #<i>r</i><sub>1</sub>=(long)rank,
     *                  but is less than the next element #<i>r</i><sub>1</sub>+1 and has the same integer part).
     * @return the found value (percentile).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #preciseValue(long[], double)
     */
    public static double value(long[] histogram, double rank) {
        //[[Repeat.SectionStart valueImpl]]
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal rank argument (NaN)");
        if (rank < 0.0) {
            rank = 0.0;
        }
        final long r = (long) rank;
        long acc = 0;
        int lastNonZero = -1;
        for (int k = 0; k < histogram.length; k++) {
            long b = histogram[k];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + k + "]=" + b);
            if (b > 0) {
                lastNonZero = k;
                if (r < acc + b) {
                    return k + (b == 1 ? rank - acc : (rank - acc) / (double) b);
                }
                acc += b;
            }
        }
        return lastNonZero + 1; // = 0 if all columns are zero
        //[[Repeat.SectionEnd valueImpl]]
    }

    /**
     * Precise equivalent of {@link #value(long[], double)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source floating-point array
     *                  that are "almost equal" to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array (if non-integer, this method returns a real value,
     *                  which is little greater than the element #<i>r</i><sub>1</sub>=(long)rank,
     *                  but is less than the next element #<i>r</i><sub>1</sub>+1 and has the same integer part).
     * @return the found value (percentile).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #preciseValue(int[], double)
     */
    public static double value(int[] histogram, double rank) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, valueImpl)
        //        \blong\b ==> int  !! Auto-generated: NOT EDIT !! ]]
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal rank argument (NaN)");
        if (rank < 0.0) {
            rank = 0.0;
        }
        final int r = (int) rank;
        int acc = 0;
        int lastNonZero = -1;
        for (int k = 0; k < histogram.length; k++) {
            int b = histogram[k];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + k + "]=" + b);
            if (b > 0) {
                lastNonZero = k;
                if (r < acc + b) {
                    return k + (b == 1 ? rank - acc : (rank - acc) / (double) b);
                }
                acc += b;
            }
        }
        return lastNonZero + 1; // = 0 if all columns are zero
        //[[Repeat.IncludeEnd]]
    }

    /**
     * "Interpolated" version of {@link #iValue(long[], long)}, rounded to the "best" integer result.
     * Alike {@link #preciseValue(long[], double)}, this function supposes that the histogram
     * is built on an array of <i>floating-point</i> values after truncating them to an integer value
     * <code>(long)value</code>. In addition to {@link #preciseValue(long[], double) preciseValue},
     * this function tries to approximate the real result by some nearest integer value.
     *
     * <p>More precisely, we suppose that if <b>b</b>[<i>k</i>]==<i>b</i>
     * (here and below <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>]),
     * it means that the source floating-point array contains <i>b</i> values
     * <i>k</i>+<i>j</i>/<i>b</i>, <i>j</i>=0,1,...,<i>b</i>&minus;1.
     * With this suggestion, this method finds the element of the source array <i>v</i><sub>1</sub>
     * with the index #<i>r</i><sub>1</sub>=&lfloor;<code>rank</code>&rfloor;=<code>(long)rank</code>
     * and the element of the source array <i>v</i><sub>2</sub>
     * with the index #<i>r</i><sub>2</sub>=<i>r</i><sub>1</sub>+1.
     * Here <i>v</i><sub>1</sub> =
     * <i>v</i><sub>0</sub>+(<i>r</i><sub>1</sub>-<i>r</i><sub>0</sub>)/<b>b</b>[<i>v</i><sub>0</sub>],
     * where <i>v</i><sub>0</sub> is the minimal integer value so that
     * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>]&gt;<i>r</i><sub>1</sub> and
     * <i>r</i><sub>0</sub>=<b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>&minus;1],
     * and there is the analogous formula for <i>v</i><sub>2</sub>.
     * If <code>rank</code> argument is integer (<code>rank==(long)rank</code>),
     * this method does not try to find <i>v</i><sub>2</sub> and just returns <i>v</i><sub>1</sub>.
     * Until this moment, this method works like {@link #preciseValue(long[], double) preciseValue}.
     *
     * <p>After this, the behavior of this method is more complicated. If <code>rank</code> is not integer,
     * we calculate <i>v</i><sub>1</sub>'=<i>v</i><sub>1</sub>+1/<b>b</b>[<i>v</i><sub>1</sub>]
     * and <i>v</i><sub>2</sub>'=<i>v</i><sub>2</sub>+1/<b>b</b>[<i>v</i><sub>2</sub>].
     * Let's consider that the true real values in the source array
     * #<i>r</i><sub>1</sub> (<i>w</i><sub>1</sub>) and
     * #<i>r</i><sub>2</sub>=<i>r</i><sub>1</sub>+1 (<i>w</i><sub>2</sub>) are unknown, but lie in ranges
     * <i>v</i><sub>1</sub>&le;<i>w</i><sub>1</sub>&lt;<i>v</i><sub>1</sub>' and
     * <i>v</i><sub>2</sub>&le;<i>w</i><sub>2</sub>&lt;<i>v</i><sub>2</sub>'.
     * (We really don't know the precise real values, we only know that some
     * <b>b</b>[&lfloor;<i>v</i><sub>1</sub>&rfloor;] values
     * lie in &lfloor;<i>v</i><sub>1</sub>&rfloor;..&lfloor;<i>v</i><sub>1</sub>&rfloor;+1 range and some
     * <b>b</b>[&lfloor;<i>v</i><sub>2</sub>&rfloor;] values
     * lie in &lfloor;<i>v</i><sub>2</sub>&rfloor;..&lfloor;<i>v</i><sub>2</sub>&rfloor;+1 range.)
     * Then the value with real "index" <code>rank</code>,
     * interpolated between <i>w</i><sub>1</sub> and <i>w</i><sub>2</sub>, lies in range
     * <i>a</i>&le;<i>w</i>&lt;<i>b</i>, where
     * <i>a</i>=<i>v</i><sub>1</sub> +
     * (<code>rank</code>&minus;<i>r</i><sub>1</sub>) * (<i>v</i><sub>2</sub>&minus;<i>v</i><sub>1</sub>)
     * (the result of {@link #preciseValue(long[], double) preciseValue} call with the same arguments)
     * and <i>b</i>=<i>v</i><sub>1</sub>' +
     * (<code>rank</code>&minus;<i>r</i><sub>1</sub>) * (<i>v</i><sub>2</sub>'&minus;<i>v</i><sub>1</sub>').
     *
     * <p>This method finds the integer range <i>v</i>..<i>v</i>+1, which "covers"
     * the range <i>a</i>..<i>b</i> in the best way. Namely, it calculates
     * <i>v</i>=&lfloor;(<i>a</i>+<i>b</i>)/2&rfloor; and returns <code>v</code>
     * as the result.
     *
     * <p>The result of this method always lies between <code>(int)p</code> and <code>Math.round(p)</code>,
     * where <code>p={@link #preciseValue(long[], double) preciseValue}(histogram,rank)</code>.
     *
     * <p>As {@link #iValue(long[], long)}, if <code>rank&lt;0</code>, this method returns
     * <i>min</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)
     * (the minimal element in the source array), and
     * if <code>rank&ge;</code>(sum of all <b>b</b>[<i>k</i>]), it returns
     * <i>max</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)+1
     * (for floating-point array, it means the maximal element plus 1/<b>b</b>[<i>k</i>]).
     * If <code>rank&gt;</code>(sum of all <b>b</b>[<i>k</i>])&minus;1, but
     * <code>rank&lt;</code>(sum of all <b>b</b>[<i>k</i>]), then in formulas above there is no element
     * <i>v</i><sub>2</sub> with the index #<i>r</i><sub>2</sub>=<i>r</i><sub>1</sub>+1;
     * in this case, this method returns
     * <i>max</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0).
     * If all columns <b>b</b>[<i>k</i>] are zero (no elements), this method returns <code>0</code>.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source floating-point array
     *                  that are "almost equal" to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array (if non-integer, this method interpolates nearest elements).
     * @return interpolated value of the found element (percentile), rounded to the "best" integer value.
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #iValue(long[], long)
     * @see #preciseValue(long[], double)
     */
    public static int iPreciseValue(long[] histogram, double rank) {
        //[[Repeat.SectionStart iPreciseValue]]
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal rank argument (NaN)");
        if (rank < 0.0) {
            rank = 0.0;
        }
        final long r = (long) rank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "rank" to "r", "leftValue" to "leftV", etc.
        assert r >= 0;
        int leftV = 0;
        long leftBar = 0;
        long acc = 0;
        int lastNonZero = -1;
        for (; leftV < histogram.length; leftV++) {
            leftBar = histogram[leftV];
            if (leftBar < 0)
                throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + leftBar);
            if (leftBar > 0) {
                lastNonZero = leftV;
                acc += leftBar;
                if (r < acc) {
                    break;
                }
            }
        }
        if (leftV >= histogram.length) {
            return lastNonZero + 1; // = 0 if all columns are zero
        }
        assert leftV == lastNonZero;
        assert leftBar > 0;
        assert r < acc;
        final long leftR = acc - leftBar;
        assert leftR <= r; // in other words, acc - leftBar <= r < acc
        final long indexInBar = r - leftR;
        assert indexInBar < leftBar; // because leftBar = acc - leftR and r < acc
        if (rank == r) {
            return leftV;
        }
        if (indexInBar < leftBar - 1) {
            return leftV;
        }
        assert r + 1 == acc; // r < acc, but r + 1 >= acc, because indexInBar + 1 >= acc - leftR = leftBar
        final double leftValue = leftBar == 1 ? leftV : leftV + (double) indexInBar / (double) leftBar;
        int rightV = leftV + 1;
        long rightBar = 0;
        for (; rightV < histogram.length; rightV++) {
            rightBar = histogram[rightV];
            if (rightBar < 0)
                throw new IllegalArgumentException("Negative histogram[" + rightV + "]=" + rightBar);
            if (rightBar > 0) { // new acc will be > r+1
                break;
            }
        }
        if (rightV >= histogram.length) {
            return leftV; // all further elements are zero
        }
        final double newValue = leftValue + (rank - r) * (rightV - leftValue);
        final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
        final double weightedMeanStripe = leftBar == rightBar ?
                leftStripe :
                leftStripe + (rank - r) * (1.0 / rightBar - leftStripe);
        assert weightedMeanStripe >= -0.001;
        final double rangeCenter = newValue + 0.5 * Math.max(weightedMeanStripe, 1e-10); // (a+b)/2
        // to be on the safe side, we guarantee that newIValue + 1 > newValue
        // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
        final int result = (int) rangeCenter;
        assert result >= leftV && result <= rightV :
                "bug: " + result + " is not in [" + leftV + ".." + rightV + "] " + "range";
        return result;
        //[[Repeat.SectionEnd iPreciseValue]]
    }

    /**
     * Precise equivalent of {@link #iPreciseValue(long[], double)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source floating-point array
     *                  that are "almost equal" to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array (if non-integer, this method interpolates nearest elements).
     * @return interpolated value of the found element (percentile), rounded to the "best" integer value.
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #iValue(long[], long)
     */
    public static int iPreciseValue(int[] histogram, double rank) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, iPreciseValue)
        //        \blong\b ==> int  !! Auto-generated: NOT EDIT !! ]]
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal rank argument (NaN)");
        if (rank < 0.0) {
            rank = 0.0;
        }
        final int r = (int) rank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "rank" to "r", "leftValue" to "leftV", etc.
        assert r >= 0;
        int leftV = 0;
        int leftBar = 0;
        int acc = 0;
        int lastNonZero = -1;
        for (; leftV < histogram.length; leftV++) {
            leftBar = histogram[leftV];
            if (leftBar < 0)
                throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + leftBar);
            if (leftBar > 0) {
                lastNonZero = leftV;
                acc += leftBar;
                if (r < acc) {
                    break;
                }
            }
        }
        if (leftV >= histogram.length) {
            return lastNonZero + 1; // = 0 if all columns are zero
        }
        assert leftV == lastNonZero;
        assert leftBar > 0;
        assert r < acc;
        final int leftR = acc - leftBar;
        assert leftR <= r; // in other words, acc - leftBar <= r < acc
        final int indexInBar = r - leftR;
        assert indexInBar < leftBar; // because leftBar = acc - leftR and r < acc
        if (rank == r) {
            return leftV;
        }
        if (indexInBar < leftBar - 1) {
            return leftV;
        }
        assert r + 1 == acc; // r < acc, but r + 1 >= acc, because indexInBar + 1 >= acc - leftR = leftBar
        final double leftValue = leftBar == 1 ? leftV : leftV + (double) indexInBar / (double) leftBar;
        int rightV = leftV + 1;
        int rightBar = 0;
        for (; rightV < histogram.length; rightV++) {
            rightBar = histogram[rightV];
            if (rightBar < 0)
                throw new IllegalArgumentException("Negative histogram[" + rightV + "]=" + rightBar);
            if (rightBar > 0) { // new acc will be > r+1
                break;
            }
        }
        if (rightV >= histogram.length) {
            return leftV; // all further elements are zero
        }
        final double newValue = leftValue + (rank - r) * (rightV - leftValue);
        final double leftStripe = leftBar == 1 ? 1.0 : 1.0 / leftBar;
        final double weightedMeanStripe = leftBar == rightBar ?
                leftStripe :
                leftStripe + (rank - r) * (1.0 / rightBar - leftStripe);
        assert weightedMeanStripe >= -0.001;
        final double rangeCenter = newValue + 0.5 * Math.max(weightedMeanStripe, 1e-10); // (a+b)/2
        // to be on the safe side, we guarantee that newIValue + 1 > newValue
        // (any real stripe width is not less than 1/Integer.MAX_VALUE>1e-10)
        final int result = (int) rangeCenter;
        assert result >= leftV && result <= rightV :
                "bug: " + result + " is not in [" + leftV + ".." + rightV + "] " + "range";
        return result;
        //[[Repeat.IncludeEnd]]
    }

    /**
     * "Interpolated" version of {@link #iValue(long[], long)}.
     * This function supposes that the histogram is built on an array of
     * <i>floating-point</i> values after truncating them to an integer value <code>(long)value</code>.
     *
     * <p>More precisely, we suppose that if <b>b</b>[<i>k</i>]==<i>b</i>
     * (here and below <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>]),
     * it means that the source floating-point array contains <i>b</i> values
     * <i>k</i>+<i>j</i>/<i>b</i>, <i>j</i>=0,1,...,<i>b</i>&minus;1.
     * With this suggestion, this method finds the element of the source array <i>v</i><sub>1</sub>
     * with the index #<i>r</i><sub>1</sub>=&lfloor;<code>rank</code>&rfloor;=<code>(long)rank</code>
     * and the element of the source array <i>v</i><sub>2</sub>
     * with the index #<i>r</i><sub>2</sub>=<i>r</i><sub>1</sub>+1.
     * Obviously, <i>v</i><sub>1</sub> =
     * <i>v</i><sub>0</sub>+(<i>r</i><sub>1</sub>-<i>r</i><sub>0</sub>)/<b>b</b>[<i>v</i><sub>0</sub>],
     * where <i>v</i><sub>0</sub> is the minimal integer value so that
     * <b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>]&gt;<i>r</i><sub>1</sub> and
     * <i>r</i><sub>0</sub>=<b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>v</i><sub>0</sub>&minus;1],
     * and there is the analogous formula for <i>v</i><sub>2</sub>.
     * Note: <i>v</i><sub>2</sub>=<i>v</i><sub>1</sub>+1/<b>b</b>[<i>v</i><sub>0</sub>]
     * if <i>r</i><sub>2</sub>&lt;<i>r</i><sub>0</sub>+<b>b</b>[<i>v</i><sub>0</sub>] or
     * if <i>r</i><sub>2</sub>=<i>r</i><sub>0</sub>+<b>b</b>[<i>v</i><sub>0</sub>] and
     * the next bar is non-zero: <b>b</b>[<i>v</i><sub>0</sub>+1]&gt;0;
     * in another case, <i>v</i><sub>2</sub> is the minimal integer &gt;<i>v</i><sub>0</sub> so that
     * <b>b</b>[<i>v</i><sub>2</sub>]&gt;0.
     *
     * <p>After finding <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub>, this method returns the value
     * interpolated between them:  <i>v</i><sub>1</sub> +
     * (<code>rank</code>&minus;<i>r</i><sub>1</sub>) * (<i>v</i><sub>2</sub>&minus;<i>v</i><sub>1</sub>).
     * Note: if <code>rank</code> argument is integer (<code>rank==(long)rank</code>),
     * this method does not try to find <i>v</i><sub>2</sub> and just returns <i>v</i><sub>1</sub>.
     *
     * <p>As {@link #iValue(long[], long)}, if <code>rank&lt;0</code>, this method returns
     * <i>min</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)
     * (the minimal element in the source array), and
     * if <code>rank&ge;</code>(sum of all <b>b</b>[<i>k</i>]), it returns
     * <i>max</i> (<i>k</i>&isin;<b>Z</b>: <b>b</b>[<i>k</i>]&gt;0)+1
     * (for floating-point array, it means the maximal element plus 1/<b>b</b>[<i>k</i>]).
     * If <code>rank&gt;</code>(sum of all <b>b</b>[<i>k</i>])&minus;1, but
     * <code>rank&lt;</code>(sum of all <b>b</b>[<i>k</i>]), then in formulas above there is no element
     * <i>v</i><sub>2</sub> with the index #<i>r</i><sub>2</sub>=<i>r</i><sub>1</sub>+1;
     * in this case, it is supposed <i>v</i><sub>2</sub>=<i>v</i><sub>1</sub>+1
     * (the maximal element of the floating-point array plus 1/<b>b</b>[<i>k</i>],
     * <i>k</i>=<i>v</i><sub>1</sub>).
     * If all columns <b>b</b>[<i>k</i>] are zero (no elements), this method returns <code>0</code>.
     *
     * <p>Please compare the described behavior with little more simple behavior of
     * {@link #value(long[], double)} method.
     *
     * <p>The result of this method is equal to the percentile <i>v</i>(<i>r</i>) for the passed
     * <i>r</i>=<code>rank</code> in terms of the <i>precise histogram model</i>:
     * see {@link Histogram comments to Histogram class} for more details.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source array
     *                  that are "almost equal" to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array (if non-integer, this method interpolates nearest elements).
     * @return interpolated value of the found element (percentile).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #value(long[], double)
     * @see #iPreciseValue(long[], double)
     */
    public static double preciseValue(long[] histogram, double rank) {
        //[[Repeat.SectionStart preciseValueImpl]]
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal rank argument (NaN)");
        if (rank < 0.0) {
            rank = 0.0;
        }
        final long r = (long) rank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "rank" to "r", "leftValue" to "leftV", etc.
        assert r >= 0;
        int leftV = 0;
        long leftBar = 0;
        long acc = 0;
        int lastNonZero = -1;
        for (; leftV < histogram.length; leftV++) {
            leftBar = histogram[leftV];
            if (leftBar < 0)
                throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + leftBar);
            if (leftBar > 0) {
                lastNonZero = leftV;
                acc += leftBar;
                if (r < acc) {
                    break;
                }
            }
        }
        if (leftV >= histogram.length) {
            return lastNonZero + 1; // = 0 if all columns are zero
        }
        assert leftV == lastNonZero;
        assert leftBar > 0;
        assert r < acc;
        final long leftR = acc - leftBar;
        assert leftR <= r; // in other words, acc - leftBar <= r < acc
        final long indexInBar = r - leftR;
        assert indexInBar < leftBar; // because leftBar = acc - leftR and r < acc
        if (rank == r) {
            return leftBar == 1 ? leftV : leftV + (double) indexInBar / (double) leftBar;
        }
        if (indexInBar < leftBar - 1) {
            return leftV + (rank - (double) leftR) / (double) leftBar;
        }
        assert r + 1 == acc; // r < acc, but r + 1 >= acc, because indexInBar + 1 >= acc - leftR = leftBar
        int rightV = nextNonZero(histogram, leftV + 1);
        if (rightV == -1) {
            // all further elements are zero: we use the formula, identical to the one from moveToPreciseRank
            return leftV + (rank - (double) leftR) / (double) leftBar;
        }
        final double leftValue = leftBar == 1 ? leftV : leftV + (double) indexInBar / (double) leftBar;
        return leftValue + (rank - r) * (rightV - leftValue);
        //[[Repeat.SectionEnd preciseValueImpl]]
    }

    /**
     * Precise equivalent of {@link #preciseValue(long[], double)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram <code>histogram[k]</code> is the number of elements in the source array
     *                  that are "almost equal" to <code>k</code>.
     *                  All <code>histogram[k]</code> must be non-negative; in another case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param rank      the index in the source array (if non-integer, this method interpolates nearest elements).
     * @return interpolated value of the found element (percentile).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(rank)</code>.
     * @see #iPreciseValue(int[], double)
     */
    public static double preciseValue(int[] histogram, double rank) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, preciseValueImpl)
        //        \blong\b ==> int  !! Auto-generated: NOT EDIT !! ]]
        if (Double.isNaN(rank))
            throw new IllegalArgumentException("Illegal rank argument (NaN)");
        if (rank < 0.0) {
            rank = 0.0;
        }
        final int r = (int) rank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "rank" to "r", "leftValue" to "leftV", etc.
        assert r >= 0;
        int leftV = 0;
        int leftBar = 0;
        int acc = 0;
        int lastNonZero = -1;
        for (; leftV < histogram.length; leftV++) {
            leftBar = histogram[leftV];
            if (leftBar < 0)
                throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + leftBar);
            if (leftBar > 0) {
                lastNonZero = leftV;
                acc += leftBar;
                if (r < acc) {
                    break;
                }
            }
        }
        if (leftV >= histogram.length) {
            return lastNonZero + 1; // = 0 if all columns are zero
        }
        assert leftV == lastNonZero;
        assert leftBar > 0;
        assert r < acc;
        final int leftR = acc - leftBar;
        assert leftR <= r; // in other words, acc - leftBar <= r < acc
        final int indexInBar = r - leftR;
        assert indexInBar < leftBar; // because leftBar = acc - leftR and r < acc
        if (rank == r) {
            return leftBar == 1 ? leftV : leftV + (double) indexInBar / (double) leftBar;
        }
        if (indexInBar < leftBar - 1) {
            return leftV + (rank - (double) leftR) / (double) leftBar;
        }
        assert r + 1 == acc; // r < acc, but r + 1 >= acc, because indexInBar + 1 >= acc - leftR = leftBar
        int rightV = nextNonZero(histogram, leftV + 1);
        if (rightV == -1) {
            // all further elements are zero: we use the formula, identical to the one from moveToPreciseRank
            return leftV + (rank - (double) leftR) / (double) leftBar;
        }
        final double leftValue = leftBar == 1 ? leftV : leftV + (double) indexInBar / (double) leftBar;
        return leftValue + (rank - r) * (rightV - leftValue);
        //[[Repeat.IncludeEnd]]
    }

    //[[Repeat() long(?!\s+sumOfColumns) ==> int]]

    /**
     * Equivalent to <code>{@link #preciseValue(long[], double)
     * preciseValue}(histogram,percentileLevel*(sumOfColumns-1))</code>,
     * if <code>sumOfColumns&gt;0</code>.
     * If <code>sumOfColumns==0</code>, this method immediately returns <code>0.0</code>;
     * if <code>sumOfColumns&lt;0</code>, this method throws an exception.
     *
     * <p>If is supposed that <code>sumOfColumns</code> is the sum of all histogram elements.
     * If it is true, the returned value is usually called the <i>percentile</i> of the source array
     * with the <i>level</i> specified by <code>percentileLevel</code> argument.
     * If <code>percentileLevel==0.5</code>, this value is also called the <i>median</i> of the source array.
     * But, of course, you can pass any positive value as <code>sumOfColumns</code>: in any case,
     * if <code>sumOfColumns&gt;0</code>, this method returns the result of <code>{@link #preciseValue(long[], double)
     * preciseValue}(histogram,percentileLevel*(sumOfColumns-1))</code>.
     *
     * @param histogram       <code>histogram[k]</code> is the number of elements in the source array
     *                        that are "almost equal" to <code>k</code>.
     *                        All <code>histogram[k]</code> must be non-negative; in another case,
     *                        <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param sumOfColumns    should be equal to sum of all histogram elements
     *                        (in other words, the length of the source array).
     * @param percentileLevel the percentile level (usually from 0.0 to 1.0).
     * @return interpolated value of the found element (percentile).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(percentileLevel)</code> or
     *                                  if <code>sumOfColumns&lt;0</code>.
     * @see #sumOf(long[])
     */
    public static double percentile(long[] histogram, long sumOfColumns, double percentileLevel) {
        if (Double.isNaN(percentileLevel))
            throw new IllegalArgumentException("Illegal percentile argument (NaN)");
        if (sumOfColumns < 0)
            throw new IllegalArgumentException("Negative sumOfColumns = " + sumOfColumns);
        if (sumOfColumns == 0) {
            return 0.0;
        }
        return preciseValue(histogram, percentileLevel * (sumOfColumns - 1));
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]

    /**
     * Equivalent to <code>{@link #preciseValue(int[], double)
     * preciseValue}(histogram,percentileLevel*(sumOfColumns-1))</code>,
     * if <code>sumOfColumns&gt;0</code>.
     * If <code>sumOfColumns==0</code>, this method immediately returns <code>0.0</code>;
     * if <code>sumOfColumns&lt;0</code>, this method throws an exception.
     *
     * <p>If is supposed that <code>sumOfColumns</code> is the sum of all histogram elements.
     * If it is true, the returned value is usually called the <i>percentile</i> of the source array
     * with the <i>level</i> specified by <code>percentileLevel</code> argument.
     * If <code>percentileLevel==0.5</code>, this value is also called the <i>median</i> of the source array.
     * But, of course, you can pass any positive value as <code>sumOfColumns</code>: in any case,
     * if <code>sumOfColumns&gt;0</code>, this method returns the result of <code>{@link #preciseValue(int[], double)
     * preciseValue}(histogram,percentileLevel*(sumOfColumns-1))</code>.
     *
     * @param histogram       <code>histogram[k]</code> is the number of elements in the source array
     *                        that are "almost equal" to <code>k</code>.
     *                        All <code>histogram[k]</code> must be non-negative; in another case,
     *                        <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param sumOfColumns    should be equal to sum of all histogram elements
     *                        (in other words, the length of the source array).
     * @param percentileLevel the percentile level (usually from 0.0 to 1.0).
     * @return interpolated value of the found element (percentile).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(percentileLevel)</code> or
     *                                  if <code>sumOfColumns&lt;0</code>.
     * @see #sumOf(int[])
     */
    public static double percentile(int[] histogram, long sumOfColumns, double percentileLevel) {
        if (Double.isNaN(percentileLevel))
            throw new IllegalArgumentException("Illegal percentile argument (NaN)");
        if (sumOfColumns < 0)
            throw new IllegalArgumentException("Negative sumOfColumns = " + sumOfColumns);
        if (sumOfColumns == 0) {
            return 0.0;
        }
        return preciseValue(histogram, percentileLevel * (sumOfColumns - 1));
    }
    //[[Repeat.AutoGeneratedEnd]]

    /**
     * Returns the sum of all elements of the passed <code>histogram</code> array.
     *
     * @param histogram any array (for example, a histogram).
     * @return the sum of all elements of the passed array.
     * @throws NullPointerException if <code>histogram</code> argument is {@code null}.
     */
    public static long sumOf(long[] histogram) {
        long result = 0;
        for (long v : histogram) {
            result += v;
        }
        return result;
    }

    /**
     * Returns the sum of all elements of the passed <code>histogram</code> array.
     *
     * @param histogram any array (for example, a histogram).
     * @return the sum of all elements of the passed array.
     * @throws NullPointerException if <code>histogram</code> argument is {@code null}.
     */
    public static int sumOf(int[] histogram) {
        int result = 0;
        for (int v : histogram) {
            result += v;
        }
        return result;
    }

    /**
     * Fills all non-zero elements of <code>histogram</code> by 0.
     *
     * <p>Works faster than trivial loop for all elements <code>histogram[0..histogram.length-1]</code>,
     * if most of the last elements of <code>histogram</code> array already contain zero.
     *
     * @param histogram    cleared histogram.
     * @param sumOfColumns should be equal to sum of all histogram elements.
     */
    public static void clear(int[] histogram, int sumOfColumns) {
        int acc = 0;
        for (int k = 0; k < histogram.length && acc < sumOfColumns; k++) {
            acc += histogram[k];
            histogram[k] = 0;
        }
        if (acc != sumOfColumns)
            throw new IllegalArgumentException("Illegal sumOfColumns in clearHistogram method: "
                    + sumOfColumns + " instead of " + acc);
    }

    //[[Repeat() long ==> int]]
    static int previousNonZero(long[] histogram, int index) {
        for (; index >= 0; index--) {
            long b = histogram[index];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + index + "]=" + b);
            if (b > 0) {
                return index;
            }
        }
        return -1;
    }

    static int nextNonZero(long[] histogram, int index) {
        for (; index < histogram.length; index++) {
            long b = histogram[index];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + index + "]=" + b);
            if (b > 0) {
                return index;
            }
        }
        return -1;
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static int previousNonZero(int[] histogram, int index) {
        for (; index >= 0; index--) {
            int b = histogram[index];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + index + "]=" + b);
            if (b > 0) {
                return index;
            }
        }
        return -1;
    }

    static int nextNonZero(int[] histogram, int index) {
        for (; index < histogram.length; index++) {
            int b = histogram[index];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + index + "]=" + b);
            if (b > 0) {
                return index;
            }
        }
        return -1;
    }

    //[[Repeat.AutoGeneratedEnd]]

    static long[] cloneBars(Object histogram0) {
        if (histogram0 instanceof long[]) {
            return ((long[]) histogram0).clone();
        } else {
            int[] bars = (int[]) histogram0;
            long[] result = new long[bars.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = bars[k];
            }
            return result;
        }
    }

    // The last command of the following Repeat instruction removes all lines with "sum", "NDV", "DifferentValues"
    //[[Repeat(INCLUDE_FROM_FILE, SummingHistogram.java, SummingLongHistogram)
    //        (Summing|summing\s) ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentSum\(\).*?} ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentNumberOfDifferentValues\(\).*?} ==> ;;
    //        [ \t]*[^\n\r]*(sum(?!Of|\sof)|Sum|NDV|DifferentValues).*?(?:\r(?!\n)|\n|\r\n) ==> !! Auto-generated: NOT EDIT !! ]]
    static class LongHistogram extends Histogram {
        private final long[][] histogram;
        private final long[] histogram0;
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private long total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private long[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private long[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private LongHistogram nextSharing = this; // circular list of all objects sharing the same data
        private long shareCount = 1; // the length of the sharing list

        private LongHistogram(
                long[][] histogram,
                long total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.total = total;
            this.bitLevels = new int[this.m]; // this.bitLevels[0] = 0 here
            System.arraycopy(bitLevels, 0, this.bitLevels, 1, bitLevels.length);
            // cloning before checking guarantees correct check while multithreading
            for (int k = 1; k < this.m; k++) {
                if (this.bitLevels[k] <= 0)
                    throw new IllegalArgumentException("Negative or zero bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k]);
                if (this.bitLevels[k] > 31)
                    throw new IllegalArgumentException("Too high bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k] + " (only 1..31 values are allowed)");
                if (this.bitLevels[k] <= this.bitLevels[k - 1])
                    throw new IllegalArgumentException("bitLevels[" + (k - 1)
                            + "] must be greater than bitLevels[" + (k - 2) + "]");
            }
            this.highBitMasks = new int[this.m];
            for (int k = 0; k < this.m; k++) {
                this.highBitMasks[k] = ~((1 << this.bitLevels[k]) - 1);
            }
            this.currentIRanks = new long[this.m]; // zero-filled
            this.alternativeIRanks = new long[this.m];
        }

        LongHistogram(long[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new long[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        long count = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                        }
                        this.histogram[k][i] = count;
                    }
                }
            }
        }

        @Override
        public long total() {
            return total;
        }

        @Override
        public long bar(int value) {
            return value < 0 ? 0 : value >= length ? 0 : histogram0[value];
        }

        @Override
        public long[] bars() {
            return cloneBars(histogram0);
        }

        @Override
        public void include(int value) {
            if (total == Long.MAX_VALUE)
                throw new IllegalStateException("Overflow of the histogram: cannot include new value "
                        + value + ", because the current total number of values is Long.MAX_VALUE");
            ++histogram0[value];
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                ++histogram[k][v];
                if (value < (currentIValue & highBitMasks[k])) {
                    ++currentIRanks[k];
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                ++currentIRanks[0];
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (LongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        ++hist.currentIRanks[k];
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                }
                ++hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int value) {
            if (--histogram0[value] < 0) {
                long b = histogram0[value];
                histogram0[value] = 0;
                throw new IllegalStateException("Disbalance in the histogram: negative number "
                        + b + " of occurrences of " + value + " value");
            }
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                --histogram[k][v];
                if (value < (currentIValue & highBitMasks[k])) {
                    --currentIRanks[k];
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                --currentIRanks[0];
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (LongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        --hist.currentIRanks[k];
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                }
                --hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void include(int... values) {
            if (total > Long.MAX_VALUE - values.length)
                throw new IllegalStateException("Overflow of the histogram: cannot include new "
                        + values.length + "values, because the total number of values will exceed Long.MAX_VALUE");
            if (shareCount == 2) { // optimization
                long currentIRank1 = currentIRanks[0];
                long currentIRank2 = nextSharing.currentIRanks[0];
                for (final int value : values) {
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            ++nextSharing.currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                    }
                    for (LongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                ++hist.currentIRanks[k];
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                        }
                        hist.total++;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total += values.length;
                currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int... values) {
            if (shareCount == 2) { // optimization
                long currentIRank1 = currentIRanks[0];
                long currentIRank2 = nextSharing.currentIRanks[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            --nextSharing.currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total -= values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total -= values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRanks[0];
                    }
                    for (LongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                --hist.currentIRanks[k];
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                        }
                        hist.total--;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total -= values.length;
                currentPreciseRank = Double.NaN;
            }
        }



        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }



        @Override
        public Histogram moveToIRank(long rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            assert total > 0;
            if (rank < 0) {
                rank = 0;
            } else if (rank > total) {
                rank = total;
            }
            if (rank == total) {
                moveToRightmostRank();
            } else if (rank < currentIRanks[0]) {
                decreasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        int k = 0;
                        while (k + 1 < m && rank < currentIRanks[k + 1]) { // here we suppose that currentIRanks[m]==0
                            k++;
                        }
                        while (k > 0) {
                            assert rank < currentIRanks[k];
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            long b;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                            } while (rank < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final long previousRank = currentIRanks[k] + b;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                            } while (k > 0 && rank >= currentIRanks[k]);
                            currentIValue <<= level;
                        }
                        assert k == 0;
                        if (rank >= currentIRanks[0]) {
                            break decreasingRank;
                        }
                    }
                    // multilevel end (for preprocessing)
                    do {
                        --currentIValue;
                        long b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {
                increasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        if (rank < currentIRanks[0] + histogram0[currentIValue]) {
                            break increasingRank;
                        }
                        int k = 0;
                        while (k + 1 < m && rank >= currentIRanks[k + 1]
                                + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {
                            // here we can suppose that histogram[m][0]==total
                            k++;
                        }
                        while (k > 0) {
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            long b = histogram[k][currentIValue];
                            while (rank >= currentIRanks[k] + b) {
                                currentIRanks[k] += b;
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final long lastRank = currentIRanks[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                            } while (k > 0 && rank < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    long b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                double frac = (double) (rank - currentIRanks[0]) / (double) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0;
                }
                currentValue = currentIValue + frac;
            }
            currentPreciseRank = rank;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToRank(double rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (Double.isNaN(rank))
                throw new IllegalArgumentException("Illegal rank argument (NaN)");
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            currentPreciseRank = Double.NaN;
            assert total > 0;
            final long r;
            if (rank < 0) {
                rank = r = 0;
            } else if (rank > total) {
                rank = r = total;
            } else {
                r = (int) rank;
            }
            if (r == total) {
                moveToRightmostRank();
            } else if (r < currentIRanks[0]) {
                decreasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        int k = 0;
                        while (k + 1 < m && r < currentIRanks[k + 1]) { // here we suppose that currentIRanks[m]==0
                            k++;
                        }
                        while (k > 0) {
                            assert r < currentIRanks[k];
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            long b;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                            } while (r < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final long previousRank = currentIRanks[k] + b;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                            } while (k > 0 && r >= currentIRanks[k]);
                            currentIValue <<= level;
                        }
                        assert k == 0;
                        if (r >= currentIRanks[0]) {
                            break decreasingRank;
                        }
                    }
                    // multilevel end (for preprocessing)
                    do {
                        --currentIValue;
                        long b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {
                increasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        if (r < currentIRanks[0] + histogram0[currentIValue]) {
                            break increasingRank;
                        }
                        int k = 0;
                        while (k + 1 < m && r >= currentIRanks[k + 1]
                                + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {
                            // here we can suppose that histogram[m][0]==total
                            k++;
                        }
                        while (k > 0) {
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            long b = histogram[k][currentIValue];
                            while (r >= currentIRanks[k] + b) {
                                currentIRanks[k] += b;
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final long lastRank = currentIRanks[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                            } while (k > 0 && r < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    long b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                long b = histogram0[currentIValue];
                double frac = (rank - currentIRanks[0]) / (double) b;
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToIValue(int value) {
//            System.out.println("move to " + value + " from " + currentIValue + ": "
//                + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (value < 0) {
                value = 0;
            } else if (value > length) {
                value = length;
            }
            currentValue = value;
            currentPreciseRank = Double.NaN;
            if (value == currentIValue) {
                return this; // in the algorithm below (2nd branch) should be value!=currentIValue
            } else if (value < currentIValue) {
                // multilevel start (for preprocessing)
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && value >> bitLevels[k + 1] < currentIValue >> bitLevels[k + 1]) {
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        final int v = value >> level;
                        currentIValue >>= level;
                        assert v < currentIValue;
                        long b;
                        do {
                            --currentIValue;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] -= b;
                        } while (v < currentIValue);
                        assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                + " < 0 for value=" + value;
                        assert currentIValue == v;
                        final int previousValue = (currentIValue + 1) << level;
                        assert value < previousValue;
                        final long previousRank = currentIRanks[k] + b;
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIValue = (previousValue >> level) - 1;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] = previousRank - b;
                        } while (k > 0 && value >> level == currentIValue);
                        currentIValue <<= level;
                    }
                    assert k == 0;
                    if (value == currentIValue) {
                        return this;
                    }
                    assert value < currentIValue;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue - 1; j >= value; j--) {
                    long b = histogram0[j];
                    currentIRanks[0] -= b;
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                // multilevel start (for preprocessing)
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && value >> bitLevels[k + 1] > currentIValue >> bitLevels[k + 1]) {
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        final int v = value >> level;
                        currentIValue >>= level;
                        assert v > currentIValue;
                        do {
                            currentIRanks[k] += histogram[k][currentIValue];
                            ++currentIValue;
                        } while (v > currentIValue);
                        assert currentIRanks[k] <= total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + "> total=" + total + " for value=" + value;
                        assert currentIValue == v;
                        currentIValue <<= level;
                        assert currentIValue <= value;
                        final long lastRank = currentIRanks[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                        } while (k > 0 && value >> level == currentIValue >> level);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue; j < value; j++) {
                    long b = histogram0[j];
                    currentIRanks[0] += b;
                }
                assert currentIRanks[0] <= total : "currentIRank=" + currentIRanks[0]
                        + " > total=" + total + " for value=" + value;
                // here is possible currentIRanks[0]==total, if the bar #value and higher are zero
            }
            currentIValue = value;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public long shareCount() {
            return shareCount;
        }

        @Override
        public Histogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public Histogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                LongHistogram result = new LongHistogram(histogram,
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                LongHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (LongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "long histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void restoreRanks() {
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            for (int k = 0; k < m; k++) {
                if (k == 0) {
                } else {
                }
                long s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
            }
            if (!Double.isNaN(currentPreciseRank) && !outsideNonZeroPart()) {
                if (Math.abs(preciseValue(histogram0, currentPreciseRank) - currentValue) > 1.0e-3) {
                    throw new AssertionError("Bug in " + this + ": for rank=" + currentPreciseRank
                            + ", precise value is " + currentValue + " instead of "
                            + preciseValue(histogram0, currentPreciseRank) + ", currentIValue = " + currentIValue
                            + ", results of iValue()/iPreciseValue() methods are " + iValue(histogram0, currentIRank())
                            + " and " + iPreciseValue(histogram0, currentPreciseRank) + ", "
                            + histogram0.length + " bars " + JArrays.toString(histogram0, ",", 3000));
                }
            }
        }

        /**
         * Basic rank movement algorithm for the case rank=total.
         */
        private void moveToRightmostRank() {
            assert total > 0; // should be checked outside this method
            assert currentIRanks[0] <= total;
            if (currentIRanks[0] < total) {
                // multilevel start (for preprocessing)
                // moving first to total-1
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && total > currentIRanks[k + 1]
                            + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {   // here we can suppose that
                        // histogram[m][0]==total
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        long b = histogram[k][currentIValue];
                        while (total > currentIRanks[k] + b) {
                            currentIRanks[k] += b;
                            ++currentIValue;
                            b = histogram[k][currentIValue];
                        }
                        assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + ">= total=" + total;
                        currentIValue <<= level;
                        final long lastRank = currentIRanks[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                        } while (k > 0 && total <= currentIRanks[k] + histogram[k][currentIValue >> level]);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                assert currentIRanks[0] < total;
                do {
                    long b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    ++currentIValue;
                } while (currentIRanks[0] < total);
                assert currentIRanks[0] == total : "currentIRank=" + currentIRanks[0] + " > total=" + total;
                assert histogram0[currentIValue - 1] > 0;
                // multilevel start (for preprocessing)
                // moving right to 1 value in levels 1,2,...,m-1
                if (m > 1) {
                    int k = 1;
                    int v;
                    while (k < m && (v = (currentIValue - 1) >> bitLevels[k]) < currentIValue >> bitLevels[k]) {
                        currentIRanks[k] += histogram[k][v];
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            } else if (histogram0[currentIValue - 1] == 0) {
                // in this branch we shall not change the rank, which is already equal to total:
                // we'll just skip trailing zero elements
                assert currentIValue == length || histogram0[currentIValue] == 0;
                currentIValue--; // since this moment we are sure that indexes will be in range: currentIValue<length
                // multilevel start (for preprocessing)
                final int previousValue = currentIValue + 1;
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && histogram[k + 1][currentIValue >> bitLevels[k + 1]] == 0) {
                        // here we can suppose that histogram[m][0]==total>0
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        assert currentIValue > 0;
                        assert histogram[k][currentIValue] == 0;
                        while (histogram[k][currentIValue - 1] == 0) {
                            currentIValue--;
                            assert currentIValue > 0;
                        }
                        int v = currentIValue - 1;
                        assert currentIValue > 0;
                        assert histogram[k][currentIValue] == 0;
                        assert histogram[k][v] > 0;
                        currentIValue <<= level;
                        k--;
                    }
                }
                // multilevel end (for preprocessing)
                assert currentIValue > 0;
                assert histogram0[currentIValue] == 0;
                while (histogram0[currentIValue - 1] == 0) {
                    currentIValue--;
                    assert currentIValue > 0;
                }
                // multilevel start (for preprocessing)
                if (m > 1) {
                    // though the rank was not changed, the partial ranks currentIRanks[k] could decrease
                    int k = 1;
                    int v;
                    while (k < m && (v = currentIValue >> bitLevels[k]) < previousValue >> bitLevels[k]) {
                        long b = histogram[k][v];
                        if (b > 0) { // usually true, if we did not stop at the left boundary of this bar
                            currentIRanks[k] -= histogram[k][v];
                        }
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            }
        }


        private static long[][] newMultilevelHistogram(long[] histogram, int numberOfLevels) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (numberOfLevels > 31)
                throw new IllegalArgumentException("Number of levels must not be greater than 31");
            long[][] result = new long[numberOfLevels][];
            result[0] = histogram;
            return result;
        }

        private static long sumOfAndCheck(long[] histogram, int from, int to) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (to > histogram.length) {
                to = histogram.length;
            }
            long result = 0;
            for (int k = from; k < to; k++) {
                if (histogram[k] < 0)
                    throw new IllegalArgumentException("Negative histogram[" + k + "]=" + histogram[k]);
                result += histogram[k];
                if (result < 0)
                    throw new IllegalArgumentException("Total number of values (sum of all bars in the histogram) "
                            + "is >Long.MAX_VALUE");
            }
            return result;
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, SummingHistogram.java, SummingLongHistogram)
    //        (Summing|summing\s) ==> ;;
    //        [ \t]*private\s+final\s+double\[\]\[\]\s+sums\b.*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentSum\(\).*?} ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentNumberOfDifferentValues\(\).*?} ==> ;;
    //        [ \t]*[^\n\r]*(sum(?!Of|\sof)|Sum|NDV|DifferentValues).*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        LongHistogram ==> Long1LevelHistogram;;
    //        (dec|inc)reasingRank\: ==> ;;
    //        [ \t]*\/\/\s+multilevel\s+start.*?\/\/\s+multilevel\s+end.*?(?:\r(?!\n)|\n|\r\n) ==> !! Auto-generated: NOT EDIT !! ]]
    static class Long1LevelHistogram extends Histogram {
        private final long[][] histogram;
        private final long[] histogram0;
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private long total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private long[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private long[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private Long1LevelHistogram nextSharing = this; // circular list of all objects sharing the same data
        private long shareCount = 1; // the length of the sharing list

        private Long1LevelHistogram(
                long[][] histogram,
                long total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.total = total;
            this.bitLevels = new int[this.m]; // this.bitLevels[0] = 0 here
            System.arraycopy(bitLevels, 0, this.bitLevels, 1, bitLevels.length);
            // cloning before checking guarantees correct check while multithreading
            for (int k = 1; k < this.m; k++) {
                if (this.bitLevels[k] <= 0)
                    throw new IllegalArgumentException("Negative or zero bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k]);
                if (this.bitLevels[k] > 31)
                    throw new IllegalArgumentException("Too high bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k] + " (only 1..31 values are allowed)");
                if (this.bitLevels[k] <= this.bitLevels[k - 1])
                    throw new IllegalArgumentException("bitLevels[" + (k - 1)
                            + "] must be greater than bitLevels[" + (k - 2) + "]");
            }
            this.highBitMasks = new int[this.m];
            for (int k = 0; k < this.m; k++) {
                this.highBitMasks[k] = ~((1 << this.bitLevels[k]) - 1);
            }
            this.currentIRanks = new long[this.m]; // zero-filled
            this.alternativeIRanks = new long[this.m];
        }

        Long1LevelHistogram(long[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new long[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        long count = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                        }
                        this.histogram[k][i] = count;
                    }
                }
            }
        }

        @Override
        public long total() {
            return total;
        }

        @Override
        public long bar(int value) {
            return value < 0 ? 0 : value >= length ? 0 : histogram0[value];
        }

        @Override
        public long[] bars() {
            return cloneBars(histogram0);
        }

        @Override
        public void include(int value) {
            if (total == Long.MAX_VALUE)
                throw new IllegalStateException("Overflow of the histogram: cannot include new value "
                        + value + ", because the current total number of values is Long.MAX_VALUE");
            ++histogram0[value];
            if (value < currentIValue) {
                ++currentIRanks[0];
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (Long1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                }
                ++hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int value) {
            if (--histogram0[value] < 0) {
                long b = histogram0[value];
                histogram0[value] = 0;
                throw new IllegalStateException("Disbalance in the histogram: negative number "
                        + b + " of occurrences of " + value + " value");
            }
            if (value < currentIValue) {
                --currentIRanks[0];
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (Long1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                }
                --hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void include(int... values) {
            if (total > Long.MAX_VALUE - values.length)
                throw new IllegalStateException("Overflow of the histogram: cannot include new "
                        + values.length + "values, because the total number of values will exceed Long.MAX_VALUE");
            if (shareCount == 2) { // optimization
                long currentIRank1 = currentIRanks[0];
                long currentIRank2 = nextSharing.currentIRanks[0];
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                    }
                    for (Long1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                        }
                        hist.total++;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total += values.length;
                currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int... values) {
            if (shareCount == 2) { // optimization
                long currentIRank1 = currentIRanks[0];
                long currentIRank2 = nextSharing.currentIRanks[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    if (value < currentIValue) {
                        --currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total -= values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total -= values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    if (value < currentIValue) {
                        --currentIRanks[0];
                    }
                    for (Long1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                        }
                        hist.total--;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total -= values.length;
                currentPreciseRank = Double.NaN;
            }
        }



        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }



        @Override
        public Histogram moveToIRank(long rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            assert total > 0;
            if (rank < 0) {
                rank = 0;
            } else if (rank > total) {
                rank = total;
            }
            if (rank == total) {
                moveToRightmostRank();
            } else if (rank < currentIRanks[0]) {

                {
                    do {
                        --currentIValue;
                        long b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    long b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                double frac = (double) (rank - currentIRanks[0]) / (double) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0;
                }
                currentValue = currentIValue + frac;
            }
            currentPreciseRank = rank;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToRank(double rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (Double.isNaN(rank))
                throw new IllegalArgumentException("Illegal rank argument (NaN)");
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            currentPreciseRank = Double.NaN;
            assert total > 0;
            final long r;
            if (rank < 0) {
                rank = r = 0;
            } else if (rank > total) {
                rank = r = total;
            } else {
                r = (int) rank;
            }
            if (r == total) {
                moveToRightmostRank();
            } else if (r < currentIRanks[0]) {

                {
                    do {
                        --currentIValue;
                        long b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    long b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                long b = histogram0[currentIValue];
                double frac = (rank - currentIRanks[0]) / (double) b;
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToIValue(int value) {
//            System.out.println("move to " + value + " from " + currentIValue + ": "
//                + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (value < 0) {
                value = 0;
            } else if (value > length) {
                value = length;
            }
            currentValue = value;
            currentPreciseRank = Double.NaN;
            if (value == currentIValue) {
                return this; // in the algorithm below (2nd branch) should be value!=currentIValue
            } else if (value < currentIValue) {
                for (int j = currentIValue - 1; j >= value; j--) {
                    long b = histogram0[j];
                    currentIRanks[0] -= b;
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                for (int j = currentIValue; j < value; j++) {
                    long b = histogram0[j];
                    currentIRanks[0] += b;
                }
                assert currentIRanks[0] <= total : "currentIRank=" + currentIRanks[0]
                        + " > total=" + total + " for value=" + value;
                // here is possible currentIRanks[0]==total, if the bar #value and higher are zero
            }
            currentIValue = value;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public long shareCount() {
            return shareCount;
        }

        @Override
        public Histogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public Histogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                Long1LevelHistogram result = new Long1LevelHistogram(histogram,
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                Long1LevelHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (Long1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "long histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void restoreRanks() {
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            for (int k = 0; k < m; k++) {
                if (k == 0) {
                } else {
                }
                long s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
            }
            if (!Double.isNaN(currentPreciseRank) && !outsideNonZeroPart()) {
                if (Math.abs(preciseValue(histogram0, currentPreciseRank) - currentValue) > 1.0e-3) {
                    throw new AssertionError("Bug in " + this + ": for rank=" + currentPreciseRank
                            + ", precise value is " + currentValue + " instead of "
                            + preciseValue(histogram0, currentPreciseRank) + ", currentIValue = " + currentIValue
                            + ", results of iValue()/iPreciseValue() methods are " + iValue(histogram0, currentIRank())
                            + " and " + iPreciseValue(histogram0, currentPreciseRank) + ", "
                            + histogram0.length + " bars " + JArrays.toString(histogram0, ",", 3000));
                }
            }
        }

        /**
         * Basic rank movement algorithm for the case rank=total.
         */
        private void moveToRightmostRank() {
            assert total > 0; // should be checked outside this method
            assert currentIRanks[0] <= total;
            if (currentIRanks[0] < total) {
                assert currentIRanks[0] < total;
                do {
                    long b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    ++currentIValue;
                } while (currentIRanks[0] < total);
                assert currentIRanks[0] == total : "currentIRank=" + currentIRanks[0] + " > total=" + total;
                assert histogram0[currentIValue - 1] > 0;
            } else if (histogram0[currentIValue - 1] == 0) {
                // in this branch we shall not change the rank, which is already equal to total:
                // we'll just skip trailing zero elements
                assert currentIValue == length || histogram0[currentIValue] == 0;
                currentIValue--; // since this moment we are sure that indexes will be in range: currentIValue<length
                assert currentIValue > 0;
                assert histogram0[currentIValue] == 0;
                while (histogram0[currentIValue - 1] == 0) {
                    currentIValue--;
                    assert currentIValue > 0;
                }
            }
        }


        private static long[][] newMultilevelHistogram(long[] histogram, int numberOfLevels) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (numberOfLevels > 31)
                throw new IllegalArgumentException("Number of levels must not be greater than 31");
            long[][] result = new long[numberOfLevels][];
            result[0] = histogram;
            return result;
        }

        private static long sumOfAndCheck(long[] histogram, int from, int to) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (to > histogram.length) {
                to = histogram.length;
            }
            long result = 0;
            for (int k = from; k < to; k++) {
                if (histogram[k] < 0)
                    throw new IllegalArgumentException("Negative histogram[" + k + "]=" + histogram[k]);
                result += histogram[k];
                if (result < 0)
                    throw new IllegalArgumentException("Total number of values (sum of all bars in the histogram) "
                            + "is >Long.MAX_VALUE");
            }
            return result;
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, SummingHistogram.java, SummingLongHistogram)
    //        (Summing|summing\s) ==> ;;
    //        [ \t]*private\s+final\s+double\[\]\[\]\s+sums\b.*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentSum\(\).*?} ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentNumberOfDifferentValues\(\).*?} ==> ;;
    //        [ \t]*[^\n\r]*(sum(?!Of|\sof)|Sum|NDV|DifferentValues).*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        \blong\b(?!\stotal\(|\scurrentIRank\(|\sbar\(|\sshareCount\(|\[\]\sbars\(|\srank\)) ==> int;;
    //        Long.MAX_VALUE ==> Integer.MAX_VALUE;;
    //        Long ==> Int  !! Auto-generated: NOT EDIT !! ]]
    static class IntHistogram extends Histogram {
        private final int[][] histogram;
        private final int[] histogram0;
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private int total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private int[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private int[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private IntHistogram nextSharing = this; // circular list of all objects sharing the same data
        private int shareCount = 1; // the length of the sharing list

        private IntHistogram(
                int[][] histogram,
                int total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.total = total;
            this.bitLevels = new int[this.m]; // this.bitLevels[0] = 0 here
            System.arraycopy(bitLevels, 0, this.bitLevels, 1, bitLevels.length);
            // cloning before checking guarantees correct check while multithreading
            for (int k = 1; k < this.m; k++) {
                if (this.bitLevels[k] <= 0)
                    throw new IllegalArgumentException("Negative or zero bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k]);
                if (this.bitLevels[k] > 31)
                    throw new IllegalArgumentException("Too high bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k] + " (only 1..31 values are allowed)");
                if (this.bitLevels[k] <= this.bitLevels[k - 1])
                    throw new IllegalArgumentException("bitLevels[" + (k - 1)
                            + "] must be greater than bitLevels[" + (k - 2) + "]");
            }
            this.highBitMasks = new int[this.m];
            for (int k = 0; k < this.m; k++) {
                this.highBitMasks[k] = ~((1 << this.bitLevels[k]) - 1);
            }
            this.currentIRanks = new int[this.m]; // zero-filled
            this.alternativeIRanks = new int[this.m];
        }

        IntHistogram(int[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new int[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        int count = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                        }
                        this.histogram[k][i] = count;
                    }
                }
            }
        }

        @Override
        public long total() {
            return total;
        }

        @Override
        public long bar(int value) {
            return value < 0 ? 0 : value >= length ? 0 : histogram0[value];
        }

        @Override
        public long[] bars() {
            return cloneBars(histogram0);
        }

        @Override
        public void include(int value) {
            if (total == Integer.MAX_VALUE)
                throw new IllegalStateException("Overflow of the histogram: cannot include new value "
                        + value + ", because the current total number of values is Integer.MAX_VALUE");
            ++histogram0[value];
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                ++histogram[k][v];
                if (value < (currentIValue & highBitMasks[k])) {
                    ++currentIRanks[k];
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                ++currentIRanks[0];
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (IntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        ++hist.currentIRanks[k];
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                }
                ++hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int value) {
            if (--histogram0[value] < 0) {
                int b = histogram0[value];
                histogram0[value] = 0;
                throw new IllegalStateException("Disbalance in the histogram: negative number "
                        + b + " of occurrences of " + value + " value");
            }
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                --histogram[k][v];
                if (value < (currentIValue & highBitMasks[k])) {
                    --currentIRanks[k];
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                --currentIRanks[0];
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (IntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        --hist.currentIRanks[k];
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                }
                --hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void include(int... values) {
            if (total > Integer.MAX_VALUE - values.length)
                throw new IllegalStateException("Overflow of the histogram: cannot include new "
                        + values.length + "values, because the total number of values will exceed Integer.MAX_VALUE");
            if (shareCount == 2) { // optimization
                int currentIRank1 = currentIRanks[0];
                int currentIRank2 = nextSharing.currentIRanks[0];
                for (final int value : values) {
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            ++nextSharing.currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                    }
                    for (IntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                ++hist.currentIRanks[k];
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                        }
                        hist.total++;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total += values.length;
                currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int... values) {
            if (shareCount == 2) { // optimization
                int currentIRank1 = currentIRanks[0];
                int currentIRank2 = nextSharing.currentIRanks[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            --nextSharing.currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total -= values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total -= values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRanks[0];
                    }
                    for (IntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                --hist.currentIRanks[k];
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                        }
                        hist.total--;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total -= values.length;
                currentPreciseRank = Double.NaN;
            }
        }



        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }



        @Override
        public Histogram moveToIRank(long rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            assert total > 0;
            if (rank < 0) {
                rank = 0;
            } else if (rank > total) {
                rank = total;
            }
            if (rank == total) {
                moveToRightmostRank();
            } else if (rank < currentIRanks[0]) {
                decreasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        int k = 0;
                        while (k + 1 < m && rank < currentIRanks[k + 1]) { // here we suppose that currentIRanks[m]==0
                            k++;
                        }
                        while (k > 0) {
                            assert rank < currentIRanks[k];
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            int b;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                            } while (rank < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final int previousRank = currentIRanks[k] + b;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                            } while (k > 0 && rank >= currentIRanks[k]);
                            currentIValue <<= level;
                        }
                        assert k == 0;
                        if (rank >= currentIRanks[0]) {
                            break decreasingRank;
                        }
                    }
                    // multilevel end (for preprocessing)
                    do {
                        --currentIValue;
                        int b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {
                increasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        if (rank < currentIRanks[0] + histogram0[currentIValue]) {
                            break increasingRank;
                        }
                        int k = 0;
                        while (k + 1 < m && rank >= currentIRanks[k + 1]
                                + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {
                            // here we can suppose that histogram[m][0]==total
                            k++;
                        }
                        while (k > 0) {
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            int b = histogram[k][currentIValue];
                            while (rank >= currentIRanks[k] + b) {
                                currentIRanks[k] += b;
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final int lastRank = currentIRanks[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                            } while (k > 0 && rank < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    int b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                double frac = (double) (rank - currentIRanks[0]) / (double) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0;
                }
                currentValue = currentIValue + frac;
            }
            currentPreciseRank = rank;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToRank(double rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (Double.isNaN(rank))
                throw new IllegalArgumentException("Illegal rank argument (NaN)");
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            currentPreciseRank = Double.NaN;
            assert total > 0;
            final int r;
            if (rank < 0) {
                rank = r = 0;
            } else if (rank > total) {
                rank = r = total;
            } else {
                r = (int) rank;
            }
            if (r == total) {
                moveToRightmostRank();
            } else if (r < currentIRanks[0]) {
                decreasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        int k = 0;
                        while (k + 1 < m && r < currentIRanks[k + 1]) { // here we suppose that currentIRanks[m]==0
                            k++;
                        }
                        while (k > 0) {
                            assert r < currentIRanks[k];
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            int b;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                            } while (r < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final int previousRank = currentIRanks[k] + b;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                            } while (k > 0 && r >= currentIRanks[k]);
                            currentIValue <<= level;
                        }
                        assert k == 0;
                        if (r >= currentIRanks[0]) {
                            break decreasingRank;
                        }
                    }
                    // multilevel end (for preprocessing)
                    do {
                        --currentIValue;
                        int b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {
                increasingRank:
                {
                    // multilevel start (for preprocessing)
                    if (m > 1) {
                        if (r < currentIRanks[0] + histogram0[currentIValue]) {
                            break increasingRank;
                        }
                        int k = 0;
                        while (k + 1 < m && r >= currentIRanks[k + 1]
                                + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {
                            // here we can suppose that histogram[m][0]==total
                            k++;
                        }
                        while (k > 0) {
                            int level = bitLevels[k];
                            currentIValue >>= level;
                            int b = histogram[k][currentIValue];
                            while (r >= currentIRanks[k] + b) {
                                currentIRanks[k] += b;
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final int lastRank = currentIRanks[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                            } while (k > 0 && r < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    int b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                int b = histogram0[currentIValue];
                double frac = (rank - currentIRanks[0]) / (double) b;
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToIValue(int value) {
//            System.out.println("move to " + value + " from " + currentIValue + ": "
//                + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (value < 0) {
                value = 0;
            } else if (value > length) {
                value = length;
            }
            currentValue = value;
            currentPreciseRank = Double.NaN;
            if (value == currentIValue) {
                return this; // in the algorithm below (2nd branch) should be value!=currentIValue
            } else if (value < currentIValue) {
                // multilevel start (for preprocessing)
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && value >> bitLevels[k + 1] < currentIValue >> bitLevels[k + 1]) {
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        final int v = value >> level;
                        currentIValue >>= level;
                        assert v < currentIValue;
                        int b;
                        do {
                            --currentIValue;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] -= b;
                        } while (v < currentIValue);
                        assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                + " < 0 for value=" + value;
                        assert currentIValue == v;
                        final int previousValue = (currentIValue + 1) << level;
                        assert value < previousValue;
                        final int previousRank = currentIRanks[k] + b;
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIValue = (previousValue >> level) - 1;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] = previousRank - b;
                        } while (k > 0 && value >> level == currentIValue);
                        currentIValue <<= level;
                    }
                    assert k == 0;
                    if (value == currentIValue) {
                        return this;
                    }
                    assert value < currentIValue;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue - 1; j >= value; j--) {
                    int b = histogram0[j];
                    currentIRanks[0] -= b;
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                // multilevel start (for preprocessing)
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && value >> bitLevels[k + 1] > currentIValue >> bitLevels[k + 1]) {
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        final int v = value >> level;
                        currentIValue >>= level;
                        assert v > currentIValue;
                        do {
                            currentIRanks[k] += histogram[k][currentIValue];
                            ++currentIValue;
                        } while (v > currentIValue);
                        assert currentIRanks[k] <= total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + "> total=" + total + " for value=" + value;
                        assert currentIValue == v;
                        currentIValue <<= level;
                        assert currentIValue <= value;
                        final int lastRank = currentIRanks[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                        } while (k > 0 && value >> level == currentIValue >> level);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue; j < value; j++) {
                    int b = histogram0[j];
                    currentIRanks[0] += b;
                }
                assert currentIRanks[0] <= total : "currentIRank=" + currentIRanks[0]
                        + " > total=" + total + " for value=" + value;
                // here is possible currentIRanks[0]==total, if the bar #value and higher are zero
            }
            currentIValue = value;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public long shareCount() {
            return shareCount;
        }

        @Override
        public Histogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public Histogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                IntHistogram result = new IntHistogram(histogram,
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                IntHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (IntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "int histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void restoreRanks() {
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            for (int k = 0; k < m; k++) {
                if (k == 0) {
                } else {
                }
                int s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
            }
            if (!Double.isNaN(currentPreciseRank) && !outsideNonZeroPart()) {
                if (Math.abs(preciseValue(histogram0, currentPreciseRank) - currentValue) > 1.0e-3) {
                    throw new AssertionError("Bug in " + this + ": for rank=" + currentPreciseRank
                            + ", precise value is " + currentValue + " instead of "
                            + preciseValue(histogram0, currentPreciseRank) + ", currentIValue = " + currentIValue
                            + ", results of iValue()/iPreciseValue() methods are " + iValue(histogram0, currentIRank())
                            + " and " + iPreciseValue(histogram0, currentPreciseRank) + ", "
                            + histogram0.length + " bars " + JArrays.toString(histogram0, ",", 3000));
                }
            }
        }

        /**
         * Basic rank movement algorithm for the case rank=total.
         */
        private void moveToRightmostRank() {
            assert total > 0; // should be checked outside this method
            assert currentIRanks[0] <= total;
            if (currentIRanks[0] < total) {
                // multilevel start (for preprocessing)
                // moving first to total-1
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && total > currentIRanks[k + 1]
                            + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {   // here we can suppose that
                        // histogram[m][0]==total
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        int b = histogram[k][currentIValue];
                        while (total > currentIRanks[k] + b) {
                            currentIRanks[k] += b;
                            ++currentIValue;
                            b = histogram[k][currentIValue];
                        }
                        assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + ">= total=" + total;
                        currentIValue <<= level;
                        final int lastRank = currentIRanks[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                        } while (k > 0 && total <= currentIRanks[k] + histogram[k][currentIValue >> level]);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                assert currentIRanks[0] < total;
                do {
                    int b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    ++currentIValue;
                } while (currentIRanks[0] < total);
                assert currentIRanks[0] == total : "currentIRank=" + currentIRanks[0] + " > total=" + total;
                assert histogram0[currentIValue - 1] > 0;
                // multilevel start (for preprocessing)
                // moving right to 1 value in levels 1,2,...,m-1
                if (m > 1) {
                    int k = 1;
                    int v;
                    while (k < m && (v = (currentIValue - 1) >> bitLevels[k]) < currentIValue >> bitLevels[k]) {
                        currentIRanks[k] += histogram[k][v];
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            } else if (histogram0[currentIValue - 1] == 0) {
                // in this branch we shall not change the rank, which is already equal to total:
                // we'll just skip trailing zero elements
                assert currentIValue == length || histogram0[currentIValue] == 0;
                currentIValue--; // since this moment we are sure that indexes will be in range: currentIValue<length
                // multilevel start (for preprocessing)
                final int previousValue = currentIValue + 1;
                if (m > 1) {
                    int k = 0;
                    while (k + 1 < m && histogram[k + 1][currentIValue >> bitLevels[k + 1]] == 0) {
                        // here we can suppose that histogram[m][0]==total>0
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        assert currentIValue > 0;
                        assert histogram[k][currentIValue] == 0;
                        while (histogram[k][currentIValue - 1] == 0) {
                            currentIValue--;
                            assert currentIValue > 0;
                        }
                        int v = currentIValue - 1;
                        assert currentIValue > 0;
                        assert histogram[k][currentIValue] == 0;
                        assert histogram[k][v] > 0;
                        currentIValue <<= level;
                        k--;
                    }
                }
                // multilevel end (for preprocessing)
                assert currentIValue > 0;
                assert histogram0[currentIValue] == 0;
                while (histogram0[currentIValue - 1] == 0) {
                    currentIValue--;
                    assert currentIValue > 0;
                }
                // multilevel start (for preprocessing)
                if (m > 1) {
                    // though the rank was not changed, the partial ranks currentIRanks[k] could decrease
                    int k = 1;
                    int v;
                    while (k < m && (v = currentIValue >> bitLevels[k]) < previousValue >> bitLevels[k]) {
                        int b = histogram[k][v];
                        if (b > 0) { // usually true, if we did not stop at the left boundary of this bar
                            currentIRanks[k] -= histogram[k][v];
                        }
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            }
        }


        private static int[][] newMultilevelHistogram(int[] histogram, int numberOfLevels) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (numberOfLevels > 31)
                throw new IllegalArgumentException("Number of levels must not be greater than 31");
            int[][] result = new int[numberOfLevels][];
            result[0] = histogram;
            return result;
        }

        private static int sumOfAndCheck(int[] histogram, int from, int to) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (to > histogram.length) {
                to = histogram.length;
            }
            int result = 0;
            for (int k = from; k < to; k++) {
                if (histogram[k] < 0)
                    throw new IllegalArgumentException("Negative histogram[" + k + "]=" + histogram[k]);
                result += histogram[k];
                if (result < 0)
                    throw new IllegalArgumentException("Total number of values (sum of all bars in the histogram) "
                            + "is >Integer.MAX_VALUE");
            }
            return result;
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, SummingHistogram.java, SummingLongHistogram)
    //        (Summing|summing\s) ==> ;;
    //        [ \t]*private\s+final\s+double\[\]\[\]\s+sums\b.*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentSum\(\).*?} ==> ;;
    //        (?:\@Override\s+)?public\s+\w+\s+currentNumberOfDifferentValues\(\).*?} ==> ;;
    //        [ \t]*[^\n\r]*(sum(?!Of|\sof)|Sum|NDV|DifferentValues).*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        \blong\b(?!\stotal\(|\scurrentIRank\(|\sbar\(|\sshareCount\(|\[\]\sbars\(|\srank\)) ==> int;;
    //        Long.MAX_VALUE ==> Integer.MAX_VALUE;;
    //        Long ==> Int ;;
    //        IntHistogram ==> Int1LevelHistogram;;
    //        (dec|inc)reasingRank\: ==> ;;
    //        [ \t]*\/\/\s+multilevel\s+start.*?\/\/\s+multilevel\s+end.*?(?:\r(?!\n)|\n|\r\n) ==> !! Auto-generated: NOT EDIT !! ]]
    static class Int1LevelHistogram extends Histogram {
        private final int[][] histogram;
        private final int[] histogram0;
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private int total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private int[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private int[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private Int1LevelHistogram nextSharing = this; // circular list of all objects sharing the same data
        private int shareCount = 1; // the length of the sharing list

        private Int1LevelHistogram(
                int[][] histogram,
                int total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.total = total;
            this.bitLevels = new int[this.m]; // this.bitLevels[0] = 0 here
            System.arraycopy(bitLevels, 0, this.bitLevels, 1, bitLevels.length);
            // cloning before checking guarantees correct check while multithreading
            for (int k = 1; k < this.m; k++) {
                if (this.bitLevels[k] <= 0)
                    throw new IllegalArgumentException("Negative or zero bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k]);
                if (this.bitLevels[k] > 31)
                    throw new IllegalArgumentException("Too high bitLevels[" + (k - 1)
                            + "]=" + this.bitLevels[k] + " (only 1..31 values are allowed)");
                if (this.bitLevels[k] <= this.bitLevels[k - 1])
                    throw new IllegalArgumentException("bitLevels[" + (k - 1)
                            + "] must be greater than bitLevels[" + (k - 2) + "]");
            }
            this.highBitMasks = new int[this.m];
            for (int k = 0; k < this.m; k++) {
                this.highBitMasks[k] = ~((1 << this.bitLevels[k]) - 1);
            }
            this.currentIRanks = new int[this.m]; // zero-filled
            this.alternativeIRanks = new int[this.m];
        }

        Int1LevelHistogram(int[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new int[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        int count = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                        }
                        this.histogram[k][i] = count;
                    }
                }
            }
        }

        @Override
        public long total() {
            return total;
        }

        @Override
        public long bar(int value) {
            return value < 0 ? 0 : value >= length ? 0 : histogram0[value];
        }

        @Override
        public long[] bars() {
            return cloneBars(histogram0);
        }

        @Override
        public void include(int value) {
            if (total == Integer.MAX_VALUE)
                throw new IllegalStateException("Overflow of the histogram: cannot include new value "
                        + value + ", because the current total number of values is Integer.MAX_VALUE");
            ++histogram0[value];
            if (value < currentIValue) {
                ++currentIRanks[0];
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (Int1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                }
                ++hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int value) {
            if (--histogram0[value] < 0) {
                int b = histogram0[value];
                histogram0[value] = 0;
                throw new IllegalStateException("Disbalance in the histogram: negative number "
                        + b + " of occurrences of " + value + " value");
            }
            if (value < currentIValue) {
                --currentIRanks[0];
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (Int1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                }
                --hist.total;
                assert hist.total == total;
                hist.currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void include(int... values) {
            if (total > Integer.MAX_VALUE - values.length)
                throw new IllegalStateException("Overflow of the histogram: cannot include new "
                        + values.length + "values, because the total number of values will exceed Integer.MAX_VALUE");
            if (shareCount == 2) { // optimization
                int currentIRank1 = currentIRanks[0];
                int currentIRank2 = nextSharing.currentIRanks[0];
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                    }
                    for (Int1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                        }
                        hist.total++;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total += values.length;
                currentPreciseRank = Double.NaN;
            }
        }

        @Override
        public void exclude(int... values) {
            if (shareCount == 2) { // optimization
                int currentIRank1 = currentIRanks[0];
                int currentIRank2 = nextSharing.currentIRanks[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    if (value < currentIValue) {
                        --currentIRank1;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                total -= values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total -= values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    if (value < currentIValue) {
                        --currentIRanks[0];
                    }
                    for (Int1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                        }
                        hist.total--;
                        hist.currentPreciseRank = Double.NaN;
                    }
                }
                total -= values.length;
                currentPreciseRank = Double.NaN;
            }
        }



        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }



        @Override
        public Histogram moveToIRank(long rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            assert total > 0;
            if (rank < 0) {
                rank = 0;
            } else if (rank > total) {
                rank = total;
            }
            if (rank == total) {
                moveToRightmostRank();
            } else if (rank < currentIRanks[0]) {

                {
                    do {
                        --currentIValue;
                        int b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    int b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                double frac = (double) (rank - currentIRanks[0]) / (double) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0;
                }
                currentValue = currentIValue + frac;
            }
            currentPreciseRank = rank;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToRank(double rank) {
//            System.out.println("move to " + rank + " from " + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (Double.isNaN(rank))
                throw new IllegalArgumentException("Illegal rank argument (NaN)");
            if (total == 0) {
                assert currentIRanks[0] == 0 : "non-zero current rank when total==0";
                currentPreciseRank = 0; // the only possible value in this case
                currentValue = currentIValue;
                // The last assignment is not obvious.
                // It is necessary because of the contract of currentIValue() method,
                // which MUST return (int)currentValue after every call of moveToRank method -
                // and even after a call of moveToPreciseRank in this special case total=0
                // (because in this case moveToPreciseRank is equivalent to moveToIRank(0)
                // according the contract of moveToIRank).
                // However, it is possible that now currentValue < currentIValue:
                // if we called moveToPreciseRank in a sparse histogram and, after this,
                // cleared the histogram by "exclude(...)" calls.
                // To provide the correct behavior, we must set some "sensible" value of currentValue.
                // The good solution is assigning to currentIValue: variations of the current value
                // should be little to provide good performance of possible future rank corrections.
                return this;
            }
            currentPreciseRank = Double.NaN;
            assert total > 0;
            final int r;
            if (rank < 0) {
                rank = r = 0;
            } else if (rank > total) {
                rank = r = total;
            } else {
                r = (int) rank;
            }
            if (r == total) {
                moveToRightmostRank();
            } else if (r < currentIRanks[0]) {

                {
                    do {
                        --currentIValue;
                        int b = histogram0[currentIValue];
                        currentIRanks[0] -= b;
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    int b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        ++currentIValue;
                        b = histogram0[currentIValue];
                    }
                    assert currentIRanks[0] < total : "currentIRank=" + currentIRanks[0]
                            + " >= total=" + total + " for rank=" + rank;
                }
            }
            if (rank == currentIRanks[0]) {
                currentValue = currentIValue;
            } else {
                int b = histogram0[currentIValue];
                double frac = (rank - currentIRanks[0]) / (double) b;
                if (DEBUG_MODE) {
                    assert frac >= 0.0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public Histogram moveToIValue(int value) {
//            System.out.println("move to " + value + " from " + currentIValue + ": "
//                + JArrays.toString(currentIRanks, ",", 100));
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            if (value < 0) {
                value = 0;
            } else if (value > length) {
                value = length;
            }
            currentValue = value;
            currentPreciseRank = Double.NaN;
            if (value == currentIValue) {
                return this; // in the algorithm below (2nd branch) should be value!=currentIValue
            } else if (value < currentIValue) {
                for (int j = currentIValue - 1; j >= value; j--) {
                    int b = histogram0[j];
                    currentIRanks[0] -= b;
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                for (int j = currentIValue; j < value; j++) {
                    int b = histogram0[j];
                    currentIRanks[0] += b;
                }
                assert currentIRanks[0] <= total : "currentIRank=" + currentIRanks[0]
                        + " > total=" + total + " for value=" + value;
                // here is possible currentIRanks[0]==total, if the bar #value and higher are zero
            }
            currentIValue = value;
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public long shareCount() {
            return shareCount;
        }

        @Override
        public Histogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public Histogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                Int1LevelHistogram result = new Int1LevelHistogram(histogram,
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                Int1LevelHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (Int1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "int histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void restoreRanks() {
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            for (int k = 0; k < m; k++) {
                if (k == 0) {
                } else {
                }
                int s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
            }
            if (!Double.isNaN(currentPreciseRank) && !outsideNonZeroPart()) {
                if (Math.abs(preciseValue(histogram0, currentPreciseRank) - currentValue) > 1.0e-3) {
                    throw new AssertionError("Bug in " + this + ": for rank=" + currentPreciseRank
                            + ", precise value is " + currentValue + " instead of "
                            + preciseValue(histogram0, currentPreciseRank) + ", currentIValue = " + currentIValue
                            + ", results of iValue()/iPreciseValue() methods are " + iValue(histogram0, currentIRank())
                            + " and " + iPreciseValue(histogram0, currentPreciseRank) + ", "
                            + histogram0.length + " bars " + JArrays.toString(histogram0, ",", 3000));
                }
            }
        }

        /**
         * Basic rank movement algorithm for the case rank=total.
         */
        private void moveToRightmostRank() {
            assert total > 0; // should be checked outside this method
            assert currentIRanks[0] <= total;
            if (currentIRanks[0] < total) {
                assert currentIRanks[0] < total;
                do {
                    int b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    ++currentIValue;
                } while (currentIRanks[0] < total);
                assert currentIRanks[0] == total : "currentIRank=" + currentIRanks[0] + " > total=" + total;
                assert histogram0[currentIValue - 1] > 0;
            } else if (histogram0[currentIValue - 1] == 0) {
                // in this branch we shall not change the rank, which is already equal to total:
                // we'll just skip trailing zero elements
                assert currentIValue == length || histogram0[currentIValue] == 0;
                currentIValue--; // since this moment we are sure that indexes will be in range: currentIValue<length
                assert currentIValue > 0;
                assert histogram0[currentIValue] == 0;
                while (histogram0[currentIValue - 1] == 0) {
                    currentIValue--;
                    assert currentIValue > 0;
                }
            }
        }


        private static int[][] newMultilevelHistogram(int[] histogram, int numberOfLevels) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (numberOfLevels > 31)
                throw new IllegalArgumentException("Number of levels must not be greater than 31");
            int[][] result = new int[numberOfLevels][];
            result[0] = histogram;
            return result;
        }

        private static int sumOfAndCheck(int[] histogram, int from, int to) {
            Objects.requireNonNull(histogram, "Null histogram argument");
            if (to > histogram.length) {
                to = histogram.length;
            }
            int result = 0;
            for (int k = from; k < to; k++) {
                if (histogram[k] < 0)
                    throw new IllegalArgumentException("Negative histogram[" + k + "]=" + histogram[k]);
                result += histogram[k];
                if (result < 0)
                    throw new IllegalArgumentException("Total number of values (sum of all bars in the histogram) "
                            + "is >Integer.MAX_VALUE");
            }
            return result;
        }
    }
    //[[Repeat.IncludeEnd]]
}
