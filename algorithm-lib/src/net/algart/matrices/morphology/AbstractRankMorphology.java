package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;

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
 * 3) <i>operation</i>, storing result in the 1st <tt>dest</tt> argument.
 * Every 2nd method from such a triplet is implemented via the 3rd one:
 * it creates the necessary resulting updatable matrix and pass it as <tt>dest</tt>
 * argument to the 3rd method. See more details in comments to the methods of this class.</p>
 *
 * <p>In addition, this class fully implements all 3 methods
 * {@link #asMean(Matrix, Pattern) asMean}, {@link #mean(Matrix, Pattern) mean} (creating new matrix) and
 * {@link #mean(Matrix, Matrix, Pattern) mean} (storing result in <tt>dest</tt> argument) via
 * the corresponding {@link #asFunctionOfSum(Matrix, Pattern, Func) asFunctionOfSum}
 * and {@link #functionOfSum(Matrix, Matrix, Pattern, Func) functionOfSum} methods.
 * Also this class fully implements the methods which get the percentile indexes in a form of <tt>double</tt>
 * parameters: these methods are implemented via the analogous methods getting the percentile indexes in
 * {@link Matrix} parameters.</p>
 *
 * <p>Besides implementing methods of {@link RankMorphology}, this class declares
 * {@link #constantPercentileMatrix(Matrix, double)} method, convenient for implementing
 * methods which get the percentile indexes in a form of <tt>double</tt> parameters.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
        Matrix<? extends PArray> src, Pattern pattern, boolean isDilation);

    @Override
    protected abstract Matrix<? extends UpdatablePArray> dilationOrErosion(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern, boolean isDilation,
        boolean disableMemoryAllocation);
    // bad idea to inherit dilationOrErosion from AbstractMorphology: it is much better to call percentile

    public abstract Matrix<? extends PArray> asPercentile(
        Matrix<? extends PArray> src, Matrix<? extends PArray> percentileIndexes, Pattern pattern);

    /**
     * This implementation just calls
     * <tt>{@link #asPercentile(Matrix, Matrix, Pattern) asPercentile}(src,m,pattern)</tt>,
     * where <nobr><tt>m={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex)</tt></nobr>.
     *
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @return                the "lazy" matrix containing the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends PArray> asPercentile(Matrix<? extends PArray> src,
        double percentileIndex, Pattern pattern)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        return asPercentile(src, constantPercentileMatrix(src, percentileIndex), pattern);
    }

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</tt></nobr>, calculates the percentile by the call
     * <nobr><tt>{@link #percentile(Matrix, Matrix, Matrix, Pattern)
     * percentile}(dest,src,percentileIndexes,pattern)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src               the source matrix.
     * @param percentileIndexes the matrix containing <i>r</i> argument: the indexes of the percentile
     *                          for every element of the result.
     * @param pattern           the pattern: the shape of the aperture.
     * @return                  the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> percentile(
        Matrix<? extends PArray> src, Matrix<? extends PArray> percentileIndexes, Pattern pattern)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Matrices.checkDimensionEquality(src, percentileIndexes);
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        percentile(dest, src, percentileIndexes, pattern);
        return dest;
    }

    /**
     * This implementation just calls
     * <tt>{@link #percentile(Matrix, Matrix, Pattern) percentile}(src,m,pattern)</tt>,
     * where <nobr><tt>m={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex)</tt></nobr>.
     *
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @return                the percentile of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> percentile(
        Matrix<? extends PArray> src, double percentileIndex, Pattern pattern)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        return percentile(src, constantPercentileMatrix(src, percentileIndex), pattern);
    }

    public abstract void percentile(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes, Pattern pattern);

    /**
     * This implementation just calls
     * <tt>{@link #percentile(Matrix, Matrix, Matrix, Pattern) percentile}(dest,src,m,pattern)</tt>,
     * where <nobr><tt>m={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex)</tt></nobr>.
     *
     * @param dest            the target matrix.
     * @param src             the source matrix.
     * @param percentileIndex <i>r</i> argument of the percentile.
     * @param pattern         the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public void percentile(Matrix<? extends UpdatablePArray> dest,
        Matrix<? extends PArray> src, double percentileIndex, Pattern pattern)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        percentile(dest, src, constantPercentileMatrix(src, percentileIndex), pattern);
    }

    public abstract  <T extends PArray> Matrix<T> asRank(Class<? extends T> requiredType,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern);

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Class, long...)
     * newMatrix}(UpdatablePArray.class,{@link Arrays#elementType(Class)
     * Arrays.elementType}(requiredType),baseMatrix.dimensions())</tt></nobr>, calculates the rank by the call
     * <nobr><tt>{@link #rank(Matrix, Matrix, Matrix, Pattern)
     * rank}(dest,baseMatrix,rankedMatrix,pattern)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param requiredType the desired type of the built-in array in the returned matrix.
     * @param baseMatrix   the source matrix.
     * @param rankedMatrix the matrix containing <i>v</i> argument: the values,
     *                     the rank of which should be calculated.
     * @param pattern      the pattern: the shape of the aperture.
     * @return             the rank of the given values.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>baseMatrix.{@link Matrix#dimCount() dimCount()}</tt>,
     *                                  or if <tt>requiredType</tt> is not one of classes
     *                                  <tt>{@link UpdatableBitArray}.class</tt> / <tt>{@link BitArray}.class</tt>,
     *                                  <tt>{@link UpdatableCharArray}.class</tt> / <tt>{@link CharArray}.class</tt>,
     *                                  <tt>{@link UpdatableByteArray}.class</tt> / <tt>{@link ByteArray}.class</tt>,
     *                                  <tt>{@link UpdatableShortArray}.class</tt> / <tt>{@link ShortArray}.class</tt>,
     *                                  <tt>{@link UpdatableIntArray}.class</tt> / <tt>{@link IntArray}.class</tt>,
     *                                  <tt>{@link UpdatableLongArray}.class</tt> / <tt>{@link LongArray}.class</tt>,
     *                                  <tt>{@link UpdatableFloatArray}.class</tt> / <tt>{@link FloatArray}.class</tt>
     *                                  or <tt>{@link UpdatableDoubleArray}.class</tt> /
     *                                  <tt>{@link DoubleArray}.class</tt>.
     */
    public <T extends PArray> Matrix<? extends T> rank(Class<? extends T> requiredType,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Matrices.checkDimensionEquality(baseMatrix, rankedMatrix);
        if (pattern.dimCount() != baseMatrix.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Class<?> elementType = Arrays.elementType(requiredType);
        Matrices.checkNewMatrixType(requiredType, elementType);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class,
            elementType, baseMatrix.dimensions());
        rank(dest, baseMatrix, rankedMatrix, pattern);
        return dest.cast(requiredType);
    }

    public abstract void rank(Matrix<? extends UpdatablePArray> dest,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern);

    public abstract Matrix<? extends PArray> asMeanBetweenPercentiles(Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler);

    /**
     * This implementation just calls
     * <tt>{@link #asMeanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * asMeanBetweenPercentiles}(src,m1,m2,pattern,filler)</tt>, where
     * <nobr><tt>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,fromPercentileIndex)</tt></nobr>
     * and <nobr><tt>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,toPercentileIndex)</tt></nobr>.
     *
     * @param src                 the source matrix.
     * @param fromPercentileIndex <i>r</i><sub>1</sub> argument: the index of
     *                            the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndex   <i>r</i><sub>2</sub> argument: the indexes of
     *                            the greater percentile of the averaged range for every element of the result.
     * @param pattern             the pattern: the shape of the aperture.
     * @param filler              the reserved value, returned when
     *                            <nobr><i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub></nobr>.
     * @return                    the "lazy" matrix containing the mean between 2 given percentiles
     *                            of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends PArray> asMeanBetweenPercentiles(
        Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        return asMeanBetweenPercentiles(src,
            constantPercentileMatrix(src, fromPercentileIndex),
            constantPercentileMatrix(src, toPercentileIndex),
            pattern, filler);
    }

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</tt></nobr>, calculates the mean between 2 percentiles by the call
     * <nobr><tt>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(dest,src,fromPercentileIndexes,toPercentileIndexes,pattern,filler)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src                   the source matrix.
     * @param fromPercentileIndexes the matrix containing <i>r</i><sub>1</sub> argument: the indexes of
     *                              the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndexes   the matrix containing <i>r</i><sub>2</sub> argument: the indexes of
     *                              the greater percentile of the averaged range for every element of the result.
     * @param pattern               the pattern: the shape of the aperture.
     * @param filler                the reserved value, returned when
     *                              <nobr><i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub></nobr>.
     * @return                      the mean between 2 given percentiles of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Matrices.checkDimensionEquality(src, fromPercentileIndexes, toPercentileIndexes);
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        meanBetweenPercentiles(dest, src, fromPercentileIndexes, toPercentileIndexes, pattern, filler);
        return dest;
    }

    /**
     * This implementation just calls
     * <tt>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(src,m1,m2,pattern,filler)</tt>, where
     * <nobr><tt>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,fromPercentileIndex)</tt></nobr>
     * and <nobr><tt>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,toPercentileIndex)</tt></nobr>.
     *
     * @param src                 the source matrix.
     * @param fromPercentileIndex <i>r</i><sub>1</sub> argument: the index of
     *                            the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndex   <i>r</i><sub>2</sub> argument: the indexes of
     *                            the greater percentile of the averaged range for every element of the result.
     * @param pattern             the pattern: the shape of the aperture.
     * @param filler              the reserved value, returned when
     *                            <nobr><i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub></nobr>.
     * @return                    the mean between 2 given percentiles of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        return meanBetweenPercentiles(src,
            constantPercentileMatrix(src, fromPercentileIndex),
            constantPercentileMatrix(src, toPercentileIndex),
            pattern, filler);
    }

    public abstract void meanBetweenPercentiles(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler);

    /**
     * This implementation just calls
     * <tt>{@link #meanBetweenPercentiles(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenPercentiles}(dest,src,m1,m2,pattern,filler)</tt>, where
     * <nobr><tt>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,fromPercentileIndex)</tt></nobr>
     * and <nobr><tt>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,toPercentileIndex)</tt></nobr>.
     *
     * @param dest                the target matrix.
     * @param src                 the source matrix.
     * @param fromPercentileIndex <i>r</i><sub>1</sub> argument: the index of
     *                            the less percentile of the averaged range for every element of the result.
     * @param toPercentileIndex   <i>r</i><sub>2</sub> argument: the indexes of
     *                            the greater percentile of the averaged range for every element of the result.
     * @param pattern             the pattern: the shape of the aperture.
     * @param filler              the reserved value, returned when
     *                            <nobr><i>r</i><sub>1</sub>&ge;<i>r</i><sub>2</sub></nobr>.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public void meanBetweenPercentiles(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        meanBetweenPercentiles(dest, src,
            constantPercentileMatrix(src, fromPercentileIndex),
            constantPercentileMatrix(src, toPercentileIndex),
            pattern, filler);
    }

    public abstract Matrix<? extends PArray> asMeanBetweenValues(Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler);

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</tt></nobr>, calculates the mean between 2 values by the call
     * <nobr><tt>{@link #meanBetweenValues(Matrix, Matrix, Matrix, Matrix, Pattern, double)
     * meanBetweenValues}(dest,src,minValues,maxValues,pattern,filler)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src       the source matrix.
     * @param minValues the matrix containing <i>v</i><sub>1</sub> argument: the low bound
     *                  of the averaged range of values for every element of the result.
     * @param maxValues the matrix containing <i>v</i><sub>2</sub> argument: the high bound
     *                  of the averaged range of values for every element of the result.
     * @param pattern   the pattern: the shape of the aperture.
     * @param filler    the reserved value, returned when
     *                  <nobr><i>r</i>(<i>v</i><sub>1</sub>*&sigma;)&ge;<i>r</i>(<i>v</i><sub>2</sub>*&sigma;)</nobr>,
     *                  or one of the special keys {@link #FILL_MIN_VALUE}, {@link #FILL_MAX_VALUE},
     *                  {@link #FILL_NEAREST_VALUE}, which mean using of special calculation modes B, C, D.
     * @return          the mean between 2 given values of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> meanBetweenValues(Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Matrices.checkDimensionEquality(src, minValues, maxValues);
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        meanBetweenValues(dest, src, minValues, maxValues, pattern, filler);
        return dest;
    }

    public abstract void meanBetweenValues(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler);

    /**
     * This method is fully implemented in this class via the equivalent call of
     * {@link #asFunctionOfSum(Matrix, Pattern, Func)} method, as described in
     * {@link RankMorphology#asMean(Matrix, Pattern) comments to this method} in {@link RankMorphology} interface.
     *
     * @param src                 the source matrix.
     * @param pattern             the pattern: the shape of the aperture.
     * @return                    the "lazy" matrix containing the mean of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends PArray> asMean(Matrix<? extends PArray> src, Pattern pattern) {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Func processingFunc = LinearFunc.getInstance(
            PFloatingArray.class.isAssignableFrom(src.type()) ? 0.0 : 0.5,
            1.0 / pattern.pointCount());
        return asFunctionOfSum(src, pattern, processingFunc);
    }

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</tt></nobr>, calculates the mean by the call
     * <nobr><tt>{@link #mean(Matrix, Matrix, Pattern)
     * mean}(dest,src,pattern)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src                 the source matrix.
     * @param pattern             the pattern: the shape of the aperture.
     * @return                    the mean of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> mean(Matrix<? extends PArray> src, Pattern pattern) {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
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
     * @param dest                  the target matrix.
     * @param src                   the source matrix.
     * @param pattern               the pattern: the shape of the aperture.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public void mean(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Func processingFunc = LinearFunc.getInstance(
            PFloatingArray.class.isAssignableFrom(src.type()) ? 0.0 : 0.5,
            1.0 / pattern.pointCount());
        functionOfSum(dest, src, pattern, processingFunc);
    }


    public abstract Matrix<? extends PArray> asFunctionOfSum(Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc);

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</tt></nobr>, calculates the function of aperture sum by the call
     * <nobr><tt>{@link #functionOfSum(Matrix, Matrix, Pattern, Func)
     * functionOfSum}(dest,src,pattern,processingFunc)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src                   the source matrix.
     * @param pattern               the pattern: the shape of the aperture.
     * @param processingFunc        the function, which should be applied to every calculated aperture sum.
     * @return                      the result of the given function for the aperture sum of the source matrix.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> functionOfSum(Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        if (processingFunc == null)
            throw new NullPointerException("Null processingFunc argument");
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        functionOfSum(dest, src, pattern, processingFunc);
        return dest;
    }

    public abstract void  functionOfSum(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc);

    public abstract Matrix<? extends PArray> asFunctionOfPercentilePair(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc);

    /**
     * This implementation just calls
     * <tt>{@link #asFunctionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * asFunctionOfPercentilePair}(src,m1,m2,pattern,processingFunc)</tt>, where
     * <nobr><tt>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex1)</tt></nobr>
     * and <nobr><tt>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex2)</tt></nobr>.
     *
     * @param src              the source matrix.
     * @param percentileIndex1 the 1st <i>r</i> argument: the index of the 1st percentile <i>v</i><sub>1</sub>.
     * @param percentileIndex2 the 2nd <i>r</i> argument: the index of the 2nd percentile <i>v</i><sub>2</sub>.
     * @param pattern          the pattern: the shape of the aperture.
     * @param processingFunc   the function, which should be applied to every calculated three
     *                         <nobr>(<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>)</nobr>,
     *                         where <i>v</i> is the element of the source matrix,
     *                         <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return                 the "lazy" matrix containing the result of the given function.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends PArray> asFunctionOfPercentilePair(
        Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        return asFunctionOfPercentilePair(src,
            constantPercentileMatrix(src, percentileIndex1),
            constantPercentileMatrix(src, percentileIndex2),
            pattern, processingFunc);
    }

    /**
     * This implementation creates a new updatable matrix <tt>dest</tt> by the call
     * <nobr><tt>dest={@link #memoryModel memoryModel}.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}(UpdatablePArray.class,src)</tt></nobr>, calculates the function of the source matrix
     * and 2 percentiles by the call
     * <nobr><tt>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     * functionOfPercentilePair}(dest,src,percentileIndexes1,percentileIndexes2,pattern,processingFunc)</tt></nobr>
     * and returns <tt>dest</tt> as the result.
     * All necessary checks of correctness of the arguments are performed before allocating new matrix.
     *
     * @param src                the source matrix.
     * @param percentileIndexes1 the 1st matrix containing <i>r</i> argument: the indexes of the 1st percentile
     *                           <i>v</i><sub>1</sub> for every element of the result.
     * @param percentileIndexes2 the 2nd matrix containing <i>r</i> argument: the indexes of the 2nd percentile
     *                           <i>v</i><sub>2</sub> for every element of the result.
     * @param pattern            the pattern: the shape of the aperture.
     * @param processingFunc     the function, which should be applied to every calculated three
     *                           <nobr>(<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>)</nobr>,
     *                           where <i>v</i> is the element of the source matrix,
     *                           <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return                   the result of the given function.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        if (processingFunc == null)
            throw new NullPointerException("Null processingFunc argument");
        Matrices.checkDimensionEquality(src, percentileIndexes1, percentileIndexes2);
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        functionOfPercentilePair(dest, src, percentileIndexes1, percentileIndexes2,
            pattern, processingFunc);
        return dest;
    }

    /**
     * This implementation just calls
     * <tt>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Pattern, Func)
     * functionOfPercentilePair}(src,m1,m2,pattern,processingFunc)</tt>, where
     * <nobr><tt>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex1)</tt></nobr>
     * and <nobr><tt>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex2)</tt></nobr>.
     *
     * @param src              the source matrix.
     * @param percentileIndex1 the 1st <i>r</i> argument: the index of the 1st percentile <i>v</i><sub>1</sub>.
     * @param percentileIndex2 the 2nd <i>r</i> argument: the index of the 2nd percentile <i>v</i><sub>2</sub>.
     * @param pattern          the pattern: the shape of the aperture.
     * @param processingFunc   the function, which should be applied to every calculated three
     *                         <nobr>(<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>)</nobr>,
     *                         where <i>v</i> is the element of the source matrix,
     *                         <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @return                 the result of the given function.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        return functionOfPercentilePair(src,
            constantPercentileMatrix(src, percentileIndex1),
            constantPercentileMatrix(src, percentileIndex2),
            pattern, processingFunc);
    }

    public abstract void functionOfPercentilePair(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc);

    /**
     * This implementation just calls
     * <tt>{@link #functionOfPercentilePair(Matrix, Matrix, Matrix, Matrix, Pattern, Func)
     * functionOfPercentilePair}(dest,src,m1,m2,pattern,processingFunc)</tt>, where
     * <nobr><tt>m1={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex1)</tt></nobr>
     * and <nobr><tt>m2={@link #constantPercentileMatrix constantPercentileMatrix}(src,percentileIndex2)</tt></nobr>.
     *
     * @param dest             the target matrix.
     * @param src              the source matrix.
     * @param percentileIndex1 the 1st <i>r</i> argument: the index of the 1st percentile <i>v</i><sub>1</sub>.
     * @param percentileIndex2 the 2nd <i>r</i> argument: the index of the 2nd percentile <i>v</i><sub>2</sub>.
     * @param pattern          the pattern: the shape of the aperture.
     * @param processingFunc   the function, which should be applied to every calculated three
     *                         <nobr>(<i>v</i>,<i>v</i><sub>1</sub>,<i>v</i><sub>2</sub>)</nobr>,
     *                         where <i>v</i> is the element of the source matrix,
     *                         <i>v</i><sub>1</sub> and <i>v</i><sub>2</sub> are the corresponding percentiles.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    public void functionOfPercentilePair(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern.dimCount() != src.dimCount())
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        functionOfPercentilePair(dest, src,
            constantPercentileMatrix(src, percentileIndex1),
            constantPercentileMatrix(src, percentileIndex2),
            pattern, processingFunc);
    }

    /**
     * Returns the matrix with the same dimensions as the given <tt>src</tt> matrix,
     * backed by a constant array with the given value.
     * More precisely, returns <nobr><tt>src.{@link Matrix#matrix(Array) matrix}(const)</tt></nobr>,
     * where
     * <ul>
     * <li><nobr><tt>const = {@link Arrays#nLongCopies(long, long)
     * Arrays.nLongCopies}(src.size(), (long)percentileIndex)</tt></nobr>,
     * if <nobr><tt>percentileIndex==(long)percentileIndex</tt></nobr>,</li>
     * <li>or <nobr><tt>const = {@link Arrays#nDoubleCopies(long, double)
     * Arrays.nDoubleCopies}(src.size(), percentileIndex)</tt></nobr>
     * in other case.</li>
     * </ul>
     *
     * @param src             some matrix.
     * @param percentileIndex some filler.
     * @return                the constant matrix with the same dimensions, filled by <tt>percentileIndex</tt>.
     */
    public static Matrix<? extends PArray> constantPercentileMatrix(Matrix<? extends PArray> src,
        double percentileIndex)
    {
        return src.matrix(percentileIndex == (long) percentileIndex ?
            Arrays.nLongCopies(src.size(), (long) percentileIndex) :
            Arrays.nDoubleCopies(src.size(), percentileIndex));
    }
}
