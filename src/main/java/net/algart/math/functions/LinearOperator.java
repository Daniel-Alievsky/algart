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

import net.algart.math.Point;

import java.util.Objects;

/**
 * <p>Linear operator (affine transformation):
 * <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>Ax</b>&nbsp;+&nbsp;<b>b</b>),
 * where the numeric <i>n</i>&nbsp;x&nbsp;<i>n</i> matrix <b>A</b>
 * and the <i>n</i>-dimensional vector <b>b</b> are parameters of the transformation.
 * (It is a particular case of the {@link ProjectiveOperator projective transformation},
 * when <b>c</b> vector is zero and <i>d</i> number is 1.0.)</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public final class LinearOperator extends ProjectiveOperator {

    LinearOperator(double[] a, double[] diagonal, double[] b) {
        super(a, diagonal, b, null, 1.0);
    }

    /**
     * Returns an instance of this class, describing the linear operator with the specified matrix <b>A</b>
     * and vector <b>b</b>:
     * <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>Ax</b>&nbsp;+&nbsp;<b>b</b>).
     * The coordinates of the vector <b>b</b> must be listed in <code>b</code> array.
     * The elements of the matrix <b>A</b> must be listed, row by row, in the <code>a</code> array:
     * <b>A</b>={<i>a</i><sub><i>ij</i></sub>},
     * <i>a</i><sub><i>ij</i></sub>=<code>a[<i>i</i>*<i>n</i>+<i>j</i>]</code>,
     * <i>i</i> is the index of the row (0..<i>n</i>-1),
     * <i>j</i> is the index of the column (0..<i>n</i>-1),
     * <i>n</i>=<code>b.length</code>.
     * The length <code>a.length</code> of the <code>a</code> array must be equal to the square <i>n</i><sup>2</sup>
     * of the length <i>n</i>=<code>b.length</code> of the <code>b</code> array.
     * Empty arrays (<i>n</i>=0) are not allowed.
     *
     * <p>The passed <code>a</code> and <code>b</code> Java arrays are cloned by this method: no references to them
     * are maintained by the created instance.
     *
     * @param a the elements of <b>A</b> matrix.
     * @param b the coordinates of <b>b</b> vector.
     * @return the linear operator <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>Ax</b>&nbsp;+&nbsp;<b>b</b>).
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException if <code>b.length==0</code> or <code>a.length!=b.length<sup>2</sup></code>.
     */
    public static LinearOperator getInstance(double[] a, double[] b) {
        Objects.requireNonNull(a, "Null A matrix");
        Objects.requireNonNull(b, "Null b vector");
        return new LinearOperator(a.clone(), null, b.clone());
    }

    /**
     * Returns an instance of this class, describing the linear operator with the diagonal matrix <b>A</b>
     * and vector <b>b</b>:
     * <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>Ax</b>&nbsp;+&nbsp;<b>b</b>),
     * where <b>A</b>={<i>a</i><sub><i>ij</i></sub>},
     * <i>a</i><sub><i>ij</i></sub>=0.0 if <i>i</i>!=<i>j</i>,
     * <i>a</i><sub><i>ii</i></sub>=<code>diagonal[<i>i</i>]</code>.
     * The coordinates of the vector <b>b</b> must be listed in <code>b</code> array.
     * Empty arrays (<code>diagonal.length=b.length=0</code>) are not allowed.
     *
     * <p>This linear operator performs resizing and shift along coordinate axes.
     *
     * <p>The passed <code>diagonal</code> and <code>b</code> Java arrays are cloned by this method: no references to them
     * are maintained by the created instance.
     *
     * @param diagonal the diagonal elements of <b>A</b> matrix (all other elements are supposed to be zero).
     * @param b        the coordinates of <b>b</b> vector.
     * @return the linear operator <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>Ax</b>&nbsp;+&nbsp;<b>b</b>).
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException if <code>diagonal.length==0</code> or <code>diagonal.length!=b.length</code>.
     */
    public static LinearOperator getDiagonalInstance(double[] diagonal, double[] b) {
        Objects.requireNonNull(diagonal, "Null diagonal array");
        Objects.requireNonNull(b, "Null b vector");
        return new LinearOperator(null, diagonal.clone(), b.clone());
    }

    /**
     * Equivalent to
     * {@link #getDiagonalInstance(double[], double[]) getDiagonalInstance(diagonal, new double[diagonal.length])}
     * (the case of zero <b>b</b> vector).
     * This linear operator performs resizing along coordinate axes.
     *
     * @param diagonal the diagonal elements of <b>A</b> matrix (all other elements are supposed to be zero).
     * @return the linear operator <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>Ax</b>).
     * @throws NullPointerException     if the argument of the method is {@code null}.
     * @throws IllegalArgumentException if <code>diagonal.length==0</code>.
     */
    public static LinearOperator getDiagonalInstance(double... diagonal) {
        Objects.requireNonNull(diagonal, "Null diagonal array");
        return new LinearOperator(null, diagonal.clone(), new double[diagonal.length]);
    }

    /**
     * Equivalent to
     * {@link #getDiagonalInstance(double[], double[]) getDiagonalInstance(diagonal, b)},
     * where <code>diagonal</code> is an array consisting of <code>b.length</code> unit values (1.0).
     * This linear operator performs shifting along coordinate axes.
     *
     * @param b the coordinates of <b>b</b> vector.
     * @return the linear operator <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>x</b>&nbsp;+&nbsp;<b>b</b>).
     * @throws NullPointerException     if the argument of the method is {@code null}.
     * @throws IllegalArgumentException if <code>b.length==0</code>.
     */
    public static LinearOperator getShiftInstance(double... b) {
        Objects.requireNonNull(b, "Null b array");
        return new LinearOperator(null, null, b);
    }

    /**
     * Returns an instance of this class, describing rotation in 2D plane by the specified angle (in radians)
     * around the specified center.
     * Almost equivalent to <code>{@link #getInstance(double[], double[]) getInstance}(a,b)</code>,
     * where:
     * <ul>
     * <li><code>a = {cos,sin,-sin,cos}</code> &mdash; matrix <b>A</b>
     * (<code>cos=StrictMath.cos(angle)</code>),
     * <code>sin=StrictMath.sin(angle)</code>);</li>
     *
     * <li><code>b = {centerX-a[0]*centerX-a[1]*centerY,
     * centerY-a[2]*centerX-a[3]*centerY}</code> &mdash; vector
     * <b>b</b>=<b>c</b>&minus;<b>Ac</b> (<b>c</b>=<code>{centerX, centerY}</code>).</li>
     * </ul>
     *
     * <p>The only difference from these formulas is special processing some cases, when the angle is <i>k</i>&pi;/2
     * with integer <i>k</i> (more precisely, <code>k/2.0*StrictMath.PI</code>):
     * <code>StrictMath.cos</code> and <code>StrictMath.sin</code> methods can return inexact results here,
     * but this method uses precise values &plusmn;1 in these cases.
     *
     * @param centerX the <i>x</i>-coordinate of the rotation center.
     * @param centerY the <i>y</i>-coordinate of the rotation center.
     * @param angle   the rotation angle (in radians; positive values correspond to <i>clockwise</i> rotation,
     *                if the <i>x</i> axis is directed <i>rightwards</i> and the <i>y</i> axis
     *                is directed <i>downwards</i>, according traditions of computer image processing).
     * @return 2-dimensional linear operator, describing this rotation.
     */
    public static LinearOperator getRotation2D(double centerX, double centerY, double angle) {
        double cos = StrictMath.cos(angle);
        double sin = StrictMath.sin(angle);
        for (int k = -4; k <= 4; k++) {
            if (angle == k / 2.0 * StrictMath.PI) { // use precise cosine and sine in these cases
                assert StrictMath.abs(StrictMath.round(cos) - cos) < 1e-6;
                assert StrictMath.abs(StrictMath.round(sin) - sin) < 1e-6;
                cos = StrictMath.round(cos);
                sin = StrictMath.round(sin);
            }
        }
        // y=A(x-c)+c=Ax+c-Ac
        double[] a = {
                cos, sin,
                -sin, cos,
        };
        double[] b = {
                centerX - a[0] * centerX - a[1] * centerY,
                centerY - a[2] * centerX - a[3] * centerY,
        };
        return new LinearOperator(a, null, b);
    }

    /**
     * Returns the n-dimensional linear operator, that transforms (maps)
     * the given <i>n</i>+1 points <b>p</b><sub>0</sub>, <b>p</b><sub>1</sub>, ..., <b>p</b><sub><i>n</i></sub> to
     * the given another <i>n</i>+1 points <b>q</b><sub>0</sub>, <b>q</b><sub>1</sub>, ..., <b>q</b><sub><i>n</i></sub>
     * of the n-dimensional space.
     * In other words, the matrix <b>A</b> and the vector <b>b</b> in the returned operator
     * fulfil the following <i>n</i>+1 conditions:
     *
     * <blockquote>
     * <b>Ap</b><sub>0</sub> + <b>b</b> = <b>q</b><sub>0</sub>,<br>
     * <b>Ap</b><sub>1</sub> + <b>b</b> = <b>q</b><sub>1</sub>,<br>
     * ...,<br>
     * <b>Ap</b><sub><i>n</i></sub> + <b>b</b> = <b>q</b><sub><i>n</i></sub>
     * </blockquote>
     *
     * <p>It is possible that there is no such operator
     * or there are many different solutions (degenerated cases).
     * In this case, this method still returns some operator, but some coefficients of <b>A</b> matrix
     * and <b>b</b> vector in the returned operator will probably be <code>Double.NaN</code>,
     * <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code>.
     *
     * <p>All passed points must be <i>n</i>-dimensional,
     * where <i>n</i><code>+1=p.length=q.length</code>.
     *
     * @param q the destination points.
     * @param p the source points.
     * @return the <i>n</i>-dimensional linear operator, which maps <b>p</b><sub><i>i</i></sub> to
     * <b>q</b><sub><i>i</i></sub> for all <i>i</i>=0,1,2,...,<i>n</i>.
     * @throws NullPointerException     if one of arguments of this method or one of elements of
     *                                  <code>p</code> and <code>q</code> arrays {@code null}.
     * @throws IllegalArgumentException if the lengths of the passed <code>p</code> and
     *                                  <code>q</code> arrays are not equal,
     *                                  or if for some <code>k</code>
     *                                  <code>p[k].{@link Point#coordCount() coordCount()}!=p.length-1</code> or
     *                                  <code>q[k].{@link Point#coordCount() coordCount()}!=p.length-1</code>.
     * @throws OutOfMemoryError         if there is not enough Java memory for storing two Java arrays
     *                                  <code>double[n*n]</code> and <code>double[(n+1)*(n+1)]</code>,
     *                                  where <code>n+1=p.length</code>,
     *                                  or if <code>(n+1)*(n+1)&gt;Integer.MAX_VALUE</code>.
     */
    public static LinearOperator getInstanceByPoints(Point[] q, Point[] p) {
        Objects.requireNonNull(p, "Null p argument");
        Objects.requireNonNull(q, "Null q argument");
        if (p.length != q.length)
            throw new IllegalArgumentException("p and q point arrays lengths mismatch: p.length="
                    + p.length + ", q.length=" + q.length);
        if (p.length == 0)
            throw new IllegalArgumentException("Empty p and q arrays");
        final int n = p.length - 1;
        if (((long) n + 1) * ((long) n + 1) > Integer.MAX_VALUE)
            throw new OutOfMemoryError("Too large necessary matrix (more than Integer.MAX_VALUE elements)");
        for (int k = 0; k < p.length; k++) {
            if (p[k].coordCount() != n)
                throw new IllegalArgumentException("n+1 n-dimensional points are necessary to "
                        + "find the linear operator, but we have " + (n + 1) + " points, "
                        + "and the source point #" + k + " is " + p[k].coordCount() + "-dimensional");
            if (q[k].coordCount() != n)
                throw new IllegalArgumentException("n+1 n-dimensional points are necessary to "
                        + "find the linear operator, but we have " + (n + 1) + " points, "
                        + "and the destination point #" + k + " is " + q[k].coordCount() + "-dimensional");
        }
        // A * (px0,py0,pz0,...) + b = (qx0,qy0,qz0,...)
        // A * (px1,py1,pz1,...) + b = (qx1,qy1,qz1,...)
        // ..., i.e.
        //
        // px0*a00 + py0*a01 + pz0*a02 + ... + bx                                         = qx0
        // px1*a00 + py1*a01 + pz1*a02 + ... + bx                                         = qx1
        // px2*a00 + py2*a01 + pz2*a02 + ... + bx                                         = qx2
        // ...
        //                                         px0*a10 + py0*a11 + pz0*a12 + ... + by = qy0
        //                                         px1*a10 + py1*a11 + pz1*a12 + ... + by = qy1
        //                                         px2*a10 + py2*a11 + pz2*a13 + ... + by = qy2
        // etc.
        // In other words, here are n independent equation systems Sv=t
        double[] s = new double[(n + 1) * (n + 1)];
        double[] t = new double[n + 1];
        double[] v = new double[n + 1];
        for (int i = 0, sOfs = 0; i <= n; i++) {
            for (int j = 0; j < n; j++, sOfs++) {
                s[sOfs] = p[i].coord(j);
            }
            s[sOfs++] = 1.0;
        }
        // S is the matrix of the equations set (same for all equations sets)
        double[] a = new double[n * n];
        double[] b = new double[n];
        for (int i = 0, aOfs = 0; i < n; i++, aOfs += n) {
            // we shall solve here equations set #i
            // and find the line #i of the matrix A and the element #i of the vector b
            for (int j = 0; j <= n; j++) {
                t[j] = q[j].coord(i);
            }
            // t is the right side of the equations set
            solveLinearEquationsSet(v, s.clone(), t);
//            solveLinearEquationsSetByCramerMethod(v, s, t);
            System.arraycopy(v, 0, a, aOfs, n);
            b[i] = v[n];

        }
        return new LinearOperator(a, null, b);
    }

    /**
     * Returns superposition of this and the passed linear operators.
     * More precisely, if this operator corresponds to the affine transformation <b>Ax</b>&nbsp;+&nbsp;<b>b</b>,
     * and the passed one corresponds to the affine transformation <b>A'x</b>&nbsp;+&nbsp;<b>b'</b>,
     * then the returned operator corresponds to the affine transformation
     * <b>A''x</b> + <b>b''</b> = <b>A'</b>(<b>Ax</b> + <b>b</b>) + <b>b'</b>,
     * i.e. in the returned operator
     * <b>A''</b> = <b>A'A</b>, <b>b''</b> = <b>A'b</b> + <b>b'</b>.
     *
     * @param operator the second operator, that should be applied after this one.
     * @return superposition of this and the passed operator.
     * @throws NullPointerException     if the argument of the method is {@code null}.
     * @throws IllegalArgumentException if <code>operator.{@link #n() n()}!=this.{@link #n() n()}</code>.
     */
    public LinearOperator superposition(LinearOperator operator) {
        Objects.requireNonNull(operator, "Null operator argument");
        if (operator.n != n)
            throw new IllegalArgumentException("Passed and this operators dimensions mismatch: operator.n()="
                    + operator.n + ", this.n()=" + n);
        if (operator.isShift()) {
            double[] b = this.b.clone();
            for (int i = 0; i < n; i++) {
                b[i] += operator.b[i];
            }
            return new LinearOperator(this.a, this.diagonal, b);
        }
        double[] a = this.a != null ? this.a : this.a(); // A
        double[] aOther = operator.a != null ? operator.a : operator.a(); // A'
        double[] aNew = new double[a.length]; // A'' (result)
        double[] bNew = new double[n]; // b'' (result)
        // y'' = A'y' + b' = A'(Ax+b) + b' = A'Ax + (A'b+b'), so A''=A'A, b''=A'b+b'
        for (int in = 0; in < a.length; in += n) { // in = i * n
            for (int j = 0; j < n; j++) {
                double sum = 0.0;
                for (int k = 0, disp = j; k < n; k++, disp += n) {
                    sum += aOther[in + k] * a[disp];  // aOther[i * n + k] * a[k * n + j];
                }
                aNew[in + j] = sum;
            }
        }
        for (int i = 0, in = 0; i < n; i++, in += n) { // in = i * n
            double sum = operator.b[i];
            for (int j = 0; j < n; j++) {
                sum += aOther[in + j] * b[j];
            }
            bNew[i] = sum;
        }
        return new LinearOperator(aNew, null, bNew);
    }

    /**
     * Returns an instance of this class, identical to this one execpting that the new instance has the specified
     * vector <b>b</b>.
     *
     * <p>The passed <code>b</code> Java array is cloned by this method: no references to it
     * are maintained by the created instance.
     *
     * @param b the new coordinates of <b>b</b> vector.
     * @return the linear operator with changed <b>b</b> vector.
     * @throws NullPointerException     if the argument of the method is {@code null}.
     * @throws IllegalArgumentException if <code>b.length!=this.{@link #n() n()}</code>.
     */
    public LinearOperator changeB(double... b) {
        Objects.requireNonNull(b, "Null b array");
        if (b.length != n)
            throw new IllegalArgumentException("Passed b and this.b vector lengths mismatch: b.length="
                    + b.length + ", this b.length=" + n);
        return new LinearOperator(this.a, this.diagonal, b);
    }

    /**
     * This implementation calculates <code>destPoint</code> by multiplication
     * the <code>srcPoint</code> by the matrix <b>A</b> and adding the vector <b>b</b>.
     * to the coordinates <code>destPoint</code> of the destination point.
     *
     * @param destPoint the coordinates of the destination point <b>y</b>, filled by this method.
     * @param srcPoint  the coordinates of the source point <b>x</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>destPoint.length</code> or <code>srcPoint.length</code>
     *                                  is not equal to the {@link #n() number of dimensions}.
     */
    public void map(double[] destPoint, double[] srcPoint) {
        calculateAxPlusB(destPoint, srcPoint);
    }


    /**
     * Transforms the coordinates <code>destPoint</code> of the destination point in <i>n</i>-dimensional space
     * back to the coordinates <code>srcPoint</code> of the original point.
     *
     * <p>To find the <code>srcPoint</code>, this method solves the system of linear equations <b>Ax=y&minus;b</b>,
     * where the matrix <b>A</b> and the vector <b>b</b> are the parameters of this transformation,
     * <b>y</b> is <code>destPoint</code>, <b>x</b> is <code>srcPoint</code>.
     * This method uses
     * <a href="http://en.wikipedia.org/wiki/Gaussian_elimination">Gauss elimination algorithm
     * with partial (column) pivoting</a>.
     * This algorithm requires <i>O</i>(<i>n</i><sup>3</sup>) operations.
     *
     * <p>It is possible that there is no required <code>srcPoint</code>
     * or there are many different solutions (degenerated cases).
     * In this case, this method still returns some point, but some found <code>srcPoint</code> coordinates
     * will probably be <code>Double.NaN</code>, <code>Double.POSITIVE_INFINITY</code>
     * or <code>Double.NEGATIVE_INFINITY</code>.
     *
     * <p>Note: this method allocates some additional memory if the matrix <b>A</b>
     * is not {@link #isDiagonal() diagonal}.
     * If you don't want to occupy additional memory, you can directly use
     * {@link #solveLinearEquationsSet(double[], double[], double[])} method.
     *
     * <p>Note: this method works correctly even if <code>destPoint</code> and <code>srcPoint</code>
     * is the same Java array.
     *
     * @param srcPoint  the coordinates of the source point <b>x</b>, filled by this method.
     * @param destPoint the coordinates of the destinated point <b>y</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>destPoint.length</code> or <code>srcPoint.length</code>
     *                                  is not equal to the {@link #n() number of dimensions}.
     */
    public void inverseMap(double[] srcPoint, double[] destPoint) {
        Objects.requireNonNull(srcPoint, "Null srcPoint");
        Objects.requireNonNull(destPoint, "Null destPoint");
        if (srcPoint.length != n)
            throw new IllegalArgumentException("Illegal length of srcPoint array: "
                    + srcPoint.length + " for " + this);
        if (destPoint.length != n)
            throw new IllegalArgumentException("Illegal length of destPoint array: "
                    + destPoint.length + " for " + this);
        if (a != null) {
            double[] yMinusB = new double[n];
            for (int i = 0; i < n; i++) {
                yMinusB[i] = destPoint[i] - b[i];
            }
            double[] aClone = this.a.clone();
            solveLinearEquationsSet(srcPoint, aClone, yMinusB);
        } else if (diagonal != null) {
            for (int i = 0; i < n; i++) {
                srcPoint[i] = (destPoint[i] - b[i]) / diagonal[i];
            }
        } else {
            for (int i = 0; i < n; i++) {
                srcPoint[i] = destPoint[i] - b[i];
            }
        }

    }

    /**
     * Solves the system of linear equations <b>Ax</b>=<b>y</b> by
     * <a href="http://en.wikipedia.org/wiki/Gaussian_elimination">Gauss elimination algorithm
     * with partial (column) pivoting</a>.
     *
     * <p>The coordinates of the vector <b>y</b> must be listed in <code>y</code> array.
     * The elements of the matrix <b>A</b> must be listed, row by row, in the <code>a</code> array:
     * <b>A</b>={<i>a</i><sub><i>ij</i></sub>},
     * <i>a</i><sub><i>ij</i></sub>=<code>a[<i>i</i>*<i>n</i>+<i>j</i>]</code>,
     * <i>i</i> is the index of the row (0..<i>n</i>-1),
     * <i>j</i> is the index of the column (0..<i>n</i>-1),
     * <i>n</i>=<code>b.length</code>.
     * The length <code>a.length</code> of the <code>a</code> array must be equal to the square <i>n</i><sup>2</sup>
     * of the length <i>n</i>=<code>b.length</code> of the <code>b</code> array.
     * Empty arrays (<i>n</i>=0) are not allowed.
     *
     * <p>It is possible that there is no required <b>x</b> vector
     * or there are many different solutions (degenerated cases).
     * In this case, this method still find some <b>x</b> vector, but some found coordinates
     * in the <code>x</code> array will probably be
     * <code>Double.NaN</code>, <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code>.
     *
     * <p>This method is called in the {@link #inverseMap(double[], double[])} method,
     * if the matrix <b>A</b> is not {@link #isDiagonal() diagonal}.
     *
     * <p><i>Warning:</i> this method destroys the content of the passed <code>a</code> and <code>y</code> arrays!
     * But this method does not occupy any additional memory, unlike {@link #inverseMap(double[], double[])} method.
     *
     * <p><i>Warning:</i> this method will not work correctly if <code>x</code> and <code>y</code>
     * is the same Java array.
     *
     * @param x the coordinates of <b>x</b> vector, filled by this method.
     * @param a the elements of <b>A</b> matrix (row by row).
     * @param y the coordinates of <b>y</b> vector.
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException if the length of one of the passed arrays is 0,
     *                                  or if <code>x.length!=y.length</code>,
     *                                  or if <code>a.length!=x.length<sup>2</sup></code>.
     */
    public static void solveLinearEquationsSet(double[] x, double[] a, double[] y) {
        Objects.requireNonNull(x, "Null x");
        Objects.requireNonNull(y, "Null y");
        Objects.requireNonNull(a, "Null a");
        final int n = x.length;
        if (y.length != n)
            throw new IllegalArgumentException("x and y vector lengths mismatch: x.length="
                    + n + ", y.length=" + y.length);
        if (a.length != (long) n * (long) n)
            throw new IllegalArgumentException("Illegal size of A matrix: a.length=" + a.length
                    + " must be equal to x.length^2=" + (long) n * (long) n);
//        System.out.print("Gauss solving " + n  + "x" + n + "... ");
//        long t1 = System.nanoTime();
//        printEquationsSet(null, a, y);

        // Elimination algorithm
        for (int k = 0; k < n - 1; k++) {
            // Let, for example, k=2. Now we have the following equations set (below m=this.n-1):
            //     a00*x0 + a01*x1 + a02*x2 + a03*x3 + ... + a0m*xm = y0
            //              a11*x1 + a12*x2 + a13*x3 + ... + a1m*xm = y1
            //                       a22*x2 + a23*x3 + ... + a2m*xm = y2
            //                       a32*x2 + a33*x3 + ... + a3m*xm = y3
            //                       a42*x2 + a43*x3 + ... + a4m*xm = y4
            //                                        . . .
            //                       am2*x2 + am3*x3 + ... + amm*xm = ym
            // Finding the pivot - the maximal |aik|, i=k,k+1,...,n-1:
            final int aOfsDiagonal = k * n + k;
            int pivotIndex = k;
            double pivot = a[aOfsDiagonal], pivotAbs = StrictMath.abs(pivot);
            for (int i = k + 1, aOfs = aOfsDiagonal + n; i < n; i++, aOfs += n) {
                double v = a[aOfs], vAbs = StrictMath.abs(v);
                if (vAbs > pivotAbs) {
                    pivot = v;
                    pivotAbs = vAbs;
                    pivotIndex = i;
                }
            }
            if (pivotAbs == 0.0) {
                // Little optimization: avoiding useless futher calculations
                for (int j = 0; j < n; j++) {
                    x[j] = Double.NaN;
                }
                return;
            }
            // Exchanging the line #k and line #i: this operation does not change the order of the unknowns
            if (pivotIndex != k) {
                double temp = y[k];
                y[k] = y[pivotIndex];
                y[pivotIndex] = temp;
                for (int j = k, aOfsK = aOfsDiagonal, aOfs = pivotIndex * n + k; j < n; j++, aOfsK++, aOfs++) {
                    // Don't exchange first k elements in the lines: they are already zero
                    temp = a[aOfsK];
                    a[aOfsK] = a[aOfs];
                    a[aOfs] = temp;
                }
            }
            assert a[aOfsDiagonal] == pivot : "Pivot element is not placed to the correct place (k = " + k + ")";
            // Now |aik|<=|akk| for all i>k.
            // Eliminating - subtracting the line #k, multiplied by aik/akk, from all lines #i
            for (int i = k + 1, aOfs = k * n + n; i < n; i++) { // aOfs refers to the line begin!
                double q = a[aOfs + k] / pivot;
                y[i] -= y[k] * q;
                for (int j = 0; j < k; j++, aOfs++) {
                    assert a[aOfs] == 0.0 : "The line was not filled by zero in previous iterations 0,...," + (k - 1);
                }
                a[aOfs++] = 0.0; // not necessary, but the resulting matrix will be "better" (triangular)
                int aOfsK = aOfsDiagonal + 1;
                assert aOfs == i * n + k + 1;
                assert aOfsK == k * n + k + 1;
                for (int j = k + 1; j < n; j++, aOfsK++, aOfs++) {
                    a[aOfs] -= a[aOfsK] * q;
                }
            }
        }

        // Now A is triangular:
        //     a00*x0 + a01*x1 + a02*x2 + a03*x3 + ... + a0m*xm = y0
        //              a11*x1 + a12*x2 + a13*x3 + ... + a1m*xm = y1
        //                       a22*x2 + a23*x3 + ... + a2m*xm = y2
        //                                a33*x3 + ... + a3m*xm = y3
        //                                        . . .
        //                                               amm*xm = ym
        // Finding the unknowns (x):
        for (int i = n - 1, aOfs = n * n - 1; i >= 0; i--) {
            assert aOfs == i * n + n - 1;
            double v = y[i];
            for (int j = n - 1; j > i; j--, aOfs--) {
                v -= a[aOfs] * x[j];
            }
            x[i] = v / a[aOfs];
            aOfs -= i + 1;
        }

//        printEquationsSet(x, a, y);
//        long t2 = System.nanoTime();
//        System.out.println("done in " + (t2 - t1) * 1e-3 + " mcs = n^3*" + (t2 - t1) / Math.pow(n, 3) + " ns");
    }

    // Not used in current version. Very slow solution for matrices >3x3.
    // The private "determinant" method is moved into Matrices class.
