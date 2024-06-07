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

import java.nio.ByteOrder;

/**
 * <p>AlgART array of any elements, read-only access.</p>
 *
 * <p>It is the basic superinterface for {@link MutableArray} and {@link UpdatableArray}.
 * Unlike that interfaces, the methods of this interface
 * does not allow to modify elements or a number of elements.
 * The instances of this interface, which do not implement
 * its inheritors {@link UpdatableArray} and {@link MutableArray},
 * are usually returned by  {@link #asImmutable()} method.</p>
 *
 * <p>If the elements of this array are primitive values (<code>byte</code>, <code>short</code>, etc.),
 * the array <b>must</b> implement one of
 * {@link BitArray}, {@link CharArray},
 * {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray},
 * {@link FloatArray}, {@link DoubleArray}
 * subinterfaces.
 * In other case, the array <b>must</b> implement {@link ObjectArray} subinterface.</p>
 *
 * <p>{@link #asImmutable() Immutable} arrays, implementing this interface,
 * are <b>thread-safe</b> and can be used simultaneously in several threads.
 * All other kinds of arrays <b>are thread-compatible</b>
 * and can be synchronized manually if multithreading access is necessary.
 * Please see more details in the
 * <a href="package-summary.html#multithreading">package description</a>.</p>
 *
 * @author Daniel Alievsky
 * @see UpdatableArray
 * @see MutableArray
 * @see Matrix
 */
public interface Array {
    /**
     * Returns the type of array elements.
     * For arrays of primitive types, returns:<ul>
     * <li><code>boolean.class</code> for {@link BitArray},</li>
     * <li><code>char.class</code> for {@link CharArray},</li>
     * <li><code>byte.class</code> for {@link ByteArray},</li>
     * <li><code>short.class</code> for {@link ShortArray},</li>
     * <li><code>int.class</code> for {@link IntArray}),</li>
     * <li><code>long.class</code> for {@link LongArray},</li>
     * <li><code>float.class</code> for {@link FloatArray},</li>
     * <li><code>double.class</code> for {@link DoubleArray}.</li>
     * </ul>
     * All elements of the array are values of this type or (for non-primitive types)
     * some inheritor of this type.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     *
     * @return the type of array elements.
     * @see Arrays#elementType(Class)
     */
    Class<?> elementType();

    /**
     * Returns the <i>canonical AlgART type</i> of this array: the class of one of 9 basic interfaces,
     * describing all kinds of AlgART arrays for 8 primitive and any non-primitive element types.
     * More precisely, returns:<ul>
     * <li><code>{@link BitArray}.class</code>, if this object is an instance of {@link BitArray},</li>
     * <li><code>{@link CharArray}.class</code>, if this object is an instance of {@link CharArray},</li>
     * <li><code>{@link ByteArray}.class</code>, if this object is an instance of {@link ByteArray},</li>
     * <li><code>{@link ShortArray}.class</code>, if this object is an instance of {@link ShortArray},</li>
     * <li><code>{@link IntArray}.class</code>, if this object is an instance of {@link IntArray},</li>
     * <li><code>{@link LongArray}.class</code>, if this object is an instance of {@link LongArray},</li>
     * <li><code>{@link FloatArray}.class</code>, if this object is an instance of {@link FloatArray},</li>
     * <li><code>{@link DoubleArray}.class</code>, if this object is an instance of {@link DoubleArray},</li>
     * <li><code>{@link ObjectArray}.class</code>, if this object is an instance of {@link ObjectArray}.</li>
     * </ul>
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant value).
     *
     * @return canonical AlgART type of this array.
     */
    Class<? extends Array> type();

    /**
     * Returns the <i>canonical updatable AlgART type</i> of arrays with the same element types:
     * the class of one of 9 basic interfaces,
     * describing all kinds of updatable AlgART arrays for 8 primitive and any non-primitive element types.
     * More precisely, returns:<ul>
     * <li><code>{@link UpdatableBitArray}.class</code>, if this object is an instance of {@link BitArray},</li>
     * <li><code>{@link UpdatableCharArray}.class</code>, if this object is an instance of {@link CharArray},</li>
     * <li><code>{@link UpdatableByteArray}.class</code>, if this object is an instance of {@link ByteArray},</li>
     * <li><code>{@link UpdatableShortArray}.class</code>, if this object is an instance of {@link ShortArray},</li>
     * <li><code>{@link UpdatableIntArray}.class</code>, if this object is an instance of {@link IntArray},</li>
     * <li><code>{@link UpdatableLongArray}.class</code>, if this object is an instance of {@link LongArray},</li>
     * <li><code>{@link UpdatableFloatArray}.class</code>, if this object is an instance of {@link FloatArray},</li>
     * <li><code>{@link UpdatableDoubleArray}.class</code>, if this object is an instance of {@link DoubleArray},</li>
     * <li><code>{@link UpdatableObjectArray}.class</code>, if this object is an instance of {@link ObjectArray}.</li>
     * </ul>
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant value).
     *
     * @return canonical AlgART type of updatable array of the same kind.
     */
    Class<? extends UpdatableArray> updatableType();

    /**
     * Returns the <i>canonical resizable AlgART type</i> of arrays with the same element types:
     * the class of one of 9 basic interfaces,
     * describing all kinds of resizable AlgART arrays for 8 primitive and any non-primitive element types.
     * More precisely, returns:<ul>
     * <li><code>{@link MutableBitArray}.class</code>, if this object is an instance of {@link BitArray},</li>
     * <li><code>{@link MutableCharArray}.class</code>, if this object is an instance of {@link CharArray},</li>
     * <li><code>{@link MutableByteArray}.class</code>, if this object is an instance of {@link ByteArray},</li>
     * <li><code>{@link MutableShortArray}.class</code>, if this object is an instance of {@link ShortArray},</li>
     * <li><code>{@link MutableIntArray}.class</code>, if this object is an instance of {@link IntArray},</li>
     * <li><code>{@link MutableLongArray}.class</code>, if this object is an instance of {@link LongArray},</li>
     * <li><code>{@link MutableFloatArray}.class</code>, if this object is an instance of {@link FloatArray},</li>
     * <li><code>{@link MutableDoubleArray}.class</code>, if this object is an instance of {@link DoubleArray},</li>
     * <li><code>{@link MutableObjectArray}.class</code>, if this object is an instance of {@link ObjectArray}.</li>
     * </ul>
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant value).
     *
     * @return canonical AlgART class of a resizable array of the same kind.
     */
    Class<? extends MutableArray> mutableType();

    /**
     * Returns the length: number of elements in this array.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     * The result of this method is never negative.</p>
     *
     * @return the length: number of elements in this array.
     */
    long length();

    /**
     * Returns the length as 32-bit <code>int</code> value.
     * If the {@link #length() actual length} is greater than <code>Integer.MAX_VALUE</code>,
     * throws <code>TooLargeArrayException</code>.
     *
     * <p>This method is convenient to allocate memory for a regular Java array
     * if you want to ensure that this AlgART array can be completely copied into such an array.</p>
     *
     * @return the length: number of elements in this array, if it is less than 2<sup>31</sup>.
     * @throws TooLargeArrayException if the actual length is greater than
     *                                <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1.
     */
    default int length32() throws TooLargeArrayException {
        long r = length();
        if (r < 0) {
            throw new AssertionError("Negative result " + r + " of length() method");
        }
        if (r > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large array (>= 2^31 elements): " + this);
        }
        return (int) r;
    }

    /**
     * Equivalent to the call <code>{@link #length() length}() == 0</code>.
     *
     * @return <code>true</code> if the array is empty, i.e. its length is zero.
     */
    default boolean isEmpty() {
        return length() == 0;
    }


    /**
     * Returns the capacity of this array: the number of elements
     * allocated for storing elements in this array.
     * For resizable arrays (implementing {@link MutableArray} interface),
     * the internal storage will be reallocated after {@link MutableArray#length(long)}
     * call only if the new length is <i>greater</i> than the current capacity.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     *
     * @return the capacity of this array.
     */
    long capacity();

