package net.algart.math.patterns;

import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;

public final strictfp class UpperHalfEllipsoidOfRevolutionFunc extends AbstractFunc implements Func {
    private final double r;
    private final double rSqr;
    private final double scaleForResult;
    private final double resultAtOrigin;
    private final double increment;

    private UpperHalfEllipsoidOfRevolutionFunc(
        double r,
        double semiAxisForResultingCoordinate,
        double resultAtOrigin)
    {
        if (r < 0.0)
            throw new IllegalArgumentException("Negative radius");
        if (semiAxisForResultingCoordinate < 0.0)
            throw new IllegalArgumentException("Negative semi-axis for the resulting coordinate");
        this.r = r;
        this.rSqr = r * r;
        this.scaleForResult = semiAxisForResultingCoordinate / r;
        this.resultAtOrigin = resultAtOrigin;
        this.increment = resultAtOrigin - semiAxisForResultingCoordinate;
    }

    public static UpperHalfEllipsoidOfRevolutionFunc getInstance(
        double r,
        double semiAxisForResultingCoordinate,
        double resultAtOrigin)
    {
        return new UpperHalfEllipsoidOfRevolutionFunc(r, semiAxisForResultingCoordinate, resultAtOrigin);
    }

    @Override
    public double get(double... x) {
        double xSqr = 0.0;
        for (double coord : x) {
            xSqr += coord * coord;
        }
        if (xSqr == 0.0) {
            return resultAtOrigin;
        }
        return StrictMath.sqrt(rSqr - xSqr) * scaleForResult + increment;
    }

    @Override
    public String toString() {
        return "ellipsoid of revolution function (upper half) f(r)="
            + scaleForResult + "*sqrt(" + r + "^2-r^2)+" + increment;
    }
}
