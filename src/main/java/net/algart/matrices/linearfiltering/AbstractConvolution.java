/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

public abstract class AbstractConvolution extends AbstractArrayProcessorWithContextSwitching implements Convolution {
    protected final boolean incrementForUnsigned;
    protected final boolean incrementByHalfForInteger;

    /**
     * Creates an instance of this class with the given context.
     *
     * @param context                   the context used by this instance for all operations.
     * @param incrementForUnsigned      if <tt>true</tt>, when the type of the convolution result is
     *                                  an unsigned number in terms of AlgART libraries &mdash;
     *                                  <tt>byte</tt>, <tt>short</tt>, <tt>char</tt> &mdash;
     *                                  it is automatically incremented by 128 (<tt>byte</tt>) or 32768
     *                                  (<tt>short</tt> and <tt>char</tt>).
     * @param incrementByHalfForInteger if <tt>true</tt>, when the type of the convolution result is integer,
     *                                  the precise result is automatically increments by 0.5 before casting.
     */
    protected AbstractConvolution(ArrayContext context,
        boolean incrementForUnsigned, boolean incrementByHalfForInteger)
    {
        super(context);
        this.incrementForUnsigned = incrementForUnsigned;
        this.incrementByHalfForInteger = incrementByHalfForInteger;
    }

    public Convolution context(ArrayContext newContext) {
        return (Convolution) super.context(newContext);
    }

    public abstract boolean isPseudoCyclic();

    public double increment(Class<?> elementType) {
        if (incrementForUnsigned) {
            if (elementType == byte.class) {
                return incrementByHalfForInteger ? 128.5 : 128.0;
            }
            if (elementType == short.class || elementType == char.class) {
                return incrementByHalfForInteger ? 32768.5 : 32768.0;
            }
        }
        if (incrementByHalfForInteger && (elementType == boolean.class || elementType == char.class ||
            elementType == byte.class || elementType == short.class ||
            elementType == int.class || elementType == long.class))
        {
            return 0.5;
        }
        return 0.0;
    }

    public Matrix<? extends PArray> asConvolution(Matrix<? extends PArray> src, WeightedPattern pattern) {
        return asConvolution(src.type(PArray.class), src, pattern);
    }

    public abstract <T extends PArray> Matrix<T> asConvolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern);


    /**
     * This implementation just creates a lazy convolution via {@link #asConvolution(Class, Matrix, WeightedPattern)}
     * method and copies it into a newly created matrix via
     * {@link Matrices#copy(ArrayContext, Matrix, Matrix)} method.
     * This method should be usually overridden to provide better implementations.
     *
     * @param src     the source matrix.
     * @param pattern the pattern.
     * @return        the result of convolution of the source matrix with the given pattern.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     * @see #asConvolution(Class, Matrix, WeightedPattern)
     */
    public Matrix<? extends UpdatablePArray> convolution(Matrix<? extends PArray> src, WeightedPattern pattern) {
        return convolution(src.updatableType(UpdatablePArray.class), src, pattern).cast(UpdatablePArray.class);
    }

    public <T extends PArray> Matrix<? extends T> convolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Class<?> elementType = Arrays.elementType(requiredType);
        Matrices.checkNewMatrixType(requiredType, elementType);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class,
            elementType, src.dimensions());
        convolution(dest, src, pattern);
        return dest.cast(requiredType);
    }

    public void convolution(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        WeightedPattern pattern)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Matrices.copy(context(), dest, asConvolution(dest.type(PArray.class), src, pattern));
    }
}
