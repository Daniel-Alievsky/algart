/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>The class allowing to execute some action, interruptable via standard Java technique,
 * in terms of some {@link InterruptionContext}.
 * In other words, it is a bridge between {@link InterruptionContext} and standard interruption technique
 * based on <tt>thread.interrupt()</tt> and <tt>Thread.interrupted()</tt> calls.</p>
 *
 * <p>To use this class, please override its {@link #run()} method and call {@link #doInterruptibly()}.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class InterruptableAction<T> {
    private final InterruptionContext context;

    /**
     * Creates an instance of this class with the given interruption context.
     *
     * @param context an interruption context that will be used for interruption of the action.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    protected InterruptableAction(InterruptionContext context) {
        if (context == null)
            throw new NullPointerException("Null context argument");
        this.context = context;
    }

    /**
     * Executes {@link #run()} method and returns its result.
     *
     * <p>If the interruption context requests an interruption
     * (i.e. an {@link InterruptionContext.Event interruption event} occurs),
     * the current thread, executing {@link #run()} method, is interrupted
     * by its <tt>interrupt()</tt> method.
     * As a result, the {@link #run()} method should stop and
     * throw the standard <tt>InterruptedException</tt>.
     * This method catches it and translates into {@link InterruptionException} with the corresponding cause;
     * this {@link InterruptionException} is thrown.
     *
     * <p>So, this method can be interrupted by both ways: by the standard
     * <tt>thread.interrupt()</tt> call and by interruption mechanism
     * provided by the {@link InterruptionContext}.
     *
     * @return the result of {@link #run()} method.
     * @throws InterruptionException if the {@link #run()} method throws <tt>InterruptedException</tt>.
     */
    public T doInterruptibly() throws InterruptionException {
        final Thread currentThread = Thread.currentThread();
        InterruptionContext.Listener listener = new InterruptionContext.Listener() {
            public void interruptionRequested(InterruptionContext.Event event) {
                currentThread.interrupt();
            }
        };
        context.addInterruptionListener(listener);
        try {
            return run();
        } catch (InterruptedException ex) {
            throw new InterruptionException(ex);
        } finally {
            context.removeInterruptionListener(listener);
        }
    }

    /**
     * This method performs some action or throws <tt>InterruptedException</tt>
     * in a case when the current thread is interrupted via <tt>thread.interrupt()</tt> call.
     *
     * @return the computed result.
     * @throws InterruptedException if the current thread is interrupted via <tt>thread.interrupt()</tt> call.
     */
    public abstract T run() throws InterruptedException;
}
