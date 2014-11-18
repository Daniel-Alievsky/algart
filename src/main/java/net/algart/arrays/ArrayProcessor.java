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

/**
 * <p>Abstract array processor: an algorithm processing AlgART arrays or matrices.</p>
 *
 * <p>It is very abstract interface: it almost does not declare any functionality.
 * The only exception is the {@link ArrayContext array context}.
 * We suppose that any array processing algorithm should work in some context,
 * and this interface declares a method for getting the current execution context
 * ({@link #context()} method).
 * Usually, the context of execution is passed to constructor or instantiation method.</p>
 *
 * <p>Some array processors may have no current context. In this situation, {@link #context()}
 * method returns <tt>null</tt>.
 * However, we recommend to provide correct context always,
 * because the context allows to show execution progress to the user, interrupt execution
 * and increase performance on multiprocessor systems by multithread execution.
 * Also the context is necessary to specify the memory model used for allocation of AlgART array:
 * by default, most of array processors will use {@link SimpleMemoryModel}.
 *
 * <p>There is an extended version of this interface, {@link ArrayProcessorWithContextSwitching},
 * that also allows to change the current context.
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayProcessor {
    /**
     * Returns the current context used by this instance for all operations.
     * This method may return <tt>null</tt>; the classes, implementing this interface,
     * should work in this situation as while using {@link ArrayContext#DEFAULT} context.
     *
     * @return the current context used by this instance; may be <tt>null</tt>.
     */
    public ArrayContext context();
}
