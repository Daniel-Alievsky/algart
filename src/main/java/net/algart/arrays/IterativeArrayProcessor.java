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

/**
 * <p>An iterative algorithm processing some AlgART array (arrays) or matrix (matrices).</p>
 *
 * <p>This abstraction describes any algorithm, processing AlgART arrays or matrices..
 * Processing starts from creating an instance of this interface, usually associated with
 * some source array or matrix and implementing some processing algorithm.
 * Then the loop of {@link #performIteration iterations} is performed,
 * until the boolean flag {@link #done() done} will be set to <code>true</code>.
 * Usually, each iteration modifies some AlgART array or matrix, and the flag <code>done</code> means
 * that further modifications are impossible.
 * Last, the result of calculations is returned by {@link #result()} method.
 * The type of the result is the generic parameter of this interface; usually it is {@link UpdatableArray}
 * or <code>{@link Matrix}&lt;?&nbsp;extends&nbsp;{@link UpdatableArray}&gt;</code>.</p>
 *
 * <p>Using of this interface is more convenient than manual programming the loop of iterations.
 * Typical scheme of using this interface is the following:</p>
 *
 * <pre>
 * {@link IterativeArrayProcessor}&lt;{@link UpdatablePArray}&gt; imp =
 * &#32;   <i>some_generation_method</i>({@link ArrayContext
 * <i>array_context</i>}, <i>processed_array</i>, <i>algorithm_settings)</i>;
 * {@link UpdatablePArray} result = imp.{@link #process()};
 * imp.{@link #freeResources(ArrayContext) freeResources}(null); // not necessary usually
 * </pre>
 *
 * <p>The {@link #process() process()} method organizes necessary loop and performs all "boring" work
 * connected with controlling the context. In particular, it tries to estimate necessary number of iterations
 * and calls {@link #performIteration(ArrayContext)} method with the corresponding
 * {@link ArrayContext#part subcontext} of the processor's {@link #context() context},
 * that allows correct showing the progress bar in the application.</p>
 *
 * <p>Moreover, there is an ability to automatically create "{@link #chain chains}" of several algorithms.</p>
 *
 * <p>To implement new iterative algorithm, it is enough to implement the following methods:</p>
 *
 * <ul>
 * <li>{@link #performIteration(ArrayContext context)},</li>
 * <li>{@link #done()},</li>
 * <li>{@link #estimatedNumberOfIterations()},</li>
 * <li>{@link #result()},</li>
 * <li>{@link #freeResources(ArrayContext)}.</li>
 * </ul>
 *
 * <p>All other methods are usually inherited from {@link AbstractIterativeArrayProcessor} class.</p>
 *
 * <p>Implementations of this interface are <b>thread-compatible</b>
 * (allow manual synchronization for multithreading access).
 * Without external synchronization, the methods of this interface may return unspecified results
 * while simultaneous accessing the same instance from several threads.</p>
 *
 * @param <T> the type of the result data returned by this processor.
 *
 * @author Daniel Alievsky
 */
public interface IterativeArrayProcessor<T> extends ArrayProcessor {

    /**
     * Performs the next iteration of the iterative algorithm.
     * If the algorithm is {@link #done()}, the results are unspecified:
     * please never call this method if {@link #done()} returns <code>true</code>.
     *
     * <p>You usually don't need to call this method: please call
     * {@link #process()} instead.
     * If you need to perform only one or <code>n</code> iterations, you may use
     * {@link #limitIterations(long) limitIterations(n)} call.
     *
     * <p><i>Warning: this method should ignore the {@link #context() current execution context} of this object</i>.
     * Instead, this method should use the context of execution specified by <code>context</code> argument.
     * This method is called by {@link #process()} method with the argument,
     * describing {@link ArrayContext#part(double, double) a&nbsp;subtrask} of the full algorithm.
     * The <code>context</code> argument can be {@code null}:
     * this method should work properly in this case (ignore the context).
     *
     * <p>This method must be implemented while creating a new iterative array-processing algorithm.
     *
     * @param context the context used by this instance for all operations; can be {@code null}.
     */
    void performIteration(ArrayContext context);

    /**
     * Returns <code>true</code> if and only if the algorithm was successfully finished and there is
     * no sense to perform further iterations.
     *
     * <p>This method usually does not perform actual calculations and works very quickly (just returns
     * and internal flag). However, this condition is not strict.
     *
     * <p>You usually don't need to call this method: it is automatically called by
     * {@link #process()} method.
     *
     * <p>This method must be implemented while creating a new iterative array-processing algorithm.
     *
     * @return <code>true</code> if and only if the algorithm was successfully finished.
     */
    boolean done();

