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

import java.util.*;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;
import javax.accessibility.*;
import javax.swing.*;
import javax.swing.text.*;
import net.algart.lib.*;

import java.awt.print.*;
import javax.print.attribute.*;

public class ASwingUtilities {

  public static final JTextComponent.KeyBinding[] windowsClipboardBindings = {
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK),
      DefaultEditorKit.copyAction),
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK),
      DefaultEditorKit.pasteAction),
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK),
      DefaultEditorKit.cutAction),
  };

  public static void addWindowsClipboardKeys() {
    Keymap keymap= JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
    JTextComponent.loadKeymap(keymap, windowsClipboardBindings, (new JTextPane()).getActions());
  }

  public static Window[] getWindowsRecursive(Window w) {
    List allWindows= new ArrayList();
    getWindowsRecursive(w,allWindows);
    return (Window[])allWindows.toArray(new Window[0]);
  }
  public static List getWindowsRecursive(Window w, List allWindows) {
    allWindows.add(w);
    Window[] owned= w.getOwnedWindows();
    for (int k=0; k<owned.length; k++)
      getWindowsRecursive(owned[k],allWindows);
    return allWindows;
  }

  public static Component[] getComponentsRecursive(Component c) {
    List allComponents= new ArrayList();
    getComponentsRecursive(c,allComponents);
    return (Component[])allComponents.toArray(new Component[0]);
  }
  public static List getComponentsRecursive(Component c, List allComponents) {
    allComponents.add(c);
    if (!(c instanceof Container)) return allComponents;
    Component[] children= ((Container)c).getComponents();
    for (int k=0; k<children.length; k++)
      getComponentsRecursive(children[k],allComponents);
    return allComponents;
  }

  public static int getComponentDepth(Component c, Container root) {
    int result= 0;
    for (; c!=root; c=c.getParent()) {
      result++;
    }
    return result;
  }

  public static void addKeyListenerRecursive(Component c, KeyListener listener) {
    Component[] allComponents= getComponentsRecursive(c);
    for (int k=0; k<allComponents.length; k++)
      allComponents[k].addKeyListener(listener);
  }

  public static Accessible[] getAccessibleChildrenRecursive(Accessible a) {
    List allAccessibleChildren= new ArrayList();
    getAccessibleChildrenRecursive(a,allAccessibleChildren);
    return (Accessible[])allAccessibleChildren.toArray(new Accessible[0]);
  }
  public static List getAccessibleChildrenRecursive(Accessible a, List allAccessibleChildren) {
    allAccessibleChildren.add(a);
    AccessibleContext ac= a.getAccessibleContext();
    if (ac==null) return allAccessibleChildren;
    int n= ac.getAccessibleChildrenCount();
    for (int k=0; k<n; k++)
      getAccessibleChildrenRecursive(ac.getAccessibleChild(k),allAccessibleChildren);
    return allAccessibleChildren;
  }

  private static boolean printDialogVisible= false;
  public static boolean isPrintDialogVisible() {
    return printDialogVisible;
  }
  public static boolean showModalPrintDialog(PrinterJob printerJob, PrintRequestAttributeSet ps)
    throws java.awt.HeadlessException {
    printDialogVisible= true;
    boolean result= false;
    try {
      result= ps==null?printerJob.printDialog():printerJob.printDialog(ps);
    } finally {
      printDialogVisible= false;
    }
    return result;
  }
}