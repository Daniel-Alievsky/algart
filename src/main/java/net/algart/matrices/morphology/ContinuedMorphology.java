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
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.Pattern;

import java.util.Objects;

import static net.algart.matrices.DependenceApertureBuilder.*;

/**
 * <p>The filter allowing to transform any {@link Morphology} object to another instance of that interface,
 * which uses some non-trivial form of continuation outside the source matrix.</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * implementing {@link Morphology}, and some {@link net.algart.arrays.Matrix.ContinuationMode continuation mode}.
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
 * The size of extension along all coordinates depends on the patterns and the performed operation.
 * After this, the corresponding method of <i>parent</i> object processes the appended matrix,
 * and the method of this class returns the corresponding submatrix of the result, with dimensions, equal
 * to the dimensions of the source matrix.</p>
 *
 * <p>The processing is little different for methods, placing the result into the first argument
 * <code>Matrix&lt;? extends UpdatablePArray&gt; dest</code>, like
 * {@link #dilation(Matrix, Matrix, Pattern)} and {@link #erosion(Matrix, Matrix, Pattern)}.
 * In these cases, the destination (updatable) matrix is (virtually) extended like the source matrix
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
 * {@link #getInstance(Morphology, Matrix.ContinuationMode)} leads
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
public class ContinuedMorphology implements Morphology {
    private static final boolean SIMPLIFIED_CONSTANT_CONTINUATION = true; // little optimization

    private final Morphology parent;
    final ArrayContext context;
    final MemoryModel memoryModel;
    final Matrix.ContinuationMode continuationMode;

    ContinuedMorphology(Morphology parent, Matrix.ContinuationMode continuationMode) {
        Objects.requireNonNull(parent, "Null parent morphology");
        Objects.requireNonNull(continuationMode, "Null continuationMode derivator");
        if (continuationMode == Matrix.ContinuationMode.NONE) {
            throw new IllegalArgumentException(getClass().getName() + " cannot be used with continuation mode \""
                    + continuationMode + "\"");
        }
        this.parent = parent;
        this.context = parent.context() == null ? ArrayContext.DEFAULT : parent.context();
        this.memoryModel = this.context.getMemoryModel();
        this.continuationMode = continuationMode;
    }

    /**
     * Returns new instance of this class with the passed parent {@link Morphology} object
     * and the specified continuation mode.
     * See comments to {@link net.algart.arrays.Matrix.ContinuationMode} class
     * for more information about possible continuations.
     *
     * @param parent           the instance of {@link Morphology} interface that will perform all operations.
     * @param continuationMode the mode of continuation outside the source matrix.
     * @return new instance of this class.
     * @throws NullPointerException     if <code>parent</code> or <code>continuationMode</code>
     *                                  argument is {@code null}.
     * @throws IllegalArgumentException if <code>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</code>.
     * @see #parent()
     * @see #continuationMode()
     */
    public static ContinuedMorphology getInstance(Morphology parent, Matrix.ContinuationMode continuationMode) {
        return new ContinuedMorphology(parent, continuationMode);
    }

    /**
     * Returns the parent {@link Morphology} object, passed to
     * {@link #getInstance(Morphology, Matrix.ContinuationMode)} method.
     *
     * @return the parent {@link Morphology} object.
     */
    public Morphology parent() {
        return this.parent;
    }

    /**
     * Returns the continuation mode, used by this object for virtual continuing the source matrix.
     * The returned value is equal to the corresponding argument of
     * {@link #getInstance(Morphology, Matrix.ContinuationMode)}
     * or {@link ContinuedRankMorphology#getInstance(RankMorphology, Matrix.ContinuationMode)} method.
     *
     * @return the continuation mode, that will be used for continuation outside the source matrix.
     */
    public Matrix.ContinuationMode continuationMode() {
        return this.continuationMode;
    }

    public ArrayContext context() {
        return this.context;
    }

    public Morphology context(ArrayContext newContext) {
        return new ContinuedMorphology(parent.context(newContext), continuationMode);
    }

    public boolean isPseudoCyclic() {
        return this.continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC;
    }

    /*Repeat() asDilation  ==> asErosion,,dilation,,erosion,,
                               weakDilation,,weakErosion,,beucherGradient;;
               (pattern,\s+)false ==> $1true,,$1false,,$1true,,
                               $1false, $1true,,$1true, $1false,,$1false, $1true;;
               SUM         ==> SUM,,SUM,,SUM,,
                               SUM_MAX_0,,SUM_MAX_0,,MAX;;
               (extends\s+PArray)(?!>\s+src) ==> $1,,extends UpdatablePArray,,... */

    public Matrix<? extends PArray> asDilation(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = SUM.getAperture(src.dimCount(), pattern, false);
        return reduce(parent.asDilation(extend(src, a), pattern), a);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public Matrix<? extends PArray> asErosion(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = SUM.getAperture(src.dimCount(), pattern, true);
        return reduce(parent.asErosion(extend(src, a), pattern), a);
    }

    public Matrix<? extends UpdatablePArray> dilation(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = SUM.getAperture(src.dimCount(), pattern, false);
        return reduce(parent.dilation(extend(src, a), pattern), a);
    }

    public Matrix<? extends UpdatablePArray> erosion(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = SUM.getAperture(src.dimCount(), pattern, true);
        return reduce(parent.erosion(extend(src, a), pattern), a);
    }

    public Matrix<? extends UpdatablePArray> weakDilation(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = SUM_MAX_0.getAperture(src.dimCount(), pattern, false, pattern, true);
        return reduce(parent.weakDilation(extend(src, a), pattern), a);
    }

    public Matrix<? extends UpdatablePArray> weakErosion(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = SUM_MAX_0.getAperture(src.dimCount(), pattern, true, pattern, false);
        return reduce(parent.weakErosion(extend(src, a), pattern), a);
    }

    public Matrix<? extends UpdatablePArray> beucherGradient(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = MAX.getAperture(src.dimCount(), pattern, false, pattern, true);
        return reduce(parent.beucherGradient(extend(src, a), pattern), a);
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() dilation ==> erosion,,closing,,opening;;
               (pattern,\s+)false ==> $1true,,$1false, $1true,,$1true, $1false */

    public Matrix<? extends UpdatablePArray> dilation(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        IRectangularArea a = (subtractionMode == SubtractionMode.NONE ? SUM : SUM_MAX_0).getAperture(
                src.dimCount(), pattern, false);
        return reduce(parent.dilation(extend(src, a), pattern, subtractionMode), a);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public Matrix<? extends UpdatablePArray> erosion(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        IRectangularArea a = (subtractionMode == SubtractionMode.NONE ? SUM : SUM_MAX_0).getAperture(
                src.dimCount(), pattern, true);
        return reduce(parent.erosion(extend(src, a), pattern, subtractionMode), a);
    }

    public Matrix<? extends UpdatablePArray> closing(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        IRectangularArea a = (subtractionMode == SubtractionMode.NONE ? SUM : SUM_MAX_0).getAperture(
                src.dimCount(), pattern, false, pattern, true);
        return reduce(parent.closing(extend(src, a), pattern, subtractionMode), a);
    }

    public Matrix<? extends UpdatablePArray> opening(
            Matrix<? extends PArray> src,
            Pattern pattern,
            SubtractionMode subtractionMode) {
        IRectangularArea a = (subtractionMode == SubtractionMode.NONE ? SUM : SUM_MAX_0).getAperture(
                src.dimCount(), pattern, true, pattern, false);
        return reduce(parent.opening(extend(src, a), pattern, subtractionMode), a);
    }

    /*Repeat.AutoGeneratedEnd*/


    /*Repeat() dilationErosion ==> erosionDilation,,maskedDilationErosion,,maskedErosionDilation;;
               (ptn1,\s+)false,\s+(ptn2,\s+)true ==> $1true, $2false,,$1false, $2true,,$1true, $2false;;
               (,\s*(?:SubtractionMode\s+)?subtractionMode) ==> $1,, ,, ;;
               SUM             ==> SUM,,SUM_MAX_0,,SUM_MAX_0 */

    public Matrix<? extends UpdatablePArray> dilationErosion(
            Matrix<? extends PArray> src,
            Pattern ptn1,
            Pattern ptn2,
            SubtractionMode subtractionMode) {
        IRectangularArea a = SUM.getAperture(src.dimCount(), ptn1, false, ptn2, true);
        return reduce(parent.dilationErosion(extend(src, a), ptn1, ptn2, subtractionMode), a);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public Matrix<? extends UpdatablePArray> erosionDilation(
            Matrix<? extends PArray> src,
            Pattern ptn1,
            Pattern ptn2,
            SubtractionMode subtractionMode) {
        IRectangularArea a = SUM.getAperture(src.dimCount(), ptn1, true, ptn2, false);
        return reduce(parent.erosionDilation(extend(src, a), ptn1, ptn2, subtractionMode), a);
    }

    public Matrix<? extends UpdatablePArray> maskedDilationErosion(
            Matrix<? extends PArray> src,
            Pattern ptn1,
            Pattern ptn2) {
        IRectangularArea a = SUM_MAX_0.getAperture(src.dimCount(), ptn1, false, ptn2, true);
        return reduce(parent.maskedDilationErosion(extend(src, a), ptn1, ptn2), a);
    }

    public Matrix<? extends UpdatablePArray> maskedErosionDilation(
            Matrix<? extends PArray> src,
            Pattern ptn1,
            Pattern ptn2) {
        IRectangularArea a = SUM_MAX_0.getAperture(src.dimCount(), ptn1, true, ptn2, false);
        return reduce(parent.maskedErosionDilation(extend(src, a), ptn1, ptn2), a);
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() dilation ==> erosion;; (dimCount\(\),\s+pattern,\s+)false ==> $1true */

    public void dilation(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern,
            boolean disableMemoryAllocation) {
        Matrices.checkDimensionEquality(dest, src);
        IRectangularArea a = (disableMemoryAllocation ? SUM_MAX_0 : SUM).getAperture(src.dimCount(), pattern, false);
        // SUM_MAX_0: including the origin is necessary while extending the destination matrix
        Matrix<? extends PArray> continued = extend(src, a);
        if (disableMemoryAllocation) { // in this case we should not create extra matrices
            parent.dilation(extend(dest, a, true), continued, pattern, disableMemoryAllocation);
        } else { // in this case destination matrix can be used as a temporary storage: it's better to work quickly
            Matrix<? extends UpdatablePArray> temp = memoryModel.newMatrix(UpdatablePArray.class, continued);
            parent.context(context.part(0.0, 0.95)).dilation(temp, continued, pattern, disableMemoryAllocation);
            Matrices.copy(context.part(0.95, 1.0), dest, reduce(temp, a));
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public void erosion(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern,
            boolean disableMemoryAllocation) {
        Matrices.checkDimensionEquality(dest, src);
        IRectangularArea a = (disableMemoryAllocation ? SUM_MAX_0 : SUM).getAperture(src.dimCount(), pattern, true);
        // SUM_MAX_0: including the origin is necessary while extending the destination matrix
        Matrix<? extends PArray> continued = extend(src, a);
        if (disableMemoryAllocation) { // in this case we should not create extra matrices
            parent.erosion(extend(dest, a, true), continued, pattern, disableMemoryAllocation);
        } else { // in this case destination matrix can be used as a temporary storage: it's better to work quickly
            Matrix<? extends UpdatablePArray> temp = memoryModel.newMatrix(UpdatablePArray.class, continued);
            parent.context(context.part(0.0, 0.95)).erosion(temp, continued, pattern, disableMemoryAllocation);
            Matrices.copy(context.part(0.95, 1.0), dest, reduce(temp, a));
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    public void dilation(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        dilation(dest, src, pattern, false);
    }

    public void erosion(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        erosion(dest, src, pattern, false);
    }

    private <T extends PArray> Matrix<T> extend(Matrix<T> src, IRectangularArea aperture) {
        return extend(src, aperture, false);
    }

    private <T extends PArray> Matrix<T> extend(Matrix<T> m, IRectangularArea aperture, boolean dest) {
        if (m.isEmpty()) {
            return m; // don't try to extend empty matrix: it is even impossible for some continuation modes
        }
        if (!parent.isPseudoCyclic()) {
            aperture = aperture.expand(IPoint.origin(aperture.coordCount()));
        }
        if (SIMPLIFIED_CONSTANT_CONTINUATION) {
            if (continuationMode.isConstant() && aperture.contains(IPoint.origin(m.dimCount()))) {
                long[] size = m.dimensions();
                for (int k = 0; k < size.length; k++) {
                    size[k] += aperture.size(k);
                    if (size[k] < 0) {
                        throw new IndexOutOfBoundsException("Too large matrix continuation for morphology: "
                                + "the dimension #" + k + " of the matrix, extended to the corresponding aperture "
                                + aperture + ", is greater than Long.MAX_VALUE");
                    }
                }
                return m.subMatr(new long[size.length], size, continuationMode);
                // new long[...] - zero-filled by Java; no sense to replace with ZERO_CONSTANT here
            }
        }
        long[] from = aperture.min().coordinates();
        long[] to = IPoint.of(m.dimensions()).add(aperture.max()).coordinates();
        for (int k = 0; k < to.length; k++) {
            if (to[k] < 0 && aperture.max(k) >= 0) {
                throw new IndexOutOfBoundsException("Too large matrix continuation for morphology: "
                        + "the dimension #" + k + " of the matrix, extended to the corresponding aperture "
                        + aperture + ", is greater than Long.MAX_VALUE");
            }
        }
        return m.subMatrix(from, to, dest ? Matrix.ContinuationMode.ZERO_CONSTANT : continuationMode);
    }

    private <T extends PArray> Matrix<T> reduce(Matrix<T> continued, IRectangularArea aperture) {
        if (continued.isEmpty()) {
            return continued;
        }
        if (!parent.isPseudoCyclic()) {
            aperture = aperture.expand(IPoint.origin(aperture.coordCount()));
        }
        if (SIMPLIFIED_CONSTANT_CONTINUATION) {
            if (continuationMode.isConstant() && aperture.contains(IPoint.origin(continued.dimCount()))) {
                long[] size = continued.dimensions();
                for (int k = 0; k < size.length; k++) {
                    size[k] -= aperture.size(k);
                }
                return continued.subMatr(new long[size.length], size, continuationMode);
                // new long[...] - zero-filled by Java
            }
        }
        long[] from = aperture.min().symmetric().coordinates();
        long[] to = IPoint.of(continued.dimensions()).subtract(aperture.max()).coordinates();
        return continued.subMatrix(from, to, Matrix.ContinuationMode.PSEUDO_CYCLIC);
    }
}
