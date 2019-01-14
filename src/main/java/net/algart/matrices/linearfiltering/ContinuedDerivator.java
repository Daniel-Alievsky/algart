/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.IPoint;

import java.util.List;

public class ContinuedDerivator implements Derivator {
    private final Derivator parent;
    private final ArrayContext context;
    private final Matrix.ContinuationMode continuationMode;

    private ContinuedDerivator(Derivator parent, Matrix.ContinuationMode continuationMode) {
        if (parent == null)
            throw new NullPointerException("Null parent derivator");
        if (continuationMode == null)
            throw new NullPointerException("Null continuationMode derivator");
        if (continuationMode == Matrix.ContinuationMode.NONE)
            throw new IllegalArgumentException(getClass().getName() + " cannot be used with continuation mode \""
                + continuationMode + "\"");
        this.parent = parent;
        this.context = parent.context() == null ? ArrayContext.DEFAULT : parent.context();
        this.continuationMode = continuationMode;
    }

    /**
     * Returns new instance of this class with the passed parent {@link Derivator} object
     * and the specified continuation mode.
     * See comments to {@link net.algart.arrays.Matrix.ContinuationMode} class
     * for more information about possible continuations.
     *
     * @param parent           the instance of {@link Derivator} interface that will perform all operations.
     * @param continuationMode the mode of continuation outside the source matrix.
     * @return                 new instance of this class.
     * @throws NullPointerException     if <tt>parent</tt> or <tt>continuationMode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</tt>.
     * @see #parent()
     * @see #continuationMode()
     */
    public static ContinuedDerivator getInstance(Derivator parent, Matrix.ContinuationMode continuationMode) {
        return new ContinuedDerivator(parent, continuationMode);
    }

    /**
     * Returns the parent {@link Derivator} object, passed to
     * {@link #getInstance(Derivator, Matrix.ContinuationMode)} method.
     *
     * @return the parent {@link Derivator} object.
     */
    public Derivator parent() {
        return this.parent;
    }

    /**
     * Returns the continuation mode, used by this object for virtual continuing the source matrix.
     * The returned value is equal to the corresponding argument of
     * {@link #getInstance(Derivator, Matrix.ContinuationMode)} method.
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
     * that it uses the specified <tt>newContext</tt> for all operations.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another newContext.
     */
    public Derivator context(ArrayContext newContext) {
        return new ContinuedDerivator(parent.context(newContext), continuationMode);
    }

    public boolean isPseudoCyclic() {
        return this.continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC;
    }

    public double decrement(Class<?> elementType) {
        return parent.decrement(elementType);
    }

