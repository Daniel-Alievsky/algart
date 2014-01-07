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

package net.algart.arrays;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>A simple implementation of {@link ThreadPoolFactory} interface.
 * It uses a thread pool, created by <tt>Executors.newFixedThreadPool</tt> method,
 * and uses {@link Arrays.SystemSettings#cpuCount()} method to determine
 * the desired number of parallel tasks, if you did not specify another number in
 * <tt>numberOfTasks</tt> argument of the {@link #DefaultThreadPoolFactory constructor}
 * or {@link #getDefaultThreadPoolFactory(int)} method.
 * See details below in comments to the methods and fields.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class DefaultThreadPoolFactory extends AbstractThreadPoolFactory implements ThreadPoolFactory {
    private static final long MIN_MULTITHREADING_LENGTH = InternalUtils.getLongProperty(
        "net.algart.arrays.minMultithreadingLength", 16);

    private static final int GLOBAL_THREAD_POOLS_PER_CPU = Math.min(128, Math.max(1, InternalUtils.getIntProperty(
        "net.algart.arrays.globalThreadPoolsPerCPU", 2)));

    private static final int GLOBAL_THREAD_POOL_SIZE = Math.min(8192, InternalUtils.getIntProperty(
        "net.algart.arrays.globalThreadPoolSize",
        1 + GLOBAL_THREAD_POOLS_PER_CPU * InternalUtils.availableProcessors()));

    private static final int GLOBAL_THREAD_POOL_KEEP_ALIVE_TIME = Math.max(0, InternalUtils.getIntProperty(
        "net.algart.arrays.globalThreadPoolKeepAliveTime", 30000)); // 30 sec keep-alive for core threads

    /**
     * Returns the global thread pool, returned by {@link #getThreadPool(Array, ThreadFactory)} method
     * in factories, created by {@link #getDefaultThreadPoolFactory()}
     * and {@link #getDefaultThreadPoolFactory(int)} methods.
     *
     * <p>More precisely, if there is the system property "<tt>net.algart.arrays.globalThreadPoolSize</tt>",
     * containing a positive integer value <tt>N</tt>, this method returns a global thread pool,
     * created by <tt>Executors.newFixedThreadPool(N,...<i>(someOurFactory)</i>)</tt> (or analogous) operator
     * while the first call of this method and saved in an internal static field.
     * If this property contains an integer greater than 8192, this value is truncated to 8192.
     * If this property contains 0 or a negative value, this method returns <tt>null</tt>
     * (in this case, no global thread pool will be created).
     * If there is no such property, or if it contains not a number,
     * or if some exception occurred while calling <tt>Integer.getInteger</tt>,
     * this method returns the default value, which is equal (in the current implementation) to
     * <tt><nobr>{@link Arrays.SystemSettings#availableProcessors()}*MULT+1</nobr></tt>,
     * where MULT is an integer value, stored in "<tt>net.algart.arrays.globalThreadPoolsPerCPU</tt>"
     * system property, or default multiplier 2 if there is no such property or it contains not a number.
     * Default value MULT=2 provides a suitable choice for most multiprocessor configurations.
     * The value of these system properties are loaded and checked only once
     * while initializing {@link DefaultThreadPoolFactory} class.
     *
     * <p>Note: the default value <tt><nobr{@link Arrays.SystemSettings#availableProcessors()}*MULT+1</nobr></tt>
     * can be changed in future implementations. It is only guaranteed that this value is chosen
     * not less than <tt><nobr>{@link Arrays.SystemSettings#availableProcessors()}</nobr></tt>.
     *
     * <p>Note: the threads, created in the global thread pool (if it exists), are <i>daemons</i>.
     * So, the application can be terminated by the usual way, even
     * if the global thread pool contains some working threads.
     *
     * @return the global thread pool, usually returned by this factory, if it exists, or <tt>null</tt> in other case.
     */
    public static ThreadPoolExecutor globalThreadPool() {
        return ConstantHolder.GLOBAL_THREAD_POOL;
    }

    private final int numberOfTasks;
    private final ThreadPoolExecutor persistentThreadPool;

    /**
     * Returns an instance of this class with the default (simplest) behaviour.
     * Namely, it is equivalent to {@link #getDefaultThreadPoolFactory(int)
     * getDefaultThreadPoolFactory(0)}. This method is the typical way for getting instances of this class.
     *
     * <p>Note: this method works very quickly (it just returns a global internal constant).
     *
     * @return an instance of this class with the default behaviour.
     */
    public static DefaultThreadPoolFactory getDefaultThreadPoolFactory() {
        return ConstantHolder.DEFAULT;
    }

    /**
     * Returns an instance of this class with the specified recommended number of tasks.
     *
     * <p>If <tt>numberOfTasks</tt> argument is positive,
     * it will be always returned by {@link #recommendedNumberOfTasks()} and
     * {@link #recommendedNumberOfTasks(Array)} methods.
     * If it is zero, that method will use the common algorithm, based on the system property:
     * see comments to {@link #recommendedNumberOfTasks()}.
     *
     * <p>This method is equivalent to <tt>new&nbsp;{@link #DefaultThreadPoolFactory(int, ThreadPoolExecutor)
     * DefaultThreadPoolFactory}(numberOfTasks, {@link #globalThreadPool()})</tt>.
     *
     * <p>Note: this method works very quickly (it just returns global internal constants) in cases
     * <tt>numberOfTasks=0</tt> and <tt>numberOfTasks=1</tt>.
     *
     * @param numberOfTasks the desired number of tasks.
     * @return an instance of this class with the specified recommended number of tasks.
     * @throws IllegalArgumentException if <tt>numberOfTasks</tt> is negative.
     */
    public static DefaultThreadPoolFactory getDefaultThreadPoolFactory(int numberOfTasks) {
        if (numberOfTasks == 0) {
            return ConstantHolder.DEFAULT;
        }
        if (numberOfTasks == 1) {
            return ConstantHolder.DEFAULT_SINGLE_THREAD;
        }
        return new DefaultThreadPoolFactory(numberOfTasks, ConstantHolder.GLOBAL_THREAD_POOL);
    }

    /**
     * Creates new instance of this class with the specified recommended number of tasks
     * and the specified thread pool.
     *
     * <p>If <tt>numberOfTasks</tt> argument is positive,
     * it will be always returned by {@link #recommendedNumberOfTasks()} and
     * {@link #recommendedNumberOfTasks(Array)} methods.
     * If it is zero, that method will use the common algorithm, based on the system property:
     * see comments to {@link #recommendedNumberOfTasks()}.
     *
     * <p>If <tt>persistentThreadPool</tt> is not <tt>null</tt>,
     * it will be always returned by {@link #getThreadPool(Array, ThreadFactory)} method and
     * {@link #releaseThreadPool(ExecutorService)} method will do nothing.
     * In this case, please note, that if the threads, created by this pool, are not daemons,
     * then the application will probably not be exited until you will directly call <tt>shutdown()</tt>
     * method for the passed <tt>persistentThreadPool</tt>.
     *
     * <p>If <tt>persistentThreadPool==null</tt>, {@link #getThreadPool(Array, ThreadFactory)} method
     * will always create new thread pool and
     * {@link #releaseThreadPool(ExecutorService)} method will shutdown the passed pool.
     *
     * @param numberOfTasks        the desired number of tasks.
     * @param persistentThreadPool the desired thread pool,
     *                             or <tt>null</tt> if {@link #getThreadPool(Array, ThreadFactory)}
     *                             should create new thread pool every time.
     * @throws IllegalArgumentException if <tt>numberOfTasks</tt> is negative.
     */
    public DefaultThreadPoolFactory(int numberOfTasks, ThreadPoolExecutor persistentThreadPool) {
        if (numberOfTasks < 0)
            throw new IllegalArgumentException("Negative numberOfTasks=" + numberOfTasks);
        this.numberOfTasks = numberOfTasks;
        this.persistentThreadPool = persistentThreadPool;
    }

    /**
     * If this instance was created by the {@link #DefaultThreadPoolFactory constructor}
     * with <tt>numberOfTasks=0</tt> argument
     * (or via {@link #getDefaultThreadPoolFactory()} method), this implementation returns
     * the result of {@link Arrays.SystemSettings#cpuCount()} method.
     *
     * <p>If this instance was created by the {@link #DefaultThreadPoolFactory constructor}
     * with non-zero <tt>numberOfTasks</tt> argument
     * (or via <nobr>{@link #getDefaultThreadPoolFactory(int numberOfTask)}</nobr> method
     * with <tt>numberOfTasks&gt;0</tt> method),
     * the given <tt>numberOfTasks</tt> argument is returned always
     * regardless of any system properties.
     *
     * <p>If this instance uses persistent thread pool, that is if
     * <nobr><tt>{@link #persistentThreadPool()}!=null</tt></nobr> (a typical situation),
     * then the result of this method, calculated by the rules above,
     * is also truncated by the limit
     * <tt>Math.max(1, <nobr>((ThreadPoolExecutor){@link #persistentThreadPool()}).getCorePoolSize())</nobr></tt>
     * &mdash; if the previously calculated result is greater then this limit, then this limit is returned instead.
     * So, there is a guarantee, that this method never returns a value greater than
     * the core number of threads in a thread pool, returned by
     * {@link #getThreadPool(ThreadFactory)} and {@link #getThreadPool(Array, ThreadFactory)} methods.
     * It is important if you are going to run threads which can depend on each other.
     *
     * @return the recommended number of parallel tasks to perform the processing.
     * @see #recommendedNumberOfTasks(Array)
     */
    public int recommendedNumberOfTasks() {
        int nt = numberOfTasks != 0 ? numberOfTasks : Arrays.SystemSettings.cpuCount();
        if (nt > 1 && persistentThreadPool != null) {
            int corePoolSize = Math.max(1, persistentThreadPool.getCorePoolSize());
            if (corePoolSize < nt) {
                nt = corePoolSize;
            }
        }
        return nt;
    }

    /**
     * This method returns the same value as {@link #recommendedNumberOfTasks()}, excepting that
     * when the passed array is very little, this method may return less value (usually <tt>1</tt>).
     *
     * <p>However, if this instance was created by the {@link #DefaultThreadPoolFactory constructor}
     * with non-zero <tt>numberOfTasks</tt> argument
     * (or via <nobr>{@link #getDefaultThreadPoolFactory(int numberOfTask)}</nobr> method
     * with <tt>numberOfTasks&gt;0</tt> method),
     * then this method is strictly equivalent to {@link #recommendedNumberOfTasks()}.
     *
     * @param sourceArray some AlgART array that should be processed.
     * @return            the recommended number of parallel tasks to perform the processing.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public int recommendedNumberOfTasks(Array sourceArray) {
        if (sourceArray == null)
            throw new NullPointerException("Null sourceArray argument");
        if (numberOfTasks != 0) {
            return recommendedNumberOfTasks();
        }
        long len = sourceArray.length();
        if (len < MIN_MULTITHREADING_LENGTH) {
            return 1;
        } else {
            return (int)Math.min(len, recommendedNumberOfTasks());
            // no sense to create more threads than the number of elements
        }
    }

    public ThreadPoolFactory singleThreadVersion() {
        if (this.numberOfTasks == 1) {
            return this;
        }
        return new DefaultThreadPoolFactory(1, persistentThreadPool);
    }

    /**
     * This implementation returns the {@link #persistentThreadPool() persistent thread pool},
     * if it exists, or creates new thread pool by the following call:
     * <tt>Executors.newFixedThreadPool({@link #recommendedNumberOfTasks()},
     * threadFactory==null ? Executors.defaultThreadFactory() : threadFactory)</tt>.
     *
     * @param threadFactory if not <tt>null</tt> and there is no
     *                      {@link #persistentThreadPool() persistent thread pool},
     *                      specifies the desired thread factory for using by new thread pool.
     * @return              the thread pool for parallel processing the array.
     * @throws NullPointerException if <tt>sourceArray</tt> argument is <tt>null</tt>.
     * @see #getThreadPool(Array, ThreadFactory)
     */
    public ExecutorService getThreadPool(ThreadFactory threadFactory) {
        if (persistentThreadPool != null) {
            return persistentThreadPool;
        } else {
            return Executors.newFixedThreadPool(recommendedNumberOfTasks(),
                threadFactory == null ? Executors.defaultThreadFactory() : threadFactory);
        }
    }

    /**
     * This implementation returns the {@link #persistentThreadPool() persistent thread pool},
     * if it exists, or creates new thread pool by the following call:
     * <tt>Executors.newFixedThreadPool({@link #recommendedNumberOfTasks(Array)
     * recommendedNumberOfTasks}(sourceArray),
     * threadFactory==null ? Executors.defaultThreadFactory() : threadFactory)</tt>.
     *
     * @param sourceArray   some AlgART array that should be processed.
     * @param threadFactory if not <tt>null</tt> and there is no
     *                      {@link #persistentThreadPool() persistent thread pool},
     *                      specifies the desired thread factory for using by new thread pool.
     * @return              the thread pool for parallel processing the array.
     * @throws NullPointerException if <tt>sourceArray</tt> argument is <tt>null</tt>.
     */
    public ExecutorService getThreadPool(Array sourceArray, ThreadFactory threadFactory) {
        if (sourceArray == null)
            throw new NullPointerException("Null sourceArray argument");
        if (persistentThreadPool != null) {
            return persistentThreadPool;
        } else {
            return Executors.newFixedThreadPool(recommendedNumberOfTasks(sourceArray),
                threadFactory == null ? Executors.defaultThreadFactory() : threadFactory);
        }
    }

    /**
     * This implementation calls <tt>pool.shutdown()</tt>, if there is no persistent thread pool
     * ({@link #persistentThreadPool()} returns <tt>null</tt>), or does nothing in other case.
     *
     * @param pool the thread pool created by the previous {@link #getThreadPool(Array, ThreadFactory)} call.
     * @throws NullPointerException if <tt>poll</tt> argument is <tt>null</tt> and there is no persistent thread pool.
     */
    public void releaseThreadPool(ExecutorService pool) {
        if (persistentThreadPool == null) {
            pool.shutdown();
        }
    }

    /**
     * Returns the persistent thread pool,
     * returned by all calls of {@link #getThreadPool(Array, ThreadFactory)} method,
     * if it exists, or <tt>null</tt> in other case.
     * (In the second case, every call of {@link #getThreadPool(Array, ThreadFactory)} method
     * creates new thread pool.)
     *
     * <p>More precisely, if this instance was created
     * by the {@link #DefaultThreadPoolFactory(int, ThreadPoolExecutor) constructor},
     * this method returns its 2nd argument.
     * If this instance was created
     * by {@link #getDefaultThreadPoolFactory()}
     * or {@link #getDefaultThreadPoolFactory(int)},
     * this method returns the result of {@link #globalThreadPool()}.
     *
     * @return the persistent thread pool and <tt>null</tt> if it exists, or <tt>null</tt> in other case.
     */
    public final ExecutorService persistentThreadPool() {
        return persistentThreadPool;
    }

    private static class ConstantHolder {
        private static final ThreadPoolExecutor GLOBAL_THREAD_POOL = GLOBAL_THREAD_POOL_SIZE <= 0 ? null :
            new ThreadPoolExecutor(GLOBAL_THREAD_POOL_SIZE, GLOBAL_THREAD_POOL_SIZE,
                GLOBAL_THREAD_POOL_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "AlgART-arrays-thread-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                });
        static {
            if (GLOBAL_THREAD_POOL != null && GLOBAL_THREAD_POOL_KEEP_ALIVE_TIME > 0) {
                try {
                    Method method = GLOBAL_THREAD_POOL.getClass().getMethod("allowCoreThreadTimeOut", boolean.class);
                    method.invoke(GLOBAL_THREAD_POOL, true);
                } catch (NoSuchMethodException e) {
                    // System.out.println("Probably Java 1.5");
                } catch (InvocationTargetException e) {
                    e.printStackTrace(); // strange situation
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); // strange situation
                }
            }
        }
        private static final DefaultThreadPoolFactory DEFAULT =
            new DefaultThreadPoolFactory(0, GLOBAL_THREAD_POOL);
        private static final DefaultThreadPoolFactory DEFAULT_SINGLE_THREAD =
            new DefaultThreadPoolFactory(1, GLOBAL_THREAD_POOL);
    }
}
