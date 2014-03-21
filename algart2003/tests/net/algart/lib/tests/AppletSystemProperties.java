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

import java.util.*;
import java.awt.*;
import java.applet.*;

/**
 * <p>Applet showing all available system properties.
 * Can be also called as a Java application.</p>
 *
 * @author Daniel Alievsky
 * @version 1.0
 * @since JDK 1.0
 */
public class AppletSystemProperties extends Applet {
    private TextArea textArea = new TextArea("", 30, 100);
    private static String[] keys = {
        "java.version",
        "java.vendor",
        "java.vendor.url",
        "java.home",
        "java.vm",
        "java.vm.specification.version",
        "java.vm.specification.vendor",
        "java.vm.specification.name",
        "java.vm.version",
        "java.vm.vendor",
        "java.vm.name",
        "java.specification.version",
        "java.specification.vendor",
        "java.specification.name",
        "java.class.version",
        "java.class.path",
        "java.library.path",
        "java.io.tmpdir",
        "java.compiler",
        "java.ext.dirs",
        "os.name",
        "os.arch",
        "os.version",
        "file.separator",
        "path.separator",
        "line.separator",
        "user.name",
        "user.home",
        "user.dir",
        "sun.arch.data.model",
    };

    public void init() {
// Commented together with Java 1.0 code from start() method
//        this.setLayout(new BorderLayout(0, 0));
        this.add(textArea);
    }

    public void start() {
// Below is Java 1.0 code, commented to remove deprecation warning
//        textArea.move(0, 0);
//        textArea.resize(this.size());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, textArea.getFont().getSize()));
        textArea.setText(getAllPropertiesInfo());
    }

    static String getAllPropertiesInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("Standard system properties by names:").append(LS);
        for (int k = 0; k < keys.length; k++) {
            sb.append(rightPad(keys[k], 40, ' '));
            String prop;
            try {
                prop = formatProperty(System.getProperty(keys[k]));
            } catch (Exception ex) {
                prop = ex.toString();
            }
            sb.append(prop).append(LS);
        }
        sb.append(LS).append(rightPad("Line separator used in PrintStream:", 40, ' '));
        sb.append(formatProperty(LS)).append(LS);
        sb.append(LS).append("All system properties:").append(LS);
        Properties allProp = null;
        try {
            allProp = System.getProperties();
            for (Enumeration en = allProp.keys(); en.hasMoreElements(); ) {
                Object key = en.nextElement();
                Object prop = allProp.get(key);
                sb.append(rightPad(String.valueOf(key), 40, ' '));
                sb.append(formatProperty(String.valueOf(prop))).append(LS);
            }
        } catch (Exception ex) {
            sb.append(ex.toString());
        }
        return sb.toString();
    }

    private static String formatProperty(String prop) {
        StringBuffer sb = new StringBuffer("\"");
        for (int j = 0; j < prop.length(); j++) {
            char ch = prop.charAt(j);
            if (ch < 32)
                sb.append("[ASCII " + (int)ch + "]");
            else
                sb.append(ch);
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String rightPad(String s, int length, char pad) {
        return s.length() > length ? s : s + dup(pad, length - s.length());
    }

    private static String dup(char c, int count) {
        char[] ca = new char[count];
        for (int k = 0; k < count; k++)
            ca[k] = c;
        return new String(ca);
    }

    private static String LS = getLineSeparator();
    private static String getLineSeparator() {
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream(8);
        java.io.PrintStream printStream = new java.io.PrintStream(byteArrayOutputStream);
        printStream.println();
        printStream.flush();
        printStream.close();
        byte[] bytes = byteArrayOutputStream.toByteArray();
        char[] chars = new char[bytes.length];
        for (int k = 0; k < bytes.length; k++)
            chars[k] = (char)(bytes[k] & 0xFF);
        // such a code provides compatibility with Java 1.0 and avoids deprecation warnings
        return String.valueOf(chars);
    }

    static final long serialVersionUID = 2981216605734561298L;

    public static void main(String[] args) {
        System.out.print(getAllPropertiesInfo());
    }
}
