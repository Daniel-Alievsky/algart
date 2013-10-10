package com.simagis.images.color;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

public class Image2DBands {
    private Image2DBands() {}

    public static int bandCount(Image2D image) {
        return image.isGrayscale() ? 1 : 3;
    }

    public static Matrix<? extends PArray> correspondingBand(Image2D image, int bandIndex, Image2D otherImage) {
        return correspondingBand(image, bandIndex, otherImage.isGrayscale() ? 1 : 3);
    }

    public static Matrix<? extends PArray> correspondingBand(Image2D image, int bandIndex, int bandCount) {
        if (bandCount == 1) {
            return image.i();
        }
        switch (bandIndex) {
            case 0:
                return image.r();
            case 1:
                return image.g();
            case 2:
                return image.b();
            default:
                return image.i();
        }
    }
}
