/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.Point;

import java.util.*;

strictfp class BasicDirectPointSetUniformGridPattern
    extends AbstractUniformGridPattern
    implements DirectPointSetUniformGridPattern
{
    private final Set<IPoint> gridIndexes;

    BasicDirectPointSetUniformGridPattern(int dimCount, Set<IPoint> gridIndexes) {
        this(Point.origin(dimCount), Point.valueOfEqualCoordinates(dimCount, 1.0).coordinates(), gridIndexes);
    }

    BasicDirectPointSetUniformGridPattern(Point originOfGrid, double[] stepsOfGrid, Set<IPoint> gridIndexes) {
        super(originOfGrid, stepsOfGrid, false);
        if (gridIndexes == null)
            throw new NullPointerException("Null gridIndexes argument");
        int n = gridIndexes.size();
        if (n == 0)
            throw new IllegalArgumentException("Empty points set");
        long[] minIndex = new long[dimCount];
        long[] maxIndex = new long[dimCount];
        Arrays.fill(minIndex, Long.MAX_VALUE);
        Arrays.fill(maxIndex, Long.MIN_VALUE);
        for (IPoint p : gridIndexes) {
            if (p == null)
                throw new NullPointerException("Null point is the collection");
            if (p.coordCount() != originOfGrid.coordCount())
                throw new IllegalArgumentException("Points dimensions mismatch: the origin of grid has "
                    + originOfGrid.coordCount() + " coordinates, but some of points has " + p.coordCount());
            checkGridIndex(p);
            checkPoint(p.scaleAndShift(stepsOfGrid, originOfGrid));
            for (int k = 0; k < dimCount; k++) {
                long coordinate = p.coord(k);
                if (coordinate < minIndex[k]) {
                    minIndex[k] = coordinate;
                }
                if (coordinate > maxIndex[k]) {
                    maxIndex[k] = coordinate;
                }
            }
        }
        this.gridIndexes = gridIndexes;
        for (int k = 0; k < dimCount; k++) {
            this.gridIndexRanges[k] = IRange.valueOf(minIndex[k], maxIndex[k]);
            checkGridIndexRange(this.gridIndexRanges[k]);
            checkCoordRange(coordRange(k, gridIndexRanges[k]));
        }
    }

    @Override
    public Set<IPoint> gridIndexes() {
        return Collections.unmodifiableSet(gridIndexes);
    }
    @Override
    public IRange gridIndexRange(int coordIndex) {
        return gridIndexRanges[coordIndex];
    }

//    Deprecated optimization: it was actual when gridIndexRange() method worked slow
//    (because in early versions the ranges were not detected in a constructor)
//    @Override
//    public boolean isActuallyRectangular() {
//        if (isRectangular == null) {
//            // below is an attempt to quickly determine that this pattern is not rectangular (very probable situation)
//            // by checking some 5 points at the left boundary along every coordinate
//            for (int coordIndex = 0; coordIndex < dimCount; coordIndex++) {
//                long[] shiftCoordinates = new long[dimCount];
//                shiftCoordinates[coordIndex] = -1;
//                IPoint shift = IPoint.valueOf(shiftCoordinates);
//                int checkCount = 0;
//                long minCoord = Long.MAX_VALUE;
//                for (IPoint p : gridIndexes) {
//                    long coord = p.coord(coordIndex);
//                    if (coord == Long.MIN_VALUE // avoiding overflow in the next check
//                        || !gridIndexes.contains(p.add(shift))) // we found a point at the left boundary
//                    {
//                        if (checkCount == 0) {
//                            minCoord = coord;
//                        } else {
//                            if (coord != minCoord)
//                                return isRectangular = false;
//                        }
//                        checkCount++;
//                        if (checkCount >= 5) // check only 5 points
//                            break;
//                    }
//                }
//            }
//        }
//        return super.isActuallyRectangular();
//    }

    @Override
    public DirectPointSetUniformGridPattern gridIndexPattern() {
        return zeroOriginOfGrid && unitStepsOfGrid ?
            this :
            new BasicDirectPointSetUniformGridPattern(dimCount, gridIndexes);
    }

    @Override
    public DirectPointSetUniformGridPattern shiftGridIndexes(IPoint shift) {
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        Set<IPoint> resultIndexes = new HashSet<IPoint>(gridIndexes.size());
        for (IPoint p : gridIndexes) {
            resultIndexes.add(p.add(shift));
        }
        return new BasicDirectPointSetUniformGridPattern(originOfGrid, stepsOfGrid, resultIndexes);
    }

    @Override
    public long pointCount() {
        return gridIndexes.size();
    }

    @Override
    public boolean isSurelySinglePoint() {
        return gridIndexes.size() == 1;
    }

    @Override
    public DirectPointSetUniformGridPattern shift(Point shift) {
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        if (shift.isOrigin()) {
            return this;
        }
        return new BasicDirectPointSetUniformGridPattern(originOfGrid.add(shift), stepsOfGrid, gridIndexes);
    }

    @Override
    public DirectPointSetUniformGridPattern symmetric() {
        return (DirectPointSetUniformGridPattern) super.symmetric();
    }

    @Override
    public DirectPointSetUniformGridPattern multiply(double multiplier) {
        return (DirectPointSetUniformGridPattern) super.multiply(multiplier);
    }

    @Override
    public DirectPointSetUniformGridPattern scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != dimCount)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount);
        multipliers = multipliers.clone();
        double[] positiveMultipliers = multipliers.clone();
        Set<IPoint> newIndexes = gridIndexes;
        boolean allPositive = true;
        boolean allUnit = true;
        for (double m : multipliers) {
            allPositive &= m > 0.0;
            allUnit &= m == 1.0;
        }
        if (allUnit) {
            return this;
        }
        if (!allPositive) {
            for (int k = 0; k < multipliers.length; k++) {
                positiveMultipliers[k] = multipliers[k] < 0.0 ? -multipliers[k] :
                    multipliers[k] == 0.0 ? 1.0 : multipliers[k];
            }
            newIndexes = new HashSet<IPoint>(gridIndexes.size());
            long[] coordinates = new long[dimCount];
            for (IPoint p : gridIndexes) {
                p.coordinates(coordinates);
                for (int k = 0; k < multipliers.length; k++) {
                    if (multipliers[k] < 0.0) {
                        coordinates[k] = -coordinates[k];
                    } else if (multipliers[k] == 0.0) {
                        coordinates[k] = 0;
                    }
                }
                newIndexes.add(IPoint.valueOf(coordinates));
            }
        }
        return new BasicDirectPointSetUniformGridPattern(
            zeroOriginOfGrid ? originOfGrid : originOfGrid.scale(multipliers),
            unitStepsOfGrid ? positiveMultipliers : stepsVector.scale(positiveMultipliers).coordinates(),
            newIndexes);
    }

    @Override
    public DirectPointSetUniformGridPattern projectionAlongAxis(int coordIndex) {
        checkCoordIndex(coordIndex);
        assert dimCount > 0;
        if (dimCount == 1)
            throw new IllegalStateException("Cannot perform projection for 1-dimensional pattern");
        Set<IPoint> resultIndexes = new HashSet<IPoint>(gridIndexes.size());
        for (IPoint p : gridIndexes) {
            resultIndexes.add(p.projectionAlongAxis(coordIndex));
        }
        return new BasicDirectPointSetUniformGridPattern(
            originOfGrid.projectionAlongAxis(coordIndex),
            stepsVector.projectionAlongAxis(coordIndex).coordinates(),
            resultIndexes);
    }

    @Override
    public DirectPointSetUniformGridPattern minBound(int coordIndex) {
        return (DirectPointSetUniformGridPattern) super.minBound(coordIndex);
    }

    @Override
    public DirectPointSetUniformGridPattern maxBound(int coordIndex) {
        return (DirectPointSetUniformGridPattern) super.maxBound(coordIndex);
    }

    @Override
    public DirectPointSetUniformGridPattern lowerSurface(int coordIndex) {
        return (DirectPointSetUniformGridPattern) super.lowerSurface(coordIndex);
    }

    @Override
    public DirectPointSetUniformGridPattern upperSurface(int coordIndex) {
        return (DirectPointSetUniformGridPattern) super.upperSurface(coordIndex);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(dimCount + "D multipoint uniform-grid ("
            + gridToString() + ") pattern containing "
            + gridIndexes.size() + " points");
        if (pointCount() <= 5) {
            sb.append(" ").append(points());
        }
        if (pointCount() <= 1024) {
            sb.append(" inside ").append(coordArea());
        }
        return sb.toString();
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
