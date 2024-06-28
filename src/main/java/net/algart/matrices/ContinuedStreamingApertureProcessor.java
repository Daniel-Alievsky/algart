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

import net.algart.arrays.ArrayContext;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.Pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>The filter allowing to transform any {@link StreamingApertureProcessor streaming aperture procesor}
 * to another instance of that class, which uses some non-trivial form of continuation outside the source matrix.</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * extending {@link StreamingApertureProcessor}, and some
 * {@link net.algart.arrays.Matrix.ContinuationMode continuation mode}.
 * This object works almost identically to the parent object with the only exception,
 * that it uses the specified continuation model instead of the default pseudo-cyclic continuation,
 * described in comments to {@link StreamingApertureProcessor}.
 *
 * <p>More precisely, both
 * {@link #asProcessed(Class, Matrix, List, Pattern)} and
 * {@link #process(Matrix, Matrix, List, Pattern)} methods of this object call
 * the corresponding methods of the parent one.
 * But before calling them, the source matrix <b>M</b> is appended (outside its bounds)
 * by some area of additional values
 * with help of {@link Matrix#subMatrix(long[], long[], net.algart.arrays.Matrix.ContinuationMode)} method,
 * using the {@link #continuationMode() continuation mode of this filter},
 * and all additional matrices
 * <b>M</b><sub>0</sub>, <b>M</b><sub>1</sub>, ..., <b>M</b><sub><i>K</i>&minus;1</sub>
 * are analogously appended until the same sizes with zeros
 * ({@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT}).
 * So, all dimensions of the appended matrices are little greater than the corresponding dimensions
 * of the source ones, and extra elements are (virtually) filled according the given continuation mode
 * (the source matrix <b>M</b>) or by zeros (additional matrices <b>M</b><sub><i>i</i></sub>).
 * The size of extension along all coordinates depends on the pattern (aperture shape).
 * After this, the corresponding method of <i>parent</i> object processes the appended matrix.
 * In a case of {@link #asProcessed(Class, Matrix, List, Pattern) asProcessed} method,
 * the corresponding submatrix of the result is extracted, with dimensions, equal
 * to the dimensions of the source matrix (by using {@link Matrix#subMatrix(long[], long[])} method),
 * and the method returns this submatrix.
 * In a case of {@link #process(Matrix, Matrix, List, Pattern) process} method,
 * the destination (updatable) <b>R</b> matrix is (virtually) extended like the source <b>M</b> matrix
 * before calculations with help of
 * {@link Matrix#subMatrix(long[], long[], net.algart.arrays.Matrix.ContinuationMode)} method;
 * so, the calculated data are correctly stored into the original <b>R</b> matrix.</p>
 *
 * <p>The appending along all coordinates is performed in such a way, that the resulting matrix
 * is the same, as if the source matrix would be virtually appended until the infinity along all directions,
 * according the selected {@link #continuationMode() continuation mode}.</p>
 *
 * <p>Note:  we append the additional matrices <b>M</b><sub><i>k</i></sub> with <code>0.0</code> constant
 * instead of using the selected continuation mode. It is possible (as well as appending with any other values)
 * because the element of the resulting matrix <b>R</b>
 * at every position <b>x</b>, according the basic specification of {@link StreamingApertureProcessor} class,
 * should depend only on the elements
 * <i>w</i><sub>0</sub>, <i>w</i><sub>1</sub>, ..., <i>w</i><sub><i>K</i>&minus;1</sub>
 * of the additional matrices <b>M</b><sub><i>k</i></sub>, placed at the same position <b>x</b>.</p>
 *
 * <p>Note: {@link net.algart.arrays.Matrix.ContinuationMode#NONE} continuation mode cannot be used in this class:
 * such value of <code>continuationMode</code> argument of the instantiation method
 * {@link #getInstance(StreamingApertureProcessor, Matrix.ContinuationMode)} leads
 * to <code>IllegalArgumentException</code>.</p>
 *
 * <p>Note: in an improbable case, when the dimensions of the source matrix and/or
 * the sizes of the pattern are extremely large (about 2<sup>63</sup>),
 * so that the necessary appended matrices should have dimensions or total number of elements,
 * greater than <code>Long.MAX_VALUE</code>,
 * the methods of this class throw <code>IndexOutOfBoundsException</code> and do nothing.
 * Of course, if is very improbable case.</p>
 *
 * <p>This class is an example of <i>non-standard implementation</i> of streaming aperture processor,
 * which does not comply with the strict definition from the comments to
 * {@link StreamingApertureProcessor} class. (But this implementation is still standard,
 * if {@link #continuationMode()} is {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC PSEUDO_CYCLIC}
 * and the {@link #parent() parent} processor is standard.)</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class ContinuedStreamingApertureProcessor extends StreamingApertureProcessor {
    private final StreamingApertureProcessor parent;
    private final Matrix.ContinuationMode continuationMode;

    private ContinuedStreamingApertureProcessor(
            StreamingApertureProcessor parent,
            Matrix.ContinuationMode continuationMode) {
        super(parent.context()); // be careful: context can be null!
        Objects.requireNonNull(continuationMode, "Null continuationMode derivator");
        if (continuationMode == Matrix.ContinuationMode.NONE) {
            throw new IllegalArgumentException(getClass().getName() + " cannot be used with continuation mode \""
                    + continuationMode + "\"");
        }
        this.parent = parent;
        this.continuationMode = continuationMode;
    }

    /**
     * Returns new instance of this class with the passed parent {@link StreamingApertureProcessor} object
     * and the specified continuation mode.
     * See comments to {@link net.algart.arrays.Matrix.ContinuationMode} class
     * for more information about possible continuations.
     *
     * @param parent           the instance of {@link StreamingApertureProcessor} class
     *                         that will perform all operations.
     * @param continuationMode the mode of continuation outside the source matrix.
     * @return new instance of this class.
     * @throws NullPointerException     if <code>parent</code> or <code>continuationMode</code>
     *                                  argument is {@code null}.
     * @throws IllegalArgumentException if <code>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</code>.
     * @see #parent()
     * @see #continuationMode()
     */
    public static ContinuedStreamingApertureProcessor getInstance(
            StreamingApertureProcessor parent,
            Matrix.ContinuationMode continuationMode) {
        return new ContinuedStreamingApertureProcessor(parent, continuationMode);
    }

    /**
     * Returns the parent {@link StreamingApertureProcessor} object, passed to
     * {@link #getInstance(StreamingApertureProcessor, Matrix.ContinuationMode)} method.
     *
     * @return the parent {@link StreamingApertureProcessor} object.
     */
    public StreamingApertureProcessor parent() {
        return this.parent;
    }

    /**
     * Returns the continuation mode, used by this object for virtual continuing the source matrix.
     * The returned mode is equal to the corresponding argument of
     * {@link #getInstance(StreamingApertureProcessor, Matrix.ContinuationMode)} method.
     *
     * @return the continuation mode, that will be used for continuation outside the source matrix.
     */
    public Matrix.ContinuationMode continuationMode() {
        return this.continuationMode;
    }

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <code>newContext</code> for all operations.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * @param newContext another context, used by the returned instance; can be {@code null}.
     * @return new instance with another context.
     */
    @Override
    public StreamingApertureProcessor context(ArrayContext newContext) {
        return new ContinuedStreamingApertureProcessor(parent.context(newContext), continuationMode);
    }

    @Override
    public boolean isStandardImplementation() {
        return parent.isStandardImplementation() && this.continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC;
    }

    @Override
    public <T extends PArray> Matrix<T> asProcessed(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> src,
            List<? extends Matrix<? extends PArray>> additionalMatrices,
            Pattern pattern) {
        Objects.requireNonNull(additionalMatrices, "Null additionalMatrices argument");
        additionalMatrices = new ArrayList<Matrix<? extends PArray>>(additionalMatrices);
        // - to avoid changing by parallel threads
        checkArguments(src, src, additionalMatrices, pattern);
        IRectangularArea a = DependenceApertureBuilder.SUM_MAX_0.getAperture(src.dimCount(), pattern, false);
        // SUM_MAX_0 can be necessary if the parent processor is not pseudo-cyclic
        Matrix<? extends PArray> continued = DependenceApertureBuilder.extend(src, a, continuationMode);
        additionalMatrices = extendAdditionalMatrices(additionalMatrices, a);
        Matrix<T> parentResult = parent.asProcessed(requiredType, continued, additionalMatrices, pattern);
        return DependenceApertureBuilder.reduce(parentResult, a);
    }

    @Override
    public void process(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
            List<? extends Matrix<? extends PArray>> additionalMatrices,
            Pattern pattern) {
        Objects.requireNonNull(additionalMatrices, "Null additionalMatrices argument");
        additionalMatrices = new ArrayList<Matrix<? extends PArray>>(additionalMatrices);
        // - to avoid changing by parallel threads
        checkArguments(dest, src, additionalMatrices, pattern);
        IRectangularArea a = DependenceApertureBuilder.SUM_MAX_0.getAperture(src.dimCount(), pattern, false);
        // SUM_MAX_0: including the origin is necessary while extending the destination matrix
        Matrix<? extends PArray> continued = DependenceApertureBuilder.extend(src, a, continuationMode);
        additionalMatrices = extendAdditionalMatrices(additionalMatrices, a);
        Matrix<? extends UpdatablePArray> continuedDest = DependenceApertureBuilder.extend(dest, a,
                Matrix.ContinuationMode.ZERO_CONSTANT);
        parent.process(continuedDest, continued, additionalMatrices, pattern);
    }

    private static List<? extends Matrix<? extends PArray>> extendAdditionalMatrices(
            List<? extends Matrix<? extends PArray>> matrices,
            IRectangularArea aperture) {
        if (matrices.isEmpty()) {
            return matrices;
        }
        List<Matrix<? extends PArray>> continued = new ArrayList<Matrix<? extends PArray>>();
        for (Matrix<? extends PArray> m : matrices) {
            long[] from = aperture.min().coordinates();
            long[] to = IPoint.valueOf(m.dimensions()).add(aperture.max()).coordinates();
            continued.add(m.size() == 0 ? m : m.subMatrix(from, to, Matrix.ContinuationMode.ZERO_CONSTANT));
            // outside values are not important
        }
        return continued;
    }
}
