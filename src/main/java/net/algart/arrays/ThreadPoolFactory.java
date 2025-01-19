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

package net.algart.arrays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * <p>The factory allowing to get a thread pool (<code>ExecutorService</code>)
 * for processing some AlgART array.</p>
 *
 * <p>The object, implementing this interface, should be used when you need to
 * process some AlgART array ("source array") in several parallel threads
 * to improve performance on the multiprocessor systems.
 * In this case, you should split the algorithm into several tasks, where each task
 * calculates a part of the result (usually processes a region of the source array).
 * Then you need to call {@link #getThreadPool(Array, ThreadFactory)
 * getThreadPool(sourceArray, someThreadFactory)}
 * method of this object, submit all your tasks to the returned thread pool,
 * wait until all tasks will be completed (for example, by <code>Future.get</code> method)
 * and, at the end, call {@link #releaseThreadPool(ExecutorService)} method
 * to finish working with the thread pool.
 * The number of tasks, which your algorithm is split into, should be
 * equal to the result of {@link #recommendedNumberOfTasks(Array) recommendedNumberOfTasks(sourceArray)}
 * method of this object. Note: if this number is 1, you should ignore multithreading and
 * perform the task in the current thread.</p>
 *
 * <p>The technique described above can be automated by
 * {@link #performTasks(java.util.concurrent.ThreadFactory, Runnable[])} method.</p>
 *
 * <p>We recommend to pass some instance of this interface to every algorithm
 * using multithreading technique to optimize performance on multiprocessor computers.
 * Then the application will be able to call algorithms with
 * a custom implementations of this interface, which controls the number of threads
 * and, maybe, supports a global application thread pool.
 * In particular, an instance of this interface can be passed to the standard
 * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method
 * via the custom implementation of {@link ArrayContext} interface.</p>
 *
 * <p>There is a simple default implementation of this interface:
 * {@link DefaultThreadPoolFactory}.</p>
 *
 * @author Daniel Alievsky
 */
public interface ThreadPoolFactory {

    /**
     * Returns the recommended number of tasks, which your algorithm is split into
     * for optimizing processing large data.
     * The returned value can be equal (or depend on) the result of
     * <code>{@link Arrays.SystemSettings#availableProcessors()}</code>,
     * or can be specified in some system property or environment variable.
     *
     * <p>The returned value is always positive.
     *
     * @return the recommended number of parallel tasks to perform the processing (&gt;0).
     */
    int recommendedNumberOfTasks();

    /**
     * Returns the recommended number of tasks, which your algorithm is split into
     * for optimizing processing the given AlgART on multiprocessor computers.
     * The returned value can be equal (or depend on) the result of
     * <code>{@link Arrays.SystemSettings#availableProcessors()}</code>,
     * or can be specified in some system property or environment variable.
     * For little arrays, that are usually processed very quickly,
     * we recommend to return <code>1</code> regardless of the number of processors.
     *
     * <p>The returned value is always positive.
     *
     * @param sourceArray some AlgART array that should be processed.
     * @return            the recommended number of parallel tasks to perform the processing (&gt;0).
     * @throws NullPointerException if the argument is {@code null} (not necessary).
     */
    int recommendedNumberOfTasks(Array sourceArray);

    /**
     * Returns new factory, identical to this one with the only exception that the recommended number of tasks
     * in the created instance is always one.
     * More precisely, the {@link #recommendedNumberOfTasks()} and {@link #recommendedNumberOfTasks(Array)}
     * methods in the created factory always return 1, and all other methods are strictly equivalent
     * to the corresponding methods of this instance.
     *
     * @return the single-thread version of this factory.
     */
    ThreadPoolFactory singleThreadVersion();

    /**
     * Returns the thread pool that should be used for multithreading processing large data.
     * Depending on implementation, this method may either create a new thread pool,
     * or return some already existing pool, for example, the global thread pool of the application.
     * In any case, you <b>must</b> call, in a <code>finally</code> section,
     * the {@link #releaseThreadPool(ExecutorService)} method for this pool
     * after finishing your algorithm.
     *
     * <p>The <code>threadFactory</code> can be used for creation new thread pool.
     * This argument <i>can be {@code null}</i>: in this case, some default thread factory should be used.
     *
     * @param threadFactory if not {@code null}, specifies the desired thread factory for using by the thread pool.
     * @return              the thread pool for parallel processing large data.
     */
    ExecutorService getThreadPool(ThreadFactory threadFactory);

    /**
     * Returns the thread pool that should be used for multithreading processing an AlgART array.
     * Depending on implementation, this method may either create a new thread pool,
     * or return some already existing pool, for example, the global thread pool of the application.
     * In any case, you <b>must</b> call, in a <code>finally</code> section,
     * the {@link #releaseThreadPool(ExecutorService)} method for this pool
     * after finishing your algorithm.
     *
     * <p>The <code>threadFactory</code> may be used for creation new thread pool.
     * This argument <i>can be {@code null}</i>: in this case, some default thread factory should be used.
     *
     * @param sourceArray   some AlgART array that should be processed.
     * @param threadFactory if not {@code null}, specifies the desired thread factory for using by the thread pool.
     * @return              the thread pool for parallel processing the array.
     * @throws NullPointerException if <code>sourceArray</code> argument is {@code null} (not necessary).
     */
    ExecutorService getThreadPool(Array sourceArray, ThreadFactory threadFactory);

    /**
     * Finishes using the thread pool returned by {@link #getThreadPool(Array, ThreadFactory)} method.
     * This method <b>must be called</b> in a <code>finally</code> section
     * after finishing usage of the pool.
     * The reason is that if the implementation of {@link #getThreadPool(Array, ThreadFactory)} method
     * has created a new thread pool, this method probably calls its <code>shutdown</code>
     * method to remove extra system threads.
     *
     * @param pool the thread pool created by the previous {@link #getThreadPool(Array, ThreadFactory)} call.
     * @throws NullPointerException if the argument is {@code null} (not necessary).
     */
    void releaseThreadPool(ExecutorService pool);

    /**
     * Equivalent to <code>{@link #performTasks(java.util.concurrent.ThreadFactory, Runnable[])
     * performTasks}(null, tasks)</code> call.
     *
     * @param tasks the tasks which should be performed.
     * @throws NullPointerException if <code>tasks</code> argument or one of the tasks is {@code null}.
     */
    void performTasks(Runnable[] tasks);

    /**
     * Performs the specified tasks by the thread pool, returned by
     * {@link #getThreadPool(java.util.concurrent.ThreadFactory) getThreadPool(threadFactory)} method
     * in the beginning of execution.
     *
     * <p>More precisely, if <code>tasks.length==0</code>, this method does nothing,
     * if <code>tasks.length==1</code>, this method just calls <code>tasks[0].run()</code>,
     * and if <code>tasks.length&gt;1</code>, the tasks are performed by the following code
     * (where <code>pool</code> is the result of
     * {@link #getThreadPool(java.util.concurrent.ThreadFactory) getThreadPool(threadFactory)} call):
     *
     * <pre>
     *
     * Future&lt;?&gt;[] results = new Future&lt;?&gt;[tasks.length];
     * for (int threadIndex = 0; threadIndex &lt; tasks.length; threadIndex++) {
     * &#32;   results[threadIndex] = pool.submit(tasks[threadIndex]);
     * }
     * try {
     * &#32;   for (int threadIndex = 0; threadIndex &lt; tasks.length; threadIndex++) {
     * &#32;       results[threadIndex].get(); // waiting for finishing
     * &#32;   }
     * } catch (ExecutionException ex) {
     * &#32;   Throwable cause = ex.getCause();
     * &#32;   if (cause instanceof RuntimeException)
     * &#32;       throw (RuntimeException)cause;
     * &#32;   if (cause instanceof Error)
     * &#32;       throw (Error)cause;
     * &#32;   throw new AssertionError("Unexpected checked exception: " + cause);
     * &#32;   // it is impossible, because run() method in tasks does not declare any exceptions
     * } catch (InterruptedException ex) {
     * &#32;   throw new java.io.IOError(ex);
     * }
     * </pre>
     *
     * <p>Before finishing, this method calls {@link #releaseThreadPool} method for the used pool
     * (in <code>finally</code> section).
     *
     * <p>As you see, if <code>java.util.concurrent.ExecutionException</code> is thrown
     * while calling one of <code>results[...].get()</code>, this exception is caught,
     * and this method throws the result of its <code>getCause()</code> method.
     * In other words, all possible exceptions while performing tasks are thrown as if they would be
     * performed in the current thread.
     *
     * <p>If <code>java.lang.InterruptedException</code> is thrown
     * while calling one of <code>results[...].get()</code>,
     * this exception is also caught, and this method throws <code>java.io.IOError</code>.
     * Usually, you should avoid interrupting the threads, processing AlgART arrays,
     * via <code>Thread.interrupt()</code> technique (which leads to <code>java.lang.InterruptedException</code>):
     * see the package description about runtime exceptions issue.
     *
     * @param threadFactory the factory, passed to {@link #getThreadPool(java.util.concurrent.ThreadFactory)}
     *                      method to get the necessary thread pool;
     *                      can be {@code null}, then some default thread factory will be used.
     * @param tasks         the tasks which should be performed.
     * @throws NullPointerException if <code>tasks</code> argument or one of the tasks is {@code null}.
     * @see #performTasks(Array, java.util.concurrent.ThreadFactory, Runnable[])
     */
    void performTasks(ThreadFactory threadFactory, Runnable[] tasks);

    /**
     * Equivalent to {@link #performTasks(java.util.concurrent.ThreadFactory, Runnable[])} method
     * with the only difference, that the thread pool is got via
     * {@link #getThreadPool(Array, java.util.concurrent.ThreadFactory)
     * getThreadPool(sourceArray, threadFactory)} method.
     *
     * @param sourceArray   the AlgART array, passed to
     *                      {@link #getThreadPool(Array, java.util.concurrent.ThreadFactory)}
     *                      method to get the necessary thread pool.
     * @param threadFactory the factory, passed to
     *                      {@link #getThreadPool(Array, java.util.concurrent.ThreadFactory)}
     *                      method to get the necessary thread pool;
     *                      can be {@code null}, then some default thread factory will be used.
     * @param tasks         the tasks which should be performed.
     * @throws NullPointerException if <code>tasks</code> argument or one of the tasks is {@code null}.
     */
    void performTasks(Array sourceArray, ThreadFactory threadFactory, Runnable[] tasks);

    /**
     * Equivalent to <code>{@link #performTasks(Runnable[])
     * performTasks}(java.util.Arrays.copyOfRange(tasks, from, to))</code> call.
     *
     * @param from  the initial index of the performed task, inclusive
     * @param to    the final index of the performed task, exclusive. (This index may lie outside the array.)
     * @param tasks the tasks which should be performed.
     * @throws NullPointerException      if <code>tasks</code> argument
     *                                   or one of tasks in the specified range is {@code null}.
     * @throws IndexOutOfBoundsException if <code>from &lt; 0</code> or <code>from &gt; tasks.length</code>.
     * @throws IllegalArgumentException  if <code>from &gt; to</code>.
     */
    void performTasks(Runnable[] tasks, int from, int to);

    /**
     * Equivalent to <code>{@link #performTasks(ThreadFactory, Runnable[])
     * performTasks}(threadFactory, java.util.Arrays.copyOfRange(tasks, from, to))</code> call.
     *
     * @param threadFactory the factory, passed to {@link #getThreadPool(java.util.concurrent.ThreadFactory)}
     *                      method to get the necessary thread pool;
     *                      can be {@code null}, then some default thread factory will be used.
     * @param from  the initial index of the performed task, inclusive
     * @param to    the final index of the performed task, exclusive. (This index may lie outside the array.)
     * @param tasks the tasks which should be performed.
     * @throws NullPointerException      if <code>tasks</code> argument or
     *                                   or one of tasks in the specified range is {@code null}.
     * @throws IndexOutOfBoundsException if <code>from &lt; 0</code> or <code>from &gt; tasks.length</code>.
     * @throws IllegalArgumentException  if <code>from &gt; to</code>.
     */
    void performTasks(ThreadFactory threadFactory, Runnable[] tasks, int from, int to);
}