    /**
     * Estimates the number of iterations, that should be performed from this moment to finish the algorithm.
     * Returns <code>0</code> if it is impossible or too difficult to estimate this number:
     * it means that the remaining number of iteration is unknown.
     *
     * <p>This method may require some time for its execution.
     *
     * <p>You usually don't need to call this method: it is automatically called from time to time by
     * {@link #process()} method.
     * It is used for creating subcontexts, describing a {@link ArrayContext#part part} of the full task.
     *
     * <p>This method must be implemented while creating a new iterative array-processing algorithm.
     *
     * @return the estimated number of iterations, that should be performed from this moment to finish the algorithm.
     */
    long estimatedNumberOfIterations();

    /**
     * Returns the result of the previous iteration.
     * Usually it is {@link UpdatableArray} or
     * <code>{@link Matrix}&lt;?&nbsp;extends&nbsp;{@link UpdatableArray}&gt;</code>.
     * This method returns valid result even if no iterations were performed yet.
     * If {@link #done()} method returns <code>true</code>, the result of this method
     * is the final result of iterative processing performed by this instance.
     *
     * <p>This method may return {@code null}.
     * In this case, the concrete implementation of this interface should provide additional methods
     * for returning calculation results.
     *
     * <p>This method does not perform actual calculations and works very quickly.
     *
     * <p>This method must be implemented while creating a new iterative array-processing algorithm.
     *
     * @return the result of the previous iteration (can be {@code null}).
     */
    T result();

    /**
     * If there are some resources, allocated by this object, which are not controlled
     * by Java garbage collectors &mdash; files, streams, sockets, locks, etc&#46; &mdash;
     * this method tries to release them (for example, to close any files).
     * The object stays alive: if the resources will be necessary for following operations,
     * they will be automatically re-acquired.
     *
     * <p>Usually, this method just calls
     * {@link Array#freeResources(ArrayContext) Array.freeResources(context)} and
     * {@link Matrix#freeResources(ArrayContext) Matrix.freeResources(context)}
     * for all temporary arrays and matrices, allocated by this object for storing work data.
     *
     * <p>If {@link #result()} method returns AlgART array or matrix (typical situation),
     * this method calls {@link Array#freeResources(ArrayContext) Array.freeResources(context)} /
     * {@link Matrix#freeResources(ArrayContext) Matrix.freeResources(context)} methods
     * for this array / matrix.
     *
     * <p>This method may be used in situations when the instance of this object has long time life
     * and will be reused in future.
     *
     * <p>This method must be implemented while creating a new iterative array-processing algorithm.
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     */
    void freeResources(ArrayContext context);

    /**
     * Performs a loop of calls of {@link #performIteration performIteration} method, while
     * {@link #done()} method returns <code>false</code>.
     * It is the main method of this interface, used by application.
     *
     * <p>This method uses its {@link #context() current context} to create an array context, that will be passed
     * to {@link #performIteration(ArrayContext)} method. The new context is made by
     * {@link ArrayContext#part(double, double)} method, according to information
     * returned by {@link #estimatedNumberOfIterations()}.
     * If the {@link #context() current context} is {@code null}, this method pass {@code null}
     * to {@link #performIteration(ArrayContext)}.
     *
     * <p>The <code>maxNumberOfIterations</code> argument allows to restrict the total number of
     * calls of {@link #performIteration performIteration},
     * that can be useful when the algorithm can work for very long time
     * (thousands or millions iterations).
     * If this argument is zero or positive,
     * this method will perform, as a maximum, <code>maxNumberOfIterations</code> iterations
     * (or less, if {@link #done()} will return <code>true</code> before this).
     * If it is zero, this method does nothing and immediately returns the {@link #result()}.
     * If it is negative, this argument is ignored.
     *
     * @return the result of all calculations
     *         (the result of {@link #result()} method after the last performed iteration).
     */
    T process();

