/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;

import java.util.Objects;

/**
 * <p>A skeletal implementation of the {@link RankMorphology} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>This class extends {@link AbstractMorphology} and inherits from it all implementations of
 * {@link Morphology} methods, excepting {@link
 * #dilationOrErosion(Matrix dest, Matrix src, Pattern pattern, boolean isDilation, boolean disableMemoryAllocation)
 * dilationOrErosion}, which is declared here as abstract.</p>
 *
 * <p>Among methods of {@link RankMorphology} interface, this class implements the 2nd method
 * from each triplet: 1) <i>asOperation</i>, 2) <i>operation</i>, creating new matrix and
 * 3) <i>operation</i>, storing result in the 1st <code>dest</code> argument.
 * Every 2nd method from such a triplet is implemented via the 3rd one:
 * it creates the necessary resulting updatable matrix and pass it as <code>dest</code>
 * argument to the 3rd method. See more details in comments to the methods of this class.</p>
 *
 * <p>In addition, this class fully implements all 3 methods
 * {@link #asMean(Matrix, Pattern) asMean}, {@link #mean(Matrix, Pattern) mean} (creating new matrix) and
 * {@link #mean(Matrix, Matrix, Pattern) mean} (storing result in <code>dest</code> argument) via
 * the corresponding {@link #asFunctionOfSum(Matrix, Pattern, Func) asFunctionOfSum}
 * and {@link #functionOfSum(Matrix, Matrix, Pattern, Func) functionOfSum} methods.
 * Also this class fully implements the methods which get the percentile indexes in a form of <code>double</code>
 * parameters: these methods are implemented via the analogous methods getting the percentile indexes in
 * {@link Matrix} parameters.</p>
 *
 * <p>Besides implementing methods of {@link RankMorphology}, this class declares
 * {@link #constantPercentileMatrix(Matrix, double)} method, convenient for implementing
 * methods which get the percentile indexes in a form of <code>double</code> parameters.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractRankMorphology extends AbstractMorphology implements RankMorphology {

    /**
     * Creates an instance of this class with the given context.
     *
     * @param context the context used by this instance for all operations.
     */
    protected AbstractRankMorphology(ArrayContext context) {
        super(context);
    }

    public RankMorphology context(ArrayContext newContext) {
        return (RankMorphology) super.context(newContext);
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
    // bad idea to inherit dilationOrErosion from AbstractMorphology: it is much better to call percentile

    public abstract Matrix<? extends PArray> asPercentile(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes,
            Pattern pattern);

    /**
     * This implementation just calls
     * <code>{@link #asPercentile(Matrix, Matrix, Pattern) asPercentile}(src,m,pattern)</code>,
     * where <code>m={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex)</code>.
     *
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @return the "lazy" matrix containing the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    public Matrix<? extends PArray> asPercentile(
            Matrix<? extends PArray> src,
            double percentileIndex,
            Pattern pattern) {
        Objects.requireNonNull(src, "Null src argument");
        return asPercentile(src, constantPercentileMatrix(src, percentileIndex), pattern);
    }

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</code>, calculates the percentile by the call
     * <code>{@link #percentile(Matrix, Matrix, Matrix, Pattern)
     * percentile}(dest,src,percentileIndexes,pattern)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
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
     */
    public Matrix<? extends UpdatablePArray> percentile(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes,
            Pattern pattern) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        Matrices.checkDimensionEquality(src, percentileIndexes);
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        percentile(dest, src, percentileIndexes, pattern);
        return dest;
    }

    /**
     * This implementation just calls
     * <code>{@link #percentile(Matrix, Matrix, Pattern) percentile}(src,m,pattern)</code>,
     * where <code>m={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex)</code>.
     *
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @return the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    public Matrix<? extends UpdatablePArray> percentile(
            Matrix<? extends PArray> src,
            double percentileIndex,
            Pattern pattern) {
        Objects.requireNonNull(src, "Null src argument");
        return percentile(src, constantPercentileMatrix(src, percentileIndex), pattern);
    }

    public abstract void percentile(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes,
            Pattern pattern);

    /**
     * This implementation just calls
     * <code>{@link #percentile(Matrix, Matrix, Matrix, Pattern) percentile}(dest,src,m,pattern)</code>,
     * where <code>m={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex)</code>.
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
     */
    public void percentile(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            double percentileIndex,
            Pattern pattern) {
        Objects.requireNonNull(src, "Null src argument");
        percentile(dest, src, constantPercentileMatrix(src, percentileIndex), pattern);
    }

    public abstract <T extends PArray> Matrix<T> asRank(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> baseMatrix,
            Matrix<? extends PArray> rankedMatrix,
            Pattern pattern);

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Class, long...)
     * newMatrix}(UpdatablePArray.class,{@link Arrays#elementType(Class)
     * Arrays.elementType}(requiredType),baseMatrix.dimensions())</code>, calculates the rank by the call
     * <code>{@link #rank(Matrix, Matrix, Matrix, Pattern)
     * rank}(dest,baseMatrix,rankedMatrix,pattern)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
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
     *                                  <code>{@link UpdatableBitArray}.class</code>
     *                                  / <code>{@link BitArray}.class</code>,
     *                                  <code>{@link UpdatableCharArray}.class</code>
     *                                  / <code>{@link CharArray}.class</code>,
     *                                  <code>{@link UpdatableByteArray}.class</code>
     *                                  / <code>{@link ByteArray}.class</code>,
     *                                  <code>{@link UpdatableShortArray}.class</code>
     *                                  / <code>{@link ShortArray}.class</code>,
     *                                  <code>{@link UpdatableIntArray}.class</code>
     *                                  / <code>{@link IntArray}.class</code>,
     *                                  <code>{@link UpdatableLongArray}.class</code>
     *                                  / <code>{@link LongArray}.class</code>,
     *                                  <code>{@link UpdatableFloatArray}.class</code>
     *                                  / <code>{@link FloatArray}.class</code>
     *                                  or <code>{@link UpdatableDoubleArray}.class</code>
     *                                  / <code>{@link DoubleArray}.class</code>.
     */
    public <T extends PArray> Matrix<? extends T> rank(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> baseMatrix,
            Matrix<? extends PArray> rankedMatrix,
            Pattern pattern) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        Matrices.checkDimensionEquality(baseMatrix, rankedMatrix);
        if (pattern.dimCount() != baseMatrix.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Class<?> elementType = Arrays.elementType(requiredType);
        Matrices.checkNewMatrixType(requiredType, elementType);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class,
                elementType, baseMatrix.dimensions());
        rank(dest, baseMatrix, rankedMatrix, pattern);
        return dest.cast(requiredType);
    }

    public abstract void rank(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> baseMatrix,
            Matrix<? extends PArray> rankedMatrix,
            Pattern pattern);

    public abstract Matrix<? extends PArray> asMeanBetweenPercentiles(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentileIndexes,
            Matrix<? extends PArray> toPercentileIndexes,
            Pattern pattern,
            double filler);

    /**
     * This implementation just calls
     * <code>{@link #asMeanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * asMeanBetweenPercentiles}(src,m1,m2,pattern,filler)</code>, where
     * <code>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,fromPercentileIndex)</code>
     * and <code>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,toPercentileIndex)</code>.
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
     */
    public Matrix<? extends PArray> asMeanBetweenPercentiles(
            Matrix<? extends PArray> src,
            double fromPercentileIndex,
            double toPercentileIndex,
            Pattern pattern, double filler) {
        Objects.requireNonNull(src, "Null src argument");
        return asMeanBetweenPercentiles(src,
                constantPercentileMatrix(src, fromPercentileIndex),
                constantPercentileMatrix(src, toPercentileIndex),
                pattern, filler);
    }

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</code>, calculates the mean between 2 percentiles by the call
     * <code>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(dest,src,fromPercentileIndexes,toPercentileIndexes,pattern,filler)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
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
     */
    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentileIndexes,
            Matrix<? extends PArray> toPercentileIndexes,
            Pattern pattern,
            double filler) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        Matrices.checkDimensionEquality(src, fromPercentileIndexes, toPercentileIndexes);
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        meanBetweenPercentiles(dest, src, fromPercentileIndexes, toPercentileIndexes, pattern, filler);
        return dest;
    }

    /**
     * This implementation just calls
     * <code>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(src,m1,m2,pattern,filler)</code>, where
     * <code>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,fromPercentileIndex)</code>
     * and <code>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,toPercentileIndex)</code>.
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
     */
    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(
            Matrix<? extends PArray> src,
            double fromPercentileIndex,
            double toPercentileIndex,
            Pattern pattern,
            double filler) {
        Objects.requireNonNull(src, "Null src argument");
        return meanBetweenPercentiles(src,
                constantPercentileMatrix(src, fromPercentileIndex),
                constantPercentileMatrix(src, toPercentileIndex),
                pattern, filler);
    }

    public abstract void meanBetweenPercentiles(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> fromPercentileIndexes,
            Matrix<? extends PArray> toPercentileIndexes,
            Pattern pattern,
            double filler);

    /**
     * This implementation just calls
     * <code>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(dest,src,m1,m2,pattern,filler)</code>, where
     * <code>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,fromPercentileIndex)</code>
     * and <code>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,toPercentileIndex)</code>.
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
     */
    public void meanBetweenPercentiles(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            double fromPercentileIndex,
            double toPercentileIndex,
            Pattern pattern,
            double filler) {
        Objects.requireNonNull(src, "Null src argument");
        meanBetweenPercentiles(dest, src,
                constantPercentileMatrix(src, fromPercentileIndex),
                constantPercentileMatrix(src, toPercentileIndex),
                pattern, filler);
    }

    public abstract Matrix<? extends PArray> asMeanBetweenValues(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern,
            double filler);

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</code>, calculates the mean between 2 values by the call
     * <code>{@link #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenValues}(dest,src,minValues,maxValues,pattern,filler)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
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
     */
    public Matrix<? extends UpdatablePArray> meanBetweenValues(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern,
            double filler) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        Matrices.checkDimensionEquality(src, minValues, maxValues);
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        meanBetweenValues(dest, src, minValues, maxValues, pattern, filler);
        return dest;
    }

    public abstract void meanBetweenValues(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> minValues,
            Matrix<? extends PArray> maxValues,
            Pattern pattern,
            double filler);

    /**
     * This method is fully implemented in this class via the equivalent call of
     * {@link #asFunctionOfSum(Matrix, Pattern, Func)} method, as described in
     * {@link RankMorphology#asMean(Matrix, Pattern) comments to this method} in {@link RankMorphology} interface.
     *
     * @param src     the source matrix.
     * @param pattern the pattern: the shape of the aperture.
     * @return the "lazy" matrix containing the mean of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    public Matrix<? extends PArray> asMean(Matrix<? extends PArray> src, Pattern pattern) {
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Func processingFunc = LinearFunc.getInstance(
                PFloatingArray.class.isAssignableFrom(src.type()) ? 0.0 : 0.5,
                1.0 / pattern.pointCount());
        return asFunctionOfSum(src, pattern, processingFunc);
    }

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</code>, calculates the mean by the call
     * <code>{@link #mean(Matrix, Matrix, Pattern)
     * mean}(dest,src,pattern)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src     the source matrix.
     * @param pattern the pattern: the shape of the aperture.
     * @return the mean of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    public Matrix<? extends UpdatablePArray> mean(Matrix<? extends PArray> src, Pattern pattern) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        mean(dest, src, pattern);
        return dest;
    }

    /**
     * This method is fully implemented in this class via the equivalent call of
     * {@link #functionOfSum(Matrix, Matrix, Pattern, Func)} method, as described in
     * {@link RankMorphology#mean(Matrix, Matrix, Pattern) comments to this method}
     * in {@link RankMorphology} interface.
     *
     * @param dest    the target matrix.
     * @param src     the source matrix.
     * @param pattern the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    public void mean(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Func processingFunc = LinearFunc.getInstance(
                PFloatingArray.class.isAssignableFrom(src.type()) ? 0.0 : 0.5,
                1.0 / pattern.pointCount());
        functionOfSum(dest, src, pattern, processingFunc);
    }


    public abstract Matrix<? extends PArray> asFunctionOfSum(
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc);

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</code>, calculates the function of aperture sum by the call
     * <code>{@link #functionOfSum(Matrix, Matrix, Pattern, Func)
     * functionOfSum}(dest,src,pattern,processingFunc)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src            the source matrix.
     * @param pattern        the pattern: the shape of the aperture.
     * @param processingFunc the function, which should be applied to every calculated aperture sum.
     * @return the result of the given function for the aperture sum of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    public Matrix<? extends UpdatablePArray> functionOfSum(
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        Objects.requireNonNull(processingFunc, "Null processingFunc argument");
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        functionOfSum(dest, src, pattern, processingFunc);
        return dest;
    }

    public abstract void functionOfSum(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            Func processingFunc);

    public abstract Matrix<? extends PArray> asFunctionOfPercentilePair(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes1,
            Matrix<? extends PArray> percentileIndexes2,
            Pattern pattern,
            Func processingFunc);

    /**
     * This implementation just calls
     * <code>{@link #asFunctionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * asFunctionOfPercentilePair}(src,m1,m2,pattern,processingFunc)</code>, where
     * <code>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex1)</code>
     * and <code>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex2)</code>.
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
     */
    public Matrix<? extends PArray> asFunctionOfPercentilePair(
            Matrix<? extends PArray> src,
            double percentileIndex1,
            double percentileIndex2,
            Pattern pattern, Func processingFunc) {
        Objects.requireNonNull(src, "Null src argument");
        return asFunctionOfPercentilePair(src,
                constantPercentileMatrix(src, percentileIndex1),
                constantPercentileMatrix(src, percentileIndex2),
                pattern, processingFunc);
    }

    /**
     * This implementation creates a new updatable matrix <code>dest</code> by the call
     * <code>dest={@link #memoryModel() memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</code>, calculates the function of the source matrix
     * and 2 percentiles by the call
     * <code>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     * functionOfPercentilePair}(dest,src,percentileIndexes1,percentileIndexes2,pattern,processingFunc)</code>
     * and returns <code>dest</code> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
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
     */
    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes1,
            Matrix<? extends PArray> percentileIndexes2,
            Pattern pattern,
            Func processingFunc) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        Objects.requireNonNull(processingFunc, "Null processingFunc argument");
        Matrices.checkDimensionEquality(src, percentileIndexes1, percentileIndexes2);
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        functionOfPercentilePair(dest, src, percentileIndexes1, percentileIndexes2,
                pattern, processingFunc);
        return dest;
    }

    /**
     * This implementation just calls
     * <code>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * functionOfPercentilePair}(src,m1,m2,pattern,processingFunc)</code>, where
     * <code>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex1)</code>
     * and <code>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex2)</code>.
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
     */
    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(
            Matrix<? extends PArray> src,
            double percentileIndex1,
            double percentileIndex2,
            Pattern pattern, Func processingFunc) {
        Objects.requireNonNull(src, "Null src argument");
        return functionOfPercentilePair(src,
                constantPercentileMatrix(src, percentileIndex1),
                constantPercentileMatrix(src, percentileIndex2),
                pattern, processingFunc);
    }

    public abstract void functionOfPercentilePair(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Matrix<? extends PArray> percentileIndexes1,
            Matrix<? extends PArray> percentileIndexes2,
            Pattern pattern,
            Func processingFunc);

    /**
     * This implementation just calls
     * <code>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     * functionOfPercentilePair}(dest,src,m1,m2,pattern,processingFunc)</code>, where
     * <code>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex1)</code>
     * and <code>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex2)</code>.
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
     */
    public void functionOfPercentilePair(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            double percentileIndex1,
            double percentileIndex2,
            Pattern pattern, Func processingFunc) {
        Objects.requireNonNull(src, "Null src argument");
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        functionOfPercentilePair(dest, src,
                constantPercentileMatrix(src, percentileIndex1),
                constantPercentileMatrix(src, percentileIndex2),
                pattern, processingFunc);
    }

    /**
     * Returns the matrix with the same dimensions as the given <code>src</code> matrix,
     * backed by a constant array with the given value.
     * More precisely, returns <code>src.{@link Matrix#matrix(Array) matrix}(const)</code>,
     * where
     * <ul>
     * <li><code>const = {@link Arrays#nLongCopies(long, long)
     * Arrays.nLongCopies}(src.size(), (long)percentileIndex)</code>,
     * if <code>percentileIndex==(long)percentileIndex</code>,</li>
     * <li>or <code>const = {@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex)</code>
     * in another case.</li>
     * </ul>
     *
     * @param src             some matrix.
     * @param percentileIndex some filler.
     * @return the constant matrix with the same dimensions, filled by <code>percentileIndex</code>.
     */
    public static Matrix<? extends PArray> constantPercentileMatrix(
            Matrix<? extends PArray> src,
            double percentileIndex) {
        return src.matrix(percentileIndex == (long) percentileIndex ?
                Arrays.nLongCopies(src.size(), (long) percentileIndex) :
                Arrays.nDoubleCopies(src.size(), percentileIndex));
    }
}
