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
 * <p>Resizable AlgART array of any numeric primitive elements (byte, short, int, long, float or double).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray},
 * {@link MutableFloatArray}, {@link MutableDoubleArray},
 * subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutablePNumberArray extends UpdatablePNumberArray, MutablePArray {
    /**
     * Equivalent to <code>(MutablePNumberArray) to {@link MemoryModel#newEmptyArray(Class)
     * memoryModel.newEmptyArray(elementType)}</code>, but with throwing
     * <code>IllegalArgumentException</code> in a case when the type casting to {@link MutablePNumberArray}
     * is impossible (non-primitive element type, <code>boolean</code> or <code>char</code>).
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @param elementType the type of array elements.
     * @return created empty AlgART array.
     * @throws NullPointerException            if one of the arguments is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not  <code>byte.class</code>,
     *                                         <code>short.class</code>, <code>int.class</code>,
     *                                         <code>long.class</code>,
     *                                         <code>float.class</code> or <code>double.class</code>.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is not supported by this memory model.
     */
    static MutablePNumberArray newArray(MemoryModel memoryModel, Class<?> elementType) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(elementType, "Null element type");
        if (!Arrays.isNumberElementType(elementType)) {
            throw new IllegalArgumentException("Not a numeric primitive type: " + elementType);
        }
        return (MutablePNumberArray) memoryModel.newEmptyArray(elementType);
    }

    /**
     * Equivalent to <code>{@link #newArray(MemoryModel, Class)
     * newArray}({@link Arrays#SMM Arrays.SMM}, elementType)</code>.
     *
     * @param elementType the type of array elements.
     * @return created empty AlgART array.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not  <code>byte.class</code>,
     *                                         <code>short.class</code>, <code>int.class</code>,
     *                                         <code>long.class</code>,
     *                                         <code>float.class</code> or <code>double.class</code>.
     */
    static MutablePNumberArray newArray(Class<?> elementType) {
        return newArray(Arrays.SMM, elementType);
    }
}
