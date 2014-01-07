/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Operations with bit arrays packed into <tt>java.nio.LongBuffer</tt>.</p>
 *
 * <p>The maximal length of bit arrays supported by this class is <tt>2<sup>37</sup>-64</tt>.
 * All indexes and lengths passed to methods of this class should not exceed this value.
 * In other case, the results are unspecified. ("Unspecified" means that any elements
 * of the passed buffers can be read or changed, or that <tt>IndexOutOfBoundsException</tt> can be thrown.)</p>
 *
 * <p>In all methods of this class, it's supposed that the bit <tt>#k</tt>
 * in a packed <tt>LongBuffer b</tt> is the bit
 * <tt>#(k%64)</tt> in the long element <tt>b.get(k/64)</tt>. In other words, the bit <tt>#k</tt>
 * (<tt>false</tt> or <tt>true</tt> value) can be extracted by the following operator:</p
 *
 * <pre>
 * (b.get(k >>> 6) & (1L << (k & 63))) != 0L
 * </pre>
 *
 * <p>and can be set or cleared by the following operators:</p>
 *
 * <pre>
 * if (newValue) // we need to set bit #k to 1
 * &#32;   b.put(k >>> 6, b.get(k >>> 6) | 1L << (k & 63));
 * else          // we need to clear bit #k to 0
 * &#32;   b.put(k >>> 6, b.get(k >>> 6) & ~(1L << (k & 63)));
 * </pre>
 *
 * <p>If any method of this class modifies some portion of an element of a packed <tt>LongBuffer</tt>,
 * i.e. modifies less than all 64 its bits, then all accesses to this <tt>long</tt> element are performed
 * <b>inside a single synchronized block</b>, using the following instruction:</p>
 *
 * <pre>
 * synchronized ({@link #getLock getLock}(buffer)) {
 * &#32;   // accessing to some element #k via buffer.get(k) and buffer.put(k, ...)
 * }
 * </pre>
 *
 * <p>(See an example in comments to {@link #setBit} method.)
 * If all 64 bits of the element are written, or if the bits are read only, then no synchronization is performed.
 * Such behavior allows to simultaneously work with non-overlapping fragments of a packed bit array
 * from several threads (different fragments for different threads), as if it would be a usual Java array.
 * Synchronization by <tt>{@link #getLock getLock}(buffer)</tt> (instead of <tt>buffer</tt> instance) allows
 * to use in different threads different instances of <tt>LongBuffer</tt>, created by <tt>LongBuffer.wrap</tt>
 * method for the sampe Java <tt>long[]</tt> array.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.4
 */
public class PackedBitBuffers {
    private PackedBitBuffers() {}

    /**
     * Returns <tt>buffer.hasArray()?buffer.array():buffer</tt>.
     * This object is used by all methods of this class for synchronization, when any portion (not all 64 bits)
     * of some <tt>long</tt> element is modified.
     * Synchronization by <tt>buffer.array()</tt> (instead of <tt>buffer</tt> instance) allows
     * to use in different threads different instances of <tt>LongBuffer</tt>, created by <tt>LongBuffer.wrap</tt>
     * method for the sampe Java <tt>long[]</tt> array.
     *
     * @param buffer the buffer.
     * @return       this buffer if it is not backed by a Java array, the underlying Java array if it is backed by it.
     */
    public static Object getLock(LongBuffer buffer) {
        return buffer.hasArray() ? buffer.array() : buffer;
    }

    /**
     * Returns the bit <tt>#index</tt> in the packed <tt>dest</tt> bit buffer.
     * Equivalent to the following expression:<pre>
     * (src.get((int)(index >>> 6)) & (1L << (index & 63))) != 0L;
     * </pre>
     *
     * @param src   the source buffer (bits are packed into <tt>long</tt> values).
     * @param index index of the returned bit.
     * @return      the bit at the specified index.
     * @throws IndexOutOfBoundsException if this method cause access of data outside buffer limits.
     * @throws NullPointerException if <tt>src</tt> is <tt>null</tt>.
     */
    public static boolean getBit(LongBuffer src, long index) {
        return (src.get((int) (index >>> 6)) & (1L << (index & 63))) != 0L;
    }

    /**
     * Sets the bit <tt>#index</tt> in the packed <tt>dest</tt> bit buffer.
     * Equivalent to the following operators:<pre>
     * synchronized ({@link #getLock PackedBitBuffers.getLock}(dest)) {
     * &#32;   if (value)
     * &#32;       dest.put((int)(index >>> 6), dest.get((int)(index >>> 6)) | 1L << (index & 63));
     * &#32;   else
     * &#32;       dest.put((int)(index >>> 6), dest.get((int)(index >>> 6)) & ~(1L << (index & 63)));
     * }
     * </pre>
     *
     * @param dest  the destination buffer (bits are packed into <tt>long</tt> values).
     * @param index index of the written bit.
     * @param value new bit value.
     * @throws IndexOutOfBoundsException if this method cause access of data outside array bounds.
     * @throws NullPointerException if <tt>dest</tt> is <tt>null</tt>.
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
     * Copies <tt>count</tt> bits, packed into <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> buffer, starting from the bit <tt>#destPos</tt>.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * bits at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt>
     * were first unpacked to a temporary <tt>boolean[]</tt> array with <tt>count</tt> elements
     * and then the contents of the temporary array were packed into positions
     * <tt>destPos..destPos+count-1</tt> of <tt>dest</tt>.
     *
     * @param dest    the destination <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination buffer.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be copied (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     * @throws NullPointerException if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void copyBits(LongBuffer dest, long destPos, LongBuffer src, long srcPos, long count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyBits(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> bits, packed into <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> buffer, starting from the bit <tt>#destPos</tt>,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies bits in normal order:
     * bit <tt>#srcPos</tt> of <tt>src</tt> to bit <tt>#destPos</tt> of <tt>dest</tt>, then
     * bit <tt>#srcPos+1</tt> of <tt>src</tt> to bit <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * bit <tt>#srcPos+2</tt> of <tt>src</tt> to bit <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * bit <tt>#srcPos+count-1</tt> of <tt>src</tt> to bit <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies bits in reverse order:
     * bit <tt>#srcPos+count-1</tt> of <tt>src</tt> to bit <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * bit <tt>#srcPos+count-2</tt> of <tt>src</tt> to bit <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * bit <tt>#srcPos</tt> of <tt>src</tt> to bit <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyBits(LongBuffer, long, LongBuffer, long, long)}
     * method that chooses the suitable order automatically.
     *
     * @param dest         the destination <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param destPos      position of the first written bit in the destination buffer.
     * @param src          the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos       position of the first read bit in the source buffer.
     * @param count        the number of bits to be copied (must be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the bits will be copied in the reverse order.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     * @throws NullPointerException if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @see #copyBits(LongBuffer, long, LongBuffer, long, long)
     */
    public static void copyBits(LongBuffer dest, long destPos, LongBuffer src, long srcPos, long count,
        boolean reverseOrder)
    {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, copyBits_method_impl)
        //  dest\[([^\]]+)\]\s*=\s*([^;]*) ==> dest.put($1, $2);;
        //  (src|dest)\[([^\]]+)\] ==> $1.get($2);;
        //  (synchronized\s*\()(\s*\w+\s*)\) ==> $1getLock($2));;
        //  //Start_reverseOrder.*?//End_reverseOrder.*?(?:\r(?!\n)|\n|\r\n) ==>
        //  if (reverseOrder);;
        //  //Start_nothingToDo.*?//End_nothingToDo.*?(\r(?!\n)|\n|\r\n) ==> $1;;
        //  //Start_sPrev.*?//End_sPrev.*?(\r(?!\n)|\n|\r\n) ==> sPrev = cnt == 0 ? 0 : src.get(sPos);
                    // Unlike PackedBitArrays.copyBits, IndexOutOfBoundException is possible here when count=0,
                    // because the reverseOrder=true argument may be passed in this case$1;;
        //  System\.arraycopy\((\w+,\s*\w+,\s*)(\w+,\s*\w+,\s*)(\w+)\) ==>
        //  JBuffers.copyLongBuffer($2$1$3, reverseOrder)   !! Auto-generated: NOT EDIT !! >>
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
        if (reverseOrder)        {
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
                        dest.put(dPos + cnt, (src.get(sPos + cnt) & maskFinish) | (dest.get(dPos + cnt) & ~maskFinish));
                    }
                }
                JBuffers.copyLongBuffer(dest, dPos, src, sPos, cnt, reverseOrder);
                if (cntStart > 0) {
                    synchronized (getLock(dest)) {
                        dest.put(dPosStart, (src.get(sPosStart) & maskStart) | (dest.get(dPosStart) & ~maskStart));
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
                // Now the bit #0 of dest.get(dPos) corresponds to the bit #sPosRem of src.get(sPos)
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
                for (; dPos > dPosMin; ) { // cnt times
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
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Swaps <tt>count</tt> bits, packed into <tt>first</tt> buffer,
     * starting from the bit <tt>#firstPos</tt>,
     * with <tt>count</tt> bits, packed into <tt>second</tt> buffer,
     * starting from the bit <tt>#secondPos</tt>.
     *
     * <p>Some bits may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * @param first     the first <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param firstPos  starting index of bit to exchange in the first <tt>LongBuffer</tt>.
     * @param second    the second <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param secondPos starting index of bit to exchange in the second <tt>LongBuffer</tt>.
     * @param count     the number of bits to be exchanged (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     * @throws NullPointerException if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void swapBits(LongBuffer first, long firstPos, LongBuffer second, long secondPos, long count) {
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
     * Copies <tt>count</tt> bits from <tt>src</tt> array, starting from the element <tt>#srcPos</tt>,
     * to packed <tt>dest</tt> buffer, starting from the bit <tt>#destPos</tt>.
     *
     * @param dest    the destination <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination buffer.
     * @param src     the source array (unpacked <tt>boolean</tt> values).
     * @param srcPos  position of the first read bit in the source array.
     * @param count   the number of bits to be packed (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds or buffer limit.
     * @throws NullPointerException if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void packBits(LongBuffer dest, long destPos, boolean[] src, int srcPos, int count) {
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
                ;
            srcPos += 32;
            dest.put(k, ((long)low & 0xFFFFFFFFL) | (((long)high) << 32));
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
     * Copies <tt>count</tt> bits, packed into <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>,
     * to <tt>dest</tt> array, starting from the element <tt>#destPos</tt>.
     *
     * @param dest    the destination array (unpacked <tt>boolean</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be unpacked (must be &gt;=0).
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds or buffer limit.
     * @throws NullPointerException if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void unpackBits(boolean[] dest, int destPos, LongBuffer src, long srcPos, int count) {
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
            dest[destPos] = (src.get((int) (srcPos >>> 6)) & (1L << (srcPos & 63))) != 0L;
        }
    }

    /**
     * Fills <tt>count</tt> bits in the packed <tt>dest</tt> buffer, starting from the bit <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * @param dest    the destination <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination buffer.
     * @param count   the number of bits to be filled (must be &gt;=0).
     * @param value   new value of all filled bits (<tt>false</tt> means the bit 0, <tt>true</tt> means the bit 1).
     * @throws IndexOutOfBoundsException if filling would cause access of data outside buffer limit.
     * @throws NullPointerException if <tt>dest</tt> is <tt>null</tt>.
     */
    public static void fillBits(LongBuffer dest, long destPos, long count, boolean value) {
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
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical NOT of corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>.
     * The packed <tt>long[]</tt> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void notBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, notBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
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
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical AND of them and corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>.
     * The packed <tt>long[]</tt> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void andBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, andBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)    !! Auto-generated: NOT EDIT !! >>
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
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical OR of them and corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>.
     * The packed <tt>long[]</tt> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void orBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, orBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
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
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical XOR of them and corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>.
     * The packed <tt>long[]</tt> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void xorBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, xorBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
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
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical AND of them and <i>inverted</i> corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>.
     * The packed <tt>long[]</tt> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void andNotBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, andNotBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
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
     * Replaces <tt>count</tt> bits,
     * packed in <tt>dest</tt> array, starting from the bit <tt>#destPos</tt>,
     * with the logical OR of them and <i>inverted</i> corresponding <tt>count</tt> bits,
     * packed in <tt>src</tt> buffer, starting from the bit <tt>#srcPos</tt>.
     * The packed <tt>long[]</tt> Java array stores bits as described in {@link PackedBitArrays} class.
     *
     * @param dest    the destination array (bits are packed in <tt>long</tt> values).
     * @param destPos position of the first written bit in the destination array.
     * @param src     the source <tt>LongBuffer</tt> (bits are packed into <tt>long</tt> values).
     * @param srcPos  position of the first read bit in the source buffer.
     * @param count   the number of bits to be replaced (must be &gt;=0).
     * @throws IndexOutOfBoundsException if accessing bits would cause access of data outside array bounds.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     */
    public static void orNotBits(long[] dest, long destPos, LongBuffer src, long srcPos, long count) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, orNotBits_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1)   !! Auto-generated: NOT EDIT !! >>
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
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;highIndex</tt>
     * and the bit <tt>#k</tt> in the packed <tt>src</tt> bit buffer is equal to <tt>value</tt>,
     * or <tt>-1</tt> if there is no such bits.
     *
     * <p>If <tt>lowIndex&gt;=highIndex</tt>, this method returns <tt>-1</tt>.
     *
     * @param src       the searched packed bit buffer.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of bit to be found.
     * @return          the index of the first occurrence of this bit in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this bit does not occur
     *                  or if <tt>lowIndex&gt;=highIndex</tt>.
     * @throws NullPointerException      if <tt>buffer</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>lowIndex</tt> is negative or
     *                                   if <tt>highIndex</tt> is greater than <tt>src.limit()*64</tt>.
     * @see #lastIndexOfBit(LongBuffer, long, long, boolean)
     */
    public static long indexOfBit(LongBuffer src, long lowIndex, long highIndex, boolean value) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, indexOfBit_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1);;
        //  src\.length ==> src.limit();;
        //  (numberOfTrailingZeros\() ==> PackedBitArrays.$1   !! Auto-generated: NOT EDIT !! >>
        if (lowIndex < 0)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: low index = " + lowIndex);
        if (highIndex > ((long)src.limit()) << 6)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: high index = " + highIndex);
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
            int index = PackedBitArrays.numberOfTrailingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskStart);
            if (index != 64)
                return fromAligned + index;
            count -= cntStart;
            sPos++;
            fromAligned += 64;
        }
        if (value) {
            for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; sPos++, fromAligned += 64) {
                int index = PackedBitArrays.numberOfTrailingZeros(src.get(sPos));
                if (index != 64) {
                    return fromAligned + index;
                }
            }
        } else {
            for (int sPosMax = sPos + (int) (count >>> 6); sPos < sPosMax; sPos++, fromAligned += 64) {
                int index = PackedBitArrays.numberOfTrailingZeros(~src.get(sPos));
                if (index != 64) {
                    return fromAligned + index;
                }
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = (1L << cntFinish) - 1; // cntFinish times 1 (from the left)
            int index = PackedBitArrays.numberOfTrailingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskFinish);
            if (index != 64) {
                return fromAligned + index;
            }
        }
        return -1;
        //<<Repeat.IncludeEnd>>
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=lowIndex</tt>
     * and the bit <tt>#k</tt> in the packed <tt>src</tt> bit buffer is equal to <tt>value</tt>,
     * or <tt>-1</tt> if there is no such bits.
     *
     * <p>If <tt>highIndex&lt;=lowIndex</tt>, this method returns <tt>-1</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfBit(LongBuffer, long, long, boolean)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param src       the searched packed bit buffer.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of bit to be found.
     * @return          the index of the last occurrence of this bit in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this bit does not occur
     *                  or if <tt>lowIndex&gt;=highIndex</tt>.
     * @throws NullPointerException      if <tt>src</tt> is <tt>null</tt>.
     * @throws IndexOutOfBoundsException if <tt>lowIndex</tt> is negative or
     *                                   if <tt>highIndex</tt> is greater than <tt>src.length*64</tt>.
     */
    public static long lastIndexOfBit(LongBuffer src, long lowIndex, long highIndex, boolean value) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, lastIndexOfBit_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1);;
        //  src\.length ==> src.limit();;
        //  (numberOfLeadingZeros\() ==> PackedBitArrays.$1   !! Auto-generated: NOT EDIT !! >>
        if (lowIndex < 0)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: low index = " + lowIndex);
        if (highIndex > ((long) src.limit()) << 6)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: high index = " + highIndex);
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
            int index = PackedBitArrays.numberOfLeadingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskStart);
            if (index != 64) {
                return fromAligned - index;
            }
            count -= cntStart;
            sPos--;
            fromAligned -= 64;
        }
        if (value) {
            for (int sPosMin = sPos - (int) (count >>> 6); sPos > sPosMin; sPos--, fromAligned -= 64) {
                int index = PackedBitArrays.numberOfLeadingZeros(src.get(sPos));
                if (index != 64) {
                    return fromAligned - index;
                }
            }
        } else {
            for (int sPosMin = sPos - (int) (count >>> 6); sPos > sPosMin; sPos--, fromAligned -= 64) {
                int index = PackedBitArrays.numberOfLeadingZeros(~src.get(sPos));
                if (index != 64) {
                    return fromAligned - index;
                }
            }
        }
        int cntFinish = (int) (count & 63);
        if (cntFinish > 0) {
            long maskFinish = -1L << (64 - cntFinish); // cntFinish times 1 (from the right)
            int index = PackedBitArrays.numberOfLeadingZeros((value ? src.get(sPos) : ~src.get(sPos)) & maskFinish);
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
     * @param fromIndex the initial checked bit index in <tt>src</tt>, inclusive.
     * @param toIndex   the end checked bit index in <tt>src</tt>, exclusive.
     * @return          the number of high bits (1) in the given fragment of the given packed bit buffer.
     * @throws  NullPointerException if the <tt>src</tt> argument is <tt>null</tt>.
     * @throws  IndexOutOfBoundsException
     *          if <tt>fromIndex</tt> or <tt>toIndex</tt> are negative,
     *          if <tt>toIndex</tt> is greater than <tt>src.limit() * 64</tt>,
     *          or if <tt>fromIndex</tt> is greater than <tt>startIndex</tt>
     */
    public static long cardinality(LongBuffer src, long fromIndex, long toIndex) {
        //<<Repeat(INCLUDE_FROM_FILE, PackedBitArrays.java, cardinality_method_impl)
        //  src\[([^\]]+)\] ==> src.get($1);;
        //  src\.length ==> src.limit();;
        //  bitCount\( ==> PackedBitArrays.bitCount(   !! Auto-generated: NOT EDIT !! >>
        if (src == null)
            throw new NullPointerException("Null src argument in cardinality method");
        if (fromIndex < 0)
            throw new ArrayIndexOutOfBoundsException("Bit array index out of range: initial index = " + fromIndex);
        if (toIndex > ((long) src.limit()) << 6)
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
            result += PackedBitArrays.bitCount(src.get(sPos) & maskStart);
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
                long value = src.get(sPos);
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
            result += PackedBitArrays.bitCount(src.get(sPos) & maskFinish);
        }
        return result;
        //<<Repeat.IncludeEnd>>
    }
}
