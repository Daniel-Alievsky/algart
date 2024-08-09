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

import net.algart.math.functions.Func;

import java.util.Objects;

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
     * <code>{@link #capacity()}*bitsPerElement()/8</code> bytes
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
     * For example, some JVM, theoretically, may store <code>byte</code> elements of <code>byte[]</code> array
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
     * <code>Integer.MIN_VALUE</code> for {@link IntArray},
     * <code>Long.MIN_VALUE</code> for {@link LongArray},
     * <code>valueForFloatingPoint</code> for {@link FloatArray} and {@link DoubleArray}.
     * For {@link PFixedArray fixed-point arrays} it is the minimal possible value,
     * that can stored in elements of this array
     * (<code>byte</code> and <code>short</code> elements are interpreted as unsigned).
     * This method is equivalent to
     * <code>{@link Arrays#minPossibleValue(Class, double) minPossibleValue}(thisArray.getClass(),
     * valueForFloatingPoint)</code>.
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
     * <code>Integer.MAX_VALUE</code> for {@link IntArray},
     * <code>Long.MAX_VALUE</code> for {@link LongArray},
     * <code>valueForFloatingPoint</code> for {@link FloatArray} and {@link DoubleArray}.
     * For {@link PFixedArray fixed-point arrays} it is the maximal possible value,
     * that can stored in elements of this array
     * (<code>byte</code> and <code>short</code> elements are interpreted as unsigned).
     * This method is equivalent to
     * <code>{@link Arrays#maxPossibleValue(Class, double) maxPossibleValue}(thisArray.getClass(),
     * valueForFloatingPoint)</code>.
     *
     * @param valueForFloatingPoint the value returned for floating-point array type.
     * @return the maximal possible value, that can stored in elements of this array,
     * if it is a fixed-point array, or the argument for floating-point arrays.
     * @see PFixedArray#maxPossibleValue()
     */
    double maxPossibleValue(double valueForFloatingPoint);

    /**
     * Returns the element #<code>index</code> converted to <code>double</code>:
     * <code>(double)(value&amp;0xFF)</code> for <code>byte</code> value,
     * <code>(double)(value&amp;0xFFFF)</code> for <code>short</code> value,
     * <code>(double)value</code> for <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code>, <code>char</code> values,
     * or <code>value?1.0:0.0</code> for <code>boolean</code> values.
     * Please note that this method returns unsigned values for byte and short arrays.
     * Returned value contains full information stored in the element,
     * excepting the case of very large <code>long</code> elements.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     * @see UpdatablePArray#setDouble(long, double)
     */
    double getDouble(long index);

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</code>
     * and <code>{@link #getDouble(long) getDouble}(k)==value</code>,
     * or <code>-1</code> if there is no such array element.
     *
     * <p>In particular, if <code>lowIndex&gt;=thisArray.{@link #length() length()}}</code>
     * or <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * <p>You may specify <code>lowIndex=0</code> and <code>highIndex=Long.MAX_VALUE</code> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long indexOf(long lowIndex, long highIndex, double value);

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>{@link #getDouble(long) getDouble}(k)==value</code>,
     * or <code>-1</code> if there is no such array element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=thisArray.{@link #length() length()}</code>,
     * the result is the same as if <code>highIndex==thisArray.{@link #length() length()}</code>.
     *
     * <p>You may specify <code>lowIndex=0</code> and <code>highIndex=Long.MAX_VALUE</code> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    long lastIndexOf(long lowIndex, long highIndex, double value);

    /**
     * Returns <code>true</code> if all elements of this array are zero
     * (<code>false</code> for <code>boolean[]</code> array, <code>(char)0</code> for <code>char[]</code>).
     * Returns <code>false</code> if at least one of elements of this array is non-zero.
     *
     * <p>For arrays of floating-point types ({@link PFloatingArray}),
     * this method considers that <code>+0.0==-0.0</code>: both values are considered to be zero.
     *
     * <p>If the {@link #length() length} of this array is 0 (the array is empty), returns <code>true</code>.
     *
     * <p>This method usually requires some time for execution, because it checks all array elements.
     *
     * @return <code>true</code> if and only if all elements of this array are zero, or if this array is empty.
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

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double */

    /**
     * Returns a newly created Java <code>byte[]</code> array containing all elements in this AlgART array,
     * cast to <code>byte</code> type if necessary.
     * Equivalent to <code>{@link #toByte(byte[]) toByte}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>byte</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaByte()
     * @see Matrix#toByte()
     */
    default byte[] toByte() {
        return toByte(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>byte</code> type if the array is not a {@link ByteArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>byte[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new byte[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof ByteArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, ByteArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>byte[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>byte</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toByte()
     * @see #jaByte()
     * @see Matrix#toByte(byte[])
     */
    default byte[] toByte(byte[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java byte[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new byte[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array byte[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof ByteArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, ByteArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof ByteArray a ? a.{@link ByteArray#ja()
     * ja()} : thisObject.{@link #toByte() toByte}()</code>.
     *
     * <p>This method may be used instead of {@link #toByte()},
     * if you need maximally quick access to this data in a form of <code>byte[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>byte[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>byte</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaByte()
     * @see #toByte()
     */
    default byte[] jaByte() {
        return this instanceof ByteArray a ? a.ja() : this.toByte();
    }/*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Returns a newly created Java <code>char[]</code> array containing all elements in this AlgART array,
     * cast to <code>char</code> type if necessary.
     * Equivalent to <code>{@link #toChar(char[]) toChar}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>char</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaChar()
     * @see Matrix#toChar()
     */
    default char[] toChar() {
        return toChar(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>char</code> type if the array is not a {@link CharArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>char[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new char[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof CharArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, CharArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>char[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>char</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toChar()
     * @see #jaChar()
     * @see Matrix#toChar(char[])
     */
    default char[] toChar(char[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java char[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new char[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array char[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof CharArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, CharArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof CharArray a ? a.{@link CharArray#ja()
     * ja()} : thisObject.{@link #toChar() toChar}()</code>.
     *
     * <p>This method may be used instead of {@link #toChar()},
     * if you need maximally quick access to this data in a form of <code>char[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>char[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>char</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaChar()
     * @see #toChar()
     */
    default char[] jaChar() {
        return this instanceof CharArray a ? a.ja() : this.toChar();
    }

    /**
     * Returns a newly created Java <code>short[]</code> array containing all elements in this AlgART array,
     * cast to <code>short</code> type if necessary.
     * Equivalent to <code>{@link #toShort(short[]) toShort}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>short</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaShort()
     * @see Matrix#toShort()
     */
    default short[] toShort() {
        return toShort(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>short</code> type if the array is not a {@link ShortArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>short[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new short[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof ShortArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, ShortArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>short[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>short</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toShort()
     * @see #jaShort()
     * @see Matrix#toShort(short[])
     */
    default short[] toShort(short[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java short[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new short[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array short[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof ShortArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, ShortArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof ShortArray a ? a.{@link ShortArray#ja()
     * ja()} : thisObject.{@link #toShort() toShort}()</code>.
     *
     * <p>This method may be used instead of {@link #toShort()},
     * if you need maximally quick access to this data in a form of <code>short[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>short[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>short</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaShort()
     * @see #toShort()
     */
    default short[] jaShort() {
        return this instanceof ShortArray a ? a.ja() : this.toShort();
    }

    /**
     * Returns a newly created Java <code>int[]</code> array containing all elements in this AlgART array,
     * cast to <code>int</code> type if necessary.
     * Equivalent to <code>{@link #toInt(int[]) toInt}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>int</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaInt()
     * @see Matrix#toInt()
     */
    default int[] toInt() {
        return toInt(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>int</code> type if the array is not a {@link IntArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>int[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new int[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof IntArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, IntArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>int[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>int</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toInt()
     * @see #jaInt()
     * @see Matrix#toInt(int[])
     */
    default int[] toInt(int[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java int[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new int[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array int[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof IntArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, IntArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof IntArray a ? a.{@link IntArray#ja()
     * ja()} : thisObject.{@link #toInt() toInt}()</code>.
     *
     * <p>This method may be used instead of {@link #toInt()},
     * if you need maximally quick access to this data in a form of <code>int[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>int[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>int</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaInt()
     * @see #toInt()
     */
    default int[] jaInt() {
        return this instanceof IntArray a ? a.ja() : this.toInt();
    }

    /**
     * Returns a newly created Java <code>long[]</code> array containing all elements in this AlgART array,
     * cast to <code>long</code> type if necessary.
     * Equivalent to <code>{@link #toLong(long[]) toLong}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>long</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaLong()
     * @see Matrix#toLong()
     */
    default long[] toLong() {
        return toLong(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>long</code> type if the array is not a {@link LongArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>long[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new long[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof LongArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, LongArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>long[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>long</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toLong()
     * @see #jaLong()
     * @see Matrix#toLong(long[])
     */
    default long[] toLong(long[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java long[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new long[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array long[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof LongArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, LongArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof LongArray a ? a.{@link LongArray#ja()
     * ja()} : thisObject.{@link #toLong() toLong}()</code>.
     *
     * <p>This method may be used instead of {@link #toLong()},
     * if you need maximally quick access to this data in a form of <code>long[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>long[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>long</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaLong()
     * @see #toLong()
     */
    default long[] jaLong() {
        return this instanceof LongArray a ? a.ja() : this.toLong();
    }

    /**
     * Returns a newly created Java <code>float[]</code> array containing all elements in this AlgART array,
     * cast to <code>float</code> type if necessary.
     * Equivalent to <code>{@link #toFloat(float[]) toFloat}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>float</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaFloat()
     * @see Matrix#toFloat()
     */
    default float[] toFloat() {
        return toFloat(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>float</code> type if the array is not a {@link FloatArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>float[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new float[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof FloatArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, FloatArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>float[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>float</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toFloat()
     * @see #jaFloat()
     * @see Matrix#toFloat(float[])
     */
    default float[] toFloat(float[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java float[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new float[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array float[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof FloatArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, FloatArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof FloatArray a ? a.{@link FloatArray#ja()
     * ja()} : thisObject.{@link #toFloat() toFloat}()</code>.
     *
     * <p>This method may be used instead of {@link #toFloat()},
     * if you need maximally quick access to this data in a form of <code>float[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>float[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>float</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaFloat()
     * @see #toFloat()
     */
    default float[] jaFloat() {
        return this instanceof FloatArray a ? a.ja() : this.toFloat();
    }

    /**
     * Returns a newly created Java <code>double[]</code> array containing all elements in this AlgART array,
     * cast to <code>double</code> type if necessary.
     * Equivalent to <code>{@link #toDouble(double[]) toDouble}(null)</code>.
     *
     * <p>This method must always allocate a new Java array.
     * The returned Java array will be "safe" in the sense that no references to it are stored inside this array.
     * The caller is thus free to modify the returned array.
     *
     * @return Java array containing all the elements in this array, cast to <code>double</code> type
     * according to AlgART rules.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toJavaArray()
     * @see #jaDouble()
     * @see Matrix#toDouble()
     */
    default double[] toDouble() {
        return toDouble(null);
    }

    /**
     * Copies all elements in the source AlgART array into the <code>result</code> array,
     * automatically casting the elements to <code>double</code> type if the array is not a {@link DoubleArray},
     * and return a reference to the <code>result</code>.
     * If <code>result</code> argument is {@code null}, creates a new <code>double[]</code> array
     * with a length equal to the source array length {@link Array#length() array.length()};
     * if it is not {@code null}, its length must be &ge;{@link Array#length32() array.length()}.
     * If the length of the source AlgART array is too large (greater than <code>Integer.MAX_VALUE</code>),
     * throws {@link TooLargeArrayException}.
     *
     * <p>This method is equivalent to the following code:</p>
     *
     * <pre>
     *      PArray array = thisArray;
     *      if (result == null) {
     *          result = new double[array.{@link Array#length32() length32()}];
     *      }
     *      if (!(array instanceof DoubleArray)) {
     *          array = {@link Arrays#asFuncArray(Func, Class, PArray...)
     *          Arrays.asFuncArray}(Func.IDENTITY, DoubleArray.class, array);
     *      }
     *      array.{@link Array#getData(long, Object) getData}(0, result);</pre>
     *
     * @param result the result <code>double[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in the source AlgART array,
     * cast to <code>double</code> type according to AlgART rules.
     * @throws TooLargeArrayException    if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException if the <code>result</code> argument is not {@code null}, but its length
     *                                   is too small: less than {@link Array#length32() array.length()}.
     * @see #toDouble()
     * @see #jaDouble()
     * @see Matrix#toDouble(double[])
     */
    default double[] toDouble(double[] result) {
        long len = length();
        if (len != (int) len) {
            throw new TooLargeArrayException("Cannot convert AlgART array to Java double[] array, "
                    + "because it is too large: " + this);
        }
        if (result == null) {
            result = new double[(int) len];
        } else {
            if (result.length < len) {
                throw new IndexOutOfBoundsException("Result array double[" + result.length +
                        "] is too short to read " + len + " elements of " + this);
            }
        }
        PArray array = this;
        if (!(array instanceof DoubleArray)) {
            array = Arrays.asFuncArray(Func.IDENTITY, DoubleArray.class, array);
        }
        array.getData(0, result);
        // - no sense to use multithreading here: it may even be anti-optimization
        return result;
    }

    /**
     * Equivalent to the following expression:
     * <code>thisObject instanceof DoubleArray a ? a.{@link DoubleArray#ja()
     * ja()} : thisObject.{@link #toDouble() toDouble}()</code>.
     *
     * <p>This method may be used instead of {@link #toDouble()},
     * if you need maximally quick access to this data in a form of <code>double[]</code> array
     * and there is high probability that this AlgART array is a {@link Array#isJavaArrayWrapper() wrapper}
     * for standard <code>double[]</code> array.</p>
     *
     * <p>Be careful: this method can potentially lead to <b>bugs</b> while inaccurate usage.
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. Typically you <b>should not</b> attempt to modify
     * the Java array returned by this method; this helps to avoid difficult bugs.</p>
     *
     * @return Java array containing all the elements in this array, cast to <code>double</code> type
     * according to AlgART rules.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see Matrix#jaDouble()
     * @see #toDouble()
     */
    default double[] jaDouble() {
        return this instanceof DoubleArray a ? a.ja() : this.toDouble();
    }/*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <code>(UpdatablePArray) {@link MemoryModel#newUnresizableArray(Class, long)
     * memoryModel.newUnresizableArray(elementType, length)}</code>, but with throwing
     * <code>IllegalArgumentException</code> in a case when the type casting to {@link UpdatablePArray}
     * is impossible (non-primitive element type).
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @param elementType the type of array elements.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if one of the arguments is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not a primitive class,
     *                                         or if the specified length is negative.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     */
    static UpdatablePArray newArray(MemoryModel memoryModel, Class<?> elementType, long length) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(elementType, "Null element type");
        if (!elementType.isPrimitive()) {
            throw new IllegalArgumentException("Not a primitive type: " + elementType);
        }
        return (UpdatablePArray) memoryModel.newUnresizableArray(elementType, length);
    }

    /**
     * Equivalent to <code>{@link #newArray(MemoryModel, Class, long)
     * newArray}({@link Arrays#SMM Arrays.SMM}, elementType, length)</code>.
     *
     * @param elementType the type of array elements.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>elementType</code> is not a primitive class,
     *                                  or if the specified length is negative.
     * @throws TooLargeArrayException   if the specified length is too large for {@link SimpleMemoryModel}.
     */
    static UpdatablePArray newArray(Class<?> elementType, long length) {
        return newArray(Arrays.SMM, elementType, length);
    }

    /**
     * Equivalent to <code>{@link SimpleMemoryModel#asUpdatablePArray(Object)
     * SimpleMemoryModel.asUpdatablePArray}(array)</code>.
     *
     * @param array the source Java array.
     * @return an unresizable AlgART array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not an array,
     *                                  or <code>boolean[]</code> array, or <code>Objects[]</code> array.
     */
    static UpdatablePArray as(Object array) {
        return SimpleMemoryModel.asUpdatablePArray(array);
    }
}
