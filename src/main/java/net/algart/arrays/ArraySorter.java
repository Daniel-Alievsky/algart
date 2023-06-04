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
 * <p>Sorting algorithms. This class provides several implementations for
 * Insertion, Shell and QuickSort algorithm.
 * (The QuickSort variant is the best choice almost always,
 * it is better than Shell algorithm in 1.5-2.5 times.)</p>
 *
 * <p>Standard Java sorting methods allows sorting only primitive arrays
 * or collections of objects, represented as usual instances of Java classes
 * (every object is a pointer and requires ~10-20 additional bytes of service memory).
 * Unlike that, {@link ArraySorter} class allows to sort any data arrays, represented in any way.
 * To do this, {@link ArraySorter} uses {@link ArrayComparator} and {@link ArrayExchanger}
 * interfaces to access all sorted elements.</p>
 *
 * <p>For example, this class can sort an array of points by x-coordinate
 * stored in a single Java array, as shown in the following example:</p>
 *
 * <blockquote><pre>
 * int n = 10;
 * final float[] xy = new float[2 * n];
 * for (int k = 0; k < n; k++) {
 * &#32;   xy[2 * k] = (float)Math.random();     // x-coordinate of the point #k
 * &#32;   xy[2 * k + 1] = (float)Math.random(); // y-coordinate of the point #k
 * }
 * Sorter.getQuickSortInstance().sort(0, n,
 * &#32;   new {@link ArrayComparator}() {
 * &#32;   public boolean less(long i, long j) {
 * &#32;       return xy[2 * (int)i] < xy[2 * (int)j];
 * &#32;   }
 * },
 * &#32;   new {@link ArrayExchanger}() {
 * &#32;   public void swap(long i, long j) {
 * &#32;       float temp = xy[2 * (int)i];
 * &#32;       xy[2 * (int)i] = xy[2 * (int)j];
 * &#32;       xy[2 * (int)j] = temp;
 * &#32;       temp = xy[2 * (int)i + 1];
 * &#32;       xy[2 * (int)i + 1] = xy[2 * (int)j + 1];
 * &#32;       xy[2 * (int)j + 1] = temp;
 * &#32;   }
 * });</pre></blockquote>
 *
 * <p>Any {@link UpdatableArray updatable AlgART array} already implements {@link ArrayExchanger} interface.
 * So, AlgART arrays can be sorted by implementing {@link ArrayComparator} interface only,
 * alike standard Java collections.
 * There is {@link Arrays#sort Arrays.sort} method performing such sorting
 * by {@link #getQuickSorter() QuickSort instance} of this class.
 *
 * <p>Also this class allows to sort <tt>int[]</tt> array of indexes of elements
 * and not move data at all. It is often useful for large sorted elements.</p>
 *
 * <p>Please note that the sorting algorithms, provided by this class, excepting
 * the slow Insertion algorithm, are not <i>stable</i>: equal elements may be
 * reordered as a result of the sort. If you need stable sorting some objects,
 * please use <tt>java.util</tt> package.
 *
 * <p>Unlike standard Java sorting algorithms, this class has no restriction 2<sup>31</sup>-1
 * for the length of sorted arrays.
 * This class allows to sort up to 2<sup>63</sup>&minus;1 elements.</p>
 *
 * @author Daniel Alievsky
 */

public abstract class ArraySorter {
    private ArraySorter() {
    }

    private static final ArraySorter INSERTION_SORTER = new InsertionSorter();
    private static final ArraySorter SHELL_SORTER = new ShellSorter();
    private static final ArraySorter QUICK_SORTER = new QuickSorter();

    /**
     * Returns an instance of this class that implements insertion sorting algorithm.
     * Do not use this algorithm for sorting large arrays - it will be very slow.
     *
     * @return implementation of insertion sorting algorithm.
     */
    public static ArraySorter getInsertionSorter() {
        return INSERTION_SORTER;
    }

    /**
     * Returns an instance of this class that implements Shell sorting algorithm.
     *
     * @return implementation of Shell sorting algorithm.
     */
    public static ArraySorter getShellSorter() {
        return SHELL_SORTER;
    }

    /**
     * Returns an instance of this class that implements QuickSort sorting algorithm.
     *
     * @return implementation of Quick-Sort sorting algorithm.
     */
    public static ArraySorter getQuickSorter() {
        return QUICK_SORTER;
    }

    /**
     * Returns <tt>true</tt> if the <tt>from..to-1</tt> range of <i>indexes</i> in some array is sorted.
     * It means that for any <tt>k</tt>, <tt>from &lt;= k &lt; to</tt>,
     * the following check returns <tt>false</tt>:<pre>
     *     comparator.less(indexes[k + 1], indexes[k])
     * </pre>
     *
     * @param indexes    indexes of elements in some data array.
     * @param from       index of the first checked element of <tt>indexes</tt> array, inclusive.
     * @param to         index of the last checked element of <tt>indexes</tt> array, exclusive.
     * @param comparator comparator for checking order.
     * @return <tt>true</tt> if the specified range of indexes is sorted.
     */
    public static boolean areIndexesSorted(int[] indexes, int from, int to, ArrayComparator comparator) {
        if (comparator == null)
            throw new NullPointerException("Null comparator argument");
        check(indexes, from, to);
        for (int k = from; k < to - 1; k++)
            if (comparator.less(indexes[k + 1], indexes[k]))
                return false;
        return true;
    }

    /**
     * Returns <tt>true</tt> if the <tt>from..to-1</tt> range of some array is sorted.
     * It means that for any <tt>k</tt>, <tt>from &lt;= k &lt; to</tt>,
     * the following check returns <tt>false</tt>:<pre>
     *     comparator.less(k + 1, k)
     * </pre>
     *
     * @param from       index of the first checked element of some data array, inclusive.
     * @param to         index of the last checked element of some data array, exclusive.
     * @param comparator comparator for checking order.
     * @return <tt>true</tt> if the specified range of the data array is sorted.
     * @throws NullPointerException if <tt>comparator</tt> argument is <tt>null</tt>.
     */
    public static boolean areSorted(long from, long to, ArrayComparator comparator) {
        if (comparator == null)
            throw new NullPointerException("Null comparator argument");
        check(from, to);
        for (long k = from; k < to - 1; k++)
            if (comparator.less(k + 1, k))
                return false;
        return true;
    }

    /**
     * Sorts <tt>indexes[from..to-1]</tt> in increasing order.
     * After sorting, for any <tt>i</tt>, <tt>j</tt>, <tt>from &lt;= i &lt; j &lt; to</tt>,
     * the following check will return <tt>false</tt>:<pre>
     *     comparator.less(indexes[j], indexes[i])
     * </pre>
     *
     * <p>Sorting is based on exchanging elements of the passed <tt>indexes</tt> array.
     *
     * <p>Note: if some exception occurs while calling <tt>comparator.less</tt> method,
     * the array stays shuffled ("partially" sorted).
     * (The typical example is <tt>ClassCastException</tt> when the comparator tries to cast some objects
     * to <tt>java.lang.Comparable</tt> type.)
     * In other words, this method <b>is non-atomic regarding this failure</b>.
     * Unlike this, sorting methods from <tt>java.util</tt>
     * package never modify the passed array or collection in a case of some exceptions.
     *
     * @param indexes    indexes of elements in some data array: this array will be sorted.
     * @param from       index of the first sorted element of <tt>indexes</tt> array, inclusive.
     * @param to         index of the last sorted element of <tt>indexes</tt> array, exclusive.
     * @param comparator comparator for checking order.
     * @throws NullPointerException     if <tt>indexes</tt> or <tt>comparator</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt>, <tt>from &lt; 0</tt>
     *                                  or <tt>to &gt; indexes.length</tt>
     * @throws ClassCastException       may be thrown while calling methods of <tt>comparator</tt>
     *                                  during the sorting, if the types of the array elements
     *                                  are not appropriate (it is only a typical example of exception:
     *                                  those methods may throw another run-time exceptions).
     */
    public abstract void sortIndexes(int[] indexes, int from, int to, ArrayComparator comparator);

    /**
     * Calls the previous {@link #sortIndexes(int[], int, int, ArrayComparator comparator)}
     * method with the same arguments.
     *
     * @param indexes    indexes of elements in some data array: this array will be sorted.
     * @param from       index of the first sorted element of <tt>indexes</tt> array, inclusive.
     * @param to         index of the last sorted element of <tt>indexes</tt> array, exclusive.
     * @param comparator comparator for checking order.
     * @throws NullPointerException     if <tt>indexes</tt> or <tt>comparator</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>from &gt; to</tt>, <tt>from &lt; 0</tt>
     *                                  or <tt>to &gt; indexes.length</tt>
     * @throws ClassCastException       may be thrown while calling methods of <tt>comparator</tt>
     *                                  during the sorting, if the types of the array elements
     *                                  are not appropriate (it is only a typical example of exception:
     *                                  those methods may throw another run-time exceptions).
     */
    public void sortIndexes(int[] indexes, int from, int to, ArrayComparator32 comparator) {
        sortIndexes(indexes, from, to, (ArrayComparator) comparator);
    }

    //Possible default implementation:
    //{
    //    if (indexes == null)
    //        throw new NullPointerException("Null indexes argument");
    //    if (comparator == null)
    //        throw new NullPointerException("Null comparator argument");
    //    sort(from, to,
    //        new ArrayComparator() {
    //            public boolean less(long i, long j) {
    //                return comparator.less(indexes[(int) i], indexes[(int) j]);
    //            }
    //        },
    //        new ArrayExchanger() {
    //            public void swap(long i, long j) {
    //                int temp = indexes[(int) i];
    //                indexes[(int) i] = indexes[(int) j];
    //                indexes[(int) j] = temp;
    //            }
    //        });
    //}

    /**
     * Sorts <tt>from..to-1</tt> fragment of some array in increasing order.
     * After sorting, for any <tt>i</tt>, <tt>j</tt>, <tt>from &lt;= i &lt; j &lt; to</tt>,
     * the following check will return <tt>false</tt>:<pre>
     *     comparator.less(j, i)
     * </pre>
     *
     * <p>Sorting is based on movement of elements of the sorted array with help of <tt>exchanger</tt>
     * object, which must provide necessary access to the sorted data.
     *
     * <p>Note: some sorting algorithms, implemented by this class, may require, that <tt>exchanger</tt>
     * must implement some additional interfaces, maybe a more powerful inheritor of
     * {@link ArrayExchanger} interface, for example, allowing not only exchanging, but also copying elements.
     * In such situation, this method throws <tt>UnsupportedOperationException</tt>, if <tt>exchanger</tt> argument
     * does not implement necessary interface. The implementations, returned by
     * {@link #getInsertionSorter()}, {@link #getShellSorter()}, {@link #getQuickSorter()}
     * (i.e. all implementation, available in the current version of this package),
     * do not require this: it is enough to implement simple {@link ArrayExchanger} interface to use them.
     * But, maybe, such algorithms will appear in future versions.
     * You can verify, does this sorter require implementing additional interfaces by <tt>exchanger</tt> object,
     * by {@link #isExchangingSorter()} method: it will return <tt>false</tt> in this case.
     *
     * <p>Note: if some exception occurs while calling <tt>comparator</tt> or <tt>exchanger</tt> methods,
     * the array stays shuffled ("partially" sorted).
     * (The typical example is <tt>ClassCastException</tt> when the comparator tries to cast some objects
     * to <tt>java.lang.Comparable</tt> type.)
     * In other words, this method <b>is non-atomic regarding this failure</b>.
     * Unlike this, sorting methods from <tt>java.util</tt>
     * package never modify the passed array or collection in a case of some exceptions.
     *
     * @param from       index of the first sorted element of some data array, inclusive.
     * @param to         index of the last sorted element of some data array, exclusive.
     * @param comparator comparator for checking order of elements.
     * @param exchanger  exchanger for exchanging sorted elements.
     * @throws NullPointerException          if <tt>comparator</tt> or <tt>exchanger</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>
     * @throws UnsupportedOperationException if {@link #isExchangingSorter()} returns <tt>false</tt> and
     *                                       the <tt>exchanger</tt> does not implement interfaces, necessary
     *                                       to perform sorting by this algorithm. Never occurs in the current
     *                                       version of this package.
     * @throws ClassCastException            may be thrown while calling methods of <tt>comparator</tt> or
     *                                       <tt>exchanger</tt> during the sorting, if the types of the array elements
     *                                       are not appropriate (it is only a typical example of exception:
     *                                       those methods may throw another run-time exceptions).
     */
    public abstract void sort(long from, long to, ArrayComparator comparator, ArrayExchanger exchanger);

    /**
     * Calls the previous {@link #sort(long, long, ArrayComparator, ArrayExchanger)} method with the same arguments.
     *
     * @param from       index of the first sorted element of some data array, inclusive.
     * @param to         index of the last sorted element of some data array, exclusive.
     * @param comparator comparator for checking order of elements.
     * @param exchanger  exchanger for exchanging sorted elements.
     * @throws NullPointerException          if <tt>comparator</tt> or <tt>exchanger</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if <tt>from &gt; to</tt> or <tt>from &lt; 0</tt>
     * @throws UnsupportedOperationException if {@link #isExchangingSorter()} returns <tt>false</tt> and
     *                                       the <tt>exchanger</tt> does not implement interfaces, necessary
     *                                       to perform sorting by this algorithm. Never occurs in the current
     *                                       version of this package.
     * @throws ClassCastException            may be thrown while calling methods of <tt>comparator</tt> or
     *                                       <tt>exchanger</tt> during the sorting, if the types of the array elements
     *                                       are not appropriate (it is only a typical example of exception:
     *                                       those methods may throw another run-time exceptions).
     */
    public void sort(int from, int to, ArrayComparator32 comparator, ArrayExchanger32 exchanger) {
        sort((long) from, (long) to, (ArrayComparator) comparator, (ArrayExchanger) exchanger);
    }

    /**
     * Returns <tt>true</tt>, if it is enough to implement the pure {@link ArrayExchanger} interface by
     * the <tt>exchanger</tt> argument of {@link #sort(long, long, ArrayComparator, ArrayExchanger)} method,
     * or <tt>false</tt>, if that method requires <tt>exchanger</tt> to implement something else.
     * If this method returns <tt>false</tt>, then it is possible that {@link #sort sort} method
     * will throw <tt>UnsupportedOperationException</tt> when its <tt>exchanger</tt> object does not
     * implement some additional interface, necessary for this sorting algorithm.
     *
     * <p>In the current version of the package, this method always returns <tt>true</tt>.
     * But this behaviour may change in future versions.
     *
     * @return whether {@link #sort sort} method works properly if its <tt>exchanger</tt> implements only
     * the simple {@link ArrayExchanger} interface.
     */
    public boolean isExchangingSorter() {
        return true;
    }

    static class InsertionSorter extends ArraySorter {
        public void sortIndexes(int[] indexes, int from, int to, ArrayComparator comparator) {
            if (comparator == null)
                throw new NullPointerException("Null comparator argument");
            check(indexes, from, to);
            insertionSortIndexesWithoutChecks(indexes, from, to, comparator);
        }

        public void sort(long from, long to, ArrayComparator comparator, ArrayExchanger exchanger) {
            if (comparator == null)
                throw new NullPointerException("Null comparator argument");
            if (exchanger == null)
                throw new NullPointerException("Null exchanger argument");
            check(from, to);
            insertionSortWithoutChecks(from, to, comparator, exchanger);
        }

        public String toString() {
            return "Insertion sorting algorithm";
        }
    }

    static void insertionSortIndexesWithoutChecks(int[] indexes, int from, int to,
                                                  ArrayComparator comparator) {
        for (int i = from + 1; i < to; i++) {
            int index = indexes[i];
            int j = i - 1;
            for (; j >= from && comparator.less(index, indexes[j]); j--)
                indexes[j + 1] = indexes[j];
            indexes[j + 1] = index;
        }
    }

    static void insertionSortWithoutChecks(long from, long to,
                                           ArrayComparator comparator, ArrayExchanger exchanger) {
        for (long i = from + 1; i < to; i++) {
            long j = i - 1;
            for (; j >= from && comparator.less(j + 1, j); j--)
                exchanger.swap(j, j + 1);
        }

    }

    static class ShellSorter extends ArraySorter {
        public void sortIndexes(int[] indexes, int from, int to, ArrayComparator comparator) {
            if (comparator == null)
                throw new NullPointerException("Null comparator argument");
            check(indexes, from, to);
            int step = 1;
            for (int maxStep = (to - 1 - from) / 9; step <= maxStep; step = 3 * step + 1) ;
            for (; step > 0; step /= 3) {
                for (int i = from + step; i < to; i++) {
                    int index = indexes[i];
                    int j = i - step;
                    for (; j >= from && comparator.less(index, indexes[j]); j -= step)
                        indexes[j + step] = indexes[j];
                    indexes[j + step] = index;
                }
            }
        }

        public void sort(long from, long to, ArrayComparator comparator, ArrayExchanger exchanger) {
            if (comparator == null)
                throw new NullPointerException("Null comparator argument");
            if (exchanger == null)
                throw new NullPointerException("Null exchanger argument");
            check(from, to);
            long step = 1;
            for (long maxStep = (to - 1 - from) / 9; step <= maxStep; step = 3 * step + 1) ;
            for (; step > 0; step /= 3) {
                for (long i = from + step; i < to; i++) {
                    long j = i - step;
                    for (; j >= from && comparator.less(j + step, j); j -= step)
                        exchanger.swap(j, j + step);
                }
            }
        }

        public String toString() {
            return "Shell sorting algorithm";
        }
    }

    static class QuickSorter extends ArraySorter {
        static final long THRESHOLD = 10; // shorter sub-arrays are sorted by insertion algorithm

        public void sortIndexes(int[] indexes, int from, int to, ArrayComparator comparator) {
            if (comparator == null)
                throw new NullPointerException("Null comparator argument");
            int temp;
            check(indexes, from, to);
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, quicksort_method_impl)
            //  \blong\b ==> int ;;
            //  comparator\.less\(([^,]+),\s*([^\)]+)\) ==>
            //  comparator.less(indexes[$1], indexes[$2]) ;;
            //  exchanger\.swap\(([^,]+),\s*([^\)]+)\) ==>
            //  temp = indexes[$1]; indexes[$1] = indexes[$2]; indexes[$2] = temp ;;
            //  (\r\n|\n|\r)[ \t]*\/\/[^\n\r]* ==> ;;
            //  [ \t]*\/\/[^\n\r]* ==>  ;;
            //  insertionSortWithoutChecks\(([^,]+,[^,]+,\s*comparator),\s*exchanger\b ==>
            //  insertionSortIndexesWithoutChecks(indexes, $1     !! Auto-generated: NOT EDIT !! ]]
            if (to - from <= 1)
                return;
            int[] stackLeft = new int[32];
            int[] stackRight = new int[32];
            int stackTop = -1;
            int left = from;
            int right = to - 1;

            for (; ; ) {

                if (right - left < THRESHOLD) {

                    insertionSortIndexesWithoutChecks(indexes, left, right + 1, comparator);
                    if (stackTop < 0)
                        break;
                    left = stackLeft[stackTop];
                    right = stackRight[stackTop];
                    stackTop--;

                } else {

                    int baseIndex = (left + right) >> 1;
                    if (comparator.less(indexes[baseIndex], indexes[left])) {
                        temp = indexes[baseIndex]; indexes[baseIndex] = indexes[left]; indexes[left] = temp;
                    }
                    if (comparator.less(indexes[right], indexes[baseIndex])) {
                        temp = indexes[right]; indexes[right] = indexes[baseIndex]; indexes[baseIndex] = temp;
                        if (comparator.less(indexes[baseIndex], indexes[left])) {
                            temp = indexes[baseIndex]; indexes[baseIndex] = indexes[left]; indexes[left] = temp;
                        }
                    }

                    temp = indexes[left + 1]; indexes[left + 1] = indexes[baseIndex]; indexes[baseIndex] = temp;
                    baseIndex = left + 1;

                    int i = left + 1;
                    int j = right;
                    for (; ; ) {
                        do
                            ++i;
                        while (comparator.less(indexes[i], indexes[baseIndex]));
                        do
                            --j;
                        while (comparator.less(indexes[baseIndex], indexes[j]));
                        if (i >= j)
                            break;
                        temp = indexes[i]; indexes[i] = indexes[j]; indexes[j] = temp;
                    }

                    temp = indexes[j]; indexes[j] = indexes[baseIndex]; indexes[baseIndex] = temp;

                    ++stackTop;
                    if (right - j >= j - left) {
                        stackRight[stackTop] = right;
                        stackLeft[stackTop] = j + 1;
                        right = j - 1;
                    } else {
                        stackRight[stackTop] = j - 1;
                        stackLeft[stackTop] = left;
                        left = j + 1;
                    }
                }
            }
            //[[Repeat.IncludeEnd]]
        }

        public void sort(long from, long to, ArrayComparator comparator, ArrayExchanger exchanger) {
            if (comparator == null)
                throw new NullPointerException("Null comparator argument");
            if (exchanger == null)
                throw new NullPointerException("Null exchanger argument");
            check(from, to);
            //[[Repeat.SectionStart quicksort_method_impl]]
            if (to - from <= 1)
                return;
            long[] stackLeft = new long[32];
            long[] stackRight = new long[32];
            // 32 stack elements allow to save bounds for sorting more than 2^32 elements (really, ~THRESHOLD/2 * 2^32)
            int stackTop = -1; // empty stack: the top element will be at stackLeft/Right[stackTop]
            long left = from;
            long right = to - 1;

            for (; ; ) { // sorting left..right elements (inclusive)

                if (right - left < THRESHOLD) {

                    // sorting by insertion algorithm
                    // some quick-sort implementations calls it after at the end of main procedure,
                    // but measuring time on Java 1.5 (-server mode, PIV-1800) shows that
                    // such correction slows down the algorithm (maybe, due to worse CPU cache usage)
                    insertionSortWithoutChecks(left, right + 1, comparator, exchanger);
                    if (stackTop < 0)
                        break; // all data are sorted
                    left = stackLeft[stackTop];
                    right = stackRight[stackTop];
                    stackTop--;

                } else { // if (right - left <= THRESHOLD)

                    // sorting 3 elements to find a median from 3: left, (left+right)/2, right
                    long baseIndex = (left + right) >> 1;
                    if (comparator.less(baseIndex, left)) {
                        exchanger.swap(baseIndex, left);
                    }
                    if (comparator.less(right, baseIndex)) {
                        exchanger.swap(right, baseIndex);
                        if (comparator.less(baseIndex, left)) {
                            exchanger.swap(baseIndex, left);
                        }
                    }

                    // Now data[left] <= data[baseIndex] <= data[right] (in other words, base is a median)
                    // moving the base at the new position left+1
                    exchanger.swap(left + 1, baseIndex);
                    baseIndex = left + 1;

                    // reordering elements left+2..right-1 so that, for some K,
                    //     data[left+2..K] <= base,
                    //     data[K+1..right-1] >= base,
                    // where base = data[baseIndex]
                    long i = left + 1;
                    long j = right;
                    for (; ; ) {
                        do
                            ++i;
                        while (comparator.less(i, baseIndex));
                        // Now
                        //     data[left+2..i-1] <= base
                        //         (= base for left+1, <= base for left+2 and exchanged indexes, < base for others),
                        //     data[i] >= base,
                        //     i <= j
                        do
                            --j;
                        while (comparator.less(baseIndex, j));
                        // Now
                        //     data[j] <= base,
                        //     data[j+1..right-1] >= base,
                        //     i <= j+1
                        if (i >= j)
                            break;
                        exchanger.swap(i, j);
                        // Now
                        //     data[left+1..i] <= base,
                        //     data[j..right-1] >= base,
                        //     i < j
                    }
                    // Now
                    //     data[left+2..i-1] <= base,
                    //     data[j] <= base,
                    //     data[j+1..right-1] >= base,
                    //     i >= j,
                    // so
                    //     data[left+2..j] <= base.
                    // It means that elements are reordered and we can assign K=j

                    exchanger.swap(j, baseIndex);
                    // Now
                    //     data[left..j-1] <= base,
                    //     data[j] = base,
                    //     data[j+1..right] >= base

                    // We need to recursively sort two sub-arrays: left..j-1 and j+1..right
                    // We use our stack instead of Java recursion
                    ++stackTop;
                    if (right - j >= j - left) {
                        stackRight[stackTop] = right;
                        stackLeft[stackTop] = j + 1;
                        right = j - 1;
                        // (j+1,right) --> stack (less half), go to sorting left..j-1
                    } else {
                        stackRight[stackTop] = j - 1;
                        stackLeft[stackTop] = left;
                        left = j + 1;
                        // (left,j-1) --> stack (less half), goto sorting j+1..right
                    }
                } // end of "else" for "if (right - left < THRESHOLD)"
            } // main stack-based loop
            //[[Repeat.SectionEnd quicksort_method_impl]]
        }

        public String toString() {
            return "Insertion sorting algorithm";
        }
    }

    static void check(long from, long to) {
        if (from < 0 || from > to)
            throw new IllegalArgumentException("Illegal from ("
                    + from + ") or to (" + to + ") arguments: "
                    + "should be 0 <= from <= to");
    }

    static void check(int[] indexes, int from, int to) {
        if (indexes == null)
            throw new NullPointerException("Null indexes argument");
        check(from, to);
        if (to > indexes.length)
            throw new IllegalArgumentException("The sorted range " + from + ".." + to
                    + " is out of indexes array int[" + indexes.length + "]");
    }

    /* OBSOLETE
     public interface ExtendedExchanger extends Exchanger {
         public void readWorkElement(int k);

         public void writeWorkElement(int k);

         public void copyTo(int i, int j);

         public boolean greaterThanWorkElement(int k);
     }

         // in Shell sort:
                 . . .
                 for (; step > 0; step /= 3) {
                     if (exchanger instanceof ExtendedExchanger) {
                         ExtendedExchanger extendedExchanger = (ExtendedExchanger)exchanger;
                         for (int i = from + step; i < to; i++) {
                             extendedExchanger.readWorkElement(i);
                             int j = i - step;
                             for (; j >= from && extendedExchanger.greaterThanWorkElement(j); j -= step)
                                 extendedExchanger.copyTo(j, j + step);
                             extendedExchanger.writeWorkElement(j + step);
                         }
                     } else {
                 . . .
        // This more complex exchanger cannot optimize Java code in -server mode in real applications.
        // Moreover, it can slow down the program, for example, in the test.
     */
}
