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

package net.algart.contexts;

/**
 * <p>Execution context for any modules.</p>
 *
 * <p>This interface is an universal concept allowing modules to request almost any information,
 * that, as supposed, describes an execution context of some module.
 * The concrete kinds of such information depend on the module nature.
 * For example, long-working mathematical algorithm may need access to some GUI component
 * that will allow it to show executiong percents.
 * Another example: a module, that creates temporary files for storing large data,
 * may need to know the directory for such files.</p>
 *
 * <p>There are two related concepts, traditionally used in algorithms and other modules.</p>
 *
 * <ul>
 * <li><i>Algorithm parameters</i>. They can be passed to the algorithm as method arguments or
 * as settings of the class instance. Usually, all parameters are absolutely necessary for performing
 * the task.</li>
 *
 * <li><i>Global application settings</i> or even <i>global OS settings</i>.
 * They may be passed to the algorithm via system properties (<code>System.getProperty</code>)
 * or environment variables (<code>System.getenv</code>).
 * The global directory for temporary files is a good example.
 * Another example: behavior of {@link net.algart.arrays} package can be customized
 * via <a href="../arrays/package-summary.html#systemProperties">several special system properties</a>.</li>
 * </ul>
 *
 * <p>The <i>context</i> is an intermediate layer between these two concepts.</p>
 *
 * <p>Unlike algorithm parameters, the context information <i>is not directly passed to the module</i>.
 * Usually the programmer, who calls some complex method, supporting the contexts,
 * should not think over and may even not know about all contextual information passed via the context.
 * The programmer just need to pass (as an argument of the method) some instance of this
 * {@link Context} interface, usually provided by the application.
 * Unlike algorithm parameters, the context <i>should not determine the final results</i> of execution.
 * But it may clarify the behavior of methods: for example, may allow them to show execution progress
 * or specify the directory for temporary files.
 * Some modules may work correctly without any context (when {@code null} is passed
 * as the {@link Context} argument).
 * Other modules may require some context providing the necessary information.
 * The requirements to the context, passed to some method,
 * are the part of the module contract and must be specified in Javadoc.</p>
 *
 * <p>Unlike global settings, a large application may use several different contexts.
 * For example, if there are several application windows,
 * and some long-working algorithm may be executed "inside" a window,
 * the application may pass to the algorithm a context, specific for a window
 * and allowing to show the executing progress bar in the corresponding window.</p>
 *
 * <p>This interface defines a maximally abstract context, that doesn't allow to
 * retrieve useful information. But its basic {@link #as(Class)} method allows to
 * get inheritors of this interface &mdash; so-called <i>specific contexts</i>,
 * that have additional methods accessing to different context information.
 * This package offers the following "standard" specific contexts: {@link InterruptionContext},
 * {@link ArrayMemoryContext}, {@link ArrayThreadPoolContext},
 * {@link StatusUpdater}, {@link ProgressUpdater}, {@link RectangleUpdater}.
 * There is also the {@link DefaultContext} class, simplifying implementation
 * of concrete contexts, and {@link SubContext} class, allowing to
 * create a new context on the base of existing one.</p>
 *
 * <p>A usage example:</p>
 *
 * <pre>
 * void someComplexAlgorithm({@link Context} context, <i>some other arguments</i>...) {
 * &#32;   {@link ProgressUpdater} pu = context.{@link #as(Class) as}({@link ProgressUpdater}.class);
 * &#32;   . . .
 * &#32;   for (int k = 0; k &lt; n; k++) { // the main long-working loop
 * &#32;       . . .
 * &#32;       pu.{@link ProgressUpdater#updateProgress(double, boolean) updateProgress}((k + 1.0) / n, k == n - 1);
 * &#32;   }
 * &#32;
 * &#32;   . . .
 * }
 * </pre>
 *
 * @author Daniel Alievsky
 */
public interface Context {

    /**
     * Retrieves a specific context according to the passed context class
     * or throws {@link UnsupportedContextException} if this context cannot serve this request.
     * The <code>contextClass</code> argument is an interface, that will be implemented
     * by the returned context, or (rarely) a class or superclass of the returned context.
     *
     * <p>If this instance already implements the required <code>contextClass</code>
     * (more precisely, if <code>contextClass.isAssignableFrom(thisInstance.getClass())</code>),
     * this method usually returns a reference to this instance.
     * In particular, it's true for all context implementations provided by this package.
     *
     * <p>If <code>contextClass</code> is {@code null} or is not an inheritor of {@link Context} interface,
     * this method throws an exception.
     *
     * @param contextClass the class of returned object (or superclass, or implemented interface).
     * @return             the required context.
     * @throws NullPointerException        if <code>contextClass</code> is {@code null}.
     * @throws IllegalArgumentException    if <code>contextClass</code> does not extends or implements
     *                                     {@link Context} interface.
     * @throws UnsupportedContextException if this context cannot serve the request.
     * @see #is(Class)
     */
    <T extends Context> T as(Class<T> contextClass);

    /**
     * Returns <code>true</code> if this context class can be processed by {@link #as(Class)} method.
     * Returns <code>false</code> if and only if <code>contextClass==null</code>,
     * <code>contextClass</code> is not an inheritor of {@link Context} interface or
     * the corresponding call {@link #as(Class) as(contextClass)} throws
     * {@link UnsupportedContextException}.
     *
     * @param contextClass the class or interface of a context.
     * @return             <code>true</code> if this context class can be processed by {@link #as(Class)} method.
     * @see #as(Class)
     */
    boolean is(Class<? extends Context> contextClass);
}
