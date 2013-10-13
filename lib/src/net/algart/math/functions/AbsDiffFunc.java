package net.algart.math.functions;

/**
 * <p>Absolute value of the difference of 2 numbers:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) =
 * |<i>x</i><sub>0</sub>-<i>x</i><sub>1</sub>|.
 * The {@link #get} method of the instance of this class requires at least 2 arguments
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is 0 or 1.
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
final class AbsDiffFunc implements Func {
    AbsDiffFunc() {
    }

    public double get(double ...x) {
        return StrictMath.abs(x[0] - x[1]);
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0, double x1) {
        return StrictMath.abs(x0 - x1);
    }

    public double get(double x0, double x1, double x2) {
        return StrictMath.abs(x0 - x1);
    }

    public double get(double x0, double x1, double x2, double x3) {
        return StrictMath.abs(x0 - x1);
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "absolute difference function f(x,y)=|x-y|";
    }
}
