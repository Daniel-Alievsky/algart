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

package net.algart.matrices.skeletons;

import net.algart.arrays.*;

/**
 * <p>Common 2-dimensional skeletonization algorithm of binary matrices, based on &le;8 thinning steps,
 * corresponding to all or some from 8 directions with the step 45 degree.
 * The following concrete skeletonization classes implements it:</p>
 *
 * <ul>
 * <li>{@link OctupleThinningSkeleton2D};</li>
 * <li>{@link WeakOctupleThinningSkeleton2D};</li>
 * <li>{@link Quadruple3x5ThinningSkeleton2D};</li>
 * <li>{@link StrongQuadruple3x5ThinningSkeleton2D}.</li>
 * </ul>
 *
 * <p>This interface extends {@link IterativeArrayProcessor} interface,
 * iteratively processing some bit matrix (<code>{@link Matrix}({@link UpdatableBitArray})</code>), named
 * <code>result</code> and usually passed to instantiation methods.
 * It is supposed, that in all implementations of this interface:</p>
 *
 * <ul>
 * <li>{@link #performIteration(net.algart.arrays.ArrayContext) performIteration(ArrayContext)}
 * method sequentially calls {@link #asThinning(int directionIndex)} method and copies its result to
 * the <code>result</code> matrix for <code>directionIndex=0,1,2,3,4,5,6,7</code>.
 * It means, that all "objects" in the matrix (areas filled by 1 elements)
 * are "thinned" 8 times: from left direction, from left-top diagonal direction, etc.
 * Depending on implementation,
 * {@link #performIteration(net.algart.arrays.ArrayContext) performIteration(ArrayContext)} may skip
 * calling {@link #asThinning asThinning} for some of these directions &mdash; you can check this
 * by {@link #isThinningRequired(int)} method.
 * </li>
 *
 * <li>{@link #done() done()} method returns <code>true</code> if the last iteration was unable to change the matrix:
 * all "objects" are already "thin".</li>
 *
 * <li>{@link #result() result()} method always returns the reference to the source matrix, passed to
 * instantiation methods of the inheritors.</li>
 * </ul>
 *
 * <p>All classes of this package, implementing this interface, guarantee that <b>8-connected "objects"
 * (areas filled by 1 elements) always stay 8-connected</b>.</p>
 *
 * <p>More precisely, let's consider an AlgART bit matrix
 * <code>{@link Matrix}&lt;{@link UpdatableBitArray}&gt;</code>, processed by this class.
 * The <i>8-connected object</i> is a connected component of the graph, built on the matrix,
 * where unit matrix elements are vertices, and <i>neighbour</i> unit elements are connected by edges.
 * Neighbour elements are 2 elements with coordinates (<i>x</i><sub>1</sub>, <i>y</i><sub>1</sub>)
 * and (<i>x</i><sub>2</sub>, <i>y</i><sub>2</sub>), where
 * max(|<i>x</i><sub><i>1</i></sub>&minus;<i>x</i><sub><i>2</i></sub>|,
 * |<i>y</i><sub><i>1</i></sub>&minus;<i>y</i><sub><i>2</i></sub>|) = 1,
 * i.e. every matrix element has 8 neighbours.
 * We can consider that the matrix contains images of some objects:
 * unit bits (1) are white pixels, belonging to objects, zero bits (0) are black pixels of the background.
 * Then our term "8-connected objects" describes the white objects, separated by black space.</p>
 *
 * <p>The state "8-connected objects always stay 8-connected" means the following.
 * Let <b>A</b> is the current <code>result</code> matrix and <b>A'</b> is either the result
 * {@link #asThinning(int)} method or the new <code>result</code> matrix after calling
 * {@link #performIteration(net.algart.arrays.ArrayContext) performIteration(ArrayContext)} method.
 * It is guaranteed that:</p>
 * <ol>
 * <li>the set of unit elements of the matrix <b>A'</b> is equal to or is a subset of the set of unit element
 * of the matrix <b>A</b>; in other words, the skeletonization always reduces objects and never expands them;
 * </li>
 * <li>if <b>C</b> is some 8-connected object in the matrix <b>A</b> and <b>C'</b> is a set of points (unit elements)
 * in the matrix <b>A'</b>, belonging to the area <b>C</b>, then <b>C'</b> is an empty set or 8-connected object.
 * </li>
 * </ol>
 *
 * <p>It is obvious that the same state is true if we performs any number of calls of
 * {@link #performIteration(net.algart.arrays.ArrayContext) performIteration} instead of one call.</p>
 *
 * <p>For most kinds of skeletons, the 2nd condition is more strong: the 8-connected object <b>C'</b> cannot be
 * an empty set. In other words, connected objects are never removed at all, they are only "thinned".
 * The only skeletons of this package, for which it is not true, are so called <i>topological</i> skeletons,
 * offered by {@link OctupleThinningSkeleton2D} and {@link WeakOctupleThinningSkeleton2D} classed
 * by the corresponding "<code>topological</code>" argument of the instantiation methods.</p>
 *
 * <p>The resulting "skeleton" (after finishing {@link #process()} method)
 * are usually "thin" enough (1-pixel lines), but some little not "thin" areas are possible.</p>
 *
 * <p>All classes of this package, implementing this interface, suppose that the processed matrix
 * is infinitely pseudo-cyclically continued, as well
 * {@link Matrices#asShifted Matrices.asShifted} method supposes it.
 * You can change this behavior by appending the source matrix with zero elements
 * by calling {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)} method,
 * where the dimensions of the "submatrix" are greater than dimensions of the source one by 1
 * and the <code>continuationMode</code> argument is {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT}.</p>
 *
 * <p>The classes, implementing this interface, are usually <b>thread-compatible</b>
 * and can be synchronized manually, if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public interface ThinningSkeleton extends IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> {

    /**
     * Returns <code>true</code> if and only if
     * {@link #performIteration(net.algart.arrays.ArrayContext) performIteration(ArrayContext)} method
     * really calls {@link #asThinning(int directionIndex)} for this direction
     * and copies its result to the <code>result</code> matrix.
     * It depends on the implementation of this interface.
     *
     * @param directionIndex the direction of thinning, from 0 to 7.
     * @return               whether the matrix should be thinned along this direction.
     * @throws IllegalArgumentException if <code>directionIndex</code> is not in 0..7 range.
     */
    boolean isThinningRequired(int directionIndex);

    // WARNING: SUN BUG IN javadoc UTILITY (1.6.0_04, 1.7.0-ea)!
    // Below we cannot write "{@link #result()}" - it leads to ClassCastException in javadoc.
    /**
     * Returns current {@link IterativeArrayProcessor#result() result()} matrix thinned along the given direction.
     * The result is "lazy": it is only a view of the current matrix. It is the basic method,
     * implementing the skeletonization algorithm.
     *
     * <p>Generally speaking, the "thinning" means removing elements
     * from the boundary of any "object" (area of the matrix filled by 1).
     * <code>directionIndex</code> specifies the "eroded side" of objects,
     * or the direction of thinning:<ul>
     * <li>0 means removing elements from the left, i.e. from the side (<i>x</i>&minus;1,<i>y</i>),</li>
     * <li>1 means "diagonal" removal from the side (<i>x</i>&minus;1,<i>y</i>&minus;1),</li>
     * <li>2 means removal from the side (<i>x</i>,<i>y</i>&minus;1),</li>
     * <li>3 means "diagonal" removal from the side (<i>x</i>+1,<i>y</i>&minus;1),</li>
     * <li>4 means removal from the right, i.e. from the side (<i>x</i>+1,<i>y</i>),</li>
     * <li>5 means "diagonal" removal from the side (<i>x</i>+1,<i>y</i>+1),</li>
     * <li>6 means removal from the side (<i>x</i>,<i>y</i>+1),</li>
     * <li>7 means "diagonal" removal from the side (<i>x</i>&minus;1,<i>y</i>+1).</li>
     * </ul>

     * <p>Some directions may be ignored by an implementation; in this case, the reference to the current
     * {@link IterativeArrayProcessor#result() result()} matrix is returned.
     * Note: there is no guarantee that this method ignores directions, for which
     * {@link #isThinningRequired(int directionIndex)} method returns <code>false</code>.
     *
     * @param directionIndex the direction of thinning, from 0 to 7.
     * @return               the thinned view if the current {@link #result()} matrix.
     * @throws IllegalArgumentException if <code>directionIndex</code> is not in 0..7 range.
     */
    Matrix<BitArray> asThinning(int directionIndex);
}
