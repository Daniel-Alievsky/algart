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
 * <p>Direct accessible array: an object that can be viewed as a Java array or a part of Java array.</p>
 *
 * <p>Some {@link Array AlgART arrays}, built on Java arrays internally, implement
 * this interface to provide quick access to their elements. Such arrays are called
 * "direct accessible".
 * In this library, direct accessible arrays are created by the {@link SimpleMemoryModel} class.</p>
 *
 * <p>More precisely, a <i>direct accessibly array</i> is an object,
 * for which the following conditions are fulfilled:</p>
 *
 * <ol>
 * <li>it implements this {@link DirectAccessible} interface;</li>
 * <li>its {@link #hasJavaArray()} method returns <tt>true</tt>.</li>
 * </ol>
 *
 * <p>If you need quick access to some AlgART <tt>array</tt>, it's a good idea to create
 * a special branch it the algorithm:</p>
 *
 * <pre>
 * if (array instanceof DirectAccessible &amp;&amp; ((DirectAccessible) array).{@link #hasJavaArray()}) {
 * &#32;   DirectAccessible da = (DirectAccessible) array;
 * &#32;   <i>elements_type</i>[] arr = (<i>elements_type</i>[]) da.{@link #javaArray()};
 * &#32;       // here "<i>elements_type</i>" is "byte", "short", "Object", "String", etc.:
 * &#32;       // it should be the result of array.{@link Array#elementType() elementType()}
 * &#32;   int ofs = da.{@link #javaArrayOffset()};
 * &#32;   int len = da.{@link #javaArrayLength()};
 * &#32;   // ... (access to elements arr[ofs]..arr[ofs+len-1])
 * } else {
 * &#32;   // ... (access to elements via array.getXxx/setXxx methods)
 * }
 * </pre>
 *
 * <p>In some situations, the effect of such optimization is not very strong.
 * For example, if the algorithm usually processes the AlgART array by large sequential blocks,
 * loaded via {@link Array#getData(long, Object)} method, the optimization will be connected
 * only with avoiding extra copying data in memory, and usually will not exceed 5-10%.
 * In these cases, if you don't need to change the array elements, you should check,
 * in addition, whether the array is {@link Array#isCopyOnNextWrite() copy-on-next-write}:</p>
 *
 * <pre>
 * if (array instanceof DirectAccessible &amp;&amp; !array.{@link Array#isCopyOnNextWrite()
 * isCopyOnNextWrite()} &amp;&amp; ((DirectAccessible) array).{@link #hasJavaArray()}) {
 * &#32;   DirectAccessible da = (DirectAccessible) array;
 * &#32;   <i>elements_type</i>[] arr = (<i>elements_type</i>[]) da.{@link #javaArray()};
 * &#32;   int ofs = da.{@link #javaArrayOffset()};
 * &#32;   int len = da.{@link #javaArrayLength()};
 * &#32;   // ... (access to elements arr[ofs]..arr[ofs+len-1])
 * } else {
 * &#32;   // ... (access to elements via array.get/set methods)
 * }
 * </pre>
 *
 * <p>Without the additional check, the call of {@link #javaArray()} method will lead to cloning all
 * internal storage, that can require a lot of memory and time almost without the benefit.</p>
 *
 * <p>In other situations, the advantage of this optimization can be great, up to 10&ndash;100 times
 * &mdash; usually in algorithms, which need random access to array elements, alike sorting algorithms,
 * Fourier transform, scanning boundaries of particles on images, etc. In such cases, there is no reasons
 * to check the {@link Array#isCopyOnNextWrite() copy-on-next-write} flag.</p>
 *
 * <p><i>Warning</i>: immutable arrays, created by {@link Array#asImmutable() asImmutable()} method,
 * never can be directly accessed via this interface:
 * they either do not implement this interface, or
 * their {@link #hasJavaArray()} method returns <tt>false</tt>.
 * However, if you trust the method processing an AlgART array,
 * you can use {@link Array#asTrustedImmutable() asTrustedImmutable()} method: "trusted" immutable
 * arrays can implement this interface.</p>
 *
 * <p><i>Warning</i>: if an AlgART array implements <tt>DirectAccessible</tt> interface,
 * it still can be impossible to create its array-view via {@link #javaArray()} method:
 * you must also call {@link #hasJavaArray()} to be ensured that array-view is really possible.</p>
 *
 * <p><i>Note</i>: {@link BitArray bit arrays} in this package never implement {@link DirectAccessible} interface.
 * According the contract, the Java array returned by {@link #javaArray()} method
 * must contain elements of the same type as the AlgART array,
 * but the bits in bit AlgART arrays are packed and cannot be viewed as <tt>boolean[]</tt> array.
 * However, you can use {@link DataBuffer data buffers} for direct block access to bit arrays.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @see DataBuffer
 * @since JDK 1.6
 */
public interface DirectAccessible {
    /**
     * Returns <tt>true</tt> if, and only if, this object (usually AlgART array)
     * is backed by an accessible Java array, that can be get by {@link #javaArray()} method.
     * If (and only if) this method returns <tt>false</tt>,
     * then {@link #javaArray()} method throws {@link NoJavaArrayException}.
     *
     * @return <tt>true</tt> if this object is mutable and it is backed by an accessible Java array.
     */
    public boolean hasJavaArray();

    /**
     * Returns an <i>array-view</i> of this object:
     * a pointer to internal Java array containing all content of this object
     * (usually - all elements in this AlgART array).
     * If there is no such an array, or access to it is disabled due to some reasons
     * (for example, this AlgART array is immutable), throws {@link NoJavaArrayException}.
     *
     * <p>Returned array can contain extra elements besides the content of this object.
     * Really, this object (usually AlgART array) corresponds to elements
     * <tt>#{@link #javaArrayOffset()}..#{@link #javaArrayOffset()}+{@link #javaArrayLength()}-1</tt>
     * of the returned array.
     * Changes to this range of the returned array "write through" to this object.
     *
     * <p>If modifications of this AlgART array characteristics lead to reallocation
     * of the internal storage, then the returned Java array ceases to be a view of this array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this array in a case when
     * this array is {@link Array#asCopyOnNextWrite() copy-on-next-write}.
     *
     * <p>It this object is an AlgART array, the type of returned Java array
     * is always one of the following:<ul>
     * <li><tt>boolean[]</tt> for {@link BitArray}
     * (this case never occurs for AlgART arrays from this package);
     * <li><tt>char[]</tt> for {@link CharArray},
     * <li><tt>byte[]</tt> for {@link ByteArray},
     * <li><tt>short[]</tt> for {@link ShortArray},
     * <li><tt>int[]</tt> for {@link IntArray},
     * <li><tt>long[]</tt> for {@link LongArray},
     * <li><tt>float[]</tt> for {@link FloatArray},
     * <li><tt>double[]</tt> for {@link DoubleArray},
     * <li><tt><i>type</i>[]</tt>, where <i>type</i> is the result of {@link Array#elementType()
     * elementType()} method, in all other cases.
     * </ul>
     *
     * @return a pointer to internal Java array containing all content of this object.
     * @throws NoJavaArrayException if this object cannot be viewed as a Java array.
     */
    public Object javaArray();

    /**
     * Returns the start offset in the Java array returned by {@link #javaArray()} call,
     * corresponding to the first element of this object.
     *
     * <p>The result is undefined if this object is not backed by an accessible Java array.
     * However, if this object is an immutable view of another mutable object <tt>a</tt>,
     * then this method returns the same result as <tt>a.{@link #javaArrayOffset()}</tt>.
     *
     * <p>Unlike {@link #javaArray()} method, and unlike <tt>java.nio.ByteBuffer.arrayOffset()</tt>,
     * this method does not throw any exceptions.
     *
     * @return the start offset in the array returned by {@link #javaArray()} call.
     */
    public int javaArrayOffset();

    /**
     * Returns the actual number of elements in the Java array returned by {@link #javaArray()} call,
     * corresponding to all elements of this object.
     * If this object is an AlgART array <tt>a</tt>, equivalent to <tt>(int)a.{@link Array#length() length()}</tt>.
     *
     * <p>The result is undefined if this object is not backed by an accessible Java array.
     * However, if this object is an immutable view of another mutable object <tt>a</tt>,
     * then this method returns the same result as <tt>a.arrayLength()</tt>.
     *
     * <p>Unlike {@link #javaArray()} method, this method does not throw any exceptions.
     *
     * @return the actual number of elements in the array returned by {@link #javaArray()} call.
     */
    public int javaArrayLength();
}
