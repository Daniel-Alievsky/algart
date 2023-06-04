/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

abstract class ArraysSubMatrixCopier {
    private static final boolean ENABLE_THIS_OPTIMIZATION = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.arrays.enableSubMatrixCopierOptimization", true);
    private static final boolean ENABLE_CONTINUOUS_OPTIMIZATION_FOR_DEST = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.arrays.enableSubMatrixCopierContinuousDestOptimization", true);
    private static final boolean ENABLE_CONTINUOUS_OPTIMIZATION_FOR_SRC = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.arrays.enableSubMatrixCopierContinuousSrcOptimization", true);

    private static final boolean DEBUG_MODE = false;
    private static final boolean DEBUG_ALSO_ONE_DIMENSIONAL = false;

    private static final long MIN_ARRAY_LENGTH_FOR_COPY_SUB_MATRIX = DEBUG_MODE ? 1 : 1024;

    final Matrix<? extends UpdatableArray> dest;
    final Matrix<? extends Array> src;
    final Matrix.ContinuationMode destContinuationMode;
    final Matrix.ContinuationMode srcContinuationMode;
    final int n;
    final long[] destPosition;
    final long[] srcPosition;
    final long[] dimensions;
    final boolean isCopierToTiled;
    final Matrix<? extends Array> tileParent;
    final ArraysTileMatrixImpl.Indexer tilingIndexer;
    final long[] tileDimensions;
    final long[] tileStartIndexes;
    final long[] tileCounts;
    final Matrix<?> tileEnumerator; // - trivial virtual matrix to enumerate all tiles
    final boolean processingDeclined;
    boolean nothingToDo = false; // - correct only if !processingDeclined

    ArraysSubMatrixCopier(
        Matrix<? extends UpdatableArray> dest,
        Matrix<? extends Array> src,
        Matrix.ContinuationMode destContinuationMode,
        Matrix.ContinuationMode srcContinuationMode,
        long[] destPosition,
        long[] srcPosition,
        long[] dimensions,
        boolean isCopierToTiled)
    {
        assert src.dimCount() == dimensions.length;
        assert destPosition.length == dimensions.length;
        assert srcPosition.length == dimensions.length;
        assert dest.dimCount() == dimensions.length;
        assert destContinuationMode != null;
        assert srcContinuationMode != null;
        assert isCopierToTiled ? dest.isTiled() : src.isTiled();
        this.dest = dest;
        this.src = src;
        this.destContinuationMode = destContinuationMode;
        this.srcContinuationMode = srcContinuationMode;
        this.destPosition = destPosition;
        this.srcPosition = srcPosition;
        this.dimensions = dimensions;
        this.n = dimensions.length;
        this.isCopierToTiled = isCopierToTiled;
        this.tileParent = (isCopierToTiled ? dest : src).tileParent();
        // Overflows below are impossible: the position information is loaded either from subMatrix, or from Region
        boolean fullyInsideDest = true;
        for (int k = 0; k < n; k++) {
            assert this.dimensions[k] >= 0;
            if (this.destPosition[k] < 0) {
                this.srcPosition[k] -= this.destPosition[k];
                this.dimensions[k] += this.destPosition[k];
                this.destPosition[k] = 0;
                fullyInsideDest = false;
            }
            if (this.destPosition[k] > dest.dim(k) - this.dimensions[k]) {
                // must check this and correct dimensions also if there was this.destPosition[k] < 0!
                this.dimensions[k] = dest.dim(k) - this.destPosition[k];
                fullyInsideDest = false;
            }
            if (this.dimensions[k] <= 0) {
                this.dimensions[k] = 0; // to be on the safe side
                this.nothingToDo = true;
            }
        }
        // After this loop, there is also a guarantee that this.dimensions is a subregion of some correct Matrix
        assert fullyInsideDest || destContinuationMode != Matrix.ContinuationMode.NONE;
        // - if dest subMatrix must be inside the full matrix, it was already checked before this call
        this.processingDeclined = !fullyInsideDest && !destContinuationMode.isConstant();
        // - the loop above is not a correct solution for non-trivial continuation models
        if (this.nothingToDo || this.processingDeclined) {
            this.tilingIndexer = null;
            this.tileDimensions = null;
            this.tileStartIndexes = null;
            this.tileCounts = null;
            this.tileEnumerator = null;
        } else {
            Array a = (isCopierToTiled ? dest : src).array();
            this.tilingIndexer = a instanceof ArraysTileMatrixImpl.TileMatrixArray ?
                ((ArraysTileMatrixImpl.TileMatrixArray) a).indexer() :
                null; // - possible in non-standard implementations of Matrix interface
            this.tileDimensions = (isCopierToTiled ? dest : src).tileDimensions();
            this.tileStartIndexes = new long[n];
            this.tileCounts = new long[n];
            long[] positionInTiled = isCopierToTiled ? destPosition : srcPosition;
            for (int k = 0; k < n; k++) {
                assert this.tileDimensions[k] > 0 : "Illegal tiling (negative tile dimension)";
                assert dimensions[k] > 0; // because !processingDeclined
                assert dimensions[k] <= dest.dim(k); // because of the loop above
                long min = positionInTiled[k];
                long minIndex = min >= 0 ? min / this.tileDimensions[k] : (min + 1) / this.tileDimensions[k] - 1;
                long max = min + this.dimensions[k] - 1;
                long maxIndex = max >= 0 ? max / this.tileDimensions[k] : (max + 1) / this.tileDimensions[k] - 1;
                assert maxIndex <= maxIndex;
                this.tileStartIndexes[k] = minIndex;
                this.tileCounts[k] = maxIndex - minIndex + 1;
                assert this.tileCounts[k] <= dimensions[k]; // and, so, overflow in longMul below is impossible
            }
//            System.out.println(JArrays.toString(dimensions, ",", 100) + "; " + JArrays.toString(tileCounts,",",100)
//                + "; " + JArrays.toString(srcPosition, ",", 100) + "; " + JArrays.toString(destPosition,",",100)
//                + "; " + isCopierToTiled);
            this.tileEnumerator = Matrices.matrix(Arrays.nIntCopies(Arrays.longMul(tileCounts), 157), tileCounts);
        }
    }

