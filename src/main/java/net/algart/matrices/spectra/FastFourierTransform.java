/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.matrices.spectra.ComplexScalarSampleArray.*;

/**
 * <p><i>Fast Fourier transform</i> (FFT). This class implements traditional one- and multidimensional FFT algorithm
 * over an abstract {@link SampleArray} and 1-, 2- or multidimensional {@link Matrix AlgART numeric matrices}.
 * All samples must be complex to be processed by this class ({@link #areComplexSamplesRequired()} method
 * returns <tt>true</tt>). For needs of spectral processing real arrays and matrices, in most cases
 * you should use {@link SeparableFastHartleyTransform} class.</p>
 *
 * <p>More precisely, this class implements the classic fast "butterfly" algorithm (FFT) for calculating
 * <i>discrete Fourier transform</i> (DFT), described at
 * <nobr><a href="http://en.wikipedia.org/wiki/Discrete_Fourier_transform"
 * >http://en.wikipedia.org/wiki/Discrete_Fourier_transform</a></nobr>.</p>
 *
 * <p>Namely, let <i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>N</i>&minus;1</sub> are some complex
 * samples (represented by abstract {@link SampleArray}), and
 * <i>F</i><sub>0</sub>,<i>F</i><sub>1</sub>,...,<i>F</i><sub><i>N</i>&minus;1</sub> are their Fourier spectrum:
 * the result of DFT. Let <b><i>i</i></b> is the usual imaginary unit.
 * This class implements two possible definitions of DFT:</p>
 *
 * <ol>
 * <li>direct transform is
 * <i>F<sub>k</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>x<sub>n</sub></i><i>e</i><sup>&minus;2<i>kn</i>&pi;<b><i>i</i></b>/<i>N</i></sup>,
 * inverse transform is
 * <i>x<sub>n</sub></i> = <i>N</i><sup>&nbsp;&minus;1</sup>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>k</i>&lt;<i>N</i>)</sub></sub>
 * <i>F<sub>k</sub></i><i>e</i><sup>2<i>kn</i>&pi;<b><i>i</i></b>/<i>N</i></sup>.
 * </li>
 *
 * <li>direct transform is
 * <i>F<sub>k</sub></i> = <i>N</i><sup>&nbsp;&minus;1</sup>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>x<sub>n</sub></i><i>e</i><sup>&minus;2<i>kn</i>&pi;<b><i>i</i></b>/<i>N</i></sup>,
 * inverse transform is
 * <i>x<sub>n</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>k</i>&lt;<i>N</i>)</sub></sub>
 * <i>F<sub>k</sub></i><i>e</i><sup>2<i>kn</i>&pi;<b><i>i</i></b>/<i>N</i></sup>;
 * </li>
 * </ol>
 *
 * <p>The only difference is when to normalize the result: while inverse transform (case 1) or direct transform
 * (case 2). The Wikipedia offers formulas of the 1st case. This class allows to calculate both variants:
 * the 1st case is chosen if the <tt>normalizeDirectTransform</tt> argument of the constructors is <tt>false</tt>
 * or if this class is created by a constructor without this argument (it is the default behaviour),
 * the 2nd case is chosen  if the <tt>normalizeDirectTransform</tt> argument of the constructors is <tt>true</tt>.</p>
 *
 * <p>The formulas above correspond to one-dimensional transforms and specify the results of
 * {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform} methods.
 * They are generalized to multidimensional case by default algorithms, implemented in
 * {@link AbstractSpectralTransform} class, i.e. by applying the transform separably to each dimension.
 * It is the traditional way of multidimensional generalizing Fourier transformations.</p>
 *
 * <p>One-dimensional Fourier transform, defined by the formulas above, complies with the <i>convolution theorem</i>.
 * Namely, let <i>p</i><sub>0</sub>,<i>p</i><sub>1</sub>,...,<i>p</i><sub><i>N</i>&minus;1</sub> is the first complex
 * numeric function, <i>q</i><sub>0</sub>,<i>q</i><sub>1</sub>,...,<i>q</i><sub><i>N</i>&minus;1</sub> is
 * the second function, and <i>c</i><sub>0</sub>,<i>c</i><sub>1</sub>,...,<i>c</i><sub><i>N</i>&minus;1</sub> is
 * their <i>convolution</i>, defined as:</p>
 *
 * <blockquote>
 * <i>c<sub>k</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>p<sub>n</sub></i><i>q</i><sub>(<i>k</i>&minus;<i>n</i>)&nbsp;mod&nbsp;<i>N</i></sub>
 * </blockquote>
 *
 * <p>(here (<i>k</i>&minus;<i>n</i>)&nbsp;mod&nbsp;<i>N</i> means
 * <nobr>(<i>k</i>&minus;<i>n</i>&lt;0 ? <i>k</i>&minus;<i>n</i>+<i>N</i> : <i>k</i>&minus;<i>n</i>)</nobr>).
 * Also, let <i>P</i><sub>0</sub>,<i>P</i><sub>1</sub>,...,<i>P</i><sub><i>N</i>&minus;1</sub>,
 * <i>Q</i><sub>0</sub>,<i>Q</i><sub>1</sub>,...,<i>Q</i><sub><i>N</i>&minus;1</sub> and
 * <i>C</i><sub>0</sub>,<i>C</i><sub>1</sub>,...,<i>C</i><sub><i>N</i>&minus;1</sub> are
 * Fourier spectra of these functions. Then:
 *
 * <ol type="A">
 * <li><i>C<sub>k</sub></i> = <i>P<sub>k</sub></i><i>Q<sub>k</sub></i>
 * (usual complex product of complex numbers <i>P<sub>k</sub></i> and <i>Q<sub>k</sub></i>),
 * if the spectra were calculated according formula 1 above (default method);</li>
 *
 * <li><i>C<sub>k</sub></i> = <i>N</i><i>P<sub>k</sub></i><i>Q<sub>k</sub></i>,
 * if the spectra were calculated according formula 2 above.</li>
 * </ol>
 *
 * <p>The similar formulas are correct for any number of dimensions: convolution of samples corresponds to
 * complex product of spectra.</p>
 *
 * <p>This class contains the method
 * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix, Matrix, Matrix, Matrix)},
 * which calculates a spectrum of convolution <i>C</i> for the given spectra <i>P</i> and <i>Q</i>
 * of two source numeric matrices <i>x</i> and <i>y</i> according the formula A
 * (and its generalization for any number of dimensions).</p>
 *
 * <p>Please note: in the one-dimensional case, the spectral transofmation algorithms, implemented by
 * {@link #directTransformMatrix directTransformMatrix} / {@link #inverseTransformMatrix inverseTransformMatrix}
 * methods of this class, work with normal (i.e. high) performance only if
 * the passed <nobr>one-dimensional</nobr> AlgART matrices are stored in {@link SimpleMemoryModel}
 * (more precisely, if they are {@link DirectAccessible directly accessible}).
 * In other case, each access to every sample leads to calling accessing methods
 * {@link PArray#getDouble(long) getDouble} and {@link UpdatablePArray#setDouble(long, double) setDouble},
 * which can work slowly in non-simple memory models like {@link LargeMemoryModel}. There is the same problem for
 * {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform} methods, if the passed
 * sample arrays are created via {@link RealScalarSampleArray#asSampleArray RealScalarSampleArray.asSampleArray}
 * or {@link ComplexScalarSampleArray#asSampleArray ComplexScalarSampleArray.asSampleArray} methods on the base of
 * updatable AlgART arrays, created by memory model other than {@link SimpleMemoryModel}.</p>
 *
 * <p>For <i>n</i>-dimensional matrices (<i>n</i>&ge;2), this problem usually does not occur at all, even for non-simple
 * memory models, if you use standard implementations of
 * {@link #directTransformMatrix directTransformMatrix} / {@link #inverseTransformMatrix inverseTransformMatrix}
 * from {@link AbstractSpectralTransform} class: these implementations automatically download necessary parts
 * of the matrix into {@link SimpleMemoryModel}.
 * This problem also does not occur while using
 * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix, Matrix, Matrix, Matrix)} method,
 * if all processed matrices have the same <tt>float</tt> or <tt>double</tt> element types.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class FastFourierTransform extends AbstractSpectralTransform implements SpectralTransform {

    static final boolean DEFAULT_NORMALIZE_DIRECT_TRANSFORM = false;

    final boolean normalizeDirectTransform;

    /**
     * Creates a new instance of this class, performing Fourier transform according to the formula 1 from
     * the {@link FastFourierTransform comments to this class}.
     * Equivalent to {@link #FastFourierTransform(boolean) FastFourierTransform(false)}.
     *
     * @see #FastFourierTransform(long)
     */
    public FastFourierTransform() {
        this.normalizeDirectTransform = DEFAULT_NORMALIZE_DIRECT_TRANSFORM;
    }

    /**
     * Creates a new instance of this class, performing Fourier transform according to the formula 1 from
     * the {@link FastFourierTransform comments to this class}.
     *
     * <p>The <tt>maxTempJavaMemory</tt> argument specifies the amount of Java memory (heap),
     * that can be used by methods of this class for internal needs. It is passed to the corresponding
     * constructor of {@link AbstractSpectralTransform}: see
     * {@link AbstractSpectralTransform#AbstractSpectralTransform(long) comments to that constructor}.
     *
     * @param maxTempJavaMemory desired maximal amount of Java memory, in bytes, allowed for allocating
     *                          by methods of this class for internal needs.
     * @see #FastFourierTransform()
     */
    public FastFourierTransform(long maxTempJavaMemory) {
        super(maxTempJavaMemory);
        this.normalizeDirectTransform = DEFAULT_NORMALIZE_DIRECT_TRANSFORM;
    }

    /**
     * Creates a new instance of this class, performing Fourier transform according either to the formula 1 from
     * the {@link FastFourierTransform comments to this class}, if <tt>normalizeDirectTransform</tt> argument is
     * <tt>false</tt>, or to the formula 2, if this argument is <tt>true</tt>.
     * The default value, used by the constructors without <tt>normalizeDirectTransform</tt> argument,
     * is <tt>false</tt>.
     *
     * <p>Please note: the value of <tt>normalizeDirectTransform</tt> argument affects only the transformation
     * methods {@link #directTransform directTransform}, {@link #inverseTransform inverseTransform},
     * {@link #directTransformMatrix directTransformMatrix}, {@link #inverseTransformMatrix inverseTransformMatrix}.
     * This value does not matter in {@link #spectrumOfConvolution spectrumOfConvolution} method.
     *
     * @param normalizeDirectTransform <tt>true</tt> if you want to perform normalization (division by the number of
     *                                 samples <i>N</i>) after the direct transform, <tt>false</tt> (the default value)
     *                                 if you want to perform normalization after the inverse transform.
     * @see #FastFourierTransform(boolean, long)
     */
    public FastFourierTransform(boolean normalizeDirectTransform) {
        this.normalizeDirectTransform = normalizeDirectTransform;
    }

    /**
     * Creates a new instance of this class, performing Fourier transform according either to the formula 1 from
     * the {@link FastFourierTransform comments to this class}, if <tt>normalizeDirectTransform</tt> argument is
     * <tt>false</tt>, or to the formula 2, if this argument is <tt>true</tt>.
     * The default value, used by the constructors without <tt>normalizeDirectTransform</tt> argument,
     * is <tt>false</tt>.
     *
     * <p>Please note: the value of <tt>normalizeDirectTransform</tt> argument affects only the transformation
     * methods {@link #directTransform directTransform}, {@link #inverseTransform inverseTransform},
     * {@link #directTransformMatrix directTransformMatrix}, {@link #inverseTransformMatrix inverseTransformMatrix}.
     * This value does not matter in {@link #spectrumOfConvolution spectrumOfConvolution} method.
     *
     * <p>The <tt>maxTempJavaMemory</tt> argument specifies the amount of Java memory (heap),
     * that can be used by methods of this class for internal needs. It is passed to the corresponding
     * constructor of {@link AbstractSpectralTransform}: see
     * {@link AbstractSpectralTransform#AbstractSpectralTransform(long) comments to that constructor}.
     *
     * @param maxTempJavaMemory        desired maximal amount of Java memory, in bytes, allowed for allocating
     *                                 by methods of this class for internal needs.
     * @param normalizeDirectTransform <tt>true</tt> if you want to perform normalization (division by the number of
     *                                 samples <i>N</i>) after the direct transform, <tt>false</tt> (the default value)
     *                                 if you want to perform normalization after the inverse transform.
     * @see #FastFourierTransform(boolean)
     */
    public FastFourierTransform(boolean normalizeDirectTransform, long maxTempJavaMemory) {
        super(maxTempJavaMemory);
        this.normalizeDirectTransform = normalizeDirectTransform;
    }

    /**
     * Calculates <i>C</i> = <i>P</i>*<i>Q</i>, i&#46;e&#46; multiplies each element of the complex multidimensional
     * matrix <i>P</i> to the corresponding element of the complex multidimensional matrix <i>Q</i> and stores
     * result in the corresponding element of the complex multidimensional matrix <i>C</i>.
     * If the complex matrices <i>P</i> and <i>Q</i> are Fourier spectra of some matrices (real or complex)
     * <i>p</i> and <i>q</i>, then the resulting complex matrix <i>C</i> will contain the spectrum of
     * the <i>convolution</i> of <i>p</i> and <i>q</i> matrices.
     * See about the convolution theorem at <a href="http://en.wikipedia.org/wiki/Discrete_Fourier_transform"
     * >http://en.wikipedia.org/wiki/Discrete_Fourier_transform</a> and
     * in the {@link FastFourierTransform comments to this class}.
     *
     * <p>The complex matrix <i>P</i> is represented as a pair of AlgART matrices <tt>(pRe,pIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix <i>P</i>.
     * Similarly, the complex matrix <i>Q</i> is represented as a pair of AlgART matrices <tt>(qRe,qIm)</tt>,
     * and the complex matrix <i>C</i> is represented as a pair of AlgART matrices <tt>(cRe,cIm)</tt>.
     *
     * <p>All matrices, passed to this method, must have {@link Matrix#dimEquals(Matrix) equal dimensions}.
     * The {@link Matrix#elementType() element type} of the passed matrices can be different, but we recommend
     * using the same <tt>float</tt> or <tt>double</tt> element type for all matrices.
     * There are no restrictions for the dimensions of the passed matrices:
     * {@link #isLengthAllowed(long)} method is not used here.
     *
     * <p>This method works correctly, if you pass the same complex matrix as <i>P</i> and <i>Q</i>,
     * or as <i>P</i> and <i>C</i>, or as <i>Q</i> and <i>C</i>, or even as all three matrices.
     * So, you can calculate and return the result in one of the source matrices.
     *
     * <p>If you need to calculate the Fourier spectrum of convolution for a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param cRe     the real parts of the elements of the resulting matrix.
     * @param cIm     the imaginary parts of the elements of the resulting matrix.
     * @param pRe     the real parts of the elements of the 1st source matrix.
     * @param pIm     the imaginary parts of the elements of the 1st source matrix.
     * @param qRe     the real parts of the elements of the 2nd source matrix.
     * @param qIm     the imaginary parts of the elements of the 2nd source matrix.
     * @throws NullPointerException  if one of <tt>cRe</tt>, <tt>cIm</tt>, <tt>pRe</tt>, <tt>pIm</tt>,
     *                               <tt>qRe</tt>, <tt>qIm</tt> arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of passed matrices have different dimensions.
     */
    public void spectrumOfConvolution(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> cRe, Matrix<? extends UpdatablePNumberArray> cIm,
        Matrix<? extends PNumberArray> pRe, Matrix<? extends PNumberArray> pIm,
        Matrix<? extends PNumberArray> qRe, Matrix<? extends PNumberArray> qIm)
    {
        if (cRe == null)
            throw new NullPointerException("Null cRe argument");
        if (cIm == null)
            throw new NullPointerException("Null cIm argument");
        if (pRe == null)
            throw new NullPointerException("Null pRe argument");
        if (pIm == null)
            throw new NullPointerException("Null pIm argument");
        if (qRe == null)
            throw new NullPointerException("Null qRe argument");
        if (qIm == null)
            throw new NullPointerException("Null qIm argument");
        if (!cRe.dimEquals(cIm))
            throw new SizeMismatchException("cRe and cIm dimensions mismatch: cRe is " + cRe + ", cIm " + cIm);
        if (!pRe.dimEquals(cRe))
            throw new SizeMismatchException("cRe and pRe dimensions mismatch: cRe is " + cRe + ", pRe " + pRe);
        if (!pIm.dimEquals(cRe))
            throw new SizeMismatchException("cRe and pIm dimensions mismatch: cRe is " + cRe + ", pIm " + pIm);
        if (!qRe.dimEquals(cRe))
            throw new SizeMismatchException("cRe and qRe dimensions mismatch: cRe is " + cRe + ", qRe " + qRe);
        if (!qIm.dimEquals(cRe))
            throw new SizeMismatchException("cRe and qIm dimensions mismatch: cRe is " + cRe + ", qIm " + qIm);
        if (cRe.isTiled() && cIm.isTiled() && pRe.isTiled() && pIm.isTiled() && qRe.isTiled() && qIm.isTiled()) {
            long[] tileDimensions = cRe.tileDimensions();
            if (java.util.Arrays.equals(tileDimensions, cIm.tileDimensions())
                && java.util.Arrays.equals(tileDimensions, pRe.tileDimensions())
                && java.util.Arrays.equals(tileDimensions, pIm.tileDimensions())
                && java.util.Arrays.equals(tileDimensions, qRe.tileDimensions())
                && java.util.Arrays.equals(tileDimensions, qIm.tileDimensions()))
            {
                cRe = cRe.tileParent();
                cIm = cIm.tileParent();
                pRe = pRe.tileParent();
                pIm = pIm.tileParent();
                qRe = qRe.tileParent();
                qIm = qIm.tileParent();
            }
        }
        SpectraOfConvolution.fourierSpectrumOfConvolution(context,
            cRe.array(), cIm.array(), pRe.array(), pIm.array(), qRe.array(), qIm.array());
    }


    @Override
    public final boolean isLengthAllowed(long length) {
        return (length & (length - 1)) == 0;
    }

    @Override
    public boolean areComplexSamplesRequired() {
        return true;
    }

    @Override
    protected String unallowedLengthMessage() {
        return "FFT algorithm can process only 2^k elements";
    }

    @Override
    protected final void transform(ArrayContext context, SampleArray samples, boolean inverse) {
        assert samples.isComplex();
        assert isLengthAllowed(samples.length());
        boolean normalize = normalizeDirectTransform ? !inverse : inverse;

//        long t1 = System.nanoTime();
        ReverseBits.reorderForButterfly(context == null ? null : context.part(0.0, 0.2), samples);

//        long t2 = System.nanoTime();
        fftMainLoop(context == null ? null : context.part(0.2, normalize ? 0.95 : 1.0), samples, inverse);

//        long t3 = System.nanoTime();
        if (normalize) {
            fftNormalize(context == null ? null : context.part(0.95, 1.0), samples);
        }
//        long t4 = System.nanoTime();
//        System.out.printf("FFT time (common branch) %.3f ms reorder, %.3f ms main, %.3f normalizing%n",
//            (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6);
    }

    static void fftMainLoop(ArrayContext context, SampleArray samples, boolean inverse) {
        if (samples instanceof DirectZeroOffsetsComplexFloatSampleArray
            || samples instanceof DirectComplexFloatSampleArray)
        {
            fftJavaFloatMainLoop(context, samples, inverse);
        } else if (samples instanceof DirectZeroOffsetsComplexDoubleSampleArray
            || samples instanceof DirectComplexDoubleSampleArray)
        {
            fftJavaDoubleMainLoop(context, samples, inverse);
        } else {
            fftCommonMainLoop(context, samples, inverse);
        }
    }

    static void fftNormalize(ArrayContext context, SampleArray samples) {
        final long n = samples.length();
        final double mult = 1.0 / n;
        final long step = samples instanceof ComplexScalarSampleArray ? 16384 : 512;
        for (long i = 0; i < n; ) {
            final long iMax = i > n - step ? n : i + step; // "n - step" allows avoiding overflow
            samples.multiplyRangeByRealScalar(i, iMax, mult);
            i = iMax;
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(null, iMax, n);
            }
        }
    }

    private static void fftCommonMainLoop(ArrayContext context, SampleArray samples, boolean inverse) {
        final long n = samples.length();
        if (n == 0) {
            return; // avoiding assertion step==n
        }
        final int logN = 63 - Long.numberOfLeadingZeros(n);
        SampleArray work = samples.newCompatibleSamplesArray(1);
        long step = 1;
        for (int bitIndex = 0; bitIndex < logN; bitIndex++) {
            long halfStep = step;
            step *= 2;
            final boolean allAnglesInCache = halfStep <= RootsOfUnity.CACHE_SIZE;
            if (allAnglesInCache) { // optimization of speed by ~30% and also better precision
//                System.out.println("CACHED");
                final int angleStep = (int)(RootsOfUnity.CACHE_SIZE / halfStep); // CACHE_SIZE corresponds to PI
                for (long i = 0; i < n; i += step) {
                    // 32-bit indexes are enough below in this case
                    for (int j = 0, angleIndex = 0; j < halfStep; j++, angleIndex += angleStep) {
                        // double wRe = RootsOfUnity.quickCos(angleIndex); // slow version
                        // double wIm = RootsOfUnity.quickSin(angleIndex); // slow version
                        double wRe = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                            RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                            -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                        double wIm = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                            RootsOfUnity.SINE_CACHE[angleIndex] :
                            RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        // (wRe, wIm) = exp(I*j*2*PI/step) = (cos(j*2*PI/step),  sin(j*2*PI/step))
                        if (!inverse) {
                            wIm = -wIm;
                        }
                        long l = i + j;
                        long r = l + halfStep;
                        work.multiplyByScalar(0, samples, r, wRe, wIm);
                        samples.sub(r, l, work, 0);
                        samples.add(l, l, work, 0);
                    }
                }
            } else { // ordinary branch
//                System.out.println("NOT CACHED");
                final double rotationAngle = inverse ? Math.PI / halfStep : -Math.PI / halfStep;
                final double rootSinHalf = Math.sin(0.5 * rotationAngle);
                final double rootReM1 = -2.0 * rootSinHalf * rootSinHalf; // cos(rotationAngle)-1
                final double rootIm = Math.sin(rotationAngle); // (rootReM1,rootIm)=root-1.0, root=exp(I*rotationAngle)
                for (long i = 0; i < n; i += step) {
                    double wRe = 1.0;
                    double wIm = 0.0;
                    for (long j = 0; j < halfStep; j++) {
                        // (wRe, wIm) = exp(-I*j*2*PI/step) = (cos(-j*2*PI/step),  sin(-j*2*PI/step)), or + for inverse
                        long l = i + j;
                        long r = l + halfStep;
                        work.multiplyByScalar(0, samples, r, wRe, wIm);
                        samples.sub(r, l, work, 0);
                        samples.add(l, l, work, 0);
                        if ((l & 15) == 15) {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            wRe = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            wIm = Math.sin(angle);
                        } else {
                            double re = wRe * rootReM1 - wIm * rootIm;
                            double im = wRe * rootIm + wIm * rootReM1; //(re,im) = w * (root-1)
                            wRe += re;
                            wIm += im;
                        }
                    }
                }
            }
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(null, bitIndex, logN);
            }
        }
        assert step == n : "step = " + step + ", n = " + n;
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\(([^)]+)\) ==> $1 ]]
    private static void fftJavaFloatMainLoop(ArrayContext context, SampleArray samples, boolean inverse) {
        final long n = samples.length();
        assert n <= Integer.MAX_VALUE;
        if (n == 0) {
            return; // avoiding assertion step==n
        }
        final int logN = 63 - Long.numberOfLeadingZeros(n);

        final float[] samplesRe = samples instanceof DirectZeroOffsetsComplexFloatSampleArray ?
            ((DirectZeroOffsetsComplexFloatSampleArray)samples).samplesRe :
            ((DirectComplexFloatSampleArray)samples).samplesRe;
        final int ofsRe = samples instanceof DirectZeroOffsetsComplexFloatSampleArray ? 0 :
            ((ComplexScalarSampleArray.DirectComplexFloatSampleArray)samples).ofsRe;
        final float[] samplesIm = samples instanceof DirectZeroOffsetsComplexFloatSampleArray ?
            ((DirectZeroOffsetsComplexFloatSampleArray)samples).samplesIm :
            ((DirectComplexFloatSampleArray)samples).samplesIm;
        final int ofsIm = samples instanceof DirectZeroOffsetsComplexFloatSampleArray ? 0 :
            ((DirectComplexFloatSampleArray)samples).ofsIm;
        int step = 1;
        for (int bitIndex = 0; bitIndex < logN; bitIndex++) {
            int halfStep = step;
            step *= 2;
            final boolean allAnglesInCache = halfStep <= RootsOfUnity.CACHE_SIZE;
            if (allAnglesInCache) { // optimization of speed by ~30% and also better precision
//                System.out.println("CACHED");
                final int angleStep = RootsOfUnity.CACHE_SIZE / halfStep; // CACHE_SIZE corresponds to PI
                for (int i = 0; i < n; i += step) {
                    // 32-bit indexes are enough below in this case
                    for (int j = 0, angleIndex = 0; j < halfStep; j++, angleIndex += angleStep) {
                        // double wRe = RootsOfUnity.quickCos(angleIndex); // slow version
                        // double wIm = RootsOfUnity.quickSin(angleIndex); // slow version
                        double wRe = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                            RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                            -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                        double wIm = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                            RootsOfUnity.SINE_CACHE[angleIndex] :
                            RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        // (wRe, wIm) = exp(I*j*2*PI/step) = (cos(j*2*PI/step),  sin(j*2*PI/step))
                        if (!inverse) {
                            wIm = -wIm;
                        }
                        int lRe = ofsRe + i + j, lIm = ofsIm + i + j;
                        int rRe = lRe + halfStep, rIm = lIm + halfStep;
                        double re = samplesRe[rRe];
                        double im = samplesIm[rIm];
                        float workRe = (float)(re * wRe - im * wIm);
                        float workIm = (float)(re * wIm + im * wRe);
                        samplesRe[rRe] = samplesRe[lRe] - workRe;
                        samplesIm[rIm] = samplesIm[lIm] - workIm;
                        samplesRe[lRe] += workRe;
                        samplesIm[lIm] += workIm;
                    }
                }
            } else { // ordinary branch
//                System.out.println("NOT CACHED");
                final double rotationAngle = inverse ? Math.PI / halfStep : -Math.PI / halfStep;
                final double rootSinHalf = Math.sin(0.5 * rotationAngle);
                final double rootReM1 = -2.0 * rootSinHalf * rootSinHalf; // cos(rotationAngle)-1
                final double rootIm = Math.sin(rotationAngle); // (rootReM1,rootIm)=root-1.0, root=exp(I*rotationAngle)
                for (int i = 0; i < n; i += step) {
                    double wRe = 1.0;
                    double wIm = 0.0;
                    for (int j = 0; j < halfStep; j++) {
                        // (wRe, wIm) = exp(-I*j*2*PI/step) = (cos(-j*2*PI/step),  sin(-j*2*PI/step)), or + for inverse
                        int lRe = ofsRe + i + j, lIm = ofsIm + i + j;
                        int rRe = lRe + halfStep, rIm = lIm + halfStep;
                        double re = samplesRe[rRe];
                        double im = samplesIm[rIm];
                        float workRe = (float)(re * wRe - im * wIm);
                        float workIm = (float)(re * wIm + im * wRe);
                        samplesRe[rRe] = samplesRe[lRe] - workRe;
                        samplesIm[rIm] = samplesIm[lIm] - workIm;
                        samplesRe[lRe] += workRe;
                        samplesIm[lIm] += workIm;
                        if (((i + j) & 15) == 15) {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            wRe = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            wIm = Math.sin(angle);
                        } else {
                            re = wRe * rootReM1 - wIm * rootIm;
                            im = wRe * rootIm + wIm * rootReM1; //(re,im) = w * (root-1)
                            wRe += re;
                            wIm += im;
                        }
                    }
                }
            }
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(null, bitIndex, logN);
            }
        }
        assert step == n : "step = " + step + ", n = " + n;
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void fftJavaDoubleMainLoop(ArrayContext context, SampleArray samples, boolean inverse) {
        final long n = samples.length();
        assert n <= Integer.MAX_VALUE;
        if (n == 0) {
            return; // avoiding assertion step==n
        }
        final int logN = 63 - Long.numberOfLeadingZeros(n);

        final double[] samplesRe = samples instanceof DirectZeroOffsetsComplexDoubleSampleArray ?
            ((DirectZeroOffsetsComplexDoubleSampleArray)samples).samplesRe :
            ((DirectComplexDoubleSampleArray)samples).samplesRe;
        final int ofsRe = samples instanceof DirectZeroOffsetsComplexDoubleSampleArray ? 0 :
            ((ComplexScalarSampleArray.DirectComplexDoubleSampleArray)samples).ofsRe;
        final double[] samplesIm = samples instanceof DirectZeroOffsetsComplexDoubleSampleArray ?
            ((DirectZeroOffsetsComplexDoubleSampleArray)samples).samplesIm :
            ((DirectComplexDoubleSampleArray)samples).samplesIm;
        final int ofsIm = samples instanceof DirectZeroOffsetsComplexDoubleSampleArray ? 0 :
            ((DirectComplexDoubleSampleArray)samples).ofsIm;
        int step = 1;
        for (int bitIndex = 0; bitIndex < logN; bitIndex++) {
            int halfStep = step;
            step *= 2;
            final boolean allAnglesInCache = halfStep <= RootsOfUnity.CACHE_SIZE;
            if (allAnglesInCache) { // optimization of speed by ~30% and also better precision
//                System.out.println("CACHED");
                final int angleStep = RootsOfUnity.CACHE_SIZE / halfStep; // CACHE_SIZE corresponds to PI
                for (int i = 0; i < n; i += step) {
                    // 32-bit indexes are enough below in this case
                    for (int j = 0, angleIndex = 0; j < halfStep; j++, angleIndex += angleStep) {
                        // double wRe = RootsOfUnity.quickCos(angleIndex); // slow version
                        // double wIm = RootsOfUnity.quickSin(angleIndex); // slow version
                        double wRe = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                            RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                            -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                        double wIm = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                            RootsOfUnity.SINE_CACHE[angleIndex] :
                            RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        // (wRe, wIm) = exp(I*j*2*PI/step) = (cos(j*2*PI/step),  sin(j*2*PI/step))
                        if (!inverse) {
                            wIm = -wIm;
                        }
                        int lRe = ofsRe + i + j, lIm = ofsIm + i + j;
                        int rRe = lRe + halfStep, rIm = lIm + halfStep;
                        double re = samplesRe[rRe];
                        double im = samplesIm[rIm];
                        double workRe = re * wRe - im * wIm;
                        double workIm = re * wIm + im * wRe;
                        samplesRe[rRe] = samplesRe[lRe] - workRe;
                        samplesIm[rIm] = samplesIm[lIm] - workIm;
                        samplesRe[lRe] += workRe;
                        samplesIm[lIm] += workIm;
                    }
                }
            } else { // ordinary branch
//                System.out.println("NOT CACHED");
                final double rotationAngle = inverse ? Math.PI / halfStep : -Math.PI / halfStep;
                final double rootSinHalf = Math.sin(0.5 * rotationAngle);
                final double rootReM1 = -2.0 * rootSinHalf * rootSinHalf; // cos(rotationAngle)-1
                final double rootIm = Math.sin(rotationAngle); // (rootReM1,rootIm)=root-1.0, root=exp(I*rotationAngle)
                for (int i = 0; i < n; i += step) {
                    double wRe = 1.0;
                    double wIm = 0.0;
                    for (int j = 0; j < halfStep; j++) {
                        // (wRe, wIm) = exp(-I*j*2*PI/step) = (cos(-j*2*PI/step),  sin(-j*2*PI/step)), or + for inverse
                        int lRe = ofsRe + i + j, lIm = ofsIm + i + j;
                        int rRe = lRe + halfStep, rIm = lIm + halfStep;
                        double re = samplesRe[rRe];
                        double im = samplesIm[rIm];
                        double workRe = re * wRe - im * wIm;
                        double workIm = re * wIm + im * wRe;
                        samplesRe[rRe] = samplesRe[lRe] - workRe;
                        samplesIm[rIm] = samplesIm[lIm] - workIm;
                        samplesRe[lRe] += workRe;
                        samplesIm[lIm] += workIm;
                        if (((i + j) & 15) == 15) {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            wRe = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            wIm = Math.sin(angle);
                        } else {
                            re = wRe * rootReM1 - wIm * rootIm;
                            im = wRe * rootIm + wIm * rootReM1; //(re,im) = w * (root-1)
                            wRe += re;
                            wIm += im;
                        }
                    }
                }
            }
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(null, bitIndex, logN);
            }
        }
        assert step == n : "step = " + step + ", n = " + n;
    }
    //[[Repeat.AutoGeneratedEnd]]
}
