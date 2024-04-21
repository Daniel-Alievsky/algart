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

/*Repeat.SectionStart all*/

/**
 * <p>AlgART array of <tt>float</tt> values, read-only access.</p>
 *
 * @author Daniel Alievsky
 */
public interface FloatArray extends PFloatingArray {
    Class<? extends FloatArray> type();

    Class<? extends UpdatableFloatArray> updatableType();

    Class<? extends MutableFloatArray> mutableType();

    /**
     * Returns the element #<tt>index</tt>.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    float getFloat(long index);

    /**
     * Returns the minimal index <tt>k</tt>, so that
     * <tt>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</tt>
     * and <tt>{@link #getFloat(long) getFloat}(k)==value</tt>,
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
     * @return the index of the first occurrence of this value in this array
     * in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     * or <tt>-1</tt> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, float value);

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>{@link #getFloat(long) getFloat}(k)==value</tt>,
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
     * @return the index of the last occurrence of this value in this array
     * in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     * or <tt>-1</tt> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, float value);

    /*Repeat.SectionStart resultTypes*/
    DataFloatBuffer buffer(DataBuffer.AccessMode mode, long capacity);

    DataFloatBuffer buffer(DataBuffer.AccessMode mode);

    DataFloatBuffer buffer(long capacity);

    DataFloatBuffer buffer();

    FloatArray asImmutable();

    FloatArray asTrustedImmutable();

    MutableFloatArray mutableClone(MemoryModel memoryModel);

    UpdatableFloatArray updatableClone(MemoryModel memoryModel);

    float[] ja();

    default Matrix<? extends FloatArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
    /*Repeat.SectionEnd resultTypes*/
    /*Repeat.SectionEnd all*/

    @Override
    default float[] jaFloat() {
        return ja();
    }

    /**
     * Equivalent to <tt>{@link SimpleMemoryModel#asUpdatableFloatArray(float[])
     * SimpleMemoryModel.asUpdatableFloatArray}(array)</tt>.
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    static UpdatableFloatArray as(float[] array) {
        return SimpleMemoryModel.asUpdatableFloatArray(array);
    }
}
