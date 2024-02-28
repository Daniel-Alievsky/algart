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

import net.algart.lib.Timing;
import net.algart.lib.Ma;
import net.algart.lib.Out;

public class TimingTest {

  public static void main(String[] args) {
    for (int k=0; k<100; k++) Out.println(Timing.timems()+"ms "+Timing.timens()+"ns "+Timing.timecpu()+"cycles");
    Out.println();
    byte a=12;
    byte b=-1;
    byte c,d;
    long la;
    for (int k=0; k<10; k++) {
      long t1= Timing.timens();
      long t2= Timing.timens();
      c= Ma.maxu(a,b);
      long t3= Timing.timens();
      d= Ma.minu(a,b);
      long t4= Timing.timens();
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      long t5= Timing.timens();
      for (int j=0; j<200000; j++) {
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
      }
      long t6= Timing.timens();
      for (int j=0; j<2000; j++) {
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
        la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens(); la= Timing.timens();
      }
      long t7= Timing.timens();
      Out.println("Timer.timens()-Timer.timens()="+Out.pad(t2-t1,5)+"ns; "
        +"maxb(="+c+"):"+Out.pad(t3-t2,5)+" minb(="+d+"):"+Out.pad(t4-t3,5)
        +" 50maxb:"+Out.pad((t5-t4)/50,4)+" 1E7minb:"+Out.dec((t6-t5)/1E7,0,3)
        +" 1E5Timer.timens:"+Out.dec((t7-t6)/1E5,0,3));
    }
    Out.println();
    for (int k=0; k<10; k++) {
      long t1= Timing.timecpu();
      long t2= Timing.timecpu();
      c= Ma.maxu(a,b);
      long t3= Timing.timecpu();
      d= Ma.minu(a,b);
      long t4= Timing.timecpu();
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b); c= Ma.maxu(a,b);
      long t5= Timing.timecpu();
      for (int j=0; j<200000; j++) {
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
        d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b); d= Ma.minu(a,b);
      }
      long t6= Timing.timecpu();
      for (int j=0; j<2000; j++) {
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
        la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu(); la= Timing.timecpu();
      }
      long t7= Timing.timecpu();
      Out.println("Timer.timecpu()-Timer.timecpu()="+Out.pad(t2-t1,5)+"cpu; "
        +"maxb(="+c+"):"+Out.pad(t3-t2,5)+" minb(="+d+"):"+Out.pad(t4-t3,5)
        +" 50maxb:"+Out.pad((t5-t4)/50,4)+" 1E7minb:"+Out.dec((t6-t5)/1E7,0,3)
        +" 1E5Timer.timecpu:"+Out.dec((t7-t6)/1E5,0,3));
    }
    Out.println();
    for (int k=0; k<10; k++) {
      for (long tfix=Timing.timens()>>28; Timing.timens()>>28==tfix; );
      long t1= Timing.timecpu();
      for (long tfix=Timing.timens()>>28; Timing.timens()>>28==tfix; );
      long t2= Timing.timecpu();
      Out.println(Ma.round(Timing.diffcpu(t1,t2)*1E9d/(1<<28))+" CPU cycles per second");
    }
  }
}