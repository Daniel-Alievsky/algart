/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.skeletons;

import net.algart.arrays.*;

abstract class AbstractThinningSkeleton2D
    extends AbstractIterativeArrayProcessor<Matrix<? extends UpdatableBitArray>>
    implements ThinningSkeleton
{
    final boolean straightThinning;
    final boolean diagonalThinning;
    final MemoryModel mm;
    Matrix<? extends UpdatableBitArray> result;
    Matrix<? extends UpdatableBitArray> temp;
    private long cardinality = -1;
    boolean done = false;

    AbstractThinningSkeleton2D(ArrayContext context, Matrix<? extends UpdatableBitArray> matrix,
        boolean straightThinning, boolean diagonalThinning)
    {
        super(context);
        if (matrix == null)
            throw new NullPointerException("Null matrix argument");
        this.mm = ErodingSkeleton.mm(memoryModel, matrix, 1);
        this.result = matrix;
        this.temp = mm.newMatrix(UpdatableBitArray.class, boolean.class, matrix.dimensions());
        this.straightThinning = straightThinning;
        this.diagonalThinning = diagonalThinning;
    }

    @Override
    public final void performIteration(ArrayContext context) {
        ArrayContext c = cardinality == -1 ? part(context, 0.02, 0.98) : part(context, 0.02, 1.0);
        if (cardinality == -1) {
            cardinality = Arrays.cardinality(part(context, 0.0, 0.02), result.array());
        }
        long lastCardinality = cardinality;
        double part = straightThinning && diagonalThinning ? 0.125 : 0.25;
        for (int k = 0; k < 8; k++) {
            if (!isThinningRequired(k)) {
                continue;
            }
            Matrices.copy(part(c, k * 0.125, Math.min(k * 0.125 + part, 1.0)), temp, asThinning(k));
            Matrix<? extends UpdatableBitArray> t = temp; temp = result; result = t;
        }
        cardinality = Arrays.cardinality(part(context, 0.98, 1.0), result.array());
        done = cardinality == lastCardinality;
    }

    @Override
    public final boolean done() {
        return done;
    }

    @Override
    public abstract long estimatedNumberOfIterations();

    @Override
    public final Matrix<? extends UpdatableBitArray> result() {
        return result;
    }

    @Override
    public final void freeResources(ArrayContext context) {
        temp.freeResources(context == null ? null : context.part(0.0, 0.5));
        result.freeResources(context == null ? null : context.part(0.5, 1.0));
    }

    public final boolean isThinningRequired(int directionIndex) {
        if (directionIndex < 0 || directionIndex > 7)
            throw new IllegalArgumentException("Illegal directionIndex = " + directionIndex + " (must be 0..7)");
        return directionIndex % 2 == 0 ? straightThinning : diagonalThinning;
    }

    public abstract Matrix<BitArray> asThinning(int directionIndex);
}
