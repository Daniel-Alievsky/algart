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

import java.util.Objects;

/**
 * <p>A function, transformed by  {@link ApertureFilterOperator}
 * in <i>n</i>-dimensional Euclidean space.
 * It is built on the base of some <i>parent</i> function <i>f</i> and
 * some operator {@link ApertureFilterOperator} by its
 * {@link ApertureFilterOperator#apply(Func) apply} method.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see ApertureFilterOperator#apply(Func)
 */
public class ApertureFilteredFunc implements Func {
    private static final boolean OPTIMIZE_LITTLE_DIMENSIONS = true;

    final Func parent;
    final ApertureFilterOperator operator;

    private ApertureFilteredFunc(Func parent, ApertureFilterOperator operator) {
        this.parent = parent;
        this.operator = operator;
    }

    /**
     * Returns an instance of this class for the given parent function and operator.
     * Equivalent to <tt>{@link ApertureFilterOperator#apply(Func) operator.apply}(parent)</tt>.
     *
     * @param parent   the parent function.
     * @param operator the operator, transforming this function.
     * @return         new function.
     * @throws NullPointerException if one of the arguments is {@code null}.
     */
    public static ApertureFilteredFunc getInstance(Func parent, ApertureFilterOperator operator) {
        Objects.requireNonNull(parent, "Null parent function");
        Objects.requireNonNull(operator, "Null operator");
        if (OPTIMIZE_LITTLE_DIMENSIONS) {
            if (operator.apertureDim.length == 1) {
                final double step0 = operator.apertureSteps[0];
                final int n0 = (int)operator.apertureDim[0];
                if (operator.isNonweightedSum) {
                    return new ApertureFilteredFunc(parent, operator) {
                        @Override
                        public double get(double... x) {
                            if (x.length != 1) {
                                return super.get(x);
                            }
                            double sum = 0.0;
                            double px0 = x[0] + operator.apertureFrom[0];
                            for (int i = 0; i < n0; i++, px0 += step0) {
                                sum += parent.get(px0);
                            }
                            return operator.a * sum + operator.b;
                        }

                        @Override
                        public double get(double x0) {
                            double sum = 0.0;
                            for (int i = 0; i < n0; i++, x0 += step0) {
                                sum += parent.get(x0);
                            }
                            return operator.a * sum + operator.b;
                        }
                    };
                } else {
                    return new ApertureFilteredFunc(parent, operator) {
                        @Override
                        public double get(double... x) {
                            if (x.length < 1) {
                                return super.get(x);
                            }
                            double[] probes = new double[operator.totalCount];
                            double px0 = x[0] + operator.apertureFrom[0];
                            int pointIndex = 0;
                            for (int i = 0; i < n0; i++, px0 += step0) {
                                probes[pointIndex++] = parent.get(px0);
                            }
                            return operator.apertureFunc().get(probes);
                        }
                    };
                }
            } else if (operator.apertureDim.length == 2) {
                final double step0 = operator.apertureSteps[0];
                final int n0 = (int)operator.apertureDim[0];
                final double step1 = operator.apertureSteps[1];
                final int n1 = (int)operator.apertureDim[1];
                if (operator.isNonweightedSum) {
                    return new ApertureFilteredFunc(parent, operator) {
                        @Override
                        public double get(double... x) {
                            if (x.length != 2) {
                                return super.get(x);
                            }
                            double sum = 0.0;
                            double px0 = x[0] + operator.apertureFrom[0];
                            for (int i = 0; i < n0; i++, px0 += step0) {
                                double px1 = x[1] + operator.apertureFrom[1];
                                for (int j = 0; j < n1; j++, px1 += step1) {
                                    sum += parent.get(px0, px1);
                                }
                            }
                            return operator.a * sum + operator.b;
                        }

                        @Override
                        public double get(double x0, double x1) {
                            double sum = 0.0;
                            for (int i = 0; i < n0; i++, x0 += step0) {
                                double px1 = x1 + operator.apertureFrom[1];
                                for (int j = 0; j < n1; j++, px1 += step1) {
                                    sum += parent.get(x0, px1);
                                }
                            }
                            return operator.a * sum + operator.b;
                        }
                    };
                } else {
                    return new ApertureFilteredFunc(parent, operator) {
                        @Override
                        public double get(double... x) {
                            if (x.length < 2) {
                                return super.get(x);
                            }
                            double[] probes = new double[operator.totalCount];
                            double px0 = x[0] + operator.apertureFrom[0];
                            int pointIndex = 0;
                            for (int i = 0; i < n0; i++, px0 += step0) {
                                double px1 = x[1] + operator.apertureFrom[1];
                                for (int j = 0; j < n1; j++, px1 += step1) {
                                    probes[pointIndex++] = parent.get(px0, px1);
                                }
                            }
                            return operator.apertureFunc().get(probes);
                        }
                    };
                }
            } else if (operator.apertureDim.length == 3) {
                final double step0 = operator.apertureSteps[0];
                final int n0 = (int)operator.apertureDim[0];
                final double step1 = operator.apertureSteps[1];
                final int n1 = (int)operator.apertureDim[1];
                final double step2 = operator.apertureSteps[2];
                final int n2 = (int)operator.apertureDim[2];
                if (operator.isNonweightedSum) {
                    return new ApertureFilteredFunc(parent, operator) {
                        @Override
                        public double get(double... x) {
                            if (x.length != 3) {
                                return super.get(x);
                            }
                            double sum = 0.0;
                            double px0 = x[0] + operator.apertureFrom[0];
                            for (int i = 0; i < n0; i++, px0 += step0) {
                                double px1 = x[1] + operator.apertureFrom[1];
                                for (int j = 0; j < n1; j++, px1 += step1) {
                                    double px2 = x[2] + operator.apertureFrom[2];
                                    for (int k = 0; k < n2; k++, px2 += step2) {
                                        sum += parent.get(px0, px1, px2);
                                    }
                                }
                            }
                            return operator.a * sum + operator.b;
                        }

                        @Override
                        public double get(double x0, double x1, double x2) {
                            double sum = 0.0;
                            for (int i = 0; i < n0; i++, x0 += step0) {
                                double px1 = x1 + operator.apertureFrom[1];
                                for (int j = 0; j < n1; j++, px1 += step1) {
                                    double px2 = x2 + operator.apertureFrom[2];
                                    for (int k = 0; k < n2; k++, px2 += step2) {
                                        sum += parent.get(x0, px1, px2);
                                    }
                                }
                            }
                            return operator.a * sum + operator.b;
                        }
                    };
                } else {
                    return new ApertureFilteredFunc(parent, operator) {
                        @Override
                        public double get(double... x) {
                            if (x.length < 3) {
                                return super.get(x);
                            }
                            double[] probes = new double[operator.totalCount];
                            double px0 = x[0] + operator.apertureFrom[0];
                            int pointIndex = 0;
                            for (int i = 0; i < n0; i++, px0 += step0) {
                                double px1 = x[1] + operator.apertureFrom[1];
                                for (int j = 0; j < n1; j++, px1 += step1) {
                                    double px2 = x[2] + operator.apertureFrom[2];
                                    for (int k = 0; k < n2; k++, px2 += step2) {
                                        probes[pointIndex++] = parent.get(px0, px1, px2);
                                    }
                                }
                            }
                            return operator.apertureFunc().get(probes);
                        }
                    };
                }
            }
        }
        return new ApertureFilteredFunc(parent, operator);
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
    public ApertureFilterOperator operator() {
        return operator;
    }

    public double get(double... x) {
        double[] probes = new double[operator.totalCount];
        fillProbes(x, x.clone(), probes, 0, 0);
        return operator.apertureFunc().get(probes);
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

    private int fillProbes(double[] x, double[] coordinates, double[] probes, int filledCount, int pointIndex) {
        assert coordinates.length == x.length;
        final double step = operator.apertureSteps[filledCount];
        final int n = (int)operator.apertureDim[filledCount];
        if (filledCount < coordinates.length) {
            coordinates[filledCount] = x[filledCount] + operator.apertureFrom[filledCount];
            if (filledCount + 1 == operator.apertureDim.length) {
                for (int k = 0; k < n; k++, coordinates[filledCount] += step) {
                    probes[pointIndex++] = parent.get(coordinates);
                }
            } else {
                for (int k = 0; k < n; k++, coordinates[filledCount] += step) {
                    pointIndex = fillProbes(x, coordinates, probes, filledCount + 1, pointIndex);
                }
            }
        } else { // do not change extra coordinates: simple repeat calling parent.get. It is an unusual case
            for (int k = 0; k < operator.apertureDim[filledCount]; k++) {
                if (filledCount + 1 == operator.apertureDim.length) {
                    probes[pointIndex++] = parent.get(coordinates);
                } else {
                    pointIndex = fillProbes(x, coordinates, probes, filledCount + 1, pointIndex);
                }
            }
        }
        return pointIndex;
    }

}
