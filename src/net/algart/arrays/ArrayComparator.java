/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Comparison interface, designed for comparing elements in some data array.</p>
 *
 * <p>Unlike the standard <tt>java.util.Comparator</tt>, the basic method {@link #less(long, long)}
 * of this interface works not with data elements, but with their indexes in the array:
 * this method should get them from the analysed array itself.
 * So, every object, implementing this interface, is supposed to be working with some fixed linear data array.
 * The method of storing data in the array can be any; for example, it can be an {@link Array AlgART array}
 * or a usual Java array.
 * The length of the array is limited only by 2<sup>63</sup>&minus;1 (maximal possible value for <tt>long</tt>
 * indexes).</p>
 *
 * <p>This interface is used by {@link ArraySorter} class.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayComparator {
    /**
     * Should return <tt>true</tt> if, and only if, the element at position <tt>firstIndex</tt>
     * in the sorted array is "less" than the element at position <tt>secondIndex</tt>.
     * ("Less" element will have less index in the sorted array.)
     * The result of this comparison <i>must be fully defined by the values of the elements</i>
     * of the sorted array.
     *
     * @param firstIndex  index of the first compared element.
     * @param secondIndex index of the second compared element.
     * @return            <tt>true</tt> if, and only if, the element <tt>#firstIndex</tt> is "less"
     *                    than the element <tt>#secondIndex</tt>.
     */
    boolean less(long firstIndex, long secondIndex);
}
