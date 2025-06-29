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

package net.algart.matrices.spectra;

import net.algart.arrays.*;

import java.util.Objects;

/**
 * <p>Array of samples, where each sample is a real number, represented by a <code>double</code> value,
 * stored in an AlgART array {@link UpdatablePNumberArray}.</p>
 *
 * <p>Please use {@link #asSampleArray(UpdatablePNumberArray)} method for
 * creating instances of this class.</p>
 *
 * <p>All operations over samples (adding, subtracting, multiplying) are performed via corresponding operations
 * over elements of the AlgART array. Elements of this array are interpreted as <code>double</code> values,
 * as if they are read/written by {@link PArray#getDouble(long)} and {@link UpdatablePArray#setDouble(long, double)}
 * methods.</p>
 *
 * <p>The instances of this class are not thread-safe, but <b>are thread-compatible</b>
 * and can may be synchronized manually if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class RealScalarSampleArray implements SampleArray {

    /**
     * Returns a view of the specified AlgART array as an array of scalar real samples.
     * More precisely, the sample <code>#k</code> in the returned sample array is stored
     * in the element <code>#k</code> of <code>samples</code> array.
     *
     * <p>The returned sample array is backed by this AlgART array, so any changes of the samples
     * in the returned array are reflected in this array, and vice versa.
     * More precisely, the returned sample array is backed by
     * <code>samples.{@link UpdatableArray#asUnresizable asUnresizable()}</code>:
     * if the passed array is {@link MutableArray resizable}, possible future changes of its length
     * will not affect behavior of the returned sample array.
     *
     * <p>The {@link #length() length} of the returned sample array is equal to length of <code>samples</code> array.
     *
     * <p>This method detects, if the passed arrays are {@link DirectAccessible}
     * (i.e. the data are stored in usual Java arrays), and provides optimized implementations for this case.
     *
     * @param samples the samples.
     * @return the array of scalar real samples, represented by this array.
     * @throws NullPointerException if <code>samples</code> is {@code null}.
     */
    public static RealScalarSampleArray asSampleArray(UpdatablePNumberArray samples) {
        Objects.requireNonNull(samples, "Null samples");
        samples = (UpdatablePNumberArray) samples.asUnresizable(); // to be sure that its length will not be changed
        if (samples instanceof DirectAccessible && ((DirectAccessible) samples).hasJavaArray()) {
            Object arr = ((DirectAccessible) samples).javaArray();
            int ofs = ((DirectAccessible) samples).javaArrayOffset();
            if (arr instanceof float[]) {
                if (ofs == 0) {
                    return new DirectZeroOffsetsRealFloatSampleArray((float[]) arr, (int) samples.length());
                } else {
                    return new DirectRealFloatSampleArray((float[]) arr, ofs, (int) samples.length());
                }
            }
            if (arr instanceof double[]) {
                if (ofs == 0) {
                    return new DirectZeroOffsetsRealDoubleSampleArray((double[]) arr, (int) samples.length());
                } else {
                    return new DirectRealDoubleSampleArray((double[]) arr, ofs, (int) samples.length());
                }
            }
        }
        return new CommonRealScalarSampleArray(samples);
    }

    public final boolean isComplex() {
        return false;
    }

    public abstract long length();

    public abstract RealScalarSampleArray newCompatibleSamplesArray(long length);

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

    public abstract void combineWithRealMultipliers(
            long destIndex,
            long srcIndex1, double a1, long srcIndex2, double a2);

    public abstract void multiplyRangeByRealScalar(long from, long to, double a);

    public abstract String toString(String format, String separator, int maxStringLength);

    static final class CommonRealScalarSampleArray extends RealScalarSampleArray {
        private final UpdatablePNumberArray samples;

        CommonRealScalarSampleArray(UpdatablePNumberArray samples) {
            Objects.requireNonNull(samples, "Null samples");
            this.samples = samples;
        }

        public long length() {
            return samples.length();
        }

        public RealScalarSampleArray newCompatibleSamplesArray(long length) {
            return new CommonRealScalarSampleArray(
                    (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(samples));
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            CommonRealScalarSampleArray a = (CommonRealScalarSampleArray) src;
            samples.setDouble(destIndex, a.samples.getDouble(srcIndex));
        }

        public void swap(long firstIndex, long secondIndex) {
            samples.swap(firstIndex, secondIndex);
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonRealScalarSampleArray a = (CommonRealScalarSampleArray) src;
            samples.setDouble(destIndex, a.samples.getDouble(srcIndex1) + a.samples.getDouble(srcIndex2));
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            CommonRealScalarSampleArray a = (CommonRealScalarSampleArray) src;
            samples.setDouble(destIndex, a.samples.getDouble(srcIndex1) - a.samples.getDouble(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonRealScalarSampleArray a2 = (CommonRealScalarSampleArray) src2;
            samples.setDouble(destIndex, samples.getDouble(srcIndex1) + a2.samples.getDouble(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            CommonRealScalarSampleArray a2 = (CommonRealScalarSampleArray) src2;
            samples.setDouble(destIndex, samples.getDouble(srcIndex1) - a2.samples.getDouble(srcIndex2));
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samples.setDouble(destIndex, samples.getDouble(srcIndex1) + samples.getDouble(srcIndex2));
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samples.setDouble(destIndex, samples.getDouble(srcIndex1) - samples.getDouble(srcIndex2));
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            CommonRealScalarSampleArray a = (CommonRealScalarSampleArray) src;
            double re = a.samples.getDouble(srcIndex);
            samples.setDouble(destIndex, re * aRe);
        }

        public void multiplyByRealScalar(long index, double a) {
            samples.setDouble(index, samples.getDouble(index) * a);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (long index = fromIndex; index < toIndex; index++) {
                samples.setDouble(index, samples.getDouble(index) * a);
            }
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            samples.setDouble(destIndex, samples.getDouble(srcIndex1) * a1 + samples.getDouble(srcIndex2) * a2);
        }

        public String toString(String format, String separator, int maxStringLength) {
            Objects.requireNonNull(format, "Null format argument");
            Objects.requireNonNull(separator, "Null separator argument");
            if (maxStringLength <= 0) {
                throw new IllegalArgumentException("maxStringLength argument must be positive");
            }
            final long n = samples.length();
            if (n == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(format, samples.getDouble(0)));
            for (long k = 1; k < n; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("...");
                    break;
                }
                sb.append(separator).append(String.format(format, samples.getDouble(k)));
            }
            return sb.toString();
        }
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\s*\(([^)]+)\) ==> $1 ]]
    static final class DirectRealFloatSampleArray extends RealScalarSampleArray {
        final float[] samples;
        final int ofs;
        final int length;

        DirectRealFloatSampleArray(float[] samples, int ofs, int length) {
            assert ofs >= 0;
            assert length >= 0;
            assert ofs <= samples.length - length;
            this.samples = samples;
            this.ofs = ofs;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public RealScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Negative length");
            }
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("length must be less than 2^31");
            }
            int len = (int) length;
            return new DirectRealFloatSampleArray(new float[len], 0, len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectRealFloatSampleArray a = (DirectRealFloatSampleArray) src;
            samples[ofs + (int) destIndex] = a.samples[a.ofs + (int) srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            float temp = samples[ofs + (int) firstIndex];
            samples[ofs + (int) firstIndex] = samples[ofs + (int) secondIndex];
            samples[ofs + (int) secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealFloatSampleArray a = (DirectRealFloatSampleArray) src;
            samples[ofs + (int) destIndex] = a.samples[a.ofs + (int) srcIndex1] + a.samples[a.ofs + (int) srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealFloatSampleArray a = (DirectRealFloatSampleArray) src;
            samples[ofs + (int) destIndex] = a.samples[a.ofs + (int) srcIndex1] - a.samples[a.ofs + (int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealFloatSampleArray a2 = (DirectRealFloatSampleArray) src2;
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] + a2.samples[a2.ofs + (int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealFloatSampleArray a2 = (DirectRealFloatSampleArray) src2;
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] - a2.samples[a2.ofs + (int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] + samples[ofs + (int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] - samples[ofs + (int) srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectRealFloatSampleArray a = (DirectRealFloatSampleArray) src;
            double re = a.samples[a.ofs + (int) srcIndex];
            samples[ofs + (int) destIndex] = (float) (re * aRe);
        }

        public void multiplyByRealScalar(long index, double a) {
            double re = samples[ofs + (int) index];
            samples[ofs + (int) index] = (float) (re * a);
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int) destIndex, srcI1 = (int) srcIndex1, srcI2 = (int) srcIndex2;
            samples[ofs + destI] = (float) (samples[ofs + srcI1] * a1 + samples[ofs + srcI2] * a2);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (int index = ofs + (int) fromIndex,
                 indexMax = index + (int) (toIndex - fromIndex); index < indexMax; index++) {
                samples[index] = (float) (samples[index] * a);
            }
        }

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
            sb.append(String.format(format, samples[ofs]));
            for (int k = 1; k < length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("...");
                    break;
                }
                sb.append(separator).append(String.format(format, samples[ofs + k]));
            }
            return sb.toString();
        }
    }

    static final class DirectZeroOffsetsRealFloatSampleArray extends RealScalarSampleArray {
        final float[] samples;
        final int length;

        DirectZeroOffsetsRealFloatSampleArray(float[] samples, int length) {
            assert length >= 0;
            assert length <= samples.length;
            this.samples = samples;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public RealScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Negative length");
            }
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("length must be less than 2^31");
            }
            int len = (int) length;
            return new DirectZeroOffsetsRealFloatSampleArray(new float[len], len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectZeroOffsetsRealFloatSampleArray a = (DirectZeroOffsetsRealFloatSampleArray) src;
            samples[(int) destIndex] = a.samples[(int) srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            float temp = samples[(int) firstIndex];
            samples[(int) firstIndex] = samples[(int) secondIndex];
            samples[(int) secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsRealFloatSampleArray a = (DirectZeroOffsetsRealFloatSampleArray) src;
            samples[(int) destIndex] = a.samples[(int) srcIndex1] + a.samples[(int) srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsRealFloatSampleArray a = (DirectZeroOffsetsRealFloatSampleArray) src;
            samples[(int) destIndex] = a.samples[(int) srcIndex1] - a.samples[(int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsRealFloatSampleArray a2 = (DirectZeroOffsetsRealFloatSampleArray) src2;
            samples[(int) destIndex] = samples[(int) srcIndex1] + a2.samples[(int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsRealFloatSampleArray a2 = (DirectZeroOffsetsRealFloatSampleArray) src2;
            samples[(int) destIndex] = samples[(int) srcIndex1] - a2.samples[(int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samples[(int) destIndex] = samples[(int) srcIndex1] + samples[(int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samples[(int) destIndex] = samples[(int) srcIndex1] - samples[(int) srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectZeroOffsetsRealFloatSampleArray a = (DirectZeroOffsetsRealFloatSampleArray) src;
            double re = a.samples[(int) srcIndex];
            samples[(int) destIndex] = (float) (re * aRe);
        }

        public void multiplyByRealScalar(long index, double a) {
            float v = samples[(int) index];
            samples[(int) index] = (float) (v * a);
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int) destIndex, srcI1 = (int) srcIndex1, srcI2 = (int) srcIndex2;
            samples[destI] = (float) (samples[srcI1] * a1 + samples[srcI2] * a2);
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int from = (int) fromIndex;
            int to = (int) toIndex;
            for (int index = from; index < to; index++) {
                float v = samples[index];
                samples[index] = (float) (v * a);
            }
        }

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
            sb.append(String.format(format, samples[0]));
            for (int k = 1; k < samples.length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("...");
                    break;
                }
                sb.append(separator).append(String.format(format, samples[k]));
            }
            return sb.toString();
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static final class DirectRealDoubleSampleArray extends RealScalarSampleArray {
        final double[] samples;
        final int ofs;
        final int length;

        DirectRealDoubleSampleArray(double[] samples, int ofs, int length) {
            assert ofs >= 0;
            assert length >= 0;
            assert ofs <= samples.length - length;
            this.samples = samples;
            this.ofs = ofs;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public RealScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Negative length");
            }
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("length must be less than 2^31");
            }
            int len = (int) length;
            return new DirectRealDoubleSampleArray(new double[len], 0, len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectRealDoubleSampleArray a = (DirectRealDoubleSampleArray) src;
            samples[ofs + (int) destIndex] = a.samples[a.ofs + (int) srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            double temp = samples[ofs + (int) firstIndex];
            samples[ofs + (int) firstIndex] = samples[ofs + (int) secondIndex];
            samples[ofs + (int) secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealDoubleSampleArray a = (DirectRealDoubleSampleArray) src;
            samples[ofs + (int) destIndex] = a.samples[a.ofs + (int) srcIndex1] + a.samples[a.ofs + (int) srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectRealDoubleSampleArray a = (DirectRealDoubleSampleArray) src;
            samples[ofs + (int) destIndex] = a.samples[a.ofs + (int) srcIndex1] - a.samples[a.ofs + (int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealDoubleSampleArray a2 = (DirectRealDoubleSampleArray) src2;
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] + a2.samples[a2.ofs + (int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectRealDoubleSampleArray a2 = (DirectRealDoubleSampleArray) src2;
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] - a2.samples[a2.ofs + (int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] + samples[ofs + (int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samples[ofs + (int) destIndex] = samples[ofs + (int) srcIndex1] - samples[ofs + (int) srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectRealDoubleSampleArray a = (DirectRealDoubleSampleArray) src;
            double re = a.samples[a.ofs + (int) srcIndex];
            samples[ofs + (int) destIndex] = re * aRe;
        }

        public void multiplyByRealScalar(long index, double a) {
            double re = samples[ofs + (int) index];
            samples[ofs + (int) index] = re * a;
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int) destIndex, srcI1 = (int) srcIndex1, srcI2 = (int) srcIndex2;
            samples[ofs + destI] = samples[ofs + srcI1] * a1 + samples[ofs + srcI2] * a2;
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            for (int index = ofs + (int) fromIndex,
                 indexMax = index + (int) (toIndex - fromIndex); index < indexMax; index++) {
                samples[index] = samples[index] * a;
            }
        }

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
            sb.append(String.format(format, samples[ofs]));
            for (int k = 1; k < length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("...");
                    break;
                }
                sb.append(separator).append(String.format(format, samples[ofs + k]));
            }
            return sb.toString();
        }
    }

    static final class DirectZeroOffsetsRealDoubleSampleArray extends RealScalarSampleArray {
        final double[] samples;
        final int length;

        DirectZeroOffsetsRealDoubleSampleArray(double[] samples, int length) {
            assert length >= 0;
            assert length <= samples.length;
            this.samples = samples;
            this.length = length;
        }

        public long length() {
            return length;
        }

        public RealScalarSampleArray newCompatibleSamplesArray(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Negative length");
            }
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("length must be less than 2^31");
            }
            int len = (int) length;
            return new DirectZeroOffsetsRealDoubleSampleArray(new double[len], len);
        }

        public void copy(long destIndex, SampleArray src, long srcIndex) {
            DirectZeroOffsetsRealDoubleSampleArray a = (DirectZeroOffsetsRealDoubleSampleArray) src;
            samples[(int) destIndex] = a.samples[(int) srcIndex];
        }

        public void swap(long firstIndex, long secondIndex) {
            double temp = samples[(int) firstIndex];
            samples[(int) firstIndex] = samples[(int) secondIndex];
            samples[(int) secondIndex] = temp;
        }

        public void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsRealDoubleSampleArray a = (DirectZeroOffsetsRealDoubleSampleArray) src;
            samples[(int) destIndex] = a.samples[(int) srcIndex1] + a.samples[(int) srcIndex2];
        }

        public void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2) {
            DirectZeroOffsetsRealDoubleSampleArray a = (DirectZeroOffsetsRealDoubleSampleArray) src;
            samples[(int) destIndex] = a.samples[(int) srcIndex1] - a.samples[(int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsRealDoubleSampleArray a2 = (DirectZeroOffsetsRealDoubleSampleArray) src2;
            samples[(int) destIndex] = samples[(int) srcIndex1] + a2.samples[(int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2) {
            DirectZeroOffsetsRealDoubleSampleArray a2 = (DirectZeroOffsetsRealDoubleSampleArray) src2;
            samples[(int) destIndex] = samples[(int) srcIndex1] - a2.samples[(int) srcIndex2];
        }

        public void add(long destIndex, long srcIndex1, long srcIndex2) {
            samples[(int) destIndex] = samples[(int) srcIndex1] + samples[(int) srcIndex2];
        }

        public void sub(long destIndex, long srcIndex1, long srcIndex2) {
            samples[(int) destIndex] = samples[(int) srcIndex1] - samples[(int) srcIndex2];
        }

        public void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm) {
            DirectZeroOffsetsRealDoubleSampleArray a = (DirectZeroOffsetsRealDoubleSampleArray) src;
            double re = a.samples[(int) srcIndex];
            samples[(int) destIndex] = re * aRe;
        }

        public void multiplyByRealScalar(long index, double a) {
            double v = samples[(int) index];
            samples[(int) index] = v * a;
        }

        public void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2) {
            int destI = (int) destIndex, srcI1 = (int) srcIndex1, srcI2 = (int) srcIndex2;
            samples[destI] = samples[srcI1] * a1 + samples[srcI2] * a2;
        }

        public void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a) {
            int from = (int) fromIndex;
            int to = (int) toIndex;
            for (int index = from; index < to; index++) {
                double v = samples[index];
                samples[index] = v * a;
            }
        }

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
            sb.append(String.format(format, samples[0]));
            for (int k = 1; k < samples.length; k++) {
                if (sb.length() >= maxStringLength) {
                    sb.append(separator).append("...");
                    break;
                }
                sb.append(separator).append(String.format(format, samples[k]));
            }
            return sb.toString();
        }
    }

    //[[Repeat.AutoGeneratedEnd]]
}
