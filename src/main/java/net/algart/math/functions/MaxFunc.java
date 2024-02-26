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

package net.algart.math.functions;

final class MaxFunc implements Func {
    MaxFunc() {
    }

    public double get(double ...x) {
        if (x.length == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        double result = x[0];
        for (int k = 1; k < x.length; k++)
            if (x[k] > result)
                result = x[k];
        return result;
    }

    public double get() {
        return Double.NEGATIVE_INFINITY;
    }

    public double get(double x0) {
        return x0;
    }

    public double get(double x0, double x1) {
        return x0 >= x1 ? x0 : x1;
    }

    public double get(double x0, double x1, double x2) {
        double x = x0 >= x1 ? x0 : x1;
        return x >= x2 ? x : x2;
    }

    public double get(double x0, double x1, double x2, double x3) {
        double x = x0 >= x1 ? x0 : x1;
        double y = x2 >= x3 ? x2 : x3;
        return x >= y ? x : y;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "maximum function f(x0,x1,...)=max(x0,x1,...)";
    }
}
