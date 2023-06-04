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

import java.util.Arrays;

/**
 * <p>Special version of {@link ArraySelector} class, optimized for selecting from <tt>byte[]</tt> arrays.</p>
 *
 * <p>We recommend to use methods of this class, instead of {@link ArraySelector#select(int[], byte[], int)} and
 * {@link ArraySelector#select(double[], byte[], int)}, for relatively large byte arrays.
 * Typically, short <tt>byte[]</tt> arrays (&le;50&ndash;100 elements) are sorted faster by {@link ArraySelector}
 * methods, and arrays larger than 200&ndash;300 bytes are sorted faster by this class.
 * But if you need to find a lot of percentiles for the same array, for example &ge;5 levels,
 * this class may provide better speed even for shorter arrays (50&ndash;100 bytes).
 * For very short arrays this class does not provide optimization.</p>
 *
 * <p>Note that this class allocates some memory (~256 bytes) while instantiation and reuse
 * it while every call of its methods.</p>
 *
 * <p>This class is not <b>immutable</b> and not <b>thread-safe</b>, but is <b>thread-compatible</b>
 * (allows manual synchronization for multithreading access).</p>
 *
 * @author Daniel Alievsky
 */
public class ByteArraySelector {
    private static final int MAX_LENGTH_FOR_PARTIAL_CLEARING = 100;

    private final int[] histogram = new int[256];
    private final int[] histogram16 = new int[16];

    /**
     * Finds the percentiles with the specified indexes among first <tt>length</tt>
     * elements of the passed array of bytes and returns them in <tt>results</tt> argument.
     * Percentile with <tt>index=percentileIndexes[k]</tt> is the value of the element of the passed array,
     * which will be placed at position <tt>array[index]</tt> after sorting this array in increasing order;
     * but this method does not actually modify this array.
     *
     * <p>For example, <tt>percentileIndexes={0, length/2, length-1}</tt> requests to find
     * the minimum, median and maximum and return them in
     * <tt>results[0]</tt>, <tt>results[1]</tt>, <tt>results[2]</tt>.
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFF</tt> and <tt>array[j] & 0xFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * <p>Note: list of indexes <tt>percentileIndexes</tt> <b>must be sorted</b> in increasing order.
     * You can provide this, for example, but simple call of standard Java sorting method
     * <tt>Arrays.sort(percentileIndexes)</tt> before calling this method.
     * If these indexes are not sorted, or if they are out of range <tt>0..length</tt>,
     * the results will be unpredictable.
     * You can check these indexes yourself by {@link ArraySelector#checkPercentileIndexes(int[], int)} method.</p>
     *
     * <p>This method does not modify the passed <tt>array</tt>.</p>
     *
     * @param results           results of this method: values of some elements of the passed array.
     * @param percentileIndexes list of indexes inside the array, the values of which, in increasing order,
     *                          must be returned.
     * @param array             array of bytes.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(byte[] results, int[] percentileIndexes, byte[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        for (int k = 0; k < length; k++) {
            int v = array[k] & 0xFF;
            histogram[v]++;
            histogram16[v >> 4]++;
        }
        int i = 0;
        int j = 0;
        // - actual position is i*16+j
        int acc16 = 0;
        int acc = 0;
        for (int levelIndex = 0; levelIndex < percentileIndexes.length; levelIndex++) {
            final int requiredArrayIndex = percentileIndexes[levelIndex];
            assert requiredArrayIndex >= 0;
            assert requiredArrayIndex < length;
            // result is in 16*i+j..255 range
            for (; ; i++) {
                int h16 = histogram16[i];
                int newAcc16 = acc16 + h16;
                if (requiredArrayIndex < newAcc16) {
                    break;
                }
                acc = acc16 = newAcc16;
                j = 0;
            }
            // result is in 16*i+j..16*i+15 range
            final int start = i << 4;
            int p = start + j;
            for (; ; p++) {
                int h = histogram[p];
                int newAcc = acc + h;
                if (requiredArrayIndex < newAcc) {
                    break;
                }
                acc = newAcc;
            }
            results[levelIndex] = (byte) p;
            j = p - start;
        }
        if (length <= MAX_LENGTH_FOR_PARTIAL_CLEARING) {
            for (int k = 0; k < length; k++) {
                histogram[array[k] & 0xFF] = 0;
            }
        } else {
            Arrays.fill(histogram, 0);
        }
        Arrays.fill(histogram16, 0);
    }

    /**
     * Finds the percentiles with the specified levels (from 0.0 to 1.0) among first <tt>length</tt>
     * elements of the passed array of bytes and returns them in <tt>results</tt> argument.
     *
     * <p>For example, <tt>percentileLevels={0.0, 0.5, 1.0}</tt> requests to find the minimum, median and maximum
     * and return them in <tt>results[0]</tt>, <tt>results[1]</tt>, <tt>results[2]</tt>.
     *
     * <p>This method is equivalent to
     * <pre>
     *     {@link #select(byte[], int[], byte[], int)
     * select}(results, percentileIndexes, array, length)
     * </pre>
     * call, where <tt>percentileIndexes[k]={@link ArraySelector#percentileIndex(double, int)
     * ArraySelector.percentileIndex}(percentileLevels[k], length)</tt>
     * for every <tt>k=0,...,percentileLevels.length-1</tt>.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFF</tt> and <tt>array[j] & 0xFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * <p>Note: list of levels <tt>percentileLevels</tt> <b>must be sorted</b> in increasing order.
     * You can provide this, for example, but simple call of standard Java sorting method
     * <tt>Arrays.sort(percentileLevels)</tt> before calling this method.
     * If these levels are not sorted, or if they are out of range <tt>0.0..1.0</tt>,
     * the results will be unpredictable.
     * You can check these levels yourself by {@link ArraySelector#checkPercentileLevels(double[])} method.</p>
     *
     * <p>This method does not modify the passed <tt>array</tt>.</p>
     *
     * @param results          results of this method: values of some elements of the passed array.
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            array of bytes.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(byte[] results, double[] percentileLevels, byte[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        for (int k = 0; k < length; k++) {
            int v = array[k] & 0xFF;
            histogram[v]++;
            histogram16[v >> 4]++;
        }
        int i = 0;
        int j = 0;
        // - actual position is i*16+j
        int acc16 = 0;
        int acc = 0;
        for (int levelIndex = 0; levelIndex < percentileLevels.length; levelIndex++) {
            final int requiredArrayIndex = ArraySelector.percentileIndex(percentileLevels[levelIndex], length);
            assert requiredArrayIndex >= 0;
            assert requiredArrayIndex < length;
            // result is in 16*i+j..255 range
            for (; ; i++) {
                int h16 = histogram16[i];
                int newAcc16 = acc16 + h16;
                if (requiredArrayIndex < newAcc16) {
                    break;
                }
                acc = acc16 = newAcc16;
                j = 0;
            }
            // result is in 16*i+j..16*i+15 range
            final int start = i << 4;
            int p = start + j;
            for (; ; p++) {
                int h = histogram[p];
                int newAcc = acc + h;
                if (requiredArrayIndex < newAcc) {
                    break;
                }
                acc = newAcc;
            }
            results[levelIndex] = (byte) p;
            j = p - start;
        }
        if (length <= MAX_LENGTH_FOR_PARTIAL_CLEARING) {
            for (int k = 0; k < length; k++) {
                histogram[array[k] & 0xFF] = 0;
            }
        } else {
            Arrays.fill(histogram, 0);
        }
        Arrays.fill(histogram16, 0);
    }
}
