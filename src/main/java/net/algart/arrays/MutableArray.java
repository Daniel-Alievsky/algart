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
 * <p>AlgART one-dimensional array of any elements, full access (reading, writing, resizing).</p>
 *
 * <p>If the elements of this array are primitive values (<tt>byte</tt>, <tt>short</tt>, etc.),
 * the array <b>must</b> implement one of
 * {@link MutableBitArray}, {@link MutableCharArray}, {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray}, {@link MutableFloatArray}, {@link MutableDoubleArray}
 * subinterfaces.
 * In other case, this array <b>must</b> implement {@link MutableObjectArray} subinterface.</p>
 *
 * <p>Resizable arrays, implementing this interface,
 * are not thread-safe, but <b>are thread-compatible</b>
 * and can be synchronized manually if multithread access is necessary.
 * Please see more details in the
 * <a href="package-summary.html#multithreading">package description</a>.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 * @see Array
 * @see UpdatableArray
 * @see Matrix
 */
public interface MutableArray extends Stack, UpdatableArray {
    /**
     * Changes the current number of elements in this array.
     * If <tt>newLength</tt> is greater than current number of elements,
     * all new elements will be always zero (0 for numbers, <tt>false</tt> for booleans,
     * <tt>null</tt> or some "empty" objects for non-primitive elements).
     *
     * <p>Notice: if <tt>newLength</tt> is less than current number of elements,
     * then the unused elements (from <tt>newLength</tt> to <tt>(current length)-1</tt>)
     * may be changed (for example, filled by zero) while calling this method.
     * It is not important usually, because the array doesn't provide access to elements
     * after <tt>(current length)-1</tt>.
     * But if there are some "views" of this array, for examples, created by
     * {@link #subArray(long, long)} or {@link #asUnresizable()} methods,
     * you need to remember that reducing length of the source array can lead to changing
     * last elements of such views.
     *
     * @param newLength the desired new number of elements.
     * @return          a reference to this array.
     * @throws TooLargeArrayException   if the specified length is too large for this type of arrays.
     * @throws IllegalArgumentException if the specified length is negative.
     * @see Arrays#lengthUnsigned(MutableArray, long)
   */
    public MutableArray length(long newLength);

    /**
     * Increases the capacity of this instance, if necessary,
     * to ensure that it can hold at least the given number of elements.
     * After this call, the current capacity will be &gt;=<tt>minCapacity</tt>.
     *
     * @param minCapacity the desired minimum capacity.
     * @return a reference to this array.
     * @throws TooLargeArrayException   if the specified capacity is too large for this type of arrays.
     * @throws IllegalArgumentException if the specified capacity is negative.
     */
    public MutableArray ensureCapacity(long minCapacity);

    /**
     * Trims the capacity of this array to be the array's current length.
     * An application can use this operation to minimize the memory used by this instance.
     * This method is the only way to reduce the capacity: all other methods can
     * only increase the capacity of this instance.
     *
     * @return a reference to this array.
     */
    public MutableArray trim();

    /**
     * Appends the specified array to the end of this array.
     * This method must work always if the element types of this and appended arrays
     * (returned by {@link #elementType()}) are the same.
     * <tt>IllegalArgumentException</tt> will be thrown.
     *
     * @param appendedArray  appended array.
     * @return               a reference to this array.
     * @throws NullPointerException     if <tt>appendedArray</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the source and this element types do not match.
     * @throws TooLargeArrayException   if the resulting array length is too large for this type of arrays.
     * @see #asUnresizable()
     */
    public MutableArray append(Array appendedArray);

    public MutableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableArray setData(long arrayPos, Object srcArray);

    public MutableArray copy(Array src);

    public MutableArray swap(UpdatableArray another);

    public MutableArray asCopyOnNextWrite();

    public MutableArray shallowClone();
}
