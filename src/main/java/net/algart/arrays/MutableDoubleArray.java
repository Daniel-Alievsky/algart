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

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  Float(?!ing) ==> Double ;;
  float ==> double
     !! Auto-generated: NOT EDIT !! */

import java.util.Objects;

/**
 * <p>Resizable AlgART array of <code>double</code> values.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutableDoubleArray extends DoubleStack, UpdatableDoubleArray, MutablePFloatingArray {
    MutableDoubleArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutableDoubleArray setData(long arrayPos, Object srcArray);

    MutableDoubleArray copy(Array src);

    MutableDoubleArray swap(UpdatableArray another);

    MutableDoubleArray length(long newLength);

    MutableDoubleArray ensureCapacity(long minCapacity);

    MutableDoubleArray trim();

    MutableDoubleArray append(Array appendedArray);

    MutableDoubleArray asCopyOnNextWrite();

    MutableDoubleArray shallowClone();
    /**
     * Equivalent to <code>{@link MemoryModel#newEmptyDoubleArray()
     * memoryModel.newEmptyDoubleArray()}</code>.
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @return created empty AlgART array.
     * @throws NullPointerException            if <code>memoryModel</code> argument is {@code null}.
     * @throws UnsupportedElementTypeException if <code>double</code> element type
     *                                         is not supported by this memory model.
     */
    static MutableDoubleArray newArray(MemoryModel memoryModel) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newEmptyDoubleArray();
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newEmptyDoubleArray()
     * newEmptyDoubleArray()}</code>.
     *
     * @return created empty AlgART array.
     */
    static MutableDoubleArray newArray() {
        return Arrays.SMM.newEmptyDoubleArray();
    }
    /*Repeat.IncludeEnd*/
}
