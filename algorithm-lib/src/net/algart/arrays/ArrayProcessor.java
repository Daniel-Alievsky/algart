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
 * <p>AlgART Laboratory 2007-2013</p>
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
