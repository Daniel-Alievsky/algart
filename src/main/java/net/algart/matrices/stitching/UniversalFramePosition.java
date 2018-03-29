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

package net.algart.matrices.stitching;

import net.algart.arrays.JArrays;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.Point;
import net.algart.math.RectangularArea;
import net.algart.math.functions.CoordinateTransformationOperator;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearOperator;

public class UniversalFramePosition implements FramePosition {
    private final RectangularArea area;
    private final CoordinateTransformationOperator inverseTransform;
    private final boolean isShift;

    UniversalFramePosition(RectangularArea area, CoordinateTransformationOperator inverseTransform) {
        this.area = area;
        this.inverseTransform = inverseTransform;
        this.isShift = inverseTransform instanceof LinearOperator
            && ((LinearOperator)inverseTransform).isShift();
    }

    public static UniversalFramePosition valueOf(RectangularArea area,
        CoordinateTransformationOperator inverseTransform)
    {
        if (area == null)
            throw new NullPointerException("Null area argument");
        if (inverseTransform == null)
            throw new NullPointerException("Null inverseTransform argument");
        return new UniversalFramePosition(area, inverseTransform);
    }

    public static RectangularArea estimateDestinationAreaByVertices(long[] sourceMatrixDimensions,
        LinearOperator inverseTransform)
    {
        if (sourceMatrixDimensions == null)
            throw new NullPointerException("Null sourceMatrixDimensions argument");
        final int n = sourceMatrixDimensions.length;
        if (n == 0)
            throw new IllegalArgumentException("Empty sourceMatrixDimensions argument");
        if (n > 63)
            throw new IllegalArgumentException("Too large number of dimensions: " + n + " > 63");
        double[] minDestCoordinates = new double[n];
        double[] maxDestCoordinates = new double[n];
        JArrays.fillDoubleArray(minDestCoordinates, Double.POSITIVE_INFINITY);
        JArrays.fillDoubleArray(maxDestCoordinates, Double.NEGATIVE_INFINITY);
        double[] srcCoordinates = new double[n];
        double[] destCoordinates = new double[n];
        MainLoop:
        for (int bits = 0, maxBits = (1 << n) - 1; bits <= maxBits; bits++) { // maxBits can be Long.MAX_VALUE here
            for (int k = 0; k < n; k++) {
                srcCoordinates[k] = ((bits >>> k) & 1) == 0 ? 0.0 : sourceMatrixDimensions[k];
            }
            inverseTransform.inverseMap(destCoordinates, srcCoordinates);
            for (double v : destCoordinates) {
                if (Double.isNaN(v)) {
                    JArrays.fillDoubleArray(minDestCoordinates, Double.NEGATIVE_INFINITY);
                    JArrays.fillDoubleArray(maxDestCoordinates, Double.POSITIVE_INFINITY);
                    break MainLoop; // return all the space
                }
            }
            JArrays.minDoubleArray(minDestCoordinates, 0, destCoordinates, 0, n);
            JArrays.maxDoubleArray(maxDestCoordinates, 0, destCoordinates, 0, n);
        }
        return RectangularArea.valueOf(Point.valueOf(minDestCoordinates), Point.valueOf(maxDestCoordinates));
    }

    public RectangularArea area() {
        return area;
    }

    public Func asInterpolationFunc(Matrix<? extends PArray> sourceMatrix) {
        Point o = area.min();
        boolean integerShift = isShift && o.equals(o.toRoundedPoint().toPoint());
        Func f = Matrices.asInterpolationFunc(sourceMatrix,
            integerShift ?
                Matrices.InterpolationMethod.STEP_FUNCTION :
                Matrices.InterpolationMethod.POLYLINEAR_FUNCTION,
            Double.NaN);
        return inverseTransform.apply(f);
    }

    public CoordinateTransformationOperator inverseTransform() {
        return inverseTransform;
    }

    @Override
    public String toString() {
        return "universal frame position " + area + " with " + inverseTransform;
    }

    public int hashCode() {
        return area.hashCode() * 37 + inverseTransform.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof UniversalFramePosition))
            return false;
        UniversalFramePosition ufp = (UniversalFramePosition)obj;
        return area.equals(ufp.area) && inverseTransform.equals(ufp.inverseTransform);
    }

}
