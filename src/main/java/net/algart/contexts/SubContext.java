/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.MemoryModel;
import net.algart.arrays.SimpleMemoryModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>A tool allowing to build a new context, called "sub-context",
 * that inherits behavior of an existing context with some changes or extensions.</p>
 *
 * <p>There are two basic ways to create a sub-context.</p>
 *
 * <p>The first way is inheriting this class with using the protected constructor
 * {@link #SubContext(Context) SubContext(Context superContext)}.
 * New subclass cannot override {@link #as(Class)} and {@link #is(Class)}
 * methods, but may (and should) implement some additional specific context interfaces
 * (for example, {@link ProgressUpdater}, {@link ArrayMemoryContext} or your custom contexts).
 * The {@link #as(Class)} and {@link #is(Class)} method of the created sub-context
 * will just call these methods of the super-context, excepting the case
 * when the required <code>contextClass</code> is an interface implemented by your sub-context class.
 * In the last case, {@link #as(Class)} method will return the reference to your sub-context instance
 * and {@link #is(Class)} will return <code>true</code>.
 * This technique is useful when you need to extend or override some
 * functionality of the given context.</p>
 *
 * <p>The second way of creating sub-context is using the public constructor
 * {@link #SubContext(Context, Class[]) SubContext(Context superContext, Class ...allowedClasses)},
 * maybe, together with inheritance as in the first way.
 * In this case, {@link #as(Class)} and {@link #is(Class)} will also pass the request
 * to the super-context, as described above, if the required <code>contextClass</code>
 * is implemented by your sub-context class (in particular, if you do not extend
 * this class and just call the public constructor).
 * However, the <code>contextClass</code> will be checked, is it in the classes list
 * passed to the constructor. If <code>contextClass</code> is not in this list
 * (and is not implemented by your sub-context), it is considered as unallowed,
 * and the request is declined: {@link #as(Class)} throws an exception,
 * and {@link #is(Class)} method returns <code>false</code>.
 * This technique allows to restrict a set of passed contexts
 * by only well-known, safe contexts.</p>
 *
 * <p>Another constructors may provide another behavior: please see comments to that constructors.</p>
 *
 * @author Daniel Alievsky
 */
public class SubContext extends AbstractContext implements Context {
    private final Context superContext;
    private final HashSet<Class<?>> allowedClassesSet;
    private final MemoryModel memoryModel;

    /**
     * Creates new context on the base of the passed super-context.
     *
     * @param superContext super-context.
     */
    protected SubContext(Context superContext) {
        super(false); // the constructor argument does not matter: we override both as and is methods
        Objects.requireNonNull(superContext, "Null superContext argument");
        this.superContext = superContext;
        this.allowedClassesSet = null;
        this.memoryModel = null;
    }

    /**
     * Creates new context on the base of the passed super-context
     * with the restricted set of allowed context classes.
     *
     * @param superContext   super-context.
     * @param allowedClasses the set of served specific contexts
     *                       (in addition to interfaces implemented by this instance).
     * @throws NullPointerException     if <code>superContext</code> or one of <code>allowedClasses</code> is {@code null}.
     * @throws IllegalArgumentException if one of <code>allowedClasses</code> is not a {@link Context}.
     */
    public SubContext(Context superContext, Class<?> ...allowedClasses) {
        super(false); // the constructor argument does not matter: we override both as and is methods
        Objects.requireNonNull(superContext, "Null superContext argument");
        for (int k = 0; k < allowedClasses.length; k++) {
            Class<?> c = allowedClasses[k];
            Objects.requireNonNull(c, "Null allowedClasses[" + k + "] argument");
            if (!Context.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException("allowedClasses[" + k + "] does not inherit Context (" + c + ")");
            }
        }
        this.superContext = superContext;
        this.allowedClassesSet = new HashSet<>(Arrays.asList(allowedClasses));
        this.memoryModel = null;
    }

    /**
     * Creates new context alike the {@link #SubContext(Context) base constructor}
     * with the only difference that {@link #as(Class) as(ArrayMemoryContext.class)}
     * method will return a {@link ArrayMemoryContext} describing the passed memory model.
     * See comments to {@link #as(Class)} and {@link #is(Class)}
     * method for more details.
     *
     * @param superContext super-context.
     * @param memoryModel  desired memory model.
     * @throws NullPointerException if one of the arguments is {@code null}.
     */
    public SubContext(Context superContext, MemoryModel memoryModel) {
        super(false); // the constructor argument does not matter: we override both as and is methods
        Objects.requireNonNull(superContext, "Null superContext argument");
        Objects.requireNonNull(memoryModel, "Null memoryModel argument");
        this.superContext = superContext;
        this.allowedClassesSet = null;
        this.memoryModel = memoryModel;
    }

    /**
     * This implementation returns the reference to this instance, if
     * <code>contextClass.isAssignableFrom(thisInstance.getClass())</code>,
     * or calls <code>superContext.{@link #as(Class) as}(contextClass)</code>
     * in another case.
     *
     * <p>If this instance was created by the constructor with the specified set of allowed classes
     * ({@link #SubContext(Context, Class[])}),
     * and the condition <code>contextClass.isAssignableFrom(thisInstance.getClass())</code> is not fulfilled,
     * then context class is checked before passing to the <code>superContext.{@link #as(Class) as}</code> method.
     * Namely, if this class is not in the list of allowed contexts, passed to the constructor,
     * this method throws {@link UnsupportedContextException}.
     *
     * <p>If this instance was created by the constructor with the specified memory model
     * ({@link #SubContext(Context, MemoryModel)}), and if <code>contextClass==ArrayMemoryContext.class</code>, but
     * <code>!contextClass.isAssignableFrom(thisInstance.getClass())</code>,
     * then this method does not call <code>superContext</code>, but creates new implementation
     * of {@link ArrayMemoryContext} with {@link ArrayMemoryContext#getMemoryModel() getMemoryModel()},
     * {@link ArrayMemoryContext#getMemoryModel(Class) getMemoryModel(Class)} and
     * {@link ArrayMemoryContext#getMemoryModel(String) getMemoryModel(String)} methods,
     * returning the memory model specified in the constructor.
     * The second {@link ArrayMemoryContext#getMemoryModel(Class) getMemoryModel(Class)} method
     * will return {@link SimpleMemoryModel#getInstance()}, if the required element type
     * is not supported by the memory model specified in the constructor.
     *
     * @param contextClass the class of returned object (or superclass, or implemented interface).
     * @return             this instance.
     * @throws NullPointerException        if <code>contextClass</code> is {@code null}.
     * @throws IllegalArgumentException    if <code>contextClass</code> does not extends or implements
     *                                     {@link Context} interface.
     * @throws UnsupportedContextException if this instance does not implement or extend the required type.
     */
    public final <T extends Context> T as(Class<T> contextClass) {
        Objects.requireNonNull(contextClass, "Null contextClass argument");
        if (!Context.class.isAssignableFrom(contextClass)) {
            throw new IllegalArgumentException("The contextClass argument is not a context class ("
                + contextClass.getName() + ")");
        }
        if (contextClass.isAssignableFrom(getClass())) {
            return contextClass.cast(this);
        } else {
            if (allowedClassesSet != null && !allowedClassesSet.contains(contextClass)) {
                throw new UnsupportedContextException("Unallowed context class: " + contextClass.getName());
            }
            if (contextClass == ArrayMemoryContext.class && this.memoryModel != null) {
                return contextClass.cast(new MemorySubContext());
            }
            return superContext.as(contextClass);
        }
    }

    /**
     * This implementation returns <code>true</code> if <code>contextClass</code> is not {@code null}
     * and <code>contextClass.isAssignableFrom(thisInstance.getClass())</code>.
     * In another case, if this instance was created by the constructor with the specified set of allowed classes
     * ({@link #SubContext(Context, Class[])}) and if the passed context class is not
     * in the list of allowed contexts, passed to the constructor,
     * this implementation returns <code>false</code>.
     *
     * <p>If this instance was created by the constructor with the specified memory model
     * ({@link #SubContext(Context, MemoryModel)}),
     * and if <code>contextClass==ArrayMemoryContext.class</code>,
     * this implementation returns <code>true</code>.
     *
     * <p>In all other cases, this implementation returns
     * <code>superContext.{@link #is is}(contextClass)</code>.
     *
     * @param contextClass the class or interface of a sub-context.
     * @return             <code>true</code> if this context class can be processed by {@link #as(Class)} method.
     */
    public final boolean is(Class<? extends Context> contextClass) {
        if (contextClass == null || !Context.class.isAssignableFrom(contextClass)) {
            return false;
        }
        if (contextClass.isAssignableFrom(getClass())) {
            return true;
        }
        if (allowedClassesSet != null && !allowedClassesSet.contains(contextClass)) {
            return false;
        }
        if (contextClass == ArrayMemoryContext.class && this.memoryModel != null) {
            return true;
        }
        return superContext.is(contextClass);
    }

    private class MemorySubContext extends AbstractContext implements ArrayMemoryContext {
        private MemorySubContext() {
            super(false);
        }

        public MemoryModel getMemoryModel() {
            return memoryModel;
        }

        public MemoryModel getMemoryModel(Class<?> elementType) {
            return memoryModel.isElementTypeSupported(elementType) ? memoryModel : SimpleMemoryModel.getInstance();
        }

        public MemoryModel getMemoryModel(String settings) {
            Objects.requireNonNull(settings, "Null settings argument");
            return memoryModel;
        }
    }
}
