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

import net.algart.arrays.PackedBitArrays;
import net.algart.math.*;

import java.util.*;

/**
 * <p>A skeletal implementation of the {@link UniformGridPattern} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>All non-abstract methods are completely implemented here and may be not overridden in subclasses.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractUniformGridPattern extends AbstractPattern implements UniformGridPattern {

    private static final boolean DEBUG_MODE = false; // thorough checking carcass

    private static final double MAX_RELATION_OF_PARALLELEPIPED_VOLUME_TO_THIS_TO_OPTIMIZE_IN_MINKOWSKI_ADD = 200.0;
    private static final int MIN_POINT_COUNT_TO_OPTIMIZE_MINKOWSKI_ADD = 32;

    private static int maxNumberOfPointsInNewParallelepipedWhileCheckingLargeCarcasses(int dimCount) {
        return dimCount <= 2 ? (int) 4e6 : (int) 16e6;
    }
    // - the maximal number of points in the circumscribed parallelepiped of NEW checked pattern n(x)P(+)n(x)P
    // must be less than this limit. This limit must be 31-bit value.
    // The total number of required bit-per-bit operations is about (this limit)*(the number of points in the carcass);
    // the number of real long-per-long operations will be less in 64 times.

    final Point originOfGrid;
    final double[] stepsOfGrid;
    final Point stepsVector;
    final boolean zeroOriginOfGrid;
    final boolean unitStepsOfGrid;
    final IPoint iOriginOfGrid;
    final long[] iStepsOfGrid;
    volatile Boolean isRectangular = null;
    final IRange[] gridIndexRanges;
    final DirectPointSetUniformGridPattern[] lowerSurface;
    final DirectPointSetUniformGridPattern[] upperSurface;
    volatile UniformGridPattern surface = null;
    private volatile UniformGridPattern carcass = null;
    private volatile int maxCarcassMultiplier = -1;
    private final boolean trivialUnionDecomposition;
    private final List<List<Pattern>> minkowskiDecompositions;
    private final List<List<List<Pattern>>> allUnionDecompositions;

    /**
     * Creates a uniform grid pattern with the given origin and steps of the grid.
     *
     * <p>The <code>trivialUnionDecomposition</code> determines behavior of
     * {@link #unionDecomposition(int)} and {@link #allUnionDecompositions(int)} methods.
     * If it is <code>false</code>, they will perform some common algorithm
     * suitable for most patterns, that have no good Minkowski decompositions (alike spheres).
     * If it is <code>true</code>, they will return degenerated decomposition
     * consisting of this pattern as the only element.
     * You should use <code>true</code> argument in inheritors that have
     * a good Minkowski decomposition.
     *
     * <p>The passed <code>stepsOfGrid</code> argument is cloned by this method: no references to it
     * are maintained by the created pattern.
     *
     * @param originOfGrid              the {@link #originOfGrid() origin of the grid}.
     * @param stepsOfGrid               the {@link #stepsOfGrid() steps of the grid}.
     * @param trivialUnionDecomposition whether this pattern has the degenerated union decomposition.
     * @throws NullPointerException     if <code>originOfGrid</code> or <code>stepsOfGrid</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>originOfGrid.{@link Point#coordCount()
     *                                  coordCount()}!=stepsOfGrid.length</code>, or if one of the passed steps
     *                                  is zero (<code>==0.0</code>).
     */
    protected AbstractUniformGridPattern(Point originOfGrid, double[] stepsOfGrid, boolean trivialUnionDecomposition) {
        super(originOfGrid.coordCount());
        Objects.requireNonNull(stepsOfGrid, "Null stepsOfGrid");
        if (stepsOfGrid.length != originOfGrid.coordCount()) {
            throw new IllegalArgumentException("The number of steps of the grid is not equal "
                + "to the number of dimensions of the origin");
        }
        this.trivialUnionDecomposition = trivialUnionDecomposition;
        this.originOfGrid = originOfGrid;
        this.stepsOfGrid = stepsOfGrid.clone();
        for (double step : this.stepsOfGrid) {
            if (step <= 0.0) {
                throw new IllegalArgumentException("Zero or negative steps of the grid are not allowed");
            }
        }
        this.zeroOriginOfGrid = this.originOfGrid.isOrigin();
        this.stepsVector = Point.valueOf(this.stepsOfGrid);
        this.unitStepsOfGrid = stepsVector.equals(Point.valueOfEqualCoordinates(dimCount, 1.0));
        this.surelyInteger = this.originOfGrid.isInteger() && stepsVector.isInteger();
        this.iOriginOfGrid = this.surelyInteger ? this.originOfGrid.toIntegerPoint() : null;
        this.iStepsOfGrid = this.surelyInteger ? stepsVector.toIntegerPoint().coordinates() : null;
        this.gridIndexRanges = new IRange[dimCount]; //null-filled
        this.lowerSurface = new DirectPointSetUniformGridPattern[dimCount]; //null-filled
        this.upperSurface = new DirectPointSetUniformGridPattern[dimCount]; //null-filled
        this.minkowskiDecompositions = Collections.synchronizedList(new ArrayList<>(16));
        for (int k = 0; k < 16; k++) {
            this.minkowskiDecompositions.add(null);
        }
        this.allUnionDecompositions = Collections.synchronizedList(new ArrayList<>(16));
        for (int k = 0; k < 16; k++) {
            this.allUnionDecompositions.add(null);
        }
    }

    /**
     * This implementation returns the grid origin, specified in the constructor.
     *
     * @return the origin <b>o</b> of the uniform grid of this pattern.
     */
    public Point originOfGrid() {
        return this.originOfGrid;
    }

    /**
     * This implementation returns a new copy of Java array of grid steps, specified in the constructor.
     *
     * @return an array containing all grid steps of this pattern.
     */
    public double[] stepsOfGrid() {
        return this.stepsOfGrid.clone();
    }

    public double stepOfGrid(int coordIndex) {
        checkCoordIndex(coordIndex);
        return this.stepsOfGrid[coordIndex];
    }

    public boolean stepsOfGridEqual(UniformGridPattern pattern) {
        Objects.requireNonNull(pattern, "Null pattern argument");
        if (pattern.dimCount() != dimCount) {
            return false;
        }
        for (int k = 0; k < dimCount; k++) {
            if (stepsOfGrid[k] != pattern.stepOfGrid(k)) {
                return false;
            }
        }
        return true;
    }

    public abstract Set<IPoint> gridIndexes();

    public abstract IRange gridIndexRange(int coordIndex);

    /**
     * This implementation is based on the loop of calls of {@link #gridIndexRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return the ranges from minimal to maximal coordinate for all space dimensions.
     */
    public IRectangularArea gridIndexArea() {
        IRange[] result = new IRange[dimCount];
        for (int k = 0; k < result.length; k++) {
            result[k] = gridIndexRange(k);
        }
        return IRectangularArea.valueOf(result);
    }

    /**
     * This implementation is based on the loop of calls of {@link #gridIndexRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return minimal grid index for all space dimensions as a point.
     */
    public IPoint gridIndexMin() {
        long[] coordinates = new long[dimCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = gridIndexRange(k).min();
        }
        return IPoint.valueOf(coordinates);
    }

    /**
     * This implementation is based on the loop of calls of {@link #gridIndexRange(int)} method
     * for all coordinate indexes from <code>0</code> to <code>{@link #dimCount()}-1</code>.
     *
     * @return maximal grid index for all space dimensions as a point.
     */
    public IPoint gridIndexMax() {
        long[] coordinates = new long[dimCount];
        for (int k = 0; k < coordinates.length; k++) {
            coordinates[k] = gridIndexRange(k).max();
        }
        return IPoint.valueOf(coordinates);
    }

    public final boolean isOrdinary() {
        return zeroOriginOfGrid && unitStepsOfGrid;
    }

    /**
     * This implementation returns <code>true</code> if and only if
     * <code>{@link #pointCount()}=</code><i>r</i><sub>0</sub><i>r</i><sub>1</sub>...<code>&lt;Long.MAX_VALUE</code>,
     * where <i>r<sub>i</sub></i><code>={@link #gridIndexRange(int) gridIndexRange(<i>i</i>)}.{@link IRange#size()
     * size()}</code>.
     *
     * <p>This method caches its results: the following calls will work faster.
     *
     * <p>This method does not provide correct result, if a pattern contains <code>&ge;Long.MAX_VALUE</code> points.
     *
     * <p>This method should be overridden for rectangular patterns, implementing {@link RectangularPattern}
     * interface, or for patterns that surely are not rectangular.
     *
     * @return <code>true</code> if this pattern is <i>n</i>-dimensional rectangular parallelepiped.
     */
    public boolean isActuallyRectangular() {
        check:
        if (isRectangular == null) {
            long count = 1;
            for (int k = 0; k < dimCount; k++) {
                long size = gridIndexRange(k).size();
                count = net.algart.arrays.Arrays.longMul(count, size);
                if (count == Long.MIN_VALUE || count == Long.MAX_VALUE) {
                    // MAX_VALUE is also prohibited here: it may be returned by pointCount() for large number of points
                    // Note that this situation is impossible for actually rectangular pattern, represented
                    // by DirectPointSetUniformGridPattern: the number of its points is <=Integer.MAX_VALUE
                    isRectangular = false;
                    break check;
                }
            }
            isRectangular = count == pointCount();
        }
        return isRectangular;
    }

    public abstract UniformGridPattern gridIndexPattern();

    public abstract UniformGridPattern shiftGridIndexes(IPoint shift);

    public UniformGridPattern lowerSurface(int coordIndex) {
        checkCoordIndex(coordIndex);
        synchronized (lowerSurface) {
            if (lowerSurface[coordIndex] == null) {
                long[] shiftCoordinates = new long[dimCount]; // zero-filled
                shiftCoordinates[coordIndex] = -1;
                IPoint shift = IPoint.valueOf(shiftCoordinates);
                Set<IPoint> points = gridIndexes();
                Set<IPoint> resultPoints = new HashSet<>();
                for (IPoint ip : points) {
                    if (ip.coord(coordIndex) == Long.MIN_VALUE // avoiding overflow in the next check
                        || !points.contains(ip.add(shift)))
                    {
                        resultPoints.add(ip);
                    }
                }
                lowerSurface[coordIndex] = newCompatiblePattern(resultPoints);
            }
            return lowerSurface[coordIndex];
        }
    }

    public UniformGridPattern upperSurface(int coordIndex) {
        checkCoordIndex(coordIndex);
        synchronized (upperSurface) {
            if (upperSurface[coordIndex] == null) {
                long[] shiftCoordinates = new long[dimCount]; // zero-filled
                shiftCoordinates[coordIndex] = 1;
                IPoint shift = IPoint.valueOf(shiftCoordinates);
                Set<IPoint> points = gridIndexes();
                Set<IPoint> resultPoints = new HashSet<>();
                for (IPoint ip : points) {
                    if (ip.coord(coordIndex) == Long.MIN_VALUE // avoiding overflow in the next check
                        || !points.contains(ip.add(shift)))
                    {
                        resultPoints.add(ip);
                    }
                }
                upperSurface[coordIndex] = newCompatiblePattern(resultPoints);
            }
            return upperSurface[coordIndex];
        }
    }

    public Pattern surface() {
        if (surface == null) {
            Set<IPoint> resultPoints = new HashSet<>();
            for (int k = 0; k < dimCount; k++) {
                resultPoints.addAll(lowerSurface(k).gridIndexes());
                resultPoints.addAll(upperSurface(k).gridIndexes());
            }
            surface = newCompatiblePattern(resultPoints);
        }
        return surface;
    }

    @Override
    public abstract long pointCount();

    @Override
    public Set<Point> points() {
        Set<Point> result = new HashSet<>();
        for (IPoint p : gridIndexes()) {
            result.add(p.scaleAndShift(stepsOfGrid, originOfGrid));
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<IPoint> roundedPoints() {
        if (zeroOriginOfGrid && unitStepsOfGrid) {
            return Collections.unmodifiableSet(gridIndexes());
        }
        return super.roundedPoints();
    }

    @Override
    public Range coordRange(int coordIndex) {
        IRange r = gridIndexRange(coordIndex);
        return coordRange(coordIndex, r);
    }

    public boolean isSurelyInteger() {
        return this.surelyInteger;
    }

    public UniformGridPattern round() {
        return zeroOriginOfGrid && unitStepsOfGrid ? this : super.round();
    }

    public abstract UniformGridPattern projectionAlongAxis(int coordIndex);

    @Override
    public UniformGridPattern minBound(int coordIndex) {
        checkCoordIndex(coordIndex);
        synchronized (minBound) {
            if (minBound[coordIndex] == null) {
                Set<IPoint> points = gridIndexes();
                Map<IPoint, IPoint> map = new HashMap<>();
                for (IPoint p : points) {
                    IPoint projection = p.projectionAlongAxis(coordIndex);
                    IPoint bound = map.get(projection);
                    if (bound == null || p.coord(coordIndex) < bound.coord(coordIndex)) {
                        map.put(projection, p);
                    }
                }
                minBound[coordIndex] = newCompatiblePattern(map.values());
            }
            return (UniformGridPattern) minBound[coordIndex];
        }
    }

    @Override
    public UniformGridPattern maxBound(int coordIndex) {
        checkCoordIndex(coordIndex);
        synchronized (maxBound) {
            if (maxBound[coordIndex] == null) {
                Set<IPoint> points = gridIndexes();
                Map<IPoint, IPoint> map = new HashMap<>();
                for (IPoint p : points) {
                    IPoint projection = p.projectionAlongAxis(coordIndex);
                    IPoint bound = map.get(projection);
                    if (bound == null || p.coord(coordIndex) > bound.coord(coordIndex)) {
                        map.put(projection, p);
                    }
                }
                maxBound[coordIndex] = newCompatiblePattern(map.values());
            }
            return (UniformGridPattern) maxBound[coordIndex];
        }
    }

    /** //TODO!! - that it checks isActuallyRectangular()
     * This method is fully implemented in this class and usually should not be overridden.
     * If you override it, you must override {@link #maxCarcassMultiplier()} also,
     * because that method returns the private field calculated by this one.
     *
     * @return the <i>carcass</i> of this pattern.
     */
    @Override
    public UniformGridPattern carcass() {
        if (pointCount() <= 2) {
            maxCarcassMultiplier = Integer.MAX_VALUE;
            return this;
        }
        if (carcass == null) {
            if (isActuallyRectangular()) { // uniform-grid rectangular pattern
                IRectangularArea area = gridIndexArea();
                long[] vertex = new long[dimCount];
                IRange[] r01 = Collections.nCopies(dimCount, IRange.valueOf(0, 1)).toArray(new IRange[dimCount]);
                Set<IPoint> vertices01 = new BasicRectangularPattern(r01).roundedPoints();
                // - all vertices of [0..1]^dimCount parallelepiped
                Set<IPoint> resultPoints = new HashSet<>();
                for (IPoint vertex01 : vertices01) {
                    for (int k = 0; k < vertex.length; k++) {
                        long coord = vertex01.coord(k);
                        assert coord == 0 || coord == 1;
                        vertex[k] = coord == 1 ? area.max(k) : area.min(k);
                    }
                    resultPoints.add(IPoint.valueOf(vertex));
                }
                maxCarcassMultiplier = Integer.MAX_VALUE;
                carcass = newCompatiblePattern(resultPoints);
            } else { // any uniform-grid pattern
                IPoint correctionShift = this.gridIndexMin();
                final UniformGridPattern p = this.shiftGridIndexes(correctionShift.symmetric());
                //TODO!! shiftGridIndexes is still dangerous for extremely large patterns
                // provides non-negative coordinates only

                IRange[] r3x3 = Collections.nCopies(dimCount, IRange.valueOf(-2, 2)).toArray(new IRange[dimCount]);
                Set<IPoint> shifts = new BasicRectangularPattern(r3x3).roundedPoints();
                // - all shifts inside of [-2..2]^dimCount parallelepiped
                Set<IPoint> bestBoundaryForDirection = null;
                Set<IPoint> intersectionOfBoundaries = null;
                final Set<IPoint> points = p.gridIndexes();
                int minPointCount = Integer.MAX_VALUE;
                for (IPoint shift : shifts) {
                    if (shift.isOrigin()) {
                        continue;
                    }
                    IPoint halfShift = shift.multiply(0.5);
                    if (shift.equals(halfShift.multiply(2.0))) {
                        // all coordinates are even, for example, (2,0) or (-2,2):
                        // no sense to check such trivial shift in addition to its half
                        continue;
                    }
                    Set<IPoint> boundaryForDirection = gridBoundaryForDirection(points, shift);
                    int pointCount = boundaryForDirection.size();
                    if (bestBoundaryForDirection == null || pointCount < minPointCount) {
                        bestBoundaryForDirection = boundaryForDirection;
                        minPointCount = pointCount;
                    }
                    if (intersectionOfBoundaries == null) {
                        intersectionOfBoundaries = new HashSet<>(boundaryForDirection);
                    } else {
                        intersectionOfBoundaries.retainAll(boundaryForDirection);
                    }
                }
                AbstractUniformGridPattern alwaysWorkingCarcass = new BasicDirectPointSetUniformGridPattern(
                    dimCount, bestBoundaryForDirection);
                AbstractUniformGridPattern goodCarcass = new BasicDirectPointSetUniformGridPattern(
                    dimCount, intersectionOfBoundaries);
                if (dimCount <= 1 || // strange use: why to build carcass for 1-dimensional segments?
                    dimCount > 16 || // very strange: probably too large to check
                    intGridDimensions(p) == null) // too large to check (>=2^31)
                {
                    maxCarcassMultiplier = 2;
                    // - see below the contrary instance after "c = alwaysWorkingCarcass" assignment
                    return carcass = alwaysWorkingCarcass.shiftGridIndexes(correctionShift)
                        .scale(stepsOfGrid).shift(originOfGrid);
                }
                // Now we are sure that we have from 2 to 16 dimensions: it will be used by the algorithm
                IRange[] ranges = p.gridIndexArea().ranges(); //32bit ranges
                for (int k = 0; k < ranges.length; k++) {
                    ranges[k] = IRange.valueOf(2 * ranges[k].min(), 2 * ranges[k].max()); //maximally 33bit ranges
                }
                AbstractUniformGridPattern circumscribed2P = new BasicRectangularPattern(ranges);
                // Now "circumscribed" contains 2(x)P; scaling and shifting is not necessary for further needs
                if (circumscribed2P.largePointCount() >
                    maxNumberOfPointsInNewParallelepipedWhileCheckingLargeCarcasses(dimCount)) // too large to check
                {
                    maxCarcassMultiplier = 2;
                    return carcass = alwaysWorkingCarcass.shiftGridIndexes(correctionShift)
                        .scale(stepsOfGrid).shift(originOfGrid);
                }
                long m = 1;
                for (; ; ) {
                    // Now a virtual "circumscribed" pattern contains 2m(x)P.
                    // Let's check the volume of the circumscribed of 4m(x)P
                    long newVolume = 1;
                    for (int k = 0; k < ranges.length; k++) {
                        ranges[k] = IRange.valueOf(2 * ranges[k].min(), 2 * ranges[k].max());
                        newVolume *= ranges[k].size();
                        // Overflow impossible: the volume at the previous step was 31-bit value,
                        // and now it is increased less than in 2^dimCount()<=2^16 times.
                    }
                    if (newVolume > maxNumberOfPointsInNewParallelepipedWhileCheckingLargeCarcasses(dimCount)) {
                        break;
                    }
                    m *= 2;
                }
                int[] dimensions = intGridDimensions(new BasicRectangularPattern(ranges));
                // Now we are sure that the circumscribed parallelepiped contains 2m(x)P
                // and is not too large, essentially less than 2^31
                assert dimensions != null : "Probably too large "
                    + "maxNumberOfPointsInNewParallelepipedWhileCheckingLargeCarcasses constants";

                final AbstractUniformGridPattern pUnscaled = new BasicDirectPointSetUniformGridPattern(
                    dimCount, points);
                TinyBitMatrix temp1 = new TinyBitMatrix(dimensions); // allocating first matrix
                TinyBitMatrix temp2 = new TinyBitMatrix(dimensions); // allocating second matrix
                int lastDim = (int) circumscribed2P.gridIndexRange(dimensions.length - 1).size();
                int lastButOneDim = (int) circumscribed2P.gridIndexRange(dimensions.length - 2).size();
                dimensions[dimensions.length - 2] = lastButOneDim;
                dimensions[dimensions.length - 1] = lastDim;
                TinyBitMatrix temp = temp1.reDim(dimensions);
                TinyBitMatrix multiple = temp2.reDim(dimensions);
                // Now we will process reduced matrices and increase them:
                // all other bits are zero, because we work with positive points only
                temp.putPattern(pUnscaled); // putting integer points
                multiple.simpleDilation(temp, goodCarcass); // multiple = P(+)C
                long correctCardinality = multiple.cardinality();
                multiple.simpleDilation(temp, alwaysWorkingCarcass); // multiple = P(+)P
                long probableCardinality = multiple.cardinality();
                int n = 1;
                AbstractUniformGridPattern c = goodCarcass;
                if (probableCardinality != correctCardinality) {
                    c = alwaysWorkingCarcass;
                    // There is a simple theorem that for any kind of pattern P(+)P = P(+)c in this case.
                    // Really, let P = union of segments Si along the chosen axis,
                    // the ends of these segments are the points of the found boundary.
                    // Then P(+)P = union of all Si(+)Sj, where we can suppose |Si|>=|Sj|
                    // (to include each pair in this union only once).
                    // Since |Si|>=|Sj|, here Si(+)Sj = Si(+)Cj, where Cj is a pair of ends of Sj,
                    // so P(+)P = union of all Si(+)Cj, where |Si|>=|Sj|.
                    // And P(+)c = union of all Si(+)Cj for ANY i, j.
                    // So, P(+)c is a superset of P(+)P.
                    // The inverse statement is obvious: P(+)c is a subset of P(+)P.
                    // The theorem is proved.
                    //
                    // Unfortunately, it's still possible that 4(x)P != 2(x)P (+) 2*c.
                    // The contrary instance: 1-dimensional pattern {0, 1, 2, 10}.
                    // Here 2(x)P = {0..4, 10..12, 20}, c = {0, 2, 10}, 2*c = {0, 4, 20},
                    // 4(x)P = {0..8, 10..16, 20..24, 30..32, 40}, but
                    // 2(x)P (+) 2*c = {0..8, 10..12, 14..16, 20..24, 30..32, 40}.
                    // So, we must test "alwaysWorkingCarcass" below, as well as the better "goodCarcass" pattern.
                }
                // Now P(+)P=P(+)c, P is this pattern
                UniformGridPattern nc = c; // will be n*c; now n=1
                AbstractUniformGridPattern np = DEBUG_MODE ? pUnscaled : null; // will be n(x)P
                AbstractUniformGridPattern np2 = DEBUG_MODE ? pUnscaled.simpleMinkowskiAdd(c) : null; // will be 2n(x)P
                while (n < m) {
                    // Here 2n<=m, so we are sure that the circumscribed parallelepiped contains 4n(x)P.
                    // Now multiple = 2n(x)P = n(x)P (+) n*c (here k(x)P means P(+)P(+)...(+)P, k times)
                    // Let's check whether 4n(x)P = 2x(x)P (+) 2n*c
                    if (4 * n < 0) {
                        assert 4 * n == Integer.MIN_VALUE;
                        break; // no sense for further checks: 4n = 2^31 > Integer.MAX_VALUE
                    }
//                    System.out.println("Checking " + n + " " + numberOfOperations + "," + circumscribed + ", " + c);

                    lastDim = 2 * lastDim - 1;
                    lastButOneDim = 2 * lastButOneDim - 1;
                    multiple = increaseMatrixBy2LastCoordinates(multiple, lastDim, lastButOneDim);
                    temp = temp.reDim(multiple.dimensions());

                    AbstractUniformGridPattern probable, correct;
                    UniformGridPattern nc2 = nc.multiply(2); // 2n*c
                    temp.simpleDilation(multiple, nc2);
                    // temp = 2n(x)P (+) 2n*c
                    probableCardinality = temp.cardinality();
                    if (DEBUG_MODE) {
                        if (np2.pointCount() <= 20000) { // avoid too much slowing down
                            probable = np2.simpleMinkowskiAdd(nc2.round()); // round() provides 1x1x... grid
                            if (probable.pointCount() != probableCardinality) {
                                throw new AssertionError("Internal error A in carcass method: probe matrix contains "
                                    + probableCardinality + " unit points instead of correct " + probable.pointCount()
                                    + ": n=" + n + ", p(unscaled)=" + pUnscaled + ", c=" + c
                                    + ", 2n(x)P(+)2n*c=" + probable);
                            }
                        }
                    }

                    temp.simpleDilation(multiple, nc);
                    multiple.simpleDilation(temp, nc);
                    // multiple = 4n(x)P, because 2n(x)P (+) 2n(x)P =
                    // = n(x)P (+) n(x)P (+) 2n(x)P =
                    // = n(x)P (+) (n(x)P (+) n(x)P) (+) n*c =
                    // = n(x)P (+) (n(x)P (+) n*c) (+) n*c =
                    // = 2n(x)P (+) n*c (+) n*c
                    correctCardinality = multiple.cardinality();
                    if (DEBUG_MODE) {
                        if (np2.pointCount() <= 20000) { // avoid too much slowing down
                            if (np2.pointCount() <= 1000) {
                                correct = np2.simpleMinkowskiAdd(np).simpleMinkowskiAdd(np);
                                // it's faster than np2(+)np2: for example, if np=n(x)p is ~KxKx...xK, K^m points,
                                // then np2(+)np2 requires O(K^2m * 2^2m) operations,
                                // but np2(+)np(+)np requires O(K^2m * (2^m+3^m)) operations only
                            } else {
                                UniformGridPattern ncRound = nc.round(); // round() provides 1x1x... grid
                                correct = np2.simpleMinkowskiAdd(ncRound).simpleMinkowskiAdd(ncRound);
                                // no ability for more thorough check
                            }
                            if (correct.pointCount() != correctCardinality) {
                                throw new AssertionError("Internal error B in carcass method: probe matrix contains "
                                    + correctCardinality + " unit points instead of correct " + correct.pointCount()
                                    + ": n=" + n + ", p(unscaled)=" + p + ", c=" + c + ", 4n(x)P=" + correct);
                            }
                            np = np2;
                            np2 = correct;
                        }
                    }
                    if (probableCardinality != correctCardinality) {
                        break;
                    }
                    // So, 4n(x)P = 2x(x)P (+) 2n*c
                    nc = nc2;
                    n *= 2;
                }
                // Now 2n(x)P = n(x)P (+) n(*)c,
                // and the same statement if true for 1, 2, 4, ..., n/2 (n is a power of two).
                // Moreover, we state that, for any k < n, (n+k)(x)P = n(x)P (+) k(*)c.
                // Proof.
                // Let k = 2^i1 + 2^i2 + ... (binary representation of the number k).
                // then k(x)P (+) k(*)c = (2^i1(x)P (+) 2^i1*c) (+) (2^i2(x)P (+) 2^i2*c) (+) ... = 2k(x)P.
                // (n+k)(x)P = (n-k)(x)P (+) 2k(x)P = (n-k)(x)P (+) k(x)P (+) k(*)c = n(x)P (+) k(*)c.
                // The statement is proved.
                // The same statement if true for n/2, n/4, ...
                // It means that for any k, 0<=k<=2n, we have
                // k(x)P = P (+) c (+) 2*c (+) ... (+) 2^(i-1)*c (+) (k-2^i)*c,
                // where i is the maximal integer for which 2^i<k.
                maxCarcassMultiplier = 2 * n;
                carcass = c.shiftGridIndexes(correctionShift).scale(stepsOfGrid).shift(originOfGrid);
            }
        }
        return carcass;
    }

    /** //TODO!! - write that it checks isActuallyRectangular()
     * This method is fully implemented in this class and usually should not be overridden.
     *
     * @return the maximal multiplier, for which the calculation of the Minkowski multiple can be optimized
     *         by using the {@link #carcass() carcass}.
     */
    public int maxCarcassMultiplier() {
        if (isActuallyRectangular()) {
            return Integer.MAX_VALUE;
        }
        carcass(); // filling maxCarcassMultiplier
        return maxCarcassMultiplier;
    }


    @Override
    public abstract UniformGridPattern shift(Point shift);

    @Override
    public UniformGridPattern symmetric() {
        return (UniformGridPattern) super.symmetric();
    }

    @Override
    public UniformGridPattern multiply(double multiplier) {
        return (UniformGridPattern) super.multiply(multiplier);
    }

    @Override
    public abstract UniformGridPattern scale(double... multipliers);

    /**
     * This implementation is based on the loop on all points returned by {@link #roundedPoints()} method in both patterns
     * and always returns the {@link Patterns#newIntegerPattern(Collection) simple pattern}
     * consisting of sums of all point pairs.
     * This algorithm may be very slow for large patterns
     * (<i>O</i>(<i>NM</i>) operations, <i>N</i>={@link #pointCount()}, <i>M</i>=added.{@link #pointCount()})
     * and does not work at all if the number of resulting points is greater than <code>Integer.MAX_VALUE</code>.
     * Please override this method if there is better implementation.
     *
     * @param added another pattern.
     * @return the Minkowski sum of this and another patterns.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the numbers of space dimensions of both patterns are different.
     */
    public Pattern minkowskiAdd(Pattern added) {
        Objects.requireNonNull(added, "Null added argument");
        if (added.dimCount() != this.dimCount) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + added.dimCount() + " instead of " + this.dimCount);
        }
        long addedPointCount = added.pointCount();
        if (addedPointCount == 1) {
            return shift(added.coordMin());
        }
        if (!(added instanceof UniformGridPattern ugAdded)) {
            return super.minkowskiAdd(added);
        }
        if (!stepsOfGridEqual(ugAdded)) {
            return super.minkowskiAdd(added);
        }
        if (pointCount() < MIN_POINT_COUNT_TO_OPTIMIZE_MINKOWSKI_ADD
            && addedPointCount < MIN_POINT_COUNT_TO_OPTIMIZE_MINKOWSKI_ADD)
        {
            return simpleMinkowskiAdd(ugAdded);
        }

        UniformGridPattern circumscribedOfThis = new BasicRectangularPattern(gridIndexArea().ranges());
        UniformGridPattern circumscribedOfAdded = new BasicRectangularPattern(ugAdded.gridIndexArea().ranges());
        Pattern halfCircumscribed = circumscribedOfThis.multiply(0.5).round()
            .minkowskiAdd(circumscribedOfAdded.multiply(0.5).round());
        // division by 2 is used to avoid overflow
        double volumeOfCircumscribed = halfCircumscribed.largePointCount() * Math.pow(2.0, dimCount);
        // The number of operations in the optimized algorithm is ~volumeOfCircumscribed*added.pointCount()
        // (very quick bit operations);
        // the number of operations in the simpleMinkowskiSum is ~this.pointCount()*added.pointCount()
        if (volumeOfCircumscribed > largePointCount()
            * MAX_RELATION_OF_PARALLELEPIPED_VOLUME_TO_THIS_TO_OPTIMIZE_IN_MINKOWSKI_ADD)
        {
            return simpleMinkowskiAdd(ugAdded);
        }

        // Now we are sure that volumeOfCircumscribed is not too large,
        // if this.pointCount can be represented by "int" type.
        // If this.pointCount is greater than Integer.MAX_VALUE, the simple algorithm is also not applicable:
        // so, OutOfMemoryError and IllegalArgumentException are suitable results.

        Pattern circumscribed = circumscribedOfThis.minkowskiAdd(circumscribedOfAdded);
        // (can throw TooManyPointsInPatternError or TooLargePatternCoordinatesException for too large patterns)
        assert circumscribed instanceof UniformGridPattern;

        int[] dimensions = intGridDimensions((UniformGridPattern) circumscribed);
        Objects.requireNonNull(dimensions, "Too large pattern for Minkowski adding: "
                + "some dimensions are 2^31 or greater");

        assert circumscribedOfThis.isOrdinary();
        assert circumscribedOfAdded.isOrdinary();
        UniformGridPattern thisGridIndexes = gridIndexPattern();
        UniformGridPattern addedGridIndexes = ugAdded.gridIndexPattern();
        IPoint thisShift = circumscribedOfThis.gridIndexMin();
        IPoint addedShift = circumscribedOfAdded.gridIndexMin();
        TinyBitMatrix src = new TinyBitMatrix(dimensions);
        src.putPattern(thisGridIndexes.shiftGridIndexes(thisShift.symmetric()));
        // shifting is necessary to provide positive coordinates for the following getPattern method
        TinyBitMatrix dest = new TinyBitMatrix(dimensions);
        dest.simpleDilation(src, addedGridIndexes.shiftGridIndexes(addedShift.symmetric()));
        // shifting is necessary to provide positive coordinates for the following getPattern method
        UniformGridPattern drawnPattern = dest.getPattern(thisShift.add(addedShift));
        // thisShift + addedShift cannot lead to overflow, because both are <Long.MAX_VALUE/2
        return drawnPattern.scale(stepsOfGrid).shift(originOfGrid.add(ugAdded.originOfGrid()));
    }

    /**
     * This implementation is based on the loop on all points returned by {@link #roundedPoints()}
     * method in both patterns
     * and always returns the {@link Patterns#newIntegerPattern(Collection) simple pattern}
     * consisting of sums of all point pairs.
     * This algorithm may be very slow for large patterns
     * (<i>O</i>(<i>NM</i>) operations, <i>N</i>={@link #pointCount()}, <i>M</i>=added.{@link #pointCount()})
     * and does not work at all if the number of resulting points is greater than <code>Integer.MAX_VALUE</code>.
     * Please override this method if there is better implementation.
     *
     * @param subtracted another pattern.
     * @return the erosion of this pattern by the specified pattern
     *         or {@code null} if this erosion is the empty set.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the numbers of space dimensions of both patterns are different.
     */
    public Pattern minkowskiSubtract(Pattern subtracted) {
        Objects.requireNonNull(subtracted, "Null subtracted argument");
        if (subtracted.dimCount() != this.dimCount) {
            throw new IllegalArgumentException("Dimensions count mismatch: "
                + subtracted.dimCount() + " instead of " + this.dimCount);
        }
        long subtractedPointCount = subtracted.pointCount();
        if (subtractedPointCount == 1) {
            return shift(subtracted.coordMin().symmetric());
        }
        if (!(subtracted instanceof UniformGridPattern ugSubtracted)) {
            return super.minkowskiSubtract(subtracted);
        }
        if (!stepsOfGridEqual(ugSubtracted)) {
            return super.minkowskiSubtract(subtracted);
        }
        Set<IPoint> subtractedPoints = ugSubtracted.gridIndexes();
        IPoint minimal = null;
        double minimalDistance = Double.POSITIVE_INFINITY;
        for (IPoint p : subtractedPoints) {
            double distance = p.distanceFromOrigin();
            if (minimal == null || distance < minimalDistance) {
                minimal = p;
                minimalDistance = distance;
            }
        }
        assert minimal != null : "Empty subtracted.points()";
        boolean containsOrigin = minimal.isOrigin();
        Set<IPoint> points = gridIndexes();
        Set<IPoint> resultPoints = new HashSet<>();
        mainLoop:
        for (IPoint p : points) {
            // Formally, we need here a loop for infinity number of all points p in the space;
            // but we can use the simple fact that the result is a subset of thisPattern.shift(-minimal)
            if (!containsOrigin) {
                p = p.subtract(minimal);
            }
            for (IPoint q : subtractedPoints) {
                if (q.equals(minimal)) {
                    continue; // no sense to check
                }
                if (!points.contains(p.add(q))) {
                    continue mainLoop; // don't add p
                }
            }
            resultPoints.add(p);
        }
        return resultPoints.isEmpty() ? null :
            Patterns.newIntegerPattern(resultPoints).scale(stepsOfGrid)
                .shift(originOfGrid.subtract(ugSubtracted.originOfGrid()));
    }

    /**
     * This implementation returns <code>Collections.&lt;Pattern&gt;singletonList(thisInstance)</code>
     * for non-rectangular patterns or a good decomposition if {@link #isActuallyRectangular()} method
     * returns <code>true</code>.
     * This method caches its results for several little values of the argument:
     * the following calls will work faster.
     * Please override this method if there is better implementation.
     *
     * @param minimalPointCount this method does not try to decompose patterns that contain
     *                          less than <code>minimalPointCount</code> points.
     *                          In particular, if the minkowski sum of several patterns containing
     *                          less than <code>minimalPointCount</code> points, this method should return
     *                          this sum in the resulting list instead of its summands.
     * @return the decomposition of this pattern to Minkowski sum.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<Pattern> minkowskiDecomposition(int minimalPointCount) {
        if (minimalPointCount < 0) {
            throw new IllegalArgumentException("Negative minimalPointCount");
        }
        if (!isActuallyRectangular()) {
            return Collections.singletonList(this);
        }
        // Below is decomposition of rectangular patterns only
        long pointCount = pointCount();
        if (pointCount <= 2) {
            return Collections.singletonList(this);
        }
        if (minimalPointCount < minkowskiDecompositions.size()) {
            List<Pattern> result = minkowskiDecompositions.get(minimalPointCount);
            if (result != null) {
                return result;
            }
        }
        if (pointCount < minimalPointCount) {
            return Collections.singletonList(this);
        }
        IRectangularArea gridIndexArea = gridIndexArea(); // actualization of all coordinate ranges
        boolean joinShortSegments = minimalPointCount > 4;
        // if <=4, then there is no sense to check shorter segments in the loop below:
        // in the best case, such a check will replace 2-point pattern (0, 1) with rectangular pattern (0..1)
        // or 3- or 4-point pattern (0,1,2[,3]) with a Minkowski sum of two patterns
        List<Pattern> result = new ArrayList<>();
        Point origin = Point.origin(dimCount);
        long[] shifts = new long[dimCount];
        double[] coordinates = new double[dimCount]; // zero-filled
        IRange[] ranges = new IRange[dimCount];
        Arrays.fill(ranges, IRange.valueOf(0, 0));
        boolean allShiftsAreZero = true;
        for (int k = 0; k < dimCount; k++) {
            shifts[k] = gridIndexArea.min(k);
            allShiftsAreZero &= shifts[k] == 0;
            boolean startSegmentAdded = false;
            for (long m = 1, n = gridIndexArea.size(k), sumLen = 1; sumLen < n; m *= 2) {
                assert m == sumLen : "m != sumLen";
                if (m > n - sumLen) {
                    m = n - sumLen;
                }
                // sumLen + m is the new summary length after the adding the summand at this loop iteration
                if (sumLen + m >= minimalPointCount && joinShortSegments && !startSegmentAdded) {
                    ranges[k] = IRange.valueOf(0, sumLen - 1);
                    result.add(new BasicRectangularPattern(origin, stepsOfGrid, ranges));
                    startSegmentAdded = true;
                }
                sumLen += m;
                if (!joinShortSegments || sumLen >= minimalPointCount) {
                    coordinates[k] = m * stepsOfGrid[k];
                    result.add(new TwoPointsPattern(origin, Point.valueOf(coordinates)));
                }
            }
            coordinates[k] = 0.0; // restoring zero coordinate
            ranges[k] = IRange.valueOf(0, 0);  // restoring zero range
        }
        if (!(allShiftsAreZero && originOfGrid.isOrigin())) {
            result.add(new OnePointPattern(IPoint.valueOf(shifts).scaleAndShift(stepsOfGrid, originOfGrid)));
            // It's better than correction of the last pattern:
            // one-point pattens may be usually processed very quickly (lazily),
            // and two-point pattern are usually processed better if they have positive points only.
        }
        result = Collections.unmodifiableList(result);
        if (minimalPointCount < minkowskiDecompositions.size()) {
            minkowskiDecompositions.set(minimalPointCount, result);
        }
//        System.out.println(this + " decomposed into " + result);
        return result;
    }

    /**
     * This implementation returns
     * <code>{@link #minkowskiDecomposition(int) minkowskiDecomposition(0)}.size()&gt;1</code>.
     * Please override this method if {@link #minkowskiDecomposition(int)} method
     * works slowly and it's possible to know, whether the pattern has Minkowski decomposition, much faster.
     *
     * @return <code>true</code> if the Minkowski decomposition contains 2 or more elements.
     */
    public boolean hasMinkowskiDecomposition() {
        return minkowskiDecomposition(0).size() > 1;
    }


    /**
     * This implementation uses a common algorithm that usually provide good results.
     * This method caches its results for several little values of the argument:
     * the following calls will work faster.
     * Please override this method if the better implementation is known.
     *
     * @param minimalPointCount the minimal number of points in every pattern in all resulting decompositions:
     *                          all pattern containing less points should be joined into one element
     *                          of the resulting list.
     * @return several good variants of decomposition of this pattern to the union of patterns.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<List<Pattern>> allUnionDecompositions(int minimalPointCount) {
        if (minimalPointCount < 0) {
            throw new IllegalArgumentException("Negative minimalPointCount");
        }
        if (trivialUnionDecomposition) {
            return Collections.singletonList(Collections.singletonList(this));
        }
        if (minimalPointCount < allUnionDecompositions.size()) {
            List<List<Pattern>> result = allUnionDecompositions.get(minimalPointCount);
            if (result != null) {
                return result;
            }
        }
        List<IPoint> points = new ArrayList<>(gridIndexes());
        if (points.size() < minimalPointCount) {
            return Collections.singletonList(Collections.singletonList(this));
        }
        List<IPoint> retainedPoints = new ArrayList<>();
        List<List<Pattern>> decompositions = new ArrayList<>();
        long[] totalComplexities = new long[dimCount];
        long bestTotalComplexity = Long.MAX_VALUE;
        for (int coordIndex = 0; coordIndex < dimCount; coordIndex++) {
            retainedPoints.clear();
            List<UniformGridPattern> gridIndexes = joinPointsToSegments(
                points, retainedPoints, coordIndex, minimalPointCount);

            // Adding retained points
            if (!retainedPoints.isEmpty()) {
                gridIndexes.add(Patterns.newIntegerPattern(retainedPoints.toArray(new IPoint[0])));
            }

            // Calculating the quality and storing the result
            double totalCount = 0;
            for (UniformGridPattern ptn : gridIndexes) {
                long pointCount = ptn.pointCount();
                if (ptn.isActuallyRectangular() && pointCount >= minimalPointCount) {
                    totalCount += 2 * (64 - Long.numberOfLeadingZeros(pointCount - 1));
                } else {
                    totalCount += pointCount;
                }
            }
            long totalComplexity = StrictMath.round(totalCount);
            // in a very improbable case, when totalCount > Long.MAX_VALUE, we shall get here Long.MAX_VALUE
            decompositions.add(new ArrayList<>(gridIndexes));
            totalComplexities[coordIndex] = totalComplexity;
            if (totalComplexity <= bestTotalComplexity) {
                bestTotalComplexity = totalComplexity;
            }
        }
        List<List<Pattern>> bestResults = new ArrayList<>();
        for (int dimIndex = 0; dimIndex < dimCount; dimIndex++) {
            if (totalComplexities[dimIndex] == bestTotalComplexity) {
                List<Pattern> gridIndexes = decompositions.get(dimIndex);
                List<Pattern> resultPatterns = new ArrayList<>();
                for (Pattern pattern : gridIndexes) {
                    resultPatterns.add(pattern.scale(stepsOfGrid).shift(originOfGrid));
                }
                bestResults.add(Collections.unmodifiableList(resultPatterns));
            }
        }
        assert !bestResults.isEmpty() : "Empty bestResults";
        List<List<Pattern>> result = Collections.unmodifiableList(bestResults);
        if (minimalPointCount < allUnionDecompositions.size()) {
            allUnionDecompositions.set(minimalPointCount, result);
        }
        return result;
    }

    public static boolean isAllowedGridIndex(IPoint gridIndex) {
        Objects.requireNonNull(gridIndex, "Null grid index");
        for (int k = 0, n = gridIndex.coordCount(); k < n; k++) {
            long coord = gridIndex.coord(k);
            if (coord < -Pattern.MAX_COORDINATE || coord > Pattern.MAX_COORDINATE) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllowedGridIndexRange(IRange gridIndexRange) {
        Objects.requireNonNull(gridIndexRange, "Null grid index range");
        long min = gridIndexRange.min();
        long max = gridIndexRange.max();
        assert min <= max;
        return min >= -MAX_COORDINATE && max <= MAX_COORDINATE && max - min <= MAX_COORDINATE;
    }

    protected String gridToString() {
        if (isOrdinary()) {
            return "trivial grid";
        }
        StringBuilder sb = new StringBuilder("steps ");
        for (int k = 0; k < dimCount; k++) {
            if (k > 0) {
                sb.append(" x ");
            }
            sb.append(stepsOfGrid[k]);
        }
        sb.append(" starting from ").append(zeroOriginOfGrid ? "the origin" : originOfGrid.toString());
        return sb.toString();
    }

    Range coordRange(int coordIndex, IRange gridIndexRange) {
        return Range.valueOf(
            gridIndexRange.min() * stepsOfGrid[coordIndex] + originOfGrid.coord(coordIndex),
            gridIndexRange.max() * stepsOfGrid[coordIndex] + originOfGrid.coord(coordIndex));
    }

    DirectPointSetUniformGridPattern newCompatiblePattern(Collection<IPoint> gridIndexes) {
        return Patterns.newUniformGridPattern(this.originOfGrid, this.stepsOfGrid, gridIndexes);
    }

    static void checkGridIndex(IPoint gridIndex) throws TooLargePatternCoordinatesException {
        if (!isAllowedGridIndex(gridIndex)) {
            throw new TooLargePatternCoordinatesException("Some grid index " + gridIndex + " is out of "
                + " out of -" + MAX_COORDINATE + ".." + MAX_COORDINATE
                + " range and cannot be used for building a pattern");
        }
    }

    static void checkGridIndexRange(IRange gridIndexRange) throws TooLargePatternCoordinatesException {
        Objects.requireNonNull(gridIndexRange, "Null grid index range");
        long min = gridIndexRange.min();
        long max = gridIndexRange.max();
        assert min <= max;
        checkGridIndexRange(min, max);
    }

    static void checkGridIndexRange(long min, long max) throws TooLargePatternCoordinatesException {
        if (min < -MAX_COORDINATE || max > MAX_COORDINATE) {
            throw new TooLargePatternCoordinatesException("Some grid index range " + min + ".." + max
                + " is out of -" + MAX_COORDINATE + ".." + MAX_COORDINATE
                + " range and cannot be used for building a pattern");
        }
        if (max - min > MAX_COORDINATE) {
            throw new TooLargePatternCoordinatesException("Some grid index range " + min + ".." + max
                + " has a size larger than " + MAX_COORDINATE + " and cannot be used for building a pattern");
        }
    }

    private AbstractUniformGridPattern simpleMinkowskiAdd(UniformGridPattern added) {
        if (!stepsOfGridEqual(added)) {
            throw new AssertionError("simpleMinkowskiAdd should be used with compatible grids only");
        }
        Set<IPoint> resultIndexes = new HashSet<>();
        Set<IPoint> indexes = gridIndexes();
        Set<IPoint> addedIndexes = added.gridIndexes();
        for (IPoint p : indexes) {
            for (IPoint q : addedIndexes) {
                resultIndexes.add(p.add(q));
            }
        }
        return new BasicDirectPointSetUniformGridPattern(
            originOfGrid.add(added.originOfGrid()),
            stepsOfGrid,
            resultIndexes);
    }

    private static Set<IPoint> gridBoundaryForDirection(Set<IPoint> points, IPoint direction) {
        Set<IPoint> result = new HashSet<>();
        IPoint directionSymmetric = direction.symmetric();
        for (IPoint ip : points) {
            boolean overflow = false;
            for (int k = 0, n = direction.coordCount(); k < n; k++) {
                long coord = ip.coord(k);
                if (coord >= Long.MAX_VALUE - 1 || coord <= Long.MIN_VALUE + 1) {
                    overflow = true;
                    break;
                }
            }
            if (overflow // to be on the safe side
                || !points.contains(ip.add(direction))
                || !points.contains(ip.add(directionSymmetric)))
            {
                result.add(ip);
            }
        }
        return result;
    }

    private static int[] intGridDimensions(UniformGridPattern pattern) {
        int[] result = new int[pattern.dimCount()];
        for (int k = 0; k < result.length; k++) {
            long size = pattern.gridIndexRange(k).size();
            if (size > Integer.MAX_VALUE) {
                return null;
            }
            result[k] = (int) size;
        }
        return result;
    }

    // points argument is updated (sorted), retainedPoints argument is appended
    // Returns the list of segments
    private static List<UniformGridPattern> joinPointsToSegments(
        List<IPoint> points, List<IPoint> retainedPoints,
        final int coordIndex, int minimalPointCount)
    {
        List<UniformGridPattern> result = new ArrayList<>();
        if (points.isEmpty()) { // to be on the safe side: never occurs for pattern's point set
            return result;
        }
        Collections.sort(points, new Comparator<>() {
            public int compare(IPoint o1, IPoint o2) {
                return o1.compareTo(o2, coordIndex);
            }
        });
        IPoint last = points.get(0);
        final int coordCount = last.coordCount();
        IPoint min = last;
        int minIndex = 0;
        final int n = points.size();
        for (int k = 1; k < n; k++) {
            IPoint p = points.get(k);
            boolean newSegment = false;
            for (int j = 0; j < coordCount; j++) {
                long lastCoord = last.coord(j);
                long nextCoord = p.coord(j);
                if (nextCoord != (j == coordIndex ? lastCoord + 1 : lastCoord)) {
                    newSegment = true;
                    break;
                }
            }
            if (newSegment) { // minIndex..k-1 is the previous segment
                int len = k - minIndex;
                if (len >= minimalPointCount) {
                    result.add(Patterns.newRectangularIntegerPattern(min, last));
                } else {
                    retainedPoints.addAll(points.subList(minIndex, k));
                }
                minIndex = k;
                min = p;
            }
            last = p;
        }
        int len = n - minIndex; // last possible segment
        if (len >= minimalPointCount) {
            result.add(Patterns.newRectangularIntegerPattern(min, last));
        } else {
            retainedPoints.addAll(points.subList(minIndex, n));
        }
        return result;
    }

    private static TinyBitMatrix increaseMatrixBy2LastCoordinates(
        TinyBitMatrix matrix,
        int newLastDim, int newLastButOneDim)
    {
        int[] dim = matrix.dimensions();
        if (dim.length < 2) {
            throw new AssertionError("This method cannot process 1-dimensional matrices");
        }
        if (newLastDim < dim[dim.length - 1]) {
            throw new AssertionError("This method cannot reduce the matrix");
        }
        if (newLastButOneDim < dim[dim.length - 2]) {
            throw new AssertionError("This method cannot reduce the matrix");
        }

        long mult = 1;
        for (int k = 0; k < dim.length - 2; k++) {
            mult *= dim[k];
        }
        long oldHyperplaneSize = dim[dim.length - 2] * mult; // x-dim for 2-dimensional case
        long newHyperplaneSize = newLastButOneDim * mult;
        int n = dim[dim.length - 1]; // y-dim for 2-dimensional case
        dim[dim.length - 2] = newLastButOneDim;
        dim[dim.length - 1] = newLastDim;
        long[] array = matrix.array();
        TinyBitMatrix result = matrix.reDim(dim);
        long oldDisp = n * oldHyperplaneSize;
        long newDisp = n * newHyperplaneSize;
        for (int k = n - 1; k >= 0; k--) {
            oldDisp -= oldHyperplaneSize;
            newDisp -= newHyperplaneSize;
            PackedBitArrays.copyBits(array, newDisp, array, oldDisp, oldHyperplaneSize);
            PackedBitArrays.fillBits(array,
                    newDisp + oldHyperplaneSize, newHyperplaneSize - oldHyperplaneSize, false);
        }
        return result;
    }
}