//    static void solveLinearEquationsSetByCramerMethod(double[] x, double[] a, double[] y) {
//        final int n = y.length;
//        long t1 = System.nanoTime();
//        System.out.print("Cramer solving " + n  + "x" + n + "...");
//        double det = determinant(n, a);
//        double[] m = new double[a.length];
//        for (int k = 0; k < n; k++) {
//            for (int i = 0, aOfs = 0, mOfs = 0; i < n; i++) {
//                for (int j = 0; j < n; j++, aOfs++, mOfs++) {
//                    m[mOfs] = j == k ? y[i] : a[aOfs];
//                }
//            }
//            x[k] = determinant(n, m) / det;
//        }
//        long t2 = System.nanoTime();
//        System.out.println(" done in " + (t2 - t1) * 1e-3 + " mcs = n^3*" + (t2 - t1) / Math.pow(n, 3) + " ns");
//    }


    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        boolean shift = isShift();
        StringBuilder sA = new StringBuilder();
        if (isDiagonal()) {
            if (diagonal != null) {
                sA.append("diag[");
                for (int k = 0; k < diagonal.length; k++) {
                    if (k > 0)
                        sA.append(",");
                    sA.append(LinearFunc.goodFormat(diagonal[k]));
                }
                sA.append("]");
            }
        } else {
            sA.append("A");
        }
        StringBuilder sB = new StringBuilder();
        for (int k = 0; k < n; k++) {
            if (k > 0)
                sB.append(",");
            sB.append(LinearFunc.goodFormat(b[k]));
        }
        return "linear (affine) " + n + "-dimensional operator "
                + sA + "x+b, b=(" + sB + ")" + (shift ? " (shift)" : "");
    }

    static void printEquationsSet(double[] x, double[] a, double[] y) {
        final int n = y.length;
        System.out.println();
        for (int i = 0; i < n; i++) {
            double v = 0.0;
            for (int j = 0; j < n; j++) {
                if (x != null) {
                    v += a[i * n + j] * x[j];
                }
                System.out.printf("%10.6f*x[%d] " + (j < n - 1 ? "+ " : "= "), a[i * n + j], j);
            }
            System.out.printf("%.7f", y[i]);
            if (x != null)
                System.out.printf(" (error %.7f)", v - y[i]);
            System.out.println();
        }
    }

    /**
     * The simplest test for this class: finds a linear operator by pairs of points.
     */
    static class Test extends ProjectiveOperator.Test {
        @Override
        int numberOfPoints(int dimCount) {
            return dimCount + 1;
        }

        @Override
        CoordinateTransformationOperator getOperator() {
            return LinearOperator.getInstanceByPoints(q, p);
        }

        void badLinearEquationsSetTest() {
            double factorial = 1.0;
            for (int k = 2; k <= 16; k++) {
                factorial *= k;
            }
            double[] a = new double[dimCount * dimCount];
            for (int i = 1, aOfs = 0; i <= dimCount; i++) {
                for (int j = 1; j <= dimCount; j++, aOfs++) {
                    a[aOfs] = factorial / (i + j);
                }
            }
            LinearOperator lo = LinearOperator.getInstance(a, new double[dimCount]);
            newRndPoints(10, dimCount);
            for (int k = 0; k < p.length; k++) {
                double[] srcPoint = p[k].coordinates();
                double[] destPoint = new double[srcPoint.length];
                q[k] = p[k];
                lo.map(destPoint, srcPoint);
                lo.inverseMap(srcPoint, destPoint);
                r[k] = Point.valueOf(srcPoint);
            }
            double error = maxDiff();
            System.out.println("Difference for \"bad\" equation set " + dimCount + "*" + dimCount + " = " + error);
            LinearOperator lo2 = LinearOperator.getInstance(a, new double[dimCount]);
            if (!lo.equals(lo2))
                throw new AssertionError("Error in equals");
            ProjectiveOperator po = ProjectiveOperator.getInstance(a, lo.b(), new double[dimCount], 1.0);
            if (!lo.equals(po))
                throw new AssertionError("Error in equals");
        }

        public static void main(String[] args) {
            Test test = new Test();
            test.init(args);
            test.badLinearEquationsSetTest();
            test.mainTest();
        }
    }
}
