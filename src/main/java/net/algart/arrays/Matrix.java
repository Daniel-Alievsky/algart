/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.math.*;

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
 * <li>the <i>set of dimensions</i>: a little usual array of integers &mdash; <tt>long[] dim</tt>,
 * describing the sizes of the multidimensional matrix in every dimension.</li>
 * </ol>
 *
 * <p>The product of all dimensions must be equal to the array length. Moreover,
 * the array must be {@link UpdatableArray#asUnresizable() unresizable}: so, the array length
 * cannot be changed after creating the matrix.</p>
 *
 * <p>It is supposed that all matrix elements are stored in the built-in AlgART array.
 * The storing scheme is traditional. For 2D matrix, the matrix element <tt>(x,y)</tt>
 * is stored at the position <tt>y*dim[0]+x</tt> of the array (<tt>dim[0]</tt> is the first
 * matrix dimension: the "width"). For 3D matrix, the matrix element <tt>(x,y,z)</tt>
 * is stored at the position <tt>z*dim[1]*dim[0]+y*dim[0]+x</tt> (<tt>dim[0]</tt> is the
 * x-dimension, dim[1] is the y-dimension). In the common case, the element of <tt>n</tt>-dimensional matrix
 * with coordinates <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i> is stored
 * in the built-in array at the position</p>
 *
 * <blockquote>
 * <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
 * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
 * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
 * </blockquote>
 *
 * <p>where <i>d<sub>k</sub></i><tt>=dim[<i>k</i>]</tt> (<tt><i>k</i>=0,1,...,<i>n</i>-1</tt>)
 * is the dimension <tt>#<i>k</i></tt>.</p>
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
 * {@link #dim(int) dim(n)}, <tt>dim</tt> argument in {@link MemoryModel#newMatrix(Class, Class, long...)
 * MemoryModel.newMatrix}, etc.) are ordered from the <i>lowest</i> index to the <i>highest</i>.
 * Please compare: for numeric matrix <tt>m</tt>, <nobr><tt>m.array().getDouble(m.index(15,10))</tt></nobr>
 * returns the element <tt>#15</tt> of the row <tt>#10</tt>. However, for usual <nobr>2-dimensional</nobr> Java array,
 * declared as "<nobr><tt>double[][] a</tt></nobr>", the same element is accessed as
 * <nobr><tt>a[10][15]</tt></nobr>!</p>
 *
 * <p>Second, the number of indexes in the {@link #index(long...) index} method
 * may <i>differ</i> from the number of dimensions ({@link #dimCount()}).
 * In any case, the returned position in calculated by the formula listed above
 * (<nobr><i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
 * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
 * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i></nobr>),
 * where <i>i<sub>0</sub></i>, <i>i<sub>2</sub></i>, ..., <i>i<sub>n-1</sub></i>
 * are the coordinates passed to the method, and <i>d<sub>k</sub></i> is the dimension <tt>#<i>k</i></tt>
 * or 1 if <tt><i>k</i>&gt;={@link #dimCount()}</tt>.</p>
 * In other words, it is supposed that all dimensions "after" the actual number of dimensions
 * are always equal to 1. For example, the one-dimensional matrix with <tt>L</tt> elements
 * can be interpreted as <nobr>2-dimensional</nobr> <tt>Lx1</tt> matrix, or 3-dimensional <tt>Lx1x1</tt> one, etc.</p>
 *
 * <p>The matrix object is <b>immutable</b>, that means that there are no ways to change
 * any dimension or the reference to the built-in AlgART array.
 * But the matrix elements can be modified, if the AlgART array is not
 * {@link Array#asImmutable() immutable}.
 * So, the matrix object is <b>thread-safe</b> or <b>thread-compatible</b>
 * in the same situations as the built-in AlgART array: see comments to {@link Array} interface.</p>
 *
 * <p>The generic argument <tt>T</tt> specifies the type of the built-in AlgART array.
 * Any array type can be declared here, but the contract of this interface
 * requires that the array must be {@link UpdatableArray#asUnresizable() unresizable}.
 * So, there are no ways to create a matrix with {@link MutableArray} (or its subinterface)
 * as the type argument, alike <tt>Matrix&lt;MutableByteArray&gt;</tt>:
 * all creation methods throw <tt>IllegalArgumentException</tt> in this case.
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
     * if some matrix dimension <tt>{@link Matrix#dim(int) dim(k)}==0</tt>, then the corresponding
     * coordinate range of a submatrix must be <tt>0..0</tt>, as for {@link #NONE} continuation mode.
     * See more details in comments to {@link Matrix#subMatrix(long[], long[], ContinuationMode)} method.
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.
     * Moreover, the constants {@link #NONE}, {@link #CYCLIC}, {@link #PSEUDO_CYCLIC}, {@link #MIRROR_CYCLIC},
     * {@link #NULL_CONSTANT}, {@link #ZERO_CONSTANT}, {@link #NAN_CONSTANT},
     * as well as constants in standard Java enumerations, are unique instances, which cannot be equal to any other
     * instance of this class. So, you can use <tt>==</tt> Java operator to compare objects with these constants,
     * instead of calling {@link #equals(Object)} method of this class.</p>
     */
    public static class ContinuationMode {

        /**
         * Simplest continuation mode: any continuation outside the source matrix is disabled.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
         * always corresponds to the element of the source matrix <tt>m</tt>
         * with the coordinates
         * <nobr><tt><i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i></tt></nobr>,
         * where <tt><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></tt>
         * are the low endpoints of all coordinates of the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method).
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <tt>m</tt>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <tt>m</tt>.
         *
         * <p>In a case of this mode, {@link Matrix#subMatrix(long[], long[], ContinuationMode continuationMode)}
         * method is strictly equivalent to more simple {@link Matrix#subMatrix(long[], long[])}, and
         * {@link Matrix#subMatr(long[], long[], ContinuationMode continuationMode)}
         * is strictly equivalent to more simple {@link Matrix#subMatr(long[], long[])}.
         * In other words, all submatrix elements must lie inside the original matrix,
         * i.e. the returned matrix must be a true sub-matrix of the original one.
         * An attempt to create a submatrix with this continuation mode,
         * which does not lie fully inside the original matrix, leads to <tt>IndexOutOfBoundsException</tt>.
         */
        public static ContinuationMode NONE = new ContinuationMode("not-continued mode");

        /**
         * The <i>cyclic</i> (or <i>true-cyclic</i>) continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
         * corresponds to the element of the built-in array
         * <tt>m.{@link Matrix#array() array()}</tt> of the source matrix <tt>m</tt>
         * with the index <nobr><tt>m.{@link Matrix#cyclicIndex(long...)
         * cyclicIndex}(<i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i>)</tt></nobr>,
         * where <tt><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></tt>
         * are the low endpoints of all coordinates of the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <tt>m</tt>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <tt>m</tt>.
         *
         * <p>In other words, in this mode you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite periodical repeating along all coordinate axes.
         */
        public static ContinuationMode CYCLIC = new ContinuationMode("cyclically-continued mode");

        /**
         * The <i>pseudo-cyclic</i> (or <i>toroidal</i>) continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
         * corresponds to the element of the built-in array
         * <tt>m.{@link Matrix#array() array()}</tt> of the source matrix <tt>m</tt>
         * with the index <nobr><tt>m.{@link Matrix#pseudoCyclicIndex(long...)
         * pseudoCyclicIndex}(<i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i>)</tt></nobr>,
         * where <tt><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></tt>
         * are the low endpoints of all coordinates of the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <tt>m</tt>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <tt>m</tt>.
         *
         * <p>In other words, in this mode you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite periodical repeating its {@link Matrix#array() built-in array}.
         * It is the most natural mode for many image processing algorithms,
         * which work directly with the built-in array instead of working with coordinates of matrix elements.
         */
        public static ContinuationMode PSEUDO_CYCLIC = new ContinuationMode("pseudo-cyclically-continued mode");

        /**
         * The <i>mirror-cyclic</i> continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
         * corresponds to the element of the built-in array
         * <tt>m.{@link Matrix#array() array()}</tt> of the source matrix <tt>m</tt>
         * with the index <nobr><tt>m.{@link Matrix#mirrorCyclicIndex(long...)
         * mirrorCyclicIndex}(<i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i>)</tt></nobr>,
         * where <tt><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></tt>
         * are the low endpoints of all coordinates of the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method.
         * An attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <tt>m</tt>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <tt>m</tt>.
         *
         * <p>In other words, in this mode you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite periodical repeating along all coordinate axes, if, while every "odd" repeating,
         * the matrix is symmetrically reflected along the corresponding coordinate.
         * In other words, it's possible to say that the matrix is infinitely reflected in each its bound as
         * in a mirror. Usually this mode provides the best smoothness of continuation of the matrix.
         */
        public static ContinuationMode MIRROR_CYCLIC = new ContinuationMode("mirroring-cyclically-continued mode");

        /**
         * The special case of constant continuation mode, corresponding to continuing by <tt>null</tt>
         * constant. Strictly equivalent to {@link #getConstantMode(Object) getConstantMode(null)}
         * (such a call always returns the reference to this constant).
         *
         * <p>Note: unlike {@link #ZERO_CONSTANT}, this mode can be used with any element type of the original matrix,
         * including non-primitive objects. For matrices with primitive element type, this mode is equivalent
         * to {@link #ZERO_CONSTANT}.
         */
        public static ContinuationMode NULL_CONSTANT = new ConstantImpl(null);

        /**
         * The special popular case of constant continuation mode, corresponding to continuing by <tt>0.0d</tt>
         * numeric constant. Strictly equivalent to
         * <nobr>{@link #getConstantMode(Object) getConstantMode(new Double(0.0d))}</nobr>
         * (such a call always returns the reference to this constant).
         *
         * <p>Note: unlike {@link #NULL_CONSTANT}, this mode can be used only with matrices, containing elements of
         * some primitive type, i.e. with <nobr><tt>{@link Matrix}&lt;? extends {@link PArray}&gt;</tt></nobr>.
         */
        public static ContinuationMode ZERO_CONSTANT = new ConstantImpl(0.0d);

        /**
         * The special popular case of constant continuation mode, corresponding to continuing by
         * <tt>Double.NaN</tt> numeric constant. Strictly equivalent to
         * <nobr>{@link #getConstantMode(Object) getConstantMode(new Double(Double.NaN))}</nobr>
         * (such a call always returns the reference to this constant).
         *
         * <p>Note: unlike {@link #NULL_CONSTANT}, this mode can be used only with matrices, containing elements of
         * some primitive type, i.e. with <nobr><tt>{@link Matrix}&lt;? extends {@link PArray}&gt;</tt></nobr>.
         */
        public static ContinuationMode NAN_CONSTANT = new ConstantImpl(Double.NaN);

        /**
         * Creates an instance of this class for <i>constant</i> continuation mode.
         *
         * <p>In this mode, the element of the returned submatrix with coordinates
         * <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
         * corresponds to the element of the source matrix <tt>m</tt>
         * with the coordinates
         * <nobr><tt><i>p<sub>0</sub></i>+<i>i<sub>0</sub></i>,<i>p<sub>1</sub></i>+<i>i<sub>1</sub></i>,
         * ..., <i>p<sub>n-1</sub></i>+<i>i<sub>n-1</sub></i></tt></nobr>
         * (where <nobr><tt><i>p<sub>0</sub></i>,<i>p<sub>1</sub></i>,...,<i>p<sub>n-1</sub></i></tt></nobr>
         * are the low endpoints of all coordinates of the submatrix,
         * passed as the first argument of {@link Matrix#subMatrix(long[], long[], ContinuationMode)}
         * or {@link Matrix#subMatr(long[], long[], ContinuationMode)} method) &mdash;
         * if this element {@link Matrix#inside(long...) lies inside} the source matrix.
         * In this case, an attempt to read this element of the submatrix returns the corresponding element
         * of the source matrix <tt>m</tt>,
         * and an attempt to write into this element of the submatrix modifies the corresponding element
         * of the source matrix <tt>m</tt>.
         * In other case (if this element lies outside the source matrix),
         * the element is considered to be equal <tt>continuationConstant</tt> (an argument of this method):
         * an attempt to read it returns this constant, and
         * an attempt to write into this element is just ignored.
         *
         * <p>In other words, in this mode, you can consider that the resulting matrix
         * is a submatrix of an infinite "matrix", which is come out from the original matrix
         * by infinite appending it along all coordinates with the specified continuation constant.
         *
         * <p>The argument <tt>continuationConstant</tt> of this method is automatically cast to the type of
         * elements of the source matrix <tt>m</tt> according the following rules.
         *
         * <p>For non-primitive element types, the <tt>continuationConstant</tt> argument
         * must be some instance of the class <nobr><tt>m.{@link #elementType()}</tt></nobr>,
         * or its superclass, or <tt>null</tt>.
         * So, the type cast is trivial here.
         *
         * <p>For primitive element types, <tt>continuationConstant</tt> may be <tt>null</tt> or <i>any</i>
         * wrapper for primitive types: <tt>Boolean</tt>, <tt>Character</tt>, <tt>Byte</tt>, <tt>Short</tt>,
         * <tt>Integer</tt>, <tt>Long</tt>, <tt>Float</tt>, <tt>Double</tt>. In this case,
         * the following casting rules are used while reading elements (I remind that attempts to write
         * outside the original matrix are ignored),
         * depending on the primitive type <nobr><tt>m.{@link #elementType()}</tt></nobr>:
         *
         * <ul>
         * <li><tt>null</tt> is converted to <tt>false</tt> for bit elements or to zero (<tt>0</tt>,
         * <tt>(char)0</tt>, <tt>0.0</tt>) for all other element types
         * (so, it is the only universal continuation constant, which can be used with any element type:
         * see {@link #NULL_CONSTANT});</li>
         *
         * <li>if the wrapper type corresponds to the element primitive type, the trivial default conversion
         * is used; in all other cases:</tt>
         *
         * <li><tt>Boolean</tt> value <tt>v</tt> is converted to <tt>v?1:0</tt> for numeric element types
         * and to <tt>v?(char)1:(char)0</tt> for <tt>char</tt> element type;
         *
         * <li><tt>Character</tt> value <tt>v</tt> is converted to <tt>(byte)v</tt>,
         * <tt>(short)v</tt>, <tt>(int)v</tt>, <tt>(long)v</tt>, <tt>(float)v</tt>, <tt>(double)v</tt>
         * for corresponding numeric element types
         * and to <tt>v!=0</tt> for <tt>boolean</tt> element type;</tt></li>
         *
         * <li><tt>Byte</tt> value <tt>v</tt> is converted to <tt>(char)(v&amp;0xFF)</tt>,
         * <tt>(short)(v&amp;0xFF)</tt>, <tt>(int)(v&amp;0xFF)</tt>, <tt>(long)(v&amp;0xFF)</tt>,
         * <tt>(float)(v&amp;0xFF)</tt>, <tt>(double)(v&amp;0xFF)</tt>
         * for corresponding numeric or character element types
         * and to <tt>v!=0</tt> for <tt>boolean</tt> element type;</tt></li>

         * <li><tt>Short</tt> value <tt>v</tt> is converted to <tt>(char)v</tt>,
         * <tt>(byte)v</tt>, <tt>(int)(v&amp;0xFFFF)</tt>, <tt>(long)(v&amp;0xFFFF)</tt>,
         * <tt>(float)(v&amp;0xFFFF)</tt>, <tt>(double)(v&amp;0xFFFF)</tt>
         * for corresponding numeric or character element types
         * and to <tt>v!=0</tt> for <tt>boolean</tt> element type;</tt></li>

         * <li><tt>Integer</tt>, <tt>Long</tt>, <tt>Float</tt> or <tt>Double</tt> value <tt>v</tt>
         * is converted to <tt>(char)v</tt>,
         * <tt>(byte)v</tt>, <tt>(short)v</tt>, <tt>(int)v</tt>, <tt>(long)v</tt>,
         * <tt>(float)v</tt>, <tt>(double)</tt>v
         * for corresponding numeric or character element types
         * and to <tt>v!=0</tt> for <tt>boolean</tt> element type.</tt></li>
         * </ul>
         *
         * @param continuationConstant the value returned while reading elements, lying outside this matrix.
         * @return                     new continuation mode with the specified continuation constant.
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
         * Returns <tt>true</tt> if and only if this instance is a constant continuation mode,
         * i&#46;e&#46; was created by {@link #getConstantMode(Object)} method or it is one
         * of the predefined constants {@link #ZERO_CONSTANT} and {@link #NULL_CONSTANT}.
         *
         * @return whether it is a constant continuation mode.
         */
        public boolean isConstant() {
            return false;
        }

        /**
         * Returns <tt>true</tt> if and only if {@link #isConstant()} returns true and
         * the result of {@link #continuationConstant()} is <tt>null</tt> or is an instance of
         * some  wrapper for primitive types: <tt>Boolean</tt>, <tt>Character</tt>, <tt>Byte</tt>, <tt>Short</tt>,
         * <tt>Integer</tt>, <tt>Long</tt>, <tt>Float</tt> or <tt>Double</tt>.
         *
         * <p>This method indicates, whether this mode can be used for constant continuation of a matrix
         * with primitive type of elements. But note that such a mode can also be used for continuation of
         * a matrix, consisting of non-primitive elements, belonging to the corresponding wrapper type
         * or its superclass like <tt>Number</tt> or <tt>Object</tt>.
         *
         * @return whether it is a constant continuation mode, where the continuation constant is <tt>null</tt>
         *         or some Java wrapper object for a primitive type.
         */
        public boolean isPrimitiveTypeOrNullConstant() {
            return false;
        }

        /**
         * Returns the continuation constant, used in this mode, if it is a
         * {@link #isConstant() constant continuation mode},
         * or throws throws {@link NonConstantMatrixContinuationModeException},
         * if it is not a constant continuation mode.
         *
         * <p>If this instance was created by {@link #getConstantMode(Object)} method,
         * this method returns exactly the same reference to an object, which was passed
         * to that method as <tt>continuationConstant</tt> argument.
         * For {@link #NULL_CONSTANT}, this method returns <tt>null</tt>.
         * For {@link #ZERO_CONSTANT}, this method returns <tt>Double.valueOf(0.0d)</tt>.
         * For {@link #NAN_CONSTANT}, this method returns <tt>Double.valueOf(Double.NaN)</tt>.
         *
         * @return the continuation constant, used in this mode,
         *         if it is a {@link #isConstant() constant continuation mode},
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
         * <p>If the argument is <tt>null</tt> or not an instance of this class, this method returns <tt>false</tt>.
         *
         * <p>If this instance is a {@link #isConstant() constant continuation mode},
         * this method returns <tt>true</tt> if and only if the argument is also a constant continuation mode
         * and either both continuation constants, returned by {@link #continuationConstant()} method,
         * are <tt>null</tt>, or they are equal objects in terms of standard <tt>equals</tt> method
         * (i.e. <tt>equal</tt> method of the {@link #continuationConstant()} object returns <tt>true</tt>
         * for <tt>((ContinuationMode)o).{@link #continuationConstant()}</tt>).
         *
         * <p>If this instance is not a constant continuation mode, this method returns <tt>true</tt>
         * if and only if this instance and <tt>o</tt> argument are the same reference (<tt>this==o</tt>).
         * It is correct, because all only possible non-constant instances of this class are represented
         * by static constants of this class, as well as in standard enumerations.
         *
         * @param o the object to be compared for equality with this instance.
         * @return  <tt>true</tt> if the specified object is a continuation mode equal to this one.
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
                    || continuationConstant instanceof Double;            }

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
     * <i>n</i><tt>&lt;={@link #MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     * In this package and all known subpackages of <tt>net.algart</tt> package,
     * the following classes require that the number of dimensions must not be greater
     * than {@link #MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}:
     * <ul>
     * <li><tt>net.algart.matrices.scanning.ConnectedObjectScanner</tt>;</li>
     * <li>{@link MatrixInfo}.</li>
     * </ul>
     *
     * <p>Note: the value of this constant ({@value}) is the maximal <i>n</i> so that
     * 3<sup><i>n</i></sup>&lt;32768=2<sup>15</sup> (3<sup>9</sup>=19683).
     * It can be useful while storing indexes of elements of little 3x3x3x... submatrix (aperture):
     * signed <tt>short</tt> type is enough in this case.
     */
    public static final int MAX_DIM_COUNT_FOR_SOME_ALGORITHMS = 9;

    /**
     * Returns a reference to the built-in AlgART array.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     *
     * @return a reference to the built-in AlgART array.
     */
    public T array();

    /**
     * Returns the type of matrix elements.
     * Equivalent to <tt>{@link #array()}.{@link Array#elementType() elementType()}</tt>.
     *
     * @return the type of the matrix elements.
     */
    public Class<?> elementType();

    /**
     * Returns the total number of matrix elements.
     * Equivalent to <tt>{@link #array()}.{@link Array#length() length()}</tt>.
     *
     * @return the total number of matrix elements.
     */
    public long size();

    /**
     * Returns <tt>{@link #array()}.{@link Array#type() type()}</tt>.
     *
     * @return the canonical type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is <tt>null</tt>.
     */
    public Class<? extends Array> type();

    /**
     * Returns <tt>{@link #array()}.{@link Array#updatableType() updatableType()}</tt>.
     *
     * @return the canonical updatable type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is <tt>null</tt>.
     */
    public Class<? extends UpdatableArray> updatableType();

    /**
     * Returns <tt>{@link #array()}.{@link Array#type() type()}</tt>,
     * if it is subtype of (or same type as) the passed <tt>arraySupertype</tt>,
     * or throws <tt>ClassCastException</tt> in other case.
     * (If the passed argument is a class of {@link UpdatableArray} or some its
     * subinterfaces or subclasses, <tt>IllegalArgumentException</tt> is thrown instead:
     * updatable array classes cannot be specified in this method.)
     *
     * @param arraySupertype the required supertype of the built-in AlgART array.
     * @return               the canonical type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed argument is a class of {@link UpdatableArray} or some its
     *                                  subinterfaces or subclasses (updatable classes cannot be supertypes of
     *                                  for {@link Array#type() Array.type()}).
     * @throws ClassCastException       if <tt>arraySupertype</tt> does not allow storing
     *                                  the immutable version of the built-in AlgART array.
     */
    public <U extends Array> Class<? extends U> type(Class<U> arraySupertype);

    /**
     * Returns <tt>{@link #array()}.{@link Array#updatableType() updatableType()}</tt>,
     * if it is subtype of (or same type as) the passed <tt>arraySupertype</tt>,
     * or throws <tt>ClassCastException</tt> in other case.
     *
     * @param arraySupertype the required supertype of the built-in AlgART array.
     * @return               the canonical updatable type of AlgART array of the same kind as the built-in one.
     * @throws NullPointerException if the passed argument is <tt>null</tt>.
     * @throws ClassCastException   if <tt>arraySupertype</tt> does not allow storing
     *                              the built-in AlgART array.
     */
    public <U extends Array> Class<? extends U> updatableType(Class<U> arraySupertype);

    /**
     * Returns <tt>true</tt> if and only if the {@link #elementType() element type} is primitive:
     * <tt>{@link #array()} instanceof {@link PArray}</tt>.
     *
     * @return whether the type of matrix element is boolean, char, byte, short, int, long, float or double.
     */
    public boolean isPrimitive();

    /**
     * Returns <tt>true</tt> if and only if the {@link #elementType() element type} is <tt>float</tt>
     * or <tt>double</tt>:
     * <tt>{@link #array()} instanceof {@link PFloatingArray}</tt>.
     *
     * @return whether the type of matrix element is float or double.
     */
    public boolean isFloatingPoint();

    /**
     * Returns <tt>true</tt> if and only if the {@link #elementType() element type} is fixed-point:
     * <tt>{@link #array()} instanceof {@link PFixedArray}</tt>.
     *
     * @return whether the type of matrix element is byte, short, int, long, char or boolean.
     */
    public boolean isFixedPoint();

    /**
     * Returns <tt>true</tt> if and only if the {@link #elementType() element type} is <tt>boolean.class</tt>,
     * <tt>short.class</tt>, <tt>byte.class</tt> or <tt>short.class</tt>.
     *
     * <p>Equivalent to <tt>{@link Arrays#isUnsignedElementType(Class)
     * Arrays.isUnsignedElementType}(thisMatrix.{@link #elementType() elementType()}).
     *
     * @return whether the element type of this matrix should be interpreted as unsigned primitive type.
     */
    public boolean isUnsigned();

    /**
     * Returns the number of in bits, required for each element of this matrix, if they are
     * {@link #isPrimitive() primitive}; in other case returns &minus;1.
     * Equivalent to <tt>{@link Arrays#bitsPerElement(Class)
     * Arrays.bitsPerElement}(thisMatrix.{@link #elementType() elementType()}).
     *
     * @return the size of each element in bits or &minus;1 if for non-primitive elements.
     */
    public long bitsPerElement();

    /**
     * Returns the maximal possible value, that can stored in elements of this matrix,
     * if they are fixed-point elements, or the argument for floating-point elements,
     * or <tt>Double.NaN</tt> if elements are not primitive.
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
     * or <tt>Double.NaN</tt> for non-primitive element types.
     */
    public double maxPossibleValue(double valueForFloatingPoint);

    /**
     * Returns the maximal possible value, that can stored in elements of this matrix,
     * if they are fixed-point elements, or <tt>1.0</tt> for floating-point elements,
     * or <tt>Double.NaN</tt> if elements are not primitive.
     *
     * <p>Equivalent to {@link #maxPossibleValue(double) maxPossibleValue(1.0)}.
     * It is a good default for most application.
     *
     * @return maximal possible value for primitive element types (1.0 for float/double),
     * or <tt>Double.NaN</tt> for non-primitive element types.
     */
    public double maxPossibleValue();

    /**
     * Returns an array containing all dimensions of this matrix.
     * Returned array is equal to the <tt>dim</tt> argument passed to methods that create new matrix instances.
     *
     * <p>The returned array is a clone of the internal dimension array, stored in this object.
     * The returned array is never empty (its length cannot be zero).
     * The elements of the returned array are never negative.
     *
     * @return an array containing all dimensions of this matrix.
     */
    public long[] dimensions();

    /**
     * Returns the number of dimensions of this matrix.
     * This value is always positive (&gt;=1).
     * Equivalent to <tt>{@link #dimensions()}.length</tt>, but works faster.
     *
     * <p>There is a guarantee that this method works very quickly
     * (usually it just returns a value of some private field).
     *
     * @return the number of dimensions of this matrix.
     */
    public int dimCount();

    /**
     * Returns the dimension <tt>#n</tt> of this matrix
     * or <tt>1</tt> if <tt>n&gt;={@link #dimCount()}</tt>.
     * Equivalent to <tt>n&lt;{@link #dimCount()}?{@link #dimensions()}[n]:1</tt>, but works faster.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @param n the index of dimension.
     * @return  the dimension <tt>#n</tt> of this matrix.
     * @throws IndexOutOfBoundsException if <tt>n&lt;0</tt> (but <i>not</i> if <tt>n</tt> is too large).
     */
    public long dim(int n);

    /**
     * Equivalent to <tt>{@link #dim(int) dim}(0)</tt>.
     *
     * @return the first matrix dimension.
     */
    public long dimX();

    /**
     * Equivalent to <tt>{@link #dim(int) dim}(1)</tt>.
     *
     * @return the second matrix dimension.
     */
    public long dimY();

    /**
     * Equivalent to <tt>{@link #dim(int) dim}(2)</tt>.
     *
     * @return the third matrix dimension.
     */
    public long dimZ();

    /**
     * Indicates whether the other matrix has the same dimension array.
     * In other words, returns <tt>true</tt> if and only if
     * both matrices have the same dimension count ({@link #dimCount()})
     * and the corresponding dimensions ({@link #dim(int) dim(k)}) are equal.
     *
     * @param m the matrix to be compared for equal dimensions with this matrix.
     * @return  <tt>true</tt> if the specified matrix has the same dimension array.
     * @throws NullPointerException if the passed argument is <tt>null</tt>.
     * @see #dimEquals(long...)
     */
    public boolean dimEquals(Matrix<?> m);

    /**
     * Indicates whether the passed dimensions are equal to the dimension array of this matrix.
     * In other words, returns <tt>true</tt> if and only if
     * <tt>dimension.length=={@link #dimCount()}</tt>
     * and the corresponding dimensions <tt>{@link #dim(int) dim(k)}==dimension[k]</tt> for all <tt>k</tt>.
     *
     * <p>Note: this method does not check, whether all passed dimensions are correct (in particular, non-negative).
     * If some elements of the passed array are incorrect, this method just returns <tt>false</tt>.
     * But it the passed array is <tt>null</tt>, this method throws <tt>NullPointerException</tt>.
     *
     * @param dimensions the dimension array.
     * @return           <tt>true</tt> if the specified dimensions are equal to the dimensions of this matrix.
     * @throws NullPointerException if the passed argument is <tt>null</tt>.
     * @see #dimEquals(Matrix)
     */
    public boolean dimEquals(long... dimensions);

    /**
     * Returns the linear index in the built-in AlgART array of the matrix element
     * with specified coordinates.
     *
     * <p>More precisely,
     * <tt>index(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i>)</tt>
     * returns the following value:
     *
     * <blockquote>
     * <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
     * </blockquote>
     *
     * where <tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</tt>.
     * All passed indexes <tt><i>i<sub>k</sub></i></tt> must be in ranges <tt>0..<i>d<sub>k</sub></i>-1</tt>.
     *
     * <p>All elements of <tt>coordinates</tt> array are always used, regardless of the number of matrix dimensions.
     * But the extra elements of <tt>coordinates</tt> array must be zero,
     * because <tt><i>d<sub>k</sub></i>=1</tt> for <tt><i>k</i>&gt;={@link #dimCount()}</tt>.
     *
     * <p>Good algorithms processing the matrix should use this method rarely:
     * usually there are more optimal ways to calculate necessary linear index.
     * For example, if you just need to calculate something for all matrix elements,
     * the best way is the following:
     *
     * <pre>
     * Array a = m.array();
     * for (long disp = 0, n = a.length(); disp < n; disp++)
     * &#32;   // process the element #k of the array
     * </pre>
     *
     * @param coordinates all coordinates.
     * @return            the linear index of the matrix element with specified coordinates.
     * @throws NullPointerException      if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException  if the passed array is empty (no coordinates are passed).
     * @throws IndexOutOfBoundsException if some coordinate <tt><i>i<sub>k</sub></i></tt> is out of range
     *                                   <tt>0..<i>d<sub>k</sub></i>-1</tt>.
     * @see #uncheckedIndex(long...)
     * @see #cyclicIndex(long...)
     * @see #pseudoCyclicIndex(long...)
     * @see #mirrorCyclicIndex(long...)
     * @see #coordinates(long, long[])
     * @see IPoint#toOneDimensional(long[], boolean)
     */
    public long index(long... coordinates);

    /**
     * The simplified version of the full {@link #index(long...) index} method for the case
     * of 2-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @return  <tt>y * {@link #dimX()} + x</tt>.
     * @throws IndexOutOfBoundsException if <tt>x&lt;0</tt>, <tt>x&gt;={@link #dimX()}</tt>,
     *                                   <tt>y&lt;0</tt> or <tt>y&gt;={@link #dimX()}</tt>.
     */
    public long index(long x, long y);

    /**
     * The simplified version of the full {@link #index(long...) index} method for the case
     * of 3-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @param z the third coordinate.
     * @return  <tt>z * {@link #dimY()} * {@link #dimX()} + y * {@link #dimX()} + x</tt>.
     * @throws IndexOutOfBoundsException if <tt>x&lt;0</tt>, <tt>x&gt;={@link #dimX()}</tt>,
     *                                   <tt>y&lt;0</tt>, <tt>y&gt;={@link #dimX()}</tt>,
     *                                   <tt>z&lt;0</tt> or <tt>z&gt;={@link #dimZ()}</tt>.
     */
    public long index(long x, long y, long z);

    /**
     * Returns the coordinates in the matrix, corresponding to the given linear index in the built-in AlgART array.
     * This method is reverse to {@link #index(long...)}: for any index,
     * <nobr><tt>{@link #index(long...) index}({@link #coordinates(long, long[])
     * coordinates}(index, null)) == index</tt></nobr>.
     *
     * <p>The <tt>result</tt> argument may be <tt>null</tt> or some array, containing at least {@link #dimCount()}
     * elements. If the first case, this method allocates new Java array <tt>long[{@link #dimCount()}]</tt>
     * for storing coordinates and returns it.
     * In the second case, this method stores the found coordinates in <tt>result</tt> array and returns it.
     * The returned coordinates are always in ranges
     * <pre>
     * 0 &le; result[<i>k</i>] &lt; {@link #dim(int) dim}(<i>k</i>)</pre>
     *
     * @param index  the linear index in the built-in AlgART array.
     * @param result the array where you want to store results; may be <tt>null</tt>.
     * @return       a reference to the <tt>result</tt> argument, if it is not <tt>null</tt>,
     *               else newly created Java array contains all calculated coordinates.
     * @throws IllegalArgumentException  if <tt>result!=null</tt>, but <tt>result.length&lt;{@link #dimCount()}</tt>.
     * @throws IndexOutOfBoundsException if <tt>index&lt;0</tt> or <tt>index&gt;={@link #dim(int)
     *                                   dim}(0)*{@link #dim(int) dim}(1)*...={@link #array()}.{@link
     *                                   Array#length() length()}</tt>.
     */
    public long[] coordinates(long index, long[] result);

    /**
     * An analog of {@link #index(long...)} method, that does not check,
     * whether the passed coordinates are in the required ranges.
     *
     * <p>More precisely,
     * <tt>uncheckedIndex(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i>)</tt>
     * always returns the following value:
     *
     * <blockquote>
     * <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
     * </blockquote>
     *
     * where <tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</tt>.
     *
     * <p>All calculations are performed with <tt>long</tt> type without any overflow checks.
     * All elements of <tt>coordinates</tt> array are always used, regardless of the number of matrix dimensions.
     * Please remember that <tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)=1</tt>
     * for <tt><i>k</i>&gt;={@link #dimCount()}
     * (extra elements of <tt>coordinates</tt> array).</tt>.
     *
     * @param coordinates all coordinates.
     * @return            the linear index of the matrix element with specified coordinates, without range checks.
     * @throws NullPointerException     if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     */
    public long uncheckedIndex(long... coordinates);

    /**
     * An analog of {@link #index(long...)} method, that, before all calculations,
     * replaces the passed coordinates with the positive remainders
     * from division of them by the corresponding matrix dimensions.
     *
     * <p>More precisely, let <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
     * are the arguments of the method. Let
     * <nobr><tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</tt></nobr> and
     * <blockquote>
     * <i>i'<sub>k</sub></i> = <i>d<sub>k</sub></i> == 0 ? 0 :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> &gt;= 0 ?
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> + <i>d<sub>k</sub></i>
     * </blockquote>
     *
     * This method returns the following value:
     *
     * <blockquote>
     * <i>i'<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i'<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i'<sub>1</sub>d<sub>0</sub></i> + <i>i'<sub>0</sub></i>,
     * </blockquote>
     *
     * In other words, the resulting index is "cyclical".
     *
     * <p>All elements of <tt>coordinates</tt> array are always used, regardless of the number of matrix dimensions.
     * (You can note that extra elements of <tt>coordinates</tt> array are ignored in fact:
     * the reminders <i>i<sub>k</sub></i>%<i>d<sub>k</sub></i>=<i>i<sub>k</sub></i>%1 will be zero for them.)
     *
     * @param coordinates all coordinates.
     * @return            the cyclical linear index of the matrix element with specified coordinates,
     *                    without range checks.
     * @throws NullPointerException     if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     * @see Matrix.ContinuationMode#CYCLIC
     */
    public long cyclicIndex(long... coordinates);

    /**
     * An analog of {@link #index(long...)} method, that does not check,
     * whether the passed coordinates are in the required ranges,
     * but replaces the resulting index with the positive remainder
     * from division of it by the length of the built-in array.
     *
     * <p>More precisely, let <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
     * are the arguments of the method, and <tt><i>index</i></tt> is the following value
     * (as in {@link #index(long...)} method):
     *
     * <blockquote>
     * <tt><i>index</i></tt> = <i>i<sub>n-1</sub>d<sub>n-2</sub>...d<sub>1</sub>d<sub>0</sub></i> + ... +
     * <i>i<sub>2</sub>d<sub>1</sub>d<sub>0</sub></i> +
     * <i>i<sub>1</sub>d<sub>0</sub></i> + <i>i<sub>0</sub></i>,
     * </blockquote>
     *
     * where <tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</tt>.
     * Here we <i>do no require</i> that the passed indexes <tt><i>i<sub>k</sub></i></tt>
     * are in ranges <tt>0..<i>d<sub>k</sub></i>-1</tt>.
     * Then, let <tt><i>len</i>={@link #array()}.{@link Array#length()
     * length()}=d<sub>n-1</sub>...d<sub>1</sub>d<sub>0</sub></i></tt>.
     * The result of this method is the following:
     *
     * <blockquote>
     * <tt><i>len</i> == 0 ? 0 : <i>index</i> % <i>len</i> &gt;= 0 ?
     * <i>index</i> % <i>len</i> : <i>index</i> % <i>len</i> + <i>len</i></tt>
     * </blockquote>
     *
     * (It is in the <tt>0..<i>len</i>-1</tt> range always, excepting the generated case <tt><i>len</i>==0</tt>.)
     * In other words, the resulting index is "pseudo-cyclical", as the resulting shift
     * in {@link Matrices#asShifted(Matrix, long...)} method.
     *
     * <p>All elements of <tt>coordinates</tt> array are always used, regardless of the number of matrix dimensions.
     * (You can note that extra elements of <tt>coordinates</tt> array are ignored in fact:
     * they add <tt>k*<i>len</i></tt> summand, where <tt>k</tt> is an integer.)
     *
     * <p>Note that all calculations are performed absolutely precisely, even in a case when
     * the direct calculation according the formulas above leads to overflow (because some
     * of values in these formulas are out of <tt>Long.MIN_VALUE..Long.MAX_VALUE</tt> range).
     *
     * @param coordinates all coordinates.
     * @return            the pseudo-cyclical linear index of the matrix element with specified coordinates,
     *                    without range checks.
     * @throws NullPointerException     if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     * @see IPoint#toOneDimensional(long[], boolean)
     * @see Matrix.ContinuationMode#PSEUDO_CYCLIC
     */
    public long pseudoCyclicIndex(long... coordinates);

    /**
     * An analog of {@link #index(long...)} method, that, before all calculations,
     * replaces the passed coordinates with the positive remainders
     * from division of them by the corresponding matrix dimensions
     * or with complement of these remainders on the dimensions,
     * as if the matrix would be reflected in each its bound as in a mirror.
     *
     * <p>More precisely, let <tt><i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n-1</sub></i></tt>
     * are the arguments of the method. Let
     * <nobr><tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</tt></nobr>,
     * <blockquote>
     * <i>i'<sub>k</sub></i> = <i>d<sub>k</sub></i> == 0 ? 0 :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> &gt;= 0 ?
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> :
     * <i>i<sub>k</sub></i> % <i>d<sub>k</sub></i> + <i>d<sub>k</sub></i>
     * </blockquote>
     * (as in {@link #cyclicIndex(long...)} and
     * <blockquote>
     * <i>i''<sub>k</sub></i> = <i>d<sub>k</sub></i> == 0 ? 0 :
     * &lfloor;<i>i<sub>k</sub></i> / <i>d<sub>k</sub></i>&rfloor; % 2 == 0 ?
     * <i>i'<sub>k</sub></i> :
     * <i>d<sub>k</sub></i> &minus; 1 &minus; <i>i'<sub>k</sub></i>
     * </blockquote>
     * (here &lfloor;<i>x</i>&rfloor; means the integer part of <i>x</i>, i.e. <tt>Math.floor(<i>x</i>)</tt>).
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
     * <p>All elements of <tt>coordinates</tt> array are always used, regardless of the number of matrix dimensions.
     * (You can note that extra elements of <tt>coordinates</tt> array are ignored in fact:
     * the reminders <i>i<sub>k</sub></i>%<i>d<sub>k</sub></i>=<i>i<sub>k</sub></i>%1 will be zero for them.)
     *
     * @param coordinates all coordinates.
     * @return            the mirror-cyclical linear index of the matrix element with specified coordinates,
     *                    without range checks.
     * @throws NullPointerException     if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     * @see Matrix.ContinuationMode#MIRROR_CYCLIC
     */
    public long mirrorCyclicIndex(long... coordinates);

    /**
     * Returns <tt>true</tt> if all specified coordinates <tt><i>i<sub>k</sub></i></tt>
     * are inside the ranges <tt>0..<i>d<sub>k</sub></i>-1</tt>,
     * where <tt><i>d<sub>k</sub></i>={@link #dim(int) dim}(<i>k</i>)</tt>.
     *
     * <p>This method allows simply check that the arguments of
     * the {@link #index(long...) index} method are correct and will not lead to
     * <tt>IndexOutOfBoundsException</tt>:
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
     * @return            <tt>true</tt> if all specified coordinates are inside the matrix.
     * @throws NullPointerException     if the passed array is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is empty (no coordinates are passed).
     */
    public boolean inside(long... coordinates);

    /**
     * The simplified version of the full {@link #inside(long...) inside} method for the case
     * of 2-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @return  tt>true</tt> if all specified coordinates are inside the matrix.
     */
    public boolean inside(long x, long y);

    /**
     * The simplified version of the full {@link #inside(long...) inside} method for the case
     * of 3-dimensional matrix.
     *
     * @param x the first coordinate.
     * @param y the second coordinate.
     * @param z the third coordinate.
     * @return  tt>true</tt> if all specified coordinates are inside the matrix.
     */
    public boolean inside(long x, long y, long z);

    /**
     * Returns the new matrix backed by the specified AlgART array with the same dimensions as this one.
     * Equivalent to <tt>{@link Matrices#matrix(Array, long...)
     * Matrices.matrix}(anotherArray, {@link #dimensions()})</tt>.
     *
     * <p>The array <tt>anotherArray</tt> must be {@link Array#isUnresizable() unresizable},
     * and its length must be equal to the length of the array built-in this matrix.
     *
     * @param anotherArray some another AlgART array with the same length as {@link #array()}.
     * @return             new matrix instance.
     * @throws NullPointerException     if <tt>anotherArray</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed array is resizable
     *                                  (for example, implements {@link MutableArray}).
     * @throws SizeMismatchException    if the product of all dimensions is not equal to the passed array length.
     */
    public <U extends Array> Matrix<U> matrix(U anotherArray);

    /**
     * Returns this matrix, cast to the specified generic array type,
     * or throws <tt>ClassCastException</tt> if the built-in AlgART array
     * cannot be cast to the required type (because the array type is not its subclass).
     * Works alike <tt>{@link #matrix(Array) matrix}((U)array)</tt>, but returns
     * the reference to this instance and is compiled without "unchecked cast" warning.
     *
     * <p>This method is useful when you need to cast the type of AlgART array,
     * built in this matrix, to to its sub- or superinterface.
     *
     * @param arrayClass the type of built-in array in the new matrix.
     * @return           new matrix with the same dimensions, based on the same array cast to the required type.
     * @throws NullPointerException  if the argument is <tt>null</tt>.
     * @throws ClassCastException    if the built-in AlgART array cannot be cast to the required type.
     */
    public <U extends Array> Matrix<U> cast(Class<U> arrayClass);

    /**
     * Returns a view of the rectangular fragment of this matrix between <tt>from</tt>,
     * inclusive, and <tt>to</tt>, exclusive.
     *
     * <p>More precisely, the returned matrix consists of all elements of this one with coordinates
     * <i>i<sub>0</sub></i>, <i>i<sub>1</sub></i>, ..., <i>i<sub>n&minus;1</sub></i>,
     * <i>n</i><tt>={@link #dimCount()}</tt>,
     * matching the following conditions:<pre>
     *     from[0] &lt;= <i>i<sub>0</sub></i> &lt; to[0],
     *     from[1] &lt;= <i>i<sub>1</sub></i> &lt; to[1],
     *     . . .
     *     from[<i>n</i>-1] &lt;= <i>i<sub>n-1</sub></i> &lt; to[<i>n</i>-1]
     * </pre>
     *
     * So, every dimension {@link #dim(int) dim(k)} in the returned matrix will be equal to <tt>to[k]-from[k]</tt>.
     * The following condition must be fulfilled for all <tt>k</tt>:
     * <tt>0&lt;=from[k]&lt;=to[k]&lt;=thisMatrix.{@link #dim(int) dim(k)}</tt>.
     * The {@link #elementType() element type} of the returned matrix is identical to the element type
     * of this matrix.
     *
     * <p>This method is equivalent to the call
     * <nobr><tt>{@link #subMatrix(long[], long[], ContinuationMode)
     * subMatrix}(from,to,{@link Matrix.ContinuationMode#NONE})</tt></nobr>.
     *
     * <p>The built-in AlgART array of the returned matrix is backed by the built-in array of this matrix,
     * so &mdash; if this matrix is not {@link #isImmutable() immutable}
     * &mdash; any changes of the elements of the returned matrix are reflected in this matrix, and vice-versa.
     * The returned matrix is {@link #isImmutable() immutable} if, and only if,
     * the built-in array of this matrix does not implement {@link UpdatableArray}.
     * The {@link Array#asTrustedImmutable()} method
     * in the built-in array of the returned matrix is equivalent to {@link Array#asImmutable()},
     * and {@link Array#asCopyOnNextWrite()} method just returns the full copy of the array.
     *
     * @param from low endpoints (inclusive) of all coordinates.
     * @param to   high endpoints (exclusive) of all coordinates.
     * @return     a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>from</tt> or <tt>to</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>from.length</tt> or <tt>to.length</tt>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if, for some <tt>k</tt>,
     *                                   <tt>from[k]&lt;0 || to[k]&gt;{@link #dim(int) dim(k)} ||
     *                                   from[k]&gt;to[k]</tt>.
     * @see #subMatrix(long[], long[], ContinuationMode)
     * @see #subMatrix(IRectangularArea)
     * @see #subMatr(long[], long[], ContinuationMode)
     * @see #subMatr(long[], long[])
     * @see #isSubMatrix()
     */
    public Matrix<T> subMatrix(long[] from, long[] to);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to)} method, where
     * <tt>from.length=to.length=area.{@link IRectangularArea#coordCount() coordCount()}</tt>,
     * <tt>from[k]=area.{@link IRectangularArea#min(int) min}(k)</tt>,
     * <tt>to[k]=area.{@link IRectangularArea#max(int) max}(k)+1</tt>.
     *
     * @param area rectangular area within this matrix.
     * @return     a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>area.{@link IRectangularArea#coordCount() coordCount()}</tt>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if, for some <tt>k</tt>,
     *                                   <tt>min[k]&lt;0 || max[k]&gt;={@link #dim(int) dim(k)}</tt>, where
     *                                   <tt>min=area.{@link IRectangularArea#min()
     *                                   min()}.{@link IPoint#coordinates() coordinates()}</tt> and,
     *                                   <tt>max=area.{@link IRectangularArea#max()
     *                                   max()}.{@link IPoint#coordinates() coordinates()}</tt>.
     */
    public Matrix<T> subMatrix(IRectangularArea area);

    /**
     * Equivalent to <tt><nobr>{@link #subMatrix(long[], long[])
     * subMatrix}(new long[]{fromX,fromY}, new long[]{toX,toY})</nobr></tt>.
     * Note that this matrix must be 2-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param fromX low endpoints (inclusive) of the first coordinate.
     * @param fromY low endpoints (inclusive) of the second coordinate.
     * @param toX   high endpoints (exclusive) of the first coordinate.
     * @param toY   high endpoints (exclusive) of the second coordinate.
     * @return      a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=2</tt>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatrix(long[], long[])}.
     */
    public Matrix<T> subMatrix(long fromX, long fromY, long toX, long toY);

    /**
     * Equivalent to <tt><nobr>{@link #subMatrix(long[], long[])
     * subMatrix}(new long[]{fromX,fromY,fromZ}, new long[]{toX,toY,toZ})</nobr></tt>.
     * Note that this matrix must be 3-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param fromX low endpoints (inclusive) of the first coordinate.
     * @param fromY low endpoints (inclusive) of the second coordinate.
     * @param fromZ low endpoints (inclusive) of the third coordinate.
     * @param toX   high endpoints (exclusive) of the first coordinate.
     * @param toY   high endpoints (exclusive) of the second coordinate.
     * @param toZ   high endpoints (exclusive) of the third coordinate.
     * @return      a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=3</tt>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatrix(long[], long[])}.
     */
    public Matrix<T> subMatrix(long fromX, long fromY, long fromZ, long toX, long toY, long toZ);

    /**
     * An extended analog of {@link #subMatrix(long[], long[])} method, allowing to get a rectangular fragment
     * which is not fully inside this matrix.
     *
     * <p>More precisely, unlike {@link #subMatrix(long[], long[])}, here
     * the only requirement for the <tt>from</tt> and <tt>to</tt> coordinate boundaries is <tt>from[k]&lt;=to[k]</tt>,
     * but <tt>from[k]</tt> may be negative and <tt>to[k]</tt> may be greater than {@link #dim(int) dim(k)}.
     * (And there is also a trivial obvious requirement
     * <nobr><tt>to[k]-from[k]&le;Long.MAX_VALUE</tt></nobr>, i.e. that the dimensions of the result must
     * be representable by <tt>long</tt> type.)
     *
     * <p>The elements of the returned matrix, that do not correspond to any elements of this one,
     * i.e. "lie outside" of the source matrix, are considered to be equal to some values, according to
     * some <i>continuation model</i>, described by <tt>continuationMode</tt> argument.
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
     * <p>Important note: there are two cases, when requirements to the <tt>from</tt> and <tt>to</tt>
     * coordinate boundaries are more strong, than described above.
     * <ol>
     *     <li>If <tt>continuationMode=={@link ContinuationMode#NONE}</tt>, this method is strictly
     *     equivalent to more simple {@link Matrix#subMatrix(long[], long[])} method,
     *     so all requirements are the same as for that method.</li>
     *     <li>If <tt>continuationMode</tt> is {@link ContinuationMode#CYCLIC},
     *     {@link ContinuationMode#PSEUDO_CYCLIC} or {@link ContinuationMode#MIRROR_CYCLIC}
     *     (but it is not a constant continuation mode) and some dimension <tt>#k</tt>
     *     of this matrix is zero &mdash; <tt>{@link #dim(int) dim}(k)==0</tt> &mdash;
     *     then both corresponding coordinate boundaries <tt>from[k]</tt> and <tt>to[k]</tt>
     *     must be zero (as in {@link Matrix#subMatrix(long[], long[])} method).</li>
     * </ol>
     *
     * @param from             low endpoints (inclusive) of all coordinates.
     * @param to               high endpoints (exclusive) of all coordinates.
     * @param continuationMode the mode of continuation outside this matrix.
     * @return                 a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>from</tt>, <tt>to</tt> or <tt>continuationMode</tt>
     *                                   argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>from.length</tt> or <tt>to.length</tt>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException for <tt>continuationMode=={@link ContinuationMode#NONE}</tt> &mdash;
     *                                   see {@link Matrix#subMatrix(long[], long[])} method;
     *                                   for other cases &mdash; if, for some <tt>k</tt>,
     *                                   <tt>from[k]&gt;to[k]</tt> or
     *                                   <nobr><tt>to[k]-from[k]&gt;Long.MAX_VALUE</tt></nobr>,
     *                                   or if (for some <tt>k</tt>) <tt>{@link #dim(int) dim(k)}==0</tt> and
     *                                   <nobr><tt>from[k]!=0 || to[k]!=0</tt></nobr>,
     *                                   or if the product of all differences <tt>to[k]-from[k]</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     * @see #subMatr(long[], long[], ContinuationMode)
     * @see #subMatrix(long[], long[])
     * @see #subMatr(long[], long[])
     * @see #isSubMatrix()
     */
    public Matrix<T> subMatrix(long[] from, long[] to, ContinuationMode continuationMode);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to, ContinuationMode continuationMode)} method, where
     * <tt>from.length=to.length=area.{@link IRectangularArea#coordCount() coordCount()}</tt>,
     * <tt>from[k]=area.{@link IRectangularArea#min(int) min}(k)</tt>,
     * <tt>to[k]=area.{@link IRectangularArea#max(int) max}(k)+1</tt>.
     *
     * @param area         rectangular area within this matrix.
     * @return             a view of the specified rectangular fragment within this matrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @throws NullPointerException      if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>area.{@link IRectangularArea#coordCount() coordCount()}</tt>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if the product of all <tt>area.{@link IRectangularArea#sizes()}</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <tt>Long.MAX_VALUE</tt>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    public Matrix<T> subMatrix(IRectangularArea area, ContinuationMode continuationMode);

    /**
     * Equivalent to <tt><nobr>{@link #subMatrix(long[], long[], ContinuationMode)
     * subMatrix}(new long[]{fromX,fromY}, new long[]{toX,toY}, continuationMode)</nobr></tt>.
     * Note that this matrix must be 2-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param fromX        low endpoints (inclusive) of the first coordinate.
     * @param fromY        low endpoints (inclusive) of the second coordinate.
     * @param toX          high endpoints (exclusive) of the first coordinate.
     * @param toY          high endpoints (exclusive) of the second coordinate.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return             a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>continuationMode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=2</tt>.
     * @throws IndexOutOfBoundsException if <tt>fromX&gt;toX</tt> or <tt>toX-fromX&gt;Long.MAX_VALUE</tt>,
     *                                   or if <tt>fromY&gt;toY</tt> or <tt>toY-fromY&gt;Long.MAX_VALUE</tt>,
     *                                   or if the product <tt>(toX-fromX)*(toY-fromY)</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    public Matrix<T> subMatrix(long fromX, long fromY, long toX, long toY, ContinuationMode continuationMode);

    /**
     * Equivalent to <tt><nobr>{@link #subMatrix(long[], long[], ContinuationMode)
     * subMatrix}(new long[]{fromX,fromY,fromZ}, new long[]{toX,toY,toZ}, continuationMode)</nobr></tt>.
     * Note that this matrix must be 3-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param fromX        low endpoints (inclusive) of the first coordinate.
     * @param fromY        low endpoints (inclusive) of the second coordinate.
     * @param fromZ        low endpoints (inclusive) of the third coordinate.
     * @param toX          high endpoints (exclusive) of the first coordinate.
     * @param toY          high endpoints (exclusive) of the second coordinate.
     * @param toZ          high endpoints (exclusive) of the third coordinate.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return             a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>continuationMode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=3</tt>.
     * @throws IndexOutOfBoundsException if <tt>fromX&gt;toX</tt> or <tt>toX-fromX&gt;Long.MAX_VALUE</tt>,
     *                                   or if <tt>fromY&gt;toY</tt> or <tt>toY-fromY&gt;Long.MAX_VALUE</tt>,
     *                                   or if <tt>fromZ&gt;toZ</tt> or <tt>toZ-fromZ&gt;Long.MAX_VALUE</tt>,
     *                                   or if the product <tt>(toX-fromX)*(toY-fromY)*(toZ-fromZ)</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    public Matrix<T> subMatrix(
        long fromX, long fromY, long fromZ, long toX, long toY, long toZ,
        ContinuationMode continuationMode);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to)} method, where
     * <tt>from[k]=position[k]</tt> and <tt>to[k]=position[k]+dimensions[k]</tt> for all <tt>k</tt>.
     *
     * @param position   low endpoints (inclusive) of all coordinates.
     * @param dimensions dimensions of the returned submatrix.
     * @return           a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>position</tt> or <tt>dimensions</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>position.length</tt> or <tt>dimensions.length</tt>
     *                                   is not equal to {@link #dimCount()}.
     * @throws IndexOutOfBoundsException if, for some <tt>k</tt>,
     *                                   <tt>position[k]&lt;0 || dimensions[k]&lt;0 ||
     *                                   position[k]+dimensions[k]&gt;{@link #dim(int) dim(k)}</tt>.
     * @see #subMatr(long[], long[], ContinuationMode)
     */
    public Matrix<T> subMatr(long[] position, long[] dimensions);

    /**
     * Equivalent to <tt><nobr>{@link #subMatr(long[], long[])
     * subMatr}(new long[]{x,y}, new long[]{dimX,dimY})</nobr></tt>.
     * Note that this matrix must be 2-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param x    low endpoint (inclusive) of the first coordinate.
     * @param y    low endpoint (inclusive) of the second coordinate.
     * @param dimX th first dimension of the returned submatrix.
     * @param dimY the second dimension of the returned submatrix.
     * @return     a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=2</tt>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatr(long[], long[])}.
     */
    public Matrix<T> subMatr(long x, long y, long dimX, long dimY);

    /**
     * Equivalent to <tt><nobr>{@link #subMatr(long[], long[])
     * subMatr}(new long[]{x,y,z}, new long[]{dimX,dimY,dimZ})</nobr></tt>.
     * Note that this matrix must be 3-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param x    low endpoint (inclusive) of the first coordinate.
     * @param y    low endpoint (inclusive) of the second coordinate.
     * @param z    low endpoint (inclusive) of the third coordinate.
     * @param dimX th first dimension of the returned submatrix.
     * @param dimY the second dimension of the returned submatrix.
     * @param dimZ the third dimension of the returned submatrix.
     * @return     a view of the specified rectangular fragment within this matrix.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=2</tt>.
     * @throws IndexOutOfBoundsException in the same situations as in {@link #subMatr(long[], long[])}.
     */
    public Matrix<T> subMatr(long x, long y, long z, long dimX, long dimY, long dimZ);

    /**
     * Equivalent to {@link #subMatrix(long[] from, long[] to, ContinuationMode continuationMode)} method, where
     * <tt>from[k]=position[k]</tt> and <tt>to[k]=position[k]+dimensions[k]</tt> for all <tt>k</tt>.
     *
     * @param position     low endpoints (inclusive) of all coordinates.
     * @param dimensions   dimensions of the returned submatrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return             a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>position</tt>, <tt>dimensions</tt> or <tt>continuationMode</tt>
     *                                   argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>position.length</tt> or <tt>dimensions.length</tt>
     *                                   is not equal to {@link #dimCount()}/
     * @throws IndexOutOfBoundsException if, for some <tt>k</tt>, <tt>dimensions[k]&lt;0</tt>
     *                                   or <tt>position[k]+dimensions[k]&gt;Long.MAX_VALUE</tt>,
     *                                   or if the product of all <tt>dimensions[k]</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     * @see #subMatr(long[], long[])
     */
    public Matrix<T> subMatr(long[] position, long[] dimensions, ContinuationMode continuationMode);

    /**
     * Equivalent to <tt><nobr>{@link #subMatr(long[], long[], ContinuationMode)
     * subMatr}(new long[]{x,y}, new long[]{dimX,dimY}, continuationMode)</nobr></tt>.
     * Note that this matrix must be 2-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param x            low endpoint (inclusive) of the first coordinate.
     * @param y            low endpoint (inclusive) of the second coordinate.
     * @param dimX         th first dimension of the returned submatrix.
     * @param dimY         the second dimension of the returned submatrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return             a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>continuationMode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=2</tt>.
     * @throws IndexOutOfBoundsException if <tt>dimX&lt;0</tt>, <tt>dimY&lt;0</tt>, <tt>x+dimX&gt;Long.MAX_VALUE</tt>
     *                                   or <tt>y+dimY&gt;Long.MAX_VALUE</tt>,
     *                                   or if the product <tt>dimX*dimY</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    public Matrix<T> subMatr(long x, long y, long dimX, long dimY, ContinuationMode continuationMode);

    /**
     * Equivalent to <tt><nobr>{@link #subMatr(long[], long[], ContinuationMode)
     * subMatr}(new long[]{x,y,z}, new long[]{dimX,dimY,dimZ}, continuationMode)</nobr></tt>.
     * Note that this matrix must be 3-dimensional (in other case <tt>IllegalArgumentException</tt> will be thrown).
     *
     * @param x            low endpoint (inclusive) of the first coordinate.
     * @param y            low endpoint (inclusive) of the second coordinate.
     * @param z            low endpoint (inclusive) of the third coordinate.
     * @param dimX         th first dimension of the returned submatrix.
     * @param dimY         the second dimension of the returned submatrix.
     * @param dimZ         the third dimension of the returned submatrix.
     * @param continuationMode the value returned while reading elements, lying outside this matrix.
     * @return             a view of the specified rectangular fragment within this matrix.
     * @throws NullPointerException      if <tt>continuationMode</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>{@link #dimCount()}!=3</tt>.
     * @throws IndexOutOfBoundsException if <tt>dimX&lt;0</tt>, <tt>dimY&lt;0</tt>, <tt>dimZ&lt;0</tt>,
     *                                   <tt>x+dimX&gt;Long.MAX_VALUE</tt>, <tt>y+dimY&gt;Long.MAX_VALUE</tt>
     *                                   or <tt>z+dimZ&gt;Long.MAX_VALUE</tt>,
     *                                   or if the product <tt>dimX*dimY*dimZ</tt>
     *                                   (i.e. desired total size of the new matrix)
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @throws ClassCastException        if <tt>continuationMode</tt> is {@link
     *                                   ContinuationMode#getConstantMode(Object) a constant mode},
     *                                   the {@link Matrix.ContinuationMode#continuationConstant()
     *                                   continuation constant} is not <tt>null</tt> and the class of this
     *                                   constant is illegal, i.e.
     *                                   cannot be casted to the necessary type according the rules, specified
     *                                   for the {@link
     *                                   ContinuationMode#getConstantMode(Object) constant continuation mode}.
     */
    public Matrix<T> subMatr(
        long x, long y, long z, long dimX, long dimY, long dimZ,
        ContinuationMode continuationMode);

    /**
     * Returns <tt>true</tt> if and only if this matrix is a {@link #subMatrix(long[], long[]) submatrix} of
     * some <tt>parent</tt> matrix, created by one of calls <tt>parent.subMatrix(...)</tt>,
     * <tt>parent.subMatr(...)</tt> or equivalent.
     * The {@link #subMatrixParent()} method throws {@link NotSubMatrixException}
     * if and only if this method returns <tt>false</tt>.
     *
     * @return whether this object is created by <tt>subMatrix(...)</tt>, <tt>subMatr(...)</tt> or equivalent call.
     * @see #subMatrix(long[], long[])
     * @see #subMatrix(long[], long[], ContinuationMode)
     * @see #subMatr(long[], long[])
     * @see #subMatr(long[], long[], ContinuationMode)
     * @see #subMatrixParent()
     */
    public boolean isSubMatrix();

    /**
     * If this matrix is a {@link #subMatrix(long[], long[]) submatrix} of some <tt>parent</tt> matrix,
     * created by one of calls <tt>parent.subMatrix(...)</tt> or <tt>parent.subMatr(...)</tt>,
     * returns a reference to the <tt>parent</tt> matrix instance.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * @return a reference to the parent matrix, if this instance is a submatrix.
     * @throws NotSubMatrixException if this object is not created by <tt>subMatrix(...)</tt>,
     *                               <tt>subMatr(...)</tt> or equivalent call.
     * @see #isSubMatrix()
     */
    public Matrix<T> subMatrixParent();

    /**
     * If this matrix is a {@link #subMatrix(long[], long[]) submatrix} of some <tt>parent</tt> matrix,
     * created by one of calls <tt>parent.subMatrix(...)</tt> or <tt>parent.subMatr(...)</tt>,
     * creates and returns a new Java array containing the starting position of this submatrix
     * in the parent one. The result will be equal to "<tt>from</tt>" argument of
     * {@link #subMatrix(long[], long[])} and {@link #subMatrix(long[], long[], ContinuationMode)} methods.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * @return low endpoints (inclusive) of all coordinates of this submatrix in its parent matrix.
     * @throws NotSubMatrixException if this object is not created by <tt>subMatrix(...)</tt>,
     *                               <tt>subMatr(...)</tt> or equivalent call.
     * @see #isSubMatrix()
     */
    public long[] subMatrixFrom();

    /**
     * If this matrix is a {@link #subMatrix(long[], long[]) submatrix} of some <tt>parent</tt> matrix,
     * created by one of calls <tt>parent.subMatrix(...)</tt> or <tt>parent.subMatr(...)</tt>,
     * creates and returns a new Java array containing the ending position (exclusive) of this submatrix
     * in the parent one. The result will be equal to "<tt>to</tt>" argument of
     * {@link #subMatrix(long[], long[])} and {@link #subMatrix(long[], long[], ContinuationMode)} methods.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * @return low endpoints (inclusive) of all coordinates of this submatrix in its parent matrix.
     * @throws NotSubMatrixException if this object is not created by <tt>subMatrix(...)</tt>,
     *                               <tt>subMatr(...)</tt> or equivalent call.
     * @see #isSubMatrix()
     */
    public long[] subMatrixTo();

    /**
     * If this matrix is a {@link #subMatrix(long[], long[], Matrix.ContinuationMode) submatrix}
     * of some <tt>parent</tt> matrix,
     * created by one of calls <tt>parent.subMatrix(...)</tt> or <tt>parent.subMatr(...)</tt>,
     * returns the {@link ContinuationMode continuation mode}, used by this submatrix.
     * If this matrix is not a submatrix, throws {@link NotSubMatrixException}.
     *
     * <p>If the submatrix was created by
     * {@link #subMatrix(long[], long[], net.algart.arrays.Matrix.ContinuationMode)} or equivalent method,
     * the <tt>continuationMode</tt> argument, passed to that method, is returned.
     * If the submatrix was created by
     * {@link #subMatrix(long[], long[])} or equivalent method,
     * {@link ContinuationMode#NONE} constant is returned.
     *
     * @return low endpoints (inclusive) of all coordinates of this submatrix in its parent matrix.
     * @throws NotSubMatrixException if this object is not created by <tt>subMatrix(...)</tt>,
     *                               <tt>subMatr(...)</tt> or equivalent call.
     * @see #isSubMatrix()
     */
    public ContinuationMode subMatrixContinuationMode();

    /**
     * Returns a view ot this matrix, where the elements are reordered in some order "like"
     * in the specified matrix <tt>m</tt>.
     * In other words, the elements of the {@link #array() built-in array} of the returned matrix are
     * the same as the elements of the {@link #array() built-in array} of this one
     * (any changes of the elements of the returned matrix are reflected in this matrix, and vice-versa),
     * but the order of the elements can differ. The precise algorithm of reordering is not specified
     * and depends of the matrix <tt>m</tt>: this method tries to help algorithms, processing the same
     * or similar areas in both matrices, to provide maximal performance.
     *
     * <p>This method returns non-trivial results only if the matrix <tt>m</tt> is already a view of some other
     * matrix with some form of reordering elements, for example, if <tt>m</tt> is a {@link #isTiled() tiled} matrix.
     * In other case, this method just returns this instance.
     *
     * <p>In the current version of this package (if this instance was created by means of methods of this package),
     * this method is equivalent to the following:
     *
     * <pre>
     * m.{@link #isTiled()} ?
     * &#32;   thisInstance.{@link #tile() tile}(m.{@link #tileDimensions()}) :
     * &#32;   thisInstance;
     * </pre>
     *
     * <p>In future versions, it is possible that this method will recognize other forms of reordering matrix elements
     * and return non-trivial results for such <tt>m</tt> matrices.
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
     * @return  a view of this matrix with elements reordered in similar order, or a reference to this instance
     *          if <tt>m</tt> matrix is not reodered or this method does not "know" about the way of that reordering.
     * @see #isStructuredLike(Matrix)
     * @throws NullPointerException if <tt>m</tt> argument is <tt>null</tt>.
     */
    public Matrix<T> structureLike(Matrix<?> m);

    /**
     * Returns <tt>true</tt> if the elements of this matrix is ordered "alike" the elements
     * of the specified matrix <tt>m</tt>, in terms of {@link #structureLike(Matrix)} method.
     * "Ordered alike" does not mean that the dimensions of both matrices are equal, or that
     * the details of the structure are the same; it means only that both matrices use similar
     * reordering algorithms.
     *
     * <p>More precisely, {@link #structureLike(Matrix)} method returns this instance if and only if
     * this method returns <tt>true</tt>.
     *
     * <p>In the current version of this package (if this instance was created by means of methods of this package),
     * this method is equivalent to: <tt>thisInstance.{@link #isTiled()}==m.{@link #isTiled()}</tt>.
     *
     * @param m some matrix, probably a view of another matrix with reordered elements
     *          (for example, {@link #tile(long...) tiled}).
     * @return  whether this matrix is reordered alike <tt>m</tt>.
     * @throws NullPointerException if <tt>m</tt> argument is <tt>null</tt>.
     */
    public boolean isStructuredLike(Matrix<?> m);

    /**
     * Returns a view ot this matrix, where the elements are reordered by <i>tiles</i>: a grid of rectangular
     * regions (<i>tiles</i>), the sizes of which are specified by <tt>tileDim</tt> argument.
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
     * (<i>n</i><tt>={@link #dimCount()}</tt>) be coordinates of some element it the tiled matrix <b>T</b>,
     * that is located in <b>T</b><tt>.{@link #array() array()}</tt>
     * at the index <i>i</i>=<b>T</b><tt>.{@link #index(long...)
     * index}</tt>(<i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>).
     * This element is located in the original array <b>M</b><tt>.{@link #array() array()}</tt> at another index
     * <i>j</i>, which is calculated by the following algorithm.
     *
     * <ol>
     * <li>Let <i>d<sub>k</sub></i> = <b>M</b>.{@link #dim(int) dim}(<i>k</i>),
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: dimensions of this and returned matrix.</li>
     *
     * <li>Let <i>i'<sub>k</sub></i> = <i>i<sub>k</sub></i>%<tt>tileDim[<i>k</i>]</tt>,
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: <i>i'<sub>k</sub></i> are the coordinates of this element inside
     * the tile, containing it in <b>T</b> matrix.</li>
     *
     * <li>Let <i>s<sub>k</sub></i> = <i>i<sub>k</sub></i>&minus;<i>i'<sub>k</sub></i>,
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: <i>s<sub>k</sub></i> are the coordinates of the starting element
     * of the tile, containing this element in <b>T</b> matrix.</li>
     *
     * <li>Let <i>t<sub>k</sub></i> = <tt>min</tt>(<tt>tileDim[</tt><i>k</i><tt>]</tt>,
     * <i>d<sub>k</sub></i>&minus;<i>s<sub>k</sub></i>),
     * <i>k</i>=0,1,...,<i>n</i>&minus;1: <i>t<sub>k</sub></i> are the dimensions
     * of the tile, containing this element in <b>T</b> matrix. (Note that boundary tiles can be less
     * than <tt>tileDim</tt>, if dimensions of matrix are not divisible by corresponding dimensions of tiles.)</li>
     *
     * <li>Let <tt>previousVolume</tt> =
     * <i>d</i><sub>0</sub><i>d</i><sub>1</sub>...<i>d</i><sub><i>n</i>&minus;3</sub><i
     * class="dummy">d</i><sub><i>n</i>&minus;2</sub><i>s</i><sub><i>n</i>&minus;1</sub>
     * + <i>d</i><sub>0</sub><i>d</i><sub>1</sub>...<i>d</i><sub><i>n</i>&minus;3</sub><i
     * class="dummy">c</i><sub><i>n</i>&minus;2</sub><i>t</i><sub><i>n</i>&minus;1</sub>
     * + ... + <i>s</i><sub>0</sub><i>t</i><sub>1</sub>...<i
     * class="dummy">t</i><sub><i>n</i>&minus;2</sub><i>t</i><sub><i>n</i>&minus;1</sub>.
     * This complex formula returns the summary sizes of all tiles, that are fully located
     * in the source <b>T</b><tt>.{@link #array() array()}</tt> before the given element.
     * In 2-dimensional case, the formula is more simple:
     * <tt>previousVolume</tt> = <i>d</i><sub><i>x</i></sub><i>s</i><sub><i>y</i></sub>
     * + <i>s</i><sub><i>x</i></sub><i>t</i><sub><i>y</i></sub>.
     * </li>
     *
     * <li>Let <tt>indexInTile</tt> =
     * <i>i'</i><sub>0</sub> + <i>i'</i><sub>1</sub><i>t</i><sub>0</sub> + ...
     * + <i>i'</i><sub><i>n</i>&minus;1</sub><i>t</i><sub><i>n</i>&minus;2</sub>...<i>t</i><sub>0</sub>:
     * it is the index of the element with coordinates
     * <nobr><i>i'<sub>0</sub></i>,<i>i'<sub>1</sub></i>,...,<i>i'<sub>n&minus;1</sub></i></nobr>
     * in the built-in array of a little matrix, dimensions of which are equal to the tile dimensions.
     * </li>
     *
     * <li>The required index of the given element in the original array <b>M</b><tt>.{@link #array() array()}</tt>
     * is <nobr><i>j</i> = <tt>previousVolume</tt> + <tt>indexInTile</tt></nobr>.</li>
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
     * &mdash; any changes of the elements of the returned matrix are reflected in this matrix, and vice-versa.
     * The returned matrix is {@link #isImmutable() immutable} if, and only if,
     * the built-in array of this matrix does not implement {@link UpdatableArray}.
     * The {@link Array#asTrustedImmutable()} method
     * in the built-in array of the returned matrix is equivalent to {@link Array#asImmutable()},
     * and {@link Array#asCopyOnNextWrite()} method just returns the full copy of the array.
     *
     * @param tileDim dimensions of the tiles in the returned matrix (excepting the boundary tiles,
     *                which can be less).
     * @return        a tiled view of this matrix.
     * @throws NullPointerException      if <tt>tileDim</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>tileDim.length</tt is not equal to {@link #dimCount()},
     *                                   or if some <tt>tileDim[k]&lt;=0</tt>,
     *                                   or if the product of all tile dimensions <tt>tileDim[k]</tt>
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @see #tile()
     * @see #isTiled()
     * @see #tileDimensions()
     */
    public Matrix<T> tile(long... tileDim);

    /**
     * Returns a tiled view ot this matrix with some default dimensions of the tiles.
     * Equivalent to <tt>{@link #tile(long...) tile}(tileDim)</tt>, where all elements of <tt>tileDim</tt>
     * are equal to the default integer value, retrieved from the system property
     * "<tt>net.algart.arrays.matrixTile2D</tt>",
     * "<tt>net.algart.arrays.matrixTile3D</tt>"
     * "<tt>net.algart.arrays.matrixTile4D</tt>"
     * "<tt>net.algart.arrays.matrixTile5D</tt>" or
     * "<tt>net.algart.arrays.matrixTileND</tt>",
     * if the {@link #dimCount() number of dimensions} of this matrix is correspondingly 2, 3, 4, 5 or greater.
     * If there is no such property, or if it contains not a number,
     * or if some exception occurred while calling <tt>Long.getLong</tt>,
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
     * @throws IllegalArgumentException  if the product of all tile dimensions <tt>tileDim[k]</tt>
     *                                   is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>.
     * @see #tile(long...)
     * @see #isTiled()
     * @see #tileDimensions()
     */
    public Matrix<T> tile();

    /**
     * Not ready yet!
     *
     * @return        original non-tiled matrix.
     */
    public Matrix<T> tileParent();

    /**
     * Not ready yet!
     *
     * @return dimensions of the tile in this matrix, if it is tiled.
     */
    public long[] tileDimensions();

    /**
     * Not ready yet!
     *
     * @return <tt>true</tt> if and only if this matrix is tiled.
     */
    public boolean isTiled();

    /**
     * Equivalent to <tt>{@link #array()}.{@link Array#isImmutable() isImmutable()}</tt>.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @return <tt>true</tt> if this instance is immutable.
     */
    public boolean isImmutable();

    /**
     * Equivalent to <tt>{@link #array()}.{@link Array#isCopyOnNextWrite() isCopyOnNextWrite()}</tt>.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @return <tt>true</tt> if this instance is copy-on-next-write.
     */
    public boolean isCopyOnNextWrite();

    /**
     * Returns <tt>true</tt> if and only if the built-in AlgART array implements {@link DirectAccessible}
     * interface and <tt>(({@link DirectAccessible}){@link #array()}).{@link DirectAccessible#hasJavaArray()
     * hasJavaArray()}</tt> method returns <tt>true</tt>.
     *
     * <p>There is a guarantee that this method works very quickly.
     *
     * @return whether this matrix can be viewed as a Java array or a part of Java array.
     */
    public boolean isDirectAccessible();

    /**
     * Equivalent to <tt>{@link #array()}.{@link Array#flushResources(ArrayContext) flushResources(context)}</tt>.
     *
     * @param context the context of execution; may be <tt>null</tt>, then it will be ignored.
     */
    public void flushResources(ArrayContext context);

    /**
     * Equivalent to <tt>{@link #array()}.{@link Array#freeResources(ArrayContext) freeResources(context)}</tt>.
     *
     * @param context the context of execution; may be <tt>null</tt>, then it will be ignored.
     */
    public void freeResources(ArrayContext context);

    /**
     * Equivalent to <tt>{@link #array()}.{@link Array#freeResources(ArrayContext) freeResources(null)}</tt>.
     */
    public void freeResources();

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * a short description of the built-in AlgART array and all matrix dimensions.
     *
     * @return a brief string description of this object.
     */
    public String toString();

    /**
     * Returns the hash code of this matrix. The result depends on all elements of the built-in array
     * (as {@link Array#hashCode()} and all matrix dimensions.
     *
     * @return the hash code of this matrix.
     */
    public int hashCode();

    /**
     * Indicates whether some other matrix is equal to this one.
     * Returns <tt>true</tt> if and only if:<ol>
     * <li>the specified object is a matrix (i.e. implements {@link Matrix}),</li>
     * <li>both matrices have the same dimension count ({@link #dimCount()})
     * and the same corresponding dimensions;</li>
     * <li>the built-in AlgART arrays ({@link #array()}) are equal (see {@link Array#equals(Object)}).</li>
     * </ol>
     *
     * @param obj the object to be compared for equality with this matrix.
     * @return    <tt>true</tt> if the specified object is a matrix equal to this one.
     */
    public boolean equals(Object obj);
}
