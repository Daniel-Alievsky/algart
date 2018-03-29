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
 * <p>Resizable AlgART array of some objects (non-primitive values) with the specified generic type <tt>E</tt>,
 * read-write and resize access.</p
 *
 * <p>Any class implementing this interface <b>must</b> contain non-primitive elements
 * ({@link #elementType()} must not return a primitive type).</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableObjectArray<E> extends ObjectStack<E>, MutableArray, UpdatableObjectArray<E> {
    public <D> MutableObjectArray<D> cast(Class<D> elementType);

    /*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
      float ==> Object ;;
      ObjectArray ==> ObjectArray<E>
         !! Auto-generated: NOT EDIT !! */
    public MutableObjectArray<E> setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableObjectArray<E> setData(long arrayPos, Object srcArray);

    public MutableObjectArray<E> copy(Array src);

    public MutableObjectArray<E> swap(UpdatableArray another);

    public MutableObjectArray<E> length(long newLength);

    public MutableObjectArray<E> ensureCapacity(long minCapacity);

    public MutableObjectArray<E> trim();

    public MutableObjectArray<E> append(Array appendedArray);

    public MutableObjectArray<E> asCopyOnNextWrite();

    public MutableObjectArray<E> shallowClone();
    /*Repeat.IncludeEnd*/
}
