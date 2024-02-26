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
 * <p>Stronger version of {@link Quadruple3x5ThinningSkeleton2D} skeletonization algorithm.</p>
 *
 * <p>This class implements almost the same algorithm as {@link Quadruple3x5ThinningSkeleton2D},
 * but the thinning method is more aggressive (and slow: it is the slowest {@link ThinningSkeleton
 * thinning skeleton} in this package). This class is also based on analysis of 3x5 aperture.
 * This algorithm also guarantees that 8-connected "objects"
 * (areas filled by 1 elements) always stay 8-connected:
 * see {@link ThinningSkeleton} interface about the precise sense of this state.</p>
 *
 * <p>The main difference from {@link Quadruple3x5ThinningSkeleton2D} is that this class never keeps
 * 3x3 and 3x2 rectangles, filled by 1 &mdash; they are successfully skeletonized.
 * (More precisely, there is only one degenerated case, when such rectangles can appear in the
 * skeletonization result: if all pixels of the bit matrix,
 * supposed to be infinitely pseudo-cyclically continued, are filled by 1.
 * All skeletonization algorithms do nothing in this case.)
 * The corresponding "bad" cases of {@link Quadruple3x5ThinningSkeleton2D} will be skeletonized
 * in the following or similar way:</p>
 *
 * <pre>
 * . . . . . . . .                     . . . . . . . .
 * 1 . . 1 . . 1 .                     1 . . 1 . . 1 .
 * . 1 . 1 . 1 . .                     . 1 . 1 . 1 . .
 * . . 1 1 1 . . .  is transformed to  . . 1 . 1 . . .
 * . . 1 1 1 . . .                     . . 1 . 1 . . .
 * . 1 . 1 . 1 . .                     . 1 . 1 . 1 . .
 * 1 . . 1 . . 1 .                     1 . . 1 . . 1 .
 *
 * 1 . . 1 . . 1 .                     1 . . 1 . . 1 .
 * . 1 . 1 . 1 . .                     . 1 . 1 . 1 . .
 * . . 1 1 1 . . .                     . . 1 1 1 . . .
 * 1 1 1 1 1 1 1 .  is transformed to  1 1 . 1 . 1 1 .
 * . . 1 1 1 . . .                     . . 1 1 1 . . .
 * . 1 . 1 . 1 . .                     . 1 . 1 . 1 . .
 * 1 . . 1 . . 1 .                     1 . . 1 . . 1 .
 * </pre>
 *
 * <p>Below are some "bad" cases, when some areas cannot be "thinned" by this algorithm:</p>
 *
 * <pre>
 * . . . . . . . .    . . . . . . .      . . . 1 . . .
 * . . . . . . . .    . 1 . . 1 . .      . 1 . 1 . 1 .
 * . . 1 . . 1 . .    . . 1 1 . . .      . . 1 1 1 . .
 * . . . 1 1 . . .    . . 1 1 1 1 .      . . . 1 . . .
 * . . . 1 1 . . .    . 1 . 1 . . .      . 1 1 1 1 1 .
 * . . 1 . . 1 . .    . . . 1 . . .      . . . 1 . . .
 * . . . . . . . .    . . . . . . .      . . . 1 . . .
 * </pre>
 *
 * <p>It is obvious that the left configuration cannot be thinned more without breaking connectivity.
 * Other configurations, theoretically, can be reduced by removing pixels in intersections of horizontal
 * and vertical lines, for example:</p>
 *
 * <pre>
 * . . . ? . . .
 * . ? . ? . ? .
 * . . ? . ? . .
 * . . . ? . . .
 * . ? ? . ? ? .
 * . . . ? . . .
 * . . . ? . . .
 * </pre>
 *
 * <p>But it means appearing strange 1-pixels "holes" in any intersection of horizontal and vertical lines
 * and seems to be a bad idea for most applications.</p>
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
 * by calling <nobr>{@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)}</nobr> method,
 * where the dimensions of the "submatrix" are greater than dimensions of the source one by 1
 * and the <tt>continuationMode</tt> argument is {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT}.</p>
 *
 * <p>This class may be applied to a matrix with any number of dimensions,
 * but it is designed for 2-dimensional case: all other dimensions will be ignored.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithread access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public class StrongQuadruple3x5ThinningSkeleton2D extends AbstractThinningSkeleton2D implements ThinningSkeleton {
    static boolean TOP_3X5_WEAKENING = false; // should be false
    static boolean BOTTOM_3X5_WEAKENING = true; // should be true
    // - These flags must not be true together to avoid breaking connectivity.
    // They are not final to allow changing via reflection.

    private static final int[][] SKELETON_XP = {
        {1, 0},          // 0: -A2
        {-1, 0},         // 1: -C2
        {1, 1},          // 2: -A1
        {0, 1},          // 3: -B1
        {-1, 1, 0, 2},   // 4: -C1, -B0 (for debugging needs only, if TOP_3X5_WEAKENING = true)
        {1, -1},         // 5: -A3
        {0, -1},         // 6: -B3
        {-1, -1, 0, -2}, // 7: -C3, -B4
        {0, 1, 0, -1, -1, 1, -1, -1},                           // 8:  -B1,-B3,-C1,-C3
        {1, 2, 1, 0, 1, -2, 0, 1, 0, -1, -1, 1, -1, 0, -1, -1}, // 9:  -A0,-A2,-A4,-B1,-B3,-C1,-C2,-C3
        {0, 2},                                                 // 10: -B0
        {0, -2},                                                // 11: -B4
        {-1, 2},                                                // 12: -C0
        {-1, -2},                                               // 13: -C4
        {0, 2, 1, 1, -1, 1, 1, 0, -1, 0, 1, -1, -1, -1, 0, -2}, // 14: -B0,-A1,-C1,-A2,-C2,-A3,-C3,-B4
    };
    private static final int[][] SKELETON_YP = rotate90(SKELETON_XP);
    private static final int[][] SKELETON_XM = rotate180(SKELETON_XP);
    private static final int[][] SKELETON_YM = rotate270(SKELETON_XP);
    private static final int[][][] SKELETON_3x5 = {
        SKELETON_XP, SKELETON_YP, SKELETON_XM, SKELETON_YM
    };

    private StrongQuadruple3x5ThinningSkeleton2D(ArrayContext context, Matrix<? extends UpdatableBitArray> matrix) {
        super(context, matrix, true, false);
    }

    /**
     * Creates new instance of this class.
     *
     * @param context the {@link #context() context} that will be used by this object;
     *                may be <tt>null</tt>, then it will be ignored.
     * @param matrix  the bit matrix that should be processed and returned by {@link #result()} method.
     * @return        new instance of this class.
     * @throws NullPointerException if <tt>matrix</tt> argument is <tt>null</tt>.
     */
    public static StrongQuadruple3x5ThinningSkeleton2D getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix)
    {
        return new StrongQuadruple3x5ThinningSkeleton2D(context, matrix);
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
     * <p>All said about {@link Quadruple3x5ThinningSkeleton2D#asThinning(int)} method is correct also in this case,
     * but this method perform little "stronger" thinning.
     * <tt>directionIndex</tt> specifies the "eroded side" of objects,
     * or the direction of thinning:<ul>
     * <li>0 means removing elements from the left, i.e. from the side <nobr>(<i>x</i>&minus;1,<i>y</i>)</nobr>,</li>
     * <li>2 means removing elements from the side <nobr>(<i>x</i>,<i>y</i>&minus;1)</nobr>,</li>
     * <li>4 means removing elements from the right, i.e. from the side <nobr>(<i>x</i>+1,<i>y</i>)</nobr>,</li>
     * <li>6 means removing elements from the side <nobr>(<i>x</i>,<i>y</i>+1)</nobr>.</li>
     * </ul>
     * Odd values (1, 3, 5, 7) are ignored by this method: for these values, the reference to the current
     * {@link IterativeArrayProcessor#result() result()} matrix is returned.
     *
     * @param directionIndex the direction of thinning, from 0 to 7.
     * @return               the thinned view if the current {@link #result()} matrix.
     * @throws IllegalArgumentException if <tt>directionIndex</tt> is not in 0..7 range.
     */
    @Override
    public Matrix<BitArray> asThinning(int directionIndex) {
        if (directionIndex < 0 || directionIndex > 7)
            throw new IllegalArgumentException("Illegal directionIndex = " + directionIndex + " (must be 0..7)");
        if (directionIndex % 2 == 0) {
            return asStrongQuadrupleThinning(result, directionIndex / 2);
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
        return "strong quadruple thinning 2D skeletonizer with 3x5 aperture";
    }

    private static Matrix<BitArray> asStrongQuadrupleThinning(Matrix<? extends BitArray> matrix,
        int directionIndex)
    {
        int[][] points = SKELETON_3x5[directionIndex];
        // Comments below suppose the case SKELETON_XP (directionIndex = 0)
        // We consider the 3x5 aperture of bit (0 or 1) elements:
        //    A0 B0 C0
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
        // If internalOrThin is 1, the central element should usually not be cleared
        // From this moment until "notRemoved =" assignment, we may suppose that it is 0, i.e. A2=0 and C2=1:
        // if not, other calculations have no effect due to the "or" operation. So, the aperture is:
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
        Matrix<BitArray> notRemoved = or(internalOrThin, topArticulation, bottomArticulation, not(dilation4Points));

        // Below we shall destroy the following configurations:
        // .  .  .  .  .  .    .  .  .  .  .  .  .
        // . A0  .  .  1  .    . A0  . C0  .  1  .
        // .  . B1 C1  .  .    .  . B1 C1  1  .  .
        // . A2  o C2  1  .    . A2  o C2  1  1  .
        // .  . B3 C3  .  .    .  . B3 C3  1  .  .
        // . A4  .  .  1  .    . A4  . C4  .  1  .
        // .  .  .  .  .  .    .  .  .  .  .  .  .
        // by removing the center. Namely, we need to remove the center if
        // ~A1 & ~A3 & ~B0 & ~B4 & (erosion8Points1 = A0 & A2 & A4 & B1 & B3 & C1 & C2 & C3),
        // that is we need to save it if A1 | A3 | B0 | B4 | ~erosion8Points1
        // In this case, we are sure that B3 will not be removed at this step:
        // it is a local 3x3-articulation point; so, the connection with A2 will not be lost.
        Matrix<BitArray> erosion8Points1 = and(shifts(matrix, points[9]));
        // points[9] = {-A0,-A2,-A4,-B1,-B3,-C1,-C2,-C3}
        Matrix<BitArray> notRemovedStrongly1 = or(
            shift(matrix, points[2]), // -A1
            shift(matrix, points[5]), // -A3
            shift(matrix, points[10]), // -B0
            shift(matrix, points[11]), // -B4
//            shift(matrix, points[12]), // -C0: this requirement can weaken the skeleton
//            shift(matrix, points[13]), // -C4: this requirement can weaken the skeleton
            not(erosion8Points1));

        /* BAD CODE: leads to connectivity break and violating the condition "left or left-top neighbour must be 0"

        // Below we shall destroy the following configuration (possible result of removing previous one):
        // .  .  .  .  .  .  .
        // .  .  ? B0  ?  .  .
        // .  . A1  . C1  .  .
        // .  . A2  o C2  .  .
        // .  . A3  . C3  .  .
        // .  .  ? B4  ?  .  .
        // .  .  .  .  .  .  .
        // by removing the center. Namely, we need to remove the center if
        // ~B1 & ~B3 & (erosion8Points2 = B0 & A1 & C1 & A2 & C2 & A3 & C3 & B4),
        // that is we need to save it if B1 | B3 | ~erosion8Points2
        Matrix<BitArray> erosion8Points2 = and(shifts(matrix, points[14]));
        // points[14] = {-B0,-A1,-C1,-A2,-C2,-A3,-C3,-B4}
        Matrix<BitArray> notRemovedStrongly2 = or(
            shift(matrix, points[3]), // -B1
            shift(matrix, points[6]), // -B3
            not(erosion8Points2));
        */
        return and(matrix, notRemoved, notRemovedStrongly1/*, notRemovedStrongly2*/);
    }
}
