/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package algart.lib.legacy;

import java.util.*;
import java.nio.*;
import java.lang.reflect.*;

/**
 * <p>Title:     <b>Algart Arrays</b><br>
 * Description:  Wide library of tools for working with arrays (native-optimized)<br>
 * @author       Daniel Alievsky
 * @version      1.1
 * @deprecated
 */

public class AArrays extends ATools implements TrueStatic {

  /* Constants, CPU service functions */

    // so, it means that all commands CMOVxx, FCMOVxx, FCOMI, FUCOMI are avaiable
  public static final long CPU_SSE= 1<<25;   // always set if CPU_SSE2 is set

  public static boolean isNative= AArraysNative.loaded;

  private static int nativeMinLenFill= 100;
  private static int nativeMinLenPairOp= 100;

  /* Arrays service */

  public static Object newCompatible(Object a, int len) {
  // Here and below "Object" means "some Array"
    if (a==null) return null;
    return Array.newInstance(a.getClass().getComponentType(), len);
  }
  public static Object ensureCapacity(Object a, int requiredLen) {
    int len= Array.getLength(a);
    if (len>=requiredLen) return a;
    int newLen= len<16?32:len<1024?(len+1)*2:(len+1)*5/4;
    if (newLen<0) newLen= Integer.MAX_VALUE;
    else if (requiredLen>newLen) newLen= requiredLen;
    Object newa= Array.newInstance(a.getClass().getComponentType(),newLen);
    copy(a,0,newa,0,len);
    return newa;
  }
  public static Object copyEnsured(Object a, int aofs, Object b, int bofs, int len) {
    if (b==null) b= Array.newInstance(a.getClass().getComponentType(),len);
    else         b= ensureCapacity(b,bofs+len);
    copy(a,aofs,b,bofs,len);
    return b;
  }

  /* Copying arrays with same-type components  */

    public static int[] copy(int a[])         {return copy(a,0,a.length);}
  public static long[] copy(long a[])       {return copy(a,0,a.length);}
  public static float[] copy(float a[])     {return copy(a,0,a.length);}
  public static double[] copy(double a[])   {return copy(a,0,a.length);}
  public static Object copy(Object a)       {return a==null? null: copy(a,0,Array.getLength(a));}
  // In the last function, "Object" means "some Array"

    public static short[] copy(short a[], int aofs, int len) {
    short[] r= new short[len]; copy(a,aofs,r,0,len); return r;
  }
  public static int[] copy(int a[], int aofs, int len) {
    int[] r= new int[len]; copy(a,aofs,r,0,len); return r;
  }
  public static long[] copy(long a[], int aofs, int len) {
    long[] r= new long[len]; copy(a,aofs,r,0,len); return r;
  }
  public static float[] copy(float a[], int aofs, int len) {
    float[] r= new float[len]; copy(a,aofs,r,0,len); return r;
  }
  public static double[] copy(double a[], int aofs, int len) {
    double[] r= new double[len]; copy(a,aofs,r,0,len); return r;
  }
  // We should skip "Object[]" case, else Java will try to interpret any
  // array of objects as "Object[]" instead "Object"
  public static Object copy(Object a, int aofs, int len) {
  // Here "Object" means "some Array"
    if (a==null) return null;
    Object r= newCompatible(a,len);
    copy(a,aofs,r,0,len);
    return r;
  }

    public static void copy(byte[] a, byte[] b)     {copy(a,0,b,0,min(a.length,b.length));}

    public static void copy(int[] a, int[] b)       {copy(a,0,b,0,min(a.length,b.length));}

    public static void copy(float[] a, float[] b)   {copy(a,0,b,0,min(a.length,b.length));}

