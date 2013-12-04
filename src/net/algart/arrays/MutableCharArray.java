/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
  PFloating ==> PFixed ;;
  Float(?!ing) ==> Char ;;
  float ==> char
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>char</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableCharArray extends CharStack, UpdatableCharArray, MutablePFixedArray {
    public MutableCharArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableCharArray setData(long arrayPos, Object srcArray);

    public MutableCharArray copy(Array src);

    public MutableCharArray swap(UpdatableArray another);

    public MutableCharArray length(long newLength);

    public MutableCharArray ensureCapacity(long minCapacity);

    public MutableCharArray trim();

    public MutableCharArray append(Array appendedArray);

    public MutableCharArray asCopyOnNextWrite();

    public MutableCharArray shallowClone();
    /*Repeat.IncludeEnd*/

    /**
     * Appends all characters of <tt>value</tt> to the end of this array and returns this array.
     * @param value a string to be appended to this array.
     * @return      a reference to this object.
     * @throws TooLargeArrayException if the resulting array length is too large for this type of arrays.
     */
    public MutableCharArray append(String value);
}
