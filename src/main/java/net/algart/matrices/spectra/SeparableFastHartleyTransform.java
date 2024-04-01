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

import java.util.Objects;

/**
 * <p><i>Fast Hartley transform</i> (FHT)
 * (in multidimensional case &mdash; the <i>separable</i> fast Hartley transform).
 * This class implements standard one-dimensional FHT algorithm over an abstract {@link SampleArray}.
 * It is generalized to multidimensional case by the simplest way, implemented in
 * {@link AbstractSpectralTransform} class (applying the transform separably to each dimension);
 * the resulting transformation for 2- or multidimensional {@link Matrix AlgART numeric matrices} is usually called
 * <i>separable fast Hartley transform</i> (SFHT).
 * The samples, processed by this class, can be both real or complex ({@link #areComplexSamplesRequired()} method
 * returns <tt>false</tt>). This class is especially useful in a case of real samples. In this case
 * it is performed faster than the classic FFT, {@link FastFourierTransform} class, because
 * there are no needs to allocate and process arrays of imaginary parts.
 * The simple relation between Hartley and Fourier transform (see below) allows to use this transform
 * almost in all areas where Fourier transform is applicable.</p>
 *
 * <p>More precisely, this class implements the classic fast "butterfly" algorithm (FHT) for calculating
 * <i>discrete Hartley transform</i> (DHT), described at
 * <nobr><a href="http://en.wikipedia.org/wiki/Discrete_Hartley_transform"
 * >http://en.wikipedia.org/wiki/Discrete_Hartley_transform</a></nobr>.</p>
 *
 * <p>Namely, let <i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>N</i>&minus;1</sub> are some
 * real or complex samples (represented by abstract {@link SampleArray}), and
 * <i>H</i><sub>0</sub>,<i>H</i><sub>1</sub>,...,<i>H</i><sub><i>N</i>&minus;1</sub> are their Hartley spectrum:
 * the result of DHT. Let's designate <nobr>cas &theta; = cos &theta; + sin &theta;</nobr>.
 * This class implements two possible definitions of DHT:</p>
 *
 * <ol>
 * <li>direct transform is
 * <i>H<sub>k</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>x<sub>n</sub></i>&nbsp;cas(2<i>kn</i>&pi;/<i>N</i>),
 * inverse transform is
 * <i>x<sub>n</sub></i> = <i>N</i><sup>&nbsp;&minus;1</sup>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>k</i>&lt;<i>N</i>)</sub></sub>
 * <i>H<sub>k</sub></i>&nbsp;cas(2<i>kn</i>&pi;/<i>N</i>).
 * </li>
 *
 * <li>direct transform is
 * <i>H<sub>k</sub></i> = <i>N</i><sup>&nbsp;&minus;1</sup>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>x<sub>n</sub></i>&nbsp;cas(2<i>kn</i>&pi;/<i>N</i>),
 * inverse transform is
 * <i>x<sub>n</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>k</i>&lt;<i>N</i>)</sub></sub>
 * <i>H<sub>k</sub></i>&nbsp;cas(2<i>kn</i>&pi;/<i>N</i>).
 * </li>
 * </ol>
 *
 * <p>The only difference is when to normalize the result: while inverse transform (case 1) or direct transform
 * (case 2). The Wikipedia offers formulas of the 1st case. This class allows to calculate both variants:
 * the 1st case is chosen if the <tt>normalizeDirectTransform</tt> argument of the constructors is <tt>false</tt>
 * or if this class is created by a constructor without this argument (it is the default behaviour),
 * the 2nd case is chosen  if the <tt>normalizeDirectTransform</tt> argument of the constructors is <tt>true</tt>.</p>
 *
 * <p>The very useful feature of DHT is that <i>for real samples x<sub>k</sub> the Hartley spectrum
 * H<sub>k</sub> is also real</i> &mdash; unlike DFT, when even real samples lead to complex spectrum.
 * As a result, the transformation algorithms in this class can process real arrays and matrices,
 * without imaginary parts. In this case, they work in two and even more times faster than FFT algorithms,
 * implemented in {@link FastFourierTransform}, and do not require allocating additional memory
 * for storing imaginary parts of the complex numbers.</p>
 *
 * <p>The formulas above correspond to one-dimensional transforms and specify the results of
 * {@link #directTransform directTransform} / {@link #inverseTransform inverseTransform} methods.
 * They are generalized to multidimensional case by default algorithms, implemented in
 * {@link AbstractSpectralTransform} class, i.e. by applying the transform separably to each dimension.
 * It leads to so-called multidimensional <i>separable discrete Hartley transformations</i> (SDHT). Below are
 * the formulas for 2-dimensional separable discrete Hartley transformation of the matrix <i>x<sub>ij</sub></i>
 * (0&le;<i>i</i>&lt;<i>M</i>, 0&le;<i>j</i>&lt;<i>N</i>) for the case 1 (normalizing the inverse transform):</p>
 *
 * <blockquote>
 * direct:
 * <i>H<sub>ij</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>m</i>&lt;<i>M</i>)</sub></sub>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>x<sub>mn</sub></i>&nbsp;cas(2<i>im</i>&pi;/<i>M</i>)&nbsp;cas(2<i>jn</i>&pi;/<i>N</i>),<br>
 * inverse:
 * <i>x<sub>mn</sub></i> = (<i>MN</i>)<sup>&nbsp;&minus;1</sup>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>i</i>&lt;<i>M</i>)</sub></sub>
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>j</i>&lt;<i>N</i>)</sub></sub>
 * <i>x<sub>ij</sub></i>&nbsp;cas(2<i>im</i>&pi;/<i>M</i>)&nbsp;cas(2<i>jn</i>&pi;/<i>N</i>).
 * </blockquote>
 *
 * <p>There is the simple relation between classic DFT (discrete Fourier transform) and
 * SDHT (separable discrete Hartley transform).</p>
 *
 * <p>Let's consider one-dimensional case (usual DHT).
 * Let <i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>,...,<i>x</i><sub><i>N</i>&minus;1</sub> are some
 * real or complex samples,
 * <i>F</i><sub>0</sub>,<i>F</i><sub>1</sub>,...,<i>F</i><sub><i>N</i>&minus;1</sub> are their Fourier spectrum and
 * <i>H</i><sub>0</sub>,<i>H</i><sub>1</sub>,...,<i>H</i><sub><i>N</i>&minus;1</sub> are their Hartley spectrum.
 * Let <b><i>i</i></b> is the usual imaginary unit.
 * For simplicity, let's consider that <i>F<sub>&minus;k</sub></i>=<i>F<sub>N&minus;k</sub></i>,
 * <i>H<sub>&minus;k</sub></i>=<i>H<sub>N&minus;k</sub></i>, <i>k</i>=1,2,... Then:</p>
 *
 * <blockquote>
 * <i>F<sub>k</sub></i> = (<i>H<sub>k</sub></i>+<i>H<sub>&minus;k</sub></i>)/2
 * &minus; <b><i>i</i></b>&nbsp;(<i>H<sub>k</sub></i>&minus;<i>H<sub>&minus;k</sub></i>)/2,<br>
 * <i>H<sub>k</sub></i> = (<i>F<sub>k</sub></i>+<i>F<sub>&minus;k</sub></i>)/2
 * + <b><i>i</i></b>&nbsp;(<i>F<sub>k</sub></i>&minus;<i>F<sub>&minus;k</sub></i>)/2,<br>
 * in a case of real samples: <i>H<sub>k</sub></i> =
 * <b>Re</b> <i>F<sub>k</sub></i> &minus; <b>Im</b> <i>F<sub>k</sub></i>
 * </blockquote>
 *
 * <p>(of course, we consider the same definition, 1 or 2, for both {@link FastFourierTransform DFT} and
 * {@link SeparableFastHartleyTransform SDHT} spectra).</p>
 *
 * <p>In 2-dimensional case, the relation between DFT and SDHT is the following (we similarly suppose that
 * <i>F<sub>&minus;i,&nbsp;j</sub></i>=<i>F<sub>M&minus;i,&nbsp;j</sub></i>,
 * <i>F<sub>i,&minus;j</sub></i>=<i>F<sub>i,&nbsp;N&minus;j</sub></i>,
 * <i>H<sub>&minus;i,&nbsp;j</sub></i>=<i>H<sub>M&minus;i,&nbsp;j</sub></i>,
 * <i>H<sub>i,&minus;j</sub></i>=<i>H<sub>i,&nbsp;N&minus;j</sub></i>):</p>
 *
 * <blockquote>
 * <i>F<sub>i,&nbsp;j</sub></i> = (<i>H<sub>i,&minus;j</sub></i>+<i>H<sub>&minus;i,&nbsp;j</sub></i>)/2
 * &minus; <b><i>i</i></b>&nbsp;(<i>H<sub>i,&nbsp;j</sub></i>&minus;<i>H<sub>&minus;i,&minus;j</sub></i>)/2,<br>
 * <i>H<sub>i,&nbsp;j</sub></i> = (<i>F<sub>i,&minus;j</sub></i>+<i>F<sub>&minus;i,&nbsp;j</sub></i>)/2
 * + <b><i>i</i></b>&nbsp;(<i>F<sub>i,&nbsp;j</sub></i>&minus;<i>F<sub>&minus;i,&minus;j</sub></i>)/2,<br>
 * in a case of real samples: <i>H<sub>i,&nbsp;j</sub></i> =
 * <b>Re</b> <i>F<sub>i,&minus;j</sub></i> &minus; <b>Im</b> <i>F<sub>i,&nbsp;j</sub></i>.
 * </blockquote>
 *
 * <p>In the common <i>n</i>-dimensional case, there are similar formulas, which express
 * <i>F<sub>i,&nbsp;j,...,k</sub></i> through a linear combination of 2<sup><i>n</i></sup> numbers
 * <i>H<sub>&plusmn;&nbsp;i,&plusmn;&nbsp;j,...,&plusmn;&nbsp;k</sub></i> and, vice versa, express
 * <i>H<sub>i,&nbsp;j,...,k</sub></i> through a linear combination of 2<sup><i>n</i></sup> numbers
 * <i>F<sub>&plusmn;&nbsp;i,&plusmn;&nbsp;j,...,&plusmn;&nbsp;k</sub></i>.</p>
 *
 * <p>This class contains the ready for use methods, allowing to convert <i>n</i>-dimensional
 * separable Hartley spectrum to Fourier one and vice versa, <i>n</i>=1,2,3,...:</p>
 *
 * <ul>
 * <li>{@link #separableHartleyToFourier(ArrayContext context, Matrix fRe, Matrix fIm, Matrix h)}
 * converts (real) Hartley spectrum of the real matrix to its Fourier spectrum;</li>
 * <li>{@link #separableHartleyToFourier(ArrayContext context, Matrix fRe, Matrix fIm, Matrix hRe, Matrix hIm)}
 * converts (complex) Hartley spectrum of the complex matrix to its Fourier spectrum;</li>
 * <li>{@link #fourierToSeparableHartley(ArrayContext context, Matrix h, Matrix fRe, Matrix fIm)}
 * converts Fourer spectrum of the real matrix to its (real) Hartley spectrum;</li>
 * <li>{@link #fourierToSeparableHartley(ArrayContext context, Matrix hRe, Matrix hIm, Matrix fRe, Matrix fIm)}
 * converts Fourer spectrum of the complex matrix to its (complex) Hartley spectrum.</li>
 * </ul>
 *
 * <p>If it is necessary to get the Fourier spectrum of some real matrix, probably process it and transform
 * the Fourier spectrum back to the real matrix, you can use a combination of SHFT,
 * provided by this class, and the conversion methods listed above (cases of real matrices).
 * But if all that you need is to calculate a convolution of two real matrices, there is a better way: see below.</p>
 *
 * <p>One-dimensional Hartley transform, defined by the formulas 1 and 2 above, complies with the
 * <i>convolution theorem</i>. Namely, let
 * <i>p</i><sub>0</sub>,<i>p</i><sub>1</sub>,...,<i>p</i><sub><i>N</i>&minus;1</sub> is the first complex or real
 * numeric function and <i>q</i><sub>0</sub>,<i>q</i><sub>1</sub>,...,<i>q</i><sub><i>N</i>&minus;1</sub> is
 * the second complex or real function, and
 * <i>c</i><sub>0</sub>,<i>c</i><sub>1</sub>,...,<i>c</i><sub><i>N</i>&minus;1</sub> is
 * their (complex or real) <i>convolution</i>, defined as:</p>
 *
 * <blockquote>
 * <i>c<sub>k</sub></i> =
 * <big><big><big>&sum;</big></big></big><sub><sub>(0&le;<i>n</i>&lt;<i>N</i>)</sub></sub>
 * <i>p<sub>n</sub></i><i>q</i><sub><i>k</i>&minus;<i>n</i></sub>
 * </blockquote>
 *
 * <p>(here and below we consider that Z<sub><i>&minus;k</i></sub>=Z<sub><i>N&minus;k</i></sub> for all samples
 * and spectra).
 * Also, let <i>P</i><sub>0</sub>,<i>P</i><sub>1</sub>,...,<i>P</i><sub><i>N</i>&minus;1</sub>,
 * <i>Q</i><sub>0</sub>,<i>Q</i><sub>1</sub>,...,<i>Q</i><sub><i>N</i>&minus;1</sub> and
 * <i>C</i><sub>0</sub>,<i>C</i><sub>1</sub>,...,<i>C</i><sub><i>N</i>&minus;1</sub> are
 * Hartley spectra of these functions. Then:
 *
 * <ol type="A">
 * <li><i>C<sub>k</sub></i> =
 * (<i>P<sub>k</sub>Q<sub>&minus;k</sub></i>+<i>P<sub>&minus;k</sub>Q<sub>k</sub></i>)/2 +
 * (<i>P<sub>k</sub>Q<sub>k</sub></i>&minus;<i>P<sub>&minus;k</sub>Q<sub>&minus;k</sub></i>)/2,
 * if the spectra were calculated according formula 1 above (default method);</li>
 *
 * <li><i>C<sub>k</sub></i> = <i>N</i>
 * ((<i>P<sub>k</sub>Q<sub>&minus;k</sub></i>+<i>P<sub>&minus;k</sub>Q<sub>k</sub></i>)/2 +
 * (<i>P<sub>k</sub>Q<sub>k</sub></i>&minus;<i>P<sub>&minus;k</sub>Q<sub>&minus;k</sub></i>)/2),
 * if the spectra were calculated according formula 2 above.</li>
 * </ol>
 *
 * <p>There are similar formulas in the common <i>n</i>-dimensional case, allowing to express the separable
 * Hartley spectrum of the convolution of two <i>n</i>-dimensional matrices via the spectra of the source matrices.
 * In particular, in the 2-dimensional case:</p>
 *
 * <ol type="A" start="3">
 * <li><i>C<sub>i,&nbsp;j</sub></i> =
 * (<nobr>(<i>P<sub>i,&nbsp;j</sub></i>+<i>P<sub>&minus;i,&minus;j</sub></i>)
 * (<i>Q<sub>i,&nbsp;j</sub></i>+<i>Q<sub>&minus;i,&minus;j</sub></i>)</nobr> &minus;
 * <nobr>(<i>P<sub>i,&minus;j</sub></i>&minus;<i>P<sub>&minus;i,&nbsp;j</sub></i>)
 * (<i>Q<sub>i,&minus;j</sub></i>&minus;<i>Q<sub>&minus;i,&nbsp;j</sub></i>)</nobr> +
 * <nobr>(<i>P<sub>i,&nbsp;j</sub></i>&minus;<i>P<sub>&minus;i,&minus;j</sub></i>)
 * (<i>Q<sub>i,&minus;j</sub></i>+<i>Q<sub>&minus;i,&nbsp;j</sub></i>)</nobr> +
 * <nobr>(<i>P<sub>i,&minus;j</sub></i>+<i>P<sub>&minus;i,&nbsp;j</sub></i>)
 * (<i>Q<sub>i,&nbsp;j</sub></i>&minus;<i>Q<sub>&minus;i,&minus;j</sub></i>))/4</nobr>,
 * if the spectra were calculated according formula 1 above (default method);</li>
 *
 * <li><i>C<sub>i,&nbsp;j</sub></i> = the same expression multiplied by <i>MN</i>
 * (<i>M</i> and <i>N</i> are the dimensions of the matrices),
 * if the spectra were calculated according formula 2 above.</li>
 * </ol>
 *
 * <p>This class contains the ready for use methods, allowing to calculate a spectrum of convolution <i>C</i>
 * on the base of the given spectra <i>P</i> and <i>Q</i> of two source numeric matrices <i>x</i> and <i>y</i>
 * according the formulas A, C and their generalization for any number of dimensions:</p>
 *
 * <ul>
 * <li>{@link
 * #spectrumOfConvolution(ArrayContext context, Matrix cRe, Matrix cIm, Matrix pRe, Matrix pRe, Matrix qRe, Matrix qRe)}
 * calculates separable Hartley spectrum of the convolution <i>C</i> on the base of separable Hartley spectra
 * <i>P</i> and <i>Q</i> of two source complex matrices;</li>
 * <li>{@link #spectrumOfConvolution(ArrayContext context, Matrix c, Matrix p, Matrix q)} calculates
 * separable Hartley spectrum of the convolution <i>C</i> on the base of separable Hartley spectra
 * <i>P</i> and <i>Q</i> of two source real matrices.</li>
 * </ul>
 *
 * <p>So, if you need to calculate a convolution of some real matrices, for example, for goals of linear filtering,
 * you can use the SFHT transform and the
 * {@link #spectrumOfConvolution(ArrayContext context, Matrix c, Matrix p, Matrix q) spectrumOfConvolution}
 * method, provided by this class: it is much better idea than using {@link FastFourierTransform} class.</p>
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
 * of the matrix into {@link SimpleMemoryModel}. This problem also does not occur while using
 * conversion methods {@link #separableHartleyToFourier(ArrayContext, Matrix, Matrix, Matrix)},
 * {@link #separableHartleyToFourier(ArrayContext, Matrix, Matrix, Matrix, Matrix)},
 * {@link #fourierToSeparableHartley(ArrayContext, Matrix, Matrix, Matrix)},
 * {@link #fourierToSeparableHartley(ArrayContext, Matrix, Matrix, Matrix, Matrix)} and
 * methods of calculation of the spectrum of convolution
 * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix)} and
 * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix, Matrix, Matrix, Matrix)},
 * if all processed matrices have the same <tt>float</tt> or <tt>double</tt> element types.</p>
 */
