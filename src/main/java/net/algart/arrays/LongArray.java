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

/*Repeat(INCLUDE_FROM_FILE, FloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Long ;;
  float ==> long
     !! Auto-generated: NOT EDIT !! */

import java.util.Objects;

/**
 * <p>AlgART array of <code>long</code> values, read-only access.</p>
 *
 * @author Daniel Alievsky
 */
public interface LongArray extends PIntegerArray {
    Class<? extends LongArray> type();

    Class<? extends UpdatableLongArray> updatableType();

    Class<? extends MutableLongArray> mutableType();

    /**
     * Returns the element #<code>index</code>.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    long getLong(long index);

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</code>
     * and <code>{@link #getLong(long) getLong}(k)==value</code>,
     * or <code>-1</code> if there is no such array element.
     *
     * <p>In particular, if <code>lowIndex&gt;=thisArray.{@link #length() length()}}</code>
     * or <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * <p>You may specify <code>lowIndex=0</code> and <code>highIndex=Long.MAX_VALUE</code> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, long value);

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>{@link #getLong(long) getLong}(k)==value</code>,
     * or <code>-1</code> if there is no such array element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=thisArray.{@link #length() length()}</code>,
     * the result is the same as if <code>highIndex==thisArray.{@link #length() length()}</code>.
     *
     * <p>You may specify <code>lowIndex=0</code> and <code>highIndex=Long.MAX_VALUE</code> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, long value);

    DataLongBuffer buffer(DataBuffer.AccessMode mode, long capacity);

    DataLongBuffer buffer(DataBuffer.AccessMode mode);

    DataLongBuffer buffer(long capacity);

    DataLongBuffer buffer();

    LongArray asImmutable();

    LongArray asTrustedImmutable();

    MutableLongArray mutableClone(MemoryModel memoryModel);

    UpdatableLongArray updatableClone(MemoryModel memoryModel);

    long[] ja();

    default Matrix<? extends LongArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }

    @Override
    default long[] jaLong() {
        return ja();
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newUnresizableLongArray(long)
     * memoryModel.newUnresizableLongArray(length)}</code>.
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <code>memoryModel</code> argument is {@code null}.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <code>long</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     */
    static UpdatableLongArray newArray(MemoryModel memoryModel, long length) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newUnresizableLongArray(length);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newUnresizableLongArray(long)
     * newUnresizableLongArray(length)}</code>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws IllegalArgumentException if the specified length is negative.
     * @throws TooLargeArrayException   if the specified length is too large for {@link SimpleMemoryModel}.
     */
    static UpdatableLongArray newArray(long length) {
        return Arrays.SMM.newUnresizableLongArray(length);
    }
    /*Repeat.IncludeEnd*/

    /**
     * Returns the element #<code>index</code>,
     * truncated to the range <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code>:
     * <code>min(max(value, Integer.MIN_VALUE), Integer.MAX_VALUE)</code>.
     *
     * <p>Note that this method is already declared in {@link PFixedArray}.
     * It is redeclared here only for documentation and code search purposes.</p>
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     */
    int getInt(long index);

    /**
     * Equivalent to <code>{@link SimpleMemoryModel#asUpdatableLongArray(long[])
     * SimpleMemoryModel.asUpdatableLongArray}(array)</code>.
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    static UpdatableLongArray as(long[] array) {
        return SimpleMemoryModel.asUpdatableLongArray(array);
    }
}
