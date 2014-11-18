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

package net.algart.swing.tests;

import javax.swing.*;
import net.algart.swing.*;

public class AJFileChooserTest {
    public static void main(String[] args) {
        boolean algart = false;
        if (args.length == 0
            || !(args[0].equals("std") || (algart = args[0].equals("algart")))) {
            System.out.println("Usage: " + AJFileChooserTest.class.getName() + " std|algart");
            return;
        }
        JFileChooser fc = algart? new AJFileChooser(): new JFileChooser();
        if (algart) {
            AJFileChooser afc = (AJFileChooser)fc;
            afc.tryToFindFilesListAndCorrectKeyboardBug();
            afc.tryToAvoidSlowingProblemInLargeDirectories();
            afc.tryToRemoveSingleClickListeners();
            afc.setTraversingLinksEnabled(true);
        }
        fc.showOpenDialog(new JFrame());
        System.out.println(fc.getSelectedFile());
        System.exit(0);
    }
}