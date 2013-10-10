package net.algart.matrices.linearfiltering;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.WeightedPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BasicConvolution extends AbstractConvolution implements Convolution {

    private BasicConvolution(ArrayContext context, boolean incrementForUnsigned, boolean incrementByHalfForInteger) {
        super(context, incrementForUnsigned, incrementByHalfForInteger);
    }

    /**
     * Returns new instance of this class.
     *
     * @param context                   the {@link #context() context} that will be used by this object;
     *                                  may be <tt>null</tt>, then it will be ignored.
     * @param incrementByHalfForInteger if <tt>true</tt>, when the type of the convolution result is integer,
     *                                  the precise result is automatically increments by 0.5 before casting.
     * @return                          new instance of this class.
     */
    public static BasicConvolution getInstance(ArrayContext context, boolean incrementByHalfForInteger) {
        return new BasicConvolution(context, false, incrementByHalfForInteger);
    }

    /**
     * Returns new instance of this class, correcting unsigned convolutions results.
     * If the type of the convolution result is an unsigned number in terms of AlgART libraries &mdash;
     * <tt>byte</tt>, <tt>short</tt>, <tt>char</tt> &mdash; it is automatically incremented by 128
     * (<tt>byte</tt>) or 32768 (<tt>short</tt> and <tt>char</tt>).
     *
     * @param context                   the {@link #context() context} that will be used by this object;
     *                                  may be <tt>null</tt>, then it will be ignored.
     * @return                          new instance of this class.
     */
    public static BasicConvolution getCorrectingUnsignedInstance(ArrayContext context) {
        return new BasicConvolution(context, true, false);
    }

    @Override
    public boolean isPseudoCyclic() {
        return true;
    }

    @Override
    public <T extends PArray> Matrix<T> asConvolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        Set<IPoint> points = pattern.roundedPoints();
        List<Matrix<? extends PArray>> shifted = new ArrayList<Matrix<? extends PArray>>();
        double[] weights = new double[points.size()];
        for (IPoint ip : points) {
            double w = pattern.weight(ip);
            if (w != 0.0) {
                Matrix<? extends PArray> m = Matrices.asShifted(src, ip.coordinates()).cast(PArray.class);
                weights[shifted.size()] = w;
                shifted.add(m);
            }
        }
        if (weights.length > shifted.size()) {
            weights = JArrays.copyOfRange(weights, 0, shifted.size());
        }
        double increment = increment(Arrays.elementType(requiredType));
        return Matrices.asFuncMatrix(LinearFunc.getInstance(increment, weights), requiredType, shifted);
    }
}
