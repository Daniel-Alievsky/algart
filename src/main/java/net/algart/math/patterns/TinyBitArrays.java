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

package net.algart.math.patterns;

/**
 * <p>A simple analog of <tt>net.algart.arrays.PackedBitArrays</tt> for internal use by this package.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class TinyBitArrays {
    private TinyBitArrays() {
    }

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/PackedBitArrays.java, primitives) !! Auto-generated: NOT EDIT !! */

    /**
     * Returns <tt>(unpackedLength + 63) >>> 6</tt>: the minimal number of <tt>long</tt> values
     * allowing to store <tt>unpackedLength</tt> bits.
     *
     * @param unpackedLength the number of bits (the length of bit array).
     * @return <tt>(unpackedLength + 63) >>> 6</tt> (the length of corresponding <tt>long[]</tt> array).
     */
    public static long packedLength(long unpackedLength) {
        return (unpackedLength + 63) >>> 6;
        // here >>> must be used instead of >>, because unpackedLength+63 may be >Long.MAX_VALUE
    }

    /**
     * Returns the bit <tt>#index</tt> in the packed <tt>dest</tt> bit array.
     * Equivalent to the following expression:<pre>
     * (src[(int)(index >>> 6)] & (1L << (index & 63))) != 0L;
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
     * &#32;       dest[(int)(index >>> 6)] |= 1L << (index & 63);
     * &#32;   else
     * &#32;       dest[(int)(index >>> 6)] &= ~(1L << (index & 63));
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
    /*Repeat.IncludeEnd*/

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/PackedBitArrays.java, copyBits)
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
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source array (bits are packed in <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     */
    public static void copyBits(long[] dest, long destPos, long[] src, long srcPos, long count) {

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
                        dest[dPos + cnt] = (src[sPos + cnt] & maskFinish) | (dest[dPos + cnt] & ~maskFinish);
                    }
                }
                System.arraycopy(src, sPos, dest, dPos, cnt);
                if (cntStart > 0) {
                    synchronized (dest) {
                        dest[dPosStart] = (src[sPosStart] & maskStart) | (dest[dPosStart] & ~maskStart);
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

    }
    /*Repeat.IncludeEnd*/

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/PackedBitArrays.java, unpackBits)
       !! Auto-generated: NOT EDIT !! */
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

            dest[destPos] = (low & 1) != 0;

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



            dest[destPos + 32] = (high & 0x1) != 0;

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

            destPos += 64;
        }
        int countFinish = count & 63;
        for (int destPosMax = destPos + countFinish; destPos < destPosMax; srcPos++, destPos++) {
            dest[destPos] = (src[(int) (srcPos >>> 6)] & (1L << (srcPos & 63))) != 0L;
        }
    }
    /*Repeat.IncludeEnd*/

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/PackedBitArrays.java, fillBits)
       !! Auto-generated: NOT EDIT !! */
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
     * @throws NullPointerException if <tt>dest</tt> is <tt>null</tt>.
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
    /*Repeat.IncludeEnd*/

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/PackedBitArrays.java, andOrBits)
       !! Auto-generated: NOT EDIT !! */
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

    }
    /*Repeat.IncludeEnd*/

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/PackedBitArrays.java, cardinality)
      \bbitCount\b ==> Long.bitCount
       !! Auto-generated: NOT EDIT !! */
    /**
     * Returns the number of high bits (1) in the given fragment of the given packed bit array.
     *
     * @param src       the source packed bit array.
     * @param fromIndex the initial checked bit index in <tt>array</tt>, inclusive.
     * @param toIndex   the end checked bit index in <tt>array</tt>, exclusive.
     * @return          the number of high bits (1) in the given fragment of the given packed bit array.
     * @throws NullPointerException if the <tt>src</tt> argument is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> or <tt>toIndex</tt> are negative,
     *                                   if <tt>toIndex</tt> is greater than <tt>src.length*64</tt>,
     *                                   or if <tt>fromIndex</tt> is greater than <tt>startIndex</tt>
     */
    public static long cardinality(long[] src, final long fromIndex, final long toIndex) {

        if (src == null)
            throw new NullPointerException("Null src argument in cardinality method");
        if (fromIndex < 0)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex);
        if (toIndex > ((long) src.length) << 6)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: end index = " + toIndex);
        if (fromIndex > toIndex)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex
                + " > end index = " + toIndex);
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
            result += Long.bitCount(src[sPos] & maskStart);
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
            result += Long.bitCount(src[sPos] & maskFinish);
        }
        return result;

    }
    /*Repeat.IncludeEnd*/

}
