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

package net.algart.matrices.spectra.demo;

import net.algart.matrices.spectra.*;
import net.algart.arrays.*;

public class FourierHartleyTest {
    static final long MAX_CHECKED_LENGTH = 2048; // maximal length to be compare with slow "discreteXxxTransform"

    public static void discreteFourierTransform(SampleArray result, SampleArray samples) {
        final long n = samples.length();
        SampleArray work = samples.newCompatibleSamplesArray(1);
        for (long k = 0; k < n; k++) {
            result.copy(k, samples, 0);
            for (long j = 1; j < n; j++) {
                double angle = -2 * Math.PI * k * j / n;
                work.multiplyByScalar(0, samples, j, Math.cos(angle), Math.sin(angle));
                result.add(k, k, work, 0);
            }
        }
    }

    public static void discreteHartleyTransform(SampleArray result, SampleArray samples) {
        final long n = samples.length();
        SampleArray work = samples.newCompatibleSamplesArray(1);
        for (long k = 0; k < n; k++) {
            result.copy(k, samples, 0);
            for (long j = 1; j < n; j++) {
                double angle = 2 * Math.PI * k * j / n;
                work.multiplyByScalar(0, samples, j, Math.cos(angle) + Math.sin(angle), 0);
                result.add(k, k, work, 0);
            }
        }
    }

    public static void discreteConvolution(UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
        PNumberArray pRe, PNumberArray pIm, PNumberArray qRe, PNumberArray qIm) {
        for (long k = 0, n = cRe.length(); k < n; k++) {
            double sumRe = 0.0, sumIm = 0.0;
            for (long j = 0; j < n; j++) {
                long i = k - j;
                if (i < 0) {
                    i += n;
                }
                double re1 = pRe.getDouble(j), im1 = pIm.getDouble(j);
                double re2 = qRe.getDouble(i), im2 = qIm.getDouble(i);
                sumRe += re1 * re2 - im1 * im2;
                sumIm += re1 * im2 + re2 * im1;
            }
            cRe.setDouble(k, sumRe);
            cIm.setDouble(k, sumIm);
        }
    }

    private static double maxDiff(PNumberArray re1, PNumberArray im1, PNumberArray re2, PNumberArray im2) {
        double result = 0.0;
        for (long k = 0, n = re1.length(); k < n; k++) {
            result = Math.max(result, Math.abs(re1.getDouble(k) - re2.getDouble(k)));
            result = Math.max(result, Math.abs(im1.getDouble(k) - im2.getDouble(k)));
        }
        return result;
    }

    private static SampleArray getSampleArray(MemoryModel mm, UpdatablePNumberArray re, UpdatablePNumberArray im,
        boolean realOnly, long step2D)
    {
        return step2D == 0 ?
            (realOnly ?
                RealScalarSampleArray.asSampleArray(re) :
                ComplexScalarSampleArray.asSampleArray(re, im)) :
            (realOnly ?
                RealVectorSampleArray.asSampleArray(mm, re, step2D, step2D, re.length() / step2D) :
                ComplexVectorSampleArray.asSampleArray(mm, re, im, step2D, step2D, re.length() / step2D));
    }

    public static void main(String[] args) {
        int startArgIndex = 0;
        boolean checkSubarrays = false, realOnly = false, rectangularFunction = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-realOnly")) {
            realOnly = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-checkSubarrays")) {
            checkSubarrays = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-rectangularFunction")) {
            rectangularFunction = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.println("Usage: " + FourierHartleyTest.class.getName()
                + " [-realOnly] [-checkSubarrays] [-rectangularFunction] double|float|long|int|short|byte"
                + " numberOfElements numberOfTests [step2D]");
            return;
        }

        final String elementType = args[startArgIndex];
        final Class<?> clazz =
            elementType.equals("byte") ? byte.class :
            elementType.equals("short") ? short.class :
            elementType.equals("int") ? int.class :
            elementType.equals("long") ? long.class :
            elementType.equals("float") ? float.class :
            elementType.equals("double") ? double.class :
            null;
        if (clazz == null)
            throw new IllegalArgumentException("Unallowed element type: " + elementType);

        long n = Long.parseLong(args[startArgIndex + 1]);
        n = n <= 1 ? n : 2 * Long.highestOneBit(n - 1);
        final double nLogN = n * Math.log(n) / Math.log(2);
        int numberOfTests = Integer.parseInt(args[startArgIndex + 2]);
        long step2D = args.length > startArgIndex + 3 ? Long.parseLong(args[startArgIndex + 3]) : 0;
        System.out.println("Checking Fast Fourier trasform for " + n + " elements"
            + (realOnly ? " (real numbers only)" : "")
            + (checkSubarrays? " (subarrays)" : ""));

