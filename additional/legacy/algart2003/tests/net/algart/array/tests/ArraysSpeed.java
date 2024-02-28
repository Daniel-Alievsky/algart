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

package net.algart.array.tests;

import net.algart.array.Arrays;
import net.algart.lib.JVM;
import net.algart.lib.Out;
import net.algart.lib.Strings;
import net.algart.lib.Timing;

public class ArraysSpeed {
  static int NUM= 200*1024;
  static int NUMe1,NUMe2;
  static int SPEED= 4;
  static int NUMc= 64*1024/SPEED;
  static int ESTIMATED_TOTAL= 128*1024*1024/SPEED;
  static int TOTAL;
  static int[] BOFS_ARRAY_FOR_COPY= {0,1,2,4,8,12,16,24,32};
  static int[] BOFS_ARRAY_FOR_FILL= {0,1,2,4,16,24,32};

  private static int min(int a, int b)    {return a<=b? a: b;}
  private static int max(int a, int b)    {return a>=b? a: b;}
  private static long min(long a, long b) {return a<=b? a: b;}
  private static long max(long a, long b) {return a>=b? a: b;}
  private static int minu(int a, int b)   {return (a^0x80000000)<(b^0x80000000)? a: b;}
  private static int maxu(int a, int b)   {return (a^0x80000000)>(b^0x80000000)? a: b;}
  private static final java.util.Random rnd = new java.util.Random();
  private static int random(int n)        {return rnd.nextInt(n);}
  private static int randomInt()          {return rnd.nextInt();}


  static long cpuFreq= 0;
  public static void calcCpuFreq() {
    for (long tfix=Timing.timens()>>29; Timing.timens()>>29==tfix; );
    long t1= Timing.timecpu();
    for (long tfix=Timing.timens()>>29; Timing.timens()>>29==tfix; );
    long t2= Timing.timecpu();
    cpuFreq= Math.round(Timing.diffcpu(t1,t2)*1E9d/(1<<29));
  }

  public static String tInfo(long d1, long d2) {
    double tElem= Math.max(d1,1.)/NUM/NUMe1;
    return Out.dec(tElem*1E9/cpuFreq,3)+"ns="+Out.rightPad(Out.dec(tElem,3),7)+"cl/element, "
      +Out.dec(d2/NUMc*1E9/cpuFreq,3)+"ns="+Out.dec(d2/NUMc,3)+"cl(="
      +Out.dec(d2/tElem/NUMc,1)+"elements)/call";
  }

  public static Object randomTypeArray(int size) {
    switch (random(5)) {
      case 0: return Arrays.randomChars((size+1)/2);
      case 1: return Arrays.randomBytes(size);
      case 2: return Arrays.randomShorts((size+1)/2);
      case 3: return Arrays.randomInts((size+3)/4);
      case 4: return Arrays.randomLongs((size+7)/8);
    }
    return null;
  }

