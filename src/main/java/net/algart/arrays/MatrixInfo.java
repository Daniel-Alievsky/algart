/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Full structural information about the {@link Matrix AlgART matrix}, consisting of elements
 * of some primitive types, in a form convenient for serialization.
 * This class contains information about the matrix element type, matrix dimensions and byte order,
 * and allows to pack this information, with special signature, into a byte array or <tt>String</tt>
 * (serialize, {@link #toBytes()} / {@link #toChars()} methods) and, vice versa, convert such byte array or <tt>String</tt> to an instance
 * of this class (deserialize, {@link #valueOf(byte[])} / {@link #valueOf(String)} methods).</p>
 *
 * <p>This tool helps to store AlgART matrices in external files.
 * The "raw" matrix data, i.e. elements of the built-in AlgART array,
 * can be stored / loaded via</p>
 *
 * <ul>
 * <li>{@link LargeMemoryModel#asArray(Object, Class, long, long, ByteOrder)},</li>
 * <li>{@link LargeMemoryModel#asUpdatableArray(Object, Class, long, long, boolean, ByteOrder)}</li>
 * </ul>
 *
 * <p>methods. However, if you want to load a matrix from a file, you need structural information
 * about it, that should be passed to these methods and to {@link Matrices#matrix(Array, long...)}.
 * This class encapsulates all this information and allows to represent it
 * in a form of Java byte array <tt>byte[]</tt> or in a usual <tt>String</tt>,
 * that can be stored in a separate file or in a prefix
 * of the given data file (before the <tt>filePosition</tt>, passed to
 * {@link LargeMemoryModel#asArray asArray} /
 * {@link LargeMemoryModel#asUpdatableArray asUpdatableArray} methods).
 * This class can be used together with higher-level methods</p>
 *
 * <ul>
 * <li>{@link LargeMemoryModel#asMatrix(Object, MatrixInfo)},</li>
 * <li>{@link LargeMemoryModel#asUpdatableMatrix(Object, MatrixInfo)},</li>
 * <li>{@link LargeMemoryModel#getMatrixInfoForSavingInFile(Matrix, long)}.</li>
 * </ul>
 *
 * <p>More precisely, this class represents the following information about the matrix:</p>
 *
 * <ol>
 * <li>The type of matrix elements: see {@link #elementType()} method.</li>
 * <li>The byte order used by the matrix for storing data: see {@link #byteOrder()} method.</li>
 * <li>The matrix dimensions: see {@link #dimensions()} method.</li>
 * </ol>
 *
 * <p>In addition to this information about a matrix, this class also contains the following data:</p>
 *
 * <ol start="4">
 * <li><i>Version</i>: some string determining the format of conversion of this information
 * to byte array by {@link #toBytes()} method or to <tt>String</tt> by {@link #toChars()} method.
 * The version is always stored in the serialized form returned by {@link #toBytes()} / {@link #toChars()} methods.
 * If the instance of this class was created by {@link #valueOf(Matrix, long)} method,
 * its version is equal to the current default version {@link #DEFAULT_VERSION}
 * (but you may specify some older version via {@link #valueOf(Matrix, long, String)} method).
 * If the instance was created by {@link #valueOf(byte[])} or {@link #valueOf(String)},
 * the version is equal to the version stored in this <tt>byte</tt>/<tt>char</tt> sequence.
 * Future implementations of this class will support all old serialization formats.</li>
 *
 * <li><i>Data offset</i>: some <tt>long</tt> value, that usually means the offset of
 * the matrix content in the data file. This offset is necessary for mapping the array to a disk file
 * via {@link LargeMemoryModel#asArray asArray} / {@link LargeMemoryModel#asUpdatableArray asUpdatableArray} methods
 * (it is their <tt>filePosition</tt> argument). For example, if you use this class to add
 * a fixed-size prefix to the file containg the matrix, this value may contain the prefix length.
 * You also may ignore this offset and always pass 0 (or any other value) as an argument
 * of {@link #valueOf(Matrix, long)} method.</li>
 *
 * <li>(Since the version 1.2) <i>Additional properties</i>: a map of some strings,
 * that can store some additional information about the matrix,
 * for example, about its {@link Matrix#tile() tiling}.
 * All properties are also serialized by {@link #toBytes()} / {@link #toChars()} methods,
 * deserialized by {@link #valueOf(byte[])} / {@link #valueOf(String)} methods and accessed via
 * {@link #additionalProperties()} and {@link #cloneWithOtherAdditionalProperties(Map)} methods.</li>
 * </ol>
 *
 * <p>This class does work with primitive element types only:
 * <tt>boolean</tt>, <tt>char</tt>, <tt>byte</tt>, <tt>short</tt>, <tt>int</tt>, <tt>long</tt>,
 * <tt>float</tt> and <tt>double</tt>.
 * The number of matrix dimensions ({@link Matrix#dimCount()}) must be not greater
 * than {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}.</p>
 *
 * <p>Below is an example of creating 1000x1000 matrix in a mapped file with the prefix based on this
 * class:</p>
 *
 * <pre>
 * final File file = new File("someFileName.dat");
 * {@link DataFileModel DataFileModel&lt;File&gt;} dfm = new {@link
 * DefaultDataFileModel#DefaultDataFileModel(java.io.File, long, boolean)
 * DefaultDataFileModel}(file, MatrixInfo.{@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH}, false);
 * {@link LargeMemoryModel LargeMemoryModel&lt;File&gt;} lmm = {@link LargeMemoryModel#getInstance(DataFileModel)
 * LargeMemoryModel.getInstance}(dfm);
 * Class elementType = float.class;
 * Matrix&lt;UpdatablePArray&gt; m = lmm.newMatrix(elementType, 1000, 1000);
 * MatrixInfo mi = MatrixInfo.{@link #valueOf(Matrix, long)
 * valueOf}(m, MatrixInfo.{@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH});
 * {@link UpdatableByteArray} ba = lmm.{@link LargeMemoryModel#asUpdatableByteArray
 * asUpdatableByteArray}(LargeMemoryModel.{@link LargeMemoryModel#getDataFilePath
 * getDataFilePath}(m.array()),
 * &#32;   0, MatrixInfo.{@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH}, false, ByteOrder.nativeOrder());
 * ba.{@link UpdatableArray#setData(long, Object) setData}(0, mi.toBytes());
 * LargeMemoryModel.{@link LargeMemoryModel#setTemporary setTemporary}(m.array(), false);
 * </pre>
 *
 * <p>The following code opens this file as a matrix for read-write access:</p>
 *
 * <pre>
 * final String file = new File("someFileName.dat");
 * {@link LargeMemoryModel LargeMemoryModel&lt;File&gt;} lmm = {@link LargeMemoryModel#getInstance()
 * LargeMemoryModel.getInstance}();
 * {@link ByteArray} ba = lmm.{@link LargeMemoryModel#asByteArray
 * asByteArray}(file, 0,
 * &#32;   Math.min(file.length(), MatrixInfo.{@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH}),
 * &#32;   ByteOrder.nativeOrder());
 * // "min" here is added to be on the safe side: maybe, this file was created by another program with shorter prefix
 * MatrixInfo mi = MatrixInfo.valueOf(Arrays.{@link Arrays#toJavaArray toJavaArray}(ba));
 * {@link UpdatablePArray} pa = lmm.asUpdatableArray(file,
 * &#32;   mi.{@link #elementType()}, mi.{@link #dataOffset()}, LargeMemoryModel.{@link LargeMemoryModel#ALL_FILE
 * ALL_FILE}, false, mi.{@link #byteOrder()});
 * pa = pa.{@link ByteArray#subArr
 * subArr}(0, Math.min(pa.length(), mi.{@link MatrixInfo#size() size()}));
 * // "min" here necessary because matrix info can be damaged: then subArr throws a non-informative exception
 * // mi.size() is necessary even for correct file: for example, bit arrays occupy uneven number of bytes
 * Matrix&lt;UpdatablePArray&gt; m = Matrices.matrix(pa, mi.{@link MatrixInfo#dimensions() dimensions()});
 * </pre>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @see DataFileModel#recommendedPrefixSize()
 * @since JDK 1.5
 */
public abstract class MatrixInfo {

    /**
     * The maximal allowed length of byte array or <tt>String</tt>, returned by
     * {@link #toBytes()} / {@link #toChars()} methods: {@value}.
     * This length is enough to store
     * <nobr>{@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS} =
     * {@value net.algart.arrays.Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</nobr>
     * matrix dimensions, start signature, element type, byte order and more than 7 KB of additional information.
     *
     * <p>Note that this limit guarantees that the string {@link #toChars()} can be written and restored via
     * <tt>DataOutput.writeUTF</tt> and <tt>DataInput.readUTF</tt> methods, because the required number of
     * bytes in the modified UTF-8 representation is much less than 65535 (the limit for these Java I/O methods).
     *
     * @see #toBytes()
     * @see #toChars()
     */
    public static final int MAX_SERIALIZED_MATRIX_INFO_LENGTH = 8192;

    /**
     * The maximal number of {@link #additionalProperties() additional properties}, that can be stored in this object:
     * {@value}.
     *
     * @see #cloneWithOtherAdditionalProperties(Map)
     */
    public static final int MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO = 10000;

    private static final String VERSION_1_1 = "1.1";
    private static final String VERSION_1_2 = "1.2";

    /**
     * The current default version of the serialized format of the information about matrices: "1.2".
     */
    public static String DEFAULT_VERSION = VERSION_1_2;

    private static final String ELEMENT_TYPE_PROPERTY_NAME = "__element";
    private static final String BYTE_ORDER_PROPERTY_NAME = "__order";
    private static final String SIZE_PROPERTY_NAME = "__size";
    private static final String DIMENSIONS_PROPERTY_NAME = "__dimensions";
    private static final String DATA_OFFSET_PROPERTY_NAME = "__offset";

    final Class<?> elementType;
    final long bitsPerElement;
    final ByteOrder byteOrder;
    final long size;
    final long[] dimensions;
    final long dataOffset;
    final String version;

    private MatrixInfo(Class<?> elementType, ByteOrder byteOrder, long size, long[] dimensions,
                       long dataOffset, String version)
    {
        assert dimensions.length <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS; // always checked before this
        assert elementType != null;
        assert byteOrder != null;
        this.elementType = elementType;
        this.bitsPerElement = Arrays.bitsPerElement(elementType);
        this.size = size;
        this.byteOrder = byteOrder;
        this.dimensions = dimensions;
        this.dataOffset = dataOffset;
        this.version = version;
    }

    /**
     * Creates an instance of this class containing full structural information
     * about the given matrix.
     * The created instance will have the {@link #version() version}, equal to the
     * default version supported by this class: {@link #DEFAULT_VERSION}.
     *
     * @param matrix     the matrix.
     * @param dataOffset the value that may be interpreted as an offset of some "main" data and
     *                   that will be stored in byte array returned by {@link #toBytes()} method.
     * @return full structural information about the matrix.
     * @throws NullPointerException   if the argument is <tt>null</tt>.
     * @throws TooLargeArrayException if the number of matrix dimensions is greater than
     *                                {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}.
     * @throws ClassCastException     if the built-in array in the matrix is not {@link PArray}.
     */
    public static MatrixInfo valueOf(Matrix<? extends PArray> matrix, long dataOffset) {
        return valueOf(matrix, dataOffset, DEFAULT_VERSION);
    }

    /**
     * Creates an instance of this class containing full structural information
     * about the given matrix with the specified {@link #version() version} of serialization format.
     *
     * <p>The specified version must be supported by this class.
     * Current implementation supports the following versions: "1.1" and "1.2".
     * There is no guarantee that all previous format versions, that was supported by old implementations
     * of this class, will be supported by this method.
     * (However, all previous format versions are always supported by deserialization methods
     * {@link #valueOf(byte[])} and {@link #valueOf(String)}.)
     *
     * @param matrix     the matrix.
     * @param dataOffset the value that may be interpreted as an offset of some "main" data and
     *                   that will be stored in byte array returned by {@link #toBytes()} method.
     * @param version    the version of the serialization format supported by the created instance.
     * @return full structural information about the matrix.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws TooLargeArrayException   if the number of matrix dimensions is greater than
     *                                  {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}.
     * @throws ClassCastException       if the built-in array in the matrix is not {@link PArray}.
     * @throws IllegalArgumentException if the specified version is not supported by this class.
     */
    public static MatrixInfo valueOf(Matrix<? extends PArray> matrix, long dataOffset, String version) {
        if (version == null)
            throw new NullPointerException("Null version argument");
        if (matrix.dimCount() > Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS)
            throw new TooLargeArrayException("Too large number of matrix dimensions: " + matrix.dimCount());
        PArray array = matrix.array();
        assert array.isUnresizable(); // was checked in the matrix constructor
        long[] dimensions = matrix.dimensions();
        assert dimensions.length > 0; // was checked in the matrix constructor
        assert array.length() == Arrays.longMul(dimensions); // was checked in the matrix constructor
        if (version.equals(VERSION_1_1)) {
            return new Version1_1(
                array.elementType(),
                array.byteOrder(),
                array.length(),
                dimensions,
                dataOffset);
        } else if (version.equals(VERSION_1_2)) {
            return new Version1_2(
                array.elementType(),
                array.byteOrder(),
                array.length(),
                dimensions,
                dataOffset,
                Collections.<String, String>emptyMap());
        } else {
            throw new IllegalArgumentException("The version \"" + version
                + "\" is not supported by " + MatrixInfo.class);
        }
    }

    /**
     * Deserializes the byte array and creates new instance of this class on the base of information,
     * stored in this byte array.
     *
     * <p>The passed data must have format, corresponding to the current
     * {@link #DEFAULT_VERSION default format version} or to any previous format version,
     * that was somewhen implemented by this class in the past.
     * In other case, {@link IllegalInfoSyntaxException} will be thrown.
     *
     * <p>The passed array may contain extra elements after the end of the stored information.
     * All versions of this class guarantee that random bytes after the end of the serialized information
     * will not lead to problems: the serialization format always allows to detect the end of information
     * in the sequence of bytes.
     *
     * @param bytes the byte array produced by some previous call of {@link #toBytes()} method.
     * @return new instance of this class, built on the base of the passed byte array.
     * @throws NullPointerException       if the argument is <tt>null</tt>.
     * @throws IllegalInfoSyntaxException if the format of <tt>bytes</tt> argument is illegal.
     * @see #toBytes()
     * @see #valueOf(String)
     */
    public static MatrixInfo valueOf(byte[] bytes) throws IllegalInfoSyntaxException {
        char[] chars = new char[Math.min(bytes.length, MAX_SERIALIZED_MATRIX_INFO_LENGTH)];
        for (int k = 0; k < chars.length; k++) {
            chars[k] = (char) (bytes[k] & 0xFF);
        }
        return valueOf(String.valueOf(chars));
    }

    /**
     * Deserializes the char array (passed as <tt>String</tt> argument)
     * and creates new instance of this class on the base of information,
     * stored in this char array.
     *
     * <p>The passed data must have format, corresponding to the current
     * {@link #DEFAULT_VERSION default format version} or to any previous format version,
     * that was somewhen implemented by this class in the past.
     * In other case, {@link IllegalInfoSyntaxException} will be thrown.
     *
     * <p>The passed char array may contain extra elements after the end of the stored information.
     * All versions of this class guarantee that random characters after the end of the serialized information
     * will not lead to problems: the serialization format always allows to detect the end of information
     * in the sequence of characters.
     *
     * @param chars the string produced by some previous call of {@link #toChars()} method.
     * @return new instance of this class, built on the base of the passed string.
     * @throws NullPointerException       if the argument is <tt>null</tt>.
     * @throws IllegalInfoSyntaxException if the format of <tt>chars</tt> argument is illegal.
     * @see #toChars()
     * @see #valueOf(byte[])
     */
    public static MatrixInfo valueOf(String chars) throws IllegalInfoSyntaxException {
        if (Version1_2.isVersion1_2(chars)) {
            return Version1_2.valueOf(chars);
        } else if (Version1_1.isVersion1_1(chars)) {
            return Version1_1.valueOf(chars);
        } else {
            throw new IllegalInfoSyntaxException("The char sequence does not contain valid start signature");
        }
    }

    /**
     * Returns <tt>true</tt> if and only if the passed string is an allowed name for an additional property,
     * that can be stored via {@link #cloneWithOtherAdditionalProperties(Map)} method.
     * Namely, this method returns <tt>true</tt> if and only if:
     *
     * <ul>
     * <li>the passed name is not empty (<tt>name.length()&gt;0</tt>);</li>
     * <li>the passed name contains only characters from the following set: digits '0'..'9',
     * latin letters 'a'..'z' and 'A'..'Z', the dot '.' and the underline character '_';</li>
     * <li>the first character of the name is a letter 'a'..'z' or 'A'..'Z'.</li>
     * </ul>
     *
     * <p>There is a guarantee that the keys in the map, returned by {@link #additionalProperties()} method,
     * always fulfil this condition.
     *
     * @param name the checked property name.
     * @return whether the passed string is an allowed name for an additional property.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public static boolean isCorrectAdditionalPropertyName(String name) {
        if (name == null)
            throw new NullPointerException("Null name argument");
        int len = name.length();
        if (len == 0) {
            return false;
        }
        char ch = name.charAt(0);
        if (ch == '_' || (ch >= '0' && ch <= '9')) {
            return false;
        }
        for (int j = 0; j < len; j++) {
            ch = name.charAt(j);
            if (!((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                || ch == '_' || ch == '.'))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the version of the serialization format implemented by this instance.
     *
     * <p>If this instance was created by {@link #valueOf(byte[])} or {@link #valueOf(String)} method,
     * this version corresponds to the format of the data in the argument of that method.
     * If this instance was created by {@link #valueOf(Matrix, long, String)} method,
     * this version is equal to the last argument of that method.
     * If this instance was created by {@link #valueOf(Matrix, long)} method,
     * this version is equal {@link #DEFAULT_VERSION}.
     *
     * <p>The version format is traditional: "N.N" or "N.N.N" (every "N" is a single digit 0..9).
     *
     * @return the version of the serialization of the stored data.
     */
    public final String version() {
        return this.version;
    }

    /**
     * Returns the type of matrix elements.
     *
     * @return the type of matrix elements.
     * @see Matrix#elementType()
     */
    public final Class<?> elementType() {
        return this.elementType;
    }

    /**
     * Returns the size in bits, required for each matrix element.
     * Equivalent to <tt>{@link Arrays#bitsPerElement(Class)
     * Arrays.bitsPerElement}(thisInstance.{@link #elementType()})</tt>.
     *
     * @return the size in bits, required for each matrix element.
     * @see PArray#bitsPerElement()
     */
    public long bitsPerElement() {
        return this.bitsPerElement;
    }

    /**
     * Returns the byte order used by the matrix for storing data.
     *
     * <p>This method never returns <tt>null</tt>.
     *
     * @return the byte order used by the matrix for storing data.
     * @see Array#byteOrder()
     */
    public final ByteOrder byteOrder() {
        return this.byteOrder;
    }

    /**
     * Returns the array length: total number of elements in the matrix.
     * Always equal to the product of all matrix dimensions.
     *
     * @return the array length: total number of elements in the matrix.
     * @see Matrix#size()
     * @see #dimensions()
     */
    public final long size() {
        return this.size;
    }

    /**
     * Returns the array containing all matrix dimensions.
     *
     * <p>The returned array is a clone of the internal dimension array stored in this object.
     *
     * @return the array containing all dimensions of the matrix.
     * @see Matrix#dimensions()
     */
    public final long[] dimensions() {
        return this.dimensions.clone();
    }

    /**
     * Returns the number of the matrix dimensions.
     * This value is always positive (&gt;=1).
     * Equivalent to <tt>{@link #dimensions()}.length</tt>, but works faster.
     *
     * @return the number of the matrix dimensions.
     * @see Matrix#dimCount()
     */
    public final int dimCount() {
        return this.dimensions.length;
    }

    /**
     * Returns the dimension <tt>#n</tt> of the matrix
     * or <tt>1</tt> if <tt>n&gt;={@link #dimCount()}</tt>.
     * Equivalent to <tt>n&lt;{@link #dimCount()}?{@link #dimensions()}[n]:1</tt>, but works faster.
     *
     * @param n the index of matrix dimension.
     * @return the dimension <tt>#n</tt> of the matrix.
     * @throws IndexOutOfBoundsException if <tt>n&lt;0</tt> (but <i>not</i> if <tt>n</tt> is too large).
     * @see Matrix#dim(int)
     */
    public final long dim(int n) {
        return n < this.dimensions.length ? this.dimensions[n] : 1;
    }

    /**
     * Returns the data offset, passed to {@link #valueOf(Matrix, long)} or {@link #valueOf(Matrix, long, String)}
     * method or loaded from the serialized form by {@link #valueOf(byte[])} or {@link #valueOf(String)} method.
     *
     * @return the data offset.
     */
    public final long dataOffset() {
        return this.dataOffset;
    }

    /**
     * Returns additional string properties, stored in this instance.
     * These properties can be loaded from a serialized form by {@link #valueOf(byte[])} / {@link #valueOf(String)}
     * methods or added by {@link #cloneWithOtherAdditionalProperties(Map)} method.
     *
     * <p>There is a guarantee that the keys and values, returned by this method,
     * are always non-null <tt>String</tt> instances, and that the keys
     * always fulfil the {@link #isCorrectAdditionalPropertyName(String)} method.
     *
     * <p>The returned map is a clone of the internal map stored in this object.
     * The returned map is always mutable and allows to add/remove elements, alike <tt>HashMap</tt>
     * or <tt>TreeMap</tt> classes.
     * The returned object is never <tt>null</tt>.
     *
     * <p>Note that the first version 1.1 of serialization format does not support this feature.
     * If the current {@link #version() version} of serialization format of this instance is 1.1,
     * this method always returns an empty map.
     *
     * @return additional string properties, stored in this instance.
     * @see #cloneWithOtherAdditionalProperties(Map)
     */
    public abstract Map<String, String> additionalProperties();

    /**
     * Creates an instance of this class, identical to this one, with the only
     * difference that the new instance have the specified {@link #byteOrder() byte order}.
     *
     * @param byteOrder new value of byte order.
     * @return modified instance of this class.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public abstract MatrixInfo cloneWithOtherByteOrder(ByteOrder byteOrder);

    /**
     * Creates an instance of this class, identical to this one, with the only
     * difference that the new instance have the specified {@link #version() version}
     * of the serialization format.
     *
     * <p>It is possible that the specified version does not support all features, supported by this object,
     * and cannot create fully identical instance.
     * In particular, it is possible if the specified version is 1.1 and the map of properties, returned by
     * {@link #additionalProperties()} method, is not empty.
     * In this case, this method throws <tt>UnsupportedOperationException</tt>.
     *
     * @param version required version.
     * @return modified instance of this class.
     * @throws NullPointerException          if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if the specified version is not supported by this class.
     * @throws UnsupportedOperationException if the specified version cannot store all information, stored
     *                                       in this object.
     */
    public MatrixInfo cloneWithOtherVersion(String version) {
        if (version == null)
            throw new NullPointerException("Null version argument");
        if (version.equals(VERSION_1_1)) {
            int size = additionalProperties().size();
            if (size != 0)
                throw new UnsupportedOperationException("This instance contains " + size + " additional properties, "
                    + "but the specified version " + version + " does not support this feature");
            return new Version1_1(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset);
        } else if (version.equals(VERSION_1_2)) {
            return new Version1_2(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset,
                additionalProperties());
        } else {
            throw new IllegalArgumentException("The version \"" + version
                + "\" is not supported by " + MatrixInfo.class);
        }
    }

    /**
     * Creates an instance of this class, identical to this one, with the only
     * difference that the new instance have the specified {@link #dataOffset() data offset}.
     *
     * @param dataOffset new value of data offset.
     * @return modified instance of this class.
     */
    public abstract MatrixInfo cloneWithOtherDataOffset(long dataOffset);

    /**
     * Creates an instance of this class, identical to this one, with the only
     * difference that the new instance have the specified map of named additional string properties.
     * The specified map can be serialized by {@link #toBytes()} / {@link #toChars()} methods and
     * retrieved by {@link #additionalProperties()} method.
     *
     * <p>The names of all specified properties must fulfil the {@link #isCorrectAdditionalPropertyName(String)}
     * method. In addition to this requirement, we recommend not to use (for your own goals) properties starting
     * with "<tt>net.algart.arrays.</tt>" substring, because some methods of this package use such properties.
     *
     * <p>The values of all specified properties must be non-null <tt>String</tt> instances,
     * that can contain any characters.
     *
     * <p>Please avoid very large number or size of properties. There are limits for the number of
     * properties ({@link #MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO})
     * and for the total size of the data that can be stored while serialization
     * ({@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH}).
     *
     * <p>Note that the first version 1.1 of serialization format does not support this feature.
     * So, if the current {@link #version() version} of serialization format of this instance is 1.1,
     * this method throws <tt>UnsupportedOperationException</tt>.
     * The method {@link #additionalProperties()} still works in the version 1.1 and returns an empty map.
     *
     * <p>The passed <tt>additionalProperties</tt> argument is cloned by this method: no references to it
     * are maintained by the created instance.
     *
     * @param additionalProperties another additional properties.
     * @return modified instance of this class.
     * @throws UnsupportedOperationException if the {@link #version() version} of this instance is 1.1.
     * @throws NullPointerException          if the argument is <tt>null</tt>
     *                                       or if some keys or values are <tt>null</tt>.
     * @throws ClassCastException            if some of keys or values are not <tt>String</tt> instances
     *                                       (this situation is possible while passing raw <tt>Map</tt> type
     *                                       without generics).
     * @throws IllegalArgumentException      if one of the passed properties has incorrect name
     *                                       (for which {@link #isCorrectAdditionalPropertyName(String)} method
     *                                       returns <tt>false</tt>)
     *                                       or if the number of passed properties is greater than
     *                                       {@link #MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO}.
     * @see #additionalProperties()
     */
    public abstract MatrixInfo cloneWithOtherAdditionalProperties(Map<String, String> additionalProperties);

    /**
     * Serializes the content of this instance into a byte array and returns it.
     * The format of serialization depends on the {@link #version() version} of this instance.
     *
     * <p>The length of returned array never exceeds {@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH}.
     *
     * @return the content of this instance converted into a byte array.
     * @throws IllegalStateException if the summary size of all {@link #additionalProperties() additional properties}
     *                               is very large and, so, this instance cannot be serialized in
     *                               {@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH} bytes.
     * @see #valueOf(byte[])
     * @see #toChars()
     */
    public byte[] toBytes() {
        // Note: this method must be overridden in versions where characters can be out of 0..255 range!
        String chars = toChars();
        byte[] result = new byte[chars.length()];
        for (int k = 0; k < result.length; k++) {
            char ch = chars.charAt(k);
            if (ch >= 256)
                throw new AssertionError("Cannot convert to bytes: some additional properties contain "
                    + "characters with codes higher than ASCII 255");
            result[k] = (byte) ch;
        }
        return result;
    }

    /**
     * Serializes the content of this instance into a char sequence and returns it.
     * The format of serialization depends on the {@link #version() version} of this instance.
     *
     * <p>The length of returned string never exceeds {@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH}.
     *
     * @return the content of this instance converted into a string.
     * @throws IllegalStateException if the summary size of all {@link #additionalProperties() additional properties}
     *                               is very large and, so, this instance cannot be serialized in
     *                               {@link #MAX_SERIALIZED_MATRIX_INFO_LENGTH} characters.
     * @see #valueOf(String)
     * @see #toBytes()
     */
    public abstract String toChars();


    /**
     * Indicates whether this information correctly describes the given matrix.
     * Returns <tt>true</tt> if and only if:<ol>
     * <li><tt>matrix.{@link Matrix#elementType() elementType()}.equals(this.{@link #elementType()})</tt>;</li>
     * <li><tt>matrix.{@link Matrix#array() array()}.{@link Array#byteOrder()
     * byteOrder()} == this.{@link #byteOrder()}</tt>;</li>
     * <li><tt>matrix.{@link Matrix#dimensions() dimensions()}</tt> and this {@link #dimensions()}
     * arrays are identical.</li>
     * </ol>
     *
     * @param matrix the matrix to be compared with this matrix information.
     * @return <tt>true</tt> if this information correctly describes the given matrix.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public boolean matches(Matrix<?> matrix) {
        return matrix.elementType().equals(elementType)
            && matrix.array().byteOrder() == byteOrder
            && matrix.dimEquals(dimensions);
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * a short description of all matrix dimensions.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        StringBuilder result = new StringBuilder("AlgART matrix descriptor v");
        result.append(version).append(": ");
        if (elementType == boolean.class)
            result.append("bit[");
        else if (elementType == char.class)
            result.append("char[");
        else if (elementType == byte.class)
            result.append("byte[");
        else if (elementType == short.class)
            result.append("short[");
        else if (elementType == int.class)
            result.append("int[");
        else if (elementType == long.class)
            result.append("long[");
        else if (elementType == float.class)
            result.append("float[");
        else if (elementType == double.class)
            result.append("double[");
        else
            throw new AssertionError("Illegal element type");
        result.append(dimensions[0]);
        for (int n = 1; n < dimensions.length; n++) {
            result.append('x').append(dimensions[n]);
        }
        result.append("] ").append(byteOrder);
        result.append(", dataOffset=").append(dataOffset);
        return result.toString();
    }

    /**
     * Returns the hash code of this matrix information. The result depends on
     * the {@link #elementType() element type}, {@link #byteOrder() byte order},
     * all {@link #dimensions() dimensions}, {@link #dataOffset() data offset}
     * and all {@link #additionalProperties() additional properties},
     * but does not depend on the {@link #version() version}.
     *
     * @return the hash code of this matrix information.
     */
    public int hashCode() {
        int result = elementType.toString().hashCode();
        result = 31 * result + byteOrder.toString().hashCode();
        result = 31 * result + JArrays.arrayHashCode(dimensions, 0, dimensions.length);
        result = 31 * result + ((int) dataOffset ^ (int) (dataOffset >>> 32));
        result = 31 * result + additionalProperties().hashCode();
        return result;
    }

    /**
     * Indicates whether some other matrix information is equal to this instance.
     * Returns <tt>true</tt> if and only if:<ol>
     * <li>the specified object is a non-null {@link MatrixInfo} instance;</li>
     * <li>both instances have the same {@link #elementType() element type} and
     * {@link #byteOrder() byte order};</li>
     * <li>both instances have equal {@link #dimensions() dimension arrays};</li>
     * <li>both instances have the same {@link #dataOffset() data offset};</li>
     * <li>both instances have the equal {@link #additionalProperties() additional properties maps},
     * in terms of <tt>Map.equals</tt> method.</li>
     * </ol>
     *
     * <p>Please note that this method, unlike {@link Matrix#equals(Object)},
     * compares the byte order in both objects. This method does not check
     * {@link #version() versions} of the objects.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return <tt>true</tt> if the specified object is a matrix information equal to this one.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof MatrixInfo))
            return false;
        MatrixInfo mi = (MatrixInfo) obj;
        return mi.elementType.equals(elementType)
            && mi.byteOrder == byteOrder
            && java.util.Arrays.equals(mi.dimensions, dimensions)
            && mi.dataOffset == dataOffset
            && mi.additionalProperties().equals(additionalProperties());
    }

    static final class Version1_1 extends MatrixInfo {
        private static final String SIGNATURE_BE_1_1 = "AlgART Matrix BE v" + VERSION_1_1 + "~~~";
        private static final String SIGNATURE_LE_1_1 = "AlgART Matrix LE v" + VERSION_1_1 + "~~~";
        private static final int FIXED_PART_LENGTH_1_1 = 75;

        private Version1_1(Class<?> elementType, ByteOrder byteOrder, long size, long[] dimensions, long dataOffset) {
            super(elementType, byteOrder, size, dimensions, dataOffset, VERSION_1_1);
        }

        public static boolean isVersion1_1(final String chars) {
            return chars.startsWith(SIGNATURE_BE_1_1) ||
                chars.startsWith(SIGNATURE_LE_1_1);
        }

        public static MatrixInfo valueOf(final String chars) throws IllegalInfoSyntaxException {
            final ByteOrder byteOrder;
            if (chars.startsWith(SIGNATURE_BE_1_1 + " OFFSET=")) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            } else if (chars.startsWith(SIGNATURE_LE_1_1 + " OFFSET=")) {
                byteOrder = ByteOrder.LITTLE_ENDIAN;
            } else {
                throw new IllegalInfoSyntaxException("The char sequence does not contain valid start signature");
            }
            if (chars.length() < FIXED_PART_LENGTH_1_1)
                throw new IllegalInfoSyntaxException("The char sequence is too short");
            final Class<?> elementType;
            final long dataOffset;
            final long length;
            long[] dimensions;
            try {
                dataOffset = Long.parseLong(chars.substring(32, 48), 16);
                String tn = chars.substring(48, 56);
                if (tn.equals("    bit["))
                    elementType = boolean.class;
                else if (tn.equals("   char["))
                    elementType = char.class;
                else if (tn.equals("   byte["))
                    elementType = byte.class;
                else if (tn.equals("  short["))
                    elementType = short.class;
                else if (tn.equals("    int["))
                    elementType = int.class;
                else if (tn.equals("   long["))
                    elementType = long.class;
                else if (tn.equals("  float["))
                    elementType = float.class;
                else if (tn.equals(" double["))
                    elementType = double.class;
                else
                    throw new IllegalInfoSyntaxException("The char sequence does not contain valid element type");
                length = Long.parseLong(chars.substring(56, 72), 16);
                if (!chars.substring(72, 75).equals("] ["))
                    throw new IllegalInfoSyntaxException("The char sequence does not contain \"] [\" after the length");
                int p = chars.indexOf("]~~~~", FIXED_PART_LENGTH_1_1);
                if (p == -1)
                    throw new IllegalInfoSyntaxException("The char sequence does not contain final \"]~~~~\" combination");
                dimensions = new long[Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS];
                int dimCount = 0;
                for (int q = FIXED_PART_LENGTH_1_1; q < p; q += 17) {
                    if (dimCount >= dimensions.length)
                        throw new IllegalInfoSyntaxException("The char sequence contains more than "
                            + Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + " dimensions");
                    String value = chars.substring(q, Math.min(p, q + 16));
                    dimensions[dimCount++] = Long.parseLong(value, 16);
                }
                dimensions = JArrays.copyOfRange(dimensions, 0, dimCount);
            } catch (NumberFormatException ex) {
                IllegalInfoSyntaxException e = new IllegalInfoSyntaxException(
                    "Illegal numeric format in the char sequence");
                e.initCause(ex);
                throw e;
            }
            try {
                AbstractMatrix.checkDimensions(dimensions, length);
            } catch (IllegalArgumentException ex) {
                throw new IllegalInfoSyntaxException(ex.getMessage());
            }
            return new Version1_1(elementType, byteOrder, length, dimensions, dataOffset);
        }

        @Override
        public Map<String, String> additionalProperties() {
            return new LinkedHashMap<String, String>();
        }


        @Override
        public MatrixInfo cloneWithOtherByteOrder(ByteOrder byteOrder) {
            if (byteOrder == null)
                throw new NullPointerException("Null byteOrder");
            return new Version1_1(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset);
        }

        @Override
        public MatrixInfo cloneWithOtherDataOffset(long dataOffset) {
            return new Version1_1(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset);
        }

        @Override
        public MatrixInfo cloneWithOtherAdditionalProperties(Map<String, String> additionalProperties) {
            throw new UnsupportedOperationException("This version does not support additional properties: " + this);
        }

        @Override
        public String toChars() {
            StringBuilder sb = new StringBuilder();
            sb.append(byteOrder == ByteOrder.BIG_ENDIAN ? SIGNATURE_BE_1_1 : SIGNATURE_LE_1_1);
            assert sb.length() == 24;
            sb.append(" OFFSET=").append(InternalUtils.toHexString(this.dataOffset));
            assert sb.length() == 48;
            if (elementType == boolean.class)
                sb.append("    bit[");
            else if (elementType == char.class)
                sb.append("   char[");
            else if (elementType == byte.class)
                sb.append("   byte[");
            else if (elementType == short.class)
                sb.append("  short[");
            else if (elementType == int.class)
                sb.append("    int[");
            else if (elementType == long.class)
                sb.append("   long[");
            else if (elementType == float.class)
                sb.append("  float[");
            else if (elementType == double.class)
                sb.append(" double[");
            else
                throw new AssertionError("Illegal element type");
            assert sb.length() == 56;
            sb.append(InternalUtils.toHexString(this.size)).append("] [");
            assert sb.length() == 75;
            sb.append(InternalUtils.toHexString(dimensions[0]));
            for (int k = 1; k < dimensions.length; k++)
                sb.append("x").append(InternalUtils.toHexString(dimensions[k]));
            sb.append("]~~~~");
            assert sb.length() <= MAX_SERIALIZED_MATRIX_INFO_LENGTH;
            return sb.toString();
        }
    }

    static final class Version1_2 extends MatrixInfo {
        private static final String SIGNATURE_NAME_1_2 = "__cfgtype";
        private static final String SIGNATURE_VALUE_1_2 = "AlgART Matrix v" + VERSION_1_2;
        private final Map<String, String> additionalProperties;

        private Version1_2(
            Class<?> elementType, ByteOrder byteOrder, long size, long[] dimensions,
            long dataOffset, Map<String, String> additionalProperties)
        {
            super(elementType, byteOrder, size, dimensions, dataOffset, VERSION_1_2);
            if (additionalProperties == null)
                throw new NullPointerException("Null additional properties");
            if (additionalProperties.size() > MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO)
                throw new IllegalArgumentException("Too many additional properties: "
                    + additionalProperties.size() + ">" + MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO);
            for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                if (!isCorrectAdditionalPropertyName(entry.getKey()))
                    throw new IllegalArgumentException("Illegal additional property name (empty "
                        + "or containing unallowed characters) \"" + entry.getKey() + "\"");
            }
            this.additionalProperties = additionalProperties;
        }

        public static boolean isVersion1_2(final String chars) {
            int p = chars.indexOf("\n");
            String line = p == -1 ? chars : chars.substring(0, p);
            int q = line.indexOf("=");
            if (q == -1) {
                return false;
            }
            String name = line.substring(0, q).trim();
            String value = line.substring(q + 1).trim();
            return name.equals(SIGNATURE_NAME_1_2) && value.equals(SIGNATURE_VALUE_1_2);
        }

        public static MatrixInfo valueOf(final String chars) throws IllegalInfoSyntaxException {
            Class<?> elementType = null;
            ByteOrder byteOrder = null;
            long length = -1;
            long[] dimensions = null;
            long dataOffset = -1;
            Map<String, String> additional = new LinkedHashMap<String, String>();
            int q;
            for (int p = 0, lineIndex = 0, len = chars.length(); p < len; p = q, lineIndex++) {
                if (lineIndex > MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO + 500)
                    throw new IllegalInfoSyntaxException("More than " + (MAX_NUMBER_OF_PROPERTIES_IN_MATRIX_INFO + 500)
                        + " system and additional properties are found in the serialized form");
                q = chars.indexOf("\n", p); // lines are separated by a single character '\n'
                if (++q == 0) {
                    q = len;
                }
                String line = chars.substring(p, q).trim(); // possible '\r' is also trimmed
                if (line.length() == 0) {
                    break; // an empty or space line, as well as p==len, signals about the end of properties list
                }
                if (line.startsWith("#")) {
                    continue;  // a comment: "   # some comment";
                }
                int r = line.indexOf("=");
                if (r == -1)
                    throw new IllegalInfoSyntaxException("= character expected in the line #"
                        + lineIndex + ": \"" + line + "\"");
                String name = line.substring(0, r).trim();
                String value = line.substring(r + 1).trim();
                try {
                    if (name.equals(SIGNATURE_NAME_1_2)) {
                        if (!value.equals(SIGNATURE_VALUE_1_2)) {
                            throw new IllegalInfoSyntaxException("Illegal signature in the line #"
                                + lineIndex + ": \"" + line + "\"");
                        }
                    } else if (name.equals(ELEMENT_TYPE_PROPERTY_NAME)) {
                        if (elementType != null) {
                            continue; // ignore extra properties with this name to avoid possible syntax errors
                        }
                        if (value.equals("bit")) {
                            elementType = boolean.class;
                        } else if (value.equals("char")) {
                            elementType = char.class;
                        } else if (value.equals("byte")) {
                            elementType = byte.class;
                        } else if (value.equals("short")) {
                            elementType = short.class;
                        } else if (value.equals("int")) {
                            elementType = int.class;
                        } else if (value.equals("long")) {
                            elementType = long.class;
                        } else if (value.equals("float")) {
                            elementType = float.class;
                        } else if (value.equals("double")) {
                            elementType = double.class;
                        } else {
                            throw new IllegalInfoSyntaxException("Unsupported element type in the line #"
                                + lineIndex + ": \"" + line + "\"");
                        }
                    } else if (name.equals(BYTE_ORDER_PROPERTY_NAME)) {
                        if (byteOrder != null) {
                            continue; // ignore extra properties with this name to avoid possible syntax errors
                        }
                        if (value.equals("BE")) {
                            byteOrder = ByteOrder.BIG_ENDIAN;
                        } else if (value.equals("LE")) {
                            byteOrder = ByteOrder.LITTLE_ENDIAN;
                        } else {
                            throw new IllegalInfoSyntaxException("Unsupported byte order (not \"BE\" or \"LE\") "
                                + "in the line #" + lineIndex + ": \"" + line + "\"");
                        }
                    } else if (name.equals(SIZE_PROPERTY_NAME)) {
                        if (length != -1) {
                            continue; // ignore extra properties with this name to avoid possible syntax errors
                        }
                        length = Long.parseLong(value);
                        if (length < 0) // necessary check to provide correct error message
                            throw new IllegalInfoSyntaxException("Negative array length in the line #"
                                + lineIndex + ": \"" + line + "\"");
                    } else if (name.equals(DIMENSIONS_PROPERTY_NAME)) {
                        if (dimensions != null) {
                            continue; // ignore extra properties with this name to avoid possible syntax errors
                        }
                        String[] dimValues = value.split("x", Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1);
                        if (dimValues.length > Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS)
                            throw new IllegalInfoSyntaxException("Too many matrix dimensions in the line #"
                                + lineIndex + ": \"" + line + "\" (maximal allowed number of dimensions is "
                                + Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + ")");
                        dimensions = new long[dimValues.length];
                        for (int k = 0; k < dimValues.length; k++) {
                            dimensions[k] = Long.parseLong(dimValues[k]);
                        }
                    } else if (name.equals(DATA_OFFSET_PROPERTY_NAME)) {
                        if (dataOffset != -1) {
                            continue; // ignore extra properties with this name to avoid possible syntax errors
                        }
                        dataOffset = Long.parseLong(value);
                        if (dataOffset < 0)
                            throw new IllegalInfoSyntaxException("Negative data offset in the line #"
                                + lineIndex + ": \"" + line + "\"");
                    } else {
                        if (!isCorrectAdditionalPropertyName(name))
                            throw new IllegalInfoSyntaxException("Illegal additional property name (empty "
                                + "or containing unallowed characters) in the line #"
                                + lineIndex + ": \"" + line + "\"");
                        if (additional.containsKey(name)) {
                            continue; // ignore extra properties with this name
                        }
                        String decodedValue;
                        try {
                            decodedValue = URLDecoder.decode(value, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new AssertionError("UTF-8 is not supported by URLDecoder? " + e);
                        } catch (IllegalArgumentException e) {
                            IllegalInfoSyntaxException ex = new IllegalInfoSyntaxException(
                                "URLDecoder cannot decode additional property value in the line #"
                                    + lineIndex + ": \"" + line + "\"");
                            ex.initCause(e);
                            throw ex;
                        }
                        additional.put(name, decodedValue);
                    }
                } catch (NumberFormatException e) {
                    IllegalInfoSyntaxException ex = new IllegalInfoSyntaxException(
                        "Illegal numeric format in the line #" + lineIndex + ": \"" + line + "\"");
                    ex.initCause(e);
                    throw ex;
                }
            }
            if (elementType == null)
                throw new IllegalInfoSyntaxException(ELEMENT_TYPE_PROPERTY_NAME + "=... property expected");
            if (byteOrder == null)
                throw new IllegalInfoSyntaxException(BYTE_ORDER_PROPERTY_NAME + "=... property expected");
            if (length == -1)
                throw new IllegalInfoSyntaxException(SIZE_PROPERTY_NAME + "=... property expected");
            if (dimensions == null)
                throw new IllegalInfoSyntaxException(DIMENSIONS_PROPERTY_NAME + "=... property expected");
            if (dataOffset == -1)
                throw new IllegalInfoSyntaxException(DATA_OFFSET_PROPERTY_NAME + "=... property expected");
            try {
                AbstractMatrix.checkDimensions(dimensions, length);
            } catch (IllegalArgumentException ex) {
                throw new IllegalInfoSyntaxException(ex.getMessage());
            }
            return new Version1_2(elementType, byteOrder, length, dimensions, dataOffset, additional);
        }

        @Override
        public Map<String, String> additionalProperties() {
            Map<String, String> result = new LinkedHashMap<String, String>();
            result.putAll(additionalProperties);
            return result;
        }

        @Override
        public MatrixInfo cloneWithOtherByteOrder(ByteOrder byteOrder) {
            if (byteOrder == null)
                throw new NullPointerException("Null byteOrder");
            return new Version1_2(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset,
                additionalProperties);
        }

        @Override
        public MatrixInfo cloneWithOtherDataOffset(long dataOffset) {
            return new Version1_2(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset,
                additionalProperties);
        }

        @Override
        public MatrixInfo cloneWithOtherAdditionalProperties(Map<String, String> additionalProperties) {
            if (additionalProperties == null)
                throw new NullPointerException("Null additionalProperties argument");
            Map<String, String> additional = new LinkedHashMap<String, String>();
            for (Map.Entry<String, String> e : additionalProperties.entrySet()) {
                Object key = e.getKey();
                if (key == null)
                    throw new NullPointerException("Null name of additional property");
                if (!(key instanceof String))
                    throw new ClassCastException("Illegal (not String) type of the name of additional property: "
                        + key.getClass());
                Object value = e.getValue();
                if (value == null)
                    throw new NullPointerException("Null value of additional property");
                if (!(value instanceof String))
                    throw new ClassCastException("Illegal (not String) type of the value of additional property: "
                        + value.getClass());
                additional.put((String) key, (String) value);
            }
            return new Version1_2(
                elementType,
                byteOrder,
                size,
                dimensions,
                dataOffset,
                additional);
        }

        @Override
        public String toChars() {
            StringBuilder sb = new StringBuilder();
            sb.append(SIGNATURE_NAME_1_2).append('=').append(SIGNATURE_VALUE_1_2).append('\n');
            sb.append(ELEMENT_TYPE_PROPERTY_NAME).append('=');
            if (elementType == boolean.class)
                sb.append("bit");
            else if (elementType == char.class)
                sb.append("char");
            else if (elementType == byte.class)
                sb.append("byte");
            else if (elementType == short.class)
                sb.append("short");
            else if (elementType == int.class)
                sb.append("int");
            else if (elementType == long.class)
                sb.append("long");
            else if (elementType == float.class)
                sb.append("float");
            else if (elementType == double.class)
                sb.append("double");
            else
                throw new AssertionError("Illegal element type");
            sb.append('\n');
            sb.append(BYTE_ORDER_PROPERTY_NAME).append('=').append(byteOrder == ByteOrder.BIG_ENDIAN ? "BE\n" : "LE\n");
            sb.append(SIZE_PROPERTY_NAME).append('=').append(size).append('\n');
            sb.append(DIMENSIONS_PROPERTY_NAME).append('=').append(dimensions[0]);
            for (int k = 1; k < dimensions.length; k++)
                sb.append("x").append(dimensions[k]);
            sb.append('\n');
            sb.append(DATA_OFFSET_PROPERTY_NAME).append('=').append(dataOffset).append('\n');
            for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                String name = entry.getKey();
                if (!isCorrectAdditionalPropertyName(name))
                    throw new AssertionError("Violation of invariants of " + getClass()
                        + ": illegal property name \"" + name + "\"");
                String encodedValue;
                try {
                    encodedValue = URLEncoder.encode(entry.getValue(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new AssertionError("UTF-8 is not supported by URLEncoder? " + e);
                }
                sb.append(name).append('=').append(encodedValue).append('\n');
                if (sb.length() > MAX_SERIALIZED_MATRIX_INFO_LENGTH - 1) { //-1 due to the final '\n'
                    throw new IllegalStateException("Too huge set of additional properties in " + this
                        + ": summary number of characters is greater than " + MAX_SERIALIZED_MATRIX_INFO_LENGTH);
                }
            }
            sb.append('\n');
            return sb.toString();
        }
    }
}
