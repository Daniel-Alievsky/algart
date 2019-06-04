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
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
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
