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

package net.algart.matrices.linearfiltering;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractDerivator extends AbstractArrayProcessorWithContextSwitching implements Derivator {
    protected final boolean decrementForUnsigned;

    /**
     * Creates an instance of this class with the given context.
     *
     * @param context              the context used by this instance for all operations.
     * @param decrementForUnsigned if <code>true</code>, when the type of the arguments of some methods is
     *                             an unsigned number in terms of AlgART libraries &mdash;
     *                             <code>byte</code>, <code>short</code>, <code>char</code> &mdash;
     *                             it is automatically decremented by 128 (<code>byte</code>) or 32768
     *                             (<code>short</code> and <code>char</code>).
     */
    protected AbstractDerivator(ArrayContext context, boolean decrementForUnsigned) {
        super(context);
        this.decrementForUnsigned = decrementForUnsigned;
    }

    public Derivator context(ArrayContext newContext) {
        return (Derivator)super.context(newContext);
    }

    public abstract boolean isPseudoCyclic();

    public double decrement(Class<?> elementType) {
        if (decrementForUnsigned) {
            if (elementType == byte.class) {
                return 128.0;
            }
            if (elementType == short.class || elementType == char.class) {
                return 32768.0;
            }
        }
        return 0.0;
    }

    /*Repeat() max ==> min;; Max ==> Min */
    public abstract Matrix<? extends PArray> asMaximumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions);

    public Matrix<? extends UpdatablePArray> maximumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        maximumFromShiftedForwardAndBackward(dest, src, directionIndexes, directions);
        return dest;
    }

    public void maximumFromShiftedForwardAndBackward(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrices.copy(context(), dest, asMaximumFromShiftedForwardAndBackward(src, directionIndexes, directions));
    }

    public abstract Matrix<? extends BitArray> asMaskOfMaximums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions);


    public Matrix<? extends UpdatableBitArray> maskOfMaximums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrix<? extends UpdatableBitArray> dest = memoryModel().newBitMatrix(src.dimensions());
        maskOfMaximums(dest, src, mode, directionIndexes, directions);
        return dest;
    }

    public void maskOfMaximums(Matrix<? extends UpdatableBitArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrices.copy(context(), dest, asMaskOfMaximums(src, mode, directionIndexes, directions));
    }

    public abstract Matrix<? extends PArray> asNonMaximumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions);

    public Matrix<? extends UpdatablePArray> nonMaximumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        nonMaximumSuppression(dest, src, mode, filler, directionIndexes, directions);
        return dest;
    }

    public void nonMaximumSuppression(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrices.copy(context(), dest, asNonMaximumSuppression(src, mode, filler, directionIndexes, directions));
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public abstract Matrix<? extends PArray> asMinimumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions);

    public Matrix<? extends UpdatablePArray> minimumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        minimumFromShiftedForwardAndBackward(dest, src, directionIndexes, directions);
        return dest;
    }

    public void minimumFromShiftedForwardAndBackward(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrices.copy(context(), dest, asMinimumFromShiftedForwardAndBackward(src, directionIndexes, directions));
    }

    public abstract Matrix<? extends BitArray> asMaskOfMinimums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions);


    public Matrix<? extends UpdatableBitArray> maskOfMinimums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrix<? extends UpdatableBitArray> dest = memoryModel().newBitMatrix(src.dimensions());
        maskOfMinimums(dest, src, mode, directionIndexes, directions);
        return dest;
    }

    public void maskOfMinimums(Matrix<? extends UpdatableBitArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrices.copy(context(), dest, asMaskOfMinimums(src, mode, directionIndexes, directions));
    }

    public abstract Matrix<? extends PArray> asNonMinimumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions);

    public Matrix<? extends UpdatablePArray> nonMinimumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrix<? extends UpdatablePArray> dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        nonMinimumSuppression(dest, src, mode, filler, directionIndexes, directions);
        return dest;
    }

    public void nonMinimumSuppression(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(dest, src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        Matrices.copy(context(), dest, asNonMinimumSuppression(src, mode, filler, directionIndexes, directions));
    }
    /*Repeat.AutoGeneratedEnd*/

    public Matrix<? extends PIntegerArray> asRoundedDirectionIndex2D(
        Matrix<? extends PArray> vectorX, Matrix<? extends PArray> vectorY)
    {
        Matrices.checkDimensionEquality(vectorX, vectorY);
        double d = decrement(vectorX.elementType());
        if (d != 0.0) {
            vectorX = Matrices.asFuncMatrix(LinearFunc.getInstance(-d, 1), DoubleArray.class, vectorX);
        }
        d = decrement(vectorY.elementType());
        if (d != 0.0) {
            vectorY = Matrices.asFuncMatrix(LinearFunc.getInstance(-d, 1), DoubleArray.class, vectorY);
        }
        return Matrices.asFuncMatrix(Func.SELECT_FROM_8_DIRECTIONS_2D, ByteArray.class, vectorX, vectorY);
    }

    public IPoint[] roundedDirections2D() {
        return Func.SHIFTS_ALONG_8_DIRECTIONS_2D.toArray(new IPoint[0]);
    }

    public <T extends PArray> Matrix<T> asModuleOfVector(Class<? extends T> requiredType,
        List<? extends Matrix<? extends PArray>> vectorComponents)
    {
        ArrayList<Matrix<? extends PArray>> list = new ArrayList<>(vectorComponents);
        Matrices.checkDimensionEquality(list);
        int n = list.size();
        if (n == 0) {
            throw new IllegalArgumentException("Empty list of vector components");
        }
        if (n == 1) {
            Matrix<? extends PArray> m = list.get(0);
            double d = decrement(m.elementType());
            if (d != 0.0) {
                m = Matrices.asFuncMatrix(LinearFunc.getInstance(-d, 1), DoubleArray.class, m);
            }
            return Matrices.asFuncMatrix(Func.ABS, requiredType, m);
        }
        for (int k = 0; k < n; k++) {
            Matrix<? extends PArray> m = list.get(k);
            double d = decrement(m.elementType());
            if (d != 0.0) {
                m = Matrices.asFuncMatrix(LinearFunc.getInstance(-d, 1), DoubleArray.class, m);
            }
            m = Matrices.asFuncMatrix(PowerFunc.getInstance(2.0), DoubleArray.class, m);
            list.set(k, m);
        }
        Matrix<? extends PArray> sqrSum = Matrices.asFuncMatrix(
            LinearFunc.getNonweightedInstance(0.0, 1.0, n), DoubleArray.class, list);
        return Matrices.asFuncMatrix(PowerFunc.getInstance(0.5), requiredType, sqrSum);

    }

    protected static IPoint[] checkAndCloneDirections(int dimCount, IPoint... directions) {
        Objects.requireNonNull(directions, "Null directions argument");
        directions = directions.clone(); // - to be sure that its content cannot be changed in a parallel thread
        int k = 0;
        for (IPoint dir : directions) {
            Objects.requireNonNull(dir, "Null direction #" + k);
            if (dir.coordCount() != dimCount) {
                throw new IllegalArgumentException("The direction #" + k + " = " + dir
                    + " has illegal number of dimensions: " + dimCount + "-dimensional vectors required");
            }
            k++;
        }
        return directions;
    }
}