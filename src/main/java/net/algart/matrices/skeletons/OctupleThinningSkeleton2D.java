/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Algorithm of 2-dimensional skeletonization of binary matrices based on 8 thinning steps,
 * corresponding to 8 directions with the step 45 degree, based on analysis of 3x3 aperture.</p>
 *
 * <p>More precisely, this class is an implementation of {@link ThinningSkeleton} interface,
 * iteratively processing some bit matrix (<tt>{@link Matrix}({@link UpdatableBitArray})</tt>), named
 * <tt>result</tt> and passed to the {@link #getInstance getInstance} method.
 * In this implementation:</p>
 *
 * <ul>
 * <li>{@link #performIteration(ArrayContext)} method sequentially calls
 * {@link #asThinning(int directionIndex)} method and copies its result to
 * the <tt>result</tt> matrix for <tt>directionIndex=0,1,2,3,4,5,6,7</tt>.
 * It means, that all "objects" in the matrix (areas filled by 1 elements)
 * are "thinned" 8 times: from left direction, from left-top diagonal direction, etc.
 * Depending on the argument of an instantiation method, {@link #performIteration(ArrayContext)} may skip
 * calling {@link #asThinning asThinning} for odd directions (1,3,5,7).
 * </li>
 *
 * <li>{@link #done()} method returns <tt>true</tt> if the last iteration was unable to change the matrix:
 * all "objects" are already "thin".</li>
 *
 * <li>{@link #result()} method always returns the reference to the source matrix, passed to
 * {@link #getInstance getInstance} method.</li>
 * </ul>
 *
 * <p>The algorithm, implemented by this class, guarantees that 8-connected "objects"
 * (areas filled by 1 elements) always stay 8-connected;
 * see {@link ThinningSkeleton} interface about the precise sense of this state.
 * The resulting "skeleton" are usually "thin" enough (1-pixel lines),
 * but some little not "thin" areas are possible.
 * An example of resulting skeleton:</p>
 *
 * <pre>
 * . . . . . . . .
 * . 1 1 . . . . .
 * . . . 1 . . . .
 * . . . 1 . . . .
 * . . . . 1 . . .
 * . . . . . 1 . .
 * . . . . . . 1 .
 * . . . . . . . .
 * </pre>
 *
 * <p>Examples of the result in the "bad" cases, when some areas cannot be "thinned" by this algorithm:</p>
 *
 * <pre>
 * . . . . . . . . .     . . . . . . . .     . . . . . . . .     . . . 1 . . . .
 * . . . 1 . 1 . . .     . . . . . . . .     . . . 1 . . . .     1 . . 1 . . 1 .
 * . . 1 . 1 . 1 . .     . . . . . . . .     1 . . 1 . . 1 .     . 1 . 1 . 1 . .
 * . 1 . 1 1 1 . 1 .     . . 1 . . 1 . .     . 1 . 1 . 1 . .     . . 1 1 1 . . .
 * . . 1 1 1 1 1 . .     . . . 1 1 . . .     . . 1 1 1 . . .     1 1 1 1 1 1 1 .
 * . 1 . 1 1 1 . 1 .     . . . 1 1 . . .     . . 1 1 1 . . .     . . 1 1 1 . . .
 * . . 1 . 1 . 1 . .     . . 1 . . 1 . .     . 1 . 1 . 1 . .     . 1 . 1 . 1 . .
 * . . . 1 . 1 . . .     . . . . . . . .     1 . . 1 . . 1 .     1 . . 1 . . 1 .
 * . . . . . . . . .     . . . . . . . .     . . . 1 . . . .     . . . 1 . . . .
 * </pre>
 *
 * <p>The left example can have any size: it is possible to construct very large area filled by 1, which cannot
 * be skeletonized. But it can be excluded by little
 * {@link net.algart.matrices.morphology.Morphology#closing(Matrix,
 * net.algart.math.patterns.Pattern, net.algart.matrices.morphology.Morphology.SubtractionMode) closing}
 * of the source matrix by the rectangle 2x1 before running skeletonization.
 * As an alternative, the left case can be processed by {@link Quadruple3x5ThinningSkeleton2D} algorithm,
 * performed after this skeletonization.</p>
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
 * @version 1.2
 * @since JDK 1.5
 * @see WeakOctupleThinningSkeleton2D
 */
public class OctupleThinningSkeleton2D extends AbstractThinningSkeleton2D implements ThinningSkeleton {
    private static final int[][] SKELETON_XP = {
        {1, 0},                       // 0: -A2
        {-1, 0},                      // 1: -C2
        {1, 1},                       // 2: -A1
        {0, 1},                       // 3: -B1
        {1, -1},                      // 4: -A3
        {0, -1},                      // 5: -B3
        {0, 1, 0, -1, -1, 1, -1, -1}, // 6: -B1,-B3,-C1,-C3
    };
    private static final int[][] SKELETON_YP = rotate90(SKELETON_XP);
    private static final int[][] SKELETON_XM = rotate180(SKELETON_XP);
    private static final int[][] SKELETON_YM = rotate270(SKELETON_XP);
    private static final int[][][] QUADRUPLE_SKELETON = {
        SKELETON_XP, SKELETON_YP, SKELETON_XM, SKELETON_YM
    };

    private static final int[][] SKELETON_XPYP = {
        {1, 0, 1, 1, 0, 1},               // 0: -A2,-A1,-B1
        {0, -1, -1, 0},                   // 1: -B3,-C2
        {1, -1, 1, 0, 1, 1, 0, 1, -1, 1}, // 2: -A3,-A2,-A1,-B1,-C1
    };
    private static final int[][] SKELETON_XMYP = rotate90(SKELETON_XPYP);
    private static final int[][] SKELETON_XMYM = rotate180(SKELETON_XPYP);
    private static final int[][] SKELETON_XPYM = rotate270(SKELETON_XPYP);
    private static final int[][][] DIAGONAL_SKELETON = {
        SKELETON_XPYP, SKELETON_XMYP, SKELETON_XMYM, SKELETON_XPYM
    };

    private final boolean topological;

    private OctupleThinningSkeleton2D(ArrayContext arrayContext, Matrix<? extends UpdatableBitArray> matrix,
        boolean diagonalThinning, boolean topological)
    {
        super(arrayContext, matrix, true, diagonalThinning); // false may be passed here for debugging
        this.topological = topological;
    }

    /**
     * Creates new instance of this class.
     *
     * <p>If the <tt>diagonalThinning</tt> argument is <tt>false</tt>, the algorithm will skip thinning
     * along diagonal directions (<tt>directionIndex=1,3,5,7</tt>).
     * The result will be still correct, but the lines of the skeleton will be not so even.
     *
     * <p>If the <tt>topological</tt> is <tt>true</tt>, the algorithm doesn't stop when all objects in the matrix
     * become "thin" (1-pixel thickness), but continues shortening all "free ends" of all skeleton lines,
     * while there is at least one "free end". As a result, objects that have no "holes" will be removed at all,
     * objects that have 1 hole will be transformed into 1-pixel closed line ("ring"), etc.
     * This mode essentially slows down the algorithm.
     *
     * @param context          the {@link #context() context} that will be used by this object;
     *                         may be <tt>null</tt>, then it will be ignored.
     * @param matrix           the bit matrix that should be processed and returned by {@link #result()} method.
     * @param diagonalThinning whether the algorithm will perform diagonal thinning; usually <tt>true</tt>.
     * @param topological      whether the algorithm will shorten isolated thin lines with "free ends".
     * @return                 new instance of this class.
     * @throws NullPointerException if <tt>matrix</tt> argument is <tt>null</tt>.
     */
    public static OctupleThinningSkeleton2D getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix,
        boolean diagonalThinning, boolean topological)
    {
        return new OctupleThinningSkeleton2D(context, matrix, diagonalThinning, topological);
    }

    /**
     * Creates new instance of this class.
     * Equivalent to {@link #getInstance(ArrayContext, Matrix, boolean, boolean)
     * getInstance(context, matrix, true, false)}.
     *
     * @param context          the {@link #context() context} that will be used by this object;
     *                         may be <tt>null</tt>, then it will be ignored.
     * @param matrix           the bit matrix that should be processed and returned by {@link #result()} method.
     * @return                 new instance of this class.
     * @throws NullPointerException if <tt>matrix</tt> argument is <tt>null</tt>.
     */
    public static OctupleThinningSkeleton2D getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix)
    {
        return new OctupleThinningSkeleton2D(context, matrix, true, false);
    }

    @Override
    public long estimatedNumberOfIterations() {
        return ThinningTools.estimatedNumberOfIterations(result, topological);
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
     * <tt>directionIndex</tt> specifies the "eroded side" of objects,
     * or the direction of thinning:<ul>
     * <li>0 means removing elements from the left, i.e. from the side <nobr>(<i>x</i>&minus;1,<i>y</i>)</nobr>,</li>
     * <li>1 means "diagonal" removal from the side <nobr>(<i>x</i>&minus;1,<i>y</i>&minus;1)</nobr>,</li>
     * <li>2 means removal from the side <nobr>(<i>x</i>,<i>y</i>&minus;1)</nobr>,</li>
     * <li>3 means "diagonal" removal from the side <nobr>(<i>x</i>+1,<i>y</i>&minus;1)</nobr>,</li>
     * <li>4 means removal from the right, i.e. from the side <nobr>(<i>x</i>+1,<i>y</i>)</nobr>,</li>
     * <li>5 means "diagonal" removal from the side <nobr>(<i>x</i>+1,<i>y</i>+1)</nobr>,</li>
     * <li>6 means removal from the side <nobr>(<i>x</i>,<i>y</i>+1)</nobr>,</li>
     * <li>7 means "diagonal" removal from the side <nobr>(<i>x</i>&minus;1,<i>y</i>+1)</nobr>.</li>
     * </ul>
     *
     * <p>Though the algorithm is not documented, there are the following guarantees:
     * <ul>
     * <li>this algorithm never sets zero elements to unit: if the element of the current matrix
     * with some coordinates <nobr>(<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>)</nobr> is 0,
     * then the element with the same coordinates in the returned matrix is also 0;</li>
     *
     * <li>each element of the returned matrix with coordinates
     * <nobr>(<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>)</nobr>
     * depends only on the elements in 3x3 aperture
     * <nobr><i>x</i><sub>0</sub>&minus;1&le;<i>x</i>&le;<i>x</i><sub>0</sub>+1,
     * <i>y</i><sub>0</sub>&minus;1&le;<i>y</i>&le;<i>y</i><sub>0</sub>+1</nobr>
     * of the current matrix;</li>
     *
     * <li>if all elements of the current matrix in 3x3 aperture
     * <nobr><i>x</i><sub>0</sub>&minus;1&le;<i>x</i>&le;<i>x</i><sub>0</sub>+1,
     * <i>y</i><sub>0</sub>&minus;1&le;<i>y</i>&le;<i>y</i><sub>0</sub>+1</nobr>
     * are inside the matrix (i.e. <nobr>1&le;<i>x</i><sub>0</sub>&le;<tt>dimX</tt>&minus;2</nobr>,
     * <nobr>1&le;<i>y</i><sub>0</sub>&le;<tt>dimY</tt>&minus;2</nobr>,
     * <tt>dimX</tt> and <tt>dimY</tt> are dimensions of the matrix)
     * and all they are equal to 1, then the element <nobr>(<i>x</i><sub>0</sub>, <i>y</i><sub>0</sub>)</nobr>
     * in the returned matrix will be equal to 1.</li>
     * </ul>
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
            return asQuadrupleThinning(result, directionIndex / 2, topological);
        } else {
            return asDiagonalThinning(result, directionIndex / 2, topological);
        }
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "octuple thinning 2D skeletonizer, "
            + (diagonalThinning ? "8 steps" : "4 steps")
            + (topological ? ", topological mode" : "");
    }

    private static Matrix<BitArray> asQuadrupleThinning(Matrix<? extends BitArray> matrix,
        int directionIndex, boolean topological)
    {
        int[][] points = QUADRUPLE_SKELETON[directionIndex];
        // Comments below suppose the case SKELETON_XP (directionIndex = 0)
        // We consider the 3x3 aperture of bit (0 or 1) elements:
        //    A1 B1 C1
        //    A2  1 C2
        //    A3 B3 C3
        // The central element can be supposed to be 1, because the last operation of the method is "and"
        // (if the central element is 0, all other calculations have no effect).
        // We need to clear the central element to 0 in some "thinning" situation.

        Matrix<BitArray> internal = shift(matrix, points[0]); // -A2
        Matrix<BitArray> internalOrThin = or(internal, not(shift(matrix, points[1]))); // -C2
        // internalOrThin is 0 if ~A2 & C2 (it is a left boundary and not both A2 and C2 are 0): candidate for removal
        // If internalOrThin is 1, the central element must never be cleared.
        // From this moment, we may suppose that it is 0, i.e. A2=0 and C2=1:
        // if not, other calculations have no effect due to the last "or" operation.

        Matrix<BitArray> topArticulation =  andNot(shift(matrix, points[2]), shift(matrix, points[3]));
        // points[2] = -A1, points[3] = -B1
        // topArticulation = 1 if A1 & ~B1: a local articulation point
        Matrix<BitArray> bottomArticulation =  andNot(shift(matrix, points[4]), shift(matrix, points[5]));
        // points[4] = -A3, points[5] = -B3
        // bottomArticulation = 1 if A3 & ~B3: a local articulation point

        Matrix<BitArray> notRemoved;
        if (topological) {
            notRemoved = or(internalOrThin, topArticulation, bottomArticulation);
        } else {
            Matrix<BitArray> dilation4Points = or(shifts(matrix, points[6])); // points[6] = {-B1,-B3,-C1,-C3}
            // dilation4Points is 0 if B1=B3=C1=C3=0: we have a "free end" of a horizontal line
            notRemoved = or(internalOrThin, topArticulation, bottomArticulation, not(dilation4Points));
        }
        return and(matrix, notRemoved);
    }

    static Matrix<BitArray> asDiagonalThinning(Matrix<? extends BitArray> matrix,
        int directionIndex, boolean topological)
    {
        int[][] points = DIAGONAL_SKELETON[directionIndex];
        // Comments below suppose the case SKELETON_XPYP (directionIndex = 0)
        // We consider the 3x3 aperture of bit (0 or 1) elements:
        //    A1 B1 C1
        //    A2  1 C2
        //    A3 B3 C3
        // The central element can be supposed to be 1, because the last operation of the method is "and"
        // (if the central element is 0, all other calculations have no effect).
        // We need to clear the central element to 0 in some "thinning" situation.

        Matrix<BitArray> dilationInvCorner3Points = or(shifts(matrix, points[0])); // points[0] = {-A2,-A1,-B1}
        // dilationInvCorner3Points is 0 if all corner A2,A1,B1 is 0: we are at the left-top boundary.
        // From this moment, we may suppose that it is true (A2=A1=B1=0)
        // if not (dilationY3Points=1), other calculations of weakErosion
        // have no effect due to the "or" operation.

        Matrix<BitArray> erosionCorner3Points = and(shifts(matrix, points[1])); // points[1] = {-B3,-C2}
        // erosionCorner3Points is 1 if there is a corner 2x2 (0,0;1,0;0,1) filled by 1 and containing the center.
        // Because we already know that the center is 1 and A2=B1=0, then erosionCorner3Points=B3&C2.
        // We allow to clear the center only if this opening =1 (we are not on a thin diagonal line A3..C1).

        Matrix<BitArray> weakErosion = or(dilationInvCorner3Points, not(erosionCorner3Points));
        // Removing the center, if dilationInvCorner3Points=0 (A2=A1=B1=0) and erosionCorner3Points=1 (B3=C2=1).

        if (topological) {
            Matrix<BitArray> dilation5Points = or(shifts(matrix, points[2]));
            // points[2] = {-A3,-A2,-A1,-B1,-C1}
            return and(matrix, weakErosion, dilation5Points);
            // In addition, removing the center if these 5 points are 0: a "free end" of the diagonal line A1..C3.
        } else {
            return and(matrix, weakErosion);
        }
    }
}
