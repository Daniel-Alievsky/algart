/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.Objects;

/**
 * <p>The degenerate memory model that does not allow to create any AlgART arrays.
 * Any attempt to create an array by methods of this class leads to
 * {@link UnsupportedElementTypeException} (or, maybe, <code>NullPointerException</code>,
 * <code>IllegalArgumentException</code> or <code>ClassCastException</code>,
 * if the comments to the method require this).
 *
 * <p>This class may be useful to detect a bug, when some method have a {@link MemoryModel} argument,
 * but you are sure that this argument must not be used for creating any AlgART arrays.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of its instance returned by {@link #getInstance()} method.
 * Moreover, it is a <b>singleton</b>: {@link #getInstance()} always returns the same object.</p>
 *
 * @author Daniel Alievsky
 */
public class SignalMemoryModel extends AbstractMemoryModel {
    private static final SignalMemoryModel INSTANCE = new SignalMemoryModel();

    private SignalMemoryModel() {
    }

    /**
     * Returns an instance of this memory model.
     *
     * @return an instance of this memory model.
     */
    public static SignalMemoryModel getInstance() {
        return INSTANCE;
    }

    public MutableArray newEmptyArray(Class<?> elementType) {
        return newEmptyArray(elementType, 157);
    }

    public MutableArray newEmptyArray(Class<?> elementType, long initialCapacity) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Negative initial capacity");
        }
        throw new UnsupportedElementTypeException("SignalMemoryModel cannot create any AlgART arrays");
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialLength < 0) {
            throw new IllegalArgumentException("Negative initial length");
        }
        throw new UnsupportedElementTypeException("SignalMemoryModel cannot create any AlgART arrays");
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0) {
            throw new IllegalArgumentException("Negative array length");
        }
        throw new UnsupportedElementTypeException("SignalMemoryModel cannot create any AlgART arrays");
    }

    /**
     * This implementation always returns <code>false</code>.
     *
     * @param elementType the type of array elements.
     * @return            <code>false</code> always.
     * @throws NullPointerException if <code>elementType</code> is {@code null}.
     */
    public boolean isElementTypeSupported(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        return false;
    }

    /**
     * This implementation always returns <code>false</code>.
     *
     * @return <code>false</code> always.
     */
    public boolean areAllPrimitiveElementTypesSupported() {
        return false;
    }

    /**
     * This implementation always returns <code>false</code>.
     *
     * @return <code>false</code> always.
     */
    public boolean areAllElementTypesSupported() {
        return false;
    }

    /**
     * This implementation always returns <code>-1</code>.
     *
     * @param elementType the type of array elements.
     * @return            <code>-1</code> always.
     * @throws NullPointerException if <code>elementType</code> is {@code null}.
     */
    public long maxSupportedLength(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        return -1;
    }

    /**
     * This implementation always returns <code>false</code>.
     *
     * @return <code>false</code> always.
     */
    public boolean isCreatedBy(Array array) {
        return false;
    }
}
