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

import java.lang.ref.*;
import java.util.*;
import net.algart.lib.Timing;
import net.algart.lib.Out;

class SomeDataReference extends WeakReference {
  private final int id;
  SomeDataReference(SomeData o, ReferenceQueue rq) {
    super(o,rq);
    this.id= o.id;
  }
  public String toString() {
    return "SomeDataReference: "+id+" ("+this.get()+" - "
      +(SomeData.finalizedIds.contains(new Integer(id))?"":"NOT ")+"finalized)";
  }
}

class SomeData {
  static Set finalizedIds= new HashSet();

  byte[] data= new byte[25*1024];
  int id= SomeData.counter++;
  {
    System.out.print(this+" created: ");
    register();
  }

  public String toString() {return "SomeData: "+id;}

  private static int counter= 0;
  private static ReferenceQueue rq= new ReferenceQueue();
  private static Set allObjects= new HashSet();
  public static void deleteUnusedObjects() {
    System.out.println("**********");
    Reference r;
    int count= 0;
    while ((r=rq.poll())!=null) {
      SomeData o= (SomeData)r.get();
      if (o!=null) {
        System.out.println("Deleting "+o+" from ReferenceQueue");
        count++;
      } else {
        System.out.println("Removing passive reference "+r);
      }
      allObjects.remove(r);
    }
    System.out.println("********** "+count+" objects deleted; "+allObjects.size()+" objects remain");
  }
  private void register() {
    Reference r= new SomeDataReference(this,rq);
    System.out.println("Registering "+r);
    allObjects.add(r);
  }
  public static void printRefStatus() {
    System.out.println("\nAll weak references:");
    for (Iterator it= allObjects.iterator(); it.hasNext(); ) {
      System.out.println("  "+it.next());
    }
    System.out.println("\n");
  }
  protected void finalize() throws Throwable {
    finalizedIds.add(new Integer(id));
    try {
      System.out.println("Finalizing "+this);
    } finally {
      super.finalize();
    }
  }
}

public class GC {
  public static String memInfo() {
    Runtime rt= Runtime.getRuntime();
    return "Memory: "+Out.dec(rt.totalMemory()/1024.0/1024,4)+" Mb total = "
      +Out.dec((rt.totalMemory()-rt.freeMemory())/1024.0/1024,4)+" Mb used + "
      +Out.dec(rt.freeMemory()/1024.0/1024,4)+" Mb free; "
      +Out.dec(rt.maxMemory()/1024.0/1024,4)+" Mb max";
  }
  public static void main(String[] args) throws Exception {
    for (int k=0; k<=1000; k++) {
      SomeData o= new SomeData();
      if (k%30==0) SomeData.printRefStatus();
      if (k%20==0) SomeData.deleteUnusedObjects();
      if (k%150==0) {
        System.out.println("\n&&&& Forcing runFinalization  "+memInfo()+" &&&&");
        long t1= Timing.timems();
        System.runFinalization();
        long t2= Timing.timems();
        Thread.sleep(1300);
        System.out.println("&&&& runFinalization complete in "+(t2-t1)+" ms  "+memInfo()+" &&&&\n");
      }
      if (k%100==0) {
        System.out.println("\n&&&& Forcing GC  "+memInfo()+" &&&&");
        long t1= Timing.timems();
        System.gc();
        long t2= Timing.timems();
        System.out.println("&&&& GC complete in "+(t2-t1)+" ms  "+memInfo()+" &&&&\n");
      }
    }
  }
}