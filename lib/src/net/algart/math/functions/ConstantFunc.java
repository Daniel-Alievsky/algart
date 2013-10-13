package net.algart.math.functions;

/**
 * <p>Trivial constant function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) = <i>c</i>,
 * where <i>c</i> is a constant.
 * The {@link #get} method of the instance of this class may process any number of arguments.</p>
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
public abstract class ConstantFunc implements Func {
    private ConstantFunc() {
    }

    /**
     * Returns an instance of this class that always returns the passed constant <tt>c</tt>.</p>
     *
     * @param c the constant returned by {@link #get(double...)} method.
     * @return  an instance of this class.</p>
     */
    public static ConstantFunc getInstance(final double c) {
        return new ConstantFunc() {
            public double get(double ...x) {
                return c;
            }

            public double get() {
                return c;
            }

            public double get(double x0) {
                return c;
            }

            public double get(double x0, double x1) {
                return c;
            }

            public double get(double x0, double x1, double x2) {
                return c;
            }

            public double get(double x0, double x1, double x2, double x3) {
                return c;
            }
        };
    }

    public abstract double get(double ...x);

    public abstract double get();

    public abstract double get(double x0);

    public abstract double get(double x0, double x1);

    public abstract double get(double x0, double x1, double x2);

    public abstract double get(double x0, double x1, double x2, double x3);

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "constant function f(x)=" + get();
    }
}
