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
 * <p>Data buffer: an interface allowing to read and write blocks
 * from / to some linear data storage, containing a sequence of elements
 * of any Java type, with maximal performance.
 * It is the recommended basic way for block accessing {@link Array AlgART arrays}.</p>
 *
 * <p>Theoretically, this technology can be used for accessing any types of data storages:
 * <code>java.nio.*</code> buffers, <code>RandomAccessFile</code>, etc.
 * But the main application area of data buffers is accessing AlgART arrays &mdash;
 * the only storage variant, for which this package offers a ready implementation.
 * Below we shall suppose that data buffers are used for accessing AlgART arrays.</p>
 *
 * <p>AlgART arrays support block read / write methods ({@link Array#getData(long, Object)},
 * {@link UpdatableArray#setData(long, Object)} and similar), that read / write some data block,
 * specified by it's position and size, into / from usual Java array.
 * However, such access way may be not most efficient.
 * For example, if an AlgART array implements {@link DirectAccessible} interface,
 * i.e. is backed by an accessible Java array, then
 * reading the fragment of this array into a separate Java array
 * ({@link Array#getData(long, Object) Array.getData} method) is not a good idea:
 * it's much more rational to work with the backing Java array directly.
 * If an AlgART array is a {@link Arrays#isNCopies(Array) constant array},
 * then reading data by {@link Array#getData(long, Object)} method
 * may be performed only once: the loaded data will be always the same.</p>
 *
 * <p>Unlike the block read / write methods, the <i>data buffer</i>
 * provides the most efficient and convenient read / write block access to any kinds of AlgART arrays.</p>
 *
 * <p>The scheme of usage is alike file mapping by <code>java.nio.channels.FileChannel</code> class,
 * with some little differences.</p>
 *
 * <p>The model of data buffers is the following.
 * Every <i>data buffer</i> is associated with a single AlgART array and consists of:</p>
 *
 * <ol type="A">
 * <li>A reference to the <i>data array</i>: a usual <i>Java</i> array, returned by {@link #data()} method.
 * This array contains all elements of some "mapped" region of the AlgART array.
 * The mapped region usually corresponds not to entire Java array, but only to its fragment
 * (see <i>actual region</i> in the section C below).
 * The type of elements of the Java array is the same as the type of AlgART array elements
 * ({@link Array#elementType()}), excepting the only case of {@link BitArray bit arrays}.
 * For bit arrays, the elements are packed into <code>long[]</code> array (64 bits per every <code>long</code>),
 * as specified in {@link PackedBitArrays} class.
 * <br>&nbsp;</li>
 *
 * <li>The <i>capacity</i>: maximal possible number of actual elements
 * in the data buffer, or, in other words, the maximal possible number of elements in a mapped region.
 * This value can be got by {@link #capacity()} method. Please note that the <i>data array</i>
 * may contain more than <i>capacity</i> elements; but the length of its <i>actual region</i>
 * (<code><i>toIndex</i>-<i>fromIndex</i></code>) never exceeds <i>capacity</i>.
 * <br>&nbsp;</li>
 *
 * <li>The <i>actual region</i> <code><i>fromIndex</i>..<i>toIndex</i></code> in the <i>data array</i>:
 * the elements of the AlgART array are always placed at the positions <code><i>fromIndex</i>..<i>toIndex</i>-1</code>
 * in this Java array. The region boundaries can be got by {@link #fromIndex()} and {@link #toIndex()} methods.
 * The number of actual elements is <code><i>count</i>=<i>toIndex</i>-<i>fromIndex</i></code>
 * and can be also got by {@link #count()} method.
 * <br>&nbsp;</li>
 *
 * <li>Current <i>mapping position</i> in the AlgART array.
 * The actual region in the Java array corresponds to the region of the same size in the AlgART array,
 * starting from current mapping position: <code><i>position</i>..<i>position</i>+<i>count</i>-1</code>.
 * This value can be got by {@link #position()} method.
 * <br>&nbsp;</li>
 *
 * <li>The <i>access mode</i>, describing possible access ways to the data:
 * see {@link DataBuffer.AccessMode}.</li>
 * </ol>
 *
 * <p>Please note that all integer values, described above &mdash; <i>capacity</i>,
 * <i>fromIndex</i>, <i>toIndex</i>, <i>count</i>, <i>position</i> &mdash;
 * are <code>long</code>, not <code>int</code> values.
 * The reason is that data buffers for bit arrays are packed into <code>long[]</code> arrays and, so,
 * can contain <code>2<sup>31</sup></code> or more bits.
 * In any case, all describied integer characteristics cannot be greater
 * than <code>2<sup>37</sup>-1</code> for bit arrays
 * and cannot be greater than <code>Integer.MAX_VALUE==2<sup>31</sup>-1</code> for all other element types.</p>
 *
 * <p>There are additional methods {@link #from()}, {@link #to()}, {@link #cnt()},
 * that return the results of full methods {@link #fromIndex()}, {@link #toIndex()}, {@link #count()},
 * cast to <code>int</code> type.
 * If the precise long values cannot be cast to <code>int</code>, because they are greater
 * than <code>Integer.MAX_VALUE</code>, these methods throw {@link DataBufferIndexOverflowException}.
 * You may use these methods to simplify your code, if the element type is not bit.  </p>
 *
 * <p>The scheme of using data buffers is the following.</p>
 *
 * <ol>
 * <li>First, you must create the data buffer (an object implementing this interface),
 * that will correspond to an AlgART array (or maybe for another kind of storage).
 * For AlgART arrays, you may use {@link Array#buffer(net.algart.arrays.DataBuffer.AccessMode, long)
 * Array.buffer(DataBuffer.AccessMode mode, long capacity)} method.
 * At this stage, you specify the <i>access mode</i> and desired <i>capacity</i>.
 * You also may use one of overloaded simplified variants of this method,
 * allowing to choose its arguments automatically, for example, {@link Array#buffer()}.
 * <br>&nbsp;<br>
 * Note: the newly created data buffer, unlike <code>java.nio.MappedByteBuffer</code>, contains no elements!
 * Now <i>count</i> is zero, the value <i>fromIndex</i>=<i>toIndex</i> is unspecified,
 * <i>mapping position</i> is zero.
 * <br>&nbsp;<br>
 * Note also: the creation of the new buffer is supposed to be very quick operation.
 * In particular, all necessary memory and other "heavy" resources are allocated later,
 * usually while the first mapping at the step 2.
 * <br>&nbsp;</li>
 *
 * <li>Then you call {@link #map(long) map(long position)} method of the created data buffer
 * for any desired <i>mapping position</i> in the AlgART array.
 * After this call, the <i>mapping position</i> becomes equal to the passed position,
 * and the number of actual elements <code><i>count</i></code> <code>(=<i>toIndex</i>-<i>fromIndex</i>)</code>
 * becomes equal to <code>min(<i>capacity</i>,<i>length</i>-<i>position</i>)</code>,
 * where <code>length</code> is the length of the AlgART array ({@link Array#length()})
 * and <code><i>capacity</i></code> is the buffer capacity specified while its creation at the step 1.
 * <br>&nbsp;<br>
 * If you don't need access all <i>capacity</i> elements (for example, the capacity, specified while
 * buffer creation, is 32 KB, but you need to read 100 elements only), you may use
 * {@link #map(long, long) map(long position, long maxCount)} method: it may work faster.
 * <br>&nbsp;<br>
 * Please compare: <code>java.nio.MappedByteBuffer</code> instance is always mapped to
 * the fixed file position, but an instance of this data buffer may be (and usually should be)
 * remapped to another positions of AlgART array by its
 * {@link #map(long) map(long position)} or
 * {@link #map(long, long) map(long position, long maxCount)} methods.
 * <br>&nbsp;</li>
 *
 * <li>Now you may access the data elements via the Java array, the reference to which
 * is returned by {@link #data()} method.
 * The actual elements will be placed in this Java array at indexes
 * <code><i>fromIndex</i>..<i>toIndex</i>-1</code>. For bits arrays, it means the indexes
 * in terms of {@link PackedBitArrays} class; so, these elements may be get and set via
 * {@link PackedBitArrays#getBit(long[], long) getBit(src,index)} and
 * {@link PackedBitArrays#setBit(long[], long, boolean) setBit(dest,index,value)} methods of that class,
 * where <code><i>fromIndex</i>&lt;=index&lt;<i>toIndex</i></code>. For other element types,
 * the actual elements may be accessed via Java operator <code>data[index]</code>,
 * <code><i>fromIndex</i>&lt;=index&lt;<i>toIndex</i></code>,
 * where <code><i>elementType</i>[]&nbsp;data</code> is the result
 * of {@link #data()} method.
 * <br>&nbsp;<br>
 * Note: you may change elements of the returned Java array, but these changes may reflect or not reflect
 * in the original data storage (in particular, the AlgART array). It depends on the nature of this storage.
 * For example, if it is backed by an accessible Java array, the changes will probably be reflected in
 * the storage immediately; if it is a {@link LargeMemoryModel large array} backed by a disk file,
 * the changes are stored in a local buffer only. See the step 4.
 * <br>&nbsp;<br>
 * Note also: for bit arrays, you must modify bits in the packed Java array
 * <i>only</i> by methods of {@link PackedBitArrays} class or by fully equivalent code, containing
 * the same synchronization while changing not all bits in some packed <code>long</code> element.
 * Without synchronization, some elements can be written incorrectly while multithreading using.
 * See comments to {@link PackedBitArrays} class for more details.
 * <br>&nbsp;<br>
 * Note also: the reference, returned by {@link #data()} method, may change after any call of
 * {@link #map(long) map} method. You should get this reference again after every its call.
 * <br>&nbsp;</li>
 *
 * <li>If you need to write the changed data into the AlgART array,
 * you must call {@link #force()} method after changing them.
 * If not all elements in the <i>actual region</i> were really changed,
 * you may use {@link #force(long, long) force(long fromIndex, long toIndex)} method: it may work faster.
 * <br>&nbsp;</li>
 *
 * <li>You may repeat steps 2-4 many times to access different <i>mapping positions</i>
 * in the same AlgART array.
 * It usually will not lead to allocating new memory and will be performed quickly.
 * To map the next region of the AlgART array, you may use {@link #mapNext()} method.
 * If the AlgART array is exhausted, the buffer becomes empty,
 * that may be checked by {@link #hasData()} method.</li>
 * </ol>
 *
 * <h2><a name="directAndIndirect"></a>Direct and indirect data buffers</h2>
 *
 * <p>The data buffers, provided by this package for AlgART arrays, are divided into 2 groups.</p>
 *
 * <p>The first group is <i>direct buffers</i>.
 * They provide maximally efficient access to AlgART arrays.
 * Such kind of buffers is returned by
 * {@link Array#buffer(net.algart.arrays.DataBuffer.AccessMode, long) Array.buffer} method, implemented
 * in this package, if and only if the following conditions are fulfilled:</p>
 *
 * <ul>
 * <li>the AlgART array is not {@link Array#asImmutable() immutable};</li>
 * <li>and it is not {@link Array#asCopyOnNextWrite() copy-on-next-write};</li>
 * <li>and:
 * <ul>
 * <li>either it implements {@link DirectAccessible} interface and its
 * {@link DirectAccessible#hasJavaArray() hasJavaArray()} method returns <code>true</code>,
 * <li>or it is a {@link BitArray bit array} created by the {@link SimpleMemoryModel simple memory model};
 * </ul>
 * <li>and the <i>access mode</i>, specified while buffer creation, is not
 * {@link DataBuffer.AccessMode#PRIVATE PRIVATE}.
 * </ul>
 *
 * <p>Direct buffers do not allocate any memory and work with maximal possible speed.
 * For these buffers, the {@link #data()} method returns a reference to the internal Java array
 * where the elements are stored. All <code>map</code> and <code>mapNext</code> methods work very quickly:
 * they just correct the <i>fromIndex</i> and <i>toIndex</i> offsets.
 * Any changes, made in the {@link #data()} array, immediately reflect in the original data storage;
 * the {@link #force()} method, in {@link DataBuffer.AccessMode#READ_WRITE READ_WRITE} access mode,
 * does nothing.</p>
 *
 * <p>The second group is <i>indirect buffers</i>. These buffers are usually not so efficient
 * as direct ones, because every mapping / writing data requires copying a Java array from / to
 * an AlgART array. The {@link Array#buffer(net.algart.arrays.DataBuffer.AccessMode, long) Array.buffer} method,
 * implemented in this package, returns an indirect buffer if one of the conditions listed above is not fulfilled,
 * in particular, if the AlgART array is created by the {@link BufferMemoryModel buffer memory model}
 * or the {@link LargeMemoryModel large memory model},
 * if it is {@link Array#asImmutable() immutable}
 * (and, so, direct access could violate immutability)
 * or it is {@link Array#asCopyOnNextWrite() copy-on-next-write}
 * (and, so, an attempt of direct access could lead to cloning all internal storage).</p>
 *
 * <p>The indirect buffer automatically allocates a Java array allowing
 * to store <i>capacity</i> elements.
 * (The memory is allocated not while the buffer creation, but while the first further mapping attempt.)
 * This array will is returned by {@link #data()} method.
 * The {@link #map(long)}, {@link #map(long, long)} and {@link #mapNext()}
 * methods load data into this Java array by {@link Array#getData(long, Object, int, int)} or,
 * for bit arrays, {@link BitArray#getBits(long, long[], long, long)} method.
 * (The {@link #map(long, boolean)}, {@link #map(long, long, boolean)} and {@link #mapNext(boolean)}
 * methods allows to skip loading data by specifying the argument <code>readData=false</code>,
 * if you really don't need the current data.)
 * The {@link #force()} method, in {@link DataBuffer.AccessMode#READ_WRITE READ_WRITE} access mode,
 * writes data back to the AlgART array
 * by {@link UpdatableArray#setData(long, Object, int, int)} or, for bit arrays,
 * {@link UpdatableBitArray#setBits(long, long[], long, long)}  method.
 * The changes, made in the {@link #data()} array, do not reflect in the original AlgART array
 * until the {@link #force()} call. Please note that the <i>fromIndex</i> offset is usually little,
 * but <i>not necessarily zero</i>: little non-zero offset may optimize copying data
 * due to better alignment.</p>
 *
 * <p>For {@link Arrays#isNCopies(Array) constant arrays}, created by
 * {@link Arrays#nBitCopies}, {@link Arrays#nCharCopies},
 * {@link Arrays#nByteCopies}, {@link Arrays#nShortCopies},
 * {@link Arrays#nIntCopies}, {@link Arrays#nLongCopies},
 * {@link Arrays#nFloatCopies}, {@link Arrays#nDoubleCopies},
 * {@link Arrays#nObjectCopies} methods,
 * the returned data buffer is also indirect, but works quickly:
 * the sequential calls of all <code>map</code> and <code>mapNext</code> methods
 * do nothing, because the elements in {@link #data()} array are always the same.</p>
 *
 * <p>For custom arrays, created by you (via custom {@link MemoryModel memory model} implemented by you,
 * or via direct implementing {@link Array} interface with its subinterfaces),
 * the {@link Array#buffer(net.algart.arrays.DataBuffer.AccessMode, long) Array.buffer} method may return direct
 * or indirect buffers: it depends on your implementation. If you extend the skeletal
 * {@link AbstractArray} class, please see comments to
 * {@link AbstractArray#buffer(net.algart.arrays.DataBuffer.AccessMode, long)} method.</p>
 *
 * <p>You may determine, whether the buffer is direct or indirect, via {@link #isDirect()} method.</p>
 *
 * <h2><a name="invariants"></a>Invariants of the data buffers</h2>
 *
 * <p>For any kind of the data buffer, there are the following guarantees:</p>
 *
 * <ul>
 * <li>the <i>mapped position</i> is always inside the range
 * <nobr><code>0 &lt;= <i>position</i> &lt;= <i>length</i></code></nobr>,
 * where <i>length</i> is the total number of elements in the data storage
 * ({@link Array#length()} in a case of AlgART arrays);
 * <br>&nbsp;</li>
 *
 * <li>the <i>mapped position</i> is <code>0</code> if
 * the buffer is newly created and <code>map</code> / <code>mapNext</code> methods were never called yet;
 * <br>&nbsp;</li>
 *
 * <li>for bit arrays <code>0&nbsp;&lt;&nbsp;<i>capacity</i>&nbsp;&lt;&nbsp;2<sup>37</sup></code>,
 * for all other element types <code>0&nbsp;&lt;&nbsp;<i>capacity</i>&nbsp;&lt;&nbsp;2<sup>31</sup></code>;
 * <br>&nbsp;</li>
 *
 * <li>for bit arrays <code>0 &lt;= <i>fromIndex</i> &lt;= <i>toIndex</i> &lt;= 64*<i>bufferDataLength</i></code>,
 * for all other element types
 * <code>0 &lt;= <i>fromIndex</i> &lt;= <i>toIndex</i> &lt;= <i>bufferDataLength</i></code>,
 * where <code><i>bufferDataLength</i></code> is the length of data array returned by {@link #data()} method.
 * <br>&nbsp;</li>
 *
 * <li><code><i>count</i> = <i>toIndex</i> - <i>fromIndex</i> =...</code>
 * <ul>
 * <li><code>...= 0</code>, if the buffer is newly created and
 * <code>map</code> / <code>mapNext</code> methods were never called yet;</li>
 * <li><code>...= min(<i>capacity</i>, <i>length</i> - <i>position</i>)</code>
 * after any call of {@link #map(long)}, {@link #map(long, boolean)}, {@link #mapNext()} or {@link #mapNext(boolean)}
 * method, where <i>length</i> is the total number of elements in the data storage
 * ({@link Array#length()} in a case of AlgART arrays),</li>
 * <li><code>...= min(<i>capacity</i>, <i>maxCount</i>, <i>length</i> - <i>position</i>)</code>
 * after any call of {@link #map(long, long)} or {@link #map(long, long, boolean)} methods,
 * where <i>maxCount</i> is the second argument of these methods.</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h2><a name="subInterface"></a>Specific subinterfaces</h2>
 *
 * <p>The {@link #data()} method, declared in this interface, returns <code>Object</code>
 * (the only existing superclass for all Java arrays), that is not too convenient.
 * But there are subinterfaces
 * {@link DataBitBuffer}, {@link DataCharBuffer},
 * {@link DataByteBuffer}, {@link DataShortBuffer},
 * {@link DataIntBuffer}, {@link DataLongBuffer},
 * {@link DataFloatBuffer}, {@link DataDoubleBuffer},
 * {@link DataObjectBuffer}, where this method is overridden and returns corresponding type of Java array:
 * <code>long[]</code> (packed bit array), <code>char[]</code>, <code>byte[]</code>, <code>short[]</code>, etc.
 * The basic {@link Array#buffer(net.algart.arrays.DataBuffer.AccessMode, long)} method (as well as its overloaded
 * simplified versions) is overridden in specific AlgART arrays ({@link BitArray}, {@link CharArray}, etc.)
 * and returns one of the listed subinterfaces.</p>
 *
 * <h2><a name="sequentialMapping"></a>Sequential mapping all AlgART array</h2>
 *
 * <p>This interface has no methods allowing to get the reference to the underlying AlgART array
 * or get its <i>length</i>. But you may check,
 * whether all elements of the AlgART array were already mapped by sequential calls
 * of {@link #mapNext()} method, by {@link #hasData()} method
 * (which is equivalent to "<code>{@link #count()}&gt;0</code>" check).
 * If {@link #mapNext()} is called when
 * <code><i>mapping position</i>&nbsp;+&nbsp;{@link #count()}&nbsp;=&nbsp;<i>length</i></code>,
 * then new mapping position becomes equal to <i>length</i> and,
 * so, the buffer size {@link #count()} becomes zero.
 * But please remember that {@link #hasData()} returns <code>false</code> also before the first mapping
 * of a newly created data buffer.</p>
 *
 * <h2><a name="usageExamples"></a>Usage examples</h2>
 *
 * <p>Below is a typical usage example:</p>
 *
 * <pre>
 * {@link ByteArray} a = ...; // some byte AlgART array
 * for ({@link DataByteBuffer} buf = a.{@link ByteArray#buffer() buffer()}.{@link #map(long)
 * map}(0); buf.{@link #hasData()}; buf.{@link #mapNext()}) {
 * &#32;   byte[] data = buf.{@link DataByteBuffer#data() data()};
 * &#32;   for (int k = buf.{@link #from()}; k &lt; buf.{@link #to()}; k++) {
 * &#32;       // ... (analyzing and/or replacing data[k])
 * &#32;   }
 * &#32;   buf.{@link #force()}; // necessary only if data were modified
 * }
 * </pre>
 *
 * <p>There is an equivalent form of this loop, little more complex, but easily
 * generalized for the case of another accessing order:</p>
 *
 * <pre>
 * {@link ByteArray} a = ...; // some byte AlgART array
 * {@link DataByteBuffer} buf = a.{@link ByteArray#buffer() buffer()};
 * for (long p = 0, n = a.length(); p &lt; n; p += buf.{@link #count()}) {
 * &#32;   buf.{@link #map(long) map}(p);
 * &#32;   byte[] data = buf.{@link DataByteBuffer#data() data()};
 * &#32;   for (int k = buf.{@link #from()}; k &lt; buf.{@link #to()}; k++) {
 * &#32;       // ... (analyzing and/or replacing data[k])
 * &#32;   }
 * &#32;   buf.{@link #force()}; // necessary only if data were modified
 * }
 * </pre>
 *
 * <p>A usage example for bits:</p>
 *
 * <pre>
 * {@link BitArray} a = ...; // some bit AlgART array
 * for ({@link DataBitBuffer} buf = a.{@link BitArray#buffer() buffer()}.{@link #map(long)
 * map}(0); buf.{@link #hasData()}; buf.{@link #mapNext()}) {
 * &#32;   long[] data = buf.{@link DataBitBuffer#data() data}();
 * &#32;   for (long k = buf.{@link #fromIndex()}; k &lt; buf.{@link #toIndex()}; k++) {
 * &#32;       boolean b = {@link PackedBitArrays#getBit(long[], long) PackedBitArrays.getBit}(data, k);
 * &#32;       // it is the element a.{@link BitArray#getBit getBit}({@link #position()}+k)
 * &#32;       // processing this bit...
 * &#32;       // then, if necessary:
 * &#32;       {@link PackedBitArrays#setBit(long[], long, boolean) PackedBitArrays.setBit}(data, k, someNewValue);
 * &#32;   }
 * &#32;   buf.{@link #force()}; // necessary only if data were modified
 * }
 * </pre>
 *
 * <h2><a name="problems"></a>Performance problem connected with a lot of data buffers</h2>
 *
 * <p>There is a problem connected with creating and using a lot of <i>indirect</i> data buffers
 * (thousands and tens of thousands). In this situation, every data buffer allocates its own
 * Java array for storing data, that cannot be shared with other data buffers.
 * Allocating thousands of Java array, where every array occupies tens of kilobytes or more
 * (typical capacity for data buffers), may require much time and create great workload for the garbage collector.
 * As a result, it may lead to reducing overall performance.
 * (Of course, the direct buffers do not lead to such a problem.)</p>
 *
 * <p>The same problem occurs with usual Java arrays, used for temporary buffers instead of this class.
 * But {@link JArrayPool} class offers a solution for this problem for Java arrays.
 * So, if allocation of the data buffer may really create the described
 * performance problem, you should check the newly created buffer, {@link #isDirect() is it direct},
 * and, if not, ignore it and use standard Java arrays with help of {@link JArrayPool}.</p>
 *
 * <h2><a name="notes"></a>Additional notes</h2>
 *
 * <p>The data buffers are allowed not to implement <code>hashCode</code> and <code>equals</code> method.
 * Usually, the default implementation of these methods from <code>Object</code> class are used.</p>
 *
 * <p>The data buffers are not thread-safe, but <b>are thread-compatible</b>
 * and can be synchronized manually (together with AlgART arrays accessed via the buffers)
 * if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public interface DataBuffer {

    /**
     * Access mode, describing access to {@link DataBuffer data buffers}.
     * There are 3 possible modes: {@link #READ}, {@link #READ_WRITE} and {@link #PRIVATE}.
     * See comments to these constants.
     *
     * @see Array#buffer(net.algart.arrays.DataBuffer.AccessMode, long)
     */
    enum AccessMode {
        /**
         * This access mode should be used if you need read-only access to the data.
         * For AlgART arrays, it is the only allowed mode if the array does not implement
         * {@link UpdatableArray} interface (so, it is probably {@link Array#asImmutable() immutable}
         * or {@link Array#asTrustedImmutable() trusted immutable}).
         *
         * <p>Please note: there are <i>no guarantees</i> that the data buffer, created
         * with this access mode, will not allow to change the data in the underlying storage.
         * For example, in <i><a href="DataBuffer.html#directAndIndirect">direct</a></i> data buffer
         * all changes in the {@link DataBuffer#data() data array}
         * reflect in the original storage immediately even in this access mode.
         *
         * <p>In this access mode, {@link DataBuffer#force()} and {@link DataBuffer#force(long, long)} methods
         * throw an exception.
         */
        READ,

        /**
         * This access mode should be used if you need write access to data.
         * It is the most common access mode, but it cannot be used with AlgART arrays
         * that do not implement {@link UpdatableArray} interface (in particular,
         * with {@link Array#asImmutable() immutable} or {@link Array#asTrustedImmutable()
         * trusted immutable} arrays).
         */
        READ_WRITE,

        /**
         * This mode works almost alike {@link #READ_WRITE}, but any changes in the data array
         * are lost. More precisely, there are two differences from {@link #READ_WRITE} mode:
         * <ol>
         * <li>any changes the {@link DataBuffer#data() data array} never reflect in the original storage
         * (so, the data buffers, created with this mode, are never
         * <i><a href="DataBuffer.html#directAndIndirect">direct</a></i>);</li>
         * <li>{@link DataBuffer#force()} and {@link DataBuffer#force(long, long)} methods
         * do nothing.</li>
         * </ol>
         *
         * <p>In other words, this mode (unlike {@link #READ}) guarantees that the data in
         * the underlying storage will not be changed, and any changes in the
         * <i>{@link DataBuffer#data() data array}</i> will be lost.
         * This mode can be convenient if you do not really need writing access to the data,
         * but want to have a right to safely change the content of the returned data array
         * (for example, to use it as a work memory).
         */
        PRIVATE
    }

    AccessMode mode();

    /**
     * Returns the Java array which contains the mapped region of the data.
     * The actual data elements are placed at the positions <code>{@link #fromIndex()}..{@link #toIndex()}-1</code>.
     * For bit elements, returned Java array is a packed <code>long[]</code> array,
     * and the positions in this array should be considered in terms of {@link PackedBitArrays} class.
     *
     * <p>The length of the returned array is always enough to fit
     * the <code>{@link #fromIndex()}..{@link #toIndex()}-1</code> positions range in it.
     *
     * <p><i>Note:</i> this method returns {@code null} if <code>map</code> / <code>mapNext</code> methods
     * were never called for this buffer yet, that is, if it is newly created.
     *
     * <p><i>Warning:</i> the reference, returned by this method, may change after any call of
     * <code>map</code> or <code>mapNext</code> methods.
     *
     * @return the Java array which contains the mapped region of the data.
     */
    Object data();

    /**
     * Maps this data buffer to the specified position of the underlying data storage (usually AlgART array)
     * for accessing first <code>{@link #capacity()}</code> elements starting from this position.
     * The fragment of the data storage will be loaded and accessible in the {@link #data()} Java array
     * at the positions <code>{@link #fromIndex()}..{@link #toIndex()}-1</code>.
     * Equivalent to <code>{@link #map(long, long, boolean) map}({@link #position()},{@link #capacity()},true)</code>.
     *
     * <p>The passed argument must be in range <code>0..<i>length</i></code>,
     * where <code><i>length</i></code> is the total number of elements in the underlying data storage
     * (for an AlgART array, its {@link Array#length()}).
     * The number of actually mapped elements ({@link #count()}) will be equal to
     * <code>min({@link #capacity},<i>length</i>-<i>position</i>)</code>.
     *
     * @param position new mapping position.
     * @return a reference to this data buffer.
     * @throws IndexOutOfBoundsException if the specified position is out of range <code>0..<i>length</i></code>.
     * @see #mapNext()
     * @see #map(long, long)
     * @see #map(long, boolean)
     */
    DataBuffer map(long position);

    /**
     * An analog of {@link #map(long)} with the only exception, that when <code>readData=false</code>,
     * <i>reading data from the data storage is not guaranteed</i>.
     * (When <code>readData=true</code>, there is no difference with {@link #map(long)} method.)
     * Equivalent to
     * <code>{@link #map(long, long, boolean) map}({@link #position()},{@link #capacity()},readData)</code>.
     * The mode <code>readData=false</code> can be useful for optimization in {@link AccessMode#READ_WRITE} mode,
     * if you are sure that you will fully rewrite all mapped elements and, so, want
     * to save time by avoiding useless reading them.
     *
     * @param position new mapping position.
     * @param readData if <code>true</code>, all mapped elements will be really loaded from the data storage;
     *                 if <code>false</code>, there is no such a guarantee.
     * @return a reference to this data buffer.
     * @throws IndexOutOfBoundsException if the specified position is out of range <code>0..<i>length</i></code>.
     * @see #mapNext(boolean)
     * @see #map(long, long, boolean)
     */
    DataBuffer map(long position, boolean readData);

    /**
     * Maps the next region in the underlying data storage (usually AlgART array).
     * Equivalent to {@link #map(long) map}({@link #position()}&nbsp;+&nbsp;{@link #count()}).
     * In particular, if the buffer is newly created and {@link #map(long)} and <code>mapNext()</code>
     * were never called yet, this method is equivalent to <code>{@link #map(long) map}(0)</code>.
     *
     * <p>The following loop allows to sequentially map all elements of the underlying data storage:
     * <pre>
     * for (buf.{@link #map(long) map}(0); buf.{@link #hasData()}; buf.mapNext()) {
     * &#32;   // ... (processing elements of buf.{@link #data()})
     * }
     * </pre>
     *
     * @return a reference to this data buffer.
     * @see #map(long)
     * @see #mapNext(boolean)
     */
    DataBuffer mapNext();

    /**
     * An analog of {@link #mapNext()} with the only exception, that when <code>readData=false</code>,
     * <i>reading data from the data storage is not guaranteed</i>.
     * (When <code>readData=true</code>, there is no difference with {@link #mapNext()} method.)
     * Equivalent to
     * <code>{@link #map(long, boolean) map}({@link #position()}&nbsp;+&nbsp;{@link #count()},&nbsp;readData)</code>.
     * The mode <code>readData=false</code> can be useful for optimization in {@link AccessMode#READ_WRITE} mode,
     * if you are sure that you will fully rewrite all mapped elements and, so, want
     * to save time by avoiding useless reading them.
     *
     * @param readData if <code>true</code>, all mapped elements will be really loaded from the data storage;
     *                 if <code>false</code>, there is no such a guarantee.
     * @return a reference to this data buffer.
     * @see #map(long, boolean)
     */
    DataBuffer mapNext(boolean readData);

    /**
     * Equivalent to {@link #map(long, long, boolean) map(position, maxCount, true)}.
     *
     * @param position new mapping position.
     * @param maxCount this method does not guarantee that the elements after <code>#position+maxCount-1</code>
     *                 will be loaded into the buffer.
     * @return a reference to this data buffer.
     * @throws IndexOutOfBoundsException if the specified position is out of range <code>0..<i>length</i></code>.
     * @throws IllegalArgumentException  if the specified <code>maxCount</code> is negative.
     * @see #map(long)
     */
    DataBuffer map(long position, long maxCount);

    /**
     * Maps this data buffer to the specified position of the underlying data storage (usually AlgART array)
     * for accessing first <code>min(maxCount,{@link #capacity()})</code> elements starting from this position.
     * If <code>readData=true</code>, the fragment of the data storage will be loaded
     * and accessible in the {@link #data()} Java array at the positions
     * <code>{@link #fromIndex()}..{@link #toIndex()}-1</code>.
     * If <code>readData=false</code>, the behaviour is the same with the exception of
     * <i>reading data from the data storage is not guaranteed</i>.
     * This mode can be useful for optimization in {@link AccessMode#READ_WRITE} mode,
     * if you are sure that you will fully rewrite all mapped elements and, so, want
     * to save time by avoiding useless reading them.
     *
     * <p>The passed position must be in range <code>0..<i>length</i></code>,
     * where <code><i>length</i></code> is the total number of elements in the underlying data storage
     * (for an AlgART array, its {@link Array#length()}).
     * The number of actually mapped elements ({@link #count()}) will be equal to
     * <code>min(maxCount,{@link #capacity},<i>length</i>-<i>position</i>)</code>.
     *
     * <p>This method should be used instead of the full {@link #map(long) map(position)} version,
     * if you need to access a less number of elements than the full buffer capacity
     * or if you are going to fully rewrite all mapped elements.
     *
     * @param position new mapping position.
     * @param maxCount this method does not guarantee that the elements after <code>#position+maxCount-1</code>
     *                 will be loaded into the buffer.
     * @param readData if <code>true</code>, all mapped elements will be really loaded from the data storage;
     *                 if <code>false</code>, there is no such a guarantee.
     * @return a reference to this data buffer.
     * @throws IndexOutOfBoundsException if the specified position is out of range <code>0..<i>length</i></code>.
     * @throws IllegalArgumentException  if the specified <code>maxCount</code> is negative.
     * @see #map(long)
     * @see #map(long, long)
     */
    DataBuffer map(long position, long maxCount, boolean readData);

    /**
     * Writes all elements in the <i>actual region</i> of the {@link #data()} Java array
     * (from {@link #fromIndex()}, inclusive, to {@link #toIndex()}, exclusive)
     * back to the underlying data storage (usually AlgART array).
     * May do nothing if the changes in this Java array reflect in the storage immediately
     * (for example, for <a href="#directAndIndirect">direct buffers</a>).
     *
     * <p>This method must be called to ensure that all changes, performed in the mapped
     * data elements, will be reflected in the original data storage.
     *
     * <p>This method must not be called in the {@link DataBuffer.AccessMode#READ READ} access mode.
     * This method does nothing in the {@link DataBuffer.AccessMode#PRIVATE PRIVATE} access mode.
     *
     * @return a reference to this data buffer.
     * @throws IllegalStateException if the access mode is {@link DataBuffer.AccessMode#READ READ}.
     * @see #force(long, long)
     */
    DataBuffer force();

    /**
     * Writes all elements in the specified region of the {@link #data()} Java array
     * (from the passed <code>fromIndex</code>, inclusive, to the passed <code>toIndex</code>, exclusive)
     * back to the underlying data storage (usually AlgART array).
     * May do nothing if the changes in this Java array reflect in the storage immediately
     * (for example, for <a href="#directAndIndirect">direct buffers</a>).
     *
     * <p>This method may be called instead of the full {@link #force()} version,
     * if you changed only part of mapped elements (for example, only one or several elements).
     * In this case, this method may work faster.
     * The {@link #force()} method is equivalent to the call
     * <code>force({@link #fromIndex()}, {@link #toIndex()})</code>.
     *
     * <p>This method must not be called in the {@link DataBuffer.AccessMode#READ READ} access mode.
     * This method does nothing in the {@link DataBuffer.AccessMode#PRIVATE PRIVATE} access mode.
     *
     * @param fromIndex low boundary (inclusive) of the written region.
     * @param toIndex   high boundary (inclusive) of the written region.
     * @return a reference to this data buffer.
     * @throws IllegalStateException    if the access mode is {@link DataBuffer.AccessMode#READ READ}.
     * @throws IllegalArgumentException if <code>fromIndex&gt;toIndex</code> or if the passed region is not
     *                                  a fragment of full actual region
     *                                  <code>{@link #fromIndex()}..{@link #toIndex()}-1</code>.
     * @see #force()
     */
    DataBuffer force(long fromIndex, long toIndex);

    /**
     * Returns the current <i>mapping position</i>.
     * Cannot be negative.
     *
     * @return the current <i>mapping position</i>.
     */
    long position();

    /**
     * Returns the <i>capacity</i> of this data buffer.
     * Cannot be negative.
     *
     * @return the <i>capacity</i> of this data buffer.
     */
    long capacity();

    /**
     * Returns the low boundary (inclusive) of the current <i>actual region</i>.
     * Cannot be negative.
     *
     * @return the low boundary (inclusive) of the current <i>actual region</i>.
     */
    long fromIndex();

    /**
     * Returns the high boundary (exclusive) of the current <i>actual region</i>.
     * Cannot be negative.
     *
     * @return the high boundary (exclusive) of the current <i>actual region</i>.
     */
    long toIndex();

    /**
     * Returns the number of elements in the current <i>actual region</i>.
     * Equivalent to <code>{@link #toIndex()}-{@link #fromIndex()}</code>.
     * Cannot be negative.
     *
     * @return the number of elements in the current <i>actual region</i>.
     */
    long count();

    /**
     * Returns <code>true</code> if and only if <code>{@link #count()}&gt;0</code>.
     *
     * <p>After a call of {@link #map(long)} or {@link #mapNext()},
     * this method may be used to check that the current mapping position
     * is equal to the <i>length</i> of the underlying data storage.
     * For sequential calls of {@link #mapNext()}, this method returns <code>false</code>
     * when all elements from the data storage have been already mapped.
     *
     * @return <code>true</code> if this buffer is non-empty.
     */
    boolean hasData();

    /**
     * Returns <code>true</code> if this buffer is <i>direct</i>.
     *
     * <p>For buffers, created by this package, the "direct" term is quite specified:
     * please see "<a href="#directAndIndirect">Direct and indirect data buffers</a>" section above.
     *
     * <p>For any kind of buffer, "direct" term means that all <code>map</code> and <code>mapNext</code> methods,
     * {@link #force()} and {@link #force(long, long)} methods,
     * <i>probably</i>, work very quickly, and the changes in the {@link #data()} array,
     * <i>probably</i>, reflect in the original data storage immediately.
     *
     * @return <code>true</code> if this buffer is direct.
     */
    boolean isDirect();

    /**
     * Returns <code>(int){@link #fromIndex()}</code>, if <code>{@link #fromIndex()}&lt;=Integer.MAX_VALUE</code>,
     * or throws {@link DataBufferIndexOverflowException} in other case.
     * May be used if you are sure that this buffer is not a {@link DataBitBuffer bit buffer},
     * or if you are sure that the AlgART array, processed by this buffer,
     * is not longer than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return <code>(int){@link #fromIndex()}</code>.
     * @throws DataBufferIndexOverflowException
     *          if <code>{@link #fromIndex()}&gt;Integer.MAX_VALUE</code>.
     */
    int from();

    /**
     * Returns <code>(int){@link #toIndex()}</code>, if <code>{@link #toIndex()}&lt;=Integer.MAX_VALUE</code>,
     * or throws {@link DataBufferIndexOverflowException} in other case.
     * May be used if you are sure that this buffer is not a {@link DataBitBuffer bit buffer},
     * or if you are sure that the AlgART array, processed by this buffer,
     * is not longer than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return <code>(int){@link #toIndex()}</code>.
     * @throws DataBufferIndexOverflowException
     *          if <code>{@link #toIndex()}&gt;Integer.MAX_VALUE</code>.
     */
    int to();

    /**
     * Returns <code>(int){@link #count()}</code>, if <code>{@link #count()}&lt;=Integer.MAX_VALUE</code>,
     * or throws {@link DataBufferIndexOverflowException} in other case.
     * May be used if you are sure that this buffer is not a {@link DataBitBuffer bit buffer},
     * or if you are sure that the AlgART array, processed by this buffer,
     * is not longer than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return <code>(int){@link #count()}</code>.
     * @throws DataBufferIndexOverflowException
     *          if <code>{@link #count()}&gt;Integer.MAX_VALUE</code>.
     */
    int cnt();
}
