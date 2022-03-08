/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Array of samples, where each sample is a vector of complex numbers with some fixed length,
 * represented by an array of pairs of <tt>double</tt> values,
 * stored in two AlgART arrays {@link UpdatablePNumberArray}.
 * These vectors correspond to ranges in these arrays, given with some fixed step.</p>
 *
 * <p>Please use {@link #asSampleArray(MemoryModel, UpdatablePNumberArray, UpdatablePNumberArray, long, long, long)}
 * method for creating instances of this class. Please use comments to this method for more details about storing
 * samples in two AlgART arrays.</p>
 *
 * <p>All operations over samples (adding, subtracting, multiplying) are performed via corresponding operations
 * over elements of the AlgART arrays. Elements of these arrays are interpreted as <tt>double</tt> values,
 * as if they are read/written by {@link PArray#getDouble(long)} and {@link UpdatablePArray#setDouble(long, double)}
 * methods. There is the only exception: if the element type of the underlying AlgART arrays
 * is not <tt>float</tt> or <tt>double</tt>, and an operation leads to overflow (for example, we try to multiply
 * a sample by the real scalar <tt>1e10</tt>), then the results can differ from the results of the simplest code based
 * on {@link PArray#getDouble(long)} and {@link UpdatablePArray#setDouble(long, double)} calls.</p>
 *
 * <p>The instances of this class are not thread-safe, but <b>are thread-compatible</b>
 * and can may be synchronized manually if multithread access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class ComplexVectorSampleArray implements SampleArray {

    private static final int BUFFER_LENGTH = 32768; // no reasons to optimize longer vectors
    private static final int NUMBER_OF_BUFFERS = 4; // maximal number of simultaneous buffers in methods below

    private static final JArrayPool FLOAT_BUFFERS =
        JArrayPool.getInstance(float.class, NUMBER_OF_BUFFERS * BUFFER_LENGTH);
    private static final JArrayPool DOUBLE_BUFFERS =
        JArrayPool.getInstance(double.class, NUMBER_OF_BUFFERS * BUFFER_LENGTH);

    final long vectorLength;
    final long vectorStep;
    final long length;
    final UpdatablePNumberArray samplesRe, samplesIm;

    ComplexVectorSampleArray(UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
        long vectorLength, long vectorStep, long length)
    // Besides more clarity, "length" argument allows processing a case vectorLength=vectorStep=0
    {
        if (samplesRe == null)
            throw new NullPointerException("Null samplesRe");
        if (samplesIm == null)
            throw new NullPointerException("Null samplesIm");
        if (vectorLength < 0)
            throw new IllegalArgumentException("Negative vectorLength = " + vectorLength);
        if (vectorStep < vectorLength)
            throw new IllegalArgumentException("vectorStep = "+ vectorStep + " < vectorLength = " + vectorLength);
        if (length < 0)
            throw new IllegalArgumentException("Negative length = " + length);
        long m = samplesRe.length() - vectorLength;
        if ((length > 0 && m < 0) || (vectorStep > 0 && (length - 1) > m / vectorStep))
            throw new IllegalArgumentException("samplesRe is too short: its length " + samplesRe.length()
                + " < (length - 1) * vectorStep + vectorLength = "
                + (length - 1) + " * " + vectorStep + " + " + vectorLength);
        m = samplesIm.length() - vectorLength;
        if ((length > 0 && m < 0) || (vectorStep > 0 && (length - 1) > m / vectorStep))
            throw new IllegalArgumentException("samplesIm is too short: its length " + samplesIm.length()
                + " < (length - 1) * vectorStep + vectorLength = "
                + (length - 1) + " * " + vectorStep + " + " + vectorLength);
        this.samplesRe = samplesRe;
        this.samplesIm = samplesIm;
        this.vectorLength = vectorLength;
        this.vectorStep = vectorStep;
        this.length = length;
    }

    /**
     * Returns a view of the specified pair of AlgART arrays as an array of complex vector samples.
     * Complex vectors are stored in the specified arrays, real parts in <tt>samplesRe</tt>,
     * imaginary parts in <tt>samplesIm</tt>, with step <tt>vectorStep</tt> elements.
     * More precisely, the sample <tt>#k</tt> in the returned sample array is a vector of <tt>vectorLength</tt>
     * complex numbers, where the real part of the number <tt>#j</tt> is stored in the element
     * <tt>#(k*vectorStep)+j</tt> of <tt>samplesRe</tt> array, and its imaginary part is stored in the element
     * <tt>#(k*vectorStep)+j</tt> of <tt>samplesIm</tt> array.
     *
     * <p>The returned sample array is backed by these two arrays, so any changes of the samples
     * in the returned array are reflected in these arrays, and vice-versa.
     * More precisely, the returned sample array is backed by
     * <tt>samplesRe.{@link UpdatableArray#asUnresizable asUnresizable()}</tt> and
     * <tt>samplesIm.{@link UpdatableArray#asUnresizable asUnresizable()}</tt>:
     * if the passed arrays are {@link MutableArray resizable}, posible future changes of their lengths
     * will not affect behaviour of the returned sample array.
     *
     * <p>The length of each vector sample, <tt>vectorLength</tt>, must be in range
     * <tt>0&lt;=vectorLength&lt;=vectorStep</tt>. Moreover, <tt>samplesRe</tt> and <tt>samplesIm</tt>
     * arrays must be long enough for storing <tt>length</tt> vectors with specified <tt>vectorStep</tt>:
     * <nobr><tt>(length-1)*vectorStep+vectorLength &lt;= min(samplesRe.length(),samplesIm.length())</tt></nobr>.
     *
     * @param memoryModel  the memory model, which will be used, when necessary, by
     *                     {@link #newCompatibleSamplesArray(long)} method; may be <tt>null</tt>,
     *                     then {@link SimpleMemoryModel} will be used.
     * @param samplesRe    the real parts of all samples.
     * @param samplesIm    the imaginary parts of all samples.
     * @param vectorLength the length of each complex vector.
     * @param vectorStep   the step of storing vectors in <tt>samplesRe</tt> and <tt>samplesIm</tt> arrays.
     * @param length       the length of the returned sample array.
     * @return             the array of vector complex samples, represented by corresponding ranges (subarrays)
     *                     of these two arrays.
     * @throws NullPointerException     if <tt>samplesRe</tt> or <tt>samplesIm</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>vectorLength&lt;0</tt>, <tt>vectorStep&lt;vectorLength</tt>,
     *                                  <tt>length&lt;0</tt> or
     *                                  <nobr><tt>(length-1)*vectorStep+vectorLength &lt;=
     *                                  min(samplesRe.length(),samplesIm.length())</tt></nobr>
     *                                  (the last condition is checked mathematically accurately even if these
     *                                  values <tt>&gt;Long.MAX_VALUE</tt>).
     * @throws TooLargeArrayException   (little probability)
     *                                  if the {@link MemoryModel#maxSupportedLength(Class) maximal length},
     *                                  supported by the specified memory model
     *                                  (or {@link SimpleMemoryModel} if <tt>memoryModel==null</tt>)
     *                                  is not enough for allocating
     *                                  <tt>{@link #GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH}*vectorLength</tt>
     *                                  elements with the type <tt>samplesRe.elementType()</tt> or
     *                                  <tt>samplesIm.elementType()</tt>.
     */
    public static ComplexVectorSampleArray asSampleArray(
        MemoryModel memoryModel,
        UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
        long vectorLength, long vectorStep, long length)
    {
        if (samplesRe == null)
            throw new NullPointerException("Null samplesRe");
        if (samplesIm == null)
            throw new NullPointerException("Null samplesIm");
        samplesRe = (UpdatablePNumberArray)samplesRe.asUnresizable(); // to be sure that its length will not be changed
        samplesIm = (UpdatablePNumberArray)samplesIm.asUnresizable(); // to be sure that its length will not be changed
        if (samplesRe instanceof DirectAccessible && ((DirectAccessible) samplesRe).hasJavaArray()
            && samplesIm instanceof DirectAccessible && ((DirectAccessible) samplesIm).hasJavaArray()
            && vectorLength <= Arrays.SMM.maxSupportedLength(samplesRe.elementType())
            / GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH)
        {
            if (samplesRe instanceof FloatArray && samplesIm instanceof FloatArray) {
                return new DirectComplexFloatVectorSampleArray(samplesRe, samplesIm,
                    vectorLength, vectorStep, length);
            }
            if (samplesRe instanceof DoubleArray && samplesIm instanceof DoubleArray) {
                return new DirectComplexDoubleVectorSampleArray(samplesRe, samplesIm,
                    vectorLength, vectorStep, length);
            }
        }
        if (vectorLength < BUFFER_LENGTH) {
            if (samplesRe instanceof FloatArray && samplesIm instanceof FloatArray) {
                return new ComplexFloatVectorSampleArray(samplesRe, samplesIm, vectorLength, vectorStep, length);
            }
            if (samplesRe instanceof DoubleArray && samplesIm instanceof DoubleArray) {
                return new ComplexDoubleVectorSampleArray(samplesRe, samplesIm, vectorLength, vectorStep, length);
            }
        }
        if (memoryModel == null) {
            memoryModel = Arrays.SMM;
        }
        if (vectorLength > Math.min(
            memoryModel.maxSupportedLength(samplesRe.elementType()),
            memoryModel.maxSupportedLength(samplesIm.elementType()))
            / GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH)
            throw new TooLargeArrayException("Too large samples for the given memory model " + memoryModel
                + ": it cannot allocate " + GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH
                + " samples (each sample is a vector of " + vectorLength + " numbers");
        return new CommonComplexVectorSampleArray(memoryModel, samplesRe, samplesIm, vectorLength, vectorStep, length);
    }

    public final boolean isComplex() {
        return true;
    }

    public final long length() {
        return length;
    }

    public abstract ComplexVectorSampleArray newCompatibleSamplesArray(long length);

    public void copy(long destIndex, SampleArray src, long srcIndex) {
        ComplexVectorSampleArray a = (ComplexVectorSampleArray)src;
        re(destIndex).copy(a.re(srcIndex));
        im(destIndex).copy(a.im(srcIndex));
    }

    public void swap(long firstIndex, long secondIndex) {
        samplesRe.swap(firstIndex * vectorStep, secondIndex * vectorStep, vectorLength);
        samplesIm.swap(firstIndex * vectorStep, secondIndex * vectorStep, vectorLength);
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
        if (format == null)
            throw new NullPointerException("Null format argument");
        if (separator == null)
            throw new NullPointerException("Null separator argument");
        if (maxStringLength <= 0)
            throw new IllegalArgumentException("maxStringLength argument must be positive");
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        format = "(" + format + " " + format + ")";
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
                sb.append(String.format(format, samplesRe.getDouble(disp + j), samplesIm.getDouble(disp + j)));
                if (j == vectorLength - 1) {
                    sb.append(")");
                }
            }
        }
        return sb.toString();
    }

    UpdatablePArray re(long index) {
        return samplesRe.subArr(index * vectorStep, vectorLength);
    }

    UpdatablePArray im(long index) {
        return samplesIm.subArr(index * vectorStep, vectorLength);
    }

    static class CommonComplexVectorSampleArray extends ComplexVectorSampleArray {
        final MemoryModel mm;
        CommonComplexVectorSampleArray(
            MemoryModel memoryModel,
            UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
            long vectorLength, long vectorStep, long length)
        {
            super(samplesRe, samplesIm, vectorLength, vectorStep, length);
            assert memoryModel != null;
            this.mm = memoryModel;
        }

        public ComplexVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength)
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            return new CommonComplexVectorSampleArray(mm,
                (UpdatablePNumberArray) mm.newUnresizableArray(samplesRe.elementType(), length * vectorLength),
                (UpdatablePNumberArray) mm.newUnresizableArray(samplesIm.elementType(), length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonComplexVectorSampleArray a = (CommonComplexVectorSampleArray)src;
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, re(destIndex), a.re(srcIndex1), a.re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, im(destIndex), a.im(srcIndex1), a.im(srcIndex2));
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonComplexVectorSampleArray a = (CommonComplexVectorSampleArray)src;
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, re(destIndex), a.re(srcIndex1), a.re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, im(destIndex), a.im(srcIndex1), a.im(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonComplexVectorSampleArray a2 = (CommonComplexVectorSampleArray)src2;
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, re(destIndex), re(srcIndex1), a2.re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, im(destIndex), im(srcIndex1), a2.im(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonComplexVectorSampleArray a2 = (CommonComplexVectorSampleArray)src2;
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, re(destIndex), re(srcIndex1), a2.re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, im(destIndex), im(srcIndex1), a2.im(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, re(destIndex), re(srcIndex1), re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, im(destIndex), im(srcIndex1), im(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, re(destIndex), re(srcIndex1), re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, im(destIndex), im(srcIndex1), im(srcIndex2));
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            CommonComplexVectorSampleArray a = (CommonComplexVectorSampleArray)src;
            PArray re = a.re(srcIndex);
            PArray im = a.im(srcIndex);
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, aRe, -aIm), re(destIndex), re, im);
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, aIm, aRe), im(destIndex), re, im);
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, a1, a2),
                re(destIndex), re(srcIndex1), re(srcIndex2));
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, a1, a2),
                im(destIndex), im(srcIndex1), im(srcIndex2));
        }

        public void multiplyByRealScalar(long index, double a) {
            UpdatablePArray re = re(index);
            UpdatablePArray im = im(index);
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, a), re, re);
            Arrays.applyFunc(null, false, 1, true, LinearFunc.getInstance(0.0, a), im, im);
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
    static class ComplexFloatVectorSampleArray extends ComplexVectorSampleArray {
        final int vectorLen;
        final int vectorLen2;
        ComplexFloatVectorSampleArray(UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
            long vectorLength, long vectorStep, long length)
        {
            super(samplesRe, samplesIm, vectorLength, vectorStep, length);
            assert length <= BUFFER_LENGTH;
            vectorLen = (int)vectorLength;
            vectorLen2 = 2 * vectorLen;
        }

        public ComplexVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength)
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            return new ComplexFloatVectorSampleArray(
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samplesRe.elementType(), length * vectorLength),
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samplesIm.elementType(), length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            ComplexFloatVectorSampleArray a = (ComplexFloatVectorSampleArray)src;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                a.samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] += buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            ComplexFloatVectorSampleArray a = (ComplexFloatVectorSampleArray)src;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                a.samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] -= buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            ComplexFloatVectorSampleArray a2 = (ComplexFloatVectorSampleArray)src2;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a2.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a2.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] += buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            ComplexFloatVectorSampleArray a2 = (ComplexFloatVectorSampleArray)src2;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a2.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a2.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] -= buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] += buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] -= buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            ComplexFloatVectorSampleArray a = (ComplexFloatVectorSampleArray)src;
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                a.samplesRe.getData(srcIndex * vectorStep, buf, 0, vectorLen);
                a.samplesIm.getData(srcIndex * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    float re = (float)(buf[i] * aRe - buf[j] * aIm);
                    float im = (float)(buf[i] * aIm + buf[j] * aRe);
                    buf[i] = re;
                    buf[j] = im;
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samplesRe.getData(index * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(index * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0; i < vectorLen2; i++) {
                    buf[i] *= a;
                }
                samplesRe.setData(index * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(index * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] = (float)(buf[i] * a1 + buf[j] * a2);
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            float[] buf = null;
            try {
                buf = (float[])FLOAT_BUFFERS.requestArray();
                for (long index = fromIndex; index < toIndex; index++) {
                    samplesRe.getData(index * vectorStep, buf, 0, vectorLen);
                    samplesIm.getData(index * vectorStep, buf, vectorLen, vectorLen);
                    for (int i = 0; i < vectorLen2; i++) {
                        buf[i] *= a;
                    }
                    samplesRe.setData(index * vectorStep, buf, 0, vectorLen);
                    samplesIm.setData(index * vectorStep, buf, vectorLen, vectorLen);
                }
            } finally {
                FLOAT_BUFFERS.releaseArray(buf);
            }
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class ComplexDoubleVectorSampleArray extends ComplexVectorSampleArray {
        final int vectorLen;
        final int vectorLen2;
        ComplexDoubleVectorSampleArray(UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
            long vectorLength, long vectorStep, long length)
        {
            super(samplesRe, samplesIm, vectorLength, vectorStep, length);
            assert length <= BUFFER_LENGTH;
            vectorLen = (int)vectorLength;
            vectorLen2 = 2 * vectorLen;
        }

        public ComplexVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength)
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            return new ComplexDoubleVectorSampleArray(
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samplesRe.elementType(), length * vectorLength),
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samplesIm.elementType(), length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            ComplexDoubleVectorSampleArray a = (ComplexDoubleVectorSampleArray)src;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                a.samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] += buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            ComplexDoubleVectorSampleArray a = (ComplexDoubleVectorSampleArray)src;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                a.samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                a.samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] -= buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            ComplexDoubleVectorSampleArray a2 = (ComplexDoubleVectorSampleArray)src2;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a2.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a2.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] += buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            ComplexDoubleVectorSampleArray a2 = (ComplexDoubleVectorSampleArray)src2;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                a2.samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                a2.samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] -= buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] += buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] -= buf[j];
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            ComplexDoubleVectorSampleArray a = (ComplexDoubleVectorSampleArray)src;
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                a.samplesRe.getData(srcIndex * vectorStep, buf, 0, vectorLen);
                a.samplesIm.getData(srcIndex * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0, j = vectorLen; i < vectorLen; i++, j++) {
                    double re = buf[i] * aRe - buf[j] * aIm;
                    double im = buf[i] * aIm + buf[j] * aRe;
                    buf[i] = re;
                    buf[j] = im;
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samplesRe.getData(index * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(index * vectorStep, buf, vectorLen, vectorLen);
                for (int i = 0; i < vectorLen2; i++) {
                    buf[i] *= a;
                }
                samplesRe.setData(index * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(index * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                samplesRe.getData(srcIndex1 * vectorStep, buf, 0, vectorLen);
                samplesIm.getData(srcIndex1 * vectorStep, buf, vectorLen, vectorLen);
                samplesRe.getData(srcIndex2 * vectorStep, buf, 2 * vectorLen, vectorLen);
                samplesIm.getData(srcIndex2 * vectorStep, buf, 3 * vectorLen, vectorLen);
                for (int i = 0, j = vectorLen2; i < vectorLen2; i++, j++) {
                    buf[i] = buf[i] * a1 + buf[j] * a2;
                }
                samplesRe.setData(destIndex * vectorStep, buf, 0, vectorLen);
                samplesIm.setData(destIndex * vectorStep, buf, vectorLen, vectorLen);
            } finally {
                DOUBLE_BUFFERS.releaseArray(buf);
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            double[] buf = null;
            try {
                buf = (double[])DOUBLE_BUFFERS.requestArray();
                for (long index = fromIndex; index < toIndex; index++) {
                    samplesRe.getData(index * vectorStep, buf, 0, vectorLen);
                    samplesIm.getData(index * vectorStep, buf, vectorLen, vectorLen);
                    for (int i = 0; i < vectorLen2; i++) {
                        buf[i] *= a;
                    }
                    samplesRe.setData(index * vectorStep, buf, 0, vectorLen);
                    samplesIm.setData(index * vectorStep, buf, vectorLen, vectorLen);
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
    static class DirectComplexFloatVectorSampleArray extends ComplexVectorSampleArray {
        final float[] samplesRe;
        final int ofsRe;
        final float[] samplesIm;
        final int ofsIm;
        final int vectorLen;
        // ofsRe==ofsIm can lead to simpler code, but it will be anti-optimization:
        // a single array is processed faster than two "parallel" arrays due to better CPU cache usage
        DirectComplexFloatVectorSampleArray(UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
            long vectorLength, long vectorStep, long length)
        {
            super(samplesRe, samplesIm, vectorLength, vectorStep, length);
            DirectAccessible daRe = (DirectAccessible)super.samplesRe;
            this.samplesRe = (float[])daRe.javaArray();
            this.ofsRe = daRe.javaArrayOffset();
            DirectAccessible daIm = (DirectAccessible)super.samplesIm;
            this.samplesIm = (float[])daIm.javaArray();
            this.ofsIm = daIm.javaArrayOffset();
            assert length <= Integer.MAX_VALUE;
            this.vectorLen = (int)vectorLength;
        }

        public ComplexVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength)
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            return new DirectComplexFloatVectorSampleArray(
                Arrays.SMM.newUnresizableFloatArray(length * vectorLength),
                Arrays.SMM.newUnresizableFloatArray(length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexFloatVectorSampleArray a = (DirectComplexFloatVectorSampleArray)src;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsRe + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsRe + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = a.samplesRe[i] + a.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsIm + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsIm + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = a.samplesIm[i] + a.samplesIm[j];
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexFloatVectorSampleArray a = (DirectComplexFloatVectorSampleArray)src;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsRe + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsRe + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = a.samplesRe[i] - a.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsIm + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsIm + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = a.samplesIm[i] - a.samplesIm[j];
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexFloatVectorSampleArray a2 = (DirectComplexFloatVectorSampleArray)src2;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = a2.ofsRe + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] + a2.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = a2.ofsIm + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] + a2.samplesIm[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexFloatVectorSampleArray a2 = (DirectComplexFloatVectorSampleArray)src2;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = a2.ofsRe + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] - a2.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = a2.ofsIm + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] - a2.samplesIm[j];
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = ofsRe + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] + samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = ofsIm + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] + samplesIm[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = ofsRe + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] - samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = ofsIm + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] - samplesIm[j];
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectComplexFloatVectorSampleArray a = (DirectComplexFloatVectorSampleArray)src;
            for (int kRe = ofsRe + (int)(destIndex * vectorStep), kReMax = kRe + vectorLen,
                kIm = ofsIm + (int)(destIndex * vectorStep),
                iRe = a.ofsRe + (int)(srcIndex * a.vectorStep),
                iIm = a.ofsIm + (int)(srcIndex * a.vectorStep);
                 kRe < kReMax; kRe++, kIm++, iRe++, iIm++) {
                float re = a.samplesRe[iRe];
                float im = a.samplesIm[iIm];
                samplesRe[kRe] = (float)(re * aRe - im * aIm);
                samplesIm[kIm] = (float)(re * aIm + im * aRe);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = ofsRe + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = (float)(a1 * samplesRe[i] + a2 * samplesRe[j]);
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = ofsIm + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = (float)(a1 * samplesIm[i] + a2 * samplesIm[j]);
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            for (int k = ofsRe + (int)(index * vectorStep), kMax = k + vectorLen; k < kMax; k++) {
                samplesRe[k] *= a;
            }
            for (int k = ofsIm + (int)(index * vectorStep), kMax = k + vectorLen; k < kMax; k++) {
                samplesIm[k] *= a;
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int dispRe = ofsRe + (int)(fromIndex * vectorStep);
            int dispIm = ofsIm + (int)(fromIndex * vectorStep);
            int dispReMax = dispRe + (int)((toIndex - fromIndex) * vectorStep);
            for (; dispRe < dispReMax; dispRe += vectorStep, dispIm += vectorStep) {
                for (int k = dispRe, kMax = k + vectorLen; k < kMax; k++) {
                    samplesRe[k] *= a;
                }
                for (int k = dispIm, kMax = k + vectorLen; k < kMax; k++) {
                    samplesIm[k] *= a;
                }
            }
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class DirectComplexDoubleVectorSampleArray extends ComplexVectorSampleArray {
        final double[] samplesRe;
        final int ofsRe;
        final double[] samplesIm;
        final int ofsIm;
        final int vectorLen;
        // ofsRe==ofsIm can lead to simpler code, but it will be anti-optimization:
        // a single array is processed faster than two "parallel" arrays due to better CPU cache usage
        DirectComplexDoubleVectorSampleArray(UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm,
            long vectorLength, long vectorStep, long length)
        {
            super(samplesRe, samplesIm, vectorLength, vectorStep, length);
            DirectAccessible daRe = (DirectAccessible)super.samplesRe;
            this.samplesRe = (double[])daRe.javaArray();
            this.ofsRe = daRe.javaArrayOffset();
            DirectAccessible daIm = (DirectAccessible)super.samplesIm;
            this.samplesIm = (double[])daIm.javaArray();
            this.ofsIm = daIm.javaArrayOffset();
            assert length <= Integer.MAX_VALUE;
            this.vectorLen = (int)vectorLength;
        }

        public ComplexVectorSampleArray newCompatibleSamplesArray(long length) {
            if (length > Long.MAX_VALUE / vectorLength)
                throw new TooLargeArrayException("Too large sample array: "
                    + length + " vectors of " + vectorLength + " numbers");
            return new DirectComplexDoubleVectorSampleArray(
                Arrays.SMM.newUnresizableDoubleArray(length * vectorLength),
                Arrays.SMM.newUnresizableDoubleArray(length * vectorLength),
                vectorLength, vectorLength, length);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexDoubleVectorSampleArray a = (DirectComplexDoubleVectorSampleArray)src;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsRe + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsRe + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = a.samplesRe[i] + a.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsIm + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsIm + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = a.samplesIm[i] + a.samplesIm[j];
            }
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexDoubleVectorSampleArray a = (DirectComplexDoubleVectorSampleArray)src;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsRe + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsRe + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = a.samplesRe[i] - a.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = a.ofsIm + (int)(srcIndex1 * a.vectorStep),
                j = a.ofsIm + (int)(srcIndex2 * a.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = a.samplesIm[i] - a.samplesIm[j];
            }
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexDoubleVectorSampleArray a2 = (DirectComplexDoubleVectorSampleArray)src2;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = a2.ofsRe + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] + a2.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = a2.ofsIm + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] + a2.samplesIm[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexDoubleVectorSampleArray a2 = (DirectComplexDoubleVectorSampleArray)src2;
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = a2.ofsRe + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] - a2.samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = a2.ofsIm + (int)(srcIndex2 * a2.vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] - a2.samplesIm[j];
            }
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = ofsRe + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] + samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = ofsIm + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] + samplesIm[j];
            }
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = ofsRe + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = samplesRe[i] - samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = ofsIm + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = samplesIm[i] - samplesIm[j];
            }
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectComplexDoubleVectorSampleArray a = (DirectComplexDoubleVectorSampleArray)src;
            for (int kRe = ofsRe + (int)(destIndex * vectorStep), kReMax = kRe + vectorLen,
                kIm = ofsIm + (int)(destIndex * vectorStep),
                iRe = a.ofsRe + (int)(srcIndex * a.vectorStep),
                iIm = a.ofsIm + (int)(srcIndex * a.vectorStep);
                 kRe < kReMax; kRe++, kIm++, iRe++, iIm++) {
                double re = a.samplesRe[iRe];
                double im = a.samplesIm[iIm];
                samplesRe[kRe] = re * aRe - im * aIm;
                samplesIm[kIm] = re * aIm + im * aRe;
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            for (int k = ofsRe + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsRe + (int)(srcIndex1 * vectorStep),
                j = ofsRe + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesRe[k] = a1 * samplesRe[i] + a2 * samplesRe[j];
            }
            for (int k = ofsIm + (int)(destIndex * vectorStep), kMax = k + vectorLen,
                i = ofsIm + (int)(srcIndex1 * vectorStep),
                j = ofsIm + (int)(srcIndex2 * vectorStep);
                 k < kMax; k++, i++, j++) {
                samplesIm[k] = a1 * samplesIm[i] + a2 * samplesIm[j];
            }
        }

        public void multiplyByRealScalar(long index, double a) {
            for (int k = ofsRe + (int)(index * vectorStep), kMax = k + vectorLen; k < kMax; k++) {
                samplesRe[k] *= a;
            }
            for (int k = ofsIm + (int)(index * vectorStep), kMax = k + vectorLen; k < kMax; k++) {
                samplesIm[k] *= a;
            }
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int dispRe = ofsRe + (int)(fromIndex * vectorStep);
            int dispIm = ofsIm + (int)(fromIndex * vectorStep);
            int dispReMax = dispRe + (int)((toIndex - fromIndex) * vectorStep);
            for (; dispRe < dispReMax; dispRe += vectorStep, dispIm += vectorStep) {
                for (int k = dispRe, kMax = k + vectorLen; k < kMax; k++) {
                    samplesRe[k] *= a;
                }
                for (int k = dispIm, kMax = k + vectorLen; k < kMax; k++) {
                    samplesIm[k] *= a;
                }
            }
        }
    }
    //[[Repeat.AutoGeneratedEnd]]
}
