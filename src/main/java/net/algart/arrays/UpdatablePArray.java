/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>AlgART array of primitive elements (boolean, char, byte, short, int, long, float or double),
 * read/write access, no resizing.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link UpdatableBitArray}, {@link UpdatableCharArray},
 * {@link UpdatableByteArray}, {@link UpdatableShortArray},
 * {@link UpdatableIntArray}, {@link UpdatableLongArray},
 * {@link UpdatableFloatArray}, {@link UpdatableDoubleArray}
 * subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface UpdatablePArray extends PArray, UpdatableArray {
    /**
     * Sets the element #<code>index</code> with conversion from <code>double</code>,
     * as <code>(xxx)value</code> for numeric element type <code>xxx</code>
     * (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> or <code>char</code>),
     * or as <code>value!=0.0</code> for <code>boolean</code> element type.
     *
     * <p>Depending on the specific subinterface implemented by the object,
     * this method is equivalent to one of the following calls:</p>
     *
     * <ul>
     *     <li>for {@link UpdatableBitArray}:
     *     <code>{@link UpdatableBitArray#setBit(long, boolean) setBit}(index, value != 0.0)</code>;</li>
     *     <li>for {@link UpdatableCharArray}:
     *     <code>{@link UpdatableCharArray#setChar(long, char) setChar}(index, (char) value)</code>;</li>
     *     <li>for {@link UpdatableByteArray}:
     *     <code>{@link UpdatableByteArray#setByte(long, byte) setByte}(index, (byte) value)</code>;</li>
     *     <li>for {@link UpdatableShortArray}:
     *     <code>{@link UpdatableShortArray#setShort(long, short) setShort}(index, (short) value)</code>;</li>
     *     <li>for {@link UpdatableLongArray}:
     *     <code>{@link UpdatableLongArray#setLong(long, long) setLong}(index, (long) value)</code>;</li>
     *     <li>for {@link UpdatableFloatArray}:
     *     <code>{@link UpdatableFloatArray#setFloat(long, float) setFloat}(index, (float) value)</code>;</li>
     *     <li>for {@link UpdatableDoubleArray}: the same method is already declared in this interface.</li>
     * </ul>
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     * @see #getDouble(long)
     */
    void setDouble(long index, double value);

    /**
     * Sets the element #<code>index</code> with conversion from <code>long</code>,
     * as <code>(xxx)value</code> for numeric element type <code>xxx</code>
     * (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> or <code>char</code>),
     * or as <code>value!=0</code> for <code>boolean</code> element type.
     *
     * <p>Depending on the specific subinterface implemented by the object,
     * this method is equivalent to one of the following calls:</p>
     *
     * <ul>
     *     <li>for {@link UpdatableBitArray}:
     *     <code>{@link UpdatableBitArray#setBit(long, boolean) setBit}(index, value != 0)</code>;</li>
     *     <li>for {@link UpdatableCharArray}:
     *     <code>{@link UpdatableCharArray#setChar(long, char) setChar}(index, (char) value)</code>;</li>
     *     <li>for {@link UpdatableByteArray}:
     *     <code>{@link UpdatableByteArray#setByte(long, byte) setByte}(index, (byte) value)</code>;</li>
     *     <li>for {@link UpdatableShortArray}:
     *     <code>{@link UpdatableShortArray#setShort(long, short) setShort}(index, (short) value)</code>;</li>
     *     <li>for {@link UpdatableLongArray}: the same method is already declared in this interface;</li>
     *     <li>for {@link UpdatableFloatArray}:
     *     <code>{@link UpdatableFloatArray#setFloat(long, float) setFloat}(index, (float) value)</code>;</li>
     *     <li>for {@link UpdatableDoubleArray}:
     *     <code>{@link UpdatableDoubleArray#setDouble(long, double) setDouble}(index, (double) value)</code>.</li>
     * </ul>
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     * @see PFixedArray#getLong(long)
     */
    void setLong(long index, long value);

    /**
     * Sets the element #<code>index</code> with conversion from <code>int</code>,
     * as <code>(xxx)value</code> for numeric element type <code>xxx</code>
     * (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> or <code>char</code>),
     * or as <code>value!=0</code> for <code>boolean</code> element type.
     *
     * <p>This method is equivalent to both {@link #setLong(long, long) setLong(index, (long) value)}
     * and {@link #setDouble(long, double) setDouble(index, (double) value)}, but can work little faster.
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     * @see PFixedArray#getInt(long)
     */
    void setInt(long index, int value);

    /**
     * Fills all elements of this array with the specified value. Equivalent to
     * <code>{@link #fill(long, long, double) fill}(0, thisArray.length(), value)</code>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return a reference to this array.
     * @see #fill(long, long, double)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatablePArray fill(double value);

    /**
     * Fills <code>count</code> elements of this array, starting from <code>position</code> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setDouble(long, double) setDouble}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <code>IndexOutOfBoundsException</code>) in the very beginning.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <code>position</code> and <code>count</code>
     *                                   (<code>position &lt; 0 || count &lt; 0
     *                                   || position + count &gt; length()</code>).
     * @see #fill(double)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatablePArray fill(long position, long count, double value);

    /**
     * Fills all the elements of this array by the specified value. Equivalent to
     * <code>{@link #fill(long, long, long) fill}(0, thisArray.length(), value)</code>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return a reference to this array.
     * @see #fill(long, long, long)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatablePArray fill(long value);

    /**
     * Fills <code>count</code> elements of this array, starting from <code>position</code> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setLong(long, long) setLong}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <code>IndexOutOfBoundsException</code>) in the very beginning.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <code>position</code> and <code>count</code>
     *                                   (<code>position &lt; 0 || count &lt; 0
     *                                   || position + count &gt; length()</code>).
     * @see #fill(long)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatablePArray fill(long position, long count, long value);

    Class<? extends UpdatablePArray> updatableType();

    UpdatablePArray subArray(long fromIndex, long toIndex);

    UpdatablePArray subArr(long position, long count);

    UpdatablePArray asUnresizable();

    default Matrix<? extends UpdatablePArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
}
