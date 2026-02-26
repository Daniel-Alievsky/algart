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

package net.algart.matrices.skeletons;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.IterativeErosion;
import net.algart.matrices.morphology.Morphology;

import java.util.Objects;

/**
 * <p>The simplest algorithm of multidimensional skeletonization of binary matrices, based on sequential
 * {@link Morphology#erosion(Matrix, Pattern) erosions} of the matrix by some small pattern.</p>
 *
 * <p>More precisely, this class is an implementation of {@link IterativeArrayProcessor} interface,
 * iteratively processing some bit matrix (<code>{@link Matrix}({@link UpdatableBitArray})</code>), named
 * <code>result</code> and passed to the {@link #getInstance getInstance} method.
 * In this implementation:</p>
 *
 * <ul>
 * <li>{@link #performIteration(ArrayContext)} method
 * calculates <code>{@link Morphology#erosion(Matrix, Pattern) erosion}(result,P)</code>
 * of the current <code>result</code> matrix
 * by some small pattern <code>P</code> (usually little circle or square, in 2-dimensional case) and
 * <code>{@link Morphology#opening(Matrix, Pattern, Morphology.SubtractionMode)
 * opening}(result,Q,{@link net.algart.matrices.morphology.Morphology.SubtractionMode#NONE})</code> of this matrix
 * by some other pattern <code>Q</code>, usually equal to <code>P</code> or little greater than <code>P</code>.
 * The opening is subtracted (in the set-theoretical sense) from the source <code>result</code> matrix
 * and the difference (i.e. "thin" areas in the bit image) is united with the erosion
 * (also in the set-theoretical sense).
 * Then the <code>result</code> matrix is replaced with this union.</li>
 *
 * <li>{@link #done()} method returns <code>true</code> if the last iteration was unable to change the matrix:
 * all "objects" are already "thin" (removed after the erosion).</li>
 *
 * <li>{@link #result()} method always returns the reference to the source matrix, passed to
 * {@link #getInstance getInstance} method.</li>
 * </ul>
 *
 * <p>The algorithm, implemented by this class, does not guarantee that connected "objects"
 * (areas filled by 1 elements) stay connected
 * and does not guarantee that the resulting "skeleton" will be "thin" enough.
 * But it guarantees that resulting "skeleton" does not contain areas "larger" than the pattern <code>Q</code>
 * used for opening operation.</p>
 *
 * <p>This class is based on {@link Matrices#asShifted Matrices.asShifted} method
 * with some elementwise logical operations (AND, OR, NOT).
 * So, the matrix is supposed to be infinitely pseudo-cyclically continued, as well
 * {@link Matrices#asShifted Matrices.asShifted} method supposes it.
 * You can change this behavior by appending the source matrix with zero elements
 * by calling {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)} method,
 * where the dimensions of the "submatrix" are greater than dimensions of the source one by 1
 * and the <code>continuationMode</code> argument is
 * {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT}.</p>
 *
 * <p>This class can process a matrix with any number of dimensions.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public class ErodingSkeleton extends AbstractIterativeArrayProcessor<Matrix<? extends UpdatableBitArray>>
    implements IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>>
{
    private final Pattern erosionPattern;
    private final Pattern openingPattern;
    private final Matrix<? extends UpdatableBitArray> result;
    private final Matrix<? extends UpdatableBitArray> temp1;
    private final Matrix<? extends UpdatableBitArray> temp2;
    private boolean done = false;
    private ErodingSkeleton(ArrayContext context, Matrix<? extends UpdatableBitArray> matrix,
        Pattern erosionPattern, Pattern openingPattern)
    {
        super(context);
        Objects.requireNonNull(matrix, "Null matrix argument");
        Objects.requireNonNull(erosionPattern, "Null erosionPattern argument");
        Objects.requireNonNull(openingPattern, "Null openingPattern argument");
        this.erosionPattern = erosionPattern;
        this.openingPattern = openingPattern;
        final boolean differentPatterns = !erosionPattern.equals(openingPattern);
        final MemoryModel mm = mm(memoryModel, matrix, differentPatterns ? 2 : 1);
        this.result = matrix;
        this.temp1 = mm.newMatrix(UpdatableBitArray.class, boolean.class, matrix.dimensions());
        if (differentPatterns)
            this.temp2 = mm.newMatrix(UpdatableBitArray.class, boolean.class, matrix.dimensions());
        else
            this.temp2 = null;
    }

    /**
     * Creates new instance of this class.
     *
     * @param context        the {@link #context() context} that will be used by this object;
     *                       can be {@code null}, in which case it will be ignored.
     * @param matrix         the bit matrix that should be processed and returned by {@link #result()} method.
     * @param erosionPattern the pattern that will be used for erosion operation at every iteration.
     * @param openingPattern the pattern that will be used for opening operation at every iteration.
     * @return               new instance of this class.
     * @throws NullPointerException if one of the arguments is {@code null}.
     */
    public static ErodingSkeleton getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix, Pattern erosionPattern, Pattern openingPattern)
    {
        return new ErodingSkeleton(context, matrix, erosionPattern, openingPattern);
    }

    @Override
    public void performIteration(ArrayContext context) {
        Morphology morphology = BasicMorphology.getInstance(context);
        Class<? extends PArray> type = result.type(PArray.class);
        if (this.temp2 == null) { // openingPattern = erosionPattern
            morphology.context(part(context, 0.0, 0.5)).erosion(temp1, result, erosionPattern);
            // temp1 = m(-)p
            Matrix<? extends PArray> lazyOpening = morphology.asDilation(temp1, erosionPattern);
            Matrix<? extends PArray> lazyRears = Matrices.asFuncMatrix(
                Func.ABS_DIFF, type, result, lazyOpening); // rears = m \ (m(-)p(+)p)
            Matrix<? extends PArray> lazyResult = Matrices.asFuncMatrix(
                Func.MAX, type, temp1, lazyRears);
            // result = m(-)p U rears
            done = !Matrices.compareAndCopy(part(context, 0.5, 1.0), result, lazyResult).changed();
        } else {
            morphology.context(part(context, 0.0, 0.3)).erosion(temp1, result, openingPattern);
            morphology.context(part(context, 0.3, 0.6)).dilation(temp2, temp1, openingPattern);
            // temp2 = m(-)q(+)q
            morphology.context(part(context, 0.6, 0.9)).erosion(temp1, result, erosionPattern);
            // temp1 = m(-)p
            Matrix<? extends PArray> lazyRears = Matrices.asFuncMatrix(
                Func.ABS_DIFF, type, result, temp2); // rears = m \ (m(-)q(+)q)
            Matrix<? extends PArray> lazyResult = Matrices.asFuncMatrix(
                Func.MAX, type, temp1, lazyRears);
            // result = m(-)p U rears
            done = !Matrices.compareAndCopy(part(context, 0.9, 1.0), result, lazyResult).changed();
        }
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public long estimatedNumberOfIterations() {
        return IterativeErosion.estimatedNumberOfIterations(result, erosionPattern);
    }

    @Override
    public Matrix<? extends UpdatableBitArray> result() {
        return result;
    }

    @Override
    public void freeResources(ArrayContext context) {
        temp1.freeResources(context == null ? null : context.part(0.0, temp2 != null ? 1.0 / 3.0 : 0.5));
        if (temp2 != null)
            temp2.freeResources(context == null ? null : context.part(1.0 / 3.0, 2.0 / 3.0));
        result.freeResources(context == null ? null : context.part(temp2 != null ? 2.0 / 3.0 : 0.5, 1.0));
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "simple skeletonizer, " +
            (this.temp2 == null ?
                erosionPattern.toString() :
                "patterns: " + erosionPattern + " (erosion) and " + openingPattern + " (opening)");
    }

    static MemoryModel mm(MemoryModel memoryModel, Matrix<? extends PArray> matrix, int numberOfMatrices) {
        if (Matrices.sizeOf(matrix) > Arrays.SystemSettings.maxTempJavaMemory() / numberOfMatrices) {
            return memoryModel;
        } else {
            return SimpleMemoryModel.getInstance();
        }
    }
}
