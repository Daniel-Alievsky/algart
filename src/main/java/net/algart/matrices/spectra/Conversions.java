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
import net.algart.math.functions.Func;

import java.util.concurrent.atomic.AtomicLong;

class Conversions {
    private static final boolean OPTIMIZE_1D_FOURIER_CONVERSION = true;
    private static final boolean OPTIMIZE_2D_FOURIER_CONVERSION = true;
    private static final int BUF_CAP = 8192;

    static void separableHartleyToFourierRecoursive(
            final ArrayContext context, final long maxTempJavaMemory,
            final UpdatablePNumberArray fRe, final UpdatablePNumberArray fIm,
            final PNumberArray hRe, final PNumberArray hIm,
            long[] dimensions, int numberOfTasks) {
        assert fRe != null && fIm != null && hRe != null;
        final long n = dimensions[dimensions.length - 1];
        assert n >= 0;
        if (n == 0) {
            return; // avoiding starting access to the element #0
        }
        if (dimensions.length == 1) {
            assert n == fRe.length();
            if (OPTIMIZE_1D_FOURIER_CONVERSION && allFloat(fRe, fIm, hRe, hIm)) {
                separableHartleyToFourierFloatArray(context, fRe, fIm, hRe, hIm, n);
                return;
            }
            if (OPTIMIZE_1D_FOURIER_CONVERSION && allDouble(fRe, fIm, hRe, hIm)) {
                separableHartleyToFourierDoubleArray(context, fRe, fIm, hRe, hIm, n);
                return;
            }
            separableHartleyToFourierArray(context, fRe, fIm, hRe, hIm, n);
            return;
        }
        final long nDiv2 = n / 2;
        final long[] layerDims = JArrays.copyOfRange(dimensions, 0, dimensions.length - 1);
        final long layerLen = Arrays.longMul(layerDims);
        final long totalLen = layerLen * n;
        final double layerSize = layerLen * (double) (fRe.bitsPerElement() + fIm.bitsPerElement()) / 8.0;
        final long progressMask = layerLen >= 128 ? 0xFF : layerLen >= 16 ? 0xFFF : 0xFFFF;
        final boolean fDirect = AbstractSpectralTransform.areDirect(fRe, fIm);
        final boolean hDirect = AbstractSpectralTransform.areDirect(hRe, hIm);
        numberOfTasks = fDirect && hDirect ? (int) Math.min(numberOfTasks, nDiv2 + 1) : 1;
        final MemoryModel mm = context == null ? Arrays.SMM :
                layerSize * (hDirect ? 8.0 * numberOfTasks : hIm == null ? 10.0 : 12.0) <=
                        Math.max(maxTempJavaMemory, 0) ? Arrays.SMM :
                        // here numberOfTasks>1 only if fDirect && qDirect
                        context.getMemoryModel();
        final boolean fast2D = OPTIMIZE_2D_FOURIER_CONVERSION && dimensions.length == 2
                && ((fDirect && hDirect) || mm instanceof SimpleMemoryModel)
                && (allFloat(fRe, fIm, hRe, hIm) || allDouble(fRe, fIm, hRe, hIm));
        final UpdatablePNumberArray
                hRe1 = hDirect ? null : newArr(mm, hRe, layerLen),
                hIm1 = hDirect || hIm == null ? null : newArr(mm, hIm, layerLen),
                hRe2 = hDirect ? null : newArr(mm, hRe, layerLen),
                hIm2 = hDirect || hIm == null ? null : newArr(mm, hIm, layerLen);
        final Runnable[] tasks = new Runnable[numberOfTasks];
        final AtomicLong readyLayers = new AtomicLong(0);
        for (int threadIndex = 0; threadIndex < tasks.length; threadIndex++) {
            final UpdatablePNumberArray
                    wRe1 = fast2D && fDirect ? null : newArr(mm, fRe, layerLen),
                    wIm1 = fast2D && fDirect ? null : newArr(mm, fIm, layerLen),
                    wRe2 = fast2D && fDirect ? null : newArr(mm, fRe, layerLen),
                    wIm2 = fast2D && fDirect ? null : newArr(mm, fIm, layerLen),
                    sRe = fast2D ? null : newArr(mm, fRe, layerLen),
                    sIm = fast2D ? null : newArr(mm, fIm, layerLen),
                    dRe = fast2D ? null : newArr(mm, fRe, layerLen),
                    dIm = fast2D ? null : newArr(mm, fIm, layerLen);
            final int ti = threadIndex;
            tasks[ti] = () -> {
                final long layerStep = tasks.length * layerLen;
                for (long k1 = ti, disp1 = ti * layerLen; k1 <= nDiv2; k1 += tasks.length, disp1 += layerStep) {
                    long disp2 = k1 == 0 ? 0 : totalLen - disp1;
                    PNumberArray
                            hRe1Local = subArrOrCopy(hDirect ? null : hRe1, hRe, disp1, layerLen),
                            hIm1Local = subArrOrCopy(hDirect ? null : hIm1, hIm, disp1, layerLen),
                            hRe2Local = subArrOrCopy(hDirect ? null : hRe2, hRe, disp2, layerLen),
                            hIm2Local = subArrOrCopy(hDirect ? null : hIm2, hIm, disp2, layerLen);
                    if (fast2D) {
                        if (fDirect) {
                            separableHartleyToFourierDirect2D(
                                    fRe.subArr(disp1, layerLen), fIm.subArr(disp1, layerLen),
                                    fRe.subArr(disp2, layerLen), fIm.subArr(disp2, layerLen),
                                    hRe1Local, hIm1Local, hRe2Local, hIm2Local, fRe.elementType());
                        } else {
                            separableHartleyToFourierDirect2D(wRe1, wIm1, wRe2, wIm2,
                                    hRe1Local, hIm1Local, hRe2Local, hIm2Local, fRe.elementType());
                            fRe.subArr(disp1, layerLen).copy(wRe1);
                            fIm.subArr(disp1, layerLen).copy(wIm1);
                            fRe.subArr(disp2, layerLen).copy(wRe2);
                            fIm.subArr(disp2, layerLen).copy(wIm2);
                        }
                    } else {
                        separableHartleyToFourierRecoursive(null, maxTempJavaMemory,
                                wRe1, wIm1, hRe1Local, hIm1Local, layerDims, 1);
                        separableHartleyToFourierRecoursive(null, maxTempJavaMemory,
                                wRe2, wIm2, hRe2Local, hIm2Local, layerDims, 1);
                        // Below we calculate
                        //     f1 = (w1+w2)/2 - i * (w1-w2)/2 = s - i * d = (sRe+dIm, sIm-dRe)
                        //     f2 = (w1+w2)/2 + i * (w1-w2)/2 = s + i * d = (sRe-dIm, sIm+dRe)
                        Arrays.applyFunc(null, false, 1, true, Func.HALF_X_PLUS_Y, sRe, wRe1, wRe2);
                        Arrays.applyFunc(null, false, 1, true, Func.HALF_X_PLUS_Y, sIm, wIm1, wIm2);
                        Arrays.applyFunc(null, false, 1, true, Func.HALF_X_MINUS_Y, dRe, wRe1, wRe2);
                        Arrays.applyFunc(null, false, 1, true, Func.HALF_X_MINUS_Y, dIm, wIm1, wIm2);
                        UpdatablePNumberArray fRe1 = (UpdatablePNumberArray) fRe.subArr(disp1, layerLen);
                        UpdatablePNumberArray fIm1 = (UpdatablePNumberArray) fIm.subArr(disp1, layerLen);
                        UpdatablePNumberArray fRe2 = (UpdatablePNumberArray) fRe.subArr(disp2, layerLen);
                        UpdatablePNumberArray fIm2 = (UpdatablePNumberArray) fIm.subArr(disp2, layerLen);
                        Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, fRe1, sRe, dIm);
                        Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, fIm1, sIm, dRe);
                        Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y, fRe2, sRe, dIm);
                        Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y, fIm2, sIm, dRe);
                    }
                    long rl = context == null ? 0 : readyLayers.getAndIncrement();
                    if (context != null && (rl & progressMask) == 0) {
                        context.checkInterruptionAndUpdateProgress(fRe.elementType(), rl + 1, nDiv2 + 1);
                    }
                }
            };
        }
        Arrays.getThreadPoolFactory(context).performTasks(tasks);
    }

    static void fourierToSeparableHartleyRecursive(
            final ArrayContext context, final long maxTempJavaMemory,
            final UpdatablePNumberArray hRe, final UpdatablePNumberArray hIm,
            final PNumberArray fRe, final PNumberArray fIm,
            long[] dimensions, int numberOfTasks) {
        assert fRe != null && fIm != null && hRe != null;
        final long n = dimensions[dimensions.length - 1];
        final long nDiv2 = n / 2;
        assert n >= 0;
        if (n == 0) {
            return; // avoiding starting access to the element #0
        }
        if (dimensions.length == 1) {
            assert n == fRe.length();
            if (OPTIMIZE_1D_FOURIER_CONVERSION && allFloat(fRe, fIm, hRe, hIm)) {
                fourierToSeparableHartleyFloatArray(context, hRe, hIm, fRe, fIm, n);
                return;
            }
            if (OPTIMIZE_1D_FOURIER_CONVERSION && allDouble(fRe, fIm, hRe, hIm)) {
                fourierToSeparableHartleyDoubleArray(context, hRe, hIm, fRe, fIm, n);
                return;
            }
            fourierToSeparableHartleyArray(context, hRe, hIm, fRe, fIm, n);
            return;
        }
        final long[] layerDims = JArrays.copyOfRange(dimensions, 0, dimensions.length - 1);
        final long layerLen = Arrays.longMul(layerDims);
        final long totalLen = layerLen * n;
        final double layerSize = layerLen * (double) (fRe.bitsPerElement() + fIm.bitsPerElement()) / 8.0;
        final long progressMask = layerLen >= 128 ? 0xFF : layerLen >= 16 ? 0xFFF : 0xFFFF;
        final boolean fDirect = AbstractSpectralTransform.areDirect(fRe, fIm);
        final boolean hDirect = AbstractSpectralTransform.areDirect(hRe, hIm);
        numberOfTasks = fDirect && hDirect ? (int) Math.min(numberOfTasks, nDiv2 + 1) : 1;
        final MemoryModel mm = context == null ? Arrays.SMM :
                layerSize * (fDirect ? (hIm == null ? 6.0 : 8.0) * numberOfTasks :
                        hIm == null ? 10.0 : 12.0) <= Math.max(maxTempJavaMemory, 0) ? Arrays.SMM :
                        // here numberOfTasks>1 only if fDirect && qDirect
                        context.getMemoryModel();
        final boolean fast2D = OPTIMIZE_2D_FOURIER_CONVERSION && dimensions.length == 2
                && ((fDirect && hDirect) || mm instanceof SimpleMemoryModel)
                && (allFloat(fRe, fIm, hRe, hIm) || allDouble(fRe, fIm, hRe, hIm));
        final UpdatablePNumberArray
                fRe1 = fDirect ? null : newArr(mm, fRe, layerLen),
                fIm1 = fDirect ? null : newArr(mm, fIm, layerLen),
                fRe2 = fDirect ? null : newArr(mm, fRe, layerLen),
                fIm2 = fDirect ? null : newArr(mm, fIm, layerLen);
        final Runnable[] tasks = new Runnable[numberOfTasks];
        final AtomicLong readyLayers = new AtomicLong(0);
        for (int threadIndex = 0; threadIndex < tasks.length; threadIndex++) {
            final UpdatablePNumberArray
                    wRe1 = fast2D && hDirect ? null : newArr(mm, hRe, layerLen),
                    wIm1 = fast2D && hDirect ? null : newArr(mm, hIm == null ? hRe : hIm, layerLen),
                    wRe2 = fast2D && hDirect ? null : newArr(mm, hRe, layerLen),
                    wIm2 = fast2D && hDirect ? null : newArr(mm, hIm == null ? hRe : hIm, layerLen),
                    sRe = fast2D ? null : newArr(mm, fRe, layerLen),
                    sIm = fast2D || hIm == null ? null : newArr(mm, fIm, layerLen),
                    dRe = fast2D || hIm == null ? null : newArr(mm, fRe, layerLen),
                    dIm = fast2D ? null : newArr(mm, fIm, layerLen);
            final int ti = threadIndex;
            tasks[ti] = () -> {
                final long layerStep = tasks.length * layerLen;
                for (long k1 = ti, disp1 = ti * layerLen; k1 <= nDiv2; k1 += tasks.length, disp1 += layerStep) {
                    long disp2 = k1 == 0 ? 0 : totalLen - disp1;
                    PNumberArray
                            fRe1Local = subArrOrCopy(fDirect ? null : fRe1, fRe, disp1, layerLen),
                            fIm1Local = subArrOrCopy(fDirect ? null : fIm1, fIm, disp1, layerLen),
                            fRe2Local = subArrOrCopy(fDirect ? null : fRe2, fRe, disp2, layerLen),
                            fIm2Local = subArrOrCopy(fDirect ? null : fIm2, fIm, disp2, layerLen);
                    if (fast2D) {
                        if (hDirect) {
                            fourierToSeparableHartleyDirect2D(
                                    hRe.subArr(disp1, layerLen), hIm == null ? null : hIm.subArr(disp1, layerLen),
                                    hRe.subArr(disp2, layerLen), hIm == null ? null : hIm.subArr(disp2, layerLen),
                                    fRe1Local, fIm1Local, fRe2Local, fIm2Local, fRe.elementType());
                        } else {
                            fourierToSeparableHartleyDirect2D(
                                    wRe1, hIm == null ? null : wIm1, wRe2, hIm == null ? null : wIm2,
                                    fRe1Local, fIm1Local, fRe2Local, fIm2Local, fRe.elementType());
                            hRe.subArr(disp1, layerLen).copy(wRe1);
                            hRe.subArr(disp2, layerLen).copy(wRe2);
                            if (hIm != null) {
                                hIm.subArr(disp1, layerLen).copy(wIm1);
                                hIm.subArr(disp2, layerLen).copy(wIm2);
                            }
                        }
                    } else {
                        fourierToSeparableHartleyRecursive(null, maxTempJavaMemory,
                                wRe1, wIm1, fRe1Local, fIm1Local, layerDims, 1);
                        fourierToSeparableHartleyRecursive(null, maxTempJavaMemory,
                                wRe2, wIm2, fRe2Local, fIm2Local, layerDims, 1);
                        // The order (lines or columns) is not important; we choose this order for the best speed.
                        // Below we calculate
                        //     h1 = (w1+w2)/2 + i * (w1-w2)/2 = s + i * d = (sRe-dIm, sIm+dRe)
                        //     h2 = (w1+w2)/2 - i * (w1-w2)/2 = s - i * d = (sRe+dIm, sIm-dRe)
                        Arrays.applyFunc(null, false, 1, true, Func.HALF_X_PLUS_Y, sRe, wRe1, wRe2);
                        if (hIm != null) {
                            Arrays.applyFunc(null, false, 1, true, Func.HALF_X_PLUS_Y, sIm, wIm1, wIm2);
                            Arrays.applyFunc(null, false, 1, true, Func.HALF_X_MINUS_Y, dRe, wRe1, wRe2);
                        }
                        Arrays.applyFunc(null, false, 1, true, Func.HALF_X_MINUS_Y, dIm, wIm1, wIm2);
                        Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y,
                                hRe.subArr(disp1, layerLen), sRe, dIm);
                        if (hIm != null) {
                            Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y,
                                    hIm.subArr(disp1, layerLen), sIm, dRe);
                        }
                        Arrays.applyFunc(null, false, 1, true, Func.X_PLUS_Y,
                                hRe.subArr(disp2, layerLen), sRe, dIm);
                        if (hIm != null) {
                            Arrays.applyFunc(null, false, 1, true, Func.X_MINUS_Y,
                                    hIm.subArr(disp2, layerLen), sIm, dRe);
                        }
                        long rl = context == null ? 0 : readyLayers.getAndIncrement();
                        if (context != null && (rl & progressMask) == 0) {
                            context.checkInterruptionAndUpdateProgress(fRe.elementType(), rl + 1, nDiv2 + 1);
                        }
                    }
                }
            };
        }
        Arrays.getThreadPoolFactory(context).performTasks(tasks);
    }

    static boolean allFloat(PArray... arrays) {
        for (PArray array : arrays) {
            if (!(array == null || array instanceof FloatArray)) {
                return false;
            }
        }
        return true;
    }

    static boolean allDouble(PArray... arrays) {
        for (PArray array : arrays) {
            if (!(array == null || array instanceof DoubleArray)) {
                return false;
            }
        }
        return true;
    }

    static UpdatablePNumberArray newArr(MemoryModel mm, PNumberArray example, long length) {
        return (UpdatablePNumberArray) mm.newUnresizableArray(example.elementType(), length);
    }

    static PNumberArray subArrOrCopy(UpdatablePNumberArray dest, PNumberArray src, long position, long count) {
        if (src == null) {
            return null;
        } else if (dest == null) {
            return (PNumberArray) src.subArr(position, count);
        } else {
            dest.copy(src.subArr(position, count));
            return dest;
        }
    }

    private static void separableHartleyToFourierArray(
            ArrayContext context,
            UpdatablePNumberArray fRe, UpdatablePNumberArray fIm,
            PNumberArray hRe, PNumberArray hIm,
            final long n) {
        fRe.setDouble(0, hRe.getDouble(0));
        fIm.setDouble(0, hIm == null ? 0.0 : hIm.getDouble(0));
        for (long k1 = 1, nDiv2 = n / 2; k1 <= nDiv2; k1++) {
            long k2 = n - k1;
            double hRe1 = hRe.getDouble(k1), hRe2 = hRe.getDouble(k2);
            if (hIm == null) {
                double s = 0.5 * (hRe1 + hRe2);
                double d = 0.5 * (hRe1 - hRe2);
                fRe.setDouble(k1, s);
                fIm.setDouble(k1, -d);
                fRe.setDouble(k2, s);
                fIm.setDouble(k2, d);
            } else {
                double hIm1 = hIm.getDouble(k1), hIm2 = hIm.getDouble(k2);
                double sRe = 0.5 * (hRe1 + hRe2);
                double sIm = 0.5 * (hIm1 + hIm2);
                double dRe = 0.5 * (hRe1 - hRe2);
                double dIm = 0.5 * (hIm1 - hIm2);
                fRe.setDouble(k1, sRe + dIm);
                fIm.setDouble(k1, sIm - dRe);
                fRe.setDouble(k2, sRe - dIm);
                fIm.setDouble(k2, sIm + dRe);
            }
            if (context != null && (k1 & 0xFFFF) == 0) {
                context.checkInterruptionAndUpdateProgress(fRe.elementType(), k1 + 1, nDiv2 + 1);
            }
        }
    }

    private static void separableHartleyToFourierDirect2D(
            UpdatablePArray fRe1, UpdatablePArray fIm1,
            UpdatablePArray fRe2, UpdatablePArray fIm2,
            PArray hRe1, PArray hIm1,
            PArray hRe2, PArray hIm2,
            Class<?> elementType) {
        if (elementType == float.class) {
            separableHartleyToFourierDirect2DFloat(
                    (DirectAccessible) fRe1, (DirectAccessible) fIm1, (DirectAccessible) fRe2, (DirectAccessible) fIm2,
                    (DirectAccessible) hRe1, (DirectAccessible) hIm1, (DirectAccessible) hRe2, (DirectAccessible) hIm2);
        } else if (elementType == double.class) {
            separableHartleyToFourierDirect2DDouble(
                    (DirectAccessible) fRe1, (DirectAccessible) fIm1, (DirectAccessible) fRe2, (DirectAccessible) fIm2,
                    (DirectAccessible) hRe1, (DirectAccessible) hIm1, (DirectAccessible) hRe2, (DirectAccessible) hIm2);
        } else {
            throw new AssertionError("Unsupported element type for 2D optimization");
        }
    }

    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\s*\(([^)]+)\) ==> $1;;
    //           \(double\)\s*([\w\-]+) ==> $1 ]]
    private static void separableHartleyToFourierFloatArray(
            ArrayContext context,
            UpdatablePNumberArray fRe, UpdatablePNumberArray fIm,
            PNumberArray hRe, PNumberArray hIm,
            final long n) {
        assert n > 0;
        fRe.setDouble(0, hRe.getDouble(0));
        fIm.setDouble(0, hIm == null ? 0.0 : hIm.getDouble(0));
        if (n == 1) {
            return;
        }
        DataFloatBuffer fReBuf1 = (DataFloatBuffer) fRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer fReBuf2 = (DataFloatBuffer) fRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer fImBuf1 = (DataFloatBuffer) fIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer fImBuf2 = (DataFloatBuffer) fIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer hReBuf1 = (DataFloatBuffer) hRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer hReBuf2 = (DataFloatBuffer) hRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer hImBuf1 = hIm == null ? null :
                (DataFloatBuffer) hIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer hImBuf2 = hIm == null ? null :
                (DataFloatBuffer) hIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "sh2f bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            fReBuf1.map(disp1, len);
            fReBuf2.map(disp2, len);
            fImBuf1.map(disp1, len);
            fImBuf2.map(disp2, len);
            hReBuf1.map(disp1, len);
            hReBuf2.map(disp2, len);
            if (hIm != null) {
                hImBuf1.map(disp1, len);
                hImBuf2.map(disp2, len);
            }
            float[] fReJA1 = fReBuf1.data();
            float[] fReJA2 = fReBuf2.data();
            float[] fImJA1 = fImBuf1.data();
            float[] fImJA2 = fImBuf2.data();
            float[] hReJA1 = hReBuf1.data();
            float[] hReJA2 = hReBuf2.data();
            float[] hImJA1 = hIm == null ? null : hImBuf1.data();
            float[] hImJA2 = hIm == null ? null : hImBuf2.data();
            int fReOfs1 = fReBuf1.from();
            int fReOfs2 = fReBuf2.from();
            int fImOfs1 = fImBuf1.from();
            int fImOfs2 = fImBuf2.from();
            int hReOfs1 = hReBuf1.from();
            int hReOfs2 = hReBuf2.from();
            int hImOfs1 = hIm == null ? 0 : hImBuf1.from();
            int hImOfs2 = hIm == null ? 0 : hImBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double hRe1 = hReJA1[hReOfs1 + k1], hRe2 = hReJA2[hReOfs2 + k2];
                if (hIm == null) {
                    double s = 0.5 * (hRe1 + hRe2);
                    double d = 0.5 * (hRe1 - hRe2);
                    fReJA1[fReOfs1 + k1] = (float) s;
                    fImJA1[fReOfs1 + k1] = (float) -d;
                    fReJA2[fReOfs2 + k2] = (float) s;
                    fImJA2[fReOfs2 + k2] = (float) d;
                } else {
                    double hIm1 = hImJA1[hImOfs1 + k1], hIm2 = hImJA2[hImOfs2 + k2];
                    double sRe = 0.5 * (hRe1 + hRe2);
                    double sIm = 0.5 * (hIm1 + hIm2);
                    double dRe = 0.5 * (hRe1 - hRe2);
                    double dIm = 0.5 * (hIm1 - hIm2);
                    fReJA1[fReOfs1 + k1] = (float) (sRe + dIm);
                    fImJA1[fImOfs1 + k1] = (float) (sIm - dRe);
                    fReJA2[fReOfs2 + k2] = (float) (sRe - dIm);
                    fImJA2[fImOfs2 + k2] = (float) (sIm + dRe);
                }
            }
            count += disp1 + len == disp2 + 1 ? 2L * len - 1 : 2L * len; //odd or even n-1
            fReBuf1.force();
            fImBuf1.force();
            fReBuf2.force();
            fImBuf2.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(fRe.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3L * len) { // no risk of overflow: len is never too big
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
                assert len > 0 : "sh2f bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "sh2f bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void separableHartleyToFourierDirect2DFloat(
            DirectAccessible fRe1, DirectAccessible fIm1,
            DirectAccessible fRe2, DirectAccessible fIm2,
            DirectAccessible hRe1, DirectAccessible hIm1,
            DirectAccessible hRe2, DirectAccessible hIm2) {
        // We shall process 4 elements if 2D matrix with indexes (j,i), (N-j,i), (j,M-i), (N-j,M-i),
        // where the line #i is stored in arrays fRe/Im1, hRe/Im1
        // and the line #(M-i) is stored in arrays fRe/Im2, hRe/Im2
        // Let h1=hRe/Im1[j], h'1=hRe/Im1[N-j], h2=hRe/Im2[j], h'2=hRe/Im2[N-j],
        // and we should find f1=fRe/Im1[j], f'1=fRe/Im1[N-j], f2=fRe/Im2[j], f'2=fRe/Im2[N-j].
        // Let
        //     b1  = (h1+h'1)/2 - i * (h1-h'1)/2,
        //     b'1 = (h1+h'1)/2 + i * (h1-h'1)/2,
        //     b2  = (h2+h'2)/2 - i * (h2-h'2)/2,
        //     b'2 = (h2+h'2)/2 + i * (h2-h'2)/2
        // results of Hartley->Fourier conversion for lines. Then
        //     f1  = (b1+b2)/2 - i * (b1-b2)/2 =
        //         = (h1+h'1+h2+h'2)/4 - i*(h1-h'1+h2-h'2)/4 - i * [(h1+h'1-h2-h'2])4 - i*(h1-h'1-h2+h'2)/4] =
        //         = (h'1+h2)/2 - i*(h1-h'2)/2,
        //     f'1 = (b'1+b'2)/2 - i * (b'1-b'2)/2 =
        //         = (h1+h'1+h2+h'2)/4 + i*(h1-h'1+h2-h'2)/4 - i * [(h1+h'1-h2-h'2])4 + i*(h1-h'1-h2+h'2)/4] =
        //         = (h1+h'2)/2 - i*(h'1-h2)/2,
        //     f2  = (b1+b2)/2 + i * (b1-b2)/2 =
        //         = (h1+h'1+h2+h'2)/4 - i*(h1-h'1+h2-h'2)/4 + i * [(h1+h'1-h2-h'2])4 - i*(h1-h'1-h2+h'2)/4] =
        //         = (h1+h'2)/2 - i*(h2-h'1)/2,
        //     f'2 = (b'1+b'2)/2 + i * (b'1-b'2)/2 =
        //         = (h1+h'1+h2+h'2)/4 + i*(h1-h'1+h2-h'2)/4 + i * [(h1+h'1-h2-h'2])4 + i*(h1-h'1-h2+h'2)/4] =
        //         = (h'1+h2)/2 - i*(h'2-h1)/2
        assert (hIm1 == null) == (hIm2 == null);
//        System.out.println("HtoF " + hIm1);
        float[] fReJA1 = (float[]) fRe1.javaArray();
        float[] fReJA2 = (float[]) fRe2.javaArray();
        float[] fImJA1 = (float[]) fIm1.javaArray();
        float[] fImJA2 = (float[]) fIm2.javaArray();
        float[] hReJA1 = (float[]) hRe1.javaArray();
        float[] hReJA2 = (float[]) hRe2.javaArray();
        float[] hImJA1 = hIm1 == null ? null : (float[]) hIm1.javaArray();
        float[] hImJA2 = hIm2 == null ? null : (float[]) hIm2.javaArray();
        int fReOfs1 = fRe1.javaArrayOffset();
        int fReOfs2 = fRe2.javaArrayOffset();
        int fImOfs1 = fIm1.javaArrayOffset();
        int fImOfs2 = fIm2.javaArrayOffset();
        int hReOfs1 = hRe1.javaArrayOffset();
        int hReOfs2 = hRe2.javaArrayOffset();
        int hImOfs1 = hIm1 == null ? 0 : hIm1.javaArrayOffset();
        int hImOfs2 = hIm2 == null ? 0 : hIm2.javaArrayOffset();
        int n = fRe1.javaArrayLength();
        assert fRe2.javaArrayLength() == n;
        assert fIm1.javaArrayLength() == n;
        assert fIm2.javaArrayLength() == n;
        assert hRe1.javaArrayLength() == n;
        assert hRe2.javaArrayLength() == n;
        assert hIm1 == null || hIm1.javaArrayLength() == n;
        assert hIm2 == null || hIm2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double hLRe1 = hReJA1[hReOfs1 + kL], hRRe1 = hReJA1[hReOfs1 + kR];
            double hLRe2 = hReJA2[hReOfs2 + kL], hRRe2 = hReJA2[hReOfs2 + kR];
            if (hIm1 == null) {
                double s12 = 0.5 * (hLRe1 + hRRe2);
                double s21 = 0.5 * (hLRe2 + hRRe1);
                double d12 = 0.5 * (hLRe1 - hRRe2);
                double d21 = 0.5 * (hLRe2 - hRRe1);
                fReJA1[fReOfs1 + kL] = (float) s21;
                fImJA1[fImOfs1 + kL] = (float) -d12;
                fReJA1[fReOfs1 + kR] = (float) s12;
                fImJA1[fImOfs1 + kR] = (float) d21;
                fReJA2[fReOfs2 + kL] = (float) s12;
                fImJA2[fImOfs2 + kL] = (float) -d21;
                fReJA2[fReOfs2 + kR] = (float) s21;
                fImJA2[fImOfs2 + kR] = (float) d12;
            } else {
                double hLIm1 = hImJA1[hImOfs1 + kL], hRIm1 = hImJA1[hImOfs1 + kR];
                double hLIm2 = hImJA2[hImOfs2 + kL], hRIm2 = hImJA2[hImOfs2 + kR];
                double sRe12 = 0.5 * (hLRe1 + hRRe2);
                double sIm12 = 0.5 * (hLIm1 + hRIm2);
                double sRe21 = 0.5 * (hLRe2 + hRRe1);
                double sIm21 = 0.5 * (hLIm2 + hRIm1);
                double dRe12 = 0.5 * (hLRe1 - hRRe2);
                double dIm12 = 0.5 * (hLIm1 - hRIm2);
                double dRe21 = 0.5 * (hLRe2 - hRRe1);
                double dIm21 = 0.5 * (hLIm2 - hRIm1);
                fReJA1[fReOfs1 + kL] = (float) (sRe21 + dIm12);
                fImJA1[fImOfs1 + kL] = (float) (sIm21 - dRe12);
                fReJA1[fReOfs1 + kR] = (float) (sRe12 - dIm21);
                fImJA1[fImOfs1 + kR] = (float) (sIm12 + dRe21);
                fReJA2[fReOfs2 + kL] = (float) (sRe12 + dIm21);
                fImJA2[fImOfs2 + kL] = (float) (sIm12 - dRe21);
                fReJA2[fReOfs2 + kR] = (float) (sRe21 - dIm12);
                fImJA2[fImOfs2 + kR] = (float) (sIm21 + dRe12);
            }
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void separableHartleyToFourierDoubleArray(
            ArrayContext context,
            UpdatablePNumberArray fRe, UpdatablePNumberArray fIm,
            PNumberArray hRe, PNumberArray hIm,
            final long n) {
        assert n > 0;
        fRe.setDouble(0, hRe.getDouble(0));
        fIm.setDouble(0, hIm == null ? 0.0 : hIm.getDouble(0));
        if (n == 1) {
            return;
        }
        DataDoubleBuffer fReBuf1 = (DataDoubleBuffer) fRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer fReBuf2 = (DataDoubleBuffer) fRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer fImBuf1 = (DataDoubleBuffer) fIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer fImBuf2 = (DataDoubleBuffer) fIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer hReBuf1 = (DataDoubleBuffer) hRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer hReBuf2 = (DataDoubleBuffer) hRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer hImBuf1 = hIm == null ? null :
                (DataDoubleBuffer) hIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer hImBuf2 = hIm == null ? null :
                (DataDoubleBuffer) hIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "sh2f bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            fReBuf1.map(disp1, len);
            fReBuf2.map(disp2, len);
            fImBuf1.map(disp1, len);
            fImBuf2.map(disp2, len);
            hReBuf1.map(disp1, len);
            hReBuf2.map(disp2, len);
            if (hIm != null) {
                hImBuf1.map(disp1, len);
                hImBuf2.map(disp2, len);
            }
            double[] fReJA1 = fReBuf1.data();
            double[] fReJA2 = fReBuf2.data();
            double[] fImJA1 = fImBuf1.data();
            double[] fImJA2 = fImBuf2.data();
            double[] hReJA1 = hReBuf1.data();
            double[] hReJA2 = hReBuf2.data();
            double[] hImJA1 = hIm == null ? null : hImBuf1.data();
            double[] hImJA2 = hIm == null ? null : hImBuf2.data();
            int fReOfs1 = fReBuf1.from();
            int fReOfs2 = fReBuf2.from();
            int fImOfs1 = fImBuf1.from();
            int fImOfs2 = fImBuf2.from();
            int hReOfs1 = hReBuf1.from();
            int hReOfs2 = hReBuf2.from();
            int hImOfs1 = hIm == null ? 0 : hImBuf1.from();
            int hImOfs2 = hIm == null ? 0 : hImBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double hRe1 = hReJA1[hReOfs1 + k1], hRe2 = hReJA2[hReOfs2 + k2];
                if (hIm == null) {
                    double s = 0.5 * (hRe1 + hRe2);
                    double d = 0.5 * (hRe1 - hRe2);
                    fReJA1[fReOfs1 + k1] = s;
                    fImJA1[fReOfs1 + k1] = -d;
                    fReJA2[fReOfs2 + k2] = s;
                    fImJA2[fReOfs2 + k2] = d;
                } else {
                    double hIm1 = hImJA1[hImOfs1 + k1], hIm2 = hImJA2[hImOfs2 + k2];
                    double sRe = 0.5 * (hRe1 + hRe2);
                    double sIm = 0.5 * (hIm1 + hIm2);
                    double dRe = 0.5 * (hRe1 - hRe2);
                    double dIm = 0.5 * (hIm1 - hIm2);
                    fReJA1[fReOfs1 + k1] = sRe + dIm;
                    fImJA1[fImOfs1 + k1] = sIm - dRe;
                    fReJA2[fReOfs2 + k2] = sRe - dIm;
                    fImJA2[fImOfs2 + k2] = sIm + dRe;
                }
            }
            count += disp1 + len == disp2 + 1 ? 2L * len - 1 : 2L * len; //odd or even n-1
            fReBuf1.force();
            fImBuf1.force();
            fReBuf2.force();
            fImBuf2.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(fRe.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3L * len) { // no risk of overflow: len is never too big
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
                assert len > 0 : "sh2f bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "sh2f bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void separableHartleyToFourierDirect2DDouble(
            DirectAccessible fRe1, DirectAccessible fIm1,
            DirectAccessible fRe2, DirectAccessible fIm2,
            DirectAccessible hRe1, DirectAccessible hIm1,
            DirectAccessible hRe2, DirectAccessible hIm2) {
        // We shall process 4 elements if 2D matrix with indexes (j,i), (N-j,i), (j,M-i), (N-j,M-i),
        // where the line #i is stored in arrays fRe/Im1, hRe/Im1
        // and the line #(M-i) is stored in arrays fRe/Im2, hRe/Im2
        // Let h1=hRe/Im1[j], h'1=hRe/Im1[N-j], h2=hRe/Im2[j], h'2=hRe/Im2[N-j],
        // and we should find f1=fRe/Im1[j], f'1=fRe/Im1[N-j], f2=fRe/Im2[j], f'2=fRe/Im2[N-j].
        // Let
        //     b1  = (h1+h'1)/2 - i * (h1-h'1)/2,
        //     b'1 = (h1+h'1)/2 + i * (h1-h'1)/2,
        //     b2  = (h2+h'2)/2 - i * (h2-h'2)/2,
        //     b'2 = (h2+h'2)/2 + i * (h2-h'2)/2
        // results of Hartley->Fourier conversion for lines. Then
        //     f1  = (b1+b2)/2 - i * (b1-b2)/2 =
        //         = (h1+h'1+h2+h'2)/4 - i*(h1-h'1+h2-h'2)/4 - i * [(h1+h'1-h2-h'2])4 - i*(h1-h'1-h2+h'2)/4] =
        //         = (h'1+h2)/2 - i*(h1-h'2)/2,
        //     f'1 = (b'1+b'2)/2 - i * (b'1-b'2)/2 =
        //         = (h1+h'1+h2+h'2)/4 + i*(h1-h'1+h2-h'2)/4 - i * [(h1+h'1-h2-h'2])4 + i*(h1-h'1-h2+h'2)/4] =
        //         = (h1+h'2)/2 - i*(h'1-h2)/2,
        //     f2  = (b1+b2)/2 + i * (b1-b2)/2 =
        //         = (h1+h'1+h2+h'2)/4 - i*(h1-h'1+h2-h'2)/4 + i * [(h1+h'1-h2-h'2])4 - i*(h1-h'1-h2+h'2)/4] =
        //         = (h1+h'2)/2 - i*(h2-h'1)/2,
        //     f'2 = (b'1+b'2)/2 + i * (b'1-b'2)/2 =
        //         = (h1+h'1+h2+h'2)/4 + i*(h1-h'1+h2-h'2)/4 + i * [(h1+h'1-h2-h'2])4 + i*(h1-h'1-h2+h'2)/4] =
        //         = (h'1+h2)/2 - i*(h'2-h1)/2
        assert (hIm1 == null) == (hIm2 == null);
//        System.out.println("HtoF " + hIm1);
        double[] fReJA1 = (double[]) fRe1.javaArray();
        double[] fReJA2 = (double[]) fRe2.javaArray();
        double[] fImJA1 = (double[]) fIm1.javaArray();
        double[] fImJA2 = (double[]) fIm2.javaArray();
        double[] hReJA1 = (double[]) hRe1.javaArray();
        double[] hReJA2 = (double[]) hRe2.javaArray();
        double[] hImJA1 = hIm1 == null ? null : (double[]) hIm1.javaArray();
        double[] hImJA2 = hIm2 == null ? null : (double[]) hIm2.javaArray();
        int fReOfs1 = fRe1.javaArrayOffset();
        int fReOfs2 = fRe2.javaArrayOffset();
        int fImOfs1 = fIm1.javaArrayOffset();
        int fImOfs2 = fIm2.javaArrayOffset();
        int hReOfs1 = hRe1.javaArrayOffset();
        int hReOfs2 = hRe2.javaArrayOffset();
        int hImOfs1 = hIm1 == null ? 0 : hIm1.javaArrayOffset();
        int hImOfs2 = hIm2 == null ? 0 : hIm2.javaArrayOffset();
        int n = fRe1.javaArrayLength();
        assert fRe2.javaArrayLength() == n;
        assert fIm1.javaArrayLength() == n;
        assert fIm2.javaArrayLength() == n;
        assert hRe1.javaArrayLength() == n;
        assert hRe2.javaArrayLength() == n;
        assert hIm1 == null || hIm1.javaArrayLength() == n;
        assert hIm2 == null || hIm2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double hLRe1 = hReJA1[hReOfs1 + kL], hRRe1 = hReJA1[hReOfs1 + kR];
            double hLRe2 = hReJA2[hReOfs2 + kL], hRRe2 = hReJA2[hReOfs2 + kR];
            if (hIm1 == null) {
                double s12 = 0.5 * (hLRe1 + hRRe2);
                double s21 = 0.5 * (hLRe2 + hRRe1);
                double d12 = 0.5 * (hLRe1 - hRRe2);
                double d21 = 0.5 * (hLRe2 - hRRe1);
                fReJA1[fReOfs1 + kL] = s21;
                fImJA1[fImOfs1 + kL] = -d12;
                fReJA1[fReOfs1 + kR] = s12;
                fImJA1[fImOfs1 + kR] = d21;
                fReJA2[fReOfs2 + kL] = s12;
                fImJA2[fImOfs2 + kL] = -d21;
                fReJA2[fReOfs2 + kR] = s21;
                fImJA2[fImOfs2 + kR] = d12;
            } else {
                double hLIm1 = hImJA1[hImOfs1 + kL], hRIm1 = hImJA1[hImOfs1 + kR];
                double hLIm2 = hImJA2[hImOfs2 + kL], hRIm2 = hImJA2[hImOfs2 + kR];
                double sRe12 = 0.5 * (hLRe1 + hRRe2);
                double sIm12 = 0.5 * (hLIm1 + hRIm2);
                double sRe21 = 0.5 * (hLRe2 + hRRe1);
                double sIm21 = 0.5 * (hLIm2 + hRIm1);
                double dRe12 = 0.5 * (hLRe1 - hRRe2);
                double dIm12 = 0.5 * (hLIm1 - hRIm2);
                double dRe21 = 0.5 * (hLRe2 - hRRe1);
                double dIm21 = 0.5 * (hLIm2 - hRIm1);
                fReJA1[fReOfs1 + kL] = sRe21 + dIm12;
                fImJA1[fImOfs1 + kL] = sIm21 - dRe12;
                fReJA1[fReOfs1 + kR] = sRe12 - dIm21;
                fImJA1[fImOfs1 + kR] = sIm12 + dRe21;
                fReJA2[fReOfs2 + kL] = sRe12 + dIm21;
                fImJA2[fImOfs2 + kL] = sIm12 - dRe21;
                fReJA2[fReOfs2 + kR] = sRe21 - dIm12;
                fImJA2[fImOfs2 + kR] = sIm21 + dRe12;
            }
        }
    }

    //[[Repeat.AutoGeneratedEnd]]


    private static void fourierToSeparableHartleyArray(
            ArrayContext context,
            UpdatablePNumberArray hRe, UpdatablePNumberArray hIm,
            PNumberArray fRe, PNumberArray fIm,
            final long n) {
        hRe.setDouble(0, fRe.getDouble(0));
        if (hIm != null) {
            hIm.setDouble(0, fIm.getDouble(0));
        }
        for (long k1 = 1, nDiv2 = n / 2; k1 <= nDiv2; k1++) {
            long k2 = n - k1;
            double fRe1 = fRe.getDouble(k1), fRe2 = fRe.getDouble(k2);
            double fIm1 = fIm.getDouble(k1), fIm2 = fIm.getDouble(k2);
            double sRe = 0.5 * (fRe1 + fRe2);
            double sIm = 0.5 * (fIm1 + fIm2);
            double dRe = 0.5 * (fRe1 - fRe2);
            double dIm = 0.5 * (fIm1 - fIm2);
            hRe.setDouble(k1, sRe - dIm);
            hRe.setDouble(k2, sRe + dIm);
            if (hIm != null) {
                hIm.setDouble(k1, sIm + dRe);
                hIm.setDouble(k2, sIm - dRe);
            }
            if (context != null && (k1 & 0xFFFF) == 0) {
                context.checkInterruptionAndUpdateProgress(fRe.elementType(), k1 + 1, nDiv2 + 1);
            }
        }
    }

    private static void fourierToSeparableHartleyDirect2D(
            UpdatablePArray hRe1, UpdatablePArray hIm1,
            UpdatablePArray hRe2, UpdatablePArray hIm2,
            PArray fRe1, PArray fIm1,
            PArray fRe2, PArray fIm2,
            Class<?> elementType) {
        if (elementType == float.class) {
            fourierToSeparableHartleyDirect2DFloat(
                    (DirectAccessible) hRe1, (DirectAccessible) hIm1, (DirectAccessible) hRe2, (DirectAccessible) hIm2,
                    (DirectAccessible) fRe1, (DirectAccessible) fIm1, (DirectAccessible) fRe2, (DirectAccessible) fIm2);
        } else if (elementType == double.class) {
            fourierToSeparableHartleyDirect2DDouble(
                    (DirectAccessible) hRe1, (DirectAccessible) hIm1, (DirectAccessible) hRe2, (DirectAccessible) hIm2,
                    (DirectAccessible) fRe1, (DirectAccessible) fIm1, (DirectAccessible) fRe2, (DirectAccessible) fIm2);
        } else {
            throw new AssertionError("Unsupported element type for 2D optimization");
        }
    }


    //[[Repeat() Float ==> Double;;
    //           float ==> double;;
    //           \(double\)\s*\(([^)]+)\) ==> $1;;
    //           \(double\)\s*([\w\-]+) ==> $1 ]]
    private static void fourierToSeparableHartleyFloatArray(
            ArrayContext context,
            UpdatablePNumberArray hRe, UpdatablePNumberArray hIm,
            PNumberArray fRe, PNumberArray fIm,
            final long n) {
        assert n > 0;
        hRe.setDouble(0, fRe.getDouble(0));
        if (hIm != null) {
            hIm.setDouble(0, fIm.getDouble(0));
        }
        if (n == 1) {
            return;
        }
        DataFloatBuffer hReBuf1 = (DataFloatBuffer) hRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer hReBuf2 = (DataFloatBuffer) hRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer hImBuf1 = hIm == null ? null :
                (DataFloatBuffer) hIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer hImBuf2 = hIm == null ? null :
                (DataFloatBuffer) hIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataFloatBuffer fReBuf1 = (DataFloatBuffer) fRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer fReBuf2 = (DataFloatBuffer) fRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer fImBuf1 = (DataFloatBuffer) fIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataFloatBuffer fImBuf2 = (DataFloatBuffer) fIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "f2sh bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            hReBuf1.map(disp1, len);
            hReBuf2.map(disp2, len);
            if (hIm != null) {
                hImBuf1.map(disp1, len);
                hImBuf2.map(disp2, len);
            }
            fReBuf1.map(disp1, len);
            fReBuf2.map(disp2, len);
            fImBuf1.map(disp1, len);
            fImBuf2.map(disp2, len);
            float[] hReJA1 = hReBuf1.data();
            float[] hReJA2 = hReBuf2.data();
            float[] hImJA1 = hIm == null ? null : hImBuf1.data();
            float[] hImJA2 = hIm == null ? null : hImBuf2.data();
            float[] fReJA1 = fReBuf1.data();
            float[] fReJA2 = fReBuf2.data();
            float[] fImJA1 = fImBuf1.data();
            float[] fImJA2 = fImBuf2.data();
            int hReOfs1 = hReBuf1.from();
            int hReOfs2 = hReBuf2.from();
            int hImOfs1 = hIm == null ? 0 : hImBuf1.from();
            int hImOfs2 = hIm == null ? 0 : hImBuf2.from();
            int fReOfs1 = fReBuf1.from();
            int fReOfs2 = fReBuf2.from();
            int fImOfs1 = fImBuf1.from();
            int fImOfs2 = fImBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double fRe1 = fReJA1[fReOfs1 + k1], fRe2 = fReJA2[fReOfs2 + k2];
                double fIm1 = fImJA1[fImOfs1 + k1], fIm2 = fImJA2[fImOfs2 + k2];
                double sRe = 0.5 * (fRe1 + fRe2);
                double sIm = 0.5 * (fIm1 + fIm2);
                double dRe = 0.5 * (fRe1 - fRe2);
                double dIm = 0.5 * (fIm1 - fIm2);
                hReJA1[hReOfs1 + k1] = (float) (sRe - dIm);
                hReJA2[hReOfs2 + k2] = (float) (sRe + dIm);
                if (hIm != null) {
                    hImJA1[hImOfs1 + k1] = (float) (sIm + dRe);
                    hImJA2[hImOfs2 + k2] = (float) (sIm - dRe);
                }
            }
            count += disp1 + len == disp2 + 1 ? 2L * len - 1 : 2L * len; //odd or even n-1
            hReBuf1.force();
            if (hIm != null) {
                hImBuf1.force();
            }
            hReBuf2.force();
            if (hIm != null) {
                hImBuf2.force();
            }
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(fRe.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3L * len) { // no risk of overflow: len is never too big
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
                assert len > 0 : "f2sh bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "f2sh bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void fourierToSeparableHartleyDirect2DFloat(
            DirectAccessible hRe1, DirectAccessible hIm1,
            DirectAccessible hRe2, DirectAccessible hIm2,
            DirectAccessible fRe1, DirectAccessible fIm1,
            DirectAccessible fRe2, DirectAccessible fIm2) {
        // We shall process 4 elements if 2D matrix with indexes (j,i), (N-j,i), (j,M-i), (N-j,M-i),
        // where the line #i is stored in arrays fRe/Im1, hRe/Im1
        // and the line #(M-i) is stored in arrays fRe/Im2, hRe/Im2
        // Let f1=fRe/Im1[j], f'1=fRe/Im1[N-j], f2=fRe/Im2[j], f'2=fRe/Im2[N-j],
        // and we should find h1=hRe/Im1[j], h'1=hRe/Im1[N-j], h2=hRe/Im2[j], h'2=hRe/Im2[N-j].
        // Let
        //     b1  = (f1+f'1)/2 + i * (f1-f'1)/2,
        //     b'1 = (f1+f'1)/2 - i * (f1-f'1)/2,
        //     b2  = (f2+f'2)/2 + i * (f2-f'2)/2,
        //     b'2 = (f2+f'2)/2 - i * (f2-f'2)/2
        // results of Fourier->Hartley conversion for lines. Then
        //     h1  = (b1+b2)/2 + i * (b1-b2)/2 =
        //         = (f1+f'1+f2+f'2)/4 + i*(f1-f'1+f2-f'2)/4 + i * [(f1+f'1-f2-f'2])4 + i*(f1-f'1-f2+f'2)/4] =
        //         = (f'1+f2)/2 + i*(f1-f'2)/2,
        //     h'1 = (b'1+b'2)/2 + i * (b'1-b'2)/2 =
        //         = (f1+f'1+f2+f'2)/4 - i*(f1-f'1+f2-f'2)/4 + i * [(f1+f'1-f2-f'2])4 - i*(f1-f'1-f2+f'2)/4] =
        //         = (f1+f'2)/2 + i*(f'1-f2)/2,
        //     h2  = (b1+b2)/2 - i * (b1-b2)/2 =
        //         = (f1+f'1+f2+f'2)/4 + i*(f1-f'1+f2-f'2)/4 - i * [(f1+f'1-f2-f'2])4 + i*(f1-f'1-f2+f'2)/4] =
        //         = (f1+f'2)/2 + i*(f2-f'1)/2,
        //     h'2 = (b'1+b'2)/2 - i * (b'1-b'2)/2 =
        //         = (f1+f'1+f2+f'2)/4 - i*(f1-f'1+f2-f'2)/4 - i * [(f1+f'1-f2-f'2])4 - i*(f1-f'1-f2+f'2)/4] =
        //         = (f'1+f2)/2 + i*(f'2-f1)/2
        assert (hIm1 == null) == (hIm2 == null);
//        System.out.println("FtoH " + hIm1);
        float[] hReJA1 = (float[]) hRe1.javaArray();
        float[] hReJA2 = (float[]) hRe2.javaArray();
        float[] hImJA1 = hIm1 == null ? null : (float[]) hIm1.javaArray();
        float[] hImJA2 = hIm2 == null ? null : (float[]) hIm2.javaArray();
        float[] fReJA1 = (float[]) fRe1.javaArray();
        float[] fReJA2 = (float[]) fRe2.javaArray();
        float[] fImJA1 = (float[]) fIm1.javaArray();
        float[] fImJA2 = (float[]) fIm2.javaArray();
        int hReOfs1 = hRe1.javaArrayOffset();
        int hReOfs2 = hRe2.javaArrayOffset();
        int hImOfs1 = hIm1 == null ? 0 : hIm1.javaArrayOffset();
        int hImOfs2 = hIm2 == null ? 0 : hIm2.javaArrayOffset();
        int fReOfs1 = fRe1.javaArrayOffset();
        int fReOfs2 = fRe2.javaArrayOffset();
        int fImOfs1 = fIm1.javaArrayOffset();
        int fImOfs2 = fIm2.javaArrayOffset();
        int n = fRe1.javaArrayLength();
        assert fRe2.javaArrayLength() == n;
        assert fIm1.javaArrayLength() == n;
        assert fIm2.javaArrayLength() == n;
        assert hRe1.javaArrayLength() == n;
        assert hRe2.javaArrayLength() == n;
        assert hIm1 == null || hIm1.javaArrayLength() == n;
        assert hIm2 == null || hIm2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double fLRe1 = fReJA1[fReOfs1 + kL], fRRe1 = fReJA1[fReOfs1 + kR];
            double fLRe2 = fReJA2[fReOfs2 + kL], fRRe2 = fReJA2[fReOfs2 + kR];
            double fLIm1 = fImJA1[fImOfs1 + kL], fRIm1 = fImJA1[fImOfs1 + kR];
            double fLIm2 = fImJA2[fImOfs2 + kL], fRIm2 = fImJA2[fImOfs2 + kR];
            double sRe12 = 0.5 * (fLRe1 + fRRe2);
            double sIm12 = 0.5 * (fLIm1 + fRIm2);
            double sRe21 = 0.5 * (fLRe2 + fRRe1);
            double sIm21 = 0.5 * (fLIm2 + fRIm1);
            double dRe12 = 0.5 * (fLRe1 - fRRe2);
            double dIm12 = 0.5 * (fLIm1 - fRIm2);
            double dRe21 = 0.5 * (fLRe2 - fRRe1);
            double dIm21 = 0.5 * (fLIm2 - fRIm1);
            hReJA1[hReOfs1 + kL] = (float) (sRe21 - dIm12);
            hReJA1[hReOfs1 + kR] = (float) (sRe12 + dIm21);
            hReJA2[hReOfs2 + kL] = (float) (sRe12 - dIm21);
            hReJA2[hReOfs2 + kR] = (float) (sRe21 + dIm12);
            if (hIm1 != null) {
                hImJA1[hImOfs1 + kL] = (float) (sIm21 + dRe12);
                hImJA1[hImOfs1 + kR] = (float) (sIm12 - dRe21);
                hImJA2[hImOfs2 + kL] = (float) (sIm12 + dRe21);
                hImJA2[hImOfs2 + kR] = (float) (sIm21 - dRe12);
            }
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static void fourierToSeparableHartleyDoubleArray(
            ArrayContext context,
            UpdatablePNumberArray hRe, UpdatablePNumberArray hIm,
            PNumberArray fRe, PNumberArray fIm,
            final long n) {
        assert n > 0;
        hRe.setDouble(0, fRe.getDouble(0));
        if (hIm != null) {
            hIm.setDouble(0, fIm.getDouble(0));
        }
        if (n == 1) {
            return;
        }
        DataDoubleBuffer hReBuf1 = (DataDoubleBuffer) hRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer hReBuf2 = (DataDoubleBuffer) hRe.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer hImBuf1 = hIm == null ? null :
                (DataDoubleBuffer) hIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer hImBuf2 = hIm == null ? null :
                (DataDoubleBuffer) hIm.buffer(DataBuffer.AccessMode.READ_WRITE, BUF_CAP);
        DataDoubleBuffer fReBuf1 = (DataDoubleBuffer) fRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer fReBuf2 = (DataDoubleBuffer) fRe.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer fImBuf1 = (DataDoubleBuffer) fIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        DataDoubleBuffer fImBuf2 = (DataDoubleBuffer) fIm.buffer(DataBuffer.AccessMode.READ, BUF_CAP);
        int len = (int) Math.min(BUF_CAP, (n + 1) / 2);
        long disp1 = 1;
        long disp2 = n - len;
        // there will be two regions: disp1..disp1+len-1 and disp2..disp2+len-1
        long count = 0;
        for (; ; ) {
            assert len >= 1;
            assert disp1 >= 1;
            assert disp1 + len <= ((n & 1) == 0 ? disp2 + 1 : disp2) :
                    "f2sh bug 1: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            assert disp2 + len <= n; // ergo, disp1 + len <= disp2 + len <= n
            hReBuf1.map(disp1, len);
            hReBuf2.map(disp2, len);
            if (hIm != null) {
                hImBuf1.map(disp1, len);
                hImBuf2.map(disp2, len);
            }
            fReBuf1.map(disp1, len);
            fReBuf2.map(disp2, len);
            fImBuf1.map(disp1, len);
            fImBuf2.map(disp2, len);
            double[] hReJA1 = hReBuf1.data();
            double[] hReJA2 = hReBuf2.data();
            double[] hImJA1 = hIm == null ? null : hImBuf1.data();
            double[] hImJA2 = hIm == null ? null : hImBuf2.data();
            double[] fReJA1 = fReBuf1.data();
            double[] fReJA2 = fReBuf2.data();
            double[] fImJA1 = fImBuf1.data();
            double[] fImJA2 = fImBuf2.data();
            int hReOfs1 = hReBuf1.from();
            int hReOfs2 = hReBuf2.from();
            int hImOfs1 = hIm == null ? 0 : hImBuf1.from();
            int hImOfs2 = hIm == null ? 0 : hImBuf2.from();
            int fReOfs1 = fReBuf1.from();
            int fReOfs2 = fReBuf2.from();
            int fImOfs1 = fImBuf1.from();
            int fImOfs2 = fImBuf2.from();
            for (int k1 = 0; k1 < len; k1++) {
                int k2 = len - 1 - k1;
                double fRe1 = fReJA1[fReOfs1 + k1], fRe2 = fReJA2[fReOfs2 + k2];
                double fIm1 = fImJA1[fImOfs1 + k1], fIm2 = fImJA2[fImOfs2 + k2];
                double sRe = 0.5 * (fRe1 + fRe2);
                double sIm = 0.5 * (fIm1 + fIm2);
                double dRe = 0.5 * (fRe1 - fRe2);
                double dIm = 0.5 * (fIm1 - fIm2);
                hReJA1[hReOfs1 + k1] = sRe - dIm;
                hReJA2[hReOfs2 + k2] = sRe + dIm;
                if (hIm != null) {
                    hImJA1[hImOfs1 + k1] = sIm + dRe;
                    hImJA2[hImOfs2 + k2] = sIm - dRe;
                }
            }
            count += disp1 + len == disp2 + 1 ? 2L * len - 1 : 2L * len; //odd or even n-1
            hReBuf1.force();
            if (hIm != null) {
                hImBuf1.force();
            }
            hReBuf2.force();
            if (hIm != null) {
                hImBuf2.force();
            }
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(fRe.elementType(), count, n);
            }
            if (disp1 + len >= disp2) {
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1;
                break;
            }
            if (disp2 - disp1 >= 3L * len) { // no risk of overflow: len is never too big
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
                assert len > 0 : "f2sh bug 2: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
                disp2 -= len;
                //        []
                //         []
                assert disp1 + len == disp2 || disp1 + len == disp2 + 1 :
                        "f2sh bug 3: disp1=" + disp1 + ", disp2=" + disp2 + ", len=" + len + ", n=" + n;
            }
        }
    }

    private static void fourierToSeparableHartleyDirect2DDouble(
            DirectAccessible hRe1, DirectAccessible hIm1,
            DirectAccessible hRe2, DirectAccessible hIm2,
            DirectAccessible fRe1, DirectAccessible fIm1,
            DirectAccessible fRe2, DirectAccessible fIm2) {
        // We shall process 4 elements if 2D matrix with indexes (j,i), (N-j,i), (j,M-i), (N-j,M-i),
        // where the line #i is stored in arrays fRe/Im1, hRe/Im1
        // and the line #(M-i) is stored in arrays fRe/Im2, hRe/Im2
        // Let f1=fRe/Im1[j], f'1=fRe/Im1[N-j], f2=fRe/Im2[j], f'2=fRe/Im2[N-j],
        // and we should find h1=hRe/Im1[j], h'1=hRe/Im1[N-j], h2=hRe/Im2[j], h'2=hRe/Im2[N-j].
        // Let
        //     b1  = (f1+f'1)/2 + i * (f1-f'1)/2,
        //     b'1 = (f1+f'1)/2 - i * (f1-f'1)/2,
        //     b2  = (f2+f'2)/2 + i * (f2-f'2)/2,
        //     b'2 = (f2+f'2)/2 - i * (f2-f'2)/2
        // results of Fourier->Hartley conversion for lines. Then
        //     h1  = (b1+b2)/2 + i * (b1-b2)/2 =
        //         = (f1+f'1+f2+f'2)/4 + i*(f1-f'1+f2-f'2)/4 + i * [(f1+f'1-f2-f'2])4 + i*(f1-f'1-f2+f'2)/4] =
        //         = (f'1+f2)/2 + i*(f1-f'2)/2,
        //     h'1 = (b'1+b'2)/2 + i * (b'1-b'2)/2 =
        //         = (f1+f'1+f2+f'2)/4 - i*(f1-f'1+f2-f'2)/4 + i * [(f1+f'1-f2-f'2])4 - i*(f1-f'1-f2+f'2)/4] =
        //         = (f1+f'2)/2 + i*(f'1-f2)/2,
        //     h2  = (b1+b2)/2 - i * (b1-b2)/2 =
        //         = (f1+f'1+f2+f'2)/4 + i*(f1-f'1+f2-f'2)/4 - i * [(f1+f'1-f2-f'2])4 + i*(f1-f'1-f2+f'2)/4] =
        //         = (f1+f'2)/2 + i*(f2-f'1)/2,
        //     h'2 = (b'1+b'2)/2 - i * (b'1-b'2)/2 =
        //         = (f1+f'1+f2+f'2)/4 - i*(f1-f'1+f2-f'2)/4 - i * [(f1+f'1-f2-f'2])4 - i*(f1-f'1-f2+f'2)/4] =
        //         = (f'1+f2)/2 + i*(f'2-f1)/2
        assert (hIm1 == null) == (hIm2 == null);
//        System.out.println("FtoH " + hIm1);
        double[] hReJA1 = (double[]) hRe1.javaArray();
        double[] hReJA2 = (double[]) hRe2.javaArray();
        double[] hImJA1 = hIm1 == null ? null : (double[]) hIm1.javaArray();
        double[] hImJA2 = hIm2 == null ? null : (double[]) hIm2.javaArray();
        double[] fReJA1 = (double[]) fRe1.javaArray();
        double[] fReJA2 = (double[]) fRe2.javaArray();
        double[] fImJA1 = (double[]) fIm1.javaArray();
        double[] fImJA2 = (double[]) fIm2.javaArray();
        int hReOfs1 = hRe1.javaArrayOffset();
        int hReOfs2 = hRe2.javaArrayOffset();
        int hImOfs1 = hIm1 == null ? 0 : hIm1.javaArrayOffset();
        int hImOfs2 = hIm2 == null ? 0 : hIm2.javaArrayOffset();
        int fReOfs1 = fRe1.javaArrayOffset();
        int fReOfs2 = fRe2.javaArrayOffset();
        int fImOfs1 = fIm1.javaArrayOffset();
        int fImOfs2 = fIm2.javaArrayOffset();
        int n = fRe1.javaArrayLength();
        assert fRe2.javaArrayLength() == n;
        assert fIm1.javaArrayLength() == n;
        assert fIm2.javaArrayLength() == n;
        assert hRe1.javaArrayLength() == n;
        assert hRe2.javaArrayLength() == n;
        assert hIm1 == null || hIm1.javaArrayLength() == n;
        assert hIm2 == null || hIm2.javaArrayLength() == n;
        for (int kL = 0, nDiv2 = n / 2; kL <= nDiv2; kL++) {
            int kR = kL == 0 ? 0 : n - kL;
            double fLRe1 = fReJA1[fReOfs1 + kL], fRRe1 = fReJA1[fReOfs1 + kR];
            double fLRe2 = fReJA2[fReOfs2 + kL], fRRe2 = fReJA2[fReOfs2 + kR];
            double fLIm1 = fImJA1[fImOfs1 + kL], fRIm1 = fImJA1[fImOfs1 + kR];
            double fLIm2 = fImJA2[fImOfs2 + kL], fRIm2 = fImJA2[fImOfs2 + kR];
            double sRe12 = 0.5 * (fLRe1 + fRRe2);
            double sIm12 = 0.5 * (fLIm1 + fRIm2);
            double sRe21 = 0.5 * (fLRe2 + fRRe1);
            double sIm21 = 0.5 * (fLIm2 + fRIm1);
            double dRe12 = 0.5 * (fLRe1 - fRRe2);
            double dIm12 = 0.5 * (fLIm1 - fRIm2);
            double dRe21 = 0.5 * (fLRe2 - fRRe1);
            double dIm21 = 0.5 * (fLIm2 - fRIm1);
            hReJA1[hReOfs1 + kL] = sRe21 - dIm12;
            hReJA1[hReOfs1 + kR] = sRe12 + dIm21;
            hReJA2[hReOfs2 + kL] = sRe12 - dIm21;
            hReJA2[hReOfs2 + kR] = sRe21 + dIm12;
            if (hIm1 != null) {
                hImJA1[hImOfs1 + kL] = sIm21 + dRe12;
                hImJA1[hImOfs1 + kR] = sIm12 - dRe21;
                hImJA2[hImOfs2 + kL] = sIm12 + dRe21;
                hImJA2[hImOfs2 + kR] = sIm21 - dRe12;
            }
        }
    }

    //[[Repeat.AutoGeneratedEnd]]
}
