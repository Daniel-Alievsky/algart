/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Exchanging interface, designed for exchanging (swapping) two elements in some data array.</p>
 *
 * <p>The basic method {@link #swap(long, long)}
 * of this interface works with indexes of the exchanged elements in the array.
 * So, every object, implementing this interface, is supposed to be working with some fixed linear data array.
 * The method of storing data in the array can be any; for example, it can be an
 * {@link UpdatableArray updatable AlgART array} or a usual Java array.
 * The length of the array is limited only by 2<sup>63</sup>&minus;1 (maximal possible value for <tt>long</tt>
 * indexes).</p>
 *
 * <p>This interface is used by {@link ArraySorter} class.</p>
 *
 * <p>Note: {@link UpdatableArray} interface extends this interface.</p>
 *
 * <p>In {@link JArrays} class you will find implementations of this interface for processing usual Java arrays.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public interface ArrayExchanger {
    /**
     * Should exchange the elements at position <tt>firstIndex</tt> and <tt>secondIndex</tt> in the data array.
     *
     * @param firstIndex  index of the first exchanged element.
     * @param secondIndex index of the second exchanged element.
     */
    void swap(long firstIndex, long secondIndex);
}
