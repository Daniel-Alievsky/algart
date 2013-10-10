package net.algart.math.patterns;

import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;

public final strictfp class ParaboloidOfRevolutionFunc extends AbstractFunc implements Func {
    private final double multiplierForResult;
    private final double resultAtOrigin;

    private ParaboloidOfRevolutionFunc(double multiplierForResult, double resultAtOrigin) {
        this.multiplierForResult = multiplierForResult;
        this.resultAtOrigin = resultAtOrigin;
    }

    public static ParaboloidOfRevolutionFunc getInstance(double multiplierForResult, double resultAtOrigin) {
        return new ParaboloidOfRevolutionFunc(multiplierForResult, resultAtOrigin);
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
        return xSqr * multiplierForResult + resultAtOrigin;
    }

    @Override
    public String toString() {
        return "paraboloid of revolution function f(r)=" + multiplierForResult + "r^2+" + resultAtOrigin;
    }
}
