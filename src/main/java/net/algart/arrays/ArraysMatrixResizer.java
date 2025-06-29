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

package net.algart.arrays;

import net.algart.math.functions.*;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

class ArraysMatrixResizer {
    private static final int MAX_OPTIMIZED_APERTURE_SIZE = 1048576;
    // - each instance of this class allocates an array int[apertureSize], so we must limit apertureSize.
    // Note: must be MAX_OPTIMIZED_APERTURE_SIZE <= Integer.MAX_VALUE / 256 = 8388607!
    // It is necessary for correct implementation of ResizerForPowerOfTwo.averageByte

    final int n;
    final long[] scales;
    final int scalesProduct;
    final int numberOfConsecutive;
    final int firstJump; // after every numberOfConsecutive pixels we must also increase the offset by firstJump
    final int step; // = firstJump + numberOfConsecutive
    final int blockSize; // after every blockSize pixels we need to recalculate the offset
    final boolean doAveraging;
    final int[] apertureOffsets;
    final double averagingMultiplier;

    private ArraysMatrixResizer(
        long[] resultingDimensions,
        long[] sourceDimensions, // actually always int (31-bit)
        long[] scales, // actually always int (31-bit)
        int scalesProduct,
        boolean doAveraging)
    {
        this.n = scales.length;
        assert resultingDimensions.length == n;
        assert sourceDimensions.length == n;
        long sourceSize = Arrays.longMul(sourceDimensions);
        assert sourceSize > 0;
        assert sourceSize <= Integer.MAX_VALUE
            : "source matrix must be less than 2^31 elements";
        // - optimization is provided only for not too large source matrices,
        // for example, created by SimpleMemoryModel or representing a tile
        this.scales = scales;
        this.scalesProduct = scalesProduct;
        int m = 0;
        int dimMul = 1;
        while (m < n && scales[m] == 1) {
            assert resultingDimensions[m] == sourceDimensions[m];
            dimMul *= (int) resultingDimensions[m];
            m++;
        }
        this.numberOfConsecutive = dimMul;
        this.firstJump = m == n ? 0 : ((int) scales[m] - 1) * dimMul;
        this.step = this.firstJump + dimMul;
        this.blockSize = m == n ? dimMul : dimMul * (int) resultingDimensions[m];
        this.doAveraging = doAveraging;
        if (doAveraging) {
            this.apertureOffsets = new int[scalesProduct];
            Matrix<?> apertureEnumerator = Matrices.matrix(Arrays.nIntCopies(scalesProduct, 157), scales);
            // - this trivial virtual matrix is a simplest way to enumerate all aperture
            Matrix<?> sourceEnumerator = Matrices.matrix(Arrays.nIntCopies(sourceSize, 157), sourceDimensions);
            long[] coordinatesInAperture = new long[n];
            for (int i = 0; i < scalesProduct; i++) {
                apertureEnumerator.coordinates(i, coordinatesInAperture);
                this.apertureOffsets[i] = (int) sourceEnumerator.index(coordinatesInAperture);
            }
        } else {
            this.apertureOffsets = null;
        }
        this.averagingMultiplier = 1.0 / scalesProduct;
    }

