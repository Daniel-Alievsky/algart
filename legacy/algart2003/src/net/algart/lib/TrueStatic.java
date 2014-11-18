/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib;

/**
 * <p>A marker interface that disables overloading classes by
 * {@link Reflection.DynamicClassOverloader DCO} system.
 *
 * <p>More exactly, if some class (for example, <code>MyClass</code>)
 * implements <code>TrueStatic</code> interface, then:<ul>
 * <li>this class <code>MyClass</code>,
 * <li>all its <i>inner classes</i> (classes declared
 * as members of <code>MyClass</code>, and members of that members, etc.),
 * <li><i>subclasses</i> of this class (extending <code>MyClass</code>),
 * <li>and also <i>inner classes</i> of <i>subclasses</i> of this class
 * </ul>are never overloaded by DCO.
 *
 * <p>Moreover, it's enough class to implement not exactly
 * this interface, but any interface named "<code>TrueStatic</code>"
 * and located in any package. Such classes, its inner classes,
 * subclasses and inner classes of subclasses are never
 * overloaded by {@link Reflection.DynamicClassOverloader DCO}
 * system also.
 *
 * @see Reflection.DynamicClassOverloader
 *
 * @version 1.0
 * @author  Daniel Alievsky
 * @since   JDK1.1
 */

public interface TrueStatic {
}