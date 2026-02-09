/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.nio.LongBuffer;
import java.util.Objects;

/**
 * <p>Operations with bit arrays packed into <code>java.nio.LongBuffer</code>.</p>
 *
 * <p>AlgART bits arrays, created by {@link BufferMemoryModel} and {@link LargeMemoryModel},
 * are based on operations provided by this class.</p>
 *
 * <p>The maximal length of bit arrays supported by this class is <code>2<sup>37</sup>-64</code>.
 * All indexes and lengths passed to methods of this class should not exceed this value.
 * In another case, the results are unspecified. ("Unspecified" means that any elements
 * of the passed buffers can be read or changed, or that <code>IndexOutOfBoundsException</code> can be thrown.)</p>
 *
 * <p>In all methods of this class, it's supposed that the bit <code>#k</code>
 * in a packed <code>LongBuffer b</code> is the bit
 * <code>#(k%64)</code> in the long element <code>b.get(k/64)</code>. In other words, the bit <code>#k</code>
 * (<code>false</code> or <code>true</code> value) can be extracted by the following operator:</p>
 *
 * <pre>
 * (b.get(k &gt;&gt;&gt; 6) &amp; (1L &lt;&lt; (k &amp; 63))) != 0L
 * </pre>
 *
 * <p>and can be set or cleared by the following operators:</p>
 *
 * <pre>
 * if (newValue) // we need to set bit #k to 1
 * &#32;   b.put(k &gt;&gt;&gt; 6, b.get(k &gt;&gt;&gt; 6) | 1L &lt;&lt; (k &amp; 63));
 * else          // we need to clear bit #k to 0
 * &#32;   b.put(k &gt;&gt;&gt; 6, b.get(k &gt;&gt;&gt; 6) &amp; ~(1L &lt;&lt; (k &amp; 63)));
 * </pre>
 *
 * <p>You may use {@link #getBit(LongBuffer, long)} and {@link #setBit(LongBuffer, long, boolean)}, implementing
 * the equivalent code.</p>
 *
 * <p>If any method of this class modifies some portion of an element of a packed <code>LongBuffer</code>,
 * i.e. modifies less than all 64 its bits, then all accesses to this <code>long</code> element are performed
 * <b>inside a single synchronized block</b>, using the following instruction:</p>
 *
 * <pre>
 * synchronized ({@link #getLock getLock}(buffer)) {
 * &#32;   // accessing to some element #k via buffer.get(k) and buffer.put(k, ...)
 * }
 * </pre>
 *
 * <p>unless otherwise specified in the method comments. (See an example in comments to {@link #setBit} method.)
 * If all 64 bits of the element are written, or if the bits are read only, then no synchronization is performed.
 * Such behavior allows to simultaneously work with non-overlapping fragments of a packed bit array
 * from several threads (different fragments for different threads), as if it would be a usual Java array.
 * Synchronization by <code>{@link #getLock getLock}(buffer)</code> (instead of <code>buffer</code> instance) allows
 * to use in different threads different instances of <code>LongBuffer</code>, created by <code>LongBuffer.wrap</code>
 * method for the sampe Java <code>long[]</code> array.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 * @see PackedBitArrays
 */
public class PackedBitBuffers {
    private PackedBitBuffers() {
    }

    /**
     * Returns <code>numberOfLongs &lt;&lt; 6</code>: the maximal number of bits that
     * can be stored in the specified number of <code>long</code> values.
     *
     * @param numberOfLongs number of packed <code>long[]</code> values.
     * @return <code>64 * numberOfLongs</code>
     * @throws TooLargeArrayException   if the argument is too large: &ge; 2<sup>57</sup>.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public static long unpackedLength(long numberOfLongs) {
        return PackedBitArrays.unpackedLength(numberOfLongs);
    }

    /**
     * Returns <code>((long) buffer.limit())) &lt;&lt; 6</code>: the maximal number of bits that
     * can be stored in the specified buffer.
     *
     * @param buffer the buffer (bits are packed into <code>long</code> values).
     * @return <code>64 * (long) buffer.limit()</code>
     * @throws NullPointerException if the argument is {@code null}.
     */
    public static long unpackedLength(LongBuffer buffer) {
        return ((long) buffer.limit()) << 6;
    }

    /**
     * Returns <code>(numberOfButs + 63) &gt;&gt;&gt; 6</code>: the minimal number of <code>long</code> values
     * allowing to store the specified number of bits.
     *
     * @param numberOfBits the number of bits (the length of bit array).
     * @return <code>(numberOfButs + 63) &gt;&gt;&gt; 6</code>
     * (the length of corresponding <code>long[]</code> array).
     */
    public static long packedLength(long numberOfBits) {
        return PackedBitArrays.packedLength(numberOfBits);
    }

    /**
     * Returns the same result as {@link #packedLength(long) packedLength(numberOfBits)} as
     * a 32-bit <code>int</code> value, or throws {@link TooLargeArrayException} if
     * that result is greater than <code>Integer.MAX_VALUE</code>.
     *
     * @param numberOfBits the number of bits (the length of bit array).
     * @return <code>(numberOfBits + 63) &gt;&gt;&gt; 6</code>
     * (the length of corresponding <code>long[]</code> array).
     * @throws IllegalArgumentException if the argument is negative.
     * @throws TooLargeArrayException   if <code>numberOfBits &ge; 2<sup>37</sup></code>.
     */
    public static int packedLength32(long numberOfBits) {
        return PackedBitArrays.packedLength32(numberOfBits);
    }

    /**
     * Equivalent of {@link #packedLength(long)} for <code>int</code> argument.
     *
     * @param numberOfBits the number of bits (the length of bit array).
     * @return <code>(numberOfBits + 63) &gt;&gt;&gt; 6</code>
     * (the length of corresponding <code>long[]</code> array).
     */
    public static int packedLength32(int numberOfBits) {
        return PackedBitArrays.packedLength32(numberOfBits);
    }

    /**
     * Returns <code>buffer.hasArray()?buffer.array():buffer</code>.
     * This object is used by all methods of this class for synchronization, when any portion (not all 64 bits)
     * of some <code>long</code> element is modified.
     * Synchronization by <code>buffer.array()</code> (instead of <code>buffer</code> instance) allows
     * to use in different threads different instances of <code>LongBuffer</code>,
     * created by <code>LongBuffer.wrap</code>
     * method for the same Java <code>long[]</code> array.
     *
     * @param buffer the buffer.
     * @return this buffer if it is not backed by a Java array, the underlying Java array if it is backed by it.
     */
    public static Object getLock(LongBuffer buffer) {
        return buffer.hasArray() ? buffer.array() : buffer;
    }

