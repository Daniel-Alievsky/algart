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

package net.algart.math.patterns;

import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.Range;

import java.util.Objects;
import java.util.Set;

class BasicWeightedPattern extends AbstractWeightedPattern implements WeightedPattern {
    private final IPoint weightCoordMin;
    private final IPoint weightCoordMax;
    private final long[] wcMin, wcMax; // little optimization for weight() method
    private final double[] weights;
    private final double outsideWeight;
    private volatile Range weightRange = null;

    BasicWeightedPattern(
        Pattern parent,
        IPoint weightCoordMin,
        IPoint weightCoordMax,
        double[] weights,
        double outsideWeight,
        boolean cloneWeights)
    {
        super(parent);
        int n = dimCount();
        if (n != weightCoordMin.coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: \"coordMin\" is "
                + weightCoordMin.coordCount() + "-dimensional, the pattern is " + n + "-dimensional");
        }
        if (n != weightCoordMax.coordCount()) {
            throw new IllegalArgumentException("Dimensions count mismatch: \"coordMax\" is "
                + weightCoordMax.coordCount() + "-dimensional, the pattern is " + n + "-dimensional");
        }
        this.weightCoordMin = weightCoordMin;
        this.weightCoordMax = weightCoordMax;
        this.wcMin = weightCoordMin.coordinates();
        this.wcMax = weightCoordMax.coordinates();
        final IRange[] ranges = new IRange[n];
        long count = 1;
        for (int k = 0; k < n; k++) {
            IRange range = IRange.valueOf(weightCoordMin.coord(k), weightCoordMax.coord(k));
            ranges[k] = range;
            if (range.size() >= Integer.MAX_VALUE || (count *= range.size()) >= Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Too large desired weight matrix: more than 2^31-1 elements");
            }
        }
        assert count >= 1;
        if (weights.length != count) {
            throw new IllegalArgumentException("The length of weights array " + weights.length
                + " does not match to the product " + count + " of dimensions of the desired weight matrix");
        }
        this.weights = cloneWeights ? weights.clone() : weights;
        for (double w : this.weights) {
            if (Double.isNaN(w)) {
                throw new IllegalArgumentException("Cannot create " + getClass().getName()
                    + ": NaN weight is not allowed");
            }
        }
        if (Double.isNaN(outsideWeight)) {
            throw new IllegalArgumentException("Cannot create " + getClass().getName()
                + ": NaN outside weight is not allowed");
        }
        this.outsideWeight = outsideWeight;
    }

    @Override
    public WeightedPattern shift(IPoint shift) {
        return new BasicWeightedPattern(parent.shift(shift.toPoint()),
            weightCoordMin.add(shift), weightCoordMax.add(shift), weights, outsideWeight, false);
    }

    @Override
    public WeightedPattern scale(double... multipliers) {
        Objects.requireNonNull(multipliers, "Null multipliers argument");
        if (multipliers.length != dimCount()) {
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount());
        }
        int n = dimCount();
        Pattern newPattern = parent.scale(multipliers);
        IPoint newMin = weightCoordMin.roundedScale(multipliers);
        IPoint newMax = weightCoordMin.roundedScale(multipliers);
        long count = 1;
        for (int k = 0; k < n; k++) {
            IRange range = IRange.valueOf(newMin.coord(k), newMax.coord(k));
            if (range.size() >= Integer.MAX_VALUE || (count *= range.size()) >= Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Too large desired weight matrix after resizing: "
                    + "more than 2^31-1 elements");
            }
        }
        double[] newWeights = new double[(int) count]; // zero-filled
        double newOutsideWeight = outsideWeight * newPattern.largePointCount() / this.largePointCount();
        BasicWeightedPattern result = new BasicWeightedPattern(newPattern,
            newMin, newMax, newWeights, newOutsideWeight, false);
        if (Math.abs(multipliers[0]) < 1.0) { //TODO!!
            Set<IPoint> points = roundedPoints();
            for (IPoint point : points) {
                IPoint newPoint = point.roundedScale(multipliers);
                int newIndex = result.weightIndex(newPoint);
                if (newIndex != -1) {
                    newWeights[newIndex] += weight(point);
                }
            }
        } else {
            double multInv = 1.0 / multipliers[0]; //TODO!!
            int[] duplicationCounts = new int[weights.length]; // zero-filled
            Set<IPoint> newPoints = result.roundedPoints();
            for (IPoint newPoint : newPoints) {
                IPoint point = newPoint.multiply(multInv);
                int index = weightIndex(point);
                if (index != -1) {
                    duplicationCounts[index]++;
                }
            }
            for (IPoint newPoint : newPoints) {
                int newIndex = result.weightIndex(newPoint);
                if (newIndex != -1) {
                    IPoint point = newPoint.multiply(multInv);
                    int index = weightIndex(point);
                    if (index != -1) {
                        newWeights[newIndex] = weights[index] / duplicationCounts[index];
                    } else {
                        newWeights[newIndex] = newOutsideWeight;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public double weight(IPoint point) {
        int index = weightIndex(point);
        if (index == -1) {
            return outsideWeight;
        }
        return weights[index];
    }

    @Override
    public Range weightRange() {
        if (weightRange != null) {
            return weightRange;
        }
        double min, max;
        if (weightCoordMin.equals(parent.coordMin().toRoundedPoint())
            && weightCoordMax.equals(parent.coordMax().toRoundedPoint()))
        {
            min = weights[0];
            max = weights[0];
            for (int k = 1; k < weights.length; k++) {
                if (weights[k] < min) {
                    min = weights[k];
                }
                if (weights[k] > max) {
                    max = weights[k];
                }
            }
        } else {
            min = Double.POSITIVE_INFINITY;
            max = Double.NEGATIVE_INFINITY;
            for (IPoint point : roundedPoints()) {
                double w = weight(point);
                if (w < min) {
                    min = w;
                }
                if (w > max) {
                    max = w;
                }
            }
        }
        return weightRange = Range.of(min, max);
    }

    int weightIndex(IPoint point) {
        int n = dimCount();
        long index = point.coord(n - 1);
        if (index < wcMin[n - 1] || index > wcMax[n - 1]) {
            return -1;
        }
        index -= wcMin[n - 1];
        for (int k = n - 2; k >= 0; k--) {
            long coord = point.coord(k);
            if (coord < wcMin[k] || coord > wcMax[k]) {
                return -1;
            }
            coord -= wcMin[k];
            long dim = wcMax[k] - wcMin[k] + 1;
            index = index * dim + coord;
        }
        assert index <= Integer.MAX_VALUE;
        return (int) index;
    }
}