public class SeparableFastHartleyTransform extends AbstractSpectralTransform implements SpectralTransform {

    final boolean normalizeDirectTransform;

    /**
     * Creates a new instance of this class, performing separable Hartley transform according to the formula 1 from
     * the {@link SeparableFastHartleyTransform comments to this class}.
     * Equivalent to {@link #SeparableFastHartleyTransform(boolean) SeparableFastHartleyTransform(false)}.
     *
     * @see #SeparableFastHartleyTransform(long)
     */
    public SeparableFastHartleyTransform() {
        this.normalizeDirectTransform = FastFourierTransform.DEFAULT_NORMALIZE_DIRECT_TRANSFORM;
    }

    /**
     * Creates a new instance of this class, performing separable Hartley transform according to the formula 1 from
     * the {@link SeparableFastHartleyTransform comments to this class}.
     *
     * <p>The <tt>maxTempJavaMemory</tt> argument specifies the amount of Java memory (heap),
     * that can be used by methods of this class for internal needs. It is passed to the corresponding
     * constructor of {@link AbstractSpectralTransform}: see
     * {@link AbstractSpectralTransform#AbstractSpectralTransform(long) comments to that constructor}.
     *
     * @param maxTempJavaMemory desired maximal amount of Java memory, in bytes, allowed for allocation
     *                          by methods of this class for internal needs.
     * @see #SeparableFastHartleyTransform()
     */
    public SeparableFastHartleyTransform(long maxTempJavaMemory) {
        super(maxTempJavaMemory);
        this.normalizeDirectTransform = FastFourierTransform.DEFAULT_NORMALIZE_DIRECT_TRANSFORM;
    }

    /**
     * Creates a new instance of this class, performing separable Hartley transform according either to the formula 1
     * from the {@link SeparableFastHartleyTransform comments to this class},
     * if <tt>normalizeDirectTransform</tt> argument is <tt>false</tt>,
     * or to the formula 2, if this argument is <tt>true</tt>.
     * The default value, used by the constructors without <tt>normalizeDirectTransform</tt> argument,
     * is <tt>false</tt>.
     *
     * <p>Please note: the value of <tt>normalizeDirectTransform</tt> argument affects only the transformation
     * methods {@link #directTransform directTransform}, {@link #inverseTransform inverseTransform},
     * {@link #directTransformMatrix directTransformMatrix}, {@link #inverseTransformMatrix inverseTransformMatrix}.
     * This value does not matter in other methods of this class: conversions between Hartley and Fourier spectrum,
     * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix)} and
     * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix, Matrix, Matrix, Matrix)}.
     *
     * @param normalizeDirectTransform <tt>true</tt> if you want to perform normalization (division by the number of
     *                                 samples <i>N</i>) after the direct transform, <tt>false</tt> (the default value)
     *                                 if you want to perform normalization after the inverse transform.
     * @see #SeparableFastHartleyTransform(boolean, long)
     */
    public SeparableFastHartleyTransform(boolean normalizeDirectTransform) {
        this.normalizeDirectTransform = normalizeDirectTransform;
    }

    /**
     * Creates a new instance of this class, performing separable Hartley transform according either to the formula 1
     * from the {@link SeparableFastHartleyTransform comments to this class},
     * if <tt>normalizeDirectTransform</tt> argument is <tt>false</tt>,
     * or to the formula 2, if this argument is <tt>true</tt>.
     * The default value, used by the constructors without <tt>normalizeDirectTransform</tt> argument,
     * is <tt>false</tt>.
     *
     * <p>Please note: the value of <tt>normalizeDirectTransform</tt> argument affects only the transformation
     * methods {@link #directTransform directTransform}, {@link #inverseTransform inverseTransform},
     * {@link #directTransformMatrix directTransformMatrix}, {@link #inverseTransformMatrix inverseTransformMatrix}.
     * This value does not matter in other methods of this class: conversions between Hartley and Fourier spectrum,
     * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix)} and
     * {@link #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix, Matrix, Matrix, Matrix)}.
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
     * @see #SeparableFastHartleyTransform(boolean)
     */
    public SeparableFastHartleyTransform(boolean normalizeDirectTransform, long maxTempJavaMemory) {
        super(maxTempJavaMemory);
        this.normalizeDirectTransform = normalizeDirectTransform;
    }

