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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>A wrapper around the parent context, allowing to describe a subtask of some long-working task.</p>
 *
 * <p>This wrapper delegates all calls of {@link #as as} and {@link #is is} methods to
 * the parent context with the only exception, concerning the {@link ProgressUpdater} contexts.
 * Namely, when we request {@link ProgressUpdater} from this wrapper via
 * <code>{@link #as as}(ProgressUpdater.class)</code> call, the returned progress updater
 * will transform the execution percents (passed to its
 * {@link ProgressUpdater#updateProgress updateProgress(readyPart,force)} method)
 * by the following formula:</p>
 *
 * <blockquote><pre>fromPart + readyPart * (toPart - fromPart)</pre></blockquote>
 *
 * <p>where <code>fromPart</code> and <code>toPart</code> are some real values in <code>0.0..1.0</code> range,
 * passed to the {@link #SubtaskContext(Context, double, double) constructor} of this class.
 * So, if we shall pass this wrapper to some method, that uses a context
 * for showing its execution percents from 0% to 100%,
 * then the source (parent) context will show the change of execution percents
 * from <code>fromPart*100%</code> to <code>toPart*100%</code>.
 * It can be very convenient, if the called method solves only a part of the full task,
 * approximately <code>fromPart*100%..toPart*100%</code>.</p>
 *
 * <p id="specification">More precisely,
 * let <code>sc</code> is the instance of this class and <code>parent</code> is its parent context,
 * and let <code>fromPart</code> and <code>toPart</code> are the constructor arguments.
 * Then:</p>
 *
 * <ul>
 * <li><code>sc.{@link #is(Class) is}(contextClass)</code> call is fully equivalent to
 * <code>parent.is(contextClass)</code>;
 * <br>&nbsp;</li>
 *
 * <li>if <code>contextClass!=ProgressUpdater.class</code>, then
 * <code>sc.{@link #as(Class) as}(contextClass)</code> call is equivalent to
 * <code>parent.as(contextClass)</code>, with the only difference that {@link #as as} method
 * in the returned instance also "remebmers" about the subtask and works according the rules,
 * specified here;
 * <br>&nbsp;</li>
 *
 * <li>however, <code>sc.{@link #as(Class) as}(ProgressUpdater.class)</code>
 * differs from <code>parent.as(ProgressUpdater.class)</code>. Namely, let
 * <code>pu=parent.as(ProgressUpdater.class)</code> (so, <code>pu</code> implements
 * {@link ProgressUpdater}). Then
 * <code>sc.{@link #as(Class) as}(ProgressUpdater.class)</code> returns
 * an instance <code>spu</code> of some internal class, implementing {@link ProgressUpdater},
 * with the following behavior:
 *
 *     <ul>
 *     <li><code>spu.is(contextClass)</code> and <code>spu.as(contextClass)</code> are equivalent to
 *     <code>sc.is(contextClass)</code> and <code>sc.as(contextClass)</code>;</li>
 *
 *     <li><code>spu.{@link ProgressUpdater#updateProgress updateProgress}(readyPart,force)</code>
 *     calls <code>pu.{@link ProgressUpdater#updateProgress
 *     updateProgress}(fromPart+readyPart*(toPart-fromPart),force)</code>.</li>
 *     </ul>
 * <p>
 * Note: if the parent context does not support {@link ProgressUpdater}
 * (<code>parent.as({@link ProgressUpdater}.class)</code> throws {@link UnsupportedContextException}),
 * then this context <code>sc</code> does not support {@link ProgressUpdater} also
 * (throws the same exception).
 * </li>
 * </ul>
 *
 * <p>Warning: in an instance <code>c</code>, returned by {@link #as as} method of this class,
 * <b>the reference <code>this</code> inside its methods may differ from <code>c</code> reference!</b>
 * It is because {@link #as as} method of this class returns a <i>proxy class</i>,
 * wrapping all method of the parent context.</p>
 *
 * @author Daniel Alievsky
 */
public class SubtaskContext implements Context {
    private final Context parentContext;
    private final double fromPart;
    private final double toPart;

    /**
     * Creates new context on the base of the passed <code>parentContext</code>
     * and <code>fromPart</code> / <code>toPart</code> values.
     *
     * @param parentContext the parent context that will serve all methods of this class.
     * @param fromPart      the estimated ready part of the total algorithm at the start of the subtask;
     *                      must be in <code>0.0..1.0</code> range.
     * @param toPart        the estimated ready part of the total algorithm at the finish of the subtask;
     *                      must be in <code>fromPart..1.0</code> range.
     * @throws NullPointerException     if the <code>parentContext</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>fromPart</code> or <code>toPart</code> is not in
     *                                  <code>0.0..1.0</code> range or if <code>fromPart&gt;toPart</code>.
     * @see #SubtaskContext(Context, long, long, long)
     */
    public SubtaskContext(Context parentContext, double fromPart, double toPart) {
        Objects.requireNonNull(parentContext, "Null parentContext argument");
        if (fromPart < 0.0) {
            throw new IllegalArgumentException("Illegal fromPart=" + fromPart + " (must be in range 0.0..1.0)");
        }
        if (toPart > 1.0) {
            throw new IllegalArgumentException("Illegal toPart=" + toPart + " (must be in range 0.0..1.0)");
        }
        if (fromPart > toPart) {
            throw new IllegalArgumentException("Illegal fromPart=" + fromPart + " or toPart=" + toPart
                    + " (fromPart must not be greater than toPart)");
        }
        this.parentContext = parentContext;
        this.fromPart = fromPart;
        this.toPart = toPart;
    }

    /**
     * Creates new context on the base of the passed <code>parentContext</code> and the range
     * <code>from/total*100%..to/total*100%</code>,
     * specified by integer values relatively some "total" number of operations.
     * More precisely, equivalent to the following call:
     * <pre>
     * new {@link #SubtaskContext(Context, double, double) SubtaskContext}(parentContext,
     * &#32;   (double)from/(double)total,
     * &#32;   to==total ? 1.0: (double)to/(double)total)
     * </pre>
     * excepting the case <code>from=to=total=0</code>, when it is equivalent to
     * <pre>
     * new {@link #SubtaskContext(Context, double, double) SubtaskContext}(0.0, 1.0)
     * </pre>
     *
     * @param parentContext the parent context that will serve all methods of this class.
     * @param from          the estimated ready part, from 0 to <code>total</code>,
     *                      of the total algorithm at the start of the subtask.
     * @param to            the estimated ready part, from 0.0 to <code>total</code>,
     *                      of the total algorithm at the finish of the subtask.
     * @param total         the number of some operation in the full task.
     * @throws IllegalArgumentException if <code>from</code> or <code>to</code> is not in <code>0..total</code> range,
     *                                  or if <code>from&gt;to</code>, or if <code>total&lt;0</code>.
     * @see #SubtaskContext(Context, double, double)
     */
    public SubtaskContext(Context parentContext, long from, long to, long total) {
        Objects.requireNonNull(parentContext, "Null parentContext argument");
        if (total < 0) {
            throw new IllegalArgumentException("Negative total=" + total);
        }
        if (from < 0) {
            throw new IllegalArgumentException("Illegal from=" + from + " (must be in range 0.." + total + ")");
        }
        if (to > total) {
            throw new IllegalArgumentException("Illegal to=" + to + " (must be in range 0.." + total + ")");
        }
        if (from > to) {
            throw new IllegalArgumentException("Illegal from=" + from + " or to=" + to
                    + " (\"from\" must not be greater than \"to\")");
        }
        this.parentContext = parentContext;
        if (total == 0) {
            assert from == 0;
            assert to == 0;
            this.fromPart = 0.0;
            this.toPart = 1.0;
        } else {
            this.fromPart = (double) from / (double) total;
            this.toPart = to == total ? 1.0 : (double) to / (double) total;
        }
    }

    /**
     * See the <a href="#specification">the detailed specification</a> in the comments to this class.
     *
     * @param contextClass the class of returned object (or superclass, or implemented interface).
     * @return the required context.
     * @throws NullPointerException        if <code>contextClass</code> is {@code null}.
     * @throws IllegalArgumentException    if <code>contextClass</code> does not extends or implements
     *                                     {@link Context} interface.
     * @throws UnsupportedContextException if the parent context cannot serve the request.
     */
    public final <T extends Context> T as(Class<T> contextClass) {
//        System.out.println("Calling as(" + contextClass + ") in " + this.getClass() + ", parent = " + parentContext);
        T castedParentContext = parentContext.as(contextClass);
        if (contextClass == ProgressUpdater.class) {
            return contextClass.cast(new ProgressUpdaterSubtaskContext((ProgressUpdater) castedParentContext));
        } else {
            return contextClass.cast(Proxy.newProxyInstance(castedParentContext.getClass().getClassLoader(),
                    new Class<?>[]{contextClass}, new SubtaskInvocationHandler(castedParentContext)));
        }
    }

    /**
     * Fully equivalent to <code>parentContext.is(contextClass)</code>, where <code>parentContext</code> is the
     * constructor argument.
     *
     * @param contextClass the class or interface of a context.
     * @return <code>true</code> if this context class can be processed by {@link #as(Class)} method.
     */
    public final boolean is(Class<? extends Context> contextClass) {
        return parentContext.is(contextClass);
    }

    public String toString() {
        return String.format(Locale.US, "subtask context %.3f..%.3f of %s", fromPart, toPart, parentContext);
    }

    private class SubtaskInvocationHandler implements InvocationHandler {
        private final Context castedParentContext;

        private SubtaskInvocationHandler(Context castedParentContext) {
            this.castedParentContext = castedParentContext;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String name = method.getName();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (name.equals("as")) {
                if (parameterTypes.length == 1 && parameterTypes[0] == Class.class) { // "as(Class<T> contextClass)"
                    return SubtaskContext.this.as((Class) args[0]);
                }
            }
            try {
                return method.invoke(castedParentContext, args);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new AssertionError("Internal error in " + SubtaskContext.class, e);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private class ProgressUpdaterSubtaskContext implements ProgressUpdater {
        private final ProgressUpdater parentProgressUpdater;

        private ProgressUpdaterSubtaskContext(ProgressUpdater parentProgressUpdater) {
            this.parentProgressUpdater = parentContext.as(ProgressUpdater.class);
        }

        public final <T extends Context> T as(Class<T> contextClass) {
            if (contextClass == ProgressUpdater.class) {
                return contextClass.cast(this); // little optimization
            } else {
                return SubtaskContext.this.as(contextClass);
            }
        }

        public final boolean is(Class<? extends Context> contextClass) {
            return SubtaskContext.this.is(contextClass);
        }

        public void updateProgress(double readyPart, boolean force) {
            this.parentProgressUpdater.updateProgress(fromPart + readyPart * (toPart - fromPart), force);
        }

        public String toString() {
            return String.format(Locale.US, "subtask progress updater %.3f..%.3f of %s",
                    fromPart, toPart, parentContext);
        }
    }
}
