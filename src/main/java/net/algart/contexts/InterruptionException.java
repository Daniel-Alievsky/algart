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

package net.algart.contexts;

import java.io.Serial;

/**
 * <p>Unchecked analog of the standard <code>InterruptedException</code>.
 * Thrown by {@link InterruptionContext#checkInterruption()} method.</p>
 *
 * @author Daniel Alievsky
 */
public class InterruptionException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public InterruptionException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public InterruptionException(String message) {
        super(message);
    }

    /**
     * Constructs an instance of this class with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    public InterruptionException(Throwable cause) {
        super(cause);
    }

    @Serial
    private static final long serialVersionUID = 8166277608312778448L;
}