    /**
     * Converts the separable Hartley spectrum <i>H</i> of some real <i>n</i>-dimensional matrix into
     * the (complex) Fourier spectrum <i>F</i> of the same matrix.
     * See the {@link SeparableFastHartleyTransform comments to this class} about the relation formulas between
     * separable Hartley and Fourier spectra.
     *
     * <p>The complex matrix <i>F</i> is represented as a pair of AlgART matrices <tt>(fRe,fIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix <i>F</i>.
     * The real matrix <i>H</i> is passed as an AlgART matrix <tt>h</tt>.
     *
     * <p>All matrices, passed to this method, must have {@link Matrix#dimEquals(Matrix) equal dimensions}.
     * The {@link Matrix#elementType() element type} of the passed matrices can be different, but we recommend
     * using the same <tt>float</tt> or <tt>double</tt> element type for all matrices.
     * There are no restrictions for the dimensions of the passed matrices:
     * {@link #isLengthAllowed(long)} method is not used here.
     *
     * <p>This method works correctly, if you pass the same matrix as <tt>fRe</tt> / <tt>fIm</tt> and <tt>h</tt>.
     *
     * <p>If you need to convert spectrum in a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param fRe     the real parts of the elements of the resulting matrix (Fourier spectrum).
     * @param fIm     the imaginary parts of the elements of the resulting matrix (Fourier spectrum).
     * @param h       the source real matrix (separable Hartley spectrum).
     * @throws NullPointerException  if one of <tt>fRe</tt>, <tt>fIm</tt>, <tt>h</tt> arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of the passed matrices have different dimensions.
     * @see #separableHartleyToFourier(ArrayContext, Matrix, Matrix, Matrix, Matrix)
     */
    public void separableHartleyToFourier(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> fRe, Matrix<? extends UpdatablePNumberArray> fIm,
        Matrix<? extends PNumberArray> h)
    {
        Objects.requireNonNull(fRe, "Null fRe argument");
        Objects.requireNonNull(fIm, "Null fIm argument");
        Objects.requireNonNull(h, "Null h argument");
        if (!fRe.dimEquals(fIm)) {
            throw new SizeMismatchException("fRe and fIm dimensions mismatch: fRe is " + fRe + ", fIm " + fIm);
        }
        if (!h.dimEquals(fRe)) {
            throw new SizeMismatchException("h and fRe dimensions mismatch: h is " + h + ", fRe " + fRe);
        }
        ThreadPoolFactory tpf = Arrays.getThreadPoolFactory(context);
        Conversions.separableHartleyToFourierRecoursive(context, maxTempJavaMemory(),
            fRe.array(), fIm.array(), h.array(), null,
            fRe.dimensions(), Math.max(1, tpf.recommendedNumberOfTasks()));
    }

    /**
     * Converts the separable Hartley spectrum <i>H</i> of some complex <i>n</i>-dimensional matrix into
     * the (complex) Fourier spectrum <i>F</i> of the same matrix.
     * See the {@link SeparableFastHartleyTransform comments to this class} about the relation formulas between
     * separable Hartley and Fourier spectra.
     *
     * <p>The complex matrix <i>F</i> is represented as a pair of AlgART matrices <tt>(fRe,fIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix <i>F</i>.
     * Similarly, the complex matrix <i>H</i> is represented as a pair of AlgART matrices <tt>(hRe,hIm)</tt>.
     *
     * <p>All matrices, passed to this method, must have {@link Matrix#dimEquals(Matrix) equal dimensions}.
     * The {@link Matrix#elementType() element type} of the passed matrices can be different, but we recommend
     * using the same <tt>float</tt> or <tt>double</tt> element type for all matrices.
     * There are no restrictions for the dimensions of the passed matrices:
     * {@link #isLengthAllowed(long)} method is not used here.
     *
     * <p>This method works correctly, if you pass the same complex matrix as <i>F</i> and <i>H</i>.
     * So, you can calculate and return the result in the source matrices.
     *
     * <p>If you need to convert spectrum in a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param fRe     the real parts of the elements of the resulting matrix (Fourier spectrum).
     * @param fIm     the imaginary parts of the elements of the resulting matrix (Fourier spectrum).
     * @param hRe     the real parts of the elements of the source matrix (separable Hartley spectrum).
     * @param hIm     the imaginary parts of the elements of the source matrix (separable Hartley spectrum).
     * @throws NullPointerException  if one of <tt>fRe</tt>, <tt>fIm</tt>, <tt>hRe</tt>, <tt>hIm</tt>
     *                               arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of the passed matrices have different dimensions.
     * @see #separableHartleyToFourier(ArrayContext, Matrix, Matrix, Matrix)
     */
    public void separableHartleyToFourier(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> fRe, Matrix<? extends UpdatablePNumberArray> fIm,
        Matrix<? extends PNumberArray> hRe, Matrix<? extends PNumberArray> hIm)
    {
        Objects.requireNonNull(fRe, "Null fRe argument");
        Objects.requireNonNull(fIm, "Null fIm argument");
        Objects.requireNonNull(hRe, "Null hRe argument");
        Objects.requireNonNull(hIm, "Null hIm argument");
        if (!fRe.dimEquals(fIm)) {
            throw new SizeMismatchException("fRe and fIm dimensions mismatch: fRe is " + fRe + ", fIm " + fIm);
        }
        if (!hRe.dimEquals(fRe)) {
            throw new SizeMismatchException("hRe and fRe dimensions mismatch: hRe is " + hRe + ", fRe " + fRe);
        }
        if (!hIm.dimEquals(fRe)) {
            throw new SizeMismatchException("hIm and fRe dimensions mismatch: hIm is " + hIm + ", fRe " + fRe);
        }
        ThreadPoolFactory tpf = Arrays.getThreadPoolFactory(context);
        Conversions.separableHartleyToFourierRecoursive(context, maxTempJavaMemory(),
            fRe.array(), fIm.array(), hRe.array(), hIm.array(),
            fRe.dimensions(), Math.max(1, tpf.recommendedNumberOfTasks()));
    }

    /**
     * Converts the Fourier spectrum <i>F</i> of some real <i>n</i>-dimensional matrix into
     * the (real) separable Hartley  spectrum <i>H</i> of the same matrix.
     * See the {@link SeparableFastHartleyTransform comments to this class} about the relation formulas between
     * separable Hartley and Fourier spectra.
     * If the passed Fourier spectrum is not a spectrum of a real matrix (in other words,
     * if the inverse Fourier transform of <i>F</i> matrix contains nonzero imaginary parts),
     * then this method still correctly calculates the real parts of the separable Hartley spectrum <i>H</i>.
     *
     * <p>The complex matrix <i>F</i> is represented as a pair of AlgART matrices <tt>(fRe,fIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix <i>F</i>.
     * The real matrix <i>H</i> (or the real parts of <i>H</i>, if the passed <i>F</i> matrix
     * is not a spectrum of a real matrix) is passed as an AlgART matrix <tt>h</tt>.
     *
     * <p>All matrices, passed to this method, must have {@link Matrix#dimEquals(Matrix) equal dimensions}.
     * The {@link Matrix#elementType() element type} of the passed matrices can be different, but we recommend
     * using the same <tt>float</tt> or <tt>double</tt> element type for all matrices.
     * There are no restrictions for the dimensions of the passed matrices:
     * {@link #isLengthAllowed(long)} method is not used here.
     *
     * <p>This method works correctly, if you pass the same matrix as <tt>fRe</tt> / <tt>fIm</tt> and <tt>h</tt>.
     *
     * <p>If you need to convert spectrum in a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param h       the resulting real matrix (separable Hartley spectrum).
     * @param fRe     the real parts of the elements of the source matrix (Fourier spectrum).
     * @param fIm     the imaginary parts of the elements of the source matrix (Fourier spectrum).
     * @throws NullPointerException  if one of <tt>h</tt>, <tt>fRe</tt>, <tt>fIm</tt> arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of the passed matrices have different dimensions.
     * @see #fourierToSeparableHartley(ArrayContext, Matrix, Matrix, Matrix, Matrix)
     */
    public void fourierToSeparableHartley(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> h,
        Matrix<? extends PNumberArray> fRe, Matrix<? extends PNumberArray> fIm)
    {
        Objects.requireNonNull(h, "Null h argument");
        Objects.requireNonNull(fRe, "Null fRe argument");
        Objects.requireNonNull(fIm, "Null fIm argument");
        if (!fRe.dimEquals(fIm)) {
            throw new SizeMismatchException("fRe and fIm dimensions mismatch: fRe is " + fRe + ", fIm " + fIm);
        }
        if (!h.dimEquals(fRe)) {
            throw new SizeMismatchException("h and fRe dimensions mismatch: h is " + h + ", fRe " + fRe);
        }
        ThreadPoolFactory tpf = Arrays.getThreadPoolFactory(context);
        Conversions.fourierToSeparableHartleyRecursive(context, maxTempJavaMemory(),
            h.array(), null, fRe.array(), fIm.array(),
            fRe.dimensions(), Math.max(1, tpf.recommendedNumberOfTasks()));
    }

    /**
     * Converts the Fourier spectrum <i>F</i> of some complex <i>n</i>-dimensional matrix into
     * the (complex) separable Hartley spectrum <i>H</i> of the same matrix.
     * See the {@link SeparableFastHartleyTransform comments to this class} about the relation formulas between
     * separable Hartley and Fourier spectra.
     *
     * <p>The complex matrix <i>F</i> is represented as a pair of AlgART matrices <tt>(fRe,fIm)</tt>:
     * the corresponding elements of these 2 matrices contain the real and imaginary parts
     * of the corresponding elements of the complex matrix <i>F</i>.
     * Similarly, the complex matrix <i>H</i> is represented as a pair of AlgART matrices <tt>(hRe,hIm)</tt>.
     *
     * <p>All matrices, passed to this method, must have {@link Matrix#dimEquals(Matrix) equal dimensions}.
     * The {@link Matrix#elementType() element type} of the passed matrices can be different, but we recommend
     * using the same <tt>float</tt> or <tt>double</tt> element type for all matrices.
     * There are no restrictions for the dimensions of the passed matrices:
     * {@link #isLengthAllowed(long)} method is not used here.
     *
     * <p>This method works correctly, if you pass the same complex matrix as <i>F</i> and <i>H</i>.
     * So, you can calculate and return the result in the source matrices.
     *
     * <p>If you need to convert spectrum in a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param hRe     the real parts of the elements of the resulting matrix (separable Hartley spectrum).
     * @param hIm     the imaginary parts of the elements of the resulting matrix (separable Hartley spectrum).
     * @param fRe     the real parts of the elements of the source matrix (Fourier spectrum).
     * @param fIm     the imaginary parts of the elements of the source matrix (Fourier spectrum).
     * @throws NullPointerException  if one of <tt>hRe</tt>, <tt>hIm</tt>, <tt>fRe</tt>, <tt>fIm</tt>
     *                               arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of the passed matrices have different dimensions.
     * @see #fourierToSeparableHartley(ArrayContext, Matrix, Matrix, Matrix)
     */
    public void fourierToSeparableHartley(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> hRe, Matrix<? extends UpdatablePNumberArray> hIm,
        Matrix<? extends PNumberArray> fRe, Matrix<? extends PNumberArray> fIm)
    {
        Objects.requireNonNull(hRe, "Null hRe argument");
        Objects.requireNonNull(fRe, "Null fRe argument");
        Objects.requireNonNull(fIm, "Null fIm argument");
        Objects.requireNonNull(hIm, "Null hIm argument");
        if (!fRe.dimEquals(fIm)) {
            throw new SizeMismatchException("fRe and fIm dimensions mismatch: fRe is " + fRe + ", fIm " + fIm);
        }
        if (!hRe.dimEquals(fRe)) {
            throw new SizeMismatchException("hRe and fRe dimensions mismatch: hRe is " + hRe + ", fRe " + fRe);
        }
        if (!hIm.dimEquals(fRe)) {
            throw new SizeMismatchException("hIm and fRe dimensions mismatch: hIm is " + hIm + ", fRe " + fRe);
        }
        ThreadPoolFactory tpf = Arrays.getThreadPoolFactory(context);
        Conversions.fourierToSeparableHartleyRecursive(context, maxTempJavaMemory(),
            hRe.array(), hIm.array(), fRe.array(), fIm.array(),
            fRe.dimensions(), Math.max(1, tpf.recommendedNumberOfTasks()));
    }

