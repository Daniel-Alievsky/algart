/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.math.functions;

/**
 * <p>Selecting constant function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>)
 * = <code>values[(int)</code><i>x</i><sub>0</sub><code>]</code>,
 * where <code>values</code> is the given array of constants.
 * The {@link #get} method of the instance of this class requires at least <i>n</i> arguments
 * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is less
 * or if the argument, cast to <code>int</code> type, is negative or <code>&gt;=values.length</code>.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public final class SelectConstantFunc implements Func {
    private final double[] values;

    private SelectConstantFunc(double[] values) {
        this.values = values.clone();
    }

    /**
     * Returns an instance of this class, describing the selecting function with the specified array of constants.
     *
     * <p>The passed reference <code>values</code> is not maintained by the created instance:
     * if necessary, the Java array is cloned.
     *
     * @param values the values, one of which is returned by {@link #get} method..
     * @return the selection function for the given array of constants.
     */
    public static SelectConstantFunc getInstance(double... values) {
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
        return values[(int) x[0]];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public double get(double x0) {
        return values[(int) x0];
    }

    public double get(double x0, double x1) {
        return values[(int) x0];
    }

    public double get(double x0, double x1, double x2) {
        return values[(int) x0];
    }

    public double get(double x0, double x1, double x2, double x3) {
        return values[(int) x0];
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("selecting constant function f(x0,x1,...)=values[(int)x0], values={");
        for (int k = 0; k < values.length; k++) {
            if (k > 0) {
                sb.append(", ");
            }
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
