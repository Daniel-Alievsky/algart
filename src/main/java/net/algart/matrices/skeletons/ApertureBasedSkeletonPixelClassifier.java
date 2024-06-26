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

package net.algart.matrices.skeletons;

import net.algart.arrays.*;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>A skeletal implementation of the {@link SkeletonPixelClassifier} abstract class,
 * minimizing the effort required to implement its abstract methods.</p>
 *
 * <p>Namely, the main {@link #asPixelTypes asPixelTypes} method is implemented in this class
 * via the following 2 abstract methods:</p>
 *
 * <ul>
 * <li>{@link #pixelTypeOrAttachingBranch(int)},</li>
 * <li>{@link #pixelTypeOrAttachedNode(int)}.</li>
 * </ul>
 *
 * <p>The methods {@link #neighbourOffset(long[], int)} and {@link #reverseNeighbourIndex(int)}
 * are implemented on the base of the array of offsets of all neighbours of a random element.
 * This array should be passed to the {@link #ApertureBasedSkeletonPixelClassifier(int, long[][]) constructor}.
 * The constructor analyses the passed array, checks that it really contains
 * offsets of all neighbours, copies this array into an internal field, which is used
 * by {@link #neighbourOffset(long[], int)}, and also automatically finds,
 * for every neighbour, the reverse neighbour index, which will be returned by {@link #reverseNeighbourIndex(int)}.
 *
 * <p>The method {@link #markNeighbouringNodesNotConnectedViaDegeneratedBranches(int[])} is implemented here
 * in some reasonable way for 2-dimensional case, as specified in comments to this method.
 * For other number of dimensions, this method does nothing.
 * It is a good solution for the degenerated case {@link #dimCount() dimCount()}=1;
 * for 3-dimensional case, this method probably should be overridden.</p>
 *
 * <p>So, it is enough to implement {@link #pixelTypeOrAttachingBranch(int)} and {@link #pixelTypeOrAttachedNode(int)}
 * methods and, maybe, override {@link #markNeighbouringNodesNotConnectedViaDegeneratedBranches(int[])} method
 * to create a full implementation of the skeleton pixel classifier on the base of this class.</p>
 *
 * <p>This class can be used in 1-, 2- and 3-dimensional cases only.
 * One of the reasons of this restriction is that the argument of {@link #pixelTypeOrAttachingBranch(int)}
 * and {@link #pixelTypeOrAttachedNode(int)} (<code>int</code> type) can represent the values of, maximally, 32
 * neighbours. It is enough for 3-dimensional case, where the number of neighbours is 3<sup>3</sup>&minus;1=26&lt;32,
 * but not enough already for 4-dimensional case, where the number of neighbours is 3<sup>4</sup>&minus;1=80
 * (and even 64-bit <code>long</code> type would have been insufficient).</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class ApertureBasedSkeletonPixelClassifier extends SkeletonPixelClassifier {
    private final long[][] neighbourOffsets;
    private final int[] reverseNeighbourIndexes;

    /**
     * Creates new instance of this class, allowing to process skeletons with the given number of dimensions,
     * with the order of neighbours, specified in the second argument.
     * The number of dimensions must be 1, 2 or 3.
     *
     * <p>The argument <code>neighbourOffsets</code> must contain offsets of all neighbours,
     * in terms of the {@link net.algart.matrices.scanning.ConnectivityType#STRAIGHT_AND_DIAGONAL
     * straight-and-diagonal connectivity kind}, of any matrix element, in some order.
     * More precisely, this array must contain
     * <code>{@link #numberOfNeighbours() numberOfNeighbours()}=3<sup>dimCount</sup>-1</code> elements
     * (2, 8 or 26 for 1-, 2-, 3-dimensional cases) in 3/3x3/3x3x3-aperture, and each its element
     * <code>neighbourOffsets[k]</code> must be equal to the result of
     * {@link #neighbourOffset(int) neighbourOffset(k)} call.
     *
     * <p>The passed <code>neighbourOffsets</code> array is deeply cloned by the constructor: no references to it
     * or its elements are maintained by the created object.
     *
     * @param dimCount         the number of dimensions, which will be returned by
     *                         {@link #dimCount() dimCount()} method.
     * @param neighbourOffsets offsets of all neighbours of any matrix element,
     *                         in terms of {@link #neighbourOffset(int) neighbourOffset(int)} method.
     * @throws NullPointerException     if <code>neighbourOffsets</code> or one of its elements is {@code null}.
     * @throws IllegalArgumentException if <code>dimCount</code> is not in <code>1..3</code> range,
     *                                  or if <code>neighbourOffsets.length!=3<sup>dimCount</sup>-1</code>,
     *                                  or if <code>neighbourOffsets[k].length!=dimCount</code>
     *                                  for some <code>k</code>,
     *                                  or if <code>neighbourOffsets</code> does not contain, in some order,
     *                                  the offsets of all <code>3<sup>dimCount</sup>-1</code> neighbours
     *                                  (in particular, if some elements <code>neighbourOffsets[k][j]</code> are
     *                                  not in <code>-1..+1</code> range or if offsets of some neighbours are equal).
     */
    protected ApertureBasedSkeletonPixelClassifier(int dimCount, long[][] neighbourOffsets) {
        super(dimCount);
        Objects.requireNonNull(neighbourOffsets, "Null neighbourOffsets array");
        if (dimCount > 3) {
            throw new IllegalArgumentException("This class " + getClass().getName() + " cannot process "
                + dimCount + "-dimensional apertures (maximum 3-dimensional ones are allowed)");
        }
        if (this.numberOfNeighbours != neighbourOffsets.length) {
            throw new IllegalArgumentException("Number of passed neighbour offsets " + neighbourOffsets.length
                + " does not match the number of neighbours in 3x3x... aperture " + this.numberOfNeighbours);
        }
        if (this.numberOfNeighbours > 30) // bit #31 can be used for the central element of the aperture
        {
            throw new AssertionError("This class " + getClass().getName()
                + " cannot process more than 30 elements in the aperture (besides the central element)");
        }
        // We cannot use full 63-bit precision here, because double values cannot precisely store all long values
        this.neighbourOffsets = new long[neighbourOffsets.length][dimCount];
        for (int k = 0; k < neighbourOffsets.length; k++) {
            long[] neighbourOffset = neighbourOffsets[k];
            // creating a copy: necessary if another thread is modifying the argument now
            Objects.requireNonNull(neighbourOffset, "Null neighbourOffsets[" + k + "]");
            if (neighbourOffset.length != dimCount) {
                throw new IllegalArgumentException("Illegal neighbourOffsets[" + k + "].length = "
                    + neighbourOffset.length + ": does not match to the number of dimensions " + dimCount);
            }
            System.arraycopy(neighbourOffset, 0, this.neighbourOffsets[k], 0, dimCount);
        }
        // now this.neighbourOffsets is a deep copy of the argument, which cannot be destroyed by another thread
        this.reverseNeighbourIndexes = new int[this.neighbourOffsets.length];
        for (int k = 0; k < this.neighbourOffsets.length; k++) {
            int reverseIndex = -1;
            boolean allZero = true;
            for (int j = 0; j < dimCount; j++) {
                if (Math.abs(this.neighbourOffsets[k][j]) > 1) {
                    throw new IllegalArgumentException("Illegal neighbourOffsets: the offset #" + k
                        + " (" + JArrays.toString(this.neighbourOffsets[k], ",", 1000)
                        + " describes not a neighbour, because some of its components is not in -1..1 range");
                }
                allZero &= this.neighbourOffsets[k][j] == 0;
            }
            if (allZero) {
                throw new IllegalArgumentException("Illegal neighbourOffsets: the offset #" + k + " is zero");
            }
            for (int i = 0; i < this.neighbourOffsets.length; i++) {
                if (i == k) {
                    continue;
                }
                boolean matchThis = true;
                for (int j = 0; j < dimCount; j++) {
                    matchThis &= this.neighbourOffsets[i][j] == this.neighbourOffsets[k][j];
                }
                if (matchThis) {
                    throw new IllegalArgumentException("Illegal neighbourOffsets: the offsets #" + k
                        + " and # " + i + " are equal");
                }
                boolean matchNegative = true;
                for (int j = 0; j < dimCount; j++) {
                    matchNegative &= this.neighbourOffsets[i][j] == -this.neighbourOffsets[k][j];
                }
                if (matchNegative) {
                    reverseIndex = i; break;
                }
            }
            if (reverseIndex == -1) {
                throw new IllegalArgumentException("Illegal neighbourOffsets: the offset #" + k
                    + " (" + JArrays.toString(this.neighbourOffsets[k], ",", 1000)
                    + ") has no corresponding reverse offset (the same but with negative sign)");
            }
            this.reverseNeighbourIndexes[k] = reverseIndex;
        }
        // We've checked 3^dimCount-1 offsets and all they are different non-zero vectors with -1..1 components,
        // so, we can be sure that they are really the offsets of all elements of 3x3x... aperture, in some order.
    }

    @Override
    public void neighbourOffset(long[] coordinateIncrements, int neighbourIndex) {
        Objects.requireNonNull(coordinateIncrements, "Null list of coordinates");
        if (coordinateIncrements.length != dimCount) {
            throw new IllegalArgumentException("Number of coordinates " + coordinateIncrements.length
                + " is not equal to the number of matrix dimensions " + dimCount());
        }
        if (neighbourIndex < 0 || neighbourIndex >= numberOfNeighbours) {
            throw new IndexOutOfBoundsException("Illegal neighbourIndex = " + neighbourIndex
                + ": must be in 0.." + (numberOfNeighbours - 1) + " range");
        }
        System.arraycopy(neighbourOffsets[neighbourIndex], 0, coordinateIncrements, 0, dimCount);
    }


    @Override
    public int reverseNeighbourIndex(int neighbourIndex) {
        return reverseNeighbourIndexes[neighbourIndex];
    }

    @Override
    public Matrix<? extends PIntegerArray> asPixelTypes(
        Matrix<? extends BitArray> skeleton,
        AttachmentInformation attachmentInformation)
    {
        Objects.requireNonNull(attachmentInformation, "Null attachmentInformation");
        Matrix<? extends PIntegerArray> packed = asNeighbourhoodBitMaps(skeleton);
        switch (attachmentInformation) {
            case NEIGHBOUR_INDEX_OF_ATTACHING_BRANCH:
                return Matrices.asFuncMatrix(false, new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        int apertureBits = (int) x0; // precise operations, because x0 is "int" 31-bit value
                        if ((apertureBits & 1) == 0) {
                            return TYPE_ZERO;
                        }
                        return pixelTypeOrAttachingBranch(apertureBits >>> 1);
                    }
                }, IntArray.class, packed);
            case NEIGHBOUR_INDEX_OF_ATTACHED_NODE:
                return Matrices.asFuncMatrix(false, new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        int apertureBits = (int) x0; // precise operations, because x0 is "int" 31-bit value
                        if ((apertureBits & 1) == 0) {
                            return TYPE_ZERO;
                        }
                        return pixelTypeOrAttachedNode(apertureBits >>> 1);
                    }
                }, IntArray.class, packed);
            default:
                throw new AssertionError("Unknown attachmentInformation: " + attachmentInformation);
        }
    }

    @Override
    public void markNeighbouringNodesNotConnectedViaDegeneratedBranches(int[] pixelTypesOfAllNeighbours) {
        if (pixelTypesOfAllNeighbours.length < numberOfNeighbours) {
            throw new IllegalArgumentException("Too short pixelTypesOfAllNeighbours array");
        }
        if (dimCount != 2) {
            return; // should be overridden for another number of dimensions
        }
        for (int neighbourIndex = 0; neighbourIndex < numberOfNeighbours; neighbourIndex++) {
            if (neighbourIndex % 2 == 0) { // diagonal degenerated branch
                if (pixelTypesOfAllNeighbours[(neighbourIndex + 1) & 7] == TYPE_USUAL_NODE
                    || pixelTypesOfAllNeighbours[(neighbourIndex + 7) & 7] == TYPE_USUAL_NODE)
                {
                    pixelTypesOfAllNeighbours[neighbourIndex] = Integer.MIN_VALUE;
                }
            }
        }
    }

    /**
     * Returns an immutable view of the passed skeleton matrix, where each element is an integer,
     * containing, in its low bits, the bit values of the corresponding element
     * <code><b><i>C</i></b></code> of the source skeleton and of all its neighbours (in terms of the
     * {@link net.algart.matrices.scanning.ConnectivityType#STRAIGHT_AND_DIAGONAL
     * straight-and-diagonal connectivity kind}).
     *
     * <p>More precisely, each integer element <i>w</i> of the resulting matrix will contain:
     * <ul>
     * <li>in the bit #0 (in other words, <i>w</i><code>&amp;1</code>):
     * the value of the corresponding element
     * <code><b><i>C</i></b></code> of the source skeleton bit matrix;</li>
     * <li>in the bit #<i>k</i>+1, 0&le;<i>k</i>&lt;{@link #numberOfNeighbours() numberOfNeighbours()}
     * (in other words, (<i>w</i><code>&gt;&gt;&gt;(</code><i>k</i><code>+1))&amp;1</code>):
     * the value of the neighbour #<i>k</i> of the central element <code><b><i>C</i></b></code>,
     * in terms of {@link #neighbourOffset(int) neighbourOffset(int)} method;</li>
     * <li>all other bits of the elements if the resulting matrix will be zero.</li>
     * </ul>
     *
     * <p>In particular, in {@link BasicSkeletonPixelClassifier2D} implementation,
     * the lower 9 bits in the elements of the returned matrix correspond to elements of 3x3 aperture
     * of the source skeleton according the following diagram:
     * <pre>
     * 1 2 3
     * 8 <b>0</b> 4
     * 7 6 5</pre>
     * <p>(the <i>x</i>-axis is directed rightward, the <i>y</i>-axis is directed downward).</p>
     *
     * <p>The implementation of {@link #asPixelTypes asPixelTypes} method in this class is based on
     * this method and {@link #pixelTypeOrAttachingBranch(int)} and {@link #pixelTypeOrAttachedNode(int)} methods:
     * the results <i>w</i>, returned by this method for unit central elements <code><b><i>C</i></b></code>
     * of the source skeleton, are shifted to the right and passed as
     * <code>apertureBits</code>=<i>w</i><code>&gt;&gt;&gt;1</code> argument to
     * {@link #pixelTypeOrAttachingBranch(int)} or {@link #pixelTypeOrAttachedNode(int)} to form the elements
     * of the resulting matrix.
     *
     * <p>Note, that the situation, when the neighbouring elements are out of ranges of the matrix coordinates,
     * is processed according to the model of infinite pseudo-cyclical continuation &mdash;
     * see the end of the {@link SkeletonPixelClassifier comments to SkeletonPixelClassifier}.
     *
     * @param skeleton    the skeleton matrix that should be processed.
     * @return            the matrix of integer values with the same sizes, containing the bit maps
     *                    of the neighbourhoods of all skeleton pixels.
     * @throws NullPointerException     if <code>skeleton</code> is {@code null}.
     * @throws IllegalArgumentException if <code>skeleton.dimCount()!={@link #dimCount()}</code>.
     */
    public final Matrix<? extends PIntegerArray> asNeighbourhoodBitMaps(
        Matrix<? extends BitArray> skeleton)
    {
        Objects.requireNonNull(skeleton, "Null skeleton");
        if (skeleton.dimCount() != dimCount) {
            throw new IllegalArgumentException("This object (" + this + ") can process "
                + dimCount + "-dimensional matrices only");
        }

        List<Matrix<? extends PArray>> shifted = new ArrayList<Matrix<? extends PArray>>();
        shifted.add(skeleton);
        long[] shift = new long[dimCount];
        for (long[] apertureOffset : this.neighbourOffsets) {
            for (int k = 0; k < dimCount; k++) {
                shift[k] = -apertureOffset[k];
            }
            shifted.add(Matrices.asShifted(skeleton, shift).cast(PArray.class));
        }
        double[] weights = new double[shifted.size()];
        assert weights.length <= 31;
        for (int k = 0; k < weights.length; k++) {
            weights[k] = 1L << k;
        }
        Func packingShiftedBits = LinearFunc.getInstance(0.0, weights);
        return Matrices.asFuncMatrix(packingShiftedBits, IntArray.class, shifted);
    }

    /*Repeat() NEIGHBOUR_INDEX_OF_ATTACHING_BRANCH ==> NEIGHBOUR_INDEX_OF_ATTACHED_NODE;;
               OrAttachingBranch                   ==> OrAttachedNode
    */

    /**
     * Calculates and returns the value of an element <code><b><i>C'</i></b></code>
     * in the resulting matrix, produced by
     * {@link #asPixelTypes asPixelTypes} method with
     * {@link SkeletonPixelClassifier.AttachmentInformation#NEIGHBOUR_INDEX_OF_ATTACHING_BRANCH
     * NEIGHBOUR_INDEX_OF_ATTACHING_BRANCH} value of <code>attachmentInformation</code> argument,
     * on the base of bit values of all neighbours (in terms of the
     * {@link net.algart.matrices.scanning.ConnectivityType#STRAIGHT_AND_DIAGONAL
     * straight-and-diagonal connectivity kind})
     * of the corresponding unit element <code><b><i>C</i></b></code> in the source skeleton bit matrix.
     *
     * <p>More precisely, the bit values of the neighbours of this skeleton element <code><b><i>C</i></b></code>
     * are passed via the low
     * <i>m</i>={@link #numberOfNeighbours() numberOfNeighbours()} bits of <code>apertureBits</code> argument.
     * The bit #<i>k</i> of this argument, 0&le;<i>k</i>&lt;<i>m</i> (its value is
     * (<code>apertureBits&gt;&gt;&gt;</code><i>k</i><code>)&amp;1</code>), is equal to the value
     * of the neighbour #<i>k</i> in terms of {@link #neighbourOffset(int) neighbourOffset(int)} method.
     * In particular, in {@link BasicSkeletonPixelClassifier2D} implementation,
     * the order of neighbours is described by the following diagram:
     * <pre>
     * 0 1 2
     * 7 <b><i>C</i></b> 3
     * 6 5 4</pre>
     * <p>So, 8 low bits of <code>apertureBits</code> contain the values of the corresponding neighbouring elements
     * in anticlockwise order (the <i>x</i>-axis is directed rightward, the <i>y</i>-axis is directed downward).
     *
     * <p>It is supposed that the central element (<code><b><i>C</i></b></code>) of the skeleton is unit
     * (for zero elements ot the skeleton matrix, {@link #asPixelTypes asPixelTypes} method of
     * this class returns {@link #TYPE_ZERO} without calling this method).
     *
     * <p>Note, that the situation, when the neighbouring elements are out of ranges of the matrix coordinates,
     * is processed according to the model of infinite pseudo-cyclical continuation &mdash;
     * see the end of the {@link SkeletonPixelClassifier comments to SkeletonPixelClassifier}.
     *
     * @param apertureBits the values of all 8 neighbours of the current unit element of the source skeleton
     *                     bit matrix.
     * @return the type of this pixel of the skeleton.
     */
    protected abstract int pixelTypeOrAttachingBranch(int apertureBits);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Calculates and returns the value of an element <code><b><i>C'</i></b></code>
     * in the resulting matrix, produced by
     * {@link #asPixelTypes asPixelTypes} method with
     * {@link SkeletonPixelClassifier.AttachmentInformation#NEIGHBOUR_INDEX_OF_ATTACHED_NODE
     * NEIGHBOUR_INDEX_OF_ATTACHED_NODE} value of <code>attachmentInformation</code> argument,
     * on the base of bit values of all neighbours (in terms of the
     * {@link net.algart.matrices.scanning.ConnectivityType#STRAIGHT_AND_DIAGONAL
     * straight-and-diagonal connectivity kind})
     * of the corresponding unit element <code><b><i>C</i></b></code> in the source skeleton bit matrix.
     *
     * <p>More precisely, the bit values of the neighbours of this skeleton element <code><b><i>C</i></b></code>
     * are passed via the low
     * <i>m</i>={@link #numberOfNeighbours() numberOfNeighbours()} bits of <code>apertureBits</code> argument.
     * The bit #<i>k</i> of this argument, 0&le;<i>k</i>&lt;<i>m</i> (its value is
     * (<code>apertureBits&gt;&gt;&gt;</code><i>k</i><code>)&amp;1</code>), is equal to the value
     * of the neighbour #<i>k</i> in terms of {@link #neighbourOffset(int) neighbourOffset(int)} method.
     * In particular, in {@link BasicSkeletonPixelClassifier2D} implementation,
     * the order of neighbours is described by the following diagram:
     * <pre>
     * 0 1 2
     * 7 <b><i>C</i></b> 3
     * 6 5 4</pre>
     * <p>So, 8 low bits of <code>apertureBits</code> contain the values of the corresponding neighbouring elements
     * in anticlockwise order (the <i>x</i>-axis is directed rightward, the <i>y</i>-axis is directed downward).
     *
     * <p>It is supposed that the central element (<code><b><i>C</i></b></code>) of the skeleton is unit
     * (for zero elements ot the skeleton matrix, {@link #asPixelTypes asPixelTypes} method of
     * this class returns {@link #TYPE_ZERO} without calling this method).
     *
     * <p>Note, that the situation, when the neighbouring elements are out of ranges of the matrix coordinates,
     * is processed according to the model of infinite pseudo-cyclical continuation &mdash;
     * see the end of the {@link SkeletonPixelClassifier comments to SkeletonPixelClassifier}.
     *
     * @param apertureBits the values of all 8 neighbours of the current unit element of the source skeleton
     *                     bit matrix.
     * @return the type of this pixel of the skeleton.
     */
    protected abstract int pixelTypeOrAttachedNode(int apertureBits);
    /*Repeat.AutoGeneratedEnd*/
}