    /**
     * Returns the bit <code>#index</code> in the packed <code>src</code> bit buffer.
     * Equivalent to the following expression:<pre>
     * (src.get((int)(index &gt;&gt;&gt; 6)) &amp; (1L &lt;&lt; (index &amp; 63))) != 0L;
     * </pre>
     *
     * @param src   the source buffer (bits are packed into <code>long</code> values).
     * @param index index of the returned bit.
     * @return the bit at the specified index.
     * @throws IndexOutOfBoundsException if this method cause access of data outside buffer limits.
     * @throws NullPointerException      if <code>src</code> is {@code null}.
     */
    public static boolean getBit(LongBuffer src, long index) {
        return (src.get((int) (index >>> 6)) & (1L << (index & 63))) != 0L;
    }

    /**
     * Sets the bit <code>#index</code> in the packed <code>dest</code> bit buffer.
     * Equivalent to the following operators:<pre>
     * synchronized ({@link #getLock PackedBitBuffers.getLock}(dest)) {
     * &#32;   if (value)
     * &#32;       dest.put((int)(index &gt;&gt;&gt; 6),
     * &#32;           dest.get((int)(index &gt;&gt;&gt; 6)) | 1L &lt;&lt; (index &amp; 63));
     * &#32;   else
     * &#32;       dest.put((int)(index &gt;&gt;&gt; 6),
     * &#32;           dest.get((int)(index &gt;&gt;&gt; 6)) &amp; ~(1L &lt;&lt; (index &amp; 63)));
     * }
     * </pre>
     *
     * @param dest  the destination buffer (bits are packed into <code>long</code> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws IndexOutOfBoundsException if this method cause access of data outside buffer limit.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     */
    public static void setBit(LongBuffer dest, long index, boolean value) {
        synchronized (getLock(dest)) {
            if (value)
                dest.put((int) (index >>> 6), dest.get((int) (index >>> 6)) | 1L << (index & 63));
            else
                dest.put((int) (index >>> 6), dest.get((int) (index >>> 6)) & ~(1L << (index & 63)));
        }
    }

    /**
     * Sets the bit <code>#index</code> in the packed <code>dest</code> bit buffer <i>without synchronization</i>.
     * May be used instead of {@link #setBit(LongBuffer, long, boolean)}, if you are not planning to call
     * this method from different threads for the same <code>dest</code> array.
     * Equivalent to the following operators:<pre>
     * &#32;   if (value)
     * &#32;       dest.put((int)(index &gt;&gt;&gt; 6),
     * &#32;           dest.get((int)(index &gt;&gt;&gt; 6)) | 1L &lt;&lt; (index &amp; 63));
     * &#32;   else
     * &#32;       dest.put((int)(index &gt;&gt;&gt; 6),
     * &#32;           dest.get((int)(index &gt;&gt;&gt; 6)) &amp; ~(1L &lt;&lt; (index &amp; 63)));
     * }
     * </pre>
     *
     * @param dest  the destination buffer (bits are packed into <code>long</code> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws IndexOutOfBoundsException if this method cause access of data outside buffer limit.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     */
    public static void setBitNoSync(LongBuffer dest, long index, boolean value) {
        if (value)
            dest.put((int) (index >>> 6), dest.get((int) (index >>> 6)) | 1L << (index & 63));
        else
            dest.put((int) (index >>> 6), dest.get((int) (index >>> 6)) & ~(1L << (index & 63)));
    }

