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

package net.algart.finalizing;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;

/**
 * <p><i>Finalizer</i>: an universal tool allowing to perform any task on deallocation of some object.</p>
 *
 * <p>This class can be a helpful alternative to the standard <code>finalize()</code> method.
 * It is based on the mechanism of phantom references and may be better solution
 * than <code>finalize()</code> method in many situations.</p>
 *
 * <p>The main advantages of <code>Finalizer</code> class are the following.</p>
 *
 * <ol>
 * <li>Declaration of <code>finalize()</code> method slows down instantiation of objects.
 * Finalization via <code>Finalizer</code> class does not lead to any slowing.</li>
 *
 * <li><code>Finalizer</code> class allows to control the thread, which performs finalization tasks,
 * in particular, to stop it or to set its priority.</li>
 *
 * <li><code>Finalizer</code> class allows to do something on deallocation of any object,
 * even if we have no ability to override its methods.</li>
 * </ol>
 *
 * <p>The typical example of using this class is the following:</p>
 *
 * <pre>
 * class MyClass {
 * &#32;   static final Finalizer fin = new Finalizer();
 * &#32;   . . .
 * &#32;   void myMethod() {
 * &#32;       // . . .
 * &#32;       // some Java code
 * &#32;       // . . .
 * &#32;       final SomeType largeResources = ...; // some external resources associated with "data" object
 * &#32;       fin.{@link #invokeOnDeallocation(Object, Runnable) invokeOnDeallocation}(data, new Runnable() {
 * &#32;           public void run() {
 * &#32;               try {
 * &#32;                   ... // disposing largeResources
 * &#32;               } catch (Throwable ex) {
 * &#32;                   ex.printStackTrace(); // or logging the exception by some logger
 * &#32;               }
 * &#32;           }
 * &#32;       }
 * &#32;       // . . .
 * &#32;       // some Java code
 * &#32;       // . . .
 * &#32;   }
 * });
 * </pre>
 * <p>In this example, <code>run()</code> method will be performed at the moment when
 * <code>data</code> object will be ready for deallocation by garbage collector.</p>
 *
 * <p>Important note: the implementation of <code>Runnable</code> interface <b>must not contain any
 * direct or indirect references to <code>data</code> object</b>.
 * In another case, the <code>data</code> instance will never become unreachable and the <code>run()</code> method
 * will never be called. In particular, <code>largeResources</code> object, processed by
 * <code>run()</code> method and accessible there via "<code>final</code>" declaration,
 * must not refer to <code>data</code> instance in any ways.
 * In other words, unlike the standard <code>finalize()</code> method,
 * the finalization code, scheduled by this class, cannot refer to the finalized data.</p>
 *
 * <p>Every <code>Finalizer</code> instance contains a daemon thread, that is started on the first
 * call of {@link #invokeOnDeallocation(Object, Runnable)
 * invokeOnDeallocation(checkedForDeallocation,task)} method.
 * This thread looks, in an infinite loop, for deallocation of all objects,
 * passed to that method, and runs corresponding tasks when the objects
 * ("checked for deallocation") become unreachable.
 * This thread will run all time until closing the application,
 * if you will not stop it by {@link #shutdownNow()} method.
 * So, you should avoid creating extra instances of <code>Finalizer</code>:
 * please use one or several global finalizers for a package or application.</p>
 *
 * <p>As well as for the classic <code>finalize()</code> method, <b>there is no guarantee
 * that finalization tasks scheduled by this class will be really performed before
 * finishing the application</b>. Usually, exiting the application just stops all daemon
 * threads inside all instances of this class, and the tasks, which were not completed yet,
 * are canceled.</p>
 *
 * <p>To increase probability of performing all finalization tasks, you may
 * add a code alike the following at the point when application is finished
 * (or closed by a user):</p>
 *
 * <pre>
 * long t = System.currentTimeMillis();
 * while (myFinalizer.{@link #activeTasksCount()} > 0) {
 * &#32;   System.runFinalization();
 * &#32;   System.gc();
 * &#32;   Thread.sleep(50);
 * &#32;   if (System.currentTimeMillis() - t > TIMEOUT_IN_MILLISECONDS)
 * &#32;       break;
 * }
 * </pre>
 *
 * <p>This "<code>while</code>" loop here waits until all tasks, scheduled in <code>myFinalizer</code>,
 * will be successfully finished. The loop should be restricted by some suitable, not too long timeout:</p>
 *
 * <ul>
 * <li>firstly, because <code>System.runFinalization()</code> and <code>System.gc()</code> do not guarantee
 * finalization of any object,</li>
 * <li>secondly, because the <code>activeTasksCount</code> value
 * will never become zero, if some references to data instances, scheduled for finalization,
 * were not "forgotten" (have not become unreachable) due to some bug in the other application code.</li>
 * </ul>
 *
 * <p>If your system use several finalizers, you should
 * perform the same loop for each one, or replace the single call of <code>activeTasksCount()</code>
 * with the sum of results of this method for all finalizers:</p>
 *
 * <pre>
 * ...
 * while (myFinalizer1.{@link #activeTasksCount()}
 * &#32;   + myFinalizer2.{@link #activeTasksCount()}
 * &#32;   + ...
 * &#32;   + myFinalizerN.{@link #activeTasksCount()} > 0) {
 * ...
 * </pre>
 *
 * <p>It's possible that your <code>task</code> object contains references to some other "large" objects,
 * which also should be finalized before finishing your application and which implement
 * finalization in another way (for example, some standard Java objects as <code>MappedByteBuffer</code>).
 * When the "<code>while</code>" loop, listed above, finishes, such objects become unreachable,
 * but not really finalized yet. To be on the safe side, we recommend to add the following loop
 * <i>after</i> the "<code>while</code>" loop listed above:</p>
 *
 * <pre>
 * for (int k = 0; k &lt; 5; k++) {
 * &#32;   // finalizing some additional objects that could be
 * &#32;   // referred from finalization tasks performed above
 * &#32;   System.runFinalization();
 * &#32;   System.gc();
 * }
 * </pre>
 *
 * <p>This class is <b>thread-safe</b>: you may use the same instance of this class in several threads.</p>
 *
 * @author Daniel Alievsky
 */