    // for usage from AbstractArray.defaultCopy
    public static boolean copySubMatrixArray(
        ArrayContext context, // not used in the current version of this library
        UpdatableArray dest, Array src)
    {
        if (!ENABLE_THIS_OPTIMIZATION) {
            return false;
        }
        if (dest instanceof ArraysSubMatrixImpl.SubMatrixArray) {
            if (dest.length() < MIN_ARRAY_LENGTH_FOR_COPY_SUB_MATRIX || src.length() < dest.length()) {
                return false;
            }
            ArraysSubMatrixImpl.SubMatrixArray sma = (ArraysSubMatrixImpl.SubMatrixArray) dest;
            Matrix<? extends UpdatableArray> baseMatrix = sma.baseMatrix().cast(UpdatableArray.class);
            if (!baseMatrix.isTiled()) {
                return false;
            }
            if (!DEBUG_ALSO_ONE_DIMENSIONAL && baseMatrix.dimCount() == 1) {
                return false;
            }
            long[] dimensions = sma.dimensions();
            long[] destPosition = sma.from();
            long[] srcPosition = new long[dimensions.length]; // zero-filled
            return new ToTiled(
                baseMatrix, Matrices.matrixAtSubArray(src, 0, dimensions),
                sma.continuationMode(), Matrix.ContinuationMode.NONE,
                destPosition, srcPosition, dimensions).copySubMatrix(context);
        }
        if (src instanceof ArraysSubMatrixImpl.SubMatrixArray) {
            if (src.length() < MIN_ARRAY_LENGTH_FOR_COPY_SUB_MATRIX || dest.length() < src.length()) {
                return false;
            }
            ArraysSubMatrixImpl.SubMatrixArray sma = (ArraysSubMatrixImpl.SubMatrixArray) src;
            Matrix<? extends Array> baseMatrix = sma.baseMatrix();
            if (!baseMatrix.isTiled()) {
                return false;
            }
            if (!DEBUG_ALSO_ONE_DIMENSIONAL && baseMatrix.dimCount() == 1) {
                return false;
            }
            long[] dimensions = sma.dimensions();
            long[] srcPosition = sma.from();
            long[] destPosition = new long[dimensions.length]; // zero-filled
            return new FromTiled(
                Matrices.matrixAtSubArray(dest, 0, dimensions), baseMatrix,
                Matrix.ContinuationMode.NONE, sma.continuationMode(),
                destPosition, srcPosition, dimensions).copySubMatrix(context);
        }
        return false;
    }