    /**
     * Returns the element #<code>index</code>.
     * It this array contains elements of primitive types,
     * the value is automatically wrapped in an object (<code>Boolean</code>,
     * <code>Byte</code>, etc.).
     *
     * <p>It is a low-level method.
     * For arrays of primitive elements, implementing one of corresponding interfaces
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray}, {@link DoubleArray},
     * we recommend to use more efficient equivalent method of that interfaces:
     * {@link BitArray#getBit(long)}, {@link CharArray#getChar(long)},
     * {@link ByteArray#getByte(long)}, {@link ShortArray#getShort(long)},
     * {@link IntArray#getInt(long)}, {@link LongArray#getLong(long)},
     * {@link FloatArray#getFloat(long)}, {@link DoubleArray#getDouble(long)}.
     * For other arrays, implementing {@link ObjectArray},
     * we recommend to use {@link ObjectArray#get(long)}.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    Object getElement(long index);

    /**
     * Copies <code>count</code> elements of this array, starting from <code>arrayPos</code> index,
     * into the specified Java array of corresponding type, starting from <code>destArrayOffset</code> index.
     *
     * <p>For non-primitive element type ({@link ObjectArray}, {@link UpdatableObjectArray},
     * {@link MutableObjectArray} subinterfaces), this method may allocate new instances
     * for Java array elements <code>destArray[destArrayOffset]..destArray[destArrayOffset+count-1]</code>,
     * but also may change the state of already existing non-null elements: it depends on implementation.
     * In any case, you can be sure that if some of the target elements <code>destArray[k]==null</code>,
     * this method always allocate new element.
     *
     * <p>Note: if <code>IndexOutOfBoundsException</code> occurs due to attempt to write data outside the passed
     * Java array, the target Java array can be partially filled.
     * In other words, this method <b>can be non-atomic regarding this failure</b>.
     * All other possible exceptions are checked in the very beginning of this method
     * before any other actions (the standard way for checking exceptions).
     *
     * @param arrayPos        starting position in this AlgART array.
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be copied.
     * @throws NullPointerException      if <code>destArray</code> argument is <code>null</code>.
     * @throws IllegalArgumentException  if <code>destArray</code> argument is not an array or
     *                                   if <code>count &lt; 0</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or target Java array.
     * @throws ArrayStoreException       if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}.
     * @throws ClassCastException        if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}
     *                                   (both this and <code>ArrayStoreException</code> are possible,
     *                                   depending on implementation).
     * @see DirectAccessible
     * @see UpdatableArray#setData(long, Object, int, int)
     * @see BitArray#getBits(long, long[], long, long)
     */
    void getData(long arrayPos, Object destArray, int destArrayOffset, int count);

    /**
     * Copies <code>min(this.{@link #length() length() - arrayPos}, destArray.length})</code>
     * elements of this array, starting from <code>arrayPos</code> index,
     * into the specified Java array of corresponding type, starting from <code>0</code> index.
     *
     * <p>For non-primitive element type ({@link ObjectArray}, {@link UpdatableObjectArray},
     * {@link MutableObjectArray} subinterfaces), this method may allocate new instances
     * for Java array elements <code>destArray[0]..destArray[count-1]</code>,
     * but also may change the state of already existing non-null elements: it depends on implementation.
     * In any case, you can be sure: if some of the target elements <code>destArray[k]==null</code>,
     * this method always allocates new elements.
     *
     * @param arrayPos  starting position in this AlgART array.
     * @param destArray the target Java array.
     * @throws NullPointerException     if <code>destArray</code> argument is <code>null</code>.
     * @throws IllegalArgumentException if <code>destArray</code> argument is not an array.
     * @throws ArrayStoreException      if <code>destArray</code> element type mismatches with this array
     *                                  {@link #elementType()}.
     * @throws ClassCastException       if <code>destArray</code> element type mismatches with this array
     *                                  {@link #elementType()}
     *                                  (both this and <code>ArrayStoreException</code> are possible,
     *                                  depending on implementation).
     * @see DirectAccessible
     * @see #getData(long, Object, int, int)
     * @see UpdatableArray#setData(long, Object)
     */
    void getData(long arrayPos, Object destArray);

    /**
     * Returns usual Java-array (zero-filled) with the specified length
     * and element type returned by {@link #elementType()} method.
     *
     * <p>This method is equivalent to the following call:
     * <code>java.lang.reflect.Array.newInstance(elementType(),&nbsp;length)</code>.
     *
     * <p>This method can be helpful while using together with
     * {@link #getData(long, Object, int, int) getData} /
     * {@link UpdatableArray#setData(long, Object, int, int) setData} methods.
     *
     * @param length the length of created Java-array.
     * @return Java-array with the specified length and the same type of elements.
     * @throws NegativeArraySizeException if the specified <code>length</code> is negative.
     */
    Object newJavaArray(int length);

    /**
     * Returns a view of the portion of this array between <code>fromIndex</code>,
     * inclusive, and <code>toIndex</code>, exclusive.
     * <ul>
     * <li>If <code>fromIndex</code> and <code>toIndex</code> are equal, the returned array is empty.
     * <li>The returned array is backed by this array, so &mdash; if this array is not immutable
     * &mdash; any changes of the elements of the returned array are reflected in this array, and vice-versa.
     * <li>The capacity of returned array (returned by {@link #capacity()} method) will be
     * equal to the its length (returned by {@link #length()}, that is <code>toIndex-fromIndex</code>.
     * <li>The {@link #elementType() type of elements} of the returned array is the same
     * as the type of elements of this array.</li>
     * <li>The returned array is {@link #asImmutable() immutable},
     * {@link #asTrustedImmutable() trusted immutable} or
     * {@link #asCopyOnNextWrite() copy-on-next-write}, if, and only if,
     * this array is immutable, trusted immutable or copy-on-next-write correspondingly.</li>
     * <li>If (and only if) this array implements {@link UpdatableArray} interface,
     * then the returned array also implements it.
     * If (and only if) this array implements {@link DirectAccessible} interface,
     * then the returned array also implements it.
     * The returned array <i>never</i> implements {@link MutableArray} interface;
     * it is always <i>unresizable</i>.</li>
     * </ul>
     *
     * <p>Like <code>List.subList</code> method, this method eliminates the need
     * for explicit range operations.
     * For example, you may use {@link Arrays#sort(UpdatableArray, ArrayComparator)}
     * method for sorting a fragment of the array.
     *
     * <p>Unlike <code>List.subList</code>, the semantics of the array returned
     * by this method is well-defined in any case, even in case of
     * resizing of the source array.
     * Namely, if the internal storage of this or returned array is reallocated,
     * then the returned array will cease to be a view of this array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link #asCopyOnNextWrite() copy-on-next-write}.
     * Also, if the length of this array will be reduced,
     * it can lead to clearing some elements in returned array:
     * see comments to {@link MutableArray#length(long)} method.
     *
     * @param fromIndex low endpoint (inclusive) of the subarray.
     * @param toIndex   high endpoint (exclusive) of the subarray.
     * @return a view of the specified range within this array.
     * @throws IndexOutOfBoundsException for illegal <code>fromIndex</code> and <code>toIndex</code>
     *                                   (<code>fromIndex &lt; 0 || toIndex &gt; {@link #length()}
     *                                   || fromIndex &gt; toIndex</code>).
     * @see #subArr(long, long)
     */
    Array subArray(long fromIndex, long toIndex);

    /**
     * Equivalent to {@link #subArray(long, long) subArray(position, position + count)}.
     * The only possible difference is other exception messages.
     * If <code>position+count&gt;Long.MAX_VALUE</code> (overflow),
     * an exception message is allowed to be not fully correct
     * (maximal speed is more important than absolutely correct exception messages for such exotic situations).
     *
     * @param position start position (inclusive) of the subarray.
     * @param count    number of elements in the subarray.
     * @return a view of the specified range within this array.
     * @throws IndexOutOfBoundsException for illegal <code>position</code> and <code>count</code>
     *                                   (<code>position &lt; 0 || count &lt; 0
     *                                   || position + count &gt; {@link #length()}</code>).
     * @see #subArray(long, long)
     */
    Array subArr(long position, long count);

    /**
     * Returns a {@link DataBuffer data buffer} allowing block access to this array
     * with the specified {@link DataBuffer.AccessMode access mode} and buffer capacity.
     *
     * <p>If this array does not implement {@link UpdatableArray} interface
     * (so, it is probably {@link #asImmutable() immutable} or {@link #asTrustedImmutable()
     * trusted immutable}), the <code>mode</code> argument must be {@link DataBuffer.AccessMode#READ}.
     *
     * <p>The <code>capacity</code> argument must be in range <code>0..2<sup>37</sup>-1</code>
     * for {@link BitArray bit arrays} or <code>0..2<sup>31</sup>-1</code> for all other element types.
     *
     * <p>If the <code>capacity</code> argument is greater than this array {@link #length()},
     * it is truncated to this length.
     *
     * @param mode     the access mode for new buffer.
     * @param capacity the capacity of the buffer.
     * @return new data buffer for accessing this array.
     * @throws NullPointerException     if <code>mode</code> argument is <code>null</code>.
     * @throws IllegalArgumentException if the <code>mode</code> is not the {@link DataBuffer.AccessMode#READ},
     *                                  but this arrays does not implement {@link UpdatableArray} interface,
     *                                  or if the specified <code>capacity</code> is negative or too high
     *                                  (&gt;=0..2<sup>37</sup> for bits or &gt;=0..2<sup>31</sup> for
     *                                  other element types).
     * @see #buffer(net.algart.arrays.DataBuffer.AccessMode)
     * @see #buffer(long)
     * @see #buffer()
     */
    DataBuffer buffer(DataBuffer.AccessMode mode, long capacity);

    /**
     * Equivalent to {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)
     * buffer(mode, someCapacity)}, where <code>mode</code> is the argument of this method
     * and <code>someCapacity</code> is chosen automatically to provide good performance in typical situations.
     * Usually, the capacity is chosen to get a buffer occupying several kilobytes,
     * that can fit in an internal cache of most processors.
     *
     * <p>In any case, you can be sure that the chosen capacity will not be greater than
     * <code>Integer.MAX_VALUE-64</code>.
     *
     * @param mode the access mode for new buffer.
     * @return new data buffer for accessing this array.
     * @throws NullPointerException     if <code>mode</code> argument is <code>null</code>.
     * @throws IllegalArgumentException if the <code>mode</code> is not the {@link DataBuffer.AccessMode#READ},
     *                                  but this arrays does not implement {@link UpdatableArray} interface.
     */
    DataBuffer buffer(DataBuffer.AccessMode mode);