    /*Repeat() max ==> min;; Max ==> Min */
    public Matrix<? extends PArray> asMaximumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends PArray> result = parent.asMaximumFromShiftedForwardAndBackward(src,
            directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public Matrix<? extends UpdatablePArray> maximumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> result = parent.maximumFromShiftedForwardAndBackward(src,
            directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public void maximumFromShiftedForwardAndBackward(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> temp = context.getMemoryModel().newMatrix(UpdatablePArray.class, dest);
        parent.context(context.part(0.0, 0.95)).maximumFromShiftedForwardAndBackward(temp, src,
            directionIndexes, directions);
        Matrices.copy(context.part(0.95, 1.0), dest, temp.subMatr(origin, dim));
    }

    public Matrix<? extends BitArray> asMaskOfMaximums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends BitArray> result = parent.asMaskOfMaximums(src, mode, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public Matrix<? extends UpdatableBitArray> maskOfMaximums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatableBitArray> result = parent.maskOfMaximums(src, mode, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public void maskOfMaximums(
        Matrix<? extends UpdatableBitArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatableBitArray> temp = context.getMemoryModel().newBitMatrix(dest.dimensions());
        parent.context(context.part(0.0, 0.95)).maskOfMaximums(temp, src, mode, directionIndexes, directions);
        Matrices.copy(context.part(0.95, 1.0), dest, temp.subMatr(origin, dim));
    }

    public Matrix<? extends PArray> asNonMaximumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends PArray> result = parent.asNonMaximumSuppression(src,
            mode, filler, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public Matrix<? extends UpdatablePArray> nonMaximumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> result = parent.nonMaximumSuppression(src,
            mode, filler, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public void nonMaximumSuppression(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> temp = context.getMemoryModel().newMatrix(UpdatablePArray.class, dest);
        parent.context(context.part(0.0, 0.95)).nonMaximumSuppression(temp, src,
            mode, filler, directionIndexes, directions);
        Matrices.copy(context.part(0.95, 1.0), dest, temp.subMatr(origin, dim));
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public Matrix<? extends PArray> asMinimumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends PArray> result = parent.asMinimumFromShiftedForwardAndBackward(src,
            directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public Matrix<? extends UpdatablePArray> minimumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> result = parent.minimumFromShiftedForwardAndBackward(src,
            directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public void minimumFromShiftedForwardAndBackward(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> temp = context.getMemoryModel().newMatrix(UpdatablePArray.class, dest);
        parent.context(context.part(0.0, 0.95)).minimumFromShiftedForwardAndBackward(temp, src,
            directionIndexes, directions);
        Matrices.copy(context.part(0.95, 1.0), dest, temp.subMatr(origin, dim));
    }

    public Matrix<? extends BitArray> asMaskOfMinimums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends BitArray> result = parent.asMaskOfMinimums(src, mode, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public Matrix<? extends UpdatableBitArray> maskOfMinimums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatableBitArray> result = parent.maskOfMinimums(src, mode, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public void maskOfMinimums(
        Matrix<? extends UpdatableBitArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatableBitArray> temp = context.getMemoryModel().newBitMatrix(dest.dimensions());
        parent.context(context.part(0.0, 0.95)).maskOfMinimums(temp, src, mode, directionIndexes, directions);
        Matrices.copy(context.part(0.95, 1.0), dest, temp.subMatr(origin, dim));
    }

    public Matrix<? extends PArray> asNonMinimumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends PArray> result = parent.asNonMinimumSuppression(src,
            mode, filler, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public Matrix<? extends UpdatablePArray> nonMinimumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> result = parent.nonMinimumSuppression(src,
            mode, filler, directionIndexes, directions);
        return result.subMatr(origin, dim);
    }

    public void nonMinimumSuppression(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = AbstractDerivator.checkAndCloneDirections(src.dimCount(), directions);
        long[] dim = src.dimensions();
        long[] increasedDim = increaseDimensions(dim, directions);
        long[] origin = new long[dim.length]; // zero-filled by Java
        src = src.subMatr(origin, increasedDim, continuationMode);
        directionIndexes = directionIndexes.subMatr(origin, increasedDim, Matrix.ContinuationMode.ZERO_CONSTANT);
        // outsideValue must be any valid index
        Matrix<? extends UpdatablePArray> temp = context.getMemoryModel().newMatrix(UpdatablePArray.class, dest);
        parent.context(context.part(0.0, 0.95)).nonMinimumSuppression(temp, src,
            mode, filler, directionIndexes, directions);
        Matrices.copy(context.part(0.95, 1.0), dest, temp.subMatr(origin, dim));
    }

    /*Repeat.AutoGeneratedEnd*/

    public <T extends PArray> Matrix<T> asModuleOfVector(Class<? extends T> requiredType,
        List<? extends Matrix<? extends PArray>> vectorComponents)
    {
        return parent.asModuleOfVector(requiredType, vectorComponents); // outside elements don't affect
    }

    public Matrix<? extends PIntegerArray> asRoundedDirectionIndex2D(
        Matrix<? extends PArray> vectorX, Matrix<? extends PArray> vectorY)
    {
        return parent.asRoundedDirectionIndex2D(vectorX, vectorY);
    }

    public IPoint[] roundedDirections2D() {
        return parent.roundedDirections2D();
    }

    private static long[] increaseDimensions(long[] dim, IPoint... directions) {
        long[] max = new long[dim.length]; // zero-filled
        for (IPoint dir : directions) {
            for (int k = 0; k < max.length; k++) {
                max[k] = Math.max(max[k], 2 * dir.coord(k));
            }
        }
        long[] result = new long[dim.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = dim[k] + 8 + max[k]; // +8 to be on the safe side: some operations may use boundaries, etc.
        }
        return result;
    }
}