    public static Matrix<PArray> asResized(
        Matrices.ResizingMethod resizingMethod,
        Matrix<? extends PArray> matrix, long[] newDim, double[] scales)
    {
        Objects.requireNonNull(resizingMethod, "Null resizingMethod argument");
        Objects.requireNonNull(matrix, "Null matrix argument");
        final int n = matrix.dimCount();
        if (newDim.length != n) {
            throw new IllegalArgumentException("Illegal number of newDim[] elements: "
                + newDim.length + " instead of " + n);
        }
        if (scales != null && scales.length != n) {
            throw new IllegalArgumentException("Illegal number of scales[] elements: "
                + scales.length + " instead of " + n);
        }
        if (matrix.isEmpty()) {
            return Matrices.asCoordFuncMatrix(ConstantFunc.getInstance(0.0), matrix.type(PArray.class), newDim);
        }
        double[] diagonal = new double[n];
        boolean trivialScales = true;
        for (int k = 0; k < n; k++) {
            long dOld = matrix.dim(k);
            long dNew = newDim[k];
            double mult = (double) dOld / (double) dNew;
            diagonal[k] = scales == null ? mult : 1.0 / scales[k];
            trivialScales &= diagonal[k] == mult;
        }
        Func interpolation = trivialScales ? // optimization
            Matrices.asInterpolationFunc(matrix, resizingMethod.interpolationMethod, false) :
            Matrices.asInterpolationFunc(matrix, resizingMethod.interpolationMethod, 0.0);
        Operator resizing = LinearOperator.getDiagonalInstance(diagonal);
        Func transformed = resizing.apply(interpolation);
        if (resizingMethod.averaging()) {
            boolean doAverage = false;
            long[] apertureDim = new long[diagonal.length];
            for (int k = 0; k < n; k++) {
                doAverage |= diagonal[k] >= 1.5;
                apertureDim[k] = Math.max(1, Math.min(1000000, Math.round(diagonal[k])));
            }
            if (doAverage && !ApertureFilterOperator.tooLargeAperture(apertureDim)) {
                Func averagingFunc = null;
                if (resizingMethod instanceof Matrices.ResizingMethod.Averaging) {
                    averagingFunc = ((Matrices.ResizingMethod.Averaging) resizingMethod).getAveragingFunc(apertureDim);
                }
                transformed = averagingFunc == null ?
                    ApertureFilterOperator.getAveragingInstance(apertureDim).apply(transformed) :
                    ApertureFilterOperator.getInstance(averagingFunc, apertureDim).apply(transformed);
            }
        }
        final Matrix<PArray> result = Matrices.asCoordFuncMatrix(transformed, matrix.type(PArray.class), newDim);
        if (trivialScales && result.array() instanceof ArraysFuncImpl.OptimizationHelperInfo) {
            // for example, it may be not so, when the new dimensions are equal to the original dimensions:
            // in this case, it is very possible that resizing operator will return unchanged function
            // (transformed==interpolation), and asCoordFuncMatrix will detect, that its argument is just
            // an interpolation function, and will return an immutable view of the original matrix
            ((ArraysFuncImpl.OptimizationHelperInfo) result.array()).setOptimizationHelperInfo(resizingMethod);
            // here we are helping ArraysBufferedCopier to optimize copying
        }
        return result;
    }

    public static void resize(
        ArrayContext context,
        Matrices.ResizingMethod resizingMethod,
        Matrix<? extends UpdatablePArray> result,
        Matrix<? extends PArray> src)
    {
        if (tryToResizeWithOptimization(context, resizingMethod, result, src)) {
            return;
        }
        Matrix<?> lazy = ArraysMatrixResizer.asResized(resizingMethod, src, result.dimensions(), null);
        Matrices.copy(context, result, lazy, 0, false);
    }

    public static boolean tryToResizeWithOptimization(
        ArrayContext context,
        Matrices.ResizingMethod resizingMethod,
        Matrix<? extends UpdatablePArray> result,
        Matrix<? extends PArray> src)
    {
        Objects.requireNonNull(resizingMethod, "Null resizingMethod argument");
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(src, "Null source matrix");
        final int n = src.dimCount();
        if (result.dimCount() != n) {
            throw new IllegalArgumentException("The source and result matrices have different number of dimensions: "
                + "the source src is " + src + ", the result is " + result);
        }
        AbstractArray.checkCopyArguments(result.array(), src.array()); // before checking result.size()
        if (result.isEmpty()) {
            return true; // nothing to do
        }
        if (result.dimEquals(src)) {
            // note: we do this even in cases, when optimization of ArraysMatrixResizer is not applicable:
            // for example, if result matrix is not directly accessible
//            System.out.println("SAME");
            Matrices.copy(context, result, src, 0, false); // no resizing
            return true;
        }
        if (src.size() > Integer.MAX_VALUE) {
            return false; // avoid assertions in the constructor
        }
        ArraysMatrixResizer resizer = getInstance(
            resizingMethod, result.elementType(), result.dimensions(), src.dimensions());
        return resizer != null && resizer.compressDirectAccessible(context, result, src);
    }