    /**
     * Calculates <i>C</i>, the separable Hartley spectrum of the <i>convolution</i> of some two real matrices,
     * on the base of <i>P</i> and <i>Q</i> &mdash; the separable Hartley spectra of these two real matrices.
     *
     * <p>The real matrices <i>P</i>, <i>Q</i>, <i>C</i> are passed as AlgART matrices
     * <tt>p</tt>, <tt>q</tt>, <tt>c</tt>.
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
     * <p>If you need to calculate the Hartley spectrum of convolution for a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param c       the resulting matrix (spectrum of the convolution).
     * @param p       the spectrum of the 1st matrix.
     * @param q       the spectrum of the 2nd matrix.
     * @throws NullPointerException  if one of <tt>c</tt>, <tt>p</tt>, <tt>q</tt> arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of the passed matrices have different dimensions.
     * @see #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix, Matrix, Matrix, Matrix)
     */
    public void spectrumOfConvolution(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> c,
        Matrix<? extends PNumberArray> p,
        Matrix<? extends PNumberArray> q)
    {
        Objects.requireNonNull(c, "Null c argument");
        Objects.requireNonNull(p, "Null p argument");
        Objects.requireNonNull(q, "Null q argument");
        if (!p.dimEquals(c)) {
            throw new SizeMismatchException("c and p dimensions mismatch: c is " + c + ", p " + p);
        }
        if (!q.dimEquals(c)) {
            throw new SizeMismatchException("c and q dimensions mismatch: c is " + c + ", q " + q);
        }
        ThreadPoolFactory tpf = Arrays.getThreadPoolFactory(context);
        SpectraOfConvolution.separableHartleySpectrumOfConvolution(context, maxTempJavaMemory(),
            c.array(), null, p.array(), null, q.array(), null,
            c.dimensions(), Math.max(1, tpf.recommendedNumberOfTasks()));
    }

    /**
     * Calculates <i>C</i>, the separable Hartley spectrum of the <i>convolution</i> of some two complex matrices,
     * on the base of <i>P</i> and <i>Q</i> &mdash; the separable Hartley spectra of these two complex matrices.
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
     * <p>If you need to calculate the Hartley spectrum of convolution for a case of <nobr>one-dimensional</nobr>
     * numeric AlgART arrays, you just need to convert them into <nobr>one-dimensional</nobr> AlgART matrices by
     * {@link Matrices#matrix(Array, long...)} call, for example:
     * <tt>{@link Matrices#matrix(Array, long...) Matrices.matrix}(array, array.length())</tt>.
     *
     * @param context the context that will be used by this algorithm; may be <tt>null</tt>
     *                (see comments to {@link SpectralTransform}).
     * @param cRe     the real parts of the elements of the resulting matrix (spectrum of the convolution).
     * @param cIm     the imaginary parts of the elements of the resulting matrix (spectrum of the convolution).
     * @param pRe     the real parts of the elements of the spectrum of the 1st matrix.
     * @param pIm     the imaginary parts of the elements of the spectrum of the 1st matrix.
     * @param qRe     the real parts of the elements of the spectrum of the 2nd matrix.
     * @param qIm     the imaginary parts of the elements of the spectrum of the 2nd matrix.
     * @throws NullPointerException  if one of <tt>cRe</tt>, <tt>cIm</tt>, <tt>pRe</tt>, <tt>pIm</tt>,
     *                               <tt>qRe</tt>, <tt>qIm</tt> arguments is <tt>null</tt>.
     * @throws SizeMismatchException if some of the passed matrices have different dimensions.
     * @see #spectrumOfConvolution(ArrayContext, Matrix, Matrix, Matrix)
     */
    public void spectrumOfConvolution(ArrayContext context,
        Matrix<? extends UpdatablePNumberArray> cRe, Matrix<? extends UpdatablePNumberArray> cIm,
        Matrix<? extends PNumberArray> pRe, Matrix<? extends PNumberArray> pIm,
        Matrix<? extends PNumberArray> qRe, Matrix<? extends PNumberArray> qIm)
    {
        Objects.requireNonNull(cRe, "Null cRe argument");
        Objects.requireNonNull(cIm, "Null cIm argument");
        Objects.requireNonNull(pRe, "Null pRe argument");
        Objects.requireNonNull(pIm, "Null pIm argument");
        Objects.requireNonNull(qRe, "Null qRe argument");
        Objects.requireNonNull(qIm, "Null qIm argument");
        if (!cRe.dimEquals(cIm)) {
            throw new SizeMismatchException("cRe and cIm dimensions mismatch: cRe is " + cRe + ", cIm " + cIm);
        }
        if (!pRe.dimEquals(cRe)) {
            throw new SizeMismatchException("cRe and pRe dimensions mismatch: cRe is " + cRe + ", pRe " + pRe);
        }
        if (!pIm.dimEquals(cRe)) {
            throw new SizeMismatchException("cRe and pIm dimensions mismatch: cRe is " + cRe + ", pIm " + pIm);
        }
        if (!qRe.dimEquals(cRe)) {
            throw new SizeMismatchException("cRe and qRe dimensions mismatch: cRe is " + cRe + ", qRe " + qRe);
        }
        if (!qIm.dimEquals(cRe)) {
            throw new SizeMismatchException("cRe and qIm dimensions mismatch: cRe is " + cRe + ", qIm " + qIm);
        }
        ThreadPoolFactory tpf = Arrays.getThreadPoolFactory(context);
        SpectraOfConvolution.separableHartleySpectrumOfConvolution(context, maxTempJavaMemory(),
            cRe.array(), cIm.array(), pRe.array(), pIm.array(), qRe.array(), qIm.array(),
            cRe.dimensions(), Math.max(1, tpf.recommendedNumberOfTasks()));
    }

    @Override
    public final boolean isLengthAllowed(long length) {
        return (length & (length - 1)) == 0;
    }

    @Override
    public boolean areComplexSamplesRequired() {
        return false;
    }

    @Override
    protected String unallowedLengthMessage() {
        return "FHT algorithm can process only 2^k elements";
    }

    @Override
    protected final void transform(ArrayContext context, SampleArray samples, boolean inverse) {
        assert isLengthAllowed(samples.length());
        boolean normalize = normalizeDirectTransform ? !inverse : inverse;

//        long t1 = System.nanoTime();
        ReverseBits.reorderForButterfly(context == null ? null : context.part(0.0, 0.2), samples);

//        long t2 = System.nanoTime();
        fhtMainLoop(context == null ? null : context.part(0.2, normalize ? 0.95 : 1.0), samples);

//        long t3 = System.nanoTime();
        if (normalize) {
            FastFourierTransform.fftNormalize(context == null ? null : context.part(0.95, 1.0), samples);
        }
//        long t4 = System.nanoTime();
//        System.out.printf("FHT time (common branch) %.3f ms reorder, %.3f ms main, %.3f normalizing%n",
//            (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6);
    }

    private static final int LOG_ANGLE_STEP = 4, ANGLE_STEP = 1 << LOG_ANGLE_STEP;
    private static final double SQRT2 = StrictMath.sqrt(2.0);
    private static final double HALF_SQRT2 = 0.5 * SQRT2;
    private static final int
        S01 = 0, D01 = 1, S23 = 2, D23 = 3,
        S45 = 0, D45 = 1, S67 = 2, D67 = 3, // reusing
        SS0123 = 4, SD0123 = 5, DS0123 = 6, DD0123 = 7,
        SS4567 = 8, DS4567 = 9,
        R1 = 0, R2 = 1,
        L1 = 2,
        L2 = 2, // reusing,
        CAS = 3,
        NUMBER_OF_WORK_VARIABLES = 10;

    private static void fhtMainLoop(ArrayContext context, SampleArray samples) {
        final int logN = 63 - Long.numberOfLeadingZeros(samples.length());
        if (logN <= 0) {
            return; // nothing to do: this check little simplifies further checks in the recursive procedure
        }

        if (samples instanceof RealScalarSampleArray.DirectZeroOffsetsRealFloatSampleArray
            || samples instanceof RealScalarSampleArray.DirectRealFloatSampleArray) {
            fhtJavaFloatMainLoop(context, samples, 0, logN, logN);
        } else if (samples instanceof RealScalarSampleArray.DirectZeroOffsetsRealDoubleSampleArray
            || samples instanceof RealScalarSampleArray.DirectRealDoubleSampleArray) {
            fhtJavaDoubleMainLoop(context, samples, 0, logN, logN);
// The functions below are anti-optimization in the large applications in the current version of 32-bit Java 1.7
//        } else if (samples instanceof RealVectorSampleArray.DirectRealFloatVectorSampleArray) {
//            fhtJavaFloatMultidimensionalMainLoop(context,
//                (RealVectorSampleArray.DirectRealFloatVectorSampleArray)samples, 0, logN, logN,
//                ((RealVectorSampleArray.DirectRealFloatVectorSampleArray)samples).
//                    newCompatibleSamplesArray(NUMBER_OF_WORK_VARIABLES));
//        } else if (samples instanceof RealVectorSampleArray.DirectRealDoubleVectorSampleArray) {
//            fhtJavaDoubleMultidimensionalMainLoop(context,
//                (RealVectorSampleArray.DirectRealDoubleVectorSampleArray)samples, 0, logN, logN,
//                ((RealVectorSampleArray.DirectRealDoubleVectorSampleArray)samples).
//                    newCompatibleSamplesArray(NUMBER_OF_WORK_VARIABLES));
        } else {
            fhtCommonMainLoop(context, samples,
                0, logN, logN, samples.newCompatibleSamplesArray(NUMBER_OF_WORK_VARIABLES));
        }
    }

