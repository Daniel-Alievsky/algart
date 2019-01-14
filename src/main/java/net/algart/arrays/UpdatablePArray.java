/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatablePArray extends PArray, UpdatableArray {
    /**
     * Sets the element #<tt>index</tt> by convertion from <tt>double</tt>,
     * as <tt>(xxx)value</tt> for numeric element type <tt>xxx</tt>
     * (<tt>byte</tt>, <tt>short</tt>, <tt>int</tt>, <tt>long</tt>,
     * <tt>float</tt>, <tt>double</tt> or <tt>char</tt>),
     * or as <tt>value!=0.0</tt> for <tt>boolean</tt> element type.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @see #getDouble(long)
     */
    public void setDouble(long index, double value);

    /**
     * Sets the element #<tt>index</tt> by convertion from <tt>long</tt>,
     * as <tt>(xxx)value</tt> for numeric element type <tt>xxx</tt>
     * (<tt>byte</tt>, <tt>short</tt>, <tt>int</tt>, <tt>long</tt>,
     * <tt>float</tt>, <tt>double</tt> or <tt>char</tt>),
     * or as <tt>value!=0</tt> for <tt>boolean</tt> element type.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @see PFixedArray#getLong(long)
     */
    public void setLong(long index, long value);

    /**
     * Equivalent to {@link #setLong(long, long) setLong(index, (long)value)},
     * but may work little faster.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @see PFixedArray#getInt(long)
     */
    public void setInt(long index, int value);

    /**
     * Fills all elements of this array by the specified value. Equivalent to
     * <tt>{@link #fill(long, long, double) fill}(0, thisArray.length(), value)</tt>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return      a reference to this array.
     * @see #fill(long, long, double)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatablePArray fill(double value);

    /**
     * Fills <tt>count</tt> elements of this array, starting from <tt>position</tt> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setDouble(long, double) setDouble}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <tt>IndexOutOfBoundsException</tt>) in the very beginning.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return         a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <tt>position</tt> and <tt>count</tt>
     *                                   (<tt>position &lt; 0 || count &lt; 0 || position + count &gt; length()</tt>).
     * @see #fill(double)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatablePArray fill(long position, long count, double value);

    /**
     * Fills all elements of this array by the specified value. Equivalent to
     * <tt>{@link #fill(long, long, long) fill}(0, thisArray.length(), value)</tt>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return      a reference to this array.
     * @see #fill(long, long, long)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatablePArray fill(long value);

    /**
     * Fills <tt>count</tt> elements of this array, starting from <tt>position</tt> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setLong(long, long) setLong}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <tt>IndexOutOfBoundsException</tt>) in the very beginning.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return         a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <tt>position</tt> and <tt>count</tt>
     *                                   (<tt>position &lt; 0 || count &lt; 0 || position + count &gt; length()</tt>).
     * @see #fill(long)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatablePArray fill(long position, long count, long value);

    public Class<? extends UpdatablePArray> updatableType();

    public UpdatablePArray subArray(long fromIndex, long toIndex);

    public UpdatablePArray subArr(long position, long count);

    public UpdatablePArray asUnresizable();
}
