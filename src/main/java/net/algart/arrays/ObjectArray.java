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
 * <p>AlgART array of some objects (non-primitive values) with the specified generic type <tt>E</tt>,
 * read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> contain non-primitive elements
 * ({@link #elementType()} must not return a primitive type).</p>
 *
 * @param &lt;E&gt; the generic type of array elements.
 *
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
     * @param index index of element to get.
     * @return      the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    E get(long index);

    /**
     * Returns the minimal index <tt>k</tt>, so that
     * <tt>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</tt>
     * and <tt>value!=null?value.equals(thisArray.{@link #get(long)
     * get}(k)):thisArray.{@link #get(long) get}(k)==null</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=thisArray.{@link #length() length()}}</tt>
     * or <tt>lowIndex&gt;=highIndex</tt>, this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * <p>Unlike the standard <tt>List.indexOf</tt> method, this method never throws <tt>ClassCastException</tt>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in this array
     *                  in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     *                  or <tt>-1</tt> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, E value);


    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>value!=null?value.equals(thisArray.{@link #get(long)
     * get}(k)):thisArray.{@link #get(long) get}(k)==null</tt>,
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
     * <p>Unlike the standard <tt>List.indexOf</tt> method, this method never throws <tt>ClassCastException</tt>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in this array
     *                  in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     *                  or <tt>-1</tt> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, E value);

    /**
     * Returns this array cast to the specified generic element type
     * or throws <tt>ClassCastException</tt> if the elements cannot be cast
     * to the required type (because the {@link #elementType() element type} is not its subclass).
     * Equivalent to <tt>(ObjectArray<D>)thisArray</tt>, but is compiled
     * without "unchecked cast" warning or "inconvertible type" error.
     *
     * <p>Unlike <tt>ArrayList</tt> architecture, such casting is safe here,
     * because all methods, storing data in the AlgART array, always check the
     * {@link #elementType() element type} and do not allow saving illegal elements.
     *
     * @param elementType the required generic type.
     * @return            this array cast to the specified generic element type.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     * @throws ClassCastException   if the elements cannot be cast to the required type.
     */
    <D> ObjectArray<D> cast(Class<D> elementType);

    /*Repeat(INCLUDE_FROM_FILE, FloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
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

    E[] ja();

    default Matrix<? extends ObjectArray<E>> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
    /*Repeat.IncludeEnd*/
}
