/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>A simple pool of the {@link UpdatableArray unresizable AlgART arrays}
 * (usually work buffers) with the same size and type of elements,
 * based on a list of <tt>SoftReference</tt> or <tt>WeakReference</tt>.
 * This class is useful in algorithms that frequently need to allocate buffers in AlgART arrays,
 * because it allows to reduce time spent by the allocation of AlgART arrays and the garbage collection.</p>
 *
 * <p>This class is <b>thread-safe</b>: you may use the same instance of this class in several threads.</p>
 *
 * @author Daniel Alievsky
 * @see JArrayPool
 */
public class ArrayPool {
    private final MemoryModel memoryModel;
    private final Class<?> elementType;
    private final long arrayLength;
    private final List<Reference<UpdatableArray>> freeArrays = new LinkedList<Reference<UpdatableArray>>();
    private final ReentrantLock lock = new ReentrantLock();

    private ArrayPool(MemoryModel memoryModel, Class<?> elementType, long arrayLength) {
        if (memoryModel == null)
            throw new NullPointerException("Null memoryModel argument");
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        if (arrayLength < 0)
            throw new IllegalArgumentException("Negative arrayLength");
        this.memoryModel = memoryModel;
        this.elementType = elementType;
        this.arrayLength = arrayLength;
    }

    /**
     * Creates the pool of AlgART arrays. Every array will have the given element type and length.
     *
     * @param memoryModel the memory model that will be used for allocating new arrays.
     * @param elementType the type of elements in the arrays.
     * @param arrayLength the length of the arrays.
     * @return            new pool of AlgART arrays.
     * @throws NullPointerException     if <tt>memoryModel</tt> or <tt>elementType</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>arrayLength</tt> is negative.
     */
    public static ArrayPool getInstance(MemoryModel memoryModel, Class<?> elementType, long arrayLength) {
        return new ArrayPool(memoryModel, elementType, arrayLength);
    }

    /**
     * Returns the memory model used for allocating new arrays.
     *
     * @return the memory model used for allocating new arrays.
     */
    public MemoryModel memoryModel() {
        return this.memoryModel;
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
    public long arrayLength() {
        return this.arrayLength;
    }

    /**
     * Returns the ready for use unresizable AlgART array. If it is not found in the internal cache, it is created,
     * in other case some free array from the cache is returned.
     *
     * <p>The {@link #releaseArray(UpdatableArray)} should be called after finishing working with this array.
     *
     * @return the ready for use AlgART array.
     */
    public UpdatableArray requestArray() {
        lock.lock();
        try {
            for (Iterator<Reference<UpdatableArray>> iterator = freeArrays.iterator(); iterator.hasNext(); ) {
                Reference<UpdatableArray> ref = iterator.next();
                UpdatableArray array = ref.get();
                iterator.remove();
                if (array != null) {
                    return array;
                }
            }
            return newArray();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases the AlgART array, returned by previous {@link #requestArray()} call,
     * and adds it to the internal cache of arrays.
     * Future calls of {@link #requestArray()}, maybe, will return it again.
     *
     * <p>Please note: it will not be an error if you will not call this method
     * after {@link #requestArray()}. But calling this method improves performance
     * of future {@link #requestArray()} calls.
     *
     * <p>This method must not be called twice for the same object.
     *
     * <p>This method does nothing if the passed argument is <tt>null</tt>.
     *
     * @param array some AlgART array, returned by previous {@link #requestArray()} call;
     *        may be <tt>null</tt>, then the method does nothing.
     * @throws IllegalArgumentException if the length or element type of the passed array
     *                                  do not match the arguments of {@link #getInstance getInstance} method.
     */
    public void releaseArray(UpdatableArray array) {
        if (array == null)
            return;
        if (array.elementType() != elementType)
            throw new IllegalArgumentException("The type of array elements does not match this AlgART array pool");
        if (array.length() != arrayLength)
            throw new IllegalArgumentException("The array length does not match this AlgART array pool");
        lock.lock();
        try {
            Reference<UpdatableArray> ref = SimpleMemoryModel.isSimpleArray(array) ?
                new SoftReference<UpdatableArray>(array) :
                new WeakReference<UpdatableArray>(array);
            // for non-trivial memory models, there is a risk that the SoftReference will not be released in time
            freeArrays.add(ref);
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
        return "AlgART array pool for storing " + elementType.getCanonicalName() + "[" + arrayLength + "] ("
            + freeArrays.size() + " arrays in the cache)";
    }

    private UpdatableArray newArray() {
        return memoryModel.newUnresizableArray(elementType, arrayLength);
    }
}
