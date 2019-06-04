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

/**
 * <p>The context informing the module, working with {@link net.algart.arrays AlgART arrays},
 * about some <i>current folder</i> (usually a disk directory).</p>
 *
 * <p>You can use this context, if your application works with different subdirectories,
 * representing workplaces, projects or something like this.
 * The typical goal is to provide information about the preferred disk path, where your functions
 * should find necessary files.</p>
 *
 * <p>This package does not provide implementations of this context.
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public interface CurrentFolderContext extends Context {
    /**
     * Returns the path to the current folder.
     *
     * @return the current folder.
     */
    public String getCurrentFolder();
}
