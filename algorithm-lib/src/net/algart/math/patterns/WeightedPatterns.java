package net.algart.math.patterns;

import net.algart.math.IPoint;

/**
 * <p>A set of static methods operating with and returning {@link WeightedPattern weighted patterns}.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class WeightedPatterns {
    private WeightedPatterns() {
    }

    public static WeightedPattern newConstantPattern(Pattern parent, double constantWeight) {
        return new ConstantWeightedPattern(parent, constantWeight);
    }

    public static WeightedPattern newPattern(
        Pattern parent,
        IPoint weightCoordMin, IPoint weightCoordMax, double[] weights, double outsideWeight)
    {
        return new BasicWeightedPattern(parent, weightCoordMin, weightCoordMax, weights, outsideWeight, true);
    }

    public static WeightedPattern newPattern(Pattern parent, double[] weights) {
        return new BasicWeightedPattern(parent,
            parent.coordMin().toRoundedPoint(),
            parent.coordMax().toRoundedPoint(), weights, 157.0, true);
    }
}