  public static void main(String[] args) throws Exception {
    Arrays.setNativeMinLenPairOp(0);
    Arrays.setNativeMinLenFill(0);

    if (!Arrays.isNative())
      Out.println("Native library was not loaded due to the following reason:\n  "
        + Arrays.initializationExceptionMessage());

    Out.println("\nTesting "+(!Arrays.isNative()?"Java":"native")+" code");
    Out.print("Checking CPU...");
    calcCpuFreq();
    Out.println("\rCPU family P"+Arrays.getCpuFamily()
      +((Arrays.getCpuInfo()&Arrays.CPU_AMD)==0?"":" AMD")
      +(Arrays.getCpuL1CacheSize()==0?"":"; "+Arrays.getCpuL1CacheSize()/1024+" KB L1 cache")
      +(Arrays.getCpuL2CacheSize()==0?"":"; "+Arrays.getCpuL2CacheSize()/1024+" KB L2 cache")
      +" (full 64-bit info: "+Out.hex(Arrays.getCpuInfo())+")");
    Out.println("CPU clock rate: "+cpuFreq+" cycles/sec");
    if (args.length<1) {
      Out.println("\nUsage: ArraysSpeed arraysize [nosse2|nommxex|nosse|nommx|java]");
      return;
    }
    for (int mode=0; mode< (args.length>1? 1: 6); mode++) {
      Arrays.resetCpuInfo();
      if (mode==1 && (Arrays.getCpuInfo()&Arrays.CPU_SSE2)==0) continue;
      if (mode==1 || (args.length>1 && args[1].toLowerCase().equals("nosse2"))) {
        Arrays.setCpuInfo(Arrays.getCpuInfo()&~Arrays.CPU_SSE2);
      }
      if (mode==2 && (Arrays.getCpuInfo()&Arrays.CPU_MMXEX)==0) continue;
      if (mode==2 || (args.length>1 && args[1].toLowerCase().equals("nommxex"))) {
        Arrays.setCpuInfo(Arrays.getCpuInfo()&~Arrays.CPU_MMXEX);
      }
      if (mode==3 && (Arrays.getCpuInfo()&Arrays.CPU_SSE)==0) continue;
      if (mode==3 || (args.length>1 && args[1].toLowerCase().equals("nosse"))) {
        Arrays.setCpuInfo(Arrays.getCpuInfo()&~Arrays.CPU_SSE);
      }
      if (mode==4 || (args.length>1 && args[1].toLowerCase().equals("nommx"))) {
        Arrays.setCpuInfo(Arrays.getCpuInfo()&~(Arrays.CPU_MMX|Arrays.CPU_CMOV));
      }
      if (mode==5 || (args.length>1 && args[1].toLowerCase().equals("java"))) {
        Arrays.setNative(false);
        NUMc*=4;
        ESTIMATED_TOTAL/=4;
      }

      NUM= Integer.parseInt(args[0]);
      if (NUM<120) {
        Out.println("arraysize is to small: must be >=120");
        return;
      }
      NUMe2= 1;
      NUMe1= ESTIMATED_TOTAL/NUM;
      if (NUMe1>32) {NUMe2= 32; NUMe1/=32;}
      TOTAL=NUM*NUMe1*NUMe2;

      final int COERCIVECOPY_TEST_COUNT= 20000000/NUM;
      final int COPY_TEST_COUNT= 500000/NUM;
      final int FILL_TEST_COUNT= 500000/NUM;

      Out.println("CPU characteristics: "
        +((Arrays.getCpuInfo()&Arrays.CPU_TSC)==0?"no ":"")+"precise timer, "
        +((Arrays.getCpuInfo()&Arrays.CPU_CMOV)==0?"no ":"")+"CMOV/FCMOV, "
        +((Arrays.getCpuInfo()&Arrays.CPU_MMX)==0?"no ":"")+"MMX, "
        +((Arrays.getCpuInfo()&Arrays.CPU_MMXEX)==0?"no ":"")+"MMX/PIII, "
        +((Arrays.getCpuInfo()&Arrays.CPU_SSE)==0?"no ":"")+"SSE, "
        +((Arrays.getCpuInfo()&Arrays.CPU_SSE2)==0?"no ":"")+"SSE2"
        +((Arrays.getCpuInfo()&Arrays.CPU_3DNOW)==0?"":", 3DNOW")
        +((Arrays.getCpuInfo()&Arrays.CPU_3DNOWEX)==0?"":", 3DNOW+"));
      Out.println("Length of arrays: "+NUM);
      Out.println(NUMe2+" test, "+NUMe1+" iterations/test, "+NUM+" elements in arrays");
      Out.println(COPY_TEST_COUNT+" copy tests, "+FILL_TEST_COUNT+" fill tests, "+COERCIVECOPY_TEST_COUNT+" coerciveCopy tests");


      long t1,d1,d2;
      int ao=1,bo=19;
      String n;
      {
        n= "byte";
        byte[] a= Arrays.randomBytes(NUM+64);
        byte[] b= Arrays.randomBytes(NUM+64);
        byte[] c= new byte[NUM+64];
        byte[] d= new byte[NUM+64];
        Out.println("\n  (a,b,c offsets: "+Out.hex(Arrays.ptrOfs(a))+","+Out.hex(Arrays.ptrOfs(b))+","+Out.hex(Arrays.ptrOfs(c))+")");
        for (int count=0; count<COPY_TEST_COUNT; count++) {
          int aofs= random(100), bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.copy(a,aofs,b,bofs,len);
          System.arraycopy(a,aofs,c,bofs,len);
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in copy(...,"+aofs+",...,"+bofs+","+len+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("copy() ("+n+") tested; ");
        for (int count=0; count<FILL_TEST_COUNT; count++) {
          byte v= (byte)randomInt();
          int bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.fill(b,bofs,bofs+len,v);
          for (int k=0; k<len; k++) c[bofs+k]= v;
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in fill(...,"+bofs+","+(bofs+len)+","+v+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("fill() ("+n+") tested; ");
        if (Arrays.isNative()) {
          for (int count=0; count<COERCIVECOPY_TEST_COUNT; count++) {
            int aofs= random(100), bofs= random(100), len= random(NUM-100);
            Object oa= randomTypeArray(NUM);
            Object ob= randomTypeArray(NUM);
            Object oc= Arrays.copy(ob);
            Arrays.setNative(false);
            Arrays.coerciveCopy(oa,aofs,ob,bofs,len);
            Arrays.setNative(true);
            Arrays.coerciveCopy(oa,aofs,oc,bofs,len);
            if (!Arrays.equals(ob,oc)) {
              byte[] bBytes= Arrays.coerciveCopyBytes(ob);
              byte[] cBytes= Arrays.coerciveCopyBytes(oc);
              for (int k=0; k<bBytes.length; k++) if (bBytes[k]!=cBytes[k]) throw new AssertionError("\nError in coerciveCopy("+JVM.toJavaClassName(oa)+","+aofs+","+JVM.toJavaClassName(ob)+","+bofs+","+len+") at "+k+": "+bBytes[k]+" instead "+cBytes[k]);
            }
          }
          Out.println("coerciveCopy() tested");
        } else {
          Out.println("coerciveCopy() NOT tested");
        }
        System.gc();
        Arrays.copy(a,c); Arrays.copy(b,d); Arrays.min(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=min(a[k+ao],b[k+bo])) throw new AssertionError("\nError in min ("+n+"s) at "+k+": "+c[k+ao]+" instead "+min(a[k+ao],b[k+bo])+"=min("+a[k+ao]+","+b[k+bo]+")");
        Arrays.copy(a,c); Arrays.copy(b,d); Arrays.max(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=max(a[k+ao],b[k+bo])) throw new AssertionError("\nError in max ("+n+"s) at "+k+": "+c[k+ao]+" instead "+max(a[k+ao],b[k+bo])+"=max("+a[k+ao]+","+b[k+bo]+")");
        java.nio.ByteBuffer ba= java.nio.ByteBuffer.allocateDirect(NUM);
        java.nio.ByteBuffer bb= java.nio.ByteBuffer.allocateDirect(NUM);
        for (int k=0; k<NUM; k++) ba.put(k,a[k]);
        for (int k=0; k<NUM; k++) bb.put(k,b[k]);
        Arrays.min(ba,0,bb,0,NUM); for (int k=0; k<NUM; k++) if (ba.get(k)!=min(a[k],b[k])) throw new AssertionError("\nError in min (ByteBuffer) at "+k+": "+ba.get(k)+" instead "+min(a[k],b[k])+"=min("+a[k]+","+b[k]+")");
        for (int k=0; k<NUM; k++) ba.put(k,a[k]);
        for (int k=0; k<NUM; k++) bb.put(k,b[k]);
        Arrays.max(ba,0,bb,0,NUM); for (int k=0; k<NUM; k++) if (ba.get(k)!=max(a[k],b[k])) throw new AssertionError("\nError in max (ByteBuffer) at "+k+": "+ba.get(k)+" instead "+max(a[k],b[k])+"=max("+a[k]+","+b[k]+")");
        Arrays.copy(a,c); Arrays.min(a,b);
        System.gc();

        for (int m=0; m<BOFS_ARRAY_FOR_COPY.length; m++) {
          int bofs= BOFS_ARRAY_FOR_COPY[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) System.arraycopy(a,0,c,bofs,NUM); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) System.arraycopy(a,1,c,2,1); d2= Timing.diffcpu(t1);
          Out.println("arraycopy["+Out.rightPad(bofs+"",2)+"](): "+tInfo(d1,d2));
        }
        for (int m=0; m<BOFS_ARRAY_FOR_COPY.length; m++) {
          int bofs= BOFS_ARRAY_FOR_COPY[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.copy(a,0,c,bofs,NUM); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.copy(a,1,c,2,1); d2= Timing.diffcpu(t1);
          Out.println("copy["+Out.rightPad(bofs+"",2)+"]():      "+tInfo(d1,d2));
        }
        for (int m=0; m<BOFS_ARRAY_FOR_FILL.length; m++) {
          int bofs= BOFS_ARRAY_FOR_FILL[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.fill(c,bofs,NUM,(byte)157); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.fill(c,2,1,(byte)1); d2= Timing.diffcpu(t1);
          Out.println("fill["+Out.rightPad(bofs+"",2)+"]("+n+"):  "+tInfo(d1,d2));
        }

        Arrays.min(a,b);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min("+n+"):       "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max("+n+"):       "+tInfo(d1,d2));

        for (int k=0; k<NUM; k++) ba.put(k,c[k]);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(ba,0,bb,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(ba,1,bb,2,1); d2= Timing.diffcpu(t1);
        Out.println("min(ByteBuffer): "+tInfo(d1,d2));

        for (int k=0; k<NUM; k++) ba.put(k,c[k]);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(ba,0,bb,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(ba,1,bb,2,1); d2= Timing.diffcpu(t1);
        Out.println("max(ByteBuffer): "+tInfo(d1,d2));

        a= Arrays.randomBytes(NUM+64);
        b= Arrays.randomBytes(NUM+64);
        Arrays.copy(a,c); Arrays.copy(b,d); Arrays.minu(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=minu(a[k+ao],b[k+bo])) throw new AssertionError("\nError in minu ("+n+"s) at "+k+": "+c[k+ao]+" instead "+minu(a[k+ao],b[k+bo])+"=minu("+a[k+ao]+","+b[k+bo]+")");
        Arrays.copy(a,c); Arrays.copy(b,d); Arrays.maxu(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=maxu(a[k+ao],b[k+bo])) throw new AssertionError("\nError in maxu ("+n+"s) at "+k+": "+c[k+ao]+" instead "+maxu(a[k+ao],b[k+bo])+"=maxu("+a[k+ao]+","+b[k+bo]+")");
        for (int k=0; k<NUM; k++) ba.put(k,a[k]);
        for (int k=0; k<NUM; k++) bb.put(k,b[k]);
        Arrays.minu(ba,0,bb,0,NUM); for (int k=0; k<NUM; k++) if (ba.get(k)!=minu(a[k],b[k])) throw new AssertionError("\nError in minu (ByteBuffer) at "+k+": "+ba.get(k)+" instead "+minu(a[k],b[k])+"=minu("+a[k]+","+b[k]+")");
        for (int k=0; k<NUM; k++) ba.put(k,a[k]);
        for (int k=0; k<NUM; k++) bb.put(k,b[k]);
        Arrays.maxu(ba,0,bb,0,NUM); for (int k=0; k<NUM; k++) if (ba.get(k)!=maxu(a[k],b[k])) throw new AssertionError("\nError in maxu (ByteBuffer) at "+k+": "+ba.get(k)+" instead "+maxu(a[k],b[k])+"=maxu("+a[k]+","+b[k]+")");
        Arrays.copy(a,c); Arrays.min(a,b);

        Arrays.min(a,b);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.minu(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.minu(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("minu("+n+"):      "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.maxu(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.maxu(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("maxu("+n+"):      "+tInfo(d1,d2));

        for (int k=0; k<NUM; k++) ba.put(k,c[k]);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.minu(ba,0,bb,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.minu(ba,1,bb,2,1); d2= Timing.diffcpu(t1);
        Out.println("minu(ByteBuffer):"+tInfo(d1,d2));

        for (int k=0; k<NUM; k++) ba.put(k,c[k]);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.maxu(ba,0,bb,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.maxu(ba,1,bb,2,1); d2= Timing.diffcpu(t1);
        Out.println("maxu(ByteBuffer):"+tInfo(d1,d2));
      }
      {
        n= "short";
        short[] a= Arrays.randomShorts(NUM);
        short[] b= Arrays.randomShorts(NUM);
        short[] c= new short[NUM+64];
        short[] d= new short[NUM+64];
        byte[] aBytes= new byte[NUM*2];
        for (int count=0; count<COPY_TEST_COUNT; count++) {
          int aofs= random(100), bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.copy(a,aofs,b,bofs,len);
          System.arraycopy(a,aofs,c,bofs,len);
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in copy(...,"+aofs+",...,"+bofs+","+len+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("\ncopy() ("+n+") tested; ");
        for (int count=0; count<FILL_TEST_COUNT; count++) {
          byte v= (byte)randomInt();
          int bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.fill(b,bofs,bofs+len,v);
          for (int k=0; k<len; k++) c[bofs+k]= v;
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in fill(...,"+bofs+","+(bofs+len)+","+v+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.println("fill() ("+n+") tested");
        System.gc();

        c= Arrays.copy(a); d=Arrays.copy(b); Arrays.min(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=min(a[k+ao],b[k+bo])) throw new AssertionError("\nError in min ("+n+"s) at "+k+": "+c[k+ao]+" instead "+min(a[k+ao],b[k+bo])+"=min("+a[k+ao]+","+b[k+bo]+")");
        c= Arrays.copy(a); d=Arrays.copy(b); Arrays.max(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=max(a[k+ao],b[k+bo])) throw new AssertionError("\nError in max ("+n+"s) at "+k+": "+c[k+ao]+" instead "+max(a[k+ao],b[k+bo])+"=max("+a[k+ao]+","+b[k+bo]+")");
        c= Arrays.copy(a); d=Arrays.copy(b); Arrays.minu(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=minu(a[k+ao],b[k+bo])) throw new AssertionError("\nError in minu ("+n+"s) at "+k+": "+c[k+ao]+" instead "+minu(a[k+ao],b[k+bo])+"=minu("+a[k+ao]+","+b[k+bo]+")");
        c= Arrays.copy(a); d=Arrays.copy(b); Arrays.maxu(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=maxu(a[k+ao],b[k+bo])) throw new AssertionError("\nError in maxu ("+n+"s) at "+k+": "+c[k+ao]+" instead "+maxu(a[k+ao],b[k+bo])+"=maxu("+a[k+ao]+","+b[k+bo]+")");
        System.gc();
        Arrays.copy(a,c); Arrays.min(a,b);

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(aBytes,0,a,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(aBytes,1,a,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")<-bytes:   "+tInfo(d1,d2));

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(a,0,aBytes,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(a,1,aBytes,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")->bytes:   "+tInfo(d1,d2));

        for (int m=0; m<BOFS_ARRAY_FOR_FILL.length; m++) {
          int bofs= BOFS_ARRAY_FOR_FILL[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.fill(c,bofs,NUM,(byte)157); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.fill(c,2,1,(byte)1); d2= Timing.diffcpu(t1);
          Out.println("fill["+Out.rightPad(bofs+"",2)+"]("+n+"): "+tInfo(d1,d2));
        }

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min("+n+"):      "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max("+n+"):      "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.minu(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.minu(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("minu("+n+"):     "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.maxu(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.maxu(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("maxu("+n+"):     "+tInfo(d1,d2));
      }
      {
        n= "int";
        int[] a= Arrays.randomInts(NUM);
        int[] b= Arrays.randomInts(NUM);
        int[] c= new int[NUM+64];
        int[] d= new int[NUM+64];
        byte[] aBytes= new byte[NUM*4];
        for (int count=0; count<COPY_TEST_COUNT; count++) {
          int aofs= random(100), bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.copy(a,aofs,b,bofs,len);
          System.arraycopy(a,aofs,c,bofs,len);
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in copy(...,"+aofs+",...,"+bofs+","+len+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("\ncopy() ("+n+") tested; ");
        for (int count=0; count<FILL_TEST_COUNT; count++) {
          byte v= (byte)randomInt();
          int bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.fill(b,bofs,bofs+len,v);
          for (int k=0; k<len; k++) c[bofs+k]= v;
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in fill(...,"+bofs+","+(bofs+len)+","+v+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.println("fill() ("+n+") tested");

        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.min(c,d); for (int k=0; k<NUM; k++) if (c[k]!=min(a[k],b[k])) throw new AssertionError("\nError in min ("+n+"s) at "+k+": "+c[k]+" instead "+min(a[k],b[k])+"=min("+a[k]+","+b[k]+")");
        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.max(c,d); for (int k=0; k<NUM; k++) if (c[k]!=max(a[k],b[k])) throw new AssertionError("\nError in max ("+n+"s) at "+k+": "+c[k]+" instead "+max(a[k],b[k])+"=max("+a[k]+","+b[k]+")");
        System.gc();
        Arrays.copy(a,c); Arrays.min(c,b);

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(aBytes,0,a,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(aBytes,1,a,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")<-bytes:   "+tInfo(d1,d2));

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(a,0,aBytes,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(a,1,aBytes,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")->bytes:   "+tInfo(d1,d2));

        for (int m=0; m<BOFS_ARRAY_FOR_FILL.length; m++) {
          int bofs= BOFS_ARRAY_FOR_FILL[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.fill(c,bofs,NUM,(byte)157); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.fill(c,2,1,(byte)1); d2= Timing.diffcpu(t1);
          Out.println("fill["+Out.rightPad(bofs+"",2)+"]("+n+"):   "+tInfo(d1,d2));
        }

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.min(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+min("+n+"):   "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.max(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+max("+n+"):   "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min("+n+"):        "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max("+n+"):        "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,2,NUM-2); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min[2]("+n+"):     "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,2,NUM-2); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max[2]("+n+"):     "+tInfo(d1,d2));
      }
      {
        n= "long";
        long[] a= Arrays.randomLongs(NUM);
        long[] b= Arrays.randomLongs(NUM);
        long[] c= new long[NUM];
        long[] d= new long[NUM];
        byte[] aBytes= new byte[NUM*8];
        for (int count=0; count<COPY_TEST_COUNT; count++) {
          int aofs= random(100), bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.copy(a,aofs,b,bofs,len);
          System.arraycopy(a,aofs,c,bofs,len);
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in copy(...,"+aofs+",...,"+bofs+","+len+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("\ncopy() ("+n+") tested; ");
        for (int count=0; count<FILL_TEST_COUNT; count++) {
          byte v= (byte)randomInt();
          int bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.fill(b,bofs,bofs+len,v);
          for (int k=0; k<len; k++) c[bofs+k]= v;
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in fill(...,"+bofs+","+(bofs+len)+","+v+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.println("fill() ("+n+") tested");

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(aBytes,0,a,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(aBytes,1,a,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")<-bytes:   "+tInfo(d1,d2));

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(a,0,aBytes,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(a,1,aBytes,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")->bytes:   "+tInfo(d1,d2));

        for (int m=0; m<BOFS_ARRAY_FOR_FILL.length; m++) {
          int bofs= BOFS_ARRAY_FOR_FILL[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.fill(c,bofs,NUM,(byte)157); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.fill(c,2,1,(byte)1); d2= Timing.diffcpu(t1);
          Out.println("fill["+Out.rightPad(bofs+"",2)+"]("+n+"):  "+tInfo(d1,d2));
        }

        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.min(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=min(a[k+ao],b[k+bo])) throw new AssertionError("\nError in min ("+n+"s) at "+k+": "+c[k+ao]+" instead "+min(a[k+ao],b[k+bo])+"=min("+a[k+ao]+","+b[k+bo]+")");
        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.max(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=max(a[k+ao],b[k+bo])) throw new AssertionError("\nError in max ("+n+"s) at "+k+": "+c[k+ao]+" instead "+max(a[k+ao],b[k+bo])+"=max("+a[k+ao]+","+b[k+bo]+")");
        System.gc();
        Arrays.copy(a,c); Arrays.min(c,b);

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.min(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+min("+n+"):  "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.max(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+max("+n+"):  "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min("+n+"):       "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max("+n+"):       "+tInfo(d1,d2));
      }
      {
        n= "float";
        float[] a= Arrays.randomFloats(NUM);
        float[] b= Arrays.randomFloats(NUM);
        float[] c= new float[NUM+64];
        float[] d= new float[NUM+64];
        byte[] aBytes= new byte[NUM*4];
        for (int count=0; count<FILL_TEST_COUNT; count++) {
          byte v= (byte)randomInt();
          int bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.fill(b,bofs,bofs+len,v);
          for (int k=0; k<len; k++) c[bofs+k]= v;
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in fill(...,"+bofs+","+(bofs+len)+","+v+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("\nfill() ("+n+") tested; ");
        if (Arrays.isNative()) {
          for (int count=0; count<COERCIVECOPY_TEST_COUNT; count++) {
            int aofs= random(100)*4, bofs= random(100)*4, len= random((NUM-100)*4);
            Object oa= Arrays.randomFloats(NUM);
            Object ob= Arrays.randomFloats(NUM);
            Object oc= Arrays.copy(ob);
            Arrays.setNative(false);
            Arrays.coerciveCopy(oa,aofs,ob,bofs,len);
            Arrays.setNative(true);
            Arrays.coerciveCopy(oa,aofs,oc,bofs,len);
            if (!Arrays.equals(ob,oc)) {
              byte[] bBytes= Arrays.coerciveCopyBytes(ob);
              byte[] cBytes= Arrays.coerciveCopyBytes(oc);
              for (int k=0; k<bBytes.length; k++) if (bBytes[k]!=cBytes[k]) throw new AssertionError("\nError in coerciveCopy("+JVM.toJavaClassName(oa)+","+aofs+","+JVM.toJavaClassName(ob)+","+bofs+","+len+") at "+k+": "+bBytes[k]+" instead "+cBytes[k]);
            }
          }
          Out.println("coerciveCopy() tested");
        } else {
          Out.println("coerciveCopy() NOT tested");
        }

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(aBytes,0,a,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(aBytes,1,a,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")<-bytes:   "+tInfo(d1,d2));

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(a,0,aBytes,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(a,1,aBytes,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")->bytes:   "+tInfo(d1,d2));

        for (int m=0; m<BOFS_ARRAY_FOR_FILL.length; m++) {
          int bofs= BOFS_ARRAY_FOR_FILL[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.fill(c,bofs,NUM,(byte)157); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.fill(c,2,1,(byte)1); d2= Timing.diffcpu(t1);
          Out.println("fill["+Out.rightPad(bofs+"",2)+"]("+n+"): "+tInfo(d1,d2));
        }

        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.min(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=Math.min(a[k+ao],b[k+bo])) throw new AssertionError("\nError in min ("+n+"s) at "+k+": "+c[k+ao]+" instead "+Math.min(a[k+ao],b[k+bo])+"=min("+a[k+ao]+","+b[k+bo]+")");
        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.max(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=Math.max(a[k+ao],b[k+bo])) throw new AssertionError("\nError in max ("+n+"s) at "+k+": "+c[k+ao]+" instead "+Math.max(a[k+ao],b[k+bo])+"=max("+a[k+ao]+","+b[k+bo]+")");
        System.gc();
        Arrays.copy(a,c); Arrays.min(c,b);

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.min(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+min("+n+"): "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.max(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+max("+n+"): "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min("+n+"):      "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max("+n+"):      "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,2,NUM-2); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min[2]("+n+"):   "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,2,NUM-2); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max[2]("+n+"):   "+tInfo(d1,d2));
      }
      {
        n= "double";
        double[] a= Arrays.randomDoubles(NUM);
        double[] b= Arrays.randomDoubles(NUM);
        double[] c= new double[NUM+64];
        double[] d= new double[NUM+64];
        byte[] aBytes= new byte[NUM*8];
        for (int count=0; count<FILL_TEST_COUNT; count++) {
          byte v= (byte)randomInt();
          int bofs= random(100), len= random(NUM-100);
          System.arraycopy(a,0,b,0,NUM); System.arraycopy(a,0,c,0,NUM);
          Arrays.fill(b,bofs,bofs+len,v);
          for (int k=0; k<len; k++) c[bofs+k]= v;
          for (int k=0; k<NUM; k++) if (b[k]!=c[k]) throw new AssertionError("\nError in fill(...,"+bofs+","+(bofs+len)+","+v+") at "+k+": "+b[k]+" instead "+c[k]);
        }
        Out.print("\nfill() ("+n+") tested; ");
        if (Arrays.isNative()) {
          for (int count=0; count<COERCIVECOPY_TEST_COUNT; count++) {
            int aofs= random(100)*8, bofs= random(100)*8, len= random((NUM-100)*8);
            Object oa= Arrays.randomDoubles(NUM);
            Object ob= Arrays.randomDoubles(NUM);
            Object oc= Arrays.copy(ob);
            Arrays.setNative(false);
            Arrays.coerciveCopy(oa,aofs,ob,bofs,len);
            Arrays.setNative(true);
            Arrays.coerciveCopy(oa,aofs,oc,bofs,len);
            if (!Arrays.equals(ob,oc)) {
              byte[] bBytes= Arrays.coerciveCopyBytes(ob);
              byte[] cBytes= Arrays.coerciveCopyBytes(oc);
              for (int k=0; k<bBytes.length; k++) if (bBytes[k]!=cBytes[k]) throw new AssertionError("\nError in coerciveCopy("+JVM.toJavaClassName(oa)+","+aofs+","+JVM.toJavaClassName(ob)+","+bofs+","+len+") at "+k+": "+bBytes[k]+" instead "+cBytes[k]);
            }
          }
          Out.println("coerciveCopy() tested");
        } else {
          Out.println("coerciveCopy() NOT tested");
        }

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(aBytes,0,a,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(aBytes,1,a,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")<-bytes:   "+tInfo(d1,d2));

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.coerciveCopy(a,0,aBytes,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.coerciveCopy(a,1,aBytes,2,1); d2= Timing.diffcpu(t1);
        Out.println("coerciveCopy("+n+")->bytes:   "+tInfo(d1,d2));

        for (int m=0; m<BOFS_ARRAY_FOR_FILL.length; m++) {
          int bofs= BOFS_ARRAY_FOR_FILL[m];
          d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.fill(c,bofs,NUM,(byte)157); d1=min(d1,Timing.diffcpu(t1));}
          t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.fill(c,2,1,(byte)1); d2= Timing.diffcpu(t1);
          Out.println("fill["+Out.rightPad(bofs+"",2)+"]("+n+"):"+tInfo(d1,d2));
        }

        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.min(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=Math.min(a[k+ao],b[k+bo])) throw new AssertionError("\nError in min ("+n+"s) at "+k+": "+c[k+ao]+" instead "+Math.min(a[k+ao],b[k+bo])+"=min("+a[k+ao]+","+b[k+bo]+")");
        c=Arrays.copy(a); d=Arrays.copy(b); Arrays.max(c,ao,d,bo,NUM-256); for (int k=0; k<NUM-256; k++) if (c[k+ao]!=Math.max(a[k+ao],b[k+bo])) throw new AssertionError("\nError in max ("+n+"s) at "+k+": "+c[k+ao]+" instead "+Math.max(a[k+ao],b[k+bo])+"=max("+a[k+ao]+","+b[k+bo]+")");
        System.gc();
        Arrays.copy(a,c); Arrays.min(c,b);

        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.min(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+min("+n+"): "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) {Arrays.copy(a,c); Arrays.max(c,0,b,0,NUM);} d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("copy+max("+n+"): "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.min(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.min(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("min("+n+"):     "+tInfo(d1,d2));

        Arrays.copy(c,a);
        d1= Long.MAX_VALUE; for (int k=0; k<NUMe2; k++) {t1= Timing.timecpu(); for (int i=0; i<NUMe1; i++) Arrays.max(a,0,b,0,NUM); d1=min(d1,Timing.diffcpu(t1));}
        t1= Timing.timecpu(); for (int k=0; k<NUMc; k++) Arrays.max(a,1,b,2,1); d2= Timing.diffcpu(t1);
        Out.println("max("+n+"):     "+tInfo(d1,d2));
      }
    Out.println();
    Out.println();
    }
  }
}
