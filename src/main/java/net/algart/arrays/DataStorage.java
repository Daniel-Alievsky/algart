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

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.nio.ByteOrder;

/**
 * <p>Data storage: low-level analog of {@link MutableArray},
 * used for implementation of {@link BufferMemoryModel} and {@link LargeMemoryModel}.</p>
 *
 * <p>All views of an AlgART array (subarrays, immutable views, etc.)
 * share the same underlying data storage.</p>
 *
 * <p>Inheritors of this class MUST NOT CONTAIN a link to AlgART array:
 * such link make impossible deallocation of the array before deallocation of the storage.</p>
 *
 * @author Daniel Alievsky
 */
abstract class DataStorage {
    static boolean DO_LAZY_INIT = InternalUtils.getBooleanProperty( // It's non-final to allow changing via reflection.
            "net.algart.arrays.DataStorage.doLazyInit", true); // true by default
    static byte ZERO_INIT_FILLER = 0;   // for debugging only! It's non-final to allow changing via reflection.
    // ZERO_INIT_FILLER is not used by getData/getBits!

    static final int BYTES_PER_BYTE_LOG = 0; // = log 1
    static final int BYTES_PER_CHAR_LOG = 1; // = log 2
    static final int BYTES_PER_SHORT_LOG = 1; // = log 2
    static final int BYTES_PER_INT_LOG = 2; // = log 4
    static final int BYTES_PER_LONG_LOG = 3; // = log 8
    static final int BYTES_PER_FLOAT_LOG = 2; // = log 4
    static final int BYTES_PER_DOUBLE_LOG = 3; // = log 8

    static final Boolean booleanZero = false;
    static final Character charZero = 0;
    static final Byte byteZero = 0;
    static final Short shortZero = 0;
    static final Integer intZero = 0;
    static final Long longZero = 0L;
    static final Float floatZero = 0.0f;
    static final Double doubleZero = 0.0;

    static long maxSupportedLengthImpl(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (elementType == boolean.class || elementType == byte.class) {
            return Long.MAX_VALUE;
        }
        if (elementType == char.class) {
            return Long.MAX_VALUE >> BYTES_PER_CHAR_LOG;
        } else if (elementType == short.class) {
            return Long.MAX_VALUE >> BYTES_PER_SHORT_LOG;
        } else if (elementType == int.class) {
            return Long.MAX_VALUE >> BYTES_PER_INT_LOG;
        } else if (elementType == long.class) {
            return Long.MAX_VALUE >> BYTES_PER_LONG_LOG;
        } else if (elementType == float.class) {
            return Long.MAX_VALUE >> BYTES_PER_FLOAT_LOG;
        } else if (elementType == double.class) {
            return Long.MAX_VALUE >> BYTES_PER_DOUBLE_LOG;
        } else {
            return -1;
        }
    }

    static void freeAllResources() {
        Set<DataStorage> all;
        synchronized (MappedDataStorages.allNonFinalizedMappedStorages) {
            all = new HashSet<DataStorage>(MappedDataStorages.allNonFinalizedMappedStorages);
        }
        for (DataStorage storage : all) {
            storage.freeResources(null, false); // implementations here do not use the first argument
        }
    }


    /**
     * Returns an instance of {@link LargeMemoryModel} that will create storages
     * with characteristics identical to this one (for any element types).
     *
     * @return an instance of {@link LargeMemoryModel} compatible with this storage.
     */
    abstract MemoryModel newCompatibleMemoryModel();

    /**
     * Creates new empty storage with characteristics identical to this one
     * (in particular, with the same element type).
     *
     * @param unresizable <code>true</code> if this storage will be used for unresizable array.
     * @return new compatible empty storage.
     */
    abstract DataStorage newCompatibleEmptyStorage(boolean unresizable);

    /**
     * Returns the binary logarithm of the number of bytes per one <code>Buffer</code> element
     * used by this storage. For bit storages, returns 3, as for <code>long</code> values.
     *
     * @return the binary logarithm of the number of bytes per one <code>Buffer</code> element.
     */
    abstract int bytesPerBufferElementLog();