    /**
     * Equivalent to {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)
     * buffer(suitableMode, capacity)}, where <code>capacity</code> is the argument of this method
     * and <code>suitableMode</code> is chosen automatically. Namely, <code>suitableMode</code> is:<ul>
     * <li>{@link DataBuffer.AccessMode#READ_WRITE} if this array implements {@link UpdatableArray} interface
     * (i.e., is modifiable and, maybe, resizable);</li>
     * <li>{@link DataBuffer.AccessMode#READ} if this array does not implements this interface
     * (i.e., probably, immutable or {@link #asTrustedImmutable trusted immutable}).</li>
     * </ul>
     *
     * @param capacity the capacity of the buffer.
     * @return new data buffer for accessing this array.
     * @throws IllegalArgumentException if the specified <code>capacity</code> is negative or too high
     *                                  (&gt;=0..2<sup>37</sup> for bits or &gt;=0..2<sup>31</sup> for
     *                                  other element types).
     */
    DataBuffer buffer(long capacity);

    /**
     * Equivalent to {@link #buffer(net.algart.arrays.DataBuffer.AccessMode, long)
     * buffer(suitableMode, someCapacity)}, where both <code>suitableMode</code> and <code>someCapacity</code>
     * arguments are chosen automatically. The algorithm of choosing these arguments is the same
     * as for {@link #buffer(net.algart.arrays.DataBuffer.AccessMode)} and {@link #buffer(long)} methods.
     *
     * <p>In any case, you can be sure that the chosen capacity will not be greater than
     * <code>Integer.MAX_VALUE-64</code>.
     *
     * @return new data buffer for accessing this array.
     */
    DataBuffer buffer();

    /**
     * Returns an <i>immutable</i> view of this array.
     * If this array is already immutable (i.e. {@link #isImmutable()} is <code>true</code>),
     * returns a reference to this object.
     *
     * <p>A array is considered to be <i>immutable</i>,
     * if there are no ways to modify its content or state with help of this instance.
     * In particular, immutable arrays never
     * implement {@link UpdatableArray} or {@link DirectAccessible} interfaces.
     * Moreover, any third-party implementation of <code>Array</code> interface
     * <i>must</i> return an instance of a class, which has no methods or fields
     * allowing to change this instance.
     *
     * <p>Query operations on the returned array "read through"
     * to this array. The returned view is also unresizable
     * (see {@link UpdatableArray#asUnresizable()}).
     *
     * <p>The returned view (when it is not a reference to this object) contains the same elements
     * as this array, but independent length, start offset, capacity, copy-on-next-write and
     * possible other information about array characteristics besides its elements,
     * as for {@link #shallowClone()} method.
     * If modifications of this array characteristics lead to reallocation
     * of the internal storage, then the returned array ceases to be a view of this array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link #asCopyOnNextWrite() copy-on-next-write}.
     *
     * <p>By default, the array factories ({@link MemoryModel memory models}) create mutable arrays,
     * but they can be converted to immutable by this method.
     *
     * <p>Note: {@link #isNew()} method, called for the result of this method, always returns <code>false</code>
     * &mdash; because it does not implement {@link UpdatableArray}.
     *
     * <p>Also note: {@link #isNewReadOnlyView()} method, called for the result of this method, always returns
     * the same value as {@link #isNewReadOnlyView()} for this object.
     * Really,
     * <ul>
     * <li>it this object is immutable (<code>{@link #isImmutable()}==true</code>),
     * then it is obvious (this method just returns a reference to this array);</li>
     * <li>it this object is not immutable (<code>{@link #isImmutable()}==false</code>),
     * then, according to the contract to {@link #isNewReadOnlyView()} method,
     * {@link #isNewReadOnlyView()} must return <code>false</code> for this array
     * (in other case {@link #isImmutable()} would return <code>true</code>) and
     * it also must return <code>false</code> for the returned array
     * (because it is a view of another array and not an original view of external data &mdash;
     * see the beginning of the comment to {@link #isNewReadOnlyView()}).
     * </li>
     * </ul>
     *
     * @return an immutable view of this array (or a reference to this array if it is already immutable).
     * @see #isImmutable()
     * @see #asTrustedImmutable()
     * @see #mutableClone(MemoryModel)
     * @see #updatableClone(MemoryModel)
     * @see UpdatableArray#asUnresizable()
     */
    Array asImmutable();

    /**
     * Returns <code>true</code> if this instance is <i>immutable</i>, i&#46;e&#46; there are no ways to
     * change its content or state. (See {@link #asImmutable()} method for more details.)
     *
     * <p>It is possible that array is immutable in fact, but this method returns <code>false</code>:
     * for example, if the array is mapped to read-only file. However, it is guaranteed:
     * <i>if the array was created via {@link #asImmutable} method, this method
     * returns <code>true</code></i>.
     *
     * <p>Typically, this method returns <code>true</code> if the array:
     * <ol>
     * <li>does not implement {@link UpdatableArray} interface;</li>
     * <li>does not implement {@link DirectAccessible} interface, or implements it,
     * but {@link DirectAccessible#hasJavaArray()} method returns <code>false</code>.</li>
     * </ol>
     *
     * <p>But you should not use these conditions to check whether an array is immutable;
     * please use this method instead.
     * In principle, it is possible that both these conditions are satisfied, but the array
     * is though mutable. Maybe, some class from another package (or from future versions
     * of this package), implementing {@link Array} interface, does not implement
     * neither {@link UpdatableArray}, nor {@link DirectAccessible}, but offers another methods
     * allowing to change its state or content.
     *
     * <p>Note: if this method returns <code>true</code>, it does not mean that its content cannot
     * be modified at all. Quite the contrary, usually an immutable array <b>a</b>
     * is just an immutable view of another mutable array <b>b</b>
     * (created via <b>a</b>=<b>b.</b>{@link #asImmutable} call),
     * and the original array <b>b</b> does allow to change the content of the immutable array <b>a</b>.
     * Immutability means only that there are no ways to modify the content or state of the object <b>a</b>,
     * <i>if this object</i> <b>a</b> <i>is the only reference to its content, which you have</i>.
     * The same note is true for immutable collections, created by the standard
     * <code>Collections.unmodifiableList</code> and analogous methods.
     * Please compare this with the behaviour of another method {@link #isNewReadOnlyView()}.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return <code>true</code> if this instance is immutable.
     * @see #asImmutable()
     */
    boolean isImmutable();

    /**
     * Returns a <i>trusted immutable</i> view of this array.
     * If this array is already <i>trusted immutable</i>, returns a reference to this object.
     *
     * <p>A array is considered to be <i>"trusted" immutable</i>,
     * if it <b>potentially can</b> change its elements,
     * but the Java code working with this array <b>promises</b> that it will not change them.
     * The returned instance <b>never</b> implements {@link UpdatableArray},
     * but <b>may</b> implement {@link DirectAccessible}, that allow quick access to its elements.
     * As for {@link #asImmutable() usual immutable view},
     * query operations on the returned array "read through"
     * to this array.
     *
     * <p>The only standard way allowing to change elements of returned array
     * is using {@link DirectAccessible#javaArray()} method, in a case when the array is backed
     * by an accessible array.
     * But the Java code, processing the trusted immutable array,
     * must use this method <i>only for quick reading</i> elements and <i>not try to change</i> them.
     * If, despite the promise, the elements of the trusted immutable array will be changed,
     * the {@link UnallowedMutationError} may be thrown by the call of
     * {@link #checkUnallowedMutation()} method.
     *
     * <p>In some implementations &mdash; for example, if {@link DirectAccessible}
     * interface is not supported by this array
     * &mdash; this method may return the same result as {@link #asImmutable()}.
     *
     * <p>The returned view is always unresizable.
     *
     * <p>The returned view (when it is not a reference to this object) contains the same elements
     * as this array, but independent length, start offset, capacity, copy-on-next-write and
     * possible other information about array characteristics besides its elements,
     * as for {@link #shallowClone()} method.
     * If modifications of this array characteristics lead to reallocation
     * of the internal storage, then the returned array ceases to be a view of this array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link #asCopyOnNextWrite() copy-on-next-write}.
     *
     * <p>Trusted immutable view is a compromise between absolute safety, provided by
     * {@link #asImmutable() usual immutable view}, and maximal efficiency,
     * achieved while using the original non-protected array.
     * Please see the <a href="package-summary.html#protectionAgainstUnallowedChanges">package description</a>
     * to learn more about possible usage of this method.
     *
     * @return a trusted immutable view of this array (or a reference to this array if it is already
     * trusted immutable).
     * @see #asImmutable()
     * @see #checkUnallowedMutation()
     */
    Array asTrustedImmutable();

