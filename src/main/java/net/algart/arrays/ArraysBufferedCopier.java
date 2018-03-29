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

import net.algart.math.functions.*;
import net.algart.math.IRange;

/**
 * <p>Buffering of {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class ArraysBufferedCopier {
    final ArrayContext context;
    final boolean compare;
    final UpdatableArray dest;
    final Array src;
    final int numberOfTasks;
    Arrays.CopyAlgorithm usedAlgorithm;

    private ArraysBufferedCopier(
        ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks,
        boolean compare)
    {
        if (dest == null)
            throw new NullPointerException("Null dest argument");
        AbstractArray.checkCopyArguments(dest, src);
        // - these checks should be performed here, before any attempts to optimize copying
        this.context = context;
        this.dest = dest;
        this.src = src;
        this.numberOfTasks = numberOfTasks;
        this.compare = compare;
    }

    public static ArraysBufferedCopier getInstance(
        ArrayContext context, UpdatableArray dest, Array src,
        int numberOfTasks, boolean strictMode, boolean compare)
    {
        if (Arrays.SystemSettings.BLOCK_OPTIMIZATION_FOR_COORDINATE_TRANSFORMATION && Arrays.isIndexFuncArray(src)) {
            return new IndexFuncCopier(context, dest, src, numberOfTasks, strictMode, compare);
        } else if (Arrays.isTiled(src) || Arrays.isTiled(dest)) {
            // must be checked AFTER isIndexFuncArray: coordinate transformation is often used with tiling
            return new TileCopier(context, dest, src, numberOfTasks, strictMode, compare);
        } else {
            return new ArraysBufferedCopier(context, dest, src, numberOfTasks, compare);
        }
    }

    public boolean process() {
        this.usedAlgorithm = Arrays.CopyAlgorithm.SIMPLE;
        return copy(context, dest, src, compare);
    }

    /**
     * Copies <tt>src</tt> to <tt>dest</tt> via {@link ArraysOpImpl.ComparingCopier} or {@link Arrays.Copier}.
     * The <tt>numberOfTasks</tt> argument of the constructors of that classes will be equal to the argument
     * of {@link #getInstance(ArrayContext, UpdatableArray, Array, int, boolean, boolean) getInstance} method.
     *
     * @param context the context of copying; may be <tt>null</tt>, then it will be ignored.
     * @param dest    the destination array.
     * @param src     the source array.
     * @param compare whether this method must use {@link ArraysOpImpl.ComparingCopier}.
     * @return the {@link ArraysOpImpl.ComparingCopier#changed} field
     *         if <tt>compare</tt> argument is <tt>true</tt>
     *         (i.e. <tt>true</tt> if and only if the <tt>dest</tt> array was changed),
     *         <tt>false</tt> <tt>compare</tt> argument is <tt>false</tt>.
     */
    final boolean copy(ArrayContext context, UpdatableArray dest, Array src, boolean compare) {
        return copy(context, dest, src, this.numberOfTasks, compare);
    }

    /**
     * Copies <tt>src</tt> to <tt>dest</tt> via {@link ArraysOpImpl.ComparingCopier} or {@link Arrays.Copier}.
     * The <tt>numberOfTasks</tt> argument of the constructors of that classes will be equal to the passed argument.
     *
     * @param context       the context of copying; may be <tt>null</tt>, then it will be ignored.
     * @param dest          the destination array.
     * @param src           the source array.
     * @param numberOfTasks the desired number of tasks; may be 0 for automatic detection.
     * @param compare       whether this method must use {@link ArraysOpImpl.ComparingCopier}.
     * @return the {@link ArraysOpImpl.ComparingCopier#changed} field
     *         if <tt>compare</tt> argument is <tt>true</tt>
     *         (i.e. <tt>true</tt> if and only if the <tt>dest</tt> array was changed),
     *         <tt>false</tt> <tt>compare</tt> argument is <tt>false</tt>.
     */
    final boolean copy(ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks, boolean compare) {
        if (compare) {
            ArraysOpImpl.ComparingCopier copier = new ArraysOpImpl.ComparingCopier(context, dest, src, numberOfTasks);
            copier.process();
            return copier.changed;
        } else {
            Arrays.Copier copier = new Arrays.Copier(context, dest, src, numberOfTasks, 0);
            copier.process();
            return false;
        }
    }

    static strictfp class IndexFuncCopier extends ArraysBufferedCopier {
        final boolean strictMode;
        private final long[] dim;
        private final long[] dimParent;
        private final int n; // == dim.length
        private final boolean truncationMode;
        private final boolean tiledMatrices;
        private final Matrix<? extends PArray> mSrc;
        private final Matrix<? extends PArray> mOutside;
        private final Matrix<? extends UpdatablePArray> mDest;
        private final Matrix<? extends PArray> mParent;
        private final ApertureFilteredFunc fFiltered;
        private final Func fInterpolation;
        private final CoordinateTransformationOperator o;
        private final ProjectiveOperator po; // null if o is not ProjectiveOperator, else (ProjectiveOperator)o
        private final double[] aFrom, aTo;
        private final double[] diagonal; // null if o is not LinearOperator, else po.diagonal()
        private final boolean projectiveTransformation;
        private final boolean affineTransformation;
        private final boolean resizingTransformation;
        private final Matrices.ResizingMethod resizingMethod;
        private final boolean optimizeResizingWholeMatrix;
        private final Matrices.InterpolationMethod interpolationMethod;
        private final IRange depRange;
        private final long bufSize;
        private final long parentTileSide;
        private final long parentTileSize; // == parentTileSide^n
        private final boolean bufferingPossible;
        private UpdatablePArray buf = null; // initialized (SimpleMemoryModel) in some branches of process() method

        private IndexFuncCopier(
            ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks,
            boolean strictMode, boolean compare)
        {
            super(context, dest, src, numberOfTasks, compare);
            assert Arrays.isIndexFuncArray(src);
            this.strictMode = strictMode;
            this.dim = Arrays.getIndexDimensions(src);
            this.n = dim.length;
            if (this.n < 1)
                throw new AssertionError("Invalid implementation of some classes: number of dimensions is " + this.n);
            if (Arrays.longMul(this.dim) < 0)
                throw new AssertionError("Invalid implementation of some classes: illegal product of dimensions");
            this.truncationMode = Arrays.getTruncationMode(src);
            Func f = Arrays.getFunc(src);
            final CoordinateTransformedFunc fTransformed;
            if (f instanceof CoordinateTransformedFunc) {
                fTransformed = (CoordinateTransformedFunc) f;
                this.fFiltered = null;
            } else if (f instanceof ApertureFilteredFunc) {
                this.fFiltered = (ApertureFilteredFunc) f;
                if (fFiltered.parent() instanceof CoordinateTransformedFunc) {
                    fTransformed = (CoordinateTransformedFunc) fFiltered.parent();
                } else {
                    fTransformed = null;
                }
            } else {
                this.fFiltered = null;
                fTransformed = null;
            }
            if (dest.length() != 0 // no reasons to optimize zero-length matrix
                && Arrays.longMul(dim) == dest.length() // we are copying whole matrix, not only a starting part
                && src.length() == dest.length() // to be on the safe side
                && fTransformed != null // the source matrix is a transformed parent function
                && Matrices.isInterpolationFunc(fTransformed.parent())) // the parent function is an interpolation
            {
                if (!(src instanceof PArray))
                    throw new AssertionError("isIndexFuncArray returns true for non-primitive array " + src);
                if (src.elementType() != dest.elementType())
                    throw new AssertionError("checkCopyArguments does not detect different element types");
                if (!(dest instanceof UpdatablePArray))
                    throw new AssertionError("AlgART array " + dest.elementType() + "[] does not implement "
                        + UpdatablePArray.class);
                this.mSrc = Matrices.matrix((PArray) src, dim);
                this.mDest = Matrices.matrix((UpdatablePArray) dest, dim);
                this.o = fTransformed.operator();
                this.po = this.o instanceof ProjectiveOperator ? (ProjectiveOperator) o : null;
                this.aFrom = new double[n]; // zero-filled
                this.aTo = new double[n]; // zero-filled
                if (this.fFiltered != null) {
                    ApertureFilterOperator afo = this.fFiltered.operator();
                    for (int k = 0; k < n; k++) {
                        this.aFrom[k] = afo.apertureFrom(k);
                        if (this.aFrom[k] < 0.0) {
                            this.aFrom[k] -= 1e-6;
                        }
                        this.aTo[k] = afo.apertureTo(k) + 1e-6;
                    }
                }
                this.diagonal = this.o instanceof LinearOperator ? this.po.diagonal() : null;
                boolean resizingTransformation = this.diagonal != null && po.isDiagonal() && po.isZeroB();
                if (resizingTransformation) {
                    for (double d : diagonal) {
                        resizingTransformation &= d > 0.0; // no inversion of coordinates
                    }
                }
                this.resizingTransformation = resizingTransformation;
                // resizing + shifting or coordinate inversion are rare cases and not optimized
                this.affineTransformation = this.o instanceof LinearOperator;
                this.projectiveTransformation = this.o instanceof ProjectiveOperator;
                this.fInterpolation = fTransformed.parent();
                this.interpolationMethod = Matrices.getInterpolationMethod(fInterpolation);
                this.depRange = this.interpolationMethod.dependenceCoordRange();
                if (Matrices.isOnlyInsideInterpolationFunc(fInterpolation)) {
                    this.mOutside = null;
                } else {
                    this.mOutside = coordFuncMatrix(
                        ConstantFunc.getInstance(Matrices.getOutsideValue(fInterpolation)), dim);
                }
                this.mParent = Matrices.getUnderlyingMatrix(this.fInterpolation);
                this.tiledMatrices = Arrays.isTiledOrSubMatrixOfTiled(dest)
                    || Arrays.isTiledOrSubMatrixOfTiled(mParent.array());
                this.dimParent = this.mParent.dimensions();
                if (dimParent.length != n)
                    throw new AssertionError("Invalid implementation of some classes: space dimension was changed");
                if (this.o instanceof LinearOperator) {
                    if (this.diagonal == null)
                        throw new AssertionError("Invalid implementation of some classes: "
                            + " null ProjectiveOperator.diagonal()");
                    if (this.diagonal.length != n || this.po.n() != n)
                        throw new AssertionError("Invalid implementation of some classes: illegal space dimension "
                            + "for the operator " + po);
                }
                Object helperInfo = resizingTransformation && src instanceof ArraysFuncImpl.OptimizationHelperInfo ?
                    ((ArraysFuncImpl.OptimizationHelperInfo) src).getOptimizationHelperInfo() :
                    null;
                this.resizingMethod = helperInfo instanceof Matrices.ResizingMethod ?
                    (Matrices.ResizingMethod) helperInfo :
                    null;
                this.optimizeResizingWholeMatrix = this.resizingMethod != null && !this.strictMode && !this.compare
                    && mParent.elementType() == mDest.elementType()
                    // - to be on the safe side: current implementations never provide resizingMethod
                    // for different source and resulting element types
                    && Arrays.SystemSettings.BLOCK_OPTIMIZATION_FOR_RESIZING_WHOLE_MATRIX;
                double elementsPerByte = 8.0 / this.mParent.array().bitsPerElement();
                double elementsPerTempJavaMemory = elementsPerByte * Arrays.SystemSettings.maxTempJavaMemory();
//                if (mParent.isTiled()) {
//                    long minTileSide = Long.MAX_VALUE;
//                    for (long tileDim : mParent.tileDimensions()) {
//                        minTileSide = Math.min(minTileSide, tileDim);
//                    }
//                    elementsPerTempJavaMemory = Math.min(elementsPerTempJavaMemory, Math.pow(minTileSide, n));
//                    // the good idea (?) is not to create tiles larger than the tiles in the parent matrix
//                    // seems to be anti-optimization
//                }
                this.bufSize = Math.max(
                    Math.round(elementsPerByte * Arrays.SystemSettings.MIN_OPTIMIZATION_JAVA_MEMORY),
                    Math.round(elementsPerTempJavaMemory));
                // - in an exotic case of long overflow, Math.round will provide .MAX_VALUE for us
                long estimatedSize = Math.min(bufSize, mParent.size());
                this.parentTileSide = estimateTileSide(dimParent, estimatedSize);
                this.parentTileSize = tileSize(dimParent, parentTileSide);
                this.bufferingPossible = true;
            } else {
                this.tiledMatrices = false;
                this.mSrc = null;
                this.mDest = null;
                this.o = null;
                this.po = null;
                this.aFrom = this.aTo = null;
                this.diagonal = null;
                this.projectiveTransformation = false;
                this.affineTransformation = false;
                this.resizingTransformation = false;
                this.resizingMethod = null;
                this.optimizeResizingWholeMatrix = false;
                this.fInterpolation = null;
                this.interpolationMethod = null;
                this.depRange = null;
                this.mOutside = null;
                this.mParent = null;
                this.dimParent = null;
                this.bufSize = -1;
                this.parentTileSize = -1;
                this.parentTileSide = -1;
                this.bufferingPossible = false;
            }
        }

        @Override
        public boolean process() {
            if (!bufferingPossible) {
                return super.process();
            }
            if (SimpleMemoryModel.isSimpleArray(mParent.array())) {
                if (optimizeResizingWholeMatrix) {
                    if (ArraysMatrixResizer.tryToResizeWithOptimization(context, resizingMethod, mDest, mParent)) {
                        this.usedAlgorithm = Arrays.CopyAlgorithm.BUFFERING_WHOLE_ARRAY;
                        return false; // "false" means "not changed" - not important in a case "!compare"
                    }
                }
                if (SimpleMemoryModel.isSimpleArray(mDest.array())) {
                    return super.process();
                } // in other case, continue checks: maybe mDest is very large (strong stretching)
            }
            // the geometrical transformation requires random access to any elements,
            // that can be very slow for all cases excepting SimpleMemoryModel;
            // so, we shall try to copy the array via SimpleMemoryModel buffer
            if (isVeryQuickCompression()) {
                return super.process();
                // Important! Even if only one of two matrices is tiled, we should prefer tiling algorithm,
                // because line-per-line algorithm can lead to extreme slowing down in some form of LargeMemoryModel
                // due to very often bank swapping
            }
            Boolean result;
            if (!tiledMatrices) { // for tiled matrices, tiling algorithms below are the best
                result = copyWithBufferingWholeMatrix();
                if (result != null) {
                    this.usedAlgorithm = Arrays.CopyAlgorithm.BUFFERING_WHOLE_ARRAY;
                    return result;
                }
            }
            if (strictMode) {
                this.usedAlgorithm = Arrays.CopyAlgorithm.SIMPLE;
                return super.process();

            }
            if (Arrays.SystemSettings.BLOCK_OPTIMIZATION_FOR_RESIZING_TRANSFORMATION
                && !tiledMatrices // for tiled matrices, copyWithRegularTiling algorithms below are the best
                && resizingTransformation)
            {
                result = copyWithBufferingLayers();
                if (result != null) {
                    this.usedAlgorithm = Arrays.CopyAlgorithm.BUFFERING_LAYERS;
                    return result;
                }
            }
            if (Arrays.SystemSettings.BLOCK_OPTIMIZATION_FOR_AFFINE_TRANSFORMATION
                && affineTransformation)
            {
                result = copyWithRegularTiling();
                if (result != null) {
                    this.usedAlgorithm = Arrays.CopyAlgorithm.REGULAR_BUFFERING_TILES;
                    return result;
                } else {
                    return super.process();
                }
            }
            if (Arrays.SystemSettings.BLOCK_OPTIMIZATION_FOR_PROJECTIVE_TRANSFORMATION
                && projectiveTransformation)
            {
                result = copyWithRecursiveTiling();
                if (result != null) {
                    this.usedAlgorithm = Arrays.CopyAlgorithm.RECURSIVE_BUFFERING_TILES;
                    return result;
                }
            }
            return super.process();
        }

        /**
         * Returns <tt>true</tt> if we have compression in many times that can be performed quickly
         * without any optimization.
         *
         * @return <tt>true</tt> if we have compression in many times that can be performed quickly
         *         without any optimization.
         */
        private boolean isVeryQuickCompression() {
            if (!resizingTransformation) {
                return false;
            }
            if (fFiltered != null) {
                // probable the filter is averaging that requires accessing all source matrix elements
                // even for strong matrix compression
                return false;
            }
            double totalAverage = 1.0;
            boolean usualWayIsBetter = true;
            for (double d : diagonal) {
                usualWayIsBetter &= d > 1.0; // if there is compression by all coordinates
                totalAverage *= d;
            }
            usualWayIsBetter &=
                totalAverage > Arrays.SystemSettings.MIN_NON_OPTIMIZED_SIMPLE_COORDINATE_COMPRESSION;
            // ...and if total compression is strong enough: in this case the copying
            // into Java memory can spend more time than optimization benefit
            return usualWayIsBetter;
        }

        /**
         * Copies the matrix with preloading whole parent matrix into RAM
         * and processing it in RAM by {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method.
         *
         * @return the result of {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method,
         *         or <tt>null</tt> if copying by this algorithm is impossible or undesirable.
         */
        private Boolean copyWithBufferingWholeMatrix() {
            if (projectiveTransformation // in other case, we cannot estimate the parent tile size
                && !optimizeResizingWholeMatrix) // if optimizeResizingWholeMatrix, whole parent matrix is always used
            {
                final double minimalUsedPart = !strictMode ?
                    Arrays.SystemSettings.MIN_USED_PART_FOR_PRELOADING_WHOLE_MATRIX_WITH_BLOCK_OPTIMIZATION :
                    Arrays.SystemSettings.MIN_USED_PART_FOR_PRELOADING_WHOLE_MATRIX_WITHOUT_BLOCK_OPTIMIZATION;
                // maybe, a common algorithm will be better if we need only a little part of the parent matrix
                long[] parentFrom = new long[n];
                long[] parentTo = new long[n];
                srcSquareTileToParentCoordSystem(parentFrom, parentTo, new long[n], dim);
                if (checkFullyOutside(parentFrom, parentTo)) {
//                    System.out.println("Outside processing: whole --> "
//                        + toS(parentFrom) + ".." + toS(parentTo) + " by " + po);
                    return copy(context, dest, mOutside == null ? src : mOutside.array(), compare);
                }
                checkFullyInsideAndTrim(parentFrom, parentTo);
                long[] dimParentTile = new long[n];
                for (int k = 0; k < n; k++) {
                    assert parentFrom[k] >= 0 && parentTo[k] <= dimParent[k] && parentFrom[k] <= parentTo[k];
                    dimParentTile[k] = parentTo[k] - parentFrom[k];
                }
                final long parentTileSize = Arrays.longMul(dimParentTile);
                assert parentTileSize >= 0;
                if (parentTileSize < minimalUsedPart * mParent.size()) {
                    return null;
                }
            }
            if (mParent.size() > bufSize) {
                return null;
            }
            Matrix<? extends UpdatablePArray> buf = Arrays.SMM.newMatrix(
                UpdatablePArray.class, mParent.elementType(), dimParent);
            copy(context == null ? null : context.part(0.0, 0.1), buf.array(), mParent.array(), false);
            if (optimizeResizingWholeMatrix) {
                if (ArraysMatrixResizer.tryToResizeWithOptimization(
                    context == null ? null : context.part(0.1, 1.0),
                    resizingMethod, mDest, buf))
                {
                    return false; // "false" means "not changed" - not important in a case "!compare"
                }
            }
            Func bufInterpolation = interpolation(buf);
            Func bufTransformed = o.apply(bufInterpolation);
            if (fFiltered != null) {
                bufTransformed = fFiltered.operator().apply(bufTransformed);
            }
            Matrix<? extends PArray> lazy = coordFuncMatrix(bufTransformed, dim);
            return copy(context == null ? null : context.part(0.1, 1.0), mDest.array(), lazy.array(), compare);
        }

        /**
         * Copies the matrix by splitting the parent matrix by the last coordinates into "layers"
         * (submatrices, where all dimensions besides the last one are the same as in the whole parent matrix),
         * preloading every layer into RAM
         * and processing it in RAM by {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method.
         * Can be used only for affine transformations,
         * but should be used only for resizing.
         *
         * @return the logical OR of results of all calls
         *         of the {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method,
         *         or <tt>null</tt> if copying by this algorithm is impossible.
         */
        private Boolean copyWithBufferingLayers() {
            assert po instanceof LinearOperator;
            assert resizingTransformation;
            long parentPlaneSize = 1; // a line, or plane, or hyperplane
            for (int k = 0; k < n - 1; k++) {
                parentPlaneSize *= dimParent[k];
            }
            long srcPlaneSize = 1; // a line, or plane, or hyperplane
            for (int k = 0; k < n - 1; k++) {
                srcPlaneSize *= dim[k];
            }
            final long parentPlaneCount = bufSize / parentPlaneSize;
            if (parentPlaneCount <= 0) {
                return null; // too large plane: cannot be loaded in Java memory
            }
            final long srcPlaneCount = Math.min(
                parentLayerToSrcCoordSystem(parentPlaneCount),
                dim[n - 1]);

//            System.out.println(bufSize + ": " + parentPlaneCount + " -> " + srcPlaneCount);
            assert srcPlaneCount >= 0;
            if (srcPlaneCount == 0) {
                return null;
            }
            long[] parentFrom = new long[n];
            long[] parentTo = new long[n];
            srcSquareTileToParentCoordSystem(parentFrom, parentTo, new long[n], dim);
            boolean layerFullyInside = true;
            for (int k = 0; k < n; k++) {
                if (parentFrom[k] < 0) {
                    parentFrom[k] = 0;
                    if (k < n - 1) {
                        layerFullyInside = false;
                    }
                }
                if (parentTo[k] > dimParent[k]) {
                    parentTo[k] = dimParent[k];
                    if (k < n - 1) {
                        layerFullyInside = false;
                    }
                }
            }
            long usedParentPlaneSize = 1;
            for (int k = 0; k < n - 1; k++) {
                usedParentPlaneSize *= parentTo[k] - parentFrom[k];
            }
            if (usedParentPlaneSize <=
                Arrays.SystemSettings.MIN_LAYER_USED_PART_FOR_LAYER_OPTIMIZATION * parentPlaneSize)
            {
                return null;
            }

            long[] dimLayer = dimParent.clone();
            dimLayer[n - 1] = parentPlaneCount;
            this.buf = (UpdatablePArray) Arrays.SMM.newUnresizableArray(
                mParent.elementType(), Arrays.longMul(dimLayer));
            long[] dimLazy = dim.clone();
            final long m = dim[n - 1];
            boolean result = false;
            double[] correctionShift = new double[n]; // zero-filled
            long srcTo;
            for (long srcFrom = 0; srcFrom < m; srcFrom = srcTo) {
                srcTo = srcFrom + Math.min(srcPlaneCount, m - srcFrom); // exclusive
                ArrayContext ac = context == null ? null : context.part(srcFrom, srcTo, m);
                final double transformedFrom = (srcFrom + aFrom[n - 1]) * diagonal[n - 1];
                final double transformedTo = (srcTo - 1 + aTo[n - 1]) * diagonal[n - 1]; // inclusive!
                long parFrom = (long) (Math.floor(transformedFrom) + depRange.min()); // "floor" necessary here!
                long parTo = (long) (Math.ceil(transformedTo) + depRange.max() + 1.0); // exclusive
                // casting to long after all operations to avoid long overflow
                boolean fullyOutside = parTo <= 0 || parFrom >= dimParent[n - 1];
//                System.out.println(srcFrom + ".." + srcTo + " --> " + transformedFrom + ".." + transformedTo
//                    + " --> " + parFrom + ".." + parTo + (fullyOutside ? "; outside" : ""));
                if (fullyOutside) {
                    // a layer is fully outside the matrix: no reasons to optimize (interpolation will work fast)
                    long srcFromIndex = srcFrom * srcPlaneSize;
                    long srcToIndex = srcTo * srcPlaneSize;
                    result |= copy(context == null ? null : context.part(srcFrom, srcTo, m),
                        mDest.array().subArray(srcFromIndex, srcToIndex),
                        (mOutside == null ? mSrc : mOutside).array().subArray(srcFromIndex, srcToIndex),
                        compare);
                    continue;
                }
                boolean fullyInside = parFrom >= 0 && parTo < dimParent[n - 1] && layerFullyInside;
                // optimization for a rare case: usually layerFullyInside=false even for strict matrix resizing,
                // due to little gaps at the matrix bounds
                parFrom = Math.max(parFrom, 0);
                parTo = Math.min(parTo, dimParent[n - 1]);
                if (parTo - parFrom > parentPlaneCount)
                    throw new AssertionError("Error while estimation in parentLayerToSrcDim: "
                        + srcFrom + ".." + srcTo + " <-- " + parFrom + ".." + parTo);
                dimLayer[n - 1] = parTo - parFrom;
                dimLazy[n - 1] = srcTo - srcFrom;
                Matrix<? extends UpdatablePArray> layer = Matrices.matrix(
                    buf.subArr(0, Arrays.longMul(dimLayer)), dimLayer);
                copy(ac == null ? null : ac.part(0.0, 0.1),
                    layer.array(),
                    mParent.array().subArray(parFrom * parentPlaneSize, parTo * parentPlaneSize),
                    false);
                Func layerInterpolation = fullyInside ?
                    Matrices.asInterpolationFunc(layer, interpolationMethod, false) :
                    interpolation(layer);
                correctionShift[n - 1] = srcFrom * diagonal[n - 1] - parFrom;
                LinearOperator correctedOperator = LinearOperator.getDiagonalInstance(diagonal, correctionShift);
                // New operator is a composition of 3 operators:
                // 1) shifting by +srcFrom;
                // 2) original resizing ("diag");
                // 3) shifting by -parentFrom
                Func layerTransformed = correctedOperator.apply(layerInterpolation);
                if (fFiltered != null) {
                    layerTransformed = fFiltered.operator().apply(layerTransformed);
                }
                Matrix<? extends PArray> lazy = coordFuncMatrix(layerTransformed, dimLazy);
                result |= copy(ac == null ? null : ac.part(0.1, 1.0),
                    mDest.array().subArray(srcFrom * srcPlaneSize, srcTo * srcPlaneSize),
                    lazy.array(),
                    compare);
            }
            return result;
        }

        /**
         * Copies the matrix by splitting the destination matrix into square (cubic, hypercubic) tiles,
         * preloading the area in the parent matrix, corresponding to every tile, into RAM
         * and processing it in RAM by {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method.
         * Can be used only for affine transformations:
         * they transform equal tiles to equal figures (parallelograms, parallelepipeds,&nbsp;...),
         * unlike, for example, projective transformations.
         *
         * @return the logical OR of results of all calls
         *         of the {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method,
         *         or <tt>null</tt> if copying by this algorithm is impossible.
         */
        private Boolean copyWithRegularTiling() {
            assert po instanceof LinearOperator;
            assert affineTransformation;
            if (parentTileSide <= 1) {
                return null;
            }
            if (n > Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS) { // too complex case for "bits" loops
                return null;
            }
            long srcSide = parentRegularSquareTileToSrcCoordSystem(parentTileSide);
            if (tiledMatrices) {
//                System.out.println(srcSide + " decreased to " + Long.highestOneBit(srcSide));
                srcSide = Long.highestOneBit(srcSide);
                // 2^k values decrease probability of intersecting tile boundaries in a tiled matrix
            }
            assert srcSide >= 0;
            if (srcSide == 0) {
                return null;
            }

            final long[] tileCounts = new long[n];
            for (int k = 0; k < n; k++) {
                if (dim[k] == 0) {
                    return null; // to be on the safe side: was be checked before this
                }
                tileCounts[k] = (dim[k] - 1) / srcSide + 1;
            }
            long[] scales = !optimizeResizingWholeMatrix ? null :
                ArraysMatrixResizer.getScalesIfCanBeOptimizedForDirectAccessibleResult(
                    resizingMethod, mParent.elementType(), dim, dimParent);
            boolean useMatrixResizer = scales != null;
            final Matrix<?> enumerator = Matrices.matrix(
                Arrays.nIntCopies(Arrays.longMul(tileCounts), 157), tileCounts);
            // - This trivial virtual matrix is a simplest way to enumerate all tiles.
            // Overflow is impossible in longMul here: parentSide > 0, so tileCounts[k] <= dimParent[k]
            long actualBufSize = parentTileSize;
            if (useMatrixResizer) {
                // necessary to clarify the size: first, we don't need so much memory in a case non-hypercube,
                // second, parentRegularSquareTileToSrcCoordSystem can be not so accurate for this algorithm
                assert mDest.elementType() == mParent.elementType();
                // - was already checked in the constructor while initialization of this.optimizeResizingWholeMatrix
                actualBufSize = 1;
                for (int k = 0; k < n; k++) {
                    long side = scales[k] * Math.min(srcSide, dim[k]);
                    assert side <= dimParent[k];
                    actualBufSize *= side;
                }
            }
            this.buf = (UpdatablePArray) Arrays.SMM.newUnresizableArray(mParent.elementType(), actualBufSize);
            long[] tileIndexes = new long[n];
            long[] srcFrom = new long[n];
            long[] srcTo = new long[n];
            long[] parentFrom = new long[n];
            long[] parentTo = new long[n];
            boolean result = false;
            for (long tileIndex = 0, tileCount = enumerator.size(); tileIndex < tileCount; tileIndex++) {
                ArrayContext ac = context == null ? null : context.part(tileIndex, tileIndex + 1, tileCount);
                enumerator.coordinates(tileIndex, tileIndexes);
                for (int k = 0; k < n; k++) {
                    srcFrom[k] = tileIndexes[k] * srcSide;
                    assert srcFrom[k] < dim[k];
                    srcTo[k] = Math.min(srcFrom[k] + srcSide, dim[k]); // exclusive
                }
                if (useMatrixResizer) {
                    long[] parentTileDim = new long[n];
                    for (int k = 0; k < n; k++) {
                        parentFrom[k] = srcFrom[k] * scales[k];
                        parentTo[k] = srcTo[k] * scales[k];
                        assert parentFrom[k] <= dimParent[k] && parentTo[k] <= dimParent[k];
                        parentTileDim[k] = parentTo[k] - parentFrom[k];
                    }
                    Matrix<? extends UpdatablePArray> parentTileBuf = Matrices.matrixAtSubArray(buf, 0, parentTileDim);
                    copy(ac == null ? null : ac.part(0.0, 0.3),
                        parentTileBuf.array(),
                        mParent.subMatrix(parentFrom, parentTo).array(),
                        false);
                    if (ArraysMatrixResizer.tryToResizeWithOptimization(ac == null ? null : ac.part(0.3, 1.0),
                        resizingMethod, mDest.subMatrix(srcFrom, srcTo), parentTileBuf))
                    {
//                        System.out.println("OPTIMIZED TILE " + JArrays.toString(parentFrom, ",", 100) + " "
//                            + JArrays.toString(parentTileDim, "x", 100));
                        continue;
                    } else {
                        // very improbable: getScalesIfCanBeOptimizedForDirectlyAccessibleResult doesn't keep its word
                        useMatrixResizer = false; // not try to optimize other tiles
                        ac = ac == null ? null : ac.part(0.3, 1.0);
                        this.buf = (UpdatablePArray) Arrays.SMM.newUnresizableArray(
                            mParent.elementType(), parentTileSize); // reallocation: parentTileSize is usually greater
                    }
                }
                // srcFrom..srcTo is a tile in the source and destination matrices
                srcSquareTileToParentCoordSystem(parentFrom, parentTo, srcFrom, srcTo);
                if (checkFullyOutside(parentFrom, parentTo)) {
                    // a tile is fully outside the matrix: no reasons to optimize (interpolation will work fast)
                    result |= copy(ac,
                        mDest.subMatrix(srcFrom, srcTo).array(),
                        (mOutside == null ? mSrc : mOutside).subMatrix(srcFrom, srcTo).array(),
                        compare);
                } else {
                    boolean fullyInside = checkFullyInsideAndTrim(parentFrom, parentTo);
                    // parentTo and parentFrom are trimmed here
                    result |= copyTile(ac, srcFrom, srcTo, parentFrom, parentTo, fullyInside);
                }
            }
            return result;
        }

        /**
         * Copies the matrix by splitting the destination matrix into 2 rectangular tiles by some coordinate,
         * attempt to preload the area in the parent matrix, corresponding to every tile, into RAM,
         * processing it in RAM by {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method
         * if there is enough memory or, if not, recursive further splitting this tile.
         * Can be used only for projective transformations.
         * (We have no algorithms for detecting corresponding area in the parent matrix
         * for more complex transformations. For projective ones it is enough to check all vertices,
         * because every hyperplane is transformed to hyperplane.)
         *
         * @return the logical OR of results of all calls
         *         of the {@link #copy(ArrayContext, UpdatableArray, Array, boolean)} method,
         *         or <tt>null</tt> if copying by this algorithm is impossible.
         */
        private Boolean copyWithRecursiveTiling() {
            assert po instanceof ProjectiveOperator;
            assert projectiveTransformation;
            if (parentTileSide <= 0) {
                return null;
            }
            if (n > Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS) { // too complex case for "bits" loops
                return null;
            }
            this.buf = (UpdatablePArray) Arrays.SMM.newUnresizableArray(mParent.elementType(), parentTileSize);
            return copyWithRecursiveTiling(context, new long[n], dim);
        }

        private boolean copyWithRecursiveTiling(ArrayContext ac, long[] srcFrom, long[] srcTo) {
            // srcFrom..srcTo is a tile in the source and destination matrices
            long[] parentFrom = new long[n];
            long[] parentTo = new long[n];
            srcSquareTileToParentCoordSystem(parentFrom, parentTo, srcFrom, srcTo);
            if (checkFullyOutside(parentFrom, parentTo)) { // should be checked before an attempt to split
//                System.out.println("Outside processing: " + toS(srcFrom) + ".." + toS(srcTo) + " --> "
//                    + toS(parentFrom) + ".." + toS(parentTo) + " by " + po);
                return copy(ac,
                    mDest.subMatrix(srcFrom, srcTo).array(),
                    (mOutside == null ? mSrc : mOutside).subMatrix(srcFrom, srcTo).array(),
                    compare);
            }
            boolean fullyInside = checkFullyInsideAndTrim(parentFrom, parentTo);
            long maxParentSide = 0;
            for (int k = 0; k < n; k++) {
                assert parentTo[k] >= parentFrom[k];
                long side = parentTo[k] - parentFrom[k];
                assert side >= 0 : "parentFrom/To are probably not trimmed";
                maxParentSide = Math.max(maxParentSide, side);
            }
            if (maxParentSide <= parentTileSide) { // stop recursion: there is enough RAM
//                System.out.println("Processing: " + toS(srcFrom) + ".." + toS(srcTo) + " --> "
//                    + toS(parentFrom) + ".." + toS(parentTo) + " by " + po);
                return copyTile(ac, srcFrom, srcTo, parentFrom, parentTo, fullyInside);
            }
            // too large tile: split it into 2 subtiles
            long srcTileSize = 1;
            for (int k = 0; k < n; k++) {
                assert srcTo[k] > srcFrom[k];
                srcTileSize *= srcTo[k] - srcFrom[k];
            }
            if (srcTileSize <= Arrays.SystemSettings.MIN_OPTIMIZATION_RESULT_TILE_VOLUME) { // stop recursion
//                System.out.println("Simple processing: " + toS(srcFrom) + ".." + toS(srcTo) + " --> "
//                    + toS(parentFrom) + ".." + toS(parentTo) + " by " + po);
                return copy(ac,
                    mDest.subMatrix(srcFrom, srcTo).array(),
                    mSrc.subMatrix(srcFrom, srcTo).array(),
                    compare);
            }
            if (srcTileSize <= 1) // necessary to provide conditions maxSide > 1 and maxSide/2 >= 1
                throw new AssertionError("The constant MIN_OPTIMIZATION_RESULT_TILE_VOLUME must be >=1");
//            System.out.println("Splitting: " + toS(srcFrom) + ".." + toS(srcTo) + " --> "
//                + toS(parentFrom) + ".." + toS(parentTo) + " by " + po);
            int splittingCoord = n - 1;
            long maxSide = srcTo[n - 1] - srcFrom[n - 1];
            for (int k = n - 2; k >= 0; k--) { // reverse order: it is better to split last coordinates firstly
                if (srcTo[k] - srcFrom[k] > maxSide) {
                    maxSide = srcTo[k] - srcFrom[k];
                    splittingCoord = k;
                }
            }
            assert maxSide > 1;
            long[] srcMiddle = new long[n];
            boolean result;
            System.arraycopy(srcTo, 0, srcMiddle, 0, n);
            srcMiddle[splittingCoord] = srcFrom[splittingCoord] + maxSide / 2; // use maxSide to avoid overflow
            result = copyWithRecursiveTiling(ac == null ? null : ac.part(0.0, 0.5), srcFrom, srcMiddle);
            System.arraycopy(srcFrom, 0, srcMiddle, 0, n);
            srcMiddle[splittingCoord] = srcFrom[splittingCoord] + maxSide / 2; // use maxSide to avoid overflow
            result |= copyWithRecursiveTiling(ac == null ? null : ac.part(0.5, 1.0), srcMiddle, srcTo);
            // Warning: not replace this with || operator! Single | is possible, but not so obvious.
            return result;
        }

        private boolean copyTile(
            ArrayContext ac,
            long[] srcFrom, long[] srcTo, long[] parentFrom, long[] parentTo,
            boolean fullyInside)
        {
            assert po != null;
            for (int k = 0; k < n; k++) {
                long side = parentTo[k] - parentFrom[k];
                if (side < 0 || side > parentTileSide) // <0 means overflow
                    throw new AssertionError("Error while estimation of the parent tile side: "
                        + toS(parentFrom) + ".." + toS(parentTo));
            }
            long[] parentTileDim = new long[n];
            long[] lazyDim = new long[n];
            for (int k = 0; k < n; k++) {
                assert parentFrom[k] >= 0 && parentTo[k] <= dimParent[k] && parentFrom[k] <= parentTo[k];
                parentTileDim[k] = parentTo[k] - parentFrom[k];
                lazyDim[k] = srcTo[k] - srcFrom[k];
            }
            long trimmedParentTileSize = Arrays.longMul(parentTileDim);
            if (trimmedParentTileSize > buf.length())
                throw new AssertionError("Error while estimation of necessary buffer size: "
                    + buf.length() + " instead of necessary " + trimmedParentTileSize);
            Matrix<? extends UpdatablePArray> parentTileBuf = Matrices.matrix(
                buf.subArr(0, trimmedParentTileSize), parentTileDim);
            copy(ac == null ? null : ac.part(0.0, 0.3),
                parentTileBuf.array(),
                mParent.subMatrix(parentFrom, parentTo).array(),
                false);
            Func parentTileInterpolation = fullyInside ?
                Matrices.asInterpolationFunc(parentTileBuf, interpolationMethod, false) :
                interpolation(parentTileBuf);
            ProjectiveOperator correctedOperator = correctOperatorForTile(srcFrom, parentFrom);
            if (fullyInside) {
                double[] parentCoordinates = new double[n];
                correctedOperator.map(parentCoordinates, new double[n]); // little additional check: mapping origin
                for (int k = 0; k < n; k++) {
                    if (parentCoordinates[k] < -1.0 || parentCoordinates[k] > dimParent[k] + 1)
                        throw new AssertionError("Error while correcting linear operator: the origin of "
                            + "coordinates is transformed to a point" + toS(parentCoordinates) + " outside the tile");
                }
            }
            Func result = correctedOperator.apply(parentTileInterpolation);
            if (fFiltered != null) {
                result = fFiltered.operator().apply(result);
            }
            Func tileTransformed = result;
            Matrix<? extends PArray> lazy = coordFuncMatrix(tileTransformed, lazyDim);
            return copy(ac == null ? null : ac.part(0.3, 1.0),
                mDest.subMatrix(srcFrom, srcTo).array(),
                lazy.array(),
                compare);
        }

        private ProjectiveOperator correctOperatorForTile(long[] srcFrom, long[] parentFrom) {
            double[] srcFromDouble = new double[n];
            for (int k = 0; k < n; k++) {
                srcFromDouble[k] = srcFrom[k];
            }
            // New operator is a composition of 3 operators:
            // 1) shifting by +srcFrom;
            // 2) original transforming;
            // 3) shifting by -parentFrom
            if (po instanceof LinearOperator) {
                // In other words, we have
                //     A(x+srcFrom)+b - parentFrom = A*x + A*srcFrom+b - parentFrom = A*x + po(srcFrom)-parentFrom
                double[] transformedFromDouble = new double[n];
                po.map(transformedFromDouble, srcFromDouble);
                double[] correctedB = new double[n];
                for (int k = 0; k < n; k++) {
                    correctedB[k] = transformedFromDouble[k] - parentFrom[k];
                }
                return ((LinearOperator) po).changeB(correctedB);
            } else {
                // In other words (x is src, y is parent, pfi=parentFrom[i]), we have
                //          Ai*(x+srcFrom)+bi         Ai*x - (pfi*c)*x + Ai*srcFrom + bi - pfi*c*srcFrom - pfi*d
                //     yi = ----------------- - pfi = ----------------------------------------------------------
                //           c*(x+srcFrom)+d                            c*x + (c*srcFrom+d)
                double[] a = po.a();
                double[] b = po.b();
                double[] c = po.c();
                double d = po.d();
                assert po.n() == n && b.length == n && c.length == n && a.length == n * n;
                double csf = 0.0; // c*srcFrom
                for (int i = 0; i < n; i++) {
                    csf += c[i] * srcFromDouble[i];
                }
                for (int i = 0, ofs = 0; i < n; i++) {
                    double pfi = parentFrom[i];
                    double aisf = 0.0; // Ai*srcFrom
                    for (int j = 0; j < n; j++, ofs++) {
                        aisf += a[ofs] * srcFromDouble[j];
                        a[ofs] -= pfi * c[j];
                    }
                    b[i] += aisf - pfi * csf - pfi * d;
                }
                d += csf;
                return ProjectiveOperator.getInstance(a, b, c, d);
            }
        }

        private boolean checkFullyOutside(long[] parentFrom, long[] parentTo) {
            for (int k = 0; k < n; k++) {
                if (parentTo[k] <= 0 || parentFrom[k] >= dimParent[k]) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkFullyInsideAndTrim(long[] parentFrom, long[] parentTo) {
            boolean result = true;
            for (int k = 0; k < n; k++) {
                if (parentFrom[k] < 0) {
                    parentFrom[k] = 0;
                    result = false;
                }
                if (parentTo[k] > dimParent[k]) {
                    parentTo[k] = dimParent[k];
                    result = false;
                }
            }
            return result;
        }

        /**
         * Returns the maximal last dimension <tt>dim</tt> of the layer submatrix of the source matrix,
         * so that any such submatrix depends on, as a maximum, the layer submatrix
         * in the parent matrix with the last dimension <tt>dimParent</tt>.
         * Here the "layer" submatrix means a submatrix where all dimension, besides the last one,
         * are the same as in the original matrix.
         * Returns <tt>0</tt> if there is no such submatrix.
         * Must be called only in a case of a resizing.
         *
         * @param dimParent dimensions in the parent matrix.
         * @return the maximal last dimension in the source layer submatrix, so that
         *         <tt>dimParent</tt> layer submatrix is enough to calculate it.
         */
        private long parentLayerToSrcCoordSystem(long dimParent) {
            assert po instanceof LinearOperator;
            assert resizingTransformation;
            dimParent--;
            // we must subtract 1, because some destination submatrices can correspond to noninteger parent
            // submatrices, that will require additional element while rounding coordinates
            dimParent -= depRange.size();
            if (dimParent <= 1) {
                return 0;
            }
            assert diagonal[n - 1] > 0.0;
            double dimSrc = dimParent / diagonal[n - 1];
            if (fFiltered != null) {
                ApertureFilterOperator afo = fFiltered.operator();
                double lastASide = afo.apertureTo(n - 1) - afo.apertureFrom(n - 1);
                dimSrc -= lastASide;
            }
            dimSrc -= 1e-3; // previous calculations could be not precise
            if (dimSrc <= 0.0)
                return 0;
            return (long) dimSrc;
        }

        /**
         * Returns the maximal side <tt>dim</tt> of the square (cubic, ...) submatrix of the source matrix,
         * so that any such submatrix (<tt><nobr>dim x dim x ...</nobr></tt>) depends on, as a maximum,
         * <tt>dimParent x dimParent x ...</tt> submatrix in the parent matrix.
         * Returns <tt>0</tt> if there is no such submatrix.
         * Must be called only in a case of an affine transformation.
         *
         * <p>In other words, let <i>O</i> be our linear (affine) transformation <i>x</i>=<i>O</i>(<i>y</i>),
         * that transforms a point <i>y</i> at the destination matrix (and simultaneously at the source lazy matrix,
         * which has the same dimensions) to a point <i>x</i> at the parent matrix, which should be transformed
         * (for example, rotated) by this copier.
         * This method finds the maximal (or, at least, as large as possible) hypercube <b>C</b>
         * in the destination and source matrices, so that its transformation <i>O</i>(<b>C</b>)
         * is a subset of a hypercube <nobr><b>D</b> = <tt>dimParent x dimParent x ...</tt></nobr>
         *
         * <p>The obvious, "direct" way to do this is finding the inverse affine transformation
         * <i>O</i><sup>-1</sup>, getting the figure <i>O</i><sup>-1</sup>(<b>D</b>) and calculating
         * its maximal inscribed hypercube <b>C</b>&sube;<i>O</i><sup>-1</sup>(<b>D</b>). It is a complex
         * algorithm, including finding the inverse matrix and finding maximal inscribed hypercube.
         * But there is much simpler algorithm.
         *
         * <p>Let <b>c</b> is a hypercube <tt><nobr>1 x 1 x ...</nobr></tt>. We find <b>d</b>: the hypercube
         * <tt><nobr>d x d x ...</nobr></tt>, circumscribed around <i>O</i>(<b>c</b>).
         * The required <tt>dim</tt> is just <tt>dimParent/d</tt>!
         * It is true because resizing the argument of an affine transformation leads to the same resizing
         * of the result. If we shall resize <b>c</b> in <tt>dim</tt> times and get
         * <nobr><b>C</b> = <tt>dim x dim x ...</tt></nobr> hypercube, then <i>O</i>(<b>c</b>)
         * will be also resized in <tt>dim</tt> times and will be subset of
         * <nobr><b>D</b> = <tt>dimParent x dimParent x ...</tt></nobr>: <tt>dimParent</tt>=<tt>dim</tt>*<tt>d</tt>.
         *
         * @param dimParent dimensions in the parent matrix.
         * @return the maximal dimensions in the source submatrix, so that
         *         <tt>dimParent x dimParent x ...</tt> submatrix is enough to calculate it.
         */
        private long parentRegularSquareTileToSrcCoordSystem(long dimParent) {
            assert po instanceof LinearOperator;
            assert affineTransformation;
            assert n <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS; // was checked before this
            dimParent--;
            // we must subtract 1, because some destination submatrices can correspond to non-integer parent
            // submatrices, that will require additional element while rounding coordinates
            dimParent -= depRange.size();
            if (dimParent <= 1) {
                return 0;
            }
            double[] minParentCoordinates = new double[n];
            double[] maxParentCoordinates = new double[n];
            JArrays.fillDoubleArray(minParentCoordinates, Double.POSITIVE_INFINITY);
            JArrays.fillDoubleArray(maxParentCoordinates, Double.NEGATIVE_INFINITY);
            double[] srcCoordinates = new double[n];
            double[] parentCoordinates = new double[n];
            for (int bits = 0, maxBits = 1 << n; bits < maxBits; bits++) {
                for (int k = 0; k < n; k++) {
                    srcCoordinates[k] = (bits >>> k) & 1;
                }
                po.map(parentCoordinates, srcCoordinates);
                JArrays.minDoubleArray(minParentCoordinates, 0, parentCoordinates, 0, n);
                JArrays.maxDoubleArray(maxParentCoordinates, 0, parentCoordinates, 0, n);
            }
            // minParentCoordinates..maxParentCoordinates is the parallelepiped, circumscribed around
            // the result O(c) of transformation of the cube c=1x1x...
            double d = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < parentCoordinates.length; k++) {
                d = Math.max(d, maxParentCoordinates[k] - minParentCoordinates[k]);
            }
            // d x d x ... is the hypercube d, circumscribed around O(c)
            if (Double.isNaN(d) || d <= 0.0)
                return 0;
            // for compression, the parent matrix is compressed not greater than in minSide times by any coordinates
            double dimSrc = dimParent / d;
            // so, dimSrc x dimSrc x ... is transformed to a hypercube not greater than dimParent x dimParent x ...
            if (fFiltered != null) {
                dimSrc -= fFiltered.operator().maxApertureSize();
            }
            dimSrc -= 0.001; // previous calculations could be not precise
            if (dimSrc <= 0.0)
                return 0;
            if (dimSrc >= dimParent)
                return (long) dimSrc;
            if (Math.pow(dimSrc / dimParent, n) <=
                1.0 / Arrays.SystemSettings.MIN_NON_OPTIMIZED_COMPRESSION_FOR_TILING)
            {
                // Affine transformations usually do not require any averaging, so,
                // it is better to calculate result point-by-point without optimization,
                // than to download in memory very large extra data (in MIN_NON_OPTIMIZED_COMPRESSION_FOR_TILING
                // times more elements that we need to process).
                if (this.fFiltered == null) {
                    // However, fFiltered may occur not null, if it is resizing and there was impossible
                    // to use copyWithBufferingLayers() due to very strong compression
                    return 0;
                }
            }
            return (long) dimSrc;
        }

        /**
         * Returns the minimal rectangular area in the parent matrix,
         * so that the given submatrix <tt>srcFrom..srcTo</tt> of the target lazy matrix
         * depends only on elements of this area.
         * The results are saved in <tt>parentFrom</tt> and <tt>parentTo</tt> arrays.
         *
         * @param parentFrom the minimal coordinates of the found area, inclusive.
         * @param parentTo   the maximal coordinates of the found area, exclusive.
         * @param srcFrom    the starting coordinates of the checked submatrix, inclusive.
         * @param srcTo      the ending coordinates of the checked submatrix, exclusive.
         */
        private void srcSquareTileToParentCoordSystem(
            long[] parentFrom, long[] parentTo,
            long[] srcFrom, long[] srcTo)
        {
            assert n <= Matrix.MAX_DIM_COUNT_FOR_SOME_ALGORITHMS; // was checked before this
            double[] minParentCoordinates = new double[n];
            double[] maxParentCoordinates = new double[n]; // inclusive!
            double[] srcCoordinates = new double[n];
            double[] parentCoordinates = new double[n];
            JArrays.fillDoubleArray(minParentCoordinates, Double.POSITIVE_INFINITY);
            JArrays.fillDoubleArray(maxParentCoordinates, Double.NEGATIVE_INFINITY);
            for (int bits = 0, maxBits = 1 << n; bits < maxBits; bits++) {
                for (int k = 0; k < n; k++) {
                    srcCoordinates[k] = ((bits >>> k) & 1) == 0 ? srcFrom[k] + aFrom[k] : srcTo[k] - 1 + aTo[k];
                }
                po.map(parentCoordinates, srcCoordinates);
                JArrays.minDoubleArray(minParentCoordinates, 0, parentCoordinates, 0, n);
                JArrays.maxDoubleArray(maxParentCoordinates, 0, parentCoordinates, 0, n);
            }
            for (int k = 0; k < n; k++) {
                parentFrom[k] = (long) (Math.floor(minParentCoordinates[k]) + depRange.min()); // "floor" necessary!
                parentTo[k] = (long) (Math.ceil(maxParentCoordinates[k]) + depRange.max() + 1.0); // exclusive
                // casting to long after all operations to avoid long overflow
                assert parentFrom[k] <= parentTo[k] :
                    "parentFrom[" + k + "] = " + parentFrom[k] + " > parentTo[" + k + "] = " + parentTo[k];
                // but it is possible that parentTo[k] == parentFrom[k],
                // usually for projective transformations in a case of division by zero and overflow
            }
//            System.out.println(toS(srcFrom) + ".." + toS(srcTo) + " --> "
//                + toS(minParentCoordinates) + ".." + toS(maxParentCoordinates) + " --> "
//                + toS(parentFrom) + ".." + toS(parentTo) + " by " + po);
        }

        private Func interpolation(Matrix<? extends PArray> m) {
            return Matrices.isOnlyInsideInterpolationFunc(fInterpolation) ?
                Matrices.asInterpolationFunc(m, interpolationMethod,
                    Matrices.isCheckedOnlyInsideInterpolationFunc(fInterpolation)) :
                Matrices.asInterpolationFunc(m, interpolationMethod,
                    Matrices.getOutsideValue(fInterpolation));
        }

        private Matrix<? extends PArray> coordFuncMatrix(Func transformed, long[] dim) {
            return Matrices.asCoordFuncMatrix(truncationMode, transformed, mDest.type(PArray.class), dim);
        }

        private static long estimateTileSide(long[] dim, long bufSize) {
            long maxDim = 0;
            for (long d : dim) {
                maxDim = Math.max(maxDim, d);
            }
            long result = (long) Math.pow(bufSize, 1.0 / dim.length);
            long lastResult;
            do {
                lastResult = result;
                result = Math.max(result + 1, (long) (result * 1.05)); // 5% mistake is not too important
                // as a maximum, ~ log(2^63)/log(1.05) = 895 iterations; really much less
            } while (result <= maxDim && tileSize(dim, result) <= bufSize);
            return lastResult;
        }

        private static long tileSize(long[] dim, long tileSide) {
            long result = 1;
            for (long d : dim) {
                result *= Math.min(d, tileSide); // overflow impossible: product of all dim is a long value
            }
            return result;
        }

        private static String toS(double[] array) {
            return JArrays.toString(array, ";", 100);
        }

        private static String toS(long[] array) {
            return JArrays.toString(array, ";", 100);
        }
    }

    static class TileCopier extends ArraysBufferedCopier {
        private final boolean strictMode; // used for recurrent call for untiled matrices
        private final long copiedLength;
        private final long[] dim;
        private final long lastDim;
        private final long[] tileDim;
        private final long lastTileDim;
        private final long[] layerDim;
        private final long layerSize;
        private final Matrix<? extends UpdatableArray> baseMatrixDest;
        private final Matrix<? extends Array> baseMatrixSrc;
        private final long maxBufSize;
        private final int n; // == dim.length
        private final boolean optimizationPossible;

        private TileCopier(
            ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks,
            boolean strictMode, boolean compare)
        {
            super(context, dest, src, numberOfTasks, compare);
            this.copiedLength = Math.min(src.length(), dest.length());
            this.strictMode = strictMode;
            if (Arrays.isTiled(src)) {
                this.baseMatrixSrc = ((ArraysTileMatrixImpl.TileMatrixArray) src).baseMatrix();
                assert this.baseMatrixSrc.size() == src.length() : "Error in implementation of " + src;
                this.dim = this.baseMatrixSrc.dimensions();
                this.tileDim = Arrays.tileDimensions(src);
                if (Arrays.isTiled(dest)) {
                    this.baseMatrixDest = ((ArraysTileMatrixImpl.TileMatrixArray) dest)
                        .baseMatrix().cast(UpdatableArray.class);
                    assert this.baseMatrixDest.size() == dest.length() : "Error in implementation of " + dest;
                    this.maxBufSize = -1; // not used in this case
                    this.optimizationPossible = this.copiedLength > 0
                        && this.baseMatrixSrc.dimEquals(this.baseMatrixDest)
                        && java.util.Arrays.equals(this.tileDim, Arrays.tileDimensions(dest));
                } else {
                    double eSize = (double) Arrays.sizeOf(dest) / (double) dest.length();
                    this.maxBufSize = Math.round(Arrays.SystemSettings.availableTempJavaMemoryForTiling() / eSize);
                    this.baseMatrixDest = null;
                    this.optimizationPossible = this.copiedLength > 0
                        && eSize > 0.0 // don't optimize if cannot estimate necessary memory
                        && !SimpleMemoryModel.isSimpleArray(this.baseMatrixSrc.array())
                        && !BufferMemoryModel.isBufferArray(this.baseMatrixSrc.array());
                    // Simple and Buffer tiled matrices are already processed with maximal performance
                }
            } else {
                this.baseMatrixSrc = null;
                if (Arrays.isTiled(dest)) {
                    this.baseMatrixDest = ((ArraysTileMatrixImpl.TileMatrixArray) dest)
                        .baseMatrix().cast(UpdatableArray.class);
                    assert this.baseMatrixDest.size() == dest.length() : "Error in implementation of " + dest;
                    this.dim = this.baseMatrixDest.dimensions();
                    this.tileDim = Arrays.tileDimensions(dest);
                    double eSize = (double) Arrays.sizeOf(src) / (double) src.length();
                    this.maxBufSize = Math.round(Arrays.SystemSettings.availableTempJavaMemoryForTiling() / eSize);
                    this.optimizationPossible = this.copiedLength > 0
                        && eSize > 0.0 // don't optimize if cannot estimate necessary memory
                        && !SimpleMemoryModel.isSimpleArray(this.baseMatrixDest.array())
                        && !BufferMemoryModel.isBufferArray(this.baseMatrixDest.array());
                    // Simple and Buffer tiled matrices are already processed with maximal performance
                } else {
                    throw new AssertionError("Internal error while using " + TileCopier.class);
                }
            }
            this.n = this.dim.length;
            if (this.n < 1)
                throw new AssertionError("Invalid implementation of some classes: number of dimensions is " + this.n);
            this.lastDim = this.dim[n - 1];
            this.lastTileDim = this.tileDim[n - 1];
            this.layerDim = this.dim.clone();
            this.layerDim[n - 1] = Math.min(this.lastDim, this.lastTileDim);
            this.layerSize = Arrays.longMul(this.layerDim);
            if (this.layerSize < 0)
                throw new AssertionError("Invalid implementation of some classes: "
                    + "illegal product of layer dimensions");
        }

        @Override
        public boolean process() {
            if (!optimizationPossible) {
                return super.process();
            }
            if (baseMatrixSrc != null && baseMatrixDest != null) {
                // Simple case: copying one tiled matrix into another tiled matrix with same dimensions and tiles.
                // It is enough to copy the underlying matrices.
                ArraysBufferedCopier copier = ArraysBufferedCopier.getInstance(context,
                    baseMatrixDest.array(), baseMatrixSrc.array(), numberOfTasks,
                    strictMode, compare);
                boolean result = copier.process();
                this.usedAlgorithm = copier.usedAlgorithm;
                return result;
            }
            assert baseMatrixSrc != null || baseMatrixDest != null;
//            System.out.println(copiedLength+";"+layerSize+","+lastDim+";"+maxBufSize+";"+dest);
            if (copiedLength < layerSize) {
                return super.process();
            }
            if (layerSize > maxBufSize) {
                return super.process();
            }
//            System.out.println("OK!"+JArrays.toString(layerDim,"x",100));
            Matrix<? extends UpdatableArray> buf = Arrays.SMM.newMatrix(
                UpdatableArray.class, baseMatrixSrc != null ? dest.elementType() : src.elementType(),
                layerDim);
            Matrix<? extends UpdatableArray> tiledBuf = buf.tile(tileDim);
            final long tiledArrayLength = baseMatrixSrc != null ? baseMatrixSrc.size() : baseMatrixDest.size();
            boolean result = false;
            for (long p = 0, lastCoord = 0; p < copiedLength; p += layerSize, lastCoord += lastTileDim) {
                long len = layerSize;
                if (p + len > tiledArrayLength) {
                    assert lastCoord + lastTileDim > lastDim; // the same check in other terms
                    len = tiledArrayLength - p;
                }
                if (p + len > copiedLength) {
                    assert src.length() != dest.length();
                    len = copiedLength - p;
                    result |= copy(context == null ? null : context.part(p, p + len, copiedLength),
                        dest.subArr(p, len),
                        src.subArr(p, len),
                        compare);
                } else {
                    ArrayContext ac = context == null ? null : context.part(p, p + len, copiedLength);
                    if (lastCoord + lastTileDim > lastDim) {
                        long[] finishingLayerDim = dim.clone();
                        finishingLayerDim[n - 1] = lastDim - lastCoord;
                        buf = Matrices.matrix(
                            buf.array().subArr(0, Arrays.longMul(finishingLayerDim)),
                            finishingLayerDim);
                        tiledBuf = buf.tile(tileDim);
                        // The last layer should be tiled with less tiles.
                        // It is important for tiling operation (baseMatrixDest != null):
                        // in other case, copying src.subArr(p, len) into tiledBuf will fill it
                        // by tiles with full height lastTileDim instead of correct lastDim - lastCoord.
                    }
                    if (baseMatrixSrc != null) { // src is tiled: untiling operation
                        copy(ac == null ? null : ac.part(0.0, 0.3),
                            buf.array(),
                            baseMatrixSrc.array().subArr(p, len),
                            false);
                        result |= copy(ac == null ? null : ac.part(0.3, 1.0),
                            dest.subArr(p, len),
                            tiledBuf.array(),
                            1, // no reasons for parallel copying tiled matrix in SimpleMemoryModel
                            compare);
                    } else { // dest is tiled: tiling operation
                        copy(ac == null ? null : ac.part(0.0, 0.7),
                            tiledBuf.array(),
                            src.subArr(p, len),
                            false);
                        result |= copy(ac == null ? null : ac.part(0.7, 1.0),
                            baseMatrixDest.array().subArr(p, len),
                            buf.array(),
                            1, // to be on the safe side
                            compare);
                    }
                }
            }
            this.usedAlgorithm = baseMatrixSrc != null ? Arrays.CopyAlgorithm.UNTILING : Arrays.CopyAlgorithm.TILING;
            return result;
        }
    }

}
