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
 * <p>Logarithm function:
 * <i>f</i>(<i>x</i><sub>0</sub>) =
 * log<sub><i>b</i></sub>(<i>x</i><sub>0</sub>)
 * (<i>b</i> is the base of the logarithm).
 * More precisely, the result of this function is
 * <code>Math.log(x[0])*(1.0/Math.log(b))</code> for the instance
 * returned by {@link #getInstance(double) getInstance(b)} method
 * or <code>StrictMath.log(x[0])*(1.0/Math.log(b))</code> for the instance
 * returned by {@link #getStrictInstance(double) getStrictInstance(b)} method.
 * The {@link #get} method of the instance of this class requires at least 1 argument
 * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class LogFunc implements Func {
    private final double base;

    private LogFunc(double base) {
        this.base = base;
    }

    /**
     * Returns an instance of this class for the given logarithm base <code>b</code>
     * using <code>Math.log</code> method.
     *
     * <p>This method returns special optimized versions of this class for <code>base=Math.E</code>
     * and <code>base=10.0</code>.
     *
     * @param b the base of the logarithm.
     * @return an instance of this class using <code>Math.log</code> method.
     */
    public static LogFunc getInstance(double b) {
        if (b == Math.E) {
            return new LogFunc(b) {
                public double get(double... x) {
                    return Math.log(x[0]);
                }

                public double get(double x0) {
                    return Math.log(x0);
                }

                public double get(double x0, double x1) {
                    return Math.log(x0);
                }

                public double get(double x0, double x1, double x2) {
                    return Math.log(x0);
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return Math.log(x0);
                }
            };
        } else if (b == 10.0) {
            return new LogFunc(b) {
                public double get(double... x) {
                    return Math.log10(x[0]);
                }

                public double get(double x0) {
                    return Math.log10(x0);
                }

                public double get(double x0, double x1) {
                    return Math.log10(x0);
                }

                public double get(double x0, double x1, double x2) {
                    return Math.log10(x0);
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return Math.log10(x0);
                }
            };
        } else {
            final double mult = 1.0 / Math.log(b);
            return new LogFunc(b) {
                public double get(double... x) {
                    return Math.log(x[0]) * mult;
                }

                public double get(double x0) {
                    return Math.log(x0) * mult;
                }

                public double get(double x0, double x1) {
                    return Math.log(x0) * mult;
                }

                public double get(double x0, double x1, double x2) {
                    return Math.log(x0) * mult;
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return Math.log(x0) * mult;
                }
            };
        }
    }

    /**
     * Returns an instance of this class for the given logarithm base <code>b</code>
     * using <code>StrictMath.log</code> method.
     *
     * <p>This method returns special optimized versions of this class for <code>base=Math.E</code>
     * and <code>base=10.0</code>.
     *
     * @param b the base of the logarithm.
     * @return an instance of this class using <code>StrictMath.log</code> method.
     */
    public static LogFunc getStrictInstance(double b) {
        if (b == Math.E) {
            return new LogFunc(b) {
                public double get(double... x) {
                    return StrictMath.log(x[0]);
                }

                public double get(double x0) {
                    return StrictMath.log(x0);
                }

                public double get(double x0, double x1) {
                    return StrictMath.log(x0);
                }

                public double get(double x0, double x1, double x2) {
                    return StrictMath.log(x0);
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return StrictMath.log(x0);
                }
            };
        } else if (b == 10.0) {
            return new LogFunc(b) {
                public double get(double... x) {
                    return StrictMath.log10(x[0]);
                }

                public double get(double x0) {
                    return StrictMath.log10(x0);
                }

                public double get(double x0, double x1) {
                    return StrictMath.log10(x0);
                }

                public double get(double x0, double x1, double x2) {
                    return StrictMath.log10(x0);
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return StrictMath.log10(x0);
                }
            };
        } else {
            final double mult = 1.0 / Math.log(b);
            return new LogFunc(b) {
                public double get(double... x) {
                    return StrictMath.log(x[0]) * mult;
                }

                public double get(double x0) {
                    return StrictMath.log(x0) * mult;
                }

                public double get(double x0, double x1) {
                    return StrictMath.log(x0) * mult;
                }

                public double get(double x0, double x1, double x2) {
                    return StrictMath.log(x0) * mult;
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return StrictMath.log(x0) * mult;
                }
            };
        }
    }

    public abstract double get(double... x);

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

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
        return "function f(x)=log(x) (base=" + this.base + ")";
    }
}
