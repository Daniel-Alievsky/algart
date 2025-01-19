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
 * <p>The context allowing to inform the user about the percents of some long-working method.</p>
 *
 * @author Daniel Alievsky
 */
public interface ProgressUpdater extends Context {
    /**
     * Informs the user that <code>readyPart*100</code> percents of calculations are done.
     *
     * <p>The <code>force</code> argument determines whether this information must be
     * shown to the user with a guarantee. If this argument is <code>false</code>,
     * this method <i>may</i> do nothing to save processing time if it's called very often.
     * Usual implementation of this method contains a time check (<code>System.currentTimeMillis()</code>)
     * and, if <code>!force</code>, does nothing if the previous call (for the same context)
     * was performed several tens of milliseconds ago.
     * Please avoid too frequent calls of this method with <code>force=true</code>:
     * millions of such calls may require long time.
     *
     * <p>For example, if the main work of some method is a long loop, this method
     * can be called in the following manner:
     * <pre>
     * &#32;   for (int k = 0; k &lt; n; k++) {
     * &#32;       . . . // some long calculations
     * &#32;       pu.{@link #updateProgress updateProgress}((k + 1.0) / n, k == n - 1);
     * &#32;       // when k==n-1, we always show the last progress state (100%)
     * &#32;   }
     * &#32;
     * </pre>
     *
     * @param readyPart the part of calculations that is already done (from 0.0 to 1.0).
     * @param force     whether this information must be shown always (<code>true</code>)
     *                  or may be lost (<code>false</code>).
     */
    void updateProgress(double readyPart, boolean force);
}
