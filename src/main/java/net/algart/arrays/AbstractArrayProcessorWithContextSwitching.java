/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>A skeletal implementation of the {@link ArrayProcessorWithContextSwitching} interface.
 * Usually, you need to extend this class to implement that interface.</p>
 *
 * <p>This class stores the context, passed to the constructor and returned by {@link #context()} method,
 * in an internal field. The {@link #context(ArrayContext newContext)} method, switching the context,
 * creates new instance of this class by standard <tt>clone()</tt> method of this object and then
 * changes the internal field (containing a reference to the current context) to the <tt>newContext</tt> value.
 * Please override {@link #context(ArrayContext newContext)} or the standard <tt>clone()</tt> method
 * if this algorithm is not suitable.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class AbstractArrayProcessorWithContextSwitching
    implements ArrayProcessorWithContextSwitching, Cloneable
{

    /**
     * Current execution context. It is returned by {@link #context()} method. May be <tt>null</tt>.
     */
    private ArrayContext context;

    /**
     * The memory model used by this instance for all operations.
     * Equal to {@link #context}.{@link ArrayContext#getMemoryModel() getMemoryModel()} if
     * <tt>{@link #context}!=null</tt>, in other case equal to
     * {@link SimpleMemoryModel#getInstance()}.
     */
    private MemoryModel memoryModel;

    /**
     * Creates an instance of this class with the given context.
     * The reference to the passed argument will be returned by {@link #context()} method.
     *
     * @param context the context used by this instance for all operations (may be <tt>null</tt>).
     */
    protected AbstractArrayProcessorWithContextSwitching(ArrayContext context) {
        setContext(context);
    }

    /**
     * <p>This method is implemented here via cloning this object
     * (by standard <tt>clone()</tt> call) and replacing the value of the field,
     * where a reference to the current context is stored, with <tt>newContext</tt> value.
     * This technique is suitable for most implementation. However, if you need, you can
     * override this method; maybe, it is enough to override <tt>clone()</tt> instead.
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another context.
     */
    public ArrayProcessorWithContextSwitching context(ArrayContext newContext) {
        AbstractArrayProcessorWithContextSwitching result;
        try {
            result = (AbstractArrayProcessorWithContextSwitching) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
        result.setContext(newContext);
        return result;
    }

    /**
     * This implementation of the method just returns a reference to <tt>newContext</tt> argument, passed
     * to the constructor.
     *
     * <p>This method works very quickly (it just returns a value of some private field).
     *
     * @return the current context used by this instance; may be <tt>null</tt>.
     */
    public final ArrayContext context() {
        return context;
    }

    /**
     * Returns the memory model used by this instance for all operations.
     * Equal to {@link #context()}.{@link ArrayContext#getMemoryModel() getMemoryModel()} if
     * <tt>{@link #context()}!=null</tt>, in other case equal to
     * {@link SimpleMemoryModel#getInstance()}.
     *
     * <p>This method works very quickly (it just returns a value of some private field).
     *
     * @return the memory model used by this instance for all operations; cannot be <tt>null</tt>.
     */
    public final MemoryModel memoryModel() {
        return memoryModel;
    }

    /**
     * <p>This method returns <tt><nobr>{@link #context()} == null ? null :
     * {@link #context()}.{@link ArrayContext#part(double, double) part}(fromPart, toPart))</nobr></tt>.
     * This operation is needful very often while implementing most array processors.
     *
     * @param fromPart the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the start of the subtask:
     *                 see {@link ArrayContext#updateProgress(net.algart.arrays.ArrayContext.Event)} method
     * @param toPart   the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the finish of the subtask:
     *                 see {@link ArrayContext#updateProgress(net.algart.arrays.ArrayContext.Event)} method;
     *                 must be not less than <tt>fromPart</tt> range.
     * @return         new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <tt>fromPart</tt> or <tt>toPart</tt> is not in
     *                                  <tt>0.0..1.0</tt> range or if <tt>fromPart&gt;toPart</tt>.
     */
    public final ArrayContext contextPart(double fromPart, double toPart) {
        ArrayContext c = context();
        return c == null ? null : c.part(fromPart, toPart);
    }

    /**
     * Sets new {@link #context} and {@link #memoryModel} fields according to the argument.
     * This method is called only immediately after allocating new instance,
     * so it does not violate immutability.
     *
     * @param context new context.
     */
    private void setContext(ArrayContext context) {
        this.context = context;
        this.memoryModel = context == null ? SimpleMemoryModel.getInstance() : context.getMemoryModel();
    }
}
