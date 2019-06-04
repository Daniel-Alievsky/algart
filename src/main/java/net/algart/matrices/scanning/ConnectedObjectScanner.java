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

package net.algart.matrices.scanning;

import net.algart.arrays.*;

/**
 * <p>Connected objects scanner: the class performing scanning and clearing connected objects,
 * "drawn" on some n-dimensional updatable bit matrix.</p>
 *
 * <p>More precisely, let's consider an AlgART bit matrix
 * (<tt>{@link Matrix}&lt;?&nbsp;extends&nbsp;{@link UpdatableBitArray}&gt;</tt>).
 * The <i>connected object</i> is a connected component of the graph, built on the matrix,
 * where unit matrix elements are vertices, and <i>neighbour</i> unit elements are connected by edges.
 * In 2D case, we can consider that the matrix contains images of some objects:
 * unit bits (1) are white pixels, belonging to objects, zero bits (0) are black pixels of the background.
 * Then our term "connected objects" describes the white objects, separated by black space.</p>
 *
 * <p>We define two kinds of connectivity: <i>{@link ConnectivityType#STRAIGHT_ONLY straight}</i>
 * and <i>{@link ConnectivityType#STRAIGHT_AND_DIAGONAL straight-and-diagonal}</i>. In the first case
 * (<i>straight</i> connectivity), two unit elements with coordinates
 * <nobr><i>i</i><sub>0</sub>, <i>i</i><sub>1</sub>, ..., <i>i</i><sub><i>n</i>-1</sub></nobr> and
 * <nobr><i>j</i><sub>0</sub>, <i>j</i><sub>1</sub>, ..., <i>j</i><sub><i>n</i>-1</sub></nobr>
 * are <i>neighbours</i> if one from the coordinates differs by 1, but all other coordinates are equal:</p>
 *
 * <blockquote><big>&sum;&nbsp;</big>|<i>i</i><sub><i>k</i></sub>&minus;<i>j</i><sub><i>k</i></sub>|=1</blockquote>
 *
 * <p>For 2D matrices, this connectivity kind is also known as "4-connectivity".
 * It the second case (<i>straight-and-diagonal</i> connectivity), two unit elements
 * are <i>neighbours</i> if several (at least one) from their coordinates differ by 1
 * and all other coordinates are equal:</p>
 *
 * <blockquote>max&nbsp;(|<i>i</i><sub><i>k</i></sub>&minus;<i>j</i><sub><i>k</i></sub>|)=1</blockquote>
 *
 * <p>For 2D matrices, this connectivity kind is also known as "8-connectivity".
 * The connectivity kind is described by {@link ConnectivityType} class.</p>
 *
 * <p>The instance of this class works with some concrete bit matrix
 * (<tt>{@link Matrix}&lt;?&nbsp;extends&nbsp;{@link UpdatableBitArray}&gt;</tt>),
 * named <i>the scanned matrix</i>, and with some concrete <i>connectivity type</i> ({@link ConnectivityType}).
 * Both the scanned matrix and the connectivity type are specified while creating the instance.</p>
 *
 * <p>This class allows to visit all elements of the scanner matrix,
 * belonging to one connected object, by its main
 * {@link #clear(ArrayContext, ConnectedObjectScanner.ElementVisitor, long[], boolean) clear} method.
 * "Visiting" means that this method calls {@link ConnectedObjectScanner.ElementVisitor#visit(long[], long)}
 * method for every visited element, passing the index of the element
 * in the {@link Matrix#array() underlying array} of the matrix to that method,
 * maybe, together with its coordinates in the matrix
 * (if getting coordinates does not slow down the scanning algorithm).</p>
 *
 * <p>Besides, the {@link #clear clear} method clears (sets to 0) all visited elements in the bit matrix.
 * But note: actual clearing may be not performed if <tt>forceClearing</tt> argument of this method
 * is <tt>false</tt>.</p>
 *
 * <p>There are following methods allowing to create the instance of this class:</p>
 *
 * <ul>
 * <li>{@link #getBreadthFirstScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #getDepthFirstScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #getStacklessDepthFirstScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #getUncheckedBreadthFirstScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #getUncheckedDepthFirstScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #getUncheckedStacklessDepthFirstScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #clone()}.</li>
 * </ul>
 *
 * <p>You can switch to another scanned bit matrix with the same dimensions by {@link #matrix(Matrix)} method.</p>
 *
 * <p>You can use this instance (call its {@link #clear clear} method)
 * many times to scan different connected objects at one matrix.
 * You may use {@link #nextUnitBit(long[])} method to find next connected object
 * after scanning and clearing the previous one.</p>
 *
 * <p>However, you <b>must not</b> use this instance after any modifications in the scanned matrix,
 * performed by an external code.
 * If you modify the matrix, you must create new instance of this class after this.</p>
 *
 * <p>Note: this class works <b>much faster</b> (in several times)
 * if the scanned matrix is created by {@link SimpleMemoryModel}.
 * So, if the matrix is not created by {@link SimpleMemoryModel} and is not too large,
 * we recommend to create its clone by {@link SimpleMemoryModel} and use this class for the clone.</p>
 *
 * <p>Note: this class cannot process matrices with too large number of dimensions.
 * The maximal allowed number of dimensions is {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS},
 * that is more than enough for most situations.</p>
 *
 * <p>This class does not use multithreading optimization, unlike
 * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} and similar methods.
 * In other words, all methods of this class are executed in the current thread.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithread access is necessary.
 * However, usually there are no reasons to use the same instance of this class in different threads:
 * usually there is much better idea to create a separate instance for every thread.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public abstract class ConnectedObjectScanner implements Cloneable {

    /**
     * <p>Visitor of matrix elements. Used by {@link ConnectedObjectScanner} class.</p>
     */
    public static interface ElementVisitor {
        /**
         * Method, called by
         * {@link ConnectedObjectScanner#clear(ArrayContext, ConnectedObjectScanner.ElementVisitor, long[], boolean)}
         * for every visited unit element of the scanned matrix.
         *
         * <p>Important: <tt>coordinatesInMatrix</tt> argument <b>can be <tt>null</tt></b> here!
         * So, if you need to know the coordinates, you must check this argument and use
         * {@link Matrix#coordinates(long, long[])} method (or equivalent code) when it is <tt>null</tt>.
         *
         * <p>If <tt>coordinatesInMatrix</tt> is not <tt>null</tt>,
         * this method may modify elements of the passed <tt>coordinatesInMatrix</tt>:
         * it will not affect to the behavior of the scanning algorithms.
         *
         * @param coordinatesInMatrix the coordinates of the element in the bit matrix or, maybe, <tt>null</tt>.
         * @param indexInArray        the index of this element in the underlying array.
         */
        public void visit(long[] coordinatesInMatrix, long indexInArray);
    }

    /**
     * <p>The simplest implementation of {@link ElementVisitor} interface.
     * It is also a good example for possible cloning in your code.</p>
     *
     * <p>This class works with some bit matrix, named <i>mask</i> and specified in the constructor's argument.
     * It should have the same dimensions, as the matrix, scanned by the connected objects scanner:
     * {@link ConnectedObjectScanner#matrix()}.
     * (If this condition is not fulfilled, {@link #visit visit} method can throw unexpected
     * <tt>IndexOutOfBoundException</tt>.)</p>
     *
     * <p>The only thing, performed by {@link #visit visit} method of this class, is increasing
     * some internal <tt>long</tt> counter by 1. You can read this counter by {@link #counter()} method.
     * This counter is zero after instantiating this object and can be cleared by {@link #reset()} method.</p>
     *
     * <p>So, this class allows to count the number of unit elements of the mask,
     * lying at the same positions as the elements of some connected object of the scanned matrix.</p>
     *
     * <p>This class is not thread-safe, but <b>is thread-compatible</b>
     * and can be synchronized manually, if multithread access is necessary.</p>
     */
    public static class MaskElementCounter implements ConnectedObjectScanner.ElementVisitor {
        private final Matrix<? extends BitArray> mask;
        private final BitArray array;

        /**
         * The internal counter, stored in this object and returned by {@link #counter()} method.
         * This counter is increased by 1 for unit elements of the mask by {@link #visit(long[], long)}
         * method and is cleared to zero by {@link #reset()} method.
         * The default value after creating a new instance is zero.
         */
        protected long counter = 0;

        /**
         * Creates new instance of this class with the specified <i>mask</i> matrix.
         *
         * @param mask the mask matrix, unit elements of which (corresponding the some unit elements
         *             of the scanned matrix) should be counted.
         * @throws NullPointerException if the argument is <tt>null</tt>.
         */
        public MaskElementCounter(Matrix<? extends BitArray> mask) {
            if (mask == null)
                throw new NullPointerException("Null mask");
            this.mask = mask;
            this.array = mask.array();
        }

        /**
         * This implementation increases the internal {@link #counter counter} by 1,
         * if the bit of the mask with the specified coordinates is 1.
         *
         * <p>More precisely, this method does the following:
         *
         * <pre>
         *     if ({@link #mask() mask()}.array().getBit(indexInArray)) {
         *         {@link #counter counter}++;
         *     }
         * </pre>
         *
         * @param coordinatesInMatrix the coordinates of the element in the bit matrix or, maybe, <tt>null</tt>
         *                            (not used by this implementation).
         * @param indexInArray        the index of this element in the underlying array.
         */
        public void visit(long[] coordinatesInMatrix, long indexInArray) {
            if (array.getBit(indexInArray)) {
                counter++;
            }
        }

        /**
         * Returns the reference to mask matrix. The result is identical to the argument of the constructor.
         *
         * @return the reference to mask matrix, passed via constructor.
         */
        public Matrix<? extends BitArray> mask() {
            return mask;
        }

        /**
         * Resets the internal {@link #counter counter} to 0.
         *
         * <p>Usually this method should be called before scanning new connected object via
         * {@link ConnectedObjectScanner#clear(ArrayContext, ConnectedObjectScanner.ElementVisitor, long[], boolean)}
         * method.
         */
        public void reset() {
            counter = 0;
        }

        /**
         * Returns the internal counter: the value of {@link #counter} field.
         *
         * @return the current value of the counter.
         */
        public long counter() {
            return counter;
        }
    }

    private static final long MAX_TEMP_JAVA_MEMORY = Arrays.SystemSettings.maxTempJavaMemory();
    private static final long MAX_TEMP_JAVA_INTS = Math.min(MAX_TEMP_JAVA_MEMORY / 4, Integer.MAX_VALUE / 2);
    // Integer.MAX_VALUE / 2 allows to be on the safe side while int calculations
    private static final long MAX_TEMP_JAVA_LONGS = MAX_TEMP_JAVA_INTS / 2;

    private static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, 262144);
    private static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, 262144);

    Matrix<? extends UpdatableBitArray> matrix;
    UpdatableBitArray array;
    final ConnectivityType connectivityType;
    final long[] dimensions;
    final int dimCount;
    final long arrayLength;
    final byte[][] coordShifts;
    final long[] xShifts, yShifts;
    final long[] indexShifts;
    final int apertureSize;
    final int[] intIndexShifts;
    ArrayContext context;
    MemoryModel mm;
    ElementVisitor elementVisitor;
    long[] coordinates;
    long index;
    boolean forceClearing;
    long maxUsedMemory = 0;
    Matrix<UpdatableBitArray> workMemory = null;

    private ConnectedObjectScanner(
        Matrix<? extends UpdatableBitArray> matrix,
        ConnectivityType connectivityType)
    {
        if (matrix == null)
            throw new NullPointerException("Null matrix argument");
        if (connectivityType == null)
            throw new NullPointerException("Null connectivityType argument");
        this.dimensions = matrix.dimensions();
        this.dimCount = dimensions.length;
        if (dimCount > Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS)
            throw new IllegalArgumentException(ConnectedObjectScanner.class
                + " cannot process a matrix with more than "
                + Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + " dimensions (" + matrix + ")");
        this.matrix = matrix;
        this.connectivityType = connectivityType;
        this.array = matrix.array();
        this.arrayLength = this.array.length();
        this.coordShifts = connectivityType.apertureShifts(dimCount);
        this.apertureSize = coordShifts.length;
        this.xShifts = new long[apertureSize];
        this.yShifts = new long[apertureSize];
        this.indexShifts = new long[apertureSize];
        this.intIndexShifts = new int[apertureSize];
        long[] temp = new long[dimCount];
        for (int k = 0; k < apertureSize; k++) {
            for (int j = 0; j < dimCount; j++) {
                temp[j] = coordShifts[k][j];
            }
            xShifts[k] = coordShifts[k][0];
            yShifts[k] = coordShifts[k][1];
            indexShifts[k] = matrix.uncheckedIndex(temp);
            intIndexShifts[k] = (int) indexShifts[k];
        }
        this.coordinates = new long[dimCount];
    }

    /**
     * Creates an instance of this class, implementing the classic breadth-first scanning algorithm.
     *
     * <p>In this case, the {@link #clear clear} method uses a queue,
     * stored in an AlgART array, created via the memory model
     * returned by the {@link ArrayContext array context}.
     * While the queue is little enough, the algorithm stores it in a usual Java array instead of AlgART array.
     *
     * <p>In typical situations, the queue stays very little (several kilobytes).
     *
     * <p>It is a good choice for most situations. But
     * {@link #getStacklessDepthFirstScanner(Matrix, ConnectivityType)} method
     * usually works faster for little matrices: see comments to that method.
     *
     * @param matrix           the matrix that will be scanned and cleared by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if <tt>matrix</tt> or <tt>connectivityType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}
     *                                  &gt; {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     * @see #getUncheckedBreadthFirstScanner(Matrix, ConnectivityType)
     */
    public static ConnectedObjectScanner getBreadthFirstScanner(
        Matrix<? extends UpdatableBitArray> matrix, ConnectivityType connectivityType)
    {
        return new BreadthFirstScanner(matrix, connectivityType);
    }

    /**
     * Creates an instance of this class, implementing the classic depth-first scanning algorithm.
     *
     * <p>In this case, the {@link #clear clear} method uses a stack,
     * stored in an AlgART array, created via the memory model
     * returned by the {@link ArrayContext array context}.
     * While the stack is little enough, the algorithm stores it in a usual Java array instead of AlgART array.
     *
     * <p>Unlike the {@link #getBreadthFirstScanner breadth-first} search,
     * this algorithm often requires large stack. If the matrix contains large areas ("objects"),
     * filled by 1, the length of stack is comparable with the number of unit elements in such areas,
     * if the worst case (1-filled matrix) &mdash; the total number of matrix elements.
     * Every stack element require several <tt>long</tt> values, so, the total occupied work memory
     * can be in 100 and more times larger, than the memory occupied by the source bit matrix.
     *
     * <p>The main advantage of this method is the well-specified order of visiting elements,
     * defined by the depth-first search. It can be a good choice for scanning thin "skeletons",
     * when the connectivity graph is a tree.
     *
     * @param matrix           the matrix that will be scanned and cleared by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if <tt>matrix</tt> or <tt>connectivityType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}
     *                                  &gt; {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     * @see #getUncheckedDepthFirstScanner(Matrix, ConnectivityType)
     */
    public static ConnectedObjectScanner getDepthFirstScanner(
        Matrix<? extends UpdatableBitArray> matrix, ConnectivityType connectivityType)
    {
        return new DepthFirstScanner(matrix, connectivityType);
    }

    /**
     * Creates an instance of this class, implementing the classic depth-first scanning algorithm,
     * but not using an explicit stack.
     *
     * <p>In this case, the first call of {@link #clear clear} method allocates a temporary work matrix
     * with the same dimensions as the passed one. Then this matrix is used for saving information,
     * necessary to perform correct depth-first search. The element type of the work matrix are <tt>byte</tt>,
     * if <tt>connectivityType.{@link ConnectivityType#numberOfNeighbours(int)
     * numberOfNeighbours}(matrix.{@link Matrix#dimCount() dimCount()})&lt;128</tt>,
     * or <tt>short</tt> in other cases (very exotic situation).
     * If the total number of matrix elements
     * (<nobr><tt>matrix.{@link Matrix#array() array()}.{@link Array#length() length()}</tt></nobr>)
     * is not greater than {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()},
     * then the work matrix is allocated by {@link SimpleMemoryModel}.
     * In other case, it is allocated by
     * <nobr><tt>context.{@link ArrayContext#getMemoryModel() getMemoryModel()}</tt></nobr>
     * (<tt>context</tt> is the argument of {@link #clear clear} method).
     *
     * <p>This is the only algorithm, that always uses the constant amount of memory: in 8 times larger
     * than the source bit matrix occupies (in 16 times for high number of dimensions).
     * If the work matrix is allocated by {@link SimpleMemoryModel},
     * it is the quickest algorithm and the best choice in most situations.
     * But if the matrix is large enough
     * (<nobr>{@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()}</nobr>
     * or more elements) and the <tt>context</tt> argument of {@link #clear clear} method
     * offers non-simple memory model, the {@link #getBreadthFirstScanner breadth-first scanning algorithm}
     * is usually better choice.
     *
     * <p>Please note: in this case the first call of {@link #clear clear} method requires essential time
     * for allocating and initializing work matrix. If you need to scan only one connected object,
     * {@link #getBreadthFirstScanner breadth-first scanning algorithm} will probably be better choice.
     *
     * @param matrix           the matrix that will be scanned and cleared by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if <tt>matrix</tt> or <tt>connectivityType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}
     *                                  &gt; {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     * @see #getUncheckedStacklessDepthFirstScanner(Matrix, ConnectivityType)
     */
    public static ConnectedObjectScanner getStacklessDepthFirstScanner(
        Matrix<? extends UpdatableBitArray> matrix, ConnectivityType connectivityType)
    {
        return new StacklessDepthFirstScanner(matrix, connectivityType, true);
    }


    /*Repeat() UncheckedBreadthFirst(Scanner\(\w+,\s*\w+)\) ==>
               UncheckedDepthFirst$1),,StacklessDepthFirst$1, false) ;;
               BreadthFirstScanner ==> DepthFirstScanner,,StacklessDepthFirstScanner
     */

    /**
     * An analog of {@link #getBreadthFirstScanner} method, returning the instance, which works correctly
     * only if all matrix elements with zero and maximal coordinates are zero.
     *
     * <p>More precisely, this method may be used only if one of the following conditions is complied:
     * <ol>
     * <li>each matrix bit with coordinates
     * <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>, where at least one
     * <i>i<sub>j</sub></i>=0 or <i>i<sub>j</sub></i>=<tt>matrix.{@link Matrix#dim(int) dim}(<i>j</i>)-1</tt>,
     * is zero;</li>
     * <li>or each matrix bit with coordinates
     * <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>, where at least one
     * <i>i<sub>j</sub></i>=<tt>matrix.{@link Matrix#dim(int) dim}(<i>j</i>)-1</tt>,
     * is zero, and also each matrix bit with last coordinate
     * <i>i<sub>n&minus;1</sub></i>=0 or <i>i<sub>n&minus;1</sub></i>=1 is zero
     * (first two lines in the 2-dimensional case).
     * </li>
     * </ol>
     * <p>Here <i>n</i>=<tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is the number of matrix dimensions.
     * If both conditions are not complied, the {@link #clear clear} method of the returned instance
     * can visit (and clear) some extra elements or can throw unexpected <tt>IndexOutOfBoundsException</tt>
     * while scanning. (And it is the only undesirable effect; no other data will be damaged,
     * no invariants will be violated.)
     *
     * <p>The scanner, returned by this method, works faster than the scanner,
     * returned by {@link #getBreadthFirstScanner(Matrix, ConnectivityType)}.
     * So, if possible, we recommend to provide the required condition and to use this method.
     * The simplest way to provide the necessary 1st condition
     * is the following call:
     *
     * <pre>
     * matrix.{@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     * subMatrix}(from, to, {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT});
     * </pre>
     *
     * where all <tt>from[k]==-1</tt> and <tt>to[k]=matrix.{@link Matrix#dim(int) dim}(k)+1</tt>.
     * Note: please copy the matrix, created by this
     * {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode) subMatrix} call,
     * to some newly created bit matrix,
     * in other case the performance will not be improved (because access to each element of the submatrix is slow).
     *
     * @param matrix           the matrix that will be scanned and cleared by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if <tt>matrix</tt> or <tt>connectivityType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}
     *                                  &gt; {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     */
    public static ConnectedObjectScanner getUncheckedBreadthFirstScanner(
        Matrix<? extends UpdatableBitArray> matrix, ConnectivityType connectivityType)
    {
        return new UncheckedBreadthFirstScanner(matrix, connectivityType);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * An analog of {@link #getDepthFirstScanner} method, returning the instance, which works correctly
     * only if all matrix elements with zero and maximal coordinates are zero.
     *
     * <p>More precisely, this method may be used only if one of the following conditions is complied:
     * <ol>
     * <li>each matrix bit with coordinates
     * <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>, where at least one
     * <i>i<sub>j</sub></i>=0 or <i>i<sub>j</sub></i>=<tt>matrix.{@link Matrix#dim(int) dim}(<i>j</i>)-1</tt>,
     * is zero;</li>
     * <li>or each matrix bit with coordinates
     * <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>, where at least one
     * <i>i<sub>j</sub></i>=<tt>matrix.{@link Matrix#dim(int) dim}(<i>j</i>)-1</tt>,
     * is zero, and also each matrix bit with last coordinate
     * <i>i<sub>n&minus;1</sub></i>=0 or <i>i<sub>n&minus;1</sub></i>=1 is zero
     * (first two lines in the 2-dimensional case).
     * </li>
     * </ol>
     * <p>Here <i>n</i>=<tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is the number of matrix dimensions.
     * If both conditions are not complied, the {@link #clear clear} method of the returned instance
     * can visit (and clear) some extra elements or can throw unexpected <tt>IndexOutOfBoundsException</tt>
     * while scanning. (And it is the only undesirable effect; no other data will be damaged,
     * no invariants will be violated.)
     *
     * <p>The scanner, returned by this method, works faster than the scanner,
     * returned by {@link #getDepthFirstScanner(Matrix, ConnectivityType)}.
     * So, if possible, we recommend to provide the required condition and to use this method.
     * The simplest way to provide the necessary 1st condition
     * is the following call:
     *
     * <pre>
     * matrix.{@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     * subMatrix}(from, to, {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT});
     * </pre>
     *
     * where all <tt>from[k]==-1</tt> and <tt>to[k]=matrix.{@link Matrix#dim(int) dim}(k)+1</tt>.
     * Note: please copy the matrix, created by this
     * {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode) subMatrix} call,
     * to some newly created bit matrix,
     * in other case the performance will not be improved (because access to each element of the submatrix is slow).
     *
     * @param matrix           the matrix that will be scanned and cleared by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if <tt>matrix</tt> or <tt>connectivityType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}
     *                                  &gt; {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     */
    public static ConnectedObjectScanner getUncheckedDepthFirstScanner(
        Matrix<? extends UpdatableBitArray> matrix, ConnectivityType connectivityType)
    {
        return new UncheckedDepthFirstScanner(matrix, connectivityType);
    }


    /**
     * An analog of {@link #getStacklessDepthFirstScanner} method, returning the instance, which works correctly
     * only if all matrix elements with zero and maximal coordinates are zero.
     *
     * <p>More precisely, this method may be used only if one of the following conditions is complied:
     * <ol>
     * <li>each matrix bit with coordinates
     * <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>, where at least one
     * <i>i<sub>j</sub></i>=0 or <i>i<sub>j</sub></i>=<tt>matrix.{@link Matrix#dim(int) dim}(<i>j</i>)-1</tt>,
     * is zero;</li>
     * <li>or each matrix bit with coordinates
     * <i>i<sub>0</sub></i>,<i>i<sub>1</sub></i>,...,<i>i<sub>n&minus;1</sub></i>, where at least one
     * <i>i<sub>j</sub></i>=<tt>matrix.{@link Matrix#dim(int) dim}(<i>j</i>)-1</tt>,
     * is zero, and also each matrix bit with last coordinate
     * <i>i<sub>n&minus;1</sub></i>=0 or <i>i<sub>n&minus;1</sub></i>=1 is zero
     * (first two lines in the 2-dimensional case).
     * </li>
     * </ol>
     * <p>Here <i>n</i>=<tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is the number of matrix dimensions.
     * If both conditions are not complied, the {@link #clear clear} method of the returned instance
     * can visit (and clear) some extra elements or can throw unexpected <tt>IndexOutOfBoundsException</tt>
     * while scanning. (And it is the only undesirable effect; no other data will be damaged,
     * no invariants will be violated.)
     *
     * <p>The scanner, returned by this method, works faster than the scanner,
     * returned by {@link #getStacklessDepthFirstScanner(Matrix, ConnectivityType)}.
     * So, if possible, we recommend to provide the required condition and to use this method.
     * The simplest way to provide the necessary 1st condition
     * is the following call:
     *
     * <pre>
     * matrix.{@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
     * subMatrix}(from, to, {@link net.algart.arrays.Matrix.ContinuationMode#ZERO_CONSTANT});
     * </pre>
     *
     * where all <tt>from[k]==-1</tt> and <tt>to[k]=matrix.{@link Matrix#dim(int) dim}(k)+1</tt>.
     * Note: please copy the matrix, created by this
     * {@link Matrix#subMatrix(long[], long[], Matrix.ContinuationMode) subMatrix} call,
     * to some newly created bit matrix,
     * in other case the performance will not be improved (because access to each element of the submatrix is slow).
     *
     * @param matrix           the matrix that will be scanned and cleared by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if <tt>matrix</tt> or <tt>connectivityType</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}
     *                                  &gt; {@link Matrix#MAX_DIM_COUNT_FOR_SOME_ALGORITHMS}</tt>.
     */
    public static ConnectedObjectScanner getUncheckedStacklessDepthFirstScanner(
        Matrix<? extends UpdatableBitArray> matrix, ConnectivityType connectivityType)
    {
        return new StacklessDepthFirstScanner(matrix, connectivityType, false);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Creates the instance, implementing the same algorithm with the same connectivity kind
     * an the same scanned matrix as this one.
     * The new instance has the same {@link #maxUsedMemory()} value as this one.
     * If this instance allocated some temporary buffers, they are not inherited:
     * the returned instance will allocate and use its own temporary buffers.
     */
    public abstract ConnectedObjectScanner clone();

    /**
     * Changes the current scanned bit matrix for this instance.
     * New matrix must have the same {@link Matrix#dimensions() dimensions} as the {@link #matrix() current one}.
     *
     * @param matrix new matrix that will be scanned and cleared by this instance.
     * @throws NullPointerException  if the argument is <tt>null</tt>.
     * @throws SizeMismatchException if the dimensions of the passed matrix
     *                               differ from the dimensions of the current one.
     * @see #matrix()
     */
    public void matrix(Matrix<? extends UpdatableBitArray> matrix) {
        if (matrix == null)
            throw new NullPointerException("Null matrix argument");
        if (!matrix.dimEquals(this.matrix))
            throw new SizeMismatchException("The passed matrix have different dimensions than the current one: "
                + "the passed is " + matrix + ", the current is " + this.matrix);
        // - so, we don't need to clear this.workMemory field
        this.matrix = matrix;
        this.array = matrix.array();
    }

    /**
     * Returns a reference to the matrix, scanned by this object.
     * It is specified while creating this instance or as an argument of {@link #matrix(Matrix)} method.
     *
     * @return a reference to the matrix, scanned by this object.
     */
    public Matrix<? extends UpdatableBitArray> matrix() {
        return this.matrix;
    }

    /**
     * Returns the connectivity kind, used by this object.
     * It is specified while creating this instance.
     *
     * @return the connectivity kind, used by this object.
     */
    public ConnectivityType connectivityType() {
        return this.connectivityType;
    }

    /**
     * Finds the next unit matrix element, starting from <tt>coordinates</tt>, saves the result
     * in the same <tt>coordinates</tt> array and returns <tt>true</tt> if such element was found.
     * If the element with specified coordinates is already unit, returns <tt>true</tt>
     * and does not change <tt>coordinates</tt> array.
     * If the unit element was not found, returns <tt>false</tt>
     * and does not change <tt>coordinates</tt> array.
     *
     * <p>If {@link #clear} method was never called with <tt>forceClearing=false</tt>,
     * this method is equivalent to the following operators:
     *
     * <pre>
     * long from = matrix.{@link Matrix#index(long...) index}(coordinates);
     * long index = {@link #matrix()}.array().{@link
     * BitArray#indexOf(long, long, boolean) indexOf}(from, {@link #matrix()}.{@link Matrix#size() size()}, true);
     * if (index == -1) {
     * &#32;   return false;
     * } else {
     * &#32;   matrix.coordinates(index, coordinates);
     * &#32;   return true;
     * }
     * </pre>
     *
     * @param coordinates coordinates of some matrix element;
     *                    the coordinates of the next unit element will be saved here.
     * @return <tt>true</tt> if the next unit element was found.
     * @throws NullPointerException      if <tt>coordinate</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if <tt>coordinate.length</tt> is not equal to
     *                                   {@link #matrix()}.{@link Matrix#dimCount() dimCount()}.
     * @throws IndexOutOfBoundsException if some coordinates are out of the {@link #matrix() scanned matrix}.
     */
    public boolean nextUnitBit(long[] coordinates) {
        if (coordinates.length != dimCount)
            throw new IllegalArgumentException("Number of passed coordinates " + coordinates.length
                + " does not match the number of dimensions of " + matrix);
        long from = matrix.index(coordinates);
        long index = indexOfUnit(from);
        if (index == -1) {
            return false;
        } else {
            matrix.coordinates(index, coordinates);
            return true;
        }
    }

    /**
     * Visits all unit (1) elements of the matrix, belonging to the connected object containing
     * the element with the specified coordinates, calls
     * <tt>elementVisitor.{@link ConnectedObjectScanner.ElementVisitor#visit visit}</tt> method for each element
     * and clears this element ({@link UpdatableBitArray#clearBit(long)}).
     * Returns the number of visited elements.
     * If the element with the specified coordinates is zero, does nothing and returns 0.
     *
     * <p>However, if <tt>forceClearing</tt> argument is <tt>false</tt>, this method
     * may skip actual clearing the visited elements in the scanned matrix,
     * but clear bits in some internal buffer instead.
     * In this case, the {@link #nextUnitBit} method will work as if the bits was actually cleared.
     * This mode is useful if you don't really need to clear bits in the source matrix,
     * but only need to visit all unit elements: this mode can improve performance.
     * If <tt>forceClearing</tt> argument is <tt>true</tt>, the behavior is strict:
     * all visited elements will be immediately cleared in the scanned matrix.
     *
     * <p>The <tt>elementVisitor.{@link ConnectedObjectScanner.ElementVisitor#visit visit}</tt>
     * method is called <i>before</i> clearing the element.</p>
     *
     * <p>The <tt>elementVisitor</tt> argument may be <tt>null</tt>: then this method only clears
     * the elements of the connected object. It may be enough if your only intention is to count the elements
     * of the connected object.</p>
     *
     * <p>The order of visiting elements is not specified and depends
     * on concrete implementation of this class.</p>
     *
     * <p>This method never modifies the passed <tt>coordinates</tt> array:
     * it is cloned in the beginning of the method and is not used after this.
     * It can be important in a case of multithread access.</p>
     *
     * @param context        the context of scanning; may be <tt>null</tt>, then will be ignored.
     *                       The main purpose of the context in most implementation is to allow interruption
     *                       of this method via {@link ArrayContext#checkInterruption()} and to allocate
     *                       work memory via {@link ArrayContext#getMemoryModel()}.
     *                       This method does not try to update execution progress via the context:
     *                       its methods {@link ArrayContext#updateProgress} and
     *                       {@link ArrayContext#checkInterruptionAndUpdateProgress} are not called.
     * @param elementVisitor the visitor, called for every visited element;
     *                       may be <tt>null</tt>, then will be ignored.
     * @param coordinates    the coordinates of some matrix element, belonging to the connected object
     *                       that should be scanned.
     * @param forceClearing  <tt>false</tt> value allows the method not to perform actual clearing
     *                       bits in the scanned matrix; <tt>true</tt> value requires actual clearing.
     * @return the number of matrix elements in the connected object
     *         or 0 if the bit with specified coordinates is zero.
     * @throws NullPointerException      if <tt>coordinates</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if the number of passed coordinates (<tt>coordinates.length</tt>)
     *                                   is not equal to the number of dimensions of the
     *                                   {@link #matrix() scanned matrix}.
     * @throws IndexOutOfBoundsException if some coordinates are out of the {@link #matrix() scanned matrix}.
     * @throws OutOfMemoryError          (low probability) if the form of the object is too complex and there is
     *                                   not enough memory to allocate necessary data structures.
     */
    public long clear(
        ArrayContext context,
        ElementVisitor elementVisitor,
        long[] coordinates,
        boolean forceClearing)
    {
        if (coordinates.length != dimCount)
            throw new IllegalArgumentException("Number of passed coordinates " + coordinates.length
                + " does not match the number of dimensions of " + matrix);
        this.index = matrix.index(coordinates);
        if (!matrix.array().getBit(index)) {
            return 0;
        }
        System.arraycopy(coordinates, 0, this.coordinates, 0, dimCount);
        this.forceClearing = forceClearing;
        this.context = context;
        this.mm = context == null ? SimpleMemoryModel.getInstance() : context.getMemoryModel();
        this.elementVisitor = elementVisitor;
        return doClear();
    }

    /**
     * Equivalent to {@link
     * #clear(ArrayContext, net.algart.matrices.scanning.ConnectedObjectScanner.ElementVisitor, long[], boolean)
     * clear(context, elementVisitor, coordinates, true)}.
     *
     * @param context        the context of scanning; may be <tt>null</tt>, then will be ignored.
     *                       The main purpose of the context in most implementation is to allow interruption
     *                       of this method via {@link ArrayContext#checkInterruption()} and to allocate
     *                       work memory via {@link ArrayContext#getMemoryModel()}.
     * @param elementVisitor the visitor, called for every visited element;
     *                       may be <tt>null</tt>, then will be ignored.
     * @param coordinates    the coordinates of some matrix element, belonging to the connected object
     *                       that should be scanned.
     * @return the number of matrix elements in the connected object
     *         or 0 if the bit with specified coordinates is zero.
     * @throws NullPointerException      if <tt>coordinates</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if the number of passed coordinates (<tt>coordinates.length</tt>)
     *                                   is not equal to the number of dimensions of the
     *                                   {@link #matrix() scanned matrix}.
     * @throws IndexOutOfBoundsException if some coordinates are out of the {@link #matrix() scanned matrix}.
     * @throws OutOfMemoryError          (low probability) if the form of the object is too complex and there is
     *                                   not enough memory to allocate necessary data structures.
     */
    public long clear(ArrayContext context, ElementVisitor elementVisitor, long... coordinates) {
        return clear(context, elementVisitor, coordinates, true);
    }

    /**
     * Equivalent to {@link
     * #clear(ArrayContext, net.algart.matrices.scanning.ConnectedObjectScanner.ElementVisitor, long[], boolean)
     * clear(context, null, coordinates, forceClearing)}.
     *
     * @param context       the context of scanning; may be <tt>null</tt>, then will be ignored.
     *                      The main purpose of the context in most implementation is to allow interruption
     *                      of this method via {@link ArrayContext#checkInterruption()} and to allocate
     *                      work memory via {@link ArrayContext#getMemoryModel()}.
     * @param coordinates   the coordinates of some matrix element, belonging to the connected object
     *                      that should be scanned.
     * @param forceClearing <tt>false</tt> value allows the method not to perform actual clearing
     *                      bits in the scanned matrix; <tt>true</tt> value requires actual clearing.
     * @return the number of matrix elements in the connected object
     *         or 0 if the bit with specified coordinates is zero.
     * @throws NullPointerException      if <tt>coordinates</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if the number of passed coordinates (<tt>coordinates.length</tt>)
     *                                   is not equal to the number of dimensions of the
     *                                   {@link #matrix() scanned matrix}.
     * @throws IndexOutOfBoundsException if some coordinates are out of the {@link #matrix() scanned matrix}.
     */
    public long clear(ArrayContext context, long[] coordinates, boolean forceClearing) {
        return clear(context, null, coordinates, forceClearing);
    }

    /**
     * Equivalent to {@link
     * #clear(ArrayContext, net.algart.matrices.scanning.ConnectedObjectScanner.ElementVisitor, long[], boolean)
     * clear(context, null, coordinates, true)}.
     *
     * @param context     the context of scanning; may be <tt>null</tt>, then will be ignored.
     *                    The main purpose of the context in most implementation is to allow interruption
     *                    of this method via {@link ArrayContext#checkInterruption()} and to allocate
     *                    work memory via {@link ArrayContext#getMemoryModel()}.
     * @param coordinates the coordinates of some matrix element, belonging to the connected object
     *                    that should be scanned.
     * @return the number of matrix elements in the connected object
     *         or 0 if the bit with specified coordinates is zero.
     * @throws NullPointerException      if <tt>coordinates</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException  if the number of passed coordinates (<tt>coordinates.length</tt>)
     *                                   is not equal to the number of dimensions of the
     *                                   {@link #matrix() scanned matrix}.
     * @throws IndexOutOfBoundsException if some coordinates are out of the {@link #matrix() scanned matrix}.
     * @throws OutOfMemoryError          (low probability) if the form of the object is too complex and there is
     *                                   not enough memory to allocate necessary data structures.
     */
    public long clear(ArrayContext context, long... coordinates) {
        return clear(context, null, coordinates, true);
    }

    /**
     * Clears all elements of all connected objects in the matrix, the volume of which
     * is less than <tt>minNonClearedSize</tt> or greater than <tt>maxNonClearedSize</tt>.
     * The <i>volume</i> here is:
     * <ul>
     * <li>just the number of elements in a connected object (result of
     * {@link #clear(ArrayContext, ConnectedObjectScanner.ElementVisitor, long[], boolean) clear} method)
     * &mdash; if the <tt>mask</tt> argument is <tt>null</tt>;</li>
     * <li>the number of such elements in a connected object, for which the corresponding
     * element in the <tt>mask</tt> bit matrix (with the same coordinates) is 1
     * &mdash; if the <tt>mask</tt> argument is not <tt>null</tt>.</li>
     * </ul>
     *
     * <p>If <tt>mask</tt> matrix is not <tt>null</tt>, it must have the same dimensions as the
     * {@link #matrix() scanned matrix}.</p>
     *
     * <p>This method creates a temporary copy of the {@link #matrix() scanned matrix}
     * and performs a loop of {@link #nextUnitBit(long[])} and {@link #clear clear} methods.
     * The temporary matrix is allocated while the first call of this method and used again in the further calls.
     *
     * @param context           the context of scanning; may be <tt>null</tt>, then will be ignored.
     * @param mask              the bit mask, on which the volume should be counted; may be <tt>null</tt>,
     *                          then the volume is the full number of elements in connected objects.
     * @param minNonClearedSize minimal volume of connected objects that will not be cleared.
     * @param maxNonClearedSize maximal volume of connected objects that will not be cleared.
     * @return the total number of cleared elements.
     * @throws SizeMismatchException if <tt>mask!=null</tt>, and <tt>mask</tt> and the scanned matrix
     *                               have different dimensions.
     */
    public long clearAllBySizes(
        ArrayContext context,
        Matrix<? extends BitArray> mask,
        long minNonClearedSize,
        long maxNonClearedSize)
    {
        if (mask != null && !mask.dimEquals(matrix))
            throw new SizeMismatchException("Current matrix and mask dimensions mismatch: current matrix is "
                + matrix + ", mask is " + mask);
        if (workMemory == null) {
            MemoryModel mm = context == null || Arrays.sizeOf(array) < Arrays.SystemSettings.maxTempJavaMemory() ?
                SimpleMemoryModel.getInstance() : context.getMemoryModel();
            workMemory = mm.newBitMatrix(matrix.dimensions());
            assert workMemory != null;
            if (!(mm instanceof SimpleMemoryModel)) {
                // anti-optimization for a case of little matrices, but can be necessary for huge matrices
                workMemory = workMemory.structureLike(matrix);
            }
        }
        Matrices.copy(context == null ? null : context.part(0.0, 0.02), workMemory, matrix);
        if (context != null) {
            context = context.part(0.02, 1.0);
        }
        ConnectedObjectScanner workScanner = clone();
        workScanner.matrix(workMemory);
        MaskElementCounter maskElementCounter = mask == null ? null : new MaskElementCounter(mask);
        long[] coordinates = new long[matrix.dimCount()]; // zero-filled
        long pixelCounter = 0;
        while (workScanner.nextUnitBit(coordinates)) {
            long size = workScanner.clear(context, maskElementCounter, coordinates, false);
            if (maskElementCounter != null) {
                size = maskElementCounter.counter();
                maskElementCounter.reset();
            }
            if (size < minNonClearedSize || size > maxNonClearedSize) {
                pixelCounter += this.clear(context, coordinates, true);
            }
            if (context != null)
                context.checkInterruptionAndUpdateProgress(boolean.class, matrix.index(coordinates), arrayLength);
        }
        return pixelCounter;
    }

    /**
     * Clears all elements of all connected objects in the {@link #matrix() matrix, corresponding to this object},
     * and also clears all objects in the passed second matrix with same sizes, which are connected with at least
     * one unit (1) element of the first matrix. In other words, this method just calls
     * {@link #clear(ArrayContext, long...) clear} method for coordinates, corresponding to all unit
     * elements of the first matrix, in a {@link #clone() clone} of this scanner,
     * where the processed matrix is {@link #matrix(Matrix) replaced} with <tt>secondaryMatrix</tt>.
     *
     * @param context         the context of scanning; may be <tt>null</tt>, then will be ignored.
     * @param secondaryMatrix the second matrix, where this method clears all objects connected with some objects
     *                        in the matrix, corresponding to this object.
     * @return the total number of cleared elements in <tt>secondaryMatrix</tt>.
     * @throws NullPointerException  if <tt>secondaryMatrix</tt> argument is <tt>null</tt>.
     * @throws SizeMismatchException if <tt>secondaryMatrix</tt> and the scanned matrix have different dimensions.
     */
    public long clearAllConnected(final ArrayContext context, Matrix<? extends UpdatableBitArray> secondaryMatrix) {
        if (secondaryMatrix == null)
            throw new NullPointerException("Null secondary matrix");
        if (!secondaryMatrix.dimEquals(matrix))
            throw new SizeMismatchException("Current and secondary matrix dimensions mismatch: current matrix is "
                + matrix + ", secondary is " + secondaryMatrix);
        final ConnectedObjectScanner secondaryScanner = clone();
        secondaryScanner.matrix(secondaryMatrix);
        ClearerOfSecondaryMatrix clearer = new ClearerOfSecondaryMatrix(context, secondaryScanner);
        while (this.nextUnitBit(coordinates)) {
            this.clear(context, clearer, coordinates);
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(boolean.class, matrix.index(coordinates), arrayLength);
            }
        }
        return clearer.pixelCounter;
    }

    /**
     * Calls <tt>context.updateProgress</tt> with an event, created by the following operator:
     * <nobr><tt>new ArrayContext.Event(boolean.class, {@link #matrix() matrix()}.{@link Matrix#size()
     * size()}, {@link #matrix() matrix()}.{@link Matrix#index(long...) index}(coordinates))</tt></nobr>.
     * Does nothing if <tt>context==null</tt>.
     *
     * <p>The method can be useful while sequentially scanning the matrix via a loop of
     * {@link #clear clear} and {@link #nextUnitBit nextUnitBit} calls.
     *
     * @param context     the context of execution;
     *                    may be <tt>null</tt>, then it will be ignored.
     * @param coordinates coordinates of currently scanned matrix element.
     */
    public final void updateProgress(ArrayContext context, long... coordinates) {
        if (context != null)
            context.updateProgress(new ArrayContext.Event(boolean.class, matrix.index(coordinates), arrayLength));
    }

    /**
     * Returns the maximal amount of work memory (in bytes), that was allocated by this instance
     * allocated during its work since its creation or calling {@link #resetUsedMemory()} method.
     * Can be used for profiling or debugging needs.
     *
     * @return the maximal amount of work memory (in bytes), used by this instance.
     */
    public long maxUsedMemory() {
        return this.maxUsedMemory;
    }

    /**
     * Resets the memory counter returned by {@link #maxUsedMemory()} method.
     */
    public void resetUsedMemory() {
        this.maxUsedMemory = 0;
    }

    /**
     * If there are some AlgART arrays or matrices, allocated by this object for storing temporary data,
     * this method calls {@link Array#freeResources(ArrayContext) Array.freeResources(context)} /
     * {@link Matrix#freeResources(ArrayContext) Matrix.freeResources(context)} methods for them.
     *
     * <p>This method may be used in situations when the instance of this object has long time life
     * and will be reused in future.
     *
     * @param context the context of execution; may be <tt>null</tt>, then it will be ignored.
     */
    public void freeResources(ArrayContext context) {
        if (workMemory != null) {
            workMemory.freeResources(context);
        }
    }

    long indexOfUnit(long from) {
        return array.indexOf(from, arrayLength, true);
    }

    abstract long doClear();

    private static class DepthFirstScanner extends ConnectedObjectScanner {
        DepthFirstScanner(
            Matrix<? extends UpdatableBitArray> matrix,
            ConnectivityType connectivityType)
        {
            super(matrix, connectivityType);
        }

        @Override
        public ConnectedObjectScanner clone() {
            ConnectedObjectScanner result = new DepthFirstScanner(this.matrix, this.connectivityType);
            result.maxUsedMemory = this.maxUsedMemory;
            return result;
        }

        @Override
        long doClear() {
            if (arrayLength < Integer.MAX_VALUE - 1) {
                return depthFirstSearchInt();
            } else {
                return depthFirstSearchLong();
            }
        }

        private long depthFirstSearchInt() {
            //[[Repeat.SectionStart depthFirstSearch_main]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+2 and analogous operations
            final int[] buffer = (int[]) INT_BUFFERS.requestArray();
            try {
                int[] stack = buffer;
                // For maximal performance, we use simple int[] stack instead of several stacks or
                // a stack of structures by CombinedMemoryModel.
                final int blockSize = dimCount + 2;
                // Our stack consists of following blocks of numbers, describing matrix elements:
                //    dimCount numbers: coordinates of the element;
                //    1 number: its index in matrix.array();
                //    1 number: the index of the current checked neighbour of the element (0..apertureSize - 1).
                long n = blockSize; // the current stack length (in ints)
                for (int j = 0; j < dimCount; j++) {
                    stack[j] = (int) coordinates[j];
                }
                stack[dimCount] = (int) index;
                stack[dimCount + 1] = 0; // extra operator, for clarity only
                if (elementVisitor != null) {
                    elementVisitor.visit(coordinates, index); // see comments to "visit" call below
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableIntArray largeStack = null;
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // stack is not empty
                    // 1. Loading stack element: coordinates, index, neighbourIndex
                    int nInt = (int) n;
                    boolean outside = false;
                    int neighbourIndex;
                    if (largeStack == null) {
                        neighbourIndex = stack[nInt - 1];
                        for (int j = 0, disp = nInt - blockSize; j < dimCount; j++, disp++) {
                            int coord = stack[disp] + coordShifts[neighbourIndex][j];
                            coordinates[j] = coord;
                            if (coord < 0 || coord >= dimensions[j]) {
                                outside = true;
                            }
                        }
                        index = stack[nInt - 2] + indexShifts[neighbourIndex];
                    } else {
                        neighbourIndex = largeStack.getInt(n - 1);
                        for (int j = 0; j < dimCount; j++) {
                            int coord = largeStack.getInt(n - blockSize + j) + coordShifts[neighbourIndex][j];
                            coordinates[j] = coord;
                            if (coord < 0 || coord >= dimensions[j]) {
                                outside = true;
                            }
                        }
                        index = largeStack.getInt(n - 2) + indexShifts[neighbourIndex];
                    }
                    // coordinates and index now refer to the neighbour

                    // 2. Saving next neighbour index in stack or removing stack element if no more neighbourds
                    neighbourIndex++;
                    if (neighbourIndex >= apertureSize) {
                        n -= blockSize; // removing last stack element; we don't actually call length(n) method here
                    } else {
                        if (largeStack == null) {
                            stack[nInt - 1] = neighbourIndex;
                        } else {
                            largeStack.setInt(n - 1, neighbourIndex);
                        }
                    }
                    // 3. Visiting the neighbour, if it is true and was not visited yet,
                    // and adding its coordinates and index to the stack
                    if (!outside) {
                        if (array.getBit(index)) {
                            counter++;
                            n += blockSize; // adding the neighbour to the stack
                            if (n < 0)
                                throw new TooLargeArrayException("Necessary stack is larger than 2^63-1");
                            nInt = (int) n;
                            if (largeStack == null) {
                                if (n > stack.length) {
                                    long newSize = Math.max(2 * (long) stack.length, n);
                                    if (newSize > MAX_TEMP_JAVA_INTS) {
                                        largeStack = mm.newIntArray(n);
                                        largeStack.setData(0, stack, 0, (int) (n - blockSize));
                                        stack = null; // allows garbage collection
                                    } else {
                                        int[] newStack = new int[(int) newSize];
                                        System.arraycopy(stack, 0, newStack, 0, nInt - blockSize);
                                        stack = newStack;
                                    }
                                }
                            } else {
                                largeStack.length(n);
                            }
                            if (n > maxMemory) {
                                maxMemory = n;
                            }
                            if (largeStack == null) {
                                for (int j = 0, disp = nInt - blockSize; j < dimCount; j++, disp++) {
                                    stack[disp] = (int) coordinates[j];
                                }
                                stack[nInt - 2] = (int) index;
                                stack[nInt - 1] = 0; // necessary operator: future neighbourIndex
                            } else {
                                for (int j = 0; j < dimCount; j++) {
                                    largeStack.setLong(n - blockSize + j, coordinates[j]);
                                }
                                largeStack.setLong(n - 2, index);
                                largeStack.setLong(n - 1, 0); // necessary operator: future neighbourIndex
                            }

                            if ((counter & 0xFFFF) == 2 && context != null)
                                context.checkInterruption();
                            if (elementVisitor != null) {
                                elementVisitor.visit(coordinates, index);
                            }
                            // Important: we call it AFTER saving coordinates in the stack;
                            // so, visit element may destroy the passed coordinates - it will not affect the algorithm
                            array.clearBit(index);
                        }
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_INT >> 3) * maxMemory);
                return counter;
            } finally {
                INT_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.SectionEnd depthFirstSearch_main]]
        }

        private long depthFirstSearchLong() {
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, depthFirstSearch_main)
            //    int\[ ==> long[;;
            //    int\s+(coord) ==> long $1;;
            //    getInt ==> getLong;;
            //    ints ==> longs;;
            //    INT ==> LONG;;
            //    IntArray ==> LongArray ;;
            //    intBuffer ==> longBuffer ;;
            //    (neighbourIndex\s*=)(\s*) ==> $1$2(int)$2 ;;
            //    \(int\)\s?(index|coord) ==> $1   !! Auto-generated: NOT EDIT !! ]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+2 and analogous operations
            final long[] buffer = (long[]) LONG_BUFFERS.requestArray();
            try {
                long[] stack = buffer;
                // For maximal performance, we use simple long[] stack instead of several stacks or
                // a stack of structures by CombinedMemoryModel.
                final int blockSize = dimCount + 2;
                // Our stack consists of following blocks of numbers, describing matrix elements:
                //    dimCount numbers: coordinates of the element;
                //    1 number: its index in matrix.array();
                //    1 number: the index of the current checked neighbour of the element (0..apertureSize - 1).
                long n = blockSize; // the current stack length (in longs)
                for (int j = 0; j < dimCount; j++) {
                    stack[j] = coordinates[j];
                }
                stack[dimCount] = index;
                stack[dimCount + 1] = 0; // extra operator, for clarity only
                if (elementVisitor != null) {
                    elementVisitor.visit(coordinates, index); // see comments to "visit" call below
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableLongArray largeStack = null;
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // stack is not empty
                    // 1. Loading stack element: coordinates, index, neighbourIndex
                    int nInt = (int) n;
                    boolean outside = false;
                    int neighbourIndex;
                    if (largeStack == null) {
                        neighbourIndex = (int) stack[nInt - 1];
                        for (int j = 0, disp = nInt - blockSize; j < dimCount; j++, disp++) {
                            long coord = stack[disp] + coordShifts[neighbourIndex][j];
                            coordinates[j] = coord;
                            if (coord < 0 || coord >= dimensions[j]) {
                                outside = true;
                            }
                        }
                        index = stack[nInt - 2] + indexShifts[neighbourIndex];
                    } else {
                        neighbourIndex = (int) largeStack.getLong(n - 1);
                        for (int j = 0; j < dimCount; j++) {
                            long coord = largeStack.getLong(n - blockSize + j) + coordShifts[neighbourIndex][j];
                            coordinates[j] = coord;
                            if (coord < 0 || coord >= dimensions[j]) {
                                outside = true;
                            }
                        }
                        index = largeStack.getLong(n - 2) + indexShifts[neighbourIndex];
                    }
                    // coordinates and index now refer to the neighbour

                    // 2. Saving next neighbour index in stack or removing stack element if no more neighbourds
                    neighbourIndex++;
                    if (neighbourIndex >= apertureSize) {
                        n -= blockSize; // removing last stack element; we don't actually call length(n) method here
                    } else {
                        if (largeStack == null) {
                            stack[nInt - 1] = neighbourIndex;
                        } else {
                            largeStack.setInt(n - 1, neighbourIndex);
                        }
                    }
                    // 3. Visiting the neighbour, if it is true and was not visited yet,
                    // and adding its coordinates and index to the stack
                    if (!outside) {
                        if (array.getBit(index)) {
                            counter++;
                            n += blockSize; // adding the neighbour to the stack
                            if (n < 0)
                                throw new TooLargeArrayException("Necessary stack is larger than 2^63-1");
                            nInt = (int) n;
                            if (largeStack == null) {
                                if (n > stack.length) {
                                    long newSize = Math.max(2 * (long) stack.length, n);
                                    if (newSize > MAX_TEMP_JAVA_LONGS) {
                                        largeStack = mm.newLongArray(n);
                                        largeStack.setData(0, stack, 0, (int) (n - blockSize));
                                        stack = null; // allows garbage collection
                                    } else {
                                        long[] newStack = new long[(int) newSize];
                                        System.arraycopy(stack, 0, newStack, 0, nInt - blockSize);
                                        stack = newStack;
                                    }
                                }
                            } else {
                                largeStack.length(n);
                            }
                            if (n > maxMemory) {
                                maxMemory = n;
                            }
                            if (largeStack == null) {
                                for (int j = 0, disp = nInt - blockSize; j < dimCount; j++, disp++) {
                                    stack[disp] = coordinates[j];
                                }
                                stack[nInt - 2] = index;
                                stack[nInt - 1] = 0; // necessary operator: future neighbourIndex
                            } else {
                                for (int j = 0; j < dimCount; j++) {
                                    largeStack.setLong(n - blockSize + j, coordinates[j]);
                                }
                                largeStack.setLong(n - 2, index);
                                largeStack.setLong(n - 1, 0); // necessary operator: future neighbourIndex
                            }

                            if ((counter & 0xFFFF) == 2 && context != null)
                                context.checkInterruption();
                            if (elementVisitor != null) {
                                elementVisitor.visit(coordinates, index);
                            }
                            // Important: we call it AFTER saving coordinates in the stack;
                            // so, visit element may destroy the passed coordinates - it will not affect the algorithm
                            array.clearBit(index);
                        }
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_LONG >> 3) * maxMemory);
                return counter;
            } finally {
                LONG_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.IncludeEnd]]
        }
    }

    private static class UncheckedDepthFirstScanner extends ConnectedObjectScanner {
        UncheckedDepthFirstScanner(
            Matrix<? extends UpdatableBitArray> matrix,
            ConnectivityType connectivityType)
        {
            super(matrix, connectivityType);
        }

        @Override
        public ConnectedObjectScanner clone() {
            ConnectedObjectScanner result = new UncheckedDepthFirstScanner(this.matrix, this.connectivityType);
            result.maxUsedMemory = this.maxUsedMemory;
            return result;
        }

        @Override
        long doClear() {
            if (arrayLength < Integer.MAX_VALUE - 1) {
                return uncheckedDepthFirstSearchInt();
            } else {
                return uncheckedDepthFirstSearchLong();
            }
        }

        private long uncheckedDepthFirstSearchInt() {
            //[[Repeat.SectionStart uncheckedDepthFirstSearch_main]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+2 and analogous operations
            final int[] buffer = (int[]) INT_BUFFERS.requestArray();
            try {
                int[] stack = buffer;
                // For maximal performance, we use simple int[] stack instead of several stacks or
                // a stack of structures by CombinedMemoryModel.
                // Our stack consists of following blocks of numbers, describing matrix elements:
                //    1 number: its index in matrix.array();
                //    1 number: the index of the current checked neighbour of the element (0..apertureSize - 1).
                long n = 2; // the current stack length (in ints)
                stack[0] = (int) index;
                stack[1] = 0; // extra operator, for clarity only
                if (elementVisitor != null) {
                    elementVisitor.visit(null, index);
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableIntArray largeStack = null;
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // stack is not empty
                    // 1. Loading stack element: index, neighbourIndex
                    int nInt = (int) n;
                    int neighbourIndex;
                    if (largeStack == null) {
                        neighbourIndex = stack[nInt - 1];
                        index = stack[nInt - 2] + indexShifts[neighbourIndex];
                    } else {
                        neighbourIndex = largeStack.getInt(n - 1);
                        index = largeStack.getInt(n - 2) + indexShifts[neighbourIndex];
                    }
                    // index now refers to the neighbour

                    // 2. Saving next neighbour index in stack or removing stack element if no more neighbourds
                    neighbourIndex++;
                    if (neighbourIndex >= apertureSize) {
                        n -= 2; // removing last stack element; we don't actually call length(n) method here
                    } else {
                        if (largeStack == null) {
                            stack[nInt - 1] = neighbourIndex;
                        } else {
                            largeStack.setInt(n - 1, neighbourIndex);
                        }
                    }
                    // 3. Visiting the neighbour, if it is true and was not visited yet,
                    // and adding its index to the stack
                    if (array.getBit(index)) {
                        counter++;
                        n += 2; // adding the neighbour to the stack
                        if (n < 0)
                            throw new TooLargeArrayException("Necessary stack is larger than 2^63-1");
                        nInt = (int) n;
                        if (largeStack == null) {
                            if (n > stack.length) {
                                long newSize = Math.max(2 * (long) stack.length, n);
                                if (newSize > MAX_TEMP_JAVA_INTS) {
                                    largeStack = mm.newIntArray(n);
                                    largeStack.setData(0, stack, 0, (int) (n - 2));
                                    stack = null; // allows garbage collection
                                } else {
                                    int[] newStack = new int[(int) newSize];
                                    System.arraycopy(stack, 0, newStack, 0, nInt - 2);
                                    stack = newStack;
                                }
                            }
                        } else {
                            largeStack.length(n);
                        }
                        if (n > maxMemory) {
                            maxMemory = n;
                        }
                        if (largeStack == null) {
                            stack[nInt - 2] = (int) index;
                            stack[nInt - 1] = 0; // necessary operator: future neighbourIndex
                        } else {
                            largeStack.setLong(n - 2, index);
                            largeStack.setLong(n - 1, 0); // necessary operator: future neighbourIndex
                        }

                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            elementVisitor.visit(null, index);
                        }
                        array.clearBit(index);
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_INT >> 3) * maxMemory);
                return counter;
            } finally {
                INT_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.SectionEnd uncheckedDepthFirstSearch_main]]
        }

        private long uncheckedDepthFirstSearchLong() {
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, uncheckedDepthFirstSearch_main)
            //    int\[ ==> long[;;
            //    int\s+(coord) ==> long $1;;
            //    getInt ==> getLong;;
            //    ints ==> longs;;
            //    INT ==> LONG;;
            //    IntArray ==> LongArray ;;
            //    intBuffer ==> longBuffer ;;
            //    (neighbourIndex\s*=)(\s*) ==> $1$2(int)$2 ;;
            //    \(int\)\s?(index|coord) ==> $1   !! Auto-generated: NOT EDIT !! ]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+2 and analogous operations
            final long[] buffer = (long[]) LONG_BUFFERS.requestArray();
            try {
                long[] stack = buffer;
                // For maximal performance, we use simple long[] stack instead of several stacks or
                // a stack of structures by CombinedMemoryModel.
                // Our stack consists of following blocks of numbers, describing matrix elements:
                //    1 number: its index in matrix.array();
                //    1 number: the index of the current checked neighbour of the element (0..apertureSize - 1).
                long n = 2; // the current stack length (in longs)
                stack[0] = index;
                stack[1] = 0; // extra operator, for clarity only
                if (elementVisitor != null) {
                    elementVisitor.visit(null, index);
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableLongArray largeStack = null;
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // stack is not empty
                    // 1. Loading stack element: index, neighbourIndex
                    int nInt = (int) n;
                    int neighbourIndex;
                    if (largeStack == null) {
                        neighbourIndex = (int) stack[nInt - 1];
                        index = stack[nInt - 2] + indexShifts[neighbourIndex];
                    } else {
                        neighbourIndex = (int) largeStack.getLong(n - 1);
                        index = largeStack.getLong(n - 2) + indexShifts[neighbourIndex];
                    }
                    // index now refers to the neighbour

                    // 2. Saving next neighbour index in stack or removing stack element if no more neighbourds
                    neighbourIndex++;
                    if (neighbourIndex >= apertureSize) {
                        n -= 2; // removing last stack element; we don't actually call length(n) method here
                    } else {
                        if (largeStack == null) {
                            stack[nInt - 1] = neighbourIndex;
                        } else {
                            largeStack.setInt(n - 1, neighbourIndex);
                        }
                    }
                    // 3. Visiting the neighbour, if it is true and was not visited yet,
                    // and adding its index to the stack
                    if (array.getBit(index)) {
                        counter++;
                        n += 2; // adding the neighbour to the stack
                        if (n < 0)
                            throw new TooLargeArrayException("Necessary stack is larger than 2^63-1");
                        nInt = (int) n;
                        if (largeStack == null) {
                            if (n > stack.length) {
                                long newSize = Math.max(2 * (long) stack.length, n);
                                if (newSize > MAX_TEMP_JAVA_LONGS) {
                                    largeStack = mm.newLongArray(n);
                                    largeStack.setData(0, stack, 0, (int) (n - 2));
                                    stack = null; // allows garbage collection
                                } else {
                                    long[] newStack = new long[(int) newSize];
                                    System.arraycopy(stack, 0, newStack, 0, nInt - 2);
                                    stack = newStack;
                                }
                            }
                        } else {
                            largeStack.length(n);
                        }
                        if (n > maxMemory) {
                            maxMemory = n;
                        }
                        if (largeStack == null) {
                            stack[nInt - 2] = index;
                            stack[nInt - 1] = 0; // necessary operator: future neighbourIndex
                        } else {
                            largeStack.setLong(n - 2, index);
                            largeStack.setLong(n - 1, 0); // necessary operator: future neighbourIndex
                        }

                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            elementVisitor.visit(null, index);
                        }
                        array.clearBit(index);
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_LONG >> 3) * maxMemory);
                return counter;
            } finally {
                LONG_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.IncludeEnd]]
        }
    }

    private static class BreadthFirstScanner extends ConnectedObjectScanner {
        final int[] intCoordinates; // work memory
        final long[] longCoordinates; // work memory

        BreadthFirstScanner(
            Matrix<? extends UpdatableBitArray> matrix,
            ConnectivityType connectivityType)
        {
            super(matrix, connectivityType);
            this.intCoordinates = new int[dimCount];
            this.longCoordinates = new long[dimCount];
        }

        @Override
        public ConnectedObjectScanner clone() {
            ConnectedObjectScanner result = new BreadthFirstScanner(this.matrix, this.connectivityType);
            result.maxUsedMemory = this.maxUsedMemory;
            return result;
        }

        @Override
        long doClear() {
            if (arrayLength < Integer.MAX_VALUE - 1) {
                return breadthFirstSearchInt();
            } else {
                return breadthFirstSearchLong();
            }
        }

        private long breadthFirstSearchInt() {
            //[[Repeat.SectionStart breadthFirstSearch_main]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+1 and analogous operations
            assert (long) apertureSize * (long) (Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1) <= Integer.MAX_VALUE;
            // because apertureSize <= 3^dimCount
            final int queueSpaceForAperture = (dimCount + 1) * apertureSize;
            final int[] buffer = (int[]) INT_BUFFERS.requestArray();
            try {
                int[] queue = buffer;
                // For maximal performance, we use simple int[] queue instead of several queues or
                // a queue of structures by CombinedMemoryModel.
                final int blockSize = dimCount + 1;
                // Our queue consists of following blocks of numbers, describing matrix elements:
                //    dimCount numbers: coordinates of the element;
                //    1 number: its index in matrix.array().
                long h = 0; // the current queue head (in ints)
                long r = blockSize; // the current queue rear + 1 block (in ints); h==r means empty queue
                long n = blockSize; // the current queue length (in ints)
                long qSize = queue.length / blockSize * blockSize;
                for (int j = 0; j < dimCount; j++) {
                    queue[j] = (int) coordinates[j];
                }
                queue[dimCount] = (int) index;
                if (elementVisitor != null) {
                    elementVisitor.visit(coordinates, index); // see comments to "visit" call below
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableIntArray largeQueue = null;
                int intIndex = 157; // compiler requires some initialization
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // queue is not empty
                    // 1. Providing enough space in the queue for the worst case
                    if (n + queueSpaceForAperture > qSize) {
                        if (qSize > Long.MAX_VALUE / 2)
                            throw new TooLargeArrayException("Necessary queue is larger than 2^63-1");
                        long newSize = 2 * qSize; // must not be less than 2*qSize! (for the branch largeQueue!=null)
                        if (largeQueue == null) {
                            if (newSize > MAX_TEMP_JAVA_INTS) {
                                largeQueue = mm.newIntArray(newSize);
                                if (h <= r) {
                                    assert h - r == n;
                                    largeQueue.setData(0, queue, (int) h, (int) n);
                                } else {
                                    largeQueue.setData(0, queue, (int) h, (int) qSize - (int) h);
                                    largeQueue.setData((int) qSize - (int) h, queue, 0, (int) r);
                                }
                                queue = null; // allows garbage collection
                            } else {
                                int[] newQueue = new int[(int) newSize];
                                if (h <= r) {
                                    assert h - r == n;
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) n);
                                } else {
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) qSize - (int) h);
                                    System.arraycopy(queue, 0, newQueue, (int) qSize - (int) h, (int) r);
                                }
                                queue = newQueue;
                            }
                        } else {
                            largeQueue.length(newSize);
                            if (h <= r) {
                                assert h - r == n;
                                largeQueue.copy(0, h, n);
                            } else {
                                largeQueue.copy(qSize, 0, r); // saving first r elements in the 2nd half
                                largeQueue.copy(0, h, qSize - h); // moving the first queue part to the beginning
                                largeQueue.copy(qSize - h, qSize, r); // attaching the rear
                            }
                        }
                        qSize = newSize;
                        h = 0;
                        r = n;
                    }

                    // 2. Preloading queue element for slow queue: coordinates and index
                    int hInt = (int) h;
                    if (largeQueue != null) {
                        for (int j = 0; j < dimCount; j++) {
                            intCoordinates[j] = largeQueue.getInt(h + j);
                        }
                        intIndex = largeQueue.getInt(h + dimCount);
                    }
                    h += blockSize;
                    if (h >= qSize)
                        h = 0;
                    // but preserving hInt!
                    n -= blockSize;

                    // 3. Loop by neighbours
                    for (int neighbourIndex = 0; neighbourIndex < apertureSize; neighbourIndex++) {
                        // 3.1 Loading neighbour coordinates and index
                        boolean outside = false;
                        if (largeQueue == null) {
                            for (int j = 0; j < dimCount; j++) {
                                int coord = queue[hInt + j] + coordShifts[neighbourIndex][j];
                                coordinates[j] = coord;
                                if (coord < 0 || coord >= dimensions[j]) {
                                    outside = true;
                                }
                            }
                            index = queue[hInt + dimCount] + indexShifts[neighbourIndex];
                        } else {
                            for (int j = 0; j < dimCount; j++) {
                                int coord = intCoordinates[j] + coordShifts[neighbourIndex][j];
                                coordinates[j] = coord;
                                if (coord < 0 || coord >= dimensions[j]) {
                                    outside = true;
                                }
                            }
                            index = intIndex + indexShifts[neighbourIndex];
                        }
                        // coordinates and index now refer to the neighbour

                        // 3.2. Visiting the neighbour, if it is true and was not visited yet,
                        // and adding its coordinates and index to the queue
                        if (!outside) {
                            if (array.getBit(index)) {
                                counter++;
                                if (largeQueue == null) {
                                    int rInt = (int) r;
                                    for (int j = 0; j < dimCount; j++) {
                                        queue[rInt + j] = (int) coordinates[j];
                                    }
                                    queue[rInt + dimCount] = (int) index;
                                } else {
                                    for (int j = 0; j < dimCount; j++) {
                                        largeQueue.setLong(r + j, coordinates[j]);
                                    }
                                    largeQueue.setLong(r + dimCount, index);
                                }
                                r += blockSize; // adding the neighbour to the queue
                                if (r >= qSize)
                                    r = 0;
                                n += blockSize;
                                if ((counter & 0xFFFF) == 2 && context != null)
                                    context.checkInterruption();
                                if (elementVisitor != null) {
                                    elementVisitor.visit(coordinates, index);
                                }
                                // Important: we call it AFTER saving coordinates in the stack;
                                // so, visit element may destroy the passed coordinates - it will not affect the algorithm
                                array.clearBit(index);
                            }
                            if (n > maxMemory) {
                                maxMemory = n;
                            }
                        }
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_INT >> 3) * maxMemory);
                return counter;
            } finally {
                INT_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.SectionEnd breadthFirstSearch_main]]
        }

        private long breadthFirstSearchLong() {
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, breadthFirstSearch_main)
            //    int\[ ==> long[;;
            //    int\s+(intIndex|coord) ==> long $1;;
            //    int(Index|Coordinates) ==> long$1 ;;
            //    getInt ==> getLong;;
            //    ints ==> longs;;
            //    INT ==> LONG;;
            //    IntArray ==> LongArray ;;
            //    intBuffer ==> longBuffer ;;
            //    \(int\)\s?(index|coord) ==> $1   !! Auto-generated: NOT EDIT !! ]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+1 and analogous operations
            assert (long) apertureSize * (long) (Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1) <= Integer.MAX_VALUE;
            // because apertureSize <= 3^dimCount
            final int queueSpaceForAperture = (dimCount + 1) * apertureSize;
            final long[] buffer = (long[]) LONG_BUFFERS.requestArray();
            try {
                long[] queue = buffer;
                // For maximal performance, we use simple long[] queue instead of several queues or
                // a queue of structures by CombinedMemoryModel.
                final int blockSize = dimCount + 1;
                // Our queue consists of following blocks of numbers, describing matrix elements:
                //    dimCount numbers: coordinates of the element;
                //    1 number: its index in matrix.array().
                long h = 0; // the current queue head (in longs)
                long r = blockSize; // the current queue rear + 1 block (in longs); h==r means empty queue
                long n = blockSize; // the current queue length (in longs)
                long qSize = queue.length / blockSize * blockSize;
                for (int j = 0; j < dimCount; j++) {
                    queue[j] = coordinates[j];
                }
                queue[dimCount] = index;
                if (elementVisitor != null) {
                    elementVisitor.visit(coordinates, index); // see comments to "visit" call below
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableLongArray largeQueue = null;
                long longIndex = 157; // compiler requires some initialization
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // queue is not empty
                    // 1. Providing enough space in the queue for the worst case
                    if (n + queueSpaceForAperture > qSize) {
                        if (qSize > Long.MAX_VALUE / 2)
                            throw new TooLargeArrayException("Necessary queue is larger than 2^63-1");
                        long newSize = 2 * qSize; // must not be less than 2*qSize! (for the branch largeQueue!=null)
                        if (largeQueue == null) {
                            if (newSize > MAX_TEMP_JAVA_LONGS) {
                                largeQueue = mm.newLongArray(newSize);
                                if (h <= r) {
                                    assert h - r == n;
                                    largeQueue.setData(0, queue, (int) h, (int) n);
                                } else {
                                    largeQueue.setData(0, queue, (int) h, (int) qSize - (int) h);
                                    largeQueue.setData((int) qSize - (int) h, queue, 0, (int) r);
                                }
                                queue = null; // allows garbage collection
                            } else {
                                long[] newQueue = new long[(int) newSize];
                                if (h <= r) {
                                    assert h - r == n;
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) n);
                                } else {
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) qSize - (int) h);
                                    System.arraycopy(queue, 0, newQueue, (int) qSize - (int) h, (int) r);
                                }
                                queue = newQueue;
                            }
                        } else {
                            largeQueue.length(newSize);
                            if (h <= r) {
                                assert h - r == n;
                                largeQueue.copy(0, h, n);
                            } else {
                                largeQueue.copy(qSize, 0, r); // saving first r elements in the 2nd half
                                largeQueue.copy(0, h, qSize - h); // moving the first queue part to the beginning
                                largeQueue.copy(qSize - h, qSize, r); // attaching the rear
                            }
                        }
                        qSize = newSize;
                        h = 0;
                        r = n;
                    }

                    // 2. Preloading queue element for slow queue: coordinates and index
                    int hInt = (int) h;
                    if (largeQueue != null) {
                        for (int j = 0; j < dimCount; j++) {
                            longCoordinates[j] = largeQueue.getLong(h + j);
                        }
                        longIndex = largeQueue.getLong(h + dimCount);
                    }
                    h += blockSize;
                    if (h >= qSize)
                        h = 0;
                    // but preserving hInt!
                    n -= blockSize;

                    // 3. Loop by neighbours
                    for (int neighbourIndex = 0; neighbourIndex < apertureSize; neighbourIndex++) {
                        // 3.1 Loading neighbour coordinates and index
                        boolean outside = false;
                        if (largeQueue == null) {
                            for (int j = 0; j < dimCount; j++) {
                                long coord = queue[hInt + j] + coordShifts[neighbourIndex][j];
                                coordinates[j] = coord;
                                if (coord < 0 || coord >= dimensions[j]) {
                                    outside = true;
                                }
                            }
                            index = queue[hInt + dimCount] + indexShifts[neighbourIndex];
                        } else {
                            for (int j = 0; j < dimCount; j++) {
                                long coord = longCoordinates[j] + coordShifts[neighbourIndex][j];
                                coordinates[j] = coord;
                                if (coord < 0 || coord >= dimensions[j]) {
                                    outside = true;
                                }
                            }
                            index = longIndex + indexShifts[neighbourIndex];
                        }
                        // coordinates and index now refer to the neighbour

                        // 3.2. Visiting the neighbour, if it is true and was not visited yet,
                        // and adding its coordinates and index to the queue
                        if (!outside) {
                            if (array.getBit(index)) {
                                counter++;
                                if (largeQueue == null) {
                                    int rInt = (int) r;
                                    for (int j = 0; j < dimCount; j++) {
                                        queue[rInt + j] = coordinates[j];
                                    }
                                    queue[rInt + dimCount] = index;
                                } else {
                                    for (int j = 0; j < dimCount; j++) {
                                        largeQueue.setLong(r + j, coordinates[j]);
                                    }
                                    largeQueue.setLong(r + dimCount, index);
                                }
                                r += blockSize; // adding the neighbour to the queue
                                if (r >= qSize)
                                    r = 0;
                                n += blockSize;
                                if ((counter & 0xFFFF) == 2 && context != null)
                                    context.checkInterruption();
                                if (elementVisitor != null) {
                                    elementVisitor.visit(coordinates, index);
                                }
                                // Important: we call it AFTER saving coordinates in the stack;
                                // so, visit element may destroy the passed coordinates - it will not affect the algorithm
                                array.clearBit(index);
                            }
                            if (n > maxMemory) {
                                maxMemory = n;
                            }
                        }
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_LONG >> 3) * maxMemory);
                return counter;
            } finally {
                LONG_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.IncludeEnd]]
        }
    }

    private static class UncheckedBreadthFirstScanner extends ConnectedObjectScanner {
        UncheckedBreadthFirstScanner(
            Matrix<? extends UpdatableBitArray> matrix,
            ConnectivityType connectivityType)
        {
            super(matrix, connectivityType);
        }

        @Override
        public ConnectedObjectScanner clone() {
            ConnectedObjectScanner result = new UncheckedBreadthFirstScanner(this.matrix, this.connectivityType);
            result.maxUsedMemory = this.maxUsedMemory;
            return result;
        }

        @Override
        long doClear() {
            if (arrayLength < Integer.MAX_VALUE - 1) {
                return uncheckedBreadthFirstSearchInt();
            } else {
                return uncheckedBreadthFirstSearchLong();
            }
        }

        private long uncheckedBreadthFirstSearchInt() {
            //[[Repeat.SectionStart uncheckedBreadthFirstSearch_main]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+1 and analogous operations
            assert (long) apertureSize * (long) (Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1) <= Integer.MAX_VALUE;
            // because apertureSize <= 3^dimCount
            final int queueSpaceForAperture = apertureSize;
            final int[] buffer = (int[]) INT_BUFFERS.requestArray();
            try {
                int[] queue = buffer;
                // Our queue consists of following blocks of numbers, describing matrix elements:
                //    1 number: its index in matrix.array().
                long h = 0; // the current queue head (in ints)
                long r = 1; // the current queue rear + 1 (in ints); h==r means empty queue
                long n = 1; // the current queue length (in ints)
                long qSize = queue.length;
                queue[0] = (int) index;
                if (elementVisitor != null) {
                    elementVisitor.visit(null, index); // see comments to "visit" call below
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableIntArray largeQueue = null;
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // queue is not empty
                    // 1. Providing enough space in the queue for the worst case
                    if (n + queueSpaceForAperture > qSize) {
                        if (qSize > Long.MAX_VALUE / 2)
                            throw new TooLargeArrayException("Necessary queue is larger than 2^63-1");
                        long newSize = 2 * qSize; // must not be less than 2*qSize! (for the branch largeQueue!=null)
                        if (largeQueue == null) {
                            if (newSize > MAX_TEMP_JAVA_INTS) {
                                largeQueue = mm.newIntArray(newSize);
                                if (h <= r) {
                                    assert h - r == n;
                                    largeQueue.setData(0, queue, (int) h, (int) n);
                                } else {
                                    largeQueue.setData(0, queue, (int) h, (int) qSize - (int) h);
                                    largeQueue.setData((int) qSize - (int) h, queue, 0, (int) r);
                                }
                                queue = null; // allows garbage collection
                            } else {
                                int[] newQueue = new int[(int) newSize];
                                if (h <= r) {
                                    assert h - r == n;
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) n);
                                } else {
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) qSize - (int) h);
                                    System.arraycopy(queue, 0, newQueue, (int) qSize - (int) h, (int) r);
                                }
                                queue = newQueue;
                            }
                        } else {
                            largeQueue.length(newSize);
                            if (h <= r) {
                                assert h - r == n;
                                largeQueue.copy(0, h, n);
                            } else {
                                largeQueue.copy(qSize, 0, r); // saving first r elements in the 2nd half
                                largeQueue.copy(0, h, qSize - h); // moving the first queue part to the beginning
                                largeQueue.copy(qSize - h, qSize, r); // attaching the rear
                            }
                        }
                        qSize = newSize;
                        h = 0;
                        r = n;
                    }

                    // 2. Preloading queue element: index
                    int intIndex = largeQueue != null ? largeQueue.getInt(h) : queue[(int) h];
                    h++;
                    if (h >= qSize)
                        h = 0;
                    n--;

                    // 3. Loop by neighbours
                    for (int neighbourIndex = 0; neighbourIndex < apertureSize; neighbourIndex++) {
                        // 3.1 Loading neighbour index
                        index = intIndex + indexShifts[neighbourIndex];
                        // index now refers to the neighbour

                        // 3.2. Visiting the neighbour, if it is true and was not visited yet,
                        // and adding its index to the queue
                        if (array.getBit(index)) {
                            counter++;
                            if (largeQueue == null) {
                                queue[(int) r] = (int) index;
                            } else {
                                largeQueue.setLong(r, index);
                            }
                            r++; // adding the neighbour to the queue
                            if (r >= qSize)
                                r = 0;
                            n++;
                            if ((counter & 0xFFFF) == 2 && context != null)
                                context.checkInterruption();
                            if (elementVisitor != null) {
                                elementVisitor.visit(null, index);
                            }
                            array.clearBit(index);
                        }
                    }
                    if (n > maxMemory) {
                        maxMemory = n;
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_INT >> 3) * maxMemory);
                return counter;
            } finally {
                INT_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.SectionEnd uncheckedBreadthFirstSearch_main]]
        }

        private long uncheckedBreadthFirstSearchLong() {
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, uncheckedBreadthFirstSearch_main)
            //    int\[ ==> long[;;
            //    int\s+(intIndex|coord) ==> long $1;;
            //    int(Index|Coordinates) ==> long$1 ;;
            //    getInt ==> getLong;;
            //    ints ==> longs;;
            //    INT ==> LONG;;
            //    IntArray ==> LongArray ;;
            //    intBuffer ==> longBuffer ;;
            //    \(int\)\s+(index|coord) ==> $1   !! Auto-generated: NOT EDIT !! ]]
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            assert dimCount <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS;
            // so, overflow is impossible in dimCount+1 and analogous operations
            assert (long) apertureSize * (long) (Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS + 1) <= Integer.MAX_VALUE;
            // because apertureSize <= 3^dimCount
            final int queueSpaceForAperture = apertureSize;
            final long[] buffer = (long[]) LONG_BUFFERS.requestArray();
            try {
                long[] queue = buffer;
                // Our queue consists of following blocks of numbers, describing matrix elements:
                //    1 number: its index in matrix.array().
                long h = 0; // the current queue head (in longs)
                long r = 1; // the current queue rear + 1 (in longs); h==r means empty queue
                long n = 1; // the current queue length (in longs)
                long qSize = queue.length;
                queue[0] = index;
                if (elementVisitor != null) {
                    elementVisitor.visit(null, index); // see comments to "visit" call below
                }
                array.clearBit(index);
                // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
                // array can be copy-on-next-write (however, direct access is not used in current implementation)
                MutableLongArray largeQueue = null;
                long maxMemory = 0;
                long counter = 1;
                while (n > 0) { // queue is not empty
                    // 1. Providing enough space in the queue for the worst case
                    if (n + queueSpaceForAperture > qSize) {
                        if (qSize > Long.MAX_VALUE / 2)
                            throw new TooLargeArrayException("Necessary queue is larger than 2^63-1");
                        long newSize = 2 * qSize; // must not be less than 2*qSize! (for the branch largeQueue!=null)
                        if (largeQueue == null) {
                            if (newSize > MAX_TEMP_JAVA_LONGS) {
                                largeQueue = mm.newLongArray(newSize);
                                if (h <= r) {
                                    assert h - r == n;
                                    largeQueue.setData(0, queue, (int) h, (int) n);
                                } else {
                                    largeQueue.setData(0, queue, (int) h, (int) qSize - (int) h);
                                    largeQueue.setData((int) qSize - (int) h, queue, 0, (int) r);
                                }
                                queue = null; // allows garbage collection
                            } else {
                                long[] newQueue = new long[(int) newSize];
                                if (h <= r) {
                                    assert h - r == n;
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) n);
                                } else {
                                    System.arraycopy(queue, (int) h, newQueue, 0, (int) qSize - (int) h);
                                    System.arraycopy(queue, 0, newQueue, (int) qSize - (int) h, (int) r);
                                }
                                queue = newQueue;
                            }
                        } else {
                            largeQueue.length(newSize);
                            if (h <= r) {
                                assert h - r == n;
                                largeQueue.copy(0, h, n);
                            } else {
                                largeQueue.copy(qSize, 0, r); // saving first r elements in the 2nd half
                                largeQueue.copy(0, h, qSize - h); // moving the first queue part to the beginning
                                largeQueue.copy(qSize - h, qSize, r); // attaching the rear
                            }
                        }
                        qSize = newSize;
                        h = 0;
                        r = n;
                    }

                    // 2. Preloading queue element: index
                    long longIndex = largeQueue != null ? largeQueue.getLong(h) : queue[(int) h];
                    h++;
                    if (h >= qSize)
                        h = 0;
                    n--;

                    // 3. Loop by neighbours
                    for (int neighbourIndex = 0; neighbourIndex < apertureSize; neighbourIndex++) {
                        // 3.1 Loading neighbour index
                        index = longIndex + indexShifts[neighbourIndex];
                        // index now refers to the neighbour

                        // 3.2. Visiting the neighbour, if it is true and was not visited yet,
                        // and adding its index to the queue
                        if (array.getBit(index)) {
                            counter++;
                            if (largeQueue == null) {
                                queue[(int) r] = index;
                            } else {
                                largeQueue.setLong(r, index);
                            }
                            r++; // adding the neighbour to the queue
                            if (r >= qSize)
                                r = 0;
                            n++;
                            if ((counter & 0xFFFF) == 2 && context != null)
                                context.checkInterruption();
                            if (elementVisitor != null) {
                                elementVisitor.visit(null, index);
                            }
                            array.clearBit(index);
                        }
                    }
                    if (n > maxMemory) {
                        maxMemory = n;
                    }
                }
                this.maxUsedMemory = Math.max(this.maxUsedMemory, (Arrays.BITS_PER_LONG >> 3) * maxMemory);
                return counter;
            } finally {
                LONG_BUFFERS.releaseArray(buffer);
            }
            //[[Repeat.IncludeEnd]]
        }
    }

    private static class StacklessDepthFirstScanner extends ConnectedObjectScanner {
        final boolean checked;
        final long[] coordinatesClone; // work memory
        UpdatablePIntegerArray buffer = null; // work memory
        // High bit in the buffer will be used to mark visited elements, low 7 or 15 bits to save the direction
        boolean bufferValid = false;
        final int bufferHighBit, bufferMask;
        int bufOfs;
        byte[] bufBytes;

        StacklessDepthFirstScanner(
            Matrix<? extends UpdatableBitArray> matrix,
            ConnectivityType connectivityType, boolean checked)
        {
            super(matrix, connectivityType);
            if (this.apertureSize >= 32768)
                throw new AssertionError("Too large aperture: must never be greater than 32767 elements");
            this.checked = checked;
            this.coordinatesClone = checked ? new long[dimCount] : null;
            this.bufferHighBit = this.apertureSize < 128 ? 0x80 : 0x8000;
            this.bufferMask = this.bufferHighBit - 1;
        }

        @Override
        public ConnectedObjectScanner clone() {
            ConnectedObjectScanner result = new StacklessDepthFirstScanner(this.matrix, this.connectivityType, this.checked);
            result.maxUsedMemory = this.maxUsedMemory;
            return result;
        }

        @Override
        public void matrix(Matrix<? extends UpdatableBitArray> matrix) {
            super.matrix(matrix);
            bufferValid = false;
        }

        @Override
        public void freeResources(ArrayContext context) {
            super.freeResources(context == null || buffer == null ? context : context.part(0.0, 0.5));
            if (buffer != null) {
                buffer.freeResources(context == null ? null : context.part(0.5, 1.0));
            }
        }

        @Override
        long indexOfUnit(long from) {
            if (!bufferValid) {
                return super.indexOfUnit(from);
            }
            return buffer.indexOf(from, arrayLength, bufferHighBit);
        }

        @Override
        long doClear() {
            // Here we are sure that the current element at this.coordinates is 1: it was checked in clear(...) method
            if (elementVisitor != null) {
                if (coordinatesClone != null) {
                    System.arraycopy(coordinates, 0, coordinatesClone, 0, dimCount);
                }
                elementVisitor.visit(coordinatesClone, index);
            }
            array.clearBit(index);
            // even for SimpleMemoryModel, we here MUST call clearBit method instead direct access to Java array:
            // array can be copy-on-next-write (however, direct access is not used in current implementation)
            initializeBuffer();
            buffer.setInt(index, 0); // duplicating "clearBit" in the buffer
            if (checked) {
                if (bufBytes != null && !forceClearing) {
                    if (dimCount == 2) {
                        return stacklessDepthFirstSearchBytes2dNoForce();
                    } else {
                        return stacklessDepthFirstSearchBytesNoForce();
                    }
                } else {
                    if (dimCount == 2) {
                        return stacklessDepthFirstSearchPArray2d();
                    } else {
                        return stacklessDepthFirstSearchPArray();
                    }
                }
            } else {
                if (bufBytes != null && !forceClearing) {
                    return uncheckedStacklessDepthFirstSearchBytesNoForce();
                } else {
                    return uncheckedStacklessDepthFirstSearchPArray();
                }
            }
        }

        private void initializeBuffer() {
            if (this.bufferValid) {
                return;
            }
            if (this.buffer == null) {
                MemoryModel mm = arrayLength > MAX_TEMP_JAVA_MEMORY ? this.mm : SimpleMemoryModel.getInstance();
                // Use arrayLength even for short type, to simplify the contract of this class
                this.buffer = apertureSize < 128 ?
                    mm.newUnresizableByteArray(arrayLength) : mm.newUnresizableShortArray(arrayLength);
                if (!(mm instanceof SimpleMemoryModel)) {
                    // to be on the safe side: maybe, it is a huge tiled matrix
                    this.buffer = matrix.matrix(buffer).structureLike(matrix).array();
                }
                boolean directBuffer = this.buffer instanceof DirectAccessible
                    && ((DirectAccessible) this.buffer).hasJavaArray();
                this.bufBytes = apertureSize < 128 && directBuffer ?
                    (byte[]) ((DirectAccessible) this.buffer).javaArray() : null;
                this.bufOfs = directBuffer ? ((DirectAccessible) this.buffer).javaArrayOffset() : 0;
                this.maxUsedMemory = arrayLength * (apertureSize < 128 ? 1 : 2);
            }
            DataBuffer dBuf = buffer.buffer(DataBuffer.AccessMode.READ_WRITE);

            long[] ja = null;
            long jaOfs = 0;
            if (SimpleMemoryModel.isSimpleArray(array)) {
                DataBitBuffer buf = array.buffer(DataBuffer.AccessMode.READ, 16);
                if (buf.isDirect()) { // possibly not for immutable or copy-on-next-write arrays
                    buf.map(0);
                    ja = buf.data();
                    jaOfs = buf.from();
                }
            }
            long[] bits = ja == null ? new long[(int) PackedBitArrays.packedLength(dBuf.capacity())] : ja;
            for (long p = 0; p < arrayLength; p += dBuf.capacity()) {
                if (context != null)
                    context.checkInterruption();
                dBuf.map(p);
                if (ja == null) {
                    array.getBits(p, bits, 0, dBuf.cnt());
                }
                if (buffer instanceof ByteArray) {
                    PackedBitArrays.unpackBits((byte[]) dBuf.data(), dBuf.from(), bits, ja == null ? 0 : jaOfs + p,
                        dBuf.cnt(), (byte) 0, (byte) 0x80);
                } else if (buffer instanceof ShortArray) {
                    PackedBitArrays.unpackBits((short[]) dBuf.data(), dBuf.from(), bits, ja == null ? 0 : jaOfs + p,
                        dBuf.cnt(), (short) 0, (short) 0x8000);
                } else {
                    throw new AssertionError("Impossible buffer class");
                }
                dBuf.force();
            }
            this.bufferValid = true;
        }

        private long stacklessDepthFirstSearchPArray() {
            //[[Repeat.SectionStart stacklessDepthFirstSearch_main]]
            final long startIndex = index;
            long counter = 1;
            int direction = 0;
            mainLoop:
            for (; ; ) {
                directionLoop:
                for (; direction < apertureSize; direction++) {
                    for (int j = 0; j < dimCount; j++) {
                        long coord = coordinates[j] + coordShifts[direction][j];
                        if (coord < 0 || coord >= dimensions[j]) {
                            continue directionLoop;
                        }
                        coordinatesClone[j] = coord;
                    }
                    long indexClone = index + indexShifts[direction];
                    final boolean bit = buffer.getInt(indexClone) >= bufferHighBit;
                    if (bit) {
                        counter++;
                        System.arraycopy(coordinatesClone, 0, coordinates, 0, dimCount);
                        index = indexClone;
                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            elementVisitor.visit(coordinatesClone, indexClone);
                        }
                        // Important: we call it AFTER saving coordinatesClone in coordinates;
                        // so, visit element may destroy the passed coordinates - it will not affect the algorithm
                        if (forceClearing) {
                            array.clearBit(index);
                        }
                        buffer.setInt(index, direction); // clearing the high bit
                        direction = 0;
                        continue mainLoop;
                    }
                }
                if (index == startIndex) {
                    break;
                }
                direction = buffer.getInt(index) & bufferMask;
                for (int j = 0; j < dimCount; j++) {
                    coordinates[j] -= coordShifts[direction][j];
                    assert coordinates[j] >= 0 && coordinates[j] < dimensions[j];
                }
                index -= indexShifts[direction];
                direction++;
            }
            return counter;
            //[[Repeat.SectionEnd stacklessDepthFirstSearch_main]]
        }

        private long stacklessDepthFirstSearchBytesNoForce() {
            assert bufferHighBit == 0x80;
            int index = (int) this.index;
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, stacklessDepthFirstSearch_main)
            //    if\s+\(forceClearing\)\s*\{(.*?)\} ==> ;;
            //    (int|long)\s+(jaIndex|jaMask)\s*=\s*0; ==> $1 $2; ;;
            //    buffer\.setInt\((\w+)\,\s*(\w+)\) ==> bufBytes[$1] = (byte) ($2) ;;
            //    buffer\.getInt\((\w+)\)\s*>=\s*bufferHighBit ==> bufBytes[$1] < 0;;
            //    buffer\.getInt\((\w+)\) ==> bufBytes[$1] ;;
            //    bufferMask ==> 0x7F ;;
            //    \bindexShifts\b ==> intIndexShifts ;;
            //    long\s+(indexClone) ==> int $1   !! Auto-generated: NOT EDIT !! ]]
            final long startIndex = index;
            long counter = 1;
            int direction = 0;
            mainLoop:
            for (; ; ) {
                directionLoop:
                for (; direction < apertureSize; direction++) {
                    for (int j = 0; j < dimCount; j++) {
                        long coord = coordinates[j] + coordShifts[direction][j];
                        if (coord < 0 || coord >= dimensions[j]) {
                            continue directionLoop;
                        }
                        coordinatesClone[j] = coord;
                    }
                    int indexClone = index + intIndexShifts[direction];
                    final boolean bit = bufBytes[indexClone] < 0;
                    if (bit) {
                        counter++;
                        System.arraycopy(coordinatesClone, 0, coordinates, 0, dimCount);
                        index = indexClone;
                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            elementVisitor.visit(coordinatesClone, indexClone);
                        }
                        // Important: we call it AFTER saving coordinatesClone in coordinates;
                        // so, visit element may destroy the passed coordinates - it will not affect the algorithm

                        bufBytes[index] = (byte) (direction); // clearing the high bit
                        direction = 0;
                        continue mainLoop;
                    }
                }
                if (index == startIndex) {
                    break;
                }
                direction = bufBytes[index] & 0x7F;
                for (int j = 0; j < dimCount; j++) {
                    coordinates[j] -= coordShifts[direction][j];
                    assert coordinates[j] >= 0 && coordinates[j] < dimensions[j];
                }
                index -= intIndexShifts[direction];
                direction++;
            }
            return counter;
            //[[Repeat.IncludeEnd]]
        }

        private long uncheckedStacklessDepthFirstSearchPArray() {
            //[[Repeat.SectionStart uncheckedStacklessDepthFirstSearch_main]]
            final long startIndex = index;
            long counter = 1;
            int direction = 0;
            mainLoop:
            for (; ; ) {
                for (; direction < apertureSize; direction++) {
                    long indexClone = index + indexShifts[direction];
                    final boolean bit = buffer.getInt(indexClone) >= bufferHighBit;
                    if (bit) {
                        counter++;
                        index = indexClone;
                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            elementVisitor.visit(null, indexClone);
                        }
                        if (forceClearing) {
                            array.clearBit(index);
                        }
                        buffer.setInt(index, direction); // clearing the high bit
                        direction = 0;
                        continue mainLoop;
                    }
                }
                if (index == startIndex) {
                    break;
                }
                direction = buffer.getInt(index) & bufferMask;
                index -= indexShifts[direction];
                direction++;
            }
            return counter;
            //[[Repeat.SectionEnd uncheckedStacklessDepthFirstSearch_main]]
        }

        private long uncheckedStacklessDepthFirstSearchBytesNoForce() {
            assert bufferHighBit == 0x80;
            int index = (int) this.index;
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, uncheckedStacklessDepthFirstSearch_main)
            //    if\s+\(forceClearing\)\s*\{(.*?)\} ==> ;;
            //    (int|long)\s+(jaIndex|jaMask)\s*=\s*0; ==> $1 $2; ;;
            //    buffer\.setInt\((\w+)\,\s*(\w+)\) ==> bufBytes[$1] = (byte) ($2) ;;
            //    buffer\.getInt\((\w+)\)\s*>=\s*bufferHighBit ==> bufBytes[$1] < 0;;
            //    buffer\.getInt\((\w+)\) ==> bufBytes[$1] ;;
            //    bufferMask ==> 0x7F ;;
            //    \bindexShifts\b ==> intIndexShifts ;;
            //    long\s+(indexClone) ==> int $1   !! Auto-generated: NOT EDIT !! ]]
            final long startIndex = index;
            long counter = 1;
            int direction = 0;
            mainLoop:
            for (; ; ) {
                for (; direction < apertureSize; direction++) {
                    int indexClone = index + intIndexShifts[direction];
                    final boolean bit = bufBytes[indexClone] < 0;
                    if (bit) {
                        counter++;
                        index = indexClone;
                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            elementVisitor.visit(null, indexClone);
                        }

                        bufBytes[index] = (byte) (direction); // clearing the high bit
                        direction = 0;
                        continue mainLoop;
                    }
                }
                if (index == startIndex) {
                    break;
                }
                direction = bufBytes[index] & 0x7F;
                index -= intIndexShifts[direction];
                direction++;
            }
            return counter;
            //[[Repeat.IncludeEnd]]
        }

        private long stacklessDepthFirstSearchPArray2d() {
            //[[Repeat.SectionStart stacklessDepthFirstSearch2d_main]]
            final long startIndex = index;
            long x = coordinates[0], y = coordinates[1];
            final long dimX = dimensions[0], dimY = dimensions[1];
            long counter = 1;
            int direction = 0;
            mainLoop:
            for (; ; ) {
                for (; direction < apertureSize; direction++) {
                    long xClone = x + xShifts[direction];
                    if (xClone < 0 || xClone >= dimX) {
                        continue;
                    }
                    long yClone = y + yShifts[direction];
                    if (yClone < 0 || yClone >= dimY) {
                        continue;
                    }
                    long indexClone = index + indexShifts[direction];
                    final boolean bit = buffer.getInt(indexClone) >= bufferHighBit;
                    if (bit) {
                        counter++;
                        x = xClone;
                        y = yClone;
                        index = indexClone;
                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            coordinatesClone[0] = xClone;
                            coordinatesClone[1] = yClone;
                            elementVisitor.visit(coordinatesClone, indexClone);
                        }
                        // Important: we call it AFTER saving coordinatesClone in coordinates;
                        // so, visit element may destroy the passed coordinates - it will not affect the algorithm
                        if (forceClearing) {
                            array.clearBit(index);
                        }
                        buffer.setInt(index, direction); // clearing the high bit
                        direction = 0;
                        continue mainLoop;
                    }
                }
                if (index == startIndex) {
                    break;
                }
                direction = buffer.getInt(index) & bufferMask;
                x -= xShifts[direction];
                y -= yShifts[direction];
                index -= indexShifts[direction];
                direction++;
            }
            return counter;
            //[[Repeat.SectionEnd stacklessDepthFirstSearch2d_main]]
        }

        private long stacklessDepthFirstSearchBytes2dNoForce() {
            assert !forceClearing;
            int index = (int) this.index;
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, stacklessDepthFirstSearch2d_main)
            //    if\s+\(forceClearing\)\s*\{(.*?)\} ==> ;;
            //    (int|long)\s+(jaIndex|jaMask)\s*=\s*0; ==> $1 $2; ;;
            //    buffer\.setInt\((\w+)\,\s*(\w+)\) ==> bufBytes[$1] = (byte) $2 ;;
            //    buffer\.getInt\((\w+)\)\s*>=\s*bufferHighBit ==> bufBytes[$1] < 0;;
            //    buffer\.getInt\((\w+)\) ==> bufBytes[$1] ;;
            //    bufferMask ==> 0x7F ;;
            //    indexShifts ==> intIndexShifts ;;
            //    long\s+(indexClone) ==> int $1   !! Auto-generated: NOT EDIT !! ]]
            final long startIndex = index;
            long x = coordinates[0], y = coordinates[1];
            final long dimX = dimensions[0], dimY = dimensions[1];
            long counter = 1;
            int direction = 0;
            mainLoop:
            for (; ; ) {
                for (; direction < apertureSize; direction++) {
                    long xClone = x + xShifts[direction];
                    if (xClone < 0 || xClone >= dimX) {
                        continue;
                    }
                    long yClone = y + yShifts[direction];
                    if (yClone < 0 || yClone >= dimY) {
                        continue;
                    }
                    int indexClone = index + intIndexShifts[direction];
                    final boolean bit = bufBytes[indexClone] < 0;
                    if (bit) {
                        counter++;
                        x = xClone;
                        y = yClone;
                        index = indexClone;
                        if ((counter & 0xFFFF) == 2 && context != null)
                            context.checkInterruption();
                        if (elementVisitor != null) {
                            coordinatesClone[0] = xClone;
                            coordinatesClone[1] = yClone;
                            elementVisitor.visit(coordinatesClone, indexClone);
                        }
                        // Important: we call it AFTER saving coordinatesClone in coordinates;
                        // so, visit element may destroy the passed coordinates - it will not affect the algorithm

                        bufBytes[index] = (byte) direction; // clearing the high bit
                        direction = 0;
                        continue mainLoop;
                    }
                }
                if (index == startIndex) {
                    break;
                }
                direction = bufBytes[index] & 0x7F;
                x -= xShifts[direction];
                y -= yShifts[direction];
                index -= intIndexShifts[direction];
                direction++;
            }
            return counter;
            //[[Repeat.IncludeEnd]]
        }
    }

    private static class ClearerOfSecondaryMatrix implements ElementVisitor {
        final ArrayContext context;
        final ConnectedObjectScanner secondaryScanner;
        final Matrix<? extends UpdatableBitArray> secondaryMatrix;
        final BitArray secondaryArray;
        final long[] secondaryCoordinates;
        long pixelCounter = 0;

        ClearerOfSecondaryMatrix(ArrayContext context, ConnectedObjectScanner secondaryScanner) {
            this.context = context;
            this.secondaryScanner = secondaryScanner;
            this.secondaryMatrix = secondaryScanner.matrix();
            this.secondaryCoordinates = new long[secondaryMatrix.dimCount()];
            this.secondaryArray = secondaryMatrix.array();
        }

        public void visit(long[] coordinatesInMatrix, long indexInArray) {
            if (secondaryArray.getBit(indexInArray)) {
                long count = secondaryScanner.clear(context, secondaryMatrix.coordinates(indexInArray, null));
                pixelCounter += count;
            }
        }
    }
}
