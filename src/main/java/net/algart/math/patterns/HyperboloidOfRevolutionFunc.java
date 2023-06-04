/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.patterns;

import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;

public abstract strictfp class HyperboloidOfRevolutionFunc extends AbstractFunc implements Func {
    final double scale;
    final double scaleInvSqr;
    final double semiAxisForResultingCoordinate;
    final double resultAtOrigin;
    final double increment;

    private HyperboloidOfRevolutionFunc(
        double scale,
        double semiAxisForResultingCoordinate,
        double resultAtOrigin)
    {
        if (scale < 0.0)
            throw new IllegalArgumentException("Negative scale");
        if (semiAxisForResultingCoordinate < 0.0)
            throw new IllegalArgumentException("Negative semi-axis for the resulting coordinate");
        this.scale = scale;
        this.scaleInvSqr = 1.0 / (scale * scale);
        this.semiAxisForResultingCoordinate = semiAxisForResultingCoordinate;
        this.resultAtOrigin = resultAtOrigin;
        this.increment = resultAtOrigin - semiAxisForResultingCoordinate;
    }

    public static HyperboloidOfRevolutionFunc getUpperInstance(
        double scale,
        double semiAxisForResultingCoordinate,
        double resultAtOrigin)
    {
        return new HyperboloidOfRevolutionFunc(scale, semiAxisForResultingCoordinate, resultAtOrigin) {
            @Override
            public double get(double... x) {
                double xSqr = 0.0;
                for (double coord : x) {
                    xSqr += coord * coord;
                }
                if (xSqr == 0.0) {
                    return resultAtOrigin;
                }
                return (StrictMath.sqrt(1.0 + xSqr * scaleInvSqr) - 1.0)
                    * semiAxisForResultingCoordinate + resultAtOrigin;
            }

            @Override
            public String toString() {
                return "hyperboloid of revolution function (upper part) f(r)="
                    + semiAxisForResultingCoordinate + "*(sqrt(1+(r/"
                    + scale + ")^2)-1)+" + resultAtOrigin;
            }
        };
    }

    public static HyperboloidOfRevolutionFunc getLowerInstance(
        double scale,
        double semiAxisForResultingCoordinate,
        double resultAtOrigin)
    {
        return new HyperboloidOfRevolutionFunc(scale, semiAxisForResultingCoordinate, resultAtOrigin) {
            @Override
            public double get(double... x) {
                double xSqr = 0.0;
                for (double coord : x) {
                    xSqr += coord * coord;
                }
                if (xSqr == 0.0) {
                    return resultAtOrigin;
                }
                return (1.0 - StrictMath.sqrt(1.0 + xSqr * scaleInvSqr))
                    * semiAxisForResultingCoordinate + resultAtOrigin;
            }

            @Override
            public String toString() {
                return "hyperboloid of revolution function (lower part) f(r)="
                    + semiAxisForResultingCoordinate + "*(1-sqrt(1+(r/"
                    + scale + ")^2))+" + resultAtOrigin;
            }
        };
    }

    @Override
    public abstract double get(double... x);
}
