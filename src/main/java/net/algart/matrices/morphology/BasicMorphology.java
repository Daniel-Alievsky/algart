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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.arrays.Arrays;
import net.algart.math.IPoint;
import net.algart.math.Point;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.*;

import java.util.*;

/**
 * <p>The simplest complete implementation of {@link Morphology} interface.
 * This implementation complies with the strict definition of dilation and erosion
 * specified in comments to {@link Morphology#dilation(Matrix, Pattern)}
 * and {@link Morphology#erosion(Matrix, Pattern)} methods.</p>
 *
 * <p>This class provides essential optimization for non-"lazy" dilation and erosion,
 * performed by {@link Morphology#dilation(Matrix, Pattern)}
 * and {@link Morphology#erosion(Matrix, Pattern)} methods, for most types of patterns.
 * So, usually you should use these method, but not {@link Morphology#asDilation(Matrix, Pattern)}
 * and {@link Morphology#asErosion(Matrix, Pattern)}.</p>
 *
 * <p>Some methods of the returned object can throw {@link TooLargeArrayException}
 * in a very improbable situation when the source matrix length (number of elements)
 * is greater than <tt>Long.MAX_VALUE/2=2<sup>62</sup>-1</tt>.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class BasicMorphology extends AbstractMorphology implements Morphology {
    private static final int MIN_LENGTH_OF_DECOMPOSITION_FOR_USING_JAVA_MEMORY = 4;
    // Java memory may mean additional copying from SimpleMemoryModel to another model;
    // current value of this constant guarantees that it cannot require more than ~20% of the total time

    private static final int MIN_POINT_COUNT_TO_DECOMPOSE = 4;

    private static final boolean QUICK_UNION_DECOMPOSITION_ALGORITHM = true;
    // For debugging only: "false" value allows to switch to much more simple, but slower algorithm

    private static final int MAX_NUMBER_OF_RANGES_FOR_CUSTOM_COPIER = 1048576;
    // Good solution for byte matrices up to (this value) * Arrays.SystemSettings.maxMultithreadingMemory() ~ 1 TB
    // (for default settings)

    private static final int MAX_NUMBER_OF_TASKS = 262144;
    // Only for guarantee that overflow is impossible. This constant plus
    // MAX_NUMBER_OF_RANGES_FOR_CUSTOM_COPIER must be less than 2^31.

    private final long maxTempJavaMemory;

    BasicMorphology(ArrayContext context, long maxTempJavaMemory) {
        super(context);
        if (maxTempJavaMemory < 0) {
            throw new IllegalArgumentException("Negative maxTempJavaMemory argument");
        }
        this.maxTempJavaMemory = maxTempJavaMemory;
    }

    /**
     * Equivalent to {@link #getInstance(ArrayContext, long)
     * getInstance}(context, {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()}).
     *
     * @param context the {@link #context() context} that will be used by this object;
     *                can be {@code null}, then it will be ignored.
     * @return new instance of this class.
     */
    public static BasicMorphology getInstance(ArrayContext context) {
        return new BasicMorphology(context, Arrays.SystemSettings.maxTempJavaMemory());
    }

    /**
     * Returns new instance of this class.
     *
     * <p>The <tt>maxTempJavaMemory</tt> argument specifies the maximal amount of usual Java memory,
     * in bytes, that methods of this class may freely use for internal needs and for creating results.
     * It means: if the size of the resulting matrix, or some temporary matrix or array
     * (or, maybe, the summary size of several temporary matrices)
     * is not greater than this limit, then a method <i>may</i> (though not <i>must</i>)
     * use {@link SimpleMemoryModel} for creating such AlgART matrices (arrays) or may allocate usual Java arrays.
     * For allocating greater amount of memory, all methods should use, when possible, the memory model
     * specified by the context: {@link ArrayContext#getMemoryModel()}.
     *
     * @param context           the {@link #context() context} that will be used by this object;
     *                          can be {@code null}, then it will be ignored.
     * @param maxTempJavaMemory maximal amount of Java memory, in bytes, allowed for allocating
     *                          by methods of this class.
     * @return new instance of this class.
     * @throws IllegalArgumentException if the <tt>maxTempJavaMemory</tt> argument is negative.
     */
    public static BasicMorphology getInstance(ArrayContext context, long maxTempJavaMemory) {
        return new BasicMorphology(context, maxTempJavaMemory);
    }

    @Override
    public boolean isPseudoCyclic() {
        return true;
    }

    @Override
    protected Matrix<? extends PArray> asDilationOrErosion(
        Matrix<? extends PArray> src, Pattern pattern,
        boolean isDilation)
    {
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (!dimensionsAllowed(src, pattern)) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        PArray array = src.array();
        final boolean additionalDimension = pattern.dimCount() == src.dimCount() + 1;
        double[] increments = !additionalDimension ? null : new double[(int) pattern.pointCount()];
        if (additionalDimension) {
            pattern = pattern.maxBound(src.dimCount());
        }
        long[] rightwardShifts = toShifts(increments, array.length(), src.dimensions(), pattern, !isDilation);
        PArray[] shifted = new PArray[rightwardShifts.length];
        for (int k = 0; k < rightwardShifts.length; k++) {
            shifted[k] = rightwardShifts[k] == 0 ? array :
                (PArray) Arrays.asShifted(array, rightwardShifts[k]);
            if (increments != null && increments[k] != 0.0) {
                shifted[k] = Arrays.asFuncArray(LinearFunc.getInstance(increments[k], 1.0),
                    src.type(PArray.class),
                    shifted[k]);
                // Important! src.type(...) here is necessary to provide good implementation of asFuncArray (min/max)
                // below: it is optimized for situations when the type of result is the same as the type of all
                // operands. Note that there is no sense to compare elements (while calculating min or max)
                // with a precision, other than the desired final precision of the result.
            }
        }
//        System.out.println("!!" + java.util.Arrays.asList(shifted));
        if (rightwardShifts.length == 1) {
            return src.matrix(shifted[0]);
        }
        return src.matrix(Arrays.asFuncArray(isDilation ? Func.MAX : Func.MIN,
            src.type(PArray.class), shifted));
    }

    @Override
    protected Matrix<? extends UpdatablePArray> dilationOrErosion(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern, boolean isDilation,
        boolean disableMemoryAllocation)
    {
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (!dimensionsAllowed(src, pattern)) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the matrix mismatch");
        }
        if (dest != null && !dest.dimEquals(src)) {
            throw new SizeMismatchException("Destination and source matrix dimensions mismatch: "
                + dest + " and " + src);
        }
        Matrix<? extends UpdatablePArray> castDest = dest == null || dest.elementType() == src.elementType() ? dest :
            Matrices.asUpdatableFuncMatrix(true, Func.UPDATABLE_IDENTITY,
                src.updatableType(UpdatablePArray.class), dest);
        Matrix<? extends UpdatablePArray> result = dilationOrErosion(castDest, src,
            null, pattern, isDilation, disableMemoryAllocation);
        if (dest != null) {
            if (result.array() != castDest.array()) { // some methods below recreate matrices based on of same arrays
                Matrices.copy(null, castDest, result);
            }
            result = dest;
        }
        return result;
    }

    @Override
    protected boolean dimensionsAllowed(Matrix<? extends PArray> matrix, Pattern pattern) {
        int patternDimCount = pattern.dimCount();
        int matrixDimCount = matrix.dimCount();
        return patternDimCount == matrixDimCount || patternDimCount == matrixDimCount + 1;
    }

    static long[] toShifts(
        double[] resultLastCoordinateIncrements,
        long totalLength, long[] dimensions, Pattern pattern, boolean symmetric)
    {
        Set<Point> points = pattern.points();
        long[] result = new long[points.size()];
        int k = 0;
        for (Point p : points) {
            double v = 0.0;
            if (p.coordCount() != dimensions.length) {
                v = p.coord(dimensions.length);
                p = p.projectionAlongAxis(dimensions.length);
            }
            long shift = p.toRoundedPoint().toOneDimensional(dimensions, true);
            assert shift >= 0 && (shift < totalLength || (totalLength == 0 && shift == 0)) :
                "illegal result of toOneDimensional(" + JArrays.toString(dimensions, ", ", 100)
                    + ", true) for point " + p + ": " + shift;
            if (symmetric && shift != 0) {
                shift = totalLength - shift;
            }
            if (symmetric) {
                v = -v;
            }
            if (resultLastCoordinateIncrements != null) {
                resultLastCoordinateIncrements[k] = v;
            }
            result[k] = shift;
            k++;
        }
        assert k == result.length;
        return result;
    }

    // possibleDest and pool may be null: then it will be allocated
    private Matrix<? extends UpdatablePArray> dilationOrErosion(
        Matrix<? extends UpdatablePArray> possibleDest,
        Matrix<? extends PArray> src,
        ArrayPool pool,
        Pattern pattern, boolean isDilation, boolean disableMemoryAllocation)
    {
//        long t0 = System.nanoTime();
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(pattern, "Null pattern argument");
        assert possibleDest == null || possibleDest.elementType() == src.elementType();
        PArray array = src.array();
        final long length = array.length();
        final long[] dimensions = src.dimensions();
        final boolean additionalDimension = pattern.dimCount() == dimensions.length + 1;
        final boolean simpleAlgorithm = disableMemoryAllocation || isSmall(pattern)
            || length == 0
            || !(additionalDimension ? pattern.projectionAlongAxis(dimensions.length) : pattern).isSurelyInteger();
        // If length==0, some algorithms alike compactCyclicPositions will not work: let's use the simplest solution.
        // If pattern is not integer, Minkowski decomposition of this pattern or
        // of the elements of union decomposition will work incorrectly: several dilations/erosions by rounded shifts
        // will not be equal to dilation/erosion by the set of original rounded points.

        List<Pattern> minkowskiDecomposition = !simpleAlgorithm ?
            pattern.minkowskiDecomposition(MIN_POINT_COUNT_TO_DECOMPOSE) :
            Collections.singletonList(pattern); // minkSize will be 1 if simpleAlgorithm
        final int minkSize = minkowskiDecomposition.size();
        assert minkSize >= 1 : "illegal Minkowski decomposition length";
        List<Pattern> unionDecomposition = null;
        int unionSize = 1;
        if (minkSize == 1 && !simpleAlgorithm && !additionalDimension) {
            // we know that the union decomposition usually cannot help in a case of additional dimension,
            // so we shall not think about the addition dimension in the union decomposition processing below
            assert pattern.isSurelyInteger() : "non-integer pattern must not be used for union decomposition";
            // - if not, simpleAlgorithm should be true, because there is no additional dimensions
            List<List<Pattern>> all = pattern.allUnionDecompositions(MIN_POINT_COUNT_TO_DECOMPOSE);
            assert all.size() >= 1 : "illegal length of the list of union decompositions";
            if (all.size() == 1 || !(array instanceof BitArray)) {
                unionDecomposition = all.get(0);
            } else {
                unionDecomposition = all.get(1); // optimization for a case when src.xDim() is k*2^m, m=16,32,64
            }
            unionSize = unionDecomposition.size();
            assert unionSize >= 1 : "illegal union decomposition length";
        }

        if (minkSize == 1 && unionSize == 1) { // simplest algorithm: no optimization ways
            if (possibleDest == null) {
                possibleDest = memoryModel().newMatrix(UpdatablePArray.class, src);
            }
            if (length > 0) { // else nothing to do
                if (additionalDimension) {
                    pattern = pattern.maxBound(dimensions.length);
                }
                double[] decrements = !additionalDimension ? null : new double[(int) pattern.pointCount()];
                long[] leftwardShifts = toShifts(decrements, length, dimensions, pattern, isDilation);
                simpleDilationOrErosion(possibleDest.array(), array, leftwardShifts, decrements, isDilation);
            }
            return possibleDest;
        }
        assert !simpleAlgorithm;
        // minkSize >= 2 means Minkowski decomposition, unionSize >= 2 means union decomposition

        MemoryModel mm = memoryModel();
        int numberOfTasks = Math.max(1, Arrays.getThreadPoolFactory(context()).recommendedNumberOfTasks(array));
        numberOfTasks = Math.min(numberOfTasks, MAX_NUMBER_OF_TASKS);
        int numberOfRanges = numberOfTasks == 1 ? 1 :
            (int) Math.min(Arrays.Copier.recommendedNumberOfRanges(array, false),
                // false to provide stable behaviour, not depending on possibleDest internal nature
                MAX_NUMBER_OF_RANGES_FOR_CUSTOM_COPIER);
        numberOfRanges = (int) Arrays.Copier.correctNumberOfRanges(numberOfRanges, numberOfTasks);
        if (minkSize >= 2) { // Minkowski decomposition
            long bufferLength = getBufferLengthForMinkowskiDecomposition(
                src, minkowskiDecomposition, isDilation, numberOfRanges);
            if (minkSize >= MIN_LENGTH_OF_DECOMPOSITION_FOR_USING_JAVA_MEMORY) {
                mm = mm(src, hasComplexPatterns(dimensions.length, minkowskiDecomposition) ? 3 : 1, bufferLength);
            }
            // - this estimation partially takes into consideration only 1 possible recursive level,
            // when minkowskiDilationOrErosion will call this method for non-trivial union decomposition,
            // that will allocate 2 additional work matrices
            if (possibleDest == null) {
                possibleDest = mm.newMatrix(UpdatablePArray.class, src);
            }
            if (pool == null) {
                pool = ArrayPool.getInstance(mm, src.elementType(), src.size());
            }
            UpdatablePArray buffer = (UpdatablePArray) mm.newUnresizableArray(src.elementType(), bufferLength);
            UpdatablePArray result = minkowskiDilationOrErosionWithoutAllocation(
                possibleDest.array(), src, buffer, pool, minkowskiDecomposition, isDilation, numberOfTasks);
            return src.matrix(result);

        } else { // union decomposition
            assert !additionalDimension;
            assert unionSize >= 2;
//            long t01 = System.nanoTime();
            long bufferLength = estimateBufferLengthForUnionDecomposition(
                src, pattern, unionDecomposition, numberOfRanges);
//            long t02 = System.nanoTime();
            if (unionSize >= MIN_LENGTH_OF_DECOMPOSITION_FOR_USING_JAVA_MEMORY) {
                mm = mm(src, 3, bufferLength);
            }
            if (possibleDest == null) {
                possibleDest = mm.newMatrix(UpdatablePArray.class, src);
            }
            if (pool == null) {
                pool = ArrayPool.getInstance(mm, src.elementType(), length);
            }
            UpdatablePArray tempForAccumulator = (UpdatablePArray) pool.requestArray();
            UpdatablePArray tempForMorph = null;
            UpdatablePArray buffer = bufferLength == length ? (UpdatablePArray) pool.requestArray() :
                (UpdatablePArray) mm.newUnresizableArray(array.elementType(), bufferLength);
//            long t03 = System.nanoTime();

            if (QUICK_UNION_DECOMPOSITION_ALGORITHM) {
                // Below is O(M) algorithm, log D <= M <= D, where D is the "diameter" of a convex pattern;
                // M = D if the groups of segments have no good Minkowski decomposition (as in a circle),
                // M = log D if the groups of segments have "log D" Minkowski decomposition (as in a rectangle).
                // The basic idea is decomposition to union of segments, for example, horizontal.
                // The compactUnionDecomposition method groups all segments with equal lengths
                // and builds a "Minkowski pair" for every group G:
                // the "normalized" horizontal segment - the first pattern MAIN
                // (with begin or end at the origin of coordinates)
                // and the set of shifts of all segments with the same length - the second pattern SHIFTS.
                // The union of segments of each group G is a Minkowski sum MAIN(+)SHIFTS.
                // So, to find the end result (dilation or erosion), we need to find
                // a dilation/erosion of the source matrix by the sum MAIN(+)SHIFTS for each "Minkowski pair"
                // and unite all such dilations/erosions by max/min operation in the accumulator ("possibleDest").
                // To find dilation src (+) (MAIN (+) SHIFT)= (src (+) MAIN) (+) SHIFT
                // (or erosion src (-) (MAIN (+) SHIFT) = (src (-) MAIN) (-) SHIFT),
                // we can use the result of the previous dilation (erosion) by the smaller segment.
                // Namely, let MAIN=MAIN'(+)LITTLE,
                // LITTLE is a little difference between previous segment MAIN' and the current segment MAIN
                // ("incrementFromPrevious" below).
                // Then src (+) MAIN = (src (+) MAIN') (+) LITTLE and can be calculated very quickly:
                // we just need to save the previous result src (+) MAIN' (we save it in "tempForMorph" below).
//                long t1 = System.nanoTime();
//                System.out.println("unionDecomposition: " + unionDecomposition);
                List<MinkowskiPair> compactedDecomposition = compactUnionDecomposition(unionDecomposition, isDilation);
//                long t2 = System.nanoTime();
                for (int k = 0, n = compactedDecomposition.size(); k < n; k++) {
                    MinkowskiPair pair = compactedDecomposition.get(k);
//                    System.out.printf("Pair %d / %d (for the original pattern %s):%n  %s%n"
//                        + "  main:                    %s%n"
//                        + "  increment from previous: %d items - %s%n"
//                        + "  decomposition of shifts: %d items - %s%n",
//                        k, n, pattern, pair,
//                        pair.main,
//                        pair.incrementFromPrevious == null ? 0 : pair.incrementFromPrevious.size(),
//                        pair.incrementFromPrevious,
//                        pair.shifts.minkowskiDecomposition(0).size(),
//                        pair.shifts.minkowskiDecomposition(0));
                    if (pair.incrementFromPrevious != null) {
                        // Best for performance: we may use previous dilation/erosion by segment,
                        // saved in tempForMorph, to switch to new (larger) segment length
                        assert tempForMorph != null : "Null tempForMorph";
                        long[][] leftwardShifts = toShifts(length, src.dimensions(),
                            pair.incrementFromPrevious, isDilation);
                        // Here we does not need to call optimizeMinkowskiDecomposition:
                        // all segments differences, prepared by compactUnionDecomposition,
                        // should have little leftward shifts.
                        // (Note: we use here the used Minkowski sum incrementFromPrevious in a form of list,
                        // prepared by compactUnionDecomposition. One of previous implementations used
                        // Minkowski sum in a form of Pattern and called its "minkowskiDecomposition" method here;
                        // sometimes it led to a bug, because that method returned another list.)
                        subTask(k, 1.0 / 8, n).simpleMinkowskiDilationOrErosionInPlace(
                            tempForMorph, buffer, leftwardShifts, isDilation, numberOfTasks);
                        subTask(k + 1.0 / 8, 7.0 / 8, n).accumulateDilationOrErosionWithoutAllocation(
                            possibleDest.array(), src.matrix(tempForMorph), pair.shifts, tempForAccumulator,
                            buffer, pool, isDilation, k == 0, numberOfTasks);
                        // - 1.0 / 8 here provides precise real division
                    } else if (pair.incrementToNext != null) {
                        // No special optimization is possible here, but we need to save
                        // the current dilation/erosion by segment for next iteration.
                        if (tempForMorph == null) {
                            tempForMorph = (UpdatablePArray) pool.requestArray();
                        }
                        UpdatablePArray result = subTask(k, 0.5, n).dilationOrErosionWithoutAllocation(
                            tempForMorph, src, pair.main, tempForAccumulator, pool, isDilation, numberOfTasks);
                        // - we use tempForAccumulator as a buffer here
                        if (result != tempForMorph) {
                            // swapping tempForMorph / tempForAccumulator, to place the result in tempForMorph
                            assert result == tempForAccumulator;
                            tempForAccumulator = tempForMorph;
                            tempForMorph = result;
                        }
                        subTask(k + 0.5, 0.5, n).accumulateDilationOrErosionWithoutAllocation(
                            possibleDest.array(), src.matrix(tempForMorph), pair.shifts, tempForAccumulator,
                            buffer, pool, isDilation, k == 0, numberOfTasks);
                    } else {
                        // Non-segment or isolated segment
                        if (tempForMorph == null) {
                            tempForMorph = (UpdatablePArray) pool.requestArray();
                        }
                        subTask(k, 1, n).accumulateDilationOrErosionWithoutAllocation(
                            possibleDest.array(), src,
                            pair.shifts.isSurelyOriginPoint() ?
                                pair.main :
                                Patterns.newMinkowskiSum(pair.main, pair.shifts),
                            tempForAccumulator, tempForMorph, pool, isDilation, k == 0, numberOfTasks);
                        // - we use tempForMorph as a buffer here: it will be reinitialized
                        // at the nearest iteration when pair.incrementToNext will be != null
                    }
                }
//                long t3 = System.nanoTime();
//                System.out.println(bufferLength + " Calculation time: " + (t1-t0)*1e-6 + " ms "
//                    + "(" + (t01-t0)*1e-6 + " + " + (t02-t01)*1e-6 + " + " + (t03-t02)*1e-6 + " + "
//                    + (t1-t03)*1e-6 + ") + "
//                    + (t2-t1)*1e-6 + " ms + " + (t3-t2)*1e-6 + " ms - " + pattern);
            } else {
                // Simple algorithm instead of previous loop; for internal testing only
                for (int k = 0; k <= unionSize; k++) {
                    Pattern ptn = null;
                    if (k < unionSize) {
                        ptn = unionDecomposition.get(k);
//                      System.out.println(k + ": " + ptn);
                        if (ptn.pointCount() == 1) { // shifting only, no sense to actualize it
                            // Why here? Extra branch? (Not important due to QUICK_UNION_DECOMPOSITION_ALGORITHM)
                            long rightwardShift = toShifts(null, length, dimensions, ptn, !isDilation)[0];
                            PArray a = rightwardShift == 0 ? array :
                                (PArray) Arrays.asShifted(array, rightwardShift);
                            if (k == 0) {
                                Arrays.copy(subTask(0, 1, unionSize).context(), possibleDest.array(), a);
                            } else {
                                subTask(k, 1, unionSize).minOrMax(possibleDest.array(), a, isDilation);
                            }
                            continue;
                        }
                    }
                    if (k < unionSize) {
                        subTask(k, 1, unionSize).accumulateDilationOrErosionWithoutAllocation(
                            possibleDest.array(), src, ptn, tempForAccumulator, buffer, pool,
                            isDilation, k == 0, numberOfTasks);
                    }
                }
            }

            pool.releaseArray(tempForAccumulator);
            pool.releaseArray(tempForMorph);
            if (bufferLength == length) {
                pool.releaseArray(buffer);
            }

            return possibleDest;
        }
    }

    // Returns possibleDest or buffer
    private UpdatablePArray dilationOrErosionWithoutAllocation(
        UpdatablePArray possibleDest,
        Matrix<? extends PArray> src, Pattern pattern, UpdatablePArray buffer, ArrayPool pool,
        boolean isDilation, int numberOfTasks)
    {
        if (pattern.dimCount() != src.dimCount()) {
            throw new AssertionError("dilationOrErosionWithoutAllocation must not be called for "
                + "patterns with additional dimension");
        }
        // - This method is called only while procession the union decomposition branch, which is skipped in this case
        List<Pattern> md = pattern.minkowskiDecomposition(MIN_POINT_COUNT_TO_DECOMPOSE);
        int m = md.size();
        if (m >= 2) {
            return minkowskiDilationOrErosionWithoutAllocation(
                possibleDest, src, buffer, pool, md, isDilation, numberOfTasks);
        } else {
            PArray array = src.array();
            long length = array.length();
            simpleDilationOrErosion(possibleDest, array,
                toShifts(null, length, src.dimensions(), pattern, isDilation),
                null,
                isDilation);
            return possibleDest;
        }
    }

    // For dilation, accumulator |= src (+) pattern
    // For erosion, accumulator &= src (-) pattern
    private void accumulateDilationOrErosionWithoutAllocation(
        UpdatablePArray accumulator,
        Matrix<? extends PArray> src, Pattern pattern,
        UpdatablePArray temp, UpdatablePArray buffer, ArrayPool pool,
        boolean isDilation, boolean accumulatorIsEmpty, int numberOfTasks)
    {
        if (pattern.dimCount() != src.dimCount()) {
            throw new AssertionError("accumulateDilationOrErosionWithoutAllocation must not be called for "
                + "patterns with additional dimension");
        }
        // - This method is called only while procession the union decomposition branch, which is skipped in this case
        List<Pattern> md = pattern.minkowskiDecomposition(MIN_POINT_COUNT_TO_DECOMPOSE);
        int m = md.size();
        if (m >= 2) {
            if (accumulatorIsEmpty) {
                UpdatablePArray result = minkowskiDilationOrErosionWithoutAllocation(
                    accumulator, src, buffer, pool, md, isDilation, numberOfTasks);
                if (result != accumulator) {
                    accumulator.copy(result);
                }
            } else {
                UpdatablePArray result = subTask(0, m, m + 1).minkowskiDilationOrErosionWithoutAllocation(
                    temp, src, buffer, pool, md, isDilation, numberOfTasks);
                subTask(m, 1, m + 1).minOrMax(accumulator, result, isDilation);
            }
        } else {
            PArray array = src.array();
            long length = array.length();
//            System.out.println("acc Dilation/erosion by points: " + pattern.roundedPoints());
//            System.out.println("acc Cardinality: " + Arrays.sumOf(accumulator));
            if (accumulatorIsEmpty) {
                simpleDilationOrErosion(accumulator, array,
                    toShifts(null, length, src.dimensions(), pattern, isDilation),
                    null,
                    isDilation);
            } else {
                Set<IPoint> points = pattern.roundedPoints();
                if (points.size() == 1) { // shifting only
                    long rightwardShift = toShifts(null, length, src.dimensions(), pattern, !isDilation)[0];
                    PArray a = rightwardShift == 0 ? array :
                        (PArray) Arrays.asShifted(array, rightwardShift);
                    minOrMax(accumulator, a, isDilation);
                } else {
                    PArray[] arrays = new PArray[points.size() + 1];
                    arrays[0] = accumulator;
                    long[] rightwardShifts = toShifts(null, length, src.dimensions(), pattern, !isDilation);

//                    System.out.println(" {" + JArrays.toString(rightwardShifts, ",", 100) + "}");
                    for (int k = 0; k < rightwardShifts.length; k++) {
                        arrays[k + 1] = rightwardShifts[k] == 0 ? array :
                            (PArray) Arrays.asShifted(array, rightwardShifts[k]);
                    }
                    minOrMax(accumulator, arrays, isDilation);
//                    System.out.println("acc New cardinality: " + Arrays.sumOf(accumulator));
                }
            }
        }
    }

    // Returns possibleDest or buffer
    private UpdatablePArray minkowskiDilationOrErosionWithoutAllocation(
        UpdatablePArray possibleDest, Matrix<? extends PArray> src,
        UpdatablePArray buffer, ArrayPool pool, List<Pattern> minkowskiDecomposition,
        boolean isDilation, int numberOfTasks)
    {
        int minkSize = minkowskiDecomposition.size();
        if (minkSize == 0) {
            throw new AssertionError("This method must not be called for empty minkowskiDecomposition list");
        }
        List<Pattern> goodPatterns = new ArrayList<Pattern>(minkowskiDecomposition);
        List<Pattern> complexPatterns = extractComplexPatterns(src.dimCount(), goodPatterns);
        int goodSize = goodPatterns.size();
        int complexSize = complexPatterns.size();
        assert goodSize + complexSize == minkSize;

        PArray array = src.array();
        long length = array.length();
        assert possibleDest.length() == length;
        long[][] leftwardShifts = toShifts(length, src.dimensions(), goodPatterns, isDilation);
        leftwardShifts = optimizeMinkowskiDecomposition(length, leftwardShifts);
        int totalSize = leftwardShifts.length + complexSize;

//        System.out.println("mm Dilation/erosion by " + minkowskiDecomposition
//            + " (" + goodSize + " good and " + complexSize + " complex patterns):");
//        for (Pattern ptn : complexPatterns)
//            System.out.print(" {" + ptn + "}");
//        for (long[] ls : leftwardShifts)
//            System.out.print(" {" + JArrays.toString(ls, ",", 100) + "}");
//        System.out.println();

        int complexIndex = 0;
        if (complexSize > 0) {
            assert buffer.length() == array.length() : "Illegal buffer length for complex Minkowski decomposition";
        }
        if (leftwardShifts.length > 0) {
            subTask(0, leftwardShifts.length, totalSize).simpleMinkowskiDilationOrErosion(
                possibleDest, array, buffer, leftwardShifts, isDilation, numberOfTasks);
        } else if (complexSize == 0) {
            // possible when minkowskiDecomposition consists of one 1-point pattern with the origin
            // (current implementation does not use this)
            Arrays.copy(context(), possibleDest, src.array());
        } else {
            subTask(0, 1, totalSize).dilationOrErosion(
                src.matrix(possibleDest), src, pool, complexPatterns.get(0),
                isDilation, false);
            complexIndex = 1;
        }
        for (; complexIndex < complexSize; complexIndex++) {
            // Here is the only RECURSIVE CALL of this module
            subTask(leftwardShifts.length + complexIndex, 1, totalSize).dilationOrErosion(
                src.matrix(buffer), src.matrix(possibleDest), pool, complexPatterns.get(complexIndex),
                isDilation, false);
            UpdatablePArray temp = buffer;
            buffer = possibleDest;
            possibleDest = temp;
        }
//        System.out.println("Cardinality: " + Arrays.sumOf(src.array()) + " -> " + Arrays.sumOf(possibleDest));
        return possibleDest;
    }

    // Destroys leftwardShifts! They must be sorted by increasing (exception the last element)!
    private void simpleMinkowskiDilationOrErosion(
        UpdatablePArray dest, PArray src,
        UpdatablePArray buffer, long[][] leftwardShifts, boolean isMax, int numberOfTasks)
    {
//        System.out.println("mm Simple dilation/erosion:");
//        for (long[] ls : leftwardShifts)
//            System.out.println(" {" + JArrays.toString(ls, ",", 100) + "}");

        int m = leftwardShifts.length;
        if (m == 0) {
            throw new AssertionError("This method must not be called for empty leftwardShifts array");
        }
        subTask(0, 1, m).simpleDilationOrErosion(dest, src, leftwardShifts[leftwardShifts.length - 1],
            null, //TODO!!
            isMax);
        // - The correction shift, that usually cannot be performed "in place" by common algorithm
        // (because it is rightward), should be placed at the end of array by optimizeMinkowskiDecomposition method.
        if (m == 1) {
            return;
        }
        for (int k = 1; k < m; k++) {
            subTask(k, 1, m).simpleDilationOrErosionInPlace(
                dest, buffer, leftwardShifts[k - 1], isMax, true, numberOfTasks);
        }
//        System.out.println("Cardinality: " + Arrays.sumOf(src) + " -> " + Arrays.sumOf(dest));
    }

    // Destroys leftwardShifts!
    private void simpleMinkowskiDilationOrErosionInPlace(
        UpdatablePArray array,
        UpdatablePArray buffer, long[][] leftwardShifts, boolean isMax, int numberOfTasks)
    {
        for (int k = 0, m = leftwardShifts.length; k < m; k++) {
            subTask(k, 1, m).simpleDilationOrErosionInPlace(
                array, buffer, leftwardShifts[k], isMax, false, numberOfTasks);
        }
    }

    // Destroys leftwardShifts!
    private void simpleDilationOrErosion(
        UpdatablePArray dest, PArray src,
        final long[] leftwardShifts, final double[] decrements, boolean isMax)
    {
        final long length = src.length();
        assert length == dest.length() : "src/dest array lengths mismatch";
        boolean additionalDimension = decrements != null;
        if (!additionalDimension) {
            Arrays.sort(SimpleMemoryModel.asUpdatableLongArray(leftwardShifts), (firstIndex, secondIndex) -> {
                long first = leftwardShifts[(int) firstIndex] - length >> 2;
                long second = leftwardShifts[(int) secondIndex] - length >> 2;
                return Math.abs(first) < Math.abs(second);
            });
            // Sorting by absolute value of the shift allows to better performance for bit array:
            // nextPosition will be based on non-shifted matrix, if the zero shift is inside the pattern.
            // While using additional dimension, it is usually not actual: the calculation are more complex.
        }
//        Runtime rt = Runtime.getRuntime(); System.gc(); System.gc(); System.gc();
        PArray[] shifted = new PArray[leftwardShifts.length];
//        System.out.printf("A %d %.3f MB used%n", shifted.length, (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
        for (int k = 0; k < leftwardShifts.length; k++) {
            shifted[k] = leftwardShifts[k] == 0 ? src :
                (PArray) Arrays.asShifted(src, -leftwardShifts[k]);
            if (additionalDimension && decrements[k] != 0.0) {
                shifted[k] = Arrays.asFuncArray(LinearFunc.getInstance(-decrements[k], 1.0),
                    dest.type(),
                    shifted[k]);
                // Important! dest.type() here is necessary to provide good implementation of asFuncArray (min/max)
                // below: it is optimized for situations when the type of result is the same as the type of all
                // operands. Note that there is no sense to compare elements (while calculating min or max)
                // with a precision, other than the desired final precision of the result.
            }
        }
//        System.out.printf("B %.3f MB used%n", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
//        System.gc(); System.gc(); System.gc();
//        System.out.printf("C %.3f MB used%n", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
//        System.out.println(java.util.Arrays.asList(shifted));
        PArray lazy = leftwardShifts.length == 1 ?
            shifted[0] :
            Arrays.asFuncArray(isMax ? Func.MAX : Func.MIN, dest.type(), shifted);
//        System.gc(); System.gc(); System.gc();
//        System.out.printf("D %.3f MB used%n", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
        Arrays.copy(context(), dest, lazy);
//        System.out.printf("E %.3f MB used%n", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
//        System.gc(); System.gc(); System.gc();
//        System.out.printf("F %.3f MB used%n", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
    }

    // Called for very small patterns, usually 2-point.
    // All leftwardShifts must be sorted (if shiftsAreSorted)
    // little non-negative values, usually starting from 0 (for better bug detection).
    private void simpleDilationOrErosionInPlace(
        UpdatablePArray array, UpdatablePArray buffer,
        long[] leftwardShifts, boolean isMax, boolean shiftsAreSorted, int numberOfTasks)
    {
        long length = array.length();
        for (int k = 1; k < leftwardShifts.length; k++) {
            if (leftwardShifts[k] < 0 || leftwardShifts[k] >= length) {
                throw new AssertionError("Illegal shift: not in 0.." + (length - 1) + " range");
            }
            if (shiftsAreSorted && (leftwardShifts[k] < leftwardShifts[k - 1])) {
                throw new AssertionError("Shifts are not sorted: " + JArrays.toString(leftwardShifts, ", ", 1000));
            }
        }
        if (leftwardShifts.length == 0 && leftwardShifts[0] == 0) {
            return; // nothing to do
        }
        long maxShift = Long.MIN_VALUE;
        for (long sh : leftwardShifts) {
            maxShift = Math.max(sh, maxShift);
        }
        if (maxShift > buffer.length()) {
            throw new AssertionError("Buffer length is less than maximal shift " + maxShift + ": buffer is " + buffer);
        }
        if (shiftsAreSorted) {
            assert maxShift == leftwardShifts[leftwardShifts.length - 1];
        }
        buffer.copy(array.subArr(0, maxShift));
        // - Saving the array begin: it will be damaged by the main part of the algorithm below.
        // This array begin is repeated at the end of "expanded" array - concatenation array|buffer.
        long mainLength = length - maxShift;
        // For all elements 0..mainLength-1 we just need to calculate MAX or MIN for several subarrays.
        // For the rest mainLength..length-1, we shall need to use subarrays of a concatenation array|buffer.

        // Processing the main part: 0..mainLength-1
        PArray[] shifted = new PArray[leftwardShifts.length];
        for (int k = 0; k < leftwardShifts.length; k++) {
            shifted[k] = array.subArr(leftwardShifts[k], mainLength);
        }
        PArray lazy = leftwardShifts.length == 1 ?
            shifted[0] :
            Arrays.asFuncArray(isMax ? Func.MAX : Func.MIN, array.type(), shifted);
        assert lazy.length() == mainLength;

        if (numberOfTasks == 1) { // no problems here
            Arrays.copy(contextPart(0.0, 0.95), array, lazy);
        } else { // we must not process last maxShift elements in every subrange
            assert numberOfTasks > 1 : "invalid numberOfTasks = " + numberOfTasks;
            final long endGap = maxShift;
            Arrays.Copier copier = new Arrays.Copier(contextPart(0.05, 0.90), array, lazy,
                numberOfTasks,
                Math.min(Arrays.Copier.recommendedNumberOfRanges(array, false),
                    MAX_NUMBER_OF_RANGES_FOR_CUSTOM_COPIER)) {
                @Override
                public long endGap(long rangeIndex) {
                    return rangeIndex < numberOfRanges - 1 ? endGap : 0;
                }
            };
            long nr = copier.numberOfRanges();
            assert nr == (int) nr; // because nr <= max(numberOfTasks, MAX_NUMBER_OF_RANGES_FOR_CUSTOM_COPIER)
            final int numberOfSavings = (int) nr - 1;
            long[] savingRangesLengths = new long[numberOfSavings + 1];
            long[] savingRangesFrom = new long[numberOfSavings + 1];
            long[] savingPosInBuffer = new long[numberOfSavings + 1];
            savingPosInBuffer[0] = maxShift;
            for (int m = 0; m < numberOfSavings; m++) {
                long gap = Math.min(maxShift, copier.rangeLength(m));
                savingRangesLengths[m] = gap;
                savingRangesFrom[m] = copier.rangeTo(m) - gap;
                savingPosInBuffer[m + 1] = savingPosInBuffer[m] + gap;
            }
            if (savingPosInBuffer[numberOfSavings] > buffer.length()) {
                throw new AssertionError("Too short buffer for multithread saving: " + buffer.length()
                    + " < " + savingPosInBuffer[numberOfSavings]);
            }
            // Proof that it is impossible.
            // We've chosen buffer enough to store min(numberOfRanges*maxShift,array.length()) elements,
            // for any shifts.
            // 1. Sum of all savingRangesLengths[m] is not greater than lazy.length(), so,
            // maxShift + this sum <= array.length().
            // 2. All gaps <= maxShift, so, this sum <= numberOfRanges*maxShift.
            UpdatablePArray[] savingBuffers = new UpdatablePArray[numberOfSavings];
            for (int m = 0; m < numberOfSavings; m++) {
                savingBuffers[m] = buffer.subArray(savingPosInBuffer[m], savingPosInBuffer[m + 1]);
                Arrays.copy(contextPart(0.05 * m / numberOfSavings, 0.05 * (m + 1) / numberOfSavings),
                    savingBuffers[m], lazy.subArr(savingRangesFrom[m], savingRangesLengths[m]));
                // multithreading is allowed here, though usually has no sense
            }
            copier.process(); // main part of algorithm: processing numberOfRanges regions
            for (int m = 0; m < numberOfSavings; m++) {
                Arrays.copy(contextPart(0.9 + 0.05 * m / numberOfSavings, 0.9 + 0.05 * (m + 1) / numberOfSavings),
                    array.subArr(savingRangesFrom[m], savingRangesLengths[m]), savingBuffers[m]);
                // multithreading is allowed here, though usually has no sense
            }
        }

        // Processing the rest: mainLength..length-1
        PArray expanded = (PArray) Arrays.asConcatenation(array, buffer);
        // Theoretically, TooLargeArrayException is possible here. But really it is impossible:
        // this branch is used only if we have ACTUAL (not lazy) array, which cannot be so large (~2^63 elements).
        // And even if we suppose an ability of such array, this branch is called only in a situation
        // when allocation memory is allowed (at least, for the appended buffer); it means that
        // TooLargeArrayException is allowed here and is not a violation of the contract,
        // because OutOfMemoryError is allowed.
        for (int k = 0; k < leftwardShifts.length; k++) {
            shifted[k] = (PArray) expanded.subArr(mainLength + leftwardShifts[k], maxShift);
        }
        lazy = leftwardShifts.length == 1 ?
            shifted[0] :
            Arrays.asFuncArray(isMax ? Func.MAX : Func.MIN, array.type(), shifted);
        assert lazy.length() == maxShift;
        Arrays.copy(contextPart(0.95, 1.0),
            array.subArr(mainLength, maxShift), lazy, 1);
        // multithreading is NOT allowed here, because it is in-place operation
    }

    private void minOrMax(UpdatablePArray dest, PArray src, boolean isMax) {
        Arrays.applyFunc(context(), isMax ? Func.MAX : Func.MIN, dest, dest, src);
    }

    private void minOrMax(UpdatablePArray dest, PArray[] src, boolean isMax) {
        Arrays.applyFunc(context(), isMax ? Func.MAX : Func.MIN, dest, src);
    }

    private static boolean isSmall(Pattern pattern) {
        if (pattern instanceof QuickPointCountPattern) {
            return pattern.pointCount() <= MIN_POINT_COUNT_TO_DECOMPOSE;
        } else {
            // don't call pointCount() for such pattern: it can be slow
            return false;
        }
    }

    // Quick check for our possible rectangular pattern, including 1- and 2-point ones
    private static boolean isRectangularOrVerySmall(Pattern pattern) {
        return pattern instanceof RectangularPattern
            || (pattern instanceof QuickPointCountPattern && pattern.pointCount() <= 2);
    }

    private static boolean isComplex(int matrixDimCount, Pattern pattern) {
        if (pattern.dimCount() != matrixDimCount || pattern.hasMinkowskiDecomposition()) {
            // we shall not try to optimally process Minkowski sums of patterns with an extra dimension
            return true;
        }
        List<List<Pattern>> ud = pattern.allUnionDecompositions(MIN_POINT_COUNT_TO_DECOMPOSE);
        return ud.size() > 1 || ud.get(0).size() > 1;
    }

    private static boolean hasComplexPatterns(int matrixDimCount, List<Pattern> patterns) {
        for (Pattern ptn : patterns) {
            if (isComplex(matrixDimCount, ptn)) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> extractComplexPatterns(int matrixDimCount, List<Pattern> patterns) {
        List<Pattern> result = new ArrayList<Pattern>();
        int newLength = 0, n = patterns.size();
        for (int k = 0; k < n; k++) {
            Pattern ptn = patterns.get(k);
            if (isComplex(matrixDimCount, ptn)) {
                result.add(ptn);
            } else {
                patterns.set(newLength++, ptn);
            }
        }
        patterns.subList(newLength, n).clear();
        return result;
    }

    // src.size() means that full 2-matrix swapping algorithm is necessary
    private static long getBufferLengthForMinkowskiDecomposition(
        Matrix<? extends PArray> src,
        List<Pattern> minkowskiDecomposition,
        boolean isDilation, int numberOfRanges)
    {
        long length = src.size();
        if (hasComplexPatterns(src.dimCount(), minkowskiDecomposition)) {
            return length;
        }
        long[][] leftwardShifts = toShifts(length, src.dimensions(), minkowskiDecomposition, isDilation);
        leftwardShifts = optimizeMinkowskiDecomposition(length, leftwardShifts);
        long maxShift = 0;
        for (long[] ptn : leftwardShifts) {
            if (ptn.length > 1) { // 1-point pattern is processed by special way and does not require buffer
                maxShift = Math.max(maxShift, ptn[ptn.length - 1]);
            }
        }
        assert maxShift < length;
        if (maxShift >= length / numberOfRanges) {
            return length;
        }
        return maxShift * numberOfRanges;
    }

    private static long estimateBufferLengthForUnionDecomposition(
        Matrix<?> src,
        Pattern pattern, List<Pattern> unionDecomposition, int numberOfRanges)
    {
        long length = src.size();
        for (Pattern ptn : unionDecomposition) {
            List<Pattern> md = ptn.minkowskiDecomposition(MIN_POINT_COUNT_TO_DECOMPOSE);
            if (hasComplexPatterns(src.dimCount(), md)) {
                return length;
            }
        }
        long[] rightBottomCorner = new long[pattern.dimCount()]; // really right-bottom for 2-dimensional case
        for (int k = 0; k < rightBottomCorner.length; k++) {
            rightBottomCorner[k] = pattern.roundedCoordRange(k).size() - 1;
            if (rightBottomCorner[k] >= src.dim(k)) { // i.e. pattern.roundedCoordRange(k).size() > src.dim(k)
                return length; // the simplest solution
            }
        }
        for (Pattern ptn : unionDecomposition) { // additional testing (to be on the safe side)
            for (int k = 0; k < rightBottomCorner.length; k++) {
                long ptnDimK = ptn.roundedCoordRange(k).size() - 1;
                if (ptnDimK > rightBottomCorner[k]) {
                    throw new AssertionError("Invalid union decomposition of " + pattern
                        + ": element " + ptn + " of the union is larger than the full pattern; "
                        + "the union decomposition is " + unionDecomposition);
                }
            }
        }
        // Now we are sure that the pattern is not greater than the matrix along all coordinates;
        // so, we can be sure that the necessary shift for any its point is not greater than
        // the shift for the "right-bottom corner" of the circumscribed parallelepiped
        long shift = IPoint.valueOf(rightBottomCorner).toOneDimensional(src.dimensions(), true);
        assert shift <= length;
        if (shift >= length / numberOfRanges) {
            return length;
        }
        return shift * numberOfRanges;
    }

    // Removes all 1-point patterns and compact all multipoint patterns;
    // adds the summary correction shift as the last element (if necessary)
    private static long[][] optimizeMinkowskiDecomposition(long totalLength, long[][] shifts) {
        List<long[]> result = new ArrayList<long[]>();
        long summaryCorrection = 0;
        for (long[] ptn : shifts) {
            assert ptn.length > 0;
            if (ptn.length == 1) {
                summaryCorrection += ptn[0];
            } else {
                summaryCorrection += Arrays.compactCyclicPositions(totalLength, ptn);
                result.add(ptn);
            }
        }
        if (summaryCorrection != 0) {
            result.add(new long[]{summaryCorrection});
        }
        return result.toArray(new long[result.size()][]);
    }

    // Places all segments, contained in the list of patterns, to the start of the list in order
    // of increasing their length; replaces all segments by their implementations from our package.
    // Returns new list in the method result and does not modify the argument.
    // Called from compactUnionDecomposition only.
    private static List<Pattern> optimizeUnionDecomposition(List<Pattern> patterns) {
        int maxDimCount = 0;
        for (Pattern ptn : patterns) {
            maxDimCount = Math.max(maxDimCount, ptn.dimCount());
        }
        List<Pattern> source = new LinkedList<Pattern>(patterns);
        List<Pattern> result = new ArrayList<Pattern>();
        for (int k = maxDimCount - 1; k >= 0; k--) {
            int before = result.size();
            for (Iterator<Pattern> iterator = source.iterator(); iterator.hasNext(); ) {
                Pattern ptn = iterator.next();
                if (isSegmentAlongTheAxis(ptn, k)) {
                    iterator.remove();
                    result.add(Patterns.newRectangularIntegerPattern(ptn.roundedCoordArea().ranges()));
                    // newRectangularIntegerPattern gives a guarantee that we shall have a good implementation
                    // from this package, even if the original ptn, for example, is just a simple pattern
                }
            }
            int after = result.size();
            Collections.sort(result.subList(before, after), new Comparator<Pattern>() {
                public int compare(Pattern o1, Pattern o2) {
                    long count1 = o1.pointCount();
                    long count2 = o2.pointCount();
                    return count1 < count2 ? -1 : count1 == count2 ? 0 : 1;
                }
            });
        }
        result.addAll(source);
        return result;
    }

    private static List<MinkowskiPair> compactUnionDecomposition(List<Pattern> patterns, boolean negativeSegments) {
        patterns = optimizeUnionDecomposition(patterns);
        List<MinkowskiPair> result = new ArrayList<MinkowskiPair>();
        Pattern lastNormalized = null;
        Set<IPoint> shiftsOfEqualSegments = new HashSet<IPoint>();
        for (Pattern ptn : patterns) {
            Pattern normalized;
            List<Pattern> minkowskiIncrement;
            IPoint rectEndOrStart;
            if (!isRectangularOrVerySmall(ptn)) {
                // surely not a segment: avoiding slow calls of coordMin, coordMax, shift methods
                // and providing a correct, non-shifted pattern for the quick algorithm
                rectEndOrStart = IPoint.origin(ptn.dimCount());
                normalized = ptn;
                minkowskiIncrement = null;
            } else {
                Point preciseRectEndOrStart = negativeSegments ? ptn.coordMax() : ptn.coordMin();
                assert preciseRectEndOrStart.isInteger();
                // - this method is called only in union-decomposition mode,
                // which is not used for non-integer or (N+1)-dimensional patterns
                rectEndOrStart = preciseRectEndOrStart.toRoundedPoint();
                normalized = ptn.shift(rectEndOrStart.symmetric().toPoint());
                // normalized segment has origin at the left or right end:
                // so minkowskiIncrement will consists of little positive / negative points
                // that provide good (small) leftward / rightward shifts
                minkowskiIncrement = lastNormalized == null ?
                    null :
                    minkowskiSubtractSegment(normalized, lastNormalized);
            }
            boolean equalSegments = minkowskiIncrement != null && minkowskiIncrement.size() == 1
                && minkowskiIncrement.get(0).isSurelyOriginPoint();
            if (minkowskiIncrement == null || !equalSegments) {
                // If minkowskiIncrement!=null, "normalized" and "lastNormalized" are segments along the same axis
                // (equal segments if minkowskiIncrement.size()==1 && minkowskiIncrement.get(0).isOriginPoint().
                // If minkowskiIncrement==null, then
                // either "lastNormalized" is not a segment and should be saved in result (if !=null),
                // or we should save the accumulated set of shifted copies of "lastNormalized" segment.
                if (lastNormalized != null) {
                    result.add(new MinkowskiPair(
                        lastNormalized, // - main pattern, probably segment
                        shiftsOfEqualSegments,
                        minkowskiIncrement));
                    shiftsOfEqualSegments.clear();
                }
            }
            shiftsOfEqualSegments.add(rectEndOrStart);
            lastNormalized = normalized;
        }
        if (lastNormalized != null) {
            result.add(new MinkowskiPair(
                lastNormalized,
                shiftsOfEqualSegments,
                null));
        }
        for (int k = 0, n = result.size(); k < n; k++) {
            MinkowskiPair pair = result.get(k);
            List<Pattern> incrementFromPrevious = k == 0 ? null : result.get(k - 1).incrementToNext;
            if (incrementFromPrevious == null && pair.incrementToNext == null) {
                // isolated pattern, in particular, not a segment
                if (pair.shifts.pointCount() == 1) {
                    // so, there is no sense to store the shift and the basic pattern separately
                    IPoint shift = pair.shifts.roundedPoints().iterator().next();
                    if (!shift.isOrigin()) {
                        pair = new MinkowskiPair(
                            pair.main.shift(shift.toPoint()),
                            Collections.singleton(IPoint.origin(shift.coordCount())),
                            null);
                    }
                }
            }
            pair.incrementFromPrevious = incrementFromPrevious;
            result.set(k, pair); // necessary in a case of rebuilding the pair for an isolated pattern
        }
        return result;
    }

    private static class MinkowskiPair {
        /**
         * The segment or another pattern.
         */
        final Pattern main;

        /**
         * Additional Minkowski summand for segment, {origin} for other patterns.
         */
        final Pattern shifts;

        /**
         * Difference between this and next segment in a form of Minkowski decomposition,
         * {@code null} if there is no such difference.
         */
        final List<Pattern> incrementToNext;

        /**
         * Difference between this and previous segment in a form of Minkowski decomposition,
         * {@code null} if there is no such difference. Filled later.
         */
        List<Pattern> incrementFromPrevious = null;

        MinkowskiPair(Pattern main, Set<IPoint> shifts, List<Pattern> incrementToNext) {
            Objects.requireNonNull(main, "Null main argument");
            Objects.requireNonNull(shifts, "Null shifts argument");
            this.main = main;
            this.shifts = Patterns.newIntegerPattern(shifts);
            this.incrementToNext = incrementToNext;
        }

        public String toString() {
            return "Main pattern [" + main + "] (+) shifts [" + shifts + "]"
                + (incrementFromPrevious == null && incrementToNext == null ? ", isolated" : "")
                + (incrementFromPrevious == null ? "" :
                ", good element increment from the previous: Minkowski sum of " + incrementFromPrevious)
                + (incrementToNext == null ? "" :
                ", good element increment to the next: Minkowski sum of " + incrementToNext);
        }
    }

    private static boolean isSegmentAlongTheAxis(Pattern pattern, int coordIndex) {
        if (!(pattern instanceof QuickPointCountPattern)) {
            return false; // in other case, the checking can be too slow
        }
        if (pattern.pointCount() == 1) {
            return true;
        }
        return pattern instanceof UniformGridPattern && ((UniformGridPattern) pattern).isActuallyRectangular()
            && !((QuickPointCountPattern) pattern).isPointCountVeryLarge()
            && pattern.roundedCoordRange(coordIndex).size() == pattern.pointCount();
    }

    /**
     * Returns Minkowski decomposition of such pattern p that larger=smaller(+)p,
     * or {@code null} if there is no such pattern.
     * Works with 1-dimensional segments; for all other types of the passed patterns,
     * returns {@code null}.
     * Returns best results if at least minimal or maximal segment ends are equal.
     *
     * @param larger  larger pattern.
     * @param smaller smaller pattern.
     * @return Minkowski decomposition of such pattern p that larger=smaller(+)p or {@code null}.
     */
    private static List<Pattern> minkowskiSubtractSegment(Pattern larger, Pattern smaller) {
        final int dimCount = larger.dimCount();
        if (smaller.dimCount() != dimCount) {
            return null;
        }
        int axis = -1;
        for (int k = 0; k < dimCount; k++) {
            if (isSegmentAlongTheAxis(larger, k)) {
                axis = k;
                break;
            }
        }
        if (axis == -1) {
            return null;
        }
        if (!isSegmentAlongTheAxis(smaller, axis)) {
            return null;
        }
        long largerLength = larger.pointCount();
        long smallerLength = smaller.pointCount();
        if (largerLength < smallerLength) {
            return null;
        }
        long[] rightShift = new long[dimCount];
        boolean sameRightEnd = true;
        for (int k = 0; k < dimCount; k++) {
            rightShift[k] = larger.roundedCoordRange(k).max() - smaller.roundedCoordRange(k).max();
            sameRightEnd &= rightShift[k] == 0;
        }
        if (largerLength == smallerLength) {
            return Collections.<Pattern>singletonList(Patterns.newIntegerPattern(IPoint.valueOf(rightShift)));
        }
        ArrayList<Pattern> result = new ArrayList<Pattern>();
        long[] leftShift = rightShift; // optimization (no allocation new array): rightShift will not be used below
        boolean sameLeftEnd = true;
        for (int k = 0; k < dimCount; k++) {
            leftShift[k] = larger.roundedCoordRange(k).min() - smaller.roundedCoordRange(k).min();
            sameLeftEnd &= leftShift[k] == 0;
        }
        boolean negativeSegments = sameRightEnd;
        if (!negativeSegments && !sameLeftEnd) {
            result.add(Patterns.newIntegerPattern(IPoint.valueOf(leftShift)));
        }
        // Building Minkowski decomposition of left..0 segment (inclusive), without pairs less than smallLength
        IPoint origin = IPoint.origin(dimCount);
        long len = largerLength - smallerLength;
        while (len > 0) {
            // We need to add 0..len segment
            if (len <= smallerLength) {
                result.add(Patterns.newIntegerPattern(origin, origin.shiftAlongAxis(axis, negativeSegments ? -len : len)));
                break;
            }
            long newLen = len >> 1;
            result.add(Patterns.newIntegerPattern(origin, origin.shiftAlongAxis(axis,
                negativeSegments ? newLen - len : len - newLen)));
            len = newLen;
        }
        return result;
    }

    private static long[][] toShifts(long totalLength, long[] dimensions, List<Pattern> patterns, boolean symmetric) {
        long[][] result = new long[patterns.size()][];
        int k = 0;
        for (Pattern ptn : patterns) {
            result[k++] = toShifts(null, totalLength, dimensions, ptn, symmetric);
        }
        assert k == result.length;
        return result;
    }

    /**
     * Equivalent to <tt>context==null ? thisInstance : {@link #context(ArrayContext)
     * context}(context().{@link ArrayContext#part(double, double)
     * part}(fromPart/totalSize, (fromPart+subtaskSize)/totalSize))</tt>.
     *
     * <p>This method is useful to perform a subtask of the full morphology task
     * with correct progress visualization via
     * {@link ArrayContext#updateProgress(net.algart.arrays.ArrayContext.Event) ArrayContext.updateProgress} method.
     *
     * @param fromPart      the estimated ready part, from 0.0 to <tt>totalTaskSize</tt>, of the total algorithm
     *                      of the total algorithm at the start of the subtask.
     * @param subTaskSize   the estimated length of the subtask, from 0.0 to <tt>totalTaskSize-fromPart</tt>.
     * @param totalTaskSize the total length of full task in some conventional units.
     * @return new instance of this class corresponding to the subtask of the full algorithm,
     *         from <tt>fromPart/totalTaskSize*100%</tt>
     *         to <tt>(fromPart+subtaskSize/totalTaskSize*100%</tt>.
     * @throws IllegalArgumentException if <tt>fromPart</tt> is not in <tt>0.0..totalTaskSize</tt> range,
     *                                  or if <tt>subtaskSize&gt;totalTaskSize-fromPart</tt>,
     *                                  or if <tt>subtaskSize&lt;0.0</tt>,
     *                                  or if <tt>totalTaskSize&lt;0.0</tt>.
     */
    private BasicMorphology subTask(double fromPart, double subTaskSize, double totalTaskSize) {
        if (totalTaskSize < 0.0) {
            throw new IllegalArgumentException("Negative totalTaskSize");
        }
        if (subTaskSize < 0.0) {
            throw new IllegalArgumentException("Negative subTaskSize");
        }
        double from = fromPart / totalTaskSize;
        double to = (fromPart + subTaskSize) / totalTaskSize;
        if (to > 1.0 && to <= 1.001) {
            to = 1.0; // to be on the safe side while estimative calculations in this class
        }
        return (BasicMorphology) context(contextPart(from, to));
    }

    private MemoryModel mm(Matrix<? extends PArray> matrix, int numberOfMatrices, long bufferLength) {
        long matrixMemory = Arrays.longMul(Matrices.sizeOf(matrix), numberOfMatrices);
        if (matrixMemory == Long.MIN_VALUE) { // overflow
            return memoryModel();
        }
        assert matrixMemory >= 0; // sizeOf works always for PArray
        long bufferMemory = Arrays.sizeOf(matrix.elementType(), bufferLength);
        if (matrixMemory > maxTempJavaMemory - bufferMemory) {
            return memoryModel();
        } else {
            return SimpleMemoryModel.getInstance();
        }
    }
}
