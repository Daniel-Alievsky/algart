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

package net.algart.math.patterns;

import java.util.List;

import net.algart.math.IPoint;
import net.algart.math.Range;

public interface WeightedPattern extends Pattern {
    /**
     * Returns the weight of the given point of the pattern.
     * The result is undefined if this point is outside the pattern.
     *
     * @param point some {@link IPoint integer point}.
     * @return the weight of this point.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link IPoint#coordCount()
     *                                  coordCount()}!={@link #dimCount()}.
     */
    public double weight(IPoint point);

    /**
     * Returns the minimal and maximal weights of all points of this pattern.
     *
     * @return the minimal and maximal weights of all points of this pattern.
     */
    public Range weightRange();

    /**
     * Returns <tt>true</tt> if the weights of all points are the same.
     * Equivalent to <tt>{@link #weightRange()}.{@link Range#size() size()}==0.0</tt>.
     *
     * @return <tt>true</tt> if the weights of all points are the same.
     */
    public boolean isConstant();

    /**
     * Returns the pattern shifted by the argument, that is consisting of points
     * with the same {@link #weight(IPoint) weights},
     * generated from points of this instance by adding the argument via {@link IPoint#add(IPoint)} method.
     *
     * @param shift the shift.
     * @return the shifted pattern.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>point.{@link IPoint#coordCount()
     *                                  coordCount()}!={@link #dimCount()}.
     */
    public WeightedPattern shift(IPoint shift);

    /**
     * Returns the pattern consisting of points, generated from points of this instance
     * by multiplying on the <tt>mult</tt> argument via {@link IPoint#multiply(double)} method.
     *
     * <p>If <tt>mult</tt> is not an integer, the generated real coordinates are rounded to integer values.
     * If several source points are rounded to the same integer point, the {@link #weight(IPoint) weights}
     * of the resulting points may differ from the weights of the source ones,
     * but the sum of all weights will be approximately same.
     * If the all source points are transformed to different points,
     * their weights are preserved.
     *
     * <p><i>Please note:</i> if <tt>mult</tt> is not an integer,
     * the algorithm of rounding <i>is not strictly specified</i>!
     * However, you can be sure that the new pattern will be near from the precise result.
     *
     * @param multiplier the multiplier.
     * @return the product of this pattern and the given scalar <tt>mult</tt>.
     */
    public WeightedPattern multiply(double multiplier);

    public WeightedPattern scale(double... multipliers);

    /**
     * Returns the symmetric pattern: equivalent to {@link #multiply(double) multiply(-1.0)}.
     *
     * @return the symmetric pattern.
     */
    public WeightedPattern symmetric();

    /**
     * Returns the product decomposition:
     * the list of patterns such that the convolution with this pattern
     * is equivalent to sequential convolution with all patterns from the list.
     *
     * <p>Let the pattern consists of n-dimensional points <i>p</i><sub>1</sub>,&nbsp;<i>p</i><sub>2</sub>,&nbsp;...
     * with {@link #weight(IPoint) weights} <i>w</i><sub>1</sub>,&nbsp;<i>w</i><sub>2</sub>,&nbsp;...,
     * and let the integer vector <b>i</b> defines the position of some element of the n-dimensional
     * numeric matrix <b>A</b>.
     * Here the convolution by this pattern means transforming
     * the matrix <b>A</b> to another matrix <b>B</b> by the following rules:
     *
     * <blockquote>
     * <b>B</b>[<b>i</b>] = <big>&sum;</big>
     * <i>w</i><sub><i>k</i></sub><b>A</b>[<b>i</b>-<i>p</i><sub><i>k</i></sub>]
     * </blockquote>
     *
     * <p>This method allows to optimize calculation of this convolution with help of several passes:
     * first you calculate the convolution with the 1st element of the returned list,
     * then you calculate  the convolution of the new matrix with the 2nd element of the returned list,
     * etc. The last convolution will be equal (or almost equal, due to possible rounding errors)
     * to the convolution of the source matrix with full this pattern.
     *
     * <p>The simplest example of such decomposition is a case of the
     * {@link UniformGridPattern#isActuallyRectangular() rectangular} {@link #isConstant() constant} pattern.
     * In this case, this method should return a list of {@link #dimCount()} linear constant patterns
     * (one-dimensional segments), the minkowski sum of which is equal to this one.
     * The linear constant segments should not be further decomposed, because there is an optimal algorithm
     * for convolution of any matrix with a linear constant segment, requiring 2 operations per element only.
     *
     * <p>There is no guarantee that this method returns a good decomposition.
     * If this method cannot find required decomposition, it returns the list containing
     * this instance as the only element.
     *
     * <p>The number of space dimensions in all returned patterns ({@link #dimCount()} is the same as in this one.
     *
     * <p>The result of this method is immutable (<tt>Collections.unmodifiableList</tt>).
     *
     * @param minimalPointCount this method does not try to decompose patterns that contain
     *                          less than <tt>minimalPointCount</tt> points.
     * @return the decomposition of this pattern to the "product" (convolution) of smaller patterns.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<WeightedPattern> productDecomposition(int minimalPointCount);

}
