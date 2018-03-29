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

package net.algart.arrays;

/**
 * <p>The context of processing AlgART arrays. It is used by
 * {@link Arrays.ParallelExecutor} class,
 * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method
 * and some other methods of this and other packages, for example,
 * {@link Arrays#rangeOf(ArrayContext, PArray)},
 * {@link Arrays#sumOf(ArrayContext, PArray)},
 * {@link Arrays#preciseSumOf(ArrayContext, PFixedArray)}.</p>
 *
 * <p>We recommend to use <tt>net.algart.contexts.DefaultArrayContext</tt>
 * as an object implementing this interface.</p>
 *
 * <p>Objects implementing this interface are usually <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayContext {

    /**
     * The simplest implementation of {@link ArrayContext}, that does almost nothing.
     * More precisely, in this object:
     *
     * <ul>
     * <li>{@link #getMemoryModel()} method returns {@link SimpleMemoryModel#getInstance()};</li>
     * <li>{@link #getThreadPoolFactory()} method returns
     * <tt>new&nbsp;{@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory()}</tt>;</li>
     * <li>{@link #checkInterruption()} method does nothing;</li>
     * <li>{@link #updateProgress(Event)} method does nothing;</li>
     * <li>{@link #currentThreadIndex()} method returns <tt>0</tt>;</li>
     * <li>{@link #numberOfThreads()} method returns <tt>1</tt>;</li>
     * <li>{@link #customData()} method returns <tt>null</tt>.</li>
     * </ul>
     *
     * <p>In most methods, that use {@link ArrayContext} argument, you can specify <tt>null</tt> as context
     * with the same effect as passing this object.
     */
    public static final ArrayContext DEFAULT = new AbstractArrayContext.Default();

    /**
     * The simplest single-thread implementation of {@link ArrayContext}, that does almost nothing.
     * More precisely, in this object:
     *
     * <ul>
     * <li>{@link #getMemoryModel()} method returns {@link SimpleMemoryModel#getInstance()};</li>
     * <li>{@link #getThreadPoolFactory()} method returns
     * <tt>new&nbsp;{@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory(int)
     * DefaultThreadPoolFactory.getDefaultThreadPoolFactory(1)}</tt>;</li>
     * <li>{@link #checkInterruption()} method does nothing;</li>
     * <li>{@link #updateProgress(Event)} method does nothing;</li>
     * <li>{@link #currentThreadIndex()} method returns <tt>0</tt>;</li>
     * <li>{@link #numberOfThreads()} method returns <tt>1</tt>;</li>
     * <li>{@link #customData()} method returns <tt>null</tt>.</li>
     * </ul>
     *
     * <p>This constant is useful, if you need some "default" single-thread context.
     * If you specify <tt>null</tt> as a context (as well as {@link #DEFAULT} object),
     * it leads to using {@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory()}</tt>,
     * which tries to use all available processor kernels.
     * To get analogous behaviour without multithreading, you should use this object.</tt>.
     *
     * <p>Note: this object is also returned by
     * <tt>ArrayContext.{@link #DEFAULT}.{@link #singleThreadVersion() singleThreadVersion()}</tt> call.
     */
    public static final ArrayContext DEFAULT_SINGLE_THREAD = new AbstractArrayContext.DefaultSingleThread();

    /**
     * Returns new context, describing the execution of some subtask of the current task,
     * from <tt>fromPart*100%</tt> of total execution until <tt>toPart*100%</tt> of total execution.
     * The returned context works alike the current context with the only exception, that
     * its {@link #updateProgress updateProgress} method passes to this (parent) context little
     * corrected event. Namely, its {@link ArrayContext.Event#readyPart()} method of the passed event
     * returns <tt>fromPart+event.readyPart()*(toPart-fromPart)</tt>.
     * The methods {@link #getMemoryModel()}, {@link #getThreadPoolFactory()}, {@link #checkInterruption()}
     * of the returned instance call the same methods of this one.
     *
     * <p>Below is an example of the situation when this method is necessary.
     * Let we have 3 methods in some class:
     * <tt>fullTask({@link ArrayContext})</tt>,
     * <tt>subTask1({@link ArrayContext})</tt>,
     * <tt>subTask2({@link ArrayContext})</tt>.
     * The only function of <tt>fullTask</tt> method is sequential call of <tt>subTask1</tt> and <tt>subTask2</tt>.
     * We can implement it in the following way:</p>
     *
     * <pre>
     * public void fullTask({@link ArrayContext} context) {
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
     * first time in <tt>subTask1</tt> method, second time in <tt>subTask2</tt> method.</p>
     *
     * <p>This class provides solutions of this problem. Namely:</p>
     *
     * <pre>
     * public void fullTask({@link ArrayContext} context) {
     * &#32;   subTask1(context.part(0.0, 0.5));
     * &#32;   subTask2(context.part(0.5, 1.0));
     * }
     * </pre>
     *
     * <p>Now the execution of <tt>subTask1</tt> method will change percents from 0% to 50%
     * and the execution of <tt>subTask2</tt> method will change percents from 50% to 100%.</p>
     *
     * <p>In many case the overloaded method {@link #part(long, long, long)} is more convenient.
     *
     * @param fromPart the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the start of the subtask:
     *                 see {@link #updateProgress updateProgress} method
     * @param toPart   the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the finish of the subtask:
     *                 see {@link #updateProgress updateProgress} method;
     *                 must be not less than <tt>fromPart</tt> range.
     * @return         new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <tt>fromPart</tt> or <tt>toPart</tt> is not in
     *                                  <tt>0.0..1.0</tt> range or if <tt>fromPart&gt;toPart</tt>.
     * @see #part(long, long, long)
     * @see #noProgressVersion()
     */
    public ArrayContext part(double fromPart, double toPart);

    /**
     * Returns new context, describing the execution of some subtask of the current task,
     * from <tt>from/total*100%</tt> of total execution until <tt>to/total*100%</tt> of total execution.
     * More precisely, equivalent to the following call:
     * <pre>
     * {@link #part(double, double) part}((double)from/(double)total, to==total ? 1.0: (double)to/(double)total)
     * </pre>
     * excepting the case <tt>from=to=total=0</tt>, when it is equivalent to
     * <pre>
     * {@link #part(double, double) part}(0.0, 1.0)
     * </pre>
     *
     * @param from  the estimated ready part, from 0 to <tt>total</tt>,
     *              of the total algorithm at the start of the subtask.
     * @param to    the estimated ready part, from 0.0 to <tt>total</tt>,
     *              of the total algorithm at the finish of the subtask.
     * @param total the number of some operation in the full task.
     * @return      new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <tt>from</tt> or <tt>to</tt> is not in <tt>0..total</tt> range,
     *                                  or if <tt>from&gt;to</tt>, or if <tt>total&lt;0</tt>.
     * @see #part(double, double)
     * @see #noProgressVersion()
     */
    public ArrayContext part(long from, long to, long total);

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
    public ArrayContext noProgressVersion();

    /**
     * Returns new context, identical to this one with the only exception that the thread pool factory,
     * returned by {@link #getThreadPoolFactory()} method, returns
     * <tt>thisInstance.{@link #getThreadPoolFactory()
     * getThreadPoolFactory()}.{@link ThreadPoolFactory#singleThreadVersion() singleThreadVersion()}</tt>.
     *
     * @return the single-thread version of this context.
     */
    public ArrayContext singleThreadVersion();

    /**
     * Returns new context, identical to this one with the only exception that
     * {@link #currentThreadIndex()} and {@link #numberOfThreads()} methods in the result return
     * the values, specified in the arguments <tt>currentThreadIndex</tt> and <tt>numberOfThreads</tt>.
     *
     * <p>You can use this method, if you need to inform some algorithm, having own context
     * (like {@link ArrayProcessor}), about the number of threads, which will simultaneously execute
     * the same algorithm, and the index of thread, which really performs this algorithm.
     * It can be necessary, for example, if an algorithm uses some external work memory,
     * which must be unique in every thread.
     *
     * <p>Note: this method is often used together with {@link #singleThreadVersion()}, for example:<br>
     * <tt>&nbsp;&nbsp;&nbsp;&nbsp;arrayContext.{@link #singleThreadVersion()
     * singleThreadVersion()}.multithreadedVersion(k,n)</tt>.<br>
     * Really, there is usually no sense to allow using multithreading (creating thread pools
     * by {@link ThreadPoolFactory} with more than 1 thread) in a thread, which is already called
     * in a multithreading environment simultaneously with other threads.
     *
     * @param currentThreadIndex an index of the thread, which should be executed simultaneously with another threads;
     *                           must be <tt>&ge;0</tt> and <tt>&lt;numberOfThreads</tt>.
     * @param numberOfThreads    the number of threads in a group of parallel threads; must be positive.
     * @return                   the version of this context, considered to be used in the thread
     *                           <tt>#currentThreadIndex</tt> in a group of <tt>numberOfThreads</tt> parallel threads.
     * @throws IllegalArgumentException if <tt>numberOfThreads&le;0</tt> or if <tt>currentThreadIndex</tt>
     *                                  does not lie in <nobr><tt>0..numberOfThreads-1</tt></nobr> range.
     */
    public ArrayContext multithreadedVersion(int currentThreadIndex, int numberOfThreads);

    /**
     * Returns new context, identical to this one with the only exception that
     * {@link #customData()} method in the result returns
     * the value, specified in the argument <tt>customData</tt>.
     *
     * <p>You can use this method, if you need to inform some algorithm, having own context
     * (like {@link ArrayProcessor}), about some additional details of the current execution process,
     * which is not provided by other methods of this interface.
     * For example: let's have some algorithm, processing some AlgART array,
     * in a form of an abstract class or interface (implementing/extending {@link ArrayProcessorWithContextSwitching})
     * with the method<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<tt>process({@link Array} arrayToProcess).</tt><br>
     * Then, let's have a special application, where this algorithm is applied to a sequence of
     * {@link Array#subArray(long, long) subarrays} of another large array.
     * It is very probable that your <tt>process</tt> method does not need to know, which subarray
     * (from which position) is processed now, but if you still need this (maybe for debugging),
     * you can pass this information via {@link ArrayProcessorWithContextSwitching#context(ArrayContext) switching
     * the context} to the result of this method with an appropriate <tt>customData</tt> object.
     *
     * @param customData some custom data; <tt>null</tt> is an allowed value.
     * @return           the version of this context, where {@link #customData()} method returns the reference
     *                   to <tt>customData</tt> argument.
     */
    public ArrayContext customDataVersion(Object customData);

    /**
     * Returns the {@link MemoryModel memory model} that should be used
     * for creating any instances of AlgART arrays.
     * This method never returns <tt>null</tt>.
     *
     * @return the desired memory model.
     */
    public MemoryModel getMemoryModel();

    /**
     * Returns the {@link ThreadPoolFactory thread pool factory} that should be used for
     * planning parallel execution in multithread methods alike
     * {@link Arrays.ParallelExecutor#process()}.
     * This method never returns <tt>null</tt>.
     *
     * @return the desired thread pool factory.
     */
    public ThreadPoolFactory getThreadPoolFactory();

    /**
     * This method is called periodically by long-working methods alike
     * {@link Arrays.ParallelExecutor#process()}.
     * If this method throws some <tt>RuntimeException</tt>, the execution of all running threads
     * is stopped and this exception is re-thrown.
     * You may override this method to allow the user to interrupt the algorithm.
     * Please note that this method may be called from several threads;
     * so, you need to synchronize access to the returned flag.
     *
     * @throws RuntimeException (or some its subclass) if the application has requested to interrupt
     *                          the currently executing module. This exception will lead to stopping
     *                          all threads, started by multithread method alike
     *                          {@link Arrays#copy(ArrayContext, UpdatableArray, Array) copy},
     *                          and will be re-thrown by that method.
     */
    public void checkInterruption() throws RuntimeException;

    /**
     * This method is called periodically by long-working methods alike
     * {@link Arrays.ParallelExecutor#process()}
     * with the argument, containing information about the execution progress.
     * You may override this method to show the progress to the end user.
     *
     * <p>This method may skip updating the corresponding visual element,
     * if it is called too often, to avoid slowing down the program.
     * However, it <i>should</i> update the visual element if {@link ArrayContext.Event#readyPart()}
     * method returns <tt>1.0</tt>.
     *
     * @param event information about the execution progress.
     */
    public void updateProgress(Event event);

    /**
     * Equivalent to 2 sequential calls: <nobr><tt>{@link #checkInterruption()}</tt></nobr> and
     * <nobr><tt>{@link #updateProgress
     * updateProgress}(new ArrayContext.Event(elementType, readyCount, length))</tt></nobr>.
     *
     * @param elementType           the result of {@link ArrayContext.Event#elementType()} method in the event;
     *                              may be <tt>null</tt>.
     * @param readyCount            the only element in the result of {@link ArrayContext.Event#readyCountPerTask()}
     *                              method in the event.
     * @param length                the result of {@link ArrayContext.Event#length()} method in the created event.
     */
    public void checkInterruptionAndUpdateProgress(Class<?> elementType, long readyCount, long length);

    /**
     * Usually returns <tt>0</tt>, but in multithreading environment this method may return the index
     * of the currently executing thread in a group of {@link #numberOfThreads()} parallel threads.
     * This information can be useful, if you create a group of tasks (for example, with help of
     * {@link ThreadPoolFactory} class), which are executed parallel in several threads
     * (usually, to optimize calculations on multiprocessor or multi-core computers).
     *
     * <p>To create a context, in which this method returns a value different than <tt>0</tt>, please use
     * <nobr>{@link #multithreadedVersion(int currentThreadIndex, int numberOfThreads)}</nobr> method.
     *
     * <p>The result of this method always lies in <tt>0..{@link #numberOfThreads() numberOfThreads()}-1</tt> range.
     *
     * <p>In {@link AbstractArrayContext} implementation, this method returns <tt>0</tt>.
     *
     * @return the index of the currently executing thread in a group of {@link #numberOfThreads()} parallel threads,
     *         or <tt>0</tt> if this feature is not used.
     */
    public int currentThreadIndex();

    /**
     * Usually returns <tt>1</tt>, but in multithreading environment this method <i>may</i> return the number
     * of currently executing threads in some group of parallel threads (but not must: in most contexts
     * it still returns 1 even in multithreading environment).
     * This value is not the number of <i>all</i> executing threads and not the number of threads, which are
     * <i>really</i> executing at this moment &mdash; it is the number of elements in some group of tasks
     * (for example, created by {@link ThreadPoolFactory} class), which should be executed parallel in several threads
     * (usually, to optimize calculations on multiprocessor or multi-core computers) and which should be distinguished
     * via {@link #currentThreadIndex()} method. The main goal of this method is to estimate
     * the maximal possible value of {@link #currentThreadIndex()}.
     *
     * <p>To create a context, in which this method returns a value different than <tt>1</tt>, please use
     * <nobr>{@link #multithreadedVersion(int currentThreadIndex, int numberOfThreads)}</nobr> method.
     *
     * <p>The result of this method is always positive (<tt>&ge;1</tt>).
     *
     * <p>In {@link AbstractArrayContext} implementation, this method returns <tt>1</tt>.
     *
     * @return the number of executing threads in a group of parallel threads,
     *         or <tt>1</tt> if this feature is not used.
     */
    public int numberOfThreads();

    /**
     * Usually returns <tt>null</tt>, but in a special environment this method may return some custom object,
     * containing additional information about the current execution context,
     * not provided by other methods of this class.
     *
     * <p>To create a context, in which this method returns a value different than <tt>null</tt>, please use
     * <nobr>{@link #customDataVersion(Object)}</nobr> method.
     *
     * <p>The result of this method may belong to any type. So, it is a good idea to check its type
     * with <tt>instanceof</tt> operator before attempt to use this result.
     *
     * <p>In {@link AbstractArrayContext} implementation, this method returns <tt>null</tt>.
     *
     * @return some custom information about the current execution environment.
     */
    public Object customData();

    /**
     * The array processing event: an argument of {@link ArrayContext#updateProgress(Event)} method.
     */
    public static class Event {
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
         * <tt>(double)readyCount/(double)length</tt>.
         * Equivalent to <tt>new&nbsp;Event(elementType, new long[] {length}, new long[] {readyCount})</tt>.
         *
         * @param elementType           the result of {@link #elementType()} method in the created event;
         *                              may be <tt>null</tt>.
         * @param readyCount            the only element in the result of {@link #readyCountPerTask()} method
         *                              in the created event.
         * @param length                the result of {@link #length()} method in the created event.
         * @throws IllegalArgumentException if <tt>length</tt> or <tt>readyCount</tt> is negative,
         *                                  or if <tt>readyCount&gt;length</tt>.
         */
        public Event(Class<?> elementType, long readyCount, long length) {
            this(elementType, new long[] {readyCount}, new long[] {length});
        }

        /**
         * Creates new event with the specified {@link #elementType()}, {@link #lengthPerTask()}
         * and {@link #readyCountPerTask()}.
         * The {@link #length()} method in the created instance returns
         * the sum of all elements of <tt>lengthPerTask</tt> argument.
         * The {@link #readyCount()} method returns
         * the sum of all elements of <tt>readyCountPerTask</tt> argument.
         * The {@link #numberOfParallelTasks()} method returns
         * the length of each of the passed arrays (their lengths must be equal).
         * The {@link #readyPart()} method returns
         * <tt>(double){@link #readyCount()}/(double){@link #length()}</tt>.
         *
         * <p>All elements of the <tt>readyCountPerTask</tt> array must not be greater than
         * the corresponding elements of the <tt>lengthsPerTask</tt> array.
         * All elements of these arrays must not be negative.
         *
         * <p>The passed <tt>lengthPerTask</tt> and <tt>readyCountPerTask</tt> arguments
         * are cloned by this method: no references to them are maintained by the created instance.
         *
         * @param elementType           the result of {@link #elementType()} method in the created event;
         *                              may be <tt>null</tt>.
         * @param readyCountPerTask     the result of {@link #readyCountPerTask()} method in the created event.
         * @param lengthPerTask         the result of {@link #lengthPerTask()} method in the created event.
         * @throws NullPointerException     if <tt>lengthPerTask</tt> or <tt>readyCountPerTask</tt> is <tt>null</tt>.
         * @throws IllegalArgumentException if <tt>lengthPerTask</tt> or <tt>readyCountPerTask</tt> is an empty array,
         *                                  or if their lengths are not equal,
         *                                  or if some their elements are negative,
         *                                  or if <tt>readyCountPerTask[k]</tt> is than <tt>lengthPerTask[k]</tt>
         *                                  for some index <tt>k</tt>,
         *                                  or if the sum of all elements of one of these arrays is
         *                                  greater than <tt>Long.MAX_VALUE</tt>.
         */
        public Event(Class<?> elementType, long[] readyCountPerTask, long[] lengthPerTask) {
            if (readyCountPerTask == null)
                throw new NullPointerException("Null readyCountPerTask argument");
            if (lengthPerTask == null)
                throw new NullPointerException("Null lengthPerTask argument");
            if (lengthPerTask.length != readyCountPerTask.length)
                throw new IllegalArgumentException("lengthPerTask and readyCountPerTask have different lengths");
            if (lengthPerTask.length == 0)
                throw new IllegalArgumentException("Zero number of tasks "
                    + "(lengthPerTask.length and readyCountPerTask.length)");
            long sumLength = 0, sumCount = 0;
            for (int k = 0; k < lengthPerTask.length; k++) {
                long length = lengthPerTask[k];
                long count = readyCountPerTask[k];
                String perTask = lengthPerTask.length == 1 ? "" : "PerTask[" + k + "]";
                if (length < 0)
                    throw new IllegalArgumentException("Negative length" + perTask);
                if (count < 0)
                    throw new IllegalArgumentException("Negative readyCount" + perTask);
                if (count > length)
                    throw new IllegalArgumentException("readyCount" +  perTask
                        + "=" + count + " is greater than length" + perTask + "=" + length);
                sumLength += length;
                if (sumLength < 0)
                    throw new IllegalArgumentException("Sum of all lengthPerTask is greater than Long.MAX_VALUE");
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
         * this method may return <tt>null</tt>.
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
         * returns <tt>min(dest.{@link Array#length() length()}, src.{@link Array#length() length()})</tt>,
         * where <tt>dest</tt> and <tt>src</tt> are the AlgART arrays passed this method.
         * For {@link Arrays.ParallelExecutor#process()} method and another methods based on it,
         * return <tt>src.{@link Array#length()}</tt>,
         * where <tt>src</tt> is the processed AlgART array.
         *
         * @return the total number of processing elements.
         */
        public long length() {
            return this.length;
        }

        /**
         * Returns the number of elements, that was already processed until this moment
         * per each of parallel tasks, which the algorithm is split into.
         * (So, the number of parallel tasks is equal to the length of the returned array.)
         * These elements are usually located in several ranges in the array,
         * some number of regions per one task.
         *
         * <p>The returned array is a clone of the internal array stored in this object.
         * The returned array is never empty (its length cannot be zero).
         *
         * @return the number of elements, that was already processed until this moment
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
         * In simple cases, returns <tt>(double){@link #readyCount()}/(double){@link #length()}</tt>.
         * Returns <tt>1.0</tt> if and only if all calculations are finished.
         * Also returns <tt>1.0</tt> if {@link #length()} is zero.
         *
         * @return the ready part of total algorithm execution (from <tt>0.0</tt> to <tt>1.0</tt>).
         * @see ArrayContext#part(double, double)
         */
        public double readyPart() {
            return this.length == 0 || this.readyCount == this.length ? 1.0 :
                (double)this.readyCount / (double)this.length;
            // the 2nd check is to be on the safe side: we must return precise 1.0 value
        }
    }
}
