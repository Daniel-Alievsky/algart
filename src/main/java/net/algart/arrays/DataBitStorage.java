/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Bit data storage: an addition to usual {@link DataStorage} allowing to read/write packed bits.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
interface DataBitStorage {
    /**
     * Copies <tt>count</tt> bits of this storage, starting from <tt>pos</tt> index,
     * into the specified packed bit array, starting from <tt>destArrayOffset</tt> index.
     *
     * @param pos             starting position in the stored AlgART array (not sub-array).
     * @param destArray       the target packed bit array.
     * @param destArrayOffset starting position in the target packed bit array.
     * @param count           the number of bits to be copied.
     * @see PackedBitArrays
     */
    void getBits(long pos, long[] destArray, long destArrayOffset, long count);

    /**
     * Copies <tt>count</tt> bits from the specified packed bit array,
     * starting from <tt>srcArrayOffset</tt> index,
     * into this storage, starting from <tt>pos</tt> index.
     *
     * @param pos            starting position in the stored AlgART array (not sub-array).
     * @param srcArray       the source packed bit array.
     * @param srcArrayOffset starting position in the source packed bit array.
     * @param count          the number of bits to be copied.
     * @see PackedBitArrays
     */
    void setBits(long pos, long[] srcArray, long srcArrayOffset, long count);

    /**
     * Replaces <tt>count</tt> bits in the specified packed bit array,
     * starting from <tt>destArrayOffset</tt> index,
     * with the logical AND of them and corresponding <tt>count</tt>
     * bits of this storage, starting from <tt>pos</tt> index.
     *
     * @param pos             starting position in the stored AlgART array (not sub-array).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    void andBits(long pos, long[] destArray, long destArrayOffset, long count);

    /**
     * Replaces <tt>count</tt> bits in the specified packed bit array,
     * starting from <tt>destArrayOffset</tt> index,
     * with the logical OR of them and corresponding <tt>count</tt>
     * bits of this storage, starting from <tt>pos</tt> index.
     *
     * @param pos             starting position in the stored AlgART array (not sub-array).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    void orBits(long pos, long[] destArray, long destArrayOffset, long count);

    /**
     * Replaces <tt>count</tt> bits in the specified packed bit array,
     * starting from <tt>destArrayOffset</tt> index,
     * with the logical XOR of them and corresponding <tt>count</tt>
     * bits of this storage, starting from <tt>pos</tt> index.
     *
     * @param pos             starting position in the stored AlgART array (not sub-array).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    void xorBits(long pos, long[] destArray, long destArrayOffset, long count);

    /**
     * Replaces <tt>count</tt> bits in the specified packed bit array,
     * starting from <tt>destArrayOffset</tt> index,
     * with the logical AND of them and <i>inverted</i> corresponding <tt>count</tt>
     * bits of this storage, starting from <tt>pos</tt> index.
     *
     * @param pos             starting position in the stored AlgART array (not sub-array).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    void andNotBits(long pos, long[] destArray, long destArrayOffset, long count);
}
