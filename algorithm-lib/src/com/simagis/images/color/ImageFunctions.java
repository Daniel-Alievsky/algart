package com.simagis.images.color;

import net.algart.arrays.*;
import net.algart.math.functions.AbstractFunc;

import java.util.ArrayList;
import java.util.List;

public class ImageFunctions {
    public static List<Matrix<? extends PArray>> asCombinationWithRGBColor(
        List<? extends Matrix<? extends PArray>> rgbImage,
        Matrix<? extends BitArray> selector,
        final double opacity,
        double... rgbColor)
    {
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        for (int k = 0, bandCount = rgbImage.size(), n = Math.max(bandCount, 3); k < n; k++) {
            Matrix<? extends PArray> m = rgbImage.get(k < bandCount ? k : bandCount - 1);
            double scale = m.array().maxPossibleValue(1.0);
            final double filler = (k < rgbColor.length ? rgbColor[k] * scale : 0.0) * opacity;
            Class<? extends PArray> type = m.type(PArray.class);
            Matrix<PArray> combined = Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1]);
                    }

                    @Override
                    public double get(double x0, double x1) {
                        return x0 != 0.0 ? x1 * (1.0 - opacity) + filler : x1;
                    }

                },
                type, selector, m);
            result.add(combined);
        }
        return result;
    }

    public static List<Matrix<? extends PArray>> newCopy(
        MemoryModel memoryModel,
        ArrayContext progressContext,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b,
        boolean useRAMIfSmallEnough)
    {
        return newCopy(memoryModel, progressContext, Matrices.several(PArray.class, r, g, b), useRAMIfSmallEnough);
    }

    public static List<Matrix<? extends PArray>> newCopy(
        MemoryModel memoryModel,
        ArrayContext progressContext,
        List<? extends Matrix<? extends PArray>> rgbImage,
        boolean useRAMIfSmallEnough)
    {
        double memory = 0.0;
        for (Matrix<? extends PArray> m : rgbImage) {
            memory += Matrices.sizeOf(m);
        }
        if (memoryModel == null || (useRAMIfSmallEnough && memory <= Arrays.SystemSettings.maxTempJavaMemory())) {
            memoryModel = SimpleMemoryModel.getInstance();
        }
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        for (int k = 0, n = rgbImage.size(); k < n; k++) {
            Matrix<? extends PArray> m = rgbImage.get(k);
            Matrix<UpdatablePArray> copy = memoryModel.newMatrix(UpdatablePArray.class, m).structureLike(m);
            Matrices.copy(progressContext == null ? null : progressContext.part(k, k + 1, n), copy, m);
            result.add(copy);
        }
        return result;
    }
}
