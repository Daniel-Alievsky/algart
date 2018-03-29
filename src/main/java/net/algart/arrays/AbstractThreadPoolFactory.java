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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * <p>A skeletal implementation of the {@link ThreadPoolFactory} interface.
 * Usually, you need to extend this class to implement that interface.</p>
 *
 * <p>All non-abstract methods are completely implemented here and usually
 * should not be overridden in subclasses.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractThreadPoolFactory implements ThreadPoolFactory {

    public void performTasks(Runnable[] tasks) {
        performTasksImpl(null, null, tasks, 0, Integer.MAX_VALUE);
    }

    public void performTasks(ThreadFactory threadFactory, Runnable[] tasks) {
        performTasksImpl(null, threadFactory, tasks, 0, Integer.MAX_VALUE);
    }

    public void performTasks(Array sourceArray, ThreadFactory threadFactory, Runnable[] tasks) {
        if (sourceArray == null)
            throw new NullPointerException("Null sourceArray argument");
        performTasksImpl(sourceArray, threadFactory, tasks, 0, Integer.MAX_VALUE);
    }

    public void performTasks(Runnable[] tasks, int from, int to) {
        performTasksImpl(null, null, tasks, from, to);
    }

    public void performTasks(ThreadFactory threadFactory, Runnable[] tasks, int from, int to) {
        performTasksImpl(null, threadFactory, tasks, from, to);
    }

    private void performTasksImpl(Array sourceArray, ThreadFactory threadFactory, Runnable[] tasks, int from, int to) {
        if (tasks == null)
            throw new NullPointerException("Null tasks argument");
        if (from > to)
            throw new IllegalArgumentException("Illegal task indexes: initial index = " + from
                + " > end index = " + to);
        if (from < 0 || from > tasks.length)
            throw new ArrayIndexOutOfBoundsException(from);
        if (to > tasks.length) {
            to = tasks.length;
        }
        if (from == to) {
            return;
        }
        if (to - from == 1) {
            tasks[from].run();
            return;
        }
        ExecutorService pool = sourceArray == null ?
            getThreadPool(threadFactory) :
            getThreadPool(sourceArray, threadFactory);
        try {
            Future<?>[] results = new Future<?>[to - from];
            for (int taskIndex = from; taskIndex < to; taskIndex++) {
                results[taskIndex - from] = pool.submit(tasks[taskIndex]);
            }
            try {
                for (Future<?> future : results) {
                    future.get(); // waiting for finishing
                }
            } catch (ExecutionException e) {
                Arrays.throwUncheckedException(e.getCause());
            } catch (InterruptedException e) {
                throw IOErrorJ5.getInstance(e);
            }
        } finally {
            releaseThreadPool(pool);
        }
    }
}
