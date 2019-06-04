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

import net.algart.math.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>A skeletal implementation of the {@link WeightedPattern} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>This implementation is based on using some "parent" pattern, implementing {@link Pattern} interface
 * and passed to the constructor.
 * All methods of this class, excepting declared in the {@link WeightedPattern} interface,
 * just call the same methods of the parent pattern.
 * To complete implementation, you just need to implement several methods from
 * the {@link WeightedPattern} interface.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public abstract class AbstractWeightedPattern implements WeightedPattern {
    /**
     * The parent pattern.
     */
    protected final Pattern parent;

    /**
     * Creates a new weighted pattern on the base of the given parent one.
     *
     * @param parent the parent pattern, serving most of all methods of this instance.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    protected AbstractWeightedPattern(Pattern parent) {
        if (parent == null)
            throw new NullPointerException("Null parent pattern");
        this.parent = parent;
    }

    public int dimCount() {
        return parent.dimCount();
    }

    public long pointCount() {
        return parent.pointCount();
    }

    public double largePointCount() {
        return parent.largePointCount();
    }

//    public boolean contains(IPoint point) {
//        return parent.contains(point);
//    }

    public Set<Point> points() {
        return parent.points();
    }

    public Set<IPoint> roundedPoints() {
        return parent.roundedPoints();
    }

    public Range coordRange(int coordIndex) {
        return parent.coordRange(coordIndex);
    }

    public RectangularArea coordArea() {
        return parent.coordArea();
    }

    public Point coordMin() {
        return parent.coordMin();
    }

    public Point coordMax() {
        return parent.coordMax();
    }

    public IRange roundedCoordRange(int coordIndex) {
        return parent.roundedCoordRange(coordIndex);
    }

    public IRectangularArea roundedCoordArea() {
        return parent.roundedCoordArea();
    }

    public boolean isSurelySinglePoint() {
        return parent.isSurelySinglePoint();
    }

    public boolean isSurelyOriginPoint() {
        return parent.isSurelyOriginPoint();
    }

    public Pattern projectionAlongAxis(int coordIndex) {
        return parent.projectionAlongAxis(coordIndex);
    }

    public boolean isSurelyInteger() {
        return parent.isSurelyInteger();
    }

    public UniformGridPattern round() {
        return parent.round();
    }

    public Pattern minBound(int coordIndex) {
        return parent.minBound(coordIndex);
    }

    public Pattern maxBound(int coordIndex) {
        return parent.maxBound(coordIndex);
    }

    public Pattern carcass() {
        return parent.carcass();
    }

    public int maxCarcassMultiplier() {
        return parent.maxCarcassMultiplier();
    }

    public Pattern minkowskiAdd(Pattern added) {
        return parent.minkowskiAdd(added);
    }

    public Pattern minkowskiSubtract(Pattern subtracted) {
        return parent.minkowskiSubtract(subtracted);
    }

    public List<Pattern> minkowskiDecomposition(int minimalPointCount) {
        return parent.minkowskiDecomposition(minimalPointCount);
    }

    public boolean hasMinkowskiDecomposition() {
        return parent.hasMinkowskiDecomposition();
    }

    public List<Pattern> unionDecomposition(int minimalPointCount) {
        return parent.unionDecomposition(minimalPointCount);
    }

    public List<List<Pattern>> allUnionDecompositions(int minimalPointCount) {
        return parent.allUnionDecompositions(minimalPointCount);
    }

    public WeightedPattern shift(Point shift) {
        return shift(shift.toRoundedPoint());
    }

    public abstract WeightedPattern shift(IPoint shift);

    public WeightedPattern multiply(double multiplier) {
        double[] multipliers = new double[dimCount()];
        Arrays.fill(multipliers, multiplier);
        return scale(multipliers);
    }

    public abstract WeightedPattern scale(double... multipliers);

    /**
     * This implementation calls {@link #multiply(double) multiply(-1.0)}.
     * There are no reasons to override this method usually.
     *
     * @return the symmetric pattern.
     */
    public WeightedPattern symmetric() {
        return multiply(-1.0);
    }

    public abstract double weight(IPoint point);

    public abstract Range weightRange();

    /**
     * This implementation returns <tt>{@link #weightRange()}.{@link Range#size() size()}==0.0</tt>.
     * There are no reasons to override this method usually.
     *
     * @return <tt>true</tt> if the weights of all points are the same.
     */
    public boolean isConstant() {
        return weightRange().size() == 0.0;
    }

    /**
     * This implementation returns <tt>Collections.singletonList(this)</tt>.
     * Please override this method if there is better implementation.
     *
     * @param minimalPointCount this method does not try to decompose patterns that contain
     *                          less than <tt>minimalPointCount</tt> points.
     * @return the decomposition of this pattern to the "product" (convolution) of smaller patterns.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public List<WeightedPattern> productDecomposition(int minimalPointCount) {
        if (minimalPointCount < 0)
            throw new IllegalArgumentException("Negative minimalPointCount");
        return Collections.<WeightedPattern>singletonList(this);
    }
}