    /**
     * Tries to check, whether some non-allowed mutations of this {@link #asTrustedImmutable()
     * trusted immutable} array took place,
     * and throw {@link UnallowedMutationError} in this case.
     * Does nothing if this array implement {@link UpdatableArray} interface
     * or if it is truly immutable.
     *
     * <p>Implementation of this method usually checks whether the hash code was changed since array creation.
     *
     * <p>We recommend to call this method in <code>finally</code>
     * sections after using the trusted immutable array.
     * If it is impossible to create necessary <code>finally</code> section,
     * you may use {@link net.algart.finalizing.Finalizer} class (or an equivalent tool)
     * to schedule call of this method for the {@link #shallowClone() shallow clone} of this array
     * on deallocation of this array:<pre>
     * Finalizer fin = ...(some global application finalizer);
     * final Array dup = thisArray.shallowClone();
     * // - must be here, not inside the following inner class, to allow deallocation of thisArray
     * fin.invokeOnDeallocation(thisArray, new Runnable() {
     * &#32;   void run() {
     * &#32;       try {
     * &#32;           dup.checkUnallowedMutation();
     * &#32;       } catch (UnallowedMutationError ex) {
     * &#32;           myLogger.severe(ex.toString());
     * &#32;       }
     * &#32;   }
     * });
     * </pre>
     * Important: while using this finalization scheme, this array <i>must not be
     * {@link #isCopyOnNextWrite() copy-on-next-write}</i>!
     * Illegal modifications of copy-on-next-write array will not change
     * it's shallow clone and will not be detected.
     *
     * @throws UnallowedMutationError if some unallowed mutations of this array took place.
     * @see #asTrustedImmutable()
     */
    void checkUnallowedMutation() throws UnallowedMutationError;

    /**
     * Returns a <i>copy-on-next-write</i> view of this array.
     * If this array is immutable (and only in this case), returns a reference to this object.
     * If (and only if) this array implements {@link UpdatableArray} interface,
     * then the returned array also implements it.
     * If (and only if) this array implements {@link MutableArray} interface,
     * then the returned array also implements it.
     *
     * <p><i>Copy-on-next-write</i> array is an array with the following special feature:
     * the next attempt (but not further!) to modify this array,
     * or any other access that can lead to its modification (like {@link DirectAccessible#javaArray()}
     * method), will lead to reallocation of the underlying storage, used for array elements,
     * before performing the operation. In other words,
     * you have a guarantee: if this array is a view of some another array or data
     * (for example, a {@link #subArray(long, long) subarray} or
     * a {@link SimpleMemoryModel#asUpdatableArray(Object) view of Java array}),
     * that there are no ways to change that data
     * via accessing the returned array. Any changes, if they will occur,
     * will be performed with the newly allocated storage only.
     *
     * <p>Please be careful: if you will want to change arrays created by this method, the result may
     * be unexpected! For example, an attempt to copy other arrays into copy-on-next-write array
     * by some methods like {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} will probably
     * do nothing. The reason is working with the array via its subarrays &mdash;
     * for example, {@link Arrays#copy(ArrayContext, UpdatableArray, Array) Arrays.copy} method
     * splits the source and target arrays into subarrays and copies these subarrays.
     * (Usual {@link UpdatableArray#copy(Array)} method and other mutation methods
     * of the resulting array will work normally.)
     * The main goal of copy-on-next-write arrays is <i>protection</i> against unwanted changing
     * an original array; it is supposed that the client, in normal situation, will only read
     * such arrays and will not try to change them.
     *
     * <p>There are <i>no guarantees</i> that the returned array will be a view of this one,
     * even immediately after creation.
     * Some implementations of updatable arrays may just return the full (deep) copy of this object,
     * alike {@link #mutableClone(MemoryModel)} method, and in this case
     * {@link TooLargeArrayException} is possible.
     * All implementations from this package, excepting <code>AbstractUpdatableXxxArray</code> classes,
     * returns a view;
     * but in <code>AbstractUpdatableXxxArray</code> classes this method is equivalent to
     * <code>{@link #updatableClone(MemoryModel) updatableClone}({@link Arrays#SMM}).</code>
     *
     * <p>Please note that <b>copy-on-next-write arrays are not traditional copy-on-write objects like
     * {@link java.util.concurrent.CopyOnWriteArrayList}!</b> In particular, copy-on-next-write arrays
     * are not thread-safe.
     *
     * <p>The main purpose of using copy-on-next-write arrays is more efficient alternative
     * to {@link #updatableClone(MemoryModel) cloning} and creating {@link #asImmutable() quite immutable views},
     * when we need to be sure that original data will not be corrupted.
     * Please see the <a href="package-summary.html#protectionAgainstUnallowedChanges">package description</a>
     * to learn more about possible usage of this technique.
     *
     * @return a copy-on-next-write view of this array (or a reference to this array if it is immutable).
     * @throws TooLargeArrayException if this method actually creates a copy of this array, but
     *                                the {@link Array#length() length} of this array is too large
     *                                for the memory model, used for allocating a copy.
     * @see #isCopyOnNextWrite()
     * @see MemoryModel#newLazyCopy(Array)
     * @see MemoryModel#newUnresizableLazyCopy(Array)
     */
    Array asCopyOnNextWrite();

    /**
     * Returns <code>true</code> if this array is copy-on-next-write.
     * In other words, if this method returns <code>true</code>, it means that the next
     * attempt (but not further) to modify this array, or any other access that can lead to
     * its modification (like {@link DirectAccessible#javaArray()} method), will lead to
     * reallocation of the underlying storage. After reallocation,
     * the array will cease to be copy-on-next-write: further calls of
     * this method will return <code>false</code>.
     *
     * <p>This method can be useful if it's possible to select another,
     * more optimal algorithm branch, allowing to avoid reallocation
     * for copy-on-next-write arrays. The typical example is usage of
     * {@link DirectAccessible} interface. That interface, providing direct access to
     * the internal Java array (which is a storage of the array elements), can optimize
     * most of the algorithms processing an array. However, reallocation of
     * the Java array, that will be a result of calling {@link DirectAccessible#javaArray()} for
     * copy-on-next-write array, can make such "optimization" very unwanted.
     *
     * <p>The only standard way to make copy-on-next-write is calling
     * {@link #asCopyOnNextWrite()} method.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return <code>true</code> if this array is copy-on-next-write.
     * @see #asCopyOnNextWrite()
     */
    boolean isCopyOnNextWrite();

    /**
     * Returns <code>true</code> if this instance is unresizable, i&#46;e&#46; there are no ways to
     * change its length or capacity.
     *
     * <p>It is guaranteed that if the array was created via {@link #asImmutable} method, this method
     * returns <code>true</code>.
     *
     * <p>Typically, this method returns <code>true</code> if the array does not implement
     * {@link MutableArray}.
     * But you should not use this condition to check whether an array is unresizable;
     * please use this method instead.
     * Maybe, some class from another package (or from future versions
     * of this package), implementing this {@link Array} interface, does not implement
     * {@link MutableArray}, but offer another methods allowing to change its state or content.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return <code>true</code> if this instance is unresizable.
     * @see UpdatableArray#asUnresizable()
     */
    boolean isUnresizable();

    /**
     * Returns <code>true</code> if this array instance is <i>new</i>, i&#46;e&#46; it was created
     * by one of {@link MemoryModel} methods, creating an array or a matrix (<code>newXxx</code>
     * or <code>valueOf</code>), or by fully equivalent methods.
     * All other ways of creating AlgART array instance ({@link #asImmutable()},
     * {@link #shallowClone()}, {@link #subArray(long, long)}, {@link SimpleMemoryModel#asUpdatableArray(Object)}
     * etc.) create <i>non-new</i> arrays.
     *
     * <p>In updatable arrays the "new status", returned by this method, can be cleared manually by
     * {@link UpdatableArray#setNonNew()} method. Note that immutable arrays, not implementing {@link UpdatableArray}
     * interface, are never <i>new</i> (this method returns <code>false</code>).
     *
     * <p>If the array is <i>new</i>, you can be sure that it is an original object, storing the data,
     * but not a view of some other array ({@link #asImmutable() immutable view},
     * {@link #subArray(long, long) subarray}, etc.) or of another kind of data (Java array, file, etc.)
     * This can be important for managing data, associated with AlgART arrays.
     *
     * <p>For example, let we have some source (factory), generating AlgART arrays
     * (like a library of installable Java plugins), and we need to safely store the content
     * of these arrays in some permanent storage.
     * Of course, we can store a full clone of each array with help of {@link #updatableClone(MemoryModel)}
     * or {@link #mutableClone(MemoryModel)} method, but it can be inefficient for very large arrays (many gigabytes).
     * On the other hand, we usually cannot just store a reference to an AlgART array
     * or to its internal data: it is very possible, that one array, received from the factory, is a view of another
     * one ({@link #asImmutable() immutable view}, {@link #subArray(long, long) subarray}, etc.), and storing
     * both references in our storage will lead to incorrect behaviour &mdash; possible future changing
     * of one element of the storage will be reflected in other elements.
     *
     * <p><i>New</i> status of the array, provided by this method, allows to correctly resolve this problem.
     * We should store in the storage the full content (clone) of an array, if this method returns <code>false</code>,
     * or the reference to an array or to its internal data (as  {@link DirectAccessible#javaArray()}),
     * if it returns <code>true</code>. In the second case, we must additionally clear the status of the original
     * array object, received from the factory, by {@link UpdatableArray#setNonNew()} method &mdash;
     * it guarantees that we shall store only 1 reference to each really new array.
     *
     * <p>Additional important feature, provided by this method: if you know the memory model, which has created this
     * instance, then you can be absolutely sure in all details of the algorithm of internal storing
     * the array data (if it is documented in the corresponding memory model). It is important,
     * for example, for {@link LargeMemoryModel}: if this method returns <code>true</code>, then the content
     * of this array corresponds to the content of an external file according to a known, fully documented scheme.
     * Unlike this, another forms of AlgART arrays &mdash; like {@link #subArray(long, long) subarrays} &mdash;
     * have no documented correspondence with the content of an external data, even when we can retrieve
     * some information about such data (as a name of the disk file {@link LargeMemoryModel#getDataFilePath(Array)}).
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return whether this array instance is <i>new</i>: a new object, allocated by some {@link MemoryModel}.
     * @see #isNewReadOnlyView()
     */
    boolean isNew();

