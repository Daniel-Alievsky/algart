/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Power function:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) =
 * <i>scale</i>*<i>x</i><sub>0</sub><sup><i>x</i><sub>1</sub></sup> or
 * <i>f</i>(<i>x</i><sub>0</sub>) =
 * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>c</sup></i>,
 * where <i>c</i> and <i>scale</i> are constants.
 * More precisely, the result of this function is:
 * <ul>
 * <li>
 * <tt>scale * Math.pow(x[0], x[1])</tt> or <tt>scale * Math.pow(x[0], c)</tt>
 * for the instance returned by {@link #getBinaryInstance(double scale)} or
 * {@link #getInstance(double c, double scale)} methods;</li>
 * <li><tt>scale * StrictMath.pow(x[0], x[1])</tt> or <tt>scale * StrictMath.pow(x[0], c)</tt>
 * for the instance returned by {@link #getStrictBinaryInstance(double scale)} or
 * {@link #getStrictInstance(double c, double scale)} methods.</li>
 * </ul>
 * The {@link #get} method of the instance of this class requires at least 2 or 1 arguments
 * (for the cases <i>x</i><sub>0</sub><i><sup>x<sub>1</sub></sup></i> and
 * <i>x</i><sub>0</sub><sup>c</sup> correspondingly)
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is less.
 * In a case of using {@link #getStrictBinaryInstance(double scale)} /
 * {@link #getStrictInstance(double c, double scale)} methods,
 * all calculations are performed in <tt>strictfp</tt> mode, so the result
 * is absolutely identical on all platforms.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class PowerFunc implements Func {
    /**
     * <p>{@link Func.Updatable Updatable extension} of the {@link PowerFunc power function}
     * with one argument.</p>
     */
    public static abstract class Updatable extends PowerFunc implements Func.Updatable {
        final double scaleInv;
        private Updatable(Double c, double scale) {
            super(c, scale);
            scaleInv = 1.0 / scale;
        }

        public abstract void set(double[] x, double newResult);

        /**
         * Returns a brief string description of this object.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return "updatable " + super.toString();
        }
    }

    private final boolean binary;
    final double c;
    final double scale;

    private PowerFunc(Double c, double scale) {
        this.binary = c != null;
        this.c = c == null ? 157.0 : c;
        this.scale = scale;
    }

    /**
     * Returns an instance of this class, a case of binary function
     * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>x<sub>1</sub></sup></i>, using <tt>Math.pow</tt> method.</p>
     *
     * @param scale the constant <i>scale</i>.
     * @return an instance of this class, a case of binary function, using <tt>Math.pow</tt> method.</p>
     */
    public static PowerFunc getBinaryInstance(double scale) {
        return new PowerFunc(null, scale) {
            public double get(double... x) {
                return this.scale * Math.pow(x[0], x[1]);
            }

            public double get() {
                throw new IndexOutOfBoundsException("At least 2 arguments required");
            }

            public double get(double x0) {
                throw new IndexOutOfBoundsException("At least 2 arguments required");
            }

            public double get(double x0, double x1) {
                return this.scale * Math.pow(x0, x1);
            }

            public double get(double x0, double x1, double x2) {
                return this.scale * Math.pow(x0, x1);
            }

            public double get(double x0, double x1, double x2, double x3) {
                return this.scale * Math.pow(x0, x1);
            }
        };
    }

    /**
     * Returns an instance of this class, a case of binary function
     * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>x<sub>1</sub></sup></i>, using <tt>StrictMath.pow</tt> method.</p>
     *
     * @param scale the constant <i>scale</i>.
     * @return      an instance of this class, a case of binary function, using <tt>StrictMath.pow</tt> method.</p>
     */
    public static PowerFunc getStrictBinaryInstance(double scale) {
        return new PowerFunc(null, scale) {
            public strictfp double get(double... x) {
                return this.scale * StrictMath.pow(x[0], x[1]);
            }

            public double get() {
                throw new IndexOutOfBoundsException("At least 2 arguments required");
            }

            public double get(double x0) {
                throw new IndexOutOfBoundsException("At least 2 arguments required");
            }

            public strictfp double get(double x0, double x1) {
                return this.scale * StrictMath.pow(x0, x1);
            }

            public strictfp double get(double x0, double x1, double x2) {
                return this.scale * StrictMath.pow(x0, x1);
            }

            public strictfp double get(double x0, double x1, double x2, double x3) {
                return this.scale * StrictMath.pow(x0, x1);
            }
        };
    }

    /**
     * Returns an instance of this class, a case of unary function
     * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>c</sup></i>, using <tt>Math.pow</tt> method.</p>
     *
     * <p>This method returns special optimized versions of this class for several popular values of
     * <tt>c</tt>argument. In particular, there are optimized version
     * for <nobr><tt>c = 1.0, 2.0, 3.0, 0.5, 1.0/3.0</tt></nobr>.
     *
     * @param c     the constant <i>c</i>.
     * @param scale the constant <i>scale</i>.
     * @return      an instance of this class, a case of unary function, using <tt>Math.pow</tt> method.</p>
     */
    public static PowerFunc getInstance(double c, double scale) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getStrictUpdatableInstance)
        //  Updatable\((\w+\,\s*\w+)\) ==> PowerFunc($1) ;;
        //  \s+public\s+strictfp\s+void\s+set\(.*?[\r\n]\s*} ==> ;;
        //  private\s+final\s+double\s+cInv.*?[\r\n]\s*(public) ==> $1 ;;
        //  strictfp\s+ ==> ;;
        //  StrictMath ==> Math    !! Auto-generated: NOT EDIT !! ]]
        if (c == 1.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return x0;
                    }

                    public double get(double x0, double x1) {
                        return x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return x0;
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return this.scale * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * x0;
                    }

                    public double get(double x0, double x1) {
                        return this.scale * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0;
                    }
                };
            }
        } else if (c == 2.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0;
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return this.scale * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return this.scale * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0;
                    }
                };
            }
        } else if (c == 3.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return x0 * x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0 * x0;
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return this.scale * x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0 * x0;
                    }
                };
            }
        } else if (c == 0.5) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return Math.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return Math.sqrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return Math.sqrt(x0);
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return this.scale * Math.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * Math.sqrt(x0);
                    }
                };
            }
        } else if (c == 1.0 / 3.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return Math.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return Math.cbrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return Math.cbrt(x0);
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return this.scale * Math.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * Math.cbrt(x0);
                    }
                };
            }
        } else {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return Math.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1) {
                        return Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2) {
                        return Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return Math.pow(x0, this.c);
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public double get(double... x) {
                        return this.scale * Math.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * Math.pow(x0, this.c);
                    }
                };
            }
        }
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Returns an instance of this class, a case of unary function
     * <i>x</i><sub>0</sub><i><sup>c</sup></i>, using <tt>Math.pow</tt> method.</p>
     * Equivalent to <tt>{@link #getInstance(double, double) getInstance}(c,1.0)</tt>.
     *
     * @param c  the constant <i>c</i>.
     * @return   an instance of this class, a case of unary function, using <tt>Math.pow</tt> method.</p>
     */
    public static PowerFunc getInstance(double c) {
        return getInstance(c, 1.0);
    }

    /**
     * Returns an instance of this class, a case of unary function
     * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>c</sup></i>, using <tt>StrictMath.pow</tt> method.</p>
     *
     * <p>This method returns special optimized versions of this class for several popular values of
     * <tt>c</tt>argument. In particular, there are optimized version
     * for <nobr><tt>c = 1.0, 2.0, 3.0, 0.5, 1.0/3.0</tt></nobr>.
     *
     * @param c     the constant <i>c</i>.
     * @param scale the constant <i>scale</i>.
     * @return      an instance of this class, a case of unary function, using <tt>StrictMath.pow</tt> method.</p>
     */
    public static PowerFunc getStrictInstance(double c, double scale) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getStrictUpdatableInstance)
        //  Updatable\((\w+\,\s*\w+)\) ==> PowerFunc($1) ;;
        //  \s+public\s+strictfp\s+void\s+set\(.*?[\r\n]\s*} ==> ;;
        //  private\s+final\s+double\s+cInv.*?[\r\n]\s*(public) ==> $1   !! Auto-generated: NOT EDIT !! ]]
        if (c == 1.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return x0;
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0;
                    }
                };
            }
        } else if (c == 2.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0;
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0;
                    }
                };
            }
        } else if (c == 3.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0 * x0;
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0 * x0;
                    }
                };
            }
        } else if (c == 0.5) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return StrictMath.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return StrictMath.sqrt(x0);
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * StrictMath.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * StrictMath.sqrt(x0);
                    }
                };
            }
        } else if (c == 1.0 / 3.0) {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return StrictMath.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return StrictMath.cbrt(x0);
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * StrictMath.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * StrictMath.cbrt(x0);
                    }
                };
            }
        } else {
            if (scale == 1.0) {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return StrictMath.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return StrictMath.pow(x0, this.c);
                    }
                };
            } else {
                return new PowerFunc(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * StrictMath.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }
                };
            }
        }
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Returns an instance of this class, a case of unary function
     * <i>x</i><sub>0</sub><i><sup>c</sup></i>, using <tt>StrictMath.pow</tt> method.</p>
     * Equivalent to <tt>{@link #getStrictInstance(double, double) getStrictInstance}(c,1.0)</tt>.
     *
     * @param c  the constant <i>c</i>.
     * @return   an instance of this class, a case of unary function, using <tt>StrictMath.pow</tt> method.</p>
     */
    public static PowerFunc getStrictInstance(double c) {
        return getStrictInstance(c, 1.0);
    }

    /**
     * Returns an instance of the updatable version of this class, a case of unary function
     * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>c</sup></i>, using <tt>Math.pow</tt> method.</p>
     *
     * <p>This method returns special optimized versions of this class for several popular values of
     * <tt>c</tt>argument. In particular, there are optimized version
     * for <nobr><tt>c = 1.0, 2.0, 3.0, 0.5, 1.0/3.0</tt></nobr>.
     *
     * @param c     the constant <i>c</i>.
     * @param scale the constant <i>scale</i>.
     * @return      an instance of this class, a case of unary function, using <tt>Math.pow</tt> method.</p>
     */
    public static Updatable getUpdatableInstance(double c, double scale) {
        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getStrictUpdatableInstance)
        //  strictfp\s+ ==> ;;
        //  StrictMath ==> Math    !! Auto-generated: NOT EDIT !! ]]
        if (c == 1.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return x0;
                    }

                    public double get(double x0, double x1) {
                        return x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return x0;
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = newResult;
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return this.scale * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * x0;
                    }

                    public double get(double x0, double x1) {
                        return this.scale * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0;
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = this.scaleInv * newResult;
                    }
                };
            }
        } else if (c == 2.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0;
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = Math.sqrt(newResult);
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return this.scale * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return this.scale * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0;
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = Math.sqrt(this.scaleInv * newResult);
                    }
                };
            }
        } else if (c == 3.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return x0 * x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0 * x0;
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = Math.cbrt(newResult);
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return this.scale * x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public double get(double x0, double x1) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = Math.cbrt(this.scaleInv * newResult);
                    }
                };
            }
        } else if (c == 0.5) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return Math.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return Math.sqrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return Math.sqrt(x0);
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = newResult * newResult;
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return this.scale * Math.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * Math.sqrt(x0);
                    }

                    public void set(double[] x, double newResult) {
                        double temp = this.scaleInv * newResult;
                        x[0] = temp * temp;
                    }
                };
            }
        } else if (c == 1.0 / 3.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return Math.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return Math.cbrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return Math.cbrt(x0);
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = newResult * newResult * newResult;
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public double get(double... x) {
                        return this.scale * Math.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public double get(double x0, double x1) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * Math.cbrt(x0);
                    }

                    public void set(double[] x, double newResult) {
                        double temp = this.scaleInv * newResult;
                        x[0] = temp * temp * temp;
                    }
                };
            }
        } else {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    private final double cInv = 1.0 / this.c;

                    public double get(double... x) {
                        return Math.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1) {
                        return Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2) {
                        return Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return Math.pow(x0, this.c);
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = Math.pow(newResult, this.cInv);
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    private final double cInv = 1.0 / this.c;

                    public double get(double... x) {
                        return this.scale * Math.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public double get(double x0) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public double get(double x0, double x1, double x2, double x3) {
                        return this.scale * Math.pow(x0, this.c);
                    }

                    public void set(double[] x, double newResult) {
                        x[0] = Math.pow(this.scaleInv * newResult, this.cInv);
                    }
                };
            }
        }
        //[[Repeat.IncludeEnd]]
    }

    /**
     * Returns an instance of the updatable version of this class, a case of unary function
     * <i>scale</i>*<i>x</i><sub>0</sub><i><sup>c</sup></i>, using <tt>StrictMath.pow</tt> method.</p>
     *
     * <p>This method returns special optimized versions of this class for several popular values of
     * <tt>c</tt>argument. In particular, there are optimized version
     * for <nobr><tt>c = 1.0, 2.0, 3.0, 0.5, 1.0/3.0</tt></nobr>.
     *
     * @param c     the constant <i>c</i>.
     * @param scale the constant <i>scale</i>.
     * @return      an instance of this class, a case of unary function, using <tt>StrictMath.pow</tt> method.</p>
     */
    public static Updatable getUpdatableStrictInstance(double c, double scale) {
        //[[Repeat.SectionStart getStrictUpdatableInstance]]
        if (c == 1.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return x0;
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = newResult;
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0;
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = this.scaleInv * newResult;
                    }
                };
            }
        } else if (c == 2.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0;
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = StrictMath.sqrt(newResult);
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0;
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = StrictMath.sqrt(this.scaleInv * newResult);
                    }
                };
            }
        } else if (c == 3.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return x0 * x0 * x0;
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = StrictMath.cbrt(newResult);
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * x[0] * x[0] * x[0];
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * x0 * x0 * x0;
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = StrictMath.cbrt(this.scaleInv * newResult);
                    }
                };
            }
        } else if (c == 0.5) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return StrictMath.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return StrictMath.sqrt(x0);
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = newResult * newResult;
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * StrictMath.sqrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * StrictMath.sqrt(x0);
                    }

                    public strictfp void set(double[] x, double newResult) {
                        double temp = this.scaleInv * newResult;
                        x[0] = temp * temp;
                    }
                };
            }
        } else if (c == 1.0 / 3.0) {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return StrictMath.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return StrictMath.cbrt(x0);
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = newResult * newResult * newResult;
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    public strictfp double get(double... x) {
                        return this.scale * StrictMath.cbrt(x[0]);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * StrictMath.cbrt(x0);
                    }

                    public strictfp void set(double[] x, double newResult) {
                        double temp = this.scaleInv * newResult;
                        x[0] = temp * temp * temp;
                    }
                };
            }
        } else {
            if (scale == 1.0) {
                return new Updatable(c, scale) {
                    private final double cInv = 1.0 / this.c;

                    public strictfp double get(double... x) {
                        return StrictMath.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return StrictMath.pow(x0, this.c);
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = StrictMath.pow(newResult, this.cInv);
                    }
                };
            } else {
                return new Updatable(c, scale) {
                    private final double cInv = 1.0 / this.c;

                    public strictfp double get(double... x) {
                        return this.scale * StrictMath.pow(x[0], this.c);
                    }

                    public double get() {
                        throw new IndexOutOfBoundsException("At least 1 argument required");
                    }

                    public strictfp double get(double x0) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp double get(double x0, double x1, double x2, double x3) {
                        return this.scale * StrictMath.pow(x0, this.c);
                    }

                    public strictfp void set(double[] x, double newResult) {
                        x[0] = StrictMath.pow(this.scaleInv * newResult, this.cInv);
                    }
                };
            }
        }
        //[[Repeat.SectionEnd getStrictUpdatableInstance]]
    }

    public abstract double get(double ...x);

    public abstract double get();

    public abstract double get(double x0);

    public abstract double get(double x0, double x1);

    public abstract double get(double x0, double x1, double x2);

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        if (binary) {
            return "power function f(x,y)=" + (scale == 1.0 ? "" : scale + "*x^y");
        } else {
            return "power function f(x)=" + (scale == 1.0 ? "" : scale) + "x^" + c;
        }
    }
}
