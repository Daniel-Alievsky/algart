package net.algart.contexts;

import net.algart.arrays.*;

/**
 * <p>The context informing the module, working with {@link net.algart.arrays AlgART arrays},
 * about the preferred {@link ThreadPoolFactory thread pool factory}.</p>
 *
 * <p>Any module, that need to process AlgART arrays in several parallel threads
 * to improve performance on the multiprocessor systems,
 * should request this context to get the preferred
 * {@link ThreadPoolFactory thread pool factory} and use it.</p>
 *
 * <p>One of examples of using this context is {@link DefaultArrayContext} class,
 * that can be passed to {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayThreadPoolContext extends Context {

    /**
     * Returns the {@link ThreadPoolFactory thread pool factory} that should be used
     * for multithreading parallel processing AlgArt arrays.
     *
     * @return the desired thread pool factory.
     */
    public ThreadPoolFactory getThreadPoolFactory();
}
