package net.algart.arrays;

/**
 * <p>Bit data storage: an addition to usual {@link DataStorage} allowing to read/write packed bits.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
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
