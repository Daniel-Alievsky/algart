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

package net.algart.matrices.spectra;

import net.algart.arrays.*;

/**
 * <p>Array of samples, where each sample is a complex number, represented by a pair of <tt>double</tt> values,
 * stored in two AlgART arrays {@link UpdatablePNumberArray}.</p>
 *
 * <p>Please use {@link #asSampleArray(UpdatablePNumberArray, UpdatablePNumberArray)} method for
 * creating instances of this class.</p>
 *
 * <p>All operations over samples (adding, subtracting, multiplying) are performed via corresponding operations
 * over elements of the AlgART arrays. Elements of these arrays are interpreted as <tt>double</tt> values,
 * as if they are read/written by {@link PArray#getDouble(long)} and {@link UpdatablePArray#setDouble(long, double)}
 * methods.</p>
 *
 * <p>The instances of this class are not thread-safe, but <b>are thread-compatible</b>
 * and can may be synchronized manually if multithread access is necessary.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class ComplexScalarSampleArray implements SampleArray {

    /**
     * Returns a view of the specified pair of AlgART arrays as an array of scalar complex samples.
     * More precisely, the real part of the sample <tt>#k</tt> in the returned sample array is stored
     * in the element <tt>#k</tt> of <tt>samplesRe</tt> array, and the imaginary part of
     * the sample <tt>#k</tt> in the returned sample array is stored in the element <tt>#k</tt>
     * of <tt>samplesIm</tt> array.
     *
     * <p>The returned sample array is backed by these two arrays, so any changes of the samples
     * in the returned array are reflected in these arrays, and vice-versa.
     * More precisely, the returned sample array is backed by
     * <tt>samplesRe.{@link UpdatableArray#asUnresizable asUnresizable()}</tt> and
     * <tt>samplesIm.{@link UpdatableArray#asUnresizable asUnresizable()}</tt>:
     * if the passed arrays are {@link MutableArray resizable}, posible future changes of their lengths
     * will not affect behaviour of the returned sample array.
     *
     * <p>The {@link #length() length} of the returned sample array is equal to lengths of <tt>samplesRe</tt>
     * and <tt>samplesIm</tt> arrays. (Their lengths must be equal to each other.)
     *
     * <p>This method detects, if the passed arrays are {@link DirectAccessible}
     * (i.e. the data are stored in usual Java arrays), and provides optimized implementations for this case.
     *
     * @param samplesRe the real parts of all samples.
     * @param samplesIm the imaginary parts of all samples.
     * @return          the array of scalar complex samples, represented by these two arrays.
     * @throws NullPointerException  if <tt>samplesRe</tt> or <tt>samplesIm</tt> is <tt>null</tt>.
     * @throws SizeMismatchException if <tt>samplesRe.length() != samplesIm.length()</tt>.
     */
    public static ComplexScalarSampleArray asSampleArray(
        UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm)
    {
        if (samplesRe == null)
            throw new NullPointerException("Null samplesRe");
        if (samplesIm == null)
            throw new NullPointerException("Null samplesIm");
        samplesRe = (UpdatablePNumberArray)samplesRe.asUnresizable(); // to be sure that its length will not be changed
        samplesIm = (UpdatablePNumberArray)samplesIm.asUnresizable(); // to be sure that its length will not be changed
        if (samplesRe.length() != samplesIm.length())
            throw new SizeMismatchException("Different lengths of samplesRe and samplesIm");
        if (samplesRe instanceof DirectAccessible && ((DirectAccessible)samplesRe).hasJavaArray()
            && samplesIm instanceof DirectAccessible && ((DirectAccessible)samplesIm).hasJavaArray())
        {
            Object arrRe = ((DirectAccessible)samplesRe).javaArray();
            int ofsRe = ((DirectAccessible)samplesRe).javaArrayOffset();
            Object arrIm = ((DirectAccessible)samplesIm).javaArray();
            int ofsIm = ((DirectAccessible)samplesIm).javaArrayOffset();
            if (arrRe instanceof float[] && arrIm instanceof float[]) {
                if (ofsRe == 0 && ofsIm == 0) {
                    return new DirectZeroOffsetsComplexFloatSampleArray(
                        (float[])arrRe, (float[])arrIm, (int)samplesRe.length());
                } else {
                    return new DirectComplexFloatSampleArray(
                        (float[])arrRe, ofsRe, (float[])arrIm, ofsIm, (int)samplesRe.length());
                }
            }
            if (arrRe instanceof double[] && arrIm instanceof double[]) {
                if (ofsRe == 0 && ofsIm == 0) {
                    return new DirectZeroOffsetsComplexDoubleSampleArray(
                        (double[])arrRe, (double[])arrIm, (int)samplesRe.length());
                } else {
                    return new DirectComplexDoubleSampleArray(
                        (double[])arrRe, ofsRe, (double[])arrIm, ofsIm, (int)samplesRe.length());
                }
            }
        }
        return new CommonComplexScalarSampleArray(samplesRe, samplesIm);
    }

    public final boolean isComplex() {
        return true;
    }

    public abstract long length();

    public abstract ComplexScalarSampleArray newCompatibleSamplesArray(long length);

    public abstract void copy(long destIndex, SampleArray src, long srcIndex);

    public abstract void swap(long firstIndex, long secondIndex);

    public abstract void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2);

    public abstract void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2);

    public abstract void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2);

    public abstract void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2);

    public abstract void add(long destIndex, long srcIndex1, long srcIndex2);

    public abstract void sub(long destIndex, long srcIndex1, long srcIndex2);

    public abstract void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm);

    public abstract void multiplyByRealScalar(long index, double a);

    public abstract void combineWithRealMultipliers(long destIndex,
        long srcIndex1, double a1, long srcIndex2, double a2);

    public abstract void multiplyRangeByRealScalar(long from, long to, double a);

    public abstract String toString(String format, String separator, int maxStringLength);

    static final class CommonComplexScalarSampleArray extends ComplexScalarSampleArray {
        private final UpdatablePNumberArray samplesRe, samplesIm;

        CommonComplexScalarSampleArray(UpdatablePNumberArray samplesRe, UpdatablePNumberArray samplesIm) {
            if (samplesRe == null)
                throw new NullPointerException("Null samplesRe");
            if (samplesIm == null)
                throw new NullPointerException("Null samplesIm");
            if (samplesRe.length() != samplesIm.length())
                throw new IllegalArgumentException("Different lengths of samplesRe and samplesIm");
            this.samplesRe = samplesRe;
            this.samplesIm = samplesIm;
        }

        public long length() {
            return samplesRe.length();
        }

        public ComplexScalarSampleArray newCompatibleSamplesArray(long length) {
            return new CommonComplexScalarSampleArray(
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samplesRe),
                (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samplesIm));
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            CommonComplexScalarSampleArray a = (CommonComplexScalarSampleArray)src;
            samplesRe.setDouble(destIndex, a.samplesRe.getDouble(srcIndex));
            samplesIm.setDouble(destIndex, a.samplesIm.getDouble(srcIndex));
        }

        public void swap(long firstIndex, long secondIndex) {
            samplesRe.swap(firstIndex, secondIndex);
            samplesIm.swap(firstIndex, secondIndex);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonComplexScalarSampleArray a = (CommonComplexScalarSampleArray)src;
            samplesRe.setDouble(destIndex, a.samplesRe.getDouble(srcIndex1) + a.samplesRe.getDouble(srcIndex2));
            samplesIm.setDouble(destIndex, a.samplesIm.getDouble(srcIndex1) + a.samplesIm.getDouble(srcIndex2));
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonComplexScalarSampleArray a = (CommonComplexScalarSampleArray)src;
            samplesRe.setDouble(destIndex, a.samplesRe.getDouble(srcIndex1) - a.samplesRe.getDouble(srcIndex2));
            samplesIm.setDouble(destIndex, a.samplesIm.getDouble(srcIndex1) - a.samplesIm.getDouble(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonComplexScalarSampleArray a2 = (CommonComplexScalarSampleArray)src2;
            samplesRe.setDouble(destIndex, samplesRe.getDouble(srcIndex1) + a2.samplesRe.getDouble(srcIndex2));
            samplesIm.setDouble(destIndex, samplesIm.getDouble(srcIndex1) + a2.samplesIm.getDouble(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonComplexScalarSampleArray a2 = (CommonComplexScalarSampleArray)src2;
            samplesRe.setDouble(destIndex, samplesRe.getDouble(srcIndex1) - a2.samplesRe.getDouble(srcIndex2));
            samplesIm.setDouble(destIndex, samplesIm.getDouble(srcIndex1) - a2.samplesIm.getDouble(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe.setDouble(destIndex, samplesRe.getDouble(srcIndex1) + samplesRe.getDouble(srcIndex2));
            samplesIm.setDouble(destIndex, samplesIm.getDouble(srcIndex1) + samplesIm.getDouble(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe.setDouble(destIndex, samplesRe.getDouble(srcIndex1) - samplesRe.getDouble(srcIndex2));
            samplesIm.setDouble(destIndex, samplesIm.getDouble(srcIndex1) - samplesIm.getDouble(srcIndex2));
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            CommonComplexScalarSampleArray a = (CommonComplexScalarSampleArray)src;
            double re = a.samplesRe.getDouble(srcIndex);
            double im = a.samplesIm.getDouble(srcIndex);
            samplesRe.setDouble(destIndex, re * aRe - im * aIm);
            samplesIm.setDouble(destIndex, re * aIm + im * aRe);
        }

        public void multiplyByRealScalar(long index, double a) {
            samplesRe.setDouble(index, samplesRe.getDouble(index) * a);
            samplesIm.setDouble(index, samplesIm.getDouble(index) * a);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (long index = fromIndex; index < toIndex; index++) {
                samplesRe.setDouble(index, samplesRe.getDouble(index) * a);
                samplesIm.setDouble(index, samplesIm.getDouble(index) * a);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            samplesRe.setDouble(destIndex, samplesRe.getDouble(srcIndex1) * a1 + samplesRe.getDouble(srcIndex2) * a2);
            samplesIm.setDouble(destIndex, samplesIm.getDouble(srcIndex1) * a1 + samplesIm.getDouble(srcIndex2) * a2);
        }

        public String toString(String format, String separator, int maxStringLength) {
            if (format == null)
                throw new NullPointerException("Null format argument");
            if (separator == null)
                throw new NullPointerException("Null separator argument");
            if (maxStringLength <= 0)
                throw new IllegalArgumentException("maxStringLength argument must be positive");
            final long n = samplesRe.length();
            if (n == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            format = "(" + format + " " + format + ")";
            sb.append(String.format(format, samplesRe.getDouble(0), samplesIm.getDouble(0)));
            for (long k = 1; k < n; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("..."); break;
                }
                sb.append(separator).append(String.format(format, samplesRe.getDouble(k), samplesIm.getDouble(k)));
            }
            return sb.toString();
        }
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\(([^)]+)\) ==> $1 ]]
    static final class DirectComplexFloatSampleArray extends ComplexScalarSampleArray {
        final float[] samplesRe;
        final int ofsRe;
        final float[] samplesIm;
        final int ofsIm;
        final int length;

        DirectComplexFloatSampleArray(float[] samplesRe, int ofsRe, float[] samplesIm, int ofsIm, int length) {
            assert ofsRe >= 0;
            assert ofsIm >= 0;
            assert length >= 0;
            assert ofsRe <= samplesRe.length - length;
            assert ofsIm <= samplesIm.length - length;
            this.samplesRe = samplesRe;
            this.ofsRe = ofsRe;
            this.samplesIm = samplesIm;
            this.ofsIm = ofsIm;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public ComplexScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0)
                throw new IllegalArgumentException("Negative length");
            if (length > Integer.MAX_VALUE)
                throw new IllegalArgumentException("length must be less than 2^31");
            int len = (int)length;
            return new DirectComplexFloatSampleArray(new float[len], 0, new float[len], 0, len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectComplexFloatSampleArray a = (DirectComplexFloatSampleArray)src;
            samplesRe[ofsRe + (int)destIndex] = a.samplesRe[a.ofsRe + (int)srcIndex];
            samplesIm[ofsIm + (int)destIndex] = a.samplesIm[a.ofsIm + (int)srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            float temp = samplesRe[ofsRe + (int)firstIndex];
            samplesRe[ofsRe + (int)firstIndex] = samplesRe[ofsRe + (int)secondIndex];
            samplesRe[ofsRe + (int)secondIndex] = temp;
            temp = samplesIm[ofsIm + (int)firstIndex];
            samplesIm[ofsIm + (int)firstIndex] = samplesIm[ofsIm + (int)secondIndex];
            samplesIm[ofsIm + (int)secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexFloatSampleArray a = (DirectComplexFloatSampleArray)src;
            samplesRe[ofsRe + (int)destIndex] =
                a.samplesRe[a.ofsRe + (int)srcIndex1] + a.samplesRe[a.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                a.samplesIm[a.ofsIm + (int)srcIndex1] + a.samplesIm[a.ofsIm + (int)srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexFloatSampleArray a = (DirectComplexFloatSampleArray)src;
            samplesRe[ofsRe + (int)destIndex] =
                a.samplesRe[a.ofsRe + (int)srcIndex1] - a.samplesRe[a.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                a.samplesIm[a.ofsIm + (int)srcIndex1] - a.samplesIm[a.ofsIm + (int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexFloatSampleArray a2 = (DirectComplexFloatSampleArray)src2;
            samplesRe[ofsRe + (int)destIndex] =
                samplesRe[ofsRe + (int)srcIndex1] + a2.samplesRe[a2.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                samplesIm[ofsIm + (int)srcIndex1] + a2.samplesIm[a2.ofsIm + (int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexFloatSampleArray a2 = (DirectComplexFloatSampleArray)src2;
            samplesRe[ofsRe + (int)destIndex] =
                samplesRe[ofsRe + (int)srcIndex1] - a2.samplesRe[a2.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                samplesIm[ofsIm + (int)srcIndex1] - a2.samplesIm[a2.ofsIm + (int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[ofsRe + (int)destIndex] = samplesRe[ofsRe + (int)srcIndex1] + samplesRe[ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] = samplesIm[ofsIm + (int)srcIndex1] + samplesIm[ofsIm + (int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[ofsRe + (int)destIndex] = samplesRe[ofsRe + (int)srcIndex1] - samplesRe[ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] = samplesIm[ofsIm + (int)srcIndex1] - samplesIm[ofsIm + (int)srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectComplexFloatSampleArray a = (DirectComplexFloatSampleArray)src;
            double re = a.samplesRe[a.ofsRe + (int)srcIndex];
            double im = a.samplesIm[a.ofsIm + (int)srcIndex];
            samplesRe[ofsRe + (int)destIndex] = (float)(re * aRe - im * aIm);
            samplesIm[ofsIm + (int)destIndex] = (float)(re * aIm + im * aRe);
        }

        public void multiplyByRealScalar(long index, double a) {
            samplesRe[ofsRe + (int)index] *= a;
            samplesIm[ofsIm + (int)index] *= a;
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int)destIndex, srcI1 = (int)srcIndex1, srcI2 = (int)srcIndex2;
            samplesRe[ofsRe + destI] = (float)(samplesRe[ofsRe + srcI1] * a1 + samplesRe[ofsRe + srcI2] * a2);
            samplesIm[ofsIm + destI] = (float)(samplesIm[ofsIm + srcI1] * a1 + samplesIm[ofsIm + srcI2] * a2);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (int indexRe = ofsRe + (int)fromIndex, indexIm = ofsIm + (int)fromIndex,
                indexReMax = indexRe + (int)(toIndex - fromIndex); indexRe < indexReMax; indexRe++, indexIm++)
            {
                samplesRe[indexRe] *= a;
                samplesIm[indexIm] *= a;
            }
        }

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
            sb.append(String.format(format, samplesRe[ofsRe], samplesIm[ofsIm]));
            for (int k = 1; k < length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("..."); break;
                }
                sb.append(separator).append(String.format(format, samplesRe[ofsRe + k], samplesIm[ofsIm + k]));
            }
            return sb.toString();
        }
    }

    static final class DirectZeroOffsetsComplexFloatSampleArray extends ComplexScalarSampleArray {
        final float[] samplesRe;
        final float[] samplesIm;
        final int length;

        DirectZeroOffsetsComplexFloatSampleArray(float[] samplesRe, float[] samplesIm, int length) {
            assert length >= 0;
            assert length <= samplesRe.length;
            assert length <= samplesIm.length;
            this.samplesRe = samplesRe;
            this.samplesIm = samplesIm;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public ComplexScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0)
                throw new IllegalArgumentException("Negative length");
            if (length > Integer.MAX_VALUE)
                throw new IllegalArgumentException("length must be less than 2^31");
            int len = (int)length;
            return new DirectZeroOffsetsComplexFloatSampleArray(new float[len], new float[len], len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectZeroOffsetsComplexFloatSampleArray a = (DirectZeroOffsetsComplexFloatSampleArray)src;
            samplesRe[(int)destIndex] = a.samplesRe[(int)srcIndex];
            samplesIm[(int)destIndex] = a.samplesIm[(int)srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            float temp = samplesRe[(int)firstIndex];
            samplesRe[(int)firstIndex] = samplesRe[(int)secondIndex];
            samplesRe[(int)secondIndex] = temp;
            temp = samplesIm[(int)firstIndex];
            samplesIm[(int)firstIndex] = samplesIm[(int)secondIndex];
            samplesIm[(int)secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsComplexFloatSampleArray a = (DirectZeroOffsetsComplexFloatSampleArray)src;
            samplesRe[(int)destIndex] = a.samplesRe[(int)srcIndex1] + a.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = a.samplesIm[(int)srcIndex1] + a.samplesIm[(int)srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsComplexFloatSampleArray a = (DirectZeroOffsetsComplexFloatSampleArray)src;
            samplesRe[(int)destIndex] = a.samplesRe[(int)srcIndex1] - a.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = a.samplesIm[(int)srcIndex1] - a.samplesIm[(int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsComplexFloatSampleArray a2 = (DirectZeroOffsetsComplexFloatSampleArray)src2;
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] + a2.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] + a2.samplesIm[(int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsComplexFloatSampleArray a2 = (DirectZeroOffsetsComplexFloatSampleArray)src2;
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] - a2.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] - a2.samplesIm[(int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] + samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] + samplesIm[(int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] - samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] - samplesIm[(int)srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectZeroOffsetsComplexFloatSampleArray a = (DirectZeroOffsetsComplexFloatSampleArray)src;
            double re = a.samplesRe[(int)srcIndex];
            double im = a.samplesIm[(int)srcIndex];
            samplesRe[(int)destIndex] = (float)(re * aRe - im * aIm);
            samplesIm[(int)destIndex] = (float)(re * aIm + im * aRe);
        }

        public void multiplyByRealScalar(long index, double a) {
            samplesRe[(int)index] *= a;
            samplesIm[(int)index] *= a;
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int)destIndex, srcI1 = (int)srcIndex1, srcI2 = (int)srcIndex2;
            samplesRe[destI] = (float)(samplesRe[srcI1] * a1 + samplesRe[srcI2] * a2);
            samplesIm[destI] = (float)(samplesIm[srcI1] * a1 + samplesIm[srcI2] * a2);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int from = (int)fromIndex;
            int to = (int)toIndex;
            for (int index = from; index < to; index++) {
                samplesRe[index] *= a;
                samplesIm[index] *= a;
            }
        }

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
            sb.append(String.format(format, samplesRe[0], samplesIm[0]));
            for (int k = 1; k < samplesRe.length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("..."); break;
                }
                sb.append(separator).append(String.format(format, samplesRe[k], samplesIm[k]));
            }
            return sb.toString();
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static final class DirectComplexDoubleSampleArray extends ComplexScalarSampleArray {
        final double[] samplesRe;
        final int ofsRe;
        final double[] samplesIm;
        final int ofsIm;
        final int length;

        DirectComplexDoubleSampleArray(double[] samplesRe, int ofsRe, double[] samplesIm, int ofsIm, int length) {
            assert ofsRe >= 0;
            assert ofsIm >= 0;
            assert length >= 0;
            assert ofsRe <= samplesRe.length - length;
            assert ofsIm <= samplesIm.length - length;
            this.samplesRe = samplesRe;
            this.ofsRe = ofsRe;
            this.samplesIm = samplesIm;
            this.ofsIm = ofsIm;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public ComplexScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0)
                throw new IllegalArgumentException("Negative length");
            if (length > Integer.MAX_VALUE)
                throw new IllegalArgumentException("length must be less than 2^31");
            int len = (int)length;
            return new DirectComplexDoubleSampleArray(new double[len], 0, new double[len], 0, len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectComplexDoubleSampleArray a = (DirectComplexDoubleSampleArray)src;
            samplesRe[ofsRe + (int)destIndex] = a.samplesRe[a.ofsRe + (int)srcIndex];
            samplesIm[ofsIm + (int)destIndex] = a.samplesIm[a.ofsIm + (int)srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            double temp = samplesRe[ofsRe + (int)firstIndex];
            samplesRe[ofsRe + (int)firstIndex] = samplesRe[ofsRe + (int)secondIndex];
            samplesRe[ofsRe + (int)secondIndex] = temp;
            temp = samplesIm[ofsIm + (int)firstIndex];
            samplesIm[ofsIm + (int)firstIndex] = samplesIm[ofsIm + (int)secondIndex];
            samplesIm[ofsIm + (int)secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexDoubleSampleArray a = (DirectComplexDoubleSampleArray)src;
            samplesRe[ofsRe + (int)destIndex] =
                a.samplesRe[a.ofsRe + (int)srcIndex1] + a.samplesRe[a.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                a.samplesIm[a.ofsIm + (int)srcIndex1] + a.samplesIm[a.ofsIm + (int)srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectComplexDoubleSampleArray a = (DirectComplexDoubleSampleArray)src;
            samplesRe[ofsRe + (int)destIndex] =
                a.samplesRe[a.ofsRe + (int)srcIndex1] - a.samplesRe[a.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                a.samplesIm[a.ofsIm + (int)srcIndex1] - a.samplesIm[a.ofsIm + (int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexDoubleSampleArray a2 = (DirectComplexDoubleSampleArray)src2;
            samplesRe[ofsRe + (int)destIndex] =
                samplesRe[ofsRe + (int)srcIndex1] + a2.samplesRe[a2.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                samplesIm[ofsIm + (int)srcIndex1] + a2.samplesIm[a2.ofsIm + (int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectComplexDoubleSampleArray a2 = (DirectComplexDoubleSampleArray)src2;
            samplesRe[ofsRe + (int)destIndex] =
                samplesRe[ofsRe + (int)srcIndex1] - a2.samplesRe[a2.ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] =
                samplesIm[ofsIm + (int)srcIndex1] - a2.samplesIm[a2.ofsIm + (int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[ofsRe + (int)destIndex] = samplesRe[ofsRe + (int)srcIndex1] + samplesRe[ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] = samplesIm[ofsIm + (int)srcIndex1] + samplesIm[ofsIm + (int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[ofsRe + (int)destIndex] = samplesRe[ofsRe + (int)srcIndex1] - samplesRe[ofsRe + (int)srcIndex2];
            samplesIm[ofsIm + (int)destIndex] = samplesIm[ofsIm + (int)srcIndex1] - samplesIm[ofsIm + (int)srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectComplexDoubleSampleArray a = (DirectComplexDoubleSampleArray)src;
            double re = a.samplesRe[a.ofsRe + (int)srcIndex];
            double im = a.samplesIm[a.ofsIm + (int)srcIndex];
            samplesRe[ofsRe + (int)destIndex] = re * aRe - im * aIm;
            samplesIm[ofsIm + (int)destIndex] = re * aIm + im * aRe;
        }

        public void multiplyByRealScalar(long index, double a) {
            samplesRe[ofsRe + (int)index] *= a;
            samplesIm[ofsIm + (int)index] *= a;
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int)destIndex, srcI1 = (int)srcIndex1, srcI2 = (int)srcIndex2;
            samplesRe[ofsRe + destI] = samplesRe[ofsRe + srcI1] * a1 + samplesRe[ofsRe + srcI2] * a2;
            samplesIm[ofsIm + destI] = samplesIm[ofsIm + srcI1] * a1 + samplesIm[ofsIm + srcI2] * a2;
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (int indexRe = ofsRe + (int)fromIndex, indexIm = ofsIm + (int)fromIndex,
                indexReMax = indexRe + (int)(toIndex - fromIndex); indexRe < indexReMax; indexRe++, indexIm++)
            {
                samplesRe[indexRe] *= a;
                samplesIm[indexIm] *= a;
            }
        }

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
            sb.append(String.format(format, samplesRe[ofsRe], samplesIm[ofsIm]));
            for (int k = 1; k < length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("..."); break;
                }
                sb.append(separator).append(String.format(format, samplesRe[ofsRe + k], samplesIm[ofsIm + k]));
            }
            return sb.toString();
        }
    }

    static final class DirectZeroOffsetsComplexDoubleSampleArray extends ComplexScalarSampleArray {
        final double[] samplesRe;
        final double[] samplesIm;
        final int length;

        DirectZeroOffsetsComplexDoubleSampleArray(double[] samplesRe, double[] samplesIm, int length) {
            assert length >= 0;
            assert length <= samplesRe.length;
            assert length <= samplesIm.length;
            this.samplesRe = samplesRe;
            this.samplesIm = samplesIm;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public ComplexScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0)
                throw new IllegalArgumentException("Negative length");
            if (length > Integer.MAX_VALUE)
                throw new IllegalArgumentException("length must be less than 2^31");
            int len = (int)length;
            return new DirectZeroOffsetsComplexDoubleSampleArray(new double[len], new double[len], len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectZeroOffsetsComplexDoubleSampleArray a = (DirectZeroOffsetsComplexDoubleSampleArray)src;
            samplesRe[(int)destIndex] = a.samplesRe[(int)srcIndex];
            samplesIm[(int)destIndex] = a.samplesIm[(int)srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            double temp = samplesRe[(int)firstIndex];
            samplesRe[(int)firstIndex] = samplesRe[(int)secondIndex];
            samplesRe[(int)secondIndex] = temp;
            temp = samplesIm[(int)firstIndex];
            samplesIm[(int)firstIndex] = samplesIm[(int)secondIndex];
            samplesIm[(int)secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsComplexDoubleSampleArray a = (DirectZeroOffsetsComplexDoubleSampleArray)src;
            samplesRe[(int)destIndex] = a.samplesRe[(int)srcIndex1] + a.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = a.samplesIm[(int)srcIndex1] + a.samplesIm[(int)srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsComplexDoubleSampleArray a = (DirectZeroOffsetsComplexDoubleSampleArray)src;
            samplesRe[(int)destIndex] = a.samplesRe[(int)srcIndex1] - a.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = a.samplesIm[(int)srcIndex1] - a.samplesIm[(int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsComplexDoubleSampleArray a2 = (DirectZeroOffsetsComplexDoubleSampleArray)src2;
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] + a2.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] + a2.samplesIm[(int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsComplexDoubleSampleArray a2 = (DirectZeroOffsetsComplexDoubleSampleArray)src2;
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] - a2.samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] - a2.samplesIm[(int)srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] + samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] + samplesIm[(int)srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samplesRe[(int)destIndex] = samplesRe[(int)srcIndex1] - samplesRe[(int)srcIndex2];
            samplesIm[(int)destIndex] = samplesIm[(int)srcIndex1] - samplesIm[(int)srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectZeroOffsetsComplexDoubleSampleArray a = (DirectZeroOffsetsComplexDoubleSampleArray)src;
            double re = a.samplesRe[(int)srcIndex];
            double im = a.samplesIm[(int)srcIndex];
            samplesRe[(int)destIndex] = re * aRe - im * aIm;
            samplesIm[(int)destIndex] = re * aIm + im * aRe;
        }

        public void multiplyByRealScalar(long index, double a) {
            samplesRe[(int)index] *= a;
            samplesIm[(int)index] *= a;
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int)destIndex, srcI1 = (int)srcIndex1, srcI2 = (int)srcIndex2;
            samplesRe[destI] = samplesRe[srcI1] * a1 + samplesRe[srcI2] * a2;
            samplesIm[destI] = samplesIm[srcI1] * a1 + samplesIm[srcI2] * a2;
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int from = (int)fromIndex;
            int to = (int)toIndex;
            for (int index = from; index < to; index++) {
                samplesRe[index] *= a;
                samplesIm[index] *= a;
            }
        }

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
            sb.append(String.format(format, samplesRe[0], samplesIm[0]));
            for (int k = 1; k < samplesRe.length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("..."); break;
                }
                sb.append(separator).append(String.format(format, samplesRe[k], samplesIm[k]));
            }
            return sb.toString();
        }
    }
    //[[Repeat.AutoGeneratedEnd]]
}
