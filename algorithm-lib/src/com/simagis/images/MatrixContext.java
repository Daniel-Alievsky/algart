package com.simagis.images;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.contexts.Context;

import java.io.IOException;

public interface MatrixContext extends Context {
    public MatrixND newMatrixND(Context allocationContext, Matrix<? extends PArray> m);

    /**
     * Equivalent to {@link #newMatrixND(Context, Matrix)} with the corresponding 1-dimensional matrix.
     */
    public Vector newVector(Context allocationContext, PArray a);

    public MatrixND copyMatrixND(Context context, MatrixND matrixND) throws IOException;

    public MatrixND openMatrixND(String path) throws IOException;

    public void shareMatrixND(MatrixND matrix, String path) throws IOException;
}
