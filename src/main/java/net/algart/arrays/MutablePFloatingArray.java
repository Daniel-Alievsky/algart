/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Resizable AlgART array of any floating-point primitive elements (float or double).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableFloatArray}, {@link MutableDoubleArray} subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface MutablePFloatingArray extends UpdatablePFloatingArray, MutablePNumberArray {
    MutablePFloatingArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    MutablePFloatingArray setData(long arrayPos, Object srcArray);

    MutablePFloatingArray copy(Array src);

    MutablePFloatingArray swap(UpdatableArray another);

    MutablePFloatingArray length(long newLength);

    MutablePFloatingArray ensureCapacity(long minCapacity);

    MutablePFloatingArray trim();

    MutablePFloatingArray append(Array appendedArray);

    MutablePFloatingArray asCopyOnNextWrite();

    MutablePFloatingArray shallowClone();
}
