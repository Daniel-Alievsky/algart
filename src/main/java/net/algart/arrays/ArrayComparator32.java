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

/**
 * <p>Version of {@link ArrayComparator} for a case of 32-bit indexes (<tt>int</tt> instead of <tt>long</tt>).
 *
 * @author Daniel Alievsky
 */
@FunctionalInterface
public interface ArrayComparator32 extends ArrayComparator {
    /**
     * This method, implemented in this interface, just calls another <tt>less</tt> method with <tt>int</tt> indexes:
     * <pre>
     *     {@link #less(int, int) less}((int) first, (int) second);
     * </pre>
     * Note: for maximal performance, it does not check that the passed intexes are really 32-bit.
     * While using with arrays, containing 2<sup>31</sup> elements or more, this comparator will work incorrecly.
     *
     * @param first  index of the first compared element.
     * @param second index of the second compared element.
     * @return <tt>true</tt> if, and only if, the element <tt>#first</tt> is "less"
     * than the element <tt>#second</tt>.
     */
    default boolean less(long first, long second) {
        return less((int) first, (int) second);
    }

    /**
     * Should return <tt>true</tt> if, and only if, the element at position <tt>first</tt>
     * in the sorted array is "less" than the element at position <tt>second</tt>.
     * ("Less" element will have less index in the sorted array.)
     * The result of this comparison <i>must be fully defined by the values of the elements</i>
     * of the sorted array.
     *
     * @param first  index of the first compared element.
     * @param second index of the second compared element.
     * @return <tt>true</tt> if, and only if, the element <tt>#first</tt> is "less"
     * than the element <tt>#second</tt>.
     */
    boolean less(int first, int second);
}
