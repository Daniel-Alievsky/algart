/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import static net.algart.matrices.skeletons.ThinningTools.*;

/**
 * <p>Algorithm of 2-dimensional skeletonization of binary matrices based on 4 thinning steps,
 * corresponding to 4 directions with the step 90 degree, based on analysis of 3x5 aperture.</p>
 *
 * <p>It is a stronger version of {@link OctupleThinningSkeleton2D} skeletonization,
 * the standard variant without diagonal thinning:
 * {@link OctupleThinningSkeleton2D#getInstance(ArrayContext, Matrix, boolean, boolean)
 * OctupleThinningSkeleton2D.getInstance(MemoryModel, Matrix, false, false)}.
 * This class implements almost the same algorithm as {@link OctupleThinningSkeleton2D},
 * but the thinning method is more aggressive (and slow):
 * it can remove pixels even with breaking connectivity in 3x3 pixel aperture,
 * if the connectivity in 3x5 aperture is not broken.
 * This algorithm also guarantees that 8-connected "objects"
 * (areas filled by 1 elements) always stay 8-connected:
 * see {@link ThinningSkeleton} interface about the precise sense of this state.</p>
 *
 * <p>The typical "bad" cases for {@link OctupleThinningSkeleton2D} are successfully
 * skeletonized by this algorithm in the following or similar way:</p>
 *
 * <pre>
 * . . . . 1 . . . .                     . . . . 1 . . . .
 * . . . 1 . 1 . . .                     . . . 1 . 1 . . .
 * . . 1 . 1 . 1 . .                     . . 1 . 1 . 1 . .
 * . 1 . 1 1 1 . 1 .                     . 1 . . 1 . . 1 .
 * 1 . 1 1 1 1 1 . 1  is transformed to  1 . 1 1 1 1 1 . 1
 * . 1 . 1 1 1 . 1 .                     . 1 . . 1 . . 1 .
 * . . 1 . 1 . 1 . .                     . . 1 . 1 . 1 . .
 * . . . 1 . 1 . . .                     . . . 1 . 1 . . .
 * . . . . 1 . . . .                     . . . . 1 . . . .
 * </pre>
 *
 * <p>Examples of the result in the "bad" cases, when some areas cannot be "thinned" by this algorithm:</p>
 *
 * <pre>
 * . . . . . . . .     . . . . . . .      . . . 1 . . .     . . . . . . . .     1 . . 1 . . 1 .
 * . . . . . . . .     . 1 . . 1 . .      . 1 . 1 . 1 .     1 . . 1 . . 1 .     . 1 . 1 . 1 . .
 * . . 1 . . 1 . .     . . 1 1 . . .      . . 1 1 1 . .     . 1 . 1 . 1 . .     . . 1 1 1 . . .
 * . . . 1 1 . . .     . . 1 1 1 1 .      . . . 1 . . .     . . 1 1 1 . . .     1 1 1 1 1 1 1 .
 * . . . 1 1 . . .     . 1 . 1 . . .      . . 1 1 1 . .     . . 1 1 1 . . .     . . 1 1 1 . . .
 * . . 1 . . 1 . .     . . . 1 . . .      . 1 . 1 . 1 .     . 1 . 1 . 1 . .     . 1 . 1 . 1 . .
 * . . . . . . . .     . . . . . . .      . . . 1 . . .     1 . . 1 . . 1 .     1 . . 1 . . 1 .
 * </pre>
 *
 * <p>It is obvious that the left configuration cannot be thinned more without breaking connectivity.</p>
 *
 * <p>I recommend to run this algorithm after finishing {@link OctupleThinningSkeleton2D}
 * and {@link WeakOctupleThinningSkeleton2D} algorithms
 * (for example, with help of {@link IterativeArrayProcessor#chain(IterativeArrayProcessor, double)} method).</p>
 *
 * <p>This class is based on {@link Matrices#asShifted Matrices.asShifted} method
 * with some elementwise logical operations (AND, OR, NOT).
 * So, the matrix is supposed to be infinitely pseudo-cyclically continued, as well
 * {@link Matrices#asShifted Matrices.asShifted} method supposes it.
 * You can change this behavior by appending the source matrix with zero elements
 * by calling {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)} method,
 * where the dimensions of the "submatrix" are greater than dimensions of the source one by 1
 * and the <code>continuationMode</code> argument is {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT}.</p>
 *
 * <p>This class may be applied to a matrix with any number of dimensions,
 * but it is designed for 2-dimensional case: all other dimensions will be ignored.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 * @see StrongQuadruple3x5ThinningSkeleton2D
 */