    /**
     * Returns <code>true</code> if this array instance is <i>new-read-only-view</i>, i&#46;e&#46;
     * a newly created <i>view</i> of some external data, providing <i>read-only</i> access to this data &mdash;
     * like {@link LargeMemoryModel#asArray(Object, Class, long, long, java.nio.ByteOrder)} method.
     * All other ways of creating AlgART array instance (methods of {@link MemoryModel} class,
     * {@link #asImmutable()}, {@link #shallowClone()}, {@link #subArray(long, long)}}
     * etc.) create arrays, in which this method returns <code>false</code>.
     * In the current version of this package, the only ways to create <i>new-read-only-view</i> AlgART array
     * are the following:
     * <ul>
     * <li>the method
     * {@link LargeMemoryModel#asArray(Object, Class, long filePosition, long fileAreaSize, ByteOrder byteOrder)},
     * when its <code>filePosition</code> argument is zero (<code>filePosition==0</code>);
     * </li>
     * <li>the versions of this method for concrete element types
     * {@link LargeMemoryModel#asBitArray(Object, long, long, java.nio.ByteOrder) asBitArray},
     * {@link LargeMemoryModel#asCharArray(Object, long, long, java.nio.ByteOrder) asCharArray},
     * {@link LargeMemoryModel#asByteArray(Object, long, long, java.nio.ByteOrder) asByteArray},
     * {@link LargeMemoryModel#asShortArray(Object, long, long, java.nio.ByteOrder) asShortArray},
     * {@link LargeMemoryModel#asIntArray(Object, long, long, java.nio.ByteOrder) asIntArray},
     * {@link LargeMemoryModel#asLongArray(Object, long, long, java.nio.ByteOrder) asLongArray},
     * {@link LargeMemoryModel#asFloatArray(Object, long, long, java.nio.ByteOrder) asFloatArray},
     * {@link LargeMemoryModel#asDoubleArray(Object, long, long, java.nio.ByteOrder) asDoubleArray}
     * in a case of the same condition: their <code>filePosition</code> argument is zero;
     * </li>
     * <li>{@link LargeMemoryModel#asMatrix(Object filePath, MatrixInfo matrixInfo)} method
     * in a case when the {@link MatrixInfo#dataOffset() data offset}, stored in its {@link MatrixInfo} argument,
     * is zero &mdash; then the array, extracted from such a matrix by
     * {@link LargeMemoryModel#getRawArrayForSavingInFile(Matrix)} method, will be
     * <i>new-read-only-view</i>.</li>
     * </ul>
     *
     * <p><i>New-read-only-view</i> status, returned by this method,
     * is final and cannot be changed after instantiation of the instance.
     * (More precisely, there are no methods, allowing to change it after finishing
     * the method, which has created a new array instance.)
     * Note that in updatable arrays, implementing {@link UpdatableArray}
     * interface, this method returns <code>false</code> always.
     * Moreover, if this method returns <code>true</code>, then there is a guarantee that
     * {@link #isImmutable()} method also returns <code>true</code>.
     *
     * <p>If this method returns <code>true</code>, you can be sure that it is an original object,
     * associated with external data with read-only access rights,
     * but not a view of some other array ({@link #asImmutable() immutable view},
     * {@link #subArray(long, long) subarray}, etc.) and not an updatable view of some external data,
     * allowing to change them.
     * This can improve efficiency of managing data, associated with AlgART arrays, in addition to
     * {@link #isNew()} method.
     *
     * <p>Please read again the possible scheme of storing array data in some storage, listed in the comments
     * to {@link #isNew()}. Sometimes we need to store some number of absolutely identical AlgART
     * arrays, corresponding to the same data, maybe very large (many gigabytes).
     * The listed scheme permits storing only 1 reference to each array
     * (or its data), because we should avoid ability of changes in one stored array,
     * reflecting in another stored array. It is a good and safe strategy, but it does not provide
     * maximal performance in a case, when <i>we know that all data are immutable (read-only) and we shall never
     * change them</i>. In the last case, there is no problem to create any number of references to the same
     * data, as well as there is no problem to create a lot of references to the same
     * immutable Java object like <code>String</code>.
     *
     * <p>This method allows to improve the described behaviour. Namely, if this method returns <code>true</code>,
     * you still <i>may</i> store the reference to an AlgART array or to its internal data in your storage,
     * though {@link #isNew()} returns <code>false</code>.
     * Yes, you can so create several references to the same array data,
     * but it does not lead to incorrect behaviour &mdash; this data will always remain unchanged.
     *
     * <p>But here is an important <i>warning</i>: while using this technique, you should never try
     * to increase the access rights to the external data, corresponding to the AlgART array, stored in your storage.
     * More precisely, you should not try to provide write access to this data &mdash; even if the API allows
     * to do this. In a case of {@link LargeMemoryModel}, it means that you should not access to the disk file,
     * retrieved from an array by {@link LargeMemoryModel#getDataFilePath(Array)} method,
     * via {@link LargeMemoryModel#asUpdatableArray(Object, Class, long, long, boolean, java.nio.ByteOrder)
     * LargeMemoryModel.asUpdatableArray} method &mdash; please use the original reference
     * to AlgART array (where this method returns <code>true</code>) or new instances,
     * created by {@link LargeMemoryModel#asArray(Object, Class, long, long, java.nio.ByteOrder)
     * LargeMemoryModel.asArray} method.
     * Maybe, future versions of {@link LargeMemoryModel} class will contain API,
     * which will allow to provide OS-level protection against any attempts to write into files,
     * containing the data, stored in your storage of AlgART arrays.
     *
     * <p>In addition, this method provides the same feature as {@link #isNew()} method:
     * if you know the memory model, which has created this instance,
     * then you can be absolutely sure in all details of the algorithm of internal storing
     * the array data (if it is documented in the corresponding memory model). It is important
     * for {@link LargeMemoryModel}: if this method returns <code>true</code>, then the content
     * of this array corresponds to the content of an external file according to a known, fully documented scheme.
     * Unlike this, another forms of AlgART arrays &mdash; like {@link #subArray(long, long) subarrays} &mdash;
     * have no documented correspondence with the content of an external data, even when we can retrieve
     * some information about such data (as a name of the disk file {@link LargeMemoryModel#getDataFilePath(Array)}).
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return whether this array instance is a newly created <i>view</i> of some
     * external data, providing <i>read-only</i> access to this data.
     */
    boolean isNewReadOnlyView();

    /**
     * Returns <code>true</code> if this array instance is <i>lazy</i>, i&#46;e&#46;
     * if access to its element means some calculations for producing result or actual saving element.
     * Examples of <i>lazy</i> arrays are results of
     * {@link Arrays#asFuncArray(net.algart.math.functions.Func, Class, PArray...)}
     * and analogous methods.
     *
     * <p>If this method returns <code>false</code>, it means that reading elements
     * (and writing for {@link UpdatableArray updatable arrays}) is performed as quickly as possible:
     * it is just copying data from one memory (maybe a disk) to another.
     * In particular, this method returns <code>false</code> for {@link #isNew new} arrays,
     * their {@link #asImmutable() immutable views}, {@link #subArray(long, long) subarrays}, etc.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return whether this array instance if <i>lazy</i>.
     */
    boolean isLazy();

    /**
     * Returns the byte order used by this array for storing data.
     *
     * <p>This value does not important for using AlgART arrays.
     * The only case when it can be interesting is when the array is stored in some external resources,
     * for example, in a disk file.
     *
     * <p>For all array instances created by this package the byte order is native
     * (<code>ByteOrder.nativeOrder()</code>), with the only exception:
     * arrays, created by {@link LargeMemoryModel#asArray(Object, Class, long, long, ByteOrder)},
     * {@link LargeMemoryModel#asUpdatableArray(Object, Class, long, long, boolean, ByteOrder)} methods
     * and their versions for concrete element types, will have byte order
     * specified by the argument of these methods.
     *
     * <p>Please note: in the combined arrays, created via {@link CombinedMemoryModel},
     * this method returns <code>ByteOrder.nativeOrder()</code>, though the byte order in the
     * underlying storage arrays may be another.
     *
     * <p>This method never returns <code>null</code>.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return the byte order used by this array for storing data.
     */
    ByteOrder byteOrder();

