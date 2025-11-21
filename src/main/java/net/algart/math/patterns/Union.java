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

import net.algart.math.Point;
import net.algart.math.Range;
import net.algart.math.RectangularArea;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

final class Union extends AbstractPattern implements Pattern {
    private final Pattern[] subsets;
    private final Pattern[] projections;
    private volatile Reference<Set<Point>> points = null;

    Union(Pattern[] patterns) {
        super(MinkowskiSum.getDimCountAndCheck(patterns));
        List<Pattern> allSubsets = new ArrayList<>();
        for (Pattern ptn : patterns) {
            if (ptn instanceof Union) {
                allSubsets.addAll(Arrays.asList(((Union) ptn).subsets));
            } else {
                allSubsets.add(ptn);
            }
            // An alternate idea could be using ptn.unionDecomposition always.
            // But it requires to choose minimalPointCount, which is unknown here:
            // for example, maybe we do not want to decompose little patterns.
        }
        double[] minCoord = new double[dimCount];
        double[] maxCoord = new double[dimCount];
        Arrays.fill(minCoord, Double.POSITIVE_INFINITY);
        Arrays.fill(maxCoord, Double.NEGATIVE_INFINITY);
        for (Pattern ptn : allSubsets) {
            for (int k = 0; k < dimCount; k++) {
                Range range = ptn.coordRange(k);
                if (range.min() < minCoord[k]) {
                    minCoord[k] = range.min();
                }
                if (range.max() > maxCoord[k]) {
                    maxCoord[k] = range.max();
                }
            }
        }
        for (int k = 0; k < dimCount; k++) {
            this.coordRanges[k] = Range.of(minCoord[k], maxCoord[k]);
            checkCoordRange(this.coordRanges[k]);
        }
        this.subsets = allSubsets.toArray(new Pattern[0]);
        this.projections = new UniformGridPattern[dimCount]; //null-filled
    }

    @Override
    public long pointCount() {
        return points().size();
    }

    @Override
    public Set<Point> points() {
        Set<Point> resultPoints = points == null ? null : points.get();
        if (resultPoints == null) {
            resultPoints = new HashSet<>();
            // temporary "resultPoints" variable is necessary while accessing from several threads
            for (Pattern ptn : subsets) {
                resultPoints.addAll(ptn.points());
            }
            resultPoints = Collections.unmodifiableSet(resultPoints);
            points = new SoftReference<>(resultPoints);
        }
        return resultPoints;
    }

    @Override
    public Range coordRange(int coordIndex) {
        return coordRanges[coordIndex];
    }

    @Override
    public RectangularArea coordArea() {
        return RectangularArea.of(coordRanges);
    }

    @Override
    public boolean isSurelySinglePoint() {
        for (Pattern p : subsets) {
            if (!p.isSurelySinglePoint()) {
                return false;
            }
        }
        return pointCount() == 1; // not too complex calculations for union of single-point patterns
    }

    @Override
    public boolean isSurelyInteger() {
        if (surelyInteger == null) {
            boolean allInteger = true;
            for (Pattern p : subsets) {
                if (!p.isSurelyInteger()) {
                    allInteger = false;
                    break;
                }
            }
            surelyInteger = allInteger;
        }
        return surelyInteger;
    }

    // Cannot optimize round(): it should return UniformGridPattern, but Union is not UniformGridPattern

    @Override
    public Pattern projectionAlongAxis(int coordIndex) {
        checkCoordIndex(coordIndex);
        assert dimCount > 0;
        if (dimCount == 1) {
            throw new IllegalStateException("Cannot perform projection for 1-dimensional pattern");
        }
        synchronized (projections) {
            if (projections[coordIndex] == null) {
                Pattern[] newSubsets = new Pattern[subsets.length];
                for (int k = 0; k < newSubsets.length; k++) {
                    newSubsets[k] = subsets[k].projectionAlongAxis(coordIndex);
                }
                projections[coordIndex] = new Union(newSubsets);
            }
            return projections[coordIndex];
        }
    }

    // We could also optimize minBound()/maxBound() here

    @Override
    public Pattern shift(Point shift) {
        if (shift.coordCount() != dimCount) {
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        }
        if (shift.isOrigin()) {
            return this;
        }
        Pattern[] newSubsets = new Pattern[subsets.length];
        for (int k = 0; k < newSubsets.length; k++) {
            newSubsets[k] = subsets[k].shift(shift);
        }
        return new Union(newSubsets);
    }

    @Override
    public Pattern scale(double... multipliers) {
        Objects.requireNonNull(multipliers, "Null multipliers argument");
        if (multipliers.length != dimCount) {
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount);
        }
        Pattern[] newSubsets = new Pattern[subsets.length];
        for (int k = 0; k < newSubsets.length; k++) {
            newSubsets[k] = subsets[k].scale(multipliers);
        }
        return new Union(newSubsets);
    }

    @Override
    public List<List<Pattern>> allUnionDecompositions(int minimalPointCount) {
        if (minimalPointCount < 0) {
            throw new IllegalArgumentException("Negative minimalPointCount");
        }
        List<Pattern> result = new ArrayList<>(subsets.length);
        for (Pattern subset : subsets) {
            result.addAll(subset.unionDecomposition(minimalPointCount));
        }
        result = Collections.unmodifiableList(result);
        return Collections.singletonList(result);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(dimCount + "D integer union of " + subsets.length + " patterns: ");
        for (int k = 0; k < subsets.length; k++) {
            if (k > 0) {
                sb.append(" U ");
            }
            sb.append(subsets[k]);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(subsets) ^ getClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Union && (obj == this || Arrays.equals(subsets, ((Union) obj).subsets));
    }
}