    /**
     * Returns the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#srcPos</code>,
     * in the packed <code>src</code> bit buffer.
     *
     * <p>More precisely, the bit <code>#(srcPos+k)</code> will be returned in the bit <code>#k</code> of the returned
     * <code>long</code> value <code>R</code>: the first bit <code>#srcPos</code> will be equal
     * to <code>R&amp;1</code>,
     * the following bit <code>#(srcPos+1)</code> will be equal to <code>(R&gt;&gt;1)&amp;1</code>, etc.
     * If <code>count=0</code>, the result is 0.</p>
     *
     * <p>The same result can be calculated using the following loop
     * (for correct <code>count</code> in the range 0..64):</p>
     *
     * <pre>
     *      long result = 0;
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = {@link #getBit(LongBuffer, long) PackedBitBuffers.getBit}(src, srcPos + k) ? 1L : 0L;
     *          result |= bit &lt;&lt; k;
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <code>count</code> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the buffer (<code>&ge;64*src.limit()</code>);
     * instead, all bits outside the buffer are considered zero.
     * (But negative indexes are not allowed.)</p>
     *
     * @param src    the source buffer (bits are packed into <code>long</code> values).
     * @param srcPos position of the first bit read in the source buffer.
     * @param count  the number of bits to be unpacked (must be &gt;=0 and &lt;64).
     * @return the sequence of <code>count</code> bits.
     * @throws NullPointerException      if <code>src</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if <code>srcPos &lt; 0</code> or
     *                                   if copying would cause access of data outside buffer limits.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static long getBits64(LongBuffer src, long srcPos, int count) {
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
        final long srcPosDiv64 = srcPos >>> 6;
        final int length = src.limit();
        if (count == 0 || srcPosDiv64 >= length) {
            return 0;
        }
        int sPosRem = (int) (srcPos & 63);
        int sPos = (int) srcPosDiv64;
        int bitsLeft = 64 - sPosRem;
        // Below is a simplified implementation of PackedBitArraysPer8.getBits for a case of maximum 2 iterations
        if (count > bitsLeft) {
            final long actualBitsLow = src.get(sPos) >>> sPosRem;
            sPos++;
            if (sPos >= length) {
                return actualBitsLow;
            }
            final long actualBitsHigh = src.get(sPos) & (-1L >>> 64 - (count - bitsLeft));
            return actualBitsLow | (actualBitsHigh << bitsLeft);
        } else {
            return (src.get(sPos) & (-1L >>> (bitsLeft - count))) >>> sPosRem;
        }
    }

    /**
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#destPos</code>,
     * in the packed <code>dest</code> bit buffer.
     * This is the reverse operation of {@link #getBits64(LongBuffer, long, int)}.
     *
     * <p>This function is equivalent to the following loop
     * (for correct <code>count</code> in the range 0..64):</p>
     *
     * <pre>
     *      for (int k = 0; k &lt; count; k++) {
     *          final long bit = (bits &gt;&gt;&gt; k) &amp; 1L;
     *          {@link #setBit(LongBuffer, long, boolean) PackedBitBuffers.setBit}(dest, destPos + k, bit != 0);
     *      }</pre>
     *
     * <p>But this function works significantly faster, if <code>count</code> is greater than 1.</p>
     *
     * <p>Note: unlike the loop listed above, this function does not throw exception for too large indexes of bits
     * after the end of the buffer (<code>&ge;64*dest.limit()</code>);
     * instead, extra bits outside the buffer are just ignored.
     * (But negative indexes are not allowed.)</p>
     *
     * @param dest    the destination buffer (bits are packed into <code>long</code> values).
     * @param destPos position of the first bit written in the destination buffer.
     * @param bits    sequence of new bits to be copied into the destination buffer.
     * @param count   the number of bits to be written (must be in range 0..64).
     * @throws NullPointerException      if <code>dest</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if <code>destPos &lt; 0</code> or
     *                                   if copying would cause access of data outside buffer limits.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static void setBits64(LongBuffer dest, long destPos, long bits, int count) {
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
        final long destPosDiv64 = destPos >>> 6;
        final int length = dest.limit();
        if (count == 0 || destPosDiv64 >= length) {
            return;
        }
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = count;
            maskStart &= (1L << (dPosRem + count)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        int dPos = (int) destPosDiv64;
        synchronized (getLock(dest)) {
            if (cntStart > 0) {
                dest.put(dPos, ((bits << dPosRem) & maskStart) | (dest.get(dPos) & ~maskStart));
                dPos++;
                count -= cntStart;
                bits >>>= cntStart;
            }
            if (count > 0 && dPos < length) {
                long maskFinish = (2L << (count - 1)) - 1; // count times 1 (from the left)
                dest.put(dPos, (bits & maskFinish) | (dest.get(dPos) & ~maskFinish));
            }
        }
    }

    /**
     * Sets the sequence of <code>count</code> bits (maximum 64 bits), starting from the bit <code>#destPos</code>,
     * in the packed <code>dest</code> bit buffer <i>without synchronization</i>.
     * May be used instead of {@link #setBits64(LongBuffer, long, long, int)}, if you are not planning to call
     * this method from different threads for the same <code>dest</code> buffer.
     *
     * @param dest    the destination buffer (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination buffer.
     * @param bits    sequence of new bits to be copied into the destination buffer.
     * @param count   the number of bits to be written (must be in range 0..64).
     * @throws NullPointerException      if <code>dest</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if <code>destPos &lt; 0</code> or
     *                                   if copying would cause access of data outside buffer limits.
     * @throws IllegalArgumentException  if <code>count &lt; 0</code> or <code>count &gt; 64</code>.
     */
    public static void setBits64NoSync(LongBuffer dest, long destPos, long bits, int count) {
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
        final long destPosDiv64 = destPos >>> 6;
        final int length = dest.limit();
        if (count == 0 || destPosDiv64 >= length) {
            return;
        }
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = count;
            maskStart &= (1L << (dPosRem + count)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        int dPos = (int) destPosDiv64;
        if (cntStart > 0) {
            dest.put(dPos, ((bits << dPosRem) & maskStart) | (dest.get(dPos) & ~maskStart));
            dPos++;
            count -= cntStart;
            bits >>>= cntStart;
        }
        if (count > 0 && dPos < length) {
            long maskFinish = (2L << (count - 1)) - 1; // count times 1 (from the left)
            dest.put(dPos, (bits & maskFinish) | (dest.get(dPos) & ~maskFinish));
        }
    }


    /**
     * Copies <code>count</code> bits, packed into <code>src</code> buffer, starting from the bit <code>#srcPos</code>,
     * to packed <code>dest</code> buffer, starting from the bit <code>#destPos</code>.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * bits at positions <code>srcPos..srcPos+count-1</code> in <code>src</code>
     * were first unpacked to a temporary <code>boolean[]</code> array with <code>count</code> elements
     * and then the contents of the temporary array were packed into positions
     * <code>destPos..destPos+count-1</code> of <code>dest</code>.
     *
     * @param dest    the destination <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param destPos position of the first bit written in the destination buffer.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    public static void copyBits(LongBuffer dest, long destPos, LongBuffer src, long srcPos, long count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyBits(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> bits, packed into <code>src</code> buffer, starting from the bit <code>#srcPos</code>,
     * to packed <code>dest</code> buffer, starting from the bit <code>#destPos</code>,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies bits in normal order:
     * bit <code>#srcPos</code> of <code>src</code> to bit <code>#destPos</code> of <code>dest</code>, then
     * bit <code>#srcPos+1</code> of <code>src</code> to bit <code>#destPos+1</code> of <code>dest</code>, then
     * bit <code>#srcPos+2</code> of <code>src</code> to bit <code>#destPos+2</code> of <code>dest</code>, ..., then
     * bit <code>#srcPos+count-1</code> of <code>src</code> to bit <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies bits in reverse order:
     * bit <code>#srcPos+count-1</code> of <code>src</code> to
     * bit <code>#destPos+count-1</code> of <code>dest</code>, then
     * bit <code>#srcPos+count-2</code> of <code>src</code> to
     * bit <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * bit <code>#srcPos</code> of <code>src</code> to bit <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyBits(LongBuffer, long, LongBuffer, long, long)}
     * method that chooses the suitable order automatically.
     *
     * @param dest         the destination <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param destPos      position of the first bit written in the destination buffer.
     * @param src          the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos       position of the first bit read in the source buffer.
     * @param count        the number of bits to be copied (must be &gt;=0).
     * @param reverseOrder if <code>true</code>, the bits will be copied in the reverse order.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @see #copyBits(LongBuffer, long, LongBuffer, long, long)
     */
    public static void copyBits(
            LongBuffer dest,
            long destPos,
            LongBuffer src,
            long srcPos,
            long count,
            boolean reverseOrder) {
        /*Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, copyBits_method_impl)
          dest\[([^\]]+)\]\s*=(\s*)([^;]*) ==> dest.put($1,$2$3);;
          (src|dest)\[([^\]]+)\] ==> $1.get($2);;
          (synchronized\s*\()(\s*\w+\s*)\) ==> $1getLock($2));;
          \/\/Start_reverseOrder.*?\/\/End_reverseOrder.*?(?:\r(?!\n)|\n|\r\n)\s+\{ ==>
          if (reverseOrder) {;;
          \/\/Start_nothingToDo.*?\/\/End_nothingToDo.*?(\r(?!\n)|\n|\r\n) ==> $1;;
          \/\/Start_sPrev.*?\/\/End_sPrev.*?(\r(?!\n)|\n|\r\n) ==> sPrev = cnt == 0 ? 0 : src.get(sPos);
                    // Unlike PackedBitArrays.copyBits, IndexOutOfBoundException is possible here when count=0,
                    // because the reverseOrder=true argument may be passed in this case $1;;
          System\.arraycopy\((\w+,\s*\w+,\s*)(\w+,\s*\w+,\s*)(\w+)\) ==>
          JBuffers.copyLongBuffer($2$1$3, reverseOrder)   !! Auto-generated: NOT EDIT !!*/
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (reverseOrder) {
            // overlap possible
            if (sPosRem == dPosRem) {

                final int sPosStart = sPos;
                final int dPosStart = dPos;
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    count -= cntStart;
                    dPos++;
                    sPos++;
                }
                int cnt = (int) (count >>> 6);
                int cntFinish = (int) (count & 63);
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    synchronized (getLock(dest)) {
                        dest.put(dPos + cnt,
                                (src.get(sPos + cnt) & maskFinish) |
                                        (dest.get(dPos + cnt) & ~maskFinish));
                    }
                }
                JBuffers.copyLongBuffer(dest, dPos, src, sPos, cnt, reverseOrder);
                if (cntStart > 0) {
                    synchronized (getLock(dest)) {
                        dest.put(dPosStart,
                                (src.get(sPosStart) & maskStart) |
                                        (dest.get(dPosStart) & ~maskStart));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                        sPosRem += cntStart;
                    } else {
                        sPos++;
                        sPosRem = (sPosRem + cntStart) & 63;
                    }
                    // we suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                    count -= cntStart;
                    dPos++;
                }
                // Now the bit #0 of dest.get(dPos) corresponds to the bit #sPosRem of src.get(sPos)
                int cnt = (int) (count >>> 6);
                final long dPosMin = dPos;
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 63);
                final int sPosRem64 = 64 - sPosRem;
                long sPrev;
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    long v;
                    if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                        v = (sPrev = src.get(sPos)) >>> sPosRem;
                    } else {
                        v = ((sPrev = src.get(sPos)) >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                    }
                    synchronized (getLock(dest)) {
                        dest.put(dPos, (v & maskFinish) | (dest.get(dPos) & ~maskFinish));
                    }
                } else {
                    sPrev = cnt == 0 ? 0 : src.get(sPos);
                    // Unlike PackedBitArrays.copyBits, IndexOutOfBoundException is possible here when count=0,
                    // because the reverseOrder=true argument may be passed in this case
                }
                while (dPos > dPosMin) { // cnt times
                    --sPos;
                    --dPos;
                    dest.put(dPos, (sPrev << sPosRem64) | ((sPrev = src.get(sPos)) >>> sPosRem));
                }
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    long v;
                    if (sPosRemStart + cntStart <= 64) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = src.get(sPosStart) << shift;
                        else
                            v = src.get(sPosStart) >>> -shift;
                    } else {
                        v = (src.get(sPosStart) >>> -shift) | (src.get(sPosStart + 1) << (64 + shift));
                    }
                    synchronized (getLock(dest)) {
                        dest.put(dPosStart, (v & maskStart) | (dest.get(dPosStart) & ~maskStart));
                    }
                }
            }
        } else {
            // usual case
            if (sPosRem == dPosRem) {
                if (cntStart > 0) {
                    synchronized (getLock(dest)) {
                        dest.put(dPos, (src.get(sPos) & maskStart) | (dest.get(dPos) & ~maskStart));
                    }
                    count -= cntStart;
                    dPos++;
                    sPos++;
                }
                int cnt = (int) (count >>> 6);
                JBuffers.copyLongBuffer(dest, dPos, src, sPos, cnt, reverseOrder);
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 63);
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    synchronized (getLock(dest)) {
                        dest.put(dPos, (src.get(sPos) & maskFinish) | (dest.get(dPos) & ~maskFinish));
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                long sNext;
                if (cntStart > 0) {
                    long v;
                    if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (sNext = src.get(sPos)) << shift;
                        else
                            v = (sNext = src.get(sPos)) >>> -shift;
                        sPosRem += cntStart;
                    } else {
                        v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                        sPos++;
                        sPosRem = (sPosRem + cntStart) & 63;
                    }
                    // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                    synchronized (getLock(dest)) {
                        dest.put(dPos, (v & maskStart) | (dest.get(dPos) & ~maskStart));
                    }
                    count -= cntStart;
                    if (count == 0) {
                        return; // little optimization
                    }
                    dPos++;
                } else {
                    if (count == 0) {
                        return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                    }
                    sNext = src.get(sPos);
                }
                // Now the bit #0 of dest.get(dPos) corresponds to the bit #sPosRem of src.get(sPos)
                final int sPosRem64 = 64 - sPosRem;
                for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                    sPos++;
                    dest.put(dPos, (sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64));
                    dPos++;
                }
                int cntFinish = (int) (count & 63);
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    long v;
                    if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                        v = sNext >>> sPosRem;
                    } else {
                        v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                    }
                    synchronized (getLock(dest)) {
                        dest.put(dPos, (v & maskFinish) | (dest.get(dPos) & ~maskFinish));
                    }
                }
            }
        }
        /*Repeat.IncludeEnd*/
    }

    /**
     * Swaps <code>count</code> bits, packed into <code>first</code> buffer,
     * starting from the bit <code>#firstPos</code>,
     * with <code>count</code> bits, packed into <code>second</code> buffer,
     * starting from the bit <code>#secondPos</code>.
     *
     * <p>Some bits may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * @param first     the first <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param firstPos  starting index of bit to exchange in the first <code>LongBuffer</code>.
     * @param second    the second <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param secondPos starting index of bit to exchange in the second <code>LongBuffer</code>.
     * @param count     the number of bits to be exchanged (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    public static void swapBits(LongBuffer first, long firstPos, LongBuffer second, long secondPos, long count) {
        Objects.requireNonNull(first, "Null first");
        Objects.requireNonNull(second, "Null second");
        for (long firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            synchronized (getLock(first)) {
                synchronized (getLock(second)) {
                    boolean v1 = (first.get((int) (firstPos >>> 6)) & (1L << (firstPos & 63))) != 0L;
                    boolean v2 = (second.get((int) (secondPos >>> 6)) & (1L << (secondPos & 63))) != 0L;
                    if (v1 != v2) {
                        if (v2)
                            first.put((int) (firstPos >>> 6),
                                    first.get((int) (firstPos >>> 6)) | 1L << (firstPos & 63));
                        else
                            first.put((int) (firstPos >>> 6),
                                    first.get((int) (firstPos >>> 6)) & ~(1L << (firstPos & 63)));
                        if (v1)
                            second.put((int) (secondPos >>> 6),
                                    second.get((int) (secondPos >>> 6)) | 1L << (secondPos & 63));
                        else
                            second.put((int) (secondPos >>> 6),
                                    second.get((int) (secondPos >>> 6)) & ~(1L << (secondPos & 63)));
                    }
                }
            }
        }
    }

    /**
     * Packs <code>count</code> bits from <code>src</code> array, starting from the element <code>#srcPos</code>,
     * to packed <code>dest</code> buffer, starting from the bit <code>#destPos</code>.
     *
     * @param dest    the destination <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param destPos position of the first bit written in the destination buffer.
     * @param src     the source array (unpacked <code>boolean</code> values).
     * @param srcPos  position of the first bit read in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds or buffer limit.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    public static void packBits(LongBuffer dest, long destPos, boolean[] src, int srcPos, int count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (getLock(dest)) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest.put((int) (destPos >>> 6), dest.get((int) (destPos >>> 6)) | 1L << (destPos & 63));
                    else
                        dest.put((int) (destPos >>> 6), dest.get((int) (destPos >>> 6)) & ~(1L << (destPos & 63)));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] ? 1 : 0)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 1 << $INDEX(start=2) : 0) ,, ...(30);;
//           1\s<<\s+31 ==> 0x80000000,, ... ]]
                    | (src[srcPos + 1] ? 1 << 1 : 0)
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    | (src[srcPos + 2] ? 1 << 2 : 0)
                    | (src[srcPos + 3] ? 1 << 3 : 0)
                    | (src[srcPos + 4] ? 1 << 4 : 0)
                    | (src[srcPos + 5] ? 1 << 5 : 0)
                    | (src[srcPos + 6] ? 1 << 6 : 0)
                    | (src[srcPos + 7] ? 1 << 7 : 0)
                    | (src[srcPos + 8] ? 1 << 8 : 0)
                    | (src[srcPos + 9] ? 1 << 9 : 0)
                    | (src[srcPos + 10] ? 1 << 10 : 0)
                    | (src[srcPos + 11] ? 1 << 11 : 0)
                    | (src[srcPos + 12] ? 1 << 12 : 0)
                    | (src[srcPos + 13] ? 1 << 13 : 0)
                    | (src[srcPos + 14] ? 1 << 14 : 0)
                    | (src[srcPos + 15] ? 1 << 15 : 0)
                    | (src[srcPos + 16] ? 1 << 16 : 0)
                    | (src[srcPos + 17] ? 1 << 17 : 0)
                    | (src[srcPos + 18] ? 1 << 18 : 0)
                    | (src[srcPos + 19] ? 1 << 19 : 0)
                    | (src[srcPos + 20] ? 1 << 20 : 0)
                    | (src[srcPos + 21] ? 1 << 21 : 0)
                    | (src[srcPos + 22] ? 1 << 22 : 0)
                    | (src[srcPos + 23] ? 1 << 23 : 0)
                    | (src[srcPos + 24] ? 1 << 24 : 0)
                    | (src[srcPos + 25] ? 1 << 25 : 0)
                    | (src[srcPos + 26] ? 1 << 26 : 0)
                    | (src[srcPos + 27] ? 1 << 27 : 0)
                    | (src[srcPos + 28] ? 1 << 28 : 0)
                    | (src[srcPos + 29] ? 1 << 29 : 0)
                    | (src[srcPos + 30] ? 1 << 30 : 0)
                    | (src[srcPos + 31] ? 0x80000000 : 0)
//[[Repeat.AutoGeneratedEnd]]
                    ;
            srcPos += 32;
            int high = (src[srcPos] ? 1 : 0)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 1 << $INDEX(start=2) : 0) ,, ...(30);;
//           1\s<<\s+31 ==> 0x80000000,, ... ]]
                    | (src[srcPos + 1] ? 1 << 1 : 0)
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    | (src[srcPos + 2] ? 1 << 2 : 0)
                    | (src[srcPos + 3] ? 1 << 3 : 0)
                    | (src[srcPos + 4] ? 1 << 4 : 0)
                    | (src[srcPos + 5] ? 1 << 5 : 0)
                    | (src[srcPos + 6] ? 1 << 6 : 0)
                    | (src[srcPos + 7] ? 1 << 7 : 0)
                    | (src[srcPos + 8] ? 1 << 8 : 0)
                    | (src[srcPos + 9] ? 1 << 9 : 0)
                    | (src[srcPos + 10] ? 1 << 10 : 0)
                    | (src[srcPos + 11] ? 1 << 11 : 0)
                    | (src[srcPos + 12] ? 1 << 12 : 0)
                    | (src[srcPos + 13] ? 1 << 13 : 0)
                    | (src[srcPos + 14] ? 1 << 14 : 0)
                    | (src[srcPos + 15] ? 1 << 15 : 0)
                    | (src[srcPos + 16] ? 1 << 16 : 0)
                    | (src[srcPos + 17] ? 1 << 17 : 0)
                    | (src[srcPos + 18] ? 1 << 18 : 0)
                    | (src[srcPos + 19] ? 1 << 19 : 0)
                    | (src[srcPos + 20] ? 1 << 20 : 0)
                    | (src[srcPos + 21] ? 1 << 21 : 0)
                    | (src[srcPos + 22] ? 1 << 22 : 0)
                    | (src[srcPos + 23] ? 1 << 23 : 0)
                    | (src[srcPos + 24] ? 1 << 24 : 0)
                    | (src[srcPos + 25] ? 1 << 25 : 0)
                    | (src[srcPos + 26] ? 1 << 26 : 0)
                    | (src[srcPos + 27] ? 1 << 27 : 0)
                    | (src[srcPos + 28] ? 1 << 28 : 0)
                    | (src[srcPos + 29] ? 1 << 29 : 0)
                    | (src[srcPos + 30] ? 1 << 30 : 0)
                    | (src[srcPos + 31] ? 0x80000000 : 0)
//[[Repeat.AutoGeneratedEnd]]
                    ;
            srcPos += 32;
            dest.put(k, ((long) low & 0xFFFFFFFFL) | (((long) high) << 32));
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (getLock(dest)) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest.put((int) (destPos >>> 6), dest.get((int) (destPos >>> 6)) | 1L << (destPos & 63));
                    else
                        dest.put((int) (destPos >>> 6), dest.get((int) (destPos >>> 6)) & ~(1L << (destPos & 63)));
                }
            }
        }
    }

    /**
     * Unpacks <code>count</code> bits, packed into <code>src</code> buffer,
     * starting from the bit <code>#srcPos</code>,
     * to <code>dest</code> array, starting from the element <code>#destPos</code>.
     *
     * @param dest    the destination array (unpacked <code>boolean</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds or buffer limit.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    public static void unpackBits(boolean[] dest, int destPos, LongBuffer src, long srcPos, int count) {
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src.get((int) (srcPos >>> 6)) & (1L << (srcPos & 63))) != 0L;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            long value = src.get(k);
            int low = (int) value;
            int high = (int) (value >>> 32);
            srcPos += 64;
//[[Repeat() dest\[.*?0; ==>
//           dest[destPos + $INDEX(start=1)] = (low & (1 << $INDEX(start=1))) != 0; ,, ...(31);;
//           1\s<<\s+31 ==> 0x80000000,, ... ]]
            dest[destPos] = (low & 1) != 0;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 1] = (low & (1 << 1)) != 0;
            dest[destPos + 2] = (low & (1 << 2)) != 0;
            dest[destPos + 3] = (low & (1 << 3)) != 0;
            dest[destPos + 4] = (low & (1 << 4)) != 0;
            dest[destPos + 5] = (low & (1 << 5)) != 0;
            dest[destPos + 6] = (low & (1 << 6)) != 0;
            dest[destPos + 7] = (low & (1 << 7)) != 0;
            dest[destPos + 8] = (low & (1 << 8)) != 0;
            dest[destPos + 9] = (low & (1 << 9)) != 0;
            dest[destPos + 10] = (low & (1 << 10)) != 0;
            dest[destPos + 11] = (low & (1 << 11)) != 0;
            dest[destPos + 12] = (low & (1 << 12)) != 0;
            dest[destPos + 13] = (low & (1 << 13)) != 0;
            dest[destPos + 14] = (low & (1 << 14)) != 0;
            dest[destPos + 15] = (low & (1 << 15)) != 0;
            dest[destPos + 16] = (low & (1 << 16)) != 0;
            dest[destPos + 17] = (low & (1 << 17)) != 0;
            dest[destPos + 18] = (low & (1 << 18)) != 0;
            dest[destPos + 19] = (low & (1 << 19)) != 0;
            dest[destPos + 20] = (low & (1 << 20)) != 0;
            dest[destPos + 21] = (low & (1 << 21)) != 0;
            dest[destPos + 22] = (low & (1 << 22)) != 0;
            dest[destPos + 23] = (low & (1 << 23)) != 0;
            dest[destPos + 24] = (low & (1 << 24)) != 0;
            dest[destPos + 25] = (low & (1 << 25)) != 0;
            dest[destPos + 26] = (low & (1 << 26)) != 0;
            dest[destPos + 27] = (low & (1 << 27)) != 0;
            dest[destPos + 28] = (low & (1 << 28)) != 0;
            dest[destPos + 29] = (low & (1 << 29)) != 0;
            dest[destPos + 30] = (low & (1 << 30)) != 0;
            dest[destPos + 31] = (low & (0x80000000)) != 0;
//[[Repeat.AutoGeneratedEnd]]
//[[Repeat() dest\[.*?0; ==>
//           dest[destPos + $INDEX(start=33)] = (high & (1 << $INDEX(start=1))) != 0; ,, ...(31);;
//           1\s<<\s+31 ==> 0x80000000,, ... ]]
            dest[destPos + 32] = (high & 0x1) != 0;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 33] = (high & (1 << 1)) != 0;
            dest[destPos + 34] = (high & (1 << 2)) != 0;
            dest[destPos + 35] = (high & (1 << 3)) != 0;
            dest[destPos + 36] = (high & (1 << 4)) != 0;
            dest[destPos + 37] = (high & (1 << 5)) != 0;
            dest[destPos + 38] = (high & (1 << 6)) != 0;
            dest[destPos + 39] = (high & (1 << 7)) != 0;
            dest[destPos + 40] = (high & (1 << 8)) != 0;
            dest[destPos + 41] = (high & (1 << 9)) != 0;
            dest[destPos + 42] = (high & (1 << 10)) != 0;
            dest[destPos + 43] = (high & (1 << 11)) != 0;
            dest[destPos + 44] = (high & (1 << 12)) != 0;
            dest[destPos + 45] = (high & (1 << 13)) != 0;
            dest[destPos + 46] = (high & (1 << 14)) != 0;
            dest[destPos + 47] = (high & (1 << 15)) != 0;
            dest[destPos + 48] = (high & (1 << 16)) != 0;
            dest[destPos + 49] = (high & (1 << 17)) != 0;
            dest[destPos + 50] = (high & (1 << 18)) != 0;
            dest[destPos + 51] = (high & (1 << 19)) != 0;
            dest[destPos + 52] = (high & (1 << 20)) != 0;
            dest[destPos + 53] = (high & (1 << 21)) != 0;
            dest[destPos + 54] = (high & (1 << 22)) != 0;
            dest[destPos + 55] = (high & (1 << 23)) != 0;
            dest[destPos + 56] = (high & (1 << 24)) != 0;
            dest[destPos + 57] = (high & (1 << 25)) != 0;
            dest[destPos + 58] = (high & (1 << 26)) != 0;
            dest[destPos + 59] = (high & (1 << 27)) != 0;
            dest[destPos + 60] = (high & (1 << 28)) != 0;
            dest[destPos + 61] = (high & (1 << 29)) != 0;
            dest[destPos + 62] = (high & (1 << 30)) != 0;
            dest[destPos + 63] = (high & (0x80000000)) != 0;
//[[Repeat.AutoGeneratedEnd]]
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src.get((int) (srcPos >>> 6)) & (1L << (srcPos & 63))) != 0L;
        }
    }

    /**
     * Fills <code>count</code> bits in the packed <code>dest</code> buffer, starting from
     * the bit <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * @param dest    the destination <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param destPos position of the first bit written in the destination buffer.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<code>false</code> means the bit 0,
     *                <code>true</code> means the bit 1).
     * @throws IndexOutOfBoundsException if filling would cause access of data outside buffer limit.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     */
    public static void fillBits(LongBuffer dest, long destPos, long count, boolean value) {
        Objects.requireNonNull(dest, "Null dest");
        int dPos = (int) (destPos >>> 6);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (cntStart > 0) {
            synchronized (getLock(dest)) {
                if (value)
                    dest.put(dPos, dest.get(dPos) | maskStart);
                else
                    dest.put(dPos, dest.get(dPos) & ~maskStart);
            }
            count -= cntStart;
            dPos++;
        }
        long longValue = value ? -1 : 0;
        for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++) {
            dest.put(dPos, longValue);
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            synchronized (getLock(dest)) {
                if (value)
                    dest.put(dPos, dest.get(dPos) | maskFinish);
                else
                    dest.put(dPos, dest.get(dPos) & ~maskFinish);
            }
        }
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical NOT of corresponding <code>count</code> bits,
     * packed in <code>src</code> buffer, starting from the bit <code>#srcPos</code>.
     * The packed <code>long[]</code> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void notBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, notBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = (~src.get(sPos) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = ~src.get(sPos);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (~src.get(sPos) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src.get(sPos)) << shift;
                    else
                        v = (sNext = src.get(sPos)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 63;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = (~v & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                }
                sNext = src.get(sPos);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src.get(sPos)
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = ~((sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64));
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = (~v & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical AND of them and corresponding <code>count</code> bits,
     * packed in <code>src</code> buffer, starting from the bit <code>#srcPos</code>.
     * The packed <code>long[]</code> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void andBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, andBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)    !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] &= src.get(sPos) | ~maskStart;
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] &= src.get(sPos);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] &= src.get(sPos) | ~maskFinish;
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src.get(sPos)) << shift;
                    else
                        v = (sNext = src.get(sPos)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 63;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] &= v | ~maskStart;
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                }
                sNext = src.get(sPos);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src.get(sPos)
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] &= (sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64);
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] &= v | ~maskFinish;
                }
            }
        }
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical OR of them and corresponding <code>count</code> bits,
     * packed in <code>src</code> buffer, starting from the bit <code>#srcPos</code>.
     * The packed <code>long[]</code> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void orBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, orBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
