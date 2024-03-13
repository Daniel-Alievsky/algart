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
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * <p>Operations with bit arrays packed into <tt>long[]</tt> Java arrays.</p>
 *
 * <p>AlgART bits arrays, created by {@link SimpleMemoryModel},
 * are based on operations provided by this class.</p>
 *
 * <p>The maximal length of bit arrays supported by this class is <tt>2<sup>37</sup>-64</tt>.
 * All indexes and lengths passed to methods of this class must not exceed this value.
 * Moreover, all indexes and length, concerning usual (non-packed) Java array,
 * must not exceed <tt>2<sup>31</sup>-1</tt>. In other case, the results are unspecified.
 * ("Unspecified" means that any elements of the passed arrays can be read or changed,
 * or that <tt>IndexOutOfBoundsException</tt> can be thrown.)</p>
 *
 * <p>In all methods of this class, it's supposed that the bit <tt>#k</tt> in a packed <tt>long[] array</tt>
 * is the bit <tt>#(k%64)</tt> in the long element <tt>array[k/64]</tt>. In other words, the bit <tt>#k</tt>
 * (<tt>false</tt> or <tt>true</tt> value) can be extracted by the following operator:</p>
 *
 * <pre>
 * (array[k &gt;&gt;&gt; 6] &amp; (1L &lt;&lt; (k &amp; 63))) != 0L
 * </pre>
 *
 * <p>and can be set or cleared by the following operators:</p>
 *
 * <pre>
 * if (newValue) // we need to set bit #k to 1
 * &#32;   array[k &gt;&gt;&gt; 6] |= 1L &lt;&lt; (k &amp; 63);
 * else          // we need to clear bit #k to 0
 * &#32;   array[k &gt;&gt;&gt; 6] &amp;= ~(1L &lt;&lt; (k &amp; 63));
 * </pre>
 *
 * <p>If any method of this class modifies some portion of an element of a packed <tt>long[]</tt> Java array,
 * i.e. modifies less than all 64 its bits, then all accesses to this <tt>long</tt> element are performed
 * <b>inside a single synchronized block</b>, using the following instruction:</p>
 *
 * <pre>
 * synchronized (array) {
 * &#32;   // accessing to some element array[k]
 * }
 * </pre>
 *
 * <p>(See an example in comments to {@link #setBit} method.)
 * If all 64 bits of the element are written, or if the bits are read only, then no synchronization is performed.
 * Such behavior allows to simultaneously work with non-overlapping fragments of a packed bit array
 * from several threads (different fragments for different threads), as if it would be a usual Java array.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitArrays {
    private PackedBitArrays() {
    }

    /*Repeat.SectionStart primitives*/

    /**
     * Returns <tt>(unpackedLength + 63) &gt;&gt;&gt; 6</tt>: the minimal number of <tt>long</tt> values
     * allowing to store <tt>unpackedLength</tt> bits.
     *
     * @param unpackedLength the number of bits (the length of bit array).
     * @return <tt>(unpackedLength + 63) &gt;&gt;&gt; 6</tt> (the length of corresponding <tt>long[]</tt> array).
     */
    public static long packedLength(long unpackedLength) {
        return (unpackedLength + 63) >>> 6;
        // here >>> must be used instead of >>, because unpackedLength+63 may be >Long.MAX_VALUE
    }

    /**
     * Returns the bit <tt>#index</tt> in the packed <tt>src</tt> bit array.
     * Equivalent to the following expression:<pre>
     * (src[(int)(index &gt;&gt;&gt; 6)] &amp; (1L &lt;&lt; (index &amp; 63))) != 0L;
     * </pre>
     *
     * @param src   the source array (bits are packed in <tt>long</tt> values).
     * @param index index of the returned bit.
     * @return the bit at the specified index.
     * @throws NullPointerException      if <tt>src</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static boolean getBit(long[] src, long index) {
        return (src[(int) (index >>> 6)] & (1L << (index & 63))) != 0L;
    }

    /**
     * Sets the bit <tt>#index</tt> in the packed <tt>dest</tt> bit array.
     * Equivalent to the following operators:<pre>
     * synchronized (dest) {
     * &#32;   if (value)
     * &#32;       dest[(int)(index &gt;&gt;&gt; 6)] |= 1L &lt;&lt; (index &amp; 63);
     * &#32;   else
     * &#32;       dest[(int)(index &gt;&gt;&gt; 6)] &amp;= ~(1L &lt;&lt; (index &amp; 63));
     * }
     * </pre>
     *
     * @param dest  the destination array (bits are packed in <tt>long</tt> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     */
    public static void setBit(long[] dest, long index, boolean value) {
        synchronized (dest) {
            if (value)
                dest[(int) (index >>> 6)] |= 1L << (index & 63);
            else
                dest[(int) (index >>> 6)] &= ~(1L << (index & 63));
        }
    }
    /*Repeat.SectionEnd primitives*/

    public static long[] toLongArray(byte[] byteArray) {
        Objects.requireNonNull(byteArray, "Null byte[] array");
        final long[] result = new long[(byteArray.length + 7) >>> 3];
        final ByteBuffer bb = ByteBuffer.wrap(byteArray);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.asLongBuffer().get(result);
        return result;
    }

    public static long[] toLongArray(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null ByteBuffer");
        ByteBuffer bb = byteBuffer.duplicate();
        final long[] result = new long[(bb.limit() + 7) >>> 3];
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.rewind();
        bb.asLongBuffer().get(result);
        return result;

    }

    /**
     * Returns a hash code based on the contents of the specified fragment of the given packed bit array.
     * If the passed array is <tt>null</tt> or <tt>fromIndex==toIndex</tt>, returns 0.
     *
     * <p>The returned hash code depends only on the sequence of packed bits, but does not depend
     * on the position of this sequence in the passed <tt>long[]</tt> array.
     *
     * <p>For any two packed bit arrays <tt>a1</tt> and <tt>a2</tt> such that
     * <tt>PackedBitArrays.bitEquals(a1, pos1, a2, pos2, count)</tt>, it is also the case that
     * <tt>PackedBitArrays.bitHashCode(a1, pos1, pos1 + count) ==
     * PackedBitArrays.bitHashCode(a2, pos2, pos2 + count)</tt>.
     *
     * @param array     the packed bit array whose content-based hash code to compute.
     * @param fromIndex the initial index of the checked fragment, inclusive.
     * @param toIndex   the end index of the checked fragment, exclusive.
     * @return a content-based hash code for the specified fragment in <tt>array</tt>.
     * @throws IllegalArgumentException  if the <tt>array</tt> argument is not a Java array.
     * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> or <tt>toIndex</tt> are negative,
     *                                   if <tt>toIndex</tt> is greater than <tt>array.length*64</tt>
     *                                   (0 if <tt>array==null</tt>),
     *                                   or if <tt>fromIndex</tt> is greater than <tt>startIndex</tt>,
     *                                   or if <tt>array==null</tt> and not <tt>fromIndex==toIndex==0</tt>
     * @see #bitEquals(long[], long, long[], long, long)
     * @see JArrays#arrayHashCode(Object, int, int)
     */
    public static int bitHashCode(long[] array, long fromIndex, long toIndex) {
        Checksum sum = new CRC32();
        updateBitHashCode(array, fromIndex, toIndex, sum);
        return fromIndex == toIndex ? 0 : (int) sum.getValue();
    }

    /**
     * Updates hash code (<tt>hash</tt> argument) on the base of the contents
     * of the specified fragment of the given packed bit array.
     * If the passed array is <tt>null</tt> or <tt>fromIndex==toIndex</tt>, does nothing.
     *
     * <p>This method is used by {@link #bitHashCode(long[], long, long)
     * bitHashCode(long[] array, long fromIndex, long toIndex)}.
     * More precisely, that method is equivalent to:<pre>
     * Checksum sum = new CRC32();
     * updateBitHashCode(array, fromIndex, toIndex, sum);
     * return fromIndex == toIndex ? 0 : (int)sum.getValue();
     * </pre>
     *
     * <p>The following 2 code fragment always produce the same results in <tt>hash</tt>argument:<pre>
     * updateBitHashCode(arr, fromIndex, toIndex, hash);
     * </pre>and<pre>
     * updateBitHashCode(arr, fromIndex, k1, hash);
     * updateBitHashCode(arr, k1, k2, hash);
     * ...
     * updateBitHashCode(arr, kN, toIndex, hash);
     * </pre>
     * where <tt>fromIndex &lt;= k1 &lt;= k2 &lt;= ... &lt;= kN &lt;= toIndex</tt>.
     * So, unlike <tt>bitHashCode</tt>, this method allows to calculate correct hash code
     * of a long array when we cannot get all its element at the same time,
     * but can get sequent portions ot it.
     *
     * @param array     the packed bit array whose content-based hash code to compute.
     * @param fromIndex the initial index of the checked fragment, inclusive.
     * @param toIndex   the end index of the checked fragment, exclusive.
     * @param hash      updated hash code.
     * @throws NullPointerException      if <tt>array</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> or <tt>toIndex</tt> are negative,
     *                                   if <tt>toIndex</tt> is greater than <tt>array.length</tt>
     *                                   (0 if <tt>array==null</tt>),
     *                                   or if <tt>fromIndex</tt> is greater than <tt>startIndex</tt>,
     *                                   or if <tt>array==null</tt> and not <tt>fromIndex==toIndex==0</tt>
     */
    public static void updateBitHashCode(long[] array, long fromIndex, long toIndex, Checksum hash) {
        Objects.requireNonNull(hash, "Null hash argument");
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex);
        }
        if (fromIndex > toIndex) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex
                    + " > end index = " + toIndex);
        }
        if (array == null) {
            if (toIndex > 0)
                throw new ArrayIndexOutOfBoundsException("Bit array index out of range for null array: end index = "
                        + toIndex);
        } else {
            if (toIndex > ((long) array.length) << 6)
                throw new ArrayIndexOutOfBoundsException("Bit array index out of range: end index = " + toIndex);
        }
        if (fromIndex == toIndex) { // in particular, if array == null
            return;
        }
        boolean[] bits = new boolean[4096];
        // Important: we must produce the same hash code for every placement in the packed array!
        // So, good optimization, based on processing long values, is difficult here.
        for (long pos = fromIndex; pos < toIndex; pos += bits.length) {
            int len = (int) Math.min(bits.length, toIndex - pos);
            unpackBits(bits, 0, array, pos, len);
            JArrays.updateArrayHashCode(bits, 0, len, hash);
        }
    }

    /**
     * Returns <tt>true</tt> if the specified fragments of the given packed bit arrays are equals,
     * or if both arguments are <tt>null</tt>.
     * Returns <tt>false</tt> if one of the arguments is <tt>null</tt>, but the other is not <tt>null</tt>.
     *
     * <p>The two packed bit arrays are considered equal if all corresponding pairs of bits
     * in the two arrays are equal.
     *
     * @param array1 one array to be tested for equality.
     * @param pos1   the initial index of the checked fragment in the first array.
     * @param array2 the other array to be tested for equality.
     * @param pos2   the initial index of the checked fragment in the second array.
     * @param length the number of compared elements.
     * @return <tt>true</tt> if the specified fragments of two arrays are equal.
     * @throws IllegalArgumentException  if the <tt>array1</tt> or <tt>array2</tt> argument is not a Java array.
     * @throws IndexOutOfBoundsException if <tt>pos1</tt>, <tt>pos2</tt> or <tt>length</tt> are negative,
     *                                   if <tt>pos1 + length</tt> is greater than <tt>array1.length*64</tt>
     *                                   (0 if <tt>array1==null</tt>),
     *                                   or if <tt>pos2 + length</tt> is greater than <tt>array2.length*64</tt>
     *                                   (0 if <tt>array2==null</tt>).
     */
    public static boolean bitEquals(long[] array1, long pos1, long[] array2, long pos2, long length) {
        long length1 = array1 == null ? 0 : ((long) array1.length) << 6;
        long length2 = array2 == null ? 0 : ((long) array2.length) << 6;
        if (pos1 < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative bit array initial index: pos1 = " + pos1);
        }
        if (pos2 < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative bit array initial index: pos2 = " + pos2);
        }
        if (length < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative number of compared elements: length = " + length);
        }
        if (pos1 + length > length1) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: pos1 + length = "
                    + (pos1 + length));
        }
        if (pos2 + length > length2) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: pos2 + length = "
                    + (pos2 + length));
        }
        if (array1 == array2) {
            return array1 == null || pos1 == pos2;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        if (length == 0) {
            return true;
        }
        if ((pos1 & 63) == (pos2 & 63)) {
            int first1 = (int) (pos1 >>> 6);
            int first2 = (int) (pos2 >>> 6);
            int last1 = (int) ((pos1 + length - 1) >>> 6);
            int last2 = (int) ((pos2 + length - 1) >>> 6);
            if (first1 == last1) {
                // so, length < 64
                long mask = ~((1L << (pos1 & 63)) - 1); // all bits pos1&63..63: correct even if (pos1&63)==0
                // here pos1 + length > 0: it can be zero only if length==0, that was checked above
                int ofs = (int) ((pos1 + length) & 63);
                if (ofs != 0)
                    mask &= (1L << ofs) - 1; // all bits 0..((pos1&63)+length)-1: incorrect if (pos1&63)+length==64
                return (array1[first1] & mask) == (array2[first2] & mask);
            }
            if ((pos1 & 63) != 0) {
                long fromMask = ~((1L << (pos1 & 63)) - 1); // all bits pos1&63..63
                if ((array1[first1] & fromMask) != (array2[first2] & fromMask)) {
                    return false;
                }
                first1++;
                first2++;
            }
            if (((pos1 + length) & 63) != 0) {
                long toMask = (1L << ((pos1 + length) & 63)) - 1; // all bits 0..((pos1+length)&63)-1
                if ((array1[last1] & toMask) != (array2[last2] & toMask)) {
                    return false;
                }
                last1--;
                last2--;
            }
            if (first1 <= last1) {
                if (!JArrays.arrayEquals(array1, first1, array2, first2, last1 - first1 + 1)) {
                    return false;
                }
            }
            return true;
        } else {
            for (long pos1Max = pos1 + length; pos1 < pos1Max; pos1++, pos2++) {
                if (((array1[(int) (pos1 >>> 6)] >>> (pos1 & 63)) & 1L) !=
                        ((array2[(int) (pos2 >>> 6)] >>> (pos2 & 63)) & 1L)) {
                    return false;
                }
            }
            return true;
        }
    }

    /*Repeat.SectionStart copyBits*/

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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart copyBits_method_impl]]
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
                int cnt = (int) (count >>> 6);
                int cntFinish = (int) (count & 63);
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    synchronized (dest) {
                        dest[dPos + cnt] =
                                (src[sPos + cnt] & maskFinish) | (dest[dPos + cnt] & ~maskFinish);
                    }
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPosStart] =
                                (src[sPosStart] & maskStart) | (dest[dPosStart] & ~maskStart);
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                final int sPosStart = sPos;
                final int dPosStart = dPos;
                final int sPosRemStart = sPosRem;
                final long dPosMin;
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
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                int cnt = (int) (count >>> 6);
                dPosMin = dPos;
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 63);
                final int sPosRem64 = 64 - sPosRem;
                long sPrev;
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    long v;
                    if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                        v = (sPrev = src[sPos]) >>> sPosRem;
                    } else {
                        v = ((sPrev = src[sPos]) >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                    }
                    synchronized (dest) {
                        dest[dPos] = (v & maskFinish) | (dest[dPos] & ~maskFinish);
                    }
                } else {
                    //Start_sPrev !! this comment is necessary for preprocessing by Repeater !!
                    sPrev = src[sPos];
                    // IndexOutOfBoundException is impossible here, because there is one of the following situations:
                    // 1) cnt > 0, then src[sPos] is really necessary in the following loop;
                    // 2) cnt == 0 and cntStart > 0, then src[sPos] will be necessary for making dest[dPosStart].
                    // All other situations are impossible here:
                    // 3) cntFinish > 0: it was processed above in "if (cntFinish > 0)..." branch;
                    // 4) cntStart == 0, cntFinish == 0 and cnt == 0, i.e. count == 0: it's impossible
                    // in this branch of all algorithm (overlap is impossible when count == 0).
                    //End_sPrev !! this comment is necessary for preprocessing by Repeater !!
                }
                for (; dPos > dPosMin; ) { // cnt times
                    --sPos;
                    --dPos;
                    dest[dPos] = (sPrev << sPosRem64) | ((sPrev = src[sPos]) >>> sPosRem);
                }
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    long v;
                    if (sPosRemStart + cntStart <= 64) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = src[sPosStart] << shift;
                        else
                            v = src[sPosStart] >>> -shift;
                    } else {
                        v = (src[sPosStart] >>> -shift) | (src[sPosStart + 1] << (64 + shift));
                    }
                    synchronized (dest) {
                        dest[dPosStart] = (v & maskStart) | (dest[dPosStart] & ~maskStart);
                    }
                }
            }
        } else {
            // usual case
            if (sPosRem == dPosRem) {
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPos] = (src[sPos] & maskStart) | (dest[dPos] & ~maskStart);
                    }
                    count -= cntStart;
                    dPos++;
                    sPos++;
                }
                int cnt = (int) (count >>> 6);
                System.arraycopy(src, sPos, dest, dPos, cnt);
                sPos += cnt;
                dPos += cnt;
                int cntFinish = (int) (count & 63);
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    synchronized (dest) {
                        dest[dPos] = (src[sPos] & maskFinish) | (dest[dPos] & ~maskFinish);
                    }
                }
            } else {
                final int shift = dPosRem - sPosRem;
                long sNext;
                if (cntStart > 0) {
                    long v;
                    if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = (sNext = src[sPos]) << shift;
                        else
                            v = (sNext = src[sPos]) >>> -shift;
                        sPosRem += cntStart;
                    } else {
                        v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
                        sPos++;
                        sPosRem = (sPosRem + cntStart) & 63;
                    }
                    // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
                    synchronized (dest) {
                        dest[dPos] = (v & maskStart) | (dest[dPos] & ~maskStart);
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
                    sNext = src[sPos];
                }
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                final int sPosRem64 = 64 - sPosRem;
                for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                    sPos++;
                    dest[dPos] = (sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64);
                    dPos++;
                }
                int cntFinish = (int) (count & 63);
                if (cntFinish > 0) {
                    long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                    long v;
                    if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                        v = sNext >>> sPosRem;
                    } else {
                        v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                    }
                    synchronized (dest) {
                        dest[dPos] = (v & maskFinish) | (dest[dPos] & ~maskFinish);
                    }
                }
            }
        }
        //[[Repeat.SectionEnd copyBits_method_impl]]
    }
    /*Repeat.SectionEnd copyBits*/

    /**
     * Copies <tt>count</tt> bits from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (unpacked <tt>boolean</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBits(long[] dest, long destPos, boolean[] src, int srcPos, int count) {
        //[[Repeat.SectionStart packBits_method_impl]]
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] ? 1 : 0)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 1 << $INDEX(start=2) : 0) ,, ...(30)]]
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
                    | (src[srcPos + 31] ? 1 << 31 : 0)
//[[Repeat.AutoGeneratedEnd]]
                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] ? 1 : 0)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 1 << $INDEX(start=2) : 0) ,, ...(30)]]
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
                    | (src[srcPos + 31] ? 1 << 31 : 0)
//[[Repeat.AutoGeneratedEnd]]
                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        //[[Repeat.SectionEnd packBits_method_impl]]
    }

    /**
     * Copies <tt>count</tt> <i>inverted</i> bits from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (unpacked inverted <tt>boolean</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsInverted(long[] dest, long destPos, boolean[] src, int srcPos, int count) {
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                    else
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] ? 0 : 1)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 0 : 1 << $INDEX(start=2)) ,, ...(30)]]
                    | (src[srcPos + 1] ? 0 : 1 << 1)
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    | (src[srcPos + 2] ? 0 : 1 << 2)
                    | (src[srcPos + 3] ? 0 : 1 << 3)
                    | (src[srcPos + 4] ? 0 : 1 << 4)
                    | (src[srcPos + 5] ? 0 : 1 << 5)
                    | (src[srcPos + 6] ? 0 : 1 << 6)
                    | (src[srcPos + 7] ? 0 : 1 << 7)
                    | (src[srcPos + 8] ? 0 : 1 << 8)
                    | (src[srcPos + 9] ? 0 : 1 << 9)
                    | (src[srcPos + 10] ? 0 : 1 << 10)
                    | (src[srcPos + 11] ? 0 : 1 << 11)
                    | (src[srcPos + 12] ? 0 : 1 << 12)
                    | (src[srcPos + 13] ? 0 : 1 << 13)
                    | (src[srcPos + 14] ? 0 : 1 << 14)
                    | (src[srcPos + 15] ? 0 : 1 << 15)
                    | (src[srcPos + 16] ? 0 : 1 << 16)
                    | (src[srcPos + 17] ? 0 : 1 << 17)
                    | (src[srcPos + 18] ? 0 : 1 << 18)
                    | (src[srcPos + 19] ? 0 : 1 << 19)
                    | (src[srcPos + 20] ? 0 : 1 << 20)
                    | (src[srcPos + 21] ? 0 : 1 << 21)
                    | (src[srcPos + 22] ? 0 : 1 << 22)
                    | (src[srcPos + 23] ? 0 : 1 << 23)
                    | (src[srcPos + 24] ? 0 : 1 << 24)
                    | (src[srcPos + 25] ? 0 : 1 << 25)
                    | (src[srcPos + 26] ? 0 : 1 << 26)
                    | (src[srcPos + 27] ? 0 : 1 << 27)
                    | (src[srcPos + 28] ? 0 : 1 << 28)
                    | (src[srcPos + 29] ? 0 : 1 << 29)
                    | (src[srcPos + 30] ? 0 : 1 << 30)
                    | (src[srcPos + 31] ? 0 : 1 << 31)
//[[Repeat.AutoGeneratedEnd]]
                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] ? 0 : 1)
//[[Repeat() \([^\)]*\) ==> (src[srcPos + $INDEX(start=2)] ? 0 : 1 << $INDEX(start=2)) ,, ...(30)]]
                    | (src[srcPos + 1] ? 0 : 1 << 1)
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    | (src[srcPos + 2] ? 0 : 1 << 2)
                    | (src[srcPos + 3] ? 0 : 1 << 3)
                    | (src[srcPos + 4] ? 0 : 1 << 4)
                    | (src[srcPos + 5] ? 0 : 1 << 5)
                    | (src[srcPos + 6] ? 0 : 1 << 6)
                    | (src[srcPos + 7] ? 0 : 1 << 7)
                    | (src[srcPos + 8] ? 0 : 1 << 8)
                    | (src[srcPos + 9] ? 0 : 1 << 9)
                    | (src[srcPos + 10] ? 0 : 1 << 10)
                    | (src[srcPos + 11] ? 0 : 1 << 11)
                    | (src[srcPos + 12] ? 0 : 1 << 12)
                    | (src[srcPos + 13] ? 0 : 1 << 13)
                    | (src[srcPos + 14] ? 0 : 1 << 14)
                    | (src[srcPos + 15] ? 0 : 1 << 15)
                    | (src[srcPos + 16] ? 0 : 1 << 16)
                    | (src[srcPos + 17] ? 0 : 1 << 17)
                    | (src[srcPos + 18] ? 0 : 1 << 18)
                    | (src[srcPos + 19] ? 0 : 1 << 19)
                    | (src[srcPos + 20] ? 0 : 1 << 20)
                    | (src[srcPos + 21] ? 0 : 1 << 21)
                    | (src[srcPos + 22] ? 0 : 1 << 22)
                    | (src[srcPos + 23] ? 0 : 1 << 23)
                    | (src[srcPos + 24] ? 0 : 1 << 24)
                    | (src[srcPos + 25] ? 0 : 1 << 25)
                    | (src[srcPos + 26] ? 0 : 1 << 26)
                    | (src[srcPos + 27] ? 0 : 1 << 27)
                    | (src[srcPos + 28] ? 0 : 1 << 28)
                    | (src[srcPos + 29] ? 0 : 1 << 29)
                    | (src[srcPos + 30] ? 0 : 1 << 30)
                    | (src[srcPos + 31] ? 0 : 1 << 31)
//[[Repeat.AutoGeneratedEnd]]
                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos])
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                    else
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                }
            }
        }
    }

    /*Repeat() char(\s+threshold)      ==> int$1,,int$1,,int$1,,long$1,,float$1,,double$1;;
               char                    ==> byte,,short,,int,,long,,float,,double;;
               (src\[[^]]*])(?!\<\/tt) ==> ($1 & 0xFF),,($1 & 0xFFFF),,$1,,...
     */

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>char</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, char[] src, int srcPos, int count,
            char threshold) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, packBits_method_impl)
        //  (src\[.*?]) ==> $1 > threshold !! Auto-generated: NOT EDIT !! ]]
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>char</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, char[] src, int srcPos, int count,
            char threshold) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, packBits_method_impl)
        //  (src\[.*?]) ==> $1 < threshold !! Auto-generated: NOT EDIT !! ]]
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>char</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, char[] src, int srcPos, int count,
            char threshold) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, packBits_method_impl)
        //  (src\[.*?]) ==> $1 >= threshold !! Auto-generated: NOT EDIT !! ]]
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>char</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, char[] src, int srcPos, int count,
            char threshold) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, packBits_method_impl)
        //  (src\[.*?]) ==> $1 <= threshold !! Auto-generated: NOT EDIT !! ]]
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        //[[Repeat.IncludeEnd]]
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFF) > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>byte</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, byte[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFF) > threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) > threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) > threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) > threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) > threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) > threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) > threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) > threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) > threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) > threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) > threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) > threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) > threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) > threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) > threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) > threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) > threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) > threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) > threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) > threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) > threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) > threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) > threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) > threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) > threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) > threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) > threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) > threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) > threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) > threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) > threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFF) > threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) > threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) > threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) > threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) > threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) > threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) > threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) > threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) > threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) > threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) > threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) > threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) > threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) > threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) > threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) > threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) > threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) > threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) > threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) > threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) > threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) > threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) > threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) > threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) > threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) > threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) > threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) > threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) > threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) > threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) > threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFF) < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>byte</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, byte[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFF) < threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) < threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) < threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) < threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) < threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) < threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) < threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) < threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) < threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) < threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) < threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) < threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) < threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) < threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) < threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) < threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) < threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) < threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) < threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) < threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) < threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) < threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) < threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) < threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) < threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) < threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) < threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) < threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) < threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) < threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) < threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFF) < threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) < threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) < threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) < threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) < threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) < threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) < threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) < threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) < threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) < threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) < threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) < threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) < threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) < threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) < threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) < threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) < threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) < threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) < threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) < threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) < threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) < threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) < threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) < threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) < threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) < threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) < threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) < threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) < threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) < threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) < threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFF) >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>byte</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, byte[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFF) >= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) >= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) >= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) >= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) >= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) >= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) >= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) >= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) >= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) >= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) >= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) >= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) >= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) >= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) >= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) >= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) >= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) >= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) >= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) >= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) >= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) >= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) >= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) >= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) >= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) >= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) >= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) >= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) >= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) >= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) >= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFF) >= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) >= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) >= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) >= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) >= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) >= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) >= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) >= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) >= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) >= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) >= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) >= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) >= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) >= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) >= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) >= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) >= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) >= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) >= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) >= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) >= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) >= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) >= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) >= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) >= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) >= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) >= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) >= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) >= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) >= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) >= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFF) <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>byte</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, byte[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFF) <= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) <= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) <= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) <= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) <= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) <= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) <= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) <= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) <= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) <= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) <= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) <= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) <= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) <= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) <= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) <= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) <= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) <= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) <= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) <= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) <= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) <= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) <= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) <= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) <= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) <= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) <= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) <= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) <= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) <= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) <= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFF) <= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFF) <= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFF) <= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFF) <= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFF) <= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFF) <= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFF) <= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFF) <= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFF) <= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFF) <= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFF) <= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFF) <= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFF) <= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFF) <= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFF) <= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFF) <= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFF) <= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFF) <= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFF) <= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFF) <= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFF) <= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFF) <= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFF) <= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFF) <= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFF) <= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFF) <= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFF) <= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFF) <= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFF) <= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFF) <= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFF) <= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFF) <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFF) <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }


    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFFFF) > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>short</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, short[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFFFF) > threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) > threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) > threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) > threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) > threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) > threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) > threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) > threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) > threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) > threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) > threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) > threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) > threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) > threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) > threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) > threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) > threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) > threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) > threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) > threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) > threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) > threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) > threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) > threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) > threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) > threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) > threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) > threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) > threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) > threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) > threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFFFF) > threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) > threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) > threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) > threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) > threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) > threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) > threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) > threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) > threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) > threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) > threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) > threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) > threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) > threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) > threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) > threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) > threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) > threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) > threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) > threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) > threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) > threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) > threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) > threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) > threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) > threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) > threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) > threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) > threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) > threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) > threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFFFF) < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>short</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, short[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFFFF) < threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) < threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) < threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) < threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) < threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) < threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) < threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) < threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) < threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) < threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) < threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) < threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) < threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) < threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) < threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) < threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) < threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) < threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) < threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) < threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) < threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) < threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) < threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) < threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) < threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) < threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) < threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) < threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) < threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) < threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) < threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFFFF) < threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) < threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) < threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) < threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) < threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) < threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) < threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) < threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) < threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) < threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) < threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) < threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) < threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) < threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) < threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) < threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) < threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) < threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) < threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) < threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) < threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) < threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) < threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) < threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) < threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) < threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) < threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) < threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) < threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) < threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) < threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFFFF) >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>short</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, short[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFFFF) >= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) >= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) >= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) >= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) >= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) >= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) >= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) >= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) >= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) >= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) >= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) >= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) >= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) >= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) >= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) >= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) >= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) >= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) >= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) >= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) >= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) >= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) >= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) >= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) >= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) >= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) >= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) >= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) >= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) >= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) >= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFFFF) >= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) >= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) >= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) >= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) >= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) >= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) >= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) >= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) >= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) >= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) >= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) >= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) >= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) >= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) >= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) >= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) >= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) >= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) >= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) >= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) >= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) >= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) >= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) >= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) >= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) >= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) >= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) >= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) >= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) >= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) >= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code (src[k] & 0xFFFF) <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>short</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, short[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = ((src[srcPos] & 0xFFFF) <= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) <= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) <= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) <= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) <= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) <= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) <= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) <= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) <= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) <= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) <= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) <= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) <= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) <= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) <= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) <= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) <= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) <= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) <= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) <= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) <= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) <= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) <= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) <= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) <= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) <= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) <= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) <= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) <= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) <= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) <= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = ((src[srcPos] & 0xFFFF) <= threshold ? 1 : 0)

                    | ((src[srcPos + 1] & 0xFFFF) <= threshold ? 1 << 1 : 0)

                    | ((src[srcPos + 2] & 0xFFFF) <= threshold ? 1 << 2 : 0)
                    | ((src[srcPos + 3] & 0xFFFF) <= threshold ? 1 << 3 : 0)
                    | ((src[srcPos + 4] & 0xFFFF) <= threshold ? 1 << 4 : 0)
                    | ((src[srcPos + 5] & 0xFFFF) <= threshold ? 1 << 5 : 0)
                    | ((src[srcPos + 6] & 0xFFFF) <= threshold ? 1 << 6 : 0)
                    | ((src[srcPos + 7] & 0xFFFF) <= threshold ? 1 << 7 : 0)
                    | ((src[srcPos + 8] & 0xFFFF) <= threshold ? 1 << 8 : 0)
                    | ((src[srcPos + 9] & 0xFFFF) <= threshold ? 1 << 9 : 0)
                    | ((src[srcPos + 10] & 0xFFFF) <= threshold ? 1 << 10 : 0)
                    | ((src[srcPos + 11] & 0xFFFF) <= threshold ? 1 << 11 : 0)
                    | ((src[srcPos + 12] & 0xFFFF) <= threshold ? 1 << 12 : 0)
                    | ((src[srcPos + 13] & 0xFFFF) <= threshold ? 1 << 13 : 0)
                    | ((src[srcPos + 14] & 0xFFFF) <= threshold ? 1 << 14 : 0)
                    | ((src[srcPos + 15] & 0xFFFF) <= threshold ? 1 << 15 : 0)
                    | ((src[srcPos + 16] & 0xFFFF) <= threshold ? 1 << 16 : 0)
                    | ((src[srcPos + 17] & 0xFFFF) <= threshold ? 1 << 17 : 0)
                    | ((src[srcPos + 18] & 0xFFFF) <= threshold ? 1 << 18 : 0)
                    | ((src[srcPos + 19] & 0xFFFF) <= threshold ? 1 << 19 : 0)
                    | ((src[srcPos + 20] & 0xFFFF) <= threshold ? 1 << 20 : 0)
                    | ((src[srcPos + 21] & 0xFFFF) <= threshold ? 1 << 21 : 0)
                    | ((src[srcPos + 22] & 0xFFFF) <= threshold ? 1 << 22 : 0)
                    | ((src[srcPos + 23] & 0xFFFF) <= threshold ? 1 << 23 : 0)
                    | ((src[srcPos + 24] & 0xFFFF) <= threshold ? 1 << 24 : 0)
                    | ((src[srcPos + 25] & 0xFFFF) <= threshold ? 1 << 25 : 0)
                    | ((src[srcPos + 26] & 0xFFFF) <= threshold ? 1 << 26 : 0)
                    | ((src[srcPos + 27] & 0xFFFF) <= threshold ? 1 << 27 : 0)
                    | ((src[srcPos + 28] & 0xFFFF) <= threshold ? 1 << 28 : 0)
                    | ((src[srcPos + 29] & 0xFFFF) <= threshold ? 1 << 29 : 0)
                    | ((src[srcPos + 30] & 0xFFFF) <= threshold ? 1 << 30 : 0)
                    | ((src[srcPos + 31] & 0xFFFF) <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if ((src[srcPos] & 0xFFFF) <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }


    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>int</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, int[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>int</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, int[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>int</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, int[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>int</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, int[] src, int srcPos, int count,
            int threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }


    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, long[] src, int srcPos, int count,
            long threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, long[] src, int srcPos, int count,
            long threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, long[] src, int srcPos, int count,
            long threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, long[] src, int srcPos, int count,
            long threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }


    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>float</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, float[] src, int srcPos, int count,
            float threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>float</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, float[] src, int srcPos, int count,
            float threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>float</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, float[] src, int srcPos, int count,
            float threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>float</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, float[] src, int srcPos, int count,
            float threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }


    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] > threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>double</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than this value are packed to unit bits (1),
     *                  the source elements less than or equal to this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreater(
            long[] dest, long destPos, double[] src, int srcPos, int count,
            double threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] > threshold ? 1 : 0)

                    | (src[srcPos + 1] > threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] > threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] > threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] > threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] > threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] > threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] > threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] > threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] > threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] > threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] > threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] > threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] > threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] > threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] > threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] > threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] > threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] > threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] > threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] > threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] > threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] > threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] > threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] > threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] > threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] > threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] > threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] > threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] > threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] > threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] > threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] > threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] < threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>double</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than to this value are packed to unit bits (1),
     *                  the source elements greater than or equal this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLess(
            long[] dest, long destPos, double[] src, int srcPos, int count,
            double threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] < threshold ? 1 : 0)

                    | (src[srcPos + 1] < threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] < threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] < threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] < threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] < threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] < threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] < threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] < threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] < threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] < threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] < threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] < threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] < threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] < threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] < threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] < threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] < threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] < threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] < threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] < threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] < threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] < threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] < threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] < threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] < threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] < threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] < threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] < threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] < threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] < threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] < threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] < threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] >= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>double</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are greater than or equal to this value are packed to unit bits (1),
     *                  the source elements less than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsGreaterOrEqual(
            long[] dest, long destPos, double[] src, int srcPos, int count,
            double threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] >= threshold ? 1 : 0)

                    | (src[srcPos + 1] >= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] >= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] >= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] >= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] >= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] >= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] >= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] >= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] >= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] >= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] >= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] >= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] >= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] >= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] >= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] >= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] >= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] >= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] >= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] >= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] >= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] >= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] >= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] >= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] >= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] >= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] >= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] >= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] >= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] >= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] >= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] >= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }

    /**
     * Packs <tt>count</tt> elements from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * so that every element <tt>src[k]</tt> is transformed to boolean (bit) value
     * <nobr>{@code src[k] <= threshold}</nobr>.
     *
     * @param dest      the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos   position of the first written bit in the destination array.
     * @param src       the source array (unpacked <tt>double</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of bits to be packed (must be &gt;=0).
     * @param threshold the source elements that are less than or equal to this value are packed to unit bits (1),
     *                  the source elements greater than this threshold are packed to zero bits (0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBitsLessOrEqual(
            long[] dest, long destPos, double[] src, int srcPos, int count,
            double threshold) {

        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int) (destPos & 63);
        if (countStart > count)
            countStart = count;
        if (countStart > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            int high = (src[srcPos] <= threshold ? 1 : 0)

                    | (src[srcPos + 1] <= threshold ? 1 << 1 : 0)

                    | (src[srcPos + 2] <= threshold ? 1 << 2 : 0)
                    | (src[srcPos + 3] <= threshold ? 1 << 3 : 0)
                    | (src[srcPos + 4] <= threshold ? 1 << 4 : 0)
                    | (src[srcPos + 5] <= threshold ? 1 << 5 : 0)
                    | (src[srcPos + 6] <= threshold ? 1 << 6 : 0)
                    | (src[srcPos + 7] <= threshold ? 1 << 7 : 0)
                    | (src[srcPos + 8] <= threshold ? 1 << 8 : 0)
                    | (src[srcPos + 9] <= threshold ? 1 << 9 : 0)
                    | (src[srcPos + 10] <= threshold ? 1 << 10 : 0)
                    | (src[srcPos + 11] <= threshold ? 1 << 11 : 0)
                    | (src[srcPos + 12] <= threshold ? 1 << 12 : 0)
                    | (src[srcPos + 13] <= threshold ? 1 << 13 : 0)
                    | (src[srcPos + 14] <= threshold ? 1 << 14 : 0)
                    | (src[srcPos + 15] <= threshold ? 1 << 15 : 0)
                    | (src[srcPos + 16] <= threshold ? 1 << 16 : 0)
                    | (src[srcPos + 17] <= threshold ? 1 << 17 : 0)
                    | (src[srcPos + 18] <= threshold ? 1 << 18 : 0)
                    | (src[srcPos + 19] <= threshold ? 1 << 19 : 0)
                    | (src[srcPos + 20] <= threshold ? 1 << 20 : 0)
                    | (src[srcPos + 21] <= threshold ? 1 << 21 : 0)
                    | (src[srcPos + 22] <= threshold ? 1 << 22 : 0)
                    | (src[srcPos + 23] <= threshold ? 1 << 23 : 0)
                    | (src[srcPos + 24] <= threshold ? 1 << 24 : 0)
                    | (src[srcPos + 25] <= threshold ? 1 << 25 : 0)
                    | (src[srcPos + 26] <= threshold ? 1 << 26 : 0)
                    | (src[srcPos + 27] <= threshold ? 1 << 27 : 0)
                    | (src[srcPos + 28] <= threshold ? 1 << 28 : 0)
                    | (src[srcPos + 29] <= threshold ? 1 << 29 : 0)
                    | (src[srcPos + 30] <= threshold ? 1 << 30 : 0)
                    | (src[srcPos + 31] <= threshold ? 1 << 31 : 0)

                    // - end of autogenerated 30 lines
                    ;
            srcPos += 32;
            dest[k] = ((long) low & 0xFFFFFFFFL) | (((long) high) << 32);
            destPos += 64;
        }
        int countFinish = count & 63;
        if (countFinish > 0) {
            synchronized (dest) {
                for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
                    if (src[srcPos] <= threshold)
                        dest[(int) (destPos >>> 6)] |= 1L << (destPos & 63);
                    else
                        dest[(int) (destPos >>> 6)] &= ~(1L << (destPos & 63));
                }
            }
        }

    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat.SectionStart unpackBits*/

    /**
     * Copies <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> boolean array, starting from the element <tt>#destPos</tt>.
     *
     * @param dest    the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(boolean[] dest, int destPos, long[] src, long srcPos, int count) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
//[[Repeat() dest\[.*?0; ==>
//           dest[destPos + $INDEX(start=1)] = (low & (1 << $INDEX(start=1))) != 0; ,, ...(31)]]
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
            dest[destPos + 31] = (low & (1 << 31)) != 0;
//[[Repeat.AutoGeneratedEnd]]

//[[Repeat() dest\[.*?0; ==>
//           dest[destPos + $INDEX(start=33)] = (high & (1 << $INDEX(start=1))) != 0; ,, ...(31)]]
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
            dest[destPos + 63] = (high & (1 << 31)) != 0;
//[[Repeat.AutoGeneratedEnd]]
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L;
        }
    }
    /*Repeat.SectionEnd unpackBits*/

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double,,Object
     */

    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> boolean array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos   position of the first written element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            boolean[] dest, int destPos, long[] src, long srcPos, int count,
            boolean bit0Value, boolean bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
