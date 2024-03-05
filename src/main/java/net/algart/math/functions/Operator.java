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

/**
 * <p>Abstract operator: a transformation from one {@link Func mathematical function} to another.
 *
 * <p>Implementations of this interface are usually <b>immutable</b> and
 * always <b>thread-safe</b>: {@link #apply apply} method of this interface may be freely used
 * while simultaneous accessing the same instance from several threads.
 * All implementations of this interface from this package are <b>immutable</b>.</p>
 *
 * @author Daniel Alievsky
 */
public interface Operator {
    /**
     * Identity operator, transforming any function to itself.
     * The {@link #apply(Func)} method of this operator returns the reference to its argument.
     */
    Operator IDENTITY = new IdentityOperator();

    /**
     * Returns the result of applying this operator to the given function.
     *
     * @param f some function.
     * @return  new transformed function.
     */
    Func apply(Func f);
}
