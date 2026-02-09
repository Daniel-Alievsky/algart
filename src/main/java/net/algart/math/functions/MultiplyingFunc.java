/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Product from several numbers:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
 * <i>scale</i>*<i>x</i><sub>0</sub><i>x</i><sub>1</sub>...<i>x</i><sub><i>n</i>-1</sub>,
 * where <i>scale</i> is a constant, passed to {@link #getInstance(double)} method.
 * The {@link #get} method of the instance of this class may process any number of arguments.
 * If the number of arguments is 0, it returns <i>scale</i> value.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify the settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public final class MultiplyingFunc implements Func {
    private final double scale;

    private MultiplyingFunc(double scale) {
        this.scale = scale;
    }

    /**
     * Returns an instance of this class.
     *
     * @param scale the scale: additional constant multiplier.
     * @return an instance of this class.
     */
    public static MultiplyingFunc getInstance(double scale) {
        return new MultiplyingFunc(scale);
    }

    public double get(double... x) {
        double result = scale;
        for (double v : x) {
            result *= v;
        }
        return result;
    }

    public double get() {
        return scale;
    }

    public double get(double x0) {
        return scale * x0;
    }

    public double get(double x0, double x1) {
        return scale * x0 * x1;
    }

    public double get(double x0, double x1, double x2) {
        return scale * x0 * x1 * x2;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return scale * x0 * x1 * x2 * x3;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "product function f(x0,x1,...)=" + scale + "*x0*x1*...";
    }
}
