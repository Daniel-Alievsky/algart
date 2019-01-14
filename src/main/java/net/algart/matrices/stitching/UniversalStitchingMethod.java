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

package net.algart.matrices.stitching;

import net.algart.arrays.JArrays;
import net.algart.math.Point;
import net.algart.math.RectangularArea;
import net.algart.math.functions.CoordinateTransformationOperator;
import net.algart.math.functions.LinearOperator;

import java.util.List;

public final class UniversalStitchingMethod<P extends UniversalFramePosition> implements StitchingMethod<P> {
    private final double defaultValue;
    private final boolean weighted;

    private UniversalStitchingMethod(double defaultValue, boolean weighted) {
        this.defaultValue = defaultValue;
        this.weighted = weighted;
    }

    public static <P extends UniversalFramePosition> UniversalStitchingMethod<P> getNearestNeighbourInstance(
        double defaultValue)
    {
        return new UniversalStitchingMethod<P>(defaultValue, false);
    }

    public static <P extends UniversalFramePosition> UniversalStitchingMethod<P> getWeightedNeighboursInstance(
        double defaultValue)
    {
        return new UniversalStitchingMethod<P>(defaultValue, true);
    }

    public boolean simpleBehaviorForEmptySpace() {
        return true;
    }

    public boolean simpleBehaviorForSingleFrame() {
        return true;
    }

    public StitchingFunc getStitchingFunc(List<? extends Frame<P>> frames) {
        if (frames == null)
            throw new NullPointerException("Null frames argument");
        boolean affine2D = true;
        for (Frame<P> frame : frames) {
            CoordinateTransformationOperator o = frame.position().inverseTransform();
            if (!(o instanceof LinearOperator && ((LinearOperator)o).n() == 2)) {
                affine2D = false; break;
            }
        }
        if (affine2D) {
            if (weighted) {
                return new Affine2DWeighedStitchingFunc(frames);
            } else {
                return new Affine2DNearestStitchingFunc(frames);
            }
        } else {
            if (weighted) {
                return new UniversalWeighedStitchingFunc(frames);
            } else {
                return new UniversalNearestStitchingFunc(frames);
            }
        }
    }

    @Override
    public String toString() {
        return "universal stitching method";
    }

