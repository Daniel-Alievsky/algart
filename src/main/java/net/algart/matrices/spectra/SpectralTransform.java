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

/**
 * <p>Spectral transform: Fourier, Hartley, Hadamar, etc.</p>
 *
 * <p>This interface is an abstraction, allowing to perform two basic operations: <i>direct transform</i>
 * and <i>inverse transform</i> over an array or <i>n</i>-dimensional matrix, consisting of some <i>samples</i>.
 * The transformed samples can be represented in two forms:</p>
 *
 * <ol>
 * <li>maximally abstract 1-dimensional array of samples, stored in some implementation
 * of {@link SampleArray} interface;</li>
 *
 * <li><i>n</i>-dimensional matrix (N=1,2,3,...) of real / complex numbers, stored in one / two
 * {@link Matrix AlgART numeric matrices}.</li>
 * </ol>
 *
 * <p>In the first case, the transformation is performed by
 * {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform} methods,
 * in the second case &mdash; by {@link #directTransformMatrix directTransformMatrix}
 * / {@link #inverseTransformMatrix inverseTransformMatrix} methods.</p>
 *
 * <p>This package, in the current implementation, offers implementations of two transforms:</p>
 *
 * <ul>
 * <li>classic Fast Fourier Transform (FFT) in {@link FastFourierTransform} class;</li>
 *
 * <li>separable Fast Hartley Transform (SFHT) in {@link SeparableFastHartleyTransform} class.</li>
 * </ul>
 *
 * <p>These classes also contain additional methods, allowing to convert the Hartley spectrum
 * to Fourier one and vise versa, and also to calculate the spectrum of the convolution of two matrices
 * (real or complex) on the base of spectra of the source matrices.</p>
 *
 * <p>Some implementations of this interface can work with complex numbers only, other implementations can process
 * both complex and real samples. In this package, {@link FastFourierTransform} class requires complex samples,
 * but {@link SeparableFastHartleyTransform} can be applied to real samples also.
 * You can check, whether complex numbers are required or no, by {@link #areComplexSamplesRequired()} method.
 * If you work with real numbers only, in most cases you don't need {@link FastFourierTransform},
 * because the separable Hartley transform works essentially faster and allows to solve the same tasks,
 * including getting the Fourier spectrum.</p>
 *
 * <p>The implementations of this interface can require that the length of the processed sample arrays
 * or dimensions of the processed matrices should fulfil some restrictions. In particular, both implementations,
 * offered by this package, require that all dimensions should be powers of two (2<sup><i>k</i></sup>).
 * To process an array or matrix with another sizes, you must append it to the nearest allowed sizes,
 * usually by zeroes or by another form of continuation: see {@link net.algart.arrays.Matrix.ContinuationMode} class.
 * You can check, whether some size is allowed or no, by {@link #isLengthAllowed(long)} method.</p>
 *
 * <p>All numeric algorithms, performed by methods of this interface and its implementations,
 * are performed over floating-point numbers, corresponding to <tt>float</tt> or <tt>double</tt> Java types.
 * Single precision is used, if the passed sample arrays are represented by <tt>float</tt> element type;
 * double precision is used, if the passed sample arrays are represented by <tt>double</tt> element type.
 * It is theoretically possible to process sample arrays, represented by fixed-point numbers,
 * alike Java <tt>int</tt>, <tt>long</tt>, <tt>short</tt> and other types (for example,
 * if the passed AlgART matrix is <nobr>{@link Matrix Matrix &lt;? extends UpdatableIntArray&gt;}</nobr>
 * or if the passed {@link SampleArray} is {@link RealScalarSampleArray}, built on the base of
 * {@link UpdatableByteArray UpdatableByteArray}). In this situation,
 * calculations can be performed with some form of rounding, possible overflows lead to unspecified results
 * and algorithms can work slower.</p>
 *
 * <p>If some methods of this interface or its implementations have several arguments, representing arrays
 * of samples &mdash; for example, real and imaginary part in
 * {@link #directTransformMatrix directTransformMatrix}
 * / {@link #inverseTransformMatrix inverseTransformMatrix} methods,
 * or source and target spectra in
 * {@link FastFourierTransform#spectrumOfConvolution FastFourierTransform.spectrumOfConvolution} method,
 * or Fourier and Hartley spectra in
 * {@link SeparableFastHartleyTransform#separableHartleyToFourier
 * SeparableFastHartleyTransform.separableHartleyToFourier} method &mdash;
 * then all passed arrays usually have the same numeric precision (<tt>float</tt>, <tt>double</tt>
 * or some fixed-point type). But it is not a requirement: for example, you may store the real part
 * of the complex matrix in <nobr>{@link Matrix Matrix &lt;? extends UpdatableDoubleArray&gt;}</nobr>
 * and the imaginary part in <nobr>{@link Matrix Matrix &lt;? extends UpdatableFloatArray&gt;}</nobr>.
 * In any case, if such a method allocates some temporary numeric arrays,
 * it uses for them the precision of one of the passed arguments or better.</p>
 *
 * <p>Most of methods of this interface and its implementations work with an {@link ArrayContext array context},
 * passed to them in the first argument. This context is used as in
 * {@link net.algart.arrays.Arrays.ParallelExecutor} class for interruption, showing the progress,
 * allocating work memory (if necessary) and multiprocessing on several CPUs.
 * If this argument is <tt>null</tt>, then all temporary AlgART arrays are allocated by
 * {@link SimpleMemoryModel}, multiprocessing (when possible) is implemented by
 * {@link DefaultThreadPoolFactory} class, interrupting by the user is impossible
 * and showing the progress is not supported.</p>
 *
 * <p>The implementations of this class are not thread-safe, but <b>are thread-compatible</b>
 * and can be synchronized manually if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public interface SpectralTransform {
    /**
     * Returns <tt>true</tt> if the specified argument is an allowed dimension for arrays or matrices,
     * transformed by {@link #directTransform directTransform}, {@link #inverseTransform inverseTransform},
     * {@link #directTransformMatrix directTransformMatrix} or {@link #inverseTransformMatrix inverseTransformMatrix}
     * method.
     *
     * <p>More precisely, if this method returns <tt>false</tt> for the length of a sample array,
     * passed to 1st or 2nd methods, or for some dimension of some matrix, passed to 3rd or 4th method,
     * then those methods throw {@link IllegalArgumentException}.
     * In other case, those methods will process that passed data.
     *
     * <p>In both implementations of this interface, offered by this package,
     * this method returns <tt>true</tt> if the passed length is a power of two (2<sup><i>k</i></sup>).
     *
     * <p>If the <tt>length</tt> argument is negative, the result of this method is unspecified.
     * It is not a problem, because lengths of sample arrays and dimensions of AlgART matrices
     * cannot be negative.
     *
     * @param length the checked length or matrix dimension.
     * @return       whether the specified argument is an allowed dimension for arrays or matrices,
     *               trasformed by this transformation.
     */
    public boolean isLengthAllowed(long length);

    /**
     * Returns <tt>true</tt> if the transformation methods of this class ({@link #directTransform directTransform},
     * {@link #inverseTransform inverseTransform}, {@link #directTransformMatrix directTransformMatrix},
     * {@link #inverseTransformMatrix inverseTransformMatrix}) can process only complex samples,
     * <tt>false</tt> if the real samples are also allowed.
     *
     * <p>More precisely, if this method returns <tt>true</tt>,
     * then the methods {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform}
     * checks, whether {@link SampleArray#isComplex()} method returns <tt>true</tt> for the <tt>samples</tt> argument,
     * and the methods {@link #directTransformMatrix directTransformMatrix} /
     * {@link #inverseTransformMatrix inverseTransformMatrix} checks, whether the <tt>matrixIm</tt> argument is
     * not <tt>null</tt>. If this condition is not fulfilled, these methods throw
     * <tt>UnsupportedOperationException</tt>.
     * In other case, these methods work normally.
     *
     * <p>In implementations, offered by this package, this method returns <tt>true</tt>
     * in {@link FastFourierTransform} class and <tt>false</tt> in {@link SeparableFastHartleyTransform} class.
     *
     * @return <tt>true</tt> if this class can transform complex samples only,
     *         <tt>false</tt> if real samples can be transformed too.
     */
    public boolean areComplexSamplesRequired();


    /**
     * Direct transform of the passed sample array to its spectrum.
     * The resulting data are returned in the same sample array.
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
    public void directTransform(ArrayContext context, SampleArray samples);

    /**
     * Inverse transform of the spectrum back to the original sample array.
     * The resulting data are returned in the same sample array.
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
    public void inverseTransform(ArrayContext context, SampleArray samples);


    /**
     * Direct transform of the passed matrix of real or complex numbers to its spectrum.
     * The complex matrix is represented as a pair of AlgART matrices <tt>(matrixRe,matrixIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix.
     * The real matrix is represented as a single AlgART matrix <tt>matrixRe</tt>;
     * in this case, <tt>matrixIm</tt> argument must be <tt>null</tt>.
     * (It is allowed only if {@link #areComplexSamplesRequired()} method returns <tt>false</tt>.)
     * The resulting data are returned in the same AlgART matrices.
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
    public void directTransformMatrix(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> matrixRe,
        Matrix<? extends UpdatablePNumberArray> matrixIm);

    /**
     * Inverse transform of the spectrum back to the original matrix of real or complex numbers.
     * The complex matrix is represented as a pair of AlgART matrices <tt>(matrixRe,matrixIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix.
     * The real matrix is represented as a single AlgART matrix <tt>matrixRe</tt>;
     * in this case, <tt>matrixIm</tt> argument must be <tt>null</tt>.
     * (It is allowed only if {@link #areComplexSamplesRequired()} method returns <tt>false</tt>.)
     * The resulting data are returned in the same AlgART matrices.
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
    public void inverseTransformMatrix(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> matrixRe,
        Matrix<? extends UpdatablePNumberArray> matrixIm);
}
