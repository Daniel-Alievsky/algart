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

import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Arrays;

/**
 * <p>Projective operator (projective transformation):
 * <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>y</b>),
 * <i>y</i><sub><i>i</i></sub> =
 * (<b>a</b><sub><i>i</i></sub><b>x</b> + <i>b</i><sub><i>i</i></sub>) / (<b>cx</b> + <i>d</i>)
 * (<b>a</b><sub><i>i</i></sub> means the line <i>i</i> of the matrix <b>A</b>),
 * where the numeric <i>n</i>&nbsp;x&nbsp;<i>n</i> matrix <b>A</b>,
 * the <i>n</i>-dimensional vectors <b>b</b> and <b>c</b> and the number <i>d</i>
 * are parameters of the transformation.
 * In other words, the argument of the function, the vector <b>x</b>, is mapped to the following vector <b>y</b>:</p>
 *
 * <blockquote>
 * <table>
 * <caption>&nbsp;</caption>
 * <tr>
 * <td><b>y</b> = </td>
 * <td><i>y</i><sub>0</sub><br><i>y</i><sub>1</sub><br>...<br><i>y</i><sub><i>n</i>&minus;1</sub></td>
 * <td> = </td>
 * <td>
 * <p>
 *   (<b>A</b><sub>0</sub><b>x</b> + <i>b</i><sub>0</sub>) / (<b>cx</b> + <i>d</i>)<br>
 *   (<b>A</b><sub>1</sub><b>x</b> + <i>b</i><sub>1</sub>) / (<b>cx</b> + <i>d</i>)<br>
 *   ...<br>
 *   (<b>A</b><sub><i>n</i>&minus;1</sub><b>x</b> + <i>b</i><sub><i>n</i>&minus;1</sub>) / (<b>cx</b> + <i>d</i>)
 * </td>
 * <td> = </td>
 * <td>
 * <p>
 *   (<i>a</i><sub>00</sub><i>x</i><sub>0</sub> + <i>a</i><sub>01</sub><i>x</i><sub>1</sub> + ...
 *     + <i>a</i><sub>0,<i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> + <i>b</i><sub>0</sub>) /
 *     (<i>c</i><sub>0</sub><i>x</i><sub>0</sub> + <i>c</i><sub>1</sub><i>x</i><sub>1</sub> + ...
 *     + <i>c</i><sub><i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> + <i>d</i>)<br>
 *   (<i>a</i><sub>10</sub><i>x</i><sub>0</sub> + <i>a</i><sub>11</sub><i>x</i><sub>1</sub> + ...
 *     + <i>a</i><sub>1,<i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> + <i>b</i><sub>1</sub>) /
 *     (<i>c</i><sub>0</sub><i>x</i><sub>0</sub> + <i>c</i><sub>1</sub><i>x</i><sub>1</sub> + ...
 *     + <i>c</i><sub><i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> + <i>d</i>)<br>
 *   ...<br>
 *   (<i>a</i><sub><i>n</i>&minus;1,0</sub><i>x</i><sub>0</sub>
 *     + <i>a</i><sub><i>n</i>&minus;1,1</sub><i>x</i><sub>1</sub> + ...
 *     + <i>a</i><sub><i>n</i>&minus;1,<i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub>
 *     + <i>b</i><sub><i>n</i>&minus;1</sub>) /
 *     (<i>c</i><sub>0</sub><i>x</i><sub>0</sub> + <i>c</i><sub>1</sub><i>x</i><sub>1</sub> + ...
 *     + <i>c</i><sub><i>n</i>&minus;1</sub><i>x</i><sub><i>n</i>&minus;1</sub> + <i>d</i>)
 * </td>
 * </tr>
 * </table>
 * </blockquote>
 *
 * <p>However, please note: we do not guarantee that the divisions in the formulas above are performed strictly
 * by "<code>c=a/b</code>" Java operator.
 * They are possibly performed via the following code: "<code>temp=1.0/b; c=a*temp;</code>"
 * The difference here is very little and not important for most practical needs.</p>
 *
 * <p>Please note: if <b>c</b> vector is zero (all <i>c</i><sub><i>i</i></sub>=0) &mdash;
 * in other words, if this transformation is really affine &mdash; then an instance of this class
 * is always an instance of its inheritor {@link LinearOperator}.
 * This rule is provided by the instantiation methods
 * {@link #getInstance(double[], double[], double[], double)} getInstance} and
 * {@link #getInstanceByPoints(net.algart.math.Point[], net.algart.math.Point[])} getInstanceByPoints}.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see LinearOperator
 */
