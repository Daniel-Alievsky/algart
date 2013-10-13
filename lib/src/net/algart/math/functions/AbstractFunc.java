package net.algart.math.functions;

/**
 * <p>A skeletal implementation of the {@link Func} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>Warning: in most cases, we <b>strongly recommend to override the overloaded versions of <tt>get</tt> method</b>
 * ({@link #get()}, {@link #get(double)}, {@link #get(double, double)}, {@link #get(double, double, double)}),
 * if they are applicable for your function. The default implementations, provided by this class,
 * work relatively slowly, because they allocate a little Java array for passing to the basic
 * {@link #get(double...)} method. For example, if you create a function with two arguments
 * <nobr><i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) = <i>x</i><sub>0</sub>/<i>x</i><sub>1</sub></nobr>,
 * you should override the version with two arguments: {@link #get(double, double)}.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractFunc implements Func {
    private double[] EMPTY = new double[0];

    public abstract double get(double ...x);

    /**
     * This implementation calls <tt>{@link #get(double[]) get}(EMPTY)</tt>,
     * where <tt>EMPTY</tt> is a constant array <tt>double[0]</tt>.
     *
     * @return   the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 1 argument.
     */
    public double get() {
        return get(EMPTY);
    }

    /**
     * This implementation calls <tt>{@link #get(double[]) get}(new double[] {x0})</tt>.
     * May be overridden to provide better performance.
     *
     * @param x0 the function argument.
     * @return   the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 2 arguments.
     */
    public double get(double x0) {
        return get(new double[] {x0});
    }

    /**
     * This implementation calls <tt>{@link #get(double[]) get}(new double[] {x0, x1})</tt>.
     * May be overridden to provide better performance.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @return   the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    public double get(double x0, double x1) {
        return get(new double[] {x0, x1});
    }

    /**
     * This implementation calls <tt>{@link #get(double[]) get}(new double[] {x0, x1, x2})</tt>.
     * May be overridden to provide better performance.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @return   the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    public double get(double x0, double x1, double x2) {
        return get(new double[] {x0, x1, x2});
    }

    /**
     * This implementation calls <tt>{@link #get(double[]) get}(new double[] {x0, x1, x2, x3})</tt>.
     * May be overridden to provide better performance.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @param x3 the fourth function argument.
     * @return   the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    public double get(double x0, double x1, double x2, double x3) {
        return get(new double[] {x0, x1, x2, x3});
    }
}
