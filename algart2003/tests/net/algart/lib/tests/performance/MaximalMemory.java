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

import java.util.*;
import java.nio.*;

public class MaximalMemory {
  public static final int SIZE = 16*1024*1024;
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: MaximalMemoryTest array|direct");
      return;
    }
    boolean direct = args[0].equals("direct");
    List list = new ArrayList();
    long sum = 0;
    for (int k = 0; k < 2000; k++) {
      if (direct) {
        ByteBuffer a = ByteBuffer.allocateDirect(SIZE);
        sum += a.capacity();
        list.add(a);
        System.out.println(sum/(1024*1024) + " Mb allocated (ByteBuffer.allocateDirect(" + SIZE + "))");
      } else {
        byte[] a = new byte[SIZE];
        sum += a.length;
        list.add(a);
        System.out.println(sum/(1024*1024) + " Mb allocated (new byte[" + SIZE + "])");
      }
    }
  }
}