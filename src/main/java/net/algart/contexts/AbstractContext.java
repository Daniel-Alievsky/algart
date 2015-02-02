/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.*;
import java.util.logging.*;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.Method;

/**
 * <p>A skeletal implementation of the {@link Context} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>The behavior, provided by this implementation, is described in the comments
 * to {@link #as(Class)} and {@link #is(Class)} methods below.</p>
 *
 * <p>The full set of
 * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html"><i>service providers</i></a>
 * for {@link Context} interface, used by these methods, is loaded once
 * when they are needed for these methods for the first time.
 * Though a documented tool for loading service providers was added since Java 1.6,
 * this technique works in earlier Java versions also.</p>
 *
 * <p>If some exception occurs while loading the set of service providers
 * (for example, if some of the service providers, listed in
 * <tt>META-INF/services/net.algart.contexts.Context</tt> file,
 * have no public constructors without arguments), this fact is logged via
 * standard <tt>java.util.logging</tt> tools
 * with the <tt>SEVERE</tt> level.
 * The following logger is used for this purpose:
 * <tt>Logger.getLogger("{@link net.algart.contexts.AbstractContext net.algart.contexts.AbstractContext}")</tt>.
 * In this case, the execution of the called {@link #as(Class)} or {@link #is(Class)} method is not interrupted,
 * but some (or all) service providers may be ignored in this and futher requests.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractContext implements Context {

    private static final Logger LOGGER = Logger.getLogger(AbstractContext.class.getName());

    /**
     * The value of the corresponding argument of the {@link #AbstractContext(boolean) constructor}.
     */
    protected final boolean useServiceLoader;

    /**
     * Creates a new instance of this class.
     *
     * @param useServiceLoader whether {@link #as(Class)} and {@link #is(Class)} methods should
     *                         check all service providers for the {@link Context} interface
     *                         to find a suitable provider, in a case when this instance
     *                         does not implement the required interface (or extends the class)
     *                         <tt>contextClass</tt>.
     */
    protected AbstractContext(boolean useServiceLoader) {
        this.useServiceLoader = useServiceLoader;
    }

    /**
     * This implementation returns, when possible, the reference to this instance
     * or to the service provider implementing the required class.
     * More precisely:
     *
     * <ol>
     * <li>if this instance implements (extends) the required interface (class) <tt>contextClass</tt>,
     * i.e. if <tt>contextClass.isAssignableFrom(thisInstance.getClass())</tt>,
     * then this method returns the reference to this instance;</li>
     *
     * <li>else, if there is at least one
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html"><i>service provider</i></a>
     * for the {@link Context} interface (<i>not</i> for the passed <tt>contextClass</tt>!),
     * listed in <tt>META-INF/services/net.algart.contexts.Context</tt> file,
     * which implements (extends) the required interface (class) <tt>contextClass</tt>,
     * then an instance of this service provider is returned;</li>
     *
     * <li>else {@link UnsupportedContextException} is thrown.</li>
     * </ol>
     *
     * <p>The 2nd check is performed only if the <tt>useServiceLoader</tt> argument, passed to
     * the constructor, was <tt>true</tt>.
     * If it was <tt>false</tt>, the service providers are not loaded and not checked.</p>
     *
     * @param contextClass the class of returned object (or superclass, or implemented interface).
     * @return             this instance, if it is suitable, or some service provider of {@link Context}
     *                     interface that implements (extends) required <tt>contextClass</tt>.
     * @throws NullPointerException        if <tt>contextClass</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException    if <tt>contextClass</tt> does not extends or implements
     *                                     {@link Context} interface.
     * @throws UnsupportedContextException if this context cannot serve the request.
     * @see #is(Class)
     */
    public <T extends Context> T as(Class<T> contextClass) {
        if (contextClass == null)
            throw new NullPointerException("Null contextClass argument");
        if (!Context.class.isAssignableFrom(contextClass))
            throw new IllegalArgumentException("The contextClass argument is not a context class ("
                + contextClass.getName() + ")");
        if (contextClass.isAssignableFrom(getClass())) {
            return contextClass.cast(this);
        }
        if (useServiceLoader) {
            T result = getContextViaServiceLoader(contextClass);
            if (result != null) {
                return result;
            }
        }
        throw new UnsupportedContextException(contextClass);
    }

    /**
     * Returns <tt>true</tt> if this context class can be processed by {@link #as(Class)} method.
     * More precisely:
     *
     * <ol>
     * <li>if <tt>contextClass==null</tt> or <tt>contextClass</tt> is not an inheritor
     * of {@link Context} interface, this method returns <tt>false</tt>;</li>
     *
     * <li>if this instance implements (extends) the required interface (class) <tt>contextClass</tt>,
     * i.e. if <tt>contextClass.isAssignableFrom(thisInstance.getClass())</tt>,
     * then this method returns <tt>true</tt>;</li>
     *
     * <li>else, if there is at least one
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html"><i>service provider</i></a>
     * for the {@link Context} interface (<i>not</i> for the passed <tt>contextClass</tt>!),
     * listed in <tt>META-INF/services/net.algart.contexts.Context</tt> file,
     * which implements (extends) the required interface (class) <tt>contextClass</tt>,
     * then this method returns <tt>true</tt>;</li>
     *
     * <li>else this method returns <tt>false</tt>.</li>
     * </ol>
     *
     * <p>The 3rd check is performed only if the <tt>useServiceLoader</tt> argument, passed to
     * the constructor, was <tt>true</tt>.
     * If it was <tt>false</tt>, the service providers are not loaded and not checked.</p>
     *
     * @param contextClass the class or interface of a sub-context.
     * @return             <tt>true</tt> if this context class can be processed by {@link #as(Class)} method.
     * @see #as(Class)
     */
    public boolean is(Class<? extends Context> contextClass) {
        if (contextClass == null || !Context.class.isAssignableFrom(contextClass))
            return false;
        if (contextClass.isAssignableFrom(getClass()))
            return true;
        if (useServiceLoader) {
            return getContextViaServiceLoader(contextClass) != null;
        }
        return false;
    }

    private static final ReentrantLock lock = new ReentrantLock();
    private static Set<Context> ALL_CONTEXTS = null;
    private static Map<Class<? extends Context>, Context> ACTIVE_CONTEXTS = null;
    private static <T extends Context> T getContextViaServiceLoader(Class<T> contextClass) {
        lock.lock();
        try {
            if (ALL_CONTEXTS == null) {
                ALL_CONTEXTS = new HashSet<Context>();
                ACTIVE_CONTEXTS = new HashMap<Class<? extends Context>, Context>();
                try {
                    Iterator<?> iterator = null;
                    try { // try ServiceLoader (Java 1.6+)
                        Class<?> loaderClass = Class.forName("java.util.ServiceLoader");
                        Method loadMethod = loaderClass.getMethod("load", Class.class);
                        Object loader = loadMethod.invoke(null, Context.class);
                        Method iteratorMethod = loaderClass.getMethod("iterator");
                        iterator = (Iterator<?>)iteratorMethod.invoke(loader);
                    } catch(Throwable ex) {
                        // System.out.println("Error in 1.6 code: " + ex);
                    }
                    if (iterator == null) {
                        // try sun.misc.Service
                        try {
                            Class<?> loaderClass = Class.forName("sun.misc.Service");
                            Method providersMethod = loaderClass.getMethod("providers", Class.class);
                            iterator = (Iterator<?>)providersMethod.invoke(null, Context.class);
                        } catch(Throwable ex) {
                            // System.out.println("Error in 1.5 code: " + ex);
                        }
                    }
                    if (iterator == null) {
                        throw new InternalError("Cannot find service provider feature. "
                            + "Maybe, current JVM version is too early: "
                            + System.getProperty("java.version")
                            + " (JVM 1.4+ is required)");
                    }
                    while (iterator.hasNext()) {
                        try {
                            Context context = (Context)iterator.next();
                            ALL_CONTEXTS.add(context);
                        } catch (Throwable ex) {
                            LOGGER.log(Level.SEVERE, "Cannot load a context by the service loader", ex);
                        }
                    }
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Error while loading contexts by the service loader", ex);
                }
            }
            T result;
            if (ACTIVE_CONTEXTS.containsKey(contextClass)) { // may be null!
                result = contextClass.cast(ACTIVE_CONTEXTS.get(contextClass));
            } else {
                result = null;
                for (Context c : ALL_CONTEXTS) {
                    if (contextClass.isInstance(c)) {
                        result = contextClass.cast(c);
                        break;
                    }
                }
                ACTIVE_CONTEXTS.put(contextClass, result); // may be null!
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
