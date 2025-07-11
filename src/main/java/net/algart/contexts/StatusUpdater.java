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

/**
 * <p>The context allowing to inform the user about execution of a long-working method
 * via some status line or its analog.</p>
 *
 * <p>When possible, we recommend using {@link ProgressUpdater} instead, because that context
 * is more abstract and does not require an algorithm to provide textual information, as this one.
 * If you decide to process this context in some algorithm,
 * you should keep in mind that the algorithm may be used in different countries,
 * so the passed textual messages should either support localized versions for main languages,
 * or be very brief and simple.</p>
 *
 * @author Daniel Alievsky
 */
public interface StatusUpdater extends Context {
    /**
     * Equivalent to {@link #updateStatus(String, boolean) updateStatus(message, true)}.
     *
     * <p>This method can throw <code>NullPointerException</code>, if its <code>message</code> argument is {@code null},
     * but it is not guaranteed.
     *
     * @param message some information message; must not be {@code null}.
     */
    void updateStatus(String message);

    /**
     * Shows some information message to the user.
     * The message should be one line and brief, to allow an application to show it inside a little status line.
     *
     * <p>The <code>force</code> argument determines whether this information must be
     * shown to the user with a guarantee. If this argument is <code>false</code>,
     * this method <i>may</i> do nothing to save processing time when very often calls.
     * Usual implementation of this method contains a time check (<code>System.currentTimeMillis()</code>)
     * and, if <code>!force</code>, does nothing if the previous call (for the same context)
     * was performed several tens of milliseconds ago.
     * Please avoid too frequent calls of this method with <code>force=true</code>:
     * millions of such calls may require long time.
     *
     * <p>This method can throw <code>NullPointerException</code>, if its <code>message</code> argument is {@code null},
     * but it is not guaranteed.
     *
     * @param message some information message; must not be {@code null}.
     * @param force   whether this information must be shown always (<code>true</code>)
     *                or may be lost (<code>false</code>).
     */
    void updateStatus(String message, boolean force);
}
