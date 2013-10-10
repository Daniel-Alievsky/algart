package net.algart.math.functions;

/**
 * <p>Weighted mean of 2 numbers:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>) =
 * <i>scale</i>*<i>x</i><sub>0</sub><i>x</i><sub>1</sub> + (1-<i>scale</i>*<i>x</i><sub>0</sub>)<i>x</i><sub>2</sub>,
 * where <i>scale</i> is a constant, passed to {@link #getInstance(double)} method.
 * The {@link #get} method of the instance of this class requires at least 3 arguments
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is 0, 1 or 2.
 * All calculations are performed in <tt>strictfp</tt> mode, so the result
 * is absolutely identical on all platforms.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public final strictfp class WeightedMeanFunc implements Func {
    private final double scale;

    private WeightedMeanFunc(double scale) {
        this.scale = scale;
    }

    /**
     * Returns an instance of this class.
     *
     * @param scale the scale for the weight.
     * @return      an instance of this class.
     */
    public static WeightedMeanFunc getInstance(double scale) {
        return new WeightedMeanFunc(scale);
    }

    public double get(double ...x) {
        return scale * x[0] * (x[1] - x[2]) + x[2];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 3 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 3 arguments required");
    }

    public double get(double x0, double x1) {
        throw new IndexOutOfBoundsException("At least 3 arguments required");
    }

    public double get(double x0, double x1, double x2) {
        return scale * x0 * (x1 - x2) + x2;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return scale * x0 * (x1 - x2) + x2;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "weighted mean function f(x0,x1,...)=" + scale + "*x0*x1+(1-" + scale + "*x0)*x2";
    }
}
