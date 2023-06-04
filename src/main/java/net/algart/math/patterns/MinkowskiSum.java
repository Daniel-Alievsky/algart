/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

final strictfp class MinkowskiSum extends AbstractPattern implements Pattern {
    private final Pattern[] summands;
    private final List<Pattern> optimizedSummands;
    private final Pattern[] projections;
    private volatile Reference<Set<Point>> points = null;

    MinkowskiSum(Pattern[] patterns) {
        this(patterns, null);
    }

    private MinkowskiSum(Pattern[] patterns, List<Pattern> optimizedSummands) {
        super(getDimCountAndCheck(patterns));
        List<Pattern> allSummands = new ArrayList<Pattern>();
        for (Pattern ptn : patterns) {
            if (ptn instanceof MinkowskiSum) {
                allSummands.addAll(Arrays.asList(((MinkowskiSum) ptn).summands));
            } else {
                allSummands.add(ptn);
            }
            // An alternate idea could be using ptn.minkowskiDecomposition always.
            // But it requires to choose minimalPointCount, which is unknown here:
            // for example, maybe we do not want to decompose little rectangular patterns.
        }
        double[] minCoord = new double[dimCount]; // zero-filled (for further summing)
        double[] maxCoord = new double[dimCount]; // zero-filled (for further summing)
        for (Pattern ptn : allSummands) {
            for (int k = 0; k < dimCount; k++) {
                Range range = ptn.coordRange(k);
                minCoord[k] += range.min();
                maxCoord[k] += range.max();
            }
        }
        for (int k = 0; k < dimCount; k++) {
            this.coordRanges[k] = Range.valueOf(minCoord[k], maxCoord[k]);
            checkCoordRange(this.coordRanges[k]);
        }
        this.summands = allSummands.toArray(new Pattern[allSummands.size()]);
        if (optimizedSummands == null) {
            this.optimizedSummands = optimizeMinkowskiSum(allSummands);
        } else {
            this.optimizedSummands = optimizedSummands;
        }
        this.projections = new Pattern[dimCount]; //null-filled
    }

    @Override
    public long pointCount() {
        return points().size();
    }

//    @Override
//    public boolean contains(IPoint point) {
//        return points().contains(point.toPoint());
//    }

    @Override
    public Set<Point> points() {
        Set<Point> resultPoints = points == null ? null : points.get();
        if (resultPoints == null) {
            Pattern ptn = new SimplePattern(optimizedSummands.get(0).points());
            // this actualization is necessary for a case when minkowskiAdd method just returns MinkowskiSum instance
            for (int k = 1, n = optimizedSummands.size(); k < n; k++) {
                ptn = ptn.minkowskiAdd(optimizedSummands.get(k));
            }
            resultPoints = ptn.points(); // immutable set
            points = new SoftReference<Set<Point>>(resultPoints);
        }
        return resultPoints;
    }

    @Override
    public Range coordRange(int coordIndex) {
        return coordRanges[coordIndex];
    }

    @Override
    public RectangularArea coordArea() {
        return RectangularArea.valueOf(coordRanges);
    }

    @Override
    public boolean isSurelySinglePoint() {
        for (Pattern p : optimizedSummands) {
            if (!p.isSurelySinglePoint()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSurelyInteger() {
        if (surelyInteger == null) {
            boolean allInteger = true;
            for (Pattern p : optimizedSummands) {
                if (!p.isSurelyInteger()) {
                    allInteger = false;
                    break;
                }
            }
            surelyInteger = allInteger;
        }
        return surelyInteger;
    }

    @Override
    public Pattern projectionAlongAxis(int coordIndex) {
        checkCoordIndex(coordIndex);
        assert dimCount > 0;
        if (dimCount == 1)
            throw new IllegalStateException("Cannot perform projection for 1-dimensional pattern");
        synchronized (projections) {
            if (projections[coordIndex] == null) {
                Pattern[] newSummands = new Pattern[summands.length];
                for (int k = 0; k < newSummands.length; k++) {
                    newSummands[k] = summands[k].projectionAlongAxis(coordIndex);
                }
                projections[coordIndex] = new MinkowskiSum(newSummands);
            }
            return projections[coordIndex];
        }
    }

    // We could also optimize minBound()/maxBound() here

    @Override
    public Pattern shift(Point shift) {
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        if (shift.isOrigin()) {
            return this;
        }
        Pattern[] newSummands = summands.clone();
        newSummands[0] = newSummands[0].shift(shift);
        List<Pattern> newOptimizedSummands = new ArrayList<Pattern>(optimizedSummands);
        newOptimizedSummands.set(0, newOptimizedSummands.get(0).shift(shift));
        return new MinkowskiSum(newSummands, newOptimizedSummands);
    }

    @Override
    public Pattern scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != dimCount)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount);
        Pattern[] newSummands = new Pattern[summands.length];
        for (int k = 0; k < newSummands.length; k++) {
            newSummands[k] = summands[k].scale(multipliers);
        }
        return new MinkowskiSum(newSummands);
    }

    @Override
    public Pattern minkowskiAdd(Pattern added) {
        if (added == null)
            throw new NullPointerException("Null added argument");
        Pattern[] newSummands = new Pattern[summands.length + 1];
        System.arraycopy(summands, 0, newSummands, 0, summands.length);
        newSummands[summands.length] = added;
        return new MinkowskiSum(newSummands);
    }

    @Override
    public List<Pattern> minkowskiDecomposition(int minimalPointCount) {
        ArrayList<Pattern> result = new ArrayList<Pattern>();
        for (Pattern summand : optimizedSummands) {
            result.addAll(summand.minkowskiDecomposition(minimalPointCount));
        }
        int numberOfPatternsWithAtLeast2Points = 0;
        for (Pattern ptn : result) {
            long pointCount = ptn.pointCount();
            if (pointCount >= 3) {
                return Collections.unmodifiableList(result);
            }
            if (pointCount >= 2) {
                numberOfPatternsWithAtLeast2Points++;
            }
        }
        if (numberOfPatternsWithAtLeast2Points <= 1) {
            return Collections.<Pattern>singletonList(this);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String toString() {
        int n = optimizedSummands.size();
        StringBuilder sb = new StringBuilder(dimCount + "D Minkowski sum of " + n + " patterns: ");
        for (int k = 0; k < n; k++) {
            if (k > 0) {
                sb.append(" (+) ");
            }
            sb.append(optimizedSummands.get(k));
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(summands) ^ getClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MinkowskiSum && (obj == this || Arrays.equals(summands, ((MinkowskiSum) obj).summands));
    }

    static int getDimCountAndCheck(Pattern[] patterns) {
        if (patterns == null)
            throw new NullPointerException("Null patterns argument");
        if (patterns.length == 0)
            throw new IllegalArgumentException("Empty patterns array");
        if (patterns[0] == null)
            throw new NullPointerException("Null pattern is the array");
        int result = patterns[0].dimCount();
        for (int k = 1; k < patterns.length; k++) {
            if (patterns[k] == null)
                throw new NullPointerException("Null pattern #" + k + " is the array");
            if (patterns[k].dimCount() != result)
                throw new IllegalArgumentException("Patterns dimensions mismatch: the first pattern has "
                    + result + " dimensions, but pattern #" + k + " has " + patterns[k].dimCount());
        }
        return result;
    }

    private static List<Pattern> optimizeMinkowskiSum(List<Pattern> patterns) {
        Map<Pattern, Integer> numbersOfEquals = new HashMap<Pattern, Integer>();
        Map<Point, UniformGridPattern> rectangularSummands = new HashMap<Point, UniformGridPattern>();
        Pattern onePointSummand = null;
        for (Pattern pattern : patterns) {
            if (pattern instanceof OnePointPattern) {
                onePointSummand = onePointSummand == null ? pattern : onePointSummand.minkowskiAdd(pattern);
                if (!(onePointSummand instanceof OnePointPattern))
                    throw new AssertionError("Invalid OnePointPattern.minkowskiAdd implementation");
            } else if (pattern instanceof UniformGridPattern
                && ((UniformGridPattern) pattern).isActuallyRectangular())
            {
                UniformGridPattern ugPattern = (UniformGridPattern) pattern;
                ugPattern = new BasicRectangularPattern(ugPattern.originOfGrid(), ugPattern.stepsOfGrid(),
                    ugPattern.gridIndexArea().ranges());
                Point steps = Point.valueOf(ugPattern.stepsOfGrid());
                UniformGridPattern previousSummand = rectangularSummands.get(steps);
                pattern = previousSummand == null ? ugPattern : previousSummand.minkowskiAdd(ugPattern);
                if (!(pattern instanceof BasicRectangularPattern))
                    throw new AssertionError("Invalid RectangularUniformGridPattern.minkowskiAdd implementation");
                rectangularSummands.put(steps, (BasicRectangularPattern) pattern);
            } else {
                Integer previousNumber = numbersOfEquals.get(pattern);
                if (previousNumber == null) {
                    numbersOfEquals.put(pattern, 1);
                } else {
                    numbersOfEquals.put(pattern, previousNumber + 1);
                }
            }
        }
        List<Map.Entry<Pattern, Integer>> multiPatterns =
            new ArrayList<Map.Entry<Pattern, Integer>>(numbersOfEquals.entrySet());
        // Sorting by decreasing number of points
        Collections.sort(multiPatterns, new Comparator<Map.Entry<Pattern, Integer>>() {
            public int compare(Map.Entry<Pattern, Integer> o1, Map.Entry<Pattern, Integer> o2) {
                long pointCount1 = o1.getKey().pointCount();
                long pointCount2 = o2.getKey().pointCount();
                return pointCount1 < pointCount2 ? 1 : pointCount1 > pointCount2 ? -1 : 0;
            }
        });
        List<Pattern> result = new ArrayList<Pattern>();
        result.addAll(rectangularSummands.values());
        if (onePointSummand != null) {
            if (result.isEmpty()) {
                result.add(onePointSummand);
            } else {
                result.set(0, onePointSummand.minkowskiAdd(result.get(0)));
            }
        }
        Pattern last = null;
        for (Map.Entry<Pattern, Integer> multiPattern : multiPatterns) {
            Pattern p = multiPattern.getKey();
            int n = multiPattern.getValue();
            Pattern c = null;
            if (n > 1 || last != null) {
                c = p.carcass();
            }
            if (last != null && last.minkowskiAdd(p).equals(last.minkowskiAdd(c))) {
                // probably true, because patterns were sorted
                result.add(c);
            } else {
                result.add(p);
            }
            n--; // 1 pattern used (included into result);
            if (n > 0) {
                int maxMultiplier = p.maxCarcassMultiplier();
                for (int m = 1; ; ) {
                    if (m > n) {
                        m = n;
                    }
                    assert c != null; // because n > 0 after n--
                    result.add(c.multiply(m));
                    n -= m; // m patterns used now
                    if (n == 0) {
                        break;
                    }
                    assert n >= 0 : "Counter overflow while optimizing Minkowski sum";
                    if (2 * m >= 0 && // no overflow yet
                        2 * m <= maxMultiplier)
                    {
                        m *= 2;
                    }
                }
            }
            last = p;
        }
        return result;
    }
}