    private static void fhtCommonMainLoop(ArrayContext context, SampleArray samples,
        final long pos, final int logN, final int originalLogN, SampleArray work)
    {
        switch (logN) { // in comments below xK, rK, yK means the source, the reordered source and the result
            case 1: {
                work.sub(0, samples, pos, pos + 1);
                samples.add(pos, pos, pos + 1);             // y0 = x0 + x1
                samples.copy(pos + 1, work, 0);             // y1 = x0 - x1
                break;
            }
            case 2: {
                work.sub(D01, samples, pos, pos + 1);
                work.add(S01, samples, pos, pos + 1);
                work.sub(D23, samples, pos + 2, pos + 3);
                work.add(S23, samples, pos + 2, pos + 3);
                samples.add(pos, work, S01, S23);           // y0 = r0 + r1 + r2 + r3 = x0 + x2 + x1 + x3
                samples.add(pos + 1, work, D01, D23);       // y1 = r0 - r1 + r2 - r3 = x0 - x2 + x1 - x3
                samples.sub(pos + 2, work, S01, S23);       // y2 = r0 + r1 - r2 - r3 = x0 + x2 - x1 - x3
                samples.sub(pos + 3, work, D01, D23);       // y3 = r0 - r1 - r2 + r3 = x0 - x2 - x1 + x3
                break;
            }
            case 3: {
                work.sub(D01, samples, pos, pos + 1);
                work.add(S01, samples, pos, pos + 1);
                work.sub(D23, samples, pos + 2, pos + 3);
                work.add(S23, samples, pos + 2, pos + 3);
                work.sub(DS0123, S01, S23);
                work.add(SS0123, S01, S23);
                work.sub(DD0123, D01, D23);
                work.add(SD0123, D01, D23); // S01, S23, D01, D23 will not be used more and can be reused
                work.add(S45, samples, pos + 4, pos + 5);
                work.add(S67, samples, pos + 6, pos + 7);
                work.sub(D45, samples, pos + 4, pos + 5);
                work.sub(D67, samples, pos + 6, pos + 7);
                work.sub(DS4567, S45, S67);
                work.add(SS4567, S45, S67);
                samples.sub(pos + 4, work, SS0123, SS4567); // y4 = r0 + r1 + r2 + r3 - r4 - r5 - r6 - r7
                samples.add(pos, work, SS0123, SS4567);     // y0 = r0 + ... + r7 = x0 + ... + x7
                samples.sub(pos + 6, work, DS0123, DS4567); // y6 = r0 + r1 - r2 - r3 - r4 - r5 + r6 + r7
                                                            //    = x0 + x4 - x2 - x6 - x1 - x5 + x3 + x7
                samples.add(pos + 2, work, DS0123, DS4567); // y2 = r0 + r1 - r2 - r3 + r4 + r5 - r6 - r7
                                                            //    = x0 + x4 - x2 - x6 + x1 + x5 - x3 - x7
                work.multiplyByRealScalar(D45, SQRT2);
                work.multiplyByRealScalar(D67, SQRT2);
                samples.sub(pos + 5, work, SD0123, D45);    // y5 = r0 - r1 + r2 - r3 - sqrt(2)*r4 + sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 - sqrt(2)*x1 + sqrt(2)*x5
                samples.add(pos + 1, work, SD0123, D45);    // y1 = r0 - r1 + r2 - r3 + sqrt(2)*r4 - sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 + sqrt(2)*x1 - sqrt(2)*x5
                samples.sub(pos + 7, work, DD0123, D67);    // y7 = r0 - r1 - r2 + r3 - sqrt(2)*r6 + sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 - sqrt(2)*x3 + sqrt(2)*x7
                samples.add(pos + 3, work, DD0123, D67);    // y3 = r0 - r1 - r2 + r3 + sqrt(2)*r6 - sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 + sqrt(2)*x3 - sqrt(2)*x7
                break;
            }
            default: {
                assert logN > 3;
                final long nDiv8 = 1L << (logN - 3);
                final long nDiv4 = nDiv8 * 2;
                final long nDiv2 = nDiv4 * 2;
                fhtCommonMainLoop(context, samples, pos, logN - 1, originalLogN, work);
                fhtCommonMainLoop(context, samples, pos + nDiv2, logN - 1, originalLogN, work);

                final boolean allAnglesInCache = logN - 1 <= RootsOfUnity.LOG_CACHE_SIZE + LOG_ANGLE_STEP;
                final double rotationAngle = StrictMath.PI / nDiv2;
                final double sin0Half = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN]; //sin(0.5 * rotationAngle);
                final double cos0M1 = -2.0 * sin0Half * sin0Half; // cos(rotationAngle)-1
                final double sin0 = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN - 1]; //sin(rotationAngle)
                double cos = 1.0 + cos0M1;
                double sin = sin0;
                long j1, j2;
                for (long j = 1; j < nDiv8; j++) {
                    // (cos, sin) = (cos(j*2*PI/N),  sin(j*2*PI/N))
                    j1 = pos + j;
                    j2 = pos + nDiv2 - j;
                    //[[Repeat.SectionStart CommonButterfly]]
                    work.copy(R1, samples, nDiv2 + j1);
                    work.copy(R2, samples, nDiv2 + j2);
                    work.combineWithRealMultipliers(CAS, R1, cos, R2, sin);
                    work.copy(L1, samples, j1);
                    samples.add(j1, work, L1, CAS);
                    samples.sub(nDiv2 + j1, work, L1, CAS);
                    work.combineWithRealMultipliers(CAS, R1, sin, R2, -cos);
                    work.copy(L2, samples, j2);
                    samples.add(j2, work, L2, CAS);
                    samples.sub(nDiv2 + j2, work, L2, CAS);
                    //[[Repeat.SectionEnd CommonButterfly]]

                    j1 = pos + nDiv4 - j;
                    j2 = pos + nDiv4 + j; // below is the same code as above with exchanging sin<->cos
                    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, CommonButterfly)
                    //  cos ==> TTTT;; sin ==> cos;; TTTT ==> sin   !! Auto-generated: NOT EDIT !! ]]
                    work.copy(R1, samples, nDiv2 + j1);
                    work.copy(R2, samples, nDiv2 + j2);
                    work.combineWithRealMultipliers(CAS, R1, sin, R2, cos);
                    work.copy(L1, samples, j1);
                    samples.add(j1, work, L1, CAS);
                    samples.sub(nDiv2 + j1, work, L1, CAS);
                    work.combineWithRealMultipliers(CAS, R1, cos, R2, -sin);
                    work.copy(L2, samples, j2);
                    samples.add(j2, work, L2, CAS);
                    samples.sub(nDiv2 + j2, work, L2, CAS);
                    //[[Repeat.IncludeEnd]]

                    if ((j & (ANGLE_STEP - 1)) == ANGLE_STEP - 1) {
                        if (allAnglesInCache) {
                            int angleIndex = (int)(j + 1) >> LOG_ANGLE_STEP
                                << (RootsOfUnity.LOG_CACHE_SIZE - (logN - 1) + LOG_ANGLE_STEP);
                            // (j + 1) * CACHE_SIZE/nDiv2
                            // cos = RootsOfUnity.quickCos(angleIndex);
                            // sin = RootsOfUnity.quickSin(angleIndex);
                            cos = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                                -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                            sin = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[angleIndex] :
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        } else {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            cos = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            sin = Math.sin(angle);
                        }
                    } else {
                        // on modern JVM and CPU, the following 4 multiplications and 4 additions work faster
                        // then extracting cos and sin from the table
                        double temp = cos;
                        cos = cos * cos0M1 - sin * sin0 + cos;
                        sin = sin * cos0M1 + temp * sin0 + sin;
                    }
                }
                j1 = pos + nDiv8;
                j2 = pos + nDiv2 - nDiv8;
                //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, CommonButterfly)
                //  (cos|sin) ==> HALF_SQRT2;;
                //  \s\s\s\s(work\.|samples\.|$) ==> $1   !! Auto-generated: NOT EDIT !! ]]
                work.copy(R1, samples, nDiv2 + j1);
                work.copy(R2, samples, nDiv2 + j2);
                work.combineWithRealMultipliers(CAS, R1, HALF_SQRT2, R2, HALF_SQRT2);
                work.copy(L1, samples, j1);
                samples.add(j1, work, L1, CAS);
                samples.sub(nDiv2 + j1, work, L1, CAS);
                work.combineWithRealMultipliers(CAS, R1, HALF_SQRT2, R2, -HALF_SQRT2);
                work.copy(L2, samples, j2);
                samples.add(j2, work, L2, CAS);
                samples.sub(nDiv2 + j2, work, L2, CAS);
                //[[Repeat.IncludeEnd]]

                work.sub(0, samples, pos, pos + nDiv2);
                samples.add(pos, pos, pos + nDiv2);         // r[0] + r[n/2]
                samples.copy(pos + nDiv2, work, 0);         // r[0] - r[n/2]

