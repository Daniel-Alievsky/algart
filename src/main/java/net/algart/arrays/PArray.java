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
 * <p>AlgART array of primitive elements (boolean, char, byte, short, int, long, float or double),
 * read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link BitArray}, {@link CharArray},
 * {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray},
 * {@link FloatArray}, {@link DoubleArray}
 * subinterfaces.</p>
 *
 * @author Daniel Alievsky
 */
public interface PArray extends Array {
    /**
     * Return the number of memory bits occupied by every element of this array.
     * The amount of memory used by the array can be estimated as
     * <tt>{@link #capacity()}*bitsPerElement()/8</tt> bytes
     * (when the array capacity is large enough).
     *
     * <p>If the number of occupied bits is not defined (for example, may depend on JVM implementation),
     * this method returns -1.
     *
     * <p>For implementations from this package, this method returns:<ul>
     * <li>1 for {@link BitArray} (the value of {@link Arrays#BITS_PER_BIT} constant),</li>
     * <li>16 for {@link CharArray} (the value of {@link Arrays#BITS_PER_CHAR} constant),</li>
     * <li>8 for {@link ByteArray} (the value of {@link Arrays#BITS_PER_BYTE} constant),</li>
     * <li>16 for {@link ShortArray} (the value of {@link Arrays#BITS_PER_SHORT} constant),</li>
     * <li>32 for {@link IntArray} (the value of {@link Arrays#BITS_PER_INT} constant),</li>
     * <li>64 for {@link LongArray} (the value of {@link Arrays#BITS_PER_LONG} constant),</li>
     * <li>32 for {@link FloatArray} (the value of {@link Arrays#BITS_PER_FLOAT} constant),</li>
     * <li>64 for {@link DoubleArray} (the value of {@link Arrays#BITS_PER_DOUBLE} constant).</li>
     * </ul>
     * <p>(-1 result is never returned by implementations from this package.)
     *
     * <p>Please keep in mind that the real amount of occupied memory, theoretically, can differ
     * from the value returned by this method.
     * For example, some JVM, theoretically, may store <tt>byte</tt> elements of <tt>byte[]</tt> array
     * in 32-bit memory words. In this case, this method will return invalid result for byte arrays
     * created by the {@link SimpleMemoryModel simple memory model}.
     * <i>However:</i> we guarantee the results of this method are always correct for arrays created
     * by the {@link BufferMemoryModel buffer memory model} and {@link LargeMemoryModel large memory model}.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a constant or a value of some private field).
     *
     * @return the number of bytes occupied by every element of this array, or -1 if it cannot be determined.
     * @see Arrays#bitsPerElement(Class)
     */
    long bitsPerElement();

    /**
     * Returns 0 for {@link BitArray}, {@link ByteArray}, {@link CharArray} and {@link ShortArray},
     * <tt>Integer.MIN_VALUE</tt> for {@link IntArray},
     * <tt>Long.MIN_VALUE</tt> for {@link LongArray},
     * <tt>valueForFloatingPoint</tt> for {@link FloatArray} and {@link DoubleArray}.
     * For {@link PFixedArray fixed-point arrays} it is the minimal possible value,
     * that can stored in elements of this array
     * (<tt>byte</tt> and <tt>short</tt> elements are interpreted as unsigned).
     * This method is equivalent to
     * <tt>{@link Arrays#minPossibleValue(Class, double) minPossibleValue}(thisArray.getClass(),
     * valueForFloatingPoint)</tt>.
     *
     * @param valueForFloatingPoint the value returned for floating-point array type.
     * @return the minimal possible value, that can stored in elements of this array,
     * if it is a fixed-point array, or the argument for floating-point arrays.
     * @see PFixedArray#minPossibleValue()
     */
    double minPossibleValue(double valueForFloatingPoint);

    /**
     * Returns 1 for {@link BitArray},
     * 0xFF for {@link ByteArray},
     * 0xFFFF for {@link CharArray} and {@link ShortArray},
     * <tt>Integer.MAX_VALUE</tt> for {@link IntArray},
     * <tt>Long.MAX_VALUE</tt> for {@link LongArray},
     * <tt>valueForFloatingPoint</tt> for {@link FloatArray} and {@link DoubleArray}.
     * For {@link PFixedArray fixed-point arrays} it is the maximal possible value,
     * that can stored in elements of this array
     * (<tt>byte</tt> and <tt>short</tt> elements are interpreted as unsigned).
     * This method is equivalent to
     * <tt>{@link Arrays#maxPossibleValue(Class, double) maxPossibleValue}(thisArray.getClass(),
     * valueForFloatingPoint)</tt>.
     *
     * @param valueForFloatingPoint the value returned for floating-point array type.
     * @return the maximal possible value, that can stored in elements of this array,
     * if it is a fixed-point array, or the argument for floating-point arrays.
     * @see PFixedArray#maxPossibleValue()
     */
    double maxPossibleValue(double valueForFloatingPoint);

