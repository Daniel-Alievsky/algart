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
 * <p>AlgART array of <tt>boolean</tt> values, read-only access.</p>
 *
 * @author Daniel Alievsky
 */
public interface BitArray extends PFixedArray {
    Class<? extends BitArray> type();

    Class<? extends UpdatableBitArray> updatableType();

    Class<? extends MutableBitArray> mutableType();

    /**
     * Returns the element #<tt>index</tt>.
     *
     * @param index index of element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    boolean getBit(long index);

    /**
     * Returns the minimal index <tt>k</tt>, so that
     * <tt>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</tt>
     * and <tt>{@link #getBit(long) getBit}(k)==value</tt>,
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
    long indexOf(long lowIndex, long highIndex, boolean value);

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>{@link #getBit(long) getBit}(k)==value</tt>,
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
     * Returns the sequence of <tt>count</tt> bits (maximum 64 bits), starting from the bit <tt>#srcPos</tt>.
     *
     * <p>More precisely, the bit <tt>#(srcPos+k)</tt> will be returned in the bit <tt>#k</tt> of the returned
     * <tt>long</tt> value <tt>R</tt>: the first bit <tt>#srcPos</tt> will be equal to <tt>R&amp;1</tt>,
     * the following bit <tt>#(srcPos+1)</tt> will be equal to <tt>(R&gt;&gt;1)&amp;1</tt>, etc.
     * If <tt>count=0</tt>, the result is 0.</p>
     *
     * <p>The same result can be calculated using the following loop:</p>
     *
     * <pre>
     *      long result = 0;
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = {@link #getBit(long) getBit}(srcPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; k;
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <tt>count</tt> is greater than 1.</p>
     *
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of bits to be unpacked (must be in range 0..64).
     * @return the sequence of <tt>count</tt> bits.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array.
     * @throws IllegalArgumentException  if <tt>count &lt; 0</tt> or <tt>count &gt; 64</tt>.
     */
    default long getBits64(long srcPos, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64 method");
        }
        long result = 0;
        for (int k = 0; k < count; k++) {
            final long bit = getBit(srcPos + k) ? 1L : 0L;
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
