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

/*Repeat(INCLUDE_FROM_FILE, AbstractFloatArray.java, all)
  value\s*==\s*\(float\)\s*value ==> value == 0 || value == 1 ;;
  ndexOf\((long.*?)\(float\)\s*value\) ==> ndexOf($1value != 0) ;;
  return\s+\(double\)\s*getFloat\(index\) ==> return getBit(index) ? 1.0 : 0.0 ;;
  return\s+\((int|long)\)\s*getFloat\(index\) ==> return getBit(index) ? 1 : 0 ;;
  FloatArray ==> BitArray ;;
  FloatBuffer ==> BitBuffer ;;
  getFloat ==> getBit ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean ;;
  PER_FLOAT ==> PER_BIT ;;
  private\s+(int|long)(\s+get) ==> public $1$2 ;;
  private\s+(long\s+(?:index|lastIndex)) ==> public $1 ;;
  (new\s+AbstractBitArray.*?)((?:\r(?!\n)|\n|\r\n)\s*public(\s+\w+)+\s+getData) ==> $1
            public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
                if (count < 0)
                    throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
                if (arrayPos < 0)
                    throw rangeException(arrayPos);
                if (arrayPos > length - count)
                    throw rangeException(arrayPos + count - 1);
                parent.getBits(offset + arrayPos, destArray, destArrayOffset, count);
            }

            @Override
            public long nextQuickPosition(long from) {
                if (from >= length)
                    return -1;
                if (from < 0)
                    from = 0;
                long p = offset + from;
                long qp = parent.nextQuickPosition(p);
                if (qp == -1)
                    return -1;
                assert qp >= p : "illegal nextQuickPosition implementation in " + parent;
                long result = from + (qp - p);
                return result >= length ? -1 : result;
            }
            $2
     !! Auto-generated: NOT EDIT !! */

import java.util.Objects;

