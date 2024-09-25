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

import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;

import java.util.List;
import java.util.Objects;

/**
 * <p>AlgART matrix: multidimensional array.</p>
 *
 * <p>Unlike {@link Array AlgART array}, AlgART matrix is a very simple thing.
 * The matrix is just <i>a pair</i>:</p>
 *
 * <ol>
 * <li>a reference to any AlgART array, so-called <i>built-in array</i> of the matrix,
 * that actually stores all matrix elements;</li>
 *
 * <li>the <i>set of dimensions</i>: a little usual array of integers &mdash; <code>long[] dim</code>,
 * describing the sizes of the multidimensional matrix in every dimension.</li>
 * </ol>
 *
 * <p>The product of all dimensions must be equal to the array length. Moreover,
 * the array must be {@link UpdatableArray#asUnresizable() unresizable}: so, the array length
 * cannot be changed after creating the matrix.</p>
 *
 * <p>It is supposed that all matrix elements are stored in the built-in AlgART array.
 * The storing scheme is traditional. For 2D matrix, the matrix element <code>(x,y)</code>
 * is stored at the position <code>y*dim[0]+x</code> of the array (<code>dim[0]</code> is the first
 * matrix dimension: the "width"). For 3D matrix, the matrix element <code>(x,y,z)</code>
 * is stored at the position <code>z*dim[1]*dim[0]+y*dim[0]+x</code> (<code>dim[0]</code> is the
 * x-dimension, dim[1] is the y-dimension). In the common case, the element of <code>n</code>-dimensional matrix
 * with coordinates <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i> is stored
 * in the built-in array at the position</p>
 *
 * <blockquote>
 * <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
 * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
 * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
 * </blockquote>
 *
 * <p>where <i>d<sub>k</sub></i><code>=dim[<i>k</i>]</code> (<code><i>k</i>=0,1,...,<i>n</i>-1</code>)
 * is the dimension <code>#<i>k</i></code>.</p>
 *
 * <p>There are 3 basic ways to create a new matrix.</p>
 *
 * <ol>
 * <li>You may create a new zero-filled matrix with new allocated array by
 * {@link MemoryModel#newMatrix(Class, Class, long...)}
 * method or one of more concrete methods {@link MemoryModel#newByteMatrix(long...)},
 * {@link MemoryModel#newShortMatrix(long...)}, etc.</li>
 *
 * <li>You may create a matrix view of an existing array with the specified dimension set
 * by {@link Matrices#matrix(Array, long...)} method.</li>
 *
 * <li>You may replace built-in array of the matrix with a new one (with the same length)
 * by {@link #matrix(Array)} method of the matrix instance;
 * the new matrix instance will be created.
 * It is the basic way to change some properties of the built-in array,
 * for example, to convert it to {@link Array#asImmutable() immutable}
 * or {@link Array#asCopyOnNextWrite() copy-on-next-write} form.</li>
 * </ol>
 *
 * <p>We do not provide special tools for accessing matrix elements by several indexes,
 * as "getByte(x,y)" or similar methods. But there is the {@link #index(long...) index}
 * method, that transforms a set of multidimensional indexes
 * <i>i<sub>0</sub></i>, <i>i<sub>2</sub></i>, ..., <i>i<sub>n-1</sub></i>
 * into the position in the corresponded array, as described above.
 * Also you can get a reference to the built-in array by the {@link #array()} method.
 * The typical example of access to matrix elements is the following:</p>
 *
 * <pre>
 * Matrix&lt;UpdatableFloatArray&gt; m = ...;
 * m.array().setFloat(m.index(x, y, z), myValue);
 * </pre>
 *
 * <p>There are two important notes concerning usage of matrices.</p>
 *
 * <p>First, the matrix indexes in all methods ({@link #index(long...) index},
 * {@link #dim(int) dim(n)}, <code>dim</code> argument in {@link MemoryModel#newMatrix(Class, Class, long...)
 * MemoryModel.newMatrix}, etc.) are ordered from the <i>lowest</i> index to the <i>highest</i>.
 * Please compare: for numeric matrix <code>m</code>, <code>m.array().getDouble(m.index(15,10))</code>
 * returns the element <code>#15</code> of the row <code>#10</code>. However,
 * for usual 2-dimensional Java array,
 * declared as "<code>double[][] a</code>", the same element is accessed as
 * <code>a[10][15]</code>!</p>
 *
 * <p>Second, the number of indexes in the {@link #index(long...) index} method
 * may <i>differ</i> from the number of dimensions ({@link #dimCount()}).
 * In any case, the returned position in calculated by the formula listed above
 * (<i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
 * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
 * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>),
 * where <i>i<sub>0</sub></i>, <i>i<sub>2</sub></i>, ..., <i>i<sub>n-1</sub></i>
 * are the coordinates passed to the method, and <i>d<sub>k</sub></i> is the dimension <code>#<i>k</i></code>
 * or 1 if <code><i>k</i>&gt;={@link #dimCount()}</code>.
 * In other words, it is supposed that all dimensions "after" the actual number of dimensions
 * are always equal to 1. For example, the one-dimensional matrix with <code>L</code> elements
 * can be interpreted as 2-dimensional <code>Lx1</code> matrix,
 * or 3-dimensional <code>Lx1x1</code> one, etc.</p>
 *
 * <p>The matrix object is <b>immutable</b>, that means that there are no ways to change
 * any dimension or the reference to the built-in AlgART array.
 * But the matrix elements can be modified, if the AlgART array is not
 * {@link Array#asImmutable() immutable}.
 * So, the matrix object is <b>thread-safe</b> or <b>thread-compatible</b>
 * in the same situations as the built-in AlgART array: see comments to {@link Array} interface.</p>
 *
 * <p>The generic argument <code>T</code> specifies the type of the built-in AlgART array.
 * Any array type can be declared here, but the contract of this interface
 * requires that the array must be {@link UpdatableArray#asUnresizable() unresizable}.
 * So, there are no ways to create a matrix with {@link MutableArray} (or its subinterface)
 * as the type argument, alike <code>Matrix&lt;MutableByteArray&gt;</code>:
 * all creation methods throw <code>IllegalArgumentException</code> in this case.
 *
 * @param <T> the type of the built-in AlgART array.
 * @author Daniel Alievsky
 * @see Array
 * @see UpdatableArray
 * @see MutableArray
 */
public interface Matrix<T extends Array> extends Cloneable {
    /**
     * <p>Continuation mode for submatrices, created by
     * {@link Matrix#subMatrix(long[], long[], ContinuationMode continuationMode)},
     * {@link Matrix#subMatr(long[], long[], ContinuationMode continuationMode)} and similar methods.
     * The continuation mode is passed to those methods as the last argument and specifies,
     * what will be the values of elements of the returned submatrix, which lie outside the original matrix.
     * (This argument is not important if all submatrix elements belong to the original matrix,
     * i.e. if the returned matrix is a true sub-matrix of the original one.)</p>
     *
     * <p>The following continuation modes are possible:</p>
     *
     * <ul>
     *     <li>{@link #NONE}: continuation is not allowed;</li>
     *     <li>{@link #CYCLIC}: <i>cyclic</i> repetition of the original matrix along all coordinates;</li>
     *     <li>{@link #PSEUDO_CYCLIC}: <i>pseudo-cyclic</i> (<i>toroidal</i>) repetition of the original matrix,
     *     corresponding to the cyclic repetition of its {@link Matrix#array() built-in array};
     *     most of algorithms of image processing work in accordance with this model;</li>
     *     <li>{@link #MIRROR_CYCLIC}: improved version of {@link #CYCLIC} model, where the original matrix
     *     is repeated with "mirror reflecting"; this mode provides the best smoothness of continuation;</li>
     *     <li>{@link #getConstantMode(Object) <i>constant</i> continuation}: the space outside the original matrix
     *     is considered to be filled by some constant value.</li>
     * </ul>
     *
     * <p>See comments to these modes for more details.</p>
     *
     * <p>Note: {@link #CYCLIC}, {@link #PSEUDO_CYCLIC}
     * and {@link #MIRROR_CYCLIC} modes are not applicable for matrices with zero dimensions:
     * if some matrix dimension <code>{@link Matrix#dim(int) dim(k)}==0</code>, then the corresponding
     * coordinate range of a submatrix must be <code>0..0</code>, as for {@link #NONE} continuation mode.
     * See more details in comments to {@link Matrix#subMatrix(long[], long[], ContinuationMode)} method.
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.
     * Moreover, the constants {@link #NONE}, {@link #CYCLIC}, {@link #PSEUDO_CYCLIC}, {@link #MIRROR_CYCLIC},
     * {@link #NULL_CONSTANT}, {@link #ZERO_CONSTANT}, {@link #NAN_CONSTANT},
     * as well as constants in standard Java enumerations, are unique instances, which cannot be equal to any other
     * instance of this class. So, you can use <code>==</code> Java operator to compare objects with these constants,
     * instead of calling {@link #equals(Object)} method of this class.</p>
     */
    class ContinuationMode {

        /**
         * Simplest continuation mode: any continuation outside the source matrix is disabled.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
         * always corresponds to the element of the source matrix <code>m</code>
         * with the coordinates
         * <code><i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i></code>,
         * where <code><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></code>
         * are the low endpoints of all coordinates in the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <code>m</code>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <code>m</code>.
         *
         * <p>In a case of this mode, {@link Matrix#subMatrix(long[], long[], ContinuationMode continuationMode)}
         * method is strictly equivalent to more simple {@link Matrix#subMatrix(long[], long[])}, and
         * {@link Matrix#subMatr(long[], long[], ContinuationMode continuationMode)}
         * is strictly equivalent to more simple {@link Matrix#subMatr(long[], long[])}.
         * In other words, all submatrix elements must lie inside the original matrix,
         * i.e. the returned matrix must be a true sub-matrix of the original one.
         * An attempt to create a submatrix with this continuation mode,
         * which does not lie fully inside the original matrix, leads to <code>IndexOutOfBoundsException</code>.
         */
        public static final ContinuationMode NONE = new ContinuationMode("not-continued mode");

        /**
         * The <i>cyclic</i> (or <i>true-cyclic</i>) continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
         * corresponds to the element of the built-in array
         * <code>m.{@link Matrix#array() array()}</code> of the source matrix <code>m</code>
         * with the index <code>m.{@link Matrix#cyclicIndex(long...)
         * cyclicIndex}(<i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i>)</code>,
         * where <code><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></code>
         * are the low endpoints of all coordinates in the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <code>m</code>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <code>m</code>.
         *
         * <p>In other words, in this mode you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite periodical repeating along all coordinate axes.
         */
        public static final ContinuationMode CYCLIC = new ContinuationMode(
                "cyclically-continued mode");

        /**
         * The <i>pseudo-cyclic</i> (or <i>toroidal</i>) continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
         * corresponds to the element of the built-in array
         * <code>m.{@link Matrix#array() array()}</code> of the source matrix <code>m</code>
         * with the index <code>m.{@link Matrix#pseudoCyclicIndex(long...)
         * pseudoCyclicIndex}(<i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i>)</code>,
         * where <code><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></code>
         * are the low endpoints of all coordinates in the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <code>m</code>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <code>m</code>.
         *
         * <p>In other words, in this mode you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite periodical repeating its {@link Matrix#array() built-in array}.
         * It is the most natural mode for many image processing algorithms,
         * which work directly with the built-in array instead of working with coordinates of matrix elements.
         */
        public static final ContinuationMode PSEUDO_CYCLIC = new ContinuationMode(
                "pseudo-cyclically-continued mode");

        /**
         * The <i>mirror-cyclic</i> continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
         * corresponds to the element of the built-in array
         * <code>m.{@link Matrix#array() array()}</code> of the source matrix <code>m</code>
         * with the index <code>m.{@link Matrix#mirrorCyclicIndex(long...)
         * mirrorCyclicIndex}(<i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i>)</code>,
         * where <code><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></code>
         * are the low endpoints of all coordinates in the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <code>m</code>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <code>m</code>.
         *
         * <p>In other words, in this mode you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite periodical repeating along all coordinate axes, if, while every "odd" repeating,
         * the matrix is symmetrically reflected along the corresponding coordinate.
         * In other words, it's possible to say that the matrix is infinitely reflected in each its bound as
         * in a mirror. Usually this mode provides the best smoothness of continuation of the matrix.
         */
        public static final ContinuationMode MIRROR_CYCLIC = new ContinuationMode(
                "mirroring-cyclically-continued mode");

        /**
         * The special case of constant continuation mode, corresponding to continuing by {@code null}
         * constant. Strictly equivalent to {@link #getConstantMode(Object) getConstantMode(null)}
         * (such a call always returns the reference to this constant).
         *
         * <p>Note: unlike {@link #ZERO_CONSTANT}, this mode can be used with any element type of the original matrix,
         * including non-primitive objects. For matrices with primitive element type, this mode is equivalent
         * to {@link #ZERO_CONSTANT}.
         */
        public static final ContinuationMode NULL_CONSTANT = new ConstantImpl(null);

