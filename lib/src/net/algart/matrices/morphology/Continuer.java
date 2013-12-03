/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.DependenceApertureBuilder;

import java.util.ArrayList;
import java.util.List;

class Continuer {
    private final Matrix<? extends UpdatablePArray> continuedDest;
    private final List<Matrix<? extends PArray>> continuedArguments;
    private final IRectangularArea aperture;

    public Continuer(Matrix<? extends UpdatablePArray> dest,
                     Matrix<? extends PArray> src, Pattern pattern,
                     RankMorphology parent, Matrix.ContinuationMode continuationMode)
    {
        this(dest, Matrices.several(PArray.class, src), pattern, parent, continuationMode);
    }

    public Continuer(Matrix<? extends UpdatablePArray> dest,
                     Matrix<? extends PArray> src1, Matrix<? extends PArray> src2, Pattern pattern,
                     RankMorphology parent, Matrix.ContinuationMode continuationMode)
    {
        this(dest, Matrices.several(PArray.class, src1, src2), pattern, parent, continuationMode);
    }

    public Continuer(Matrix<? extends UpdatablePArray> dest,
                     Matrix<? extends PArray> src1, Matrix<? extends PArray> src2, Matrix<? extends PArray> src3,
                     Pattern pattern,
                     RankMorphology parent, Matrix.ContinuationMode continuationMode)
    {
        this(dest, Matrices.several(PArray.class, src1, src2, src3), pattern, parent, continuationMode);
    }

    public Continuer(Matrix<? extends UpdatablePArray> dest,
                     List<Matrix<? extends PArray>> arguments, Pattern pattern,
                     RankMorphology parent, Matrix.ContinuationMode continuationMode)
    {
        arguments = new ArrayList<Matrix<? extends PArray>>(arguments);
        Matrix<? extends PArray> mainArgument = arguments.get(0);
        if (dest != null) {
            Matrices.checkDimensionEquality(dest, mainArgument);
        }
        Matrices.checkDimensionEquality(arguments);
        arguments.remove(0);
        DependenceApertureBuilder builder = dest == null && parent.isPseudoCyclic() ?
            DependenceApertureBuilder.SUM :
            DependenceApertureBuilder.SUM_MAX_0;
        this.aperture = builder.getAperture(mainArgument.dimCount(), pattern, false);
        Matrix<? extends PArray> continuedSrc = DependenceApertureBuilder.extend(mainArgument,
            aperture, continuationMode); // also checks overflow
        this.continuedArguments = extendAdditionalMatrices(arguments);
        this.continuedArguments.add(0, continuedSrc);
        this.continuedDest = dest == null ? null :
            DependenceApertureBuilder.extend(dest, aperture, Matrix.ContinuationMode.ZERO_CONSTANT);
    }

    public Matrix<? extends PArray> get(int index) {
        return continuedArguments.get(index);
    }

    public Matrix<? extends UpdatablePArray> continuedDest() {
        assert continuedDest != null;
        return continuedDest;
    }

    public <T extends PArray> Matrix<T> reduce(Matrix<T> matrix) {
        assert continuedDest == null;
        return DependenceApertureBuilder.reduce(matrix, aperture);
    }

    private List<Matrix<? extends PArray>> extendAdditionalMatrices(List<Matrix<? extends PArray>> matrices) {
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
