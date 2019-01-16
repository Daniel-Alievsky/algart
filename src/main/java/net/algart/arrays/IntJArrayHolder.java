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

import java.lang.ref.SoftReference;

/*Repeat(INCLUDE_FROM_FILE, FloatJArrayHolder.java, all)
  Float ==> Int;;
  float ==> int
     !! Auto-generated: NOT EDIT !! */
/**
 * A simple class allowing to reuse Java <tt>int[]</tt> array many times.
 * It can be useful, when the algorithm usually allocates  <tt>int[]</tt> array with the same size
 * many times (changing the size is a rare event). In this case, you can replace <tt>"new"</tt> operator
 * (that spends time for zero-initialing new array) with {@link #quickNew(int)} method,
 * that probably will work very quickly.
 * The previously allocated array is stored inside the object in a <tt>SoftReference</tt>.
 *
 * <p>This class is <b>thread-safe</b>: you may use the same instance of this class in several threads.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public final class IntJArrayHolder {
    private SoftReference<int[]> reference = new SoftReference<int[]>(new int[0]);
    private final Object lock = new Object();

    /**
     * Equivalent of {@link #quickNew(int)} method, but in addition it checks that
     * <tt>newArrayLength</tt> is actually 32-bit value (<tt>newArrayLength==(int)newArrayLength</tt>)
     * and, if not, throws {@link TooLargeArrayException}.
     *
     * @param newArrayLength required array length.
     * @return newly created <tt>"new int[newArrayLength]"</tt>
     *         or previously allocated array, if it exists and has identical length.
     * @throws IllegalArgumentException if <tt>newArrayLength &lt; 0</tt>.
     * @throws TooLargeArrayException if <tt>newArrayLength &gt; Integer.MAX_VALUE</tt>.
     */
    public int[] quickNew(long newArrayLength) {
        if (newArrayLength < 0) {
            throw new IllegalArgumentException("Zero or negative array new array length");
        }
        if (newArrayLength != (int) newArrayLength) {
            throw new TooLargeArrayException("Too large requested array: " + newArrayLength + " elements");
        }
        return quickNew((int) newArrayLength);
    }

    /**
     * Quick analog of <tt>new int[newArrayLength]}</tt>.
     * If this method is called several times for allocating data with the same size,
     * this method returns previously allocated array.
     * (Previous array is stored in <tt>SoftReference</tt> and, if there is not enough memory,
     * can be utilized by garbage collector; in this case, this method will just use <tt>"new"</tt>
     * operator.)
     *
     * <p>Please remember: unlike standard <tt>new</tt> operator, the returned array is usually <b>not</b>
     * filled by zeros.
     *
     * @param newArrayLength required array length.
     * @return newly created <tt>"new int[newArrayLength]"</tt>
     *         or previously allocated array, if it exists and has identical length.
     * @throws IllegalArgumentException if <tt>newArrayLength &lt; 0</tt>
     */
    public int[] quickNew(int newArrayLength) {
        if (newArrayLength < 0) {
            throw new IllegalArgumentException("Zero or negative array new array length");
        }
        synchronized (lock) {
            final int[] oldArray = reference.get();
            if (oldArray != null && oldArray.length == newArrayLength) {
                return oldArray;
            }
            final int[] result = new int[newArrayLength];
            reference = new SoftReference<int[]>(result);
            return result;
        }
    }

    /**
     * Quick analog of <tt>array.clone()</tt>.
     * Equivalent to the following operators:
     * <pre>
     *     int[] result = {@link #quickNew(int)} quickNew}(array.length);
     *     System.arraycopy(array, 0, result, 0, array.length);
     *     (return result)
     * </pre>
     *
     * @param array some array to clone.
     * @return exact copy of the source array.
     * @throws NullPointerException if the passed array is <tt>null</tt>.
     */
    public int[] quickClone(int[] array) {
        int[] result = quickNew(array.length);
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }
}
/*Repeat.IncludeEnd*/
