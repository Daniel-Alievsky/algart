package com.simagis.images.color;

import net.algart.arrays.ArrayContext;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

import java.util.List;

/**
 * User: av@smartimtech.com
 * Date: 08.09.2007
 * Time: 20:24:29
 */
public interface Image2D {
    boolean isGrayscale();

    long dimX();

    long dimY();

    Matrix<? extends PArray> r();

    Matrix<? extends PArray> g();

    Matrix<? extends PArray> b();

    Matrix<? extends PArray> i();

    /**
     * Returns list (r, g, b) for color image and list (i) for grayscale image.
     *
     * @return list (r, g, b) for color image and list (i) for grayscale image.
     */
    List<Matrix<? extends PArray>> rgbi();

    void freeResources(ArrayContext context);
}
