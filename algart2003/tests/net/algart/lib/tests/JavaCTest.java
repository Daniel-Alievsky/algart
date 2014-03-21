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

import java.io.*;
import java.util.*;

public class JavaCTest {

  public static String out,err;
  public static void main(String[] args) throws Exception {

    System.out.println(net.algart.lib.Out.join(args,";"));
/*
    ByteArrayOutputStream baos= new ByteArrayOutputStream();
    sun.tools.javac.Main compiler= new sun.tools.javac.Main(baos,"javac");
    System.out.println("Compiler call: "+compiler.compile(args));
    System.out.println("<<"+baos+">>"+compiler.getExitStatus());
*/
/*
    CharArrayWriter writer= new CharArrayWriter();
    int result= com.sun.tools.javac.Main.compile(args,new PrintWriter(writer,true));
    System.out.println("Compiler call: <<"
      +writer.toString()
      +">>"+result);
*/
///*
    Process p= Runtime.getRuntime().exec(net.algart.lib.Out.join(args," "));
    System.out.println(args[0]+" started");

    final InputStreamReader is=
      new InputStreamReader(p.getInputStream());
    final InputStreamReader es=
      new InputStreamReader(p.getErrorStream());
    final StringBuffer out= new StringBuffer();
    final StringBuffer err= new StringBuffer();
    new Thread() {
      public void run() {
        try {
          char[] buf= new char[32768];
          int len;
          while ((len=is.read(buf,0,buf.length))>=0) {
            out.append(buf,0,len);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }.start();
    new Thread() {
      public void run() {
        try {
          char[] buf= new char[32768];
          int len;
          while ((len=es.read(buf,0,buf.length))>=0) {
            err.append(buf,0,len);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }.start();

    int result= p.waitFor();
    System.out.println("Terminated");
    System.out.println("Compiler call: <<"
      +out+">>\nErrors:<<"+err+">>"+result);
//*/
  }
}