    /**
     * Returns new object, implementing this interface, equivalent to this algorithm
     * with the only difference that the number of performed iterations does not exceed
     * the specified argument.
     *
     * <p>More precisely:
     *
     * <ul>
     * <li>the {@link #context() current context} in the returned instance is equal
     * to the current context of this instance.</li>
     *
     * <li>{@link #performIteration(ArrayContext context)} in the returned instance always calls
     * <code>thisInstance.{@link #performIteration performIteration}(context)</code>.</li>
     *
     * <li>{@link #done()} in the returned instance is equivalent to
     * <code>count&gt;=maxNumberOfIterations||thisInstance.{@link #done()}</code>,
     * where <code>count</code> is the total number of performed calls of
     * {@link #performIteration(ArrayContext context) performIteration} method.</li>
     *
     * <li>{@link #estimatedNumberOfIterations()} in the returned instance always returns
     * <code>max(0,maxNumberOfIterations-count)</code>,
     * where <code>count</code> is the total number of performed calls of
     * {@link #performIteration(ArrayContext context) performIteration} method.</li>
     *
     * <li>{@link #result()} in the returned instance always returns
     * <code>!thisInstance.{@link #result()}</code>.</li>
     *
     * <li>{@link #freeResources(ArrayContext) freeResources(context)} in the returned instance calls
     * <code>thisInstance.{@link #freeResources(ArrayContext) freeResources(context)}</code>.</li>
     * </ul>
     *
     * <p>As a result, the basic {@link #process()} method in the returned instance
     * will perform, as a maximum, <code>maxNumberOfIterations</code> only.
     * In particular, if <code>maxNumberOfIterations==0</code>, {@link #process()} method does nothing
     * and immediately returns {@link #result()} object.
     *
     * <p>If <code>maxNumberOfIterations&lt;0</code>, this method just returns the reference to this instance.
     * In other words, negative <code>maxNumberOfIterations</code> means unlimited number of iterations.
     *
     * @param maxNumberOfIterations the number of iterations, after which the {@link #done()} method
     *                              in the returned instance always returns <code>true</code>.
     * @return                      new algorithm, equivalent to this algorithm with limited number of iterations.
     */
    IterativeArrayProcessor<T> limitIterations(long maxNumberOfIterations);

    /**
     * Returns new object, implementing this interface, equivalent to the chain of this algorithm
     * and <code>followingProcessor</code> algorithm, executed after this.
     * In other words, {@link #process()} method of the returned instance performs
     * iterations of this instance, while its {@link #done()} method returns <code>false</code>,
     * and then performs iterations of <code>followingProcessor</code>, while
     * <code>followingProcessor.{@link #done()}</code> method returns <code>false</code>.
     *
     * <p>More precisely:
     *
     * <ul>
     * <li>the {@link #context() current context} in the returned instance is equal
     * to the current context of this instance.</li>
     *
     * <li>{@link #performIteration(ArrayContext context)} in the returned instance calls
     * <code>thisInstance.{@link #performIteration performIteration}(context)</code>, if
     * <code>!thisInstance.{@link #done()}</code>, or calls
     * <code>followingProcessor.{@link #performIteration performIteration}(context)</code> in another case.</li>
     *
     * <li>{@link #done()} in the returned instance is equivalent to
     * <code>thisInstance.{@link #done()}&amp;&amp;followingProcessor.{@link #done()}</code>.</li>
     *
     * <li>{@link #estimatedNumberOfIterations()} in the returned instance calls
     * <code>n1=thisInstance.{@link #estimatedNumberOfIterations()}</code> and
     * <code>n2=followingProcessor.{@link #estimatedNumberOfIterations()}</code>,
     * and returns <code>n2</code> if <code>thisInstance.{@link #done()}</code>,
     * or, in another case, <code>n1+Math.round(weight*n2)</code>, where <code>weight</code>
     * is the 2nd argument if this method. (If <code>n1==0</code> or <code>n2==0</code>, <code>0</code> is returned.)
     * The necessity of the weight here is connected with the fact, that
     * the <code>followingProcessor</code> may return illegal results,
     * because this processor did not process the matrix yet.</li>
     *
     * <li>{@link #result()} in the returned instance returns
     * <code>!thisInstance.{@link #done()} ? thisInstance.{@link #result()} :
     * followingProcessor.{@link #result()}</code>.</li>
     *
     * <li>{@link #freeResources(ArrayContext) freeResources(context)} in the returned instance calls
     * <code>thisInstance.{@link #freeResources(ArrayContext) freeResources(c1)}</code> and
     * <code>followingProcessor.{@link #freeResources(ArrayContext) freeResources(c2)}</code>,
     * where <code>c1</code> and <code>c2</code> are {@link ArrayContext#part(long, long, long) parts}
     * of the passed context.</li>
     * </ul>
     *
     * <p>It is obvious that both instances of iterative algorithms, this and <code>followingProcessor</code>,
     * must share the same processed data. In other words, <code>followingProcessor</code>
     * (its {@link #performIteration(ArrayContext)} and {@link #done()} methods) must
     * be able to "see" the results of execution of this processor.
     * To provide this, the constructors (or generation methods) of both instances
     * usually get a reference to the same updatable AlgART array or matrix,
     * storing the intermediate calculation results.
     *
     * @param followingProcessor the next iterative algorithm, that should be performed after this will be done.
     * @param weight             the weight for {@link #estimatedNumberOfIterations()
     *                           estimated number of iterations} of the next algorithm,
     *                           used while first (this) one is not finished yet.
     * @return                   new algorithm, equivalent to the chain of this algorithm and
     *                           <code>followingProcessor</code>.
     * @throws NullPointerException     if <code>followingProcessor</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>weight</code> argument is negative.
     */
    IterativeArrayProcessor<T> chain(IterativeArrayProcessor<T> followingProcessor, double weight);
}
