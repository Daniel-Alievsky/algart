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

import java.beans.*;
import java.io.*;
import javax.swing.*;
import net.algart.lib.*;

class Xy {
    int x,y;
    Xy(int x, int y) {
        this.x = x; this.y = y;
    }
}

public class XMLEncoderTest {
    public int a, b;
    public XMLEncoderTest() {
        this(0,0);
    }
    public XMLEncoderTest(int a, int b) {
        this.a = a;
        this.b = b;
    }
    public int getA() {return a;}
    public int getB() {return b;}

    String serializationFile = "/tmp/ser.xml";
    String exec() throws Exception {
        Object[] o = {
            "somestring",
            new Integer(20),
            serializationFile,
            new Xy(2,5),
            new java.awt.Rectangle(1,2,3,4),
            new JButton("Hi"),
            new XMLEncoderTest(2,3),
        };
        File f = new File(serializationFile);
        XMLEncoder e = new XMLEncoder(
            new FileOutputStream(f));
        e.writeObject(o);
        for (int k = 0; k < o.length; k++) {
            System.err.println(
                "\nOutput " + o[k] + "...");
            e.writeObject(o[k]);
        }
        e.close();
        return TextIO.UTF8.read(f);
    }
    public static void main(String[] argv) throws Exception {
        Out.println("\n\nResults:\n" + new XMLEncoderTest().exec());
    }
}