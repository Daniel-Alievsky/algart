/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Inverse function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
 * <i>c</i>/<i>x</i><sub>0</sub>
 * (<i>c</i> is some constant).</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class InverseNumberFunc implements Func {
    final double c;

    private InverseNumberFunc(double c) {
        this.c = c;
    }

    /**
     * <p>{@link Func.Updatable Updatable extension} of the {@link InverseNumberFunc inverse function}.</p>
     */
    public static class Updatable extends InverseNumberFunc implements Func.Updatable {
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
