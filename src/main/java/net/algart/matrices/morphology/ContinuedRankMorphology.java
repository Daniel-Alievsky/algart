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
import net.algart.math.patterns.*;

/**
 * <p>The filter allowing to transform any {@link RankMorphology} object to another instance of that interface,
 * which uses some non-trivial form of continuation outside the source matrix.</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * implementing {@link RankMorphology}, and some {@link net.algart.arrays.Matrix.ContinuationMode continuation mode}.
 * This object works almost identically to the parent object with the only exception,
 * that it uses the specified continuation model instead of the default pseudo-cyclic continuation.
 *
 * <p>More precisely, all methods of this object call the corresponding methods of the parent one.
 * But before calling any processing method, the source matrix is appended (outside its bounds)
 * by some area of additional values
 * with help of {@link Matrix#subMatrix(long[], long[], net.algart.arrays.Matrix.ContinuationMode)} method,
 * using the {@link #continuationMode() continuation mode of this filter}.
 * So, all dimensions of the appended matrix are little greater than the corresponding dimensions
 * of the source one, and extra elements are (virtually) filled by some values according the given continuation mode.
 * The size of extension along all coordinates depends on the patterns (aperture shapes) and the performed operation.
 * After this, the corresponding method of <i>parent</i> object processes the appended matrix,
 * and the method of this class returns the corresponding submatrix of the result, with dimensions, equal
 * to the dimensions of the source matrix.</p>
 *
 * <p>The processing is little different for methods, placing the result into the first argument
 * <code>Matrix&lt;? extends UpdatablePArray&gt; dest</code>, like
 * {@link #percentile(Matrix, Matrix, Matrix, Pattern)}.
 * In these cases, the destination (updatable) matrix is (virtually) extended like the source matrix
 * before calculations with help of
 * {@link Matrix#subMatrix(long[], long[], net.algart.arrays.Matrix.ContinuationMode)} method;
 * so, the calculated data are correctly stored into the original destination matrix.</p>
 *
 * <p>The appending along all coordinates is performed in such a way, that the resulting matrix
 * is the same, as if the source matrix would be virtually appended until the infinity along all directions,
 * according the selected {@link #continuationMode() continuation mode}.</p>
 *
 * <p>Note: {@link net.algart.arrays.Matrix.ContinuationMode#NONE} continuation mode cannot be used in this class:
 * such value of <code>continuationMode</code> argument of the instantiation method
 * {@link #getInstance(Morphology, Matrix.ContinuationMode)} leads
 * to <code>IllegalArgumentException</code>.</p>
 *
 * <p>Note: in improbable cases, when the dimensions of the source matrix and/or
 * the sizes of the pattern are extremely large (about 2<sup>63</sup>),
 * so that the necessary appended matrices should have dimensions or total number of elements,
 * greater than <code>Long.MAX_VALUE</code>,
 * the methods of this class throw <code>IndexOutOfBoundsException</code> and do nothing.
 * Of course, these are very improbable cases.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class ContinuedRankMorphology extends ContinuedMorphology implements RankMorphology {
    private final RankMorphology parent;

    ContinuedRankMorphology(RankMorphology parent, Matrix.ContinuationMode continuationMode) {
        super(parent, continuationMode);
        this.parent = parent;
    }

    /**
     * Returns new instance of this class with the passed parent {@link RankMorphology} object
     * and the specified continuation mode.
     * See comments to {@link net.algart.arrays.Matrix.ContinuationMode} class
     * for more information about possible continuations.
     *
     * @param parent           the instance of {@link RankMorphology} interface that will perform all operations.
     * @param continuationMode the mode of continuation outside the source matrix.
     * @return                 new instance of this class.
     * @throws NullPointerException     if <code>parent</code> or <code>continuationMode</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</code>.
     * @see #parent()
     * @see #continuationMode()
     */
    public static ContinuedRankMorphology getInstance(RankMorphology parent,
        Matrix.ContinuationMode continuationMode)
    {
        return new ContinuedRankMorphology(parent, continuationMode);
    }

    /**
     * Returns the parent {@link RankMorphology} object,
     * passed to {@link #getInstance(RankMorphology, Matrix.ContinuationMode)} method.
     *
     * @return the parent {@link RankMorphology} object.
     */
    @Override
    public RankMorphology parent() {
        return this.parent;
    }

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <code>newContext</code> for all operations.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * @param newContext another context, used by the returned instance; can be {@code null}.
     * @return           new instance with another newContext.
     */
    @Override
    public RankMorphology context(ArrayContext newContext) {
        return new ContinuedRankMorphology(parent.context(newContext), continuationMode);
    }

    /*Repeat.SectionStart lazy*/
    // **** LAZY FUNCTIONS ****
    public Matrix<? extends PArray> asPercentile(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes, Pattern pattern)
    {
        Continuer c = new Continuer(null, src, percentileIndexes, pattern, parent, continuationMode);
        return c.reduce(parent.asPercentile(c.get(0), c.get(1), pattern));
    }

    public Matrix<? extends PArray> asPercentile(Matrix<? extends PArray> src,
        double percentileIndex, Pattern pattern)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asPercentile(c.get(0), percentileIndex, pattern));
    }

    public <T extends PArray> Matrix<T> asRank(Class<? extends T> requiredType,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern)
    {
        Continuer c = new Continuer(null, baseMatrix, rankedMatrix, pattern, parent, continuationMode);
        return c.reduce(parent.asRank(requiredType, c.get(0), c.get(1), pattern));
    }

    public Matrix<? extends PArray> asMeanBetweenPercentiles(Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(null, src, fromPercentileIndexes, toPercentileIndexes, pattern,
            parent, continuationMode);
        return c.reduce(parent.asMeanBetweenPercentiles(c.get(0), c.get(1), c.get(2), pattern, filler));
    }

    public Matrix<? extends PArray> asMeanBetweenPercentiles(Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asMeanBetweenPercentiles(c.get(0), fromPercentileIndex, toPercentileIndex,
            pattern, filler));
    }

    public Matrix<? extends PArray> asMeanBetweenValues(Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(null, src, minValues, maxValues, pattern, parent, continuationMode);
        return c.reduce(parent.asMeanBetweenValues(c.get(0), c.get(1), c.get(2), pattern, filler));
    }

    public Matrix<? extends PArray> asMean(Matrix<? extends PArray> src, Pattern pattern) {
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asMean(c.get(0), pattern));
    }

    public Matrix<? extends PArray> asFunctionOfSum(Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asFunctionOfSum(c.get(0), pattern, processingFunc));
    }

    public Matrix<? extends PArray> asFunctionOfPercentilePair(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(null, src, percentileIndexes1, percentileIndexes2, pattern,
            parent, continuationMode);
        return c.reduce(parent.asFunctionOfPercentilePair(c.get(0), c.get(1), c.get(2), pattern, processingFunc));
    }

    public Matrix<? extends PArray> asFunctionOfPercentilePair(Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asFunctionOfPercentilePair(c.get(0), percentileIndex1, percentileIndex2,
            pattern, processingFunc));
    }
    /*Repeat.SectionEnd lazy*/

    // **** ACTUALIZING FUNCTIONS ****
    public Matrix<? extends UpdatablePArray> percentile(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes, Pattern pattern)
    {
        Continuer c = new Continuer(null, src, percentileIndexes, pattern, parent, continuationMode);
        return c.reduce(parent.percentile(c.get(0), c.get(1), pattern));
    }

    public Matrix<? extends UpdatablePArray> percentile(Matrix<? extends PArray> src,
        double percentileIndex, Pattern pattern)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.percentile(c.get(0), percentileIndex, pattern));
    }

    public <T extends PArray> Matrix<? extends T> rank(Class<? extends T> requiredType,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern)
    {
        Continuer c = new Continuer(null, baseMatrix, rankedMatrix, pattern, parent, continuationMode);
        return c.reduce(parent.rank(requiredType, c.get(0), c.get(1), pattern));
    }

    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(null, src, fromPercentileIndexes, toPercentileIndexes, pattern,
            parent, continuationMode);
        return c.reduce(parent.meanBetweenPercentiles(c.get(0), c.get(1), c.get(2), pattern, filler));
    }

    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.meanBetweenPercentiles(c.get(0), fromPercentileIndex, toPercentileIndex,
            pattern, filler));
    }

    public Matrix<? extends UpdatablePArray> meanBetweenValues(Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(null, src, minValues, maxValues, pattern, parent, continuationMode);
        return c.reduce(parent.meanBetweenValues(c.get(0), c.get(1), c.get(2), pattern, filler));
    }

    public Matrix<? extends UpdatablePArray> mean(Matrix<? extends PArray> src, Pattern pattern) {
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.mean(c.get(0), pattern));
    }

    public Matrix<? extends UpdatablePArray> functionOfSum(Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.functionOfSum(c.get(0), pattern, processingFunc));
    }

    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(null, src, percentileIndexes1, percentileIndexes2, pattern,
            parent, continuationMode);
        return c.reduce(parent.functionOfPercentilePair(c.get(0), c.get(1), c.get(2), pattern, processingFunc));
    }

    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.functionOfPercentilePair(c.get(0), percentileIndex1, percentileIndex2,
            pattern, processingFunc));
    }

    // **** IN-PLACE FUNCTIONS ****
    public void percentile(Matrix<? extends UpdatablePArray> dest,
        Matrix<? extends PArray> src, Matrix<? extends PArray> percentileIndexes, Pattern pattern)
    {
        Continuer c = new Continuer(dest, src, percentileIndexes, pattern, parent, continuationMode);
        parent.percentile(c.continuedDest(), c.get(0), c.get(1), pattern);
    }

    public void percentile(Matrix<? extends UpdatablePArray> dest,
        Matrix<? extends PArray> src, double percentileIndex, Pattern pattern)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(dest, src, pattern, parent, continuationMode);
        parent.percentile(c.continuedDest(), c.get(0), percentileIndex, pattern);
    }

    public void rank(Matrix<? extends UpdatablePArray> dest,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern)
    {
        Continuer c = new Continuer(dest, baseMatrix, rankedMatrix, pattern, parent, continuationMode);
        parent.rank(c.continuedDest(), c.get(0), c.get(1), pattern);
    }

    public void meanBetweenPercentiles(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(dest, src, fromPercentileIndexes, toPercentileIndexes, pattern,
            parent, continuationMode);
        parent.meanBetweenPercentiles(c.continuedDest(), c.get(0), c.get(1), c.get(2), pattern, filler);
    }

    public void meanBetweenPercentiles(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(dest, src, pattern, parent, continuationMode);
        parent.meanBetweenPercentiles(c.continuedDest(), c.get(0), fromPercentileIndex, toPercentileIndex,
            pattern, filler);
    }

    public void meanBetweenValues(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(dest, src, minValues, maxValues, pattern, parent, continuationMode);
        parent.meanBetweenValues(c.continuedDest(), c.get(0), c.get(1), c.get(2), pattern, filler);
    }

    public void mean(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        Continuer c = new Continuer(dest, src, pattern, parent, continuationMode);
        parent.mean(c.continuedDest(), c.get(0), pattern);
    }

    public void functionOfSum(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(dest, src, pattern, parent, continuationMode);
        parent.functionOfSum(c.continuedDest(), c.get(0), pattern, processingFunc);
    }

    public void functionOfPercentilePair(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(dest, src, percentileIndexes1, percentileIndexes2, pattern,
            parent, continuationMode);
        parent.functionOfPercentilePair(c.continuedDest(), c.get(0), c.get(1), c.get(2), pattern, processingFunc);
    }

    public void functionOfPercentilePair(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(dest, src, pattern, parent, continuationMode);
        parent.functionOfPercentilePair(c.continuedDest(), c.get(0), percentileIndex1, percentileIndex2,
            pattern, processingFunc);
    }
}