public class Quadruple3x5ThinningSkeleton2D extends AbstractThinningSkeleton2D implements ThinningSkeleton {
    static final boolean TOP_3X5_WEAKENING = false; // should be false
    static final boolean BOTTOM_3X5_WEAKENING = true; // should be true
    // - These flags must not be true together to avoid breaking connectivity.

    private static final int[][] SKELETON_XP = {
        {1, 0},          // 0: -A2
        {-1, 0},         // 1: -C2
        {1, 1},          // 2: -A1
        {0, 1},          // 3: -B1
        {-1, 1, 0, 2},   // 4: -C1, -B0 (for debugging needs only, if TOP_3X5_WEAKENING = true)
        {1, -1},         // 5: -A3
        {0, -1},         // 6: -B3
        {-1, -1, 0, -2}, // 7: -C3, -B4
        {0, 1, 0, -1, -1, 1, -1, -1}, // 8:  -B1,-B3,-C1,-C3
    };
    private static final int[][] SKELETON_YP = rotate90(SKELETON_XP);
    private static final int[][] SKELETON_XM = rotate180(SKELETON_XP);
    private static final int[][] SKELETON_YM = rotate270(SKELETON_XP);
    private static final int[][][] SKELETON_3x5 = {
        SKELETON_XP, SKELETON_YP, SKELETON_XM, SKELETON_YM
    };

    private Quadruple3x5ThinningSkeleton2D(ArrayContext context, Matrix<? extends UpdatableBitArray> matrix) {
        super(context, matrix, true, false);
    }

