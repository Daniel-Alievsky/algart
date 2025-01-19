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
import net.algart.math.IPoint;
import net.algart.math.functions.Func;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.UniformGridPattern;

/**
 * <p>Mathematical morphology over {@link Matrix <i>n</i>-dimensional matrices} with a random
 * <i>n</i>-dimensional structuring element (shape), represented by {@link Pattern} class.
 * It is supposed that the type of matrix elements is one of primitive Java types
 * (<code>boolean</code>, <code>char</code>, <code>byte</code>, <code>short</code>, <code>int</code>,
 * <code>long</code>, <code>float</code>, <code>double</code>) and, so, represents an integer or a real number,
 * according to comments to {@link PFixedArray#getLong(long)} and {@link PArray#getDouble(long)} methods.
 * In 2-dimensional case, these operations can be used for processing grayscale digital images.
 * Please see <a href="http://en.wikipedia.org/wiki/Mathematical_morphology">Wikipedia</a>
 * about the "Mathematical morphology" concept.</p>
 *
 * <p>Basic operations, defined by this interface, are <i>{@link #dilation(Matrix, Pattern) dilation}</i>
 * and <i>{@link #erosion(Matrix, Pattern) erosion}</i>. Other operations are combinations
 * of the basic ones and, probably, some arithmetic elementwise operations.</p>
 *
 * <p>This package provides the following basic methods for creating objects, implementing this interface:</p>
 *
 * <ul>
 * <li>{@link BasicMorphology#getInstance(ArrayContext)};</li>
 * <li>{@link BasicMorphology#getInstance(ArrayContext, long)};</li>
 * <li>{@link ContinuedMorphology#getInstance(Morphology, Matrix.ContinuationMode)};</li>
 * </ul>
 *
 * <p>and also the methods creating {@link RankMorphology} objects &mdash; see comments to that interface.</p>
 *
 * <p>The classes, implementing this interface, are <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public interface Morphology extends ArrayProcessorWithContextSwitching {

    /**
     * <p>Subtraction mode for some methods of {@link Morphology} interface.</p>
     *
     * <p>The following morphology methods:</p>
     *
     * <ul>
     *     <li>{@link Morphology#dilation(Matrix, Pattern, Morphology.SubtractionMode)},</li>
     *     <li>{@link Morphology#erosion(Matrix, Pattern, Morphology.SubtractionMode)},</li>
     *     <li>{@link Morphology#dilationErosion(Matrix, Pattern, Pattern, Morphology.SubtractionMode)},</li>
     *     <li>{@link Morphology#erosionDilation(Matrix, Pattern, Pattern, Morphology.SubtractionMode)},</li>
     *     <li>{@link Morphology#closing(Matrix, Pattern, Morphology.SubtractionMode)},</li>
     *     <li>{@link Morphology#opening(Matrix, Pattern, Morphology.SubtractionMode)},</li>
     * </ul>
     *
     * <p>vary their behavior depending on the last argument of this type.
     * The usual behavior, corresponding to names of the methods, obtains when this argument is {@link #NONE}.
     * However, if this argument is {@link #SUBTRACT_RESULT_FROM_SRC} or {@link #SUBTRACT_SRC_FROM_RESULT},
     * every method, after calculating the result <code>R</code> by usual rules,
     * automatically performs elementwise subtraction
     * either the result <code>R</code> from the source matrix <code>A</code> (the 1st argument),
     * or the source matrix <code>A</code> from the result <code>R</code>.
     * Namely:</p>
     *
     * <ul>
     *     <li>if the last argument is {@link #SUBTRACT_RESULT_FROM_SRC}, every of the listed methods returns
     *     the elementwise positive difference <code>max(0,A-R)</code>;</li>
     *     <li>if the last argument is {@link #SUBTRACT_SRC_FROM_RESULT}, every of the listed methods returns
     *     the elementwise positive difference <code>max(0,R-A)</code>;</li>
     *     <li>if the last argument is {@link #NONE}, no additional subtractions are performed: the method
     *     just returns <code>R</code>.</li>
     * </ul>
     *
     * <p>Note that the <code>byte</code> and <code>short</code> elements are considered to be unsigned integers
     * while subtraction.</p>
     */
    enum SubtractionMode {
        /**
         * No subtractions are performed: standard behavior.
         * See {@link SubtractionMode comments to this enumeration} for more details.
         */
        NONE() {
            @Override
            void subtract(ArrayContext context, Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
                // don't modify dest
            }
        },

        /**
         * The result of calculations is subtracted from the source matrix, and the (positive) difference is returned.
         * See {@link SubtractionMode comments to this enumeration} for more details.
         */
        SUBTRACT_RESULT_FROM_SRC() {
            @Override
            void subtract(ArrayContext context, Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
                Matrices.applyFunc(context, true, Func.POSITIVE_DIFF, dest, src, dest);
            }
        },

        /**
         * The source matrix is subtracted from the result of calculations, and the (positive) difference is returned.
         * See {@link SubtractionMode comments to this enumeration} for more details.
         */
        SUBTRACT_SRC_FROM_RESULT() {
            @Override
            void subtract(ArrayContext context, Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
                Matrices.applyFunc(context, true, Func.POSITIVE_DIFF, dest, dest, src);
            }
        };

        abstract void subtract(ArrayContext context,
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src);
    }

    Morphology context(ArrayContext newContext);

    /**
     * Returns <code>true</code>, if this class works in the default
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC
     * pseudo-cyclic continuation mode}.
     *
     * <p>More precisely, it means that when the value in some element of the processed matrix,
     * returned by a method of this class, depends on elements of the source matrix, lying outside its bounds,
     * then it is supposed that the values outside the source matrix are calculated as described in
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC}. Exactly such behavior is specified in
     * the comments to the basic {@link #dilation(Matrix, Pattern)} and {@link #erosion(Matrix, Pattern)}
     * methods as the default definition of dilation and erosion.
     *
     * <p>This method returns <code>true</code> in {@link BasicMorphology} and {@link BasicRankMorphology} implementation.
     * However, it usually returns <code>false</code> in {@link ContinuedMorphology} and
     * {@link ContinuedRankMorphology} classes &mdash; excepting the only degenerated case when the used
     * {@link ContinuedMorphology#continuationMode() continuation mode} is
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC PSEUDO_CYCLIC}.
     *
     * @return whether this class works in the pseudo-cyclic continuation mode.
     */
    boolean isPseudoCyclic();

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>dilation</i>
     * of the source matrix by the specified pattern.
     * See {@link #dilation(Matrix, Pattern)} method about the "<i>dilation</i>" term.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>The result is usually "lazy", that means that this method finishes immediately and all
     * actual calculations are performed while getting elements of the returned matrix.
     * It is true for all implementations provided by this package.
     * However, some implementations may not support lazy dilation;
     * then this method will be equivalent to {@link #dilation(Matrix, Pattern)}.
     *
     * <p>Please note: this method does not require time (if the result is "lazy"),
     * but the resulting matrix can work slowly!
     * For example, reading all its content than work much slower than {@link #dilation(Matrix, Pattern)}
     * method for complex patterns.
     * Usually you should use it only for very little patterns, or if you know that the implementation
     * of this interface does not provide better algorithm for non-"lazy"
     * {@link #dilation(Matrix, Pattern)} method.
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the "lazy" matrix containing the dilation of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends PArray> asDilation(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>erosion</i>
     * of the source matrix by the specified pattern.
     * See {@link #erosion(Matrix, Pattern)} method about the "<i>erosion</i>" term.
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     *
     * <p>The result is usually "lazy", that means that this method finishes immediately and all
     * actual calculations are performed while getting elements of the returned matrix.
     * It is true for all implementations provided by this package.
     * However, some implementations may not support lazy erosion;
     * then this method will be equivalent to {@link #erosion(Matrix, Pattern)}.
     *
     * <p>Please note: this method does not require time (if the result is "lazy"),
     * but the resulting matrix can work slowly!
     * For example, reading all its content than work much slower than {@link #dilation(Matrix, Pattern)}
     * method for complex patterns.
     * Usually you should use it only for very little patterns, or if you know that the implementation
     * of this interface does not provide better algorithm for non-"lazy"
     * {@link #erosion(Matrix, Pattern)} method.
     *
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the "lazy" matrix containing the erosion of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends PArray> asErosion(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>dilation</i>
     * of the source matrix by the specified pattern.
     *
     * <p>Usually <i>dilation</i> means the elementwise maximum from the set of matrices,
     * obtained by pseudo-cyclic shifting the source matrix by the vectors,
     * equal to all pattern points.
     * More precisely, let <i>m</i><sub><i>i</i></sub><code>={@link
     * Matrices#asShifted(Matrix, long...)
     * Matrices.asShifted}(src,ip.{@link IPoint#coordinates() coordinates()})</code>,
     * where <code>ip</code> is the point <code>#<i>i</i></code> from all points contained in the pattern.
     * Then the every element of the returned matrix is the maximum from all corresponding elements
     * of all <i>m</i><sub><i>i</i></sub> matrices. The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     * The <code>byte</code> and <code>short</code> elements are considered to be unsigned.
     * In a case of <code>bit</code> elements, the maximum is equivalent to logical OR.
     *
     * <p>The basic morphology implementation {@link BasicMorphology} strictly complies with this definition.
     * However, other implementations of this interface may use alternate definitions of the <i>dilation</i> term.
     * For example, some percentile (90% or 80%) may be used instead of strict maximum
     * (as in objects, returned by {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision)}
     * method),
     * or elements outside the matrix may be supposed to be filled according some non-trivial rules
     * instead of pseudo-cyclic continuation
     * (as in {@link ContinuedMorphology} objects),
     * or only some region of the matrix may be processed, etc.
     *
     * <p>Please see
     * <a href="http://en.wikipedia.org/wiki/Dilation_%28morphology%29">Wikipedia</a>
     * to know more about the dilation.
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the result of dilation of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asDilation(Matrix, Pattern)
     * @see #dilation(Matrix, Matrix, Pattern, boolean)
     * @see #dilation(Matrix, Pattern, Morphology.SubtractionMode)
     */
    Matrix<? extends UpdatablePArray> dilation(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>erosion</i>
     * of the source matrix by the specified pattern.
     *
     * <p>Usually <i>erosion</i> means the elementwise minimum from the set of matrices,
     * obtained by pseudo-cyclic shifting the source matrix by the vectors,
     * symmetric to all pattern points relatively the origin of coordinates.
     * More precisely, let <i>m</i><sub><i>i</i></sub><code>={@link
     * Matrices#asShifted(Matrix, long...)
     * Matrices.asShifted}(src,ip.{@link IPoint#symmetric()
     * symmetric()}.{@link IPoint#coordinates() coordinates()})</code>,
     * where <code>ip</code> is the point <code>#<i>i</i></code> from all points contained in the pattern.
     * Then the every element of the returned matrix is the minimum from all corresponding elements
     * of all <i>m</i><sub><i>i</i></sub> matrices. The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     * The <code>byte</code> and <code>short</code> elements are considered to be unsigned.
     * In a case of <code>bit</code> elements, the minimum is equivalent to logical AND.
     *
     * <p>The basic morphology implementation {@link BasicMorphology} strictly complies with this definition.
     * However, other implementations of this interface may use alternate definitions of the <i>erosion</i> term.
     * For example, some percentile (10% or 20%) may be used instead of strict minimum
     * (as in objects, returned by {@link BasicRankMorphology#getInstance(ArrayContext, double, CustomRankPrecision)}
     * method),
     * or elements outside the matrix may be supposed to be filled according some non-trivial rules
     * instead of pseudo-cyclic continuation
     * (as in {@link ContinuedMorphology} objects),
     * or only some region of the matrix may be processed, etc.
     *
     * <p>Please see
     * <a href="http://en.wikipedia.org/wiki/Erosion_%28morphology%29">Wikipedia</a>
     * to know more about the erosion.
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the result of erosion of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     * @see #asDilation(Matrix, Pattern)
     * @see #erosion(Matrix, Matrix, Pattern, boolean)
     * @see #erosion(Matrix, Pattern, Morphology.SubtractionMode)
     */
    Matrix<? extends UpdatablePArray> erosion(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Extended version of {@link #dilation(Matrix, Pattern)} method: if <code>subtractionMode</code> argument
     * is not {@link SubtractionMode#NONE},
     * returns the difference between the dilation and the <code>src</code> matrix,
     * according the specified mode.
     *
     * <p>If <code>subtractionMode=={@link SubtractionMode#NONE}</code>, this method is strictly equivalent
     * to {@link #dilation(Matrix, Pattern)}.
     *
     * <p>The result of this operation with
     * <code>subtractionMode=={@link SubtractionMode#SUBTRACT_SRC_FROM_RESULT}</code>
     * is also called the <i>external gradient</i> of the source matrix.
     *
     * @param src             the source matrix.
     * @param pattern         the pattern.
     * @param subtractionMode whether the difference of the dilation and the source matrix should be returned.
     * @return                the result of dilation of the source matrix by the given pattern
     *                        or the difference of the dilation and the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> dilation(
            Matrix<? extends PArray> src, Pattern pattern,
            SubtractionMode subtractionMode);

    /**
     * Extended version of {@link #erosion(Matrix, Pattern)} method: if <code>subtractionMode</code> argument
     * is not {@link SubtractionMode#NONE},
     * returns the difference between the erosion and the <code>src</code> matrix,
     * according the specified mode.
     *
     * <p>If <code>subtractionMode=={@link SubtractionMode#NONE}</code>, this method is strictly equivalent
     * to {@link #erosion(Matrix, Pattern)}.
     *
     * <p>The result of this operation with
     * <code>subtractionMode=={@link SubtractionMode#SUBTRACT_RESULT_FROM_SRC}</code>
     * is also called the <i>internal gradient</i> of the source matrix.
     *
     * @param src             the source matrix.
     * @param pattern         the pattern.
     * @param subtractionMode whether the difference of the erosion and the source matrix should be returned.
     * @return                the result of erosion of the source matrix by the given pattern
     *                        or the difference of the erosion and the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> erosion(
            Matrix<? extends PArray> src, Pattern pattern,
            SubtractionMode subtractionMode);

    /**
     * Equivalent to {@link #dilation(Matrix, Pattern)} method, but the result matrix
     * will be placed in the <code>dest</code> argument.
     * It allows to avoid extra memory allocation if you need to perform dilation many times
     * from one matrix to another.
     *
     * <p>Moreover, if <code>disableMemoryAllocation</code> argument is <code>true</code>, this method
     * guarantees that no any additional memory will be allocated, even if it can optimize the algorithm speed.
     * In this case, this method is always executed in one pass:
     * it is equivalent to creating new lazy matrix by {@link #asDilation(Matrix src, Pattern pattern)} method
     * and further copying it into <code>dest</code> by {@link Matrices#copy(ArrayContext, Matrix, Matrix)} method.
     * It can be useful if you are sure that the pattern is small enough (usually 2-10 points),
     * and allocation additional work matrices can slow down the algorithm to greater extent
     * than using the simple one-pass algorithm.
     *
     * <p>If the element type of the <code>dest</code> matrix is not the same as the source element type
     * (<code>dest.{@link Matrix#elementType() elementType()}!=src.{@link Matrix#elementType() elementType()}</code>),
     * the elements are automatically cast to the necessary type. More precisely, in this case
     * the <code>dest</code> matrix, before all further calculations, is replaced with
     *
     * <pre>
     * {@link
     * Matrices#asUpdatableFuncMatrix(boolean, net.algart.math.functions.Func.Updatable, Class, Matrix)
     * Matrices.asUpdatableFuncMatrix}(true, {@link net.algart.math.functions.Func#UPDATABLE_IDENTITY
     * Func.UPDATABLE_IDENTITY}, src.updatableType(UpdatablePArray.class), dest)
     * </pre>
     *
     * <p>We do not recommend to pass matrices with different element types: it can slow down calculations.
     *
     * @param dest                    the target matrix.
     * @param src                     the source matrix.
     * @param pattern                 the pattern.
     * @param disableMemoryAllocation if <code>false</code>, this method may allocate additional temporary matrices
     *                                for optimizing the algorithm speed;
     *                                if <code>true</code>, no any work memory will be allocated.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    void dilation(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern,
            boolean disableMemoryAllocation);

    /**
     * Equivalent to {@link #erosion(Matrix, Pattern)} method, but the result matrix
     * will be placed in the <code>dest</code> argument.
     * It allows to avoid extra memory allocation if you need to perform erosion many times
     * from one matrix to another.
     *
     * <p>Moreover, if <code>disableMemoryAllocation</code> argument is <code>true</code>, this method
     * guarantees that no any additional memory will be allocated, even if it can optimize the algorithm speed.
     * In this case, this method is always executed in one pass:
     * it is equivalent to creating new lazy matrix by {@link #asDilation(Matrix src, Pattern pattern)} method
     * and further copying it into <code>dest</code> by {@link Matrices#copy(ArrayContext, Matrix, Matrix)} method.
     * It can be useful if you are sure that the pattern is small enough (usually 2-10 points),
     * and allocation additional work matrices can slow down the algorithm to greater extent
     * than using the simple one-pass algorithm.
     *
     * <p>If the element type of the <code>dest</code> matrix is not the same as the source element type
     * (<code>dest.{@link Matrix#elementType() elementType()}!=src.{@link Matrix#elementType() elementType()}</code>),
     * the elements are automatically cast to the necessary type. More precisely, in this case
     * the <code>dest</code> matrix, before all further calculations, is replaced with
     *
     * <pre>
     * {@link
     * Matrices#asUpdatableFuncMatrix(boolean, net.algart.math.functions.Func.Updatable, Class, Matrix)
     * Matrices.asUpdatableFuncMatrix}(true, {@link net.algart.math.functions.Func#UPDATABLE_IDENTITY
     * Func.UPDATABLE_IDENTITY}, src.updatableType(UpdatablePArray.class), dest)
     * </pre>
     *
     * <p>We do not recommend to pass matrices with different element types: it can slow down calculations.
     *
     * @param dest                    the target matrix.
     * @param src                     the source matrix.
     * @param pattern                 the pattern.
     * @param disableMemoryAllocation if <code>false</code>, this method may allocate additional temporary matrices
     *                                for optimizing the algorithm speed;
     *                                if <code>true</code>, no any work memory will be allocated.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    void erosion(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern,
            boolean disableMemoryAllocation);


    /**
     * Equivalent to {@link #dilation(Matrix, Matrix, Pattern, boolean) dilation(dest, src, pattern, false)}.
     *
     * @param dest                    the target matrix.
     * @param src                     the source matrix.
     * @param pattern                 the pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    void dilation(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Equivalent to {@link #erosion(Matrix, Matrix, Pattern, boolean) erosion(dest, src, pattern, false)}.
     *
     * @param dest                    the target matrix.
     * @param src                     the source matrix.
     * @param pattern                 the pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    void erosion(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the result of sequential
     * {@link #dilation(Matrix, Pattern) dilation(src, dilationPattern)} and
     * {@link #erosion(Matrix, Pattern) erosion(src, erosionPattern)}
     * of the source matrix by the specified patterns.
     *
     * <p>If <code>subtractionMode</code> is not {@link SubtractionMode#NONE},
     * the behavior is little other: this method returns the difference between
     * the result of these two operation and the <code>src</code> matrix, according the specified mode.
     *
     * <p>When both patterns are equal, the result is the {@link #closing(Matrix, Pattern, Morphology.SubtractionMode)
     * closing} of the matrix.
     *
     * @param src             the source matrix.
     * @param dilationPattern the pattern for dilation.
     * @param erosionPattern  the pattern for erosion.
     * @param subtractionMode whether the difference with the source matrix should be returned.
     * @return                the result of sequential dilation and erosion of the source matrix by the given patterns
     *                        or the difference of such result and the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>dilationPattern.{@link Pattern#dimCount() dimCount()}</code> or
     *                                  <code>erosionPattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> dilationErosion(
            Matrix<? extends PArray> src,
            Pattern dilationPattern, Pattern erosionPattern, SubtractionMode subtractionMode);

    /**
     * Returns a new updatable matrix, containing the result of sequential
     * {@link #erosion(Matrix, Pattern) erosion(src, erosionPattern)} and
     * {@link #dilation(Matrix, Pattern) dilation(src, dilationPattern)}
     * of the source matrix by the specified patterns.
     *
     * <p>If <code>subtractionMode</code> is not {@link SubtractionMode#NONE},
     * the behaviour is little other: this method returns the difference between
     * the result of these two operation and the <code>src</code> matrix, according the specified mode.
     *
     * <p>When both patterns are equal, the result is the {@link #opening(Matrix, Pattern, Morphology.SubtractionMode)
     * opening} of the matrix.
     *
     * @param src             the source matrix.
     * @param erosionPattern  the pattern for erosion.
     * @param dilationPattern the pattern for dilation.
     * @param subtractionMode whether the difference with the source matrix should be returned.
     * @return                the result of sequential erosion and dilation of the source matrix by the given patterns
     *                        or the difference of such result and the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>dilationPattern.{@link Pattern#dimCount() dimCount()}</code> or
     *                                  <code>erosionPattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> erosionDilation(
            Matrix<? extends PArray> src,
            Pattern erosionPattern, Pattern dilationPattern, SubtractionMode subtractionMode);

    /**
     * Returns a new updatable matrix, containing the <i>closing</i>
     * of the source matrix by the specified pattern.
     *
     * <p><i>Closing</i> means the result of sequential performing
     * {@link #dilation(Matrix, Pattern) dilation} and {@link #erosion(Matrix, Pattern) erosion} of the source matrix
     * with the same pattern.
     *
     * <p>If <code>subtractionMode</code> is not {@link SubtractionMode#NONE},
     * the behaviour is little other: this method returns the difference between
     * the closing and the <code>src</code> matrix, according the specified mode.
     * For example, {@link SubtractionMode#SUBTRACT_SRC_FROM_RESULT} argument
     * with this method allows to remove "light" background from a gray-scale image,
     * represented by <code>src</code> matrix.
     *
     * <p>This method is equivalent to {@link #dilationErosion(Matrix, Pattern, Pattern, Morphology.SubtractionMode)
     * dilationErosion(src, pattern, pattern, subtractionMode)}.
     *
     * <p>Please see
     * <a href="http://en.wikipedia.org/wiki/Closing_%28morphology%29">Wikipedia</a>
     * to know more about the closing.
     *
     * @param src             the source matrix.
     * @param pattern         the pattern.
     * @param subtractionMode whether the difference of the closing and the source matrix should be returned.
     * @return                the result of closing of the source matrix by the given pattern
     *                        or the difference of the closing and the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> closing(
            Matrix<? extends PArray> src, Pattern pattern,
            SubtractionMode subtractionMode);

    /**
     * Returns a new updatable matrix, containing the <i>opening</i>
     * of the source matrix by the specified pattern.
     *
     * <p><i>Opening</i> means the result of sequential performing
     * {@link #erosion(Matrix, Pattern) erosion} and {@link #dilation(Matrix, Pattern) dilation} of the source matrix
     * with the same pattern.
     *
     * <p>If <code>subtractionMode</code> is not {@link SubtractionMode#NONE},
     * the behaviour is little other: this method returns the difference between
     * the opening and the <code>src</code> matrix, according the specified mode.
     * For example, {@link SubtractionMode#SUBTRACT_RESULT_FROM_SRC} argument
     * with this method allows to remove "dark" background from a gray-scale image,
     * represented by <code>src</code> matrix.
     *
     * <p>This method is equivalent to {@link #erosionDilation(Matrix, Pattern, Pattern, Morphology.SubtractionMode)
     * erosionDilation(src, pattern, pattern, subtractionMode)}.
     *
     * <p>Please see
     * <a href="http://en.wikipedia.org/wiki/Opening_%28morphology%29">Wikipedia</a>
     * to know more about the opening.
     *
     * @param src             the source matrix.
     * @param pattern         the pattern.
     * @param subtractionMode whether the difference of the opening and the source matrix should be returned.
     * @return                the result of opening of the source matrix by the given pattern
     *                        or the difference of the opening and the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> opening(
            Matrix<? extends PArray> src, Pattern pattern,
            SubtractionMode subtractionMode);

    /**
     * Returns a new updatable matrix, containing the <i>weak dilation</i>
     * of the source matrix by the specified pattern.
     *
     * <p><i>Weak dilation</i> of the matrix <code>A</code> is defined as an elementwise difference
     * <code>B={@link #dilation(Matrix, Pattern)
     * dilation}(A)-({@link #closing(Matrix, Pattern, Morphology.SubtractionMode) closing}(A)-A)</code>.
     * It is obvious that, for any elements, <code>A&lt;=B&lt;={@link #dilation(Matrix, Pattern) dilation}(A)</code>
     * (because both differences
     * <code>{@link #dilation(Matrix, Pattern)
     * dilation}(A)-{@link #closing(Matrix, Pattern, Morphology.SubtractionMode) closing}(A)</code>
     * and <code>{@link #closing(Matrix, Pattern, Morphology.SubtractionMode) closing}(A)-A</code> are non-negative).
     *
     * <p>(In this method, the {@link #closing(Matrix, Pattern, Morphology.SubtractionMode) closing} is supposed
     * to be performed with the last argument {@link SubtractionMode#NONE}.)
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the result of weak dilation of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> weakDilation(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>weak erosion</i>
     * of the source matrix by the specified pattern.
     *
     * <p><i>Weak erosion</i> of the matrix <code>A</code> is defined as an elementwise sum
     * <code>B={@link #erosion(Matrix, Pattern)
     * erosion}(A)+(A-{@link #opening(Matrix, Pattern, Morphology.SubtractionMode) opening}(A))</code>.
     * It is obvious that, for any elements, <code>A&gt;=B&gt;={@link #erosion(Matrix, Pattern) erosion}(A)</code>
     * (because both differences
     * <code>{@link #opening(Matrix, Pattern, Morphology.SubtractionMode)
     * opening}(A)-{@link #erosion(Matrix, Pattern) erosion}(A)</code>
     * and <code>A-{@link #opening(Matrix, Pattern, Morphology.SubtractionMode) opening}(A)</code> are non-negative).
     *
     * <p>(In this method, the {@link #opening(Matrix, Pattern, Morphology.SubtractionMode) opening} is supposed
     * to be performed with the last argument {@link SubtractionMode#NONE}.)
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the result of weak dilation of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> weakErosion(Matrix<? extends PArray> src, Pattern pattern);

    /**
     * Returns the elementwise minimum between the source matrix and the result of
     * <code>{@link #dilationErosion(Matrix, Pattern, Pattern, Morphology.SubtractionMode)
     * dilationErosion}(src, dilationPattern, erosionPattern, {@link SubtractionMode#NONE})</code> call.
     *
     * <p>Let <code>B</code> is the result of this method, <code>A</code> is the source matrix,
     * <code>Q</code> is <code>dilationPattern</code>, <code>P</code> is <code>erosionPattern</code>:<br>
     * <code>B=min(A,{@link #erosion(Matrix, Pattern)
     * erosion}({@link #dilation(Matrix, Pattern) dilation}(A,Q),P))</code><br> for any elements.
     * It is obvious that (for any elements) <code>A&gt;=B&gt;={@link #erosion(Matrix, Pattern) erosion}(A,P)</code>.
     * But if <code>Q</code> is a some "boundary" or "carcass" of the erosion pattern <code>P</code>,
     * then a stronger condition is true:
     * <code>A&gt;=B&gt;={@link #opening(Matrix, Pattern, Morphology.SubtractionMode) opening}(A,P)</code>.
     *
     * <p>More precisely, there is the following theorem.
     *
     * <p><b>If <code>Q</code> is a subset of <code>P</code> and the Minkowski sum <code>P&oplus;Q</code> is equal to
     * <code>P&oplus;P</code> (see {@link Pattern#carcass} method),
     * then <code>B&gt;={@link #opening(Matrix, Pattern, Morphology.SubtractionMode) opening}(A,P)</code>.</b>
     *
     * <p>Below is the proof for the binary case.
     * (For other element types, it's enough to consider the system of binary matrices
     * <code>A&gt;=threshold</code> for all possible real values <code>threshold</code>.)
     *
     * <blockquote>
     * <p>Let some point <i>x</i><code>&isin;{@link #opening(Matrix, Pattern, Morphology.SubtractionMode)
     * opening}(A,P)</code>.
     * It means: there is such <i>p</i><sub>1</sub><code>&isin;P</code>,
     * that for all <i>p</i><code>&isin;P</code> we have <i>x</i>+<i>p</i><sub>1</sub>-<i>p</i><code>&isin;A</code>
     * (the <b>statement <i>A</i></b>).
     * We already know, that <i>x</i><code>&isin;A</code> (the case <i>p</i>=<i>p</i><sub>1</sub>),
     * and we also need to prove, that <i>x</i><code>&isin;{@link #erosion(Matrix, Pattern)
     * erosion}({@link #dilation(Matrix, Pattern) dilation}(A,Q),P)</code>.
     *
     * <p>Let's suppose that it is not true. It means: there is such <i>p</i><sub>2</sub><code>&isin;P</code>,
     * that for all <i>q</i><code>&isin;Q</code> we have <i>x</i>+<i>p</i><sub>2</sub>-<i>q</i><code>&notin;A</code>
     * (the <b>statement <i>B</i></b>)
     *
     * <p>Let <i>x</i> will be the origin of coordinates: <i>x</i>=<b>0</b>. Then, let
     * <code>P</code><sub>1</sub>=-<code>P</code>+<i>p</i><sub>1</sub>={<i>p</i><sub>1</sub>-<i>p</i>,
     * <i>p</i><code>&isin;P</code>}. Note: the origin <b>0</b><code>&isin;P</code><sub>1</sub>
     * (the case <i>p</i>=<i>p</i><sub>1</sub>). We have <code>P</code><sub>1</sub><code>&sub;A</code>
     * (statement <i>A</i>), so,
     * for all <i>q</i><code>&isin;Q</code> we have <i>p</i><sub>2</sub>-<i>q</i><code>&notin;P</code><sub>1</sub>
     * (because <i>p</i><sub>2</sub>-<i>q</i><code>&notin;A</code>, statement <i>B</i>).
     * In other words, <i>p</i><sub>2</sub><code>&notin;P<sub>1</sub>&oplus;Q</code> (dilation of P by Q,
     * or Minkowski sum of P and Q).
     * On the other hand, it's obvious that <i>p</i><sub>2</sub><code>&isin;P<sub>1</sub>&oplus;P</code>,
     * because <b>0</b><code>&isin;P</code><sub>1</sub> and, so,
     * <code>P&sub;P&oplus;P<sub>1</sub>=P<sub>1</sub>&oplus;P</code>.
     *
     * <p>There is a contradiction: according to the condition, there must be
     * <code>P<sub>1</sub>&oplus;P=P<sub>1</sub>&oplus;Q</code>. The theorem is proved.
     * </blockquote>
     *
     * <p>This fact allows to interpret this method, if <code>dilationPattern</code>
     * is a "boundary" of <code>erosionPattern</code> (usually {@link UniformGridPattern#surface()}
     * or a similar point set), as a "weak" analog of opening.
     * For binary images, it helps to remove small isolated objects, but (unlike usual opening)
     * to preserve thin structures.
     *
     * @param src             the source matrix.
     * @param dilationPattern the pattern for dilation.
     * @param erosionPattern  the pattern for erosion.
     * @return                the elementwise minimum between the source matrix and
     *                        its sequential dilation and erosion by the given patterns.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>dilationPattern.{@link Pattern#dimCount() dimCount()}</code> or
     *                                  <code>erosionPattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> maskedDilationErosion(
            Matrix<? extends PArray> src,
            Pattern dilationPattern, Pattern erosionPattern);

    /**
     * Returns the elementwise maximum between the source matrix and the result of
     * <code>{@link #erosionDilation(Matrix, Pattern, Pattern, Morphology.SubtractionMode)
     * erosionDilation}(src, erosionPattern, dilationPattern, {@link SubtractionMode#NONE})</code> call.
     *
     * <p>This is an inverse method for {@link #maskedDilationErosion(Matrix, Pattern, Pattern)}.
     *
     * @param src             the source matrix.
     * @param erosionPattern  the pattern for erosion.
     * @param dilationPattern the pattern for dilation.
     * @return                the elementwise maximum between the source matrix and
     *                        its sequential erosion and dilation by the given patterns.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>dilationPattern.{@link Pattern#dimCount() dimCount()}</code> or
     *                                  <code>erosionPattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> maskedErosionDilation(
            Matrix<? extends PArray> src,
            Pattern erosionPattern, Pattern dilationPattern);


    /**
     * Returns a new updatable matrix, containing the <i>Beucher gradient</i>
     * of the source matrix by the specified pattern, that means
     * the elementwise difference between {@link #dilation(Matrix, Pattern) dilation}
     * and {@link #erosion(Matrix, Pattern) erosion} of the source matrix with the same pattern.
     *
     * <p>More precisely, the <i>Beucher gradient</i> of the matrix <code>A</code> is defined
     * as an elementwise positive difference
     * <code>B=max(0,{@link #dilation(Matrix, Pattern) dilation}(A)-{@link
     * #erosion(Matrix, Pattern) erosion}(A))</code>.
     *
     * <p>The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     * The <code>byte</code> and <code>short</code> elements are considered to be unsigned.
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the Beucher gradient of the source matrix by the given pattern.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    Matrix<? extends UpdatablePArray> beucherGradient(Matrix<? extends PArray> src, Pattern pattern);
}