                work.sub(0, samples, pos + nDiv4, pos + nDiv2 + nDiv4);
                samples.add(pos + nDiv4, pos + nDiv4, pos + nDiv2 + nDiv4); // r[n/4] + r[3*n/4]
                samples.copy(pos + nDiv2 + nDiv4, work, 0);                 // r[n/4] - r[3*n/4]
            }

            if (context != null && (pos + (1 << logN) == samples.length())) {
                context.checkInterruptionAndUpdateProgress(null, logN, originalLogN);
            }
        }
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\(([^)]+)\) ==> $1 ]]
    private static void fhtJavaFloatMainLoop(ArrayContext context, SampleArray samples,
        final int pos, final int logN, final int originalLogN)
    {
        final float[] values = samples instanceof RealScalarSampleArray.DirectZeroOffsetsRealFloatSampleArray ?
            ((RealScalarSampleArray.DirectZeroOffsetsRealFloatSampleArray)samples).samples :
            ((RealScalarSampleArray.DirectRealFloatSampleArray)samples).samples;
        final int ofs = pos + (samples instanceof RealScalarSampleArray.DirectZeroOffsetsRealFloatSampleArray ? 0 :
            ((RealScalarSampleArray.DirectRealFloatSampleArray)samples).ofs);
        switch (logN) { // in comments below xK, rK, yK means the source, the reordered source and the result
            case 1: {
                float temp = values[ofs] - values[ofs + 1];
                values[ofs] += values[ofs + 1];             // y0 = x0 + x1
                values[ofs + 1] = temp;                     // y1 = x0 - x1
                break;
            }
            case 2: {
                float d01 = values[ofs] - values[ofs + 1];
                float s01 = values[ofs] + values[ofs + 1];
                float d23 = values[ofs + 2] - values[ofs + 3];
                float s23 = values[ofs + 2] + values[ofs + 3];
                values[ofs] = s01 + s23;                    // y0 = r0 + r1 + r2 + r3 = x0 + x2 + x1 + x3
                values[ofs + 1] = d01 + d23;                // y1 = r0 - r1 + r2 - r3 = x0 - x2 + x1 - x3
                values[ofs + 2] = s01 - s23;                // y2 = r0 + r1 - r2 - r3 = x0 + x2 - x1 - x3
                values[ofs + 3] = d01 - d23;                // y3 = r0 - r1 - r2 + r3 = x0 - x2 - x1 + x3
                break;
            }
            case 3: {
                float d01 = values[ofs] - values[ofs + 1];
                float s01 = values[ofs] + values[ofs + 1];
                float d23 = values[ofs + 2] - values[ofs + 3];
                float s23 = values[ofs + 2] + values[ofs + 3];
                float ds0123 = s01 - s23;
                float ss0123 = s01 + s23;
                float dd0123 = d01 - d23;
                float sd0123 = d01 + d23;
                float s45 = values[ofs + 4] + values[ofs + 5];
                float s67 = values[ofs + 6] + values[ofs + 7];
                float d45 = values[ofs + 4] - values[ofs + 5];
                float d67 = values[ofs + 6] - values[ofs + 7];
                float ds4567 = s45 - s67;
                float ss4567 = s45 + s67;
                values[ofs + 4] = ss0123 - ss4567;          // y4 = r0 + r1 + r2 + r3 - r4 - r5 - r6 - r7
                values[ofs] = ss0123 + ss4567;              // y0 = r0 + ... + r7 = x0 + ... + x7
                values[ofs + 6] = ds0123 - ds4567;          // y6 = r0 + r1 - r2 - r3 - r4 - r5 + r6 + r7
                                                            //    = x0 + x4 - x2 - x6 - x1 - x5 + x3 + x7
                values[ofs + 2] = ds0123 + ds4567;          // y2 = r0 + r1 - r2 - r3 + r4 + r5 - r6 - r7
                                                            //    = x0 + x4 - x2 - x6 + x1 + x5 - x3 - x7
                d45 *= SQRT2;
                d67 *= SQRT2;
                values[ofs + 5] = sd0123 - d45;             // y5 = r0 - r1 + r2 - r3 - sqrt(2)*r4 + sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 - sqrt(2)*x1 + sqrt(2)*x5
                values[ofs + 1] = sd0123 + d45;             // y1 = r0 - r1 + r2 - r3 + sqrt(2)*r4 - sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 + sqrt(2)*x1 - sqrt(2)*x5
                values[ofs + 7] = dd0123 - d67;             // y7 = r0 - r1 - r2 + r3 - sqrt(2)*r6 + sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 - sqrt(2)*x3 + sqrt(2)*x7
                values[ofs + 3] = dd0123 + d67;             // y3 = r0 - r1 - r2 + r3 + sqrt(2)*r6 - sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 + sqrt(2)*x3 - sqrt(2)*x7
                break;
            }
            default: {
                assert logN > 3;
                final int nDiv8 = 1 << (logN - 3);
                final int nDiv4 = nDiv8 * 2;
                final int nDiv2 = nDiv4 * 2;
                fhtJavaFloatMainLoop(context, samples, pos, logN - 1, originalLogN);
                fhtJavaFloatMainLoop(context, samples, pos + nDiv2, logN - 1, originalLogN);

                final boolean allAnglesInCache = logN - 1 <= RootsOfUnity.LOG_CACHE_SIZE + LOG_ANGLE_STEP;
                final double rotationAngle = StrictMath.PI / nDiv2;
                final double sin0Half = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN]; //sin(0.5 * rotationAngle);
                final double cos0M1 = -2.0 * sin0Half * sin0Half; // cos(rotationAngle)-1
                final double sin0 = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN - 1]; //sin(rotationAngle)
                double cos = 1.0 + cos0M1;
                double sin = sin0;
                int j1, j2;
                float r1, r2, cas, l1, l2;
                for (int j = 1; j < nDiv8; j++) {
                    // (cos, sin) = (cos(j*2*PI/N),  sin(j*2*PI/N))
                    j1 = ofs + j;
                    j2 = ofs + nDiv2 - j;
                    //[[Repeat.SectionStart JavaFloatButterfly]]
                    r1 = values[nDiv2 + j1];
                    r2 = values[nDiv2 + j2];
                    cas = (float)(r1 * cos + r2 * sin);
                    l1 = values[j1];
                    values[j1] = l1 + cas;
                    values[nDiv2 + j1] = l1 - cas;
                    cas = (float)(r1 * sin - r2 * cos);
                    l2 = values[j2];
                    values[j2] = l2 + cas;
                    values[nDiv2 + j2] = l2 - cas;
                    //[[Repeat.SectionEnd JavaFloatButterfly]]

                    j1 = ofs + nDiv4 - j;
                    j2 = ofs + nDiv4 + j; // below is the same code as above with exchanging sin<->cos
                    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, JavaFloatButterfly)
                    //  cos ==> TTTT;; sin ==> cos;; TTTT ==> sin !! Generated by Repeater: DO NOT EDIT !! ]]
                    r1 = values[nDiv2 + j1];
                    r2 = values[nDiv2 + j2];
                    cas = (float)(r1 * sin + r2 * cos);
                    l1 = values[j1];
                    values[j1] = l1 + cas;
                    values[nDiv2 + j1] = l1 - cas;
                    cas = (float)(r1 * cos - r2 * sin);
                    l2 = values[j2];
                    values[j2] = l2 + cas;
                    values[nDiv2 + j2] = l2 - cas;
                    //[[Repeat.IncludeEnd]]

                    if ((j & (ANGLE_STEP - 1)) == ANGLE_STEP - 1) {
                        if (allAnglesInCache) {
                            int angleIndex = (j + 1) >> LOG_ANGLE_STEP
                                << (RootsOfUnity.LOG_CACHE_SIZE - (logN - 1) + LOG_ANGLE_STEP);
                            // (j + 1) * CACHE_SIZE/nDiv2
                            // cos = RootsOfUnity.quickCos(angleIndex);
                            // sin = RootsOfUnity.quickSin(angleIndex);
                            cos = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                                -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                            sin = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[angleIndex] :
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        } else {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            cos = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            sin = Math.sin(angle);
                        }
                    } else {
                        // on modern JVM and CPU, the following 4 multiplications and 4 additions work faster
                        // then extracting cos and sin from the table
                        double temp = cos;
                        cos = cos * cos0M1 - sin * sin0 + cos;
                        sin = sin * cos0M1 + temp * sin0 + sin;
                    }
                }
                j1 = ofs + nDiv8;
                j2 = ofs + nDiv2 - nDiv8;
                //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, JavaFloatButterfly)
                //  (cos|sin) ==> HALF_SQRT2;;
                //  \s\s\s\s(r1\s|r2\s|cas\s|l1\s|l2\s|values\b|$) ==> $1 !! Generated by Repeater: DO NOT EDIT !! ]]
                r1 = values[nDiv2 + j1];
                r2 = values[nDiv2 + j2];
                cas = (float)(r1 * HALF_SQRT2 + r2 * HALF_SQRT2);
                l1 = values[j1];
                values[j1] = l1 + cas;
                values[nDiv2 + j1] = l1 - cas;
                cas = (float)(r1 * HALF_SQRT2 - r2 * HALF_SQRT2);
                l2 = values[j2];
                values[j2] = l2 + cas;
                values[nDiv2 + j2] = l2 - cas;
                //[[Repeat.IncludeEnd]]

                float temp = values[ofs] - values[ofs + nDiv2];
                values[ofs] += values[ofs + nDiv2];         // r[0] + r[n/2]
                values[ofs + nDiv2] = temp;                 // r[0] - r[n/2]

                temp = values[ofs + nDiv4] - values[ofs + nDiv2 + nDiv4];
                values[ofs + nDiv4] += values[ofs + nDiv2 + nDiv4];         // r[n/4] + r[3*n/4]
                values[ofs + nDiv2 + nDiv4] = temp;                         // r[n/4] - r[3*n/4]
            }

            if (context != null && (pos + (1 << logN) == samples.length())) {
                context.checkInterruptionAndUpdateProgress(null, logN, originalLogN);
            }
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void fhtJavaDoubleMainLoop(ArrayContext context, SampleArray samples,
        final int pos, final int logN, final int originalLogN)
    {
        final double[] values = samples instanceof RealScalarSampleArray.DirectZeroOffsetsRealDoubleSampleArray ?
            ((RealScalarSampleArray.DirectZeroOffsetsRealDoubleSampleArray)samples).samples :
            ((RealScalarSampleArray.DirectRealDoubleSampleArray)samples).samples;
        final int ofs = pos + (samples instanceof RealScalarSampleArray.DirectZeroOffsetsRealDoubleSampleArray ? 0 :
            ((RealScalarSampleArray.DirectRealDoubleSampleArray)samples).ofs);
        switch (logN) { // in comments below xK, rK, yK means the source, the reordered source and the result
            case 1: {
                double temp = values[ofs] - values[ofs + 1];
                values[ofs] += values[ofs + 1];             // y0 = x0 + x1
                values[ofs + 1] = temp;                     // y1 = x0 - x1
                break;
            }
            case 2: {
                double d01 = values[ofs] - values[ofs + 1];
                double s01 = values[ofs] + values[ofs + 1];
                double d23 = values[ofs + 2] - values[ofs + 3];
                double s23 = values[ofs + 2] + values[ofs + 3];
                values[ofs] = s01 + s23;                    // y0 = r0 + r1 + r2 + r3 = x0 + x2 + x1 + x3
                values[ofs + 1] = d01 + d23;                // y1 = r0 - r1 + r2 - r3 = x0 - x2 + x1 - x3
                values[ofs + 2] = s01 - s23;                // y2 = r0 + r1 - r2 - r3 = x0 + x2 - x1 - x3
                values[ofs + 3] = d01 - d23;                // y3 = r0 - r1 - r2 + r3 = x0 - x2 - x1 + x3
                break;
            }
            case 3: {
                double d01 = values[ofs] - values[ofs + 1];
                double s01 = values[ofs] + values[ofs + 1];
                double d23 = values[ofs + 2] - values[ofs + 3];
                double s23 = values[ofs + 2] + values[ofs + 3];
                double ds0123 = s01 - s23;
                double ss0123 = s01 + s23;
                double dd0123 = d01 - d23;
                double sd0123 = d01 + d23;
                double s45 = values[ofs + 4] + values[ofs + 5];
                double s67 = values[ofs + 6] + values[ofs + 7];
                double d45 = values[ofs + 4] - values[ofs + 5];
                double d67 = values[ofs + 6] - values[ofs + 7];
                double ds4567 = s45 - s67;
                double ss4567 = s45 + s67;
                values[ofs + 4] = ss0123 - ss4567;          // y4 = r0 + r1 + r2 + r3 - r4 - r5 - r6 - r7
                values[ofs] = ss0123 + ss4567;              // y0 = r0 + ... + r7 = x0 + ... + x7
                values[ofs + 6] = ds0123 - ds4567;          // y6 = r0 + r1 - r2 - r3 - r4 - r5 + r6 + r7
                                                            //    = x0 + x4 - x2 - x6 - x1 - x5 + x3 + x7
                values[ofs + 2] = ds0123 + ds4567;          // y2 = r0 + r1 - r2 - r3 + r4 + r5 - r6 - r7
                                                            //    = x0 + x4 - x2 - x6 + x1 + x5 - x3 - x7
                d45 *= SQRT2;
                d67 *= SQRT2;
                values[ofs + 5] = sd0123 - d45;             // y5 = r0 - r1 + r2 - r3 - sqrt(2)*r4 + sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 - sqrt(2)*x1 + sqrt(2)*x5
                values[ofs + 1] = sd0123 + d45;             // y1 = r0 - r1 + r2 - r3 + sqrt(2)*r4 - sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 + sqrt(2)*x1 - sqrt(2)*x5
                values[ofs + 7] = dd0123 - d67;             // y7 = r0 - r1 - r2 + r3 - sqrt(2)*r6 + sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 - sqrt(2)*x3 + sqrt(2)*x7
                values[ofs + 3] = dd0123 + d67;             // y3 = r0 - r1 - r2 + r3 + sqrt(2)*r6 - sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 + sqrt(2)*x3 - sqrt(2)*x7
                break;
            }
            default: {
                assert logN > 3;
                final int nDiv8 = 1 << (logN - 3);
                final int nDiv4 = nDiv8 * 2;
                final int nDiv2 = nDiv4 * 2;
                fhtJavaDoubleMainLoop(context, samples, pos, logN - 1, originalLogN);
                fhtJavaDoubleMainLoop(context, samples, pos + nDiv2, logN - 1, originalLogN);

                final boolean allAnglesInCache = logN - 1 <= RootsOfUnity.LOG_CACHE_SIZE + LOG_ANGLE_STEP;
                final double rotationAngle = StrictMath.PI / nDiv2;
                final double sin0Half = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN]; //sin(0.5 * rotationAngle);
                final double cos0M1 = -2.0 * sin0Half * sin0Half; // cos(rotationAngle)-1
                final double sin0 = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN - 1]; //sin(rotationAngle)
                double cos = 1.0 + cos0M1;
                double sin = sin0;
                int j1, j2;
                double r1, r2, cas, l1, l2;
                for (int j = 1; j < nDiv8; j++) {
                    // (cos, sin) = (cos(j*2*PI/N),  sin(j*2*PI/N))
                    j1 = ofs + j;
                    j2 = ofs + nDiv2 - j;

                    r1 = values[nDiv2 + j1];
                    r2 = values[nDiv2 + j2];
                    cas = r1 * cos + r2 * sin;
                    l1 = values[j1];
                    values[j1] = l1 + cas;
                    values[nDiv2 + j1] = l1 - cas;
                    cas = r1 * sin - r2 * cos;
                    l2 = values[j2];
                    values[j2] = l2 + cas;
                    values[nDiv2 + j2] = l2 - cas;


                    j1 = ofs + nDiv4 - j;
                    j2 = ofs + nDiv4 + j; // below is the same code as above with exchanging sin<->cos

                    r1 = values[nDiv2 + j1];
                    r2 = values[nDiv2 + j2];
                    cas = r1 * sin + r2 * cos;
                    l1 = values[j1];
                    values[j1] = l1 + cas;
                    values[nDiv2 + j1] = l1 - cas;
                    cas = r1 * cos - r2 * sin;
                    l2 = values[j2];
                    values[j2] = l2 + cas;
                    values[nDiv2 + j2] = l2 - cas;


                    if ((j & (ANGLE_STEP - 1)) == ANGLE_STEP - 1) {
                        if (allAnglesInCache) {
                            int angleIndex = (j + 1) >> LOG_ANGLE_STEP
                                << (RootsOfUnity.LOG_CACHE_SIZE - (logN - 1) + LOG_ANGLE_STEP);
                            // (j + 1) * CACHE_SIZE/nDiv2
                            // cos = RootsOfUnity.quickCos(angleIndex);
                            // sin = RootsOfUnity.quickSin(angleIndex);
                            cos = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                                -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                            sin = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[angleIndex] :
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        } else {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            cos = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            sin = Math.sin(angle);
                        }
                    } else {
                        // on modern JVM and CPU, the following 4 multiplications and 4 additions work faster
                        // then extracting cos and sin from the table
                        double temp = cos;
                        cos = cos * cos0M1 - sin * sin0 + cos;
                        sin = sin * cos0M1 + temp * sin0 + sin;
                    }
                }
                j1 = ofs + nDiv8;
                j2 = ofs + nDiv2 - nDiv8;

                r1 = values[nDiv2 + j1];
                r2 = values[nDiv2 + j2];
                cas = r1 * HALF_SQRT2 + r2 * HALF_SQRT2;
                l1 = values[j1];
                values[j1] = l1 + cas;
                values[nDiv2 + j1] = l1 - cas;
                cas = r1 * HALF_SQRT2 - r2 * HALF_SQRT2;
                l2 = values[j2];
                values[j2] = l2 + cas;
                values[nDiv2 + j2] = l2 - cas;


                double temp = values[ofs] - values[ofs + nDiv2];
                values[ofs] += values[ofs + nDiv2];         // r[0] + r[n/2]
                values[ofs + nDiv2] = temp;                 // r[0] - r[n/2]

                temp = values[ofs + nDiv4] - values[ofs + nDiv2 + nDiv4];
                values[ofs + nDiv4] += values[ofs + nDiv2 + nDiv4];         // r[n/4] + r[3*n/4]
                values[ofs + nDiv2 + nDiv4] = temp;                         // r[n/4] - r[3*n/4]
            }

            if (context != null && (pos + (1 << logN) == samples.length())) {
                context.checkInterruptionAndUpdateProgress(null, logN, originalLogN);
            }
        }
    }
    //[[Repeat.AutoGeneratedEnd]]


    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\(([^)]+)\) ==> $1 ]]
    private static void fhtJavaFloatMultidimensionalMainLoop(ArrayContext context,
        RealVectorSampleArray.DirectRealFloatVectorSampleArray samples,
        final int pos, final int logN, final int originalLogN,
        RealVectorSampleArray.DirectRealFloatVectorSampleArray work)
    {
        final float[] values = samples.samples;
        final int step = (int)samples.vectorStep;
        final int ofs = pos * step + samples.ofs;
        switch (logN) { // in comments below xK, rK, yK means the source, the reordered source and the result
            case 1: {
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    float temp = values[k] - values[k + step];
                    values[k] += values[k + step];          // y0 = x0 + x1
                    values[k + step] = temp;                // y1 = x0 - x1
                }
                break;
            }
            case 2: {
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    float d01 = values[k] - values[k + step];
                    float s01 = values[k] + values[k + step];
                    float d23 = values[k + 2 * step] - values[k + 3 * step];
                    float s23 = values[k + 2 * step] + values[k + 3 * step];
                    values[k] = s01 + s23;                  // y0 = r0 + r1 + r2 + r3 = x0 + x2 + x1 + x3
                    values[k + step] = d01 + d23;           // y1 = r0 - r1 + r2 - r3 = x0 - x2 + x1 - x3
                    values[k + 2 * step] = s01 - s23;       // y2 = r0 + r1 - r2 - r3 = x0 + x2 - x1 - x3
                    values[k + 3 * step] = d01 - d23;       // y3 = r0 - r1 - r2 + r3 = x0 - x2 - x1 + x3
                }
                break;
            }
            case 3: {
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    float d01 = values[k] - values[k + step];
                    float s01 = values[k] + values[k + step];
                    float d23 = values[k + 2 * step] - values[k + 3 * step];
                    float s23 = values[k + 2 * step] + values[k + 3 * step];
                    float ds0123 = s01 - s23;
                    float ss0123 = s01 + s23;
                    float dd0123 = d01 - d23;
                    float sd0123 = d01 + d23;
                    float s45 = values[k + 4 * step] + values[k + 5 * step];
                    float s67 = values[k + 6 * step] + values[k + 7 * step];
                    float d45 = values[k + 4 * step] - values[k + 5 * step];
                    float d67 = values[k + 6 * step] - values[k + 7 * step];
                    float ds4567 = s45 - s67;
                    float ss4567 = s45 + s67;
                    values[k + 4 * step] = ss0123 - ss4567; // y4 = r0 + r1 + r2 + r3 - r4 - r5 - r6 - r7
                    values[k] = ss0123 + ss4567;            // y0 = r0 + ... + r7 = x0 + ... + x7
                    values[k + 6 * step] = ds0123 - ds4567; // y6 = r0 + r1 - r2 - r3 - r4 - r5 + r6 + r7
                                                            //    = x0 + x4 - x2 - x6 - x1 - x5 + x3 + x7
                    values[k + 2 * step] = ds0123 + ds4567; // y2 = r0 + r1 - r2 - r3 + r4 + r5 - r6 - r7
                                                            //    = x0 + x4 - x2 - x6 + x1 + x5 - x3 - x7
                    d45 *= SQRT2;
                    d67 *= SQRT2;
                    values[k + 5 * step] = sd0123 - d45;    // y5 = r0 - r1 + r2 - r3 - sqrt(2)*r4 + sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 - sqrt(2)*x1 + sqrt(2)*x5
                    values[k + step] = sd0123 + d45;        // y1 = r0 - r1 + r2 - r3 + sqrt(2)*r4 - sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 + sqrt(2)*x1 - sqrt(2)*x5
                    values[k + 7 * step] = dd0123 - d67;    // y7 = r0 - r1 - r2 + r3 - sqrt(2)*r6 + sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 - sqrt(2)*x3 + sqrt(2)*x7
                    values[k + 3 * step] = dd0123 + d67;    // y3 = r0 - r1 - r2 + r3 + sqrt(2)*r6 - sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 + sqrt(2)*x3 - sqrt(2)*x7

                }
                break;
            }
            default: {
                assert logN > 3;
                final int nDiv8 = 1 << (logN - 3);
                final int nDiv4 = nDiv8 * 2;
                final int nDiv2 = nDiv4 * 2;
                final int nDiv8Step = nDiv8 * step;
                final int nDiv4Step = nDiv8Step * 2;
                final int nDiv2Step = nDiv4Step * 2;
                fhtJavaFloatMultidimensionalMainLoop(context, samples, pos, logN - 1, originalLogN, work);
                fhtJavaFloatMultidimensionalMainLoop(context, samples, pos + nDiv2, logN - 1, originalLogN, work);

                final boolean allAnglesInCache = logN - 1 <= RootsOfUnity.LOG_CACHE_SIZE + LOG_ANGLE_STEP;
                final double rotationAngle = StrictMath.PI / nDiv2;
                final double sin0Half = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN]; //sin(0.5 * rotationAngle);
                final double cos0M1 = -2.0 * sin0Half * sin0Half; // cos(rotationAngle)-1
                final double sin0 = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN - 1]; //sin(rotationAngle)
                double cos = 1.0 + cos0M1;
                double sin = sin0;
                int j1, j2, j1Step, j2Step;
                float r1, r2, cas, l1, l2;
                for (int j = 1, jStep = step; j < nDiv8; j++, jStep += step) {
                    // (cos, sin) = (cos(j*2*PI/N),  sin(j*2*PI/N))
                    for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                        int j1Step1 = k + jStep;
                        int j2Step1 = k + nDiv2Step - jStep;
                        //[[Repeat.SectionStart JavaFloatMultidimensionalButterfly]]
                        r1 = values[nDiv2Step + j1Step1];
                        r2 = values[nDiv2Step + j2Step1];
                        cas = (float)(r1 * cos + r2 * sin);
                        l1 = values[j1Step1];
                        values[j1Step1] = l1 + cas;
                        values[nDiv2Step + j1Step1] = l1 - cas;
                        cas = (float)(r1 * sin - r2 * cos);
                        l2 = values[j2Step1];
                        values[j2Step1] = l2 + cas;
                        values[nDiv2Step + j2Step1] = l2 - cas;
                        //[[Repeat.SectionEnd JavaFloatMultidimensionalButterfly]]

                        j1Step1 = k + nDiv4Step - jStep;
                        j2Step1 = k + nDiv4Step + jStep;
                        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, JavaFloatMultidimensionalButterfly)
                        //  cos ==> TTTT;; sin ==> cos;; TTTT ==> sin !! Generated by Repeater: DO NOT EDIT !! ]]
                        r1 = values[nDiv2Step + j1Step1];
                        r2 = values[nDiv2Step + j2Step1];
                        cas = (float)(r1 * sin + r2 * cos);
                        l1 = values[j1Step1];
                        values[j1Step1] = l1 + cas;
                        values[nDiv2Step + j1Step1] = l1 - cas;
                        cas = (float)(r1 * cos - r2 * sin);
                        l2 = values[j2Step1];
                        values[j2Step1] = l2 + cas;
                        values[nDiv2Step + j2Step1] = l2 - cas;
                        //[[Repeat.IncludeEnd]]
                    }
                    if ((j & (ANGLE_STEP - 1)) == ANGLE_STEP - 1) {
                        if (allAnglesInCache) {
                            int angleIndex = (j + 1) >> LOG_ANGLE_STEP
                                << (RootsOfUnity.LOG_CACHE_SIZE - (logN - 1) + LOG_ANGLE_STEP);
                            // (j + 1) * CACHE_SIZE/nDiv2
                            // cos = RootsOfUnity.quickCos(angleIndex);
                            // sin = RootsOfUnity.quickSin(angleIndex);
                            cos = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                                -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                            sin = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[angleIndex] :
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        } else {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            cos = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            sin = Math.sin(angle);
                        }
                    } else {
                        // on modern JVM and CPU, the following 4 multiplications and 4 additions work faster
                        // then extracting cos and sin from the table
                        double temp = cos;
                        cos = cos * cos0M1 - sin * sin0 + cos;
                        sin = sin * cos0M1 + temp * sin0 + sin;
                    }
                }
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    j1Step = k + nDiv8Step;
                    j2Step = k + nDiv2Step - nDiv8Step;
                    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, JavaFloatMultidimensionalButterfly)
                    //  (cos|sin) ==> HALF_SQRT2;;
                    //  \s\s\s\s(r1\s|r2\s|cas\s|l1\s|l2\s|values\b|$) ==> $1 !! Generated by Repeater: DO NOT EDIT !! ]]
                    r1 = values[nDiv2Step + j1Step];
                    r2 = values[nDiv2Step + j2Step];
                    cas = (float)(r1 * HALF_SQRT2 + r2 * HALF_SQRT2);
                    l1 = values[j1Step];
                    values[j1Step] = l1 + cas;
                    values[nDiv2Step + j1Step] = l1 - cas;
                    cas = (float)(r1 * HALF_SQRT2 - r2 * HALF_SQRT2);
                    l2 = values[j2Step];
                    values[j2Step] = l2 + cas;
                    values[nDiv2Step + j2Step] = l2 - cas;
                    //[[Repeat.IncludeEnd]]

                    float temp = values[k] - values[k + nDiv2Step];
                    values[k] += values[k + nDiv2Step];         // r[0] + r[n/2]
                    values[k + nDiv2Step] = temp;               // r[0] - r[n/2]

                    temp = values[k + nDiv4Step] - values[k + nDiv2Step + nDiv4Step];
                    values[k + nDiv4Step] += values[k + nDiv2Step + nDiv4Step]; // r[n/4] + r[3*n/4]
                    values[k + nDiv2Step + nDiv4Step] = temp;                   // r[n/4] - r[3*n/4]
                }
            }
            if (context != null && (pos + (1 << logN) == samples.length())) {
                context.checkInterruptionAndUpdateProgress(null, logN, originalLogN);
            }
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void fhtJavaDoubleMultidimensionalMainLoop(ArrayContext context,
        RealVectorSampleArray.DirectRealDoubleVectorSampleArray samples,
        final int pos, final int logN, final int originalLogN,
        RealVectorSampleArray.DirectRealDoubleVectorSampleArray work)
    {
        final double[] values = samples.samples;
        final int step = (int)samples.vectorStep;
        final int ofs = pos * step + samples.ofs;
        switch (logN) { // in comments below xK, rK, yK means the source, the reordered source and the result
            case 1: {
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    double temp = values[k] - values[k + step];
                    values[k] += values[k + step];          // y0 = x0 + x1
                    values[k + step] = temp;                // y1 = x0 - x1
                }
                break;
            }
            case 2: {
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    double d01 = values[k] - values[k + step];
                    double s01 = values[k] + values[k + step];
                    double d23 = values[k + 2 * step] - values[k + 3 * step];
                    double s23 = values[k + 2 * step] + values[k + 3 * step];
                    values[k] = s01 + s23;                  // y0 = r0 + r1 + r2 + r3 = x0 + x2 + x1 + x3
                    values[k + step] = d01 + d23;           // y1 = r0 - r1 + r2 - r3 = x0 - x2 + x1 - x3
                    values[k + 2 * step] = s01 - s23;       // y2 = r0 + r1 - r2 - r3 = x0 + x2 - x1 - x3
                    values[k + 3 * step] = d01 - d23;       // y3 = r0 - r1 - r2 + r3 = x0 - x2 - x1 + x3
                }
                break;
            }
            case 3: {
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    double d01 = values[k] - values[k + step];
                    double s01 = values[k] + values[k + step];
                    double d23 = values[k + 2 * step] - values[k + 3 * step];
                    double s23 = values[k + 2 * step] + values[k + 3 * step];
                    double ds0123 = s01 - s23;
                    double ss0123 = s01 + s23;
                    double dd0123 = d01 - d23;
                    double sd0123 = d01 + d23;
                    double s45 = values[k + 4 * step] + values[k + 5 * step];
                    double s67 = values[k + 6 * step] + values[k + 7 * step];
                    double d45 = values[k + 4 * step] - values[k + 5 * step];
                    double d67 = values[k + 6 * step] - values[k + 7 * step];
                    double ds4567 = s45 - s67;
                    double ss4567 = s45 + s67;
                    values[k + 4 * step] = ss0123 - ss4567; // y4 = r0 + r1 + r2 + r3 - r4 - r5 - r6 - r7
                    values[k] = ss0123 + ss4567;            // y0 = r0 + ... + r7 = x0 + ... + x7
                    values[k + 6 * step] = ds0123 - ds4567; // y6 = r0 + r1 - r2 - r3 - r4 - r5 + r6 + r7
                                                            //    = x0 + x4 - x2 - x6 - x1 - x5 + x3 + x7
                    values[k + 2 * step] = ds0123 + ds4567; // y2 = r0 + r1 - r2 - r3 + r4 + r5 - r6 - r7
                                                            //    = x0 + x4 - x2 - x6 + x1 + x5 - x3 - x7
                    d45 *= SQRT2;
                    d67 *= SQRT2;
                    values[k + 5 * step] = sd0123 - d45;    // y5 = r0 - r1 + r2 - r3 - sqrt(2)*r4 + sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 - sqrt(2)*x1 + sqrt(2)*x5
                    values[k + step] = sd0123 + d45;        // y1 = r0 - r1 + r2 - r3 + sqrt(2)*r4 - sqrt(2)*r5
                                                            //    = x0 - x4 + x2 - x6 + sqrt(2)*x1 - sqrt(2)*x5
                    values[k + 7 * step] = dd0123 - d67;    // y7 = r0 - r1 - r2 + r3 - sqrt(2)*r6 + sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 - sqrt(2)*x3 + sqrt(2)*x7
                    values[k + 3 * step] = dd0123 + d67;    // y3 = r0 - r1 - r2 + r3 + sqrt(2)*r6 - sqrt(2)*r7
                                                            //    = x0 - x4 - x2 + x6 + sqrt(2)*x3 - sqrt(2)*x7

                }
                break;
            }
            default: {
                assert logN > 3;
                final int nDiv8 = 1 << (logN - 3);
                final int nDiv4 = nDiv8 * 2;
                final int nDiv2 = nDiv4 * 2;
                final int nDiv8Step = nDiv8 * step;
                final int nDiv4Step = nDiv8Step * 2;
                final int nDiv2Step = nDiv4Step * 2;
                fhtJavaDoubleMultidimensionalMainLoop(context, samples, pos, logN - 1, originalLogN, work);
                fhtJavaDoubleMultidimensionalMainLoop(context, samples, pos + nDiv2, logN - 1, originalLogN, work);

                final boolean allAnglesInCache = logN - 1 <= RootsOfUnity.LOG_CACHE_SIZE + LOG_ANGLE_STEP;
                final double rotationAngle = StrictMath.PI / nDiv2;
                final double sin0Half = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN]; //sin(0.5 * rotationAngle);
                final double cos0M1 = -2.0 * sin0Half * sin0Half; // cos(rotationAngle)-1
                final double sin0 = RootsOfUnity.LOGARITHMICAL_SINE_CACHE[logN - 1]; //sin(rotationAngle)
                double cos = 1.0 + cos0M1;
                double sin = sin0;
                int j1, j2, j1Step, j2Step;
                double r1, r2, cas, l1, l2;
                for (int j = 1, jStep = step; j < nDiv8; j++, jStep += step) {
                    // (cos, sin) = (cos(j*2*PI/N),  sin(j*2*PI/N))
                    for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                        int j1Step1 = k + jStep;
                        int j2Step1 = k + nDiv2Step - jStep;

                        r1 = values[nDiv2Step + j1Step1];
                        r2 = values[nDiv2Step + j2Step1];
                        cas = r1 * cos + r2 * sin;
                        l1 = values[j1Step1];
                        values[j1Step1] = l1 + cas;
                        values[nDiv2Step + j1Step1] = l1 - cas;
                        cas = r1 * sin - r2 * cos;
                        l2 = values[j2Step1];
                        values[j2Step1] = l2 + cas;
                        values[nDiv2Step + j2Step1] = l2 - cas;


                        j1Step1 = k + nDiv4Step - jStep;
                        j2Step1 = k + nDiv4Step + jStep;

                        r1 = values[nDiv2Step + j1Step1];
                        r2 = values[nDiv2Step + j2Step1];
                        cas = r1 * sin + r2 * cos;
                        l1 = values[j1Step1];
                        values[j1Step1] = l1 + cas;
                        values[nDiv2Step + j1Step1] = l1 - cas;
                        cas = r1 * cos - r2 * sin;
                        l2 = values[j2Step1];
                        values[j2Step1] = l2 + cas;
                        values[nDiv2Step + j2Step1] = l2 - cas;

                    }
                    if ((j & (ANGLE_STEP - 1)) == ANGLE_STEP - 1) {
                        if (allAnglesInCache) {
                            int angleIndex = (j + 1) >> LOG_ANGLE_STEP
                                << (RootsOfUnity.LOG_CACHE_SIZE - (logN - 1) + LOG_ANGLE_STEP);
                            // (j + 1) * CACHE_SIZE/nDiv2
                            // cos = RootsOfUnity.quickCos(angleIndex);
                            // sin = RootsOfUnity.quickSin(angleIndex);
                            cos = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.HALF_CACHE_SIZE - angleIndex] :
                                -RootsOfUnity.SINE_CACHE[angleIndex - RootsOfUnity.HALF_CACHE_SIZE];
                            sin = angleIndex < RootsOfUnity.HALF_CACHE_SIZE ?
                                RootsOfUnity.SINE_CACHE[angleIndex] :
                                RootsOfUnity.SINE_CACHE[RootsOfUnity.CACHE_SIZE - angleIndex];
                        } else {
                            double angle = (j + 1) * rotationAngle;
                            double sinHalf = Math.sin(0.5 * angle);
                            cos = 1.0 - 2.0 * sinHalf * sinHalf; // = cos(angle)
                            sin = Math.sin(angle);
                        }
                    } else {
                        // on modern JVM and CPU, the following 4 multiplications and 4 additions work faster
                        // then extracting cos and sin from the table
                        double temp = cos;
                        cos = cos * cos0M1 - sin * sin0 + cos;
                        sin = sin * cos0M1 + temp * sin0 + sin;
                    }
                }
                for (int k = ofs, kMax = k + samples.vectorLen; k < kMax; k++) {
                    j1Step = k + nDiv8Step;
                    j2Step = k + nDiv2Step - nDiv8Step;

                    r1 = values[nDiv2Step + j1Step];
                    r2 = values[nDiv2Step + j2Step];
                    cas = r1 * HALF_SQRT2 + r2 * HALF_SQRT2;
                    l1 = values[j1Step];
                    values[j1Step] = l1 + cas;
                    values[nDiv2Step + j1Step] = l1 - cas;
                    cas = r1 * HALF_SQRT2 - r2 * HALF_SQRT2;
                    l2 = values[j2Step];
                    values[j2Step] = l2 + cas;
                    values[nDiv2Step + j2Step] = l2 - cas;


                    double temp = values[k] - values[k + nDiv2Step];
                    values[k] += values[k + nDiv2Step];         // r[0] + r[n/2]
                    values[k + nDiv2Step] = temp;               // r[0] - r[n/2]

                    temp = values[k + nDiv4Step] - values[k + nDiv2Step + nDiv4Step];
                    values[k + nDiv4Step] += values[k + nDiv2Step + nDiv4Step]; // r[n/4] + r[3*n/4]
                    values[k + nDiv2Step + nDiv4Step] = temp;                   // r[n/4] - r[3*n/4]
                }
            }
            if (context != null && (pos + (1 << logN) == samples.length())) {
                context.checkInterruptionAndUpdateProgress(null, logN, originalLogN);
            }
        }
    }
    //[[Repeat.AutoGeneratedEnd]]
}