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

import net.algart.arrays.SimpleArraysImpl.*;

/**
 * <p>The simplest memory model, based on usual Java arrays.
 * It means that AlgART array of elements of some <tt><i>type</i></tt>
 * contains an underlying Java array <tt><i>type</i>[]</tt>,
 * and any access to AlgART array elements is translated
 * to corresponding access to the underlying Java array.
 * The only exception is {@link BitArray bit arrays},
 * where the bits are packed into <tt>long[]</tt> Java array,
 * as specified in {@link PackedBitArrays} class.</p>
 *
 * <p>This memory model supports all possible element types.
 * The maximal theoretical limit for length and capacity of AlgART arrays,
 * supported by this memory model, is <tt>2<sup>37</sup>-64</tt> for bit arrays and
 * <tt>2<sup>31</sup>-1</tt> for all other element types.
 * The real limit is essentially less, usually 2 GB in 64-bit JVM or ~1.0-1.5 GB in 32-bit JVM.</p>
 *
 * <p>This memory model may be not effective enough for very large arrays of
 * non-primitive elements (inheritors of <tt>Object</tt> class):
 * it stores them as array of pointers, alike standard <tt>ArrayList</tt>.
 * For effective storing arrays of objects, we recommend using
 * the {@link CombinedMemoryModel combined memory model}.</p>
 *
 * <p>All arrays created by this memory model, besides bit arrays and {@link Array#asImmutable()
 * immutable views} of the arrays, implement {@link DirectAccessible} interface,
 * and {@link DirectAccessible#hasJavaArray()} returns <tt>true</tt> for such arrays.</p>
 *
 * <p>All arrays, created by this memory model, have empty implementation of
 * {@link Array#loadResources(ArrayContext)},
 * {@link Array#flushResources(ArrayContext)}, {@link Array#flushResources(ArrayContext, boolean)} and
 * {@link Array#freeResources(ArrayContext)} methods:
 * these methods do nothing.</p>
 *
 * <p>In the arrays of objects (non-primitive) created by this memory model:</p>
 * <ul>
 * <li>{@link ObjectInPlaceArray}, {@link UpdatableObjectInPlaceArray},
 * {@link MutableObjectInPlaceArray} interfaces are never implemented;</li>
 * <li>{@link UpdatableArray#copy(Array)},
 * {@link UpdatableArray#copy(long, long)},
 * {@link UpdatableArray#copy(long, long, long)},
 * {@link UpdatableArray#swap(UpdatableArray)},
 * {@link UpdatableArray#swap(long, long)},
 * {@link UpdatableArray#swap(long, long, long)} methods
 * copies only references to objects, but do not perform deep cloning;</li>
 * <li>{@link Array#hashCode()}, {@link Array#equals(Object)} methods
 * are based on implementation of <tt>hashCode</tt> and <tt>equals</tt> method
 * in the class of elements ({@link Array#elementType()}).</li>
 * </ul>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of its instance returned by {@link #getInstance()} method.
 * Moreover, it is a <b>singleton</b>: {@link #getInstance()} always returns the same object.</p>
 *
 * @author Daniel Alievsky
 */
public final class SimpleMemoryModel extends AbstractMemoryModel {

    static final SimpleMemoryModel INSTANCE = new SimpleMemoryModel();

    private SimpleMemoryModel() {
    }

    /**
     * Returns an instance of this memory model. This method always returns the same object.
     *
     * @return an instance of this memory model.
     * @see Arrays#SMM
     */
    public static SimpleMemoryModel getInstance() {
        return INSTANCE;
    }

    public MutableArray newEmptyArray(Class<?> elementType) {
        return newEmptyArray(elementType, 10);
    }

