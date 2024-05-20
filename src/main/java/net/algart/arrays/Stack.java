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

/**
 * <p>Resizable stack of any elements.</p>
 *
 * <p><tt>Stack</tt> is a restricted version (inheritor) of {@link MutableArray} interface,
 * allowing only access to the top element.
 *
 * <p>Please keep in mind: this package does not contain classes
 * that implements <tt>Stack</tt> but not implements {@link MutableArray}.
 * It means that the following operation usually works successfully:</p><pre>
 * void someMyMethod(Stack stack) {
 * &#32;   MutableArray a = (MutableArray)stack;
 * &#32;   ... // access to any non-top elements
 * }</pre>
 *
 * <p>Of course, it is not an example of good programming style, and there are no guarantees
 * that such operator will not throw <tt>ClassCastException</tt>.
 * But such an operation is usually posssible. Please compare:</p><pre>
 * void someMyMethod(Array readOnlyArray) {
 * &#32;   MutableArray a = (MutableArray)readOnlyArray;
 * &#32;   ... // attempt to modify elements
 * }</pre>
 *
 * <p>This code will throw <tt>ClassCastException</tt>, if the caller of <tt>someMyMethod</tt>
 * does not forget to use {@link UpdatableArray#asImmutable() asImmutable()} method for
 * creating <tt>readOnlyArray</tt> argument.</p>
 *
 * <p>If this stack elements are primitive values (<tt>byte</tt>, <tt>short</tt>, etc.),
 * the stack <b>must</b> implement one of
 * {@link BitStack}, {@link CharStack}, {@link ByteStack}, {@link ShortStack},
 * {@link IntStack}, {@link LongStack}, {@link FloatStack}, {@link DoubleStack}
 * subinterfaces.
 * In other case, the stack <b>must</b> implement {@link ObjectStack} subinterface.</p>
 *
 * <p>Objects, implementing this interface,
 * are not thread-safe, but <b>are thread-compatible</b>
 * and can be synchronized manually if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */

public interface Stack {
    /**
     * Returns the number of elements in this stack.
     *
     * @return the number of elements in this stack.
     */
    long length();

    /**
     * Removes the element at the top of this stack and returns it,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART array, implementing {@link MutableArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * Object result = array.{@link MutableArray#getElement(long) getElement}(array.{@link #length() length()}-1);
     * array.{@link MutableArray#length(long) length}(array.{@link #length() length()}-1);
     * </pre>
     *
     * <p>It is a low-level method.
     * For stacks of primitive elements, implementing one of corresponding interfaces
     * {@link BitStack}, {@link CharStack}, {@link ByteStack}, {@link ShortStack},
     * {@link IntStack}, {@link LongStack}, {@link FloatStack}, {@link DoubleStack},
     * we recommend to use more efficient equivalent method of that interfaces:
     * {@link BitStack#popBit()}, {@link CharStack#popChar()},
     * {@link ByteStack#popByte()}, {@link ShortStack#popShort()},
     * {@link IntStack#popInt()}, {@link LongStack#popLong()},
     * {@link FloatStack#popFloat()}, {@link DoubleStack#popDouble()}.
     * For other stacks, implementing {@link ObjectStack},
     * we recommend to use {@link ObjectStack#pop()}.
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    Object popElement();

    /**
     * Appends <tt>value</tt> element to the top of this stack.
     *
     * <p>It this object is an AlgART array, implementing {@link MutableArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * array.{@link MutableArray#length(long) length}(array.{@link #length() length()}+1);
     * array.{@link UpdatableArray#setElement(long, Object) setElement}(array.{@link #length() length()}-1, value);
     * </pre>
     *
     * <p>It is a low-level method.
     * For stacks of primitive elements, implementing one of corresponding interfaces
     * {@link BitStack}, {@link CharStack}, {@link ByteStack}, {@link ShortStack},
     * {@link IntStack}, {@link LongStack}, {@link FloatStack}, {@link DoubleStack},
     * we recommend to use more efficient equivalent method of that interfaces:
     * {@link BitStack#pushBit(boolean)}, {@link CharStack#pushChar(char)},
     * {@link ByteStack#pushByte(byte)}, {@link ShortStack#pushShort(short)},
     * {@link IntStack#pushInt(int)}, {@link LongStack#pushLong(long)},
     * {@link FloatStack#pushFloat(float)}, {@link DoubleStack#pushDouble(double)}.
     * For other stacks, implementing {@link ObjectStack},
     * we recommend to use {@link ObjectStack#push(Object)}.
     *
     * @param value to be added to the top of this stack.
     * @throws ClassCastException        if it is a stack of primitive elements and <tt>value</tt> is not
     *                                   a corresponding wrapped class (<tt>Boolean</tt>, <tt>Integer</tt>, etc.).
     * @throws ArrayStoreException       if it is a stack of non-primitive elements and <tt>value</tt> is not
     *                                   an instance of the class of stack elements.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    void pushElement(Object value);

    /**
     * Removes the element at the top of this stack and returns it,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     * This method differs from {@link #popElement()} only in that it does not return any result,
     * so it works slightly faster.
     *
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    void removeTop();

    /**
     * Removes all elements from the stack
     *
     * @return a reference to this array.
     */
    Stack clear();
}
