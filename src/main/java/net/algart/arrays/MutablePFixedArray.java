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
 * <p>Resizable AlgART array of any fixed-point numeric, character or bit primitive elements
 * (byte, short, int, long, char or boolean).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableBitArray}, {@link MutableCharArray},
 * {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray}
 * subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutablePFixedArray
        extends UpdatablePFixedArray, MutablePArray {
    /**
     * Removes the last element at this array and returns its value,
     * converted to <tt>long</tt>:
     * <tt>(long)value&amp;0xFF</tt> for <tt>byte</tt> value,
     * <tt>(long)value&amp;0xFFFF</tt> for <tt>short</tt> value,
     * <tt>(long)value</tt> for <tt>int</tt>, <tt>long</tt> or <tt>char</tt> values,
     * or as <tt>value?1:0</tt> for <tt>boolean</tt> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * excepting the case of very large <tt>long</tt> elements.
     * If this array {@link #isEmpty() is empty}, the method throws <tt>EmptyStackException</tt>.
     *
     * <p>Depending on the specific subinterface implemented by the object,
     * this method is equivalent to one of the following expressions:</p>
     *
     * <ul>
     *     <li>for {@link MutableBitArray}: {@link BitStack#popBit() popBit()} ? 1 : 0;</li>
     *     <li>for {@link MutableCharArray}: {@link CharStack#popChar() popChar()};</li>
     *     <li>for {@link MutableByteArray}: {@link ByteStack#popByte() popByte()} &amp; 0xFF;</li>
     *     <li>for {@link MutableShortArray}: {@link ShortStack#popShort() popShort()} &amp; 0xFFFF;</li>
     *     <li>for {@link MutableIntArray}: {@link IntStack#popInt() popInt()};</li>
     *     <li>for {@link MutableLongArray}: the same method is already declared in its superinterface
     *     {@link LongStack}.</li>
     * </ul>
     *
     * @return the last element at array (it is removed from the array).
     * @throws java.util.EmptyStackException if this array is empty.
     */
    long popLong();

    /**
     * Removes the last element at this array and returns its value,
     * converted to <tt>long</tt>:
     * <tt>(int)value&amp;0xFF</tt> for <tt>byte</tt> value,
     * <tt>(int)value&amp;0xFFFF</tt> for <tt>short</tt> value,
     * <tt>(int)value</tt> for <tt>int</tt> or <tt>char</tt> values,
     * <tt>value?1:0</tt> for <tt>boolean</tt> values,
     * <tt>min(max(value, Integer.MIN_VALUE), Integer.MAX_VALUE)</tt> (i&#46;e&#46; the value
     * truncated to the range <tt>Integer.MIN_VALUE..Integer.MAX_VALUE</tt>)
     * for <tt>long</tt> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * if it is not an array of <tt>long</tt>, <tt>float</tt> or <tt>double</tt> elements.
     * If this array {@link #isEmpty() is empty}, the method throws <tt>EmptyStackException</tt>.
     *
     * <p>Depending on the specific subinterface implemented by the object,
     * this method is equivalent to one of the following expressions:</p>
     *
     * <ul>
     *     <li>for {@link MutableBitArray}: {@link BitStack#popBit() popBit()} ? 1 : 0;</li>
     *     <li>for {@link MutableCharArray}: {@link CharStack#popChar() popChar()};</li>
     *     <li>for {@link MutableByteArray}: {@link ByteStack#popByte() popByte()} &amp; 0xFF;</li>
     *     <li>for {@link MutableShortArray}: {@link ShortStack#popShort() popShort()} &amp; 0xFFFF;</li>
     *     <li>for {@link MutableIntArray}: the same method is already declared in its superinterface
     *     {@link IntStack};</li>
     *     <li>for {@link MutableLongArray}: Arrays.truncateLongToInt({@link LongStack#popLong() popLong()}).</li>
     * </ul>
     *
     * @return the last element at array (it is removed from the array).
     * @throws java.util.EmptyStackException if this array is empty.
     */
    int popInt();

    MutablePFixedArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutablePFixedArray setData(long arrayPos, Object srcArray);

    MutablePFixedArray copy(Array src);

    MutablePFixedArray swap(UpdatableArray another);

    MutablePFixedArray length(long newLength);

    MutablePFixedArray ensureCapacity(long minCapacity);

    MutablePFixedArray trim();

    MutablePFixedArray append(Array appendedArray);

    MutablePFixedArray asCopyOnNextWrite();

    MutablePFixedArray shallowClone();
}
