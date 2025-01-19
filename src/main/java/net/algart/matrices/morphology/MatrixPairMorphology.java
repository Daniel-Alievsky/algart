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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.filters3x3.DilationByCross3x3;
import net.algart.matrices.filters3x3.DilationBySquare3x3;
import net.algart.matrices.filters3x3.ErosionByCross3x3;
import net.algart.matrices.filters3x3.ErosionBySquare3x3;

import java.util.List;
import java.util.Objects;

public final class MatrixPairMorphology {
    private static final Pattern CROSS = Patterns.newIntegerPattern(
            IPoint.valueOf(0, 0),
            IPoint.valueOf(1, 0), IPoint.valueOf(0, 1),
            IPoint.valueOf(-1, 0), IPoint.valueOf(0, -1));

    private static final Morphology MULTITHREADING_MORPHOLOGY =
            BasicMorphology.getInstance(null);
    private static final Morphology SINGLETHREADING_MORPHOLOGY =
            BasicMorphology.getInstance(ArrayContext.DEFAULT_SINGLE_THREAD);

    private final boolean binary;
    private final DilationByCross3x3 dilationByCross3x3;
    private final ErosionByCross3x3 erosionByCross3x3;
    private final DilationBySquare3x3 dilationBySquare3x3;
    private final ErosionBySquare3x3 erosionBySquare3x3;

    private boolean multithreading = net.algart.arrays.Arrays.SystemSettings.cpuCount() > 1;
    private Matrix.ContinuationMode continuationMode = null;
    private Matrix<? extends UpdatablePArray> originalResult;
    Matrix<? extends UpdatablePArray> result = null;
    Matrix<? extends UpdatablePArray> work;

    private MatrixPairMorphology(Matrix<? extends UpdatablePArray> work) {
        this.work = Objects.requireNonNull(work, "Null work matrix");
        final Class<?> elementType = work.elementType();
        this.binary = elementType == boolean.class;
        if (binary) {
            this.dilationByCross3x3 = null;
            this.erosionByCross3x3 = null;
            this.dilationBySquare3x3 = null;
            this.erosionBySquare3x3 = null;
        } else {
            this.dilationByCross3x3 = DilationByCross3x3.newInstance(elementType, work.dimensions());
            this.erosionByCross3x3 = ErosionByCross3x3.newInstance(elementType, work.dimensions());
            this.dilationBySquare3x3 = DilationBySquare3x3.newInstance(elementType, work.dimensions());
            this.erosionBySquare3x3 = ErosionBySquare3x3.newInstance(elementType, work.dimensions());
        }
    }

    public static MatrixPairMorphology newInstance(Matrix<? extends UpdatablePArray> work) {
        return new MatrixPairMorphology(work);
    }

    public boolean isMultithreading() {
        return multithreading;
    }

    public MatrixPairMorphology setMultithreading(boolean multithreading) {
        this.multithreading = multithreading;
        if (!binary) {
            this.dilationByCross3x3.setMultithreading(multithreading);
            this.erosionByCross3x3.setMultithreading(multithreading);
            this.dilationBySquare3x3.setMultithreading(multithreading);
            this.erosionBySquare3x3.setMultithreading(multithreading);
        }
        return this;
    }

    public Matrix.ContinuationMode continuationMode() {
        return continuationMode;
    }

    /**
     * Sets continuation model.
     *
     * <p>Note: if the continuation mode is not specified ({@code null}), this class works with maximal performance,
     * but the results near matrix boundary are not strictly specified. Really, in this case the current implmentation
     * uses CYCLIC or PSEUDO_CYCLIC mode, but it can change in future versions.
     *
     * @param continuationMode new continuation mode; can be {@code null}, then will be chosen
     *                         automatically to provide better performance
     * @return a reference to this object.
     */
    public MatrixPairMorphology setContinuationMode(Matrix.ContinuationMode continuationMode) {
        this.continuationMode = continuationMode;
        return this;
    }

    public MatrixPairMorphology setMatrixToProcess(Matrix<? extends UpdatablePArray> matrix) {
        Matrices.checkDimensionEquality(matrix, work);
        if (matrix.elementType() != work.elementType()) {
            throw new IllegalArgumentException("Element type of new " + matrix
                    + " does not match the element type of this object: " + work.elementType());
        }
        this.result = Objects.requireNonNull(matrix);
        this.originalResult = matrix;
        return this;
    }

    public MatrixPairMorphology copyFrom(Matrix<? extends PArray> source) {
        if (result == null) {
            throw new IllegalArgumentException("Matrix to process was not specified yet");
        }
        Matrices.copy(null, result, source);
        return this;
    }

    public MatrixPairMorphology openBySquare(long side) {
        erosionByRectangle(0, 0, side, side, false);
        dilationByRectangle(0, 0, side, side, true);
        return this;
    }