    /**
     * Returns the byte order used by this storage.
     *
     * @return the byte order used by this storage.
     */
    abstract ByteOrder byteOrder();

    /**
     * Creates new <code>a</code> zero-filled array, currently stored in this storage.
     * The only usage of <code>a</code> argument should
     * be correction of information about it that may be stored here.
     *
     * @param capacity    new array capacity.
     * @param unresizable if <code>true</code>, the {@link #changeCapacity(long, long, long)} method
     *                    should never be called.
     */
    abstract void allocate(long capacity, boolean unresizable);

    /**
     * Changes the capacity of <code>a</code> array, currently stored in this storage
     * starting from <code>offset</code> position,
     * to <code>newCapacity</code>, copies elements <code>offset..offset+length-1</code>
     * to new allocated array, and returns the resulting storage.
     * Result may be equal to this one or may be a new instance,
     * if all data were really copied into new created storage.
     *
     * <p>If the capacity is increased, all new elements
     * are zero. The only usage of <code>a</code> argument should
     * be correction of information about it that may be stored here.
     *
     * @param newCapacity new array capacity.
     * @param offset      start position of the array which should be copied to new array.
     * @param length      number of elements which should be copied to new array.
     * @return this instance or new created storage.
     */
    abstract DataStorage changeCapacity(long newCapacity, long offset, long length);

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a bit storage
     * <b>in a non-thread-safe manner</b>.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the bit to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a bit storage.
     */
    void setBitNoSync(long index, boolean value) {
        setBit(index, value);
    }

    long getBits64(long arrayPos, int count) {
        throw new UnsupportedOperationException("It is not a bit storage");
    }

    void setBits64(long arrayPos, long bits, int count) {
        throw new UnsupportedOperationException("It is not a bit storage");
    }

    void setBits64NoSync(long arrayPos, long bits, int count) {
        setBits64(arrayPos, bits, count);
    }

    /*Repeat() boolean|bit ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit         ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double
     */

