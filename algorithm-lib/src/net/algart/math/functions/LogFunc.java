package net.algart.math.functions;

/**
 * <p>Logarithm function:
 * <i>f</i>(<i>x</i><sub>0</sub>) =
 * log<sub><i>b</i></sub>(<i>x</i><sub>0</sub>)
 * (<i>b</i> is the base of the logarithm).
 * More precisely, the result of this function is
 * <tt>Math.log(x[0])*(1.0/Math.log(b))</tt> for the instance
 * returned by {@link #getInstance(double) getInstance(b)} method
 * or <tt>StrictMath.log(x[0])*(1.0/Math.log(b))</tt> for the instance
 * returned by {@link #getStrictInstance(double) getStrictInstance(b)} method.
 * The {@link #get} method of the instance of this class requires at least 1 argument
 * and throws <tt>IndexOutOfBoundsException</tt> if the number of arguments is 0.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class LogFunc implements Func {
    private final double base;

    private LogFunc(double base) {
        this.base = base;
    }

    /**
     * Returns an instance of this class for the given logarithm base <tt>b</tt>
     * using <tt>Math.log</tt> method.</p>
     *
     * <p>This method returns special optimized versions of this class for <tt>base=Math.E</tt>
     * and <tt>base=10.0</tt>.
     *
     * @param b the base of the logarithm.
     * @return  an instance of this class using <tt>Math.log</tt> method.
     */
    public static LogFunc getInstance(double b) {
        if (b == Math.E) {
            return new LogFunc(b) {
                public double get(double ...x) {
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
                public double get(double ...x) {
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
                public double get(double ...x) {
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
      * Returns an instance of this class for the given logarithm base <tt>b</tt>
      * using <tt>StrictMath.log</tt> method.</p>
      *
      * <p>This method returns special optimized versions of this class for <tt>base=Math.E</tt>
      * and <tt>base=10.0</tt>.
      *
      * @param b the base of the logarithm.
      * @return  an instance of this class using <tt>StrictMath.log</tt> method.
      */
     public static LogFunc getStrictInstance(double b) {
         if (b == Math.E) {
             return new LogFunc(b) {
                 public strictfp double get(double ...x) {
                     return StrictMath.log(x[0]);
                 }

                 public strictfp double get(double x0) {
                     return StrictMath.log(x0);
                 }

                 public strictfp double get(double x0, double x1) {
                     return StrictMath.log(x0);
                 }

                 public strictfp double get(double x0, double x1, double x2) {
                     return StrictMath.log(x0);
                 }

                 public strictfp double get(double x0, double x1, double x2, double x3) {
                     return StrictMath.log(x0);
                 }
             };
         } else if (b == 10.0) {
             return new LogFunc(b) {
                 public strictfp double get(double ...x) {
                     return StrictMath.log10(x[0]);
                 }

                 public strictfp double get(double x0) {
                     return StrictMath.log10(x0);
                 }

                 public strictfp double get(double x0, double x1) {
                     return StrictMath.log10(x0);
                 }

                 public strictfp double get(double x0, double x1, double x2) {
                     return StrictMath.log10(x0);
                 }

                 public strictfp double get(double x0, double x1, double x2, double x3) {
                     return StrictMath.log10(x0);
                 }
             };
         } else {
             final double mult = 1.0 / Math.log(b);
             return new LogFunc(b) {
                 public strictfp double get(double ...x) {
                     return StrictMath.log(x[0]) * mult;
                 }

                 public strictfp double get(double x0) {
                     return StrictMath.log(x0) * mult;
                 }

                 public strictfp double get(double x0, double x1) {
                     return StrictMath.log(x0) * mult;
                 }

                 public strictfp double get(double x0, double x1, double x2) {
                     return StrictMath.log(x0) * mult;
                 }

                 public strictfp double get(double x0, double x1, double x2, double x3) {
                     return StrictMath.log(x0) * mult;
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
