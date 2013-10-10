package net.algart.math.functions;

import net.algart.math.Range;

/**
 * <p>Absolute value function:
 * <i>f</i>(<i>x</i><sub>0</sub>) = <tt>in</tt>
 * if <tt>min&lt;=<i>x</i><sub>0</sub>&lt;=max</tt> and
 * <i>f</i>(<i>x</i><sub>0</sub>) = <tt>out</tt>
 * in other case, where <tt>min</tt>, <tt>max</tt>, <tt>in</tt>, <tt>out</tt> are parameters of this function.
 * The {@link #get} method of the instance of this class requires at least 1 argument
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is 0.</p>
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
public final class RectangularFunc implements Func {
    final Range range;
    final double min;
    final double max;
    final double in;
    final double out;

    private RectangularFunc(Range range, double in, double out) {
        this.range = range;
        this.min = range.min();
        this.max = range.max();
        this.in = in;
        this.out = out;
    }

    /**
     * Returns an instance of this class describing the rectangular function with the following parameters:
     * <tt>range.{@link Range#min() min()}</tt>,
     * <tt>range.{@link Range#max() max()}</tt>,
     * <tt>in</tt>, <tt>out</tt>.
     * The {@link #get get} method of the function will return <tt>in</tt> if
     * <tt>range.{@link Range#contains(double) contains}(<i>x</i><sub>0</sub>)</tt>
     * and <tt>out</tt> otherwise.
     *
     * @param range the <tt>min</tt> and <tt>max</tt> parameters of this function.
     * @param in    the value returned inside the range.
     * @param out   the value returned outside the range.
     * @return      an instance of this class with the passed parameters.
     * @throws NullPointerException if <tt>range</tt> is <tt>null</tt>.
     */
    public static RectangularFunc getInstance(Range range, double in, double out) {
        return new RectangularFunc(range, in, out);
    }

    /**
     * Returns an instance of this class describing the rectangular function with the passed parameters.
     * The {@link #get get} method of the function will return <tt>in</tt> if
     * <tt><tt>min&lt;=<i>x</i><sub>0</sub>&lt;=max</tt>
     * and <tt>out</tt> otherwise.
     *
     * @param min the minimum number in the range, inclusive.
     * @param max the maximum number in the range, inclusive.
     * @param in  the value returned inside the range.
     * @param out the value returned outside the range.
     * @return    an instance of this class with the passed parameters.
     * @throws IllegalArgumentException if <tt>min &gt; max</tt>, <tt>min</tt> is <tt>NaN</tt> or
     *                                  <tt>max</tt> is <tt>NaN</tt>.
     */
    public static RectangularFunc getInstance(double min, double max, double in, double out) {
        return new RectangularFunc(Range.valueOf(min, max), in, out);
    }

    /**
     * Returns <tt>min..max</tt> range, inside which this function returns <tt>in</tt> value.
     *
     * @return <tt>min..max</tt> range, specified while creating this instance.
     */
    public Range range() {
        return this.range;
    }

    /**
     * Returns the value, which is returned by this function inside <tt>min..max</tt> range.
     *
     * @return <tt>in</tt> value, specified while creating this instance.
     */
    public double in() {
        return this.in;
    }

    /**
     * Returns the value, which is returned by this function outside <tt>min..max</tt> range.
     *
     * @return <tt>out</tt> value, specified while creating this instance.
     */
    public double out() {
        return this.out;
    }

    public double get(double ...x) {
        return x[0] >= min && x[0] <= max ? in : out;
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public double get(double x0) {
        return x0 >= min && x0 <= max ? in : out;
    }

    public double get(double x0, double x1) {
        return x0 >= min && x0 <= max ? in : out;
    }

    public double get(double x0, double x1, double x2) {
        return x0 >= min && x0 <= max ? in : out;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return x0 >= min && x0 <= max ? in : out;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "rectangular function f(x)=" + min + "<=x<=" + max + "?" + in + ":" + out;
    }
}
