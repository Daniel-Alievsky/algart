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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

public class AUndoText {

  public AUndoText() {
  }
  public AUndoText(Document document) {
    add(document);
  }

  public static final String UNDO_ACTION= "Undo";
  public static final String REDO_ACTION= "Redo";

  public final UndoAction undoAction= new UndoAction();
  public final RedoAction redoAction= new RedoAction();
  public final UndoableEditListener undoHandler= new UndoHandler();
  protected UndoManager undo= new UndoManager();

  public void add(Document document) {
    document.addUndoableEditListener(undoHandler);
  }
  public void remove(Document document) {
    document.removeUndoableEditListener(undoHandler);
  }
  public void reset() {
    undo.discardAllEdits();
    undoAction.update();
    redoAction.update();
  }

  public final JTextComponent.KeyBinding[] undoBindings = {
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_MASK),
      UNDO_ACTION),
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK),
      REDO_ACTION),
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
      UNDO_ACTION),
    new JTextComponent.KeyBinding(
      KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
      REDO_ACTION),
  };
  public final Action[] undoActions= {
    undoAction,
    redoAction,
  };


  public class UndoAction extends AbstractAction {
    public UndoAction() {
      super(UNDO_ACTION);
      setEnabled(false);
    }
    public void actionPerformed(ActionEvent e) {
      try {
        undo.undo();
      } catch (CannotUndoException ex) {
        Toolkit.getDefaultToolkit().beep();
      }
      update();
      redoAction.update();
    }
    protected void update() {
      if(undo.canUndo()) {
        setEnabled(true);
        putValue(Action.NAME, undo.getUndoPresentationName());
      } else {
        setEnabled(false);
        putValue(Action.NAME, UNDO_ACTION);
      }
    }
  }
  public class RedoAction extends AbstractAction {
    public RedoAction() {
      super(REDO_ACTION);
      setEnabled(false);
    }
    public void actionPerformed(ActionEvent e) {
      try {
        undo.redo();
      } catch (CannotRedoException ex) {
        Toolkit.getDefaultToolkit().beep();
      }
      update();
      undoAction.update();
    }
    protected void update() {
      if(undo.canRedo()) {
        setEnabled(true);
        putValue(Action.NAME, undo.getRedoPresentationName());
      } else {
        setEnabled(false);
        putValue(Action.NAME, REDO_ACTION);
      }
    }
  }
  public class UndoHandler implements UndoableEditListener {
    public void undoableEditHappened(UndoableEditEvent e) {
      undo.addEdit(e.getEdit());
      undoAction.update();
      redoAction.update();
    }
  }
}
