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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;

import java.util.Objects;

/**
 * <p>A skeletal implementation of the {@link Morphology} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>All methods of the interface are completely implemented here via 3 following methods:</p>
 *
 * <ul>
 * <li>{@link #context(ArrayContext newContext)},</li>
 * <li>{@link #asDilationOrErosion(Matrix src, Pattern pattern, boolean isDilation)},</li>
 * <li>{@link
 * #dilationOrErosion(Matrix dest, Matrix src, Pattern pattern, boolean isDilation, boolean disableMemoryAllocation)}.
 * </li>
 * </ul>
 *
 * <p>Usually the subclasses need to override only these 3 methods and {@link #isPseudoCyclic()}.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractMorphology extends AbstractArrayProcessorWithContextSwitching implements Morphology {

    /**
     * Creates an instance of this class with the given context.
     *
     * @param context the context used by this instance for all operations.
     */
    protected AbstractMorphology(ArrayContext context) {
        super(context);
    }

    public Morphology context(ArrayContext newContext) {
        return (Morphology) super.context(newContext);
    }

    public abstract boolean isPseudoCyclic();

    public Matrix<? extends PArray> asDilation(Matrix<? extends PArray> src, Pattern pattern) {
        return asDilationOrErosion(src, pattern, true);
    }

    public Matrix<? extends PArray> asErosion(Matrix<? extends PArray> src, Pattern pattern) {
        return asDilationOrErosion(src, pattern, false);
    }

    public Matrix<? extends UpdatablePArray> dilation(Matrix<? extends PArray> src, Pattern pattern) {
        return dilationOrErosion(null, src, pattern, true, false);
    }

    public Matrix<? extends UpdatablePArray> erosion(Matrix<? extends PArray> src, Pattern pattern) {
        return dilationOrErosion(null, src, pattern, false, false);
    }

    public Matrix<? extends UpdatablePArray> dilation(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        Objects.requireNonNull(subtractionMode, "Null subtractionMode");
        Matrix<? extends UpdatablePArray> dilation =
                (subtractionMode == SubtractionMode.NONE ? this : context(contextPart(0.0, 0.9))).dilation(src,
                        pattern);
        subtractionMode.subtract(contextPart(0.9, 1.0), dilation, src);
        return dilation;
    }

    public Matrix<? extends UpdatablePArray> erosion(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        Objects.requireNonNull(subtractionMode, "Null subtractionMode");
        Matrix<? extends UpdatablePArray> erosion =
                (subtractionMode == SubtractionMode.NONE ? this : context(contextPart(0.0, 0.9))).erosion(src, pattern);
        subtractionMode.subtract(contextPart(0.9, 1.0), erosion, src);
        return erosion;
    }

    public void dilation(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            boolean disableMemoryAllocation) {
        Objects.requireNonNull(dest, "Null dest argument");
        dilationOrErosion(dest, src, pattern, true, disableMemoryAllocation);
    }

    public void erosion(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            boolean disableMemoryAllocation) {
        Objects.requireNonNull(dest, "Null dest argument");
        dilationOrErosion(dest, src, pattern, false, disableMemoryAllocation);
    }

    public void dilation(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        dilation(dest, src, pattern, false);
    }

    public void erosion(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        erosion(dest, src, pattern, false);
    }

    public Matrix<? extends UpdatablePArray> dilationErosion(
            Matrix<? extends PArray> src,
            Pattern dilationPattern,
            Pattern erosionPattern,
            SubtractionMode subtractionMode) {
        Objects.requireNonNull(dilationPattern, "Null dilationPattern argument");
        Objects.requireNonNull(erosionPattern, "Null erosionPattern argument");
        if (!dimensionsAllowed(src, dilationPattern)) {
            throw new IllegalArgumentException("Number of dimensions of the dilation pattern and the matrix mismatch");
        }
        if (!dimensionsAllowed(src, erosionPattern)) {
            throw new IllegalArgumentException("Number of dimensions of the erosion pattern and the matrix mismatch");
        }
        Objects.requireNonNull(subtractionMode, "Null subtractionMode");
        double w = subtractionMode == SubtractionMode.NONE ? 1.0 : 0.9;
        Matrix<? extends UpdatablePArray> actual = context(contextPart(0.0, 0.5 * w)).dilation(src, dilationPattern);
        actual = context(contextPart(0.5 * w, 1.0 * w)).erosion(actual, erosionPattern);
        subtractionMode.subtract(contextPart(0.9, 1.0), actual, src);
        return actual;
    }

    public Matrix<? extends UpdatablePArray> erosionDilation(
            Matrix<? extends PArray> src,
            Pattern erosionPattern,
            Pattern dilationPattern,
            SubtractionMode subtractionMode) {
        Objects.requireNonNull(erosionPattern, "Null erosionPattern argument");
        Objects.requireNonNull(dilationPattern, "Null dilationPattern argument");
        if (!dimensionsAllowed(src, dilationPattern)) {
            throw new IllegalArgumentException("Number of dimensions of the erosion pattern and the matrix mismatch");
        }
        if (!dimensionsAllowed(src, erosionPattern)) {
            throw new IllegalArgumentException("Number of dimensions of the dilation pattern and the matrix mismatch");
        }
        double w = subtractionMode == SubtractionMode.NONE ? 1.0 : 0.9;
        Matrix<? extends UpdatablePArray> actual = context(contextPart(0.0, 0.5 * w)).erosion(src, erosionPattern);
        actual = context(contextPart(0.5 * w, 1.0 * w)).dilation(actual, dilationPattern);
        subtractionMode.subtract(contextPart(0.9, 1.0), actual, src);
        return actual;
    }

    public Matrix<? extends UpdatablePArray> closing(
            Matrix<? extends PArray> src, Pattern pattern,
            SubtractionMode subtractionMode) {
        return dilationErosion(src, pattern, pattern, subtractionMode);
    }

    public Matrix<? extends UpdatablePArray> opening(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        return erosionDilation(src, pattern, pattern, subtractionMode);
    }

    public Matrix<? extends UpdatablePArray> weakDilation(Matrix<? extends PArray> src, Pattern pattern) {
        Matrix<? extends UpdatablePArray> dilation = context(contextPart(0.0, 0.5)).dilation(src, pattern);
        Matrix<? extends UpdatablePArray> closing = context(contextPart(0.45, 0.9)).erosion(dilation, pattern);
        Matrices.applyFunc(contextPart(0.9, 1.0),
                !(this instanceof BasicMorphology), // overflow is impossible for BasicMorphology
                LinearFunc.getInstance(0, 1, -1, 1), dilation, dilation, closing, src);
        return dilation;
    }

    public Matrix<? extends UpdatablePArray> weakErosion(Matrix<? extends PArray> src, Pattern pattern) {
        Matrix<? extends UpdatablePArray> erosion = context(contextPart(0.0, 0.5)).erosion(src, pattern);
        Matrix<? extends UpdatablePArray> opening = context(contextPart(0.45, 0.9)).dilation(erosion, pattern);
        Matrices.applyFunc(contextPart(0.9, 1.0),
                !(this instanceof BasicMorphology), // overflow is impossible for BasicMorphology
                LinearFunc.getInstance(0, 1, -1, 1), erosion, erosion, opening, src);
        return erosion;
    }

    public Matrix<? extends UpdatablePArray> maskedDilationErosion(
            Matrix<? extends PArray> src,
            Pattern dilationPattern, Pattern erosionPattern) {

        Matrix<? extends UpdatablePArray> actual = context(contextPart(0.0, 0.95)).dilationErosion(
                src, dilationPattern, erosionPattern, SubtractionMode.NONE);
        Matrices.applyFunc(contextPart(0.95, 1.0), Func.MIN, actual, actual, src);
        return actual;
    }

    public Matrix<? extends UpdatablePArray> maskedErosionDilation(
            Matrix<? extends PArray> src,
            Pattern erosionPattern,
            Pattern dilationPattern) {
        Matrix<? extends UpdatablePArray> actual = context(contextPart(0.0, 0.95)).erosionDilation(
                src, erosionPattern, dilationPattern, SubtractionMode.NONE);
        Matrices.applyFunc(contextPart(0.95, 1.0), Func.MAX, actual, actual, src);
        return actual;
    }

    public Matrix<? extends UpdatablePArray> beucherGradient(Matrix<? extends PArray> src, Pattern pattern) {
        Matrix<? extends UpdatablePArray> dilation = context(contextPart(0.0, 0.45)).dilation(src, pattern);
        Matrix<? extends UpdatablePArray> erosion = context(contextPart(0.45, 0.9)).erosion(src, pattern);
        Matrices.applyFunc(contextPart(0.9, 1.0), Func.POSITIVE_DIFF, dilation, dilation, erosion);
        return dilation;
    }

    /**
     * This method must be equivalent to
     * {@link #asDilation(Matrix src, Pattern pattern)}
     * if <code>isDilation</code> argument is <code>true</code> or to
     * {@link #asErosion(Matrix src, Pattern pattern)}
     * if <code>isDilation</code> argument is <code>false</code>.
     *
     * <p>The implementations of those methods, provided by this class,
     * just call this method with corresponding <code>isDilation</code> argument.
     *
     * @param src        the source matrix.
     * @param pattern    the pattern.
     * @param isDilation what should return this method: dilation or erosion.
     * @return the "lazy" matrix containing the dilation or erosion of the source matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    protected abstract Matrix<? extends PArray> asDilationOrErosion(
            Matrix<? extends PArray> src,
            Pattern pattern,
            boolean isDilation);

    /**
     * This method must be equivalent to
     * {@link #dilation(Matrix dest, Matrix src, Pattern pattern, boolean disableMemoryAllocation)}
     * if <code>isDilation</code> argument is <code>true</code> or to
     * {@link #erosion(Matrix dest, Matrix src, Pattern pattern, boolean disableMemoryAllocation)}
     * if <code>isDilation</code> argument is <code>false</code>.
     * There is the only little difference: if <code>dest</code> argument is {@code null},
     * this methods does not throw <code>NullPointerException</code>, but
     * allocates new matrix with the same dimensions and element type as <code>src</code>
     * and use it for storing the result.
     * The result (newly created matrix or non-null <code>dest</code> argument) is returned
     * as the result of this method.
     *
     * <p>The implementations of
     * {@link #dilation(Matrix dest, Pattern pattern)},
     * {@link #erosion(Matrix dest, Pattern pattern)},
     * {@link #dilation(Matrix dest, Matrix src, Pattern pattern, boolean disableMemoryAllocation)},
     * {@link #erosion(Matrix dest, Matrix src, Pattern pattern, boolean disableMemoryAllocation)} methods,
     * provided by this class, just call this method
     * with corresponding <code>isDilation</code> argument and with <code>dest==null</code> in a case of first two
     * methods.
     *
     * <p>The implementation of this method, provided by {@link AbstractMorphology} class, just copies the result
     * of {@link #asDilationOrErosion(Matrix, Pattern, boolean)}  asDilationOrErosion} method to
     * <code>dest</code> matrix:
     * <pre>
     * {@link Matrices#copy(ArrayContext, Matrix, Matrix)
     * Matrices.copy}(context, castDest, {@link #asDilationOrErosion(Matrix, Pattern, boolean)}
     * asDilationOrErosion}(src, pattern, isDilation));</pre>
     *
     * <p>where <code>castDest</code> is <code>dest</code> if <code>dest.elementType()==src.elementType()</code>,
     * the newly created matrix if <code>dest==null</code> or the <code>dest</code> matrix, cast to the necessary
     * element type, if the source and destination element types are different:
     *
     * <pre>
     * {@link
     * Matrices#asUpdatableFuncMatrix(boolean, net.algart.math.functions.Func.Updatable, Class, Matrix)
     * Matrices.asUpdatableFuncMatrix}(true, {@link net.algart.math.functions.Func#UPDATABLE_IDENTITY
     * Func.UPDATABLE_IDENTITY}, src.updatableType(UpdatablePArray.class), dest)
     * </pre>
     *
     * <p>The implementations of this method in the inheritors usually provide better algorithms,
     * especially if <code>disableMemoryAllocation</code> argument is <code>false</code>.
     *
     * @param dest                    the target matrix (or {@code null} for creating a new matrix).
     * @param src                     the source matrix.
     * @param pattern                 the pattern.
     * @param isDilation              what should perform this method: dilation or erosion.
     * @param disableMemoryAllocation if <code>false</code>, this method may allocate additional temporary matrices
     *                                for optimizing the algorithm speed;
     *                                if <code>true</code>, no any work memory will be allocated.
     * @return the reference to <code>dest</code> argument if it is not {@code null},
     * newly allocated resulting matrix in another case.
     * @throws NullPointerException     if <code>src</code> or <code>pattern</code> argument is {@code null}.
     * @throws SizeMismatchException    if <code>dest&nbsp;!=&nbsp;null</code>
     *                                  and the passed matrices have different dimensions.
     * @throws IllegalArgumentException if the number of the pattern dimensions
     *                                  <code>pattern.{@link Pattern#dimCount() dimCount()}</code> is not equal
     *                                  to <code>src.{@link Matrix#dimCount() dimCount()}</code>.
     */
    protected Matrix<? extends UpdatablePArray> dilationOrErosion(
            Matrix<? extends UpdatablePArray> dest,
            Matrix<? extends PArray> src,
            Pattern pattern,
            boolean isDilation,
            boolean disableMemoryAllocation) {
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (!dimensionsAllowed(src, pattern)) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        if (dest == null) {
            dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        } else {
            if (!dest.dimEquals(src)) {
                throw new SizeMismatchException("Destination and source matrix dimensions mismatch: "
                        + dest + " and " + src);
            }
            Matrix<? extends UpdatablePArray> castDest = dest.elementType() == src.elementType() ? dest :
                    Matrices.asUpdatableFuncMatrix(true, Func.UPDATABLE_IDENTITY,
                            src.updatableType(UpdatablePArray.class), dest);
            Matrices.copy(context(), castDest, asDilationOrErosion(src, pattern, isDilation));
        }
        return dest;
    }

    protected boolean dimensionsAllowed(Matrix<? extends PArray> matrix, Pattern pattern) {
        return pattern.dimCount() == matrix.dimCount();
    }
}
