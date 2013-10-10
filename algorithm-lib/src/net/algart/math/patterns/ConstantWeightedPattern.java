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
