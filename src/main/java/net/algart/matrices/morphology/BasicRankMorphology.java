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

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.StreamingApertureProcessor;

import java.util.Objects;

/**
 * <p>Almost complete implementation of {@link RankMorphology} interface with an instantiation method
 * of some complete implementation.</p>
 *
 * <p>This class fully implements all methods, declared in {@link RankMorphology} interface,
 * according to the detailed specifications listed in the comments to that interface.
 * The only methods, which stay abstract here, are the following 3 methods of {@link AbstractMorphology}
 * superclass:</p>
 *
 * <ul>
 * <li>{@link #context(ArrayContext newContext)},</li>
 * <li>{@link #asDilationOrErosion(Matrix src, Pattern pattern, boolean isDilation)},</li>
 * <li>{@link
 * #dilationOrErosion(Matrix dest, Matrix src, Pattern pattern, boolean isDilation, boolean disableMemoryAllocation)}.
 * </li>
 * </ul>
 *
 * <p>All other methods are implemented via the simple calls of
 * {@link StreamingApertureProcessor#asProcessed(Class, Matrix, java.util.List, Pattern) asProcessed}
 * and {@link StreamingApertureProcessor#process(Matrix, Matrix, java.util.List, Pattern) process}
 * method of the corresponding streaming aperture processors, listed in {@link RankProcessors} class.</p>
 *
 * <p>This package provides some concrete complete inheritor, which implements also these 3 methods.
 * This inheritor is instantiated by the following method:</p>
 *
 * <ul>
 * <li>{@link #getInstance(ArrayContext, double, CustomRankPrecision)}.</li>
 * </ul>
 *
 * <p>But you can also inherit this class yourself and implement dilation and erosion (the basic operations
 * of the mathematical morphology) in other way.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class BasicRankMorphology extends AbstractRankMorphology implements RankMorphology {
    final boolean interpolated;
    final int[] bitLevels; // we never change them, so the standard cloning scheme is suitable

    BasicRankMorphology(ArrayContext context, boolean interpolated, int[] bitLevels) {
        super(context);
        Objects.requireNonNull(bitLevels, "Null bitLevels argument");
        this.bitLevels = bitLevels.clone();
        this.interpolated = interpolated;
        RankProcessors.getPercentiler(null, interpolated, this.bitLevels); // checking bitLevels
    }

    /**
     * Returns new instance of some inheritor of this class, implementing dilation and erosion operations
     * via the percentiles. Namely, in the created object
     * <code>{@link #dilation(Matrix, Pattern) dilation}(m,pattern)</code> method is equivalent to
     * <code>{@link #percentile(Matrix, double, Pattern)
     * percentile}(m,dilationLevel*<i>N</i>,pattern)</code> and
     * <code>{@link #erosion(Matrix, Pattern) erosion}(m,pattern)</code> method is equivalent to
     * <code>{@link #percentile(Matrix, double, Pattern)
     * percentile}(m,dilationLevel*<i>N</i>,pattern.{@link Pattern#symmetric() symmetric()})</code>,
     * where <code><i>N</i>=pattern.{@link Pattern#pointCount() pointCount()}-1</code>
     * and <code>dilationLevel</code> is the argument of this method. This argument must be in range
     * <code>0.0 &le; dilationLevel &le; 1.0</code>.
     *
     * <p>More precisely, in the created object the methods
     * <ol>
     * <li>{@link #asDilationOrErosion(Matrix src, Pattern pattern, boolean isDilation)} and</li>
     * <li>{@link
     * #dilationOrErosion(Matrix dest, Matrix src, Pattern pattern, boolean isDilation, boolean disableMemoryAllocation)}
     * </li>
     * </ol>
     * <p>work in the following way.
     *
     * <p>Let the double value <i>r</i> be
     * <i>r</i>=<code>dilationLevel*(pattern.{@link Pattern#pointCount() pointCount()}-1)</code>
     * in a case of dilation (<code>isDilation</code> argument is <code>true</code>) or
     * <i>r</i>=<code>(1.0-dilationLevel)*(pattern.{@link Pattern#pointCount() pointCount()}-1)</code>
     * in a case of erosion (<code>isDilation</code> argument is <code>false</code>).
     * Then, let <code>index</code> be:
     * <ul>
     * <li>this double value <code>index</code>=<i>r</i>, if the element type of
     * the source matrix <code>src</code> is <code>float</code> or <code>double</code> <b>or</b>
     * if we are using the precise histogram model
     * (<code>precision.{@link CustomRankPrecision#interpolated() interpolated()}</code> is <code>true</code>);</li>
     * <li>or the rounded (<code>long</code>) integer value <code>index=Math.round(</code><i>r</i><code>)</code>,
     * if the source matrix is fixed-point <b>and</b> we are using the simple histogram model
     * (<code>precision.{@link CustomRankPrecision#interpolated() interpolated()}</code> is <code>false</code>).</li>
     * </ul>
     *
     * <p>At last, let <b>P</b> be <code>pattern</code>
     * in a case of dilation (<code>isDilation</code> argument is <code>true</code>)
     * or <code>pattern.{@link Pattern#symmetric() symmetric()}</code>
     * in a case of erosion (<code>isDilation</code> argument is <code>false</code>).
     *
     * <p>Then, in the returned object the 1st method
     * {@link #asDilationOrErosion(Matrix, Pattern, boolean) asDilationOrErosion}
     * is equivalent to
     * <code>{@link #asPercentile(Matrix, double, Pattern) asPercentile}(src,index,<b>P</b>)</code>
     * and the 2nd method {@link #dilationOrErosion(Matrix, Matrix, Pattern, boolean, boolean) dilationOrErosion}
     * is equivalent to
     * <code>{@link #percentile(Matrix, Matrix, double, Pattern) percentile}(dest,src,index,<b>P</b>)</code>.
     * (The <code>disableMemoryAllocation</code> argument of
     * {@link #dilationOrErosion(Matrix, Matrix, Pattern, boolean, boolean) dilationOrErosion} method
     * is ignored by this implementation.)
     *
     * <p>Please note: using <code>Math.round</code> for rounding the percentile index does not correspond
     * to the standard behavior of integer percentiles, which are rounded, in a case of the precise histogram model,
     * according more complicated rules (see comments to
     * {@link RankMorphology#percentile(Matrix, Matrix, Pattern)} and
     * {@link Histogram#iPreciseValue(long[], double)} methods).
     * So, this method does not try to round the real percentile index <i>r</i> in the precise histogram model.
     * But in the simple histogram model, when the element type is fixed-point and some form of rounding is required
     * in any case, this method calls <code>Math.round</code> &mdash; it provides
     * better "symmetry" between dilation and erosion operations.
     *
     * <p>The precise behavior of all methods of {@link RankMorphology} interface in the returned object
     * depends on <code>precision</code> object: see comments to {@link CustomRankPrecision} and
     * {@link RankMorphology}.
     *
     * @param context       the {@link #context() context} that will be used by this object;
     *                      can be {@code null}, then it will be ignored.
     * @param dilationLevel the level: 1.0 means strict dilation and erosion as described in {@link Morphology}
     *                      interface, 0.5 means median, 0.0 means that dilation works like erosion and erosion
     *                      works like dilation (but with a {@link Pattern#symmetric() symmetric} pattern).
     * @param precision     precision characteristics of all rank operations, performed by the created object.
     * @return new instance of this class.
     * @throws NullPointerException     if <code>precision</code> argument is {@code null}
     * @throws IllegalArgumentException if the <code>dilationLevel</code> argument
     *                                  is out of <code>0.0..1.0</code> range
     *                                  of if <code>bitLevels=precision.{@link CustomRankPrecision#bitLevels()
     *                                  bitLevels()}</code> are incorrect:
     *                                  <code>bitLevels.length==0</code>, or if <code>bitLevels.length&gt;31</code>,
     *                                  or if some of the elements <code>bitLevels</code> is not in 1..30 range, or if
     *                                  <code>bitLevels</code>[<i>k</i>]&gt;=<code>bitLevels</code>[<i>k</i>+1]
     *                                  for some&nbsp;<i>k</i>.
     */
    public static RankMorphology getInstance(
            ArrayContext context,
            double dilationLevel,
            CustomRankPrecision precision) {
        return new FixedPercentileRankMorphology(context, dilationLevel,
                precision.interpolated(), precision.bitLevels());
    }

    @Override
    public boolean isPseudoCyclic() {
        return true;
    }

    @Override
    protected abstract Matrix<? extends PArray> asDilationOrErosion(
            Matrix<? extends PArray> src,
            Pattern pattern,
            boolean isDilation);

    @Override
    protected abstract Matrix<? extends UpdatablePArray> dilationOrErosion(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            boolean isDilation,
            boolean disableMemoryAllocation);

    @Override
    public Matrix<? extends PArray> asPercentile(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes,
            Pattern pattern) {
        StreamingApertureProcessor percentiler = RankProcessors.getPercentiler(context(), interpolated, bitLevels);
        return percentiler.asProcessed(src.array().type(), src, percentileIndexes, pattern);
    }

    @Override
    public void percentile(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes,
            Pattern pattern) {
        StreamingApertureProcessor percentiler = RankProcessors.getPercentiler(context(), interpolated, bitLevels);
        percentiler.process(dest, src, percentileIndexes, pattern);
    }

    @Override
    public <T extends PArray> Matrix<T> asRank(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> baseMatrix,
            Matrix<? extends PArray> rankedMatrix,
            Pattern pattern) {
        StreamingApertureProcessor ranker = RankProcessors.getRanker(context(),
                interpolated && PFloatingArray.class.isAssignableFrom(requiredType), bitLevels);
        // if requiredType is not floating-point, interpolation leads to the same ranks
        return ranker.asProcessed(requiredType, baseMatrix, rankedMatrix, pattern);
    }

    @Override
    public void rank(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> baseMatrix,
            Matrix<? extends PArray> rankedMatrix,
            Pattern pattern) {
        StreamingApertureProcessor ranker = RankProcessors.getRanker(context(),
                interpolated && PFloatingArray.class.isAssignableFrom(dest.type()), bitLevels);
        // if dest.type() is not floating-point, interpolation leads to the same ranks
        ranker.process(dest, baseMatrix, rankedMatrix, pattern);
    }

    @Override
    public Matrix<? extends PArray> asMeanBetweenPercentiles(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentilesIndexes,
            Matrix<? extends PArray> toPercentilesIndexes,
            Pattern pattern,
            double filler) {
        StreamingApertureProcessor averager = RankProcessors.getAveragerBetweenPercentiles(context(), filler,
                interpolated, bitLevels);
        return averager.asProcessed(src.array().type(), src, fromPercentilesIndexes, toPercentilesIndexes, pattern);
    }

    @Override
    public void meanBetweenPercentiles(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentilesIndexes,
            Matrix<? extends PArray> toPercentilesIndexes,
            Pattern pattern,
            double filler) {
        StreamingApertureProcessor averager = RankProcessors.getAveragerBetweenPercentiles(context(), filler,
                interpolated, bitLevels);
        averager.process(dest, src, fromPercentilesIndexes, toPercentilesIndexes, pattern);
    }

    @Override
    public Matrix<? extends PArray> asMeanBetweenValues(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern,
            double filler) {
        StreamingApertureProcessor averager = RankProcessors.getAveragerBetweenValues(context(), filler,
                interpolated, bitLevels);
        return averager.asProcessed(src.array().type(), src, minValues, maxValues, pattern);
    }

    @Override
    public void meanBetweenValues(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern,
            double filler) {
        StreamingApertureProcessor averager = RankProcessors.getAveragerBetweenValues(context(), filler,
                interpolated, bitLevels);
        averager.process(dest, src, minValues, maxValues, pattern);
    }

    @Override
    public Matrix<? extends PArray> asFunctionOfSum(
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc) {
        StreamingApertureProcessor averager = RankProcessors.getSummator(context(), processingFunc);
        return averager.asProcessed(src.array().type(), src, pattern);
    }

    @Override
    public void functionOfSum(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc) {
        StreamingApertureProcessor averager = RankProcessors.getSummator(context(), processingFunc);
        averager.process(dest, src, pattern);
    }

    @Override
    public Matrix<? extends PArray> asFunctionOfPercentilePair(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentilesIndexes1,
            Matrix<? extends PArray> percentilesIndexes2,
            Pattern pattern,
            Func processingFunc) {
        StreamingApertureProcessor contraster = RankProcessors.getPercentilePairProcessor(context(),
                processingFunc, interpolated, bitLevels);
        return contraster.asProcessed(src.array().type(),
                src, src, percentilesIndexes1, percentilesIndexes2, pattern);
    }

    @Override
    public void functionOfPercentilePair(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentilesIndexes1,
            Matrix<? extends PArray> percentilesIndexes2,
            Pattern pattern,
            Func processingFunc) {
        StreamingApertureProcessor contraster = RankProcessors.getPercentilePairProcessor(context(),
                processingFunc, interpolated, bitLevels);
        contraster.process(dest, src, src, percentilesIndexes1, percentilesIndexes2, pattern);
    }

}