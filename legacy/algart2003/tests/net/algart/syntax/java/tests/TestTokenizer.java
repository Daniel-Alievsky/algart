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

package net.algart.syntax.java.tests;

import java.io.*;
import java.util.*;

public class TestTokenizer {
 public static void main(String[] args) throws IOException {
    if (args.length<1) {
      System.out.println("Usage: TestTokenizer some_java_file");
      return;
    }
    File f= new File(args[0]);
    Reader r= new BufferedReader(new FileReader(f));
    StreamTokenizer stok= new StreamTokenizer(r);
    stok.eolIsSignificant(true);
    stok.slashSlashComments(true);
    stok.slashStarComments(true);
    stok.wordChars('_','_');
    while (stok.nextToken()!=StreamTokenizer.TT_EOF) {
      System.out.println(stok+"");
    }
    r.close();
  }
}
