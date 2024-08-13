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

package net.algart.arrays;

import java.util.Objects;

/**
 * <p>Summing histogram: an extension of {@link Histogram} class, allowing quick calculation of sums
 * of all elements of the sorted source array <b>A</b>[<i>k</i>] with indexes, lying in some range
 * <i>r</i><sub>1</sub>&le;<i>k</i>&le;<i>r</i><sub>2</sub>, or with values, lying in some range
 * <i>v</i><sub>1</sub>&le;<b>A</b>[<i>k</i>]&le;<i>v</i><sub>2</sub>.
 *
 * <p>This class is an inheritor of {@link Histogram} class, so any summing histogram is also a usual histogram:
 * an array of non-negative integer numbers <b>b</b>[<i>v</i>], 0&le;<i>v</i>&lt;<i>M</i>,
 * where every element <b>b</b>[<i>v</i>] represents the number of occurrence of the value <i>v</i>
 * in some source array <b>A</b>, consisting of integer elements in 0..<i>M</i>&minus;1 range.
 * As in {@link Histogram} class, the integer values <i>v</i> in the source array are 31-bit:
 * 0&le;<i>M</i>&lt;2<sup>31</sup>, the bars of the histogram <b>b</b>[<i>v</i>] and their sum <i>N</i>
 * are 63-bit: 0&le;<i>N</i>&lt;2<sup>63</sup>, and
 * the source array <b>A</b> is always supposed to be <i>sorted in increasing order</i>:
 * <b>A</b>[0]&le;<b>A</b>[1]&le;...&le;<b>A</b>[<i>N</i>&minus;1], where
 * <i>N</i>=<b>b</b>[0]+<b>b</b>[1]+...+<b>b</b>[<i>M</i>&minus;1]
 * is the number of elements in <b>A</b>.</p>
 *
 * <p>The difference from usual histograms is that this class implement several additional methods,
 * which allow efficient solving two additional tasks.
 * Namely, in addition to (1) finding the percentile <i>v</i>(<i>r</i>)
 * and (2) finding the rank <i>r</i>(<i>v</i>) (see the beginning of the comment
 * to the {@link Histogram} class), this class provide efficient solution of the following tasks:</p>
 *
 * <ol start="3">
 * <li>to find the sum <i>Z</i>(<i>r</i>) =
 * <span style="font-size:200%">&sum;</span>&nbsp;<sub>0&le;<i>k</i>&lt;<i>r</i></sub><b>A</b>[<i>k</i>]
 * of <i>r</i> first elements of the sorted source array <b>A</b>[<i>k</i>],
 * if we know the index <i>r</i> in this array;</li>
 * <li>to find the sum <i>z</i>(<i>v</i>) =
 * <span style="font-size:200%">&sum;</span>&nbsp;<sub><b>A</b>[<i>k</i>]&lt;<i>v</i></sub><b>A</b>[<i>k</i>] =
 * <span style="font-size:200%">&sum;</span>&nbsp;<sub>0&le;<i>j</i>&lt;<i>v</i></sub><i>j</i>*<b>b</b>[<i>j</i>]
 * of all elements of the source array <b>A</b>, less then the given value <i>v</i>.</li>
 * </ol>
 *
 * <p>Obviously, it allows to find the sum of all elements lying in a range of indexes in the sorted
 * source array <i>r</i><sub>1</sub>&le;<i>r</i>&le;<i>r</i><sub>2</sub>:
 * it is <i>Z</i>(<i>r</i><sub>2</sub>)&minus;<i>Z</i>(<i>r</i><sub>1</sub>),
 * or in a range of values of the elements in the source array
 * <i>v</i><sub>1</sub>&le;<i>v</i>&le;<i>v</i><sub>2</sub>: it is
 * it is <i>z</i>(<i>v</i><sub>2</sub>)&minus;<i>z</i>(<i>v</i><sub>1</sub>).</p>
 *
 * <p>Like {@link Histogram}, this class does not store and does not try to sort the source array <b>A</b>,
 * it stores only the histogram <b>b</b> and solves all tasks on the base of it.
 * The price of the additional features is less performance: the methods of this class
 * work little slower than the methods of more simple {@link Histogram} class.</p>
 *
 * <p>Like {@link Histogram}, this class generalizes the concept of sum of elements to the
 * floating-point case. Below is the formal definition of the real <i>S</i> and
 * <i>s</i> functions, calculated by this class.</p>
 *
 * <blockquote>
 * <table border="1" style="border-spacing:0">
 * <caption>
 *     <b>Definition of floating-point summing functions <i>S</i>(<i>r</i>) and <i>s</i>(<i>v</i>)</b>
 * </caption>
 *
 * <tr><td style="padding:8px">
 * <p>Let <b>b</b>[0..<i>M</i>&minus;1] be an array of non-negative integer numbers, called
 * the <i>histogram</i> (and stored by this class), and let <i>N</i> be the sum of all these elements
 * (the length of the supposed, but not stored source sorted array <b>A</b>).
 * Let <i>v</i>(<i>r</i>), 0.0&le;<i>r</i>&le;<i>N</i>, and <i>r</i>(<i>v</i>),
 * 0.0&le;<i>v</i>&le;<i>M</i>, are the percentile and the rank real functions,
 * formally defined in the comments to {@link Histogram} class.</p>
 *
 * <p>This class allow to calculate two additional real functions:
 * <i>S</i>(<i>r</i>), where <i>r</i> is a real number in range
 * 0.0&le;<i>r</i>&le;<i>N</i>, and
 * <i>s</i>(<i>v</i>), where <i>v</i> is a real number in range
 * 0.0&le;<i>v</i>&le;<i>M</i>.
 * Like <i>v</i>(<i>r</i>) and <i>r</i>(<i>v</i>), the <i>S</i>(<i>r</i>)
 * and <i>s</i>(<i>v</i>) functions are different in different histogram models: simple and precise
 * (see comments to {@link Histogram} class).
 * Namely, these functions are defined via the following definite integrals:</p>
 *
 * <dl>
 * <dt><b>Simple histogram model</b></dt>
 *
 * <dd>
 * <p>Generalization of <i>Z</i>(<i>r</i>), which is an sum of elements <b>A</b>[<i>r</i>]
 * with integer indexes, to the real function <i>S</i>(<i>r</i>) is very simple:
 * we use a definite integral of the function <i>v</i>(<i>r</i>) with a real argument.
 * After this, we just define <i>s</i>(<i>v</i>) as <i>S</i>(<i>r</i>(<i>v</i>)).
 * <ol>
 * <li><i>S</i>(<i>r</i>) =
 * <span style="font-size:200%">&int;</span><sub>0&le;<i>x</i>&le;<i>r</i></sub>
 * <i>v</i>(<i>x</i>)&nbsp;<i>dx</i>;
 * </li>
 *
 * <li><i>s</i>(<i>v</i>) = <i>S</i>(<i>r</i>(<i>v</i>)) =
 * <span style="font-size:200%">&int;</span><sub>0&le;<i>x</i>&le;<i>r</i>(<i>v</i>)</sub>
 * <i>v</i>(<i>x</i>)&nbsp;<i>dx</i>.<br>
 * Note: according this definition,
 * <i>s</i>(<i>v</i>)=<i>S</i>(<i>0</i>)=<i>0</i> when <i>v</i>&lt;<i>v</i>(0)
 * and <i>s</i>(<i>v</i>)=<i>S</i>(<i>N</i>)
 * when <i>v</i>&gt;<i>v</i>(<i>N</i>).
 * </li>
 * </ol>
 *
 * <p>In the simple histogram model, there is a simple relation between
 * <i>s</i>(<i>v</i>) function and the more simple concept of
 * <i>z</i>(<i>v</i>) sum, defined above in integer terms.
 * Namely, if <i>v</i><sub>0</sub> is integer, then</p>
 *
 * <blockquote>
 * <i>s</i>(<i>v</i><sub>0</sub>) =
 * <span style="font-size:200%">&sum;</span>&nbsp;
 * <sub>0&le;<i>j</i>&lt;<i>v</i>&#x2080;</sub>(<i>j</i>+0.5)*<b>b</b>[<i>j</i>] =
 * <i>z</i>(<i>v</i><sub>0</sub>) + <i>r</i>(<i>v</i><sub>0</sub>)/2
 * </blockquote>
 * </dd>
 *
 * <dt><b>Precise histogram model</b></dt>
 *
 * <dd>
 * <p>In this case, the behaviour of <i>v</i>(<i>r</i>) and <i>r</i>(<i>v</i>)
 * is more complicated and calculating the integral is little more difficult. To allow this class
 * optimization of algorithms, we little change the definition of <i>S</i>(<i>r</i>)
 * and <i>s</i>(<i>v</i>). Namely, in the precise histogram model, these functions
 * also depend on some constant <i>C</i>, the value of which is undocumented and can be chosen by
 * this class to provide the maximal performance:</p>
 *
 * <ol>
 * <li><i>S</i>(<i>r</i>) = <i>C</i> +
 * <span style="font-size:200%">&int;</span><sub>0&le;<i>x</i>&le;<i>r</i></sub>
 * <i>v</i>(<i>x</i>)&nbsp;<i>dx</i>, where <i>C</i> is some constant, the value of which is undocumented;
 * </li>
 *
 * <li><i>s</i>(<i>v</i>) = <i>S</i>(<i>r</i>(<i>v</i>)) = <i>C</i> +
 * <span style="font-size:200%">&int;</span><sub>0&le;<i>x</i>&le;<i>r</i>(<i>v</i>)</sub>
 * <i>v</i>(<i>x</i>)&nbsp;<i>dx</i>.<br>
 * Note: according this definition,
 * <i>s</i>(<i>v</i>)=<i>S</i>(<i>0</i>)=<i>C</i> when <i>v</i>&lt;<i>v</i>(0)
 * and <i>s</i>(<i>v</i>)=<i>S</i>(<i>N</i>)
 * when <i>v</i>&gt;<i>v</i>(<i>N</i>).
 * </li>
 * </ol>
 *
 * <p>Though this definition includes some unknown constant <i>C</i>, is is not important if you
 * need to calculate the difference <i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>)
 * or <i>s</i>(<i>v</i><sub>2</sub>)&minus;<i>s</i>(<i>v</i><sub>1</sub>):
 * such differences do not contain <i>C</i> constant.
 * If you really need to calculate the integral from the right sides of the formulas above,
 * you can calculate it as <i>S</i>(<i>r</i>)&minus;<i>S</i>(0)
 * or <i>s</i>(<i>v</i>)&minus;<i>s</i>(0).</p>
 *
 * <p>The value of the constant <i>C</i> depends on the histogram bars <b>b</b>[<i>k</i>]:
 * in this class, for example, it can vary when
 * you add or remove elements by {@link #include(int) include} / {@link #exclude(int) exclude} methods.</p>
 * </dd>
 * </dl>
 *
 * </td></tr>
 * </table>
 * </blockquote>
 *
 * <p>Like {@link Histogram}, this class is optimized for the case, when we already know some corresponding
 * pair <i>r</i> (rank) and <i>v</i> (percentile), and we need to slightly change the situation:
 * add or remove several <b>A</b> elements, increase or decrease the known rank <i>r</i> or
 * the value <i>v</i>. But in addition to the <i>current value v</i>,
 * <i>current simple rank r<sup>S</sup></i> and <i>current precise rank r<sup>P</sup></i>,
 * this class supports two new parameters:</p>
 *
 * <ul>
 * <li><i>the current simple integral S<sup>S</sup></i> = <i>S</i>(<i>r<sup>S</sup></i>),
 * where <i>r<sup>S</sup></i> is the current simple rank and <i>S</i>(<i>r</i>) function
 * is defined in terms of the simple histogram model;
 * this integral can be got by {@link #currentIntegral()} method;</li>
 *
 * <li><i>the current precise integral S<sup>P</sup></i> = <i>S</i>(<i>r<sup>P</sup></i>),
 * where <i>r<sup>P</sup></i> is the current precise rank and <i>S</i>(<i>r</i>) function
 * is defined in terms of the precise histogram model;
 * this integral can be got by {@link #currentPreciseIntegral()} method.</li>
 * </ul>
 *
 * <p>Unlike <i>v</i>, <i>r<sup>S</sup></i> and <i>r<sup>P</sup></i>, which can be set and read,
 * the <i>S<sup>S</sup></i> and <i>S<sup>P</sup></i> parameters are read-only: they can be only read
 * according the current values of <i>v</i>, <i>r<sup>S</sup></i> and <i>r<sup>P</sup></i>.</p>
 *
 * <p>If you want to get the simple sum of elements of the source <b>A</b> array in integer terms,
 * you also can use {@link #currentSum()} method, which just returns
 * <i>z</i>(<i>v</i><sub>0</sub>) =
 * <span style="font-size:200%">&sum;</span>&nbsp;
 * <sub>0&le;<i>j</i>&lt;<i>v</i>&#x2080;</sub><i>j</i>*<b>b</b>[<i>j</i>]
 * for integer <i>v</i><sub>0</sub>={@link #currentIValue() currentIValue()}.</p>
 *
 * <p>You can create an instance of this class by the following methods:</p>
 *
 * <ul>
 * <li>{@link #newSummingLongHistogram(int histogramLength, int... bitLevelsOfPyramid)};</li>
 * <li>{@link
 * #newSummingLongHistogram(int histogramLength, boolean optimizeSimpleIntegral, int... bitLevelsOfPyramid)};</li>
 * <li>{@link #newSummingLongHistogram(long[] histogram, int... bitLevelsOfPyramid)};</li>
 * <li>{@link
 * #newSummingLongHistogram(long[] histogram, boolean optimizeSimpleIntegral, int... bitLevelsOfPyramid)};</li>
 * <li>{@link #newSummingIntHistogram(int histogramLength, int... bitLevelsOfPyramid)};</li>
 * <li>{@link
 * #newSummingIntHistogram(int histogramLength, boolean optimizeSimpleIntegral, int... bitLevelsOfPyramid)};</li>
 * <li>{@link #newSummingIntHistogram(int[] histogram, int... bitLevelsOfPyramid)};</li>
 * <li>{@link
 * #newSummingIntHistogram(int[] histogram, boolean optimizeSimpleIntegral, int... bitLevelsOfPyramid)}.</li>
 * </ul>
 *
 * <p>This class is often used for calculating differences
 * <i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>) or
 * <i>s</i>(<i>v</i><sub>2</sub>)&minus;<i>s</i>(<i>v</i><sub>1</sub>),
 * when we need to recalculate the difference
 * after little changes of the histogram and of each from two ranks
 * <i>r</i><sub>1</sub>, <i>r</i><sub>2</sub> or two values <i>v</i><sub>1</sub>, <i>v</i><sub>2</sub>.
 * In this situation, it is convenient to {@link #share() share} the histogram between two instances of this object
 * and set the 1nd necessary rank <i>r</i><sub>1</sub> (or value <i>v</i><sub>1</sub>) in the 1st instance
 * and the 2nd necessary rank <i>r</i><sub>2</sub> (or value <i>v</i><sub>2</sub>) in the 2nd instance.
 * The difference between {@link #currentIntegral()} or {@link #currentPreciseIntegral()}, calculated in two
 * instances, will contain the necessary result. Because this situation is often enough, there are
 * special methods {@link #currentIntegralBetweenSharing()} and {@link #currentPreciseIntegralBetweenSharing()}
 * for this task.</p>
 *
 * <p>This class also provides static methods for calculating
 * <i>S</i>(<i>r</i><sub>2</sub>)&minus;<i>S</i>(<i>r</i><sub>1</sub>) or
 * <i>s</i>(<i>v</i><sub>2</sub>)&minus;<i>s</i>(<i>v</i><sub>1</sub>) differences:
 * correspondingly
 * {@link #integralBetweenRanks(long[], double, double)} /
 * {@link #integralBetweenRanks(int[], double, double)} and
 * {@link #integralBetweenValues(long[], double, double, CountOfValues)} /
 * {@link #integralBetweenValues(int[], double, double, CountOfValues)}
 * for the simple histogram model,
 * {@link #preciseIntegralBetweenRanks(long[], double, double)} /
 * {@link #preciseIntegralBetweenRanks(int[], double, double)} and
 * {@link #preciseIntegralBetweenValues(long[], double, double, CountOfValues)} /
 * {@link #preciseIntegralBetweenValues(int[], double, double, CountOfValues)}
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
public abstract class SummingHistogram extends Histogram {
    SummingHistogram(int length) {
        super(length);
    }

    /**
     * <p>The helper class for static methods of {@link SummingHistogram} class,
     * calculating the integrals of <i>v</i>(<i>r</i>) function between
     * two given values: <code>minValue</code>&le;<i>v</i>&le;<code>maxValue</code>.</p>
     *
     * <p>More precisely, this class is used by the static methods</p>
     *
     * <ul>
     * <li>{@link SummingHistogram#integralBetweenValues(long[], double, double, CountOfValues)},</li>
     * <li>{@link SummingHistogram#integralBetweenValues(int[], double, double, CountOfValues)},</li>
     * <li>{@link SummingHistogram#preciseIntegralBetweenValues(long[], double, double, CountOfValues)},</li>
     * <li>{@link SummingHistogram#preciseIntegralBetweenValues(int[], double, double, CountOfValues)}</li>
     * </ul>
     *
     * <p>and allows to return some additional information. All these methods have two arguments
     * <code>minValue</code>, <code>maxValue</code> and calculate the integral of <i>v</i>(<i>r</i>),
     * defined in {@link Histogram  comments to Histogram class},
     * in terms of the simple histogram model for first 2 methods or the precise
     * histogram model for the last 2 methods. The integral is calculated between
     * <i>r</i><sub>1</sub>=<i>r</i>(<code>maxValue</code>)
     * and <i>r</i><sub>2</sub>=<i>r</i>(<code>maxValue</code>),
     * where <i>r</i>(<i>v</i>) is the inverse function to <i>v</i>(<i>r</i>)
     * (see {@link Histogram} class). This integral is returned in the result of the methods.</p>
     *
     * <p>But, while calculating the integral, these methods incidentally calculate the additional information,
     * which is stored in the instance of this class, passed as the last argument, if it is not {@code null}.
     * Namely, they calculate:</p>
     *
     * <ul>
     * <li>{@link #count()}: the difference
     * <i>r</i><sub>2</sub>&minus;<i>r</i><sub>1</sub> =
     * <i>r</i>(<code>maxValue</code>)&minus;<i>r</i>(<code>minValue</code>);</li>
     *
     * <li>{@link #isLeftBound()} flag: it is <code>true</code> if
     * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=0 &mdash;
     * in other words, if <code>minValue..maxValue</code> range fully lies to the left
     * from the minimal element of the source array <b>A</b>[<i>k</i>];</li>
     *
     * <li>{@link #isRightBound()} flag: it is <code>true</code> if
     * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=<i>N</i> &mdash;
     * in other words, if <code>minValue..maxValue</code> range fully lies to the right
     * from the maximal element of the source array <b>A</b>[<i>k</i>].</li>
     * </ul>
     *
     * <p>If <code>minValue&ge;maxValue</code>, these methods always return 0.0 and fill the last argument
     * (if it is not {@code null}) by the following values: <code>{@link #count()}=0</code>,
     * <code>{@link #isLeftBound()}=false</code>, <code>{@link #isRightBound()}=false</code>.
     *
     * <p>Note: in the special case <i>N</i>=0 (all bars <b>b</b>[<i>k</i>] are zero)
     * the values of {@link #isLeftBound()} and {@link #isRightBound()} flags are not specified.</p>
     *
     * <p>The only way to create an instance of this class is the constructor without arguments,
     * that creates an <i>uninitialized</i> instance.
     * "Uninitialized" means that any attempt to read information by {@link #count()},
     * {@link #isLeftBound()} or {@link #isRightBound()} leads to <code>IllegalStateException</code>.
     * The only way to change the information stored in this instance is calling one of 4 static methods
     * of {@link SummingHistogram} class, listed above.
     * These methods change its state to initialized.</p>
     *
     * <p>This class does not implement own <code>equals</code> and <code>hashCode</code> methods.
     * So, this class does not provide a mechanism for comparing different instances of this class.</p>
     *
     * <p>This class is not thread-safe, but <b>is thread-compatible</b>
     * and can be synchronized manually, if multithreading access is necessary.</p>
     */
    public static final class CountOfValues {
        double count = Double.NaN;
        boolean leftBound = false;
        boolean rightBound = false;

        /**
         * Creates new {@link #isInitialized() uninitialized} instance of this class.
         * You must call one of
         * {@link SummingHistogram#integralBetweenValues(long[], double, double, CountOfValues)},
         * {@link SummingHistogram#integralBetweenValues(int[], double, double, CountOfValues)},
         * {@link SummingHistogram#preciseIntegralBetweenValues(long[], double, double, CountOfValues)},
         * {@link SummingHistogram#preciseIntegralBetweenValues(int[], double, double, CountOfValues)}
         * methods for this instance before you will be able to use it.
         */
        public CountOfValues() {
        }

        /**
         * Returns <code>true</code> if and only this object is <i>initialized</i>.
         * It means that it was passed to one of
         * {@link SummingHistogram#integralBetweenValues(long[], double, double, CountOfValues)},
         * {@link SummingHistogram#integralBetweenValues(int[], double, double, CountOfValues)},
         * {@link SummingHistogram#preciseIntegralBetweenValues(long[], double, double, CountOfValues)},
         * {@link SummingHistogram#preciseIntegralBetweenValues(int[], double, double, CountOfValues)}
         * methods at least once
         * and that method was successfully finished.
         * If the object is not initialized, then all its methods, excepting
         * this one and methods of the basic <code>Object</code> class (<code>toString</code>, <code>equals</code>, etc.)
         * throw <code>IllegalStateException</code>.
         *
         * @return whether this object is <i>initialized</i>.
         */
        public boolean isInitialized() {
            return !Double.isNaN(count);
        }

        /**
         * Returns <code>true</code> if
         * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=0 &mdash;
         * in other words, if <code>minValue..maxValue</code> range fully lies to the left
         * from the minimal element of the source array <b>A</b>[<i>k</i>].
         * See the {@link CountOfValues comments to this class} for more details.
         *
         * <p>If <code>minValue&gt;=maxValue</code>, this method returns <code>false</code>.
         *
         * @return whether <code>minValue..maxValue</code> range fully lies to the left
         * from the minimal element of the source array <b>A</b>[<i>k</i>].
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public boolean isLeftBound() {
            checkInitialized();
            return leftBound;
        }

        /**
         * Returns <code>true</code> if
         * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=<i>N</i> &mdash;
         * in other words, if <code>minValue..maxValue</code> range fully lies to the right
         * from the maximal element of the source array <b>A</b>[<i>k</i>].
         * See the {@link CountOfValues comments to this class} for more details.
         *
         * <p>If <code>minValue&gt;=maxValue</code>, this method returns <code>false</code>.
         *
         * @return whether <code>minValue..maxValue</code> range fully lies to the right
         * from the maximal element of the source array <b>A</b>[<i>k</i>].
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public boolean isRightBound() {
            checkInitialized();
            return rightBound;
        }

        /**
         * Returns the difference <i>r</i>(<code>maxValue</code>)&minus;<i>r</i>(<code>minValue</code>).
         * In other words, it is the number of elements of the source <b>A</b> array,
         * lying in range <code>minValue..maxValue</code>, generalized to the real case.
         * See the {@link CountOfValues comments to this class} for more details.
         *
         * <p>In the precise histogram model, this value can be zero only if
         * <code>minValue..maxValue</code> range fully lies to the left from
         * the minimal element or to the right from the maximal element of the source array,
         * in other words, if {@link #isLeftBound()} || {@link #isRightBound()}.
         * In all other cases, this method returns a positive value, because <i>r</i>(<i>v</i>)
         * function is increasing. Unlike this, in the simple histogram model
         * this value will be zero also in a case, when all histogram bars <b>b</b>[<i>k</i>],
         * <i>k</i> is an integer in range <code>(int)minValue</code>&le;<i>k</i>&lt;<code>maxValue</code>,
         * are zero.
         *
         * <p>If <code>minValue&gt;=maxValue</code>, this method returns 0.0.
         *
         * @return the difference <i>r</i>(<code>maxValue</code>)&minus;<i>r</i>(<code>minValue</code>).
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public double count() {
            checkInitialized();
            return count;
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
            if (Double.isNaN(count)) {
                return "not initialized CountOfValues";
            }
            return count + " ("
                    + (leftBound ? "" : "not ") + " left bound, "
                    + (rightBound ? "" : "not ") + " right bound)";

        }

        private void checkInitialized() {
            if (Double.isNaN(count))
                throw new IllegalStateException("This instance is not initialized by integralBetweenValues "
                        + "or preciseIntegralBetweenValues method yet");
        }
    }

    //[[Repeat() long ==> int;; Long.MAX_VALUE ==> Integer.MAX_VALUE;; Long ==> Int]]

    /**
     * Equivalent to {@link #newSummingLongHistogram(int, boolean, int...)
     * newSummingLongHistogram(histogramLength, false, bitLevelsOfPyramid)}.
     *
     * @param histogramLength    the number <i>M</i> of bars of the new histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new summing histogram with zero (empty) bars <b>b</b>[<i>k</i>]=0.
     * @throws NullPointerException     if <code>bitLevelsOfPyramid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>histogramLength&lt;0</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code>
     *                                  is not in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static SummingHistogram newSummingLongHistogram(
            int histogramLength,
            int... bitLevelsOfPyramid) {
        return newSummingLongHistogram(histogramLength, false, bitLevelsOfPyramid);
    }

    /**
     * Creates new histogram, consisting of <i>M</i>=<code>histogramLength</code> empty bars.
     * It is an analog of {@link #newLongHistogram(int, int...)} method; the only difference
     * is that this method creates an instance of {@link SummingHistogram} class.
     *
     * <p>The <code>optimizeSimpleIntegral</code> argument allows to provide maximal performance
     * if you are going to use the created instance for calculating only the simple current
     * integral <i>S<sup>S</sup></i> and do not need to calculate the precise integral <i>S<sup>P</sup></i>
     * (see the {@link SummingHistogram comments to this class}). Namely, if this argument is <code>false</code>,
     * this class provides good performance for calculating both integrals:
     * all methods of this class usually require <i>O</i>(1) operations.
     * If it is <code>true</code>, then {@link #include(int) include}, {@link #exclude(int) exclude}
     * and all <code>moveTo...</code> methods will work rather more quickly, because they will not recalculate
     * some internal invariants necessary for calculating the current precise integral <i>S<sup>P</sup></i>.
     * But, as a result, the methods {@link #currentPreciseIntegral()}
     * and {@link #currentPreciseIntegralBetweenSharing()}, calculating <i>S<sup>P</sup></i>,
     * and also {@link #currentNumberOfDifferentValues()} method
     * will work more slowly. Namely, they can require <i>O</i>(<i>M</i>) operations,
     * even in a case of using the histogram pyramid (see comments to <code>bitLevelsOfPyramid</code> argument
     * in {@link #newLongHistogram(int, int...)} method).
     *
     * @param histogramLength        the number <i>M</i> of bars of the new histogram.
     * @param optimizeSimpleIntegral whether the created instance should be optimized for calculating
     *                               the current simple integral <i>S<sup>S</sup></i>.
     * @param bitLevelsOfPyramid     the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                               in the "histogram pyramid"; can be empty, then will be ignored
     *                               (the histogram pyramid will not be used).
     * @return the new summing histogram with zero (empty) bars <b>b</b>[<i>k</i>]=0.
     * @throws NullPointerException     if <code>bitLevelsOfPyramid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>histogramLength&lt;0</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code> is not
     *                                  in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static SummingHistogram newSummingLongHistogram(
            int histogramLength,
            boolean optimizeSimpleIntegral,
            int... bitLevelsOfPyramid) {
        if (histogramLength < 0)
            throw new IllegalArgumentException("Negative histogramLength");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        if (optimizeSimpleIntegral) {
            return bitLevelsOfPyramid.length == 0 ?
                    new SimplifiedSummingLong1LevelHistogram(new long[histogramLength], bitLevelsOfPyramid, true) :
                    new SimplifiedSummingLongHistogram(new long[histogramLength], bitLevelsOfPyramid, true);
        } else {
            return bitLevelsOfPyramid.length == 0 ?
                    new SummingLong1LevelHistogram(new long[histogramLength], bitLevelsOfPyramid, true) :
                    new SummingLongHistogram(new long[histogramLength], bitLevelsOfPyramid, true);
        }
    }

    /**
     * Equivalent to {@link #newSummingLongHistogram(long[], boolean, int...)
     * newSummingLongHistogram(histogram, false, bitLevelsOfPyramid)}.
     *
     * @param histogram          initial values of the bars <b>b</b>[<i>k</i>] of the histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new histogram with bars <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>].
     * @throws NullPointerException     if <code>histogram</code> or <code>bitLevelsOfPyramid</code> argument
     *                                  is {@code null}.
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
    public static SummingHistogram newSummingLongHistogram(
            long[] histogram,
            int... bitLevelsOfPyramid) {
        return newSummingLongHistogram(histogram, false, bitLevelsOfPyramid);
    }

    /**
     * Creates new histogram, consisting of <i>M</i>=<code>histogram.length</code> bars, equal to elements
     * of the given array.
     * It is an analog of {@link #newLongHistogram(long[], int...)} method; the only difference
     * is that this method creates an instance of {@link SummingHistogram} class.
     *
     * <p>The <code>optimizeSimpleIntegral</code> argument allows to provide maximal performance
     * if you are going to use the created instance for calculating only the simple current
     * integral <i>S<sup>S</sup></i> and do not need to calculate the precise integral <i>S<sup>P</sup></i>
     * (see the {@link SummingHistogram comments to this class}). Namely, if this argument is <code>false</code>,
     * this class provides good performance for calculating both integrals:
     * all methods of this class usually require <i>O</i>(1) operations.
     * If it is <code>true</code>, then {@link #include(int) include}, {@link #exclude(int) exclude}
     * and all <code>moveTo...</code> methods will work rather more quickly, because they will not recalculate
     * some internal invariants necessary for calculating the current precise integral <i>S<sup>P</sup></i>.
     * But, as a result, the methods {@link #currentPreciseIntegral()}
     * and {@link #currentPreciseIntegralBetweenSharing()}, calculating <i>S<sup>P</sup></i>,
     * and also {@link #currentNumberOfDifferentValues()} method
     * will work more slowly. Namely, they can require <i>O</i>(<i>M</i>) operations,
     * even in a case of using the histogram pyramid (see comments to <code>bitLevelsOfPyramid</code> argument
     * in {@link #newLongHistogram(long[], int...)} method).
     *
     * @param histogram              initial values of the bars <b>b</b>[<i>k</i>] of the histogram.
     * @param optimizeSimpleIntegral whether the created instance should be optimized for calculating
     *                               the current simple integral <i>S<sup>S</sup></i>.
     * @param bitLevelsOfPyramid     the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                               in the "histogram pyramid"; can be empty, then will be ignored
     *                               (the histogram pyramid will not be used).
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
    public static SummingHistogram newSummingLongHistogram(
            long[] histogram,
            boolean optimizeSimpleIntegral,
            int... bitLevelsOfPyramid) {
        Objects.requireNonNull(histogram, "Null histogram argument");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        if (optimizeSimpleIntegral) {
            return bitLevelsOfPyramid.length == 0 ?
                    new SimplifiedSummingLong1LevelHistogram(histogram.clone(), bitLevelsOfPyramid, false) :
                    new SimplifiedSummingLongHistogram(histogram.clone(), bitLevelsOfPyramid, false);
        } else {
            return bitLevelsOfPyramid.length == 0 ?
                    new SummingLong1LevelHistogram(histogram.clone(), bitLevelsOfPyramid, false) :
                    new SummingLongHistogram(histogram.clone(), bitLevelsOfPyramid, false);
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]

    /**
     * Equivalent to {@link #newSummingIntHistogram(int, boolean, int...)
     * newSummingIntHistogram(histogramLength, false, bitLevelsOfPyramid)}.
     *
     * @param histogramLength    the number <i>M</i> of bars of the new histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new summing histogram with zero (empty) bars <b>b</b>[<i>k</i>]=0.
     * @throws NullPointerException     if <code>bitLevelsOfPyramid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>histogramLength&lt;0</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code>
     *                                  is not in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static SummingHistogram newSummingIntHistogram(
            int histogramLength,
            int... bitLevelsOfPyramid) {
        return newSummingIntHistogram(histogramLength, false, bitLevelsOfPyramid);
    }

    /**
     * Creates new histogram, consisting of <i>M</i>=<code>histogramLength</code> empty bars.
     * It is an analog of {@link #newIntHistogram(int, int...)} method; the only difference
     * is that this method creates an instance of {@link SummingHistogram} class.
     *
     * <p>The <code>optimizeSimpleIntegral</code> argument allows to provide maximal performance
     * if you are going to use the created instance for calculating only the simple current
     * integral <i>S<sup>S</sup></i> and do not need to calculate the precise integral <i>S<sup>P</sup></i>
     * (see the {@link SummingHistogram comments to this class}). Namely, if this argument is <code>false</code>,
     * this class provides good performance for calculating both integrals:
     * all methods of this class usually require <i>O</i>(1) operations.
     * If it is <code>true</code>, then {@link #include(int) include}, {@link #exclude(int) exclude}
     * and all <code>moveTo...</code> methods will work rather more quickly, because they will not recalculate
     * some internal invariants necessary for calculating the current precise integral <i>S<sup>P</sup></i>.
     * But, as a result, the methods {@link #currentPreciseIntegral()}
     * and {@link #currentPreciseIntegralBetweenSharing()}, calculating <i>S<sup>P</sup></i>,
     * and also {@link #currentNumberOfDifferentValues()} method
     * will work more slowly. Namely, they can require <i>O</i>(<i>M</i>) operations,
     * even in a case of using the histogram pyramid (see comments to <code>bitLevelsOfPyramid</code> argument
     * in {@link #newIntHistogram(int, int...)} method).
     *
     * @param histogramLength        the number <i>M</i> of bars of the new histogram.
     * @param optimizeSimpleIntegral whether the created instance should be optimized for calculating
     *                               the current simple integral <i>S<sup>S</sup></i>.
     * @param bitLevelsOfPyramid     the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                               in the "histogram pyramid"; can be empty, then will be ignored
     *                               (the histogram pyramid will not be used).
     * @return the new summing histogram with zero (empty) bars <b>b</b>[<i>k</i>]=0.
     * @throws NullPointerException     if <code>bitLevelsOfPyramid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>histogramLength&lt;0</code>,
     *                                  or if <code>bitLevelsOfPyramid.length&gt;30</code>,
     *                                  or if some of the elements <code>bitLevelsOfPyramid</code> is not
     *                                  in 1..31 range,
     *                                  or if <code>bitLevelsOfPyramid</code>[<i>k</i>] &gt;=
     *                                  <code>bitLevelsOfPyramid</code>[<i>k</i>+1]
     *                                  for some <i>k</i>.
     */
    public static SummingHistogram newSummingIntHistogram(
            int histogramLength,
            boolean optimizeSimpleIntegral,
            int... bitLevelsOfPyramid) {
        if (histogramLength < 0)
            throw new IllegalArgumentException("Negative histogramLength");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        if (optimizeSimpleIntegral) {
            return bitLevelsOfPyramid.length == 0 ?
                    new SimplifiedSummingInt1LevelHistogram(new int[histogramLength], bitLevelsOfPyramid, true) :
                    new SimplifiedSummingIntHistogram(new int[histogramLength], bitLevelsOfPyramid, true);
        } else {
            return bitLevelsOfPyramid.length == 0 ?
                    new SummingInt1LevelHistogram(new int[histogramLength], bitLevelsOfPyramid, true) :
                    new SummingIntHistogram(new int[histogramLength], bitLevelsOfPyramid, true);
        }
    }

    /**
     * Equivalent to {@link #newSummingIntHistogram(int[], boolean, int...)
     * newSummingIntHistogram(histogram, false, bitLevelsOfPyramid)}.
     *
     * @param histogram          initial values of the bars <b>b</b>[<i>k</i>] of the histogram.
     * @param bitLevelsOfPyramid the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                           in the "histogram pyramid"; can be empty, then will be ignored
     *                           (the histogram pyramid will not be used).
     * @return the new histogram with bars <b>b</b>[<i>k</i>]=<code>histogram</code>[<i>k</i>].
     * @throws NullPointerException     if <code>histogram</code> or <code>bitLevelsOfPyramid</code> argument
     *                                  is {@code null}.
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
    public static SummingHistogram newSummingIntHistogram(
            int[] histogram,
            int... bitLevelsOfPyramid) {
        return newSummingIntHistogram(histogram, false, bitLevelsOfPyramid);
    }

    /**
     * Creates new histogram, consisting of <i>M</i>=<code>histogram.length</code> bars, equal to elements
     * of the given array.
     * It is an analog of {@link #newIntHistogram(int[], int...)} method; the only difference
     * is that this method creates an instance of {@link SummingHistogram} class.
     *
     * <p>The <code>optimizeSimpleIntegral</code> argument allows to provide maximal performance
     * if you are going to use the created instance for calculating only the simple current
     * integral <i>S<sup>S</sup></i> and do not need to calculate the precise integral <i>S<sup>P</sup></i>
     * (see the {@link SummingHistogram comments to this class}). Namely, if this argument is <code>false</code>,
     * this class provides good performance for calculating both integrals:
     * all methods of this class usually require <i>O</i>(1) operations.
     * If it is <code>true</code>, then {@link #include(int) include}, {@link #exclude(int) exclude}
     * and all <code>moveTo...</code> methods will work rather more quickly, because they will not recalculate
     * some internal invariants necessary for calculating the current precise integral <i>S<sup>P</sup></i>.
     * But, as a result, the methods {@link #currentPreciseIntegral()}
     * and {@link #currentPreciseIntegralBetweenSharing()}, calculating <i>S<sup>P</sup></i>,
     * and also {@link #currentNumberOfDifferentValues()} method
     * will work more slowly. Namely, they can require <i>O</i>(<i>M</i>) operations,
     * even in a case of using the histogram pyramid (see comments to <code>bitLevelsOfPyramid</code> argument
     * in {@link #newIntHistogram(int[], int...)} method).
     *
     * @param histogram              initial values of the bars <b>b</b>[<i>k</i>] of the histogram.
     * @param optimizeSimpleIntegral whether the created instance should be optimized for calculating
     *                               the current simple integral <i>S<sup>S</sup></i>.
     * @param bitLevelsOfPyramid     the bit levels: binary logarithms of widths of bars in the sub-histograms
     *                               in the "histogram pyramid"; can be empty, then will be ignored
     *                               (the histogram pyramid will not be used).
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
    public static SummingHistogram newSummingIntHistogram(
            int[] histogram,
            boolean optimizeSimpleIntegral,
            int... bitLevelsOfPyramid) {
        Objects.requireNonNull(histogram, "Null histogram argument");
        Objects.requireNonNull(bitLevelsOfPyramid, "Null bitLevelsOfPyramid argument");
        if (optimizeSimpleIntegral) {
            return bitLevelsOfPyramid.length == 0 ?
                    new SimplifiedSummingInt1LevelHistogram(histogram.clone(), bitLevelsOfPyramid, false) :
                    new SimplifiedSummingIntHistogram(histogram.clone(), bitLevelsOfPyramid, false);
        } else {
            return bitLevelsOfPyramid.length == 0 ?
                    new SummingInt1LevelHistogram(histogram.clone(), bitLevelsOfPyramid, false) :
                    new SummingIntHistogram(histogram.clone(), bitLevelsOfPyramid, false);
        }
    }
    //[[Repeat.AutoGeneratedEnd]]

    @Override
    public abstract SummingHistogram nextSharing();

    @Override
    public abstract SummingHistogram share();

    /**
     * Returns the number of non-zero bars <b>b</b>[<i>k</i>] with indexes
     * <i>k</i>&lt;{@link #currentIValue() currentIValue()}.
     * In other words, it is the count of <i>different</i> elements of the source array <b>A</b>,
     * less than {@link #currentIValue() currentIValue()}.
     *
     * <p>This method works quickly (it just returns an internal variable, supported by all methods of this class)
     * only if this class was not created by
     * {@link #newSummingLongHistogram(int, boolean, int...)},
     * {@link #newSummingLongHistogram(long[], boolean, int...)},
     * {@link #newSummingIntHistogram(int, boolean, int...)},
     * {@link #newSummingIntHistogram(int[], boolean, int...)} methods
     * with the argument <code>optimizeSimpleIntegral=true</code>.
     * In this case, the internal value, returned by this method, is also used for calculating
     * the current precise integral <i>S<sup>P</sup></i>.
     * If this class was created with the flag <code>optimizeSimpleIntegral=true</code>,
     * this method just performs a simple loop on all <b>b</b>[<i>k</i>],
     * <i>k</i>=0,1,...,{@link #currentIValue() currentIValue()}&minus;1,
     * and therefore works slowly.
     *
     * @return the number of non-zero bars <b>b</b>[<i>k</i>] with indexes
     * <i>k</i>&lt;{@link #currentIValue() currentIValue()}.
     * @see #currentPreciseIntegral()
     */
    public abstract int currentNumberOfDifferentValues();

    /**
     * Returns the sum of all elements of the source array <b>A</b>, less than
     * <i>v</i><sub>0</sub>={@link #currentIValue() currentIValue()}:
     * <i>z</i>(<i>v</i><sub>0</sub>) =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub><b>A</b>[<i>k</i>]&lt;<i>v</i></sub><b>A</b>[<i>k</i>] =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub>0&le;<i>j</i>&lt;<i>v</i></sub><i>j</i>*<b>b</b>[<i>j</i>].
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * <p>Note: if the current value is integer, for example, after
     * {@link #moveToIValue(int) moveToIValue}(<i>v</i><sub>0</sub>) call,
     * the {@link #currentIntegral()} <i>S<sup>S</sup></i> is equal to
     * this sum plus 0.5*{@link #currentRank() currentRank()}:
     *
     * <blockquote>
     * <i>S<sup>S</sup></i> = <i>s</i>(<i>v</i><sub>0</sub>) =
     * <span style="font-size:200%">&sum;</span>&nbsp;<sub>0&le;<i>j</i>&lt;<i>v</i><sub>0</sub></sub>
     * (<i>j</i>+0.5)*<b>b</b>[<i>j</i>] =
     * <i>z</i>(<i>v</i><sub>0</sub>) +
     * 0.5 * <span style="font-size:200%">&sum;</span>&nbsp;<sub>0&le;<i>j</i>&lt;<i>v</i><sub>0</sub></sub>
     * <b>b</b>[<i>j</i>] =
     * <i>z</i>(<i>v</i><sub>0</sub>) + <i>r</i>(<i>v</i><sub>0</sub>)/2
     * </blockquote>
     *
     * @return the sum of all elements, less than {@link #currentIValue() currentIValue()}.
     * @see #currentSum()
     * @see #currentIntegral()
     * @see #currentPreciseIntegral()
     */
    public abstract double currentSum();

    /**
     * Returns the current simple integral <i>S<sup>S</sup></i>.
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * @return the current simple integral.
     * @see #currentSum()
     * @see #currentPreciseIntegral()
     * @see #currentIntegralBetweenSharing()
     */
    public final double currentIntegral() {
        final long total = total();
        assert currentValue >= currentIValue - 0.5001 : "currentValue = " + currentValue
                + " < currentIValue - 0.5001, currentIValue = " + currentIValue;
        assert currentValue < currentIValue + 1.0001 :
                "currentValue = " + currentValue + " >= currentIValue + 1.0 = " + (currentIValue + 1.0);
        if (total == 0) {
            return 0.0;
        }
        final boolean shifted = currentValue < currentIValue; // possible after moveToPreciseRank
        final int v = shifted ? currentIValue - 1 : currentIValue;
        final long b = bar(v);
        final long r = shifted ? currentIRank() - b : currentIRank();
        if (DEBUG_MODE) {
            if (v < 0)
                throw new AssertionError("Negative value" + v);
            if (b < 0)
                throw new AssertionError("Negative histogram bar #" + v);
            if (r < 0)
                throw new AssertionError("Negative rank " + r + " at bar #" + v + " (" + currentIValue + ") = " + b);
            if (r + b > total)
                throw new AssertionError("Rank " + r + " + bar #"
                        + v + " (" + currentIValue + ") = " + b + " > total number of elements " + total);
            if (r + b == total && shifted)
                throw new AssertionError("currentValue = " + currentValue + " < currentIValue = "
                        + currentIValue + ": it cannot at the histogram end, but rank is " + r + "+" + b + " / " + total);
        }
        if (r + b == 0) {
            return 0.0;
        }
        final double s = shifted ? currentSum() - (double) b * (double) v : currentSum();
        if (b == 0) {
            return s + 0.5 * r;
        }
        final double delta = currentValue - v;
        if (delta == 0.0) {
            return s + 0.5 * r;
        }
        double indexInBar = b == 1 ? delta : delta * (double) b;
        return s + 0.5 * r + indexInBar * (v + 0.5 * delta);
    }

    /**
     * Returns the current precise integral <i>S<sup>P</sup></i>.
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * @return the current precise integral.
     * @see #currentSum()
     * @see #currentIntegral()
     * @see #currentPreciseIntegralBetweenSharing()
     */
    public final double currentPreciseIntegral() {
        // For integer v, this method implements the formula s(v) = z(v) + 0.5 * (r(v) + v - ndv(v)),
        // where z(v) is currentSums[0], r(v) is currentIRank()
        // and ndv(v) is currentNumberOfDifferentValues()
        final long total = total();
        assert currentValue >= currentIValue - 0.5001 : "currentValue = " + currentValue
                + " < currentIValue - 0.5001, currentIValue = " + currentIValue;
        assert currentValue < currentIValue + 1.0001 :
                "currentValue = " + currentValue + " >= currentIValue + 1.0 = " + (currentIValue + 1.0);
        if (total == 0) {
            return 0.0;
        }
        final boolean shifted = currentValue < currentIValue; // possible after moveToPreciseRank
        final int v = shifted ? currentIValue - 1 : currentIValue;
        final long b = bar(v);
        final long r = shifted ? currentIRank() - b : currentIRank();
        if (DEBUG_MODE) {
            if (v < 0)
                throw new AssertionError("Negative value" + v);
            if (b < 0)
                throw new AssertionError("Negative histogram bar #" + v);
            if (r < 0)
                throw new AssertionError("Negative rank " + r + " at bar #" + v + " (" + currentIValue + ") = " + b);
            if (r + b > total)
                throw new AssertionError("Rank " + r + " + bar #"
                        + v + " (" + currentIValue + ") = " + b + " > total number of elements " + total);
            if (r + b == total && shifted)
                throw new AssertionError("currentValue = " + currentValue + " < currentIValue = "
                        + currentIValue + ": it cannot at the histogram end, but rank is " + r + "+" + b + " / " + total);
        }
        final int savedIValue = currentIValue;
        final double savedValue = currentValue;
        if (r + b == 0) {
            saveRanks();
            moveToIRank(0);
            double result = 0.5 * currentValue; // before the 1st bar s(v)=vmin/2
            restoreRanks();
            currentIValue = savedIValue;
            currentValue = savedValue;
            return result;
        }
        final int ndv = shifted && b > 0 ? currentNumberOfDifferentValues() - 1 : currentNumberOfDifferentValues();
        if (r == total) {
            assert !shifted; // checked above (r+b==total)
            // Here we have s + 0.5 * (r + v(r) - (ndv-1));
            // we continue this as this constant.
            // Here currentValue-ndv is the total number of empty bars:
            // currentValue >= v+1, where v is the rightmost bar.
            saveRanks();
            moveToIRank(total);
            double result = currentSum() + 0.5 * r + 0.5 * (currentValue - ndv);
            restoreRanks();
            currentIValue = savedIValue;
            currentValue = savedValue;
            return result;
        }
        assert r < total;
        final double delta = currentValue - v;
        // If b > 0, then v is the correct value for the current integer rank r: v=v(r).
        // (In another case, it is possible that v is inside a zero area between v(r) and v(r+bar(v(r))).)
        // Really, r <= precise rank < r+b, v(r) <= currentValue < v(r+b),
        // and the only non-zero bar between v(r) and v(r'=r+bar(v(r))) is v(r):
        // it's impossible that v(r)<v<v(r'), and also impossible v>=v(r'), because r(v)=r, not r'.
        if (b > 0 && delta == 0.0) {
            final double s = shifted ? currentSum() - (double) b * (double) v : currentSum();
            return s + 0.5 * r + 0.5 * (v - ndv); // the correct formula for integer ranks
            // here and below we write just "v-ndv": overflow is impossible while subtracting non-negative integers
        }
        final double savedPreciseRank = currentPreciseRank;
        final boolean needRank = Double.isNaN(savedPreciseRank);
        final double v1, sum1;
        if (b > 1) {
            final double s = shifted ? currentSum() - (double) b * (double) v : currentSum();
            double indexInBar = delta * (double) b;
            if (indexInBar <= b - 1 // delta in range 0..(b-1)/b: simple case
                    || r + b == total) // the rightmost range (b-1)/b..1.0: special case
            {
                if (needRank) {
                    currentPreciseRank = r + indexInBar;
                }
                return s + 0.5 * r + 0.5 * (v - ndv) + indexInBar * (v + 0.5 * delta);
                // indexInBar * (v + 0.5 * delta) is the area of the additional trapezoid
            }
            v1 = v + (double) (b - 1) / (double) b;
            sum1 = s + 0.5 * r + 0.5 * (v - ndv) + (b - 1) * 0.5 * (v + v1);
            // sum1 is the integral (area left from) v(x) until x=r+b-1, v(x)=v1
            // (b - 1) * 0.5 * (v + v1) is the area of the additional trapezoid
            saveRanks();
        } else {
            if (r + b == total) {
                assert !shifted; // checked above
                assert b == 1; // else we should already return due to the check "r==total"
                if (needRank) {
                    currentPreciseRank = r + delta;
                }
                return currentSum() + 0.5 * r + 0.5 * (v - ndv) + delta * (v + 0.5 * delta);
                // delta * (v + 0.5 * delta) is the area of the additional trapezoid
            }
            saveRanks();
            if (b == 1) {
                v1 = v;
                final double s = shifted ? currentSum() - (double) b * (double) v : currentSum();
                sum1 = s + 0.5 * r + 0.5 * (v - ndv);
                // sum1 is the integral (area left from) v(x) until x=r, v(x)=v1
            } else {
                assert r > 0; // else we should already return due to the check "r+b==0"
                assert b == 0;
                moveToIRank(r - 1);
                final long r1 = currentIRank();
                final long b1 = bar(currentIValue);
                assert b1 == r - r1;
                assert currentNumberOfDifferentValues() == ndv - 1;
                v1 = currentValue; // right boundary of the corresponding bar
                sum1 = currentSum() + 0.5 * r1 + 0.5 * (currentIValue - ndv + 1) + (b1 - 1) * 0.5 * (currentIValue + v1);
                // sum1 is the integral (area left from) v(x) until x=r-1, v(x)=v1
                // (b1 - 1) * 0.5 * (currentIValue + v1) is the area of the additional trapezoid
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
        final double deltaRank = (currentValue - v1) / (v2 - v1);
        final double newPreciseRank = r + b - 1 + deltaRank;
        if (needRank) {
            currentPreciseRank = newPreciseRank;
        } else {
            currentPreciseRank = savedPreciseRank;
            assert Math.abs(currentPreciseRank - newPreciseRank) <= 1.0e-3 :
                    "bug: currentPreciseRank should be " + newPreciseRank + ", but saved value is " + savedPreciseRank;
        }
        if (DEBUG_MODE) {
            checkIntegrity();
        }
        return sum1 + deltaRank * 0.5 * (v1 + currentValue);
    }

    /**
     * Equivalent to <code>{@link #nextSharing()}.{@link #currentIntegral()}
     * - thisInstance.{@link #currentIntegral()}</code>, but probably works little faster.
     *
     * @return the difference between the current simple integrals <i>S<sup>S</sup></i>, calculated
     * in two instances, sharing the same histogram <b>b</b>[<i>k</i>].
     */
    public final double currentIntegralBetweenSharing() {
        final SummingHistogram next = nextSharing();
        final long total = total();
        assert currentValue >= currentIValue - 0.5001 :
                "currentValue = " + currentValue
                        + " < currentIValue - 0.5001, currentIValue = " + currentIValue;
        assert currentValue < currentIValue + 1.0001 :
                "currentValue = " + currentValue + " >= currentIValue + 1.0 = " + (currentIValue + 1.0);
        assert next.currentValue >= next.currentIValue - 0.5001 :
                "next.currentValue = " + next.currentValue
                        + " < next.currentIValue - 0.5001, next.currentIValue = " + next.currentIValue;
        assert next.currentValue < next.currentIValue + 1.0001 :
                "next.currentValue = " + next.currentValue
                        + " >= next.currentIValue + 1.0 = " + (next.currentIValue + 1.0);
        assert total == next.total();
        if (total == 0) {
            return 0.0;
        }
        final boolean shifted1 = currentValue < currentIValue; // possible after moveToPreciseRank
        final int v1 = shifted1 ? currentIValue - 1 : currentIValue;
        final long b1 = bar(v1);
        final long r1 = shifted1 ? currentIRank() - b1 : currentIRank();
        final boolean shifted2 = next.currentValue < next.currentIValue; // possible after moveToPreciseRank
        final int v2 = shifted2 ? next.currentIValue - 1 : next.currentIValue;
        final long b2 = next.bar(v2);
        final long r2 = shifted2 ? next.currentIRank() - b2 : next.currentIRank();
        if (DEBUG_MODE) {
            if (v1 < 0)
                throw new AssertionError("Negative value" + v1);
            if (b1 < 0)
                throw new AssertionError("Negative histogram bar #" + v1);
            if (r1 < 0)
                throw new AssertionError("Negative rank " + r1 + " at bar #" + v1 + " (" + currentIValue + ") = " + b1);
            if (r1 + b1 > total)
                throw new AssertionError("Rank " + r1 + " + bar #"
                        + v1 + " (" + currentIValue + ") = " + b1 + " > total number of elements " + total);
            if (r1 + b1 == total && shifted1)
                throw new AssertionError("currentValue = " + currentValue + " < currentIValue = "
                        + currentIValue + ": it cannot at the histogram end, but rank is " + r1 + "+" + b1 + " / " + total);
            if (v2 < 0)
                throw new AssertionError("Negative value" + v2);
            if (b2 < 0)
                throw new AssertionError("Negative histogram bar #" + v2);
            if (r2 < 0)
                throw new AssertionError("Negative rank " + r2 + " at bar #" + v2 + " (" + currentIValue + ") = " + b2);
            if (r2 + b2 > total)
                throw new AssertionError("Rank " + r2 + " + bar #"
                        + v2 + " (" + currentIValue + ") = " + b2 + " > total number of elements " + total);
            if (r2 + b2 == total && shifted2)
                throw new AssertionError("currentValue = " + currentValue + " < currentIValue = "
                        + currentIValue + ": it cannot at the histogram end, but rank is " + r2 + "+" + b2 + " / " + total);
        }
        final double s1 = shifted1 ? currentSum() - (double) b1 * (double) v1 : currentSum();
        final double s2 = shifted2 ? next.currentSum() - (double) b2 * (double) v2 : next.currentSum();
        final double correction1, correction2;
        double delta;
        if (b1 == 0 || (delta = currentValue - v1) == 0.0) {
            correction1 = 0.0;
        } else {
            double indexInBar = b1 == 1 ? delta : delta * (double) b1;
            correction1 = indexInBar * (v1 + 0.5 * delta);
        }
        if (b2 == 0 || (delta = next.currentValue - v2) == 0.0) {
            correction2 = 0.0;
        } else {
            double indexInBar = b2 == 1 ? delta : delta * (double) b2;
            correction2 = indexInBar * (v2 + 0.5 * delta);
        }
        return s2 - s1 + 0.5 * (r2 - r1) + (correction2 - correction1);
    }

    /**
     * Equivalent to <code>{@link #nextSharing()}.{@link #currentPreciseIntegral()}
     * - thisInstance.{@link #currentPreciseIntegral()}</code>, but probably works little faster.
     *
     * @return the difference between the current precise integrals <i>S<sup>P</sup></i>, calculated
     * in two instances, sharing the same histogram <b>b</b>[<i>k</i>].
     */
    public final double currentPreciseIntegralBetweenSharing() {
        return nextSharing().currentPreciseIntegral() - currentPreciseIntegral();
    }

    @Override
    public abstract SummingHistogram moveToIRank(long rank);

    @Override
    public abstract SummingHistogram moveToIValue(int value);

    @Override
    public SummingHistogram moveToPreciseRank(double rank) {
        return (SummingHistogram) super.moveToPreciseRank(rank);
    }

    @Override
    public SummingHistogram moveToValue(double value) {
        return (SummingHistogram) super.moveToValue(value);
    }

    /**
     * <p>Returns the difference <i>S</i>(<code>toRank</code>)&minus;<i>S</i>(<code>fromRank</code>),
     * where <i>S</i>(<i>r</i>) is the summing function, defined in terms of
     * the simple histogram model for the histogram <b>b</b>[<i>k</i>], passed via
     * <code>histogram</code> argument.
     * In other words, this method returns the definite integral of <i>v</i>(<i>r</i>) function,
     * defined in terms of the simple histogram model,
     * between <i>r</i>=<code>fromRank</code> and <i>r</i>=<code>toRank</code>.
     * The <code>fromRank</code> argument should be not greater than <code>toRank</code>;
     * in other case this method returns 0.0.
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * <p>If <code>fromRank&lt;=toRank</code>, the result of this method is equal to the result of the following operators:
     *
     * <pre>
     * &#32;   {@link SummingHistogram} hist = {@link SummingHistogram}.{@link #newSummingLongHistogram(long[], int...)
     * newSummingLongHistogram}(histogram);
     * &#32;    double fromIntegral = hist.{@link #moveToRank(double)
     * moveToRank}(fromRank).{@link #currentIntegral() currentIntegral()};
     * &#32;    double toIntegral = hist.{@link #moveToRank(double)
     * moveToRank}(toRank).{@link #currentIntegral() currentIntegral()};
     * &#32;    double result = toIntegral - fromIntegral;
     * </pre>
     *
     * <p>but this method works little faster.
     *
     * @param histogram <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>]
     *                  is the number of elements in the source array
     *                  that are equal to <i>k</i>.
     *                  All <code>histogram[k]</code> must be non-negative; in other case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param fromRank  the start rank.
     * @param toRank    the end rank.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the simple histogram model, between <i>r</i>=<code>fromRank</code> and
     * <i>r</i>=<code>toRank</code>.
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(fromRank)</code> or <code>Double.isNaN(toRank)</code>.
     */
    public static double integralBetweenRanks(long[] histogram, double fromRank, double toRank) {
        //[[Repeat.SectionStart integralBetweenRanksImpl]]
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(fromRank))
            throw new IllegalArgumentException("Illegal fromRank argument (NaN)");
        if (Double.isNaN(toRank))
            throw new IllegalArgumentException("Illegal toRank argument (NaN)");
        if (fromRank < 0.0) {
            fromRank = 0.0;
        }
        if (toRank < 0.0) {
            toRank = 0.0;
        }
        if (fromRank >= toRank) {
            return 0.0;
        }
        final long fromR = (long) fromRank;
        final long toR = (long) toRank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "fromRank" to "fromR", "fromValue" to "fromV", etc.
        int fromV = 0;
        long acc = 0;
        long b = 0;
        for (; fromV < histogram.length; fromV++) {
            b = histogram[fromV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + fromV + "]=" + b);
            acc += b;
            if (fromR < acc) {
                break;
            }
        }
        if (fromV >= histogram.length) {
            return 0.0;
        }
        assert b > 0;
        assert fromR < acc;
        int toV = fromV + 1;
        if (toRank <= acc) { // special case: we are inside a single bar
            double middleRank = 0.5 * (fromRank + toRank);
            return (toRank - fromRank) * (toV - (acc - middleRank) / (double) b);
        }
        double sum = (acc - fromRank) * (toV - 0.5 * (acc - fromRank) / (double) b);
        for (; toV < histogram.length; toV++) {
            b = histogram[toV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + toV + "]=" + b);
            if (b > 0) {
                acc += b;
                if (toR < acc) {
                    sum += (toRank - (acc - b)) * (toV + 0.5 * (toRank - (acc - b)) / (double) b);
                    break;
                } else {
                    sum += (double) b * (toV + 0.5);
                }
            }
        }
        return sum;
        //[[Repeat.SectionEnd integralBetweenRanksImpl]]
    }

    /**
     * Precise equivalent of {@link #integralBetweenRanks(long[], double, double)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>] is the number of elements in the source array
     *                  that are equal to <i>k</i>.
     *                  All <code>histogram[k]</code> must be non-negative; in other case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param fromRank  the start rank.
     * @param toRank    the end rank.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the simple histogram model, between <i>r</i>=<code>fromRank</code> and
     * <i>r</i>=<code>toRank</code>.
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(fromRank)</code> or <code>Double.isNaN(toRank)</code>.
     */
    public static double integralBetweenRanks(int[] histogram, double fromRank, double toRank) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, integralBetweenRanksImpl)
        //        \blong\b ==> int    !! Auto-generated: NOT EDIT !! ]]
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(fromRank))
            throw new IllegalArgumentException("Illegal fromRank argument (NaN)");
        if (Double.isNaN(toRank))
            throw new IllegalArgumentException("Illegal toRank argument (NaN)");
        if (fromRank < 0.0) {
            fromRank = 0.0;
        }
        if (toRank < 0.0) {
            toRank = 0.0;
        }
        if (fromRank >= toRank) {
            return 0.0;
        }
        final int fromR = (int) fromRank;
        final int toR = (int) toRank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "fromRank" to "fromR", "fromValue" to "fromV", etc.
        int fromV = 0;
        int acc = 0;
        int b = 0;
        for (; fromV < histogram.length; fromV++) {
            b = histogram[fromV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + fromV + "]=" + b);
            acc += b;
            if (fromR < acc) {
                break;
            }
        }
        if (fromV >= histogram.length) {
            return 0.0;
        }
        assert b > 0;
        assert fromR < acc;
        int toV = fromV + 1;
        if (toRank <= acc) { // special case: we are inside a single bar
            double middleRank = 0.5 * (fromRank + toRank);
            return (toRank - fromRank) * (toV - (acc - middleRank) / (double) b);
        }
        double sum = (acc - fromRank) * (toV - 0.5 * (acc - fromRank) / (double) b);
        for (; toV < histogram.length; toV++) {
            b = histogram[toV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + toV + "]=" + b);
            if (b > 0) {
                acc += b;
                if (toR < acc) {
                    sum += (toRank - (acc - b)) * (toV + 0.5 * (toRank - (acc - b)) / (double) b);
                    break;
                } else {
                    sum += (double) b * (toV + 0.5);
                }
            }
        }
        return sum;
        //[[Repeat.IncludeEnd]]
    }

    /**
     * <p>Returns the difference <i>S</i>(<code>toRank</code>)&minus;<i>S</i>(<code>fromRank</code>),
     * where <i>S</i>(<i>r</i>) is the summing function, defined in terms of
     * the precise histogram model for the histogram <b>b</b>[<i>k</i>], passed via
     * <code>histogram</code> argument.
     * In other words, this method returns the definite integral of <i>v</i>(<i>r</i>) function,
     * defined in terms of the precise histogram model,
     * between <i>r</i>=<code>fromRank</code> and <i>r</i>=<code>toRank</code>.
     * The <code>fromRank</code> argument should be not greater than <code>toRank</code>;
     * in other case this method returns 0.0.
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * <p>If <code>fromRank&lt;=toRank</code>, the result of this method is equal to the result of the following operators:
     *
     * <pre>
     * &#32;   {@link SummingHistogram} hist = {@link SummingHistogram}.{@link #newSummingLongHistogram(long[], int...)
     * newSummingLongHistogram}(histogram);
     * &#32;    double fromIntegral = hist.{@link #moveToPreciseRank(double)
     * moveToPreciseRank}(fromRank).{@link #currentPreciseIntegral() currentPreciseIntegral()};
     * &#32;    double toIntegral = hist.{@link #moveToPreciseRank(double)
     * moveToPreciseRank}(toRank).{@link #currentPreciseIntegral() currentPreciseIntegral()};
     * &#32;    double result = toIntegral - fromIntegral;
     * </pre>
     *
     * <p>but this method works little faster.
     *
     * @param histogram <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>]
     *                  is the number of elements in the source array
     *                  that are equal to <i>k</i>.
     *                  All <code>histogram[k]</code> must be non-negative; in other case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param fromRank  the start rank.
     * @param toRank    the end rank.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the precise histogram model, between <i>r</i>=<code>fromRank</code> and
     * <i>r</i>=<code>toRank</code>.
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(fromRank)</code> or <code>Double.isNaN(toRank)</code>.
     */
    public static double preciseIntegralBetweenRanks(long[] histogram, double fromRank, double toRank) {
        double result = preciseIntegralBetweenRanksImpl(histogram, fromRank, toRank);
        if (DEBUG_MODE) {
            double fromIntegral = preciseIntegralBetweenRanksImpl(histogram, 0.0, fromRank);
            double toIntegral = preciseIntegralBetweenRanksImpl(histogram, 0.0, toRank);
            if (Math.abs((toIntegral - fromIntegral) - result) > 0.01)
                throw new AssertionError("Bug in preciseIntegralBetweenRanks(histogram, "
                        + fromRank + ", " + toRank + "): fromIntegral = " + fromIntegral
                        + ", toIntegral = " + toIntegral + ", but result = "
                        + preciseIntegralBetweenRanksImpl(histogram, fromRank, toRank)
                        + ": " + histogram.length + " bars " + JArrays.toString(histogram, ",", 2048));
        }
        return result;
    }

    /**
     * Precise equivalent of {@link #preciseIntegralBetweenRanks(long[], double, double)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>] is the number of elements in the source array
     *                  that are equal to <i>k</i>.
     *                  All <code>histogram[k]</code> must be non-negative; in other case,
     *                  <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param fromRank  the start rank.
     * @param toRank    the end rank.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the precise histogram model, between <i>r</i>=<code>fromRank</code> and
     * <i>r</i>=<code>toRank</code>.
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(fromRank)</code> or <code>Double.isNaN(toRank)</code>.
     */
    public static double preciseIntegralBetweenRanks(int[] histogram, double fromRank, double toRank) {
        double result = preciseIntegralBetweenRanksImpl(histogram, fromRank, toRank);
        if (DEBUG_MODE) {
            double fromIntegral = preciseIntegralBetweenRanksImpl(histogram, 0.0, fromRank);
            double toIntegral = preciseIntegralBetweenRanksImpl(histogram, 0.0, toRank);
            if (Math.abs((toIntegral - fromIntegral) - result) > 0.01)
                throw new AssertionError("Bug in preciseIntegralBetweenRanks(histogram, "
                        + fromRank + ", " + toRank + "): fromIntegral = " + fromIntegral
                        + ", toIntegral = " + toIntegral + ", but result = "
                        + preciseIntegralBetweenRanksImpl(histogram, fromRank, toRank)
                        + ": " + histogram.length + " bars " + JArrays.toString(histogram, ",", 2048));
        }
        return result;
    }

    //[[Repeat.SectionStart preciseIntegralBetweenRanksImpl]]
    private static double preciseIntegralBetweenRanksImpl(long[] histogram, double fromRank, double toRank) {
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(fromRank))
            throw new IllegalArgumentException("Illegal fromRank argument (NaN)");
        if (Double.isNaN(toRank))
            throw new IllegalArgumentException("Illegal toRank argument (NaN)");
        if (fromRank < 0.0) {
            fromRank = 0.0;
        }
        if (toRank < 0.0) {
            toRank = 0.0;
        }
        if (fromRank >= toRank) {
            return 0.0;
        }
        final long fromR = (long) fromRank;
        final long toR = (long) toRank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "fromRank" to "fromR", "fromValue" to "fromV", etc.
        int leftV = 0;
        long acc = 0;
        long b = 0;
        for (; leftV < histogram.length; leftV++) {
            b = histogram[leftV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + b);
            acc += b;
            if (fromR < acc) {
                break;
            }
        }
        if (leftV >= histogram.length) {
            return 0.0;
        }
        long leftR = acc - b;
        assert b > 0;
        assert leftR <= fromR; // in other words, acc - b <= fromR < acc
        assert fromRank < leftR + b;
        // We "stop" at this position, leftR/leftV, and calculate integrals from this position:
        // fromIntegral until fromRank and toIntegral until toRank.
        // fromRank is now "near" leftR, toRank can be near or far from it.
        long indexInBar = fromR - leftR;
        assert indexInBar < b;

        // Calculating fromIntegral
        final double fromIntegral;
        if (fromRank == fromR // in particular, if fromRank = leftR and b == 1
                || indexInBar < b - 1) {
            fromIntegral = b == 1 ? 0.0 :
                    (fromRank - (double) leftR) * (leftV + 0.5 * (fromRank - (double) leftR) / (double) b);
        } else {
            assert indexInBar == b - 1;
            assert fromR + 1 == acc;
            int rightV = nextNonZero(histogram, leftV + 1);
            if (rightV == -1) { // all further elements are zero
                if (toRank > acc) {
                    toRank = acc;
                }
                // it is a special case: simple trapezoid fromRank..toRank, fromValue..toValue, where
                // fromValue = leftV + (fromRank - leftR) / (double)b,
                // toValue = leftV + (toRank - leftR) / (double)b
                double middleRank = 0.5 * (fromRank + toRank);
                return (toRank - fromRank) * (leftV + (middleRank - (double) leftR) / (double) b);
            }
            double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
            double wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (leftV + 0.5 * wideTrapDeltaV);
            double leftValue = leftV + wideTrapDeltaV;
            double fromDelta = fromRank - (double) fromR;
            double partialNarrowTrap = fromDelta * (leftValue + 0.5 * fromDelta * (rightV - leftValue));
            fromIntegral = wideTrap + partialNarrowTrap;
        }

        // Calculating toIntegral
        double toIntegral = 0.0;
        if (toR >= acc) { // common case: toRank is "far" from leftR/leftV; we move rightwards until toR < acc
            int lastNonZero = Integer.MAX_VALUE;
            acc -= b; // restoring the situation before exiting the first loop
            for (; leftV < histogram.length; leftV++) {
                b = histogram[leftV];
                if (b < 0)
                    throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + b);
                if (b > 0) {
                    // Every non-empty bar v..v+1 (ranks r..r'=r+b) adds to the integral a pentagon MABCN,
                    // which is a figure from the left of ABC polygonal line:
                    //     M = (0, r),
                    //     A = (v, r),
                    //     B = (v+(b-1)/b, r+b-1), can be equal to A,
                    //     C = (v', r') (v'=v(r') is the next non-empty bar),
                    //     N = (0, r').
                    // In common case, it consists of two trapezoids from the left of AB and BC.
                    // In a special case r'=N (no non-empty bars rightwards from v), we should consider v'=v+1.
                    // Instead of direct calculation, we splits this pentagon into two parts: the trapezoid MADN,
                    //     D = (v+1,r'),
                    // and the triangle BCD.
                    // The area of MADN part is added at the loop iteration, corresponding to the value v, and
                    // the area of BCD part is added at the loop iteration, corresponding the next non-zero value v'.
                    if (leftV - lastNonZero > 1) {
                        // height of BCD is 1 (rank increment), base of the triangle CD = leftV-(lastNonZero+1)
                        toIntegral += 0.5 * (leftV - lastNonZero - 1);
                    } // else BCD triangle is degenerated: C=D
                    lastNonZero = leftV;
                    acc += b;
                    if (toR < acc) {
                        break;
                    }
                    toIntegral += (double) b * (leftV + 0.5);
                }
            }
            if (leftV >= histogram.length) { // special case toRank >= N: the last BCD triangle is also degenerated
                return toIntegral - fromIntegral;
            }
            leftR = acc - b;
        }
        assert b > 0;
        assert toR < acc;
        assert leftR <= toR; // if we performed the loop above, it means acc - b <= toR < acc
        assert toRank < leftR + b;
        indexInBar = toR - leftR;
        assert indexInBar < b;

        // Calculating toIntegral, last "appendix"
        if (toRank == toR // in particular, if toRank = leftR and b == 1
                || indexInBar < b - 1) {
            if (b > 1) {
                toIntegral += (toRank - (double) leftR) * (leftV + 0.5 * (toRank - (double) leftR) / (double) b);
            }
        } else {
            assert indexInBar == b - 1;
            assert toR + 1 == acc;
            int rightV = nextNonZero(histogram, leftV + 1);
            if (rightV == -1) { // all further elements are zero
                if (toRank > acc) {
                    toRank = acc;
                }
                // it is a special case: appendix is simple trapezoid leftR..toRank, leftV..toValue, where
                // toValue = leftV + (toRank - leftR) / (double)b
                toIntegral += (toRank - (double) leftR) * (leftV + 0.5 * (toRank - (double) leftR) / (double) b);
                return toIntegral - fromIntegral;
            }
            double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
            double wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (leftV + 0.5 * wideTrapDeltaV);
            double leftValue = leftV + wideTrapDeltaV;
            double toDelta = toRank - (double) toR;
            double partialNarrowTrap = toDelta * (leftValue + 0.5 * toDelta * (rightV - leftValue));
            toIntegral += wideTrap + partialNarrowTrap;
        }
        return toIntegral - fromIntegral;
    }
    //[[Repeat.SectionEnd preciseIntegralBetweenRanksImpl]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, preciseIntegralBetweenRanksImpl)
    //        \blong\b ==> int    !! Auto-generated: NOT EDIT !! ]]
    private static double preciseIntegralBetweenRanksImpl(int[] histogram, double fromRank, double toRank) {
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(fromRank))
            throw new IllegalArgumentException("Illegal fromRank argument (NaN)");
        if (Double.isNaN(toRank))
            throw new IllegalArgumentException("Illegal toRank argument (NaN)");
        if (fromRank < 0.0) {
            fromRank = 0.0;
        }
        if (toRank < 0.0) {
            toRank = 0.0;
        }
        if (fromRank >= toRank) {
            return 0.0;
        }
        final int fromR = (int) fromRank;
        final int toR = (int) toRank;
        // here and below we get identifiers of integer variables by "truncating" identifiers of corresponding
        // real variables to the first letter, for example, "fromRank" to "fromR", "fromValue" to "fromV", etc.
        int leftV = 0;
        int acc = 0;
        int b = 0;
        for (; leftV < histogram.length; leftV++) {
            b = histogram[leftV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + b);
            acc += b;
            if (fromR < acc) {
                break;
            }
        }
        if (leftV >= histogram.length) {
            return 0.0;
        }
        int leftR = acc - b;
        assert b > 0;
        assert leftR <= fromR; // in other words, acc - b <= fromR < acc
        assert fromRank < leftR + b;
        // We "stop" at this position, leftR/leftV, and calculate integrals from this position:
        // fromIntegral until fromRank and toIntegral until toRank.
        // fromRank is now "near" leftR, toRank can be near or far from it.
        int indexInBar = fromR - leftR;
        assert indexInBar < b;

        // Calculating fromIntegral
        final double fromIntegral;
        if (fromRank == fromR // in particular, if fromRank = leftR and b == 1
                || indexInBar < b - 1) {
            fromIntegral = b == 1 ? 0.0 :
                    (fromRank - (double) leftR) * (leftV + 0.5 * (fromRank - (double) leftR) / (double) b);
        } else {
            assert indexInBar == b - 1;
            assert fromR + 1 == acc;
            int rightV = nextNonZero(histogram, leftV + 1);
            if (rightV == -1) { // all further elements are zero
                if (toRank > acc) {
                    toRank = acc;
                }
                // it is a special case: simple trapezoid fromRank..toRank, fromValue..toValue, where
                // fromValue = leftV + (fromRank - leftR) / (double)b,
                // toValue = leftV + (toRank - leftR) / (double)b
                double middleRank = 0.5 * (fromRank + toRank);
                return (toRank - fromRank) * (leftV + (middleRank - (double) leftR) / (double) b);
            }
            double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
            double wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (leftV + 0.5 * wideTrapDeltaV);
            double leftValue = leftV + wideTrapDeltaV;
            double fromDelta = fromRank - (double) fromR;
            double partialNarrowTrap = fromDelta * (leftValue + 0.5 * fromDelta * (rightV - leftValue));
            fromIntegral = wideTrap + partialNarrowTrap;
        }

        // Calculating toIntegral
        double toIntegral = 0.0;
        if (toR >= acc) { // common case: toRank is "far" from leftR/leftV; we move rightwards until toR < acc
            int lastNonZero = Integer.MAX_VALUE;
            acc -= b; // restoring the situation before exiting the first loop
            for (; leftV < histogram.length; leftV++) {
                b = histogram[leftV];
                if (b < 0)
                    throw new IllegalArgumentException("Negative histogram[" + leftV + "]=" + b);
                if (b > 0) {
                    // Every non-empty bar v..v+1 (ranks r..r'=r+b) adds to the integral a pentagon MABCN,
                    // which is a figure from the left of ABC polygonal line:
                    //     M = (0, r),
                    //     A = (v, r),
                    //     B = (v+(b-1)/b, r+b-1), can be equal to A,
                    //     C = (v', r') (v'=v(r') is the next non-empty bar),
                    //     N = (0, r').
                    // In common case, it consists of two trapezoids from the left of AB and BC.
                    // In a special case r'=N (no non-empty bars rightwards from v), we should consider v'=v+1.
                    // Instead of direct calculation, we splits this pentagon into two parts: the trapezoid MADN,
                    //     D = (v+1,r'),
                    // and the triangle BCD.
                    // The area of MADN part is added at the loop iteration, corresponding to the value v, and
                    // the area of BCD part is added at the loop iteration, corresponding the next non-zero value v'.
                    if (leftV - lastNonZero > 1) {
                        // height of BCD is 1 (rank increment), base of the triangle CD = leftV-(lastNonZero+1)
                        toIntegral += 0.5 * (leftV - lastNonZero - 1);
                    } // else BCD triangle is degenerated: C=D
                    lastNonZero = leftV;
                    acc += b;
                    if (toR < acc) {
                        break;
                    }
                    toIntegral += (double) b * (leftV + 0.5);
                }
            }
            if (leftV >= histogram.length) { // special case toRank >= N: the last BCD triangle is also degenerated
                return toIntegral - fromIntegral;
            }
            leftR = acc - b;
        }
        assert b > 0;
        assert toR < acc;
        assert leftR <= toR; // if we performed the loop above, it means acc - b <= toR < acc
        assert toRank < leftR + b;
        indexInBar = toR - leftR;
        assert indexInBar < b;

        // Calculating toIntegral, last "appendix"
        if (toRank == toR // in particular, if toRank = leftR and b == 1
                || indexInBar < b - 1) {
            if (b > 1) {
                toIntegral += (toRank - (double) leftR) * (leftV + 0.5 * (toRank - (double) leftR) / (double) b);
            }
        } else {
            assert indexInBar == b - 1;
            assert toR + 1 == acc;
            int rightV = nextNonZero(histogram, leftV + 1);
            if (rightV == -1) { // all further elements are zero
                if (toRank > acc) {
                    toRank = acc;
                }
                // it is a special case: appendix is simple trapezoid leftR..toRank, leftV..toValue, where
                // toValue = leftV + (toRank - leftR) / (double)b
                toIntegral += (toRank - (double) leftR) * (leftV + 0.5 * (toRank - (double) leftR) / (double) b);
                return toIntegral - fromIntegral;
            }
            double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
            double wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (leftV + 0.5 * wideTrapDeltaV);
            double leftValue = leftV + wideTrapDeltaV;
            double toDelta = toRank - (double) toR;
            double partialNarrowTrap = toDelta * (leftValue + 0.5 * toDelta * (rightV - leftValue));
            toIntegral += wideTrap + partialNarrowTrap;
        }
        return toIntegral - fromIntegral;
    }
    //[[Repeat.IncludeEnd]]

    /**
     * <p>Returns the difference <i>s</i>(<code>maxValue</code>)&minus;<i>s</i>(<code>minValue</code>),
     * where <i>s</i>(<i>v</i>) is the summing function, defined in terms of
     * the simple histogram model for the histogram <b>b</b>[<i>k</i>], passed via
     * <code>histogram</code> argument.
     * In other words, this method returns the definite integral of <i>v</i>(<i>r</i>) function,
     * defined in terms of the simple histogram model,
     * between <i>r</i>=<i>r</i>(<code>minValue</code>) and
     * <i>r</i>=<i>r</i>(<code>maxValue</code>).
     * The <code>minValue</code> argument should be not greater than <code>maxValue</code>;
     * in other case this method returns 0.0.
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * <p>If <code>minValue&lt;=maxValue</code>, the result of this method is equal
     * to the result of the following operators:
     *
     * <pre>
     * &#32;   {@link SummingHistogram} hist = {@link SummingHistogram}.{@link #newSummingLongHistogram(long[], int...)
     * newSummingLongHistogram}(histogram);
     * &#32;   hist.{@link #moveToValue(double) moveToValue}(minValue);
     * &#32;   double fromRank = hist.{@link #currentRank() currentRank()};
     * &#32;   double fromIntegral = hist.{@link #currentIntegral() currentIntegral()};
     * &#32;   hist.{@link #moveToValue(double) moveToValue}(maxValue);
     * &#32;   double toRank = hist.{@link #currentRank() currentRank()};
     * &#32;   double toIntegral = hist.{@link #currentIntegral() currentIntegral()};
     * &#32;   double result = toIntegral - fromIntegral;
     * </pre>
     *
     * <p>but this method works little faster.
     *
     * <p>The <code>countOfValue</code> argument, if it is not {@code null}, is filled by this method
     * by some additional information. Namely:
     *
     * <ul>
     * <li><code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#count()
     * count()}</code> will be equal to the difference
     * <i>r</i>(<code>maxValue</code>)&minus;<i>r</i>(<code>minValue</code>),
     * where <i>r</i>(<i>v</i>) is the rank function, defined in terms of
     * of the simple histogram model, or 0.0 if <code>minValue&gt;maxValue</code>
     * (see the {@link Histogram comments to Histogram class});
     * in the code example, listed above, it will be equal to
     * <code>toRank-fromRank</code>;</li>
     *
     * <li><code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isLeftBound()
     * isLeftBound()}</code> will be <code>true</code> if <code>minValue&lt;maxValue</code> and
     * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=0 &mdash;
     * in other words, if <code>minValue..maxValue</code> range fully lies to the left
     * from the minimal element of the source array <b>A</b>[<i>k</i>];
     * the analogous information can be got by
     * <code>hist.{@link Histogram#leftFromNonZeroPart() leftFromNonZeroPart()}</code> method after
     * <code>hist.{@link #moveToValue(double) moveToValue}(maxValue)</code> call;</li>
     *
     * <li><code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isRightBound()
     * isRightBound()}</code> will be <code>true</code> if <code>minValue&lt;maxValue</code> and
     * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=<i>N</i> &mdash;
     * in other words, if <code>minValue..maxValue</code> range fully lies to the right
     * from the maximal element of the source array <b>A</b>[<i>k</i>];
     * the analogous information can be got by
     * <code>hist.{@link Histogram#rightFromNonZeroPart() rightFromNonZeroPart()}</code> method after
     * <code>hist.{@link #moveToValue(double) moveToValue}(minValue)</code> call.</li>
     * </ul>
     *
     * <p>Note: in the special case <i>N</i>=0 (all bars <b>b</b>[<i>k</i>] are zero),
     * the <code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isLeftBound()
     * isLeftBound()}</code> and
     * <code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isRightBound()
     * isRightBound()}</code> values can be any: they are not specified.
     * It is the only exception from the rules specified above.
     *
     * <p>This information, for example, allows to calculate the <i>mean</i> of all elements
     * of the source array <b>A</b>[<i>k</i>], lying in range <code>minValue..maxValue</code>,
     * with generalization to the floating-point case: it is
     * result_of_this_method/countOfValues.{@link net.algart.arrays.SummingHistogram.CountOfValues#count()
     * count()}.
     *
     * @param histogram     <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>] is the number of elements
     *                      in the source array that are equal to <i>k</i>.
     *                      All <code>histogram[k]</code> must be non-negative; in other case,
     *                      <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param minValue      the minimal value.
     * @param maxValue      the maximal value.
     * @param countOfValues some additional information filled by this method;
     *                      can be {@code null}, then will be ignored.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the simple histogram model, between <i>r</i>=<i>r</i>(<code>minValue</code>) and
     * <i>r</i>=<i>r</i>(<code>maxValue</code>).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(minValue)</code> or <code>Double.isNaN(maxValue)</code>.
     */
    public static double integralBetweenValues(
            long[] histogram,
            double minValue,
            double maxValue,
            CountOfValues countOfValues) {
        //[[Repeat.SectionStart integralBetweenValuesImpl]]
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(minValue))
            throw new IllegalArgumentException("Illegal minValue argument (NaN)");
        if (Double.isNaN(maxValue))
            throw new IllegalArgumentException("Illegal maxValue argument (NaN)");
        if (minValue < 0.0) {
            minValue = 0.0;
        }
        if (maxValue > histogram.length) {
            maxValue = histogram.length;
        }
        if (minValue >= maxValue) {
            if (countOfValues != null) {
                countOfValues.count = 0.0;
                countOfValues.leftBound = countOfValues.rightBound = false;
            }
            return 0.0;
        }
        final int minV = (int) minValue;
        final int maxV = (int) maxValue;
        assert minV <= maxV;
        assert minValue < histogram.length;
        assert minV < histogram.length;
        assert maxV <= histogram.length;
        long b = histogram[minV];
        if (b < 0)
            throw new IllegalArgumentException("Negative histogram[" + minV + "]=" + b);
        if (minV == maxV) { // special case: we are inside a single bar
            double count = b == 0 ? 0.0 : (maxValue - minValue) * (double) b;
            if (countOfValues != null) {
                countOfValues.count = count;
                countOfValues.leftBound = b == 0 && previousNonZero(histogram, minV - 1) == -1;
                countOfValues.rightBound = b == 0 && nextNonZero(histogram, maxV + 1) == -1;
            }
            return b == 0 ? 0.0 : 0.5 * (maxValue + minValue) * count;
        }
        double count = b == 0 ? 0.0 : (minV + 1 - minValue) * (double) b;
        double sum = b == 0 ? 0.0 : 0.5 * (minV + 1 + minValue) * count;
        for (int v = minV + 1; v < maxV; v++) {
            b = histogram[v];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + v + "]=" + b);
            if (b > 0) {
                count += b;
                sum += (v + 0.5) * (double) b;
            }
        }
        if (maxValue > maxV) {
            b = histogram[maxV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + maxV + "]=" + b);
            if (b > 0) {
                count += (maxValue - maxV) * (double) b;
                sum += 0.5 * (maxValue + maxV) * (maxValue - maxV) * (double) b;
            }
        }
        if (countOfValues != null) {
            countOfValues.count = count;
            countOfValues.leftBound = count <= 0.0 && previousNonZero(histogram, minV) == -1; //0..minV
            countOfValues.rightBound = count <= 0.0 && nextNonZero(histogram, maxV) == -1; //maxV..histogram.length-1
            // Not minV-1 and maxV+1: here it is possible that these bars are non-zero.
            // Really, we need to check 0..maxV-1 and minV+1..histogram.length-1, but
            // count <= 0, and it means that between minV and maxV all bars are 0.
            // Here we use "<= 0" to be on the safe side for a case of "almost 0.0"
        }
        return sum;
        //[[Repeat.SectionEnd integralBetweenValuesImpl]]
    }

    /**
     * Precise equivalent of {@link #integralBetweenValues(long[], double, double, CountOfValues)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram     <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>] is the number of elements
     *                      in the source array that are equal to <i>k</i>.
     *                      All <code>histogram[k]</code> must be non-negative; in other case,
     *                      <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param minValue      the minimal value.
     * @param maxValue      the maximal value.
     * @param countOfValues some additional information filled by this method;
     *                      can be {@code null}, then will be ignored.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the simple histogram model, between <i>r</i>=<i>r</i>(<code>minValue</code>) and
     * <i>r</i>=<i>r</i>(<code>maxValue</code>).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(minValue)</code> or <code>Double.isNaN(maxValue)</code>.
     */
    public static double integralBetweenValues(
            int[] histogram,
            double minValue,
            double maxValue,
            CountOfValues countOfValues) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, integralBetweenValuesImpl)
        //        \blong\b ==> int    !! Auto-generated: NOT EDIT !! ]]
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(minValue))
            throw new IllegalArgumentException("Illegal minValue argument (NaN)");
        if (Double.isNaN(maxValue))
            throw new IllegalArgumentException("Illegal maxValue argument (NaN)");
        if (minValue < 0.0) {
            minValue = 0.0;
        }
        if (maxValue > histogram.length) {
            maxValue = histogram.length;
        }
        if (minValue >= maxValue) {
            if (countOfValues != null) {
                countOfValues.count = 0.0;
                countOfValues.leftBound = countOfValues.rightBound = false;
            }
            return 0.0;
        }
        final int minV = (int) minValue;
        final int maxV = (int) maxValue;
        assert minV <= maxV;
        assert minValue < histogram.length;
        assert minV < histogram.length;
        assert maxV <= histogram.length;
        int b = histogram[minV];
        if (b < 0)
            throw new IllegalArgumentException("Negative histogram[" + minV + "]=" + b);
        if (minV == maxV) { // special case: we are inside a single bar
            double count = b == 0 ? 0.0 : (maxValue - minValue) * (double) b;
            if (countOfValues != null) {
                countOfValues.count = count;
                countOfValues.leftBound = b == 0 && previousNonZero(histogram, minV - 1) == -1;
                countOfValues.rightBound = b == 0 && nextNonZero(histogram, maxV + 1) == -1;
            }
            return b == 0 ? 0.0 : 0.5 * (maxValue + minValue) * count;
        }
        double count = b == 0 ? 0.0 : (minV + 1 - minValue) * (double) b;
        double sum = b == 0 ? 0.0 : 0.5 * (minV + 1 + minValue) * count;
        for (int v = minV + 1; v < maxV; v++) {
            b = histogram[v];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + v + "]=" + b);
            if (b > 0) {
                count += b;
                sum += (v + 0.5) * (double) b;
            }
        }
        if (maxValue > maxV) {
            b = histogram[maxV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + maxV + "]=" + b);
            if (b > 0) {
                count += (maxValue - maxV) * (double) b;
                sum += 0.5 * (maxValue + maxV) * (maxValue - maxV) * (double) b;
            }
        }
        if (countOfValues != null) {
            countOfValues.count = count;
            countOfValues.leftBound = count <= 0.0 && previousNonZero(histogram, minV) == -1; //0..minV
            countOfValues.rightBound = count <= 0.0 && nextNonZero(histogram, maxV) == -1; //maxV..histogram.length-1
            // Not minV-1 and maxV+1: here it is possible that these bars are non-zero.
            // Really, we need to check 0..maxV-1 and minV+1..histogram.length-1, but
            // count <= 0, and it means that between minV and maxV all bars are 0.
            // Here we use "<= 0" to be on the safe side for a case of "almost 0.0"
        }
        return sum;
        //[[Repeat.IncludeEnd]]
    }

    /**
     * <p>Returns the difference <i>s</i>(<code>maxValue</code>)&minus;<i>s</i>(<code>minValue</code>),
     * where <i>s</i>(<i>v</i>) is the summing function, defined in terms of
     * the precise histogram model for the histogram <b>b</b>[<i>k</i>], passed via
     * <code>histogram</code> argument.
     * In other words, this method returns the definite integral of <i>v</i>(<i>r</i>) function,
     * defined in terms of the precise histogram model,
     * between <i>r</i>=<i>r</i>(<code>minValue</code>) and
     * <i>r</i>=<i>r</i>(<code>maxValue</code>).
     * The <code>minValue</code> argument should be not greater than <code>maxValue</code>;
     * in other case this method returns 0.0.
     * See the {@link SummingHistogram comments to this class} for more details.
     *
     * <p>If <code>minValue&lt;=maxValue</code>, the result of this method is equal
     * to the result of the following operators:
     *
     * <pre>
     * &#32;   {@link SummingHistogram} hist = {@link SummingHistogram}.{@link #newSummingLongHistogram(long[], int...)
     * newSummingLongHistogram}(histogram);
     * &#32;   hist.{@link #moveToValue(double) moveToValue}(minValue);
     * &#32;   double fromRank = hist.{@link #currentPreciseRank() currentPreciseRank()};
     * &#32;   double fromIntegral = hist.{@link #currentPreciseIntegral() currentPreciseIntegral()};
     * &#32;   hist.{@link #moveToValue(double) moveToValue}(maxValue);
     * &#32;   double toRank = hist.{@link #currentPreciseRank() currentPreciseRank()};
     * &#32;   double toIntegral = hist.{@link #currentPreciseIntegral() currentPreciseIntegral()};
     * &#32;   double result = toIntegral - fromIntegral;
     * </pre>
     *
     * <p>but this method works little faster.
     *
     * <p>The <code>countOfValue</code> argument, if it is not {@code null}, is filled by this method
     * by some additional information. Namely:
     *
     * <ul>
     * <li><code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#count()
     * count()}</code> will be equal to the difference
     * <i>r</i>(<code>maxValue</code>)&minus;<i>r</i>(<code>minValue</code>),
     * where <i>r</i>(<i>v</i>) is the rank function, defined in terms of
     * of the precise histogram model, or 0.0 if <code>minValue&gt;maxValue</code>
     * (see the {@link Histogram comments to Histogram class});
     * in the code example, listed above, it will be equal to
     * <code>toRank-fromRank</code>;</li>
     *
     * <li><code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isLeftBound()
     * isLeftBound()}</code> will be <code>true</code> if <code>minValue&lt;maxValue</code> and
     * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=0 &mdash;
     * in other words, if <code>minValue..maxValue</code> range fully lies to the left
     * from the minimal element of the source array <b>A</b>[<i>k</i>];
     * the analogous information can be got by
     * <code>hist.{@link Histogram#leftFromNonZeroPart() leftFromNonZeroPart()}</code> method after
     * <code>hist.{@link #moveToValue(double) moveToValue}(maxValue)</code> call;</li>
     *
     * <li><code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isRightBound()
     * isRightBound()}</code> will be <code>true</code> if <code>minValue&lt;maxValue</code> and
     * <i>r</i>(<code>maxValue</code>)=<i>r</i>(<code>minValue</code>)=<i>N</i> &mdash;
     * in other words, if <code>minValue..maxValue</code> range fully lies to the right
     * from the maximal element of the source array <b>A</b>[<i>k</i>];
     * the analogous information can be got by
     * <code>hist.{@link Histogram#rightFromNonZeroPart() rightFromNonZeroPart()}</code> method after
     * <code>hist.{@link #moveToValue(double) moveToValue}(minValue)</code> call.</li>
     * </ul>
     *
     * <p>Note: in the special case <i>N</i>=0 (all bars <b>b</b>[<i>k</i>] are zero),
     * the <code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isLeftBound()
     * isLeftBound()}</code> and
     * <code>countOfValue.{@link net.algart.arrays.SummingHistogram.CountOfValues#isRightBound()
     * isRightBound()}</code> values can be any: they are not specified.
     * It is the only exception from the rules specified above.
     *
     * <p>This information, for example, allows to calculate the <i>mean</i> of all elements
     * of the source array <b>A</b>[<i>k</i>], lying in range <code>minValue..maxValue</code>,
     * with generalization to the floating-point case: it is
     * result_of_this_method/countOfValues.{@link net.algart.arrays.SummingHistogram.CountOfValues#count()
     * count()}.
     *
     * @param histogram     <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>] is the number of elements
     *                      in the source array that are equal to <i>k</i>.
     *                      All <code>histogram[k]</code> must be non-negative; in other case,
     *                      <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param minValue      the minimal value.
     * @param maxValue      the maximal value.
     * @param countOfValues some additional information filled by this method;
     *                      can be {@code null}, then will be ignored.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the precise histogram model, between <i>r</i>=<i>r</i>(<code>minValue</code>) and
     * <i>r</i>=<i>r</i>(<code>maxValue</code>).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(minValue)</code> or <code>Double.isNaN(maxValue)</code>.
     */
    public static double preciseIntegralBetweenValues(
            long[] histogram,
            double minValue,
            double maxValue,
            CountOfValues countOfValues) {
        //[[Repeat.SectionStart preciseIntegralBetweenValuesImpl]]
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(minValue))
            throw new IllegalArgumentException("Illegal minValue argument (NaN)");
        if (Double.isNaN(maxValue))
            throw new IllegalArgumentException("Illegal maxValue argument (NaN)");
        if (minValue < 0.0) {
            minValue = 0.0;
        }
        if (maxValue > histogram.length) {
            maxValue = histogram.length;
        }
        if (minValue >= maxValue) {
            if (countOfValues != null) {
                countOfValues.count = 0.0;
                countOfValues.leftBound = countOfValues.rightBound = false;
            }
            return 0.0;
        }
        final int minV = (int) minValue;
        final int maxV = (int) maxValue;
        assert minV <= maxV;
        assert minValue < histogram.length; // because minValue < maxValue
        assert minV < histogram.length;
        assert maxV <= histogram.length;

        boolean trapProcessed = false;
        int rightV = -157;
        // Processing the range minLeftV..minValue, where minLeftV is the nearest to maxV non-zero bar
        double wideTrap = Double.NaN, leftValue = Double.NaN;
        final double minCount, minIntegral;
        int minLeftV = minV;
        long b;
        do {
            b = histogram[minLeftV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + minLeftV + "]=" + b);
            if (b != 0) {
                break;
            }
            --minLeftV;
        } while (minLeftV >= 0);
        if (minLeftV == -1) {
            minCount = 0.0;
            minIntegral = 0.0;
        } else if (minV == minLeftV && b > 1 && (minValue - minV) * (double) b <= b - 1) {
            minCount = (minValue - minV) * (double) b;
            minIntegral = 0.5 * (minValue + minV) * minCount;
        } else {
            assert b > 0; // because minLeftV != -1
            rightV = nextNonZero(histogram, minV + 1);
            if (rightV == -1) { // all further elements are zero
                if (minV > minLeftV) { // special case: minValue..maxValue is inside the rightmost zero area
                    if (countOfValues != null) {
                        countOfValues.count = 0.0;
                        countOfValues.leftBound = false;
                        countOfValues.rightBound = true;
                    }
                    return 0.0;
                }
                assert b == histogram[minV];
                assert b > 0;
                if (minV == maxV) { // special case: we are inside the rightmost single bar
                    double count = (maxValue - minValue) * (double) b;
                    if (countOfValues != null) {
                        countOfValues.count = count;
                        countOfValues.leftBound = false;
                        countOfValues.rightBound = count <= 0.0; // very improbable
                    }
                    return 0.5 * (maxValue + minValue) * count;
                } else {
                    double count = (minV + 1 - minValue) * (double) b;
                    if (countOfValues != null) {
                        countOfValues.count = count;
                        countOfValues.leftBound = false;
                        countOfValues.rightBound = count <= 0.0; // very improbable
                    }
                    return 0.5 * (minV + 1 + minValue) * count;
                }
            }
            trapProcessed = true;
            double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
            wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (minLeftV + 0.5 * wideTrapDeltaV);
            leftValue = minLeftV + wideTrapDeltaV;
            assert minValue >= leftValue - 0.001;
            double deltaRank = (minValue - leftValue) / (rightV - leftValue);
            double partialNarrowTrap = 0.5 * (minValue + leftValue) * deltaRank;
            minCount = b - 1 + deltaRank;
            minIntegral = wideTrap + partialNarrowTrap;
        }

        // Processing the range maxLeftV..maxValue, where maxLeftV is the nearest to maxV non-zero bar
        final double maxCount, maxIntegral;
        int maxLeftV = maxV;
        if (maxV < histogram.length) {
            do {
                b = histogram[maxLeftV];
                if (b < 0)
                    throw new IllegalArgumentException("Negative histogram[" + maxLeftV + "]=" + b);
                if (b != 0) {
                    break;
                }
                --maxLeftV;
            } while (maxLeftV >= 0);
        }
        if (maxV == histogram.length) { // possible for maxV, but not for minV
            maxCount = 0.0;
            maxIntegral = 0.0;
        } else if (maxLeftV == -1) {
            assert minLeftV == -1; // so, we did not anything while processing minLeftV..minValue
            if (countOfValues != null) {
                countOfValues.count = 0.0;
                countOfValues.leftBound = true;
                countOfValues.rightBound = false;
            }
            return 0.0;
        } else if (maxV == maxLeftV && b > 1 && (maxValue - maxV) * (double) b <= b - 1) {
            maxCount = (maxValue - maxV) * (double) b;
            maxIntegral = 0.5 * (maxValue + maxV) * maxCount;
        } else {
            assert b > 0; // because maxLeftV != -1
            if (maxLeftV != minLeftV || !trapProcessed) { // maybe (not always) rightV was already calculated
                rightV = nextNonZero(histogram, maxV + 1);
            }
            if (rightV == -1) { // all further elements are zero
                if (maxV > maxLeftV) { // special case: no narrow trapezoid, full right bar
                    maxCount = b;
                    maxIntegral = (maxLeftV + 0.5) * (double) b;
                } else {
                    assert b == histogram[maxV];
                    assert b > 0;
                    maxCount = (maxValue - maxV) * (double) b;
                    maxIntegral = 0.5 * (maxValue + maxV) * maxCount;
                }
            } else {
                if (maxLeftV != minLeftV || !trapProcessed) { // maybe, all this was already calculated
                    double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
                    wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (maxLeftV + 0.5 * wideTrapDeltaV);
                    leftValue = maxLeftV + wideTrapDeltaV;
                }
                assert maxValue >= leftValue - 0.001;
                double deltaRank = (maxValue - leftValue) / (rightV - leftValue);
                double partialNarrowTrap = 0.5 * (maxValue + leftValue) * deltaRank;
                maxCount = b - 1 + deltaRank;
                maxIntegral = wideTrap + partialNarrowTrap;
            }
        }
        if (minLeftV == maxLeftV) {
            if (countOfValues != null) {
                countOfValues.count = maxCount - minCount;
                countOfValues.leftBound = countOfValues.count <= 0.0 && previousNonZero(histogram, minV) == -1;
                countOfValues.rightBound = countOfValues.count <= 0.0 && nextNonZero(histogram, maxV) == -1;
                // count <= 0.0 is very improbable
            }
            return maxIntegral - minIntegral;
        }
        assert minLeftV < maxLeftV;

        // Processing the range minLeftV..maxLeftV-1
        double middleCount = 0.0, middleIntegral = 0.0;
        int lastNonZero = Integer.MAX_VALUE;
        int v;
        if (minLeftV == -1) {
            assert histogram[minV] == 0; // we already know that all bars 0..minV are zero
            v = minV + 1;
        } else {
            assert histogram[minLeftV] > 0;
            v = minLeftV;
        }
        for (; v < maxLeftV; v++) {
            b = histogram[v];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + v + "]=" + b);
            if (b > 0) {
                // Every non-empty bar v..v+1 (ranks r..r'=r+b) adds to the integral a pentagon MABCN,
                // which is a figure from the left of ABC polygonal line:
                //     M = (0, r),
                //     A = (v, r),
                //     B = (v+(b-1)/b, r+b-1), can be equal to A,
                //     C = (v', r') (v'=v(r') is the next non-empty bar),
                //     N = (0, r').
                // In common case, it consists of two trapezoids from the left of AB and BC.
                // In a special case r'=N (no non-empty bars rightwards from v), we should consider v'=v+1.
                // Instead of direct calculation, we splits this pentagon into two parts: the trapezoid MADN,
                //     D = (v+1,r'),
                // and the triangle BCD.
                // The area of MADN part is added at the loop iteration, corresponding to the value v, and
                // the area of BCD part is added at the loop iteration, corresponding the next non-zero value v'.
                if (v - lastNonZero > 1) {
                    // height of BCD is 1 (rank increment), base of the triangle CD = leftV-(lastNonZero+1)
                    middleIntegral += 0.5 * (v - lastNonZero - 1);
                } // else BCD triangle is degenerated: C=D
                lastNonZero = v;
                middleCount += b;
                middleIntegral += (v + 0.5) * (double) b;
            }
        }
        if (maxLeftV < histogram.length && maxLeftV - lastNonZero > 1) { // last MADN trapezoid
            middleIntegral += 0.5 * (maxLeftV - lastNonZero - 1);
        }
        if (countOfValues != null) {
            countOfValues.count = maxCount - minCount + middleCount;
            countOfValues.leftBound = countOfValues.count <= 0.0 && previousNonZero(histogram, minV) == -1;
            countOfValues.rightBound = countOfValues.count <= 0.0 && nextNonZero(histogram, maxV) == -1;
            // count <= 0.0 is very improbable
        }
        return maxIntegral - minIntegral + middleIntegral;
        //[[Repeat.SectionEnd preciseIntegralBetweenValuesImpl]]
    }

    /**
     * Precise equivalent of {@link #preciseIntegralBetweenValues(long[], double, double, CountOfValues)} for a case
     * of <code>int[]</code> type of the histogram.
     *
     * @param histogram     <code>histogram</code>[<i>k</i>]=<b>b</b>[<i>k</i>] is the number of elements
     *                      in the source array that are equal to <i>k</i>.
     *                      All <code>histogram[k]</code> must be non-negative; in other case,
     *                      <code>IllegalArgumentException</code> can be thrown (but also can be not thrown).
     * @param minValue      the minimal value.
     * @param maxValue      the maximal value.
     * @param countOfValues some additional information filled by this method;
     *                      can be {@code null}, then will be ignored.
     * @return the definite integral of <i>v</i>(<i>r</i>) function, defined in terms of
     * the precise histogram model, between <i>r</i>=<i>r</i>(<code>minValue</code>) and
     * <i>r</i>=<i>r</i>(<code>maxValue</code>).
     * @throws NullPointerException     if <code>histogram</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>Double.isNaN(minValue)</code> or <code>Double.isNaN(maxValue)</code>.
     */
    public static double preciseIntegralBetweenValues(
            int[] histogram,
            double minValue,
            double maxValue,
            CountOfValues countOfValues) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, preciseIntegralBetweenValuesImpl)
        //        \blong\b ==> int  !! Auto-generated: NOT EDIT !! ]]
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (Double.isNaN(minValue))
            throw new IllegalArgumentException("Illegal minValue argument (NaN)");
        if (Double.isNaN(maxValue))
            throw new IllegalArgumentException("Illegal maxValue argument (NaN)");
        if (minValue < 0.0) {
            minValue = 0.0;
        }
        if (maxValue > histogram.length) {
            maxValue = histogram.length;
        }
        if (minValue >= maxValue) {
            if (countOfValues != null) {
                countOfValues.count = 0.0;
                countOfValues.leftBound = countOfValues.rightBound = false;
            }
            return 0.0;
        }
        final int minV = (int) minValue;
        final int maxV = (int) maxValue;
        assert minV <= maxV;
        assert minValue < histogram.length; // because minValue < maxValue
        assert minV < histogram.length;
        assert maxV <= histogram.length;

        boolean trapProcessed = false;
        int rightV = -157;
        // Processing the range minLeftV..minValue, where minLeftV is the nearest to maxV non-zero bar
        double wideTrap = Double.NaN, leftValue = Double.NaN;
        final double minCount, minIntegral;
        int minLeftV = minV;
        int b;
        do {
            b = histogram[minLeftV];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + minLeftV + "]=" + b);
            if (b != 0) {
                break;
            }
            --minLeftV;
        } while (minLeftV >= 0);
        if (minLeftV == -1) {
            minCount = 0.0;
            minIntegral = 0.0;
        } else if (minV == minLeftV && b > 1 && (minValue - minV) * (double) b <= b - 1) {
            minCount = (minValue - minV) * (double) b;
            minIntegral = 0.5 * (minValue + minV) * minCount;
        } else {
            assert b > 0; // because minLeftV != -1
            rightV = nextNonZero(histogram, minV + 1);
            if (rightV == -1) { // all further elements are zero
                if (minV > minLeftV) { // special case: minValue..maxValue is inside the rightmost zero area
                    if (countOfValues != null) {
                        countOfValues.count = 0.0;
                        countOfValues.leftBound = false;
                        countOfValues.rightBound = true;
                    }
                    return 0.0;
                }
                assert b == histogram[minV];
                assert b > 0;
                if (minV == maxV) { // special case: we are inside the rightmost single bar
                    double count = (maxValue - minValue) * (double) b;
                    if (countOfValues != null) {
                        countOfValues.count = count;
                        countOfValues.leftBound = false;
                        countOfValues.rightBound = count <= 0.0; // very improbable
                    }
                    return 0.5 * (maxValue + minValue) * count;
                } else {
                    double count = (minV + 1 - minValue) * (double) b;
                    if (countOfValues != null) {
                        countOfValues.count = count;
                        countOfValues.leftBound = false;
                        countOfValues.rightBound = count <= 0.0; // very improbable
                    }
                    return 0.5 * (minV + 1 + minValue) * count;
                }
            }
            trapProcessed = true;
            double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
            wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (minLeftV + 0.5 * wideTrapDeltaV);
            leftValue = minLeftV + wideTrapDeltaV;
            assert minValue >= leftValue - 0.001;
            double deltaRank = (minValue - leftValue) / (rightV - leftValue);
            double partialNarrowTrap = 0.5 * (minValue + leftValue) * deltaRank;
            minCount = b - 1 + deltaRank;
            minIntegral = wideTrap + partialNarrowTrap;
        }

        // Processing the range maxLeftV..maxValue, where maxLeftV is the nearest to maxV non-zero bar
        final double maxCount, maxIntegral;
        int maxLeftV = maxV;
        if (maxV < histogram.length) {
            do {
                b = histogram[maxLeftV];
                if (b < 0)
                    throw new IllegalArgumentException("Negative histogram[" + maxLeftV + "]=" + b);
                if (b != 0) {
                    break;
                }
                --maxLeftV;
            } while (maxLeftV >= 0);
        }
        if (maxV == histogram.length) { // possible for maxV, but not for minV
            maxCount = 0.0;
            maxIntegral = 0.0;
        } else if (maxLeftV == -1) {
            assert minLeftV == -1; // so, we did not anything while processing minLeftV..minValue
            if (countOfValues != null) {
                countOfValues.count = 0.0;
                countOfValues.leftBound = true;
                countOfValues.rightBound = false;
            }
            return 0.0;
        } else if (maxV == maxLeftV && b > 1 && (maxValue - maxV) * (double) b <= b - 1) {
            maxCount = (maxValue - maxV) * (double) b;
            maxIntegral = 0.5 * (maxValue + maxV) * maxCount;
        } else {
            assert b > 0; // because maxLeftV != -1
            if (maxLeftV != minLeftV || !trapProcessed) { // maybe (not always) rightV was already calculated
                rightV = nextNonZero(histogram, maxV + 1);
            }
            if (rightV == -1) { // all further elements are zero
                if (maxV > maxLeftV) { // special case: no narrow trapezoid, full right bar
                    maxCount = b;
                    maxIntegral = (maxLeftV + 0.5) * (double) b;
                } else {
                    assert b == histogram[maxV];
                    assert b > 0;
                    maxCount = (maxValue - maxV) * (double) b;
                    maxIntegral = 0.5 * (maxValue + maxV) * maxCount;
                }
            } else {
                if (maxLeftV != minLeftV || !trapProcessed) { // maybe, all this was already calculated
                    double wideTrapDeltaV = b == 1 ? 0.0 : (double) (b - 1) / (double) b;
                    wideTrap = b == 1 ? 0.0 : (double) (b - 1) * (maxLeftV + 0.5 * wideTrapDeltaV);
                    leftValue = maxLeftV + wideTrapDeltaV;
                }
                assert maxValue >= leftValue - 0.001;
                double deltaRank = (maxValue - leftValue) / (rightV - leftValue);
                double partialNarrowTrap = 0.5 * (maxValue + leftValue) * deltaRank;
                maxCount = b - 1 + deltaRank;
                maxIntegral = wideTrap + partialNarrowTrap;
            }
        }
        if (minLeftV == maxLeftV) {
            if (countOfValues != null) {
                countOfValues.count = maxCount - minCount;
                countOfValues.leftBound = countOfValues.count <= 0.0 && previousNonZero(histogram, minV) == -1;
                countOfValues.rightBound = countOfValues.count <= 0.0 && nextNonZero(histogram, maxV) == -1;
                // count <= 0.0 is very improbable
            }
            return maxIntegral - minIntegral;
        }
        assert minLeftV < maxLeftV;

        // Processing the range minLeftV..maxLeftV-1
        double middleCount = 0.0, middleIntegral = 0.0;
        int lastNonZero = Integer.MAX_VALUE;
        int v;
        if (minLeftV == -1) {
            assert histogram[minV] == 0; // we already know that all bars 0..minV are zero
            v = minV + 1;
        } else {
            assert histogram[minLeftV] > 0;
            v = minLeftV;
        }
        for (; v < maxLeftV; v++) {
            b = histogram[v];
            if (b < 0)
                throw new IllegalArgumentException("Negative histogram[" + v + "]=" + b);
            if (b > 0) {
                // Every non-empty bar v..v+1 (ranks r..r'=r+b) adds to the integral a pentagon MABCN,
                // which is a figure from the left of ABC polygonal line:
                //     M = (0, r),
                //     A = (v, r),
                //     B = (v+(b-1)/b, r+b-1), can be equal to A,
                //     C = (v', r') (v'=v(r') is the next non-empty bar),
                //     N = (0, r').
                // In common case, it consists of two trapezoids from the left of AB and BC.
                // In a special case r'=N (no non-empty bars rightwards from v), we should consider v'=v+1.
                // Instead of direct calculation, we splits this pentagon into two parts: the trapezoid MADN,
                //     D = (v+1,r'),
                // and the triangle BCD.
                // The area of MADN part is added at the loop iteration, corresponding to the value v, and
                // the area of BCD part is added at the loop iteration, corresponding the next non-zero value v'.
                if (v - lastNonZero > 1) {
                    // height of BCD is 1 (rank increment), base of the triangle CD = leftV-(lastNonZero+1)
                    middleIntegral += 0.5 * (v - lastNonZero - 1);
                } // else BCD triangle is degenerated: C=D
                lastNonZero = v;
                middleCount += b;
                middleIntegral += (v + 0.5) * (double) b;
            }
        }
        if (maxLeftV < histogram.length && maxLeftV - lastNonZero > 1) { // last MADN trapezoid
            middleIntegral += 0.5 * (maxLeftV - lastNonZero - 1);
        }
        if (countOfValues != null) {
            countOfValues.count = maxCount - minCount + middleCount;
            countOfValues.leftBound = countOfValues.count <= 0.0 && previousNonZero(histogram, minV) == -1;
            countOfValues.rightBound = countOfValues.count <= 0.0 && nextNonZero(histogram, maxV) == -1;
            // count <= 0.0 is very improbable
        }
        return maxIntegral - minIntegral + middleIntegral;
        //[[Repeat.IncludeEnd]]
    }


    //[[Repeat.SectionStart SummingLongHistogram]]
    static class SummingLongHistogram extends SummingHistogram {
        private final long[][] histogram;
        private final long[] histogram0;
        private final double[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[][] numbersOfDifferentValues;
        // numbersOfDifferentValues[0]=null is unused,
        // numbersOfDifferentValues[k] are numbers of non-empty 1-level bars in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private long total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private long[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private double[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private int[] currentNumberOfDifferentValues;
        // currentNumberOfDifferentValues[k] = number of non-empty bars < currentIValue & highBitMasks[k-1]
        private long[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private double[] alternativeSums; // buffer for saveRanks/restoreRanks
        private int[] alternativeNumberOfDifferentValues; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SummingLongHistogram nextSharing = this; // circular list of all objects sharing the same data
        private long shareCount = 1; // the length of the sharing list

        private SummingLongHistogram(
                long[][] histogram,
                double[][] sums, // separate line for removing by preprocessor
                int[][] numbersOfDifferentValues, // separate line for removing by preprocessor
                long total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
            this.numbersOfDifferentValues = numbersOfDifferentValues;
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
            this.currentSums = new double[this.m]; // zero-filled
            this.currentNumberOfDifferentValues = new int[this.m]; // zero-filled
            this.alternativeIRanks = new long[this.m];
            this.alternativeSums = new double[this.m];
            this.alternativeNumberOfDifferentValues = new int[this.m];
        }

        SummingLongHistogram(long[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new double[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    new int[bitLevels.length + 1][], // numbersOfDifferentValues: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new long[levelCount];
                this.sums[k] = new double[levelCount];
                this.numbersOfDifferentValues[k] = new int[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        long count = 0;
                        double sum = 0.0;
                        int numberOfDifferentValues = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (double) value * histogram[value];
                            if (histogram[value] != 0) { // numberOfDifferentValues: removed by preprocessor
                                numberOfDifferentValues++;
                            }  // numberOfDifferentValues: removed by preprocessor
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
                        this.numbersOfDifferentValues[k][i] = numberOfDifferentValues;
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
            final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            ++histogram0[value];
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                ++histogram[k][v];
                sums[k][v] += value;
                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                    ++numbersOfDifferentValues[k][v];
                } // numberOfDifferentValues: removed by preprocessor
                if (value < (currentIValue & highBitMasks[k])) {
                    ++currentIRanks[k];
                    currentSums[k] += value;
                    if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[k];
                    } // numberOfDifferentValues: removed by preprocessor
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                ++currentIRanks[0];
                currentSums[0] += value;
                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                    ++currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        ++hist.currentIRanks[k];
                        hist.currentSums[k] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++hist.currentNumberOfDifferentValues[k];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
                    if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                        ++hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
            final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                --histogram[k][v];
                sums[k][v] -= value;
                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                    --numbersOfDifferentValues[k][v];
                } // numberOfDifferentValues: removed by preprocessor
                if (value < (currentIValue & highBitMasks[k])) {
                    --currentIRanks[k];
                    currentSums[k] -= value;
                    if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                        --currentNumberOfDifferentValues[k];
                    } // numberOfDifferentValues: removed by preprocessor
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                --currentIRanks[0];
                currentSums[0] -= value;
                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                    --currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        --hist.currentIRanks[k];
                        hist.currentSums[k] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --hist.currentNumberOfDifferentValues[k];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
                    if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                        --hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        sums[k][v] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            ++nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++nextSharing.currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        sums[k][v] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                ++hist.currentIRanks[k];
                                hist.currentSums[k] += value;
                                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                    ++hist.currentNumberOfDifferentValues[k];
                                } // numberOfDifferentValues: removed by preprocessor
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        sums[k][v] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            --nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --nextSharing.currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        sums[k][v] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRanks[0];
                        currentSums[0] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                --hist.currentIRanks[k];
                                hist.currentSums[k] -= value;
                                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                    --hist.currentNumberOfDifferentValues[k];
                                } // numberOfDifferentValues: removed by preprocessor
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
        public int currentNumberOfDifferentValues() {
            return currentNumberOfDifferentValues[0];
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            double sum;
                            int numberOfDifferentValues;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                                numberOfDifferentValues = numbersOfDifferentValues[k][currentIValue];
                                currentNumberOfDifferentValues[k] -= numberOfDifferentValues;
                            } while (rank < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final long previousRank = currentIRanks[k] + b;
                            final double previousSum = currentSums[k] + sum;
                            final int previousNumberOfDifferentValues =
                                    currentNumberOfDifferentValues[k] + numberOfDifferentValues;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (double) b * (double) currentIValue);
                                currentNumberOfDifferentValues[k] = previousNumberOfDifferentValues
                                        - (k > 0 ? numbersOfDifferentValues[k][currentIValue] : b > 0 ? 1 : 0);
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
                        currentSums[0] -= (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                                currentSums[k] += sums[k][currentIValue];
                                currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final long lastRank = currentIRanks[k];
                            final double lastSum = currentSums[k];
                            final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                                currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                            } while (k > 0 && rank < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    long b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            double sum;
                            int numberOfDifferentValues;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                                numberOfDifferentValues = numbersOfDifferentValues[k][currentIValue];
                                currentNumberOfDifferentValues[k] -= numberOfDifferentValues;
                            } while (r < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final long previousRank = currentIRanks[k] + b;
                            final double previousSum = currentSums[k] + sum;
                            final int previousNumberOfDifferentValues =
                                    currentNumberOfDifferentValues[k] + numberOfDifferentValues;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (double) b * (double) currentIValue);
                                currentNumberOfDifferentValues[k] = previousNumberOfDifferentValues
                                        - (k > 0 ? numbersOfDifferentValues[k][currentIValue] : b > 0 ? 1 : 0);
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
                        currentSums[0] -= (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                                currentSums[k] += sums[k][currentIValue];
                                currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final long lastRank = currentIRanks[k];
                            final double lastSum = currentSums[k];
                            final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                                currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                            } while (k > 0 && r < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    long b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
        public SummingHistogram moveToIValue(int value) {
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
                        double sum;
                        int numberOfDifferentValues;
                        do {
                            --currentIValue;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] -= b;
                            sum = sums[k][currentIValue];
                            currentSums[k] -= sum;
                            numberOfDifferentValues = numbersOfDifferentValues[k][currentIValue];
                            currentNumberOfDifferentValues[k] -= numberOfDifferentValues;
                        } while (v < currentIValue);
                        assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                + " < 0 for value=" + value;
                        assert currentIValue == v;
                        final int previousValue = (currentIValue + 1) << level;
                        assert value < previousValue;
                        final long previousRank = currentIRanks[k] + b;
                        final double previousSum = currentSums[k] + sum;
                        final int previousNumberOfDifferentValues =
                                currentNumberOfDifferentValues[k] + numberOfDifferentValues;
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIValue = (previousValue >> level) - 1;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] = previousRank - b;
                            currentSums[k] = previousSum
                                    - (k > 0 ? sums[k][currentIValue] : (double) b * (double) currentIValue);
                            currentNumberOfDifferentValues[k] = previousNumberOfDifferentValues
                                    - (k > 0 ? numbersOfDifferentValues[k][currentIValue] : b > 0 ? 1 : 0);
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
                    currentSums[0] -= (double) b * (double) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        --currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                            currentSums[k] += sums[k][currentIValue];
                            currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                            ++currentIValue;
                        } while (v > currentIValue);
                        assert currentIRanks[k] <= total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + "> total=" + total + " for value=" + value;
                        assert currentIValue == v;
                        currentIValue <<= level;
                        assert currentIValue <= value;
                        final long lastRank = currentIRanks[k];
                        final double lastSum = currentSums[k];
                        final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                            currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                        } while (k > 0 && value >> level == currentIValue >> level);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue; j < value; j++) {
                    long b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (double) b * (double) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SummingLongHistogram result = new SummingLongHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        numbersOfDifferentValues, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SummingLongHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing long histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            System.arraycopy(currentNumberOfDifferentValues, 0, alternativeNumberOfDifferentValues, 0, m);
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void restoreRanks() {
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            if (currentNumberOfDifferentValues[0] < 0 || currentNumberOfDifferentValues[0] > Math.min(length, total))
                throw new AssertionError("Bug in " + this + ": currentNumberOfDifferentValues = "
                        + currentNumberOfDifferentValues[0] + " is out of range 0..min(" + length + "," + total + ")");
            for (int k = 0; k < m; k++) {
                int nDifferentValues = 0;
                if (k == 0) {
                    nDifferentValues = simpleCurrentNDV();
                } else {
                    for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // numberOfDifferentValues: removed...
                        nDifferentValues += histogram0[j] == 0 ? 0 : 1;
                    } // numberOfDifferentValues: removed by preprocessor
                }
                if (currentNumberOfDifferentValues[k] != nDifferentValues)
                    throw new AssertionError("Bug in " + this + ": illegal currentNumberOfDifferentValues[" + k
                            + "] = " + currentNumberOfDifferentValues[k] + " != "
                            + nDifferentValues + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // numberOfDifferentValues: removed...
                long s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
                double sum = 0.0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (double) histogram0[j] * (double) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                            + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {   // here we can suppose that histogram[m][0]==total
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        long b = histogram[k][currentIValue];
                        while (total > currentIRanks[k] + b) {
                            currentIRanks[k] += b;
                            currentSums[k] += sums[k][currentIValue];
                            currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                            ++currentIValue;
                            b = histogram[k][currentIValue];
                        }
                        assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + ">= total=" + total;
                        currentIValue <<= level;
                        final long lastRank = currentIRanks[k];
                        final double lastSum = currentSums[k];
                        final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                            currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                        } while (k > 0 && total <= currentIRanks[k] + histogram[k][currentIValue >> level]);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                assert currentIRanks[0] < total;
                do {
                    long b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    currentSums[0] += (double) b * (double) currentIValue;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                        currentSums[k] += sums[k][v];
                        currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][v];
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
                            currentSums[k] -= sums[k][v];
                            currentNumberOfDifferentValues[k] -= numbersOfDifferentValues[k][v];
                        }
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            }
        }

        // Don't using in the following method name "DifferentValues" string, to avoid removing by preprocessor
        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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
    //[[Repeat.SectionEnd SummingLongHistogram]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        SummingLongHistogram ==> SummingLong1LevelHistogram;;
    //        (dec|inc)reasingRank\: ==> ;;
    //        [ \t]*\/\/\s+multilevel\s+start.*?\/\/\s+multilevel\s+end.*?(?:\r(?!\n)|\n|\r\n) ==>   !! Auto-generated: NOT EDIT !! ]]
    static class SummingLong1LevelHistogram extends SummingHistogram {
        private final long[][] histogram;
        private final long[] histogram0;
        private final double[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[][] numbersOfDifferentValues;
        // numbersOfDifferentValues[0]=null is unused,
        // numbersOfDifferentValues[k] are numbers of non-empty 1-level bars in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private long total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private long[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private double[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private int[] currentNumberOfDifferentValues;
        // currentNumberOfDifferentValues[k] = number of non-empty bars < currentIValue & highBitMasks[k-1]
        private long[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private double[] alternativeSums; // buffer for saveRanks/restoreRanks
        private int[] alternativeNumberOfDifferentValues; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SummingLong1LevelHistogram nextSharing = this; // circular list of all objects sharing the same data
        private long shareCount = 1; // the length of the sharing list

        private SummingLong1LevelHistogram(
                long[][] histogram,
                double[][] sums, // separate line for removing by preprocessor
                int[][] numbersOfDifferentValues, // separate line for removing by preprocessor
                long total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
            this.numbersOfDifferentValues = numbersOfDifferentValues;
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
            this.currentSums = new double[this.m]; // zero-filled
            this.currentNumberOfDifferentValues = new int[this.m]; // zero-filled
            this.alternativeIRanks = new long[this.m];
            this.alternativeSums = new double[this.m];
            this.alternativeNumberOfDifferentValues = new int[this.m];
        }

        SummingLong1LevelHistogram(long[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new double[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    new int[bitLevels.length + 1][], // numbersOfDifferentValues: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new long[levelCount];
                this.sums[k] = new double[levelCount];
                this.numbersOfDifferentValues[k] = new int[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        long count = 0;
                        double sum = 0.0;
                        int numberOfDifferentValues = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (double) value * histogram[value];
                            if (histogram[value] != 0) { // numberOfDifferentValues: removed by preprocessor
                                numberOfDifferentValues++;
                            }  // numberOfDifferentValues: removed by preprocessor
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
                        this.numbersOfDifferentValues[k][i] = numberOfDifferentValues;
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
            final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            ++histogram0[value];
            if (value < currentIValue) {
                ++currentIRanks[0];
                currentSums[0] += value;
                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                    ++currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
                    if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                        ++hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
            final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            if (value < currentIValue) {
                --currentIRanks[0];
                currentSums[0] -= value;
                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                    --currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
                    if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                        --hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    if (value < currentIValue) {
                        --currentIRanks[0];
                        currentSums[0] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
        public int currentNumberOfDifferentValues() {
            return currentNumberOfDifferentValues[0];
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    long b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    long b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
        public SummingHistogram moveToIValue(int value) {
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
                    currentSums[0] -= (double) b * (double) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        --currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                for (int j = currentIValue; j < value; j++) {
                    long b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (double) b * (double) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SummingLong1LevelHistogram result = new SummingLong1LevelHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        numbersOfDifferentValues, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SummingLong1LevelHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing long histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            System.arraycopy(currentNumberOfDifferentValues, 0, alternativeNumberOfDifferentValues, 0, m);
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void restoreRanks() {
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            if (currentNumberOfDifferentValues[0] < 0 || currentNumberOfDifferentValues[0] > Math.min(length, total))
                throw new AssertionError("Bug in " + this + ": currentNumberOfDifferentValues = "
                        + currentNumberOfDifferentValues[0] + " is out of range 0..min(" + length + "," + total + ")");
            for (int k = 0; k < m; k++) {
                int nDifferentValues = 0;
                if (k == 0) {
                    nDifferentValues = simpleCurrentNDV();
                } else {
                    for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // numberOfDifferentValues: removed...
                        nDifferentValues += histogram0[j] == 0 ? 0 : 1;
                    } // numberOfDifferentValues: removed by preprocessor
                }
                if (currentNumberOfDifferentValues[k] != nDifferentValues)
                    throw new AssertionError("Bug in " + this + ": illegal currentNumberOfDifferentValues[" + k
                            + "] = " + currentNumberOfDifferentValues[k] + " != "
                            + nDifferentValues + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // numberOfDifferentValues: removed...
                long s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
                double sum = 0.0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (double) histogram0[j] * (double) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                    currentSums[0] += (double) b * (double) currentIValue;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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

        // Don't using in the following method name "DifferentValues" string, to avoid removing by preprocessor
        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        \blong\b(?!\stotal\(|\scurrentIRank\(|\sbar\(|\sshareCount\(|\[\]\sbars\(|\srank\)) ==> int;;
    //        Long.MAX_VALUE ==> Integer.MAX_VALUE;;
    //        Long ==> Int;;
    //        double(?!\)\s*\(rank|\srank|\spreciseValue|\sfrac|\scurrentSum\() ==> long;;
    //        0\.0 ==> 0   !! Auto-generated: NOT EDIT !! ]]
    static class SummingIntHistogram extends SummingHistogram {
        private final int[][] histogram;
        private final int[] histogram0;
        private final long[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[][] numbersOfDifferentValues;
        // numbersOfDifferentValues[0]=null is unused,
        // numbersOfDifferentValues[k] are numbers of non-empty 1-level bars in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private int total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private int[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private long[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private int[] currentNumberOfDifferentValues;
        // currentNumberOfDifferentValues[k] = number of non-empty bars < currentIValue & highBitMasks[k-1]
        private int[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private long[] alternativeSums; // buffer for saveRanks/restoreRanks
        private int[] alternativeNumberOfDifferentValues; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SummingIntHistogram nextSharing = this; // circular list of all objects sharing the same data
        private int shareCount = 1; // the length of the sharing list

        private SummingIntHistogram(
                int[][] histogram,
                long[][] sums, // separate line for removing by preprocessor
                int[][] numbersOfDifferentValues, // separate line for removing by preprocessor
                int total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
            this.numbersOfDifferentValues = numbersOfDifferentValues;
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
            this.currentSums = new long[this.m]; // zero-filled
            this.currentNumberOfDifferentValues = new int[this.m]; // zero-filled
            this.alternativeIRanks = new int[this.m];
            this.alternativeSums = new long[this.m];
            this.alternativeNumberOfDifferentValues = new int[this.m];
        }

        SummingIntHistogram(int[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new long[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    new int[bitLevels.length + 1][], // numbersOfDifferentValues: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new int[levelCount];
                this.sums[k] = new long[levelCount];
                this.numbersOfDifferentValues[k] = new int[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        int count = 0;
                        long sum = 0;
                        int numberOfDifferentValues = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (long) value * histogram[value];
                            if (histogram[value] != 0) { // numberOfDifferentValues: removed by preprocessor
                                numberOfDifferentValues++;
                            }  // numberOfDifferentValues: removed by preprocessor
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
                        this.numbersOfDifferentValues[k][i] = numberOfDifferentValues;
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
            final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            ++histogram0[value];
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                ++histogram[k][v];
                sums[k][v] += value;
                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                    ++numbersOfDifferentValues[k][v];
                } // numberOfDifferentValues: removed by preprocessor
                if (value < (currentIValue & highBitMasks[k])) {
                    ++currentIRanks[k];
                    currentSums[k] += value;
                    if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[k];
                    } // numberOfDifferentValues: removed by preprocessor
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                ++currentIRanks[0];
                currentSums[0] += value;
                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                    ++currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        ++hist.currentIRanks[k];
                        hist.currentSums[k] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++hist.currentNumberOfDifferentValues[k];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
                    if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                        ++hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
            final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            // multilevel start (for preprocessing)
            for (int k = m - 1; k > 0; k--) {
                int v = value >> bitLevels[k];
                --histogram[k][v];
                sums[k][v] -= value;
                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                    --numbersOfDifferentValues[k][v];
                } // numberOfDifferentValues: removed by preprocessor
                if (value < (currentIValue & highBitMasks[k])) {
                    --currentIRanks[k];
                    currentSums[k] -= value;
                    if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                        --currentNumberOfDifferentValues[k];
                    } // numberOfDifferentValues: removed by preprocessor
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                --currentIRanks[0];
                currentSums[0] -= value;
                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                    --currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        --hist.currentIRanks[k];
                        hist.currentSums[k] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --hist.currentNumberOfDifferentValues[k];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
                    if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                        --hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        sums[k][v] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            ++nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++nextSharing.currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        sums[k][v] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                ++hist.currentIRanks[k];
                                hist.currentSums[k] += value;
                                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                    ++hist.currentNumberOfDifferentValues[k];
                                } // numberOfDifferentValues: removed by preprocessor
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        sums[k][v] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            --nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --nextSharing.currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        --histogram[k][v];
                        sums[k][v] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --numbersOfDifferentValues[k][v];
                        } // numberOfDifferentValues: removed by preprocessor
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --currentNumberOfDifferentValues[k];
                            } // numberOfDifferentValues: removed by preprocessor
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRanks[0];
                        currentSums[0] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                --hist.currentIRanks[k];
                                hist.currentSums[k] -= value;
                                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                    --hist.currentNumberOfDifferentValues[k];
                                } // numberOfDifferentValues: removed by preprocessor
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
        public int currentNumberOfDifferentValues() {
            return currentNumberOfDifferentValues[0];
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            long sum;
                            int numberOfDifferentValues;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                                numberOfDifferentValues = numbersOfDifferentValues[k][currentIValue];
                                currentNumberOfDifferentValues[k] -= numberOfDifferentValues;
                            } while (rank < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final int previousRank = currentIRanks[k] + b;
                            final long previousSum = currentSums[k] + sum;
                            final int previousNumberOfDifferentValues =
                                    currentNumberOfDifferentValues[k] + numberOfDifferentValues;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (long) b * (long) currentIValue);
                                currentNumberOfDifferentValues[k] = previousNumberOfDifferentValues
                                        - (k > 0 ? numbersOfDifferentValues[k][currentIValue] : b > 0 ? 1 : 0);
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
                        currentSums[0] -= (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                                currentSums[k] += sums[k][currentIValue];
                                currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final int lastRank = currentIRanks[k];
                            final long lastSum = currentSums[k];
                            final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                                currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                            } while (k > 0 && rank < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    int b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                double frac = (double) (rank - currentIRanks[0]) / (long) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0;
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            long sum;
                            int numberOfDifferentValues;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                                numberOfDifferentValues = numbersOfDifferentValues[k][currentIValue];
                                currentNumberOfDifferentValues[k] -= numberOfDifferentValues;
                            } while (r < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final int previousRank = currentIRanks[k] + b;
                            final long previousSum = currentSums[k] + sum;
                            final int previousNumberOfDifferentValues =
                                    currentNumberOfDifferentValues[k] + numberOfDifferentValues;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (long) b * (long) currentIValue);
                                currentNumberOfDifferentValues[k] = previousNumberOfDifferentValues
                                        - (k > 0 ? numbersOfDifferentValues[k][currentIValue] : b > 0 ? 1 : 0);
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
                        currentSums[0] -= (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                                currentSums[k] += sums[k][currentIValue];
                                currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final int lastRank = currentIRanks[k];
                            final long lastSum = currentSums[k];
                            final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                                currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                            } while (k > 0 && r < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    int b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                double frac = (rank - currentIRanks[0]) / (long) b;
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public SummingHistogram moveToIValue(int value) {
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
                        long sum;
                        int numberOfDifferentValues;
                        do {
                            --currentIValue;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] -= b;
                            sum = sums[k][currentIValue];
                            currentSums[k] -= sum;
                            numberOfDifferentValues = numbersOfDifferentValues[k][currentIValue];
                            currentNumberOfDifferentValues[k] -= numberOfDifferentValues;
                        } while (v < currentIValue);
                        assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                + " < 0 for value=" + value;
                        assert currentIValue == v;
                        final int previousValue = (currentIValue + 1) << level;
                        assert value < previousValue;
                        final int previousRank = currentIRanks[k] + b;
                        final long previousSum = currentSums[k] + sum;
                        final int previousNumberOfDifferentValues =
                                currentNumberOfDifferentValues[k] + numberOfDifferentValues;
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIValue = (previousValue >> level) - 1;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] = previousRank - b;
                            currentSums[k] = previousSum
                                    - (k > 0 ? sums[k][currentIValue] : (long) b * (long) currentIValue);
                            currentNumberOfDifferentValues[k] = previousNumberOfDifferentValues
                                    - (k > 0 ? numbersOfDifferentValues[k][currentIValue] : b > 0 ? 1 : 0);
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
                    currentSums[0] -= (long) b * (long) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        --currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                            currentSums[k] += sums[k][currentIValue];
                            currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                            ++currentIValue;
                        } while (v > currentIValue);
                        assert currentIRanks[k] <= total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + "> total=" + total + " for value=" + value;
                        assert currentIValue == v;
                        currentIValue <<= level;
                        assert currentIValue <= value;
                        final int lastRank = currentIRanks[k];
                        final long lastSum = currentSums[k];
                        final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                            currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                        } while (k > 0 && value >> level == currentIValue >> level);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue; j < value; j++) {
                    int b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (long) b * (long) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SummingIntHistogram result = new SummingIntHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        numbersOfDifferentValues, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SummingIntHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing int histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            System.arraycopy(currentNumberOfDifferentValues, 0, alternativeNumberOfDifferentValues, 0, m);
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void restoreRanks() {
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            if (currentNumberOfDifferentValues[0] < 0 || currentNumberOfDifferentValues[0] > Math.min(length, total))
                throw new AssertionError("Bug in " + this + ": currentNumberOfDifferentValues = "
                        + currentNumberOfDifferentValues[0] + " is out of range 0..min(" + length + "," + total + ")");
            for (int k = 0; k < m; k++) {
                int nDifferentValues = 0;
                if (k == 0) {
                    nDifferentValues = simpleCurrentNDV();
                } else {
                    for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // numberOfDifferentValues: removed...
                        nDifferentValues += histogram0[j] == 0 ? 0 : 1;
                    } // numberOfDifferentValues: removed by preprocessor
                }
                if (currentNumberOfDifferentValues[k] != nDifferentValues)
                    throw new AssertionError("Bug in " + this + ": illegal currentNumberOfDifferentValues[" + k
                            + "] = " + currentNumberOfDifferentValues[k] + " != "
                            + nDifferentValues + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // numberOfDifferentValues: removed...
                int s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
                long sum = 0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (long) histogram0[j] * (long) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                            + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {   // here we can suppose that histogram[m][0]==total
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        int b = histogram[k][currentIValue];
                        while (total > currentIRanks[k] + b) {
                            currentIRanks[k] += b;
                            currentSums[k] += sums[k][currentIValue];
                            currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][currentIValue];
                            ++currentIValue;
                            b = histogram[k][currentIValue];
                        }
                        assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + ">= total=" + total;
                        currentIValue <<= level;
                        final int lastRank = currentIRanks[k];
                        final long lastSum = currentSums[k];
                        final int lastNumberOfDifferentValues = currentNumberOfDifferentValues[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                            currentNumberOfDifferentValues[k] = lastNumberOfDifferentValues;
                        } while (k > 0 && total <= currentIRanks[k] + histogram[k][currentIValue >> level]);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                assert currentIRanks[0] < total;
                do {
                    int b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    currentSums[0] += (long) b * (long) currentIValue;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                        currentSums[k] += sums[k][v];
                        currentNumberOfDifferentValues[k] += numbersOfDifferentValues[k][v];
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
                            currentSums[k] -= sums[k][v];
                            currentNumberOfDifferentValues[k] -= numbersOfDifferentValues[k][v];
                        }
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            }
        }

        // Don't using in the following method name "DifferentValues" string, to avoid removing by preprocessor
        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        \blong\b(?!\stotal\(|\scurrentIRank\(|\sbar\(|\sshareCount\(|\[\]\sbars\(|\srank\)) ==> int;;
    //        Long.MAX_VALUE ==> Integer.MAX_VALUE;;
    //        Long ==> Int;;
    //        double(?!\)\s*\(rank|\srank|\spreciseValue|\sfrac|\scurrentSum\() ==> long;;
    //        0\.0 ==> 0;;
    //        SummingIntHistogram ==> SummingInt1LevelHistogram;;
    //        (dec|inc)reasingRank\: ==> ;;
    //        [ \t]*\/\/\s+multilevel\s+start.*?\/\/\s+multilevel\s+end.*?(?:\r(?!\n)|\n|\r\n) ==>   !! Auto-generated: NOT EDIT !! ]]
    static class SummingInt1LevelHistogram extends SummingHistogram {
        private final int[][] histogram;
        private final int[] histogram0;
        private final long[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[][] numbersOfDifferentValues;
        // numbersOfDifferentValues[0]=null is unused,
        // numbersOfDifferentValues[k] are numbers of non-empty 1-level bars in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private int total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private int[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private long[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private int[] currentNumberOfDifferentValues;
        // currentNumberOfDifferentValues[k] = number of non-empty bars < currentIValue & highBitMasks[k-1]
        private int[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private long[] alternativeSums; // buffer for saveRanks/restoreRanks
        private int[] alternativeNumberOfDifferentValues; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SummingInt1LevelHistogram nextSharing = this; // circular list of all objects sharing the same data
        private int shareCount = 1; // the length of the sharing list

        private SummingInt1LevelHistogram(
                int[][] histogram,
                long[][] sums, // separate line for removing by preprocessor
                int[][] numbersOfDifferentValues, // separate line for removing by preprocessor
                int total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
            this.numbersOfDifferentValues = numbersOfDifferentValues;
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
            this.currentSums = new long[this.m]; // zero-filled
            this.currentNumberOfDifferentValues = new int[this.m]; // zero-filled
            this.alternativeIRanks = new int[this.m];
            this.alternativeSums = new long[this.m];
            this.alternativeNumberOfDifferentValues = new int[this.m];
        }

        SummingInt1LevelHistogram(int[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new long[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    new int[bitLevels.length + 1][], // numbersOfDifferentValues: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new int[levelCount];
                this.sums[k] = new long[levelCount];
                this.numbersOfDifferentValues[k] = new int[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        int count = 0;
                        long sum = 0;
                        int numberOfDifferentValues = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (long) value * histogram[value];
                            if (histogram[value] != 0) { // numberOfDifferentValues: removed by preprocessor
                                numberOfDifferentValues++;
                            }  // numberOfDifferentValues: removed by preprocessor
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
                        this.numbersOfDifferentValues[k][i] = numberOfDifferentValues;
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
            final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            ++histogram0[value];
            if (value < currentIValue) {
                ++currentIRanks[0];
                currentSums[0] += value;
                if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                    ++currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
                    if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                        ++hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
            final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed by preprocessor
            if (value < currentIValue) {
                --currentIRanks[0];
                currentSums[0] -= value;
                if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                    --currentNumberOfDifferentValues[0];
                } // numberOfDifferentValues: removed by preprocessor
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
                    if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                        --hist.currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    final boolean wasEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                        if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
                            if (wasEmpty) { // numberOfDifferentValues: removed by preprocessor
                                ++hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --nextSharing.currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                    final boolean becomeEmpty = histogram0[value] == 0; // numberOfDifferentValues: removed...
                    if (value < currentIValue) {
                        --currentIRanks[0];
                        currentSums[0] -= value;
                        if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    }
                    // the following loop must be an inner one for correct processing numberOfDifferentValues
                    for (SummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
                            if (becomeEmpty) { // numberOfDifferentValues: removed by preprocessor
                                --hist.currentNumberOfDifferentValues[0];
                            } // numberOfDifferentValues: removed by preprocessor
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
        public int currentNumberOfDifferentValues() {
            return currentNumberOfDifferentValues[0];
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    int b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                double frac = (double) (rank - currentIRanks[0]) / (long) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0;
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            --currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    int b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
                        if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                            ++currentNumberOfDifferentValues[0];
                        } // numberOfDifferentValues: removed by preprocessor
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
                double frac = (rank - currentIRanks[0]) / (long) b;
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public SummingHistogram moveToIValue(int value) {
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
                    currentSums[0] -= (long) b * (long) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        --currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                for (int j = currentIValue; j < value; j++) {
                    int b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (long) b * (long) j;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SummingInt1LevelHistogram result = new SummingInt1LevelHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        numbersOfDifferentValues, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SummingInt1LevelHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing int histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            System.arraycopy(currentNumberOfDifferentValues, 0, alternativeNumberOfDifferentValues, 0, m);
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void restoreRanks() {
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
            int[] tempNumberOfDifferentValues = currentNumberOfDifferentValues;
            currentNumberOfDifferentValues = alternativeNumberOfDifferentValues;
            alternativeNumberOfDifferentValues = tempNumberOfDifferentValues;
        }

        @Override
        void checkIntegrity() {
            if (currentIValue < 0 || currentIValue > length)
                throw new AssertionError("Bug in " + this + ": currentIValue = " + currentIValue
                        + " is out of range 0.." + length);
            if (currentIRanks[0] < 0 || currentIRanks[0] > total)
                throw new AssertionError("Bug in " + this + ": currentIRank = " + currentIRanks[0]
                        + " is out of range 0.." + total);
            if (currentNumberOfDifferentValues[0] < 0 || currentNumberOfDifferentValues[0] > Math.min(length, total))
                throw new AssertionError("Bug in " + this + ": currentNumberOfDifferentValues = "
                        + currentNumberOfDifferentValues[0] + " is out of range 0..min(" + length + "," + total + ")");
            for (int k = 0; k < m; k++) {
                int nDifferentValues = 0;
                if (k == 0) {
                    nDifferentValues = simpleCurrentNDV();
                } else {
                    for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // numberOfDifferentValues: removed...
                        nDifferentValues += histogram0[j] == 0 ? 0 : 1;
                    } // numberOfDifferentValues: removed by preprocessor
                }
                if (currentNumberOfDifferentValues[k] != nDifferentValues)
                    throw new AssertionError("Bug in " + this + ": illegal currentNumberOfDifferentValues[" + k
                            + "] = " + currentNumberOfDifferentValues[k] + " != "
                            + nDifferentValues + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // numberOfDifferentValues: removed...
                int s;
                if (currentIRanks[k] != (s = sumOfAndCheck(histogram[k], 0, currentIValue >> bitLevels[k])))
                    throw new AssertionError("Bug in " + this + ": illegal currentIRanks[" + k + "] = "
                            + currentIRanks[k] + " != " + s + " for " + currentIValue + ": "
                            + histogram[k].length + " bars " + JArrays.toString(histogram[k], ",", 3000));
                long sum = 0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (long) histogram0[j] * (long) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                    currentSums[0] += (long) b * (long) currentIValue;
                    if (b != 0) { // numberOfDifferentValues: removed by preprocessor
                        ++currentNumberOfDifferentValues[0];
                    } // numberOfDifferentValues: removed by preprocessor
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

        // Don't using in the following method name "DifferentValues" string, to avoid removing by preprocessor
        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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

    // The last command of the following Repeat instruction removes all lines containing the word "DifferentValues"
    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        (SummingLong) ==> Simplified$1;;
    //        ((?:\@Override\s+)?\w+\s+\w+\s+currentNumberOfDifferentValues\(\))[^\n\r]*?(\r(?!\n)|\n|\r\n).*?\} ==>
    //            $1 {$2            return simpleCurrentNDV();$2        } ;;
    //        [ \t]*[^\n\r]*DifferentValues(?!\(\)).*?(?:\r(?!\n)|\n|\r\n) ==>   !! Auto-generated: NOT EDIT !! ]]
    static class SimplifiedSummingLongHistogram extends SummingHistogram {
        private final long[][] histogram;
        private final long[] histogram0;
        private final double[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private long total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private long[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private double[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private long[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private double[] alternativeSums; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SimplifiedSummingLongHistogram nextSharing = this; // circular list of all objects sharing the same data
        private long shareCount = 1; // the length of the sharing list

        private SimplifiedSummingLongHistogram(
                long[][] histogram,
                double[][] sums, // separate line for removing by preprocessor
                long total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
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
            this.currentSums = new double[this.m]; // zero-filled
            this.alternativeIRanks = new long[this.m];
            this.alternativeSums = new double[this.m];
        }

        SimplifiedSummingLongHistogram(long[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new double[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new long[levelCount];
                this.sums[k] = new double[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        long count = 0;
                        double sum = 0.0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (double) value * histogram[value];
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
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
                sums[k][v] += value;
                if (value < (currentIValue & highBitMasks[k])) {
                    ++currentIRanks[k];
                    currentSums[k] += value;
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                ++currentIRanks[0];
                currentSums[0] += value;
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        ++hist.currentIRanks[k];
                        hist.currentSums[k] += value;
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
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
                sums[k][v] -= value;
                if (value < (currentIValue & highBitMasks[k])) {
                    --currentIRanks[k];
                    currentSums[k] -= value;
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                --currentIRanks[0];
                currentSums[0] -= value;
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        --hist.currentIRanks[k];
                        hist.currentSums[k] -= value;
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        sums[k][v] += value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            ++nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] += value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                        sums[k][v] += value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                    }
                    for (SimplifiedSummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                ++hist.currentIRanks[k];
                                hist.currentSums[k] += value;
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
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
                        sums[k][v] -= value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            --nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] -= value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                        sums[k][v] -= value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRanks[0];
                        currentSums[0] -= value;
                    }
                    for (SimplifiedSummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                --hist.currentIRanks[k];
                                hist.currentSums[k] -= value;
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
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
        public int currentNumberOfDifferentValues() {
            return simpleCurrentNDV();
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            double sum;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                            } while (rank < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final long previousRank = currentIRanks[k] + b;
                            final double previousSum = currentSums[k] + sum;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (double) b * (double) currentIValue);
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
                        currentSums[0] -= (double) b * (double) currentIValue;
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
                                currentSums[k] += sums[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final long lastRank = currentIRanks[k];
                            final double lastSum = currentSums[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                            } while (k > 0 && rank < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    long b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            double sum;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                            } while (r < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final long previousRank = currentIRanks[k] + b;
                            final double previousSum = currentSums[k] + sum;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (double) b * (double) currentIValue);
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
                        currentSums[0] -= (double) b * (double) currentIValue;
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
                                currentSums[k] += sums[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final long lastRank = currentIRanks[k];
                            final double lastSum = currentSums[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                            } while (k > 0 && r < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    long b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
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
        public SummingHistogram moveToIValue(int value) {
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
                        double sum;
                        do {
                            --currentIValue;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] -= b;
                            sum = sums[k][currentIValue];
                            currentSums[k] -= sum;
                        } while (v < currentIValue);
                        assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                + " < 0 for value=" + value;
                        assert currentIValue == v;
                        final int previousValue = (currentIValue + 1) << level;
                        assert value < previousValue;
                        final long previousRank = currentIRanks[k] + b;
                        final double previousSum = currentSums[k] + sum;
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIValue = (previousValue >> level) - 1;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] = previousRank - b;
                            currentSums[k] = previousSum
                                    - (k > 0 ? sums[k][currentIValue] : (double) b * (double) currentIValue);
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
                    currentSums[0] -= (double) b * (double) j;
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
                            currentSums[k] += sums[k][currentIValue];
                            ++currentIValue;
                        } while (v > currentIValue);
                        assert currentIRanks[k] <= total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + "> total=" + total + " for value=" + value;
                        assert currentIValue == v;
                        currentIValue <<= level;
                        assert currentIValue <= value;
                        final long lastRank = currentIRanks[k];
                        final double lastSum = currentSums[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                        } while (k > 0 && value >> level == currentIValue >> level);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue; j < value; j++) {
                    long b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (double) b * (double) j;
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SimplifiedSummingLongHistogram result = new SimplifiedSummingLongHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SimplifiedSummingLongHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SimplifiedSummingLongHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing long histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
        }

        @Override
        void restoreRanks() {
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
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
                double sum = 0.0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (double) histogram0[j] * (double) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                            + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {   // here we can suppose that histogram[m][0]==total
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        long b = histogram[k][currentIValue];
                        while (total > currentIRanks[k] + b) {
                            currentIRanks[k] += b;
                            currentSums[k] += sums[k][currentIValue];
                            ++currentIValue;
                            b = histogram[k][currentIValue];
                        }
                        assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + ">= total=" + total;
                        currentIValue <<= level;
                        final long lastRank = currentIRanks[k];
                        final double lastSum = currentSums[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                        } while (k > 0 && total <= currentIRanks[k] + histogram[k][currentIValue >> level]);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                assert currentIRanks[0] < total;
                do {
                    long b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    currentSums[0] += (double) b * (double) currentIValue;
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
                        currentSums[k] += sums[k][v];
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
                            currentSums[k] -= sums[k][v];
                        }
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            }
        }

        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        (SummingLong) ==> Simplified$1;;
    //        ((?:\@Override\s+)?\w+\s+\w+\s+currentNumberOfDifferentValues\(\))[^\n\r]*?(\r(?!\n)|\n|\r\n).*?\} ==>
    //            $1 {$2            return simpleCurrentNDV();$2        } ;;
    //        [ \t]*[^\n\r]*DifferentValues(?!\(\)).*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        SummingLongHistogram ==> SummingLong1LevelHistogram;;
    //        (dec|inc)reasingRank\: ==> ;;
    //        [ \t]*\/\/\s+multilevel\s+start.*?\/\/\s+multilevel\s+end.*?(?:\r(?!\n)|\n|\r\n) ==>   !! Auto-generated: NOT EDIT !! ]]
    static class SimplifiedSummingLong1LevelHistogram extends SummingHistogram {
        private final long[][] histogram;
        private final long[] histogram0;
        private final double[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private long total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private long[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private double[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private long[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private double[] alternativeSums; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SimplifiedSummingLong1LevelHistogram nextSharing = this; // circular list of all objects sharing the same data
        private long shareCount = 1; // the length of the sharing list

        private SimplifiedSummingLong1LevelHistogram(
                long[][] histogram,
                double[][] sums, // separate line for removing by preprocessor
                long total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
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
            this.currentSums = new double[this.m]; // zero-filled
            this.alternativeIRanks = new long[this.m];
            this.alternativeSums = new double[this.m];
        }

        SimplifiedSummingLong1LevelHistogram(long[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new double[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new long[levelCount];
                this.sums[k] = new double[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        long count = 0;
                        double sum = 0.0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (double) value * histogram[value];
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
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
                currentSums[0] += value;
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
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
                currentSums[0] -= value;
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                    }
                    for (SimplifiedSummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
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
                double currentSum1 = currentSums[0];
                double currentSum2 = nextSharing.currentSums[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        long b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                        currentSums[0] -= value;
                    }
                    for (SimplifiedSummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
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
        public int currentNumberOfDifferentValues() {
            return simpleCurrentNDV();
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (double) b * (double) currentIValue;
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    long b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (double) b * (double) currentIValue;
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    long b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (double) b * (double) currentIValue;
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
        public SummingHistogram moveToIValue(int value) {
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
                    currentSums[0] -= (double) b * (double) j;
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                for (int j = currentIValue; j < value; j++) {
                    long b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (double) b * (double) j;
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SimplifiedSummingLong1LevelHistogram result = new SimplifiedSummingLong1LevelHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SimplifiedSummingLong1LevelHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SimplifiedSummingLong1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing long histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
        }

        @Override
        void restoreRanks() {
            long[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            double[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
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
                double sum = 0.0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (double) histogram0[j] * (double) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                    currentSums[0] += (double) b * (double) currentIValue;
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

        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        (SummingLong) ==> Simplified$1;;
    //        ((?:\@Override\s+)?\w+\s+\w+\s+currentNumberOfDifferentValues\(\))[^\n\r]*?(\r(?!\n)|\n|\r\n).*?\} ==>
    //            $1 {$2            return simpleCurrentNDV();$2        } ;;
    //        [ \t]*[^\n\r]*DifferentValues(?!\(\)).*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        \blong\b(?!\stotal\(|\scurrentIRank\(|\sbar\(|\sshareCount\(|\[\]\sbars\(|\srank\)) ==> int;;
    //        Long.MAX_VALUE ==> Integer.MAX_VALUE;;
    //        Long ==> Int;;
    //        double(?!\)\s*\(rank|\srank|\spreciseValue|\sfrac|\scurrentSum\() ==> long;;
    //        0\.0 ==> 0   !! Auto-generated: NOT EDIT !! ]]
    static class SimplifiedSummingIntHistogram extends SummingHistogram {
        private final int[][] histogram;
        private final int[] histogram0;
        private final long[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private int total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private int[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private long[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private int[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private long[] alternativeSums; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SimplifiedSummingIntHistogram nextSharing = this; // circular list of all objects sharing the same data
        private int shareCount = 1; // the length of the sharing list

        private SimplifiedSummingIntHistogram(
                int[][] histogram,
                long[][] sums, // separate line for removing by preprocessor
                int total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
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
            this.currentSums = new long[this.m]; // zero-filled
            this.alternativeIRanks = new int[this.m];
            this.alternativeSums = new long[this.m];
        }

        SimplifiedSummingIntHistogram(int[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new long[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new int[levelCount];
                this.sums[k] = new long[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        int count = 0;
                        long sum = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (long) value * histogram[value];
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
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
                sums[k][v] += value;
                if (value < (currentIValue & highBitMasks[k])) {
                    ++currentIRanks[k];
                    currentSums[k] += value;
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                ++currentIRanks[0];
                currentSums[0] += value;
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        ++hist.currentIRanks[k];
                        hist.currentSums[k] += value;
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
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
                sums[k][v] -= value;
                if (value < (currentIValue & highBitMasks[k])) {
                    --currentIRanks[k];
                    currentSums[k] -= value;
                }
            }
            // multilevel end (for preprocessing)
            if (value < currentIValue) {
                --currentIRanks[0];
                currentSums[0] -= value;
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                // multilevel start (for preprocessing)
                for (int k = m - 1; k > 0; k--) {
                    if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                        --hist.currentIRanks[k];
                        hist.currentSums[k] -= value;
                    }
                }
                // multilevel end (for preprocessing)
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    ++histogram0[value];
                    // multilevel start (for preprocessing)
                    for (int k = m - 1; k > 0; k--) {
                        int v = value >> bitLevels[k];
                        ++histogram[k][v];
                        sums[k][v] += value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            ++nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] += value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                        sums[k][v] += value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            ++currentIRanks[k];
                            currentSums[k] += value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                    }
                    for (SimplifiedSummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                ++hist.currentIRanks[k];
                                hist.currentSums[k] += value;
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
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
                        sums[k][v] -= value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                        }
                        if (value < (nextSharing.currentIValue & nextSharing.highBitMasks[k])) {
                            --nextSharing.currentIRanks[k];
                            nextSharing.currentSums[k] -= value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                        sums[k][v] -= value;
                        if (value < (currentIValue & highBitMasks[k])) {
                            --currentIRanks[k];
                            currentSums[k] -= value;
                        }
                    }
                    // multilevel end (for preprocessing)
                    if (value < currentIValue) {
                        --currentIRanks[0];
                        currentSums[0] -= value;
                    }
                    for (SimplifiedSummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        // multilevel start (for preprocessing)
                        for (int k = m - 1; k > 0; k--) {
                            if (value < (hist.currentIValue & hist.highBitMasks[k])) {
                                --hist.currentIRanks[k];
                                hist.currentSums[k] -= value;
                            }
                        }
                        // multilevel end (for preprocessing)
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
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
        public int currentNumberOfDifferentValues() {
            return simpleCurrentNDV();
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            long sum;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                            } while (rank < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final int previousRank = currentIRanks[k] + b;
                            final long previousSum = currentSums[k] + sum;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (long) b * (long) currentIValue);
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
                        currentSums[0] -= (long) b * (long) currentIValue;
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
                                currentSums[k] += sums[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final int lastRank = currentIRanks[k];
                            final long lastSum = currentSums[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                            } while (k > 0 && rank < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    int b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
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
                double frac = (double) (rank - currentIRanks[0]) / (long) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0;
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                            long sum;
                            do {
                                --currentIValue;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] -= b;
                                sum = sums[k][currentIValue];
                                currentSums[k] -= sum;
                            } while (r < currentIRanks[k]);
                            assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                    + " < 0 for rank=" + rank;
                            final int previousValue = (currentIValue + 1) << level;
                            final int previousRank = currentIRanks[k] + b;
                            final long previousSum = currentSums[k] + sum;
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIValue = (previousValue >> level) - 1;
                                b = histogram[k][currentIValue];
                                currentIRanks[k] = previousRank - b;
                                currentSums[k] = previousSum
                                        - (k > 0 ? sums[k][currentIValue] : (long) b * (long) currentIValue);
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
                        currentSums[0] -= (long) b * (long) currentIValue;
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
                                currentSums[k] += sums[k][currentIValue];
                                ++currentIValue;
                                b = histogram[k][currentIValue];
                            }
                            assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                    + ">= total=" + total + " for rank=" + rank;
                            currentIValue <<= level;
                            final int lastRank = currentIRanks[k];
                            final long lastSum = currentSums[k];
                            do {
                                k--;
                                level = bitLevels[k];
                                assert k > 0 || level == 0;
                                currentIRanks[k] = lastRank;
                                currentSums[k] = lastSum;
                            } while (k > 0 && r < currentIRanks[k] + histogram[k][currentIValue >> level]);
                        }
                        assert k == 0;
                    }
                    // multilevel end (for preprocessing)
                    int b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
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
                double frac = (rank - currentIRanks[0]) / (long) b;
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public SummingHistogram moveToIValue(int value) {
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
                        long sum;
                        do {
                            --currentIValue;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] -= b;
                            sum = sums[k][currentIValue];
                            currentSums[k] -= sum;
                        } while (v < currentIValue);
                        assert currentIRanks[k] >= 0 : "currentIRanks[" + k + "]=" + currentIRanks[k]
                                + " < 0 for value=" + value;
                        assert currentIValue == v;
                        final int previousValue = (currentIValue + 1) << level;
                        assert value < previousValue;
                        final int previousRank = currentIRanks[k] + b;
                        final long previousSum = currentSums[k] + sum;
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIValue = (previousValue >> level) - 1;
                            b = histogram[k][currentIValue];
                            currentIRanks[k] = previousRank - b;
                            currentSums[k] = previousSum
                                    - (k > 0 ? sums[k][currentIValue] : (long) b * (long) currentIValue);
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
                    currentSums[0] -= (long) b * (long) j;
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
                            currentSums[k] += sums[k][currentIValue];
                            ++currentIValue;
                        } while (v > currentIValue);
                        assert currentIRanks[k] <= total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + "> total=" + total + " for value=" + value;
                        assert currentIValue == v;
                        currentIValue <<= level;
                        assert currentIValue <= value;
                        final int lastRank = currentIRanks[k];
                        final long lastSum = currentSums[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                        } while (k > 0 && value >> level == currentIValue >> level);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                for (int j = currentIValue; j < value; j++) {
                    int b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (long) b * (long) j;
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SimplifiedSummingIntHistogram result = new SimplifiedSummingIntHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SimplifiedSummingIntHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SimplifiedSummingIntHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing int histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
        }

        @Override
        void restoreRanks() {
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
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
                long sum = 0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (long) histogram0[j] * (long) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                            + histogram[k + 1][currentIValue >> bitLevels[k + 1]]) {   // here we can suppose that histogram[m][0]==total
                        k++;
                    }
                    while (k > 0) {
                        int level = bitLevels[k];
                        currentIValue >>= level;
                        int b = histogram[k][currentIValue];
                        while (total > currentIRanks[k] + b) {
                            currentIRanks[k] += b;
                            currentSums[k] += sums[k][currentIValue];
                            ++currentIValue;
                            b = histogram[k][currentIValue];
                        }
                        assert currentIRanks[k] < total : "currentIRank[" + k + "]=" + currentIRanks[k]
                                + ">= total=" + total;
                        currentIValue <<= level;
                        final int lastRank = currentIRanks[k];
                        final long lastSum = currentSums[k];
                        do {
                            k--;
                            level = bitLevels[k];
                            assert k > 0 || level == 0;
                            currentIRanks[k] = lastRank;
                            currentSums[k] = lastSum;
                        } while (k > 0 && total <= currentIRanks[k] + histogram[k][currentIValue >> level]);
                    }
                    assert k == 0;
                }
                // multilevel end (for preprocessing)
                assert currentIRanks[0] < total;
                do {
                    int b = histogram0[currentIValue];
                    currentIRanks[0] += b;
                    currentSums[0] += (long) b * (long) currentIValue;
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
                        currentSums[k] += sums[k][v];
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
                            currentSums[k] -= sums[k][v];
                        }
                        k++;
                    }
                }
                // multilevel end (for preprocessing)
            }
        }

        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SummingLongHistogram)
    //        (SummingLong) ==> Simplified$1;;
    //        ((?:\@Override\s+)?\w+\s+\w+\s+currentNumberOfDifferentValues\(\))[^\n\r]*?(\r(?!\n)|\n|\r\n).*?\} ==>
    //            $1 {$2            return simpleCurrentNDV();$2        } ;;
    //        [ \t]*[^\n\r]*DifferentValues(?!\(\)).*?(?:\r(?!\n)|\n|\r\n) ==> ;;
    //        \blong\b(?!\stotal\(|\scurrentIRank\(|\sbar\(|\sshareCount\(|\[\]\sbars\(|\srank\)) ==> int;;
    //        Long.MAX_VALUE ==> Integer.MAX_VALUE;;
    //        Long ==> Int ;;
    //        double(?!\)\s*\(rank|\srank|\spreciseValue|\sfrac|\scurrentSum\() ==> long;;
    //        0\.0 ==> 0;;
    //        SummingIntHistogram ==> SummingInt1LevelHistogram;;
    //        (dec|inc)reasingRank\: ==> ;;
    //        [ \t]*\/\/\s+multilevel\s+start.*?\/\/\s+multilevel\s+end.*?(?:\r(?!\n)|\n|\r\n) ==>   !! Auto-generated: NOT EDIT !! ]]
    static class SimplifiedSummingInt1LevelHistogram extends SummingHistogram {
        private final int[][] histogram;
        private final int[] histogram0;
        private final long[][] sums;
        // sums[0]=null is unused, sums[k] are sums of elements in k-level bars
        private final int[] bitLevels; // bitLevels[0]==0
        private final int[] highBitMasks; // highBitMasks[k] = ~((1 << bitLevels[k]) - 1)
        private final int m; // number of levels (1 for simple one-level histogram)
        private int total;
        // The fields above are shared or supported to be identical in all objects sharing the same data

        private int[] currentIRanks;
        // currentIRanks[k] = number of values < currentIValue & highBitMasks[k-1]
        private long[] currentSums;
        // currentSums[k] = sum of values < currentIValue & highBitMasks[k-1]
        private int[] alternativeIRanks; // buffer for saveRanks/restoreRanks
        private long[] alternativeSums; // buffer for saveRanks/restoreRanks
        // The fields above are unique and independent in objects sharing the same data

        private SimplifiedSummingInt1LevelHistogram nextSharing = this; // circular list of all objects sharing the same data
        private int shareCount = 1; // the length of the sharing list

        private SimplifiedSummingInt1LevelHistogram(
                int[][] histogram,
                long[][] sums, // separate line for removing by preprocessor
                int total,
                int[] bitLevels) {
            super(histogram[0].length);
            assert bitLevels != null;
            this.m = bitLevels.length + 1;
            assert histogram.length == this.m;
            assert sums.length == this.m;
            this.histogram = histogram;
            this.histogram0 = histogram[0];
            this.sums = sums;
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
            this.currentSums = new long[this.m]; // zero-filled
            this.alternativeIRanks = new int[this.m];
            this.alternativeSums = new long[this.m];
        }

        SimplifiedSummingInt1LevelHistogram(int[] histogram, int[] bitLevels, boolean histogramIsZeroFilled) {
            this(newMultilevelHistogram(histogram, bitLevels.length + 1),
                    new long[bitLevels.length + 1][], // sums: this line will be removed by preprocessor
                    histogramIsZeroFilled ? 0 : sumOfAndCheck(histogram, 0, Integer.MAX_VALUE), bitLevels);
            for (int k = 1; k < this.bitLevels.length; k++) {
                int levelLen = 1 << this.bitLevels[k];
                int levelCount = histogram.length >> this.bitLevels[k];
                if (levelCount << this.bitLevels[k] != histogram.length) {
                    levelCount++;
                }
                this.histogram[k] = new int[levelCount];
                this.sums[k] = new long[levelCount];
                if (!histogramIsZeroFilled) {
                    for (int i = 0, value = 0; i < levelCount; i++) {
                        int count = 0;
                        long sum = 0;
                        for (int max = value + Math.min(levelLen, this.length - value); value < max; value++) {
                            count += histogram[value];
                            sum += (long) value * histogram[value];
                        }
                        this.histogram[k][i] = count;
                        this.sums[k][i] = sum;
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
                currentSums[0] += value;
            }
            ++total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    ++hist.currentIRanks[0];
                    hist.currentSums[0] += value;
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
                currentSums[0] -= value;
            }
            --total;
            currentPreciseRank = Double.NaN;
            for (SimplifiedSummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                assert hist.m == m;
                assert hist.histogram == histogram;
                if (value < hist.currentIValue) {
                    --hist.currentIRanks[0];
                    hist.currentSums[0] -= value;
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRank1;
                        currentSum1 += value;
                    }
                    if (value < nextSharing.currentIValue) {
                        ++currentIRank2;
                        currentSum2 += value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
                total += values.length;
                currentPreciseRank = Double.NaN;
                nextSharing.total += values.length;
                nextSharing.currentPreciseRank = Double.NaN;
            } else {
                for (final int value : values) {
                    ++histogram0[value];
                    if (value < currentIValue) {
                        ++currentIRanks[0];
                        currentSums[0] += value;
                    }
                    for (SimplifiedSummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            ++hist.currentIRanks[0];
                            hist.currentSums[0] += value;
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
                long currentSum1 = currentSums[0];
                long currentSum2 = nextSharing.currentSums[0];
                for (int value : values) {
                    if (--histogram0[value] < 0) {
                        int b = histogram0[value];
                        histogram0[value] = 0;
                        throw new IllegalStateException("Disbalance in the histogram: negative number "
                                + b + " of occurrences of " + value + " value");
                    }
                    if (value < currentIValue) {
                        --currentIRank1;
                        currentSum1 -= value;
                    }
                    if (value < nextSharing.currentIValue) {
                        --currentIRank2;
                        currentSum2 -= value;
                    }
                }
                currentIRanks[0] = currentIRank1;
                nextSharing.currentIRanks[0] = currentIRank2;
                currentSums[0] = currentSum1;
                nextSharing.currentSums[0] = currentSum2;
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
                        currentSums[0] -= value;
                    }
                    for (SimplifiedSummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                        assert hist.m == m;
                        assert hist.histogram == histogram;
                        if (value < hist.currentIValue) {
                            --hist.currentIRanks[0];
                            hist.currentSums[0] -= value;
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
        public int currentNumberOfDifferentValues() {
            return simpleCurrentNDV();
        }

        @Override
        public long currentIRank() {
            return currentIRanks[0];
        }

        @Override
        public double currentSum() {
            return currentSums[0];
        }

        @Override
        public SummingHistogram moveToIRank(long rank) {
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (long) b * (long) currentIValue;
                    } while (rank < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    int b = histogram0[currentIValue];
                    while (rank >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
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
                double frac = (double) (rank - currentIRanks[0]) / (long) histogram0[currentIValue];
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0;
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
                // To provide the correct behaviour, we must set some "sensible" value of currentValue.
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
                        currentSums[0] -= (long) b * (long) currentIValue;
                    } while (r < currentIRanks[0]);
                    assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for rank=" + rank;
                }
            } else {

                {
                    int b = histogram0[currentIValue];
                    while (r >= currentIRanks[0] + b) {
                        currentIRanks[0] += b;
                        currentSums[0] += (long) b * (long) currentIValue;
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
                double frac = (rank - currentIRanks[0]) / (long) b;
                if (DEBUG_MODE) {
                    assert frac >= 0 && frac < 1.0001;
                }
                currentValue = currentIValue + frac;
            }
            if (DEBUG_MODE) {
                checkIntegrity();
            }
            return this;
        }

        @Override
        public SummingHistogram moveToIValue(int value) {
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
                    currentSums[0] -= (long) b * (long) j;
                }
                assert currentIRanks[0] >= 0 : "currentIRank=" + currentIRanks[0] + " < 0 for value=" + value;
            } else {
                for (int j = currentIValue; j < value; j++) {
                    int b = histogram0[j];
                    currentIRanks[0] += b;
                    currentSums[0] += (long) b * (long) j;
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
        public SummingHistogram nextSharing() {
            if (shareCount == 1)
                throw new IllegalStateException("No sharing instances");
            return nextSharing;
        }

        @Override
        public SummingHistogram share() {
            // synchronization is to be on the safe side: destroying sharing list is most undesirable danger
            synchronized (histogram0) { // histogram[0] is shared between several instances
                SimplifiedSummingInt1LevelHistogram result = new SimplifiedSummingInt1LevelHistogram(histogram,
                        sums, // separate line for removing by preprocessor
                        total, JArrays.copyOfRange(bitLevels, 1, m));
                SimplifiedSummingInt1LevelHistogram last = this;
                int count = 1;
                while (last.nextSharing != this) {
                    last = last.nextSharing;
                    count++;
                }
                assert count == shareCount;
                last.nextSharing = result;
                result.nextSharing = this;
                shareCount = count + 1;
                for (SimplifiedSummingInt1LevelHistogram hist = nextSharing; hist != this; hist = hist.nextSharing) {
                    hist.shareCount = count + 1;
                }
                return result;
            }
        }

        @Override
        public String toString() {
            return "summing int histogram with " + length + " bars and " + m + " bit level"
                    + (m == 1 ? "" : "s {" + JArrays.toString(bitLevels, ",", 100) + "}")
                    + ", current value " + currentIValue + " (precise " + currentValue + ")"
                    + ", current rank " + currentIRanks[0] + " (precise "
                    + (Double.isNaN(currentPreciseRank) ? "unknown" : currentPreciseRank) + ")"
                    + (shareCount == 1 ? "" : ", shared between " + shareCount + " instances");
        }

        @Override
        void saveRanks() {
            System.arraycopy(currentIRanks, 0, alternativeIRanks, 0, m);
            System.arraycopy(currentSums, 0, alternativeSums, 0, m);
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
        }

        @Override
        void restoreRanks() {
            int[] tempRanks = currentIRanks;
            currentIRanks = alternativeIRanks;
            alternativeIRanks = tempRanks;
            long[] tempSums = currentSums;
            currentSums = alternativeSums;
            alternativeSums = tempSums;
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
                long sum = 0;
                for (int j = 0; j < (currentIValue & highBitMasks[k]); j++) { // sum: removed by preprocessor
                    sum += (long) histogram0[j] * (long) j;
                } // sum: removed by preprocessor
                if (currentSums[k] != sum)
                    throw new AssertionError("Bug in " + this + ": illegal currentSums[" + k
                            + "] = " + currentSums[k] + " != "
                            + sum + " for " + currentIValue + ": " + histogram[k].length + " bars "
                            + JArrays.toString(histogram[k], ",", 3000)); // sum: removed by preprocessor
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
                    currentSums[0] += (long) b * (long) currentIValue;
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

        private int simpleCurrentNDV() { // NDV: removed from Histogram.java by preprocessor
            int result = 0; // NDV: removed from Histogram.java by preprocessor
            for (int j = 0; j < currentIValue; j++) { // NDV: removed from Histogram.java by preprocessor
                if (histogram0[j] != 0) { // NDV: removed from Histogram.java by preprocessor
                    result++; // NDV: removed from Histogram.java by preprocessor
                } // NDV: removed from Histogram.java by preprocessor
            } // NDV: removed from Histogram.java by preprocessor
            return result; // NDV: removed from Histogram.java by preprocessor
        } // NDV: removed from Histogram.java by preprocessor

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
