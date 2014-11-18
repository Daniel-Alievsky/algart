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

package net.algart.lib.tests.reflection.classfromfile;

import java.io.*;
import net.algart.lib.*;

public class TestForHotReloading {
  public class Sub {
    public String toString(int a) {
      return "SubSubSub"+a;
    }
    public String toString() {
      return "SubSub";
    }
  }
  static Object thisClass,subClass;
  public String toString() {
    if (this.getClass()!=thisClass) {
      thisClass= this.getClass();
      Out.println("this class structure address changed: @"
        +Integer.toHexString(thisClass.hashCode()).toUpperCase());
    }
    if (Sub.class!=subClass) {
      subClass= Sub.class;
      Out.println("Sub class structure address changed: @"
        +Integer.toHexString(subClass.hashCode()).toUpperCase());
    }
    return "HaHaHa\n"+(new Sub());
  }
  public static void main(String[] args) throws java.io.IOException {
    try {
      java.io.DataInputStream  dis= new java.io.DataInputStream(System.in);
      // dis.readLine();
      // - some deprecated instruction for testing compiler, please uncomment to test
      Out.println("Hello");
      throw new NullPointerException();
    } finally {
      try {
        throw new ArrayIndexOutOfBoundsException();
      } catch (Exception e) {
        Out.println("Caught:  "+e);
      }
    }
  }
}
