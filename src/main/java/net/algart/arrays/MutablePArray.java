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

import java.util.Objects;

/**
 * <p>Resizable AlgART array of primitive elements (boolean, char, byte, short, int, long, float or double).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableBitArray}, {@link MutableCharArray},
 * {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray},
 * {@link MutableFloatArray}, {@link MutableDoubleArray}
 * subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutablePArray extends UpdatablePArray, MutableArray {
    /**
     * Removes the last element at this array and returns its value,
     * converted to <code>double</code>:
     * <code>(double)(value&amp;0xFF)</code> for <code>byte</code> value,
     * <code>(double)(value&amp;0xFFFF)</code> for <code>short</code> value,
     * <code>(double)value</code> for <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code>, <code>char</code> values,
     * or <code>value?1.0:0.0</code> for <code>boolean</code> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * excepting the case of very large <code>long</code> elements.
     * If this array {@link #isEmpty() is empty}, the method throws <code>EmptyStackException</code>.
     *
     * <p>Depending on the specific subinterface implemented by the object,
     * this method is equivalent to one of the following expressions:</p>
     *
     * <ul>
     *     <li>for {@link MutableBitArray}: {@link BitStack#popBit() popBit()} ? 1.0 : 0.0;</li>
     *     <li>for {@link MutableCharArray}: {@link CharStack#popChar() popChar()};</li>
     *     <li>for {@link MutableByteArray}: {@link ByteStack#popByte() popByte()} &amp; 0xFF;</li>
     *     <li>for {@link MutableShortArray}: {@link ShortStack#popShort() popShort()} &amp; 0xFFFF;</li>
     *     <li>for {@link MutableIntArray}: {@link IntStack#popInt() popInt()};</li>
     *     <li>for {@link MutableLongArray}: {@link LongStack#popLong() popLong()};</li>
     *     <li>for {@link MutableFloatArray}: {@link FloatStack#popFloat() popFloat()};</li>
     *     <li>for {@link MutableDoubleArray}: the same method is already declared in its superinterface
     *     {@link DoubleStack}.</li>
     * </ul>
     *
     * @return the last element at the array (it is removed from the array).
     * @throws java.util.EmptyStackException if this array is empty.
     */
    double popDouble();

    /**
     * Appends <code>value</code> element to the end of this array with conversion from <code>double</code>,
     * as <code>(xxx)value</code> for numeric element type <code>xxx</code>
     * (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> or <code>char</code>),
     * or as <code>value!=0.0</code> for <code>boolean</code> element type.
     *
     * <p>The same action may be performed by the following code:</p>
     * <pre>
     * array.{@link MutablePArray#length(long) length}(array.{@link #length() length()}+1);
     * array.{@link UpdatablePArray#setDouble(long, double)
     * setDouble}(array.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this array.
     * @throws TooLargeArrayException if the resulting array length is too large for this type of array.
     */
    void addDouble(double value);

    /**
     * Appends <code>value</code> element to the end of this array with conversion from <code>long</code>,
     * as <code>(xxx)value</code> for numeric element type <code>xxx</code>
     * (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> or <code>char</code>),
     * or as <code>value!=0</code> for <code>boolean</code> element type.
     *
     * <p>The same action may be performed by the following code:</p>
     * <pre>
     * array.{@link MutablePArray#length(long) length}(array.{@link #length() length()}+1);
     * array.{@link UpdatablePArray#setLong(long, long)
     * setLong}(array.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this array.
     * @throws TooLargeArrayException if the resulting array length is too large for this type of array.
     */
    void addLong(long value);

    /**
     * Appends <code>value</code> element to the end of this array with conversion from <code>int</code>,
     * as <code>(xxx)value</code> for numeric element type <code>xxx</code>
     * (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> or <code>char</code>),
     * or as <code>value!=0</code> for <code>boolean</code> element type.
     *
     * <p>This method is equivalent to both {@link #addLong(long) addLong(index, (long) value)}
     * and {@link #addDouble(double) addDouble((double) value)}, but can work little faster.
     *
     * @param value to be added to the top of this array.
     * @throws TooLargeArrayException if the resulting array length is too large for this type of array.
     */
    void addInt(int value);

    MutablePArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutablePArray setData(long arrayPos, Object srcArray);

    MutablePArray copy(Array src);

    MutablePArray swap(UpdatableArray another);

    MutablePArray length(long newLength);

    MutablePArray ensureCapacity(long minCapacity);

    MutablePArray trim();

    MutablePArray append(Array appendedArray);

    MutablePArray asCopyOnNextWrite();

    MutablePArray shallowClone();

    /**
     * Equivalent to <code>(MutablePArray) to {@link MemoryModel#newEmptyArray(Class)
     * memoryModel.newEmptyArray(elementType)}</code>, but with throwing
     * <code>IllegalArgumentException</code> in a case when the type casting to {@link MutablePArray}
     * is impossible (non-primitive element type).
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @param elementType the type of array elements.
     * @return created empty AlgART array.
     * @throws NullPointerException            if one of the arguments is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not a primitive class.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is not supported by this memory model.
     */
    static MutablePArray newArray(MemoryModel memoryModel, Class<?> elementType) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(elementType, "Null element type");
        if (!elementType.isPrimitive()) {
            throw new IllegalArgumentException("Not a primitive type: " + elementType);
        }
        return (MutablePArray) memoryModel.newEmptyArray(elementType);
    }

    /**
     * Equivalent to <code>{@link #newArray(MemoryModel, Class)
     * newArray}({@link Arrays#SMM Arrays.SMM}, elementType)</code>.
     *
     * @param elementType the type of array elements.
     * @return created empty AlgART array.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if <code>elementType</code> is not a primitive class.
     */
    static MutablePArray newArray(Class<?> elementType) {
        return newArray(Arrays.SMM, elementType);
    }
}