//      System.out.println((sPosRem == dPosRem ? "AORGOOD " : "AORBAD  ") + sPosRem + "," + dPosRem);
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] |= src.get(sPos) & maskStart;
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] |= src.get(sPos);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] |= src.get(sPos) & maskFinish;
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src.get(sPos)) << shift;
                    else
                        v = (sNext = src.get(sPos)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 63;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] |= v & maskStart;
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                }
                sNext = src.get(sPos);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src.get(sPos)
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] |= (sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64);
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] |= v & maskFinish;
                }
            }
        }
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical XOR of them and corresponding <code>count</code> bits,
     * packed in <code>src</code> buffer, starting from the bit <code>#srcPos</code>.
     * The packed <code>long[]</code> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void xorBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, xorBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] ^ src.get(sPos)) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] ^= src.get(sPos);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] ^ src.get(sPos)) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src.get(sPos)) << shift;
                    else
                        v = (sNext = src.get(sPos)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 63;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] ^ v) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                }
                sNext = src.get(sPos);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src.get(sPos)
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] ^= (sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64);
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] ^ v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical AND of them and <i>inverted</i> corresponding <code>count</code> bits,
     * packed in <code>src</code> buffer, starting from the bit <code>#srcPos</code>.
     * The packed <code>long[]</code> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void andNotBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, andNotBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] & ~src.get(sPos)) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] &= ~src.get(sPos);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] & ~src.get(sPos)) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src.get(sPos)) << shift;
                    else
                        v = (sNext = src.get(sPos)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 63;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] & ~v) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                }
                sNext = src.get(sPos);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src.get(sPos)
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] &= ~((sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64));
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] & ~v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Replaces <code>count</code> bits,
     * packed in <code>dest</code> array, starting from the bit <code>#destPos</code>,
     * with the logical OR of them and <i>inverted</i> corresponding <code>count</code> bits,
     * packed in <code>src</code> buffer, starting from the bit <code>#srcPos</code>.
     * The packed <code>long[]</code> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <code>long</code> values).
     * @param destPos position of the first bit written in the destination array.
     * @param src     the source <code>LongBuffer</code> (bits are packed into <code>long</code> values).
     * @param srcPos  position of the first bit read in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void orNotBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, orNotBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(dest, "Null dest");
        Objects.requireNonNull(src, "Null src");
        int sPos = (int) (srcPos >>> 6);
        int dPos = (int) (destPos >>> 6);
        int sPosRem = (int) (srcPos & 63);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
