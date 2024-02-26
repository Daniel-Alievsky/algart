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

package net.algart.arrays;

/**
 * <p>Array processor allowing to switch the current context.</p>
 *
 * <p>It is an extended version of {@link ArrayProcessor} interface.
 * Many algorithms should implement this interface instead of {@link ArrayProcessor}.
 * The reason is often necessity to extract a subtask of the full algorithmic task,
 * with a context corresponding to this subtask
 * ({@link ArrayContext#part(double, double)} method).
 *
 * @author Daniel Alievsky
 */
public interface ArrayProcessorWithContextSwitching extends ArrayProcessor {

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <tt>newContext</tt> for all operations.
     * The returned instance is usually a clone of this one, but there is no guarantees
     * that it is a deep clone.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another context.
     */
    public ArrayProcessorWithContextSwitching context(ArrayContext newContext);
}
