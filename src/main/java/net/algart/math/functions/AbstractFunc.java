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
 * <p>A skeletal implementation of the {@link Func} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>Warning: in most cases, we <b>strongly recommend to override the overloaded versions
 * of <code>get</code> method</b>
 * ({@link #get()}, {@link #get(double)}, {@link #get(double, double)}, {@link #get(double, double, double)}),
 * if they are applicable for your function. The default implementations, provided by this class,
 * work relatively slowly, because they allocate a little Java array for passing to the basic
 * {@link #get(double...)} method. For example, if you create a function with two arguments
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) = <i>x</i><sub>0</sub>/<i>x</i><sub>1</sub>,
 * you should override the version with two arguments: {@link #get(double, double)}.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractFunc implements Func {
    private final double[] EMPTY = new double[0];

    public abstract double get(double... x);

    /**
     * This implementation calls <code>{@link #get(double[]) get}(EMPTY)</code>,
     * where <code>EMPTY</code> is a constant array <code>double[0]</code>.
     *
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 1 argument.
     */
    public double get() {
        return get(EMPTY);
    }

    /**
     * This implementation calls <code>{@link #get(double[]) get}(new double[] {x0})</code>.
     * May be overridden to provide better performance.
     *
     * @param x0 the function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 2 arguments.
     */
    public double get(double x0) {
        return get(new double[]{x0});
    }

    /**
     * This implementation calls <code>{@link #get(double[]) get}(new double[] {x0, x1})</code>.
     * May be overridden to provide better performance.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    public double get(double x0, double x1) {
        return get(new double[]{x0, x1});
    }

    /**
     * This implementation calls <code>{@link #get(double[]) get}(new double[] {x0, x1, x2})</code>.
     * May be overridden to provide better performance.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    public double get(double x0, double x1, double x2) {
        return get(new double[]{x0, x1, x2});
    }

    /**
     * This implementation calls <code>{@link #get(double[]) get}(new double[] {x0, x1, x2, x3})</code>.
     * May be overridden to provide better performance.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @param x3 the fourth function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    public double get(double x0, double x1, double x2, double x3) {
        return get(new double[]{x0, x1, x2, x3});
    }
}