    /**
     * Returns a "shallow" clone of this object:
     * another array consisting of the same elements,
     * but with independent length, start offset, capacity, copy-on-next-write and
     * possible other information about any array characteristics besides its elements.
     * In other words, any changes in the <i>elements</i> of the returned array
     * are usually reflected in this array, and vice-versa; but changes of any other
     * characteristics of the original or returned array, including the length,
     * capacity, etc., will not be reflected in another array.
     *
     * <p>Please note: this method <i>never</i> returns a reference to this object,
     * even if this array is immutable!
     * Moreover, the returned object does not store any references to this instance in any internal fields.
     * It can be important while using weak references and reference queues:
     * this object and its shallow copy are deallocated by the garbage collector separately.
     *
     * <p>There are <i>no guarantees</i> that the returned array will share the elements with this one.
     * Some implementations may return the full (deep) copy of this object, alike
     * {@link #updatableClone(MemoryModel)} or {@link #mutableClone(MemoryModel)} methods.
     * All implementations from this package returns a shallow (non-deep) copy.
     *
     * <p>If modifications of this or returned array characteristics lead to reallocation
     * of the internal storage, then the returned array ceases to be a view of this array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link #asCopyOnNextWrite() copy-on-next-write}.
     *
     * <p>The values of {@link #length()}, {@link #capacity()}, {@link DirectAccessible#javaArrayOffset()},
     * {@link #isCopyOnNextWrite()} in the result will be the same as in this array.
     * The returned instance implements the same set of interfaces as this array.
     *
     * <p>This method is an analog of the standard Java NIO
     * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/nio/ByteBuffer.html#duplicate()"
     * ><code>ByteBuffer.duplicate()</code></a> method. However, unlike <code>ByteBuffer.duplicate()</code>,
     * <i>this method is necessary very rarely</i>. Usually, you need another forms
     * of array views: {@link #asImmutable()}, {@link UpdatableArray#asUnresizable()}, etc.
     * The most often usage of this method is finalization
     * via {@link net.algart.finalizing.Finalizer Finalizer} class:
     * see example in comments to {@link #checkUnallowedMutation()} method. Also, this method
     * can be useful if you need to pass an array with the same content
     * into some another class, but must be sure that further resizing
     * of the source array will not affect to correct work of that class.
     *
     * @return a shallow copy of this object.
     * @see #length()
     * @see #capacity()
     * @see DirectAccessible#javaArrayOffset()
     * @see #isCopyOnNextWrite()
     */
    Array shallowClone();

    /**
     * Returns a mutable resizable copy of this array. This method is equivalent to the following code:
     *
     * <pre>
     * memoryModel.{@link MemoryModel#newArray(Array)
     * newArray}(thisArray).{@link UpdatableArray#copy(Array) copy}(thisArray);
     * </pre>
     *
     * <p>Please note: this method is a good choice for cloning little arrays (thousands,
     * maybe millions elements). If you clone large arrays by this method,
     * the user, in particular, has no ways to view the progress of copying or to interrupt copying.
     * To clone large arrays, we recommend the following code:
     *
     * <pre>
     * MutableArray clone = memoryModel.{@link MemoryModel#newArray(Array)
     * newArray}(thisArray);
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array) Arrays.copy}(someContext, clone, a);
     * </pre>
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @return a mutable copy of this array.
     * @throws NullPointerException            if the argument is <code>null</code>.
     * @throws UnsupportedElementTypeException if <code>thisArray.{@link Array#elementType()}</code> is not supported
     *                                         by the specified memory model.
     * @throws TooLargeArrayException          if the {@link Array#length() length} of this array is too large
     *                                         for this the specified memory model.
     * @see #updatableClone(MemoryModel)
     */
    MutableArray mutableClone(MemoryModel memoryModel);

    /**
     * Returns an unresizable updatable copy of this array.
     * This method is equivalent to the following code:
     *
     * <pre>
     * memoryModel.{@link MemoryModel#newUnresizableArray(Array)
     * newUnresizableArray}(thisArray).{@link UpdatableArray#copy(Array) copy}(thisArray);
     * </pre>
     *
     * <p>Please note: this method is a good choice for cloning little arrays (thousands,
     * maybe millions elements). If you clone large arrays by this method,
     * the user, in particular, has no ways to view the progress of copying or to interrupt copying.
     * To clone large arrays, we recommend the following code:
     *
     * <pre>
     * UpdatableArray clone = memoryModel.{@link MemoryModel#newUnresizableArray(Array)
     * newUnresizableArray}(thisArray);
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array) Arrays.copy}(someContext, clone, a);
     * </pre>
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @return an updatable copy of this array.
     * @throws NullPointerException            if the argument is <code>null</code>.
     * @throws UnsupportedElementTypeException if <code>thisArray.{@link Array#elementType()}</code> is not supported
     *                                         by the specified memory model.
     * @throws TooLargeArrayException          if the {@link Array#length() length} of this array is too large
     *                                         for this the specified memory model.
     * @see #mutableClone(MemoryModel)
     */
    UpdatableArray updatableClone(MemoryModel memoryModel);

    /**
     * Returns <code>true</code> this array is actually a <i>wrapper</i> for
     * standard Java array, like wrappers returned by {@link SimpleMemoryModel#asUpdatableArray(Object)} method.
     * That array is returned by {@link #ja()} method,
     * if and only if this method returns <code>true</code>;
     * otherwise {@link #ja()} method returns a copy of array data.
     *
     * <p>More precisely, this method returns <code>true</code>,
     * if and only if all the following conditions are fulfilled:</p>
     * <ol>
     *     <li><code>thisArray instanceof {@link DirectAccessible}</code>;
     *     let <code>da = (DirectAccessible) thisArray</code>;</li>
     *     <li><code>da.{@link DirectAccessible#hasJavaArray() hasJavaArray()}</code>;</li>
     *     <li><code>da.{@link DirectAccessible#javaArrayOffset() javaArrayOffset()} == 0</code>;</li>
     *     <li><code>array.{@link Array#length() length()} == n</code>, where <code>n</code>
     *     is the length of Java array
     *     <code>da.{@link DirectAccessible#javaArray() javaArray()}</code>:<br>
     *     <code>array.{@link Array#length() length()} == java.lang.reflect.Array.getLength(da.javaArray())</code>
     *     (this condition is usually not fulfilled, for example, in a growing {@link MutableArray}).</li>
     * </ol>
     *
     * <p>In this situation, the specified AlgART array is called <i>a wrapper</i>
     * of the underlying Java array, returned by <code>da.{@link DirectAccessible#javaArray() javaArray()}</code>
     * or by {@link #ja()} method.</p>
     *
     * @return whether this array is a wrapper for standard Java array: direct-accessible array with zero offset and
     * with length, equal to the number of elements of the underlying Java array.
     * @see #ja()
     */
    default boolean isJavaArrayWrapper() {
        return this instanceof DirectAccessible da &&
                da.hasJavaArray() &&
                da.javaArrayOffset() == 0 &&
                java.lang.reflect.Array.getLength(da.javaArray()) == length();
    }

    /**
     * Returns a reference to the underlying Java array <code>ja</code>, if this AlgART array is its wrapper
     * (see {@link #isJavaArrayWrapper()}); otherwise returns
     * <code>{@link Arrays#toJavaArray(Array) Arrays.toJavaArray}(thisObject)</code>.
     *
     * <p>In other words, this method returns a Java-array, absolutely identical to this AlgART array &mdash;
     * having identical length and elements, &mdash; and does this as quickly as possible
     * (unlike {@link Arrays#toJavaArray(Array)}, which always copies the data).</p>
     *
     * <p>This method is equivalent to the following operators:</p>
     * <pre>
     *     thisObject thisArray DirectAccessible da &amp;&amp;
     *                 da.hasJavaArray() &amp;&amp;
     *                 da.javaArrayOffset() == 0 &amp;&amp;
     *                 java.lang.reflect.Array.getLength(da.javaArray()) == thisObject.{@link Array#length()
     *                 length()} ?
     *                 da.javaArray() :
     *                 Arrays.toJavaArray(this);
     * </pre>
     * <p>but works little faster if the first case, "<code>da.javaArray()</code>",
     * is selected (this is a wrapper).</p>
     *
     * <p>There are overridden versions of this method in subinterfaces for specific element types:
     * {@link BitArray#ja()}, {@link CharArray#ja()}},
     * {@link ByteArray#ja()}}, {@link ShortArray#ja()}},
     * {@link IntArray#ja()}}, {@link LongArray#ja()}},
     * {@link FloatArray#ja()}}, {@link DoubleArray#ja()}},
     * {@link ObjectArray#ja()}}.</p>
     *
     * <p>Be careful: this method can be potentially unsafe while inaccurate usage!
     * The typical purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the results of such modification can be different: this may change the original AlgART array,
     * but may also not change. (Of course, this is impossible for {@link #isImmutable() immutable} arrays.)</p>
     *
     * <p>Therefore, if you only need to read the array data,
     * you <b>should not</b> attempt to modify the Java array
     * returned by this method: this will help to avoid difficult bugs.</p>
     *
     * <p>If you really want to modify array data, you may do this by updating the returned Java array, but
     * you <b>must</b> follow two conditions:
     * 1) this object must be an instance {@link UpdatableArray} of its subinterfaces;
     * 2) {@link #isJavaArrayWrapper()} method must return <code>true</code>.</p>
     *
     * <p>Note that usually you <b>should</b> prefer methods of {@link DirectAccessible} interface
     * instead of this method, because that interface allows to quickly process sub-arrays
     * with non-zero start offsets and mutable arrays, for which the length of underlying Java array (capacity)
     * is usually greater than the actual AlgART array {@link #length() length}.
     * But if you are sure that your array is created by {@link SimpleMemoryModel} and is not a sub-array,
     * this method provides the simplest way to receive an identical Java array maximally quickly.
     *
     * @return Java array, equivalent to this AlgART array.
     * @see DirectAccessible
     * @see #isJavaArrayWrapper()
     * @see PArray#jaByte()
     * @see PArray#jaShort()
     * @see PArray#jaInt()
     * @see PArray#jaLong()
     * @see PArray#jaFloat()
     * @see PArray#jaDouble()
     */
    Object ja();

