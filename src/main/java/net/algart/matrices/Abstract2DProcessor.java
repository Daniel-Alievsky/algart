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

package net.algart.matrices;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.TooLargeArrayException;

import java.util.Objects;

public abstract class Abstract2DProcessor {
    private final Class<?> elementType;
    private final int dimX;
    private final long dimY;
    private final long matrixSize;

    protected Abstract2DProcessor(Class<?> elementType, long[] dimensions) {
        this(elementType, dimensions, Integer.MAX_VALUE - 16);
        // - little gap helps to avoid overflow while simple operations
    }

    protected Abstract2DProcessor(Class<?> elementType, long[] dimensions, int maxDimX) {
        this.elementType = Objects.requireNonNull(elementType, "Null elementType");
        Objects.requireNonNull(dimensions, "Null dimensions");
        if (maxDimX <= 0) {
            throw new IllegalArgumentException("Zero or negative maxDimX = " + maxDimX);
        }
        if (dimensions.length != 2) {
            throw new IllegalArgumentException(getClass() + " can be used for 2-dimensional matrices only");
        }
        long dimX = dimensions[0];
        long dimY = dimensions[1];
        if (dimX <= 0 || dimY <= 0) {
            throw new IllegalArgumentException("Zero or negative matrix dimensions: " + dimX + "x" + dimY);
        }
        if (dimX > maxDimX) {
            throw new TooLargeArrayException("Matrix width must be < "
                    + maxDimX + ", but we have " + dimX + "x" + dimY);
        }
        this.dimX = (int) dimX;
        this.dimY = dimY;
        this.matrixSize = Arrays.longMul(dimX, dimY);
        if (matrixSize == Long.MIN_VALUE) {
            throw new TooLargeArrayException("Matrix size " + dimX + "x" + dimY + " is too large: >2^63-1");
        }
    }

    public Class<?> elementType() {
        return elementType;
    }

    public int dimX() {
        return dimX;
    }

    public long dimY() {
        return dimY;
    }

    public long matrixSize() {
        return matrixSize;
    }

    public void checkCompatibility(Matrix<? extends PArray> matrix) {
        Objects.requireNonNull(matrix, "Null matrix");
        if (matrix.elementType() != elementType) {
            throw new IllegalArgumentException("Element type of " + matrix
                    + " does not match the element type of this finder: " + elementType);
        }
        if (matrix.dimCount() != 2) {
            throw new IllegalArgumentException(getClass() + " can be used for 2-dimensional matrices only, " +
                    "but we have " + matrix);
        }
        if (matrix.dimX() != dimX || matrix.dimY() != dimY) {
            throw new IllegalArgumentException("New matrix has other dimensions than the stored dimensions: "
                    + "stored are " + dimY + "x" + dimY + ", new is " + matrix);
        }
    }

    public long previousLineOffset(long offset) {
        offset -= dimX;
        return offset < 0 ? matrixSize - dimX : offset;
    }

    public long nextLineOffset(long offset) {
        offset += dimX;
        return offset >= matrixSize ? 0 : offset;
    }

    public static long rem(long index, long dim) {
        index %= dim;
        return index >= 0 ? index : index + dim;
    }

    public static int rem(int index, int dim) {
        index %= dim;
        return index >= 0 ? index : index + dim;
    }

}
