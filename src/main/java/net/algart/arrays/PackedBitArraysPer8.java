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

import java.util.Objects;

/**
 * <p>Operations with bit arrays packed into <tt>byte[]</tt> Java arrays.</p>
 *
 * <p>This is a reduced analog of {@link PackedBitArrays} class, using <tt>byte</tt> values
 * for packing bits instead of <tt>long</tt> values< AlgART bit arrays do not use this class,
 * but it can be useful in external modules, where bits are packed into bytes.
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
 * <p>(See an example in comments to {@link #setBit} method.)
 * If all 8 bits of the element are written, or if the bits are read only, then no synchronization is performed.
 * Such behavior allows to simultaneously work with non-overlapping fragments of a packed bit array
 * from several threads (different fragments for different threads), as if it would be a usual Java array.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class PackedBitArraysPer8 {
    private PackedBitArraysPer8() {}

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
    public static void setBit(long[] dest, long index, boolean value) {
        synchronized (dest) {
            if (value)
                dest[(int)(index >>> 3)] |= 1 << (index & 7);
            else
                dest[(int)(index >>> 3)] &= ~(1 << (index & 7));
        }
    }



  /*Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, copyBits)
        <tt>long<\/tt> ==> <tt>byte</tt> ;;
        long\[\] ==> byte[] ;;
        >>>\s*6 ==> >>> 3 ;;
        63\b ==> 7 ;;
        64\b ==> 8 ;;
        long\s+(maskStart|maskFinish|v|sPrev|sNext)\b ==> int $1 ;;
        1L\b ==> 1 ;;
        dest(\[[^\]]+\])\s+=\s+([^;]+); ==> dest$1 = (byte) ($2);
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
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBits(byte[] dest, long destPos, byte[] src, long srcPos, long count) {

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
                        dest[dPos + cnt] = (byte) ((src[sPos + cnt] & maskFinish) | (dest[dPos + cnt] & ~maskFinish));
                    }
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPosStart] = (byte) ((src[sPosStart] & maskStart) | (dest[dPosStart] & ~maskStart));
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
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
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
                        v = (sPrev = src[sPos]) >>> sPosRem;
                    } else {
                        v = ((sPrev = src[sPos]) >>> sPosRem) | (src[sPos + 1] << sPosRem8);
                    }
                    synchronized (dest) {
                        dest[dPos] = (byte) ((v & maskFinish) | (dest[dPos] & ~maskFinish));
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
                    dest[dPos] = (byte) ((sPrev << sPosRem8) | ((sPrev = src[sPos]) >>> sPosRem));
                }
                if (cntStart > 0) { // here we correct indexes only: we delay actual access until the end
                    int v;
                    if (sPosRemStart + cntStart <= 8) { // cntStart bits are in a single src element
                        if (shift > 0)
                            v = src[sPosStart] << shift;
                        else
                            v = src[sPosStart] >>> -shift;
                    } else {
                        v = (src[sPosStart] >>> -shift) | (src[sPosStart + 1] << (8 + shift));
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
                            v = (sNext = src[sPos]) << shift;
                        else
                            v = (sNext = src[sPos]) >>> -shift;
                        sPosRem += cntStart;
                    } else {
                        v = (src[sPos] >>> -shift) | ((sNext = src[sPos + 1]) << (8 + shift));
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
                    sNext = src[sPos];
                }
                // Now the bit #0 of dest[dPos] corresponds to the bit #sPosRem of src[sPos]
                final int sPosRem8 = 8 - sPosRem;
                for (int dPosMax = dPos + (int) (count >>> 3); dPos < dPosMax; ) {
                    sPos++;
                    dest[dPos] = (byte) ((sNext >>> sPosRem) | ((sNext = src[sPos]) << sPosRem8));
                    dPos++;
                }
                int cntFinish = (int) (count & 7);
                if (cntFinish > 0) {
                    int maskFinish = (1 << cntFinish) - 1; // cntFinish times 1 (from the left)
                    int v;
                    if (sPosRem + cntFinish <= 8) { // cntFinish bits are in a single src element
                        v = sNext >>> sPosRem;
                    } else {
                        v = (sNext >>> sPosRem) | (src[sPos + 1] << sPosRem8);
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
     * Copies <tt>count</tt> bits from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (unpacked <tt>boolean</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void packBits(byte[] dest, long destPos, boolean[] src, int srcPos, int count) {
        int countStart = (destPos & 7) == 0 ? 0 : 8 - (int) (destPos & 7);
        if (countStart > count)
            countStart = count;
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
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>byte</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void unpackBits(boolean[] dest, int destPos, byte[] src, long srcPos, int count) {
        int countStart = (srcPos & 7) == 0 ? 0 : 8 - (int) (srcPos & 7);
        if (countStart > count)
            countStart = count;
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
     * Fills <tt>count</tt> bits in the packed <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * @param dest    the destination array (bits are packed in <tt>byte</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<tt>false</tt> means the bit 0, <tt>true</tt> means the bit 1).
     * @throws NullPointerException if <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if filling would cause access of data outside array bounds.
     */
    public static void fillBits(byte[] dest, long destPos, long count, boolean value) {
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

    /**
     * Returns the number of high bits (1) in the given fragment of the given packed bit array.
     *
     * @param src       the source packed bit array.
     * @param fromIndex the initial checked bit index in <tt>array</tt>, inclusive.
     * @param toIndex   the end checked bit index in <tt>array</tt>, exclusive.
     * @return          the number of high bits (1) in the given fragment of the given packed bit array.
     * @throws NullPointerException if the <tt>src</tt> argument is <tt>null</tt>.
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

}
