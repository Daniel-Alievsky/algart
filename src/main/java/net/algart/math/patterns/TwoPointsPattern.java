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

import net.algart.math.Point;
import net.algart.math.Range;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple 2-points pattern.
 * This easy implementation helps to save memory and time while using Minkowski decomposition,
 * build by {@link AbstractUniformGridPattern#minkowskiDecomposition(int)} method.
 */
final strictfp class TwoPointsPattern extends AbstractPattern implements DirectPointSetPattern {
    private final Point p1, p2;

    TwoPointsPattern(Point p1, Point p2) {
        super(SimplePattern.getDimCountAndCheck(Arrays.asList(p1, p2)));
        if (p1.equals(p2))
            throw new AssertionError("Identical 2 points");
        this.p1 = p1;
        this.p2 = p2;
        fillCoordRangesWithCheck(Arrays.asList(this.p1, this.p2));
    }

    @Override
    public long pointCount() {
        return 2;
    }

    @Override
    public Set<Point> points() {
        Set<Point> resultPoints = new HashSet<Point>(2);
        resultPoints.add(p1);
        resultPoints.add(p2);
        return Collections.unmodifiableSet(resultPoints);
    }

    @Override
    public Range coordRange(int coordIndex) {
        return coordRanges[coordIndex];
    }

    @Override
    public boolean isSurelySinglePoint() {
        return false;
    }

    @Override
    public boolean isSurelyInteger() {
        return p1.isInteger() && p2.isInteger();
    }

    @Override
    public DirectPointSetPattern shift(Point shift) {
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        if (shift.isOrigin()) {
            return this;
        }
        return new TwoPointsPattern(p1.add(shift), p2.add(shift));
    }

    @Override
    public DirectPointSetPattern scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != dimCount)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount);
        boolean allUnit = true;
        for (double m : multipliers) {
            allUnit &= m == 1.0;
        }
        if (allUnit) {
            return this;
        }
        return Patterns.newPattern(p1.scale(multipliers), p2.scale(multipliers));
        // possibly will return 1-point pattern if all multipliers are 0.0 or almost 0.0
    }

    @Override
    public DirectPointSetPattern symmetric() {
        return (DirectPointSetPattern) super.symmetric();
    }

    @Override
    public DirectPointSetPattern multiply(double multiplier) {
        return (DirectPointSetPattern) super.multiply(multiplier);
    }

    @Override
    public DirectPointSetPattern projectionAlongAxis(int coordIndex) {
        checkCoordIndex(coordIndex);
        assert dimCount > 0;
        if (dimCount == 1)
            throw new IllegalStateException("Cannot perform projection for 1-dimensional pattern");
        return Patterns.newPattern(p1.projectionAlongAxis(coordIndex), p2.projectionAlongAxis(coordIndex));
        // possibly will return 1-point pattern if the projections are the same
    }

    @Override
    public DirectPointSetPattern minBound(int coordIndex) {
        return (DirectPointSetPattern) super.minBound(coordIndex);
    }

    @Override
    public DirectPointSetPattern maxBound(int coordIndex) {
        return (DirectPointSetPattern) super.maxBound(coordIndex);
    }

    @Override
    public String toString() {
        return dimCount + "D 2-point pattern (" + p1 + " and " + p2 + ")";
    }

    @Override
    public int hashCode() {
        return points().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pattern && SimplePattern.simplePatternsEqual(this, (Pattern) obj);
    }
}
