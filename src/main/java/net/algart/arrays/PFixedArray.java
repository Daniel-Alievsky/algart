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
     * <tt>Integer.MIN_VALUE</tt> for {@link IntArray},
     * <tt>Long.MIN_VALUE</tt> for {@link LongArray}.
     * It is the minimal possible value,
     * that can stored in elements of this array
     * (<tt>byte</tt> and <tt>short</tt> elements are interpreted as unsigned).
     * This method is equivalent to
     * <tt>{@link Arrays#minPossibleIntegerValue(Class) minPossibleIntegerValue}(thisArray.getClass())</tt>.
     *
     * @return the minimal possible value, that can stored in elements of this array.
     * @see PArray#minPossibleValue(double)
     */
    long minPossibleValue();

    /**
     * Returns 1 for {@link BitArray},
     * 0xFF for {@link ByteArray},
     * 0xFFFF for {@link CharArray} and {@link ShortArray},
     * <tt>Integer.MAX_VALUE</tt> for {@link IntArray},
     * <tt>Long.MAX_VALUE</tt> for {@link LongArray}.
     * <tt>valueForFloatingPoint</tt> for {@link FloatArray} and {@link DoubleArray}.
     * It is the maximal possible value,
     * that can stored in elements of this array
     * (<tt>byte</tt> and <tt>short</tt> elements are interpreted as unsigned).
     * This method is equivalent to
     * <tt>{@link Arrays#maxPossibleIntegerValue(Class) maxPossibleIntegerValue}(thisArray.getClass())</tt>.
     *
     * @return the maximal possible value, that can stored in elements of this array.
     * @see PArray#maxPossibleValue(double)
     */
    long maxPossibleValue();

    /**
     * Returns the element #<tt>index</tt> converted to <tt>long</tt>:
     * <tt>(long)value&amp;0xFF</tt> for <tt>byte</tt> value,
     * <tt>(long)value&amp;0xFFFF</tt> for <tt>short</tt> value,
     * <tt>(long)value</tt> for <tt>int</tt>, <tt>long</tt>,
     * <tt>float</tt>, <tt>double</tt>, <tt>char</tt> values,
     * or as <tt>value?1:0</tt> for <tt>boolean</tt> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * if it is not a array of <tt>float</tt> or <tt>double</tt> elements.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @see UpdatablePArray#setLong(long, long)
     */
    long getLong(long index);

    /**
     * Returns the element #<tt>index</tt> converted to <tt>int</tt>:
     * <tt>(int)value&amp;0xFF</tt> for <tt>byte</tt> value,
     * <tt>(int)value&amp;0xFFFF</tt> for <tt>short</tt> value,
     * <tt>(int)value</tt> for <tt>int</tt>,
     * <tt>float</tt>, <tt>double</tt>, <tt>char</tt> values,
     * <tt>value?1:0</tt> for <tt>boolean</tt> values,
     * <tt>min(max(value, Integer.MIN_VALUE), Integer.MAX_VALUE)</tt> (i&#46;e&#46; the value
     * truncated to the range <tt>Integer.MIN_VALUE..Integer.MAX_VALUE</tt>)
     * for <tt>long</tt> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * if it is not a array of <tt>long</tt>, <tt>float</tt> or <tt>double</tt> elements.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @see UpdatablePArray#setInt(long, int)
     */
    int getInt(long index);

    /**
     * Returns the minimal index <tt>k</tt>, so that
     * <tt>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</tt>
     * and <tt>{@link #getLong(long) getLong}(k)==value</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=thisArray.{@link #length() length()}}</tt>
     * or <tt>lowIndex&gt;=highIndex</tt>, this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in this array
     *                  in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     *                  or <tt>-1</tt> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, long value);

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>{@link #getLong(long) getLong}(k)==value</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=thisArray.{@link #length() length()}</tt>,
     * the result is the same as if <tt>highIndex==thisArray.{@link #length() length()}</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in this array
     *                  in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     *                  or <tt>-1</tt> if this value does not occur in this range.
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
