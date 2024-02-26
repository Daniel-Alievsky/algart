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
 * <p>Unchecked exception thrown if the current or desired array length is extremely large.
 * It is thrown by methods, creating new arrays or resizing existing arrays,
 * if the desired array length is too large (regardless of the amount of memory,
 * for example, greater than <tt>Long.MAX_VALUE</tt>),
 * or by methods trying to convert the array into a Java array (as {@link Arrays#toJavaArray(Array)}),
 * if the array is too large for storing its content in a form of Java array
 * (Java arrays can contain, as a maximum, <tt>Integer.MAX_VALUE</tt> (2<sup>31</sup>-1) elements).</p>
 *
 * @author Daniel Alievsky
 */
public class TooLargeArrayException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public TooLargeArrayException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public TooLargeArrayException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 6967772412276478354L;
}
