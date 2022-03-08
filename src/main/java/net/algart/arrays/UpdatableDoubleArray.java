/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/*Repeat(INCLUDE_FROM_FILE, UpdatableFloatArray.java, all)
  Float(?!ing) ==> Double ;;
  float ==> double
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>AlgART array of <tt>double</tt> values, read/write access, no resizing.</p>
 *
 * @author Daniel Alievsky
 */
public interface UpdatableDoubleArray extends DoubleArray, UpdatablePFloatingArray {
    /**
     * Sets the element #<tt>index</tt> to specified <tt>value</tt>.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    public void setDouble(long index, double value);

    /**
     * Fills all elements of this array by the specified value. Equivalent to
     * <tt>{@link #fill(long, long, double) fill}(0, thisArray.length(), value)</tt>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return      a reference to this array.
     * @see #fill(long, long, double)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableDoubleArray fill(double value);

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
    public UpdatableDoubleArray fill(long position, long count, double value);

    public UpdatableDoubleArray subArray(long fromIndex, long toIndex);

    public UpdatableDoubleArray subArr(long position, long count);

    public UpdatableDoubleArray asUnresizable();
    /*Repeat.IncludeEnd*/
}
