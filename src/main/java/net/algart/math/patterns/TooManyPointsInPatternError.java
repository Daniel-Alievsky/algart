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

package net.algart.math.patterns;

import java.io.Serial;

/**
 * <p>Error thrown if a {@link Pattern pattern} is extremely large to be correctly processed.
 * "Extremely large" usually means that the number of points of the pattern is greater, or is probably greater,
 * than <code>Integer.MAX_VALUE</code>=2<sup>31</sup>&minus;1, but the method cannot process such number of points
 * due to Java limits, connected with 32-bit indexing. The typical example is {@link Pattern#points()} method,
 * which throws this exception if the number of points is greater than <code>Integer.MAX_VALUE</code>:
 * this method returns result as Java <code>Set</code> type, which is limited by 2<sup>31</sup>&minus;1 elements.</p>
 *
 * <p>Usually <code>OutOfMemoryError</code> is also probable in situations, when this exception is probable.</p>
 *
 * @author Daniel Alievsky
 */
public class TooManyPointsInPatternError extends Error {
    /**
     * Constructs an instance of this class.
     */
    public TooManyPointsInPatternError() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public TooManyPointsInPatternError(String message) {
        super(message);
    }

    @Serial
    private static final long serialVersionUID = -7030226573403677350L;
}
