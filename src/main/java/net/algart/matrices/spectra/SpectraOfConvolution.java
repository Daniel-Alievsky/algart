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
import net.algart.math.functions.AbstractFunc;

import java.util.concurrent.atomic.AtomicLong;

class SpectraOfConvolution {
    private static final boolean OPTIMIZE_1D_FOURIER_SPECTRUM_OF_CONVOLUTION = true;
    private static final boolean OPTIMIZE_1D_HARTLEY_SPECTRUM_OF_CONVOLUTION = true;
    private static final boolean OPTIMIZE_2D_HARTLEY_SPECTRUM_OF_CONVOLUTION = true;
    private static final int BUF_CAP = 8192;

    private static final Func XY_MINUS_XY_PLUS_XY_MINUS_XY = new AbstractFunc() {
        public double get(double... x) {
            return x[0] * x[1] - x[2] * x[3] + x[4] * x[5] - x[6] * x[7];
        }
    };

    private static final Func XY_MINUS_XY_MINUS_XY_PLUS_XY = new AbstractFunc() {
        public double get(double... x) {
            return x[0] * x[1] - x[2] * x[3] - x[4] * x[5] + x[6] * x[7];
        }
    };

    private static final Func XY_PLUS_XY_PLUS_XY_PLUS_XY = new AbstractFunc() {
        public double get(double... x) {
            return x[0] * x[1] + x[2] * x[3] + x[4] * x[5] + x[6] * x[7];
        }
    };

    private static final Func XY_PLUS_XY_MINUS_XY_MINUS_XY = new AbstractFunc() {
        public double get(double... x) {
            return x[0] * x[1] + x[2] * x[3] - x[4] * x[5] - x[6] * x[7];
        }
    };

    static void fourierSpectrumOfConvolution(
            ArrayContext context,
            UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
            PNumberArray pRe, PNumberArray pIm, PNumberArray qRe, PNumberArray qIm) {
        if (OPTIMIZE_1D_FOURIER_SPECTRUM_OF_CONVOLUTION && Conversions.allFloat(cRe, cIm, pRe, pIm, qRe, qIm)) {
            fourierSpectrumOfConvolutionOfFloatArrays(context, cRe, cIm, pRe, pIm, qRe, qIm, cRe.length());
            return;
        }
        if (OPTIMIZE_1D_FOURIER_SPECTRUM_OF_CONVOLUTION && Conversions.allDouble(cRe, cIm, pRe, pIm, qRe, qIm)) {
            fourierSpectrumOfConvolutionOfDoubleArrays(context, cRe, cIm, pRe, pIm, qRe, qIm, cRe.length());
            return;
        }
        for (long k = 0, n = cRe.length(); k < n; k++) {
            double pReV = pRe.getDouble(k), pImV = pIm.getDouble(k);
            double qReV = qRe.getDouble(k), qImV = qIm.getDouble(k);
            cRe.setDouble(k, pReV * qReV - pImV * qImV);
            cIm.setDouble(k, pReV * qImV + pImV * qReV);
            if (context != null && (k & 0xFFFF) == 0xFFFF) {
                context.checkInterruptionAndUpdateProgress(cRe.elementType(), k, n);
            }
        }
    }

