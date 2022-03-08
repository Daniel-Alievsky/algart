/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.stitching;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

/**
 * <p><i>Frame</i>: a pair of the {@link Matrix AlgART matrix} and its {@link FramePosition position};
 * in other words, an <i>n</i>-dimensional matrix, placed by some way in <i>n</i>-dimensional space.
 * The matrix position is represented by {@link FramePosition} interface and specifies, what area of
 * <i>n</i>-dimensional space corresponds to this matrix. In the simplest case, the position
 * is just coordinates of the starting element (0,0,...) of the matrix in the space.
 * See comments to {@link FramePosition} interface for more details.</p>
 *
 * <p>A set of frames is the basic data structure that is possible to be <i>stitched</i> by this package,
 * i.e. transformed to a united matrix, corresponding to any rectangular area in <i>n</i>-dimensional space,
 * probably containing all matrices, placed in the space at their positions.</p>
 *
 * <p>Implementations of this interface are usually <b>immutable</b> and
 * always <b>thread-safe</b>.
 * All implementations of this interface from this package are <b>immutable</b>.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public interface Frame<P extends FramePosition> {

    /**
     * The number of space dimensions.
     *
     * @return the number of space dimensions
     */
    public int dimCount();

    /**
     * The matrix, contained in this frame.
     * There is a guarantee that
     * <tt>thisInstance.{@link #matrix()}.{@link Matrix#dimCount() dimCount()}==thisInstance.{@link #dimCount()}</tt>.
     *
     * @return matrix contained in this frame.
     */
    public Matrix<? extends PArray> matrix();

    /**
     * The position in <i>n</i>-dimensional space, where the given {@link #matrix() matrix} is placed.
     * There is a guarantee that
     * <tt>thisInstance.{@link #position()}.{@link FramePosition#area()
     * area()}.{@link net.algart.math.RectangularArea#coordCount()
     * coordCount()}==thisInstance.{@link #dimCount()}</tt>.
     *
     * @return position in <i>n</i>-dimensional space, where the given {@link #matrix() matrix} is placed.
     */
    public P position();

    /**
     * Calls {@link Matrix#freeResources(net.algart.arrays.ArrayContext) Matrix.freeResources(null)} for the matrix,
     * contained in this frame.
     */
    public void freeResources();

    /**
     * Returns the hash code of this object. The result depends both on the {@link #matrix() matrix}
     * and the {@link #position() frame position}.
     *
     * @return the hash code of this frame.
     */
    public int hashCode();

    /**
     * Indicates whether some other object is also a {@link Frame},
     * containg the {@link #matrix() matrix} and {@link #position() position}, equal to the matrix and position
     * in this frame.
     *
     * <p>Note: this method should return <tt>true</tt> even the class of the passed frame
     * is different than this class. This method checks only the built-in matrices and positions.
     *
     * @param obj the object to be compared for equality with this frame.
     * @return    <tt>true</tt> if the specified object is a frame containing the equal matrix and position.
     */
    public boolean equals(Object obj);
}
