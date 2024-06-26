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

package net.algart.matrices;

import net.algart.arrays.Matrix;
import net.algart.math.IRectangularArea;

import java.util.Map;

/**
 * <p><i>Abstract aperture matrix processor</i>: an algorithm, processing a group of
 * {@link Matrix <i>n</i>-dimensional matrices} and returning a group of resulting matrices,
 * where the value of every element of every resulting matrix depends only on
 * the elements of the source matrices in some aperture "around" the same position.
 * This aperture should be a subset of some rectangular area ({@link IRectangularArea});
 * this area (possibly different for different source matrices) is called <i>a dependence aperture</i>
 * of the aperture processor. All matrices must have same dimensions.
 * This interface is used, for example, together with {@link TiledApertureProcessorFactory} class.</p>
 *
 * <p>Below is more precise formal definition of the aperture matrix processors.
 * All classes, implementing this interface, should comply this definition.</p>
 *
 * <p>The main method of this interface, {@link #process(Map, Map) process},
 * works with some group of {@link Matrix AlgART matrices} <b>M</b><sub><i>i</i></sub>, <i>i</i>&isin;<i>Q</i>,
 * called the <i>arguments</i> or <i>source matrices</i>,
 * and a group of another AlgART matrices <b>M'</b><sub><i>j</i></sub>, <i>j</i>&isin;<i>R</i>,
 * called the <i>results</i> or <i>resulting matrices</i>.
 * Here the indexes <i>i</i> and <i>j</i> (elements of the index sets <i>Q</i> and <i>R</i>)
 * can be objects of any nature and should be represented by Java objects of the generic type <code>K</code> &mdash;
 * the generic argument of this interface (for example, <code>Integer</code> or <code>String</code>).
 * The indexes of arguments and results are also called their <i>keys</i>.
 * The group of the source arguments and the group of results are represented by <code>java.util.Map</code>,
 * more precisely, by a generic type <code>java.util.Map&lt;K, {@link Matrix}&lt;?&gt;&gt;</code>:</p>
 *
 * <pre>
 *     {@link #process(Map, Map)
 * process}(java.util.Map&lt;K, {@link Matrix}&lt;?&gt;&gt; dest, java.util.Map&lt;K, {@link Matrix}&lt;?&gt;&gt; src)
 * </pre>
 *
 * <p>The group <code>dest</code>of resulting matrices <b>M'</b><sub><i>j</i></sub> can be passed to
 * {@link #process(Map, Map) process} method, or can be formed by this method dynamically
 * (an empty map <code>dest</code> can be passed in this case),
 * or it is possible that a part of resulting matrices is passed in <code>dest</code> while calling the method
 * and other resulting matrices are added to <code>dest</code> map by the method itself.</p>
 *
 * <p>In all comments, the designations <b>M'</b><sub><i>j</i></sub>
 * and <i>R</i> means the group (<code>dest</code>) and the index set
 * (<code>dest.keySet()</code>) of resulting matrices
 * <i>after</i> the call of {@link #process(Map, Map) process} method.</p>
 *
 * <p>Some of the resulting matrices <b>M'</b><sub><i>j</i></sub> (but not the source arguments),
 * passed to {@link #process(Map, Map) process} method, can be {@code null} &mdash;
 * it means that {@link #process(Map, Map) process} method <i>must</i> create them itself.
 * (And it also <i>may</i> create some additional matrices <b>M'</b><sub><i>j</i></sub>, not contained
 * in <code>dest</code> while calling this method.)
 * All non-null resulting matrices <b>M'</b><sub><i>j</i></sub>, passed to the method in <code>dest</code>
 * argument, must be <i>updatable</i>, i.e. their {@link Matrix#array() built-in arrays}
 * must implement {@link net.algart.arrays.UpdatableArray} interface.
 * The dimensions of all source and non-null resulting matrices must be the same:
 * {@link Matrix#dimEquals(Matrix)} method must return <code>true</code> for any pair of them.
 * The sets of indexes of arguments <i>Q</i> and results <i>R</i>, as well as the dimensions of matrices,
 * may vary for different calls of {@link #process(Map, Map) process} method.
 * (Even the number of dimensions <i>n</i> may vary, but it is a rarer situation.)
 * Degenerated cases <i>Q</i>=&empty; (no arguments) and <i>R</i>=&empty; (no results) are allowed.
 * If <i>Q</i>=&empty; (no matrices are passed in <code>src</code> map) and
 * the <code>dest</code> map, passed to {@link #process(Map, Map) process} method,
 * either is empty or contains only {@code null} matrices,
 * then {@link #process(Map, Map) process} method usually does nothing.
 * See {@link #process(Map, Map) comments to that method} for additional details.</p>
 *
 * <p>For each source argument <b>M</b><sub><i>i</i></sub> this aperture processor defines
 * the corresponding <i>dependence aperture</i> <b>A</b><sub><i>i</i></sub>: a rectangular set of
 * integer points, represented by {@link IRectangularArea} class and returned by
 * {@link #dependenceAperture(Object) dependenceAperture(<i>i</i>)} method.
 * This aperture can depend only on the argument index <i>i</i> and <i>cannot vary for different calls</i>
 * of {@link #dependenceAperture(Object) dependenceAperture} method for the same processor.</p>
 *
 * <p>The goal of the main {@link #process(Map, Map) process} method is
 * to fill elements of the resulting matrices <b>M'</b><sub><i>j</i></sub>
 * (usually all elements of all <b>M'</b><sub><i>j</i></sub>, <i>j</i>&isin;<i>R</i>) on the base
 * of the elements of the source matrices <b>M</b><sub><i>i</i></sub>.
 * There should be a guarantee, that every element of any resulting <i>n</i>-dimensional matrix
 * <b>M'</b><sub><i>j</i></sub> with coordinates
 * <b>x</b> = (<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>&minus;1</sub>)
 * can depend only on elements of each source matrix <b>M</b><sub><i>i</i></sub> with coordinates</p>
 *
 * <blockquote>
 * <b>x</b>+<b>a</b> =
 * <i>x</i><sub>0</sub>+<i>a</i><sub>0</sub>,
 * <i>x</i><sub>1</sub>+<i>a</i><sub>1</sub>, ...,
 * <i>x</i><sub><i>n</i>&minus;1</sub>+<i>a</i><sub><i>n</i>&minus;1</sub>,
 * </blockquote>
 *
 * <p>where
 * <b>a</b> = (<i>a</i><sub>0</sub>, <i>a</i><sub>1</sub>, ..., <i>a</i><sub><i>n</i>&minus;1</sub>)
 * is one of points belonging to the corresponding dependence aperture
 * <b>A</b><sub><i>i</i></sub>={@link #dependenceAperture(Object) dependenceAperture(<i>i</i>)}.
 * Please draw attention that we use plus sing + in this definition, instead of more traditional minus sign &minus;
 * (used, for example, in specifications of {@link StreamingApertureProcessor}
 * or {@link net.algart.matrices.morphology.RankMorphology}).
 * In many cases, it is convenient to use {@link DependenceApertureBuilder} class to create
 * dependence apertures for an aperture processor.</p>
 *
 * <p>If this rule is violated, for example, if the aperture, returned by
 * {@link #dependenceAperture(Object) dependenceAperture} method, is too little (the results depend on elements
 * outside this aperture), then {@link #process(Map, Map) process} method still works,
 * but the results of processing can be incorrect.
 * If the aperture, returned by {@link #dependenceAperture(Object) dependenceAperture} method, is too large
 * (the results do not depend on most of elements in the aperture),
 * then {@link #process(Map, Map) process} method still works and the results are correct,
 * but excessively large aperture sizes can slow down the calculations.</p>
 *
 * <p>The {@link #process(Map, Map) process} method fills all resulting matrices.
 * But if some of the resulting matrices <b>M'</b><sub><i>j</i></sub>, passed via <code>dest</code> map
 * while calling this method, is {@code null}, then it <i>must</i> be automatically created.
 * Moreover, if some of the resulting matrices <b>M'</b><sub><i>j</i></sub> was not {@code null}
 * while calling this method, the method still <i>may</i> create new matrix for this key (index) <i>j</i>
 * and store it in <code>dest</code> map instead of the original matrix.
 * In addition, this method may <i>create</i> (and store in <code>dest</code> map) some new resulting matrices
 * with new keys (indexes), which were not contained in <code>dest</code> while calling the method.
 * But here must be the following guarantee: <i>the set of indexes j of the resulting matrix
 * <b>M'</b><sub>j</sub>, created and stored in <code>dest</code> map by this method in addition to
 * existing key/value pairs in <code>dest</code> map,
 * cannot vary for different calls of {@link #process(Map, Map) process}
 * method of the same instance of the processor</i>.</p>
 *
 * <p>If {@link #process(Map, Map) process} method allocates some resulting matrix,
 * then all these matrices must be created with the same dimensions as all the source and non-null resulting
 * matrices, passed to the method.
 * The type of elements of the newly created matrix is selected by some rules, depending on the implementation.
 * Typical example &mdash; it is selected to be equal to the element type of some source matrices.
 * But here must be the following guarantee: <i>the element type of the resulting matrix <b>M'</b><sub>j</sub>
 * with the given index j cannot vary for different calls of {@link #process(Map, Map) process}
 * method of the same processor, if the element types of all source matrices <b>M</b><sub>i</sub>
 * do not vary while these calls</i>. In other words, a concrete instance of the processor may
 * select the element type of newly created matrices only on the base of the element types of the arguments,
 * but not, for example, on the base of the matrix dimensions.</p>
 *
 * @param <K> the type of the keys.
 * @author Daniel Alievsky
 */
