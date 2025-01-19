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

    /**
     * Equivalent to <code>(UpdatablePNumberArray) {@link MemoryModel#newUnresizableArray(Class, long)
     * memoryModel.newUnresizableArray(elementType, length)}</code>, but with throwing
     * <code>IllegalArgumentException</code> in a case when the type casting to {@link UpdatablePNumberArray}
     * is impossible (non-primitive element type, <code>boolean</code> or <code>char</code>).
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @param elementType the type of array elements.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if one of the arguments is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not  <code>byte.class</code>,
     *                                         <code>short.class</code>, <code>int.class</code>,
     *                                         <code>long.class</code>,
     *                                         <code>float.class</code> or <code>double.class</code>,
     *                                         or if the specified length is negative.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     */
    static UpdatablePNumberArray newArray(MemoryModel memoryModel, Class<?> elementType, long length) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(elementType, "Null element type");
        if (!Arrays.isNumberElementType(elementType)) {
            throw new IllegalArgumentException("Not a numeric primitive type: " + elementType);
        }
        return (UpdatablePNumberArray) memoryModel.newUnresizableArray(elementType, length);
    }

    /**
     * Equivalent to <code>{@link #newArray(MemoryModel, Class, long)
     * newArray}({@link Arrays#SMM Arrays.SMM}, elementType, length)</code>.
     *
     * @param elementType the type of array elements.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>elementType</code> is not  <code>byte.class</code>,
     *                                  <code>short.class</code>, <code>int.class</code>,
     *                                  <code>long.class</code>,
     *                                  <code>float.class</code> or <code>double.class</code>,
     *                                  or if the specified length is negative.
     * @throws TooLargeArrayException   if the specified length is too large for {@link SimpleMemoryModel}.
     */
    static UpdatablePNumberArray newArray(Class<?> elementType, long length) {
        return newArray(Arrays.SMM, elementType, length);
    }

    /**
     * Equivalent to <code>{@link SimpleMemoryModel#asUpdatablePNumberArray(Object)
     * SimpleMemoryModel.asUpdatablePNumberArray}(array)</code>.
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not an array,
     *                                  or <code>boolean[]</code> array,
     *                                  or <code>char[]</code> array, or <code>Objects[]</code> array.
     */
    static UpdatablePNumberArray as(Object array) {
        return SimpleMemoryModel.asUpdatablePNumberArray(array);
    }
}