    // for usage from copyRegion
    public static boolean copySubMatrixRegion(
        ArrayContext context,
        Matrix<? extends UpdatableArray> dest, Matrix<? extends Array> src,
        Matrix.ContinuationMode continuationMode,
        Matrices.Region destRegion, long... shifts)
    {
        if (!ENABLE_THIS_OPTIMIZATION) {
            return false;
        }
        if (!(destRegion instanceof Matrices.Hyperparallelepiped)) {
            return false;
        }
        final int n = destRegion.n();
        if (dest.dimCount() != n || src.dimCount() != n) {
            return false; // non-traditional use, not supported by copySubMatrix
        }
        if (!src.isTiled() && !dest.isTiled()) {
            return false;
        }
        if (!DEBUG_ALSO_ONE_DIMENSIONAL && n == 1) {
            return false;
        }
        long[] dimensions = new long[n];
        long[] destPosition = new long[n];
        long[] srcPosition = new long[n];
        for (int k = 0; k < n; k++) {
            IRange range = destRegion.coordRange(k);
            dimensions[k] = range.size();
            destPosition[k] = range.min();
            srcPosition[k] = shifts.length > k ? destPosition[k] - shifts[k] : destPosition[k];
        }
        if (dest.isTiled()) {
            return new ToTiled(dest, src, continuationMode, continuationMode,
                destPosition, srcPosition, dimensions).copySubMatrix(context);
        } else if (src.isTiled()) {
            return new FromTiled(dest, src, continuationMode, continuationMode,
                destPosition, srcPosition, dimensions).copySubMatrix(context);
        } else {
            throw new AssertionError("Mutable isTiled!");
        }
    }

