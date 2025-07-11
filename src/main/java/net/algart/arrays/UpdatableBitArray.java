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

/*Repeat(INCLUDE_FROM_FILE, UpdatableFloatArray.java, all)
  PFloating ==> PFixed ;;
  Float(Array|Copies) ==> Bit$1 ;;
  setFloat ==> setBit ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean
     !! Auto-generated: NOT EDIT !! */

/**
 * <p>AlgART array of <code>boolean</code> values, read/write access, no resizing.</p>
 *
 * @author Daniel Alievsky
 */
public interface UpdatableBitArray extends BitArray, UpdatablePFixedArray {
    /**
     * Sets the element #<code>index</code> to the specified <code>value</code>.
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    void setBit(long index, boolean value);

    /**
     * Fills all the elements of this array by the specified value. Equivalent to
     * <code>{@link #fill(long, long, boolean) fill}(0, thisArray.length(), value)</code>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return a reference to this array.
     * @see #fill(long, long, boolean)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatableBitArray fill(boolean value);

    /**
     * Fills <code>count</code> elements of this array, starting from <code>position</code> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setBit(long, boolean) setBit}(position + k, value);
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
     * @see #fill(boolean)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    UpdatableBitArray fill(long position, long count, boolean value);

    UpdatableBitArray subArray(long fromIndex, long toIndex);

    UpdatableBitArray subArr(long position, long count);

    UpdatableBitArray asUnresizable();

    default Matrix<UpdatableBitArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
    /*Repeat.IncludeEnd*/

    /**
     * Equivalent to <code>{@link #setBit(long, boolean) setBit}(index, true)</code>.
     *
     * @param index index of the element to replace.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     */
    void setBit(long index);