    public static void copy(byte[] a, int aofs, byte[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs,b,bofs,len-1);
      return;
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }
  public static void copy(short[] a, int aofs, short[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs<<1,b,bofs<<1,(len-1)<<1);
      return;
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }
  public static void copy(int[] a, int aofs, int[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs<<2,b,bofs<<2,(len-1)<<2);
      return;
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }
  public static void copy(long[] a, int aofs, long[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs<<3,b,bofs<<3,(len-1)<<3);
      return;
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }
  public static void copy(float[] a, int aofs, float[] b, int bofs, int len) {
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs<<2,b,bofs<<2,(len-1)<<2);
      return;
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }
  public static void copy(double[] a, int aofs, double[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs<<3,b,bofs<<3,(len-1)<<3);
      return;
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }
  public static void copy(Object a, int aofs, Object b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && (AArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
      Class elemClass= a.getClass().getComponentType();
      int sizeLog= elemClass==byte.class? 0:
        elemClass==short.class || elemClass==char.class? 1:
        elemClass==int.class || elemClass==float.class? 2:
        elemClass==long.class || elemClass==double.class? 3:
        -1;
      if (sizeLog!=-1) {
        System.arraycopy(a,aofs,b,bofs,1);
        System.arraycopy(a,aofs+len-1,b,bofs+len-1,1);
        AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs<<sizeLog,b,bofs<<sizeLog,
          (len-1)<<sizeLog);
        return;
      }
    }
    System.arraycopy(a,aofs,b,bofs,len);
  }

  public static int[] append(int[] a, int[] b) {
    if (a==null) return b; if (b==null) return a;
    int[] r= new int[a.length+b.length];
    copy(a,0,r,0,a.length);
    copy(b,0,r,a.length,b.length);
    return r;
  }

    public static Object append(Object a, Object b) {
  // Here "Object" means "some Array"
    if (a==null) return b; if (b==null) return a;
    int len1= Array.getLength(a);
    int len2= Array.getLength(b);
    Object r= newCompatible(b,len1+len2);
    copy(a,0,r,0,len1);
    copy(b,0,r,len1,len2);
    return r;
  }

  /* Coercion arrays with different-type components */

  private static void checkByteCC(byte[] a, int ofs)  {byte v= a[ofs];}
  private static void checkByteCC(int[] a, int ofs)   {int v= a[ofs/4];}
  public static void coerciveCopy(int[] a, int aofs, byte[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
      checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs,b,bofs,len);
      return;
    }
    for (; len>0 && (aofs&3)!=0; len--,aofs++,bofs++) b[bofs]= (byte)(a[aofs>>>2]>>>((aofs&3)<<3));
    int k=aofs>>2, l=len>>2, n=k+l;
    for (; k<n; k++,bofs+=4) {
      int v= a[k];
      b[bofs]= (byte)v;          b[bofs+1]= (byte)(v>>>8);
      b[bofs+2]= (byte)(v>>>16); b[bofs+3]= (byte)(v>>>24);
    }
    len-=(l<<2); aofs+=(l<<2);
    for (; len>0; len--,aofs++,bofs++) b[bofs]= (byte)(a[aofs>>>2]>>>((aofs&3)<<3));
  }
  public static void coerciveCopy(byte[] a, int aofs, int[] b, int bofs, int len) {
    if (len<=0) return;
    if (isNative && AArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
      checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
      AArraysNative.copyBytes(AArraysNative.cpuInfo,a,aofs,b,bofs,len);
      return;
    }
    for (; len>0 && (bofs&3)!=0; len--,aofs++,bofs++) {int sh= (bofs&3)<<3; b[bofs>>>2]= (b[bofs>>>2]&~(0xFF<<sh))|((a[aofs]&0xFF)<<sh);}
    int k=bofs>>2, l=len>>2, n=k+l;
    for (; k<n; k++,aofs+=4) {
      b[k]= (a[aofs]&0xFF) | (a[aofs+1]&0xFF)<<8
        | (a[aofs+2]&0xFF)<<16 | (a[aofs+3]&0xFF)<<24;
    }
    len-=(l<<2); bofs+=(l<<2);
    for (; len>0; len--,aofs++,bofs++) {int sh= (bofs&3)<<3; b[bofs>>>2]= (b[bofs>>>2]&~(0xFF<<sh))|((a[aofs]&0xFF)<<sh);}
  }


  /* Converting arrays: int[] to byte[], char[] to long[], etc...  */

    public static double[] toDoubles(byte[] a)    {return toDoubles(a,0,a.length);}
  public static double[] toDoubles(short[] a)   {return toDoubles(a,0,a.length);}
  public static double[] toDoubles(int[] a)     {return toDoubles(a,0,a.length);}
  public static double[] toDoubles(long[] a)    {return toDoubles(a,0,a.length);}
  public static double[] toDoubles(float[] a)   {return toDoubles(a,0,a.length);}


    public static double[] toDoubles(byte[] a, int aofs, int len)    {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
  public static double[] toDoubles(short[] a, int aofs, int len)   {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
  public static double[] toDoubles(int[] a, int aofs, int len)     {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
  public static double[] toDoubles(long[] a, int aofs, int len)    {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
  public static double[] toDoubles(float[] a, int aofs, int len)   {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}


  public static void toDoubles(byte[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
  public static void toDoubles(short[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
  public static void toDoubles(int[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
  public static void toDoubles(long[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
  public static void toDoubles(float[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}

  /* Filling arrays  */

  public static int[] constantInts(int len, int v) {
    int[] a= new int[len]; fill(a,v); return a;
  }

  public static void fill(boolean[] a, boolean v) {fill(a, 0, a.length, v);}

    public static void fill(int[] a, int v)         {fill(a, 0, a.length, v);}

    public static void fill(double[] a, double v)   {fill(a, 0, a.length, v);}

    public static void fill(boolean[] a, int beginIndex, int endIndex, boolean v) {
    // We are not using native here: Java doesn't specify sizeof(boolean)
    for (int k=beginIndex; k<endIndex; k++) a[k] = v;
  }

    public static void fill(int[] a, int beginIndex, int endIndex, int v) {
    if (isNative && AArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
      a[beginIndex]= a[endIndex-1]= v;
      AArraysNative.fill(AArraysNative.cpuInfo,a,beginIndex,endIndex,v);
      return;
    }
    for (int k=beginIndex; k<endIndex; k++) a[k] = v;
  }

    public static void fill(double[] a, int beginIndex, int endIndex,double v){
    if (isNative && AArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
      a[beginIndex]= a[endIndex-1]= v;
      AArraysNative.fill(AArraysNative.cpuInfo,a,beginIndex,endIndex,v);
      return;
    }
    for (int k=beginIndex; k<endIndex; k++) a[k] = v;
  }


    public static int indexOf(int[] a, int b)             {return indexOf(a,b,0);}

    public static int indexOf(Object[] a, Object b)       {return indexOf(a,b,0);}

    public static int indexOf(int[] a, int b, int fromIndex)             {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}

    public static int indexOf(Object[] a, Object b, int fromIndex)       {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}


    public static Object deleteOne(Object a, int aofs) {
    return replace(a,aofs,1,null,0,0);
  }

    public static Object replace(Object a, int aofs, int oldlen, Object b, int bofs, int newlen) {
    try {
      int alen= Array.getLength(a);
      Object r= newCompatible(a,alen+newlen-oldlen);
      if (oldlen>alen-aofs) oldlen= alen-aofs;
      if (oldlen<0) oldlen= 0;
      copy(a,0,r,0,aofs);
      copy(b,0,r,aofs,newlen);
      copy(a,aofs+oldlen,r,aofs+newlen,alen-aofs-oldlen);
      return r;
    } catch (NullPointerException e) {
      return copy(a);
    } catch (IndexOutOfBoundsException e) {
      return copy(a);
    }
  }

    public static interface Matcher {
    public boolean matches(Object o);
  }

    public static Object[] choose(Object[] a, Matcher matcher, Object[] results) {
    List list= new ArrayList();
    for (int k=0; k<a.length; k++) {
      if (matcher.matches(a[k])) list.add(a[k]);
    }
    return list.toArray(results==null?(Object[])newCompatible(a,0):results);
  }
  public static Object[] delete(Object[] a, Matcher matcher) {
    return delete(a,matcher,null);
  }
  public static Object[] delete(Object[] a, Matcher matcher, Object[] results) {
    List list= new ArrayList();
    for (int k=0; k<a.length; k++) {
      if (!matcher.matches(a[k])) list.add(a[k]);
    }
    return list.toArray(results==null?(Object[])newCompatible(a,0):results);
  }


  /* Minimum and maximum  */

  public static byte min(byte[] a)      {byte r= Byte.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]<r) r= a[i]; return r;}
  public static short min(short[] a)    {short r= Short.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]<r) r= a[i]; return r;}
  public static int min(int[] a)        {int r= Integer.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]<r) r= a[i]; return r;}
  public static long min(long[] a)      {long r= Long.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]<r) r= a[i]; return r;}
  public static float min(float[] a)    {float r= Float.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]<r) r= a[i]; return r;}
  public static double min(double[] a)  {double r= Double.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]<r) r= a[i]; return r;}

  public static byte max(byte[] a)      {byte r= Byte.MIN_VALUE; for (int i=0;i<a.length;i++) if (a[i]>r) r= a[i]; return r;}
  public static short max(short[] a)    {short r= Short.MIN_VALUE; for (int i=0;i<a.length;i++) if (a[i]>r) r= a[i]; return r;}
  public static int max(int[] a)        {int r= Integer.MIN_VALUE; for (int i=0;i<a.length;i++) if (a[i]>r) r= a[i]; return r;}
  public static long max(long[] a)      {long r= Long.MIN_VALUE; for (int i=0;i<a.length;i++) if (a[i]>r) r= a[i]; return r;}
  public static float max(float[] a)    {float r= -Float.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]>r) r= a[i]; return r;}
  public static double max(double[] a)  {double r= -Double.MAX_VALUE; for (int i=0;i<a.length;i++) if (a[i]>r) r= a[i]; return r;}

  public static long sum(byte[] a)      {long r= 0; for (int i=0;i<a.length;r+=a[i],i++); return r;}
  public static long sum(short[] a)     {long r= 0; for (int i=0;i<a.length;r+=a[i],i++); return r;}
  public static long sum(int[] a)       {long r= 0; for (int i=0;i<a.length;r+=a[i],i++); return r;}
  public static long sum(long[] a)      {long r= 0; for (int i=0;i<a.length;r+=a[i],i++); return r;}
  public static double sum(float[] a)   {double r= 0.0; for (int i=0;i<a.length;r+=a[i],i++); return r;}
  public static double sum(double[] a)  {double r= 0.0; for (int i=0;i<a.length;r+=a[i],i++); return r;}

  public static int[] minIndexes(byte[] a) {
    if (a==null) return new int[0];
    int cnt= 0;
    byte min= Byte.MAX_VALUE;
    for (int k= 0; k< a.length; k++) {
      if (min> a[k]) { min= a[k]; cnt= 1; }
      else if (min== a[k]) { cnt++; }
    }
    int[] res= new int[cnt]; cnt= 0;
    for (int k= 0; k< a.length; k++) { if (a[k]== min) res[cnt++]= k; }
    return res;
  }
  public static int[] minIndexes(short[] a) {
    if (a==null) return new int[0];
    int cnt= 0;
    short min= Short.MAX_VALUE;
    for (int k= 0; k< a.length; k++) {
      if (min> a[k]) { min= a[k]; cnt= 1; }
      else if (min== a[k]) { cnt++; }
    }
    int[] res= new int[cnt]; cnt= 0;
    for (int k= 0; k< a.length; k++) { if (a[k]== min) res[cnt++]= k; }
    return res;
  }
  public static int[] minIndexes(int[] a) {
    if (a==null) return new int[0];
    int cnt= 0;
    int min= Integer.MAX_VALUE;
    for (int k= 0; k< a.length; k++) {
      if (min> a[k]) { min= a[k]; cnt= 1; }
      else if (min== a[k]) { cnt++; }
    }
    int[] res= new int[cnt]; cnt= 0;
    for (int k= 0; k< a.length; k++) { if (a[k]== min) res[cnt++]= k; }
    return res;
  }
  public static int[] minIndexes(long[] a) {
    if (a==null) return new int[0];
    int cnt= 0;
    long min= Long.MAX_VALUE;
    for (int k= 0; k< a.length; k++) {
      if (min> a[k]) { min= a[k]; cnt= 1; }
      else if (min== a[k]) { cnt++; }
    }
    int[] res= new int[cnt]; cnt= 0;
    for (int k= 0; k< a.length; k++) { if (a[k]== min) res[cnt++]= k; }
    return res;
  }
  public static int[] minIndexes(float[] a) {
    if (a==null) return new int[0];
    int cnt= 0;
    float min= Float.POSITIVE_INFINITY;
    for (int k= 0; k< a.length; k++) {
      if (min> a[k]) { min= a[k]; cnt= 1; }
      else if (min== a[k]) { cnt++; }
    }
    int[] res= new int[cnt]; cnt= 0;
    for (int k= 0; k< a.length; k++) { if (a[k]== min) res[cnt++]= k; }
    return res;
  }
  public static int[] minIndexes(double[] a) {
    if (a==null) return new int[0];
    int cnt= 0;
    double min= Double.POSITIVE_INFINITY;
    for (int k= 0; k< a.length; k++) {
      if (min> a[k]) { min= a[k]; cnt= 1; }
      else if (min== a[k]) { cnt++; }
    }
    int[] res= new int[cnt]; cnt= 0;
    for (int k= 0; k< a.length; k++) { if (a[k]== min) res[cnt++]= k; }
    return res;
  }


  public static void min(Object a, Object b) throws Exception {
    min(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
  }
  public static void max(Object a, Object b) throws Exception {
    max(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
  }
  public static void min(Object a, int aofs, Object b, int bofs, int len) throws Exception {
    if (a instanceof byte[]) min((byte[])a,aofs,(byte[])b,bofs,len);
    else if (a instanceof short[]) min((short[])a,aofs,(short[])b,bofs,len);
    else if (a instanceof int[]) min((int[])a,aofs,(int[])b,bofs,len);
    else if (a instanceof long[]) min((long[])a,aofs,(long[])b,bofs,len);
    else if (a instanceof float[]) min((float[])a,aofs,(float[])b,bofs,len);
    else if (a instanceof double[]) min((double[])a,aofs,(double[])b,bofs,len);
    else if (a instanceof ByteBuffer) min((ByteBuffer)a,aofs,(ByteBuffer)b,bofs,len);
    else throw new IllegalArgumentException("Unsupported argument type in AArrays.min(): "+toJavaClassName(a));
  }
  public static void max(Object a, int aofs, Object b, int bofs, int len) throws Exception {
    if (a instanceof byte[]) max((byte[])a,aofs,(byte[])b,bofs,len);
    else if (a instanceof short[]) max((short[])a,aofs,(short[])b,bofs,len);
    else if (a instanceof int[]) max((int[])a,aofs,(int[])b,bofs,len);
    else if (a instanceof long[]) max((long[])a,aofs,(long[])b,bofs,len);
    else if (a instanceof float[]) max((float[])a,aofs,(float[])b,bofs,len);
    else if (a instanceof double[]) max((double[])a,aofs,(double[])b,bofs,len);
    else if (a instanceof ByteBuffer) max((ByteBuffer)a,aofs,(ByteBuffer)b,bofs,len);
    else throw new IllegalArgumentException("Unsupported argument type in AArrays.max(): "+toJavaClassName(a));
  }

  public static void min(byte[] a, int aofs, byte[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>=nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]>b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]>b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]>b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
  }
  public static void max(byte[] a, int aofs, byte[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>=nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
      AArraysNative.max(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]<b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]<b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]<b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
  }

  public static void min(ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp && a.isDirect() && b.isDirect()) {
      boolean check= a.get(aofs)<b.get(bofs) && a.get(aofs+len-1)<b.get(bofs+len-1); //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a.get(aofs)>b.get(bofs)) a.put(aofs,b.get(bofs));
      if (a.get(aofs+1)>b.get(bofs+1)) a.put(aofs+1,b.get(bofs+1));
      if (a.get(aofs+2)>b.get(bofs+2)) a.put(aofs+2,b.get(bofs+2));
      if (a.get(aofs+3)>b.get(bofs+3)) a.put(aofs+3,b.get(bofs+3));
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a.get(aofs)>b.get(bofs)) a.put(aofs,b.get(bofs));
  }
  public static void max(ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp && a.isDirect() && b.isDirect()) {
      boolean check= a.get(aofs)<b.get(bofs) && a.get(aofs+len-1)<b.get(bofs+len-1); //avoiding GPF
      AArraysNative.max(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a.get(aofs)<b.get(bofs)) a.put(aofs,b.get(bofs));
      if (a.get(aofs+1)<b.get(bofs+1)) a.put(aofs+1,b.get(bofs+1));
      if (a.get(aofs+2)<b.get(bofs+2)) a.put(aofs+2,b.get(bofs+2));
      if (a.get(aofs+3)<b.get(bofs+3)) a.put(aofs+3,b.get(bofs+3));
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a.get(aofs)<b.get(bofs)) a.put(aofs,b.get(bofs));
  }

  public static void min(short[] a, int aofs, short[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]>b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]>b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]>b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
  }
  public static void max(short[] a, int aofs, short[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
      AArraysNative.max(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]<b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]<b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]<b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
  }

  public static void min(int[] a, int aofs, int[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]>b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]>b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]>b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
  }
  public static void max(int[] a, int aofs, int[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
      AArraysNative.max(AArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]<b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]<b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]<b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
  }

  public static void min(long[] a, int aofs, long[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo, a, aofs, b, bofs, len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]>b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]>b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]>b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
  }
  public static void max(long[] a, int aofs, long[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
      AArraysNative.max(AArraysNative.cpuInfo, a, aofs, b, bofs, len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]<b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]<b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]<b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
  }

  public static void min(float[] a, int aofs, float[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo, a, aofs, b, bofs, len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]>b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]>b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]>b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
  }
  public static void max(float[] a, int aofs, float[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
      AArraysNative.max(AArraysNative.cpuInfo, a, aofs, b, bofs, len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]<b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]<b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]<b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
  }

  public static void min(double[] a, int aofs, double[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
      AArraysNative.min(AArraysNative.cpuInfo, a, aofs, b, bofs, len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]>b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]>b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]>b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]>b[bofs]) a[aofs]=b[bofs];
  }
  public static void max(double[] a, int aofs, double[] b, int bofs, int len) {
    if (isNative && AArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
      boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
      AArraysNative.max(AArraysNative.cpuInfo, a, aofs, b, bofs, len); return;
    }
    int aofsmax=aofs+len-3;
    for (; aofs<aofsmax; aofs+=4,bofs+=4) {
      if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
      if (a[aofs+1]<b[bofs+1]) a[aofs+1]=b[bofs+1];
      if (a[aofs+2]<b[bofs+2]) a[aofs+2]=b[bofs+2];
      if (a[aofs+3]<b[bofs+3]) a[aofs+3]=b[bofs+3];
    }
    for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if (a[aofs]<b[bofs]) a[aofs]=b[bofs];
  }

  /* Adding and subtracting */

  public static void add(Object a, Object b) throws Exception {
    add(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
  }
  public static void sub(Object a, Object b) throws Exception {
    sub(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
  }
  public static void adds(Object a, Object b) throws Exception {
    adds(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
  }
  public static void add(Object a, int aofs, Object b, int bofs, int len) throws Exception {
    if (a instanceof byte[] && b instanceof byte[]) add((byte[]) a, aofs, (byte[]) b, bofs, len);
    else if (a instanceof short[] && b instanceof byte[]) add((short[]) a, aofs, (byte[]) b, bofs, len);
    else if (a instanceof short[] && b instanceof short[]) add((short[]) a, aofs, (short[]) b, bofs, len);
    else if (a instanceof int[] && b instanceof byte[]) add((int[])a,aofs,(byte[])b,bofs,len);
    else if (a instanceof int[] && b instanceof short[]) add((int[])a,aofs,(short[])b,bofs,len);
    else if (a instanceof int[] && b instanceof int[]) add((int[])a,aofs,(int[])b,bofs,len);
    else if (a instanceof long[] && b instanceof long[]) add((long[])a,aofs,(long[])b,bofs,len);
    else if (a instanceof float[] && b instanceof float[]) add((float[])a,aofs,(float[])b,bofs,len);
    else if (a instanceof float[] && b instanceof double[]) add((float[])a,aofs,(double[])b,bofs,len);
    else if (a instanceof double[] && b instanceof float[]) add((double[])a,aofs,(float[])b,bofs,len);
    else if (a instanceof double[] && b instanceof double[]) add((double[])a,aofs,(double[])b,bofs,len);
    else throw new IllegalArgumentException("Unsupported arguments types in AArrays.add(): "+toJavaClassName(a)+","+toJavaClassName(b));
  }
  public static void sub(Object a, int aofs, Object b, int bofs, int len) throws Exception {
    if (a instanceof byte[] && b instanceof byte[]) sub((byte[]) a, aofs, (byte[]) b, bofs, len);
    else if (a instanceof short[] && b instanceof byte[]) sub((short[]) a, aofs, (byte[]) b, bofs, len);
    else if (a instanceof short[] && b instanceof short[]) sub((short[]) a, aofs, (short[]) b, bofs, len);
    else if (a instanceof int[] && b instanceof byte[]) sub((int[])a,aofs,(byte[])b,bofs,len);
    else if (a instanceof int[] && b instanceof short[]) sub((int[])a,aofs,(short[])b,bofs,len);
    else if (a instanceof int[] && b instanceof int[]) sub((int[])a,aofs,(int[])b,bofs,len);
    else if (a instanceof long[] && b instanceof long[]) sub((long[])a,aofs,(long[])b,bofs,len);
    else if (a instanceof float[] && b instanceof float[]) sub((float[])a,aofs,(float[])b,bofs,len);
    else if (a instanceof float[] && b instanceof double[]) sub((float[])a,aofs,(double[])b,bofs,len);
    else if (a instanceof double[] && b instanceof float[]) sub((double[])a,aofs,(float[])b,bofs,len);
    else if (a instanceof double[] && b instanceof double[]) sub((double[])a,aofs,(double[])b,bofs,len);
    else throw new IllegalArgumentException("Unsupported arguments types in AArrays.sub(): "+toJavaClassName(a)+","+toJavaClassName(b));
  }
  public static void adds(Object a, int aofs, Object b, int bofs, int len) throws Exception {
    if (a instanceof byte[] && b instanceof byte[]) adds((byte[]) a, aofs, (byte[]) b, bofs, len);
    else if (a instanceof short[] && b instanceof byte[]) adds((short[]) a, aofs, (byte[]) b, bofs, len);
    else if (a instanceof short[] && b instanceof short[]) adds((short[]) a, aofs, (short[]) b, bofs, len);
    else throw new IllegalArgumentException("Unsupported arguments types in AArrays.adds(): "+toJavaClassName(a)+","+toJavaClassName(b));
  }

  public static void add(byte[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(byte[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(short[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(short[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(short[] a, int aofs, short[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(short[] a, int aofs, short[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(int[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(int[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(int[] a, int aofs, short[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(int[] a, int aofs, short[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(int[] a, int aofs, int[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(int[] a, int aofs, int[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(long[] a, int aofs, long[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(long[] a, int aofs, long[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(float[] a, int aofs, float[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(float[] a, int aofs, float[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(float[] a, int aofs, double[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(float[] a, int aofs, double[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(double[] a, int aofs, float[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(double[] a, int aofs, float[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }
  public static void add(double[] a, int aofs, double[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=b[bofs];
  }
  public static void sub(double[] a, int aofs, double[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=b[bofs];
  }

  public static void adds(byte[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(byte)median((int)a[aofs]+(int)b[bofs],Byte.MIN_VALUE,Byte.MAX_VALUE);}
  }
  public static void adds(short[] a, int aofs, byte[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((int)a[aofs]+(int)b[bofs],Short.MIN_VALUE,Short.MAX_VALUE);}
  }
  public static void adds(short[] a, int aofs, short[] b, int bofs, int len) {
    for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((int)a[aofs]+(int)b[bofs],Short.MIN_VALUE,Short.MAX_VALUE);}
  }

}

class AArraysNative implements TrueStatic {

  static boolean copyBytesImplemented= false;
  static boolean fillImplemented= false;
  static boolean minmaxImplemented= false;
  static boolean minmaxuImplemented= false;
  static native void detectImplementedFlags();

  static long cpuInfo= 0;
  static native long getCpuInfoInternal();
  static native int ptrOfs(Object a);

  static native void copyBytes(long cpuInfo, Object a, int aofs, Object b, int bofs, int len);
  static native void fill(long cpuInfo, char[] a, int beginIndex, int endIndex, char v);
  static native void fill(long cpuInfo, byte[] a, int beginIndex, int endIndex, byte v);
  static native void fill(long cpuInfo, short[] a, int beginIndex, int endIndex, short v);
  static native void fill(long cpuInfo, int[] a, int beginIndex, int endIndex, int v);
  static native void fill(long cpuInfo, long[] a, int beginIndex, int endIndex, long v);
  static native void fill(long cpuInfo, float[] a, int beginIndex, int endIndex, float v);
  static native void fill(long cpuInfo, double[] a, int beginIndex, int endIndex, double v);
  static native void fill(long cpuInfo, Object[] a, int beginIndex, int endIndex, Object v);
  static native void min(long cpuInfo, byte[] a, int aofs, byte[] b, int bofs, int len);
  static native void max(long cpuInfo, byte[] a, int aofs, byte[] b, int bofs, int len);
  static native void min(long cpuInfo, ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len);
  static native void max(long cpuInfo, ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len);
  static native void min(long cpuInfo, short[] a, int aofs, short[] b, int bofs, int len);
  static native void max(long cpuInfo, short[] a, int aofs, short[] b, int bofs, int len);
  static native void min(long cpuInfo, int[] a, int aofs, int[] b, int bofs, int len);
  static native void max(long cpuInfo, int[] a, int aofs, int[] b, int bofs, int len);
  static native void min(long cpuInfo, long[] a, int aofs, long[] b, int bofs, int len);
  static native void max(long cpuInfo, long[] a, int aofs, long[] b, int bofs, int len);
  static native void min(long cpuInfo, float[] a, int aofs, float[] b, int bofs, int len);
  static native void max(long cpuInfo, float[] a, int aofs, float[] b, int bofs, int len);
  static native void min(long cpuInfo, double[] a, int aofs, double[] b, int bofs, int len);
  static native void max(long cpuInfo, double[] a, int aofs, double[] b, int bofs, int len);
  static native void minu(long cpuInfo, byte[] a, int aofs, byte[] b, int bofs, int len);
  static native void maxu(long cpuInfo, byte[] a, int aofs, byte[] b, int bofs, int len);
  static native void minu(long cpuInfo, ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len);
  static native void maxu(long cpuInfo, ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len);
  static native void minu(long cpuInfo, short[] a, int aofs, short[] b, int bofs, int len);
  static native void maxu(long cpuInfo, short[] a, int aofs, short[] b, int bofs, int len);
  static boolean loaded= false;
  /* // Native support is very deprecated
  static {
    try {
      System.loadLibrary("AArraysNative");
      loaded= true;
      detectImplementedFlags();
      cpuInfo= getCpuInfoInternal();
    } catch (UnsatisfiedLinkError e) {
    } catch (SecurityException e) {
    }
  }
  */
}