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

package net.algart.math.geometry.demo;

import net.algart.math.geometry.CollinearityException;
import net.algart.math.geometry.Orthonormal3DBasis;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public final class Orthonormal3DBasisTest {
    private static void testNewBasis(double ix, double iy, double iz, double jx, double jy, double jz) {
        Orthonormal3DBasis basis = null;
        try {
            basis = Orthonormal3DBasis.newBasis(ix, iy, iz, jx, jy, jz, true);
        } catch (CollinearityException | IllegalArgumentException e) {
            System.out.printf("Cannot create basis for (%s %s %s), (%s %s %s): %s%n", ix, iy, iz, jx, jy, jz, e);
        }
        Optional<Orthonormal3DBasis> opt = Orthonormal3DBasis.optBasis(ix, iy, iz, jx, jy, jz);
        if (opt.isEmpty() != (basis == null)) {
            throw new AssertionError("optBasis/newBasis mismatch!");
        }
    }

    private static double iAbs(Orthonormal3DBasis basis) {
        return Math.sqrt(basis.ix() * basis.ix() + basis.iy() * basis.iy() + basis.iz() * basis.iz());
    }

    private static double jAbs(Orthonormal3DBasis basis) {
        return Math.sqrt(basis.jx() * basis.jx() + basis.jy() * basis.jy() + basis.jz() * basis.jz());
    }

    private static double kAbs(Orthonormal3DBasis basis) {
        return Math.sqrt(basis.kx() * basis.kx() + basis.ky() * basis.ky() + basis.kz() * basis.kz());
    }

    public static void main(String[] args) {
        testNewBasis(0 , 0 , 0, 0, 0, 0);
        testNewBasis(1000 , 0 , 0, 1000, 0, 0);
        testNewBasis(1000 , 0 , 0, 0, 100, 0);
        testNewBasis(0 , 1e-10 , 0, 0, 100, 0);
        Random rnd = new Random(157);
        Orthonormal3DBasis basis = Orthonormal3DBasis.DEFAULT;
        System.out.printf("Default basis: %s%n", basis);
        System.out.printf("jki basis: %s%n", basis.jki());
        System.out.printf("kij basis: %s%n", basis.kij());
        basis = Orthonormal3DBasis.newBasis(0, 1, 0, 0, 5, 0, false);
        System.out.printf("Basis made for collinear pair: %s%n", basis);
        basis = Orthonormal3DBasis.newSomeBasis(
                rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian());
        System.out.printf(Locale.US, "Starting basis: %s%n", basis);
        for (int k = 0; k < 11111111; k++) {
            final Orthonormal3DBasis inverse = basis.inverse();
            final Orthonormal3DBasis product = basis.multiply(inverse);
            final double distanceSquare = product.distanceSquare(Orthonormal3DBasis.DEFAULT);
            if (distanceSquare >= 1e-6) {
                throw new AssertionError("Problem with inverse basis: "
                        + inverse + ", product = " + product + ", distance square = " + distanceSquare);
            }
            final double angle = rnd.nextDouble();
            basis = switch (rnd.nextInt(4)) {
                case 0 -> basis.rotateJK(angle);
//                    System.out.printf("Rotation I by %f: %s (%f)%n", angle, basis, iAbs((basis)));
                case 1 -> basis.rotateKI(angle);
//                    System.out.printf("Rotation J by %f: %s (%f)%n", angle, basis, iAbs((basis)));
                case 2 -> basis.rotateIJ(angle);
//                    System.out.printf("Rotation K by %f: %s (%f)%n", angle, basis, iAbs((basis)));
                case 3 -> basis.rotate(rnd.nextDouble(), angle, rnd.nextDouble());
                default -> basis;
            };
        }
        System.out.printf(Locale.US, "Rotated basis: %s%n  |i|=%s, |j|=%s, |k|=%s%n",
                basis, iAbs(basis), jAbs(basis), kAbs((basis)));
    }
}
