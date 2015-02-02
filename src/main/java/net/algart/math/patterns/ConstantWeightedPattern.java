/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.math.IPoint;
import net.algart.math.Range;

class ConstantWeightedPattern extends AbstractWeightedPattern implements WeightedPattern {
    private final double constantWeight;

    ConstantWeightedPattern(Pattern parent, double constantWeight) {
        super(parent);
        if (Double.isNaN(constantWeight))
            throw new IllegalArgumentException("Cannot create " + getClass().getName()
                + ": NaN weight is not allowed");
        this.constantWeight = constantWeight;
    }

    @Override
    public WeightedPattern shift(IPoint shift) {
        return new ConstantWeightedPattern(parent.shift(shift.toPoint()), this.constantWeight);
    }

    @Override
    public WeightedPattern scale(double... multipliers) {
        Pattern newPattern = parent.scale(multipliers);
        double newConstantWeight = this.constantWeight * newPattern.largePointCount() / this.largePointCount();
        return new ConstantWeightedPattern(newPattern, newConstantWeight);
    }

    public double weight(IPoint point) {
        return constantWeight;
    }

    public Range weightRange() {
        return Range.valueOf(constantWeight, constantWeight);
    }
}
