/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.*;

public class DefaultFrame<P extends FramePosition> implements Frame<P> {

    private final Matrix<? extends PArray> matrix;
    private final P position;

    protected DefaultFrame(Matrix<? extends PArray> matrix, P position) {
        if (matrix == null)
            throw new NullPointerException("Null matrix argument");
        if (position == null)
            throw new NullPointerException("Null position argument");
        if (matrix.dimCount() != position.area().coordCount())
            throw new IllegalArgumentException("Different number of dimensions in passed matrix and position: "
              + "position coordinates are " + position.area() + ", matrix is " + matrix);
        this.matrix = matrix;
        this.position = position;
    }

    public static <P extends FramePosition> DefaultFrame<P> valueOf(Matrix<? extends PArray> matrix, P position) {
        return new DefaultFrame<P>(matrix, position);
    }

    public int dimCount() {
        return matrix().dimCount();
    }

    public Matrix<? extends PArray> matrix() {
        return matrix;
    }

    public P position() {
        return position;
    }

    public void freeResources() {
        matrix.freeResources();
    }

    @Override
    public String toString() {
        return "frame " + matrix() + " at " + position();
    }

    public int hashCode() {
        return matrix().hashCode() * 37 + position().hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Frame<?>
            && matrix().equals(((Frame<?>)obj).matrix()) && position().equals(((Frame<?>)obj).position()));
    }

}