public final class Finalizer {
    private Thread thread = null;
    private int priority = Thread.NORM_PRIORITY;

    private final Set<PhantomFinalizeHolder> taskSet = new HashSet<>();
    private boolean shutdownRequested = false;
    private ReferenceQueue<Object> refQueue = null;

    /**
     * Creates new instance of finalizer.
     *
     * <p>Please avoid creating extra finalizers. It is a good practice to create
     * only one finalizer for a class or package requiring finalization tehcnique,
     * for example, in a package-private static field.
     */
    public Finalizer() {
    }

    /**
     * Schedules running of the given <code>task</code> (its <code>run()</code> method)
     * at the moment when the <code>checkedForDeallocation</code> object will become unreachable
     * (more precisely, <i>phantom reachable</i>).
     *
     * <p>The first call of this method starts the internal thread in this object.
     * This thread will look, in an infinite loop, for the levels of reachability
     * of all objects passed to this method as <code>checkedForDeallocation</code> argument,
     * and will call corresponding tasks when these objects will become phantomly
     * reachable.
     *
     * <p>Important: the implementation of <code>task</code> <i>must not contain references
     * to the passed <code>checkedForDeallocation</code> instance</i>! In another case,
     * this instance will never become unreachable and <code>task.run()</code> method
     * will never be called.
     *
     * <p>Note: if the class of <code>checkedForDeallocation</code> object,
     * or the class of the last object which allows to reach <code>checkedForDeallocation</code>,
     * declares standard <code>finalize()</code> method, then the <code>task</code> may not be called
     * while the <i>first</i> <code>System.gc()</code> call after the moment when
     * <code>checkedForDeallocation</code> object will become phantomly reachable,
     * but only while <i>second or further</i> <code>System.gc()</code> calls.
     *
     * @param checkedForDeallocation some object.
     * @param task                   a task thah will be performed on deallocation
     *                               of <code>checkedForDeallocation</code> object.
     */
    public void invokeOnDeallocation(Object checkedForDeallocation, Runnable task) {
        synchronized (taskSet) {
            if (thread == null) {
                refQueue = new ReferenceQueue<>();
                shutdownRequested = false;
                thread = new CleanupThread(this);
                thread.setDaemon(true);
                try {
                    thread.setPriority(priority);
                } catch (SecurityException ignored) {
                }
                thread.start();
            }
            new PhantomFinalizeHolder(this, checkedForDeallocation, task);
        }
    }