    boolean copySubMatrix(ArrayContext context) {
        if (processingDeclined) { // must be checked before nothingToDo!
            return false;
        }
        if (nothingToDo) {
            return true;
        }
        final long tileCount = tileEnumerator.size();
        if (tileCount <= 1) {
            // no sense to optimize copying inside 1 tile: for example, copyRegion will probably work faster
            return false;
        }
        long[] positionInTiled = isCopierToTiled ? destPosition : srcPosition;
        long[] tileIndexes = new long[n];
        long[] tilePartPos = new long[n];
        long[] tilePartDim = new long[n];
        long[] work = new long[n];
        long readyCount = 0;
        long totalCount = Arrays.longMul(dimensions);
        for (long tileIndex = 0; tileIndex < tileCount; tileIndex++) {
            tileEnumerator.coordinates(tileIndex, tileIndexes);
            boolean continuousSubArray = true;
            // - this tile part corresponds to a continuous block in the parent array (which was tiled), if:
            // 1) it fully lies inside the matrix (in other case, subMatrix mechanism provides different schemes
            // for continuation depending on the continuation model: we do not try to optimize this case)
            // 2) it is the whole tile (100% of the matrix tile) or, maybe, a part along the high (last) coordinate,
            // but the full tile along all other coordinates; for example, for 2-dimensional matrix 8700x3400 and
            // tiles 1000x1000: (7000..7642)x(1000..2000) is not continuous, but (7000..7999)x(1100x1900) and
            // (8000..8699)x(1100x1900) are continuous
            long tileSize = 1;
            long dimMul = 1;
            long tilePosIndex = 0;
            for (int k = 0; k < n; k++) {
                long i = tileIndexes[k];
                long tileFrom = (i + tileStartIndexes[k]) * tileDimensions[k];
                long tileTo = tileFrom + tileDimensions[k];
                long partFrom = tileFrom;
                long partTo = tileTo;
                long dim = tileParent.dim(k);
                if (i == 0) {
                    // "(i + tileStartIndexes[k]) * tileDimensions[k]" can lead to negative overflow in this case
                    partFrom = positionInTiled[k];
                    if (k < n - 1 && partFrom != tileFrom) {
                        continuousSubArray = false;
                    }
                }
                if (i == tileCounts[k] - 1) {
                    // "tileFrom + tileDimensions[k]" can lead partTo positive overflow in this case
                    partTo = positionInTiled[k] + dimensions[k];
                    tileTo = tileTo < dim ? tileTo : dim; // stays unchanged in a case of overflow
                    if (k < n - 1 && partTo != tileTo) {
                        continuousSubArray = false;
                    }
                }
                if (partFrom > partTo)
                    throw new AssertionError("k=" + k + ", partFrom..partTo=" + partFrom + ".." + partTo + ", i=" + i
                    + ", tileStartIndexes[k]=" + tileStartIndexes[k]
                    + ", positionInTiled[k]=" + positionInTiled[k] + ", dimensions[k]=" + dimensions[k]
                    + ", tilePartDim[k]=" + tileDimensions[k]
                        + ", tilePartDim={" + JArrays.toString(tileDimensions, ",", 200) + "}");
                if (partFrom < 0 || partTo > dim) { // this tile part lies outside the tiled matrix
                    continuousSubArray = false;
                }
                tilePartPos[k] = partFrom;
                tilePartDim[k] = partTo - partFrom;
                tilePosIndex += partFrom * dimMul;
                dimMul *= dim;
                tileSize *= tilePartDim[k];
            }
            long translatedTilePosIndex = continuousSubArray ? tilingIndexer.translate(tilePosIndex) : -1;
            if (DEBUG_MODE && continuousSubArray) {
                assert tilePosIndex == tileParent.index(tilePartPos);
                assert tilePosIndex >= 0 && tilePosIndex + tileSize <= tileParent.size() :
                    tilePosIndex + "+" + tileSize + ", " + tileParent.size();
                assert translatedTilePosIndex >= 0 && translatedTilePosIndex + tileSize <= tileParent.size();
            }
            copyTileOrPartOfTile(tilePartPos, tilePartDim, work, translatedTilePosIndex, tileSize);
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(dest.elementType(), readyCount, totalCount);
            }
            readyCount += tileSize;
            assert readyCount <= totalCount;
        }
        return true;
    }


    abstract void copyTileOrPartOfTile(
        long[] partPos, long[] partDim, long[] workMemory,
        long partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase, long partSize);

    private static class ToTiled extends ArraysSubMatrixCopier {
        private ToTiled(
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            Matrix.ContinuationMode destContinuationMode,
            Matrix.ContinuationMode srcContinuationMode,
            long[] destPosition,
            long[] srcPosition,
            long[] dimensions)
        {
            super(dest, src, destContinuationMode, srcContinuationMode, destPosition, srcPosition, dimensions, true);
        }

        @Override
        void copyTileOrPartOfTile(
            long[] partPos, long[] partDim, long[] workMemoryForSrcPos,
            long partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase, long partSize)
        {
            for (int k = 0; k < partDim.length; k++) {
                workMemoryForSrcPos[k] = srcPosition[k] + partPos[k] - destPosition[k];
            }
//            System.out.print("Copying tile " + JArrays.toString(partDim, ",", 100) + " started at "
//                + JArrays.toString(partPos, ",", 100) + " to tiled, index "
//                + partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase + "... ");
            UpdatableArray destArray = ENABLE_CONTINUOUS_OPTIMIZATION_FOR_DEST
                && partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase != -1 ?
                (UpdatableArray) tileParent.array().subArr(
                    partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase, partSize) :
                dest.subMatr(partPos, partDim).array();
            Array srcArray = src.subMatr(workMemoryForSrcPos, partDim, srcContinuationMode).array();
            AbstractArray.defaultCopyWithoutOptimizations(destArray, srcArray);
//            System.out.println("done");
        }
    }

    private static class FromTiled extends ArraysSubMatrixCopier {
        private FromTiled(
            Matrix<? extends UpdatableArray> dest,
            Matrix<? extends Array> src,
            Matrix.ContinuationMode destContinuationMode,
            Matrix.ContinuationMode srcContinuationMode,
            long[] destPosition,
            long[] srcPosition,
            long[] dimensions)
        {
            super(dest, src, destContinuationMode, srcContinuationMode, destPosition, srcPosition, dimensions, false);
        }

        @Override
        void copyTileOrPartOfTile(
            long[] partPos, long[] partDim, long[] workMemoryForDestPos,
            long partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase, long partSize)
        {
            for (int k = 0; k < partDim.length; k++) {
                workMemoryForDestPos[k] = destPosition[k] + partPos[k] - srcPosition[k];
            }
//            System.out.print("Copying tile " + JArrays.toString(partDim, ",", 100) + " started at "
//                + JArrays.toString(partPos, ",", 100) + " from tiled, index "
//                + partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase + "... ");
            UpdatableArray destArray = dest.subMatr(workMemoryForDestPos, partDim).array();
            Array srcArray = ENABLE_CONTINUOUS_OPTIMIZATION_FOR_SRC
                && partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase != -1 ?
                tileParent.array().subArr(
                    partIndexInTheBaseIfItIsContinuousSubArrayOfTheBase, partSize) :
                src.subMatr(partPos, partDim, srcContinuationMode).array();
            AbstractArray.defaultCopyWithoutOptimizations(destArray, srcArray);
//            System.out.println("done");
        }
    }
}
