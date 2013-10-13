package net.algart.math.functions;

/**
 * <p>Selecting constant function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>)
 * = <tt>values[(int)</tt><i>x</i><sub>0</sub><tt>]</tt>,
 * where <tt>values</tt> is the given array of constants.
 * The {@link #get} method of the instance of this class requires at least <i>n</i> arguments
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is less
 * or if the argument, cast to <tt>int</tt> type, is negative or <tt>&gt;=values.length</tt>.</p>
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
public final class SelectConstantFunc implements Func {
    private final double[] values;

    private SelectConstantFunc(double[] values) {
        this.values = values.clone();
    }

    /**
     * Returns an instance of this class, describing the selecting function with the specified array of constants.
     *
     * <p>The passed reference <tt>values</tt> is not maintained by the created instance:
     * if necessary, the Java array is cloned.
     *
     * @param values the values, one of which is returned by {@link #get} method..
     * @return       the selection function for the given array of constants.
     */
    public static SelectConstantFunc getInstance(double ...values) {
        return new SelectConstantFunc(values);
    }

    /**
     * Returns all values returned by this function.
     *
     * <p>The returned array is not a reference to an internal array stored in this object:
     * the internal Java array is cloned.
     *
     * @return all values returned by this function.
     */
    public double[] values() {
        return values.clone();
    }

    public double get(double... x) {
        return values[(int)x[0]];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public double get(double x0) {
        return values[(int)x0];
    }

    public double get(double x0, double x1) {
        return values[(int)x0];
    }

    public double get(double x0, double x1, double x2) {
        return values[(int)x0];
    }

    public double get(double x0, double x1, double x2, double x3) {
        return values[(int)x0];
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("selecting constant function f(x0,x1,...)=values[(int)x0], values={");
        for (int k = 0; k < values.length; k++) {
            if (k > 0)
                sb.append(", ");
            if (k >= 2 && k < values.length - 2) {
                sb.append("...");
                k = values.length - 2;
            } else {
                sb.append(LinearFunc.goodFormat(values[k]));
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
