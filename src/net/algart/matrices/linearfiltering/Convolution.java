/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.linearfiltering;

import net.algart.arrays.*;
import net.algart.math.patterns.WeightedPattern;

public interface Convolution extends ArrayProcessorWithContextSwitching {

    public Convolution context(ArrayContext newContext);

    /**
     * Returns <tt>true</tt>, if this class works in the default
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC
     * pseudo-cyclic continuation mode}.
     *
     * <p>More precisely, it means that when the value in some element of the processed matrix,
     * returned by a method of this class, depends on elements of the source matrix, lying outside its bounds,
     * then it is supposed that the values outside the source matrix are calculated as described in
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC}. Exactly such behaviour is specified in
     * the comments to the {@link #convolution(Matrix, WeightedPattern)}
     * method as the default definition of convolution.
     *
     * <p>This method returns <tt>true</tt> in {@link BasicConvolution} implementation.
     * However, it usually returns <tt>false</tt> in {@link ContinuedConvolution} class
     * &mdash; excepting the only degenerated case when the used
     * {@link ContinuedConvolution#continuationMode() continuation mode} is
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC PSEUDO_CYCLIC}.
     *
     * @return whether this class works in the pseudo-cyclic continuation mode.
     */
    public boolean isPseudoCyclic();

    public double increment(Class<?> elementType);
    /**
     * Equivalent to <tt>{@link #asConvolution(Class, Matrix, WeightedPattern)
     * asConvolution}(src.{@link Matrix#type(Class) type}(PArray.class), src, pattern)</tt>.
     * In other words, the element type of the returned matrix is chosen the same as in <tt>src</tt> matrix.
     *
     * @param src          the source matrix.
     * @param pattern      the pattern.
     * @return             the "lazy" matrix containing the convolution of the source matrix with the given pattern.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     */
    public Matrix<? extends PArray> asConvolution(Matrix<? extends PArray> src, WeightedPattern pattern);

    /**
     * Returns an immutable view of the passed source matrix,
     * such that any reading data from it calculates and returns the <i>convolution</i>
     * of the source matrix by the specified pattern.
     * See {@link #convolution(Class, Matrix, WeightedPattern)} method about the "<i>convolution</i>" term.
     *
     * <p>The matrix, returned by this method, is immutable, and the class of its built-in array
     * implements one of the basic interfaces
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray} or {@link DoubleArray}.
     * The class of desired interface (one of 8 possible classes) must be passed as <tt>requiredType</tt> argument.
     * So, it defines the element type of the returned matrix.
     * For example, if <tt>requiredType={@link ByteArray}.class</tt>, the returned matrix consists of <tt>byte</tt>
     * elements. The rules of casting the real numbers, results of the convolution, to the desired element type
     * are the same as in
     * {@link Arrays#asFuncArray(boolean, net.algart.math.functions.Func, Class, PArray...)}
     * method with the argument <tt>truncateOverflows=true</tt>.
     *
     * <p>The result is usually "lazy", that means that this method finishes immediately and all
     * actual calculations are performed while getting elements of the returned matrix.
     * It is true for all implementations provided by this package.
     * However, some implementations may not support lazy dilation;
     * then this method will be equivalent to {@link #convolution(Class, Matrix, WeightedPattern)}.
     *
     * <p>Please note: this method does not require time, but the resulting matrix can work slowly!
     * for example, reading all its content than work much slower than
     * {@link #convolution(Class, Matrix, WeightedPattern)} method for some complex patterns.
     * Usually you should use it only for very little patterns, or if you know that the implementation
     * of this interface does not provide better algorithm for non-"lazy"
     * {@link #convolution(Class, , Matrix, WeightedPattern)} method.
     *
     * @param requiredType desired type of the built-in array in the returned matrix.
     * @param src          the source matrix.
     * @param pattern      the pattern.
     * @return             the "lazy" matrix containing the convolution of the source matrix with the given pattern.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     */
    public <T extends PArray> Matrix<T> asConvolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern);

    /**
     * Returns a new updatable matrix, containing the <i>convolution</i>
     * of the source matrix by the specified pattern.
     *
     * <p>Usually <i>convolution</i> means the weighted sum of the set of matrices,
     * obtained by pseudo-cyclic shifting the source matrix by the vectors,
     * equal to all pattern points, with weights, equal to
     * {@link WeightedPattern#weight(net.algart.math.IPoint) weights}
     * of the pattern points.
     * More precisely, let <i>m</i><sub><i>i</i></sub><tt>={@link Matrices#asShifted(Matrix, long...)
     * Matrices.asShifted}(src,ip.{@link net.algart.math.IPoint#coordinates() coordinates()})</tt>,
     * where <tt>ip</tt> is the point <tt>#<i>i</i></tt> from all points contained in the pattern,
     * and let <i>w</i><i>i</i>=<tt>pattern.{@link WeightedPattern#weight(net.algart.math.IPoint)
     * weight}(ip)</tt>.
     * Then the every element of the returned matrix is the weighted sum of all corresponding elements
     * of all <i>m</i><sub><i>i</i></sub> matrices:</p>
     *
     * <blockquote>
     * <big>&sum;</big> <i>w</i><sub><i>i</i></sub><i>m</i><sub><i>i</i></sub>
     * </blockquote>
     *
     * The {@link Matrix#elementType() element type}
     * of the created matrix is the same as the element type of the source one.
     * The <tt>byte</tt> and <tt>short</tt> elements are considered to be unsigned.
     * If the element type if integer, the precise is rounded to the nearest integer.
     *
     * <p>The {@link BasicConvolution} class strictly complies this definition.
     * However, other implementations of this interface may use alternate definitions of the <i>convolution</i> term.
     * For example, elements outside the matrix may be supposed to be filled according some non-trivial rules
     * instead of pseudo-cyclic continuation
     * (as in {@link ContinuedConvolution} objects),
     * or only some region of the matrix may be processed, etc.
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the result of convolution of the source matrix with the given pattern.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     * @see #asConvolution(Class, Matrix, WeightedPattern)
     */
    public Matrix<? extends UpdatablePArray> convolution(Matrix<? extends PArray> src, WeightedPattern pattern);

    public <T extends PArray> Matrix<? extends T> convolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern);

    public void convolution(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        WeightedPattern pattern);
}
