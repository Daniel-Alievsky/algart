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

@Deprecated
public class AJOptionPane extends JOptionPane {

  public AJOptionPane() {
    super();
  }
  public AJOptionPane(Object message) {
    super(message);
  }
  public AJOptionPane(Object message, int messageType) {
    super(message,messageType);
  }
  public AJOptionPane(Object message, int messageType, int optionType) {
    super(message,messageType,optionType);
  }
  public AJOptionPane(Object message, int messageType, int optionType, Icon icon) {
    super(message,messageType,optionType,icon);
  }
  public AJOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options) {
    super(message,messageType,optionType,icon,options);
  }
  public AJOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue) {
    super(message,messageType,optionType,icon,options,initialValue);
  }
}