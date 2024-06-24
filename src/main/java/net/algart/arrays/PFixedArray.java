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

/**
 * <p>AlgART array of any fixed-point primitive numeric, character or bit elements
 * (byte, short, int, long, char or boolean), read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link BitArray}, {@link CharArray},
 * {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray} subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface PFixedArray extends PArray {
    /**
     * Returns 0 for {@link BitArray}, {@link ByteArray}, {@link CharArray} and {@link ShortArray},
     * <code>Integer.MIN_VALUE</code> for {@link IntArray},
     * <code>Long.MIN_VALUE</code> for {@link LongArray}.
     * It is the minimal possible value,
     * that can stored in elements of this array
     * (<code>byte</code> and <code>short</code> elements are interpreted as unsigned).
     * This method is equivalent to
     * <code>{@link Arrays#minPossibleIntegerValue(Class) minPossibleIntegerValue}(thisArray.getClass())</code>.
     *
     * @return the minimal possible value, that can stored in elements of this array.
     * @see PArray#minPossibleValue(double)
     */
    long minPossibleValue();

    /**
     * Returns 1 for {@link BitArray},
     * 0xFF for {@link ByteArray},
     * 0xFFFF for {@link CharArray} and {@link ShortArray},
     * <code>Integer.MAX_VALUE</code> for {@link IntArray},
     * <code>Long.MAX_VALUE</code> for {@link LongArray}.
     * <code>valueForFloatingPoint</code> for {@link FloatArray} and {@link DoubleArray}.
     * It is the maximal possible value,
     * that can stored in elements of this array
     * (<code>byte</code> and <code>short</code> elements are interpreted as unsigned).
     * This method is equivalent to
     * <code>{@link Arrays#maxPossibleIntegerValue(Class) maxPossibleIntegerValue}(thisArray.getClass())</code>.
     *
     * @return the maximal possible value, that can stored in elements of this array.
     * @see PArray#maxPossibleValue(double)
     */
    long maxPossibleValue();

    /**
     * Returns the element #<code>index</code> converted to <code>long</code>:
     * <code>(long)value&amp;0xFF</code> for <code>byte</code> value,
     * <code>(long)value&amp;0xFFFF</code> for <code>short</code> value,
     * <code>(long)value</code> for <code>int</code>, <code>long</code> or <code>char</code> values,
     * or as <code>value?1:0</code> for <code>boolean</code> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * if it is not an array of <code>float</code> or <code>double</code> elements.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     * @see UpdatablePArray#setLong(long, long)
     */
    long getLong(long index);

    /**
     * Returns the element #<code>index</code> converted to <code>int</code>:
     * <code>(int)value&amp;0xFF</code> for <code>byte</code> value,
     * <code>(int)value&amp;0xFFFF</code> for <code>short</code> value,
     * <code>(int)value</code> for <code>int</code> or <code>char</code> values,
     * <code>value?1:0</code> for <code>boolean</code> values,
     * <code>min(max(value, Integer.MIN_VALUE), Integer.MAX_VALUE)</code> (i&#46;e&#46; the value
     * truncated to the range <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code>)
     * for <code>long</code> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * if it is not an array of <code>long</code>, <code>float</code> or <code>double</code> elements.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     * @see UpdatablePArray#setInt(long, int)
     */
    int getInt(long index);

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
     * @return          the index of the first occurrence of this value in this array
     *                  in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     *                  or <code>-1</code> if this value does not occur in this range.
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
     * @return          the index of the last occurrence of this value in this array
     *                  in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     *                  or <code>-1</code> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, long value);

    PFixedArray asImmutable();

    PFixedArray asTrustedImmutable();

    MutablePFixedArray mutableClone(MemoryModel memoryModel);

    UpdatablePFixedArray updatableClone(MemoryModel memoryModel);

    default Matrix<? extends PFixedArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
}
