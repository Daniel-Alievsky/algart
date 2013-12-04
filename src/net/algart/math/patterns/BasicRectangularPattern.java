/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.IRectangularArea;
import net.algart.math.Point;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final strictfp class BasicRectangularPattern extends AbstractUniformGridPattern implements RectangularPattern {
    private final IRectangularArea gridIndexArea;
    private final long pointCount;
    private final double largePointCount;
    private final boolean veryLarge;
    private volatile Reference<Set<Point>> pointsRef = null;
    private volatile Reference<Set<IPoint>> gridIndexesRef = null;

    BasicRectangularPattern(IRange[] gridIndexRanges) {
        this(
            Point.origin(gridIndexRanges.length),
            Point.valueOfEqualCoordinates(gridIndexRanges.length, 1.0).coordinates(),
            gridIndexRanges);
    }

    BasicRectangularPattern(Point originOfGrid, double[] stepsOfGrid, IRange[] gridIndexRanges) {
        super(originOfGrid, stepsOfGrid, true);
        if (gridIndexRanges == null)
            throw new NullPointerException("Null coordinate ranges argument");
        if (gridIndexRanges.length != originOfGrid.coordCount())
            throw new IllegalArgumentException("The number of coordinate ranges is not equal "
                + "to the number of dimensions of the origin");
        System.arraycopy(gridIndexRanges, 0, this.gridIndexRanges, 0, gridIndexRanges.length);
        long count = 1;
        double largeCount = 1.0;
        for (int k = 0; k < this.gridIndexRanges.length; k++) {
            IRange gridIndexRange = this.gridIndexRanges[k];
            checkGridIndexRange(gridIndexRange);
            checkCoordRange(coordRange(k, gridIndexRange));
            long size = gridIndexRange.size();
            if (count != Long.MIN_VALUE) {
                count = Patterns.longMul(count, size);
            }
            largeCount *= size;
        }
        this.gridIndexArea = IRectangularArea.valueOf(this.gridIndexRanges);
        this.pointCount = count == Long.MIN_VALUE ? Long.MAX_VALUE : count;
        this.largePointCount = count == Long.MIN_VALUE ? largeCount : count;
        this.veryLarge = count == Long.MIN_VALUE;
    }

    @Override
    public Set<IPoint> gridIndexes() {
        if (pointCount > Integer.MAX_VALUE)
            throw new TooManyPointsInPatternError("Too large number of points: "
                + largePointCount + " > Integer.MAX_VALUE");
        Set<IPoint> resultIndexes = gridIndexesRef == null ? null : gridIndexesRef.get();
        if (resultIndexes == null) {
            resultIndexes = new HashSet<IPoint>((int) pointCount);
            addIPointsToParallelepiped(resultIndexes, new long[dimCount], 0); //new long[]: zero-filled
            gridIndexesRef = new SoftReference<Set<IPoint>>(resultIndexes);
        }
        return Collections.unmodifiableSet(resultIndexes);
    }

    @Override
    public IRange gridIndexRange(int coordIndex) {
        return gridIndexRanges[coordIndex];
    }

    @Override
    public IRectangularArea gridIndexArea() {
        return gridIndexArea;
    }

    @Override
    public boolean isActuallyRectangular() {
        return true;
    }

    @Override
    public RectangularPattern gridIndexPattern() {
        return zeroOriginOfGrid && unitStepsOfGrid ? this : new BasicRectangularPattern(gridIndexRanges);
    }

    @Override
    public RectangularPattern shiftGridIndexes(IPoint shift) {
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        if (shift.isOrigin()) {
            return this;
        }
        IRange[] newRanges = new IRange[gridIndexRanges.length];
        for (int k = 0; k < gridIndexRanges.length; k++) {
            long sh = shift.coord(k);
            long newMin = gridIndexRanges[k].min() + sh;
            long newMax = gridIndexRanges[k].max() + sh;
            checkGridIndexRange(newMin, newMax); // for more informative exception in a case of overflow
            newRanges[k] = IRange.valueOf(newMin, newMax);
        }
        return new BasicRectangularPattern(originOfGrid, stepsOfGrid, newRanges);
    }

    @Override
    public RectangularPattern lowerSurface(int coordIndex) {
        checkCoordIndex(coordIndex);
        IRange[] surfaceRanges = gridIndexRanges.clone();
        long coord = gridIndexRange(coordIndex).min();
        surfaceRanges[coordIndex] = IRange.valueOf(coord, coord);
        return Patterns.newRectangularIntegerPattern(surfaceRanges).scale(stepsOfGrid).shift(originOfGrid);
    }

    @Override
    public RectangularPattern upperSurface(int coordIndex) {
        checkCoordIndex(coordIndex);
        IRange[] surfaceRanges = gridIndexRanges.clone();
        long coord = gridIndexRange(coordIndex).max();
        surfaceRanges[coordIndex] = IRange.valueOf(coord, coord);
        return Patterns.newRectangularIntegerPattern(surfaceRanges).scale(stepsOfGrid).shift(originOfGrid);
    }

    @Override
    public Pattern surface() {
        Pattern[] facets = new Pattern[2 * dimCount];
        IRange[] surfaceRanges = gridIndexRanges.clone();
        int facetIndex = 0;
        for (int coordIndex = surfaceRanges.length - 1; coordIndex >= 0; coordIndex--) {
            long min = gridIndexRange(coordIndex).min();
            surfaceRanges[coordIndex] = IRange.valueOf(min, min);
            facets[facetIndex++] = Patterns.newRectangularIntegerPattern(surfaceRanges)
                .scale(stepsOfGrid).shift(originOfGrid);
            long max = gridIndexRange(coordIndex).max();
            assert max >= min;
            if (max == min) {
                break;
            }
            surfaceRanges[coordIndex] = IRange.valueOf(max, max);
            facets[facetIndex++] = Patterns.newRectangularIntegerPattern(surfaceRanges)
                .scale(stepsOfGrid).shift(originOfGrid);
            if (max - min == 1) {
                break;
            }
            surfaceRanges[coordIndex] = IRange.valueOf(min + 1, max - 1);
            // removing these 2 facets from the full parallelepiped: no sense to add vertices several times
        }
        Pattern[] result = new Pattern[facetIndex];
        System.arraycopy(facets, 0, result, 0, facetIndex);
        return Patterns.newUnion(result);
    }

    @Override
    public long pointCount() {
        return this.pointCount;
    }

    @Override
    public double largePointCount() {
        return this.largePointCount;
    }

    @Override
    public boolean isSurelySinglePoint() {
        return this.pointCount == 1;
    }

    @Override
    public boolean isPointCountVeryLarge() {
        return this.veryLarge;
    }

    @Override
    public Set<Point> points() {
        if (pointCount > Integer.MAX_VALUE)
            throw new TooManyPointsInPatternError("Too large number of points: "
                + largePointCount + " > Integer.MAX_VALUE");
        Set<Point> resultIndexes = pointsRef == null ? null : pointsRef.get();
        if (resultIndexes == null) {
            resultIndexes = new HashSet<Point>((int) pointCount);
            addPointsToParallelepiped(resultIndexes, new long[dimCount], 0); //new long[]: zero-filled
            pointsRef = new SoftReference<Set<Point>>(resultIndexes);
        }
        return Collections.unmodifiableSet(resultIndexes);
    }

    public UniformGridPattern round() {
        if (zeroOriginOfGrid && unitStepsOfGrid) {
            return this;
        }
        return new BasicRectangularPattern(roundedCoordArea().ranges());

    }

    @Override
    public RectangularPattern shift(Point shift) {
        if (shift == null)
            throw new NullPointerException("Null shift argument");
        if (shift.coordCount() != dimCount)
            throw new IllegalArgumentException("The number of shift coordinates " + shift.coordCount()
                + " is not equal to the number of pattern coordinates " + dimCount);
        if (shift.isOrigin()) {
            return this;
        }
        return new BasicRectangularPattern(originOfGrid.add(shift), stepsOfGrid, gridIndexRanges);
    }

    @Override
    public RectangularPattern symmetric() {
        return (RectangularPattern) super.symmetric();
    }

    @Override
    public RectangularPattern multiply(double multiplier) {
        return (RectangularPattern) super.multiply(multiplier);
    }

    @Override
    public RectangularPattern scale(double... multipliers) {
        if (multipliers == null)
            throw new NullPointerException("Null multipliers argument");
        if (multipliers.length != dimCount)
            throw new IllegalArgumentException("Illegal number of multipliers: "
                + multipliers.length + " instead of " + dimCount);
        double[] positiveMultipliers = multipliers.clone();
        multipliers = multipliers.clone();
        IRange[] newRanges = gridIndexRanges;
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
            newRanges = new IRange[dimCount];
            for (int k = 0; k < dimCount; k++) {
                if (multipliers[k] < 0.0) {
                    newRanges[k] = IRange.valueOf(-gridIndexRanges[k].max(), -gridIndexRanges[k].min());
                } else if (multipliers[k] == 0.0) {
                    newRanges[k] = IRange.valueOf(0, 0);
                } else {
                    newRanges[k] = gridIndexRanges[k];
                }
            }
        }
        return new BasicRectangularPattern(
            zeroOriginOfGrid ? originOfGrid : originOfGrid.scale(multipliers),
            unitStepsOfGrid ? positiveMultipliers : stepsVector.scale(positiveMultipliers).coordinates(),
            newRanges);
    }


    @Override
    public RectangularPattern projectionAlongAxis(int coordIndex) {
        checkCoordIndex(coordIndex);
        assert dimCount > 0;
        if (dimCount == 1)
            throw new IllegalStateException("Cannot perform projection for 1-dimensional pattern");
        IRange[] newRanges = new IRange[gridIndexRanges.length - 1];
        System.arraycopy(gridIndexRanges, 0, newRanges, 0, coordIndex);
        System.arraycopy(gridIndexRanges, coordIndex + 1, newRanges, coordIndex, newRanges.length - coordIndex);
        return new BasicRectangularPattern(
            originOfGrid.projectionAlongAxis(coordIndex),
            stepsVector.projectionAlongAxis(coordIndex).coordinates(),
            newRanges);
    }

    @Override
    public RectangularPattern minBound(int coordIndex) {
        return lowerSurface(coordIndex);
    }

    @Override
    public RectangularPattern maxBound(int coordIndex) {
        return upperSurface(coordIndex);
    }

    @Override
    public Pattern minkowskiAdd(Pattern added) {
        if (!(added instanceof BasicRectangularPattern) || added.dimCount() != this.dimCount) {
            return super.minkowskiAdd(added);
        }
        UniformGridPattern ugAdded = (UniformGridPattern) added;
        if (!stepsOfGridEqual(ugAdded)) {
            return super.minkowskiAdd(added);
        }
        IRange[] sumRanges = new IRange[gridIndexRanges.length];
        for (int k = 0; k < gridIndexRanges.length; k++) {
            long newMin = gridIndexRanges[k].min() + ugAdded.gridIndexRange(k).min();
            long newMax = gridIndexRanges[k].max() + ugAdded.gridIndexRange(k).max();
            // overflow is impossible: both summands are in -Long.MAX_VALUE/2..Long.MAX_VALUE/2 range
            sumRanges[k] = IRange.valueOf(newMin, newMax);
        }
        return new BasicRectangularPattern(
            originOfGrid.add(ugAdded.originOfGrid()),
            stepsOfGrid,
            sumRanges); // TooLargePatternCoordinatesException is possible
    }

    @Override
    public Pattern minkowskiSubtract(Pattern subtracted) {
        if (!(subtracted instanceof BasicRectangularPattern) || subtracted.dimCount() != this.dimCount) {
            return super.minkowskiSubtract(subtracted);
        }
        UniformGridPattern ugSubtracted = (UniformGridPattern) subtracted;
        if (!stepsOfGridEqual(ugSubtracted)) {
            return super.minkowskiAdd(subtracted);
        }
        IRange[] diffRanges = new IRange[gridIndexRanges.length];
        for (int k = 0; k < gridIndexRanges.length; k++) {
            long newMin = gridIndexRanges[k].min() - ugSubtracted.gridIndexRange(k).min();
            long newMax = gridIndexRanges[k].max() - ugSubtracted.gridIndexRange(k).max();
            // overflow is impossible: minuend and subtrahend are in -Long.MAX_VALUE/2..Long.MAX_VALUE/2 range
            if (newMin > newMax) {
                return null;
            }
            diffRanges[k] = IRange.valueOf(newMin, newMax);
        }
        return new BasicRectangularPattern(
            originOfGrid.subtract(ugSubtracted.originOfGrid()),
            stepsOfGrid,
            diffRanges); // TooLargePatternCoordinatesException is possible
    }

    @Override
    public boolean hasMinkowskiDecomposition() {
        return pointCount > 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(dimCount + "D rectangular uniform-grid pattern containing "
            + (pointCount == Long.MAX_VALUE ? largePointCount + " points" : pointCount + " points ("));
        for (int k = 0; k < dimCount; k++) {
            if (k > 0) {
                sb.append(",");
            }
            sb.append(coordRange(k).min());
        }
        sb.append(")..(");
        for (int k = 0; k < dimCount; k++) {
            if (k > 0)
                sb.append(",");
            sb.append(coordRange(k).max());
        }
        sb.append(") - on grid ").append(gridToString());
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return ((this.getClass().getName().hashCode() * 31
            + Arrays.hashCode(gridIndexRanges)) * 31
            + originOfGrid.hashCode()) * 31
            + stepsVector.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicRectangularPattern)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        BasicRectangularPattern pattern = (BasicRectangularPattern) obj;
        return Arrays.equals(gridIndexRanges, pattern.gridIndexRanges)
            && originOfGrid.equals(pattern.originOfGrid)
            && stepsVector.equals(pattern.stepsVector);
    }

    private void addIPointsToParallelepiped(Set<IPoint> points, long[] coordinates, int lastCoordinatesCount) {
        int cIndex = dimCount - 1 - lastCoordinatesCount;
        if (cIndex == 0) {
            for (long i = gridIndexRanges[0].min(), iMax = gridIndexRanges[0].max(); i <= iMax; i++) {
                coordinates[0] = i;
                points.add(IPoint.valueOf(coordinates));
            }
        } else {
            for (long i = gridIndexRanges[cIndex].min(), iMax = gridIndexRanges[cIndex].max(); i <= iMax; i++) {
                coordinates[cIndex] = i;
                addIPointsToParallelepiped(points, coordinates, lastCoordinatesCount + 1);
            }
        }
    }

    private void addPointsToParallelepiped(Set<Point> points, long[] coordinates, int lastCoordinatesCount) {
        int cIndex = dimCount - 1 - lastCoordinatesCount;
        if (cIndex == 0) {
            for (long i = gridIndexRanges[0].min(), iMax = gridIndexRanges[0].max(); i <= iMax; i++) {
                coordinates[0] = i;
                points.add(IPoint.valueOf(coordinates).scaleAndShift(stepsOfGrid, originOfGrid));
            }
        } else {
            for (long i = gridIndexRanges[cIndex].min(), iMax = gridIndexRanges[cIndex].max(); i <= iMax; i++) {
                coordinates[cIndex] = i;
                addPointsToParallelepiped(points, coordinates, lastCoordinatesCount + 1);
            }
        }
    }
}
