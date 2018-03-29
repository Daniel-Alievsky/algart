/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Weaker version of {@link OctupleThinningSkeleton2D} skeletonization algorithm.</p>
 *
 * <p>This class implements almost the same algorithm as {@link OctupleThinningSkeleton2D},
 * but the thinning methods are not so aggressive.
 * This class also guarantees that 8-connected "objects"
 * (areas filled by 1 elements) always stay 8-connected:
 * see {@link ThinningSkeleton} interface about the precise sense of this state.</p>
 *
 * <p>The lines of the skeleton can be more even, than in {@link OctupleThinningSkeleton2D},
 * but some additional "bad" cases will not be thinned until 1-pixel thickness.
 * For example:</p>
 *
 * <pre>
 * . . . . . 1 . . .      . . . . . 1 . . .
 * . . . . . 1 . . .      . . . . . 1 . . .
 * . . . . . 1 . . .      . . . . . 1 . . .
 * . 1 1 1 1 1 . . .      . 1 1 1 1 1 1 1 .
 * . . . . 1 1 1 1 1      . . . . 1 . . . .
 * . . . . 1 . . . .      . . . . 1 . . . .
 * . . . . 1 . . . .      . . . . 1 . . . .
 * . . . . 1 . . . .      . . . . . . . . .
 * </pre>
 *
 * <p>These objects cannot be thinned by this algorithm.
 * Unlike this, {@link OctupleThinningSkeleton2D} transforms them
 * to the following (or similar) skeletons:</p>
 *
 * <pre>
 * . . . . . 1 . . .      . . . . . 1 . . .
 * . . . . . 1 . . .      . . . . . 1 . . .
 * . . . . . 1 . . .      . . . . . 1 . . .
 * . 1 1 1 1 . . . .      . 1 1 1 . 1 1 1 .
 * . . . . . 1 1 1 1      . . . . 1 . . . .
 * . . . . 1 . . . .      . . . . 1 . . . .
 * . . . . 1 . . . .      . . . . 1 . . . .
 * . . . . 1 . . . .      . . . . . . . . .
 * </pre>

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
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class WeakOctupleThinningSkeleton2D extends AbstractThinningSkeleton2D implements ThinningSkeleton {
    private static final int[][] SKELETON_XP = {
        {1, -1, 1, 0, 1, 1},          // 0: -A1,-A2,-A3
        {-1, 0},                      // 1: -C2
        {0, 1, 0, -1, -1, 1, -1, -1}, // 2: -B1,-B3,-C1,-C3
    };
    private static final int[][] SKELETON_YP = rotate90(SKELETON_XP);
    private static final int[][] SKELETON_XM = rotate180(SKELETON_XP);
    private static final int[][] SKELETON_YM = rotate270(SKELETON_XP);
    private static final int[][][] STRAIGHT_SKELETON = {
        SKELETON_XP, SKELETON_YP, SKELETON_XM, SKELETON_YM
    };
    private final boolean topological;

    private WeakOctupleThinningSkeleton2D(ArrayContext arrayContext, Matrix<? extends UpdatableBitArray> matrix,
        boolean straightThinning, boolean diagonalThinning, boolean topological)
    {
        super(arrayContext, matrix, straightThinning, diagonalThinning);
        this.topological = topological;
    }

    /**
     * Creates new instance of this class.
     *
     * <p>If the <tt>straightThinning</tt> or <tt>diagonalThinning</tt> argument is <tt>false</tt>,
     * the algorithm will skip thinning, correspondingly, along the axes or along diagonal directions.
     * Note that the results will usually be <i>incorrect</i>: large object will stay not skeletonized.
     * (Compare with <tt>diagonalThinning</tt> argument in
     * {@link OctupleThinningSkeleton2D#getInstance(ArrayContext, Matrix, boolean, boolean)}.)
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
     * @param straightThinning whether the algorithm will perform thinning along x- and y-axes; usually <tt>true</tt>.
     * @param diagonalThinning whether the algorithm will perform diagonal thinning; usually <tt>true</tt>.
     * @param topological      whether the algorithm will shorten isolated thin lines with "free ends".
     * @return                 new instance of this class.
     * @throws NullPointerException if <tt>matrix</tt> argument is <tt>null</tt>.
     */
    public static WeakOctupleThinningSkeleton2D getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix,
        boolean straightThinning, boolean diagonalThinning, boolean topological)
    {
        return new WeakOctupleThinningSkeleton2D(context, matrix, straightThinning, diagonalThinning, topological);
    }

    /**
     * Creates new instance of this class.
     * Equivalent to {@link #getInstance(ArrayContext, Matrix, boolean, boolean, boolean)
     * getInstance(context, matrix, true, true, false)}.
     *
     * @param context          the {@link #context() context} that will be used by this object;
     *                         may be <tt>null</tt>, then it will be ignored.
     * @param matrix           the bit matrix that should be processed and returned by {@link #result()} method.
     * @return                 new instance of this class.
     * @throws NullPointerException if <tt>matrix</tt> argument is <tt>null</tt>.
     */
    public static WeakOctupleThinningSkeleton2D getInstance(ArrayContext context,
        Matrix<? extends UpdatableBitArray> matrix)
    {
        return new WeakOctupleThinningSkeleton2D(context, matrix, true, true, false);
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
            return asStraightThinning(result, directionIndex / 2, topological);
        } else {
            return OctupleThinningSkeleton2D.asDiagonalThinning(result, directionIndex / 2, topological);
        }
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "weak octuple thinning 2D skeletonizer, "
            + (diagonalThinning ? "8 steps" : "4 steps")
            + (topological ? ", topological mode" : "");
    }

    private Matrix<BitArray> asStraightThinning(Matrix<? extends BitArray> result,
        int directionIndex, boolean topological)
    {
        int[][] points = STRAIGHT_SKELETON[directionIndex];
        // Comments below suppose the case SKELETON_XP (directionIndex = 0)
        // We consider the 3x3 aperture of bit (0 or 1) elements:
        //    A1 B1 C1
        //    A2  1 C2
        //    A3 B3 C3
        // The central element can be supposed to be 1, because the last operation of the method is "and"
        // (if the central element is 0, all other calculations have no effect).
        // We need to clear the central element to 0 in some "thinning" situation.

        Matrix<BitArray> dilationY3Points = or(shifts(result, points[0])); // points[0] = {-A1,-A2,-A3}
        // dilationY3Points is 0 if all A column is 0: we are at the left boundary.
        // From this moment, we may suppose that it is true, i.e. A1=A2=A3=0:
        // if not (dilationY3Points=1), other calculations have no effect due to the last "or" operation.
        // Please note: this iteration does nothing at diagonal sides!

        Matrix<BitArray> openingX = shift(result, points[1]); // -C2
        // openingX is 1 if there is a segment 2x1 filled by 1 and containing the center.
        // Because we already know that the center is 1 and A2=0, then just openingX=C2.
        // We allow to clear the center only if openingX=1 (we are not on a thin vertical line).

        if (!topological) {
            Matrix<BitArray> dilation4Points = or(shifts(result, points[2])); // points[2] = {-B1,-B3,-C1,-C3}
            openingX = and(openingX, dilation4Points);
            // dilation4Points is 0 if B1=B3=C1=C3=0 (and, so, the center is removed from openingX,
            // and clearing the result will not be allowed).
            // As all A column is zero, B1=B3=C1=C3=0 means that we have a "free end" of a horizontal line,
            // that should not be removed while non-topological skeletonization.
        }

        Matrix<BitArray> weakErosion = or(dilationY3Points, not(openingX));
        // Removing the center, if dilationY3Points=0 (A1=A2=A3=0) and openingX=1
        return and(result, weakErosion);
    }
}
