/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Exponent function:
 * <i>f</i>(<i>x</i><sub>0</sub>) =
 * <i>b</i><sup><i>x</i><sub>0</sub></sup>
 * (<i>b</i> is the base of the exponent).
 * More precisely, the result of this function is
 * <tt>Math.exp(x[0]*Math.log(b))</tt> for the instance returned by {@link #getInstance(double)} method
 * or <tt>StrictMath.exp(x[0]*StrictMath.log(b))</tt> for the instance
 * returned by {@link #getStrictInstance(double)} method.
 * The {@link #get} method of the instance of this class requires at least 1 argument
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is 0.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class ExpFunc implements Func {
    private final double base;

    private ExpFunc(double base) {
        this.base = base;
    }

    /**
     * Returns an instance of this class for the given logarithm base <tt>b</tt>
     * using <tt>Math.exp</tt> and <tt>Math.log</tt> methods.</p>
     *
     * @param b the base of the logarithm.
     * @return  an instance of this class using <tt>Math.exp</tt> and <tt>Math.log</tt> methods.
     */
    public static ExpFunc getInstance(double b) {
        if (b == Math.E) {
            return new ExpFunc(b) {
                public double get(double ...x) {
                    return Math.exp(x[0]);
                }

                public double get(double x0) {
                    return Math.exp(x0);
                }

                public double get(double x0, double x1) {
                    return Math.exp(x0);
                }

                public double get(double x0, double x1, double x2) {
                    return Math.exp(x0);
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return Math.exp(x0);
                }
            };
        } else {
            final double mult = Math.log(b);
            return new ExpFunc(b) {
                public double get(double ...x) {
                    return Math.exp(x[0] * mult);
                }

                public double get(double x0) {
                    return Math.exp(x0 * mult);
                }

                public double get(double x0, double x1) {
                    return Math.exp(x0 * mult);
                }

                public double get(double x0, double x1, double x2) {
                    return Math.exp(x0 * mult);
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return Math.exp(x0 * mult);
                }
            };
        }
    }

    /**
     * Returns an instance of this class for the given logarithm base <tt>b</tt>
     * using <tt>StrictMath.exp</tt> and <tt>StrictMath.log</tt> methods.</p>
     *
     * @param b the base of the exponent.
     * @return  an instance of this class using <tt>StrictMath.exp</tt> and <tt>StrictMath.log</tt> methods.
     */
    public static ExpFunc getStrictInstance(double b) {
        if (b == Math.E) {
            return new ExpFunc(b) {
                public strictfp double get(double ...x) {
                    return StrictMath.exp(x[0]);
                }

                public strictfp double get(double x0) {
                    return StrictMath.exp(x0);
                }

                public strictfp double get(double x0, double x1) {
                    return StrictMath.exp(x0);
                }

                public strictfp double get(double x0, double x1, double x2) {
                    return StrictMath.exp(x0);
                }

                public strictfp double get(double x0, double x1, double x2, double x3) {
                    return StrictMath.exp(x0);
                }
            };
        } else {
            final double mult = StrictMath.log(b);
            return new ExpFunc(b) {
                public strictfp double get(double ...x) {
                    return StrictMath.exp(x[0] * mult);
                }

                public strictfp double get(double x0) {
                    return StrictMath.exp(x0 * mult);
                }

                public strictfp double get(double x0, double x1) {
                    return StrictMath.exp(x0 * mult);
                }

                public strictfp double get(double x0, double x1, double x2) {
                    return StrictMath.exp(x0 * mult);
                }

                public strictfp double get(double x0, double x1, double x2, double x3) {
                    return StrictMath.exp(x0 * mult);
                }
            };
        }
    }

    public abstract double get(double ...x);

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public abstract double get(double x0);

    public abstract double get(double x0, double x1);

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "exponent function f(x)=" + (base == Math.E ? "e^x" : base + "^x");
    }
}
