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

package net.algart.matrices.stitching;

import net.algart.math.functions.Func;

public class LastExceptingNaN implements Func {
    private final double defaultValue;

    private LastExceptingNaN(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static LastExceptingNaN getInstance(double defaultValue) {
        return new LastExceptingNaN(defaultValue);
    }

    public double get(double... x) {
        for (int k = x.length - 1; k >= 0; k--) {
            if (x[k] == x[k]) { // not NaN
                return x[k];
            }
        }
        return defaultValue;
    }

    public double get() {
        return defaultValue;
    }

    public double get(double x0) {
        return x0 == x0 ? x0 : defaultValue;
    }

    public double get(double x0, double x1) {
        if (x1 == x1) {
            return x1;
        }
        if (x0 == x0) {
            return x0;
        }
        return defaultValue;
    }

    public double get(double x0, double x1, double x2) {
        if (x2 == x2) {
            return x2;
        }
        if (x1 == x1) {
            return x1;
        }
        if (x0 == x0) {
            return x0;
        }
        return defaultValue;
    }

    public double get(double x0, double x1, double x2, double x3) {
        if (x3 == x3) {
            return x3;
        }
        if (x2 == x2) {
            return x2;
        }
        if (x1 == x1) {
            return x1;
        }
        if (x0 == x0) {
            return x0;
        }
        return defaultValue;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "last not-NaN argument";
    }
}
