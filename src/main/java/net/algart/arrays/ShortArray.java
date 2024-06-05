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

/*Repeat(INCLUDE_FROM_FILE, FloatArray.java, all)
  PFloating ==> PInteger ;;
  float\s+getFloat ==> int getShort ;;
  Float(?!ing) ==> Short ;;
  float ==> short ;;
  (eturns?\s+the\s+)element ==> $1unsigned short (char)
     !! Auto-generated: NOT EDIT !! */

/**
 * <p>AlgART array of <code>short</code> values, read-only access.</p>
 *
 * @author Daniel Alievsky
 */
public interface ShortArray extends PIntegerArray {
    Class<? extends ShortArray> type();

    Class<? extends UpdatableShortArray> updatableType();

    Class<? extends MutableShortArray> mutableType();

    /**
     * Returns the unsigned short (char) #<code>index</code>.
     *
     * @param index index of element to get.
     * @return the unsigned short (char) at the specified position in this array.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    int getShort(long index);

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</code>
     * and <code>{@link #getShort(long) getShort}(k)==value</code>,
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
     * @return the index of the first occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, short value);

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>{@link #getShort(long) getShort}(k)==value</code>,
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
     * @return the index of the last occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, short value);

    DataShortBuffer buffer(DataBuffer.AccessMode mode, long capacity);

    DataShortBuffer buffer(DataBuffer.AccessMode mode);

    DataShortBuffer buffer(long capacity);

    DataShortBuffer buffer();

    ShortArray asImmutable();

    ShortArray asTrustedImmutable();

    MutableShortArray mutableClone(MemoryModel memoryModel);

    UpdatableShortArray updatableClone(MemoryModel memoryModel);

    short[] ja();

    default Matrix<? extends ShortArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
    /*Repeat.IncludeEnd*/

    /**
     * Equivalent to {@link #getShort(long)}.
     *
     * <p>Note that this method is already declared in {@link PFixedArray}.
     * It is redeclared here only for documentation and code search purposes.</p>
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     */
    int getInt(long index);

    @Override
    default short[] jaShort() {
        return ja();
    }

    /**
     * Equivalent to <tt>{@link SimpleMemoryModel#asUpdatableShortArray(short[])
     * SimpleMemoryModel.asUpdatableShortArray}(array)</tt>.
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    static UpdatableShortArray as(short[] array) {
        return SimpleMemoryModel.asUpdatableShortArray(array);
    }
}
