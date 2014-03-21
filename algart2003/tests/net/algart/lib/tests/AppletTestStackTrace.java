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

import java.io.*;
import java.awt.*;
import java.applet.*;
import net.algart.lib.*;

public class AppletTestStackTrace extends Applet {
    TextArea textArea = new TextArea(20, 80);
    static class MyTestException extends Exception {
        public MyTestException() {
            super("Test exception");
        }
    }

    static class Nested {
        static String s;
        static {
            GlobalProperties.setBooleanProperty(GlobalProperties.JAVA_NATIVE_ENABLED_PROPERTY_NAME, false);
            Out.setPrintDelay(3000);
            s = "Java " + System.getProperty("java.version") + "\n"
                + "Vendor " + System.getProperty("java.vendor") + "\n"
                + "Vendor URL " + System.getProperty("java.vendor.url") + "\n"
                              + "VM version " + System.getProperty("java.vm.version") + "\n"
//                              + "VM vendor " + System.getProperty("java.vm.vendor") + "\n"
//                              + "VM name " + System.getProperty("java.vm.name") + "\n"
//                              + "VM specification version " + System.getProperty("java.vm.specification.version") + "\n"
//                              + "VM specification vendor " + System.getProperty("java.vm.specification.vendor") + "\n"
//                              + "VM specification name " + System.getProperty("java.vm.specification.name") + "\n"
//                              + "Java specification version " + System.getProperty("java.specification.version") + "\n"
//                              + "Java specification vendor " + System.getProperty("java.specification.vendor") + "\n"
//                              + "Java specification name " + System.getProperty("java.specification.name") + "\n"
                + "Class version " + System.getProperty("java.class.version") + "\n"
                + "OS name " + System.getProperty("os.name") + "\n"
                + "OS arch " + System.getProperty("os.arch") + "\n"
                + "OS version " + System.getProperty("os.version") + "\n"
// commented properties are unavailable under Microsoft Internet Explorer
                + (JVM.JAVA_11_REFLECTION_SUPPORTED ? "Java 1.1 reflection is supported\n" : "")
                + (JVM.JAVA_11_SERIALIZATION_SUPPORTED ? "Java 1.1 serialization is supported\n" : "")
                + (JVM.JAVA_12_COMPARABLE_SUPPORTED ? "Java 1.2 Comparable is supported\n" : "")
                + (JVM.JAVA_12_COLLECTIONS_SUPPORTED ? "Java 1.2 collections are supported\n" : "")
                ;

//      try {
//        s += "All properties:\n" + System.getProperties() + "\n";
//      } catch (Exception e) {
//        s += e + "\n";
//      }
            try {
                throw new MyTestException();
            } catch (Exception e) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(out, true));
                byte[] bytes = out.toByteArray();
                char[] chars = new char[bytes.length];
                for (int k = 0; k < bytes.length; k++)
                    chars[k] = (char)bytes[k];
                // this code provides compatibility with Java 1.0 and avoids deprecation warnings
                s += String.valueOf(chars) + "\n";
            }

            try {
                s += "We are " + (JVM.isInStaticInitialization(AppletTestStackTrace.class) ? "IN" : "OUT of")
                    + " the AppletTestStackTrace static class initialization\n";
                s += "We are " + (JVM.isInStaticInitialization(Nested.class) ? "IN" : "OUT of")
                    + " the AppletTestStackTrace.Nested static class initialization\n";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Construct the applet
    public AppletTestStackTrace() {
        textArea.setText(Nested.s);
        this.add(textArea);
        Out.println("Hello from the " + Out.class +"!");
        Memory.gc(true, true);
    }
}