        /**
         * The special popular case of constant continuation mode, corresponding to continuing by <code>0.0d</code>
         * numeric constant. Strictly equivalent to
         * {@link #getConstantMode(Object) getConstantMode(new Double(0.0d))}
         * (such a call always returns the reference to this constant).
         *
         * <p>Note: unlike {@link #NULL_CONSTANT}, this mode can be used only with matrices, containing elements of
         * some primitive type, i.e. with <code>{@link Matrix}&lt;? extends {@link PArray}&gt;</code>.
         */
        public static final ContinuationMode ZERO_CONSTANT = new ConstantImpl(0.0d);

        /**
         * The special popular case of constant continuation mode, corresponding to continuing by
         * <code>Double.NaN</code> numeric constant. Strictly equivalent to
         * {@link #getConstantMode(Object) getConstantMode(new Double(Double.NaN))}
         * (such a call always returns the reference to this constant).
         *
         * <p>Note: unlike {@link #NULL_CONSTANT}, this mode can be used only with matrices, containing elements of
         * some primitive type, i.e. with <code>{@link Matrix}&lt;? extends {@link PArray}&gt;</code>.
         */
        public static final ContinuationMode NAN_CONSTANT = new ConstantImpl(Double.NaN);

        /**
         * Creates an instance of this class for <i>constant</i> continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
         * corresponds to the element of the source matrix <code>m</code>
         * with the coordinates
         * <code><i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i></code>
         * (where <code><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></code>
         * are the low endpoints of all coordinates in the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method) &mdash;
         * if this element {@link Matrix#inside(long...) lies inside} the source matrix.
         * In this case, an attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <code>m</code>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <code>m</code>.
         * In another case (if this element lies outside the source matrix),
         * the element is considered to be equal <code>continuationConstant</code> (an argument of this method):
         * an attempt to read it returns this constant, and
         * an attempt to write into this element is just ignored.
         *
         * <p>In other words, in this mode, you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite appending it along all coordinates with the specified continuation constant.
         *
         * <p>The argument <code>continuationConstant</code> of this method is automatically cast to the type of
         * elements of the source matrix <code>m</code> according the following rules.
         *
         * <p>For non-primitive element types, the <code>continuationConstant</code> argument
         * must be some instance of the class <code>m.{@link #elementType()}</code>,
         * or its superclass, or {@code null}.
         * So, the type cast is trivial here.
         *
         * <p>For primitive element types, <code>continuationConstant</code> can be {@code null} or <i>any</i>
         * wrapper for primitive types: <code>Boolean</code>, <code>Character</code>,
         * <code>Byte</code>, <code>Short</code>,
         * <code>Integer</code>, <code>Long</code>, <code>Float</code>, <code>Double</code>. In this case,
         * the following casting rules are used while reading elements (I remind that attempts to write
         * outside the original matrix are ignored),
         * depending on the primitive type <code>m.{@link #elementType()}</code>:
         *
         * <ul>
         * <li>{@code null} is converted to <code>false</code> for bit elements or to zero (<code>0</code>,
         * <code>(char)0</code>, <code>0.0</code>) for all other element types
         * (so, it is the only universal continuation constant, which can be used with any element type:
         * see {@link #NULL_CONSTANT});</li>
         *
         * <li>if the wrapper type corresponds to the element primitive type, the trivial default conversion
         * is used; in all other cases:
         *
         * <li><code>Boolean</code> value <code>v</code> is converted to <code>v?1:0</code> for numeric element types
         * and to <code>v?(char)1:(char)0</code> for <code>char</code> element type;
         *
         * <li><code>Character</code> value <code>v</code> is converted to <code>(byte)v</code>,
         * <code>(short)v</code>, <code>(int)v</code>, <code>(long)v</code>,
         * <code>(float)v</code>, <code>(double)v</code>
         * for corresponding numeric element types
         * and to <code>v!=0</code> for <code>boolean</code> element type;</li>
         *
         * <li><code>Byte</code> value <code>v</code> is converted to <code>(char)(v&amp;0xFF)</code>,
         * <code>(short)(v&amp;0xFF)</code>, <code>(int)(v&amp;0xFF)</code>, <code>(long)(v&amp;0xFF)</code>,
         * <code>(float)(v&amp;0xFF)</code>, <code>(double)(v&amp;0xFF)</code>
         * for corresponding numeric or character element types
         * and to <code>v!=0</code> for <code>boolean</code> element type;</li>
         *
         * <li><code>Short</code> value <code>v</code> is converted to <code>(char)v</code>,
         * <code>(byte)v</code>, <code>(int)(v&amp;0xFFFF)</code>, <code>(long)(v&amp;0xFFFF)</code>,
         * <code>(float)(v&amp;0xFFFF)</code>, <code>(double)(v&amp;0xFFFF)</code>
         * for corresponding numeric or character element types
         * and to <code>v!=0</code> for <code>boolean</code> element type;</li>
         *
         * <li><code>Integer</code>, <code>Long</code>, <code>Float</code> or <code>Double</code> value <code>v</code>
         * is converted to <code>(char)v</code>,
         * <code>(byte)v</code>, <code>(short)v</code>, <code>(int)v</code>, <code>(long)v</code>,
         * <code>(float)v</code>, <code>(double)</code>v
         * for corresponding numeric or character element types
         * and to <code>v!=0</code> for <code>boolean</code> element type.</li>
         * </ul>
         *
         * @param continuationConstant the value returned while reading elements, lying outside this matrix.
         * @return new continuation mode with the specified continuation constant.
         */
        public static ContinuationMode getConstantMode(Object continuationConstant) {
            if (continuationConstant == null) {
                return NULL_CONSTANT;
            }
            if (ZERO_CONSTANT.continuationConstant().equals(continuationConstant)) {
                return ZERO_CONSTANT;
            }
            if (NAN_CONSTANT.continuationConstant().equals(continuationConstant)) {
                return NAN_CONSTANT;
            }
            return new ConstantImpl(continuationConstant);
        }

        private final String description;

        private ContinuationMode(String description) {
            assert description != null;
            this.description = description;
        }

        /**
         * Returns <code>true</code> if and only if this instance is a constant continuation mode,
         * i&#46;e&#46; was created by {@link #getConstantMode(Object)} method, or it is one
         * of the predefined constants {@link #ZERO_CONSTANT} and {@link #NULL_CONSTANT}.
         *
         * @return whether it is a constant continuation mode.
         */
        public boolean isConstant() {
            return false;
        }

        /**
         * Returns <code>true</code> if and only if {@link #isConstant()} returns true and
         * the result of {@link #continuationConstant()} is {@code null} or is an instance of
         * some  wrapper for primitive types: <code>Boolean</code>, <code>Character</code>,
         * <code>Byte</code>, <code>Short</code>,
         * <code>Integer</code>, <code>Long</code>, <code>Float</code> or <code>Double</code>.
         *
         * <p>This method indicates, whether this mode can be used for constant continuation of a matrix
         * with primitive type of elements. But note that such a mode can also be used for continuation of
         * a matrix, consisting of non-primitive elements, belonging to the corresponding wrapper type
         * or its superclass like <code>Number</code> or <code>Object</code>.
         *
         * @return whether it is a constant continuation mode, where the continuation constant is {@code null}
         * or some Java wrapper object for a primitive type.
         */
        public boolean isPrimitiveTypeOrNullConstant() {
            return false;
        }

        /**
         * Returns the continuation constant, used in this mode, if it is a
         * {@link #isConstant() constant continuation mode},
         * or throws {@link NonConstantMatrixContinuationModeException},
         * if it is not a constant continuation mode.
         *
         * <p>If this instance was created by {@link #getConstantMode(Object)} method,
         * this method returns exactly the same reference to an object, which was passed
         * to that method as <code>continuationConstant</code> argument.
         * For {@link #NULL_CONSTANT}, this method returns {@code null}.
         * For {@link #ZERO_CONSTANT}, this method returns <code>Double.valueOf(0.0d)</code>.
         * For {@link #NAN_CONSTANT}, this method returns <code>Double.valueOf(Double.NaN)</code>.
         *
         * @return the continuation constant, used in this mode,
         * if it is a {@link #isConstant() constant continuation mode},
         * @throws NonConstantMatrixContinuationModeException if this mode is not a constant continuation mode.
         * @see #isConstant()
         */
        public Object continuationConstant() {
            throw new NonConstantMatrixContinuationModeException(this + " has no continuation constant");
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            return description;
        }

        /**
         * Returns the hash code of this object.
         *
         * @return the hash code of this object.
         */
        @Override
        public int hashCode() {
            Object constant = isConstant() ? continuationConstant() : null;
            return description.hashCode() ^ (constant != null ? constant.hashCode() : 157);
        }

        /**
         * Indicates whether some continuation mode is equal to this instance.
         *
         * <p>If the argument is {@code null} or not an instance of this class,
         * this method returns <code>false</code>.
         *
         * <p>If this instance is a {@link #isConstant() constant continuation mode},
         * this method returns <code>true</code> if and only if the argument is also a constant continuation mode
         * and either both continuation constants, returned by {@link #continuationConstant()} method,
         * are {@code null}, or they are equal objects in terms of standard <code>equals</code> method
         * (i.e. <code>equal</code> method of the {@link #continuationConstant()} object returns <code>true</code>
         * for <code>((ContinuationMode)o).{@link #continuationConstant()}</code>).
         *
         * <p>If this instance is not a constant continuation mode, this method returns <code>true</code>
         * if and only if this instance and <code>o</code> argument are the same reference (<code>this==o</code>).
         * It is correct, because all only possible non-constant instances of this class are represented
         * by static constants of this class, as well as in standard enumerations.
         *
         * @param o the object to be compared for equality with this instance.
         * @return <code>true</code> if the specified object is a continuation mode equal to this one.
         */
        @Override
        public boolean equals(Object o) {
            if (!isConstant() || this == o) {
                return this == o;
            }
            if (!(o instanceof ContinuationMode && ((ContinuationMode) o).isConstant())) {
                return false;
            }
            Object constant = continuationConstant();
            return constant == null ? ((ContinuationMode) o).continuationConstant() == null :
                    constant.equals(((ContinuationMode) o).continuationConstant());
        }

        private static class ConstantImpl extends ContinuationMode {
            private final Object continuationConstant;
            private final boolean primitiveTypeOrNullConstant;

            private ConstantImpl(Object continuationConstant) {
                super("constantly-continued (by " + continuationConstant + ") mode");
                this.continuationConstant = continuationConstant;
                this.primitiveTypeOrNullConstant = continuationConstant == null
                        || continuationConstant instanceof Boolean
                        || continuationConstant instanceof Character
                        || continuationConstant instanceof Byte
                        || continuationConstant instanceof Short
                        || continuationConstant instanceof Integer
                        || continuationConstant instanceof Long
                        || continuationConstant instanceof Float
                        || continuationConstant instanceof Double;
            }

            @Override
            public boolean isConstant() {
                return true;
            }

            @Override
            public Object continuationConstant() {
                return continuationConstant;
            }

            @Override
            public boolean isPrimitiveTypeOrNullConstant() {
                return primitiveTypeOrNullConstant;
            }
        }
    }

    /**
     * Maximal number of dimensions for some complex algorithms or service classes: {@value}.
     * Most modules process matrices with any number of dimensions, but there are some cases
     * when an algorithm can work only with 2-dimensional, 3-dimensional or <i>n</i>-dimensional matrices with
     * <i>n</i><code>&lt;={@link #MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</code>.
     * In this package and all known subpackages of <code>net.algart</code> package,
     * the following classes require that the number of dimensions must not be greater
     * than {@link #MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}:
     * <ul>
     * <li><code>net.algart.matrices.scanning.ConnectedObjectScanner</code>;</li>
     * <li>{@link MatrixInfo}.</li>
     * </ul>
     *
     * <p>Note: the value of this constant ({@value}) is the maximal <i>n</i> so that
     * 3<sup><i>n</i></sup>&lt;32768=2<sup>15</sup> (3<sup>9</sup>=19683).
     * It can be useful while storing indexes of elements of little 3x3x3x... submatrix (aperture):
     * signed <code>short</code> type is enough in this case.
     */
    int MAX_DIM_COUNT_FOR_SOME_ALGORITHMS = 9;

    /**
     * Returns a reference to the built-in AlgART array.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     *
     * @return a reference to the built-in AlgART array.
     */
    T array();

