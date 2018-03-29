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

import net.algart.math.IRange;

import java.util.Locale;

/**
 * <p>Implementation of
 * {@link Matrices#copyRegion(ArrayContext, Matrix, Matrix, net.algart.arrays.Matrices.Region, long...)}
 * method.</p>
 *
 * <p>This class is <b>thread-compatible</b> and recommended to be used in a single thread.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
abstract strictfp class ArraysMatrixRegionCopier {
    private static final boolean OPTIMIZE_POLYGON_2D = true;
    private static final boolean DEBUG_OPTIMIZE_POLYGON_2D =
        InternalUtils.getBooleanProperty("net.algart.arrays.ArraysMatrixRegionCopier.debugOptimizePolygon2D", false);
    private static final int MINIMAL_VERTICES_COUNT_TO_OPTIMIZE_POLYGON_2D = 3;
    private static final int MAXIMAL_MEMORY_TO_OPTIMIZE_POLYGON_2D = 128 * 1048576; // ~128MB
    private static final long OUTSIDE_SRC_INDEX = -1;

    private final ArrayContext context;
    private final Matrix<? extends UpdatableArray> dest;
    private final Matrix<? extends Array> src;
    private final UpdatableArray destArray;
    private final Array srcArray;
    final Object destJArray;
    private final Object srcJArray;
    final int destJArrayOffset;
    private final int srcJArrayOffset;
    private final long destDimX;
    private final long srcDimX;
    final long destDimY;
    private final long[] shifts;
    private final long[] srcCoordinates;
    private final long[] destCoordinates;
    private final boolean mustBeInside;

    private long copiedElementsCount, lastCopiedElementsCount;
    private long currentProgress, totalProgress; // can be Long.MAX_VALUE
    private long lastProgressTime = Long.MIN_VALUE;

    private ArraysMatrixRegionCopier(
        ArrayContext context, int maxNumberOfDimensions,
        Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
        long[] shifts, boolean mustBeInside)
    {
        this.context = context;
        this.dest = dest;
        this.src = src;
        this.destArray = dest.array();
        this.srcArray = src.array();
        this.destJArray = Arrays.javaArrayInternal(this.destArray);
        this.srcJArray = Arrays.javaArrayInternal(this.srcArray);
        this.destJArrayOffset = this.destJArray == null ? -1 : Arrays.javaArrayOffsetInternal(this.destArray);
        this.srcJArrayOffset = this.srcJArray == null ? -1 : Arrays.javaArrayOffsetInternal(this.srcArray);
        // checks above are necessary for a case of BitArray
        this.destDimX = dest.dimX();
        this.srcDimX = src.dimX();
        this.destDimY = dest.dimY();
        this.shifts = shifts;
        this.srcCoordinates = new long[maxNumberOfDimensions];
        this.destCoordinates = new long[maxNumberOfDimensions];
        this.mustBeInside = mustBeInside;
    }

    public static ArraysMatrixRegionCopier getInstance(
        ArrayContext context, int maxNumberOfDimensions,
        Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
        long[] shifts, Object outsideValue, boolean mustBeInside)
    {
        assert dest != null;
        assert src != null;
        assert maxNumberOfDimensions > 0;
        outsideValue = Matrices.castOutsideValue(outsideValue, dest.array());
        // - necessary even for null, to avoid NullPointerException below while auto-unboxing

        Class<?> elementType = dest.elementType();
        //[[Repeat() bit|boolean ==> char,,byte,,short,,int,,long,,float,,double;;
        //           Boolean     ==> Character,,Byte,,Short,,Integer,,Long,,Float,,Double;;
        //           Bit         ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
        //           (if\s+\()   ==> } else $1,,... ]]
        if (elementType == boolean.class) {
            return new BitArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Boolean) outsideValue, mustBeInside);
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        } else if (elementType == char.class) {
            return new CharArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Character) outsideValue, mustBeInside);
        } else if (elementType == byte.class) {
            return new ByteArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Byte) outsideValue, mustBeInside);
        } else if (elementType == short.class) {
            return new ShortArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Short) outsideValue, mustBeInside);
        } else if (elementType == int.class) {
            return new IntArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Integer) outsideValue, mustBeInside);
        } else if (elementType == long.class) {
            return new LongArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Long) outsideValue, mustBeInside);
        } else if (elementType == float.class) {
            return new FloatArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Float) outsideValue, mustBeInside);
        } else if (elementType == double.class) {
            return new DoubleArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                (Double) outsideValue, mustBeInside);
            //[[Repeat.AutoGeneratedEnd]]
        } else {
            return new ObjectArraysMatrixRegionCopier(context, maxNumberOfDimensions, dest, src, shifts,
                outsideValue, mustBeInside);
        }
    }

    public final void process(Matrices.Region destRegion) {
        initializeProgress(destRegion);
        boolean destFullyInside = true;
        boolean srcFullyInside = true;
        boolean srcFullyOutside = false;
        for (int k = 0, n = destRegion.n(); k < n; k++) {
            IRange destRange = destRegion.coordRange(k);
            long destMin = destRange.min();
            long destMax = destRange.max();
            if (destMin < 0 || destMax >= dest.dim(k)) {
                destFullyInside = false;
            }
            long srcMin = shifts.length > k ? destMin - shifts[k] : destMin;
            long srcMax = shifts.length > k ? destMax - shifts[k] : destMax;
            if (srcMin < 0 || srcMax >= src.dim(k)) {
                srcFullyInside = false;
            }
            if (srcMin >= src.dim(k) || srcMax < 0) {
                srcFullyOutside = true;
            }
        }
        JArrayPool bufferPool = this.destJArray != null || this.srcJArray != null ? null
            : this.destArray instanceof CharArray ? ArraysFuncImpl.CHAR_BUFFERS
            : this.destArray instanceof ByteArray ? ArraysFuncImpl.BYTE_BUFFERS
            : this.destArray instanceof ShortArray ? ArraysFuncImpl.SHORT_BUFFERS
            : this.destArray instanceof IntArray ? ArraysFuncImpl.INT_BUFFERS
            : this.destArray instanceof LongArray ? ArraysFuncImpl.LONG_BUFFERS
            : this.destArray instanceof FloatArray ? ArraysFuncImpl.FLOAT_BUFFERS
            : this.destArray instanceof DoubleArray ? ArraysFuncImpl.DOUBLE_BUFFERS
            : null;
        Object buf = bufferPool == null ? null : bufferPool.requestArray();
        try {
            SegmentCopier segmentCopier = destFullyInside && srcFullyInside ? new UncheckedSegmentCopier(buf)
                : mustBeInside ? new CheckedSegmentCopier(buf)
                : OPTIMIZE_POLYGON_2D && destCoordinates.length == 2 && srcFullyOutside
                ? (destFullyInside ? getUncheckedSegmentFiller2D() : getContinuedSegmentFiller2D())
                : new ContinuedSegmentCopier(buf);
            if (OPTIMIZE_POLYGON_2D && destRegion instanceof Matrices.Polygon2D) {
                if (processPolygon2D((Matrices.Polygon2D) destRegion, segmentCopier)) {
                    return;
                }
            }
            processRecursively(destRegion, segmentCopier);
        } finally {
            if (bufferPool != null) {
                bufferPool.releaseArray(buf);
            }
        }
    }

    /**
     * Copies the element <tt>#srcIndex</tt> from the source array to the element <tt>#destIndex</tt>
     * of the destination array or, if <tt>#srcIndex=={@link #OUTSIDE_SRC_INDEX}</tt>,
     * fills the element <tt>#destIndex</tt> of the destination array by the outside value.
     *
     * @param destIndex the index in the destination array.
     * @param srcIndex  the index in the source array or {@link #OUTSIDE_SRC_INDEX}.
     */
    abstract void copyElement(long destIndex, long srcIndex);

    /**
     * Fills <tt>count</tt> elements of this array, starting from <tt>position</tt> index,
     * by the outside value.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @throws IndexOutOfBoundsException for illegal <tt>position</tt> and <tt>count</tt>
     *                                   (<tt>position &lt; 0 || count &lt; 0 || position + count &gt; length()</tt>).
     * @see Arrays#zeroFill(UpdatableArray)
     */
    abstract void fill(long position, long count);

    UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
        return new UncheckedSegmentFiller2D();
    }

    ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
        return new ContinuedSegmentFiller2D();
    }

    private void processRecursively(Matrices.Region destRegion, SegmentCopier segmentCopier) {
        final int n = destRegion.n();
//        System.out.println("Copying " + destRegion + ", shifts=" + JArrays.toString(shifts, ",", 100));
        if (n == 1) {
            segmentCopier.copySegment(destRegion);
        } else { // the recursion
            final IRange destRange = destRegion.coordRange(n - 1);
            long destMin = destRange.min();
            long destMax = destRange.max();
            if (!mustBeInside) {
                destMin = Math.max(destMin, 0);
                destMax = Math.min(destMax, dest.dim(n - 1) - 1);
                if (destMin > destMax) {
                    return;
                }
            }
            Matrices.Region.MutableIRange mutableCoordRange = new Matrices.Region.MutableIRange();
            for (long k = destMin; k <= destMax; k++) {
                destCoordinates[n - 1] = k;
                srcCoordinates[n - 1] = shifts.length >= n ? k - shifts[n - 1] : k;
                if (destRegion.sectionIsUninterruptedSegment(k)) {
                    if (n != 2) {
                        throw new AssertionError("Invalid implementation of " + destRegion.getClass()
                            + ": sectionIsUninterruptedSegment must return true only for n=2 dimensions, but n=" + n);
                    }
                    if (destRegion.segmentSectionAtLastCoordinate(mutableCoordRange, k)) {
                        segmentCopier.copyUninterruptedSegment(mutableCoordRange.min, mutableCoordRange.max);
                    }
                } else {
                    for (Matrices.Region section : destRegion.sectionAtLastCoordinate(k)) {
                        if (section.n != n - 1) {
                            throw new AssertionError("Invalid implementation of " + destRegion.getClass()
                                + ": number of dimensions of its section is "
                                + section.n + " instead of " + n + "-1");
                        }
                        processRecursively(section, segmentCopier);
                    }
                }
            }
        }
    }

    private boolean processPolygon2D(Matrices.Polygon2D destPolygon, SegmentCopier segmentCopier) {
        assert destPolygon.n() == 2;
        final IRange yRange = destPolygon.coordRange(1);
        long yMin = yRange.min();
        long yMax = yRange.max();
        if (!mustBeInside) {
            yMin = Math.max(yMin, 0);
            yMax = Math.min(yMax, destDimY - 1);
            // limitation by 0..destDimY range is important to avoid "almost infinite" loops
            // for extremely large polygons already at the 1st pass;
            // it is not actual if mustBeInside, because in this case we will throw an exception soon
            if (yMin > yMax) {
                return true;
            }
        }
        if (yMax - yMin + 1 > Integer.MAX_VALUE) {
            // improbable, especially in 32-bit JVM: it means that the matrix destDimY is VERY large
            if (DEBUG_OPTIMIZE_POLYGON_2D) {
                System.out.println("amrc Too large polygon in y-dimension (>Integer.MAX_VALUE); "
                    + "switching to more simple algorithm to avoid OutOfMemory error");
            }
            return false;
            // maybe it is relatively simple polygon, but very large in y-dimension:
            // there are changes that it can be processed by default algorithm
        }
        final int m = destPolygon.verticesCount();
        if (m < MINIMAL_VERTICES_COUNT_TO_OPTIMIZE_POLYGON_2D) {
            // for simple polygons we prefer to save memory
            if (DEBUG_OPTIMIZE_POLYGON_2D) {
                System.out.println("amrc Very simple polygon (" + m + " < "
                    + MINIMAL_VERTICES_COUNT_TO_OPTIMIZE_POLYGON_2D + " vertices); "
                    + "switching to more simple algorithm");
            }
            return false;
        }

        long t1 = DEBUG_OPTIMIZE_POLYGON_2D ? System.nanoTime() : 0;
        final int[] intersectionsCounts = new int[(int) (yMax - yMin + 1)];
        // - zero-filled by Java
        boolean increasingY = false;
        for (int k = m - 1; k >= 0; k--) {
            final double vy1 = destPolygon.vertexY(k);
            final double vy2 = destPolygon.vertexY(k < m - 1 ? k + 1 : 0);
            if (vy1 < vy2) {
                increasingY = true;
                break;
            } else if (vy1 > vy2) {
                increasingY = false;
                break;
            }
            // else continue search: we need to know the common y-direction of the polyline
        }
        // In a degenerated case of single horizontal line, the value of increasingY will be never used
        for (int k = 0; k < m; k++) {
            final double vy1 = destPolygon.vertexY(k);
            final double vy2 = destPolygon.vertexY(k < m - 1 ? k + 1 : 0);
            final boolean vy1Integer = vy1 == StrictMath.floor(vy1);
            if (vy1 < vy2) {
                long vyMin = (long) StrictMath.ceil(vy1);
                long vyMax = (long) StrictMath.floor(vy2);
                if (vy1Integer && increasingY) {
                    // skipping v1 if the y-direction is not changed
                    vyMin++;
                }
                increasingY = true;
                vyMin = Math.max(vyMin, yMin);
                vyMax = Math.min(vyMax, yMax);
                for (long sectionY = vyMin; sectionY <= vyMax; sectionY++) {
                    final int yIndex = (int) (sectionY - yMin);
                    intersectionsCounts[yIndex]++;
                }
            } else if (vy1 > vy2) {
                long vyMax = (long) StrictMath.floor(vy1);
                long vyMin = (long) StrictMath.ceil(vy2);
                if (vy1Integer && !increasingY) {
                    // skipping v1 if the y-direction is not changed
                    vyMax--;
                }
                increasingY = false;
                vyMin = Math.max(vyMin, yMin);
                vyMax = Math.min(vyMax, yMax);
                for (long sectionY = vyMax; sectionY >= vyMin; sectionY--) {
                    final int yIndex = (int) (sectionY - yMin);
                    intersectionsCounts[yIndex]++;
                }
            }
            // else vy1 == vy2: even if it is integer, we will fill this edge by special algorithm
        }
        // increasingY already has a correct value for the loop beginning
        for (int i = 0; i < intersectionsCounts.length; i++) {
            if (intersectionsCounts[i] % 2 != 0) {
                throw new AssertionError("Odd number of intersection at horizontal #" + (yMin + i)
                    + ", line " + i + ": it must not occur");
            }
        }
        long t2 = DEBUG_OPTIMIZE_POLYGON_2D ? System.nanoTime() : 0;
        final int[] intersectionsIndexes = intersectionsCounts;
        // - the same memory for 2nd role
        for (int i = 1; i < intersectionsIndexes.length; i++) {
            intersectionsIndexes[i] += intersectionsIndexes[i - 1];
            // intersectionsIndexes[i] becomes equal to sum of all intersectionsCounts[0..i-1]
            if (intersectionsIndexes[i] < 0
                || intersectionsIndexes[i] > MAXIMAL_MEMORY_TO_OPTIMIZE_POLYGON_2D / 8)
            {
                // sum of 2 non-negative integers is negative only in a case of overflow (>2^31);
                // but we will not use the following algorithm even if the resulting number of intersections
                // is too large: it is better to use slower algorithm than to fail with OutOfMemory
                if (DEBUG_OPTIMIZE_POLYGON_2D) {
                    System.out.println("amrc Very complex polygon (" + m + " vertices): "
                        + "it consists of too large number of solid lines; "
                        + "switching to more simple algorithm to avoid OutOfMemory error");
                }
                return false;
            }
        }
        final double[] intersections = new double[intersectionsIndexes[intersectionsIndexes.length - 1]];
        // the storage for all intersections, splitted to continuous blocks for each horizontal;
        // positions of the end of blocks are in intersectionsIndexes
        if (intersections.length % 2 != 0) {
            throw new AssertionError("Odd number of intersections of the polygon and horizontals: it's impossible");
        }
        boolean hasHorizontalEdges = false;
        for (int k = 0; k < m; k++) {
            final double vx1 = destPolygon.vertexX(k);
            final double vy1 = destPolygon.vertexY(k);
            final double vx2 = destPolygon.vertexX(k < m - 1 ? k + 1 : 0);
            final double vy2 = destPolygon.vertexY(k < m - 1 ? k + 1 : 0);
            final boolean vy1Integer = vy1 == StrictMath.floor(vy1);
            if (vy1 < vy2) {
                long vyMin = (long) StrictMath.ceil(vy1);
                long vyMax = (long) StrictMath.floor(vy2);
                if (vy1Integer && increasingY) {
                    // skipping v1 if the y-direction is not changed
                    vyMin++;
                }
                increasingY = true;
                vyMin = Math.max(vyMin, yMin);
                vyMax = Math.min(vyMax, yMax);
                final double rel = (vx2 - vx1) / (vy2 - vy1);
                for (long sectionY = vyMin; sectionY <= vyMax; sectionY++) {
                    final int yIndex = (int) (sectionY - yMin);
                    final double x = vx1 + (sectionY - vy1) * rel;
                    intersections[--intersectionsIndexes[yIndex]] = x;
                }
            } else if (vy1 > vy2) {
                long vyMax = (long) StrictMath.floor(vy1);
                long vyMin = (long) StrictMath.ceil(vy2);
                if (vy1Integer && !increasingY) {
                    // skipping v1 if the y-direction is not changed
                    vyMax--;
                }
                increasingY = false;
                vyMin = Math.max(vyMin, yMin);
                vyMax = Math.min(vyMax, yMax);
                final double rel = (vx2 - vx1) / (vy2 - vy1);
                for (long sectionY = vyMax; sectionY >= vyMin; sectionY--) {
                    final int yIndex = (int) (sectionY - yMin);
                    final double x = vx1 + (sectionY - vy1) * rel;
                    intersections[--intersectionsIndexes[yIndex]] = x;
                }
            } else {
                hasHorizontalEdges = true;
            }
        }
        if (intersectionsIndexes[0] != 0) {
            throw new AssertionError("Now the indexes must point to the begin of each block in intersections array");
        }
        long t3 = DEBUG_OPTIMIZE_POLYGON_2D ? System.nanoTime() : 0;
        long segmentCount = 0;
        if (hasHorizontalEdges) {
            for (int k = 0; k < m; k++) {
                final double vy1 = destPolygon.vertexY(k);
                final double vy2 = destPolygon.vertexY(k < m - 1 ? k + 1 : 0);
                if (vy1 == vy2 && vy1 == StrictMath.floor(vy1)) {
                    // just draw this edge at the result if it is integer
                    final long i = (long) vy1;
                    if (i < yMin || i > yMax) {
                        continue;
                    }
                    final double vx1 = destPolygon.vertexX(k);
                    final double vx2 = destPolygon.vertexX(k < m - 1 ? k + 1 : 0);
                    destCoordinates[1] = i;
                    srcCoordinates[1] = shifts.length >= 2 ? i - shifts[1] : i;
                    final long minX, maxX;
                    if (vx1 < vx2) {
                        minX = (long) StrictMath.ceil(vx1);
                        maxX = (long) StrictMath.floor(vx2);
                    } else {
                        minX = (long) StrictMath.ceil(vx2);
                        maxX = (long) StrictMath.floor(vx1);
                    }
                    if (minX <= maxX) {
                        segmentCopier.copyUninterruptedSegment(minX, maxX);
                        segmentCount++;
                    }
                }
            }
        }
        long t4 = DEBUG_OPTIMIZE_POLYGON_2D ? System.nanoTime() : 0;
        for (long i = yMin; i <= yMax; i++) {
            final int yIndex = (int) (i - yMin);
            final int fromIndex = intersectionsIndexes[yIndex];
            final int toIndex = i == yMax ? intersections.length : intersectionsIndexes[yIndex + 1];
            if (fromIndex % 2 != 0 || toIndex % 2 != 0) {
                throw new AssertionError("Odd indexes at horizontal #" + yIndex + ", line " + i + ": it's impossible");
            }
            java.util.Arrays.sort(intersections, fromIndex, toIndex);
            destCoordinates[1] = i;
            srcCoordinates[1] = shifts.length >= 2 ? i - shifts[1] : i;
            for (int j = fromIndex; j < toIndex; j += 2) {
                final long minX = (long) StrictMath.ceil(intersections[j]);
                final long maxX = (long) StrictMath.floor(intersections[j + 1]);
                if (minX <= maxX) {
                    segmentCopier.copyUninterruptedSegment(minX, maxX);
                    segmentCount++;
                }
            }
        }
        long t5 = DEBUG_OPTIMIZE_POLYGON_2D ? System.nanoTime() : 0;
        if (DEBUG_OPTIMIZE_POLYGON_2D) {
            System.out.printf(Locale.US, "amrc Filling polygon with %d vertices, %d horizontals, %d intersections "
                    + "in %.3f ms: %.3f ms 1st pass, %.3f ms 2nd pass, "
                    + "%.3f ms filling horizontal edges%s + %.3f ms main filling loop "
                    + "(%d segments, %.4f mcs/segment)%n",
                m, intersectionsCounts.length, intersections.length,
                (t5 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6,
                hasHorizontalEdges ? "" : " (skipped)",
                (t5 - t4) * 1e-6,
                segmentCount, (t5 - t3) * 1e-3 / (double) segmentCount);
        }
        return true;
    }

    private void initializeProgress(Matrices.Region destRegion) {
        copiedElementsCount = 0;
        lastCopiedElementsCount = 0;
        currentProgress = 0;
        totalProgress = 1;
        for (int k = 0, n = destRegion.n(); k < n; k++) {
            long size = destRegion.coordRange(k).size();
            long product;
            if ((product = Arrays.longMul(totalProgress, size)) == Long.MIN_VALUE) {
                totalProgress = Long.MAX_VALUE;
                break; // overflow
            }
            totalProgress = product;
        }
    }

    private void updateProgressForSegment(IRange destRange) {
        long size = destRange.size();
        if (currentProgress + size < 0) { // overflow
            currentProgress = Long.MAX_VALUE;
        } else {
            currentProgress += size;
        }
        if (context != null && copiedElementsCount - lastCopiedElementsCount > 4096) { // overflow is unimportant here
            lastCopiedElementsCount = copiedElementsCount;
            long t = System.currentTimeMillis();
            if (lastProgressTime == Long.MIN_VALUE ||
                t - lastProgressTime > Arrays.SystemSettings.RECOMMENDED_TIME_OF_NONINTERRUPTABLE_PROCESSING)
            {
                lastProgressTime = t;
                context.checkInterruptionAndUpdateProgress(srcArray.elementType(), currentProgress, totalProgress);
            }
        }
    }

    abstract class SegmentCopier {
        final Object workJArray;
        final int workJArrayLength;

        SegmentCopier(Object workJArray) {
            this.workJArray = workJArray;
            this.workJArrayLength = workJArray == null ? 0 : java.lang.reflect.Array.getLength(workJArray);
        }

        abstract void copyUninterruptedSegment(long destMin, long destMax);

        abstract void copyInterruptedSegment(Matrices.Region destRegion, long destMin, long destMax);

        final void copySegment(Matrices.Region destRegion) {
            final IRange destRange = destRegion.coordRange(0);
            if (destRegion.isRectangular()) {
                copyUninterruptedSegment(destRange.min(), destRange.max());
            } else {
                // rare situation: only non-standard implementations of Matrices.Region
                copyInterruptedSegment(destRegion, destRange.min(), destRange.max());
            }
            updateProgressForSegment(destRange);
        }

        final void copyRange(long destIndex, long srcIndex, long len) {
            if (destJArray != null) {
                srcArray.getData(srcIndex, destJArray, destJArrayOffset + (int) destIndex, (int) len);
            } else if (srcJArray != null) {
                destArray.setData(destIndex, srcJArray, srcJArrayOffset + (int) srcIndex, (int) len);
            } else if (len < workJArrayLength) {
                srcArray.getData(srcIndex, workJArray, 0, (int) len);
                destArray.setData(destIndex, workJArray, 0, (int) len);
            } else {
                destArray.subArr(destIndex, len).copy(srcArray.subArr(srcIndex, len));
            }
        }
    }

    private class UncheckedSegmentCopier extends SegmentCopier {
        UncheckedSegmentCopier(Object workJArray) {
            super(workJArray);
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            destCoordinates[0] = destMin;
            long srcMin = shifts.length >= 1 ? destMin - shifts[0] : destMin;
            long destIndex = dest.uncheckedIndex(destCoordinates);
            assert destMax >= 0 && destMax < destDimX;
            long len = destMax - destMin + 1;
            copiedElementsCount += len;
            srcCoordinates[0] = srcMin;
            long srcIndex = src.uncheckedIndex(srcCoordinates);
            copyRange(destIndex, srcIndex, len);
        }

        @Override
        void copyInterruptedSegment(Matrices.Region destRegion, long destMin, long destMax) {
            destCoordinates[0] = destMin;
            long destIndex = dest.uncheckedIndex(destCoordinates);
            long destX = destMin;
            long srcX = shifts.length >= 1 ? destX - shifts[0] : destX;
            srcCoordinates[0] = srcX;
            long srcIndex = src.uncheckedIndex(srcCoordinates);
            for (; destX <= destMax; destX++, srcX++, destIndex++, srcIndex++) {
                destCoordinates[0] = destX;
                if (destRegion.contains(destCoordinates)) {
                    copyElement(destIndex, srcIndex);
                    copiedElementsCount++;
                }
            }
        }
    }

    private class CheckedSegmentCopier extends SegmentCopier {
        CheckedSegmentCopier(Object workJArray) {
            super(workJArray);
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            destCoordinates[0] = destMin;
            long srcMin = shifts.length >= 1 ? destMin - shifts[0] : destMin;
            long destIndex;
            try {
                destIndex = dest.index(destCoordinates);
            } catch (IndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException("Destination region, " + e.getMessage());
            }
            assert destMax >= 0 : "destMin is correct " + destMin + ", but destMax is negative " + destMax;
            if (destMax >= destDimX) {
                throw new IndexOutOfBoundsException("Destination region, index (" + destMax
                    + ") >= dim(0) (" + destDimX + ")");
            }
            long len = destMax - destMin + 1;
            long srcMax = srcMin + destMax - destMin;
            copiedElementsCount += len;
            srcCoordinates[0] = srcMin;
            long srcIndex = src.index(srcCoordinates);
            if (srcMax >= srcDimX) {
                throw new IndexOutOfBoundsException("Source region, index (" + srcMax
                    + ") >= dim(0) (" + srcDimX + ")");
            }
            copyRange(destIndex, srcIndex, len);
        }

        @Override
        void copyInterruptedSegment(Matrices.Region destRegion, long destMin, long destMax) {
            long destX = destMin;
            // find the 1st copied element before checking coordinates
            for (; destX <= destMax; destX++) {
                destCoordinates[0] = destX;
                if (destRegion.contains(destCoordinates)) {
                    break;
                }
            }
            if (destX > destMax) {
                return; // no integer points in the specified region
            }
            destCoordinates[0] = destX;
            long destIndex;
            try {
                destIndex = dest.index(destCoordinates);
            } catch (IndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException("Destination region, " + e.getMessage());
            }
            long srcX = shifts.length >= 1 ? destX - shifts[0] : destX;
            srcCoordinates[0] = srcX;
            long srcIndex = src.index(srcCoordinates);
            for (; destX <= destMax; destX++, srcX++, destIndex++, srcIndex++) {
                destCoordinates[0] = destX;
                if (destRegion.contains(destCoordinates)) {
                    if (destX >= destDimX) {
                        throw new IndexOutOfBoundsException("Destination region, index (" + destX
                            + (destX < 0 ? ") < 0" : ") >= dim(0) (" + destDimX + ")"));
                    }
                    if (srcX >= srcDimX) {
                        throw new IndexOutOfBoundsException("Source region, index (" + srcX
                            + (srcX < 0 ? ") < 0" : ") >= dim(0) (" + srcDimX + ")"));
                    }
                    copyElement(destIndex, srcIndex);
                    copiedElementsCount++;
                }
            }
        }
    }

    private class ContinuedSegmentCopier extends SegmentCopier {

        ContinuedSegmentCopier(Object workJArray) {
            super(workJArray);
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            destCoordinates[0] = 0;
            if (!dest.inside(destCoordinates)) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            destCoordinates[0] = destMin;
            long srcMin = shifts.length >= 1 ? destMin - shifts[0] : destMin;
            long destIndex = dest.index(destCoordinates);
            assert destMax >= 0 && destMax < destDimX : "destMin is " + destMin + ", but destMax is " + destMax;
            long len = destMax - destMin + 1;
            long srcMax = srcMin + destMax - destMin;
            copiedElementsCount += len;
            srcCoordinates[0] = 0;
            if (srcMax < 0 || srcMin >= srcDimX || !src.inside(srcCoordinates)) {
                // the line is outside the source matrix: filling from outsideConst
                fill(destIndex, len);
                return;
            }
            if (srcMin < 0) {
                fill(destIndex, -srcMin);
                destIndex -= srcMin; // increasing destIndex
                len += srcMin; // decreasing len
                srcMin = 0;
            }
            assert srcMin < srcDimX;
            srcCoordinates[0] = srcMin;
            long srcIndex = src.index(srcCoordinates);
            long rear = 0;
            if (srcMax >= srcDimX) {
                rear = srcMax - srcDimX + 1;
                len -= rear;
            }
            copyRange(destIndex, srcIndex, len);
            if (rear > 0) {
                fill(destIndex + len, rear);
            }
        }

        @Override
        void copyInterruptedSegment(Matrices.Region destRegion, long destMin, long destMax) {
            destCoordinates[0] = 0;
            if (!dest.inside(destCoordinates)) {
                return; // the line is outside the destination matrix: nothing to do
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                return; // nothing to do
            }
            destCoordinates[0] = destMin;
            long destIndex = dest.index(destCoordinates);
            assert destMax >= 0 && destMax < destDimX : "destMin is " + destMin + ", but destMax is " + destMax;
            long destX = destMin;
            long srcX = shifts.length >= 1 ? destX - shifts[0] : destX;
            srcCoordinates[0] = 0;
            if (srcX >= srcDimX || !src.inside(srcCoordinates)) {
                // the line is outside the source matrix: filling from outsideConst;
                // the check of srcX is necessary to be sure that srcCoordinates will be correct for "index" call
                for (; destX <= destMax; destX++, srcX++, destIndex++) {
                    destCoordinates[0] = destX;
                    if (destRegion.contains(destCoordinates)) {
                        copyElement(destIndex, OUTSIDE_SRC_INDEX);
                        copiedElementsCount++;
                    }
                }
                return;
            }
            for (; srcX < 0 && destX <= destMax; destX++, srcX++, destIndex++) {
                destCoordinates[0] = destX;
                if (destRegion.contains(destCoordinates)) {
                    copyElement(destIndex, OUTSIDE_SRC_INDEX);
                    copiedElementsCount++;
                }
            }
            if (destX > destMax) {
                return; // nothing to do more
            }
            assert srcX >= 0 && srcX < srcDimX;
            srcCoordinates[0] = srcX;
            long srcIndex = src.index(srcCoordinates);
            for (; destX <= destMax; destX++, srcX++, destIndex++, srcIndex++) {
                destCoordinates[0] = destX;
                if (destRegion.contains(destCoordinates)) {
                    copyElement(destIndex, srcX >= srcDimX ? OUTSIDE_SRC_INDEX : srcIndex);
                    copiedElementsCount++;
                }
            }
        }
    }

    private class UncheckedSegmentFiller2D extends ContinuedSegmentCopier {
        UncheckedSegmentFiller2D() {
            super(null);
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final long len = destMax - destMin + 1;
            fill(destCoordinates[1] * destDimX + destMin, len);
            copiedElementsCount += len;
        }
    }

    private class ContinuedSegmentFiller2D extends ContinuedSegmentCopier {
        ContinuedSegmentFiller2D() {
            super(null);
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final long len = destMax - destMin + 1;
            fill(destCoordinates[1] * destDimX + destMin, len);
        }
    }

    // Current version does not try to optimize bit arrays
    private class DirectBitArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        DirectBitArraysUncheckedSegmentFiller2D(long[] destArray, int offset, boolean filler) {
            super();
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            super.copyUninterruptedSegment(destMin, destMax);
        }
    }

    private class DirectBitArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {

        DirectBitArraysContinuedSegmentFiller2D(long[] destArray, int offset, boolean filler) {
            super();
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            super.copyUninterruptedSegment(destMin, destMax);
        }
    }

    /*Repeat() char ==> byte,,short,,int,,long,,float,,double;;
               Char ==> Byte,,Short,,Int,,Long,,Float,,Double
     */
    private class DirectCharArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final char[] destArray;
        private final int offset;
        private final char filler;

        DirectCharArraysUncheckedSegmentFiller2D(char[] destArray, int offset, char filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectCharArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final char[] destArray;
        private final int offset;
        private final char filler;

        DirectCharArraysContinuedSegmentFiller2D(char[] destArray, int offset, char filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private class DirectByteArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final byte[] destArray;
        private final int offset;
        private final byte filler;

        DirectByteArraysUncheckedSegmentFiller2D(byte[] destArray, int offset, byte filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectByteArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final byte[] destArray;
        private final int offset;
        private final byte filler;

        DirectByteArraysContinuedSegmentFiller2D(byte[] destArray, int offset, byte filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }


    private class DirectShortArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final short[] destArray;
        private final int offset;
        private final short filler;

        DirectShortArraysUncheckedSegmentFiller2D(short[] destArray, int offset, short filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectShortArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final short[] destArray;
        private final int offset;
        private final short filler;

        DirectShortArraysContinuedSegmentFiller2D(short[] destArray, int offset, short filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }


    private class DirectIntArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final int[] destArray;
        private final int offset;
        private final int filler;

        DirectIntArraysUncheckedSegmentFiller2D(int[] destArray, int offset, int filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectIntArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final int[] destArray;
        private final int offset;
        private final int filler;

        DirectIntArraysContinuedSegmentFiller2D(int[] destArray, int offset, int filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }


    private class DirectLongArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final long[] destArray;
        private final int offset;
        private final long filler;

        DirectLongArraysUncheckedSegmentFiller2D(long[] destArray, int offset, long filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectLongArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final long[] destArray;
        private final int offset;
        private final long filler;

        DirectLongArraysContinuedSegmentFiller2D(long[] destArray, int offset, long filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }


    private class DirectFloatArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final float[] destArray;
        private final int offset;
        private final float filler;

        DirectFloatArraysUncheckedSegmentFiller2D(float[] destArray, int offset, float filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectFloatArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final float[] destArray;
        private final int offset;
        private final float filler;

        DirectFloatArraysContinuedSegmentFiller2D(float[] destArray, int offset, float filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }


    private class DirectDoubleArraysUncheckedSegmentFiller2D extends UncheckedSegmentFiller2D {
        private final double[] destArray;
        private final int offset;
        private final double filler;

        DirectDoubleArraysUncheckedSegmentFiller2D(double[] destArray, int offset, double filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    private class DirectDoubleArraysContinuedSegmentFiller2D extends ContinuedSegmentFiller2D {
        private final double[] destArray;
        private final int offset;
        private final double filler;

        DirectDoubleArraysContinuedSegmentFiller2D(double[] destArray, int offset, double filler) {
            super();
            this.destArray = destArray;
            this.offset = offset;
            this.filler = filler;
        }

        @Override
        void copyUninterruptedSegment(long destMin, long destMax) {
            if (destCoordinates[1] < 0 || destCoordinates[1] >= destDimY) {
                // the line is outside the destination matrix: nothing to do
                return;
            }
            if (destMin < 0) {
                destMin = 0;
            }
            if (destMax > destDimX - 1) {
                destMax = destDimX - 1;
            }
            if (destMin > destMax) {
                // nothing to do
                return;
            }
            final int len = (int) destMax - (int) destMin + 1;
            final int position = (int) destCoordinates[1] * (int) destDimX + (int) destMin;
            for (int k = offset + position, kMax = k + len; k < kMax; k++) {
                destArray[k] = filler;
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() bit|boolean(?!\smustBeInside) ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit|Boolean                   ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
               (src(?:Byte|Short)Array\.get) ==> $1,,(byte) $1,,(short) $1,,$1,,...;;
               \(long(\[\]\)\s*destJArray)   ==> (char$1,,(byte$1,,(short$1,,(int$1,,(long$1,,(float$1,,(double$1;;
               (set|get)Object               ==> $1Element,,...
     */
    private static class BitArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableBitArray destBitArray;
        private final BitArray srcBitArray;
        private final boolean outsideValue;

        private BitArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, boolean outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destBitArray = (UpdatableBitArray) dest.array();
            this.srcBitArray = (BitArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destBitArray.setBit(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcBitArray.getBit(srcIndex));
        }

        void fill(long position, long count) {
            destBitArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectBitArraysUncheckedSegmentFiller2D(
                    (long[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectBitArraysContinuedSegmentFiller2D(
                    (long[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static class CharArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableCharArray destCharArray;
        private final CharArray srcCharArray;
        private final char outsideValue;

        private CharArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, char outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destCharArray = (UpdatableCharArray) dest.array();
            this.srcCharArray = (CharArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destCharArray.setChar(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcCharArray.getChar(srcIndex));
        }

        void fill(long position, long count) {
            destCharArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectCharArraysUncheckedSegmentFiller2D(
                    (char[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectCharArraysContinuedSegmentFiller2D(
                    (char[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }


    private static class ByteArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableByteArray destByteArray;
        private final ByteArray srcByteArray;
        private final byte outsideValue;

        private ByteArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, byte outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destByteArray = (UpdatableByteArray) dest.array();
            this.srcByteArray = (ByteArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destByteArray.setByte(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : (byte) srcByteArray.getByte(srcIndex));
        }

        void fill(long position, long count) {
            destByteArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectByteArraysUncheckedSegmentFiller2D(
                    (byte[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectByteArraysContinuedSegmentFiller2D(
                    (byte[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }


    private static class ShortArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableShortArray destShortArray;
        private final ShortArray srcShortArray;
        private final short outsideValue;

        private ShortArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, short outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destShortArray = (UpdatableShortArray) dest.array();
            this.srcShortArray = (ShortArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destShortArray.setShort(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : (short) srcShortArray.getShort(srcIndex));
        }

        void fill(long position, long count) {
            destShortArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectShortArraysUncheckedSegmentFiller2D(
                    (short[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectShortArraysContinuedSegmentFiller2D(
                    (short[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }


    private static class IntArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableIntArray destIntArray;
        private final IntArray srcIntArray;
        private final int outsideValue;

        private IntArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, int outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destIntArray = (UpdatableIntArray) dest.array();
            this.srcIntArray = (IntArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destIntArray.setInt(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcIntArray.getInt(srcIndex));
        }

        void fill(long position, long count) {
            destIntArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectIntArraysUncheckedSegmentFiller2D(
                    (int[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectIntArraysContinuedSegmentFiller2D(
                    (int[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }


    private static class LongArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableLongArray destLongArray;
        private final LongArray srcLongArray;
        private final long outsideValue;

        private LongArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, long outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destLongArray = (UpdatableLongArray) dest.array();
            this.srcLongArray = (LongArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destLongArray.setLong(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcLongArray.getLong(srcIndex));
        }

        void fill(long position, long count) {
            destLongArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectLongArraysUncheckedSegmentFiller2D(
                    (long[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectLongArraysContinuedSegmentFiller2D(
                    (long[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }


    private static class FloatArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableFloatArray destFloatArray;
        private final FloatArray srcFloatArray;
        private final float outsideValue;

        private FloatArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, float outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destFloatArray = (UpdatableFloatArray) dest.array();
            this.srcFloatArray = (FloatArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destFloatArray.setFloat(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcFloatArray.getFloat(srcIndex));
        }

        void fill(long position, long count) {
            destFloatArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectFloatArraysUncheckedSegmentFiller2D(
                    (float[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectFloatArraysContinuedSegmentFiller2D(
                    (float[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }


    private static class DoubleArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableDoubleArray destDoubleArray;
        private final DoubleArray srcDoubleArray;
        private final double outsideValue;

        private DoubleArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, double outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destDoubleArray = (UpdatableDoubleArray) dest.array();
            this.srcDoubleArray = (DoubleArray) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destDoubleArray.setDouble(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcDoubleArray.getDouble(srcIndex));
        }

        void fill(long position, long count) {
            destDoubleArray.fill(position, count, outsideValue);
        }

        @Override
        UncheckedSegmentFiller2D getUncheckedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectDoubleArraysUncheckedSegmentFiller2D(
                    (double[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getUncheckedSegmentFiller2D();
        }

        @Override
        ContinuedSegmentFiller2D getContinuedSegmentFiller2D() {
            if (destJArray != null) {
                return new DirectDoubleArraysContinuedSegmentFiller2D(
                    (double[]) destJArray, destJArrayOffset, outsideValue);
            }
            return super.getContinuedSegmentFiller2D();
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private static class ObjectArraysMatrixRegionCopier extends ArraysMatrixRegionCopier {
        private final UpdatableObjectArray<Object> destObjectArray;
        private final ObjectArray<?> srcObjectArray;
        private final Object outsideValue;

        private ObjectArraysMatrixRegionCopier(
            ArrayContext context, int maxNumberOfDimensions,
            Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
            long[] shifts, Object outsideValue, boolean mustBeInside)
        {
            super(context, maxNumberOfDimensions, dest, src, shifts, mustBeInside);
            this.destObjectArray = ((UpdatableObjectArray<?>) dest.array()).cast(Object.class);
            this.srcObjectArray = (ObjectArray<?>) src.array();
            this.outsideValue = outsideValue;
        }

        void copyElement(long destIndex, long srcIndex) {
            destObjectArray.setElement(destIndex,
                srcIndex == OUTSIDE_SRC_INDEX ? outsideValue : srcObjectArray.getElement(srcIndex));
        }

        void fill(long position, long count) {
            destObjectArray.fill(position, count, outsideValue);
        }
    }
}
