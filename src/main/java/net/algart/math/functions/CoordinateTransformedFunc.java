/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>A function, the arguments of which are transformed by some
 * {@link CoordinateTransformationOperator coordinate transformation operator}
 * in <i>n</i>-dimensional Euclidean space.
 * It is built on the base of some <i>parent</i> function <i>f</i> and
 * some {@link CoordinateTransformationOperator coordinate transformation operator} <i>O</i>.
 * This function <i>g</i> is the following:
 * <nobr><i>g</i>(<b>x</b>) = <i>O</i>&nbsp;<i>g</i>(<b>x</b>) = <i>f</i>(<b>map</b>(<b>x</b>)),</nobr>
 * <nobr><b>x</b> = <i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub></nobr>,
 * <b>map</b>(<b>x</b>) is the result of applying {@link CoordinateTransformationOperator#map} method
 * to <b>x</b> array.
 * In other words, the basic {@link #get(double[] x)} method of this instance is equivalent to
 * the following code:</p>
 *
 * <pre>
 * double[] y = new double[x.length];
 * operator.{@link CoordinateTransformationOperator#map(double[], double[]) map}(y, x);
 * return parent.{@link Func#get(double[]) get}(y);
 * </pre>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @see CoordinateTransformationOperator#apply(Func)
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class CoordinateTransformedFunc implements Func {
    final Func parent;
    final CoordinateTransformationOperator operator;

    private CoordinateTransformedFunc(Func parent, CoordinateTransformationOperator operator) {
        if (parent == null)
            throw new NullPointerException("Null parent function");
        if (operator == null)
            throw new NullPointerException("Null operator");
        this.parent = parent;
        this.operator = operator;
    }

    /**
     * Returns an instance of this class for the given parent function and operator.
     * (If <tt>operator</tt> is {@link Operator#IDENTITY} or equivalent, this method may return
     * the <tt>parent</tt> argument.)
     *
     * <p>The result is almost always an instance of this class, but there is an exception: if <tt>operator</tt>
     * is an identity operator, which does not change the passed parent function at all, this method
     * can just return <tt>parent</tt> argument.
     *
     * @param parent   the parent function.
     * @param operator the operator, transforming the arguments of this function before passing them to the parent one.
     * @return         new function.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     */
    public static Func getInstance(Func parent, CoordinateTransformationOperator operator) {
        if (operator instanceof LinearOperator) {
            LinearOperator lo = (LinearOperator)operator;
            if (lo.isShift() && lo.isZeroB()) {
                return parent;
            }
            int dimCount = lo.n();
            final double[] b = lo.b();
            if (dimCount == 1) {
                final double[] a = lo.a();
                return new CoordinateTransformedFunc(parent, operator) {
                    @Override
                    public strictfp double get(double x0) {
                        double y0 = a[0] * x0 + b[0];
                        return parent.get(y0);
                    }
                };
            } else if (dimCount == 2) {
                if (lo.isDiagonal()) {
                    final double[] diagonal = lo.diagonal();
                    if (b[0] == 0.0 && b[1] == 0.0) {
                        return new CoordinateTransformedFunc(parent, operator) {
                            @Override
                            public strictfp double get(double x0, double x1) {
                                return parent.get(diagonal[0] * x0, diagonal[1] * x1);
                            }
                        };
                    } else {
                        return new CoordinateTransformedFunc(parent, operator) {
                            @Override
                            public strictfp double get(double x0, double x1) {
                                double y0 = diagonal[0] * x0 + b[0];
                                double y1 = diagonal[1] * x1 + b[1];
                                return parent.get(y0, y1);
                            }
                        };
                    }
                } else {
                    final double[] a = lo.a();
                    return new CoordinateTransformedFunc(parent, operator) {
                        @Override
                        public double get(double x0, double x1) {
                            double y0 = a[0] * x0 + a[1] * x1 + b[0];
                            double y1 = a[2] * x0 + a[3] * x1 + b[1];
                            return parent.get(y0, y1);
                        }
                    };
                }
            } else if (dimCount == 3) {
                if (lo.isDiagonal()) {
                    final double[] diagonal = lo.diagonal();
                    if (b[0] == 0.0 && b[1] == 0.0 && b[2] == 0.0) {
                        return new CoordinateTransformedFunc(parent, operator) {
                            @Override
                            public strictfp double get(double x0, double x1, double x2) {
                                return parent.get(diagonal[0] * x0, diagonal[1] * x1, diagonal[2] * x2);
                            }
                        };
                    } else {
                        return new CoordinateTransformedFunc(parent, operator) {
                            @Override
                            public strictfp double get(double x0, double x1, double x2) {
                                double y0 = diagonal[0] * x0 + b[0];
                                double y1 = diagonal[1] * x1 + b[1];
                                double y2 = diagonal[2] * x2 + b[2];
                                return parent.get(y0, y1, y2);
                            }
                        };
                    }
                } else {
                    final double[] a = lo.a();
                    return new CoordinateTransformedFunc(parent, operator) {
                        @Override
                        public strictfp double get(double x0, double x1, double x2) {
                            double y0 = a[0] * x0 + a[1] * x1 + a[2] * x2 + b[0];
                            double y1 = a[3] * x0 + a[4] * x1 + a[5] * x2 + b[1];
                            double y2 = a[6] * x0 + a[7] * x1 + a[8] * x2 + b[2];
                            return parent.get(y0, y1, y2);
                        }
                    };
                }
            }
        }
        if (operator instanceof ProjectiveOperator) {
            ProjectiveOperator po = (ProjectiveOperator)operator;
            int dimCount = po.n();
            if (po.isShift() && po.isZeroB()) {
                throw new AssertionError("Identity operator must be " + LinearOperator.class);
            }
            final double[] a = po.a();
            final double[] b = po.b();
            final double[] c = po.c();
            final double d = po.d();
            assert po.n() ==  b.length && b.length == c.length && a.length == b.length * b.length;
            if (dimCount == 1) {
                return new CoordinateTransformedFunc(parent, operator) {
                    @Override
                    public strictfp double get(double x0) {
                        double y0 = (a[0] * x0 + b[0]) / (c[0] * x0 + d);
                        return parent.get(y0);
                    }
                };
            } else if (dimCount == 2) {
                return new CoordinateTransformedFunc(parent, operator) {
                    @Override
                    public double get(double x0, double x1) {
                        double multiplier = 1.0 / (c[0] * x0 + c[1] * x1 + d);
                        double y0 = (a[0] * x0 + a[1] * x1 + b[0]) * multiplier;
                        double y1 = (a[2] * x0 + a[3] * x1 + b[1]) * multiplier;
                        return parent.get(y0, y1);
                    }
                };
            } else if (dimCount == 3) {
                return new CoordinateTransformedFunc(parent, operator) {
                    @Override
                    public strictfp double get(double x0, double x1, double x2) {
                        double multiplier = 1.0 / (c[0] * x0 + c[1] * x1 + c[2] * x2 + d);
                        double y0 = (a[0] * x0 + a[1] * x1 + a[2] * x2 + b[0]) * multiplier;
                        double y1 = (a[3] * x0 + a[4] * x1 + a[5] * x2 + b[1]) * multiplier;
                        double y2 = (a[6] * x0 + a[7] * x1 + a[8] * x2 + b[2]) * multiplier;
                        return parent.get(y0, y1, y2);
                    }
                };
            }
        }
        return new CoordinateTransformedFunc(parent, operator);
    }

    /**
     * Returns the parent function of this one: the first argument of {@link #getInstance getInstance} method.
     *
     * @return the parent function of this one.
     */
    public Func parent() {
        return parent;
    }

    /**
     * Returns the operator, used while building this transformed function:
     * the second argument of {@link #getInstance getInstance} method.
     *
     * @return the operator, used while building this transformed function.
     */
    public CoordinateTransformationOperator operator() {
        return operator;
    }

    public double get(double... x) {
        double[] y = new double[x.length];
        operator.map(y, x);
        return parent.get(y);
    }

    public double get() {
        return parent.get();
    }

    public double get(double x0) {
        return get(new double[] {x0});
    }

    public double get(double x0, double x1) {
        return get(new double[] {x0, x1});
    }

    public double get(double x0, double x1, double x2) {
        return get(new double[] {x0, x1, x2});
    }

    public double get(double x0, double x1, double x2, double x3) {
        return get(new double[] {x0, x1, x2, x3});
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return parent + " transformed by " + operator;
    }
}
