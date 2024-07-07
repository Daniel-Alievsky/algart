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

package net.algart.math.geometry;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;

/**
 * <p>Right orthonormal basis in 3D Euclidean space: 3 orthogonal unit vectors <b>i</b>, <b>j</b>, <b>k</b>.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public final class Orthonormal3DBasis {
    /**
     * While creating a new basis, two vectors are considered to be "almost collinear"
     * if the sine of the angle between them is less or about {@link #COLLINEARITY_EPSILON}.
     */
    public static final double COLLINEARITY_EPSILON = 1e-7;
    /**
     * The square of {@link #COLLINEARITY_EPSILON}.
     */
    public static final double COLLINEARITY_EPSILON_SQR = COLLINEARITY_EPSILON * COLLINEARITY_EPSILON;

    private static final int REVIVING_COUNT = 32;
    // - must be not too big: every rotation can replace |r| to |r|^2, so 50-100 iteration can lead to overflow

    /**
     * Minimal allowed length of the vectors (<code>ix</code>,<code>iy</code>,<code>iz</code>) and
     * (<code>jx</code>,<code>jy</code>,<code>jz</code>), passed to creation methods
     * {@link #newBasis(double, double, double, double, double, double, boolean)},
     * {@link #newSomeBasis(double, double, double)}.
     */
    public static final double MIN_ALLOWED_LENGTH = 1e-100;

    private static final AtomicInteger globalCallCount = new AtomicInteger();

    /**
     * Default basis: I=(1,0,0), J=(0,1,0), K=(0,0,1).
     */
    public static final Orthonormal3DBasis DEFAULT =
            new Orthonormal3DBasis(1, 0, 0, 0, 1, 0, 0, 0, 1, 0);

    private final double ix, iy, iz, jx, jy, jz, kx, ky, kz;
    private final int counter;

    private Orthonormal3DBasis(
            double ix, double iy, double iz,
            double jx, double jy, double jz,
            double kx, double ky, double kz,
            int counter) {
        if ((globalCallCount.incrementAndGet() & 0x3FF) == 0) {
            final double iSqr = ix * ix + iy * iy + iz * iz;
            final double jSqr = jx * jx + jy * jy + jz * jz;
            final double kSqr = kx * kx + ky * ky + kz * kz;
            final double ij = ix * jx + iy * jy + iz * jz;
            final double ik = ix * kx + iy * ky + iz * kz;
            final double jk = jx * kx + jy * ky + jz * kz;
//System.out.println("|I|=" + Math.sqrt(iSqr) + ", |J|=" + Math.sqrt(jSqr) + ", |K|=" + Math.sqrt(kSqr)
//+ "; IJ=" + ij + ", IK=" + ik + ", JK=" + jk + " I=(" + ix + "," + iy + "," + iz
//+ "),J=(" + jx + "," + jy + "," +jz
//+ "),K=(" + kx + "," + ky + "," + kz + ")");
            if (iSqr < 0.98 || iSqr > 1.02)
                throw new AssertionError("Internal error! |I| == " + Math.sqrt(iSqr) + " != 1.0");
            if (jSqr < 0.98 || jSqr > 1.02)
                throw new AssertionError("Internal error! |J| == " + Math.sqrt(jSqr) + " != 1.0");
            if (ij < -0.02 || ij > 0.02)
                throw new AssertionError("Internal error! IJ == " + ij + " != 0.0");
            if (kSqr < 0.9 || kSqr > 1.1)
                throw new AssertionError("Internal error! |K| == " + Math.sqrt(kSqr) + " != 1.0");
            if (ik < -0.1 || ik > 0.1)
                throw new AssertionError("Internal error! IK == " + ik + " != 0.0");
            if (jk < -0.1 || jk > 0.1)
                throw new AssertionError("Internal error! JK == " + jk + " != 0.0");
        }
        this.ix = ix;
        this.iy = iy;
        this.iz = iz;
        this.jx = jx;
        this.jy = jy;
        this.jz = jz;
        this.kx = kx;
        this.ky = ky;
        this.kz = kz;
        this.counter = counter;
    }

    private Orthonormal3DBasis(double ix, double iy, double iz, double jx, double jy, double jz, int counter) {
        this(ix, iy, iz, jx, jy, jz,
                iy * jz - iz * jy,
                iz * jx - ix * jz,
                ix * jy - iy * jx,
                counter);
        // vector K = [IJ]
    }

    /**
     * Creates new basis, where <b>i</b> vector has components
     * (<code>ix</code>/d, <code>iy</code>/d, <code>iz</code>/d),
     * d=<code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>). Other vectors <b>j</b> and <b>k</b>
     * will have some values, depending on the implementation.
     *
     * @param ix <i>x</i>-component of new <b>i</b> vector (maybe, multiplied by some <i>d</i> constant).
     * @param iy <i>y</i>-component of new <b>i</b> vector (maybe, multiplied by some <i>d</i> constant).
     * @param iz <i>z</i>-component of new <b>i</b> vector (maybe, multiplied by some <i>d</i> constant).
     * @return new right orthonormal basis with given direction of <b>i</b> vector.
     * @throws IllegalArgumentException if the length <code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>)
     *                                  of the passed vector (<code>ix</code>,<code>iy</code>,<code>iz</code>) is zero or
     *                                  too small (&lt; {@link #MIN_ALLOWED_LENGTH}).
     */
    public static Orthonormal3DBasis newSomeBasis(double ix, double iy, double iz) {
        final double xAbs = ix >= 0.0 ? ix : -ix;
        final double yAbs = iy >= 0.0 ? iy : -iy;
        final double zAbs = iz >= 0.0 ? iz : -iz;
        if (yAbs < zAbs) {
            if (xAbs < yAbs) {
                // So, normalized vector I=(ix,iy,iz)/sqrt(ix^2+iy^2+iz^2) is in sectors 45 degree
                // around axes Y and Z, strongly not collinear with (1,0,0):
                // CollinearityException in the following getInstance is impossible
                return newBasis(ix, iy, iz, 1.0, 0.0, 0.0, true);
            } else {
                // and so on...
                return newBasis(ix, iy, iz, 0.0, 1.0, 0.0, true);
            }
        } else {
            if (xAbs < zAbs) {
                return newBasis(ix, iy, iz, 1.0, 0.0, 0.0, true);
            } else {
                return newBasis(ix, iy, iz, 0.0, 0.0, 1.0, true);
            }
        }
    }

    /**
     * Creates new basis, where <b>i</b> vector has components
     * (<code>ix</code>/d<sub>1</sub>, <code>iy</code>/d<sub>1</sub>, <code>iz</code>/d<sub>1</sub>),
     * d<sub>1</sub>=<code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>),
     * <b>j</b> vector has components
     * (<code>jx</code>/d<sub>2</sub>, <code>jy</code>/d<sub>2</sub>, <code>jz</code>/d<sub>2</sub>),
     * d<sub>2</sub>=<code>sqrt</code>(jx<sup>2</sup>+jy<sup>2</sup>+jz<sup>2</sup>),
     * <b>k</b> vector is chosen automatically to provide right orthonormal basis.
     *
     * <p>If the passed vectors (<code>ix</code>,<code>iy</code>,<code>iz</code>) and
     * (<code>jx</code>,<code>jy</code>,<code>jz</code>)
     * are not orthogonal, the vector (<code>jx</code>,<code>jy</code>,<code>jz</code>) is automatically corrected,
     * before all other calculations, to become orthogonal to <b>i</b>, and the plane <b>ij</b> is preserved
     * while this correction.
     *
     * <p>If the passed vectors are collinear or almost collinear (with very little angle difference,
     * about 10<sup>&minus;8</sup>..10<sup>&minus;6</sup> radians or something like this),
     * or if the length of one of the passed vectors is less than {@link #MIN_ALLOWED_LENGTH},
     * then behaviour depends on <code>exceptionOnCollinearity</code> argument. If it is <code>true</code>,
     * the method throws {@link CollinearityException}. In other case, the method ignores the passed
     * vector (<code>jx</code>,<code>jy</code>,<code>jz</code>) and returns some basis according the passed vector
     * (<code>ix</code>,<code>iy</code>,<code>iz</code>), as {@link #newSomeBasis(double, double, double)} method.
     *
     * @param ix                      <i>x</i>-component of new <b>i</b> vector
     *                                (maybe, multiplied by some <i>d</i><sub>1</sub> constant).
     * @param iy                      <i>y</i>-component of new <b>i</b> vector
     *                                (maybe, multiplied by some <i>d</i><sub>1</sub> constant).
     * @param iz                      <i>z</i>-component of new <b>i</b> vector
     *                                (maybe, multiplied by some <i>d</i><sub>1</sub> constant).
     * @param jx                      <i>x</i>-component of new <b>j</b> vector
     *                                (maybe, multiplied by some <i>d</i><sub>2</sub> constant).
     * @param jy                      <i>y</i>-component of new <b>j</b> vector
     *                                (maybe, multiplied by some <i>d</i><sub>2</sub> constant).
     * @param jz                      <i>z</i>-component of new <b>j</b> vector
     *                                (maybe, multiplied by some <i>d</i><sub>2</sub> constant).
     * @param exceptionOnCollinearity whether exception is thrown for collinear vector pair.
     * @return new right orthonormal basis with given direction of <b>i</b> vector
     * and the direction of <b>j</b> vector, chosen according the arguments (see above).
     * @throws IllegalArgumentException if the length <code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>)
     *                                  of the passed vector (<code>ix</code>,<code>iy</code>,<code>iz</code>) or
     *                                  the length <code>sqrt</code>(jx<sup>2</sup>+jy<sup>2</sup>+jz<sup>2</sup>)
     *                                  of the passed vector (<code>jx</code>,<code>jy</code>,<code>jz</code>)
     *                                  is zero or too small (&lt; {@link #MIN_ALLOWED_LENGTH}).
     * @throws CollinearityException    if the passed two vectors are almost collinear and
     *                                  <code>exceptionOnCollinearity==true</code>.
     */
    public static Orthonormal3DBasis newBasis(
            final double ix, final double iy, final double iz,
            final double jx, final double jy, final double jz,
            final boolean exceptionOnCollinearity)
            throws CollinearityException {
        final double lengthI = length(ix, iy, iz);
        if (lengthI < MIN_ALLOWED_LENGTH) {
            throw new IllegalArgumentException("Zero or too short I vector (" + ix + ", " + iy + ", " + iz + ")"
                    + " (vectors with length <" + MIN_ALLOWED_LENGTH + " are not allowed)");
        }
        final double lengthJ = length(jx, jy, jz);
        if (lengthJ < MIN_ALLOWED_LENGTH) {
            throw new IllegalArgumentException("Zero or too short J vector (" + jx + ", " + jy + ", " + jz + ")"
                    + " (vectors with length <" + MIN_ALLOWED_LENGTH + " are not allowed)");
        }
        double mult = 1.0 / lengthI;
        final double newIx = ix * mult;
        final double newIy = iy * mult;
        final double newIz = iz * mult;
        mult = 1.0 / lengthJ;
        double newJx = jx * mult;
        double newJy = jy * mult;
        double newJz = jz * mult;
        final double ij = newIx * newJx + newIy * newJy + newIz * newJz;
        if (ij != 0.0) {
            newJx -= newIx * ij;
            newJy -= newIy * ij;
            newJz -= newIz * ij; // correct if (ij)!=0: J = J-I(ij)
            final double correctedLengthJ = length(newJx, newJy, newJz);
            if (correctedLengthJ < COLLINEARITY_EPSILON) {
                if (exceptionOnCollinearity) {
                    throw new CollinearityException("Passed I vector (" + ix + ", " + iy + ", " + iz + ") and "
                            + "J vector (" + jx + ", " + jy + ", " + jz + ") are collinear or almost collinear");
                } else {
                    return newSomeBasis(ix, iy, iz);
                }
            }
            mult = 1.0 / correctedLengthJ;
            newJx *= mult;
            newJy *= mult;
            newJz *= mult; // correct again
        }
        return new Orthonormal3DBasis(newIx, newIy, newIz, newJx, newJy, newJz, 0);
    }

    /**
     * Analogue of {@link #newBasis(double, double, double, double, double, double, boolean)
     * getBasis(ix, iy, iz, jx, jy, jz, true}}, but instead of throwing exceptions this method
     * just returns <code>Optional.empty()</code>.
     *
     * <p>In other words, this method returns <code>Optional.empty()</code> when
     * the length <code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>)
     * of the passed vector (<code>ix</code>,<code>iy</code>,<code>iz</code>) or
     * the length <code>sqrt</code>(jx<sup>2</sup>+jy<sup>2</sup>+jz<sup>2</sup>)
     * of the passed vector (<code>jx</code>,<code>jy</code>,<code>jz</code>)
     * is zero or too small (&lt; {@link #MIN_ALLOWED_LENGTH}),
     * and also this method returns <code>Optional.empty()</code> when
     * the passed two vectors are almost collinear.
     * In all other cases, this method is equivalent
     * to <code>Optional.of({@link #newBasis(double, double, double, double, double, double, boolean)
     * getBasis(ix, iy, iz, jx, jy, jz, true/false)})</code> (the last argument is not important).
     * This method <i>never</i> throws any exceptions.
     *
     * @param ix <i>x</i>-component of new <b>i</b> vector
     *           (maybe, multiplied by some <i>d</i><sub>1</sub> constant).
     * @param iy <i>y</i>-component of new <b>i</b> vector
     *           (maybe, multiplied by some <i>d</i><sub>1</sub> constant).
     * @param iz <i>z</i>-component of new <b>i</b> vector
     *           (maybe, multiplied by some <i>d</i><sub>1</sub> constant).
     * @param jx <i>x</i>-component of new <b>j</b> vector
     *           (maybe, multiplied by some <i>d</i><sub>2</sub> constant).
     * @param jy <i>y</i>-component of new <b>j</b> vector
     *           (maybe, multiplied by some <i>d</i><sub>2</sub> constant).
     * @param jz <i>z</i>-component of new <b>j</b> vector
     *           (maybe, multiplied by some <i>d</i><sub>2</sub> constant).
     * @return new right orthonormal basis with given direction of <b>i</b> vector
     * and the direction of <b>j</b> vector, chosen according the arguments
     * (see {@link #newBasis(double, double, double, double, double, double, boolean)}),
     * or empty value in a case of problems.
     */
    public static Optional<Orthonormal3DBasis> optBasis(
            final double ix, final double iy, final double iz,
            final double jx, final double jy, final double jz) {
        final double lengthI = length(ix, iy, iz);
        if (lengthI < MIN_ALLOWED_LENGTH) {
            return Optional.empty();
        }
        final double lengthJ = length(jx, jy, jz);
        if (lengthJ < MIN_ALLOWED_LENGTH) {
            return Optional.empty();
        }
        double mult = 1.0 / lengthI;
        final double newIx = ix * mult;
        final double newIy = iy * mult;
        final double newIz = iz * mult;
        mult = 1.0 / lengthJ;
        double newJx = jx * mult;
        double newJy = jy * mult;
        double newJz = jz * mult;
        final double ij = newIx * newJx + newIy * newJy + newIz * newJz;
        if (ij != 0.0) {
            newJx -= newIx * ij;
            newJy -= newIy * ij;
            newJz -= newIz * ij; // correct if (ij)!=0: J = J-I(ij)
            final double correctedLengthJ = length(newJx, newJy, newJz);
            if (correctedLengthJ < COLLINEARITY_EPSILON) {
                return Optional.empty();
            }
            mult = 1.0 / correctedLengthJ;
            newJx *= mult;
            newJy *= mult;
            newJz *= mult; // correct again
        }
        return Optional.of(new Orthonormal3DBasis(newIx, newIy, newIz, newJx, newJy, newJz, 0));
    }


    /**
     * Creates a pseudorandom basis, which orientation is uniformly distributed in the space.
     * The orientation is chosen with help of <code>random.nextDouble()</code> method.
     *
     * @param random random generator used to create the basis.
     * @return new right orthonormal basis with random orientation.
     */
    public static Orthonormal3DBasis newRandomBasis(RandomGenerator random) {
        for (; ; ) {
            final double ix = 2 * random.nextDouble() - 1.0;
            final double iy = 2 * random.nextDouble() - 1.0;
            final double iz = 2 * random.nextDouble() - 1.0;
            final double distanceSqr = ix * ix + iy * iy + iz * iz;
            if (distanceSqr >= 0.01 && distanceSqr < 1.0) {
                // Note: the second check is necessary to provide uniform distribution in a sphere (not in a cube).
                return newRandomBasis(random, ix, iy, iz);
            }
        }
    }

    /**
     * Creates a pseudorandom basis, where <b>i</b> vector has components
     * (<code>ix</code>/d, <code>iy</code>/d, <code>iz</code>/d),
     * d=<code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>).
     * Directions of vector pair <b>j</b>, <b>k</b> are uniformly distributed in the plane, perpendicular to
     * <b>i</b> vector. These directions are chosen with help of <code>random.nextDouble()</code> method.
     *
     * @param ix     <i>x</i>-component of new <b>i</b> vector (maybe, multiplied by some <i>d</i> constant).
     * @param iy     <i>y</i>-component of new <b>i</b> vector (maybe, multiplied by some <i>d</i> constant).
     * @param iz     <i>z</i>-component of new <b>i</b> vector (maybe, multiplied by some <i>d</i> constant).
     * @param random random generator used to create the basis.
     * @return new randomly oriented right orthonormal basis with given direction of <b>i</b> vector.
     * @throws IllegalArgumentException if the length <code>sqrt</code>(ix<sup>2</sup>+iy<sup>2</sup>+iz<sup>2</sup>)
     *                                  of the passed vector (<code>ix</code>,<code>iy</code>,<code>iz</code>) is zero or
     *                                  too small (&lt; {@link #MIN_ALLOWED_LENGTH}).
     */
    public static Orthonormal3DBasis newRandomBasis(RandomGenerator random, double ix, double iy, double iz) {
        return Orthonormal3DBasis.newSomeBasis(ix, iy, iz).rotateJK(2 * Math.PI * random.nextDouble());
    }

    /**
     * Returns <i>x</i>-component of <b>i</b> vector.
     *
     * @return <i>x</i>-component of <b>i</b> vector.
     */
    public double ix() {
        return ix;
    }

    /**
     * Returns <i>y</i>-component of <b>i</b> vector.
     *
     * @return <i>y</i>-component of <b>i</b> vector.
     */
    public double iy() {
        return iy;
    }

    /**
     * Returns <i>z</i>-component of <b>i</b> vector.
     *
     * @return <i>z</i>-component of <b>i</b> vector.
     */
    public double iz() {
        return iz;
    }

    /**
     * Returns <i>x</i>-component of <b>j</b> vector.
     *
     * @return <i>x</i>-component of <b>j</b> vector.
     */
    public double jx() {
        return jx;
    }

    /**
     * Returns <i>y</i>-component of <b>j</b> vector.
     *
     * @return <i>y</i>-component of <b>j</b> vector.
     */
    public double jy() {
        return jy;
    }

    /**
     * Returns <i>z</i>-component of <b>j</b> vector.
     *
     * @return <i>z</i>-component of <b>j</b> vector.
     */
    public double jz() {
        return jz;
    }

    /**
     * Returns <i>x</i>-component of <b>k</b> vector.
     *
     * @return <i>x</i>-component of <b>k</b> vector.
     */
    public double kx() {
        return kx;
    }

    /**
     * Returns <i>y</i>-component of <b>j</b> vector.
     *
     * @return <i>y</i>-component of <b>j</b> vector.
     */
    public double ky() {
        return ky;
    }

    /**
     * Returns <i>z</i>-component of <b>j</b> vector.
     *
     * @return <i>z</i>-component of <b>j</b> vector.
     */
    public double kz() {
        return kz;
    }

    /**
     * Returns <code>i * {@link #ix()} + j * {@link #jx()} + k * {@link #kx()}</code>
     *
     * @param i projection of some vector <b>p</b> to the basis vector <b>i</b>.
     * @param j projection of some vector <b>p</b> to the basis vector <b>j</b>.
     * @param k projection of some vector <b>p</b> to the basis vector <b>k</b>.
     * @return <i>x</i>-component of such <b>p</b> vector.
     */
    public double x(double i, double j, double k) {
        return i * ix + j * jx + k * kx;
    }

    /**
     * Returns <code>i * {@link #iy()} + j * {@link #jy()} + k * {@link #ky()}</code>
     *
     * @param i projection of some vector <b>p</b> to the basis vector <b>i</b>.
     * @param j projection of some vector <b>p</b> to the basis vector <b>j</b>.
     * @param k projection of some vector <b>p</b> to the basis vector <b>k</b>.
     * @return <i>y</i>-component of such <b>p</b> vector.
     */
    public double y(double i, double j, double k) {
        return i * iy + j * jy + k * ky;
    }

    /**
     * Returns <code>i * {@link #iz()} + j * {@link #jz()} + k * {@link #kz()}</code>
     *
     * @param i projection of some vector <b>p</b> to the basis vector <b>i</b>.
     * @param j projection of some vector <b>p</b> to the basis vector <b>j</b>.
     * @param k projection of some vector <b>p</b> to the basis vector <b>k</b>.
     * @return <i>z</i>-component of such <b>p</b> vector.
     */
    public double z(double i, double j, double k) {
        return i * iz + j * jz + k * kz;
    }

    /**
     * Returns new basis (<b>i'</b>, <b>j'</b>, <b>k'</b>), where
     * <b>i'</b>=<b>j</b>, <b>j'</b>=<b>k</b>, <b>k'</b>=<b>i</b>.
     *
     * @return new basis (<b>j</b>, <b>k</b>, <b>i</b>).
     */
    public Orthonormal3DBasis jki() {
        return new Orthonormal3DBasis(jx, jy, jz, kx, ky, kz, ix, iy, iz, counter);
    }

    /**
     * Returns new basis (<b>i'</b>, <b>j'</b>, <b>k'</b>), where
     * <b>i'</b>=<b>k</b>, <b>j'</b>=<b>i</b>, <b>k'</b>=<b>j</b>.
     *
     * @return new basis (<b>k</b>, <b>i</b>, <b>j</b>).
     */
    public Orthonormal3DBasis kij() {
        return new Orthonormal3DBasis(kx, ky, kz, ix, iy, iz, jx, jy, jz, counter);
    }

    public Orthonormal3DBasis rotateJK(double angleInRadians) {
        if (angleInRadians == 0.0) {
            return this;
        }
        final double cos = Math.cos(angleInRadians);
        final double sin = Math.sin(angleInRadians);
        final double newJx = cos * jx + sin * kx,
                newJy = cos * jy + sin * ky,
                newJz = cos * jz + sin * kz;
        return new Orthonormal3DBasis(ix, iy, iz, newJx, newJy, newJz, counter + 1).reviveIfNecessary();
    }

    public Orthonormal3DBasis rotateKI(double angleInRadians) {
        if (angleInRadians == 0.0) {
            return this;
        }
        final double cos = Math.cos(angleInRadians);
        final double sin = Math.sin(angleInRadians);
        final double newIx = -sin * kx + cos * ix,
                newIy = -sin * ky + cos * iy,
                newIz = -sin * kz + cos * iz;
        return new Orthonormal3DBasis(newIx, newIy, newIz, jx, jy, jz, counter + 1).reviveIfNecessary();
    }

    public Orthonormal3DBasis rotateIJ(double angleInRadians) {
        if (angleInRadians == 0.0) {
            return this;
        }
        final double cos = Math.cos(angleInRadians);
        final double sin = Math.sin(angleInRadians);
        final double newIx = cos * ix + sin * jx,
                newIy = cos * iy + sin * jy,
                newIz = cos * iz + sin * jz,
                newJx = -sin * ix + cos * jx,
                newJy = -sin * iy + cos * jy,
                newJz = -sin * iz + cos * jz;
        return new Orthonormal3DBasis(newIx, newIy, newIz, newJx, newJy, newJz, counter + 1)
                .reviveIfNecessary();
    }

    public Orthonormal3DBasis rotate(double angleXYInRadians, double angleYZInRadians, double angleZXInRadians) {
        double ix = this.ix, iy = this.iy, iz = this.iz;
        double jx = this.jx, jy = this.jy, jz = this.jz;
        if (angleYZInRadians != 0.0) {
            final double cos = Math.cos(angleYZInRadians);
            final double sin = Math.sin(angleYZInRadians);
            final double newIy = iz * sin + iy * cos;
            final double newIz = iz * cos - iy * sin;
            final double newJy = jz * sin + jy * cos;
            final double newJz = jz * cos - jy * sin;
            iy = newIy;
            iz = newIz;
            jy = newJy;
            jz = newJz;
        }
        if (angleZXInRadians != 0.0) {
            final double cos = Math.cos(angleZXInRadians);
            final double sin = Math.sin(angleZXInRadians);
            final double newIz = ix * sin + iz * cos;
            final double newIx = ix * cos - iz * sin;
            final double newJz = jx * sin + jz * cos;
            final double newJx = jx * cos - jz * sin;
            iz = newIz;
            ix = newIx;
            jz = newJz;
            jx = newJx;
        }
        if (angleXYInRadians != 0.0) {
            final double cos = Math.cos(angleXYInRadians);
            final double sin = Math.sin(angleXYInRadians);
            final double newIx = iy * sin + ix * cos;
            final double newIy = iy * cos - ix * sin;
            final double newJx = jy * sin + jx * cos;
            final double newJy = jy * cos - jx * sin;
            ix = newIx;
            iy = newIy;
            jx = newJx;
            jy = newJy;
        }
        return new Orthonormal3DBasis(ix, iy, iz, jx, jy, jz, counter + 1).reviveIfNecessary();
    }

    public Orthonormal3DBasis rotate(double directionX, double directionY, double directionZ, double angle) {
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        double mult = 1.0 / Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        directionX *= mult;
        directionY *= mult;
        directionZ *= mult;

        final double id = ix * directionX + iy * directionY + iz * directionZ;
        final double idx = id * directionX, idy = id * directionY, idz = id * directionZ;
        double ax = ix - idx;
        double ay = iy - idy;
        double az = iz - idz;
        double aLength = Math.sqrt(ax * ax + ay * ay + az * az);
        mult = 1.0 / aLength;
        ax *= mult;
        ay *= mult;
        az *= mult;
        double bx = directionY * az - directionZ * ay;
        double by = directionZ * ax - directionX * az;
        double bz = directionX * ay - directionY * ax;
        // (a,b,g) is a basis, i = id*g + aLength*a
        double newAx = cos * ax + sin * bx;
        double newAy = cos * ay + sin * by;
        double newAz = cos * az + sin * bz;
        if ((mult = Math.abs(newAx * newAx + newAy * newAy + newAz * newAz - 1.0)) > 0.01)
            throw new AssertionError("Internal error (|newA| = " + mult + " != 1) in rotate method");
        final double newIx = idx + aLength * newAx;
        final double newIy = idy + aLength * newAy;
        final double newIz = idz + aLength * newAz;

        final double jd = jx * directionX + jy * directionY + jz * directionZ;
        final double jdx = jd * directionX, jdy = jd * directionY, jdz = jd * directionZ;
        ax = jx - jdx;
        ay = jy - jdy;
        az = jz - jdz;
        aLength = Math.sqrt(ax * ax + ay * ay + az * az);
        mult = 1.0 / aLength;
        ax *= mult;
        ay *= mult;
        az *= mult;
        bx = directionY * az - directionZ * ay;
        by = directionZ * ax - directionX * az;
        bz = directionX * ay - directionY * ax;
        // (a,b,g) is a basis, j = jd*g + aLength*a
        newAx = cos * ax + sin * bx;
        newAy = cos * ay + sin * by;
        newAz = cos * az + sin * bz;
        if ((mult = Math.abs(newAx * newAx + newAy * newAy + newAz * newAz - 1.0)) > 0.01)
            throw new AssertionError("Internal error (|newA| = " + mult + " != 1) in rotate method");
        final double newJx = jdx + aLength * newAx;
        final double newJy = jdy + aLength * newAy;
        final double newJz = jdz + aLength * newAz;

        if ((mult = Math.abs(newIx * newIx + newIy * newIy + newIz * newIz - 1.0)) > 0.01)
            throw new AssertionError("Internal error (|newI| = " + mult + " != 1) in rotate method");
        if ((mult = Math.abs(newJx * newJx + newJy * newJy + newJz * newJz - 1.0)) > 0.01)
            throw new AssertionError("Internal error (|newJ| = " + mult + " != 1) in rotate method");
        if ((mult = Math.abs(newIx * newJx + newIy * newJy + newIz * newJz)) > 0.01)
            throw new AssertionError("Internal error (newI * newJ = " + mult + " != 0) in rotate method");
        return new Orthonormal3DBasis(newIx, newIy, newIz, newJx, newJy, newJz, counter + 1)
                .reviveIfNecessary();
    }

    public Orthonormal3DBasis multiply(Orthonormal3DBasis other) {
        Objects.requireNonNull(other, "Null other basis");
        return new Orthonormal3DBasis(
                // new I: ix*other.I+iy*other.J+iz*other.K
                ix * other.ix + iy * other.jx + iz * other.kx,
                ix * other.iy + iy * other.jy + iz * other.ky,
                ix * other.iz + iy * other.jz + iz * other.kz,
                // new J: jx*other.I+jy*other.J+jz*other.K
                jx * other.ix + jy * other.jx + jz * other.kx,
                jx * other.iy + jy * other.jy + jz * other.ky,
                jx * other.iz + jy * other.jz + jz * other.kz,
                counter + 1).reviveIfNecessary();
    }

    public Orthonormal3DBasis inverse() {
        return new Orthonormal3DBasis(
                ix, jx, kx,
                iy, jy, ky,
                iz, jz, kz,
                counter + 1).reviveIfNecessary();
    }

    public double distanceSquare(Orthonormal3DBasis other) {
        Objects.requireNonNull(other, "Null other basis");
        return lengthSquare(other.ix - ix, other.iy - iy, other.iz - iz)
                + lengthSquare(other.jx - jx, other.jy - jy, other.jz - jz)
                + lengthSquare(other.kx - kx, other.ky - ky, other.kz - kz);
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "I=(" + ix + "," + iy + "," + iz
                + "),J=(" + jx + "," + jy + "," + jz
                + "),K=(" + kx + "," + ky + "," + kz + ")";
    }

    /**
     * Indicates whether some other object is an instance of this class, representing the same basis.
     * The corresponding coordinates of vectors are compared as in <code>Double.equals</code> method,
     * i.e. they are converted to <code>long</code> values by <code>Double.doubleToLongBits</code> method
     * and the results are compared.
     *
     * @param o the object to be compared for equality with this instance.
     * @return <code>true</code> if and only if the specified object is an instance of {@link Orthonormal3DBasis},
     * representing the same right orthonormal basis as this object.
     */
    public boolean equals(Object o) {
        if (!(o instanceof Orthonormal3DBasis that)) {
            return false;
        }
        return (Double.doubleToLongBits(that.ix) == Double.doubleToLongBits(ix)
                && Double.doubleToLongBits(that.iy) == Double.doubleToLongBits(iy)
                && Double.doubleToLongBits(that.iz) == Double.doubleToLongBits(iz)
                && Double.doubleToLongBits(that.jx) == Double.doubleToLongBits(jx)
                && Double.doubleToLongBits(that.jy) == Double.doubleToLongBits(jy)
                && Double.doubleToLongBits(that.jz) == Double.doubleToLongBits(jz));
    }

    /**
     * Returns the hash code of this object.
     *
     * @return the hash code of this object.
     */
    public int hashCode() {
        int result = 0;
        result = 37 * result + hashCode(ix);
        result = 37 * result + hashCode(iy);
        result = 37 * result + hashCode(iz);
        result = 37 * result + hashCode(jx);
        result = 37 * result + hashCode(jy);
        result = 37 * result + hashCode(jz);
        return result;
    }

    /**
     * Returns the square of the length of 3D segment with the given projections to the axes.
     * Equivalent to <code>x * x + y * y + z * z</code>.
     *
     * @param x <i>x</i>-projection of the segment.
     * @param y <i>y</i>-projection of the segment.
     * @param z <i>z</i>-projection of the segment.
     * @return the square of the segment length.
     */
    public static double lengthSquare(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    /**
     * Returns the length of 3D segment with the given projections to the axes.
     * Equivalent to <code>Math.sqrt({@link #lengthSquare(double, double, double) lengthSquare}(x, y, z)).</code>.
     *
     * @param x <i>x</i>-projection of the segment.
     * @param y <i>y</i>-projection of the segment.
     * @param z <i>z</i>-projection of the segment.
     * @return the segment length.
     */
    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Analog ot {@link #lengthSquare(double, double, double)} for integer components.
     * Equivalent to <code>x * x + y * y + z * z</code>.
     *
     * <p>Note: this method does not check possible overflow! To avoid overflow, we recommend never
     * pass here the components of the segment, which precise length &ge;2<sup>31</sup>*&radic;2.
     * If you can guarantee that overflow is impossible, this method works faster than
     * {@link #lengthSquare(double, double, double)}.
     *
     * @param x <i>x</i>-projection of the segment.
     * @param y <i>y</i>-projection of the segment.
     * @param z <i>z</i>-projection of the segment.
     * @return the square of the segment length.
     */
    public static long lengthSquareInteger(long x, long y, long z) {
        return x * x + y * y + z * z;
    }

    /**
     * Returns the scalar product of <b>a</b> and <b>b</b> vectors:
     * <code>ax * bx + ay * by + az * bz</code>.
     *
     * @param ax x-component of the vector <b>a</b>.
     * @param ay y-component of the vector <b>a</b>.
     * @param az z-component of the vector <b>a</b>.
     * @param bx x-component of the vector <b>b</b>.
     * @param by y-component of the vector <b>b</b>.
     * @param bz z-component of the vector <b>b</b>.
     * @return scalar product of two vectors.
     */
    public static double scalarProduct(double ax, double ay, double az, double bx, double by, double bz) {
        return ax * bx + ay * by + az * bz;
    }

    private Orthonormal3DBasis reviveIfNecessary() {
        if (counter <= REVIVING_COUNT) {
            return this;
        }
        return newBasis(ix, iy, iz, jx, jy, jz, false);
    }

    private static int hashCode(double value) {
        return Double.hashCode(value);
    }
}