    /**
     * Creates new instance of this class.
     *
     * @param context the {@link #context() context} that will be used by this object;
     *                can be {@code null}, then it will be ignored.
     * @param matrix  the bit matrix that should be processed and returned by {@link #result()} method.
     * @return        new instance of this class.
     * @throws NullPointerException if <code>matrix</code> argument is {@code null}.
     */
    public static Quadruple3x5ThinningSkeleton2D getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix)
    {
        return new Quadruple3x5ThinningSkeleton2D(context, matrix);
    }

    @Override
    public long estimatedNumberOfIterations() {
        return ThinningTools.estimatedNumberOfIterations(result, false);
    }

    // WARNING: SUN BUG IN javadoc UTILITY (1.6.0_04, 1.7.0-ea)!
    // Below we cannot write "{@link #result()}" - it leads to ClassCastException in javadoc.
    /**
     * Returns current {@link IterativeArrayProcessor#result() result()} matrix thinned along the given direction.
     * The result is "lazy": it is only a view of the current matrix.
     *
     * <p>The precise algorithm of thinning is not documented.
     * Generally speaking, the "thinning" means removing elements
     * from the boundary of any "object" (area of the matrix filled by 1).
     * <code>directionIndex</code> specifies the "eroded side" of objects,
     * or the direction of thinning:<ul>
     * <li>0 means removing elements from the left, i.e. from the side (<i>x</i>&minus;1,<i>y</i>),</li>
     * <li>2 means removing elements from the side (<i>x</i>,<i>y</i>&minus;1),</li>
     * <li>4 means removing elements from the right, i.e. from the side (<i>x</i>+1,<i>y</i>),</li>
     * <li>6 means removing elements from the side (<i>x</i>,<i>y</i>+1).</li>
     * </ul>
     * Odd values (1, 3, 5, 7) are ignored by this method: for these values, the reference to the current
     * {@link IterativeArrayProcessor#result() result()} matrix is returned.
     *
     * <p>Though the algorithm is not documented, there are the following guarantees:
     * <ul>
     * <li>this algorithm never sets zero elements to unit: if the element of the current matrix
     * with some coordinates (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>) is 0,
     * then the element with the same coordinates in the returned matrix is also 0;</li>
     *
     * <li>each element of the returned matrix with coordinates
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>)
     * depends only on the elements in 3x5 or 5x3 aperture of the current matrix:
     * <ul>
     * <li><i>x</i><sub>0</sub>&minus;1&le;<i>x</i>&le;<i>x</i><sub>0</sub>+1,
     * <i>y</i><sub>0</sub>&minus;2&le;<i>y</i>&le;<i>y</i><sub>0</sub>+2,
     * if <code>directionIndex</code> is 0 or 2,</li>
     * <li><i>x</i><sub>0</sub>&minus;2&le;<i>x</i>&le;<i>x</i><sub>0</sub>+2,
     * <i>y</i><sub>0</sub>&minus;1&le;<i>y</i>&le;<i>y</i><sub>0</sub>+1,
     * if <code>directionIndex</code> is 1 or 3;</li>
     * </ul></li>
     *
     * <li>moreover, the element of the returned matrix with coordinates
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>) does not depend on <i>isolated</i>
     * unit elements in the aperture, specified above, i.e.
     * if there is no 8-connected series of unit elements, connecting the central
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>) unit element with another unit element
     * (<i>x</i>, <i>y</i>) in this aperture, then the result value of the central element
     * (will it be cleared or no) does not depend on
     * (<i>x</i>, <i>y</i>) element of the current matrix.
     * For example, in cases <code>directionIndex=0</code> and <code>directionIndex=2</code>, if the central element is 1
     * and <i>y</i>&ge;<i>y</i><sub>0</sub>, the (<i>x</i>, <i>y</i>) is isolated
     * in the following three situations:
     * a) <i>y</i>=<i>y</i><sub>0</sub>+2 and all 3 elements with <i>y</i>=<i>y</i><sub>0</sub>+1 are zero;
     * b) <i>y</i>=<i>y</i><sub>0</sub>+2, <i>x</i>=<i>x</i><sub>0</sub>+1 and 3 elements
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>+2),
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>+1),
     * (<i>x</i><sub>0</sub>+1, <i>y</i><sub>0</sub>+1) are zero;
     * c) <i>y</i>=<i>y</i><sub>0</sub>+2, <i>x</i>=<i>x</i><sub>0</sub>&minus;1 and 3 elements
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>+2),
     * (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>+1),
     * (<i>x</i><sub>0</sub>&minus;1, <i>y</i><sub>0</sub>+1) are zero;
     * </li>
     *
     * <li>if all elements of the current matrix in 3x3 (not 3x5!) aperture
     * <i>x</i><sub>0</sub>&minus;1&le;<i>x</i>&le;<i>x</i><sub>0</sub>+1,
     * <i>y</i><sub>0</sub>&minus;1&le;<i>y</i>&le;<i>y</i><sub>0</sub>+1
     * are inside the matrix (i.e. 1&le;<i>x</i><sub>0</sub>&le;<code>dimX</code>&minus;2,
     * 1&le;<i>y</i><sub>0</sub>&le;<code>dimY</code>&minus;2,
     * <code>dimX</code> and <code>dimY</code> are dimensions of the matrix)
     * and all they are equal to 1, then the element (<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>)
     * in the returned matrix will be equal to 1.</li>
     * </ul>
     *
     * @param directionIndex the direction of thinning, from 0 to 7.
     * @return               the thinned view if the current {@link #result()} matrix.
     * @throws IllegalArgumentException if <code>directionIndex</code> is not in 0..7 range.
     */
    @Override
    public Matrix<BitArray> asThinning(int directionIndex) {
        if (directionIndex < 0 || directionIndex > 7)
            throw new IllegalArgumentException("Illegal directionIndex = " + directionIndex + " (must be 0..7)");
        if (directionIndex % 2 == 0) {
            return asQuadrupleThinning(result, directionIndex / 2);
        } else {
            return result.cast(BitArray.class);
        }
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "quadruple thinning 2D skeletonizer with 3x5 aperture";
    }

    private static Matrix<BitArray> asQuadrupleThinning(Matrix<? extends BitArray> matrix,
        int directionIndex)
    {
        int[][] points = SKELETON_3x5[directionIndex];
        // Comments below suppose the case SKELETON_XP (directionIndex = 0)
        // We consider the 3x5 aperture of bit (0 or 1) elements:
        //   (A0 B0 C0)
        //    A1 B1 C1
        //    A2  1 C2
        //    A3 B3 C3
        //    A4 B4 C4
        // The central element can be supposed to be 1, because the last operation of the method is "and"
        // (if the central element is 0, all other calculations have no effect).
        // We need to clear the central element to 0 in some "thinning" situation.

        Matrix<BitArray> internal = shift(matrix, points[0]); // -A2
        Matrix<BitArray> internalOrThin = or(internal, not(shift(matrix, points[1]))); // -C2
        // internalOrThin is 0 if ~A2 & C2 (it is a left boundary and not both A2 and C2 are 0): candidate for removal
        // If internalOrThin is 1, the central element must never be cleared.
        // From this moment, we may suppose that it is 0, i.e. A2=0 and C2=1:
        // if not, other calculations have no effect due to the last "or" operation. So, the aperture is:
        //    A0 B0 C0
        //    A1 B1 C1
        //     0  1  1
        //    A3 B3 C3
        //    A4 B4 C4

        Matrix<BitArray> topArticulation =  andNot(shift(matrix, points[2]),
            TOP_3X5_WEAKENING ?
                or(shift(matrix, points[3]), and(shifts(matrix, points[4]))) :
                shift(matrix, points[3]));
        // points[2] = -A1, points[3] = -B1, points[4] = {-C1, -B0}
        // TOP_3X5_WEAKENING=true:  topArticulation = 1 if A1 & ~(B1 | (B0 & C1)): a local 3x5 articulation point
        // TOP_3X5_WEAKENING=false: topArticulation = 1 if A1 & ~B1: a local 3x3 articulation point
        Matrix<BitArray> bottomArticulation = andNot(shift(matrix, points[5]),
            BOTTOM_3X5_WEAKENING ?
                or(shift(matrix, points[6]), and(shifts(matrix, points[7]))) :
                shift(matrix, points[6]));
        // points[5] = -A3, points[6] = -B3, points[7] = {-C3, -B4}
        // BOTTOM_3X5_WEAKENING=true:  bottomArticulation = 1 if A3 & ~(B3 | (B4 & C3)): a local 3x5 articulation point
        // BOTTOM_3X5_WEAKENING=false: bottomArticulation = 1 if A3 & ~B3: a local 3x3 articulation point

        Matrix<BitArray> dilation4Points = or(shifts(matrix, points[8])); // points[8] = {-B1,-B3,-C1,-C3}
        // dilation4Points is 0 if B1=B3=C1=C3=0: we have a "free end" of a horizontal line.
        // Note: such algorithm removes the central point in the situation
        // (1) .  1  . (1)
        // (1) 1  .  1 (1)
        // (1) .  1  1 (1)
        // (1) 1  .  1 (1)
        // (1) .  1  . (1)
        Matrix<BitArray> notRemoved = or(internalOrThin, topArticulation, bottomArticulation,
            not(dilation4Points));
        return and(matrix, notRemoved);
    }
}
