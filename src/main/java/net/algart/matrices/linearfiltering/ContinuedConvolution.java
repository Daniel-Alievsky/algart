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

package net.algart.matrices.linearfiltering;

import net.algart.arrays.*;
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.WeightedPattern;
import net.algart.matrices.DependenceApertureBuilder;

import java.util.Objects;

/**
 * <p>The filter allowing to transform any {@link Convolution} object to another instance of that interface,
 * which uses some non-trivial form of continuation outside the source matrix.</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * implementing {@link Convolution}, and some {@link net.algart.arrays.Matrix.ContinuationMode continuation mode}.
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
 * The size of extension along all coordinates depends on the pattern and the performed operation.
 * After this, the corresponding method of <i>parent</i> object processes the appended matrix,
 * and the method of this class returns the corresponding submatrix of the result, with dimensions, equal
 * to the dimensions of the source matrix.</p>
 *
 * <p>The processing is little different for the method
 * {@link #convolution(Matrix, Matrix, WeightedPattern)},
 * placing the result into the first argument.
 * In this case, the destination (updatable) matrix is (virtually) extended like the source matrix
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
 * {@link #getInstance(Convolution, Matrix.ContinuationMode)} leads
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
public class ContinuedConvolution implements Convolution {
    private final Convolution parent;
    private final ArrayContext context;
    private final Matrix.ContinuationMode continuationMode;

    private ContinuedConvolution(Convolution parent, Matrix.ContinuationMode continuationMode) {
        Objects.requireNonNull(parent, "Null parent convolution");
        Objects.requireNonNull(continuationMode, "Null continuationMode derivator");
        if (continuationMode == Matrix.ContinuationMode.NONE) {
            throw new IllegalArgumentException(getClass().getName() + " cannot be used with continuation mode \""
                + continuationMode + "\"");
        }
        this.parent = parent;
        this.context = parent.context() == null ? ArrayContext.DEFAULT : parent.context();
        this.continuationMode = continuationMode;
    }

    /**
     * Returns new instance of this class with the passed parent {@link Convolution} object
     * and the specified continuation mode.
     * See comments to {@link net.algart.arrays.Matrix.ContinuationMode} class
     * for more information about possible continuations.
     *
     * @param parent           the instance of {@link Convolution} interface that will perform all operations.
     * @param continuationMode the mode of continuation outside the source matrix.
     * @return                 new instance of this class.
     * @throws NullPointerException     if <code>parent</code> or <code>continuationMode</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</code>.
     * @see #parent()
     * @see #continuationMode()
     */
    public static ContinuedConvolution getInstance(Convolution parent, Matrix.ContinuationMode continuationMode) {
        return new ContinuedConvolution(parent, continuationMode);
    }

    /**
     * Returns the parent {@link Convolution} object, passed to
     * {@link #getInstance(Convolution, Matrix.ContinuationMode)} method.
     *
     * @return the parent {@link Convolution} object.
     */
    public Convolution parent() {
        return this.parent;
    }

    /**
     * Returns the continuation mode, used by this object for virtual continuing the source matrix.
     * The returned value is equal to the corresponding argument of
     * {@link #getInstance(Convolution, Matrix.ContinuationMode)} method.
     *
     * @return the continuation mode, that will be used for continuation outside the source matrix.
     */
    public Matrix.ContinuationMode continuationMode() {
        return this.continuationMode;
    }

    public ArrayContext context() {
        return this.context;
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
    public Convolution context(ArrayContext newContext) {
        return new ContinuedConvolution(parent.context(newContext), continuationMode);
    }

    public boolean isPseudoCyclic() {
        return this.continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC;
    }

    public double increment(Class<?> elementType) {
        return parent.increment(elementType);
    }

    public Matrix<? extends PArray> asConvolution(Matrix<? extends PArray> src, WeightedPattern pattern) {
        Continuer c = new Continuer(null, src, pattern);
        return c.reduce(parent.asConvolution(c.continuedSrc(), pattern));
    }

    public <T extends PArray> Matrix<T> asConvolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern)
    {
        Continuer c = new Continuer(null, src, pattern);
        return c.reduce(parent.asConvolution(requiredType, c.continuedSrc(), pattern));
    }

    public Matrix<? extends UpdatablePArray> convolution(Matrix<? extends PArray> src, WeightedPattern pattern) {
        Continuer c = new Continuer(null, src, pattern);
        return c.reduce(parent.convolution(c.continuedSrc(), pattern));
    }

    public <T extends PArray> Matrix<? extends T> convolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern)
    {
        Continuer c = new Continuer(null, src, pattern);
        return c.reduce(parent.convolution(requiredType, c.continuedSrc(), pattern));
    }

    public void convolution(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        WeightedPattern pattern)
    {
        Continuer c = new Continuer(dest, src, pattern);
        parent.convolution(c.continuedDest(), c.continuedSrc(), pattern);
    }

    private class Continuer {
        private final Matrix<? extends UpdatablePArray> continuedDest;
        private final Matrix<? extends PArray> continuedSrc;
        private final IRectangularArea aperture;

        private Continuer(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
            if (dest != null) {
                Matrices.checkDimensionEquality(dest, src);
            }
            DependenceApertureBuilder builder = dest == null && parent.isPseudoCyclic() ?
                DependenceApertureBuilder.SUM :
                DependenceApertureBuilder.SUM_MAX_0;
            this.aperture = builder.getAperture(src.dimCount(), pattern, false);
            this.continuedSrc = DependenceApertureBuilder.extend(src, aperture, continuationMode);
            this.continuedDest = dest == null ? null :
                DependenceApertureBuilder.extend(dest, aperture, Matrix.ContinuationMode.ZERO_CONSTANT);
        }

        public Matrix<? extends PArray> continuedSrc() {
            return continuedSrc;
        }

        public Matrix<? extends UpdatablePArray> continuedDest() {
            assert continuedDest != null;
            return continuedDest;
        }

        public <T extends PArray> Matrix<T> reduce(Matrix<T> matrix) {
            assert continuedDest == null;
            return DependenceApertureBuilder.reduce(matrix, aperture);
        }
    }
}
