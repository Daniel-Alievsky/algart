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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * <p>Operations with bit arrays packed into <tt>byte[]</tt> Java arrays.</p>
 *
 * <p>This is a shorthand analog of {@link PackedBitArrays} class, using <tt>byte</tt> values
 * for packing bits instead of <tt>long</tt> values.
 * AlgART bit arrays do not use this class, but it can be useful while using external modules,
 * where bits are packed into bytes.
 * Note that for processing packed bit arrays, you
 * <b>should prefer {@link PackedBitArrays} class</b> for processing packed bit arrays
 * when possible: that class works significantly faster for the same number of packed bits.
 *
 * <p>The maximal length of bit arrays supported by this class is <tt>2<sup>34</sup>-8</tt>.
 * All indexes and lengths passed to methods of this class must not exceed this value.
 *
 * <p>In all methods of this class, it's supposed that the bit <tt>#k</tt> in a packed <tt>byte[] array</tt>
 * is the bit <tt>#(k%8)</tt> in the byte element <tt>array[k/8]</tt>. In other words, the bit <tt>#k</tt>
 * (<tt>false</tt> or <tt>true</tt> value) can be extracted by the following operator:</p>
 *
 * <pre>
 * (array[k &gt;&gt;&gt; 3] &amp; (1 &lt;&lt; (k &amp; 7))) != 0
 * </pre>
 *
 * <p>and can be set or cleared by the following operators:</p>
 *
 * <pre>
 * if (newValue) // we need to set bit #k to 1
 * &#32;   array[k &gt;&gt;&gt; 3] |= 1 &lt;&lt; (k &amp; 7);
 * else          // we need to clear bit #k to 0
 * &#32;   array[k &gt;&gt;&gt; 3] &amp;= ~(1 &lt;&lt; (k &amp; 7));
 * </pre>
 *
 * <p>You may use {@link #getBit(byte[], long)} and {@link #setBit(byte[], long, boolean)}, implementing
 * the equivalent code.</p>
 *
 * <p>If any method of this class modifies some portion of an element of a packed <tt>byte[]</tt> Java array,
 * i.e. modifies less than all 8 its bits, then all accesses to this <tt>byte</tt> element are performed
 * <b>inside a single synchronized block</b>, using the following instruction:</p>
 *
 * <pre>
 * synchronized (array) {
 * &#32;   // accessing to some element array[k]
 * }
 * </pre>
 *
 * <p>unless otherwise specified in the method comments. (See an example in comments to {@link #setBit} method.)
 * If all 8 bits of the element are written, or if the bits are read only, then no synchronization is performed.
 * Such behavior allows to simultaneously work with non-overlapping fragments of a packed bit array
 * from several threads (different fragments for different threads), as if it would be a usual Java array.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 * @see PackedBitArrays
 */
public class PackedBitArraysPer8 {
    private PackedBitArraysPer8() {
    }

    private static final byte[] REVERSE = {0x00, -0x80, 0x40, -0x40, 0x20, -0x60,
            0x60, -0x20, 0x10, -0x70, 0x50, -0x30, 0x30, -0x50, 0x70, -0x10, 0x08,
            -0x78, 0x48, -0x38, 0x28, -0x58, 0x68, -0x18, 0x18, -0x68, 0x58, -0x28,
            0x38, -0x48, 0x78, -0x08, 0x04, -0x7c, 0x44, -0x3c, 0x24, -0x5c, 0x64,
            -0x1c, 0x14, -0x6c, 0x54, -0x2c, 0x34, -0x4c, 0x74, -0x0c, 0x0c, -0x74,
            0x4c, -0x34, 0x2c, -0x54, 0x6c, -0x14, 0x1c, -0x64, 0x5c, -0x24, 0x3c,
            -0x44, 0x7c, -0x04, 0x02, -0x7e, 0x42, -0x3e, 0x22, -0x5e, 0x62, -0x1e,
            0x12, -0x6e, 0x52, -0x2e, 0x32, -0x4e, 0x72, -0x0e, 0x0a, -0x76, 0x4a,
            -0x36, 0x2a, -0x56, 0x6a, -0x16, 0x1a, -0x66, 0x5a, -0x26, 0x3a, -0x46,
            0x7a, -0x06, 0x06, -0x7a, 0x46, -0x3a, 0x26, -0x5a, 0x66, -0x1a, 0x16,
            -0x6a, 0x56, -0x2a, 0x36, -0x4a, 0x76, -0x0a, 0x0e, -0x72, 0x4e, -0x32,
            0x2e, -0x52, 0x6e, -0x12, 0x1e, -0x62, 0x5e, -0x22, 0x3e, -0x42, 0x7e,
            -0x02, 0x01, -0x7f, 0x41, -0x3f, 0x21, -0x5f, 0x61, -0x1f, 0x11, -0x6f,
            0x51, -0x2f, 0x31, -0x4f, 0x71, -0x0f, 0x09, -0x77, 0x49, -0x37, 0x29,
            -0x57, 0x69, -0x17, 0x19, -0x67, 0x59, -0x27, 0x39, -0x47, 0x79, -0x07,
            0x05, -0x7b, 0x45, -0x3b, 0x25, -0x5b, 0x65, -0x1b, 0x15, -0x6b, 0x55,
            -0x2b, 0x35, -0x4b, 0x75, -0x0b, 0x0d, -0x73, 0x4d, -0x33, 0x2d, -0x53,
            0x6d, -0x13, 0x1d, -0x63, 0x5d, -0x23, 0x3d, -0x43, 0x7d, -0x03, 0x03,
            -0x7d, 0x43, -0x3d, 0x23, -0x5d, 0x63, -0x1d, 0x13, -0x6d, 0x53, -0x2d,
            0x33, -0x4d, 0x73, -0x0d, 0x0b, -0x75, 0x4b, -0x35, 0x2b, -0x55, 0x6b,
            -0x15, 0x1b, -0x65, 0x5b, -0x25, 0x3b, -0x45, 0x7b, -0x05, 0x07, -0x79,
            0x47, -0x39, 0x27, -0x59, 0x67, -0x19, 0x17, -0x69, 0x57, -0x29, 0x37,
            -0x49, 0x77, -0x09, 0x0f, -0x71, 0x4f, -0x31, 0x2f, -0x51, 0x6f, -0x11,
            0x1f, -0x61, 0x5f, -0x21, 0x3f, -0x41, 0x7f, -0x01};


    /**
     * Returns <tt>((long) array.length) &lt;&lt; 3</tt>: the maximal number of bits that
     * can be stored in the specified array.
     *
     * @param array <tt>byte[]</tt> array.
     * @return <tt>8 * (long) array.length</tt>
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public static long unpackedLength(byte[] array) {
        return ((long) array.length) << 3;
    }

    /**
     * Returns <tt>(unpackedLength + 7) &gt;&gt;&gt; 3</tt>: the minimal number of <tt>byte</tt> values
     * allowing to store <tt>unpackedLength</tt> bits.
     *
     * @param unpackedLength the number of bits (the length of bit array).
     * @return <tt>(unpackedLength + 7) &gt;&gt;&gt; 3</tt> (the length of corresponding <tt>byte[]</tt> array).
     */
    public static long packedLength(long unpackedLength) {
        return (unpackedLength + 7) >>> 3;
        // here >>> must be used instead of >>, because unpackedLength+7 may be >Long.MAX_VALUE
    }

    /**
     * Returns the bit <tt>#index</tt> in the packed <tt>src</tt> bit array.
     * Equivalent to the following expression:<pre>
     * (src[(int)(index &gt;&gt;&gt; 3)] &amp; (1 &lt;&lt; (index &amp; 7))) != 0;
     * </pre>
     *
     * @param src   the source array (bits are packed in <tt>byte</tt> values).
     * @param index index of the returned bit.
     * @return the bit at the specified index.
     * @throws NullPointerException      if <tt>src</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static boolean getBit(byte[] src, long index) {
        return (src[(int) (index >>> 3)] & (1 << ((int) index & 7))) != 0;
    }

    /**
     * Sets the bit <tt>#index</tt> in the packed <tt>dest</tt> bit array.
     * Equivalent to the following operators:<pre>
     * synchronized (dest) {
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] |= 1 &lt;&lt; (index &amp; 7);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] &amp;= ~(1 &lt;&lt; (index &amp; 7));
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <tt>long</tt> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void setBit(byte[] dest, long index, boolean value) {
        synchronized (dest) {
            if (value)
                dest[(int) (index >>> 3)] |= (byte) (1 << ((int) index & 7));
            else
                dest[(int) (index >>> 3)] &= (byte) ~(1 << ((int) index & 7));
        }
    }

    /**
     * Sets the bit <tt>#index</tt> in the packed <tt>dest</tt> bit array <i>without synchronization</i>.
     * May be used instead of {@link #setBit(byte[], long, boolean)}, if you are not planning to call
     * this method from different threads for the same <tt>dest</tt> array.
     * Equivalent to the following operators:<pre>
     * synchronized (dest) {
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] |= 1 &lt;&lt; (index &amp; 7);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] &amp;= ~(1 &lt;&lt; (index &amp; 7));
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <tt>long</tt> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static void setBitNoSync(byte[] dest, long index, boolean value) {
        if (value)
            dest[(int) (index >>> 3)] |= (byte) (1 << ((int) index & 7));
        else
            dest[(int) (index >>> 3)] &= (byte) ~(1 << ((int) index & 7));
    }

    /**
     * Returns the sequence of <tt>count</tt> bits (maximum 64 bits), starting from the bit <tt>#srcPos</tt>,
     * in the packed <tt>src</tt> bit array.
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
     *          final long bit = {@link #getBit(byte[], long) PackedBitArraysPer8.getBit}(src, srcPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; k;
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <tt>count</tt> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the array (<tt>&ge;8*src.length</tt>); instead, all bits outside the array are considered zero.
     * (But negative indexes are not allowed.)</p>
     *
     * @param src    the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of bits to be unpacked (must be &gt;=0 and &lt;64).
     * @return the sequence of <tt>count</tt> bits.
     * @throws NullPointerException      if <tt>src</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>srcPos &lt; 0</tt>.
     * @throws IllegalArgumentException  if <tt>count &lt; 0</tt> or <tt>count &gt; 64</tt>.
     */
    public static long getBits(byte[] src, long srcPos, int count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IndexOutOfBoundsException("Negative srcPos argument: " + srcPos);
        }
        final long srcPosDiv8 = srcPos >>> 3;
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot get > 64 bits in getBits method");
        }
        if (count == 0 || srcPosDiv8 >= src.length) {
            return 0;
        }
        long result = 0;
        int sPosRem = (int) (srcPos & 7);
        int sPos = (int) srcPosDiv8;
        int shift = 0;
        for (; ; ) {
            final int bitsLeft = 8 - sPosRem;
            if (count > bitsLeft) {
                final long actualBits = (src[sPos] & 0xFFL) >>> sPosRem;
                // sPosRem=5, bitsLeft=3:
                //     (01234567)
                //      abcdefgh
                //      fgh00000
                result |= actualBits << shift;
                count -= bitsLeft;
                shift += bitsLeft;
                sPos++;
                if (sPos >= src.length) {
                    break;
                }
                sPosRem = 0;
            } else {
                final long actualBits = (src[sPos] & (0xFFL >>> (bitsLeft - count))) >>> sPosRem;
                // sPosRem=5, bitsLeft=3, count=2:
                //     (01234567)
                //      abcdefgh
                //      11111110    0xFFL >>> (bitsLeft - count)
                //      abcdefg0
                //      fg000000
                result |= actualBits << shift;
                break;
            }
        }
        return result;
    }

    /**
     * Returns the bit <tt>#index</tt> in the packed <tt>src</tt> bit array
     * for a case, when the bits are packed in each byte in the reverse order:
     * highest bit first, lowest bit last.
     * Equivalent to the following expression:<pre>
     * (src[(int)(index &gt;&gt;&gt; 3)] &amp; (1 &lt;&lt; (7 - (index &amp; 7)))) != 0;
     * </pre>
     *
     * <p>This bit order is used, for example, in TIFF format when storing binary images or
     * image with less than 8 bits per channel.</p>
     *
     * @param src   the source array (bits are packed in <tt>byte</tt> values in reverse order 76543210).
     * @param index index of the returned bit.
     * @return the bit at the specified index.
     * @throws NullPointerException      if <tt>src</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     * @see #reverseBitsOrderInEachByte(byte[], int, int)
     */
    public static boolean getBitInReverseOrder(byte[] src, long index) {
        return (src[(int) (index >>> 3)] & (1 << (7 - ((int) index & 7)))) != 0;
    }

    /**
     * Sets the bit <tt>#index</tt> in the packed <tt>dest</tt> bit array
     * for a case, when the bits are packed in each byte in the reverse order:
     * highest bit first, lowest bit last.
     * Equivalent to the following operators:<pre>
     * synchronized (dest) {
     * &#32;   final int bitIndex = 7 - ((int) index &amp; 7);
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] |= (1 &lt;&lt; bitIndex);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] &amp;= ~(1 &lt;&lt; bitIndex);
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <tt>long</tt> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     * @see #reverseBitsOrderInEachByte(byte[], int, int)
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void setBitInReverseOrder(byte[] dest, long index, boolean value) {
        synchronized (dest) {
            final int bitIndex = 7 - ((int) index & 7);
            if (value)
                dest[(int) (index >>> 3)] |= (byte) (1 << bitIndex);
            else
                dest[(int) (index >>> 3)] &= (byte) ~(1 << bitIndex);
        }
    }

    /**
     * Returns the sequence of <tt>count</tt> bits (maximum 64 bits), starting from the bit <tt>#srcPos</tt>,
     * in the packed <tt>src</tt> bit array for a case, when the bits are packed in each byte in the reverse order:
     * highest bit first, lowest bit last.
     *
     * <p>More precisely, the bit <tt>#(srcPos+k)</tt>, that can be read by the call
     * <tt>{@link #getBitInReverseOrder(byte[] src, long index)
     * getBitInReverseOrder}(src, srcPos+k)</tt>,
     * will be returned in the bit <tt>#(count-1-k)</tt> (in direct order) of the returned
     * <tt>long</tt> value <tt>R</tt>, i.e. it is equal to <tt><tt>(R&gt;&gt;(count-1-k))&amp;1</tt></tt>.
     * If <tt>count=0</tt>, the result is 0.</p>
     *
     * <p>The same result can be calculated using the following loop:</p>
     *
     * <pre>
     *      long result = 0;
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = {@link #getBitInReverseOrder(byte[], long)
     *              PackedBitArraysPer8.getBitInReverseOrder}(src, srcPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; (count - 1 - k);
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <tt>count</tt> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the array (<tt>&ge;8*src.length</tt>); instead, all bits outside the array are considered zero.
     * (But negative indexes are not allowed.)</p>
     *
     * <p>This bit order is used, for example, in TIFF format when storing binary images or
     * image with less than 8 bits per channel.</p>
     *
     * @param src    the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of bits to be unpacked (must be &gt;=0 and &lt;64).
     * @return the sequence of <tt>count</tt> bits.
     * @throws NullPointerException      if <tt>src</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>srcPos &lt; 0</tt>.
     * @throws IllegalArgumentException  if <tt>count &lt; 0</tt> or <tt>count &gt; 64</tt>.
     */
    public static long getBitsInReverseOrder(byte[] src, long srcPos, int count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IndexOutOfBoundsException("Negative srcPos argument: " + srcPos);
        }
        final long srcPosDiv8 = srcPos >>> 3;
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count == 0 || srcPosDiv8 >= src.length) {
            return 0;
        }
        long result = 0;
        int sPosRem = (int) (srcPos & 7);
        int sPos = (int) srcPosDiv8;
        for (; ; ) {
            final int bitsLeft = 8 - sPosRem;
            if (count > bitsLeft) {
                final long actualBits = (long) src[sPos] & (0xFFL >>> sPosRem);
                // sPosRem=5, bitsLeft=3:
                //     (76543210)
                //      abcdefgh
                //      00000fgh
                result = (result << bitsLeft) | actualBits;
                count -= bitsLeft;
                sPos++;
                sPosRem = 0;
            } else {
                final long actualBits = (long) src[sPos] & ~(0xFF00 >>> sPosRem) & 0xFF;
                // sPosRem=5, bitsLeft=3, count=2:
                //     (76543210)
                //      abcdefgh
                //      11111000    0xFF00 >>> sPosRem
                //      00000111    ~(0xFF00 >>> sPosRem)
                //      00000fgh
                result = (result << count) | (actualBits >> (bitsLeft - count));
                //      000000fg
                break;
            }
            if (sPos >= src.length) {
                return result << count;
            }
        }
        return result;
    }

    /**
     * Packs byte array to <tt>long[]</tt>, so that the bits, stored in the result array according the rules
     * of {@link PackedBitArrays} class, will be identical to bits stored in the source array according
     * the rules of this class.
     *
     * <p>The length of created array will be <tt>(byteArray.length + 7) / 8</tt>.
     * The bytes of the returned <tt>long</tt> values are just the bytes of the source array,
     * packed in little-endian order.
     * If <tt>byteArray.length</tt> is not divisible by 8, the unused high bytes of the last <tt>long</tt>
     * element will be zero.</p>
     *
     * @param byteArray byte array, supposedly storing packed bits according the rules of this class.
     * @return <tt>long</tt> array, storing the same packed bits according the rules of {@link PackedBitArrays}.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public static long[] toLongArray(byte[] byteArray) {
        Objects.requireNonNull(byteArray, "Null byte[] array");
        final long[] result = new long[(byteArray.length + 7) >>> 3];
        final int numberOfWholeLongs = byteArray.length >>> 3;
        final int alignedLength = numberOfWholeLongs << 3;
        final ByteBuffer bb = ByteBuffer.wrap(byteArray);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.limit(alignedLength);
        bb.asLongBuffer().get(result, 0, numberOfWholeLongs);
        if (byteArray.length > alignedLength) {
            assert result.length == numberOfWholeLongs + 1;
            long v = 0;
            for (int k = alignedLength, shift = 0; k < byteArray.length; k++, shift += 8) {
                v |= ((long) byteArray[k] & 0xFF) << shift;
            }
            result[numberOfWholeLongs] = v;
        }
        return result;
    }

    /**
     * Exact analog of {@link #toLongArray(byte[])} method, but the original bytes are stored in <tt>ByteBuffer</tt>
     * instead of <tt>byte[]</tt> array. If <tt>b</tt> is some <tt>byte[]</tt> array, then the following calls
     * are equivalent:
     * <pre>
     *     {@link #toLongArray(ByteBuffer) toLongArray}(ByteBuffer.wrap(b))
     *     {@link #toLongArray(byte[]) toLongArray}(b)
     * </pre>
     *
     * <p>This method works with a duplicate of the specified <tt>ByteBuffer</tt> and, so, does not modify
     * its settings like position and limit. Note that the byte order in the passes <tt>ByteBuffer</tt> is ignored:
     * the bytes are always packed into <tt>long</tt> values in little-endian order.</p>
     *
     * @param byteBuffer bytes, supposedly storing packed bits according the rules of this class.
     * @return <tt>long</tt> array, storing the same packed bits according the rules of {@link PackedBitArrays}.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    @SuppressWarnings("JavadocDeclaration")
    public static long[] toLongArray(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null ByteBuffer");
        ByteBuffer bb = byteBuffer.duplicate();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.rewind();
        final int numberOfBytes = bb.limit();
        final long[] result = new long[(numberOfBytes + 7) >>> 3];
        final int numberOfWholeLongs = numberOfBytes >>> 3;
        final int alignedLength = numberOfWholeLongs << 3;
        bb.limit(alignedLength);
        bb.asLongBuffer().get(result, 0, numberOfWholeLongs);
        bb.limit(numberOfBytes);
        if (numberOfBytes > alignedLength) {
            assert result.length == numberOfWholeLongs + 1;
            long v = 0;
            for (int k = alignedLength, shift = 0; k < numberOfBytes; k++, shift += 8) {
                v |= ((long) (bb.get(k) & 0xFF)) << shift;
            }
            result[numberOfWholeLongs] = v;
        }
        return result;
    }

    /**
     * Unpacks <tt>long[]</tt> array to <tt>byte[]</tt>, so that the bits, stored in the result array according
     * the rules of this class, will be identical to bits stored in the source array according
     * the rules of {@link PackedBitArrays}. The actual length of the passed bit array should be specified
     * in <tt>packedBitArrayLength</tt> argument.
     *
     * <p>The length of created array will be <tt>{@link #packedLength
     * PackedBitArraysPer8.packedLength}(packedBitArrayLength)</tt>. The bytes of the returned array
     * are just the bytes of the source <tt>long</tt> values, packed in little-endian order.
     *
     * @param packedBitArray       <tt>long</tt>> array, supposedly storing packed bits according the rules
     *                             of {@link PackedBitArrays} class.
     * @param packedBitArrayLength the number of packed bits.
     * @return <tt>byte[]</tt> array, storing the same packed bits according the rules of this class.
     * @throws IllegalArgumentException if <tt>packedBitArrayLength</tt> is negative or too large:
     *                                  greater than <tt>packedBitArray.length*64</tt> or <tt>2^34&minus;1</tt>
     *                                  (in the latter case, the required length of the returned array
     *                                  exceeds Java limit 2^31).
     */
    public static byte[] toByteArray(long[] packedBitArray, long packedBitArrayLength) {
        Objects.requireNonNull(packedBitArray, "Null long[] array (packed bits)");
        if (packedBitArrayLength < 0) {
            throw new IllegalArgumentException("Negative length");
        }
        final long numberOfLongs = PackedBitArrays.packedLength(packedBitArrayLength);
        if (numberOfLongs > packedBitArray.length) {
            throw new IllegalArgumentException("Too short long[" + packedBitArray.length +
                    "] array (packed bits): it must contain at least " + numberOfLongs +
                    " long elements to store " + packedBitArrayLength + " bits");
        }
        final long numberOfBytes = packedLength(packedBitArrayLength);
        if (numberOfBytes > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large number of bits " + packedBitArrayLength +
                    " > 2^34-1: cannot pack such a large bit array in byte[] array");
        }
        final byte[] result = new byte[(int) numberOfBytes];
        final int numberOfWholeLongs = result.length >>> 3;
        final int alignedLength = numberOfWholeLongs << 3;
        final ByteBuffer bb = ByteBuffer.wrap(result);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.asLongBuffer().put(packedBitArray, 0, numberOfWholeLongs);
        if (result.length > alignedLength) {
            assert numberOfLongs == numberOfWholeLongs + 1;
            long v = packedBitArray[numberOfWholeLongs];
            for (int k = alignedLength, shift = 0; k < result.length; k++, shift += 8) {
                result[k] = (byte) (v >>> shift);
            }
        }
        return result;
    }

    // Note: in the following regexp, we must replace src[...] ==> src[...] & 0xFF,
    // because we sometimes SHIFT this byte and, so, should work with low 8 bits;
    // but we may stay dest[...] without "& 0xFF": we use only lowest 8 bits from this value.
    /*Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, copyBits)
        <tt>long<\/tt> ==> <tt>byte</tt> ;;
        long\[\] ==> byte[] ;;
        >>>\s*6 ==> >>> 3 ;;
        63\b ==> 7 ;;
        64\b ==> 8 ;;
        long\s+(maskStart|maskFinish|v|sPrev|sNext)\b ==> int $1 ;;
        1L\b ==> 1 ;;
        src(\[[^\]]+\]) ==> (src$1 & 0xFF) ;;
        dest(\[[^\]]+\])\s+=(\s+)([^;]+); ==> dest$1 =$2(byte) ($3);
       !! Auto-generated: NOT EDIT !! */

    /**
     * Copies <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     *
     * <p><i>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap</i>,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * bits at positions <tt>srcPos..srcPos+count-1</tt>
     * were first unpacked to a temporary <tt>boolean[]</tt> array with <tt>count</tt> elements
     * and then the contents of the temporary array were packed into positions
     * <tt>destPos..destPos+count-1</code>.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void copyBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        //Start_reverseOrder !! this comment is necessary for preprocessing by Repeater !!
        if (src == dest && srcPos <= destPos && srcPos + count > destPos)
        //End_reverseOrder !! this comment is necessary for preprocessing by Repeater !!
        {
            // overlap possible
            if (sPosRem == dPosRem) {
                //Start_nothingToDo !! this comment is necessary for preprocessing by Repeater !!
                if (sPos == dPos) {
                    return; // nothing to do
                }
                //End_nothingToDo !! this comment is necessary for preprocessing by Repeater !!
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    count -= cntStart;
                    dPos++;
                    sPos++;
                }
                int cnt = (int) (count >>> 3);
                int cntFinish = (int) (count & 7);
                if (cntFinish > 0) {
                    int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                    synchronized (dest) {
                        dest[dPos + cnt] =
                                (byte) (((src[sPos + cnt] & 0xFF) & maskFinish) |
                                        (dest[dPos + cnt] & ~maskFinish));
                    }
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPosStart] =
                                (byte) (((src[sPosStart] & 0xFF) & maskStart) |
                                        (dest[dPosStart] & ~maskStart));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
                final long dPosMin;
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                        sPosRem += cntStart;
                    } else {
                        sPos++;
                        sPosRem = (sPosRem + cntStart) & 7;
                    }
                    // we suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                    count -= cntStart;
                    dPos++;
                }
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
                int cnt = (int) (count >>> 3);
                dPosMin = dPos;
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 7);
                final int sPosRem8 = 8 - sPosRem;
                int sPrev;
                if (cntFinish > 0) {
                    int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = (sPrev = (src[sPos] & 0xFF)) >>> sPosRem;
                    } else {
                        v = ((sPrev = (src[sPos] & 0xFF)) >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                    }
                    synchronized (dest) {
                        dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                    }
                } else {
                    //Start_sPrev !! this comment is necessary for preprocessing by Repeater !!
                    sPrev = (src[sPos] & 0xFF);
                    // IndexOutOfBoundException is impossible here, because there is one of the following situations:
                    // 1) cnt > 0, then (src[sPos] & 0xFF) is really necessary in the following loop;
                    // 2) cnt == 0 and cntStart > 0, then (src[sPos] & 0xFF) will be necessary for making dest[dPosStart].
                    // All other situations are impossible here:
                    // 3) cntFinish > 0: it was processed above in "if (cntFinish > 0)..." branch;
                    // 4) cntStart == 0, cntFinish == 0 and cnt == 0, i.e. count == 0: it's impossible
                    // in this branch of all algorithm (overlap is impossible when count == 0).
                    //End_sPrev !! this comment is necessary for preprocessing by Repeater !!
                }
                while (dPos > dPosMin) { // cnt times
                    --sPos;
                    --dPos;
                    dest[dPos] = (byte) ((sPrev << sPosRem8) | ((sPrev = (src[sPos] & 0xFF)) >>> sPosRem));
                }
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    int v;
                    if (sPosRemStart + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (src[sPosStart] & 0xFF) << shift;
                        else
                            v = (src[sPosStart] & 0xFF) >>> -shift;
                    } else {
                        v = ((src[sPosStart] & 0xFF) >>> -shift) | ((src[sPosStart + 1] & 0xFF) << (8 + shift));
                    }
                    synchronized (dest) {
                        dest[dPosStart] = (byte) ((v & maskStart) | (dest[dPosStart] & ~maskStart));
                    }
                }
            }
        } else {
            // usual case
            if (sPosRem == dPosRem) {
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPos] = (byte) (((src[sPos] & 0xFF) & maskStart) | (dest[dPos] & ~maskStart));
                    }
                    count -= cntStart;
                    dPos++;
                    sPos++;
                }
                int cnt = (int) (count >>> 3);
                System.arraycopy(src, sPos, dest, dPos, cnt);
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 7);
                if (cntFinish > 0) {
                    int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                    synchronized (dest) {
                        dest[dPos] = (byte) (((src[sPos] & 0xFF) & maskFinish) | (dest[dPos] & ~maskFinish));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                int sNext;
                if (cntStart > 0) {
                    int v;
                    if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (sNext = (src[sPos] & 0xFF)) << shift;
                        else
                            v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                        sPosRem += cntStart;
                    } else {
                        v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                        sPos++;
                        sPosRem = (sPosRem + cntStart) & 7;
                    }
                    // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                    synchronized (dest) {
                        dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
                    }
                    count -= cntStart;
                    if (count == 0) {
                        return; // little optimization
                    }
                    dPos++;
                } else {
                    if (count == 0) {
                        return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                    }
                    sNext = (src[sPos] & 0xFF);
                }
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
                final int sPosRem8 = 8 - sPosRem;
                for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                    sPos++;
                    dest[dPos] = (byte) ((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8));
                    dPos++;
                }
                int cntFinish = (int) (count & 7);
                if (cntFinish > 0) {
                    int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = sNext >>> sPosRem;
                    } else {
                        v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                    }
                    synchronized (dest) {

                        dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                    }
                }
            }
        }

    }
    /*Repeat.IncludeEnd*/

    /**
     * Copies <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     *
     * <p><i>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap</i>,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * bits at positions <tt>srcPos..srcPos+count-1</tt>
     * were first unpacked to a temporary <tt>boolean[]</tt> array with <tt>count</tt> elements
     * and then the contents of the temporary array were packed into positions
     * <tt>destPos..destPos+count-1</code>.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void copyBitsFromReverseOrder(byte[] dest, long destPos, byte[] src, long srcPos, long count) {
        // Note: the following method IS NOT BUILT by Repeater; in a case of any improbable changes,
        // this must be re-written manually!
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        // Note: overlapping IS NOT supported!
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (byte) (((src[sPos] & 0xFF) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            int cnt = (int) (count >>> 3);
            System.arraycopy(src, sPos, dest, dPos, cnt);
            sPos += cnt;
            dPos += cnt;
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (byte) (((src[sPos] & 0xFF) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = (byte) ((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }
    }

    /**
     * Copies <tt>count</tt> bits from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (unpacked <tt>boolean</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void packBits(byte[] dest, long destPos, boolean[] src, int srcPos, int count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (destPos & 7) == 0 ? 0 : 8 - (int) (destPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest[(int) (destPos >>> 3)] |= (byte) (1 << (destPos & 7));
                    else
                        dest[(int) (destPos >>> 3)] &= (byte) (~(1 << (destPos & 7)));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (destPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            dest[k] = (byte) ((src[srcPos] ? 1 : 0)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 1 << $INDEX(start=2) : 0) ,, ...(6)]]
                    | (src[srcPos + 1] ? 1 << 1 : 0)
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    | (src[srcPos + 2] ? 1 << 2 : 0)
                    | (src[srcPos + 3] ? 1 << 3 : 0)
                    | (src[srcPos + 4] ? 1 << 4 : 0)
                    | (src[srcPos + 5] ? 1 << 5 : 0)
                    | (src[srcPos + 6] ? 1 << 6 : 0)
                    | (src[srcPos + 7] ? 1 << 7 : 0)
//[[Repeat.AutoGeneratedEnd]]
            );
            srcPos += 8;
            destPos += 8;
        }
        int countFinish = count & 7;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest[(int) (destPos >>> 3)] |= (byte) (1 << (destPos & 7));
                    else
                        dest[(int) (destPos >>> 3)] &= (byte) (~(1 << (destPos & 7)));
                }
            }
        }
    }

    /**
     * Copies <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> boolean array, starting from the element <tt>#destPos</tt>.
     *
     * @param dest    the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(boolean[] dest, int destPos, byte[] src, long srcPos, int count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
//[[Repeat() dest\[.*?0; ==>
//           dest[destPos + $INDEX(start=1)] = (v & (1 << $INDEX(start=1))) != 0; ,, ...(7)]]
            dest[destPos] = (v & 1) != 0;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 1] = (v & (1 << 1)) != 0;
            dest[destPos + 2] = (v & (1 << 2)) != 0;
            dest[destPos + 3] = (v & (1 << 3)) != 0;
            dest[destPos + 4] = (v & (1 << 4)) != 0;
            dest[destPos + 5] = (v & (1 << 5)) != 0;
            dest[destPos + 6] = (v & (1 << 6)) != 0;
            dest[destPos + 7] = (v & (1 << 7)) != 0;
//[[Repeat.AutoGeneratedEnd]]
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0;
        }
    }

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double,,Object */

    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> boolean array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            boolean[] dest, int destPos, byte[] src, long srcPos, int count,
            boolean bit0Value, boolean bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
