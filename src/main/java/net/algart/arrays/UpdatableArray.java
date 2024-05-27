/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>AlgART one-dimensional array of any elements, read/write access, no resizing.</p>
 *
 * <p>It is a superinterface for {@link MutableArray}.
 * Unlike {@link MutableArray}, the methods of this interface
 * does not allow to change number of elements.
 * The instances of this interface are usually returned by {@link MutableArray#asUnresizable()} method.</p>
 *
 * <p>If elements of this array are primitive values (<tt>byte</tt>, <tt>short</tt>, etc.),
 * the array <b>must</b> implement one of
 * {@link UpdatableBitArray}, {@link UpdatableCharArray},
 * {@link UpdatableByteArray}, {@link UpdatableShortArray},
 * {@link UpdatableIntArray}, {@link UpdatableLongArray},
 * {@link UpdatableFloatArray}, {@link UpdatableDoubleArray}
 * subinterfaces.</p>
 * In other case, this array <b>must</b> implement {@link UpdatableObjectArray} subinterface.</p>
 *
 * <p>Updatable arrays, implementing this interface,
 * are not thread-safe, but <b>are thread-compatible</b>
 * and can be synchronized manually if multithreading access is necessary.
 * Please see more details in the
 * <a href="package-summary.html#multithreading">package description</a>.</p>
 *
 * @author Daniel Alievsky
 * @see Array
 * @see MutableArray
 * @see Matrix
 */
public interface UpdatableArray extends Array, ArrayExchanger {
    /**
     * Sets the element #<tt>index</tt> to the specified <tt>value</tt>.
     * The new value is first automatically unwrapped if this array contains elements of primitive types.
     *
     * <p>It is a low-level method.
     * For arrays of primitive elements, implementing one of corresponding interfaces
     * {@link UpdatableBitArray}, {@link UpdatableCharArray},
     * {@link UpdatableByteArray}, {@link UpdatableShortArray},
     * {@link UpdatableIntArray}, {@link UpdatableLongArray},
     * {@link UpdatableFloatArray}, {@link UpdatableDoubleArray},
     * we recommend to use more efficient equivalent method of that interfaces:
     * {@link UpdatableBitArray#setBit(long, boolean)}, {@link UpdatableCharArray#setChar(long, char)},
     * {@link UpdatableByteArray#setByte(long, byte)}, {@link UpdatableShortArray#setShort(long, short)},
     * {@link UpdatableIntArray#setInt(long, int)}, {@link UpdatableLongArray#setLong(long, long)},
     * {@link UpdatableFloatArray#setFloat(long, float)}, {@link UpdatableDoubleArray#setDouble(long, double)}.
     * For other arrays, implementing {@link ObjectArray},
     * we recommend to use {@link UpdatableObjectArray#set(long, Object)}.
     *
     * <p>For arrays of primitive elements, this method
     * throws <tt>NullPointerException</tt> if <tt>value == null</tt>.
     * For arrays of non-primitive elements, this method
     * does not throw <tt>NullPointerException</tt> in this case,
     * but may set the element to some default value, if <tt>null</tt> elements are not supported
     * by the {@link MemoryModel memory model} (as in a case of {@link CombinedMemoryModel}).
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     * @throws NullPointerException      if <tt>value == null</tt> and it is an array of primitive elements.
     * @throws ClassCastException        if it is an array of primitive elements and <tt>value</tt>
     *                                   is not a corresponding wrapped class
     *                                   (<tt>Boolean</tt>, <tt>Integer</tt>, etc.)
     * @throws ArrayStoreException       if it is an array of non-primitive elements and <tt>value</tt>
     *                                   is not an instance of {@link #elementType()} class.
     */
    void setElement(long index, Object value);

    /**
     * Copies <tt>count</tt> elements from the specified Java array of corresponding type,
     * starting from <tt>srcArrayOffset</tt> index,
     * into this array, starting from <tt>arrayPos</tt> index.
     *
     * <p>For {@link UpdatableObjectArray arrays of non-primitive elements},
     * if some elements of Java array are <tt>null</tt>,
     * this method does not throw <tt>NullPointerException</tt>,
     * but may set the corresponding array elements to some default value,
     * if <tt>null</tt> elements are not supported
     * by the {@link MemoryModel memory model} (as in a case of {@link CombinedMemoryModel}).
     *
     * <p>Note: if <tt>IndexOutOfBoundsException</tt> occurs due to attempt to read data outside the passed
     * Java array, this AlgART array can be partially filled.
     * In other words, this method <b>can be non-atomic regarding this failure</b>.
     * All other possible exceptions are checked in the very beginning of this method
     * before any other actions (the standard way for checking exceptions).
     *
     * @param arrayPos       starting position in this AlgART array.
     * @param srcArray       the source Java array.
     * @param srcArrayOffset starting position in the source Java array.
     * @param count          the number of elements to be copied.
     * @return               a reference to this AlgART array.
     * @throws NullPointerException      if <tt>srcArray</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>srcArray</tt> argument is not an array or if <tt>count &lt; 0</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or source Java array.
     * @throws ArrayStoreException       if <tt>destArray</tt> element type mismatches with this array
     *                                   {@link #elementType() elementType()}.
     * @throws ClassCastException        if <tt>destArray</tt> element type mismatches with this array
     *                                   {@link #elementType() elementType()}
     *                                   (both this and <tt>ArrayStoreException</tt> are possible,
     *                                   depending on implementation).
     * @see DirectAccessible
     * @see #getData(long, Object, int, int)
     */
    UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    /**
     * Copies <tt>min(this.{@link #length() length()} - arrayPos, srcArray.length})</tt>
     * elements from the specified Java array of corresponding type,
     * starting from <tt>0</tt> index,
     * into this array, starting from <tt>arrayPos</tt> index.
     *
     * <p>For {@link UpdatableObjectArray arrays of non-primitive elements},
     * if some elements of Java array are <tt>null</tt>,
     * this method does not throw <tt>NullPointerException</tt>,
     * but may set the corresponding array elements to some default value,
     * if <tt>null</tt> elements are not supported
     * by the {@link MemoryModel memory model} (as in a case of {@link CombinedMemoryModel}).
     *
     * @param arrayPos       starting position in this AlgART array.
     * @param srcArray       the source Java array.
     * @return               a reference to this AlgART array.
     * @throws NullPointerException      if <tt>srcArray</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>srcArray</tt> argument is not an array.
     * @throws ArrayStoreException       if <tt>destArray</tt> element type mismatches with this array
     *                                   {@link #elementType()}.
     * @throws ClassCastException        if <tt>destArray</tt> element type mismatches with this array
     *                                   {@link #elementType()}
     *                                   (both this and <tt>ArrayStoreException</tt> are possible,
     *                                   depending on implementation).
     * @see DirectAccessible
     * @see #setData(long, Object, int, int)
     * @see #getData(long, Object)
     * @see UpdatableBitArray#setBits(long, long[], long, long)
     */
    UpdatableArray setData(long arrayPos, Object srcArray);

    /**
     * Copies element #<tt>srcIndex</tt> to position #<tt>destIndex</tt> inside this array.
     *
     * @param destIndex index of element to replace.
     * @param srcIndex  index of element to be copied.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <tt>0..length()-1</tt>.
     */
    void copy(long destIndex, long srcIndex);

    /**
     * Copies <tt>count</tt> elements, starting from <tt>srcIndex</tt> index,
     * to the same array, starting from <tt>destIndex</tt> index.
     *
     * <p><i>This method works correctly even if the copied areas overlap</i>,
     * i.e. if <tt>Math.abs(destIndex - srcIndex) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcIndex..srcIndex+count-1</tt>
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destIndex..destIndex+count-1</tt> of this array.
     *
     * @param destIndex starting index of element to replace.
     * @param srcIndex  starting index of element to be copied.
     * @param count     the number of elements to be copied.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <tt>0..length()-1</tt>.
     */
    void copy(long destIndex, long srcIndex, long count);

    /**
     * Swaps elements at positions #<tt>firstIndex</tt> and #<tt>secondIndex</tt> inside this array.
     *
     * @param firstIndex  first index of element to exchange.
     * @param secondIndex second index of element to exchange.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <tt>0..length()-1</tt>.
     */
    void swap(long firstIndex, long secondIndex);

    /**
     * Swaps <tt>count</tt> elements, starting from <tt>firstIndex</tt> index,
     * with <tt>count</tt> elements in the same array, starting from <tt>secondIndex</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>.
     *
     * @param firstIndex  starting first index of element to exchange.
     * @param secondIndex starting second index of element to exchange.
     * @param count       the number of elements to be exchanged.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <tt>0..length()-1</tt>.
     */
    void swap(long firstIndex, long secondIndex, long count);

    /**
     * Copies <tt>min(this.{@link #length() length()}, src.{@link #length() length()})</tt> elements of
     * <tt>src</tt> array, starting from index 0,
     * to this array, starting from index 0.
     *
     * <p>You may use {@link #subArray(long, long)} or {@link #subArr(long, long)} methods
     * to copy any portion of one array to any position of another array, for example:<pre>
     * destArray.subArr(destPos, count).copy(srcArray.subArr(srcPos, count));
     * </pre>
     *
     * <p>Some elements may be copied incorrectly if this array and <tt>src</tt>
     * are views of the same data and the swapped data areas overlap
     * (in the example above, <tt>Math.abs(destPos - srcPos) &lt; count)</tt>).
     * However, for arrays, created by {@link SimpleMemoryModel}, this method work correctly <i>always</i>,
     * even for overlapping areas.
     * For any arrays, if the copied areas of the underlying data are <i>the same</i>
     * (for example, if <tt>src</tt> if some view of this array generated by
     * {@link Arrays#asIndexFuncArray Arrays.asFuncArray}, but not {@link Arrays#asShifted Arrays.asShifted} method),
     * this method work correctly: any elements will be read before they will be updated.
     *
     * <p>This method works only if the element types of this and <tt>src</tt> arrays
     * (returned by {@link #elementType()}) are the same, or (for non-primitive elements)
     * if element type of this array is a superclass of the source element type.
     *
     * <p>For non-primitive element type ({@link ObjectArray}, {@link UpdatableObjectArray},
     * {@link MutableObjectArray} subinterfaces), this method may copy only references to elements,
     * but also may copy the content of elements: it depends on implementation.
     *
     * <p>Please note: this method is a good choice for copying little arrays (thousands,
     * maybe hundreds of thousands elements). If you copy large arrays by this method,
     * the user, in particular, has no ways to view the progress of copying or to interrupt copying.
     * In most situations, we recommend more "intellectual" method
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} for copying large arrays.
     *
     * @param src the source array.
     * @return    a reference to this array.
     * @throws NullPointerException     if <tt>src</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the source and this element types do not match.
     * @see Arrays#copy(ArrayContext, UpdatableArray, Array)
     */
    UpdatableArray copy(Array src);

    /**
     * Swaps <tt>min(this.{@link #length() length()}, src.{@link #length() length()})</tt> elements of
     * <tt>another</tt> array, starting from index 0,
     * and the same number of elements of this array, starting from index 0.
     *
     * <p>You may use {@link #subArray(long, long)} or {@link #subArr(long, long)} methods
     * to swap any portions of two array, for example:<pre>
     * array1.subArr(pos1, count).swap(array2.subArr(pos2, count));
     * </pre>
     *
     * <p>Some elements may be swapped incorrectly if this array and <tt>another</tt> is the same array,
     * or are views of the same data, and the swapped areas overlap
     * (in the example above, <tt>Math.abs(pos1 - pos2) &lt; count)</tt>).
     *
     * <p>This method must work always if the element types of this and <tt>src</tt> arrays
     * (returned by {@link #elementType()}) are the same.
     *
     * <p>For non-primitive element type ({@link ObjectArray}, {@link UpdatableObjectArray},
     * {@link MutableObjectArray} subinterfaces), this method may swap only references to elements,
     * but also may swap the content of elements: it depends on implementation.
     *
     * <p>Please note: this method is a good choice for swapping little arrays (thousands,
     * maybe hundreds of thousands elementthousands elements). If you swap large arrays by this method,
     * the user, in particular, has no ways to view the progress of copying or to interrupt copying.
     * To swap large arrays, you can split them to little regions and swap the regions in a loop by this method,
     * with calling {@link ArrayContext} methods to allow interruption and showing progress.
     *
     * @param another another array.
     * @return        a reference to this array.
     * @throws NullPointerException           if <tt>another</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException       if another and this element types do not match.
     */
    UpdatableArray swap(UpdatableArray another);

    /**
     * Returns an unresizable view of this array.
     * If this array is not resizable already, returns a reference to this object.
     * Query operations on the returned array "read through" and "write through" to this array.
     *
     * <p>The returned view (when it is not a reference to this object) contains the same elements
     * as this array, but independent length, start offset, capacity, copy-on-next-write and
     * possible other information about array characteristics besides its elements,
     * as for {@link #shallowClone()} method.
     * If modifications of this or returned array characteristics lead to reallocation
     * of the internal storage, then the returned array <i>ceases to be a view of this array</i>.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link #asCopyOnNextWrite() copy-on-next-write}.
     *
     * <p>Resizable arrays, created by this package, implement full {@link MutableArray}
     * interface, but unresizable ones implement only its {@link UpdatableArray} superinterface.
     *
     * @return an unresizable view of this array.
     * @see #isUnresizable()
     */
    UpdatableArray asUnresizable();

    /**
     * Clears the "{@link #isNew() new status}" for this array instance.
     * After this call, the {@link #isNew()} method will return <tt>false</tt> for this instance.
     * If the internal storage of this array will be reallocated in the future,
     * this instance may become "new" again.
     *
     * <p>The access to the "new status", provided by this and {@link #isNew()} method,
     * is always internally synchronized. So, clearing "new status" by this method
     * will be immediately visible in all threads, using this instance.
     */
    void setNonNew();

    UpdatableArray subArray(long fromIndex, long toIndex);

    UpdatableArray subArr(long position, long count);

    UpdatableArray asCopyOnNextWrite();

    UpdatableArray shallowClone();

    default Matrix<? extends UpdatableArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }
}