    public MatrixPairMorphology closeBySquare(long side) {
        dilationByRectangle(0, 0, side, side, true);
        erosionByRectangle(0, 0, side, side, false);
        return this;
    }

    /*Repeat() Dilation ==> Erosion;;
               dilation ==> erosion;;
               shiftAndSwap ==> shiftBackAndSwap
    */
    public static void dilationBySquare(
            Matrix<? extends UpdatablePArray> matrix,
            Matrix<? extends UpdatablePArray> temporaryMatrix,
            long side) {
        new MatrixPairMorphology(temporaryMatrix).setMatrixToProcess(matrix).dilationBySquare(side);
    }

    public MatrixPairMorphology dilationBySquare(long side) {
        return dilationByRectangle(-side / 2, -side / 2, side, side);
    }

    public MatrixPairMorphology dilationByDoubleSquare(int halfSide) {
        return dilationByRectangle(-halfSide, -halfSide, 2 * (long) halfSide + 1, 2 * (long) halfSide + 1);
    }

    public MatrixPairMorphology dilationByRectangle(long minX, long minY, long sizeX, long sizeY) {
        return dilationByRectangle(minX, minY, sizeX, sizeY, true);
    }

    public MatrixPairMorphology dilationByRectangle(
            long minX,
            long minY,
            long sizeX,
            long sizeY,
            boolean provideResult) {
        if (sizeX < 0 || sizeY < 0) {
            throw new IllegalArgumentException("Negative sizeX=" + sizeX + " or sizeY=" + sizeY);
        }
//        System.out.println("!!!!dilation " + work.elementType() + " " + sizeX + "x" + sizeY + ", " + minX + "," + minY);
        if (minX == -1 && minY == -1 && sizeX == 3 && sizeY == 3 && simple3x3Optimization()) {
            dilationBySquare3x3.filter(work, result);
            swap();
        } else {
            shiftAndSwap(IPoint.valueOf(minX, minY));
            final IPoint origin = IPoint.valueOf(0, 0);
            long i = 1;
            for (; 2 * i <= sizeX; i *= 2) {
                dilationAndSwap(origin, IPoint.valueOf(i, 0));
            }
            if (i < sizeX) {
                dilationAndSwap(origin, IPoint.valueOf(sizeX - i, 0));
            }
            i = 1;
            for (; 2 * i <= sizeY; i *= 2) {
                dilationAndSwap(origin, IPoint.valueOf(0, i));
            }
            if (i < sizeY) {
                dilationAndSwap(origin, IPoint.valueOf(0, sizeY - i));
            }
        }
        if (provideResult) {
            provideResult();
        }
        return this;
    }

    public MatrixPairMorphology dilationByOctagonWithDiameter(long diameter) {
        return dilationByOctagon(diameter / 2, diameter % 2 != 0);
    }

    public MatrixPairMorphology dilationByOctagon(long radius) {
        return dilationByOctagon(radius, false);
    }

