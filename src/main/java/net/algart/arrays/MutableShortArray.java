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

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Short ;;
  float ==> short
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>short</tt> values.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutableShortArray extends ShortStack, UpdatableShortArray, MutablePIntegerArray {
    MutableShortArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutableShortArray setData(long arrayPos, Object srcArray);

    MutableShortArray copy(Array src);

    MutableShortArray swap(UpdatableArray another);

    MutableShortArray length(long newLength);

    MutableShortArray ensureCapacity(long minCapacity);

    MutableShortArray trim();

    MutableShortArray append(Array appendedArray);

    MutableShortArray asCopyOnNextWrite();

    MutableShortArray shallowClone();
    /*Repeat.IncludeEnd*/
}
