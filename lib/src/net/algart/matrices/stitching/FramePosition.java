package net.algart.matrices.stitching;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.RectangularArea;
import net.algart.math.functions.Func;


public interface FramePosition {
    public RectangularArea area();

    public Func asInterpolationFunc(Matrix<? extends PArray> sourceMatrix);

    /**
     * Returns the hash code of this object. The result depends on all parameters, specifying
     * this frame position.
     *
     * @return the hash code of this frame position.
     */
    public int hashCode();

    /**
     * Indicates whether some other object is also {@link FramePosition},
     * specifying the same position as this one.
     *
     * <p>There is high probability, but no guarantee that this method returns <tt>true</tt> if the passed object
     * specifies a frame position, identical to this one.
     * There is a guarantee that this method returns <tt>false</tt>
     * if the passed object specifies a frame position, different than this one.
     *
     * @param obj the object to be compared for equality with this frame position.
     * @return    <tt>true</tt> if the specified object is a frame position equal to this one.
     */
    public boolean equals(Object obj);
}
