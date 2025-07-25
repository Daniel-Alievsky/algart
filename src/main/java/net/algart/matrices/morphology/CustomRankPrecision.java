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

package net.algart.matrices.morphology;

/**
 * <p>Complete description of precision characteristics of rank operations, described in {@link RankMorphology}
 * interface. Usually this interface is used for instantiating {@link BasicRankMorphology} class
 * by {@link BasicRankMorphology#getInstance} method.</p>
 *
 * <p>This package offers {@link RankPrecision} class, providing a ready set of instances of this interface,
 * enough for most situations. If you need another precision parameters, not listed in that class,
 * you can implement this interface yourself.</p>
 *
 * <p>The classes, implementing this interface, are <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public interface CustomRankPrecision {
    /**
     * The maximal possible number of analyzed bits: {@value}.
     * The bit levels, returned by {@link #bitLevels()} method,
     * must not be greater than this value; in another case, an attempt to create an instance
     * of {@link BasicRankMorphology} will lead to <code>IllegalArgumentException</code>.
     */
    int MAX_NUMBER_OF_ANALYZED_BITS = 30;
    // must be in 1..30 range; 31 or greater values can lead to problems in RankOperationProcessor inheritors

    /**
     * The <i>bit levels</i>. (Here and below we shall designate this array as <code>bitLevels</code>.)
     *
     * <p>The last element of this array <code>bitLevels[bitLevels.length-1]</code> is named
     * <i>the number of analyzed bits</i> and specifies the logarithm of the length
     * of the histogram, used while calculating rank characteristics. More precisely, the length
     * of the histogram is <i>M</i>=2<sup>&mu;</sup>,
     * where &mu; = <code>bitLevels[bitLevels.length-1]</code>
     * for floating-point matrix elements or
     * &mu; = min(<code>bitLevels[bitLevels.length-1]</code>, &beta;)
     * for fixed-point matrix elements,
     * &beta; = <b>M</b>.{@link net.algart.arrays.Matrix#array()
     * array()}.{@link net.algart.arrays.PArray#bitsPerElement() bitsPerElement()}.
     * See more details in comments to {@link RankMorphology} interface, section 3.
     *
     * <p>First <code>bitLevels.length-1</code> elements of this array, i.e.
     * <code>{@link net.algart.arrays.JArrays#copyOfRange(int[], int, int)
     * JArrays.copyOfRange}(bitLevels,0,bitLevels.length-1)</code>,
     * are passed as <code>bitLevelsOfPyramid</code> argument of
     * {@link net.algart.arrays.Histogram#newIntHistogram(int, int...) Histogram.newIntHistogram}
     * or {@link net.algart.arrays.SummingHistogram#newSummingIntHistogram(int, int...)
     * SummingHistogram.newSummingIntHistogram} methods, when they are called for creating objects,
     * which really calculate the rank characteristics, described in {@link RankMorphology} interface.
     * In other words, first <code>bitLevels.length-1</code> elements describe the levels of the pyramid
     * of histograms: it is necessary for efficient processing large histograms,
     * consisting of thousands or millions bars.
     *
     * <p>This array must not be empty and must not contain more than 31 elements,
     * and all its elements must be sorted in strictly increasing order:
     * <code>bitLevels</code>[<i>k</i>]&lt;<code>bitLevels</code>[<i>k</i>+1] for all <i>k</i>.
     * The elements of this array must not exceed {@link #MAX_NUMBER_OF_ANALYZED_BITS} limit.
     *
     * <p>Below are possible examples of the array, returned by this method:
     *
     * <ul>
     * <li>{8} &mdash; the rank operations will be performed with a simple histogram, consisting of 256 bars;
     * this precision is enough for <code>byte</code> matrices, but usually too low for
     * <code>short</code>, <code>int</code>
     * or floating-point matrices;</li>

     * <li>{6, 12} &mdash; the rank operations will be performed with a two-level pyramid of histograms,
     * consisting of 4096 bars (or min(4096,2<sup>&beta;</sup>)=256 for <code>byte</code> matrices),
     * which will be grouped by 64 bars into "wide" bars of the 2nd level;
     * such a precision is enough for many application;</li>

     * <li>{8, 16, 24} &mdash; the rank operations will be performed with a three-level pyramid of histograms,
     * consisting of 2<sup>24</sup>=16777216 bars (or min(2<sup>24</sup>,2<sup>&beta;</sup>)=256 or 65636
     * for <code>byte</code> or <code>short</code>/<code>char</code> matrices),
     * which will be grouped by 256 bars into "wide" bars of the 2nd level and by 65536 bars into "wide" bars
     * of the 3rd levels; this precision is good for most applications;</li>
     *
     * <li>{12, 24} &mdash; the rank operations will be performed with a two-level pyramid of histograms,
     * consisting of 2<sup>24</sup>=16777216 bars (or min(2<sup>24</sup>,2<sup>&beta;</sup>)=256 or 65636
     * for <code>byte</code> or <code>short</code>/<code>char</code> matrices),
     * which will be grouped by 4096 bars into "wide" bars of the 2nd level;
     * this precision is equivalent to the previous example, but usually provides better performance.</li>
     * </ul>
     *
     * <p>Note that the situation, when some or even all elements of this array are greater than 2<sup>&beta;</sup>,
     * is not an error &mdash; it just will lead to unjustified slowing down of calculations,
     * because some levels of the pyramid of histograms will contain only 1 "wide" bar.
     *
     * @return bit levels of {@link net.algart.arrays.Histogram#newIntHistogram(int, int...)
     *         the pyramid of histograms} (all elements excepting the last one) and
     *         the maximal possible total number of analyzed bits &mu;=log<sub>2</sub>(histogram length)
     *         (the last element of this array).
     * @see #numberOfAnalyzedBits()
     */
    int[] bitLevels();

    /**
     * Returns the last element of the {@link #bitLevels()} array.
     *
     * @return the maximal possible total number of analyzed bits &mu;=log<sub>2</sub>(histogram length).
     */
    int numberOfAnalyzedBits();

    /**
     * Selects the histogram model used while calculating rank characteristics:
     * <code>true</code> means the precise histogram model, <code>false</code> means the simple histogram model.
     * See comments to {@link net.algart.arrays.Histogram} and {@link net.algart.arrays.SummingHistogram} classes
     * about these models.
     *
     * @return whether the rank operations should be performed in the precise histogram model.
     */
    boolean interpolated();
}
