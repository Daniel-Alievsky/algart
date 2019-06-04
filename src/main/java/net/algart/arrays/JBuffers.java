/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.nio.*;

/**
 * <p>Some operations for Java NIO buffers manipulation in the same manner as array operations from
 * {@link JArrays} and <tt>java.util.Arrays</tt> classes.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class JBuffers {
    private static final int FILL_NON_BLOCKED_LEN = 256; // must be 2^k
    private static final int FILL_BLOCK_LEN = 4 * 1024; // must be 2^k
    private static final int ZERO_FILL_BLOCK_LEN = 4 * 1024; // must be 2^k
    private static final boolean OPTIMIZE_BYTE_MIN_MAX_BY_TABLES = false; // antioptimization under Java 1.6+

    private JBuffers() {}

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double
     */
    /**
     * Copies <tt>count</tt> byte elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for ByteBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>ByteBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>ByteBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyByteBuffer(ByteBuffer dest, int destPos, ByteBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyByteBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> byte elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyByteBuffer(ByteBuffer, int, ByteBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>ByteBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>ByteBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyByteBuffer(ByteBuffer dest, int destPos, ByteBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> byte elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> byte elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>ByteBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>ByteBuffer</tt>.
     * @param second    the second <tt>ByteBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>ByteBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapByteBuffer(ByteBuffer first, int firstPos, ByteBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            byte v1 = first.get(firstPos);
            byte v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Copies <tt>count</tt> char elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for CharBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>CharBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>CharBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyCharBuffer(CharBuffer dest, int destPos, CharBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyCharBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> char elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyCharBuffer(CharBuffer, int, CharBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>CharBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>CharBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyCharBuffer(CharBuffer dest, int destPos, CharBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> char elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> char elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>CharBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>CharBuffer</tt>.
     * @param second    the second <tt>CharBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>CharBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapCharBuffer(CharBuffer first, int firstPos, CharBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            char v1 = first.get(firstPos);
            char v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <tt>count</tt> short elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for ShortBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>ShortBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>ShortBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyShortBuffer(ShortBuffer dest, int destPos, ShortBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyShortBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> short elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyShortBuffer(ShortBuffer, int, ShortBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>ShortBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>ShortBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyShortBuffer(ShortBuffer dest, int destPos, ShortBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> short elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> short elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>ShortBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>ShortBuffer</tt>.
     * @param second    the second <tt>ShortBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>ShortBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapShortBuffer(ShortBuffer first, int firstPos, ShortBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            short v1 = first.get(firstPos);
            short v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <tt>count</tt> int elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for IntBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>IntBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>IntBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyIntBuffer(IntBuffer dest, int destPos, IntBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyIntBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> int elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyIntBuffer(IntBuffer, int, IntBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>IntBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>IntBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyIntBuffer(IntBuffer dest, int destPos, IntBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> int elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> int elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>IntBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>IntBuffer</tt>.
     * @param second    the second <tt>IntBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>IntBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapIntBuffer(IntBuffer first, int firstPos, IntBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            int v1 = first.get(firstPos);
            int v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <tt>count</tt> long elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for LongBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>LongBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>LongBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyLongBuffer(LongBuffer dest, int destPos, LongBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyLongBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> long elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyLongBuffer(LongBuffer, int, LongBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>LongBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>LongBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyLongBuffer(LongBuffer dest, int destPos, LongBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> long elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> long elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>LongBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>LongBuffer</tt>.
     * @param second    the second <tt>LongBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>LongBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapLongBuffer(LongBuffer first, int firstPos, LongBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            long v1 = first.get(firstPos);
            long v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <tt>count</tt> float elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for FloatBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>FloatBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>FloatBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyFloatBuffer(FloatBuffer dest, int destPos, FloatBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyFloatBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> float elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyFloatBuffer(FloatBuffer, int, FloatBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>FloatBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>FloatBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyFloatBuffer(FloatBuffer dest, int destPos, FloatBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> float elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> float elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>FloatBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>FloatBuffer</tt>.
     * @param second    the second <tt>FloatBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>FloatBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapFloatBuffer(FloatBuffer first, int firstPos, FloatBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            float v1 = first.get(firstPos);
            float v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <tt>count</tt> double elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index.
     * It is an analog of standard <tt>System.arraycopy</tt> method for DoubleBuffer.
     *
     * <p>This method works correctly even if <tt>src == dest</tt>
     * and the copied areas overlap,
     * i.e. if <tt>Math.abs(destPos - srcPos) &lt; count</tt>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <tt>srcPos..srcPos+count-1</tt> in <tt>src</tt> buffer
     * were first copied to a temporary array with <tt>count</tt> elements
     * and then the contents of the temporary array were copied into positions
     * <tt>destPos..destPos+count-1</tt> in <tt>dest</tt> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <tt>DoubleBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param src     the source <tt>DoubleBuffer</tt>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyDoubleBuffer(DoubleBuffer dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null)
            return;
        copyDoubleBuffer(dest, destPos, src, srcPos, count,
            src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <tt>count</tt> double elements from <tt>src</tt> buffer,
     * starting from <tt>srcPos</tt> index,
     * to the <tt>dest</tt> buffer, starting from <tt>destPos</tt> index,
     * in normal or reverse order depending on <tt>reverseOrder</tt> argument.
     *
     * <p>If <tt>reverseOrder</tt> flag is <tt>false</tt>, this method copies elements in normal order:
     * element <tt>#srcPos</tt> of <tt>src</tt> to element <tt>#destPos</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+1</tt> of <tt>src</tt> to element <tt>#destPos+1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+2</tt> of <tt>src</tt> to element <tt>#destPos+2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>.
     * If <tt>reverseOrder</tt> flag is <tt>true</tt>, this method copies elements in reverse order:
     * element <tt>#srcPos+count-1</tt> of <tt>src</tt> to element  <tt>#destPos+count-1</tt> of <tt>dest</tt>, then
     * element <tt>#srcPos+count-2</tt> of <tt>src</tt> to element  <tt>#destPos+count-2</tt> of <tt>dest</tt>, ..., then
     * element <tt>#srcPos</tt> of <tt>src</tt> to element  <tt>#destPos</tt> of <tt>dest</tt>.
     * Usually, copying in reverse order is slower, but it is necessary if <tt>src</tt>
     * and <tt>dest</tt> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <tt>src==dest</tt>, you may use {@link #copyDoubleBuffer(DoubleBuffer, int, DoubleBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <tt>DoubleBuffer</tt>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <tt>DoubleBuffer</tt>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <tt>true</tt>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyDoubleBuffer(DoubleBuffer dest, int destPos, DoubleBuffer src, int srcPos, int count,
        boolean reverseOrder)
    {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--)
                dest.put(destPos2, src.get(srcPos2));
        } else {
            src = src.duplicate();
            dest = dest.duplicate();
            src.position(srcPos);
            dest.position(destPos);
            src.limit(srcPos + count);
            dest.put(src);
        }
    }

    /**
     * Swaps <tt>count</tt> double elements in <tt>first</tt> buffer,
     * starting from <tt>firstPos</tt> index,
     * with <tt>count</tt> double elements in <tt>second</tt> buffer,
     * starting from <tt>secondPos</tt> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <tt>first==second</tt> and <tt>Math.abs(firstIndex - secondIndex) &lt; count</tt>,
     * or if <tt>first</tt> and <tt>second</tt> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <tt>DoubleBuffer</tt>.
     * @param firstPos  starting index of element to exchange in the first <tt>DoubleBuffer</tt>.
     * @param second    the second <tt>DoubleBuffer</tt>.
     * @param secondPos starting index of element to exchange in the second <tt>DoubleBuffer</tt>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <tt>first</tt> or <tt>second</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapDoubleBuffer(DoubleBuffer first, int firstPos, DoubleBuffer second, int secondPos, int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            double v1 = first.get(firstPos);
            double v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double
     */
    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>ByteBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillByteBuffer(ByteBuffer dest, int destPos, int count, byte value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            byte[] arr;
            if (value != 0) {
                arr = new byte[FILL_BLOCK_LEN];
                JArrays.fillByteArray(arr, value);
            } else {
                arr = new byte[ZERO_FILL_BLOCK_LEN];
            }
            ByteBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>CharBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillCharBuffer(CharBuffer dest, int destPos, int count, char value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            char[] arr;
            if (value != 0) {
                arr = new char[FILL_BLOCK_LEN];
                JArrays.fillCharArray(arr, value);
            } else {
                arr = new char[ZERO_FILL_BLOCK_LEN];
            }
            CharBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }

    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>ShortBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillShortBuffer(ShortBuffer dest, int destPos, int count, short value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            short[] arr;
            if (value != 0) {
                arr = new short[FILL_BLOCK_LEN];
                JArrays.fillShortArray(arr, value);
            } else {
                arr = new short[ZERO_FILL_BLOCK_LEN];
            }
            ShortBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }

    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>IntBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillIntBuffer(IntBuffer dest, int destPos, int count, int value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            int[] arr;
            if (value != 0) {
                arr = new int[FILL_BLOCK_LEN];
                JArrays.fillIntArray(arr, value);
            } else {
                arr = new int[ZERO_FILL_BLOCK_LEN];
            }
            IntBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }

    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>LongBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillLongBuffer(LongBuffer dest, int destPos, int count, long value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            long[] arr;
            if (value != 0) {
                arr = new long[FILL_BLOCK_LEN];
                JArrays.fillLongArray(arr, value);
            } else {
                arr = new long[ZERO_FILL_BLOCK_LEN];
            }
            LongBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }

    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>FloatBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillFloatBuffer(FloatBuffer dest, int destPos, int count, float value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            float[] arr;
            if (value != 0) {
                arr = new float[FILL_BLOCK_LEN];
                JArrays.fillFloatArray(arr, value);
            } else {
                arr = new float[ZERO_FILL_BLOCK_LEN];
            }
            FloatBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }

    /**
     * Fills <tt>count</tt> elements in the <tt>dest</tt> buffer, starting from the element <tt>#destPos</tt>,
     * by the specified value. <i>Be careful:</i> the second <tt>int</tt> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <tt>java.util.Arrays.fill</tt> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <tt>DoubleBuffer</tt>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limit.
     */
    public static void fillDoubleBuffer(DoubleBuffer dest, int destPos, int count, double value) {
        JArrays.rangeCheck(dest.limit(), destPos, count);
        if (count >= FILL_NON_BLOCKED_LEN) {
            double[] arr;
            if (value != 0) {
                arr = new double[FILL_BLOCK_LEN];
                JArrays.fillDoubleArray(arr, value);
            } else {
                arr = new double[ZERO_FILL_BLOCK_LEN];
            }
            DoubleBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= arr.length; count -= arr.length)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double
     */
    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>ByteBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfByte(ByteBuffer, int, int, byte)
     */
    public static int indexOfByte(ByteBuffer buffer, int lowIndex, int highIndex, byte value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfByte(ByteBuffer, int, int, byte)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfByte(ByteBuffer, int, int, byte)
     */
    public static int lastIndexOfByte(ByteBuffer buffer, int lowIndex, int highIndex, byte value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>CharBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfChar(CharBuffer, int, int, char)
     */
    public static int indexOfChar(CharBuffer buffer, int lowIndex, int highIndex, char value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfChar(CharBuffer, int, int, char)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfChar(CharBuffer, int, int, char)
     */
    public static int lastIndexOfChar(CharBuffer buffer, int lowIndex, int highIndex, char value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>ShortBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfShort(ShortBuffer, int, int, short)
     */
    public static int indexOfShort(ShortBuffer buffer, int lowIndex, int highIndex, short value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfShort(ShortBuffer, int, int, short)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfShort(ShortBuffer, int, int, short)
     */
    public static int lastIndexOfShort(ShortBuffer buffer, int lowIndex, int highIndex, short value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>IntBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfInt(IntBuffer, int, int, int)
     */
    public static int indexOfInt(IntBuffer buffer, int lowIndex, int highIndex, int value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfInt(IntBuffer, int, int, int)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfInt(IntBuffer, int, int, int)
     */
    public static int lastIndexOfInt(IntBuffer buffer, int lowIndex, int highIndex, int value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>LongBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfLong(LongBuffer, int, int, long)
     */
    public static int indexOfLong(LongBuffer buffer, int lowIndex, int highIndex, long value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfLong(LongBuffer, int, int, long)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfLong(LongBuffer, int, int, long)
     */
    public static int lastIndexOfLong(LongBuffer buffer, int lowIndex, int highIndex, long value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>FloatBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfFloat(FloatBuffer, int, int, float)
     */
    public static int indexOfFloat(FloatBuffer buffer, int lowIndex, int highIndex, float value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfFloat(FloatBuffer, int, int, float)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfFloat(FloatBuffer, int, int, float)
     */
    public static int lastIndexOfFloat(FloatBuffer buffer, int lowIndex, int highIndex, float value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <tt>k</tt>, so that <tt>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=buffer.limit()</tt> or <tt>lowIndex&gt;=highIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * @param buffer    the searched <tt>DoubleBuffer</tt>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <tt>buffer.limit()</tt> to search all remaining elements.
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #lastIndexOfDouble(DoubleBuffer, int, int, double)
     */
    public static int indexOfDouble(DoubleBuffer buffer, int lowIndex, int highIndex, double value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        int maxPlus1 = Math.min(buffer.limit(), highIndex);
        for (; lowIndex < maxPlus1; lowIndex++) {
            if (buffer.get(lowIndex) == value) {
                return lowIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>buffer.get(k)==value</tt>,
     * or <tt>-1</tt> if there is no such buffer element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=buffer.limit()</tt>, the result is the same as if <tt>highIndex==buffer.limit()</tt>.
     *
     * <p>Note that <tt>lowIndex</tt> and <tt>highIndex</tt> arguments have the same sense as in
     * {@link #indexOfDouble(DoubleBuffer, int, int, double)} method:
     * they describes the search index range <tt>lowIndex&lt;=k&lt;highIndex</tt>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <tt>0</tt> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in range <tt>lowIndex..highIndex-1</tt>,
     *                  or <tt>-1</tt> if this value does not occur
     *                  or if <tt>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</tt>.
     * @throws NullPointerException if <tt>buffer</tt> is <tt>null</tt>.
     * @see #indexOfDouble(DoubleBuffer, int, int, double)
     */
    public static int lastIndexOfDouble(DoubleBuffer buffer, int lowIndex, int highIndex, double value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        for (; highIndex > min; ) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     * The byte elements are considered to be unsigned: <tt>min(a,b)=(a&amp;0xFF)&lt;(b&amp;0xFF)?a:b</tt>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ByteBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minByteArrayAndBuffer(byte[] dest, int destPos, ByteBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (OPTIMIZE_BYTE_MIN_MAX_BY_TABLES) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] = JArrays.MinMaxTables.MIN_TABLE[
                    ((src.get(srcPos) & 0xFF) << 8) | (dest[destPos] & 0xFF)];
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                byte v = src.get(srcPos);
                if ((v & 0xFF) < (dest[destPos] & 0xFF))
                    dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     * The byte elements are considered to be unsigned: <tt>min(a,b)=(a&amp;0xFF)&lt;(b&amp;0xFF)?a:b</tt>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ByteBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxByteArrayAndBuffer(byte[] dest, int destPos, ByteBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (OPTIMIZE_BYTE_MIN_MAX_BY_TABLES) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] = JArrays.MinMaxTables.MAX_TABLE[
                    ((src.get(srcPos) & 0xFF) << 8) | (dest[destPos] & 0xFF)];
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                byte v = src.get(srcPos);
                if ((v & 0xFF) > (dest[destPos] & 0xFF))
                    dest[destPos] = v;
            }
        }
    }

    /*Repeat() short ==> char,,int,,long,,float,,double;;
               Short ==> Char,,Int,,Long,,Float,,Double;;
               (\s*&(?:amp;)?\s*0xFFFF) ==> ,,...;;
               (The\s+\w+\s+elements.*?<\/tt>\.) ==> ,,...
     */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     * The short elements are considered to be unsigned: <tt>min(a,b)=(a&amp;0xFFFF)&lt;(b&amp;0xFFFF)?a:b</tt>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ShortBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minShortArrayAndBuffer(short[] dest, int destPos, ShortBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            short v = src.get(srcPos);
            if ((v & 0xFFFF) < (dest[destPos] & 0xFFFF))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     * The short elements are considered to be unsigned: <tt>min(a,b)=(a&amp;0xFFFF)&lt;(b&amp;0xFFFF)?a:b</tt>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ShortBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxShortArrayAndBuffer(short[] dest, int destPos, ShortBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            short v = src.get(srcPos);
            if ((v & 0xFFFF) > (dest[destPos] & 0xFFFF))
                dest[destPos] = v;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>CharBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minCharArrayAndBuffer(char[] dest, int destPos, CharBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            char v = src.get(srcPos);
            if ((v) < (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>CharBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxCharArrayAndBuffer(char[] dest, int destPos, CharBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            char v = src.get(srcPos);
            if ((v) > (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>IntBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minIntArrayAndBuffer(int[] dest, int destPos, IntBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            int v = src.get(srcPos);
            if ((v) < (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>IntBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxIntArrayAndBuffer(int[] dest, int destPos, IntBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            int v = src.get(srcPos);
            if ((v) > (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>LongBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minLongArrayAndBuffer(long[] dest, int destPos, LongBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            long v = src.get(srcPos);
            if ((v) < (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>LongBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxLongArrayAndBuffer(long[] dest, int destPos, LongBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            long v = src.get(srcPos);
            if ((v) > (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>FloatBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minFloatArrayAndBuffer(float[] dest, int destPos, FloatBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            float v = src.get(srcPos);
            if ((v) < (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>FloatBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxFloatArrayAndBuffer(float[] dest, int destPos, FloatBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            float v = src.get(srcPos);
            if ((v) > (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>DoubleBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minDoubleArrayAndBuffer(double[] dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            double v = src.get(srcPos);
            if ((v) < (dest[destPos]))
                dest[destPos] = v;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the minimum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>DoubleBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxDoubleArrayAndBuffer(double[] dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            double v = src.get(srcPos);
            if ((v) > (dest[destPos]))
                dest[destPos] = v;
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
               (\s*&(?:amp;)?\s*0xFF) ==> ,,$1FF,, ,,...;;
               (The\s+\w+\s+elements.*?\.) ==> ,,$1,, ,,...
     */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFF)</tt>.
     * The byte elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ByteBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addByteBufferToArray(int[] dest, int destPos, ByteBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos) & 0xFF;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFF)*mult</tt>.
     * The byte elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ByteBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addByteBufferToArray(double[] dest, int destPos, ByteBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos) & 0xFF;
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos) & 0xFF;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos) & 0xFF) * mult;
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>CharBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addCharBufferToArray(int[] dest, int destPos, CharBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))*mult</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>CharBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addCharBufferToArray(double[] dest, int destPos, CharBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos);
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos)) * mult;
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFFFF)</tt>.
     * The short elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ShortBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addShortBufferToArray(int[] dest, int destPos, ShortBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos) & 0xFFFF;
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFFFF)*mult</tt>.
     * The short elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>ShortBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addShortBufferToArray(double[] dest, int destPos, ShortBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos) & 0xFFFF;
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos) & 0xFFFF;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos) & 0xFFFF) * mult;
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>IntBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addIntBufferToArray(int[] dest, int destPos, IntBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))*mult</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>IntBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addIntBufferToArray(double[] dest, int destPos, IntBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos);
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos)) * mult;
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>LongBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addLongBufferToArray(int[] dest, int destPos, LongBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))*mult</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>LongBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addLongBufferToArray(double[] dest, int destPos, LongBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos);
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos)) * mult;
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>FloatBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addFloatBufferToArray(int[] dest, int destPos, FloatBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))*mult</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>FloatBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addFloatBufferToArray(double[] dest, int destPos, FloatBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos);
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos)) * mult;
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>DoubleBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addDoubleBufferToArray(int[] dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] += src.get(srcPos);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the sum of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * multiplied by <tt>mult</tt> argument,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]+=(src.get(srcPos+i))*mult</tt>.
     *
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <tt>DoubleBuffer</tt>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <tt>src</tt> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addDoubleBufferToArray(double[] dest, int destPos, DoubleBuffer src, int srcPos, int count,
        double mult)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0)
            return;
        if (mult == 1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += src.get(srcPos);
            }
        } else if (mult == -1.0) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] += (src.get(srcPos)) * mult;
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() (int\s*v\s*=) ==> $1,,$1;;
               (\(int\))(dest|src) ==> $1$2,,$1$2;;
               byte ==> char,,short;;
               Byte ==> Char,,Short;;
               BYTE ==> CHAR,,SHORT;;
               (\s*&(?:amp;)?\s*0xFF) ==> ,,$1FF;;
               (0\.\.0xFF) ==> $1FF,,$1FF;;
               (The\s+\w+\s+elements.*?\.) ==> ,,$1
     */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     * If <tt>truncateOverflows</tt> argument is <tt>true</tt>, the difference is truncated
     * to <tt>0..0xFF</tt> range before assigning to <tt>dest</tt> elements.
     * The byte elements are considered to be unsigned.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>ByteBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <tt>0..0xFF</tt> range.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractByteBufferFromArray(byte[] dest, int destPos, ByteBuffer src, int srcPos, int count,
        boolean truncateOverflows)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = ((int)dest[destPos] & 0xFF) - ((int)src.get(srcPos) & 0xFF);
                dest[destPos] = v < 0 ? 0 : (byte)v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     * If <tt>truncateOverflows</tt> argument is <tt>true</tt>, the difference is truncated
     * to <tt>0..0xFFFF</tt> range before assigning to <tt>dest</tt> elements.
     *
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>CharBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <tt>0..0xFFFF</tt> range.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractCharBufferFromArray(char[] dest, int destPos, CharBuffer src, int srcPos, int count,
        boolean truncateOverflows)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = ((int)dest[destPos]) - ((int)src.get(srcPos));
                dest[destPos] = v < 0 ? 0 : (char)v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     * If <tt>truncateOverflows</tt> argument is <tt>true</tt>, the difference is truncated
     * to <tt>0..0xFFFF</tt> range before assigning to <tt>dest</tt> elements.
     * The short elements are considered to be unsigned.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>ShortBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <tt>0..0xFFFF</tt> range.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractShortBufferFromArray(short[] dest, int destPos, ShortBuffer src, int srcPos, int count,
        boolean truncateOverflows)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = ((int)dest[destPos] & 0xFFFF) - ((int)src.get(srcPos) & 0xFFFF);
                dest[destPos] = v < 0 ? 0 : (short)v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     * If <tt>truncateOverflows</tt> argument is <tt>true</tt>, the difference is truncated
     * to <tt>Integer.MIN_VALUE..Integer.MAX_VALUE</tt> range before assigning to <tt>dest</tt> elements.
     *
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>IntBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <tt>Integer.MIN_VALUE..Integer.MAX_VALUE</tt> range.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractIntBufferFromArray(int[] dest, int destPos, IntBuffer src, int srcPos, int count,
        boolean truncateOverflows)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                long v = (long)dest[destPos] - (long)src.get(srcPos);
                dest[destPos] = v < Integer.MIN_VALUE ? Integer.MIN_VALUE :
                v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)v;
        }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }

    /*Repeat() long ==> float,,double;;
               Long ==> Float,,Double
     */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>LongBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractLongBufferFromArray(long[] dest, int destPos, LongBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] -= src.get(srcPos);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>FloatBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractFloatBufferFromArray(float[] dest, int destPos, FloatBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] -= src.get(srcPos);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the difference of them and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</tt>.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>DoubleBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractDoubleBufferFromArray(double[] dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] -= src.get(srcPos);
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,long,,float,,double;;
               Byte ==> Char,,Short,,Long,,Float,,Double;;
               (\s*&\s*0xFF) ==> ,,$1FF,, ,,...;;
               \((long|float|double)\) ==> ,,...;;
               (\(The\s+\w+\s+elements.*?\.\)) ==> ,,$1,, ,,...
     */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</tt>.
     * (The byte elements are considered to be unsigned.)
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>ByteBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfByteArrayAndBuffer(byte[] dest, int destPos, ByteBuffer src, int srcPos,
        int count)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            byte v = src.get(srcPos);
            dest[destPos] = (dest[destPos] & 0xFF) >= (v & 0xFF) ?
                (byte)(dest[destPos] - v) :
                (byte)(v - dest[destPos]);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</tt>.
     *
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>CharBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfCharArrayAndBuffer(char[] dest, int destPos, CharBuffer src, int srcPos,
        int count)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            char v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                (char)(dest[destPos] - v) :
                (char)(v - dest[destPos]);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</tt>.
     * (The short elements are considered to be unsigned.)
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>ShortBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfShortArrayAndBuffer(short[] dest, int destPos, ShortBuffer src, int srcPos,
        int count)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            short v = src.get(srcPos);
            dest[destPos] = (dest[destPos] & 0xFFFF) >= (v & 0xFFFF) ?
                (short)(dest[destPos] - v) :
                (short)(v - dest[destPos]);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</tt>.
     *
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>LongBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfLongArrayAndBuffer(long[] dest, int destPos, LongBuffer src, int srcPos,
        int count)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            long v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                (dest[destPos] - v) :
                (v - dest[destPos]);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</tt>.
     *
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>FloatBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfFloatArrayAndBuffer(float[] dest, int destPos, FloatBuffer src, int srcPos,
        int count)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            float v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                (dest[destPos] - v) :
                (v - dest[destPos]);
        }
    }

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</tt>.
     *
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>DoubleBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfDoubleArrayAndBuffer(double[] dest, int destPos, DoubleBuffer src, int srcPos,
        int count)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            double v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                (dest[destPos] - v) :
                (v - dest[destPos]);
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Replaces <tt>count</tt> elements in <tt>dest</tt> array,
     * starting from the element <tt>#destPos</tt>,
     * with the absolute value of the difference of them
     * and corresponding <tt>count</tt> elements in <tt>src</tt> buffer,
     * starting from the element <tt>#srcPos</tt>:
     * <tt>dest[destPos+i]=|dest[destPos+i]-src[srcPos+i]|</tt>.
     * If <tt>truncateOverflows</tt> argument is <tt>true</tt>, the difference is truncated
     * to <tt>0..Integer.MAX_VALUE</tt> range before assigning to <tt>dest</tt> elements.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <tt>IntBuffer</tt>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <tt>Integer.MIN_VALUE..Integer.MAX_VALUE</tt> range.
     * @throws NullPointerException      if either <tt>src</tt> or <tt>dest</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>count</tt> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfIntArrayAndBuffer(int[] dest, int destPos, IntBuffer src, int srcPos, int count,
        boolean truncateOverflows)
    {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                long v = (long)dest[destPos] - (long)src.get(srcPos);
                if (v < 0)
                    v = -v;
                dest[destPos] = v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = src.get(srcPos);
                dest[destPos] = dest[destPos] >= v ?
                    dest[destPos] - v :
                    v - dest[destPos];
            }
        }
    }
}
