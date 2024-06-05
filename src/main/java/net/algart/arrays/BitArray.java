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
  (or <tt>null</tt> in other case\.) ==> $1
     * Usually, this method always returns <tt>null</tt>,
     * because typical implementation packs bits in another primitive types
     * (as <tt>int</tt> or <tt>long</tt>). ;;
  PFloating ==> PFixed ;;
  FloatArray ==> BitArray ;;
  FloatBuffer ==> BitBuffer ;;
  getFloat ==> getBit ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean
     !! Auto-generated: NOT EDIT !! */

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
    /*Repeat.IncludeEnd*/

    /**
     * Equivalent to <tt>{@link #getBit(long) getBit()} ? 1 : 0</tt>.
     *
     * <p>Note that this method is already declared in {@link PFixedArray}.
     * It is redeclared here only for documentation and code search purposes.</p>
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     */
    int getInt(long index);

    /**
     * Returns the sequence of <tt>count</tt> bits (maximum 64 bits), starting from the bit <tt>#arrayPos</tt>.
     *
     * <p>More precisely, the bit <tt>#(arrayPos+k)</tt> will be returned in the bit <tt>#k</tt> of the returned
     * <tt>long</tt> value <tt>R</tt>: the first bit <tt>#arrayPos</tt> will be equal to <tt>R&amp;1</tt>,
     * the following bit <tt>#(arrayPos+1)</tt> will be equal to <tt>(R&gt;&gt;1)&amp;1</tt>, etc.
     * If <tt>count=0</tt>, the result is 0.</p>
     *
     * <p>The same result can be calculated using the following loop
     * (for correct <tt>count</tt> in the range 0..64):</p>
     *
     * <pre>
     *      long result = 0;
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = {@link #getBit(long) getBit}(arrayPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; k;
     *      }</pre>
     *
     * <p>But this method works significantly faster in basic implementations of this interface,
     * if <tt>count</tt> is greater than 1.</p>
     *
     * @param arrayPos position of the first bit read in the source array.
     * @param count    the number of bits to be unpacked (must be in range 0..64).
     * @return the sequence of <tt>count</tt> bits.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array.
     * @throws IllegalArgumentException  if <tt>count &lt; 0</tt> or <tt>count &gt; 64</tt>.
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
     * Copies <tt>count</tt> bits of this array, starting from <tt>arrayPos</tt> index,
     * into the specified <i>packed</i> bit array, starting from <tt>destArrayOffset</tt> index.
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
     * <p>Note: if <tt>IndexOutOfBoundsException</tt> occurs due to attempt to write data outside the passed
     * Java array, the target Java array can be partially filled.
     * In other words, this method <b>can be non-atomic regarding this failure</b>.
     * All other possible exceptions are checked in the very beginning of this method
     * before any other actions (the standard way for checking exceptions).
     *
     * @param arrayPos        starting position in this AlgART array.
     * @param destArray       the target packed bit array.
     * @param destArrayOffset starting position in the target packed bit array.
     * @param count           the number of bits to be copied.
     * @throws NullPointerException      if <tt>destArray</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or target Java array.
     * @throws IllegalArgumentException  if <tt>count &lt; 0</tt>.
     * @see BitArray#getData(long, Object, int, int)
     * @see UpdatableBitArray#setBits(long, long[], long, long)
     * @see PackedBitArrays
     */
    void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count);

    /**
     * Returns the minimal <tt>pos &gt;= max(position,0)</tt>, for which the calls
     * {@link #getBits(long, long[], long, long) getBits(pos, destArray, 0, someCount)}
     * and (for updatable array)
     * {@link UpdatableBitArray#setBits(long, long[], long, long) setBits(pos, srcArray, 0, someCount)}
     * work essentially faster than for most of the other ("slow") positions,
     * or <tt>-1</tt> if there is no such position,
     * in particular, if <tt>position&gt;={@link #length() length()}</tt>.
     *
     * <p>These calls perform copying bits to / from a bit array (with zero offset),
     * where the bits are packed into usual Java <tt>long[]</tt> array.
     * For many implementations of this interface, in particular, for AlgART bit arrays
     * created by all {@link SimpleMemoryModel simple}, {@link BufferMemoryModel buffer} and
     * {@link LargeMemoryModel large memory models}, it means copying bits between internal
     * packed representation and Java <tt>long[]</tt>array. If the bit index "<tt>position</tt>" corresponds
     * to a beginning of some packed portion of such representation (usually <tt>long</tt> value),
     * in other words, if the position is <i>aligned</i>,
     * then the copying may be performed quickly: it is just copying a sequence of <tt>long</tt> values.
     * In other case, the copying requires bit shift of <tt>long</tt> values.
     * This method returns the nearest aligned position, starting from the passed index,
     * for such AlgART bit arrays.
     *
     * <p>In common case, when you call
     * {@link #getBits(long, long[], long, long) getBits(pos, destArray, arrayOffset, someCount)}
     * and (for updatable array)
     * {@link UpdatableBitArray#setBits(long, long[], long, long)
     * setBits(pos, srcArray, arrayOffset, someCount)}
     * with any (non-zero) <tt>arrayOffset</tt>,
     * you may suppose that these calls are quick, for most implementations,
     * when <tt>pos=qp+arrayOffset&plusmn;64*j</tt>,
     * where <tt>qp!=-1</tt> is the result of this method for the given position
     * and <tt>j=0,1,2,...</tt> is any integer.
     *
     * <p>This method may return <tt>-1</tt> in some cases: it means that there are no preferred
     * indexes in the bit array, at least, after the passed index <tt>position</tt>.
     * (For example, <tt>-1</tt> is usually returned for any <tt>position</tt> argument
     * in "lazy" arrays, created by
     * {@link Arrays#asFuncArray(boolean, net.algart.math.functions.Func, Class, PArray...)
     * Arrays.asFuncArray}
     * and similar methods.)
     * In other case, the returned value is usually in a range <tt>position..position+63</tt>.
     *
     * <p>Important note: if this method returns <tt>-1</tt> for the argument <tt>position=0</tt>, it means
     * that {@link #getBits getBits} method never (or almost never) work faster than
     * {@link #getData(long, Object, int, int) getData} for any positions in this bit array.
     * Moreover, if <tt>{@link #nextQuickPosition(long) nextQuickPosition}(0)==-1</tt>,
     * then {@link #getBits getBits}, as well as the standard {@link DataBitBuffer data buffer},
     * probably works slower than {@link #getData(long, Object, int, int) getData}.
     * In this situation, it's good idea to choose the algorithm branch based on
     * {@link #getData(long, Object, int, int) getData} and processing <tt>boolean[]</tt> arrays.
     *
     * <p>For arrays, created by {@link Arrays#nBitCopies(long, boolean)} method,
     * this method just returns <tt>max(position,0)</tt>
     * (or <tt>-1</tt> if <tt>position&gt;={@link #length() length()}</tt>):
     * all positions are good enough in such arrays.
     *
     * <p>For negative <tt>position</tt> argument this method returns the same result as for <tt>position=0</tt>.
     *
     * @param position some index inside this bit array.
     * @return the minimal "quick" (usually "aligned") position starting from this index.
     * @see Arrays#goodStartOffsetInArrayOfLongs(BitArray, long, int)
     */
    long nextQuickPosition(long position);

    /**
     * Returns <tt>true</tt> this array is actually a <i>wrapper</i> for
     * a packed bit array,
     * like wrappers returned by {@link SimpleMemoryModel#asUpdatableBitArray(long[], long)} method
     * (see {@link PackedBitArrays} class about packed bit <tt>long[]</tt> arrays).
     *
     * <p>Note that this method returns <tt>false</tt> for {@link #subArray(long, long) subarrays} with non-zero
     * offset. Also, it returns <tt>false</tt> if the underlying packed array contains more than
     * <code>{@link PackedBitArrays#packedLength(long)
     * PackedBitArrays.packedLength}(thisArray.{@link Array#length() length()}</code> elements:
     * possible for a growing {@link MutableBitArray}.
     * In other words, the situation is similar to {@link Array#isJavaArrayWrapper()} method,
     * but for packed bits.
     *
     * @return whether this array is a wrapper for a packed bit array.
     * @see #jaBits()
     */
    default boolean isPackedBitArrayWrapper() {
        return false;
    }

    /**
     * Returns a reference to the underlying packed bit array, if this AlgART array is its wrapper
     * (see {@link #isPackedBitArrayWrapper()}, otherwise returns
     * <tt>{@link Arrays#toPackedBitArray(BitArray) Arrays.toPackedBitArray(thisArray)} in other case.
     *
     * <p>In other words, this method returns a packed bit array, identical to this AlgART array
     * in terms of {@link PackedBitArrays} class.</p>
     *
     * <p>The returned array is always identical to
     * the result of {@link Arrays#toPackedBitArray(BitArray) Arrays.toPackedBitArray(thisArray)}.
     * But this method works very quickly when possible, in particular, for most bit arrays created
     * by {@link SimpleMemoryModel}.</p>
     *
     * <p><b>Be careful: this method can be potentially unsafe while inaccurate usage!</b>
     * The main purpose of this method is to quickly access array data for <i>reading</i>.
     * But it also allows you to <i>modify</i> this data,
     * and the result of such modification is unpredictable: this may change the original AlgART array,
     * but may also not change. (Of course, this is impossible for {@link #isImmutable() immutable} arrays.)
     * Typically you <b>should not</b> attempt to modify the Java array returned by this method;
     * this helps to avoid difficult bugs.</p>
     *
     * @return packed Java bit array containing all the bits in this array.
     * @see #isPackedBitArrayWrapper()
     */
    default long[] jaBits() {
        return Arrays.toPackedBitArray(this);
    }

    /**
     * Equivalent to <tt>{@link SimpleMemoryModel#asUpdatableBitArray(long[], long)
     * SimpleMemoryModel.asUpdatableBitArray}(packedBitArray, length)</tt>.
     *
     * @param packedBitArray the source <tt>long[]</tt>> array.
     * @param length         the length of the returned bit array.
     * @return an unresizable AlgART bit array backed by the specified Java array.
     * @throws NullPointerException     if <tt>array</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>length&lt;0</tt> or
     *                                  if the passed <tt>array</tt> is too short to store
     *                                  <tt>length</tt> bits (i.e. if <tt>array.length &lt; (length+63)/64).</tt>
     */
    static UpdatableBitArray as(long[] packedBitArray, long length) {
        return SimpleMemoryModel.asUpdatableBitArray(packedBitArray, length);
    }
}
