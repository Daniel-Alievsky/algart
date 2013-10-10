package net.algart.matrices.stitching;

import net.algart.arrays.*;

public class DefaultFrame<P extends FramePosition> implements Frame<P> {

    private final Matrix<? extends PArray> matrix;
    private final P position;

    protected DefaultFrame(Matrix<? extends PArray> matrix, P position) {
        if (matrix == null)
            throw new NullPointerException("Null matrix argument");
        if (position == null)
            throw new NullPointerException("Null position argument");
        if (matrix.dimCount() != position.area().coordCount())
            throw new IllegalArgumentException("Different number of dimensions in passed matrix and position: "
              + "position coordinates are " + position.area() + ", matrix is " + matrix);
        this.matrix = matrix;
        this.position = position;
    }

    public static <P extends FramePosition> DefaultFrame<P> valueOf(Matrix<? extends PArray> matrix, P position) {
        return new DefaultFrame<P>(matrix, position);
    }

    public int dimCount() {
        return matrix().dimCount();
    }

    public Matrix<? extends PArray> matrix() {
        return matrix;
    }

    public P position() {
        return position;
    }

    public void freeResources() {
        matrix.freeResources();
    }

    @Override
    public String toString() {
        return "frame " + matrix() + " at " + position();
    }

    public int hashCode() {
        return matrix().hashCode() * 37 + position().hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Frame<?>
            && matrix().equals(((Frame<?>)obj).matrix()) && position().equals(((Frame<?>)obj).position()));
    }

}