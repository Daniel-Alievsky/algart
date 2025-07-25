/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

final class MinFunc implements Func {
    MinFunc() {
    }

    public double get(double... x) {
        if (x.length == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double result = x[0];
        for (int k = 1; k < x.length; k++) {
            if (x[k] < result) {
                result = x[k];
            }
        }
        return result;
    }

    public double get() {
        return Double.POSITIVE_INFINITY;
    }

    public double get(double x0) {
        return x0;
    }

    public double get(double x0, double x1) {
        return Math.min(x0, x1);
    }

    public double get(double x0, double x1, double x2) {
        double x = Math.min(x0, x1);
        return Math.min(x, x2);
    }

    public double get(double x0, double x1, double x2, double x3) {
        double x = Math.min(x0, x1);
        double y = Math.min(x2, x3);
        return Math.min(x, y);
    }


    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "minimum function f(x0,x1,...)=min(x0,x1,...)";
    }
}