public class ProjectiveOperator
        extends AbstractCoordinateTransformationOperator
        implements CoordinateTransformationOperator {
    final double[] a; // null if diagonal (in particular, identity)
    final double[] diagonal; // null if non-diagonal or identity
    final double[] b;
    final double[] c; // null if all c[i]=0 (affine transformation)
    final double d;
    final int n;
    final boolean zeroB;

    ProjectiveOperator(double[] a, double[] diagonal, double[] b, double[] c, double d) {
        assert b != null;
        if (b.length == 0) {
            throw new IllegalArgumentException("Empty b vector (no coordinates)");
        }
        if (a != null && a.length != (long) b.length * (long) b.length) {
            throw new IllegalArgumentException("Illegal size of A matrix: a.length=" + a.length
                    + " must be equal to b.length^2=" + (long) b.length * (long) b.length);
        }
        if (diagonal != null && diagonal.length != b.length) {
            throw new IllegalArgumentException("b and diagonal vector lengths mismatch: diagonal.length="
                    + diagonal.length + ", b.length=" + b.length);
        }
        if (c != null) {
            if (c.length != b.length) {
                throw new IllegalArgumentException("b and c vector lengths mismatch: b.length="
                        + b.length + ", c.length=" + c.length);
            }
        }
        this.n = b.length;
        if (a != null) {
            boolean isDiag = true;
            boolean unitDiag = true;
            for (int i = 0, disp = 0; i < this.n; i++) {
                for (int j = 0; j < this.n; j++, disp++) {
                    if (i == j) {
                        unitDiag &= a[disp] == 1.0;
                    } else {
                        isDiag &= a[disp] == 0.0;
                    }
                }
            }
            if (isDiag) {
                if (!unitDiag) {
                    diagonal = new double[this.n];
                    for (int i = 0, disp = 0; i < this.n; i++, disp += this.n + 1) {
                        diagonal[i] = a[disp];
                    }
                }
                a = null;
            }
        } else if (diagonal != null) {
            boolean unitDiag = true;
            for (double v : diagonal) {
                unitDiag &= v == 1.0;
            }
            if (unitDiag) {
                diagonal = null;
            }
        }
        this.a = a;
        this.diagonal = diagonal;
        this.b = b;
        boolean zeroC = true;
        if (c != null) {
            for (double v : c) {
                zeroC &= v == 0.0;
            }
        }
        this.c = zeroC ? null : c;
        this.d = d;
        boolean zeroB = true;
        for (double v : b) {
            zeroB &= v == 0.0;
        }
        this.zeroB = zeroB;
    }

    /**
     * Returns an instance of this class, describing the projective operator with the specified matrix <b>A</b>,
     * the vectors <b>b</b> and <b>c</b> and the number <i>d</i>.
     * See the {@link ProjectiveOperator comments to this class} for more details.
     * The coordinates of the vectors <b>b</b> and <b>c</b> must be listed in <code>b</code> and <code>c</code> arrays.
     * The elements of the matrix <b>A</b> must be listed, row by row, in the <code>a</code> array:
     * <b>A</b>={<i>a</i><sub><i>ij</i></sub>},
     * <i>a</i><sub><i>ij</i></sub>=<code>a[<i>i</i>*<i>n</i>+<i>j</i>]</code>,
     * <i>i</i> is the index of the row (0..<i>n</i>-1),
     * <i>j</i> is the index of the column (0..<i>n</i>-1),
     * <i>n</i>=<code>b.length</code>.
     * The lengths of <code>b</code> and <code>c</code> arrays must be the same:
     * <code>b.length</code>=<code>c.length</code>=<i>n</i>.
     * The length <code>a.length</code> of the <code>a</code> array must be equal to its square <i>n</i><sup>2</sup>.
     * Empty arrays (<i>n</i>=0) are not allowed.
     *
     * <p>Please note: the returned operator can have another <b>A</b>, <b>b</b>, <b>c</b>, <i>d</i> parameters
     * (returned by {@link #a()}, {@link #b()}, {@link #c()}, {@link #d()} methods),
     * than specified in the arguments of this method.
     * Namely, all these numbers can be multiplied by some constant: such modification does not change
     * the projective transformation.
     *
     * <p>In particular, if the arguments of this method really describe
     * an affine transformation (<b>c</b>=<b>0</b>), then this method
     * returns an instance of {@link LinearOperator} class, where all elements of <b>A</b> matrix
     * and <b>b</b> vector are divided by <i>d</i> number.
     *
     * <p>The passed <code>a</code>, <code>b</code> and <code>c</code> Java arrays are cloned by this method:
     * no references to them are maintained by the created instance.
     *
     * @param a the elements of <b>A</b> matrix.
     * @param b the coordinates of <b>b</b> vector.
     * @param c the coordinates of <b>c</b> vector.
     * @param d the <i>d</i> parameter.
     * @return the projective operator described by these parameters.
     * @throws NullPointerException     if one of the arguments of the method is {@code null}.
     * @throws IllegalArgumentException if <code>b.length==0</code>, <code>c.length==0</code>,
     *                                  <code>b.length!=c.length</code>
     *                                  or <code>a.length!=b.length<sup>2</sup></code>.
     */
    public static ProjectiveOperator getInstance(double[] a, double[] b, double[] c, double d) {
        Objects.requireNonNull(a, "Null A matrix");
        Objects.requireNonNull(b, "Null b vector");
        Objects.requireNonNull(c, "Null c vector");
        if (c.length != b.length) {
            throw new IllegalArgumentException("b and c vector lengths mismatch: b.length="
                    + b.length + ", c.length=" + c.length);
        }
        if (a.length != ((long) b.length) * ((long) b.length)) {
            throw new IllegalArgumentException("Illegal size of A matrix: a.length=" + a.length
                    + " must be equal to b.length^2=" + (b.length * b.length));
        }
        // to be on the safe side, we check all this before following operations - we not wait for the constructor
        boolean zeroC = true;
        for (double v : c) {
            zeroC &= v == 0.0;
        }
        if (zeroC) {
            a = a.clone();
            b = b.clone();
            if (d != 1.0) {
                for (int k = 0; k < a.length; k++) {
                    a[k] /= d;
                }
                for (int k = 0; k < b.length; k++) {
                    b[k] /= d;
                }
            }
            return new LinearOperator(a, null, b);
        }
        return new ProjectiveOperator(a.clone(), null, b.clone(), c.clone(), d);
    }

    /**
     * Returns the n-dimensional projective operator, that transforms (maps)
     * the given <i>n</i>+2 points
     * <b>p</b><sub>0</sub>, <b>p</b><sub>1</sub>, ..., <b>p</b><sub><i>n</i>+1</sub> to
     * the given another <i>n</i>+2 points
     * <b>q</b><sub>0</sub>, <b>q</b><sub>1</sub>, ..., <b>q</b><sub><i>n</i>+1</sub>
     * of the n-dimensional space. The parameter <i>d</i> in the returned operator is 1.0.
     * In other words, the matrix <b>A</b>, the vectors <b>b</b>, <b>c</b> and the parameter <i>d</i>
     * in the returned operator fulfil the following conditions:
     *
     * <blockquote>
     * <table>
     * <caption>&nbsp;</caption>
     * <tr>
     * <td><i>d</i> = 1.0;</td>
     * </tr>
     * <tr>
     * <td>
     * <p>
     *   (<b>A</b><sub>0</sub><b>p</b><sub><i>k</i></sub> + <i>b</i><sub>0</sub>) /
     *   (<b>cp</b><sub><i>k</i></sub> + 1)<br>
     *   (<b>A</b><sub>1</sub><b>p</b><sub><i>k</i></sub> + <i>b</i><sub>1</sub>) /
     *   (<b>cp</b><sub><i>k</i></sub> + 1)<br>
     *   ...<br>
     *   (<b>A</b><sub><i>n</i>&minus;1</sub><b>p</b><sub><i>k</i></sub> + <i>b</i><sub><i>n</i>&minus;1</sub>) /
     *   (<b>cp</b><sub><i>k</i></sub> + 1)
     * </td>
     * <td> = </td>
     * <td><i>q</i><sub><i>k</i>0</sub><br><i>q</i><sub><i>k</i>1</sub><br>...<br>
     *   <i>q</i><sub><i>k</i>,<i>n</i>&minus;1</sub></td>
     * <td> = <b>q</b><sub><i>k</i></sub> for <i>k</i> = 0, 1, ..., <i>n</i>+1</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * <p>(<b>a</b><sub><i>i</i></sub> means the line <i>i</i> of the matrix <b>A</b>).
     * It is possible that there is no such operator
     * or there are many different solutions (degenerated cases).
     * In this case, this method still returns some operator, but some coefficients of <b>A</b> matrix,
     * <b>b</b> and <b>c</b> vectors in the returned operator will probably be <code>Double.NaN</code>,
     * <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code>.
     *
     * <p>All passed points must be <i>n</i>-dimensional,
     * where <i>n</i><code>+2=p.length=q.length</code>.
     *
     * @param q the destination points.
     * @param p the source points.
     * @return the <i>n</i>-dimensional projective operator, which maps <b>p</b><sub><i>i</i></sub> to
     * <b>q</b><sub><i>i</i></sub> for all <i>i</i>=0,1,2,...,<i>n</i>+1.
     * @throws NullPointerException     if one of arguments of this method or one of elements of
     *                                  <code>p</code> and <code>q</code> arrays {@code null}.
     * @throws IllegalArgumentException if the lengths of the passed <code>p</code> and
     *                                  <code>q</code> arrays are not equal,
     *                                  or if for some <code>k</code>
     *                                  <code>p[k].{@link Point#coordCount() coordCount()}!=p.length-2</code> or
     *                                  <code>q[k].{@link Point#coordCount() coordCount()}!=p.length-2</code>.
     * @throws OutOfMemoryError         if there is not enough Java memory for storing Java array
     *                                  <code>double[n*(n+2)*n*(n+2)]</code>,
     *                                  where <code>n+2=p.length</code>,
     *                                  or if <code>n*(n+2)*n*(n+2)&gt;Integer.MAX_VALUE</code>.
     */
    public static ProjectiveOperator getInstanceByPoints(Point[] q, Point[] p) {
        Objects.requireNonNull(p, "Null p argument");
        Objects.requireNonNull(q, "Null q argument");
        if (p.length != q.length) {
            throw new IllegalArgumentException("p and q point arrays lengths mismatch: p.length=" +
                    p.length + ", q.length=" + q.length);
        }
        if (p.length == 0) {
            throw new IllegalArgumentException("Empty p and q arrays");
        }
        final int n = p.length - 2;
        long numberOfUnknowns = (long) n * (long) (n + 2); // number of unknowns
        if (numberOfUnknowns > Integer.MAX_VALUE || numberOfUnknowns * numberOfUnknowns > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Too large necessary matrix (more than Integer.MAX_VALUE elements)");
        }
        for (int k = 0; k < p.length; k++) {
            if (p[k].coordCount() != n) {
                throw new IllegalArgumentException("n+2 n-dimensional points are necessary to "
                        + "find the projective operator, but we have " + (n + 2) + " points, "
                        + "and the source point #" + k + " is " + p[k].coordCount() + "-dimensional");
            }
            if (q[k].coordCount() != n) {
                throw new IllegalArgumentException("n+2 n-dimensional points are necessary to "
                        + "find the projective operator, but we have " + (n + 2) + " points, "
                        + "and the destination point #" + k + " is " + q[k].coordCount() + "-dimensional");
            }
        }
        // px0*a00 + py0*a01 + pz0*a02 + ... + bx        - qx0*px0*cx - qx0*py0*cy - qx0*pz0*cz* - ... = qx0 (*d=1)
        // px1*a00 + py1*a01 + pz1*a02 + ... + bx        - qx1*px1*cx - qx1*py1*cy - qx1*pz1*cz* - ... = qx1
        // px2*a00 + py2*a01 + pz2*a02 + ... + bx        - qx2*px2*cx - qx2*py2*cy - qx2*pz2*cz* - ... = qx2
        // ...
        //     px0*a10 + py0*a11 + pz0*a12 + ... + by    - qy0*px0*cx - qy0*py0*cy - qy0*pz0*cz* - ... = qy0 (*d=1)
        //     px1*a10 + py1*a11 + pz1*a12 + ... + by    - qy1*px1*cx - qy1*py1*cy - qy1*pz1*cz* - ... = qy1
        //     px2*a10 + py2*a11 + pz2*a12 + ... + by    - qy2*px2*cx - qy2*py2*cy - qy2*pz2*cz* - ... = qy2
        // etc.
        // In other words, here is m*m equation system Sv=t, m=(n+2)*n,
        // for finding m unknowns a00, a01, ..., a10, a11, ..., bx, by, ..., cx, cy, ....
        final int m = (int) numberOfUnknowns;
        double[] s = new double[m * m]; // zero-filled
        double[] t = new double[m];
        double[] v = new double[m];
        for (int i = 0, k = 0; i < n; i++) {
            for (int pointIndex = 0; pointIndex < p.length; pointIndex++, k++) {
                // filling a line of S matrix and an element of t vector #k=0,1,...,m-1
                for (int j = 0, sOfs = k * m + i * n; j < n; j++, sOfs++) {
                    s[sOfs] = p[pointIndex].coord(j); // coefficient for aIJ
                }
                s[k * m + n * n + i] = 1.0; // coefficient for bx, by, ...
                for (int j = 0, sOfs = k * m + n * n + n; j < n; j++, sOfs++) {
                    s[sOfs] = -q[pointIndex].coord(i) * p[pointIndex].coord(j); // coefficient for cx, cy, ...
                }
                t[k] = q[pointIndex].coord(i);
            }
        }
//        for (int i = 0; i < m; i++) {
//            for (int j = 0; j < n * n; j++) {
//                System.out.printf("%7.3f ", s[i * m + j]);
//            }
//            System.out.print(" | ");
//            for (int j = 0; j < n; j++) {
//                System.out.printf("%7.3f ", s[i * m + n * n + j]);
//            }
//            System.out.print(" | ");
//            for (int j = 0; j < n; j++) {
//                System.out.printf("%7.3f ", s[i * m + n * n + n + j]);
//            }
//            System.out.printf("= %.7f%n", t[i]);
//        }
        LinearOperator.solveLinearEquationsSet(v, s, t);
//        LinearOperator.solveLinearEquationsSetByCramerMethod(v, s, t);
        assert v.length == m;
        double[] a = new double[n * n];
        double[] b = new double[n];
        double[] c = new double[n];
        System.arraycopy(v, 0, a, 0, n * n);
        System.arraycopy(v, n * n, b, 0, n);
        System.arraycopy(v, n * n + n, c, 0, n);
        return getInstance(a, b, c, 1.0);
    }

    /**
     * Returns an array containing <b>A</b> matrix.
     *
     * <p>The returned array is always newly created: it is not a reference
     * to some internal data stored in this object.
     *
     * @return <b>A</b> matrix.
     * @throws OutOfMemoryError if this instance was created by some creation method of
     *                          the {@link LinearOperator} class,
     *                          besides {@link LinearOperator#getInstance(double[], double[])},
     *                          and the matrix is too large to be stored in Java memory
     *                          or its size is greater than <code>Integer.MAX_VALUE</code>.
     */
    public final double[] a() {
        if (a != null) {
            return a.clone();
        }
        if ((long) n * (long) n > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Too large matrix A (more than Integer.MAX_VALUE elements)");
        }
        double[] result = new double[n * n];
        for (int i = 0, disp = 0; i < n; i++, disp += n + 1) {
            result[disp] = diagonal == null ? 1.0 : diagonal[i];
        }
        return result;
    }

    /**
     * Returns an array containing <b>b</b> vector.
     *
     * <p>The returned array is always newly created: it is not a reference
     * to some internal data stored in this object.
     *
     * @return <b>b</b> vector.
     */
    public final double[] b() {
        return b.clone();
    }

    /**
     * Returns an array containing <b>c</b> vector.
     * In a case of {@link LinearOperator}, the result is always zero-filled.
     *
     * <p>The returned array is always newly created: it is not a reference
     * to some internal data stored in this object.
     *
     * @return <b>c</b> vector.
     */
    public final double[] c() {
        return c == null ? new double[n] : c.clone();
    }

    /**
     * Returns the <i>d</i> parameter.
     * In a case of {@link LinearOperator}, the result is always 0.0.
     *
     * @return <b>d</b> parameter.
     */
    public final double d() {
        return d;
    }

    /**
     * Returns an array containing the main diagonal of <b>A</b> matrix.
     *
     * <p>The returned array is always newly created: it is not a reference
     * to some internal data stored in this object.
     *
     * @return the main diagonal of <b>A</b> matrix.
     */
    public final double[] diagonal() {
        if (diagonal != null) {
            return diagonal.clone();
        }
        double[] result = new double[n];
        if (a == null) {
            for (int i = 0; i < n; i++) {
                result[i] = 1.0;
            }
        } else {
            for (int i = 0, disp = 0; i < n; i++, disp += n + 1) {
                result[i] = a[i];
            }
        }
        return result;
    }

    /**
     * Returns the number of dimensions.
     * The result is equal to the number of components in the <b>b</b> and <b>c</b> vectors.
     *
     * @return the number of dimensions.
     */
    public final int n() {
        return n;
    }

    /**
     * Returns <code>true</code> if and only if <b>A</b> matrix is diagonal,
     * i&#46;e&#46; if <i>a</i><sub><i>ij</i></sub>=0.0 when <i>i</i>!=<i>j</i>.
     *
     * @return <code>true</code> if and only if <b>A</b> matrix is diagonal.
     */
    public final boolean isDiagonal() {
        return a == null;
    }

    /**
     * Returns <code>true</code> if and only if <b>A</b> matrix is identity
     * (i&#46;e&#46; if <i>a</i><sub><i>ij</i></sub>=0.0 when <i>i</i>!=<i>j</i> and
     * <i>a</i><sub><i>ij</i></sub>=1.0 when <i>i</i>==<i>j</i>)
     * and <b>c</b> vector is zero.
     * In this case, this operator corresponds to a parallel shift.
     * In this case, this object is always an instance of {@link LinearOperator}.
     *
     * @return <code>true</code> if and only if this operator describes a parallel shift in the space.
     */
    public final boolean isShift() {
        boolean result = a == null && diagonal == null && c == null;
        if (result && !(this instanceof LinearOperator)) {
            throw new AssertionError("Shift operator must be an instance of " + LinearOperator.class);
        }
        return result;
    }

    /**
     * Returns <code>true</code> if and only if the <b>b</b> vector is zero,
     * i&#46;e&#46; if <i>b</i><sub><i>i</i></sub>=0.0 for all <i>i</i>.
     * If <code>{@link #isZeroB()}&nbsp;&amp;&amp;&nbsp;{@link #isShift()}</code>,
     * this operator is identity: it doesn't change the passed function.
     *
     * @return <code>true</code> if and only if the <b>b</b> vector is zero.
     */
    public final boolean isZeroB() {
        return zeroB;
    }


    /**
     * This implementation calculates <code>destPoint</code> by the formula
     * <i>y</i><sub><i>i</i></sub> =
     * (<b>a</b><sub><i>i</i></sub><b>x</b> + <i>b</i><sub><i>i</i></sub>) / (<b>cx</b> + <i>d</i>),
     * where <b>x</b>=<code>srcPoint</code> and <b>y</b>=<code>destPoint</code>.
     * See more details in the comments to {@link ProjectiveOperator this class}.
     *
     * @param destPoint the coordinates of the destinated point <b>y</b>, filled by this method.
     * @param srcPoint  the coordinates of the source point <b>x</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>destPoint.length</code> or <code>srcPoint.length</code>
     *                                  is not equal to the {@link #n() number of dimensions}.
     */
    public void map(double[] destPoint, double[] srcPoint) {
        calculateAxPlusB(destPoint, srcPoint);
        double divisor = d;
        if (c != null) { // to be on the safe side: null is impossible in the current implementation
            for (int i = 0; i < c.length; i++) {
                divisor += c[i] * srcPoint[i];
            }
        }
        if (divisor != 1.0) {
            double multiplier = 1.0 / divisor;
            for (int i = 0; i < destPoint.length; i++) {
                destPoint[i] *= multiplier;
            }
        }
    }

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
                    if (k > 0) {
                        sA.append(",");
                    }
                    sA.append(LinearFunc.goodFormat(diagonal[k]));
                }
                sA.append("]");
            }
        } else {
            sA.append("A");
        }
        StringBuilder sB = new StringBuilder();
        for (int k = 0; k < n; k++) {
            if (k > 0) {
                sB.append(",");
            }
            sB.append(LinearFunc.goodFormat(b[k]));
        }
        StringBuilder sC = new StringBuilder();
        if (c != null) { // to be on the safe side: null is impossible in the current implementation
            for (int k = 0; k < c.length; k++) {
                if (k > 0) {
                    sC.append(",");
                }
                sC.append(LinearFunc.goodFormat(c[k]));
            }
        }
        return "projective " + n + "-dimensional operator ("
                + sA + "x+b)/(cx+d), b=(" + sB + "), c=(" + sC + "), d=" + LinearFunc.goodFormat(d)
                + (shift ? " (shift)" : "");
    }

    public int hashCode() {
        int result = (a != null ? Arrays.hashCode(a) : 0);
        result = 37 * result + (diagonal != null ? Arrays.hashCode(diagonal) : 0);
        result = 37 * result + Arrays.hashCode(b);
        result = 37 * result + (c != null ? Arrays.hashCode(c) : 0);
        result = 37 * result + Double.hashCode(d);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProjectiveOperator po)) {
            return false;
        }
        if (n != po.n) {
            return false;
        }
        if (d != po.d) {
            return false;
        }
        if ((a == null) == (po.a != null)) {
            return false;
        }
        if (a != null) {
            for (int k = 0; k < a.length; k++) {
                if (a[k] != po.a[k]) {
                    return false;
                }
            }
        }
        if ((diagonal == null) == (po.diagonal != null)) {
            return false;
        }
        if (diagonal != null) {
            for (int k = 0; k < diagonal.length; k++) {
                if (diagonal[k] != po.diagonal[k]) {
                    return false;
                }
            }
        }
        for (int k = 0; k < b.length; k++) {
            if (b[k] != po.b[k]) {
                return false;
            }
        }
        if ((c == null) == (po.c != null)) {
            return false;
        }
        if (c != null) {
            for (int k = 0; k < c.length; k++) {
                if (c[k] != po.c[k]) {
                    return false;
                }
            }
        }
        return true;
    }

    void calculateAxPlusB(double[] destPoint, double[] srcPoint) {
        Objects.requireNonNull(destPoint, "Null destPoint");
        Objects.requireNonNull(srcPoint, "Null srcPoint");
        if (destPoint.length != n) {
            throw new IllegalArgumentException("Illegal length of destPoint array: "
                    + destPoint.length + " for " + this);
        }
        if (srcPoint.length != n) {
            throw new IllegalArgumentException("Illegal length of srcPoint array: "
                    + srcPoint.length + " for " + this);
        }
        if (a != null) {
            System.arraycopy(b, 0, destPoint, 0, destPoint.length);
            for (int i = 0, disp = 0; i < n; i++) {
                double sum = 0.0;
                for (int j = 0; j < n; j++, disp++) {
                    sum += a[disp] * srcPoint[j];
                }
                destPoint[i] = sum + b[i];
                // Note! It is possible here to change the order of summimg: calculate
                //     b[i] + a[disp]*srcPoint[0]+...
                // But such an order will lead to little different results than declared by Ax+b formula,
                // because real sums in a computer are not commutative.
                // For more accurate and expected results, we calculate this in a usual order.
            }
        } else if (diagonal != null) {
            for (int i = 0; i < n; i++) {
                destPoint[i] = diagonal[i] * srcPoint[i] + b[i];
            }
        } else {
            for (int i = 0; i < n; i++) {
                destPoint[i] = srcPoint[i] + b[i];
            }
        }
    }

    /**
     * The simplest test for this class: finds a linear operator by pairs of points.
     */
    static class Test {
        boolean verbose = false;
        int dimCount;
        int numberOfTests;
        long startSeed;
        Random rnd;
        Point[] p, q, r;

        final void init(String[] args) {
            int startArgIndex = 0;
            if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-v")) {
                verbose = true;
                startArgIndex++;
            }
            if (args.length < startArgIndex + 2) {
                System.out.println("Usage: " + Test.class.getName() + " [-v] dimCount numberOfTests [randSeed]");
                System.exit(0);
            }
            this.dimCount = Integer.parseInt(args[startArgIndex]);
            this.numberOfTests = Integer.parseInt(args[startArgIndex]);
            if (args.length < startArgIndex + 3) {
                this.startSeed = new Random().nextLong();
            } else {
                this.startSeed = Long.parseLong(args[startArgIndex + 2]);
            }
            this.rnd = new Random(startSeed);
            System.out.printf(Locale.US, "%d tests, randSeed = %d%n", numberOfTests, startSeed);
        }

        final void mainTest() {
            long t1 = System.nanoTime();
            double maxError = 0.0;
            for (int testCount = 1; testCount <= numberOfTests; testCount++) {
                newRndPoints(numberOfPoints(dimCount), dimCount);
                CoordinateTransformationOperator o = getOperator();
                mapPoints(o);
                double error = maxDiff();
                maxError = Math.max(maxError, error);
                if (verbose) {
                    System.out.println(testCount + ": difference " + error + "; operator hash code " + o.hashCode());
                }
                if (error > 0.001) {
                    System.err.println(testCount + ": difference " + error +
                            " is BIG: " + o + " incorrectly maps " + java.util.Arrays.asList(p) + " to "
                            + java.util.Arrays.asList(r) + " instead of " + java.util.Arrays.asList(q));
                }
                CoordinateTransformationOperator o2 = getOperator();
                if (!o.equals(o2)) {
                    throw new AssertionError("Error in equals");
                }
                if (o2.hashCode() != o.hashCode()) {
                    throw new AssertionError("Error in hashCode");
                }
            }
            long t2 = System.nanoTime();
            System.out.printf(Locale.US,
                    "All tests done in %.3f seconds (%.2f mcs/test), maximal error = %g, randSeed = %d%n",
                    (t2 - t1) * 1e-9, (t2 - t1) * 1e-3 / numberOfTests, maxError, startSeed);
        }

        final void newRndPoints(int numberOfPoints, int dimCount) {
            p = new Point[numberOfPoints];
            q = new Point[numberOfPoints];
            r = new Point[numberOfPoints];
            double[] coordinates = new double[dimCount];
            for (int k = 0; k < p.length; k++) {
                for (int j = 0; j < dimCount; j++) {
                    coordinates[j] = rnd.nextDouble() - 0.5;
                }
                p[k] = Point.valueOf(coordinates);
                for (int j = 0; j < dimCount; j++) {
                    coordinates[j] = rnd.nextDouble() - 0.5;
                }
                q[k] = Point.valueOf(coordinates);
            }
        }

        final void mapPoints(CoordinateTransformationOperator o) {
            for (int k = 0; k < p.length; k++) {
                double[] srcPoint = p[k].coordinates();
                double[] destPoint = new double[srcPoint.length];
                o.map(destPoint, srcPoint);
                r[k] = Point.valueOf(destPoint);
            }
        }

        final double maxDiff() {
            double result = 0.0;
            for (int k = 0; k < p.length; k++) {
                result = Math.max(result, q[k].subtract(r[k]).distanceFromOrigin());
            }
            return result;
        }

        int numberOfPoints(int dimCount) {
            return dimCount + 2;
        }

        CoordinateTransformationOperator getOperator() {
            return ProjectiveOperator.getInstanceByPoints(q, p);
        }

        public static void main(String[] args) {
            Test test = new Test();
            test.init(args);
            test.mainTest();
        }
    }
}
