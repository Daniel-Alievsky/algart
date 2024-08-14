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

package net.algart.matrices.spectra;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;

import java.util.Objects;

/**
 * <p>Array of samples, where each sample is a vector of real numbers with some fixed length,
 * represented by an array of <code>double</code> values,
 * stored in an AlgART arras {@link UpdatablePNumberArray}.
 * These vectors correspond to ranges in this array, given with some fixed step.</p>
 *
 * <p>Please use {@link #asSampleArray(MemoryModel, UpdatablePNumberArray, long, long, long)} method for
 * creating instances of this class. Please use comments to this method for more details about storing
 * samples in an  AlgART array.</p>
 *
 * <p>All operations over samples (adding, subtracting, multiplying) are performed via corresponding operations
 * over elements of the AlgART array. Elements of this array are interpreted as <code>double</code> values,
 * as if they are read/written by {@link PArray#getDouble(long)} and {@link UpdatablePArray#setDouble(long, double)}
 * methods. There is the only exception: if the element type of the underlying AlgART array
 * is not <code>float</code> or <code>double</code>, and an operation leads to overflow (for example,
 * we try to multiply
 * a sample by the real scalar <code>1e10</code>), then the results can differ from the results
 * of the simplest code based
 * on {@link PArray#getDouble(long)} and {@link UpdatablePArray#setDouble(long, double)} calls.</p>
 *
 * <p>The instances of this class are not thread-safe, but <b>are thread-compatible</b>
 * and can may be synchronized manually if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class RealVectorSampleArray implements SampleArray {
    private static final int BUFFER_LENGTH = 32768; // no reasons to optimize longer vectors
    private static final int NUMBER_OF_BUFFERS = 2; // maximal number of simultaneous buffers in methods below

    private static final JArrayPool FLOAT_BUFFERS =
        JArrayPool.getInstance(float.class, NUMBER_OF_BUFFERS * BUFFER_LENGTH);
    private static final JArrayPool DOUBLE_BUFFERS =
        JArrayPool.getInstance(double.class, NUMBER_OF_BUFFERS * BUFFER_LENGTH);

    final long vectorLength;
    final long vectorStep;
    final long length;
    final UpdatablePNumberArray samples;

    RealVectorSampleArray(UpdatablePNumberArray samples, long vectorLength, long vectorStep, long length) {
    // Besides more clarity, "length" argument allows processing a case vectorLength=vectorStep=0
        Objects.requireNonNull(samples, "Null samples");
        if (vectorLength < 0) {
            throw new IllegalArgumentException("Negative vectorLength = " + vectorLength);
        }
        if (vectorStep < vectorLength) {
            throw new IllegalArgumentException("vectorStep = "+ vectorStep + " < vectorLength = " + vectorLength);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Negative length = " + length);
        }
        long m = samples.length() - vectorLength;
        if ((length > 0 && m < 0) || (vectorStep > 0 && (length - 1) > m / vectorStep)) {
            throw new IllegalArgumentException("samples is too short: its length " + samples.length()
                + " < (length - 1) * vectorStep + vectorLength = "
                + (length - 1) + " * " + vectorStep + " + " + vectorLength);
        }
        this.samples = samples;
        this.vectorLength = vectorLength;
        this.vectorStep = vectorStep;
        this.length = length;
    }

    /**
     * Returns a view of the specified pair of AlgART arrays as an array of real vector samples.
     * Real vectors are stored in the specified array <code>samples</code>.
     * More precisely, the sample <code>#k</code> in the returned sample array is a vector of <code>vectorLength</code>
     * real numbers, where the number <code>#j</code> is stored in the element
     * <code>#(k*vectorStep)+j</code> of <code>samples</code> array.
     *
     * <p>The returned sample array is backed by this AlgART array, so any changes of the samples
     * in the returned array are reflected in this array, and vice versa.
     * More precisely, the returned sample array is backed by
     * <code>samples.{@link UpdatableArray#asUnresizable asUnresizable()}</code>:
     * if the passed arrays are {@link MutableArray resizable}, posible future changes of its length
     * will not affect behavior of the returned sample array.
     *
     * <p>The length of each vector sample, <code>vectorLength</code>, must be in range
     * <code>0&lt;=vectorLength&lt;=vectorStep</code>. Moreover, <code>samples</code>
     * array must be long enough for storing <code>length</code> vectors with specified <code>vectorStep</code>:
     * <code>(length-1)*vectorStep+vectorLength &lt;= samples.length()</code>.
     *
     * @param memoryModel  the memory model, which will be used, when necessary, by
     *                     {@link #newCompatibleSamplesArray(long)} method; can be {@code null},
     *                     then {@link SimpleMemoryModel} will be used.
     * @param samples      the samples.
     * @param vectorLength the length of each real vector.
     * @param vectorStep   the step of storing vectors in <code>samples</code> array.
     * @param length       the length of the returned sample array.
     * @return             the array of vector real samples, represented by corresponding ranges (subarrays)
     *                     of this AlgART array.
     * @throws NullPointerException     if <code>samples</code> is {@code null}.
     * @throws IllegalArgumentException if <code>vectorLength&lt;0</code>, <code>vectorStep&lt;vectorLength</code>,
     *                                  <code>length&lt;0</code> or
     *                                  <code>(length-1)*vectorStep+vectorLength &lt;=
     *                                  samples.length()</code>
     *                                  (the last condition is checked mathematically accurately even if these
     *                                  values <code>&gt;Long.MAX_VALUE</code>).
     * @throws TooLargeArrayException   (little probability)
     *                                  if the {@link MemoryModel#maxSupportedLength(Class) maximal length},
     *                                  supported by the specified memory model
     *                                  (or {@link SimpleMemoryModel} if <code>memoryModel==null</code>)
     *                                  is not enough for allocating
     *                                  <code>{@link #GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH}*vectorLength</code>
     *                                  elements with the type <code>samples.elementType()</code>.
     */
    public static RealVectorSampleArray asSampleArray(
        MemoryModel memoryModel,
        UpdatablePNumberArray samples,
        long vectorLength, long vectorStep, long length)
    {
        Objects.requireNonNull(samples, "Null samples");
        samples = (UpdatablePNumberArray)samples.asUnresizable(); // to be sure that its length will not be changed
        if (samples instanceof DirectAccessible && ((DirectAccessible)samples).hasJavaArray()
            && vectorLength <= Arrays.SMM.maxSupportedLength(samples.elementType())
            / GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH)
        {
            if (samples instanceof FloatArray) {
                return new DirectRealFloatVectorSampleArray(samples, vectorLength, vectorStep, length);
            }
            if (samples instanceof DoubleArray) {
                return new DirectRealDoubleVectorSampleArray(samples, vectorLength, vectorStep, length);
            }
        }
        if (vectorLength < BUFFER_LENGTH) {
            if (samples instanceof FloatArray) {
                return new RealFloatVectorSampleArray(samples, vectorLength, vectorStep, length);
            }
            if (samples instanceof DoubleArray) {
                return new RealDoubleVectorSampleArray(samples, vectorLength, vectorStep, length);
            }
        }
        if (memoryModel == null) {
            memoryModel = Arrays.SMM;
        }
        if (vectorLength > memoryModel.maxSupportedLength(samples.elementType())
            / GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH) {
            throw new TooLargeArrayException("Too large samples for the given memory model " + memoryModel
                + ": it cannot allocate " + GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH
                + " samples (each sample is a vector of " + vectorLength + " numbers");
        }
        return new CommonRealVectorSampleArray(memoryModel, samples, vectorLength, vectorStep, length);
    }

    public final boolean isComplex() {
        return false;
    }

    public final long length() {
        return length;
    }

    public abstract RealVectorSampleArray newCompatibleSamplesArray(long length);

    public void copy(long destIndex, SampleArray src, long srcIndex) {
        RealVectorSampleArray a = (RealVectorSampleArray)src;
        v(destIndex).copy(a.v(srcIndex));
    }

    public void swap(long firstIndex, long secondIndex) {
        samples.swap(firstIndex * vectorStep, secondIndex * vectorStep, vectorLength);
    }

    public abstract void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2);

    public abstract void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2);

    public abstract void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2);

    public abstract void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2);

    public abstract void add(long destIndex, long srcIndex1, long srcIndex2);

    public abstract void sub(long destIndex, long srcIndex1, long srcIndex2);

    public abstract void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm);

    public abstract void combineWithRealMultipliers(long destIndex,
        long srcIndex1, double a1, long srcIndex2, double a2);

    public abstract void multiplyByRealScalar(long index, double a);

    public String toString(String format, String separator, int maxStringLength) {
        Objects.requireNonNull(format, "Null format argument");
        Objects.requireNonNull(separator, "Null separator argument");
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("maxStringLength argument must be positive");
        }
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (long i = 0, disp = 0; i < length; i++, disp += vectorStep) {
            if (sb.length() >= maxStringLength) {
                sb.append(separator).append("..."); break;
            }
            if (i > 0) {
                sb.append(" ").append(separator);
            }
            for (long j = 0; j < vectorLength; j++) {
                if (sb.length() >= maxStringLength) {
                    break;
                }
                sb.append(j == 0 ? "(" : separator);
                sb.append(String.format(format, samples.getDouble(disp + j)));
                if (j == vectorLength - 1) {
                    sb.append(")");
                }
            }
        }
        return sb.toString();
    }

    UpdatablePArray v(long index) {
        return samples.subArr(index * vectorStep, vectorLength);
    }

    static class CommonRealVectorSampleArray extends RealVectorSampleArray {
        final MemoryModel mm;
        CommonRealVectorSampleArray(
            MemoryModel memoryModel,
            UpdatablePNumberArray samples,
            long vectorLength, long vectorStep, long length)
        {
            super(samples, vectorLength, vectorStep, length);
            assert memoryModel != null;
            this.mm = memoryModel;
        }

        public RealVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength) {
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            }
            return new CommonRealVectorSampleArray(mm,
                (UpdatablePNumberArray) mm.newUnresizableArray(samples.elementType(), length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonRealVectorSampleArray a = (CommonRealVectorSampleArray)src;
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, v(destIndex), a.v(srcIndex1), a.v(srcIndex2));
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonRealVectorSampleArray a = (CommonRealVectorSampleArray)src;
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, v(destIndex), a.v(srcIndex1), a.v(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonRealVectorSampleArray a2 = (CommonRealVectorSampleArray)src2;
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, v(destIndex), v(srcIndex1), a2.v(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonRealVectorSampleArray a2 = (CommonRealVectorSampleArray)src2;
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, v(destIndex), v(srcIndex1), a2.v(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, v(destIndex), v(srcIndex1), v(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, v(destIndex), v(srcIndex1), v(srcIndex2));
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            CommonRealVectorSampleArray a = (CommonRealVectorSampleArray)src;
            PArray v = a.v(srcIndex);
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, aRe), v(destIndex), v);
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, a1, a2),
                v(destIndex), v(srcIndex1), v(srcIndex2));
        }

        public void multiplyByRealScalar(long index, double a) {
            UpdatablePArray v = v(index);
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, a), v, v);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (long index = fromIndex; index < toIndex; index++) {
                multiplyByRealScalar(index, a);
            }
        }
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           FLOAT ==> DOUBLE;;
    //           \(double\)\(([^)]+)\) ==> $1 ]]
    static class RealFloatVectorSampleArray extends RealVectorSampleArray {
        final int vectorLen;
        RealFloatVectorSampleArray(UpdatablePNumberArray samples,  long vectorLength, long vectorStep, long length) {
            super(samples, vectorLength, vectorStep, length);
            assert length <= BUFFER_LENGTH;
            vectorLen = (int)vectorLength;
        }

        public RealVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength) {
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            }
            return new RealFloatVectorSampleArray(
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samples.elementType(), length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            RealFloatVectorSampleArray a = (RealFloatVectorSampleArray)src;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                a.samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] += buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            RealFloatVectorSampleArray a = (RealFloatVectorSampleArray)src;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                a.samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] -= buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            RealFloatVectorSampleArray a2 = (RealFloatVectorSampleArray)src2;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a2.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] += buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            RealFloatVectorSampleArray a2 = (RealFloatVectorSampleArray)src2;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a2.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] -= buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] += buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] -= buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            RealFloatVectorSampleArray a = (RealFloatVectorSampleArray)src;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                a.samples.getData(srcIndex * vectorStep, buf, 0, vectorLen);
                for (int i = 0; i < vectorLen; i++) {
                    buf[i] *= aRe;
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samples.getData(index * vectorStep, buf, 0, vectorLen);
                for (int i = 0; i < vectorLen; i++) {
                    buf[i] *= a;
                }
                samples.setData(index * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] = (float)(buf[i] * a1 + buf[j] * a2);
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                for (long index = fromIndex; index < toIndex; index++) {
                    samples.getData(index * vectorStep, buf, 0, vectorLen);
                    for (int i = 0; i < vectorLen; i++) {
                        buf[i] *= a;
                    }
                    samples.setData(index * vectorStep, buf, 0, vectorLen);
                }
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class RealDoubleVectorSampleArray extends RealVectorSampleArray {
        final int vectorLen;
        RealDoubleVectorSampleArray(UpdatablePNumberArray samples,  long vectorLength, long vectorStep, long length) {
            super(samples, vectorLength, vectorStep, length);
            assert length <= BUFFER_LENGTH;
            vectorLen = (int)vectorLength;
        }

        public RealVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength) {
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            }
            return new RealDoubleVectorSampleArray(
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samples.elementType(), length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            RealDoubleVectorSampleArray a = (RealDoubleVectorSampleArray)src;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                a.samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] += buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            RealDoubleVectorSampleArray a = (RealDoubleVectorSampleArray)src;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                a.samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] -= buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            RealDoubleVectorSampleArray a2 = (RealDoubleVectorSampleArray)src2;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a2.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] += buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            RealDoubleVectorSampleArray a2 = (RealDoubleVectorSampleArray)src2;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a2.samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] -= buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] += buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] -= buf[j];
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            RealDoubleVectorSampleArray a = (RealDoubleVectorSampleArray)src;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                a.samples.getData(srcIndex * vectorStep, buf, 0, vectorLen);
                for (int i = 0; i < vectorLen; i++) {
                    buf[i] *= aRe;
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samples.getData(index * vectorStep, buf, 0, vectorLen);
                for (int i = 0; i < vectorLen; i++) {
                    buf[i] *= a;
                }
                samples.setData(index * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samples.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samples.getData(srcIndex2 * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    buf[i] = buf[i] * a1 + buf[j] * a2;
                }
                samples.setData(destIndex * vectorStep, buf, 0, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                for (long index = fromIndex; index < toIndex; index++) {
                    samples.getData(index * vectorStep, buf, 0, vectorLen);
                    for (int i = 0; i < vectorLen; i++) {
                        buf[i] *= a;
                    }
                    samples.setData(index * vectorStep, buf, 0, vectorLen);
                }
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }
    }
    //[[Repeat.AutoGeneratedEnd]]

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           FLOAT ==> DOUBLE;;
    //           \(double\)\(([^)]+)\) ==> $1 ]]
    static class DirectRealFloatVectorSampleArray extends RealVectorSampleArray {
        final float[] samples;
        final int ofs;
        final int vectorLen;
        DirectRealFloatVectorSampleArray(UpdatablePNumberArray samples,
            long vectorLength, long vectorStep, long length)
        {
            super(samples, vectorLength, vectorStep, length);
            DirectAccessible da = (DirectAccessible)super.samples;
            this.samples = (float[])da.javaArray();
            this.ofs = da.javaArrayOffset();
            assert length <= Integer.MAX_VALUE;
            this.vectorLen = (int)vectorLength;
        }

        public DirectRealFloatVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength) {
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            }
            return new DirectRealFloatVectorSampleArray(
                Arrays.SMM.newUnresizableFloatArray(length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealFloatVectorSampleArray a = (DirectRealFloatVectorSampleArray)src;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofs + (int)(srcIndex1 * a.vectorStep),
                j = a.ofs + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = a.samples[i] + a.samples[j];
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealFloatVectorSampleArray a = (DirectRealFloatVectorSampleArray)src;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofs + (int)(srcIndex1 * a.vectorStep),
                j = a.ofs + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = a.samples[i] - a.samples[j];
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealFloatVectorSampleArray a2 = (DirectRealFloatVectorSampleArray)src2;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = a2.ofs + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] + a2.samples[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealFloatVectorSampleArray a2 = (DirectRealFloatVectorSampleArray)src2;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = a2.ofs + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] - a2.samples[j];
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = ofs + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] + samples[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = ofs + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] - samples[j];
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectRealFloatVectorSampleArray a = (DirectRealFloatVectorSampleArray)src;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofs + (int)(srcIndex * a.vectorStep); k < kMax; k++, i++) {
                float v = a.samples[i];
                samples[k] = (float)(v * aRe);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = ofs + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = (float)(a1 * samples[i] + a2 * samples[j]);
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            for (int k = ofs + (int)(index * vectorStep), kMax = k + vectorLen; k < kMax; k++) {
                samples[k] *= a;
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int disp = ofs + (int)(fromIndex * vectorStep);
            int dispMax = disp + (int)((toIndex - fromIndex) * vectorStep);
            for (; disp < dispMax; disp += vectorStep) {
                for (int k = disp, kMax = k + vectorLen; k < kMax; k++) {
                    samples[k] *= a;
                }
            }
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class DirectRealDoubleVectorSampleArray extends RealVectorSampleArray {
        final double[] samples;
        final int ofs;
        final int vectorLen;
        DirectRealDoubleVectorSampleArray(UpdatablePNumberArray samples,
            long vectorLength, long vectorStep, long length)
        {
            super(samples, vectorLength, vectorStep, length);
            DirectAccessible da = (DirectAccessible)super.samples;
            this.samples = (double[])da.javaArray();
            this.ofs = da.javaArrayOffset();
            assert length <= Integer.MAX_VALUE;
            this.vectorLen = (int)vectorLength;
        }

        public DirectRealDoubleVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength) {
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            }
            return new DirectRealDoubleVectorSampleArray(
                Arrays.SMM.newUnresizableDoubleArray(length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealDoubleVectorSampleArray a = (DirectRealDoubleVectorSampleArray)src;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofs + (int)(srcIndex1 * a.vectorStep),
                j = a.ofs + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = a.samples[i] + a.samples[j];
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealDoubleVectorSampleArray a = (DirectRealDoubleVectorSampleArray)src;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofs + (int)(srcIndex1 * a.vectorStep),
                j = a.ofs + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = a.samples[i] - a.samples[j];
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealDoubleVectorSampleArray a2 = (DirectRealDoubleVectorSampleArray)src2;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = a2.ofs + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] + a2.samples[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealDoubleVectorSampleArray a2 = (DirectRealDoubleVectorSampleArray)src2;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = a2.ofs + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] - a2.samples[j];
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = ofs + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] + samples[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = ofs + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = samples[i] - samples[j];
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectRealDoubleVectorSampleArray a = (DirectRealDoubleVectorSampleArray)src;
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofs + (int)(srcIndex * a.vectorStep); k < kMax; k++, i++) {
                double v = a.samples[i];
                samples[k] = v * aRe;
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            for (int k = ofs + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofs + (int)(srcIndex1 * vectorStep),
                j = ofs + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samples[k] = a1 * samples[i] + a2 * samples[j];
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            for (int k = ofs + (int)(index * vectorStep), kMax = k + vectorLen; k < kMax; k++) {
                samples[k] *= a;
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int disp = ofs + (int)(fromIndex * vectorStep);
            int dispMax = disp + (int)((toIndex - fromIndex) * vectorStep);
            for (; disp < dispMax; disp += vectorStep) {
                for (int k = disp, kMax = k + vectorLen; k < kMax; k++) {
                    samples[k] *= a;
                }
            }
        }
    }
    //[[Repeat.AutoGeneratedEnd]]
}
