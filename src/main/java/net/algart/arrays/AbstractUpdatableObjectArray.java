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
 * <p>Implementation of almost all basic functions of {@link UpdatableObjectArray} interface.
 * The only {@link ObjectArray#get(long)} and {@link UpdatableObjectArray#set(long, Object)} methods
 * are not defined in this class;
 * all other methods are implemented via calls of these 2 methods.</p>
 *
 * @param <E> the generic type of array elements.
 * @author Daniel Alievsky
 */
public abstract class AbstractUpdatableObjectArray<E>
        extends AbstractObjectArray<E> implements UpdatableObjectArray<E> {

    /**
     * Creates an array with the given initial capacity and length.
     *
     * <p>This array is not <i>{@link #isNew() new}</i> by default.
     * This is correct usually, because this class is often used
     * for creating a view of another data. However, if the instance
     * of this class does not depend on any other data sources,
     * you may call {@link #setNewStatus(boolean) setNewStatus(true)} in
     * the constructor of your subclass.
     *
     * @param elementType                 the {@link #elementType() element type} of this array.
     * @param initialCapacity             initial capacity of the array.
     * @param initialLength               initial length of the array.
     * @param underlyingArraysAreParallel see the same argument of
     *                                    {@link AbstractFloatArray#AbstractFloatArray(long, long, boolean, Array...)}.
     * @param underlyingArrays            see the same argument of
     *                                    {@link AbstractArray#AbstractArray(long, long, Array...)}.
     * @throws NullPointerException     if <code>underlyingArrays</code> argument or some of
     *                                  <code>underlyingArrays[k]</code>
     *                                  elements are {@code null}.
     * @throws IllegalArgumentException if the <code>initialCapacity</code> or <code>initialLength</code> arguments
     *                                  are illegal (negative, or capacity &lt; length).
     * @throws SizeMismatchException    if <code>underlyingArraysAreParallel=true</code>,
     *                                  <code>underlyingArrays.length&gt;1</code> and some of passed arrays
     *                                  have different lengths.
     */
    protected AbstractUpdatableObjectArray(
            Class<E> elementType, long initialCapacity, long initialLength,
            boolean underlyingArraysAreParallel, Array... underlyingArrays) {
        super(elementType, initialCapacity, initialLength, underlyingArraysAreParallel, underlyingArrays);
    }

    /**
     * Equivalent to the constructor
     * {@link #AbstractUpdatableObjectArray(Class, long, long, boolean, Array...)},
     * where both <code>initialCapacity</code> and <code>initialLength</code> arguments are equal to
     * <code>initialCapacityAndLength</code>.
     *
     * @param elementType                 the {@link #elementType() element type} of this array.
     * @param initialCapacityAndLength    initial capacity and length of the array.
     * @param underlyingArraysAreParallel see
     *                                    {@link #AbstractUpdatableObjectArray(Class, long, long, boolean, Array...)}.
     * @param underlyingArrays            see
     *                                    {@link #AbstractUpdatableObjectArray(Class, long, long, boolean, Array...)}
     * @throws IllegalArgumentException if the passed argument are illegal (negative).
     */
    protected AbstractUpdatableObjectArray(
            Class<E> elementType, long initialCapacityAndLength,
            boolean underlyingArraysAreParallel, Array... underlyingArrays) {
        this(elementType,
                initialCapacityAndLength, initialCapacityAndLength, underlyingArraysAreParallel, underlyingArrays);
    }


    /**
     * This implementation returns new instance of {@link AbstractUpdatableObjectArray} with the same memory model
     * and underlying arrays, that were passed to the constructor of this instance,
     * and with overridden methods {@link #get(long)}, {@link #set(long, Object)},
     * {@link #getData(long, Object, int, int)} and
     * {@link #setData(long, Object, int, int)}
     * calling the same methods of this instance with corresponding corrections of the arguments.
     *
     * <p>The returned instance also have overridden methods {@link #loadResources(ArrayContext, long, long)},
     * {@link #flushResources(ArrayContext, long, long, boolean)} and
     * {@link #freeResources(ArrayContext, long, long, boolean)},
     * that also call the same methods of this instance with corresponding correction of their <code>fromIndex</code>
     * argument.
     *
     * <p>The returned instance also have overridden method {@link #isLazy()},
     * that just calls the same methods of this instance with the same arguments.
     *
     * @param fromIndex low endpoint (inclusive) of the subarray.
     * @param toIndex   high endpoint (exclusive) of the subarray.
     * @return a view of the specified range within this array.
     * @throws IndexOutOfBoundsException for illegal fromIndex and toIndex
     *                                   (fromIndex &lt; 0 || toIndex &gt; length() || fromIndex &gt; toIndex).
     */
    public UpdatableObjectArray<E> subArray(long fromIndex, long toIndex) {
        checkSubArrayArguments(fromIndex, toIndex);
        final AbstractUpdatableObjectArray<E> parent = this;
        final long offset = fromIndex;
        return new AbstractUpdatableObjectArray<>(elementType, toIndex - fromIndex,
                underlyingArraysAreParallel, underlyingArrays) {
            @Override
            public E get(long index) {
                if (index < 0 || index >= length) {
                    throw rangeException(index);
                }
                return parent.get(offset + index);
            }

            @Override
            public void set(long index, E value) {
                if (index < 0 || index >= length) {
                    throw rangeException(index);
                }
                parent.set(offset + index, value);
            }

            @Override
            public long indexOf(long lowIndex, long highIndex, E value) {
                if (lowIndex < 0) {
                    lowIndex = 0;
                }
                if (highIndex > length) {
                    highIndex = length;
                }
                if (highIndex <= lowIndex) {
                    // this check guarantees that overflow is impossible below:
                    // offset + lowIndex <= offset + highIndex <= offset + length = toIndex <= Long.MAX_VALUE
                    return -1;
                }
                long result = parent.indexOf(offset + lowIndex, offset + highIndex, value);
                return result == -1 ? -1 : result - offset;
            }

            @Override
            public long lastIndexOf(long lowIndex, long highIndex, E value) {
                if (lowIndex < 0) {
                    lowIndex = 0;
                }
                if (highIndex > length) {
                    highIndex = length;
                }
                if (highIndex <= lowIndex) {
                    // this check guarantees that overflow is impossible below:
                    // offset + lowIndex <= offset + highIndex <= offset + length = toIndex <= Long.MAX_VALUE
                    return -1;
                }
                long result = parent.lastIndexOf(offset + lowIndex, offset + highIndex, value);
                return result == -1 ? -1 : result - offset;
            }

            @Override
            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                if (count < 0) {
                    throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
                }
                if (arrayPos < 0) {
                    throw rangeException(arrayPos);
                }
                if (arrayPos > length - count) {
                    throw rangeException(arrayPos + count - 1);
                }
                parent.getData(offset + arrayPos, destArray, destArrayOffset, count);
            }

            @Override
            public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
                if (count < 0) {
                    throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
                }
                if (arrayPos < 0) {
                    throw rangeException(arrayPos);
                }
                if (arrayPos > length - count) {
                    throw rangeException(arrayPos + count - 1);
                }
                parent.setData(offset + arrayPos, srcArray, srcArrayOffset, count);
                return this;
            }

            @Override
            public UpdatableObjectArray<E> fill(long position, long count, E value) {
                checkSubArrArguments(position, count);
                parent.fill(offset + position, count, value);
                return this;
            }

            @Override
            public boolean isLazy() {
                return parent.isLazy();
            }

            @Override
            protected void loadResources(ArrayContext context, long fromIndex, long toIndex) {
                parent.loadResources(context, offset + fromIndex, offset + toIndex);
            }

            @Override
            protected void flushResources(
                    ArrayContext context, long fromIndex, long toIndex,
                    boolean forcePhysicalWriting) {
                parent.flushResources(context, offset + fromIndex, offset + toIndex, forcePhysicalWriting);
            }

            @Override
            protected void freeResources(
                    ArrayContext context, long fromIndex, long toIndex,
                    boolean forcePhysicalWriting) {
                parent.freeResources(context, offset + fromIndex, offset + toIndex, forcePhysicalWriting);
            }
        };
    }

    public UpdatableObjectArray<E> subArr(long position, long count) {
        return subArray(position, position + count);
    }

    public abstract void set(long index, E value);

    /**
     * This implementation just calls
     * <code>{@link #fill(long, long, Object) fill}(0, thisArray.length(), value)</code>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return a reference to this array.
     */
    public final UpdatableObjectArray<E> fill(E value) {
        return fill(0, length(), value);
    }

    /**
     * This implementation does the following:
     * <code>{@link #subArr(long, long) subArr}(position, count).{@link UpdatablePArray#copy(Array)
     * copy}({@link Arrays#nObjectCopies(long, Object) Arrays.nObjectCopies}(count, value))</code>.
     * Please override this method if it's possible to perform the same task more efficiently.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <code>position</code> and <code>count</code>
     *                                   (<code>position &lt; 0 || count &lt; 0
     *                                   || position + count &gt; length()</code>).
     */
    public UpdatableObjectArray<E> fill(long position, long count, E value) {
        UpdatableObjectArray<?> a = position == 0 && count == length() ? this : subArr(position, count);
        a.copy(Arrays.nObjectCopies(count, value));
        return this;
    }


    /**
     * This implementation returns <code>false</code>.
     *
     * @return <code>true</code> if this instance is immutable.
     */
    @Override
    public boolean isImmutable() {
        return false;
    }

    /**
     * This implementation returns an instance of the subclass of {@link AbstractObjectArray},
     * where the following method are overridden and call the same methods of this instance:
     * {@link #get(long)}, {@link #getData(long, Object, int, int)},
     * {@link #getData(long, Object)},
     * {@link #loadResources(ArrayContext, long, long)},
     * {@link #flushResources(ArrayContext, long, long, boolean)} and
     * {@link #freeResources(ArrayContext, long, long, boolean)}.
     *
     * <p>The returned instance also have overridden method {@link #isLazy()},
     * that just calls the same methods of this instance with the same arguments.
     *
     * @return an immutable view of this array.
     */
    @Override
    public ObjectArray<E> asImmutable() {
        final AbstractUpdatableObjectArray<E> parent = this;
        return new AbstractObjectArray<>(elementType, length, false, underlyingArrays) {
            @Override
            public E get(long index) {
                return parent.get(index);
            }

            @Override
            public long indexOf(long lowIndex, long highIndex, E value) {
                return parent.indexOf(lowIndex, highIndex, value);
            }

            @Override
            public long lastIndexOf(long lowIndex, long highIndex, E value) {
                return parent.lastIndexOf(lowIndex, highIndex, value);
            }

            @Override
            public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
                parent.getData(arrayPos, destArray, destArrayOffset, count);
            }

            @Override
            public void getData(long arrayPos, Object destArray) {
                parent.getData(arrayPos, destArray);
            }

            @Override
            public boolean isLazy() {
                return parent.isLazy();
            }

            @Override
            protected void loadResources(ArrayContext context, long fromIndex, long toIndex) {
                parent.loadResources(context, fromIndex, toIndex);
            }

            @Override
            protected void flushResources(
                    ArrayContext context, long fromIndex, long toIndex,
                    boolean forcePhysicalWriting) {
                parent.flushResources(context, fromIndex, toIndex, forcePhysicalWriting);
            }

            @Override
            protected void freeResources(
                    ArrayContext context, long fromIndex, long toIndex,
                    boolean forcePhysicalWriting) {
                parent.freeResources(context, fromIndex, toIndex, forcePhysicalWriting);
            }
        };
    }

    /**
     * This implementation returns <code>(UpdatableArray)super.{@link AbstractArray#shallowClone()
     * shallowClone()}</code>.
     *
     * @return a shallow copy of this object.
     */
    @Override
    public UpdatableArray shallowClone() {
        return (UpdatableArray) super.shallowClone();
    }

    /**
     * This implementation calls <code>set(index, value)</code>.
     *
     * @param index index of the element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     * @throws NullPointerException      if <code>value == null</code> and it is an array of primitive elements
     * @throws ClassCastException        if it is an array of primitive elements and <code>value</code>
     *                                   is not a corresponding wrapped class
     *                                   (<code>Boolean</code>, <code>Integer</code>, etc.)
     * @throws ArrayStoreException       if it is an array of non-primitive elements and <code>value</code>
     *                                   is not an instance of {@link #elementType()} class
     */
    public void setElement(long index, Object value) {
        set(index, InternalUtils.<E>cast(value));
    }

    /**
     * This implementation is based on a loop of calls of {@link #set(long, Object)} method.
     * Please override this method if it's possible to perform the same task more efficiently
     * than such a loop.
     *
     * @param arrayPos       starting position in this AlgART array.
     * @param srcArray       the source Java array.
     * @param srcArrayOffset starting position in the source Java array.
     * @param count          the number of elements to be copied.
     * @return a reference to this AlgART array.
     * @throws NullPointerException      if <code>srcArray</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>srcArray</code> argument is not an array.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or source Java array.
     * @throws ArrayStoreException       if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType() elementType()}.
     * @throws ClassCastException        if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType() elementType()}
     *                                   (both this and <code>ArrayStoreException</code> are possible,
     *                                   depending on implementation).
     */
    public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
        Objects.requireNonNull(srcArray, "Null srcArray argument");
        E[] a = InternalUtils.cast(srcArray);
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw rangeException(arrayPos);
        }
        if (arrayPos > length - count) {
            throw rangeException(arrayPos + count - 1);
        }
        for (long arrayPosMax = arrayPos + count; arrayPos < arrayPosMax; arrayPos++, srcArrayOffset++) {
            set(arrayPos, a[srcArrayOffset]);
        }
        return this;
    }

    /**
     * This implementation calls {@link #setData(long, Object, int, int)}
     * with corresponding arguments.
     *
     * @param arrayPos starting position in this AlgART array.
     * @param srcArray the source Java array.
     * @return a reference to this AlgART array.
     * @throws NullPointerException      if <code>srcArray</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>srcArray</code> argument is not an array.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or source Java array.
     * @throws ArrayStoreException       if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}.
     * @throws ClassCastException        if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}
     *                                   (both this and <code>ArrayStoreException</code> are possible,
     *                                   depending on implementation).
     */
    public UpdatableArray setData(long arrayPos, Object srcArray) {
        Objects.requireNonNull(srcArray, "Null srcArray argument");
        if (arrayPos < 0 || arrayPos > length) {
            throw rangeException(arrayPos);
        }
        int count = ((Object[]) srcArray).length;
        if (count > length - arrayPos) {
            count = (int) (length - arrayPos);
        }
        setData(arrayPos, srcArray, 0, count);
        return this;
    }

    /**
     * This implementation calls <code>set(destIndex, get(srcIndex))</code>.
     *
     * @param destIndex index of the element to replace.
     * @param srcIndex  index of the element to be copied.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <code>0..length()-1</code>.
     */
    public void copy(long destIndex, long srcIndex) {
        set(destIndex, get(srcIndex));
    }

    /**
     * This implementation is based on a loop of calls of {@link #copy(long, long)} method.
     *
     * @param destIndex starting index of the element to replace.
     * @param srcIndex  starting index of the element to be copied.
     * @param count     the number of elements to be copied.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <code>0..length()-1</code>.
     */
    public void copy(long destIndex, long srcIndex, long count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Negative number of copied elements (count = " + count
                    + ") in " + getClass());
        }
        if (srcIndex <= destIndex && srcIndex + count > destIndex) {
            srcIndex += count;
            destIndex += count;
            for (long k = 0; k < count; k++) {
                copy(--destIndex, --srcIndex);
            }
        } else {
            for (long k = 0; k < count; k++) {
                copy(destIndex++, srcIndex++);
            }
        }
    }

    /**
     * This implementation swaps two elements by {@link #get(long)}
     * and {@link #set(long, Object)} methods.
     *
     * @param firstIndex  first index of the element to exchange.
     * @param secondIndex second index of the element to exchange.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <code>0..length()-1</code>.
     */
    public void swap(long firstIndex, long secondIndex) {
        E temp = get(firstIndex);
        set(firstIndex, get(secondIndex));
        set(secondIndex, temp);
    }

    /**
     * This implementation is based on a loop of calls of {@link #swap(long, long)} method.
     *
     * @param firstIndex  starting first index of the element to exchange.
     * @param secondIndex starting second index of the element to exchange.
     * @param count       the number of elements to be exchanged.
     * @throws IndexOutOfBoundsException if one of indexes is out of range <code>0..length()-1</code>.
     */
    public void swap(long firstIndex, long secondIndex, long count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Negative number of swapped elements (count = " + count
                    + ") in " + getClass());
        }
        for (long k = 0; k < count; k++) {
            swap(firstIndex++, secondIndex++);
        }
    }

    /**
     * This implementation calls {@link #defaultCopy defaultCopy(thisInstance, src)}.
     *
     * @param src the source array.
     * @return a reference to this array.
     * @throws NullPointerException     if <code>src</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and this element types do not match.
     */
    public UpdatableArray copy(Array src) {
        defaultCopy(this, src);
        return this;
    }

    /**
     * This implementation calls {@link #defaultSwap defaultSwap(thisInstance, another)}.
     *
     * @param another another array.
     * @return a reference to this array.
     * @throws NullPointerException     if <code>another</code> argument is {@code null}.
     * @throws IllegalArgumentException if another and this element types do not match.
     */
    public UpdatableArray swap(UpdatableArray another) {
        defaultSwap(this, another);
        return this;
    }

    /**
     * This implementation calls {@link #setNewStatus(boolean) setNewStatus(false)}.
     */
    public void setNonNew() {
        setNewStatus(false);
    }

    /**
     * This implementation returns <code>{@link #updatableClone(MemoryModel)
     * updatableClone}({@link Arrays#SMM})</code>.
     *
     * @return a copy of this object.
     */
    @Override
    public UpdatableArray asCopyOnNextWrite() {
        return updatableClone(Arrays.SMM);
    }

    /**
     * This implementation returns this object.
     * Should be overridden if the inheritor is resizable.
     *
     * @return an unresizable view of this array.
     */
    public UpdatableObjectArray<E> asUnresizable() {
        return this;
    }

    @Override
    public <D> UpdatableObjectArray<D> cast(Class<D> elementType) {
        return InternalUtils.cast(super.cast(elementType));
    }

    @Override
    public String toString() {
        return "unresizable AlgART array Object[" + length + "]" + (underlyingArrays.length == 0 ? "" :
                " based on " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : ""));
    }
}
