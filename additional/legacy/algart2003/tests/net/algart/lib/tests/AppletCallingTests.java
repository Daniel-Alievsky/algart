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

import java.awt.*;
import java.applet.*;
import java.util.Vector;
import java.lang.reflect.*;

/**
 * <p>Applet allowing to run usual Java test: "main" static methods.</p>
 *
 * @author Daniel Alievsky
 */
public class AppletCallingTests extends Applet {

    private String mainClassName;
    private Vector params = new Vector();
    private String lastException;
    private boolean isException = false;

    public void init() {
        mainClassName = getParameter("mainClassName");
    }

    public void setMainClassName(String value) {
        mainClassName = value;
    }

    public void setParam(int index, String param) {
        if (index >= params.size())
            params.setSize(index + 1);
        params.set(index, param);
    }

    public int getParamCount() {
        return (int)params.size();
    }

    public String getParam(int index) {
        return (String)params.get(index);
    }

    public void callMainClass() {
        isException = false;
        String[] paramsArray = new String[params.size()];
        for (int k = 0; k < paramsArray.length; k++)
            paramsArray[k] = (String)params.elementAt(k);
        try {
            Class c = Class.forName(mainClassName);
            Method m = c.getMethod("main", new Class[] {String[].class});
            m.invoke(null, new Object[] {paramsArray});
        } catch (Throwable e) {
            isException = true;
            lastException = e instanceof InvocationTargetException ?
                ((InvocationTargetException)e).getTargetException().toString() :
                e.toString();
            e.printStackTrace();
        }
    }

    public boolean isException() {
        return this.isException;
    }

    public String lastException() {
        return this.lastException;
    }

    public void paint(Graphics g) {
        g.setColor(new Color(0xFFFFFFD0));
        g.fillRect(0, 0, getSize().width, getSize().height);
        g.setColor(new Color(0xFF000000));
        g.drawString("Current JVM: " + System.getProperty("java.version"), 4, 4 + g.getFontMetrics().getHeight());
        g.setColor(new Color(0xFF808080));
        g.drawString("The following applet methods should be called via JavaScript:", 4, 4 + 2 * g.getFontMetrics().getHeight());
        g.drawString("setMainClassName(String value)", 20, 4 + 3 * g.getFontMetrics().getHeight());
        g.drawString("setParam(int index, String param)", 20, 4 + 4 * g.getFontMetrics().getHeight());
        g.drawString("callMainClass()", 20, 4 + 5 * g.getFontMetrics().getHeight());
    }

    static final long serialVersionUID = -365501438837494796L;
}
