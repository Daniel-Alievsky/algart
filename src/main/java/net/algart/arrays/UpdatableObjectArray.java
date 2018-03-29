/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * read/write access, no resizing.</p
 * <p>Any class implementing this interface <b>must</b> contain non-primitive elements
 * ({@link #elementType()} must not return a primitive type).</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatableObjectArray<E> extends ObjectArray<E>, UpdatableArray {
    /**
     * Equivalent to {@link #setElement(long, Object) setElement(index, value)}.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     * @throws NullPointerException      if <tt>value == null</tt> and it is an array of primitive elements.
     * @throws ArrayStoreException       if <tt>value</tt> is not an instance of {@link #elementType()} class.
     */
    public void set(long index, E value);

    /**
     * Fills all elements of this array by the specified value. Equivalent to
     * <tt>{@link #fill(long, long, Object) fill}(0, thisArray.length(), value)</tt>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return      a reference to this array.
     * @see #fill(long, long, Object)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableObjectArray<E> fill(E value);

    /**
     * Fills <tt>count</tt> elements of this array, starting from <tt>position</tt> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #set(long, Object) set}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <tt>IndexOutOfBoundsException</tt>) in the very beginning.
     *
     * <p>If <tt>value == null</tt>, this method does not throw <tt>NullPointerException</tt>,
     * but may fill the elements by some default value, if <tt>null</tt> elements are not supported
     * by the {@link MemoryModel memory model} (as in a case of {@link CombinedMemoryModel}).
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return         a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <tt>position</tt> and <tt>count</tt>
     *                                   (<tt>position &lt; 0 || count &lt; 0 || position + count &gt; length()</tt>).
     * @see #fill(Object)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableObjectArray<E> fill(long position, long count, E value);

    public <D> UpdatableObjectArray<D> cast(Class<D> elementType);

    /*Repeat(INCLUDE_FROM_FILE, UpdatableFloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
      float ==> Object ;;
      ObjectArray ==> ObjectArray<E>
         !! Auto-generated: NOT EDIT !! */
    public UpdatableObjectArray<E> subArray(long fromIndex, long toIndex);

    public UpdatableObjectArray<E> subArr(long position, long count);

    public UpdatableObjectArray<E> asUnresizable();
    /*Repeat.IncludeEnd*/
}
