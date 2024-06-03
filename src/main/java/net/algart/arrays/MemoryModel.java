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
 * <p>Virtual memory model: implementations of this abstract factory is
 * the main way of creating new AlgART arrays.</p>
 *
 * <p>Virtual memory model is an abstraction describing how the array elements are stored in memory.
 * For example, the simplest memory model is {@link SimpleMemoryModel}, that use Java arrays for storing array
 * elements (alike <tt>java.util.ArrayList</tt>). Another example is {@link LargeMemoryModel}:
 * memory model based on direct byte buffers created via <tt>java.nio.ByteBuffer.allocateDirect</tt>
 * method or via mapping disk files.</p>
 *
 * <p>The arrays, created with help of different memory models, implement identical interfaces
 * and usually look absolutely identical for an application, but may work with different
 * performance. For example, if you need an array of simple object elements
 * (as "circles" or "points"), you may choose {@link CombinedMemoryModel} memory model to save memory
 * and provide quick memory allocation,
 * or {@link SimpleMemoryModel} to provide maximal performance while accessing array elements.</p>
 *
 * <p>Some arrays may have no well-defined memory model, but be created without any factories.
 * The simples example is a constant arrays created by methods
 * {@link Arrays#nByteCopies(long, byte)},
 * {@link Arrays#nCharCopies(long, char)}, etc.</p>
 *
 * <p>Objects implementing this interface may be not <b>immutable</b>,
 * though all its implementations in this package are immutable.
 * But there is a guarantee that any memory model <b>is thread-safe</b> and
 * can be used simultaneously in several threads. (In other words,
 * if another package implements a custom mutable memory model,
 * it must be internally synchronized.)
 *
 * @author Daniel Alievsky
 */
public interface MemoryModel {

    /**
     * Allocates an empty resizable array with the specified element type and a little initial capacity.
     * It is equivalent to {@link #newEmptyArray(Class, long) newEmptyArray(elementType, n)}, where
     * <tt>n</tt> is some little value.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableFloatArray a = (MutableFloatArray)memoryModel.newEmptyArray(float.class);
     * </pre>
     *
     * @param elementType the type of array elements.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if <tt>elementType</tt> is not supported of <tt>void.class</tt>.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newArray(Class, long)
     */
    MutableArray newEmptyArray(Class<?> elementType);

    /**
     * Allocates an empty resizable array with the specified element type and initial capacity.
     *
     * <p>The element type can be either usual object class (as <tt>String.class</tt>),
     * or one of the primitive types: <tt>boolean.class</tt>,
     * <tt>byte.class</tt>, <tt>short.class</tt>, <tt>int.class</tt>,
     * <tt>long.class</tt>, <tt>float.class</tt>, <tt>double.class</tt>,
     * <tt>char.class</tt>. The element type cannot be <tt>void.class</tt>.
     *
     * <p>In a case of primitive types, the created array
     * will implement the corresponding interface {@link BitArray},
     * {@link ByteArray}, {@link ShortArray}, {@link IntArray},
     * {@link LongArray}, {@link FloatArray}, {@link DoubleArray} or
     * {@link CharArray}. In this case, the created array
     * (unlike standard <tt>ArrayList</tt>) will occupy the same amount of memory
     * as the Java array <tt>boolean[initialCapacity]</tt>, <tt>byte[initialCapacity]</tt>, etc.
     *
     * <p>In a case of non-primitive types (<tt>Object</tt> inheritors), the created array
     * will implement the {@link MutableObjectArray} interface.
     *
     * <p>Some element type may be not supported by this memory model.
     * For example, some memory models may support only primitive types, or only one concrete type.
     * In such a case, {@link UnsupportedElementTypeException} will be thrown.
     *
     * <p>Some too large array capacities may be not supported by this memory model.
     * For example, {@link SimpleMemoryModel} does not support arrays larger than 0x7FFFFFFF
     * (<tt>Integer.MAX_VALUE</tt>) elements.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableFloatArray a = (MutableFloatArray)memoryModel.newEmptyArray(float.class, 10000);
     * </pre>
     *
     * @param elementType     the type of array elements.
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if <tt>elementType</tt> is <tt>void.class</tt>
     *                                         or if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newArray(Class, long)
     */
    MutableArray newEmptyArray(Class<?> elementType, long initialCapacity);

    /**
     * Allocates a zero-filled resizable array with the specified element type and initial length.
     * The capacity of new array will be equal to its length.
     *
     * <p>This method is equivalent to the following call:
     * <tt>{@link #newEmptyArray(Class, long) newEmptyArray(elementType, initialLength)}.{@link
     * MutableArray#length(long) length(initialLength)}.{@link MutableArray#trim() trim()}</tt>.
     *
     * @param elementType   the type of array elements.
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if <tt>elementType</tt> is <tt>void.class</tt>
     *                                         or if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyArray(Class, long)
     * @see #newUnresizableArray(Class, long)
     */
    MutableArray newArray(Class<?> elementType, long initialLength);

    /**
     * Allocates a zero-filled unresizable array with the specified element type and length.
     * The capacity of new array will be equal to its length.
     *
     * <p>The analogous result may be obtained the following call:
     * <tt>{@link #newArray(Class, long) newArray(elementType, length)}.{@link
     * MutableArray#asUnresizable() asUnresizable()}</tt>.
     * However, we don't recommend to use such code.
     * If you are sure that you will not need to change the array length,
     * please always use this method
     * (or {@link #newUnresizableBitArray(long) newUnresizableBitArray},
     * {@link #newUnresizableByteArray(long) newUnresizableBteArray}, etc.).
     * In some memory models, creating resizable array with the given length
     * may require much more resources that creating unresizable one.
     * For example, in the {@link LargeMemoryModel large memory model}
     * every resizable array is stored in the file consisting of integer number
     * of blocks per {@link DataFileModel#recommendedBankSize(boolean)
     * DataFileModel.recommendedBankSize(true)} bytes.
     *
     * @param elementType the type of array elements.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if <tt>elementType</tt> is <tt>void.class</tt>
     *                                         or if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newArray(Class, long)
     */
    UpdatableArray newUnresizableArray(Class<?> elementType, long length);

    /**
     * Equivalent to <tt>{@link #newArray(Class, long)
     * newArray}(array.{@link Array#elementType() elementType()}, array.{@link Array#length()
     * length()})</tt>.
     *
     * @param array the pattern array.
     * @return new AlgART array with the same length and element type.
     * @throws NullPointerException            if <tt>array</tt> is <tt>null</tt>.
     * @throws UnsupportedElementTypeException if <tt>array.elementType()</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the {@link Array#length() length} of passed array is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     */
    MutableArray newArray(Array array);

    /**
     * Equivalent to <tt>{@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(array.{@link Array#elementType() elementType()}, array.{@link Array#length()
     * length()})</tt>.
     *
     * @param array the pattern array.
     * @return new unresizable AlgART array with the same length and element type.
     * @throws NullPointerException            if <tt>array</tt> is <tt>null</tt>.
     * @throws UnsupportedElementTypeException if <tt>array.elementType()</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the {@link Array#length() length} of passed array is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     */
    UpdatableArray newUnresizableArray(Array array);

    /*Repeat() boolean           ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit               ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
               bit               ==> char,,byte,,short,,int,,long,,float,,double */

    /**
     * Equivalent to <tt>(MutableBitArray){@link #newEmptyArray(Class) newEmptyArray}(boolean.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableBitArray a = memoryModel.newEmptyBitArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>boolean</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newBitArray(long)
     */
    MutableBitArray newEmptyBitArray();

    /**
     * Equivalent to <tt>(MutableBitArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(boolean.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableBitArray a = memoryModel.newEmptyBitArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>boolean</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newBitArray(long)
     */
    MutableBitArray newEmptyBitArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableBitArray){@link #newArray(Class, long)
     * newArray}(boolean.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>boolean</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyBitArray(long)
     */
    MutableBitArray newBitArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableBitArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(boolean.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>boolean</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyBitArray(long)
     */
    UpdatableBitArray newUnresizableBitArray(long length);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <tt>(MutableCharArray){@link #newEmptyArray(Class) newEmptyArray}(char.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableCharArray a = memoryModel.newEmptyCharArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>char</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newCharArray(long)
     */
    MutableCharArray newEmptyCharArray();

    /**
     * Equivalent to <tt>(MutableCharArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(char.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableCharArray a = memoryModel.newEmptyCharArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>char</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newCharArray(long)
     */
    MutableCharArray newEmptyCharArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableCharArray){@link #newArray(Class, long)
     * newArray}(char.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>char</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyCharArray(long)
     */
    MutableCharArray newCharArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableCharArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(char.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>char</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyCharArray(long)
     */
    UpdatableCharArray newUnresizableCharArray(long length);


    /**
     * Equivalent to <tt>(MutableByteArray){@link #newEmptyArray(Class) newEmptyArray}(byte.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableByteArray a = memoryModel.newEmptyByteArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>byte</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newByteArray(long)
     */
    MutableByteArray newEmptyByteArray();

    /**
     * Equivalent to <tt>(MutableByteArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(byte.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableByteArray a = memoryModel.newEmptyByteArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>byte</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newByteArray(long)
     */
    MutableByteArray newEmptyByteArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableByteArray){@link #newArray(Class, long)
     * newArray}(byte.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>byte</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyByteArray(long)
     */
    MutableByteArray newByteArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableByteArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(byte.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>byte</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyByteArray(long)
     */
    UpdatableByteArray newUnresizableByteArray(long length);


    /**
     * Equivalent to <tt>(MutableShortArray){@link #newEmptyArray(Class) newEmptyArray}(short.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableShortArray a = memoryModel.newEmptyShortArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>short</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newShortArray(long)
     */
    MutableShortArray newEmptyShortArray();

    /**
     * Equivalent to <tt>(MutableShortArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(short.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableShortArray a = memoryModel.newEmptyShortArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>short</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newShortArray(long)
     */
    MutableShortArray newEmptyShortArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableShortArray){@link #newArray(Class, long)
     * newArray}(short.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>short</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyShortArray(long)
     */
    MutableShortArray newShortArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableShortArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(short.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>short</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyShortArray(long)
     */
    UpdatableShortArray newUnresizableShortArray(long length);


    /**
     * Equivalent to <tt>(MutableIntArray){@link #newEmptyArray(Class) newEmptyArray}(int.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableIntArray a = memoryModel.newEmptyIntArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>int</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newIntArray(long)
     */
    MutableIntArray newEmptyIntArray();

    /**
     * Equivalent to <tt>(MutableIntArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(int.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableIntArray a = memoryModel.newEmptyIntArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>int</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newIntArray(long)
     */
    MutableIntArray newEmptyIntArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableIntArray){@link #newArray(Class, long)
     * newArray}(int.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>int</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyIntArray(long)
     */
    MutableIntArray newIntArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableIntArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(int.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>int</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyIntArray(long)
     */
    UpdatableIntArray newUnresizableIntArray(long length);


    /**
     * Equivalent to <tt>(MutableLongArray){@link #newEmptyArray(Class) newEmptyArray}(long.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableLongArray a = memoryModel.newEmptyLongArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>long</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newLongArray(long)
     */
    MutableLongArray newEmptyLongArray();

    /**
     * Equivalent to <tt>(MutableLongArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(long.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableLongArray a = memoryModel.newEmptyLongArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>long</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newLongArray(long)
     */
    MutableLongArray newEmptyLongArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableLongArray){@link #newArray(Class, long)
     * newArray}(long.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>long</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyLongArray(long)
     */
    MutableLongArray newLongArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableLongArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(long.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>long</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyLongArray(long)
     */
    UpdatableLongArray newUnresizableLongArray(long length);


    /**
     * Equivalent to <tt>(MutableFloatArray){@link #newEmptyArray(Class) newEmptyArray}(float.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableFloatArray a = memoryModel.newEmptyFloatArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>float</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newFloatArray(long)
     */
    MutableFloatArray newEmptyFloatArray();

    /**
     * Equivalent to <tt>(MutableFloatArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(float.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableFloatArray a = memoryModel.newEmptyFloatArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>float</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newFloatArray(long)
     */
    MutableFloatArray newEmptyFloatArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableFloatArray){@link #newArray(Class, long)
     * newArray}(float.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>float</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyFloatArray(long)
     */
    MutableFloatArray newFloatArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableFloatArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(float.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>float</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyFloatArray(long)
     */
    UpdatableFloatArray newUnresizableFloatArray(long length);


    /**
     * Equivalent to <tt>(MutableDoubleArray){@link #newEmptyArray(Class) newEmptyArray}(double.class)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableDoubleArray a = memoryModel.newEmptyDoubleArray();
     * </pre>
     *
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if <tt>double</tt> element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newDoubleArray(long)
     */
    MutableDoubleArray newEmptyDoubleArray();

    /**
     * Equivalent to <tt>(MutableDoubleArray){@link #newEmptyArray(Class, long)
     * newEmptyArray}(double.class, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableDoubleArray a = memoryModel.newEmptyDoubleArray(10000);
     * </pre>
     *
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if <tt>double</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newDoubleArray(long)
     */
    MutableDoubleArray newEmptyDoubleArray(long initialCapacity);

    /**
     * Equivalent to <tt>(MutableDoubleArray){@link #newArray(Class, long)
     * newArray}(double.class, initialLength)</tt>.
     *
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <tt>double</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyDoubleArray(long)
     */
    MutableDoubleArray newDoubleArray(long initialLength);

    /**
     * Equivalent to <tt>(UpdatableDoubleArray){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(double.class, initialLength)</tt>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <tt>double</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyDoubleArray(long)
     */
    UpdatableDoubleArray newUnresizableDoubleArray(long length);
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <tt>(MutableObjectArray&lt;E&gt;){@link #newEmptyArray(Class) newEmptyArray}(elementType)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableObjectArray&lt;String&gt; a =
     * &nbsp;&nbsp;&nbsp;&nbsp;memoryModel.&lt;String&gt;newEmptyObjectArray(Class elementType);
     * </pre>
     *
     * @param <E>         the generic type of array elements.
     * @param elementType the type of array elements.
     * @return created AlgART array.
     * @throws UnsupportedElementTypeException if this element type is not supported by this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newObjectArray(Class elementType, long)
     */
    <E> MutableObjectArray<E> newEmptyObjectArray(Class<E> elementType);

    /**
     * Equivalent to <tt>(MutableObjectArray&lt;E&gt;){@link #newEmptyArray(Class, long)
     * newEmptyArray}(elementType, initialCapacity)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;MutableObjectArray&lt;String&gt; a =
     * &nbsp;&nbsp;&nbsp;&nbsp;memoryModel.&lt;String&gt;newEmptyObjectArray(10000);
     * </pre>
     *
     * @param <E>             the generic type of array elements.
     * @param elementType     the type of array elements.
     * @param initialCapacity the initial capacity of the array.
     * @return created AlgART array.
     * @throws IllegalArgumentException        if the specified initial capacity is negative.
     * @throws UnsupportedElementTypeException if this element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial capacity is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newObjectArray(Class elementType, long)
     */
    <E> MutableObjectArray<E> newEmptyObjectArray(Class<E> elementType, long initialCapacity);

    /**
     * Equivalent to <tt>(MutableObjectArray&lt;E&gt;){@link #newArray(Class, long)
     * newArray}(elementType, initialLength)</tt>.
     *
     * @param <E>           the generic type of array elements.
     * @param elementType   the type of array elements.
     * @param initialLength the initial length and capacity of the array.
     * @return created AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if this element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified initial length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyObjectArray(Class elementType, long)
     */
    <E> MutableObjectArray<E> newObjectArray(Class<E> elementType, long initialLength);

    /**
     * Equivalent to <tt>(UpdatableObjectArray&lt;E&gt;){@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(elementType, initialLength)</tt>.
     *
     * @param <E>         the generic type of array elements.
     * @param elementType the type of array elements.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if this element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newEmptyObjectArray(Class elementType, long)
     */
    <E> UpdatableObjectArray<E> newUnresizableObjectArray(Class<E> elementType, long length);

    /**
     * Allocates a resizable array with the same content as the passed one, where the actual copying the elements
     * will be deferred as long as possible, maybe, until the first access to elements.
     * The capacity of new array will be equal to its length (i.e. <tt>array.length()</tt>).
     *
     * <p>More precisely, this method does the same things as the following operators:
     *
     * <pre>
     * MutableArray result = thisMemoryModel.{@link #newArray(Class, long)
     * newArray}(array.{@link Array#elementType() elementType()}, array.{@link Array#length() length()});
     * result.{@link UpdatableArray#copy(Array) copy}(array); // now the result is the copy of the source array
     * </pre>
     * <p>with the only difference that the copying, maybe, will not be performed immediately.
     * If you will not modify the source <tt>array</tt>, the array, returned by this method,
     * will work absolutely identically to the <tt>result</tt> array in the example, listed above.
     * If you will modify the source <tt>array</tt> after calling this method,
     * the precise content of the returned array will be unspecified.
     *
     * <p>Any changes, performed in the returned array, will never be reflected in the source <tt>array</tt>.
     * In other words, any corrections in the returned lazy copy do not affect the source <tt>array</tt>.
     *
     * <p>The {@link Array#flushResources(ArrayContext)} and {@link Array#freeResources(ArrayContext)} methods,
     * called in the returned array, guarantee completion of all deferred copying.
     * Since calling those methods, the returned array always becomes the actual, not lazy copy of the source one.
     *
     * <p>The described lazy copying is supported not by all memory models.
     * In this package, only {@link LargeMemoryModel} supports this.
     * Other memory models of this package inherit implementation from {@link AbstractMemoryModel#newLazyCopy(Array)},
     * where this method just performs the actual copying as described in the code example above.
     *
     * @param array the source array.
     * @return the lazy copy of the source array, if lazy copying is supported by the memory model,
     * or the usual identical copy of the source array in other case.
     * @throws NullPointerException            if the argument is <tt>null</tt>.
     * @throws UnsupportedElementTypeException if the element type of the passed array
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the length of the passed array is too large for this memory model.
     */
    MutableArray newLazyCopy(Array array);

    /**
     * Equivalent to {@link #newLazyCopy(Array)} method with the only difference, that the returned array
     * is unresizable.
     * The capacity of new array will be equal to its length (i.e. <tt>array.length()</tt>).
     *
     * <p>As well as for {@link #newLazyCopy(Array)}, this method does the same things as the following operators:
     *
     * <pre>
     * UpdatableArray result = thisMemoryModel.{@link #newUnresizableArray(Class, long)
     * newUnresizableArray}(array.{@link Array#elementType() elementType()}, array.{@link Array#length() length()});
     * result.{@link UpdatableArray#copy(Array) copy}(array); // now the result is the copy of the source array
     * </pre>
     * <p>with the only difference that the copying, maybe, will be deferred.
     *
     * <p>The analogous result may be obtained the following call:
     * <tt>{@link #newLazyCopy(Array) newLazyCopy(array)}.{@link
     * MutableArray#asUnresizable() asUnresizable()}</tt>.
     * However, such call may require much more resources than this method.
     * See {@link #newUnresizableArray(Class, long)} method to learn more about this difference.
     *
     * <p>The described lazy copying is supported not by all memory models.
     * In this package, only {@link LargeMemoryModel} supports this.
     * Other memory models of this package inherit implementation
     * from {@link AbstractMemoryModel#newUnresizableLazyCopy(Array)},
     * where this method just performs the actual copying as described in the code example above.
     *
     * @param array the source array.
     * @return the lazy unresizable copy of the source array, if lazy copying is supported by the memory model,
     * or the usual identical unresizable copy of the source array in other case.
     * @throws NullPointerException            if the argument is <tt>null</tt>.
     * @throws UnsupportedElementTypeException if the element type of the passed array
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the length of the passed array is too large for this memory model.
     */
    UpdatableArray newUnresizableLazyCopy(Array array);

    /**
     * Allocates a zero-filled {@link Matrix matrix} with the specified element type and dimensions.
     *
     * <p>The AlgART array, that backs the new matrix (and is will be returned by {@link Matrix#array()}
     * method), will be unresizable. It is created by the following call:
     * <tt>{@link #newUnresizableArray(Class, long) newUnresizableArray}(elementType, len)</tt>,
     * where <tt>len</tt> is the product of all passed dimensions:
     * <tt>len = dim[0] * dim[1] * ...</tt>.
     * The {@link Matrix#dimCount()} method will return <tt>dim.length</tt>,
     * and {@link Matrix#dim(int) Matrix.dim(n)} method will return <tt>dim[n]</tt>.
     *
     * <p>The <tt>arraySupertype</tt> argument must be equal to or supertype of the class of
     * the underlying array, created by the call specified above.
     * In other case, <tt>ClassCastException</tt> is thrown.
     * (This check is performed at the very beginning,
     * before actual calling {@link #newUnresizableArray(Class, long) newUnresizableArray}
     * and allocating memory.)
     * Also <tt>arraySupertype</tt> argument must not be {@link MutableArray} or its subtype,
     * because the underlying array of any matrix is always unresizable.
     * This argument allows to use this method for safe creating a matrix
     * with the required generics type parameter.
     *
     * <p>The <tt>dim</tt> argument is cloned by this method: no references to it
     * are maintained by the created matrix.
     *
     * @param <T>            the generic type of AlgART array.
     * @param arraySupertype the desired type of the underlying array (the generic argument of the matrix type).
     * @param elementType    the type of matrix elements.
     * @param dim            the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>elementType</tt> or <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if <tt>elementType</tt> is <tt>void.class</tt>,
     *                                         or if <tt>arraySupertype</tt> is {@link MutableArray} or its subtype,
     *                                         or if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt; 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws ClassCastException              if <tt>arraySupertype</tt> and <tt>elementType</tt> do not match.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see Matrix
     * @see #isElementTypeSupported(Class)
     * @see #newArray(Class, long)
     * @see #newMatrix(Class, Matrix)
     * @see Matrices#matrix(Array, long...)
     * @see Matrices#checkNewMatrixType(Class, Class)
     */
    <T extends UpdatableArray> Matrix<T> newMatrix(Class<T> arraySupertype, Class<?> elementType, long... dim);

    /**
     * An analog of {@link #newMatrix(Class, Class, long...)}, which automatically uses {@link SignalMemoryModel}
     * instead of this memory model if the required matrix size is not greater than
     * <tt>maxSizeAllocatedInJavaMemory</tt> bytes.
     *
     * <p>This method is equivalent to the following:
     * <pre>
     *     ({@link Arrays#sizeOf(Class, long) Arrays.sizeOf}(elementType, {@link Arrays#longMul(long...)
     *     Arrays.longMul}(dim)) &lt;= maxSizeAllocatedInJavaMemory ?
     *         {@link Arrays#SMM} :
     *         <tt>thisInstance</tt>)
     *     .{@link #newMatrix(Class, Class, long...) newMatrix}(arraySupertype, elementType, dim);
     * </pre>
     *
     * <p>The typical value for <tt>maxSizeAllocatedInJavaMemory</tt> argument is the result of
     * {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()} method.</p>
     *
     * @param <T>                          the generic type of AlgART array.
     * @param maxSizeAllocatedInJavaMemory the maximal amount of required memory, for which this method
     *                                     just redirects to the same method of
     *                                     {@link SimpleMemoryModel#getInstance()}
     * @param arraySupertype               the desired type of the underlying array (the generic argument
     *                                     of the matrix type).
     * @param elementType                  the type of matrix elements.
     * @param dim                          the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>elementType</tt> or <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if <tt>elementType</tt> is <tt>void.class</tt>,
     *                                         or if <tt>arraySupertype</tt> is {@link MutableArray} or its subtype,
     *                                         or if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt; 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws ClassCastException              if <tt>arraySupertype</tt> and <tt>elementType</tt> do not match.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model
     *                                         or, for a matrix smaller than <tt>maxSizeAllocatedInJavaMemory</tt>,
     *                                         by {@link SimpleMemoryModel}.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model or,
     *                                         for a matrix smaller than <tt>maxSizeAllocatedInJavaMemory</tt>,
     *                                         by {@link SimpleMemoryModel}.
     */
    <T extends UpdatableArray> Matrix<T> newMatrix(
            long maxSizeAllocatedInJavaMemory,
            Class<T> arraySupertype,
            Class<?> elementType,
            long... dim);

    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(arraySupertype, matrix.{@link Matrix#elementType()
     * elementType()}, matrix.{@link Matrix#dimensions() dimensions()})</tt>.
     *
     * @param <T>            the generic type of AlgART array.
     * @param arraySupertype the desired type of the underlying array of the new matrix
     *                       (usually an updatable version of the built-in array of the source matrix).
     * @param matrix         the pattern matrix.
     * @return new matrix with the same dimensions and element type.
     * @throws NullPointerException            if <tt>matrix</tt> is <tt>null</tt>.
     * @throws ClassCastException              if <tt>arraySupertype</tt> and <tt>matrix.elementType()</tt>
     *                                         do not match.
     * @throws UnsupportedElementTypeException if <tt>matrix.elementType()</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the {@link Matrix#size() size} of passed matrix is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newStructuredMatrix(Class, Matrix)
     * @see Matrix
     */
    <T extends UpdatableArray> Matrix<T> newMatrix(Class<T> arraySupertype, Matrix<?> matrix);

    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Matrix)
     * newMatrix}(arraySupertype, matrix).{@link Matrix#structureLike(Matrix) structureLike}(matrix)</tt>.
     *
     * @param <T>            the generic type of AlgART array.
     * @param arraySupertype the desired type of the underlying array of the new matrix
     *                       (usually an updatable version of the built-in array of the source matrix).
     * @param matrix         the pattern matrix.
     * @return new matrix with the same dimensions, element type and similar ordering elements.
     * @throws NullPointerException            if <tt>matrix</tt> is <tt>null</tt>.
     * @throws ClassCastException              if <tt>arraySupertype</tt> and <tt>matrix.elementType()</tt>
     *                                         do not match.
     * @throws UnsupportedElementTypeException if <tt>matrix.elementType()</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the {@link Matrix#size() size} of passed matrix is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see Matrix
     */
    <T extends UpdatableArray> Matrix<T> newStructuredMatrix(Class<T> arraySupertype, Matrix<?> matrix);

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
               bit     ==> char,,byte,,short,,int,,long,,float,,double */

    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableBitArray.class, boolean.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableBitArray&gt; m = memoryModel.newBitMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>boolean</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newBitArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableBitArray> newBitMatrix(long... dim);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableCharArray.class, char.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableCharArray&gt; m = memoryModel.newCharMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>char</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newCharArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableCharArray> newCharMatrix(long... dim);


    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableByteArray.class, byte.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableByteArray&gt; m = memoryModel.newByteMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>byte</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newByteArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableByteArray> newByteMatrix(long... dim);


    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableShortArray.class, short.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableShortArray&gt; m = memoryModel.newShortMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>short</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newShortArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableShortArray> newShortMatrix(long... dim);


    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableIntArray.class, int.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableIntArray&gt; m = memoryModel.newIntMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>int</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newIntArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableIntArray> newIntMatrix(long... dim);


    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableLongArray.class, long.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableLongArray&gt; m = memoryModel.newLongMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>long</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newLongArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableLongArray> newLongMatrix(long... dim);


    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableFloatArray.class, float.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableFloatArray&gt; m = memoryModel.newFloatMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>float</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newFloatArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableFloatArray> newFloatMatrix(long... dim);


    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableDoubleArray.class, double.class, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableDoubleArray&gt; m = memoryModel.newDoubleMatrix(100, 100);
     * </pre>
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>double</tt> element type is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newDoubleArray(long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    Matrix<UpdatableDoubleArray> newDoubleMatrix(long... dim);
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <tt>{@link #newMatrix(Class, Class, long...)
     * newMatrix}(UpdatableObjectArray.class, elementType, dim)</tt>.
     *
     * <p>Example of usage:<pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;Matrix&lt;UpdatableObjectArray&lt;String&gt;&gt; m =
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;memoryModel.newObjectMatrix(String.class, 100, 100);
     * </pre>
     *
     * @param elementType the type of matrix elements.
     * @param dim         the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <tt>dim</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <tt>dim.length == 0</tt>,
     *                                         <tt>dim[n] &lt;= 0</tt> for some <tt>n</tt>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws UnsupportedElementTypeException if <tt>elementType</tt> is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     * @see #isElementTypeSupported(Class)
     * @see #newObjectArray(Class, long)
     * @see Matrices#matrix(Array, long...)
     * @see Matrix
     */
    <E> Matrix<UpdatableObjectArray<E>> newObjectMatrix(Class<E> elementType, long... dim);

    /**
     * Equivalent to <tt>matrix.{@link Matrix#matrix(Array) matrix}({@link #newUnresizableLazyCopy(Array)
     * newUnresizableLazyCopy}(matrix.{@link Matrix#array() array()})).{@link Matrix#cast(Class)
     * cast}(arraySupertype)</tt>.
     *
     * @param arraySupertype the desired type of the underlying array of the new lazy copy
     *                       (usually an updatable version of the built-in array of the source matrix).
     * @param matrix         the source matrix.
     * @return the lazy copy of the source matrix, if lazy copying is supported by the memory model,
     * or the usual identical copy of the source matrix in other case.
     * @throws NullPointerException            if onn of the arguments is <tt>null</tt>.
     * @throws ClassCastException              if <tt>arraySupertype</tt> does not match
     *                                         to the element type of the passed matrix.
     * @throws UnsupportedElementTypeException if the element type of the passed matrix
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the total size of the passed matrix
     *                                         is too large for this memory model.
     */
    <T extends UpdatableArray> Matrix<T> newLazyCopy(Class<T> arraySupertype, Matrix<?> matrix);


    /**
     * Allocates an unresizable AlgART array containing <tt>count</tt> elements of
     * the specified Java array:
     * <tt>array[offset], array[offset + 1], ..., array[offset + count - 1]</tt>.
     *
     * <p>The returned AlgART array will be "safe" in the sense that no references to the passed Java array
     * are maintained by it.
     * In other words, this method must always allocate a new AlgART array.
     *
     * <p>This method is equivalent to the following expression:
     * <tt>{@link #newUnresizableArray(Class, long) newUnresizableArray}(elementType, count).{@link
     * UpdatableArray#setData(long, Object, int, int) setData}(0, array, offset, count)</tt>,
     * where <tt>elementType</tt> is the type of <tt>array</tt> elements
     * (<tt>array.getClass().getComponentType()</tt>).
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>array</tt> argument is not a Java array.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableArray valueOf(Object array, int offset, int count);

    /**
     * Allocates an unresizable AlgART array containing all elements of the specified Java array:
     * <tt>array[0], array[1], ..., array[array.length - 1]</tt>.
     *
     * <p>The returned AlgART array will be "safe" in the sense that no references to the passed Java array
     * are maintained by it.
     * In other words, this method must always allocate a new array.
     *
     * <p>This method is equivalent to the following expression:
     * <tt>{@link #newUnresizableArray(Class, long) newUnresizableArray}(elementType, len).{@link
     * UpdatableArray#setData(long, Object) setData}(0, array)</tt>,
     * where <tt>elementType</tt> is the type of <tt>array</tt> elements
     * (<tt>array.getClass().getComponentType()</tt>) and
     * <tt>len</tt> is the length of the passed Java array.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>array</tt> argument is not a Java array.
     */
    UpdatableArray valueOf(Object array);

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double */

    /**
     * Equivalent to <tt>(UpdatableBitArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableBitArray valueOf(boolean[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableBitArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableBitArray valueOf(boolean[] array);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <tt>(UpdatableCharArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableCharArray valueOf(char[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableCharArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableCharArray valueOf(char[] array);


    /**
     * Equivalent to <tt>(UpdatableByteArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableByteArray valueOf(byte[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableByteArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableByteArray valueOf(byte[] array);


    /**
     * Equivalent to <tt>(UpdatableShortArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableShortArray valueOf(short[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableShortArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableShortArray valueOf(short[] array);


    /**
     * Equivalent to <tt>(UpdatableIntArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableIntArray valueOf(int[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableIntArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableIntArray valueOf(int[] array);


    /**
     * Equivalent to <tt>(UpdatableLongArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableLongArray valueOf(long[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableLongArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableLongArray valueOf(long[] array);


    /**
     * Equivalent to <tt>(UpdatableFloatArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableFloatArray valueOf(float[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableFloatArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableFloatArray valueOf(float[] array);


    /**
     * Equivalent to <tt>(UpdatableDoubleArray){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    UpdatableDoubleArray valueOf(double[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableDoubleArray){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    UpdatableDoubleArray valueOf(double[] array);
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <tt>(UpdatableObjectArray&lt;E&gt;){@link #valueOf(Object, int, int)
     * valueOf}((Object)array, offset, count)</tt>.
     *
     * @param array  the source Java array with elements of constructed AlgART array.
     * @param offset starting position in the source Java array.
     * @param count  the length of returned AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException      if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside the passed Java array.
     */
    <E> UpdatableObjectArray<E> valueOf(E[] array, int offset, int count);

    /**
     * Equivalent to <tt>(UpdatableObjectArray&lt;E&gt;){@link #valueOf(Object) valueOf}((Object)array)</tt>.
     *
     * @param array the source Java array with elements of constructed AlgART array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    <E> UpdatableObjectArray<E> valueOf(E[] array);


    /**
     * Returns <tt>true</tt> if this memory model can create arrays with this element type.
     * If it does not support it, creation methods of this memory model
     * will throw {@link UnsupportedElementTypeException}.
     * The result is not defined for <tt>void.class</tt>.
     *
     * @param elementType the type of array elements.
     * @return <tt>true</tt> if this memory model supports this element type.
     * @throws NullPointerException if <tt>elementType</tt> is <tt>null</tt>.
     */
    boolean isElementTypeSupported(Class<?> elementType);

    /**
     * Returns <tt>true</tt> if this memory model can create arrays with all primitive element types:
     * <tt>boolean</tt>, <tt>char</tt>, <tt>byte</tt>, <tt>short</tt>,
     * <tt>int</tt>, <tt>long</tt>, <tt>float</tt>, <tt>double</tt>.
     *
     * @return <tt>true</tt> if this memory model supports all primitive element types.
     * @see #isElementTypeSupported(Class)
     */
    boolean areAllPrimitiveElementTypesSupported();

    /**
     * Returns <tt>true</tt> if this memory model can create arrays with <i>all</i> element types.
     * This package offers only one such memory model: {@link SimpleMemoryModel}.
     *
     * @return <tt>true</tt> if this memory model supports element types.
     * @see #isElementTypeSupported(Class)
     */
    boolean areAllElementTypesSupported();

    /**
     * Returnes maximal possible length of arrays with the specified element type supported by this memory model.
     * If the capacity / length passed to {@link #newEmptyArray(Class, long)} / {@link #newArray(Class, long)}
     * methods is greater than the result of this method, they will throw
     * {@link TooLargeArrayException}.
     * The result is not defined it the passed element type is not supported by this memory model
     * (for example, it may be -1).
     *
     * <p>For some memory model, the method may be not enough informative: creation methods
     * may throw {@link TooLargeArrayException} even if the passed capacity / length
     * is less than the result of this method. Please refer to the documentation
     * of the corresponding memory model to know the precise behavior of this method.
     * In any case, maximal possible array length is restricted by the amount of Java memory.
     *
     * @param elementType the type of array elements.
     * @return maximal possible length of arrays supported by this memory model.
     * @throws NullPointerException if <tt>elementType</tt> is <tt>null</tt>.
     */
    long maxSupportedLength(Class<?> elementType);

    /**
     * Returns <tt>true</tt> if the passed <tt>array</tt> was created by this
     * (or identical) memory model.
     *
     * <p>For {@link SimpleMemoryModel} and {@link BufferMemoryModel},
     * "identical" means the same memory model.
     *
     * <p>For {@link LargeMemoryModel}, "identical" means that
     * the memory model is also large and was created with
     * the same <i>instance</i> of the {@link DataFileModel data file factory}.
     *
     * <p>For {@link CombinedMemoryModel}, "identical" means that
     * the memory model is also combined and was created with
     * the same <i>instance</i> of the {@link CombinedMemoryModel.Combiner combiner}.
     *
     * <p>Returns <tt>false</tt> if the passed argument is <tt>null</tt>.
     *
     * @param array the AlgART array (can be <tt>null</tt>, than the method returns <tt>false</tt>).
     * @return <tt>true</tt> if the passed <tt>array</tt> was created by this memory model.
     */
    boolean isCreatedBy(Array array);
}
