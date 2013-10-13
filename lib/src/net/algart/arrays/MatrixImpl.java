package net.algart.arrays;

class MatrixImpl<T extends Array> extends AbstractMatrix<T> implements Matrix<T> {
    private final T array;
    private final long[] dimensions;

    MatrixImpl(T array, long[] dimensions) {
        if (array == null)
            throw new NullPointerException("Null array argument");
        if (!array.isUnresizable())
            throw new IllegalArgumentException("Matrix cannot be created on the base of resizable array: "
                + "please use UpdatableArray.asUnresizable() method before constructing a matrix");
        this.array = array;
        this.dimensions = dimensions.clone();
        checkDimensions(this.dimensions, array.length());
    }

    public T array() {
        return this.array;
    }

    public long size() {
        return this.array.length();
    }

    public long[] dimensions() {
        return this.dimensions.clone();
    }

    public int dimCount() {
        return this.dimensions.length;
    }

    public long dim(int n) {
        return n < this.dimensions.length ? this.dimensions[n] : 1;
    }

    public <U extends Array> Matrix<U> matrix(U anotherArray) {
        return new MatrixImpl<U>(anotherArray, dimensions);
    }
}
