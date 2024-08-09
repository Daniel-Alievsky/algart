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

package net.algart.arrays;

import java.util.Objects;

class MatrixImpl<T extends Array> extends AbstractMatrix<T> implements Matrix<T> {
    private final T array;
    private final long[] dimensions;

    MatrixImpl(T array, long[] dimensions) {
        Objects.requireNonNull(array, "Null array argument");
        if (!array.isUnresizable()) {
            throw new IllegalArgumentException("Matrix cannot be created on the base of resizable array: "
                    + "please use UpdatableArray.asUnresizable() method before constructing a matrix");
        }
        this.array = array;
        this.dimensions = dimensions.clone();
        checkDimensions(this.dimensions, array.length());
    }

    @Override
    public T array() {
        return this.array;
    }

    @Override
    public Object toJavaArray() {
        return this.array.toJavaArray();
    }

    @Override
    public Object ja() {
        return this.array.ja();
    }

    @Override
    public long size() {
        return this.array.length();
    }

    @Override
    public int size32() {
        return this.array.length32();
    }

    @Override
    public long[] dimensions() {
        return this.dimensions.clone();
    }

    @Override
    public int dimCount() {
        return this.dimensions.length;
    }

    @Override
    public long dim(int n) {
        return n < this.dimensions.length ? this.dimensions[n] : 1;
    }

    @Override
    public <U extends Array> Matrix<U> matrix(U anotherArray) {
        return new MatrixImpl<>(anotherArray, dimensions);
    }
}
