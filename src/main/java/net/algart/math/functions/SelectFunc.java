/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

final class SelectFunc implements Func {
    SelectFunc() {
    }

    public double get(double ...x) {
        return x[(int)x[0] + 1];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0, double x1) {
        int i = (int)x0;
        if (i == 0)
            return x1;
        throw new IndexOutOfBoundsException("Illegal index " + i);
    }

    public double get(double x0, double x1, double x2) {
        int i = (int)x0;
        if (i == 0)
            return x1;
        if (i == 1)
            return x2;
        throw new IndexOutOfBoundsException("Illegal index " + i);
    }

    public double get(double x0, double x1, double x2, double x3) {
        int i = (int)x0;
        if (i == 0)
            return x1;
        if (i == 1)
            return x2;
        if (i == 2)
            return x3;
        throw new IndexOutOfBoundsException("Illegal index " + i);
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "select function f(x0,x1,...)=x[(int)x0+1]";
    }
}
