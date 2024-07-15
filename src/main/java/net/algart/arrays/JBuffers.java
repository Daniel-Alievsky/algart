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

import java.nio.*;

/**
 * <p>Some operations for Java NIO buffers manipulation in the same manner as array operations from
 * {@link JArrays} and <code>java.util.Arrays</code> classes.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class JBuffers {
    private static final int FILL_NON_BLOCKED_LEN = 256; // must be 2^k
    private static final int FILL_BLOCK_LEN = 4 * 1024; // must be 2^k
    private static final int ZERO_FILL_BLOCK_LEN = 4 * 1024; // must be 2^k
    private static final boolean OPTIMIZE_BYTE_MIN_MAX_BY_TABLES = false; // antioptimization under Java 1.6+

    private JBuffers() {
    }

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double
     */

    /**
     * Copies <code>count</code> byte elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for ByteBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>ByteBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>ByteBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyByteBuffer(ByteBuffer dest, int destPos, ByteBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyByteBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> byte elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyByteBuffer(ByteBuffer, int, ByteBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>ByteBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>ByteBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyByteBuffer(
            ByteBuffer dest,
            int destPos,
            ByteBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> byte elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> byte elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>ByteBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>ByteBuffer</code>.
     * @param second    the second <code>ByteBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>ByteBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapByteBuffer(
            ByteBuffer first,
            int firstPos,
            ByteBuffer second,
            int secondPos,
            int count) {
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
     * Copies <code>count</code> char elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for CharBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>CharBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>CharBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyCharBuffer(CharBuffer dest, int destPos, CharBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyCharBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> char elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyCharBuffer(CharBuffer, int, CharBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>CharBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>CharBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyCharBuffer(
            CharBuffer dest,
            int destPos,
            CharBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> char elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> char elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>CharBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>CharBuffer</code>.
     * @param second    the second <code>CharBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>CharBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapCharBuffer(
            CharBuffer first,
            int firstPos,
            CharBuffer second,
            int secondPos,
            int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            char v1 = first.get(firstPos);
            char v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <code>count</code> short elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for ShortBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>ShortBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>ShortBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyShortBuffer(ShortBuffer dest, int destPos, ShortBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyShortBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> short elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyShortBuffer(ShortBuffer, int, ShortBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>ShortBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>ShortBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyShortBuffer(
            ShortBuffer dest,
            int destPos,
            ShortBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> short elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> short elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>ShortBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>ShortBuffer</code>.
     * @param second    the second <code>ShortBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>ShortBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapShortBuffer(
            ShortBuffer first,
            int firstPos,
            ShortBuffer second,
            int secondPos,
            int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            short v1 = first.get(firstPos);
            short v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <code>count</code> int elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for IntBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>IntBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>IntBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyIntBuffer(IntBuffer dest, int destPos, IntBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyIntBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> int elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyIntBuffer(IntBuffer, int, IntBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>IntBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>IntBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyIntBuffer(
            IntBuffer dest,
            int destPos,
            IntBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> int elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> int elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>IntBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>IntBuffer</code>.
     * @param second    the second <code>IntBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>IntBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapIntBuffer(
            IntBuffer first,
            int firstPos,
            IntBuffer second,
            int secondPos,
            int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            int v1 = first.get(firstPos);
            int v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <code>count</code> long elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for LongBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>LongBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyLongBuffer(LongBuffer dest, int destPos, LongBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyLongBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> long elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyLongBuffer(LongBuffer, int, LongBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>LongBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>LongBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyLongBuffer(
            LongBuffer dest,
            int destPos,
            LongBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> long elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> long elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>LongBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>LongBuffer</code>.
     * @param second    the second <code>LongBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>LongBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapLongBuffer(
            LongBuffer first,
            int firstPos,
            LongBuffer second,
            int secondPos,
            int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            long v1 = first.get(firstPos);
            long v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <code>count</code> float elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for FloatBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>FloatBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyFloatBuffer(FloatBuffer dest, int destPos, FloatBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyFloatBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> float elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyFloatBuffer(FloatBuffer, int, FloatBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>FloatBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>FloatBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyFloatBuffer(
            FloatBuffer dest,
            int destPos,
            FloatBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> float elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> float elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>FloatBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>FloatBuffer</code>.
     * @param second    the second <code>FloatBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>FloatBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapFloatBuffer(
            FloatBuffer first,
            int firstPos,
            FloatBuffer second,
            int secondPos,
            int count) {
        JArrays.rangeCheck(first.limit(), firstPos, second.limit(), secondPos, count);
        for (int firstPosMax = firstPos + count; firstPos < firstPosMax; firstPos++, secondPos++) {
            float v1 = first.get(firstPos);
            float v2 = second.get(secondPos);
            first.put(firstPos, v2);
            second.put(secondPos, v1);
        }
    }

    /**
     * Copies <code>count</code> double elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index.
     * It is an analog of standard <code>System.arraycopy</code> method for DoubleBuffer.
     *
     * <p>This method works correctly even if <code>src == dest</code>
     * and the copied areas overlap,
     * i.e. if <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code> in <code>src</code> buffer
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> in <code>dest</code> buffer.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest    the destination <code>DoubleBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  starting index of element to be copied.
     * @param count   the number of elements to be copied (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyDoubleBuffer(DoubleBuffer dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        if (src == dest && srcPos == destPos && src != null) {
            return;
        }
        copyDoubleBuffer(dest, destPos, src, srcPos, count,
                src == dest && srcPos <= destPos && srcPos + count > destPos);
    }

    /**
     * Copies <code>count</code> double elements from <code>src</code> buffer,
     * starting from <code>srcPos</code> index,
     * to the <code>dest</code> buffer, starting from <code>destPos</code> index,
     * in normal or reverse order depending on <code>reverseOrder</code> argument.
     *
     * <p>If <code>reverseOrder</code> flag is <code>false</code>, this method copies elements in normal order:
     * element <code>#srcPos</code> of <code>src</code>
     * to element <code>#destPos</code> of <code>dest</code>, then
     * element <code>#srcPos+1</code> of <code>src</code>
     * to element <code>#destPos+1</code> of <code>dest</code>, then
     * element <code>#srcPos+2</code> of <code>src</code>
     * to element <code>#destPos+2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>.
     * If <code>reverseOrder</code> flag is <code>true</code>, this method copies elements in reverse order:
     * element <code>#srcPos+count-1</code> of <code>src</code>
     * to element  <code>#destPos+count-1</code> of <code>dest</code>, then
     * element <code>#srcPos+count-2</code> of <code>src</code>
     * to element  <code>#destPos+count-2</code> of <code>dest</code>, ..., then
     * element <code>#srcPos</code> of <code>src</code> to element  <code>#destPos</code> of <code>dest</code>.
     * Usually, copying in reverse order is slower, but it is necessary if <code>src</code>
     * and <code>dest</code> are the same buffer or views or the same data (for example,
     * buffers mapped to the same file), the copied areas overlap and
     * destination position is greater than source position.
     * If <code>src==dest</code>, you may use {@link #copyDoubleBuffer(DoubleBuffer, int, DoubleBuffer, int, int)}
     * method that chooses the suitable order automatically.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param dest         the destination <code>DoubleBuffer</code>.
     * @param destPos      starting index of element to replace.
     * @param src          the source <code>DoubleBuffer</code>.
     * @param srcPos       starting index of element to be copied.
     * @param count        the number of elements to be copied (should be &gt;=0).
     * @param reverseOrder if <code>true</code>, the elements will be copied in the reverse order.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void copyDoubleBuffer(
            DoubleBuffer dest,
            int destPos,
            DoubleBuffer src,
            int srcPos,
            int count,
            boolean reverseOrder) {
        JArrays.rangeCheck(src.limit(), srcPos, dest.limit(), destPos, count);
        if (reverseOrder) {
            int srcPos2 = srcPos + count - 1;
            int destPos2 = destPos + count - 1;
            for (; srcPos2 >= srcPos; srcPos2--, destPos2--) {
                dest.put(destPos2, src.get(srcPos2));
            }
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
     * Swaps <code>count</code> double elements in <code>first</code> buffer,
     * starting from <code>firstPos</code> index,
     * with <code>count</code> double elements in <code>second</code> buffer,
     * starting from <code>secondPos</code> index.
     *
     * <p>Some elements may be swapped incorrectly if the swapped areas overlap,
     * i.e. if <code>first==second</code> and <code>Math.abs(firstIndex - secondIndex) &lt; count</code>,
     * or if <code>first</code> and <code>second</code> are views of the same data
     * (for example, buffers mapped to the same file) and the corresponding areas of this data
     * overlap.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffers.
     *
     * @param first     the first <code>DoubleBuffer</code>.
     * @param firstPos  starting index of element to exchange in the first <code>DoubleBuffer</code>.
     * @param second    the second <code>DoubleBuffer</code>.
     * @param secondPos starting index of element to exchange in the second <code>DoubleBuffer</code>.
     * @param count     the number of elements to be exchanged (should be &gt;=0).
     * @throws NullPointerException      if either <code>first</code> or <code>second</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside buffer limits.
     */
    public static void swapDoubleBuffer(
            DoubleBuffer first,
            int firstPos,
            DoubleBuffer second,
            int secondPos,
            int count) {
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
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>ByteBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>CharBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }

    /**
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>ShortBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }

    /**
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>IntBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }

    /**
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>LongBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }

    /**
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>FloatBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }

    /**
     * Fills <code>count</code> elements in the <code>dest</code> buffer,
     * starting from the element <code>#destPos</code>,
     * by the specified value. <i>Be careful:</i> the second <code>int</code> argument in this method
     * is the number of filled element, but not the end filled index
     * as in <code>java.util.Arrays.fill</code> methods.
     *
     * <p>This method does not modify <i>limit</i>, <i>position</i> and <i>mark</i> properties
     * of the passed buffer.
     *
     * @param dest    the filled <code>DoubleBuffer</code>.
     * @param destPos starting index of element to replace.
     * @param count   the number of elements to be filled (should be &gt;=0).
     * @param value   the filler.
     * @throws NullPointerException      if <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
            for (; count >= arr.length; count -= arr.length) {
                destDup.put(arr);
            }
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++) {
                dest.put(destPos + k, value);
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double
     */

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>ByteBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfByte(ByteBuffer, int, int, byte)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfByte(ByteBuffer, int, int, byte)
     */
    public static int lastIndexOfByte(ByteBuffer buffer, int lowIndex, int highIndex, byte value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>CharBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfChar(CharBuffer, int, int, char)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfChar(CharBuffer, int, int, char)
     */
    public static int lastIndexOfChar(CharBuffer buffer, int lowIndex, int highIndex, char value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>ShortBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfShort(ShortBuffer, int, int, short)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfShort(ShortBuffer, int, int, short)
     */
    public static int lastIndexOfShort(ShortBuffer buffer, int lowIndex, int highIndex, short value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>IntBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfInt(IntBuffer, int, int, int)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfInt(IntBuffer, int, int, int)
     */
    public static int lastIndexOfInt(IntBuffer buffer, int lowIndex, int highIndex, int value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>LongBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfLong(LongBuffer, int, int, long)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfLong(LongBuffer, int, int, long)
     */
    public static int lastIndexOfLong(LongBuffer buffer, int lowIndex, int highIndex, long value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>FloatBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfFloat(FloatBuffer, int, int, float)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfFloat(FloatBuffer, int, int, float)
     */
    public static int lastIndexOfFloat(FloatBuffer buffer, int lowIndex, int highIndex, float value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the minimal index <code>k</code>, so that
     * <code>lowIndex&lt;=k&lt;min(highIndex,buffer.limit())</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>lowIndex&gt;=buffer.limit()</code> or <code>lowIndex&gt;=highIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>lowIndex&lt;0</code>, the result is the same as if <code>lowIndex==0</code>.
     *
     * @param buffer    the searched <code>DoubleBuffer</code>.
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive);
     *                  pass <code>buffer.limit()</code> to search all remaining elements.
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
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
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=max(lowIndex,0)</code>
     * and <code>buffer.get(k)==value</code>,
     * or <code>-1</code> if there is no such buffer element.
     *
     * <p>In particular, if <code>highIndex&lt;=0</code> or <code>highIndex&lt;=lowIndex</code>,
     * this method returns <code>-1</code>,
     * and if <code>highIndex&gt;=buffer.limit()</code>, the result
     * is the same as if <code>highIndex==buffer.limit()</code>.
     *
     * <p>Note that <code>lowIndex</code> and <code>highIndex</code> arguments have the same sense as in
     * {@link #indexOfDouble(DoubleBuffer, int, int, double)} method:
     * they describe the search index range <code>lowIndex&lt;=k&lt;highIndex</code>.
     *
     * @param buffer    the searched Java array.
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this value does not occur
     * or if <code>max(lowIndex,0)&gt;=min(highIndex,buffer.limit())</code>.
     * @throws NullPointerException if <code>buffer</code> is {@code null}.
     * @see #indexOfDouble(DoubleBuffer, int, int, double)
     */
    public static int lastIndexOfDouble(DoubleBuffer buffer, int lowIndex, int highIndex, double value) {
        if (highIndex > buffer.limit()) {
            highIndex = buffer.limit();
        }
        int min = Math.max(lowIndex, 0);
        while (highIndex > min) {
            if (buffer.get(--highIndex) == value) {
                return highIndex;
            }
        }
        return -1;
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     * The byte elements are considered to be unsigned: <code>min(a,b)=(a&amp;0xFF)&lt;(b&amp;0xFF)?a:b</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ByteBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
                if ((v & 0xFF) < (dest[destPos] & 0xFF)) {
                    dest[destPos] = v;
                }
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     * The byte elements are considered to be unsigned: <code>min(a,b)=(a&amp;0xFF)&lt;(b&amp;0xFF)?a:b</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ByteBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
                if ((v & 0xFF) > (dest[destPos] & 0xFF)) {
                    dest[destPos] = v;
                }
            }
        }
    }

    /*Repeat() short ==> char,,int,,long,,float,,double;;
               Short ==> Char,,Int,,Long,,Float,,Double;;
               (\s*&(?:amp;)?\s*0xFFFF) ==> ,,...;;
               (The\s+\w+\s+elements.*?<\/code>\.\s*\*) ==> ,,... */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     * The short elements are considered to be unsigned: <code>min(a,b)=(a&amp;0xFFFF)&lt;(b&amp;0xFFFF)?a:b</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ShortBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minShortArrayAndBuffer(short[] dest, int destPos, ShortBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            short v = src.get(srcPos);
            if ((v & 0xFFFF) < (dest[destPos] & 0xFFFF)) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     * The short elements are considered to be unsigned: <code>min(a,b)=(a&amp;0xFFFF)&lt;(b&amp;0xFFFF)?a:b</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ShortBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxShortArrayAndBuffer(short[] dest, int destPos, ShortBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            short v = src.get(srcPos);
            if ((v & 0xFFFF) > (dest[destPos] & 0xFFFF)) {
                dest[destPos] = v;
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>CharBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minCharArrayAndBuffer(char[] dest, int destPos, CharBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            char v = src.get(srcPos);
            if ((v) < (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>CharBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxCharArrayAndBuffer(char[] dest, int destPos, CharBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            char v = src.get(srcPos);
            if ((v) > (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>IntBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minIntArrayAndBuffer(int[] dest, int destPos, IntBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            int v = src.get(srcPos);
            if ((v) < (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>IntBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxIntArrayAndBuffer(int[] dest, int destPos, IntBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            int v = src.get(srcPos);
            if ((v) > (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minLongArrayAndBuffer(long[] dest, int destPos, LongBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            long v = src.get(srcPos);
            if ((v) < (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxLongArrayAndBuffer(long[] dest, int destPos, LongBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            long v = src.get(srcPos);
            if ((v) > (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minFloatArrayAndBuffer(float[] dest, int destPos, FloatBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            float v = src.get(srcPos);
            if ((v) < (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxFloatArrayAndBuffer(float[] dest, int destPos, FloatBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            float v = src.get(srcPos);
            if ((v) > (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void minDoubleArrayAndBuffer(double[] dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            double v = src.get(srcPos);
            if ((v) < (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the minimum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void maxDoubleArrayAndBuffer(double[] dest, int destPos, DoubleBuffer src, int srcPos, int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            double v = src.get(srcPos);
            if ((v) > (dest[destPos])) {
                dest[destPos] = v;
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
               (\s*&(?:amp;)?\s*0xFF) ==> ,,$1FF,, ,,...;;
               (The\s+\w+\s+elements.*?\.\s*\*) ==> ,,$1,, ,,...
     */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFF)</code>.
     * The byte elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ByteBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFF)*mult</code>.
     * The byte elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ByteBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addByteBufferToArray(
            double[] dest,
            int destPos,
            ByteBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>CharBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))*mult</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>CharBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addCharBufferToArray(
            double[] dest,
            int destPos,
            CharBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFFFF)</code>.
     * The short elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ShortBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i)&amp;0xFFFF)*mult</code>.
     * The short elements are considered to be unsigned.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ShortBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addShortBufferToArray(
            double[] dest,
            int destPos,
            ShortBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>IntBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))*mult</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>IntBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addIntBufferToArray(
            double[] dest,
            int destPos,
            IntBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))*mult</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addLongBufferToArray(
            double[] dest,
            int destPos,
            LongBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))*mult</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addFloatBufferToArray(
            double[] dest,
            int destPos,
            FloatBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the sum of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * multiplied by <code>mult</code> argument,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]+=(src.get(srcPos+i))*mult</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @param mult    the elements from <code>src</code> array are multiplied by this value before adding.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limit.
     */
    public static void addDoubleBufferToArray(
            double[] dest,
            int destPos,
            DoubleBuffer src,
            int srcPos,
            int count,
            double mult) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (mult == 0.0) {
            return;
        }
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
               (The\s+\w+\s+elements.*?\.\s*\*) ==> ,,$1
     */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     * If <code>truncateOverflows</code> argument is <code>true</code>, the difference is truncated
     * to <code>0..0xFF</code> range before assigning to <code>dest</code> elements.
     * The byte elements are considered to be unsigned.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <code>ByteBuffer</code>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <code>0..0xFF</code> range.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractByteBufferFromArray(
            byte[] dest,
            int destPos,
            ByteBuffer src,
            int srcPos,
            int count,
            boolean truncateOverflows) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = ((int) dest[destPos] & 0xFF) - ((int) src.get(srcPos) & 0xFF);
                dest[destPos] = v < 0 ? 0 : (byte) v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     * If <code>truncateOverflows</code> argument is <code>true</code>, the difference is truncated
     * to <code>0..0xFFFF</code> range before assigning to <code>dest</code> elements.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <code>CharBuffer</code>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <code>0..0xFFFF</code> range.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractCharBufferFromArray(
            char[] dest,
            int destPos,
            CharBuffer src,
            int srcPos,
            int count,
            boolean truncateOverflows) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = ((int) dest[destPos]) - ((int) src.get(srcPos));
                dest[destPos] = v < 0 ? 0 : (char) v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     * If <code>truncateOverflows</code> argument is <code>true</code>, the difference is truncated
     * to <code>0..0xFFFF</code> range before assigning to <code>dest</code> elements.
     * The short elements are considered to be unsigned.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <code>ShortBuffer</code>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to <code>0..0xFFFF</code> range.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractShortBufferFromArray(
            short[] dest,
            int destPos,
            ShortBuffer src,
            int srcPos,
            int count,
            boolean truncateOverflows) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                int v = ((int) dest[destPos] & 0xFFFF) - ((int) src.get(srcPos) & 0xFFFF);
                dest[destPos] = v < 0 ? 0 : (short) v;
            }
        } else {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                dest[destPos] -= src.get(srcPos);
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     * If <code>truncateOverflows</code> argument is <code>true</code>, the difference is truncated
     * to <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code> range before assigning to <code>dest</code> elements.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <code>IntBuffer</code>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to
     *                          <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code> range.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractIntBufferFromArray(
            int[] dest, int destPos, IntBuffer src, int srcPos, int count,
            boolean truncateOverflows) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                long v = (long) dest[destPos] - (long) src.get(srcPos);
                dest[destPos] = v < Integer.MIN_VALUE ? Integer.MIN_VALUE :
                        v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractLongBufferFromArray(
            long[] dest,
            int destPos,
            LongBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] -= src.get(srcPos);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractFloatBufferFromArray(
            float[] dest,
            int destPos,
            FloatBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] -= src.get(srcPos);
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the difference of them and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=dest[destPos+i]-src.get(srcPos+i)</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void subtractDoubleBufferFromArray(
            double[] dest,
            int destPos,
            DoubleBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            dest[destPos] -= src.get(srcPos);
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() byte ==> char,,short,,long,,float,,double;;
               Byte ==> Char,,Short,,Long,,Float,,Double;;
               (\s*&\s*0xFF) ==> ,,$1FF,, ,,...;;
               \((long|float|double)\)\s+ ==> ,,...;;
               (\(The\s+\w+\s+elements.*?\.\)\s*\*) ==> ,,$1,, ,,...
     */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</code>.
     * (The byte elements are considered to be unsigned.)
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ByteBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfByteArrayAndBuffer(
            byte[] dest,
            int destPos,
            ByteBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            byte v = src.get(srcPos);
            dest[destPos] = (dest[destPos] & 0xFF) >= (v & 0xFF) ?
                    (byte) (dest[destPos] - v) :
                    (byte) (v - dest[destPos]);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>CharBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfCharArrayAndBuffer(
            char[] dest,
            int destPos,
            CharBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            char v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                    (char) (dest[destPos] - v) :
                    (char) (v - dest[destPos]);
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</code>.
     * (The short elements are considered to be unsigned.)
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>ShortBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfShortArrayAndBuffer(
            short[] dest,
            int destPos,
            ShortBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            short v = src.get(srcPos);
            dest[destPos] = (dest[destPos] & 0xFFFF) >= (v & 0xFFFF) ?
                    (short) (dest[destPos] - v) :
                    (short) (v - dest[destPos]);
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>LongBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfLongArrayAndBuffer(
            long[] dest,
            int destPos,
            LongBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            long v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                    (dest[destPos] - v) :
                    (v - dest[destPos]);
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>FloatBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfFloatArrayAndBuffer(
            float[] dest,
            int destPos,
            FloatBuffer src,
            int srcPos,
            int count) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
            float v = src.get(srcPos);
            dest[destPos] = (dest[destPos]) >= (v) ?
                    (dest[destPos] - v) :
                    (v - dest[destPos]);
        }
    }

    /**
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src.get(srcPos+i)|</code>.
     *
     * @param dest    the destination array.
     * @param destPos position of the first replaced element in the destination array.
     * @param src     the source <code>DoubleBuffer</code>.
     * @param srcPos  position of the first read element in the source buffer.
     * @param count   the number of elements to be replaced (should be &gt;=0).
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfDoubleArrayAndBuffer(
            double[] dest,
            int destPos,
            DoubleBuffer src,
            int srcPos,
            int count) {
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
     * Replaces <code>count</code> elements in <code>dest</code> array,
     * starting from the element <code>#destPos</code>,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code> elements in <code>src</code> buffer,
     * starting from the element <code>#srcPos</code>:
     * <code>dest[destPos+i]=|dest[destPos+i]-src[srcPos+i]|</code>.
     * If <code>truncateOverflows</code> argument is <code>true</code>, the difference is truncated
     * to <code>0..Integer.MAX_VALUE</code> range before assigning to <code>dest</code> elements.
     *
     * @param dest              the destination array.
     * @param destPos           position of the first replaced element in the destination array.
     * @param src               the source <code>IntBuffer</code>.
     * @param srcPos            position of the first read element in the source buffer.
     * @param count             the number of elements to be replaced (should be &gt;=0).
     * @param truncateOverflows whether the results should be truncated to
     *                          <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code> range.
     * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is {@code null}.
     * @throws IllegalArgumentException  if <code>count</code> is negative.
     * @throws IndexOutOfBoundsException if accessing elements would cause access of data outside array bounds
     *                                   or buffer limits.
     */
    public static void absDiffOfIntArrayAndBuffer(
            int[] dest,
            int destPos,
            IntBuffer src,
            int srcPos,
            int count,
            boolean truncateOverflows) {
        JArrays.rangeCheck(dest.length, destPos, src.limit(), srcPos, count);
        if (truncateOverflows) {
            for (int srcPosMax = srcPos + count; srcPos < srcPosMax; srcPos++, destPos++) {
                long v = (long) dest[destPos] - (long) src.get(srcPos);
                if (v < 0) {
                    v = -v;
                }
                dest[destPos] = v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
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
