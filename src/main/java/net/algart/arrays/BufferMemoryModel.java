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

import net.algart.arrays.BufferArraysImpl.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>The memory model, based on <tt>ByteBuffer</tt> and other buffers from
 * <noindex>
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/nio/package-summary.html"><tt>java.nio</tt></a>
 * </noindex>
 * package.
 * It means that AlgART array of elements of some <tt><i>type</i></tt> (<tt>byte</tt>,
 * <tt>char</tt>, etc.) contains an underlying NIO buffer
 * <tt><i>Type</i>Buffer</tt> (correspondingly <tt>ByteBuffer</tt>, <tt>CharBuffer</tt>, etc.),
 * and any access to AlgART array elements is translated
 * to corresponding access to the underlying NIO buffer.
 * The only exception is {@link BitArray bit arrays},
 * where the bits are packed into elements of <tt>LongBuffer</tt>,
 * as specified in {@link PackedBitBuffers} class.</p>
 *
 * <p>Unlike the {@link SimpleMemoryModel simple memory model}, this model supports
 * primitive element types only:
 * {@link BitArray}, {@link CharArray}, {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray}, {@link FloatArray}, {@link DoubleArray}.
 * If you need to store objects in NIO buffers, you may use the AlgART arrays,
 * created by this memory model, as a storage for combined arrays,
 * created by {@link CombinedMemoryModel}.</p>
 *
 * <p>The maximal theoretical limit for length and capacity of AlgART arrays,
 * supported by this memory model, is <tt>2<sup>34</sup>-64</tt> for bit arrays and
 * <tt>2<sup>31</sup>/<i>size</i>-1</tt> for all other element types,
 * where <tt><i>size</i></tt> is <tt>1,2,2,4,8,4,8</tt> for element types
 * <tt>byte</tt>, <tt>char</tt>, <tt>short</tt>, <tt>int</tt>, <tt>long</tt>,
 * <tt>float</tt>, <tt>double</tt> correspondingly.
 * In other words, the capacity of AlgART array cannot exceed 2 GB.
 * The real limit is little less in 32-bit JVM, usually ~1.0-1.5 GB.</p>
 *
 * <p>The NIO buffers, used by this memory model, are always
 * <noindex>
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/nio/ByteBuffer.html#direct"><i>direct</i></a>:
 * </noindex>
 * they are created by <tt>ByteBuffer.allocateDirect</tt> method with, for non-byte arrays,
 * the following <tt>byteBuffer.as<i>Type</i>Buffer()</tt> call.
 * The byte order in the buffers is always
 * <noindex>
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/nio/ByteOrder.html#nativeOrder()"><i>native</i></a>,
 * </noindex>
 * excepting the case when AlgART arrays are created via {@link #asUpdatableArray(ByteBuffer, Class)}
 * method, when the byte order of the passed buffer is preserver.</p>
 *
 * <p>Arrays, created by this memory model, never implement {@link DirectAccessible} interface.
 * However, if is absolutely necessary to provide maximal performance for this memory model,
 * you may use {@link #getByteBuffer(Array)} method to
 * get a reference to the internal underlying <tt>ByteBuffer</tt>.</p>
 *
 * <p>All arrays, created by this memory model, have empty implementation of
 * {@link Array#loadResources(ArrayContext)},
 * {@link Array#flushResources(ArrayContext)}, {@link Array#flushResources(ArrayContext, boolean)} and
 * {@link Array#freeResources(ArrayContext, boolean)} methods:
 * these methods do nothing.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of its instance returned by {@link #getInstance()} method.
 * Moreover, it is a <b>singleton</b>: {@link #getInstance()} always returns the same object.</p>
 *
 * @author Daniel Alievsky
 */
public class BufferMemoryModel extends AbstractMemoryModel {
    private static final BufferMemoryModel INSTANCE = new BufferMemoryModel();

    private BufferMemoryModel() {
    }

    /**
     * Returns an instance of this memory model. This method always returns the same object.
     *
     * @return an instance of this memory model.
     */
    public static BufferMemoryModel getInstance() {
        return INSTANCE;
    }

    public MutableArray newEmptyArray(Class<?> elementType) {
        return newEmptyArray(elementType, 10);
    }

    public MutableArray newEmptyArray(Class<?> elementType, long initialCapacity) {
        return newArray(elementType, initialCapacity, 0);
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        return newArray(elementType, initialLength, initialLength);
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0)
            throw new IllegalArgumentException("Negative array length");
        DataStorage storage;
        //[[Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (elementType == boolean.class) {
            storage = new DirectDataStorages.DirectBitStorage();
            UpdatableBufferBitArray result = new UpdatableBufferBitArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == char.class) {
            storage = new DirectDataStorages.DirectCharStorage();
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == byte.class) {
            storage = new DirectDataStorages.DirectByteStorage();
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == short.class) {
            storage = new DirectDataStorages.DirectShortStorage();
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == int.class) {
            storage = new DirectDataStorages.DirectIntStorage();
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == long.class) {
            storage = new DirectDataStorages.DirectLongStorage();
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == float.class) {
            storage = new DirectDataStorages.DirectFloatStorage();
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == double.class) {
            storage = new DirectDataStorages.DirectDoubleStorage();
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else //[[Repeat.AutoGeneratedEnd]]
            throw new UnsupportedElementTypeException(
                "Only primitive element types are allowed in BufferMemoryModel (passed type: " + elementType + ")");
    }

    public boolean isElementTypeSupported(Class<?> elementType) {
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        return elementType.isPrimitive();
    }

    public boolean areAllPrimitiveElementTypesSupported() {
        return true;
    }

    public boolean areAllElementTypesSupported() {
        return false;
    }

    /**
     * This implementation returns <tt>2<sup>34</sup>-64</tt> for <tt>boolean.class</tt>
     * or <tt>2<sup>31</sup>/<i>size</i>-1</tt> for all other element types,
     * where <tt><i>size</i></tt> is <tt>1,2,2,4,8,4,8</tt> for element types
     * <tt>byte</tt>, <tt>char</tt>, <tt>short</tt>, <tt>int</tt>, <tt>long</tt>,
     * <tt>float</tt>, <tt>double</tt> correspondingly.
     *
     * @param elementType the type of array elements.
     * @return            maximal possible length of arrays supported by this memory model.
     * @throws NullPointerException if <tt>elementType</tt> is <tt>null</tt>.
     */
    public long maxSupportedLength(Class<?> elementType) {
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        if (elementType == boolean.class)
            return ((long)Integer.MAX_VALUE - 7) << 3;
        //[[Repeat() char         ==> byte,,short,,int,,long,,float,,double;;
        //           CHAR(?=_LOG) ==> BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
        //           \s*>>\s*DataStorage\.BYTES_PER_BYTE_LOG ==> ,,... ]]
        if (elementType == char.class) {
            return Integer.MAX_VALUE >> DataStorage.BYTES_PER_CHAR_LOG;
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == byte.class) {
            return Integer.MAX_VALUE;
        } else
        if (elementType == short.class) {
            return Integer.MAX_VALUE >> DataStorage.BYTES_PER_SHORT_LOG;
        } else
        if (elementType == int.class) {
            return Integer.MAX_VALUE >> DataStorage.BYTES_PER_INT_LOG;
        } else
        if (elementType == long.class) {
            return Integer.MAX_VALUE >> DataStorage.BYTES_PER_LONG_LOG;
        } else
        if (elementType == float.class) {
            return Integer.MAX_VALUE >> DataStorage.BYTES_PER_FLOAT_LOG;
        } else
        if (elementType == double.class) {
            return Integer.MAX_VALUE >> DataStorage.BYTES_PER_DOUBLE_LOG;
        } else //[[Repeat.AutoGeneratedEnd]]
        {
            return -1;
        }
    }

    public boolean isCreatedBy(Array array) {
        return isBufferArray(array);
    }

    /**
     * Returns <tt>true</tt> if the passed array was created by some instance of this memory model.
     * Returns <tt>false</tt> if the passed array is <tt>null</tt> or an AlgART array created by another memory model.
     *
     * <p>As this memory model is a singleton, this method is equivalent to {@link #isCreatedBy(Array)}.
     *
     * @param array the checked array.
     * @return      <tt>true</tt> if this array is an array created by the buffer memory model.
     */
    public static boolean isBufferArray(Array array) {
        return array instanceof AbstractBufferArray
            && ((AbstractBufferArray)array).storage instanceof DirectDataStorages.DirectStorage;
    }

    /**
     * Returns the underlying <tt>ByteBuffer</tt> instance where all elements of the passed array
     * are stored. More precisely, if the backed <tt>ByteBuffer</tt> is <tt>bb</tt>, this method returns
     * <pre>
     * bufferArray.{@link Array#isImmutable()
     * isImmutable()} ? bb.asReadOnlyBuffer().order(o) : bb.duplicate().order(o);
     * </pre>
     * where <tt>o</tt> is the byte order of the original <tt>bb</tt>, which is usually the native order.
     * So, the returned buffer does not allow to violate immutability of the passed array,
     * and in any case you may freely change the position and limit in the returned buffer.
     *
     * <p>The returned object is <tt>ByteBuffer</tt>, even if the AlgART array
     * consists of elements of another primitive type: for example, <tt>int</tt> or <tt>float</tt>.
     * The buffer, which elements really correspond to the elements of the passed array,
     * may be got by the following operators (where <tt>byteBuffer</tt> is a result of this method):
     * <ul>
     * <li>just this <tt>byteBuffer</tt> if the passed array is a {@link ByteArray};</li>
     * <li><tt>byteBuffer.asCharBuffer()</tt> if the passed array is a {@link CharArray};</li>
     * <li><tt>byteBuffer.asShortBuffer()</tt> if the passed array is a {@link ShortArray};</li>
     * <li><tt>byteBuffer.asIntBuffer()</tt> if the passed array is a {@link IntArray};</li>
     * <li><tt>byteBuffer.asLongBuffer()</tt> if the passed array is a {@link LongArray};</li>
     * <li><tt>byteBuffer.asFloatBuffer()</tt> if the passed array is a {@link FloatArray};</li>
     * <li><tt>byteBuffer.asDoubleBuffer()</tt> if the passed array is a {@link DoubleArray};</li>
     * <li><tt>byteBuffer.asLongBuffer()</tt> if the passed array is a {@link BitArray};
     * in this case, the bits are packed into <tt>long</tt> values as specified in
     * {@link PackedBitBuffers} class.</li>
     * </ul>
     *
     * <p>The <tt>ByteBuffer</tt> and the specific buffer, returned by the conversion operators,
     * listed above, may contain extra elements besides the content of the passed AlgART array.
     * Really, the elements of the AlgART array correspond to elements
     * <tt>#offset..#offset+bufferArray.{@link Array#length()}-1</tt>
     * of the buffer returned by the conversion operator (or the <tt>ByteBuffer</tt> in a case of
     * a byte array), where <tt>offset</tt> is
     * <tt>{@link #getBufferOffset(Array) getBufferOffset}(bufferArray)</tt>.
     * Changes to this range of the returned buffer "write through" to the AlgART array.
     * For bit arrays, this range is specified in terms of {@link PackedBitBuffers} class;
     * so, all elements of the AlgART array may be get and set via
     * {@link PackedBitBuffers#getBit(java.nio.LongBuffer, long) getBit(src,offset+k)} and
     * {@link PackedBitBuffers#setBit(java.nio.LongBuffer, long, boolean) setBit(dest,offset+k,value)} methods
     * of that class, where <tt>k=0,1,...,bufferArray.{@link Array#length() length()}-1</tt>.
     *
     * <p>If modifications of this AlgART array characteristics lead to reallocation
     * of the internal storage, then the returned <tt>ByteBuffer</tt> ceases to be a view of this array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this array in a case when
     * this array is {@link Array#asCopyOnNextWrite() copy-on-next-write}.
     *
     * <p>The passed argument must be created by the buffer memory model: {@link #isBufferArray(Array)}
     * must return <tt>true</tt>. In other case, <tt>IllegalArgumentException</tt> will be thrown.
     *
     * @param bufferArray the AlgART array created by this memory model.
     * @return            the underlying <tt>ByteBuffer</tt>.
     * @throws NullPointerException     if <tt>bufferArray</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>bufferArray</tt> is not created by the buffer memory model.
     */
    public static ByteBuffer getByteBuffer(Array bufferArray) {
        if (bufferArray == null)
            throw new NullPointerException("Null bufferArray argument");
        if (!(isBufferArray(bufferArray)))
            throw new IllegalArgumentException("The passed argument is not a buffer array");
        ByteBuffer bb = ((DirectDataStorages.DirectStorage)((AbstractBufferArray)bufferArray).storage).bb;
        ByteOrder o = bb.order();
        return bufferArray.isImmutable() ? bb.asReadOnlyBuffer().order(o) : bb.duplicate().order(o);
    }

    /**
     * Returns the start offset in the buffer returned by {@link #getByteBuffer(Array)} call,
     * corresponding to the first element of the passed AlgART array.
     * The returned offset is specified in terms of array elements (bits, bytes, shorts, etc.)
     *
     * @param bufferArray the AlgART array created by this memory model.
     * @return            the start offset in the buffer returned returned by {@link #getByteBuffer(Array)} call.
     * @throws NullPointerException     if <tt>bufferArray</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>bufferArray</tt> is not created by the buffer memory model.
     */
    public static long getBufferOffset(Array bufferArray) {
        if (bufferArray == null)
            throw new NullPointerException("Null bufferArray argument");
        if (!(isBufferArray(bufferArray)))
            throw new IllegalArgumentException("The passed argument is not a buffer array");
        return ((AbstractBufferArray)bufferArray).offset;
    }

    /**
     * Returns an unresizable AlgART array backed by a duplicate of the specified <tt>ByteBuffer</tt>
     * (more precisely, <tt>byteBuffer.duplicate().order(byteBuffer.order())</tt>),
     * excluding a case of <tt>boolean</tt> element type.
     *
     * <p>If <tt>elementType</tt> is not <tt>byte.class</tt>,
     * the passed byte buffer is automatically converted to a buffer of corresponding type
     * (<tt>CharBuffer</tt>, <tt>ShortBuffer</tt>, etc.)
     * via <tt>((ByteBuffer)byteBuffer.duplicate().order(byteBuffer.order()).rewind()).as<i>Type</i>Buffer()</tt> call,
     * where <tt><i>Type</i></tt> is the passed element type.
     * The result AlgART array contains all elements of the converted NIO buffer,
     * and changes in the returned array "write through" to <tt>byteBuffer</tt> argument.
     * The length and capacity of the returned array are equal to <tt>byteBuffer.limit()/<i>size</i></tt>,
     * where <tt><i>size</i></tt> is <tt>1,2,2,4,8,4,8</tt> for element types
     * <tt>byte</tt>, <tt>char</tt>, <tt>short</tt>, <tt>int</tt>, <tt>long</tt>,
     * <tt>float</tt>, <tt>double</tt> correspondingly.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @param elementType the required primitive element type.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException     if <tt>byteBuffer</tt> or <tt>elementType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>elementType</tt> is <tt>boolean.class</tt> or non-primitive type.
     */
    public static UpdatableArray asUpdatableArray(ByteBuffer byteBuffer, Class<?> elementType) {
        if (byteBuffer == null)
            throw new NullPointerException("Null byteBuffer argument");
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        if (elementType == boolean.class)
            throw new IllegalArgumentException("asUpdatableArray cannot be called for boolean.class");
        ByteBuffer bb = byteBuffer.duplicate();
        bb.order(byteBuffer.order());
        bb.rewind(); // necessary for correct creation of non-byte buffers via asXxxBuffer
        //[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
        //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double;;
        //           CHAR ==> BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
        //           \s*>>\s*DataStorage\.BYTES_PER_BYTE_LOG ==> ,,... ]]
        if (elementType == char.class) {
            DataStorage storage = new DirectDataStorages.DirectCharStorage(bb);
            int length = bb.limit() >> DataStorage.BYTES_PER_CHAR_LOG;
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == byte.class) {
            DataStorage storage = new DirectDataStorages.DirectByteStorage(bb);
            int length = bb.limit();
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == short.class) {
            DataStorage storage = new DirectDataStorages.DirectShortStorage(bb);
            int length = bb.limit() >> DataStorage.BYTES_PER_SHORT_LOG;
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == int.class) {
            DataStorage storage = new DirectDataStorages.DirectIntStorage(bb);
            int length = bb.limit() >> DataStorage.BYTES_PER_INT_LOG;
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == long.class) {
            DataStorage storage = new DirectDataStorages.DirectLongStorage(bb);
            int length = bb.limit() >> DataStorage.BYTES_PER_LONG_LOG;
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == float.class) {
            DataStorage storage = new DirectDataStorages.DirectFloatStorage(bb);
            int length = bb.limit() >> DataStorage.BYTES_PER_FLOAT_LOG;
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == double.class) {
            DataStorage storage = new DirectDataStorages.DirectDoubleStorage(bb);
            int length = bb.limit() >> DataStorage.BYTES_PER_DOUBLE_LOG;
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(storage, length, length, 0L, false);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else //[[Repeat.AutoGeneratedEnd]]
            throw new IllegalArgumentException(
                "Only non-boolean primitive element types are allowed in BufferMemoryModel (passed type: "
                    + elementType + ")");
    }

    /*Repeat() char ==> byte,,short,,int,,long,,float,,double;;
               Char ==> Byte,,Short,,Int,,Long,,Float,,Double
     */
    /**
     * Equivalent to <tt>(UpdatableCharArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, char.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableCharArray asUpdatableCharArray(ByteBuffer byteBuffer) {
        return (UpdatableCharArray)asUpdatableArray(byteBuffer, char.class);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    /**
     * Equivalent to <tt>(UpdatableByteArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, byte.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableByteArray asUpdatableByteArray(ByteBuffer byteBuffer) {
        return (UpdatableByteArray)asUpdatableArray(byteBuffer, byte.class);
    }

    /**
     * Equivalent to <tt>(UpdatableShortArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, short.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableShortArray asUpdatableShortArray(ByteBuffer byteBuffer) {
        return (UpdatableShortArray)asUpdatableArray(byteBuffer, short.class);
    }

    /**
     * Equivalent to <tt>(UpdatableIntArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, int.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableIntArray asUpdatableIntArray(ByteBuffer byteBuffer) {
        return (UpdatableIntArray)asUpdatableArray(byteBuffer, int.class);
    }

    /**
     * Equivalent to <tt>(UpdatableLongArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, long.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableLongArray asUpdatableLongArray(ByteBuffer byteBuffer) {
        return (UpdatableLongArray)asUpdatableArray(byteBuffer, long.class);
    }

    /**
     * Equivalent to <tt>(UpdatableFloatArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, float.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableFloatArray asUpdatableFloatArray(ByteBuffer byteBuffer) {
        return (UpdatableFloatArray)asUpdatableArray(byteBuffer, float.class);
    }

    /**
     * Equivalent to <tt>(UpdatableDoubleArray){@link #asUpdatableArray asUpdatableArray}(byteBuffer, double.class)</tt>.
     *
     * @param byteBuffer  the source NIO ByteBuffer.
     * @return            an unresizable AlgART array backed by the specified byte buffer.
     * @throws NullPointerException if <tt>byteBuffer</tt> argument is <tt>null</tt>.
     */
    public static UpdatableDoubleArray asUpdatableDoubleArray(ByteBuffer byteBuffer) {
        return (UpdatableDoubleArray)asUpdatableArray(byteBuffer, double.class);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns a brief string description of this memory model.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "Buffer memory model";
    }

    private MutableArray newArray(Class<?> elementType, long initialCapacity, long initialLength) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialLength < 0)
            throw new IllegalArgumentException("Negative initial length");
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Negative initial capacity");
        DataStorage storage;
        //[[Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        if (elementType == boolean.class) {
            storage = new DirectDataStorages.DirectBitStorage();
            MutableBufferBitArray result = new MutableBufferBitArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (elementType == char.class) {
            storage = new DirectDataStorages.DirectCharStorage();
            MutableBufferCharArray result = new MutableBufferCharArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == byte.class) {
            storage = new DirectDataStorages.DirectByteStorage();
            MutableBufferByteArray result = new MutableBufferByteArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == short.class) {
            storage = new DirectDataStorages.DirectShortStorage();
            MutableBufferShortArray result = new MutableBufferShortArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == int.class) {
            storage = new DirectDataStorages.DirectIntStorage();
            MutableBufferIntArray result = new MutableBufferIntArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == long.class) {
            storage = new DirectDataStorages.DirectLongStorage();
            MutableBufferLongArray result = new MutableBufferLongArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == float.class) {
            storage = new DirectDataStorages.DirectFloatStorage();
            MutableBufferFloatArray result = new MutableBufferFloatArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else
        if (elementType == double.class) {
            storage = new DirectDataStorages.DirectDoubleStorage();
            MutableBufferDoubleArray result = new MutableBufferDoubleArray(storage,
                initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else //[[Repeat.AutoGeneratedEnd]]
            throw new UnsupportedElementTypeException(
                "Only primitive element types are allowed in BufferMemoryModel (passed type: " + elementType + ")");
    }
}
