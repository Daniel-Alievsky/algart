/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.Objects;

/**
 * <p>An adapter class containing the simplest ("empty") implementations of the following standard contexts:
 * {@link InterruptionContext}, {@link ArrayMemoryContext}, {@link ArrayThreadPoolContext},
 * {@link ProgressUpdater}, {@link StatusUpdater}.
 * See comments to the methods of this class to clarify behavior of this implementation.</p>
 *
 * <p>This class is an inheritor of {@link AbstractContext},
 * and all its constructors calls {@link AbstractContext#AbstractContext(boolean) the superconstructor}
 * with the argument <code>useServiceLoader=true</code>.
 * So, this class can serve requests not only for the standard context listed above,
 * but also for any other contexts, that are specified in
 * {@link java.util.ServiceLoader service providers}
 * for {@link Context} interface.</p>
 *
 * <p>This class is a good possible superclass for implementation custom behavior of the contexts
 * or for overriding the behavior of an existing context, specified as a service provider.</p>
 *
 * @author Daniel Alievsky
 */
public class DefaultContext extends AbstractContext
        implements
        Context, InterruptionContext,
        ArrayMemoryContext, ArrayThreadPoolContext,
        ProgressUpdater, StatusUpdater {
    private final MemoryModel memoryModel;
    private final ThreadPoolFactory threadPoolFactory;

    /**
     * This constructor prevents direct instantiation of this class.
     * Please use {@link #getInstance()} method instead.
     */
    protected DefaultContext() {
        super(true);
        this.memoryModel = Arrays.SystemSettings.globalMemoryModel();
        this.threadPoolFactory = DefaultThreadPoolFactory.getDefaultThreadPoolFactory();
    }

    /**
     * Creates an instance of this context with the specified memory model, which will be returned by
     * {@link #getMemoryModel()} method.
     *
     * @param memoryModel desired memory model.
     */
    public DefaultContext(MemoryModel memoryModel) {
        super(true);
        this.memoryModel = memoryModel;
        this.threadPoolFactory = DefaultThreadPoolFactory.getDefaultThreadPoolFactory();
    }


    /**
     * Returns an instance of this class, created by "<code>new&nbsp;{@link DefaultContext}(){}</code>" call.
     * This instance can be used as a simplest default implementation of most standard contexts,
     * offered by this package: {@link InterruptionContext},
     * {@link ArrayMemoryContext}, {@link ArrayThreadPoolContext},
     * {@link StatusUpdater}, {@link ProgressUpdater}.
     * This implementation does not allow interrupt modules,
     * provides the default {@link net.algart.arrays.Arrays.SystemSettings#globalMemoryModel() global memory model},
     * the {@link DefaultThreadPoolFactory default thread pool factory}
     * and does nothing while attempts to show the status line or execution progress.
     *
     * <p>The result of this method is probably a singleton: all calls of this method
     * may return the same instance.
     *
     * @return the default instance of this class.
     */
    public static DefaultContext getInstance() {
        return DefaultContextHolder.INSTANCE;
    }

    /**
     * This implementation does nothing. So, this context
     * <i>does not allow</i> to interrupt long-working algorithms.
     *
     * <p>The simplest possible implementation of this method in your subclass,
     * allowing to interrupt algorithsm, may be the following:
     *
     * <pre>
     * if (Thread.interrupted())
     *     throw new {@link InterruptionException}();
     * </pre>
     */
    public void checkInterruption() {
    }

    /**
     * This implementation does nothing.
     *
     * @param listener is ignored.
     */
    public void addInterruptionListener(Listener listener) {
    }

    /**
     * This implementation does nothing.
     *
     * @param listener is ignored.
     */
    public void removeInterruptionListener(Listener listener) {
    }


    /**
     * This implementation returns the memory model, specified by the argument of the
     * {@link #DefaultContext(MemoryModel) corresponding constructor},
     * or {@link net.algart.arrays.Arrays.SystemSettings#globalMemoryModel()}
     * if the {@link #DefaultContext() constructor without arguments} was used.
     *
     * @return the desired memory model.
     */
    public MemoryModel getMemoryModel() {
        return memoryModel;
    }

    /**
     * This implementation returns
     * <code>mm.{@link MemoryModel#isElementTypeSupported
     * isElementTypeSupported}(elementType) ? mm : {@link SimpleMemoryModel#getInstance()}</code>,
     * where <code>mm</code> is the result of {@link #getMemoryModel()} method.
     *
     * @param elementType the required element type.
     * @return the desired memory model.
     */
    public MemoryModel getMemoryModel(Class<?> elementType) {
        MemoryModel mm = getMemoryModel();
        return mm.isElementTypeSupported(elementType) ? mm : SimpleMemoryModel.getInstance();
    }

    /**
     * This implementation calls {@link #getMemoryModel()} and returns its result.
     *
     * @param settings additional desires about the required memory model.
     * @return the desired memory model.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public MemoryModel getMemoryModel(String settings) {
        Objects.requireNonNull(settings, "Null settings argument");
        return getMemoryModel();
    }

    /**
     * This implementation returns an instance of {@link DefaultThreadPoolFactory}.
     *
     * @return the desired thread tool factory.
     */
    public ThreadPoolFactory getThreadPoolFactory() {
        return threadPoolFactory;
    }

    /**
     * This implementation calls {@link #updateStatus(String, boolean)}
     * with the first argument alike <code>Math.round(readyPart*100)+"%"</code>
     * and the second argument <code>force</code>.
     *
     * @param readyPart the part of calculations that is already done (from 0.0 to 1.0).
     * @param force     whether this information must be shown always (<code>true</code>)
     *                  or may be lost (<code>false</code>).
     */
    public void updateProgress(double readyPart, boolean force) {
        updateStatus(Math.round(readyPart * 100) + "%", force);
    }

    /**
     * This implementation calls {@link #updateStatus(String, boolean) updateStatus(message, true)}.
     *
     * @param message some information message.
     */
    public void updateStatus(String message) {
        updateStatus(message, true);
    }

    /**
     * This implementation does nothing.
     *
     * @param message some information message.
     * @param force   whether this information must be shown always (<code>true</code>)
     *                or may be lost (<code>false</code>).
     */
    public void updateStatus(String message, boolean force) {
    }

    private static class DefaultContextHolder {
        private static final DefaultContext INSTANCE = new DefaultContext() {
        };
    }
}
