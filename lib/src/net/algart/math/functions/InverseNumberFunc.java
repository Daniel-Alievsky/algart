package net.algart.math.functions;

/**
 * <p>Inverse function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
 * <i>c</i>/<i>x</i><sub>0</sub>
 * (<i>c</i> is some constant).
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
public strictfp class InverseNumberFunc implements Func {
    final double c;

    private InverseNumberFunc(double c) {
        this.c = c;
    }

    /**
     * <p>{@link Func.Updatable Updatable extension} of the {@link InverseNumberFunc inverse function}.</p>
     */
    public static strictfp class Updatable extends InverseNumberFunc implements Func.Updatable {
        private Updatable(double c) {
            super(c);
        }

        public void set(double[] x, double newResult) {
            x[0] = c / newResult;
        }
    }

    /**
     * Returns an instance of this class for the given constant <tt>c</tt>.
     *
     * @param c the numerator of fraction.
     * @return  an instance of this class.
     */
    public static InverseNumberFunc getInstance(double c) {
        return new InverseNumberFunc(c);
    }

    /**
     * Returns an instance of the updatable version of this class for the given constant <tt>c</tt>.
     *
     * @param c the numerator of fraction.
     * @return an instance of the updatable version of this class.
     */
    public static Updatable getUpdatableInstance(double c) {
        return new InverseNumberFunc.Updatable(c);
    }

    public double get(double ...x) {
        return c / x[0];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public double get(double x0) {
        return c / x0;
    }

    public double get(double x0, double x1) {
        return c / x0;
    }

    public double get(double x0, double x1, double x2) {
        return c / x0;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return c / x0;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "inverse function f(x)=" + c + "/x";
    }
}