    /**
     * Returns the type of matrix elements.
     * Equivalent to <code>{@link #array()}.{@link Array#elementType() elementType()}</code>.
     *
     * @return the type of the matrix elements.
     */
    Class<?> elementType();

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#toJavaArray() toJavaArray()}</code>.
     *
     * @return Java array containing all the elements in this matrix.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toBoolean()
     * @see #toByte()
     * @see #toChar()
     * @see #toShort()
     * @see #toInt()
     * @see #toLong()
     * @see #toFloat()
     * @see #toDouble()
     * @see #ja()
     * @see Array#toJavaArray()
     */
    Object toJavaArray();

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double,,boolean;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double,,Boolean;;
               \*\s+\@see \#jaBoolean\(\)\s*(?:\r(?!\n)|\n|\r\n)\s*\* ==> *,,... */

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toByte() toByte()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>byte</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toByte(byte[])
     * @see #jaByte()
     * @see #toJavaArray()
     * @see PArray#toByte()
     */
    default byte[] toByte() {
        return toByte(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toByte(byte[]) toByte(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>byte[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>byte</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toByte()
     * @see #jaByte()
     * @see #toJavaArray()
     * @see PArray#toByte(byte[])
     */
    default byte[] toByte(byte[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toByte(): " + this);
        }
        return a.toByte(result);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toChar() toChar()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>char</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toChar(char[])
     * @see #jaChar()
     * @see #toJavaArray()
     * @see PArray#toChar()
     */
    default char[] toChar() {
        return toChar(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toChar(char[]) toChar(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>char[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>char</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toChar()
     * @see #jaChar()
     * @see #toJavaArray()
     * @see PArray#toChar(char[])
     */
    default char[] toChar(char[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toChar(): " + this);
        }
        return a.toChar(result);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toShort() toShort()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>short</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toShort(short[])
     * @see #jaShort()
     * @see #toJavaArray()
     * @see PArray#toShort()
     */
    default short[] toShort() {
        return toShort(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toShort(short[]) toShort(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>short[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>short</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toShort()
     * @see #jaShort()
     * @see #toJavaArray()
     * @see PArray#toShort(short[])
     */
    default short[] toShort(short[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toShort(): " + this);
        }
        return a.toShort(result);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toInt() toInt()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>int</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toInt(int[])
     * @see #jaInt()
     * @see #toJavaArray()
     * @see PArray#toInt()
     */
    default int[] toInt() {
        return toInt(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toInt(int[]) toInt(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>int[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>int</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toInt()
     * @see #jaInt()
     * @see #toJavaArray()
     * @see PArray#toInt(int[])
     */
    default int[] toInt(int[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toInt(): " + this);
        }
        return a.toInt(result);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toLong() toLong()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>long</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toLong(long[])
     * @see #jaLong()
     * @see #toJavaArray()
     * @see PArray#toLong()
     */
    default long[] toLong() {
        return toLong(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toLong(long[]) toLong(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>long[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>long</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toLong()
     * @see #jaLong()
     * @see #toJavaArray()
     * @see PArray#toLong(long[])
     */
    default long[] toLong(long[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toLong(): " + this);
        }
        return a.toLong(result);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toFloat() toFloat()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>float</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toFloat(float[])
     * @see #jaFloat()
     * @see #toJavaArray()
     * @see PArray#toFloat()
     */
    default float[] toFloat() {
        return toFloat(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toFloat(float[]) toFloat(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>float[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>float</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toFloat()
     * @see #jaFloat()
     * @see #toJavaArray()
     * @see PArray#toFloat(float[])
     */
    default float[] toFloat(float[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toFloat(): " + this);
        }
        return a.toFloat(result);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toDouble() toDouble()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>double</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toDouble(double[])
     * @see #jaDouble()
     * @see #toJavaArray()
     * @see PArray#toDouble()
     */
    default double[] toDouble() {
        return toDouble(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toDouble(double[]) toDouble(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>double[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>double</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toDouble()
     * @see #jaDouble()
     * @see #toJavaArray()
     * @see PArray#toDouble(double[])
     */
    default double[] toDouble(double[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toDouble(): " + this);
        }
        return a.toDouble(result);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toBoolean() toBoolean()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>boolean</code> type.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toBoolean(boolean[])
     * @see #toJavaArray()
     * @see PArray#toBoolean()
     */
    default boolean[] toBoolean() {
        return toBoolean(null);
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#toBoolean(boolean[]) toBoolean(result)}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @param result the result <code>boolean[]</code> array; can be {@code null},
     *               then it will be created automatically.
     * @return a reference to <code>result</code> argument or (when <code>result==null</code>) a newly created array:
     * Java array containing all the elements in this matrix,
     * cast to <code>boolean</code> type according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the matrix size is greater than <code>Integer.MAX_VALUE</code>.
     * @throws IndexOutOfBoundsException     if the <code>result</code> argument is not {@code null}, but its length
     *                                       is too small: less than {@link Matrix#size() matrix.size()}.
     * @see #toBoolean()
     * @see #toJavaArray()
     * @see PArray#toBoolean(boolean[])
     */
    default boolean[] toBoolean(boolean[] result) {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using toBoolean(): " + this);
        }
        return a.toBoolean(result);
    }

    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#ja() ja()}</code>.
     *
     * @return Java array, equivalent to {@link #array()}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #jaByte()
     * @see #jaChar()
     * @see #jaShort()
     * @see #jaInt()
     * @see #jaLong()
     * @see #jaFloat()
     * @see #jaDouble()
     * @see #toJavaArray()
     * @see Array#ja()
     */
    Object ja();

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double */

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaByte() jaByte()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>byte</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toByte()
     * @see #toByte(byte[])
     * @see #ja
     * @see PArray#jaByte()
     */
    default byte[] jaByte() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaByte(): " + this);
        }
        return a.jaByte();
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaChar() jaChar()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>char</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toChar()
     * @see #toChar(char[])
     * @see #ja
     * @see PArray#jaChar()
     */
    default char[] jaChar() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaChar(): " + this);
        }
        return a.jaChar();
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaShort() jaShort()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>short</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toShort()
     * @see #toShort(short[])
     * @see #ja
     * @see PArray#jaShort()
     */
    default short[] jaShort() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaShort(): " + this);
        }
        return a.jaShort();
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaInt() jaInt()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>int</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toInt()
     * @see #toInt(int[])
     * @see #ja
     * @see PArray#jaInt()
     */
    default int[] jaInt() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaInt(): " + this);
        }
        return a.jaInt();
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaLong() jaLong()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>long</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toLong()
     * @see #toLong(long[])
     * @see #ja
     * @see PArray#jaLong()
     */
    default long[] jaLong() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaLong(): " + this);
        }
        return a.jaLong();
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaFloat() jaFloat()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>float</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toFloat()
     * @see #toFloat(float[])
     * @see #ja
     * @see PArray#jaFloat()
     */
    default float[] jaFloat() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaFloat(): " + this);
        }
        return a.jaFloat();
    }

    /**
     * Equivalent to <code>(PArray) {@link #array()}.{@link PArray#jaDouble() jaDouble()}</code>.
     * However, if the built-in AlgART array is not {@link PArray}, in other words,
     * if this matrix contains objects (non-primitive elements), this method
     * throws {@link UnsupportedOperationException} instead of {@link ClassCastException}.
     *
     * @return Java array containing all the elements in this matrix, cast to <code>double</code> type
     * according to AlgART rules.
     * @throws UnsupportedOperationException if {@link #array()} is not {@link PArray}.
     * @throws TooLargeArrayException        if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see #toDouble()
     * @see #toDouble(double[])
     * @see #ja
     * @see PArray#jaDouble()
     */
    default double[] jaDouble() {
        if (!(array() instanceof PArray a)) {
            throw new UnsupportedOperationException("Matrix, containing non-primitive (Object) elements, " +
                    "cannot be accessed using jaDouble(): " + this);
        }
        return a.jaDouble();
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns the total number of matrix elements.
     * Equivalent to <code>{@link #array()}.{@link Array#length() length()}</code>.
     *
     * @return the total number of matrix elements.
     */
    default long size() {
        return array().length();
    }

    /**
     * Returns the total number of matrix elements as 32-bit <code>int</code> value.
     * Equivalent to <code>{@link #array()}.{@link Array#length32() length32()}</code>.
     *
     * @return the total number of matrix elements, if it is less than 2<sup>31</sup>.
     * @throws TooLargeArrayException if the total number of matrix elements is greater than
     *                                <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1.
     */
    default int size32() {
        return array().length32();
    }

    /**
     * Equivalent to the call <code>{@link #size() size}() == 0</code>.
     *
     * @return <code>true</code> if the matrix is empty, i.e. at least one its dimensions is zero.
     */
    default boolean isEmpty() {
        return array().isEmpty();
    }

    /**
     * Returns <code>{@link #array()}.{@link Array#type() type()}</code>.
     *
     * @return the canonical type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is {@code null}.
     */
    Class<? extends Array> type();

    /**
     * Returns <code>{@link #array()}.{@link Array#updatableType() updatableType()}</code>.
     *
     * @return the canonical updatable type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is {@code null}.
     */
    Class<? extends UpdatableArray> updatableType();

    /**
     * Returns <code>{@link #array()}.{@link Array#type() type()}</code>,
     * if it is a subtype of (or same type as) the passed <code>arraySupertype</code>,
     * or throws <code>ClassCastException</code> in another case.
     * (If the passed argument is a class of {@link UpdatableArray} or some its
     * subinterfaces or subclasses, <code>IllegalArgumentException</code> is thrown instead:
     * updatable array classes cannot be specified in this method.)
     *
     * @param <U>            the generic type of AlgART array.
     * @param arraySupertype the required supertype of the built-in AlgART array.
     * @return the canonical type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException     if the passed argument is {@code null}.
     * @throws IllegalArgumentException if the passed argument is a class of {@link UpdatableArray} or some its
     *                                  subinterfaces or subclasses (updatable classes cannot be supertypes of
     *                                  for {@link Array#type() Array.type()}).
     * @throws ClassCastException       if <code>arraySupertype</code> does not allow storing
     *                                  the immutable version of the built-in AlgART array.
     */
    <U extends Array> Class<? extends U> type(Class<U> arraySupertype);

    /**
     * Returns <code>{@link #array()}.{@link Array#updatableType() updatableType()}</code>,
     * if it is a subtype of (or same type as) the passed <code>arraySupertype</code>,
     * or throws <code>ClassCastException</code> in another case.
     *
     * @param <U>            the generic type of AlgART array.
     * @param arraySupertype the required supertype of the built-in AlgART array.
     * @return the canonical updatable type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is {@code null}.
     * @throws ClassCastException   if <code>arraySupertype</code> does not allow storing
     *                              the built-in AlgART array.
     */
    <U extends Array> Class<? extends U> updatableType(Class<U> arraySupertype);

    /**
     * Returns <code>true</code> if and only if the {@link #elementType() element type} is primitive:
     * <code>{@link #array()} instanceof {@link PArray}</code>.
     *
     * @return whether the type of matrix element is boolean, char, byte, short, int, long, float or double.
     */
    boolean isPrimitive();

    /**
     * Returns <code>true</code> if and only if the {@link #elementType() element type} is <code>float</code>
     * or <code>double</code>:
     * <code>{@link #array()} instanceof {@link PFloatingArray}</code>.
     *
     * @return whether the type of matrix element is float or double.
     */
    boolean isFloatingPoint();

    /**
     * Returns <code>true</code> if and only if the {@link #elementType() element type} is fixed-point:
     * <code>{@link #array()} instanceof {@link PFixedArray}</code>.
     *
     * @return whether the type of matrix element is byte, short, int, long, char or boolean.
     */
    boolean isFixedPoint();

    /**
     * Returns <code>true</code> if and only if the {@link #elementType() element type} is <code>boolean.class</code>,
     * <code>short.class</code>, <code>byte.class</code> or <code>short.class</code>.
     *
     * <p>Equivalent to {@link Arrays#isUnsignedElementType(Class)
     * Arrays.isUnsignedElementType}(thisMatrix.{@link #elementType() elementType()}).
     *
     * @return whether the element type of this matrix should be interpreted as unsigned primitive type.
     */
    boolean isUnsigned();

    /**
     * Returns the number of in bits, required for each element of this matrix, if they are
     * {@link #isPrimitive() primitive}; in another case returns &minus;1.
     * Equivalent to {@link Arrays#bitsPerElement(Class)
     * Arrays.bitsPerElement}(thisMatrix.{@link #elementType() elementType()}).
     *
     * @return the size of each element in bits or &minus;1 if for non-primitive elements.
     */
    long bitsPerElement();

    /**
     * Returns the maximal possible value, that can stored in elements of this matrix,
     * if they are fixed-point elements, or the argument for floating-point elements,
     * or <code>Double.NaN</code> if elements are not primitive.
     *
     * <p>Equivalent to<pre>
     *     thisMatrix.{@link #isPrimitive() isPrimitive()} ?
     *         ((PArray) thisMatrix.array()).{@link PArray#maxPossibleValue(double)
     *         maxPossibleValue(valueForFloatingPoint)} :
     *         Double.NaN;
     * </pre>
     *
     * @param valueForFloatingPoint some "default" value returned for floating-point element type.
     * @return {@link #array()}.{@link PArray#maxPossibleValue maxPossibleValue()} for primitive element types,
     * or <code>Double.NaN</code> for non-primitive element types.
     */
    double maxPossibleValue(double valueForFloatingPoint);

    /**
     * Returns the maximal possible value, that can stored in elements of this matrix,
     * if they are fixed-point elements, or <code>1.0</code> for floating-point elements,
     * or <code>Double.NaN</code> if elements are not primitive.
     *
     * <p>Equivalent to {@link #maxPossibleValue(double) maxPossibleValue(1.0)}.
     * It is a good default for most application.
     *
     * @return maximal possible value for primitive element types (1.0 for float/double),
     * or <code>Double.NaN</code> for non-primitive element types.
     */
    double maxPossibleValue();

    /**
     * Returns an array containing all dimensions of this matrix.
     * Returned array is equal to the <code>dim</code> argument passed to methods that create new matrix instances.
     *
     * <p>The returned array is a clone of the internal dimension array, stored in this object.
     * The returned array is never empty (its length cannot be zero).
     * The elements of the returned array are never negative.
     *
     * @return an array containing all dimensions of this matrix.
     */
    long[] dimensions();

    /**
     * Returns the number of dimensions of this matrix.
     * This value is always positive (&gt;=1).
     * Equivalent to <code>{@link #dimensions()}.length</code>, but works faster.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     *
     * @return the number of dimensions of this matrix.
     */
    int dimCount();

    /**
     * Returns the dimension <code>#n</code> of this matrix
     * or <code>1</code> if <code>n&gt;={@link #dimCount()}</code>.
     * Equivalent to <code>n&lt;{@link #dimCount()}?{@link #dimensions()}[n]:1</code>, but works faster.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @param n the index of dimension.
     * @return the dimension <code>#n</code> of this matrix or 1 if the index is too large.
     * @throws IndexOutOfBoundsException if <code>n&lt;0</code> (but <i>not</i> if <code>n</code> is too large).
     * @see #dim32(int)
     */
    long dim(int n);

    /**
     * Returns the same result as the method {@link #dim(int) dim(n)} as 32-bit <code>int</code> value.
     * If the dimension, returned by  {@link #dim(int) dim(n)}, is greater than <code>Integer.MAX_VALUE</code>,
     * throws <code>TooLargeArrayException</code>.
     *
     * <p>This method is convenient when you are sure that matrix sizes
     * cannot exceed the limit 2<sup>31</sup>&minus;1.</p>
     *
     * @param n the index of dimension.
     * @return the dimension <code>#n</code> of this matrix, if it is less than 2<sup>31</sup>.
     * @throws IndexOutOfBoundsException if <code>n&lt;0</code> (but <i>not</i> if <code>n</code> is too large).
     * @throws TooLargeArrayException    if this matrix dimension is greater than
     *                                   <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1.
     */
    default int dim32(int n) {
        long result = dim(n);
        if (result < 0) {
            throw new AssertionError("Negative result " + result + " of dim() method");
        }
        if (result > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large matrix dimension #" + n + " (" +
                    result + " >= 2^31): " + this);
        }
        return (int) result;
    }

    /**
     * Equivalent to <code>{@link #dim(int) dim}(0)</code>.
     *
     * @return the first matrix dimension.
     */
    default long dimX() {
        return dim(0);
    }

    /**
     * Equivalent to <code>{@link #dim32(int) dim32}(0)</code>.
     *
     * @return the first matrix dimension.
     * @throws TooLargeArrayException if this matrix dimension is greater than
     *                                <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1.
     */
    default int dimX32() {
        return dim32(0);
    }

    /**
     * Equivalent to <code>{@link #dim(int) dim}(1)</code>.
     *
     * @return the second matrix dimension (or 1 for 1-dimensional matrix).
     */
    default long dimY() {
        return dim(1);
    }

    /**
     * Equivalent to <code>{@link #dim32(int) dim32}(1)</code>.
     *
     * @return the second matrix dimension (or 1 for 1-dimensional matrix).
     * @throws TooLargeArrayException if this matrix dimension is greater than
     *                                <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1.
     */
    default int dimY32() {
        return dim32(1);
    }

    /**
     * Equivalent to <code>{@link #dim(int) dim}(2)</code>.
     *
     * @return the third matrix dimension (or 1 for 1-dimensional or 2-dimensional matrix).
     */
    default long dimZ() {
        return dim(2);
    }

    /**
     * Equivalent to <code>{@link #dim32(int) dim32}(2)</code>.
     *
     * @return the third matrix dimension (or 1 for 1-dimensional or 2-dimensional matrix).
     * @throws TooLargeArrayException if this matrix dimension is greater than
     *                                <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1.
     */
    default int dimZ32() {
        return dim32(2);
    }

    /**
     * Indicates whether the other matrix has the same dimension array.
     * In other words, returns <code>true</code> if and only if
     * both matrices have the same dimension count ({@link #dimCount()})
     * and the corresponding dimensions ({@link #dim(int) dim(k)}) are equal.
     *
     * @param m the matrix to be compared for equal dimensions with this matrix.
     * @return <code>true</code> if the specified matrix has the same dimension array.
     * @throws NullPointerException if the passed argument is {@code null}.
     * @see #dimEquals(long...)
     */
    boolean dimEquals(Matrix<?> m);

    /**
     * Indicates whether the passed dimensions are equal to the dimension array of this matrix.
     * In other words, returns <code>true</code> if and only if
     * <code>dimension.length=={@link #dimCount()}</code>
     * and the corresponding dimensions <code>{@link #dim(int) dim(k)}==dimension[k]</code> for all <code>k</code>.
     *
     * <p>Note: this method does not check, whether all passed dimensions are correct (in particular, non-negative).
     * If some elements of the passed array are incorrect, this method just returns <code>false</code>.
     * But it the passed array is {@code null}, this method throws <code>NullPointerException</code>.
     *
     * @param dimensions the dimension array.
     * @return <code>true</code> if the specified dimensions are equal to the dimensions of this matrix.
     * @throws NullPointerException if the passed argument is {@code null}.
     * @see #dimEquals(Matrix)
     */
    boolean dimEquals(long... dimensions);

    /**
     * Returns the linear index in the built-in AlgART array of the matrix element
     * with specified coordinates.
     *
     * <p>More precisely,
     * <code>index(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i>)</code>
     * returns the following value:
     *
     * <blockquote>
     * <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
     * </blockquote>
     * <p>
     * where <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</code>.
     * All passed indexes <code><i>i<sub>k</sub></i></code> must be in ranges <code>0..<i>d<sub>k</sub></i>-1</code>.
     *
     * <p>All elements of <code>coordinates</code> array are always used, regardless
     * of the number of matrix dimensions.
     * But the extra elements of <code>coordinates</code> array must be zero,
     * because <code><i>d<sub>k</sub></i>=1</code> for <code><i>k</i>&gt;={@link #dimCount()}</code>.
     *
     * <p>Good algorithms processing the matrix should use this method rarely:
     * usually there are more optimal ways to calculate necessary linear index.
     * For example, if you just need to calculate something for all matrix elements,
     * the best way is the following:
     *
     * <pre>
     * Array a = m.array();
     * for (long disp = 0, n = a.length(); disp &lt; n; disp++)
     * &#32;   // process the element #k of the array
     * </pre>
     *
     * @param coordinates all coordinates.
     * @return the linear index of the matrix element with specified coordinates.
     * @throws NullPointerException      if the passed array is {@code null}.
     * @throws IllegalArgumentException  if the passed array is empty (no coordinates are passed).
     * @throws IndexOutOfBoundsException if some coordinate <code><i>i<sub>k</sub></i></code> is out of range
     *                                   <code>0..<i>d<sub>k</sub></i>-1</code>.
     * @see #uncheckedIndex(long...)
     * @see #cyclicIndex(long...)
     * @see #pseudoCyclicIndex(long...)
     * @see #mirrorCyclicIndex(long...)
     * @see #coordinates(long, long[])
     * @see IPoint#toOneDimensional(long[], boolean)
     */
    long index(long... coordinates);

    /**
     * The simplified version of the full {@link #index(long...) index} method for the case
     * of 2-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @return <code>y * {@link #dimX()} + x</code>.
     * @throws IndexOutOfBoundsException if <code>x&lt;0</code>, <code>x&gt;={@link #dimX()}</code>,
     *                                   <code>y&lt;0</code> or <code>y&gt;={@link #dimX()}</code>.
     */
    long index(long x, long y);

    /**
     * The simplified version of the full {@link #index(long...) index} method for the case
     * of 3-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @param z the third coordinate.
     * @return <code>z * {@link #dimY()} * {@link #dimX()} + y * {@link #dimX()} + x</code>.
     * @throws IndexOutOfBoundsException if <code>x&lt;0</code>, <code>x&gt;={@link #dimX()}</code>,
     *                                   <code>y&lt;0</code>, <code>y&gt;={@link #dimX()}</code>,
     *                                   <code>z&lt;0</code> or <code>z&gt;={@link #dimZ()}</code>.
     */
    long index(long x, long y, long z);

    /**
     * Returns the coordinates in the matrix, corresponding to the given linear index in the built-in AlgART array.
     * This method is reverse to {@link #index(long...)}: for any index,
     * <code>{@link #index(long...) index}({@link #coordinates(long, long[])
     * coordinates}(index, null)) == index</code>.
     *
     * <p>The <code>result</code> argument can be {@code null} or some array,
     * containing at least {@link #dimCount()}
     * elements. If the first case, this method allocates new Java array <code>long[{@link #dimCount()}]</code>
     * for storing coordinates and returns it.
     * In the second case, this method stores the found coordinates in <code>result</code> array and returns it.
     * The returned coordinates are always in ranges
     * <pre>
     * 0 &le; result[<i>k</i>] &lt; {@link #dim(int) dim}(<i>k</i>)</pre>
     *
     * @param index  the linear index in the built-in AlgART array.
     * @param result the array where you want to store results; can be {@code null}.
     * @return a reference to the <code>result</code> argument, if it is not {@code null},
     * else newly created Java array contains all calculated coordinates.
     * @throws IllegalArgumentException  if <code>result!=null</code>,
     *                                   but <code>result.length&lt;{@link #dimCount()}</code>.
     * @throws IndexOutOfBoundsException if <code>index&lt;0</code> or <code>index&gt;={@link #dim(int)
     *                                   dim}(0)*{@link #dim(int) dim}(1)*...={@link #array()}.{@link
     *                                   Array#length() length()}</code>.
     */
    long[] coordinates(long index, long[] result);

    /**
     * An analog of {@link #index(long...)} method, that does not check,
     * whether the passed coordinates are in the required ranges.
     *
     * <p>More precisely,
     * <code>uncheckedIndex(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i>)</code>
     * always returns the following value:
     *
     * <blockquote>
     * <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
     * </blockquote>
     * <p>
     * where <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</code>.
     *
     * <p>All calculations are performed with <code>long</code> type without any overflow checks.
     * All elements of <code>coordinates</code> array are always used, regardless of the number of matrix dimensions.
     * Please remember that <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)=1</code>
     * for <code><i>k</i>&gt;={@link #dimCount()}</code>
     * (extra elements of <code>coordinates</code> array).
     *
     * @param coordinates all coordinates.
     * @return the linear index of the matrix element with specified coordinates, without range checks.
     * @throws NullPointerException     if the passed array is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     */
    long uncheckedIndex(long... coordinates);

    /**
     * An analog of {@link #index(long...)} method, that, before all calculations,
     * replaces the passed coordinates with the positive remainders
     * from division of them by the corresponding matrix dimensions.
     *
     * <p>More precisely, let <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
     * are the arguments of the method. Let
     * <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</code> and
     * <blockquote>
     * <i>i'<sub>k</sub></i> = <i>d<sub>k</sub></i> == 0 ? 0 :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> &gt;= 0 ?
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> + <i>d<sub>k</sub></i>
     * </blockquote>
     * <p>
     * This method returns the following value:
     *
     * <blockquote>
     * <i>i'<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i'<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i'<sub>1</sub>d<sub>0</sub></i> + <i>i'<sub>0</sub></i>,
     * </blockquote>
     * <p>
     * In other words, the resulting index is "cyclical".
     *
     * <p>All elements of <code>coordinates</code> array are always used,
     * regardless of the number of matrix dimensions.
     * (You can note that extra elements of <code>coordinates</code> array are ignored in fact:
     * the reminders <i>i<sub>k</sub></i>%<i>d<sub>k</sub></i>=<i>i<sub>k</sub></i>%1 will be zero for them.)
     *
     * @param coordinates all coordinates.
     * @return the cyclical linear index of the matrix element with specified coordinates,
     * without range checks.
     * @throws NullPointerException     if the passed array is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     * @see Matrix.ContinuationMode#CYCLIC
     */
    long cyclicIndex(long... coordinates);

    /**
     * An analog of {@link #index(long...)} method, that does not check,
     * whether the passed coordinates are in the required ranges,
     * but replaces the resulting index with the positive remainder
     * from division of it by the length of the built-in array.
     *
     * <p>More precisely, let <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
     * are the arguments of the method, and <code><i>index</i></code> is the following value
     * (as in {@link #index(long...)} method):
     *
     * <blockquote>
     * <code><i>index</i></code> = <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
     * </blockquote>
     * <p>
     * where <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</code>.
     * Here we <i>do no require</i> that the passed indexes <code><i>i<sub>k</sub></i></code>
     * are in ranges <code>0..<i>d<sub>k</sub></i>-1</code>.
     * Then, let <code><i>len</i>={@link #array()}.{@link Array#length()
     * length()}=d<sub>n-1</sub>...d<sub>1</sub>d<sub>0</sub></code>.
     * The result of this method is the following:
     *
     * <blockquote>
     * <code><i>len</i> == 0 ? 0 : <i>index</i> % <i>len</i> &gt;= 0 ?
     * <i>index</i> % <i>len</i> : <i>index</i> % <i>len</i> + <i>len</i></code>
     * </blockquote>
     * <p>
     * (It is in the <code>0..<i>len</i>-1</code> range always,
     * excepting the degenerated case <code><i>len</i>==0</code>.)
     * In other words, the resulting index is "pseudo-cyclical", as the resulting shift
     * in {@link Matrices#asShifted(Matrix, long...)} method.
     *
     * <p>All elements of <code>coordinates</code> array are always used,
     * regardless of the number of matrix dimensions.
     * (You can note that extra elements of <code>coordinates</code> array are ignored in fact:
     * they add <code>k*<i>len</i></code> summand, where <code>k</code> is an integer.)
     *
     * <p>Note that all calculations are performed absolutely precisely, even in a case when
     * the direct calculation according the formulas above leads to overflow (because some
     * of the values in these formulas are out of <code>Long.MIN_VALUE..Long.MAX_VALUE</code> range).
     *
     * @param coordinates all coordinates.
     * @return the pseudo-cyclical linear index of the matrix element with specified coordinates,
     * without range checks.
     * @throws NullPointerException     if the passed array is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     * @see IPoint#toOneDimensional(long[], boolean)
     * @see Matrix.ContinuationMode#PSEUDO_CYCLIC
     */
    long pseudoCyclicIndex(long... coordinates);

    /**
     * An analog of {@link #index(long...)} method, that, before all calculations,
     * replaces the passed coordinates with the positive remainders
     * from division of them by the corresponding matrix dimensions
     * or with complement of these remainders on the dimensions,
     * as if the matrix would be reflected in each its bound as in a mirror.
     *
     * <p>More precisely, let <code><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></code>
     * are the arguments of the method. Let
     * <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</code>,
     * <blockquote>
     * <i>i'<sub>k</sub></i> = <i>d<sub>k</sub></i> == 0 ? 0 :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> &gt;= 0 ?
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> + <i>d<sub>k</sub></i>
     * </blockquote>
     * (as in {@link #cyclicIndex(long...)}) and
     * <blockquote>
     * <i>i''<sub>k</sub></i> = <i>d<sub>k</sub></i> == 0 ? 0 :
     * &lfloor;<i>i<sub>k</sub></i> / <i>d<sub>k</sub></i>&rfloor; % 2 == 0 ?
     * <i>i'<sub>k</sub></i> :
     * <i>d<sub>k</sub></i> &minus; 1 &minus; <i>i'<sub>k</sub></i>
     * </blockquote>
     * (here &lfloor;<i>x</i>&rfloor; means the integer part of <i>x</i>, i.e. <code>Math.floor(<i>x</i>)</code>).
     *
     * <p>This method returns the following value:
     *
     * <blockquote>
     * <i>i''<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i''<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i''<sub>1</sub>d<sub>0</sub></i> + <i>i''<sub>0</sub></i>,
     * </blockquote>
     *
     * <p>In other words, the resulting index is "mirroring-cyclical".
     *
     * <p>All elements of <code>coordinates</code> array are always used, regardless of the number of matrix dimensions.
     * (You can note that extra elements of <code>coordinates</code> array are ignored in fact:
     * the reminders <i>i<sub>k</sub></i>%<i>d<sub>k</sub></i>=<i>i<sub>k</sub></i>%1 will be zero for them.)
     *
     * @param coordinates all coordinates.
     * @return the mirror-cyclical linear index of the matrix element with specified coordinates,
     * without range checks.
     * @throws NullPointerException     if the passed array is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     * @see Matrix.ContinuationMode#MIRROR_CYCLIC
     */
    long mirrorCyclicIndex(long... coordinates);

    /**
     * Returns <code>true</code> if all specified coordinates <code><i>i<sub>k</sub></i></code>
     * are inside the ranges <code>0..<i>d<sub>k</sub></i>-1</code>,
     * where <code><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</code>.
     *
     * <p>This method allows simply check that the arguments of
     * the {@link #index(long...) index} method are correct and will not lead to
     * <code>IndexOutOfBoundsException</code>:
     * <pre>
     * if (matrix.inside(i1, i2, ...)) {
     * &#32;   long index = matrix.index(i1, i2, ...);
     * &#32;   // processing an element at this index
     * } else {
     * &#32;   // special branch for positions outside the matrix
     * }
     * </pre>
     *
     * @param coordinates all coordinates.
     * @return <code>true</code> if all specified coordinates are inside the matrix.
     * @throws NullPointerException     if the passed array is {@code null}.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     */
    boolean inside(long... coordinates);

    /**
     * The simplified version of the full {@link #inside(long...) inside} method for the case
     * of 2-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @return <code>true</code> if all specified coordinates are inside the matrix.
     */
    boolean inside(long x, long y);

    /**
     * The simplified version of the full {@link #inside(long...) inside} method for the case
     * of 3-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @param z the third coordinate.
     * @return <code>true</code> if all specified coordinates are inside the matrix.
     */
    boolean inside(long x, long y, long z);

    /**
     * Returns the new matrix backed by the specified AlgART array with the same dimensions as this one.
     * Equivalent to <code>{@link Matrices#matrix(Array, long...)
     * Matrices.matrix}(anotherArray, {@link #dimensions()})</code>.
     *
     * <p>The array <code>anotherArray</code> must be {@link Array#isUnresizable() unresizable},
     * and its length must be equal to the length of the array built-in this matrix.
     *
     * @param <U>          the generic type of AlgART array.
     * @param anotherArray some another AlgART array with the same length as {@link #array()}.
     * @return new matrix instance.
     * @throws NullPointerException     if <code>anotherArray</code> argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is resizable
     *                                  (for example, implements {@link MutableArray}).
     * @throws SizeMismatchException    if the product of all dimensions is not equal to the passed array length.
     */
    <U extends Array> Matrix<U> matrix(U anotherArray);

    /**
     * Returns this matrix, cast to the specified generic array type,
     * or throws <code>ClassCastException</code> if the built-in AlgART array
     * cannot be cast to the required type (because the array type is not its subclass).
     * Works alike <code>{@link #matrix(Array) matrix}((U)array)</code>, but returns
     * the reference to this instance and is compiled without "unchecked cast" warning.
     *
     * <p>This method is useful when you need to cast the type of AlgART array,
     * built in this matrix, to to its sub- or superinterface.
     *
     * @param <U>        the generic type of AlgART array.
     * @param arrayClass the type of built-in array in the new matrix.
     * @return new matrix with the same dimensions, based on the same array cast to the required type.
     * @throws NullPointerException if the argument is {@code null}.
     * @throws ClassCastException   if the built-in AlgART array cannot be cast to the required type.
     */
    <U extends Array> Matrix<U> cast(Class<U> arrayClass);

    /**
     * Returns a view of the rectangular fragment of this matrix between <code>from</code>,
     * inclusive, and <code>to</code>, exclusive.
     *
     * <p>More precisely, the returned matrix consists of all elements of this one with coordinates
     * <i>i<sub>0</sub></i>, <i>i<sub>1</sub></i>, ..., <i>i<sub>n&minus;1</sub></i>,
     * <i>n</i><code>={@link #dimCount()}</code>,
     * matching the following conditions:<pre>
     *     from[0] &lt;= <i>i<sub>0</sub></i> &lt; to[0],
     *     from[1] &lt;= <i>i<sub>1</sub></i> &lt; to[1],
     *     . . .
     *     from[<i>n</i>-1] &lt;= <i>i<sub>n-1</sub></i> &lt; to[<i>n</i>-1]
     * </pre>
     * <p>
     * So, every dimension {@link #dim(int) dim(k)} in the returned matrix will be equal to <code>to[k]-from[k]</code>.
     * The following condition must be fulfilled for all <code>k</code>:
     * <code>0&lt;=from[k]&lt;=to[k]&lt;=thisMatrix.{@link #dim(int) dim(k)}</code>.
     * The {@link #elementType() element type} of the returned matrix is identical to the element type
     * of this matrix.
     *
     * <p>This method is equivalent to the call
     * <code>{@link #subMatrix(long[], long[], ContinuationMode)
     * subMatrix}(from,to,{@link Matrix.ContinuationMode#NONE})</code>.
     *
     * <p>The built-in AlgART array of the returned matrix is backed by the built-in array of this matrix,
     * so &mdash; if this matrix is not {@link #isImmutable() immutable}
     * &mdash; any changes of the elements in the returned matrix are reflected in this matrix, and vice versa.
     * The returned matrix is {@link #isImmutable() immutable} if, and only if,
     * the built-in array of this matrix does not implement {@link UpdatableArray}.
     * The {@link Array#asTrustedImmutable()} method
     * in the built-in array of the returned matrix is equivalent to {@link Array#asImmutable()},
     * and {@link Array#asCopyOnNextWrite()} method just returns the full copy of the array.
     *
     * @param from low endpoints (inclusive) of all coordinates.
     * @param to   high endpoints (exclusive) of all coordinates.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>from</code> or <code>to</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>from.length</code> or <code>to.length</code>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if, for some <code>k</code>,
     *                                   <code>from[k]&lt;0 || to[k]&gt;{@link #dim(int) dim(k)} ||
     *                                   from[k]&gt;to[k]</code>.
     * @see #subMatrix(long[], long[], ContinuationMode)
     * @see #subMatrix(IRectangularArea)
     * @see #subMatr(long[], long[], ContinuationMode)
     * @see #subMatr(long[], long[])
     * @see #isSubMatrix()
     */
    Matrix<T> subMatrix(long[] from, long[] to);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to)} method, where
     * <code>from.length=to.length=area.{@link IRectangularArea#coordCount() coordCount()}</code>,
     * <code>from[k]=area.{@link IRectangularArea#min(int) min}(k)</code>,
     * <code>to[k]=area.{@link IRectangularArea#max(int) max}(k)+1</code>.
     *
     * @param area rectangular area within this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if the argument is {@code null}.
     * @throws IllegalArgumentException  if <code>area.{@link IRectangularArea#coordCount() coordCount()}</code>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if, for some <code>k</code>,
     *                                   <code>min[k]&lt;0 || max[k]&gt;={@link #dim(int) dim(k)}</code>, where
     *                                   <code>min=area.{@link IRectangularArea#min()
     *                                   min()}.{@link IPoint#coordinates() coordinates()}</code> and,
     *                                   <code>max=area.{@link IRectangularArea#max()
     *                                   max()}.{@link IPoint#coordinates() coordinates()}</code>.
     */
    Matrix<T> subMatrix(IRectangularArea area);

    /**
     * Equivalent to <code>{@link #subMatrix(long[], long[])
     * subMatrix}(new long[]{fromX,fromY}, new long[]{toX,toY})</code>.
     * Note that this matrix must be 2-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param fromX low endpoints (inclusive) of the first coordinate.
     * @param fromY low endpoints (inclusive) of the second coordinate.
     * @param toX   high endpoints (exclusive) of the first coordinate.
     * @param toY   high endpoints (exclusive) of the second coordinate.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=2</code>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatrix(long[], long[])}.
     */
    Matrix<T> subMatrix(long fromX, long fromY, long toX, long toY);

    /**
     * Equivalent to <code>{@link #subMatrix(long[], long[])
     * subMatrix}(new long[]{fromX,fromY,fromZ}, new long[]{toX,toY,toZ})</code>.
     * Note that this matrix must be 3-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param fromX low endpoints (inclusive) of the first coordinate.
     * @param fromY low endpoints (inclusive) of the second coordinate.
     * @param fromZ low endpoints (inclusive) of the third coordinate.
     * @param toX   high endpoints (exclusive) of the first coordinate.
     * @param toY   high endpoints (exclusive) of the second coordinate.
     * @param toZ   high endpoints (exclusive) of the third coordinate.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=3</code>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatrix(long[], long[])}.
     */
    Matrix<T> subMatrix(long fromX, long fromY, long fromZ, long toX, long toY, long toZ);

    /**
     * An extended analog of {@link #subMatrix(long[], long[])} method, allowing to get a rectangular fragment
     * which is not fully inside this matrix.
     *
     * <p>More precisely, unlike {@link #subMatrix(long[], long[])}, here
     * the only requirement for the <code>from</code>
     * and <code>to</code> coordinate boundaries is <code>from[k]&lt;=to[k]</code>,
     * but <code>from[k]</code> may be negative and <code>to[k]</code> may be greater than {@link #dim(int) dim(k)}.
     * (And there is also a trivial obvious requirement
     * <code>to[k]-from[k]&le;Long.MAX_VALUE</code>, i.e. that the dimensions of the result must
     * be representable by <code>long</code> type.)
     *
     * <p>The elements of the returned matrix, that do not correspond to any elements of this one,
     * i.e. "lie outside" of this matrix, are considered to be equal to some values, according to
     * some <i>continuation model</i>, described by <code>continuationMode</code> argument.
     * Such "outside" elements can correspond (according some rules) to actual elements of the source elements &mdash;
     * then attempts to read them return the values of the corresponding source elements
     * and attempts to write into them modify the corresponding source elements
     * (it is so for {@link ContinuationMode#CYCLIC}, {@link ContinuationMode#PSEUDO_CYCLIC},
     * {@link ContinuationMode#MIRROR_CYCLIC} modes),
     * &mdash; or can be calculated "virtually" (according some rules) &mdash;
     * then attempts to read them return the calculated values
     * and attempts to modify them are ignored
     * (it is so for the {@link ContinuationMode#getConstantMode(Object) constant continuation} mode).
     * See {@link ContinuationMode} class for more details.
     *
     * <p>Important note: there are two cases, when requirements to the <code>from</code> and <code>to</code>
     * coordinate boundaries are stronger, than described above.
     * <ol>
     *     <li>If <code>continuationMode=={@link ContinuationMode#NONE}</code>, this method is strictly
     *     equivalent to more simple {@link Matrix#subMatrix(long[], long[])} method,
     *     so all requirements are the same as for that method.</li>
     *     <li>If <code>continuationMode</code> is {@link ContinuationMode#CYCLIC},
     *     {@link ContinuationMode#PSEUDO_CYCLIC} or {@link ContinuationMode#MIRROR_CYCLIC}
     *     (but it is not a constant continuation mode) and some dimension <code>#k</code>
     *     of this matrix is zero &mdash; <code>{@link #dim(int) dim}(k)==0</code> &mdash;
     *     then both corresponding coordinate boundaries <code>from[k]</code> and <code>to[k]</code>
     *     must be zero (as in {@link Matrix#subMatrix(long[], long[])} method).</li>
     * </ol>
     *
     * @param from             low endpoints (inclusive) of all coordinates.
     * @param to               high endpoints (exclusive) of all coordinates.
     * @param continuationMode the mode of continuation outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>from</code>, <code>to</code> or <code>continuationMode</code>
     *                                   argument is {@code null}.
     * @throws IllegalArgumentException  if <code>from.length</code> or <code>to.length</code>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatrix(long[], long[])} method;
     *                                   for other cases &mdash; if, for some <code>k</code>,
     *                                   <code>from[k]&gt;to[k]</code> or
     *                                   <code>to[k]-from[k]&gt;Long.MAX_VALUE</code>,
     *                                   or if (for some <code>k</code>) <code>{@link #dim(int) dim(k)}==0</code> and
     *                                   <code>from[k]!=0 || to[k]!=0</code>,
     *                                   or if the product of all differences <code>to[k]-from[k]</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     * @see #subMatr(long[], long[], ContinuationMode)
     * @see #subMatrix(long[], long[])
     * @see #subMatr(long[], long[])
     * @see #isSubMatrix()
     */
    Matrix<T> subMatrix(long[] from, long[] to, ContinuationMode continuationMode);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to, ContinuationMode continuationMode)} method, where
     * <code>from.length=to.length=area.{@link IRectangularArea#coordCount() coordCount()}</code>,
     * <code>from[k]=area.{@link IRectangularArea#min(int) min}(k)</code>,
     * <code>to[k]=area.{@link IRectangularArea#max(int) max}(k)+1</code>.
     *
     * @param area             rectangular area within this matrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if one of the arguments is {@code null}.
     * @throws IllegalArgumentException  if <code>area.{@link IRectangularArea#coordCount() coordCount()}</code>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatrix(long[], long[])} method; for other cases &mdash;
     *                                   if the product of all <code>area.{@link IRectangularArea#sizes()}</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    Matrix<T> subMatrix(IRectangularArea area, ContinuationMode continuationMode);

    /**
     * Equivalent to <code>{@link #subMatrix(long[], long[], ContinuationMode)
     * subMatrix}(new long[]{fromX,fromY}, new long[]{toX,toY}, continuationMode)</code>.
     * Note that this matrix must be 2-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param fromX            low endpoints (inclusive) of the first coordinate.
     * @param fromY            low endpoints (inclusive) of the second coordinate.
     * @param toX              high endpoints (exclusive) of the first coordinate.
     * @param toY              high endpoints (exclusive) of the second coordinate.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>continuationMode</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=2</code>.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatrix(long[], long[])} method; for other cases &mdash;
     *                                   if <code>fromX&gt;toX</code> or <code>toX-fromX&gt;Long.MAX_VALUE</code>,
     *                                   or if <code>fromY&gt;toY</code> or <code>toY-fromY&gt;Long.MAX_VALUE</code>,
     *                                   or if the product <code>(toX-fromX)*(toY-fromY)</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    Matrix<T> subMatrix(long fromX, long fromY, long toX, long toY, ContinuationMode continuationMode);

    /**
     * Equivalent to <code>{@link #subMatrix(long[], long[], ContinuationMode)
     * subMatrix}(new long[]{fromX,fromY,fromZ}, new long[]{toX,toY,toZ}, continuationMode)</code>.
     * Note that this matrix must be 3-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param fromX            low endpoints (inclusive) of the first coordinate.
     * @param fromY            low endpoints (inclusive) of the second coordinate.
     * @param fromZ            low endpoints (inclusive) of the third coordinate.
     * @param toX              high endpoints (exclusive) of the first coordinate.
     * @param toY              high endpoints (exclusive) of the second coordinate.
     * @param toZ              high endpoints (exclusive) of the third coordinate.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>continuationMode</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=3</code>.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatrix(long[], long[])} method; for other cases &mdash;
     *                                   or if <code>fromY&gt;toY</code> or <code>toY-fromY&gt;Long.MAX_VALUE</code>,
     *                                   or if <code>fromZ&gt;toZ</code> or <code>toZ-fromZ&gt;Long.MAX_VALUE</code>,
     *                                   or if the product <code>(toX-fromX)*(toY-fromY)*(toZ-fromZ)</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    Matrix<T> subMatrix(
            long fromX, long fromY, long fromZ, long toX, long toY, long toZ,
            ContinuationMode continuationMode);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to)} method, where
     * <code>from[k]=position[k]</code> and <code>to[k]=position[k]+dimensions[k]</code> for all <code>k</code>.
     *
     * @param position   low endpoints (inclusive) of all coordinates.
     * @param dimensions dimensions of the returned submatrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>position</code> or <code>dimensions</code>
     *                                   argument is {@code null}.
     * @throws IllegalArgumentException  if <code>position.length</code> or <code>dimensions.length</code>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if, for some <code>k</code>,
     *                                   <code>position[k]&lt;0 || dimensions[k]&lt;0 ||
     *                                   position[k]+dimensions[k]&gt;{@link #dim(int) dim(k)}</code>.
     * @see #subMatr(long[], long[], ContinuationMode)
     */
    Matrix<T> subMatr(long[] position, long[] dimensions);

    /**
     * Equivalent to <code>{@link #subMatr(long[], long[])
     * subMatr}(new long[]{x,y}, new long[]{dimX,dimY})</code>.
     * Note that this matrix must be 2-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param x    low endpoint (inclusive) of the first coordinate.
     * @param y    low endpoint (inclusive) of the second coordinate.
     * @param dimX the first dimension of the returned submatrix.
     * @param dimY the second dimension of the returned submatrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=2</code>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatr(long[], long[])}.
     */
    Matrix<T> subMatr(long x, long y, long dimX, long dimY);

    /**
     * Equivalent to <code>{@link #subMatr(long[], long[])
     * subMatr}(new long[]{x,y,z}, new long[]{dimX,dimY,dimZ})</code>.
     * Note that this matrix must be 3-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param x    low endpoint (inclusive) of the first coordinate.
     * @param y    low endpoint (inclusive) of the second coordinate.
     * @param z    low endpoint (inclusive) of the third coordinate.
     * @param dimX the first dimension of the returned submatrix.
     * @param dimY the second dimension of the returned submatrix.
     * @param dimZ the third dimension of the returned submatrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=2</code>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatr(long[], long[])}.
     */
    Matrix<T> subMatr(long x, long y, long z, long dimX, long dimY, long dimZ);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to, ContinuationMode continuationMode)} method, where
     * <code>from[k]=position[k]</code> and <code>to[k]=position[k]+dimensions[k]</code> for all <code>k</code>.
     *
     * @param position         low endpoints (inclusive) of all coordinates.
     * @param dimensions       dimensions of the returned submatrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>position</code>, <code>dimensions</code>
     *                                   or <code>continuationMode</code>
     *                                   argument is {@code null}.
     * @throws IllegalArgumentException  if <code>position.length</code> or <code>dimensions.length</code>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatr(long[], long[])} method; for other cases &mdash;
     *                                   if, for some <code>k</code>, <code>dimensions[k]&lt;0</code>
     *                                   or <code>position[k]+dimensions[k]&gt;Long.MAX_VALUE</code>,
     *                                   or if the product of all <code>dimensions[k]</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     * @see #subMatr(long[], long[])
     */
    Matrix<T> subMatr(long[] position, long[] dimensions, ContinuationMode continuationMode);

    /**
     * Equivalent to <code>{@link #subMatr(long[], long[], ContinuationMode)
     * subMatr}(new long[]{x,y}, new long[]{dimX,dimY}, continuationMode)</code>.
     * Note that this matrix must be 2-dimensional
     * (in another case <code>IllegalArgumentException</code> will be thrown).
     *
     * @param x                low endpoint (inclusive) of the first coordinate.
     * @param y                low endpoint (inclusive) of the second coordinate.
     * @param dimX             the first dimension of the returned submatrix.
     * @param dimY             the second dimension of the returned submatrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>continuationMode</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=2</code>.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatr(long[], long[])} method; for other cases &mdash;
     *                                   if <code>dimX&lt;0</code>, <code>dimY&lt;0</code>,
     *                                   <code>x+dimX&gt;Long.MAX_VALUE</code>
     *                                   or <code>y+dimY&gt;Long.MAX_VALUE</code>,
     *                                   or if the product <code>dimX*dimY</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.,
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    Matrix<T> subMatr(long x, long y, long dimX, long dimY, ContinuationMode continuationMode);

    /**
     * Equivalent to <code>{@link #subMatr(long[], long[], ContinuationMode)
     * subMatr}(new long[]{x,y,z}, new long[]{dimX,dimY,dimZ}, continuationMode)</code>.
     * Note that this matrix must be 3-dimensional (otherwise, <code>IllegalArgumentException</code> will be thrown).
     *
     * @param x                low endpoint (inclusive) of the first coordinate.
     * @param y                low endpoint (inclusive) of the second coordinate.
     * @param z                low endpoint (inclusive) of the third coordinate.
     * @param dimX             the first dimension of the returned submatrix.
     * @param dimY             the second dimension of the returned submatrix.
     * @param dimZ             the third dimension of the returned submatrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <code>continuationMode</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>{@link #dimCount()}!=3</code>.
     * @throws IndexOutOfBoundsException for <code>continuationMode=={@link ContinuationMode#NONE}</code> &mdash;
     *                                   see {@link Matrix#subMatr(long[], long[])} method; for other cases &mdash;
     *                                   if <code>dimX&lt;0</code>, <code>dimY&lt;0</code>, <code>dimZ&lt;0</code>,
     *                                   <code>x+dimX&gt;Long.MAX_VALUE</code>, <code>y+dimY&gt;Long.MAX_VALUE</code>
     *                                   or <code>z+dimZ&gt;Long.MAX_VALUE</code>,
     *                                   or if the product <code>dimX*dimY*dimZ</code>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <code>Long.MAX_VALUE</code>.
     * @throws ClassCastException        if <code>continuationMode</code> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not {@code null} and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be cast to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    Matrix<T> subMatr(
            long x, long y, long z, long dimX, long dimY, long dimZ,
            ContinuationMode continuationMode);

    /**
     * Returns <code>true</code> if and only if this matrix is a {@link #subMatrix(long[], long[]) submatrix} of
     * some <code>parent</code> matrix, created by one of calls <code>parent.subMatrix(...)</code>,
     * <code>parent.subMatr(...)</code> or equivalent.
     * The {@link #subMatrixParent()} method throws {@link NotSubMatrixException}
     * if and only if this method returns <code>false</code>.
     *
     * @return whether this object is created by <code>subMatrix(...)</code>,
     * <code>subMatr(...)</code> or equivalent call.
     * @see #subMatrix(long[], long[])
     * @see #subMatrix(long[], long[], ContinuationMode)
     * @see #subMatr(long[], long[])
     * @see #subMatr(long[], long[], ContinuationMode)
     * @see #subMatrixParent()
     */
    boolean isSubMatrix();

    /**
     * If this matrix is a {@link #subMatrix(long[], long[]) submatrix} of some <code>parent</code> matrix,
     * created by one of calls <code>parent.subMatrix(...)</code> or <code>parent.subMatr(...)</code>,
     * returns a reference to the <code>parent</code> matrix instance.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * @return a reference to the parent matrix, if this instance is a submatrix.
     * @throws NotSubMatrixException if this object is not created by <code>subMatrix(...)</code>,
     *                               <code>subMatr(...)</code> or equivalent call.
     * @see #isSubMatrix()
     */
    Matrix<T> subMatrixParent() throws NotSubMatrixException;

    /**
     * If this matrix is a {@link #subMatrix(long[], long[]) submatrix} of some <code>parent</code> matrix,
     * created by one of calls <code>parent.subMatrix(...)</code> or <code>parent.subMatr(...)</code>,
     * creates and returns a new Java array containing the starting position of this submatrix
     * in the parent one. The result will be equal to "<code>from</code>" argument of
     * {@link #subMatrix(long[], long[])} and {@link #subMatrix(long[], long[], ContinuationMode)} methods.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * @return low endpoints (inclusive) of all coordinates of this submatrix in its parent matrix.
     * @throws NotSubMatrixException if this object is not created by <code>subMatrix(...)</code>,
     *                               <code>subMatr(...)</code> or equivalent call.
     * @see #isSubMatrix()
     */
    long[] subMatrixFrom() throws NotSubMatrixException;

    /**
     * If this matrix is a {@link #subMatrix(long[], long[]) submatrix} of some <code>parent</code> matrix,
     * created by one of calls <code>parent.subMatrix(...)</code> or <code>parent.subMatr(...)</code>,
     * creates and returns a new Java array containing the ending position (exclusive) of this submatrix
     * in the parent one. The result will be equal to "<code>to</code>" argument of
     * {@link #subMatrix(long[], long[])} and {@link #subMatrix(long[], long[], ContinuationMode)} methods.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * @return low endpoints (inclusive) of all coordinates of this submatrix in its parent matrix.
     * @throws NotSubMatrixException if this object is not created by <code>subMatrix(...)</code>,
     *                               <code>subMatr(...)</code> or equivalent call.
     * @see #isSubMatrix()
     */
    long[] subMatrixTo() throws NotSubMatrixException;

    /**
     * If this matrix is a {@link #subMatrix(long[], long[], Matrix.ContinuationMode) submatrix}
     * of some <code>parent</code> matrix,
     * created by one of calls <code>parent.subMatrix(...)</code> or <code>parent.subMatr(...)</code>,
     * returns the {@link ContinuationMode continuation mode}, used by this submatrix.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * <p>If the submatrix was created by
     * {@link #subMatrix(long[], long[], net.algart.arrays.Matrix.ContinuationMode)} or equivalent method,
     * the <code>continuationMode</code> argument, passed to that method, is returned.
     * If the submatrix was created by
     * {@link #subMatrix(long[], long[])} or equivalent method,
     * {@link ContinuationMode#NONE} constant is returned.
     *
     * @return low endpoints (inclusive) of all coordinates of this submatrix in its parent matrix.
     * @throws NotSubMatrixException if this object is not created by <code>subMatrix(...)</code>,
     *                               <code>subMatr(...)</code> or equivalent call.
     * @see #isSubMatrix()
     */
    ContinuationMode subMatrixContinuationMode() throws NotSubMatrixException;

    /**
     * Returns a view ot this matrix, where the elements are reordered in some order "like"
     * in the specified matrix <code>m</code>.
     * In other words, the elements of the {@link #array() built-in array} of the returned matrix are
     * the same as the elements of the {@link #array() built-in array} of this one
     * (any changes of the elements in the returned matrix are reflected in this matrix, and vice versa),
     * but the order of the elements can differ. The precise algorithm of reordering is not specified
     * and depends on the matrix <code>m</code>: this method tries to help algorithms, processing the same
     * or similar areas in both matrices, to provide maximal performance.
     *
     * <p>This method returns non-trivial results only if the matrix <code>m</code> is already a view of some other
     * matrix with some form of reordering elements, for example, if <code>m</code> is a {@link #isTiled() tiled}
     * matrix.
     * In another case, this method just returns this instance.
     *
     * <p>In the current version of this package (if this instance was created by methods of this package),
     * this method is equivalent to the following:
     *
     * <pre>
     * m.{@link #isTiled()} ?
     * &#32;   thisInstance.{@link #tile() tile}(m.{@link #tileDimensions()}) :
     * &#32;   thisInstance;
     * </pre>
     *
     * <p>In future versions, it is possible that this method will recognize other forms of reordering matrix elements
     * and return non-trivial results for such <code>m</code> matrices.
     *
     * <p>Because the precise order of elements of the returning matrix is not specified, we recommend to use
     * this method generally for newly created matrices, for example:
     *
     * <pre>
     * memoryModel.{@link MemoryModel#newMatrix(Class, Matrix)
     * newMatrix}({@link UpdatablePArray}.class, m).{@link #structureLike(Matrix) structureLike}(m);
     * </pre>
     * or, more briefly,
     * <pre>
     * memoryModel.{@link MemoryModel#newStructuredMatrix(Class, Matrix)
     * newStructuredMatrix}({@link UpdatablePArray}.class, m);
     * </pre>
     *
     * @param m some matrix, probably a view of another matrix with reordered elements
     *          (for example, {@link #tile(long...) tiled}).
     * @return a view of this matrix with elements reordered in similar order, or a reference to this instance
     * if <code>m</code> matrix is not reodered or this method does not "know" about the way of that reordering.
     * @throws NullPointerException if <code>m</code> argument is {@code null}.
     * @see #isStructuredLike(Matrix)
     */
    Matrix<T> structureLike(Matrix<?> m);

    /**
     * Returns <code>true</code> if the elements of this matrix is ordered "alike" the elements
     * of the specified matrix <code>m</code>, in terms of {@link #structureLike(Matrix)} method.
     * "Ordered alike" does not mean that the dimensions of both matrices are equal, or that
     * the details of the structure are the same; it means only that both matrices use similar
     * reordering algorithms.
     *
     * <p>More precisely, {@link #structureLike(Matrix)} method returns this instance if and only if
     * this method returns <code>true</code>.
     *
     * <p>In the current version of this package (if this instance was created by means of methods of this package),
     * this method is equivalent to: <code>thisInstance.{@link #isTiled()}==m.{@link #isTiled()}</code>.
     *
     * @param m some matrix, probably a view of another matrix with reordered elements
     *          (for example, {@link #tile(long...) tiled}).
     * @return whether this matrix is reordered alike <code>m</code>.
     * @throws NullPointerException if <code>m</code> argument is {@code null}.
     */
    boolean isStructuredLike(Matrix<?> m);

    /**
     * Returns a view ot this matrix, where the elements are reordered by <i>tiles</i>: a grid of rectangular
     * regions (<i>tiles</i>), the sizes of which are specified by <code>tileDim</code> argument.
     * It means that the elements of the built-in AlgART array of the returned matrix are the elements
     * of the built-in array of this one, but "shuffled" so that all elements of every tile in the returned matrix
     * are located in a continuous block of the built-in array of this matrix.
     * The returned matrix is named <i>tiled matrix</i>. The {@link #dimensions() dimensions}
     * of the returned matrix are the same as the dimensions of this one.
     * The {@link #elementType() element type} of the returned matrix is identical to the element type
     * of this matrix.
     *
     * <p>More precisely, let this matrix be <b>M</b> and the tiled matrix, returned by this method, be <b>T</b>.
     * Let <i>i<sub>0</sub></i>, <i>i<sub>1</sub></i>, ..., <i>i<sub>n&minus;1</sub></i>
     * (<i>n</i><code>={@link #dimCount()}</code>) be coordinates of some element it the tiled matrix <b>T</b>,
     * that is located in <b>T</b><code>.{@link #array() array()}</code>
     * at the index <i>i</i>=<b>T</b><code>.{@link #index(long...)
     * index}</code>(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>).
     * This element is located in the original array <b>M</b><code>.{@link #array() array()}</code> at another index
     * <i>j</i>, which is calculated by the following algorithm.
     *
     * <ol>
     * <li>Let <i>d<sub>k</sub></i> = <b>M</b>.{@link #dim(int) dim}(<i>k</i>),
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: dimensions of this and returned matrix.</li>
     *
     * <li>Let <i>i'<sub>k</sub></i> = <i>i<sub>k</sub></i>%<code>tileDim[<i>k</i>]</code>,
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: <i>i'<sub>k</sub></i> are the coordinates of this element inside
     * the tile, containing it in <b>T</b> matrix.</li>
     *
     * <li>Let <i>s<sub>k</sub></i> = <i>i<sub>k</sub></i>&minus;<i>i'<sub>k</sub></i>,
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: <i>s<sub>k</sub></i> are the coordinates of the starting element
     * of the tile, containing this element in <b>T</b> matrix.</li>
     *
     * <li>Let <i>t<sub>k</sub></i> = <code>min</code>(<code>tileDim[</code><i>k</i><code>]</code>,
     * <i>d<sub>k</sub></i>&minus;<i>s<sub>k</sub></i>),
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: <i>t<sub>k</sub></i> are the dimensions
     * of the tile, containing this element in <b>T</b> matrix. (Note that boundary tiles can be less
     * than <code>tileDim</code>, if dimensions of matrix are not divisible by corresponding dimensions of tiles.)</li>
     *
     * <li>Let <code>previousVolume</code> =
     * <i>d</i><sub>0</sub><i>d</i><sub>1</sub>...<i>d</i><sub><i>n</i>&minus;3</sub><i
     * class="dummy">d</i><sub><i>n</i>&minus;2</sub><i>s</i><sub><i>n</i>&minus;1</sub>
     * + <i>d</i><sub>0</sub><i>d</i><sub>1</sub>...<i>d</i><sub><i>n</i>&minus;3</sub><i
     * class="dummy">c</i><sub><i>n</i>&minus;2</sub><i>t</i><sub><i>n</i>&minus;1</sub>
     * + ... + <i>s</i><sub>0</sub><i>t</i><sub>1</sub>...<i
     * class="dummy">t</i><sub><i>n</i>&minus;2</sub><i>t</i><sub><i>n</i>&minus;1</sub>.
     * This complex formula returns the summary sizes of all tiles, that are fully located
     * in the source <b>T</b><code>.{@link #array() array()}</code> before the given element.
     * In 2-dimensional case, the formula is more simple:
     * <code>previousVolume</code> = <i>d</i><sub><i>x</i></sub><i>s</i><sub><i>y</i></sub>
     * + <i>s</i><sub><i>x</i></sub><i>t</i><sub><i>y</i></sub>.
     * </li>
     *
     * <li>Let <code>indexInTile</code> =
     * <i>i'</i><sub>0</sub> + <i>i'</i><sub>1</sub><i>t</i><sub>0</sub> + ...
     * + <i>i'</i><sub><i>n</i>&minus;1</sub><i>t</i><sub><i>n</i>&minus;2</sub>...<i>t</i><sub>0</sub>:
     * it is the index of the element with coordinates
     * <i>i'<sub>0</sub></i>,<i>i'<sub>1</sub></i>,...,<i>i'<sub>n&minus;1</sub></i>
     * in the built-in array of a little matrix, dimensions of which are equal to the tile dimensions.
     * </li>
     *
     * <li>The required index of the given element in the original array
     * <b>M</b><code>.{@link #array() array()}</code>
     * is <i>j</i> = <code>previousVolume</code> + <code>indexInTile</code>.</li>
     * </ol>
     *
     * <p>Tiled matrices are necessary to provide good performance of many algorithms, if this matrix is very large
     * (much greater than amount of RAM) and is located on disk or other external devices.
     * For example, extracting a rectangular area 1000x1000 from a byte matrix 1000000x1000000 (1&nbsp;terabyte)
     * will probably work much faster if it is tiled, than if it is a usual matrix, where every line
     * occupies 1 MB of continuous disk space.
     *
     * <p>In the degenerated case of 1-dimensional matrix ({@link #dimCount()}=1)
     * the tiled matrix is absolutely useless, though still works correctly.
     *
     * <p>Recommended tile dimensions are from several hundreds to several thousands, but it depends
     * on the number of dimensions. If tile dimensions are degrees of two (2<sup><i>k</i></sup>),
     * the tiled matrix will probably work faster.
     *
     * <p>The built-in AlgART array of the returned matrix is backed by the built-in array of this matrix,
     * so &mdash; if this matrix is not {@link #isImmutable() immutable}
     * &mdash; any changes of the elements of the returned matrix are reflected in this matrix, and vice versa.
     * The returned matrix is {@link #isImmutable() immutable} if, and only if,
     * the built-in array of this matrix does not implement {@link UpdatableArray}.
     * The {@link Array#asTrustedImmutable()} method
     * in the built-in array of the returned matrix is equivalent to {@link Array#asImmutable()},
     * and {@link Array#asCopyOnNextWrite()} method just returns the full copy of the array.
     *
     * @param tileDim dimensions of the tiles in the returned matrix (excepting the boundary tiles,
     *                which can be less).
     * @return a tiled view of this matrix.
     * @throws NullPointerException     if <code>tileDim</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>tileDim.length</code> is not equal to {@link #dimCount()},
     *                                  or if some <code>tileDim[k]&lt;=0</code>,
     *                                  or if the product of all tile dimensions <code>tileDim[k]</code>
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @see #tile()
     * @see #isTiled()
     * @see #tileDimensions()
     */
    Matrix<T> tile(long... tileDim);

    /**
     * Returns a tiled view ot this matrix with some default dimensions of the tiles.
     * Equivalent to <code>{@link #tile(long...) tile}(tileDim)</code>, where all elements of <code>tileDim</code>
     * are equal to the default integer value, retrieved from the system property
     * "<code>net.algart.arrays.matrixTile2D</code>",
     * "<code>net.algart.arrays.matrixTile3D</code>"
     * "<code>net.algart.arrays.matrixTile4D</code>"
     * "<code>net.algart.arrays.matrixTile5D</code>" or
     * "<code>net.algart.arrays.matrixTileND</code>",
     * if the {@link #dimCount() number of dimensions} of this matrix is correspondingly 2, 3, 4, 5 or greater.
     * If there is no such property, or if it contains not a number,
     * or if some exception occurred while calling <code>Long.getLong</code>,
     * this method uses the following tile dimensions:
     * 4096x4096 in 2-dimensional case,
     * 256x256x256 in 3-dimensional case,
     * 64x64x64x64 in 4-dimensional case,
     * 32x32x32x32x32 in 5-dimensional case,
     * 16x16x... if the number of dimensions is greater than 5.
     * If the corresponding property exists and contains a valid integer number,
     * but it is too small, in particular, zero or negative, then it is replaced with some minimal positive value.
     * The values of all these system property is loaded and checked only once
     * while initializing {@link Arrays} class.
     * If the number of dimensions is 1 (degenerated case), this method always uses 65536 as the tile size.
     * (<i>Warning</i>! These defaults can be changed in future versions!)
     *
     * @return a tiled view of this matrix with default tile dimensions.
     * @throws IllegalArgumentException if the product of all tile dimensions <code>tileDim[k]</code>
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @see #tile(long...)
     * @see #isTiled()
     * @see #tileDimensions()
     */
    Matrix<T> tile();

    /**
     * Returns <code>true</code> if and only if this matrix is a {@link #tile(long...) tiled view}
     * of some <code>parent</code> matrix, created by a call <code>parent.tile(...)</code> or an equivalent call.
     * The {@link #tileParent()} method throws {@link NotSubMatrixException}
     * if and only if this method returns <code>false</code>.
     *
     * @return whether this object is created by <code>tile(...)</code> or equivalent call.
     * @see #tile(long...)
     * @see #tile()
     */
    boolean isTiled();

    /**
     * If this matrix is a {@link #tile(long...) tiled view} of some <code>parent</code> matrix,
     * created by a call <code>parent.tile(...)</code>,
     * returns a reference to the <code>parent</code> matrix instance.
     * If this matrix is not a tiled view, throws {@link NotTiledMatrixException}.
     *
     * @return a reference to the parent matrix, if this instance is a tiled view of other matrix.
     * @throws NotTiledMatrixException if this object is not created by <code>tile(...)</code> or equivalent call.
     * @see #isTiled()
     */
    Matrix<T> tileParent() throws NotTiledMatrixException;

    /**
     * If this matrix is a {@link #tile(long...) tiled view} of some <code>parent</code> matrix,
     * created by a call <code>parent.tile(...)</code>,
     * creates and returns a new Java array containing the tile dimensions, used while creating this tiled view
     * (argument of {@link #tile(long...)} method).
     * If this matrix is not a tiled view, throws {@link NotTiledMatrixException}.
     *
     * @return sizes of each tile, if this instance is a tiled view of other matrix.
     * @throws NotTiledMatrixException if this object is not created by <code>tile(...)</code> or equivalent call.
     * @see #isTiled()
     */
    long[] tileDimensions() throws NotTiledMatrixException;

    /**
     * Equivalent to <code>{@link Matrices#asLayers(Matrix) Matrices.asLayers}(thisMatrix)</code>.
     *
     * @return a list of matrices: "layers" of this matrix one along the last dimension.
     * @throws IllegalStateException if this matrix is 1-dimensional.
     */
    default List<Matrix<T>> asLayers() {
        return Matrices.asLayers(this);
    }

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#isImmutable() isImmutable()}</code>.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @return <code>true</code> if this instance is immutable.
     */
    boolean isImmutable();

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#isCopyOnNextWrite() isCopyOnNextWrite()}</code>.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @return <code>true</code> if this instance is copy-on-next-write.
     */
    boolean isCopyOnNextWrite();

    /**
     * Returns <code>true</code> if and only if the built-in AlgART array implements {@link DirectAccessible}
     * interface and <code>(({@link DirectAccessible}){@link #array()}).{@link DirectAccessible#hasJavaArray()
     * hasJavaArray()}</code> method returns <code>true</code>.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @return whether this matrix can be viewed as a Java array or a part of Java array.
     */
    boolean isDirectAccessible();

    /**
     * Equivalent to {@link #clone(ArrayContext) clone(null)}.
     * <p>Note: <code>null</code> context provides the fastest multithreading copying.
     * If you don't want to use multithreading,
     * please pass {@link ArrayContext#DEFAULT_SINGLE_THREAD} as an argument.
     *
     * @return an exact clone of the passed matrix.
     */
    Matrix<T> clone();

    /**
     * Returns an exact clone of this matrix, created in a memory model, returned by the specified context,
     * or in {@link SimpleMemoryModel} for <code>null</code> context.
     *
     * <p>Equivalent to the following operators:
     * <pre>
     *     MemoryModel memoryModel = context == null ? Arrays.SMM : context.getMemoryModel();
     *     final Matrix&lt;UpdatableArray&gt; result = memoryModel.{@link MemoryModel#newMatrix(Class, Matrix)
     *     newMatrix}(UpdatableArray.class, thisInstance);
     *     {@link Matrices#copy(ArrayContext, Matrix, Matrix)
     *     Matrices.copy}(context, result, thisInstance);
     *     (return result)
     * </pre>
     *
     * @return an exact clone of the passed matrix.
     * @see #clone()
     * @see Matrices#clone(MemoryModel, Matrix)
     */
    Matrix<T> clone(ArrayContext arrayContext);

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#flushResources(ArrayContext) flushResources(context)}</code>.
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     */
    void flushResources(ArrayContext context);

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#freeResources(ArrayContext) freeResources(context)}</code>.
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     */
    void freeResources(ArrayContext context);

    /**
     * Equivalent to <code>{@link #array()}.{@link Array#freeResources() freeResources()}</code>.
     */
    void freeResources();

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * a short description of the built-in AlgART array and all matrix dimensions.
     *
     * @return a brief string description of this object.
     */
    String toString();

    /**
     * Returns the hash code of this matrix. The result depends on all elements of the built-in array
     * (as {@link Array#hashCode()}) and all matrix dimensions.
     *
     * @return the hash code of this matrix.
     */
    int hashCode();

    /**
     * Indicates whether some other matrix is equal to this one.
     * Returns <code>true</code> if and only if:<ol>
     * <li>the specified object is a matrix (i.e. implements {@link Matrix}),</li>
     * <li>both matrices have the same dimension count ({@link #dimCount()})
     * and the same corresponding dimensions;</li>
     * <li>the built-in AlgART arrays ({@link #array()}) are equal (see {@link Array#equals(Object)}).</li>
     * </ol>
     *
     * @param obj the object to be compared for equality with this matrix.
     * @return <code>true</code> if the specified object is a matrix equal to this one.
     */
    boolean equals(Object obj);

    /**
     * Equivalent to <code>{@link MemoryModel#newMatrix(Class, Class, long...)
     * memoryModel.newMatrix(UpdatablePArray.class, elementType, dim)}</code>. Throws
     * <code>IllegalArgumentException</code> in a case when the element type is not primitive.
     *
     * @param memoryModel the memory model, used for allocation new matrix.
     * @param elementType the type of matrix elements.
     * @param dim         the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if one of the arguments is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not a primitive class,
     *                                         or if the specified dimensions are incorrect:
     *                                         <code>dim.length==0</code>,
     *                                         <code>dim[n] &lt; 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatablePArray> newMatrix(MemoryModel memoryModel, Class<?> elementType, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(elementType, "Null element type");
        if (!elementType.isPrimitive()) {
            throw new IllegalArgumentException("Not a primitive type: " + elementType);
        }
        return memoryModel.newMatrix(UpdatablePArray.class, elementType, dim);
    }

    /**
     * Equivalent to <code>{@link #newMatrix(MemoryModel, Class, long...)
     * newMatrix}({@link Arrays#SMM Arrays.SMM}, elementType, dim)</code>.
     *
     * @param elementType the type of matrix elements.
     * @param dim         the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>elementType</code> is not a primitive class,
     *                                  or if the specified dimensions are incorrect:
     *                                  <code>dim.length==0</code>,
     *                                  <code>dim[n] &lt; 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatablePArray> newMatrix(Class<?> elementType, long... dim) {
        return newMatrix(Arrays.SMM, elementType, dim);
    }

    /*Repeat() boolean ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
               bit     ==> char,,byte,,short,,int,,long,,float,,double */

    /**
     * Equivalent to <code>{@link MemoryModel#newBitMatrix(long...) memoryModel.newBitMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>boolean</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableBitArray> newBitMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newBitMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newBitMatrix(long...)
     * newBitMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableBitArray> newBitMatrix(long... dim) {
        return Arrays.SMM.newBitMatrix(dim);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <code>{@link MemoryModel#newCharMatrix(long...) memoryModel.newCharMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>char</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableCharArray> newCharMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newCharMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newCharMatrix(long...)
     * newCharMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableCharArray> newCharMatrix(long... dim) {
        return Arrays.SMM.newCharMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newByteMatrix(long...) memoryModel.newByteMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>byte</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableByteArray> newByteMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newByteMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newByteMatrix(long...)
     * newByteMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableByteArray> newByteMatrix(long... dim) {
        return Arrays.SMM.newByteMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newShortMatrix(long...) memoryModel.newShortMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>short</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableShortArray> newShortMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newShortMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newShortMatrix(long...)
     * newShortMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableShortArray> newShortMatrix(long... dim) {
        return Arrays.SMM.newShortMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newIntMatrix(long...) memoryModel.newIntMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>int</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableIntArray> newIntMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newIntMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newIntMatrix(long...)
     * newIntMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableIntArray> newIntMatrix(long... dim) {
        return Arrays.SMM.newIntMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newLongMatrix(long...) memoryModel.newLongMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>long</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableLongArray> newLongMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newLongMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newLongMatrix(long...)
     * newLongMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableLongArray> newLongMatrix(long... dim) {
        return Arrays.SMM.newLongMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newFloatMatrix(long...) memoryModel.newFloatMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>float</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableFloatArray> newFloatMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newFloatMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newFloatMatrix(long...)
     * newFloatMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableFloatArray> newFloatMatrix(long... dim) {
        return Arrays.SMM.newFloatMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link MemoryModel#newDoubleMatrix(long...) memoryModel.newDoubleMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException            if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException        if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                         <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                         or the product of all specified dimensions
     *                                         is greater than <code>Long.MAX_VALUE</code>.
     * @throws UnsupportedElementTypeException if <code>double</code> element type
     *                                         is not supported by this memory model.
     * @throws TooLargeArrayException          if the product of all specified dimensions is too large
     *                                         for this memory model.
     */
    static Matrix<UpdatableDoubleArray> newDoubleMatrix(MemoryModel memoryModel, long... dim) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        return memoryModel.newDoubleMatrix(dim);
    }

    /**
     * Equivalent to <code>{@link Arrays#SMM Arrays.SMM}.{@link MemoryModel#newDoubleMatrix(long...)
     * newDoubleMatrix(dim)}</code>.
     *
     * @param dim the dimensions of the matrix.
     * @return created matrix.
     * @throws NullPointerException     if <code>dim</code> is {@code null}.
     * @throws IllegalArgumentException if the specified dimensions are incorrect: <code>dim.length == 0</code>,
     *                                  <code>dim[n] &lt;= 0</code> for some <code>n</code>,
     *                                  or the product of all specified dimensions
     *                                  is greater than <code>Long.MAX_VALUE</code>.
     * @throws TooLargeArrayException   if the product of all specified dimensions is too large
     *                                  for {@link SimpleMemoryModel}.
     */
    static Matrix<UpdatableDoubleArray> newDoubleMatrix(long... dim) {
        return Arrays.SMM.newDoubleMatrix(dim);
    }
    /*Repeat.AutoGeneratedEnd*/


    /**
     * Equivalent to <code>{@link SimpleMemoryModel#asMatrix(Object, long...)}
     * SimpleMemoryModel.asMatrix}(array, dim)</code>.
     *
     * @param array the source Java array.
     * @param dim   the matrix dimensions.
     * @return a matrix backed by the specified Java array with the specified dimensions.
     * @throws NullPointerException     if <code>array</code> or <code>dim</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>array</code> argument is not a Java array
     *                                  and is not {@link UpdatablePArray},
     *                                  or if it is <code>boolean[]</code> array, or array of objects,
     *                                  or if the number of dimensions is 0 (empty <code>dim</code> Java array),
     *                                  or if some of the dimensions are negative.
     * @throws SizeMismatchException    if the product of all dimensions is not equal to the array length.
     * @throws TooLargeArrayException   if the product of all dimensions is greater than <code>Long.MAX_VALUE</code>.
     */
    static Matrix<UpdatablePArray> as(Object array, long... dim) {
        return SimpleMemoryModel.asMatrix(array, dim);
    }
}
