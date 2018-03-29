/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.*;

/**
 * <p>A simple implementation of {@link ArrayContext} interface,
 * based on the {@link ArrayMemoryContext memory},
 * {@link ArrayThreadPoolContext thread pool},
 * {@link InterruptionContext interruption} and
 * {@link ProgressUpdater progress} contexts defined in this package.
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class DefaultArrayContext extends AbstractArrayContext implements ArrayContext {
    private final ArrayMemoryContext arrayMemoryContext;
    private final ArrayThreadPoolContext arrayThreadPoolContext;
    private final InterruptionContext interruptionContext;
    private final ProgressUpdater progressUpdater;
    private final ThreadPoolFactory threadPoolFactory;
    private final String parentContextString;

    /**
     * Creates new instance of this class, based on the passed context.
     * The passed context must {@link Context#is allow to get} the following specific contexts:
     * {@link ArrayMemoryContext},
     * {@link ArrayThreadPoolContext},
     * {@link InterruptionContext} and
     * {@link ProgressUpdater}.
     *
     * @param context the context that will be used by methods of this class.
     * @throws NullPointerException        if the argument is <tt>null</tt>.
     * @throws UnsupportedContextException if this context cannot serve at least one from
     *                                     the requested 4 specified contexts.
     */
    public DefaultArrayContext(Context context) {
        if (context == null)
            throw new NullPointerException("Null context argument");
        this.arrayMemoryContext = context.as(ArrayMemoryContext.class);
        this.arrayThreadPoolContext = context.as(ArrayThreadPoolContext.class);
        this.interruptionContext = context.as(InterruptionContext.class);
        this.progressUpdater = context.as(ProgressUpdater.class);
        this.threadPoolFactory = null;
        this.parentContextString = context.toString();
    }

    /**
     * Creates new instance of this class, based on the passed context,
     * which returns the specified <tt>threadPoolFactory</tt> by
     * {@link #getThreadPoolFactory()} method.
     * The passed context must {@link Context#is allow to get} the following specific contexts:
     * {@link ArrayMemoryContext},
     * {@link InterruptionContext} and
     * {@link ProgressUpdater}.
     * This constructor can be used, for example, if you need to clarify
     * the desired number of parallel threads (usually set it to 1)
     * by {@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory(int)} method.
     *
     * @param context           the context that will be used by methods of this class.
     * @param threadPoolFactory the thread pool factory that will be used by this object.
     * @throws NullPointerException        if the <tt>context</tt> or <tt>threadPoolFactory</tt> argument
     *                                     is <tt>null</tt>.
     * @throws UnsupportedContextException if this context cannot serve at least one from
     *                                     the requested 3 specified contexts.
     */
    public DefaultArrayContext(Context context, ThreadPoolFactory threadPoolFactory) {
        if (context == null)
            throw new NullPointerException("Null context argument");
        if (threadPoolFactory == null)
            throw new NullPointerException("Null threadPoolFactory argument");
        this.arrayMemoryContext = context.as(ArrayMemoryContext.class);
        this.arrayThreadPoolContext = null;
        this.interruptionContext = context.as(InterruptionContext.class);
        this.progressUpdater = context.as(ProgressUpdater.class);
        this.threadPoolFactory = threadPoolFactory;
        this.parentContextString = context.toString();
    }

    /**
     * Returns the result of <tt>context.{@link Context#as(Class)
     * as}({@link ArrayMemoryContext}.class).{@link ArrayMemoryContext#getMemoryModel()
     * getMemoryModel()}</tt> call, where <tt>context</tt> is the argument of the construcor.
     *
     * @return the desired thread pool factory.
     */
    public MemoryModel getMemoryModel() {
        return arrayMemoryContext.getMemoryModel();
    }

    /**
     * Returns the result of <tt>context.{@link Context#as(Class)
     * as}({@link ArrayThreadPoolContext}.class).{@link ArrayThreadPoolContext#getThreadPoolFactory()
     * getThreadPoolFactory()}</tt> call, where <tt>context</tt> is the argument of the construcor,
     * or <tt>threadPoolFactory</tt> constructor argument,
     * if this instance was created with
     * {@link DefaultArrayContext#DefaultArrayContext(Context, ThreadPoolFactory)
     * the corresponding variant of the constructor}.
     *
     * @return the desired thread pool factory.
     */
    public ThreadPoolFactory getThreadPoolFactory() {
        return threadPoolFactory != null ? threadPoolFactory : arrayThreadPoolContext.getThreadPoolFactory();
    }

    /**
     * Calls <tt>context.{@link Context#as(Class)
     * as}({@link InterruptionContext}.class).{@link InterruptionContext#checkInterruption()
     * checkInterruption()}</tt>, where <tt>context</tt> is the argument of the construcor.
     *
     * @throws RuntimeException if the application has requested to interrupt the currently executing module;
     *                          in this implementation, it will be always {@link InterruptionException} instance.
     */
    public void checkInterruption() throws RuntimeException {
        interruptionContext.checkInterruption();
    }

    /**
     * Calls <tt>context.{@link Context#as(Class)
     * as}({@link ProgressUpdater}.class).{@link ProgressUpdater#updateProgress(double, boolean)
     * updateProgress}(part, part==1.0)</tt>, where <tt>context</tt> is the argument of the construcor
     * and <tt>part=event.{@link net.algart.arrays.ArrayContext.Event#readyPart()
     * readyPart()}</tt>.
     *
     * @param event information about the execution progress.
     */
    public void updateProgress(ArrayContext.Event event) {
        double part = event.readyPart();
        progressUpdater.updateProgress(part, part == 1.0);
    }

    @Override
    public String toString() {
        return "default array context based on " + parentContextString;
    }
}
