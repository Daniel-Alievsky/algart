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

package net.algart.matrices.scanning;

import net.algart.arrays.*;

public abstract class Boundary2DProjectionMeasurer extends Boundary2DWrapper {
    final ContourLineType contourLineType;
    final double startAngleInRadians;
    final int m;

    Boundary2DProjectionMeasurer(Boundary2DScanner parent,
        ContourLineType contourLineType, double startAngleInRadians, int numberOfDirections)
    {
        super(parent);
        if (contourLineType == null)
            throw new NullPointerException("Null contourLineType");
        if (numberOfDirections <= 0)
            throw new IllegalArgumentException("Negative or zero number of directions");
        this.contourLineType = contourLineType;
        this.startAngleInRadians = startAngleInRadians;
        this.m = numberOfDirections;
    }

    public static Boundary2DProjectionMeasurer getInstance(Boundary2DScanner parent,
        ContourLineType contourLineType, double startAngleInRadians, int numberOfDirections)
    {
        if (parent == null)
            throw new NullPointerException("Null parent argument");
        return new DoubleVersion(parent, contourLineType, startAngleInRadians, numberOfDirections);
    }

    /**
     * Returns the contour line style, used for measuring by this instance.
     * The returned reference is identical to the corresponding argument of
     * {@link #getInstance(Boundary2DScanner, ContourLineType, double, int)} method,
     * used for creating this instance.
     *
     * @return the contour line style, used for measuring by this instance.
     */
    public ContourLineType contourLineType() {
        return contourLineType;
    }

    public double startAngleInRadians() {
        return startAngleInRadians;
    }

    public int numberOfDirections() {
        return this.m;
    }

    public double directionAngleInRadians(int directionIndex) {
        return startAngleInRadians + StrictMath.PI * (double) directionIndex / (double) m;
    }

    /**
     * Returns the oriented area inside the contour, following along the scanned boundary.
     * "Oriented" means that the result is equal to the area of the figure inside this contour,
     * Equivalent to <tt>{@link #area(ContourLineType) area}(thisObject.{@link #contourLineType()
     * contourLineType()})</tt>.
     *
     * @return the oriented area inside the scanned contour.
     */
    public double area() {
        return area(contourLineType);
    }

    /**
     * Returns the total length of the contour, following along the scanned boundary:
     * an estimated perimeter of the measured object, "drawn" at the bit matrix.
     * Equivalent to <tt>{@link #perimeter(ContourLineType) perimeter}(thisObject.{@link #contourLineType()
     * contourLineType()})</tt>.
     *
     * @return the length of the contour line, following along the scanned boundary.
     */
    public double perimeter() {
        return perimeter(contourLineType);
    }

    public abstract double projectionMin(int directionIndex);

    public abstract double projectionMax(int directionIndex);

    public abstract double projectionLength(int directionIndex);

    public double meanProjectionLength() {
        double sum = 0.0;
        for (int k = 0; k < m; k++) {
            sum += projectionLength(k);
        }
        return sum / (double) m;
    }