public interface ApertureProcessor<K> {

    /**
     * Main method of the aperture processor, that performs processing the source matrices <code>src</code>
     * with saving results in the resulting matrices <code>dest</code>.
     * The source matrices
     * <b>M</b><sub><i>i</i></sub>, <i>i</i>&isin;<i>Q</i>
     * are passed in <code>src</code> map: <b>M</b><sub><i>i</i></sub>=<code>src.get(<i>i</i>)</code>.
     * The resulting matrices
     * <b>M'</b><sub><i>j</i></sub>, <i>j</i>&isin;<i>R</i>,
     * are passed or, maybe, dynamically created and stored in <code>dest</code> map:
     * <b>M'</b><sub><i>j</i></sub>=<code>dest.get(<i>j</i>)</code>.
     * So, the sets of indexes <i>Q</i> and <i>R</i> are the Java sets
     * <code>src.keySet()</code> and <code>dest.keySet()</code> (after the call of this method).
     * See {@link ApertureProcessor comments to ApertureProcessor} for more details.
     *
     * <p>Note 1: some of the source matrices may be references to the same object
     * (<code>src.get(i)==src.get(j)</code>) &mdash; such situations must be processed correctly by this method.
     *
     * <p>Note 2: this method must not modify <code>src</code> map and the source matrices, contained in this map.
     *
     * <p>Note 3: this method must not remove key/value pairs from <code>dest</code> map
     * (but may add new resulting matrices).
     *
     * <p>Note 4: if some resulting matrix <b>M'</b><sub><i>j</i></sub>, passed in <code>dest</code> map
     * as <code>dest.get(<i>j</i>)</code>, is {@code null} while calling this method,
     * it <i>must</i> be automatically created and stored back in <code>dest</code>
     * map for the same key (index) <i>j</i>.
     * (Besides this, this method may create and store in <code>dest</code> another additional resulting matrices
     * with another indexes.)
     *
     * <p>Note 5: if this method creates some resulting matrices itself, then <code>dest</code> map should be mutable.
     * The created resulting matrices are saved in this map by <code>dest.put(K,...)</code> call.
     *
     * <p>Note 6: if this method stores some resulting matrices it <code>dest</code> map, they must have the same
     * dimensions as all matrices, passed in <code>src</code> and <code>dest</code> maps while calling the method.
     * This method must not store {@code null} values in <code>dest</code> map.
     *
     * <p>Note 7: resulting matrices, created by this method instead of {@code null} values in <code>dest</code> map
     * or in addition to the existing matrices in <code>dest</code> map,
     * <i>may</i> be not updatable, i.e. it is possible that their {@link Matrix#array() built-in arrays} will not
     * implement {@link net.algart.arrays.UpdatableArray} interface.
     * The simplest example: the algorithm may return, as a result (saved in <code>dest</code>),
     * some lazy view of one of the source matrices <code>src.get(i)</code>
     * or even just a reference to a source matrix <code>src.get(i)</code>.
     * (But if this object is a tiled processors, returned by
     * {@link TiledApertureProcessorFactory#tile(ApertureProcessor)} method, then it is impossible:
     * in this case, all resulting matrices, created by this method and stored in
     * <code>dest</code> map, are always updatable, and their {@link Matrix#array() built-in arrays}
     * implement {@link net.algart.arrays.UpdatableArray}.)
     *
     * <p>Note 8: unlike this, all non-null values, present in <code>dest</code> map while calling this method,
     * <i>must</i> be updatable &mdash; because this method may need to store results in them.
     * It is true even if this method does not use the old non-null values
     * <b>M'</b><sub><i>j</i></sub> and replace them with newly created matrices &mdash;
     * even in this case the method may check, that they are not updatable, and throw an exception if so.
     *
     * <p>Note 9: the sets of indexes of arguments <i>Q</i> and results <i>R</i>,
     * as well as the dimensions of matrices, may vary for different calls of this method.
     * (Even the number of dimensions may vary in some implementations, but it is a rarer situation.
     * The tiled processors, returned by {@link TiledApertureProcessorFactory#tile(ApertureProcessor)} method,
     * work only with a fixed number of dimensions, equal to {@link TiledApertureProcessorFactory#dimCount()}.)
     *
     * <p>Note 10: degenerated cases <i>Q</i>=&empty; (<code>src.isEmpty()</code>: no arguments) and
     * <i>R</i>=&empty; (<code>dest.isEmpty()</code>: no results) are allowed.
     * If <i>Q</i>=<i>R</i>=&empty;, as well as if <i>Q</i>=&empty; and
     * the <code>dest</code> map, passed to this method,
     * either is empty or contains only {@code null} matrices,
     * then this method usually does nothing &mdash;
     * it is really true for the tiled processors, returned by
     * {@link TiledApertureProcessorFactory#tile(ApertureProcessor)} method,
     * but it is not a strict requirement for other implementations.
     *
     * @param dest the resulting matrices: the result <b>M'</b><sub><i>j</i></sub>
     *             will be the value <code>dest.get(<i>j</i>)</code> of this map after calling this method.
     * @param src  the source arguments: the argument <b>M</b><sub><i>i</i></sub> is the value
     *             <code>src.get(<i>i</i>)</code> of this map.
     * @throws NullPointerException                    if <code>dest</code> or <code>src</code> argument
     *                                                 is {@code null},
     *                                                 or if one of values in <code>src</code> map is {@code null}.
     * @throws IllegalArgumentException                if some values in <code>dest</code> map are not {@code null}
     *                                                 and, at the same time, their {@link Matrix#array()
     *                                                 built-in arrays} are not
     *                                                 {@link net.algart.arrays.UpdatableArray updatable};
     *                                                 <code>ClassCastException</code> can be also thrown in this case
     *                                                 instead of this exception.
     * @throws net.algart.arrays.SizeMismatchException if some matrices among the source arguments (values of
     *                                                 <code>src</code> map) or non-null resulting matrices
     *                                                 (values of <code>dest</code> map), passed while calling this
     *                                                 method, have different dimensions. It is a typical run-time
     *                                                 exception in this case, and it is really thrown by the tiled
     *                                                 processors, created by
     *                                                 {@link TiledApertureProcessorFactory#tile(ApertureProcessor)}
     *                                                 method; but other implementations are permitted to throw
     *                                                 another exceptions in this case.
     */
    void process(Map<K, Matrix<?>> dest, Map<K, Matrix<?>> src);

    /**
     * Returns the dependence aperture <b>A</b><sub><i>i</i></sub>
     * for the source matrix <b>M</b><sub><i>i</i></sub> with the given index
     * <i>i</i>=<code>srcMatrixKey</code>.
     * See {@link ApertureProcessor comments to ApertureProcessor} for more details.
     *
     * <p>This method must return correct result for any key, which can appear and can be processed in
     * {@link #process(Map, Map) process} method as a key in its <code>src</code> map.
     * If <code>srcMatrixKey</code> argument has some "impossible" value, the result is not specified.
     *
     * <p>This method should work quickly and must never throw exceptions.
     *
     * @param srcMatrixKey the index (key) of the source matrix.
     * @return the dependence aperture of the processing algorithm for this source matrix.
     */
    IRectangularArea dependenceAperture(K srcMatrixKey);
}
