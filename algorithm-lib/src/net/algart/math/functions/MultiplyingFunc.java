package net.algart.math.functions;

/**
 * <p>Product from several numbers:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
 * <i>scale</i>*<i>x</i><sub>0</sub><i>x</i><sub>1</sub>...<i>x</i><sub><i>n</i>-1</sub>,
 * where <i>scale</i> is a constant, passed to {@link #getInstance(double)} method.
 * The {@link #get} method of the instance of this class may process any number of arguments.
 * If the number of arguments is 0, it returns <i>scale</i> value.
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
public final strictfp class MultiplyingFunc implements Func {
    private final double scale;

    private MultiplyingFunc(double scale) {
        this.scale = scale;
    }

    /**
     * Returns an instance of this class.
     *
     * @param scale the scale: additional constant multiplier.
     * @return      an instance of this class.
     */
    public static MultiplyingFunc getInstance(double scale) {
        return new MultiplyingFunc(scale);
    }

    public double get(double ...x) {
        double result = scale;
        for (double v : x)
            result *= v;
        return result;
    }

    public double get() {
        return scale;
    }

    public double get(double x0) {
        return scale * x0;
    }

    public double get(double x0, double x1) {
        return scale * x0 * x1;
    }

    public double get(double x0, double x1, double x2) {
        return scale * x0 * x1 * x2;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return scale * x0 * x1 * x2 * x3;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "product function f(x0,x1,...)=" + scale + "*x0*x1*...";
    }
}
