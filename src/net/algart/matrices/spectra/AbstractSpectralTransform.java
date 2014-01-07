/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>A skeletal implementation of the {@link SpectralTransform} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>The main purpose of this class is implementing <i>n</i>-dimensional
 * {@link #directTransformMatrix directTransformMatrix} / {@link #inverseTransformMatrix inverseTransformMatrix}
 * methods via more simple (and more abstract) one-dimensional
 * {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform} methods.
 * The algorithm of this implementation is the following:</p>
 *
 * <ul>
 * <li>At the first iteration, all 1-dimensional "lines" of the matrix, i.e. every sequential
 * {@link Matrix#dim(int) dim(0)} numbers, are represented as {@link SampleArray} by
 * {@link RealScalarSampleArray#asSampleArray RealScalarSampleArray.asSampleArray} or
 * {@link ComplexScalarSampleArray#asSampleArray ComplexScalarSampleArray.asSampleArray} method.
 * All these sample arrays are transformed by {@link #directTransform directTransform} call
 * (in a case of the direct transform) or by {@link #inverseTransform inverseTransform} call
 * (in a case of the inverse transform).</li>
 *
 * <li>At the second iteration, all 2-dimensional "layers" of the (multidimensional) matrix, i.e. every sequential
 * {@link Matrix#dim(int) dim(0)}*{@link Matrix#dim(int) dim(1)} numbers, are represented as {@link SampleArray} by
 * {@link RealVectorSampleArray#asSampleArray RealVectorSampleArray.asSampleArray} or
 * {@link ComplexVectorSampleArray#asSampleArray ComplexVectorSampleArray.asSampleArray} method,
 * where the vector length and step are chosen equal to {@link Matrix#dim(int) dim(0)}.
 * All these sample arrays are transformed by {@link #directTransform directTransform} call
 * (in a case of the direct transform) or by {@link #inverseTransform inverseTransform} call
 * (in a case of the inverse transform).</li>
 *
 * <li>...</li>
 *
 * <li>At the last iteration, the whole <i>n</i>-dimensional matrix is represented as a {@link SampleArray} by
 * {@link RealVectorSampleArray#asSampleArray RealVectorSampleArray.asSampleArray} or
 * {@link ComplexVectorSampleArray#asSampleArray ComplexVectorSampleArray.asSampleArray} method,
 * where the vector length and step are chosen equal to
 * {@link Matrix#dim(int) dim(0)}*{@link Matrix#dim(int) dim(1)}*...*{@link Matrix#dim(int) dim(N-1)}.
 * This sample array is transformed by {@link #directTransform directTransform} call
 * (in a case of the direct transform) or by {@link #inverseTransform inverseTransform} call
 * (in a case of the inverse transform).</li>
 * </ul>
 *
 * <p>In other words, the transform is sequentially applied along the dimension 0,1,...,<i>N</i>&minus;1.</p>
 *
 * <p>The "real" case is chosen above if the second <tt>matrixIm</tt> argument of
 * {@link #directTransformMatrix directTransformMatrix} / {@link #inverseTransformMatrix inverseTransformMatrix}
 * methods is <tt>null</tt>; in this case, the
 * {@link RealScalarSampleArray#asSampleArray RealScalarSampleArray.asSampleArray} or
 * {@link RealVectorSampleArray#asSampleArray RealVectorSampleArray.asSampleArray} method is applied
 * to the corresponding {@link Array#subArr(long, long) subarrays} of the {@link Matrix#array() underlying array}
 * of the <tt>matrixRe</tt> argument.
 *
 * <p>The "complex" case is chosen if the second <tt>matrixIm</tt> argument is not <tt>null</tt>; in this case, the
 * {@link ComplexScalarSampleArray#asSampleArray ComplexScalarSampleArray.asSampleArray} or
 * {@link ComplexVectorSampleArray#asSampleArray ComplexVectorSampleArray.asSampleArray} method is applied
 * to the pairs of corresponding {@link Array#subArr(long, long) subarrays} of the
 * {@link Matrix#array() underlying arrays} of both <tt>matrixRe</tt> and <tt>matrixIm</tt>  arguments.</p>
 *
 * <p>This algorithm is a traditional way of generalizing 1-dimensional FFT (Fourier transform)
 * to 2-dimensional and multidimensional case.
 * For FHT (Hartley transform), this generalization leads to so-called <i>separable</i> multidimensional
 * Hartley transform.</p>
 *
 * <p>The described algorithm is only a basic scheme of the implementation of
 * {@link #directTransformMatrix directTransformMatrix} and {@link #inverseTransformMatrix inverseTransformMatrix}
 * methods by this class. Really, this implementation performs much more: for example, downloads parts of large
 * matrices into Java memory ({@link SimpleMemoryModel}) for better performance; splits the execution into
 * several parallel tasks on multi-core or multiprocessor computers, according the passed {@link ArrayContext}
 * or via {@link DefaultThreadPoolFactory} when this context is <tt>null</tt>; provides used interruption and
 * showing progress via the passed {@link ArrayContext} (if it's not <tt>null</tt>); etc.</p>
 *
 * <p>Please note: for very large matrices, much greater than the available Java RAM, the algorithms, implemented
 * in this class, are designed to {@link Matrix#tile() tiled} matrices. For non-tiled matrices, these
 * algorithms can work slowly.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractSpectralTransform implements SpectralTransform {

    private static final boolean BUFFERING_LARGE_MATRICES = true;

    /**
     * The minimal possible result of {@link #maxTempJavaMemory()} method of this class: {@value} bytes (4 MB).
     *
     * @see #AbstractSpectralTransform(long)
     */
    public static final long MIN_SPECTRAL_JAVA_MEMORY = 4194304;

    private final long maxTempJavaMemory;

    /**
     * Creates a new instance of this class.
     *
     * <p>This constructor is called by all constructors of
     * {@link FastFourierTransform} and {@link SeparableFastHartleyTransform} classes,
     * which have no <tt>maxTempJavaMemory</tt> argument.
     *
     * @see #AbstractSpectralTransform(long)
     */
    protected AbstractSpectralTransform() {
        this(Arrays.SystemSettings.maxTempJavaMemory());
    }

    /**
     * Creates a new instance of this class.
     *
     * <p>The <tt>maxTempJavaMemory</tt> argument specifies the amount of Java memory (heap),
     * that can be used by methods of this class for internal needs.
     * If this class was created by {@link #AbstractSpectralTransform() the costructor without argument},
     * then the standard value {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()} will be used.
     * This constructor allows to specify this amount manually, usually larger than that standard value.
     * Java memory is very useful for improving performance of {@link #transformMatrix transformMatrix} method
     * (and {@link #directTransformMatrix directTransformMatrix} /
     * {@link #inverseTransformMatrix inverseTransformMatrix} methods),
     * especially for a case of very large matrices.
     *
     * <p>If the <tt>maxTempJavaMemory</tt> argument is less then {@link #MIN_SPECTRAL_JAVA_MEMORY},
     * it is ignored and {@link #MIN_SPECTRAL_JAVA_MEMORY} will be used instead.
     *
     * <p>The <tt>maxTempJavaMemory</tt> value is accesed via {@link #maxTempJavaMemory()} method only.
     * If you override that method, you can change the described behaviour.
     *
     * <p>This constructor is called by all constructors of
     * {@link FastFourierTransform} and {@link SeparableFastHartleyTransform} classes,
     * which have <tt>maxTempJavaMemory</tt> argument.
     *
     * @param maxTempJavaMemory desired maximal amount of Java memory, in bytes, allowed for allocating
     *                          by methods of this class for internal needs.
     * @see #AbstractSpectralTransform()
     */
    protected AbstractSpectralTransform(long maxTempJavaMemory) {
        this.maxTempJavaMemory = maxTempJavaMemory;
    }

    public abstract boolean isLengthAllowed(long length);

    public abstract boolean areComplexSamplesRequired();

    /**
     * This implementation checks <tt>samples</tt> array and calls
     * <nobr>{@link #transform(ArrayContext, SampleArray, boolean) transform(context,samples,false)}</nobr>.
     *
     * <p>Checking <tt>samples</tt> array means the following.
     * First, if {@link #areComplexSamplesRequired()} returns <tt>true</tt>, this method checks the result
     * of <tt>samples.{@link SampleArray#isComplex() isComplex()}</tt> method, and if it is <tt>false</tt>,
     * this method throws <tt>UnsupportedOperationException</tt>.
     * Then this method checks <tt>samples.{@link SampleArray#length() length()}</tt>
     * by {@link #isLengthAllowed(long)} method;
     * if {@link #isLengthAllowed(long) isLengthAllowed} returns <tt>false</tt>, this mewthod
     * throws <tt>IllegalArgumentException</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param samples the transformed samples.
     * @throws NullPointerException          if the <tt>samples</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if the {@link SampleArray#length() length} of the passed array
     *                                       is not allowed, i.e. if {@link #isLengthAllowed} method
     *                                       returns <tt>false</tt> for this value.
     * @throws UnsupportedOperationException if {@link #areComplexSamplesRequired()} method returns <tt>true</tt>,
     *                                       but <tt>samples.{@link SampleArray#isComplex() isComplex()}</tt> method
     *                                       returns <tt>false</tt>.
     */
    public final void directTransform(ArrayContext context, SampleArray samples) {
        checkSamples(samples);
        transform(context, samples, false);
    }

    /**
     * This implementation checks <tt>samples</tt> array and calls
     * <nobr>{@link #transform(ArrayContext, SampleArray, boolean) transform(context,samples,true)}</nobr>.
     *
     * <p>Checking <tt>samples</tt> array means the following.
     * First, if {@link #areComplexSamplesRequired()} returns <tt>true</tt>, this method checks the result
     * of <tt>samples.{@link SampleArray#isComplex() isComplex()}</tt> method, and if it is <tt>false</tt>,
     * this method throws <tt>UnsupportedOperationException</tt>.
     * Then this method checks <tt>samples.{@link SampleArray#length() length()}</tt>
     * by {@link #isLengthAllowed(long)} method;
     * if {@link #isLengthAllowed(long) isLengthAllowed} returns <tt>false</tt>, this mewthod
     * throws <tt>IllegalArgumentException</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param samples the transformed samples.
     * @throws NullPointerException          if the <tt>samples</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if the {@link SampleArray#length() length} of the passed array
     *                                       is not allowed, i.e. if {@link #isLengthAllowed} method
     *                                       returns <tt>false</tt> for this value.
     * @throws UnsupportedOperationException if {@link #areComplexSamplesRequired()} method returns <tt>true</tt>,
     *                                       but <tt>samples.{@link SampleArray#isComplex() isComplex()}</tt> method
     *                                       returns <tt>false</tt>.
     */
    public final void inverseTransform(ArrayContext context, SampleArray samples) {
        checkSamples(samples);
        transform(context, samples, true);
    }

    /**
     * This implementation checks the passed matrices and calls
     * <nobr>{@link #transformMatrix(ArrayContext, Matrix, Matrix, boolean)
     * transformMatrix(context,matrixRe,matrixIm,false)}</nobr>.
     *
     * <p>Checking matrices means the following.
     * First, if <tt>matrixRe</tt> argument is <tt>null</tt>, this method throws <tt>NullPointerException</tt>.
     * Second, if {@link #areComplexSamplesRequired()} returns <tt>true</tt> and <tt>matrixIm</tt> argument
     * is <tt>null</tt>, this method throws <tt>UnsupportedOperationException</tt>.
     * Third, if <tt>matrixIm</tt> argument is not <tt>null</tt>,
     * this method checks that its dimensions are {@link Matrix#dimEquals(Matrix) equal} to the dimensions
     * of <tt>matrixRe</tt>. If it is not so, {@link SizeMismatchException} is thrown.
     * Last, this method checks, whether all dimensions of the passed matrices are allowed, i.e. that
     * {@link #isLengthAllowed(long)} method returns <tt>true</tt> for them. If it is not so,
     * {@link IllegalArgumentException} is thrown.
     *
     * @param context  the context that will be used by this algorithm; may be <tt>null</tt>
     *                 (see comments to {@link SpectralTransform}).
     * @param matrixRe the transformed matrix if we have a real matrix;
     *                 the real parts of the elements of the transformed matrix if it is a complex matrix.
     * @param matrixIm <tt>null</tt> if we have a real matrix;
     *                 the imaginary parts of the elements of the transformed matrix if it is a complex matrix.
     * @throws NullPointerException          if the <tt>matrixRe</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if the some of {@link Matrix#dim(int) dimensions} of the passed matrices
     *                                       is not allowed, i.e. if {@link #isLengthAllowed} method
     *                                       returns <tt>false</tt> for this value.
     * @throws SizeMismatchException         if both passed matrices are not <tt>null</tt> (the case of the complex
     *                                       matrix) and have different dimensions.
     * @throws UnsupportedOperationException if {@link #areComplexSamplesRequired()} method returns <tt>true</tt>
     *                                       and <tt>matrixIm</tt> argument is <tt>null</tt>.
     */
    public final void directTransformMatrix(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> matrixRe,
        Matrix<? extends UpdatablePNumberArray> matrixIm)
    {
        checkMatrices(matrixRe, matrixIm);
        transformMatrix(context, matrixRe, matrixIm, false);
    }

    /**
     * This implementation checks the matrices and calls
     * <nobr>{@link #transformMatrix(ArrayContext, Matrix, Matrix, boolean)
     * transformMatrix(context,matrixRe,matrixIm,true)}</nobr>.
     *
     * <p>Checking matrices means the following.
     * First, if <tt>matrixRe</tt> argument is <tt>null</tt>, this method throws <tt>NullPointerException</tt>.
     * Second, if {@link #areComplexSamplesRequired()} returns <tt>true</tt> and <tt>matrixIm</tt> argument
     * is <tt>null</tt>, this method throws <tt>UnsupportedOperationException</tt>.
     * Third, if <tt>matrixIm</tt> argument is not <tt>null</tt>,
     * this method checks that its dimensions are {@link Matrix#dimEquals(Matrix) equal} to the dimensions
     * of <tt>matrixRe</tt>. If it is not so, {@link SizeMismatchException} is thrown.
     * Last, this method checks, whether all dimensions of the passed matrices are allowed, i.e. that
     * {@link #isLengthAllowed(long)} method returns <tt>true</tt> for them. If it is not so,
     * {@link IllegalArgumentException} is thrown.
     *
     * @param context  the context that will be used by this algorithm; may be <tt>null</tt>
     *                 (see comments to {@link SpectralTransform}).
     * @param matrixRe the transformed matrix if we have a real matrix;
     *                 the real parts of the elements of the transformed matrix if it is a complex matrix.
     * @param matrixIm <tt>null</tt> if we have a real matrix;
     *                 the imaginary parts of the elements of the transformed matrix if it is a complex matrix.
     * @throws NullPointerException          if the <tt>matrixRe</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException      if the some of {@link Matrix#dim(int) dimensions} of the passed matrices
     *                                       is not allowed, i.e. if {@link #isLengthAllowed} method
     *                                       returns <tt>false</tt> for this value.
     * @throws SizeMismatchException         if both passed matrices are not <tt>null</tt> (the case of the complex
     *                                       matrix) and have different dimensions.
     * @throws UnsupportedOperationException if {@link #areComplexSamplesRequired()} method returns <tt>true</tt>
     *                                       and <tt>matrixIm</tt> argument is <tt>null</tt>.
     */
    public final void inverseTransformMatrix(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> matrixRe,
        Matrix<? extends UpdatablePNumberArray> matrixIm)
    {
        checkMatrices(matrixRe, matrixIm);
        transformMatrix(context, matrixRe, matrixIm, true);
    }

    /**
     * Retrurns a message used while throwing <tt>IllegalArgumentException</tt> by methods of this class
     * in a case, when the length of the samples array or some of the matrix dimensions is not allowed
     * according to {@link #isLengthAllowed(long)} method.
     * Typical examples of this message (implemented in {@link FastFourierTransform} and
     * {@link SeparableFastHartleyTransform} classes):
     * <tt>"FFT algorithm can process only 2^k elements"</tt> or
     * <tt>"FHT algorithm can process only 2^k elements"</tt>.
     *
     * @return a message used while thrown exception if {@link #isLengthAllowed(long)} method returns <tt>false</tt>.
     */
    protected abstract String unallowedLengthMessage();

    /**
     * Actually performs the 1-dimensional transform of the sample array, direct or inverse.
     *
     * <p>It is called from {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform}
     * methods. In this case, there is a guarantee that:
     * 1) <tt>samples!=null</tt>;
     * 2) if {@link #areComplexSamplesRequired()}, then <tt>samples.{@link SampleArray#isComplex() isComplex()}</tt>
     * returns <tt>true</tt>;
     * 3) {@link #isLengthAllowed(long)} returns <tt>true</tt> for <tt>samples.length()</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param samples the transformed samples.
     * @param inverse <tt>true</tt> if this method implements the inverse transform,
     *                <tt>false</tt> if this method implements the direct transform.
     */
    protected abstract void transform(ArrayContext context, SampleArray samples, boolean inverse);

    /**
     * Implements the generalization of the 1-dimensional spectral transformation,
     * performing by {@link #transform(ArrayContext, SampleArray, boolean)} method, to multidimensional case,
     * as described in the {@link AbstractSpectralTransform comments to this class}.
     * You can override this method, if such generalization is unsuitable, for example, if you want to implement
     * the traditional (nonseparable) multidimensional Hartley transform.
     *
     * <p>This method is called from {@link #directTransformMatrix directTransformMatrix}
     * / {@link #inverseTransformMatrix inverseTransformMatrix} methods.
     * In this case, there is a guarantee that: 1) <tt>matrixRe!=null</tt>;
     * 2) in a case {@link #areComplexSamplesRequired()}, also <tt>matrixIm!=null</tt>;
     * 3) <tt>matrixIm</tt> (if not <tt>null</tt>) has the same dimensions as <tt>matrixRe</tt> and
     * 4) {@link #isLengthAllowed(long)} returns <tt>true</tt> for all these dimensions.
     *
     * @param context  the context that will be used by this algorithm; may be <tt>null</tt>
     *                 (see comments to {@link SpectralTransform}).
     * @param matrixRe the transformed matrix if we have a real matrix;
     *                 the real parts of the elements of the transformed matrix if it is a complex matrix.
     * @param matrixIm <tt>null</tt> if we have a real matrix;
     *                 the imaginary parts of the elements of the transformed matrix if it is a complex matrix.
     * @param inverse  <tt>true</tt> if this method implements the inverse transform,
     *                 <tt>false</tt> if this method implements the direct transform.
     */
    protected void transformMatrix(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> matrixRe,
        Matrix<? extends UpdatablePNumberArray> matrixIm,
        boolean inverse)
    {
        int numberOfTasks = Math.max(1, Arrays.getThreadPoolFactory(context).recommendedNumberOfTasks());
        transformMatrixRecursively(context, matrixRe, matrixIm, inverse, numberOfTasks);
    }

    /**
     * Specifies the maximal amount of usual Java memory,
     * in bytes, that methods of this class may freely use for internal needs.
     * Larger amounts of work memory, if necessary, are allocated by the {@link MemoryModel memory model},
     * returned by the {@link ArrayContext context}, passed to methods of this class.
     *
     * <p>By default, this method returns
     * <tt>max({@link #MIN_SPECTRAL_JAVA_MEMORY},maxTempJavaMemory)</tt>,
     * where <tt>maxTempJavaMemory</tt> is the argument of
     * {@link #AbstractSpectralTransform(long) the corresponding constructor}
     * or {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()},
     * if {@link #AbstractSpectralTransform() the costructor without argument} has been used.
     *
     * <p>You may override this method if you want to change this behaviour.
     * Please not return here too small values: transformation of two- or multidimensional matrices
     * can work very slowly, if the result of this method does not allow allocate a work buffer for
     * storing at least several matrix lines (i.e. K*{@link Matrix#dimX()} elements, where K
     * is a little integer number, usually from 1-2 to 10-20).
     *
     * @return maximal amount of Java memory, in bytes, allowed for allocating
     *         by methods of this class for internal needs.
     */
    protected long maxTempJavaMemory() {
        return Math.max(maxTempJavaMemory, MIN_SPECTRAL_JAVA_MEMORY);
    }

    private void checkSamples(SampleArray samples) {
        if (areComplexSamplesRequired() && !samples.isComplex())
            throw new UnsupportedOperationException("Fast Fourier transformation requires complex samples");
        if (!isLengthAllowed(samples.length())) {
            throw new IllegalArgumentException("The length of sample array " + samples.length()
                + " is not allowed: " + unallowedLengthMessage());
        }
    }

    private void checkMatrices(
        Matrix<? extends UpdatablePNumberArray> matrixRe,
        Matrix<? extends UpdatablePNumberArray> matrixIm)
    {
        if (matrixRe == null)
            throw new NullPointerException("Null matrixRe argument");
        if (areComplexSamplesRequired() && matrixIm == null)
            throw new UnsupportedOperationException("Null matrixRe argument, but "
                + "Fast Fourier transformation requires complex samples");
        if (matrixIm != null && !matrixRe.dimEquals(matrixIm))
            throw new SizeMismatchException("matrixRe and matrixIm dimensions mismatch: "
                + "matrixRe is " + matrixRe + ", matrixIm " + matrixIm);
        long[] dimensions = matrixRe.dimensions();
        for (int k = 0; k < dimensions.length; k++) {
            if (!isLengthAllowed(dimensions[k]))
                throw new IllegalArgumentException("The matrix dimension #" + k + " = " + dimensions[k]
                    + " is not allowed: " + unallowedLengthMessage());
        }
    }

    private void transformMatrixRecursively(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> matrixRe, Matrix<? extends UpdatablePNumberArray> matrixIm,
        boolean inverse, int numberOfTasks)
    {
        final long[] dim = matrixRe.dimensions();
        UpdatablePNumberArray arrayRe = matrixRe.array();
        UpdatablePNumberArray arrayIm = matrixIm == null ? null : matrixIm.array();
        boolean doClone = !areDirect(arrayRe, arrayIm)
            && Arrays.sizeOf(arrayRe) <= maxTempJavaMemory() - (matrixIm == null ? 0 : Arrays.sizeOf(arrayIm));
        if (doClone) {
            UpdatablePNumberArray a = (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(arrayRe);
            Arrays.copy(context == null ? null : context.part(0.0, matrixIm == null ? 0.1 : 0.05), a, arrayRe);
            arrayRe = a;
            if (matrixIm != null) {
                a = (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(arrayIm);
                Arrays.copy(context == null ? null : context.part(0.05, 0.1), a, arrayIm);
                arrayIm = a;
            }
        }

        transformMatrixBaseAlgorithm(context == null || !doClone? context : context.part(0.1, 0.9),
            arrayRe, arrayIm, dim, inverse, numberOfTasks);

        if (doClone) {
            Arrays.copy(context == null ? null : context.part(0.9, arrayIm == null ? 1.0 : 0.95),
                matrixRe.array(), arrayRe);
            if (matrixIm != null) {
                Arrays.copy(context == null ? null : context.part(0.95, 1.0), matrixIm.array(), arrayIm);
            }
        }
    }

    private void transformMatrixBaseAlgorithm(ArrayContext context,
        UpdatablePNumberArray arrayRe, UpdatablePNumberArray arrayIm, long[] dim,
        boolean inverse, int numberOfTasks)
    {
        if (dim.length == 1) {
            SampleArray samples = arrayIm == null ?
                RealScalarSampleArray.asSampleArray(arrayRe) :
                ComplexScalarSampleArray.asSampleArray(arrayRe, arrayIm);
            transform(context, samples, inverse);
        } else {
//            long t1 = System.nanoTime();
            transformHorizontalRecursively(context == null ? null : context.part(0, dim.length - 1, dim.length),
                arrayRe, arrayIm, dim, inverse, numberOfTasks);
//            long t2 = System.nanoTime();
            transformVertical(context == null ? null : context.part(dim.length - 1, dim.length, dim.length),
                arrayRe, arrayIm, dim, inverse, numberOfTasks);
//            long t3 = System.nanoTime();
//            System.out.printf("Horizontal time %.3f ms, vertical time %.3f ms%n", (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
        }
    }

    private void transformHorizontalRecursively(ArrayContext context,
        UpdatablePNumberArray arrayRe, UpdatablePNumberArray arrayIm, long[] dimensions,
        boolean inverse, int numberOfTasks)
    {
        assert dimensions.length >= 2;
        final long[] layerDims = JArrays.copyOfRange(dimensions, 0, dimensions.length - 1);
        final long layerLen = Arrays.longMul(layerDims);
        final long lastDim = dimensions[dimensions.length - 1];
        final double elementSize = (arrayRe.bitsPerElement() + (arrayIm == null ? 0 : arrayIm.bitsPerElement())) / 8.0;
        final long bufLen = (long)(Math.max(maxTempJavaMemory(), 0) / elementSize);
        boolean direct = areDirect(arrayRe, arrayIm);
        if (BUFFERING_LARGE_MATRICES && layerLen <= bufLen / 2 && !direct) {
            // (there should be at least 2 lines in a buffer; we do not use 2*layerLen operator to avoid overflow)
            transformHorizontalRecursivelyWithSplitting(context,
                arrayRe, arrayIm, dimensions, layerLen, bufLen, inverse, numberOfTasks);
        } else {
            transformHorizontalRecursivelyBaseAlgorithm(context,
                arrayRe, arrayIm, layerDims, lastDim, layerLen, inverse, direct ? numberOfTasks : 1);
        }
    }

    private void transformHorizontalRecursivelyBaseAlgorithm(final ArrayContext context,
        final UpdatablePNumberArray arrayRe, final UpdatablePNumberArray arrayIm,
        final long[] layerDims, final long lastDim, final long layerLen,
        final boolean inverse, int numberOfTasks)
    {
        numberOfTasks = (int)Math.min(numberOfTasks, lastDim);
        if (numberOfTasks <= 1) { // maybe 0
            for (long k = 0, disp = 0; k < lastDim; k++, disp += layerLen) {
                transformMatrixRecursively(null,
                    Matrices.matrix((UpdatablePNumberArray)arrayRe.subArr(disp, layerLen), layerDims),
                    arrayIm == null ? null :
                        Matrices.matrix((UpdatablePNumberArray)arrayIm.subArr(disp, layerLen), layerDims),
                    inverse, 1);
                if (context != null) {
                    context.checkInterruptionAndUpdateProgress(null, k + 1, lastDim);
                }
            }
        } else {
            final Runnable[] tasks = new Runnable[numberOfTasks];
            final AtomicLong readyLayers = new AtomicLong(0);
            for (int threadIndex = 0; threadIndex < tasks.length; threadIndex++) {
                final int ti = threadIndex;
                tasks[ti] = new Runnable() {
                    public void run() {
                        final long layerStep = tasks.length * layerLen;
                        for (long k = ti, disp = ti * layerLen; k < lastDim; k += tasks.length, disp += layerStep) {
                            transformMatrixRecursively(null,
                                Matrices.matrix((UpdatablePNumberArray)arrayRe.subArr(disp, layerLen), layerDims),
                                arrayIm == null ? null :
                                    Matrices.matrix((UpdatablePNumberArray)arrayIm.subArr(disp, layerLen), layerDims),
                                inverse, 1);
                            if (context != null) {
                                context.checkInterruptionAndUpdateProgress(null,
                                    readyLayers.incrementAndGet(), lastDim);
                            }
                        }
                    }
                };
            }
            Arrays.getThreadPoolFactory(context).performTasks(tasks);
        }
    }

    private void transformHorizontalRecursivelyWithSplitting(ArrayContext context,
        UpdatablePNumberArray arrayRe, UpdatablePNumberArray arrayIm,
        long[] dimensions, long layerLen, long bufLen,
        boolean inverse, int numberOfTasks)
    {
        assert layerLen <= bufLen / 2; // so, the splitting has a sense
        final long lastDim = dimensions[dimensions.length - 1];
        final long layersPerBuf = bufLen / layerLen;
        assert layersPerBuf > 1; // because 2 * layerLen <= bufLen
        final long batchLength = layersPerBuf * layerLen;
//        System.out.println("Splitting " + lastDim + " per " + layersPerBuf + " layers");
        UpdatablePNumberArray bufRe =
            (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(arrayRe.elementType(), batchLength);
        UpdatablePNumberArray bufIm = arrayIm == null ? null :
            (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(arrayIm.elementType(), batchLength);
        long[] subDim = dimensions.clone();
        for (long k = 0, disp = 0; k < lastDim; k += layersPerBuf, disp += batchLength) {
            final long lpb = Math.min(layersPerBuf, lastDim - k);
            subDim[subDim.length - 1] = lpb;
            final long bl = lpb * layerLen; // current batch length
            UpdatablePNumberArray subArrayRe = (UpdatablePNumberArray)arrayRe.subArr(disp, bl);
            UpdatablePNumberArray subArrayIm = arrayIm == null ? null :
                (UpdatablePNumberArray)arrayIm.subArr(disp, bl);
            bufRe.copy(subArrayRe);
            if (arrayIm != null) {
                bufIm.copy(subArrayIm);
            }
            transformHorizontalRecursively(context == null ? null : context.part(k, k + lpb, lastDim),
                bufRe, bufIm, subDim, inverse, numberOfTasks);
            subArrayRe.copy(bufRe);
            if (arrayIm != null) {
                subArrayIm.copy(bufIm);
            }
        }
    }

    private void transformVertical(ArrayContext context,
        UpdatablePNumberArray arrayRe, UpdatablePNumberArray arrayIm, long[] dimensions,
        boolean inverse, int numberOfTasks)
    {
        assert dimensions.length >= 2;
        final long[] layerDims = JArrays.copyOfRange(dimensions, 0, dimensions.length - 1);
        final long layerLen = Arrays.longMul(layerDims);
        final long lastDim = dimensions[dimensions.length - 1];
        final double elementSize = (arrayRe.bitsPerElement() + (arrayIm == null ? 0 : arrayIm.bitsPerElement())) / 8.0;
        final long bufLen = (long)(Math.max(maxTempJavaMemory(), 0) / elementSize);
        long columnLength = lastDim;
        for (int k = 0; k < dimensions.length - 2; k++) {
            columnLength *= dimensions[k];
        }
        boolean direct = areDirect(arrayRe, arrayIm);
        if (BUFFERING_LARGE_MATRICES && columnLength <= bufLen / 2 && !direct) {
            // (there should be at least 2 columns in a buffer; we do not use 2*layerLen operator to avoid overflow)
            transformVerticalWithSplitting(context,
                arrayRe, arrayIm, dimensions, columnLength, bufLen, inverse, numberOfTasks);
        } else {
            transformVerticalBaseAlgorithm(context,
                arrayRe, arrayIm, lastDim, layerLen, inverse, direct ? numberOfTasks : 1);
        }
    }

    private void transformVerticalBaseAlgorithm(ArrayContext context,
        final UpdatablePNumberArray arrayRe, final UpdatablePNumberArray arrayIm,
        final long lastDim, final long layerLen,
        final boolean inverse, int numberOfTasks)
    {
        numberOfTasks = (int) Math.min(numberOfTasks, layerLen); // note: not lastDim!
        MemoryModel mm = context == null ? null : context.getMemoryModel();
        if (numberOfTasks <= 1) { // maybe 0
            SampleArray samples = arrayIm == null ?
                RealVectorSampleArray.asSampleArray(mm, arrayRe, layerLen, layerLen, lastDim) :
                ComplexVectorSampleArray.asSampleArray(mm, arrayRe, arrayIm, layerLen, layerLen, lastDim);
            transform(context, samples, inverse);
        } else {
            assert areDirect(arrayRe, arrayIm);
            final Runnable[] tasks = new Runnable[numberOfTasks];
            final double layerStep = (double) layerLen / (double) numberOfTasks;
            long layerDelimiter = 0;
            for (int threadIndex = 0; threadIndex < tasks.length; threadIndex++) {
                final long layerFrom = layerDelimiter;
                layerDelimiter = threadIndex == tasks.length - 1 ? layerLen : (long) ((threadIndex + 1) * layerStep);
                final long layerTo = layerDelimiter;
                final ArrayContext contextNoProgress = context == null ? null : context.noProgressVersion();
                final SampleArray samples = arrayIm == null ?
                    RealVectorSampleArray.asSampleArray(mm,
                        (UpdatablePNumberArray) arrayRe.subArray(layerFrom, arrayRe.length()),
                        layerTo - layerFrom, layerLen, lastDim) :
                    ComplexVectorSampleArray.asSampleArray(mm,
                        (UpdatablePNumberArray) arrayRe.subArray(layerFrom, arrayRe.length()),
                        (UpdatablePNumberArray) arrayIm.subArray(layerFrom, arrayIm.length()),
                        layerTo - layerFrom, layerLen, lastDim);
                tasks[threadIndex] = new Runnable() {
                    public void run() {
                        transform(contextNoProgress, samples, inverse);
                    }
                };
            }
            Arrays.getThreadPoolFactory(context).performTasks(tasks);
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(null, lastDim, lastDim);
            }
        }
    }

    private void transformVerticalWithSplitting(ArrayContext context,
        UpdatablePNumberArray arrayRe, UpdatablePNumberArray arrayIm,
        long[] dimensions, long columnLength, long bufLen,
        boolean inverse, int numberOfTasks)
    {
        assert columnLength <= bufLen / 2; // so, the splitting has a sense
        final long previousDim = dimensions[dimensions.length - 2];
        final long columnsPerBuf = bufLen / columnLength;
        assert columnsPerBuf > 1; // because 2 * layerLen <= bufLen
        final long batchLength = columnsPerBuf * columnLength;
//        System.out.println("Splitting " + previousDim + " per " + columnsPerBuf + " columns");
        UpdatablePNumberArray bufRe =
            (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(arrayRe.elementType(),
                batchLength);
        UpdatablePNumberArray bufIm = arrayIm == null ? null :
            (UpdatablePNumberArray) Arrays.SMM.newUnresizableArray(arrayIm.elementType(), batchLength);
        Matrix<? extends UpdatablePNumberArray> matrixRe = Matrices.matrix(arrayRe, dimensions);
        Matrix<? extends UpdatablePNumberArray> matrixIm = arrayIm == null ? null :
            Matrices.matrix(arrayIm, dimensions);
        long[] subPos = new long[dimensions.length];
        long[] subDim = dimensions.clone();
        for (long k = 0; k < previousDim; k += columnsPerBuf) {
            final long cpb = Math.min(columnsPerBuf, previousDim - k);
            subPos[dimensions.length - 2] = k;
            subDim[dimensions.length - 2] = cpb;
            final long bl = cpb * columnLength; // current batch length
            Matrix<? extends UpdatablePNumberArray> matrixBufRe = Matrices.matrix(
                bl == batchLength ? bufRe : (UpdatablePNumberArray)bufRe.subArr(0, bl), subDim);
            Matrix<? extends UpdatablePNumberArray> matrixBufIm = arrayIm == null ? null : Matrices.matrix(
                bl == batchLength ? bufIm : (UpdatablePNumberArray)bufIm.subArr(0, bl), subDim);
            Matrix<? extends UpdatablePNumberArray> subMatrixRe = matrixRe.subMatr(subPos, subDim);
            Matrix<? extends UpdatablePNumberArray> subMatrixIm = arrayIm == null ? null :
                matrixIm.subMatr(subPos, subDim);
            matrixBufRe.array().copy(subMatrixRe.array());
            if (arrayIm != null) {
                matrixBufIm.array().copy(subMatrixIm.array());
            }
            transformVertical(context == null ? null : context.part(k, k + cpb, previousDim),
                bufRe, bufIm, subDim, inverse, numberOfTasks);
            subMatrixRe.array().copy(matrixBufRe.array());
            if (arrayIm != null) {
                subMatrixIm.array().copy(matrixBufIm.array());
            }
        }
    }

    static boolean areDirect(Array arrayRe, Array arrayIm) {
        return arrayRe instanceof DirectAccessible && ((DirectAccessible)arrayRe).hasJavaArray()
            && (arrayIm == null ||
            (arrayIm instanceof DirectAccessible && ((DirectAccessible)arrayIm).hasJavaArray()));
    }
}
