/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.functions;

final class SelectFrom8Directions2DFunc implements Func {
    private static final double COS_PI_DIV_8 = StrictMath.cos(StrictMath.PI / 8.0);
    private static final double SIN_PI_DIV_8 = StrictMath.sin(StrictMath.PI / 8.0);

    SelectFrom8Directions2DFunc() {
    }

    public double get(double... x) { // dx=x[0], dy=x[1]
        double dx = COS_PI_DIV_8 * x[0] - SIN_PI_DIV_8 * x[1];
        double dy = SIN_PI_DIV_8 * x[0] + COS_PI_DIV_8 * x[1];
        // rotate anticlockwise by 22.5 degree
        if (dy >= 0.0) {
            return dx >= dy ? 0 : dx >= 0.0 ? 1 : dx >= -dy ? 2 : 3;
        } else {
            return dx <= dy ? 4 : dx <= 0.0 ? 5 : dx <= -dy ? 6 : 7;
        }
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0, double x1) {
        double dx = COS_PI_DIV_8 * x0 - SIN_PI_DIV_8 * x1;
        double dy = SIN_PI_DIV_8 * x0 + COS_PI_DIV_8 * x1;
        // rotate anticlockwise by 22.5 degree
        if (dy >= 0.0) {
            return dx >= dy ? 0 : dx >= 0.0 ? 1 : dx >= -dy ? 2 : 3;
        } else {
            return dx <= dy ? 4 : dx <= 0.0 ? 5 : dx <= -dy ? 6 : 7;
        }
    }

    public double get(double x0, double x1, double x2) {
        return get(x0, x1);
    }

    public double get(double x0, double x1, double x2, double x3) {
        return get(x0, x1);
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "select from 8 directions in 2D plane f(x,y)=0..7 (depending on x, y)";
    }
}
