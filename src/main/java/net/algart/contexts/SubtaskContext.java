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
 * <tt>{@link #as as}(ProgressUpdater.class)</tt> call, the returned progress updater
 * will transform the execution percents (passed to its
 * {@link ProgressUpdater#updateProgress updateProgress(readyPart,force)} method)
 * by the following formula:</p>
 *
 * <blockquote><pre>fromPart + readyPart * (toPart - fromPart)</pre></blockquote>
 *
 * <p>where <tt>fromPart</tt> and <tt>toPart</tt> are some real values in <tt>0.0..1.0</tt> range,
 * passed to the {@link #SubtaskContext(Context, double, double) constructor} of this class.
 * So, if we shall pass this wrapper to some method, that uses a context
 * for showing its execution percents from 0% to 100%,
 * then the source (parent) context will show the change of execution percents
 * from <tt>fromPart*100%</tt> to <tt>toPart*100%</tt>.
 * It can be very convenient, if the called method solves only a part of the full task,
 * approximately <tt>fromPart*100%..toPart*100%</tt>.</p>
 *
 * <p><a name="specification"></a>More precisely,
 * let <tt>sc</tt> is the instance of this class and <tt>parent</tt> is its parent context,
 * and let <tt>fromPart</tt> and <tt>toPart</tt> are the constructor arguments.
 * Then:</p>
 *
 * <ul>
 * <li><tt>sc.{@link #is(Class) is}(contextClass)</tt> call is fully equivalent to
 * <tt>parent.is(contextClass)</tt>;
 * <br>&nbsp;</li>
 *
 * <li>if <tt>contextClass!=ProgressUpdater.class</tt>, then
 * <tt>sc.{@link #as(Class) as}(contextClass)</tt> call is equivalent to
 * <tt>parent.as(contextClass)</tt>, with the only difference that {@link #as as} method
 * in the returned instance also "remebmers" about the subtask and works according the rules,
 * specified here;
 * <br>&nbsp;</li>
 *
 * <li>however, <tt>sc.{@link #as(Class) as}(ProgressUpdater.class)</tt>
 * differs from <tt>parent.as(ProgressUpdater.class)</tt>. Namely, let
 * <tt>pu=parent.as(ProgressUpdater.class)</tt> (so, <tt>pu</tt> implements
 * {@link ProgressUpdater}). Then
 * <tt>sc.{@link #as(Class) as}(ProgressUpdater.class)</tt> returns
 * an instance <tt>spu</tt> of some internal class, implementing {@link ProgressUpdater},
 * with the following behavior:
 *
 *     <ul>
 *     <li><tt>spu.is(contextClass)</tt> and <tt>spu.as(contextClass)</tt> are equivalent to
 *     <tt>sc.is(contextClass)</tt> and <tt>sc.as(contextClass)</tt>;</li>
 *
 *     <li><tt>spu.{@link ProgressUpdater#updateProgress updateProgress}(readyPart,force)</tt>
 *     calls <tt>pu.{@link ProgressUpdater#updateProgress
 *     updateProgress}(fromPart+readyPart*(toPart-fromPart),force)</tt>.</li>
 *     </ul>
 *
 * Note: if the parent context does not support {@link ProgressUpdater}
 * (<tt>parent.as({@link ProgressUpdater}.class)</tt> throws {@link UnsupportedContextException}),
 * then this context <tt>sc</tt> does not support {@link ProgressUpdater} also
 * (throws the same exception).
 * </li>
 * </ul>
 *
 * <p>Warning: in an instance <tt>c</tt>, returned by {@link #as as} method of this class,
 * <b>the reference <tt>this</tt> inside its methods may differ from <tt>c</tt> reference!</b>
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
     * Creates new context on the base of the passed <tt>parentContext</tt>
     * and <tt>fromPart</tt> / <tt>toPart</tt> values.
     *
     * @param parentContext the parent context that will serve all methods of this class.
     * @param fromPart      the estimated ready part of the total algorithm at the start of the subtask;
     *                      must be in <tt>0.0..1.0</tt> range.
     * @param toPart        the estimated ready part of the total algorithm at the finish of the subtask;
     *                      must be in <tt>fromPart..1.0</tt> range.
     * @throws NullPointerException     if the <tt>parentContext</tt> argument is {@code null}.
     * @throws IllegalArgumentException if <tt>fromPart</tt> or <tt>toPart</tt> is not in
     *                                  <tt>0.0..1.0</tt> range or if <tt>fromPart&gt;toPart</tt>.
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
     * Creates new context on the base of the passed <tt>parentContext</tt> and the range
     * <tt>from/total*100%..to/total*100%</tt>,
     * specified by integer values relatively some "total" number of operations.
     * More precisely, equivalent to the following call:
     * <pre>
     * new {@link #SubtaskContext(Context, double, double) SubtaskContext}(parentContext,
     * &#32;   (double)from/(double)total,
     * &#32;   to==total ? 1.0: (double)to/(double)total)
     * </pre>
     * excepting the case <tt>from=to=total=0</tt>, when it is equivalent to
     * <pre>
     * new {@link #SubtaskContext(Context, double, double) SubtaskContext}(0.0, 1.0)
     * </pre>
     *
     * @param parentContext the parent context that will serve all methods of this class.
     * @param from  the estimated ready part, from 0 to <tt>total</tt>,
     *              of the total algorithm at the start of the subtask.
     * @param to    the estimated ready part, from 0.0 to <tt>total</tt>,
     *              of the total algorithm at the finish of the subtask.
     * @param total the number of some operation in the full task.
     * @throws IllegalArgumentException if <tt>from</tt> or <tt>to</tt> is not in <tt>0..total</tt> range,
     *                                  or if <tt>from&gt;to</tt>, or if <tt>total&lt;0</tt>.
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
            this.fromPart = (double)from / (double)total;
            this.toPart = to == total ? 1.0 : (double)to / (double)total;
        }
    }

    /**
     * See the <a href="#specification">the detailed specification</a> in the comments to this class.
     *
     * @param contextClass the class of returned object (or superclass, or implemented interface).
     * @return             the required context.
     * @throws NullPointerException        if <tt>contextClass</tt> is {@code null}.
     * @throws IllegalArgumentException    if <tt>contextClass</tt> does not extends or implements
     *                                     {@link Context} interface.
     * @throws UnsupportedContextException if the parent context cannot serve the request.
     */
    public final <T extends Context> T as(Class<T> contextClass) {
//        System.out.println("Calling as(" + contextClass + ") in " + this.getClass() + ", parent = " + parentContext);
        T castedParentContext = parentContext.as(contextClass);
        if (contextClass == ProgressUpdater.class) {
            return contextClass.cast(new ProgressUpdaterSubtaskContext((ProgressUpdater)castedParentContext));
        } else {
            return contextClass.cast(Proxy.newProxyInstance(castedParentContext.getClass().getClassLoader(),
                new Class<?>[] {contextClass}, new SubtaskInvocationHandler(castedParentContext)));
        }
    }

    /**
     * Fully equivalent to <tt>parentContext.is(contextClass)</tt>, where <tt>parentContext</tt> is the
     * constructor argument.
     *
     * @param contextClass the class or interface of a context.
     * @return             <tt>true</tt> if this context class can be processed by {@link #as(Class)} method.
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
                    return SubtaskContext.this.as((Class)args[0]);
                }
            }
            try {
                return method.invoke(castedParentContext, args);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new AssertionError("Internal error in " + SubtaskContext.class).initCause(e);
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
