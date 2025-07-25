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

import net.algart.lib.*;

public class TempDirectoryTest {

  public static void main(String[] argv) {
    if (argv.length > 0) {
      Out.println("java.io.tmpdir will be " + argv[0]);
      System.setProperty("java.io.tmpdir",argv[0]);
    }
    Out.println("tempDirectory(): \"" + Directory.tempDirectory() + "\"");
    Out.print("createTempDirectory(): ");
    try {
      java.io.File f = Directory.createTempDirectory("tempdir","");
      f.deleteOnExit();
      Out.println("\"" + f + "\"");
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    Out.print("createTempFile(): ");
    try {
      java.io.File f = Directory.createTempFile("tempfile","");
      Out.println("\"" + f + "\"");
      f.deleteOnExit();
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    try {
      new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
    } catch (java.io.IOException e) {
    }
  }
}