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

/*Repeat.SectionStart all*/

/**
 * <p>AlgART array of <code>float</code> values, read/write access, no resizing.</p>
 *
 * @author Daniel Alievsky
 */
public interface UpdatableFloatArray extends FloatArray, UpdatablePFloatingArray {
    /**
     * Sets the element #<code>index</code> to the specified <code>value</code>.
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    void setFloat(long index, float value);

    /**
     * Fills all the elements of this array by the specified value. Equivalent to
     * <code>{@link #fill(long, long, float) fill}(0, thisArray.length(), value)</code>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return a reference to this array.
     * @see #fill(long, long, float)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatableFloatArray fill(float value);

    /**
     * Fills <code>count</code> elements of this array, starting from <code>position</code> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setFloat(long, float) setFloat}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <code>IndexOutOfBoundsException</code>) in the very beginning.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <code>position</code> and <code>count</code>
     *                                   (<code>position &lt; 0 || count &lt; 0 || position + count &gt; length()</code>).
     * @see #fill(float)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatableFloatArray fill(long position, long count, float value);

    /*Repeat.SectionStart resultTypes*/
    UpdatableFloatArray subArray(long fromIndex, long toIndex);

    UpdatableFloatArray subArr(long position, long count);

    UpdatableFloatArray asUnresizable();

    default Matrix<UpdatableFloatArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
    /*Repeat.SectionEnd resultTypes*/
    /*Repeat.SectionEnd all*/
}