    /**
     * Returns the bit #<code>index</code> if it is a bit storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the bit at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a bit storage.
     */
    boolean getBit(long index) {
        throw new UnsupportedOperationException("It is not a bit storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a bit storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the bit to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a bit storage.
     */
    void setBit(long index, boolean value) {
        throw new UnsupportedOperationException("It is not a bit storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getBit(long) getBit}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a bit storage.
     */
    long indexOfBit(long lowIndex, long highIndex, boolean value) {
        throw new UnsupportedOperationException("It is not a bit storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getBit(long) getBit}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this bit in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this bit does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
        throw new UnsupportedOperationException("It is not a bit storage");
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Returns the char #<code>index</code> if it is a char storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the char at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a char storage.
     */
    char getChar(long index) {
        throw new UnsupportedOperationException("It is not a char storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a char storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the char to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a char storage.
     */
    void setChar(long index, char value) {
        throw new UnsupportedOperationException("It is not a char storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getChar(long) getChar}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a char storage.
     */
    long indexOfChar(long lowIndex, long highIndex, char value) {
        throw new UnsupportedOperationException("It is not a char storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getChar(long) getChar}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this char in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this char does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfChar(long lowIndex, long highIndex, char value) {
        throw new UnsupportedOperationException("It is not a char storage");
    }

    /**
     * Returns the byte #<code>index</code> if it is a byte storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the byte at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a byte storage.
     */
    byte getByte(long index) {
        throw new UnsupportedOperationException("It is not a byte storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a byte storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the byte to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a byte storage.
     */
    void setByte(long index, byte value) {
        throw new UnsupportedOperationException("It is not a byte storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getByte(long) getByte}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a byte storage.
     */
    long indexOfByte(long lowIndex, long highIndex, byte value) {
        throw new UnsupportedOperationException("It is not a byte storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getByte(long) getByte}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this byte in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this byte does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
        throw new UnsupportedOperationException("It is not a byte storage");
    }

    /**
     * Returns the short #<code>index</code> if it is a short storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the short at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a short storage.
     */
    short getShort(long index) {
        throw new UnsupportedOperationException("It is not a short storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a short storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the short to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a short storage.
     */
    void setShort(long index, short value) {
        throw new UnsupportedOperationException("It is not a short storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getShort(long) getShort}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a short storage.
     */
    long indexOfShort(long lowIndex, long highIndex, short value) {
        throw new UnsupportedOperationException("It is not a short storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getShort(long) getShort}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this short in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this short does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfShort(long lowIndex, long highIndex, short value) {
        throw new UnsupportedOperationException("It is not a short storage");
    }

    /**
     * Returns the int #<code>index</code> if it is a int storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the int at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a int storage.
     */
    int getInt(long index) {
        throw new UnsupportedOperationException("It is not a int storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a int storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the int to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a int storage.
     */
    void setInt(long index, int value) {
        throw new UnsupportedOperationException("It is not a int storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getInt(long) getInt}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a int storage.
     */
    long indexOfInt(long lowIndex, long highIndex, int value) {
        throw new UnsupportedOperationException("It is not a int storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getInt(long) getInt}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this int in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this int does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfInt(long lowIndex, long highIndex, int value) {
        throw new UnsupportedOperationException("It is not a int storage");
    }

    /**
     * Returns the long #<code>index</code> if it is a long storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the long at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a long storage.
     */
    long getLong(long index) {
        throw new UnsupportedOperationException("It is not a long storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a long storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the long to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a long storage.
     */
    void setLong(long index, long value) {
        throw new UnsupportedOperationException("It is not a long storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getLong(long) getLong}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a long storage.
     */
    long indexOfLong(long lowIndex, long highIndex, long value) {
        throw new UnsupportedOperationException("It is not a long storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getLong(long) getLong}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this long in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this long does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfLong(long lowIndex, long highIndex, long value) {
        throw new UnsupportedOperationException("It is not a long storage");
    }

    /**
     * Returns the float #<code>index</code> if it is a float storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the float at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a float storage.
     */
    float getFloat(long index) {
        throw new UnsupportedOperationException("It is not a float storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a float storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the float to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a float storage.
     */
    void setFloat(long index, float value) {
        throw new UnsupportedOperationException("It is not a float storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getFloat(long) getFloat}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a float storage.
     */
    long indexOfFloat(long lowIndex, long highIndex, float value) {
        throw new UnsupportedOperationException("It is not a float storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getFloat(long) getFloat}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this float in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this float does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
        throw new UnsupportedOperationException("It is not a float storage");
    }

    /**
     * Returns the double #<code>index</code> if it is a double storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @return the double at the specified position in the source array.
     * @throws UnsupportedOperationException if it is not a double storage.
     */
    double getDouble(long index) {
        throw new UnsupportedOperationException("It is not a double storage");
    }

    /**
     * Sets the element #<code>index</code> to the specified <code>value</code> if it is a double storage.
     *
     * @param index the index of the data element in the source array (not subarray).
     * @param value the double to be stored at the specified position.
     * @throws UnsupportedOperationException if it is not a double storage.
     */
    void setDouble(long index, double value) {
        throw new UnsupportedOperationException("It is not a double storage");
    }

    /**
     * Returns the minimal index <code>k</code>, so that <code>lowIndex&lt;=k&lt;highIndex</code>
     * and <code>{@link #getDouble(long) getDouble}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>lowIndex&gt;=highIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index for search (inclusive).
     * @param highIndex the high index for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the first occurrence of this element in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this element does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     * @throws UnsupportedOperationException if it is not a double storage.
     */
    long indexOfDouble(long lowIndex, long highIndex, double value) {
        throw new UnsupportedOperationException("It is not a double storage");
    }

    /**
     * Returns the maximal index <code>k</code>, so that <code>highIndex&gt;k&gt;=lowIndex</code>
     * and <code>{@link #getDouble(long) getDouble}(k)==value</code>,
     * or <code>-1</code> if there is no such element.
     *
     * <p>If <code>highIndex&lt;=lowIndex</code>, this method returns <code>-1</code>.
     *
     * @param lowIndex  the low index in the array for search (inclusive);
     *                  pass <code>0</code> to search all remaining elements.
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value of element to be found.
     * @return the index of the last occurrence of this double in range <code>lowIndex..highIndex-1</code>,
     * or <code>-1</code> if this double does not occur
     * or if <code>lowIndex&gt;=highIndex</code>.
     */
    long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
        throw new UnsupportedOperationException("It is not a double storage");
    }

    /*Repeat.AutoGeneratedEnd*/

    /**
     * Copies element #<code>srcIndex</code> to position #<code>destIndex</code> inside
     * the stored array (not subarray).
     *
     * @param destIndex index of the element to replace.
     * @param srcIndex  index of the element to be copied.
     */
    abstract void copy(long destIndex, long srcIndex);

    /**
     * Swaps elements at positions #<code>firstIndex</code> and #<code>secondIndex</code> inside
     * the stored array (not subarray).
     *
     * @param firstIndex  first index of the element to exchange.
     * @param secondIndex second index of the element to exchange.
     */
    abstract void swap(long firstIndex, long secondIndex);

    /**
     * Copies <code>count</code> elements of this storage, starting from <code>pos</code> index,
     * into the specified Java array of corresponding type, starting from <code>destArrayOffset</code> index.
     *
     * @param pos             starting position in the stored AlgART array (not subarray).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be copied.
     */
    abstract void getData(long pos, Object destArray, int destArrayOffset, int count);

    /**
     * Copies <code>count</code> elements from the specified Java array of corresponding type,
     * starting from <code>srcArrayOffset</code> index,
     * into this storage, starting from <code>pos</code> index.
     *
     * @param pos            starting position in the stored AlgART array (not subarray).
     * @param srcArray       the source Java array.
     * @param srcArrayOffset starting position in the source Java array.
     * @param count          the number of elements to be copied.
     */
    abstract void setData(long pos, Object srcArray, int srcArrayOffset, int count);

    /**
     * Fills elements <code>#pos..#pos+count-1</code> of this storage
     * by the filler stored in <code>fillerWrapper</code>
     * (<code>Boolean</code>, <code>Character</code>, <code>Byte</code>, <code>Short</code>,
     * <code>Integer</code>, <code>Long</code>, <code>Float</code> or <code>Double</code>).
     *
     * @param pos           starting position in the stored AlgART array (not subarray).
     * @param count         the number of elements to be copied.
     * @param fillerWrapper the wrapper of primitive type storing the filling value.
     */
    abstract void fillData(long pos, long count, Object fillerWrapper);

    /**
     * Fills elements <code>#pos..#pos+count-1</code> of this storage by zero.
     *
     * @param pos   starting position in the stored AlgART array (not subarray).
     * @param count the number of elements to be filled.
     */
    abstract void clearData(long pos, long count);

    /**
     * Copies <code>count</code> elements of <code>src</code> storage, starting from <code>srcPos</code> index,
     * to this storage, starting from <code>destPos</code> index.
     *
     * <p>This operation may be not supported for some kind of the passed storage;
     * in this case, this method must return <code>false</code>.
     * In a case of success this method returns <code>true</code>.
     * Always returns <code>true</code> is <code>src==this</code> or if <code>src</code>
     * was created via <code>this.{@link #newCompatibleEmptyStorage(boolean)}</code>.
     *
     * <p><i>This method works correctly even if the copied areas overlap</i>,
     * i.e. if <code>src</code> is this storage and <code>Math.abs(destPos - srcPos) &lt; count</code>.
     * More precisely, in this case the copying is performed as if the
     * elements at positions <code>srcPos..srcPos+count-1</code>
     * were first copied to a temporary array with <code>count</code> elements
     * and then the contents of the temporary array were copied into positions
     * <code>destPos..destPos+count-1</code> of this array.
     *
     * @param src     the source storage.
     * @param srcPos  starting position in the source storage.
     * @param destPos starting position in this storage.
     * @param count   the number of elements to be copied.
     * @return <code>true</code> if elements were copied, <code>false</code> if this method did nothing.
     */
    abstract boolean copy(DataStorage src, long srcPos, long destPos, long count);

    /**
     * Swaps <code>count</code> elements of <code>another</code> storage, starting from <code>anotherPos</code> index,
     * and the same number of elements of this storage, starting from <code>thisPos</code> index.
     *
     * <p>This operation may be not supported for some kind of the passed storage;
     * in this case, this method must return <code>false</code>.
     * In a case of success this method returns <code>true</code>.
     * Always returns <code>true</code> is <code>another==this</code>.
     *
     * <p>Some elements may be swapped incorrectly if this storage and <code>another</code> is the same object
     * and the swapped areas overlap (<code>Math.abs(anotherPos - thisPos) &lt; count</code>).
     *
     * @param another    another storage.
     * @param anotherPos starting position in another storage.
     * @param thisPos    starting position in this storage.
     * @param count      the number of elements to be exchanged.
     * @return <code>true</code> if elements were copied, <code>false</code> if this method did nothing.
     */
    abstract boolean swap(DataStorage another, long anotherPos, long thisPos, long count);

    /**
     * Replaces <code>count</code> elements of Java array <code>destArray</code> of corresponding type,
     * starting from <code>destArrayOffset</code> index,
     * with the minimum of them and corresponding <code>count</code>
     * elements of this storage, starting from <code>pos</code> index.
     * Must not be called for bit storages.
     *
     * @param pos             starting position in the stored AlgART array (not subarray).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    abstract void minData(long pos, Object destArray, int destArrayOffset, int count);

    /**
     * Replaces <code>count</code> elements of Java array <code>destArray</code> of corresponding type,
     * starting from <code>destArrayOffset</code> index,
     * with the maximum of them and corresponding <code>count</code>
     * elements of this storage, starting from <code>pos</code> index.
     * Must not be called for bit storages.
     *
     * @param pos             starting position in the stored AlgART array (not subarray).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    abstract void maxData(long pos, Object destArray, int destArrayOffset, int count);

    /**
     * Replaces <code>count</code> elements of Java array <code>destArray</code>,
     * starting from <code>destArrayOffset</code> index,
     * with the sum of them and corresponding <code>count</code>
     * elements of this storage, starting from <code>pos</code> index.
     * Must not be called for bit storages.
     *
     * @param pos             starting position in the stored AlgART array (not subarray).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     */
    abstract void addData(long pos, int[] destArray, int destArrayOffset, int count);

    /**
     * Replaces <code>count</code> elements of Java array <code>destArray</code>,
     * starting from <code>destArrayOffset</code> index,
     * with the sum of them and corresponding <code>count</code>
     * elements of this storage, multiplied by <code>mult</code> argument,
     * starting from <code>pos</code> index.
     * Must not be called for bit storages.
     *
     * @param pos             starting position in the stored AlgART array (not subarray).
     * @param destArray       the target Java array.
     * @param destArrayOffset starting position in the target Java array.
     * @param count           the number of elements to be replaced.
     * @param mult            the elements from this storage are multiplied by this value before adding.
     */
    abstract void addData(
            long pos, double[] destArray, int destArrayOffset, int count, double mult);

    /**
     * Replaces <code>count</code> elements of Java array <code>destArray</code> of corresponding type,
     * starting from <code>destArrayOffset</code> index,
     * with the difference of them and corresponding <code>count</code>
     * elements of this storage, starting from <code>pos</code> index.
     * If <code>truncateOverflows</code> argument is <code>true</code>,
     * and the element type is <code>byte</code>, <code>short</code>, <code>int</code> or <code>char</code>,
     * then the difference is truncated
     * to <code>0..0xFF</code>, <code>0..0xFFFF</code>,
     * <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code>, <code>0..0xFFFF</code>
     * range before assigning to <code>dest</code> elements.
     * The byte / short elements are considered to be unsigned.
     * Must not be called for bit storages.
     *
     * @param pos               starting position in the stored AlgART array (not subarray).
     * @param destArray         the target Java array.
     * @param destArrayOffset   starting position in the target Java array.
     * @param count             the number of elements to be replaced.
     * @param truncateOverflows whether the results should be truncated in a case of overflow.
     */
    abstract void subtractData(
            long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows);

    /**
     * Replaces <code>count</code> elements of Java array <code>destArray</code> of corresponding type,
     * starting from <code>destArrayOffset</code> index,
     * with the absolute value of the difference of them
     * and corresponding <code>count</code>
     * elements of this storage, starting from <code>pos</code> index.
     * If <code>truncateOverflows</code> argument is <code>true</code>,
     * and the element type is <code>int</code>,
     * then the difference is truncated
     * to <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code>
     * range before assigning to <code>dest</code> elements.
     * The byte / short elements are considered to be unsigned.
     * Must not be called for bit storages.
     *
     * @param pos               starting position in the stored AlgART array (not subarray).
     * @param destArray         the target Java array.
     * @param destArrayOffset   starting position in the target Java array.
     * @param count             the number of elements to be replaced.
     * @param truncateOverflows whether the results should be truncated in a case of overflow.
     */
    abstract void absDiffData(
            long pos, Object destArray, int destArrayOffset, int count,
            boolean truncateOverflows);

    /**
     * This method is always called at least once before first using this object.
     * The method must not store any direct or soft references to the passed array,
     * but may store some weak reference.
     *
     * @param a the new array using this storage.
     */
    void attachArray(Array a) {
    }

    /**
     * If overridden, must forget any information the AlgART array
     * and, if it is the last stored array, dispose all data.
     * The argument must be the result of <code>System.identityHashCode(a)</code>
     * for the AlgART array using this storage.
     * Called only while AlgART array finalization and copying-on-next-write.
     *
     * @param arrayIdentityHashCode the array that is currently using this object.
     */
    void forgetArray(int arrayIdentityHashCode) {
    }

    /**
     * If overridden, should actualize elements <code>#fromIndex..#toIndex-1</code>.
     * Some forms of data storages do not initialize some elements immediately after creating the storage,
     * but the first access fills them by some standard value (usually by zero).
     * This method guarantees that all elements <code>#fromIndex..#toIndex-1</code> will be really initialized,
     * if it is necessary.
     *
     * @param context   the context of calculations; if not {@code null}, used for showing progress and interruption.
     * @param fromIndex start index (inclusive) in the stored AlgART array (not subarray).
     * @param toIndex   end index (exclusive) in the stored AlgART array (not subarray).
     */
    void actualizeLazyFilling(ArrayContext context, long fromIndex, long toIndex) {
    }

    /**
     * If overridden, should try to preload the resources connected with the specified region
     * of this storage into RAM. It is possible that not all resources will be preloaded.
     *
     * @param fromIndex start index (inclusive) in the stored AlgART array (not subarray).
     * @param toIndex   end index (exclusive) in the stored AlgART array (not subarray).
     */
    void loadResources(long fromIndex, long toIndex) {
    }

    /**
     * If overridden, should flush to disk all resources, corresponding to the specified region on this storage.
     * Maybe, flushs some additional resources.
     * Maybe, does not fill the rest of array by zeroes; to do this, you need to call
     * {@link #actualizeLazyFilling(ArrayContext, long, long)} method before.
     *
     * @param fromIndex            start index (inclusive) in the stored AlgART array (not subarray).
     * @param toIndex              end index (exclusive) in the stored AlgART array (not subarray).
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device.
     */
    void flushResources(long fromIndex, long toIndex, boolean forcePhysicalWriting) {
    }

    /**
     * If overridden, should free the resources connected with <code>a</code>.
     * In current implementation, ignores <code>a</code> and always frees all resources.
     *
     * @param a                    the array that is currently using this object.
     * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
     *                             to the external device.
     */
    void freeResources(Array a, boolean forcePhysicalWriting) {
    }

    /**
     * If overridden, must try to dispose all data.
     * Current implementation of {@link LargeMemoryModel} does not use this method.
     */
    void dispose() {
    }
}