    private strictfp class UniversalNearestStitchingFunc extends AbstractStitchingFunc implements StitchingFunc {
        final RectangularArea[] areasInMatrices;
        final CoordinateTransformationOperator[] inverseTransforms;

        private UniversalNearestStitchingFunc(List<? extends Frame<P>> frames) {
            this.areasInMatrices = new RectangularArea[frames.size()];
            this.inverseTransforms = new CoordinateTransformationOperator[frames.size()];
            int n = 0;
            for (Frame<P> frame : frames) {
                UniversalFramePosition p = frame.position();
                this.areasInMatrices[n] = ShiftFramePosition.area(
                    Point.origin(frame.dimCount()), frame.matrix().dimensions());
                this.inverseTransforms[n] = p.inverseTransform();
                n++;
            }
        }

        public double get(double[] coordinates, double[] values) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    if (srcCoordinates == null) {
                        srcCoordinates = new double[coordinates.length];
                    }
                    inverseTransforms[k].map(srcCoordinates, coordinates);
                    double distance = areasInMatrices[k].parallelDistance(srcCoordinates);
                    if (distance <= minDistance) {
                        minDistance = distance; nearest = values[k];
                    }
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get(double[] coordinates, double v0, double v1) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v1;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0, double v1, double v2) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v2;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0, double v1, double v2, double v3) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v3;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0, double v1, double v2, double v3, double v4) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v4;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[5].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[5].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v5;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[5].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[5].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[6].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[6].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v6;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                minDistance = areasInMatrices[0].parallelDistance(srcCoordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[5].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[5].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[6].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[6].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v6;
                }
            }
            if (v7 == v7) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[7].map(srcCoordinates, coordinates);
                double distance = areasInMatrices[7].parallelDistance(srcCoordinates);
                if (distance <= minDistance) {
                    nearest = v7;
                }
            }
            return nearest;
        }

        public String toString() {
            return "\"nearest-frame-in-translated-coordinates\" stitching function";
        }
    }

    private strictfp class Affine2DNearestStitchingFunc extends UniversalNearestStitchingFunc {
        private final double[][] a, b;
        private final double[] a0, a1, a2, a3, a4, a5, a6, a7, b0, b1, b2, b3, b4, b5, b6, b7;

        private Affine2DNearestStitchingFunc(List<? extends Frame<P>> frames) {
            super(frames);
            this.a = new double[this.inverseTransforms.length][];
            this.b = new double[this.inverseTransforms.length][];
            for (int k = 0; k < this.inverseTransforms.length; k++) {
                LinearOperator lo = (LinearOperator)this.inverseTransforms[k];
                assert lo.n() == 2;
                this.a[k] = lo.a();
                this.b[k] = lo.b();
            }
            this.a0 = this.inverseTransforms.length >= 1 ? this.a[0] : null;
            this.a1 = this.inverseTransforms.length >= 2 ? this.a[1] : null;
            this.a2 = this.inverseTransforms.length >= 3 ? this.a[2] : null;
            this.a3 = this.inverseTransforms.length >= 4 ? this.a[3] : null;
            this.a4 = this.inverseTransforms.length >= 5 ? this.a[4] : null;
            this.a5 = this.inverseTransforms.length >= 6 ? this.a[5] : null;
            this.a6 = this.inverseTransforms.length >= 7 ? this.a[6] : null;
            this.a7 = this.inverseTransforms.length >= 8 ? this.a[7] : null;
            this.b0 = this.inverseTransforms.length >= 1 ? this.b[0] : null;
            this.b1 = this.inverseTransforms.length >= 2 ? this.b[1] : null;
            this.b2 = this.inverseTransforms.length >= 3 ? this.b[2] : null;
            this.b3 = this.inverseTransforms.length >= 4 ? this.b[3] : null;
            this.b4 = this.inverseTransforms.length >= 5 ? this.b[4] : null;
            this.b5 = this.inverseTransforms.length >= 6 ? this.b[5] : null;
            this.b6 = this.inverseTransforms.length >= 7 ? this.b[6] : null;
            this.b7 = this.inverseTransforms.length >= 8 ? this.b[7] : null;
        }

        @Override
        public double get2D(double x0, double x1, double[] values) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    double y0 = a[k][0] * x0 + a[k][1] * x1 + b[k][0];
                    double y1 = a[k][2] * x0 + a[k][3] * x1 + b[k][1];
                    double distance = areasInMatrices[k].parallelDistance(y0, y1);
                    if (distance <= minDistance) {
                        minDistance = distance; nearest = values[k];
                    }
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v1;
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1, double v2) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                double distance = areasInMatrices[2].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v2;
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                double distance = areasInMatrices[2].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                double distance = areasInMatrices[3].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v3;
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3, double v4) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                double distance = areasInMatrices[2].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                double distance = areasInMatrices[3].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                double distance = areasInMatrices[4].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v4;
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                double distance = areasInMatrices[2].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                double distance = areasInMatrices[3].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                double distance = areasInMatrices[4].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double y0 = a5[0] * x0 + a5[1] * x1 + b5[0];
                double y1 = a5[2] * x0 + a5[3] * x1 + b5[1];
                double distance = areasInMatrices[5].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v5;
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                double distance = areasInMatrices[2].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                double distance = areasInMatrices[3].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                double distance = areasInMatrices[4].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double y0 = a5[0] * x0 + a5[1] * x1 + b5[0];
                double y1 = a5[2] * x0 + a5[3] * x1 + b5[1];
                double distance = areasInMatrices[5].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double y0 = a6[0] * x0 + a6[1] * x1 + b6[0];
                double y1 = a6[2] * x0 + a6[3] * x1 + b6[1];
                double distance = areasInMatrices[6].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v6;
                }
            }
            return nearest;
        }

        @Override
        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            double[] srcCoordinates = null;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                minDistance = areasInMatrices[0].parallelDistance(y0, y1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                double distance = areasInMatrices[1].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                double distance = areasInMatrices[2].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                double distance = areasInMatrices[3].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                double distance = areasInMatrices[4].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double y0 = a5[0] * x0 + a5[1] * x1 + b5[0];
                double y1 = a5[2] * x0 + a5[3] * x1 + b5[1];
                double distance = areasInMatrices[5].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double y0 = a6[0] * x0 + a6[1] * x1 + b6[0];
                double y1 = a6[2] * x0 + a6[3] * x1 + b6[1];
                double distance = areasInMatrices[6].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v6;
                }
            }
            if (v7 == v7) { // not NaN
                double y0 = a7[0] * x0 + a7[1] * x1 + b7[0];
                double y1 = a7[2] * x0 + a7[3] * x1 + b7[1];
                double distance = areasInMatrices[6].parallelDistance(y0, y1);
                if (distance <= minDistance) {
                    nearest = v7;
                }
            }
            return nearest;
        }

        public String toString() {
            return "affine 2D \"nearest-frame-in-translated-coordinates\" stitching function";
        }
    }

    private strictfp class UniversalWeighedStitchingFunc extends AbstractStitchingFunc implements StitchingFunc {
        final RectangularArea[] areasInMatrices;
        final CoordinateTransformationOperator[] inverseTransforms;

        private UniversalWeighedStitchingFunc(List<? extends Frame<P>> frames) {
            this.areasInMatrices = new RectangularArea[frames.size()];
            this.inverseTransforms = new CoordinateTransformationOperator[frames.size()];
            int n = 0;
            Point shiftMinus05 = null;
            for (Frame<P> frame : frames) {
                UniversalFramePosition p = frame.position();
                RectangularArea area = ShiftFramePosition.area(
                    Point.origin(frame.dimCount()), frame.matrix().dimensions());
                if (shiftMinus05 == null) {
                    double[] shift = new double[area.coordCount()];
                    JArrays.fillDoubleArray(shift, -0.5);
                    shiftMinus05 = Point.valueOf(shift);
                }
                this.areasInMatrices[n] = area.shift(shiftMinus05);
                this.inverseTransforms[n] = p.inverseTransform();
                n++;
            }
        }

        public double get(double[] coordinates, double[] values) {
            double[] srcCoordinates = null;
            double[] distances = new double[values.length]; // zero-filled
            double b = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    if (srcCoordinates == null) {
                        srcCoordinates = new double[coordinates.length];
                    }
                    inverseTransforms[k].map(srcCoordinates, coordinates);
                    distances[k] = areasInMatrices[k].parallelDistance(srcCoordinates);
                    if (distances[k] < 0.0) {
                        b -= distances[k];
                    }
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (distances[k] < 0.0) {
                    a -= distances[k] * values[k];
                }
            }
            return a / b;
        }

        public double get(double[] coordinates, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get(double[] coordinates, double v0, double v1) {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            return a / b;
        }

        public double get(double[] coordinates, double v0, double v1, double v2) {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                d2 = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            return a / b;
        }

        public double get(double[] coordinates, double v0, double v1, double v2, double v3) {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                d2 = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                d3 = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            return a / b;
        }

        public double get(double[] coordinates, double v0, double v1, double v2, double v3, double v4) {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                d2 = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                d3 = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                d4 = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            return a / b;
        }

        public double get(double[] coordinates,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                d2 = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                d3 = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                d4 = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[5].map(srcCoordinates, coordinates);
                d5 = areasInMatrices[5].parallelDistance(srcCoordinates);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            if (d5 < 0.0) {
                a -= d5 * v5;
            }
            return a / b;
        }

        public double get(double[] coordinates,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                d2 = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                d3 = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                d4 = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[5].map(srcCoordinates, coordinates);
                d5 = areasInMatrices[5].parallelDistance(srcCoordinates);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[6].map(srcCoordinates, coordinates);
                d6 = areasInMatrices[6].parallelDistance(srcCoordinates);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            if (d5 < 0.0) {
                a -= d5 * v5;
            }
            if (d6 < 0.0) {
                a -= d6 * v6;
            }
            return a / b;
        }

        public double get(double[] coordinates,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0, d7 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                srcCoordinates = new double[coordinates.length];
                inverseTransforms[0].map(srcCoordinates, coordinates);
                d0 = areasInMatrices[0].parallelDistance(srcCoordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[1].map(srcCoordinates, coordinates);
                d1 = areasInMatrices[1].parallelDistance(srcCoordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[2].map(srcCoordinates, coordinates);
                d2 = areasInMatrices[2].parallelDistance(srcCoordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[3].map(srcCoordinates, coordinates);
                d3 = areasInMatrices[3].parallelDistance(srcCoordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[4].map(srcCoordinates, coordinates);
                d4 = areasInMatrices[4].parallelDistance(srcCoordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[5].map(srcCoordinates, coordinates);
                d5 = areasInMatrices[5].parallelDistance(srcCoordinates);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[6].map(srcCoordinates, coordinates);
                d6 = areasInMatrices[6].parallelDistance(srcCoordinates);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (v7 == v7) { // not NaN
                if (srcCoordinates == null) {
                    srcCoordinates = new double[coordinates.length];
                }
                inverseTransforms[7].map(srcCoordinates, coordinates);
                d7 = areasInMatrices[7].parallelDistance(srcCoordinates);
                if (d7 < 0.0) {
                    b -= d7;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            if (d5 < 0.0) {
                a -= d5 * v5;
            }
            if (d6 < 0.0) {
                a -= d6 * v6;
            }
            if (d7 < 0.0) {
                a -= d7 * v7;
            }
            return a / b;
        }

        public String toString() {
            return "\"weighted-frames-in-translated-coordinates\" stitching function";
        }
    }

    private strictfp class Affine2DWeighedStitchingFunc extends UniversalWeighedStitchingFunc {
        private final double[][] a, b;
        private final double[] a0, a1, a2, a3, a4, a5, a6, a7, b0, b1, b2, b3, b4, b5, b6, b7;

        private Affine2DWeighedStitchingFunc(List<? extends Frame<P>> frames) {
            super(frames);
            this.a = new double[this.inverseTransforms.length][];
            this.b = new double[this.inverseTransforms.length][];
            for (int k = 0; k < this.inverseTransforms.length; k++) {
                LinearOperator lo = (LinearOperator)this.inverseTransforms[k];
                assert lo.n() == 2;
                this.a[k] = lo.a();
                this.b[k] = lo.b();
            }
            this.a0 = this.inverseTransforms.length >= 1 ? this.a[0] : null;
            this.a1 = this.inverseTransforms.length >= 2 ? this.a[1] : null;
            this.a2 = this.inverseTransforms.length >= 3 ? this.a[2] : null;
            this.a3 = this.inverseTransforms.length >= 4 ? this.a[3] : null;
            this.a4 = this.inverseTransforms.length >= 5 ? this.a[4] : null;
            this.a5 = this.inverseTransforms.length >= 6 ? this.a[5] : null;
            this.a6 = this.inverseTransforms.length >= 7 ? this.a[6] : null;
            this.a7 = this.inverseTransforms.length >= 8 ? this.a[7] : null;
            this.b0 = this.inverseTransforms.length >= 1 ? this.b[0] : null;
            this.b1 = this.inverseTransforms.length >= 2 ? this.b[1] : null;
            this.b2 = this.inverseTransforms.length >= 3 ? this.b[2] : null;
            this.b3 = this.inverseTransforms.length >= 4 ? this.b[3] : null;
            this.b4 = this.inverseTransforms.length >= 5 ? this.b[4] : null;
            this.b5 = this.inverseTransforms.length >= 6 ? this.b[5] : null;
            this.b6 = this.inverseTransforms.length >= 7 ? this.b[6] : null;
            this.b7 = this.inverseTransforms.length >= 8 ? this.b[7] : null;
        }

        @Override
        public double get2D(double x0, double x1, double[] values) {
            double[] distances = new double[values.length]; // zero-filled
            double b = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    double y0 = this.a[k][0] * x0 + this.a[k][1] * x1 + this.b[k][0];
                    double y1 = this.a[k][2] * x0 + this.a[k][3] * x1 + this.b[k][1];
                    distances[k] = areasInMatrices[k].parallelDistance(y0, y1);
                    if (distances[k] < 0.0) {
                        b -= distances[k];
                    }
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (distances[k] < 0.0) {
                    a -= distances[k] * values[k];
                }
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1) {
            double d0 = 0.0, d1 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1, double v2) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                d2 = areasInMatrices[2].parallelDistance(y0, y1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                d2 = areasInMatrices[2].parallelDistance(y0, y1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                d3 = areasInMatrices[3].parallelDistance(y0, y1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3, double v4) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                d2 = areasInMatrices[2].parallelDistance(y0, y1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                d3 = areasInMatrices[3].parallelDistance(y0, y1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                d4 = areasInMatrices[4].parallelDistance(y0, y1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double[] srcCoordinates = null;
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                d2 = areasInMatrices[2].parallelDistance(y0, y1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                d3 = areasInMatrices[3].parallelDistance(y0, y1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                d4 = areasInMatrices[4].parallelDistance(y0, y1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                double y0 = a5[0] * x0 + a5[1] * x1 + b5[0];
                double y1 = a5[2] * x0 + a5[3] * x1 + b5[1];
                d5 = areasInMatrices[5].parallelDistance(y0, y1);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            if (d5 < 0.0) {
                a -= d5 * v5;
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                d2 = areasInMatrices[2].parallelDistance(y0, y1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                d3 = areasInMatrices[3].parallelDistance(y0, y1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                d4 = areasInMatrices[4].parallelDistance(y0, y1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                double y0 = a5[0] * x0 + a5[1] * x1 + b5[0];
                double y1 = a5[2] * x0 + a5[3] * x1 + b5[1];
                d5 = areasInMatrices[5].parallelDistance(y0, y1);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                double y0 = a6[0] * x0 + a6[1] * x1 + b6[0];
                double y1 = a6[2] * x0 + a6[3] * x1 + b6[1];
                d6 = areasInMatrices[6].parallelDistance(y0, y1);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            if (d5 < 0.0) {
                a -= d5 * v5;
            }
            if (d6 < 0.0) {
                a -= d6 * v6;
            }
            return a / b;
        }

        @Override
        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0, d7 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                double y0 = a0[0] * x0 + a0[1] * x1 + b0[0];
                double y1 = a0[2] * x0 + a0[3] * x1 + b0[1];
                d0 = areasInMatrices[0].parallelDistance(y0, y1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                double y0 = a1[0] * x0 + a1[1] * x1 + b1[0];
                double y1 = a1[2] * x0 + a1[3] * x1 + b1[1];
                d1 = areasInMatrices[1].parallelDistance(y0, y1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                double y0 = a2[0] * x0 + a2[1] * x1 + b2[0];
                double y1 = a2[2] * x0 + a2[3] * x1 + b2[1];
                d2 = areasInMatrices[2].parallelDistance(y0, y1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                double y0 = a3[0] * x0 + a3[1] * x1 + b3[0];
                double y1 = a3[2] * x0 + a3[3] * x1 + b3[1];
                d3 = areasInMatrices[3].parallelDistance(y0, y1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                double y0 = a4[0] * x0 + a4[1] * x1 + b4[0];
                double y1 = a4[2] * x0 + a4[3] * x1 + b4[1];
                d4 = areasInMatrices[4].parallelDistance(y0, y1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                double y0 = a5[0] * x0 + a5[1] * x1 + b5[0];
                double y1 = a5[2] * x0 + a5[3] * x1 + b5[1];
                d5 = areasInMatrices[5].parallelDistance(y0, y1);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                double y0 = a6[0] * x0 + a6[1] * x1 + b6[0];
                double y1 = a6[2] * x0 + a6[3] * x1 + b6[1];
                d6 = areasInMatrices[6].parallelDistance(y0, y1);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (v7 == v7) { // not NaN
                double y0 = a7[0] * x0 + a7[1] * x1 + b7[0];
                double y1 = a7[2] * x0 + a7[3] * x1 + b7[1];
                d7 = areasInMatrices[7].parallelDistance(y0, y1);
                if (d7 < 0.0) {
                    b -= d7;
                }
            }
            if (b == 0.0) {
                return defaultValue;
            }
            double a = 0.0;
            if (d0 < 0.0) {
                a -= d0 * v0;
            }
            if (d1 < 0.0) {
                a -= d1 * v1;
            }
            if (d2 < 0.0) {
                a -= d2 * v2;
            }
            if (d3 < 0.0) {
                a -= d3 * v3;
            }
            if (d4 < 0.0) {
                a -= d4 * v4;
            }
            if (d5 < 0.0) {
                a -= d5 * v5;
            }
            if (d6 < 0.0) {
                a -= d6 * v6;
            }
            if (d7 < 0.0) {
                a -= d7 * v7;
            }
            return a / b;
        }

        public String toString() {
            return "affine 2D \"weighted-frames-in-translated-coordinates\" stitching function";
        }
    }
}
