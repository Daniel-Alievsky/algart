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
 * <p>Operations with bit arrays packed into <code>byte[]</code> Java arrays.</p>
 *
 * <p>This is an analog of {@link PackedBitArrays} class, using <code>byte</code> values
 * for packing bits instead of <code>long</code> values.
 * AlgART bit arrays do not use this class, but it can be useful while using external modules,
 * where bits are packed into bytes.
 * Also this class contains some features for processing bits, packed into bytes in the reverse order:
 * highest bit first, lowest bit last.</p>
 *
 * <p>Note that you
 * <b>should prefer</b> {@link PackedBitArrays} class for processing packed bit arrays
 * when possible: that class works significantly faster for the same number of packed bits.
 *
 * <p>The maximal length of bit arrays supported by this class is <code>2<sup>34</sup>-8</code>.
 * All indexes and lengths passed to methods of this class must not exceed this value.
 *
 * <p>In most methods of this class, it's supposed that the bit <code>#k</code> in a packed <code>byte[] array</code>
 * is the bit <code>#(k%8)</code> in the byte element <code>array[k/8]</code>. In other words, the bit <code>#k</code>
 * (<code>false</code> or <code>true</code> value) can be extracted by the following operator:</p>
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
 * <p>The order of bits within a byte, that corresponds to the above specification, is what we call
 * the <b>normal</b> order. Some methods of this class also support the <b>reverse</b> bits order:
 * see {@link #getBitInReverseOrder(byte[], long)} and {@link #setBitInReverseOrder(byte[], long, boolean)}
 * methods.</p>
 *
 * <p>If any method of this class modifies some portion of an element of a packed <code>byte[]</code> Java array,
 * i.e. modifies less than all 8 its bits, then all accesses to this <code>byte</code> element are performed
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
 * from several threads (different fragments for different threads), as if it would be a usual Java array.
 * However, some methods of this class <b>do not perform</b> such synchronization; the names of all such methods has
 * <code>...NoSync</code> postfix.</p>
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
     * Packs byte array to <code>long[]</code>, so that the bits, stored in the result array according the rules
     * of {@link PackedBitArrays} class, will be identical to bits stored in the source array according
     * the rules of this class.
     *
     * <p>The length of created array will be <code>(byteArray.length + 7) / 8</code>.
     * The bytes of the returned <code>long</code> values are just the bytes of the source array,
     * packed in little-endian order.
     * If <code>byteArray.length</code> is not divisible by 8, the unused high bytes of the last <code>long</code>
     * element will be zero.</p>
     *
     * @param byteArray byte array, supposedly storing packed bits according the rules of this class.
     * @return <code>long</code> array, storing the same packed bits according the rules of {@link PackedBitArrays}.
     * @throws NullPointerException if the argument is <code>null</code>.
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
     * Exact analog of {@link #toLongArray(byte[])} method, but the original bytes are stored in
     * <code>ByteBuffer</code>
     * instead of <code>byte[]</code> array. If <code>b</code> is some <code>byte[]</code> array,
     * then the following calls are equivalent:
     * <pre>
     *     {@link #toLongArray(ByteBuffer) toLongArray}(ByteBuffer.wrap(b))
     *     {@link #toLongArray(byte[]) toLongArray}(b)
     * </pre>
     *
     * <p>This method works with a duplicate of the specified <code>ByteBuffer</code> and, so, does not modify
     * its settings like position and limit. Note that the byte order in the passes <code>ByteBuffer</code> is ignored:
     * the bytes are always packed into <code>long</code> values in little-endian order.</p>
     *
     * @param byteBuffer bytes, supposedly storing packed bits according the rules of this class.
     * @return <code>long</code> array, storing the same packed bits according the rules of {@link PackedBitArrays}.
     * @throws NullPointerException if the argument is <code>null</code>.
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
     * Unpacks <code>long[]</code> array to <code>byte[]</code>, so that the bits, stored in the result array according
     * the rules of this class, will be identical to bits stored in the source array according
     * the rules of {@link PackedBitArrays}. The actual length of the passed bit array should be specified
     * in <code>packedBitArrayLength</code> argument.
     *
     * <p>The length of created array will be <code>{@link #packedLength
     * PackedBitArraysPer8.packedLength}(packedBitArrayLength)</code>. The bytes of the returned array
     * are just the bytes of the source <code>long</code> values, packed in little-endian order.
     *
     * @param packedBitArray       <code>long</code>> array, supposedly storing packed bits according the rules
     *                             of {@link PackedBitArrays} class.
     * @param packedBitArrayLength the number of packed bits.
     * @return <code>byte[]</code> array, storing the same packed bits according the rules of this class.
     * @throws IllegalArgumentException if <code>packedBitArrayLength</code> is negative or too large:
     *                                  greater than <code>packedBitArray.length*64</code> or <code>2^34&minus;1</code>
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

    /**
     * Returns <code>packedLength &lt;&lt; 3</code>: the maximal number of bits that
     * can be stored in the specified number of bytes.
     *
     * @param packedLength number of packed bytes.
     * @return <code>8 * packedLength</code>
     * @throws TooLargeArrayException   if the argument is too large: &ge; 2<sup>60</sup>.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public static long unpackedLength(long packedLength) {
        if (packedLength < 0) {
            throw new IllegalArgumentException("Negative packed length");
        }
        if (packedLength >= 1L << 60) {
            throw new TooLargeArrayException("Too large packed length: number of unpacked bits >= 2^60");
        }
        return packedLength << 3;
    }


    /**
     * Returns <code>((long) array.length) &lt;&lt; 3</code>: the maximal number of bits that
     * can be stored in the specified array.
     *
     * @param array <code>byte[]</code> array.
     * @return <code>8 * (long) array.length</code>
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    public static long unpackedLength(byte[] array) {
        return ((long) array.length) << 3;
    }

    /**
     * Returns <code>(unpackedLength + 7) &gt;&gt;&gt; 3</code>: the minimal number of <code>byte</code> values
     * allowing to store <code>unpackedLength</code> bits.
     *
     * @param unpackedLength the number of bits (the length of bit array).
     * @return <code>(unpackedLength + 7) &gt;&gt;&gt; 3</code> (the length of corresponding <code>byte[]</code> array).
     * @throws IllegalArgumentException if the argument is negative.
     */
    public static long packedLength(long unpackedLength) {
        if (unpackedLength < 0) {
            throw new IllegalArgumentException("Negative unpacked length");
        }
        return (unpackedLength + 7) >>> 3;
        // here >>> must be used instead of >>, because unpackedLength+7 may be >Long.MAX_VALUE
    }

    /**
     * Equivalent of {@link #packedLength(long)} for <code>int</code> argument.
     *
     * @param unpackedLength the number of bits (the length of bit array).
     * @return <code>(unpackedLength + 7) &gt;&gt;&gt; 3</code>
     * (the length of corresponding <code>byte[]</code> array).
     * @throws IllegalArgumentException if the argument is negative.
     */
    public static int packedLength(int unpackedLength) {
        if (unpackedLength < 0) {
            throw new IllegalArgumentException("Negative unpacked length");
        }
        return (unpackedLength + 7) >>> 3;
        // here >>> must be used instead of >>, because unpackedLength+63 may be >Integer.MAX_VALUE
    }

    /**
     * Returns the bit <code>#index</code> in the packed <code>src</code> bit array.
     * Equivalent to the following expression:<pre>
     * (src[(int)(index &gt;&gt;&gt; 3)] &amp; (1 &lt;&lt; (index &amp; 7))) != 0;
     * </pre>
     *
     * @param src   the source array (bits are packed in <code>byte</code> values).
     * @param index index of the returned bit.
     * @return the bit at the specified index.
     * @throws NullPointerException      if <code>src</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static boolean getBit(byte[] src, long index) {
        return (src[(int) (index >>> 3)] & (1 << ((int) index & 7))) != 0;
    }

    /**
     * Sets the bit <code>#index</code> in the packed <code>dest</code> bit array.
     * Equivalent to the following operators:<pre>
     * synchronized (dest) {
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] |= 1 &lt;&lt; (index &amp; 7);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] &amp;= ~(1 &lt;&lt; (index &amp; 7));
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <code>byte</code> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
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
     * Sets the bit <code>#index</code> in the packed <code>dest</code> bit array <i>without synchronization</i>.
     * May be used instead of {@link #setBit(byte[], long, boolean)}, if you are not planning to call
     * this method from different threads for the same <code>dest</code> array.
     * Equivalent to the following operators:<pre>
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] |= 1 &lt;&lt; (index &amp; 7);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] &amp;= ~(1 &lt;&lt; (index &amp; 7));
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <code>byte</code> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static void setBitNoSync(byte[] dest, long index, boolean value) {
        if (value)
            dest[(int) (index >>> 3)] |= (byte) (1 << ((int) index & 7));
        else
            dest[(int) (index >>> 3)] &= (byte) ~(1 << ((int) index & 7));
    }

    /**
     * Returns the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#srcPos</code>,
     * in the packed <code>src</code> bit array.
     *
     * <p>More precisely, the bit <code>#(srcPos+k)</code> will be returned in the bit <code>#k</code> of the returned
     * <code>long</code> value <code>R</code>: the first bit <code>#srcPos</code> will be equal to
     * <code>R&amp;1</code>,
     * the following bit <code>#(srcPos+1)</code> will be equal to <code>(R&gt;&gt;1)&amp;1</code>, etc.
     * If <code>count=0</code>, the result is 0.</p>
     *
     * <p>The same result can be calculated using the following loop
     * (for correct <code>count</code> in the range 0..64):</p>
     *
     * <pre>
     *      long result = 0;
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = {@link #getBit(byte[], long) PackedBitArraysPer8.getBit}(src, srcPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; k;
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <code>count</code> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the array (<code>&ge;8*src.length</code>);
     * instead, all bits outside the array are considered zero.
     * (But negative indexes are not allowed.)</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of bits to be unpacked (must be in range 0..64).
     * @return the sequence of <code>count</code> bits.
     * @throws NullPointerException      if <code>src</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>srcPos &lt; 0</code> or
     *                                   if copying would cause access of data outside array bounds.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static long getBits64(byte[] src, long srcPos, int count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IndexOutOfBoundsException("Negative srcPos argument: " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot get > 64 bits in getBits64 method");
        }
        final long srcPosDiv8 = srcPos >>> 3;
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
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#destPos</code>,
     * in the packed <code>dest</code> bit array. This is the reverse operation of
     * {@link #getBits64(byte[], long, int)}.
     *
     * <p>This function is equivalent to the following loop
     * (for correct <code>count</code> in the range 0..64):</p>
     *
     * <pre>
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = (bits &gt;&gt;&gt; k) &amp; 1L;
     *          {@link #setBit(byte[], long, boolean) PackedBitArraysPer8.setBit}(dest, destPos + k, bit != 0);
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <code>count</code> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the array (<code>&ge;8*dest.length</code>);
     * instead, extra bits outside the array are just ignored.
     * (But negative indexes are not allowed.)</p>
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be written (must be in range 0..64).
     * @throws NullPointerException      if <code>dest</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>destPos &lt; 0</code> or
     *                                   if copying would cause access of data outside array bounds.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static void setBits64(byte[] dest, long destPos, long bits, int count) {
        Objects.requireNonNull(dest, "Null dest");
        if (destPos < 0) {
            throw new IndexOutOfBoundsException("Negative destPos argument: " + destPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64 method");
        }
        final long destPosDiv8 = destPos >>> 3;
        if (count == 0 || destPosDiv8 >= dest.length) {
            return;
        }
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = count;
            maskStart &= (1 << (dPosRem + count)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        int dPos = (int) destPosDiv8;
        synchronized (dest) {
            if (cntStart > 0) {
                dest[dPos] = (byte) (((bits << dPosRem) & maskStart) | (dest[dPos] & ~maskStart));
                dPos++;
                count -= cntStart;
                bits >>>= cntStart;
            }
            while (count >= 8) {
                if (dPos >= dest.length) {
                    return;
                }
                dest[dPos++] = (byte) bits;
                count -= 8;
                bits >>>= 8;
            }
            if (count > 0 && dPos < dest.length) {
                int maskFinish = (1 << count) - 1; // count times 1 (from the left)
                dest[dPos] = (byte) ((bits & maskFinish) | (dest[dPos] & ~maskFinish));
            }
        }
    }

    /**
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#destPos</code>,
     * in the packed <code>dest</code> bit array, <i>without synchronization</i>.
     * May be used instead of {@link #setBits64(byte[], long, long, int)}, if you are not planning to call
     * this method from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be written (must be in range 0..64).
     * @throws NullPointerException      if <code>dest</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>destPos &lt; 0</code> or
     *                                   if copying would cause access of data outside array bounds.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static void setBits64NoSync(byte[] dest, long destPos, long bits, int count) {
        Objects.requireNonNull(dest, "Null dest");
        if (destPos < 0) {
            throw new IndexOutOfBoundsException("Negative destPos argument: " + destPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64NoSync method");
        }
        final long destPosDiv8 = destPos >>> 3;
        if (count == 0 || destPosDiv8 >= dest.length) {
            return;
        }
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = -1 << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = count;
            maskStart &= (1 << (dPosRem + count)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        int dPos = (int) destPosDiv8;
        if (cntStart > 0) {
            dest[dPos] = (byte) (((bits << dPosRem) & maskStart) | (dest[dPos] & ~maskStart));
            dPos++;
            count -= cntStart;
            bits >>>= cntStart;
        }
        while (count >= 8) {
            if (dPos >= dest.length) {
                return;
            }
            dest[dPos++] = (byte) bits;
            count -= 8;
            bits >>>= 8;
        }
        if (count > 0 && dPos < dest.length) {
            int maskFinish = (1 << count) - 1; // count times 1 (from the left)
            dest[dPos] = (byte) ((bits & maskFinish) | (dest[dPos] & ~maskFinish));
        }
    }

    /**
     * Returns the bit <code>#index</code> in the packed <code>src</code> bit array
     * for a case, when the bits are packed in each byte in the reverse order:
     * highest bit first, lowest bit last.
     * Equivalent to the following expression:<pre>
     * (src[(int)(index &gt;&gt;&gt; 3)] &amp; (1 &lt;&lt; (7 - (index &amp; 7)))) != 0;
     * </pre>
     *
     * <p>This bit order is used, for example, in TIFF format when storing binary images or
     * image with less than 8 bits per channel.</p>
     *
     * @param src   the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param index index of the returned bit.
     * @return the bit at the specified index.
     * @throws NullPointerException      if <code>src</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     * @see #reverseBitsOrderInEachByte(byte[], int, int)
     */
    public static boolean getBitInReverseOrder(byte[] src, long index) {
        return (src[(int) (index >>> 3)] & (1 << (7 - ((int) index & 7)))) != 0;
    }

    /**
     * Sets the bit <code>#index</code> in the packed <code>dest</code> bit array
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
     * @param dest  the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
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
     * Sets the bit <code>#index</code> in the packed <code>dest</code> bit array
     * for a case, when the bits are packed in each byte in the reverse order,
     * <i>without synchronization</i>.
     * May be used instead of {@link #setBitInReverseOrder(byte[], long, boolean)}, if you are not planning to call
     * this method from different threads for the same <code>dest</code> array.
     * Equivalent to the following operators:<pre>
     * &#32;   final int bitIndex = 7 - ((int) index &amp; 7);
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] |= (1 &lt;&lt; bitIndex);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 3)] &amp;= ~(1 &lt;&lt; bitIndex);
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static void setBitInReverseOrderNoSync(byte[] dest, long index, boolean value) {
        final int bitIndex = 7 - ((int) index & 7);
        if (value)
            dest[(int) (index >>> 3)] |= (byte) (1 << bitIndex);
        else
            dest[(int) (index >>> 3)] &= (byte) ~(1 << bitIndex);
    }

    /**
     * Returns the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#srcPos</code>,
     * in the packed <code>src</code> bit array for a case, when the bits are packed in each byte in the reverse order:
     * highest bit first, lowest bit last.
     *
     * <p>More precisely, the bit <code>#(srcPos+k)</code>, that can be read by the call
     * <code>{@link #getBitInReverseOrder(byte[] src, long index)
     * getBitInReverseOrder}(src, srcPos+k)</code>,
     * will be returned in the bit <code>#(count-1-k)</code> (in direct order) of the returned
     * <code>long</code> value <code>R</code>, i.e. it is equal to <code>(R&gt;&gt;(count-1-k))&amp;1</code></code>.
     * If <code>count=30</code>, the result is 0.</p>
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
     * <p>But this function works significantly faster, if <code>count</code> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the array (<code>&ge;8*src.length</code>); instead, all bits outside the array are considered zero.
     * (But negative indexes are not allowed.)</p>
     *
     * <p>This bit order is used, for example, in TIFF format when storing binary images or
     * image with less than 8 bits per channel.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of bits to be unpacked (must be in range 0..64).
     * @return the sequence of <code>count</code> bits.
     * @throws NullPointerException      if <code>src</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>srcPos &lt; 0</code> or
     *                                   if copying would cause access of data outside array bounds.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static long getBits64InReverseOrder(byte[] src, long srcPos, int count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IndexOutOfBoundsException("Negative srcPos argument: " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot get > 64 bits in getBits64 method");
        }
        final long srcPosDiv8 = srcPos >>> 3;
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
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#destPos</code>,
     * in the packed <code>dest</code> bit array
     * for a case, when the bits are packed in each byte in the reverse order.
     * This is the reverse operation of {@link #getBits64InReverseOrder(byte[], long, int)}.
     *
     * <p>This function is equivalent to the following loop:</p>
     *
     * <pre>
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = (bits &gt;&gt;&gt; (count - 1 - k)) &amp; 1L;
     *          {@link #setBitInReverseOrder(byte[], long, boolean)
     *          PackedBitArraysPer8.setBitInReverseOrder}(dest, destPos + k, bit != 0);
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <code>count</code> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the array (<code>&ge;8*dest.length</code>);
     * instead, extra bits outside the array are just ignored.
     * (But negative indexes are not allowed.)</p>
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be written (must be in range 0..64).
     * @throws NullPointerException      if <code>dest</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>destPos &lt; 0</code> or
     *                                   if copying would cause access of data outside array bounds.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static void setBits64InReverseOrder(byte[] dest, long destPos, long bits, int count) {
        Objects.requireNonNull(dest, "Null dest");
        if (destPos < 0) {
            throw new IndexOutOfBoundsException("Negative destPos argument: " + destPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64InReverseOrder method");
        }
        final long destPosDiv8 = destPos >>> 3;
        if (count == 0 || destPosDiv8 >= dest.length) {
            return;
        }
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = count;
            maskStart &= (0xFF00 >>> (dPosRem + count)); // &= dPosRem+cntStart times 1 (from the highest bit)
        }
        int dPos = (int) destPosDiv8;
        synchronized (dest) {
            if (cntStart > 0) {
                // bits #count-cntStart...#count-1 of "bits" argument should be copied to
                // bits #8-dPosRem-cntStart..#8-dPosRem-1 (where 8-dPosRem = original cntStart)
                // This means shifting >>> (count-1)-(8-dPosRem-1)
                int shift = count - (8 - dPosRem);
                long v = shift >= 0 ? bits >>> shift : bits << -shift;
                dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
                dPos++;
                count -= cntStart;
            }
            while (count >= 8) {
                if (dPos >= dest.length) {
                    return;
                }
                count -= 8;
                dest[dPos++] = (byte) (bits >>> count);
            }
            if (count > 0 && dPos < dest.length) {
                int maskFinish = 0xFF00 >>> count; // count times 1 (from the highest bit)
                // We still did not use only the count lowest bits
                // #0...#count-3; they should be copied to
                // #7...#(8-count) bits ot dest[dPos]
                dest[dPos] = (byte) (((bits << (8 - count)) & maskFinish) | (dest[dPos] & ~maskFinish));
            }
        }
    }

    /**
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#destPos</code>,
     * in the packed <code>dest</code> bit array
     * for a case, when the bits are packed in each byte in the reverse order,
     * <i>without synchronization</i>.
     * May be used instead of {@link #setBits64InReverseOrder(byte[], long, long, int)},
     * if you are not planning to call
     * this method from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be written (must be in range 0..64).
     * @throws NullPointerException      if <code>dest</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>destPos &lt; 0</code> or
     *                                   if copying would cause access of data outside array bounds.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static void setBits64InReverseOrderNoSync(byte[] dest, long destPos, long bits, int count) {
        Objects.requireNonNull(dest, "Null dest");
        if (destPos < 0) {
            throw new IndexOutOfBoundsException("Negative destPos argument: " + destPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count argument: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Too large count argument: " + count +
                    "; we cannot set > 64 bits in setBits64InReverseOrderNoSync method");
        }
        final long destPosDiv8 = destPos >>> 3;
        if (count == 0 || destPosDiv8 >= dest.length) {
            return;
        }
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = count;
            maskStart &= (0xFF00 >>> (dPosRem + cntStart)); // &= dPosRem+cntStart times 1 (from the highest bit)
        }
        int dPos = (int) destPosDiv8;
        if (cntStart > 0) {
            // bits #count-cntStart...#count-1 of "bits" argument should be copied to
            // bits #8-dPosRem-cntStart..#8-dPosRem-1 (where 8-dPosRem = original cntStart)
            // This means shifting >>> (count-1)-(8-dPosRem-1)
            int shift = count - (8 - dPosRem);
            long v = shift >= 0 ? bits >>> shift : bits << -shift;
            dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
            dPos++;
            count -= cntStart;
        }
        while (count >= 8) {
            if (dPos >= dest.length) {
                return;
            }
            count -= 8;
            dest[dPos++] = (byte) (bits >>> count);
        }
        if (count > 0 && dPos < dest.length) {
            int maskFinish = 0xFF00 >>> count; // count times 1 (from the highest bit)
            // We still did not use only the count lowest bits
            // #0...#count-3; they should be copied to
            // #7...#(8-count) bits ot dest[dPos]
            dest[dPos] = (byte) (((bits << (8 - count)) & maskFinish) | (dest[dPos] & ~maskFinish));
        }
    }

    /**
     * Copies <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to packed <code>dest</code> array, starting from the bit <code>#destPos</code>.
     *
     * <p><i>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and the copied areas overlap</i>,
     * i.e. if <code>Math.abs(destPos&nbsp;-&nbsp;srcPos)&nbsp;&lt;&nbsp;count</code>.
     * More precisely, in this case the copying is performed as if the
     * bits at positions <code>srcPos..srcPos+count-1</code>
     * were first unpacked to a temporary <code>boolean[]</code> array with <code>count</code> elements
     * and then the contents of the temporary array were packed into positions
     * <code>destPos..destPos+count-1</code>.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
                                (byte) ((src[sPos + cnt] & maskFinish) |
                                        (dest[dPos + cnt] & ~maskFinish));
                    }
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPosStart] =
                                (byte) ((src[sPosStart] & maskStart) |
                                        (dest[dPosStart] & ~maskStart));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
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
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                int cnt = (int) (count >>> 3);
                final long dPosMin = dPos;
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
                    // 1) cnt > 0, then src[sPos] is really necessary in the following loop;
                    // 2) cnt == 0 and cntStart > 0, then src[sPos] will be necessary for making dest[dPosStart].
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
                        dest[dPos] = (byte) ((src[sPos] & maskStart) | (dest[dPos] & ~maskStart));
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
                        dest[dPos] = (byte) ((src[sPos] & maskFinish) | (dest[dPos] & ~maskFinish));
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

    /**
     * Equivalent to {@link #copyBits(byte[], long, byte[], long, long)} method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #copyBits},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBitsNoSync(byte[] dest, long destPos, byte[] src, long srcPos, long count) {
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
                    dest[dPos + cnt] =
                            (byte) ((src[sPos + cnt] & maskFinish) |
                                    (dest[dPos + cnt] & ~maskFinish));
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    dest[dPosStart] =
                            (byte) ((src[sPosStart] & maskStart) |
                                    (dest[dPosStart] & ~maskStart));
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
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
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                int cnt = (int) (count >>> 3);
                final long dPosMin = dPos;
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
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                } else {
                    //Start_sPrev !! this comment is necessary for preprocessing by Repeater !!
                    sPrev = (src[sPos] & 0xFF);
                    // IndexOutOfBoundException is impossible here, because there is one of the following situations:
                    // 1) cnt > 0, then src[sPos] is really necessary in the following loop;
                    // 2) cnt == 0 and cntStart > 0, then src[sPos] will be necessary for making dest[dPosStart].
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
                    dest[dPosStart] = (byte) ((v & maskStart) | (dest[dPosStart] & ~maskStart));
                }
            }
        } else {
            // usual case
            if (sPosRem == dPosRem) {
                if (cntStart > 0) {
                    dest[dPos] = (byte) (((src[sPos] & 0xFF) & maskStart) | (dest[dPos] & ~maskStart));
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
                    dest[dPos] = (byte) (((src[sPos] & 0xFF) & maskFinish) | (dest[dPos] & ~maskFinish));
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
                    dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
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
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }
    }

    /**
     * Copies <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to packed <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * for a case, when the bits are packed in both arrays in the reverse order.
     *
     * <p><i>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and the copied areas overlap</i>,
     * i.e. if <code>Math.abs(destPos&nbsp;-&nbsp;srcPos)&nbsp;&lt;&nbsp;count</code>.
     * More precisely, in this case the copying is performed as if the
     * bits at positions <code>srcPos..srcPos+count-1</code>
     * were first unpacked to a temporary <code>boolean[]</code> array with <code>count</code> elements
     * and then the contents of the temporary array were packed into positions
     * <code>destPos..destPos+count-1</code> (in both cases in the reverse order).
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void copyBitsInReverseOrder(byte[] dest, long destPos, byte[] src, long srcPos, long count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (0xFF00 >>> (dPosRem + cntStart)); // &= dPosRem+cntStart times 1 (from the highest bit)
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
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    synchronized (dest) {
                        dest[dPos + cnt] =
                                (byte) ((src[sPos + cnt] & maskFinish) |
                                        (dest[dPos + cnt] & ~maskFinish));
                    }
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPosStart] =
                                (byte) ((src[sPosStart] & maskStart) |
                                        (dest[dPosStart] & ~maskStart));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
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
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                int cnt = (int) (count >>> 3);
                final long dPosMin = dPos;
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 7);
                final int sPosRem8 = 8 - sPosRem;
                int sPrev;
                if (cntFinish > 0) {
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = (sPrev = (src[sPos] & 0xFF)) << sPosRem;
                    } else {
                        v = ((sPrev = (src[sPos] & 0xFF)) << sPosRem) | ((src[sPos + 1] & 0xFF) >>> sPosRem8);
                    }
                    synchronized (dest) {
                        dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                    }
                } else {
                    sPrev = (src[sPos] & 0xFF);
                    // IndexOutOfBoundException is impossible here, because there is one of the following situations:
                    // 1) cnt > 0, then src[sPos] is really necessary in the following loop;
                    // 2) cnt == 0 and cntStart > 0, then src[sPos] will be necessary for making dest[dPosStart].
                    // All other situations are impossible here:
                    // 3) cntFinish > 0: it was processed above in "if (cntFinish > 0)..." branch;
                    // 4) cntStart == 0, cntFinish == 0 and cnt == 0, i.e. count == 0: it's impossible
                    // in this branch of all algorithm (overlap is impossible when count == 0).
                }
                while (dPos > dPosMin) { // cnt times
                    --sPos;
                    --dPos;
                    dest[dPos] = (byte) ((sPrev >>> sPosRem8) | ((sPrev = (src[sPos] & 0xFF)) << sPosRem));
                }
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    int v;
                    if (sPosRemStart + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (src[sPosStart] & 0xFF) >>> shift;
                        else
                            v = (src[sPosStart] & 0xFF) << -shift;
                    } else {
                        v = ((src[sPosStart] & 0xFF) << -shift) | ((src[sPosStart + 1] & 0xFF) >>> (8 + shift));
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
                        dest[dPos] = (byte) ((src[sPos] & maskStart) | (dest[dPos] & ~maskStart));
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
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    synchronized (dest) {
                        dest[dPos] = (byte) ((src[sPos] & maskFinish) | (dest[dPos] & ~maskFinish));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                int sNext;
                if (cntStart > 0) {
                    int v;
                    if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (sNext = (src[sPos] & 0xFF)) >>> shift;
                        else
                            v = (sNext = (src[sPos] & 0xFF)) << -shift;
                        sPosRem += cntStart;
                    } else {
                        v = ((src[sPos] & 0xFF) << -shift) | ((sNext = (src[sPos + 1] & 0xFF)) >>> (8 + shift));
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
                        return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                    }
                    sNext = (src[sPos] & 0xFF);
                }
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                final int sPosRem8 = 8 - sPosRem;
                for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                    sPos++;
                    dest[dPos] = (byte) ((sNext << sPosRem) | ((sNext = (src[sPos] & 0xFF)) >>> sPosRem8));
                    dPos++;
                }
                int cntFinish = (int) (count & 7);
                if (cntFinish > 0) {
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = sNext << sPosRem;
                    } else {
                        v = (sNext << sPosRem) | ((src[sPos + 1] & 0xFF) >>> sPosRem8);
                    }
                    synchronized (dest) {
                        dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                    }
                }
            }
        }
    }

    /**
     * Equivalent to {@link #copyBitsInReverseOrder(byte[], long, byte[], long, long)} method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #copyBitsInReverseOrder},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBitsInReverseOrderNoSync(byte[] dest, long destPos, byte[] src, long srcPos, long count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (0xFF00 >>> (dPosRem + cntStart)); // &= dPosRem+cntStart times 1 (from the highest bit)
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
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    dest[dPos + cnt] =
                            (byte) ((src[sPos + cnt] & maskFinish) |
                                    (dest[dPos + cnt] & ~maskFinish));
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    dest[dPosStart] =
                            (byte) ((src[sPosStart] & maskStart) |
                                    (dest[dPosStart] & ~maskStart));
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
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
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                int cnt = (int) (count >>> 3);
                final long dPosMin = dPos;
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 7);
                final int sPosRem8 = 8 - sPosRem;
                int sPrev;
                if (cntFinish > 0) {
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = (sPrev = (src[sPos] & 0xFF)) << sPosRem;
                    } else {
                        v = ((sPrev = (src[sPos] & 0xFF)) << sPosRem) | ((src[sPos + 1] & 0xFF) >>> sPosRem8);
                    }
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));

                } else {
                    sPrev = (src[sPos] & 0xFF);
                    // IndexOutOfBoundException is impossible here, because there is one of the following situations:
                    // 1) cnt > 0, then src[sPos] is really necessary in the following loop;
                    // 2) cnt == 0 and cntStart > 0, then src[sPos] will be necessary for making dest[dPosStart].
                    // All other situations are impossible here:
                    // 3) cntFinish > 0: it was processed above in "if (cntFinish > 0)..." branch;
                    // 4) cntStart == 0, cntFinish == 0 and cnt == 0, i.e. count == 0: it's impossible
                    // in this branch of all algorithm (overlap is impossible when count == 0).
                }
                while (dPos > dPosMin) { // cnt times
                    --sPos;
                    --dPos;
                    dest[dPos] = (byte) ((sPrev >>> sPosRem8) | ((sPrev = (src[sPos] & 0xFF)) << sPosRem));
                }
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    int v;
                    if (sPosRemStart + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (src[sPosStart] & 0xFF) >>> shift;
                        else
                            v = (src[sPosStart] & 0xFF) << -shift;
                    } else {
                        v = ((src[sPosStart] & 0xFF) << -shift) | ((src[sPosStart + 1] & 0xFF) >>> (8 + shift));
                    }
                    dest[dPosStart] = (byte) ((v & maskStart) | (dest[dPosStart] & ~maskStart));
                }
            }
        } else {
            // usual case
            if (sPosRem == dPosRem) {
                if (cntStart > 0) {
                    dest[dPos] = (byte) ((src[sPos] & maskStart) | (dest[dPos] & ~maskStart));

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
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    dest[dPos] = (byte) ((src[sPos] & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            } else {
                final int shift = dPosRem - sPosRem;
                int sNext;
                if (cntStart > 0) {
                    int v;
                    if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (sNext = (src[sPos] & 0xFF)) >>> shift;
                        else
                            v = (sNext = (src[sPos] & 0xFF)) << -shift;
                        sPosRem += cntStart;
                    } else {
                        v = ((src[sPos] & 0xFF) << -shift) | ((sNext = (src[sPos + 1] & 0xFF)) >>> (8 + shift));
                        sPos++;
                        sPosRem = (sPosRem + cntStart) & 7;
                    }
                    // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                    dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
                    count -= cntStart;
                    if (count == 0) {
                        return; // little optimization
                    }
                    dPos++;
                } else {
                    if (count == 0) {
                        return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                    }
                    sNext = (src[sPos] & 0xFF);
                }
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                final int sPosRem8 = 8 - sPosRem;
                for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                    sPos++;
                    dest[dPos] = (byte) ((sNext << sPosRem) | ((sNext = (src[sPos] & 0xFF)) >>> sPosRem8));
                    dPos++;
                }
                int cntFinish = (int) (count & 7);
                if (cntFinish > 0) {
                    int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = sNext << sPosRem;
                    } else {
                        v = (sNext << sPosRem) | ((src[sPos + 1] & 0xFF) >>> sPosRem8);
                    }
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }
    }

    /**
     * Copies <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>, to packed <code>dest</code> array, stored in the normal order,
     * starting from the bit <code>#destPos</code>.
     *
     * <p>The same action may be performed by the following operators:</p>
     * <pre>
     *     byte[] copy = pSrc.clone();
     *     {@link #reverseBitsOrderInEachByte(byte[])
     *     PackedBitArraysPer8.reverseBitsOrderInEachByte}(copy);
     *     {@link #copyBits PackedBitArraysPer8.copyBits}(dest, destPos, copy, srcPos, count);
     * </pre>
     * <p>but without necessity to copy <code>src</code> bytes into a new array.
     *
     * <p>Warning: unlike {@link #copyBits(byte[], long, byte[], long, long)},
     * this method <i>does not provide correct processing</i> the situation when <code>src&nbsp;==&nbsp;dest</code>
     * and the copied areas overlap. However, this method still <i>does</i> work correctly if
     * <code>src&nbsp;==&nbsp;dest</code> and <code>destPos&nbsp;&le;&nbsp;srcPos</code>. In particular,
     * this method allows you to invert the bit order in place: the following call</p>
     * <pre>
     *      PackedBitArraysPer8.copyBitsFromReverseToNormalOrder(dest, 0, dest, 0, dest.length * 8);
     * </pre>
     * <p>is equivalent to <code>{@link #reverseBitsOrderInEachByte(byte[])
     * PackedBitArraysPer8.reverseBitsOrderInEachByte}(dest)</code>.</p>
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     * @see #copyBits(byte[], long, byte[], long, long)
     * @see #copyBitsFromNormalToReverseOrder(byte[], long, byte[], long, long)
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void copyBitsFromReverseToNormalOrder(
            byte[] dest,
            long destPos,
            byte[] src,
            long srcPos,
            long count) {
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
                    dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskStart) | (dest[dPos] & ~maskStart));
                }
                // - only 8 low bits are stored
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = REVERSE[src[sPos] & 0xFF];
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskFinish) | (dest[dPos] & ~maskFinish));
                }
                // - only 8 low bits are stored
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) << shift;
                    else
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((REVERSE[src[sPos] & 0xFF] & 0xFF) >>> -shift) |
                            ((sNext = (REVERSE[src[sPos + 1] & 0xFF] & 0xFF)) << (8 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = REVERSE[src[sPos] & 0xFF] & 0xFF;
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = (byte) ((sNext >>> sPosRem) | ((sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) << sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((REVERSE[src[sPos + 1] & 0xFF] & 0xFF) << sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }
    }


    /**
     * Equivalent to {@link #copyBitsFromReverseToNormalOrder(byte[], long, byte[], long, long)}
     * method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #copyBitsFromReverseToNormalOrder},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBitsFromReverseToNormalOrderNoSync(
            byte[] dest,
            long destPos,
            byte[] src,
            long srcPos,
            long count) {
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
                dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskStart) | (dest[dPos] & ~maskStart));
                // - only 8 low bits are stored
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = REVERSE[src[sPos] & 0xFF];
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskFinish) | (dest[dPos] & ~maskFinish));
                // - only 8 low bits are stored
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) << shift;
                    else
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((REVERSE[src[sPos] & 0xFF] & 0xFF) >>> -shift) |
                            ((sNext = (REVERSE[src[sPos + 1] & 0xFF] & 0xFF)) << (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = REVERSE[src[sPos] & 0xFF] & 0xFF;
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = (byte) ((sNext >>> sPosRem) | ((sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) << sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | ((REVERSE[src[sPos + 1] & 0xFF] & 0xFF) << sPosRem8);
                }
                dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
            }
        }
    }

    /**
     * Copies <code>count</code> bits, packed in <code>src</code> array in the normal order,
     * starting from the bit <code>#srcPos</code>, to packed <code>dest</code> array, stored in the reverse order,
     * starting from the bit <code>#destPos</code>.
     *
     * <p>The same action may be performed by the following simple loop:</p>
     * <pre>
     *      byte[] copy = pSrc.clone();
     *      for (int k = 0; k &lt; count; k++) {
     *          final boolean bit = {@link #getBit
     *              PackedBitArraysPer8.getBit}(copy, srcPos + k);
     *          {@link #setBitInReverseOrder
     *              PackedBitArraysPer8.setBitInReverseOrder}(dest, destPos + k, bit);
     *      }
     * </pre>
     * <p>but this method works much faster.
     *
     * <p>Warning: unlike {@link #copyBits(byte[], long, byte[], long, long)},
     * this method <i>does not provide correct processing</i> the situation when <code>src&nbsp;==&nbsp;dest</code>
     * and the copied areas overlap. However, this method still <i>does</i> work correctly if
     * <code>src&nbsp;==&nbsp;dest</code> and <code>destPos&nbsp;&le;&nbsp;srcPos</code>. In particular,
     * this method allows you to invert the bit order in place: the following call</p>
     * <pre>
     *      PackedBitArraysPer8.copyBitsFromNormalToReverseOrder(dest, 0, dest, 0, dest.length * 8);
     * </pre>
     * <p>(like the similar call of {@link #copyBitsFromReverseToNormalOrder})
     * is equivalent to <code>{@link #reverseBitsOrderInEachByte(byte[])
     * PackedBitArraysPer8.reverseBitsOrderInEachByte}(dest)</code>.</p>
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     * @see #copyBits(byte[], long, byte[], long, long)
     * @see #copyBitsFromReverseToNormalOrder(byte[], long, byte[], long, long)
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void copyBitsFromNormalToReverseOrder(
            byte[] dest,
            long destPos,
            byte[] src,
            long srcPos,
            long count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (0xFF00 >>> (dPosRem + cntStart)); // &= dPosRem+cntStart times 1 (from the highest bit)
        }
        // Note: overlapping IS NOT supported!
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskStart) | (dest[dPos] & ~maskStart));
                    // - only 8 low bits are stored
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = REVERSE[src[sPos] & 0xFF];
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                synchronized (dest) {
                    dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskFinish) | (dest[dPos] & ~maskFinish));
                    // - only 8 low bits are stored
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) >>> shift;
                    else
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) << -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((REVERSE[src[sPos] & 0xFF] & 0xFF) << -shift) |
                            ((sNext = (REVERSE[src[sPos + 1] & 0xFF] & 0xFF)) >>> (8 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = REVERSE[src[sPos] & 0xFF] & 0xFF;
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = (byte) ((sNext << sPosRem) | ((sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) >>> sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext << sPosRem;
                } else {
                    v = (sNext << sPosRem) | ((REVERSE[src[sPos + 1] & 0xFF] & 0xFF) >>> sPosRem8);
                }
                synchronized (dest) {
                    dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
                }
            }
        }
    }

    /**
     * Equivalent to {@link #copyBitsFromNormalToReverseOrder(byte[], long, byte[], long, long)}
     * method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #copyBitsFromNormalToReverseOrder},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBitsFromNormalToReverseOrderNoSync(
            byte[] dest,
            long destPos,
            byte[] src,
            long srcPos,
            long count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 3);
        int dPos = (int) (destPos >>> 3);
        int sPosRem = (int) (srcPos & 7);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (0xFF00 >>> (dPosRem + cntStart)); // &= dPosRem+cntStart times 1 (from the highest bit)
        }
        // Note: overlapping IS NOT supported!
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskStart) | (dest[dPos] & ~maskStart));
                // - only 8 low bits are stored
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = REVERSE[src[sPos] & 0xFF];
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                dest[dPos] = (byte) ((REVERSE[src[sPos] & 0xFF] & maskFinish) | (dest[dPos] & ~maskFinish));
                // - only 8 low bits are stored
            }
        } else {
            final int shift = dPosRem - sPosRem;
            int sNext;
            if (cntStart > 0) {
                int v;
                if (sPosRem + cntStart <= 8) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) >>> shift;
                    else
                        v = (sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) << -shift;
                    sPosRem += cntStart;
                } else {
                    v = ((REVERSE[src[sPos] & 0xFF] & 0xFF) << -shift) |
                            ((sNext = (REVERSE[src[sPos + 1] & 0xFF] & 0xFF)) >>> (8 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 7;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                dest[dPos] = (byte) ((v & maskStart) | (dest[dPos] & ~maskStart));
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = REVERSE[src[sPos] & 0xFF] & 0xFF;
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem8 = 8 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = (byte) ((sNext << sPosRem) | ((sNext = (REVERSE[src[sPos] & 0xFF] & 0xFF)) >>> sPosRem8));
                dPos++;
            }
            int cntFinish = (int) (count & 7);
            if (cntFinish > 0) {
                int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
                int v;
                if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                    v = sNext << sPosRem;
                } else {
                    v = (sNext << sPosRem) | ((REVERSE[src[sPos + 1] & 0xFF] & 0xFF) >>> sPosRem8);
                }
                dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
            }
        }
    }

    /**
     * Copies <code>count</code> bits from the <code>src</code> array, starting from the element <code>#srcPos</code>,
     * into a newly created packed bit array <code>byte[{@link #packedLength(int) packedLength}(count)]</code>
     * returned as a result, starting from the bit #0.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #packBits(byte[], long, boolean[], int, int)}
     * method.</p>
     *
     * @param src    the source array (unpacked <code>boolean</code> values).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of bits to be packed (must be &gt;=0).
     * @return the result bit array, where bits are packed in <code>byte</code>.
     * @throws NullPointerException     if <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     */
    public static byte[] packBits(boolean[] src, int srcPos, int count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > src.length - srcPos) {
            throw new IllegalArgumentException("Too short source array boolean[" + src.length +
                    "]: it does not contain " + count + " bits since position " + srcPos);
        }
        final byte[] dest = new byte[packedLength(count)];

        final int cnt = count >>> 3;
        for (int k = 0; k < cnt; k++) {
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
        }
        int countFinish = count & 7;
        for (int k = 0; k < countFinish; srcPos++, k++) {
            if (src[srcPos]) {
                dest[cnt] |= (byte) (1 << k);
            } else {
                dest[cnt] &= (byte) ~(1 << k);
            }
        }
        return dest;
    }

    /**
     * Copies <code>count</code> bits from <code>src</code> array, starting from the element <code>#srcPos</code>,
     * to packed <code>dest</code> array, starting from the bit <code>#destPos</code>.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (unpacked <code>boolean</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Equivalent to {@link #packBits(byte[], long, boolean[], int, int)}
     * method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #packBits(byte[], long, boolean[], int, int)},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (unpacked <code>boolean</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsNoSync(byte[] dest, long destPos, boolean[] src, int srcPos, int count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (destPos & 7) == 0 ? 0 : 8 - (int) (destPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        if (countStart > 0) {
            for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                if (src[srcPos])
                    dest[(int) (destPos >>> 3)] |= (byte) (1 << (destPos & 7));
                else
                    dest[(int) (destPos >>> 3)] &= (byte) (~(1 << (destPos & 7)));
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
            for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                if (src[srcPos])
                    dest[(int) (destPos >>> 3)] |= (byte) (1 << (destPos & 7));
                else
                    dest[(int) (destPos >>> 3)] &= (byte) (~(1 << (destPos & 7)));
            }
        }
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>boolean[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(boolean[], int, byte[], long, int)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of elements to be unpacked (must be &gt;=0).
     * @return the unpacked <code>boolean</code> array.
     * @throws NullPointerException     if <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static boolean[] unpackBits(byte[] src, long srcPos, long count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final boolean[] result = new boolean[(int) count];
        unpackBits(result, 0, src, srcPos, result.length);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>boolean</code> array, starting from the element <code>#destPos</code>.
     *
     * @param dest    the destination array (unpacked <code>boolean</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>boolean[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBitsInReverseOrder(boolean[], int, byte[], long, int)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos position of the first bit read in the source array.
     * @param count  the number of elements to be unpacked (must be &gt;=0).
     * @return the unpacked <code>boolean</code> array.
     * @throws NullPointerException     if <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static boolean[] unpackBitsInReverseOrder(byte[] src, long srcPos, long count) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final boolean[] result = new boolean[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>boolean</code> array, starting from the element <code>#destPos</code>.
     *
     * @param dest    the destination array (unpacked <code>boolean</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(boolean[] dest, int destPos, byte[] src, long srcPos, int count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0;
            dest[destPos + 1] = (v & (1 << 6)) != 0;
            dest[destPos + 2] = (v & (1 << 5)) != 0;
            dest[destPos + 3] = (v & (1 << 4)) != 0;
            dest[destPos + 4] = (v & (1 << 3)) != 0;
            dest[destPos + 5] = (v & (1 << 2)) != 0;
            dest[destPos + 6] = (v & (1 << 1)) != 0;
            dest[destPos + 7] = (v & 1) != 0;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0;
        }
    }

    /*Repeat() (boolean\[\] unpackBits(?:InReverseOrder)?) ==>
               $1ToChars,,$1ToBytes,,$1ToShorts,,$1ToInts,,$1ToLongs,,$1ToFloats,,$1ToDoubles;;
               boolean ==> char,,byte,,short,,int,,long,,float,,double;;
               Booleans ==> Chars,,Bytes,,Shorts,,Ints,,Longs,,Floats,,Doubles */

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>boolean[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(boolean[], int, byte[], long, int, boolean, boolean)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>boolean</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static boolean[] unpackBits(
            byte[] src, long srcPos, long count,
            boolean bit0Value, boolean bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final boolean[] result = new boolean[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>boolean</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>boolean</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>boolean[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(boolean[], int, byte[], long, int, boolean, boolean)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>boolean</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static boolean[] unpackBitsInReverseOrder(
            byte[] src, long srcPos, long count,
            boolean bit0Value, boolean bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final boolean[] result = new boolean[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>boolean</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>boolean</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            boolean[] dest, int destPos, byte[] src, long srcPos, int count,
            boolean bit0Value, boolean bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>char[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(char[], int, byte[], long, int, char, char)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>char</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static char[] unpackBitsToChars(
            byte[] src, long srcPos, long count,
            char bit0Value, char bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final char[] result = new char[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>char</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>char</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>char[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(char[], int, byte[], long, int, char, char)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>char</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static char[] unpackBitsInReverseOrderToChars(
            byte[] src, long srcPos, long count,
            char bit0Value, char bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final char[] result = new char[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>char</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>char</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            char[] dest, int destPos, byte[] src, long srcPos, int count,
            char bit0Value, char bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>byte[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(byte[], int, byte[], long, int, byte, byte)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>byte</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static byte[] unpackBitsToBytes(
            byte[] src, long srcPos, long count,
            byte bit0Value, byte bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final byte[] result = new byte[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>byte</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>byte</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>byte[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(byte[], int, byte[], long, int, byte, byte)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>byte</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static byte[] unpackBitsInReverseOrderToBytes(
            byte[] src, long srcPos, long count,
            byte bit0Value, byte bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final byte[] result = new byte[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>byte</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>byte</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            byte[] dest, int destPos, byte[] src, long srcPos, int count,
            byte bit0Value, byte bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>short[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(short[], int, byte[], long, int, short, short)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>short</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static short[] unpackBitsToShorts(
            byte[] src, long srcPos, long count,
            short bit0Value, short bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final short[] result = new short[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>short</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>short</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>short[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(short[], int, byte[], long, int, short, short)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>short</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static short[] unpackBitsInReverseOrderToShorts(
            byte[] src, long srcPos, long count,
            short bit0Value, short bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final short[] result = new short[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>short</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>short</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            short[] dest, int destPos, byte[] src, long srcPos, int count,
            short bit0Value, short bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>int[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(int[], int, byte[], long, int, int, int)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>int</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static int[] unpackBitsToInts(
            byte[] src, long srcPos, long count,
            int bit0Value, int bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final int[] result = new int[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>int</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>int</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>int[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(int[], int, byte[], long, int, int, int)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>int</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static int[] unpackBitsInReverseOrderToInts(
            byte[] src, long srcPos, long count,
            int bit0Value, int bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final int[] result = new int[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>int</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>int</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            int[] dest, int destPos, byte[] src, long srcPos, int count,
            int bit0Value, int bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>long[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(long[], int, byte[], long, int, long, long)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>long</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static long[] unpackBitsToLongs(
            byte[] src, long srcPos, long count,
            long bit0Value, long bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final long[] result = new long[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>long</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>long</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>long[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(long[], int, byte[], long, int, long, long)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>long</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static long[] unpackBitsInReverseOrderToLongs(
            byte[] src, long srcPos, long count,
            long bit0Value, long bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final long[] result = new long[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>long</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>long</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            long[] dest, int destPos, byte[] src, long srcPos, int count,
            long bit0Value, long bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>float[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(float[], int, byte[], long, int, float, float)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>float</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static float[] unpackBitsToFloats(
            byte[] src, long srcPos, long count,
            float bit0Value, float bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final float[] result = new float[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>float</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>float</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>float[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(float[], int, byte[], long, int, float, float)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>float</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static float[] unpackBitsInReverseOrderToFloats(
            byte[] src, long srcPos, long count,
            float bit0Value, float bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final float[] result = new float[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>float</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>float</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            float[] dest, int destPos, byte[] src, long srcPos, int count,
            float bit0Value, float bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }


    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to newly created array <code>double[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(double[], int, byte[], long, int, double, double)}
     * method.</p>
     *
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>double</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static double[] unpackBitsToDoubles(
            byte[] src, long srcPos, long count,
            double bit0Value, double bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final double[] result = new double[(int) count];
        unpackBits(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>double</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>double</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Unpacks <code>count</code> bits, packed in <code>src</code> array in the reverse order,
     * starting from the bit <code>#srcPos</code>,
     * to newly created array <code>double[count]</code> array returned as a result.
     * Every element <code>result[k]</code> of the result array is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * <p>Note that this method provides more user-friendly exception messages in a case
     * of incorrect arguments, than {@link #unpackBits(double[], int, byte[], long, int, double, double)}
     * method.</p>
     *
     * @param src    the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @return the unpacked <code>double</code> array.
     * @throws NullPointerException     if either <code>src</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>srcPos</code> or <code>count</code> is negative, or
     *                                  if copying would cause access of data outside the source array bounds.
     * @throws TooLargeArrayException   if <code>count &ge; Integer.MAX_VALUE</code> (cannot create the result array).
     */
    public static double[] unpackBitsInReverseOrderToDoubles(
            byte[] src, long srcPos, long count,
            double bit0Value, double bit1Value) {
        Objects.requireNonNull(src, "Null src");
        if (srcPos < 0) {
            throw new IllegalArgumentException("Negative srcPos = " + srcPos);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Negative count = " + count);
        }
        if (count > unpackedLength(src) - srcPos) {
            throw new IllegalArgumentException("Too short source array byte[" + src.length +
                    "]: it cannot contain " + count + " bits since position " + srcPos);
        }
        if (count > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large bit array for unpacking to Java array: " +
                    count + " >= 2^31 bits");
        }
        final double[] result = new double[(int) count];
        unpackBitsInReverseOrder(result, 0, src, srcPos, result.length, bit0Value, bit1Value);
        return result;
    }

    /**
     * Unpacks <code>count</code> bits, packed in <code>src</code> array, starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> <code>double</code> array, starting from the element <code>#destPos</code>.
     * It means that every element <code>dest[destPos+k]</code> is assigned to
     * <code>{@link #getBitInReverseOrder getBitInReverseOrder}(srcPos+k)?bit1Value:bit0Value</code>.
     *
     * @param dest      the destination array (unpacked <code>double</code> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param srcPos    position of the first bit read in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBitsInReverseOrder(
            double[] dest, int destPos, byte[] src, long srcPos, int count,
            double bit0Value, double bit1Value) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count) {
            countStart = count;
        }
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 3;
        for (int k = (int) (srcPos >>> 3), kMax = k + cnt; k < kMax; k++) {
            int v = src[k];
            srcPos += 8;
            dest[destPos] = (v & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 1] = (v & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (v & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (v & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (v & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (v & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (v & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (v & 1) != 0 ? bit1Value : bit0Value;
            destPos += 8;
        }
        int countFinish = count & 7;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 3)] & (1 << (7 - (srcPos & 7)))) != 0 ? bit1Value : bit0Value;
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, logicalOperations)
        long\[\] ==> byte[] ;;
        <code>long<\/code> ==> <code>byte</code> ;;
        >>>\s*6 ==> >>> 3 ;;
        63 ==> 7 ;;
        64 ==> 8 ;;
        (dest\[\w+\]\s*(?:\&=|\|=|\^=|=)\s*)([^;]+); ==> $1(byte) ($2); ;;
        src\[(sPos|sPos \+ 1)\] ==> (src[$1] & 0xFF) ;;
        long\s+(v\b|sNext\b|mask) ==> int $1 ;;
        1L\s+<< ==> 1 <<
        !! Auto-generated: NOT EDIT !! */

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical NOT of corresponding <code>count</code> bits,
     * packed in <code>src</code> array, starting from the bit <code>#srcPos</code>.
     *
     * <p>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and <code>srcPos&nbsp;==&nbsp;destPos</code>:
     * in this case it just inverts the specified bits.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Equivalent to <code>{@link #notBits(byte[], long, byte[], long, long)
     * notBits}(dest, destPos, dest, destPos, count)</code>.
     *
     * @param dest    the source/destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the source/destination array.
     * @param count   the number of bits to be inverted (must be &gt;=0).
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void notBits(byte[] dest, long destPos, long count) {
        notBits(dest, destPos, dest, destPos, count);
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical AND of them and corresponding <code>count</code> bits,
     * packed in <code>src</code> array, starting from the bit <code>#srcPos</code>.
     *
     * <p>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and <code>srcPos&nbsp;==&nbsp;destPos</code>:
     * in this case it does nothing (so there are no reasons for this call).
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical OR of them and corresponding <code>count</code> bits,
     * packed in <code>src</code> array, starting from the bit <code>#srcPos</code>.
     *
     * <p>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and <code>srcPos&nbsp;==&nbsp;destPos</code>:
     * in this case it does nothing (so there are no reasons for this call).
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical XOR of them and corresponding <code>count</code> bits,
     * packed in <code>src</code> array, starting from the bit <code>#srcPos</code>.
     *
     * <p>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and <code>srcPos&nbsp;==&nbsp;destPos</code>:
     * in this case it clears all specified bits.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical AND of them and <i>inverted</i> corresponding <code>count</code> bits,
     * packed in <code>src</code> array, starting from the bit <code>#srcPos</code>.
     *
     * <p>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and <code>srcPos&nbsp;==&nbsp;destPos</code>:
     * in this case it clears all specified bits.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical OR of them and <i>inverted</i> corresponding <code>count</code> bits,
     * packed in <code>src</code> array, starting from the bit <code>#srcPos</code>.
     *
     * <p>This method works correctly even if <code>src&nbsp;==&nbsp;dest</code>
     * and <code>srcPos&nbsp;==&nbsp;destPos</code>:
     * in this case it sets all specified bits to 1.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source array (bits are packed in <code>byte</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
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
    /*Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, fillBits)
        long\[\] ==> byte[] ;;
        <code>long<\/code> ==> <code>byte</code> ;;
        >>>\s*6 ==> >>> 3 ;;
        63 ==> 7 ;;
        64 ==> 8 ;;
        (dest\[\w+\]\s*(?:\&=|\|=|\^=|=)\s*)([^;]+); ==> $1(byte) ($2); ;;
        long longValue = value \? -1 \: 0 ==> byte byteValue = value ? (byte) -1 : (byte) 0 ;;
        \(byte\) \(longValue\) ==> byteValue ;;
        src\[(sPos|sPos \+ 1)\] ==> (src[$1] & 0xFF) ;;
        long\s+(v\b|sNext\b|mask) ==> int $1 ;;
        1L\s+<< ==> 1 <<

        !! Auto-generated: NOT EDIT !! */

    /**
     * Fills <code>count</code> bits in the packed <code>dest</code> array, starting
     * from the bit <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<code>false</code> means the bit 0,
     *                <code>true</code> means the bit 1).
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
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
                    dest[dPos] |= (byte) (maskStart);
                else
                    dest[dPos] &= (byte) (~maskStart);
            }
            count -= cntStart;
            dPos++;
        }
        byte byteValue = value ? (byte) -1 : (byte) 0;
        for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++) {
            dest[dPos] = byteValue;
        }
        int cntFinish = (int) (count & 7);
        if (cntFinish > 0) {
            int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
            synchronized (dest) {
                if (value)
                    dest[dPos] |= (byte) (maskFinish);
                else
                    dest[dPos] &= (byte) (~maskFinish);
            }
        }
    }

    /**
     * Equivalent to {@link #fillBits(byte[], long, long, boolean)}
     * method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #fillBits},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<code>false</code> means the bit 0,
     *                <code>true</code> means the bit 1).
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if filling would cause access of data outside array bounds.
     */
    public static void fillBitsNoSync(byte[] dest, long destPos, long count, boolean value) {
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
            if (value)
                dest[dPos] |= (byte) (maskStart);
            else
                dest[dPos] &= (byte) (~maskStart);
            count -= cntStart;
            dPos++;
        }
        byte byteValue = value ? (byte) -1 : (byte) 0;
        for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++) {
            dest[dPos] = byteValue;
        }
        int cntFinish = (int) (count & 7);
        if (cntFinish > 0) {
            int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
            if (value)
                dest[dPos] |= (byte) (maskFinish);
            else
                dest[dPos] &= (byte) (~maskFinish);

        }
    }
    /*Repeat.IncludeEnd*/

    /**
     * Fills <code>count</code> bits in the packed <code>dest</code> array, starting from the bit
     * <code>#destPos</code>, for a case, when the bits are packed in each byte in the reverse order.
     * <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<code>false</code> means the bit 0,
     *                <code>true</code> means the bit 1).
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if filling would cause access of data outside array bounds.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void fillBitsInReverseOrder(byte[] dest, long destPos, long count, boolean value) {
        Objects.requireNonNull(dest, "Null dest");
        int dPos = (int) (destPos >>> 3);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (0xFF00 >>> (dPosRem + count)); // &= dPosRem+cntStart times 1 (from the highest bit)
        }
        if (cntStart > 0) {
            synchronized (dest) {
                if (value)
                    dest[dPos] |= (byte) (maskStart);
                else
                    dest[dPos] &= (byte) (~maskStart);
            }
            count -= cntStart;
            dPos++;
        }
        byte byteValue = value ? (byte) -1 : (byte) 0;
        for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++) {
            dest[dPos] = byteValue;
        }
        int cntFinish = (int) (count & 7);
        if (cntFinish > 0) {
            int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
            synchronized (dest) {
                if (value)
                    dest[dPos] |= (byte) (maskFinish);
                else
                    dest[dPos] &= (byte) (~maskFinish);
            }
        }
    }

    /**
     * Equivalent to {@link #fillBitsInReverseOrder(byte[], long, long, boolean)}
     * method with the only exception,
     * that this method does not perform synchronization on <code>dest</code> array.
     * You may use this method instead of {@link #fillBitsInReverseOrder},
     * if you are not planning to call it from different threads for the same <code>dest</code> array.
     *
     * @param dest    the destination array (bits are packed in <code>byte</code> values in reverse order 76543210).
     * @param destPos position of the first bit written in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<code>false</code> means the bit 0,
     *                <code>true</code> means the bit 1).
     * @throws NullPointerException      if <code>dest</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if filling would cause access of data outside array bounds.
     */
    public static void fillBitsInReverseOrderNoSync(byte[] dest, long destPos, long count, boolean value) {
        Objects.requireNonNull(dest, "Null dest");
        int dPos = (int) (destPos >>> 3);
        int dPosRem = (int) (destPos & 7);
        int cntStart = (-dPosRem) & 7;
        int maskStart = 0xFF >>> dPosRem; // dPosRem times 0, then 1 (from the highest bit)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (0xFF00 >>> (dPosRem + count)); // &= dPosRem+cntStart times 1 (from the highest bit)
        }
        if (cntStart > 0) {
            if (value)
                dest[dPos] |= (byte) (maskStart);
            else
                dest[dPos] &= (byte) (~maskStart);
            count -= cntStart;
            dPos++;
        }
        byte byteValue = value ? (byte) -1 : (byte) 0;
        for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; dPos++) {
            dest[dPos] = byteValue;
        }
        int cntFinish = (int) (count & 7);
        if (cntFinish > 0) {
            int maskFinish = 0xFF00 >>> cntFinish; // cntFinish times 1 (from the highest bit)
            if (value)
                dest[dPos] |= (byte) (maskFinish);
            else
                dest[dPos] &= (byte) (~maskFinish);
        }
    }

    /**
     * Returns the number of high bits (1) in the given fragment of the given packed bit array.
     *
     * @param src       the source packed bit array.
     * @param fromIndex the initial checked bit index in <code>array</code>, inclusive.
     * @param toIndex   the end checked bit index in <code>array</code>, exclusive.
     * @return the number of high bits (1) in the given fragment of the given packed bit array.
     * @throws NullPointerException      if the <code>src</code> argument is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>fromIndex</code> or <code>toIndex</code> are negative,
     *                                   if <code>toIndex</code> is greater than <code>src.length*8</code>,
     *                                   or if <code>fromIndex</code> is greater than <code>startIndex</code>
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
     * Equivalent to <code>{@link #reverseBitsOrderInEachByte(byte[], int, int)
     * reverseBitOrder}(bytes, 0, bytes.length)</code>.
     *
     * @param bytes array to be processed.
     * @throws NullPointerException if <code>bytes</code> is <code>null</code>.
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
     * <code>(b>>7)&amp;1</code>,
     * <code>(b>>6)&amp;1</code>,
     * <code>(b>>5)&amp;1</code>,
     * <code>(b>>4)&amp;1</code>,
     * <code>(b>>3)&amp;1</code>,
     * <code>(b>>2)&amp;1</code>,
     * <code>(b>>1)&amp;1</code>,
     * <code>b&amp;1</code>
     * (highest bits first) for each byte <code>b</code>. You should reverse the bit order in such an array
     * before using other methods of this class or, for simple cases, use the methods
     * {@link #getBitInReverseOrder(byte[], long)} and {@link #setBitInReverseOrder(byte[], long, boolean)}.</p>
     *
     * @param bytes array to be processed.
     * @throws NullPointerException      if <code>bytes</code> is <code>null</code>.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
