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

package net.algart.swing;

import javax.swing.*;
import java.awt.*;

@Deprecated
public class AJDialog extends JDialog {

    public AJDialog() throws HeadlessException {
        super();
    }

    public AJDialog(boolean modal) throws HeadlessException {
        super((Frame) null, modal);
    }

    public AJDialog(Frame owner) throws HeadlessException {
        super(owner);
    }

    public AJDialog(Frame owner, boolean modal) throws HeadlessException {
        super(owner, modal);
    }

    public AJDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title);
    }

    public AJDialog(Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
    }

    public AJDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    public AJDialog(Dialog owner) throws HeadlessException {
        super(owner);
    }

    public AJDialog(Dialog owner, boolean modal) throws HeadlessException {
        super(owner, modal);
    }

    public AJDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title);
    }

    public AJDialog(Dialog owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
    }

    public AJDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) throws HeadlessException {
        super(owner, title, modal, gc);
    }

}