    public MatrixPairMorphology dilationByOctagon(long radius, boolean addHalf) {
        //             *****
        //            *44444*
        //           *44***44*
        //          *44*222*44*
        //          *4*22*22*4*
        //          *4*2*0*2*4*
        //          *4*22*22*4*
        //          *44*222*44*
        //           *44***44*
        //            *44444*
        //             *****
        if (radius < 0) {
            throw new IllegalArgumentException("Negative radius=" + radius);
        }
        final long crossCount = (radius + 1) >>> 1;
        for (long i = 0; i < crossCount; i++) {
            // - it could be optimized by newMinkowskiMultiplePattern, but it is not so important here
            if (simple3x3Optimization()) {
                dilationByCross3x3.filter(work, result);
                swap();
            } else {
                dilationAndSwap(CROSS);
            }
        }
        final long squareCount = radius >>> 1;
        final long squareSide = 2 * squareCount + 1 + (addHalf ? 1 : 0);
        dilationByRectangle(-squareCount, -squareCount, squareSide, squareSide);
        return this;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public static void erosionBySquare(
            Matrix<? extends UpdatablePArray> matrix,
            Matrix<? extends UpdatablePArray> temporaryMatrix,
            long side) {
        new MatrixPairMorphology(temporaryMatrix).setMatrixToProcess(matrix).erosionBySquare(side);
    }

    public MatrixPairMorphology erosionBySquare(long side) {
        return erosionByRectangle(-side / 2, -side / 2, side, side);
    }

    public MatrixPairMorphology erosionByDoubleSquare(int halfSide) {
        return erosionByRectangle(-halfSide, -halfSide, 2 * (long) halfSide + 1, 2 * (long) halfSide + 1);
    }

    public MatrixPairMorphology erosionByRectangle(long minX, long minY, long sizeX, long sizeY) {
        return erosionByRectangle(minX, minY, sizeX, sizeY, true);
    }

    public MatrixPairMorphology erosionByRectangle(
            long minX,
            long minY,
            long sizeX,
            long sizeY,
            boolean provideResult) {
        if (sizeX < 0 || sizeY < 0) {
            throw new IllegalArgumentException("Negative sizeX=" + sizeX + " or sizeY=" + sizeY);
        }
//        System.out.println("!!!!erosion " + work.elementType() + " " + sizeX + "x" + sizeY + ", " + minX + "," + minY);
        if (minX == -1 && minY == -1 && sizeX == 3 && sizeY == 3 && simple3x3Optimization()) {
            erosionBySquare3x3.filter(work, result);
            swap();
        } else {
            shiftBackAndSwap(IPoint.valueOf(minX, minY));
            final IPoint origin = IPoint.valueOf(0, 0);
            long i = 1;
            for (; 2 * i <= sizeX; i *= 2) {
                erosionAndSwap(origin, IPoint.valueOf(i, 0));
            }
            if (i < sizeX) {
                erosionAndSwap(origin, IPoint.valueOf(sizeX - i, 0));
            }
            i = 1;
            for (; 2 * i <= sizeY; i *= 2) {
                erosionAndSwap(origin, IPoint.valueOf(0, i));
            }
            if (i < sizeY) {
                erosionAndSwap(origin, IPoint.valueOf(0, sizeY - i));
            }
        }
        if (provideResult) {
            provideResult();
        }
        return this;
    }

    public MatrixPairMorphology erosionByOctagonWithDiameter(long diameter) {
        return erosionByOctagon(diameter / 2, diameter % 2 != 0);
    }

    public MatrixPairMorphology erosionByOctagon(long radius) {
        return erosionByOctagon(radius, false);
    }

    public MatrixPairMorphology erosionByOctagon(long radius, boolean addHalf) {
        //             *****
        //            *44444*
        //           *44***44*
        //          *44*222*44*
        //          *4*22*22*4*
        //          *4*2*0*2*4*
        //          *4*22*22*4*
        //          *44*222*44*
        //           *44***44*
        //            *44444*
        //             *****
        if (radius < 0) {
            throw new IllegalArgumentException("Negative radius=" + radius);
        }
        final long crossCount = (radius + 1) >>> 1;
        for (long i = 0; i < crossCount; i++) {
            // - it could be optimized by newMinkowskiMultiplePattern, but it is not so important here
            if (simple3x3Optimization()) {
                erosionByCross3x3.filter(work, result);
                swap();
            } else {
                erosionAndSwap(CROSS);
            }
        }
        final long squareCount = radius >>> 1;
        final long squareSide = 2 * squareCount + 1 + (addHalf ? 1 : 0);
        erosionByRectangle(-squareCount, -squareCount, squareSide, squareSide);
        return this;
    }

    /*Repeat.AutoGeneratedEnd*/

    public void shiftAndSwap(IPoint p) {
        if (!p.isOrigin()) {
            getSinglethreadingMorphology().dilation(
                    work, result, Patterns.newIntegerPattern(p), true);
            swap();
        }
    }

    public void shiftBackAndSwap(IPoint p) {
        shiftAndSwap(p.symmetric());
    }

    public void dilationAndSwap(IPoint p1, IPoint p2) {
        dilationAndSwap(java.util.Arrays.asList(p1, p2));
    }

    public void dilationAndSwap(List<IPoint> points) {
        dilationAndSwap(Patterns.newIntegerPattern(points));
    }

    public void dilationAndSwap(Pattern pattern) {
        getMorphology().dilation(work, result, pattern, true);
        swap();
    }

    public void erosionAndSwap(IPoint p1, IPoint p2) {
        erosionAndSwap(java.util.Arrays.asList(p1, p2));
    }

    public void erosionAndSwap(List<IPoint> points) {
        erosionAndSwap(Patterns.newIntegerPattern(points));
    }

    public void erosionAndSwap(Pattern pattern) {
        getMorphology().erosion(work, result, pattern, true);
        swap();
    }

    public void provideResult() {
        if (result != originalResult) {
            originalResult.array().copy(result.array());
            swap();
        }
    }

    public Morphology getSinglethreadingMorphology() {
        Morphology morphology = SINGLETHREADING_MORPHOLOGY;
        if (continuationMode != null) {
            morphology = ContinuedMorphology.getInstance(morphology, continuationMode);
        }
        return morphology;
    }

    public Morphology getMorphology() {
        Morphology morphology = multithreading ? MULTITHREADING_MORPHOLOGY : SINGLETHREADING_MORPHOLOGY;
        if (continuationMode != null) {
            morphology = ContinuedMorphology.getInstance(morphology, continuationMode);
        }
        return morphology;
    }

    private void swap() {
        Matrix<? extends UpdatablePArray> temp = result;
        result = work;
        work = temp;
    }

    private boolean simple3x3Optimization() {
        return (continuationMode == null || continuationMode == Matrix.ContinuationMode.CYCLIC)
                && work.elementType() != boolean.class;
    }

}
