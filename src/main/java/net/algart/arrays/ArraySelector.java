/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Selecting algorithms. This class provides an implementation for
 * <a target="_blank" href="https://en.wikipedia.org/wiki/Quickselect">QuickSelect</a> algorithm.</p>
 *
 * <p>The architecture of this class is similar to {@link ArraySorter} class.</p>
 *
 * <p>Unlike standard Java sorting algorithms, this class has no restriction 2<sup>31</sup>-1
 * for the length of arrays.
 * This class allows to select among up to 2<sup>63</sup>&minus;1 elements.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @see ByteArraySelector
 * @since JDK 1.6
 */
public class ArraySelector {
    private static final long THRESHOLD = 4;
    // - Shorter sub-arrays are sorted by insertion algorithm; must be >=3, because we find the median of 3 elements.
    // Note: this constant is used only in common method!
    // Specific method for primitive types implements special code for 1, 2, 3, 4 elements.

    private ArraySelector() {
    }

    /**
     * Returns an instance of this class that implements QuickSelect algorithm.
     *
     * @return implementation of QuickSelect algorithm.
     */
    public static ArraySelector getQuickSelector() {
        return new ArraySelector();
    }

    /**
     * Partially reorders elements of some array <tt>0..numberOfElements-1</tt> so, that
     * every element, having index <tt>m=percentileIndexes[k]</tt> in increasing order,
     * will be placed at the position <tt>array[m]</tt> (<tt>m=0..numberOfElements-1</tt>),
     * all previous elements (in increasing order) will be placed before it, and all further elements
     * will be placed after it.</p>
     *
     * <p>Аfter this reordering we have a guarantee, that for any <tt>m=percentileIndexes[...]</tt> and
     * for any <tt>i, j</tt>, that <tt>0 &le; i &lt; m</tt>, <tt>m &lt; j &lt; numberOfElements</tt>,
     * the following checks will return <tt>false</tt>:<pre>
     *     comparator.less(j, m)
     *     comparator.less(m, i)
     * </pre>
     *
     * <p>Reordering is based on movement of elements of the array with help of <tt>exchanger</tt>
     * object, which must provide necessary access to the data.
     *
     * <p>Note: list of indexes <tt>percentileIndexes</tt> <b>must be sorted</b> in increasing order.
     * You can provide this, for example, by simple call of standard Java sorting method
     * <tt>Arrays.sort(percentileIndexes)</tt> before calling this method.
     *
     * <p>Note: if some exception occurs while calling <tt>comparator</tt> or <tt>exchanger</tt> methods,
     * the array stays shuffled ("partially" sorted).
     * (The typical example is <tt>ClassCastException</tt> when the comparator tries to cast some objects
     * to <tt>java.lang.Comparable</tt> type.)
     * In other words, this method <b>is non-atomic regarding this failure</b>.
     *
     * @param numberOfElements  number of elements in the array.
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param comparator        comparator for checking order of elements.
     * @param exchanger         exchanger for exchanging reordered elements.
     * @throws NullPointerException     if <tt>percentileIndexes</tt>, <tt>comparator</tt> or <tt>exchanger</tt>
     *                                  argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>numberOfElements&le;0</tt>,
     *                                  or if <tt>percentileIndexes</tt> array is empty,
     *                                  or if it contains indexes outside <tt>0..numberOfElements-1</tt> range,
     *                                  or if it is not sorted in increasing order.
     * @throws ClassCastException       may be thrown while calling methods of <tt>comparator</tt> or
     *                                  <tt>exchanger</tt> during the sorting, if the types of the array elements
     *                                  are not appropriate (it is only a typical example of exception:
     *                                  those methods may throw another run-time exceptions).
     */
    public void select(
            final long numberOfElements,
            long[] percentileIndexes,
            ArrayComparator comparator,
            ArrayExchanger exchanger) {
        checkPercentileIndexes(percentileIndexes, numberOfElements);
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1, numberOfElements,
                comparator, exchanger);
    }

    /**
     * Equivalent of
     * <pre>
     *     {@link #select(long, long[], ArrayComparator, ArrayExchanger)
     * select}(numberOfElements, percentileIndexes, comparator, exchanger)
     * </pre>
     * call, where <tt>percentileIndexes[k]={@link #percentileIndex(double, long)
     * percentileIndex}(percentileLevels[k], numberOfElements)</tt>
     * for every <tt>k=0,...,percentileLevels.length-1</tt>.</p>
     *
     * <p>For example, <tt>percentileLevels={0.0, 0.5, 1.0}</tt> requests to find the minimum, median and maximum
     * and place them to positions <tt>0</tt>,
     * <tt>{@link #percentileIndex(double, long) percentileIndex}(0.5, numberOfElements)</tt> and
     * <tt>numberOfElements-1</tt>.
     *
     * <p>Note: list of levels <tt>percentileLevels</tt> <b>must be sorted</b> in increasing order.
     * You can provide this, for example, but simple call of standard Java sorting method
     * <tt>Arrays.sort(percentileLevels)</tt> before calling this method.
     *
     * @param numberOfElements number of elements in the array.
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param comparator       comparator for checking order of elements.
     * @param exchanger        exchanger for exchanging sorted elements.
     * @throws NullPointerException     if <tt>percentileLevels</tt>, <tt>comparator</tt> or <tt>exchanger</tt>
     *                                  argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>numberOfElements&le;0</tt>, or
     *                                  if <tt>percentileLevels</tt> array is empty,
     *                                  or if it contains elements outside
     *                                  0..1 range or <tt>NaN</tt>,
     *                                  or if it is not sorted in increasing order.
     * @throws ClassCastException       may be thrown while calling methods of <tt>comparator</tt> or
     *                                  <tt>exchanger</tt> during the sorting, if the types of the array elements
     *                                  are not appropriate (it is only a typical example of exception:
     *                                  those methods may throw another run-time exceptions).
     */
    public void select(
            final long numberOfElements,
            double[] percentileLevels,
            ArrayComparator comparator,
            ArrayExchanger exchanger) {
        if (numberOfElements <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + numberOfElements);
        }
        checkPercentileLevels(percentileLevels);
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1, numberOfElements,
                comparator, exchanger);
    }

    /**
     * The version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the single element with the required index.
     * Unlike the previous method, allows to select element in a part of the array <tt>from &le; index &lt; to</tt>.
     *
     * <p>After this reordering we have a guarantee, that for any <tt>i, j</tt>, that
     * <tt>from &le; i &lt; requiredIndex</tt>, <tt>requiredIndex &lt; j &lt; to</tt>,
     * the following checks will return <tt>false</tt>:<pre>
     *     comparator.less(j, m)
     *     comparator.less(m, i)
     * </pre>
     * <p>Elements with indexes <tt>&lt;from</tt> and <tt>&ge;to</tt> are not analysed.
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param comparator    comparator for checking order of elements.
     * @param exchanger     exchanger for exchanging reordered elements.
     * @throws NullPointerException     if <tt>comparator</tt> or <tt>exchanger</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     * @throws ClassCastException       may be thrown while calling methods of <tt>comparator</tt> or
     *                                  <tt>exchanger</tt> during the sorting, if the types of the array elements
     *                                  are not appropriate (it is only a typical example of exception:
     *                                  those methods may throw another run-time exceptions).
     */
    public void select(
            final long from,
            final long to,
            final long requiredIndex,
            ArrayComparator comparator,
            ArrayExchanger exchanger) {
        if (comparator == null) {
            throw new NullPointerException("Null comparator");
        }
        if (exchanger == null) {
            throw new NullPointerException("Null exchanger");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        long left = from;
        long right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        for (; ; ) {
            if (requiredIndex == left) {
                selectMin(left, right, comparator, exchanger);
                return;
            }
            if (requiredIndex == right) {
                selectMax(left, right, comparator, exchanger);
                return;
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            if (right - left < THRESHOLD) {
                sortLittleArray(left, right, comparator, exchanger);
                break;
                // Now
                //     data[left..requiredIndex] <= data[requiredIndex],
                //     data[requiredIndex..right] >= data[requiredIndex]
            }
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            long base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            if (comparator.less(base, left)) {
                exchanger.swap(base, left);
            }
            if (comparator.less(right, base)) {
                exchanger.swap(right, base);
                if (comparator.less(base, left)) {
                    exchanger.swap(base, left);
                }
            }

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            exchanger.swap(left + 1, base);
            base = left + 1;

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            long i = left + 1;
            long j = right;
            for (; ; ) {
                do
                    ++i;
                while (comparator.less(i, base));
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while (comparator.less(base, j));
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                exchanger.swap(i, j);
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j

            exchanger.swap(j, base);
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return;
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }

    /*Repeat() byte    ==> char,,short,,int,,long,,float,,double;;
               (\s+)\& 0xFF    ==> ,,$1& 0xFFFF,, ,,...;;
               (\([\w\[\]]+\))\s<\s(\([\w\[\]]+\)) ==>
                   $1 < $2,,$1 < $2,,$1 < $2,,$1 < $2,,Float.compare($1, $2) < 0,,Double.compare($1, $2) < 0;;
               (<p>Note that.*?\<\/p\>\s*\*\s*\*\s) ==> ,,$1,, ,, ,,
               <p>Note that elements of <tt>float[]</tt> array are compared by <tt>Float.compare(float, float)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other float values
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     *
     ,,
                <p>Note that elements of <tt>double[]</tt> array are compared by <tt>Double.compare(double, double)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other double alues
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     *;;
               \*(\@param (?:from|percentile)) ==> * $1,,...
     */

    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>byte[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFF</tt> and <tt>array[j] & 0xFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, byte[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>byte[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFF</tt> and <tt>array[j] & 0xFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, byte[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>byte[]</tt> array.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFF</tt> and <tt>array[j] & 0xFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public byte select(
            final int from,
            final int to,
            final int requiredIndex,
            byte[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                byte a = array[left];
                byte b = array[base];
                final byte c = array[right];
                // Sorting a, b, c
                if ((b & 0xFF) < (a & 0xFF)) {
                    a = b;
                    b = array[left];
                }
                if ((c & 0xFF) < (b & 0xFF)) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if ((b & 0xFF) < (a & 0xFF)) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                byte a = array[left];
                byte b = array[afterLeft];
                byte c = array[beforeRight];
                byte d = array[right];
                // Sorting a, b, c, d
                if ((b & 0xFF) < (a & 0xFF)) {
                    a = b;
                    b = array[left];
                }
                if ((d & 0xFF) < (c & 0xFF)) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if ((a & 0xFF) < (c & 0xFF)) {
                    // a..b, then c..d
                    array[left] = a;
                    if ((b & 0xFF) < (d & 0xFF)) {
                        array[right] = d;
                        if ((b & 0xFF) < (c & 0xFF)) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if ((d & 0xFF) < (b & 0xFF)) {
                        array[right] = b;
                        if ((d & 0xFF) < (a & 0xFF)) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            byte a = array[left];
            byte c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            byte b = array[base];
            // Sorting a, b, c
            if ((b & 0xFF) < (a & 0xFF)) {
                a = b;
                b = array[left];
            }
            if ((c & 0xFF) < (b & 0xFF)) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if ((b & 0xFF) < (a & 0xFF)) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            byte tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while ((array[i] & 0xFF) < (b & 0xFF));
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while ((b & 0xFF) < (array[j] & 0xFF));
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>char[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, char[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>char[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, char[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>char[]</tt> array.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public char select(
            final int from,
            final int to,
            final int requiredIndex,
            char[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                char a = array[left];
                char b = array[base];
                final char c = array[right];
                // Sorting a, b, c
                if ((b) < (a)) {
                    a = b;
                    b = array[left];
                }
                if ((c) < (b)) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if ((b) < (a)) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                char a = array[left];
                char b = array[afterLeft];
                char c = array[beforeRight];
                char d = array[right];
                // Sorting a, b, c, d
                if ((b) < (a)) {
                    a = b;
                    b = array[left];
                }
                if ((d) < (c)) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if ((a) < (c)) {
                    // a..b, then c..d
                    array[left] = a;
                    if ((b) < (d)) {
                        array[right] = d;
                        if ((b) < (c)) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if ((d) < (b)) {
                        array[right] = b;
                        if ((d) < (a)) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            char a = array[left];
            char c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            char b = array[base];
            // Sorting a, b, c
            if ((b) < (a)) {
                a = b;
                b = array[left];
            }
            if ((c) < (b)) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if ((b) < (a)) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            char tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while ((array[i]) < (b));
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while ((b) < (array[j]));
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }


    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>short[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFFFF</tt> and <tt>array[j] & 0xFFFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, short[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>short[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFFFF</tt> and <tt>array[j] & 0xFFFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, short[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>short[]</tt> array.</p>
     *
     * <p>Note that the elements of this array are supposed to be <b>unsigned</b>:
     * we always compare <tt>array[i] & 0xFFFF</tt> and <tt>array[j] & 0xFFFF</tt>
     * instead of simple <tt>array[i]</tt> and <tt>array[j]</tt>.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public short select(
            final int from,
            final int to,
            final int requiredIndex,
            short[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                short a = array[left];
                short b = array[base];
                final short c = array[right];
                // Sorting a, b, c
                if ((b & 0xFFFF) < (a & 0xFFFF)) {
                    a = b;
                    b = array[left];
                }
                if ((c & 0xFFFF) < (b & 0xFFFF)) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if ((b & 0xFFFF) < (a & 0xFFFF)) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                short a = array[left];
                short b = array[afterLeft];
                short c = array[beforeRight];
                short d = array[right];
                // Sorting a, b, c, d
                if ((b & 0xFFFF) < (a & 0xFFFF)) {
                    a = b;
                    b = array[left];
                }
                if ((d & 0xFFFF) < (c & 0xFFFF)) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if ((a & 0xFFFF) < (c & 0xFFFF)) {
                    // a..b, then c..d
                    array[left] = a;
                    if ((b & 0xFFFF) < (d & 0xFFFF)) {
                        array[right] = d;
                        if ((b & 0xFFFF) < (c & 0xFFFF)) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if ((d & 0xFFFF) < (b & 0xFFFF)) {
                        array[right] = b;
                        if ((d & 0xFFFF) < (a & 0xFFFF)) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            short a = array[left];
            short c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            short b = array[base];
            // Sorting a, b, c
            if ((b & 0xFFFF) < (a & 0xFFFF)) {
                a = b;
                b = array[left];
            }
            if ((c & 0xFFFF) < (b & 0xFFFF)) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if ((b & 0xFFFF) < (a & 0xFFFF)) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            short tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while ((array[i] & 0xFFFF) < (b & 0xFFFF));
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while ((b & 0xFFFF) < (array[j] & 0xFFFF));
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }


    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>int[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, int[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>int[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, int[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>int[]</tt> array.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public int select(
            final int from,
            final int to,
            final int requiredIndex,
            int[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                int a = array[left];
                int b = array[base];
                final int c = array[right];
                // Sorting a, b, c
                if ((b) < (a)) {
                    a = b;
                    b = array[left];
                }
                if ((c) < (b)) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if ((b) < (a)) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                int a = array[left];
                int b = array[afterLeft];
                int c = array[beforeRight];
                int d = array[right];
                // Sorting a, b, c, d
                if ((b) < (a)) {
                    a = b;
                    b = array[left];
                }
                if ((d) < (c)) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if ((a) < (c)) {
                    // a..b, then c..d
                    array[left] = a;
                    if ((b) < (d)) {
                        array[right] = d;
                        if ((b) < (c)) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if ((d) < (b)) {
                        array[right] = b;
                        if ((d) < (a)) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            int a = array[left];
            int c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            int b = array[base];
            // Sorting a, b, c
            if ((b) < (a)) {
                a = b;
                b = array[left];
            }
            if ((c) < (b)) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if ((b) < (a)) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            int tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while ((array[i]) < (b));
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while ((b) < (array[j]));
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }


    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>long[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, long[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>long[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, long[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>long[]</tt> array.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public long select(
            final int from,
            final int to,
            final int requiredIndex,
            long[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                long a = array[left];
                long b = array[base];
                final long c = array[right];
                // Sorting a, b, c
                if ((b) < (a)) {
                    a = b;
                    b = array[left];
                }
                if ((c) < (b)) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if ((b) < (a)) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                long a = array[left];
                long b = array[afterLeft];
                long c = array[beforeRight];
                long d = array[right];
                // Sorting a, b, c, d
                if ((b) < (a)) {
                    a = b;
                    b = array[left];
                }
                if ((d) < (c)) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if ((a) < (c)) {
                    // a..b, then c..d
                    array[left] = a;
                    if ((b) < (d)) {
                        array[right] = d;
                        if ((b) < (c)) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if ((d) < (b)) {
                        array[right] = b;
                        if ((d) < (a)) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            long a = array[left];
            long c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            long b = array[base];
            // Sorting a, b, c
            if ((b) < (a)) {
                a = b;
                b = array[left];
            }
            if ((c) < (b)) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if ((b) < (a)) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            long tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while ((array[i]) < (b));
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while ((b) < (array[j]));
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }


    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>float[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * <p>Note that elements of <tt>float[]</tt> array are compared by <tt>Float.compare(float, float)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other float values
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, float[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>float[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * <p>Note that elements of <tt>float[]</tt> array are compared by <tt>Float.compare(float, float)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other float values
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, float[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>float[]</tt> array.</p>
     *
     * <p>Note that elements of <tt>float[]</tt> array are compared by <tt>Float.compare(float, float)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other float values
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public float select(
            final int from,
            final int to,
            final int requiredIndex,
            float[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                float a = array[left];
                float b = array[base];
                final float c = array[right];
                // Sorting a, b, c
                if (Float.compare((b), (a)) < 0) {
                    a = b;
                    b = array[left];
                }
                if (Float.compare((c), (b)) < 0) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if (Float.compare((b), (a)) < 0) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                float a = array[left];
                float b = array[afterLeft];
                float c = array[beforeRight];
                float d = array[right];
                // Sorting a, b, c, d
                if (Float.compare((b), (a)) < 0) {
                    a = b;
                    b = array[left];
                }
                if (Float.compare((d), (c)) < 0) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if (Float.compare((a), (c)) < 0) {
                    // a..b, then c..d
                    array[left] = a;
                    if (Float.compare((b), (d)) < 0) {
                        array[right] = d;
                        if (Float.compare((b), (c)) < 0) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if (Float.compare((d), (b)) < 0) {
                        array[right] = b;
                        if (Float.compare((d), (a)) < 0) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            float a = array[left];
            float c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            float b = array[base];
            // Sorting a, b, c
            if (Float.compare((b), (a)) < 0) {
                a = b;
                b = array[left];
            }
            if (Float.compare((c), (b)) < 0) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if (Float.compare((b), (a)) < 0) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            float tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while (Float.compare((array[i]), (b)) < 0);
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while (Float.compare((b), (array[j])) < 0);
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }


    /**
     * Optimized version of {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>double[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, long[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileIndexes</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileIndexes(int[], int)} method.</p>
     *
     * <p>Note that elements of <tt>double[]</tt> array are compared by <tt>Double.compare(double, double)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other double alues
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @param array             data array.
     * @param length            number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileIndexes</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(int[] percentileIndexes, double[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileIndexes, 0, percentileIndexes.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method for selecting
     * the element from first <tt>length</tt> elements of <tt>double[]</tt> array.</p>
     *
     * <p>Note: unlike {@link #select(long, double[], ArrayComparator, ArrayExchanger)}, this method
     * does not check elements of <tt>percentileLevels</tt>, that they are correct and correctly sorted.
     * If they are incorrect, the results will be undefined. You can check them yourself by
     * {@link #checkPercentileLevels(double[])} method.</p>
     *
     * <p>Note that elements of <tt>double[]</tt> array are compared by <tt>Double.compare(double, double)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other double alues
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @param array            data array.
     * @param length           number of elements: only elements <tt>array[0..length-1</tt> are analysed.
     * @throws NullPointerException     if <tt>percentileLevels</tt> or <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&le;0</tt>, or <tt>length&gt;array.length</tt>.
     */
    public void select(double[] percentileLevels, double[] array, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + length);
        }
        if (length > array.length) {
            throw new IllegalArgumentException("length = " + length + " > array.length = " + array.length);
        }
        selectSomePercentiles(
                percentileLevels, 0, percentileLevels.length - 1,
                length, array);
    }

    /**
     * Optimized version of {@link #select(long, long, long, ArrayComparator, ArrayExchanger)} method for selecting
     * the element from <tt>double[]</tt> array.</p>
     *
     * <p>Note that elements of <tt>double[]</tt> array are compared by <tt>Double.compare(double, double)</tt>
     * method. So, <tt>NaN</tt> is considered to be equal to itself and greater than all other double alues
     * (including <tt>POSITIVE_INFINITY</tt>), and <tt>0.0 </tt>is considered  be greater than <tt>-0.0</tt>.</p>
     *
     * @param from          index of the first analysed element of some data array, inclusive.
     * @param to            index of the last analysed element of some data array, exclusive.
     * @param requiredIndex index inside the array, that should be placed to correct place in increasing order.
     * @param array         data array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>, or
     *                                  or if <tt>requiredIndex</tt> is outside <tt>from..to-1</tt> range.
     */
    public double select(
            final int from,
            final int to,
            final int requiredIndex,
            double[] array) {
        if (array == null) {
            throw new NullPointerException("Null array");
        }
        if (from < 0 || from >= to) {
            throw new IllegalArgumentException("Illegal from (" + from + ") or to (" + to
                    + ") arguments: must be 0 <= from < to");
        }
        int left = from;
        int right = to - 1;
        if (requiredIndex < left || requiredIndex > right) {
            throw new IllegalArgumentException("Index " + requiredIndex + " is out of range " + left + ".." + right);
        }
        // - measuring at real data shows that it is better to check min/max only for ALL array, not for sub-arrays
        for (; ; ) {
            if (requiredIndex == left) {
                return selectMin(left, right, array);
            }
            if (requiredIndex == right) {
                return selectMax(left, right, array);
            }
            assert requiredIndex > left;
            assert requiredIndex < right;
            final int difference = right - left;
            if (difference == 2) {
                // 3 elements
                final int base = left + 1;
                double a = array[left];
                double b = array[base];
                final double c = array[right];
                // Sorting a, b, c
                if (Double.compare((b), (a)) < 0) {
                    a = b;
                    b = array[left];
                }
                if (Double.compare((c), (b)) < 0) {
                    array[right] = b;
                    b = c;
                    // since this moment, c is not 3rd element
                    if (Double.compare((b), (a)) < 0) {
                        b = a;
                        a = c;
                    }
                }
                array[left] = a;
                array[base] = b;
                return array[requiredIndex];

            } else if (difference == 3) {
                // 4 elements
                final int afterLeft = left + 1;
                final int beforeRight = right - 1;
                double a = array[left];
                double b = array[afterLeft];
                double c = array[beforeRight];
                double d = array[right];
                // Sorting a, b, c, d
                if (Double.compare((b), (a)) < 0) {
                    a = b;
                    b = array[left];
                }
                if (Double.compare((d), (c)) < 0) {
                    d = c;
                    c = array[right];
                }
                // Now a <= b, c <= d
                if (Double.compare((a), (c)) < 0) {
                    // a..b, then c..d
                    array[left] = a;
                    if (Double.compare((b), (d)) < 0) {
                        array[right] = d;
                        if (Double.compare((b), (c)) < 0) {
                            array[afterLeft] = b;
                            array[beforeRight] = c;
                        } else {
                            array[afterLeft] = c;
                            array[beforeRight] = b;
                        }
                    } else {
                        // a <= c <= d <= b
                        array[afterLeft] = c;
                        array[beforeRight] = d;
                        array[right] = b;
                    }
                } else {
                    // c..d, then a..b
                    array[left] = c;
                    if (Double.compare((d), (b)) < 0) {
                        array[right] = b;
                        if (Double.compare((d), (a)) < 0) {
                            array[afterLeft] = d;
                            array[beforeRight] = a;
                        } else {
                            array[afterLeft] = a;
                            array[beforeRight] = d;
                        }
                    } else {
                        // c <= a <= b <= d
                        array[afterLeft] = a;
                        array[beforeRight] = b;
                        array[right] = d;
                    }
                }
                return array[requiredIndex];
            }

            // Switch above is better than common code below:
            // if (right - left < THRESHOLD) {
            //    sortLittleArray(left, right, array);
            //    return array[requiredIndex];
            // }

            double a = array[left];
            double c = array[right];
            // Sorting 3 elements to find a median from 3: left, (left+right)/2, right:
            int base = (left + right) >>> 1;
            // ">>>" will be correct even in the case of overflow
            double b = array[base];
            // Sorting a, b, c
            if (Double.compare((b), (a)) < 0) {
                a = b;
                b = array[left];
            }
            if (Double.compare((c), (b)) < 0) {
                array[right] = b;
                b = c;
                // since this moment, c is not 3rd element
                if (Double.compare((b), (a)) < 0) {
                    b = a;
                    a = c;
                }
            }
            array[left] = a;
            // array[base] = b; - virtually (really we can skip this operator)

            // Now data[left] <= data[base] <= data[right] (in other words, base is a median)
            // moving the base at the new position left+1
            double tmp = array[left + 1];
            array[left + 1] = b;
            array[base] = tmp;
            base = left + 1;
            // assert b == array[base]; // - base=left+1 and array[left+1] = b
            // NOTE: we must not actually perform assert operator, because for float/double types
            // it will work incorrectly for NaN value (NaN != NaN in Java)

            // Reordering elements left+2..right-1 so that, for some K,
            //     data[left+2..K] <= data[base],
            //     data[K+1..right-1] >= data[base]
            int i = left + 1;
            int j = right;
            for (; ; ) {
                do
                    ++i;
                while (Double.compare((array[i]), (b)) < 0);
                // Now
                //     data[left+2..i-1] <= data[base]
                //         (= data[base] for left+1,
                //         <= data[base] for left+2 and exchanged indexes,
                //         < data[base] for others),
                //     data[i] >= data[base],
                //     i <= j
                do
                    --j;
                while (Double.compare((b), (array[j])) < 0);
                // Now
                //     data[j] <= data[base],
                //     data[j+1..right-1] >= data[base],
                //     i <= j+1
                if (i >= j) {
                    break;
                }
                a = array[i];
                array[i] = array[j];
                array[j] = a;
                // Now
                //     data[left+1..i] <= data[base],
                //     data[j..right-1] >= data[base],
                //     i < j
            }
            // Now
            //     data[left+2..i-1] <= data[base],
            //     data[j] <= data[base],
            //     data[j+1..right-1] >= data[base],
            //     i >= j,
            // so
            //     data[left+2..j] <= data[base].
            // It means that elements are reordered and we can assign K=j
            array[base] = array[j];
            array[j] = b;
            // Now
            //     data[left..j-1] <= data[base],
            //     data[j] = data[base},
            //     data[j+1..right] >= data[base]
            assert left <= j;
            assert j <= right;
            if (requiredIndex == j) {
                return array[requiredIndex];
            } else if (requiredIndex < j) {
                right = j - 1;
            } else {
                left = j + 1;
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns index of the percentile with the given level in the array with the given number of elements.
     * The result of this method is
     * <pre>
     *     Math.round(percentileLevel * (numberOfElements - 1))
     * </pre>
     *
     * @param percentileLevel  percentile level.
     * @param numberOfElements number of elements in the array.
     * @return corresponding index of the element in the increasing order.
     */
    public static long percentileIndex(double percentileLevel, long numberOfElements) {
        return Math.round(percentileLevel * (numberOfElements - 1));
    }

    /**
     * Returns index of the percentile with the given level in the array with the given number of elements.
     * The result of this method is
     * <pre>
     *     (int) Math.round(percentileLevel * (numberOfElements - 1))
     * </pre>
     *
     * @param percentileLevel  percentile level.
     * @param numberOfElements number of elements in the array.
     * @return corresponding index of the element in the increasing order.
     */
    public static int percentileIndex(double percentileLevel, int numberOfElements) {
        return (int) Math.round(percentileLevel * (numberOfElements - 1));
    }

    /**
     * Checks whether the given percentile indexes are correct for passing to
     * {@link #select(long, long[], ArrayComparator, ArrayExchanger)} method.
     * Throws an exception if they are incorrect.
     *
     * @param numberOfElements  number of elements in the array.
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @throws IllegalArgumentException if <tt>numberOfElements&le;0</tt>,
     *                                  or if <tt>percentileIndexes</tt> array is empty,
     *                                  or if it contains indexes outside <tt>0..numberOfElements-1</tt> range,
     *                                  or if it is not sorted in increasing order.
     */
    public static void checkPercentileIndexes(long[] percentileIndexes, long numberOfElements) {
        if (numberOfElements <= 0) {
            throw new IllegalArgumentException("Zero or negative number of elements = " + numberOfElements);
        }
        if (percentileIndexes == null) {
            throw new NullPointerException("Null percentile indexes");
        }
        for (int k = 0; k < percentileIndexes.length; k++) {
            if (percentileIndexes[k] < 0 || percentileIndexes[k] >= numberOfElements) {
                throw new IllegalArgumentException("Illegal percentile index #" + k + " = " + percentileIndexes[k]
                        + ": out of range 0.." + (numberOfElements - 1));
            }
            if (k > 0 && percentileIndexes[k] < percentileIndexes[k - 1]) {
                throw new IllegalArgumentException("Illegal percentile indexes order: index #"
                        + (k - 1) + " > index #" + k + " in array ("
                        + JArrays.toString(percentileIndexes, ", ", 1024) + ")");
            }
        }
    }

    /**
     * Checks whether the given percentile indexes are correct for passing to
     * {@link #select(int[], float[], int)} and similar methods.
     * Throws an exception if they are incorrect.
     *
     * @param numberOfElements  number of elements in the array.
     * @param percentileIndexes list of indexes inside the array, that should be placed to correct place
     *                          in increasing order.
     * @throws IllegalArgumentException if <tt>numberOfElements&le;0</tt>,
     *                                  or if <tt>percentileIndexes</tt> array is empty,
     *                                  or if it contains indexes outside <tt>0..numberOfElements-1</tt> range,
     *                                  or if it is not sorted in increasing order.
     */
    public static void checkPercentileIndexes(int[] percentileIndexes, int numberOfElements) {
        if (percentileIndexes == null) {
            throw new NullPointerException("Null percentile indexes");
        }
        for (int k = 0; k < percentileIndexes.length; k++) {
            if (percentileIndexes[k] < 0 || percentileIndexes[k] >= numberOfElements) {
                throw new IllegalArgumentException("Illegal percentile index #" + k + " = " + percentileIndexes[k]
                        + ": out of range 0.." + (numberOfElements - 1));
            }
            if (k > 0 && percentileIndexes[k] < percentileIndexes[k - 1]) {
                throw new IllegalArgumentException("Illegal percentile indexes order: index #"
                        + (k - 1) + " > index #" + k + " in array ("
                        + JArrays.toString(percentileIndexes, ", ", 1024) + ")");
            }
        }
    }

    /**
     * Checks whether the given percentile levels are correct for passing to
     * {@link #select(long, double[], ArrayComparator, ArrayExchanger)} method.
     * Throws an exception if they are incorrect.
     *
     * @param percentileLevels list of percentile levels: required indexes, divided by array length.
     * @throws IllegalArgumentException if <tt>percentileLevels</tt> array is empty,
     *                                  or if it contains elements outside
     *                                  0..1 range or <tt>NaN</tt>,
     *                                  or if it is not sorted in increasing order.
     */
    public static void checkPercentileLevels(double[] percentileLevels) {
        if (percentileLevels == null) {
            throw new NullPointerException("Null percentile levels");
        }
        if (percentileLevels.length == 0) {
            throw new IllegalArgumentException("No percentile levels");
        }
        for (int k = 0; k < percentileLevels.length; k++) {
            if (Double.isNaN(percentileLevels[k]) || percentileLevels[k] < 0.0 || percentileLevels[k] > 1.0) {
                throw new IllegalArgumentException("Illegal percentile level #" + k + " = " + percentileLevels[k]
                        + ": out of range 0..1");
            }
            if (k > 0 && percentileLevels[k] < percentileLevels[k - 1]) {
                throw new IllegalArgumentException("Illegal percentile levels order: level #"
                        + (k - 1) + " > level #" + k + " in array ("
                        + JArrays.toString(percentileLevels, ", ", 1024) + ")");
            }
        }
    }

    @Override
    public String toString() {
        return "QuickSelect algorithm";
    }

    /*Repeat() ArrayComparator\s+comparator,\s*ArrayExchanger\s+exchanger ==>
               byte[] array,,char[] array,,short[] array,,int[] array,,long[] array,,float[] array,,double[] array;;
               comparator,\s*exchanger                   ==> array,,...;;
               long\s+(base|left|right|numberOfElements) ==> int $1,,...;;
               long\[\] (percentileIndexes)              ==> int[] $1,,...
     */
    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final long numberOfElements,
            ArrayComparator comparator,
            ArrayExchanger exchanger) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final long base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final long left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final long right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, comparator, exchanger);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    comparator,
                    exchanger);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    comparator,
                    exchanger);
        }
    }

    private void selectSomePercentiles(
            long[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final long numberOfElements,
            ArrayComparator comparator,
            ArrayExchanger exchanger) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final long base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final long left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final long right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, comparator, exchanger);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    comparator,
                    exchanger);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    comparator,
                    exchanger);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            byte[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            byte[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }


    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            char[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            char[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }


    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            short[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            short[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }


    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            int[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            int[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }


    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            long[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            long[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }


    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            float[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            float[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }


    private void selectSomePercentiles(
            double[] percentileLevels,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            double[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndex(percentileLevels[basePercentile], numberOfElements);
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndex(percentileLevels[leftPercentile - 1], numberOfElements) + 1;
        final int right = rightPercentile == percentileLevels.length - 1 ?
                numberOfElements - 1 :
                percentileIndex(percentileLevels[rightPercentile + 1], numberOfElements) - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileLevels,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    private void selectSomePercentiles(
            int[] percentileIndexes,
            final int leftPercentile,
            final int rightPercentile,
            final int numberOfElements,
            double[] array) {
        // We suppose that the percentiles leftPercentile-1 and rightPercentile+1 are already selected:
        // no sense to search outside them
        final int basePercentile = (leftPercentile + rightPercentile) >>> 1;
        final int base = percentileIndexes[basePercentile];
        // Let's find the central percentile first, then all percentiles from the left and from the right
        final int left = leftPercentile == 0 ?
                0 :
                percentileIndexes[leftPercentile - 1] + 1;
        final int right = rightPercentile == percentileIndexes.length - 1 ?
                numberOfElements - 1 :
                percentileIndexes[rightPercentile + 1] - 1;
        if (base >= left && base <= right) {
            // - it can be not so when we have identical indexes
            select(left, right + 1, base, array);
        }
        if (basePercentile > leftPercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    leftPercentile,
                    basePercentile - 1,
                    numberOfElements,
                    array);
        }
        if (rightPercentile > basePercentile) {
            selectSomePercentiles(
                    percentileIndexes,
                    basePercentile + 1,
                    rightPercentile,
                    numberOfElements,
                    array);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private static void selectMin(long left, long right, ArrayComparator comparator, ArrayExchanger exchanger) {
        for (long i = left + 1; i <= right; i++) {
            if (comparator.less(i, left)) {
                exchanger.swap(left, i);
            }
        }
    }

    private static void selectMax(long left, long right, ArrayComparator comparator, ArrayExchanger exchanger) {
        for (long i = right - 1; i >= left; i--) {
            if (comparator.less(right, i)) {
                exchanger.swap(right, i);
            }
        }
    }

    private static void sortLittleArray(long left, long right, ArrayComparator comparator, ArrayExchanger exchanger) {
        for (long i = left + 1; i <= right; i++) {
            long j = i - 1;
            for (; j >= left && comparator.less(j + 1, j); j--) {
                exchanger.swap(j, j + 1);
            }
        }
    }

    /*Repeat() \(byte\)(\s*)   ==> ,, (short)$1,, ,, ,, ,, ;;
               byte            ==> char,,short,,int,,long,,float,,double;;
               (\s+)\& 0xFF    ==> ,,$1& 0xFFFF,, ,,...;;
               int\s(result|v) ==> char $1,,int $1,,int $1,,long $1,,float $1,,double $1;;
               (\([\w\[\]]+\))\s<\s(\([\w\[\]]+\)) ==>
                   $1 < $2,,$1 < $2,,$1 < $2,,$1 < $2,,Float.compare($1, $2) < 0,,Double.compare($1, $2) < 0;;
               (v|result)\s<\s(v|result) ==>
                   $1 < $2,,$1 < $2,,$1 < $2,,$1 < $2,,Float.compare($1, $2) < 0,,Double.compare($1, $2) < 0
     */
    private static byte selectMin(int left, int right, byte[] array) {
        int index = left;
        int result = array[left] & 0xFF;
        for (int i = left + 1; i <= right; i++) {
            int v = array[i] & 0xFF;
            if (v < result) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = (byte) result;
        }
        return (byte) result;
    }

    private static byte selectMax(int left, int right, byte[] array) {
        int index = right;
        int result = array[right] & 0xFF;
        for (int i = right - 1; i >= left; i--) {
            int v = array[i] & 0xFF;
            if (result < v) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = (byte) result;
        }
        return (byte) result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, byte[] array) {
        final int base = left + 1;
        byte a = array[left];
        byte b = array[base];
        final byte c = array[right];
        // Sorting a, b, c
        if ((b & 0xFF) < (a & 0xFF)) {
            a = b;
            b = array[left];
        }
        if ((c & 0xFF) < (b & 0xFF)) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if ((b & 0xFF) < (a & 0xFF)) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, byte[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        byte a = array[left];
        byte b = array[afterLeft];
        byte c = array[beforeRight];
        byte d = array[right];
        // Sorting a, b, c, d
        if ((b & 0xFF) < (a & 0xFF)) {
            a = b;
            b = array[left];
        }
        if ((d & 0xFF) < (c & 0xFF)) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if ((a & 0xFF) < (c & 0xFF)) {
            // a..b, then c..d
            array[left] = a;
            if ((b & 0xFF) < (d & 0xFF)) {
                array[right] = d;
                if ((b & 0xFF) < (c & 0xFF)) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if ((d & 0xFF) < (b & 0xFF)) {
                array[right] = b;
                if ((d & 0xFF) < (a & 0xFF)) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, byte[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            int v = array[i] & 0xFF;
            while (j >= left && v < (array[j] & 0xFF)) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = (byte) v;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static char selectMin(int left, int right, char[] array) {
        int index = left;
        char result = array[left];
        for (int i = left + 1; i <= right; i++) {
            char v = array[i];
            if (v < result) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = result;
        }
        return result;
    }

    private static char selectMax(int left, int right, char[] array) {
        int index = right;
        char result = array[right];
        for (int i = right - 1; i >= left; i--) {
            char v = array[i];
            if (result < v) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = result;
        }
        return result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, char[] array) {
        final int base = left + 1;
        char a = array[left];
        char b = array[base];
        final char c = array[right];
        // Sorting a, b, c
        if ((b) < (a)) {
            a = b;
            b = array[left];
        }
        if ((c) < (b)) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if ((b) < (a)) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, char[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        char a = array[left];
        char b = array[afterLeft];
        char c = array[beforeRight];
        char d = array[right];
        // Sorting a, b, c, d
        if ((b) < (a)) {
            a = b;
            b = array[left];
        }
        if ((d) < (c)) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if ((a) < (c)) {
            // a..b, then c..d
            array[left] = a;
            if ((b) < (d)) {
                array[right] = d;
                if ((b) < (c)) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if ((d) < (b)) {
                array[right] = b;
                if ((d) < (a)) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, char[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            char v = array[i];
            while (j >= left && v < (array[j])) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = v;
        }
    }


    private static short selectMin(int left, int right, short[] array) {
        int index = left;
        int result = array[left] & 0xFFFF;
        for (int i = left + 1; i <= right; i++) {
            int v = array[i] & 0xFFFF;
            if (v < result) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = (short) result;
        }
        return (short) result;
    }

    private static short selectMax(int left, int right, short[] array) {
        int index = right;
        int result = array[right] & 0xFFFF;
        for (int i = right - 1; i >= left; i--) {
            int v = array[i] & 0xFFFF;
            if (result < v) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = (short) result;
        }
        return (short) result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, short[] array) {
        final int base = left + 1;
        short a = array[left];
        short b = array[base];
        final short c = array[right];
        // Sorting a, b, c
        if ((b & 0xFFFF) < (a & 0xFFFF)) {
            a = b;
            b = array[left];
        }
        if ((c & 0xFFFF) < (b & 0xFFFF)) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if ((b & 0xFFFF) < (a & 0xFFFF)) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, short[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        short a = array[left];
        short b = array[afterLeft];
        short c = array[beforeRight];
        short d = array[right];
        // Sorting a, b, c, d
        if ((b & 0xFFFF) < (a & 0xFFFF)) {
            a = b;
            b = array[left];
        }
        if ((d & 0xFFFF) < (c & 0xFFFF)) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if ((a & 0xFFFF) < (c & 0xFFFF)) {
            // a..b, then c..d
            array[left] = a;
            if ((b & 0xFFFF) < (d & 0xFFFF)) {
                array[right] = d;
                if ((b & 0xFFFF) < (c & 0xFFFF)) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if ((d & 0xFFFF) < (b & 0xFFFF)) {
                array[right] = b;
                if ((d & 0xFFFF) < (a & 0xFFFF)) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, short[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            int v = array[i] & 0xFFFF;
            while (j >= left && v < (array[j] & 0xFFFF)) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = (short) v;
        }
    }


    private static int selectMin(int left, int right, int[] array) {
        int index = left;
        int result = array[left];
        for (int i = left + 1; i <= right; i++) {
            int v = array[i];
            if (v < result) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = result;
        }
        return result;
    }

    private static int selectMax(int left, int right, int[] array) {
        int index = right;
        int result = array[right];
        for (int i = right - 1; i >= left; i--) {
            int v = array[i];
            if (result < v) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = result;
        }
        return result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, int[] array) {
        final int base = left + 1;
        int a = array[left];
        int b = array[base];
        final int c = array[right];
        // Sorting a, b, c
        if ((b) < (a)) {
            a = b;
            b = array[left];
        }
        if ((c) < (b)) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if ((b) < (a)) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, int[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        int a = array[left];
        int b = array[afterLeft];
        int c = array[beforeRight];
        int d = array[right];
        // Sorting a, b, c, d
        if ((b) < (a)) {
            a = b;
            b = array[left];
        }
        if ((d) < (c)) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if ((a) < (c)) {
            // a..b, then c..d
            array[left] = a;
            if ((b) < (d)) {
                array[right] = d;
                if ((b) < (c)) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if ((d) < (b)) {
                array[right] = b;
                if ((d) < (a)) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, int[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            int v = array[i];
            while (j >= left && v < (array[j])) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = v;
        }
    }


    private static long selectMin(int left, int right, long[] array) {
        int index = left;
        long result = array[left];
        for (int i = left + 1; i <= right; i++) {
            long v = array[i];
            if (v < result) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = result;
        }
        return result;
    }

    private static long selectMax(int left, int right, long[] array) {
        int index = right;
        long result = array[right];
        for (int i = right - 1; i >= left; i--) {
            long v = array[i];
            if (result < v) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = result;
        }
        return result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, long[] array) {
        final int base = left + 1;
        long a = array[left];
        long b = array[base];
        final long c = array[right];
        // Sorting a, b, c
        if ((b) < (a)) {
            a = b;
            b = array[left];
        }
        if ((c) < (b)) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if ((b) < (a)) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, long[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        long a = array[left];
        long b = array[afterLeft];
        long c = array[beforeRight];
        long d = array[right];
        // Sorting a, b, c, d
        if ((b) < (a)) {
            a = b;
            b = array[left];
        }
        if ((d) < (c)) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if ((a) < (c)) {
            // a..b, then c..d
            array[left] = a;
            if ((b) < (d)) {
                array[right] = d;
                if ((b) < (c)) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if ((d) < (b)) {
                array[right] = b;
                if ((d) < (a)) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, long[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            long v = array[i];
            while (j >= left && v < (array[j])) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = v;
        }
    }


    private static float selectMin(int left, int right, float[] array) {
        int index = left;
        float result = array[left];
        for (int i = left + 1; i <= right; i++) {
            float v = array[i];
            if (Float.compare(v, result) < 0) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = result;
        }
        return result;
    }

    private static float selectMax(int left, int right, float[] array) {
        int index = right;
        float result = array[right];
        for (int i = right - 1; i >= left; i--) {
            float v = array[i];
            if (Float.compare(result, v) < 0) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = result;
        }
        return result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, float[] array) {
        final int base = left + 1;
        float a = array[left];
        float b = array[base];
        final float c = array[right];
        // Sorting a, b, c
        if (Float.compare((b), (a)) < 0) {
            a = b;
            b = array[left];
        }
        if (Float.compare((c), (b)) < 0) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if (Float.compare((b), (a)) < 0) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, float[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        float a = array[left];
        float b = array[afterLeft];
        float c = array[beforeRight];
        float d = array[right];
        // Sorting a, b, c, d
        if (Float.compare((b), (a)) < 0) {
            a = b;
            b = array[left];
        }
        if (Float.compare((d), (c)) < 0) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if (Float.compare((a), (c)) < 0) {
            // a..b, then c..d
            array[left] = a;
            if (Float.compare((b), (d)) < 0) {
                array[right] = d;
                if (Float.compare((b), (c)) < 0) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if (Float.compare((d), (b)) < 0) {
                array[right] = b;
                if (Float.compare((d), (a)) < 0) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, float[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            float v = array[i];
            while (j >= left && v < (array[j])) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = v;
        }
    }


    private static double selectMin(int left, int right, double[] array) {
        int index = left;
        double result = array[left];
        for (int i = left + 1; i <= right; i++) {
            double v = array[i];
            if (Double.compare(v, result) < 0) {
                result = v;
                index = i;
            }
        }
        if (index != left) {
            array[index] = array[left];
            array[left] = result;
        }
        return result;
    }

    private static double selectMax(int left, int right, double[] array) {
        int index = right;
        double result = array[right];
        for (int i = right - 1; i >= left; i--) {
            double v = array[i];
            if (Double.compare(result, v) < 0) {
                // IMPORTANT: use this comparison for correct work of Repeater
                result = v;
                index = i;
            }
        }
        if (index != right) {
            array[index] = array[right];
            array[right] = result;
        }
        return result;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray3(int left, int right, double[] array) {
        final int base = left + 1;
        double a = array[left];
        double b = array[base];
        final double c = array[right];
        // Sorting a, b, c
        if (Double.compare((b), (a)) < 0) {
            a = b;
            b = array[left];
        }
        if (Double.compare((c), (b)) < 0) {
            array[right] = b;
            b = c;
            // since this moment, c is not 3rd element
            if (Double.compare((b), (a)) < 0) {
                b = a;
                a = c;
            }
        }
        array[left] = a;
        array[base] = b;
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortArray4(int left, int right, double[] array) {
        final int afterLeft = left + 1;
        final int beforeRight = right - 1;
        double a = array[left];
        double b = array[afterLeft];
        double c = array[beforeRight];
        double d = array[right];
        // Sorting a, b, c, d
        if (Double.compare((b), (a)) < 0) {
            a = b;
            b = array[left];
        }
        if (Double.compare((d), (c)) < 0) {
            d = c;
            c = array[right];
        }
        // Now a <= b, c <= d
        if (Double.compare((a), (c)) < 0) {
            // a..b, then c..d
            array[left] = a;
            if (Double.compare((b), (d)) < 0) {
                array[right] = d;
                if (Double.compare((b), (c)) < 0) {
                    array[afterLeft] = b;
                    array[beforeRight] = c;
                } else {
                    array[afterLeft] = c;
                    array[beforeRight] = b;
                }
            } else {
                // a <= c <= d <= b
                array[afterLeft] = c;
                array[beforeRight] = d;
                array[right] = b;
            }
        } else {
            // c..d, then a..b
            array[left] = c;
            if (Double.compare((d), (b)) < 0) {
                array[right] = b;
                if (Double.compare((d), (a)) < 0) {
                    array[afterLeft] = d;
                    array[beforeRight] = a;
                } else {
                    array[afterLeft] = a;
                    array[beforeRight] = d;
                }
            } else {
                // c <= a <= b <= d
                array[afterLeft] = a;
                array[beforeRight] = b;
                array[right] = d;
            }
        }
    }

    // Used in previous versions of the algorithm; not used now
    private static void sortLittleArray(int left, int right, double[] array) {
        switch (right - left) {
            case 3:
        }
        for (int i = left + 1; i <= right; i++) {
            int j = i - 1;
            double v = array[i];
            while (j >= left && v < (array[j])) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = v;
        }
    }

    /*Repeat.AutoGeneratedEnd*/
}