    // May return null
    public static ArraysMatrixResizer getInstance(
        Matrices.ResizingMethod resizingMethod,
        Class<?> elementType,
        long[] resultingDimensions,
        long[] sourceDimensions)
    {
        long[] scales = getScalesIfCanBeOptimizedForDirectAccessibleResult(
            resizingMethod, elementType, resultingDimensions, sourceDimensions);
        if (scales == null) {
            return null;
        }
        boolean doAveraging = resizingMethod == Matrices.ResizingMethod.AVERAGING ||
            resizingMethod == Matrices.ResizingMethod.POLYLINEAR_AVERAGING;
        long scalesProduct = 1;
        for (long scale : scales) {
            scalesProduct *= scale;
        }
        assert scalesProduct == (int) scalesProduct;
        boolean powerOfTwo = (scalesProduct & (scalesProduct - 1)) == 0;
        return powerOfTwo && doAveraging ?
            new ResizerForPowerOfTwo(resultingDimensions, sourceDimensions, scales, (int) scalesProduct) :
            new ArraysMatrixResizer(resultingDimensions, sourceDimensions, scales, (int) scalesProduct, doAveraging);
    }

    // May return null; the product of the resulting scales is always 31-bit (int)
    // Theoretically, future versions of this class may refuse in optimization, even if this method returns
    // non-null array; but it is not a normal situation (if the result matrix is direct-accessible).
    public static long[] getScalesIfCanBeOptimizedForDirectAccessibleResult(
        Matrices.ResizingMethod resizingMethod,
        Class<?> elementType,
        long[] resultingDimensions,
        long[] sourceDimensions)
    {
        // Note: this method does not check, whether sourceDimensions are too large!
        // Maybe this method is called for sizes of very large matrix, but further we shall
        // use the results for its little tiles
        Objects.requireNonNull(resizingMethod, "Null resizingMethod argument");
        if (sourceDimensions.length != resultingDimensions.length) {
            throw new IllegalArgumentException("Different lengths of resultingDimensions and sourceDimensions");
        }
        if (!(resizingMethod == Matrices.ResizingMethod.AVERAGING ||
            resizingMethod == Matrices.ResizingMethod.POLYLINEAR_AVERAGING ||
            resizingMethod == Matrices.ResizingMethod.SIMPLE ||
            resizingMethod == Matrices.ResizingMethod.POLYLINEAR_INTERPOLATION))
        {
            return null;
        }
        if (elementType == boolean.class) {
            return null;
        }
        long[] scales = new long[sourceDimensions.length];
        long scalesProduct = 1;
        for (int k = 0; k < scales.length; k++) {
            long newDim = resultingDimensions[k];
            long oldDim = sourceDimensions[k];
            if (newDim < 0) {
                throw new IllegalArgumentException("Negative resultingDimensions[" + k + "]");
            }
            if (oldDim < 0) {
                throw new IllegalArgumentException("Negative sourceDimensions[" + k + "]");
            }
            if (newDim == 0 || oldDim == 0 || newDim > oldDim) {
                return null;
            }
            scales[k] = oldDim / newDim;
            if (oldDim != scales[k] * newDim) {
                return null;
            }
            if (scales[k] > Integer.MAX_VALUE) {
                return null;
            }
            scalesProduct *= scales[k]; // overflow impossible: scalesProduct <= MAX_OPTIMIZED_APERTURE_SIZE (int)
            if (scalesProduct > MAX_OPTIMIZED_APERTURE_SIZE) {
                return null;
            }
        }
        return scales;
    }

