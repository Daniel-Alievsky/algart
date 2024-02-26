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