    /**
     * Equivalent to <code>{@link Matrices#matrix(Array, long[]) matrix}(thisArray, dim)</code>.
     *
     * @param dim the matrix dimensions.
     * @return new matrix backed by <code>array</code> with the given dimensions.
     * @throws NullPointerException     if <code>array</code> or <code>dim</code> argument is <code>null</code>.
     * @throws IllegalArgumentException if the passed array is resizable
     *                                  (for example, implements {@link MutableArray}),
     *                                  or if the number of dimensions is 0 (empty <code>dim</code> Java array),
     *                                  or if some of the dimensions are negative.
     * @throws SizeMismatchException    if the product of all dimensions is not equal to the array length.
     * @throws TooLargeArrayException   if the product of all dimensions is greater than <code>Long.MAX_VALUE</code>.
     */
    default Matrix<? extends Array> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }

    /**
     * Equivalent to <code>{@link #loadResources(ArrayContext) loadResources}(null)</code>.
     * This is the most typical case of loading resources.
     *
     * @see #loadResources(ArrayContext)
     * @see #freeResources(ArrayContext)
     */
    default void loadResources() {
        loadResources(null);
    }

    /**
     * If there are some external resources, associated with this array, &mdash;
     * files, streams, sockets, locks, etc&#46; &mdash;
     * this method makes an effort to ensure that, when it returns,
     * the content of this array will be resident in physical memory.
     * In other words, this method tries to preload the content of this array into RAM
     * to provide the fastest access to its elements in the nearest future.
     *
     * <p>There are no any guarantees that <i>all</i> elements of this array will be really preloaded into RAM.
     * This method usually avoids loading too large amount of data, comparable with the whole amount of RAM.
     * For example, if the size of this array is 10 GB, it is possible only first several megabytes
     * or tens of megabytes will be preloaded.
     * If you work with a large array, we recommend to call this method
     * for a not too large its {@link #subArray subarray}, which really should be processed now, for example:
     *
     * <pre>
     * final int blockSize = 1048576;
     * for (long pos = 0, n = array.{@link #length() length()}; pos &lt; n; pos += blockSize) {
     * &#32;   int len = (int)Math.min(blockSize, n - pos);
     * &#32;   Array region = array.{@link #subArr subArr}(pos, len);
     * &#32;   region.loadResources(someContext)};
     * &#32;   // some algorithm processing len elements of this region
     * }
     * </pre>
     *
     * <p>If the resources, associated with this array, are shared with some another arrays
     * (usually when one array is a view of another one), this method
     * tries to preload the content of that arrays, though it is not guaranteed.
     * This method usually does nothing if the {@link #length() length} of this array is zero.
     *
     * <p>The <code>context</code> argument is necessary to allow user to interrupt this method and to view
     * the execution progress. Namely, if this argument is not <code>null</code>, this method probably calls
     * <code>context.{@link ArrayContext#checkInterruption() checkInterruption}</code> and
     * <code>context.{@link ArrayContext#updateProgress updateProgress}</code> methods from time to time.
     * It may be useful if preloading data can require long time, for example, the content should be
     * loaded from Internet.
     * You always may pass <code>null</code> as this argument; then all will work correctly,
     * but, maybe, the user will be bored for some time.
     * For all arrays, created by this package, this method works quickly enough
     * and doesn't require non-null context.
     *
     * <p>This method does nothing for arrays created by the {@link SimpleMemoryModel simple memory model}
     * and the {@link BufferMemoryModel buffer memory model}, and also for constant arrays
     * created by {@link Arrays#nByteCopies}, {@link Arrays#nCharCopies}, etc.:
     * these arrays have no associated resources.
     *
     * @param context the context of execution; can be <code>null</code>, then it will be ignored.
     * @see #freeResources(ArrayContext)
     * @see #flushResources(ArrayContext)
     * @see #flushResources(ArrayContext, boolean)
     */
    void loadResources(ArrayContext context);

    /**
     * Equivalent to <code>{@link #flushResources(ArrayContext, boolean) flushResources}(null, false)</code>.
     * This is the most typical case of flushing resources.
     *
     * @see #loadResources(ArrayContext)
     * @see #freeResources(ArrayContext)
     */
    default void flushResources() {
        flushResources(null, false);
    }

    /**
     * Equivalent to <code>{@link #flushResources(ArrayContext, boolean) flushResources}(context, false)</code>.
     *
     * @param context the context of execution; can be <code>null</code>, then it will be ignored.
     * @see #loadResources(ArrayContext)
     * @see #freeResources(ArrayContext)
     */
    default void flushResources(ArrayContext context) {
        flushResources(context, false);
    }

    /**
     * If there are some external resources, associated with this array, &mdash;
     * files, streams, sockets, locks, etc&#46; &mdash;
     * and some array data are not still reflected in that resources
     * (for example, were not saved in the disk file yet),
     * this method <i>flushes</i> all these data to the external devices.
     *
     * <p>This method may not perform <i>immediate</i> writing data to the storage devices.
     * But it guarantees that:<ol>
     *
     * <li>if some AlgART array will be created and associated with the same resources as this array
     * &mdash; for example, will be mapped to the same external file &mdash; all data stored
     * in this array will be "visible" in the new array;
     * <br>&nbsp;</li>
     *
     * <li>if this array is a view of some external resource, that is not "temporary"
     * (i.e. will not be automatically deleted after shutting down JVM),
     * then all changes, made in this array, will be really stored in that resource
     * and will be able to be loaded by another software, at least, after shutting down JVM.
     * </li>
     * </ol>
     *
     * <p>If <code>forcePhysicalWriting</code> argument is <code>false</code>, this method works as quick as possible.
     *
     * <p>If <code>forcePhysicalWriting</code> argument is <code>true</code>, this
     * method tries to physically flush all unsaved elements of this array to the storage device.
     * The precise actions, performed in this case, are not specified.
     * The typical behaviour: all internal caches, if they are provided by Java implementation of the AlgART array,
     * are written to the external device via OS calls, and OS is requested to flush buffers or file-mapping
     * to the physical disk.
     * The mode <code>forcePhysicalWriting=true</code> increases chances that the data will be really flushed to
     * external devices and, so, OS will release physical memory, which was probably used for disk or another cache.
     * This mode also increases chances that all changes, made in this array until this moment,
     * will be immediately "visible" in another software (another OS process) as changes in the corresponding
     * external resources (for example, in the disk file).
     *
     * <p>If the resources, associated with this array, are shared with some another arrays
     * (usually when one array is a view of another one), this method still
     * <i>does flush these resource</i>.
     *
     * <p>You may use {@link #subArray(long, long)} / {@link #subArr(long, long)} methods
     * to flush any portion of this array, for example:<pre>
     *     array.subArr(destPos, count).flushResources(context, false);</pre>
     * <p>But there is no guarantee that flushing a subarray will not lead to flushing some other
     * parts of the source array.
     *
     * <p>In particular, please note: this method may do something even for immutable arrays.
     * If an array is an {@link #asImmutable() immutable view} of another array <code><b>a</b></code>,
     * flushing this view is equivalent to flushing the original array <code><b>a</b></code>.
     *
     * <p>The <code>context</code> argument is necessary to allow user to interrupt this method and to view
     * the execution progress. Namely, if this argument is not <code>null</code>, this method probably calls
     * <code>context.{@link ArrayContext#checkInterruption() checkInterruption}</code> and
     * <code>context.{@link ArrayContext#updateProgress updateProgress}</code> methods from time to time.
     * It may be useful if this array is very large and writing non-flushed data to an external device
     * requires long time. For example, it is possible for arrays, created by
     * {@link LargeMemoryModel#newLazyCopy(Array) LargeMemoryModel.newLazyCopy} method.
     * You always may pass <code>null</code> as this argument; then all will work correctly,
     * but, maybe, the user will be bored for some time.
     *
     * <p>This method does nothing for arrays created by the {@link SimpleMemoryModel simple memory model}
     * and the {@link BufferMemoryModel buffer memory model}, and also for constant arrays
     * created by {@link Arrays#nByteCopies}, {@link Arrays#nCharCopies}, etc.:
     * these arrays have no associated resources.
     *
     * <p>All operations, performed by this method, are also performed by
     * {@link #freeResources(ArrayContext, boolean)} method with
     * the same <code>forcePhysicalWriting</code> argument.
     *
     * <p><b>Performance note:</b> please avoid sequential calls of this method, like the following:<pre>
     *     array.flushResources(null, false);
     *     array.flushResources(null, false); // - unnecessary call
     * </pre>
     * <p>The second call here is not necessary, because all data are already flushed while the first call &mdash;
     * however, it is still possible that the second call will spend time for writing data again t
     * o an external device.
     *
     * @param context              the context of execution; can be <code>null</code>, then it will be ignored.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device.
     * @see #loadResources(ArrayContext)
     * @see #flushResources(ArrayContext)
     * @see #freeResources(ArrayContext, boolean)
     */
    void flushResources(ArrayContext context, boolean forcePhysicalWriting);

    /**
     * Equivalent to <code>{@link #freeResources(ArrayContext, boolean) freeResources}(null, false)</code>.
     * This is the most typical case of freeing resources.
     *
     * @see #loadResources(ArrayContext)
     * @see #flushResources(ArrayContext)
     */
    default void freeResources() {
        freeResources(null, false);
    }

    /**
     * Equivalent to <code>{@link #freeResources(ArrayContext, boolean) freeResources}(context, false)</code>.
     *
     * @param context the context of execution; can be <code>null</code>, then it will be ignored.
     * @see #loadResources(ArrayContext)
     * @see #flushResources(ArrayContext)
     */
    default void freeResources(ArrayContext context) {
        freeResources(context, false);
    }

    /**
     * If there are some resources, associated with this array, which are not controlled
     * by Java garbage collectors &mdash; files, streams, sockets, locks, etc&#46; &mdash;
     * this method tries to release them (for example, to close any files).
     * However, the array stays alive: if the resources will be necessary for following array operations,
     * they will be automatically re-acquired.
     *
     * <p>As a part of the work, this method always performs the same actions as
     * <code>{@link #flushResources(ArrayContext, boolean) flushResources}(context, forcePhysicalWriting)</code>
     * call with the same <code>forcePhysicalWriting</code> argument. See comments to that method for more details
     * about flushing external resources.
     *
     * <p>This method guarantees that any changes, made after its call in any external resources,
     * associated with this array, will be "visible" in this array while any accesses to it
     * after these changes. For example, if this array is a view of some disk file,
     * you need to call this method to be sure that any changes, performed in the file
     * by another application <i>after</i> this call and <i>before</i> the next access to this array,
     * will be successfully reflected in this array.
     *
     * <p>If the resources, associated with this array, are shared with some another arrays
     * (usually when one array is a view of another one), this method still
     * <i>does release these resource</i>.
     * It means that calling this method can slow down the next access not only to this array,
     * but also to another ones, for example, to its {@link #subArray(long, long) subarrays}.
     *
     * <p>The <code>context</code> argument is necessary to allow user to interrupt this method and to view
     * the execution progress. Namely, if this argument is not <code>null</code>, this method probably calls
     * <code>context.{@link ArrayContext#checkInterruption() checkInterruption}</code> and
     * <code>context.{@link ArrayContext#updateProgress updateProgress}</code> methods from time to time.
     * It may be useful if this array is very large and writing non-flushed data to an external device
     * requires long time. For example, it is possible for arrays, created by
     * {@link LargeMemoryModel#newLazyCopy(Array) LargeMemoryModel.newLazyCopy} method.
     * You always may pass <code>null</code> as this argument; then all will work correctly,
     * but, maybe, the user will be bored for some time.
     *
     * <p>This method does nothing for arrays created by the {@link SimpleMemoryModel simple memory model}
     * and the {@link BufferMemoryModel buffer memory model}, and also for constant arrays
     * created by {@link Arrays#nByteCopies Arrays.nByteCopies}, {@link Arrays#nCharCopies Arrays.nCharCopies}, etc.:
     * these arrays have no associated resources.
     *
     * <p><b>Performance note 1:</b> you may use the call<pre>
     *     array.{@link #subArray(long, long) subArray(0,0)}.freeResources(null, false)
     * </pre>
     * to release all resources, associated with the original <code>array</code>.
     * It may work <i>faster</i> than simple
     * "<code>array.freeResources(null,false)</code>",
     * because there is no necessity to <i>flush all array elements</i>:
     * releasing a subarray requires flushing only the subarray elements,
     * i.e. no elements in a case of the zero-length subarray.
     * This speeding-up is guaranteed for arrays, created by {@link LargeMemoryModel large memory model},
     * but is not guaranteed for other arrays: it is still possible that
     * "<code>array.{@link #subArray(long, long) subArray(0,0)}.freeResources(context,false)</code>"
     * call will flush <i>all</i> <code>array</code> data.
     *
     * <p><b>Performance note 2:</b> please avoid calling {@link #flushResources(ArrayContext, boolean)}
     * together with this method:<pre>
     *     array.flushResources(null, false); // - unnecessary call
     *     array.freeResources(null, false);
     * </pre>
     * <p>or<pre>
     *     array.flushResources(null, true);
     *     array.freeResources(null);
     *     // - it's much better to use a single call "array.freeResources(null, true)"
     * </pre>
     * <p>It is very possible that the call of <code>freeResources</code> method will spend time for flushing data again,
     * though they were already flushed by the previous <code>flushResources</code> method.
     * If is much better to perform all the work in a single call of this method with the corresponding
     * <code>forcePhysicalWriting</code> argument.
     *
     * <p>All resources allocated by this package are automatically released and &mdash;
     * for temporary resources, i.e. garbage, &mdash;
     * removed (as it is possible) by built-in cleanup procedures while JVM termination.
     * However, we recommend to directly call
     * <code>freeResources(context)</code> at the end of methods
     * that create and process large AlgART arrays.
     * The reason is that if there will be a lot of non-released large arrays, the automatic cleanup procedure
     * may strongly reduce the speed of closing the application.
     *
     * <p><b>Important:</b> you <i>must</i> use this method if you are working with some collection
     * (like <code>java.util.List</code> or a usual Java array) of large AlgART arrays
     * (each per many megabytes or gigabytes) <i>and</i> if this collection contains many elements:
     * more than several tens. If an AlgART array has some associated resources, for example,
     * like arrays created by {@link LargeMemoryModel} and mapped on a disk file, then every such instance
     * usually occupies some RAM (and also, maybe, OS address space for mapping),
     * usually up to several tens of megabytes (for large arrays) for caching and similar needs.
     * This RAM cannot be automatically released by the garbage collector until you call this method.
     * So, you <i>must</i> manually release all resources by calling this method every time,
     * when this array is already not necessary, but should be stored in some collection for the future.
     * In other case, thousands of inactive instances of AlgART arrays with non-released resources
     * can exhaust all available RAM (or address space).
     *
     * @param context              the context of execution; can be <code>null</code>, then it will be ignored.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device.
     * @see #loadResources(ArrayContext context)
     * @see #flushResources(ArrayContext context, boolean forcePhysicalWriting)
     * @see #freeResources(ArrayContext)
     * @see Arrays#freeAllResources()
     */
    void freeResources(ArrayContext context, boolean forcePhysicalWriting);

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * a short description of the array length, capacity, element type.
     *
     * <p>Note: for {@link CharArray character arrays}, unlike <code>CharSequence.toString()</code>,
     * this method works as for all other array types.
     * If you need to convert a character array to a string,
     * containing all characters of the array, you may use
     * {@link Arrays#toString(CharArray)} method.
     *
     * @return a brief string description of this object.
     */
    String toString();

    /**
     * Returns the hash code of this array. The result depends on all elements of the array
     * (from element <code>#0</code> to element <code>#{@link #length()}-1</code>).
     *
     * <p>For non-primitive element type ({@link ObjectArray}, {@link UpdatableObjectArray},
     * {@link MutableObjectArray} subinterfaces), the result is always based on implementation
     * of <code>hashCode</code> method in the class of elements ({@link #elementType()}).
     *
     * @return the hash code of this array.
     */
    int hashCode();

    /**
     * Indicates whether some other array is equal to this one.
     * Returns <code>true</code> if and only if:<ol>
     * <li>the specified object is an array (i.e. implements {@link Array}),</li>
     * <li>both arrays have the same {@link #length() length},</li>
     * <li>for arrays of primitive elements
     * ({@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray}, {@link DoubleArray}):
     * both arrays have the same {@link #elementType() element type}
     * and all corresponding pairs of elements are equal
     * (for <code>float</code> and <code>double</code> elements,
     * unlike the <code>==</code> operator, this method considers
     * <code>NaN</code> equals to itself, and 0.0 unequal to -0.0);</li>
     * <li>for non-primitive arrays (when both arrays implement {@link ObjectArray}),
     * the method may check one from two following conditions
     * (only 1st or only 2nd, depending on implementation):<ul>
     * <li>both arrays have the same {@link #elementType() element type}
     * and all corresponding pairs of elements contain identical data
     * (<code>equals</code> method of the class of elements
     * is not used in this case: {@link CombinedMemoryModel combined arrays} are an example);</li>
     * <li>for all corresponding pairs of elements <code>e1</code>, <code>e2</code>
     * (<code>e1</code> is an element <code>#i</code> of this array,
     * <code>e2</code> is an element <code>#i</code> of the <code>obj</code> argument,
     * <code>i=0,1,...,{@link #length() length()}-1)</code>, the following check returns <code>true</code>:
     * <code>(e1==null ? e2==null : e1.equals(e2))</code>.</li>
     * </ul></li>
     * </ol>
     *
     * @param obj the object to be compared for equality with this array.
     * @return <code>true</code> if the specified object is an array equal to this one.
     */
    boolean equals(Object obj);
}
