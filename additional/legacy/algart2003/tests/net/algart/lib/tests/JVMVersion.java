/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib.tests;

import net.algart.lib.*;

public class JVMVersion {
    public static void main(String[] argv) {
        Out.println(
            "Java " + System.getProperty("java.version") + "\n"
            + "Vendor " + System.getProperty("java.vendor") + "\n"
            + "Vendor URL " + System.getProperty("java.vendor.url") + "\n"
            + "VM version " + System.getProperty("java.vm.version") + "\n"
            + "VM vendor " + System.getProperty("java.vm.vendor") + "\n"
            + "VM name " + System.getProperty("java.vm.name") + "\n"

//            + "VM specification version " + System.getProperty("java.vm.specification.version") + "\n"
//            + "VM specification vendor " + System.getProperty("java.vm.specification.vendor") + "\n"
//            + "VM specification name " + System.getProperty("java.vm.specification.name") + "\n"
//            + "Java specification version " + System.getProperty("java.specification.version") + "\n"
//            + "Java specification vendor " + System.getProperty("java.specification.vendor") + "\n"
//            + "Java specification name " + System.getProperty("java.specification.name") + "\n"

            + "Class version " + System.getProperty("java.class.version") + "\n"
            + "OS name " + System.getProperty("os.name") + "\n"
            + "OS arch " + System.getProperty("os.arch") + "\n"
            + "OS version " + System.getProperty("os.version") + "\n"
// commented properties are unavailable under Microsoft Internet Explorer
            + (JVM.JAVA_11_REFLECTION_SUPPORTED ? "Java 1.1 reflection is supported\n" : "")
            + (JVM.JAVA_11_SERIALIZATION_SUPPORTED ? "Java 1.1 serialization is supported\n" : "")
            + (JVM.JAVA_12_COMPARABLE_SUPPORTED ? "Java 1.2 Comparable is supported\n" : "")
            + (JVM.JAVA_12_COLLECTIONS_SUPPORTED ? "Java 1.2 collections are supported\n" : "")
            );
    }
}