//      System.out.println((sPosRem == dPosRem ? "AORGOOD " : "AORBAD  ") + sPosRem + "," + dPosRem);
        if (sPosRem == dPosRem) {
            if (cntStart > 0) {
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] | ~src.get(sPos)) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] |= ~src.get(sPos);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] | ~src.get(sPos)) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src.get(sPos)) << shift;
                    else
                        v = (sNext = src.get(sPos)) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src.get(sPos) >>> -shift) | ((sNext = src.get(sPos + 1)) << (64 + shift));
                    sPos++;
                    sPosRem = (sPosRem + cntStart) & 63;
                }
                // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] | ~v) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                if (count == 0) {
                    return; // little optimization
                }
                dPos++;
            } else {
                if (count == 0) {
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src.get(sPos)
                }
                sNext = src.get(sPos);
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src.get(sPos)
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] |= ~((sNext >>> sPosRem) | ((sNext = src.get(sPos)) << sPosRem64));
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src.get(sPos + 1) << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] | ~v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and the bit <code>#k</code> in the packed <code>src</code> bit buffer is equal to <code>value</code>,
     * or <code>-1</code> if there is no such bits.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param src       the searched packed bit buffer.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of bit to be found.
     * @return the index of the first occurrence of this bit in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this bit does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws NullPointerException      if <code>buffer</code> is {@code null}.
     * @throws IndexOutOfBoundsException if <code>lowIndex</code> is negative or
     *                                   if <code>highIndex</code> is greater than <code>src.limit()*64</code>.
     * @see #lastIndexOfBit(LongBuffer, long, long, boolean)
     */
    public static long indexOfBit(LongBuffer src, long lowIndex, long highIndex, boolean value) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, indexOfBit_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1);;
        //  src\.length ==> src.limit()  !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(src, "Null src");
        if (lowIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: low index = " + lowIndex);
        }
        if (highIndex > ((long) src.limit()) << 6) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: high index = " + highIndex);
        }
        if (lowIndex >= highIndex) {
            return -1;
        }
        long count = highIndex - lowIndex;
        int sPos = (int) (lowIndex >>> 6);
        int sPosRem = (int) (lowIndex & 63);
        int cntStart = (-sPosRem) & 63;
        long fromAligned = lowIndex - sPosRem;
        long maskStart = -1L << sPosRem; // sPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (sPosRem + cntStart)) - 1; // &= sPosRem+cntStart times 1 (from the left)
        }
        if (cntStart > 0) {
            int index = Long.numberOfTrailingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskStart);
            if (index != 64)
                return fromAligned + index;
            count -= cntStart;
            sPos++;
            fromAligned += 64;
        }
        if (value) {
            for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; sPos++, fromAligned += 64) {
                int index = Long.numberOfTrailingZeros(src.get(sPos));
                if (index != 64) {
                    return fromAligned + index;
                }
            }
        } else {
            for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; sPos++, fromAligned += 64) {
                int index = Long.numberOfTrailingZeros(~src.get(sPos));
                if (index != 64) {
                    return fromAligned + index;
                }
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            int index = Long.numberOfTrailingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskFinish);
            if (index != 64) {
                return fromAligned + index;
            }
        }
        return -1;
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and the bit <code>#k</code> in the packed <code>src</code> bit buffer is equal to <code>value</code>,
     * or <code>-1</code> if there is no such bits.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfBit(LongBuffer, long, long, boolean)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param src       the searched packed bit buffer.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of bit to be found.
     * @return the index of the last occurrence of this bit in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this bit does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws NullPointerException      if <code>src</code> is {@code null}.
     * @throws IndexOutOfBoundsException if <code>lowIndex</code> is negative or
     *                                   if <code>highIndex</code> is greater than <code>src.length*64</code>.
     */
    public static long lastIndexOfBit(LongBuffer src, long lowIndex, long highIndex, boolean value) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, lastIndexOfBit_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1);;
        //  src\.length ==> src.limit()   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(src, "Null src");
        if (lowIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: low index = " + lowIndex);
        }
        if (highIndex > ((long) src.limit()) << 6) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: high index = " + highIndex);
        }
        if (lowIndex >= highIndex) {
            return -1;
        }
        long count = highIndex - lowIndex;
        int sPos = (int) ((highIndex - 1) >>> 6);
        int cntStart = ((int) highIndex) & 63;
        long fromAligned = ((highIndex - 1) & ~63) + 63; // 64k+63
        long maskStart = (1L << cntStart) - 1; // cntStart times 1 (from the left)
        if (cntStart > count) {
            maskStart &= -1L << (cntStart - count); // &= cntStart-count times 0, then 1 (from the left)
            cntStart = (int) count;
        }
        if (cntStart > 0) {
            int index = Long.numberOfLeadingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskStart);
            if (index != 64) {
                return fromAligned - index;
            }
            count -= cntStart;
            sPos--;
            fromAligned -= 64;
        }
        if (value) {
            for (int sPosMin = sPos - (int) (count >>> 6); sPos > sPosMin; sPos--, fromAligned -= 64) {
                int index = Long.numberOfLeadingZeros(src.get(sPos));
                if (index != 64) {
                    return fromAligned - index;
                }
            }
        } else {
            for (int sPosMin = sPos - (int) (count >>> 6); sPos > sPosMin; sPos--, fromAligned -= 64) {
                int index = Long.numberOfLeadingZeros(~src.get(sPos));
                if (index != 64) {
                    return fromAligned - index;
                }
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = -1L << (64 - cntFinish); // cntFinish times 1 (from the right)
            int index = Long.numberOfLeadingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskFinish);
            if (index != 64) {
                return fromAligned - index;
            }
        }
        return -1;
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Returns the number of high bits (1) in the given fragment of the given packed bit buffer.
     *
     * @param src       the source packed bit buffer.
     * @param fromIndex the initial checked bit index in <code>src</code>, inclusive.
     * @param toIndex   the end checked bit index in <code>src</code>, exclusive.
     * @return the number of high bits (1) in the given fragment of the given packed bit buffer.
     * @throws NullPointerException      if the <code>src</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if <code>fromIndex</code> or <code>toIndex</code> are negative,
     *                                   if <code>toIndex</code> is greater than <code>src.limit() * 64</code>,
     *                                   or if <code>fromIndex</code> is greater than <code>startIndex</code>
     */
    public static long cardinality(LongBuffer src, long fromIndex, long toIndex) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, cardinality_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1);;
        //  src\.length ==> src.limit()   !! Auto-generated: NOT EDIT !! >>
        Objects.requireNonNull(src, "Null src argument in cardinality method");
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex);
        }
        if (toIndex > ((long) src.limit()) << 6) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: end index = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex
                    + " > end index = " + toIndex);
        }
        long count = toIndex - fromIndex;

        int sPos = (int) (fromIndex >>> 6);
        int sPosRem = (int) (fromIndex & 63);
        int cntStart = (-sPosRem) & 63;
        long maskStart = -1L << sPosRem; // sPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (sPosRem + cntStart)) - 1; // &= sPosRem+cntStart times 1 (from the left)
        }
        long result = 0;
        if (cntStart > 0) {
            result += Long.bitCount(src.get(sPos) & maskStart);
            count -= cntStart;
            sPos++;
        }
        final int sPosMax = sPos + (int) (count >>> 6);
        result += bitCount(src, sPos, sPosMax);
        sPos = sPosMax;
        /*
        // The loop below is the 64-bit version of the algorithm published in
        // "Hacker's Delight" by Henry S. Warren, figure 5-4,
        // Addison-Wesley Publishing Company, Inc., 2002.
        // In new JVM, this is usually a bad idea, because
        // Long.bitCount is an intrinsic candidate and works faster on most CPU.
        for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; ) {
            long s8 = 0;
            for (int lim = Math.min(sPosMax, sPos + 31); sPos < lim; sPos++) {
                // maximal intermediate sums (bit counts in bytes) are 8,
                // so 31 iterations do not lead to overflow (8*31<256)
                long value = src.get(sPos);
                value -= (value >>> 1) & 0x5555555555555555L;
                value = (value & 0x3333333333333333L) + ((value >>> 2) & 0x3333333333333333L);
                s8 += (value + (value >>> 4)) & 0x0F0F0F0F0F0F0F0FL;
            }
            s8 = (s8 & 0x00FF00FF00FF00FFL) + ((s8 >>> 8) & 0x00FF00FF00FF00FFL);
            s8 = (s8 & 0x0000FFFF0000FFFFL) + ((s8 >>> 16) & 0x0000FFFF0000FFFFL);
            result += ((int) s8 + (int) (s8 >>> 32)) & 0xFFFF;

        }
        */
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            result += Long.bitCount(src.get(sPos) & maskFinish);
        }
        return result;
        //<<Repeat.IncludeEnd>>
    }

    static long bitCount(LongBuffer src, int from, int to) {
        long result = 0;
        for (int i = from; i < to; i++) {
            result += Long.bitCount(src.get(i));
        }
        return result;
    }
}