        final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        System.out.println(mm);

        final long arrayLen = checkSubarrays ? n + 10 : n;
        UpdatablePNumberArray reOriginal = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray imOriginal = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray reOriginal2 = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray imOriginal2 = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray reConvolution  = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray imConvolution = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray re = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray im = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray reFFT = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray imFFT = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray reSFHT = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        UpdatablePNumberArray imSFHT = (UpdatablePNumberArray)mm.newUnresizableArray(clazz, arrayLen);
        if (checkSubarrays) {
            reOriginal = (UpdatablePNumberArray)reOriginal.subArr(5, n);
            imOriginal = (UpdatablePNumberArray)imOriginal.subArr(3, n);
            reOriginal2 = (UpdatablePNumberArray)reOriginal2.subArr(5, n);
            imOriginal2 = (UpdatablePNumberArray)imOriginal2.subArr(3, n);
            reConvolution = (UpdatablePNumberArray)reConvolution.subArr(5, n);
            imConvolution = (UpdatablePNumberArray)imConvolution.subArr(3, n);
            re = (UpdatablePNumberArray)re.subArr(5, n);
            im = (UpdatablePNumberArray)im.subArr(3, n);
            reFFT = (UpdatablePNumberArray)reFFT.subArr(5, n);
            imFFT = (UpdatablePNumberArray)imFFT.subArr(3, n);
            reSFHT = (UpdatablePNumberArray)reSFHT.subArr(5, n);
            imSFHT = (UpdatablePNumberArray)imSFHT.subArr(3, n);
        }

