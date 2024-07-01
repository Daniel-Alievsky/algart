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

/*Repeat(INCLUDE_FROM_FILE, FloatArray.java, all)
  PFloating ==> PFixed ;;
  FloatArray ==> BitArray ;;
  FloatBuffer ==> BitBuffer ;;
  getFloat ==> getBit ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean ;;
  \@Override\s+default\s+boolean\[\]\s+jaBoolean(.*?)(?:\r(?!\n)|\n|\r\n)\s*}\s* ==>
     !! Auto-generated: NOT EDIT !! */

import java.util.Objects;

/**
 * <p>AlgART array of <code>boolean</code> values, read-only access.</p>
 *
 * @author Daniel Alievsky
 */
public interface BitArray extends PFixedArray {
    Class<? extends BitArray> type();

    Class<? extends UpdatableBitArray> updatableType();

    Class<? extends MutableBitArray> mutableType();

    /**
     * Returns the element #<code>index</code>.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    boolean getBit(long index);

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</code>
     * and <code>{@link #getBit(long) getBit}(k)==value</code>,
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
    long indexOf(long lowIndex, long highIndex, boolean value);

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>{@link #getBit(long) getBit}(k)==value</code>,
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
    long lastIndexOf(long lowIndex, long highIndex, boolean value);

    DataBitBuffer buffer(DataBuffer.AccessMode mode, long capacity);

    DataBitBuffer buffer(DataBuffer.AccessMode mode);

    DataBitBuffer buffer(long capacity);

    DataBitBuffer buffer();

    BitArray asImmutable();

    BitArray asTrustedImmutable();

    MutableBitArray mutableClone(MemoryModel memoryModel);

    UpdatableBitArray updatableClone(MemoryModel memoryModel);

    boolean[] ja();

    default Matrix<? extends BitArray> matrix(long... dim) {
        return Matrices.matrix(this, dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newUnresizableBitArray(long)
     * memoryModel.newUnresizableBitArray(length)}</code>.
     *
     * @param memoryModel the memory model, used for allocation new array.
     * @param length      the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws NullPointerException            if <code>memoryModel</code> argument is {@code null}.
     * @throws IllegalArgumentException        if the specified length is negative.
     * @throws UnsupportedElementTypeException if <code>boolean</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the specified length is too large for this memory model.
     */
    static UpdatableBitArray newArray(MemoryModel memoryModel, long length) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newUnresizableBitArray(length);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newUnresizableBitArray(long)
     * newUnresizableBitArray(length)}</code>.
     *
     * @param length the length and capacity of the array.
     * @return created unresizable AlgART array.
     * @throws IllegalArgumentException if the specified length is negative.
     * @throws TooLargeArrayException   if the specified length is too large for {@link SimpleMemoryModel}.
     */
    static UpdatableBitArray newArray(long length) {
        return Arrays.SMM.newUnresizableBitArray(length);
    }
    /*Repeat.IncludeEnd*/

    /**
     * Equivalent to <code>{@link #getBit(long) getBit()} ? 1 : 0</code>.
     *
     * <p>Note that this method is already declared in {@link PFixedArray}.
     * It is redeclared here only for documentation and code search purposes.</p>
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <code>0..length()-1</code>.
     */
    int getInt(long index);

    /**
     * Returns the sequence of <code>count</code> bits (maximum 64 bits),
     * starting from the bit <code>#arrayPos</code>.
     *
     * <p>More precisely, the bit <code>#(arrayPos+k)</code> will be returned
     * in the bit <code>#k</code> of the returned
     * <code>long</code> value <code>R</code>: the first bit <code>#arrayPos</code>
     * will be equal to <code>R&amp;1</code>,
     * the following bit <code>#(arrayPos+1)</code> will be equal to <code>(R&gt;&gt;1)&amp;1</code>, etc.
     * If <code>count=0</code>, the result is 0.</p>
     *
     * <p>The same result can be calculated using the following loop
     * (for correct <code>count</code> in the range 0..64):</p>
     *
     * <pre>
     *      long result = 0;
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = {@link #getBit(long) getBit}(arrayPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; k;
     *      }</pre>
     *
     * <p>But this method works significantly faster in basic implementations of this interface,
     * if <code>count</code> is greater than 1.</p>
     *
     * @param arrayPos position of the first bit read in the source array.
     * @param count    the number of bits to be unpacked (must be in range 0..64).
     * @return the sequence of <code>count</code> bits.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    default long getBits64(long arrayPos, int count) {
        if (arrayPos < 0) {
            throw new IndexOutOfBoundsException("Negative arrayPos argument: " + arrayPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot get > 64 bits in getBits64 method");
        }
        long result = 0;
        for (int k = 0; k < count; k++) {
            final long bit = getBit(arrayPos + k) ? 1L : 0L;
            result |= bit << k;
        }
        return result;
    }

    /**
     * Copies <code>count</code> bits of this array, starting from <code>arrayPos</code> index,
     * into the specified <i>packed</i> bit array, starting from <code>destArrayOffset</code> index.
     *
     * <p>This method is equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++)
     * &#32;   {@link PackedBitArrays#setBit(long[], long, boolean)
     * PackedBitArrays.setBit}(destArray, destArrayOffset + k, thisArray.{@link #getBit(long)
     * getBit}(arrayPos + k);
     * </pre>
     *
     * <p>but usually works much faster.</p>
     *
     * <p>Note: if <code>IndexOutOfBoundsException</code> occurs due to attempt to write data outside the passed
     * Java array, the target Java array can be partially filled.
     * In other words, this method <b>can be non-atomic regarding this failure</b>.
     * All other possible exceptions are checked in the very beginning of this method
     * before any other actions (the standard way for checking exceptions).
     *
     * @param arrayPos        starting position in this AlgART array.
     * @param destArray       the target packed bit array.
     * @param destArrayOffset starting position in the target packed bit array.
     * @param count           the number of bits to be copied.
     * @throws NullPointerException      if <code>destArray</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or target Java array.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code>.
     * @see BitArray#getData(long, Object, int, int)
     * @see UpdatableBitArray#setBits(long, long[], long, long)
     * @see PackedBitArrays
     */
    void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count);

    /**
     * Returns the minimal <code>pos &gt;= max(position,0)</code>, for which the calls
     * {@link #getBits(long, long[], long, long) getBits(pos, destArray, 0, someCount)}
     * and (for updatable array)
     * {@link UpdatableBitArray#setBits(long, long[], long, long) setBits(pos, srcArray, 0, someCount)}
     * work essentially faster than for most of the other ("slow") positions,
     * or <code>-1</code> if there is no such position,
     * in particular, if <code>position&gt;={@link #length() length()}</code>.
     *
     * <p>These calls perform copying bits to / from a bit array (with zero offset),
     * where the bits are packed into usual Java <code>long[]</code> array.
     * For many implementations of this interface, in particular, for AlgART bit arrays
     * created by all {@link SimpleMemoryModel simple}, {@link BufferMemoryModel buffer} and
     * {@link LargeMemoryModel large memory models}, it means copying bits between internal
     * packed representation and Java <code>long[]</code>array. If the bit index "<code>position</code>" corresponds
     * to a beginning of some packed portion of such representation (usually <code>long</code> value),
     * in other words, if the position is <i>aligned</i>,
     * then the copying may be performed quickly: it is just copying a sequence of <code>long</code> values.
     * In other case, the copying requires bit shift of <code>long</code> values.
     * This method returns the nearest aligned position, starting from the passed index,
     * for such AlgART bit arrays.
     *
     * <p>In common case, when you call
     * {@link #getBits(long, long[], long, long) getBits(pos, destArray, arrayOffset, someCount)}
     * and (for updatable array)
     * {@link UpdatableBitArray#setBits(long, long[], long, long)
     * setBits(pos, srcArray, arrayOffset, someCount)}
     * with any (non-zero) <code>arrayOffset</code>,
     * you may suppose that these calls are quick, for most implementations,
     * when <code>pos=qp+arrayOffset&plusmn;64*j</code>,
     * where <code>qp!=-1</code> is the result of this method for the given position
     * and <code>j=0,1,2,...</code> is any integer.
     *
     * <p>This method may return <code>-1</code> in some cases: it means that there are no preferred
     * indexes in the bit array, at least, after the passed index <code>position</code>.
     * (For example, <code>-1</code> is usually returned for any <code>position</code> argument
     * in "lazy" arrays, created by
     * {@link Arrays#asFuncArray(boolean, net.algart.math.functions.Func, Class, PArray...)
     * Arrays.asFuncArray}
     * and similar methods.)
     * In other case, the returned value is usually in a range <code>position..position+63</code>.
     *
     * <p>Important note: if this method returns <code>-1</code> for the argument <code>position=0</code>, it means
     * that {@link #getBits getBits} method never (or almost never) work faster than
     * {@link #getData(long, Object, int, int) getData} for any positions in this bit array.
     * Moreover, if <code>{@link #nextQuickPosition(long) nextQuickPosition}(0)==-1</code>,
     * then {@link #getBits getBits}, as well as the standard {@link DataBitBuffer data buffer},
     * probably works slower than {@link #getData(long, Object, int, int) getData}.
     * In this situation, it's good idea to choose the algorithm branch based on
     * {@link #getData(long, Object, int, int) getData} and processing <code>boolean[]</code> arrays.
     *
     * <p>For arrays, created by {@link Arrays#nBitCopies(long, boolean)} method,
     * this method just returns <code>max(position,0)</code>
     * (or <code>-1</code> if <code>position&gt;={@link #length() length()}</code>):
     * all positions are good enough in such arrays.
     *
     * <p>For negative <code>position</code> argument this method returns
     * the same result as for <code>position=0</code>.
     *
     * @param position some index inside this bit array.
     * @return the minimal "quick" (usually "aligned") position starting from this index.
     * @see Arrays#goodStartOffsetInArrayOfLongs(BitArray, long, int)
     */
    long nextQuickPosition(long position);

    /**
     * Returns <code>true</code> this array is actually a <i>wrapper</i> for
     * a packed bit array,
     * like wrappers returned by {@link SimpleMemoryModel#asUpdatableBitArray(long[], long)} method
     * (see {@link PackedBitArrays} class about packed bit <code>long[]</code> arrays).
     * That packed bit array is returned by {@link #jaBit()} method,
     * if and only if this method returns <code>true</code>;
     * otherwise {@link #jaBit()} method returns a copy of array data.
     *
     * <p>Note that this method returns <code>false</code> for {@link #subArray(long, long) subarrays} with non-zero
     * offset. Also, it returns <code>false</code> if the underlying packed array contains more than
     * <code>{@link PackedBitArrays#packedLength(long)
     * PackedBitArrays.packedLength}(thisArray.{@link Array#length() length()}</code> elements:
     * possible for a growing {@link MutableBitArray}.
     * In other words, the situation is similar to {@link Array#isJavaArrayWrapper()} method,
     * but for packed bits.
     *
     * @return whether this array is a wrapper for a packed bit array.
     * @see #jaBit()
     */
    default boolean isPackedBitArrayWrapper() {
        return false;
    }

    /**
     * Returns a reference to the underlying packed bit array, if this AlgART array is its wrapper
     * (see {@link #isPackedBitArrayWrapper()}, otherwise returns
     * <code>{@link Arrays#toPackedBitArray(BitArray) Arrays.toPackedBitArray(thisArray)}</code>.
     *
     * <p>In other words, this method returns a packed bit array, identical to this AlgART array
     * in terms of {@link PackedBitArrays} class.</p>
     *
     * <p>The returned array is always identical to
     * the result of {@link Arrays#toPackedBitArray(BitArray) Arrays.toPackedBitArray(thisArray)}.
     * But this method works very quickly when possible, in particular, for most bit arrays created
     * by {@link SimpleMemoryModel}.</p>
     *
     * <p>Be careful: this method can potentially lead to bugs while inaccurate usage.
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
     * 1) this object must be an instance {@link UpdatableBitArray};
     * 2) {@link #isPackedBitArrayWrapper()} method must return <code>true</code>.</p>
     *
     * @return packed Java bit array containing all the bits in this array.
     * @see #isPackedBitArrayWrapper()
     */
    default long[] jaBit() {
        return Arrays.toPackedBitArray(this);
    }

    /**
     * Equivalent to <code>{@link SimpleMemoryModel#asUpdatableBitArray(long[], long)
     * SimpleMemoryModel.asUpdatableBitArray}(packedBitArray, length)</code>.
     *
     * @param packedBitArray the source <code>long[]</code> array.
     * @param length         the length of the returned bit array.
     * @return an unresizable AlgART bit array backed by the specified Java array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>length&lt;0</code> or
     *                                  if the passed <code>array</code> is too short to store
     *                                  <code>length</code> bits
     *                                  (i.e. if <code>array.length &lt; (length+63)/64).</code>
     */
    static UpdatableBitArray as(long[] packedBitArray, long length) {
        return SimpleMemoryModel.asUpdatableBitArray(packedBitArray, length);
    }
}
