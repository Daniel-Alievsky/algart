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
 * <p>Special version of {@link MutableObjectArray} allowing
 * to load an element without creating new Java object.</p>
 *
 * <p>The arrays of object created via some {@link MemoryModel memory models} may not implement this interface.
 * You can check, is this interface implemented, by <code>intstanceof</code> operator.</p>
 *
 * @param <E> the generic type of array elements.
 *
 * @author Daniel Alievsky
 */
public interface MutableObjectInPlaceArray<E> extends UpdatableObjectInPlaceArray<E>, MutableObjectArray<E> {
    /*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
      float ==> Object ;;
      ObjectArray ==> ObjectInPlaceArray<E>
         !! Auto-generated: NOT EDIT !! */
    MutableObjectInPlaceArray<E> setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutableObjectInPlaceArray<E> setData(long arrayPos, Object srcArray);

    MutableObjectInPlaceArray<E> copy(Array src);

    MutableObjectInPlaceArray<E> swap(UpdatableArray another);

    MutableObjectInPlaceArray<E> length(long newLength);

    MutableObjectInPlaceArray<E> ensureCapacity(long minCapacity);

    MutableObjectInPlaceArray<E> trim();

    MutableObjectInPlaceArray<E> append(Array appendedArray);

    MutableObjectInPlaceArray<E> asCopyOnNextWrite();

    MutableObjectInPlaceArray<E> shallowClone();
    /*Repeat.IncludeEnd*/
}
