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

package net.algart.lib.tests.performance;

import net.algart.lib.*;

public class ThrowAndCatchSpeed {
  static final int NUM = 100000;
  static String info() {
    return new Integer(157).toString();
  }
  static String infoThrow() {
    try {
      throw new Throwable();
    } catch (Throwable e) {
      return e.toString();
    }
  }

    public ThrowAndCatchSpeed() {
    }

    static String infoStackTrace() {
    try {

      throw new Throwable();
    } catch (Throwable e) {
      return e.getStackTrace().length + " elements";
    }
  }

  public static void main(String[] argv) {
    long t1,t2;
    String info = "";
    t1 = Timing.timens();
    for (int i = 0; i < NUM; i++) {
      info = info();
      info = info();
      info = info();
      info = info();
      info = info();
    }
    t2 = Timing.timens();
    Out.println(Out.rightPad("some info:", 40) + (t2-t1)/(5*NUM) + " ns, " + info);

    t1 = Timing.timens();
    for (int i = 0; i < NUM; i++) {
      info = infoThrow();
      info = infoThrow();
      info = infoThrow();
      info = infoThrow();
      info = infoThrow();
    }
    t2 = Timing.timens();
    Out.println(Out.rightPad("with throw/catch:", 40) + (t2-t1)/(5*NUM) + " ns, " + info);

    t1 = Timing.timens();
    for (int i = 0; i < NUM; i++) {
      info = infoStackTrace();
      info = infoStackTrace();
      info = infoStackTrace();
      info = infoStackTrace();
      info = infoStackTrace();
    }
    t2 = Timing.timens();
    Out.println(Out.rightPad("with throw/catch/getStackTrace():", 40) + (t2-t1)/(5*NUM) + " ns, " + info);
  }
}