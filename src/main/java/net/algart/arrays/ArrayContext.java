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

import java.util.Objects;

/**
 * <p>The context of processing AlgART arrays. It is used by
 * {@link Arrays.ParallelExecutor} class,
 * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method
 * and some other methods of this and other packages, for example,
 * {@link Arrays#rangeOf(ArrayContext, PArray)},
 * {@link Arrays#sumOf(ArrayContext, PArray)},
 * {@link Arrays#preciseSumOf(ArrayContext, PFixedArray)}.</p>
 *
 * <p>We recommend to use <code>net.algart.contexts.DefaultArrayContext</code>
 * as an object implementing this interface.</p>
 *
 * <p>Objects implementing this interface are usually <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public interface ArrayContext {

    /**
     * The simplest implementation of {@link ArrayContext}, that does almost nothing.
     * More precisely, in this object:
     *
     * <ul>
     * <li>{@link #getMemoryModel()} method returns {@link SimpleMemoryModel#getInstance()};</li>
     * <li>{@link #getThreadPoolFactory()} method returns
     * <code>new&nbsp;{@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory()}</code>;</li>
     * <li>{@link #checkInterruption()} method does nothing;</li>
     * <li>{@link #updateProgress(Event)} method does nothing;</li>
     * <li>{@link #currentThreadIndex()} method returns <code>0</code>;</li>
     * <li>{@link #numberOfThreads()} method returns <code>1</code>;</li>
     * <li>{@link #customData()} method returns {@code null}.</li>
     * </ul>
     *
     * <p>In most methods, that use {@link ArrayContext} argument, you can specify {@code null} as context
     * with the same effect as passing this object.
     */
    ArrayContext DEFAULT = new AbstractArrayContext.Default();

    /**
     * The simplest single-thread implementation of {@link ArrayContext}, that does almost nothing.
     * More precisely, in this object:
     *
     * <ul>
     * <li>{@link #getMemoryModel()} method returns {@link SimpleMemoryModel#getInstance()};</li>
     * <li>{@link #getThreadPoolFactory()} method returns
     * <code>new&nbsp;{@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory(int)
     * DefaultThreadPoolFactory.getDefaultThreadPoolFactory(1)}</code>;</li>
     * <li>{@link #checkInterruption()} method does nothing;</li>
     * <li>{@link #updateProgress(Event)} method does nothing;</li>
     * <li>{@link #currentThreadIndex()} method returns <code>0</code>;</li>
     * <li>{@link #numberOfThreads()} method returns <code>1</code>;</li>
     * <li>{@link #customData()} method returns {@code null}.</li>
     * </ul>
     *
     * <p>This constant is useful, if you need some "default" single-thread context.
     * If you specify {@code null} as a context (as well as {@link #DEFAULT} object),
     * it leads to using {@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory()},
     * which tries to use all available processor kernels.
     * To get analogous behavior without a multithreading, you should use this object.
     *
     * <p>Note: this object is also returned by
     * <code>ArrayContext.{@link #DEFAULT}.{@link #singleThreadVersion() singleThreadVersion()}</code> call.
     */
    ArrayContext DEFAULT_SINGLE_THREAD = new AbstractArrayContext.DefaultSingleThread();

    /**
     * Creates the simplest implementation of {@link ArrayContext} with the only difference from
     * the {@link #DEFAULT}/{@link #DEFAULT_SINGLE_THREAD} objects,
     * that {@link #getMemoryModel()} method will return the specified memory model.
     * (If <code>multithreading</code> argument is <code>true</code>, the result will work as
     * {@link #DEFAULT} object, if <code>false</code>, it will work as {@link #DEFAULT_SINGLE_THREAD}.)
     *
     * @param memoryModel memory model.
     * @param multithreading whether the returned context will use multithreading.
     * @return new array context.
     * @throws NullPointerException if the argument is {@code null}.
     */
    static ArrayContext getSimpleContext(MemoryModel memoryModel, boolean multithreading) {
        if (memoryModel instanceof SimpleMemoryModel) {
            return multithreading ? DEFAULT : DEFAULT_SINGLE_THREAD;
        }
        return multithreading ?
                new AbstractArrayContext.Default(memoryModel) :
                new AbstractArrayContext.DefaultSingleThread(memoryModel);
    }

    /**
     * Returns new context, describing the execution of some subtask of the current task,
     * from <code>fromPart*100%</code> of total execution until <code>toPart*100%</code> of total execution.
     * The returned context works alike the current context with the only exception, that
     * its {@link #updateProgress updateProgress} method passes to this (parent) context little-corrected
     * event. Namely, its {@link ArrayContext.Event#readyPart()} method of the passed event
     * returns <code>fromPart+event.readyPart()*(toPart-fromPart)</code>.
     * The methods {@link #getMemoryModel()}, {@link #getThreadPoolFactory()}, {@link #checkInterruption()}
     * of the returned instance call the same methods of this one.
     *
     * <p>Below is an example of the situation when this method is necessary.
     * Let we have 3 methods in some class:
     * <code>fullTask({@link ArrayContext})</code>,
     * <code>subTask1({@link ArrayContext})</code>,
     * <code>subTask2({@link ArrayContext})</code>.
     * The only function of <code>fullTask</code> method is sequential call of
     * <code>subTask1</code> and <code>subTask2</code>.
     * We can implement it in the following way:</p>
     *
     * <pre>
     * void fullTask({@link ArrayContext} context) {
     * &#32;   subTask1(context);
     * &#32;   subTask2(context);
     * }
     * </pre>
     *
     * <p>However, such implementation leads to a problem. The context probably has
     * {@link #updateProgress updateProgress} method, that shows the execution progress to the user,
     * for example, shows the percents of performed calculations, from 0% to 100%.
     * This information is returned by {@link ArrayContext.Event#readyPart()} method.
     * But the implementation, listed above, will show the change of percents from 0% to 100% twice:
     * first time in <code>subTask1</code> method, second time in <code>subTask2</code> method.</p>
     *
     * <p>This class provides solutions of this problem. Namely:</p>
     *
     * <pre>
     * void fullTask({@link ArrayContext} context) {
     * &#32;   subTask1(context.part(0.0, 0.5));
     * &#32;   subTask2(context.part(0.5, 1.0));
     * }
     * </pre>
     *
     * <p>Now the execution of <code>subTask1</code> method will change percents from 0% to 50%
     * and the execution of <code>subTask2</code> method will change percents from 50% to 100%.</p>
     *
     * <p>In many case the overloaded method {@link #part(long, long, long)} is more convenient.
     *
     * @param fromPart the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the start of the subtask:
     *                 see {@link #updateProgress updateProgress} method
     * @param toPart   the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the finish of the subtask:
     *                 see {@link #updateProgress updateProgress} method;
     *                 must be not less than <code>fromPart</code> range.
     * @return         new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <code>fromPart</code> or <code>toPart</code> is not in
     *                                  <code>0.0..1.0</code> range or if <code>fromPart&gt;toPart</code>.
     * @see #part(long, long, long)
     * @see #noProgressVersion()
     */
    ArrayContext part(double fromPart, double toPart);

    /**
     * Returns new context, describing the execution of some subtask of the current task,
     * from <code>from/total*100%</code> of total execution until <code>to/total*100%</code> of total execution.
     * More precisely, equivalent to the following call:
     * <pre>
     * {@link #part(double, double) part}((double)from/(double)total, to==total ? 1.0: (double)to/(double)total)
     * </pre>
     * excepting the case <code>from=to=total=0</code>, when it is equivalent to
     * <pre>
     * {@link #part(double, double) part}(0.0, 1.0)
     * </pre>
     *
     * @param from  the estimated ready part, from 0 to <code>total</code>,
     *              of the total algorithm at the start of the subtask.
     * @param to    the estimated ready part, from 0.0 to <code>total</code>,
     *              of the total algorithm at the finish of the subtask.
     * @param total the number of some operation in the full task.
     * @return      new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <code>from</code> or <code>to</code> is not in <code>0..total</code> range,
     *                                  or if <code>from&gt;to</code>, or if <code>total&lt;0</code>.
     * @see #part(double, double)
     * @see #noProgressVersion()
     */
    ArrayContext part(long from, long to, long total);

    /**
     * Equivalent to {@link #part(long, long, long)} for <code>int</code> arguments.
     * @param from  the estimated ready part, from 0 to <code>total</code>,
     *              of the total algorithm at the start of the subtask.
     * @param to    the estimated ready part, from 0.0 to <code>total</code>,
     *              of the total algorithm at the finish of the subtask.
     * @param total the number of some operation in the full task.
     * @return      new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <code>from</code> or <code>to</code> is not in <code>0..total</code> range,
     *                                  or if <code>from&gt;to</code>, or if <code>total&lt;0</code>.
     */
    ArrayContext part(int from, int to, int total);

    /**
     * Returns new context, identical to this one with the only exception that its
     * {@link #updateProgress(Event)} method does nothing.
     * It can be useful in multithreading algorithms, when some complex tasks,
     * requiring a context for their execution, are executed simultaneously,
     * but these tasks do not "know" about this fact and have no ways to determine
     * the part of total execution.
     *
     * @return the version of this context without progress support.
     * @see #part(double, double)
     * @see #part(long, long, long)
     */
    ArrayContext noProgressVersion();

    /**
     * Returns new context, identical to this one with the only exception that the thread pool factory,
     * returned by {@link #getThreadPoolFactory()} method, returns
     * <code>thisInstance.{@link #getThreadPoolFactory()
     * getThreadPoolFactory()}.{@link ThreadPoolFactory#singleThreadVersion() singleThreadVersion()}</code>.
     *
     * @return the single-thread version of this context.
     */
    ArrayContext singleThreadVersion();

    /**
     * Returns new context, identical to this one with the only exception that
     * {@link #currentThreadIndex()} and {@link #numberOfThreads()} methods in the result return
     * the values, specified in the arguments <code>currentThreadIndex</code> and <code>numberOfThreads</code>.
     *
     * <p>You can use this method, if you need to inform some algorithm, having own context
     * (like {@link ArrayProcessor}), about the number of threads, which will simultaneously execute
     * the same algorithm, and the index of thread, which really performs this algorithm.
     * It can be necessary, for example, if an algorithm uses some external work memory,
     * which must be unique in every thread.
     *
     * <p>Note: this method may be used together with {@link #singleThreadVersion()}, for example:<br>
     * <code>&nbsp;&nbsp;&nbsp;&nbsp;arrayContext.{@link #singleThreadVersion()
     * singleThreadVersion()}.multithreadingVersion(k,n)</code>.<br>
     * Really, there is usually no sense to allow using multithreading (creating thread pools
     * by {@link ThreadPoolFactory} with more than 1 thread) in a thread, which is already called
     * in a multithreading environment simultaneously with other threads.
     *
     * @param currentThreadIndex an index of the thread, which should be executed simultaneously with another threads;
     *                           must be <code>&ge;0</code> and <code>&lt;numberOfThreads</code>.
     * @param numberOfThreads    the number of threads in a group of parallel threads; must be positive.
     * @return                   the version of this context, considered to be used in the thread
     *                           <code>#currentThreadIndex</code> in a group of
     *                           <code>numberOfThreads</code> parallel threads.
     * @throws IllegalArgumentException if <code>numberOfThreads&le;0</code> or if <code>currentThreadIndex</code>
     *                                  does not lie in <code>0..numberOfThreads-1</code> range.
     */
    ArrayContext multithreadingVersion(int currentThreadIndex, int numberOfThreads);

    /**
     * Returns new context, identical to this one with the only exception that
     * {@link #customData()} method in the result returns
     * the value, specified in the argument <code>customData</code>.
     *
     * <p>You can use this method, if you need to inform some algorithm, having own context
     * (like {@link ArrayProcessor}), about some additional details of the current execution process,
     * which is not provided by other methods of this interface.
     * For example: let's have some algorithm, processing some AlgART array,
     * in a form of an abstract class or interface (implementing/extending {@link ArrayProcessorWithContextSwitching})
     * with the method<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<code>process({@link Array} arrayToProcess).</code><br>
     * Then, let's have a special application, where this algorithm is applied to a sequence of
     * {@link Array#subArray(long, long) subarrays} of another large array.
     * It is very probable that your <code>process</code> method does not need to know which subarray
     * (from which position) is processed now, but if you still need this (maybe for debugging),
     * you can pass this information via {@link ArrayProcessorWithContextSwitching#context(ArrayContext) switching
     * the context} to the result of this method with an appropriate <code>customData</code> object.
     *
     * @param customData some custom data; {@code null} is an allowed value.
     * @return           the version of this context, where {@link #customData()} method returns the reference
     *                   to <code>customData</code> argument.
     */
    ArrayContext customDataVersion(Object customData);

    /**
     * Returns the {@link MemoryModel memory model} that should be used
     * for creating any instances of AlgART arrays.
     * This method never returns {@code null}.
     *
     * @return the desired memory model.
     */
    MemoryModel getMemoryModel();

    /**
     * Returns the {@link ThreadPoolFactory thread pool factory} that should be used for
     * planning parallel execution in multithreading methods alike
     * {@link Arrays.ParallelExecutor#process()}.
     * This method never returns {@code null}.
     *
     * @return the desired thread pool factory.
     */
    ThreadPoolFactory getThreadPoolFactory();

    /**
     * This method is called periodically by long-working methods alike
     * {@link Arrays.ParallelExecutor#process()}.
     * If this method throws some <code>RuntimeException</code>, the execution of all running threads
     * is stopped and this exception is re-thrown.
     * You may override this method to allow the user to interrupt the algorithm.
     * Please note that this method may be called from several threads;
     * so, you need to synchronize access to the returned flag.
     *
     * @throws RuntimeException (or some its subclass) if the application has requested to interrupt
     *                          the currently executing module. This exception will lead to stopping
     *                          all threads, started by multithreading method alike
     *                          {@link Arrays#copy(ArrayContext, UpdatableArray, Array) copy},
     *                          and will be re-thrown by that method.
     */
    void checkInterruption() throws RuntimeException;

    /**
     * This method is called periodically by long-working methods alike
     * {@link Arrays.ParallelExecutor#process()}
     * with the argument, containing information about the execution progress.
     * You may override this method to show the progress to the end user.
     *
     * <p>This method may skip updating the corresponding visual element,
     * if it is called too often, to avoid slowing down the program.
     * However, it <i>should</i> update the visual element if {@link ArrayContext.Event#readyPart()}
     * method returns <code>1.0</code>.
     *
     * @param event information about the execution progress.
     */
    void updateProgress(Event event);

    /**
     * Equivalent to 2 sequential calls: <code>{@link #checkInterruption()}</code> and
     * <code>{@link #updateProgress
     * updateProgress}(new ArrayContext.Event(elementType, readyCount, length))</code>.
     *
     * @param elementType           the result of {@link ArrayContext.Event#elementType()} method in the event;
     *                              can be {@code null}.
     * @param readyCount            the only element in the result of {@link ArrayContext.Event#readyCountPerTask()}
     *                              method in the event.
     * @param length                the result of {@link ArrayContext.Event#length()} method in the created event.
     */
    void checkInterruptionAndUpdateProgress(Class<?> elementType, long readyCount, long length);

    /**
     * Usually returns <code>0</code>, but in multithreading environment this method may return the index
     * of the currently executing thread in a group of {@link #numberOfThreads()} parallel threads.
     * This information can be useful, if you create a group of tasks (for example, with help of
     * {@link ThreadPoolFactory} class), which are executed parallel in several threads
     * (usually, to optimize calculations on multiprocessor or multicore computers).
     *
     * <p>To create a context, in which this method returns a value, different from <code>0</code>, please use
     * {@link #multithreadingVersion(int currentThreadIndex, int numberOfThreads)} method.
     *
     * <p>The result of this method always lies in
     * <code>0..{@link #numberOfThreads() numberOfThreads()}-1</code> range.
     *
     * <p>In {@link AbstractArrayContext} implementation, this method returns <code>0</code>.
     *
     * @return the index of the currently executing thread in a group of {@link #numberOfThreads()} parallel threads,
     *         or <code>0</code> if this feature is not used.
     */
    int currentThreadIndex();

    /**
     * Usually returns <code>1</code>, but in multithreading environment this method <i>may</i> return the number
     * of currently executing threads in some group of parallel threads (but not must: in most contexts
     * it still returns 1 even in multithreading environment).
     * This value is not the number of <i>all</i> executing threads and not the number of threads, which are
     * <i>really</i> executing at this moment &mdash; it is the number of elements in some group of tasks
     * (for example, created by {@link ThreadPoolFactory} class), which should be executed parallel in several threads
     * (usually, to optimize calculations on multiprocessor or multicore computers) and which should be distinguished
     * via {@link #currentThreadIndex()} method. The main goal of this method is to estimate
     * the maximal possible value of {@link #currentThreadIndex()}.
     *
     * <p>To create a context, in which this method returns a value different from <code>1</code>, please use
     * {@link #multithreadingVersion(int currentThreadIndex, int numberOfThreads)} method.
     *
     * <p>The result of this method is always positive (<code>&ge;1</code>).
     *
     * <p>In {@link AbstractArrayContext} implementation, this method returns <code>1</code>.
     *
     * @return the number of executing threads in a group of parallel threads,
     *         or <code>1</code> if this feature is not used.
     */
    int numberOfThreads();

    /**
     * Usually returns {@code null}, but in a special environment this method may return some custom object,
     * containing additional information about the current execution context,
     * not provided by other methods of this class.
     *
     * <p>To create a context, in which this method returns a value different from {@code null}, please use
     * {@link #customDataVersion(Object)} method.
     *
     * <p>The result of this method may belong to any type. So, it is a good idea to check its type
     * with <code>instanceof</code> operator before attempt to use this result.
     *
     * <p>In {@link AbstractArrayContext} implementation, this method returns {@code null}.
     *
     * @return some custom information about the current execution environment.
     */
    Object customData();

    /**
     * The array processing event: an argument of {@link ArrayContext#updateProgress(Event)} method.
     */
    class Event {
        private final Class<?> elementType;
        private final long[] lengthPerTask;
        private final long length;
        private final long[] readyCountPerTask;
        private final long readyCount;
        private final int numberOfParallelTasks;

        /**
         * Creates new event with the specified {@link #elementType()}, {@link #length()},
         * {@link #readyCount()} for the case of 1 {@link #numberOfParallelTasks() parallel task}.
         * The {@link #readyPart()} method in the created instance returns
         * <code>(double)readyCount/(double)length</code>.
         * Equivalent to <code>new&nbsp;Event(elementType, new long[] {length}, new long[] {readyCount})</code>.
         *
         * @param elementType           the result of {@link #elementType()} method in the created event;
         *                              can be {@code null}.
         * @param readyCount            the only element in the result of {@link #readyCountPerTask()} method
         *                              in the created event.
         * @param length                the result of {@link #length()} method in the created event.
         * @throws IllegalArgumentException if <code>length</code> or <code>readyCount</code> is negative,
         *                                  or if <code>readyCount&gt;length</code>.
         */
        public Event(Class<?> elementType, long readyCount, long length) {
            this(elementType, new long[] {readyCount}, new long[] {length});
        }

        /**
         * Creates new event with the specified {@link #elementType()}, {@link #lengthPerTask()}
         * and {@link #readyCountPerTask()}.
         * The {@link #length()} method in the created instance returns
         * the sum of all elements of <code>lengthPerTask</code> argument.
         * The {@link #readyCount()} method returns
         * the sum of all elements of <code>readyCountPerTask</code> argument.
         * The {@link #numberOfParallelTasks()} method returns
         * the length of each of the passed arrays (their lengths must be equal).
         * The {@link #readyPart()} method returns
         * <code>(double){@link #readyCount()}/(double){@link #length()}</code>.
         *
         * <p>All elements of the <code>readyCountPerTask</code> array must not be greater than
         * the corresponding elements of the <code>lengthsPerTask</code> array.
         * All elements of these arrays must not be negative.
         *
         * <p>The passed <code>lengthPerTask</code> and <code>readyCountPerTask</code> arguments
         * are cloned by this method: no references to them are maintained by the created instance.
         *
         * @param elementType           the result of {@link #elementType()} method in the created event;
         *                              can be {@code null}.
         * @param readyCountPerTask     the result of {@link #readyCountPerTask()} method in the created event.
         * @param lengthPerTask         the result of {@link #lengthPerTask()} method in the created event.
         * @throws NullPointerException     if <code>lengthPerTask</code> or
         * <code>readyCountPerTask</code> is {@code null}.
         * @throws IllegalArgumentException if <code>lengthPerTask</code> or
         * <code>readyCountPerTask</code> is an empty array,
         *                                  or if their lengths are not equal,
         *                                  or if some their elements are negative,
         *                                  or if <code>readyCountPerTask[k]</code> is greater
         *                                  than <code>lengthPerTask[k]</code>
         *                                  for some index <code>k</code>,
         *                                  or if the sum of all elements of one of these arrays is
         *                                  greater than <code>Long.MAX_VALUE</code>.
         */
        public Event(Class<?> elementType, long[] readyCountPerTask, long[] lengthPerTask) {
            Objects.requireNonNull(readyCountPerTask, "Null readyCountPerTask argument");
            Objects.requireNonNull(lengthPerTask, "Null lengthPerTask argument");
            if (lengthPerTask.length != readyCountPerTask.length) {
                throw new IllegalArgumentException("lengthPerTask and readyCountPerTask have different lengths");
            }
            if (lengthPerTask.length == 0) {
                throw new IllegalArgumentException("Zero number of tasks "
                    + "(lengthPerTask.length and readyCountPerTask.length)");
            }
            long sumLength = 0, sumCount = 0;
            for (int k = 0; k < lengthPerTask.length; k++) {
                long length = lengthPerTask[k];
                long count = readyCountPerTask[k];
                String perTask = lengthPerTask.length == 1 ? "" : "PerTask[" + k + "]";
                if (length < 0) {
                    throw new IllegalArgumentException("Negative length" + perTask);
                }
                if (count < 0) {
                    throw new IllegalArgumentException("Negative readyCount" + perTask);
                }
                if (count > length) {
                    throw new IllegalArgumentException("readyCount" +  perTask
                        + "=" + count + " is greater than length" + perTask + "=" + length);
                }
                sumLength += length;
                if (sumLength < 0) {
                    throw new IllegalArgumentException("Sum of all lengthPerTask is greater than Long.MAX_VALUE");
                }
                sumCount += count;
                assert sumCount >= 0; // because every count <= length
            }
            this.elementType = elementType;
            this.readyCountPerTask = readyCountPerTask.clone();
            this.readyCount = sumCount;
            this.lengthPerTask = lengthPerTask.clone();
            this.length = sumLength;
            this.numberOfParallelTasks = readyCountPerTask.length;
        }

        /**
         * Returns the type of elements in the source AlgART array, passed to
         * an array processing method alike
         * {@link Arrays.ParallelExecutor#process()}.
         * If the context of using cannot define a suitable type of array elements,
         * this method may return {@code null}.
         *
         * @return the type of elements in the source array.
         * @see Array#elementType()
         */
        public Class<?> elementType() {
            return this.elementType;
        }

        /**
         * Returns the total number of elements, that must be processed
         * per each of parallel tasks, which the algorithm is split into.
         * (So, the number of parallel tasks is equal to the length of the returned array.)
         * These elements are usually located in several ranges in the array,
         * some number of regions per one task.
         *
         * <p>The returned array is a clone of the internal array stored in this object.
         * The returned array is never empty (its length cannot be zero).
         *
         * @return the total number of elements, that must be processed
         *         per each of parallel tasks, which the algorithm is split into.
         */
        public long[] lengthPerTask() {
            return this.lengthPerTask.clone();
        }

        /**
         * Returns the total number of processed elements.
         * Always equal to the sum of all elements in the array, returned by
         * {@link #lengthPerTask()} method.
         *
         * <p>For {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method,
         * returns <code>min(dest.{@link Array#length() length()}, src.{@link Array#length() length()})</code>,
         * where <code>dest</code> and <code>src</code> are the AlgART arrays passed this method.
         * For {@link Arrays.ParallelExecutor#process()} method and another methods based on it,
         * return <code>src.{@link Array#length()}</code>,
         * where <code>src</code> is the processed AlgART array.
         *
         * @return the total number of processing elements.
         */
        public long length() {
            return this.length;
        }

        /**
         * Returns the number of the elements that were already processed until this moment
         * per each of parallel tasks, which the algorithm is split into.
         * (So, the number of parallel tasks is equal to the length of the returned array.)
         * These elements are usually located in several ranges in the array,
         * some number of regions per one task.
         *
         * <p>The returned array is a clone of the internal array stored in this object.
         * The returned array is never empty (its length cannot be zero).
         *
         * @return the numbers of the elements that were already processed until this moment
         *         per each of parallel tasks, which the algorithm is split into.
         */
        public long[] readyCountPerTask() {
            return this.readyCountPerTask.clone();
        }

        /**
         * Returns the number of elements that was already processed until this moment.
         * Always equal to the sum of all elements in the array, returned by
         * {@link #readyCountPerTask()} method.
         *
         * @return the number of elements that was already processed until this moment.
         */
        public long readyCount() {
            return this.readyCount;
        }

        /**
         * Returns the number of parallel tasks, which the algorithm is split into.
         * Always equal to the number of elements in the arrays, returned by
         * {@link #lengthPerTask()} and {@link #readyCountPerTask()} method.
         *
         * @return the number of parallel tasks that are executed simultaneously in several threads.
         * @see Arrays#copy(ArrayContext, UpdatableArray, Array)
         * @see Arrays.ParallelExecutor
         */
        public int numberOfParallelTasks() {
            return this.numberOfParallelTasks;
        }

        /**
         * Returns the ready part of total algorithm execution.
         * In simple cases, returns <code>(double){@link #readyCount()}/(double){@link #length()}</code>.
         * Returns <code>1.0</code> if and only if all calculations are finished.
         * Also returns <code>1.0</code> if {@link #length()} is zero.
         *
         * @return the ready part of total algorithm execution (from <code>0.0</code> to <code>1.0</code>).
         * @see ArrayContext#part(double, double)
         */
        public double readyPart() {
            return this.length == 0 || this.readyCount == this.length ? 1.0 :
                (double)this.readyCount / (double)this.length;
            // the 2nd check is to be on the safe side: we must return precise 1.0 value
        }
    }
}
