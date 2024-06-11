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

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PFixed ;;
  Float(?!ing) ==> Char ;;
  float ==> char
     !! Auto-generated: NOT EDIT !! */

import java.util.Objects;

/**
 * <p>Resizable AlgART array of <code>char</code> values.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutableCharArray extends CharStack, UpdatableCharArray, MutablePFixedArray {
    MutableCharArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutableCharArray setData(long arrayPos, Object srcArray);

    MutableCharArray copy(Array src);

    MutableCharArray swap(UpdatableArray another);

    MutableCharArray length(long newLength);

    MutableCharArray ensureCapacity(long minCapacity);

    MutableCharArray trim();

    MutableCharArray append(Array appendedArray);

    MutableCharArray asCopyOnNextWrite();

    MutableCharArray shallowClone();
    /**
     * Equivalent to <code>{@link MemoryModel#newEmptyCharArray()
     * memoryModel.newEmptyCharArray()}</code>.
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @return created empty AlgART array.
     * @throws NullPointerException            if <code>memoryModel</code>> argument is {@code null}.
     * @throws UnsupportedElementTypeException if <code>char</code> element type
     *                                         is not supported by this memory model.
     */
    static MutableCharArray newEmpty(MemoryModel memoryModel) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newEmptyCharArray();
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newEmptyCharArray()
     * newEmptyCharArray()}</code>.
     *
     * @return created empty AlgART array.
     */
    static MutableCharArray newEmpty() {
        return Arrays.SMM.newEmptyCharArray();
    }
    /*Repeat.IncludeEnd*/

    /**
     * Appends all characters of <tt>value</tt> to the end of this array and returns this array.
     * @param value a string to be appended to this array.
     * @return      a reference to this object.
     * @throws TooLargeArrayException if the resulting array length is too large for this type of arrays.
     */
    MutableCharArray append(String value);
}
