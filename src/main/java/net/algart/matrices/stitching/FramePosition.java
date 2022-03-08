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
import net.algart.math.RectangularArea;
import net.algart.math.functions.Func;


public interface FramePosition {
    public RectangularArea area();

    public Func asInterpolationFunc(Matrix<? extends PArray> sourceMatrix);

    /**
     * Returns the hash code of this object. The result depends on all parameters, specifying
     * this frame position.
     *
     * @return the hash code of this frame position.
     */
    public int hashCode();

    /**
     * Indicates whether some other object is also {@link FramePosition},
     * specifying the same position as this one.
     *
     * <p>There is high probability, but no guarantee that this method returns <tt>true</tt> if the passed object
     * specifies a frame position, identical to this one.
     * There is a guarantee that this method returns <tt>false</tt>
     * if the passed object specifies a frame position, different than this one.
     *
     * @param obj the object to be compared for equality with this frame position.
     * @return    <tt>true</tt> if the specified object is a frame position equal to this one.
     */
    public boolean equals(Object obj);
}
