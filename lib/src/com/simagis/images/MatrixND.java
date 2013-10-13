package com.simagis.images;

import net.algart.arrays.ArrayContext;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

public interface MatrixND {
    public Class<?> elementType();

    public int dimCount();

    public long dim(int coordIndex);

    public Matrix<? extends PArray> m();

    public void freeResources(ArrayContext context);
}