/**
 * <p>Implementation of almost all basic functions of {@link BitArray} interface.
 * The only {@link BitArray#getBit(long)} method is not defined in this class;
 * all other methods are implemented via calls of {@link BitArray#getBit(long)}.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractBitArray extends AbstractArray implements BitArray {
    final boolean underlyingArraysAreParallel;

    /**
     * Creates an array with the given initial capacity and length.
     *
     * <p>The <code>underlyingArraysAreParallel</code> informs whether the passed underlying arrays (if they exist)
     * are "parallel" to this one and to each other.
     * Intuitively, it means that every element #<code>k</code> of this array is connected (for example, depends on)
     * the elements #<code>k</code> (of, maybe, #<code>k&plusmn;i</code>, where <code>i</code> is little) of the
     * underlying arrays.
     * Precisely, this argument affects the following in this implementation:
     *
     * <ol>
     * <li>If it is <code>true</code>, then all passed underlying arrays (if <code>underlyingArrays.length&gt;1</code>)
     * must have identical length &mdash; in another case, this constructor throws {@link SizeMismatchException}.
     * </li>
     *
     * <li>If it is <code>true</code>, then<ul>
     * <li>{@link #loadResources(ArrayContext context, long fromIndex, long toIndex)},</li>
     * <li>{@link #flushResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting)}
     * and</li>
     * <li>{@link #freeResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting)}
     * </li>
     * </ul> methods call
     * {@link #loadResources(ArrayContext context)},
     * {@link #flushResources(ArrayContext context, boolean forcePhysicalWriting)} and
     * {@link #freeResources(ArrayContext context, boolean forcePhysicalWriting)}
     * methods for the corresponding {@link #subArray subarrays} of all underlying
     * arrays. If this argument is <code>false</code>, then<ul>
     * <li>{@link #loadResources(ArrayContext context, long fromIndex, long toIndex)} does nothing,</li>
     * <li>{@link #flushResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting)}
     * and {@link #freeResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting)}
     * methods ignore their <code>fromIndex</code> / <code>toIndex</code> arguments and call
     * {@link #loadResources(ArrayContext context)},
     * {@link #flushResources(ArrayContext context, boolean forcePhysicalWriting)} and
     * {@link #freeResources(ArrayContext context, boolean forcePhysicalWriting)}
     * methods for <i>original</i> underlying arrays (not their subarrays).</li>
     * </ul>Of course, if you specify <code>underlyingArraysAreParallel=false</code>, you can override
     * {@link #loadResources(ArrayContext context, long fromIndex, long toIndex)},
     * {@link #flushResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting)}
     * and {@link #freeResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting)}
     * methods and offer better implementations for subarrays,
     * for example, that will work with suitable regions of the underlying arrays.
     * </li>
     * </ol>
     *
     * <p>The created array is not <i>{@link #isNew() new}</i> by default.
     * This is usually correct because this class is often used
     * for creating a view of another data. However, if the instance
     * of this class does not depend on any other data sources,
     * you may call {@link #setNewStatus(boolean) setNewStatus(true)} in
     * the constructor of your subclass.
     *
     * <p>The created array never has <i>new-read-only-view</i> status:
     * {@link Array#isNewReadOnlyView()} method always returns <code>false</code> in this class and its inheritors.
     *
     * @param initialCapacity             initial capacity of the array.
     * @param initialLength               initial length of the array.
     * @param underlyingArraysAreParallel whether the underlying arrays are "parallel" to this.
     * @param underlyingArrays            see the same argument of
     *                                    {@link AbstractArray#AbstractArray(long, long, Array...)}.
     * @throws NullPointerException     if <code>underlyingArrays</code> argument
     *                                  or some of <code>underlyingArrays[k]</code>
     *                                  elements is {@code null}.
     * @throws IllegalArgumentException if the <code>initialCapacity</code> or <code>initialLength</code> arguments
     *                                  are illegal (negative, or capacity &lt; length).
     * @throws SizeMismatchException    if <code>underlyingArraysAreParallel=true</code>,
     *                                  <code>underlyingArrays.length&gt;1</code> and some of the passed arrays
     *                                  have different lengths.
     */
    protected AbstractBitArray(
            long initialCapacity, long initialLength,
            boolean underlyingArraysAreParallel, Array... underlyingArrays) {
        super(initialCapacity, initialLength, underlyingArrays);
        if (initialLength < 0) {
            throw new IllegalArgumentException("Negative initialLength argument");
        }
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Negative initialCapacity argument");
        }
        if (initialLength > initialCapacity) {
            throw new IllegalArgumentException("initialCapacity argument must not be less than initialLength");
        }
        Objects.requireNonNull(underlyingArrays, "Null underlyingArrays argument");
        this.underlyingArraysAreParallel = underlyingArraysAreParallel;
        long len = -1;
        for (int k = 0; k < underlyingArrays.length; k++) {
            Objects.requireNonNull(underlyingArrays[k], "Null underlyingArrays[" + k + "] argument");
            if (underlyingArraysAreParallel) {
                if (k == 0) {
                    len = underlyingArrays[k].length();
                } else if (underlyingArrays[k].length() != len) {
                    throw new SizeMismatchException("underlyingArrays[" + k
                            + "].length() and underlyingArrays[0].length() mismatch");
                }
            }
        }
    }

    /**
     * Equivalent to the constructor {@link #AbstractBitArray(long, long, boolean, Array...)},
     * where both <code>initialCapacity</code> and <code>initialLength</code> arguments are equal to
     * <code>initialCapacityAndLength</code>.
     *
     * @param initialCapacityAndLength    initial capacity and length of the array.
     * @param underlyingArraysAreParallel see {@link #AbstractBitArray(long, long, boolean, Array...)}.
     * @param underlyingArrays            see {@link #AbstractBitArray(long, long, boolean, Array...)}.
     * @throws NullPointerException     if <code>underlyingArrays</code> argument
     *                                  or some of <code>underlyingArrays[k]</code>
     *                                  elements is {@code null}.
     * @throws IllegalArgumentException if <code>initialCapacityAndLength</code> argument is negative.
     * @throws SizeMismatchException    if <code>underlyingArraysAreParallel=true</code>,
     *                                  <code>underlyingArrays.length&gt;1</code> and some of the passed arrays
     *                                  have different lengths.
     */
    protected AbstractBitArray(
            long initialCapacityAndLength,
            boolean underlyingArraysAreParallel, Array... underlyingArrays) {
        this(initialCapacityAndLength, initialCapacityAndLength, underlyingArraysAreParallel, underlyingArrays);
    }

    @Override
    public Class<?> elementType() {
        return boolean.class;
    }

    @Override
    public Class<? extends BitArray> type() {
        return BitArray.class;
    }

    @Override
    public Class<? extends UpdatableBitArray> updatableType() {
        return UpdatableBitArray.class;
    }

    @Override
    public Class<? extends MutableBitArray> mutableType() {
        return MutableBitArray.class;
    }

    /**
     * This implementation is based on a loop of calls of {@link #getBit(long)} method.
     * Please override this method if it's possible to perform the same task more efficiently
     * than such a loop.
     *
     * @param arrayPos        starting position in this AlgART array.
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be copied.
     * @throws NullPointerException      if <code>destArray</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>destArray</code> argument is not an array.
     * @throws IndexOutOfBoundsException if copying causes access of data outside this array or target array.
     * @throws ArrayStoreException       if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}.
     * @throws ClassCastException        if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}
     *                                   (both this and <code>ArrayStoreException</code> are possible,
     *                                   depending on implementation).
     */
    @Override
    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        boolean[] a = (boolean[]) destArray;
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw rangeException(arrayPos);
        }
        if (arrayPos > length - count) {
            throw rangeException(arrayPos + count - 1);
        }
        for (long arrayPosMax = arrayPos + count; arrayPos < arrayPosMax; arrayPos++, destArrayOffset++) {
            a[destArrayOffset] = getBit(arrayPos);
        }
    }

    /**
     * This implementation calls {@link #getData(long, Object, int, int)}
     * with corresponding arguments.
     *
     * @param arrayPos  starting position in this AlgART array.
     * @param destArray the target Java array.
     * @throws NullPointerException      if <code>destArray</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>destArray</code> argument is not an array.
     * @throws IndexOutOfBoundsException if <code>arrayPos</code> is out of range <code>0..length()-1</code>.
     * @throws ArrayStoreException       if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}.
     * @throws ClassCastException        if <code>destArray</code> element type mismatches with this array
     *                                   {@link #elementType()}
     *                                   (both this and <code>ArrayStoreException</code> are possible,
     *                                   depending on implementation).
     */
    @Override
    public void getData(long arrayPos, Object destArray) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (arrayPos < 0 || arrayPos > length) {
            throw rangeException(arrayPos);
        }
        int count = ((boolean[]) destArray).length;
        if (count > length - arrayPos) {
            count = (int) (length - arrayPos);
        }
        getData(arrayPos, destArray, 0, count);
    }

    /**
     * This implementation returns <code>getBit(index)</code>.
     *
     * @param index index of the element to get.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range <code>0..length()-1</code>.
     */
    @Override
    public Object getElement(long index) {
        return getBit(index);
    }


    /**
     * This implementation returns new instance of {@link AbstractBitArray} with the same memory model,
     * underlying arrays and <code>underlyingArraysAreParallel</code> flag, that were passed
     * to the constructor of this instance,
     * and with overridden methods {@link #getBit(long)} and
     * {@link #getData(long, Object, int, int)},
     * calling the same methods of this instance with corresponding corrections of the arguments.
     *
     * <p>The returned instance also has overridden methods {@link #loadResources(ArrayContext, long, long)},
     * {@link #flushResources(ArrayContext, long, long, boolean)} and
     * {@link #freeResources(ArrayContext, long, long, boolean)},
     * that also call the same methods of this instance with corresponding correction of their <code>fromIndex</code>
     * argument.
     *
     * <p>The returned instance also has overridden method {@link #isLazy()},
     * that just calls the same methods of this instance with the same arguments.
     *
     * @param fromIndex low endpoint (inclusive) of the subarray.
     * @param toIndex   high endpoint (exclusive) of the subarray.
     * @return a view of the specified range within this array.
     * @throws IndexOutOfBoundsException for illegal fromIndex and toIndex
     *                                   (fromIndex &lt; 0 || toIndex &gt; length() || fromIndex &gt; toIndex).
     */
    @Override
    public Array subArray(long fromIndex, long toIndex) {
        checkSubArrayArguments(fromIndex, toIndex);
        final AbstractBitArray parent = this;
        final long offset = fromIndex;
        return new AbstractBitArray(toIndex - fromIndex, underlyingArraysAreParallel, underlyingArrays) {
            @Override
            public boolean getBit(long index) {
                if (index < 0 || index >= length) {
                    throw rangeException(index);
                }
                return parent.getBit(offset + index);
            }

            @Override
            public long indexOf(long lowIndex, long highIndex, boolean value) {
                if (lowIndex < 0) {
                    lowIndex = 0;
                }
                if (highIndex > length) {
                    highIndex = length;
                }
                if (highIndex <= lowIndex) {
                    // this check guarantees that overflow is impossible:
                    // offset + lowIndex <= offset + highIndex <= offset + length = toIndex <= Long.MAX_VALUE
                    return -1;
                }
                long result = parent.indexOf(offset + lowIndex, offset + highIndex, value);
                return result == -1 ? -1 : result - offset;
            }

            @Override
            public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
                if (lowIndex < 0) {
                    lowIndex = 0;
                }
                if (highIndex > length) {
                    highIndex = length;
                }
                if (highIndex <= lowIndex) {
                    // this check guarantees that overflow is impossible:
                    // offset + lowIndex <= offset + highIndex <= offset + length = toIndex <= Long.MAX_VALUE
                    return -1;
                }
                long result = parent.lastIndexOf(offset + lowIndex, offset + highIndex, value);
                return result == -1 ? -1 : result - offset;
            }

            @Override
            public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
                if (count < 0)
                    throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
                if (arrayPos < 0)
                    throw rangeException(arrayPos);
                if (arrayPos > length - count)
                    throw rangeException(arrayPos + count - 1);
                parent.getBits(offset + arrayPos, destArray, destArrayOffset, count);
            }

            @Override
            public long nextQuickPosition(long from) {
                if (from >= length)
                    return -1;
                if (from < 0)
                    from = 0;
                long p = offset + from;
                long qp = parent.nextQuickPosition(p);
                if (qp == -1)
                    return -1;
                assert qp >= p : "illegal nextQuickPosition implementation in " + parent;
                long result = from + (qp - p);
                return result >= length ? -1 : result;
            }

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

    @Override
    public DataBitBuffer buffer(DataBuffer.AccessMode mode, long capacity) {
        return (DataBitBuffer) super.buffer(mode, capacity);
    }

    @Override
    public DataBitBuffer buffer(DataBuffer.AccessMode mode) {
        return (DataBitBuffer) super.buffer(mode);
    }

    @Override
    public DataBitBuffer buffer(long capacity) {
        return (DataBitBuffer) super.buffer(capacity);
    }

    @Override
    public DataBitBuffer buffer() {
        return (DataBitBuffer) super.buffer();
    }

    public long bitsPerElement() {
        return Arrays.BITS_PER_BIT;
    }

    public double getDouble(long index) {
        return getBit(index) ? 1.0 : 0.0;
    }

    /**
     * <!--index_double (necessary for preprocessing)-->
     * This implementation returns
     * <code>value == 0 || value == 1 ? {@link #indexOf(long, long, boolean)
     * indexOf}(lowIndex, highIndex, value != 0) : -1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    public long indexOf(long lowIndex, long highIndex, double value) {
        return value == 0 || value == 1 ? indexOf(lowIndex, highIndex, value != 0) : -1;
    }

    /**
     * <!--index_double (necessary for preprocessing)-->
     * This implementation returns
     * <code>value == 0 || value == 1 ? {@link #lastIndexOf(long, long, boolean)
     * lastIndexOf}(lowIndex, highIndex, value != 0) : -1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    public long lastIndexOf(long lowIndex, long highIndex, double value) {
        return value == 0 || value == 1 ? lastIndexOf(lowIndex, highIndex, value != 0) : -1;
    }

    public long getLong(long index) { // should not be available for boolean/double
        return getBit(index) ? 1 : 0;
    }

    public int getInt(long index) { // should not be available for boolean/double
        return getBit(index) ? 1 : 0;
    }

    /**
     * <!--index_long (necessary for preprocessing)-->
     * This implementation returns
     * <code>value == 0 || value == 1 ? {@link #indexOf(long, long, boolean)
     * indexOf}(lowIndex, highIndex, value != 0) : -1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    public long indexOf(long lowIndex, long highIndex, long value) {
        return value == 0 || value == 1 ? indexOf(lowIndex, highIndex, value != 0) : -1;
    }

    /**
     * <!--index_long (necessary for preprocessing)-->
     * This implementation returns
     * <code>value == 0 || value == 1 ? {@link #lastIndexOf(long, long, boolean)
     * lastIndexOf}(lowIndex, highIndex, value != 0) : -1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    public long lastIndexOf(long lowIndex, long highIndex, long value) {
        return value == 0 || value == 1 ? lastIndexOf(lowIndex, highIndex, value != 0) : -1;
    }

    public abstract boolean getBit(long index);

    /**
     * This implementation is based on a loop of calls of {@link #getBit(long)} method
     * from index <code>max(lowIndex,0)</code> until index <code>min({@link #length()},highIndex)-1</code>.
     * Please override this method if it's possible to perform the same task more efficiently
     * than such a loop.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the first occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    public long indexOf(long lowIndex, long highIndex, boolean value) {
        for (long k = Math.max(lowIndex, 0), n = Math.min(length(), highIndex); k < n; k++) {
            if (getBit(k) == value) {
                return k;
            }
        }
        return -1;
    }

    /**
     * This implementation is based on a loop of calls of {@link #getBit(long)} method
     * from index <code>min({@link #length()},highIndex)-1</code> back until index <code>max(lowIndex,0)</code>.
     * Please override this method if it's possible to perform the same task more efficiently
     * than such a loop.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return the index of the last occurrence of this value in this array
     * in range <code>lowIndex&lt;=index&lt;highIndex</code>,
     * or <code>-1</code> if this value does not occur in this range.
     */
    public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
        for (long k = Math.min(length(), highIndex), low = Math.max(lowIndex, 0); k > low; ) {
            // warning: highIndex-1 can be invalid value Long.MAX_VALUE
            if (getBit(--k) == value) {
                return k;
            }
        }
        return -1;
    }

    /**
     * This implementation returns <code>true</code>.
     * Should be overridden if the inheritor is mutable.
     *
     * @return <code>true</code> if this instance is immutable.
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * This implementation returns <code>true</code>.
     * Should be overridden if the inheritor is resizable
     *
     * @return <code>true</code> if this instance is unresizable.
     */
    @Override
    public boolean isUnresizable() {
        return true;
    }

    /**
     * This implementation does nothing.
     *
     * @throws UnallowedMutationError never in this implementation.
     */
    @Override
    public void checkUnallowedMutation() throws UnallowedMutationError {
    }

    /**
     * This implementation calls {@link #asImmutable()} and returns its result.
     *
     * @return a trusted immutable view of this array (or a reference to this array if it is already
     * trusted immutable).
     */
    @Override
    public BitArray asTrustedImmutable() {
        return asImmutable();
    }

    /**
     * This implementation returns this object.
     * Should be overridden if the inheritor is mutable.
     *
     * @return a copy-on-next-write view of this array (or a reference to this array if it is
     * immutable or already copy-on-next-write).
     */
    @Override
    public Array asCopyOnNextWrite() {
        return this;
    }

    /**
     * This implementation returns <code>false</code>.
     * Should be overridden if the inheritor is mutable.
     *
     * @return <code>true</code> if this array is in copy-on-next-write mode
     */
    @Override
    public boolean isCopyOnNextWrite() {
        return false;
    }

    /**
     * This implementation returns this object.
     * Should be overridden if the inheritor is mutable.
     *
     * @return an immutable view of this array (or a reference to this array if it is immutable).
     */
    @Override
    public BitArray asImmutable() {
        return this;
    }

    @Override
    public MutableBitArray mutableClone(MemoryModel memoryModel) {
        return (MutableBitArray) super.mutableClone(memoryModel);
    }

    @Override
    public UpdatableBitArray updatableClone(MemoryModel memoryModel) {
        return (UpdatableBitArray) super.updatableClone(memoryModel);
    }

    public boolean[] ja() {
        return (boolean[]) super.ja();
    }

    /**
     * This implementation calls
     * {@link #loadResources(ArrayContext, long, long) loadResources(context, 0, length())}.
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     */
    @Override
    public void loadResources(ArrayContext context) {
        loadResources(context, 0, length());
    }

    /**
     * This implementation calls
     * {@link #flushResources(ArrayContext, long, long, boolean)
     * flushResources(context, 0, length(), forcePhysicalWriting)}.
     *
     * @param context              the context of execution; can be {@code null}, then it will be ignored.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device?
     */
    @Override
    public void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
        flushResources(context, 0, length(), forcePhysicalWriting);
    }

    /**
     * This implementation calls
     * {@link #freeResources(ArrayContext, long, long, boolean)
     * freeResources(context, 0, length()), forcePhysicalWriting)}.
     *
     * @param context              the context of execution; can be {@code null}, then it will be ignored.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device?
     */
    @Override
    public void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
        freeResources(context, 0, length(), forcePhysicalWriting);
    }

    /**
     * This method implements all actions that should be performed by
     * <code>{@link #subArray subArray}(fromIndex, toIndex).{@link #loadResources(ArrayContext)
     * loadResources}(context)</code> call.
     * This default implementation calls {@link #loadResources(ArrayContext) loadResources(c)}
     * (where <code>c</code> is a necessary {@link ArrayContext#part(long, long, long) part} of the passed context)
     * for the corresponding subarray
     * of all underlying arrays, passed via the last argument of the constructor,
     * if the <code>underlyingArraysAreParallel</code> constructor argument was <code>true</code>,
     * or does nothing in another case.
     *
     * @param context   the context of execution; can be {@code null}, then it will be ignored.
     * @param fromIndex low endpoint (inclusive) of the subarray that should be loaded.
     * @param toIndex   high endpoint (exclusive) of the subarray that should be loaded.
     * @throws IndexOutOfBoundsException for illegal fromIndex and toIndex
     *                                   (fromIndex &lt; 0 || toIndex &gt; length() || fromIndex &gt; toIndex).
     */
    protected void loadResources(ArrayContext context, long fromIndex, long toIndex) {
        checkSubArrayArguments(fromIndex, toIndex);
        if (underlyingArraysAreParallel) {
            for (int k = 0; k < underlyingArrays.length; k++) {
                underlyingArrays[k].subArray(fromIndex, toIndex).loadResources(
                        context == null ? null : context.part(k, k + 1, underlyingArrays.length));
            }
        }
    }

    /**
     * This method implements all actions that should be performed by
     * <code>{@link #subArray subArray}(fromIndex, toIndex).{@link #flushResources(ArrayContext, boolean)
     * flushResources(context, forcePhysicalWriting)}</code> call.
     * This default implementation calls {@link #flushResources(ArrayContext, boolean)
     * flushResources(c, forcePhysicalWriting)}
     * (where <code>c</code> is a necessary {@link ArrayContext#part(long, long, long) part} of the passed context)
     * for the corresponding subarray of all underlying arrays, passed via the last argument of the constructor,
     * if the <code>underlyingArraysAreParallel</code> constructor argument was <code>true</code>,
     * or for original underlying arrays in another case
     * (alike {@link AbstractArray#flushResources(ArrayContext, boolean)}).
     *
     * @param context              the context of execution; can be {@code null}, then it will be ignored.
     * @param fromIndex            low endpoint (inclusive) of the subarray that should be flushed.
     * @param toIndex              high endpoint (exclusive) of the subarray that should be flushed.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device?
     * @throws IndexOutOfBoundsException for illegal fromIndex and toIndex
     *                                   (fromIndex &lt; 0 || toIndex &gt; length() || fromIndex &gt; toIndex).
     */
    protected void flushResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting) {
        checkSubArrayArguments(fromIndex, toIndex);
        if (underlyingArraysAreParallel) {
            for (int k = 0; k < underlyingArrays.length; k++) {
                underlyingArrays[k].subArray(fromIndex, toIndex).flushResources(
                        context == null ? null : context.part(k, k + 1, underlyingArrays.length),
                        forcePhysicalWriting);
            }
        } else {
            super.flushResources(context, forcePhysicalWriting);
        }
    }

    /**
     * This method implements all actions that should be performed by
     * <code>{@link #subArray subArray}(fromIndex, toIndex).{@link #freeResources(ArrayContext, boolean)
     * freeResources(context, forcePhysicalWriting)}</code> call.
     * This default implementation calls {@link #freeResources(ArrayContext, boolean)
     * freeResources(c, forcePhysicalWriting)}
     * (where <code>c</code> is a necessary {@link ArrayContext#part(long, long, long) part} of the passed context)
     * for the corresponding subarray of all underlying arrays, passed via the last argument of the constructor,
     * if the <code>underlyingArraysAreParallel</code> constructor argument was <code>true</code>,
     * or for original underlying arrays in another case
     * (alike {@link AbstractArray#freeResources(ArrayContext, boolean)}).
     *
     * @param context              the context of execution; can be {@code null}, then it will be ignored.
     * @param fromIndex            low endpoint (inclusive) of the subarray that should be freed.
     * @param toIndex              high endpoint (exclusive) of the subarray that should be freed.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device?
     * @throws IndexOutOfBoundsException for illegal fromIndex and toIndex
     *                                   (fromIndex &lt; 0 || toIndex &gt; length() || fromIndex &gt; toIndex).
     */
    protected void freeResources(ArrayContext context, long fromIndex, long toIndex, boolean forcePhysicalWriting) {
        checkSubArrayArguments(fromIndex, toIndex);
        if (underlyingArraysAreParallel) {
            for (int k = 0; k < underlyingArrays.length; k++) {
                underlyingArrays[k].subArray(fromIndex, toIndex).freeResources(
                        context == null ? null : context.part(k, k + 1, underlyingArrays.length),
                        forcePhysicalWriting);
            }
        } else {
            super.freeResources(context, forcePhysicalWriting);
        }
    }

    @Override
    public String toString() {
        return "immutable AlgART array boolean[" + length + "]" + (underlyingArrays.length == 0 ? "" :
                " based on " + underlyingArrays.length + " underlying array" + (underlyingArrays.length > 1 ? "s" : ""));
    }

    Object javaArrayInternal() {
        return null;
    }

    int javaArrayOffsetInternal() {
        return 0;
    }
    /*Repeat.IncludeEnd*/

    public double minPossibleValue(double valueForFloatingPoint) {
        return minPossibleValue();
    }

    public double maxPossibleValue(double valueForFloatingPoint) {
        return maxPossibleValue();
    }

    public long minPossibleValue() {
        return 0;
    }

    public long maxPossibleValue() {
        return 1;
    }

    /**
     * This implementation is based on a loop of calls of {@link #getBit(long)} method.
     * Please override this method if it's possible to perform the same task more efficiently
     * than such a loop.
     *
     * @param arrayPos        starting position in this AlgART array.
     * @param destArray       the target packed bit array.
     * @param destArrayOffset starting position in the target packed bit array.
     * @param count           the number of bits to be copied.
     * @throws NullPointerException      if <code>destArray</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if copying would cause access of data outside this array or target Java array.
     * @see BitArray#getData(long, Object, int, int)
     * @see UpdatableBitArray#setBits(long, long[], long, long)
     * @see PackedBitArrays
     */
    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        Objects.requireNonNull(destArray, "Null destArray argument");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
        }
        if (arrayPos < 0) {
            throw rangeException(arrayPos, length, getClass());
        }
        if (arrayPos > length - count) {
            throw rangeException(arrayPos + count - 1, length, getClass());
        }
        for (long arrayPosMax = arrayPos + count; arrayPos < arrayPosMax; arrayPos++, destArrayOffset++) {
            PackedBitArrays.setBit(destArray, destArrayOffset, getBit(arrayPos));
        }
    }

    /**
     * This implementation returns <code>-1</code> always.
     *
     * @param position some index inside this bit array.
     * @return <code>-1</code>.
     */
    public long nextQuickPosition(long position) {
        return -1;
    }
}
