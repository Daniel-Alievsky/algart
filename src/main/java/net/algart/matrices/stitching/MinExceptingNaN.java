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

public class MinExceptingNaN implements Func {
    private final double defaultValue;

    private MinExceptingNaN(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static MinExceptingNaN getInstance(double defaultValue) {
        return new MinExceptingNaN(defaultValue);
    }

    public double get(double... x) {
        boolean valid = false;
        double min = Double.POSITIVE_INFINITY;
        for (double v : x) {
            if (v == v) { // not NaN
                if (v < min) {
                    min = v;
                }
                valid = true;
            }
        }
        return valid ? min : defaultValue;
    }

    public double get() {
        return defaultValue;
    }

    public double get(double x0) {
        return x0 == x0 ? x0 : defaultValue;
    }

    public double get(double x0, double x1) {
        return x0 == x0 ?
            (x1 == x1 ? Math.min(x0, x1) : x0) :
            (x1 == x1 ? x1 : defaultValue);
    }

    public double get(double x0, double x1, double x2) {
        boolean valid = false;
        if (x0 == x0) {
            valid = true;
        } else {
            x0 = Double.POSITIVE_INFINITY;
        }
        if (x1 == x1) {
            valid = true;
        } else {
            x1 = Double.POSITIVE_INFINITY;
        }
        if (x2 == x2) {
            valid = true;
        } else {
            x2 = Double.POSITIVE_INFINITY;
        }
        if (!valid) {
            return defaultValue;
        }
        double x = Math.min(x0, x1);
        return Math.min(x, x2);
    }

    public double get(double x0, double x1, double x2, double x3) {
        boolean valid = false;
        if (x0 == x0) {
            valid = true;
        } else {
            x0 = Double.POSITIVE_INFINITY;
        }
        if (x1 == x1) {
            valid = true;
        } else {
            x1 = Double.POSITIVE_INFINITY;
        }
        if (x2 == x2) {
            valid = true;
        } else {
            x2 = Double.POSITIVE_INFINITY;
        }
        if (x3 == x3) {
            valid = true;
        } else {
            x3 = Double.POSITIVE_INFINITY;
        }
        if (!valid) {
            return defaultValue;
        }
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
        return "minimum function skipping NaN arguments";
    }
}
