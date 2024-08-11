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
 * <p>AlgART array of some objects (non-primitive values) with the specified generic type <code>E</code>,
 * read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> contain non-primitive elements
 * ({@link #elementType()} must not return a primitive type).</p>
 *
 * @param <E> the generic type of array elements.
 * @author Daniel Alievsky
 */
public interface ObjectArray<E> extends Array {

    Class<E> elementType();

    Class<? extends ObjectArray<E>> type();

    Class<? extends UpdatableObjectArray<E>> updatableType();

    Class<? extends MutableObjectArray<E>> mutableType();

    /**
     * Equivalent to {@link #getElement(long) getElement(index)}.
     *
     * @param index index of the element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    E get(long index);

    default E[] newJavaArray(int length) {
        return InternalUtils.cast(java.lang.reflect.Array.newInstance(elementType(), length));
    }

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</code>
     * and <code>value!=null?value.equals(thisArray.{@link #get(long)
     * get}(k)):thisArray.{@link #get(long) get}(k)==null</code>,
     * or <code>-1</code> if there is no such array element.
     *
     * <p>In particular, if <code>lowIndex&gt;=thisArray.{@link #length() length()}}</code>
     * or <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * <p>You may specify <code>lowIndex=0</code> and <code>highIndex=Long.MAX_VALUE</code> to search
     * through all array elements.
     *
     * <p>Unlike the standard <code>List.indexOf</code> method, this method never
     * throws <code>ClassCastException</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array.
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, E value);


    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>value!=null?value.equals(thisArray.{@link #get(long)
     * get}(k)):thisArray.{@link #get(long) get}(k)==null</code>,
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
     * <p>Unlike the standard <code>List.indexOf</code> method, this method never throws <code>ClassCastException</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array.
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, E value);

    /**
     * Returns this array cast to the specified generic element type
     * or throws <code>ClassCastException</code> if the elements cannot be cast
     * to the required type (because the {@link #elementType() element type} is not its subclass).
     * Equivalent to <code>(ObjectArray<D>)thisArray</code>, but is compiled
     * without "unchecked cast" warning or "inconvertible type" error.
     *
     * <p>Unlike <code>ArrayList</code> architecture, such casting is safe here,
     * because all methods, storing data in the AlgART array, always check the
     * {@link #elementType() element type} and do not allow saving illegal elements.
     *
     * @param elementType the required generic type.
     * @return this array cast to the specified generic element type.
     * @throws NullPointerException if the argument is {@code null}.
     * @throws ClassCastException   if the elements cannot be cast to the required type.
     */
    <D> ObjectArray<D> cast(Class<D> elementType);

    /*Repeat(INCLUDE_FROM_FILE, FloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
      float\[\]\s*toJavaArray ==> E[] toJavaArray ;;
      float\[\]\s*ja ==> E[] ja ;;
      float ==> Object ;;
      ObjectArray ==> ObjectArray<E> ;;
      DataObjectBuffer ==> DataObjectBuffer<E>
         !! Auto-generated: NOT EDIT !! */
    DataObjectBuffer<E> buffer(DataBuffer.AccessMode mode, long capacity);

    DataObjectBuffer<E> buffer(DataBuffer.AccessMode mode);

    DataObjectBuffer<E> buffer(long capacity);

    DataObjectBuffer<E> buffer();

    ObjectArray<E> asImmutable();

    ObjectArray<E> asTrustedImmutable();

    MutableObjectArray<E> mutableClone(MemoryModel memoryModel);

    UpdatableObjectArray<E> updatableClone(MemoryModel memoryModel);

    default E[] toJavaArray() {
        final long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Object[] Java array, "
                    + "because it is too large: " + this);
        }
        var result = newJavaArray((int) len);
        getData(0, result);
        return result;
    }

    E[] ja();

    default Matrix<? extends ObjectArray<E>> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }

    /*Repeat.IncludeEnd*/
}
