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

import net.algart.arrays.*;
import net.algart.math.Point;
import net.algart.math.patterns.Pattern;
import net.algart.math.IPoint;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

/**
 * <p><i>Streaming aperture matrix processor</i>: an algorithm, processing one or
 * several {@link Matrix <i>n</i>-dimensional matrices} and returning one resulting matrix,
 * where the value of every element of the resulting matrix depends on (and only on)
 * the source elements in an aperture with the fixed shape "around" the same position.
 * This class allows to optimize such type of processing in all cases, when the source matrix
 * is not {@link Matrix#isDirectAccessible() direct accessible}, for example, if it is very large
 * and created via {@link LargeMemoryModel}: this class automatically splits the source matrix into blocks
 * which can fit in Java memory, downloads them into (directly accessible) matrices, created by
 * {@link SimpleMemoryModel}, and processes them by the abstract method
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}, overridden by the user.
 * It is supposed that the type of matrix elements is one of primitive Java types
 * (<tt>boolean</tt>, <tt>char</tt>, <tt>byte</tt>, <tt>short</tt>, <tt>int</tt>,
 * <tt>long</tt>, <tt>float</tt>, <tt>double</tt>) and, so, represents an integer or a real number,
 * according to comments to {@link PFixedArray#getLong(long)} and {@link PArray#getDouble(long)} methods.
 * See below for more details.</p>
 *
 * <p>First of all, let's define all terms and specify, what kind of algorithms can be performed by this
 * class.</p>
 *
 * <p>This class works with some {@link Matrix AlgART matrix} <b>M</b>, called the <i>source</i> matrix,
 * some list of additional matrices
 * <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, ..., <b>M</b><sub><i>K</i>&minus;1</sub>,
 * called the <i>additional arguments</i> (<i>K</i> can be 0: an empty list),
 * and some {@link Pattern pattern} <b>P</b>, called the <i>aperture shape</i>.
 * The dimensions of all additional arguments always must be the same as the dimensions of the source
 * matrix: <b>M</b><sub><i>k</i></sub>.{@link Matrix#dimEquals(Matrix) dimEquals}(<b>M</b>);
 * the aperture shape must have the same number of dimensions:
 * <b>P</b>.{@link Pattern#dimCount() dimCount()}<tt>==</tt><b>M</b>.{@link Matrix#dimCount() dimCount()}.
 * The aperture shape <b>P</b> is supposed to be an {@link Pattern <i>integer</i> pattern};
 * if a pattern, passed to the main {@link #process(Matrix, Matrix, List, Pattern) process}
 * method of this class, is not integer, it is automatically
 * rounded to the nearest integer pattern by the call
 * <tt>pattern=pattern.{@link Pattern#round() round()}</tt>.</p>
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
 * (0&le;<i>x</i><sub><i>k</i></sub>&lt;<b>M</b>.<tt>{@link Matrix#dim(int) dim}(<i>k</i>)</tt>
 * for all <i>k</i>), but this condition can be not true for points of the aperture
 * <b>x</b>&minus;<b>p</b><sub><i>i</i></sub>.
 * <br>&nbsp;</li>
 *
 * <li>For every point <b>x</b>' = <b>x</b>&minus;<b>p</b><sub><i>i</i></sub> of the aperture
 * we consider the corresponding <i>value</i> <i>v<sub>i</sub></i> of the source matrix <b>M</b>.
 * More formally, <i>v<sub>i</sub></i> it is the value of the element
 * (integer: {@link PFixedArray#getLong(long)}, if the type of the matrix elements is <tt>boolean</tt>, <tt>char</tt>,
 * <tt>byte</tt>, <tt>short</tt>, <tt>int</tt> or <tt>long</tt>, or real: {@link PArray#getDouble(long)},
 * if the element type is <tt>float</tt> or <tt>double</tt>) of the underlying array
 * <b>M</b>.{@link Matrix#array() array()} with an index
 * <b>M</b>.{@link Matrix#pseudoCyclicIndex(long...)
 * pseudoCyclicIndex}(<i>x</i>'<sub>0</sub>, <i>x</i>'<sub>1</sub>, ..., <i>x</i>'<sub><i>n</i>&minus;1</sub>),
 * where <i>x</i>'<sub><i>k</i></sub> = <i>x</i><sub><i>k</i></sub>&minus;<i>p</i><sub><i>i,k</i></sub>.
 * These values <i>v<sub>i</sub></i> form the <i>unordered</i> set of
 * <i>N</i>=<b>P</b>.{@link Pattern#pointCount() pointCount()} "neighbour" values.
 * <br>&nbsp;</li>
 *
 * <li>Also for the position <b>x</b> we consider an <i>ordered</i> list of <i>K</i> <i>additional values</i>
 * <i>w</i><sub>0</sub>, <i>w</i><sub>1</sub>, ..., <i>w</i><sub><i>K</i>&minus;1</sub> &mdash;
 * the values of corresponding elements of the additional arguments
 * <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, ..., <b>M</b><sub><i>K</i>&minus;1</sub>.
 * More formally, <i>w<sub>k</sub></i> it is the value of the element
 * (integer: {@link PFixedArray#getLong(long)}, if the type of the matrix elements is <tt>boolean</tt>, <tt>char</tt>,
 * <tt>byte</tt>, <tt>short</tt>, <tt>int</tt> or <tt>long</tt>, or real: {@link PArray#getDouble(long)},
 * if the element type is <tt>float</tt> or <tt>double</tt>) of the underlying array
 * <b>M</b><sub><i>k</i></sub>.{@link Matrix#array() array()} with an index
 * <b>M</b><sub><i>k</i></sub>.{@link Matrix#index(long...)
 * index}(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>).
 * <br>&nbsp;</li>
 *
 * <li>The <i>streaming aperture processor</i> is an algorithm, which transforms the source matrix <b>M</b>
 * and the additional matrices
 * <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, ..., <b>M</b><sub><i>K</i>&minus;1</sub>
 * to the resulting matrix <b>R</b>, every element <i>r</i> of which at the position <b>x</b> is the result
 * of some <i>processing function g</i> with <i>N</i>+<i>K</i> arguments:
 * <blockquote>
 * <i>r</i> = <i>g</i> (<i>v</i><sub>0</sub>, <i>v</i><sub>1</sub>, ..., <i>v</i><sub><i>N</i>&minus;1</sub>,
 * <i>w</i><sub>0</sub>, <i>w</i><sub>1</sub>, ..., <i>w</i><sub><i>K</i>&minus;1</sub>).
 * </blockquote>
 * The processing function <i>g</i> is a parameter of this object and is specified by the concrete implementation
 * of {@link #asProcessed(Class, Matrix, List, Pattern)} abstract method.
 * As <i>v</i><sub>0</sub>, <i>v</i><sub>1</sub>, ..., <i>v</i><sub><i>N</i>&minus;1</sub>
 * is unordered set with unspecified order of elements, this function <i>must not depend on the order of
 * its first N arguments</i> <i>v<sub>i</sub></i>. (But it usually depends on the order of the additional
 * arguments <i>w<sub>k</sub></i>.)
 * </li>
 * </ol>
 *
 * <p>This class declares two base methods:</p>
 * <ul>
 * <li>{@link #asProcessed(Class, Matrix, List, Pattern)} abstract method, which fully defines the processing
 * algorithm, i.e. the processing function <i>g</i>, and returns the result of this algorithm as a
 * "lazy" matrix <b>R</b> (i.e. an immutable view of the passed source matrix,
 * such that any reading data from it calculates and returns the resulting <i>r</i> elements);</li>
 * <li>and {@link #process(Matrix, Matrix, List, Pattern)} non-abstract method,
 * which really performs all calculations, using {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}
 * method, and stores the resulting matrix <b>R</b> in its first argument <tt>dest</tt>.</li>
 * </ul>
 *
 * <p>Usually, this class really represents a streaming aperture processor, according the rules listed above.
 * Such implementations of this class are called <i>standard implementations</i>.
 * In standard implementations, it is enough to implement only the abstract method
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}.
 * But it is also allowed this class to represent any other algorithm, that converts <b>M</b> matrix and
 * a set of <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, ..., <b>M</b><sub><i>K</i>&minus;1</sub>
 * additional matrices, according to the given aperture shape <b>P</b>, to the resulting matrix <b>R</b>.
 * Such implementations of this class are called <i>non-standard implementations</i>.
 * In non-standard implementations, you must override both methods
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} and
 * {@link #process(Matrix, Matrix, List, Pattern) process}.
 * You can detect, is this implementation standard or no, with help of
 * {@link #isStandardImplementation()} method.</p>
 *
 * <p><b>Note 1:</b> the definition of the aperture above always supposes the pseudo-cyclic continuation
 * of the source matrix, as described in {@link Matrix#pseudoCyclicIndex(long...)} method. You can use
 * {@link ContinuedStreamingApertureProcessor} (an example of non-standard implementation of this class)
 * to change this behaviour to another continuation way.</p>
 *
 * <p><b>Note 2:</b> it is easy to see that all basic morphology, defined in
 * {@link net.algart.matrices.morphology.Morphology} interface
 * (in a simple case when the number of morphology pattern dimensions
 * is equal to the number of the source matrix dimensions <b>M</b>.{@link Matrix#dimCount() dimCount()},
 * as this class requires),
 * and all rank operations, defined in {@link net.algart.matrices.morphology.RankMorphology} interface,
 * are streaming aperture processors, when they use standard pseudo-cyclic continuation of the matrix
 * ({@link net.algart.matrices.morphology.ContinuedMorphology} and
 * {@link net.algart.matrices.morphology.ContinuedRankMorphology} work in other way).
 * The basic morphology operations have no additional arguments <i>M<sub>k</sub></i>,
 * as well as <i>aperture sum</i> rank operation. The <i>percentile</i> has 1 additional argument <i>r</i>,
 * the <i>rank</i> has 1 additional argument <i>v</i>, the <i>mean between percentiles</i> and
 * <i>mean between values</i> have 2 additional arguments. Most of filters, using in image processing,
 * like the traditional linear filters, are also streaming aperture processors.</p>
 *
 * <p>The main task, solved by this class, is a ready implementation of the second
 * ({@link #process(Matrix, Matrix, List, Pattern) process}) method on the base of the first method
 * ({@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}) for a case of the standard implementations
 * of this class. Namely, the implementation, offered by this class, performs the necessary calculations
 * via one or more calls of {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method,
 * and it tries to optimize them by downloading parts of the source matrix <b>M</b> into quick accessible
 * (i.e. {@link Matrix#isDirectAccessible() direct accessible}) temporary matrices,
 * created by {@link SimpleMemoryModel}, and applying {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}
 * to them. The idea of this optimization is the following. Usually, calculating the result of
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} for every aperture position&nbsp;<b>x</b>
 * requires accessing to <i>N</i> elements of the source matrix in the aperture.
 * If the source matrix is not direct accessible, it can require essential time, especially if
 * it is created by {@link LargeMemoryModel}. But if you are using this class, you can call
 * {@link #process(Matrix, Matrix, List, Pattern) process} method and be almost sure,
 * that it will call {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method with relatively
 * little {@link Matrix#isDirectAccessible() direct accessible} temporary matrices.
 * Such preloading usually increases performance in times even without any additional efforts;
 * but if you want to provide maximal performance, you should check (in your implementation of
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}), whether the passed <tt>src</tt>
 * matrix is {@link Matrix#isDirectAccessible() direct accessible}, and, probably, provide
 * a special optimized branch of the algorithm which works with the
 * {@link DirectAccessible#javaArray() internal Java array}.</p>
 *
 * <p>The amount of Java memory, which {@link #process(Matrix, Matrix, List, Pattern) process} method
 * may spend for the optimization, is specified via {@link #maxTempBufferSize(PArray)} method
 * and usually corresponds to {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()
 * Arrays.SystemSettings.maxTempJavaMemory()} limit.</p>
 *
 * <p>This optimization is possible <b>only in standard implementations</b>, when your
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method really represents
 * some streaming aperture processor. It means that it calculates and returns the results of
 * some processing function <i>g</i> (not depending on the order of first <i>N</i> arguments),
 * as described in the definition above. If these conditions are not fulfilled, the result of
 * {@link #process(Matrix, Matrix, List, Pattern) process} method can be incorrect. For example,
 * a typical possible error while implementing {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}
 * is using the value <i>w</i> of the element of the source matrix <b>M</b> at the position&nbsp;<b>x</b>.
 * It can lead to error, because {@link #process(Matrix, Matrix, List, Pattern) process} method sometimes
 * {@link Pattern#shift(net.algart.math.Point) shifts} the passed pattern and correspondingly
 * {@link Matrices#asShifted(Matrix, long...) shifts} the source matrix <b>M</b>
 * for optimization goals &mdash; so, your {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed}
 * method, while trying to access to the element of the source matrix <b>M</b> at the position&nbsp;<b>x</b>,
 * will really read the value of another element instead. If you need to implement an algorithm, where the result
 * element <i>r</i> at the position&nbsp;<b>x</b> depends also on the element of the source matrix
 * at the same position&nbsp;<b>x</b>, you should pass the source matrix <b>M</b> also as one of additional
 * <b>M</b><sub><i>k</i></sub> matrices. You can find an example of this technique in comments to
 * {@link net.algart.matrices.morphology.RankProcessors#getPercentilePairProcessor(ArrayContext,
 * net.algart.math.functions.Func, boolean, int...)
 * RankProcessors.getPercentilePairProcessor} method, where the usage of it for implementing the corresponding
 * {@link net.algart.matrices.morphology.BasicRankMorphology} methods is described.</p>
 *
 * <p>Note that this class does not try to preload necessary parts of the additional <b>M</b><sub><i>k</i></sub>
 * matrices into Java memory (i.e. temporary matrices, created by {@link SimpleMemoryModel}). The reason is that
 * the aperture size <i>N</i> is usually much greater than the number of additional matrices <i>K</i>
 * (in most applications <i>K</i> is from 0 to 2..3), so it is not too important to optimize
 * access to additional matrices. But, if necessary, you can do it yourself in your
 * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method by good implementing
 * {@link Array#getData(long, Object, int, int)} method in the "lazy" built-in array of the resulting matrix.
 * To do this, you need to load the corresponding subarrays of the additional arguments into your own Java arrays
 * by the corresponding {@link Array#getData(long, Object, int, int)} calls for all arrays
 * <b>M</b><sub><i>k</i></sub>.{@link Matrix#array() array()}
 * and then to process these Java arrays. The {@link JArrayPool} class can help you to minimize necessary
 * memory allocations.</p>
 *
 * <p>This package provides a set of methods for creating objects, extending this class for
 * all basic {@link net.algart.matrices.morphology.RankMorphology rank operations},
 * in {@link net.algart.matrices.morphology.RankProcessors} class.</p>
 *
 * <p><b>Warning</b>: this class can process only patterns
 * where <tt>{@link Pattern#pointCount() pointCount()}&le;Integer.MAX_VALUE</tt>.
 * More precisely, any methods of this class, which have {@link Pattern} argument,
 * can throw {@link net.algart.math.patterns.TooManyPointsInPatternError TooManyPointsInPatternError}
 * or <tt>OutOfMemoryError</tt> in the same situations as {@link Pattern#points()} method.</p>
 *
 * <p>The classes, implementing this interface, are usually <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance. It is not guaranteed for any classes,
 * but it is guaranteed for all instances created by the methods of the classes of this package.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class StreamingApertureProcessor extends AbstractArrayProcessorWithContextSwitching {
    private static final boolean ENABLE_STREAMING = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.matrices.StreamingApertureProcessor.enableStreaming", true);
    // should be true for you want this class to be useful

    /**
     * Creates an instance of this class with the given context.
     *
     * @param context the context used by this instance for all operations.
     */
    protected StreamingApertureProcessor(ArrayContext context) {
        super(context);
    }

    @Override
    public StreamingApertureProcessor context(ArrayContext newContext) {
        return (StreamingApertureProcessor) super.context(newContext);
    }

    /**
     * Returns <tt>true</tt> if there is a guarantee that this object is
     * a <i>standard implementations</i> of this class.
     * For non-standard implementation, this method usually returns <tt>false</tt>.
     * See comments to {@link StreamingApertureProcessor} class for more details.
     *
     * @return whether this implementation is standard.
     */
    public boolean isStandardImplementation() {
        return true;
    }

    /**
     * Equivalent to <tt>{@link #asProcessed(Class, Matrix, List, Pattern)
     * asProcessed}(requiredType, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class), pattern)</tt>.
     *
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param src          the source matrix <b>M</b>.
     * @param pattern      the aperture shape <b>P</b>.
     * @return the "lazy" matrix containing the result of this algorithm.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires at least 1 additional matrix.
     */
    public final <T extends PArray> Matrix<T> asProcessed(
        Class<? extends T> requiredType,
        Matrix<? extends PArray> src,
        Pattern pattern)
    {
        return asProcessed(requiredType, src, Matrices.several(PArray.class), pattern);
    }

    /**
     * Equivalent to <tt>{@link #asProcessed(Class, Matrix, List, Pattern)
     * asProcessed}(requiredType, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class, additionalMatrix), pattern)</tt>.
     *
     * @param requiredType     desired type of the built-in array in the returned matrix.
     * @param src              the source matrix <b>M</b>.
     * @param additionalMatrix the additional matrix <b>M</b><sub>0</sub>.
     * @param pattern          the aperture shape <b>P</b>.
     * @return the "lazy" matrix containing the result of this algorithm.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires more than 1 additional matrix.
     */
    public final <T extends PArray> Matrix<T> asProcessed(
        Class<? extends T> requiredType,
        Matrix<? extends PArray> src,
        Matrix<? extends PArray> additionalMatrix,
        Pattern pattern)
    {
        return asProcessed(requiredType, src,
            Matrices.several(PArray.class, additionalMatrix), pattern);
    }

    /**
     * Equivalent to <tt>{@link #asProcessed(Class, Matrix, List, Pattern)
     * asProcessed}(requiredType, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class, additionalMatrix1, additionalMatrix2), pattern)</tt>.
     *
     * @param requiredType      desired type of the built-in array in the returned matrix.
     * @param src               the source matrix <b>M</b>.
     * @param additionalMatrix1 the additional matrix <b>M</b><sub>0</sub>.
     * @param additionalMatrix2 the additional matrix <b>M</b><sub>1</sub>.
     * @param pattern           the aperture shape <b>P</b>.
     * @return the "lazy" matrix containing the result of this algorithm.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires more than 2 additional matrices.
     */
    public final <T extends PArray> Matrix<T> asProcessed(
        Class<? extends T> requiredType,
        Matrix<? extends PArray> src,
        Matrix<? extends PArray> additionalMatrix1,
        Matrix<? extends PArray> additionalMatrix2,
        Pattern pattern)
    {
        return asProcessed(requiredType, src,
            Matrices.several(PArray.class, additionalMatrix1, additionalMatrix2), pattern);
    }

    /**
     * Equivalent to <tt>{@link #asProcessed(Class, Matrix, List, Pattern)
     * asProcessed}(requiredType, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class, additionalMatrix1, additionalMatrix2, additionalMatrix3), pattern)</tt>.
     *
     * @param requiredType      the desired type of the built-in array in the returned matrix.
     * @param src               the source matrix <b>M</b>.
     * @param additionalMatrix1 the additional matrix <b>M</b><sub>0</sub>.
     * @param additionalMatrix2 the additional matrix <b>M</b><sub>1</sub>.
     * @param additionalMatrix3 the additional matrix <b>M</b><sub>2</sub>.
     * @param pattern           the aperture shape <b>P</b>.
     * @return the "lazy" matrix containing the result of this algorithm.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires more than 3 additional matrices.
     */
    public final <T extends PArray> Matrix<T> asProcessed(
        Class<? extends T> requiredType,
        Matrix<? extends PArray> src,
        Matrix<? extends PArray> additionalMatrix1,
        Matrix<? extends PArray> additionalMatrix2,
        Matrix<? extends PArray> additionalMatrix3,
        Pattern pattern)
    {
        return asProcessed(requiredType, src,
            Matrices.several(PArray.class, additionalMatrix1, additionalMatrix2, additionalMatrix3), pattern);
    }

    /**
     * Returns an immutable view of the passed source matrix <b>M</b>=<tt>src</tt>
     * and the passed additional matrices
     * <b>M</b><sub><i>k</i></sub>=<tt>additionalMatrices.get(</tt><i>k</i><tt>)</tt>,
     * such that any reading data from it calculates and returns the result <b>R</b> of
     * this streaming aperture processor.
     * See the {@link StreamingApertureProcessor comments to this class} for more details.
     *
     * <p>The matrix, returned by this method, is immutable, and the class of its built-in array
     * implements one of the basic interfaces
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray} or {@link DoubleArray}.
     * The class of desired interface (one of 8 possible classes) must be passed as <tt>requiredType</tt> argument.
     * So, it defines the element type of the returned matrix.
     * The rules of casting the floating-point result of the processing function <i>g</i>
     * to the desired element type depend on implementation.
     * In many (but not all) implementations they are the same as in
     * {@link Arrays#asFuncArray(boolean, net.algart.math.functions.Func, Class, PArray...)}
     * method with the argument <tt>truncateOverflows=true</tt>.
     *
     * <p>The concrete algorithm, implementing by this class, can require some number of additional
     * arguments <b>M</b><sub><i>k</i></sub>. If the number of matrices in the specified list
     * <tt>additionalMatrices</tt> is less than the required one, this method throws
     * <tt>IllegalArgumentException</tt>.
     * If the number of passed matrices is greater than the required one, it is not an error:
     * the extra arguments are ignored.
     *
     * <p>Usually you should use {@link #process(Matrix, Matrix, List, Pattern) process} method, which work
     * faster than this method.
     *
     * @param requiredType       the desired type of the built-in array in the returned matrix.
     * @param src                the source matrix <b>M</b>.
     * @param additionalMatrices the additional matrices <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>,
     *                           ..., <b>M</b><sub><i>K</i>&minus;1</sub>.
     * @param pattern            the aperture shape <b>P</b>.
     * @return the "lazy" matrix containing the result of this algorithm.
     * @throws NullPointerException     if one of the arguments is {@code null} or
     *                                  if one of <tt>additionalMatrices</tt> elements is {@code null}.
     * @throws SizeMismatchException    if some passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if the number of additional matrices <tt>additionalMatrices.size()</tt>
     *                                  is less than the number of arguments, required by this implementation.
     */
    public abstract <T extends PArray> Matrix<T> asProcessed(
        Class<? extends T> requiredType,
        Matrix<? extends PArray> src,
        List<? extends Matrix<? extends PArray>> additionalMatrices,
        Pattern pattern);

    /**
     * Equivalent to <tt>{@link #process(Matrix, Matrix, List, Pattern)
     * process}(dest, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class), pattern)</tt>.
     *
     * @param dest    the resulting matrix <b>R</b>.
     * @param src     the source matrix <b>M</b>.
     * @param pattern the aperture shape <b>P</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires at least 1 additional matrix.
     */
    public final void process(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern)
    {
        process(dest, src, Matrices.several(PArray.class), pattern);
    }

    /**
     * Equivalent to <tt>{@link #process(Matrix, Matrix, List, Pattern)
     * process}(dest, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class, additionalMatrix1), pattern)</tt>.
     *
     * @param dest             the resulting matrix <b>R</b>.
     * @param src              the source matrix <b>M</b>.
     * @param additionalMatrix the additional matrix <b>M</b><sub>0</sub>.
     * @param pattern          the aperture shape <b>P</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires more than 1 additional matrix.
     */
    public final void process(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> additionalMatrix,
        Pattern pattern)
    {
        process(dest, src, Matrices.several(PArray.class, additionalMatrix), pattern);
    }

    /**
     * Equivalent to <tt>{@link #process(Matrix, Matrix, List, Pattern)
     * process}(dest, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class, additionalMatrix1, additionalMatrix2), pattern)</tt>.
     *
     * @param dest              the resulting matrix <b>R</b>.
     * @param src               the source matrix <b>M</b>.
     * @param additionalMatrix1 the additional matrix <b>M</b><sub>0</sub>.
     * @param additionalMatrix2 the additional matrix <b>M</b><sub>1</sub>.
     * @param pattern           the aperture shape <b>P</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires more than 2 additional matrices.
     */
    public final void process(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> additionalMatrix1,
        Matrix<? extends PArray> additionalMatrix2,
        Pattern pattern)
    {
        process(dest, src, Matrices.several(PArray.class, additionalMatrix1, additionalMatrix2), pattern);
    }

    /**
     * Equivalent to <tt>{@link #process(Matrix, Matrix, List, Pattern)
     * process}(dest, src, {@link Matrices#several(Class, Matrix[])
     * Matrices.several}(PArray.class, additionalMatrix1, additionalMatrix2, additionalMatrix3), pattern)</tt>.
     *
     * @param dest              the resulting matrix <b>R</b>.
     * @param src               the source matrix <b>M</b>.
     * @param additionalMatrix1 the additional matrix <b>M</b><sub>0</sub>.
     * @param additionalMatrix2 the additional matrix <b>M</b><sub>1</sub>.
     * @param additionalMatrix3 the additional matrix <b>M</b><sub>2</sub>.
     * @param pattern           the aperture shape <b>P</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if this implementation requires more than 3 additional matrices.
     */
    public final void process(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> additionalMatrix1,
        Matrix<? extends PArray> additionalMatrix2,
        Matrix<? extends PArray> additionalMatrix3,
        Pattern pattern)
    {
        process(dest, src,
            Matrices.several(PArray.class, additionalMatrix1, additionalMatrix2, additionalMatrix3), pattern);
    }

    /**
     * Processes the passed source matrix <b>M</b>=<tt>src</tt>
     * and the passed additional matrices
     * <b>M</b><sub><i>k</i></sub>=<tt>additionalMatrices.get(</tt><i>k</i><tt>)</tt>
     * by this streaming aperture processor and stores the result <b>R</b>
     * in <tt>dest</tt> argument.
     * See the {@link StreamingApertureProcessor comments to this class} for more details.
     *
     * <p>This default implementations is based on one or more calls of
     * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method and
     * copying its result into <tt>dest</tt> matrix.
     * The <tt>requiredType</tt> argument of {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method
     * is chosen as <tt>dest.{@link Matrix#type(Class) type}(PArray.class)</tt>.
     * If you need to create a non-standard implementation (a class which does not represent
     * a streaming aperture processor, complying with strict definition from the comments to this class),
     * you must override this method. You also may override this method, if it is possible to provide
     * better performance than the default implementation, for example, for some specific variants of
     * the aperture shape <tt>pattern</tt>.
     *
     * <p>If the element type of <tt>dest</tt> matrix is not floating-point,
     * then this method casts the floating-point result of the processing function <i>g</i>
     * to the types of <tt>dest</tt> elements. The rules of casting depend on implementation
     * and usually are the same as in {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method.
     * The default implementation does not need casting, because all necessary casting is already performed
     * by {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method.
     *
     * <p>The concrete algorithm, implementing by this class, can require some number of additional
     * arguments <b>M</b><sub><i>k</i></sub>. If the number of matrices in the specified list
     * <tt>additionalMatrices</tt> is less than the required one, this method throws
     * <tt>IllegalArgumentException</tt>.
     * If the number of passed matrices is greater than the required one, it is not an error:
     * the extra arguments are ignored.
     *
     * <p>The aperture shape <tt>pattern</tt>, passed to this method, is automatically rounded to the nearest
     * {@link Pattern integer pattern} by the operators
     * <tt>pattern = pattern.{@link Pattern#round() round()}</tt>
     * in the very beginning of this method.
     * In other words, this class is designed for processing integer aperture shapes only.
     * It is the a normal situation for most aperture matrix processing algorithms.
     *
     * @param dest               the resulting matrix <b>R</b>.
     * @param src                the source matrix <b>M</b>.
     * @param additionalMatrices the additional matrices <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>,
     *                           ..., <b>M</b><sub><i>K</i>&minus;1</sub>.
     * @param pattern            the aperture shape <b>P</b>.
     * @throws NullPointerException     if one of the arguments is {@code null} or
     *                                  if one of <tt>additionalMatrices</tt> elements is {@code null}.
     * @throws SizeMismatchException    if some passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>
     *                                  or if the number of additional matrices <tt>additionalMatrices.size()</tt>
     *                                  is less than the number of arguments, required by this implementation.
     */
    public void process(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        List<? extends Matrix<? extends PArray>> additionalMatrices, Pattern pattern)
    {
        Objects.requireNonNull(additionalMatrices, "Null additionalMatrices argument");
        additionalMatrices = new ArrayList<Matrix<? extends PArray>>(additionalMatrices);
        // - to avoid changing by parallel threads
        checkArguments(dest, src, additionalMatrices, pattern);
        pattern = pattern.round(); // - necessary to guarantee further shift() calls without exceptions
        final Class<? extends PArray> requiredType = dest.type(PArray.class);
        PArray sa = src.array();
        final UpdatablePArray da = dest.array();
        final long size = sa.length();
        if (size == 0) {
            return; // to simplify some assertions and provide layerSize!=0 (to avoid division by zero)
        }
        final long bufSize = Math.min(size, maxTempBufferSize(sa));
        ArrayContext arrayContext = context();
        if (size <= bufSize) {
            // If there is enough memory, it is a good idea to download matrix into RAM and to provide
            // non-negative coordinates for all pattern points by shifting the matrix;
            // such shifting is useful even if the source matrix is already in RAM (DirectAccessible).
            // The reason of shifting is to provide stable behaviour of checks in loops which process aperture
            // in most aperture processors. The typical loop is
            //     for (long shift : shifts) {
            //        long i = index - shift;
            //        if (i < 0) {
            //            i += length;
            //        }
            //        do something with element #i;
            //     }
            // and if the checks "i < 0" lead to the same stable result (almost always true or, little better,
            // almost always false), then CPU can optimize this branching.
//            System.out.println("QUICK");
            final IPoint min = pattern.coordMin().toRoundedPoint();
            final Pattern shiftedPattern = pattern.shift(min.symmetric().toPoint()); // "normalized" coordinates >=0
            final Matrix<?> shiftedSrc = Matrices.asShifted(src, min.coordinates());
            Matrix<? extends UpdatablePArray> clone = Arrays.SMM.newMatrix(UpdatablePArray.class, src);
            Matrices.copy(arrayContext == null ? null : arrayContext.part(0.0, 0.05),
                clone, shiftedSrc);
            Matrix<? extends PArray> lazy = asProcessed(requiredType, clone, additionalMatrices, shiftedPattern);
            new Arrays.Copier(arrayContext == null ? null : arrayContext.part(0.05, 1.0),
                dest.array(), lazy.array(), 0, 1).process();
            // "1" allows to split into minimal number of ranges
            return;
        }
        boolean direct = sa instanceof DirectAccessible && ((DirectAccessible) sa).hasJavaArray();
        if (direct || !ENABLE_STREAMING) {
//            System.out.println("SIMPLE");
            Matrix<? extends PArray> lazy = asProcessed(requiredType, src, additionalMatrices, pattern);
            new Arrays.Copier(arrayContext, dest.array(), lazy.array(), 0, 1).process();
            // "1" allows to split into minimal number of ranges
            return;
        }
        assert bufSize < size;

        final IPoint min = pattern.coordMin().toRoundedPoint();
        final Pattern shiftedPattern = pattern.shift(min.symmetric().toPoint()); // "normalized" coordinates >=0
        final Matrix<? extends PArray> shiftedSrc = Matrices.asShifted(src, min.coordinates()).cast(PArray.class);

        final long[] dimensions = src.dimensions();
        final long lSize = Arrays.longMul(dimensions, 0, dimensions.length - 1);
        final long lastDim = dimensions[dimensions.length - 1];
        assert lastDim * lSize == size;
        final long nBuf = bufSize / lSize; // number of layers, that can be loaded into Java memory
        final long[] shifts = toShifts(dimensions, shiftedPattern);
        long nPattern = 1; // number of layers, required to calculate 1 resulting layer
        for (long shift : shifts) {
            assert shift < size;
            long layerIndex = shift / lSize;
            if (layerIndex + 1 > nPattern) { // overflow impossible: layerIndex < lSize <= Long.MAX_VALUE
                nPattern = layerIndex + 1;
            }
        }
        if (nPattern >= nBuf) { // ">=" - really we need nPattern+1 layers
//            System.out.println("SLOW");
            Matrix<? extends PArray> lazy = asProcessed(requiredType, src, additionalMatrices, pattern);
            Matrices.copy(arrayContext, dest, lazy);
            return;
        }
        // now nPattern is the number of layers required to calculate 1 point, not 1 layer
        nPattern++; // overflow impossible: nBuf <= bufSize < size
        // now nPattern is the number of layers required to calculate 1 layer, even with one extra point
        assert nPattern >= 2;

        sa = shiftedSrc.array();
        assert nPattern - 1 < nBuf; // because the "slow" branch has not been used above
        assert nBuf <= lastDim;  // because bufSize < size; it will be important below to be sure that extra<=lastDim
        assert lastDim > 0; // we have checked above the situation size==0
        final long nPerLoop = nBuf - nPattern + 1; // number of layers, processed while one loop iteration
        final long gapSize = (nPattern - 1) * lSize;
        final long[] layerDimensions = dimensions.clone();
        layerDimensions[dimensions.length - 1] = nBuf;
        Matrix<UpdatablePArray> buf = Arrays.SMM.newMatrix(UpdatablePArray.class, sa.elementType(), layerDimensions);
        final UpdatablePArray ba = buf.array();
        ArrayList<Matrix<? extends PArray>> additionalBuf = new ArrayList<Matrix<? extends PArray>>();
        PArray[] additionalArrays = Matrices.arraysOfParallelMatrices(PArray.class, additionalMatrices);
//        System.out.println("ITERATIVE by " + nPerLoop + "/" + nBuf + " from " + lastDim);
        for (long i = 0; i < lastDim; ) {
//            System.out.println("ITERATION at " + i + " from " + lastDim);
            assert nPerLoop + nPattern - 1 == nBuf;
            final long nPerThisLoop = Math.min(nPerLoop, lastDim - i);
            final long nBufThisLoop = nPerThisLoop + nPattern - 1;
            final long bufSizeThisLoop = nBufThisLoop * lSize;
            ArrayContext ac = arrayContext == null ? null : arrayContext.part(i, i + nPerThisLoop, lastDim);
            // buf should contain nBufThisLoop<=nBuf layers with indexes i-nPattern+1..i+nPerThisLoop-1
            if (i == 0) {
                ba.copy(sa.subArray((lastDim - nPattern + 1) * lSize, size));
                // nPattern-1 last layers from shiftedSrc
            } else {
                // so, nPattern-1 additional source layers was loaded to get previous (i>0) nPerLoop result layers
                ba.copy(0, nPerLoop * lSize, gapSize);
                // in previous iteration there was nPerThisLoop==nPerLoop (it can be not so only at the last iteration)
            }
            ba.subArr(gapSize, nPerThisLoop * lSize).copy(sa.subArr(i * lSize, nPerThisLoop * lSize));
            // at the last iteration, ba is filled only partially: last part will not be used
            if (nPerThisLoop < nPerLoop) {
                layerDimensions[dimensions.length - 1] = nBufThisLoop; // only at the last iteration
                buf = Matrices.matrix(ba.subArr(0, bufSizeThisLoop), layerDimensions);
            }
            additionalBuf.clear();
            for (PArray additionalArray : additionalArrays) {
                PArray a;
                if (i < nPattern - 1) {
                    long extra = nPattern - 1 - i;
                    assert extra <= lastDim;
                    // because extra <= nPattern-1 < nBuf <= lastDim
                    assert nPerThisLoop == nPerLoop;
                    // because nPerLoop = nBuf - (nPattern-1) <= lastDim - (nPattern-1) < lastDim-i
                    a = (PArray) Arrays.asConcatenation(
                        additionalArray.subArray((lastDim - extra) * lSize, size),
                        additionalArray.subArray(0, (i + nPerThisLoop) * lSize));
                    assert a.length() == nBuf * lSize;
                } else {
                    a = (PArray) additionalArray.subArray((i - nPattern + 1) * lSize, (i + nPerThisLoop) * lSize);
                    assert a.length() == bufSizeThisLoop;
                }
                additionalBuf.add(Matrices.matrix(a, layerDimensions));
            }
            Matrix<? extends PArray> lazy = asProcessed(requiredType, buf, additionalBuf, shiftedPattern);
            new Arrays.Copier(ac,
                da.subArr(i * lSize, nPerThisLoop * lSize),
                lazy.array().subArr(gapSize, nPerThisLoop * lSize), 0, 1).process();
            i += nPerThisLoop;
        }
    }

    /**
     * Specifies the maximal amount of usual Java memory,
     * measured in elements of temporary arrays, that {@link #process(Matrix, Matrix, List, Pattern) process} method
     * may freely use for optimization needs.
     * The <tt>src</tt> arguments is <b>M</b>.{@link Matrix#array() array()},
     * where <b>M</b> is the source processed matrix, passed to
     * {@link #process(Matrix, Matrix, List, Pattern) process}  method.
     *
     * <p>By default, this method returns <tt>Math.round(maxTempJavaMemory/elementSize)</tt>,
     * where <tt>maxTempJavaMemory =
     * Math.max({@link net.algart.arrays.Arrays.SystemSettings#MIN_OPTIMIZATION_JAVA_MEMORY},
     * {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()})</tt>
     * and <tt>elementSize</tt> is the number of bytes, required for each element of <tt>src</tt> array
     * (i.e. <tt>src.{@link PArray#bitsPerElement() bitsPerElement()}/8.0</tt>).
     *
     * <p>You may override this method if you want to change this behaviour.
     * For example, it can be necessary if your implementation of
     * {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method allocates some Java memory itself:
     * in this case, you should correct the result of this method in such a way, that the total
     * amount of allocated temporary Java memory will not exceed <tt>maxTempJavaMemory</tt> limit.
     *
     * @param src the built-in array of <tt>src</tt> argument of
     *            {@link #process(Matrix, Matrix, List, Pattern) process} method, from which this method is called.
     * @return maximal amount of Java memory, in array elements, allowed for allocation
     *         by {@link #process(Matrix, Matrix, List, Pattern) process} method for optimization needs.
     */
    protected long maxTempBufferSize(PArray src) {
        return Math.round(8.0 / src.bitsPerElement() *
            Math.max(Arrays.SystemSettings.MIN_OPTIMIZATION_JAVA_MEMORY, Arrays.SystemSettings.maxTempJavaMemory()));
    }

    /**
     * Checks whether the passed arguments are allowed arguments for
     * {@link #process(Matrix, Matrix, List, Pattern)} method and throws the corresponding exception
     * if it is not so. Does nothing if the arguments are correct.
     *
     * <p>More precisely, this method checks that all arguments are not {@code null},
     * all elements of <tt>additionalMatrices</tt> (if this list is not empty) are not {@code null},
     * <tt>dest</tt> and <tt>src</tt> matrices and elements of <tt>additionalMatrices</tt> (if this list is not empty)
     * have the {@link Matrix#dimEquals(Matrix) same dimensions} and
     * <tt>pattern.{@link Pattern#dimCount() dimCount()}==src.{@link Matrix#dimCount() dimCount()}</tt>.
     * This method is called in the beginning of {@link #process(Matrix, Matrix, List, Pattern) process} method.
     * It also can be used in the beginning of {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method,
     * with passing <tt>src</tt> argument of that method in a role of both <tt>dest</tt> and <tt>src</tt> arguments
     * of this one.
     *
     * @param dest               the resulting matrix <b>R</b>.
     * @param src                the source matrix <b>M</b>.
     * @param additionalMatrices the additional matrices <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>,
     *                           ..., <b>M</b><sub><i>K</i>&minus;1</sub>.
     * @param pattern            the aperture shape <b>P</b>.
     * @throws NullPointerException     if one of the arguments is {@code null} or
     *                                  if one of <tt>additionalMatrices</tt> elements is {@code null}.
     * @throws SizeMismatchException    if some passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <tt>pattern.{@link Pattern#dimCount() dimCount()}</tt> is not equal
     *                                  to <tt>src.{@link Matrix#dimCount() dimCount()}</tt>.
     */
    protected static void checkArguments(
        Matrix<? extends PArray> dest, Matrix<? extends PArray> src,
        List<? extends Matrix<? extends PArray>> additionalMatrices, final Pattern pattern)
    {
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(additionalMatrices, "Null additionalMatrices argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (pattern.dimCount() != src.dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        if (!dest.dimEquals(src)) {
            throw new SizeMismatchException("Destination and source matrix dimensions mismatch: "
                + dest + " and " + src);
        }
        for (int k = 0, n = additionalMatrices.size(); k < n; k++) {
            Matrix<? extends PArray> m = additionalMatrices.get(k);
            Objects.requireNonNull(m, "Null additional matrix #" + k);
            if (!m.dimEquals(src)) {
                throw new SizeMismatchException("The additional matrix #" + k + " and the src matrix dimensions "
                    + "mismatch: the additional matrix #" + k + " is " + m + ", the src matrix is " + src);
            }
        }
    }

    private static long[] toShifts(long[] dimensions, Pattern pattern) {
        Set<Point> points = pattern.points();
        long[] result = new long[points.size()];
        int k = 0;
        for (Point p : points) {
            result[k++] = p.toRoundedPoint().toOneDimensional(dimensions, true);
        }
        return result;
    }
}
