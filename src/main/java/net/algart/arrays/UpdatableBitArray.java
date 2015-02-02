/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
  PFloating ==> PFixed ;;
  Float(Array|Copies) ==> Bit$1 ;;
  setFloat ==> setBit ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>AlgART array of <tt>boolean</tt> values, read/write access, no resizing.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatableBitArray extends BitArray, UpdatablePFixedArray {
    /**
     * Sets the element #<tt>index</tt> to specified <tt>value</tt>.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    public void setBit(long index, boolean value);

    /**
     * Fills all elements of this array by the specified value. Equivalent to
     * <tt>{@link #fill(long, long, boolean) fill}(0, thisArray.length(), value)</tt>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return      a reference to this array.
     * @see #fill(long, long, boolean)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableBitArray fill(boolean value);

    /**
     * Fills <tt>count</tt> elements of this array, starting from <tt>position</tt> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setBit(long, boolean) setBit}(position + k, value);
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
     * @see #fill(boolean)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableBitArray fill(long position, long count, boolean value);

    public UpdatableBitArray subArray(long fromIndex, long toIndex);

    public UpdatableBitArray subArr(long position, long count);

    public UpdatableBitArray asUnresizable();
    /*Repeat.IncludeEnd*/

    /**
     * Sets the bit #<tt>index</tt> to 1 (<tt>true</tt>).
     *
     * @param index index of element to replace.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     */
    public void setBit(long index);

    /**
     * Clears the bit #<tt>index</tt> to 0 (<tt>false</tt>).
     *
     * @param index index of element to replace.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     */
    public void clearBit(long index);

    /**
     * Copies <tt>count</tt> bits from the specified <i>packed</i> bit array,
     * starting from <tt>srcArrayOffset</tt> index,
     * into this array, starting from <tt>arrayPos</tt> index.
     *
     * <p>This method is equivalent to the following loop:<pre>
     * for (long k = 0; k < count; k++)
     * &#32;   thisArray.{@link #setBit(long, boolean)
     * setBit}(arrayPos + k, {@link PackedBitArrays#getBit(long[], long)
     * PackedBitArrays.getBit}(srcArray, srcArrayOffset + k));
     * </pre>
     *
     * <p>Note: if <tt>IndexOutOfBoundsException</tt> occurs due to attempt to read data outside the passed
     * Java array, this AlgART array can be partially filled.
     * In other words, this method <b>can be non-atomic regarding this failure</b>.
     * All other possible exceptions are checked in the very beginning of this method
     * before any other actions (the standard way for checking exceptions).
     *
     * @param arrayPos       starting position in this AlgART array.
     * @param srcArray       the source packed bit array.
     * @param srcArrayOffset starting position in the source packed bit array.
     * @param count          the number of bits to be copied.
     * @return               a reference to this AlgART array.
     * @throws NullPointerException      if <tt>srcArray</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or source Java array.
     * @see #getData(long, Object, int, int)
     * @see #getBits(long, long[], long, long)
     * @see PackedBitArrays
     */
    public UpdatableBitArray setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count);
}