//[[Repeat() dest\[.*?0\s*\? ==>
//           dest[destPos + $INDEX(start=1)] = (low & (1 << $INDEX(start=1))) != 0 ?,, ...(31)]]
            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;
//[[Repeat.AutoGeneratedEnd]]

//[[Repeat() dest\[.*?0\s+\? ==>
//           dest[destPos + $INDEX(start=33)] = (high & (1 << $INDEX(start=1))) != 0 ?,, ...(31)]]
            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;
//[[Repeat.AutoGeneratedEnd]]
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> boolean array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            boolean[] dest, int destPos, long[] src, long srcPos, int count,
            boolean bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {
//[[Repeat() if.*?\] ==>
//           if ((low & (1 << $INDEX(start=1))) != 0) dest[destPos + $INDEX(start=1)],, ...(15)]]
                if ((low & 1) != 0) dest[destPos] = bit1Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;
//[[Repeat.AutoGeneratedEnd]]
            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {
//[[Repeat() if.*?\] ==>
//           if ((low & (1 << $INDEX(start=17))) != 0) dest[destPos + $INDEX(start=17)],, ...(15)]]
                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;
//[[Repeat.AutoGeneratedEnd]]
            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {
//[[Repeat() if.*?\] ==>
//           if ((high & (1 << $INDEX(start=1))) != 0) dest[destPos + $INDEX(start=33)],, ...(15)]]
                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;
//[[Repeat.AutoGeneratedEnd]]
            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {
//[[Repeat() if.*?\] ==>
//           if ((high & (1 << $INDEX(start=17))) != 0) dest[destPos + $INDEX(start=49)],, ...(15)]]
                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;
//[[Repeat.AutoGeneratedEnd]]
            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> boolean array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            boolean[] dest, int destPos, long[] src, long srcPos, int count,
            boolean bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {
//[[Repeat() if.*?\] ==>
//           if ((low & (1 << $INDEX(start=1))) == 0) dest[destPos + $INDEX(start=1)],, ...(15)]]
                if ((low & 1) == 0) dest[destPos] = bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;
//[[Repeat.AutoGeneratedEnd]]
            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {
//[[Repeat() if.*?\] ==>
//           if ((low & (1 << $INDEX(start=17))) == 0) dest[destPos + $INDEX(start=17)],, ...(15)]]
                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;
//[[Repeat.AutoGeneratedEnd]]
            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {
//[[Repeat() if.*?\] ==>
//           if ((high & (1 << $INDEX(start=1))) == 0) dest[destPos + $INDEX(start=33)],, ...(15)]]
                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;
//[[Repeat.AutoGeneratedEnd]]
            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {
//[[Repeat() if.*?\] ==>
//           if ((high & (1 << $INDEX(start=17))) == 0) dest[destPos + $INDEX(start=49)],, ...(15)]]
                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;
//[[Repeat.AutoGeneratedEnd]]
            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            char[] dest, int destPos, long[] src, long srcPos, int count,
            char bit0Value, char bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> char array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>char</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            char[] dest, int destPos, long[] src, long srcPos, int count,
            char bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> char array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>char</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            char[] dest, int destPos, long[] src, long srcPos, int count,
            char bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            byte[] dest, int destPos, long[] src, long srcPos, int count,
            byte bit0Value, byte bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> byte array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>byte</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            byte[] dest, int destPos, long[] src, long srcPos, int count,
            byte bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> byte array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>byte</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            byte[] dest, int destPos, long[] src, long srcPos, int count,
            byte bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            short[] dest, int destPos, long[] src, long srcPos, int count,
            short bit0Value, short bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> short array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>short</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            short[] dest, int destPos, long[] src, long srcPos, int count,
            short bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> short array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>short</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            short[] dest, int destPos, long[] src, long srcPos, int count,
            short bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            int[] dest, int destPos, long[] src, long srcPos, int count,
            int bit0Value, int bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> int array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>int</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            int[] dest, int destPos, long[] src, long srcPos, int count,
            int bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> int array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>int</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            int[] dest, int destPos, long[] src, long srcPos, int count,
            int bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            long[] dest, int destPos, long[] src, long srcPos, int count,
            long bit0Value, long bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> long array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>long</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            long[] dest, int destPos, long[] src, long srcPos, int count,
            long bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> long array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>long</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            long[] dest, int destPos, long[] src, long srcPos, int count,
            long bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            float[] dest, int destPos, long[] src, long srcPos, int count,
            float bit0Value, float bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> float array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>float</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            float[] dest, int destPos, long[] src, long srcPos, int count,
            float bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> float array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>float</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            float[] dest, int destPos, long[] src, long srcPos, int count,
            float bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            double[] dest, int destPos, long[] src, long srcPos, int count,
            double bit0Value, double bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> double array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>double</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            double[] dest, int destPos, long[] src, long srcPos, int count,
            double bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> double array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>double</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            double[] dest, int destPos, long[] src, long srcPos, int count,
            double bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
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
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements in the destination array to which the bit 0 is translated.
     * @param bit1Value the value of elements in the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(
            Object[] dest, int destPos, long[] src, long srcPos, int count,
            Object bit0Value, Object bit1Value) {
        if (bit0Value == bit1Value) {
            java.util.Arrays.fill(dest, destPos, destPos + count, bit0Value);
            return;
        }
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;

            dest[destPos] = (low & 1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 1] = (low & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 2] = (low & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 3] = (low & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 4] = (low & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 5] = (low & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 6] = (low & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 7] = (low & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 8] = (low & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 9] = (low & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 10] = (low & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 11] = (low & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 12] = (low & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 13] = (low & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 14] = (low & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 15] = (low & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 16] = (low & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 17] = (low & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 18] = (low & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 19] = (low & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 20] = (low & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 21] = (low & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 22] = (low & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 23] = (low & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 24] = (low & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 25] = (low & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 26] = (low & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 27] = (low & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 28] = (low & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 29] = (low & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 30] = (low & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 31] = (low & (1 << 31)) != 0 ? bit1Value : bit0Value;


            dest[destPos + 32] = (high & 0x1) != 0 ? bit1Value : bit0Value;

            dest[destPos + 33] = (high & (1 << 1)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 34] = (high & (1 << 2)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 35] = (high & (1 << 3)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 36] = (high & (1 << 4)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 37] = (high & (1 << 5)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 38] = (high & (1 << 6)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 39] = (high & (1 << 7)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 40] = (high & (1 << 8)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 41] = (high & (1 << 9)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 42] = (high & (1 << 10)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 43] = (high & (1 << 11)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 44] = (high & (1 << 12)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 45] = (high & (1 << 13)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 46] = (high & (1 << 14)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 47] = (high & (1 << 15)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 48] = (high & (1 << 16)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 49] = (high & (1 << 17)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 50] = (high & (1 << 18)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 51] = (high & (1 << 19)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 52] = (high & (1 << 20)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 53] = (high & (1 << 21)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 54] = (high & (1 << 22)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 55] = (high & (1 << 23)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 56] = (high & (1 << 24)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 57] = (high & (1 << 25)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 58] = (high & (1 << 26)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 59] = (high & (1 << 27)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 60] = (high & (1 << 28)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 61] = (high & (1 << 29)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 62] = (high & (1 << 30)) != 0 ? bit1Value : bit0Value;
            dest[destPos + 63] = (high & (1 << 31)) != 0 ? bit1Value : bit0Value;

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L ? bit1Value : bit0Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 1 sets the corresponding element of <tt>dest</tt> Object array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit1Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to zero bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?bit1Value:dest[destPos+k]</tt>.
     *
     * @param dest      the destination array (unpacked <tt>Object</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit1Value the value of elements of the destination array to which the bit 1 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackUnitBits(
            Object[] dest, int destPos, long[] src, long srcPos, int count,
            Object bit1Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF) != 0) {

                if ((low & 1) != 0) dest[destPos] = bit1Value;

                if ((low & (1 << 1)) != 0) dest[destPos + 1] = bit1Value;
                if ((low & (1 << 2)) != 0) dest[destPos + 2] = bit1Value;
                if ((low & (1 << 3)) != 0) dest[destPos + 3] = bit1Value;
                if ((low & (1 << 4)) != 0) dest[destPos + 4] = bit1Value;
                if ((low & (1 << 5)) != 0) dest[destPos + 5] = bit1Value;
                if ((low & (1 << 6)) != 0) dest[destPos + 6] = bit1Value;
                if ((low & (1 << 7)) != 0) dest[destPos + 7] = bit1Value;
                if ((low & (1 << 8)) != 0) dest[destPos + 8] = bit1Value;
                if ((low & (1 << 9)) != 0) dest[destPos + 9] = bit1Value;
                if ((low & (1 << 10)) != 0) dest[destPos + 10] = bit1Value;
                if ((low & (1 << 11)) != 0) dest[destPos + 11] = bit1Value;
                if ((low & (1 << 12)) != 0) dest[destPos + 12] = bit1Value;
                if ((low & (1 << 13)) != 0) dest[destPos + 13] = bit1Value;
                if ((low & (1 << 14)) != 0) dest[destPos + 14] = bit1Value;
                if ((low & (1 << 15)) != 0) dest[destPos + 15] = bit1Value;

            }
            if ((low & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((low & 0xFFFF0000) != 0) {

                if ((low & (1 << 16)) != 0) dest[destPos + 16] = bit1Value;

                if ((low & (1 << 17)) != 0) dest[destPos + 17] = bit1Value;
                if ((low & (1 << 18)) != 0) dest[destPos + 18] = bit1Value;
                if ((low & (1 << 19)) != 0) dest[destPos + 19] = bit1Value;
                if ((low & (1 << 20)) != 0) dest[destPos + 20] = bit1Value;
                if ((low & (1 << 21)) != 0) dest[destPos + 21] = bit1Value;
                if ((low & (1 << 22)) != 0) dest[destPos + 22] = bit1Value;
                if ((low & (1 << 23)) != 0) dest[destPos + 23] = bit1Value;
                if ((low & (1 << 24)) != 0) dest[destPos + 24] = bit1Value;
                if ((low & (1 << 25)) != 0) dest[destPos + 25] = bit1Value;
                if ((low & (1 << 26)) != 0) dest[destPos + 26] = bit1Value;
                if ((low & (1 << 27)) != 0) dest[destPos + 27] = bit1Value;
                if ((low & (1 << 28)) != 0) dest[destPos + 28] = bit1Value;
                if ((low & (1 << 29)) != 0) dest[destPos + 29] = bit1Value;
                if ((low & (1 << 30)) != 0) dest[destPos + 30] = bit1Value;
                if ((low & (1 << 31)) != 0) dest[destPos + 31] = bit1Value;

            }

            if ((high & 0xFFFF) == 0xFFFF) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF) != 0) {

                if ((high & 1) != 0) dest[destPos + 32] = bit1Value;

                if ((high & (1 << 1)) != 0) dest[destPos + 33] = bit1Value;
                if ((high & (1 << 2)) != 0) dest[destPos + 34] = bit1Value;
                if ((high & (1 << 3)) != 0) dest[destPos + 35] = bit1Value;
                if ((high & (1 << 4)) != 0) dest[destPos + 36] = bit1Value;
                if ((high & (1 << 5)) != 0) dest[destPos + 37] = bit1Value;
                if ((high & (1 << 6)) != 0) dest[destPos + 38] = bit1Value;
                if ((high & (1 << 7)) != 0) dest[destPos + 39] = bit1Value;
                if ((high & (1 << 8)) != 0) dest[destPos + 40] = bit1Value;
                if ((high & (1 << 9)) != 0) dest[destPos + 41] = bit1Value;
                if ((high & (1 << 10)) != 0) dest[destPos + 42] = bit1Value;
                if ((high & (1 << 11)) != 0) dest[destPos + 43] = bit1Value;
                if ((high & (1 << 12)) != 0) dest[destPos + 44] = bit1Value;
                if ((high & (1 << 13)) != 0) dest[destPos + 45] = bit1Value;
                if ((high & (1 << 14)) != 0) dest[destPos + 46] = bit1Value;
                if ((high & (1 << 15)) != 0) dest[destPos + 47] = bit1Value;

            }
            if ((high & 0xFFFF0000) == 0xFFFF0000) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit1Value;
                }
            } else if ((high & 0xFFFF0000) != 0) {

                if ((high & (1 << 16)) != 0) dest[destPos + 48] = bit1Value;

                if ((high & (1 << 17)) != 0) dest[destPos + 49] = bit1Value;
                if ((high & (1 << 18)) != 0) dest[destPos + 50] = bit1Value;
                if ((high & (1 << 19)) != 0) dest[destPos + 51] = bit1Value;
                if ((high & (1 << 20)) != 0) dest[destPos + 52] = bit1Value;
                if ((high & (1 << 21)) != 0) dest[destPos + 53] = bit1Value;
                if ((high & (1 << 22)) != 0) dest[destPos + 54] = bit1Value;
                if ((high & (1 << 23)) != 0) dest[destPos + 55] = bit1Value;
                if ((high & (1 << 24)) != 0) dest[destPos + 56] = bit1Value;
                if ((high & (1 << 25)) != 0) dest[destPos + 57] = bit1Value;
                if ((high & (1 << 26)) != 0) dest[destPos + 58] = bit1Value;
                if ((high & (1 << 27)) != 0) dest[destPos + 59] = bit1Value;
                if ((high & (1 << 28)) != 0) dest[destPos + 60] = bit1Value;
                if ((high & (1 << 29)) != 0) dest[destPos + 61] = bit1Value;
                if ((high & (1 << 30)) != 0) dest[destPos + 62] = bit1Value;
                if ((high & (1 << 31)) != 0) dest[destPos + 63] = bit1Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L)
                dest[destPos] = bit1Value;
        }
    }

    /**
     * Tests <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and for every found bit 0 sets the corresponding element of <tt>dest</tt> Object array,
     * starting from the element <tt>#destPos</tt>, to <tt>bit0Value</tt>.
     * Elements of <tt>dest</tt> array, corresponding to unit bits of the source array, are not changed.
     * In other words, every element <tt>dest[destPos+k]</tt> is assigned to new value
     * <tt>{@link #getBit getBit}(srcPos+k)?dest[destPos+k]:bit0Value</tt>.
     *
     * @param dest      the destination array (unpacked <tt>Object</tt> values).
     * @param destPos   position of the first element in the destination array.
     * @param src       the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos    position of the first read bit in the source array.
     * @param count     the number of elements to be unpacked (must be &gt;=0).
     * @param bit0Value the value of elements of the destination array to which the bit 0 is translated.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackZeroBits(
            Object[] dest, int destPos, long[] src, long srcPos, int count,
            Object bit0Value) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
        count -= countStart;
        int cnt = count >>> 6;
        int fillPos = -1;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
            if ((low & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos, 16);
                    fillPos = destPos; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF) != 0xFFFF) {

                if ((low & 1) == 0) dest[destPos] = bit0Value;

                if ((low & (1 << 1)) == 0) dest[destPos + 1] = bit0Value;
                if ((low & (1 << 2)) == 0) dest[destPos + 2] = bit0Value;
                if ((low & (1 << 3)) == 0) dest[destPos + 3] = bit0Value;
                if ((low & (1 << 4)) == 0) dest[destPos + 4] = bit0Value;
                if ((low & (1 << 5)) == 0) dest[destPos + 5] = bit0Value;
                if ((low & (1 << 6)) == 0) dest[destPos + 6] = bit0Value;
                if ((low & (1 << 7)) == 0) dest[destPos + 7] = bit0Value;
                if ((low & (1 << 8)) == 0) dest[destPos + 8] = bit0Value;
                if ((low & (1 << 9)) == 0) dest[destPos + 9] = bit0Value;
                if ((low & (1 << 10)) == 0) dest[destPos + 10] = bit0Value;
                if ((low & (1 << 11)) == 0) dest[destPos + 11] = bit0Value;
                if ((low & (1 << 12)) == 0) dest[destPos + 12] = bit0Value;
                if ((low & (1 << 13)) == 0) dest[destPos + 13] = bit0Value;
                if ((low & (1 << 14)) == 0) dest[destPos + 14] = bit0Value;
                if ((low & (1 << 15)) == 0) dest[destPos + 15] = bit0Value;

            }
            if ((low & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 16, 16);
                    fillPos = destPos + 16; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 16;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((low & 0xFFFF0000) != 0xFFFF0000) {

                if ((low & (1 << 16)) == 0) dest[destPos + 16] = bit0Value;

                if ((low & (1 << 17)) == 0) dest[destPos + 17] = bit0Value;
                if ((low & (1 << 18)) == 0) dest[destPos + 18] = bit0Value;
                if ((low & (1 << 19)) == 0) dest[destPos + 19] = bit0Value;
                if ((low & (1 << 20)) == 0) dest[destPos + 20] = bit0Value;
                if ((low & (1 << 21)) == 0) dest[destPos + 21] = bit0Value;
                if ((low & (1 << 22)) == 0) dest[destPos + 22] = bit0Value;
                if ((low & (1 << 23)) == 0) dest[destPos + 23] = bit0Value;
                if ((low & (1 << 24)) == 0) dest[destPos + 24] = bit0Value;
                if ((low & (1 << 25)) == 0) dest[destPos + 25] = bit0Value;
                if ((low & (1 << 26)) == 0) dest[destPos + 26] = bit0Value;
                if ((low & (1 << 27)) == 0) dest[destPos + 27] = bit0Value;
                if ((low & (1 << 28)) == 0) dest[destPos + 28] = bit0Value;
                if ((low & (1 << 29)) == 0) dest[destPos + 29] = bit0Value;
                if ((low & (1 << 30)) == 0) dest[destPos + 30] = bit0Value;
                if ((low & (1 << 31)) == 0) dest[destPos + 31] = bit0Value;

            }

            if ((high & 0xFFFF) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 32, 16);
                    fillPos = destPos + 32; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 32;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF) != 0xFFFF) {

                if ((high & 1) == 0) dest[destPos + 32] = bit0Value;

                if ((high & (1 << 1)) == 0) dest[destPos + 33] = bit0Value;
                if ((high & (1 << 2)) == 0) dest[destPos + 34] = bit0Value;
                if ((high & (1 << 3)) == 0) dest[destPos + 35] = bit0Value;
                if ((high & (1 << 4)) == 0) dest[destPos + 36] = bit0Value;
                if ((high & (1 << 5)) == 0) dest[destPos + 37] = bit0Value;
                if ((high & (1 << 6)) == 0) dest[destPos + 38] = bit0Value;
                if ((high & (1 << 7)) == 0) dest[destPos + 39] = bit0Value;
                if ((high & (1 << 8)) == 0) dest[destPos + 40] = bit0Value;
                if ((high & (1 << 9)) == 0) dest[destPos + 41] = bit0Value;
                if ((high & (1 << 10)) == 0) dest[destPos + 42] = bit0Value;
                if ((high & (1 << 11)) == 0) dest[destPos + 43] = bit0Value;
                if ((high & (1 << 12)) == 0) dest[destPos + 44] = bit0Value;
                if ((high & (1 << 13)) == 0) dest[destPos + 45] = bit0Value;
                if ((high & (1 << 14)) == 0) dest[destPos + 46] = bit0Value;
                if ((high & (1 << 15)) == 0) dest[destPos + 47] = bit0Value;

            }
            if ((high & 0xFFFF0000) == 0) {
                if (fillPos >= 0) {
                    System.arraycopy(dest, fillPos, dest, destPos + 48, 16);
                    fillPos = destPos + 48; // we prefer copying from minimal distance, due to memory cache
                } else {
                    fillPos = destPos + 48;
                    for (int i = fillPos, iMax = i + 16; i < iMax; i++)
                        dest[i] = bit0Value;
                }
            } else if ((high & 0xFFFF0000) != 0xFFFF0000) {

                if ((high & (1 << 16)) == 0) dest[destPos + 48] = bit0Value;

                if ((high & (1 << 17)) == 0) dest[destPos + 49] = bit0Value;
                if ((high & (1 << 18)) == 0) dest[destPos + 50] = bit0Value;
                if ((high & (1 << 19)) == 0) dest[destPos + 51] = bit0Value;
                if ((high & (1 << 20)) == 0) dest[destPos + 52] = bit0Value;
                if ((high & (1 << 21)) == 0) dest[destPos + 53] = bit0Value;
                if ((high & (1 << 22)) == 0) dest[destPos + 54] = bit0Value;
                if ((high & (1 << 23)) == 0) dest[destPos + 55] = bit0Value;
                if ((high & (1 << 24)) == 0) dest[destPos + 56] = bit0Value;
                if ((high & (1 << 25)) == 0) dest[destPos + 57] = bit0Value;
                if ((high & (1 << 26)) == 0) dest[destPos + 58] = bit0Value;
                if ((high & (1 << 27)) == 0) dest[destPos + 59] = bit0Value;
                if ((high & (1 << 28)) == 0) dest[destPos + 60] = bit0Value;
                if ((high & (1 << 29)) == 0) dest[destPos + 61] = bit0Value;
                if ((high & (1 << 30)) == 0) dest[destPos + 62] = bit0Value;
                if ((high & (1 << 31)) == 0) dest[destPos + 63] = bit0Value;

            }
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            if ((src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) == 0L)
                dest[destPos] = bit0Value;
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Unpacks <tt>count</tt> bits, packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and add them <tt>count</tt> elements of <tt>dest</tt> array, starting from the element <tt>#destPos</tt>.
     * It means that every element <tt>dest[destPos+k]</tt> is assigned to
     * <tt>dest[destPos+k]+({@link #getBit getBit}(srcPos+k)?1:0)</tt>.
     *
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param dest    the destination array (unpacked <tt>int</tt> values).
     * @param destPos position of the first increased element in the destination array.
     * @param count   the number of bits to be added to <tt>dest</tt> elements (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void addBitsToInts(int[] dest, int destPos, long[] src, long srcPos, int count) {
        int countStart = (srcPos & 63) == 0 ? 0 : 64 - (int) (srcPos & 63);
        if (countStart > count)
            countStart = count;
        for (int destPosMax = destPos + countStart; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] += (src[(int) (srcPos >>> 6)] >>> (srcPos & 63)) & 1;
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int) (srcPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (int) src[k];
            int high = (int) (src[k] >>> 32);
            srcPos += 64;
//[[Repeat() dest\[.*?1; ==>
//           dest[destPos + $INDEX(start=1)] += (low >>> $INDEX(start=1)) & 1; ,, ...(31)]]
            dest[destPos] += low & 1;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 1] += (low >>> 1) & 1;
            dest[destPos + 2] += (low >>> 2) & 1;
            dest[destPos + 3] += (low >>> 3) & 1;
            dest[destPos + 4] += (low >>> 4) & 1;
            dest[destPos + 5] += (low >>> 5) & 1;
            dest[destPos + 6] += (low >>> 6) & 1;
            dest[destPos + 7] += (low >>> 7) & 1;
            dest[destPos + 8] += (low >>> 8) & 1;
            dest[destPos + 9] += (low >>> 9) & 1;
            dest[destPos + 10] += (low >>> 10) & 1;
            dest[destPos + 11] += (low >>> 11) & 1;
            dest[destPos + 12] += (low >>> 12) & 1;
            dest[destPos + 13] += (low >>> 13) & 1;
            dest[destPos + 14] += (low >>> 14) & 1;
            dest[destPos + 15] += (low >>> 15) & 1;
            dest[destPos + 16] += (low >>> 16) & 1;
            dest[destPos + 17] += (low >>> 17) & 1;
            dest[destPos + 18] += (low >>> 18) & 1;
            dest[destPos + 19] += (low >>> 19) & 1;
            dest[destPos + 20] += (low >>> 20) & 1;
            dest[destPos + 21] += (low >>> 21) & 1;
            dest[destPos + 22] += (low >>> 22) & 1;
            dest[destPos + 23] += (low >>> 23) & 1;
            dest[destPos + 24] += (low >>> 24) & 1;
            dest[destPos + 25] += (low >>> 25) & 1;
            dest[destPos + 26] += (low >>> 26) & 1;
            dest[destPos + 27] += (low >>> 27) & 1;
            dest[destPos + 28] += (low >>> 28) & 1;
            dest[destPos + 29] += (low >>> 29) & 1;
            dest[destPos + 30] += (low >>> 30) & 1;
            dest[destPos + 31] += (low >>> 31) & 1;
//[[Repeat.AutoGeneratedEnd]]

//[[Repeat() dest\[.*?1; ==>
//           dest[destPos + $INDEX(start=33)] += (high >>> $INDEX(start=1)) & 1; ,, ...(31)]]
            dest[destPos + 32] += high & 1;
//[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            dest[destPos + 33] += (high >>> 1) & 1;
            dest[destPos + 34] += (high >>> 2) & 1;
            dest[destPos + 35] += (high >>> 3) & 1;
            dest[destPos + 36] += (high >>> 4) & 1;
            dest[destPos + 37] += (high >>> 5) & 1;
            dest[destPos + 38] += (high >>> 6) & 1;
            dest[destPos + 39] += (high >>> 7) & 1;
            dest[destPos + 40] += (high >>> 8) & 1;
            dest[destPos + 41] += (high >>> 9) & 1;
            dest[destPos + 42] += (high >>> 10) & 1;
            dest[destPos + 43] += (high >>> 11) & 1;
            dest[destPos + 44] += (high >>> 12) & 1;
            dest[destPos + 45] += (high >>> 13) & 1;
            dest[destPos + 46] += (high >>> 14) & 1;
            dest[destPos + 47] += (high >>> 15) & 1;
            dest[destPos + 48] += (high >>> 16) & 1;
            dest[destPos + 49] += (high >>> 17) & 1;
            dest[destPos + 50] += (high >>> 18) & 1;
            dest[destPos + 51] += (high >>> 19) & 1;
            dest[destPos + 52] += (high >>> 20) & 1;
            dest[destPos + 53] += (high >>> 21) & 1;
            dest[destPos + 54] += (high >>> 22) & 1;
            dest[destPos + 55] += (high >>> 23) & 1;
            dest[destPos + 56] += (high >>> 24) & 1;
            dest[destPos + 57] += (high >>> 25) & 1;
            dest[destPos + 58] += (high >>> 26) & 1;
            dest[destPos + 59] += (high >>> 27) & 1;
            dest[destPos + 60] += (high >>> 28) & 1;
            dest[destPos + 61] += (high >>> 29) & 1;
            dest[destPos + 62] += (high >>> 30) & 1;
            dest[destPos + 63] += (high >>> 31) & 1;
//[[Repeat.AutoGeneratedEnd]]
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] += (src[(int) (srcPos >>> 6)] >>> (srcPos & 63)) & 1;
        }
    }

    /*Repeat.SectionStart fillBits*/

    /**
     * Fills <tt>count</tt> bits in the packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<tt>false</tt> means the bit 0, <tt>true</tt> means the bit 1).
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if filling would cause access of data outside array bounds.
     */
    public static void fillBits(long[] dest, long destPos, long count, boolean value) {
        int dPos = (int) (destPos >>> 6);
        int dPosRem = (int) (destPos & 63);
        int cntStart = (-dPosRem) & 63;
        long maskStart = -1L << dPosRem; // dPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (dPosRem + cntStart)) - 1; // &= dPosRem+cntStart times 1 (from the left)
        }
        if (cntStart > 0) {
            synchronized (dest) {
                if (value)
                    dest[dPos] |= maskStart;
                else
                    dest[dPos] &= ~maskStart;
            }
            count -= cntStart;
            dPos++;
        }
        long longValue = value ? -1 : 0;
        for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++) {
            dest[dPos] = longValue;
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            synchronized (dest) {
                if (value)
                    dest[dPos] |= maskFinish;
                else
                    dest[dPos] &= ~maskFinish;
            }
        }
    }
    /*Repeat.SectionEnd fillBits*/

    /**
     * Returns <tt>true</tt> if the specified fragment of the given packed bit array
     * is filled by zero bits (<tt>0</tt>).
     * Returns <tt>false</tt> if at least one of <tt>count</tt> bits of this array,
     * starting from the bit <tt>#pos</tt>, is <tt>1</tt>.
     *
     * <p>If the <tt>count</tt> argument (number of elements) is 0, this method returns <tt>true</tt>.
     *
     * @param array the checked bit array (bits are packed in <tt>long</tt> values).
     * @param pos   the initial index of the checked fragment in the array.
     * @param count the number of checked bits.
     * @return <tt>true</tt> if and only if all <tt>count</tt> bits, starting from the bit <tt>#pos</tt>,
     * are zero, or if <tt>count==0</tt>.
     * @throws NullPointerException      if the <tt>array</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if checking would cause access of data outside array bounds.
     */
    public static boolean areBitsZero(long[] array, long pos, long count) {
        int sPos = (int) (pos >>> 6);
        int sPosRem = (int) (pos & 63);
        int cntStart = (-sPosRem) & 63;
        long maskStart = -1L << sPosRem; // sPosRem times 0, then 1 (from the left)
        if (cntStart > count) {
            cntStart = (int) count;
            maskStart &= (1L << (sPosRem + cntStart)) - 1; // &= sPosRem+cntStart times 1 (from the left)
        }
        if (cntStart > 0) {
            if ((array[sPos] & maskStart) != 0L) {
                return false;
            }
            count -= cntStart;
            sPos++;
        }
        for (int dPosMax = sPos + (int) (count >>> 6); sPos < dPosMax; sPos++) {
            if (array[sPos] != 0L) {
                return false;
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            if ((array[sPos] & maskFinish) != 0L) {
                return false;
            }
        }
        return true;
    }

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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void notBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart notBits_method_impl]]
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
                    dest[dPos] = (~src[sPos] & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] = ~src[sPos];
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (~src[sPos] & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src[sPos]) << shift;
                    else
                        v = (sNext = src[sPos]) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = src[sPos];
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] = ~((sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64));
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = (~v & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //[[Repeat.SectionEnd notBits_method_impl]]
    }

    /*Repeat.SectionStart andOrBits*/

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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void andBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart andBits_method_impl]]
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
                    dest[dPos] &= src[sPos] | ~maskStart;
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] &= src[sPos];
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] &= src[sPos] | ~maskFinish;
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src[sPos]) << shift;
                    else
                        v = (sNext = src[sPos]) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = src[sPos];
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] &= (sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64);
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] &= v | ~maskFinish;
                }
            }
        }
        //[[Repeat.SectionEnd andBits_method_impl]]
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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void orBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart orBits_method_impl]]
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
                    dest[dPos] |= src[sPos] & maskStart;
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] |= src[sPos];
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] |= src[sPos] & maskFinish;
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src[sPos]) << shift;
                    else
                        v = (sNext = src[sPos]) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = src[sPos];
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] |= (sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64);
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] |= v & maskFinish;
                }
            }
        }
        //[[Repeat.SectionEnd orBits_method_impl]]
    }
    /*Repeat.SectionEnd andOrBits*/

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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void xorBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart xorBits_method_impl]]
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
                    dest[dPos] = ((dest[dPos] ^ src[sPos]) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] ^= src[sPos];
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] ^ src[sPos]) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src[sPos]) << shift;
                    else
                        v = (sNext = src[sPos]) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = src[sPos];
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] ^= (sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64);
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] ^ v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //[[Repeat.SectionEnd xorBits_method_impl]]
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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void andNotBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart andNotBits_method_impl]]
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
                    dest[dPos] = ((dest[dPos] & ~src[sPos]) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] &= ~src[sPos];
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] & ~src[sPos]) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src[sPos]) << shift;
                    else
                        v = (sNext = src[sPos]) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = src[sPos];
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] &= ~((sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64));
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] & ~v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //[[Repeat.SectionEnd andNotBits_method_impl]]
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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void orNotBits(long[] dest, long destPos, long[] src, long srcPos, long count) {
        //[[Repeat.SectionStart orNotBits_method_impl]]
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
                    dest[dPos] = ((dest[dPos] | ~src[sPos]) & maskStart) | (dest[dPos] & ~maskStart);
                }
                count -= cntStart;
                dPos++;
                sPos++;
            }
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++, sPos++) {
                dest[dPos] |= ~src[sPos];
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] | ~src[sPos]) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            final int shift = dPosRem - sPosRem;
            long sNext;
            if (cntStart > 0) {
                long v;
                if (sPosRem + cntStart <= 64) { // cntStart bits are in a single src element
                    if (shift > 0)
                        v = (sNext = src[sPos]) << shift;
                    else
                        v = (sNext = src[sPos]) >>> -shift;
                    sPosRem += cntStart;
                } else {
                    v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (64 + shift));
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
                    return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
                }
                sNext = src[sPos];
            }
            // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
            final int sPosRem64 = 64 - sPosRem;
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; ) {
                sPos++;
                dest[dPos] |= ~((sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem64));
                dPos++;
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem + cntFinish <= 64) { // cntFinish bits are in a single src element
                    v = sNext >>> sPosRem;
                } else {
                    v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem64);
                }
                synchronized (dest) {
                    dest[dPos] = ((dest[dPos] | ~v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
        //[[Repeat.SectionEnd orNotBits_method_impl]]
    }

    /**
     * Reverse order of <tt>count</tt> bits,
     * packed in <tt>src</tt> array, starting from the bit <tt>#srcPos</tt>,
     * and puts the result into <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     * So, the bit <tt>#(destPos+k)</tt> in <tt>dest</tt> bit array will be equal to the bit
     * <tt>#(srcPos+count-1-k)</tt> of the <tt>src</tt> bit array.
     *
     * <p>This method does not work correctly if <tt>src&nbsp;==&nbsp;dest</tt>.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     */
    public static void reverseBitsOrder(long[] dest, long destPos, long[] src, long srcPos, long count) {
        if (count == 0) {
            return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
        }
        srcPos += count;
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
        if (cntStart > 0) {
            long v;
            if (sPosRem >= cntStart) { // cntStart bits are in a single src element
                v = src[sPos] >>> sPosRem - cntStart;
                sPosRem -= cntStart;
            } else {
                v = sPosRem == 0 ? // necessary check to avoid possible IndexOutOfBoundsException
                        src[sPos - 1] >>> (64 - cntStart) :
                        (src[sPos] << cntStart - sPosRem) | (src[sPos - 1] >>> (64 + sPosRem - cntStart));
                sPos--;
                sPosRem = (sPosRem - cntStart) & 63;
            }
            // low cntStart bits of v contain the last cntStart bits of src
            v = Long.reverse(v) >>> 64 - cntStart;
            // low cntStart bits of v contain the first cntStart bits of the result (dest)
            synchronized (dest) {
                dest[dPos] = ((v << dPosRem) & maskStart) | (dest[dPos] & ~maskStart);
            }
            // let's suppose dPosRem = 0 now; don't perform it, because we'll not use dPosRem more
            count -= cntStart;
            if (count == 0) {
                return; // necessary check to avoid IndexOutOfBoundException while accessing src[sPos]
            }
            dPos++;
        }

        if (sPosRem == 0) {
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++) {
                sPos--;
                dest[dPos] = Long.reverse(src[sPos]);
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                synchronized (dest) {
                    dest[dPos] = (Long.reverse(src[sPos - 1]) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        } else {
            int sPosRem64 = 64 - sPosRem;
            long sNext = src[sPos];
            // count > 0, so either this loop or the next loop will really work: src[sPos] must exist
            for (int dPosMax = dPos + (int) (count >>> 6); dPos < dPosMax; dPos++) {
                sPos--;
                dest[dPos] = Long.reverse((sNext << sPosRem64) | ((sNext = src[sPos]) >>> sPosRem));
            }
            int cntFinish = (int) (count & 63);
            if (cntFinish > 0) {
                long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
                long v;
                if (sPosRem >= cntFinish) { // cntFinish bits are in a single src element
                    v = sNext << sPosRem64;
                } else {
                    v = (sNext << sPosRem64) | (src[sPos - 1] >>> sPosRem);
                }
                synchronized (dest) {
                    dest[dPos] = (Long.reverse(v) & maskFinish) | (dest[dPos] & ~maskFinish);
                }
            }
        }
    }

    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;highIndex</tt>
     * and the bit <tt>#k</tt> in the packed <tt>src</tt> bit array is equal to <tt>value</tt>,
     * or <tt>-1</tt> if there is no such bits.
     *
     * <p>If <tt>lowIndex&gt;=highIndex</tt>, this method returns <tt>-1</tt>.
     *
     * @param src       the searched packed bit array.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of bit to be found.
     * @return the index of the first occurrence of this bit in range <tt>lowIndex..highIndex-1</tt>,
     * or <tt>-1</tt> if this bit does not occur
     * or if <tt>lowIndex&gt;=highIndex</tt>.
     * @throws NullPointerException      if <tt>array</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>lowIndex</tt> is negative or
     *                                   if <tt>highIndex</tt> is greater than <tt>src.length*64</tt>.
     * @see #lastIndexOfBit(long[], long, long, boolean)
     */
    public static long indexOfBit(long[] src, long lowIndex, long highIndex, boolean value) {
        //[[Repeat.SectionStart indexOfBit_method_impl]]
        if (lowIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: low index = " + lowIndex);
        }
        if (highIndex > ((long) src.length) << 6) {
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
            int index = numberOfTrailingZeros((value ? src[sPos] : ~src[sPos]) & maskStart);
            if (index != 64)
                return fromAligned + index;
            count -= cntStart;
            sPos++;
            fromAligned += 64;
        }
        if (value) {
            for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; sPos++, fromAligned += 64) {
                int index = numberOfTrailingZeros(src[sPos]);
                if (index != 64) {
                    return fromAligned + index;
                }
            }
        } else {
            for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; sPos++, fromAligned += 64) {
                int index = numberOfTrailingZeros(~src[sPos]);
                if (index != 64) {
                    return fromAligned + index;
                }
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            int index = numberOfTrailingZeros((value ? src[sPos] : ~src[sPos]) & maskFinish);
            if (index != 64) {
                return fromAligned + index;
            }
        }
        return -1;
        //[[Repeat.SectionEnd indexOfBit_method_impl]]
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=lowIndex</tt>
     * and the bit <tt>#k</tt> in the packed <tt>src</tt> bit array is equal to <tt>value</tt>,
     * or <tt>-1</tt> if there is no such bits.
     *
     * <p>If <tt>highIndex&lt;=lowIndex</tt>, this method returns <tt>-1</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfBit(long[], long, long, boolean)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param src       the searched packed bit array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of bit to be found.
     * @return the index of the last occurrence of this bit in range <tt>lowIndex..highIndex-1</tt>,
     * or <tt>-1</tt> if this bit does not occur
     * or if <tt>lowIndex&gt;=highIndex</tt>.
     * @throws NullPointerException      if <tt>src</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>lowIndex</tt> is negative or
     *                                   if <tt>highIndex</tt> is greater than <tt>src.length*64</tt>.
     * @see #indexOfBit(long[], long, long, boolean)
     */
    public static long lastIndexOfBit(long[] src, long lowIndex, long highIndex, boolean value) {
        //[[Repeat.SectionStart lastIndexOfBit_method_impl]]
        if (lowIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: low index = " + lowIndex);
        }
        if (highIndex > ((long) src.length) << 6) {
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
            int index = numberOfLeadingZeros((value ? src[sPos] : ~src[sPos]) & maskStart);
            if (index != 64) {
                return fromAligned - index;
            }
            count -= cntStart;
            sPos--;
            fromAligned -= 64;
        }
        if (value) {
            for (int sPosMin = sPos - (int) (count >>> 6); sPos > sPosMin; sPos--, fromAligned -= 64) {
                int index = numberOfLeadingZeros(src[sPos]);
                if (index != 64) {
                    return fromAligned - index;
                }
            }
        } else {
            for (int sPosMin = sPos - (int) (count >>> 6); sPos > sPosMin; sPos--, fromAligned -= 64) {
                int index = numberOfLeadingZeros(~src[sPos]);
                if (index != 64) {
                    return fromAligned - index;
                }
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = -1L << (64 - cntFinish); // cntFinish times 1 (from the right)
            int index = numberOfLeadingZeros((value ? src[sPos] : ~src[sPos]) & maskFinish);
            if (index != 64) {
                return fromAligned - index;
            }
        }
        return -1;
        //[[Repeat.SectionEnd lastIndexOfBit_method_impl]]
    }

    /*Repeat.SectionStart cardinality*/

    /**
     * Returns the number of high bits (1) in the given fragment of the given packed bit array.
     *
     * @param src       the source packed bit array.
     * @param fromIndex the initial checked bit index in <tt>array</tt>, inclusive.
     * @param toIndex   the end checked bit index in <tt>array</tt>, exclusive.
     * @return the number of high bits (1) in the given fragment of the given packed bit array.
     * @throws NullPointerException      if the <tt>src</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> or <tt>toIndex</tt> are negative,
     *                                   if <tt>toIndex</tt> is greater than <tt>src.length*64</tt>,
     *                                   or if <tt>fromIndex</tt> is greater than <tt>startIndex</tt>
     */
    public static long cardinality(long[] src, final long fromIndex, final long toIndex) {
        //[[Repeat.SectionStart cardinality_method_impl]]
        Objects.requireNonNull(src, "Null src argument in cardinality method");
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex);
        }
        if (toIndex > ((long) src.length) << 6) {
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
            result += bitCount(src[sPos] & maskStart);
            count -= cntStart;
            sPos++;
        }
        // The loop below is the 64-bit version of the algorithm published in
        // "Hacker's Delight" by Henry S. Warren, figure 5-4,
        // Addison-Wesley Publishing Company, Inc., 2002.
        for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; ) {
            long s8 = 0;
            for (int lim = Math.min(sPosMax, sPos + 31); sPos < lim; sPos++) {
                // maximal intermediate sums (bit counts in bytes) are 8,
                // so 31 iterations do not lead to overflow (8*31<256)
                long value = src[sPos];
                value -= (value >>> 1) & 0x5555555555555555L;
                value = (value & 0x3333333333333333L) + ((value >>> 2) & 0x3333333333333333L);
                s8 += (value + (value >>> 4)) & 0x0F0F0F0F0F0F0F0FL;
            }
            s8 = (s8 & 0x00FF00FF00FF00FFL) + ((s8 >>> 8) & 0x00FF00FF00FF00FFL);
            s8 = (s8 & 0x0000FFFF0000FFFFL) + ((s8 >>> 16) & 0x0000FFFF0000FFFFL);
            result += ((int) s8 + (int) (s8 >>> 32)) & 0xFFFF;

        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            result += bitCount(src[sPos] & maskFinish);
        }
        return result;
        //[[Repeat.SectionEnd cardinality_method_impl]]
    }
    /*Repeat.SectionEnd cardinality*/

    static int numberOfLeadingZeros(long i) {
        // The 64-bit version of the algorithm published in
        // "Hacker's Delight" by Henry S. Warren, figure 5-5,
        // Addison-Wesley Publishing Company, Inc., 2002.
        // Long.numberOfLeadingZeros method is not used for compatibility with JDK 1.1.
        if (i == 0) {
            return 64;
        }
        int n = 1;
        int x = (int) (i >>> 32);
        if (x == 0) {
            n += 32;
            x = (int) i;
        }
        if (x >>> 16 == 0) {
            n += 16;
            x <<= 16;
        }
        if (x >>> 24 == 0) {
            n += 8;
            x <<= 8;
        }
        if (x >>> 28 == 0) {
            n += 4;
            x <<= 4;
        }
        if (x >>> 30 == 0) {
            n += 2;
            x <<= 2;
        }
        n -= x >>> 31;
        return n;
    }

    static int numberOfTrailingZeros(long i) {
        // The 64-bit version of the algorithm published in
        // "Hacker's Delight" by Henry S. Warren, figure 5-13,
        // Addison-Wesley Publishing Company, Inc., 2002.
        // Long.numberOfTrailingZeros method is not used for compatibility with JDK 1.1.
        int x, y;
        if (i == 0) {
            return 64;
        }
        int n = 63;
        y = (int) i;
        if (y != 0) {
            n = n - 32;
            x = y;
        } else x = (int) (i >>> 32);
        y = x << 16;
        if (y != 0) {
            n = n - 16;
            x = y;
        }
        y = x << 8;
        if (y != 0) {
            n = n - 8;
            x = y;
        }
        y = x << 4;
        if (y != 0) {
            n = n - 4;
            x = y;
        }
        y = x << 2;
        if (y != 0) {
            n = n - 2;
            x = y;
        }
        return n - ((x << 1) >>> 31);
    }

    static int bitCount(long value) {
        // The 64-bit version of the algorithm published in
        // "Hacker's Delight" by Henry S. Warren, figure 5-1,
        // Addison-Wesley Publishing Company, Inc., 2002.
        // Long.bitCount method is not used for compatibility with JDK 1.1.
        value -= (value >>> 1) & 0x5555555555555555L;
        value = (value & 0x3333333333333333L) + ((value >>> 2) & 0x3333333333333333L);
        value = (value + (value >>> 4)) & 0x0F0F0F0F0F0F0F0FL;
        value += value >>> 8;
        value += value >>> 16;
        return ((int) value + (int) (value >>> 32)) & 0xFF;
    }

}