    /*Repeat() int\s+indexOfM ==> double m;;
               (return\s+)(index|-1) ==> $1result;;
               [ \t]*[^\n\r]*(index).*?(?:\r(?!\n)|\n|\r\n) ==>
    */
    public int indexOfMinProjectionLength() {
        double result = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int k = 0; k < m; k++) {
            double length = projectionLength(k);
            if (length < result) {
                index = k;
                result = length;
            }
        }
        return index;
    }

    public int indexOfMaxProjectionLength() {
        double result = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int k = 0; k < m; k++) {
            double length = projectionLength(k);
            if (length > result) {
                index = k;
                result = length;
            }
        }
        return index;
    }

    public int indexOfMinCircumscribedRhombus(int stepBetweenDirections) {
        if (stepBetweenDirections < 0)
            throw new IllegalArgumentException("Negative stepBetweenDirections");
        double result = Double.POSITIVE_INFINITY;
        stepBetweenDirections %= m;
        int index = -1;
        for (int k = 0, i = stepBetweenDirections; k < m; k++, i++) {
            if (i == m) {
                i = 0;
            }
            double length = projectionLength(k);
            double otherLength = projectionLength(i);
            if (length < otherLength) {
                length = otherLength;
            } // length = maximum from 2 projections
            if (length < result) {
                index = k;
                result = length;
            }
        }
        return index;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public double minProjectionLength() {
        double result = Double.POSITIVE_INFINITY;
        for (int k = 0; k < m; k++) {
            double length = projectionLength(k);
            if (length < result) {
                result = length;
            }
        }
        return result;
    }

    public double maxProjectionLength() {
        double result = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < m; k++) {
            double length = projectionLength(k);
            if (length > result) {
                result = length;
            }
        }
        return result;
    }

    public double minCircumscribedRhombus(int stepBetweenDirections) {
        if (stepBetweenDirections < 0)
            throw new IllegalArgumentException("Negative stepBetweenDirections");
        double result = Double.POSITIVE_INFINITY;
        stepBetweenDirections %= m;
        for (int k = 0, i = stepBetweenDirections; k < m; k++, i++) {
            if (i == m) {
                i = 0;
            }
            double length = projectionLength(k);
            double otherLength = projectionLength(i);
            if (length < otherLength) {
                length = otherLength;
            } // length = maximum from 2 projections
            if (length < result) {
                result = length;
            }
        }
        return result;
    }

    /*Repeat.AutoGeneratedEnd*/

    public String toString() {
        return m + "-projection measurer of " + parent;
    }

    strictfp static class DoubleVersion extends Boundary2DProjectionMeasurer {
        private final double[] projectionMin;
        private final double[] projectionMax;
        private final double[] dirX;
        private final double[] dirY;

        DoubleVersion(Boundary2DScanner parent,
            ContourLineType contourLineType, double startAngleInRadians, int numberOfDirections)
        {
            super(parent, contourLineType, startAngleInRadians, numberOfDirections);
            this.projectionMin = new double[m];
            this.projectionMax = new double[m];
            this.dirX = new double[m];
            this.dirY = new double[m];
            for (int k = 0; k < m; k++) {
                this.dirX[k] = StrictMath.cos(startAngleInRadians + StrictMath.PI * (double) k / (double) m);
                this.dirY[k] = StrictMath.sin(startAngleInRadians + StrictMath.PI * (double) k / (double) m);
            }
        }

        @Override
        public double projectionMin(int directionIndex) {
            return projectionMin[directionIndex];
        }

        @Override
        public double projectionMax(int directionIndex) {
            return projectionMax[directionIndex];
        }

        @Override
        public double projectionLength(int directionIndex) {
            return projectionMax[directionIndex] - projectionMin[directionIndex];
        }

        @Override
        public void next() {
            parent.next();
            double x = contourLineType.x(parent);
            double y = contourLineType.y(parent);
            for (int k = 0; k < m; k++) {
                double projection = x * dirX[k] + y * dirY[k];
                if (projection < projectionMin[k]) {
                    projectionMin[k] = projection;
                }
                if (projection > projectionMax[k]) {
                    projectionMax[k] = projection;
                }
            }
        }

        @Override
        public void resetCounters() {
            super.resetCounters();
            JArrays.fillDoubleArray(this.projectionMin, Double.POSITIVE_INFINITY);
            JArrays.fillDoubleArray(this.projectionMax, Double.NEGATIVE_INFINITY);
        }
    }

    /* Below is an example of anti-optimization for PIXEL_CENTERS_POLYLINE:
    strictfp static class IntVersion extends Boundary2DProjectionMeasurer {
        private static final double MULT = 1.0 / 65535;
        private final int[] projectionMin;
        private final int[] projectionMax;
        private final int[] dirX;
        private final int[] dirY;

        private IntVersion(Boundary2DScanner parent,
            ContourLineType contourLineType, double startAngleInRadians, int numberOfDirections)
        {
            super(parent, contourLineType, startAngleInRadians, numberOfDirections);
            if (!acceptableSizes(parent.matrix()))
                throw new IllegalArgumentException("Too large matrix sizes");

            this.projectionMin = new int[m];
            this.projectionMax = new int[m];
            this.dirX = new int[m];
            this.dirY = new int[m];
            for (int k = 0; k < m; k++) {
                this.dirX[k] = (int) Math.round(65535 *
                    StrictMath.cos(startAngleInRadians + StrictMath.PI * (double) k / (double) m));
                this.dirY[k] = (int) Math.round(65535 *
                    StrictMath.sin(startAngleInRadians + StrictMath.PI * (double) k / (double) m));
            }
        }

        public static boolean acceptableSizes(Matrix<?> m) {
            return m.dimX() <= (1L << 13) && m.dimY() <= (1L << 13);
            // so, x*65535+y*65535 is in -2^30+1..2^30-1 range
        }

        @Override
        public double projectionMin(int directionIndex) {
            return projectionMin[directionIndex] * MULT;
        }

        @Override
        public double projectionMax(int directionIndex) {
            return projectionMax[directionIndex] * MULT;
        }

        @Override
        public double projectionLength(int directionIndex) {
            return (projectionMax[directionIndex] - projectionMin[directionIndex]) * MULT;
        }

        @Override
        public void next() {
            parent.next();
            int x = (int) parent.x();
            int y = (int) parent.y();
            for (int k = 0; k < m; k++) {
                int projection = x * dirX[k] + y * dirY[k];
                if (projection < projectionMin[k]) {
                    projectionMin[k] = projection;
                }
                if (projection > projectionMax[k]) {
                    projectionMax[k] = projection;
                }
            }
        }

        @Override
        public void reset() {
            JArrays.fillIntArray(this.projectionMin, Integer.MAX_VALUE);
            JArrays.fillIntArray(this.projectionMax, Integer.MIN_VALUE);
        }
    }
    */
}
