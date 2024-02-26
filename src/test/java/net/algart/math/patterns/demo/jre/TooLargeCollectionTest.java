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

package net.algart.math.patterns.demo.jre;

import java.util.Collection;
import java.util.Locale;

/**
 * <p>Simple test that allocates very large Java collections (<tt>List</tt> or <tt>Set</tt>).
 * Illustrates correct (or incorrect) error messages while creating more than 2^31 elements.
 * Should be called in 64-bit JVMs.</p>
 *
 * @author Daniel Alievsky
 */
@SuppressWarnings("unchecked")
public class TooLargeCollectionTest {
    static final long NUMBER_OF_VALUES = 4500000000L;
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: " + TooLargeCollectionTest.class.getName()
                + " ArrayList|LinkedList|HashSet|TreeSet");
            return;
        }
        Collection<Long> values = (Collection<Long>)Class.forName("java.util." + args[0]).newInstance();
        for (long k = 0; k < NUMBER_OF_VALUES; k++) {
            values.add(k);
            if (k % 1048576 == 0) {
                Runtime rt= Runtime.getRuntime();
                long used = rt.totalMemory() - rt.freeMemory();
                System.out.printf(Locale.US, "\r%d: %.1f%% (%.3f MB, %.3f bytes/element)",
                    k + 1, (k + 1.0) * 100.0 / NUMBER_OF_VALUES, used / 1048576.0, used / (k + 1.0));
            }
        }
        System.out.println("The miracle: " + values.size() + "=" + NUMBER_OF_VALUES + " elements are allocated");
    }
}
