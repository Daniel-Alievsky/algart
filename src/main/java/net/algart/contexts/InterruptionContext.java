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

import java.util.EventListener;

/**
 * <p>The context allowing the user to interrupt any algorithm.
 * It is an extension of standard technique based on <code>thread.interrupt()</code> and
 * <code>Thread.interrupted()</code> calls.</p>
 *
 * <p>The basic way of using this context is calling {@link #checkInterruption()} method
 * in the points of algorithm that are "safe" for interruption.
 * In other words, this method should be called in the places
 * where <code>Thread.interrupted()</code> is called in the standard technique.
 * For example:</p>
 *
 * <pre>
 * public void someComplexAlgorithm({@link Context} context, some-other-arguments...) {
 * &#32;   {@link InterruptionContext} ic = context.{@link #as(Class) as}({@link InterruptionContext}.class);
 * &#32;   . . .
 * &#32;   for (int k = 0; k &lt; n; k++) { // the main long-working loop
 * &#32;       . . .
 * &#32;       ic.{@link #checkInterruption()}
 * &#32;   }
 * &#32;
 * &#32;   . . .
 * }
 * </pre>
 *
 * <p>Unlike <code>Thread.interrupted()</code>, the {@link #checkInterruption()} method informs
 * about interruption request by throwing special {@link InterruptionException}.
 * This behavior complies with the {@link Context context definition}:
 * the method results do not depend on the presense or features of the context.
 * This exception, unlike the standard <code>InterruptedException</code>,
 * is unchecked, so you don't need to specify it in the method declaration.</p>
 *
 * <p>Another possible way of using this context is {@link #addInterruptionListener adding}
 * a {@link Listener listener}, which will be invoked when the application
 * will attempt to interrupt the executing module.
 * The implementation of this listener, provided by the algorithm, for example,
 * can set some volatile flag "<code>isInterrupted</code>", which is checked inside the main loop.
 * Please not forget to {@link #removeInterruptionListener remove} all added listeners
 * (usually in the <code>finally</code> section).</p>
 *
 * <p><i>Please note</i>: some interruption contexts may provide empty implementations of
 * {@link #addInterruptionListener addInterruptionListener} /
 * {@link #removeInterruptionListener removeInterruptionListener} methods!
 * So, we recommend to use {@link #checkInterruption()} method always, when this solution is possible.
 * In most situations the {@link #checkInterruption()} method is quite enough technique;
 * interruption listeners are usually needed in very advanced algorithms only.</p>
 *
 * @author Daniel Alievsky
 */
public interface InterruptionContext extends Context {
    /**
     * The interruption event: an argument of
     * {@link InterruptionContext.Listener#interruptionRequested(InterruptionContext.Event)} method.
     * The application may extend this class to pass some additional information about
     * interruption reason to that method.
     */
    class Event {
    }

    /**
     * The interruption listener, that can be
     * {@link InterruptionContext#addInterruptionListener added} and
     * {@link InterruptionContext#removeInterruptionListener removed} by
     * the {@link InterruptionContext interruption context}.
     */
    interface Listener extends EventListener {
        /**
         * This method is called by an application when it attempts to stop execution
         * of some long-working module.
         *
         * @param event interruption event.
         */
        void interruptionRequested(Event event);
    }

    /**
     * Checks, whether interruption is requested by the application, and throws
     * {@link InterruptionException} in this case.
     *
     * @throws InterruptionException if the application has requested to interrupt the currently executing module.
     */
    void checkInterruption() throws InterruptionException;

    /**
     * Adds the listener to receive interruption requests.
     * May do nothing in some implementations of this interface.
     * Please not forget to remove all added listener
     * by {@link #removeInterruptionListener(Listener)} method (usually in the <code>finally</code>
     * section of your method).
     *
     * <p>If <code>listener</code> is {@code null}, no exception is thrown and no action is performed.
     *
     * @param listener the listener that will be invoked when the application attempts to interrupt module execution.
     */
    void addInterruptionListener(Listener listener);

    /**
     * Removes the listener added by {@link #addInterruptionListener(Listener)} method.
     *
     * <p>If <code>listener</code> is {@code null}, no exception is thrown and no action is performed.
     *
     * @param listener the listener that should be removed.
     */
    void removeInterruptionListener(Listener listener);
}
