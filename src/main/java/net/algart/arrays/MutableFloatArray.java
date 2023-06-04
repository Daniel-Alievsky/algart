/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Resizable AlgART array of <tt>float</tt> values.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutableFloatArray extends FloatStack, UpdatableFloatArray, MutablePFloatingArray {
    /*Repeat.SectionStart resultTypes*/
    public MutableFloatArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableFloatArray setData(long arrayPos, Object srcArray);

    public MutableFloatArray copy(Array src);

    public MutableFloatArray swap(UpdatableArray another);

    public MutableFloatArray length(long newLength);

    public MutableFloatArray ensureCapacity(long minCapacity);

    public MutableFloatArray trim();

    public MutableFloatArray append(Array appendedArray);

    public MutableFloatArray asCopyOnNextWrite();

    public MutableFloatArray shallowClone();
    /*Repeat.SectionEnd resultTypes*/
/*Repeat.SectionEnd all*/
}