    /**
     * Returns the element #<tt>index</tt> converted to <tt>double</tt>:
     * <tt>(double)(value&amp;0xFF)</tt> for <tt>byte</tt> value,
     * <tt>(double)(value&amp;0xFFFF)</tt> for <tt>short</tt> value,
     * <tt>(double)value</tt> for <tt>int</tt>, <tt>long</tt>,
     * <tt>float</tt>, <tt>double</tt>, <tt>char</tt> values,
     * or <tt>value?1.0:0.0</tt> for <tt>boolean</tt> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * excepting a case of very large <tt>long</tt> elements.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @see UpdatablePArray#setDouble(long, double)
     */
    double getDouble(long index);

    /**
     * Returns the minimal index <tt>k</tt>, so that
     * <tt>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</tt>
     * and <tt>{@link #getDouble(long) getDouble}(k)==value</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=thisArray.{@link #length() length()}}</tt>
     * or <tt>lowIndex&gt;=highIndex</tt>, this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array
     * in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     * or <tt>-1</tt> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, double value);

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>{@link #getDouble(long) getDouble}(k)==value</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=thisArray.{@link #length() length()}</tt>,
     * the result is the same as if <tt>highIndex==thisArray.{@link #length() length()}</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array
     * in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     * or <tt>-1</tt> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, double value);

    /**
     * Returns <tt>true</tt> if all elements of this array are zero
     * (<tt>false</tt> for <tt>boolean[]</tt> array, <tt>(char)0</tt> for <tt>char[]</tt>).
     * Returns <tt>false</tt> if at least one of elements of this array is non-zero.
     *
     * <p>For arrays of floating-point types ({@link PFloatingArray}),
     * this method considers that <tt>+0.0==-0.0</tt>: both values are considered to be zero.
     *
     * <p>If the {@link #length() length} of this array is 0 (the array is empty), returns <tt>true</tt>.
     *
     * <p>This method usually requires some time for execution, because it checks all array elements.
     *
     * @return <tt>true</tt> if and only if all elements of this array are zero, or if this array is empty.
     */
    boolean isZeroFilled();

    Class<? extends PArray> type();

    Class<? extends UpdatablePArray> updatableType();

    Class<? extends MutablePArray> mutableType();

    PArray asImmutable();

    PArray asTrustedImmutable();

    MutablePArray mutableClone(MemoryModel memoryModel);

    UpdatablePArray updatableClone(MemoryModel memoryModel);