        FastFourierTransform fft = new FastFourierTransform(false);
        SeparableFastHartleyTransform sfht = new SeparableFastHartleyTransform(false);
        for (int testIndex = 1; testIndex <= numberOfTests; testIndex++) {
            System.out.println();
            System.out.println("Test #" + testIndex);
            for (int k = 0; k < n; k++) {
                if (rectangularFunction) {
                    reOriginal.setDouble(k, k < 2 ? 1 : 0);
                    reOriginal2.setDouble(k, k < 14 ? 11 : 0);
                } else {
                    reOriginal.setDouble(k, -5 + k * 3 / 4 + (k / 10) % 10);
                    reOriginal2.setDouble(k, -30 + k * 2 / 3 + (k / 5) % 5);
                }
            }
            if (realOnly) {
                imOriginal.fill(0.0);
                imOriginal2.fill(0.0);
            } else {
                for (int k = 0; k < n; k++) {
                    if (rectangularFunction) {
                        imOriginal.setDouble(k, k < 10 ? 7 : 0);
                        imOriginal2.setDouble(k, k < 14 ? 18 : 0);
                    } else {
                        imOriginal.setDouble(k, -1 + k * 5 / 6 + (k / 12) % 12);
                        imOriginal2.setDouble(k, -11 + k * 7 / 8 + (k / 14) % 14);
                    }
                }
            }
            re.copy(reOriginal);
            im.copy(imOriginal);
            SampleArray samplesComplex = getSampleArray(mm, re, im, false, step2D);
            SampleArray samples = getSampleArray(mm, re, im, realOnly, step2D);
            SampleArray samples2 = getSampleArray(mm, reOriginal2, imOriginal2, realOnly, step2D);
            SampleArray samplesConvolutionComplex = getSampleArray(mm, reConvolution, imConvolution, false, step2D);
            SampleArray samplesConvolution = getSampleArray(mm, reConvolution, imConvolution, realOnly, step2D);
            System.out.println(samplesComplex.length() + " complex samples created: " + samplesComplex);
            if (realOnly) {
                System.out.println(samples.length() + " real samples created: " + samples);
            }
            System.out.printf("%nSource:    %s", samples.toString("%.3f", ", ", 500));
            System.out.printf("%nSource #2: %s%n%n", samples2.toString("%.3f", ", ", 500));
            double maxDiff;

            if (n <= MAX_CHECKED_LENGTH) {
                SampleArray samplesDFT = getSampleArray(mm, reFFT, imFFT, false, step2D);
                discreteFourierTransform(samplesDFT, samplesComplex);
                System.out.printf("DFT:         %s%n", samplesDFT.toString("%.3f", ", ", 500));
                SampleArray samplesDHT = getSampleArray(mm, reSFHT, imSFHT, false, step2D);
                discreteHartleyTransform(samplesDHT, samplesComplex);
                System.out.printf("DHT:         %s%n", samplesDHT.toString("%.3f", ", ", 500));
                discreteHartleyTransform(samplesDHT, samplesComplex);
            }

            long t1 = System.nanoTime();
            fft.directTransform(null, samplesComplex);
            long t2 = System.nanoTime();
            System.out.printf("%nFFT: %.3f ms, %.3f ns * N, %.3f ns * N log N%n  %s%n", (t2 - t1) * 1e-6,
                (t2 - t1 + 0.0) / n, (t2 - t1) / nLogN, samplesComplex.toString("%.3f", ", ", 500));
            if (n <= MAX_CHECKED_LENGTH) {
                maxDiff = maxDiff(re, im, reFFT, imFFT);
                System.out.println("Maximal difference from the simplest algorithm: " + maxDiff);
                if (maxDiff >= 0.1 * Math.max(n, 1) && re instanceof PFloatingArray)
                    throw new AssertionError("Too big error for floating-point FFT/DFT");
            }
            reFFT.copy(re);
            imFFT.copy(im);
            t1 = System.nanoTime();
            fft.inverseTransform(null, samplesComplex);
            t2 = System.nanoTime();
            System.out.printf("%nReverse FFT: %.3f ms, %.3f ns * N, %.3f ns * N log N%n  %s%n", (t2 - t1) * 1e-6,
                (t2 - t1 + 0.0) / n, (t2 - t1) / nLogN, samplesComplex.toString("%.3f", ", ", 500));
            maxDiff = maxDiff(re, im, reOriginal, imOriginal);
            System.out.println("Maximal error: " + maxDiff);
            if (maxDiff >= 100.0 && re instanceof PFloatingArray)
                throw new AssertionError("Too big error for floating-point FFT");
            if (n <= MAX_CHECKED_LENGTH && step2D == 0) {
                re.copy(reOriginal2);
                im.copy(imOriginal2);
                fft.directTransform(null, samplesComplex);
                fft.spectrumOfConvolution(null,
                    Matrices.matrix(re, re.length()),
                    Matrices.matrix(im, im.length()),
                    Matrices.matrix(re, re.length()),
                    Matrices.matrix(im, im.length()),
                    Matrices.matrix(reFFT, reFFT.length()),
                    Matrices.matrix(imFFT, imFFT.length()));
                fft.inverseTransform(null, samplesComplex);
                discreteConvolution(reConvolution, imConvolution,
                    reOriginal, imOriginal, reOriginal2, imOriginal2);
                System.out.printf("Convolution via FFT:  %s%n", samplesComplex.toString("%.3f", ", ", 500));
                System.out.printf("Convolution:          %s%n", samplesConvolutionComplex.toString("%.3f", ", ", 500));
                maxDiff = maxDiff(re, im, reConvolution, imConvolution);
                System.out.println("Maximal difference from the simplest convolution (FFT): " + maxDiff);
                if (maxDiff >= Math.max(n, 1) && re instanceof PFloatingArray)
                    throw new AssertionError("Too big error for floating-point Fourier spectrum of convolution");
            }

            re.copy(reOriginal);
            im.copy(imOriginal);
            t1 = System.nanoTime();
            sfht.directTransform(null, samples);
            t2 = System.nanoTime();
            System.out.printf("%nFHT: %.3f ms, %.3f ns * N, %.3f ns * N log N%n  %s%n", (t2 - t1) * 1e-6,
                (t2 - t1 + 0.0) / n, (t2 - t1) / nLogN, samples.toString("%.3f", ", ", 500));
            if (n <= MAX_CHECKED_LENGTH) {
                maxDiff = maxDiff(re, im, reSFHT, imSFHT);
                System.out.println("Maximal difference from the simplest algorithm: " + maxDiff);
                if (maxDiff >= 0.1 * Math.max(n, 1) && re instanceof PFloatingArray)
                    throw new AssertionError("Too big error for floating-point FHT/DHT");
            }
            reSFHT.copy(re);
            imSFHT.copy(im);
            t1 = System.nanoTime();
            sfht.inverseTransform(null, samples);
            t2 = System.nanoTime();
            System.out.printf("%nReverse FHT: %.3f ms, %.3f ns * N, %.3f ns * N log N%n  %s%n", (t2 - t1) * 1e-6,
                (t2 - t1 + 0.0) / n, (t2 - t1) / nLogN, samples.toString("%.3f", ", ", 500));
            maxDiff = maxDiff(re, im, reOriginal, imOriginal);
            System.out.println("Maximal error: " + maxDiff);
            if (maxDiff >= 100.0 && re instanceof PFloatingArray)
                throw new AssertionError("Too big error for floating-point FHT");

            if (step2D == 0) {
                t1 = System.nanoTime();
                sfht.separableHartleyToFourier(null,
                    Matrices.matrix(re, re.length()),
                    Matrices.matrix(im, im.length()),
                    Matrices.matrix(reSFHT, reSFHT.length()),
                    Matrices.matrix(imSFHT, imSFHT.length()));
                t2 = System.nanoTime();
                System.out.printf("%nFFT via FHT: %.3f ms, %.3f ns * N%n  %s%n", (t2 - t1) * 1e-6,
                    (t2 - t1 + 0.0) / n, samplesComplex.toString("%.3f", ", ", 500));
                maxDiff = maxDiff(re, im, reFFT, imFFT);
                System.out.println("Maximal difference between FFT and Fourier transform,"
                    + " calculated via FHT: " + maxDiff + " = " + maxDiff / n + " * N");
                if (maxDiff >= 2.0 * Math.max(n, 1) && re instanceof PFloatingArray)
                    throw new AssertionError("Too big error for floating-point separableHartleyToFourier");

                t1 = System.nanoTime();
                sfht.fourierToSeparableHartley(null,
                    Matrices.matrix(re, re.length()),
                    Matrices.matrix(im, im.length()),
                    Matrices.matrix(reFFT, reFFT.length()),
                    Matrices.matrix(imFFT, imFFT.length()));
                t2 = System.nanoTime();
                System.out.printf("%nFHT via FFT: %.3f ms, %.3f ns * N%n  %s%n", (t2 - t1) * 1e-6,
                    (t2 - t1 + 0.0) / n, samplesComplex.toString("%.3f", ", ", 500));
                maxDiff = maxDiff(re, im, reSFHT, imSFHT);
                System.out.println("Maximal difference between FHT and Hartley transform,"
                    + " calculated via FFT: " + maxDiff + " = " + maxDiff / n + " * N");
                if (maxDiff >= 2.0 * Math.max(n, 1) && re instanceof PFloatingArray)
                    throw new AssertionError("Too big error for floating-point fourierToSeparableHartley");

                maxDiff = 0.0;
                for (long k = 0; k < n; k++) {
                    if (realOnly && imSFHT.getDouble(k) != 0.0)
                        throw new AssertionError("Non-zero imaginary part in real FHT");
                    double reH1 = reSFHT.getDouble(k);
                    double imH1 = imSFHT.getDouble(k);
                    double reH2 = reSFHT.getDouble(k == 0 ? 0 : n - k);
                    double imH2 = imSFHT.getDouble(k == 0 ? 0 : n - k);
                    double reF = 0.5 * ((reH1 + reH2) - (imH2 - imH1));
                    double imF = 0.5 * ((reH2 - reH1) + (imH1 + imH2));
                    // (h1+h2)/2 - i*(h1-h2)/2
                    maxDiff = Math.max(maxDiff, Math.abs(reFFT.getDouble(k) - reF));
                    maxDiff = Math.max(maxDiff, Math.abs(imFFT.getDouble(k) - imF));
                }
                System.out.println("Maximal difference between FFT and Fourier transform,"
                    + " calculated via FHT in test: " + maxDiff + " = " + maxDiff / n + " * N");
                if (maxDiff >= 2.0 * Math.max(n, 1) && re instanceof PFloatingArray)
                    throw new AssertionError("Too big error for Fourier transform, calculated via FHT");
                if (n <= MAX_CHECKED_LENGTH) {
                    re.copy(reOriginal2);
                    im.copy(imOriginal2);
                    sfht.directTransform(null, samples);
                    if (realOnly) {
                        sfht.spectrumOfConvolution(null,
                            Matrices.matrix(re, re.length()),
                            Matrices.matrix(re, re.length()),
                            Matrices.matrix(reSFHT, reSFHT.length()));
                    } else {
                        sfht.spectrumOfConvolution(null,
                            Matrices.matrix(re, re.length()),
                            Matrices.matrix(im, im.length()),
                            Matrices.matrix(re, re.length()),
                            Matrices.matrix(im, im.length()),
                            Matrices.matrix(reSFHT, reSFHT.length()),
                            Matrices.matrix(imSFHT, imSFHT.length()));
                    }
                    sfht.inverseTransform(null, samples);
                    discreteConvolution(reConvolution, imConvolution,
                        reOriginal, imOriginal, reOriginal2, imOriginal2);
                    System.out.printf("Convolution via SFHT: %s%n", samples.toString("%.3f", ", ", 500));
                    System.out.printf("Convolution:          %s%n", samplesConvolution.toString("%.3f", ", ", 500));
                    maxDiff = maxDiff(re, im, reConvolution, imConvolution);
                    System.out.println("Maximal difference from the simplest convolution (SFHT): " + maxDiff);
                    if (maxDiff >= Math.max(n, 1) && re instanceof PFloatingArray)
                        throw new AssertionError("Too big error for floating-point Fourier spectrum of convolution");
                }
            }
        }
    }
}
