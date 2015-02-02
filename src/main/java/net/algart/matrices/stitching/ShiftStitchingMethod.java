/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.List;

public final class ShiftStitchingMethod<P extends FramePosition> implements StitchingMethod<P> {
    private final double defaultValue;
    private final boolean weighted;

    private ShiftStitchingMethod(double defaultValue, boolean weighted) {
        this.defaultValue = defaultValue;
        this.weighted = weighted;
    }

    public static <P extends FramePosition> ShiftStitchingMethod<P> getNearestNeighbourInstance(
        double defaultValue)
    {
        return new ShiftStitchingMethod<P>(defaultValue, false);
    }

    public static <P extends FramePosition> ShiftStitchingMethod<P> getWeightedNeighboursInstance(
        double defaultValue)
    {
        return new ShiftStitchingMethod<P>(defaultValue, true);
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
        if (weighted) {
            return new ShiftWeighedStitchingFunc(frames);
        } else {
            return new ShiftNearestStitchingFunc(frames);
        }
    }

    @Override
    public String toString() {
        return "parallel shift stitching method";
    }

    private class ShiftNearestStitchingFunc implements StitchingFunc {
        private final RectangularArea[] areas;

        private ShiftNearestStitchingFunc(List<? extends Frame<P>> frames) {
            this.areas = new RectangularArea[frames.size()];
            int n = 0;
            for (Frame<P> frame : frames) {
                this.areas[n] = ShiftFramePosition.area(
                    frame.position().area().min(), frame.matrix().dimensions());
                // important: we should not use frame.position().area() here, because it can be too large
                n++;
            }
        }

