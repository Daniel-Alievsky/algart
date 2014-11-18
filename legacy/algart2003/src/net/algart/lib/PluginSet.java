/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2004 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p><code>PluginSet</code> interface is recommended to be used
 * in large applications as a tool for enumerating <i>plugin</i>
 * classes, i.e. classes that can be added to the application
 * as extensions. The application should use some mechanism to
 * enumerate all <code>PluginSet</code> classes that extend
 * the application with some plugin sets. For example,
 * META-INF/services/net.algart.lib.PluginSet file can
 * be used (please see <a
 * href="http://java.sun.com/j2se/1.4.2/docs/guide/sound/programmer_guide/chapter13.html">
 * &quot;Chapter 13: Introduction to the Service Provider Interfaces&quot;</a>
 * at java.sun.com site.
 *
 * @version 1.0
 * @author  Daniel Alievsky
 * @since   JDK1.0
 */

public interface PluginSet {

    /**
     * Returns all plugin classes in this set.
     */
    public Class[] getPlugins();

    /**
     * Returns the version of application-specific plugin subsystem
     * used by this plugin set. For example: "2.1" or "4.0.1".
     */
    public String getVersion();
}