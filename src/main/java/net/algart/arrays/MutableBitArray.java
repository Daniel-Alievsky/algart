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
  PFloating ==> PFixed ;;
  Float(Array|Stack) ==> Bit$1 ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <code>boolean</code> values.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutableBitArray extends BitStack, UpdatableBitArray, MutablePFixedArray {
    MutableBitArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutableBitArray setData(long arrayPos, Object srcArray);

    MutableBitArray copy(Array src);

    MutableBitArray swap(UpdatableArray another);

    MutableBitArray length(long newLength);

    MutableBitArray ensureCapacity(long minCapacity);

    MutableBitArray trim();

    MutableBitArray append(Array appendedArray);

    MutableBitArray asCopyOnNextWrite();

    MutableBitArray shallowClone();
    /*Repeat.IncludeEnd*/
}