    boolean compressDirectAccessible(
        final ArrayContext context,
        final Matrix<? extends UpdatablePArray> result,
        final Matrix<? extends PArray> src)
    {
        final UpdatablePArray r = result.array();
        final PArray a = src.array();
        assert r.length() <= a.length();
        assert r.elementType() == a.elementType();
        assert !(a instanceof BitArray) : getClass() + " must not be instantiated for boolean elements";
        final DirectAccessible da;
        if (!(a instanceof DirectAccessible && (da = (DirectAccessible) a).hasJavaArray())) {
            return false;
        }
//        System.out.println("Compression in " + JArrays.toString(scales, ",", 100)
//            + " times, " + numberOfConsecutive + ", " + blockSize + ": " + result + ", " + src + "; " + getClass());
        final int jaOfs = da.javaArrayOffset();
        assert r.length() <= Integer.MAX_VALUE; // because a is DirectAccessible
        final ThreadPoolFactory threadPoolFactory = Arrays.getThreadPoolFactory(context);
        final int numberOfTasks = threadPoolFactory.recommendedNumberOfTasks(r);
        final AtomicLong readyElements = new AtomicLong(0);
        Runnable[] tasks = new Runnable[numberOfTasks];
        for (int taskIndex = 0; taskIndex < numberOfTasks; taskIndex++) {
            final int from = (int) ((long) taskIndex * r.length() / (long) numberOfTasks);
            final int to = (int) ((long) (taskIndex + 1) * r.length() / (long) numberOfTasks);
            // overflow impossible, because r.length() is really 31-bit int
            if (taskIndex == numberOfTasks - 1) {
                assert to == r.length();
            }
            tasks[taskIndex] = new Runnable() {
                final long[] work = new long[n];
                long progressCount = 0;
                long lastProgressCount = 0;

                public void run() {
                    final int fromRem = from % blockSize;
                    final int startCount = Math.min(blockSize - fromRem, to - from);
                    final DataBuffer resultBuffer = Arrays.bufferInternal(r,
                        DataBuffer.AccessMode.READ_WRITE,
                        // 1 + new java.util.Random().nextInt(15), // - for debugging
                        AbstractArray.defaultBufferCapacity(r),
                        true);
                    Arrays.enableCaching(resultBuffer);
                    try {
                        int jumpingRem = from % numberOfConsecutive;
                        //[[Repeat() Char ==> Byte,,Short,,Int,,Long,,Float,,Double;;
                        //           char ==> byte,,short,,int,,long,,float,,double;;
                        //           if(?=\s+\(a\s+instanceof\s+\w+Array) ==> } else if,,...]]
                        if (a instanceof CharArray) {
                            assert r instanceof UpdatableCharArray;
                            char[] ja = (char[]) da.javaArray();
                            DataCharBuffer buf = (DataCharBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    char[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageChar(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageChar(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                        } else if (a instanceof ByteArray) {
                            assert r instanceof UpdatableByteArray;
                            byte[] ja = (byte[]) da.javaArray();
                            DataByteBuffer buf = (DataByteBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    byte[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageByte(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageByte(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                        } else if (a instanceof ShortArray) {
                            assert r instanceof UpdatableShortArray;
                            short[] ja = (short[]) da.javaArray();
                            DataShortBuffer buf = (DataShortBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    short[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageShort(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageShort(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                        } else if (a instanceof IntArray) {
                            assert r instanceof UpdatableIntArray;
                            int[] ja = (int[]) da.javaArray();
                            DataIntBuffer buf = (DataIntBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    int[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageInt(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageInt(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                        } else if (a instanceof LongArray) {
                            assert r instanceof UpdatableLongArray;
                            long[] ja = (long[]) da.javaArray();
                            DataLongBuffer buf = (DataLongBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    long[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageLong(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageLong(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                        } else if (a instanceof FloatArray) {
                            assert r instanceof UpdatableFloatArray;
                            float[] ja = (float[]) da.javaArray();
                            DataFloatBuffer buf = (DataFloatBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    float[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageFloat(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageFloat(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                        } else if (a instanceof DoubleArray) {
                            assert r instanceof UpdatableDoubleArray;
                            double[] ja = (double[]) da.javaArray();
                            DataDoubleBuffer buf = (DataDoubleBuffer) resultBuffer;
                            for (int p = from; p < to; ) {
                                int count = p == from ? startCount : Math.min(blockSize, to - p);
                                int srcOfs = apertureInitialPosition(p) + jaOfs;
                                for (int len; count > 0; p += len, count -= len) {
                                    len = buf.map(p, count, false).cnt();
                                    double[] data = buf.data();
                                    if (doAveraging) {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = averageDouble(ja, srcOfs);
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = averageDouble(ja, srcOfs);
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    } else {
                                        if (numberOfConsecutive == 1) {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++, srcOfs += step) {
                                                data[j] = ja[srcOfs];
                                            }
                                        } else {
                                            for (int j = buf.from(), jMax = j + len; j < jMax; j++) {
                                                data[j] = ja[srcOfs];
                                                srcOfs++;
                                                jumpingRem++;
                                                if (jumpingRem == numberOfConsecutive) {
                                                    srcOfs += firstJump;
                                                    jumpingRem = 0;
                                                }
                                            }
                                        }
                                    }
                                    buf.force();
                                    updateProgress(len);
                                }
                            }
                            //[[Repeat.AutoGeneratedEnd]]
                        } else {
                            throw new AssertionError("Unallowed type of built-in array: " + a.getClass());
                        }
                    } finally {
                        Arrays.dispose(resultBuffer);
                    }
                }

                private int apertureInitialPosition(int resultPosition) {
                    result.coordinates(resultPosition, work);
                    for (int k = 0; k < n; k++) {
                        work[k] *= scales[k];
                    }
                    return (int) src.index(work);
                }

                private void updateProgress(int resultElementsCount) {
                    if (context == null) {
                        return;
                    }
                    progressCount += (long) resultElementsCount * scalesProduct;
                    long newReadyElements = readyElements.addAndGet((long) resultElementsCount * scalesProduct);
                    if (progressCount - lastProgressCount >= 262144) {
                        assert newReadyElements <= src.size();
                        lastProgressCount = progressCount;
                        context.checkInterruptionAndUpdateProgress(src.elementType(), newReadyElements, src.size());
                    }
                }
            };
        }
        threadPoolFactory.performTasks(tasks);
        return true;
    }

    //[[Repeat() Byte ==> Short,,Char,,Int,,Long,,Float,,Double;;
    //           byte ==> short,,char,,int,,long,,float,,double;;
    //           (\s+\&\s+0xFF) ==> $1FF,, ,,...;;
    //           (long\s+sum) ==> $1,,$1,,$1,,double sum,,...;;
    //           return\s+\(double\)\s+\((.*?)\) ==> return $1,,...]]
    byte averageByte(byte[] src, int offset) {
        long sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset] & 0xFF;
        }
        return (byte) (averagingMultiplier * sum);
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    short averageShort(short[] src, int offset) {
        long sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset] & 0xFFFF;
        }
        return (short) (averagingMultiplier * sum);
    }

    char averageChar(char[] src, int offset) {
        long sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset];
        }
        return (char) (averagingMultiplier * sum);
    }

    int averageInt(int[] src, int offset) {
        long sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset];
        }
        return (int) (averagingMultiplier * sum);
    }

    long averageLong(long[] src, int offset) {
        double sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset];
        }
        return (long) (averagingMultiplier * sum);
    }

    float averageFloat(float[] src, int offset) {
        double sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset];
        }
        return (float) (averagingMultiplier * sum);
    }

    double averageDouble(double[] src, int offset) {
        double sum = 0;
        for (int apertureOffset : apertureOffsets) {
            sum += src[offset + apertureOffset];
        }
        return averagingMultiplier * sum;
    }

    //[[Repeat.AutoGeneratedEnd]]

    private static class ResizerForPowerOfTwo extends ArraysMatrixResizer {
        private final int scaleProductLog;

        private ResizerForPowerOfTwo(
            long[] resultingDimensions,
            long[] sourceDimensions,
            long[] scales,
            int scalesProduct)
        {
            super(resultingDimensions, sourceDimensions, scales, scalesProduct, true);
            this.scaleProductLog = 31 - Integer.numberOfLeadingZeros(scalesProduct);
            assert scalesProduct == 1 << this.scaleProductLog;
            assert apertureOffsets.length == scalesProduct;
        }

        @Override
        byte averageByte(byte[] src, int offset) {
            int sum = 0;
            for (int apertureOffset : apertureOffsets) {
                sum += src[offset + apertureOffset] & 0xFF;
            }
            return (byte) (sum >>> scaleProductLog);
        }

        @Override
        short averageShort(short[] src, int offset) {
            long sum = 0;
            for (int apertureOffset : apertureOffsets) {
                sum += src[offset + apertureOffset] & 0xFFFF;
            }
            return (short) (sum >>> scaleProductLog);
        }

        @Override
        char averageChar(char[] src, int offset) {
            long sum = 0;
            for (int apertureOffset : apertureOffsets) {
                sum += src[offset + apertureOffset];
            }
            return (char) (sum >>> scaleProductLog);
        }

        @Override
        int averageInt(int[] src, int offset) {
            long sum = 0;
            for (int apertureOffset : apertureOffsets) {
                sum += src[offset + apertureOffset];
            }
            return (int) (sum >>> scaleProductLog);
        }
    }
}
