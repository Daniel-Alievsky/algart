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

package net.algart.matrices.scanning;

import net.algart.arrays.JArrays;
import net.algart.arrays.Matrix;

/**
 * <p>Connectivity kind of connected objects in the matrix.
 * Used by {@link ConnectedObjectScanner} and {@link Boundary2DScanner} classes.
 * See definition of this term in comments to {@link ConnectedObjectScanner}.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify the settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public enum ConnectivityType {
    /**
     * <i>Straight</i> connectivity.
     * Two unit elements with coordinates
     * <i>i</i><sub>0</sub>, <i>i</i><sub>1</sub>, ..., <i>i</i><sub><i>n</i>-1</sub> and
     * <i>j</i><sub>0</sub>, <i>j</i><sub>1</sub>, ..., <i>j</i><sub><i>n</i>-1</sub>
     * are <i>neighbours</i> and, so, connected with each other
     * if one from the coordinates differs by 1, but all other coordinates are equal:
     *
     * <blockquote><span style="font-size:200%">&sum;</span>
     * |<i>i</i><sub><i>k</i></sub>&minus;<i>j</i><sub><i>k</i></sub>|=1
     * </blockquote>
     *
     * <p>For 2D matrices, this connectivity kind is also known as "4-connectivity".
     */
    STRAIGHT_ONLY() {
        byte[][] apertureShifts(int dimCount) {
            assert dimCount < STRAIGHT_SHIFTS.length;
            return STRAIGHT_SHIFTS[dimCount];
        }
    },

    /**
     * <i>Straight-and-diagonal</i> connectivity.
     * Two unit elements with coordinates
     * <i>i</i><sub>0</sub>, <i>i</i><sub>1</sub>, ..., <i>i</i><sub><i>n</i>-1</sub> and
     * <i>j</i><sub>0</sub>, <i>j</i><sub>1</sub>, ..., <i>j</i><sub><i>n</i>-1</sub>
     * are <i>neighbours</i> and, so, connected with each other
     * if several (at least one) from their coordinates differ by 1 and all other coordinates are equal:
     *
     * <blockquote>max&nbsp;(|<i>i</i><sub><i>k</i></sub>&minus;<i>j</i><sub><i>k</i></sub>|)=1</blockquote>
     *
     * <p>For 2D matrices, this connectivity kind is also known as "8-connectivity".
     */
    STRAIGHT_AND_DIAGONAL() {
        byte[][] apertureShifts(int dimCount) {
            assert dimCount < STRAIGHT_AND_DIAGONAL_SHIFTS.length;
            return STRAIGHT_AND_DIAGONAL_SHIFTS[dimCount];
        }
    };

    /**
     * Returns the number of neighbours of any matrix element:
     * <code>2*dimCount</code> for {@link #STRAIGHT_ONLY straight connectivity},
     * <code>3<sup>dimCount</sup>-1</code> for {@link #STRAIGHT_AND_DIAGONAL straight-and-diagonal connectivity}.
     *
     * @param dimCount the number of dimensions of the matrix.
     * @return         the number of neighbours of any matrix element.
     * @throws IllegalArgumentException if <code>dimCount&lt;=0</code> or
     *                                  <code>dimCount&gt;{@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</code>.
     */
    public int numberOfNeighbours(int dimCount) {
        if (dimCount <= 0 || dimCount > Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS)
            throw new IllegalArgumentException("dimCount = " + dimCount
                + " is not in range 1.." + Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS);
        return apertureShifts(dimCount).length;
    }

    abstract byte[][] apertureShifts(int dimCount);

    private static final byte[][][] STRAIGHT_SHIFTS =
        new byte[Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1][][];
    private static final byte[][][] STRAIGHT_AND_DIAGONAL_SHIFTS =
        new byte[Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1][][];
    static {
        for (int dimCount = 1; dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS; dimCount++) {
            STRAIGHT_SHIFTS[dimCount] = generateStraightShifts(dimCount);
            STRAIGHT_AND_DIAGONAL_SHIFTS[dimCount] = generateStraightAndDiagonalShifts(dimCount);
        }
        // This loop requires ~5-10 ms in 1 thread on 2.4 GHz without any hot-spot optimization.
        // Loading this class from disk usually requires more time.
    }

    private static byte[][] generateStraightShifts(int dimCount) {
        byte[][] shifts = new byte[2 * dimCount][dimCount]; // zero-filled
        for (int k = 0, i = 0; k < dimCount; k++) {
            shifts[i++][k] = 1;
            shifts[i++][k] = -1;
        } // 2D-case: right, left, up, down, center
        return shifts;
    }

    private static byte[][] generateStraightAndDiagonalShifts(int dimCount) {
        int totalCount = 1;
        for (int k = 0; k < dimCount; k++) {
            totalCount *= 3;
        } // 3^dimCount
        byte[][] shifts = new byte[totalCount - 1][dimCount]; // zero-filled
        byte[] coordinates = new byte[dimCount];
        JArrays.fill(coordinates, (byte)-1);
        for (int index = 0, shiftsIndex = 0; ; ) {
            boolean origin = true;
            for (int j = 0; j < dimCount; j++) {
                if (coordinates[j] != 0) {
                    origin = false; break;
                }
            }
            if (!origin) {
                System.arraycopy(coordinates, 0, shifts[shiftsIndex], 0, dimCount);
                shiftsIndex++;
            }
            if (++index >= totalCount) {
                break; // check here, not in "for" statement: it allows to skip checking j>=0 in the loop below
            }
            // Below we increment by 1 the "number" with the radix 3 and digits "-1", "0", "1" stored in "coordinates"
            int j = dimCount - 1;
            while (coordinates[j] == 1) {
                // not check that j>=0: the "1,1,1,...,1" case is possible in the last
                // iteration only, but in this case we have already exited the main loop
                coordinates[j] = -1;
                j--;
            }
            coordinates[j]++;
        }
        return shifts;
    }
}
