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

/**
 * <p>AlgART array of any primitive numeric elements (byte, short, int, long, float or double),
 * read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray},
 * {@link FloatArray}, {@link DoubleArray} subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface PNumberArray extends PArray {
    Class<? extends PNumberArray> type();

    Class<? extends UpdatablePNumberArray> updatableType();

    Class<? extends MutablePNumberArray> mutableType();

    default Matrix<? extends PNumberArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }

    static UpdatablePNumberArray newArray(MemoryModel memoryModel, Class<?> elementType, long length) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(elementType, "Null element type");
        if (!Arrays.isNumberElementType(elementType)) {
            throw new IllegalArgumentException("Not a numeric primitive type: " + elementType);
        }
        return (UpdatablePNumberArray) memoryModel.newUnresizableArray(elementType, length);
    }

    static UpdatablePNumberArray newArray(Class<?> elementType, long length) {
        return newArray(Arrays.SMM, elementType, length);
    }

}
