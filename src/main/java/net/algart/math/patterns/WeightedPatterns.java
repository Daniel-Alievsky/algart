/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>A set of static methods operating with and returning {@link WeightedPattern weighted patterns}.</p>
 *
 * <p>This class cannot be instantiated.</p>
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
