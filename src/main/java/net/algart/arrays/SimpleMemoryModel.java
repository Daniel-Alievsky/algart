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

import net.algart.arrays.SimpleArraysImpl.*;

import java.util.Objects;

/**
 * <p>The simplest memory model, based on usual Java arrays.
 * It means that AlgART array of elements of some <code><i>type</i></code>
 * contains an underlying Java array <code><i>type</i>[]</code>,
 * and any access to AlgART array elements is translated
 * to corresponding access to the underlying Java array.
 * The only exception is {@link BitArray bit arrays},
 * where the bits are packed into <code>long[]</code> Java array,
 * as specified in {@link PackedBitArrays} class.</p>
 *
 * <p>This memory model supports all possible element types.
 * The maximal theoretical limit for length and capacity of AlgART arrays,
 * supported by this memory model, is <code>2<sup>37</sup>-64</code> for bit arrays and
 * <code>2<sup>31</sup>-1</code> for all other element types.
 * The real limit is essentially less, usually 2 GB in 64-bit JVM or ~1.0-1.5 GB in 32-bit JVM.</p>
 *
 * <p>This memory model may be not effective enough for very large arrays of
 * non-primitive elements (inheritors of <code>Object</code> class):
 * it stores them as array of pointers, alike standard <code>ArrayList</code>.
 * For effective storing arrays of objects, we recommend using
 * the {@link CombinedMemoryModel combined memory model}.</p>
 *
 * <p>All arrays created by this memory model, besides bit arrays and {@link Array#asImmutable()
 * immutable views} of the arrays, implement {@link DirectAccessible} interface,
 * {@link DirectAccessible#hasJavaArray()} returns <code>true</code> for such arrays,
 * and {@link DirectAccessible#javaArrayOffset()} returns 0 for them.
 * Moreover, if this array is created as unresizable using
 * {@link #newUnresizableArray(Class, long)} method or some equivalent way,
 * including <code>newUnresizableXxxArray</code> or <code>newMatrix</code> methods,
 * then you can be sure that its length (returned by {@link Array#length()} and
 * {@link DirectAccessible#javaArrayLength()} methods) is equal to the actual length
 * of the Java array, returned by {@link DirectAccessible#javaArray()} method;
 * {@link Array#isJavaArrayWrapper()} returns <code>true</code> for such arrays.</p>
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
 * are based on implementation of <code>hashCode</code> and <code>equals</code> method
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
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Negative initial capacity");
        }
        if (elementType == boolean.class) {
            return new MutableJABitArray(initialCapacity, 0);
        } else if (elementType == char.class) {
            return new MutableJACharArray(initialCapacity, 0);
        } else if (elementType == byte.class) {
            return new MutableJAByteArray(initialCapacity, 0);
        } else if (elementType == short.class) {
            return new MutableJAShortArray(initialCapacity, 0);
        } else if (elementType == int.class) {
            return new MutableJAIntArray(initialCapacity, 0);
        } else if (elementType == long.class) {
            return new MutableJALongArray(initialCapacity, 0);
        } else if (elementType == float.class) {
            return new MutableJAFloatArray(initialCapacity, 0);
        } else if (elementType == double.class) {
            return new MutableJADoubleArray(initialCapacity, 0);
        } else {
            return new MutableJAObjectArray(elementType, initialCapacity, 0);
        }
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialLength < 0) {
            throw new IllegalArgumentException("Negative initial length");
        }
        if (elementType == boolean.class) {
            return new MutableJABitArray(initialLength, initialLength);
        } else if (elementType == char.class) {
            return new MutableJACharArray(initialLength, initialLength);
        } else if (elementType == byte.class) {
            return new MutableJAByteArray(initialLength, initialLength);
        } else if (elementType == short.class) {
            return new MutableJAShortArray(initialLength, initialLength);
        } else if (elementType == int.class) {
            return new MutableJAIntArray(initialLength, initialLength);
        } else if (elementType == long.class) {
            return new MutableJALongArray(initialLength, initialLength);
        } else if (elementType == float.class) {
            return new MutableJAFloatArray(initialLength, initialLength);
        } else if (elementType == double.class) {
            return new MutableJADoubleArray(initialLength, initialLength);
        } else {
            return new MutableJAObjectArray(elementType, initialLength, initialLength);
        }
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0) {
            throw new IllegalArgumentException("Negative array length");
        }
        if (elementType == boolean.class) {
            return new UpdatableJABitArray(length, length);
        } else if (elementType == char.class) {
            return new UpdatableJACharArray(length, length);
        } else if (elementType == byte.class) {
            return new UpdatableJAByteArray(length, length);
        } else if (elementType == short.class) {
            return new UpdatableJAShortArray(length, length);
        } else if (elementType == int.class) {
            return new UpdatableJAIntArray(length, length);
        } else if (elementType == long.class) {
            return new UpdatableJALongArray(length, length);
        } else if (elementType == float.class) {
            return new UpdatableJAFloatArray(length, length);
        } else if (elementType == double.class) {
            return new UpdatableJADoubleArray(length, length);
        } else {
            return new UpdatableJAObjectArray(elementType, length, length);
        }
    }

    public UpdatableArray valueOf(Object array, int offset, int count) {
        Objects.requireNonNull(array, "Null array argument");
        UpdatableArray result;
        if (array instanceof boolean[]) {
            result = new UpdatableJABitArray(count, count);
            PackedBitArrays.packBits(((JABitArray) result).bitArray, 0, (boolean[]) array, offset, count);
        } else if (array instanceof char[]) {
            result = new UpdatableJACharArray(count, count);
            System.arraycopy(array, offset, ((JACharArray) result).charArray, 0, count);
        } else if (array instanceof byte[]) {
            result = new UpdatableJAByteArray(count, count);
            System.arraycopy(array, offset, ((JAByteArray) result).byteArray, 0, count);
        } else if (array instanceof short[]) {
            result = new UpdatableJAShortArray(count, count);
            System.arraycopy(array, offset, ((JAShortArray) result).shortArray, 0, count);
        } else if (array instanceof int[]) {
            result = new UpdatableJAIntArray(count, count);
            System.arraycopy(array, offset, ((JAIntArray) result).intArray, 0, count);
        } else if (array instanceof long[]) {
            result = new UpdatableJALongArray(count, count);
            System.arraycopy(array, offset, ((JALongArray) result).longArray, 0, count);
        } else if (array instanceof float[]) {
            result = new UpdatableJAFloatArray(count, count);
            System.arraycopy(array, offset, ((JAFloatArray) result).floatArray, 0, count);
        } else if (array instanceof double[]) {
            result = new UpdatableJADoubleArray(count, count);
            System.arraycopy(array, offset, ((JADoubleArray) result).doubleArray, 0, count);
        } else if (array instanceof Object[]) {
            result = new UpdatableJAObjectArray(array.getClass().getComponentType(), count, count);
            System.arraycopy(array, offset, ((JAObjectArray) result).objectArray, 0, count);
        } else {
            throw new IllegalArgumentException("The argument is not a Java array: " + array.getClass());
        }
        return result;
    }

    public UpdatableArray valueOf(Object array) {
        Objects.requireNonNull(array, "Null array argument");
        return valueOf(array, 0, java.lang.reflect.Array.getLength(array));
    }

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double */

    public UpdatableBitArray valueOf(boolean[] array, int offset, int count) {
        return (UpdatableBitArray) valueOf((Object) array, offset, count);
    }

    public UpdatableBitArray valueOf(boolean[] array) {
        return (UpdatableBitArray) valueOf((Object) array);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public UpdatableCharArray valueOf(char[] array, int offset, int count) {
        return (UpdatableCharArray) valueOf((Object) array, offset, count);
    }

    public UpdatableCharArray valueOf(char[] array) {
        return (UpdatableCharArray) valueOf((Object) array);
    }

    public UpdatableByteArray valueOf(byte[] array, int offset, int count) {
        return (UpdatableByteArray) valueOf((Object) array, offset, count);
    }

    public UpdatableByteArray valueOf(byte[] array) {
        return (UpdatableByteArray) valueOf((Object) array);
    }

    public UpdatableShortArray valueOf(short[] array, int offset, int count) {
        return (UpdatableShortArray) valueOf((Object) array, offset, count);
    }

    public UpdatableShortArray valueOf(short[] array) {
        return (UpdatableShortArray) valueOf((Object) array);
    }

    public UpdatableIntArray valueOf(int[] array, int offset, int count) {
        return (UpdatableIntArray) valueOf((Object) array, offset, count);
    }

    public UpdatableIntArray valueOf(int[] array) {
        return (UpdatableIntArray) valueOf((Object) array);
    }

    public UpdatableLongArray valueOf(long[] array, int offset, int count) {
        return (UpdatableLongArray) valueOf((Object) array, offset, count);
    }

    public UpdatableLongArray valueOf(long[] array) {
        return (UpdatableLongArray) valueOf((Object) array);
    }

    public UpdatableFloatArray valueOf(float[] array, int offset, int count) {
        return (UpdatableFloatArray) valueOf((Object) array, offset, count);
    }

    public UpdatableFloatArray valueOf(float[] array) {
        return (UpdatableFloatArray) valueOf((Object) array);
    }

    public UpdatableDoubleArray valueOf(double[] array, int offset, int count) {
        return (UpdatableDoubleArray) valueOf((Object) array, offset, count);
    }

    public UpdatableDoubleArray valueOf(double[] array) {
        return (UpdatableDoubleArray) valueOf((Object) array);
    }

    /*Repeat.AutoGeneratedEnd*/

    public boolean isElementTypeSupported(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        return true;
    }

    public boolean areAllPrimitiveElementTypesSupported() {
        return true;
    }

    public boolean areAllElementTypesSupported() {
        return true;
    }

    /**
     * This implementation returns <code>Integer.MAX_VALUE == 2<sup>31</sup>-1</code>
     * for all element types besides <code>boolean.class</code>,
     * or some large value (depending on implementation) for <code>boolean.class</code>.
     *
     * <p>In current implementation, returns <code>2<sup>37</sup>-64</code>
     * for <code>boolean.class</code>, because bits are stored in <code>long</code> values.
     *
     * @param elementType the type of array elements.
     * @return maximal possible length of arrays supported by this memory model.
     * @throws NullPointerException if <code>elementType</code> is {@code null}.
     */
    public long maxSupportedLength(Class<?> elementType) {
        return maxSupportedLengthImpl(elementType);
    }

    public boolean isCreatedBy(Array array) {
        return isSimpleArray(array);
    }

    /**
     * Returns <code>true</code> if the passed instance is an array created by this memory model.
     * Returns <code>false</code> if the passed array is {@code null} or an AlgART array created
     * by another memory model.
     *
     * <p>As this memory model is a singleton, this method is equivalent to {@link #isCreatedBy(Array)}.
     *
     * @param array the checked array.
     * @return <code>true</code> if this array is created by the simple memory model.
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
     * Returns a <i>wrapper</i>: an unresizable AlgART array backed by the specified Java array,
     * excluding a case of <code>boolean[]</code> array.
     * For <code>boolean</code> elements ({@link BitArray}) please use the method
     * {@link #asUpdatableBitArray(long[], long)}.
     *
     * <p>The result AlgART array contains all elements the passed Java array:
     * <code>array[0], array[1], ..., array[array.length - 1]</code>,
     * and changes in the returned array "write through" to <code>array</code> argument.
     * The length and capacity of the returned array are equal to <code>array.length</code>.
     *
     * <p>For arrays, created by this method, {@link Array#isJavaArrayWrapper()}
     * method returns <code>true</code>.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not an array
     *                                  or <code>boolean[]</code> array.
     * @see Array#ja()
     */
    public static UpdatableArray asUpdatableArray(Object array) {
        Objects.requireNonNull(array, "Null array argument");
        if (array instanceof boolean[]) {
            throw new IllegalArgumentException("boolean[] Java array cannot be viewed as UpdatableArray");
            //[[Repeat() byte ==> char,,short,,int,,long,,float,,double,,Object;;
            //           Byte ==> Char,,Short,,Int,,Long,,Float,,Double,,Object]]
        } else if (array instanceof byte[]) {
            return new UpdatableJAByteArray((byte[]) array, ((byte[]) array).length, ((byte[]) array).length);
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        } else if (array instanceof char[]) {
            return new UpdatableJACharArray((char[]) array, ((char[]) array).length, ((char[]) array).length);
        } else if (array instanceof short[]) {
            return new UpdatableJAShortArray((short[]) array, ((short[]) array).length, ((short[]) array).length);
        } else if (array instanceof int[]) {
            return new UpdatableJAIntArray((int[]) array, ((int[]) array).length, ((int[]) array).length);
        } else if (array instanceof long[]) {
            return new UpdatableJALongArray((long[]) array, ((long[]) array).length, ((long[]) array).length);
        } else if (array instanceof float[]) {
            return new UpdatableJAFloatArray((float[]) array, ((float[]) array).length, ((float[]) array).length);
        } else if (array instanceof double[]) {
            return new UpdatableJADoubleArray((double[]) array, ((double[]) array).length, ((double[]) array).length);
        } else if (array instanceof Object[]) {
            return new UpdatableJAObjectArray((Object[]) array, ((Object[]) array).length, ((Object[]) array).length);
            //[[Repeat.AutoGeneratedEnd]]
        } else {
            throw new IllegalArgumentException("The argument is not a Java array: " + array.getClass());
        }
    }

    /**
     * Analog of {@link #asUpdatableArray(Object)} with the only difference, that this method
     * does not work with a Java array of objects.
     *
     * <p>This method has a brief alias: {@link PArray#as(Object)}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not an array,
     *                                  or <code>boolean[]</code> array, or <code>Objects[]</code> array.
     */
    public static UpdatablePArray asUpdatablePArray(Object array) {
        if (array instanceof Object[]) {
            throw new IllegalArgumentException("Object[] Java array cannot be viewed as UpdatablePArray");
        }
        return (UpdatablePArray) asUpdatableArray(array);
    }

    /**
     * Analog of {@link #asUpdatableArray(Object)} with the only difference, that this method
     * does not work with a Java array of objects.
     *
     * <p>This method has a brief alias: {@link PNumberArray#as(Object)}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not an array,
     *                                  or <code>boolean[]</code> array,
     *                                  or <code>char[]</code> array, or <code>Objects[]</code> array.
     */
    public static UpdatablePNumberArray asUpdatablePNumberArray(Object array) {
        if (array instanceof Object[]) {
            throw new IllegalArgumentException("Object[] Java array cannot be viewed as UpdatablePArray");
        }
        if (array instanceof boolean[]) {
            throw new IllegalArgumentException("boolean[] Java array cannot be viewed as UpdatablePArray");
        }
        if (array instanceof char[]) {
            throw new IllegalArgumentException("char[] Java array cannot be viewed as UpdatablePArray");
        }
        return (UpdatablePNumberArray) asUpdatableArray(array);
    }

    /**
     * Returns an unresizable AlgART bit array backed by the specified <code>long[]</code> array
     * according the packing rules, describing in {@link PackedBitArrays} class.
     * Changes in the returned array "write through" to <code>array</code> argument.
     * The length and capacity of the returned array are equal to the specified <code>length</code> argument.
     *
     * <p>This method has a brief alias: {@link BitArray#as(long[], long)}.</p>
     *
     * @param packedBitArray the source <code>long[]</code> array.
     * @param length         the length of the returned bit array.
     * @return an unresizable AlgART bit array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>length&lt;0</code> or
     *                                  if the passed <code>array</code> is too short to store
     *                                  <code>length</code> bits
     *                                  (i.e. if <code>array.length &lt; (length+63)/64).</code>
     * @see BitArray#jaBit()
     */
    public static UpdatableBitArray asUpdatableBitArray(long[] packedBitArray, long length) {
        Objects.requireNonNull(packedBitArray, "Null packedBitArray");
        if (length < 0) {
            throw new IllegalArgumentException("Negative length");
        }
        final long packedLength = PackedBitArrays.packedLength(length);
        if (packedLength > packedBitArray.length) {
            throw new IllegalArgumentException("Too short packedBitArray long[" + packedBitArray.length +
                    "]: it must contain at least " + packedLength + " long elements to store " + length + " bits");
        }
        return new UpdatableJABitArray(packedBitArray, length);

    }
    /*Repeat() char ==> byte,,short,,int,,long,,float,,double;;
               Char ==> Byte,,Short,,Int,,Long,,Float,,Double
     */

    /**
     * Equivalent to <code>(UpdatableCharArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link CharArray#as(char[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableCharArray asUpdatableCharArray(char[] array) {
        return (UpdatableCharArray) asUpdatableArray((Object) array);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <code>(UpdatableByteArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link ByteArray#as(byte[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableByteArray asUpdatableByteArray(byte[] array) {
        return (UpdatableByteArray) asUpdatableArray((Object) array);
    }

    /**
     * Equivalent to <code>(UpdatableShortArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link ShortArray#as(short[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableShortArray asUpdatableShortArray(short[] array) {
        return (UpdatableShortArray) asUpdatableArray((Object) array);
    }

    /**
     * Equivalent to <code>(UpdatableIntArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link IntArray#as(int[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableIntArray asUpdatableIntArray(int[] array) {
        return (UpdatableIntArray) asUpdatableArray((Object) array);
    }

    /**
     * Equivalent to <code>(UpdatableLongArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link LongArray#as(long[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableLongArray asUpdatableLongArray(long[] array) {
        return (UpdatableLongArray) asUpdatableArray((Object) array);
    }

    /**
     * Equivalent to <code>(UpdatableFloatArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link FloatArray#as(float[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableFloatArray asUpdatableFloatArray(float[] array) {
        return (UpdatableFloatArray) asUpdatableArray((Object) array);
    }

    /**
     * Equivalent to <code>(UpdatableDoubleArray){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * <p>This method has a brief alias: {@link DoubleArray#as(double[])}.</p>
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static UpdatableDoubleArray asUpdatableDoubleArray(double[] array) {
        return (UpdatableDoubleArray) asUpdatableArray((Object) array);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <code>(UpdatableObjectArray<E>){@link #asUpdatableArray(Object)
     * asUpdatableArray}((Object)array)</code>.
     *
     * @param <E>   the generic type of array elements.
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static <E> UpdatableObjectArray<E> asUpdatableObjectArray(E[] array) {
        return InternalUtils.cast(asUpdatableArray((Object) array));
    }

    /**
     * Equivalent to <code>{@link Matrices#matrix Matrices.matrix}({@link #asUpdatablePArray(Object)
     * asUpdatablePArray}(array), dim)</code> or, if the <code>array</code>argument is an instance of
     * {@link Array} interface, equivalent to <code>{@link Matrices#matrix Matrices.matrix}(array)</code>.
     *
     * <p>Note that this method <b>cannot</b> be used with <code>Object[]</code> array. If you still need to
     * return a matrix of objects, you may use {@link Matrices#matrix} method together with
     * {@link #asUpdatableArray(Object)}.
     *
     * <p>Note that this method <b>can</b> be used with AlgART array, but in this case it must
     * be an instance of {@link UpdatablePArray}. If you need to build a matrix on the base on other kind of
     * AlgART array, you may use {@link Matrices#matrix} method directly.</p>
     *
     * <p>This method has a brief alias: {@link Matrix#as(Object, long...)}.</p>
     *
     * @param array the source Java array.
     * @param dim   the matrix dimensions.
     * @return a matrix backed by the specified Java array with the specified dimensions.
     * @throws NullPointerException     if <code>array</code> or <code>dim</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not a Java array
     *                                  and is not {@link UpdatablePArray},
     *                                  or if it is <code>boolean[]</code> array, or array of objects,
     *                                  or if the number of dimensions is 0 (empty <code>dim</code> Java array),
     *                                  or if some of the dimensions are negative.
     * @throws SizeMismatchException    if the product of all dimensions is not equal to the array length.
     * @throws TooLargeArrayException   if the product of all dimensions is greater than <code>Long.MAX_VALUE</code>.
     */
    public static Matrix<UpdatablePArray> asMatrix(Object array, long... dim) {
        if (array instanceof Object[]) {
            throw new IllegalArgumentException("Object[] Java array cannot be viewed as Matrix<UpdatablePArray>");
        }
        if (array instanceof boolean[]) {
            throw new IllegalArgumentException("boolean[] Java array cannot be viewed as Matrix<UpdatablePArray>");
        }
        if (array instanceof Array) {
            if (!(array instanceof UpdatablePArray a)) {
                throw new IllegalArgumentException("AlgART array, which is not UpdatablePArray, " +
                        "cannot be viewed as Matrix<UpdatablePArray>: " + array);
            }
            return Matrices.matrix(a, dim);
        }
        return Matrices.matrix((UpdatablePArray) asUpdatableArray(array), dim);
    }

    static long maxSupportedLengthImpl(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (elementType == boolean.class) {
            return ((long) Integer.MAX_VALUE) << 6;
        } else {
            return Integer.MAX_VALUE;
        }
    }
}
