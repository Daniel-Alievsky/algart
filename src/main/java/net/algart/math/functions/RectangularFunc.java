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

import net.algart.math.Range;

/**
 * <p>Absolute value function:
 * <i>f</i>(<i>x</i><sub>0</sub>) = <code>in</code>
 * if <code>min&lt;=<i>x</i><sub>0</sub>&lt;=max</code> and
 * <i>f</i>(<i>x</i><sub>0</sub>) = <code>out</code>
 * in another case, where <code>min</code>, <code>max</code>, <code>in</code>, <code>out</code>
 * are parameters of this function.
 * The {@link #get} method of the instance of this class requires at least 1 argument
 * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
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
     * <code>range.{@link Range#min() min()}</code>,
     * <code>range.{@link Range#max() max()}</code>,
     * <code>in</code>, <code>out</code>.
     * The {@link #get get} method of the function will return <code>in</code> if
     * <code>range.{@link Range#contains(double) contains}(<i>x</i><sub>0</sub>)</code>
     * and <code>out</code> otherwise.
     *
     * @param range the <code>min</code> and <code>max</code> parameters of this function.
     * @param in    the value returned inside the range.
     * @param out   the value returned outside the range.
     * @return an instance of this class with the passed parameters.
     * @throws NullPointerException if <code>range</code> is {@code null}.
     */
    public static RectangularFunc getInstance(Range range, double in, double out) {
        return new RectangularFunc(range, in, out);
    }

    /**
     * Returns an instance of this class describing the rectangular function with the passed parameters.
     * The {@link #get get} method of the function will return <code>in</code> if
     * <code>min&lt;=<i>x</i><sub>0</sub>&lt;=max</code>
     * and <code>out</code> otherwise.
     *
     * @param min the minimum number in the range, inclusive.
     * @param max the maximum number in the range, inclusive.
     * @param in  the value returned inside the range.
     * @param out the value returned outside the range.
     * @return an instance of this class with the passed parameters.
     * @throws IllegalArgumentException if <code>min &gt; max</code>, <code>min</code> is <code>NaN</code> or
     *                                  <code>max</code> is <code>NaN</code>.
     */
    public static RectangularFunc getInstance(double min, double max, double in, double out) {
        return new RectangularFunc(Range.valueOf(min, max), in, out);
    }

    /**
     * Returns <code>min..max</code> range, inside which this function returns <code>in</code> value.
     *
     * @return <code>min..max</code> range, specified while creating this instance.
     */
    public Range range() {
        return this.range;
    }

    /**
     * Returns the value, which is returned by this function inside <code>min..max</code> range.
     *
     * @return <code>in</code> value, specified while creating this instance.
     */
    public double in() {
        return this.in;
    }

    /**
     * Returns the value, which is returned by this function outside <code>min..max</code> range.
     *
     * @return <code>out</code> value, specified while creating this instance.
     */
    public double out() {
        return this.out;
    }

    public double get(double... x) {
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
