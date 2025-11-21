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

import java.util.Objects;

/**
 * <p>Aperture filtering operator in <i>n</i>-dimensional Euclidean space:
 * <i>g</i>(<b>x</b>) = <i>O</i>&nbsp;<i>f</i>(<b>x</b>) =
 * <b>apertureFunc</b>(<i>f</i>(<b>x</b>+<b>a</b><sub>0</sub>), <i>f</i>(<b>x</b>+<b>a</b><sub>1</sub>), ...,
 * <i>f</i>(<b>x</b>+<b>a</b><sub><i>m</i>-1</sub>)),
 * where <b>x</b> is a point of the <i>n</i>-dimensional space,
 * <b>apertureFunc</b> is some function with <i>m</i> arguments (the <i>aperture function</i>),
 * {<b>a</b><sub><i>i</i></sub>} is a set of points with (usually) little coordinates (the <i>aperture</i>),
 * <i>f</i> is the source mathematical function
 * and <i>g</i> is the result of applying the operator to <i>f</i>.</p>
 *
 * <p>The number of space dimensions <i>n</i> is equal to the length of the <code>apertureDim</code> array,
 * passed to all generation methods. However, the new function, returned by this operator
 * (by its {@link #apply(Func)} method, can be called with any number of arguments <i>N</i>.
 * If the number of arguments <i>N</i>&lt;<i>n</i>, than only first <i>N</i> coordinates
 * of the aperture points <b>a</b><sub><i>i</i></sub> will be used:
 * in other words, the aperture will be projected to the <i>N</i>-dimensional subspace.
 * If the number of arguments <i>N</i>&gt;<i>n</i>, than only first <i>n</i> coordinates
 * of the source point <b>x</b> will be increased by the corresponding coordinates of aperture points:
 * in other words, all extra coordinates of aperture points will be supposed to be zero.
 * In any case, the original function <i>f</i> will be called with the same number of arguments <i>N</i>,
 * as were passed to the new function <i>g</i>.
 * So, if the function <i>f</i> has some restriction for the possible number of arguments,
 * the new function <i>g</i> will have the same restrictions.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see ApertureFilteredFunc
 */
public final class ApertureFilterOperator implements Operator {
    final long[] apertureDim;
    final double[] apertureFrom;
    final double[] apertureSteps;
    final int totalCount;
    final Func apertureFunc;
    final boolean isNonweightedSum;
    final double a, b;

