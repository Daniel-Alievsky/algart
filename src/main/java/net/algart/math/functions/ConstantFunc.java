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
 * <p>Trivial constant function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) = <i>c</i>,
 * where <i>c</i> is a constant.
 * The {@link #get} method of the instance of this class may process any number of arguments.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify the settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class ConstantFunc implements Func {
    private ConstantFunc() {
    }

    /**
     * Returns an instance of this class that always returns the passed constant <code>c</code>.
     *
     * @param c the constant returned by {@link #get(double...)} method.
     * @return an instance of this class.
     */
    public static ConstantFunc getInstance(final double c) {
        return new ConstantFunc() {
            public double get(double... x) {
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

    public abstract double get(double... x);

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
