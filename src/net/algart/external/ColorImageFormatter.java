package net.algart.external;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

import java.awt.image.BufferedImage;
import java.util.List;

// Deprecated - should be replaced with new methods in Converters
interface ColorImageFormatter {
    public BufferedImage toBufferedImage(List<? extends Matrix<? extends PArray>> image);

    public List<Matrix<? extends PArray>> toImage(BufferedImage bufferedImage);
}
