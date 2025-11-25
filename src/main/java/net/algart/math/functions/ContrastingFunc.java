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
 * <p>Contrasting function: by default,
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>) =
 * <i>M</i> * (<i>x</i><sub>0</sub>/<i>x</i><sub>1</sub>) * (<i>x</i>&minus;<i>x</i><sub>2</sub>) /
 * max(<i>x</i><sub>3</sub>&minus;<i>x</i><sub>2</sub>,<i>threshold</i>),
 * where <i>x</i> = (<i>x</i><sub>1</sub>&lt;<i>x</i><sub>2</sub> ?
 * <i>x</i><sub>2</sub> : <i>x</i><sub>1</sub>&gt;x<sub>3</sub> ? <i>x</i><sub>3</sub> : <i>x</i><sub>1</sub>)
 * is <i>x</i><sub>1</sub>, truncated to <i>x</i><sub>2</sub>..<i>x</i><sub>3</sub> range,
 * <i>M</i> and <i>threshold</i> are constants.
 * In addition, in a case of 3 arguments this function always returns
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>) =
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>).
 * Note that <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>) =
 * (<i>x</i><sub>0</sub>/<i>x</i><sub>1</sub>) *
 * <i>f</i>(<i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>).</p>
 *
 * <p>If <i>x</i><sub>0</sub>=<i>x</i><sub>1</sub>=<i>b</i> is a brightness of some image pixel,
 * <i>x</i><sub>2</sub>..<i>x</i><sub>3</sub> represents some brightness range,
 * and <i>threshold</i> is a positive value &le;<i>x</i><sub>3</sub>&minus;<i>x</i><sub>2</sub>,
 * this function performs contrasting of <i>x</i><sub>2</sub>..<i>x</i><sub>3</sub> range to 0..<i>M</i> range:
 * all values <i>b</i>&le;<i>x</i><sub>2</sub> are transformed to 0,
 * all values <i>b</i>&ge;<i>x</i><sub>3</sub> are transformed to <i>M</i>,
 * and values between <i>x</i><sub>2</sub> and <i>x</i><sub>3</sub> are proportionally
 * transformed to values between 0 and <i>M</i>.
 * However, if <i>x</i><sub>3</sub>&minus;<i>x</i><sub>2</sub> is too small, i.e. &lt;<i>threshold</i>,
 * this value is used instead of <i>x</i><sub>3</sub>&minus;<i>x</i><sub>2</sub>:
 * <i>b</i>=<i>x</i><sub>3</sub> and greater values are transformed not to <i>M</i>,
 * but only to <i>M</i>*(<i>x</i><sub>3</sub>&minus;<i>x</i><sub>2</sub>)/<i>threshold</i>.
 * It allows avoiding to perform very "strong" contrasting when <i>x</i><sub>3</sub>&minus;<i>x</i><sub>2</sub>
 * is very small and, in particular, avoiding 0/0 when <i>x</i><sub>3</sub>=<i>x</i><sub>2</sub>.
 * Usually <i>x</i><sub>2</sub> and <i>x</i><sub>3</sub> are the minimal (or almost minimal)
 * and the maximal (or almost maximal) brightnesses of the image or of some image area.</p>
 *
 * <p>If <i>x</i><sub>0</sub>&ne;<i>x</i><sub>1</sub>, this functions performs contrasting of <i>x</i><sub>1</sub>
 * to the contrasted value <i>y</i> =
 * <i>f</i>(<i>x</i><sub>1</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>),
 * as described above, and returns proportionally changed <i>x</i><sub>0</sub>,
 * i.e. <i>y</i>*(<i>x</i><sub>0</sub>/<i>x</i><sub>1</sub>). It can be useful for contrasting color images:
 * we can pass the average intensity (brightness) as <i>x</i><sub>1</sub> argument and a concrete color component
 * (red, green, blue) as <i>x</i><sub>0</sub>, then the image will be contrasted with preserving colors.</p>
 *
 * <p>The described implementation is default and used when you create an instance by
 * {@link #getInstance(double, double) getInstance(<i>M</i>,<i>threshold</i>)} call.
 * But you can create your own implementation of this abstract class, for example, with another
 * logic of threshold processing. It is supposed that any implementation of this class
 * in a case of 3 arguments performs some "contrasting" of the brightness, specified by the 1st argument,
 * accordingly the brightness range, specified by 2nd and 3rd argument, and that in a case of 4 arguments
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>) =
 * (<i>x</i><sub>0</sub>/<i>x</i><sub>1</sub>) *
 * <i>f</i>(<i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>).
 *
 * <p>The {@link #get} method of the instances of this class requires at least 3 arguments
 * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0, 1 or 2.</p>
 *
 * <p>All implementations of this class are <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify the settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class ContrastingFunc implements Func {

    /**
     * Creates an instance of this class.
     */
    protected ContrastingFunc() {
    }

    /**
     * Returns an instance of this class, providing the default behavior described in the
     * {@link ContrastingFunc comments to this class}.
     *
     * @param m         the <i>M</i> constant: maximal value returned by this function.
     * @param threshold the <i>threshold</i> constant.
     * @return the contrasting function with the given parameters.
     */
    public static ContrastingFunc getInstance(final double m, final double threshold) {
        return new ContrastingFunc() {
            @Override
            public double get(double... x) {
                if (x.length < 3) {
                    throw new IndexOutOfBoundsException("At least 3 arguments required");
                }
                if (x.length == 3) {
                    double w = x[0] < x[1] ? x[1] : Math.min(x[0], x[2]);
                    return m * (w - x[1]) / Math.max(x[2] - x[1], threshold);
                }
                double w = x[1] < x[2] ? x[2] : Math.min(x[1], x[3]);
                return m * x[0] * (w - x[2]) / (Math.max(x[3] - x[2], threshold) * x[1]);
            }

            @Override
            public double get(double x0, double x1, double x2) {
                double w = x0 < x1 ? x1 : Math.min(x0, x2);
                return m * (w - x1) / Math.max(x2 - x1, threshold);
            }

            @Override
            public double get(double x0, double x1, double x2, double x3) {
                double w = x1 < x2 ? x2 : Math.min(x1, x3);
                return m * x0 * (w - x2) / (Math.max(x3 - x2, threshold) * x1);
            }

            @Override
            public String toString() {
                return "contrasting function f(x,y,a,b)=" + m + "*(x/y)*max(y'-a,"
                        + threshold + ")/(b-a), y'=y<a?a:y>b?b:y";
            }
        };
    }

    /**
     * This method is fully implemented in this class:
     * it returns {@link #get(double, double, double, double) get(x[0],x[1],x[2],x[3])},
     * if <code>x.length&gt;=4</code>,
     * returns {@link #get(double, double, double) get(x[0],x[1],x[2])}, if <code>x.length==3</code>,
     * and throws <code>IndexOutOfBoundsException</code>, if <code>x.length&lt;3</code>.
     *
     * @param x the function arguments.
     * @return the function result.
     * @throws IndexOutOfBoundsException if the number of passed arguments is 0, 1 or 2.
     */
    public double get(double... x) {
        if (x.length < 3) {
            throw new IndexOutOfBoundsException("At least 3 arguments required");
        }
        if (x.length == 3) {
            return get(x[0], x[1], x[2]);
        }
        return get(x[0], x[1], x[2], x[3]);
    }

    public final double get() {
        throw new IndexOutOfBoundsException("At least 3 arguments required");
    }

    public final double get(double x0) {
        throw new IndexOutOfBoundsException("At least 3 arguments required");
    }

    public final double get(double x0, double x1) {
        throw new IndexOutOfBoundsException("At least 3 arguments required");
    }

    public abstract double get(double x0, double x1, double x2);

    /**
     * This method is fully implemented in this class:
     * it returns <code>x0/x1*{@link #get(double, double, double) get(x1,x2,x3)}</code>.
     * You can override it toe provide better performance or precision.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @param x3 the fourth function argument.
     * @return the function result.
     */
    public double get(double x0, double x1, double x2, double x3) {
        return x0 / x1 * get(x1, x2, x3);
    }

}
