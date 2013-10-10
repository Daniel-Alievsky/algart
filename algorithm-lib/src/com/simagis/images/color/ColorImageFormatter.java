package com.simagis.images.color;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

import java.awt.image.BufferedImage;
import java.util.List;

public interface ColorImageFormatter {
    public BufferedImage toBufferedImage(List<? extends Matrix<? extends PArray>> image);

    public List<Matrix<? extends PArray>> toImage(BufferedImage bufferedImage);
}