    @Override
    default Matrix<? extends PArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }

    /*Repeat() byte ==> short,,int,,long,,float,,double;;
               Byte ==> Short,,Int,,Long,,Float,,Double */

    /**
     * Equivalent to the following expression:
     * <tt>thisObject instanceof ByteArray a ? a.{@link ByteArray#ja()
     * ja()} : {@link Arrays#toByteJavaArray(PArray) Arrays.toByteJavaArray}(thisObject)</tt>.
     *
     * <p>This method may be used instead of {@link Arrays#toByteJavaArray(PArray)},
     * if you need maximally quick access to this data in a form of <tt>byte[]</tt> array
     * and there is high probability that this AlgART array is a {@link Arrays#isJavaArrayWrapper(Array) wrapper}
     * for standard <tt>byte[]</tt> array.</p>
     *
     * <p><b>Be careful: this method is potentially unsafe!</b> The main purpose of this method
     * is to quickly access array data for <i>reading</i>. But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <tt>byte</tt> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <tt>Integer.MAX_VALUE</tt>.
     */
    default byte[] jaByte() {
        return this instanceof ByteArray a ? a.ja() : Arrays.toByteJavaArray(this);
    }/*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to the following expression:
     * <tt>thisObject instanceof ShortArray a ? a.{@link ShortArray#ja()
     * ja()} : {@link Arrays#toShortJavaArray(PArray) Arrays.toShortJavaArray}(thisObject)</tt>.
     *
     * <p>This method may be used instead of {@link Arrays#toShortJavaArray(PArray)},
     * if you need maximally quick access to this data in a form of <tt>short[]</tt> array
     * and there is high probability that this AlgART array is a {@link Arrays#isJavaArrayWrapper(Array) wrapper}
     * for standard <tt>short[]</tt> array.</p>
     *
     * <p><b>Be careful: this method is potentially unsafe!</b> The main purpose of this method
     * is to quickly access array data for <i>reading</i>. But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <tt>short</tt> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <tt>Integer.MAX_VALUE</tt>.
     */
    default short[] jaShort() {
        return this instanceof ShortArray a ? a.ja() : Arrays.toShortJavaArray(this);
    }

    /**
     * Equivalent to the following expression:
     * <tt>thisObject instanceof IntArray a ? a.{@link IntArray#ja()
     * ja()} : {@link Arrays#toIntJavaArray(PArray) Arrays.toIntJavaArray}(thisObject)</tt>.
     *
     * <p>This method may be used instead of {@link Arrays#toIntJavaArray(PArray)},
     * if you need maximally quick access to this data in a form of <tt>int[]</tt> array
     * and there is high probability that this AlgART array is a {@link Arrays#isJavaArrayWrapper(Array) wrapper}
     * for standard <tt>int[]</tt> array.</p>
     *
     * <p><b>Be careful: this method is potentially unsafe!</b> The main purpose of this method
     * is to quickly access array data for <i>reading</i>. But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <tt>int</tt> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <tt>Integer.MAX_VALUE</tt>.
     */
    default int[] jaInt() {
        return this instanceof IntArray a ? a.ja() : Arrays.toIntJavaArray(this);
    }

    /**
     * Equivalent to the following expression:
     * <tt>thisObject instanceof LongArray a ? a.{@link LongArray#ja()
     * ja()} : {@link Arrays#toLongJavaArray(PArray) Arrays.toLongJavaArray}(thisObject)</tt>.
     *
     * <p>This method may be used instead of {@link Arrays#toLongJavaArray(PArray)},
     * if you need maximally quick access to this data in a form of <tt>long[]</tt> array
     * and there is high probability that this AlgART array is a {@link Arrays#isJavaArrayWrapper(Array) wrapper}
     * for standard <tt>long[]</tt> array.</p>
     *
     * <p><b>Be careful: this method is potentially unsafe!</b> The main purpose of this method
     * is to quickly access array data for <i>reading</i>. But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <tt>long</tt> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <tt>Integer.MAX_VALUE</tt>.
     */
    default long[] jaLong() {
        return this instanceof LongArray a ? a.ja() : Arrays.toLongJavaArray(this);
    }

    /**
     * Equivalent to the following expression:
     * <tt>thisObject instanceof FloatArray a ? a.{@link FloatArray#ja()
     * ja()} : {@link Arrays#toFloatJavaArray(PArray) Arrays.toFloatJavaArray}(thisObject)</tt>.
     *
     * <p>This method may be used instead of {@link Arrays#toFloatJavaArray(PArray)},
     * if you need maximally quick access to this data in a form of <tt>float[]</tt> array
     * and there is high probability that this AlgART array is a {@link Arrays#isJavaArrayWrapper(Array) wrapper}
     * for standard <tt>float[]</tt> array.</p>
     *
     * <p><b>Be careful: this method is potentially unsafe!</b> The main purpose of this method
     * is to quickly access array data for <i>reading</i>. But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <tt>float</tt> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <tt>Integer.MAX_VALUE</tt>.
     */
    default float[] jaFloat() {
        return this instanceof FloatArray a ? a.ja() : Arrays.toFloatJavaArray(this);
    }

    /**
     * Equivalent to the following expression:
     * <tt>thisObject instanceof DoubleArray a ? a.{@link DoubleArray#ja()
     * ja()} : {@link Arrays#toDoubleJavaArray(PArray) Arrays.toDoubleJavaArray}(thisObject)</tt>.
     *
     * <p>This method may be used instead of {@link Arrays#toDoubleJavaArray(PArray)},
     * if you need maximally quick access to this data in a form of <tt>double[]</tt> array
     * and there is high probability that this AlgART array is a {@link Arrays#isJavaArrayWrapper(Array) wrapper}
     * for standard <tt>double[]</tt> array.</p>
     *
     * <p><b>Be careful: this method is potentially unsafe!</b> The main purpose of this method
     * is to quickly access array data for <i>reading</i>. But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <tt>double</tt> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <tt>Integer.MAX_VALUE</tt>.
     */
    default double[] jaDouble() {
        return this instanceof DoubleArray a ? a.ja() : Arrays.toDoubleJavaArray(this);
    }/*Repeat.AutoGeneratedEnd*/
}