        /*Repeat() \bget\b ==> get1D,,get2D,,get3D;;
                   double\[\]\s*coordinates ==> double x0,,
                                                double x0, double x1,,
                                                double x0, double x1, double x2;;
                   (parallelDistance\()coordinates ==> $1x0,,$1x0, x1,,$1x0, x1, x2
        */
        public double get(double[] coordinates, double[] values) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    double distance = areas[k].parallelDistance(coordinates);
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
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    nearest = v1;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0, double v1, double v2) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    nearest = v2;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0, double v1, double v2, double v3) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    nearest = v3;
                }
            }
            return nearest;
        }

        public double get(double[] coordinates, double v0, double v1, double v2, double v3, double v4) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(coordinates);
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
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(coordinates);
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
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(coordinates);
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
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(coordinates);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v6;
                }
            }
            if (v7 == v7) { // not NaN
                double distance = areas[7].parallelDistance(coordinates);
                if (distance <= minDistance) {
                    nearest = v7;
                }
            }
            return nearest;
        }
        /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
        public double get1D(double x0, double[] values) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    double distance = areas[k].parallelDistance(x0);
                    if (distance <= minDistance) {
                        minDistance = distance; nearest = values[k];
                    }
                }
            }
            return nearest;
        }

        public double get1D(double x0, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get1D(double x0, double v0, double v1) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v1;
                }
            }
            return nearest;
        }

        public double get1D(double x0, double v0, double v1, double v2) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v2;
                }
            }
            return nearest;
        }

        public double get1D(double x0, double v0, double v1, double v2, double v3) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v3;
                }
            }
            return nearest;
        }

        public double get1D(double x0, double v0, double v1, double v2, double v3, double v4) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v4;
                }
            }
            return nearest;
        }

        public double get1D(double x0,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v5;
                }
            }
            return nearest;
        }

        public double get1D(double x0,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v6;
                }
            }
            return nearest;
        }

        public double get1D(double x0,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(x0);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v6;
                }
            }
            if (v7 == v7) { // not NaN
                double distance = areas[7].parallelDistance(x0);
                if (distance <= minDistance) {
                    nearest = v7;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1, double[] values) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    double distance = areas[k].parallelDistance(x0, x1);
                    if (distance <= minDistance) {
                        minDistance = distance; nearest = values[k];
                    }
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get2D(double x0, double x1, double v0, double v1) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v1;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1, double v0, double v1, double v2) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v2;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v3;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3, double v4) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v4;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v5;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v6;
                }
            }
            return nearest;
        }

        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v6;
                }
            }
            if (v7 == v7) { // not NaN
                double distance = areas[7].parallelDistance(x0, x1);
                if (distance <= minDistance) {
                    nearest = v7;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2, double[] values) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    double distance = areas[k].parallelDistance(x0, x1, x2);
                    if (distance <= minDistance) {
                        minDistance = distance; nearest = values[k];
                    }
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get3D(double x0, double x1, double x2, double v0, double v1) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v1;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2, double v0, double v1, double v2) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v2;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2, double v0, double v1, double v2, double v3) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v3;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2, double v0, double v1, double v2, double v3, double v4) {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v4;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v5;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v6;
                }
            }
            return nearest;
        }

        public double get3D(double x0, double x1, double x2,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double nearest = defaultValue;
            double minDistance = Double.POSITIVE_INFINITY;
            if (v0 == v0) { // not NaN
                minDistance = areas[0].parallelDistance(x0, x1, x2);
                nearest = v0;
            }
            if (v1 == v1) { // not NaN
                double distance = areas[1].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v1;
                }
            }
            if (v2 == v2) { // not NaN
                double distance = areas[2].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v2;
                }
            }
            if (v3 == v3) { // not NaN
                double distance = areas[3].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v3;
                }
            }
            if (v4 == v4) { // not NaN
                double distance = areas[4].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v4;
                }
            }
            if (v5 == v5) { // not NaN
                double distance = areas[5].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v5;
                }
            }
            if (v6 == v6) { // not NaN
                double distance = areas[6].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    minDistance = distance; nearest = v6;
                }
            }
            if (v7 == v7) { // not NaN
                double distance = areas[7].parallelDistance(x0, x1, x2);
                if (distance <= minDistance) {
                    nearest = v7;
                }
            }
            return nearest;
        }
        /*Repeat.AutoGeneratedEnd*/

        public String toString() {
            return "\"nearest-frame\" simple stitching function";
        }
    }

    private class ShiftWeighedStitchingFunc implements StitchingFunc {
        private final RectangularArea[] areas;

        private ShiftWeighedStitchingFunc(List<? extends Frame<P>> frames) {
            this.areas = new RectangularArea[frames.size()];
            int n = 0;
            Point shiftMinus05 = null;
            for (Frame<P> frame : frames) {
                RectangularArea area = ShiftFramePosition.area(
                    frame.position().area().min(), frame.matrix().dimensions());
                // important: we should not use frame.position().area() here, because it can be too large
                if (shiftMinus05 == null) {
                    double[] shift = new double[area.coordCount()];
                    JArrays.fillDoubleArray(shift, -0.5);
                    shiftMinus05 = Point.valueOf(shift);
                }
                this.areas[n] = area.shift(shiftMinus05);
                n++;
            }
        }

        /*Repeat() \bget\b ==> get1D,,get2D,,get3D;;
                   double\[\]\s*coordinates ==> double x0,,
                                                double x0, double x1,,
                                                double x0, double x1, double x2;;
                   (parallelDistance\()coordinates ==> $1x0,,$1x0, x1,,$1x0, x1, x2
        */
        public double get(double[] coordinates, double[] values) {
            double[] distances = new double[values.length]; // zero-filled
            double b = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    distances[k] = areas[k].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0, d2 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(coordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(coordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(coordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(coordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(coordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(coordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(coordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(coordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(coordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(coordinates);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(coordinates);
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
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0, d7 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(coordinates);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(coordinates);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(coordinates);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(coordinates);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(coordinates);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(coordinates);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(coordinates);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (v7 == v7) { // not NaN
                d7 = areas[7].parallelDistance(coordinates);
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
        /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
        public double get1D(double x0, double[] values) {
            double[] distances = new double[values.length]; // zero-filled
            double b = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    distances[k] = areas[k].parallelDistance(x0);
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

        public double get1D(double x0, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get1D(double x0, double v0, double v1) {
            double d0 = 0.0, d1 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
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

        public double get1D(double x0, double v0, double v1, double v2) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0);
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

        public double get1D(double x0, double v0, double v1, double v2, double v3) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0);
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

        public double get1D(double x0, double v0, double v1, double v2, double v3, double v4) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0);
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

        public double get1D(double x0,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0);
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

        public double get1D(double x0,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(x0);
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

        public double get1D(double x0,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0, d7 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(x0);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (v7 == v7) { // not NaN
                d7 = areas[7].parallelDistance(x0);
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

        public double get2D(double x0, double x1, double[] values) {
            double[] distances = new double[values.length]; // zero-filled
            double b = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    distances[k] = areas[k].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get2D(double x0, double x1, double v0, double v1) {
            double d0 = 0.0, d1 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1, double v0, double v1, double v2) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1, double v0, double v1, double v2, double v3, double v4) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0, x1);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(x0, x1);
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

        public double get2D(double x0, double x1,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0, d7 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0, x1);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(x0, x1);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (v7 == v7) { // not NaN
                d7 = areas[7].parallelDistance(x0, x1);
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

        public double get3D(double x0, double x1, double x2, double[] values) {
            double[] distances = new double[values.length]; // zero-filled
            double b = 0.0;
            for (int k = 0; k < values.length; k++) {
                if (values[k] == values[k]) { // not NaN
                    distances[k] = areas[k].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2, double v0) {
            if (v0 == v0) { // not NaN
                return v0;
            }
            return defaultValue;
        }

        public double get3D(double x0, double x1, double x2, double v0, double v1) {
            double d0 = 0.0, d1 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2, double v0, double v1, double v2) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2, double v0, double v1, double v2, double v3) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1, x2);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2, double v0, double v1, double v2, double v3, double v4) {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1, x2);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1, x2);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2,
            double v0, double v1, double v2, double v3, double v4, double v5)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1, x2);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1, x2);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1, x2);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1, x2);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1, x2);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1, x2);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0, x1, x2);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(x0, x1, x2);
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

        public double get3D(double x0, double x1, double x2,
            double v0, double v1, double v2, double v3, double v4, double v5, double v6, double v7)
        {
            double d0 = 0.0, d1 = 0.0, d2 = 0.0, d3 = 0.0, d4 = 0.0, d5 = 0.0, d6 = 0.0, d7 = 0.0;
            double b = 0.0;
            if (v0 == v0) { // not NaN
                d0 = areas[0].parallelDistance(x0, x1, x2);
                if (d0 < 0.0) {
                    b -= d0;
                }
            }
            if (v1 == v1) { // not NaN
                d1 = areas[1].parallelDistance(x0, x1, x2);
                if (d1 < 0.0) {
                    b -= d1;
                }
            }
            if (v2 == v2) { // not NaN
                d2 = areas[2].parallelDistance(x0, x1, x2);
                if (d2 < 0.0) {
                    b -= d2;
                }
            }
            if (v3 == v3) { // not NaN
                d3 = areas[3].parallelDistance(x0, x1, x2);
                if (d3 < 0.0) {
                    b -= d3;
                }
            }
            if (v4 == v4) { // not NaN
                d4 = areas[4].parallelDistance(x0, x1, x2);
                if (d4 < 0.0) {
                    b -= d4;
                }
            }
            if (v5 == v5) { // not NaN
                d5 = areas[5].parallelDistance(x0, x1, x2);
                if (d5 < 0.0) {
                    b -= d5;
                }
            }
            if (v6 == v6) { // not NaN
                d6 = areas[6].parallelDistance(x0, x1, x2);
                if (d6 < 0.0) {
                    b -= d6;
                }
            }
            if (v7 == v7) { // not NaN
                d7 = areas[7].parallelDistance(x0, x1, x2);
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
        /*Repeat.AutoGeneratedEnd*/

        public String toString() {
            return "\"weighted-frames\" simple stitching function";
        }
    }
}
