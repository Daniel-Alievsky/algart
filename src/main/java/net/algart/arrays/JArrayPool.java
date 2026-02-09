/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>A simple pool of the Java arrays (usually work buffers) with the same size and type of elements,
 * based on a list of <code>SoftReference</code>.
 * This class is useful in algorithms that frequently need to allocate little buffers (tens of kilobytes),
 * because it allows to reduce time spent by the allocation of Java memory and the garbage collection.</p>
 *
 * <p>This class is <b>thread-safe</b>: you may use the same instance of this class in several threads.</p>
 *
 * @author Daniel Alievsky
 * @see ArrayPool
 */
public final class JArrayPool {
    private final Class<?> elementType;
    private final int arrayLength;
    private final List<Reference<Object>> freeArrays = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private JArrayPool(Class<?> elementType, int arrayLength) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (arrayLength < 0) {
            throw new IllegalArgumentException("Negative arrayLength");
        }
        this.elementType = elementType;
        this.arrayLength = arrayLength;
    }

    /**
     * Creates the pool of Java arrays. Every array will have the given element type and length.
     *
     * @param elementType the type of elements in the arrays.
     * @param arrayLength the length of the arrays.
     * @return            new pool of Java arrays.
     * @throws NullPointerException     if <code>elementType</code> is {@code null}.
     * @throws IllegalArgumentException if <code>arrayLength</code> is negative.
     */
    public static JArrayPool getInstance(Class<?> elementType, int arrayLength) {
        return new JArrayPool(elementType, arrayLength);
    }

    /**
     * Returns the type of elements in the arrays in this pool.
     *
     * @return the type of elements in the arrays in this pool.
     */
    public Class<?> elementType() {
        return this.elementType;
    }

    /**
     * Returns the size of all arrays in this pool.
     *
     * @return the size of all arrays in this pool.
     */
    public int arrayLength() {
        return this.arrayLength;
    }

    /**
     * Returns the ready for use Java array. If it is not found in the internal cache, it is created,
     * in another case some free array from the cache is returned.
     *
     * <p>The {@link #releaseArray(Object)} should be called after finishing working with this array.
     *
     * @return the ready for use Java array.
     */
    public Object requestArray() {
        lock.lock();
        try {
            for (Iterator<Reference<Object>> iterator = freeArrays.iterator(); iterator.hasNext(); ) {
                Reference<Object> ref = iterator.next();
                Object array = ref.get();
                iterator.remove();
                if (array != null) {
                    return array;
                }
            }
            return newJavaArray();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases the Java array, returned by previous {@link #requestArray()} call,
     * and adds it to the internal cache of arrays.
     * Future calls of {@link #requestArray()}, maybe, will return it again.
     *
     * <p>Please note: it will not be an error if you will not call this method
     * after {@link #requestArray()}. But calling this method improves performance
     * of future {@link #requestArray()} calls.
     *
     * <p>This method must not be called twice for the same object.
     *
     * <p>This method does nothing if the passed argument is {@code null}.
     *
     * @param array some Java array, returned by previous {@link #requestArray()} call;
     *        can be {@code null}, then the method does nothing.
     * @throws IllegalArgumentException if the argument is not a Java array, or if its size or element type
     *                                  do not match the arguments of {@link #getInstance} method.
     */
    public void releaseArray(Object array) {
        if (array == null) {
            return;
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("The array argument is not a Java array");
        }
        if (array.getClass().getComponentType() != elementType) {
            throw new IllegalArgumentException("The type of array elements does not match this Java array pool");
        }
        if (java.lang.reflect.Array.getLength(array) != arrayLength) {
            throw new IllegalArgumentException("The array length does not match this Java array pool");
        }
        lock.lock();
        try {
            freeArrays.add(new SoftReference<>(array));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "Java array pool for storing " + elementType.getCanonicalName() + "[" + arrayLength + "] ("
            + freeArrays.size() + " arrays in the cache)";
    }

    private Object newJavaArray() {
//        System.out.println("!!!Creating new Java array: " + this);
        return java.lang.reflect.Array.newInstance(elementType, arrayLength);
    }
}
