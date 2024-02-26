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

import net.algart.math.Range;

import java.util.Locale;

/**
 * <p>Linear function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
 * <i>b</i> + <i>a</i><sub>0</sub><i>x</i><sub>0</sub> + <i>a</i><sub>1</sub><i>x</i><sub>1</sub>
 * +...+ <i>a</i><sub><i>n</i>-1</sub><i>x</i><sub><i>n</i>-1</sub>.
 * Note: if <i>b</i>==+0.0 or <i>b</i>==&minus;0.0, this sum is calculated as
 * <nobr>+0.0 + <i>a</i><sub>0</sub><i>x</i><sub>0</sub> + <i>a</i><sub>1</sub><i>x</i><sub>1</sub>
 * +...+ <i>a</i><sub><i>n</i>-1</sub><i>x</i><sub><i>n</i>-1</sub></nobr>;
 * according Java specification, it means that this function never returns &minus;0.0 double value.</p>
 *
 * <p>The {@link #get} method of the instance of this class requires at least <i>n</i> arguments
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is less.</p>
 * All calculations are performed in <tt>strictfp</tt> mode, so the result
 * is absolutely identical on all platforms.</p>
 *
 * <p>Please note: if all <i>a</i><sub><i>i</i></sub> coefficients are equal (averaging function),
 * this class does not spend Java memory for storing them.
 * So you can freely create averaging linear function with very large number of coefficients;
 * but in this case you should avoid calling {@link #a()} method.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public abstract strictfp class LinearFunc implements Func {
    /**
     * <p>{@link Func.Updatable Updatable extension} of the {@link LinearFunc linear function}
     * with one argument.</p>
     */
    public static strictfp class Updatable extends LinearFunc implements Func.Updatable {
        private final double aInv;
        private Updatable(double b, double a) {
            super(b, new double[] {a});
            aInv = 1.0 / a;
        }

        public double get(double ...x) {
            return this.b + this.a[0] * x[0];
        }

        public double get() {
            throw new IndexOutOfBoundsException("At least 1 argument required");
        }

        public double get(double x0) {
            return this.b + this.a[0] * x0;
        }

        public double get(double x0, double x1) {
            return this.b + this.a[0] * x0;
        }

        public double get(double x0, double x1, double x2) {
            return this.b + this.a[0] * x0;
        }

        public double get(double x0, double x1, double x2, double x3) {
            return this.b + this.a[0] * x0;
        }

        public void set(double[] x, double newResult) {
            x[0] = (newResult - b) * aInv;
        }
    }
    final double b;
    final double[] a;
    final int n;
    final double a0;
    private final boolean nonweighted;

    private LinearFunc(double b, double[] a) {
        if (a == null)
            throw new NullPointerException("Null a argument");
        this.b = b == -0.0 ? +0.0 : b; // replacing -0.0 with +0.0 for stable results
        this.n = a.length;
        this.a0 = a.length == 0 ? Double.NaN : a[0];
        boolean eq = true;
        for (int k = 1; k < a.length; k++) {
            if (a[k] != a[0])
                eq = false;
        }
        this.nonweighted = eq;
        this.a = eq && a.length > 3 ? null : a.clone();
    }

    private LinearFunc(double b, double a, int n) {
        assert n >= 3;
        this.b = b == -0.0 ? +0.0 : b; // replacing -0.0 with +0.0 for stable results
        this.n = n;
        this.a0 = a;
        this.nonweighted = true;
        this.a =  null;
    }

    /**
     * Returns an instance of this class, describing the linear function with specified coefficients:
     * <i>b</i> + <i>a</i><sub>0</sub><i>x</i><sub>0</sub> + <i>a</i><sub>1</sub><i>x</i><sub>1</sub>
     * +...+ <i>a</i><sub><i>n</i>-1</sub><i>x</i><sub><i>n</i>-1</sub>.
     *
     * <p>The passed reference <tt>a</tt> is not maintained by the created instance:
     * if necessary, the Java array is cloned.
     *
     * @param b the <i>b</i> coefficient.
     * @param a the <i>a</i> coefficients.
     * @return  the linear function with the given coefficients.
     */
    public static LinearFunc getInstance(double b, double ...a) {
        if (a.length == 0) {
            return new LinearFunc(b, a) {
                public double get(double... x) {
                    return this.b;
                }

                public double get() {
                    return this.b;
                }

                public double get(double x0) {
                    return this.b;
                }

                public double get(double x0, double x1) {
                    return this.b;
                }

                public double get(double x0, double x1, double x2) {
                    return this.b;
                }

                public double get(double x0, double x1, double x2, double x3) {
                    return this.b;
                }
            };
        } else if (a.length == 1) {
            if (a[0] == 1.0) {
                return new LinearFunc(b, a) {
                    public strictfp double get(double... x) {
                        return this.b + x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.b + x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.b + x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.b + x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.b + x0;
                    }
                };
            } else {
                return new LinearFunc(b, a) {
                    public strictfp double get(double... x) {
                        return this.b + this.a[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.b + this.a[0] * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.b + this.a[0] * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.b + this.a[0] * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.b + this.a[0] * x0;
                    }
                };
            }
        } else if (a.length == 2) {
            return new LinearFunc(b, a) {
                public double get(double... x) {
                    return this.b + this.a[0] * x[0] + this.a[1] * x[1];
                }

                public double get() {
                    throw new IndexOutOfBoundsException("At least 2 arguments required");
                }

                public double get(double x0) {
                    throw new IndexOutOfBoundsException("At least 2 arguments required");
                }

                public strictfp double get(double x0, double x1) {
                    return this.b + this.a[0] * x0 + this.a[1] * x1;
                }

                public strictfp double get(double x0, double x1, double x2) {
                    return this.b + this.a[0] * x0 + this.a[1] * x1;
                }

                public strictfp double get(double x0, double x1, double x2, double x3) {
                    return this.b + this.a[0] * x0 + this.a[1] * x1;
                }
            };
        } else {
            assert a.length >= 3;
            boolean eq = true;
            for (int k = 1; k < a.length; k++) {
                if (a[k] != a[0])
                    eq = false;
            }
            if (eq) {
                return getNonweightedInstance(b, a[0], a.length);
            }
            return new LinearFunc(b, a) {
                public strictfp double get(double ...x) {
                    double result = this.b;
                    for (int k = 0; k < this.n; k++)
                        result += this.a[k] * x[k];
                    return result;
                }

                public double get() {
                    throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                }

                public double get(double x0) {
                    throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                }

                public double get(double x0, double x1) {
                    throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                }

                public strictfp double get(double x0, double x1, double x2) {
                    if (this.n > 3)
                        throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                    return this.b + this.a[0] * x0 + this.a[1] * x1 + this.a[2] * x2;
                }

                public strictfp double get(double x0, double x1, double x2, double x3) {
                    if (this.n > 4)
                        throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                    return this.b + this.a[0] * x0 + this.a[1] * x1 + this.a[2] * x2 + this.a[3] * x3;
                }
            };
        }
    }


    /**
     * Returns an instance of this class, describing the linear function with the specified <i>b</i>
     * and the specified number (<i>n</i>) of equal coefficients <i>a</i><sub><i>i</i></sub>:
     * <i>b</i> + <i>a</i>(<i>x</i><sub>0</sub> + <i>x</i><sub>1</sub> +...+ <i>x</i><sub><i>n</i>-1</sub>).
     *
     * @param b the <i>b</i> coefficient.
     * @param a the common value of all <i>a</i><sub><i>i</i></sub> coefficients.
     * @param n the number of <i>a</i><sub><i>i</i></sub> coefficients.
     * @return  the linear function with the given coefficients.
     */
    public static LinearFunc getNonweightedInstance(double b, double a, int n) {
        if (n < 0)
            throw new IllegalArgumentException("Negative n argument");
        switch (n) {
            case 0:
                return getInstance(b);
            case 1:
                return getInstance(b, a);
            case 2:
                return getInstance(b, a, a);
            default: {
                assert n >= 3;
                return new LinearFunc(b, a, n) {
                    public strictfp double get(double ...x) {
                        double sum = 0.0;
                        for (int k = 0; k < this.n; k++)
                            sum += x[k];
                        return this.b + this.a0 * sum;
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                    }

                    public double get(double x0) {
                        throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                    }

                    public double get(double x0, double x1) {
                        throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        if (this.n > 3)
                            throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                        return this.b + this.a0 * (x0 + x1 + x2);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        if (this.n > 4)
                            throw new IndexOutOfBoundsException("At least " + this.n + " arguments required");
                        return this.b + this.a0 * (x0 + x1 + x2 + x3);
                    }
                };
            }
        }
    }

    /**
     * Equivalent to {@link #getNonweightedInstance(double, double, int) getNonweightedInstance(0.0, 1.0/n, n)}:
     * the average from <tt>n</tt> numbers.
     *
     * @param n the number of <i>a</i><sub><i>i</i></sub> coefficients.
     * @return  the function calculating average from <tt>n</tt> numbers.
     */
    public static LinearFunc getAveragingInstance(int n) {
        return getNonweightedInstance(0.0, 1.0 / n, n);
    }

    /**
     * Returns an instance of this class describing the following linear function with one argument:
     * <i><i>d</i><sub>min</sub> + (<i>d</i><sub>max</sub>-<i>d</i><sub>min</sub>) *
     * (x</i>-<i>s</i><sub>min</sub>) / (<i>s</i><sub>max</sub>-<i>s</i><sub>min</sub>),
     * where <i>s</i><sub>min</sub>..<i>s</i><sub>max</sub> is <tt>srcRange</tt>
     * and <i>d</i><sub>min</sub>..<i>d</i><sub>max</sub> is <tt>destRange</tt>.
     * This function maps the source range <tt>srcRange</tt>
     * to the destination range <tt>destRange</tt>,
     * excepting the only case when <tt>srcRange.{@link Range#size() size()}==0</tt>.
     * In that special case the behavior of the returned function is not specified
     * (but no exceptions are thrown).
     *
     * @param destRange the destination range.
     * @param srcRange  the source range.
     * @return          the linear function mapping the source range to the destination range.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     */
    public static strictfp LinearFunc getInstance(Range destRange, Range srcRange) {
        double mult = destRange.size() / srcRange.size();
        double b = destRange.min() - srcRange.min() * mult;
        return getInstance(b, mult);
    }

    /**
     * Returns an instance of the updatable version of this class,
     * describing the linear function with specified coefficients:
     * <i>b</i> + <i>a</i><i>x</i><sub>0</sub>.</p>
     * The {@link Func.Updatable#set set} method of this instance sets
     * <tt>x[0]=(newResult-b)*aInv</tt>, where <tt>aInv=1.0/a</tt>
     * is calculated while the instance creation.
     *
     * @param b the <i>b</i> coefficient.
     * @param a the <i>a</i> coefficient.
     * @return               the updatable linear function with the given coefficients.
     */
    public static Updatable getUpdatableInstance(double b, double a) {
        return new Updatable(b, a);
    }

    public abstract double get(double ...x);

    public abstract double get();

    public abstract double get(double x0);

    public abstract double get(double x0, double x1);

    public abstract double get(double x0, double x1, double x2);

    public abstract double get(double x0, double x1, double x2, double x3);

    /**
     * Returns the number of <i>a</i><sub><i>i</i></sub> coefficients.

     * @return the number of argument of this function.
     */
    public int n() {
        return n;
    }

    /**
     * Returns <i>b</i> coefficient of this linear function.
     *
     * @return <i>b</i> coefficient.
     */
    public double b() {
        return b;
    }

    /**
     * Returns <i>a</i><sub><i>i</i></sub> coefficient of this linear function.
     *
     * @param i the index of the coefficient.
     * @return  <i>a</i><sub><i>i</i></sub> coefficient.
     * @throws IndexOutOfBoundsException if the given index is negative or &gt;={@link #n()}
     */
    public double a(int i) {
        if (a != null) {
            return(a[i]);
        } else {
            if (i < 0 || i >= n)
                throw new IndexOutOfBoundsException("Index (" + i + ") is out of bounds 0.." + (n - 1));
            return a0;
        }
    }

    /**
     * Returns an array containing all <i>a</i><sub><i>i</i></sub> coefficients of this linear function.
     *
     * <p>If {@link #isNonweighted()} method returns <tt>true</tt>, it can be more efficient, to save memory,
     * not to use this method, but to get the common value of all coefficients via {@link #a(int) a(0)} call
     * (please not forget to check that <tt>{@link #n()}&gt;0</tt>).
     *
     * <p>The returned array is never a reference to an internal array stored in this object:
     * if necessary, the internal Java array is cloned.
     *
     * @return all <i>a</i><sub><i>i</i></sub> coefficients.
     */
    public double[] a() {
        double[] result = new double[this.n];
        if (a != null) {
            System.arraycopy(a, 0, result, 0, n);
        } else {
            for (int k = 0; k < n; k++)
                result[k] = a0;
        }
        return result;
    }

    /**
     * Returns <tt>true</tt> if <tt>{@link #n() n()}&lt;=1</tt> or
     * if all <i>a</i><sub><i>i</i></sub> coefficients are equal.
     * This function works little faster in this case, because it can be simplified as
     * <i>b</i> + <i>a</i><sub>0</sub>(<i>x</i><sub>0</sub> + <i>x</i><sub>1</sub>
     * +...+ <i>x</i><sub><i>n</i>-1</sub>).
     *
     * @return <tt>true</tt> if <tt>{@link #n() n()}&lt;=1</tt> or
     *         if all <i>a</i><sub><i>i</i></sub> coefficients are equal.
     */
    public boolean isNonweighted() {
        return nonweighted;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("linear function f(");
        for (int k = 0; k < n; k++) {
            if (k > 0)
                sb.append(",");
            if (k >= 2 && k < n - 2) {
                sb.append("...");
                k = n - 2;
            } else {
                sb.append("x").append(k);
            }
        }
        sb.append(")=");
        if (n > 1 && nonweighted) {
            sb.append(goodFormat(a0)).append("*(x0+x1+...)");
        } else {
            for (int k = 0; k < a.length; k++) {
                if (k > 0 && a[k] >= 0.0)
                    sb.append("+");
                sb.append(goodFormat(a[k])).append("*x").append(k);
            }
        }
        if (b != 0.0) {
            if (b >= 0.0)
                sb.append("+");
            sb.append(goodFormat(b));
        }
        return sb.toString();
    }

    static String goodFormat(double v) {
        return String.format(Locale.US, Math.abs(v) >= 0.1 && Math.abs(v) <= 1e6 ? "%.3f" : "%.3g", v);
    }
}
