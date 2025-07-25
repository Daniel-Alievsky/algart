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

package net.algart.matrices.stitching;

import net.algart.math.functions.Func;

public class MaxExceptingNaN implements Func {
    private final double defaultValue;

    private MaxExceptingNaN(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static MaxExceptingNaN getInstance(double defaultValue) {
        return new MaxExceptingNaN(defaultValue);
    }

    public double get(double... x) {
        boolean valid = false;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : x) {
            if (v == v) { // not NaN
                if (v > max) {
                    max = v;
                }
                valid = true;
            }
        }
        return valid ? max : defaultValue;
    }

    public double get() {
        return defaultValue;
    }

    public double get(double x0) {
        return x0 == x0 ? x0 : defaultValue;
    }

    public double get(double x0, double x1) {
        return x0 == x0 ?
            (x1 == x1 ? Math.max(x0, x1) : x0) :
            (x1 == x1 ? x1 : defaultValue);
    }

    public double get(double x0, double x1, double x2) {
        boolean valid = false;
        if (x0 == x0) {
            valid = true;
        } else {
            x0 = Double.NEGATIVE_INFINITY;
        }
        if (x1 == x1) {
            valid = true;
        } else {
            x1 = Double.NEGATIVE_INFINITY;
        }
        if (x2 == x2) {
            valid = true;
        } else {
            x2 = Double.NEGATIVE_INFINITY;
        }
        if (!valid) {
            return defaultValue;
        }
        double x = Math.max(x0, x1);
        return Math.max(x, x2);
    }

    public double get(double x0, double x1, double x2, double x3) {
        boolean valid = false;
        if (x0 == x0) {
            valid = true;
        } else {
            x0 = Double.NEGATIVE_INFINITY;
        }
        if (x1 == x1) {
            valid = true;
        } else {
            x1 = Double.NEGATIVE_INFINITY;
        }
        if (x2 == x2) {
            valid = true;
        } else {
            x2 = Double.NEGATIVE_INFINITY;
        }
        if (x3 == x3) {
            valid = true;
        } else {
            x3 = Double.NEGATIVE_INFINITY;
        }
        if (!valid) {
            return defaultValue;
        }
        double x = Math.max(x0, x1);
        double y = Math.max(x2, x3);
        return Math.max(x, y);
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "maximum function skipping NaN arguments";
    }
}
