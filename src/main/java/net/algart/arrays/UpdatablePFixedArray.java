/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>AlgART array of any fixed-point primitive numeric, character or bit elements
 * (byte, short, int, long, char or boolean),
 * read/write access, no resizing.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link UpdatableBitArray}, {@link UpdatableCharArray},
 * {@link UpdatableByteArray}, {@link UpdatableShortArray},
 * {@link UpdatableIntArray}, {@link UpdatableLongArray}
 * subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface UpdatablePFixedArray extends PFixedArray, UpdatablePArray {
    UpdatablePFixedArray subArray(long fromIndex, long toIndex);

    UpdatablePFixedArray subArr(long position, long count);

    UpdatablePFixedArray asUnresizable();

    default Matrix<? extends UpdatablePFixedArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
}
