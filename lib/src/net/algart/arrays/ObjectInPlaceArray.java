/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Special version of {@link ObjectArray} allowing
 * to load an element without creating new Java object.</p>
 *
 * <p>The arrays of object created via some {@link MemoryModel memory models} may not implement this interface.
 * You may check, is this interface implemented, by <tt>intstanceof</tt> operator.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ObjectInPlaceArray<E> extends ObjectArray<E> {

    /**
     * Copies the content of element #<tt>index</tt> to the passed <tt>resultValue</tt>
     * and returns <tt>resultValue</tt>. Never returns <tt>null</tt>.
     *
     * <p>This method may work much faster than {@link #getElement(long)} in long loops,
     * because allows to avoid allocating Java objects in the heap.
     *
     * @return            a reference to <tt>resultValue</tt>.
     * @param index       index of element to load.
     * @param resultValue the object where the retrieved content will be stored.
     * @throws IndexOutOfBoundsException if index out of range <tt>0..length()-1</tt>.
     * @throws NullPointerException if <tt>resultValue</tt> is <tt>null</tt>.
     */
    public E getInPlace(long index, Object resultValue);

    /**
     * Creates one instance of {@link #elementType()} class in some state.
     * This method, for example, may be used before a loop of calls of
     * {@link #getInPlace(long, Object)}.
     *
     * @return some instance of {@link #elementType()} class
     */
    public E allocateElement();
}
