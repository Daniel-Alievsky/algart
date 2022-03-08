/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Int ;;
  float ==> int
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>int</tt> values.</p>
 *
 * @author Daniel Alievsky
 */
public interface IntStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART int array, implementing {@link MutableIntArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * int result = intArray.{@link IntArray#getInt(long) getInt}(intArray.{@link #length() length()}-1);
     * intArray.{@link MutableArray#length(long) length}(intArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public int popInt();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART int array, implementing {@link MutableIntArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * intArray.{@link MutableIntArray#length(long) length}(intArray.{@link #length() length()}+1);
     * intArray.{@link UpdatableIntArray#setInt(long, int)
     * setInt}(intArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushInt(int value);
/*Repeat.IncludeEnd*/
}
