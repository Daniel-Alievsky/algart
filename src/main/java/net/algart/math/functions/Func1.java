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

/**
 * Returns a functional version of {@link Func} interface for the function with 1 argument.
 *
 * <p>The method {@link #get(double)} with 1 argument is the <b>main</b>.
 * All other methods have default implementations that call the main method.
 * Method {@link #get()} with 0 arguments throw {@link UnsupportedOperationException}.</p>
 *
 * @author Daniel Alievsky
 */
@FunctionalInterface
public interface Func1 extends Func {
    @Override
    default double get(double... x) {
        return get(x[0]);
    }

    @Override
    default double get() {
        throw new UnsupportedOperationException("At least 1 argument required");
    }

    @Override
    double get(double x0);

    @Override
    default double get(double x0, double x1) {
        return get(x0);
    }

    @Override
    default double get(double x0, double x1, double x2) {
        return get(x0);
    }

    @Override
    default double get(double x0, double x1, double x2, double x3) {
        return get(x0);
    }
}
