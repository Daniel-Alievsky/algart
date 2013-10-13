package com.simagis.images;

import net.algart.arrays.*;
import net.algart.contexts.AbstractContext;
import net.algart.contexts.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public class SimpleMatrixContext extends AbstractContext implements MatrixContext {
    public SimpleMatrixContext() { // public to be usable as a service
        super(false);
    }

    private static MatrixContext INSTANCE = new SimpleMatrixContext();

    public static MatrixContext getInstance() {
        return INSTANCE;
    }

    // See https://support.simagis.com/browse/AA-216 about necessity of allocationContext
    public MatrixND newMatrixND(Context allocationContext, final Matrix<? extends PArray> m) {
        if (allocationContext == null)
            throw new NullPointerException("Null allocationContext");
        if (m == null)
            throw new NullPointerException("Null m argument");
        return new MatrixND() {
            public Class<?> elementType() {
                return m.elementType();
            }

            public int dimCount() {
                return m.dimCount();
            }

            public long dim(int coordIndex) {
                return m.dim(coordIndex);
            }

            public Matrix<? extends PArray> m() {
                return m;
            }

            public void freeResources(ArrayContext context) {
                m.freeResources(context);
            }
        };
    }

    public Vector newVector(Context allocationContext, final PArray a) {
        if (allocationContext == null)
            throw new NullPointerException("Null allocationContext");
        if (a == null)
            throw new NullPointerException("Null a argument");
        return new Vector() {
            public Class<?> elementType() {
                return a.elementType();
            }

            public int dimCount() {
                return 1;
            }

            public long dim(int coordIndex) {
                return coordIndex == 0 ? a.length() : 1;
            }

            public Matrix<? extends PArray> m() {
                PArray array = a instanceof UpdatablePArray ? ((UpdatablePArray) a).asUnresizable() : a;
                return Matrices.matrix(array, array.length());
            }

            public void freeResources(ArrayContext context) {
                a.freeResources(context);
            }

            public long length() {
                return a.length();
            }

            public PArray a() {
                return a;
            }
        };
    }

    public MatrixND copyMatrixND(Context context, MatrixND matrixND) throws IOException {
        return matrixND.dimCount() == 1 ?
            newVector(context, matrixND.m().array()) :
            newMatrixND(context, matrixND.m());
    }

    public MatrixND openMatrixND(String path) throws IOException {
        File directory = new File(path);
        if (!directory.isDirectory())
            throw new FileNotFoundException("Matrix path not found or not a directory: " + path);
        MatrixInfo mi = readMatrixInfo(directory, "index");
        File matrixFile = new File(directory, mi.dimCount() == 1 ? "vector" : "matrix");
        try {
            return newMatrixND(this, LargeMemoryModel.getInstance(new StandardIODataFileModel()).asMatrix(
                matrixFile.getAbsoluteFile(), mi));
        } catch (IllegalInfoSyntaxException e) {
            IOException ex = new IOException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public void shareMatrixND(MatrixND matrix, String path) throws IOException {
        if (matrix == null)
            throw new NullPointerException("Null matrix");
        File matrixDir = new File(path);
        if (!matrixDir.mkdir()) {
            if (!matrixDir.isDirectory()) // i.e. if doesn't really exist
                throw new IOException("Cannot create matrix directory " + matrixDir);
            // Important note: we must attempt to create matrixDir BEFORE checking its existence;
            // in other case, some parallel threads can attempt to create this directory twice,
            // that will lead to illegal messages about "errors" while creation
        }
        final Matrix<? extends PArray> m = matrix.m();
        final PArray array = LargeMemoryModel.getRawArrayForSavingInFile(m);
        MatrixInfo mi = LargeMemoryModel.getMatrixInfoForSavingInFile(m, 0);
        File matrixFile = new File(matrixDir, m.dimCount() == 1 ? "vector" : "matrix");
        File indexFile = new File(matrixDir, "index");
        final LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(new StandardIODataFileModel(
            matrixFile, false));
        final UpdatablePArray dest = (UpdatablePArray) mm.newUnresizableArray(m.elementType(), array.length());
        LargeMemoryModel.setTemporary(dest, false);
        mi = mi.cloneWithOtherByteOrder(dest.byteOrder());
        final FileOutputStream outputStream = new FileOutputStream(indexFile);
        try {
            outputStream.write(mi.toBytes());
        } finally {
            outputStream.close();
        }
        // We cannot use this context for creating DefaultArrayContext: this implementation
        // does not support necessary features. So, we must pass null below.
        Arrays.copy(null, dest, array, 0, false);
        dest.freeResources(null); // actually saves possible cached data to the file
        array.freeResources(null); // necessary to avoid overflowing 2 GB limit in 32-bit JVM
    }

    private static MatrixInfo readMatrixInfo(File directory, String indexFileName) throws IOException {
        File indexFile = new File(directory, indexFileName);
        try {
            final ByteArray byteArray = LargeMemoryModel.getInstance(
                new StandardIODataFileModel(false, false)).asByteArray(
                indexFile, 0, LargeMemoryModel.ALL_FILE, ByteOrder.nativeOrder());
            try {
                byte[] ja = new byte[MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH];
                byteArray.getData(0, ja);
                return MatrixInfo.valueOf(ja);
            } finally {
                byteArray.freeResources(null);
            }
        } catch (IllegalInfoSyntaxException e) {
            IOException ex = new IOException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

}