    public MutableArray newEmptyArray(Class<?> elementType, long initialCapacity) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Negative initial capacity");
        //[[Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (elementType == boolean.class)
            return new MutableJABitArray(initialCapacity, 0);
        else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == char.class)
            return new MutableJACharArray(initialCapacity, 0);
        else
        if (elementType == byte.class)
            return new MutableJAByteArray(initialCapacity, 0);
        else
        if (elementType == short.class)
            return new MutableJAShortArray(initialCapacity, 0);
        else
        if (elementType == int.class)
            return new MutableJAIntArray(initialCapacity, 0);
        else
        if (elementType == long.class)
            return new MutableJALongArray(initialCapacity, 0);
        else
        if (elementType == float.class)
            return new MutableJAFloatArray(initialCapacity, 0);
        else
        if (elementType == double.class)
            return new MutableJADoubleArray(initialCapacity, 0);
        else //[[Repeat.AutoGeneratedEnd]]
            return new MutableJAObjectArray(elementType, initialCapacity, 0);
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialLength < 0)
            throw new IllegalArgumentException("Negative initial length");
        //[[Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (elementType == boolean.class)
            return new MutableJABitArray(initialLength, initialLength);
        else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == char.class)
            return new MutableJACharArray(initialLength, initialLength);
        else
        if (elementType == byte.class)
            return new MutableJAByteArray(initialLength, initialLength);
        else
        if (elementType == short.class)
            return new MutableJAShortArray(initialLength, initialLength);
        else
        if (elementType == int.class)
            return new MutableJAIntArray(initialLength, initialLength);
        else
        if (elementType == long.class)
            return new MutableJALongArray(initialLength, initialLength);
        else
        if (elementType == float.class)
            return new MutableJAFloatArray(initialLength, initialLength);
        else
        if (elementType == double.class)
            return new MutableJADoubleArray(initialLength, initialLength);
        else //[[Repeat.AutoGeneratedEnd]]
            return new MutableJAObjectArray(elementType, initialLength, initialLength);
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0)
            throw new IllegalArgumentException("Negative array length");
        //[[Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (elementType == boolean.class)
            return new UpdatableJABitArray(length, length);
        else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == char.class)
            return new UpdatableJACharArray(length, length);
        else
        if (elementType == byte.class)
            return new UpdatableJAByteArray(length, length);
        else
        if (elementType == short.class)
            return new UpdatableJAShortArray(length, length);
        else
        if (elementType == int.class)
            return new UpdatableJAIntArray(length, length);
        else
        if (elementType == long.class)
            return new UpdatableJALongArray(length, length);
        else
        if (elementType == float.class)
            return new UpdatableJAFloatArray(length, length);
        else
        if (elementType == double.class)
            return new UpdatableJADoubleArray(length, length);
        else //[[Repeat.AutoGeneratedEnd]]
            return new UpdatableJAObjectArray(elementType, length, length);
    }

    public UpdatableArray valueOf(Object array, int offset, int count) {
        if (array == null)
            throw new NullPointerException("Null array argument");
        UpdatableArray result;
        if (array instanceof boolean[]) {
            result = new UpdatableJABitArray(count, count);
            PackedBitArrays.packBits(((JABitArray)result).bitArray, 0, (boolean[])array, offset, count);
        }//[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
         //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double]]
        else if (array instanceof char[]) {
            result = new UpdatableJACharArray(count, count);
            System.arraycopy(array, offset, ((JACharArray)result).charArray, 0, count);
        } //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        else if (array instanceof byte[]) {
            result = new UpdatableJAByteArray(count, count);
            System.arraycopy(array, offset, ((JAByteArray)result).byteArray, 0, count);
        }
        else if (array instanceof short[]) {
            result = new UpdatableJAShortArray(count, count);
            System.arraycopy(array, offset, ((JAShortArray)result).shortArray, 0, count);
        }
        else if (array instanceof int[]) {
            result = new UpdatableJAIntArray(count, count);
            System.arraycopy(array, offset, ((JAIntArray)result).intArray, 0, count);
        }
        else if (array instanceof long[]) {
            result = new UpdatableJALongArray(count, count);
            System.arraycopy(array, offset, ((JALongArray)result).longArray, 0, count);
        }
        else if (array instanceof float[]) {
            result = new UpdatableJAFloatArray(count, count);
            System.arraycopy(array, offset, ((JAFloatArray)result).floatArray, 0, count);
        }
        else if (array instanceof double[]) {
            result = new UpdatableJADoubleArray(count, count);
            System.arraycopy(array, offset, ((JADoubleArray)result).doubleArray, 0, count);
        } //[[Repeat.AutoGeneratedEnd]]
        else if (array instanceof Object[]) {
            result = new UpdatableJAObjectArray(array.getClass().getComponentType(), count, count);
            System.arraycopy(array, offset, ((JAObjectArray)result).objectArray, 0, count);
        } else {
            throw new IllegalArgumentException("The passed argument is not a Java array");
        }
        return result;
    }

    public UpdatableArray valueOf(Object array) {
        if (array == null)
            throw new NullPointerException("Null array argument");
        return valueOf(array, 0, java.lang.reflect.Array.getLength(array));
    }

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double
     */
    public UpdatableBitArray valueOf(boolean[] array, int offset, int count) {
        return (UpdatableBitArray)valueOf((Object)array, offset, count);
    }

    public UpdatableBitArray valueOf(boolean[] array) {
        return (UpdatableBitArray)valueOf((Object)array);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public UpdatableCharArray valueOf(char[] array, int offset, int count) {
        return (UpdatableCharArray)valueOf((Object)array, offset, count);
    }

    public UpdatableCharArray valueOf(char[] array) {
        return (UpdatableCharArray)valueOf((Object)array);
    }

    public UpdatableByteArray valueOf(byte[] array, int offset, int count) {
        return (UpdatableByteArray)valueOf((Object)array, offset, count);
    }

    public UpdatableByteArray valueOf(byte[] array) {
        return (UpdatableByteArray)valueOf((Object)array);
    }

    public UpdatableShortArray valueOf(short[] array, int offset, int count) {
        return (UpdatableShortArray)valueOf((Object)array, offset, count);
    }

    public UpdatableShortArray valueOf(short[] array) {
        return (UpdatableShortArray)valueOf((Object)array);
    }

    public UpdatableIntArray valueOf(int[] array, int offset, int count) {
        return (UpdatableIntArray)valueOf((Object)array, offset, count);
    }

    public UpdatableIntArray valueOf(int[] array) {
        return (UpdatableIntArray)valueOf((Object)array);
    }

    public UpdatableLongArray valueOf(long[] array, int offset, int count) {
        return (UpdatableLongArray)valueOf((Object)array, offset, count);
    }

    public UpdatableLongArray valueOf(long[] array) {
        return (UpdatableLongArray)valueOf((Object)array);
    }

    public UpdatableFloatArray valueOf(float[] array, int offset, int count) {
        return (UpdatableFloatArray)valueOf((Object)array, offset, count);
    }

    public UpdatableFloatArray valueOf(float[] array) {
        return (UpdatableFloatArray)valueOf((Object)array);
    }

    public UpdatableDoubleArray valueOf(double[] array, int offset, int count) {
        return (UpdatableDoubleArray)valueOf((Object)array, offset, count);
    }

    public UpdatableDoubleArray valueOf(double[] array) {
        return (UpdatableDoubleArray)valueOf((Object)array);
    }
    /*Repeat.AutoGeneratedEnd*/

    public boolean isElementTypeSupported(Class<?> elementType) {
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        return true;
    }

    public boolean areAllPrimitiveElementTypesSupported() {
        return true;
    }

    public boolean areAllElementTypesSupported() {
        return true;
    }

    /**
     * This implementation returns <tt>Integer.MAX_VALUE == 2<sup>31</sup>-1</tt>
     * for all element types besides <tt>boolean.class</tt>,
     * or some large value (depending on implementation) for <tt>boolean.class</tt>.
     *
     * <p>In current implementation, returns <tt>2<sup>37</sup>-64</tt>
     * for <tt>boolean.class</tt>, because bits are stored in <tt>long</tt> values.
     *
     * @param elementType the type of array elements.
     * @return            maximal possible length of arrays supported by this memory model.
     * @throws NullPointerException if <tt>elementType</tt> is <tt>null</tt>.
     */
    public long maxSupportedLength(Class<?> elementType) {
        return maxSupportedLengthImpl(elementType);
    }

    public boolean isCreatedBy(Array array) {
        return isSimpleArray(array);
    }

    /**
     * Returns <tt>true</tt> if the passed instance is an array created by this memory model.
     * Returns <tt>false</tt> if the passed array is <tt>null</tt> or an AlgART array created
     * by another memory model.
     *
     * <p>As this memory model is a singleton, this method is equivalent to {@link #isCreatedBy(Array)}.
     *
     * @param array the checked array.
     * @return      <tt>true</tt> if this array is created by the simple memory model.

     */
    public static boolean isSimpleArray(Array array) {
        return array instanceof AbstractJAArray;
    }

    /**
     * Returns a brief string description of this memory model.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "Simple memory model";
    }

    /**
     * Returns an unresizable AlgART array backed by the specified Java array,
     * excluding a case of <tt>boolean[]</tt> array.
     * The result AlgART array contains all elements the passed Java array:
     * <tt>array[0], array[1], ..., array[array.length - 1]</tt>,
     * and changes in the returned array "write through" to <tt>array</tt> argument.
     * The length and capacity of the returned array are equal to <tt>array.length</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>array</tt> argument is not an array
     *                                  or <tt>boolean[]</tt> array.
     */
    public static UpdatableArray asUpdatableArray(Object array) {
        if (array == null)
            throw new NullPointerException("Null array argument");
        if (array instanceof boolean[]) {
            throw new IllegalArgumentException("asUpdatableArray cannot be called for boolean[] array");
        } else
        //[[Repeat() byte ==> char,,short,,int,,long,,float,,double,,Object;;
        //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double,,Object]]
        if (array instanceof byte[]) {
            return new UpdatableJAByteArray((byte[])array, ((byte[])array).length, ((byte[])array).length);
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (array instanceof char[]) {
            return new UpdatableJACharArray((char[])array, ((char[])array).length, ((char[])array).length);
        } else
        if (array instanceof short[]) {
            return new UpdatableJAShortArray((short[])array, ((short[])array).length, ((short[])array).length);
        } else
        if (array instanceof int[]) {
            return new UpdatableJAIntArray((int[])array, ((int[])array).length, ((int[])array).length);
        } else
        if (array instanceof long[]) {
            return new UpdatableJALongArray((long[])array, ((long[])array).length, ((long[])array).length);
        } else
        if (array instanceof float[]) {
            return new UpdatableJAFloatArray((float[])array, ((float[])array).length, ((float[])array).length);
        } else
        if (array instanceof double[]) {
            return new UpdatableJADoubleArray((double[])array, ((double[])array).length, ((double[])array).length);
        } else
        if (array instanceof Object[]) {
            return new UpdatableJAObjectArray((Object[])array, ((Object[])array).length, ((Object[])array).length);
        } else //[[Repeat.AutoGeneratedEnd]]
        {
            throw new IllegalArgumentException("The passed argument is not a Java array");
        }
    }

    /*Repeat() char ==> byte,,short,,int,,long,,float,,double;;
               Char ==> Byte,,Short,,Int,,Long,,Float,,Double
     */
    /**
     * Equivalent to <tt>(UpdatableCharArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableCharArray asUpdatableCharArray(char[] array) {
        return (UpdatableCharArray)asUpdatableArray((Object)array);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Equivalent to <tt>(UpdatableByteArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableByteArray asUpdatableByteArray(byte[] array) {
        return (UpdatableByteArray)asUpdatableArray((Object)array);
    }

    /**
     * Equivalent to <tt>(UpdatableShortArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableShortArray asUpdatableShortArray(short[] array) {
        return (UpdatableShortArray)asUpdatableArray((Object)array);
    }

    /**
     * Equivalent to <tt>(UpdatableIntArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableIntArray asUpdatableIntArray(int[] array) {
        return (UpdatableIntArray)asUpdatableArray((Object)array);
    }

    /**
     * Equivalent to <tt>(UpdatableLongArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableLongArray asUpdatableLongArray(long[] array) {
        return (UpdatableLongArray)asUpdatableArray((Object)array);
    }

    /**
     * Equivalent to <tt>(UpdatableFloatArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableFloatArray asUpdatableFloatArray(float[] array) {
        return (UpdatableFloatArray)asUpdatableArray((Object)array);
    }

    /**
     * Equivalent to <tt>(UpdatableDoubleArray){@link #asUpdatableArray(Object) asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static UpdatableDoubleArray asUpdatableDoubleArray(double[] array) {
        return (UpdatableDoubleArray)asUpdatableArray((Object)array);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <tt>(UpdatableObjectArray<E>){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</tt>.
     *
     * @param array the source Java array.
     * @return      an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <tt>array</tt> argument is <tt>null</tt>.
     */
    public static <E> UpdatableObjectArray<E> asUpdatableObjectArray(E[] array) {
        return InternalUtils.cast(asUpdatableArray((Object)array));
    }

    static long maxSupportedLengthImpl(Class<?> elementType) {
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        if (elementType == boolean.class)
            return ((long)Integer.MAX_VALUE) << 6;
        else
            return Integer.MAX_VALUE;
    }
}
