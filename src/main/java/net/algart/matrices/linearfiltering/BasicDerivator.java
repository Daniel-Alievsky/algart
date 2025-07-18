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

package net.algart.matrices.linearfiltering;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.MaxFromTwoSelectedNumbersFunc;
import net.algart.math.functions.MinFromTwoSelectedNumbersFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BasicDerivator extends AbstractDerivator implements Derivator {
    private BasicDerivator(ArrayContext context, boolean decrementForUnsigned) {
        super(context, decrementForUnsigned);
    }

    /**
     * Returns new instance of this class.
     *
     * @param context the {@link #context() context} that will be used by this object;
     *                can be {@code null}, then it will be ignored.
     * @return        new instance of this class.
     */
    public static BasicDerivator getInstance(ArrayContext context) {
        return new BasicDerivator(context, false);
    }

    /**
     * Returns new instance of this class, correcting unsigned arguments.
     * If the type of the arguments of some methods is an unsigned number in terms of AlgART libraries &mdash;
     * <code>byte</code>, <code>short</code>, <code>char</code> &mdash; it is automatically decremented by 128
     * (<code>byte</code>) or 32768 (<code>short</code> and <code>char</code>).
     *
     * @param context              the {@link #context() context} that will be used by this object;
     *                             can be {@code null}, then it will be ignored.
     * @return                     new instance of this class.
     */
    public static BasicDerivator getCorrectingUnsignedInstance(ArrayContext context) {
        return new BasicDerivator(context, true);
    }

    @Override
    public boolean isPseudoCyclic() {
        return true;
    }

    /*Repeat() src,\s*max ==> min, src;; max ==> min;; Max ==> Min */
    @Override
    public Matrix<? extends PArray> asMaximumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        return Matrices.asFuncMatrix(
            MaxFromTwoSelectedNumbersFunc.getInstance(directions.length), src.type(PArray.class),
            getShiftedMatrices(src, directionIndexes, directions));
    }

    @Override
    public Matrix<? extends BitArray> asMaskOfMaximums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Objects.requireNonNull(mode, "Null suppression mode argument");
        Matrix<? extends PArray> max = asMaximumFromShiftedForwardAndBackward(src, directionIndexes, directions);
        Matrix<? extends BitArray> zeroMatrix = Matrices.asCoordFuncMatrix(
            ConstantFunc.getInstance(0.0), BitArray.class, src.dimensions());
        Matrix<? extends BitArray> unitMatrix = Matrices.asCoordFuncMatrix(
            ConstantFunc.getInstance(1.0), BitArray.class, src.dimensions());
        return Matrices.asFuncMatrix(mode.selectionFunc, BitArray.class, src, max, unitMatrix, zeroMatrix);
    }

    @Override
    public Matrix<? extends PArray> asNonMaximumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Objects.requireNonNull(mode, "Null suppression mode argument");
        Matrix<? extends PArray> max = asMaximumFromShiftedForwardAndBackward(src, directionIndexes, directions);
        Matrix<? extends PArray> filledMatrix = Matrices.asCoordFuncMatrix(
            ConstantFunc.getInstance(filler), src.type(PArray.class), src.dimensions());
        return Matrices.asFuncMatrix(mode.selectionFunc, src.type(PArray.class), src, max, src, filledMatrix);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    @Override
    public Matrix<? extends PArray> asMinimumFromShiftedForwardAndBackward(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Matrices.checkDimensionEquality(src, directionIndexes);
        directions = checkAndCloneDirections(src.dimCount(), directions);
        return Matrices.asFuncMatrix(
            MinFromTwoSelectedNumbersFunc.getInstance(directions.length), src.type(PArray.class),
            getShiftedMatrices(src, directionIndexes, directions));
    }

    @Override
    public Matrix<? extends BitArray> asMaskOfMinimums(Matrix<? extends PArray> src,
        SuppressionMode mode, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Objects.requireNonNull(mode, "Null suppression mode argument");
        Matrix<? extends PArray> min = asMinimumFromShiftedForwardAndBackward(src, directionIndexes, directions);
        Matrix<? extends BitArray> zeroMatrix = Matrices.asCoordFuncMatrix(
            ConstantFunc.getInstance(0.0), BitArray.class, src.dimensions());
        Matrix<? extends BitArray> unitMatrix = Matrices.asCoordFuncMatrix(
            ConstantFunc.getInstance(1.0), BitArray.class, src.dimensions());
        return Matrices.asFuncMatrix(mode.selectionFunc, BitArray.class, min, src, unitMatrix, zeroMatrix);
    }

    @Override
    public Matrix<? extends PArray> asNonMinimumSuppression(Matrix<? extends PArray> src,
        SuppressionMode mode, double filler, Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        Objects.requireNonNull(mode, "Null suppression mode argument");
        Matrix<? extends PArray> min = asMinimumFromShiftedForwardAndBackward(src, directionIndexes, directions);
        Matrix<? extends PArray> filledMatrix = Matrices.asCoordFuncMatrix(
            ConstantFunc.getInstance(filler), src.type(PArray.class), src.dimensions());
        return Matrices.asFuncMatrix(mode.selectionFunc, src.type(PArray.class), min, src, src, filledMatrix);
    }

    /*Repeat.AutoGeneratedEnd*/

    private List<Matrix<? extends PArray>> getShiftedMatrices(Matrix<? extends PArray> src,
        Matrix<? extends PIntegerArray> directionIndexes, IPoint... directions)
    {
        List<Matrix<? extends PArray>> matrices = new ArrayList<>();
        matrices.add(directionIndexes);
        for (IPoint dir : directions) {
            matrices.add(Matrices.asShifted(src, dir.coordinates()).cast(PArray.class));
        }
        for (IPoint dir : directions) {
            matrices.add(Matrices.asShifted(src, dir.symmetric().coordinates()).cast(PArray.class));
        }
        return matrices;
    }
}