//[[Repeat() dest\[.*?0\s*\? ==>
//           dest[destPos + $INDEX(start=1)] = (v & (1 << $INDEX(start=1))) != 0 ?,, ...(7)]]
            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
//[[Repeat.AutoGeneratedEnd]]
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> char array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>char</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            char[] dest, int destPos, byte[] src, long srcPos, int count,
            char bit0Value, char bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> byte array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>byte</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            byte[] dest, int destPos, byte[] src, long srcPos, int count,
            byte bit0Value, byte bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> short array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>short</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            short[] dest, int destPos, byte[] src, long srcPos, int count,
            short bit0Value, short bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> int array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>int</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            int[] dest, int destPos, byte[] src, long srcPos, int count,
            int bit0Value, int bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> long array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>long</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            long[] dest, int destPos, byte[] src, long srcPos, int count,
            long bit0Value, long bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> float array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>float</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            float[] dest, int destPos, byte[] src, long srcPos, int count,
            float bit0Value, float bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> double array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>double</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            double[] dest, int destPos, byte[] src, long srcPos, int count,
            double bit0Value, double bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> Object array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>Object</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            Object[] dest, int destPos, byte[] src, long srcPos, int count,
            Object bit0Value, Object bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;

            dest[destPos] = (v & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;

            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (srcPos & 7))) != 0 ? bit1Value : bit0Value;
        }
    }
    /*Repeat.AutoGeneratedEnd*/


    /**
     * Fills <tt>count</tt> bits in the packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<tt>false</tt> means the bit 0, <tt>true</tt> means the bit 1).
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if filling would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void fillBits(byte[] dest, long destPos, long count, boolean value) {
        Objects.requireNonNull(dest, "Null dest");
        int dPos = (int) (destPos >>> 3);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (cntStart > 0) {
            synchronized (dest) {
                if (value)
                    dest[dPos] |= (byte) maskStart;
                else
                    dest[dPos] &= (byte) ~maskStart;
            }
            count -= cntStart;
            dPos++;
        }
        byte byteValue = value ? (byte) -1 : 0;
        for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++) {
            dest[dPos] = byteValue;
        }
        int cntFinish = (int) (count & 7);
        if (cntFinish > 0) {
            int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
            synchronized (dest) {
                if (value)
                    dest[dPos] |= (byte) maskFinish;
                else
                    dest[dPos] &= (byte) ~maskFinish;
            }
        }
    }

    /*Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, logicalOperations)
        long\[\] ==> byte[] ;;
        <tt>long<\/tt> ==> <tt>byte</tt> ;;
        >>>\s*6 ==> >>> 3 ;;
        63 ==> 7 ;;
        64 ==> 8 ;;
        (dest\[\w+\]\s*(?:\&=|\|=|\^=|=)\s*)([^;]+); ==> $1(byte) ($2); ;;
        src\[(sPos|sPos \+ 1)\] ==> (src[$1] & 0xFF) ;;
        long\s+(v\b|sNext\b|mask) ==> int $1 ;;
        1L\s+<< ==> 1 <<
        !! Auto-generated: NOT EDIT !! */

    /**
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical NOT of corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>.
     *
     * <p>This method works correctly even if <tt>src&nbsp;==&nbsp;dest</tt>
     * and <tt>srcPos&nbsp;==&nbsp;destPos</tt>:
     * in this case it just inverts the specified bits.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void notBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (byte) ((~(src[sPos] & 0xFF) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = (byte) (~(src[sPos] & 0xFF));
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (byte) ((~(src[sPos] & 0xFF) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = (byte) ((~v & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = (byte) (~((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8)));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) ((~v & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }

    }

    /**
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical AND of them and corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>.
     *
     * <p>This method works correctly even if <tt>src&nbsp;==&nbsp;dest</tt>
     * and <tt>srcPos&nbsp;==&nbsp;destPos</tt>:
     * in this case it does nothing (so there are no reasons for this call).
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void andBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] &= (byte) ((src[sPos] & 0xFF) | ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] &= (byte) ((src[sPos] & 0xFF));
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] &= (byte) ((src[sPos] & 0xFF) | ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] &= (byte) (v | ~maskStart);
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] &= (byte) ((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] &= (byte) (v | ~maskFinish);
                }
            }
        }

    }

    /**
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical OR of them and corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>.
     *
     * <p>This method works correctly even if <tt>src&nbsp;==&nbsp;dest</tt>
     * and <tt>srcPos&nbsp;==&nbsp;destPos</tt>:
     * in this case it does nothing (so there are no reasons for this call).
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void orBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
//      System.out.println((sPosRem == dPosRem ? "AORGOOD " : "AORBAD  ") + sPosRem + "," + dPosRem);
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] |= (byte) ((src[sPos] & 0xFF) & maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] |= (byte) ((src[sPos] & 0xFF));
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] |= (byte) ((src[sPos] & 0xFF) & maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] |= (byte) (v & maskStart);
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] |= (byte) ((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] |= (byte) (v & maskFinish);
                }
            }
        }

    }

    /**
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical XOR of them and corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>.
     *
     * <p>This method works correctly even if <tt>src&nbsp;==&nbsp;dest</tt>
     * and <tt>srcPos&nbsp;==&nbsp;destPos</tt>:
     * in this case it clears all specified bits.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void xorBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] ^ (src[sPos] & 0xFF)) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] ^= (byte) ((src[sPos] & 0xFF));
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] ^ (src[sPos] & 0xFF)) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] ^ v) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] ^= (byte) ((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] ^ v) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }

    }

    /**
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical AND of them and <i>inverted</i> corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>.
     *
     * <p>This method works correctly even if <tt>src&nbsp;==&nbsp;dest</tt>
     * and <tt>srcPos&nbsp;==&nbsp;destPos</tt>:
     * in this case it clears all specified bits.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void andNotBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] & ~(src[sPos] & 0xFF)) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] &= (byte) (~(src[sPos] & 0xFF));
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] & ~(src[sPos] & 0xFF)) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] & ~v) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] &= (byte) (~((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8)));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] & ~v) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }

    }

    /**
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical OR of them and <i>inverted</i> corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>.
     *
     * <p>This method works correctly even if <tt>src&nbsp;==&nbsp;dest</tt>
     * and <tt>srcPos&nbsp;==&nbsp;destPos</tt>:
     * in this case it sets all specified bits to 1.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void orNotBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
//      System.out.println((sPosRem == dPosRem ? "AORGOOD " : "AORBAD  ") + sPosRem + "," + dPosRem);
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] | ~(src[sPos] & 0xFF)) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] |= (byte) (~(src[sPos] & 0xFF));
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] | ~(src[sPos] & 0xFF)) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (src[sPos] & 0xFF)) << shift;
                    else
                        v = (sNext = (src[sPos] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((src[sPos] & 0xFF) >>> -shift) | ((sNext = (src[sPos + 1] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] | ~v) & maskStart) | (dest[dPos] & ~maskStart));
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing (src[sPos] & 0xFF)
                }
                sNext = (src[sPos] & 0xFF);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of (src[sPos] & 0xFF)
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] |= (byte) (~((sNext >>> sPosRem) | ((sNext = (src[sPos] & 0xFF)) << sPosRem8)));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((src[sPos + 1] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) (((dest[dPos] | ~v) & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }

    }
    /*Repeat.IncludeEnd*/

    /**
     * Returns the number of high bits (1) in the given fragment of the given packed bit array.
     *
     * @param src       the source packed bit array.
     * @param fromIndex the initial checked bit index in <tt>array</tt>, inclusive.
     * @param toIndex   the end checked bit index in <tt>array</tt>, exclusive.
     * @return the number of high bits (1) in the given fragment of the given packed bit array.
     * @throws NullPointerException      if the <tt>src</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> or <tt>toIndex</tt> are negative,
     *                                   if <tt>toIndex</tt> is greater than <tt>src.length*8</tt>,
     *                                   or if <tt>fromIndex</tt> is greater than <tt>startIndex</tt>
     */
    public static long cardinality(byte[] src, final long fromIndex, final long toIndex) {
        Objects.requireNonNull(src, "Null src argument in cardinality method");
        if (fromIndex < 0)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex);
        if (toIndex > ((long) src.length) << 3)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: end index = " + toIndex);
        if (fromIndex > toIndex)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex
                    + " > end index = " + toIndex);
        long count = toIndex - fromIndex;

        int sPos = (int) (fromIndex >>> 3);
        int sPosRem = (int) (fromIndex & 7);
        int cntStart = (-sPosRem) & 7;
        int maskStart = (-1 << sPosRem) & 0xFF; // sPosRem times 0, then 1 (from the left)
        // - 0xFF is important here for correct usage of bitCount
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1 << (sPosRem + cntStart)) - 1; // &= sPosRem+cntStart times 1 (from the left)
        }
        long result = 0;
        if (cntStart > 0) {
            result += PackedBitArrays.bitCount(src[sPos] & maskStart);
            count -= cntStart;
            sPos++;
        }
        for (int sPosMax = sPos + (int) (count >>> 3); sPos < sPosMax; sPos++) {
            result += PackedBitArrays.bitCount(src[sPos] & 0xFF);
        }
        int cntFinish = (int) (count & 7);
        if (cntFinish > 0) {
            int maskFinish = ((1 << cntFinish) - 1) & 0xFF; // cntFinish times 1 (from the left)
            result += PackedBitArrays.bitCount(src[sPos] & maskFinish);
        }
        return result;
    }

    /**
     * Equivalent to <tt>{@link #reverseBitsOrderInEachByte(byte[], int, int)
     * reverseBitOrder}(bytes, 0, bytes.length)</tt>.
     *
     * @param bytes array to be processed.
     * @throws NullPointerException if <tt>bytes</tt> is <tt>null</tt>.
     */
    public static void reverseBitsOrderInEachByte(byte[] bytes) {
        Objects.requireNonNull(bytes, "Null bytes");
        reverseBitsOrderInEachByte(bytes, 0, bytes.length);
    }

    /**
     * Inverts bits order in all bytes in the specified array.
     * <p>Equivalent to the following loop:</p>
     * <pre>
     *     for (int i = 0; i &lt; count; i++) {
     *         bytes[pos + i] = (byte) ({@link Integer#reverse(int)
     *         Integer.reverse}(bytes[pos + i] &amp; 0xFF) >>> 24);
     * </pre>
     *
     * <p>This method can be useful if you have an array of bits, packed into bytes in reverse order:
     * <tt>(b>>7)&amp;1</tt>,
     * <tt>(b>>6)&amp;1</tt>,
     * <tt>(b>>5)&amp;1</tt>,
     * <tt>(b>>4)&amp;1</tt>,
     * <tt>(b>>3)&amp;1</tt>,
     * <tt>(b>>2)&amp;1</tt>,
     * <tt>(b>>1)&amp;1</tt>,
     * <tt>b&amp;1</tt>
     * (highest bits first) for each byte <tt>b</tt>. You should reverse the bit order in such an array
     * before using other methods of this class or, for simple cases, use the methods
     * {@link #getBitInReverseOrder(byte[], long)} and {@link #setBitInReverseOrder(byte[], long, boolean)}.</p>
     *
     * @param bytes array to be processed.
     * @throws NullPointerException      if <tt>bytes</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if processing would cause access of data outside the array.
     */
    public static void reverseBitsOrderInEachByte(byte[] bytes, int pos, int count) {
        Objects.requireNonNull(bytes, "Null bytes");
        JArrays.rangeCheck(bytes.length, pos, count);
        for (int i = pos, toIndex = pos + count; i < toIndex; i++) {
            bytes[i] = REVERSE[bytes[i] & 0xFF];
        }
    }
}