    /**
     * Returns the current number of tasks that are scheduled by
     * {@link #invokeOnDeallocation(Object, Runnable) invokeOnDeallocation} method,
     * but not fully performed yet.
     *
     * @return the current number of scheduled tasks.
     */
    public int activeTasksCount() {
        synchronized (taskSet) {
            return taskSet.size();
        }
    }

    /**
     * Shutdown the finalizer. If some task is running now, it will be completed.
     * All tasks that are not running yet will be removed and will not be performed.
     *
     * <p><i>You should not use this <code>Finalizer</code> instance after calling this method.</i>
     *
     * <p>You may call this method if you are absolutely sure that this finalizer will not be useful more.
     * It is the only way to stop the thread serving this finalizer before finishing the application.
     */
    public void shutdownNow() {
        synchronized (taskSet) {
            if (thread != null) {
                shutdownRequested = true;
                thread.interrupt();
            }
        }
    }

    /**
     * Sets the priority of the thread serving this finalizer.
     * The argument of this method will be passed to standard <code>Thread.setPriority</code> method.
     *
     * <p>Unlike <code>Thread.setPriority</code> method, the <code>SecurityException</code>
     * (that can occur while setting priority) will be ignored: if it occurs,
     * the priority is not changed.
     *
     * @param priority priority to set the internal thread to.
     * @throws IllegalArgumentException if the priority is not in the
     *                                  range <code>Thread.MIN_PRIORITY</code>..<code>Thread.MAX_PRIORITY</code>.
     */
    public void setPriority(int priority) {
        synchronized (taskSet) {
            if (priority > Thread.MAX_PRIORITY || priority < Thread.MIN_PRIORITY) {
                throw new IllegalArgumentException();
            }
            this.priority = priority;
            if (thread != null) {
                try {
                    thread.setPriority(priority);
                } catch (SecurityException ignored) {
                }
            }
        }
    }

    private static class PhantomFinalizeHolder extends PhantomReference<Object> {
        final Runnable task;

        PhantomFinalizeHolder(Finalizer fin, Object checkedForDeallocation, Runnable task) {
            super(checkedForDeallocation, fin.refQueue);
            this.task = task;
            fin.taskSet.add(this);
            // avoid deallocation of this reference before the cleanup procedure
        }
    }

    private static class CleanupThread extends Thread {
        final Finalizer fin;

        CleanupThread(Finalizer fin) {
            this.fin = fin;
        }

        public void run() {
            while (true) {
                PhantomFinalizeHolder phantomHolder = null;
                Reference<?> holder = null;
                try {
                    holder = fin.refQueue.remove();
                    phantomHolder = (PhantomFinalizeHolder) holder;
                } catch (InterruptedException ignored) {
                }
                if (phantomHolder != null) {
                    phantomHolder.task.run();
                    // run() method may try to control the current thread,
                    // in particular, to clear "interrupted" flag
                }
                if (holder != null) {
                    synchronized (fin.taskSet) {
                        fin.taskSet.remove(holder);
                    }
                }
                synchronized (fin.taskSet) {
                    if (fin.shutdownRequested) {
                        fin.taskSet.clear();
                        fin.thread = null;
                        return;
                    }
                }
            }//while
        }
    }
}
