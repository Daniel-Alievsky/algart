package net.algart.math.functions;

/**
 * <p>A skeletal implementation of the {@link CoordinateTransformationOperator} interface to minimize
 * the effort required to implement this interface.
 * Namely, this class contain complete implementation of {@link CoordinateTransformationOperator#apply} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractCoordinateTransformationOperator implements CoordinateTransformationOperator {
    public abstract void map(double[] destPoint, double[] srcPoint);

    public Func apply(Func f) {
        return CoordinateTransformedFunc.getInstance(f, this);
    }
}