    static void separableHartleySpectrumOfConvolution(
            final ArrayContext context, final long maxTempJavaMemory,
            final UpdatablePNumberArray cRe, final UpdatablePNumberArray cIm,
            final PNumberArray pRe, final PNumberArray pIm, final PNumberArray qRe, final PNumberArray qIm,
            long[] dimensions, int numberOfTasks) {
        final long n = dimensions[dimensions.length - 1];
        assert n >= 0;
        if (n == 0) {
            return; // avoiding starting access to the element #0
        }
        assert (cIm == null) == (pIm == null);
        assert (cIm == null) == (qIm == null);
        if (dimensions.length == 1) {
            assert n == pRe.length();
            if (OPTIMIZE_1D_HARTLEY_SPECTRUM_OF_CONVOLUTION && Conversions.allFloat(cRe, cIm, pRe, pIm, qRe, qIm)) {
                if (cIm != null) {
                    separableHartleySpectrumOfConvolutionOfComplexFloatArrays(context,
                            cRe, cIm, pRe, pIm, qRe, qIm, n);
                } else {
                    separableHartleySpectrumOfConvolutionOfRealFloatArrays(context, cRe, pRe, qRe, n);
                }
                return;
            }
            if (OPTIMIZE_1D_HARTLEY_SPECTRUM_OF_CONVOLUTION && Conversions.allDouble(cRe, cIm, pRe, pIm, qRe, qIm)) {
                if (cIm != null) {
                    separableHartleySpectrumOfConvolutionOfComplexDoubleArrays(context,
                            cRe, cIm, pRe, pIm, qRe, qIm, n);
                } else {
                    separableHartleySpectrumOfConvolutionOfRealDoubleArrays(context, cRe, pRe, qRe, n);
                }
                return;
            }
            if (cIm != null) {
                separableHartleySpectrumOfConvolutionOfComplexArrays(context, cRe, cIm, pRe, pIm, qRe, qIm, n);
            } else {
                separableHartleySpectrumOfConvolutionOfRealArrays(context, cRe, pRe, qRe, n);
            }
            return;
        }
        final long nDiv2 = n / 2;
        final long[] layerDims = JArrays.copyOfRange(dimensions, 0, dimensions.length - 1);
        final long layerLen = Arrays.longMul(layerDims);
        final long totalLen = layerLen * n;
        final double layerSize = layerLen * (double) (cRe.bitsPerElement()
                + (cIm == null ? 0 : cIm.bitsPerElement())) / 8.0;
        final long progressMask = layerLen >= 128 ? 0xFF : layerLen >= 16 ? 0xFFF : 0xFFFF;
        final boolean cDirect = AbstractSpectralTransform.areDirect(cRe, cIm);
        final boolean pqDirect = AbstractSpectralTransform.areDirect(pRe, pIm)
                && AbstractSpectralTransform.areDirect(qRe, qIm);
        numberOfTasks = cDirect && pqDirect ? (int) Math.min(numberOfTasks, nDiv2 + 1) : 1;
        final MemoryModel mm = context == null ? Arrays.SMM :
                layerSize * (pqDirect ? 12.0 * numberOfTasks : cIm == null ? 16.0 : 20.0) <=
                        Math.max(maxTempJavaMemory, 0) ? Arrays.SMM :
                        // here numberOfTasks>1 only if cDirect && pqDirect
                        context.getMemoryModel();
        final boolean fast2D = OPTIMIZE_2D_HARTLEY_SPECTRUM_OF_CONVOLUTION && dimensions.length == 2
                && ((cDirect && pqDirect) || mm instanceof SimpleMemoryModel)
                && (Conversions.allFloat(cRe, cIm, pRe, pIm, qRe, qIm)
                || Conversions.allDouble(cRe, cIm, pRe, pIm, qRe, qIm));
        final UpdatablePNumberArray
                pRe1 = pqDirect ? null : Conversions.newArr(mm, pRe, layerLen),
                pIm1 = pqDirect || pIm == null ? null : Conversions.newArr(mm, pIm, layerLen),
                pRe2 = pqDirect ? null : Conversions.newArr(mm, pRe, layerLen),
                pIm2 = pqDirect || pIm == null ? null : Conversions.newArr(mm, pIm, layerLen),
                qRe1 = pqDirect ? null : Conversions.newArr(mm, qRe, layerLen),
                qIm1 = pqDirect || qIm == null ? null : Conversions.newArr(mm, qIm, layerLen),
                qRe2 = pqDirect ? null : Conversions.newArr(mm, qRe, layerLen),
                qIm2 = pqDirect || qIm == null ? null : Conversions.newArr(mm, qIm, layerLen);
        final Runnable[] tasks = new Runnable[numberOfTasks];
        final AtomicLong readyLayers = new AtomicLong(0);
        for (int threadIndex = 0; threadIndex < tasks.length; threadIndex++) {
            final UpdatablePNumberArray
                    wpRe1 = fast2D ? null : Conversions.newArr(mm, pRe, layerLen),
                    wpIm1 = fast2D ? null : Conversions.newArr(mm, pIm != null ? pIm : pRe, layerLen),
                    wpRe2 = fast2D ? null : Conversions.newArr(mm, pRe, layerLen),
                    wpIm2 = fast2D ? null : Conversions.newArr(mm, pIm != null ? pIm : pRe, layerLen),
                    wqRe1 = fast2D ? null : Conversions.newArr(mm, qRe, layerLen),
                    wqIm1 = fast2D ? null : Conversions.newArr(mm, qIm != null ? qIm : qRe, layerLen),
                    wqRe2 = fast2D ? null : Conversions.newArr(mm, qRe, layerLen),
                    wqIm2 = fast2D ? null : Conversions.newArr(mm, qIm != null ? qIm : qRe, layerLen),
                    wcRe1 = Conversions.newArr(mm, cRe, layerLen),
                    wcIm1 = Conversions.newArr(mm, cIm != null ? cIm : cRe, layerLen),
                    wcRe2 = Conversions.newArr(mm, cRe, layerLen),
                    wcIm2 = Conversions.newArr(mm, cIm != null ? cIm : cRe, layerLen),
                    sRe = fast2D ? null : Conversions.newArr(mm, cRe, layerLen),
                    sIm = fast2D ? null : Conversions.newArr(mm, cIm != null ? cIm : cRe, layerLen),
                    dRe = fast2D ? null : Conversions.newArr(mm, cRe, layerLen),
                    dIm = fast2D ? null : Conversions.newArr(mm, cIm != null ? cIm : cRe, layerLen);
            final int ti = threadIndex;
            tasks[ti] = new Runnable() {
                public void run() {
                    final long layerStep = tasks.length * layerLen;
                    for (long k1 = ti, disp1 = ti * layerLen; k1 <= nDiv2; k1 += tasks.length, disp1 += layerStep) {
                        long disp2 = k1 == 0 ? 0 : totalLen - disp1;
                        PNumberArray
                                pRe1Local = Conversions.subArrOrCopy(pqDirect ? null : pRe1, pRe, disp1, layerLen),
                                pIm1Local = Conversions.subArrOrCopy(pqDirect ? null : pIm1, pIm, disp1, layerLen),
                                pRe2Local = Conversions.subArrOrCopy(pqDirect ? null : pRe2, pRe, disp2, layerLen),
                                pIm2Local = Conversions.subArrOrCopy(pqDirect ? null : pIm2, pIm, disp2, layerLen),
                                qRe1Local = Conversions.subArrOrCopy(pqDirect ? null : qRe1, qRe, disp1, layerLen),
                                qIm1Local = Conversions.subArrOrCopy(pqDirect ? null : qIm1, qIm, disp1, layerLen),
                                qRe2Local = Conversions.subArrOrCopy(pqDirect ? null : qRe2, qRe, disp2, layerLen),
                                qIm2Local = Conversions.subArrOrCopy(pqDirect ? null : qIm2, qIm, disp2, layerLen);
                        if (fast2D) {
                            if (cDirect) {
                                if (cIm == null) {
                                    separableHartleySpectrumOfConvolutionOfDirectReal2D(
                                            cRe.subArr(disp1, layerLen), cRe.subArr(disp2, layerLen),
                                            pRe1Local, pRe2Local, qRe1Local, qRe2Local, cRe.elementType());
                                } else {
                                    separableHartleySpectrumOfConvolutionOfDirectComplex2D(
                                            cRe.subArr(disp1, layerLen), cIm.subArr(disp1, layerLen),
                                            cRe.subArr(disp2, layerLen), cIm.subArr(disp2, layerLen),
                                            pRe1Local, pIm1Local, pRe2Local, pIm2Local,
                                            qRe1Local, qIm1Local, qRe2Local, qIm2Local, cRe.elementType());
                                }
                            } else {
                                if (cIm == null) {
                                    separableHartleySpectrumOfConvolutionOfDirectReal2D(wcRe1, wcRe2,
                                            pRe1Local, pRe2Local, qRe1Local, qRe2Local, cRe.elementType());
                                } else {
                                    separableHartleySpectrumOfConvolutionOfDirectComplex2D(wcRe1, wcIm1, wcRe2, wcIm2,
                                            pRe1Local, pIm1Local, pRe2Local, pIm2Local,
                                            qRe1Local, qIm1Local, qRe2Local, qIm2Local, cRe.elementType());
                                }
                                cRe.subArr(disp1, layerLen).copy(wcRe1);
                                cRe.subArr(disp2, layerLen).copy(wcRe2);
                                if (cIm != null) {
                                    cIm.subArr(disp1, layerLen).copy(wcIm1);
                                    cIm.subArr(disp2, layerLen).copy(wcIm2);
                                }
                            }
                        } else {
                            Conversions.separableHartleyToFourierRecoursive(null, maxTempJavaMemory,
                                    wpRe1, wpIm1, pRe1Local, pIm1Local, layerDims, 1);
                            Conversions.separableHartleyToFourierRecoursive(null, maxTempJavaMemory,
                                    wpRe2, wpIm2, pRe2Local, pIm2Local, layerDims, 1);
                            Conversions.separableHartleyToFourierRecoursive(null, maxTempJavaMemory,
                                    wqRe1, wqIm1, qRe1Local, qIm1Local, layerDims, 1);
                            Conversions.separableHartleyToFourierRecoursive(null, maxTempJavaMemory,
                                    wqRe2, wqIm2, qRe2Local, qIm2Local, layerDims, 1);
                            // Below we calculate
                            //     c1 = wq1 * (wp1+wp2)/2 + wq2 * (wp1-wp2)/2 =
                            //        = (wqRe1*sRe - wqIm1*sIm + wqRe2*dRe - wqIm2*dIm,
                            //           wqRe1*sIm + wqIm1*sRe + wqRe2*dIm + wqIm2*dRe)
                            //     c2 = wq2 * (wp1+wp2)/2 - wq1 * (wp1-wp2)/2 =
                            //        = (wqRe2*sRe - wqIm2*sIm - wqRe1*dRe + wqIm1*dIm,
                            //           wqRe2*sIm + wqIm2*sRe - wqRe1*dIm - wqIm1*dRe)
                            Arrays.applyFunc(null, false, 1, true, Func.HALF_X_PLUS_Y, sRe, wpRe1, wpRe2);
                            Arrays.applyFunc(null, false, 1, true, Func.HALF_X_PLUS_Y, sIm, wpIm1, wpIm2);
                            Arrays.applyFunc(null, false, 1, true, Func.HALF_X_MINUS_Y, dRe, wpRe1, wpRe2);
                            Arrays.applyFunc(null, false, 1, true, Func.HALF_X_MINUS_Y, dIm, wpIm1, wpIm2);
                            Arrays.applyFunc(null, false, 1, true, XY_MINUS_XY_PLUS_XY_MINUS_XY,
                                    wcRe1, wqRe1, sRe, wqIm1, sIm, wqRe2, dRe, wqIm2, dIm);
                            Arrays.applyFunc(null, false, 1, true, XY_MINUS_XY_MINUS_XY_PLUS_XY,
                                    wcRe2, wqRe2, sRe, wqIm2, sIm, wqRe1, dRe, wqIm1, dIm);
                            Arrays.applyFunc(null, false, 1, true, XY_PLUS_XY_PLUS_XY_PLUS_XY,
                                    wcIm1, wqRe1, sIm, wqIm1, sRe, wqRe2, dIm, wqIm2, dRe);
                            Arrays.applyFunc(null, false, 1, true, XY_PLUS_XY_MINUS_XY_MINUS_XY,
                                    wcIm2, wqRe2, sIm, wqIm2, sRe, wqRe1, dIm, wqIm1, dRe);
                            UpdatablePNumberArray
                                    cRe1 = cDirect ? (UpdatablePNumberArray) cRe.subArr(disp1, layerLen) : wcRe1,
                                    cRe2 = cDirect ? (UpdatablePNumberArray) cRe.subArr(disp2, layerLen) : wcRe2,
                                    cIm1 = cIm == null ? null :
                                            cDirect ? (UpdatablePNumberArray) cIm.subArr(disp1, layerLen) : wcIm1,
                                    cIm2 = cIm == null ? null :
                                            cDirect ? (UpdatablePNumberArray) cIm.subArr(disp2, layerLen) : wcIm2;
                            Conversions.fourierToSeparableHartleyRecursive(null, maxTempJavaMemory,
                                    cRe1, cIm1, wcRe1, wcIm1, layerDims, 1);
                            Conversions.fourierToSeparableHartleyRecursive(null, maxTempJavaMemory,
                                    cRe2, cIm2, wcRe2, wcIm2, layerDims, 1);
                            if (!cDirect) {
                                cRe.subArr(disp1, layerLen).copy(wcRe1);
                                cRe.subArr(disp2, layerLen).copy(wcRe2);
                                if (cIm != null) {
                                    cIm.subArr(disp1, layerLen).copy(wcIm1);
                                    cIm.subArr(disp2, layerLen).copy(wcIm2);
                                }
                            }
                        }
                        long rl = context == null ? 0 : readyLayers.getAndIncrement();
                        if (context != null && (rl & progressMask) == 0) {
                            context.checkInterruptionAndUpdateProgress(cRe.elementType(), rl + 1, nDiv2 + 1);
                        }
                    }
                }
            };
        }
        Arrays.getThreadPoolFactory(context).performTasks(tasks);
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\s*\(([^)]+)\) ==> $1;;
    //           \(double\)\s*([\w\-]+) ==> $1 ]]
    private static void fourierSpectrumOfConvolutionOfFloatArrays(
            ArrayContext context,
            UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
            PNumberArray pRe, PNumberArray pIm,
            PNumberArray qRe, PNumberArray qIm,
            final long n) {
        DataFloatBuffer cReBuf = (DataFloatBuffer) cRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer cImBuf = (DataFloatBuffer) cIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer pReBuf = (DataFloatBuffer) pRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer pImBuf = (DataFloatBuffer) pIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qReBuf = (DataFloatBuffer) qRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qImBuf = (DataFloatBuffer) qIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        for (long disp = 0; disp < n; disp += BUF_CAP) {
            cReBuf.map(disp);
            cImBuf.map(disp);
            pReBuf.map(disp);
            pImBuf.map(disp);
            qReBuf.map(disp);
            qImBuf.map(disp);
            float[] cReJA = cReBuf.data();
            float[] cImJA = cImBuf.data();
            float[] pReJA = pReBuf.data();
            float[] pImJA = pImBuf.data();
            float[] qReJA = qReBuf.data();
            float[] qImJA = qImBuf.data();
            int cReOfs = cReBuf.from();
            int cImOfs = cImBuf.from();
            int pReOfs = pReBuf.from();
            int pImOfs = pImBuf.from();
            int qReOfs = qReBuf.from();
            int qImOfs = qImBuf.from();
            int len = (int) Math.min(n - disp, BUF_CAP);
            for (int k = 0; k < len; k++) {
                double pReV = pReJA[pReOfs + k], pImV = pImJA[pImOfs + k];
                double qReV = qReJA[qReOfs + k], qImV = qImJA[qImOfs + k];
                cReJA[cReOfs + k] = (float) (pReV * qReV - pImV * qImV);
                cImJA[cImOfs + k] = (float) (pReV * qImV + pImV * qReV);
            }
            cReBuf.force();
            cImBuf.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(cRe.elementType(), disp + len, n);
            }
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void fourierSpectrumOfConvolutionOfDoubleArrays(
            ArrayContext context,
            UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
            PNumberArray pRe, PNumberArray pIm,
            PNumberArray qRe, PNumberArray qIm,
            final long n) {
        DataDoubleBuffer cReBuf = (DataDoubleBuffer) cRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer cImBuf = (DataDoubleBuffer) cIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer pReBuf = (DataDoubleBuffer) pRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer pImBuf = (DataDoubleBuffer) pIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qReBuf = (DataDoubleBuffer) qRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qImBuf = (DataDoubleBuffer) qIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        for (long disp = 0; disp < n; disp += BUF_CAP) {
            cReBuf.map(disp);
            cImBuf.map(disp);
            pReBuf.map(disp);
            pImBuf.map(disp);
            qReBuf.map(disp);
            qImBuf.map(disp);
            double[] cReJA = cReBuf.data();
            double[] cImJA = cImBuf.data();
            double[] pReJA = pReBuf.data();
            double[] pImJA = pImBuf.data();
            double[] qReJA = qReBuf.data();
            double[] qImJA = qImBuf.data();
            int cReOfs = cReBuf.from();
            int cImOfs = cImBuf.from();
            int pReOfs = pReBuf.from();
            int pImOfs = pImBuf.from();
            int qReOfs = qReBuf.from();
            int qImOfs = qImBuf.from();
            int len = (int) Math.min(n - disp, BUF_CAP);
            for (int k = 0; k < len; k++) {
                double pReV = pReJA[pReOfs + k], pImV = pImJA[pImOfs + k];
                double qReV = qReJA[qReOfs + k], qImV = qImJA[qImOfs + k];
                cReJA[cReOfs + k] = pReV * qReV - pImV * qImV;
                cImJA[cImOfs + k] = pReV * qImV + pImV * qReV;
            }
            cReBuf.force();
            cImBuf.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(cRe.elementType(), disp + len, n);
            }
        }
    }

