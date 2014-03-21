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
class SomeClassByte {
  byte v;
}
class SomeClassDouble {
  double v;
}
class SomeClassInt {
  int v;
}
class SomeClassInt3 {
  int a,b,c;
}
class SomeClassInt4 {
  int a,b,c,d;
}
class SomeClassInt5 {
  int a,b,c,d,e;
}
class SomeClassByteArray16 {
  byte v[]= new byte[16];
}

public class CommonMemory {
  int NUM = 1024;

  public CommonMemory(int NUM) {
    this.NUM= NUM;
  }
  public CommonMemory() {
    this(1024);
  }

  private void output(String s) {
    System.out.print(s);
  }

  void freeall() {
    Runtime.getRuntime().runFinalization();
    System.gc();
  }

  public void exec() throws InterruptedException {
    int size= NUM*1024;
    final int PAUSE= 5000;

    output ("\n");
    output ("Free memory: "+Runtime.getRuntime().freeMemory()+"b\n");
    Thread.sleep(2*PAUSE);
    output ("Allocating byte["+size+"]... ");
    byte[] bytea= new byte[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing byte["+size+"]... ");
    bytea= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating int["+size+"]... ");
    int[] inta= new int[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing int["+size+"]... ");
    inta= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating double["+size+"]... ");
    double[] doublea= new double[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing double["+size+"]... ");
    doublea= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassByte["+size+"]... ");
    SomeClassByte[] someClassBytea= new SomeClassByte[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassBytea[k]= new SomeClassByte();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassByte["+size+"]... ");
    someClassBytea= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassInt["+size+"]... ");
    SomeClassInt[] someClassInta= new SomeClassInt[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassInta[k]= new SomeClassInt();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassInt["+size+"]... ");
    someClassInta= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassDouble["+size+"]... ");
    SomeClassDouble[] someClassDoublea= new SomeClassDouble[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassDoublea[k]= new SomeClassDouble();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassDouble["+size+"]... ");
    someClassDoublea= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassInt3["+size+"]... ");
    SomeClassInt3[] someClassInt3a= new SomeClassInt3[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassInt3a[k]= new SomeClassInt3();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassInt3["+size+"]... ");
    someClassInt3a= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassInt4["+size+"]... ");
    SomeClassInt4[] someClassInt4a= new SomeClassInt4[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassInt4a[k]= new SomeClassInt4();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassInt4["+size+"]... ");
    someClassInt4a= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassInt5["+size+"]... ");
    SomeClassInt5[] someClassInt5a= new SomeClassInt5[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassInt5a[k]= new SomeClassInt5();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassInt5["+size+"]... ");
    someClassInt5a= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Allocating SomeClassByteArray16["+size+"]... ");
    SomeClassByteArray16[] someClassByteArray16a= new SomeClassByteArray16[size];
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b);");
    Thread.sleep(PAUSE);
    output (" creating elements... ");
    for (int k=0; k<size; k++) someClassByteArray16a[k]= new SomeClassByteArray16();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(PAUSE);

    output ("Freeing SomeClassByteArray16["+size+"]... ");
    someClassByteArray16a= null; freeall();
    output ("OK ("+Runtime.getRuntime().freeMemory()+"b)\n");
    Thread.sleep(2*PAUSE);

  }

  public static void main(String[] args) throws InterruptedException {
    (args.length==0?new CommonMemory():new CommonMemory(Integer.parseInt(args[0]))).exec();
  }
}