    /**
     * Equivalent to <code>{@link #setBit(long, boolean) setBit}(index, false)</code>.
     *
     * @param index index of the element to replace.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     */
    void clearBit(long index);

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> <b>in a non-thread-safe manner</b>:
     * without a strict requirement for internal synchronization.
     * This means that when calling this method from different threads for the same instance,
     * it can cause modification (corruption) of some bits "around" the bit <code>#index</code>,
     * namely the bits inside the same 64-bit block with indexes <code>64k...64k+63</code>,
     * where <code>k=index/64</code>.
     *
     * <p>In contrast, {@link #setBit(long, boolean)} method guarantees that setting a bit at index
     * <i>i</i> will never affect to any bit with other index <i>j&ne;i</i>.
     * This allows you to split bit array into several blocks and process each block in a separate thread,
     * as it is possible for a regular Java array.</p>
     *
     * <p>Note that this method is usually <b>much</b> faster than {@link #setBit(long, boolean)}.
     * If you are not going to work with this array from different threads, you should prefer this method.
     * Also you may freely use this method if you are synchronizing all access to this array via some
     * form of external synchronization: in this case, no additional internal synchronization is needed.
     * (But remember: such external synchronization must be used on <b>any</b> access to this array,
     * not only when calling this method!)</p>
     *
     * <p>Note that some classes may correctly implement this interface without any synchronization
     * or, vise versa, always use synchronization. In such cases this method may be equivalent
     * to {@link #setBit(long, boolean)}.</p>
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    void setBitNoSync(long index, boolean value);

    /**
     * Equivalent to <code>{@link #setBitNoSync(long, boolean) setBitNoSync}(index, true)</code>.
     *
     * @param index index of the element to replace.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     */
    void setBitNoSync(long index);

    /**
     * Equivalent to <code>{@link #setBitNoSync(long, boolean) setBitNoSync}(index, false)</code>.
     *
     * @param index index of the element to replace.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     */
    void clearBitNoSync(long index);

    /**
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#arrayPos</code>.
     * This is the reverse operation of {@link #getBits64(long, int)}.
     *
     * <p>This function is equivalent to the following loop (for correct <code>count</code> in the range 0..64):</p>
     *
     * <pre>
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = (bits &gt;&gt;&gt; k) &amp; 1L;
     *          {@link #setBit(long, boolean) setBit}(arrayPos + k, bit != 0);
     *      }</pre>
     *
     * <p>But this method works significantly faster in basic implementations of this interface,
     * if <code>count</code> is greater than 1.</p>
     *
     * @param arrayPos position of the first bit written in the destination array.
     * @param bits     sequence of new bits to be copied into the destination array.
     * @param count    the number of bits to be written (must be in range 0..64).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    default void setBits64(long arrayPos, long bits, int count) {
        if (arrayPos < 0) {
            throw new IndexOutOfBoundsException("Negative arrayPos argument: " + arrayPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64 method");
        }
        for (int k = count - 1; k >= 0; k--) {
            // - inverse loop order allows to guarantee that IndexOutOfBoundsException
            // will occur before modifying anything
            final long bit = (bits >>> k) & 1L;
            setBit(arrayPos + k, bit != 0);
        }
    }

    /**
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#arrayPos</code>
     * <b>in a non-thread-safe manner</b>:
     * without a strict requirement for internal synchronization.
     * This means that when calling this method from different threads for the same instance,
     * it can cause modification (corruption) of some bits "around" the bit <code>#index</code>,
     * namely the bits inside the same 64-bit block with indexes <code>64k...64k+63</code>,
     * where <code>k=index/64</code>.
     *
     * <p>Note that this method is usually <b>much</b> faster than {@link #setBits64(long, long, int)}.
     * If you are not going to work with this array from different threads, you should prefer this method.
     * Also you may freely use this method if you are synchronizing all access to this array via some
     * form of external synchronization: in this case, no additional internal synchronization is needed.
     * (But remember: such external synchronization must be used on <b>any</b> access to this array,
     * not only when calling this method!)</p>
     *
     * <p>Note that some classes may correctly implement this interface without any synchronization
     * or, vise versa, always use synchronization. In such cases this method may be equivalent
     * to {@link #setBits64(long, long, int)}.</p>
     *
     * @param arrayPos position of the first bit written in the destination array.
     * @param bits     sequence of new bits to be copied into the destination array.
     * @param count    the number of bits to be written (must be in range 0..64).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    default void setBits64NoSync(long arrayPos, long bits, int count) {
        if (arrayPos < 0) {
            throw new IndexOutOfBoundsException("Negative arrayPos argument: " + arrayPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64NoSync method");
        }
        for (int k = count - 1; k >= 0; k--) {
            // - inverse loop order allows to guarantee that IndexOutOfBoundsException
            // will occur before modifying anything
            final long bit = (bits >>> k) & 1L;
            setBitNoSync(arrayPos + k, bit != 0);
        }
    }

    /**
     * Copies <code>count</code> bits from the specified <i>packed</i> bit array,
     * starting from <code>srcArrayOffset</code> index,
     * into this array, starting from <code>arrayPos</code> index.
     *
     * <p>This method is equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++)
     * &#32;   thisArray.{@link #setBit(long, boolean)
     * setBit}(arrayPos + k, {@link PackedBitArrays#getBit(long[], long)
     * PackedBitArrays.getBit}(srcArray, srcArrayOffset + k));
     * </pre>
     * <p>but usually works much faster.</p>
     *
     * <p>Note: if <code>IndexOutOfBoundsException</code> occurs due to attempt to read data outside the passed
     * Java array, this AlgART array can be partially filled.
     * In other words, this method <b>can be non-atomic regarding this failure</b>.
     * All other possible exceptions are checked in the very beginning of this method
     * before any other actions (the standard way for checking exceptions).
     *
     * @param arrayPos       starting position in this AlgART array.
     * @param srcArray       the source packed bit array.
     * @param srcArrayOffset starting position in the source packed bit array.
     * @param count          the number of bits to be copied.
     * @return a reference to this AlgART array.
     * @throws NullPointerException      if <code>srcArray</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or source Java array.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code>.
     * @see #getData(long, Object, int, int)
     * @see #getBits(long, long[], long, long)
     * @see PackedBitArrays
     */
    UpdatableBitArray setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count);
}