    //[[Repeat.AutoGeneratedEnd]]

    private static void separableHartleySpectrumOfConvolutionOfDirectComplex2D(
            UpdatablePArray cRe1, UpdatablePArray cIm1, UpdatablePArray cRe2, UpdatablePArray cIm2,
            PArray pRe1, PArray pIm1, PArray pRe2, PArray pIm2,
            PArray qRe1, PArray qIm1, PArray qRe2, PArray qIm2,
            Class<?> elementType) {
        if (elementType == float.class) {
            separableHartleySpectrumOfConvolutionOfDirectComplex2DFloat(
                    (DirectAccessible) cRe1, (DirectAccessible) cIm1, (DirectAccessible) cRe2, (DirectAccessible) cIm2,
                    (DirectAccessible) pRe1, (DirectAccessible) pIm1, (DirectAccessible) pRe2, (DirectAccessible) pIm2,
                    (DirectAccessible) qRe1, (DirectAccessible) qIm1, (DirectAccessible) qRe2, (DirectAccessible) qIm2);
        } else if (elementType == double.class) {
            separableHartleySpectrumOfConvolutionOfDirectComplex2DDouble(
                    (DirectAccessible) cRe1, (DirectAccessible) cIm1, (DirectAccessible) cRe2, (DirectAccessible) cIm2,
                    (DirectAccessible) pRe1, (DirectAccessible) pIm1, (DirectAccessible) pRe2, (DirectAccessible) pIm2,
                    (DirectAccessible) qRe1, (DirectAccessible) qIm1, (DirectAccessible) qRe2, (DirectAccessible) qIm2);
        } else {
            throw new AssertionError("Unsupported element type for 2D optimization");
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfDirectReal2D(
            UpdatablePArray c1, UpdatablePArray c2,
            PArray p1, PArray p2,
            PArray q1, PArray q2,
            Class<?> elementType) {
        if (elementType == float.class) {
            separableHartleySpectrumOfConvolutionOfDirectReal2DFloat(
                    (DirectAccessible) c1, (DirectAccessible) c2,
                    (DirectAccessible) p1, (DirectAccessible) p2,
                    (DirectAccessible) q1, (DirectAccessible) q2);
        } else if (elementType == double.class) {
            separableHartleySpectrumOfConvolutionOfDirectReal2DDouble(
                    (DirectAccessible) c1, (DirectAccessible) c2,
                    (DirectAccessible) p1, (DirectAccessible) p2,
                    (DirectAccessible) q1, (DirectAccessible) q2);
        } else {
            throw new AssertionError("Unsupported element type for 2D optimization");
        }
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\s*\(([^)]+)\) ==> $1;;
    //           \(double\)\s*([\w\-]+) ==> $1 ]]
    private static void separableHartleySpectrumOfConvolutionOfComplexFloatArrays(
            ArrayContext context,
            UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
            PNumberArray pRe, PNumberArray pIm,
            PNumberArray qRe, PNumberArray qIm,
            final long n) {
        assert n > 0;
        double pRe0 = pRe.getDouble(0);
        double qRe0 = qRe.getDouble(0);
        double pIm0 = pIm.getDouble(0);
        double qIm0 = qIm.getDouble(0);
        cRe.setDouble(0, pRe0 * qRe0 - pIm0 * qIm0);
        cIm.setDouble(0, pRe0 * qIm0 + pIm0 * qRe0);
        if (n == 1) {
            return;
        }
        DataFloatBuffer cReBuf1 = (DataFloatBuffer) cRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer cReBuf2 = (DataFloatBuffer) cRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer cImBuf1 = (DataFloatBuffer) cIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer cImBuf2 = (DataFloatBuffer) cIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer pReBuf1 = (DataFloatBuffer) pRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer pReBuf2 = (DataFloatBuffer) pRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer pImBuf1 = (DataFloatBuffer) pIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer pImBuf2 = (DataFloatBuffer) pIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qReBuf1 = (DataFloatBuffer) qRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qReBuf2 = (DataFloatBuffer) qRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qImBuf1 = (DataFloatBuffer) qIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qImBuf2 = (DataFloatBuffer) qIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "shscc bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            cReBuf1.map(disp1, len);
            cReBuf2.map(disp2, len);
            cImBuf1.map(disp1, len);
            cImBuf2.map(disp2, len);
            pReBuf1.map(disp1, len);
            pReBuf2.map(disp2, len);
            pImBuf1.map(disp1, len);
            pImBuf2.map(disp2, len);
            qReBuf1.map(disp1, len);
            qReBuf2.map(disp2, len);
            qImBuf1.map(disp1, len);
            qImBuf2.map(disp2, len);
            float[] cReJA1 = cReBuf1.data(), cReJA2 = cReBuf2.data();
            float[] cImJA1 = cImBuf1.data(), cImJA2 = cImBuf2.data();
            float[] pReJA1 = pReBuf1.data(), pReJA2 = pReBuf2.data();
            float[] pImJA1 = pImBuf1.data(), pImJA2 = pImBuf2.data();
            float[] qReJA1 = qReBuf1.data(), qReJA2 = qReBuf2.data();
            float[] qImJA1 = qImBuf1.data(), qImJA2 = qImBuf2.data();
            int cReOfs1 = cReBuf1.from(), cReOfs2 = cReBuf2.from();
            int cImOfs1 = cImBuf1.from(), cImOfs2 = cImBuf2.from();
            int pReOfs1 = pReBuf1.from(), pReOfs2 = pReBuf2.from();
            int pImOfs1 = pImBuf1.from(), pImOfs2 = pImBuf2.from();
            int qReOfs1 = qReBuf1.from(), qReOfs2 = qReBuf2.from();
            int qImOfs1 = qImBuf1.from(), qImOfs2 = qImBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double pRe1 = pReJA1[pReOfs1 + k1], pRe2 = pReJA2[pReOfs2 + k2];
                double pIm1 = pImJA1[pImOfs1 + k1], pIm2 = pImJA2[pImOfs2 + k2];
                double qRe1 = qReJA1[qReOfs1 + k1], qRe2 = qReJA2[qReOfs2 + k2];
                double qIm1 = qImJA1[qImOfs1 + k1], qIm2 = qImJA2[qImOfs2 + k2];
                double sRe = 0.5 * (pRe1 + pRe2);
                double sIm = 0.5 * (pIm1 + pIm2);
                double dRe = 0.5 * (pRe1 - pRe2);
                double dIm = 0.5 * (pIm1 - pIm2);
                cReJA1[cReOfs1 + k1] = (float) (sRe * qRe1 - sIm * qIm1 + dRe * qRe2 - dIm * qIm2);
                cImJA1[cImOfs1 + k1] = (float) (sRe * qIm1 + sIm * qRe1 + dRe * qIm2 + dIm * qRe2);
                cReJA2[cReOfs2 + k2] = (float) (sRe * qRe2 - sIm * qIm2 - dRe * qRe1 + dIm * qIm1);
                cImJA2[cImOfs2 + k2] = (float) (sRe * qIm2 + sIm * qRe2 - dRe * qIm1 - dIm * qRe1);
            }
            count += disp1 + len == disp2 + 1 ? 2 * len - 1 : 2 * len; //odd or even n-1
            cReBuf1.force();
            cImBuf1.force();
            cReBuf2.force();
            cImBuf2.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(cRe.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3 * len) { // no risk of overflow: len is never too big
                //.............................
                //  [    ]
                //                      [    ]
                disp1 += len;
                disp2 -= len;
                //        [    ]
                //                [    ]
            } else {
                //.................
                //  [    ]
                //           [    ]
                disp1 += len;
                len = (int) ((disp2 - disp1 + 1) / 2);
                assert len > 0 : "shscc bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "shscc bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfRealFloatArrays(
            ArrayContext context,
            UpdatablePNumberArray c, PNumberArray p, PNumberArray q, final long n) {
        assert n > 0;
        c.setDouble(0, p.getDouble(0) * q.getDouble(0));
        if (n == 1) {
            return;
        }
        DataFloatBuffer cBuf1 = (DataFloatBuffer) c.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer cBuf2 = (DataFloatBuffer) c.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer pBuf1 = (DataFloatBuffer) p.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer pBuf2 = (DataFloatBuffer) p.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qBuf1 = (DataFloatBuffer) q.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer qBuf2 = (DataFloatBuffer) q.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "shscr bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            cBuf1.map(disp1, len);
            cBuf2.map(disp2, len);
            pBuf1.map(disp1, len);
            pBuf2.map(disp2, len);
            qBuf1.map(disp1, len);
            qBuf2.map(disp2, len);
            float[] cJA1 = cBuf1.data(), cJA2 = cBuf2.data();
            float[] pJA1 = pBuf1.data(), pJA2 = pBuf2.data();
            float[] qJA1 = qBuf1.data(), qJA2 = qBuf2.data();
            int cOfs1 = cBuf1.from(), cOfs2 = cBuf2.from();
            int pOfs1 = pBuf1.from(), pOfs2 = pBuf2.from();
            int qOfs1 = qBuf1.from(), qOfs2 = qBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double p1 = pJA1[pOfs1 + k1], p2 = pJA2[pOfs2 + k2];
                double q1 = qJA1[qOfs1 + k1], q2 = qJA2[qOfs2 + k2];
                double s = 0.5 * (p1 + p2);
                double d = 0.5 * (p1 - p2);
                cJA1[cOfs1 + k1] = (float) (s * q1 + d * q2);
                cJA2[cOfs2 + k2] = (float) (s * q2 - d * q1);
            }
            count += disp1 + len == disp2 + 1 ? 2 * len - 1 : 2 * len; //odd or even n-1
            cBuf1.force();
            cBuf2.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(c.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3 * len) { // no risk of overflow: len is never too big
                //.............................
                //  [    ]
                //                      [    ]
                disp1 += len;
                disp2 -= len;
                //        [    ]
                //                [    ]
            } else {
                //.................
                //  [    ]
                //           [    ]
                disp1 += len;
                len = (int) ((disp2 - disp1 + 1) / 2);
                assert len > 0 : "shscr bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "shscr bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfDirectComplex2DFloat(
            DirectAccessible cRe1, DirectAccessible cIm1, DirectAccessible cRe2, DirectAccessible cIm2,
            DirectAccessible pRe1, DirectAccessible pIm1, DirectAccessible pRe2, DirectAccessible pIm2,
            DirectAccessible qRe1, DirectAccessible qIm1, DirectAccessible qRe2, DirectAccessible qIm2) {
        // We shall process 4 elements if 2D matrix with indexes (j,i), (N-j,i), (j,M-i), (N-j,M-i),
        // where the line #i is stored in arrays pRe/Im1, qRe/Im1, cRe/Im1,
        // and the line #(M-i) is stored in arrays pRe/Im2, qRe/Im2, cRe/Im2.
        // Let p1=pRe/Im1[j], p'1=pRe/Im1[N-j], p2=hRe/Im2[j], p'2=hRe/Im2[N-j],
        // q1=qRe/Im1[j], q'1=qRe/Im1[N-j], q2=qRe/Im2[j], q'2=qRe/Im2[N-j],
        // and we should find c1=cRe/Im1[j], c'1=cRe/Im1[N-j], c2=cRe/Im2[j], c'2=cRe/Im2[N-j].
        // Let
        //     fp1  = (p'1+p2)/2 - i*(p1-p'2)/2,
        //     fp'1 = (p1+p'2)/2 - i*(p'1-p2)/2,
        //     fp2  = (p1+p'2)/2 - i*(p2-p'1)/2,
        //     fp'2 = (p'1+p2)/2 - i*(p'2-p1)/2
        // are values of Fourier spectrum for these indexes, and similarly for q and c; we are finding
        //     c1 = (fc'1+fc2)/2 + i*(fc1-fc'2)/2
        // and so on for c'1, c2, c'2. We have:
        //     4*fp1*fq1   = (p'1+p2)(q'1+q2) - (p1-p'2)(q1-q'2) - i * [(p'1+p2)(q1-q'2) + (p1-p'2)(q'1+q2)]
        //     4*fp'1*fq'1 = (p1+p'2)(q1+q'2) - (p'1-p2)(q'1-q2) - i * [(p1+p'2)(q'1-q2) + (p'1-p2)(q1+q'2)]
        //     4*fp2*fq2   = (p'2+p1)(q'2+q1) - (p2-p'1)(q2-q'1) - i * [(p'2+p1)(q2-q'1) + (p2-p'1)(q'2+q1)]
        //     4*fp'2*fq'2 = (p2+p'1)(q2+q'1) - (p'2-p1)(q'2-q1) - i * [(p2+p'1)(q'2-q1) + (p'2-p1)(q2+q'1)]
        // Thus
        //     8*c1 = 4*(fp'1*fq'1 + fp2*fq2) + i*4*(fp1*fq1 - fp'2*fq'2) =
        //          = (p1+p'2)(q1+q'2) - (p'1-p2)(q'1-q2) - i * [(p1+p'2)(q'1-q2) + (p'1-p2)(q1+q'2)] +
        //          + (p'2+p1)(q'2+q1) - (p2-p'1)(q2-q'1) - i * [(p'2+p1)(q2-q'1) + (p2-p'1)(q'2+q1)] +
        //          + (p'1+p2)(q1-q'2) + (p1-p'2)(q'1+q2) - i * [-(p'1+p2)(q'1+q2) + (p1-p'2)(q1-q'2)] -
        //          - (p2+p'1)(q'2-q1) - (p'2-p1)(q2+q'1) - i *[(p2+p'1)(q2+q'1) - (p'2-p1)(q'2-q1)] =
        //          = 2*p1*q1+ 2*p1*q2 + 2*p1*q'1 + 2*p1*q'2 +
        //          + 2*p2*q1 - 2*p2*q2 + 2*p2*q'1 - 2*p2*q'2 +
        //          + 2*p'1*q1 + 2*p'1*q2 - 2*p'1*q'1 - 2*p'1*q'2 +
        //          + 2*p'2*q1 - 2*p'2*q2 - 2*p'2*q'1 + 2*p'2*q'2;
        // the coefficient for the imaginary unit is zero (it is obvious, because for real samples we should
        // get the real convolution). Therefore,
        //     4*c1  = (p1+p'2)(q1+q'2) + (p1-p'2)(q'1+q2) + (p1'+p2)(q1-q'2) - (p'1-p2)(q'1-q2)
        // By analogy we have
        //     4*c'1 = (p'1+p2)(q'1+q2) + (p'1-p2)(q1+q'2) + (p1+p'2)(q'1-q2) - (p1-p'2)(q1-q'2)
        //     4*c2  = (p'1+p2)(q'1+q2) - (p'1-p2)(q1+q'2) - (p1+p'2)(q'1-q2) - (p1-p'2)(q1-q'2)
        //     4*c'2 = (p1+p'2)(q1+q'2) - (p1-p'2)(q'1+q2) - (p'1+p2)(q1-q'2) - (p'1-p2)(q'1-q2)
        float[] pReJA1 = (float[]) pRe1.javaArray();
        float[] pReJA2 = (float[]) pRe2.javaArray();
        float[] pImJA1 = (float[]) pIm1.javaArray();
        float[] pImJA2 = (float[]) pIm2.javaArray();
        float[] qReJA1 = (float[]) qRe1.javaArray();
        float[] qReJA2 = (float[]) qRe2.javaArray();
        float[] qImJA1 = (float[]) qIm1.javaArray();
        float[] qImJA2 = (float[]) qIm2.javaArray();
        float[] cReJA1 = (float[]) cRe1.javaArray();
        float[] cReJA2 = (float[]) cRe2.javaArray();
        float[] cImJA1 = (float[]) cIm1.javaArray();
        float[] cImJA2 = (float[]) cIm2.javaArray();
        int pReOfs1 = pRe1.javaArrayOffset();
        int pReOfs2 = pRe2.javaArrayOffset();
        int pImOfs1 = pIm1.javaArrayOffset();
        int pImOfs2 = pIm2.javaArrayOffset();
        int qReOfs1 = qRe1.javaArrayOffset();
        int qReOfs2 = qRe2.javaArrayOffset();
        int qImOfs1 = qIm1.javaArrayOffset();
        int qImOfs2 = qIm2.javaArrayOffset();
        int cReOfs1 = cRe1.javaArrayOffset();
        int cReOfs2 = cRe2.javaArrayOffset();
        int cImOfs1 = cIm1.javaArrayOffset();
        int cImOfs2 = cIm2.javaArrayOffset();
        int n = pRe1.javaArrayLength();
        assert pRe2.javaArrayLength() == n;
        assert pIm1.javaArrayLength() == n;
        assert pIm2.javaArrayLength() == n;
        assert qRe1.javaArrayLength() == n;
        assert qRe2.javaArrayLength() == n;
        assert qIm1.javaArrayLength() == n;
        assert qIm2.javaArrayLength() == n;
        assert cRe1.javaArrayLength() == n;
        assert cRe2.javaArrayLength() == n;
        assert cIm1.javaArrayLength() == n;
        assert cIm2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double pLRe1 = pReJA1[pReOfs1 + kL], pRRe1 = pReJA1[pReOfs1 + kR];
            double pLRe2 = pReJA2[pReOfs2 + kL], pRRe2 = pReJA2[pReOfs2 + kR];
            double pLIm1 = pImJA1[pImOfs1 + kL], pRIm1 = pImJA1[pImOfs1 + kR];
            double pLIm2 = pImJA2[pImOfs2 + kL], pRIm2 = pImJA2[pImOfs2 + kR];
            double qLRe1 = qReJA1[qReOfs1 + kL], qRRe1 = qReJA1[qReOfs1 + kR];
            double qLRe2 = qReJA2[qReOfs2 + kL], qRRe2 = qReJA2[qReOfs2 + kR];
            double qLIm1 = qImJA1[qImOfs1 + kL], qRIm1 = qImJA1[qImOfs1 + kR];
            double qLIm2 = qImJA2[qImOfs2 + kL], qRIm2 = qImJA2[qImOfs2 + kR];
            double psLRRe = pLRe1 + pRRe2, qsLRRe = qLRe1 + qRRe2;
            double psRLRe = pRRe1 + pLRe2, qsRLRe = qRRe1 + qLRe2;
            double psLRIm = pLIm1 + pRIm2, qsLRIm = qLIm1 + qRIm2;
            double psRLIm = pRIm1 + pLIm2, qsRLIm = qRIm1 + qLIm2;
            double pdLRRe = pLRe1 - pRRe2, qdLRRe = qLRe1 - qRRe2;
            double pdRLRe = pRRe1 - pLRe2, qdRLRe = qRRe1 - qLRe2;
            double pdLRIm = pLIm1 - pRIm2, qdLRIm = qLIm1 - qRIm2;
            double pdRLIm = pRIm1 - pLIm2, qdRLIm = qRIm1 - qLIm2;
            cReJA1[cReOfs1 + kL] = (float) (0.25 * (psLRRe * qsLRRe - psLRIm * qsLRIm
                    + pdLRRe * qsRLRe - pdLRIm * qsRLIm
                    + psRLRe * qdLRRe - psRLIm * qdLRIm
                    - pdRLRe * qdRLRe + pdRLIm * qdRLIm));
            cImJA1[cImOfs1 + kL] = (float) (0.25 * (psLRRe * qsLRIm + psLRIm * qsLRRe
                    + pdLRRe * qsRLIm + pdLRIm * qsRLRe
                    + psRLRe * qdLRIm + psRLIm * qdLRRe
                    - pdRLRe * qdRLIm - pdRLIm * qdRLRe));
            cReJA1[cReOfs1 + kR] = (float) (0.25 * (psRLRe * qsRLRe - psRLIm * qsRLIm
                    + pdRLRe * qsLRRe - pdRLIm * qsLRIm
                    + psLRRe * qdRLRe - psLRIm * qdRLIm
                    - pdLRRe * qdLRRe + pdLRIm * qdLRIm));
            cImJA1[cImOfs1 + kR] = (float) (0.25 * (psRLRe * qsRLIm + psRLIm * qsRLRe
                    + pdRLRe * qsLRIm + pdRLIm * qsLRRe
                    + psLRRe * qdRLIm + psLRIm * qdRLRe
                    - pdLRRe * qdLRIm - pdLRIm * qdLRRe));
            cReJA2[cReOfs2 + kL] = (float) (0.25 * (psRLRe * qsRLRe - psRLIm * qsRLIm
                    - pdRLRe * qsLRRe + pdRLIm * qsLRIm
                    - psLRRe * qdRLRe + psLRIm * qdRLIm
                    - pdLRRe * qdLRRe + pdLRIm * qdLRIm));
            cImJA2[cImOfs2 + kL] = (float) (0.25 * (psRLRe * qsRLIm + psRLIm * qsRLRe
                    - pdRLRe * qsLRIm - pdRLIm * qsLRRe
                    - psLRRe * qdRLIm - psLRIm * qdRLRe
                    - pdLRRe * qdLRIm - pdLRIm * qdLRRe));
            cReJA2[cReOfs2 + kR] = (float) (0.25 * (psLRRe * qsLRRe - psLRIm * qsLRIm
                    - pdLRRe * qsRLRe + pdLRIm * qsRLIm
                    - psRLRe * qdLRRe + psRLIm * qdLRIm
                    - pdRLRe * qdRLRe + pdRLIm * qdRLIm));
            cImJA2[cImOfs2 + kR] = (float) (0.25 * (psLRRe * qsLRIm + psLRIm * qsLRRe
                    - pdLRRe * qsRLIm - pdLRIm * qsRLRe
                    - psRLRe * qdLRIm - psRLIm * qdLRRe
                    - pdRLRe * qdRLIm - pdRLIm * qdRLRe));
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfDirectReal2DFloat(
            DirectAccessible c1, DirectAccessible c2,
            DirectAccessible p1, DirectAccessible p2,
            DirectAccessible q1, DirectAccessible q2) {
        float[] pJA1 = (float[]) p1.javaArray();
        float[] pJA2 = (float[]) p2.javaArray();
        float[] qJA1 = (float[]) q1.javaArray();
        float[] qJA2 = (float[]) q2.javaArray();
        float[] cJA1 = (float[]) c1.javaArray();
        float[] cJA2 = (float[]) c2.javaArray();
        int pOfs1 = p1.javaArrayOffset();
        int pOfs2 = p2.javaArrayOffset();
        int qOfs1 = q1.javaArrayOffset();
        int qOfs2 = q2.javaArrayOffset();
        int cOfs1 = c1.javaArrayOffset();
        int cOfs2 = c2.javaArrayOffset();
        int n = p1.javaArrayLength();
        assert p2.javaArrayLength() == n;
        assert q1.javaArrayLength() == n;
        assert q2.javaArrayLength() == n;
        assert c1.javaArrayLength() == n;
        assert c2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double pL1 = pJA1[pOfs1 + kL], pR1 = pJA1[pOfs1 + kR];
            double pL2 = pJA2[pOfs2 + kL], pR2 = pJA2[pOfs2 + kR];
            double qL1 = qJA1[qOfs1 + kL], qR1 = qJA1[qOfs1 + kR];
            double qL2 = qJA2[qOfs2 + kL], qR2 = qJA2[qOfs2 + kR];
            double psLR = pL1 + pR2, qsLR = qL1 + qR2;
            double psRL = pR1 + pL2, qsRL = qR1 + qL2;
            double pdLR = pL1 - pR2, qdLR = qL1 - qR2;
            double pdRL = pR1 - pL2, qdRL = qR1 - qL2;
            cJA1[cOfs1 + kL] = (float) (0.25 * (psLR * qsLR + pdLR * qsRL + psRL * qdLR - pdRL * qdRL));
            cJA1[cOfs1 + kR] = (float) (0.25 * (psRL * qsRL + pdRL * qsLR + psLR * qdRL - pdLR * qdLR));
            cJA2[cOfs2 + kL] = (float) (0.25 * (psRL * qsRL - pdRL * qsLR - psLR * qdRL - pdLR * qdLR));
            cJA2[cOfs2 + kR] = (float) (0.25 * (psLR * qsLR - pdLR * qsRL - psRL * qdLR - pdRL * qdRL));
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void separableHartleySpectrumOfConvolutionOfComplexDoubleArrays(
            ArrayContext context,
            UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
            PNumberArray pRe, PNumberArray pIm,
            PNumberArray qRe, PNumberArray qIm,
            final long n) {
        assert n > 0;
        double pRe0 = pRe.getDouble(0);
        double qRe0 = qRe.getDouble(0);
        double pIm0 = pIm.getDouble(0);
        double qIm0 = qIm.getDouble(0);
        cRe.setDouble(0, pRe0 * qRe0 - pIm0 * qIm0);
        cIm.setDouble(0, pRe0 * qIm0 + pIm0 * qRe0);
        if (n == 1) {
            return;
        }
        DataDoubleBuffer cReBuf1 = (DataDoubleBuffer) cRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer cReBuf2 = (DataDoubleBuffer) cRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer cImBuf1 = (DataDoubleBuffer) cIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer cImBuf2 = (DataDoubleBuffer) cIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer pReBuf1 = (DataDoubleBuffer) pRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer pReBuf2 = (DataDoubleBuffer) pRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer pImBuf1 = (DataDoubleBuffer) pIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer pImBuf2 = (DataDoubleBuffer) pIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qReBuf1 = (DataDoubleBuffer) qRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qReBuf2 = (DataDoubleBuffer) qRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qImBuf1 = (DataDoubleBuffer) qIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qImBuf2 = (DataDoubleBuffer) qIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "shscc bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            cReBuf1.map(disp1, len);
            cReBuf2.map(disp2, len);
            cImBuf1.map(disp1, len);
            cImBuf2.map(disp2, len);
            pReBuf1.map(disp1, len);
            pReBuf2.map(disp2, len);
            pImBuf1.map(disp1, len);
            pImBuf2.map(disp2, len);
            qReBuf1.map(disp1, len);
            qReBuf2.map(disp2, len);
            qImBuf1.map(disp1, len);
            qImBuf2.map(disp2, len);
            double[] cReJA1 = cReBuf1.data(), cReJA2 = cReBuf2.data();
            double[] cImJA1 = cImBuf1.data(), cImJA2 = cImBuf2.data();
            double[] pReJA1 = pReBuf1.data(), pReJA2 = pReBuf2.data();
            double[] pImJA1 = pImBuf1.data(), pImJA2 = pImBuf2.data();
            double[] qReJA1 = qReBuf1.data(), qReJA2 = qReBuf2.data();
            double[] qImJA1 = qImBuf1.data(), qImJA2 = qImBuf2.data();
            int cReOfs1 = cReBuf1.from(), cReOfs2 = cReBuf2.from();
            int cImOfs1 = cImBuf1.from(), cImOfs2 = cImBuf2.from();
            int pReOfs1 = pReBuf1.from(), pReOfs2 = pReBuf2.from();
            int pImOfs1 = pImBuf1.from(), pImOfs2 = pImBuf2.from();
            int qReOfs1 = qReBuf1.from(), qReOfs2 = qReBuf2.from();
            int qImOfs1 = qImBuf1.from(), qImOfs2 = qImBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double pRe1 = pReJA1[pReOfs1 + k1], pRe2 = pReJA2[pReOfs2 + k2];
                double pIm1 = pImJA1[pImOfs1 + k1], pIm2 = pImJA2[pImOfs2 + k2];
                double qRe1 = qReJA1[qReOfs1 + k1], qRe2 = qReJA2[qReOfs2 + k2];
                double qIm1 = qImJA1[qImOfs1 + k1], qIm2 = qImJA2[qImOfs2 + k2];
                double sRe = 0.5 * (pRe1 + pRe2);
                double sIm = 0.5 * (pIm1 + pIm2);
                double dRe = 0.5 * (pRe1 - pRe2);
                double dIm = 0.5 * (pIm1 - pIm2);
                cReJA1[cReOfs1 + k1] = sRe * qRe1 - sIm * qIm1 + dRe * qRe2 - dIm * qIm2;
                cImJA1[cImOfs1 + k1] = sRe * qIm1 + sIm * qRe1 + dRe * qIm2 + dIm * qRe2;
                cReJA2[cReOfs2 + k2] = sRe * qRe2 - sIm * qIm2 - dRe * qRe1 + dIm * qIm1;
                cImJA2[cImOfs2 + k2] = sRe * qIm2 + sIm * qRe2 - dRe * qIm1 - dIm * qRe1;
            }
            count += disp1 + len == disp2 + 1 ? 2 * len - 1 : 2 * len; //odd or even n-1
            cReBuf1.force();
            cImBuf1.force();
            cReBuf2.force();
            cImBuf2.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(cRe.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3 * len) { // no risk of overflow: len is never too big
                //.............................
                //  [    ]
                //                      [    ]
                disp1 += len;
                disp2 -= len;
                //        [    ]
                //                [    ]
            } else {
                //.................
                //  [    ]
                //           [    ]
                disp1 += len;
                len = (int) ((disp2 - disp1 + 1) / 2);
                assert len > 0 : "shscc bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "shscc bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfRealDoubleArrays(
            ArrayContext context,
            UpdatablePNumberArray c, PNumberArray p, PNumberArray q, final long n) {
        assert n > 0;
        c.setDouble(0, p.getDouble(0) * q.getDouble(0));
        if (n == 1) {
            return;
        }
        DataDoubleBuffer cBuf1 = (DataDoubleBuffer) c.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer cBuf2 = (DataDoubleBuffer) c.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer pBuf1 = (DataDoubleBuffer) p.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer pBuf2 = (DataDoubleBuffer) p.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qBuf1 = (DataDoubleBuffer) q.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer qBuf2 = (DataDoubleBuffer) q.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "shscr bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            cBuf1.map(disp1, len);
            cBuf2.map(disp2, len);
            pBuf1.map(disp1, len);
            pBuf2.map(disp2, len);
            qBuf1.map(disp1, len);
            qBuf2.map(disp2, len);
            double[] cJA1 = cBuf1.data(), cJA2 = cBuf2.data();
            double[] pJA1 = pBuf1.data(), pJA2 = pBuf2.data();
            double[] qJA1 = qBuf1.data(), qJA2 = qBuf2.data();
            int cOfs1 = cBuf1.from(), cOfs2 = cBuf2.from();
            int pOfs1 = pBuf1.from(), pOfs2 = pBuf2.from();
            int qOfs1 = qBuf1.from(), qOfs2 = qBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double p1 = pJA1[pOfs1 + k1], p2 = pJA2[pOfs2 + k2];
                double q1 = qJA1[qOfs1 + k1], q2 = qJA2[qOfs2 + k2];
                double s = 0.5 * (p1 + p2);
                double d = 0.5 * (p1 - p2);
                cJA1[cOfs1 + k1] = s * q1 + d * q2;
                cJA2[cOfs2 + k2] = s * q2 - d * q1;
            }
            count += disp1 + len == disp2 + 1 ? 2 * len - 1 : 2 * len; //odd or even n-1
            cBuf1.force();
            cBuf2.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(c.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3 * len) { // no risk of overflow: len is never too big
                //.............................
                //  [    ]
                //                      [    ]
                disp1 += len;
                disp2 -= len;
                //        [    ]
                //                [    ]
            } else {
                //.................
                //  [    ]
                //           [    ]
                disp1 += len;
                len = (int) ((disp2 - disp1 + 1) / 2);
                assert len > 0 : "shscr bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "shscr bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfDirectComplex2DDouble(
            DirectAccessible cRe1, DirectAccessible cIm1, DirectAccessible cRe2, DirectAccessible cIm2,
            DirectAccessible pRe1, DirectAccessible pIm1, DirectAccessible pRe2, DirectAccessible pIm2,
            DirectAccessible qRe1, DirectAccessible qIm1, DirectAccessible qRe2, DirectAccessible qIm2) {
        // We shall process 4 elements if 2D matrix with indexes (j,i), (N-j,i), (j,M-i), (N-j,M-i),
        // where the line #i is stored in arrays pRe/Im1, qRe/Im1, cRe/Im1,
        // and the line #(M-i) is stored in arrays pRe/Im2, qRe/Im2, cRe/Im2.
        // Let p1=pRe/Im1[j], p'1=pRe/Im1[N-j], p2=hRe/Im2[j], p'2=hRe/Im2[N-j],
        // q1=qRe/Im1[j], q'1=qRe/Im1[N-j], q2=qRe/Im2[j], q'2=qRe/Im2[N-j],
        // and we should find c1=cRe/Im1[j], c'1=cRe/Im1[N-j], c2=cRe/Im2[j], c'2=cRe/Im2[N-j].
        // Let
        //     fp1  = (p'1+p2)/2 - i*(p1-p'2)/2,
        //     fp'1 = (p1+p'2)/2 - i*(p'1-p2)/2,
        //     fp2  = (p1+p'2)/2 - i*(p2-p'1)/2,
        //     fp'2 = (p'1+p2)/2 - i*(p'2-p1)/2
        // are values of Fourier spectrum for these indexes, and similarly for q and c; we are finding
        //     c1 = (fc'1+fc2)/2 + i*(fc1-fc'2)/2
        // and so on for c'1, c2, c'2. We have:
        //     4*fp1*fq1   = (p'1+p2)(q'1+q2) - (p1-p'2)(q1-q'2) - i * [(p'1+p2)(q1-q'2) + (p1-p'2)(q'1+q2)]
        //     4*fp'1*fq'1 = (p1+p'2)(q1+q'2) - (p'1-p2)(q'1-q2) - i * [(p1+p'2)(q'1-q2) + (p'1-p2)(q1+q'2)]
        //     4*fp2*fq2   = (p'2+p1)(q'2+q1) - (p2-p'1)(q2-q'1) - i * [(p'2+p1)(q2-q'1) + (p2-p'1)(q'2+q1)]
        //     4*fp'2*fq'2 = (p2+p'1)(q2+q'1) - (p'2-p1)(q'2-q1) - i * [(p2+p'1)(q'2-q1) + (p'2-p1)(q2+q'1)]
        // Thus
        //     8*c1 = 4*(fp'1*fq'1 + fp2*fq2) + i*4*(fp1*fq1 - fp'2*fq'2) =
        //          = (p1+p'2)(q1+q'2) - (p'1-p2)(q'1-q2) - i * [(p1+p'2)(q'1-q2) + (p'1-p2)(q1+q'2)] +
        //          + (p'2+p1)(q'2+q1) - (p2-p'1)(q2-q'1) - i * [(p'2+p1)(q2-q'1) + (p2-p'1)(q'2+q1)] +
        //          + (p'1+p2)(q1-q'2) + (p1-p'2)(q'1+q2) - i * [-(p'1+p2)(q'1+q2) + (p1-p'2)(q1-q'2)] -
        //          - (p2+p'1)(q'2-q1) - (p'2-p1)(q2+q'1) - i *[(p2+p'1)(q2+q'1) - (p'2-p1)(q'2-q1)] =
        //          = 2*p1*q1+ 2*p1*q2 + 2*p1*q'1 + 2*p1*q'2 +
        //          + 2*p2*q1 - 2*p2*q2 + 2*p2*q'1 - 2*p2*q'2 +
        //          + 2*p'1*q1 + 2*p'1*q2 - 2*p'1*q'1 - 2*p'1*q'2 +
        //          + 2*p'2*q1 - 2*p'2*q2 - 2*p'2*q'1 + 2*p'2*q'2;
        // the coefficient for the imaginary unit is zero (it is obvious, because for real samples we should
        // get the real convolution). Therefore,
        //     4*c1  = (p1+p'2)(q1+q'2) + (p1-p'2)(q'1+q2) + (p1'+p2)(q1-q'2) - (p'1-p2)(q'1-q2)
        // By analogy we have
        //     4*c'1 = (p'1+p2)(q'1+q2) + (p'1-p2)(q1+q'2) + (p1+p'2)(q'1-q2) - (p1-p'2)(q1-q'2)
        //     4*c2  = (p'1+p2)(q'1+q2) - (p'1-p2)(q1+q'2) - (p1+p'2)(q'1-q2) - (p1-p'2)(q1-q'2)
        //     4*c'2 = (p1+p'2)(q1+q'2) - (p1-p'2)(q'1+q2) - (p'1+p2)(q1-q'2) - (p'1-p2)(q'1-q2)
        double[] pReJA1 = (double[]) pRe1.javaArray();
        double[] pReJA2 = (double[]) pRe2.javaArray();
        double[] pImJA1 = (double[]) pIm1.javaArray();
        double[] pImJA2 = (double[]) pIm2.javaArray();
        double[] qReJA1 = (double[]) qRe1.javaArray();
        double[] qReJA2 = (double[]) qRe2.javaArray();
        double[] qImJA1 = (double[]) qIm1.javaArray();
        double[] qImJA2 = (double[]) qIm2.javaArray();
        double[] cReJA1 = (double[]) cRe1.javaArray();
        double[] cReJA2 = (double[]) cRe2.javaArray();
        double[] cImJA1 = (double[]) cIm1.javaArray();
        double[] cImJA2 = (double[]) cIm2.javaArray();
        int pReOfs1 = pRe1.javaArrayOffset();
        int pReOfs2 = pRe2.javaArrayOffset();
        int pImOfs1 = pIm1.javaArrayOffset();
        int pImOfs2 = pIm2.javaArrayOffset();
        int qReOfs1 = qRe1.javaArrayOffset();
        int qReOfs2 = qRe2.javaArrayOffset();
        int qImOfs1 = qIm1.javaArrayOffset();
        int qImOfs2 = qIm2.javaArrayOffset();
        int cReOfs1 = cRe1.javaArrayOffset();
        int cReOfs2 = cRe2.javaArrayOffset();
        int cImOfs1 = cIm1.javaArrayOffset();
        int cImOfs2 = cIm2.javaArrayOffset();
        int n = pRe1.javaArrayLength();
        assert pRe2.javaArrayLength() == n;
        assert pIm1.javaArrayLength() == n;
        assert pIm2.javaArrayLength() == n;
        assert qRe1.javaArrayLength() == n;
        assert qRe2.javaArrayLength() == n;
        assert qIm1.javaArrayLength() == n;
        assert qIm2.javaArrayLength() == n;
        assert cRe1.javaArrayLength() == n;
        assert cRe2.javaArrayLength() == n;
        assert cIm1.javaArrayLength() == n;
        assert cIm2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double pLRe1 = pReJA1[pReOfs1 + kL], pRRe1 = pReJA1[pReOfs1 + kR];
            double pLRe2 = pReJA2[pReOfs2 + kL], pRRe2 = pReJA2[pReOfs2 + kR];
            double pLIm1 = pImJA1[pImOfs1 + kL], pRIm1 = pImJA1[pImOfs1 + kR];
            double pLIm2 = pImJA2[pImOfs2 + kL], pRIm2 = pImJA2[pImOfs2 + kR];
            double qLRe1 = qReJA1[qReOfs1 + kL], qRRe1 = qReJA1[qReOfs1 + kR];
            double qLRe2 = qReJA2[qReOfs2 + kL], qRRe2 = qReJA2[qReOfs2 + kR];
            double qLIm1 = qImJA1[qImOfs1 + kL], qRIm1 = qImJA1[qImOfs1 + kR];
            double qLIm2 = qImJA2[qImOfs2 + kL], qRIm2 = qImJA2[qImOfs2 + kR];
            double psLRRe = pLRe1 + pRRe2, qsLRRe = qLRe1 + qRRe2;
            double psRLRe = pRRe1 + pLRe2, qsRLRe = qRRe1 + qLRe2;
            double psLRIm = pLIm1 + pRIm2, qsLRIm = qLIm1 + qRIm2;
            double psRLIm = pRIm1 + pLIm2, qsRLIm = qRIm1 + qLIm2;
            double pdLRRe = pLRe1 - pRRe2, qdLRRe = qLRe1 - qRRe2;
            double pdRLRe = pRRe1 - pLRe2, qdRLRe = qRRe1 - qLRe2;
            double pdLRIm = pLIm1 - pRIm2, qdLRIm = qLIm1 - qRIm2;
            double pdRLIm = pRIm1 - pLIm2, qdRLIm = qRIm1 - qLIm2;
            cReJA1[cReOfs1 + kL] = 0.25 * (psLRRe * qsLRRe - psLRIm * qsLRIm
                    + pdLRRe * qsRLRe - pdLRIm * qsRLIm
                    + psRLRe * qdLRRe - psRLIm * qdLRIm
                    - pdRLRe * qdRLRe + pdRLIm * qdRLIm);
            cImJA1[cImOfs1 + kL] = 0.25 * (psLRRe * qsLRIm + psLRIm * qsLRRe
                    + pdLRRe * qsRLIm + pdLRIm * qsRLRe
                    + psRLRe * qdLRIm + psRLIm * qdLRRe
                    - pdRLRe * qdRLIm - pdRLIm * qdRLRe);
            cReJA1[cReOfs1 + kR] = 0.25 * (psRLRe * qsRLRe - psRLIm * qsRLIm
                    + pdRLRe * qsLRRe - pdRLIm * qsLRIm
                    + psLRRe * qdRLRe - psLRIm * qdRLIm
                    - pdLRRe * qdLRRe + pdLRIm * qdLRIm);
            cImJA1[cImOfs1 + kR] = 0.25 * (psRLRe * qsRLIm + psRLIm * qsRLRe
                    + pdRLRe * qsLRIm + pdRLIm * qsLRRe
                    + psLRRe * qdRLIm + psLRIm * qdRLRe
                    - pdLRRe * qdLRIm - pdLRIm * qdLRRe);
            cReJA2[cReOfs2 + kL] = 0.25 * (psRLRe * qsRLRe - psRLIm * qsRLIm
                    - pdRLRe * qsLRRe + pdRLIm * qsLRIm
                    - psLRRe * qdRLRe + psLRIm * qdRLIm
                    - pdLRRe * qdLRRe + pdLRIm * qdLRIm);
            cImJA2[cImOfs2 + kL] = 0.25 * (psRLRe * qsRLIm + psRLIm * qsRLRe
                    - pdRLRe * qsLRIm - pdRLIm * qsLRRe
                    - psLRRe * qdRLIm - psLRIm * qdRLRe
                    - pdLRRe * qdLRIm - pdLRIm * qdLRRe);
            cReJA2[cReOfs2 + kR] = 0.25 * (psLRRe * qsLRRe - psLRIm * qsLRIm
                    - pdLRRe * qsRLRe + pdLRIm * qsRLIm
                    - psRLRe * qdLRRe + psRLIm * qdLRIm
                    - pdRLRe * qdRLRe + pdRLIm * qdRLIm);
            cImJA2[cImOfs2 + kR] = 0.25 * (psLRRe * qsLRIm + psLRIm * qsLRRe
                    - pdLRRe * qsRLIm - pdLRIm * qsRLRe
                    - psRLRe * qdLRIm - psRLIm * qdLRRe
                    - pdRLRe * qdRLIm - pdRLIm * qdRLRe);
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfDirectReal2DDouble(
            DirectAccessible c1, DirectAccessible c2,
            DirectAccessible p1, DirectAccessible p2,
            DirectAccessible q1, DirectAccessible q2) {
        double[] pJA1 = (double[]) p1.javaArray();
        double[] pJA2 = (double[]) p2.javaArray();
        double[] qJA1 = (double[]) q1.javaArray();
        double[] qJA2 = (double[]) q2.javaArray();
        double[] cJA1 = (double[]) c1.javaArray();
        double[] cJA2 = (double[]) c2.javaArray();
        int pOfs1 = p1.javaArrayOffset();
        int pOfs2 = p2.javaArrayOffset();
        int qOfs1 = q1.javaArrayOffset();
        int qOfs2 = q2.javaArrayOffset();
        int cOfs1 = c1.javaArrayOffset();
        int cOfs2 = c2.javaArrayOffset();
        int n = p1.javaArrayLength();
        assert p2.javaArrayLength() == n;
        assert q1.javaArrayLength() == n;
        assert q2.javaArrayLength() == n;
        assert c1.javaArrayLength() == n;
        assert c2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double pL1 = pJA1[pOfs1 + kL], pR1 = pJA1[pOfs1 + kR];
            double pL2 = pJA2[pOfs2 + kL], pR2 = pJA2[pOfs2 + kR];
            double qL1 = qJA1[qOfs1 + kL], qR1 = qJA1[qOfs1 + kR];
            double qL2 = qJA2[qOfs2 + kL], qR2 = qJA2[qOfs2 + kR];
            double psLR = pL1 + pR2, qsLR = qL1 + qR2;
            double psRL = pR1 + pL2, qsRL = qR1 + qL2;
            double pdLR = pL1 - pR2, qdLR = qL1 - qR2;
            double pdRL = pR1 - pL2, qdRL = qR1 - qL2;
            cJA1[cOfs1 + kL] = 0.25 * (psLR * qsLR + pdLR * qsRL + psRL * qdLR - pdRL * qdRL);
            cJA1[cOfs1 + kR] = 0.25 * (psRL * qsRL + pdRL * qsLR + psLR * qdRL - pdLR * qdLR);
            cJA2[cOfs2 + kL] = 0.25 * (psRL * qsRL - pdRL * qsLR - psLR * qdRL - pdLR * qdLR);
            cJA2[cOfs2 + kR] = 0.25 * (psLR * qsLR - pdLR * qsRL - psRL * qdLR - pdRL * qdRL);
        }
    }

    //[[Repeat.AutoGeneratedEnd]]

    private static void separableHartleySpectrumOfConvolutionOfComplexArrays(
            ArrayContext context,
            UpdatablePNumberArray cRe, UpdatablePNumberArray cIm,
            PNumberArray pRe, PNumberArray pIm, PNumberArray qRe, PNumberArray qIm,
            final long n) {
        for (long k1 = 0, k2 = 0, nDiv2 = n / 2; k1 <= nDiv2; k1++, k2 = n - k1) { // k2 = 0,n-1,n-2,...
            double pRe1 = pRe.getDouble(k1), pRe2 = pRe.getDouble(k2);
            double pIm1 = pIm.getDouble(k1), pIm2 = pIm.getDouble(k2);
            double qRe1 = qRe.getDouble(k1), qRe2 = qRe.getDouble(k2);
            double qIm1 = qIm.getDouble(k1), qIm2 = qIm.getDouble(k2);
            double sRe = 0.5 * (pRe1 + pRe2);
            double sIm = 0.5 * (pIm1 + pIm2);
            double dRe = 0.5 * (pRe1 - pRe2);
            double dIm = 0.5 * (pIm1 - pIm2);
            cRe.setDouble(k1, sRe * qRe1 - sIm * qIm1 + dRe * qRe2 - dIm * qIm2);
            cIm.setDouble(k1, sRe * qIm1 + sIm * qRe1 + dRe * qIm2 + dIm * qRe2);
            cRe.setDouble(k2, sRe * qRe2 - sIm * qIm2 - dRe * qRe1 + dIm * qIm1);
            cIm.setDouble(k2, sRe * qIm2 + sIm * qRe2 - dRe * qIm1 - dIm * qRe1);
            if (context != null && (k1 & 0xFFFF) == 0) {
                context.checkInterruptionAndUpdateProgress(pRe.elementType(), k1 + 1, nDiv2 + 1);
            }
        }
    }

    private static void separableHartleySpectrumOfConvolutionOfRealArrays(
            ArrayContext context,
            UpdatablePNumberArray c, PNumberArray p, PNumberArray q, final long n) {
        for (long k1 = 0, k2 = 0, nDiv2 = n / 2; k1 <= nDiv2; k1++, k2 = n - k1) { // k2 = 0,n-1,n-2,...
            double p1 = p.getDouble(k1), p2 = p.getDouble(k2);
            double q1 = q.getDouble(k1), q2 = q.getDouble(k2);
            double s = 0.5 * (p1 + p2);
            double d = 0.5 * (p1 - p2);
            c.setDouble(k1, s * q1 + d * q2);
            c.setDouble(k2, s * q2 - d * q1);
            if (context != null && (k1 & 0xFFFF) == 0) {
                context.checkInterruptionAndUpdateProgress(p.elementType(), k1 + 1, nDiv2 + 1);
            }
        }
    }
}