    private ApertureFilterOperator(
            Func apertureFunc,
            long[] apertureDim, double[] apertureFrom, double[] apertureSteps) {
        Objects.requireNonNull(apertureDim, "Null apertureDim argument");
        if (apertureDim.length == 0) {
            throw new IllegalArgumentException("Empty apertureDim array");
        }
        long product = 1;
        for (int k = 0; k < apertureDim.length; k++) {
            long d = apertureDim[k];
            if (d <= 0) {
                throw new IllegalArgumentException("Negative or zero aperture dimension #" + k + ": " + d);
            }
            // assert product <= Integer.MAX_VALUE;
            if (d > Integer.MAX_VALUE || (product *= d) > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Too large number of points in the aperture: "
                        + "apertureDim[0] * apertureDim[1] * ... > Integer.MAX_VALUE");
            }
        }
        this.totalCount = (int) product;
        if (apertureFrom != null) {
            if (apertureFrom.length != apertureDim.length) {
                throw new IllegalArgumentException("apertureFrom.length (" + apertureFrom.length
                        + ") does not match apertureDim.length (" + apertureDim.length + ")");
            }
        } else {
            apertureFrom = new double[apertureDim.length]; // zero-filled
        }
        if (apertureSteps != null) {
            if (apertureSteps.length != apertureDim.length) {
                throw new IllegalArgumentException("apertureSteps.length (" + apertureSteps.length
                        + ") does not match apertureDim.length (" + apertureDim.length + ")");
            }
        } else {
            apertureSteps = new double[apertureDim.length];
            for (int k = 0; k < apertureDim.length; k++) {
                apertureSteps[k] = 1.0 / apertureDim[k];
            }
        }
        if (apertureFunc == null) {
            this.a = 1.0 / this.totalCount;
            this.b = 0.0;
            this.isNonweightedSum = true;
        } else if (apertureFunc instanceof LinearFunc lf) {
            this.b = lf.b();
            if (lf.n() > this.totalCount) {
                throw new IllegalArgumentException("Insufficient number of aperture points for the aperture function");
            }
            if (lf.n() == this.totalCount) { // in another case, we'll not perform optimization
                this.a = lf.a(0);
                this.isNonweightedSum = lf.isNonweighted();
            } else {
                this.a = Double.NaN;
                this.isNonweightedSum = false;
            }
        } else {
            this.a = this.b = Double.NaN;
            this.isNonweightedSum = false;
        }
        this.apertureDim = apertureDim.clone();
        this.apertureFrom = apertureFrom.clone();
        this.apertureSteps = apertureSteps.clone();
        this.apertureFunc = apertureFunc;
    }

    /**
     * Returns an instance of this class, describing the aperture filter with the specified aperture and
     * aperture function <code>apertureFunc</code>.
     *
     * <p>The aperture {<b>a</b><sub><i>i</i></sub>}, <i>i</i>=0,1,...,<i>m</i>-1 is defined by
     * <code>apertureDim</code>, <code>apertureFrom</code>, <code>apertureSteps</code> arguments. Namely,
     * the number of the space dimensions for the new operator will be equal to <code>apertureDim.length</code>,
     * and the aperture will consist of the following points
     * <b>a</b><sub><i>i</i></sub> = (<i>a</i><sub><i>i</i>0</sub>, <i>a</i><sub><i>i</i>1</sub>,
     * ..., <i>a</i><sub><i>i</i>,<i>n</i>-1</sub>):
     *
     * <blockquote>
     * <i>a</i><sub><i>i</i>0</sub> = apertureFrom[0] + <i>j</i><sub>0</sub> * apertureSteps[0],
     * &nbsp;&nbsp;<i>j</i><sub>0</sub> = 0,1,...,apertureDim[0],<br>
     * <i>a</i><sub><i>i</i>1</sub> = apertureFrom[1] + <i>j</i><sub>1</sub> * apertureSteps[1],
     * &nbsp;&nbsp;<i>j</i><sub>1</sub> = 0,1,...,apertureDim[1],<br>
     * . . .<br>
     * <i>a</i><sub><i>i</i>,<i>n</i>-1</sub> = apertureFrom[<i>n</i>-1] +
     * <i>j</i><sub><i>n</i>-1</sub> * apertureSteps[<i>n</i>-1],
     * &nbsp;&nbsp;<i>j</i><sub><i>n</i>-1</sub> = 0,1,...,apertureDim[<i>n</i>-1]
     * </blockquote>
     *
     * <p>In other words, the aperture is a rectangular <i>n</i>-dimensional grid of point,
     * the coordinates of which are started from <code>apertureFrom</code>
     * and increased by the steps <code>apertureSteps</code>.
     * The number of points in the aperture is
     *
     * <blockquote><i>m</i> =
     * <code>apertureDim[0] * apertureDim[1] * ... * apertureDim[<i>n</i>-1]</code></blockquote>
     *
     * <p>This number must not be greater than <code>Integer.MAX_VALUE</code>.
     *
     * <p>The passed Java arrays are cloned by this method: no references to them
     * are maintained by the created instance.
     *
     * @param apertureFunc  the aperture function.
     * @param apertureDim   the dimensions of the aperture.
     * @param apertureFrom  the start coordinates of the points in the aperture.
     * @param apertureSteps the steps of changing coordinates of the points in the aperture.
     * @return the aperture filtering operator with the specified aperture and aperture function.
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException if the lengths of <code>apertureDim</code>, <code>apertureFrom</code> and
     *                                  <code>apertureSteps</code> arrays are not equal,
     *                                  or if they are zero ("0-dimensional" space),
     *                                  or if some elements of <code>apertureDim</code> array are zero or negative,
     *                                  or if {@link #tooLargeAperture(long[]) tooLargeAperture(apertureDim)}
     *                                  returns <code>true</code>,
     *                                  or, probably, if it's possible to detect that the number of points
     *                                  in the aperture (<i>m</i>) is insufficient
     *                                  for passing to the <code>apertureFunc</code> function.
     * @see #getInstance(Func, long[])
     * @see #getAveragingInstance(long[], double[], double[])
     * @see #getAveragingInstance(long[])
     * @see #tooLargeAperture(long[])
     */
    public static ApertureFilterOperator getInstance(
            Func apertureFunc,
            long[] apertureDim,
            double[] apertureFrom,
            double[] apertureSteps) {
        Objects.requireNonNull(apertureFrom, "Null apertureFrom argument");
        Objects.requireNonNull(apertureSteps, "Null apertureSteps argument");
        Objects.requireNonNull(apertureFunc, "Null apertureFunc argument");
        return new ApertureFilterOperator(apertureFunc, apertureDim, apertureFrom, apertureSteps);
    }

    /**
     * Equivalent to <code>{@link #getInstance(Func, long[], double[], double[])
     * getInstance(apertureFunc, apertureDim, apertureFrom, apertureSteps)}</code>,
     * where <code>averagingFrom</code> and <code>apertureSteps</code> are chosen automatically
     * to get an aperture <code>1.0x1.0x...</code> starting from the origin of coordinates
     * (0&lt;=<i>a</i><sub><i>ij</i></sub>&lt;1).
     * More precisely, the following <code>averagingFrom</code> and <code>apertureSteps</code> are chosen:
     *
     * <pre>
     * apertureFrom[k] = 0.0;
     * apertureSteps[k] = 1.0 / apertureDim[n];
     * </pre>
     *
     * @param apertureFunc the aperture function.
     * @param apertureDim  the dimensions of the aperture.
     * @return the aperture filtering operator with the specified aperture and aperture function.
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException if the lengths of <code>apertureDim</code> is zero ("0-dimensional" space),
     *                                  or if some elements of <code>apertureDim</code> array are zero or negative,
     *                                  or if {@link #tooLargeAperture(long[]) tooLargeAperture(apertureDim)}
     *                                  returns <code>true</code>,
     *                                  or, probably, if it's possible to detect that the number of points
     *                                  in the aperture (<i>m</i>) is insufficient
     *                                  for passing to the <code>apertureFunc</code> function.
     */
    public static ApertureFilterOperator getInstance(Func apertureFunc, long... apertureDim) {
        Objects.requireNonNull(apertureFunc, "Null apertureFunc argument");
        return new ApertureFilterOperator(apertureFunc, apertureDim, null, null);
    }

    /**
     * Equivalent to <code>{@link #getInstance(Func, long[], double[], double[])
     * getInstance(averagingFunc, apertureDim, apertureFrom, apertureSteps)}</code>,
     * where <code>averagingFunc</code> is the averaging linear function
     * {@link LinearFunc#getAveragingInstance(int) LinearFunc.getAveragingInstance(<i>m</i>)},
     * <code><i>m</i>&nbsp;=&nbsp;apertureDim[0]*apertureDim[1]*...</code>.
     *
     * @param apertureDim   the dimensions of the aperture.
     * @param apertureFrom  the start coordinates of the points in the aperture.
     * @param apertureSteps the steps of changing coordinates of the points in the aperture.
     * @return the aperture averaging (smoothing) operator with the specified aperture.
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #getInstance(Func, long[], double[], double[])}.
     */
    public static ApertureFilterOperator getAveragingInstance(
            long[] apertureDim,
            double[] apertureFrom,
            double[] apertureSteps) {
        Objects.requireNonNull(apertureFrom, "Null apertureFrom argument");
        Objects.requireNonNull(apertureSteps, "Null apertureSteps argument");
        return new ApertureFilterOperator(null, apertureDim, apertureFrom, apertureSteps);
    }

    /**
     * Equivalent to <code>{@link #getInstance(Func, long[])
     * getInstance(averagingFunc, apertureDim)}</code>,
     * where <code>averagingFunc</code> is the averaging linear function
     * {@link LinearFunc#getAveragingInstance(int) LinearFunc.getAveragingInstance(<i>m</i>)},
     * <code><i>m</i>&nbsp;=&nbsp;apertureDim[0]*apertureDim[1]*...</code>.
     *
     * @param apertureDim the dimensions of the aperture.
     * @return the aperture averaging (smoothing) operator with the specified aperture.
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #getInstance(Func, long[])}.
     */
    public static ApertureFilterOperator getAveragingInstance(long... apertureDim) {
        return new ApertureFilterOperator(null, apertureDim, null, null);
    }

    /**
     * Returns <code>true</code> if the specified sizes of the aperture are too large for processing by this class.
     * Namely, it returns <code>true</code> if and only if the product of all dimensions
     * <code>apertureDim[0]*apertureDim[1]*...</code> is greater than <code>Integer.MAX_VALUE</code>.
     *
     * <p>If you are not sure that your aperture is small enough, please call this method before
     * instantiating this class.
     *
     * @param apertureDim the dimensions of the aperture.
     * @return <code>true</code> if the specified dimensions of the aperture are too large
     * (<code>&gt;Integer.MAX_VALUE</code> points).
     * @throws IllegalArgumentException if some elements of <code>apertureDim</code> array are zero or negative.
     */
    public static boolean tooLargeAperture(long... apertureDim) {
        long totalCount = 1;
        for (int k = 0; k < apertureDim.length; k++) {
            long d = apertureDim[k];
            if (d <= 0) {
                throw new IllegalArgumentException("Negative or zero aperture dimension #" + k + ": " + d);
            }
            assert totalCount <= Integer.MAX_VALUE;
            if (d > Integer.MAX_VALUE || (totalCount *= d) > Integer.MAX_VALUE) {
                return true;
            }
        }
        return false;
    }

    public Func apply(Func f) {
        return ApertureFilteredFunc.getInstance(f, this);
    }

    /**
     * Returns the number of dimensions in the aperture of this filter.
     * The result is equal to the length of <code>apertureDim</code> array, passed to all generation methods.
     *
     * @return the number of dimensions in the aperture of this filter.
     */
    public int n() {
        return apertureDim.length;
    }

    /**
     * Returns the dimensions in the aperture of this filter.
     * The result is equal to <code>apertureDim</code> array, passed to all generation methods.
     *
     * <p>The returned array is a clone of the internal dimension array stored in this object.
     * The returned array is never empty (its length cannot be zero).
     *
     * @return the dimensions in the aperture of this filter.
     */
    public long[] apertureDim() {
        return apertureDim.clone();
    }

    /**
     * Returns the start coordinates of the points in the aperture of this filter.
     * The result is equal to <code>apertureFrom</code> array, passed to
     * {@link #getInstance(Func, long[], double[], double[])} or
     * {@link #getAveragingInstance(long[], double[], double[])} generation methods.
     *
     * <p>The returned array is a clone of the internal dimension array stored in this object.
     * The returned array is never empty (its length cannot be zero).
     *
     * @return the start coordinates of the points in the aperture of this filter.
     */
    public double[] apertureFrom() {
        return apertureFrom.clone();
    }

    /**
     * Returns the steps of changing coordinates of the points in the aperture of this filter.
     * The result is equal to <code>apertureSteps</code> array, passed to
     * {@link #getInstance(Func, long[], double[], double[])} or
     * {@link #getAveragingInstance(long[], double[], double[])} generation methods.
     *
     * <p>The returned array is a clone of the internal dimension array stored in this object.
     * The returned array is never empty (its length cannot be zero).
     *
     * @return the steps of changing coordinates of the points in the aperture of this filter.
     */
    public double[] apertureSteps() {
        return apertureSteps.clone();
    }

    /**
     * Equivalent to <code>{@link #apertureDim()}[coordIndex]</code>.
     *
     * @param coordIndex the index of dimension.
     * @return the dimension of the aperture of this filter.
     */
    public double apertureDim(int coordIndex) {
        return apertureDim[coordIndex];
    }

    /**
     * Equivalent to <code>{@link #apertureFrom()}[coordIndex]</code>.
     *
     * @param coordIndex the index of coordinate.
     * @return the start coordinate of the points in the aperture of this filter.
     */
    public double apertureFrom(int coordIndex) {
        return apertureFrom[coordIndex];
    }

    /**
     * Equivalent to <code>{@link #apertureFrom()}[coordIndex] + ({@link #apertureDim()}[coordIndex] - 1)
     * * {@link #apertureSteps()}[coordIndex]</code>.
     *
     * @param coordIndex the index of coordinate.
     * @return the last coordinate of the points in the aperture of this filter.
     */
    public double apertureTo(int coordIndex) {
        return apertureFrom[coordIndex] + (apertureDim[coordIndex] - 1) * apertureSteps[coordIndex];
    }

    /**
     * Equivalent to <code>{@link #apertureSteps()}[coordIndex]</code>.
     *
     * @param coordIndex the index of coordinate.
     * @return the steps of changing this coordinate of the points in the aperture of this filter.
     */
    public double apertureStep(int coordIndex) {
        return apertureSteps[coordIndex];
    }

    /**
     * Returns the maximal aperture size for all dimensions: the maximal value of
     * <code>{@link #apertureTo(int) apertureTo}(k)-{@link #apertureFrom(int) apertureFrom}(k)</code>
     * for all <code>k=0,1,...,{@link #n() n()}-1</code>
     *
     * @return the maximal aperture size for all dimensions.
     */
    public double maxApertureSize() {
        double result = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < apertureDim.length; k++) {
            result = Math.max(result, (apertureDim[k] - 1) * apertureSteps[k]);
        }
        return result;
    }

    /**
     * Returns the aperture function, used by this filter.
     *
     * @return the aperture function, used by this filter.
     */
    public Func apertureFunc() {
        if (apertureFunc != null) {
            return apertureFunc;
        } else {
            return LinearFunc.getNonweightedInstance(0.0, this.a, this.totalCount);
        }
    }

    /**
     * Returns <code>true</code> if and only if this filter performs averaging, i&#46;e&#46;
     * if the {@link #apertureFunc() aperture function} is a {@link LinearFunc linear function},
     * where the <i>b</i> coefficient is zero and all <i>a<sub>i</sub></i> coefficients
     * are equal to 1/<i>m</i>, <i>m</i>=<code>apertureDim[0]*apertureDim[1]*...</code>.
     * In particular, this method returns <code>true</code> if this filter was created by
     * {@link #getAveragingInstance(long[], double[], double[])} or
     * {@link #getAveragingInstance(long[])} method.
     *
     * @return <code>true</code> if and only if this filter performs averaging (smoothing).
     */
    public boolean isAveraging() {
        return isNonweightedSum && this.a == 1.0 / this.totalCount && this.b == 0.0;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "aperture filter " + apertureDim.length + "-dimensional operator"
                + (isAveraging() ? " (averaging)" : " (based on " + apertureFunc + ")");
    }
}
